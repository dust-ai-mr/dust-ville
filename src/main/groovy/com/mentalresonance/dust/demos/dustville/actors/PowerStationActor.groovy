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

import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsRequestMsg
import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsResponseMsg
import com.mentalresonance.dust.demos.dustville.msgs.town.PowerLoadMsg
import com.mentalresonance.dust.demos.dustville.msgs.StatsMsg
import com.mentalresonance.dust.demos.dustville.msgs.TimeSeriesEntry
import com.mentalresonance.dust.demos.dustville.utils.RingBuffer

import static com.mentalresonance.dust.demos.dustville.TownSetup.*
import com.mentalresonance.dust.core.actors.ActorBehavior
import com.mentalresonance.dust.core.actors.ActorRef
import com.mentalresonance.dust.core.actors.Props
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Simply track total load on the Power station.
 */
@CompileStatic
@Slf4j
class PowerStationActor extends Actor {

	Map<ActorRef, Double> loads = [:] // Actor with load e.g. House, Intersection -> amount of load
	double megaLoad = 0d

	RingBuffer stats = new RingBuffer<TimeSeriesEntry<Double>>(96) // 24 hours

	static Props props() {
		Props.create(PowerStationActor)
	}

	@Override
	void preStart() {
		scheduleIn(new StatsMsg(), STATS_SAMPLE_DT)
	}

	@Override
	ActorBehavior createBehavior() {
		(message) -> {
			switch (message) {
					/**
					 * Update load from some sender
					 */
				case PowerLoadMsg:
					Double load = ((PowerLoadMsg)message).load
					if (load != null) {
						loads[sender] = load
					}
					megaLoad = (loads.values().sum() as double + megaLoad) / 2.0d
					context.actorSelection('/user/services/websocket').tell(new PowerLoadMsg(megaLoad), self)
					break

					/**
					 * Add stats periodically so we can display them
					 */
				case StatsMsg:
					stats.add(new TimeSeriesEntry<>(System.currentTimeMillis(), megaLoad))
					scheduleIn(new StatsMsg(), STATS_SAMPLE_DT)
					break

					/**
					 * Get stats when asked
					 */
				case GetTimeSeriesStatsRequestMsg:
					sender.tell(
						new GetTimeSeriesStatsResponseMsg(stats, "Power Station Load", "Kilowatts"),
						self
					)
					break
			}
		}
	}

}
