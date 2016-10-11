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

package model.persisted

import model.{ ApplicationStatus, Phase1TestProfileExamples, SelectedSchemesExamples }
import org.joda.time.DateTime

object ApplicationPhase1EvaluationExamples {
  def application(implicit now: DateTime) = ApplicationPhase1ReadyForEvaluation("app1", ApplicationStatus.PHASE1_TESTS, isGis = false,
    Phase1TestProfileExamples.profile, SelectedSchemesExamples.TwoSchemes)
}