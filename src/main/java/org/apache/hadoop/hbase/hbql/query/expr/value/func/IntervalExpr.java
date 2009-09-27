package org.apache.hadoop.hbase.hbql.query.expr.value.func;

import org.apache.hadoop.hbase.hbql.client.HBqlException;
import org.apache.hadoop.hbase.hbql.client.TypeException;
import org.apache.hadoop.hbase.hbql.query.expr.node.DateValue;
import org.apache.hadoop.hbase.hbql.query.expr.node.NumberValue;
import org.apache.hadoop.hbase.hbql.query.expr.node.ValueExpr;
import org.apache.hadoop.hbase.hbql.query.expr.value.GenericOneExpr;
import org.apache.hadoop.hbase.hbql.query.expr.value.literal.DateLiteral;
import org.apache.hadoop.hbase.hbql.query.schema.HUtil;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Sep 7, 2009
 * Time: 10:03:28 PM
 */
public class IntervalExpr extends GenericOneExpr implements DateValue {

    public enum IntervalType {
        MILLI(1),
        SECOND(1000 * MILLI.getIntervalMillis()),
        MINUTE(60 * SECOND.getIntervalMillis()),
        HOUR(60 * MINUTE.getIntervalMillis()),
        DAY(24 * HOUR.getIntervalMillis()),
        WEEK(7 * DAY.getIntervalMillis()),
        YEAR(52 * WEEK.getIntervalMillis());

        private final long intervalMillis;

        IntervalType(final long intervalMillis) {
            this.intervalMillis = intervalMillis;
        }

        public long getIntervalMillis() {
            return intervalMillis;
        }
    }

    private final IntervalType intervalType;

    public IntervalExpr(final IntervalType intervalType, final ValueExpr expr) {
        super(expr);
        this.intervalType = intervalType;
    }

    private IntervalType getIntervalType() {
        return this.intervalType;
    }

    @Override
    public Class<? extends ValueExpr> validateTypes(final ValueExpr parentExpr,
                                                    final boolean allowsCollections) throws TypeException {
        HUtil.validateParentClass(this, NumberValue.class, this.getExpr().validateTypes(this, false));
        return DateValue.class;
    }

    @Override
    public ValueExpr getOptimizedValue() throws HBqlException {
        this.setExpr(this.getExpr().getOptimizedValue());
        return this.isAConstant() ? new DateLiteral(this.getValue(null)) : this;
    }

    @Override
    public Long getValue(final Object object) throws HBqlException {
        final Number num = (Number)this.getExpr().getValue(object);
        final long val = num.longValue();
        return val * this.getIntervalType().getIntervalMillis();
    }

    @Override
    public String asString() {
        return this.getIntervalType().name() + "(" + this.getExpr().asString() + ")";
    }

}