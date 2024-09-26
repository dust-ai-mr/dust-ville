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

package com.mentalresonance.dust.demos.dustville

import com.mentalresonance.dust.core.actors.ActorSystem
import com.mentalresonance.dust.demos.dustville.actors.ServicesActor
import groovy.util.logging.Slf4j


@Slf4j
/**
 * Start:
 *	A services Actor - this manages all the other Actors
 * 	A webserver on 9090 to vend the UI

 */
class Main {

	static int port = 9090
	static ActorSystem dustvilleActorSystem
	static ConfigObject config
	static String WEATHER_STATION_KEY = 'WEATHER_STATION_KEY'

	static void main(String[] args) {
		String configText = Main.classLoader.getResourceAsStream('Config.groovy').readLines().join('\n')
		config = new ConfigSlurper().parse(configText)

		if (System.getProperty(WEATHER_STATION_KEY))
			config[WEATHER_STATION_KEY] = System.getProperty(WEATHER_STATION_KEY)
		else if (System.getenv(WEATHER_STATION_KEY))
			config[WEATHER_STATION_KEY] = System.getenv(WEATHER_STATION_KEY)

		dustvilleActorSystem = new ActorSystem('dustville')
		dustvilleActorSystem.context.actorOf(ServicesActor.props(), "services")
		new WebServer(port)
	}
}
