package org.apache.expreval.examples;

import org.apache.expreval.client.HBqlException;
import org.apache.expreval.util.HUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

public class RawAccess2 {

    public static void main(String[] args) throws IOException, HBqlException {

        final byte[] family = Bytes.toBytes("f3");
        final byte[] col1 = Bytes.toBytes("val1");
        final byte[] col2 = Bytes.toBytes("val2");

        HTable table = new HTable(new HBaseConfiguration(), "table1");

        for (int i = 30; i < 35; i++) {
            Put put = new Put(Bytes.toBytes(HUtil.getZeroPaddedNumber(i, 10)));
            put.add(family, col1, Bytes.toBytes(34));
            put.add(family, col2, Bytes.toBytes(68));
            table.put(put);
            table.flushCommits();
        }

        Scan scan = new Scan();
        scan.addColumn(family, col1);
        scan.addColumn(family, col2);
        ResultScanner scanner = table.getScanner(scan);

        for (Result result : scanner) {
            System.out.println(Bytes.toString(result.getRow()) + " - "
                               + Bytes.toInt(result.getValue(family, col1)) + " - "
                               + Bytes.toInt(result.getValue(family, col2)));
        }
    }
}