/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: SoftMapCache.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.image.loader.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides a simple cache using soft references and storing the values in a
 * Map. The keys into the Map are hard references, the values are referenced
 * through soft references. The collected values are cleaned up through a
 * ReferenceQueue.
 */
@Slf4j
public class SoftMapCache {

    private Map<Object, Reference> map;
    private final ReferenceQueue<Reference> refQueue = new ReferenceQueue<>();

    /**
     * Creates a new soft cache.
     * 
     * @param synched
     *            true if the Map containing the values should by synchronized
     */
    public SoftMapCache(final boolean synched) {
        this.map = new HashMap<>();
        if (synched) {
            this.map = Collections.synchronizedMap(this.map);
        }
    }

    /**
     * Returns the value associated with the given key. If the value is not
     * found or the value has been collected, null is returned.
     * 
     * @param key
     *            the key
     * @return the requested value or null
     */
    public Object get(final Object key) {
        final Reference ref = this.map.get(key);
        return getReference(key, ref);
    }

    /**
     * Removed the value associated with the given key. The value that is
     * removed is returned as the methods result. If the value is not found or
     * the value has been collected, null is returned.
     * 
     * @param key
     *            the key
     * @return the requested value or null
     */
    public Object remove(final Object key) {
        final Reference ref = this.map.remove(key);
        return getReference(key, ref);
    }

    private Object getReference(final Object key, final Reference ref) {
        Object value = null;
        if (ref != null) {
            value = ref.get();
            if (value == null) {
                // Remove key if its value has been garbage collected
                log.trace("Image has been collected: {}", key);
                checkReferenceQueue();
            }
        }
        return value;
    }

    /**
     * Put a new value in the cache overwriting any existing value with the same
     * key.
     * 
     * @param key
     *            The key
     * @param value
     *            the value
     */
    public void put(final Object key, final Object value) {
        this.map.put(key, wrapInReference(value, key));
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        this.map.clear();
    }

    /**
     * Triggers some house-keeping, i.e. processes any pending objects in the
     * reference queue.
     */
    public void doHouseKeeping() {
        checkReferenceQueue();
    }

    private Reference wrapInReference(final Object obj, final Object key) {
        return new SoftReferenceWithKey(obj, key, this.refQueue);
    }

    /**
     * Checks the reference queue if any references have been cleared and
     * removes them from the cache.
     */
    private void checkReferenceQueue() {
        SoftReferenceWithKey ref;
        while ((ref = (SoftReferenceWithKey) this.refQueue.poll()) != null) {
            log.trace("Removing ImageInfo from ref queue: {}", ref.getKey());
            this.map.remove(ref.getKey());
        }
    }
}
