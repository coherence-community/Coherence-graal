package com.oracle.bedrock.runtime.coherence.graal;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.concurrent.RemoteChannelSerializer;
import com.oracle.bedrock.runtime.concurrent.RemoteEvent;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;
import org.graalvm.nativeimage.hosted.RuntimeSerialization;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectStreamException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BedrockNativeImageFeature
        implements Feature
    {
    private final Set<ReasonClass> processedTypes = ConcurrentHashMap.newKeySet();

    private final List<Class<?>> serializableTypes = List.of(RemoteRunnable.class,
            RemoteCallable.class, RemoteEvent.class);

    public BedrockNativeImageFeature()
        {
        }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access)
        {
        RuntimeResourceAccess.addResource(getClass().getModule(), "channel-serializer-pof-config.xml");

        ClassLoader imageClassLoader = access.getApplicationClassLoader();
        try (ScanResult scanResult = new ClassGraph()
                .overrideClasspath(access.getApplicationClassPath())
                .overrideClassLoaders(imageClassLoader)
                .enableAllInfo()
                .scan(Runtime.getRuntime().availableProcessors()))
            {
            scanResult.getAllClasses().forEach(classInfo ->
                {
                try
                    {
                    var clazz = Class.forName(classInfo.getName(), false, imageClassLoader);
                    if (RemoteChannelSerializer.class.isAssignableFrom(clazz))
                        {
                        logRegistration(RemoteChannelSerializer.class, clazz);
                        registerAllElements(clazz);
                        }
                    for (Class<?> serializableType : serializableTypes)
                        {
                        if (serializableType.isAssignableFrom(clazz))
                            {
                            logRegistration(serializableType, clazz);
                            RuntimeSerialization.register(clazz);
                            registerAllElements(clazz);
                            }
                        }
                    }
                catch (ClassNotFoundException | LinkageError e)
                    {
                    // ignore: due to incomplete classpath
                    }
                });
            }
        }

    private static void registerAllElements(Class<?> clazz)
        {
        registerClass(clazz);
        RuntimeReflection.register(clazz.getDeclaredConstructors());
        RuntimeReflection.register(clazz.getConstructors());
        RuntimeReflection.register(clazz.getDeclaredMethods());
        RuntimeReflection.register(clazz.getMethods());
        RuntimeReflection.register(clazz.getFields());
        RuntimeReflection.register(clazz.getDeclaredFields());
        }

    private static String getProcessedElementsPath()
        {
        return System.getProperty("com.oracle.coherence.graal.processedElementsPath");
        }

    private static void registerClass(Class<?> clazz)
        {
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

        try
            {
            for (Constructor<?> constructor : clazz.getDeclaredConstructors())
                {
                RuntimeReflection.register(constructor);
                }
            }
        catch (LinkageError e)
            {
            // ignore, can't link class
            }
        }

    private static void writeToFile(String filePath, String content)
        {
        try
            {
            Path path = Paths.get(filePath);
            if (path.getParent() != null)
                {
                Files.createDirectories(path.getParent());
                }
            Files.writeString(path, content);
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    private void logRegistration(Class<?> reason, Class<?> clazz)
        {
        if (getProcessedElementsPath() != null)
            {
            processedTypes.add(new ReasonClass(reason, clazz));
            }
        }

    private record ReasonClass(Class<?> reason, Class<?> type)
        {
        }
    }
