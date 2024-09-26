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

package com.mentalresonance.dust.demos.dustville.msgs

import com.mentalresonance.dust.demos.dustville.utils.RingBuffer
import groovy.transform.CompileStatic

@CompileStatic
class GetTimeSeriesStatsRequestMsg implements Serializable {
	String name

	GetTimeSeriesStatsRequestMsg(String name) {
		this.name = name
	}
}

@CompileStatic
class GetTimeSeriesStatsResponseMsg<E> implements Serializable {
	RingBuffer<TimeSeriesEntry<E>> ringBuffer
	String title
	String seriesName

	GetTimeSeriesStatsResponseMsg(RingBuffer ringBuffer, String title, String seriesName) {
		this.ringBuffer = ringBuffer
		this.title = title
		this.seriesName = seriesName
	}
}

@CompileStatic
class TimeSeriesEntry<E> implements Serializable {
	long ts
	E value

	TimeSeriesEntry(long ts, E value) {
		this.ts = ts
		this.value = value
	}
}


