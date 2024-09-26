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

package com.mentalresonance.dust.demos.dustville.objects

import com.mentalresonance.dust.core.actors.ActorRef
import groovy.transform.CompileStatic

@CompileStatic
class Car {

	int speed, id

	ActorRef carRef
	float distance = 0.0f // Travelled along the road from entrance so is always increasing within a road
	double angle		  // Of current road
	Point point			  // xy coords
	Point oldPoint 		  // Where I was - useful to restore position after charging
	float chargeCapacity, chargeRemaining
	boolean charging = false, isGoing // Which lane am I in - coming or going ?

	Car(int id) {
		this.id = id
	}

	Car clone() {
		Car car = new Car(id)
		car.speed = speed
		car.carRef = carRef
		car.distance = distance
		car.angle = angle
		car.point = new Point(point.x, point.y)
		car.chargeCapacity = chargeCapacity
		car.chargeRemaining = chargeRemaining
		car.charging = charging
		car.isGoing = isGoing
		car
	}
}
