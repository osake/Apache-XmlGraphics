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

/* $Id: DSCCommentBeginResource.java 734420 2009-01-14 15:38:32Z maxberger $ */

package org.apache.xmlgraphics.ps.dsc.events;

import java.io.IOException;

import org.apache.xmlgraphics.ps.DSCConstants;
import org.apache.xmlgraphics.ps.PSGenerator;
import org.apache.xmlgraphics.ps.PSResource;

/**
 * Represents a %BeginResource DSC comment.
 */
public class DSCCommentBeginResource extends AbstractResourceDSCComment {

    private Integer min;
    private Integer max;

    /**
     * Creates a new instance
     */
    public DSCCommentBeginResource() {
        super();
    }

    /**
     * Creates a new instance for a given PSResource instance
     * 
     * @param resource
     *            the resource
     */
    public DSCCommentBeginResource(final PSResource resource) {
        super(resource);
    }

    /**
     * Creates a new instance for a given PSResource instance
     * 
     * @param resource
     *            the resource
     * @param min
     *            Minimum VM used by the resource
     * @param max
     *            Maximum VM used by the resource
     */
    public DSCCommentBeginResource(final PSResource resource, final int min,
            final int max) {
        super(resource);
        this.min = min;
        this.max = max;
    }

    /**
     * Returns the minimum VM used by the resource.
     * 
     * @return the minimum VM used by the resource
     */
    public Integer getMin() {
        return this.min;
    }

    /**
     * Returns the maximum VM used by the resource.
     * 
     * @return the maximum VM used by the resource
     */
    public Integer getMax() {
        return this.max;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return DSCConstants.BEGIN_RESOURCE;
    }

    /** {@inheritDoc} */
    @Override
    public void generate(final PSGenerator gen) throws IOException {
        if (getMin() != null) {
            final Object[] params = new Object[] { getResource(), getMin(),
                    getMax() };
            gen.writeDSCComment(getName(), params);
        } else {
            super.generate(gen);
        }
    }

}
