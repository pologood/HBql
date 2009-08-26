grammar Hql;

options {
	superClass=HBaseParser;
	backtrack=true;
}

tokens {
	DOT = '.';
	COLON = ':';
	STAR = '*';
	DIV = '/';
	COMMA = ',';
	PLUS = '+';
	MINUS = '-';
	EQ = '=';
	LT = '<';
	GT = '>';
	LTGT = '<>';	
	LTEQ = '<=';	
	GTEQ = '>=';	
	DQUOTE = '"';
	SQUOTE = '\'';
	LPAREN = '(';
	RPAREN = ')';
}

@rulecatch {
catch (RecognitionException re) {
	handleRecognitionException(re);
}
}

@header {
package com.imap4j.hbase;
import com.imap4j.hbase.hql.*;
import com.imap4j.hbase.hql.expr.*;
import com.imap4j.hbase.antlr.*;
import com.google.common.collect.Lists;
import com.imap4j.imap.antlr.imap.AntlrActions;
}

@lexer::header {
package com.imap4j.hbase;
import com.google.common.collect.Lists;
}

selectStmt returns [QueryArgs retval]
	: keySELECT (STAR | cols=columnList) 
	  keyFROM table=dottedValue 
	  where=whereClause?				{retval = new QueryArgs($cols.retval, $table.text, $where.retval);};

execCommand returns [ExecArgs retval]
	: create=createStmt				{retval = $create.retval;}
	| desc=describeStmt 				{retval = $desc.retval;}
	| show=showStmt 				{retval = $show.retval;}
	| del=deleteStmt 				{retval = $del.retval;}
	| set=setStmt					{retval = $set.retval;}
	;

createStmt returns [CreateArgs retval]
	: keyCREATE keyTABLE table=ID 			{retval = new CreateArgs($table.text);};

describeStmt returns [DescribeArgs retval]
	: keyDESCRIBE keyTABLE table=ID 		{retval = new DescribeArgs($table.text);};

showStmt returns [ShowArgs retval]
	: keySHOW keyTABLES 		 		{retval = new ShowArgs();};

deleteStmt returns [DeleteArgs retval]
	: keyDELETE keyFROM table=ID whereClause?	{retval = new DeleteArgs($table.text);};

setStmt returns [SetArgs retval]
	: keySET var=ID (keyTO | EQ)? val=dottedValue 	{retval = new SetArgs($var.text, $val.text);};

whereClause returns [OrExpr retval]
	: keyWHERE c=orExpr 				{retval = $c.retval;};
		
orExpr returns [OrExpr retval]
@init {retval = new OrExpr();}
	: expr1=andExpr (keyOR expr2=orExpr)?
	{
	 retval.expr1 = $expr1.retval;
	 retval.expr2 = $expr2.retval;
	}
	//| cond_expr keyOR cond_term
	;

andExpr returns [AndExpr retval]
@init {retval = new AndExpr();}
	: expr1=condFactor (keyAND expr2=andExpr)?
	{
	 retval.expr1 = $expr1.retval;
	 retval.expr2 = $expr2.retval;	 
	}
	//| cond_term keyAND cond_factor
	;
	
condFactor returns [CondFactor retval]
@init {retval = new CondFactor();}
	: keyNOT? primary=condPrimary
	{
	  retval.not = $keyNOT.text != null;
	  retval.primary = $primary.retval;
	}
	;

condPrimary returns [CondPrimary retval]
@init {retval = new CondPrimary();}
	: simpleCondExpr  		{retval.expr = $simpleCondExpr.retval;}
	| LPAREN orExpr RPAREN		{retval.expr = $orExpr.retval;}
	;

simpleCondExpr returns [SimpleCondExpr retval]
@init {retval = new SimpleCondExpr();}
	: betweenExpr			//{retval.expr = $betweenExpr.retval;}
	| likeExpr			//{retval.expr = $likeExpr.retval;}
	| inExpr			{retval.expr = $inExpr.retval;}
	| nullCompExpr			//{retval.expr = $nullCompExpr.retval;}
	| compareExpr 			{retval.expr = $compareExpr.retval;}
	;

betweenExpr returns [BetweenExpr retval]
	: attribRef[Number.class] keyNOT? keyBETWEEN numExpr keyAND numExpr
	| attribRef[String.class] keyNOT? keyBETWEEN stringExpr keyAND stringExpr
	| attribRef[Date.class] keyNOT? keyBETWEEN datetimeExpr keyAND datetimeExpr
	;

