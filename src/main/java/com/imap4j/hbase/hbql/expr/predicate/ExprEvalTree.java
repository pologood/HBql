package com.imap4j.hbase.hbql.expr.predicate;

import com.imap4j.hbase.hbql.HPersistException;
import com.imap4j.hbase.hbql.expr.EvalContext;
import com.imap4j.hbase.hbql.expr.node.PredicateExpr;
import com.imap4j.hbase.hbql.expr.value.literal.BooleanLiteral;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Aug 25, 2009
 * Time: 6:58:31 PM
 */
public class ExprEvalTree implements PredicateExpr {

    private PredicateExpr expr = null;
    private long start, end;

    public ExprEvalTree(final PredicateExpr expr) {
        this.expr = expr;

    }

    private PredicateExpr getExpr() {
        return expr;
    }

    @Override
    public List<String> getQualifiedColumnNames() {
        return this.getExpr().getQualifiedColumnNames();
    }

    @Override
    public boolean optimizeForConstants(final EvalContext context) throws HPersistException {

        boolean retval = true;

        if (this.getExpr().optimizeForConstants(context))
            this.expr = new BooleanLiteral(this.getExpr().evaluate(context));
        else
            retval = false;

        return retval;
    }

    @Override
    public boolean evaluate(final EvalContext context) throws HPersistException {

        this.start = System.nanoTime();

        final boolean retval = (this.getExpr() == null) || (this.getExpr().evaluate(context));

        this.end = System.nanoTime();

        return retval;
    }

    public long getElapsedNanos() {
        return this.end - this.start;
    }
}