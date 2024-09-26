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
import com.mentalresonance.dust.core.actors.Cancellable
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsRequestMsg
import com.mentalresonance.dust.demos.dustville.msgs.GetTimeSeriesStatsResponseMsg
import com.mentalresonance.dust.demos.dustville.msgs.StatsMsg
import com.mentalresonance.dust.demos.dustville.msgs.TimeSeriesEntry
import com.mentalresonance.dust.demos.dustville.msgs.cars.AtIntersectionMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.CarEnteringMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.CarFinishedChargingMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.GetCarsRequestMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.GetCarsResponseMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.RoadClosureMsg
import com.mentalresonance.dust.demos.dustville.objects.Car
import com.mentalresonance.dust.demos.dustville.objects.Point
import com.mentalresonance.dust.demos.dustville.utils.RingBuffer
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.ThreadLocalRandom

import static com.mentalresonance.dust.demos.dustville.TownSetup.*

/**
 * Once a car enters a road at a certain speed the road controls the speed of the car on it. The car however decides
 * which way it want to turn at intersections.
 *
 * Roads meet at common points which are managed by a {@link IntersectionActor} however we usurp the IntersectionActor
 * if a car is about to enter me and needs charging - in which case I add it to a list of charging cars
 * and move it out to a charging spot
 */
@Slf4j
@CompileStatic
class RoadActor extends Actor {

	/**
	 * 	A Road has a start and an end location
 	 */
	private int startX, startY, endX, endY, deltaX, deltaY, id

	/**
	 * 	A bit of math computes the length of the road and its angle in the place
 	 */
	private double length, angle

	/**
	 * How often to update the state of the road
	 */
	private long intervalT = 100, lastT = 0, deltaT

	/**
	 * If a road is closed cars are denied entry
	 */
	private boolean closed = false

	/**
	 * The road tracks the number of cars on it
	 */
	private int carCount = 0

	/**
	 * Last 96 buckets of statistics
	 */
	private RingBuffer stats = new RingBuffer<TimeSeriesEntry<Integer>>(96)

	/**
	 * 	At any given time it has cars on it - single lane road in each direction going = start -> end, coming is end -> start
 	 */
	LinkedList<Car> going = [], coming = [], charging = []

	Cancellable update

	/**
	 *
	 * @param ends [startx, starty, ends, endy]
	 * @param id - integer id from setup
	 * @return
	 */
	static Props props(List<Integer> ends, int id) {
		Props.create(RoadActor, ends[0], ends[1], ends[2], ends[3], id)
	}

	RoadActor(int startX, int startY, int endX, int endY, int id) {
		this.startX = startX
		this.startY = startY
		this.endX = endX
		this.endY = endY
		this.id = id

		deltaX = endX - startX
		deltaY = endY - startY
		length = Math.sqrt((double)((startX-endX)**2 + (startY - endY)**2))

		angle = Math.asin((double)deltaY / length)

		if (angle < 0) angle = 2 * Math.PI + angle
	}

	@Override
	void preStart() {
		update = scheduleIn(new UpdateMsg(), intervalT)
		scheduleIn(new StatsMsg(), 15 * 1000)
	}


