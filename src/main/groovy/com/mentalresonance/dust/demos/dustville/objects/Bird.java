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

package com.mentalresonance.dust.demos.dustville.objects;

import java.io.Serializable;

import com.mentalresonance.dust.demos.dustville.utils.Vector3D;

/**
 * Bird contains instantaneous state of bird
 */
public class Bird implements Serializable {

	public String name;
	public Vector3D position; // Current position
	public Vector3D direction; // Of flight
	public Double speed; // Along direction

	public Bird(String name, Vector3D position, Vector3D direction, Double speed) {
		this.name = name;
		this.position = position;
		this.direction = direction;
		this.speed = speed;
	}
}
