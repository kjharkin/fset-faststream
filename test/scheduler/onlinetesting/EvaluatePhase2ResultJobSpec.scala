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

package scheduler.onlinetesting

import model.exchange.passmarksettings.{ Phase2PassMarkSettings, Phase2PassMarkSettingsExamples }
import model.persisted.ApplicationReadyForEvaluation
import model._
import org.joda.time.{ DateTime, DateTimeZone }
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.Format
import testkit.UnitWithAppSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.concurrent.{ ExecutionContext, Future }

class EvaluatePhase2ResultJobSpec extends UnitWithAppSpec {
  implicit val now: DateTime = DateTime.now().withZone(DateTimeZone.UTC)

  "Scheduler execution" should {
    "evaluate applications ready for evaluation" in new TestFixture {
      when(mockEvaluateService.nextCandidatesReadyForEvaluation(any[Int])(any[Format[Phase2PassMarkSettings]]))
        .thenReturn(Future.successful(Some((apps.toList, passmark))))

      scheduler.tryExecute().futureValue

      assertAllApplicationsWereEvaluated(apps)
    }

    "evaluate all applications even when some of them fail" in new TestFixture {
      when(mockEvaluateService.nextCandidatesReadyForEvaluation(any[Int])(any[Format[Phase2PassMarkSettings]]))
        .thenReturn(Future.successful(Some((apps.toList, passmark))))
      when(mockEvaluateService.evaluate(apps(0), passmark)).thenReturn(Future.failed(new IllegalStateException("first application fails")))
      when(mockEvaluateService.evaluate(apps(5), passmark)).thenReturn(Future.failed(new Exception("fifth application fails")))

      val exception = scheduler.tryExecute().failed.futureValue

      assertAllApplicationsWereEvaluated(apps)
      exception mustBe a[IllegalStateException]
    }

    "do not evaluate candidate if none of them are ready to evaluate" in new TestFixture {
      when(mockEvaluateService.nextCandidatesReadyForEvaluation(any[Int])(any[Format[Phase2PassMarkSettings]]))
        .thenReturn(Future.successful(None))
      scheduler.tryExecute().futureValue

      verify(mockEvaluateService, never).evaluate(any[ApplicationReadyForEvaluation], any[Phase2PassMarkSettings])
    }
  }

  trait TestFixture {
    val mockEvaluateService = mock[EvaluateOnlineTestResultService[Phase2PassMarkSettings]]
    val profile = Phase2TestProfileExamples.profile
    val schemes = SelectedSchemesExamples.TwoSchemes
    val passmark = Phase2PassMarkSettingsExamples.passmark

    val apps = 1 to 10 map { id =>
      ApplicationReadyForEvaluation(s"app$id", ApplicationStatus.PHASE2_TESTS, ApplicationRoute.Faststream, isGis = false,
        profile.activeTests, None, None, schemes)
    }

    apps.foreach { app =>
      when(mockEvaluateService.evaluate(app, passmark)).thenReturn(Future.successful(()))
    }

    lazy val scheduler = new EvaluateOnlineTestResultJob[Phase2PassMarkSettings] {
      val phase = Phase.PHASE2
      val evaluateService = mockEvaluateService
      override val batchSize = 1
      override val lockId = "1"
      override val forceLockReleaseAfter: Duration = mock[Duration]
      override implicit val ec: ExecutionContext = mock[ExecutionContext]
      override val name = "test"
      override val initialDelay: FiniteDuration = mock[FiniteDuration]
      override val interval: FiniteDuration = mock[FiniteDuration]
      val config = EvaluatePhase2ResultJobConfig
    }

    def assertAllApplicationsWereEvaluated(apps: Seq[ApplicationReadyForEvaluation]) = apps foreach { app =>
      verify(mockEvaluateService).evaluate(app, passmark)
    }
  }
}
