/*
 * Copyright 2019 helloscala.com
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

package fusion.core.extension

import akka.actor.ExtendedActorSystem
import akka.actor.typed._
import akka.http.scaladsl.model.HttpHeader
import com.typesafe.scalalogging.StrictLogging
import fusion.core.FusionProtocol
import fusion.core.event.FusionEvents
import fusion.core.extension.impl.FusionCoreImpl
import fusion.core.setting.CoreSetting
import helloscala.common.Configuration

import scala.concurrent.Future

abstract class FusionCore extends Extension with StrictLogging {
  def name: String
  val setting: CoreSetting
  val events: FusionEvents
  val shutdowns: FusionCoordinatedShutdown
  val currentXService: HttpHeader
  def configuration: Configuration
  def fusionSystem: ActorSystem[FusionProtocol.Command]
  def classicSystem: ExtendedActorSystem
  def spawnUserActor[REF](behavior: Behavior[REF], name: String, props: Props): Future[ActorRef[REF]]

  def spawnUserActor[REF](behavior: Behavior[REF], name: String): Future[ActorRef[REF]] =
    spawnUserActor(behavior, name, Props.empty)
}

object FusionCore extends ExtensionId[FusionCore] {
  override def createExtension(system: ActorSystem[_]): FusionCore = new FusionCoreImpl(system)
  def get(system: ActorSystem[_]): FusionCore = apply(system)
}
