/*
   GASH 2

   InvidDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.58 $ %D%
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
                                                                    InvidDBField

------------------------------------------------------------------------------*/

public final class InvidDBField extends DBField implements invid_field {

  static final boolean debug = false;

  // --

  /**
   *
   * Receive constructor.  Used to create a InvidDBField from a DBStore/DBJournal
   * DataInput stream.
   *
   */

  InvidDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException, RemoteException
  {
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
   * but for which no value has been stored in the DBStore.
   *
   * Used to provide the client a template for 'creating' this
   * field if so desired.
   *
   */

  InvidDBField(DBObject owner, DBObjectBaseField definition) throws RemoteException
  {
    this.owner = owner;
    this.definition = definition;
    
    defined = false;
    value = null;

    if (isVector())
      {
	values = new Vector();
      }
    else
      {
	values = null;
      }
  }

  /**
   *
   * Copy constructor.
   *
   */

  public InvidDBField(DBObject owner, InvidDBField field) throws RemoteException
  {
    this.owner = owner;
    definition = field.definition;
    
    if (isVector())
      {
	values = (Vector) field.values.clone();
	value = null;
      }
    else
      {
	value = field.value;
	values = null;
      }

    defined = true;
  }

  /**
   *
   * Scalar value constructor.
   *
   */

  public InvidDBField(DBObject owner, Invid value, DBObjectBaseField definition) throws RemoteException
  {
    if (definition.isArray())
      {
	throw new IllegalArgumentException("scalar value constructor called on vector field " + getName() +
					   " in object " + owner.getLabel());
      }

    this.owner = owner;
    this.definition = null;
    this.value = value;

    if (value != null)
      {
	defined = true;
      }

    values = null;
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public InvidDBField(DBObject owner, Vector values, DBObjectBaseField definition) throws RemoteException
  {
    if (!definition.isArray())
      {
	throw new IllegalArgumentException("vector value constructor called on scalar field " + getName() +
					   " in object " + owner.getLabel());
      }

    this.owner = owner;
    this.definition = definition;
    
    if (values == null)
      {
	this.values = new Vector();
	defined = false;
      }
    else
      {
	this.values = (Vector) values.clone();
	defined = (values.size() > 0);
      }

    value = null;
  }
  
  public Object clone()
  {
    try
      {
	return new InvidDBField(owner, this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Couldn't create InvidDBField: " + ex);
      }
  }

  void emit(DataOutput out) throws IOException
  {
    Invid temp;

    /* -- */

    if (isVector())
      {
	out.writeShort(values.size());
	for (int i = 0; i < values.size(); i++)
	  {
	    temp = (Invid) values.elementAt(i);
	    out.writeShort(temp.getType());
	    out.writeInt(temp.getNum());
	  }
      }
    else
      {
	temp = (Invid) value;

	try
	  {
	    out.writeShort(temp.getType());
	    out.writeInt(temp.getNum());
	  }
	catch (NullPointerException ex)
	  {
	    System.err.println(owner.getLabel() + ":" + getName() + ": void value in emit");

	    if (temp == null)
	      {
		System.err.println(owner.getLabel() + ":" + getName() + ": field value itself is null");
	      }

	    throw ex;
	  }
      }
  }

  void receive(DataInput in) throws IOException
  {
    Invid temp;
    int count;

    /* -- */

    if (isVector())
      {
	count = in.readShort();
	values = new Vector(count);
	for (int i = 0; i < count; i++)
	  {
	    temp = new Invid(in.readShort(), in.readInt());
	    values.addElement(temp);
	  }
      }
    else
      {
	value = new Invid(in.readShort(), in.readInt());
      }

    defined = true;
  }

  // ****
  //
  // type-specific accessor methods
  //
  // ****

  public Invid value()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector " + getName() +
					   " in object " + owner.getLabel());
      }

    return (Invid) value;
  }

