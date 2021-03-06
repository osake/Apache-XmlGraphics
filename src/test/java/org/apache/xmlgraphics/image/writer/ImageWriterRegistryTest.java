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

/* $Id: ImageWriterRegistryTest.java 991838 2010-09-02 07:55:21Z jeremias $ */

package org.apache.xmlgraphics.image.writer;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

import junit.framework.TestCase;

import org.apache.xmlgraphics.image.writer.imageio.ImageIOPNGImageWriter;
import org.junit.Test;

/**
 * Tests the {@link ImageWriterRegistry}.
 */
public class ImageWriterRegistryTest extends TestCase {

    @Test
    public void testRegistry() {
        final ImageWriterRegistry registry = new ImageWriterRegistry();

        ImageWriter writer;
        writer = registry.getWriterFor("image/something");
        assertNull(writer);

        writer = registry.getWriterFor("image/png");
        assertTrue(writer instanceof ImageIOPNGImageWriter);

        registry.register(new DummyPNGWriter());

        ImageWriter dummy = registry.getWriterFor("image/png");
        assertEquals(DummyPNGWriter.class, dummy.getClass());

        registry.register(new OtherPNGWriter(), 50);

        dummy = registry.getWriterFor("image/png");
        assertEquals(OtherPNGWriter.class, dummy.getClass());
    }

    private static class DummyPNGWriter extends AbstractImageWriter {

        @Override
        public String getMIMEType() {
            return "image/png";
        }

        @Override
        public void writeImage(final RenderedImage image, final OutputStream out)
                throws IOException {
            // nop
        }

        @Override
        public void writeImage(final RenderedImage image,
                final OutputStream out, final ImageWriterParams params)
                throws IOException {
            // nop
        }

    }

    private static class OtherPNGWriter extends DummyPNGWriter {

    }
}
