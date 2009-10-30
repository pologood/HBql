package org.apache.hadoop.hbase.contrib.hbql.client;

import org.apache.expreval.client.HBqlException;
import org.apache.hadoop.hbase.contrib.hbql.impl.HRecordImpl;
import org.apache.hadoop.hbase.contrib.hbql.impl.ValueMap;

public class HFamilyDefaultValueMap extends ValueMap<byte[]> {

    public HFamilyDefaultValueMap(final HRecordImpl hrecord, final String name) throws HBqlException {
        super(hrecord, name, null);
    }

    public String toString() {
        return "Family default for: " + this.getName();
    }
}