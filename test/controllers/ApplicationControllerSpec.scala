/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import config.TestFixtureBase
import mocks.application.DocumentRootInMemoryRepository
import model.EvaluationResults.Green
import model.Exceptions.NotFoundException
import model.{ ApplicationRoute, SchemeType }
import model.command.WithdrawApplication
import model.persisted.{ PassmarkEvaluation, SchemeEvaluationResult }
import org.mockito.ArgumentMatchers.{ eq => eqTo, _ }
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest, Helpers }
import repositories.application.GeneralApplicationRepository
import services.AuditService
import services.application.ApplicationService
import services.onlinetesting.phase3.EvaluatePhase3ResultService
import testkit.UnitWithAppSpec
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.language.postfixOps

class ApplicationControllerSpec extends UnitWithAppSpec {

  val ApplicationId = "1111-1111"
  val aWithdrawApplicationRequest = WithdrawApplication("Something", Some("Else"), "Candidate")
  val auditDetails = Map("applicationId" -> ApplicationId, "withdrawRequest" -> aWithdrawApplicationRequest.toString)

  "Create Application" must {

    "create an application" in new TestFixture {
      val result = TestApplicationController.createApplication(createApplicationRequest(
        s"""
           |{
           |  "userId":"1234",
           |  "frameworkId":"FASTSTREAM-2016",
           |  "applicationRoute":"${ApplicationRoute.Faststream}"
           |}
        """.stripMargin
      ))
      val jsonResponse = contentAsJson(result)

      (jsonResponse \ "applicationStatus").as[String] must be("CREATED")
      (jsonResponse \ "userId").as[String] must be("1234")

      verify(mockAuditService).logEvent(eqTo("ApplicationCreated"))(any[HeaderCarrier], any[RequestHeader])
    }

    "return a system error on invalid json" in new TestFixture {
      val result =
        TestApplicationController.createApplication(createApplicationRequest(
          s"""
             |{
             |  "some":"1234",
             |  "other":"FASTTRACK-2015"
             |}
          """.stripMargin
        ))

      status(result) must be(400)
    }
  }

  "Application Progress" must {

    "return the progress of an application" in new TestFixture {
      val result = TestApplicationController.applicationProgress(ApplicationId)(applicationProgressRequest(ApplicationId)).run
      val jsonResponse = contentAsJson(result)

      (jsonResponse \ "applicationId").as[String] must be(ApplicationId)
      (jsonResponse \ "personalDetails").as[Boolean] must be(true)

      status(result) must be(200)
    }

    "return a system error when applicationId doesn't exists" in new TestFixture {
      val result = TestApplicationController.applicationProgress("1111-1234")(applicationProgressRequest("1111-1234")).run

      status(result) must be(404)
    }
  }

  "Find application" must {

    "return the application" in new TestFixture {
      val result = TestApplicationController.findApplication(
        "validUser",
        "validFramework"
      )(findApplicationRequest("validUser", "validFramework")).run
      val jsonResponse = contentAsJson(result)

      (jsonResponse \ "applicationId").as[String] must be(ApplicationId)
      (jsonResponse \ "applicationStatus").as[String] must be("CREATED")
      (jsonResponse \ "userId").as[String] must be("validUser")

      status(result) must be(200)
    }

    "return a system error when application doesn't exists" in new TestFixture {
      val result = TestApplicationController.findApplication(
        "invalidUser",
        "invalidFramework"
      )(findApplicationRequest("invalidUser", "invalidFramework")).run

      status(result) must be(404)
    }
  }

  "Withdraw application" must {
    "withdraw the application" in new TestFixture {
      val result = TestApplicationController.withdrawApplication("1111-1111")(withdrawApplicationRequest("1111-1111")(
        s"""
           |{
           |  "reason":"Something",
           |  "otherReason":"Else",
           |  "withdrawer":"Candidate"
           |}
        """.stripMargin
      ))
      status(result) must be(200)
    }
  }

