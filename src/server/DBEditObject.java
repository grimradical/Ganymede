/*
   GASH 2

   DBEditObject.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.107 $
   Last Mod Date: $Date: 1999/04/28 06:46:50 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBEditOBject

------------------------------------------------------------------------------*/

/**
 * <p>DBEditObject is the main base class that is subclassed by individual
 * application object types to provide editing and management intelligence.
 * Both static and instance methods are defined in DBEditObject which can
 * be subclassed to provide object management intelligence.</p> 
 *
 * <p>A instance of DBEditObject is a copy of a DBObject that has been
 * exclusively checked out from the main database so that a 
 * {@link arlut.csd.ganymede.DBSession DBSession}
 * can edit the fields of the object.  The DBEditObject class keeps
 * track of the changes made to fields, keeping things properly
 * synchronized with unique field name spaces.</p>
 *
 * <p>All DBEditObjects are obtained in the context of a 
 * {@link arlut.csd.ganymede.DBEditSet DBEditSet} transaction object.  When
 * the DBEditSet is committed, the DBEditObject is made to replace the
 * original object from the DBStore.  If the EditSet is aborted, the
 * DBEditObject is dropped.</p>
 *
 * <p><b>IMPORTANT PROGRAMMING NOTE!</b>: It is critical that
 * synchronized methods in DBEditObject and in subclasses thereof do not
 * call synchronized methods in DBSession, as there is a strong possibility
 * of nested monitor deadlocking.</p>
 *   
 * @version $Revision: 1.107 $ $Date: 1999/04/28 06:46:50 $ $Name:  $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class DBEditObject extends DBObject implements ObjectStatus, FieldType {

  static boolean debug = false;

  public final static int FIRSTOP = 0;
  public final static int SETVAL = 1;
  public final static int SETELEMENT = 2;
  public final static int ADDELEMENT = 3;
  public final static int DELELEMENT = 4;
  public final static int LASTOP = 4;

  public final static void setDebug(boolean val)
  {
    debug = val;
  }

  /* --------------------- Instance fields and methods --------------------- */

  /**
   * <p>Unless this DBEditObject was newly created, we'll have a reference
   * to the original DBObject which is currently registered in the DBStore.
   * Only one DBEditObject can be connected to a DBObject at a time, giving
   * us object-level locking.</p>
   */

  protected DBObject original;

  /**
   * true if this object has had its commitPhase1() method called,
   * but has not yet had its commitPhase2() or release() methods
   * called.  If committing is true, no editing will be allowed
   * on this object.
   */

  protected boolean committing;

  /**
   * true if the object is in the middle of carrying
   * out deletion logic.. consulted by subclasses
   * to determine whether they should object to fields
   * being set to null
   */

  protected boolean deleting = false;	

  /**
   * true if this object has been processed
   * by a DBEditSet's commit logic
   */

  boolean finalized = false;

  /**
   * tracks this object's editing status.  See
   * {@link arlut.csd.ganymede.ObjectStatus ObjectStatus}.
   */

  byte status;

  /**
   * true if the object has a version currently
   * stored in the DBStore
   */

  boolean stored;		

  /**
   * <p>Used as a coordinating signal with InvidDBField to handle
   * clearing the backlinks field in 
   * {@link arlut.csd.ganymede.DBEditObject#finalizeRemove(boolean) finalizeRemove()}.</p>
   *
   * <p>Should never ever ever ever be messed with outside this
   * object.</p>
   */

  boolean clearingBackLinks = false;

  /* -- */

  /**
   * <p>Dummy constructor, is responsible for creating a DBEditObject strictly
   * for the purpose of having a handle to call customization methods on.</p>
   */

  public DBEditObject(DBObjectBase base)
  {
    this.objectBase = base;
    editset = null;		// this will be our cue to our static handle status for our methods
  }

  /**
   * <p>Creation constructor, is responsible for creating a new editable
   * object with all fields listed in the DBObjectBaseField instantiated
   * but undefined.</p>
   *
   * <p>This constructor is not really intended to be overriden in subclasses.
   * Creation time field value initialization is to be handled by
   * initializeNewObject().</p>
   *
   * @see arlut.csd.ganymede.DBField
   */

  public DBEditObject(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid.getNum());

    if (editset == null)
      {
	throw new NullPointerException("null editset");
      }

    original = null;
    this.editset = editset;
    this.gSession = editset.getSession().getGSession();
    committing = false;
    stored = false;
    status = CREATING;

    /* -- */

    Enumeration 
      enum = null;

    DBObjectBaseField 
      fieldDef;

    DBField 
      tmp = null;

    /* -- */

    fields = new DBFieldTable(objectBase.fieldTable.size(), (float) 1.0);

    synchronized (objectBase)
      {
	enum = objectBase.fieldTable.elements();
	
	while (enum.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) enum.nextElement();

	    // check for permission to create a particular field

	    if (!checkNewField(fieldDef.getID()))
	      {
		continue;
	      }

	    switch (fieldDef.getType())
	      {
	      case BOOLEAN:
		tmp = new BooleanDBField(this, fieldDef);
		break;
		    
	      case NUMERIC:
		tmp = new NumericDBField(this, fieldDef);
		break;
		
	      case DATE:
		tmp = new DateDBField(this, fieldDef);
		break;

	      case STRING:
		tmp = new StringDBField(this, fieldDef);
		break;
		    
	      case INVID:
		tmp = new InvidDBField(this, fieldDef);
		break;

	      case PERMISSIONMATRIX:
		tmp = new PermissionMatrixDBField(this, fieldDef);
		break;

	      case PASSWORD:
		tmp = new PasswordDBField(this, fieldDef);
		break;

	      case IP:
		tmp = new IPDBField(this, fieldDef);
		break;
	      }

	    if (tmp != null)
	      {
		fields.putNoSyncNoRemove(tmp);
	      }
	  }
      }
  }

  /**
   * <p>Check-out constructor, used by
   * {@link arlut.csd.ganymede.DBObject#createShadow(arlut.csd.ganymede.DBEditSet) DBObject.createShadow()}
   * to pull out an object for editing.</p>
   */

  public DBEditObject(DBObject original, DBEditSet editset)
  {
    super(original.objectBase);

    Enumeration 
      enum;

    DBObjectBaseField 
      fieldDef;

    DBField 
      field, 
      tmp = null;

    Short key;

    /* -- */

    shadowObject = null;
    this.editset = editset;
    committing = false;
    stored = true;
    status = EDITING;

    fields = new DBFieldTable(objectBase.fieldTable.size(), (float) 1.0);

    gSession = getSession().getGSession();

    synchronized (original)
      {
	this.original = original;
	this.id = original.id;
	this.myInvid = original.myInvid;
	this.objectBase = original.objectBase;
      }

    // clone the fields from the original object
    // since we own these, the field-modifying
    // methods on the copied fields will allow editing
    // to go forward

    if (original.fields != null)
      {
	enum = original.fields.elements();

	while (enum.hasMoreElements())
	  {
	    field = (DBField) enum.nextElement();

	    switch (field.getType())
	      {
	      case BOOLEAN:
		tmp = new BooleanDBField(this, (BooleanDBField) field);
		break;
		    
	      case NUMERIC:
		tmp = new NumericDBField(this, (NumericDBField) field);
		break;

	      case DATE:
		tmp = new DateDBField(this, (DateDBField) field);
		break;

	      case STRING:
		tmp = new StringDBField(this, (StringDBField) field);
		break;
		    
	      case INVID:
		tmp = new InvidDBField(this, (InvidDBField) field);
		break;

	      case PERMISSIONMATRIX:
		tmp = new PermissionMatrixDBField(this, (PermissionMatrixDBField) field);
		break;

	      case PASSWORD:
		tmp = new PasswordDBField(this, (PasswordDBField) field);
		break;

	      case IP:
		tmp = new IPDBField(this, (IPDBField) field);
		break;
	      }

	    if (tmp != null)
	      {
		fields.putNoSyncNoRemove(tmp);
	      }
	  }
      }
	
    // now create slots for any fields that are in this object type's
    // DBObjectBase, but which were not present in the original
    
    synchronized (objectBase)
      {
	enum = objectBase.fieldTable.elements();
	
	while (enum.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) enum.nextElement();
	    
	    if (!fields.containsKey(fieldDef.getID()))
	      {
		if (!checkNewField(fieldDef.getID()))
		  {
		    continue;
		  }
		
		switch (fieldDef.getType())
		  {
		  case BOOLEAN:
		    tmp = new BooleanDBField(this, fieldDef);
		    break;
		    
		  case NUMERIC:
		    tmp = new NumericDBField(this, fieldDef);
		    break;
		
		  case DATE:
		    tmp = new DateDBField(this, fieldDef);
		    break;

		  case STRING:
		    tmp = new StringDBField(this, fieldDef);
		    break;
		    
		  case INVID:
		    tmp = new InvidDBField(this, fieldDef);
		    break;

		  case PERMISSIONMATRIX:
		    tmp = new PermissionMatrixDBField(this, fieldDef);
		    break;

		  case PASSWORD:
		    tmp = new PasswordDBField(this, fieldDef);
		    break;

		  case IP:
		    tmp = new IPDBField(this, fieldDef);
		    break;

		  }

		fields.putNoSyncNoRemove(tmp);
	      }
	  }
      }
  }

  /**
   * <p>Returns the DBSession that this object is checked out in
   * care of.</p>
   *
   * @see arlut.csd.ganymede.DBSession
   */

  public final DBSession getSession()
  {
    return editset.getSession();
  }

  /**
   * <p>Returns the GanymedeSession that this object is checked out in
   * care of.</p>
   *
   * @see arlut.csd.ganymede.GanymedeSession
   */

  public final GanymedeSession getGSession()
  {
    return getSession().getGSession();
  }

  /**
   * <p>Returns the original version of the object that we were created
   * to edit.  If we are a newly created object, this method will
   * return null.</p>
   */

  public final DBObject getOriginal()
  {
    return original;
  }

  /**
   * <p>Returns a code indicating whether this object
   * is being created, edited, or deleted.</p>
   *
   * @see arlut.csd.ganymede.ObjectStatus#CREATING
   * @see arlut.csd.ganymede.ObjectStatus#EDITING
   * @see arlut.csd.ganymede.ObjectStatus#DELETING
   * @see arlut.csd.ganymede.ObjectStatus#DROPPING
   */

  public final byte getStatus()
  {
    return status;
  }

  /**
   * <p>This method is used to make sure that the built-in fields that
   * the server assumes will always be present in any editable object
   * will be in place.</p>
   * 
   * <p>This method checks with instantiateNewField() if the field id is
   * not one of those that is needfull.  If instantiateNewField() approves
   * the creation of a new field, checkNewField() will check to see if
   * the {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}'s
   * permissions permit the field creation.</p>
   */

  public final boolean checkNewField(short fieldID)
  {
    if (fieldID <= 8)
      {
	return true;		// we always allow the built in fields
      }

    return instantiateNewField(fieldID);
  }

  /**
   * <p>Sets this object's status code</p>
   *
   * @see arlut.csd.ganymede.ObjectStatus#CREATING
   * @see arlut.csd.ganymede.ObjectStatus#EDITING
   * @see arlut.csd.ganymede.ObjectStatus#DELETING
   * @see arlut.csd.ganymede.ObjectStatus#DROPPING
   */

  final void setStatus(byte new_status)
  {
    switch (new_status)
      {
      case CREATING:
      case EDITING:
      case DELETING:
      case DROPPING:
	status = new_status;
	break;

      default:
	throw new RuntimeException("unrecognized status code");
      }
  }

  /**
   * <p>Shortcut method to set a field's value.  Using this
   * method can save the client a roundtrip to the server.</p>
   *
   * <p>This method cannot be used on permission fields or password
   * fields.</p>
   *
   * @see arlut.csd.ganymede.db_object 
   */

  public final ReturnVal setFieldValue(short fieldID, Object value)
  {
    // note! this *must* be setValue(), not setValueLocal(), as this
    // is a method that the client calls directly.

    try
      {
	return getField(fieldID).setValue(value);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote on a local op: " + ex);
      }
  }

  /**
   * <p>Shortcut method to set a field's value.  This version bypasses
   * permission checking and is only intended for server-side
   * use.</p>
   *
   * <p>This method cannot be used on permission fields or password
   * fields.</p>
   */

  public final ReturnVal setFieldValueLocal(short fieldID, Object value)
  {
    ReturnVal retval;
    DBField field = (DBField) getField(fieldID);

    /* -- */

    if (field != null)
      {
	return field.setValueLocal(value);
      }

    return Ganymede.createErrorDialog("DBEditObject.setFieldValueLocal() error",
				      "DBEditObject.setFieldValueLocal() couldn't find field " + fieldID + 
				      " in object " + getLabel());
  }

  /** 
   * <p>Returns true if the object has ever been stored in the 
   * {@link arlut.csd.ganymede.DBStore DBStore} under the
   * current invid.</p>
   */

  public final boolean isStored()
  {
    return stored;
  }

  /**
   * <p>Clears out any non-valued fields, used to clean out any fields 
   * that remained undefined after editing is done.</p>
   */

  final synchronized void clearTransientFields()
  {
    Enumeration enum;
    DBField field;
    Vector removeList;
    
    /* -- */

    removeList = new Vector();
    enum = fields.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();

	// we don't want to emit fields that don't have anything in them..
	// DBField.isDefined() is supposed to tell us whether a field should
	// be kept.

	if (!field.isDefined())
	  {
	    removeList.addElement(field);

	    if (false)
	      {
		System.err.println("going to be removing transient: " + ((DBField) field).getName()); 
	      }
	  }
      }

    enum = removeList.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();
	fields.remove(field.getID());
      }
  }

  /* -------------------- pseudo-static Customization hooks -------------------- 


     The following block of methods are intended to be used in static fashion..
     that is, a DBObjectBase can load in a class that extends DBEditObjectBase
     and hold an instance of such as DBObjectBase.objectHook.  The following
     methods are used in a static fashion, that is they are primarily intended
     to perform actions on designated external DBObjects rather than on the
     per-DBObjectBase instance.

  */

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * make a link to the given field in this 
   * {@link arlut.csd.ganymede.DBObject DBObject} type when the
   * user only has editing access for the source 
   * {@link arlut.csd.ganymede.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param object The object that the link is to be created in
   * @param fieldID The field that the link is to be created in
   * @param gsession Who is trying to do this linking?
   */

  public boolean anonymousLinkOK(DBObject object, short fieldID, GanymedeSession gsession)
  {
    return anonymousLinkOK(object, fieldID);
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this 
   * {@link arlut.csd.ganymede.DBObject DBObject} type when the
   * user only has editing access for the source 
   * {@link arlut.csd.ganymede.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param object The object that the link is to be removed from
   * @param fieldID The field that the linkk is to be removed from
   * @param gsession Who is trying to do this unlinking?
   */

  public boolean anonymousUnlinkOK(DBObject object, short fieldID, GanymedeSession gsession)
  {
    return anonymousUnlinkOK(object, fieldID);
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * make a link to the given field in this 
   * {@link arlut.csd.ganymede.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param object The object that the link is to be created in
   * @param fieldID The field that the link is to be created in
   */

  public boolean anonymousLinkOK(DBObject object, short fieldID)
  {
    return false;		// by default, permission is denied
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this 
   * {@link arlut.csd.ganymede.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param object The object that the link is to be removed from
   * @param fieldID The field that the linkk is to be removed from
   */

  public boolean anonymousUnlinkOK(DBObject object, short fieldID)
  {
    return true;		// by default, permission is granted to unlink
  }

  /**
   * <p>Customization method to allow this Ganymede object type to
   * override the default permissions mechanism for special
   * purposes.</p>
   *
   * <p>If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the permissions specified by this method for access to the
   * given object, and no further elaboration of the permission
   * will be performed.  Note that this override capability does
   * not apply to operations performed in supergash mode.</p>
   *
   * <p>This method should be used very sparingly.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public PermEntry permOverride(GanymedeSession session, DBObject object)
  {
    return null;
  }

  /**
   * <p>Customization method to allow this Ganymede object type to grant
   * permissions above and beyond the default permissions mechanism
   * for special purposes.</p>
   *
   * <p>If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the union of the permissions specified by this method for access to the
   * given object.</p>
   *
   * <p>This method is essentially different from permOverride() in that
   * the permissions system will not just take the result of this
   * method for an answer, but will grant additional permissions as
   * appropriate.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public PermEntry permExpand(GanymedeSession session, DBObject object)
  {
    return null;
  }

  /**
   * <p>Customization method to allow this Ganymede object type to
   * override the default permissions mechanism for special
   * purposes.</p>
   *
   * <p>If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the permissions specified by this method for access to the
   * given field, and no further elaboration of the permission
   * will be performed.  Note that this override capability does
   * not apply to operations performed in supergash mode.</p>
   *
   * <p>This method should be used very sparingly.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public PermEntry permOverride(GanymedeSession session, DBObject object, short fieldid)
  {
    return null;
  }

  /**
   * <p>Customization method to allow this Ganymede object type to grant
   * permissions above and beyond the default permissions mechanism
   * for special purposes.</p>
   *
   * <p>If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the union of the permissions specified by this method for access to the
   * given field.</p>
   *
   * <p>This method is essentially different from permOverride() in that
   * the permissions system will not just take the result of this
   * method for an answer, but will grant additional permissions as
   * appropriate.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public PermEntry permExpand(GanymedeSession session, DBObject object, short fieldid)
  {
    return null;
  }

  /**
   * <p>Customization method to verify overall consistency of
   * a DBObject.  While default code has not yet been
   * written for this method, it may need to have its
   * parameter list modified to include the controlling
   * {@link arlut.csd.ganymede.DBSession DBSession}
   * to allow coordination of {@link arlut.csd.ganymede.DBLock DBLock}
   * and the the use of 
   * {@link arlut.csd.ganymede.DBEditSet#findObject(arlut.csd.ganymede.DBObject) DBEditSet.findObject()}
   * to get a transaction-consistent view of related objects.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal consistencyCheck(DBObject object)
  {
    return null;
  }

  /**
   * <p>Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p>Note that this method will not be called if the controlling
   * GanymedeSession's enableOversight is turned off, as in
   * bulk loading.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    return false;
  }

  /**
   * <p>Customization method to verify whether the user has permission
   * to view a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for viewing to the client.</p>
   *
   * To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canRead(DBSession session, DBObject object)
  {
    return true;
  }

  /**
   * <p>Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of 
   * {@link arlut.csd.ganymede.DBField DBField} will
   * wind up calling up to here to let us override the normal visibility
   * process.</p>
   *
   * <p>Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.</p>
   *
   * <p>If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    // by default, return the field definition's visibility

    if (field.getFieldDef().base != this.objectBase)
      {
	throw new IllegalArgumentException("field/object mismatch");
      }

    return field.getFieldDef().isVisible(); 
  }

  /**
   * <p>Customization method to verify whether the user has permission
   * to edit a given object.  The client's 
   * {@link arlut.csd.ganymede.DBSession DBSession} object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for editing by the client.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canWrite(DBSession session, DBObject object)
  {
    return true;
  }

  /**
   * <p>Customization method to verify whether this object type has an inactivation
   * mechanism.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canBeInactivated()
  {
    return false;
  }

  /**
   * <p>Customization method to verify whether the user has permission
   * to inactivate a given object.  The client's 
   * {@link arlut.csd.ganymede.DBSession DBSession} object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for inactivating by the client.</p>
   *
   * <p>Note that unlike canRemove(), canInactivate() takes a
   * DBEditObject instead of a DBObject.  This is because inactivating
   * an object is based on editing the object, and so we have the
   * GanymedeSession/DBSession classes go ahead and check the object
   * out for editing before calling us.  This serves to force the
   * session classes to check for write permission before attempting
   * inactivation.</p>
   *
   * <p>Use canBeInactivated() to test for the presence of an inactivation
   * protocol outside of an edit context if needed.
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canInactivate(DBSession session, DBEditObject object)
  {
    return false;
  }

  /**
   * <p>Customization method to verify whether the user has permission
   * to remove a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for removal by the client.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canRemove(DBSession session, DBObject object)
  {
    // our default behavior is that objects that can be inactivated
    // should not be deleted except by supergash

    if (this.canBeInactivated())
      {
	GanymedeSession myMaster = session.getGSession();

	/* -- */
	
	if (myMaster == null)
	  {
	    // hm, not an end-user.. let it go
	    
	    return true;
	  }

	// only supergash can delete users.. everyone else can only
	// inactivate.
	
	if (myMaster.isSuperGash())
	  {
	    return true;
	  }

	return false;
      }
    
    return true;
  }

  /**
   * <p>Customization method to verify whether the user has permission
   * to clone a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for cloning by the client.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canClone(DBSession session, DBObject object)
  {
    return false;
  }


  /**
   * <p>Hook to allow the cloning of an object.  If this object type
   * supports cloning (which should be very much customized for this
   * object type.. creation of the ancillary objects, which fields to
   * clone, etc.), this customization method will actually do the work.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public DBEditObject cloneObject(DBSession session, DBObject object)
  {
    return null;
  }

  /**
   * <p>Customization method to verify whether the user has permission
   * to create an instance of this object type.  The client's 
   * {@link arlut.csd.ganymede.DBSession DBSession} object
   * will call the canCreate method in the 
   * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} for this object type
   * to determine whether creation is allowed to the user.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canCreate(Session session)
  {
    if (session != null && (session instanceof GanymedeSession))
      {
	GanymedeSession gSession;

	/* -- */

	gSession = (GanymedeSession) session;

	return gSession.getPerm(getTypeID(), true).isCreatable(); // *sync* GanymedeSession
      }

    // note that we are going ahead and returning false here, as
    // we assume that the client will always use the local BaseDump
    // copy and won't generally call us remotely with a remote
    // interface.

    return false;
  }

  /**
   * <p>Hook to allow intelligent generation of labels for DBObjects
   * of this type.  Subclasses of DBEditObject should override
   * this method to provide for custom generation of the
   * object's label type</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public String getLabelHook(DBObject object)
  {
    return null;		// no default
  }

  /**
   * <p>Hook to allow subclasses to grant ownership privileges to a given
   * object.  If this method returns true on a given object, the Ganymede
   * Permissions system will provide access to the object as owned with
   * whatever permissions apply to objects owned by the persona active
   * in gSession.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean grantOwnership(GanymedeSession gSession, DBObject object)
  {
    return false;
  }

  /**
   * <p>This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * user's name interposed.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean virtualizeField(short fieldID)
  {
    return false;
  }

  /**
   * <p>This method provides a hook to return interposed values for
   * fields that have their data massaged by a DBEditObject
   * subclass.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public Object getVirtualValue(DBField field)
  {
    return null;
  }

  /* -------------------- editing/creating Customization hooks -------------------- 


     The following block of methods are intended to be subclassed to
     provide intelligence for the object creation/editing process.

  */

  /**
   * <p>Initializes a newly created DBEditObject.</p>
   *
   * <p>When this method is called, the DBEditObject has been created,
   * its ownership set, and all fields defined in the controlling
   * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}
   * have been instantiated without defined
   * values.</p>
   *
   * <p>This method is responsible for filling in any default
   * values that can be calculated from the 
   * {@link arlut.csd.ganymede.DBSession DBSession}
   * associated with the editset defined in this DBEditObject.</p>
   *
   * <p>If initialization fails for some reason, initializeNewObject()
   * will return false.  If the owning GanymedeSession is not in
   * bulk-loading mode (i.e., enableOversight is true),
   * {@link arlut.csd.ganymede.DBSession#createDBObject(short, arlut.csd.ganymede.Invid, java.util.Vector) 
   * DBSession.createDBObject()} will checkpoint the transaction before
   * calling this method.  If this method returns false, the calling
   * method will rollback the transaction.  This method has no
   * responsibility for undoing partial initialization, the
   * checkpoint/rollback logic will take care of that.</p>
   *
   * <p>If enableOversight is false, DBSession.createDBObject() will not
   * checkpoint the transaction status prior to calling initializeNewObject(),
   * so it is the responsibility of this method to handle any checkpointing
   * needed.</p>
   *
   * <p>This method should be overridden in subclasses.</p>
   */

  public boolean initializeNewObject()
  {
    return true;
  }

  /**
   * <p>This method provides a hook that can be used to indicate whether
   * a field that is defined in this object's field dictionary
   * should be newly instantiated in this particular object.</p>
   *
   * <p>This method does not affect those fields which are actually present
   * in the object's record in the
   * {@link arlut.csd.ganymede.DBStore DBStore}.  What this method allows
   * you to do is have a subclass decide whether it wants to instantiate
   * a potential field (one that is declared in the field dictionary for
   * this object, but which doesn't happen to be presently defined in
   * this object) in this particular object.</p>
   *
   * <p>A concrete example will help here.  The Permissions Object type
   * (base number SchemaConstants.PermBase) holds a permission
   * matrix, a descriptive title, and a list of admin personae that hold
   * those permissions for objects they own.</p>
   *
   * <p>There are a few specific instances of SchemaConstants.PermBase
   * that don't properly need the list of admin personae, as their
   * object invids are hard-coded into the Ganymede security system, and
   * their permission matrices are automatically consulted in certain
   * situations.  In order to support this, we're going to want to have
   * a DBEditObject subclass for managing permission objects.  In that
   * subclass, we'll define instantiateNewField() so that it will return
   * false if the fieldID corresponds to the admin personae list if the
   * object's ID is that of one of these special objects.  As a result,
   * when the objects are viewed by an administrator, the admin personae
   * list will not be seen.</p>
   */

  public boolean instantiateNewField(short fieldID)
  {
    return gSession.getPerm(getTypeID(), fieldID, true).isCreatable(); // *sync* GanymedeSession
  }

  /**
   * <p>This method is the hook that DBEditObject subclasses use to interpose
   * {@link arlut.csd.ganymede.GanymediatorWizard wizards} when a field's
   * value is being changed.</p>
   *
   * <p>Whenever a field is changed in this object, this method will be
   * called with details about the change. This method can refuse to
   * perform the operation, it can make changes to other objects in
   * the database in response to the requested operation, or it can
   * choose to allow the operation to continue as requested.</p>
   *
   * <p>In the latter two cases, the wizardHook code may specify a list
   * of fields and/or objects that the client may need to update in
   * order to maintain a consistent view of the database.</p>
   *
   * <p>If server-local code has called
   * {@link arlut.csd.ganymede.GanymedeSession#enableOversight(boolean) 
   * enableOversight(false)},
   * this method will never be
   * called.  This mode of operation is intended only for initial
   * bulk-loading of the database.</p>
   *
   * <p>This method may also be bypassed when server-side code uses
   * setValueLocal() and the like to make changes in the database.</p>
   *
   * <p>This method is called before the finalize*() methods.. the finalize*()
   * methods is where last minute cascading changes should be performed..
   * Note as well that wizardHook() is called before the namespace checking
   * for the proposed value is performed, while the finalize*() methods are
   * called after the namespace checking.</p>
   *
   * @return a ReturnVal object indicated success or failure, objects and
   * fields to be rescanned by the client, and a doNormalProcessing flag
   * that will indicate to the field code whether or not the operation
   * should continue to completion using the field's standard logic.
   * <b>It is very important that wizardHook return a new ReturnVal(true, true)
   * if the wizardHook wishes to simply specify rescan information while
   * having the field perform its standard operation.</b>  wizardHook() may
   * return new ReturnVal(true, false) if the wizardHook performs the operation
   * (or a logically related operation) itself.  The same holds true for the
   * respond() method in GanymediatorWizard subclasses.
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    return null;		// by default, we just ok whatever
  }

  /**
   * <p>Hook to have this object create a new embedded object
   * in the given field.</p>
   *
   * <p>This method now has the appropriate default logic for creating
   * embedded objects with the user's permissions, but this method may
   * still be overridden to do customization, if needed.</p>
   */

  public Invid createNewEmbeddedObject(InvidDBField field)
  {    
    DBEditObject newObject;
    DBObjectBase targetBase;
    DBObjectBaseField fieldDef;
    ReturnVal retVal;

    /* -- */

    fieldDef = field.getFieldDef();

    if (!fieldDef.isEditInPlace())
      {
	throw new RuntimeException("error in server, DBEditObject.createNewEmbeddedObject() called " +
				    "on non-embedded object");
      }
	
    if (fieldDef.getTargetBase() > -1)
      {
	if (getGSession() != null)
	  {
	    // we use GanymedeSession to check permissions to create the target.

	    retVal = getGSession().create_db_object(fieldDef.getTargetBase(), true);
	
	    if (retVal == null)
	      {
		throw new RuntimeException("error in server, createNewEmbeddedObject " +
					   "could not get a useful result from " +
					   "create_db_object");
	      }
	
	    if (!retVal.didSucceed())
	      {
		return null;	// failure
	      }
	
	    newObject = (DBEditObject) retVal.getObject();
	
	    return newObject.getInvid();
	  }
	else
	  {
	    throw new RuntimeException("error in schema.. createNewEmbeddedObject called " +
				       "without a valid GanymedeSession..");
	  }
      }
    else
      {
	throw new RuntimeException("error in schema.. createNewEmbeddedObject " +
				   "called without a valid target..");
      }
  }

  /**
   * <p>This method provides a hook that can be used to check any values
   * to be set in any field in this object.  Subclasses of
   * DBEditObject should override this method, implementing basically
   * a large switch statement to check for any given field whether the
   * submitted value is acceptable given the current state of the
   * object.</p>
   *
   * <p>Question: what synchronization issues are going to be needed
   * between DBEditObject and DBField to insure that we can have
   * a reliable verifyNewValue method here?</p>
   */

  public ReturnVal verifyNewValue(DBField field, Object value)
  {
    return null;
  }

  // ****
  //
  // The following methods are here to allow our DBEditObject
  // to be involved in the processing of a particular 
  // vector operation on a field in this object.. otherwise
  // we'd have to subclass our fields for any processing
  // that would need to be done in response to an operation..
  //
  // ****

  /**
   * <p>This method allows the DBEditObject to have executive approval of
   * any vector delete operation, and to take any special actions in
   * reaction to the delete.. if this method returns null or a success
   * code in its ReturnVal, the {@link arlut.csd.ganymede.DBField DBField}
   * that called us will proceed to
   * make the change to its vector.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.</p>
   *
   * <p>The DBField that called us will take care of all possible checks
   * on the operation (including vector bounds, etc.).  Under normal
   * circumstances, we won't need to do anything here.</p>
   */

  public ReturnVal finalizeDeleteElement(DBField field, int index)
  {
    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval of
   * any vector add operation, and to take any special actions in
   * reaction to the add.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us will proceed to
   * make the change to its vector.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.</p>
   *
   * <p>The DBField that called us will take care of all possible checks
   * on the operation (including vector bounds, etc.).  Under normal
   * circumstances, we won't need to do anything here.</p>
   */

  public ReturnVal finalizeAddElement(DBField field, Object value)
  {
    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval of
   * any vector set operation, and to take any special actions in
   * reaction to the set.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us will proceed to
   * make the change to its vector.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.</p>
   *
   * <p>The DBField that called us will take care of all possible checks
   * on the operation (including vector bounds, etc.).  Under normal
   * circumstances, we won't need to do anything here.</p>
   */

  public ReturnVal finalizeSetElement(DBField field, int index, Object value)
  {
    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval of
   * any scalar set operation, and to take any special actions in
   * reaction to the set.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us will proceed to
   * make the change to its value.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.</p>
   *
   * <p>The DBField that called us will take care of all possible checks
   * on the operation (including a call to our own verifyNewValue()
   * method.  Under normal circumstances, we won't need to do anything
   * here.</p>
   */

  public ReturnVal finalizeSetValue(DBField field, Object value)
  {
    return null;
  }

  /**
   * <p>This method returns true if field1 should not show any choices
   * that are currently selected in field2, where both field1 and
   * field2 are fields in this object.</p>
   * 
   * <p>The purpose of this method is to allow mutual exclusion between
   * a pair of fields with mandatory choices.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   */

  public boolean excludeSelected(db_field field1, db_field field2)
  {
    return false;
  }

  /**
   * <p>This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.</p>
   *
   * <p>If there is no caching key, this method will return null.
   */

  public Object obtainChoicesKey(DBField field)
  {
    // by default, we return a Short containing the base
    // id for the field's target

    if ((field instanceof InvidDBField) && 
	!field.isEditInPlace())
      {
	DBObjectBaseField fieldDef;
	short baseId;

	/* -- */

	fieldDef = field.getFieldDef();
	
	baseId = fieldDef.getTargetBase();

	if (baseId < 0)
	  {
	    //	    Ganymede.debug("DBEditObject: Returning null 2 for choiceList for field: " + field.getName());
	    return null;
	  }

	return new Short(baseId);
      }

    return null;
  }

  /**
   * <p>This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.</p>
   *
   * <p>This method will provide a reasonable default for targetted
   * invid fields.</p>
   *
   * <p>NOTE: This method does not need to be synchronized.  Making this
   * synchronized can lead to DBEditObject/DBSession nested monitor
   * deadlocks.</p>
   */

  public QueryResult obtainChoiceList(DBField field)
  {
    if (field.isEditable() && (field instanceof InvidDBField) && 
	!field.isEditInPlace())
      {
	DBObjectBaseField fieldDef;
	short baseId;

	/* -- */

	fieldDef = field.getFieldDef();
	
	baseId = fieldDef.getTargetBase();

	if (baseId < 0)
	  {
	    //	    Ganymede.debug("DBEditObject: Returning null 2 for choiceList for field: " + field.getName());
	    return null;
	  }

	if (Ganymede.internalSession == null)
	  {
	    return null;
	  }

	// and we want to return a list of choices.. can use the regular
	// query output here

	QueryNode root;

	// if we are pointing to objects of our own type, we don't want ourselves to be
	// a valid choice by default.. (DBEditObject subclasses can override this, of course)

	if (baseId == getTypeID())
	  {
	    root = new QueryNotNode(new QueryDataNode((short) -2, QueryDataNode.EQUALS, getInvid()));
	  }
	else
	  {
	    root = null;
	  }

	boolean editOnly = !choiceListHasExceptions(field);

	// note that the query we are submitting here *will* be filtered by the
	// current visibilityFilterInvid field in GanymedeSession.

	return editset.getSession().getGSession().query(new Query(baseId, root, editOnly), this);
      }
    
    //    Ganymede.debug("DBEditObject: Returning null for choiceList for field: " + field.getName());
    return null;
  }

  /**
   * <p>This method is used to tell the client whether the list of options it gets
   * back for a field can be taken out of the cache.  If this method returns
   * true, that means that some of the results that obtainChoiceList() will
   * return will include items that normally wouldn't be availble to the
   * client, but are in this case because of the anonymousLinkOK() results.</p>
   *
   * <p>This is kind of wacked-out random stuff.  It's basically a way of
   * allowing DBEditObject to get involved in the decision as to whether an
   * {@link arlut.csd.ganymede.InvidDBField InvidDBField}'s 
   * {@link arlut.csd.ganymede.InvidDBField#choicesKey() choicesKey()}'s
   * method should disallow client-side caching for the field's choice list.</p>
   */

  public boolean choiceListHasExceptions(DBField field)
  {
    if (!(field instanceof InvidDBField))
      {
	throw new IllegalArgumentException("choiceListHasExceptions(): field not an InvidDBField.");
      }

    // --

    DBObjectBaseField fieldDef;
    short baseId;
    short targetField;

    /* -- */

    fieldDef = field.getFieldDef();
	
    baseId = fieldDef.getTargetBase();

    if (fieldDef.isSymmetric())
      {
	targetField = fieldDef.getTargetField();
      }
    else
      { 
	targetField = SchemaConstants.BackLinksField;
      }
    
    DBObjectBase targetBase = Ganymede.db.getObjectBase(baseId);

    return targetBase.getObjectHook().anonymousLinkOK(this, targetField, this.gSession);
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()</p>
   */

  public boolean mustChoose(DBField field)
  {
    // by default, we assume that InvidDBField's are always
    // must choose.
    
    if (field instanceof InvidDBField)
      {
	return true;
      }

    return false;
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass
   * can use to determine whether it is permissible to enter
   * IPv6 address in a particular (IP) DBField.</p>
   */

  public boolean isIPv6OK(DBField field)
  {
    return false;
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given 
   * {@link arlut.csd.ganymede.DateDBField DateDBField} has a restricted
   * range of possibilities.</p>
   */

  public boolean isDateLimited(DBField field)
  {
    return false;
  }

  /**
   * This method is used to specify the earliest acceptable date
   * for the specified {@link arlut.csd.ganymede.DateDBField DateDBField}.
   */

  public Date minDate(DBField field)
  {
    return new Date(Long.MIN_VALUE);
  }

  /**
   * This method is used to specify the latest acceptable date
   * for the specified {@link arlut.csd.ganymede.DateDBField DateDBField}.
   */

  public Date maxDate(DBField field)
  {
    return new Date(Long.MAX_VALUE);
  }

  /**
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given
   * {@link arlut.csd.ganymede.NumericDBField NumericDBField}
   * has a restricted range of possibilities.
   */

  public boolean isIntLimited(DBField field)
  {
    return false;
  }

  /**
   * This method is used to specify the minimum acceptable value
   * for the specified
   * {@link arlut.csd.ganymede.NumericDBField NumericDBField}.
   */

  public int minInt(DBField field)
  {
    return Integer.MIN_VALUE;
  }

  /**
   * This method is used to specify the maximum acceptable value
   * for the specified    
   * {@link arlut.csd.ganymede.NumericDBField NumericDBField}.
   */

  public int maxInt(DBField field)
  {
    return Integer.MAX_VALUE;
  }

  /**
   * <p>This method handles inactivation logic for this object type.  A
   * DBEditObject must first be checked out for editing, then the
   * inactivate() method can then be called on the object to put the
   * object into inactive mode.  inactivate() will set the object's
   * removal date and fix up any other state information to reflect
   * the object's inactive status.</p>
   *
   * <p>inactive() is designed to run synchronously with the user's
   * request for inactivation.  It can return a wizard reference
   * in the ReturnVal object returned, to guide the user through
   * a set of interactive dialogs to inactive the object.</p>
   *
   * <p>The inactive() method can cause other objects to be deleted, can cause
   * strings to be removed from fields in other objects, whatever.</p>
   *
   * <p>If inactivate() returns a ReturnVal that has its success flag set
   * to false and does not include a JDialogBuff for further
   * interaction with the user, then DBSEssion.inactivateDBObject()
   * method will rollback any changes made by this method.</p>
   *
   * <p>If inactivate() returns a success value, we expect that the object
   * will have a removal date set.</p>
   *
   * <p>IMPORTANT NOTE 1: This method is intended to be called by the
   * DBSession.inactivateDBObject() method, which establishes a
   * checkpoint before calling inactivate.  If this method is not
   * called by DBSession.inactivateDBObject(), you need to push
   * a checkpoint with the key 'inactivate'+label, where label is
   * the returned name of this object.
   *
   * <p>IMPORTANT NOTE 2: If a custom object's inactivate() logic decides
   * to enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeInactivate() with a boolean
   * indicating ultimate success of the operation.</p>
   *
   * <p>Finally, it is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object inactivation when
   * the transaction is committed..</p>
   *
   * @see #commitPhase1()
   * @see #commitPhase2() 
   */

  public ReturnVal inactivate()
  {
    return Ganymede.createErrorDialog("DBEditObject.inactivate() error",
				      "Base DBEditObject does not support inactivation");
  }

  /**
   * <p>This method is to be called by the custom DBEditObject inactivate()
   * logic when the inactivation is performed so that logging can be
   * done.</p>
   *
   * <p>If inactivation of an object causes the label to be null, this
   * won't work as well as we'd really like.</p>
   */

  final protected void finalizeInactivate(boolean success)
  {
    if (success)
      {
	Object val = getFieldValueLocal(SchemaConstants.RemovalField);

	if (val != null)
	  {
	    Vector invids = new Vector();
	    
	    invids.addElement(this.getInvid());
	
	    StringBuffer buffer = new StringBuffer();

	    buffer.append(getTypeDesc());
	    buffer.append(" ");
	    buffer.append(getLabel());
	    buffer.append(" has been inactivated.\n\nThe object is due to be removed from the database at ");
	    buffer.append(getFieldValueLocal(SchemaConstants.RemovalField).toString());
	    buffer.append(".");
	
	    editset.logEvents.addElement(new DBLogEvent("inactivateobject",
							buffer.toString(),
							(gSession.personaInvid == null ?
							 gSession.userInvid : gSession.personaInvid),
							gSession.username,
							invids,
							null));
	  }
	else
	  {
	    Vector invids = new Vector();
	    
	    invids.addElement(this.getInvid());
	
	    StringBuffer buffer = new StringBuffer();

	    buffer.append(getTypeDesc());
	    buffer.append(" ");
	    buffer.append(getLabel());
	    buffer.append(" has been inactivated.\n\nThe object has no removal date set.");
	
	    editset.logEvents.addElement(new DBLogEvent("inactivateobject",
							buffer.toString(),
							(gSession.personaInvid == null ?
							 gSession.userInvid : gSession.personaInvid),
							gSession.username,
							invids,
							null));
	  }

	editset.popCheckpoint("inactivate" + getLabel());
      }
    else
      {
	editset.rollback("inactivate" + getLabel());
      }
  }

  /**
   * <p>This method handles reactivation logic for this object type.  A
   * DBEditObject must first be checked out for editing, then the
   * reactivate() method can then be called on the object to make the
   * object active again.  reactivate() will clear the object's
   * removal date and fix up any other state information to reflect
   * the object's reactive status.</p>
   *
   * <p>reactive() is designed to run synchronously with the user's
   * request for inactivation.  It can return a wizard reference
   * in the ReturnVal object returned, to guide the user through
   * a set of interactive dialogs to reactive the object.</p>
   *
   * <p>If reactivate() returns a ReturnVal that has its success flag set to false
   * and does not include a {@link arlut.csd.JDataComponent.JDialogBuff JDialogBuff}
   * for further interaction with the
   * user, then 
   * {@link arlut.csd.ganymede.DBSession#inactivateDBObject(arlut.csd.ganymede.DBEditObject) inactivateDBObject()}
   * method will rollback any changes made by this method.</p>
   *
   * <p>IMPORTANT NOTE: If a custom object's inactivate() logic decides
   * to enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeInactivate() with a boolean
   * indicating ultimate success of the operation.</p>
   *
   * <p>Finally, it is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object reactivation when
   * the transaction is committed..</p>
   *
   * @see #commitPhase1()
   * @see #commitPhase2() 
   */

  public ReturnVal reactivate()
  {
    if (isInactivated())
      {
	// by default, we'll just clear the removal field

	setFieldValueLocal(SchemaConstants.RemovalField, null);
	return null;		// success
      }

    return Ganymede.createErrorDialog("DBEditObject.reactivate() error",
				      "Object not inactivated.");
  }

  /**
   * <p>This method is to be called by the custom DBEditObject reactivate()
   * logic when the reactivation is performed so that logging can be
   * done.</p>
   */

  final protected void finalizeReactivate(boolean success)
  {
    if (success)
      {
	Vector invids = new Vector();

	invids.addElement(this.getInvid());

	StringBuffer buffer = new StringBuffer();

	buffer.append(getTypeDesc());
	buffer.append(" ");
	buffer.append(getLabel());
	buffer.append(" has been reactivated.\n");
	
	editset.logEvents.addElement(new DBLogEvent("reactivateobject",
						    buffer.toString(),
						    (gSession.personaInvid == null ?
						     gSession.userInvid : gSession.personaInvid),
						    gSession.username,
						    invids,
						    null));
      }
    else
      {
	editset.rollback("reactivate" + getLabel());
      }
  }

  /**
   * <p>This method handles removal logic for this object type.  This method
   * will be called immediately from DBSession.deleteDBObject().</p>
   *
   * <p>The remove() method can cause other objects to be deleted, can cause
   * strings to be removed from fields in other objects, whatever.</p>
   *
   * <p>If remove() returns a ReturnVal that has its success flag set to false
   * and does not include a JDialogBuff for further interaction with the
   * user, the DBSession.deleteDBObject() method will roll back any changes
   * made by this method.</p>
   *
   * <p>remove() is intended for subclassing, whereas finalizeRemove() is
   * not.  finalizeRemove() provides the standard logic for wiping out
   * fields and what not to cause the object to be unlinked from
   * other objects.</p>
   *
   * <p>IMPORTANT NOTE: If a custom object's remove() logic decides to
   * enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeRemove() on the object when
   * it is determined that the object really should be removed,
   * with a boolean indicating whether success was had.</p>
   */

  public ReturnVal remove()
  {
    return null;
  }

  /**
   * <p>This method handles Ganymede-internal deletion logic for this
   * object type.  finalizeRemove() is responsible for dissolving any
   * invid inter-object references in particular.</p>
   *
   * <p>It is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object removal when
   * the transaction is committed..</p>
   *
   * <p>finalizeremove() returns a ReturnVal indicating whether the
   * internal removal bookkeeping was successful.  A failure result
   * will cause the DBSession to rollback the transaction to the state
   * prior to any removal actions for this object were
   * attempted.</p>
   *
   * <p>remove() is intended for subclassing, whereas finalizeRemove() is
   * not.  finalizeRemove() provides the standard logic for wiping out
   * fields and what not to cause the object to be unlinked from
   * other objects.</p>
   *
   * @param success If true, finalizeRemove() will clear all fields,
   * thereby unlinking invid fields and relinquishing namespace claims.
   * If false, finalizeRemove() will rollback to the state the system
   * was in before DBSession.deleteDBObject() was entered.
   *
   * @see #commitPhase1()
   * @see #commitPhase2() 
   */

  public final synchronized ReturnVal finalizeRemove(boolean success)
  {
    ReturnVal finalResult = new ReturnVal(true); // we use this to track rescan requests
    ReturnVal retVal = null;
    DBField field;
    Enumeration enum;
    DBSession session;
    String label = getLabel();	// remember the label before we clear it

    /* -- */

    if (!success)
      {
	editset.rollback("del" + label); // *sync*
	return Ganymede.createErrorDialog("Object Removal Failure",
					  "Could not delete object " + label +
					  ", custom code rejected this operation.");
      }

    // we want to delete / null out all fields.. this will take care
    // of invid links, embedded objects, and namespace allocations.
    
    // set the deleting flag to true so that our subclasses won't
    // freak about values being set to null.

    if (debug)
      {
	System.err.println("++ Attempting to delete object " + label);

	if (isEmbedded())
	  {
	    InvidDBField invf = (InvidDBField) getField(SchemaConstants.ContainerField);

	    if (invf == null)
	      {
		System.err.println("++ Argh, no container field in embedded!");
	      }
	    else
	      {
		System.err.println("++ We are embedded in object " + invf.getValueString());
	      }
	  }
      }

    this.deleting = true;

    try
      {
	enum = fields.elements();

	while (enum.hasMoreElements())
	  {
	    field = (DBField) enum.nextElement();

	    // we can't clear field 0 yet, since we need that
	    // for permissions verifications for other fields
	    
	    if (field.getID() == 0)
	      {
		continue;
	      }

	    if (field.isVector())
	      {
		if (debug)
		  {
		    System.err.println("++ Attempting to clear vector field " + field.getName());
		  }

		try
		  {
		    if (field.getID() == SchemaConstants.BackLinksField)
		      {
			// let the InvidDBField code know that it doesn't need
			// to wrap unbindAll() with its own checkpoint/rollback.

			clearingBackLinks = true;
		      }

		    while (field.size() > 0)
		      {
			// if this is an InvidDBField, deleteElement()
			// will convert this request into a deletion of
			// the embedded object.
			
			retVal = field.deleteElement(0); // *sync*
			
			if (retVal != null && !retVal.didSucceed())
			  {
			    session = editset.getSession();
			    
			    if (session != null)
			      {
				session.setLastError("DBEditObject disapproved of deleting element from field " + 
						     field.getName());
			      }
			    
			    editset.rollback("del" + label); // *sync*
			    
			    return Ganymede.createErrorDialog("Server: Error in DBEditObject.finalizeRemove()",
							      "DBEditObject disapproved of deleting element from field " + 
							      field.getName());
			  }
			else
			  {
			    finalResult.unionRescan(retVal);
			  }
		      }
		  }
		finally
		  {
		    clearingBackLinks = false;
		  }
	      }
	    else
	      {
		// permission matrices and passwords don't allow us to
		// call set value directly.  We're mainly concerned
		// with invid's (for linking), i.p. addresses and
		// strings (for the namespace) here anyway.

		if (debug)
		  {
		    System.err.println("++ Attempting to clear scalar field " + field.getName());
		  }

		if (field.getType() != PERMISSIONMATRIX &&
		    field.getType() != PASSWORD)
		  {
		    retVal = field.setValueLocal(null); // *sync*

		    if (retVal != null && !retVal.didSucceed())
		      {
			session = editset.getSession();
		    
			if (session != null)
			  {
			    session.setLastError("DBEditObject could not clear field " + 
						 field.getName());
			  }

			editset.rollback("del" + label); // *sync*

			return Ganymede.createErrorDialog("Server: Error in DBEditObject.finalizeRemove()",
							  "DBEditObject could not clear field " + 
							  field.getName());
		      }
		    else
		      {
			finalResult.unionRescan(retVal);
		      }
		  }
		else
		  {
		    // catchall for permission matrix and password
		    // fields, which do this their own way.

		    field.setUndefined(true);
		  }
	      }
	  }

	// ok, we've cleared all fields but field 0.. clear that to finish up.

	field = (DBField) getField((short) 0);

	if (field != null)
	  {
	    if (field.isVector())
	      {
		// if we're deleting elements out of vector field 0 (the list
		// of owner groups), we'll want to deleteElementLocal.. this
		// will simplify things and will prevent us from losing permission
		// to write to this field in midstream (although the new DBField
		// permCache would actually obviate this problem as well).

		while (field.size() > 0)
		  {
		    retVal = field.deleteElementLocal(0); // *sync*

		    if (retVal != null && !retVal.didSucceed())
		      {
			session = editset.getSession();
		    
			if (session != null)
			  {
			    session.setLastError("DBEditObject disapproved of deleting element from field " + 
						 field.getName());
			  }

			editset.rollback("del" + label); // *sync*

			return Ganymede.createErrorDialog("Server: Error in DBEditObject.finalizeRemove()",
							  "DBEditObject disapproved of deleting element from field " + 
							  field.getName());
		      }
		    else
		      {
			finalResult.unionRescan(retVal);
		      }
		  }
	      }
	    else
	      {
		// scalar field 0 is the ContainerField for an embedded
		// object.  Note that setting this field to null will
		// not unlink us from the the container object, since
		// the ContainerField pointer is a generic one.

		retVal = field.setValueLocal(null); // *sync*

		if (retVal != null && !retVal.didSucceed())
		  {
		    session = editset.getSession();
		    
		    if (session != null)
		      {
			session.setLastError("DBEditObject could not clear field " + 
					     field.getName());
		      }

		    editset.rollback("del" + label); // *sync*

		    return Ganymede.createErrorDialog("Server: Error in DBEditObject.finalizeRemove()",
						      "DBEditObject could not clear field " + 
						      field.getName());
		  }
	      }
	  }

	// ok, we should be successful if we get here.  log the object deletion.

	Vector invids = new Vector();
	invids.addElement(this.getInvid());

	editset.logEvents.addElement(new DBLogEvent("deleteobject",
						    getTypeDesc() + ":" + label,
						    (gSession.personaInvid == null ?
						     gSession.userInvid : gSession.personaInvid),
						    gSession.username,
						    invids,
						    null));

	return finalResult;
      }
    finally
      {
	// make sure we clear deleting before we return
	
	deleting = false;
      }
  }

  /**
   * <p>This method performs verification for the first phase of
   * the two-phase commit algorithm.  If this object returns
   * true from commitPhase1() when called during an editSet's
   * commit() routine, this object CAN NOT refuse commit()
   * at a subsequent point.  Once commitPhase1() is called,
   * the object CAN NOT be changed until the transaction
   * is either fully committed or abandoned.</p>
   *
   * <p>This method is intended to be subclassed by application
   * objects that need to include extra-Ganymede processes
   * in the two-phase commit protocol.  If a particular
   * subclass of DBEditObject does not need to involve outside
   * processes in the full two-phase commit protocol, this
   * method should not be overridden.</p>
   *
   * <p>If this method is overridden, be sure and set this.committing to
   * true before doing anything else.  Failure to set committing to
   * true in this method will cause the two phase commit mechanism to
   * behave unpredictably.</p>
   *
   * @see arlut.csd.ganymede.DBEditSet 
   */

  public synchronized ReturnVal commitPhase1()
  {
    committing = true;
    return consistencyCheck(this);
  }

  /**
   * <p>This method returns true if this object has already gone
   * through phase 1 of the commit process, which requires
   * the DBEditObject not to accept further changes.</p>
   */

  public final boolean isCommitting()
  {
    return committing;
  }

  /**
   * <p>This method is a hook for subclasses to override to
   * pass the phase-two commit command to external processes.</p>
   *
   * <p>For normal usage this method would not be overridden.  For
   * cases in which change to an object would result in an external
   * process being initiated whose <b>success or failure would not
   * affect the successful commit of this DBEditObject in the
   * Ganymede server</b>, the process invokation should be placed here,
   * rather than in commitPhase1().</p>
   *
   * <p>Subclasses that override this method may wish to make this method 
   * synchronized.</p>
   *
   * @see arlut.csd.ganymede.DBEditSet
   */

  public void commitPhase2()
  {
    return;
  }

  /**  
   * <p>A hook for subclasses to use to clean up any external
   * resources allocated for this object.  This method can be called
   * after commitPhase1() has been called, or it may be called at any
   * time to indicate that this object is being withdrawn from the
   * transaction (as by a checkpoint rollback).  This method <b>will
   * not</b> be called after commitPhase2() has been called.  If you
   * do anything external in commitPhase2(), make sure that all
   * resources allocated for this object (at any time in this object's
   * editing life-cycle) are released before commitPhase2()
   * completes.</p>
   *
   * <p>Ordinarily, there is no need for customizers to override this
   * method.  The only reason to override the release method is if you
   * need to do maintenance on external data structures or connections
   * that were created in commitPhase1() or when this DBEditObject was
   * created.</p>
   *
   * <p>If &lt;finalAbort&gt; is true, the transaction for which this
   * DBEditObject was created is being completely abandoned (if
   * isCommitting() returns true), or this object is being dropped out
   * of the transaction by a checkpoint rollback.  In either case, a
   * customizer may want to clean up all external structures or
   * connections that were created either at the time this
   * DBEditObject was created/checked-out and/or that were created by
   * commitPhase1().</p>
   *
   * <p>If &lt;finalAbort&gt; is false, isCommitting() should always
   * return true.  In this case, one of the DBEditObjects in the
   * transaction returned false from a later commitPhase1() call
   * and all objects that had their commitPhase1() methods called
   * previously will be revisted and release(false) will be called
   * on them.  Customizers may want to clean up any external structures
   * or connections that were established in commitPhase1().</p>
   *
   * <p>Remember, you will usually want to perform external actions in
   * commitPhase2(), in which case release() is not needed.  release()
   * is only useful when you allocate external structures or
   * connections when the object is created or during commitPhase1().</p>
   *
   * <p>It is safe to call release() from your commitPhase2() method
   * if you wish to have one place to clean up structures allocated by
   * initializeNewObject() or commitPhase1().</p>
   *
   * <p>Customizers subclassing this method may want to keep a couple of
   * things in mind.  First, the default release method is not synchronized,
   * and it basically just clear a boolean flag (this.committing) to
   * indicate that edit methods on this object may once again go forward.
   * You may want to synchronize your release method if you do anything
   * at all fancy.  More importantly, it is essential that you clear this.committing
   * if &lt;finalAbort&gt; is false so that this object can be edited afterwards.</p>
   *
   * @param finalAbort If true, this object is being dropped, either due to an
   * aborted transaction or a checkpoint rollback.  
   */

  public void release(boolean finalAbort)
  {
    if (!finalAbort)
      {
	this.committing = false;
      }
  }

  // ***
  //
  // Checkpoint / Rollback support
  //
  // ***

  synchronized final Hashtable checkpoint()
  {
    Enumeration enum;
    Object key, value;
    Hashtable result = new Hashtable();
    DBField field;

    /* -- */

    enum = fields.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();
	key = new Short(field.getID());
	value = field.checkpoint();

	if (value != null)
	  {
	    result.put(key, value);
	  }
	else
	  {
	    // hack, hack.. we're using a reference
	    // to this object to represent a null value

	    result.put(key, this);
	  }
      }

    return result;
  }

  synchronized final void rollback(Hashtable ckpoint)
  {
    Enumeration enum;
    Short key;
    Object value;
    Hashtable result = new Hashtable();
    DBField field;

    /* -- */

    enum = ckpoint.keys();

    while (enum.hasMoreElements())
      {
	key = (Short) enum.nextElement();

	field = fields.get(key.shortValue());

	value = ckpoint.get(key);

	// again, we use a reference to ourselves as a
	// hackish way of representing null in the
	// hashtable

	if (value == this)
	  {
	    field.rollback(null);
	  }
	else
	  {
	    field.rollback(value);
	  }
      }
  }

  /**
   * <p>This method is used to generate a String describing the difference
   * between the current state of the DBEditObject and the original
   * object's state.</p>
   *
   * <p>This method can also be used if this object is newly created.. in
   * this case, it will just return a string containing many 'FieldAdded'
   * entries.</p>
   *
   * @return null if no difference was found
   */

  public synchronized String diff()
  {
    boolean diffFound = false;
    StringBuffer result = new StringBuffer();
    DBObjectBaseField fieldDef;
    DBField origField, currentField;
    StringBuffer added = new StringBuffer();
    StringBuffer deleted = new StringBuffer();
    StringBuffer changed = new StringBuffer();

    /* -- */

    // algorithm: iterate over base.sortedFields to find all fields
    // possibly contained in the object.. for each field, check to
    // see if the value has changed.  if so, emit a before and after
    // diff.  if one has a field and the other does not, indicate
    // the change.
    //
    // in the case of vectors, the change description can be a simple
    // delta (x added, y removed)

    // note that we're counting on objectBase.sortedFields not being
    // changed while we're iterating here.. this is an ok assumption,
    // since only the loader and the schema editor will trigger changes
    // in sortedFields.
    
    if (debug)
      {
	System.err.println("Entering diff for object " + getLabel());
      }

    Enumeration enum = objectBase.sortedFields.elements();

    while (enum.hasMoreElements())
      {
	fieldDef = (DBObjectBaseField) enum.nextElement();

	// we don't care if certain fields change

	if (fieldDef.getID() == SchemaConstants.CreationDateField ||
	    fieldDef.getID() == SchemaConstants.CreatorField ||
	    fieldDef.getID() == SchemaConstants.ModificationDateField ||
	    fieldDef.getID() == SchemaConstants.ModifierField)
	  {
	    continue;
	  }

	if (debug)
	  {
	    System.err.println("Comparing field " + fieldDef.getName());
	  }

	// if we're newly created, we'll just treat the old field as
	// non-existent.

	if (original == null)
	  {
	    origField = null;
	  }
	else
	  {
	    origField = (DBField) original.getField(fieldDef.getID());
	  }

	currentField = (DBField) this.getField(fieldDef.getID());

	if ((origField == null || !origField.isDefined()) && 
	    (currentField == null || !currentField.isDefined()))
	  {
	    continue;
	  }

	if (((origField == null) || !origField.isDefined()) &&
	    ((currentField != null) && currentField.isDefined()))
	  {
	    added.append("\t");
	    added.append(fieldDef.getName());
	    added.append(":");
	    added.append(currentField.getValueString());
	    added.append("\n");

	    diffFound = true;

	    if (debug)
	      {
		System.err.println("Field added: " + fieldDef.getName() + "\nValue: " +
				   currentField.getValueString() + "\n");
	      }
	  }
	else if (((currentField == null) || !currentField.isDefined()) &&
		 ((origField != null) && origField.isDefined()))

	  {
	    deleted.append("\t");
	    deleted.append(fieldDef.getName());
	    deleted.append(":");
	    deleted.append(origField.getValueString());
	    deleted.append("\n");

	    diffFound = true;

	    if (debug)
	      {
		System.err.println("Field deleted: " + fieldDef.getName() + "\nValue: " +
				   origField.getValueString() + "\n");
	      }
	  }
	else
	  {
	    String diff = currentField.getDiffString(origField);

	    if (diff != null)
	      {
		changed.append(fieldDef.getName());
		changed.append("\n");
		changed.append(diff);

		diffFound = true;

		if (debug)
		  {
		    System.err.println("Field changed: " + 
				       fieldDef.getName() + "\n" +
				       diff);
		  }
	      }
	  }
      }

    if (diffFound)
      {
	if (added.length() > 0)
	  {
	    result.append("Fields Added:\n\n");
	    result.append(added);
	    result.append("\n");
	  }

	if (changed.length() > 0)
	  {
	    result.append("Fields changed:\n\n");
	    result.append(changed);
	    result.append("\n");
	  }

	if (deleted.length() > 0)
	  {
	    result.append("Fields Deleted:\n\n");
	    result.append(deleted);
	    result.append("\n");
	  }

	return result.toString();
      }
    else
      {
	return null;
      }
  }

  /*----------------------------------------------------------

    Convenience methods for our customization subclasses

  ----------------------------------------------------------*/

  protected final GanymedeSession internalSession()
  {
    return Ganymede.internalSession;
  }
}
