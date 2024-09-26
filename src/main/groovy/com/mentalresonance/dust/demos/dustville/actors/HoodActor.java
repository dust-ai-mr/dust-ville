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

package com.mentalresonance.dust.demos.dustville.actors;

import com.mentalresonance.dust.core.actors.Actor;
import com.mentalresonance.dust.core.actors.ActorBehavior;
import com.mentalresonance.dust.core.actors.ActorRef;
import com.mentalresonance.dust.core.actors.Props;
import com.mentalresonance.dust.core.msgs.StartMsg;
import com.mentalresonance.dust.demos.dustville.msgs.birds.*;
import com.mentalresonance.dust.demos.dustville.objects.Bird;
import com.mentalresonance.dust.demos.dustville.utils.HoodUtils;
import com.mentalresonance.dust.demos.dustville.utils.Vector3D;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.mentalresonance.dust.demos.dustville.utils.HoodGeomtery.*;

/**
 * Neighborhood is a 20x20x20 cube of 'hoods' - each a box 10x10x10 units. So each hood has immediate
 * neighbors (not all of which may be present in edge cases)  - N, S, E, W, T(op), B(ottom).
 * <p/>
 * The name of a Hood Actor is hood_x_y_z where x,y,z are the coordinates of its center.
 * We manage all the birds that are currently within us and exits and entrances. We compute the overall
 * average direction of the birds and tell them when they ask.
 * We also respond to periodic messages for the state of all the
 * birds we know about and that info is passed to the UI.
 *
 * We update our state every 2 seconds.
 */
@Slf4j
public class HoodActor extends Actor {

	Vector3D position; // x, y, z of middle of box
	Map<String, Bird> birds = new HashMap<>();  // Bird name -> BirdInfo
	Map<String, ActorRef> neighbors = new HashMap<>();  // hood_x_y_z -> actor ref
	Vector3D averageDirection; // Of all the birds in the hood
	Double averageSpeed;

	public static Props props(Vector3D position) {
		return Props.create(HoodActor.class, position);
	}

	public HoodActor(Vector3D position) {
		this.position = position;
	}

	/**
	 * Find the names of my immediate neighbors and then start me up.
	 */
	@Override
	protected void preStart()
	{
		if (position.x > 5)
			neighbors.put(HoodUtils.hoodName(new Vector3D(position.x - 10, position.y, position.z)).toString(), (ActorRef)null);
		if (position.x < 5 + (SIZE_X-1)*10)
			neighbors.put(HoodUtils.hoodName(new Vector3D(position.x + 10, position.y, position.z)).toString(), (ActorRef)null);
		if (position.y > 5)
			neighbors.put(HoodUtils.hoodName(new Vector3D(position.x, position.y - 10, position.z)).toString(), (ActorRef)null);
		if (position.y < 5 + (SIZE_Y-1)*10)
			neighbors.put(HoodUtils.hoodName(new Vector3D(position.x, position.y + 10, position.z)).toString(), (ActorRef)null);
		if (position.z > 5)
			neighbors.put(HoodUtils.hoodName(new Vector3D(position.x, position.y, position.z - 10)).toString(), (ActorRef)null);
		if (position.z < 5 + (SIZE_Z-1)*10)
			neighbors.put(HoodUtils.hoodName(new Vector3D(position.x, position.y, position.z + 10)).toString(), (ActorRef)null);

		self.tell(new StartMsg(), self);
	}

	protected void postStop() {
		log.warn(self.path + " has shut down !!");
	}

	@Override
	protected ActorBehavior createBehavior() {
		return (Serializable message) -> {
			switch(message)
			{
				case AddBirdMsg ignored:
					birds.put(sender.name, ((AddBirdMsg)message).bird);
					break;

				case UpdateBirdMsg ignored:
					birds.put(sender.name, ((UpdateBirdMsg)message).bird);
					break;

				case RemoveBirdMsg ignored:
					birds.remove(sender.name);
					break;

				case GetBirdsRequestMsg ignored:
					sender.tell(new GetBirdsResponseMsg(self.name, birds.values().stream().toList()), self);
					break;

				case StartMsg ignored:
					averageDirection = new Vector3D(0, 0, 0);
					averageSpeed = 0.0;
					int flock = birds.size();
					if (flock > 0) {
						birds.forEach(
							(String k, Bird bird) -> {
								averageDirection = averageDirection.plus(bird.direction);
								averageSpeed += bird.speed;
							}
						);
						averageDirection = averageDirection.scale(1.0d / flock).normalize();
						averageSpeed = averageSpeed / flock;
					}
					scheduleIn(new StartMsg(), 5000L);
					break;

				case GetAveragesMsg msg:
					msg.direction = averageDirection;
					msg.speed = averageSpeed;
					sender.tell(message, self);
					break;

				default: super.createBehavior().onMessage(message);

			}
		};
	}
}
