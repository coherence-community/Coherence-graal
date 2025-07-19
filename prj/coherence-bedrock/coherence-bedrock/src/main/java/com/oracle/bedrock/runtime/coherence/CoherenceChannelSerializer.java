package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.runtime.concurrent.RemoteChannelSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.SafeConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

public class CoherenceChannelSerializer
        implements RemoteChannelSerializer
    {
    public CoherenceChannelSerializer()
        {
        m_serializer = new SafeConfigurablePofContext("coherence-pof-config.xml");
        }

    @Override
    public byte[] serialize(Object o)
        {
        Binary binary = ExternalizableHelper.toBinary(o, m_serializer);
        return binary == null ? Binary.NO_BYTES : binary.toByteArray();
        }

    @Override
    public <T> T deserialize(byte[] bytes)
        {
        if (bytes == null || bytes.length == 0)
            {
            return null;
            }
        return ExternalizableHelper.fromBinary(new Binary(bytes), m_serializer);
        }

    public static final CoherenceChannelSerializer INSTANCE = new CoherenceChannelSerializer();

    private final Serializer m_serializer;
    }
