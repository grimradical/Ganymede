/*

   DBQueryHandler.java

   This is the query processing engine for the Directory Droid database.
   
   Created: 10 July 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
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

package arlut.csd.ddroid.server;

import java.util.Date;
import java.util.Vector;

import arlut.csd.ddroid.common.Invid;
import arlut.csd.ddroid.common.Query;
import arlut.csd.ddroid.common.QueryAndNode;
import arlut.csd.ddroid.common.QueryDataNode;
import arlut.csd.ddroid.common.QueryNode;
import arlut.csd.ddroid.common.QueryNotNode;
import arlut.csd.ddroid.common.QueryOrNode;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  DBQueryHandler

------------------------------------------------------------------------------*/

/**
 * <P>This class is the query processing engine for the Directory Droid database.  Static
 * methods in this class are used to test a query tree against an individual object
 * in the database.</P>
 *
 * <P>The {@link arlut.csd.ddroid.server.GanymedeSession GanymedeSession}'s
 * {@link arlut.csd.ddroid.server.GanymedeSession#queryDispatch(arlut.csd.ddroid.common.Query,
 * boolean, boolean, arlut.csd.ddroid.server.DBLock, arlut.csd.ddroid.server.DBEditObject)
 * queryDispatch()}
 * method contains most of the query engine's
 * logic (including namespace-indexed query optimization).  This class is just
 * responsible for applying a recursive QueryNode tree to a particular object.</P>
 *
 * @see QueryNode
 * @see Query
 *
 * @version $Id$
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public class DBQueryHandler {

  final static boolean debug = false;

  /**
   *
   * This method compares an object with a submitted query, returning true if
   * the object matches the query.
   *
   * @param session The GanymedeSession performing the query.  This parameter is
   * used to access the database to find the object's label if the query contains
   * any clauses matching against the object's label.
   * @param q The Query being compared to this object.
   * @param obj The object being tested.
   *
   */

  public static final boolean matches(GanymedeSession session, Query q, DBObject obj)
  {
    if ((q == null) || (obj == null))
      {
	throw new NullPointerException("null param to DBQueryHandler");
      }

    if (q.root == null)
      {
	return true;		// need to add check for editability here
      }
    else
      {
	return nodeMatch(session, q.root, obj);
      }
  }

  /**
   *
   * Recursive static method to compare a Query tree against an object in the database.
   *
   * @param session The GanymedeSession performing the query.  This parameter is
   * used to access the database to find the object's label if the query contains
   * any clauses matching against the object's label.
   * @param qN The QueryNode being compared to this object.
   * @param obj The object being tested.
   *
   */

  public static final boolean nodeMatch(GanymedeSession session, QueryNode qN, DBObject obj)
  {
    Object value = null;
    Vector values = null;
    int intval;
    
    /* -- */

    try				// wheee, sloppy
      {
	if (qN == null)
	  {
	    return false;
	  }

	if (qN instanceof QueryNotNode)
	  {
	    return (!nodeMatch(session, ((QueryNotNode)qN).child, obj));
	  }

	if (qN instanceof QueryAndNode)
	  {
	    return (nodeMatch(session, ((QueryAndNode)qN).child1, obj) && 
		    nodeMatch(session, ((QueryAndNode)qN).child2, obj));
	  }

	if (qN instanceof QueryOrNode)
	  {
	    return (nodeMatch(session, ((QueryOrNode)qN).child1, obj) ||
		    nodeMatch(session, ((QueryOrNode)qN).child2, obj));
	  }

	if (qN instanceof QueryDataNode)
	  {
	    DBField field = null;
	    QueryDataNode n;

	    /* - */

	    n = (QueryDataNode) qN;

	    if (n.fieldId == -2)
	      {
		// compare the Invid

		if (n.comparator == n.EQUALS)
		  {
		    return obj.getInvid().equals(n.value);
		  }
		else
		  {
		    return false;
		  }
	      }

	    if (n.fieldname != null)
	      {
		field = (DBField) obj.getField(n.fieldname);

		if ((field != null) && (field.isDefined()))
		  {
		    if (field.isVector())
		      {
			values = field.getValues();
		      }
		    else
		      {
			value = field.getValue();
		      }
		  }
		else
		  {
		    return false;
		  }
	      }
	    else if (n.fieldId == -1)
	      {
		value = obj.getLabel();

		if (debug)
		  {
		    System.err.println("Doing comparison against object label: " + value);
		  }
	      }
	    else
	      {
		field = (DBField) obj.getField(n.fieldId);

		if ((field != null) && (field.isDefined()))
		  {
		    if (field.isVector())
		      {
			values = field.getValues();
		      }
		    else
		      {
			value = field.getValue();
		      }
		  }
		else
		  {
		    return false;
		  }
	      }

	    // if we've gotten this far, the field is defined in the
	    // field dictionary, but we want to check to see if it is
	    // null-valued.

	    if (n.comparator == n.DEFINED)
	      {
		return (field != null) && field.isDefined();
	      }

	    // ok.. check to see if we are checking against the length of an
	    // array field.

	    switch (n.arrayOp)
	      {
	      case QueryDataNode.LENGTHEQ:

		if (!field.isVector())
		  {
		    throw new RuntimeException("Can't do an array test on a scalar field.");
		  }

		intval = ((Integer) n.value).intValue();

		if (debug)
		  {
		    System.err.println("Comparing vector field size: " + intval + " == " + values.size() + "?");
		  }

		return (intval == values.size());

	      case QueryDataNode.LENGTHGR:

		if (!field.isVector())
		  {
		    throw new RuntimeException("Can't do an array test on a scalar field.");
		  }

		intval = ((Integer) n.value).intValue();

		if (debug)
		  {
		    System.err.println("Comparing vector field size: " + values.size() + " > " + intval + "?");
		  }

		return (values.size() > intval);

	      case QueryDataNode.LENGTHLE:

		if (!field.isVector())
		  {
		    throw new RuntimeException("Can't do an array test on a scalar field.");
		  }

		intval = ((Integer) n.value).intValue();

		if (debug)
		  {
		    System.err.println("Comparing vector field size: " + values.size() + " < " + intval + "?");
		  }

		return (values.size() > intval);
	      }

	    // okay.  Now we check each field type

	    if (n.value instanceof String &&
		(((value != null) && value instanceof String) ||
		((values != null) && (values.size() > 0) && 
		 (values.elementAt(0) instanceof String)))) // assume type consistent array
	      {
		// Compare a string value or regexp against a string field

		if (n.arrayOp == QueryDataNode.NONE)
		  {
		    return compareStrings(n, (String) n.value, (String) value);
		  }
		else
		  {
		    return compareStringArray(n, (String) n.value, values);
		  }
	      }

	    // the client may pass us real Invid's.

	    if (n.value instanceof Invid &&
		(((value != null) && value instanceof Invid) ||
		((values != null) && (values.size() > 0) && (values.elementAt(0) instanceof Invid))))
	      {
		Invid
		  i1 = null,
		  i2 = null;

		/* -- */

		if (debug)
		  {
		    System.err.println("Doing a real invid compare");
		  }

		if (n.comparator == QueryDataNode.EQUALS)
		  {
		    i1 = (Invid) n.value;

		    if (i1 == null)
		      {
			return false;
		      }

		    if (n.arrayOp == QueryDataNode.NONE)
		      {
			if (debug)
			  {
			    System.err.println("Doing a scalar invid compare against value " + i1);
			  }

			i2 = (Invid) value;

			return i1.equals(i2);
		      }
		    else
		      {
			switch (n.arrayOp)
			  {
			  case QueryDataNode.CONTAINS:

			    if (debug)
			      {
				System.err.println("Doing a vector invid compare against value " + i1);
			      }

			    for (int i = 0; i < values.size(); i++)
			      {
				i2 = (Invid) values.elementAt(i);

				if (i1.equals(i2))
				  {
				    return true;
				  }
			      }

			    return false;

			  default:
			    return false;
			  }
		      }
		  }
		else
		  {
		    if (debug)
		      {
			System.err.println("*** INVALID invid compare against value " + i1);
		      }

		    return false;	// invalid comparator
		  }
	      }

	    // The client can pass us a String for invid comparisons.. we need to 
	    // turn Invid's in the field we're looking at to Strings for the
	    // compare.

	    if (n.value instanceof String &&
		(((value != null) && value instanceof Invid) ||
		((values != null) && (values.size() > 0) && (values.elementAt(0) instanceof Invid))))
	      {
		String
		  s1 = null,
		  s2 = null;

		/* -- */

		if (debug)
		  {
		    System.err.println("Doing a string/invid compare");
		  }

		if (n.arrayOp == QueryDataNode.NONE)
		  {
		    s1 = (String) n.value;
		    s2 = (String) session.viewObjectLabel((Invid)value);
		    
		    return compareString(n, s1, s2);
		  }
		else if (n.arrayOp == QueryDataNode.CONTAINS)
		  {
		    s1 = (String) n.value;
		    
		    /* -- */
		    
		    if (debug)
		      {
			System.err.println("Doing a vector string/invid compare against value/regexp " + s1);
		      }
		    
		    for (int i = 0; i < values.size(); i++)
		      {
			s2 = session.viewObjectLabel((Invid) values.elementAt(i));
			
			if (compareString(n, s1, s2))
			  {
			    return true;
			  }
		      }
		  }
		else
		  {
		    return false;	// invalid comparator
		  }
	      }

	    // The client can pass us a String for IP field
	    // comparisons.. we need to turn IP values in the field
	    // we're looking at to Strings for the compare.

	    if (n.value instanceof String &&
		(((value != null) && value instanceof Byte[]) ||
		((values != null) && (values.size() > 0) && (values.elementAt(0) instanceof Byte[]))))
	      {
		String
		  s1 = null,
		  s2 = null;

		/* -- */

		if (debug)
		  {
		    System.err.println("Doing a string/IP address compare");
		  }

		if (n.arrayOp == QueryDataNode.NONE)
		  {
		    s1 = (String) n.value;
		    Byte[] ipBytes = (Byte[]) value;
		   
		    if (ipBytes.length == 4)
		      {
			s2 = IPDBField.genIPV4string(ipBytes);
		      }
		    else if (ipBytes.length == 16)
		      {
			s2 = IPDBField.genIPV6string(ipBytes);
		      }
		    
		    if (debug)
		      {
			System.err.println("Comparison: " + n + ", string check = " + s2);
		      }

		    return compareString(n, s1, s2);
		  }
		else if (n.arrayOp == QueryDataNode.CONTAINS)
		  {
		    s1 = (String) n.value;
		    
		    /* -- */
		    
		    if (debug)
		      {
			System.err.println("Doing a vector string/ip address compare against value/regexp " + s1);
		      }
		    
		    for (int i = 0; i < values.size(); i++)
		      {
			Byte[] ipBytes = (Byte[]) values.elementAt(i)
;
			if (ipBytes.length == 4)
			  {
			    s2 = IPDBField.genIPV4string(ipBytes);
			  }
			else if (ipBytes.length == 16)
			  {
			    s2 = IPDBField.genIPV6string(ipBytes);
			  }
			
			if (compareString(n, s1, s2))
			  {
			    return true;
			  }
		      }
		  }
		else
		  {
		    return false;	// invalid comparator
		  }
	      }

	    // i.p. address can be arrays.. note that the client's
	    // query box will pass us a true array of Bytes for
	    // equality, starts with, or ends with tests.  if the user
	    // is attempting a regexp match, the parameter will be a
	    // String.

	    if (n.value instanceof Byte[])
	      {
		Byte[] fBytes;
		Byte[] oBytes;

		/* -- */

		// scalar compare?

		if (n.arrayOp == QueryDataNode.NONE)
		  {
		    fBytes = (Byte[]) value;
		    oBytes = (Byte[]) n.value;
		    
		    if (n.comparator == QueryDataNode.EQUALS)
		      {
			return compareIPs(fBytes, oBytes);
		      }
		    else if (n.comparator == QueryDataNode.STARTSWITH)
		      {
			return ipBeginsWith(fBytes, oBytes);
		      }
		    else if (n.comparator == QueryDataNode.ENDSWITH)
		      {
			return ipEndsWith(fBytes, oBytes);
		      }
		    else
		      {
			return false;	// invalid comparator
		      }
		  }
		else // vector compare.. EQUALS only
		  {
		    if (n.comparator == n.EQUALS)
		      {
			oBytes = (Byte[]) n.value;
			
			switch (n.arrayOp)
			  {
			  case QueryDataNode.CONTAINS:
			    
			    for (int i = 0; i < values.size(); i++)
			      {
				if (compareIPs(oBytes, ((Byte[]) values.elementAt(i))))
				  {
				    return true;
				  }
			      }
			    
			    return false;
			    
			  default:
			    
			    return false;
			  }
		      }
		    else
		      {
			return false;
		      }
		  }
	      }

	    // ---------------------------------------- End possible array fields --------------------

	    if (n.arrayOp != QueryDataNode.NONE)
	      {
		return false;
	      }

	    // ---------------------------------------- Check remaining scalar types --------------------

	    // booleans can't be arrays

	    if (value instanceof Boolean)
	      {
		if (n.comparator == QueryDataNode.EQUALS)
		  {
		    return ((Boolean) value).equals(n.value);
		  }
		else
		  {
		    return false;	// invalid comparator
		  }
	      }

	    // dates can't be arrays

	    if (value instanceof Date)
	      {
		long time1, time2;

		/* -- */

		time1 = ((Date) value).getTime();
		time2 = ((Date) n.value).getTime();

		if (n.comparator == QueryDataNode.EQUALS)
		  {
		    return (time1 == time2);
		  }
		else if (n.comparator == QueryDataNode.LESS)
		  {
		    return (time1 < time2);
		  }
		else if (n.comparator == QueryDataNode.LESSEQ)
		  {
		    return (time1 <= time2);
		  }
		else if (n.comparator == QueryDataNode.GREAT)
		  {
		    return (time1 > time2);
		  }
		else if (n.comparator == QueryDataNode.GREATEQ)
		  {
		    return (time1 >= time2);
		  }
		else
		  {
		    return false;	// invalid comparator
		  }
	      }

	    // integers can't be arrays

	    if (value instanceof Integer)
	      {
		int val1, val2;

		/* -- */

		val1 = ((Integer) value).intValue();
		val2 = ((Integer) n.value).intValue();

		if (n.comparator == QueryDataNode.EQUALS)
		  {
		    return (val1 == val2);
		  }
		else if (n.comparator == QueryDataNode.LESS)
		  {
		    return (val1 < val2);
		  }
		else if (n.comparator == QueryDataNode.LESSEQ)
		  {
		    return (val1 <= val2);
		  }
		else if (n.comparator == QueryDataNode.GREAT)
		  {
		    return (val1 > val2);
		  }
		else if (n.comparator == QueryDataNode.GREATEQ)
		  {
		    return (val1 >= val2);
		  }
		else
		  {
		    return false;	// invalid comparator
		  }
	      }
	  }

	return false;		// wtf?
      }
    catch (ClassCastException ex)
      {
	return false;
      }
  }

  private static boolean compareStrings(QueryDataNode n, String queryValue, String value)
  {
    return compareString(n, queryValue, value);
  }

  private static boolean compareStringArray(QueryDataNode n, String queryValue, Vector values)
  {
    if (values == null)
      {
	return false;
      }

    switch (n.arrayOp)
      {
      case QueryDataNode.CONTAINS:
	
	for (int i = 0; i < values.size(); i++)
	  {
	    if (compareString(n, queryValue, (String) values.elementAt(i)))
	      {
		return true;
	      }
	  }

	return false;

      default:
	
	return false;
      }
  }

  /**
   *
   * string1 is the query value provided by the client, string2 is the 
   * value we are testing.
   *
   */

  private static boolean compareString(QueryDataNode n, String string1, String string2)
  {
    int result;

    /* -- */

    if (string1 == null || string2 == null)
      {
	if (debug)
	  {
	    System.err.println("null param for compareString");
	  }

	return false;
      }

    switch (n.comparator)
      {
      case QueryDataNode.MATCHES:

	if (n.regularExpression == null)
	  {
	    if (debug)
	      {
		System.err.println("DBQueryHandler: trying to build regexp: /" + n.value + "/");
	      }

	    if (n.value == null || n.value.toString().equals(""))
	      {
		return false;
	      }
	    
	    try
	      {
		n.regularExpression = new gnu.regexp.RE(n.value);
	      }
	    catch (gnu.regexp.REException ex)
	      {
		System.err.println("DBQueryHandler: REException construction regexp: " + ex.getMessage());
		
		return false;
	      }
	    
	    if (debug)
	      {
		System.err.println("DBQueryHandler: regexp built successfully: " + n.regularExpression);
	      }
	  }
	
	gnu.regexp.RE regexp = (gnu.regexp.RE) n.regularExpression;
	
	if (debug)
	  {
	    System.err.println("DBQueryHandler: Trying to match regexp against " + string2);
	  }
	
	gnu.regexp.REMatch match = regexp.getMatch(string2);
	
	if (debug)
	  {
	    if (match == null)
	      {
		System.err.println("DBQueryHandler: Failed match regexp against " + string2);
	      }
	    else
	      {
		System.err.println("DBQueryHandler: Found match regexp against " + string2);
	      }
	  }
	
	return (match != null);

      case QueryDataNode.NOCASEMATCHES:

	if (n.regularExpression == null)
	  {
	    if (debug)
	      {
		System.err.println("DBQueryHandler: trying to build case-insensitive regexp: /" + n.value + "/");
	      }

	    if (n.value == null || n.value.toString().equals(""))
	      {
		return false;
	      }
	    
	    try
	      {
		n.regularExpression = new gnu.regexp.RE(n.value, gnu.regexp.RE.REG_ICASE);
	      }
	    catch (gnu.regexp.REException ex)
	      {
		System.err.println("DBQueryHandler: REException construction regexp: " + ex.getMessage());
		
		return false;
	      }
	    
	    if (debug)
	      {
		System.err.println("DBQueryHandler: case insensitive regexp built successfully: " + n.regularExpression);
	      }
	  }
	
	gnu.regexp.RE nocaseregexp = (gnu.regexp.RE) n.regularExpression;
	
	if (debug)
	  {
	    System.err.println("DBQueryHandler: Trying to match case insensitive regexp against " + string2);
	  }
	
	gnu.regexp.REMatch nocasematch = nocaseregexp.getMatch(string2);
	
	if (debug)
	  {
	    if (nocasematch == null)
	      {
		System.err.println("DBQueryHandler: Failed case insensitive match regexp against " + string2);
	      }
	    else
	      {
		System.err.println("DBQueryHandler: Found case insensitive match regexp against " + string2);
	      }
	  }
	
	return (nocasematch != null);

      case QueryDataNode.EQUALS:

	if (debug)
	  {
	    System.err.println("EQUALS compare, " + string1 + ", " + string2);
	  }

	return string1.equals(string2);

      case QueryDataNode.NOCASEEQ:

	if (debug)
	  {
	    System.err.println("NOCASEEQ compare, " + string1 + ", " + string2);
	  }

	return string1.equalsIgnoreCase(string2);

      case QueryDataNode.STARTSWITH:

	if (debug)
	  {
	    System.err.println("STARTSWITH compare, " + string1 + ", " + string2);
	  }

	return string2.startsWith(string1);

      case QueryDataNode.ENDSWITH:

	if (debug)
	  {
	    System.err.println("ENDSWITH compare, " + string1 + ", " + string2);
	  }

	return string2.endsWith(string1);

      case QueryDataNode.LESS:

	if (debug)
	  {
	    System.err.println("LESS compare, " + string1 + ", " + string2);
	  }

	result = string2.compareTo(string1);
	return result < 0;

      case QueryDataNode.LESSEQ:

	if (debug)
	  {
	    System.err.println("LESSEQ compare, " + string1 + ", " + string2);
	  }

	result = string2.compareTo(string1);
	return result <= 0;

      case QueryDataNode.GREAT:

	if (debug)
	  {
	    System.err.println("GREAT compare, " + string1 + ", " + string2);
	  }

	result = string2.compareTo(string1);
	return result > 0;

      case QueryDataNode.GREATEQ:

	if (debug)
	  {
	    System.err.println("GREATEQ compare, " + string1 + ", " + string2);
	  }

	result = string2.compareTo(string1);
	return result >= 0;
      }

    return false;
  }

  // helpers

  /**
   *
   * IP address values are encoded as byte arrays in the Directory Droid server.. this 
   * method is used to compare two IP address values for equality.
   *
   */

  private static boolean compareIPs(Byte[] param1, Byte[] param2)
  {
    if (param1.length != param2.length)
      {
	return false;
      }
    else
      {
	for (int i = 0; i < param1.length; i++)
	  {
	    if (param1[i].byteValue() != param2[i].byteValue())
	      {
		return false;
	      }
	  }
	
	return true;
      }
  }

  /**
   *
   * IP address values are encoded as byte arrays in the Directory Droid server.. this 
   * method is used to compare two IP address values for a prefix relationship.
   *
   * @return Returns true if param1 begins with param2.
   *
   */

  private static boolean ipBeginsWith(Byte[] param1, Byte[] param2)
  {
    Byte[] prefix = ipAddrNoPad(param2);

    /* -- */

    if (prefix.length > param1.length)
      {
	return false;
      }
    else
      {
	for (int i = 0; i < prefix.length; i++)
	  {
	    if (prefix[i].byteValue() != param1[i].byteValue())
	      {
		return false;
	      }
	  }
	
	return true;
      }
  }

  /**
   *
   * IP address values are encoded as byte arrays in the Directory Droid server.. this 
   * method is used to compare two IP address values for a suffix relationship.
   *   
   * @return Returns true if param1 ends with param2.
   *
   */

  private static boolean ipEndsWith(Byte[] param1, Byte[] param2)
  {
    Byte[] suffix = ipAddrNoPad(param2);

    /* -- */

    if (suffix.length > param1.length)
      {
	return false;
      }
    else
      {
	for (int i = (param1.length - 1), j = (suffix.length - 1); 
	     j >= 0;
	     i--, j--)
	  {
	    if (suffix[j].byteValue() != param1[i].byteValue())
	      {
		return false;
	      }
	  }
	
	return true;
      }
  }

  /**
   * <P>This helper method extracts the leading octets from the supplied
   * IP address that are not all zeros.  I.e., for the address
   * 129.0.116.0, ipAddrNoPad() would return 129.0.116, where
   * for the address 129.116.0.0, ipAddrNoPad() would return
   * 129.116.</P>
   *
   * <P>Note that, like all Directory Droid code dealing with IP addresses,
   * Directory Droid is using the u2s() and s2u() methods here to handle
   * encoded unsigned values in the Java signed byte/Byte type/object.</P>
   */

  private static Byte[] ipAddrNoPad(Byte[] ipaddr)
  {
    int i = ipaddr.length;

    for (; i > 0 &&
	   (s2u(ipaddr[i-1].byteValue()) == 0); i--);

    Byte[] result = new Byte[i];

    for (i = 0; i < result.length; i++)
      {
	result[i] = ipaddr[i];
      }

    return result;
  }

  /**
   * <P>This method maps an int value between 0 and 255 inclusive
   * to a legal signed byte value.</P>
   */

  private final static byte u2s(int x)
  {
    if ((x < 0) || (x > 255))
      {
	throw new IllegalArgumentException("Out of range: " + x);
      }

    return (byte) (x - 128);
  }

  /**
   * <P>This method maps a u2s-encoded signed byte value to an
   * int value between 0 and 255 inclusive.</P>
   */

  private final static short s2u(byte b)
  {
    return (short) (b + 128);
  }

}
