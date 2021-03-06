/*
 * Copyright (c) 2011.  The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hbase.hbql.statement.args;

import org.apache.expreval.expr.node.GenericValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.hbql.client.HBqlException;

import java.io.IOException;

public class VersionArgs extends SelectStatementArgs {

    public VersionArgs(final GenericValue val) {
        super(ArgType.VERSION, val);
    }

    public int getMaxVersions() throws HBqlException {
        return ((Number)this.evaluateConstant(0, false)).intValue();
    }

    public String asString() {
        return "VERSIONS " + this.getGenericValue(0).asString();
    }

    public void setMaxVersions(final Get get) throws HBqlException {
        try {
            final int max = this.getMaxVersions();
            if (max == Integer.MAX_VALUE)
                get.setMaxVersions();
            else
                get.setMaxVersions(max);
        }
        catch (IOException e) {
            throw new HBqlException(e);
        }
    }

    public void setMaxVersions(final Scan scan) throws HBqlException {
        final int max = this.getMaxVersions();
        if (max == Integer.MAX_VALUE)
            scan.setMaxVersions();
        else
            scan.setMaxVersions(max);
    }
}