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

/* $Id: CodecResourcesTest.java 954256 2010-06-13 16:27:18Z jeremias $ */

package org.apache.xmlgraphics.image.codec.png;

import java.io.InputStream;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.xmlgraphics.image.codec.util.MemoryCacheSeekableStream;
import org.apache.xmlgraphics.image.codec.util.SeekableStream;
import org.junit.Test;

/**
 * Checks for the presence of message resources for the internal codecs.
 */
@Slf4j
public class CodecResourcesTest extends TestCase {

    @Test
    public void testResources() {

        final InputStream in = getClass().getResourceAsStream(
                "/images/barcode.eps");
        final SeekableStream seekStream = new MemoryCacheSeekableStream(in);
        try {
            new PNGImage(seekStream, null);
            fail("Exception expected");
        } catch (final Exception re) {
            log.info("FullException :", re);
            final String msg = re.getMessage();
            if ("PNGImageDecoder0".equals(msg)) {
                log.error("Exception", re);
                fail("Message resource don't seem to be present! Message is: "
                        + msg);
            } else if (msg.toLowerCase().indexOf("magic") < 0) {
                fail("Message not as expected! Message is: " + msg);
            }
        } finally {
            IOUtils.closeQuietly(seekStream);
            IOUtils.closeQuietly(in);
        }

    }

}
