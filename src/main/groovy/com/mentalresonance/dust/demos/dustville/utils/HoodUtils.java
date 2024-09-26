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

public class HoodUtils {

    /**
     * @param position
     * @return coords of neighborhood containing point
     */
    public static Vector3D hood(Vector3D position) {
        return new Vector3D(
                5 + 10 * Math.floor(position.x / (double) HoodGeomtery.SIZE_X),
                5 + 10 * Math.floor(position.y / (double) HoodGeomtery.SIZE_Y),
                5 + 10 * Math.floor(position.z / (double) HoodGeomtery.SIZE_Z)
        );
    }

    /**
     * @param position
     * @return name (hood_x_y_z) coords of neighborhood containing point
     */
    public static String hoodName(Vector3D position) {
        Vector3D hood = hood(position);

        return "hood_%d_%d_%d".formatted(hood.x.intValue(), hood.y.intValue(), hood.z.intValue());
    }
}
