/*
 *
 *  Copyright 2024 Alan Littleford
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package com.mentalresonance.dust.demos.dustville.actors

import com.mentalresonance.dust.core.actors.Actor
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.core.actors.SupervisionStrategy
import groovy.util.logging.Slf4j

@Slf4j
class ServicesActor extends Actor {

	static Props props() {
		Props.create(ServicesActor)
	}

	@Override
	void preStart() {
		/**
		 * If a service boinks restart it - this can happen with the Weather API which can return bogus values sometimes.
		 * So we follow the Erlang 'Let it crash' technique by changing our supervision strategy to Restart
 		 */
		supervisor = new SupervisionStrategy(SupervisionStrategy.SS_RESTART, SupervisionStrategy.MODE_ONE_FOR_ONE)
		/**
		 * Web socket interfaces between the Websocket server (which it creates) and the Actor world.
		 * He also starts up the town and all it contains
		 */
		actorOf(WebsocketActor.props(), 'websocket')
	}
}
