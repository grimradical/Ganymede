/*

   PermissionMatrixDBField.java

   This class defines the permission matrix field used in the
   'Admin' DBObjectBase class.
   
   Created: 27 June 1997
   Version: $Revision: 1.19 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                         PermissionMatrixDBField

------------------------------------------------------------------------------*/

public class PermissionMatrixDBField extends DBField implements perm_field {

  static final boolean debug = false;

  Hashtable matrix;

  /* -- */

  /**
   *
   * Receive constructor.  Used to create a PermissionMatrixDBField
   * from a DBStore/DBJournal DataInput stream.
   * 
   */

  PermissionMatrixDBField(DBObject owner, 
			  DataInput in,
			  DBObjectBaseField definition) throws IOException, RemoteException
  {
    super();			// initialize UnicastRemoteObject

    value = values = null;
    this.owner = owner;
    this.definition = definition;
    receive(in);
  }

  /**
   *
   * No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the DBObjectBase
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the DBStore.<br><br>
   *
   * Used to provide the client a template for 'creating' this
   * field if so desired.
   *
   */

  PermissionMatrixDBField(DBObject owner, 
			  DBObjectBaseField definition) throws RemoteException
  {
    super();			// initialize UnicastRemoteObject

    this.owner = owner;
    this.definition = definition;
    
    matrix = new Hashtable();
    defined = false;
    value = null;
    values = null;
  }

  /**
   *
   * Copy constructor.
   *
   */

  public PermissionMatrixDBField(DBObject owner, 
				 PermissionMatrixDBField field) throws RemoteException
  {
    super();			// initialize UnicastRemoteObject

    // --

    Object key;
    PermEntry entry;

    /* -- */

    value = values = null;

    if (debug)
      {
	System.err.println("PermissionMatrixDBField: Copy constructor");
      }

    this.definition = field.definition;
    this.owner = owner;
    this.matrix = new Hashtable(field.matrix.size());

    Enumeration enum = field.matrix.keys();

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();

	if (debug)
	  {
	    System.err.print("PermissionMatrixDBField: copying ");

	    if (key instanceof DBObjectBase)
	      {
		System.err.print("base " + ((DBObjectBase) key).getName());
	      }
	    else if (key instanceof DBObjectBaseField)
	      {
		System.err.print("basefield " + ((DBObjectBaseField) key).getName());
	      }
	    else
	      {
		System.err.print("unrecognized key");
	      }
	  }

	entry = new PermEntry((PermEntry) field.matrix.get(key));

	if (debug)
	  {
	    System.err.println(" contents: " + entry);
	  }

	this.matrix.put(key, entry);
      }

