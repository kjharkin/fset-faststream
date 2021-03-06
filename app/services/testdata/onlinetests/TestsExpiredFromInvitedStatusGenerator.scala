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

package services.testdata.onlinetests

import model.ProgressStatuses.{ PHASE1_TESTS_EXPIRED, PHASE2_TESTS_EXPIRED, PHASE3_TESTS_EXPIRED, ProgressStatus }
import model.command.testdata.GeneratorConfig
import play.api.mvc.RequestHeader
import services.onlinetesting.OnlineTestService
import services.onlinetesting.phase1.Phase1TestService
import services.onlinetesting.phase2.Phase2TestService
import services.onlinetesting.phase3.Phase3TestService
import services.testdata.ConstructiveGenerator
import services.testdata.onlinetests.phase1.Phase1TestsInvitedStatusGenerator
import services.testdata.onlinetests.phase2.Phase2TestsInvitedStatusGenerator
import services.testdata.onlinetests.phase3.Phase3TestsInvitedStatusGenerator
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

object Phase1TestsExpiredFromInvitedStatusGenerator extends TestsExpiredFromInvitedStatusGenerator {
  override val previousStatusGenerator = Phase1TestsInvitedStatusGenerator
  override val otService = Phase1TestService
  override val expiredStatus = PHASE1_TESTS_EXPIRED
}

object Phase2TestsExpiredFromInvitedStatusGenerator extends TestsExpiredFromInvitedStatusGenerator {
  override val previousStatusGenerator = Phase2TestsInvitedStatusGenerator
  override val otService = Phase2TestService
  override val expiredStatus = PHASE2_TESTS_EXPIRED
}

object Phase3TestsExpiredFromInvitedStatusGenerator extends TestsExpiredFromInvitedStatusGenerator {
  override val previousStatusGenerator = Phase3TestsInvitedStatusGenerator
  override val otService = Phase3TestService
  override val expiredStatus = PHASE3_TESTS_EXPIRED
}

trait TestsExpiredFromInvitedStatusGenerator extends ConstructiveGenerator {
  val otService: OnlineTestService
  val expiredStatus: ProgressStatus

  def generate(generationId: Int, generatorConfig: GeneratorConfig)(implicit hc: HeaderCarrier, rh: RequestHeader) = {
    for {
      candidateInPreviousStatus <- previousStatusGenerator.generate(generationId, generatorConfig)
      _ <- otService.commitProgressStatus(candidateInPreviousStatus.applicationId.get, expiredStatus)
    } yield {
      candidateInPreviousStatus
    }

  }
}
