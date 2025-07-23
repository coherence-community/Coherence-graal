/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.graal;

import com.oracle.coherence.common.schema.SchemaExtension;
import com.oracle.coherence.common.schema.SchemaSource;

import com.oracle.coherence.inject.Injector;

import com.tangosol.application.LifecycleListener;

import com.tangosol.coherence.Component;

import com.tangosol.coherence.config.EnvironmentVariableResolver;
import com.tangosol.coherence.config.SystemPropertyResolver;

import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;

import com.tangosol.coherence.http.HttpApplication;
import com.tangosol.coherence.http.HttpServer;

import com.tangosol.config.xml.DocumentElementPreprocessor;
import com.tangosol.config.xml.DocumentPreprocessor;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.NamespaceHandler;

import com.tangosol.internal.tracing.TracingShimLoader;

import com.tangosol.internal.util.graal.ScriptHandler;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.io.pof.PofConfigProvider;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.schema.annotation.PortableType;

import com.tangosol.net.Coherence;
import com.tangosol.net.MemberIdentityProvider;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.SessionProvider;

import com.tangosol.net.events.InterceptorMetadataResolver;

import com.tangosol.net.grpc.GrpcAcceptorController;

import com.tangosol.net.management.MapJsonBodyHandler;

import com.tangosol.net.metrics.MetricsRegistryAdapter;

import com.tangosol.run.xml.PropertyAdapter;
import com.tangosol.run.xml.XmlSerializable;

import com.tangosol.util.HealthCheck;

import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;
import org.graalvm.nativeimage.hosted.RuntimeSerialization;

import java.lang.annotation.Annotation;

import java.util.Collections;
import java.util.Set;

/**
 * A GraalVM native image {@link org.graalvm.nativeimage.hosted.Feature}
 * used when building Coherence native applications.
 */
@SuppressWarnings("unused")
public class CoherenceNativeImageFeature
        extends AbstractNativeImageFeature
    {
    /**
     * Create a {@link CoherenceNativeImageFeature}
     */
    public CoherenceNativeImageFeature()
        {
        super(SUPERTYPES, SERIALIZABLE_TYPES, ANNOTATIONS, RESOURCES);
        }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access)
        {
        super.beforeAnalysis(access);
        RuntimeSerialization.register(Collections.EMPTY_LIST.getClass());
        registerAllElements(Collections.EMPTY_LIST.getClass());
        RuntimeSerialization.register(Collections.EMPTY_MAP.getClass());
        registerAllElements(Collections.EMPTY_MAP.getClass());
        RuntimeSerialization.register(Collections.EMPTY_SET.getClass());
        registerAllElements(Collections.EMPTY_SET.getClass());
        }

    @Override
    protected void processClass(AfterRegistrationAccess access, Class<?> clazz)
        {
        if (XmlSerializable.class.isAssignableFrom(clazz))
            {
            // this is an XML bean so register its corresponding config resource
            String xmlName = "/" + clazz.getName().replace('.', '/') + ".xml";
            RuntimeResourceAccess.addResource(clazz.getModule(), xmlName);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * All subclasses of these types will be included.
     */
    public static final Set<Class<?>> SUPERTYPES = Set.of(
            CacheConfigNamespaceHandler.Extension.class,
            Coherence.LifecycleListener.class,
            Component.class,
            DocumentElementPreprocessor.ElementPreprocessor.class,
            DocumentPreprocessor.class,
            ElementProcessor.class,
            EnvironmentVariableResolver.class,
            ExternalizableLite.class,
            GrpcAcceptorController.class,
            HealthCheck.class,
            HttpApplication.class,
            HttpServer.class,
            Injector.class,
            InterceptorMetadataResolver.class,
            LifecycleListener.class,
            MapJsonBodyHandler.class,
            MemberIdentityProvider.class,
            MetricsRegistryAdapter.class,
            NamespaceHandler.class,
            OperationalConfigNamespaceHandler.Extension.class,
            PropertyAdapter.class,
            PofConfigProvider.class,
            PofSerializer.class,
            PortableObject.class,
            SchemaExtension.class,
            SchemaSource.class,
            ScriptHandler.class,
            Serializer.class,
            SerializerFactory.class,
            SessionConfiguration.class,
            SessionProvider.class,
            SystemPropertyResolver.class,
            Throwable.class,
            TracingShimLoader.class,
            XmlSerializable.class);

    /**
     * The types to register for serialization.
     */
    public static final Set<Class<?>> SERIALIZABLE_TYPES = Set.of(
            ExternalizableLite.class,
            Throwable.class);

    /**
     * All types with these annotations will be included.
     */
    public static final Set<Class<? extends Annotation>> ANNOTATIONS = Set.of(PortableType.class);

    /**
     * The resources to be registered in the native image.
     */
    public static final Set<String> RESOURCES = Set.of();
    }