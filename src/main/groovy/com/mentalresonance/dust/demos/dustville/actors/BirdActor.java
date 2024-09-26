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
import com.mentalresonance.dust.demos.dustville.objects.*;
import com.mentalresonance.dust.demos.dustville.utils.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import static com.mentalresonance.dust.demos.dustville.utils.HoodGeomtery.*;

/**
 * Control the flight of a single bird. We know our position, our direction and our speed so we can update our position.
 * We also know which (neighbor)hood we are in (the small cube) and can see if we are moving out of hood into another
 * in which case we tell our current hood we are leaving and our new one we are entering. The hoods take this message
 * to update the overall direction of birds in its volume and sends that message back to me so we can adjust to the flow.
 */

@Slf4j
public class BirdActor extends Actor {

	Vector3D position; // Current position
	Vector3D direction; // Of flight
	Vector3D currentHood;
	Double speed; // Along direction
	ActorRef currentHoodRef = null;

	public static Props props(Bird bird) {
		return Props.create(BirdActor.class, bird);
	}

	public BirdActor(Bird bird) {
		position = bird.position;
		direction = bird.direction;
		speed = bird.speed;
	}

	@Override
	public void preStart() {
		scheduleIn(new InitMsg(), 5000L);
	}

	@Override
	protected ActorBehavior createBehavior() {
		return (Serializable message) -> {
			switch(message)
			{
				case InitMsg ignored:
					currentHood = HoodUtils.hood(position);
					currentHoodRef = context.actorSelection("/user/" + HoodUtils.hoodName(position));
					currentHoodRef.tell(new AddBirdMsg(new Bird(self.name, position, direction, speed)), self);
					self.tell(new StartMsg(), self);
					break;

				case StartMsg ignored:
					Vector3D newPosition = position.plus(direction.scale(speed)), newHood;

					if (newPosition.equals(position))
						log.error("Did not move $position");

					// If we are leaving the volume change our direction to fly back in
					if (newPosition.x < 0) direction.setXSign(true);
					if (newPosition.x > 5 + (SIZE_X-1) * 10) direction.setXSign(false);
					if (newPosition.y < 0) direction.setYSign(true);
					if (newPosition.y > 5 + (SIZE_Y-1) * 10) direction.setYSign(false);
					if (newPosition.z < 0) direction.setZSign(true);
					if (newPosition.z > 5 + (SIZE_Z-1) * 10)
						direction.setZSign(false);

					newHood = HoodUtils.hood(newPosition);
					position = newPosition;

					if (position.inCube()) {
						if (! newHood.equals(currentHood)) {
							currentHoodRef.tell(new RemoveBirdMsg(), self);
							currentHoodRef = context.actorSelection("/user/" + HoodUtils.hoodName(newHood));
							currentHoodRef.tell(new AddBirdMsg(new Bird(self.name, position, direction, speed)), self);
							currentHood = newHood;
						}
						else {
							currentHoodRef.tell(new UpdateBirdMsg(new Bird(self.name, position, direction, speed)), self);
						}
						currentHoodRef.tell(new GetAveragesMsg(), self);
					}
					else {
						// Let direction changes pull us back ...
						position = position.plus(direction.scale(speed));
					}
					scheduleIn(new StartMsg(), 500L);
					break;

				case GetAveragesMsg msg:
					Vector3D avgs = msg.direction;
					if (avgs.length() > 0) {
						direction = avgs;
						speed = msg.speed;
					}
					break;

				default: super.createBehavior().onMessage(message);
			}
		};
	}

	static class InitMsg implements Serializable {}
}
