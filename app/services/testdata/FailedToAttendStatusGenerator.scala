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

package services.testdata

import connectors.testdata.ExchangeObjects.DataGenerationResponse
import model.CandidateScoresCommands.CandidateScoresAndFeedback
import play.api.mvc.RequestHeader
import repositories._
import model.ApplicationStatus._
import repositories.application.GeneralApplicationRepository
import uk.gov.hmrc.play.http.HeaderCarrier
import model.command.testdata.GeneratorConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FailedToAttendStatusGenerator extends FailedToAttendStatusGenerator {
  override val previousStatusGenerator = AllocationStatusGenerator
  override val aRepository = applicationRepository
  override val aasRepository = applicationAssessmentScoresRepository
}

trait FailedToAttendStatusGenerator extends ConstructiveGenerator {
  val aRepository: GeneralApplicationRepository
  val aasRepository: ApplicationAssessmentScoresRepository

  def generate(generationId: Int, generatorConfig: GeneratorConfig)
    (implicit hc: HeaderCarrier, rh: RequestHeader): Future[DataGenerationResponse] = {
    for {
      candidateInPreviousStatus <- previousStatusGenerator.generate(generationId, generatorConfig)
      _ <- aasRepository.save(CandidateScoresAndFeedback(candidateInPreviousStatus.applicationId.get, Some(false),
        assessmentIncomplete = true))
      _ <- aRepository.updateStatus(candidateInPreviousStatus.applicationId.get, FAILED_TO_ATTEND)
    } yield {
      candidateInPreviousStatus
    }

  }
}
