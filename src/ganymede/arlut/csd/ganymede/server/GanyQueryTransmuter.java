/*

   GanyQueryTransmuter.java

   This is the textual query processor for the Ganymede server.  It
   takes a string query, applies an ANTLR grammar to it, and generates
   a traditional Ganymede-style arlut.csd.ganymede.common.Query out of
   it.
   
   Created: 31 August 2004
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Deepak Giridharagopal, Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import arlut.csd.ganymede.common.GanyParseException;
import arlut.csd.ganymede.common.DumpResult;
import arlut.csd.ganymede.common.FieldType;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryAndNode;
import arlut.csd.ganymede.common.QueryDeRefNode;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryNode;
import arlut.csd.ganymede.common.QueryNotNode;
import arlut.csd.ganymede.common.QueryOrNode;

import arlut.csd.Util.StringUtils;

import antlr.ANTLRException;
import antlr.DumpASTVisitor;

import antlr.collections.AST;

import java.io.StringReader;

import java.text.DateFormat;
import java.text.ParseException;

import java.util.ArrayList;
import java.util.HashMap;


/*------------------------------------------------------------------------------
                                                                           class
                                                             GanyQueryTransmuter

------------------------------------------------------------------------------*/

/**
 * This class processes textual queries using an ANTLR-generated parser, and
 * generates an old, Ganymede-style arlut.csd.ganymede.common.Query, with attendant
 * arlut.csd.ganymede.common.QueryNode tree.
 *
 * @see arlut.csd.ganymede.common.QueryNode
 * @see arlut.csd.ganymede.common.Query
 *
 * @version $Id$
 * @author Deepak Giridharagopal, deepak@arlut.utexas.edu
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public class GanyQueryTransmuter implements QueryParserTokenTypes {

  private static HashMap op_scalar_mapping;
  private static HashMap op_vector_mapping;
  private static HashMap validity_mapping;

  static
  {
    // initialize op_scalar_mapping

    op_scalar_mapping = new HashMap();
    op_scalar_mapping.put("=~", new Byte(QueryDataNode.MATCHES));
    op_scalar_mapping.put("=~_ci", new Byte(QueryDataNode.NOCASEMATCHES));
    op_scalar_mapping.put("==", new Byte(QueryDataNode.EQUALS));
    op_scalar_mapping.put("==_ci", new Byte(QueryDataNode.NOCASEEQ));
    op_scalar_mapping.put("<", new Byte(QueryDataNode.LESS));
    op_scalar_mapping.put("<=", new Byte(QueryDataNode.LESSEQ));
    op_scalar_mapping.put(">", new Byte(QueryDataNode.GREAT));
    op_scalar_mapping.put(">=", new Byte(QueryDataNode.GREATEQ));
    op_scalar_mapping.put("starts", new Byte(QueryDataNode.STARTSWITH));
    op_scalar_mapping.put("ends", new Byte(QueryDataNode.ENDSWITH));
    op_scalar_mapping.put("defined", new Byte(QueryDataNode.DEFINED));

    // initialize op_vector_mapping

    op_vector_mapping = new HashMap();
    op_vector_mapping.put("len<", new Byte(QueryDataNode.LENGTHLE));
    op_vector_mapping.put("len<=", new Byte(QueryDataNode.LENGTHLEEQ));
    op_vector_mapping.put("len>", new Byte(QueryDataNode.LENGTHGR));
    op_vector_mapping.put("len>=", new Byte(QueryDataNode.LENGTHGREQ));
    op_vector_mapping.put("len==", new Byte(QueryDataNode.LENGTHEQ));

    validity_mapping = new HashMap();
    validity_mapping.put(new Integer(FieldType.DATE), new String[] {"<", ">", "<=", ">=", "==", "defined"});
    validity_mapping.put(new Integer(FieldType.NUMERIC), new String[] {"<", ">", "<=", ">=", "==", "defined"});
    validity_mapping.put(new Integer(FieldType.FLOAT), new String[] {"<", ">", "<=", ">=", "==", "defined"});
    validity_mapping.put(new Integer(FieldType.BOOLEAN), new String[] {"==", "defined"});
    validity_mapping.put(new Integer(FieldType.IP), new String[] {"==", "=~", "starts", "ends", "defined"});
    validity_mapping.put(new Integer(FieldType.STRING), new String[] {"<", ">", "<=", ">=", "=~", "=~_ci", "==", "==_ci", "starts", "ends", "defined"});
    validity_mapping.put(new Integer(FieldType.INVID), new String[] {"<", ">", "<=", ">=", "=~", "=~_ci", "==", "==_ci", "starts", "ends", "defined"});
    validity_mapping.put(new Integer(FieldType.PASSWORD), new String[] {"defined"});
    validity_mapping.put(new Integer(FieldType.PERMISSIONMATRIX), new String[] {"defined"});
  }

  // ---

  DBObjectBase objectBase = null;
  ArrayList selectFields = null;

  public GanyQueryTransmuter()
  {
  }

  public Query transmuteQueryString(String queryString) throws GanyParseException
  {
    QueryLexer lexer = new QueryLexer(new StringReader(queryString));
    QueryParser parser = new QueryParser(lexer);

    try
      {
	parser.query();
      }
    catch (ANTLRException ex)
      {
	
      }

    AST ast = parser.getAST();

    if (ast == null)
      {
	throw new GanyParseException("I couldn't parse the query, bub.  Make sure you've parenthesized everything correctly.\n\n" + queryString);
      }

    QueryNode root = parse_tree(ast);

    Query query = new Query(objectBase.getName(), root);

    if (selectFields == null)
      {
	query.resetPermitList(); // return the default list of fields
      }
    else
      {
	for (int i = 0; i < selectFields.size(); i ++)
	  {
	    DBObjectBaseField field = (DBObjectBaseField) selectFields.get(i);
	    query.addField(field.getID());
	  }
      }

    // clear out our refs for GC

    objectBase = null;

    if (selectFields != null)
      {
	selectFields.clear();
	selectFields = null;
      }

    // et voila

    return query;
  }

  private QueryNode parse_tree(AST ast) throws GanyParseException
  {
    this.objectBase = parse_from_tree(ast.getNextSibling());
    this.selectFields = parse_select_tree(ast);

    if (ast.getNextSibling().getNextSibling().getType() == QueryParserTokenTypes.WHERE)
      {
	AST where_node = ast.getNextSibling().getNextSibling().getFirstChild();

	if (where_node != null)
	  {
	    QueryNode where_tree = parse_where_clause(where_node, objectBase);
	    
	    return where_tree;
	  }
      }

    return null;
  }

  private DBObjectBase parse_from_tree(AST ast) throws GanyParseException
  {
    String from_objectbase = StringUtils.dequote(ast.getFirstChild().getText());
    this.objectBase = Ganymede.db.getObjectBase(from_objectbase);

    if (objectBase == null)
      {
	throw new GanyParseException("The objectBase " + from_objectbase + " doesn't exist, bub.");
      }

    return this.objectBase;
  }

  private ArrayList parse_select_tree(AST ast) throws GanyParseException
  {
    ArrayList selectFields = new ArrayList();
    AST select_node = ast.getFirstChild();

    if (select_node.getType() == QueryParserTokenTypes.OBJECT)
      {
	return null;
      }

    while (select_node != null)
      {
	String field_name = StringUtils.dequote(select_node.getText());
	DBObjectBaseField field = (DBObjectBaseField) objectBase.getField(field_name);

	if (field == null)
	  {
	    throw new GanyParseException("Can't find field " + field_name + " in object base " + objectBase.getName());
	  }

	selectFields.add(field);
	select_node = select_node.getNextSibling();
      }

    return selectFields;
  }

  private QueryNode parse_where_clause(AST ast, DBObjectBase base) throws GanyParseException
  {
    int root_type;
    QueryNode child1 = null, child2 = null;
    DBObjectBase target_objectbase = null;
    String field_name = null;
    DBObjectBaseField field = null;
    byte scalar_operator = -1, vector_operator = -1;
    int field_type = -1, argument_type = -1;
    String op;
    Object argument;
    AST field_node, argument_node;
    
    /* -- */

    root_type = ast.getType();

    switch (root_type)
      {
      case QueryParserTokenTypes.NOT:
	return new QueryNotNode(parse_where_clause(ast.getFirstChild(), base));

      case QueryParserTokenTypes.AND:
	child1 = parse_where_clause(ast.getFirstChild(), base);
	child2 = parse_where_clause(ast.getFirstChild().getNextSibling(), base);
	return new QueryAndNode(child1, child2);

      case QueryParserTokenTypes.OR:
	child1 = parse_where_clause(ast.getFirstChild(), base);
	child2 = parse_where_clause(ast.getFirstChild().getNextSibling(), base);
	return new QueryOrNode(child1, child2);

      case QueryParserTokenTypes.DEREF:
	field_name = StringUtils.dequote(ast.getFirstChild().getText());

	if (base != null)
	  {
	    field = (DBObjectBaseField) base.getField(field_name);

	    if (field == null)
	      {
		throw new GanyParseException("Can't find field " + field_name + " in object base " + base.getName());
	      }
	    
	    if (field.getType() != FieldType.INVID)
	      {
		throw new GanyParseException("You can only dereference an Invid field, which " + field_name + " isn't.");
	      }

	    short target_objectbase_id = field.getTargetBase();

	    if (target_objectbase_id >= 0)
	      {
		target_objectbase = Ganymede.db.getObjectBase(target_objectbase_id);
	      }
	    else
	      {
		target_objectbase = null;
	      }
	  }
	else
	  {
	    target_objectbase = null;
	  }

	child2 = parse_where_clause(ast.getFirstChild().getNextSibling(), target_objectbase);
	
	return new QueryDeRefNode(field_name, child2);

      case QueryParserTokenTypes.BINARY_OPERATOR:
      case QueryParserTokenTypes.UNARY_OPERATOR:

	op = ast.getText();
	field_node = ast.getFirstChild();
	field_name = StringUtils.dequote(field_node.getText());

	if (base == null)
	  {
	    field = null;
	  }
	else
	  {
	    field = (DBObjectBaseField) base.getField(field_name);
	    
	    if (field == null)
	      {
		throw new GanyParseException("Can't find field " + field_name + " in object base " + base.getName());
	      }

	    field_type = field.getType();
	  }

	if (root_type == QueryParserTokenTypes.BINARY_OPERATOR)
	  {
	    argument_node = field_node.getNextSibling();
	    argument_type = argument_node.getType();
	    argument = parse_argument(op, argument_node.getText(), argument_type, field);
	  }
	else
	  {
	    argument = null;
	  }

        /*
         * If we don't know the objectbase, then we have to think. We can't
         * check and see if the field we're querying is a scalar or a vector
         * field. Thus, we'll have to trust that the user knows what he wants.
         * If he's wrong about the vector/scalar type of the field he's
         * querying, then the query engine will try its best to figure out what
         * the user really meant.
         */
         
	if (base == null)
          {
            if (op_vector_mapping.containsKey(op))
              {
                Byte opI = (Byte) op_vector_mapping.get(op);
                vector_operator = opI.byteValue();
                scalar_operator = QueryDataNode.NONE;
              }
            else if (op_scalar_mapping.containsKey(op))
              {
                Byte opI = (Byte) op_scalar_mapping.get(op);
                vector_operator = QueryDataNode.NONE;
                scalar_operator = opI.byteValue();
              }
            else
              {
              	throw new GanyParseException(op + " is not an operation that I understand.");
              }
          }
        else
          {
            if (field.isArray())
              {
                if (op_vector_mapping.containsKey(op))
                  {
                    Byte opI = (Byte) op_vector_mapping.get(op);

                    vector_operator = opI.byteValue();
                    scalar_operator = QueryDataNode.NONE;
                  }
                else if (op_scalar_mapping.containsKey(op))
                  {
                    Byte opI = (Byte) op_scalar_mapping.get(op);
                    scalar_operator = opI.byteValue();
                    vector_operator = QueryDataNode.CONTAINS;
                  }
                else
                  {
                    throw new GanyParseException(op
                        + " is not an operation that I understand.");
                  }
              }
            else
              {
                if (op_scalar_mapping.containsKey(op))
                  {
                    Byte opI = (Byte) op_scalar_mapping.get(op);
                    scalar_operator = opI.byteValue();
                    vector_operator = QueryDataNode.NONE;
                  }
              }
          }
	
	if (base != null && scalar_operator != QueryDataNode.NONE && !valid_op(op, field_type))
	  {
	    throw new GanyParseException("The " + op + " operation is not valid on a " +
				       base.getName() + "'s " + field_name + " field, which is of type " +
				       field.getTypeDesc());
	  }

	return new QueryDataNode(field_name, scalar_operator, vector_operator, argument);

      default:

	throw new GanyParseException("I couldn't process node type: " + root_type);
      }
  }

  private boolean valid_op(String op, int field_type)
  {
    String[] ops = (String[]) validity_mapping.get(new Integer(field_type));

    if (ops == null)		// should only happen if field_type is invalid
      {
	return false;
      }

    for (int i = 0; i < ops.length; i++)
      {
	if (ops[i].equals(op))
	  {
	    return true;
	  }
      }

    return false;
  }

  private Object parse_argument(String operator, String argument, int argument_type, DBObjectBaseField field) throws GanyParseException
  {
    if (field == null)
      {
	switch (argument_type)
	  {
	  case QueryParserTokenTypes.INT_VALUE:
	    return Integer.valueOf(argument);

	  case QueryParserTokenTypes.DECIMAL_VALUE:
	    return Double.valueOf(argument);

	  case QueryParserTokenTypes.STRING_VALUE:
	    return StringUtils.dequote(argument);

	  default:
	    throw new GanyParseException("Unrecognized argument type parsing argument " + argument);
	  }
      }

    if (field.isArray() && argument_type == QueryParserTokenTypes.INT_VALUE &&
	op_vector_mapping.containsKey(operator))
      {
	return Integer.valueOf(argument);
      } 

    int field_type = field.getType();

    if (field_type == FieldType.NUMERIC && argument_type == QueryParserTokenTypes.INT_VALUE)
      {
	return new Integer(argument);
      }
    else if (field_type == FieldType.FLOAT && argument_type == QueryParserTokenTypes.DECIMAL_VALUE)
      {
	return new Double(argument);
      }
    else if (argument_type == QueryParserTokenTypes.BOOLEAN_VALUE)
      {
	if (argument.toLowerCase().equals("true"))
	  {
	    return Boolean.TRUE;
	  }
	else
	  {
	    return Boolean.FALSE;
	  }
      }
    else if (argument_type == QueryParserTokenTypes.STRING_VALUE)
      {
	switch (field_type)
	  {
	  case FieldType.STRING:
	  case FieldType.INVID:
	  case FieldType.IP:
	    return StringUtils.dequote(argument);
	  case FieldType.DATE:
	    DateFormat format = DateFormat.getInstance();
	    try
	      {
		return format.parse(StringUtils.dequote(argument));
	      }
	    catch (ParseException ex)
	      {
		throw new GanyParseException("I couldn't parse " + argument + " as a date value.");
	      }
	  }
      }

    throw new GanyParseException("The field " + field.getName() + " takes arguments of type " + field.getTypeDesc());
  }
}
