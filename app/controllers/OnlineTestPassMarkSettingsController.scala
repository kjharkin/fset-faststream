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

package controllers

import factories.UUIDFactory
import model.Commands.Implicits._
import model.exchange.passmarksettings.SchemePassMarkSettings
import play.api.libs.json.Json
import play.api.mvc.Action
import repositories._
import services.AuditService
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

object OnlineTestPassMarkSettingsController extends OnlineTestPassMarkSettingsController {
  val pmsRepository = passMarkSettingsRepository
  val fwRepository = frameworkRepository
  val auditService = AuditService
  val uuidFactory = UUIDFactory
}

trait OnlineTestPassMarkSettingsController extends BaseController {

  val pmsRepository: PassMarkSettingsRepository
  val auditService: AuditService
  val uuidFactory: UUIDFactory

  def createPassMarkSettings = Action.async(parse.json) { implicit request =>
    withJsonBody[SchemePassMarkSettings] { passMarkSettings => {
        val newVersionUUID = uuidFactory.generateUUID()
        for {
          createResult <- pmsRepository.create(passMarkSettings.copy(version = newVersionUUID))
        } yield {
          auditService.logEvent("PassMarkSettingsCreated", Map(
            "Version" -> newVersionUUID,
            "CreatedByUserId" -> passMarkSettings.createdByUser,
            "StoredCreateDate" -> passMarkSettings.createDate.toString
          ))
          Ok(Json.toJson(createResult))
        }
      }
    }
  }

  def getLatestVersion = Action.async { implicit request =>
    for {
      latestVersionOpt <- pmsRepository.tryGetLatestVersion
    } yield {
      latestVersionOpt.map {
        passMarkSettings => Ok(Json.toJson(passMarkSettings))
      } getOrElse {
        NotFound("Pass mark settings not found")
      }
    }
  }
}