  public Invid value(int index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar " + getName() +
					   " in object " + owner.getLabel());
      }

    return (Invid) values.elementAt(index);
  }

  /**
   *
   * This method returns a text encoded value for this InvidDBField
   * without checking permissions.<br><br>
   *
   * This method avoids checking permissions because it is used on
   * the server side only and because it is involved in the getLabel()
   * logic for DBObject, which is invoked from GanymedeSession.getPerm().<br><br>
   *
   * If this method checked permissions and the getPerm() method
   * failed for some reason and tried to report the failure using
   * object.getLabel(), as it does at present, the server could get
   * into an infinite loop.
   *
   */

  public synchronized String getValueString()
  {
    GanymedeSession gsession = null;

    /* -- */

    // where will we go to look up the label for our target(s)?

    try
      {
	if (owner.editset != null)
	  {
	    gsession = owner.editset.getSession().getGSession();
	  }
      }
    catch (NullPointerException ex)
      {
      }

    if (gsession == null)
      {
	gsession = Ganymede.internalSession;
      }

    // now do the work

    if (!isVector())
      {
	if (value == null)
	  {
	    return "null";
	  }

	Invid localInvid = (Invid) this.value();

	// XXX note: we don't use our owner's lookupLabel() method
	// for scalar invid values.. 

	if (gsession != null)
	  {
	    return gsession.viewObjectLabel(localInvid);
	  }
	else
	  {
	    return this.value().toString();
	  }
      }
    else
      {
	String result = "";
	int size = size();
	Invid tmp;

	for (int i = 0; i < size; i++)
	  {
	    if (i != 0)
	      {
		result = result + ", ";
	      }

	    tmp = this.value(i);

	    if (gsession != null)
	      {
		result = result + gsession.viewObjectLabel(tmp);
	      }
	    else
	      {
		result = result + this.value(i).toString();
	      }
	  }

	return result;
      }
  }

  /**
   *
   * OK, this is a bit vague.. getEncodingString() is used by the new
   * dump system to allow all fields to be properly sorted in the table..
   * a real reversible encoding of an invid field would *not* be the
   * getValueString() results, but getValueString() is what we want in
   * the dump result table, so we'll do that here for now.
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
   * for inclusion in a log entry and in an email message.
   *
   * If there is no change in the field, null will be returned.
   * 
   */

  public synchronized String getDiffString(DBField orig)
  {
    StringBuffer result = new StringBuffer();
    InvidDBField origI;
    DBObject object;
    GanymedeSession gsession = null;

    /* -- */

    if (!(orig instanceof InvidDBField))
      {
	throw new IllegalArgumentException("bad field comparison " + getName());
      }

    if (debug)
      {
	System.err.println("Entering InvidDBField getDiffString()");
      }

    if (orig == this)
      {
	return "";
      }

    origI = (InvidDBField) orig;

    try
      {
	if (owner.editset != null)
	  {
	    gsession = owner.editset.getSession().getGSession();
	  }
      }
    catch (NullPointerException ex)
      {
      }
    
    if (gsession == null)
      {
	gsession = Ganymede.internalSession;
      }

    if (isVector())
      {
	Vector 
	  added = new Vector(),
	  deleted = new Vector();

	Enumeration enum;

	Object 
	  element = null;

	Invid
	  elementA = null,
	  elementB = null;

	boolean found = false;

	/* -- */

	if (debug)
	  {
	    System.err.println("vector diff.. searching for deleted items");
	  }

	// find elements in the orig field that aren't in our present field

	Hashtable currentElements = new Hashtable();

	for (int i = 0; !found && i < values.size(); i++)
	  {
	    if (debug)
	      {
		System.err.print(",");
	      }

	    element = values.elementAt(i);

	    currentElements.put(element, element);
	  }

	enum = origI.values.elements();

	while (enum.hasMoreElements())
	  {
	    if (debug)
	      {
		System.err.print("x");
	      }

	    element = enum.nextElement();

	    if (currentElements.get(element) == null)
	      {
		deleted.addElement(element);
	      }
	  }

	// find elements in our present field that aren't in the orig field

	if (debug)
	  {
	    System.err.println("vector diff.. searching for added items");
	  }

	Hashtable origElements = new Hashtable();

	for (int i = 0; !found && i < origI.values.size(); i++)
	  {
	    if (debug)
	      {
		System.err.print(",");
	      }

	    element = origI.values.elementAt(i);
	    
	    origElements.put(element, element);
	  }

	enum = values.elements();

	while (enum.hasMoreElements())
	  {
	    if (debug)
	      {
		System.err.print("x");
	      }

	    element = enum.nextElement();

	    if (origElements.get(element) == null)
	      {
		added.addElement(element);
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
		if (debug)
		  {
		    System.err.print("Working out deleted items");
		  }

		result.append("\tDeleted: ");
	    
		for (int i = 0; i < deleted.size(); i++)
		  {
		    elementA = (Invid) deleted.elementAt(i);

		    if (i > 0)
		      {
			result.append(", ");
		      }

		    if (gsession != null)
		      {
			result.append(gsession.viewObjectLabel(elementA));
		      }
		    else
		      {
			result.append(elementA.toString());
		      }
		  }

		result.append("\n");
	      }

	    if (added.size() != 0)
	      {
		if (debug)
		  {
		    System.err.print("Working out added items");
		  }

		result.append("\tAdded: ");
	    
		for (int i = 0; i < added.size(); i++)
		  {
		    elementA = (Invid) added.elementAt(i);

		    if (i > 0)
		      {
			result.append(", ");
		      }

		    if (gsession != null)
		      {
			result.append(gsession.viewObjectLabel(elementA));
		      }
		    else
		      {
			result.append(elementA.toString());
		      }
		  }

		result.append("\n");
	      }

	    return result.toString();
	  }
      }
    else
      {
	if (debug)
	  {
	    System.err.println("InvidDBField: scalar getDiffString() comparison");
	  }

	if (origI.value().equals(this.value()))
	  {
	    return null;
	  }
	else
	  {
	    result.append("\tOld: ");

	    if (gsession != null)
	      {
		result.append(gsession.viewObjectLabel(origI.value()));
	      }
	    else
	      {
		result.append(origI.value().toString());
	      }

	    result.append("\n\tNew: ");

	    if (gsession != null)
	      {
		result.append(gsession.viewObjectLabel(this.value()));
	      }
	    else
	      {
		result.append(this.value().toString());
	      }

	    result.append("\n");
	
	    return result.toString();
	  }
      }
  }

  // ****
  //
  // methods for maintaining invid symmetry
  //
  // ****

  /**
   *
   * This method is used to link the remote invid to this checked-out invid
   * in accordance with this field's defined symmetry constraints.<br><br>
   *
   * This method will extract the objects referenced by the old and new
   * remote parameters, and will cause the appropriate invid dbfields in
   * them to be updated to reflect the change in link status.  If either
   * operation can not be completed, bind will return the system to its
   * pre-bind status and return false.  One or both of the specified
   * remote objects may remain checked out in the current editset until
   * the transaction is committed or released.<br><br>
   *
   * It is an error for newRemote to be null;  if you wish to undo an
   * existing binding, use the unbind() method call.  oldRemote may
   * be null if this currently holds no value, or if this is a vector
   * field and newRemote is being added.<br><br>
   *
   * This method should only be called from synchronized methods.
   *
   * @param oldRemote the old invid to be replaced
   * @param newRemote the new invid to be linked
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   *
   * @return null on success, or a ReturnVal with an error dialog encoded on failure
   *
   * @see unbind
   *
   */

  private ReturnVal bind(Invid oldRemote, Invid newRemote, boolean local)
  {
    short targetField;

    DBEditObject 
      eObj = null,
      oldRef = null,
      newRef = null;

    InvidDBField 
      oldRefField = null,
      newRefField = null;

    DBSession
      session = null;

    boolean 
      anonymous = false,
      anonymous2 = false;

    ReturnVal retVal = null;

    /* -- */

    if (newRemote == null)
      {
	setLastError("InvidDBField.bind: null newRemote");
	throw new IllegalArgumentException("null newRemote " + getName() + " in object " + owner.getLabel());
      }

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("not an editable invid field: " + getName() + 
					   " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) this.owner;
    session = eObj.getSession();

    // find out whether there is an explicit back-link field

    if (getFieldDef().isSymmetric())
      {
	// find out what field in remote we might need to update

	targetField = getFieldDef().getTargetField();
      }
    else
      {
	targetField = SchemaConstants.BackLinksField;
      }

    if ((oldRemote != null) && oldRemote.equals(newRemote))
      {
	return null;		// success
      }

    // check out the old object and the new object
    // remove the reference from the old object
    // add the reference to the new object

    if (oldRemote != null)
      {
	// check to see if we have permission to anonymously unlink
	// this field from the target field, else go through the
	// GanymedeSession layer to have our permissions checked.

	// note that if the GanymedeSession layer has already checked out the
	// object, session.editDBObject() will return a reference to that
	// version, and we'll lose our security bypass.. for that reason,
	// we also use the anonymous variable to instruct dissolve to disregard
	// write permissions if we have gotten the anonymous OK

	if (session.getObjectHook(oldRemote).anonymousUnlinkOK(targetField))
	  {
	    oldRef = (DBEditObject) session.editDBObject(oldRemote);
	    anonymous = true;
	  }
	else
	  {
	    oldRef = (DBEditObject) session.getGSession().edit_db_object(oldRemote);
	  }

	if (oldRef == null)
	  {
	    setLastError("couldn't check out old invid " + oldRemote + " for symmetry maintenance");
	    return Ganymede.createErrorDialog("InvidDBField.bind(): Couldn't unlink old reference",
					      "Your operation could not succeed because field " + getName() +
					      " was linked to a remote reference " + oldRef.toString() + 
					      " that could not be found for unlinking.\n\n" +
					      "This is a serious logic error in the server.");
	  }

	try
	  {
	    oldRefField = (InvidDBField) oldRef.getField(targetField);
	  }
	catch (ClassCastException ex)
	  {
	    setLastError("InvidDBField.bind: invid target field designated in schema is not an invid field");

	    try
	      {
		return Ganymede.createErrorDialog("InvidDBField.bind(): Couldn't unlink old reference",
						  "Your operation could not succeed due to an error in the " +
						  "server's schema.  Target field " + 
						  oldRef.getField(targetField).getName() +
						  " in object " + oldRef.getLabel() +
						  " is not an invid field.");
	      }
	    catch (RemoteException rx)
	      {
		return Ganymede.createErrorDialog("InvidDBField.bind(): Couldn't unlink old reference",
						  "Your operation could not succeed due to an error in the " +
						  "server's schema.  Target field " + targetField +
						  " in object " + oldRef.getLabel() +
						  " is not an invid field.");
	      }
	  }
	
	if (oldRefField == null)
	  {
	    // editDBObject() will create undefined fields for all fields defined
	    // in the DBObjectBase, so if we got a null result we have a schema
	    // corruption problem.

	    String tempString = "InvidDBField.bind: old target field not defined <" + owner.getLabel() +
	      ":" + getName() + "> in <" + oldRef.getLabel() + ":" + targetField + ">";
	    
	    setLastError(tempString);

	    return Ganymede.createErrorDialog("InvidDBField.bind(): Couldn't unlink old reference",
					      "Your operation could not succeed due to a possible inconsistency in the " +
					      "server database.  Target field number " + targetField +
					      " in object " + oldRef.getLabel() +
					      " does not exist, or you do not have permission to access " +
					      "this field.");
	  }
      }

    // check to see if we have permission to anonymously link
    // this field to the target field, else go through the
    // GanymedeSession layer to have our permissions checked.

    // note that if the GanymedeSession layer has already checked out the
    // object, session.editDBObject() will return a reference to that
    // version, and we'll lose our security bypass.. for that reason,
    // we also use the anonymous2 variable to instruct establish to disregard
    // write permissions if we have gotten the anonymous OK
    
    if (session.getObjectHook(newRemote).anonymousLinkOK(targetField))
      {
	newRef = session.editDBObject(newRemote);
	anonymous2 = true;
      }
    else
      {
	newRef = (DBEditObject) session.getGSession().edit_db_object(newRemote);
      }
    
    if (newRef == null)
      {
	setLastError("couldn't check out new invid " + newRemote + " (" + 
		     session.getGSession().viewObjectLabel(newRemote) + ") for symmetry maintenance");

	return Ganymede.createErrorDialog("InvidDBField.bind(): Couldn't link to new reference",
					  "Your operation could not succeed because field " + getName() +
					  " could not be linked to " + newRemote.toString() + 
					  ".  This could be due to a lack of permissions.");
      }

    try
      {
	newRefField = (InvidDBField) newRef.getField(targetField);
      }
    catch (ClassCastException ex)
      {
	setLastError("invid target field designated in schema is not an invid field");

	try
	  {
	    return Ganymede.createErrorDialog("InvidDBField.bind(): Couldn't link to new reference",
					      "Your operation could not succeed due to an error in the " +
					      "server's schema.  Target field " + 
					      newRef.getField(targetField).getName() +
					      " in object " + newRef.getLabel() +
					      " is not an invid field.");
	  }
	catch (RemoteException rx)
	  {
	    return Ganymede.createErrorDialog("InvidDBField.bind(): Couldn't link to new reference",
					      "Your operation could not succeed due to an error in the " +
					      "server's schema.  Target field " + 
					      targetField +
					      " in object " + newRef.getLabel() +
					      " is not an invid field.");
	  }
      }
    
    if (newRefField == null)
      {
	// editDBObject() will create undefined fields for all fields defined
	// in the DBObjectBase, so if we got a null result we have a schema
	// corruption problem.
	
	String tempString = "InvidDBField.bind: target field not defined <" + owner.getLabel() +
	  ":" + getName() + "> in <" + newRef.getLabel() + ":" + targetField + ">";

	setLastError(tempString);

	return Ganymede.createErrorDialog("InvidDBField.bind(): Couldn't link new reference",
					  "Your operation could not succeed due to a possible inconsistency in the " +
					  "server database.  Target field number " + targetField +
					  " in object " + newRef.getLabel() +
					  " does not exist, or you do not have permission to access " +
					  "this field.");
      }

    if (oldRefField != null)
      {
        retVal = oldRefField.dissolve(owner.getInvid(), (anonymous||local));

 	if (retVal != null && !retVal.didSucceed())
	  {
	    return retVal;
	  }
      }
    
    retVal = newRefField.establish(owner.getInvid(), (anonymous2||local));

    if (retVal != null && !retVal.didSucceed())
      {
	// oops!  try to undo what we did.. this probably isn't critical
	// because something above us will do a rollback, but it's polite.

	if (oldRefField != null)
	  {
	    oldRefField.establish(owner.getInvid(), (anonymous||local)); // hope this works
	  }
	
	setLastError("couldn't establish field symmetry with " + newRef);

	return retVal;
      }

    return null;		// success
  }

  /**
   *
   * This method is used to unlink this field from the specified remote
   * invid in accordance with this field's defined symmetry constraints.
   *
   * @param remote An invid for an object to be checked out and unlinked
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   *
   * @return null on success, or a ReturnVal with an error dialog encoded on failure
   *
   */

  private ReturnVal unbind(Invid remote, boolean local)
  {
    short targetField;

    DBEditObject 
      eObj = null,
      oldRef = null;

    InvidDBField 
      oldRefField = null;

    DBSession
      session = null;

    // debug vars

    boolean anon = false;

    /* -- */

    if (remote == null)
      {
	throw new IllegalArgumentException("null remote: " + getName() + " in object " + owner.getLabel());
      }

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("not an editable invid field: " + getName() +
					   " in object " + owner.getLabel());
      }

    // find out whether there is an explicit back-link field

    if (getFieldDef().isSymmetric())
      {
	// find out what field in remote we might need to update

	targetField = getFieldDef().getTargetField();
      }
    else
      {
	targetField = SchemaConstants.BackLinksField;
      }

    eObj = (DBEditObject) this.owner;
    session = eObj.getSession();

    // check to see if we have permission to anonymously unlink
    // this field from the target field, else go through the
    // GanymedeSession layer to have our permissions checked.

    // note that if the GanymedeSession layer has already checked out the
    // object, session.editDBObject() will return a reference to that
    // version, and we'll lose our security bypass.. for that reason,
    // we also use the anon variable to instruct dissolve to disregard
    // write permissions if we have gotten the anonymous OK

    if (session.getObjectHook(remote).anonymousUnlinkOK(targetField))
      {
	anon= true;		// debug

	oldRef = session.editDBObject(remote);

	if (oldRef == null)
	  {
	    return null;		// it's not there, so we are certainly unbound, no?
	  }
      }
    else
      {
	oldRef = (DBEditObject) session.getGSession().edit_db_object(remote);

	if (oldRef == null)
	  {
	    if (session.viewDBObject(remote) == null)
	      {
		return null;	// it's not there, so we are certainly unbound, no?
	      }
	    else
	      {
		// it's there, but we can't unlink it

		return Ganymede.createErrorDialog("InvidDBField.unbind(): Couldn't unlink old reference",
						  "We couldn't unlink field " + getName() +
						  " in object " + getOwner().getLabel() +
						  " from field " + targetField + " in object " +
						  session.getGSession().viewObjectLabel(remote) + 
						  " due to a permissions problem.");
	      }
	  }
      }

    try
      {
	oldRefField = (InvidDBField) oldRef.getField(targetField);
      }
    catch (ClassCastException ex)
      {
	try
	  {
	    return Ganymede.createErrorDialog("InvidDBField.unbind(): Couldn't unlink old reference",
					      "Your operation could not succeed due to an error in the " +
					      "server's schema.  Target field " + oldRef.getField(targetField).getName() +
					      " in object " + oldRef.getLabel() +
					      " is not an invid field.");
	  }
	catch (RemoteException rx)
	  {
	    return Ganymede.createErrorDialog("InvidDBField.unbind(): Couldn't unlink old reference",
					      "Your operation could not succeed due to an error in the " +
					      "server's schema.  Target field " + targetField +
					      " in object " + oldRef.getLabel() +
					      " is not an invid field.");
	  }
      }

    if (oldRefField == null)
      {
	// editDBObject() will create undefined fields for all fields defined
	// in the DBObjectBase, so if we got a null result we have a schema
	// corruption problem.

	return Ganymede.createErrorDialog("InvidDBField.unbind(): Couldn't unlink old reference",
					  "Your operation could not succeed due to a possible inconsistency in the " +
					  "server database.  Target field number " + targetField +
					  " in object " + oldRef.getLabel() +
					  " does not exist, or you do not have permission to access " +
					  "this field.");
      }

    try
      {
	ReturnVal retVal = oldRefField.dissolve(owner.getInvid(), anon||local);

	if (retVal != null && !retVal.didSucceed())
	  {
	    return retVal;
	  }
      }
    catch (IllegalArgumentException ex)
      {
	System.err.println("hm, couldn't dissolve a reference in " + getName());

	if (anon)
	  {
	    System.err.println("Did do an anonymous edit on target");
	  }
	else
	  {
	    System.err.println("Didn't do an anonymous edit on target");
	  }

	throw (IllegalArgumentException) ex;
      }
	
    return null;
  }

  /**
   *
   * This method is used to effect the remote side of an unbind operation.<br><br>
   *
   * An InvidDBField being manipulated with the standard editing accessors
   * (setValue, addElement, deleteElement, setElement) will call this method
   * on another InvidDBField in order to unlink a pair of symmetrically bound
   * InvidDBFields.<br><br>
   *
   * This method will return false if the unbinding could not be performed for
   * some reason.
   *
   * @param oldInvid The invid to be unlinked from this field.  If this
   * field is not linked to the invid specified, nothing will happen.
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   *
   */

  synchronized ReturnVal dissolve(Invid oldInvid, boolean local)
  {
    int 
      index = -1;

    Invid tmp;

    DBEditObject eObj;

    /* -- */

    // NOTE: WE PROBABLY DON'T WANT TO CALL ISEDITABLE HERE, AS WE PROBABLY WANT
    // TO ALLOW DISSOLVE TO GO FORWARD EVEN IN CASES WHERE THE CURRENT USER WOULDN'T
    // BE ABLE TO EDIT THIS FIELD.. IF A USER'S BEING DELETED, THAT USER SHOULD BE
    // REMOVABLE FROM GROUPS AND WHATNOT REGARDLESS OF WHETHER THE SESSION WOULD HAVE
    // EDIT PERMISSION FOR THE GROUP.

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("dissolve called on non-editable field: " + getName() +
					   " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) owner;

    if (isVector())
      {
	for (int i = 0; i < values.size(); i++)
	  {
	    tmp = (Invid) values.elementAt(i);

	    if (tmp.equals(oldInvid))
	      {
		index = i;
		break;
	      }
	  }

	if (index == -1)
	  {
	    Ganymede.debug("warning: dissolve for " + 
			   owner.getLabel() + ":" + getName() + 
			   " called with an unbound invid (vector): " + 
			   oldInvid.toString());

	    return null;	// we're already dissolved, effectively
	  }

	if (eObj.finalizeDeleteElement(this, index))
	  {
	    values.removeElementAt(index);

	    defined = (values.size() > 0 ? true : false);

	    return null;
	  }
	else
	  {
	    setLastError("InvidDBField remote dissolve: couldn't finalizeDeleteElement");

	    return Ganymede.createErrorDialog("InvidDBField.dissolve(): couldn't finalizeDeleteElement",
					      "The custom plug-in class for object " + eObj.getLabel() +
					      "refused to allow us to clear out all the references in field " + 
					      getName());
	  }
      }
    else
      {
	tmp = (Invid) value;

	if (!tmp.equals(oldInvid))
	  {
	    throw new RuntimeException("dissolve called with an unbound invid (scalar)");
	  }

	if (eObj.finalizeSetValue(this, null))
	  {
	    value = null;
	    return null;
	  }
	else
	  {
	    setLastError("InvidDBField remote dissolve: couldn't finalizeSetValue");

	    return Ganymede.createErrorDialog("InvidDBField.dissolve(): couldn't finalizeSetValue",
					      "The custom plug-in class for object " + 
					      eObj.getLabel() +
					      "refused to allow us to clear out the reference in field " + 
					      getName());
	  }
      }
  }

  /**
   *
   * This method is used to effect the remote side of an bind operation.<br><br>
   *
   * An InvidDBField being manipulated with the standard editing accessors
   * (setValue, addElement, deleteElement, setElement) will call this method
   * on another InvidDBField in order to link a pair of symmetrically bound
   * InvidDBFields.<br><br>
   *
   * This method will return false if the binding could not be performed for
   * some reason.
   *
   * @param newInvid The invid to be linked to this field.
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   *
   */

  synchronized ReturnVal establish(Invid newInvid, boolean local)
  {
    Invid 
      tmp = null;

    DBEditObject eObj;
    
    ReturnVal retVal;

    /* -- */

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("establish called on non-editable field: " + getName() +
					   " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) owner;

    if (isVector())
      {
	if (size() >= getMaxArraySize())
	  {
	    setLastError("InvidDBField remote establish: vector overrun");

	    return Ganymede.createErrorDialog("InvidDBField.establish(): field overrun",
					      "Couldn't establish a new linkage in vector field " + getName() +
					      " in object " + getOwner().getLabel() +
					      "because the vector field is already at maximum capacity");
	  }

	if (eObj.finalizeAddElement(this, newInvid))
	  {
	    values.addElement(newInvid);
	    defined = true;

	    return null;
	  }
	else
	  {
	    setLastError("InvidDBField remote establish: finalize returned false");

	    return Ganymede.createErrorDialog("InvidDBField.establish(): field addvalue refused",
					      "Couldn't establish a new linkage in vector field " + getName() +
					      " in object " + getOwner().getLabel() +
					      "because the custom plug in code for this object refused to " +
					      "approve the operation.");
	  }
      }
    else
      {
	// ok, since we're scalar, *we* need to be unbound from *our* existing target
	// to be free to point back to our friend who is trying to establish a link
	// to us

	if (value != null)
	  {
	    tmp = (Invid) value;
	    
	    if (tmp.equals(newInvid))
	      {
		return null;	// already linked
	      }

	    retVal = unbind(tmp, local);

	    if (retVal != null && !retVal.didSucceed())
	      {
		return retVal;
	      }
	  }

	if (eObj.finalizeSetValue(this, newInvid))
	  {
	    value = newInvid;
	    defined = true;

	    return null;
	  }
	else
	  {
	    retVal = bind(null, tmp, local); // should always work

	    if (retVal != null && !retVal.didSucceed())	
	      {
		throw new RuntimeException("couldn't rebind a value " + tmp + " we just unbound.. sync error");
	      }

	    return  Ganymede.createErrorDialog("InvidDBField.establish(): field set value refused",
					       "Couldn't establish a new linkage in field " + getName() +
					       " in object " + getOwner().getLabel() +
					       "because the custom plug in code for this object refused to " +
					       "approve the operation.");
	  }
      }
  }

  /**
   *
   * This method tests to see if the invid's held in this InvidDBField
   * are properly back-referenced.
   *
   */

  synchronized boolean test(DBSession session, String objectName)
  {
    Invid temp = null;
    Invid myInvid = getOwner().getInvid();
    short targetField;
    DBObject target;
    InvidDBField backField;
    boolean result = true;

    /* -- */

    // find out what the back-pointer field in the target object is

    if (getFieldDef().isSymmetric())
      {
	targetField = getFieldDef().getTargetField();
      }
    else
      {
	targetField = SchemaConstants.BackLinksField;
      }

    if (isVector())
      {
	// test for all values in our vector

	for (int i = 0; i < values.size(); i++)
	  {
	    temp = (Invid) values.elementAt(i);

	    if (temp == null)
	      {
		Ganymede.debug("HEEEEEYYYYY!!!!!");
	      }

	    // find the object that this invid points to

	    target = session.viewDBObject(temp);

	    if (target == null)
	      {
		Ganymede.debug("*** InvidDBField.test(): Invid pointer to null object located: " + 
			       objectName + " in field " + getName());
		result = false;

		continue;
	      }

	    // find the field that should contain the back-pointer
	    
	    try
	      {
		backField = (InvidDBField) target.getField(targetField);
	      }
	    catch (ClassCastException ex)
	      {
		String fieldName = ((DBField) target.getField(targetField)).getName();

		Ganymede.debug("**** InvidDBField.test(): schema error!  back-reference field not an invid field!!\n\t>" +
			       owner.lookupLabel(target) + ":" + fieldName + ", referenced from " + objectName +
			       ":" + getName());
		result = false;

		continue;
	      }

	    if (backField == null || !backField.defined)
	      {
		Ganymede.debug("InvidDBField.test(): Null backField field in targeted field: " + 
			       objectName + " in field " + getName());
		result = false;

		continue;
	      }

	    if (backField.isVector())
	      {
		if (backField.values == null)
		  {
		    Ganymede.debug("*** InvidDBField.test(): Null back-link invid found for invid " + 
				   temp + " in object " + objectName + " in field " + getName());
		    result = false;
		    
		    continue;
		  }
		else
		  {
		    boolean found = false;
		    Invid testInv;

		    /* -- */

		    for (int j = 0; !found && (j < backField.values.size()); j++)
		      {
			testInv = (Invid) backField.values.elementAt(j);

			if (myInvid.equals(testInv))
			  {
			    found = true;
			  }
		      }

		    if (!found)
		      {
			Ganymede.debug("*** InvidDBField.test(): No back-link invid found for invid " + 
				       temp + " in object " + objectName + ":" + getName() + " in " + 
				       backField.getName());
			result = false;
			
			continue;
		      }
		  }
	      }
	    else
	      {
		if ((backField.value == null) || !(backField.value.equals(myInvid)))
		  {
		    Ganymede.debug("*** InvidDBField.test(): <scalar> No back-link invid found for invid " + 
				   temp + " in object " + objectName + " in field " + getName());
		    result = false;
		    
		    continue;
		  }
	      }
	  }
      }
    else
      {
	temp = (Invid) value;

	if (temp != null)
	  {
	    target = session.viewDBObject(temp);

	    if (target == null)
	      {
		Ganymede.debug("*** InvidDBField.test(): Invid pointer to null object located: " + objectName);
	    
		return false;
	      }

	    try
	      {
		backField = (InvidDBField) target.getField(targetField);
	      }
	    catch (ClassCastException ex)
	      {
		Ganymede.debug("**** InvidDBField.test(): schema error!  back-reference field not an invid field!! " +
			       "field: " + getName() + " in object " + objectName);

		return false;
	      }

	    if (backField == null || !backField.defined)
	      {
		Ganymede.debug("*** InvidDBField.test(): No proper back-reference field in targeted field: " + 
			       objectName + ":" + getName());
	    
		return false;
	      }
	
	    if (backField.isVector())
	      {
		if (backField.values == null)
		  {
		    Ganymede.debug("*** InvidDBField.test(): Null back-link invid found for invid " + 
				   temp + " in object " + objectName + " in field " + getName());
		    
		    return false;
		  }
		else
		  {
		    boolean found = false;
		    Invid testInv;

		    for (int j = 0; !found && (j < backField.values.size()); j++)
		      {
			testInv = (Invid) backField.values.elementAt(j);

			if (myInvid.equals(testInv))
			  {
			    found = true;
			  }
		      }

		    if (!found)
		      {
			Ganymede.debug(">>> InvidDBField.test(): No back-link invid found for invid " + 
				       temp + " in object " + objectName + ":" + getName() + " in " + 
				       backField.getName());

			return false;
		      }
		  }
	      }
	    else
	      {
		if ((backField.value == null) || !(backField.value.equals(myInvid)))
		  {
		    Ganymede.debug("*** InvidDBField.test(): <scalar> No back-link invid found for invid " + 
				   temp + " in object " + objectName + ":" + getName());
		    
		    return false;
		  }
	      }
	  }
      }

    return result;
  }

  // ****
  //
  // InvidDBField is a special kind of DBField in that we have symmetry
  // maintenance issues to handle.  We're overriding all DBField field-changing
  // methods to include symmetry maintenance code.
  //
  // ****

  /**
   *
   * Sets the value of this field, if a scalar.<br><br>
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.

   * @param value the value to set this field to.
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   *
   * @see arlut.csd.ganymede.DBSession
   *
   */

  public synchronized ReturnVal setValue(Object value, boolean local)
  {
    DBEditObject eObj;
    Invid oldRemote, newRemote;
    ReturnVal retVal = null, newRetVal;

    /* -- */

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object: " +
					   getName() + " in object " + owner.getLabel());
      }

    if (isVector())
      {
	throw new IllegalArgumentException("scalar method called on a vector field: " + getName() +
					   " in object " + owner.getLabel());
      }

    if (!verifyNewValue(value))
      {
	return Ganymede.createErrorDialog("Server: Error in InvidDBField.setValue()",
					  getLastError());
      }

    // we now know that value is an invid
    
    oldRemote = (Invid) this.value;
    newRemote = (Invid) value;

    eObj = (DBEditObject) owner;

    if (!local)
      {
	// Wizard check
	
	newRetVal = eObj.wizardHook(this, DBEditObject.SETVAL, value, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (newRetVal != null)
	  {
	    return newRetVal;
	  }
      }

    // try to do the binding

    if (newRemote != null)
      {
	newRetVal = bind(oldRemote, newRemote, local);

	if (newRetVal != null && !newRetVal.didSucceed())
	  {
	    return newRetVal;
	  }
      }
    else
      {
	if (oldRemote != null)
	  {
	    newRetVal = unbind(oldRemote, local);

	    if (newRetVal != null && !newRetVal.didSucceed())
	      {
		return newRetVal;
	      }
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    this.newValue = value;

    if (eObj.finalizeSetValue(this, value))
      {
	this.value = value;

	if (value == null)
	  {
	    defined = false;
	  }
	else
	  {
	    defined = true;
	  }

	this.newValue = null;

	return retVal;
      }
    else
      {
	this.newValue = null;

	setLastError("InvidDBField setValue: couldn't finalize");

	// we don't much care about the success of the following two
	// operations.. they really *should* work because they are
	// undoing what we just did, but we already have an error
	// condition to report.

	unbind(newRemote, local);
	bind(null, oldRemote, local);

	return Ganymede.createErrorDialog("Server: Error in InvidDBField.setValue()",
					  "InvidDBField setValue: couldn't finalize");
      }
  }

  /**
   *
   * Sets the value of an element of this field, if a vector.<br><br>
   *
   * The ReturnVal object returned encodes success or failure, and may
   * optionally pass back a dialog.<br><br>
   *
   * It is an error to call this method on an edit in place vector,
   * or on a scalar field.  An IllegalArgumentException will be thrown
   * in these cases.
   *
   * @param index The index of the element in this field to change.
   * @param value The value to put into this vector.
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   *
   * @see arlut.csd.ganymede.DBSession
   *
   */
  
  public synchronized ReturnVal setElement(int index, Object value, boolean local)
  {
    DBEditObject eObj;
    Invid oldRemote, newRemote;
    ReturnVal retVal = null, newRetVal;

    /* -- */

    if (isEditInPlace())
      {
	throw new IllegalArgumentException("can't manually set element in edit-in-place vector: " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object: " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field: " +
					   getName() + " in object " + owner.getLabel());
      }

    if ((index < 0) || (index > values.size()))
      {
	throw new IllegalArgumentException("invalid index " + (index) + 
					   getName() + " in object " + owner.getLabel());
      }

    if (!verifyNewValue(value))
      {
	return Ganymede.createErrorDialog("Server: Error in InvidDBField.setElement()",
					  getLastError());
      }

    eObj = (DBEditObject) owner;

    if (!local)
      {
	// Wizard check

	newRetVal = eObj.wizardHook(this, DBEditObject.SETELEMENT, new Integer(index), value);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (newRetVal != null)
	  {
	    return newRetVal;
	  }
      }

    oldRemote = (Invid) values.elementAt(index);
    newRemote = (Invid) value;
    
    // try to do the binding

    newRetVal = bind(oldRemote, newRemote, local);

    if (newRetVal != null && !newRetVal.didSucceed())
      {
	return newRetVal;
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    if (eObj.finalizeSetElement(this, index, value))
      {
	values.setElementAt(value, index);
	return retVal;
      }
    else
      {
	// we don't much care about the success of the following two
	// operations.. they really *should* work because they are
	// undoing what we just did, but we already have an error
	// condition to report.

	unbind(newRemote, local);
	bind(null, oldRemote, local);

	return Ganymede.createErrorDialog("Server: Error in InvidDBField.setElement()",
					  "InvidDBField setElement: couldn't finalize\n" +
					  getLastError());
      }
  }

  /**
   *
   * Adds an element to the end of this field, if a vector.<br><br>
   *
   * The ReturnVal object returned encodes success or failure, and may
   * optionally pass back a dialog.<br><br>
   *
   * It is an error to call this method on an edit in place vector,
   * or on a scalar field.  An IllegalArgumentException will be thrown
   * in these cases.
   *
   * @param value The value to put into this vector.
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   * 
   */

  public synchronized ReturnVal addElement(Object value, boolean local)
  {
    DBEditObject eObj;
    Invid remote;
    ReturnVal retVal = null, newRetVal;

    /* -- */

    if (isEditInPlace())
      {
	throw new IllegalArgumentException("can't manually add element to edit-in-place vector" +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isEditable(local))	// *sync* on GanymedeSession possible
      {
	setLastError("don't have permission to change field /  non-editable object");
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isVector())
      {
	setLastError("vector accessor called on scalar field");
	throw new IllegalArgumentException("vector accessor called on scalar field " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!verifyNewValue(value))
      {
	return Ganymede.createErrorDialog("InvidDBField.addElement() - bad value submitted",
					  getLastError());
      }

    if (size() >= getMaxArraySize())
      {
	setLastError("Field " + getName() + " already at or beyond array size limit");

	return Ganymede.createErrorDialog("InvidDBField.addElement() - vector overflow",
					  "Field " + getName() +
					  " already at or beyond array size limit");
      }

    remote = (Invid) value;

    eObj = (DBEditObject) owner;

    if (!local)
      {
	// Wizard check

	newRetVal = eObj.wizardHook(this, DBEditObject.ADDELEMENT, value, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (newRetVal != null)
	  {
	    return newRetVal;
	  }
      }

    newRetVal = bind(null, remote, local);

    if (newRetVal != null && !newRetVal.didSucceed())
      {
	return newRetVal;
      }

    if (eObj.finalizeAddElement(this, value)) 
      {
	values.addElement(value);

	defined = true;		// very important!

	return retVal;
      } 
    else
      {
	// we don't much care about the success of the following
	// operation.. it really *should* work because it is
	// undoing what we just did, but we already have an error
	// condition to report.

	unbind(remote, local);

	return Ganymede.createErrorDialog("InvidDBField.addElement() - custom logic reject",
					  "Couldn't finalize\n" + getLastError());
      }
  }

  /**
   *
   * Creates and adds a new embedded object in this
   * field, if it is an edit-in-place vector.<br><br>
   *
   * Returns an Invid pointing to the newly created
   * and appended embedded object, or null if
   * creation / addition was not possible.
   *
   * @see arlut.csd.ganymede.invid_field
   *
   */

  public Invid createNewEmbedded()
  {
    return createNewEmbedded(false);
  }

  /**
   *
   * Creates and adds a new embedded object in this
   * field, if it is an edit-in-place vector.<br><br>
   *
   * Returns an Invid pointing to the newly created and appended
   * embedded object, or null if creation / addition was not possible.
   *
   * @param local If true, we don't check permission to edit this
   * field before creating the new object.
   * 
   */

  public synchronized Invid createNewEmbedded(boolean local)
  {
    if (!isEditable(local))
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object: " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector method called on a scalar field " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isEditInPlace())
      {
	throw new IllegalArgumentException("edit-in-place method called on a referential invid field " +
					   getName() + " in object " + owner.getLabel());
      }

    if (size() >= getMaxArraySize())
      {
	setLastError("Field " + getName() + 
		     " already at or beyond array size limit");
	return null;
      }

    DBEditObject eObj = (DBEditObject) owner;

    // have our owner create a new embedded object
    // for us 

    Invid newObj = eObj.createNewEmbeddedObject(this);

    if (newObj == null)
      {
	return null;
      }

    // now we need to do the binding as appropriate
    // note that we assume that we don't need to verify the
    // new value

    DBEditObject embeddedObj = (DBEditObject) owner.editset.getSession().editDBObject(newObj); // *sync* DBSession DBObject

    if (embeddedObj == null)
      {
	throw new NullPointerException("gah, null embedded obj!");
      }

    // bind the object to its container.. note that ContainerField
    // is a standard built-in field for embedded objects and as
    // such it doesn't have the specific details as to the containing
    // object's binding recorded.  We'll have to do the bidirectional
    // binding ourselves, in two steps.

    // we have to use setFieldValueLocal() here because the
    // permissions system uses the ContainerField to determine rights
    // to modify the field.. since we are just now setting the
    // container, the permissions system will fail if we don't bypass
    // it by using the local variant.

    if (embeddedObj.setFieldValueLocal(SchemaConstants.ContainerField, // *sync* DBField
				       owner.getInvid()) != null)
      {
	setLastError("Couldn't bind reverse pointer");
	return null;
      }
    else if (debug)
      {
	InvidDBField invf = (InvidDBField)  embeddedObj.getField(SchemaConstants.ContainerField);
	System.err.println("-- Created a new embedded object in " + owner.getLabel() + 
			   ", set it's container pointer to " + invf.getValueString());
      }

    // finish the binding.  Note that we are directly modifying values
    // here rather than going to this.addElement().  If we did
    // this.addElement(), we might get a redundant attempt to do the
    // invid binding, as the containing field may indeed have the
    // reverse pointer in the object's container field specified in
    // the schema.  Doing it this way, we don't have to worry about
    // whether the admins got this part of the schema right.

    if (eObj.finalizeAddElement(this, newObj))
      {
	values.addElement(newObj);

	if (debug)
	  {
	    setLastError("InvidDBField debug: successfully added " + newObj);
	  }

	defined = true;		// very important!

	return newObj;
      } 
    else
      {
	embeddedObj.setFieldValue(SchemaConstants.ContainerField, null); // *sync* DBField
	return null;
      }
  }

  /**
   *
   * <p>Return the object type that this invid field is constrained to point to, if set</p>
   *
   * <p>-1 means there is no restriction on target type.</p>
   *
   * <p>-2 means there is no restriction on target type, but there is a specified symmetric field.</p>
   *
   * @see arlut.csd.ganymede.invid_field
   */

  public short getTargetBase()
  {
    return definition.getTargetBase();
  }

  /**
   *
   * Deletes an element of this field, if a vector.<br><br>
   *
   * Returns null on success, non-null on failure.<br><br>
   *
   * If non-null is returned, the ReturnVal object
   * will include a dialog specification that the
   * client can use to display the error condition.
   *
   */

  public synchronized ReturnVal deleteElement(int index, boolean local)
  {
    DBEditObject eObj;
    Invid remote;
    ReturnVal retVal = null, newRetVal;
    String checkKey;

    /* -- */

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " +
					   getName() + " in object " + owner.getLabel());
      }

    if ((index < 0) || (index >= values.size()))
      {
	throw new IllegalArgumentException("invalid index " + index + 
					   getName() + " in object " + owner.getLabel());
      }

    remote = (Invid) values.elementAt(index);

    checkKey = "del" + remote.toString();

    eObj = (DBEditObject) owner;

    if (!local)
      {
	// Wizard check

	newRetVal = eObj.wizardHook(this, DBEditObject.DELELEMENT, new Integer(index), null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (newRetVal != null)
	  {
	    return newRetVal;
	  }
      }

    // ok, we're going to handle it.  Checkpoint
    // so we can easily undo any changes that we make
    // if we have to return failure.

    if (debug)
      {
	System.err.println("][ InvidDBField.deleteElement() checkpointing " + checkKey);
      }

    eObj.getSession().checkpoint(checkKey);

    if (debug)
      {
	System.err.println("][ InvidDBField.deleteElement() checkpointed " + checkKey);
      }

    // if we are an edit in place object, we don't want to do an
    // unbinding.. we'll do a deleteDBObject() below, instead.  The
    // reason for this is that the deleteDBObject() code requires that
    // the SchemaConstants.ContainerField field be intact to properly
    // check permissions for embedded objects.

    if (!getFieldDef().isEditInPlace())
      {
	newRetVal = unbind(remote, local);

	if (newRetVal != null && !newRetVal.didSucceed())
	  {
	    return newRetVal;
	  }
      }

    if (eObj.finalizeDeleteElement(this, index))
      {
	values.removeElementAt(index);

	// if we are an editInPlace field, unlinking this object means
	// that we should go ahead and delete the object.

	if (getFieldDef().isEditInPlace())
	  {
	    retVal = eObj.getSession().deleteDBObject(remote);
	  }

	if (retVal != null && !retVal.didSucceed())
	  {
	    eObj.getSession().rollback(checkKey);
	    return retVal;	// go ahead and return our error code
	  }

	// success

	if (values.size() == 0)
	  {
	    defined = false;
	  }

	eObj.getSession().popCheckpoint(checkKey);
	return retVal;
      }
    else
      {
	eObj.getSession().rollback(checkKey);

	return Ganymede.createErrorDialog("InvidDBField.deleteElement() - custom code rejected element deletion",
					  "Couldn't finalize\n" + getLastError());
      }
  }

  // ****
  //
  // invid_field methods
  //
  // ****

  /**
   *
   * Returns true if this invid field may only point to objects
   * of a particular type.
   * 
   * @see arlut.csd.ganymede.invid_field 
   *
   */

  public boolean limited()
  {
    return definition.isTargetRestricted();
  }

  /**
   *
   * Returns the object type permitted for this field if this invid
   * field may only point to objects of a particular type.
   * 
   * @see arlut.csd.ganymede.invid_field 
   * 
   */

  public int getAllowedTarget()
  {
    return definition.getTargetBase();
  }

  /**
   *
   * Returns a QueryResult encoded list of the current values
   * stored in this field.
   *
   * @see arlut.csd.ganymede.invid_field
   *
   */

  public synchronized QueryResult encodedValues()
  {
    QueryResult results = new QueryResult();
    Invid invid;
    String label;
    DBObject object;
    GanymedeSession gsession = null;

    /* -- */

    if (!isVector())
      {
	throw new IllegalArgumentException("can't call encodedValues on scalar field: " +
					   getName() + " in object " + owner.getLabel());
      }

    try
      {
	if (owner.editset != null)
	  {
	    gsession = owner.editset.getSession().getGSession();
	  }
      }
    catch (NullPointerException ex)
      {
      }

    if (gsession == null)
      {
	gsession = Ganymede.internalSession;
      }

    for (int i = 0; i < values.size(); i++)
      {
	invid = (Invid) values.elementAt(i);

	if (gsession != null)
	  {
	    object = gsession.getSession().viewDBObject(invid);
	    label = owner.lookupLabel(object);
	  }
	else
	  {
	    label = invid.toString();
	  }
	
	if (label != null)
	  {
	    results.addRow(invid, label, false); // we're not going to report the values as editable here
	  }
      }

    return results;
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
   *
   * Returns a StringBuffer encoded list of acceptable invid values
   * for this field.
   *
   * @see arlut.csd.ganymede.invid_field
   * 
   */

  public QueryResult choices()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
	throw new IllegalArgumentException("not an editable field: " + 
					   getName() + " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) owner;

    return eObj.obtainChoiceList(this);
  }

  /**
   *
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.
   *
   * If there is no caching key, this method will return null.
   *
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

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof Invid));
  }

  public boolean verifyNewValue(Object o)
  {
    DBEditObject eObj;
    Invid i;

    /* -- */

    if (!isEditable(true))
      {
	setLastError("object/field not editable");
	return false;
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	setLastError("type mismatch");
	return false;
      }

    i = (Invid) o;

    if (i == null)
      {
	return eObj.verifyNewValue(this, o);
      }

    if (limited() && (getAllowedTarget() != -2) &&
	(i.getType() != getAllowedTarget()))
      {
	// the invid points to an object of the wrong type

	setLastError("invid value " + i + 
		     " points to the wrong kind of" +
		     " object for field " +
		     getName() +
		     " which should point to an" +
		     " object of type " + 
		     getAllowedTarget());
	return false;
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }
}
