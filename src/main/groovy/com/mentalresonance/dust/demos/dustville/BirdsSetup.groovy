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

import com.mentalresonance.dust.core.actors.ActorContext
import com.mentalresonance.dust.core.actors.ActorRef
import com.mentalresonance.dust.demos.dustville.actors.BirdActor
import com.mentalresonance.dust.demos.dustville.actors.HoodActor
import com.mentalresonance.dust.demos.dustville.objects.Bird
import com.mentalresonance.dust.demos.dustville.utils.Vector3D
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import static com.mentalresonance.dust.demos.dustville.utils.HoodGeomtery.*

import java.util.concurrent.ThreadLocalRandom

/**
 * Build a 20x20x20 cube of Neighborhoods. Each is a 10x10x10 unit volume and we identify it by the
 * 3D coordinates of its center. Each get assigned a HoodActor whose name is hood_x_y_z where x,y,z are the
 * center coordinates. Thus given a position inside this big cube we can easily compute which HoodActor is
 * going to be managing the space around it. Then set up some birds randomly in the cube.
 *
 * Each small cube is managed by an Actor whose name is hood_x_y_z where x, y, z are the coords of the
 * center of the small cube.
 */
@CompileStatic
@Slf4j
class BirdsSetup {

	static List<ActorRef> hoods = []

	void init(ActorContext context)
	{
		// Set up hoods
		for (int x = 0; x < SIZE_X; ++x) {
			for (int y = 0; y < SIZE_Y; ++y) {
				for (int z = 0; z < SIZE_Z; ++z) {
					int _x = 5 + x * 10, _y =  5 + y * 10,  _z = 5 + z * 10
					hoods <<context.actorOf(HoodActor.props(new Vector3D(_x, _y, _z)), "hood_${_x}_${_y}_${_z}")
				}
			}
		}

		// Populate a portion of the hoods with 10 birds each
		int birdIndex = 0;
		for (int x = 0; x < SIZE_X; ++x) {
			for (int y = 0; y < SIZE_Y; ++y) {
				for (int z = 0; z < SIZE_Z; ++z) {
					if (0 == ThreadLocalRandom.current().nextInt(0, 80)) {
						for (int i = 0; i < 10; ++i) {
							Vector3D position = new Vector3D(
								5 + x * 10 + ThreadLocalRandom.current().nextDouble(-5, 5),
								5 + y * 10 + ThreadLocalRandom.current().nextDouble(-5, 5),
								5 + z * 10 + ThreadLocalRandom.current().nextDouble(-5, 5),
							)
							Vector3D direction = new Vector3D(
								ThreadLocalRandom.current().nextDouble(-1, 1),
								ThreadLocalRandom.current().nextDouble(-1, 1),
								ThreadLocalRandom.current().nextDouble(-1, 1)
							)
							Bird bird = new Bird("bird_${birdIndex}" as String, position, direction, ThreadLocalRandom.current().nextDouble(10d, 40d))
							context.actorOf(BirdActor.props(bird), "bird_${birdIndex}")
							++birdIndex
						}
					}
				}
			}
		}
	}
}
