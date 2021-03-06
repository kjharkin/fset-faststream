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

import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {
  override val appName = "fset-faststream"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object Versions {

  val microserviceBootstrapVersion  = "5.14.0"
  val ficus                         = "1.1.2"
  val playHealthVersion             = "2.1.0"
  val playConfigVersion             = "4.2.0"
  val hmrcScheduler                 = "4.0.0"
  val hmrcTestVersion               = "2.3.0"
  val playReactivemongoVersion      = "5.2.0"
  val logbackJsonLogger             = "3.1.0"

  val pegdown                       = "1.6.0"
  val mockito                       = "2.2.17"
  val scalatestplus                 = "1.5.1"
 }


private object AppDependencies {
  import Versions._
  import play.core.PlayVersion

  val compile = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo" % playReactivemongoVersion,
    "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLogger,
    "uk.gov.hmrc" %% "play-scheduling" % hmrcScheduler,
    "org.webjars" %% "webjars-play" % "2.3.0",
    "org.webjars" % "bootstrap" % "3.1.1",
    "org.webjars" % "jquery" % "1.11.0",
    "net.ceedubs" %% "ficus" % ficus,
    "org.yaml" % "snakeyaml" % "1.16",
    "com.jsuereth" %% "scala-arm" % "1.4",
    filters,
    cache,
    ws
  )

  abstract class TestDependencies(scope: String) {
    lazy val test: Seq[ModuleID] = Seq(
      "de.leanovate.play-mockws" %% "play-mockws" % "2.5.1" % "test",
      "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
      "org.mockito" % "mockito-core" % mockito % scope,
      "org.scalatestplus.play" %% "scalatestplus-play" % scalatestplus % scope,
      "org.pegdown" % "pegdown" % pegdown % scope,
      "com.typesafe.play" %% "play-test" % PlayVersion.current % scope excludeAll ExclusionRule(organization = "org.specs2")
    )
  }

  object Test extends TestDependencies("test")
  object IntegrationTest extends TestDependencies("it")

  def apply() = compile ++ Test.test ++ IntegrationTest.test

}

