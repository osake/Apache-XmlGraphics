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

/* $Id: CorruptImagesTestCase.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.image.loader;

import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Tests for error behaviour with corrupt images.
 */
public class CorruptImagesTestCase extends TestCase {

	private final MockImageContext imageContext = MockImageContext
			.getInstance();

	public CorruptImagesTestCase(final String name) {
		super(name);
	}

	@Test
	public void testCorruptPNG() throws IOException {
		final String uri = "corrupt-image.png";

		final ImageSessionContext sessionContext = this.imageContext
				.newSessionContext();
		final ImageManager manager = this.imageContext.getImageManager();

		try {
			manager.preloadImage(uri, sessionContext);
			fail("Expected an ImageException!");
		} catch (final ImageException ie) {
			// Expected exception
			assertNotNull(ie.getMessage());
		}
	}

}
