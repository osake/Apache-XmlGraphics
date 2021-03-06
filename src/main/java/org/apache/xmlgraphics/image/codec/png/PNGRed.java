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

/* $Id: PNGRed.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.image.codec.png;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.xmlgraphics.image.GraphicsUtil;
import org.apache.xmlgraphics.image.codec.util.PropertyUtil;
import org.apache.xmlgraphics.image.rendered.AbstractRed;
import org.apache.xmlgraphics.image.rendered.CachableRed;

// CSOFF: ConstantName
// CSOFF: InnerAssignment
// CSOFF: MethodName
// CSOFF: MissingSwitchDefault
// CSOFF: MultipleVariableDeclarations
// CSOFF: NoWhitespaceAfter
// CSOFF: OperatorWrap
// CSOFF: WhitespaceAround

/**
 * @version $Id: PNGRed.java 1345683 2012-06-03 14:50:33Z gadams $
 */
@Slf4j
public class PNGRed extends AbstractRed {

    static class PNGChunk {
        int length;
        int type;
        byte[] data;
        int crc;

        String typeString;

        public PNGChunk(final int length, final int type, final byte[] data,
                final int crc) {
            this.length = length;
            this.type = type;
            this.data = data;
            this.crc = crc;

            this.typeString = "";
            this.typeString += (char) (type >> 24);
            this.typeString += (char) (type >> 16 & 0xff);
            this.typeString += (char) (type >> 8 & 0xff);
            this.typeString += (char) (type & 0xff);
        }

        public int getLength() {
            return this.length;
        }

        public int getType() {
            return this.type;
        }

        public String getTypeString() {
            return this.typeString;
        }

        public byte[] getData() {
            return this.data;
        }

        public byte getByte(final int offset) {
            return this.data[offset];
        }

        public int getInt1(final int offset) {
            return this.data[offset] & 0xff;
        }

        public int getInt2(final int offset) {
            return (this.data[offset] & 0xff) << 8 | this.data[offset + 1]
                    & 0xff;
        }

        public int getInt4(final int offset) {
            return (this.data[offset] & 0xff) << 24
                    | (this.data[offset + 1] & 0xff) << 16
                    | (this.data[offset + 2] & 0xff) << 8
                    | this.data[offset + 3] & 0xff;
        }

        public String getString4(final int offset) {
            String s = new String();
            s += (char) this.data[offset];
            s += (char) this.data[offset + 1];
            s += (char) this.data[offset + 2];
            s += (char) this.data[offset + 3];
            return s;
        }

        public boolean isType(final String typeName) {
            return this.typeString.equals(typeName);
        }
    }

    public static final int PNG_COLOR_GRAY = 0;
    public static final int PNG_COLOR_RGB = 2;
    public static final int PNG_COLOR_PALETTE = 3;
    public static final int PNG_COLOR_GRAY_ALPHA = 4;
    public static final int PNG_COLOR_RGB_ALPHA = 6;

    private static final String[] colorTypeNames = { "Grayscale", "Error",
            "Truecolor", "Index", "Grayscale with alpha", "Error",
            "Truecolor with alpha" };

    public static final int PNG_FILTER_NONE = 0;
    public static final int PNG_FILTER_SUB = 1;
    public static final int PNG_FILTER_UP = 2;
    public static final int PNG_FILTER_AVERAGE = 3;
    public static final int PNG_FILTER_PAETH = 4;

    private final int[][] bandOffsets = { null, { 0 }, // G
            { 0, 1 }, // GA in GA order
            { 0, 1, 2 }, // RGB in RGB order
            { 0, 1, 2, 3 } // RGBA in RGBA order
    };

    private int bitDepth;
    private int colorType;

    private int compressionMethod;
    private int filterMethod;
    private int interlaceMethod;

    private int paletteEntries;
    private byte[] redPalette;
    private byte[] greenPalette;
    private byte[] bluePalette;
    private byte[] alphaPalette;

    private int bkgdRed;
    private int bkgdGreen;
    private int bkgdBlue;

    private int grayTransparentAlpha;
    private int redTransparentAlpha;
    private int greenTransparentAlpha;
    private int blueTransparentAlpha;

    private int maxOpacity;

    private int[] significantBits = null;

    // Parameter information

    // If true, the user wants destination alpha where applicable.
    private boolean suppressAlpha = false;

    // If true, perform palette lookup internally
    private boolean expandPalette = false;

    // If true, output < 8 bit gray images in 8 bit components format
    private boolean output8BitGray = false;

    // Create an alpha channel in the destination color model.
    private boolean outputHasAlphaPalette = false;

    // Perform gamma correction on the image
    private boolean performGammaCorrection = false;

    // Expand GA to GGGA for compatbility with Java2D
    private boolean expandGrayAlpha = false;

    // Produce an instance of PNGEncodeParam
    private boolean generateEncodeParam = false;

    // PNGDecodeParam controlling decode process
    private PNGDecodeParam decodeParam = null;

    // PNGEncodeParam to store file details in
    private PNGEncodeParam encodeParam = null;

    private final boolean emitProperties = true;

    private float fileGamma = 45455 / 100000.0F;

    private float userExponent = 1.0F;

    private float displayExponent = 2.2F;

    private float[] chromaticity = null;

    private int sRGBRenderingIntent = -1;

    // Post-processing step implied by above parameters
    private int postProcess = POST_NONE;

    // Possible post-processing steps

    // Do nothing
    private static final int POST_NONE = 0;

    // Gamma correct only
    private static final int POST_GAMMA = 1;

    // Push gray values through grayLut to expand to 8 bits
    private static final int POST_GRAY_LUT = 2;

    // Push gray values through grayLut to expand to 8 bits, add alpha
    private static final int POST_GRAY_LUT_ADD_TRANS = 3;

    // Push palette value through R,G,B lookup tables
    private static final int POST_PALETTE_TO_RGB = 4;

    // Push palette value through R,G,B,A lookup tables
    private static final int POST_PALETTE_TO_RGBA = 5;

    // Add transparency to a given gray value (w/ optional gamma)
    private static final int POST_ADD_GRAY_TRANS = 6;

    // Add transparency to a given RGB value (w/ optional gamma)
    private static final int POST_ADD_RGB_TRANS = 7;

    // Remove the alpha channel from a gray image (w/ optional gamma)
    private static final int POST_REMOVE_GRAY_TRANS = 8;

    // Remove the alpha channel from an RGB image (w/optional gamma)
    private static final int POST_REMOVE_RGB_TRANS = 9;

    // Mask to add expansion of GA -> GGGA
    private static final int POST_EXP_MASK = 16;

    // Expand gray to G/G/G
    private static final int POST_GRAY_ALPHA_EXP = POST_NONE | POST_EXP_MASK;

    // Expand gray to G/G/G through a gamma lut
    private static final int POST_GAMMA_EXP = POST_GAMMA | POST_EXP_MASK;

    // Push gray values through grayLut to expand to 8 bits, expand, add alpha
    private static final int POST_GRAY_LUT_ADD_TRANS_EXP = POST_GRAY_LUT_ADD_TRANS
            | POST_EXP_MASK;

    // Add transparency to a given gray value, expand
    private static final int POST_ADD_GRAY_TRANS_EXP = POST_ADD_GRAY_TRANS
            | POST_EXP_MASK;

    private List<InputStream> streamVec = new ArrayList<>();
    private DataInputStream dataStream;

    private int bytesPerPixel; // number of bytes per input pixel
    private int inputBands;
    private int outputBands;

