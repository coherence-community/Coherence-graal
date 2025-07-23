package queues;/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

import com.tangosol.io.pof.PofConfigProvider;

public class QueueTestPofConfig
        implements PofConfigProvider
    {
    @Override
    public String getConfigURI()
        {
        return "queue-test-pof-config.xml";
        }
    }
