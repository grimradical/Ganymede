/*

   personCustom.java

   This file is a management class for person objects in Ganymede.
   
   Created: 25 March 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    personCustom

------------------------------------------------------------------------------*/

public class personCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;

  // ---

  /**
   *
   * This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * interface's name interposed.
   *
   */

  /**
   *
   * Customization Constructor
   *
   */

  public personCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public personCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public personCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }
  
  /**
   *
   * Hook to allow intelligent generation of labels for DBObjects
   * of this type.  Subclasses of DBEditObject should override
   * this method to provide for custom generation of the
   * object's label type
   *
   */

  public String getLabelHook(DBObject object)
  {
    String lastname, firstname;

    /* -- */

    if ((object == null) || (object.getTypeID() != getTypeID()))
      {
	return null;
      }

    lastname = (String) object.getFieldValueLocal(personSchema.LASTNAME);
    firstname = (String) object.getFieldValueLocal(personSchema.FIRSTNAME);

    return lastname + ", " + firstname;
  }
}
