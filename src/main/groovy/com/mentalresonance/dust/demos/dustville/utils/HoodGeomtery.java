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

package com.mentalresonance.dust.demos.dustville.utils;

public class HoodGeomtery {
    /**
     * Divide a part of space in neighborhoods - 10x10x10 cubes. SIZE_ indicates number of cubes
     * and BASE_ is the lowest value for the center of cubes
     */
    public static int
            BASE_X = 75,        // Position of 0,0,0 corner
            BASE_Y = 5,
            BASE_Z = 15,
            SIZE_X = 20,        // # of 10x10 cubes on each side
            SIZE_Y = 20,
            SIZE_Z = 20;
}
