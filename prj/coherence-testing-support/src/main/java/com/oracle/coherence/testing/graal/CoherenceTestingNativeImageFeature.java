package com.oracle.coherence.testing.graal;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;

public class CoherenceTestingNativeImageFeature
        implements Feature
    {
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access)
        {
        Module module = getClass().getModule();
        RuntimeResourceAccess.addResource(module, "tangosol-coherence-override.xml");
        RuntimeResourceAccess.addResource(module, "logging.properties");
        RuntimeResourceAccess.addResource(module, "/common/tangosol-coherence-override.xml");
        RuntimeResourceAccess.addResource(module, "/META-INF/helidon/serial-config.properties");
        }
    }
