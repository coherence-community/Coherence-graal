package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

public class CoherenceSerializerRunnable
        implements RemoteRunnable
    {
    public CoherenceSerializerRunnable(RemoteRunnable delegate)
        {
        m_delegate = delegate;
        m_binary   = ExternalizableHelper.toBinary(delegate, SERIALIZER); 
        }

    @Override
    public void run()
        {
        RemoteRunnable delegate = m_delegate;
        if (delegate == null)
            {
            delegate = m_delegate = ExternalizableHelper.fromBinary(m_binary, SERIALIZER);
            }
        delegate.run();
        }

    // ----- data members ---------------------------------------------------
    
    private final Binary m_binary;
    
    private transient RemoteRunnable m_delegate;
    
    public static final Serializer SERIALIZER = new DefaultSerializer();
    }
