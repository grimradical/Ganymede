/*

   Base.java

   Client side interface to the object type dictionary
   
   Created: 17 April 1997
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

package arlut.csd.ddroid.rmi;

import arlut.csd.ddroid.common.*;

import java.rmi.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                            Base

------------------------------------------------------------------------------*/

/**
 * <P>Client side interface definition for the Ganymede
 * {@link arlut.csd.ddroid.server.DBObjectBase DBObjectBase} class.  This
 * interface allows the client to query type information remotely, and allows
 * the schema editor in the admin console to remotely edit object type information.</P>
 *
 * <P>The {@link arlut.csd.ddroid.rmi.Category Category} interface is also vital to
 * the client and schema editor's work with object types.</P>
 *
 * @version $Id$
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public interface Base extends CategoryNode, Remote {

  /**
   * <p>This method indicates whether this base may be removed in
   * the Schema Editor.</p>
   *
   * <p>We don't allow removal of built-in Bases that the server
   * depends on for its operation, such as permissions, notification,
   * and logging object types.</p>
   */

  public boolean isRemovable() throws RemoteException;

  /**
   * This method returns true if this object base is for
   * an embedded object.  Embedded objects do not have
   * their own expiration and removal dates, do not have
   * history trails, and can be only owned by a single
   * object, not by a list of administrators.
   */

  public boolean isEmbedded() throws RemoteException;

  /**
   * Returns the name of this object type.  Guaranteed
   * to be unique in the Directory Droid server.
   */

  public String getName() throws RemoteException;

  /**
   * Returns the name and category path of this object type.
   * Guaranteed to be unique in the Directory Droid server.
   */

  public String getPath() throws RemoteException;

  /**
   * Returns the name of the class managing this object type
   */

  public String getClassName() throws RemoteException;

  /**
   * Returns the invid type id for this object definition
   */

  public short getTypeID() throws RemoteException;

  /**
   * Returns the short type id for the field designated as this object's
   * primary label field, if any.  Objects do not need to have a primary
   * label field designated if labels for this object type are dynamically
   * generated.
   */

  public short getLabelField() throws RemoteException;

  /**
   * Returns the field name for the field designated as this object's
   * primary label field.  null is returned if no label has been
   * designated.
   */

  public String getLabelFieldName() throws RemoteException;

  /**
   * <p>Returns {@link arlut.csd.ddroid.rmi.BaseField BaseField}
   * base field definitions for objects of this type.
   *
   * <P>If includeBuiltIns is false, the fields returned will be the
   * custom fields defined for this object type, and they will be
   * returned in display order.  If includeBuiltIns is true, the
   * built-in fields will be appended to the Vector after the custom
   * types, in random order.</P>
   */

  public Vector getFields(boolean includeBuiltIns) throws RemoteException;

  /**
   * <p>This method returns a list of all
   * {@link arlut.csd.ddroid.rmi.BaseField BaseField} references for the
   * fields defined by this object type, in random order.</p>
   */

  public Vector getFields() throws RemoteException;

  /**
   * Returns the field definition for the field matching id,
   * or null if no match found.
   */

  public BaseField getField(short id) throws RemoteException;

  /**
   * Returns the field definition for the field matching name,
   * or null if no match found.
   */

  public BaseField getField(String name) throws RemoteException;

  /**
   * Returns true if the current session is permitted to
   * create an object of this type.
   */

  public boolean canCreate(Session session) throws RemoteException;

  /**
   *
   * Returns true if this object type can be inactivated.  Not all
   * object types will have inactivation protocols defined in the
   * server.  Those that do not can just be deleted.
   *
   * @see arlut.csd.ddroid.rmi.Base
   */

  public boolean canInactivate() throws RemoteException;

  /**
   * <p>Sets the name for this object type</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ddroid.rmi.SchemaEdit SchemaEdit} reference
   * by the Directory Droid schema editor.</p>
   */

  public ReturnVal setName(String newName) throws RemoteException;

  /**
   * <p>Sets the fully qualified classname of the class 
   * managing this object type</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ddroid.rmi.SchemaEdit SchemaEdit} reference
   * by the Directory Droid schema editor.</p>
   */

  public ReturnVal setClassName(String newName) throws RemoteException;

  /**
   * <p>This method is used to adjust the ordering of a custom field
   * in this Base.</p>
   *
   * @param fieldName The name of the field to move
   * @param previousFieldName The name of the field that fieldName is going to
   * be put after, or null if fieldName is to be the first field displayed
   * in this object type.
   */

  public ReturnVal moveFieldAfter(String fieldName, String previousFieldName) throws RemoteException;

  /**
   * <p>This method is used to adjust the ordering of a custom field
   * in this Base.</p>
   *
   * @param fieldName The name of the field to move
   * @param nextFieldName The name of the field that fieldName is going to
   * be put before, or null if fieldName is to be the last field displayed
   * in this object type.
   */

  public ReturnVal moveFieldBefore(String fieldName, String nextFieldName) throws RemoteException;

  /**
   * <p>Choose what field will serve as this objectBase's label.  A fieldName
   * parameter of null will cause the object's label field to be undefined,
   * in which case the object will have to generate its own label using the
   * {@link arlut.csd.ddroid.server.DBEditObject#getLabelHook(arlut.csd.ddroid.server.DBObject) getLabelHook()}
   * method.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ddroid.rmi.SchemaEdit SchemaEdit} reference
   * by the Directory Droid schema editor.</p>
   */

  public ReturnVal setLabelField(String fieldName) throws RemoteException;

  /**
   * <p>Choose what field will serve as this objectBase's label.  A fieldID
   * parameter of -1 will cause the object's label field to be undefined,
   * in which case the object will have to generate its own label using the
   * {@link arlut.csd.ddroid.server.DBEditObject#getLabelHook(arlut.csd.ddroid.server.DBObject) getLabelHook()}
   * method.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ddroid.rmi.SchemaEdit SchemaEdit} reference
   * by the Directory Droid schema editor.</p>
   */

  public ReturnVal setLabelField(short fieldID) throws RemoteException;

  /**
   * <p>Get the parent Category for this object type.  This is used by the
   * Directory Droid client and schema editor to present object types in
   * a hierarchical tree.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ddroid.rmi.SchemaEdit SchemaEdit} reference
   * by the Directory Droid schema editor.</p>
   */
 
  public Category getCategory() throws RemoteException;

  /**
   * <p>Creates a new base field and inserts it
   * into the DBObjectBase field definitions hash.  The newly created
   * field will the first short id code value available at or above
   * 256.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ddroid.rmi.SchemaEdit SchemaEdit} reference
   * by the Directory Droid schema editor.</p>
   */

  public BaseField createNewField() throws RemoteException;

  /**
   * <p>This method is used to remove a field definition from 
   * the current schema.</p>
   *
   * <p>Of course, this removal will only take effect if
   * the schema editor commits.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ddroid.rmi.SchemaEdit SchemaEdit} reference
   * by the Directory Droid schema editor.</p>
   *
   * @see arlut.csd.ddroid.rmi.Base
   */

  public ReturnVal deleteField(String fieldName) throws RemoteException;

  /**
   * This method is used by the SchemaEditor to detect whether any
   * objects are using a field definition.  If any objects of this
   * type in the database have this field defined, this method
   * will return true.
   *
   * @see arlut.csd.ddroid.rmi.Base
   */

  public boolean fieldInUse(String fieldName) throws RemoteException;
}