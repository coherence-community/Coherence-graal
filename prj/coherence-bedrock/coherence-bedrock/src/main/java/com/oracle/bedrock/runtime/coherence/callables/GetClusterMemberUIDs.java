/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Member;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.UID;

import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("unchecked")
public class GetClusterMemberUIDs
        implements RemoteCallable<Set<UID>>
    {
    @Override
    public Set<UID> call() throws Exception
        {
        // attempt to get the cluster
        com.tangosol.net.Cluster cluster = CacheFactory.getCluster();

        if (cluster == null)
            {
            return null;
            }
        else
            {
            Set<UID> memberUIDs = new ImmutableArrayList();

            Set<Member> memberSet = cluster.getMemberSet();

            for (Member member : memberSet)
                {
                memberUIDs.add(member.getUid());
                }

            return memberUIDs;
            }
        }
    }
