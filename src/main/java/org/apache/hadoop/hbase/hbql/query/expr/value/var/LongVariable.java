package org.apache.hadoop.hbase.hbql.query.expr.value.var;

import org.apache.hadoop.hbase.hbql.client.HBqlException;
import org.apache.hadoop.hbase.hbql.query.expr.node.NumberValue;
import org.apache.hadoop.hbase.hbql.query.schema.FieldType;
import org.apache.hadoop.hbase.hbql.query.schema.VariableAttrib;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Aug 25, 2009
 * Time: 6:58:31 PM
 */
public class LongVariable extends GenericVariable<NumberValue> implements NumberValue {

    public LongVariable(VariableAttrib attrib) {
        super(attrib, FieldType.LongType);
    }

    @Override
    public Long getValue(final Object object) throws HBqlException {
        return (Long)this.getVariableAttrib().getCurrentValue(object);
    }

}