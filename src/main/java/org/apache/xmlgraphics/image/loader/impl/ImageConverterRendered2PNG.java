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

/* $Id: ImageConverterRendered2PNG.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.image.loader.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.writer.ImageWriter;
import org.apache.xmlgraphics.image.writer.ImageWriterParams;
import org.apache.xmlgraphics.image.writer.ImageWriterRegistry;
import org.apache.xmlgraphics.util.MimeConstants;

/**
 * This ImageConverter converts Rendered to PNG images.
 */
public class ImageConverterRendered2PNG extends AbstractImageConverter {

    /** {@inheritDoc} */
    @Override
    public Image convert(final Image src, final Map<Object, Object> hints)
            throws ImageException, IOException {
        checkSourceFlavor(src);
        final ImageRendered rendered = (ImageRendered) src;
        final ImageWriter writer = ImageWriterRegistry.getInstance()
                .getWriterFor(MimeConstants.MIME_PNG);
        if (writer == null) {
            throw new ImageException(
                    "Cannot convert image to PNG. No suitable ImageWriter found.");
        }
        try (final ByteArrayOutputStream baout = new ByteArrayOutputStream()) {
            final ImageWriterParams params = new ImageWriterParams();
            params.setResolution((int) Math.round(src.getSize()
                    .getDpiHorizontal()));
            writer.writeImage(rendered.getRenderedImage(), baout, params);
            return new ImageRawStream(src.getInfo(), getTargetFlavor(),
                    new ByteArrayInputStream(baout.toByteArray()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getSourceFlavor() {
        return ImageFlavor.RENDERED_IMAGE;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getTargetFlavor() {
        return ImageFlavor.RAW_PNG;
    }

}
