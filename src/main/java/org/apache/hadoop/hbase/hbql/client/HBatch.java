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

package org.apache.hadoop.hbase.hbql.client;

import org.apache.expreval.util.Lists;
import org.apache.expreval.util.Maps;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.hbql.impl.BatchAction;
import org.apache.hadoop.hbase.hbql.impl.DeleteAction;
import org.apache.hadoop.hbase.hbql.impl.HConnectionImpl;
import org.apache.hadoop.hbase.hbql.impl.HRecordImpl;
import org.apache.hadoop.hbase.hbql.impl.HTableReference;
import org.apache.hadoop.hbase.hbql.impl.InsertAction;
import org.apache.hadoop.hbase.hbql.mapping.AnnotationResultAccessor;
import org.apache.hadoop.hbase.hbql.mapping.ColumnAttrib;
import org.apache.hadoop.hbase.hbql.mapping.HBaseTableMapping;
import org.apache.hadoop.hbase.hbql.mapping.ResultAccessor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HBatch<T> {

    private final HConnection connection;

    private final Map<String, List<BatchAction>> actionList = Maps.newHashMap();

    public HBatch(final HConnection connection) {
        this.connection = connection;
    }

    public static <E> HBatch<E> newHBatch(final HConnection connection) {
        return new HBatch<E>(connection);
    }

    public HConnection getConnection() {
        return this.connection;
    }

    private HConnectionImpl getHConnectionImpl() {
        return (HConnectionImpl)this.getConnection();
    }

    public Map<String, List<BatchAction>> getActionList() {
        return this.actionList;
    }

    public synchronized List<BatchAction> getActionList(final String tableName) {
        List<BatchAction> retval = this.getActionList().get(tableName);
        if (retval == null) {
            retval = Lists.newArrayList();
            this.getActionList().put(tableName, retval);
        }
        return retval;
    }

    public void insert(final T newrec) throws HBqlException {

        if (newrec instanceof HRecordImpl) {
            final HRecordImpl record = (HRecordImpl)newrec;
            final HBaseTableMapping tableMapping = record.getHBaseTableMapping();
            final ColumnAttrib keyAttrib = tableMapping.getKeyAttrib();
            if (!record.isCurrentValueSet(keyAttrib))
                throw new HBqlException("Record key value must be assigned");

            final Put put = this.createPut(record.getResultAccessor(), record);
            this.getActionList(tableMapping.getTableName()).add(new InsertAction(put));
        }
        else {
            final AnnotationResultAccessor accessor = this.getHConnectionImpl().getAnnotationMapping(newrec);
            final Put put = this.createPut(accessor, newrec);
            this.getActionList(accessor.getMapping().getTableName()).add(new InsertAction(put));
        }
    }

    public void delete(final T newrec) throws HBqlException {

        if (newrec instanceof HRecordImpl) {
            final HRecordImpl record = (HRecordImpl)newrec;
            final HBaseTableMapping tableMapping = record.getHBaseTableMapping();
            final ColumnAttrib keyAttrib = tableMapping.getKeyAttrib();
            if (!record.isCurrentValueSet(keyAttrib))
                throw new HBqlException("Record key value must be assigned");
            this.delete(tableMapping, record);
        }
        else {
            final AnnotationResultAccessor accessor = this.getHConnectionImpl().getAnnotationMapping(newrec);
            this.delete(accessor.getHBaseTableMapping(), newrec);
        }
    }

    private void delete(HBaseTableMapping tableMapping, final Object newrec) throws HBqlException {
        final ColumnAttrib keyAttrib = tableMapping.getKeyAttrib();
        final byte[] keyval = keyAttrib.getValueAsBytes(newrec);
        this.getActionList(tableMapping.getTableName()).add(new DeleteAction(new Delete(keyval)));
    }

    private Put createPut(final ResultAccessor resultAccessor, final Object newrec) throws HBqlException {

        final Put put;
        final HBaseTableMapping tableMapping = resultAccessor.getHBaseTableMapping();
        final ColumnAttrib keyAttrib = resultAccessor.getKeyAttrib();

        if (newrec instanceof HRecordImpl) {
            final HRecordImpl record = (HRecordImpl)newrec;
            final byte[] keyval = keyAttrib.getValueAsBytes(record);
            put = new Put(keyval);

            for (final String family : tableMapping.getFamilySet()) {
                for (final ColumnAttrib attrib : tableMapping.getColumnAttribListByFamilyName(family)) {
                    if (record.isCurrentValueSet(attrib)) {
                        final byte[] b = attrib.getValueAsBytes(record);
                        put.add(attrib.getFamilyNameAsBytes(), attrib.getColumnNameAsBytes(), b);
                    }
                }
            }
        }
        else {
            final byte[] keyval = keyAttrib.getValueAsBytes(newrec);
            put = new Put(keyval);
            for (final String family : tableMapping.getFamilySet()) {
                for (final ColumnAttrib colattrib : tableMapping.getColumnAttribListByFamilyName(family)) {

                    // One extra lookup for annotations
                    final ColumnAttrib attrib = resultAccessor.getColumnAttribByName(colattrib.getFamilyQualifiedName());
                    final byte[] b = attrib.getValueAsBytes(newrec);
                    put.add(attrib.getFamilyNameAsBytes(), attrib.getColumnNameAsBytes(), b);
                }
            }
        }
        return put;
    }

    public void apply() throws HBqlException {
        try {
            for (final String tableName : this.getActionList().keySet()) {
                HTableReference tableref = null;
                try {
                    tableref = this.getHConnectionImpl().getHTableReference(tableName);
                    for (final BatchAction batchAction : this.getActionList(tableName))
                        batchAction.apply(tableref.getHTable());
                    tableref.getHTable().flushCommits();
                    tableref.getHTable().close();
                }
                finally {
                    // release to table pool
                    if (tableref != null)
                        tableref.release();
                }
            }
        }
        catch (IOException e) {
            throw new HBqlException(e);
        }
    }
}
