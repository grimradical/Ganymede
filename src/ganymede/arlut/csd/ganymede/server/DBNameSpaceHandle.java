/*
   GASH 2

   DBNameSpaceHandle.java

   The GANYMEDE object storage system.

   Created: 15 January 1999
   Version: $Revision$
   Last Mod Date: $Date$
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import arlut.csd.ganymede.common.Invid;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBNameSpaceHandle

------------------------------------------------------------------------------*/

/**
 * <p>This class is intended to be the targets of elements of a name
 * space's unique value hash.  The fields in this class are used to
 * keep track of who currently 'owns' a given value, and whether or not
 * there is actually any field in the namespace that really contains
 * that value.</p>
 *
 * <p>This class will be manipulated by the DBNameSpace class and by the
 * DBEditObject class.</p>
 */

class DBNameSpaceHandle implements Cloneable {

  /**
   * if this value is currently being shuffled
   * by a transaction, this is the transaction
   */

  DBEditSet owner;

  /**
   * remember if the value was in use at the
   * start of the transaction
   */

  boolean original;

  /**
   * is the value currently in use?
   */

  boolean inuse;

  /**
   * <P>so the namespace hash can be used as an index fieldInvid always
   * points to the object that contained the field that contained this
   * value at the time this field was last committed in a transaction.</P>
   *
   * <P>fieldInvid will be null if the value pointing to this handle
   * has not been committed into the database outside of an active
   * transaction.</P>
   */

  private Invid fieldInvid;

  /**
   * <P>If this handle is associated with a value that has been
   * checked into the database, fieldId will be the field number for
   * the field that holds that value in the database, within the
   * object referenced by fieldInvid.</P>
   */

  private short fieldId;

  /**
   * if this handle is currently being edited by an editset,
   * shadowField points to the field in the transaction that contains
   * this value.  If the transaction is committed, the DBField pointer
   * in shadowField will be transferred to field.  If this value is
   * not being manipulated by a transaction, shadowField will be equal
   * to null.
   */

  DBField shadowField;

  /* -- */

  public DBNameSpaceHandle(DBEditSet owner, boolean originalValue)
  {
    this.owner = owner;
    this.original = this.inuse = originalValue;
  }

  public DBNameSpaceHandle(DBEditSet owner, boolean originalValue, DBField field)
  {
    this.owner = owner;
    this.original = this.inuse = originalValue;

    setPersistentField(field);
  }

  public boolean matches(DBEditSet set)
  {
    return (this.owner == set);
  }

  /**
   * <p>This method returns true if the namespace-managed value that
   * this handle is associated with is held in a committed object in the
   * Ganymede data store.</p>
   *
   * <p>If this method returns false, that means that this handle must
   * be associated with a field in an active DBEditSet's transaction
   * set, or else we wouldn't have a handle for it.</p>
   */

  public boolean isPersisted()
  {
    return fieldInvid != null;
  }

  /**
   * <p>This method associates this value with a DBField that is
   * persisted (or will be persisted?) in the Ganymede persistent
   * store.</p>
   */

  public void setPersistentField(DBField field)
  {
    if (field != null)
      {
	fieldInvid = field.getOwner().getInvid();
	fieldId = field.getID();
      }
    else
      {
	fieldInvid = null;
	fieldId = -1;
      }
  }

  /**
   * <p>If the value that this handle is associated with is stored in the Ganymede server's
   * persistent data store (i.e., that this handle is associated with a field in an
   * already-committed object), this method will return a pointer to the DBField that
   * contains this handle's value in the committed data store.</p>
   */

  public DBField getPersistentField(GanymedeSession session)
  {
    if (fieldInvid == null)
      {
	return null;
      }

    if (session != null)
      {
	DBObject _obj = session.session.viewDBObject(fieldInvid);
	
	return (DBField) _obj.getField(fieldId);
      }
    else
      {
	// during start-up, before we have a session available

	DBObjectBase _base = Ganymede.db.getObjectBase(fieldInvid.getType());

	if (_base == null)
	  {
	    return null;
	  }

	DBObject _obj = _base.getObject(fieldInvid);

	if (_obj == null)
	  {
	    return null;
	  }

	return (DBField) _obj.getField(fieldId);
      }
  }

  /**
   * <p>This method returns true if the namespace-constrained value
   * controlled by this handle is being edited by the GanymedeSession
   * provided.</p>
   */

  public boolean isEditedByUs(GanymedeSession session)
  {
    return (owner != null && session.getSession().getEditSet() == owner);
  }

  /**
   * <p>This method returns true if the namespace-constrained value
   * controlled by this handle is being edited by the transaction
   * provided.</p>
   */

  public boolean isEditedByUs(DBEditSet editSet)
  {
    return (owner != null && editSet == owner);
  }

  /**
   * <p>If this namespace-managed value is being edited in an active
   * Ganymede transaction, this method may be used to set a pointer to
   * the editable DBField which contains the constrained value in the
   * active transaction.</p>
   */

  public void setShadowField(DBField newShadow)
  {
    shadowField = newShadow;
  }

  /**
   * <p>If this namespace-managed value is being edited in an active
   * Ganymede transaction, this method will return a pointer to the
   * editable DBField which contains the constrained value in the active
   * transaction.</p>
   */

  public DBField getShadowField()
  {
    return shadowField;
  }


  public Object clone()
  {
    // we should be clonable

    try
      {
	return super.clone();
      }
    catch (CloneNotSupportedException ex)
      {
	throw new RuntimeException(ex.getMessage());
      }
  }

  /**
   * <p>This method is used to verify that this handle points to the same
   * field as the one specified by the parameter list.</p>
   */

  public boolean matches(Invid fieldInvid, short fieldId)
  {
    return (this.fieldInvid == fieldInvid) && (this.fieldId == fieldId);
  }

  /**
   * <p>This method is used to verify that this handle points to the same
   * kind of field as the one specified by the parameter list.</p>
   */

  public boolean matches (short objectType, short fieldId)
  {
    return (this.fieldInvid.getType() == objectType) &&
      (this.fieldId == fieldId);
  }

  public void cleanup()
  {
    owner = null;
    fieldInvid = null;
    shadowField = null;
  }
}
