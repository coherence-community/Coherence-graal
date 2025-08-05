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

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A base class for GraalVM native {@link Feature} implementations.
 */
@SuppressWarnings("unused")
public abstract class AbstractNativeImageFeature
        implements Feature
    {
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access)
        {
        ClassLoader   imageClassLoader = access.getApplicationClassLoader();
        List<Path>    classPath        = access.getApplicationClassPath();

        // Register a reachability handler for all the serializable types
        // which will register any subtypes for serialization
        for (Class<?> clazz : getSerializableTypes())
            {
            if (!clazz.isInterface())
                {
                RuntimeSerialization.register(clazz);
                }
            access.registerSubtypeReachabilityHandler(SerializableReachableTypeHandler.INSTANCE, clazz);
            }

        // Find any subtypes of serializable classes on the class path
        // and register them
        scan(imageClassLoader, classPath, classInfo ->
            {
            try
                {
                var clazz = Class.forName(classInfo.getName(), false, imageClassLoader);
                for (Class<?> serializableType : getSerializableTypes())
                    {
                    if (serializableType.isAssignableFrom(clazz))
                        {
                        logRegistration(serializableType, clazz);
                        RuntimeSerialization.register(clazz);
                        registerAllElements(clazz);
                        break;
                        }
                    }

                processClassBeforeAnalysis(access, clazz);
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

                for (Class<? extends Annotation> annotation : getAnnotations())
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
                    for (Class<?> handledSuperType : getSupertypes())
                        {
                        if (!handledSuperType.isAssignableFrom(clazz))
                            {
                            logRegistration(handledSuperType, clazz);
                            registerAllElements(clazz);
                            break;
                            }
                        }
                    }

                processClassAfterRegistration(access, clazz);
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

    /**
     * Return the set of supertypes to register for serialization with all subtypes of these types.
     *
     * @return the set of supertypes to register
     */
    protected abstract Set<Class<?>> getSerializableTypes();

    /**
     * Return the set of supertypes to register as accessible, along with all subtypes of these types.
     *
     * @return the set of supertypes to register as accessible
     */
    protected abstract Set<Class<?>> getSupertypes();

    /**
     * Return a set of annotations so that any class with those annotations will be registered as accessible.
     *
     * @return  the set of annotations to use to register annotated classes as accessible
     */
    protected abstract Set<Class<? extends Annotation>> getAnnotations();

    /**
     * Scan a classpath and pass all the types found to a handler.
     *
     * @param imageClassLoader  the {@link ClassLoader} to use
     * @param classPath         the classpath to scan
     * @param consumer          the consumer to handle each type from the classpath
     */
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
     * @param access  the GraalVM {@link BeforeAnalysisAccess}
     * @param clazz   the class to process
     */
    protected void processClassBeforeAnalysis(BeforeAnalysisAccess access, Class<?> clazz)
        {
        }

    /**
     * Perform any custom handling of the specified class.
     *
     * @param access  the GraalVM {@link AfterRegistrationAccess}
     * @param clazz   the class to process
     */
    protected void processClassAfterRegistration(AfterRegistrationAccess access, Class<?> clazz)
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

    // ----- inner class: ReasonClass ---------------------------------------

    protected record ReasonClass(Class<?> reason, Class<?> type)
        {
        }

    // ----- inner class: SerializableReachableTypeHandler ------------------

    /**
     * A reachability handler to register types for serialization.
     */
    public static class SerializableReachableTypeHandler
            implements BiConsumer<DuringAnalysisAccess, Class<?>>
        {
        @Override
        public void accept(DuringAnalysisAccess access, Class<?> clazz)
            {
            if (processed.add(clazz))
                {
                RuntimeSerialization.register(clazz);
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * A singleton instance of {@link SerializableReachableTypeHandler}.
         */
        public static final SerializableReachableTypeHandler INSTANCE = new SerializableReachableTypeHandler();

        /**
         * The set of types that this handler has already seen.
         */
        protected static final Set<Class<?>> processed = ConcurrentHashMap.newKeySet();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The types already processed by the feature.
     */
    private final Set<ReasonClass> processedTypes = ConcurrentHashMap.newKeySet();
    }