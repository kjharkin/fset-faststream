/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.onlinetesting

import _root_.services.AuditService
import config.LaunchpadGatewayConfig
import connectors.ExchangeObjects._
import connectors.LaunchpadGatewayClient.{ InviteApplicantRequest, InviteApplicantResponse, RegisterApplicantRequest }
import connectors.{ CSREmailClient, EmailClient, LaunchpadGatewayClient }
import factories.{ DateTimeFactory, UUIDFactory }
import model.OnlineTestCommands._
import model.ProgressStatuses
import model.persisted.Phase1TestProfileWithAppId
import model.persisted.phase3tests.{ Phase3Test, Phase3TestApplication, Phase3TestGroup }
import org.joda.time.DateTime
import play.api.Logger
import repositories._
import repositories.application.GeneralApplicationRepository
import repositories.onlinetesting.Phase3TestRepository
import services.events.{ EventService, EventSink }
import services.onlinetesting.OnlineTestService.ReportIdNotDefinedException
import services.onlinetesting.Phase3TestService.InviteLinkCouldNotBeCreatedSuccessfully
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

object Phase3TestService extends Phase3TestService {
  import config.MicroserviceAppConfig._
  val appRepository = applicationRepository
  val p3TestRepository = phase3TestRepository
  val cdRepository = faststreamContactDetailsRepository
  val launchpadGatewayClient = LaunchpadGatewayClient
  val tokenFactory = UUIDFactory
  val dateTimeFactory = DateTimeFactory
  val emailClient = CSREmailClient
  val auditService = AuditService
  val gatewayConfig = launchpadGatewayConfig
  val eventService = EventService

  case class InviteLinkCouldNotBeCreatedSuccessfully(message: String) extends Exception(message)
}

trait Phase3TestService extends EventSink {
  val appRepository: GeneralApplicationRepository
  val p3TestRepository: Phase3TestRepository
  val cdRepository: contactdetails.ContactDetailsMongoRepository
  val launchpadGatewayClient: LaunchpadGatewayClient
  val tokenFactory: UUIDFactory
  val dateTimeFactory: DateTimeFactory
  val emailClient: EmailClient
  val auditService: AuditService
  val gatewayConfig: LaunchpadGatewayConfig

  def registerAndInviteForTestGroup(application: Phase3TestApplication)(implicit hc: HeaderCarrier): Future[Unit] = {
    registerAndInviteForTestGroup(application, getInterviewIdForApplication(application))
  }

  def registerAndInviteForTestGroup(application: Phase3TestApplication, interviewId: Int)
    (implicit hc: HeaderCarrier): Future[Unit] = {
    val (invitationDate, expirationDate) =
      dateTimeFactory.nowLocalTimeZone -> dateTimeFactory.nowLocalTimeZone.plusDays(gatewayConfig.phase3Tests.timeToExpireInDays)

    for {
      emailAddress <- candidateEmailAddress(application)
      candidateId <- registerAndInviteApplicant(application, emailAddress, interviewId, invitationDate, expirationDate)
      // _ <- emailInviteToApplicant(application, emailAddress, invitationDate, expirationDate)
      _ <- markAsInvited(application)(Phase1TestProfile(expirationDate = expirationDate, tests = successfullyRegisteredTests))
    } yield audit("Phase3TestInvitationProcessComplete", application.userId)
  }

  private def registerAndInviteApplicant(application: Phase3TestApplication, emailAddress: String, interviewId: Int, invitationDate: DateTime,
    expirationDate: DateTime
  )(implicit hc: HeaderCarrier): Future[Phase3Test] = {
    val customCandidateId = "FSCND-" + tokenFactory.generateUUID()

    for {
      candidateId <- registerApplicant(application, emailAddress, customCandidateId)
      invitation <- inviteApplicant(application, interviewId, customCandidateId)
    } yield {
      Phase3Test(interviewId = interviewId,
        usedForResults = true,
        testUrl = invitation.link.url,
        token = invitation.custom_invite_id,
        candidateId = candidateId,
        customCandidateId = invitation.custom_candidate_id,
        invitationDate = invitationDate
      )
    }
  }