    // Number of private chunks
    private int chunkIndex = 0;

    private final List<String> textKeys = new ArrayList<>();
    private final List<String> textStrings = new ArrayList<>();

    private final List<String> ztextKeys = new ArrayList<>();
    private final List<String> ztextStrings = new ArrayList<>();

    private WritableRaster theTile;
    private Rectangle bounds;

    /** A Hashtable containing the image properties. */
    private final Map<String, Object> properties = new HashMap<>();

    private int[] gammaLut = null;

    private void initGammaLut(final int bits) {
        final double exp = (double) this.userExponent
                / (this.fileGamma * this.displayExponent);
        final int numSamples = 1 << bits;
        final int maxOutSample = bits == 16 ? 65535 : 255;

        this.gammaLut = new int[numSamples];
        for (int i = 0; i < numSamples; ++i) {
            final double gbright = (double) i / (numSamples - 1);
            final double gamma = Math.pow(gbright, exp);
            int igamma = (int) (gamma * maxOutSample + 0.5);
            if (igamma > maxOutSample) {
                igamma = maxOutSample;
            }
            this.gammaLut[i] = igamma;
        }
    }

    private final byte[][] expandBits = {
            null,
            { (byte) 0x00, (byte) 0xff },
            { (byte) 0x00, (byte) 0x55, (byte) 0xaa, (byte) 0xff },
            null,
            { (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44,
                    (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88,
                    (byte) 0x99, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc,
                    (byte) 0xdd, (byte) 0xee, (byte) 0xff } };

    private int[] grayLut = null;

    private void initGrayLut(final int bits) {
        final int len = 1 << bits;
        this.grayLut = new int[len];

        if (this.performGammaCorrection) {
            System.arraycopy(this.gammaLut, 0, this.grayLut, 0, len);
        } else {
            for (int i = 0; i < len; ++i) {
                this.grayLut[i] = this.expandBits[bits][i];
            }
        }
    }

    public PNGRed(final InputStream stream) {
        this(stream, null);
    }

    public PNGRed(final InputStream inStream, final PNGDecodeParam inDecodeParam) {
        final InputStream stream;
        if (!inStream.markSupported()) {
            stream = new BufferedInputStream(inStream);
        } else {
            stream = inStream;
        }
        DataInputStream distream = null;
        try {
            distream = new DataInputStream(stream);

            final PNGDecodeParam decodeParam;
            if (inDecodeParam == null) {
                decodeParam = new PNGDecodeParam();
            } else {
                decodeParam = inDecodeParam;
            }
            this.decodeParam = decodeParam;

            // Get parameter values
            this.suppressAlpha = decodeParam.getSuppressAlpha();
            this.expandPalette = decodeParam.getExpandPalette();
            this.output8BitGray = decodeParam.getOutput8BitGray();
            this.expandGrayAlpha = decodeParam.getExpandGrayAlpha();
            if (decodeParam.getPerformGammaCorrection()) {
                this.userExponent = decodeParam.getUserExponent();
                this.displayExponent = decodeParam.getDisplayExponent();
                this.performGammaCorrection = true;
                this.output8BitGray = true;
            }
            this.generateEncodeParam = decodeParam.getGenerateEncodeParam();

            if (this.emitProperties) {
                this.properties.put("file_type", "PNG v. 1.0");
            }

            try {
                final long magic = distream.readLong();
                if (magic != 0x89504e470d0a1a0aL) {
                    final String msg = PropertyUtil
                            .getString("PNGImageDecoder0");
                    throw new RuntimeException(msg);
                }
            } catch (final Exception e) {
                log.error("Exception", e);
                final String msg = PropertyUtil.getString("PNGImageDecoder1");
                throw new RuntimeException(msg);
            }

            do {
                try {
                    PNGChunk chunk;

                    final String chunkType = getChunkType(distream);
                    if (chunkType.equals("IHDR")) {
                        chunk = readChunk(distream);
                        parse_IHDR_chunk(chunk);
                    } else if (chunkType.equals("PLTE")) {
                        chunk = readChunk(distream);
                        parse_PLTE_chunk(chunk);
                    } else if (chunkType.equals("IDAT")) {
                        chunk = readChunk(distream);
                        this.streamVec.add(new ByteArrayInputStream(chunk
                                .getData()));
                    } else if (chunkType.equals("IEND")) {
                        chunk = readChunk(distream);
                        parse_IEND_chunk(chunk);
                        break; // fall through to the bottom
                    } else if (chunkType.equals("bKGD")) {
                        chunk = readChunk(distream);
                        parse_bKGD_chunk(chunk);
                    } else if (chunkType.equals("cHRM")) {
                        chunk = readChunk(distream);
                        parse_cHRM_chunk(chunk);
                    } else if (chunkType.equals("gAMA")) {
                        chunk = readChunk(distream);
                        parse_gAMA_chunk(chunk);
                    } else if (chunkType.equals("hIST")) {
                        chunk = readChunk(distream);
                        parse_hIST_chunk(chunk);
                    } else if (chunkType.equals("iCCP")) {
                        chunk = readChunk(distream);
                        parse_iCCP_chunk(chunk);
                    } else if (chunkType.equals("pHYs")) {
                        chunk = readChunk(distream);
                        parse_pHYs_chunk(chunk);
                    } else if (chunkType.equals("sBIT")) {
                        chunk = readChunk(distream);
                        parse_sBIT_chunk(chunk);
                    } else if (chunkType.equals("sRGB")) {
                        chunk = readChunk(distream);
                        parse_sRGB_chunk(chunk);
                    } else if (chunkType.equals("tEXt")) {
                        chunk = readChunk(distream);
                        parse_tEXt_chunk(chunk);
                    } else if (chunkType.equals("tIME")) {
                        chunk = readChunk(distream);
                        parse_tIME_chunk(chunk);
                    } else if (chunkType.equals("tRNS")) {
                        chunk = readChunk(distream);
                        parse_tRNS_chunk(chunk);
                    } else if (chunkType.equals("zTXt")) {
                        chunk = readChunk(distream);
                        parse_zTXt_chunk(chunk);
                    } else {
                        chunk = readChunk(distream);
                        // Output the chunk data in raw form

                        final String type = chunk.getTypeString();
                        final byte[] data = chunk.getData();
                        if (this.encodeParam != null) {
                            this.encodeParam.addPrivateChunk(type, data);
                        }
                        if (this.emitProperties) {
                            final String key = "chunk_" + this.chunkIndex++
                                    + ':' + type;
                            this.properties.put(key.toLowerCase(), data);
                        }
                    }
                } catch (final Exception e) {
                    log.error("Exception", e);
                    final String msg = PropertyUtil
                            .getString("PNGImageDecoder2");
                    throw new RuntimeException(msg);
                }
            } while (true);

            // Final post-processing

            if (this.significantBits == null) {
                this.significantBits = new int[this.inputBands];
                for (int i = 0; i < this.inputBands; ++i) {
                    this.significantBits[i] = this.bitDepth;
                }

                if (this.emitProperties) {
                    this.properties.put("significant_bits",
                            this.significantBits);
                }
            }
        } finally {
            IOUtils.closeQuietly(distream);
            IOUtils.closeQuietly(stream);
        }
    }

    private static String getChunkType(final DataInputStream distream) {
        try {
            distream.mark(8);
            /* int length = */distream.readInt();
            final int type = distream.readInt();
            distream.reset();

            final String typeString = "" + (char) (type >> 24 & 0xff)
                    + (char) (type >> 16 & 0xff) + (char) (type >> 8 & 0xff)
                    + (char) (type & 0xff);
            return typeString;
        } catch (final Exception e) {
            log.error("Exception", e);
            return null;
        }
    }

    private static PNGChunk readChunk(final DataInputStream distream) {
        try {
            final int length = distream.readInt();
            final int type = distream.readInt();
            final byte[] data = new byte[length];
            distream.readFully(data);
            final int crc = distream.readInt();

            return new PNGChunk(length, type, data, crc);
        } catch (final Exception e) {
            log.error("Exception", e);
            return null;
        }
    }

    private void parse_IHDR_chunk(final PNGChunk chunk) {
        final int width = chunk.getInt4(0);
        final int height = chunk.getInt4(4);

        this.bounds = new Rectangle(0, 0, width, height);

        this.bitDepth = chunk.getInt1(8);

        final int validMask = 1 << 1 | 1 << 2 | 1 << 4 | 1 << 8 | 1 << 16;
        if ((1 << this.bitDepth & validMask) == 0) {
            // bitDepth is not one of { 1, 2, 4, 8, 16 }: Error -- bad bit depth
            final String msg = PropertyUtil.getString("PNGImageDecoder3");
            throw new RuntimeException(msg);
        }
        this.maxOpacity = (1 << this.bitDepth) - 1;

        this.colorType = chunk.getInt1(9);
        if (this.colorType != PNG_COLOR_GRAY && this.colorType != PNG_COLOR_RGB
                && this.colorType != PNG_COLOR_PALETTE
                && this.colorType != PNG_COLOR_GRAY_ALPHA
                && this.colorType != PNG_COLOR_RGB_ALPHA) {
            log.info(PropertyUtil.getString("PNGImageDecoder4"));
        }

        if (this.colorType == PNG_COLOR_RGB && this.bitDepth < 8) {
            // Error -- RGB images must have 8 or 16 bits
            final String msg = PropertyUtil.getString("PNGImageDecoder5");
            throw new RuntimeException(msg);
        }

        if (this.colorType == PNG_COLOR_PALETTE && this.bitDepth == 16) {
            // Error -- palette images must have < 16 bits
            final String msg = PropertyUtil.getString("PNGImageDecoder6");
            throw new RuntimeException(msg);
        }

        if (this.colorType == PNG_COLOR_GRAY_ALPHA && this.bitDepth < 8) {
            // Error -- gray/alpha images must have >= 8 bits
            final String msg = PropertyUtil.getString("PNGImageDecoder7");
            throw new RuntimeException(msg);
        }

        if (this.colorType == PNG_COLOR_RGB_ALPHA && this.bitDepth < 8) {
            // Error -- RGB/alpha images must have >= 8 bits
            final String msg = PropertyUtil.getString("PNGImageDecoder8");
            throw new RuntimeException(msg);
        }

        if (this.emitProperties) {
            this.properties.put("color_type", colorTypeNames[this.colorType]);
        }

        if (this.generateEncodeParam) {
            if (this.colorType == PNG_COLOR_PALETTE) {
                this.encodeParam = new PNGEncodeParam.Palette();
            } else if (this.colorType == PNG_COLOR_GRAY
                    || this.colorType == PNG_COLOR_GRAY_ALPHA) {
                this.encodeParam = new PNGEncodeParam.Gray();
            } else {
                this.encodeParam = new PNGEncodeParam.RGB();
            }
            this.decodeParam.setEncodeParam(this.encodeParam);
        }

        if (this.encodeParam != null) {
            this.encodeParam.setBitDepth(this.bitDepth);
        }
        if (this.emitProperties) {
            this.properties.put("bit_depth", this.bitDepth);
        }

        if (this.performGammaCorrection) {
            // Assume file gamma is 1/2.2 unless we get a gAMA chunk
            final float gamma = 1.0F / 2.2F * (this.displayExponent / this.userExponent);
            if (this.encodeParam != null) {
                this.encodeParam.setGamma(gamma);
            }
            if (this.emitProperties) {
                this.properties.put("gamma", new Float(gamma));
            }
        }

        this.compressionMethod = chunk.getInt1(10);
        if (this.compressionMethod != 0) {
            // Error -- only know about compression method 0
            final String msg = PropertyUtil.getString("PNGImageDecoder9");
            throw new RuntimeException(msg);
        }

        this.filterMethod = chunk.getInt1(11);
        if (this.filterMethod != 0) {
            // Error -- only know about filter method 0
            final String msg = PropertyUtil.getString("PNGImageDecoder10");
            throw new RuntimeException(msg);
        }

        this.interlaceMethod = chunk.getInt1(12);
        if (this.interlaceMethod == 0) {
            if (this.encodeParam != null) {
                this.encodeParam.setInterlacing(false);
            }
            if (this.emitProperties) {
                this.properties.put("interlace_method", "None");
            }
        } else if (this.interlaceMethod == 1) {
            if (this.encodeParam != null) {
                this.encodeParam.setInterlacing(true);
            }
            if (this.emitProperties) {
                this.properties.put("interlace_method", "Adam7");
            }
        } else {
            // Error -- only know about Adam7 interlacing
            final String msg = PropertyUtil.getString("PNGImageDecoder11");
            throw new RuntimeException(msg);
        }

        this.bytesPerPixel = this.bitDepth == 16 ? 2 : 1;

        switch (this.colorType) {
        case PNG_COLOR_GRAY:
            this.inputBands = 1;
            this.outputBands = 1;

            if (this.output8BitGray && this.bitDepth < 8) {
                this.postProcess = POST_GRAY_LUT;
            } else if (this.performGammaCorrection) {
                this.postProcess = POST_GAMMA;
            } else {
                this.postProcess = POST_NONE;
            }
            break;

        case PNG_COLOR_RGB:
            this.inputBands = 3;
            this.bytesPerPixel *= 3;
            this.outputBands = 3;

            if (this.performGammaCorrection) {
                this.postProcess = POST_GAMMA;
            } else {
                this.postProcess = POST_NONE;
            }
            break;

        case PNG_COLOR_PALETTE:
            this.inputBands = 1;
            this.bytesPerPixel = 1;
            this.outputBands = this.expandPalette ? 3 : 1;

            if (this.expandPalette) {
                this.postProcess = POST_PALETTE_TO_RGB;
            } else {
                this.postProcess = POST_NONE;
            }
            break;

        case PNG_COLOR_GRAY_ALPHA:
            this.inputBands = 2;
            this.bytesPerPixel *= 2;

            if (this.suppressAlpha) {
                this.outputBands = 1;
                this.postProcess = POST_REMOVE_GRAY_TRANS;
            } else {
                if (this.performGammaCorrection) {
                    this.postProcess = POST_GAMMA;
                } else {
                    this.postProcess = POST_NONE;
                }
                if (this.expandGrayAlpha) {
                    this.postProcess |= POST_EXP_MASK;
                    this.outputBands = 4;
                } else {
                    this.outputBands = 2;
                }
            }
            break;

        case PNG_COLOR_RGB_ALPHA:
            this.inputBands = 4;
            this.bytesPerPixel *= 4;
            this.outputBands = !this.suppressAlpha ? 4 : 3;

            if (this.suppressAlpha) {
                this.postProcess = POST_REMOVE_RGB_TRANS;
            } else if (this.performGammaCorrection) {
                this.postProcess = POST_GAMMA;
            } else {
                this.postProcess = POST_NONE;
            }
            break;
        }
    }

    private void parse_IEND_chunk(final PNGChunk chunk) throws IOException {
        // Store text strings
        final int textLen = this.textKeys.size();
        final String[] textArray = new String[2 * textLen];
        for (int i = 0; i < textLen; ++i) {
            final String key = this.textKeys.get(i);
            final String val = this.textStrings.get(i);
            textArray[2 * i] = key;
            textArray[2 * i + 1] = val;
            if (this.emitProperties) {
                final String uniqueKey = "text_" + i + ':' + key;
                this.properties.put(uniqueKey.toLowerCase(), val);
            }
        }
        if (this.encodeParam != null) {
            this.encodeParam.setText(textArray);
        }

        // Store compressed text strings
        final int ztextLen = this.ztextKeys.size();
        final String[] ztextArray = new String[2 * ztextLen];
        for (int i = 0; i < ztextLen; ++i) {
            final String key = this.ztextKeys.get(i);
            final String val = this.ztextStrings.get(i);
            ztextArray[2 * i] = key;
            ztextArray[2 * i + 1] = val;
            if (this.emitProperties) {
                final String uniqueKey = "ztext_" + i + ':' + key;
                this.properties.put(uniqueKey.toLowerCase(), val);
            }
        }
        if (this.encodeParam != null) {
            this.encodeParam.setCompressedText(ztextArray);
        }

        // Parse prior IDAT chunks
        final InputStream seqStream = new SequenceInputStream(
                Collections.enumeration(this.streamVec));
        final InputStream infStream = new InflaterInputStream(seqStream,
                new Inflater());
        this.dataStream = new DataInputStream(infStream);

        // Create an empty WritableRaster
        int depth = this.bitDepth;
        if (this.colorType == PNG_COLOR_GRAY && this.bitDepth < 8
                && this.output8BitGray) {
            depth = 8;
        }
        if (this.colorType == PNG_COLOR_PALETTE && this.expandPalette) {
            depth = 8;
        }
        final int width = this.bounds.width;
        final int height = this.bounds.height;

        final int bytesPerRow = (this.outputBands * width * depth + 7) / 8;
        final int scanlineStride = depth == 16 ? bytesPerRow / 2 : bytesPerRow;

        this.theTile = createRaster(width, height, this.outputBands,
                scanlineStride, depth);

        if (this.performGammaCorrection && this.gammaLut == null) {
            initGammaLut(this.bitDepth);
        }
        if (this.postProcess == POST_GRAY_LUT
                || this.postProcess == POST_GRAY_LUT_ADD_TRANS
                || this.postProcess == POST_GRAY_LUT_ADD_TRANS_EXP) {
            initGrayLut(this.bitDepth);
        }

        decodeImage(this.interlaceMethod == 1);

        // Free resources associated with compressed data.
        this.dataStream.close();
        infStream.close();
        seqStream.close();
        this.streamVec = null;

        final SampleModel sm = this.theTile.getSampleModel();
        ColorModel cm;

        if (this.colorType == PNG_COLOR_PALETTE && !this.expandPalette) {
            if (this.outputHasAlphaPalette) {
                cm = new IndexColorModel(this.bitDepth, this.paletteEntries,
                        this.redPalette, this.greenPalette, this.bluePalette,
                        this.alphaPalette);
            } else {
                cm = new IndexColorModel(this.bitDepth, this.paletteEntries,
                        this.redPalette, this.greenPalette, this.bluePalette);
            }
        } else if (this.colorType == PNG_COLOR_GRAY && this.bitDepth < 8
                && !this.output8BitGray) {
            final byte[] palette = this.expandBits[this.bitDepth];
            cm = new IndexColorModel(this.bitDepth, palette.length, palette,
                    palette, palette);
        } else {
            cm = createComponentColorModel(sm);
        }

        init((CachableRed) null, this.bounds, cm, sm, 0, 0, this.properties);
    }

    private static final int[] GrayBits8 = { 8 };
    private static final ComponentColorModel colorModelGray8 = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_GRAY), GrayBits8, false,
            false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

    private static final int[] GrayAlphaBits8 = { 8, 8 };
    private static final ComponentColorModel colorModelGrayAlpha8 = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_GRAY), GrayAlphaBits8, true,
            false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

