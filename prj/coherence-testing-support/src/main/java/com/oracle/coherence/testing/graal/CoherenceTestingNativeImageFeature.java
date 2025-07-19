/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.graal;

import com.tangosol.coherence.graal.AbstractNativeImageFeature;

import java.util.Set;

/**
 * A GraalVM native image feature used when building native images
 * for Coherence testing.
 */
public class CoherenceTestingNativeImageFeature
        extends AbstractNativeImageFeature
    {
    public CoherenceTestingNativeImageFeature()
        {
        super(Set.of(), Set.of(), RESOURCES);
        }

    /**
     * The resources to be registered in the native image.
     */
    public static final Set<String> RESOURCES = Set.of(
            "tangosol-coherence-override.xml",
            "logging.properties",
            "/common/tangosol-coherence-override.xml",
            "/META-INF/helidon/serial-config.properties");
    }
