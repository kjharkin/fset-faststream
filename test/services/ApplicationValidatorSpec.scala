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

package services

import model.PersistedObjects.PersonalDetails
import model.persisted.AssistanceDetailsExamples
import model.{ ApplicationValidator, LocationPreference, Preferences }
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import repositories.FrameworkRepository.{ CandidateHighestQualification, Framework, Location, Region }

class ApplicationValidatorSpec extends PlaySpec {

  import ApplicationValidatorSpec._

  "given valid personal details" should {
    "return true when details are valid" in {
      val validator = ApplicationValidator(personalDetails, assistanceDetails, None, List())
      validator.validateGeneralDetails must be(true)
    }
  }

  "given assistance details" should {
    "return true when details are all there" in {
      val validator = ApplicationValidator(personalDetails, assistanceDetails, None, List())
      validator.validateAssistanceDetails must be(true)
    }

    "return false if we don't have a description for at venue adjustment" in {
      val validator = ApplicationValidator(
        personalDetails,
        assistanceDetails.copy(needsSupportAtVenueDescription = None), None, List()
      )
      validator.validateAssistanceDetails must be(false)
    }

    "return false if we don't have a description for online adjustment" in {
      val validator = ApplicationValidator(
        personalDetails,
        assistanceDetails.copy(needsSupportForOnlineAssessmentDescription = None), None, List()
      )
      validator.validateAssistanceDetails must be(false)
    }

    "return false if we don't have gis setting and have disability" in {
      val validator = ApplicationValidator(
        personalDetails,
        assistanceDetails.copy(guaranteedInterview = None), None, List()
      )
      validator.validateAssistanceDetails must be(false)
    }
  }

  "given schemes and locations" should {
    "return true if the chosen locations are ok" in {
      val validator = ApplicationValidator(personalDetails, assistanceDetails, preferences, regions)
      validator.validateSchemes must be(true)
    }

    "return false if the chosen locations are have stem levels and the candidate doesn't have" in {
      val validator = ApplicationValidator(personalDetails, assistanceDetails, preferences, regionsNoStem)
      validator.validateSchemes must be(false)
    }
  }
}

object ApplicationValidatorSpec {
  def personalDetails = PersonalDetails("firstName", "lastName", "preferredName", new LocalDate(), aLevel = true, stemLevel = true)

  def assistanceDetails = AssistanceDetailsExamples.DisabilityGisAndAdjustments
  def preferences: Option[Preferences] = Some(
    Preferences(LocationPreference("London", "London", "Business", Some("IT")), Some(LocationPreference("London", "Reading", "Logistics", None)))
  )

  def regions = List(
    Region(
      "London",
      List(Location(
        "London",
        List(Framework("Business", CandidateHighestQualification(2)), Framework("IT", CandidateHighestQualification(3)))
      ), Location(
        "Reading",
        List(Framework("Logistics", CandidateHighestQualification(1)))
      ))
    ),
    Region(
      "Kent",
      List(Location(
        "Some Area",
        List(Framework("Business", CandidateHighestQualification(2)), Framework("IT", CandidateHighestQualification(3)))
      ), Location(
        "Other Area",
        List(Framework("Logistics", CandidateHighestQualification(1)))
      ))
    )
  )

  def regionsNoStem = List(
    Region(
      "London",
      List(Location(
        "London",
        List(Framework("Business", CandidateHighestQualification(2)))
      ), Location(
        "Reading",
        List(Framework("Logistics", CandidateHighestQualification(1)))
      ))
    ),
    Region(
      "Kent",
      List(Location(
        "Some Area",
        List(Framework("Business", CandidateHighestQualification(2)))
      ), Location(
        "Other Area",
        List(Framework("Logistics", CandidateHighestQualification(1)))
      ))
    )
  )
}
