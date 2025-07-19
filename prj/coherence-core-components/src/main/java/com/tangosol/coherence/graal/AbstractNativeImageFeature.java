/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.graal;

import com.tangosol.config.annotation.Injectable;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;
import org.graalvm.nativeimage.hosted.RuntimeSerialization;

import java.io.IOException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Comparator;

import java.util.List;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A base class for GraalVM native {@link Feature} implementations.
 */
@SuppressWarnings("unused")
public abstract class AbstractNativeImageFeature implements Feature
    {
    /**
     * Create a native image feature.
     *
     * @param handledSuperTypes  all subclasses of these types will be included
     * @param annotations        all types with these annotations will be included
     * @param resources          the resources to be registered in the native image
     */
    protected AbstractNativeImageFeature(Set<Class<?>> handledSuperTypes,
                                         Set<Class<? extends Annotation>> annotations,
                                         Set<String> resources)
        {
        this(handledSuperTypes, Set.of(), annotations, resources);
        }

    /**
     * Create a native image feature.
     *
     * @param superTypes         all subclasses of these types will be included
     * @param serializableTypes  types to be registered for serialization
     * @param annotations        all types with these annotations will be included
     * @param resources          the resources to be registered in the native image
     */
    protected AbstractNativeImageFeature(Set<Class<?>> superTypes,
                                         Set<Class<?>> serializableTypes,
                                         Set<Class<? extends Annotation>> annotations,
                                         Set<String> resources)
        {
        m_superTypes        = superTypes;
        m_serializableTypes = serializableTypes;
        m_annotations       = annotations;
        m_resources         = resources;
        }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access)
        {
        Module module = getClass().getModule();
        for (String resource : m_resources)
            {
            RuntimeResourceAccess.addResource(module, resource);
            }

        ClassLoader imageClassLoader = access.getApplicationClassLoader();
        List<Path>  classPath        = access.getApplicationClassPath();

        scan(imageClassLoader, classPath, classInfo ->
            {
            try
                {
                var clazz = Class.forName(classInfo.getName(), false, imageClassLoader);
                for (Class<?> serializableType : m_serializableTypes)
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

    @Override
    public void afterRegistration(AfterRegistrationAccess access)
        {
        ClassLoader imageClassLoader = access.getApplicationClassLoader();
        List<Path>  classPath        = access.getApplicationClassPath();

        scan(imageClassLoader, classPath, classInfo ->
            {
            try
                {
                var clazz = Class.forName(classInfo.getName(), false, imageClassLoader);
                boolean registered = false;

                for (Class<? extends Annotation> annotation : m_annotations)
                    {
                    if (clazz.getAnnotation(annotation) != null)
                        {
                        logRegistration(annotation, clazz);
                        registerAllElements(clazz);
                        registered = true;
                        break;
                        }
                    }

                if (!registered)
                    {
                    for (Class<?> handledSuperType : m_superTypes)
                        {
                        if (!handledSuperType.isAssignableFrom(clazz))
                            {
                            logRegistration(handledSuperType, clazz);
                            registerAllElements(clazz);
                            }
                        }
                    }

                processClass(access, clazz);
                }
            catch (ClassNotFoundException | LinkageError e)
                {
                // ignore: due to incomplete classpath
                }
            });

        /* Dump processed elements into Json */
        if (getProcessedElementsPath() != null)
            {
            writeToFile(getProcessedElementsPath(), processedTypes.stream()
                    .sorted(Comparator.comparing(c -> c.type.getTypeName()))
                    .map(c -> "{ \"reason\": \"" + c.reason + "\", \"type\": \"" + c.type.getTypeName() + "\" }")
                    .collect(Collectors.joining(",\n ", "[\n ", "\n]"))
            );
            }
        }

    protected void scan(ClassLoader imageClassLoader, List<Path> classPath, Consumer<ClassInfo> consumer)
        {
        try (ScanResult scanResult = new ClassGraph()
                .overrideClasspath(classPath)
                .overrideClassLoaders(imageClassLoader)
                .enableAllInfo()
                .scan(Runtime.getRuntime().availableProcessors()))
            {
            scanResult.getAllClasses().forEach(consumer);
            }
        }

    /**
     * Perform any custom handling of the specified class.
     *
     * @param access  the GraalVM {@link AfterRegistrationAccess}
     * @param clazz   the class to process
     */
    protected void processClass(AfterRegistrationAccess access, Class<?> clazz)
        {
        }

    protected static void registerAllElements(Class<?> clazz)
        {
        registerClass(clazz);
        RuntimeReflection.register(clazz.getDeclaredConstructors());
        RuntimeReflection.register(clazz.getConstructors());
        RuntimeReflection.register(clazz.getDeclaredMethods());
        RuntimeReflection.register(clazz.getMethods());
        RuntimeReflection.register(clazz.getFields());
        RuntimeReflection.register(clazz.getDeclaredFields());
        }

    protected static String getProcessedElementsPath()
        {
        return System.getProperty("com.oracle.coherence.graal.processedElementsPath");
        }

    protected static void registerClass(Class<?> clazz)
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
            for (Method declaredMethod : clazz.getDeclaredMethods())
                {
                if (declaredMethod.getAnnotation(Injectable.class) != null ||
                        declaredMethod.getName().equals("$deserializeLambda$"))
                    {
                    RuntimeReflection.register(declaredMethod);
                    }
                }
            }
        catch (LinkageError e)
            {
            // ignore, can't link class
            }
        }

    protected static void writeToFile(String filePath, String content)
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

    protected void logRegistration(Class<?> reason, Class<?> clazz)
        {
        if (getProcessedElementsPath() != null)
            {
            processedTypes.add(new ReasonClass(reason, clazz));
            }
        }

    protected record ReasonClass(Class<?> reason, Class<?> type)
        {
        }

    // ----- data members ---------------------------------------------------

    /**
     * All subclasses annotated of these classes will be included.
     */
    private final Set<Class<?>> m_superTypes;

    /**
     * All subclasses annotated of these classes will be registered for serialization.
     */
    private final Set<Class<?>> m_serializableTypes;

    /**
     * All classes annotated with these annotations will be included.
     */
    private final Set<Class<? extends Annotation>> m_annotations;

    /**
     * The resources to register.
     */
    private final Set<String> m_resources;

    /**
     * The processed types.
     */
    private final Set<ReasonClass> processedTypes = ConcurrentHashMap.newKeySet();
    }