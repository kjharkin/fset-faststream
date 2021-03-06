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

package model.exchange.passmarksettings

import model.SchemeType.SchemeType
import play.api.libs.json.Json
import reactivemongo.bson.Macros

trait PassMark {
  def schemeName: SchemeType
  def schemeThresholds: PassMarkThresholds
}

case class Phase1PassMark(
  schemeName: SchemeType,
  schemeThresholds: Phase1PassMarkThresholds
) extends PassMark

object Phase1PassMark {
  implicit val phase1PassMark = Json.format[Phase1PassMark]
  implicit val phase1PassMarkHandler = Macros.handler[Phase1PassMark]
}

case class Phase2PassMark(
  schemeName: SchemeType,
  schemeThresholds: Phase2PassMarkThresholds
) extends PassMark

object Phase2PassMark {
  implicit val phase2PassMark = Json.format[Phase2PassMark]
  implicit val phase2PassMarkHandler = Macros.handler[Phase2PassMark]
}

case class Phase3PassMark(
  schemeName: SchemeType,
  schemeThresholds: Phase3PassMarkThresholds
) extends PassMark

object Phase3PassMark {
  implicit val phase3PassMark = Json.format[Phase3PassMark]
  implicit val phase3PassMarkHandler = Macros.handler[Phase3PassMark]
}