    defined = true;
  }

  // we never allow setValue

  public boolean verifyTypeMatch(Object v)
  {
    return false;
  }

  // we never allow setValue

  public boolean verifyNewValue(Object v)
  {
    return false;
  }

  /**
   *
   * fancy equals method really does check for value equality
   *
   */

  public synchronized boolean equals(Object obj)
  {
    PermissionMatrixDBField pmdb;
    Enumeration keys;
    String key;
    
    /* -- */

    if (!(obj.getClass().equals(this.getClass())))
      {
	return false;
      }

    pmdb = (PermissionMatrixDBField) obj;

    if (matrix.size() != pmdb.matrix.size())
      {
	return false;
      }

    keys = matrix.keys();

    while (keys.hasMoreElements())
      {
	key = (String) keys.nextElement();

	try
	  {
	    if (!(matrix.get(key).equals(pmdb.matrix.get(key))))
	      {
		return false;
	      }
	  }
	catch (NullPointerException ex)
	  {
	    return false;
	  }
      }
    
    return true;
  }

  /**
   *
   * we don't really want to hash according to our permission
   * contents, so just hash according to our containing object's
   * i.d.
   *
   */

  public Object key()
  {
    return new Integer(owner.getID());
  }

  /**
   *
   * we don't allow setValue.. PermissionMatrixDBField doesn't allow
   * direct setting of the entire matrix.. just use the get() and set()
   * methods below.
   *
   */

  public ReturnVal setValue(Object value, boolean local)
  {
    return Ganymede.createErrorDialog("Server: Error in PermissionMatrixDBField.setValue()",
				      "Error.. can't call setValue() on a PermissionMatrixDBField");
  }

  public Object clone()
  {
    try
      {
	return new PermissionMatrixDBField(owner, this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Couldn't create PermissionMatrixDBField: " + ex);
      }
  }

  synchronized void emit(DataOutput out) throws IOException
  {
    Enumeration keys;
    PermEntry pe;
    String key;

    /* -- */

    out.writeInt(matrix.size());

    keys = matrix.keys();

    while (keys.hasMoreElements())
      {
	key = (String) keys.nextElement();
	pe = (PermEntry) matrix.get(key);

	out.writeUTF(key);
	pe.emit(out);
      }
  }

  synchronized void receive(DataInput in) throws IOException
  {
    int tableSize;
    PermEntry pe;
    String key;

    /* -- */

    tableSize = in.readInt();
    matrix = new Hashtable(tableSize);
    
    for (int i = 0; i < tableSize; i++)
      {
	key = in.readUTF();
	pe = new PermEntry(in);
	matrix.put(key, pe);
      }

    defined = true;
  }

  public synchronized String getValueString()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (value == null)
      {
	return "null";
      }

    return "PermissionMatrix";
  }

  /**
   *
   * The default getValueString() encoding is acceptable.
   *
   */

  public String getEncodingString()
  {
    return getValueString();
  }

  /**
   *
   * Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.<br><br>
   *
   * If there is no change in the field, null will be returned.
   * 
   */

  public String getDiffString(DBField orig)
  {
    PermissionMatrixDBField origP;
    StringBuffer result = new StringBuffer();

    /* -- */

    if (!(orig instanceof PermissionMatrixDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origP = (PermissionMatrixDBField) orig;

    if (!origP.equals(this))
      {
	return "\tPermission Matrix Changes.. Descriptive Code Not Yet Implemented";
      }
    else
      {
	return null;
      }
  }

  /**
   *
   * Return a copy of this field's permission matrix
   *
   * @see arlut.csd.ganymede.perm_field
   *
   */

  public PermMatrix getMatrix()
  {
    return new PermMatrix(this);
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field &lt;fieldID&gt; in base &lt;baseID&gt;
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(short baseID, short fieldID)
  {
    PermEntry result;

    /* -- */

    result = (PermEntry) matrix.get(matrixEntry(baseID, fieldID));

    if (result == null)
      {
	result = (PermEntry) matrix.get(matrixEntry(baseID, "default"));
      }

    return result;
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base &lt;baseID&gt;
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(short baseID)
  {
    return (PermEntry) matrix.get(matrixEntry(baseID));
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field &lt;field&gt; in base &lt;base&gt;
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(Base base, BaseField field)
  {
    PermEntry result;

    /* -- */

    try
      {
	result = (PermEntry) matrix.get(matrixEntry(base.getTypeID(), 
						    field.getID()));
	if (result == null)
	  {
	    result = (PermEntry) matrix.get(matrixEntry(base.getTypeID(), 
							"default"));
	  }

	return result;
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base &lt;base&gt;
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(Base base)
  {
    try
      {
	return (PermEntry) matrix.get(matrixEntry(base.getTypeID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   *
   * Sets the permission entry for this matrix for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to PermEntry &lt;entry&gt;.<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix 
   */

  public synchronized void setPerm(short baseID, short fieldID, PermEntry entry)
  {
    if (isEditable())
      {
	matrix.put(matrixEntry(baseID, fieldID), entry);
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }

    if (debug)
      {
	System.err.println("PermissionMatrixDBField: base " + 
			   baseID + ", field " + fieldID + " set to " + entry);
      }

    defined = true;
  }

  /**
   *
   * Sets the permission entry for this matrix for base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public synchronized void setPerm(short baseID, PermEntry entry)
  {
    if (isEditable())
      {
	matrix.put(matrixEntry(baseID), entry);
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }

    if (debug)
      {
	System.err.println("PermissionMatrixDBField: base " + baseID + " set to " + entry);
      }

    defined = true;
  }

  /**
   *
   * Sets the default permission entry to apply to fields under base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public void setDefaultFieldsPerm(short baseID, PermEntry entry)
  {
    if (isEditable())
      {
	matrix.put(matrixEntry(baseID, "default"), entry);
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }

    if (debug)
      {
	System.err.println("PermissionMatrixDBField: base " + baseID + " set to " + entry);
      }

    defined = true;
  }

  /**
   *
   * Sets the permission entry for this matrix for base &lt;base&gt;,
   * field &lt;field&gt; to PermEntry &lt;entry&gt;<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public synchronized void setPerm(Base base, BaseField field, PermEntry entry)
  {
    if (isEditable())
      {
	try
	  {
	    matrix.put(matrixEntry(base.getTypeID(), field.getID()), entry);

	    if (debug)
	      {
		System.err.println("PermissionMatrixDBField: base " + base.getName() + 
				   ", field " + field.getName() + " set to " + entry);
	      }
	  }
	catch (RemoteException ex)
	  {
	  }
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }

    defined = true;
  }

  /**
   *
   * Sets the permission entry for this matrix for base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;.<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public synchronized void setPerm(Base base, PermEntry entry)
  {
    if (isEditable())
      {
	try
	  {
	    matrix.put(matrixEntry(base.getTypeID()), entry);

	    if (debug)
	      {
		System.err.println("PermissionMatrixDBField: base " + 
				   base.getName() + 
				   " set to " + entry);
	      }
	  }
	catch (RemoteException ex)
	  {
	  }
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }

    defined = true;
  }

  /**
   *
   * Sets the default permission entry to apply to fields under base &lt;base&gt;
   * to PermEntry &lt;entry&gt;<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public void setDefaultFieldsPerm(Base base, PermEntry entry)
  {
    if (isEditable())
      {
	try
	  {
	    matrix.put(matrixEntry(base.getTypeID(), "default"), entry);

	    if (debug)
	      {
		System.err.println("PermissionMatrixDBField: base " +
				   base.getName() + " set to " + entry);
	      }
	  }
	catch (RemoteException ex)
	  {
	  }
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }


    defined = true;
  }


  private String matrixEntry(short baseID, String text)
  {
    return (baseID + ":" + text);
  }

  private String matrixEntry(short baseID, short fieldID)
  {
    return (baseID + ":" + fieldID);
  }
  
  private String matrixEntry(short baseID)
  {
    return (baseID + "::");
  }

  /**
   *
   * This method is used to basically dump state out of this field
   * so that the DBEditSet checkpoint() code can restore it later
   * if need be.
   *
   */

  public synchronized Object checkpoint()
  {
    if (matrix != null)
      {
	return new PermMatrixCkPoint(this);
      }
    else
      {
	return null;
      }
  }

  /**
   *
   * This method is used to basically force state into this field.<br><br>
   *
   * It is used to place a value or set of values that were known to
   * be good during the current transaction back into this field,
   * without creating or changing this DBField's object identity.
   *
   */

  public synchronized void rollback(Object oldval)
  {
    if (!(owner instanceof DBEditObject))
      {
	throw new RuntimeException("Invalid rollback on field " + 
				   getName() + ", not in an editable context");
      }

    if (!(oldval instanceof PermMatrixCkPoint))
      {
	throw new RuntimeException("Invalid rollback on field " + 
				   getName() + ", not a PermMatrixCkPoint");
      }

    if (oldval == null)
      {
	this.defined = false;
      }
    else
      {
	this.matrix = ((PermMatrixCkPoint) oldval).matrix;
	this.defined = true;
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                               PermMatrixCkPoint

------------------------------------------------------------------------------*/

class PermMatrixCkPoint {

  Hashtable matrix = new Hashtable();

  /* -- */

  public PermMatrixCkPoint(PermissionMatrixDBField field)
  {
    Enumeration enum = field.matrix.keys();
    Object key;
    PermEntry val;

    /* -- */

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();
	val = (PermEntry) field.matrix.get(key);

	matrix.put(key, new PermEntry(val));
      }
  }

}

