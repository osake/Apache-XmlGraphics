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

/* $Id: TransformStackElement.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.java2d;

import java.awt.geom.AffineTransform;

import lombok.extern.slf4j.Slf4j;

// CSOFF: EmptyBlock
// CSOFF: NoWhitespaceAfter
// CSOFF: OperatorWrap
// CSOFF: WhitespaceAround

/**
 * Contains a description of an elementary transform stack element, such as a
 * rotate or translate. A transform stack element has a type and a value, which
 * is an array of double values.<br>
 *
 * @version $Id: TransformStackElement.java 1345683 2012-06-03 14:50:33Z gadams
 *          $
 *
 *          Originally authored by Vincent Hardy and Paul Evenblij.
 */
@Slf4j
public abstract class TransformStackElement implements Cloneable {

    /**
     * Transform type
     */
    private final TransformType type;

    /**
     * Value
     */
    private double[] transformParameters;

    /**
     * @param type
     *            transform type
     * @param transformParameters
     *            parameters for transform
     */
    protected TransformStackElement(final TransformType type,
            final double[] transformParameters) {
        this.type = type;
        this.transformParameters = transformParameters;
    }

    /**
     * @return an object which is a deep copy of this one
     */
    @Override
    public Object clone() {
        TransformStackElement newElement = null;

        // start with a shallow copy to get our implementations right
        try {
            newElement = (TransformStackElement) super.clone();
        } catch (final CloneNotSupportedException ex) {
            log.error("CloneNotSupportedException", ex);
        }

        // now deep copy the parameter array
        final double[] transformParameters = new double[this.transformParameters.length];
        System.arraycopy(this.transformParameters, 0, transformParameters, 0,
                transformParameters.length);
        newElement.transformParameters = transformParameters;
        return newElement;
    }

    /*
     * Factory methods
     */

    public static TransformStackElement createTranslateElement(final double tx,
            final double ty) {
        return new TransformStackElement(TransformType.TRANSLATE, new double[] {
                tx, ty }) {
            @Override
            boolean isIdentity(final double[] parameters) {
                return parameters[0] == 0 && parameters[1] == 0;
            }
        };
    }

    public static TransformStackElement createRotateElement(final double theta) {
        return new TransformStackElement(TransformType.ROTATE,
                new double[] { theta }) {
            @Override
            boolean isIdentity(final double[] parameters) {
                return Math.cos(parameters[0]) == 1;
            }
        };
    }

    public static TransformStackElement createScaleElement(final double scaleX,
            final double scaleY) {
        return new TransformStackElement(TransformType.SCALE, new double[] {
                scaleX, scaleY }) {
            @Override
            boolean isIdentity(final double[] parameters) {
                return parameters[0] == 1 && parameters[1] == 1;
            }
        };
    }

    public static TransformStackElement createShearElement(final double shearX,
            final double shearY) {
        return new TransformStackElement(TransformType.SHEAR, new double[] {
                shearX, shearY }) {
            @Override
            boolean isIdentity(final double[] parameters) {
                return parameters[0] == 0 && parameters[1] == 0;
            }
        };
    }

    public static TransformStackElement createGeneralTransformElement(
            final AffineTransform txf) {
        final double[] matrix = new double[6];
        txf.getMatrix(matrix);
        return new TransformStackElement(TransformType.GENERAL, matrix) {
            @Override
            boolean isIdentity(final double[] m) {
                return m[0] == 1 && m[2] == 0 && m[4] == 0 && m[1] == 0
                        && m[3] == 1 && m[5] == 0;
            }
        };
    }

    /**
     * Implementation should determine if the parameter list represents an
     * identity transform, for the instance transform type.
     */
    abstract boolean isIdentity(final double[] parameters);

    /**
     * @return true iff this transform is the identity transform
     */
    public boolean isIdentity() {
        return isIdentity(this.transformParameters);
    }

    /**
     * @return array of values containing this transform element's parameters
     */
    public double[] getTransformParameters() {
        return this.transformParameters;
    }

    /**
     * @return this transform type
     */
    public TransformType getType() {
        return this.type;
    }

    /*
     * Concatenation utility. Requests this transform stack element to
     * concatenate with the input stack element. Only elements of the same types
     * are concatenated. For example, if this element represents a translation,
     * it will concatenate with another translation, but not with any other kind
     * of stack element.
     * 
     * @param stackElement element to be concatenated with this one.
     * 
     * @return true if the input stackElement was concatenated with this one.
     * False otherwise.
     */
    public boolean concatenate(final TransformStackElement stackElement) {
        boolean canConcatenate = false;

        if (this.type.toInt() == stackElement.type.toInt()) {
            canConcatenate = true;
            switch (this.type.toInt()) {
            case TransformType.TRANSFORM_TRANSLATE:
                this.transformParameters[0] += stackElement.transformParameters[0];
                this.transformParameters[1] += stackElement.transformParameters[1];
                break;
            case TransformType.TRANSFORM_ROTATE:
                this.transformParameters[0] += stackElement.transformParameters[0];
                break;
            case TransformType.TRANSFORM_SCALE:
                this.transformParameters[0] *= stackElement.transformParameters[0];
                this.transformParameters[1] *= stackElement.transformParameters[1];
                break;
            case TransformType.TRANSFORM_GENERAL:
                this.transformParameters = matrixMultiply(
                        this.transformParameters,
                        stackElement.transformParameters);
                break;
            default:
                canConcatenate = false;
            }
        }

        return canConcatenate;
    }

    /**
     * Multiplies two 2x3 matrices of double precision values
     */
    private double[] matrixMultiply(final double[] matrix1,
            final double[] matrix2) {
        final double[] product = new double[6];
        final AffineTransform transform1 = new AffineTransform(matrix1);
        transform1.concatenate(new AffineTransform(matrix2));
        transform1.getMatrix(product);
        return product;
    }

}