likeExpr 
	: attribRef[String.class] keyNOT? keyLIKE pattern_value=stringLiteral; // ('ESCAPE' escape_character=string_literal)?;

inExpr returns [InExpr retval]
@init {retval = new InExpr();}
	: attribRef[Number.class] keyNOT? keyIN LPAREN intItemList RPAREN
	{
	 retval.attrib = $attrib.text; 
	 retval.not = $keyNOT.text != null; 
	 retval.intList = $intlist.retval;
	 retval.strList = $strlist.retval;
	}
	| attribRef[String.class] keyNOT? keyIN LPAREN strItemList RPAREN
	{
	 retval.attrib = $attrib.text; 
	 retval.not = $keyNOT.text != null; 
	 retval.intList = $intlist.retval;
	 retval.strList = $strlist.retval;
	}

	;

intItemList returns [List<Integer> retval]
@init {retval = Lists.newArrayList();}
	: item1=intItem {retval.add($item1.retval);} (COMMA item2=intItem {retval.add($item2.retval);})*;
	
strItemList returns [List<String> retval]
@init {retval = Lists.newArrayList();}
	: item1=strItem {retval.add($item1.text);} (COMMA item2=strItem {retval.add($item2.text);})*;
	
intItem returns [Integer retval]
	: num=numericLiteral		{retval = Integer.valueOf($num.text);};

strItem : stringLiteral;

nullCompExpr
	: attribRef[String.class] keyIS (keyNOT)? keyNULL;

compareExpr returns [CompareExpr retval]
	: attribRef[String.class] compareOp stringExpr		{retval = new StringCompareExpr($attribRef.retval, $compareOp.retval, $stringExpr.retval);}
	| attribRef[Date.class] compareOp datetimeExpr 
	| attribRef[Number.class] compareOp numExpr		{retval = new NumberCompareExpr($attribRef.retval, $compareOp.retval, $numExpr.retval);}
	| stringExpr compareOp attribRef[String.class]		{retval = new StringCompareExpr($stringExpr.retval, $compareOp.retval, $attribRef.retval);}
	| datetimeExpr compareOp attribRef[Date.class]
	| numExpr compareOp attribRef[Number.class]		{retval = new NumberCompareExpr($numExpr.retval, $compareOp.retval, $attribRef.retval);}
	;
	
compareOp returns [CompareExpr.Operator retval]
	: EQ 		{retval = CompareExpr.Operator.EQ;}
	| GT 		{retval = CompareExpr.Operator.GT;}
	| GTEQ 		{retval = CompareExpr.Operator.GTEQ;}
	| LT 		{retval = CompareExpr.Operator.LT;}
	| LTEQ 		{retval = CompareExpr.Operator.LTEQ;}
	| LTGT		{retval = CompareExpr.Operator.LTGT;}
	;

numExpr returns [NumExpr retval]
	: simpleNumExpr
	;

simpleNumExpr
	: numTerm ((PLUS | MINUS) simpleNumExpr)?
	//| simpleNumExpr (PLUS | MINUS) numTerm
	;

numTerm
	: numFactor ((STAR | DIV) numTerm)?
	//| numTerm (STAR | DIV) numFactor
	;

numFactor
	: (PLUS | MINUS)? numPrimary;

numPrimary
	: numericLiteral
	| LPAREN simpleNumExpr RPAREN
	| funcsReturningNumeric
	;

stringExpr returns [StringExpr retval]
	: lit=stringLiteral			{retval = new StringExpr($lit.retval);}
	| func=funcReturningStrings
	| attrib=attribRef[String.class]	{retval = new StringExpr($attrib.retval);}
	;

datetimeExpr
	: funcReturningDatetime
	;

funcsReturningNumeric
	: keyLENGTH LPAREN stringExpr RPAREN
	| keyABS LPAREN simpleNumExpr RPAREN
	| keyMOD LPAREN simpleNumExpr COMMA simpleNumExpr RPAREN
	;

funcReturningDatetime
	: keyCURRENT_DATE
	| keyCURRENT_TIME
	| keyCURRENT_TIMESTAMP
	;

funcReturningStrings
	: keyCONCAT LPAREN stringExpr COMMA stringExpr RPAREN
	| keySUBSTRING LPAREN stringExpr COMMA simpleNumExpr COMMA simpleNumExpr RPAREN
	| keyTRIM LPAREN stringExpr RPAREN
	| keyLOWER LPAREN stringExpr RPAREN
	| keyUPPER LPAREN stringExpr RPAREN
	;

