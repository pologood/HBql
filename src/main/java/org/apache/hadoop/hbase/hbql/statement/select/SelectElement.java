/*
 * Copyright (c) 2009.  The Apache Software Foundation
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

package org.apache.hadoop.hbase.hbql.statement.select;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.hbql.client.Connection;
import org.apache.hadoop.hbase.hbql.client.HBqlException;
import org.apache.hadoop.hbase.hbql.impl.AggregateValue;
import org.apache.hadoop.hbase.hbql.schema.ColumnAttrib;
import org.apache.hadoop.hbase.hbql.schema.HBaseSchema;
import org.apache.hadoop.hbase.hbql.statement.SelectStatement;

import java.io.Serializable;
import java.util.List;

public interface SelectElement extends Serializable {

    void validate(HBaseSchema schema, Connection connection) throws HBqlException;

    List<ColumnAttrib> getAttribsUsedInExpr();

    void assignAsNamesForExpressions(SelectStatement selectStatement);

    void assignSelectValue(Object newobj, int maxVerions, Result result) throws HBqlException;

    int setParameter(String name, Object val) throws HBqlException;

    String getAsName();

    String getElementName();

    boolean hasAsName();

    boolean isAFamilySelect();

    String asString();

    AggregateValue newAggregateValue() throws HBqlException;

    boolean isAnAggregateElement();

    void validateTypes(boolean allowColumns, boolean allowCollections) throws HBqlException;
}