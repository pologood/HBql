package com.imap4j.hbase.hbql.expr;

import com.imap4j.hbase.hbql.HPersistException;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Aug 26, 2009
 * Time: 10:18:22 AM
 */
public interface DateValue {

    Date getValue(final AttribContext context) throws HPersistException;
}