/*

   groupCustom.java

   This file is a management class for group objects in Ganymede.
   
   Created: 30 July 1997
   Last Commit: $Format:%cd$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2009
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

package arlut.csd.ganymede.gasharl;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import org.doomdark.uuid.EthernetAddress;
import org.doomdark.uuid.UUIDGenerator;

import arlut.csd.Util.FileOps;
import arlut.csd.Util.PathComplete;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.db_field;
import arlut.csd.ganymede.rmi.db_object;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBEditSet;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.DBNameSpace;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.DBObjectBase;
import arlut.csd.ganymede.server.DBSession;
import arlut.csd.ganymede.server.DateDBField;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.InvidDBField;
import arlut.csd.ganymede.server.NumericDBField;
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     groupCustom

------------------------------------------------------------------------------*/

public class groupCustom extends DBEditObject implements SchemaConstants, groupSchema {
  
  static final boolean debug = false;
  static final boolean debug2 = false;

  /**
   *
   * Customization Constructor
   *
   */

  public groupCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public groupCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public groupCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
  }

  /**
   * <p>Initializes a newly created DBEditObject.</p>
   *
   * <p>When this method is called, the DBEditObject has been created,
   * its ownership set, and all fields defined in the controlling
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * have been instantiated without defined
   * values.  If this DBEditObject is an embedded type, it will
   * have been linked into its parent object before this method
   * is called.</p>
   *
   * <p>This method is responsible for filling in any default
   * values that can be calculated from the 
   * {@link arlut.csd.ganymede.server.DBSession DBSession}
   * associated with the editset defined in this DBEditObject.</p>
   *
   * <p>If initialization fails for some reason, initializeNewObject()
   * will return a ReturnVal with an error result..  If the owning
   * GanymedeSession is not in bulk-loading mode (i.e.,
   * GanymedeSession.enableOversight is true), {@link
   * arlut.csd.ganymede.server.DBSession#createDBObject(short, arlut.csd.ganymede.common.Invid, java.util.Vector)
   * DBSession.createDBObject()} will checkpoint the transaction
   * before calling this method.  If this method returns a failure code, the
   * calling method will rollback the transaction.  This method has no
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

  public ReturnVal initializeNewObject()
  {
    ReturnVal retVal;
    Integer gidVal = new Integer(1001);

    /* -- */

    // we don't want to do any of this initialization during
    // bulk-loading.

    if (!getGSession().enableOversight)
      {
	return null;
      }

    // need to find a global unique id (guid) for this user

    StringDBField guidField = (StringDBField) getField(GUID);

    if (guidField == null)
      {
	return Ganymede.createErrorDialog("Group Initialization Failure",
					  "Couldn't find the guid field.. schema problem?");
      }

    String guid = generateGUID(); // create a globally unique uid

    retVal = guidField.setValueLocal(guid);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // need to find a gid for this group

    // see if we have an owner set, check it for our starting gid

    Vector owners = getFieldValuesLocal(SchemaConstants.OwnerListField);

    if (owners != null && owners.size() > 0)
      {
	Invid primaryOwner = (Invid) owners.elementAt(0);

	DBObject owner = getSession().viewDBObject(primaryOwner);

	if (owner != null)
	  {
	    // field 256 in the owner group is the GASHARL starting
	    // uid/gid

	    gidVal = (Integer) owner.getFieldValueLocal((short) 256);

	    if (gidVal == null)
	      {
		gidVal = new Integer(1001);
	      }
	  }
      }

    NumericDBField numField = (NumericDBField) getField((short) 258);

    if (numField == null)
      {
	return Ganymede.createErrorDialog("Group Initialization Failure",
					  "Couldn't find the gid field.. schema problem?");
      }

    DBNameSpace namespace = numField.getNameSpace();

    if (namespace == null)
      {
	return Ganymede.createErrorDialog("Group Initialization Failure",
					  "Couldn't find the gid namespace.. schema problem?");
      }

    // now, find a gid.. unfortunately, we have to use immutable Integers here.. not
    // the most efficient at all.

    while (!namespace.reserve(editset, gidVal))
      {
	gidVal = new Integer(gidVal.intValue()+1);
      }

    // we use setValueLocal so we can set a value that the user can't edit.

    retVal = numField.setValueLocal(gidVal);

    return retVal;
  }

  /**
   *
   * Customization method to verify whether this object type has an inactivation
   * mechanism.
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean canBeInactivated()
  {
    return true;
  }

  /**
   *
   * Customization method to verify whether the group has permission
   * to inactivate a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for inactivating by the client.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean canInactivate(DBSession session, DBEditObject object)
  {
    return true;
  }

  /**
   *
   * Called when object is inactivated.  This will take care of users
   * who have this group as the home group.  
   *
   * Overides inactivate() in DBEditObject.
   */

  public ReturnVal inactivate(String ckp_label)
  {
    return inactivate(false, false, ckp_label);
  }

  public ReturnVal inactivate(boolean suceeded, boolean fromWizard, String ckp_label)
  {
    ReturnVal retVal = null;
    groupInactivateWizard wiz;

    /* -- */

    if (fromWizard)
      {
	if (suceeded)
	  {
	    if (debug)
	      {
		System.err.println("groupCustom.inactivate: setting removal date.");
	      }

	    DateDBField date;
	    Calendar cal = Calendar.getInstance();
	    Date time;
	    
	    // make sure that the expiration date is cleared.. we're on
	    // the removal track now.
	    
	    date = (DateDBField) getField(SchemaConstants.ExpirationField);
	    retVal = date.setValueLocal(null);
	    
	    if (retVal != null && !retVal.didSucceed())
	      {
		super.finalizeInactivate(false, ckp_label);
		return retVal;
	      }
	    
	    // determine what will be the date 3 months from now
	    
	    time = new Date();
	    cal.setTime(time);
	    cal.add(Calendar.MONTH, 3);
	    
	    // and set the removal date
	    if (debug)
	      {
		System.err.println("groupCustom.inactivate: setting removal date to: " + cal.getTime());
	      }

	    date = (DateDBField) getField(SchemaConstants.RemovalField);
	    retVal = date.setValueLocal(cal.getTime());

	    if ((retVal == null) || (retVal.didSucceed()))
	      {
		if (debug)
		  {
		    System.err.println("groupCustom.inactivate: retVal returns true, I am finalizing.");
		  }

		finalizeInactivate(true, ckp_label);
	      }

	    return retVal;
	  }
	else
	  {
	    finalizeInactivate(false, ckp_label);
	    return new ReturnVal(false);
	  }
      }
    else			// not called by wizard
      {
	if (getEditSet().isInteractive())
	  {
	    try
	      {
		if (debug)
		  {
		    System.err.println("groupCustom.inactivate: Starting new groupInactivateWizard");
		  }
		
		wiz = new groupInactivateWizard(this.gSession, this, ckp_label);
		
		return wiz.respond(null);
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not create groupInactivateWizard: " + rx);
	      }	    
	  }
	else
	  {
	    // non-interactive.  we can only inactivate if there are no home users
	    // in this group

	    InvidDBField homeField = (InvidDBField) getField(groupSchema.HOMEUSERS);

	    if (homeField.size() == 0)
	      {
		return this.inactivate(true, true, ckp_label);
	      }
	    else
	      {
		return this.inactivate(false, true, ckp_label);
	      }
	  }
      }
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
      case groupSchema.GROUPNAME:
      case groupSchema.GID:
	return true;
      }

    return false;
  }

  /**
   * <p>Customization method to verify overall consistency of
   * a DBObject.  This method is intended to be overridden
   * in DBEditObject subclasses, and will be called by
   * {@link arlut.csd.ganymede.server.DBEditObject#commitPhase1() commitPhase1()}
   * to verify the readiness of this object for commit.  The
   * DBObject passed to this method will be a DBEditObject,
   * complete with that object's GanymedeSession reference
   * if this method is called during transaction commit, and
   * that session reference may be used by the verifying code if
   * the code needs to access the database.</p>
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
    GanymedeSession gsession = object.getGSession();

    Vector users = object.getFieldValuesLocal(USERS);
    Vector homeUsers = object.getFieldValuesLocal(HOMEUSERS);

    Vector diff = VectorUtils.difference(homeUsers, users);

    if (diff.size() != 0)
      {
	Vector names = new Vector();

	for (int i = 0; i < diff.size(); i++)
	  {
	    Invid objId = (Invid) diff.elementAt(i);

	    if (gsession != null)
	      {
		names.addElement(gsession.viewObjectLabel(objId));
	      }
	    else
	      {
		names.addElement(objId);
	      }
	  }

	return Ganymede.createErrorDialog("Group Consistency Violation",
					  "Error, the following users have group " + object.getLabel() + " listed as their home " +
					  "group, but are not listed as normal members of the group:\n\n" + 
					  VectorUtils.vectorString(names));
      }

    // okay, then

    return null;
  }

  /**
   *
   * This method is a hook for subclasses to override to
   * pass the phase-two commit command to external processes.<br><br>
   *
   * For normal usage this method would not be overridden.  For
   * cases in which change to an object would result in an external
   * process being initiated whose success or failure would not
   * affect the successful commit of this DBEditObject in the
   * Ganymede server, the process invokation should be placed here,
   * rather than in commitPhase1().<br><br>
   *
   * Subclasses that override this method may wish to make this method 
   * synchronized.
   *
   * @see arlut.csd.ganymede.server.DBEditSet
   */

  public void commitPhase2()
  {
    switch (getStatus())
      {
      case DROPPING:
	break;

      case CREATING:
	break;

      case DELETING:

	handleGroupDelete(original.getLabel());

	break;
	
      case EDITING:

	String name = getLabel();
	String oldname = original.getLabel();

	if (!name.equals(oldname))
	  {
	    handleGroupRename(oldname, name);
	  }
      }

    return;
  }

  /**
   * Handle the wizards for dealing with users that have this as the home group.
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    try
      {
	groupHomeGroupWizard homeWizard;
	ReturnVal retVal = null;

	/* -- */

	// First find out what they are changing

	if (debug)
	  {
	    System.out.println("Field name: " + field.getName() + 
			       " Field typeDesc: " + field.getTypeDesc());
	  }

	if (field.getID() == HOMEUSERS) // from groupSchema
	  {    
	    // What are they doing to the home users?

	    switch (operation) 
	      {
	      case ADDELEMENT:
	      case ADDELEMENTS:

		if (debug)
		  {
		    print("it's an ADDELEMENT, ignoring it.");
		  }

		// we don't need to rescan anything, do we?
		return null;

	      case DELELEMENTS:

		if (gSession == null)
		  {
		    // If there is no session, the server is doing something special.
		    // Assume the server knows what is going on, and let it do the deed.
		    return null;
		  }
		else
		  {
		    return Ganymede.createErrorDialog("Group Validation Error",
						      "Can't do bulk removal of home group entries right now.");
		  }

	      case DELELEMENT:

		if (debug)
		  {
		    print("HOMEUSERS field changing.");
		  }
	
		Vector users = getFieldValuesLocal(HOMEUSERS);
		int index = ((Integer) param1).intValue();
		Invid userInvid = (Invid)users.elementAt(index);

		if (gSession == null)
		  {
		    // If there is no session, the server is doing something special.
		    // Assume the server knows what is going on, and let it do the deed.
		    return null;
		  }
		else if (!gSession.enableWizards)
		  {
		    // Stupid client won't let us show wizards.  We'll teach them!
		    // First, find out what is going on.  How many groups is this user in?

		    db_object user = gSession.edit_db_object(userInvid).getObject();
		    int size = 0;

		    try
		      {
			size = user.getField(userSchema.GROUPLIST).size();
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("How come I can't talk to the server, when I AM the server? " + rx);
		      }
	      
		    if (size == 2)
		      {
			// They belong to two groups: this one, and one other one.
			// We will make the other one the home group.

			try
			  {
			    db_field groupListField = user.getField(userSchema.GROUPLIST);
			    Vector groupList = groupListField.getValues();    
			
			    for (int i = 0; i < groupList.size() ;i++)
			      {
				if (!this.equals(groupList.elementAt(i)))
				  {
				    // this will be the new home group

				    if (debug)
				      {
					print("Found the other group, changing the user's home group.");
				      }
			      
				    db_field homeGroup = user.getField(userSchema.HOMEGROUP);
				    retVal = homeGroup.setValue(groupList.elementAt(i));
				
				    break;
				  }
			      }
			  }
			catch (RemoteException rx)
			  {
			    throw new RuntimeException("Again, with the remote exceptions: " + rx);
			  }
		      }
		    else if (size < 1)
		      {
			// They are only in one group, so what good is that?
			return Ganymede.createErrorDialog("Group Change Failed",
							  "This user has this group for a home group. " +
							  " You cannot remove this user, since this is his only group.");
		      }
		    else 
		      {
			return Ganymede.createErrorDialog("Group Change Failed",
							  "This user has many groups to choose from. " +
							  " You must choose one to be the home group, or turn wizards on.");
		      }
		  }
  
		// This calls for a wizard.  See if one is running already

		if (gSession.isWizardActive() && gSession.getWizard() instanceof groupHomeGroupWizard)
		  {
		    if (debug)
		      {
			print("Ok, wizard is running.  Checking to see if it is done.");
		      }

		    // We are already in this wizard, lets see where we are

		    homeWizard = (groupHomeGroupWizard)gSession.getWizard();
	      
		    if (homeWizard.getState() == homeWizard.DONE)
		      {
			// Ok, the home wizard has done its deed, so get
			// rid of it

			homeWizard.unregister();

			// I don't think it is a good idea to return null
			// here.

			if (debug)
			  {
			    print("Returning null, because I am in groupCustom.wizardHook " +
				  "with an active wizard that is done.");
			  }

			return null;
		      }
		    else
		      {
			if (homeWizard.groupObject != this)
			  {
			    print("bad object, group objects confused somehow.");
			  }
		    
			if (homeWizard.getState() != homeWizard.DONE)
			  {
			    print(" bad state: " + homeWizard.getState());
			  }
		  
			homeWizard.unregister(); // get rid of it, so it doesn't mess other stuff up
			return Ganymede.createErrorDialog("Group object error",
							  "The client is attempting to do an operation " +
							  "on a user object with an active wizard.");
		      }
		  }
		else if (gSession.isWizardActive() &&
			 !(gSession.getWizard() instanceof groupHomeGroupWizard))
		  {
		    return Ganymede.createErrorDialog("Group Object Error",
						      "The client is trying to change the group object " +
						      "while other wizards are running around.");
		  }
	  
		// Ok, if we get to here, then we need to start up a new wizard.
		// The user is trying to remove someone out of the HOMEUSER field, which may cause problems.
	    
		try
		  {
		    if (debug)
		      {
			print("Starting up a new wizard");
		      }
		
		    homeWizard = new groupHomeGroupWizard(this.gSession, this, userInvid);
		
		    return homeWizard.respond(null);
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not send wizard to client: " + rx);
		  }
	      }
	  }
	else if (field.getID() == USERS) // from groupSchema
	  {
	    switch (operation)
	      {
	      case DELELEMENTS:

		if (gSession == null)
		  {
		    return null;	// fine, whatever
		  }
		else
		  {
		    return Ganymede.createErrorDialog("Group Validation Error",
						      "Can't do bulk removal of group entries right now.");
		  }

	      case DELELEMENT:
		Vector users = getFieldValuesLocal(USERS);
		int index = ((Integer) param1).intValue();
		Invid userInvid = (Invid)users.elementAt(index);

		Vector homeUsers = getFieldValuesLocal(HOMEUSERS);

		if (!homeUsers.contains(userInvid))
		  {
		    return null;	// fine, whatever
		  }

		if (gSession == null)
		  {
		    // no session, this is being done by an automated process, let
		    // it go

		    return null;
		  }

		if (!gSession.getSession().isInteractive())
		  {
		    // let it go for now, we'll verify at transaction commit if we have to

		    return null;
		  }

		String username = gSession.viewObjectLabel(userInvid);

		return Ganymede.createErrorDialog("Group Validation Error",
						  "Can't remove user " + username + " from group " + getLabel() 
						  + "'s list of users, this user is using " + getLabel() + 
						  " as their home group.  Remove this user from the home users list first.");
	      }
	  }

	// otherwise, we don't care, at least not yet

	return retVal;
      }
    catch (NotLoggedInException ex)
      {
	return Ganymede.loginError(ex);
      }
  }

  /**
   * This method handles external actions for deleting a user.
   */

  private void handleGroupDelete(String name)
  {
    String deleteFilename;
    File deleteHandler = null;

    /* -- */

    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    // This would be unusual for a delete, but..

    if (Ganymede.log == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("groupCustom.handleGroupDelete(): group " + name +
			   "is being deleted");
      }

    deleteFilename = System.getProperty("ganymede.builder.scriptlocation");

    if (deleteFilename != null)
      {
	// make sure we've got the path separator at the end of
	// deleteFilename, add our script name
	
	deleteFilename = PathComplete.completePath(deleteFilename) + "/scripts/group_deleter";
	
	deleteHandler = new File(deleteFilename);
      }
    else
      {
	Ganymede.debug("groupCustom.handleGroupDelete(): Couldn't find " +
		       "ganymede.builder.scriptlocation property");
      }

    if (deleteHandler != null && deleteHandler.exists())
      {
	try
	  {
	    String execLine = deleteFilename + " " + name;

	    if (debug)
	      {
		System.err.println("handleGroupDelete: running " + execLine);
	      }

	    try
	      {
		if (debug)
		  {
		    System.err.println("handleGroupDelete: blocking");
		  }

		int result = FileOps.runProcess(execLine);

		if (debug)
		  {
		    System.err.println("handleGroupDelete: done");
		  }

		if (result != 0)
		  {
		    Ganymede.debug("Couldn't handle externals for deleting group " + name + 
				   "\n" + deleteFilename + 
				   " returned a non-zero result: " + result);
		  }
	      }
	    catch (InterruptedException ex)
	      {
		Ganymede.debug("Couldn't handle externals for deleting group " + name + 
			       ex.getMessage());
	      }
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug("Couldn't handle externals for deleting group " + name + 
			   ex.getMessage());
	  }
      }
  }

  /**
   * This method handles external actions for renaming a group.
   */

  private void handleGroupRename(String orig, String newname)
  {
    String renameFilename;
    File renameHandler = null;

    /* -- */

    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("groupCustom.handleGroupRename(): user " + orig +
			   "is being renamed to " + newname);
      }

    renameFilename = System.getProperty("ganymede.builder.scriptlocation");

    if (renameFilename != null)
      {
	// make sure we've got the path separator at the end of
	// renameFilename, add our script name
	    
	renameFilename = PathComplete.completePath(renameFilename) + "/scripts/group_namer";
	    
	renameHandler = new File(renameFilename);
      }
    else
      {
	Ganymede.debug("groupCustom.handleGroupRename(): Couldn't find " +
		       "ganymede.builder.scriptlocation property");
      }

    if (renameHandler != null && renameHandler.exists())
      {
	try
	  {
	    String execLine = renameFilename + " " + orig + " " + newname;

	    if (debug)
	      {
		System.err.println("handleGroupRename: running " + execLine);
	      }

	    try
	      {
		if (debug)
		  {
		    System.err.println("handleGroupRename: blocking");
		  }

		int result = FileOps.runProcess(execLine);

		if (debug)
		  {
		    System.err.println("handleGroupRename: done");
		  }

		if (result != 0)
		  {
		    Ganymede.debug("Couldn't handle externals for renaming group " + orig + 
				   " to " + newname + "\n" + renameFilename + 
				   " returned a non-zero result: " + result);
		  }
	      }
	    catch (InterruptedException ex)
	      {
		Ganymede.debug("Couldn't handle externals for renaming group " + orig + 
			       " to " + 
			       newname + "\n" + 
			       ex.getMessage());
	      }
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug("Couldn't handle externals for renaming group " + orig + 
			   " to " + 
			   newname + "\n" + 
			   ex.getMessage());
	  }
      }
  }

  private void print(String s)
  {
    System.err.println("groupCustom.wizardHook(): " + s);
  }

  /**
   * <p>Private method to create a globally unique UID value suitable
   * for certain LDAP applications</p>
   */

  private String generateGUID()
  {
    UUIDGenerator gen = UUIDGenerator.getInstance();
    org.doomdark.uuid.UUID guid = gen.generateTimeBasedUUID(new EthernetAddress("8:0:20:fd:6b:7")); // csdsun9

    return guid.toString();
  }

}
