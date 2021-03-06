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

package org.apache.hadoop.hbase.hbql;

import org.apache.hadoop.hbase.hbql.client.ExecutionResults;
import org.apache.hadoop.hbase.hbql.client.HBqlException;
import org.apache.hadoop.hbase.hbql.client.HConnection;
import org.apache.hadoop.hbase.hbql.client.HConnectionManager;
import org.apache.hadoop.hbase.hbql.client.HPreparedStatement;
import org.apache.hadoop.hbase.hbql.client.HRecord;
import org.apache.hadoop.hbase.hbql.client.HResultSet;
import org.apache.hadoop.hbase.hbql.client.HStatement;
import org.apache.hadoop.hbase.hbql.client.Util;
import org.apache.hadoop.hbase.hbql.impl.InvalidTypeException;
import org.apache.hadoop.hbase.hbql.util.TestSupport;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

public class InsertWithSelectTest extends TestSupport {

    static HConnection connection = null;

    static Random randomVal = new Random();

    @BeforeClass
    public static void beforeClass() throws HBqlException {

        connection = HConnectionManager.newConnection();

        connection.execute("CREATE TEMP MAPPING tab3 FOR TABLE table3"
                           + "("
                           + "keyval key, "
                           + "f1 ("
                           + "  val1 string alias val1, "
                           + "  val2 int alias val2, "
                           + "  val3 int alias val3 DEFAULT 12 "
                           + "))");

        if (!connection.tableExists("table3"))
            System.out.println(connection.execute("create table table3 (f1())"));
        else {
            System.out.println(connection.execute("delete from tab3"));
        }

        insertRecords(connection, 10);
    }

    private static void insertRecords(final HConnection connection,
                                      final int cnt) throws HBqlException {

        HPreparedStatement stmt = connection.prepareStatement("insert into tab3 " +
                                                              "(keyval, val1, val2, val3) values " +
                                                              "(:key, :val1, :val2, DEFAULT)");

        for (int i = 0; i < cnt; i++) {

            int val = 10 + i;

            final String keyval = Util.getZeroPaddedNonNegativeNumber(i, TestSupport.keywidth);

            stmt.setParameter("key", keyval);
            stmt.setParameter("val1", "" + val);
            stmt.setParameter("val2", val);
            stmt.execute();
        }
    }

    private static void showValues() throws HBqlException {

        final String query1 = "SELECT keyval, val1, val2, val3 FROM tab3";

        HStatement stmt = connection.createStatement();
        HResultSet<HRecord> results = stmt.executeQuery(query1);

        int rec_cnt = 0;
        for (HRecord rec : results) {

            String keyval = (String)rec.getCurrentValue("keyval");
            String val1 = (String)rec.getCurrentValue("val1");
            int val2 = (Integer)rec.getCurrentValue("val2");
            int val3 = (Integer)rec.getCurrentValue("val3");

            System.out.println("Current Values: " + keyval + " : " + val1 + " : " + val2 + " : " + val3);
            rec_cnt++;
        }

        assertTrue(rec_cnt == 10);
    }

    @Test
    public void insertWithSelect() throws HBqlException {

        final String q1 = "insert into tab3 " +
                          "(keyval, val1, val2) " +
                          "select keyval, val1+val1, val2+1 FROM tab3 ";
        showValues();

        HPreparedStatement stmt = connection.prepareStatement(q1);

        ExecutionResults executionResults = stmt.execute();

        System.out.println(executionResults);

        showValues();

        executionResults = connection.execute(q1);

        System.out.println(executionResults);

        showValues();
    }

    @Test
    public void insertWithSelect2() throws HBqlException {

        final String q1 = "insert into tab3 " +
                          "(keyval, f1(val1, val2)) " +
                          "select keyval, val1+val1, val2+1 FROM tab3 ";
        showValues();

        HPreparedStatement stmt = connection.prepareStatement(q1);

        ExecutionResults executionResults = stmt.execute();

        System.out.println(executionResults);

        showValues();

        executionResults = connection.execute(q1);

        System.out.println(executionResults);

        showValues();
    }


    private Class<? extends Exception> execute(final String str) {

        try {
            connection.execute(str);
        }
        catch (Exception e) {
            e.printStackTrace();
            return e.getClass();
        }
        return null;
    }

    @Test
    public void insertTypeExceptions() throws HBqlException {

        Class<? extends Exception> caught;

        caught = this.execute("insert into tab3 " +
                              "(keyval, val1, val2) " +
                              "select keyval, DOUBLE(val1+val1), val2+1 FROM tab3 ");
        assertTrue(caught != null && caught == InvalidTypeException.class);

        caught = this.execute("insert into tab3 " +
                              "(keyval, val1, val2) " +
                              "select keyval, val2, val1 FROM tab3 ");
        assertTrue(caught != null && caught == InvalidTypeException.class);

        caught = this.execute("insert into tab3 " +
                              "(keyval, val1, val2) " +
                              "select keyval, val2 FROM tab3 ");
        assertTrue(caught != null && caught == HBqlException.class);

        caught = this.execute("insert into tab3 " +
                              "(keyval, val1, val2) " +
                              "values ('123', 'aaa', 'ss') ");
        assertTrue(caught != null && caught == InvalidTypeException.class);

        caught = this.execute("insert into tab3 " +
                              "(keyval, val1, val2) " +
                              "values (4, 'aaa', 5) ");
        assertTrue(caught != null && caught == InvalidTypeException.class);

        caught = this.execute("insert into tab3 " +
                              "(val1, val2) " +
                              "values ('aaa', 5) ");
        assertTrue(caught != null && caught == InvalidTypeException.class);

        caught = this.execute("insert into tab3 " +
                              "(keyval, 'd', val2) " +
                              "values (4, 'aaa', 5) ");
        assertTrue(caught != null && caught == InvalidTypeException.class);

        caught = this.execute("insert into tab3 " +
                              "(keyval, val1, val2) " +
                              "values (ZEROPAD(12, 5), 'aaa', DEFAULT) ");
        assertTrue(caught != null && caught == HBqlException.class);
    }
}