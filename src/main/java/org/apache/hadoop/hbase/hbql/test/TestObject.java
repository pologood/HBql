package org.apache.hadoop.hbase.hbql.test;

import org.apache.hadoop.hbase.hbql.client.HColumn;
import org.apache.hadoop.hbase.hbql.client.HColumnVersionMap;
import org.apache.hadoop.hbase.hbql.client.HConnection;
import org.apache.hadoop.hbase.hbql.client.HFamily;
import org.apache.hadoop.hbase.hbql.client.HPersistException;
import org.apache.hadoop.hbase.hbql.client.HQuery;
import org.apache.hadoop.hbase.hbql.client.HRecord;
import org.apache.hadoop.hbase.hbql.client.HResults;
import org.apache.hadoop.hbase.hbql.client.HTable;
import org.apache.hadoop.hbase.hbql.client.HTransaction;
import org.apache.hadoop.hbase.hbql.query.schema.HUtil;
import org.apache.hadoop.hbase.hbql.query.util.Maps;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Aug 19, 2009
 * Time: 4:39:06 PM
 */
@HTable(name = "testobjects",
        families = {
                @HFamily(name = "family1", maxVersions = 10),
                @HFamily(name = "family2"),
                @HFamily(name = "family3", maxVersions = 5)
        })
public class TestObject {

    private enum TestEnum {
        RED, BLUE, BLACK, ORANGE
    }

    @HColumn(key = true)
    private String keyval;

    @HColumn(family = "family1")
    private TestEnum enumValue = TestEnum.BLUE;

    @HColumn(family = "family1")
    private int intValue = -1;

    @HColumn(family = "family1")
    private String strValue = "";

    @HColumn(family = "family1")
    private String title = "";

    @HColumnVersionMap(instance = "title")
    private NavigableMap<Long, String> titles = new TreeMap<Long, String>();

    @HColumn(family = "family1", column = "author")
    private String author = "";

    @HColumnVersionMap(instance = "author")
    private NavigableMap<Long, String> authorVersions;

    @HColumn(family = "family2", getter = "getHeaderBytes", setter = "setHeaderBytes")
    private String header = "A header value";

    @HColumn(family = "family2", column = "bodyimage")
    private String bodyimage = "A bodyimage value";

    @HColumn(family = "family2")
    private int[] array1 = {1, 2, 3};

    @HColumn(family = "family2")
    private String[] array2 = {"val1", "val2", "val3"};

    @HColumn(family = "family3", mapKeysAsColumns = true)
    private Map<String, String> mapval1 = Maps.newHashMap();

    @HColumn(family = "family3", mapKeysAsColumns = false)
    private Map<String, String> mapval2 = Maps.newHashMap();

    public TestObject() {
    }

    public TestObject(int val) throws HPersistException {
        this.keyval = HUtil.getZeroPaddedNumber(val, 6);

        this.title = "A title value";
        this.author = "An author value";
        strValue = "v" + val;

        mapval1.put("key1", "val1");
        mapval1.put("key2", "val2");

        mapval2.put("key3", "val3");
        mapval2.put("key4", "val4");

        author += "-" + val + System.nanoTime();
        header += "-" + val;
        title += "-" + val;
    }

    public byte[] getHeaderBytes() {
        return this.header.getBytes();
    }

    public void setHeaderBytes(byte[] val) {
        this.header = new String(val);
    }

    public static void main(String[] args) throws IOException, HPersistException {

        HConnection conn = HConnection.newHConnection();
        System.out.println(conn.exec("define table testobjects "
                                     + "("
                                     + "keyval key, "
                                     + "family1:author string, "
                                     + "family1:title string "
                                     + ")"));

        //System.out.println(conn.exec("delete from TestObject with client filter where true"));
        //System.out.println(conn.exec("create table using TestObject"));
        System.out.println(conn.exec("show tables"));
        System.out.println(conn.exec("describe table TestObject"));

        final HTransaction tx = conn.newHTransaction();
        int cnt = 0;
        for (int i = 10; i < cnt; i++)
            tx.insert(new TestObject(i));

        tx.commit();

        final String query1 = "SELECT family1:author, family1:title "
                              + "FROM testobjects "
                              + "WITH "
                              // + "KEYS  '000002' TO '000005', '000007' TO LAST "
                              //+ "TIME RANGE NOW()-DAY(15) TO NOW()+DAY(1)"
                              //+ "VERSIONS 3 "
                              // + "SCAN LIMIT 1"
                              + "SERVER FILTER WHERE TRUE "
                //+ "SERVER FILTER WHERE family1:author LIKE '.*val.*' "
                //+ "CLIENT FILTER WHERE family1:author LIKE '.*282.*'"
                ;
        HQuery<HRecord> q1 = conn.newHQuery(query1);
        HResults<HRecord> results1 = q1.execute();

        for (HRecord val1 : results1) {
            System.out
                    .println("Current Values: " + val1.getCurrentValueByVariableName("keyval")
                             + " - " + val1.getCurrentValueByVariableName("family1:author")
                             + " - " + val1.getCurrentValueByVariableName("family1:title"));

            System.out.println("Historicals");

            if (val1.getVersionedValueMapByVariableName("family1:author") != null) {
                Map<Long, Object> versioned = val1.getVersionedValueMapByVariableName("family1:author");
                for (final Long key : versioned.keySet())
                    System.out.println(new Date(key) + " - " + versioned.get(key));
            }

            if (val1.getVersionedValueMapByVariableName("family1:title") != null) {
                Map<Long, Object> versioned = val1.getVersionedValueMapByVariableName("family1:title");
                for (final Long key : versioned.keySet())
                    System.out.println(new Date(key) + " - " + versioned.get(key));
            }
        }

        results1.close();

        /*
�        final String query2 = "SELECT title, titles, author, authorVersions "
                              + "FROM TestObject "
                              + "WITH "
                              + "KEYS  '000002' TO '000005', '000007' TO LAST "
                              + "TIME RANGE NOW()-DAY(15) TO NOW()+DAY(1)"
                              + "VERSIONS MAX "
                              //+ "SERVER FILTER WHERE author LIKE '.*val.*'"
                              //+ "CLIENT FILTER WHERE author LIKE '.*282.*'"
                ;

        HQuery<TestObject> q2 = conn.newHQuery(query2);
        HResults<TestObject> results2 = q2.execute();

        for (TestObject val2 : results2) {
            System.out.println("Current Values: " + val2.keyval + " - " + val2.author + " - " + val2.title);

            System.out.println("Historicals");

            if (val2.authorVersions != null)
                for (final Long key : val2.authorVersions.keySet())
                    System.out.println(new Date(key) + " - " + val2.authorVersions.get(key));

            if (val2.titles != null)
                for (final Long key : val2.titles.keySet())
                    System.out.println(new Date(key) + " - " + val2.titles.get(key));
        }

        results2.close();
        */
    }
}