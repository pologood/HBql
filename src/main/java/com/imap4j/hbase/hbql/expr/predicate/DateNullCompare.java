package com.imap4j.hbase.hbql.expr.predicate;

import com.imap4j.hbase.hbql.HPersistException;
import com.imap4j.hbase.hbql.expr.EvalContext;
import com.imap4j.hbase.hbql.expr.ExprVariable;
import com.imap4j.hbase.hbql.expr.node.DateValue;
import com.imap4j.hbase.hbql.expr.node.PredicateExpr;
import com.imap4j.hbase.hbql.expr.value.literal.DateLiteral;

import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Aug 25, 2009
 * Time: 10:30:32 PM
 */
public class DateNullCompare extends GenericNotStmt implements PredicateExpr {

    private DateValue expr = null;

    public DateNullCompare(final boolean not, final DateValue expr) {
        super(not);
        this.expr = expr;
    }

    private DateValue getExpr() {
        return this.expr;
    }

    @Override
    public List<ExprVariable> getExprVariables() {
        return this.getExpr().getExprVariables();
    }

    @Override
    public boolean optimizeForConstants(final EvalContext context) throws HPersistException {

        boolean retval = true;

        if (this.getExpr().optimizeForConstants(context))
            this.expr = new DateLiteral(this.getExpr().getValue(context));
        else
            retval = false;

        return retval;
    }

    @Override
    public boolean evaluate(final EvalContext context) throws HPersistException {
        final Date val = this.getExpr().getValue(context);
        final boolean retval = (val == null);
        return (this.isNot()) ? !retval : retval;
    }

    @Override
    public boolean isAConstant() {
        return this.getExpr().isAConstant();
    }
}