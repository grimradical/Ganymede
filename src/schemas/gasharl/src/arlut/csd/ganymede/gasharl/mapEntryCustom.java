/*

   mapEntryCustom.java

   This file is a management class for Automounter map entry objects in Ganymede.
   
   Created: 9 December 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.gasharl;

import java.util.Vector;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBEditSet;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.DBObjectBase;
import arlut.csd.ganymede.server.DBSession;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.InvidDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  mapEntryCustom

------------------------------------------------------------------------------*/

/**
 *
 * This class represents the automounter entry objects that are
 * embedded in user objects in the Ganymede schema.
 *
 */

public class mapEntryCustom extends DBEditObject implements SchemaConstants, mapEntrySchema {

  static PermEntry noEditPerm = new PermEntry(true, false, false, false);

  // ---

  /**
   *
   * Customization Constructor
   *
   */

  public mapEntryCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public mapEntryCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public mapEntryCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
  }

  /**
   *
   * Customization method to allow this Ganymede object type to
   * override the default permissions mechanism for special
   * purposes.<br><br>
   *
   * If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the permissions specified by this method for access to the
   * given field, and no further elaboration of the permission
   * will be performed.  Note that this override capability does
   * not apply to operations performed in supergash mode.<br><br>
   *
   * This method should be used very sparingly.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public PermEntry permOverride(GanymedeSession session, DBObject object, short fieldid)
  {
    if (fieldid != mapEntrySchema.MAP)
      {
	return null;
      }

    DBField field = (DBField) object.getField(fieldid);

    String label = field.getValueString();

    // XXX Note: this schema assumes that all users will have entries in auto.home.default

    if (label.equals("auto.home.default"))
      {
	return noEditPerm;
      }

    return null;
  }

  /**
   *
   * Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
      case mapEntrySchema.MAP:
      case mapEntrySchema.VOLUME:
	return true;
      }

    return false;
  }

  /**
   *
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.<br><br>
   *
   * If there is no caching key, this method will return null.
   *
   */

  public Object obtainChoicesKey(DBField field)
  {
    if (field.getID() == mapEntrySchema.MAP)
      {
	return null;
      }

    return super.obtainChoicesKey(field);
  }

  /**
   *
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.<br><br>
   *
   * This method will provide a reasonable default for targetted
   * invid fields.
   * 
   */

  public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
  {
    if (field.getID() != mapEntrySchema.MAP)
      {
	return super.obtainChoiceList(field);
      }

    InvidDBField invf = (InvidDBField) getField(mapEntrySchema.MAP);

    if (invf.getValueString().equals("auto.home.default"))
      {
	return new QueryResult(); // can't change a map reference set to auto.home.default
      }

    // ok, we are returning the list of choices for what
    // map to put this entry into.  We don't want it
    // to include any maps that already have an entry
    // for this user.

    // first, get the list of map entries contained in this
    // object.

    Vector mapsToSkip = new Vector();
    DBObject entry;
    Invid mapInvid;

    Vector entries = getSiblingInvids();

    for (int i = 0; i < entries.size(); i++)
      {
	entry = getSession().viewDBObject((Invid) entries.elementAt(i));

	mapInvid = (Invid) entry.getFieldValueLocal(mapEntrySchema.MAP);

	mapsToSkip.addElement(mapInvid);
      }
    
    // ok, mapsToSkip has a list of invid's to skip in our choice list.

    QueryResult result = new QueryResult();
    QueryResult baseList = super.obtainChoiceList(field);

    for (int i = 0; i < baseList.size(); i++)
      {
	mapInvid = baseList.getInvid(i);

	if (!mapsToSkip.contains(mapInvid))
	  {
	    result.addRow(baseList.getObjectHandle(i));
	  }
      }
    
    return result;
  }

  /**
   *
   * This is the hook that DBEditObject subclasses use to interpose wizards when
   * a field's value is being changed.
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    if ((field.getID() != mapEntrySchema.MAP) ||
	(operation != DBEditObject.SETVAL))
      {
	return null;		// by default, we just ok whatever
      }

    InvidDBField invf = (InvidDBField) getField(mapEntrySchema.MAP);

    // if we aren't deleting this entry, reject any attempt to unlink
    // us from auto.home.default, if we are linked there.

    if (!isDeleting() && invf.getValueString().equals("auto.home.default"))
      {
	return Ganymede.createErrorDialog("Error, auto.home.default is required",
					  "Sorry, it is mandatory to have a directory entry on the auto.home.default map.");
      }
  
    // ok, we want to go ahead and approve the operation, but we want
    // to cause the client to rescan the MAP field in all of our
    // siblings so that their choice list gets updated to not show
    // whatever map *we* just chose.

    // Create a ReturnVal which will cause the field.setValue() call
    // which triggered us to continue normal processing, and return
    // our list of rescan preferences to the client.

    ReturnVal result = new ReturnVal(true, true);
    Vector entries = getSiblingInvids();

    for (int i = 0; i < entries.size(); i++)
      {
	result.addRescanField((Invid) entries.elementAt(i), mapEntrySchema.MAP);
      }

    return result;
  }

  /**
   * If this DBEditObject is managing an embedded object, the
   * getEmbeddedObjectLabel() can be overridden to display a synthetic
   * label in the context of viewing or editing the containing object,
   * and when doing queries on the containing type.
   *
   * The getLabel() method will not consult this hook, however, and
   * embedded objects will be represented with their unique label
   * field when processed in an XML context.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public final String getEmbeddedObjectDisplayLabelHook(DBObject object)
  {
    InvidDBField field, field2;
    StringBuilder buff = new StringBuilder();

    /* -- */

    if ((object == null) || (object.getTypeID() != getTypeID()))
      {
	return null;
      }

    field = (InvidDBField) object.getField(MAP); // map name
    field2 = (InvidDBField) object.getField(VOLUME); // volume

    try
      {
	if (field != null)
	  {
	    buff.append(field.getValueString() + ":");
	  }

	if (field2 != null)
	  {
	    buff.append(field2.getValueString());
	  }
      }
    catch (IllegalArgumentException ex)
      {
	buff.append("<?:?>");
      }

    return buff.toString();
  }

  /**
   *
   * Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of DBField will
   * wind up calling up to here to let us override the normal visibility
   * process.<br><br>
   *
   * Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.<br><br>
   *
   * If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   * 
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    // don't show off our hidden label for direct editing or viewing

    if (field.getID() == mapEntrySchema.XMLLABEL)
      {
	return false;
      }

    return super.canSeeField(session, field);
  }

  private Vector getSiblingInvids()
  {
    Vector result;
    Invid userInvid = (Invid) getFieldValueLocal(mapEntrySchema.CONTAININGUSER);
    DBObject user = getSession().viewDBObject(userInvid);

    result = (Vector) user.getFieldValuesLocal(userSchema.VOLUMES).clone();
    
    // we are not our own sibling.

    result.removeElement(getInvid());

    return result;
  }

  String getMapName()
  {
    return getGSession().viewObjectLabel((Invid) getFieldValueLocal(mapEntrySchema.MAP));
  }

  String getOriginalMapName()
  {
    return getGSession().viewObjectLabel((Invid) getOriginal().getFieldValueLocal(mapEntrySchema.MAP));
  }

  Invid getMapInvid()
  {
    return (Invid) getFieldValueLocal(mapEntrySchema.MAP);
  }

  Invid getOriginalMapInvid()
  {
    return (Invid) getOriginal().getFieldValueLocal(mapEntrySchema.MAP);
  }

  String getVolumeName()
  {
    return getGSession().viewObjectLabel((Invid) getFieldValueLocal(mapEntrySchema.VOLUME));
  }

  String getOriginalVolumeName()
  {
    return getGSession().viewObjectLabel((Invid) getOriginal().getFieldValueLocal(mapEntrySchema.VOLUME));
  }

  Invid getVolumeInvid()
  {
    return (Invid) getFieldValueLocal(mapEntrySchema.VOLUME);
  }

  Invid getOriginalVolumeInvid()
  {
    return (Invid) getOriginal().getFieldValueLocal(mapEntrySchema.VOLUME);
  }

}
