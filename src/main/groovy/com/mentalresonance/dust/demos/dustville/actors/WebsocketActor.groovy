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

import com.google.gson.Gson
import com.mentalresonance.dust.core.actors.Actor
import com.mentalresonance.dust.core.actors.ActorBehavior
import com.mentalresonance.dust.core.actors.ActorRef
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.core.msgs.StartMsg
import com.mentalresonance.dust.demos.dustville.BirdsSetup
import com.mentalresonance.dust.demos.dustville.CarsSetup
import com.mentalresonance.dust.demos.dustville.TownSetup
import com.mentalresonance.dust.demos.dustville.WebsocketServer
import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsRequestMsg
import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsResponseMsg
import com.mentalresonance.dust.demos.dustville.msgs.birds.GetBirdsRequestMsg
import com.mentalresonance.dust.demos.dustville.msgs.birds.GetBirdsResponseMsg
import com.mentalresonance.dust.demos.dustville.msgs.town.DustvilleGazetteMsg
import com.mentalresonance.dust.demos.dustville.msgs.town.HouseLoadMsg
import com.mentalresonance.dust.demos.dustville.msgs.town.PowerLoadMsg
import com.mentalresonance.dust.demos.dustville.msgs.town.WeatherStationIntensityMsg
import com.mentalresonance.dust.demos.dustville.msgs.town.WeatherStationMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.DropCarMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.GetCarsRequestMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.GetCarsResponseMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.RoadClosureMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.TotalCarsMsg
import com.mentalresonance.dust.demos.dustville.utils.Vector3D
import fi.iki.elonen.NanoWSD
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import static com.mentalresonance.dust.demos.dustville.utils.HoodGeomtery.BASE_X
import static com.mentalresonance.dust.demos.dustville.utils.HoodGeomtery.BASE_Y
import static com.mentalresonance.dust.demos.dustville.utils.HoodGeomtery.BASE_Z
import static groovy.json.JsonOutput.toJson

@CompileStatic
@Slf4j
class WebsocketActor extends Actor {

	ActorRef town
	List<ActorRef> roads = [], hoods = []
	int numRoads, numCars, numCarResponses
	NanoWSD.WebSocket webSocket
	static WebsocketServer websocketServer

	static Props props() {
		Props.create(WebsocketActor)
	}

	@Override
	void preStart() {
		new TownSetup().init(context)
		new CarsSetup().init(context)
		new BirdsSetup().init(context)

		town = context.actorSelection("/user/town")

		hoods = BirdsSetup.hoods
		roads = CarsSetup.roads
		numRoads = roads.size()

		websocketServer = new WebsocketServer(9091, self)
	}

	@Override
	void preRestart(Throwable t) {
		if (websocketServer) {
			websocketServer.stop()
			websocketServer = new WebsocketServer(9091, self)
		} else
			log.warn "No websocketServer on restart !!!"
		become(createBehavior())
		log.info("Restarted: " + self.path + " because of " + t.getMessage());
	}

	/**
	 * We can't do anything until we have a web socket client, so our default behavior
	 * is to wait for the Websocket server itself to send us a with the session Websocket in it
	 * @return
	 */
	@Override
	ActorBehavior createBehavior() {
		(message) -> {
			switch (message) {

				case WebsocketMsg:
					WebsocketMsg msg = (WebsocketMsg) message
					webSocket = msg.webSocket
					become(startedBehavior())
					self.tell(new StartMsg(), self)
					break

				default:
					break
			}
		}
	}

	ActorBehavior startedBehavior() {
		(Serializable message) -> {
			switch(message) {
				case StartMsg:
					self.tell(new DoCarsMsg(), self)
					self.tell(new DoBirdsMsg(), self)
					self.tell(new DoSlowMsg(), self)
					break

				case DoCarsMsg:
					numCars = 0; numCarResponses = 0
					for (int i = 0; i < numRoads; ++i)
					{
						roads[i].tell(new GetCarsRequestMsg(), self)
					}
					scheduleIn(new DoCarsMsg(), 500)
					break

				case GetCarsResponseMsg:
					GetCarsResponseMsg cars = (GetCarsResponseMsg)message
					if (cars.coming || cars.going || cars.charging)
						webSocket.send(new Gson().toJson([
							type: "roads",
							value:[cars: cars, index: cars.roadId, density: cars.density, time: System.currentTimeMillis()]
						]))
					numCars += cars.coming.size() + cars.going.size()

					if (++numCarResponses == numRoads) {
						town.tell(new TotalCarsMsg(numCars), null)
						numCarResponses = 0
					}
					break

				case RoadClosureMsg:
					RoadClosureMsg msg = (RoadClosureMsg)message
					context.actorSelection("/user/${msg.name}").tell(msg, self)
					break

				case DoBirdsMsg:
					hoods.each { hood ->
						hood.tell(new GetBirdsRequestMsg(), self)
					}
					scheduleIn(new DoBirdsMsg(), 500)
					break

				case GetBirdsResponseMsg:
					GetBirdsResponseMsg birds = (GetBirdsResponseMsg)message
					Vector3D offset = new Vector3D(BASE_X, BASE_Y, BASE_Z)

					if (birds.birds.size() > 0) {
						webSocket.send(new Gson().toJson([
							type: "hood",
							value: [
								ts: System.currentTimeMillis(),
								name: birds.name,
								birds: birds.birds.collect {
									[name: it.name, position: it.position.plus(offset)]
								}
							]
						]))
					}
					break

				case DoSlowMsg:
					webSocket.send(new Gson().toJson([
						type: "time",
						value: new Date().toString()[4..18]
					]))
					scheduleIn(new DoSlowMsg(), 30 * 1000L)
					break

				case PowerLoadMsg:
					webSocket.send(toJson([type: "load", value:((PowerLoadMsg)message).load]))
					break

				case DropCarMsg:
					webSocket.send(toJson([type: "dropcar", value:((DropCarMsg)message).name]))
					break

				case HouseLoadMsg:
					HouseLoadMsg msg = (HouseLoadMsg)message
					webSocket.send(toJson([type: "houseload", value:[name: msg.name, load: msg.load]]))
					break

				case GetTimeSeriesStatsRequestMsg:
					GetTimeSeriesStatsRequestMsg msg = (GetTimeSeriesStatsRequestMsg)message
					context.actorSelection("/user/${msg.name}").tell(msg, self)
					break

				case GetTimeSeriesStatsResponseMsg:
					GetTimeSeriesStatsResponseMsg stats = (GetTimeSeriesStatsResponseMsg)message
					webSocket.send(toJson([
						type: "stats",
						value:[ts: stats.ringBuffer.contents(), title: stats.title, seriesName: stats.seriesName]
					]))
					break

				case CrashMsg:
					throw new Exception("I've crashed")
					break

				case DustvilleGazetteMsg:
					webSocket.send(toJson([
						type: "news",
						value: ((DustvilleGazetteMsg)message).news
					]))
					break

				default:
					super.createBehavior().onMessage(message)
			}
		}
	}

	static class DoCarsMsg implements Serializable {}

	static class DoBirdsMsg implements Serializable {}

	static class DoSlowMsg implements Serializable {}

	static class CrashMsg implements Serializable {}

	static class WebsocketMsg implements Serializable {

		NanoWSD.WebSocket webSocket

		WebsocketMsg(NanoWSD.WebSocket webSocket) {
			this.webSocket = webSocket
		}
	}
}