attribRef [Class clazz] returns [AttribRef retval]
	: v=ID 					{retval = new AttribRef(clazz, $v.text);};
		
stringLiteral returns [StringLiteral retval]
	: v=QUOTED 				{retval = new StringLiteral($v.text);};
	
numericLiteral 
	: v=INT;
		
columnList returns [List<String> retval]
@init {retval = Lists.newArrayList();}
	: column[retval] (COMMA column[retval])*;

qstringList returns [List<String> retval]
@init {retval = Lists.newArrayList();}
	: qstring[retval] (COMMA qstring[retval])*;

column [List<String> list]	
	: charstr=dottedValue 			{if (list != null) list.add($charstr.text);};

dottedValue	
	: ID ((DOT | COLON) ID)*;

qstring	[List<String> list]
	: QUOTED 				{if (list != null) list.add($QUOTED.text);};

INT	: DIGIT+;
ID	: CHAR (CHAR | DIGIT)*;
 
QUOTED		
@init {final StringBuffer sbuf = new StringBuffer();}	
	: DQUOTE (options {greedy=false;} : any=. {sbuf.append((char)$any);})* DQUOTE 	{setText(sbuf.toString());}
	| SQUOTE (options {greedy=false;} : any=. {sbuf.append((char)$any);})* SQUOTE	{setText(sbuf.toString());}
	;

fragment
DIGIT	: '0'..'9'; 

fragment
CHAR 	: 'a'..'z' | 'A'..'Z'; 

WS 	: (' ' |'\t' |'\n' |'\r' )+ {skip();} ;

keySELECT 	: {AntlrActions.isKeyword(input, "SELECT")}? ID;
keyDELETE 	: {AntlrActions.isKeyword(input, "DELETE")}? ID;
keyCREATE 	: {AntlrActions.isKeyword(input, "CREATE")}? ID;
keyDESCRIBE 	: {AntlrActions.isKeyword(input, "DESCRIBE")}? ID;
keySHOW 	: {AntlrActions.isKeyword(input, "SHOW")}? ID;
keyTABLE 	: {AntlrActions.isKeyword(input, "TABLE")}? ID;
keyTABLES 	: {AntlrActions.isKeyword(input, "TABLES")}? ID;
keyWHERE	: {AntlrActions.isKeyword(input, "WHERE")}? ID;
keyFROM 	: {AntlrActions.isKeyword(input, "FROM")}? ID;
keySET 		: {AntlrActions.isKeyword(input, "SET")}? ID;
keyIN 		: {AntlrActions.isKeyword(input, "IN")}? ID;
keyIS 		: {AntlrActions.isKeyword(input, "IS")}? ID;
keyLIKE		: {AntlrActions.isKeyword(input, "LIKE")}? ID;
keyTO 		: {AntlrActions.isKeyword(input, "TO")}? ID;
keyOR 		: {AntlrActions.isKeyword(input, "OR")}? ID;
keyAND 		: {AntlrActions.isKeyword(input, "AND")}? ID;
keyNOT 		: {AntlrActions.isKeyword(input, "NOT")}? ID;
keyBETWEEN 	: {AntlrActions.isKeyword(input, "BETWEEN")}? ID;
keyNULL 	: {AntlrActions.isKeyword(input, "NULL")}? ID;
keyLOWER 	: {AntlrActions.isKeyword(input, "LOWER")}? ID;
keyUPPER 	: {AntlrActions.isKeyword(input, "UPPER")}? ID;
keyTRIM 	: {AntlrActions.isKeyword(input, "TRIM")}? ID;
keyCONCAT 	: {AntlrActions.isKeyword(input, "CONCAT")}? ID;
keySUBSTRING 	: {AntlrActions.isKeyword(input, "SUBSTRING")}? ID;
keyLENGTH 	: {AntlrActions.isKeyword(input, "LENGTH")}? ID;
keyABS 		: {AntlrActions.isKeyword(input, "ABS")}? ID;
keyMOD	 	: {AntlrActions.isKeyword(input, "MOD")}? ID;
keyCURRENT_DATE	: {AntlrActions.isKeyword(input, "CURRENT_DATE")}? ID;
keyCURRENT_TIME : {AntlrActions.isKeyword(input, "CURRENT_TIME")}? ID;
keyCURRENT_TIMESTAMP : {AntlrActions.isKeyword(input, "CURRENT_TIMESTAMP")}? ID;
