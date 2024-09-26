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

import com.mentalresonance.dust.core.actors.*
import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsRequestMsg
import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsResponseMsg
import com.mentalresonance.dust.demos.dustville.msgs.StatsMsg
import com.mentalresonance.dust.demos.dustville.msgs.TimeSeriesEntry
import com.mentalresonance.dust.demos.dustville.msgs.cars.AtIntersectionMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.CarChargingMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.CarEnteringMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.CarFinishedChargingMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.DropCarMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.GetRoadsMsg
import com.mentalresonance.dust.demos.dustville.objects.Car
import com.mentalresonance.dust.demos.dustville.utils.RingBuffer
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.ThreadLocalRandom

import static com.mentalresonance.dust.demos.dustville.TownSetup.*

@CompileStatic
@Slf4j
class CarActor extends Actor {

	Car car
	Cancellable sampleMsg, statsMsg

	/**
	 * Our stats are average speed so we independently sample our car's speed and average
	 */
	private RingBuffer stats = new RingBuffer<TimeSeriesEntry<Double>>(96,
		(val) -> {
			TimeSeriesEntry<Double> tse = (TimeSeriesEntry<Double>) val
			return new TimeSeriesEntry<Double>(tse.ts, 20 * tse.value)
		}),
		speedSamples = new RingBuffer<TimeSeriesEntry<Double>>(12)

	ActorRef currentRoad = null

	static Props props(Car car) {
		Props.create(CarActor, car)
	}

	CarActor(Car car) {
		this.car = car
	}

	@Override
	void preStart() {
		sampleMsg = scheduleIn(new SampleSpeedMsg(), 15 * 1000)
		statsMsg = scheduleIn(new StatsMsg(), 15 * 1000)
	}

	/**
	 * Basically handle intersection hand offs -- learn about the roads and the intersection and choose one.
	 * Check to see if we need to recharge. If we do set our speed to 0 and move us to a charging spot on the intersection
	 * but choose our next road so we will still get reported as existing. Also get our glow on.
	 * When we have spent enough time charging chose a random speed and our road will take over
	 * @return
	 */
	@Override
	ActorBehavior createBehavior() {
		(Serializable message) -> {
			switch(message)
			{
				case AtIntersectionMsg:
					car = ((AtIntersectionMsg)message).car

					actorSelection("/user/intersection_${(int)car.point.x}_${(int)car.point.y}").tell( new GetRoadsMsg(), self)
					break

				case GetRoadsMsg:
					GetRoadsMsg msg = (GetRoadsMsg)message
					// No U turns unless no choice
					List<ActorRef> roads = currentRoad ? msg.roads - currentRoad : msg.roads
				/**
				 * Chose a road at random and set random speed. If I need to charge set my flag and
				 * tell the intersection and start charging. In either case tell the new road
				 * I am here.
				 *
				 * If I am a cul-de-sac then take the car off the road
				 */
					if (roads != []) {
						currentRoad = roads[ThreadLocalRandom.current().nextInt(0, roads.size())]
						car.speed = ThreadLocalRandom.current().nextInt(1, 4)
						if (car.chargeRemaining < car.chargeCapacity * 0.1) {
							car.charging = true
							actorSelection("/user/intersection_${(int)car.point.x}_${(int)car.point.y}").tell( new CarChargingMsg(car), self)
							scheduleIn(new DoneChargingMsg(), car.chargeCapacity as Long * 1000L)
						}
						currentRoad.tell(new CarEnteringMsg(car), self)
					}
					else {
						sampleMsg.cancel()
						statsMsg.cancel()
						context.actorSelection('/user/services/websocket').tell(new DropCarMsg(self.name), self)
						context.stop(self)
					}
					break

				case DoneChargingMsg:
					car.charging = false
					car.chargeRemaining = car.chargeCapacity
					actorSelection("/user/intersection_${(int)car.oldPoint.x}_${(int)car.oldPoint.y}").tell( new CarFinishedChargingMsg(car), self)
					currentRoad.tell(new CarFinishedChargingMsg(car), self)
					break

				case SampleSpeedMsg:
					speedSamples.add(car.speed)
					sampleMsg = scheduleIn(new SampleSpeedMsg(), 15)
					break

				case StatsMsg:
					Double[] speeds = speedSamples.contents() as Double[]
					int size = speeds.size()
					if (size) {
						stats.add(new TimeSeriesEntry<>(System.currentTimeMillis(), speeds.sum() as Double / size))
						// log.info "${self.name} avg speed = ${speeds.sum() as Double / size}"
					}
					statsMsg = scheduleIn(new StatsMsg(), STATS_SAMPLE_DT)
					break

				case GetTimeSeriesStatsRequestMsg:
				/**
				 * Speed is an internal thing - so x10 for a 'realistic' value to display
				 */
					sender.tell(
						new GetTimeSeriesStatsResponseMsg(stats, "Car ${self.name} speed in time period", "Speed"),
						self
					)
					break

				default: super.createBehavior().onMessage(message)
			}
		}
	}

	static class DoneChargingMsg implements Serializable {}

	static class SampleSpeedMsg implements Serializable {}
}


