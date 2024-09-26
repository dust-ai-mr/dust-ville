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

import com.google.gson.Gson
import com.mentalresonance.dust.core.actors.ActorRef
import com.mentalresonance.dust.demos.dustville.actors.WebsocketActor
import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsRequestMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.RoadClosureMsg
import com.mentalresonance.dust.demos.dustville.msgs.town.WeatherStationIntensityMsg
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class WebsocketServer extends NanoWSD {

	static ActorRef self;

	WebsocketServer(int port, ActorRef self) throws IOException {
		super(port)
		this.self = self
		start(5*60*1000, false)
	}

	@Override
	protected WebSocket openWebSocket(IHTTPSession ihttpSession) {
		WebSocket webSocket = new WsdSocket(ihttpSession)
		self.tell(new WebsocketActor.WebsocketMsg(webSocket), null)

		return webSocket;
	}

	static class WsdSocket extends NanoWSD.WebSocket
	{
		WsdSocket(NanoHTTPD.IHTTPSession handshakeRequest)
		{
			super(handshakeRequest)
		}

		@Override
		protected void onOpen() {
			log.info "WS OPEN"
		}

		@Override
		protected void onClose(NanoWSD.WebSocketFrame.CloseCode closeCode, String s, boolean b) {
			log.info "WS CLOSE"
			self.tell(new com.mentalresonance.dust.demos.dustville.actors.WebsocketActor.CrashMsg(), null)
		}

		@Override
		protected void onMessage(NanoWSD.WebSocketFrame webSocketFrame) {
			try {
				// log.info "WS onMessage: ${webSocketFrame.toString()}"
				Map msg = new Gson().fromJson(webSocketFrame.textPayload, HashMap)
				switch(msg.type)
				{
					case "setup":
						send(new Gson().toJson([
							type: "setup",
							roads: CarsSetup.roadDefs,
							intersections: CarsSetup.intersections.keySet().toList(),
							numhouses: TownSetup.NUM_HOUSES
						]))
						break

					case "ping":
						break

					case "closure":
						self.tell(new RoadClosureMsg(closed: msg.closed, name: msg.name), null)
						break

						/**
						 * value is 0 <= 1. If not 1 then intercept weather to modify light intensity and temperature
						 * (i.e. we are faking out 'night"
						 */
					case "daylight":
						self.tell(new WeatherStationIntensityMsg((float)msg.value), null)
						break

					case "stats":
						self.tell(new GetTimeSeriesStatsRequestMsg((String)msg.name), null)
						break
				}
			} catch (IOException e) {
				// handle
			}
		}

		@Override
		protected void onPong(NanoWSD.WebSocketFrame webSocketFrame) {
			log.info "WS ON PONG"
		}

		@Override
		protected void onException(IOException e) {
			e.printStackTrace()
			log.info "${e.message}"
		}
	}
}