    private static final int[] GrayBits16 = { 16 };
    private static final ComponentColorModel colorModelGray16 = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_GRAY), GrayBits16, false,
            false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);

    private static final int[] GrayAlphaBits16 = { 16, 16 };
    private static final ComponentColorModel colorModelGrayAlpha16 = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_GRAY), GrayAlphaBits16, true,
            false, Transparency.TRANSLUCENT, DataBuffer.TYPE_USHORT);

    private static final int[] GrayBits32 = { 32 };
    private static final ComponentColorModel colorModelGray32 = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_GRAY), GrayBits32, false,
            false, Transparency.OPAQUE, DataBuffer.TYPE_INT);

    private static final int[] GrayAlphaBits32 = { 32, 32 };
    private static final ComponentColorModel colorModelGrayAlpha32 = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_GRAY), GrayAlphaBits32, true,
            false, Transparency.TRANSLUCENT, DataBuffer.TYPE_INT);

    private static final int[] RGBBits8 = { 8, 8, 8 };
    private static final ComponentColorModel colorModelRGB8 = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB), RGBBits8, false, false,
            Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

    private static final int[] RGBABits8 = { 8, 8, 8, 8 };
    private static final ComponentColorModel colorModelRGBA8 = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB), RGBABits8, true, false,
            Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

    private static final int[] RGBBits16 = { 16, 16, 16 };
    private static final ComponentColorModel colorModelRGB16 = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB), RGBBits16, false,
            false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);

    private static final int[] RGBABits16 = { 16, 16, 16, 16 };
    private static final ComponentColorModel colorModelRGBA16 = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB), RGBABits16, true,
            false, Transparency.TRANSLUCENT, DataBuffer.TYPE_USHORT);

    private static final int[] RGBBits32 = { 32, 32, 32 };
    private static final ComponentColorModel colorModelRGB32 = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB), RGBBits32, false,
            false, Transparency.OPAQUE, DataBuffer.TYPE_INT);

    private static final int[] RGBABits32 = { 32, 32, 32, 32 };
    private static final ComponentColorModel colorModelRGBA32 = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB), RGBABits32, true,
            false, Transparency.TRANSLUCENT, DataBuffer.TYPE_INT);

    /**
     * A convenience method to create an instance of
     * <code>ComponentColorModel</code> suitable for use with the given
     * <code>SampleModel</code>. The <code>SampleModel</code> should have a data
     * type of <code>DataBuffer.TYPE_BYTE</code>, <code>TYPE_USHORT</code>, or
     * <code>TYPE_INT</code> and between 1 and 4 bands. Depending on the number
     * of bands of the <code>SampleModel</code>, either a gray, gray+alpha, rgb,
     * or rgb+alpha <code>ColorModel</code> is returned.
     */
    public static ColorModel createComponentColorModel(final SampleModel sm) {
        final int type = sm.getDataType();
        final int bands = sm.getNumBands();
        ComponentColorModel cm = null;

        if (type == DataBuffer.TYPE_BYTE) {
            switch (bands) {
            case 1:
                cm = colorModelGray8;
                break;
            case 2:
                cm = colorModelGrayAlpha8;
                break;
            case 3:
                cm = colorModelRGB8;
                break;
            case 4:
                cm = colorModelRGBA8;
                break;
            }
        } else if (type == DataBuffer.TYPE_USHORT) {
            switch (bands) {
            case 1:
                cm = colorModelGray16;
                break;
            case 2:
                cm = colorModelGrayAlpha16;
                break;
            case 3:
                cm = colorModelRGB16;
                break;
            case 4:
                cm = colorModelRGBA16;
                break;
            }
        } else if (type == DataBuffer.TYPE_INT) {
            switch (bands) {
            case 1:
                cm = colorModelGray32;
                break;
            case 2:
                cm = colorModelGrayAlpha32;
                break;
            case 3:
                cm = colorModelRGB32;
                break;
            case 4:
                cm = colorModelRGBA32;
                break;
            }
        }

        return cm;
    }

    private void parse_PLTE_chunk(final PNGChunk chunk) {
        this.paletteEntries = chunk.getLength() / 3;
        this.redPalette = new byte[this.paletteEntries];
        this.greenPalette = new byte[this.paletteEntries];
        this.bluePalette = new byte[this.paletteEntries];

        int pltIndex = 0;

        // gAMA chunk must precede PLTE chunk
        if (this.performGammaCorrection) {
            if (this.gammaLut == null) {
                initGammaLut(this.bitDepth == 16 ? 16 : 8);
            }

            for (int i = 0; i < this.paletteEntries; ++i) {
                final byte r = chunk.getByte(pltIndex++);
                final byte g = chunk.getByte(pltIndex++);
                final byte b = chunk.getByte(pltIndex++);

                this.redPalette[i] = (byte) this.gammaLut[r & 0xff];
                this.greenPalette[i] = (byte) this.gammaLut[g & 0xff];
                this.bluePalette[i] = (byte) this.gammaLut[b & 0xff];
            }
        } else {
            for (int i = 0; i < this.paletteEntries; ++i) {
                this.redPalette[i] = chunk.getByte(pltIndex++);
                this.greenPalette[i] = chunk.getByte(pltIndex++);
                this.bluePalette[i] = chunk.getByte(pltIndex++);
            }
        }
    }

    private void parse_bKGD_chunk(final PNGChunk chunk) {
        switch (this.colorType) {
        case PNG_COLOR_PALETTE:
            final int bkgdIndex = chunk.getByte(0) & 0xff;

            this.bkgdRed = this.redPalette[bkgdIndex] & 0xff;
            this.bkgdGreen = this.greenPalette[bkgdIndex] & 0xff;
            this.bkgdBlue = this.bluePalette[bkgdIndex] & 0xff;

            if (this.encodeParam != null) {
                ((PNGEncodeParam.Palette) this.encodeParam)
                .setBackgroundPaletteIndex(bkgdIndex);
            }
            break;
        case PNG_COLOR_GRAY:
        case PNG_COLOR_GRAY_ALPHA:
            final int bkgdGray = chunk.getInt2(0);
            this.bkgdRed = this.bkgdGreen = this.bkgdBlue = bkgdGray;

            if (this.encodeParam != null) {
                ((PNGEncodeParam.Gray) this.encodeParam)
                .setBackgroundGray(bkgdGray);
            }
            break;
        case PNG_COLOR_RGB:
        case PNG_COLOR_RGB_ALPHA:
            this.bkgdRed = chunk.getInt2(0);
            this.bkgdGreen = chunk.getInt2(2);
            this.bkgdBlue = chunk.getInt2(4);

            final int[] bkgdRGB = new int[3];
            bkgdRGB[0] = this.bkgdRed;
            bkgdRGB[1] = this.bkgdGreen;
            bkgdRGB[2] = this.bkgdBlue;
            if (this.encodeParam != null) {
                ((PNGEncodeParam.RGB) this.encodeParam)
                .setBackgroundRGB(bkgdRGB);
            }
            break;
        }

        if (this.emitProperties) {
            int r = 0, g = 0, b = 0;
            if (this.colorType == PNG_COLOR_PALETTE || this.bitDepth == 8) {
                r = this.bkgdRed;
                g = this.bkgdGreen;
                b = this.bkgdBlue;
            } else if (this.bitDepth < 8) {
                r = this.expandBits[this.bitDepth][this.bkgdRed];
                g = this.expandBits[this.bitDepth][this.bkgdGreen];
                b = this.expandBits[this.bitDepth][this.bkgdBlue];
            } else if (this.bitDepth == 16) {
                r = this.bkgdRed >> 8;
                g = this.bkgdGreen >> 8;
                b = this.bkgdBlue >> 8;
            }
            this.properties.put("background_color", new Color(r, g, b));
        }
    }

    private void parse_cHRM_chunk(final PNGChunk chunk) {
        // If an sRGB chunk exists, ignore cHRM chunks
        if (this.sRGBRenderingIntent != -1) {
            return;
        }

        this.chromaticity = new float[8];
        this.chromaticity[0] = chunk.getInt4(0) / 100000.0F;
        this.chromaticity[1] = chunk.getInt4(4) / 100000.0F;
        this.chromaticity[2] = chunk.getInt4(8) / 100000.0F;
        this.chromaticity[3] = chunk.getInt4(12) / 100000.0F;
        this.chromaticity[4] = chunk.getInt4(16) / 100000.0F;
        this.chromaticity[5] = chunk.getInt4(20) / 100000.0F;
        this.chromaticity[6] = chunk.getInt4(24) / 100000.0F;
        this.chromaticity[7] = chunk.getInt4(28) / 100000.0F;

        if (this.encodeParam != null) {
            this.encodeParam.setChromaticity(this.chromaticity);
        }
        if (this.emitProperties) {
            this.properties.put("white_point_x",
                    new Float(this.chromaticity[0]));
            this.properties.put("white_point_y",
                    new Float(this.chromaticity[1]));
            this.properties.put("red_x", new Float(this.chromaticity[2]));
            this.properties.put("red_y", new Float(this.chromaticity[3]));
            this.properties.put("green_x", new Float(this.chromaticity[4]));
            this.properties.put("green_y", new Float(this.chromaticity[5]));
            this.properties.put("blue_x", new Float(this.chromaticity[6]));
            this.properties.put("blue_y", new Float(this.chromaticity[7]));
        }
    }

    private void parse_gAMA_chunk(final PNGChunk chunk) {
        // If an sRGB chunk exists, ignore gAMA chunks
        if (this.sRGBRenderingIntent != -1) {
            return;
        }

        this.fileGamma = chunk.getInt4(0) / 100000.0F;
        // log.info("Gamma: " + fileGamma);
        final float exp = this.performGammaCorrection ? this.displayExponent
                / this.userExponent : 1.0F;
        if (this.encodeParam != null) {
            this.encodeParam.setGamma(this.fileGamma * exp);
        }
        if (this.emitProperties) {
            this.properties.put("gamma", new Float(this.fileGamma * exp));
        }
    }

    private void parse_hIST_chunk(final PNGChunk chunk) {
        if (this.redPalette == null) {
            final String msg = PropertyUtil.getString("PNGImageDecoder18");
            throw new RuntimeException(msg);
        }

        final int length = this.redPalette.length;
        final int[] hist = new int[length];
        for (int i = 0; i < length; ++i) {
            hist[i] = chunk.getInt2(2 * i);
        }

        if (this.encodeParam != null) {
            this.encodeParam.setPaletteHistogram(hist);
        }
    }

    private void parse_iCCP_chunk(final PNGChunk chunk) {
        final StringBuilder name = new StringBuilder("");
        byte b;

        int textIndex = 0;
        while ((b = chunk.getByte(textIndex++)) != 0) {
            name.append((char) b);
        }
    }

    private void parse_pHYs_chunk(final PNGChunk chunk) {
        final int xPixelsPerUnit = chunk.getInt4(0);
        final int yPixelsPerUnit = chunk.getInt4(4);
        final int unitSpecifier = chunk.getInt1(8);

        if (this.encodeParam != null) {
            this.encodeParam.setPhysicalDimension(xPixelsPerUnit,
                    yPixelsPerUnit, unitSpecifier);
        }
        if (this.emitProperties) {
            this.properties.put("x_pixels_per_unit", xPixelsPerUnit);
            this.properties.put("y_pixels_per_unit", yPixelsPerUnit);
            this.properties.put("pixel_aspect_ratio", new Float(
                    (float) xPixelsPerUnit / yPixelsPerUnit));
            if (unitSpecifier == 1) {
                this.properties.put("pixel_units", "Meters");
            } else if (unitSpecifier != 0) {
                // Error -- unit specifier must be 0 or 1
                final String msg = PropertyUtil.getString("PNGImageDecoder12");
                throw new RuntimeException(msg);
            }
        }
    }

    private void parse_sBIT_chunk(final PNGChunk chunk) {
        if (this.colorType == PNG_COLOR_PALETTE) {
            this.significantBits = new int[3];
        } else {
            this.significantBits = new int[this.inputBands];
        }
        for (int i = 0; i < this.significantBits.length; ++i) {
            final int bits = chunk.getByte(i);
            final int depth = this.colorType == PNG_COLOR_PALETTE ? 8
                    : this.bitDepth;
            if (bits <= 0 || bits > depth) {
                // Error -- significant bits must be between 0 and
                // image bit depth.
                final String msg = PropertyUtil.getString("PNGImageDecoder13");
                throw new RuntimeException(msg);
            }
            this.significantBits[i] = bits;
        }

        if (this.encodeParam != null) {
            this.encodeParam.setSignificantBits(this.significantBits);
        }
        if (this.emitProperties) {
            this.properties.put("significant_bits", this.significantBits);
        }
    }

    private void parse_sRGB_chunk(final PNGChunk chunk) {
        this.sRGBRenderingIntent = chunk.getByte(0);

        // The presence of an sRGB chunk implies particular
        // settings for gamma and chroma.
        this.fileGamma = 45455 / 100000.0F;

        this.chromaticity = new float[8];
        this.chromaticity[0] = 31270 / 10000.0F;
        this.chromaticity[1] = 32900 / 10000.0F;
        this.chromaticity[2] = 64000 / 10000.0F;
        this.chromaticity[3] = 33000 / 10000.0F;
        this.chromaticity[4] = 30000 / 10000.0F;
        this.chromaticity[5] = 60000 / 10000.0F;
        this.chromaticity[6] = 15000 / 10000.0F;
        this.chromaticity[7] = 6000 / 10000.0F;

        if (this.performGammaCorrection) {
            // File gamma is 1/2.2
            final float gamma = this.fileGamma
                    * (this.displayExponent / this.userExponent);
            if (this.encodeParam != null) {
                this.encodeParam.setGamma(gamma);
                this.encodeParam.setChromaticity(this.chromaticity);
            }
            if (this.emitProperties) {
                this.properties.put("gamma", new Float(gamma));
                this.properties.put("white_point_x", new Float(
                        this.chromaticity[0]));
                this.properties.put("white_point_y", new Float(
                        this.chromaticity[1]));
                this.properties.put("red_x", new Float(this.chromaticity[2]));
                this.properties.put("red_y", new Float(this.chromaticity[3]));
                this.properties.put("green_x", new Float(this.chromaticity[4]));
                this.properties.put("green_y", new Float(this.chromaticity[5]));
                this.properties.put("blue_x", new Float(this.chromaticity[6]));
                this.properties.put("blue_y", new Float(this.chromaticity[7]));
            }
        }
    }

    private void parse_tEXt_chunk(final PNGChunk chunk) {
        final StringBuilder key = new StringBuilder();
        final StringBuilder value = new StringBuilder();
        byte b;

        int textIndex = 0;
        while ((b = chunk.getByte(textIndex++)) != 0) {
            key.append((char) b);
        }

        for (int i = textIndex; i < chunk.getLength(); ++i) {
            value.append((char) chunk.getByte(i));
        }

        this.textKeys.add(key.toString());
        this.textStrings.add(value.toString());
    }

    private void parse_tIME_chunk(final PNGChunk chunk) {
        final int year = chunk.getInt2(0);
        final int month = chunk.getInt1(2) - 1;
        final int day = chunk.getInt1(3);
        final int hour = chunk.getInt1(4);
        final int minute = chunk.getInt1(5);
        final int second = chunk.getInt1(6);

        final TimeZone gmt = TimeZone.getTimeZone("GMT");

        final GregorianCalendar cal = new GregorianCalendar(gmt);
        cal.set(year, month, day, hour, minute, second);
        final Date date = cal.getTime();

        if (this.encodeParam != null) {
            this.encodeParam.setModificationTime(date);
        }
        if (this.emitProperties) {
            this.properties.put("timestamp", date);
        }
    }

    private void parse_tRNS_chunk(final PNGChunk chunk) {
        if (this.colorType == PNG_COLOR_PALETTE) {
            final int entries = chunk.getLength();
            if (entries > this.paletteEntries) {
                // Error -- mustn't have more alpha than RGB palette entries
                final String msg = PropertyUtil.getString("PNGImageDecoder14");
                throw new RuntimeException(msg);
            }

            // Load beginning of palette from the chunk
            this.alphaPalette = new byte[this.paletteEntries];
            for (int i = 0; i < entries; ++i) {
                this.alphaPalette[i] = chunk.getByte(i);
            }

            // Fill rest of palette with 255
            for (int i = entries; i < this.paletteEntries; ++i) {
                this.alphaPalette[i] = (byte) 255;
            }

            if (!this.suppressAlpha) {
                if (this.expandPalette) {
                    this.postProcess = POST_PALETTE_TO_RGBA;
                    this.outputBands = 4;
                } else {
                    this.outputHasAlphaPalette = true;
                }
            }
        } else if (this.colorType == PNG_COLOR_GRAY) {
            this.grayTransparentAlpha = chunk.getInt2(0);

            if (!this.suppressAlpha) {
                if (this.bitDepth < 8) {
                    this.output8BitGray = true;
                    this.maxOpacity = 255;
                    this.postProcess = POST_GRAY_LUT_ADD_TRANS;
                } else {
                    this.postProcess = POST_ADD_GRAY_TRANS;
                }

                if (this.expandGrayAlpha) {
                    this.outputBands = 4;
                    this.postProcess |= POST_EXP_MASK;
                } else {
                    this.outputBands = 2;
                }

                if (this.encodeParam != null) {
                    ((PNGEncodeParam.Gray) this.encodeParam)
                            .setTransparentGray(this.grayTransparentAlpha);
                }
            }
        } else if (this.colorType == PNG_COLOR_RGB) {
            this.redTransparentAlpha = chunk.getInt2(0);
            this.greenTransparentAlpha = chunk.getInt2(2);
            this.blueTransparentAlpha = chunk.getInt2(4);

            if (!this.suppressAlpha) {
                this.outputBands = 4;
                this.postProcess = POST_ADD_RGB_TRANS;

                if (this.encodeParam != null) {
                    final int[] rgbTrans = new int[3];
                    rgbTrans[0] = this.redTransparentAlpha;
                    rgbTrans[1] = this.greenTransparentAlpha;
                    rgbTrans[2] = this.blueTransparentAlpha;
                    ((PNGEncodeParam.RGB) this.encodeParam)
                            .setTransparentRGB(rgbTrans);
                }
            }
        } else if (this.colorType == PNG_COLOR_GRAY_ALPHA
                || this.colorType == PNG_COLOR_RGB_ALPHA) {
            // Error -- GA or RGBA image can't have a tRNS chunk.
            final String msg = PropertyUtil.getString("PNGImageDecoder15");
            throw new RuntimeException(msg);
        }
    }

    private void parse_zTXt_chunk(final PNGChunk chunk) {
        final StringBuilder key = new StringBuilder();
        final StringBuilder value = new StringBuilder();
        byte b;

        int textIndex = 0;
        while ((b = chunk.getByte(textIndex++)) != 0) {
            key.append((char) b);
        }
        /* int method = */chunk.getByte(textIndex++);

        try {
            final int length = chunk.getLength() - textIndex;
            final byte[] data = chunk.getData();
            final InputStream cis = new ByteArrayInputStream(data, textIndex,
                    length);
            final InputStream iis = new InflaterInputStream(cis);

            int c;
            while ((c = iis.read()) != -1) {
                value.append((char) c);
            }

            this.ztextKeys.add(key.toString());
            this.ztextStrings.add(value.toString());
        } catch (final Exception e) {
            log.error("Exception", e);
        }
    }

    private WritableRaster createRaster(final int width, final int height,
            final int bands, final int scanlineStride, final int bitDepth) {

        DataBuffer dataBuffer;
        WritableRaster ras = null;
        final Point origin = new Point(0, 0);
        if (bitDepth < 8 && bands == 1) {
            dataBuffer = new DataBufferByte(height * scanlineStride);
            ras = Raster.createPackedRaster(dataBuffer, width, height,
                    bitDepth, origin);
        } else if (bitDepth <= 8) {
            dataBuffer = new DataBufferByte(height * scanlineStride);
            ras = Raster.createInterleavedRaster(dataBuffer, width, height,
                    scanlineStride, bands, this.bandOffsets[bands], origin);
        } else {
            dataBuffer = new DataBufferUShort(height * scanlineStride);
            ras = Raster.createInterleavedRaster(dataBuffer, width, height,
                    scanlineStride, bands, this.bandOffsets[bands], origin);
        }

        return ras;
    }

    // Data filtering methods

    private static void decodeSubFilter(final byte[] curr, final int count,
            final int bpp) {
        for (int i = bpp; i < count; ++i) {
            int val;

            val = curr[i] & 0xff;
            val += curr[i - bpp] & 0xff;

            curr[i] = (byte) val;
        }
    }

    private static void decodeUpFilter(final byte[] curr, final byte[] prev,
            final int count) {
        for (int i = 0; i < count; ++i) {
            final int raw = curr[i] & 0xff;
            final int prior = prev[i] & 0xff;

            curr[i] = (byte) (raw + prior);
        }
    }

    private static void decodeAverageFilter(final byte[] curr,
            final byte[] prev, final int count, final int bpp) {
        for (int i = 0; i < bpp; ++i) {
            final int raw = curr[i] & 0xff;
            final int priorRow = prev[i] & 0xff;

            curr[i] = (byte) (raw + priorRow / 2);
        }

        for (int i = bpp; i < count; ++i) {
            final int raw = curr[i] & 0xff;
            final int priorPixel = curr[i - bpp] & 0xff;
            final int priorRow = prev[i] & 0xff;

            curr[i] = (byte) (raw + (priorPixel + priorRow) / 2);
        }
    }

    private static int paethPredictor(final int a, final int b, final int c) {
        final int p = a + b - c;
        final int pa = Math.abs(p - a);
        final int pb = Math.abs(p - b);
        final int pc = Math.abs(p - c);

        if (pa <= pb && pa <= pc) {
            return a;
        } else if (pb <= pc) {
            return b;
        } else {
            return c;
        }
    }

    private static void decodePaethFilter(final byte[] curr, final byte[] prev,
            final int count, final int bpp) {
        int priorPixel, priorRowPixel;

        for (int i = 0; i < bpp; ++i) {
            final int raw = curr[i] & 0xff;
            final int priorRow = prev[i] & 0xff;

            curr[i] = (byte) (raw + priorRow);
        }

        for (int i = bpp; i < count; ++i) {
            final int raw = curr[i] & 0xff;
            priorPixel = curr[i - bpp] & 0xff;
            final int priorRow = prev[i] & 0xff;
            priorRowPixel = prev[i - bpp] & 0xff;

            curr[i] = (byte) (raw + paethPredictor(priorPixel, priorRow,
                    priorRowPixel));
        }
    }

    private void processPixels(final int process, final Raster src,
            final WritableRaster dst, final int xOffset, final int step,
            final int y, final int width) {
        int srcX, dstX;

        // Create an array suitable for holding one pixel
        final int[] ps = src.getPixel(0, 0, (int[]) null);
        final int[] pd = dst.getPixel(0, 0, (int[]) null);

        dstX = xOffset;
        switch (process) {
        case POST_NONE:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);
                dst.setPixel(dstX, y, ps);
                dstX += step;
            }
            break;

        case POST_GAMMA:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                for (int i = 0; i < this.inputBands; ++i) {
                    final int x = ps[i];
                    ps[i] = this.gammaLut[x];
                }

                dst.setPixel(dstX, y, ps);
                dstX += step;
            }
            break;

        case POST_GRAY_LUT:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                pd[0] = this.grayLut[ps[0]];

                dst.setPixel(dstX, y, pd);
                dstX += step;
            }
            break;

        case POST_GRAY_LUT_ADD_TRANS:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                final int val = ps[0];
                pd[0] = this.grayLut[val];
                if (val == this.grayTransparentAlpha) {
                    pd[1] = 0;
                } else {
                    pd[1] = this.maxOpacity;
                }

                dst.setPixel(dstX, y, pd);
                dstX += step;
            }
            break;

        case POST_PALETTE_TO_RGB:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                final int val = ps[0];
                pd[0] = this.redPalette[val];
                pd[1] = this.greenPalette[val];
                pd[2] = this.bluePalette[val];

                dst.setPixel(dstX, y, pd);
                dstX += step;
            }
            break;

        case POST_PALETTE_TO_RGBA:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                final int val = ps[0];
                pd[0] = this.redPalette[val];
                pd[1] = this.greenPalette[val];
                pd[2] = this.bluePalette[val];
                pd[3] = this.alphaPalette[val];

                dst.setPixel(dstX, y, pd);
                dstX += step;
            }
            break;

        case POST_ADD_GRAY_TRANS:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                int val = ps[0];
                if (this.performGammaCorrection) {
                    val = this.gammaLut[val];
                }
                pd[0] = val;
                if (val == this.grayTransparentAlpha) {
                    pd[1] = 0;
                } else {
                    pd[1] = this.maxOpacity;
                }

                dst.setPixel(dstX, y, pd);
                dstX += step;
            }
            break;

        case POST_ADD_RGB_TRANS:
            final boolean flagGammaCorrection = this.performGammaCorrection; // local
            // is
            // cheaper
            final int[] workGammaLut = this.gammaLut;
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                final int r = ps[0];
                final int g = ps[1];
                final int b = ps[2];
                if (flagGammaCorrection) {
                    pd[0] = workGammaLut[r];
                    pd[1] = workGammaLut[g];
                    pd[2] = workGammaLut[b];
                } else {
                    pd[0] = r;
                    pd[1] = g;
                    pd[2] = b;
                }
                if (r == this.redTransparentAlpha
                        && g == this.greenTransparentAlpha
                        && b == this.blueTransparentAlpha) {
                    pd[3] = 0;
                } else {
                    pd[3] = this.maxOpacity;
                }

                dst.setPixel(dstX, y, pd);
                dstX += step;
            }
            break;

        case POST_REMOVE_GRAY_TRANS:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                final int g = ps[0];
                if (this.performGammaCorrection) {
                    pd[0] = this.gammaLut[g];
                } else {
                    pd[0] = g;
                }

                dst.setPixel(dstX, y, pd);
                dstX += step;
            }
            break;

        case POST_REMOVE_RGB_TRANS:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                final int r = ps[0];
                final int g = ps[1];
                final int b = ps[2];
                if (this.performGammaCorrection) {
                    pd[0] = this.gammaLut[r];
                    pd[1] = this.gammaLut[g];
                    pd[2] = this.gammaLut[b];
                } else {
                    pd[0] = r;
                    pd[1] = g;
                    pd[2] = b;
                }

                dst.setPixel(dstX, y, pd);
                dstX += step;
            }
            break;

        case POST_GAMMA_EXP:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                final int val = ps[0];
                final int alpha = ps[1];
                final int gamma = this.gammaLut[val];
                pd[0] = gamma;
                pd[1] = gamma;
                pd[2] = gamma;
                pd[3] = alpha;

                dst.setPixel(dstX, y, pd);
                dstX += step;
            }
            break;

        case POST_GRAY_ALPHA_EXP:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                final int val = ps[0];
                final int alpha = ps[1];
                pd[0] = val;
                pd[1] = val;
                pd[2] = val;
                pd[3] = alpha;

                dst.setPixel(dstX, y, pd);
                dstX += step;
            }
            break;

        case POST_ADD_GRAY_TRANS_EXP:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                int val = ps[0];
                if (this.performGammaCorrection) {
                    val = this.gammaLut[val];
                }
                pd[0] = val;
                pd[1] = val;
                pd[2] = val;
                if (val == this.grayTransparentAlpha) {
                    pd[3] = 0;
                } else {
                    pd[3] = this.maxOpacity;
                }

                dst.setPixel(dstX, y, pd);
                dstX += step;
            }
            break;

        case POST_GRAY_LUT_ADD_TRANS_EXP:
            for (srcX = 0; srcX < width; srcX++) {
                src.getPixel(srcX, 0, ps);

                final int val = ps[0];
                final int val2 = this.grayLut[val];
                pd[0] = val2;
                pd[1] = val2;
                pd[2] = val2;
                if (val == this.grayTransparentAlpha) {
                    pd[3] = 0;
                } else {
                    pd[3] = this.maxOpacity;
                }

                dst.setPixel(dstX, y, pd);
                dstX += step;
            }
            break;
        }
    }

    /**
     * Reads in an image of a given size and returns it as a WritableRaster.
     */
    private void decodePass(final WritableRaster imRas, final int xOffset,
            final int yOffset, final int xStep, final int yStep,
            final int passWidth, final int passHeight) {
        if (passWidth == 0 || passHeight == 0) {
            return;
        }

        final int bytesPerRow = (this.inputBands * passWidth * this.bitDepth + 7) / 8;
        final int eltsPerRow = this.bitDepth == 16 ? bytesPerRow / 2
                : bytesPerRow;
        byte[] curr = new byte[bytesPerRow];
        byte[] prior = new byte[bytesPerRow];

        // Create a 1-row tall Raster to hold the data
        final WritableRaster passRow = createRaster(passWidth, 1,
                this.inputBands, eltsPerRow, this.bitDepth);
        final DataBuffer dataBuffer = passRow.getDataBuffer();
        final int type = dataBuffer.getDataType();
        byte[] byteData = null;
        short[] shortData = null;
        if (type == DataBuffer.TYPE_BYTE) {
            byteData = ((DataBufferByte) dataBuffer).getData();
        } else {
            shortData = ((DataBufferUShort) dataBuffer).getData();
        }

        // Decode the (sub)image row-by-row
        int srcY, dstY;
        for (srcY = 0, dstY = yOffset; srcY < passHeight; srcY++, dstY += yStep) {
            // Read the filter type byte and a row of data
            int filter = 0;
            try {
                filter = this.dataStream.read();
                this.dataStream.readFully(curr, 0, bytesPerRow);
            } catch (final Exception e) {
                log.error("Exception", e);
            }

            switch (filter) {
            case PNG_FILTER_NONE:
                break;
            case PNG_FILTER_SUB:
                decodeSubFilter(curr, bytesPerRow, this.bytesPerPixel);
                break;
            case PNG_FILTER_UP:
                decodeUpFilter(curr, prior, bytesPerRow);
                break;
            case PNG_FILTER_AVERAGE:
                decodeAverageFilter(curr, prior, bytesPerRow,
                        this.bytesPerPixel);
                break;
            case PNG_FILTER_PAETH:
                decodePaethFilter(curr, prior, bytesPerRow, this.bytesPerPixel);
                break;
            default:
                // Error -- unknown filter type
                final String msg = PropertyUtil.getString("PNGImageDecoder16");
                throw new RuntimeException(msg);
            }

            // Copy data into passRow byte by byte
            if (this.bitDepth < 16) {
                System.arraycopy(curr, 0, byteData, 0, bytesPerRow);
            } else {
                int idx = 0;
                for (int j = 0; j < eltsPerRow; j++) {
                    shortData[j] = (short) (curr[idx] << 8 | curr[idx + 1] & 0xff);
                    idx += 2;
                }
            }

            processPixels(this.postProcess, passRow, imRas, xOffset, xStep,
                    dstY, passWidth);

            // Swap curr and prior
            final byte[] tmp = prior;
            prior = curr;
            curr = tmp;
        }
    }

    private void decodeImage(final boolean useInterlacing) {
        final int width = this.bounds.width;
        final int height = this.bounds.height;

        if (!useInterlacing) {
            decodePass(this.theTile, 0, 0, 1, 1, width, height);
        } else {
            decodePass(this.theTile, 0, 0, 8, 8, (width + 7) / 8,
                    (height + 7) / 8);
            decodePass(this.theTile, 4, 0, 8, 8, (width + 3) / 8,
                    (height + 7) / 8);
            decodePass(this.theTile, 0, 4, 4, 8, (width + 3) / 4,
                    (height + 3) / 8);
            decodePass(this.theTile, 2, 0, 4, 4, (width + 1) / 4,
                    (height + 3) / 4);
            decodePass(this.theTile, 0, 2, 2, 4, (width + 1) / 2,
                    (height + 1) / 4);
            decodePass(this.theTile, 1, 0, 2, 2, width / 2, (height + 1) / 2);
            decodePass(this.theTile, 0, 1, 1, 2, width, height / 2);
        }
    }

    @Override
    public WritableRaster copyData(final WritableRaster wr) {
        GraphicsUtil.copyData(this.theTile, wr);
        return wr;
    }

    // RenderedImage stuff
    @Override
    public Raster getTile(final int tileX, final int tileY) {
        if (tileX != 0 || tileY != 0) {
            // Error -- bad tile requested
            final String msg = PropertyUtil.getString("PNGImageDecoder17");
            throw new IllegalArgumentException(msg);
        }
        return this.theTile;
    }
}
