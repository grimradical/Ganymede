/*

   perm_field.java

   Client-side interface to the PermissionMatrixDBField class.
   
   Created: 27 June 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996 - 2004
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

package arlut.csd.ddroid.rmi;

import arlut.csd.ddroid.common.*;

import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                      perm_field

------------------------------------------------------------------------------*/

/**
 * <P>Client-side remote interface to the {@link
 * arlut.csd.ddroid.server.PermissionMatrixDBField PermissionMatrixDBField}
 * class.</P> 
 */

public interface perm_field extends db_field {

  /**
   *
   * Return a serializable, read-only copy of this field's permission
   * matrix
   *
   */

  public PermMatrix getMatrix() throws RemoteException;

  /**
   * <P>Return a serializable, read-only copy of the maximum permissions
   * that can be set for this field's permission matrix.  This matrix
   * is drawn from the union of delegatable roles that the client's
   * adminPersona is a member of.</P>
   * 
   * <P>This method will return null if this perm_field is not associated
   * with an object that is being edited, or if the client is logged
   * into the server as supergash.</P>
   */

  public PermMatrix getTemplateMatrix() throws RemoteException;

  /**
   * <P>Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field &lt;field&gt; in base &lt;base&gt;<br><br></P>
   *
   * <P>If this permissions field has no explicit record for the specified
   * field, the default permissions value for this base will be returned
   * if defined, else getPerm() will return null.</P>
   *
   * @see arlut.csd.ddroid.common.PermEntry
   */

  public PermEntry getPerm(short baseID, short fieldID) throws RemoteException;

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base &lt;base&gt;
   *
   * @see arlut.csd.ddroid.common.PermEntry
   */

  public PermEntry getPerm(short baseID) throws RemoteException;

  /**
   * <P>Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field &lt;field&gt; in base &lt;base&gt;</P>
   *
   * <P>If this permissions field has no explicit record for the specified
   * field, the default permissions value for this base will be returned
   * if defined, else getPerm() will return null.</P>
   *
   * @see arlut.csd.ddroid.common.PermEntry
   */

  public PermEntry getPerm(Base base, BaseField field) throws RemoteException;

  /**
   * <P>Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base &lt;base&gt;</P>
   *
   * @see arlut.csd.ddroid.common.PermEntry
   */

  public PermEntry getPerm(Base base) throws RemoteException;

  /**
   * <P>Resets the permissions in this PermissionMatrixDBField to
   * the empty set.  Used by non-interactive clients to reset
   * the Permission Matrix to a known state before setting
   * permissions.</P>
   *
   * <P>Returns null on success, or a failure-coded ReturnVal
   * on permissions failure.</P>
   */

  public ReturnVal resetPerms() throws RemoteException;

  /**
   * <P>Sets the permission entry for this matrix for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to PermEntry &lt;entry&gt;.</P>
   *
   * <P>This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ddroid.server.PermissionMatrixDBField PermissionMatrixDBField}.</P>
   *
   * @see arlut.csd.ddroid.common.PermEntry 
   */

  public ReturnVal setPerm(short baseID, short fieldID, PermEntry entry) throws RemoteException;

  /**
   * <P>Sets the permission entry for this matrix for base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;</P>
   *
   * <P>This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ddroid.server.PermissionMatrixDBField PermissionMatrixDBField}.</P>
   *
   * @see arlut.csd.ddroid.common.PermEntry
   */

  public ReturnVal setPerm(short baseID, PermEntry entry) throws RemoteException;

  /**
   * <P>Sets the permission entry for this matrix for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to PermEntry &lt;entry&gt;.</P>
   *
   * <P>This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ddroid.server.PermissionMatrixDBField PermissionMatrixDBField}.</P>
   *
   * @see arlut.csd.ddroid.common.PermEntry 
   */

  public ReturnVal setPerm(Base base, BaseField field, PermEntry entry) throws RemoteException;

  /**
   * <P>Sets the permission entry for this matrix for base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;</P>
   *
   * <P>This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ddroid.server.PermissionMatrixDBField PermissionMatrixDBField}.</P>
   *
   * @see arlut.csd.ddroid.common.PermEntry
   */

  public ReturnVal setPerm(Base base, PermEntry entry) throws RemoteException;

  /**
   * <P>Sets the permission entry for all fields in base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;</P>
   *
   * <P>This operation will fail if this
   * PermissionMatrixDBField is not editable.</P>
   *
   * @param includeBuiltins if true, this will set the permissions for the
   * built-in fields to entry as well as the custom fields.
   */

  public ReturnVal setFieldPerms(short baseID, PermEntry entry, boolean includeBuiltins) throws RemoteException;
}
