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

package services.testdata

import model.persisted.PartnerGraduateProgrammes
import play.api.mvc.RequestHeader
import repositories._
import repositories.partnergraduateprogrammes.PartnerGraduateProgrammesRepository
import uk.gov.hmrc.play.http.HeaderCarrier
import model.command.testdata.GeneratorConfig

import scala.concurrent.ExecutionContext.Implicits.global

object InProgressPartnerGraduateProgrammesStatusGenerator extends InProgressPartnerGraduateProgrammesStatusGenerator {
  override val previousStatusGenerator = InProgressSchemePreferencesStatusGenerator
  override val pgpRepository = faststreamPartnerGraduateProgrammesRepository
}

trait InProgressPartnerGraduateProgrammesStatusGenerator extends ConstructiveGenerator {
  val pgpRepository: PartnerGraduateProgrammesRepository

  def generate(generationId: Int, generatorConfig: GeneratorConfig)(implicit hc: HeaderCarrier, rh: RequestHeader) = {
    for {
      candidateInPreviousStatus <- previousStatusGenerator.generate(generationId, generatorConfig)
      _ <- pgpRepository.update(candidateInPreviousStatus.applicationId.get, PartnerGraduateProgrammes(true,
        Some(List("Entrepreneur First", "Police Now"))))
    } yield {
      candidateInPreviousStatus
    }
  }
}
