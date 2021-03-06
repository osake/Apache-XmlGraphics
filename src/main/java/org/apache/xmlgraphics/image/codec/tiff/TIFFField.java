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

/* $Id: TIFFField.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.image.codec.tiff;

import java.io.Serializable;

// CSOFF: WhitespaceAround

/**
 * A class representing a field in a TIFF 6.0 Image File Directory.
 *
 * <p>
 * The TIFF file format is described in more detail in the comments for the
 * TIFFDescriptor class.
 *
 * <p>
 * A field in a TIFF Image File Directory (IFD). A field is defined as a
 * sequence of values of identical data type. TIFF 6.0 defines 12 data types,
 * which are mapped internally onto the Java datatypes byte, int, long, float,
 * and double.
 *
 * <p>
 * <b> This class is not a committed part of the JAI API. It may be removed or
 * changed in future releases of JAI.</b>
 *
 * @see TIFFDirectory
 */
public class TIFFField implements Comparable<TIFFField>, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 207783128222415437L;

    /** Flag for 8 bit unsigned integers. */
    public static final int TIFF_BYTE = 1;

    /** Flag for null-terminated ASCII strings. */
    public static final int TIFF_ASCII = 2;

    /** Flag for 16 bit unsigned integers. */
    public static final int TIFF_SHORT = 3;

    /** Flag for 32 bit unsigned integers. */
    public static final int TIFF_LONG = 4;

    /** Flag for pairs of 32 bit unsigned integers. */
    public static final int TIFF_RATIONAL = 5;

    /** Flag for 8 bit signed integers. */
    public static final int TIFF_SBYTE = 6;

    /** Flag for 8 bit uninterpreted bytes. */
    public static final int TIFF_UNDEFINED = 7;

    /** Flag for 16 bit signed integers. */
    public static final int TIFF_SSHORT = 8;

    /** Flag for 32 bit signed integers. */
    public static final int TIFF_SLONG = 9;

    /** Flag for pairs of 32 bit signed integers. */
    public static final int TIFF_SRATIONAL = 10;

    /** Flag for 32 bit IEEE floats. */
    public static final int TIFF_FLOAT = 11;

    /** Flag for 64 bit IEEE doubles. */
    public static final int TIFF_DOUBLE = 12;

    /** The tag number. */
    int tag;

    /** The tag type. */
    int type;

    /** The number of data items present in the field. */
    int count;

    /** The field data. */
    Object data;

    /** The default constructor. */
    TIFFField() {
    }

    /**
     * Constructs a TIFFField with arbitrary data. The data parameter must be an
     * array of a Java type appropriate for the type of the TIFF field. Since
     * there is no available 32-bit unsigned datatype, long is used. The mapping
     * between types is as follows:
     *
     * <table border=1>
     * <tr>
     * <th>TIFF type</th>
     * <th>Java type</th>
     * <tr>
     * <td><tt>TIFF_BYTE</tt></td>
     * <td><tt>byte</tt></td>
     * <tr>
     * <td><tt>TIFF_ASCII</tt></td>
     * <td><tt>String</tt></td>
     * <tr>
     * <td><tt>TIFF_SHORT</tt></td>
     * <td><tt>char</tt></td>
     * <tr>
     * <td><tt>TIFF_LONG</tt></td>
     * <td><tt>long</tt></td>
     * <tr>
     * <td><tt>TIFF_RATIONAL</tt></td>
     * <td><tt>long[2]</tt></td>
     * <tr>
     * <td><tt>TIFF_SBYTE</tt></td>
     * <td><tt>byte</tt></td>
     * <tr>
     * <td><tt>TIFF_UNDEFINED</tt></td>
     * <td><tt>byte</tt></td>
     * <tr>
     * <td><tt>TIFF_SSHORT</tt></td>
     * <td><tt>short</tt></td>
     * <tr>
     * <td><tt>TIFF_SLONG</tt></td>
     * <td><tt>int</tt></td>
     * <tr>
     * <td><tt>TIFF_SRATIONAL</tt></td>
     * <td><tt>int[2]</tt></td>
     * <tr>
     * <td><tt>TIFF_FLOAT</tt></td>
     * <td><tt>float</tt></td>
     * <tr>
     * <td><tt>TIFF_DOUBLE</tt></td>
     * <td><tt>double</tt></td>
     * </table>
     */
    public TIFFField(final int tag, final int type, final int count,
            final Object data) {
        this.tag = tag;
        this.type = type;
        this.count = count;
        this.data = data;
    }

    /**
     * Returns the tag number, between 0 and 65535.
     */
    public int getTag() {
        return this.tag;
    }

    /**
     * Returns the type of the data stored in the IFD. For a TIFF6.0 file, the
     * value will equal one of the TIFF_ constants defined in this class. For
     * future revisions of TIFF, higher values are possible.
     *
     */
    public int getType() {
        return this.type;
    }

    /**
     * Returns the number of elements in the IFD.
     */
    public int getCount() {
        return this.count;
    }

    /**
     * Returns the data as an uninterpreted array of bytes. The type of the
     * field must be one of TIFF_BYTE, TIFF_SBYTE, or TIFF_UNDEFINED;
     *
     * <p>
     * For data in TIFF_BYTE format, the application must take care when
     * promoting the data to longer integral types to avoid sign extension.
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_BYTE, TIFF_SBYTE, or TIFF_UNDEFINED.
     */
    public byte[] getAsBytes() {
        return (byte[]) this.data;
    }

    /**
     * Returns TIFF_SHORT data as an array of chars (unsigned 16-bit integers).
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_SHORT.
     */
    public char[] getAsChars() {
        return (char[]) this.data;
    }

    /**
     * Returns TIFF_SSHORT data as an array of shorts (signed 16-bit integers).
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_SSHORT.
     */
    public short[] getAsShorts() {
        return (short[]) this.data;
    }

    /**
     * Returns TIFF_SLONG data as an array of ints (signed 32-bit integers).
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_SLONG.
     */
    public int[] getAsInts() {
        return (int[]) this.data;
    }

    /**
     * Returns TIFF_LONG data as an array of longs (signed 64-bit integers).
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_LONG.
     */
    public long[] getAsLongs() {
        return (long[]) this.data;
    }

    /**
     * Returns TIFF_FLOAT data as an array of floats.
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_FLOAT.
     */
    public float[] getAsFloats() {
        return (float[]) this.data;
    }

    /**
     * Returns TIFF_DOUBLE data as an array of doubles.
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_DOUBLE.
     */
    public double[] getAsDoubles() {
        return (double[]) this.data;
    }

    /**
     * Returns TIFF_SRATIONAL data as an array of 2-element arrays of ints.
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_SRATIONAL.
     */
    public int[][] getAsSRationals() {
        return (int[][]) this.data;
    }

    /**
     * Returns TIFF_RATIONAL data as an array of 2-element arrays of longs.
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_RATTIONAL.
     */
    public long[][] getAsRationals() {
        return (long[][]) this.data;
    }

    /**
     * Returns data in TIFF_BYTE, TIFF_SBYTE, TIFF_UNDEFINED, TIFF_SHORT,
     * TIFF_SSHORT, or TIFF_SLONG format as an int.
     *
     * <p>
     * TIFF_BYTE and TIFF_UNDEFINED data are treated as unsigned; that is, no
     * sign extension will take place and the returned value will be in the
     * range [0, 255]. TIFF_SBYTE data will be returned in the range [-128,
     * 127].
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_BYTE, TIFF_SBYTE, TIFF_UNDEFINED, TIFF_SHORT, TIFF_SSHORT, or
     * TIFF_SLONG.
     */
    public int getAsInt(final int index) {
        switch (this.type) {
        case TIFF_BYTE:
        case TIFF_UNDEFINED:
            return ((byte[]) this.data)[index] & 0xff;
        case TIFF_SBYTE:
            return ((byte[]) this.data)[index];
        case TIFF_SHORT:
            return ((char[]) this.data)[index] & 0xffff;
        case TIFF_SSHORT:
            return ((short[]) this.data)[index];
        case TIFF_SLONG:
            return ((int[]) this.data)[index];
        default:
            throw new ClassCastException();
        }
    }

    /**
     * Returns data in TIFF_BYTE, TIFF_SBYTE, TIFF_UNDEFINED, TIFF_SHORT,
     * TIFF_SSHORT, TIFF_SLONG, or TIFF_LONG format as a long.
     *
     * <p>
     * TIFF_BYTE and TIFF_UNDEFINED data are treated as unsigned; that is, no
     * sign extension will take place and the returned value will be in the
     * range [0, 255]. TIFF_SBYTE data will be returned in the range [-128,
     * 127].
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_BYTE, TIFF_SBYTE, TIFF_UNDEFINED, TIFF_SHORT, TIFF_SSHORT,
     * TIFF_SLONG, or TIFF_LONG.
     */
    public long getAsLong(final int index) {
        switch (this.type) {
        case TIFF_BYTE:
        case TIFF_UNDEFINED:
            return ((byte[]) this.data)[index] & 0xff;
        case TIFF_SBYTE:
            return ((byte[]) this.data)[index];
        case TIFF_SHORT:
            return ((char[]) this.data)[index] & 0xffff;
        case TIFF_SSHORT:
            return ((short[]) this.data)[index];
        case TIFF_SLONG:
            return ((int[]) this.data)[index];
        case TIFF_LONG:
            return ((long[]) this.data)[index];
        default:
            throw new ClassCastException();
        }
    }

    /**
     * Returns data in any numerical format as a float. Data in TIFF_SRATIONAL
     * or TIFF_RATIONAL format are evaluated by dividing the numerator into the
     * denominator using double-precision arithmetic and then truncating to
     * single precision. Data in TIFF_SLONG, TIFF_LONG, or TIFF_DOUBLE format
     * may suffer from truncation.
     *
     * <p>
     * A ClassCastException will be thrown if the field is of type
     * TIFF_UNDEFINED or TIFF_ASCII.
     */
    public float getAsFloat(final int index) {
        switch (this.type) {
        case TIFF_BYTE:
            return ((byte[]) this.data)[index] & 0xff;
        case TIFF_SBYTE:
            return ((byte[]) this.data)[index];
        case TIFF_SHORT:
            return ((char[]) this.data)[index] & 0xffff;
        case TIFF_SSHORT:
            return ((short[]) this.data)[index];
        case TIFF_SLONG:
            return ((int[]) this.data)[index];
        case TIFF_LONG:
            return ((long[]) this.data)[index];
        case TIFF_FLOAT:
            return ((float[]) this.data)[index];
        case TIFF_DOUBLE:
            return (float) ((double[]) this.data)[index];
        case TIFF_SRATIONAL:
            final int[] ivalue = getAsSRational(index);
            return (float) ((double) ivalue[0] / ivalue[1]);
        case TIFF_RATIONAL:
            final long[] lvalue = getAsRational(index);
            return (float) ((double) lvalue[0] / lvalue[1]);
        default:
            throw new ClassCastException();
        }
    }

    /**
     * Returns data in any numerical format as a float. Data in TIFF_SRATIONAL
     * or TIFF_RATIONAL format are evaluated by dividing the numerator into the
     * denominator using double-precision arithmetic.
     *
     * <p>
     * A ClassCastException will be thrown if the field is of type
     * TIFF_UNDEFINED or TIFF_ASCII.
     */
    public double getAsDouble(final int index) {
        switch (this.type) {
        case TIFF_BYTE:
            return ((byte[]) this.data)[index] & 0xff;
        case TIFF_SBYTE:
            return ((byte[]) this.data)[index];
        case TIFF_SHORT:
            return ((char[]) this.data)[index] & 0xffff;
        case TIFF_SSHORT:
            return ((short[]) this.data)[index];
        case TIFF_SLONG:
            return ((int[]) this.data)[index];
        case TIFF_LONG:
            return ((long[]) this.data)[index];
        case TIFF_FLOAT:
            return ((float[]) this.data)[index];
        case TIFF_DOUBLE:
            return ((double[]) this.data)[index];
        case TIFF_SRATIONAL:
            final int[] ivalue = getAsSRational(index);
            return (double) ivalue[0] / ivalue[1];
        case TIFF_RATIONAL:
            final long[] lvalue = getAsRational(index);
            return (double) lvalue[0] / lvalue[1];
        default:
            throw new ClassCastException();
        }
    }

    /**
     * Returns a TIFF_ASCII data item as a String.
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_ASCII.
     */
    public String getAsString(final int index) {
        return ((String[]) this.data)[index];
    }

    /**
     * Returns a TIFF_SRATIONAL data item as a two-element array of ints.
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_SRATIONAL.
     */
    public int[] getAsSRational(final int index) {
        return ((int[][]) this.data)[index];
    }

    /**
     * Returns a TIFF_RATIONAL data item as a two-element array of ints.
     *
     * <p>
     * A ClassCastException will be thrown if the field is not of type
     * TIFF_RATIONAL.
     */
    public long[] getAsRational(final int index) {
        return ((long[][]) this.data)[index];
    }

    /**
     * Compares this <code>TIFFField</code> with another <code>TIFFField</code>
     * by comparing the tags.
     *
     * <p>
     * <b>Note: this class has a natural ordering that is inconsistent with
     * <code>equals()</code>.</b>
     *
     * @throws NullPointerException
     *             if the parameter is <code>null</code>.
     * @throws ClassCastException
     *             if the parameter is not a <code>TIFFField</code>.
     */
    @Override
    public int compareTo(final TIFFField o) {
        if (o == null) {
            throw new NullPointerException();
        }

        final int oTag = o.getTag();

        if (this.tag < oTag) {
            return -1;
        } else if (this.tag > oTag) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.count;
        result = prime * result
                + (this.data == null ? 0 : this.data.hashCode());
        result = prime * result + this.tag;
        result = prime * result + this.type;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TIFFField other = (TIFFField) obj;
        if (this.count != other.count) {
            return false;
        }
        if (this.data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!this.data.equals(other.data)) {
            return false;
        }
        if (this.tag != other.tag) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return true;
    }

}
