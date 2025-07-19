/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.graal;

import com.tangosol.coherence.Component;
import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.xml.DocumentElementPreprocessor;
import com.tangosol.config.xml.DocumentPreprocessor;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.NamespaceHandler;
import com.tangosol.run.xml.PropertyAdapter;
import com.tangosol.run.xml.XmlBean;
import com.tangosol.run.xml.XmlSerializable;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.schema.annotation.PortableType;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class CoherenceNativeImageFeature implements Feature {
    private record ReasonClass(Class<?> reason, Class<?> type) {
    }

    List<Class<?>> handledSuperTypes = List.of(
            ElementProcessor.class,
            PortableObject.class,
            PofSerializer.class,
            ExternalizableLite.class,
            DocumentPreprocessor.class,
            DocumentElementPreprocessor.ElementPreprocessor.class,
            NamespaceHandler.class,
            CacheConfigNamespaceHandler.Extension.class,
            OperationalConfigNamespaceHandler.Extension.class
    );

    Set<ReasonClass> processedTypes = ConcurrentHashMap.newKeySet();

@Override
public void beforeAnalysis(BeforeAnalysisAccess access)
    {
    Module module = getClass().getModule();
    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/defaults/coherence-cache-config.xml");
    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/defaults/grpc-proxy-cache-config.xml");
    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/defaults/management-config.xml");
    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/defaults/management-http-config.xml");
    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/defaults/pof-config.xml");
    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/defaults/tangosol-coherence-override-dev.xml");
    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/defaults/tangosol-coherence-override-eval.xml");
    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/defaults/tangosol-coherence-override-prod.xml");

    RuntimeResourceAccess.addResource(module, "/META-INF/helidon/serial-config.properties");
    RuntimeResourceAccess.addResource(module, "/META-INF/schema.xml");
    RuntimeResourceAccess.addResource(module, "/META-INF/type-aliases.properties");

    RuntimeResourceAccess.addResource(module, "/reports/report-all.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-cache-effectiveness.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-cache-size.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-cache-storage.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-executor.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-group.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-grpc-proxy-connections.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-grpc-proxy-v0.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-grpc-proxy-v1.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-jcache-configuration.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-jcache-statistics.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-management.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-memory-status.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-network-health.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-network-health-detail.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-node.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-persistence.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-persistence-detail.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-proxy.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-proxy-connections.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-proxy-http.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-service.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-service-partitions.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-topic.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-topic-subscriber-groups.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-topic-subscribers.xml");
    RuntimeResourceAccess.addResource(module, "/reports/report-view-effectiveness.xml");

    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/xsd/schema.xsd");
    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/xsd/schema-cpp.xsd");
    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/xsd/schema-dotnet.xsd");
    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/xsd/schema-java.xsd");
    RuntimeResourceAccess.addResource(module, "/com/oracle/coherence/xsd/schema-pof.xsd");

    RuntimeResourceAccess.addResource(module, "coherence-cache-config.xsd");
    RuntimeResourceAccess.addResource(module, "coherence-cache-config-base.xsd");
    RuntimeResourceAccess.addResource(module, "coherence-config-base.xsd");
    RuntimeResourceAccess.addResource(module, "coherence-operational-config.xsd");
    RuntimeResourceAccess.addResource(module, "coherence-operational-config-base.xsd");
    RuntimeResourceAccess.addResource(module, "coherence-pof-config.xml");
    RuntimeResourceAccess.addResource(module, "coherence-pof-config.xsd");
    RuntimeResourceAccess.addResource(module, "coherence-report-config.xsd");
    RuntimeResourceAccess.addResource(module, "coherence-report-group-config.xsd");
    RuntimeResourceAccess.addResource(module, "coherence-rest-config.xsd");
    RuntimeResourceAccess.addResource(module, "coherence-system-config.xml");
    RuntimeResourceAccess.addResource(module, "management-swagger.json");
    RuntimeResourceAccess.addResource(module, "metrics-http-config.xml");
    RuntimeResourceAccess.addResource(module, "tangosol-coherence.xml");
    }

@Override
    public void afterRegistration(AfterRegistrationAccess access) {
        ClassLoader imageClassLoader = access.getApplicationClassLoader();
        try (ScanResult scanResult = new ClassGraph()
                .overrideClasspath(access.getApplicationClassPath())
                .overrideClassLoaders(imageClassLoader)
                .enableAllInfo()
                .scan(Runtime.getRuntime().availableProcessors())) {

            scanResult.getAllClasses().forEach(classInfo -> {
                try {
                    var clazz = Class.forName(classInfo.getName(), false, imageClassLoader);
                    if (XmlSerializable.class.isAssignableFrom(clazz))
                        {
                        // this is an XML bean so register its corresponding config resource
                        String xmlName = "/" + clazz.getName().replace('.', '/') + ".xml";
                        RuntimeResourceAccess.addResource(clazz.getModule(), xmlName);
                        logRegistration(XmlSerializable.class, clazz);
                        registerAllElements(clazz);
                        }
                    else if (clazz.getAnnotation(PortableType.class) != null) {
                        logRegistration(PortableType.class, clazz);
                        registerAllElements(clazz);
                    } else if (Component.class.isAssignableFrom(clazz)) {
                        logRegistration(Component.class, clazz);
                        registerAllElements(clazz);
                    } else if (PropertyAdapter.class.isAssignableFrom(clazz)) {
                        logRegistration(Component.class, clazz);
                        registerAllElements(clazz);
                    }
                    for (Class<?> handledSuperType : handledSuperTypes) {
                        logRegistration(handledSuperType, clazz);
                        registerAllElements(clazz);
                    }
                } catch (ClassNotFoundException | LinkageError e) {
                    // ignore: due to incomplete classpath
                }
            });
        }

        /* Dump processed elements into json */
        if (getProcessedElementsPath() != null) {
            writeToFile(getProcessedElementsPath(), processedTypes.stream()
                    .sorted(Comparator.comparing(c -> c.type.getTypeName()))
                    .map(c -> "{ \"reason\": \"" + c.reason + "\", \"type\": \"" + c.type.getTypeName() + "\" }")
                    .collect(Collectors.joining(",\n ", "[\n ", "\n]"))
            );
        }
    }

    private static void registerAllElements(Class<?> clazz) {
        registerClass(clazz);
        RuntimeReflection.register(clazz.getDeclaredConstructors());
        RuntimeReflection.register(clazz.getConstructors());
        RuntimeReflection.register(clazz.getDeclaredMethods());
        RuntimeReflection.register(clazz.getMethods());
        RuntimeReflection.register(clazz.getFields());
        RuntimeReflection.register(clazz.getDeclaredFields());
    }

    private void logRegistration(Class<?> reason, Class<?> clazz) {
        if (getProcessedElementsPath() != null) {
            processedTypes.add(new ReasonClass(reason, clazz));
        }
    }

    private static String getProcessedElementsPath() {
        return System.getProperty("com.oracle.coherence.graal.processedElementsPath");
    }

    private static void registerClass(Class<?> clazz) {
        /* Register all members: a new API is coming where this is one line */
        RuntimeReflection.register(clazz);
        RuntimeReflection.registerAllClasses(clazz);
        RuntimeReflection.registerAllDeclaredClasses(clazz);
        RuntimeReflection.registerAllDeclaredMethods(clazz);
        RuntimeReflection.registerAllMethods(clazz);
        RuntimeReflection.registerAllDeclaredConstructors(clazz);
        RuntimeReflection.registerAllConstructors(clazz);
        RuntimeReflection.registerAllFields(clazz);
        RuntimeReflection.registerAllDeclaredFields(clazz);
        RuntimeReflection.registerAllNestMembers(clazz);
        RuntimeReflection.registerAllPermittedSubclasses(clazz);
        RuntimeReflection.registerAllRecordComponents(clazz);
        RuntimeReflection.registerAllSigners(clazz);

        try {
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                RuntimeReflection.register(constructor);
            }
            for (Method declaredMethod : clazz.getDeclaredMethods()) {
                if (declaredMethod.getAnnotation(Injectable.class) != null ||
                        declaredMethod.getName().equals("$deserializeLambda$")) {
                    RuntimeReflection.register(declaredMethod);
                }
            }
        } catch (LinkageError e) {
            // ignore, can't link class
        }
    }

    private static void writeToFile(String filePath, String content) {
        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}