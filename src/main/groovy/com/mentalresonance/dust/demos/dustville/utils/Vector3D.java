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

import java.io.Serializable;
import static com.mentalresonance.dust.demos.dustville.utils.HoodGeomtery.*;

public class Vector3D implements Serializable {

    public Double x, y, z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D(Vector3D other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public double length() {
        return Math.sqrt( Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    public Vector3D minus(Vector3D other) {
        return new Vector3D(x - other.x, y - other.y, z - other.z);
    }

    public Vector3D plus(Vector3D other) {
        return new Vector3D(x + other.x, y + other.y, z + other.z);
    }

    public Vector3D scale(double scale) {
        return new Vector3D(x * scale, y * scale, z * scale);
    }

    public Vector3D normalize() { return scale( 1 / length()); }

    public boolean hasNeg() { return (x < 0 || y < 0 || z < 0); }

    public double distance(Vector3D other) {
        return this.minus(other).length();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Vector3D) {
            Vector3D vec = (Vector3D) other;
            return x.equals(vec.x) && y.equals(vec.y)  && z.equals(vec.z);
        } else
            return false;
    }

    @Override
    public String toString() {
        return "%f_%f_%f ".formatted(x, y, z);
    }

    public boolean inCube() {
        return (! hasNeg()) &&
            x < 10 * SIZE_X &&
            y < 10 * SIZE_Y &&
            z < 10 * SIZE_Z;
    }

    public void setXSign(boolean pos) {
        x = pos ? (x < 0 ? -x: x) : (x > 0 ? -x: x);
    }

    public void setYSign(boolean pos) {
        y = pos ? (y < 0 ? -y: y) : (y > 0 ? -y: y);
    }

    public void setZSign(boolean pos) {
        z = pos ? (z < 0 ? -z: z) : (z > 0 ? -z: z);
    }
}
