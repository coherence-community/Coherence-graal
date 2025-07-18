package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

public class CoherenceSerializerCallable<T>
        implements RemoteCallable<T>
    {
    public CoherenceSerializerCallable(RemoteCallable<T> delegate)
        {
        m_delegate = delegate;
        m_binary   = ExternalizableHelper.toBinary(delegate, SERIALIZER);
        }

    @Override
    public T call() throws Exception
        {
        RemoteCallable<T> delegate = m_delegate;
        if (delegate == null)
            {
            delegate = m_delegate = ExternalizableHelper.fromBinary(m_binary, SERIALIZER);
            }
        return delegate.call();
        }

    // ----- data members ---------------------------------------------------

    private final Binary m_binary;

    private transient RemoteCallable<T> m_delegate;

    public static final Serializer SERIALIZER = new DefaultSerializer();
    }
