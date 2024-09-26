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

var tablet = function(scene, width, height) {
    var screenDisplay = BABYLON.MeshBuilder.CreatePlane("screenDisplay", {height: height, width: width}, scene);
    screenDisplay.position.y = 2;
    var advancedTexture = BABYLON.GUI.AdvancedDynamicTexture.CreateForMesh(screenDisplay, 1024, 1024 * height / width);
    var tb = new BABYLON.GUI.TextBlock();
    tb.textWrapping = BABYLON.GUI.TextWrapping.WordWrap;
    tb.resizeToFit = true;
    tb.paddingTop = "5%";
    tb.paddingLeft = "30px";
    tb.paddingRight = "20px";
    tb.paddingBottom = "5%";
    tb.horizontalAlignment = BABYLON.GUI.Control.HORIZONTAL_ALIGNMENT_LEFT;
    tb.verticalAlignment = BABYLON.GUI.Control.VERTICAL_ALIGNMENT_TOP;
    tb.textHorizontalAlignment = BABYLON.GUI.Control.HORIZONTAL_ALIGNMENT_LEFT;
    tb.textVerticalAlignment = BABYLON.GUI.Control.VERTICAL_ALIGNMENT_TOP;
    tb.color = "white";
    tb.background = "yellow";
    tb.fontSize = "40px";

    this.getDisplay = function() {
        return screenDisplay;
    }

    this.setContent = function(content) {
        tb.text = content;
        //scroll
        var sv = new BABYLON.GUI.ScrollViewer();
        sv.thickness = 0;
        sv.color = "white";
        sv.width = 1;
        sv.height = 1;
        sv.background = "rgba(187,86,86,0.81)";
        advancedTexture.addControl(sv);
        sv.addControl(tb);
    }

    this.setFontColor = function(color) {
        tb.color = color;
    }

    this.setFontSize = function(size) {
        tb.fontSize = size;
    }

    return this;
}