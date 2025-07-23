/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package data.graal;

import com.tangosol.coherence.graal.AbstractNativeImageFeature;

import java.util.Set;

/**
 * A GraalVM native image feature used when building native images
 * for Coherence test data.
 */
public class CoherenceTestDataNativeImageFeature
       extends AbstractNativeImageFeature
    {
    public CoherenceTestDataNativeImageFeature()
        {
        super(Set.of(), Set.of(), RESOURCES);
        }

    /**
     * The resources to be registered in the native image.
     */
    public static final Set<String> RESOURCES = Set.of();
    }