  def registerApplicant(application: Phase3TestApplication,
                        emailAddress: String, customCandidateId: String)(implicit hc: HeaderCarrier): Future[String] = {
    val registerApplicant = RegisterApplicantRequest(emailAddress, customCandidateId, application.preferredName, application.lastName)
    launchpadGatewayClient.registerApplicant(registerApplicant).map { registration =>
      audit("UserRegisteredForPhase3Test", application.userId)
      registration.candidate_id
    }
  }

  private def inviteApplicant(application: Phase3TestApplication, interviewId: Int, candidateId: String)
    (implicit hc: HeaderCarrier): Future[InviteApplicantResponse] = {

    val customInviteId = "FSINV-" + tokenFactory.generateUUID()

    val completionRedirectUrl = s"${gatewayConfig.phase3Tests.candidateCompletionRedirectUrl}/fset-fast-stream/" +
      s"phase3-tests/complete/$customInviteId"

    val inviteApplicant = InviteApplicantRequest(interviewId, candidateId, customInviteId, completionRedirectUrl)
    launchpadGatewayClient.inviteApplicant(inviteApplicant).map { invitation =>
      invitation.link.status match {
        case "success" =>
          audit("UserInvitedToPhase3Test", application.userId)
          invitation
        case _ =>
          throw InviteLinkCouldNotBeCreatedSuccessfully(s"Status of invite for " +
            s"candidate $candidateId to $interviewId was ${invitation.link.status} (Invite ID: $customInviteId)")
      }
    }
  }

  private def emailInviteToApplicant(application: Phase3TestApplication, emailAddress: String,
    invitationDate: DateTime, expirationDate: DateTime
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    val preferredName = application.preferredName
    emailClient.sendOnlineTestInvitation(emailAddress, preferredName, expirationDate).map { _ =>
      audit("Phase3TestInvitationEmailSent", application.userId, Some(emailAddress))
    }
  }

  private def merge(currentProfile: Option[Phase3TestGroup], newProfile: Phase3TestGroup): Phase3TestGroup = currentProfile match {
    case None =>
      newProfile
    case Some(profile) =>
      val scheduleIdsToArchive = newProfile.tests.map(_.scheduleId)
      val existingTestsAfterUpdate = profile.tests.map(t =>
        if (scheduleIdsToArchive.contains(t.scheduleId)) {
          t.copy(usedForResults = false)
        } else {
          t
        }
      )
      Phase1TestProfile(newProfile.expirationDate, existingTestsAfterUpdate ++ newProfile.tests)
  }

  private def markAsInvited(application: Phase3TestApplication)
                           (newPhase3TestGroup: Phase3TestGroup): Future[Unit] = for {
    currentp3TestGroup <- p3TestRepository.getTestGroup(application.applicationId)
    updatedOnlineTestProfile = merge(currentOnlineTestProfile, newOnlineTestProfile)
    _ <- phase1TestRepo.insertOrUpdatePhase1TestGroup(application.applicationId, updatedOnlineTestProfile)
    _ <- phase1TestRepo.removePhase1TestProfileProgresses(application.applicationId, determineStatusesToRemove(updatedOnlineTestProfile))
  } yield {
    audit("Phase3TestInvited", application.userId)
  }

  private def candidateEmailAddress(application: Phase3TestApplication): Future[String] =
    cdRepository.find(application.userId).map(_.email)

  private def audit(event: String, userId: String, emailAddress: Option[String] = None): Unit = {
    Logger.info(s"$event for user $userId")

    auditService.logEventNoRequest(
      event,
      Map("userId" -> userId) ++ emailAddress.map("email" -> _).toMap
    )
  }

  // TODO: This needs to cater for 10% extra, 33% extra etc
  private def getInterviewIdForApplication(application: Phase3TestApplication): Int = {
      gatewayConfig.phase3Tests.mainInterviewId
  }
}

