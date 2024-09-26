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
import com.mentalresonance.dust.demos.dustville.actors.HouseActor
import com.mentalresonance.dust.demos.dustville.actors.PowerStationActor
import com.mentalresonance.dust.demos.dustville.actors.TownActor
import groovy.transform.CompileStatic

/**
 * Kick off building the town. We set up Actors for the Town in general (which for now just adds cars to the roads and
 * takes them off. {@link TownActor} the Power station {@link PowerStationActor}.
 * Finally it creates a set of Actors managing houses {@link HouseActor}
 */
@CompileStatic
class TownSetup {

	public static int NUM_HOUSES = 50

	// Common stats sampling time
	public static long STATS_SAMPLE_DT = 5 * 60 * 1000L

	void init(ActorContext context)
	{
		context.actorOf( TownActor.props(), "town")
		context.actorOf( PowerStationActor.props(), "powerstation")

		/**
		 * House Actors are named uniquely by their index
		 */
		for (int j = 0; j < NUM_HOUSES; ++j) {
			context.actorOf( HouseActor.props(), "house$j")
		}

	}

}
