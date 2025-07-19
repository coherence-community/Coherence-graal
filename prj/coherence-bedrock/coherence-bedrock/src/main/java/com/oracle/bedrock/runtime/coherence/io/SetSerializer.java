package com.oracle.bedrock.runtime.coherence.io;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SetSerializer
        implements PofSerializer<Set<?>>
    {
    @Override
    public void serialize(PofWriter out, Set<?> set) throws IOException
        {
        out.writeCollection(0, set);
        out.writeRemainder(null);
        }

    @Override
    public Set<?> deserialize(PofReader in) throws IOException
        {
        Set<?> set = in.readCollection(0, new HashSet<>());
        in.readRemainder();
        return set;
        }
    }