	@Override
	ActorBehavior createBehavior() {
		(Serializable message) -> {
			switch(message) {
				case CarEnteringMsg:
					CarEnteringMsg msg = (CarEnteringMsg)message
					/**
					 * If I'm charging recall where I was and put me out to charge. Although I have a speed I will not be moved
					 * since I am charging.
					 */
					if (msg.car.charging) {
						// I'm charging so save where I was and move me to charging spot
						msg.car.oldPoint = msg.car.point
						msg.car.point = chargePoint(msg.car.point.x, msg.car.point.y, 30.0d)
						charging << msg.car
					}
					else {
						/**
						 * I've entered the road. Figure out if I'm coming or going and put me in the correct lane
						 */
						++carCount
						if (msg.car.point.x == startX && msg.car.point.y == startY) {
							msg.car.isGoing = true
							going << msg.car
						}
						else if (msg.car.point.x == endX && msg.car.point.y == endY) {
							msg.car.isGoing = false
							coming << msg.car
						}
						else
							log.info "!!! At intersection speed = ${msg.car.speed} but not at an end ${msg.car.point.x},${msg.car.point.y} ${startX},${startY} ${endX},${endY}"

						/**
						 * Put me in the right lane - then set the Angle I will travel at and tell the car it
						 * is at the start of the road
						 */
						if (! msg.car.charging) {
							msg.car.point = getPoint(msg.car.isGoing, 0)
						}
						msg.car.distance = 0
						msg.car.angle = angle
					}
					break

				/**
				 * Restore my position and re-enter - I will not be charging
				 */
				case CarFinishedChargingMsg:
					CarFinishedChargingMsg msg = (CarFinishedChargingMsg)message
					msg.car.point = msg.car.oldPoint
					charging.remove(msg.car)
					self.tell(new CarEnteringMsg(msg.car), self)
					break

					/**
					 * Update the state of the road
					 */
				case UpdateMsg:
					deltaT = (lastT > 0) ? System.currentTimeMillis() - lastT : intervalT
					// Are we laboring ? -- diagnostic
					if (deltaT > 2 * intervalT) {
						log.info "${self.name} dt=$deltaT intervalT = $intervalT mbox=${mailboxSize()}"
					}
					/**
					 * Process each direction separately
					 */
					[going, coming].each {stream ->
						Car inFront = null
						List<Car> toRemove = []

						/**
						 * For each car figure out how to move me
						 */
						stream.each {
							// Update position if not charging
							if (! it.charging) {
								it.distance = (float) (it.distance + it.speed * deltaT / 100.0f)
								it.point = getPoint(it.isGoing, it.distance)

								/**
								 * If I'm close to the intersection put me there and tell me.
								 * Also remove me from this road.
								 */
								if (length - it.distance < 2) {
									it.point.x = it.isGoing ? endX : startX
									it.point.y = it.isGoing ? endY : startY
									it.chargeRemaining = (float) (it.chargeRemaining - 0.02 * length)
									it.carRef.tell(new AtIntersectionMsg(it), self)
									toRemove << it
								}
								else {
									if (inFront) {
										/**
										 * If I'm close to the next car in my stream match its speed
										 */
										if (inFront.distance - it.distance < 15) {
											it.speed = inFront.speed
										}
									}
									inFront = it
								}
							}
						}
						toRemove.each {
							stream.remove(it)
						}
						toRemove = []
					}
					lastT = System.currentTimeMillis()
					update = scheduleIn(new UpdateMsg(), intervalT)
					break

				case GetCarsRequestMsg:
					// Don't know what is going to be done with the cars so clone them
					GetCarsResponseMsg resp = new GetCarsResponseMsg()
					resp.roadId = id
					resp.closed = closed

					[coming, going].each { stream ->
						boolean isGoing = stream.is(going)
						List<Car> cars = stream.collect {it.clone()}
						if (isGoing)
							resp.going = cars
						else
							resp.coming = cars
					}
					resp.charging = charging.collect { it.clone() }
					int totalCars = going.size() + coming.size()
					resp.density = totalCars / length
					sender.tell(resp, self)
					break

				// Road closure status has changed -- inform my two intersections
				case RoadClosureMsg:
					closed = ((RoadClosureMsg)message).closed
					actorSelection("/user/intersection_${startX}_$startY").tell( message, self)
					actorSelection("/user/intersection_${endX}_$endY").tell( message, self)
					log.info "${self.name} closed = $closed"
					break

				case StatsMsg:
					stats.add(new TimeSeriesEntry<>(System.currentTimeMillis(), carCount))
					carCount = 0
					scheduleIn(new StatsMsg(), STATS_SAMPLE_DT)
					break

				case GetTimeSeriesStatsRequestMsg:
					sender.tell(
						new GetTimeSeriesStatsResponseMsg(stats, "Cars enetring ${self.name} in time period", "Car count"),
						self
					)
					break

				default: super.createBehavior().onMessage(message)
			}
		}
	}

	private Point getPoint(boolean isGoing, double distance) {
		double ratio, x, y

		ratio = (length - distance) / length

		x = isGoing ? endX - ratio * deltaX : startX  + ratio * deltaX
		y = isGoing ? endY - ratio * deltaY : startY  + ratio * deltaY
		// Traffic lanes !!
		if (! isGoing) {
			x = x - 4 * Math.sin(angle)
			y = y - 4 * Math.cos(angle)
		}
		return new Point(x, y)
	}


	Point chargePoint(double x, double y, double radius) {
		double theta = 	ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI)
		new Point(x + radius * Math.cos(theta), y + radius * Math.sin(theta))
	}

	private static class UpdateMsg implements Serializable {}
}
