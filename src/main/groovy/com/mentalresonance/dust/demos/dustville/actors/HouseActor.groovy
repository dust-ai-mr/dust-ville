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
import com.mentalresonance.dust.core.actors.ActorBehavior
import com.mentalresonance.dust.core.actors.ActorRef
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.core.msgs.PubSubMsg
import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsRequestMsg
import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsResponseMsg
import com.mentalresonance.dust.demos.dustville.msgs.town.HouseLoadMsg
import com.mentalresonance.dust.demos.dustville.msgs.town.PowerLoadMsg
import com.mentalresonance.dust.demos.dustville.msgs.StatsMsg
import com.mentalresonance.dust.demos.dustville.msgs.TimeSeriesEntry
import com.mentalresonance.dust.demos.dustville.msgs.town.WeatherStationMsg
import com.mentalresonance.dust.demos.dustville.utils.RingBuffer
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static com.mentalresonance.dust.demos.dustville.TownSetup.*
import java.util.concurrent.ThreadLocalRandom


@CompileStatic
@Slf4j
/**
 * A House has a base electrical load and an excess that depends on temperature (for heating or cooling). Temperature
 * depends on the external temperature (from the weather station) and how well insulated the house is
 * (a randomly assigned value)
 */
class HouseActor extends Actor {

	Double baseLoad, insulation, temp, spArea, spEfficiency, currentGen

	RingBuffer stats = new RingBuffer<TimeSeriesEntry<Double>>(96) // 24 hours

	ActorRef powerstationRef

	static Props props() {
		Props.create(HouseActor)
	}

	HouseActor() {
		ThreadLocalRandom rand = ThreadLocalRandom.current()
		insulation = rand.nextDouble(0.8d, 1.2d)
		baseLoad = rand.nextDouble(4d, 8d)
		// Until we get a weather station report
		temp = rand.nextDouble(70d, 85d)
		// Solar panels
		spArea = rand.nextDouble(150d, 250d)
		spEfficiency = rand.nextDouble(0.15d, 0.25d)

	}

	@Override
	void preStart() {
		powerstationRef = context.actorSelection("/user/powerstation")
		scheduleIn(new StatsMsg(), 15 * 1000L)
	}

	@Override
	ActorBehavior createBehavior() {
		(Serializable message) -> {
			switch(message) {
				case StatsMsg:
					stats.add(new TimeSeriesEntry<>(System.currentTimeMillis(), currentGen))
					scheduleIn(new StatsMsg(), STATS_SAMPLE_DT)
					break

				case GetTimeSeriesStatsRequestMsg:
					sender.tell(
						new GetTimeSeriesStatsResponseMsg(stats, "House ${self.name}", "Solar Power (KW)"),
						self
					)
					break
				default:
					super.createBehavior().onMessage(message)
			}
		}
	}
}
