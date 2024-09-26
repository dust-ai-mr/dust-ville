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
import com.mentalresonance.dust.core.msgs.StartMsg
import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsRequestMsg
import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsResponseMsg
import com.mentalresonance.dust.demos.dustville.msgs.town.PowerLoadMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.RegisterRoadMsg
import com.mentalresonance.dust.demos.dustville.msgs.StatsMsg
import com.mentalresonance.dust.demos.dustville.msgs.TimeSeriesEntry
import com.mentalresonance.dust.demos.dustville.msgs.cars.CarChargingMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.CarFinishedChargingMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.GetRoadsMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.RoadClosureMsg
import com.mentalresonance.dust.demos.dustville.utils.RingBuffer
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static com.mentalresonance.dust.demos.dustville.TownSetup.*

/**
 * Manages collection of roads ending at the same point and charging load. Responds to queries from Cars
 * to return the list of Roads so they can choose one.
 *
 * EVs charge at an intersection so we track how much power is being drained and pass this info off to the
 * power plant.
 */
@CompileStatic
@Slf4j
class IntersectionActor extends Actor {

	int x, y

	Double chargeLoad  = 0d

	/**
	 * Time series of charging load at this intersection
	 */
	RingBuffer stats = new RingBuffer<TimeSeriesEntry<Double>>(96) // 24 hours sampling at 15 seconds

	Map<ActorRef, Road> roads = [:]

	/**
	 * Create intersection @ x,y
	 * @param x
	 * @param y
	 * @return
	 */
	static Props props(int x, int y) {
		Props.create(IntersectionActor, x, y)
	}

	IntersectionActor(int x, int y) {
		this.x = x
		this.y = y
	}

	@Override
	void preStart() {
		scheduleIn(new StartMsg(), 30 * 1000)
		scheduleIn(new StatsMsg(), 15 * 1000)
	}


	@Override
	ActorBehavior createBehavior() {
		(Serializable message) -> {
			switch(message) {
					/**
					 * Build process -- a Road meets this intersection
					 */
				case RegisterRoadMsg:
					roads[sender] = new Road(closed: false, ref: sender)
					break
					/**
					 * Get all open roads at this intersection
					 */
				case GetRoadsMsg:
					((GetRoadsMsg)message).roads =
						roads.values().findAll { ! it.closed }.collect { it.ref }
					sender.tell(message, self)
					break
					/**
					 * Update Road closed state
					 */
				case RoadClosureMsg:
					 roads[sender].closed = ((RoadClosureMsg)message).closed
					break
					/**
					 * New car is charging
					 */
				case CarChargingMsg:
					chargeLoad += ((CarChargingMsg)message).car.chargeCapacity
					break
					/**
					 * Car has finished charging
					 */
				case CarFinishedChargingMsg:
					chargeLoad -= ((CarFinishedChargingMsg)message).car.chargeCapacity
					break
					/**
					 * Periodically update power station with how much power cars charging at me
					 */
				case StartMsg:
					context.actorSelection("/user/powerstation").tell(new PowerLoadMsg(chargeLoad), sender)
					scheduleIn(new StartMsg(), 30 * 1000)
					break
					/**
					 * Update stats
					 */
				case StatsMsg:
					stats.add(new TimeSeriesEntry<>(System.currentTimeMillis(), chargeLoad))
					scheduleIn(new StatsMsg(), STATS_SAMPLE_DT)
					break
					/**
					 * Get stats
					 */
				case GetTimeSeriesStatsRequestMsg:
					sender.tell(
						new GetTimeSeriesStatsResponseMsg(stats, "Total charging load for ${self.name}", "Kilowatts"),
						self
					)
					break

				default: super.createBehavior().onMessage(message)
			}
		}
	}

	private static class Road {
		boolean closed
		ActorRef ref
	}
}
