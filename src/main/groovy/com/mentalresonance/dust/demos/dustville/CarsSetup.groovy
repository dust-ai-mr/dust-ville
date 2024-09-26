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
import com.mentalresonance.dust.demos.dustville.actors.IntersectionActor
import com.mentalresonance.dust.demos.dustville.actors.RoadActor
import com.mentalresonance.dust.demos.dustville.msgs.cars.RegisterRoadMsg
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Setup roads and intersections. Intersections are defined by the coords of the ends of the roads.
 * Crate Actors for both - roads are just named numerically whereas the name of the intersection Actor
 * includes x and y coords and the intersection.
 *
 */
@CompileStatic
@Slf4j
class CarsSetup {
	static List<ActorRef> roads = []

	// [startx, starty, endx, endy]
	public static List<List<Integer>> roadDefs = [
	    [0, 0, 500, 0],
		[0, 0, 0, 500],
		[500, 0, 500, -500],
		[500, 0, 500, 500],
		[500, 500, -500, 500],
		[0, -500, 0, 0],
		[0, 0, -500, -600],
		[-500, -600, -500, -130],
		[0, -500, -500, -600],
		[0, 0, -500, -130],
		[0, -500, 500, -500],
		[500, -500, 0, 0],
		[-500, -130, -500, 500],
		[ 0, 0, -500, 500],
		[250, 250, 0, 500],

	]

	/**
	 * Maps x,y of intersection to all those Road Actors whose road ends at the intersection.
	 */
	public static Map<List<Integer>, List<ActorRef>> intersections = [:]

	void init(ActorContext context)
	{
		int i = 0;

		// Create roads
		roadDefs.each {
			ActorRef road = context.actorOf(RoadActor.props(it, i), "road$i")
			roads << road

			++i
			List key1 = it[0..1], key2 = it[2..3]

			[key1, key2].each {
				List<ActorRef> refs = intersections[it] ?: []
				refs << road
				intersections[it] = refs
			}
		}

		// For all road Actors at each intersection register them with the Intersection Actor
		intersections.each {
			int x = it.key[0], y = it.key[1]
			ActorRef intersection = context.actorOf(IntersectionActor.props(x, y), "intersection_${x}_$y")
			it.value.each {
				intersection.tell(new RegisterRoadMsg(), it)
			}
		}
	}
}
