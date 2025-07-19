package data.graal;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;

public class CoherenceTestDataNativeImageFeature
        implements Feature
    {
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access)
        {
        Module module = getClass().getModule();
        RuntimeResourceAccess.addResource(module, "/data/readme.txt");
        RuntimeResourceAccess.addResource(module, "/data/test-1.txt");
        RuntimeResourceAccess.addResource(module, "/data/pof/test-pof-config.xml");
        RuntimeResourceAccess.addResource(module, "/META-INF/helidon/serial-config.properties");
        RuntimeResourceAccess.addResource(module, "/META-INF/type-aliases.properties");
        }
    }
