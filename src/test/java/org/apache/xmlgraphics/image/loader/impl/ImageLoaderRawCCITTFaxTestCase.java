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

/* $Id$ */

package org.apache.xmlgraphics.image.loader.impl;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.xmlgraphics.image.loader.ImageContext;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.MockImageContext;
import org.apache.xmlgraphics.image.loader.MockImageSessionContext;
import org.apache.xmlgraphics.util.MimeConstants;
import org.junit.Test;

/**
 * Test case for {@link ImageLoaderRawCCITTFax}.
 */
public class ImageLoaderRawCCITTFaxTestCase extends TestCase {
    private ImageLoaderRawCCITTFax sut;

    @Test
    public void testLoadImage() throws ImageException, IOException {
        final ImageContext context = MockImageContext.newSafeInstance();
        final ImageSessionContext session = new MockImageSessionContext(context);
        // This image file is NOT a valid tif! It is the directory table ONLY.
        final ImageInfo info = new ImageInfo("dirOnly.tif",
                MimeConstants.MIME_TIFF);
        final ImageSize size = new ImageSize();
        // Size data can be retrieve by parsing the directory table in the TIFF
        size.setSizeInPixels(1728, 2266);
        size.setResolution(203, 192);
        size.calcSizeFromPixels();
        info.setSize(size);

        this.sut = new ImageLoaderRawCCITTFax();
        final ImageRawCCITTFax rawImage = (ImageRawCCITTFax) this.sut
                .loadImage(info, null, session);
        assertEquals(2, rawImage.getCompression());
    }
}
