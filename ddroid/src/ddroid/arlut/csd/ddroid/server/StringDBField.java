/*
   GASH 2

   StringDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
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

import arlut.csd.ddroid.common.*;
import arlut.csd.ddroid.rmi.*;

import java.io.*;
import java.util.*;
import java.rmi.*;
import gnu.regexp.*;

import com.jclark.xml.output.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   StringDBField

------------------------------------------------------------------------------*/

/**
 * <P>StringDBField is a subclass of DBField for the storage and handling of string
 * fields in the {@link arlut.csd.ddroid.server.DBStore DBStore} on the Ganymede
 * server.</P>
 *
 * <P>The Directory Droid client talks to StringDBFields through the
 * {@link arlut.csd.ddroid.rmi.string_field string_field} RMI interface.</P> 
 */

public class StringDBField extends DBField implements string_field {

  /**
   * <P>Receive constructor.  Used to create a StringDBField from a
   * {@link arlut.csd.ddroid.server.DBStore DBStore}/{@link arlut.csd.ddroid.server.DBJournal DBJournal}
   * DataInput stream.</P>
   */

  StringDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = null;
    this.owner = owner;
    this.fieldcode = definition.getID();
    receive(in, definition);
  }

  /**
   * <P>No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the 
   * {@link arlut.csd.ddroid.server.DBObjectBase DBObjectBase}
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the 
   * {@link arlut.csd.ddroid.server.DBStore DBStore}.</P>
   *
   * <P>Used to provide the client a template for 'creating' this
   * field if so desired.</P>
   */

  StringDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.fieldcode = definition.getID();
    
    if (isVector())
      {
	value = new Vector();
      }
    else
      {
	value = null;
      }
  }

  /**
   *
   * Copy constructor.
   *
   */

  public StringDBField(DBObject owner, StringDBField field)
  {
    this.owner = owner;
    this.fieldcode = field.getID();
    
    if (isVector())
      {
	value = field.getVectVal().clone();
      }
    else
      {
	value = field.value;
      }
  }

  /**
   *
   * Scalar value constructor.
   *
   */

  public StringDBField(DBObject owner, String value, DBObjectBaseField definition)
  {
    if (definition.isArray())
      {
	throw new IllegalArgumentException("scalar constructor called on vector field");
      }

    this.owner = owner;
    this.fieldcode = definition.getID();
    this.value = value;
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public StringDBField(DBObject owner, Vector values, DBObjectBaseField definition)
  {
    if (!definition.isArray())
      {
	throw new IllegalArgumentException("vector constructor called on scalar field");
      }

    this.owner = owner;
    this.fieldcode = definition.getID();

    if (values == null)
      {
	value = new Vector();
      }
    else
      {
	value = values.clone();
      }
  }

  /**
   * <p>This method is used to return a copy of this field, with the field's owner
   * set to newOwner.</p>
   */

  public DBField getCopy(DBObject newOwner)
  {
    return new StringDBField(newOwner, this);
  }

  public Object clone()
  {
    return new StringDBField(owner, this);
  }

  void emit(DataOutput out) throws IOException
  {
    if (isVector())
      {
	Vector values = getVectVal();

	int count = 0;

	for (int i = 0; i < values.size(); i++)
	  {
	    if (!values.elementAt(i).equals(""))
	      {
		count++;
	      }
	  }

	out.writeInt(count);

	for (int i = 0; i < values.size(); i++)
	  {
	    if (!values.elementAt(i).equals(""))
	      {
		out.writeUTF((String) values.elementAt(i));
	      }
	  }
      }
    else
      {
	out.writeUTF((String)value);
      }
  }

  void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    int count;

    /* -- */

    if (definition.isArray())
      {
	if (Ganymede.db.isLessThan(2,3))
	  {
	    count = in.readShort();
	  }
	else
	  {
	    count = in.readInt();
	  }

	value = new Vector(count);

	Vector values = (Vector) value;

	for (int i = 0; i < count; i++)
	  {
	    values.addElement(in.readUTF().intern());
	  }
      }
    else
      {
	value = in.readUTF().intern();
      }
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().</p>
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.startElementIndent(this.getXMLName());

    if (!isVector())
      {
	xmlOut.write(value());	// for scalar fields, just write the string in place
      }
    else
      {
	Vector values = getVectVal();

	for (int i = 0; i < values.size(); i++)
	  {
	    xmlOut.indentOut();
	    xmlOut.indent();
	    xmlOut.indentIn();
	    emitStringXML(xmlOut, (String) values.elementAt(i));
	  }

	xmlOut.indent();
      }

    xmlOut.endElement(this.getXMLName());
  }

  public void emitStringXML(XMLDumpContext xmlOut, String value) throws IOException
  {
    xmlOut.startElement("string");
    xmlOut.attribute("val", value);
    xmlOut.endElement("string");
  }

  // ****
  //
  // type specific value accessors
  //
  // ****

  public String value()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    return (String) value;
  }

  public String value(int index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar");
      }

    return (String) getVectVal().elementAt(index);
  }

  /**
   * <P>This method returns a text encoded value for this StringDBField
   * without checking permissions.</P>
   *
   * <P>This method avoids checking permissions because it is used on
   * the server side only and because it is involved in the 
   * {@link arlut.csd.ddroid.server.DBObject#getLabel() getLabel()}
   * logic for {@link arlut.csd.ddroid.server.DBObject DBObject}, 
   * which is invoked from {@link arlut.csd.ddroid.server.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ddroid.server.GanymedeSession#getPerm(arlut.csd.ddroid.server.DBObject) getPerm()} 
   * method.</P>
   *
   * <P>If this method checked permissions and the getPerm() method
   * failed for some reason and tried to report the failure using
   * object.getLabel(), as it does at present, the server could get
   * into an infinite loop.</P>
   */

  public synchronized String getValueString()
  {
    if (!isVector())
      {
	if (value == null)
	  {
	    return "null";
	  }

	return this.value();
      }

    int size = size();
    
    if (size == 0)
      {
	return "";
      }
    
    String entries[] = new String[size];
    
    for (int i = 0; i < size; i++)
      {
	entries[i] = this.value(i);
      }
    
    new arlut.csd.Util.QuickSort(entries,
				 new arlut.csd.Util.Compare()
				 {
				   public int compare(Object a, Object b)
				     {
				       String aS, bS;
				       
				       aS = (String) a;
				       bS = (String) b;
				       
				       return aS.compareTo(bS);
				     }
				 }
				 ).sort();
    
    StringBuffer result = new StringBuffer();
    
    for (int i = 0; i < entries.length; i++)
      {
	if (i > 0)
	  {
	    result.append(",");
	  }
	
	result.append(entries[i]);
      }
    
    return result.toString();
  }

  /**
   * <P>For strings, we don't care about having a reversible encoding,
   * because we can sort and select normally based on the getValueString()
   * result.</P>
   */

  public String getEncodingString()
  {
    return getValueString();
  }

  /**
   * <P>Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.</P>
   *
   * <P>If there is no change in the field, null will be returned.</P>
   */

  public synchronized String getDiffString(DBField orig)
  {
    StringBuffer result = new StringBuffer();
    StringDBField origS;

    /* -- */

    if (!(orig instanceof StringDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origS = (StringDBField) orig;

    if (isVector())
      {
	Vector 
	  added = new Vector(),
	  deleted = new Vector();

	Vector values = getVectVal();
	Vector origValues = origS.getVectVal();

	Enumeration en;

	String elementA, elementB;

	boolean found = false;

	/* -- */

	// find elements in the orig field that aren't in our present field

	en = origValues.elements();

	while (en.hasMoreElements())
	  {
	    elementA = (String) en.nextElement();

	    found = false;

	    for (int i = 0; !found && i < values.size(); i++)
	      {
		elementB = (String) values.elementAt(i);

		if (elementA.equals(elementB))
		  {
		    found = true;
		  }
	      }

	    if (!found)
	      {
		deleted.addElement(elementA);
	      }
	  }

	// find elements in present our field that aren't in the orig field

	en = values.elements();

	while (en.hasMoreElements())
	  {
	    elementA = (String) en.nextElement();

	    found = false;

	    for (int i = 0; !found && i < origValues.size(); i++)
	      {
		elementB = (String) origValues.elementAt(i);

		if (elementA.equals(elementB))
		  {
		    found = true;
		  }
	      }

	    if (!found)
	      {
		added.addElement(elementA);
	      }
	  }

	// were there any changes at all?

	if (deleted.size() == 0 && added.size() == 0)
	  {
	    return null;
	  }
	else
	  {
	    if (deleted.size() != 0)
	      {
		result.append("\tDeleted: ");
	    
		for (int i = 0; i < deleted.size(); i++)
		  {
		    if (i > 0)
		      {
			result.append(", ");
		      }

		    result.append((String) deleted.elementAt(i));
		  }

		result.append("\n");
	      }

	    if (added.size() != 0)
	      {
		result.append("\tAdded: ");
	    
		for (int i = 0; i < added.size(); i++)
		  {
		    if (i > 0)
		      {
			result.append(", ");
		      }

		    result.append((String) added.elementAt(i));
		  }

		result.append("\n");
	      }

	    return result.toString();
	  }
      }
    else
      {
	if (origS.value().equals(this.value()))
	  {
	    return null;
	  }
	else
	  {
	    result.append("\tOld: ");
	    result.append(origS.value());
	    result.append("\n\tNew: ");
	    result.append(this.value());
	    result.append("\n");
	
	    return result.toString();
	  }
      }
  }

  /**
   * <P>Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.</P>
   *
   * @see arlut.csd.ddroid.rmi.db_field
   */

  public synchronized boolean isDefined()
  {
    if (isVector())
      {
	if (value != null && getVectVal().size() > 0)
	  {
	    return true;
	  }
	else
	  {
	    return false;
	  }
      }
    else
      {
	if (value != null && !((String) value).equals(""))
	  {
	    return true;
	  }
	else
	  {
	    return false;
	  }
      }
  }

  // ****
  //
  // string_field methods 
  //
  // ****

  /**
   *
   * Returns the maximum acceptable string length
   * for this field.
   *
   * @see arlut.csd.ddroid.rmi.string_field
   *
   */

  public int maxSize()
  {
    return getFieldDef().getMaxLength();
  }

  /**
   *
   * Returns the minimum acceptable string length
   * for this field.
   *
   * @see arlut.csd.ddroid.rmi.string_field
   *
   */

  public int minSize()
  {
    return getFieldDef().getMinLength();
  }

  /**
   *
   * Returns true if the client should echo characters
   * entered into the string field.
   *
   * @see arlut.csd.ddroid.rmi.string_field
   *
   */
  
  public boolean showEcho()
  {
    return true;
  }

  /**
   *
   * Returns true if this field has a list of recommended
   * options for choices from the choices() method.
   *
   * @see arlut.csd.ddroid.rmi.string_field
   *
   */

  public boolean canChoose() throws NotLoggedInException
  {
    if (owner instanceof DBEditObject)
      {
	return (((DBEditObject) owner).obtainChoiceList(this) != null);
      }
    else
      {
	return false;
      }
  }

  /**
   *
   * Returns true if the only valid values
   * for this string field are in the
   * vector returned by choices().
   *
   * @see arlut.csd.ddroid.rmi.string_field
   *
   */

  public boolean mustChoose() throws NotLoggedInException
  {
    if (!canChoose())
      {
	return false;
      }

    if (owner instanceof DBEditObject)
      {
	return ((DBEditObject) owner).mustChoose(this);
      }

    return false;
  }

  /**
   *
   * This method returns true if this invid field should not
   * show any choices that are currently selected in field
   * x, where x is another field in this db_object.
   *
   */

  public boolean excludeSelected(db_field x)
  {
    return ((DBEditObject) owner).excludeSelected(x, this);    
  }

  /**
   * <P>Returns a list of recommended and/or mandatory choices 
   * for this field.  This list is dynamically generated by
   * subclasses of {@link arlut.csd.ddroid.server.DBEditObject DBEditObject};
   * this method should not need
   * to be overridden.</P>
   *
   * @see arlut.csd.ddroid.rmi.string_field
   */

  public QueryResult choices() throws NotLoggedInException
  {
    if (!(owner instanceof DBEditObject))
      {
	throw new IllegalArgumentException("can't get choice list on non-editable object");
      }

    return ((DBEditObject) owner).obtainChoiceList(this);
  }

  /**
   * <P>This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.</P>
   *
   * <P>If there is no caching key, this method will return null.</P>
   */

  public Object choicesKey()
  {
    if (owner instanceof DBEditObject)
      {
	return ((DBEditObject) owner).obtainChoicesKey(this);
      }
    else
      {
	return null;
      }
  }

  /**
   * <P>Returns a string containing the list of acceptable characters.
   * If the string is null, it should be interpreted as meaning all
   * characters not listed in disallowedChars() are allowable by
   * default.</P>
   *
   * @see arlut.csd.ddroid.rmi.string_field
   */

  public String allowedChars()
  {
    return getFieldDef().getOKChars();
  }

  /**
   * <P>Returns a string containing the list of forbidden
   * characters for this field.  If the string is null,
   * it should be interpreted as meaning that no characters
   * are specifically disallowed.</P>
   *
   * @see arlut.csd.ddroid.rmi.string_field
   */

  public String disallowedChars()
  {
    return getFieldDef().getBadChars();
  }

  /**
   * <P>Convenience method to identify if a particular
   * character is acceptable in this field.</P>
   *
   * @see arlut.csd.ddroid.rmi.string_field
   */

  public boolean allowed(char c)
  {
    if (allowedChars() != null && (allowedChars().indexOf(c) == -1))
      {
	return false;
      }

    if (disallowedChars() != null && (disallowedChars().indexOf(c) != -1))
      {
	return false;
      }
    
    return true;
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof String));
  }

  public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj;
    String s, s2;
    QueryResult qr;
    boolean ok = true;

    /* -- */

    if (!isEditable(true))
      {
	return Ganymede.createErrorDialog("String Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	return Ganymede.createErrorDialog("String Field Error",
					  "Submitted value " + o + " is not a string!  Major client error while" +
					  " trying to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    s = (String) o;

    if (s == null)
      {
	return eObj.verifyNewValue(this, s);
      }

    if (s.length() > maxSize())
      {
	// string too long
	
	return Ganymede.createErrorDialog("String Field Error",
					  "string value " + s +
					  " is too long for field " + 
					  getName() + " in object " +
					  owner.getLabel() +
					  ", which has a length limit of " + 
					  maxSize());
      }

    if (s.length() < minSize())
      {
	// string too short
	
	return Ganymede.createErrorDialog("String Field Error",
					  "string value " + s +
					  " is too short for field " + 
					  getName() +
					  " which has a minimum length of " + 
					  minSize());
      }

    if (getFieldDef().regexp != null)
      {
	gnu.regexp.REMatch match = getFieldDef().regexp.getMatch(s);

	if (match == null)
	  {
	    String desc = getFieldDef().getRegexpDesc();

	    if (desc == null || desc.equals(""))
	      {
		return Ganymede.createErrorDialog("String Field Error",
						  "String value " + s + " " +
						  "does not conform to the regular expression pattern established " +
						  "for this string field.\n\n" +
						  "This string field only accepts strings matching the " +
						  "following regular expression:\n\n\"" +
						  getFieldDef().getRegexpPat() + "\"");
	      }
	    else
	      {
		return Ganymede.createErrorDialog("String Field Error",
						  "String value " + s + " " +
						  "does not conform to the regular expression pattern established " +
						  "for this string field.\n\n" +
						  "This string field only accepts strings matching the " +
						  "following criteria:\n\n\"" +
						  desc + "\"");
	      }
	  }
      }

    if (allowedChars() != null && !allowedChars().equals(""))
      {
	String okChars = allowedChars();
	
	for (int i = 0; i < s.length(); i++)
	  {
	    if (okChars.indexOf(s.charAt(i)) == -1)
	      {
		return Ganymede.createErrorDialog("String Field Error",
						  "string value " + s +
						  " contains a character '" + 
						  s.charAt(i) + "' which is not allowed in field " +
						  getName() + " in object " + owner.getLabel());
	      }
	  }
      }
    
    if (disallowedChars() != null && !disallowedChars().equals(""))
      {
	String badChars = disallowedChars();
	
	for (int i = 0; i < s.length(); i++)
	  {
	    if (badChars.indexOf(s.charAt(i)) != -1)
	      {
		return Ganymede.createErrorDialog("String Field Error",
						  "string value " + s +
						  " contains a character '" + 
						  s.charAt(i) + "' which is not allowed in field " +
						  getName() + " in object " + owner.getLabel());
	      }
	  }
      }

    try
      {
	if (mustChoose())
	  {
	    ok = false;
	    qr = choices();
	    
	    if (!qr.containsLabel(s))
	      {
		return Ganymede.createErrorDialog("String Field Error",
						  "string value " + s +
						  " is not a valid choice for field " +
						  getName() + " in object " + owner.getLabel());
	      }
	  }
      }
    catch (NotLoggedInException ex)
      {
	return Ganymede.createErrorDialog("Error",
					  "Not Logged In");
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, s);
  }
}