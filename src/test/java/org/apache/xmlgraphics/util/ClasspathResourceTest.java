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

/* $Id: ClasspathResourceTest.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.util;

import java.net.URL;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Test for the Service class.
 */
public class ClasspathResourceTest extends TestCase {

    /**
     * Tests whether the file /sample.txt with mime-type text/plain exists.
     *
     * @throws Exception
     *             in case of an error
     */
    @Test
    public void testSampleResource() {
        final List<URL> list = ClasspathResource.getInstance()
                .listResourcesOfMimeType("text/plain");
        boolean found = false;
        for (final URL url : list) {
            if (url.getPath().endsWith("sample.txt")) {
                found = true;
            }
        }
        assertTrue(found);
    }

    /**
     * Tests the mode where Service returns class names.
     *
     * @throws Exception
     *             in case of an error
     */
    @Test
    public void testNonexistingResource() {
        final List<URL> list = ClasspathResource.getInstance()
                .listResourcesOfMimeType("nota/mime-type");
        assertTrue(list.isEmpty());
    }

}
