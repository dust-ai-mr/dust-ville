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
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.core.msgs.StartMsg
import com.mentalresonance.dust.demos.dustville.CarsSetup
import com.mentalresonance.dust.demos.dustville.msgs.cars.AtIntersectionMsg
import com.mentalresonance.dust.demos.dustville.msgs.cars.TotalCarsMsg
import com.mentalresonance.dust.demos.dustville.msgs.town.DustvilleGazetteMsg
import com.mentalresonance.dust.demos.dustville.objects.Car
import com.mentalresonance.dust.demos.dustville.objects.Point
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.ThreadLocalRandom

/**
 * The town Actor adds cars to random roads. The Car's parameters (charge, speed etc) are given random values
 * within plausible ranges. We have different ranges of numbers of cars on the road based on time. Also we do not
 * track the total number of cards on the road here, rather we let the UI tell us via the websocket
 * and the periodic {@link TotalCarsMsg}. We could, of course, have the websocket tell us everytime a Car is taken
 * off the roads (at a cul-de-sac) but this way is a little more efficient.
 */
@CompileStatic
@Slf4j
class TownActor extends Actor {

	boolean canAddCar = true
	int carIndex = 0, maxCars = 1100, minCars = 800
	ThreadLocalRandom rand = ThreadLocalRandom.current()

	List<List<Integer>> roads = CarsSetup.roadDefs

	static Props props() {
		Props.create(TownActor)
	}

	@Override
	void preStart() {
		self.tell(new StartMsg(), self)
		scheduleIn(new AddCarMsg(), 2000)
	}

	@Override
	ActorBehavior createBehavior() {
		(Serializable message) -> {
			switch(message) {
				case AddCarMsg:
					if (canAddCar) {
						Car car = new Car(carIndex)

						// Choose a random road
						int road = rand.nextInt(0, roads.size())
						// Coords of road start
						List coord = roads[road][0..1]

						// Set up parameters of the car somewhat randomly
						car.chargeCapacity = rand.nextInt(130, 160)
						car.chargeRemaining = car.chargeCapacity
						car.speed =  rand.nextInt(1, 4)

						// Position car on Road
						car.point = new Point(coord[0] as double, coord[1] as double)

						// Create an Actor for the Car with initial args the Car itself
						car.carRef = context.actorOf(CarActor.props(car), "car$carIndex")

						// And tell the car it is at an Intersection
						car.carRef.tell(new AtIntersectionMsg(car), null)
						++carIndex
					}
					// And try to add a car again in some random future
					scheduleIn(new AddCarMsg(), rand.nextInt(150, 1000))
					break

				// Sent by UI to Websocket and thence to us
				case TotalCarsMsg:
					int totalCars = ((TotalCarsMsg)message).totalCars
					if (totalCars > maxCars)
						canAddCar = false
					if (totalCars < minCars)
						canAddCar = true
					break

				// Update car bounds
				case StartMsg:
					int hour = new Date().hours
					if (hour < 7 || hour > 21) { // Night
						maxCars = 500
						minCars = 400
					}
					else if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour < 19)) { // Rush hour
						maxCars = 1100
						minCars = 800
					}
					else { // Rest of day
						maxCars = 800
						minCars = 500
					}
					// Check again in an hour
					scheduleIn(new StartMsg(), 60 * 60 * 3600)
					break

				default:
					super.createBehavior().onMessage(message)
			}
		}
	}

	static class AddCarMsg implements Serializable {}
}
