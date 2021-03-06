package repositories

import model.{ ApplicationRoute, SchemeType }
import model.report.CandidateProgressReportItem
import testkit.{ ShortTimeout, UnitWithAppSpec }

class NorthSouthIndicatorCSVRepositorySpec extends UnitWithAppSpec with ShortTimeout {

  import TestFixture._

  "North South Indicator Repository" should {
    "parse file with expected number of post code areas" in {
      val result = NorthSouthIndicatorCSVRepository.fsacIndicators
      result.size mustBe 121
    }
  }

  "calculateFsacIndicator" should {
    "return no indicator when in UK but no postcode" in {
      val result = NorthSouthIndicatorCSVRepository.calculateFsacIndicator(None, outsideUk = false)
      result mustBe None
    }
    "return default indicator when outside UK and no postcode" in {
      val result = NorthSouthIndicatorCSVRepository.calculateFsacIndicator(None, outsideUk = true)
      result mustBe Some(NorthSouthIndicatorCSVRepository.DefaultIndicator)
    }
    "return default indicator when in UK and no postcode match is found" in {
      val result = NorthSouthIndicatorCSVRepository.calculateFsacIndicator(Some("BOGUS3"), outsideUk = false)
      result mustBe Some(NorthSouthIndicatorCSVRepository.DefaultIndicator)
    }
    "return default indicator when in UK for an empty postcode " in {
      val result = NorthSouthIndicatorCSVRepository.calculateFsacIndicator(Some(""), outsideUk = false)
      result mustBe Some(NorthSouthIndicatorCSVRepository.DefaultIndicator)
    }
    "ignore postcode if outside UK and return the default indicator" in {
      val result = NorthSouthIndicatorCSVRepository.calculateFsacIndicator(Some("OX1 4DB"), outsideUk = true)
      result mustBe Some(NorthSouthIndicatorCSVRepository.DefaultIndicator)
    }
    "return London for Oxford postcode" in {
      val result = NorthSouthIndicatorCSVRepository.calculateFsacIndicator(Some("OX1 4DB"), outsideUk = false)
      result mustBe Some("London")
    }
    "return Newcastle for Edinburgh postcode" in {
      val result = NorthSouthIndicatorCSVRepository.calculateFsacIndicator(Some("EH1 3EG"), outsideUk = false)
      result mustBe Some("Newcastle")
    }
    "return London even when postcode is lowercase" in {
      val result = NorthSouthIndicatorCSVRepository.calculateFsacIndicator(Some("ec1v 3eg"), outsideUk = false)
      result mustBe Some("London")
    }
  }

  "calculateFsacIndicatorForReports" should {
    "returns an indicator if the candidate is a faststream with no applicationRoute" in {
      val result = NorthSouthIndicatorCSVRepository.calculateFsacIndicatorForReports(Some("EH1 3EG"), CandidateProgressReportItemFaststream)
      result mustBe Some("Newcastle")
    }
    "returns an indicator if the candidate is a faststream with applicationRoute" in {
      val result = NorthSouthIndicatorCSVRepository.calculateFsacIndicatorForReports(Some("EH1 3EG"), CandidateProgressReportItemFaststream)
      result mustBe Some("Newcastle")
    }
    "returns no indicator if the candidate is a Edip" in {
      val result = NorthSouthIndicatorCSVRepository.calculateFsacIndicatorForReports(Some("EH1 3EG"), CandidateProgressReportItemEdip)
      result mustBe None
    }
    "returns no indicator if the candidate is in a registered status" in {
      val result = NorthSouthIndicatorCSVRepository.calculateFsacIndicatorForReports(Some("EH1 3EG"), CandidateProgressReportItemRegistered)
      result mustBe None
    }
  }
}

object TestFixture {

  val CandidateProgressReportItemFaststreamBase = CandidateProgressReportItem("user123", "app123", Some("submitted"),
    List(SchemeType.DiplomaticService, SchemeType.GovernmentOperationalResearchService), Some("Yes"),
    Some("No"), Some("No"), None, Some("No"), Some("No"), Some("No"), Some("No"), Some("No"), Some("No"), Some("1234567"),
    None, ApplicationRoute.Faststream)
  val CandidateProgressReportItemFaststream = CandidateProgressReportItemFaststreamBase.copy(applicationRoute = ApplicationRoute.Faststream)
  val CandidateProgressReportItemEdip = CandidateProgressReportItemFaststreamBase.copy(applicationRoute = ApplicationRoute.Edip)
  val CandidateProgressReportItemRegistered = CandidateProgressReportItemFaststreamBase.copy(progress = Some("registered"))

}