  "Preview application" must {
    "mark the application as previewed" in new TestFixture {
      val result = TestApplicationController.preview(ApplicationId)(previewApplicationRequest(ApplicationId)(
        s"""
           |{
           |  "flag": true
           |}
        """.stripMargin
      ))
      status(result) must be(200)
      verify(mockAuditService).logEvent(eqTo("ApplicationPreviewed"))(any[HeaderCarrier], any[RequestHeader])
    }
  }

  "Get Scheme Results" must {

    "return the scheme results for an application" in new TestFixture {
      val resultToSave = List(SchemeEvaluationResult(SchemeType.DigitalAndTechnology, Green.toString))
      val evaluation = PassmarkEvaluation("version1", None, resultToSave)
      when(mockPassmarkService.getPassmarkEvaluation(any[String])).thenReturn(Future.successful(evaluation))

      val result = TestApplicationController.getSchemeResults(ApplicationId)(getSchemeResultsRequest(ApplicationId)).run
      val jsonResponse = contentAsJson(result)

      jsonResponse mustBe Json.toJson(resultToSave)
      status(result) must be(200)
    }

    "Return a 404 if no results are found for the application Id" in new TestFixture {
      val result = TestApplicationController.applicationProgress("1111-1234")(applicationProgressRequest("1111-1234")).run
      status(result) must be(404)
    }
  }

  "Mark as ready for export" must {
    "return a 404 if the application can't be found" in new TestFixture {
      when(mockApplicationService.markForExportToParity(any[String])(any[HeaderCarrier])).thenReturn(Future.failed(new NotFoundException()))
      val result = TestApplicationController.markForExportToParity("appId").apply(FakeRequest())
      status(result) mustBe 404

    }
  }

  trait TestFixture extends TestFixtureBase {
    val mockApplicationService = mock[ApplicationService]
    val mockPassmarkService = mock[EvaluatePhase3ResultService]
    when(mockApplicationService.withdraw(eqTo(ApplicationId), eqTo(aWithdrawApplicationRequest))(any[HeaderCarrier], any[RequestHeader]))
      .thenReturn(Future.successful(()))

    object TestApplicationController extends ApplicationController {
      override val appRepository: GeneralApplicationRepository = DocumentRootInMemoryRepository
      override val auditService: AuditService = mockAuditService
      override val applicationService: ApplicationService = mockApplicationService
      override val passmarkService: EvaluatePhase3ResultService = mockPassmarkService
    }

    def applicationProgressRequest(applicationId: String) = {
      FakeRequest(Helpers.GET, controllers.routes.ApplicationController.applicationProgress(applicationId).url, FakeHeaders(), "")
        .withHeaders("Content-Type" -> "application/json")
    }

    def findApplicationRequest(userId: String, frameworkId: String) = {
      FakeRequest(Helpers.GET, controllers.routes.ApplicationController.findApplication(userId, frameworkId).url, FakeHeaders(), "")
        .withHeaders("Content-Type" -> "application/json")
    }

    def createApplicationRequest(jsonString: String) = {
      val json = Json.parse(jsonString)
      FakeRequest(Helpers.PUT, controllers.routes.ApplicationController.createApplication().url, FakeHeaders(), json)
        .withHeaders("Content-Type" -> "application/json")
    }

    def withdrawApplicationRequest(applicationId: String)(jsonString: String) = {
      val json = Json.parse(jsonString)
      FakeRequest(Helpers.PUT, controllers.routes.ApplicationController.withdrawApplication(applicationId).url, FakeHeaders(), json)
        .withHeaders("Content-Type" -> "application/json")
    }

    def previewApplicationRequest(applicationId: String)(jsonString: String) = {
      val json = Json.parse(jsonString)
      FakeRequest(Helpers.PUT, controllers.routes.ApplicationController.preview(applicationId).url, FakeHeaders(), json)
        .withHeaders("Content-Type" -> "application/json")
    }

    def getSchemeResultsRequest(applicationId: String) = {
      FakeRequest(Helpers.GET, controllers.routes.ApplicationController.getSchemeResults(applicationId).url, FakeHeaders(), "")
        .withHeaders("Content-Type" -> "application/json")
    }
  }
}
