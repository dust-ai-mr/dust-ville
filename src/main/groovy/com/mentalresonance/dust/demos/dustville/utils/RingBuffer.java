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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Simple endless buffer. All you can do is add next element and return current contents (in correct order)
 * @param <E> - contents type
 */
public class RingBuffer<E> {

    E[] data;
    int capacity,
        first = 0,  // Index of first element in list
        last = -1;  // Index of last element in list.
    Function<Object, Object> mapper = null;

    public RingBuffer(int size) {
        this.capacity = size;
        this.data = (E[]) new Object[size];
    }

    public RingBuffer(int size, Function<Object, Object> mapper) {
        this(size);
        this.mapper = mapper;
    }

    boolean isEmpty() { return last == -1; }

    public void add(E e) {
        if (first <= last || isEmpty()) {
            if (++last == capacity) {
                last = 0;
                first = (++first) % capacity;
            }
        }
        else {
            first = (++first) % capacity;
            last = (++last) % capacity;
        }
        data[last] = e;
    }

    /**
     *
     * @return New array of contents in order
     */
    public E[] contents() {
        E[] buffer;

        if (isEmpty())
            return (E[])new Object[0];
        else if (first < last) {
            buffer = (E[]) new Object[1+last - first];
            for (int i = first; i <= last; ++i) {
                buffer[i] = data[i];
            }
        }
        else if (first == last) {
            buffer =  (E[])new Object[1];
            buffer[0] = data[0];
        }
        else {
            buffer = (E[]) new Object[capacity];
            for (int i = first; i < capacity; ++i) {
                buffer[i-first] = data[i];
            }
            for (int i = 0; i <= last; ++i) {
                buffer[capacity - first + i] = data[i];
            }
        }
        if (null != mapper)
            buffer = (E[]) Arrays.stream(buffer).map(mapper).toArray();

        return buffer;
    }

    public HashMap<String, Object> serialize() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("data", contents());
        map.put("capacity", capacity);
        return map;
    }

    public static RingBuffer deSerialize(Map serialized) {
        Integer capacity = (Integer)serialized.get("capacity");
        RingBuffer rb = new RingBuffer<>(capacity);
        Object[] contents = (Object[]) serialized.get("data");

        for (Object content : contents) {
            rb.add(content);
        }
        return rb;
    }
}
