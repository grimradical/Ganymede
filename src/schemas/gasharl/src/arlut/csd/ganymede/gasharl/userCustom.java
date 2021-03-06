/*

   userCustom.java

   This file is a management class for user objects in Ganymede.

   Created: 30 July 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import org.doomdark.uuid.EthernetAddress;
import org.doomdark.uuid.UUIDGenerator;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.FileOps;
import arlut.csd.Util.PathComplete;
import arlut.csd.Util.RandomUtils;
import arlut.csd.Util.StringUtils;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.GanyParseException;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBEditSet;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.DBLog;
import arlut.csd.ganymede.server.DBNameSpace;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.DBObjectBase;
import arlut.csd.ganymede.server.DBObjectBaseField;
import arlut.csd.ganymede.server.DBSession;
import arlut.csd.ganymede.server.DateDBField;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.InvidDBField;
import arlut.csd.ganymede.server.NumericDBField;
import arlut.csd.ganymede.server.PasswordDBField;
import arlut.csd.ganymede.server.StringDBField;
import arlut.csd.ganymede.server.adminPersonaCustom;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      userCustom

------------------------------------------------------------------------------*/

/**
 * This class is the custom plug-in to handle the user object type in the
 * Ganymede server.  It does special validations of operations on the user,
 * handles inactivation and reactivation logic, and generates Wizards as
 * needed.
 *
 * See the userSchema.java file for a list of field definitions that this
 * module expects to work with.
 *
 * @see arlut.csd.ganymede.gasharl.userSchema
 * @see arlut.csd.ganymede.server.DBEditObject
 *
 */

public class userCustom extends DBEditObject implements SchemaConstants, userSchema {

  static final boolean debug = false;

  static QueryResult shellChoices = new QueryResult();
  static Date shellChoiceStamp = null;

  static String mailsuffix = null;
  static String homedir = null;

  static String renameFilename = null;
  static File renameHandler = null;

  static String createFilename = null;
  static File createHandler = null;

  static String deleteFilename = null;
  static File deleteHandler = null;

  static final int lowUID = 2001;

  static int file_identifier = 0;

  public static synchronized int getNextAuthIdent()
  {
    if (file_identifier == Integer.MAX_VALUE)
      {
	file_identifier = 0;
      }
    else
      {
	file_identifier++;
      }

    return file_identifier;
  }

  public static File getNextFileName()
  {
    return new File("/tmp/ganymede_ext_validate_" + getNextAuthIdent());
  }

  // ---

  QueryResult groupChoices = null;

  String newUsername = null;

  private boolean amChangingExpireDate = false;

  /**
   * Private flag used to allow us to return an error dialog the first
   * time we detect an HR/IRIS problem.  Once we've presented an HR
   * error to the admin, we'll set this flag and disregard the problem
   * if the admin hits commit again in the client without changing
   * anything.
   */

  private boolean IRISWarningGiven = false;

  /**
   *
   * Customization Constructor
   *
   */

  public userCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public userCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public userCustom(DBObject original, DBEditSet editset)
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
    try
      {
	ReturnVal retVal;
	Random rand = new Random();
	Integer uidVal = null;

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
	    return Ganymede.createErrorDialog("User Initialization Failure",
					      "Couldn't find the guid field.. schema problem?");
	  }

	String guid = generateGUID(); // create a globally unique uid

	retVal = guidField.setValueLocal(guid);

	if (!ReturnVal.didSucceed(retVal))
	  {
	    return retVal;
	  }

	// need to find a uid for this user

	NumericDBField numField = (NumericDBField) getField(UID);

	if (numField == null)
	  {
	    return Ganymede.createErrorDialog("User Initialization Failure",
					      "Couldn't find the uid field.. schema problem?");
	  }

	DBNameSpace namespace = numField.getNameSpace();

	if (namespace == null)
	  {
	    return Ganymede.createErrorDialog("User Initialization Failure",
					      "Couldn't find the uid namespace.. schema problem?");
	  }

	// now, find a uid.. unfortunately, we have to use immutable Integers here.. not
	// the most efficient at all.

	int count = 0;
	uidVal = new Integer(rand.nextInt(31767) + lowUID);

	while (!namespace.reserve(getEditSet(), uidVal) && count < 30000)
	  {
	    uidVal = new Integer(rand.nextInt(31767) + lowUID);
	    count++;
	  }

	if (count > 30000)
	  {
	    // we've been looping too long, maybe there's no
	    // uid's free?  let's do an exhaustive search

	    uidVal = new Integer(lowUID);

	    while (!namespace.reserve(getEditSet(), uidVal))
	      {
		uidVal = new Integer(uidVal.intValue() + 1);

		if (uidVal.intValue() > 32767)
		  {
		    throw new RuntimeException("Couldn't find an allocatable uid through random search");
		  }
	      }
	  }

	// we use setValueLocal so we can set a value that the user can't edit.

	retVal = numField.setValueLocal(uidVal);

	if (!ReturnVal.didSucceed(retVal))
	  {
	    return retVal;
	  }

	// set the new user account to type 'normal'.

	InvidDBField catf = (InvidDBField) getField(userSchema.CATEGORY);

	Invid normalCat = getGSession().findLabeledObject("normal", userCategorySchema.BASE);

	retVal = catf.setValueLocal(normalCat, true);

	if (!ReturnVal.didSucceed(retVal))
	  {
	    return retVal;
	  }

	// create a volume entry for the user.

	InvidDBField invf = (InvidDBField) getField(userSchema.VOLUMES);

	try
	  {
	    retVal = invf.createNewEmbedded(true);
	  }
	catch (GanyPermissionsException ex)
	  {
	    return Ganymede.createErrorDialog("permissions", "permissions error creating embedded object" + ex);

	  }

	if ((retVal == null) || (!retVal.didSucceed()))
	  {
	    return retVal;
	  }

	Invid invid = retVal.getInvid();

	if (invid != null)
	  {
	    // find the auto.home.default map, if we can.

	    Vector results = getGSession().internalQuery(new Query((short) 277, // automounter map
								   new QueryDataNode(QueryDataNode.EQUALS,
										     "auto.home.default"),
								   false));

	    // if we found auto.home.default, set the new volume entry map
	    // field to point to auto.home.default.

	    if (results != null && results.size() == 1)
	      {
		Result objid = (Result) results.elementAt(0);

		DBEditObject eObj = getDBSession().editDBObject(invid);
		invf = (InvidDBField) eObj.getField(mapEntrySchema.MAP);

		retVal = invf.setValueLocal(objid.getInvid());

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }

		// we want the permissions system to reject edit privs
		// on this now.. by setting permCache to null, we allow
		// the mapEntryCustom permOverride method to get a chance
		// to refuse edit privileges.

		eObj.clearFieldPerm(mapEntrySchema.MAP);
	      }
	  }

	return null;
      }
    catch (NotLoggedInException ex)
      {
	return Ganymede.loginError(ex);
      }
  }

  /**
   * <p>Private method to create a globally unique UID value suitable
   * for certain LDAP applications</p>
   */

  private String generateGUID()
  {
    UUIDGenerator gen = UUIDGenerator.getInstance();
    org.doomdark.uuid.UUID guid = gen.generateTimeBasedUUID(new EthernetAddress("00:11:43:D5:F7:F8")); // csdsun9

    return guid.toString();
  }

  /**
   * <p>This method provides a pre-commit hook that runs after the user
   * has hit commit but before the system has established write locks
   * for the commit.</p>
   *
   * <p>The intended purpose of this hook is to allow objects that
   * dynamically maintain hidden label fields to update those fields
   * from the contents of the object's other fields at commit time.</p>
   *
   * <p>This method runs in a checkpointed context.  If this method fails
   * in any operation, you should return a ReturnVal with a failure
   * dialog encoded, and the transaction's commit will be blocked and
   * a dialog explaining the problem will be presented to the user.</p>
   */

  public ReturnVal preCommitHook()
  {
    if (this.getStatus() == ObjectStatus.DELETING ||
	this.getStatus() == ObjectStatus.DROPPING)
      {
	return null;
      }

    ReturnVal retVal = renameEntries(this.getLabel());

    if (!ReturnVal.didSucceed(retVal))
      {
	return retVal;
      }

    // now check to make sure that the username/badge combination
    // doesn't conflict with something in IRIS' history.
    //
    // we do this in preCommitHook() so that we can check the final
    // status of the username and badge information since we need both
    // to make an intelligent check.

    boolean needBadgeNameCheck = false;

    if (this.getStatus() == ObjectStatus.CREATING)
      {
	needBadgeNameCheck = true;
      }
    else
      {
	Set<DBObjectBaseField> fieldsChanged = new HashSet<DBObjectBaseField>();
	String ignoreResult = diff(fieldsChanged);

	if (fieldsChanged.contains(getFieldDef(userSchema.USERNAME)) ||
	    fieldsChanged.contains(getFieldDef(userSchema.BADGE)) ||
	    fieldsChanged.contains(getFieldDef(userSchema.CATEGORY)))
	  {
	    needBadgeNameCheck = true;
	  }
      }

    Invid category = (Invid) this.getFieldValueLocal(userSchema.CATEGORY);
    DBObject categoryObj = lookupInvid(category, false);

    if (categoryObj == null)
      {
	// The fieldRequired() method is run after preCommitHook(), so
	// this could be null here.
	//
	// If null, we'll just return early and let DBEditSet call
	// checkRequiredFields() on this later to report the error.

	return null;
      }

    String categoryName = categoryObj.getLabel();

    if (!categoryName.equals("normal"))
      {
	needBadgeNameCheck = false;
      }

    if (needBadgeNameCheck)
      {
	String username = (String) getFieldValueLocal(userSchema.USERNAME);
	String badge = (String) getFieldValueLocal(userSchema.BADGE);

	// if we have a SQL exception thrown, we'll catch and log it,
	// but we won't block user creation / edit.

	try
	  {
	    if (!IRISLink.isRegisteredBadgeNumber(badge))
	      {
		if (!IRISWarningGiven)
		  {
		    IRISWarningGiven = true;

		    return ReturnVal.merge(retVal, Ganymede.createErrorDialog("Warning: Badge number not in HR database",
									      "The " + username + " user object is currently registered as having badge number " +
									      badge + ", which is not registered in the HR database.\n\n" +
									      "You should either correct the badge number or contact HR to find out why the badge " +
									      "is not properly recorded in HR's database."));
		  }
	      }

	    if (!IRISLink.okayToUseName(username, badge))
	      {
		String badgeConflict = IRISLink.findHistoricalBadge(username);
		String fullNameConflict = IRISLink.findHistoricalEmployeeName(username);

		if (!IRISWarningGiven)
		  {
		    IRISWarningGiven = true;

		    return ReturnVal.merge(retVal, Ganymede.createErrorDialog("Warning: Historical username conflict",
									     "The '" + username +
									     "' user object (with badge id '" + badge + "') conflicts with an earlier '" + username +
									     "' account that is still referenced in the HR database.\n\n" +
									     "The previous account was owned by employee '" + fullNameConflict +
									     "', with badge id '" + badgeConflict +
									     "'.\n\n" +
									     "You should change the username or badge id in the '" + username + "' object in order " +
									     "to resolve this conflict.\n\n" +
									     "You could also change this user object's user category to something other than 'normal'.\n\n" +
									      "If you feel this message is in error, hit commit again to proceed."));
		  }
	      }
	  }
	catch (Exception ex)
	  {
	    Ganymede.logError(ex);
	  }
      }

    // Send out mail describing the external credentials for this
    // user, if such are defined

    if (isSet(userSchema.ALLOWEXTERNAL) && isDefined(userSchema.MAILUSER) && isDefined(userSchema.MAILPASSWORD2))
      {
	DBObject originalObject = getOriginal();
	String titleString = null;
	String messageString = null;
	String expireString = null;

	String mailUsername = (String) getFieldValueLocal(userSchema.MAILUSER);
	PasswordDBField mailPasswordField = (PasswordDBField) getField(userSchema.MAILPASSWORD2);
	String mailPassword = mailPasswordField.getPlainText();

	if (mailPassword == null)
	  {
	    mailPassword = "";	// shouldn't need this if MAILPASSWORD2 is configured properly
	  }

	Date mailExpireDate = (Date) getFieldValueLocal(userSchema.MAILEXPDATE);

	if (originalObject == null || !originalObject.isDefined(userSchema.MAILUSER) || !originalObject.isDefined(userSchema.MAILPASSWORD2))
	  {
	    titleString = "External Email Credentials Set For User " + this.getLabel();

	    messageString = "User account " + this.getLabel() +
	      " has been granted access to laboratory email from outside the internal ARL:UT network.\n\n" +

	      "In order to read and send mail from outside the laboratory, you will need to configure your external email client " +
	      "to send outgoing email through smail.arlut.utexas.edu using TLS-encrypted SMTP on port 25 or port 587, and to " +
	      "read incoming mail from mailboxes.arlut.utexas.edu via IMAP over SSL.\n\n" +

	      "You will need to specify the following randomly assigned user name and password for both services:\n\n" +

	      "Username: " + mailUsername + "\n" +
	      "Password: " + mailPassword;

	    if (isDefined(userSchema.MAILEXPDATE))
	      {
		messageString = messageString + "\n\nThese credentials will expire on " + mailExpireDate.toString() +
		  ".  You will be assigned new credentials for your external mail access four weeks before these credentials expire.";
	      }
	  }
	else
	  {
	    PasswordDBField oldMailPasswordField = (PasswordDBField) originalObject.getField(userSchema.MAILPASSWORD2);
	    String oldPassword = oldMailPasswordField.getPlainText();

	    if (!mailUsername.equals(originalObject.getFieldValueLocal(userSchema.MAILUSER)) ||
		!mailPassword.equals(oldPassword))
	      {
		PasswordDBField myOldPasswordField = (PasswordDBField) this.getField(userSchema.OLDMAILPASSWORD2);
		String myOldPassword = myOldPasswordField != null ? myOldPasswordField.getPlainText(): "";

		if (this.getGSession().getSessionName().equals("ExternalMailTask") && myOldPassword.equals(oldPassword))
		  {
		    // we're processing a credentials renewal by ExternalMailTask

		    titleString = "New Email Credentials Created For User " + this.getLabel();

		    messageString = "The current external mail credentials for user account " + this.getLabel() +
		      " are due to expire in four weeks.\n\n" +

		      "New external email credentials have been prepared for you:\n\n" +

		      "Username: " + mailUsername + "\n" +
		      "Password: " + mailPassword + "\n\n" +

		      "These credentials are now active on your account, and should be entered into any email client " +
		      "that you use outside of the laboratory's internal network.\n\n" +

		      "Your current external email credentials will continue to function for one month, after " +
		      "which time they will be removed from your account.";
		  }
		else
		  {
		    titleString = "External Email Credentials Changed For User " + this.getLabel();

		    messageString = "The external mail credentials for user account " + this.getLabel() +
		      " have been changed.\n\n" +

		      "In order to continue to read and send mail from outside the laboratory, you will need to configure your external email client " +
		      "to send outgoing email through smail.arlut.utexas.edu using TLS-encrypted SMTP on port 25 or port 587, and to " +
		      "read incoming mail from mailboxes.arlut.utexas.edu via IMAP over SSL.\n\n" +

		      "You will need to specify the following randomly assigned user name and password for both services:\n\n" +

		      "Username: " + mailUsername + "\n" +
		      "Password: " + mailPassword;


		    if (isDefined(userSchema.MAILEXPDATE))
		      {
			messageString = messageString + "\n\nThese credentials will expire on " + mailExpireDate.toString() +
			  ".  You will be assigned new credentials for your external mail access four weeks before these credentials expire.";
		      }
		  }
	      }
	  }

	if (titleString != null)
	  {
	    Vector objVect = new Vector();

	    objVect.addElement(this.getInvid());

	    // we want to sent to the user but not to the owners

	    Ganymede.log.sendMail(null, titleString, messageString, DBLog.MailMode.USERS, objVect);
	  }
      }

    return retVal;
  }

  /**
   * This private helper method goes through all embedded automounter
   * map entries and updates their hidden labels, using the newName as
   * the prefix.
   */

  private ReturnVal renameEntries(String newName)
  {
    ReturnVal retVal = null;
    InvidDBField volumeMapEntries = (InvidDBField) getField(userSchema.VOLUMES);
    Vector values = volumeMapEntries.getValuesLocal();

    for (int i = 0; i < values.size(); i++)
      {
	Invid entryInvid = (Invid) values.elementAt(i);

	DBEditObject eObj = getDBSession().editDBObject(entryInvid);

	Invid mapInvid = (Invid) eObj.getFieldValueLocal(mapEntrySchema.MAP);

	if (mapInvid == null)
	  {
	    return Ganymede.createErrorDialog("Can't commit transaction with an empty automounter map definition in user " + this.getLabel());
	  }

	DBObject mapObj = getDBSession().viewDBObject(mapInvid);

	String mapName = mapObj.getLabel();

	retVal = eObj.setFieldValueLocal(mapEntrySchema.XMLLABEL, newName + "/" + mapName);

	if (retVal != null && !retVal.didSucceed())
	  {
	    return retVal;
	  }
      }

    return null;
  }

  /**
   * <p>This method provides a hook that can be used to indicate whether
   * a field that is defined in this object's field dictionary
   * should be newly instantiated in this particular object.</p>
   *
   * <p>This method does not affect those fields which are actually present
   * in the object's record in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.  What this method allows
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
    if (fieldID == userSchema.PASSWORDCHANGETIME)
      {
	return true;
      }

    return super.instantiateNewField(fieldID);
  }

  /**
   * Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of
   * {@link arlut.csd.ganymede.server.DBField DBField} will
   * wind up calling up to here to let us override the normal visibility
   * process.
   *
   * Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.
   *
   * If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.
   *
   * To be overridden on necessity in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canSeeField(DBSession dbSession, DBField field)
  {
    short fieldid = field.getID();

    switch (fieldid)
      {
      case MAILUSER:
      case MAILPASSWORD2:
      case MAILEXPDATE:
	return field.getObject().isSet(ALLOWEXTERNAL);

      case OLDMAILUSER:
      case OLDMAILPASSWORD2:
	return false;

      case EXCHANGESTORE:
	String type = (String) field.getObject().getFieldValueLocal(EMAILACCOUNTTYPE);

	if (type == null || !type.equals("Exchange"))
	  {
	    return false;
	  }
      }

    return super.canSeeField(dbSession, field);
  }

  /**
   * Controls whether change information for the field can be safely
   * logged to the Ganymede log.
   *
   * If okToLogField() returns false for a field, the change to the
   * field will be mentioned in the log, but the actual values will
   * not be.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean okToLogField(DBField field)
  {
    short fieldid = field.getID();

    switch (fieldid)
      {
      case MAILUSER:
      case OLDMAILUSER:
	return false;
      }

    // MAILPASSWORD2 and OLDMAILPASSWORD2 are password fields, and
    // Ganymede will already keep that sensitive data from the logs
    // and transaction email.

    return super.okToLogField(field);
  }

  /**
   * <p>Customization method to verify whether a specific field
   * in object should be cloned using the basic field-clone
   * logic.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canCloneField(DBSession dbSession, DBObject object, DBField field)
  {
    short fieldid = field.getID();

    switch (fieldid)
      {
      case USERNAME:
      case UID:
      case CATEGORY:		// but see special logic in cloneFromObject()
      case userSchema.PASSWORD:
      case HOMEDIR:
      case PERSONAE:
      case ALIASES:
      case SIGNATURE:
      case EMAILTARGET:
      case PASSWORDCHANGETIME:
      case MAILUSER:
      case MAILPASSWORD2:
      case OLDMAILUSER:
      case OLDMAILPASSWORD2:
	return false;
      }

    // by default, all custom fields are cloneable, so this call will
    // return true for all but the built-in fields.

    return super.canCloneField(dbSession, object, field);
  }

  /**
   * Hook to allow the cloning of an object.  If this object type
   * supports cloning (which should be very much customized for this
   * object type.. creation of the ancillary objects, which fields to
   * clone, etc.), this customization method will actually do the work.
   *
   * This method is called on a newly created object, in order
   * to clone the state of origObj into it.  This method does not actually
   * create a new object.. that is handled by
   * {@link arlut.csd.ganymede.server.GanymedeSession#clone_db_object(arlut.csd.ganymede.common.Invid) clone_db_object()}
   * before this method is called on the newly created object.
   *
   * The default (DBEditObject) implementation of this method will only clone
   * fields for which
   * {@link arlut.csd.ganymede.server.DBEditObject#canCloneField(arlut.csd.ganymede.server.DBSession,
   * arlut.csd.ganymede.server.DBObject, arlut.csd.ganymede.server.DBField) canCloneField()}
   * returns true, and which are not connected to a namespace (and thus could not
   * possibly be cloned).
   *
   * If one or more fields in the original object are unreadable by the cloning
   * session, we will provide a list of fields that could not be cloned due to
   * a lack of read permissions in a dialog in the ReturnVal.  Such a problem will
   * not result in a failure code being returned, however.. the clone will succeed,
   * but an informative dialog will be provided to the user.
   *
   * To be overridden on necessity in DBEditObject subclasses, but
   * this method's default logic will probably do what you need it to
   * do.  If you need to make changes, try to chain your subclassed
   * method to this one via super.cloneFromObject().
   *
   * @param dbSession The DBSession that the new object is to be created in
   * @param origObj The object we are cloning
   * @param local If true, fields that have choice lists will not be checked against
   * those choice lists and read permissions for each field will not be consulted.
   * The canCloneField() method will still be consulted, however.
   *
   * @return A standard ReturnVal status object.  May be null on success, or
   * else may carry a dialog with information on problems and a success flag.
   */

  public ReturnVal cloneFromObject(DBSession dbSession, DBObject origObj, boolean local)
  {
    try
      {
	if (debug)
	  {
	    System.err.println("Attempting to clone User " + origObj.getLabel());
	  }

	boolean problem = false;
	ReturnVal tmpVal;
	StringBuilder resultBuf = new StringBuilder();

        // clone all of the fields that we don't inhibit in canCloneField().

	ReturnVal retVal = super.cloneFromObject(dbSession, origObj, local);

	if (retVal != null && retVal.getDialog() != null)
	  {
	    resultBuf.append(retVal.getDialog().getText());

	    problem = true;
	  }

	// We have the default canCloneField() refuse to clone
	// userSchema.CATEGORY to avoid dealing or bypassing with the
	// wizard.  If we are cloning a normal user, it is safe enough
	// to copy that value.  Else we'll leave it blank for the user
	// to set.

	Invid category = (Invid) origObj.getFieldValueLocal(userSchema.CATEGORY);

	if (dbSession.getObjectLabel(category).equals("normal"))
	  {
	    try
	      {
		((DBField) getField(userSchema.CATEGORY)).setValue(category, local, true);
	      }
	    catch (GanyPermissionsException ex)
	      {
		return Ganymede.createErrorDialog("permissions", "permissions error setting category" + ex);
	      }
	  }

	if (debug)
	  {
	    System.err.println("User " + origObj.getLabel() + " cloned, working on embeddeds");
	  }

	// and clone the embedded objects

	InvidDBField newVolumes = (InvidDBField) getField(userSchema.VOLUMES);
	InvidDBField oldVolumes = (InvidDBField) origObj.getField(userSchema.VOLUMES);

	Vector newOnes;
	Vector oldOnes;

	if (local)
	  {
	    newOnes = (Vector) newVolumes.getValuesLocal().clone();
	    oldOnes = (Vector) oldVolumes.getValuesLocal().clone();
	  }
	else
	  {
	    newOnes = newVolumes.getValuesLocal();
	    oldOnes = oldVolumes.getValuesLocal();
	  }

	DBObject origVolume;
	DBEditObject workingVolume;
	int i;

	for (i = 0; i < newOnes.size(); i++)
	  {
	    if (debug)
	      {
		System.err.println("User clone sub " + i);
	      }

	    workingVolume = (DBEditObject) dbSession.editDBObject((Invid) newOnes.elementAt(i));
	    origVolume = dbSession.viewDBObject((Invid) oldOnes.elementAt(i));

	    if (debug)
	      {
		System.err.println("Attempting to clone user volume " + origVolume.getLabel());
	      }

	    tmpVal = workingVolume.cloneFromObject(dbSession, origVolume, local);

	    if (tmpVal != null && tmpVal.getDialog() != null)
	      {
		if (resultBuf.length() != 0)
		  {
		    resultBuf.append("\n\n");
		  }

		resultBuf.append(tmpVal.getDialog().getText());

		problem = true;
	      }
	  }

	Invid newInvid;

	if (i < oldOnes.size())
	  {
	    for (; i < oldOnes.size(); i++)
	      {
		if (debug)
		  {
		    System.err.println("User clone sub sub " + i);
		  }

		try
		  {
		    tmpVal = newVolumes.createNewEmbedded(local);
		  }
		catch (GanyPermissionsException ex)
		  {
		    tmpVal = Ganymede.createErrorDialog("permissions", "permissions error creating embedded object during user cloning" + ex);
		  }

		if (!tmpVal.didSucceed())
		  {
		    if (debug)
		      {
			System.err.println("User clone couldn't allocate new embedded");
		      }

		    if (tmpVal != null && tmpVal.getDialog() != null)
		      {
			if (resultBuf.length() != 0)
			  {
			    resultBuf.append("\n\n");
			  }

			resultBuf.append(tmpVal.getDialog().getText());

			problem = true;
		      }
		    continue;
		  }

		newInvid = tmpVal.getInvid();

		workingVolume = (DBEditObject) dbSession.editDBObject(newInvid);
		origVolume = dbSession.viewDBObject((Invid) oldOnes.elementAt(i));

		if (debug)
		  {
		    System.err.println("Attempting to clone user volume " + origVolume.getLabel());
		  }

		tmpVal = workingVolume.cloneFromObject(dbSession, origVolume, local);

		if (tmpVal != null && tmpVal.getDialog() != null)
		  {
		    if (resultBuf.length() != 0)
		      {
			resultBuf.append("\n\n");
		      }

		    resultBuf.append(tmpVal.getDialog().getText());

		    problem = true;
		  }
	      }
	  }

	retVal = new ReturnVal(true, !problem);

	if (problem)
	  {
	    retVal.setDialog(new JDialogBuff("Possible Clone Problems", resultBuf.toString(),
					     "Ok", null, "ok.gif"));
	  }

	return retVal;
      }
    catch (NotLoggedInException ex)
      {
	return Ganymede.loginError(ex);
      }
  }

  /**
   * <p>This method provides a hook to allow custom DBEditObject subclasses to
   * indicate that the given object is interested in receiving notification
   * when changes involving it occur, and can provide one or more addresses for
   * such notification to go to.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean hasEmailTarget(DBObject object)
  {
    return true;
  }

  /**
   * <p>This method provides a hook to allow custom DBEditObject subclasses to
   * return a Vector of Strings comprising a list of addresses to be
   * notified above and beyond the normal owner group notification when
   * the given object is changed in a transaction.  Used for letting end-users
   * be notified of changes to their account, etc.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public Vector getEmailTargets(DBObject object)
  {
    // don't tell this user's email address if this user is in the process
    // of being created.  this will avoid causing email to be sent to
    // the newly created account, which would likely bounce at this point
    // in time

    if (object instanceof DBEditObject)
      {
	if (((DBEditObject) object).getStatus() == ObjectStatus.CREATING)
	  {
	    return null;
	  }
      }

    Vector x = new Vector();

    x.add(object.getLabel());	// let our mail system handle routing.

    return x;
  }

  /**
   * This method provides a hook to allow custom DBEditObject
   * subclasses to return a String containing a URL for an image to
   * represent this object.  Intended to be used for users, primarily.
   *
   * To be overridden on necessity in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public String getImageURLForObject(DBObject object)
  {
    String badge = (String) object.getFieldValueLocal(userSchema.BADGE);

    if (badge == null || badge.trim().equals(""))
      {
        return null;
      }

    return "http://csdsun9.arlut.utexas.edu/pictures/" + badge + ".jpg";
  }

  /**
   * This method provides a hook to allow custom DBEditObject
   * subclasses to react to forthcoming object removal.
   *
   * This method will be called without benefit of an open DBEditSet,
   * so any email generated will need to make use of the
   * non-transactional mail methods in the Ganymede.log object.
   *
   * To be overridden on necessity in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   * @return true if the DBEditObject subclass wishes to completely
   * handle the warning, or false if the default warning transmisssion
   * logic should also be sent.
   */

  public boolean reactToRemovalWarning(DBObject object, int days)
  {
    StringDBField deliveryAddresses = null;
    Vector values = null;

    deliveryAddresses = (StringDBField) object.getField(userSchema.EMAILTARGET);

    if (deliveryAddresses == null)
      {
	Ganymede.debug("Missing email target for user " + object.getLabel());

	return false;
      }

    values = deliveryAddresses.getValuesLocal();

    for (int i = 0; i < values.size(); i++)
      {
        String x = (String) values.elementAt(i);

        if (x.endsWith("@arlex.arlut.utexas.edu"))
          {
            Vector toAddresses = new Vector();
            toAddresses.addElement("pcshelp@arlut.utexas.edu");
            toAddresses.addElement("broccol@arlut.utexas.edu");

            Ganymede.log.sendMail(toAddresses,
                                  "Exchange User " + object.getLabel() + " Scheduled for Deletion",
                                  "User " + object.getLabel() +
                                  " is scheduled to be deleted from Ganymede in " + days +
                                  " days.\n\nPCS will need to be prepared for clearing the account out of the Exchange server.\n");
            return false;
          }
      }

    return false;
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * make a link to the given field in this
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p>This version of anonymousLinkOK takes additional parameters
   * to allow an object type to decide that it does or does not want
   * to allow a link based on what field of what object wants to link
   * to it.</P>
   *
   * <p>By default, the 3 variants of the DBEditObject anonymousLinkOK()
   * method are chained together, so that the customizer can choose
   * which level of detail he is interested in.
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField}'s
   * {@link arlut.csd.ganymede.server.InvidDBField#bind(arlut.csd.ganymede.common.Invid,arlut.csd.ganymede.common.Invid,boolean) bind()}
   * method calls this version.  This version calls the three parameter
   * version, which calls the two parameter version, which returns
   * false by default.  Customizers can implement any of the three
   * versions, but unless you maintain the version chaining yourself,
   * there's no point to implementing more than one of them.</P>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param targetObject The object that the link is to be created in
   * @param targetFieldID The field that the link is to be created in
   * @param sourceObject The object on the other side of the proposed link
   * @param sourceFieldID  The field on the other side of the proposed link
   * @param gsession Who is trying to do this linking?
   */

  public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID,
				 DBObject sourceObject, short sourceFieldID,
				 GanymedeSession gsession)
  {
    // if they can edit the group, they can put us in it.. the
    // gasharl schema specifies the mandatory type for the other
    // end of the GROUPLIST field's link, so we don't have to
    // check that here

    if (targetFieldID == userSchema.GROUPLIST)
      {
	return true;
      }

    // go ahead and allow the same for netgroups

    if (targetFieldID == userSchema.NETGROUPS)
      {
	return true;
      }

    // if someone tries to put this user in an email list, let them.

    if ((targetFieldID == SchemaConstants.BackLinksField) &&
	(sourceObject.getTypeID() == 274) && // email list
	(sourceFieldID == 257))	// email list members
      {
	return true;
      }

    // the default anonymousLinkOK() method returns false

    return super.anonymousLinkOK(targetObject, targetFieldID,
				 sourceObject, sourceFieldID, gsession);
  }

  /**
   * <p>Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
      case userSchema.USERNAME:
      case userSchema.UID:
      case userSchema.LOGINSHELL:
      case userSchema.HOMEDIR:
      case userSchema.VOLUMES:
      case userSchema.CATEGORY:
      case userSchema.HOMEGROUP:
      case userSchema.SIGNATURE:
      case userSchema.EMAILTARGET:
	return true;

      // the following fields are only necessary if the account has
      // not been inactivated

      case userSchema.PASSWORD:
      case userSchema.PASSWORDCHANGETIME:
	return !object.isInactivated();

      case userSchema.MAILUSER:
      case userSchema.MAILPASSWORD2:
      case userSchema.MAILEXPDATE:
	return object.isSet(ALLOWEXTERNAL);

      case userSchema.EXCHANGESTORE:
	return StringUtils.stringEquals((String) object.getFieldValueLocal(EMAILACCOUNTTYPE), "Exchange");
      }

    // Whether or not the Badge number field is required depends on
    // the user category.

    if (fieldid == userSchema.BADGE)
      {
	boolean needIdentifier = false;

	try
	  {
	    Invid catInvid = (Invid) object.getFieldValueLocal(userSchema.CATEGORY);

	    // we're PSEUDOSTATIC, so we need to get ahold of the internal session
	    // so we can look up objects

	    DBObject category = internalSession().getDBSession().viewDBObject(catInvid);

	    needIdentifier = category.isSet(userCategorySchema.SSREQUIRED);
	  }
	catch (NullPointerException ex)
	  {
	    // if we can't get the category reference, assume that we
	    // aren't gonna require the category.. the user will still
	    // be prompted to set a category, and once they go back
	    // and do that and try to re-commit, they'll hit us again
	    // and we can make the proper determination at that point.

	    return false;
	  }

	return needIdentifier;
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
    GanymedeSession gSession = null;
    DBObject categoryObj = null;
    String categoryName = null;

    /* -- */

    Invid category = (Invid) object.getFieldValueLocal(userSchema.CATEGORY);

    if (category == null)
      {
	return null;
      }

    if (object instanceof DBEditObject)
      {
	gSession = ((DBEditObject) object).getGSession();
      }
    else
      {
        gSession = Ganymede.getInternalSession();
      }

    categoryObj = lookupInvid(category, false);

    if (categoryObj == null)
      {
	// shouldn't happen, but if it does we'll assume something
	// else will catch this

	return null;
      }

    categoryName = categoryObj.getLabel();

    if (categoryObj.isSet(userCategorySchema.EXPIRE))
      {
	if (!object.isDefined(SchemaConstants.ExpirationField) &&
            !object.isDefined(SchemaConstants.RemovalField))
	  {
	    return Ganymede.createErrorDialog("Missing Expiration Field",
					      "User objects belonging to the " + categoryName +
					      " category require an expiration date to be set.");
	  }
      }

    // now let's make sure the signature alias is valid

    String signature = (String) object.getFieldValueLocal(SIGNATURE);
    String myUsername = (String) object.getLabel();
    Vector aliases = object.getFieldValuesLocal(ALIASES);

    if (!StringUtils.stringEquals(signature, myUsername) &&
        (aliases == null || !aliases.contains(signature)))
      {
        return Ganymede.createErrorDialog("Bad Signature Alias",
                                          "Ganymede server configuration error.  The signature alias (" + signature + ") for this user is " +
                                          "not a valid choice.");
      }

    // and the home group as well

    Invid homeGroupInvid = (Invid) object.getFieldValueLocal(HOMEGROUP);
    Vector myGroups = object.getFieldValuesLocal(GROUPLIST);

    if (myGroups != null)
      {
	if (!myGroups.contains(homeGroupInvid))
	  {
	    DBObject homeGroupObj = object.lookupInvid(homeGroupInvid, false);

	    if (homeGroupObj != null)
	      {
		return Ganymede.createErrorDialog("Bad Home Group",
						  "Ganymede server configuration error.  The home group (" +
						  homeGroupObj.getLabel() + ") for this user is " +
						  "not a valid choice.");
	      }
	    else
	      {
		return Ganymede.createErrorDialog("Bad Home Group",
						  "Ganymede server configuration error.  The home group " +
						  "for this user does not point to a valid object.");
	      }
	  }
      }
    else
      {
	return Ganymede.createErrorDialog("Missing Groups",
					  "This user is not a member of any groups.");
      }

    // and make sure that the badge number is unique, if we're a
    // normal account and we don't have any attached admin personae.

    if (object.isDefined(BADGE))
      {
        Vector personaeList = object.getFieldValuesLocal(PERSONAE);

        if ((personaeList == null || personaeList.size() == 0) && categoryName.equals("normal"))
          {
            String badge = (String) object.getFieldValueLocal(BADGE);

            QueryResult qr = null;

            try
              {
                qr = gSession.query("select object from 'User' where 'Badge' == '" + StringUtils.escape(badge) +
                                    "' and (not 'Username' == '" + StringUtils.escape(myUsername) +
                                    "') and ('Account Category' == 'normal') and (not 'Removal Date' defined)");
              }
            catch (NotLoggedInException ex)
              {
                throw new RuntimeException("Error in userObject.consistencyCheck(): query threw a NotLoggedInException.", ex);
              }
            catch (GanyParseException ex)
              {
                throw new RuntimeException("Error in userObject.consistencyCheck(): query could not be parsed correctly.", ex);
              }

            if (qr != null && qr.size() != 0)
              {
                boolean badge_is_admin = false;
                String conflict_name = null;

                for (int i = 0; !badge_is_admin && i < qr.size(); i++)
                  {
                    Invid matchInvid = qr.getInvid(i);

                    DBObject conflictUserObject = lookupInvid(matchInvid, false);

                    Vector personae = conflictUserObject.getFieldValuesLocal(PERSONAE);

                    if (personae != null && personae.size() > 0)
                      {
                        badge_is_admin = true;
                      }
                    else
                      {
                        conflict_name = conflictUserObject.getLabel();
                      }
                  }

                if (!badge_is_admin)
                  {
                    return Ganymede.createErrorDialog("Duplicate Badge Number",
                                                      "This user object shares a badge number with the " + conflict_name + " user object.\n\n" +
                                                      "Since both of these user accounts are 'normal' accounts, Ganymede can't tell which one should " +
                                                      "be the account of record for information transfer to the HR database.");
                  }
              }
          }
      }

    return null;
  }

  /**
   *
   * Customization method to verify whether this object type has an inactivation
   * mechanism.
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canBeInactivated()
  {
    return true;
  }

  /**
   *
   * Customization method to verify whether the user has permission
   * to inactivate a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for inactivating by the client.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canInactivate(DBSession dbSession, DBEditObject object)
  {
    return true;
  }

  /**
   * This method handles inactivation logic for this object type.  A
   * DBEditObject must first be checked out for editing, then the
   * inactivate() method can then be called on the object to put the
   * object into inactive mode.  inactivate() will set the object's
   * removal date and fix up any other state information to reflect
   * the object's inactive status.<br><br>
   *
   * inactive() is designed to run synchronously with the user's
   * request for inactivation.  It can return a wizard reference
   * in the ReturnVal object returned, to guide the user through
   * a set of interactive dialogs to inactive the object.<br><br>
   *
   * The inactive() method can cause other objects to be deleted, can cause
   * strings to be removed from fields in other objects, whatever.<br><br>
   *
   * If remove() returns a ReturnVal that has its success flag set to false
   * and does not include a JDialogBuff for further interaction with the
   * user, then DBSEssion.inactivateDBObject() method will rollback any changes
   * made by this method.<br><br>
   *
   * IMPORTANT NOTE: If a custom object's inactivate() logic decides
   * to enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeInactivate() with a boolean
   * indicating ultimate success of the operation.<br><br>
   *
   * Finally, it is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object inactivation when
   * the transaction is committed..
   *
   * @param interactive If true, the inactivate() logic can present
   * a wizard to the client to customize the inactivation logic.
   *
   * @see #commitPhase1()
   * @see #commitPhase2()
   */

  public ReturnVal inactivate(String ckp_label)
  {
    return inactivate(null, false, ckp_label);
  }

  public ReturnVal inactivate(String forward, boolean calledByWizard, String ckp_label)
  {
    ReturnVal retVal;
    StringDBField stringfield;
    PasswordDBField passfield;
    DateDBField date;
    Calendar cal = Calendar.getInstance();
    Date time;

    /* -- */

    if (!gSession.enableWizards || calledByWizard)
      {
	// ok, we want to null the password field and set the
	// removal time to current time + 3 months.

	passfield = (PasswordDBField) getField(userSchema.PASSWORD);
	retVal = passfield.setCryptPass(null); // we know our schema uses crypted pass'es

	if (retVal != null && !retVal.didSucceed())
	  {
	    if (calledByWizard)
	      {
		finalizeInactivate(false, ckp_label);
	      }

	    return retVal;
	  }

	// we're not going to set the shell to /bin/false
	// anymore.. we'll depend on our builder task to write it out
	// as /bin/false for us.

	if (false)
	  {
	    // set the shell to /bin/false

	    stringfield = (StringDBField) getField(LOGINSHELL);
	    retVal = stringfield.setValueLocal("/bin/false");

	    if (retVal != null && !retVal.didSucceed())
	      {
		if (calledByWizard)
		  {
		    finalizeInactivate(false, ckp_label);
		  }

		return retVal;
	      }
	  }

	// reset the forwarding address?

	if (forward != null && !forward.equals(""))
	  {
	    stringfield = (StringDBField) getField(EMAILTARGET);

	    while (stringfield.size() > 0)
	      {
		retVal = stringfield.deleteElementLocal(0);

		if (retVal != null && !retVal.didSucceed())
		  {
		    if (calledByWizard)
		      {
			finalizeInactivate(false, ckp_label);
		      }

		    return retVal;
		  }
	      }

	    stringfield.addElementLocal(forward);
	  }

	// determine what will be the date 3 months from now

	time = new Date();
	cal.setTime(time);
	cal.add(Calendar.MONTH, 3);

	// and set the removal date

	date = (DateDBField) getField(SchemaConstants.RemovalField);
	retVal = date.setValueLocal(cal.getTime());

	if (retVal != null && !retVal.didSucceed())
	  {
	    if (calledByWizard)
	      {
		finalizeInactivate(false, ckp_label);
	      }

	    return retVal;
	  }

	// make sure that the expiration date is cleared.. we're on
	// the removal track now.

	date = (DateDBField) getField(SchemaConstants.ExpirationField);
	retVal = date.setValueLocal(null);

	if (retVal != null && !retVal.didSucceed())
	  {
	    if (calledByWizard)
	      {
		finalizeInactivate(false, ckp_label);
	      }

	    return retVal;
	  }

	// success, have our DBEditObject superclass clean up.

	if (calledByWizard)
	  {
	    finalizeInactivate(true, ckp_label);
	  }

	// ok, we succeeded, now we have to tell the client
	// what to refresh to see the inactivation results

	ReturnVal result = ReturnVal.success();

	result.addRescanField(this.getInvid(), SchemaConstants.RemovalField);
	result.addRescanField(this.getInvid(), userSchema.LOGINSHELL);
	result.addRescanField(this.getInvid(), userSchema.EMAILTARGET);

	return result;
      }
    else  // interactive, but not called by wizard.. return a wizard
      {
	userInactivateWizard theWiz;

	try
	  {
	    if (debug)
	      {
		System.err.println("userCustom: creating inactivation wizard");
	      }

	    theWiz = new userInactivateWizard(this.gSession, this, ckp_label);
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("oops, userCustom couldn't create wizard for remote ex " + ex);
	  }

	if (debug)
	  {
	    System.err.println("userCustom: returning inactivation wizard");
	  }

	return theWiz.respond(null);
      }
  }

  /**
   * This method handles reactivation logic for this object type.  A
   * DBEditObject must first be checked out for editing, then the
   * reactivate() method can then be called on the object to make the
   * object active again.  reactivate() will clear the object's
   * removal date and fix up any other state information to reflect
   * the object's reactive status.<br><br>
   *
   * reactive() is designed to run synchronously with the user's
   * request for inactivation.  It can return a wizard reference
   * in the ReturnVal object returned, to guide the user through
   * a set of interactive dialogs to reactive the object.<br>
   *
   * If reactivate() returns a ReturnVal that has its success flag set to false
   * and does not include a JDialogBuff for further interaction with the
   * user, then DBSEssion.inactivateDBObject() method will rollback any changes
   * made by this method.<br><br>
   *
   * IMPORTANT NOTE: If a custom object's reactivate() logic decides
   * to enter into a wizard interaction with the user, that logic is
   * responsible for calling editset.rollback("reactivate" +
   * getLabel()) in the case of a failure to properly do all the reactivation
   * stuff, where getLabel() must be the name of the object
   * prior to any attempts to clear fields which could impact the
   * returned label.<br><br>
   *
   * Finally, it is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object reactivation when
   * the transaction is committed..
   *
   * @see arlut.csd.ganymede.server.DBEditObject#commitPhase1()
   * @see arlut.csd.ganymede.server.DBEditObject#commitPhase2()
   */

  public ReturnVal reactivate(String ckp_label)
  {
    userReactivateWizard theWiz;

    /* -- */

    try
      {
	if (debug)
	  {
	    System.err.println("userCustom: creating reactivation wizard");
	  }

	theWiz = new userReactivateWizard(this.gSession, this, ckp_label);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("oops, userCustom couldn't create wizard for remote ex " + ex);
      }

    if (debug)
      {
	System.err.println("userCustom: returning reactivation wizard");
      }

    return theWiz.respond(null);
  }

  /**
   * This method is called by the userReactivateWizard on successfully
   * obtaining the necessary information from the client on a
   * reactivate operation.  We then do the actual work to reactivate
   * the user in this method.
   *
   * @see arlut.csd.ganymede.gasharl.userReactivateWizard
   */

  public ReturnVal reactivate(userReactivateWizard reactivateWizard, String ckp_label)
  {
    ReturnVal retVal = null;
    StringDBField stringfield;
    PasswordDBField passfield;
    DateDBField date;
    boolean success = false;

    /* -- */

    if (reactivateWizard == null)
      {
	return Ganymede.createErrorDialog("userCustom.reactivate() error",
					  "Error, reactivate() called without a valid user wizard");
      }

    try
      {
	// reset the password

	if (reactivateWizard.password != null && reactivateWizard.password.length() != 0)
	  {
	    passfield = (PasswordDBField) getField(userSchema.PASSWORD);
	    retVal = passfield.setPlainTextPass(reactivateWizard.password);

	    if (retVal != null && !retVal.didSucceed())
	      {
		return retVal;
	      }
	  }
	else
	  {
	    return Ganymede.createErrorDialog("userCustom.reactivate() error",
					      "Error, reactivate() called without a password selected");
	  }

	// reset the shell

	if (reactivateWizard.shell != null)
	  {
	    stringfield = (StringDBField) getField(LOGINSHELL);

	    try
	      {
		retVal = stringfield.setValue(reactivateWizard.shell);
	      }
	    catch (GanyPermissionsException ex)
	      {
		return Ganymede.createErrorDialog("permissions", "permissions error setting shell during reactivation" + ex);
	      }

	    if (retVal != null && !retVal.didSucceed())
	      {
		return retVal;
	      }
	  }

	// reset the forwarding address

	if (reactivateWizard.forward != null && !reactivateWizard.forward.equals(""))
	  {
	    stringfield = (StringDBField) getField(EMAILTARGET);

	    while (stringfield.size() > 0)
	      {
		retVal = stringfield.deleteElementLocal(0);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
	      }

	    String[] strings = arlut.csd.Util.StringUtils.split(reactivateWizard.forward, ",");

	    for (int i = 0; i < strings.length; i++)
	      {
		stringfield.addElementLocal(strings[i]);
	      }
	  }

	// make sure that the removal date is cleared..

	date = (DateDBField) getField(SchemaConstants.RemovalField);
	retVal = date.setValueLocal(null);

	if (retVal != null && !retVal.didSucceed())
	  {
	    return retVal;
	  }

	finalizeReactivate(true, ckp_label);
	success = true;

	// ok, we succeeded, now we have to tell the client
	// what to refresh to see the reactivation results

	ReturnVal result = ReturnVal.success();

	result.addRescanField(this.getInvid(), SchemaConstants.RemovalField);
	result.addRescanField(this.getInvid(), userSchema.LOGINSHELL);
	result.addRescanField(this.getInvid(), userSchema.EMAILTARGET);
	result.addRescanField(this.getInvid(), userSchema.PASSWORDCHANGETIME);

	return result;
      }
    finally
      {
	if (!success)
	  {
	    finalizeReactivate(false, ckp_label);
	  }
      }
  }

  /**
   * This method handles removal logic for this object type.  This method
   * will be called immediately from DBSession.deleteDBObject().
   *
   * The remove() method can cause other objects to be deleted, can cause
   * strings to be removed from fields in other objects, whatever.
   *
   * If remove() returns a ReturnVal that has its success flag set to false
   * and does not include a JDialogBuff for further interaction with the
   * user, the DBSession.deleteDBObject() method will roll back any changes
   * made by this method.
   *
   * remove() is intended for subclassing, whereas finalizeRemove() is
   * not.  finalizeRemove() provides the standard logic for wiping out
   * fields and what not to cause the object to be unlinked from
   * other objects.
   *
   * IMPORTANT NOTE: If a custom object's remove() logic decides to
   * enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeRemove() on the object when
   * it is determined that the object really should be removed,
   * with a boolean indicating whether success was had.
   *
   * To be overridden on necessity in DBEditObject subclasses.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal remove()
  {
    StringDBField deliveryAddresses = (StringDBField) this.getField(userSchema.EMAILTARGET);
    Vector values = deliveryAddresses.getValuesLocal();

    for (int i = 0; i < values.size(); i++)
      {
        String x = (String) values.elementAt(i);

        if (x.endsWith("@arlex.arlut.utexas.edu"))
          {
            Vector toAddresses = new Vector();
            toAddresses.addElement("pcshelp@arlut.utexas.edu");
            toAddresses.addElement("broccol@arlut.utexas.edu");

            this.getEditSet().logMail(toAddresses,
                                      "Exchange User " + this.getLabel() + " Deleted",
                                      "User " + this.getLabel() +
                                      " has been deleted from Ganymede, and will need to be cleared out of the Exchange server.\n");

            return null;
          }
      }

    return null;
  }

  /**
   * <p>This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.</p>
   *
   * <p>The default logic in this method is designed to cause the client
   * to cache choice lists for invid fields in the 'all objects of
   * invid target type' cache bucket.  If your InvidDBField needs to
   * provide a restricted subset of objects of the targeted type as
   * the choice list, you'll need to override this method to either
   * return null (to turn off choice list caching), or generate some
   * kind of unique key that won't collide with the Short objects used
   * to represent the default object list caches.</p>
   *
   * <p>See also the {@link
   * arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)}
   * hook, which controls whether or not the default logic will
   * encourage the client to cache a given InvidDBField's choice list.</p>
   *
   * <p>If there is no caching key, this method will return null.</p>
   *
   * <p>We don't want the HOMEGROUP field's choice list to be cached on
   * the client because it is dynamically generated for this
   * context, and doesn't make sense in other contexts.</p>
   */

  public Object obtainChoicesKey(DBField field)
  {
    if (field.getID() == HOMEGROUP)
      {
	return null;
      }
    else
      {
	return super.obtainChoicesKey(field);
      }
  }

  /**
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given string field can only
   * choose from a choice provided by obtainChoiceList()
   */

  public boolean mustChoose(DBField field)
  {
    switch (field.getID())
      {
      case SIGNATURE:
	// we want to force signature alias choosing

      case EMAILACCOUNTTYPE:
	return true;
      }

    return super.mustChoose(field);
  }

  /**
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.
   *
   * Notice that fields 263 (login shell) and 268 (signature alias)
   * do not have their choice lists cached on the client, because
   * they are custom generated without any kind of accompanying
   * cache key.
   */

  public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
  {
    switch (field.getID())
      {
      case LOGINSHELL:			// login shell

	updateShellChoiceList();

	if (debug)
	  {
	    System.err.println("userCustom: obtainChoice returning " + shellChoices + " for shell field.");
	  }

	return shellChoices;

      case HOMEGROUP:			// home group

	updateGroupChoiceList();
	return groupChoices;

      case EMAILACCOUNTTYPE:
	QueryResult typeResult = new QueryResult();

	typeResult.addRow("IMAP");
	typeResult.addRow("Exchange");
	typeResult.addRow("Other");
	return typeResult;

      case SIGNATURE:			// signature alias

	QueryResult result = new QueryResult();

	/* -- */

	// our list of possible aliases includes the user's name

	// note that we first check the new value, if any, for the
	// user name.. this way the user rename code can change the
	// signature alias without having the StringDBField for the
	// signature alias reject the new name.

	String name = newUsername;

	if (name != null)
	  {
	    result.addRow(null, name, false);
	  }
	else
	  {
	    name = (String) ((DBField) getField(USERNAME)).getValueLocal();

	    if (name != null)
	      {
		result.addRow(null, name, false);
	      }
	  }

	// and any aliases defined

	Vector values = ((DBField) getField(ALIASES)).getValuesLocal();

	for (int i = 0; i < values.size(); i++)
	  {
	    result.addRow(null, (String) values.elementAt(i), false);
	  }

	return result;

      default:
	return super.obtainChoiceList(field);
      }
  }

  /**
   * We update the groupChoices list to contain all of the groups
   * the user is currently in.
   */

  void updateGroupChoiceList()
  {
    if (groupChoices == null)
      {
	groupChoices = new QueryResult();

	Vector invids = getFieldValuesLocal(GROUPLIST); // groups list
	Invid invid;

	for (int i = 0; i < invids.size(); i++)
	  {
	    invid = (Invid) invids.elementAt(i);

	    // must be editable because the client cares

	    groupChoices.addRow(invid, gSession.getDBSession().getObjectLabel(invid), true);
	  }
      }
  }

  void updateShellChoiceList()
  {
    synchronized (shellChoices)
      {
	DBObjectBase base = Ganymede.db.getObjectBase("Shell Choice");

	// just go ahead and throw the null pointer if we didn't get our base.

	if (shellChoiceStamp == null || shellChoiceStamp.before(base.getTimeStamp()))
	  {
	    if (debug)
	      {
		System.err.println("userCustom - updateShellChoiceList()");
	      }

	    shellChoices = new QueryResult();

	    Query query = new Query("Shell Choice", null, false);

	    // internalQuery doesn't care if the query has its filtered bit set

	    if (debug)
	      {
		System.err.println("userCustom - issuing query");
	      }

	    Vector results = internalSession().internalQuery(query);

	    if (debug)
	      {
		System.err.println("userCustom - processing query results");
	      }

	    for (int i = 0; i < results.size(); i++)
	      {
		shellChoices.addRow(null, results.elementAt(i).toString(), false); // no invid
	      }

	    if (shellChoiceStamp == null)
	      {
		shellChoiceStamp = new Date();
	      }
	    else
	      {
		shellChoiceStamp.setTime(System.currentTimeMillis());
	      }
	  }
      }
  }

  /**
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
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public PermEntry permOverride(GanymedeSession session, DBObject object, short fieldid)
  {
    if (fieldid != UID)
      {
	return null;
      }

    // we don't want to allow anyone other than supergash to change our
    // uid once it is set.

    if (object.isDefined(UID))
      {
	return PermEntry.getPermEntry(true, false, true, false);
      }
    else
      {
	return null;
      }
  }

  /**
   *
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given Date field has a restricted
   * range of possibilities.
   *
   */

  public boolean isDateLimited(DBField field)
  {
    if (field.getID() == SchemaConstants.ExpirationField)
      {
	return true;
      }

    if (field.getID() == userSchema.PASSWORDCHANGETIME)
      {
	return true;
      }

    return super.isDateLimited(field);
  }

  /**
   * This method is used to specify the earliest acceptable date
   * for the specified {@link arlut.csd.ganymede.server.DateDBField DateDBField}.
   */

  public Date minDate(DBField field)
  {
    if (field.getID() == userSchema.PASSWORDCHANGETIME)
      {
	return new Date(); // no values in the past, thanks
      }

    return super.minDate(field);
  }

  /**
   * This method is used to specify the latest acceptable date
   * for the specified {@link arlut.csd.ganymede.server.DateDBField DateDBField}.
   */

  public Date maxDate(DBField field)
  {
    if (field.getID() == SchemaConstants.ExpirationField)
      {
	// if we have a user category set, limit the acceptable date to
	// current date + max days

	Date currentDate = new Date();

	Calendar cal = Calendar.getInstance();

	cal.setTime(currentDate);

	try
	  {
	    Invid catInvid = (Invid) this.getFieldValueLocal(userSchema.CATEGORY);

	    DBObject category = internalSession().getDBSession().viewDBObject(catInvid);

	    Integer maxDays = (Integer) category.getFieldValueLocal(userCategorySchema.LIMIT);

	    cal.add(Calendar.DATE, maxDays.intValue());
	  }
	catch (NullPointerException ex)
	  {
	    // oops, no category set.. <shrug>

	    return super.maxDate(field);
	  }

	return cal.getTime();
      }

    if (field.getID() == userSchema.PASSWORDCHANGETIME)
      {
	GanymedeSession mySession = this.getGSession();

	// if we are supergash or we are reacting to a password change
	// cascade, don't restrict what the date can be set to.

	if (mySession == null || mySession.getPermManager().isSuperGash() || amChangingExpireDate)
	  {
	    return super.maxDate(field);
	  }

	Date maxDate = getMaxPasswordExtension();

	DateDBField passDateField = (DateDBField) getField(userSchema.PASSWORDCHANGETIME);

	if (passDateField != null)
	  {
	    Date currentDate = passDateField.value();

	    if (currentDate != null && currentDate.after(maxDate))
	      {
		maxDate = currentDate;
	      }
	  }

	return maxDate;
      }

    return super.maxDate(field);
  }

  /**
   * This method is called after the set value operation has been ok'ed
   * by any appropriate wizard code.
   */

  public synchronized ReturnVal finalizeSetValue(DBField field, Object value)
  {
    InvidDBField inv;
    Vector personaeInvids;
    String oldName;
    StringDBField sf;

    /* -- */

    // we don't want to allow the home directory to be changed except
    // by when the username field is being changed.

    if (field.getID() == HOMEDIR)
      {
	String dir = (String) value;

	/* -- */

	if (homedir == null)
	  {
	    homedir = System.getProperty("ganymede.homedirprefix");
	  }

	// we will only check against a defined prefix if
	// we have set one in our properties file.

	if (homedir != null && homedir.length() != 0)
	  {
	    if (newUsername != null)
	      {
		String expected = homedir + (String) newUsername;

		if (!dir.equals(expected))
		  {
		    return Ganymede.createErrorDialog("Schema Error",
						      "Home directory should be " + expected + ".\n" +
						      "This is a restriction encoded in userCustom.java.");
		  }
	      }
	  }

	return null;
      }

    if (field.getID() == userSchema.PASSWORD)
      {
	// the password is being changed, update the time that it will need to
	// be changed again

	DateDBField dateField = (DateDBField) getField(userSchema.PASSWORDCHANGETIME);

	if (dateField != null)
	  {
	    Date passwordDate = getNewPasswordExpirationDate();
	    Date currentDate = dateField.value();

	    // be sure and check to make sure we never pull a password
	    // expiration date backwards in time

	    if (currentDate == null || !currentDate.after(passwordDate))
	      {
		// set the amChangingExpireDate flag to true so that we
		// won't try and restrict the forward date when the date
		// set operation cascades forward through our maxDate()
		// method

		ReturnVal result;

		amChangingExpireDate = true;

		try
		  {
		    result = dateField.setValueLocal(passwordDate);
		  }
		finally
		  {
		    amChangingExpireDate = false;
		  }

		if (result != null)
		  {
		    System.err.println("UserCustom: setValueLocal on PASSWORDCHANGETIME field failed: " + result);
		  }
	      }
	  }
	else
	  {
	    System.err.println("UserCustom: can't find PASSWORDCHANGETIME field");
	  }

	ReturnVal result = ReturnVal.success();

	result.addRescanField(this.getInvid(), userSchema.PASSWORDCHANGETIME);

	return result;
      }

    // our maxDate() and isDateLimited() methods have pre-filtered any
    // non-null expiration date for us.. just need to check to see
    // whether the field can be cleared here.

    if ((field.getID() == SchemaConstants.ExpirationField) && value == null)
      {
	if (isDeleting())
	  {
	    // approve it, everything's being cleaned out.

	    return null;
	  }

	if (willBeRemoved())
	  {
	    // it's okay for us to null the expiration date, since we already
	    // have a removal date set

	    return null;
	  }

	// check to see if the user category doesn't mind not having an expiration
	// or removal date set.

	try
	  {
	    Invid catInvid = (Invid) this.getFieldValueLocal(userSchema.CATEGORY);

	    DBObject category = internalSession().getDBSession().viewDBObject(catInvid);

	    Boolean expDateRequired = (Boolean) category.getFieldValueLocal(userCategorySchema.EXPIRE);

	    if (expDateRequired.booleanValue())
	      {
		return Ganymede.createErrorDialog("Schema Error",
						  "This user requires an expiration date because of its " +
						  "user category.");
	      }
	    else
	      {
		// ok, then

		return null;
	      }
	  }
	catch (NullPointerException ex)
	  {
	    // ah, no category or limit set.. go ahead and let em do
	    // it

	    return null;
	  }
      }

    // when we rename a user, we have lots to do.. a number of other
    // fields in this object and others need to be updated to match.

    if (field.getID() == USERNAME)
      {
	// remember the new user name we are changing to, so that the
	// other fields that we will change as a result of the
	// username change will be able to get the new name.

	newUsername = (String) value;

	try
	  {
	    // if we are being told to clear the user name field, go ahead and
	    // do it.. we assume this is being done by user removal logic,
	    // so we won't press the issue.

	    if (isDeleting() && (value == null))
	      {
		return null;
	      }

	    // so we're renaming.  rename the hidden label for any
	    // embedded automounter entries, please.

	    ReturnVal retVal = renameEntries(newUsername);

	    if (retVal != null && !retVal.didSucceed())
	      {
		return retVal;
	      }

	    // signature alias field will need to be rescanned

	    sf = (StringDBField) getField(USERNAME); // old user name

	    oldName = (String) sf.getValueLocal();

	    if (oldName != null)
	      {
		sf = (StringDBField) getField(SIGNATURE); // signature alias

		// if the signature alias was the user's name, we'll want
		// to continue that.

		if (oldName.equals((String) sf.getValueLocal()))
		  {
		    sf.setValueLocal(value); // set the signature alias to the user's new name
		  }
	      }

	    // update the home directory location.. we assume that if
	    // the user has permission to rename the user, they can
	    // automatically execute this change to the home directory.

	    if (homedir == null)
	      {
		homedir = System.getProperty("ganymede.homedirprefix");
	      }

	    // do we have a homedir prefix?  if so, set the home dir here

	    if (homedir != null && homedir.length() != 0)
	      {
		sf = (StringDBField) getField(HOMEDIR);

		sf.setValueLocal(homedir + (String) value);	// ** ARL
	      }

	    // if we don't have a signature set, set it to the username.

	    sf = (StringDBField) getField(SIGNATURE);

	    String sigVal = (String) sf.getValueLocal();

	    if (sigVal == null || sigVal.equals(oldName))
	      {
		sf.setValueLocal(value);
	      }

	    // update the email target field.  We want to look for
	    // oldName@arlut.utexas.edu and replace it if we find it.

	    sf = (StringDBField) getField(EMAILTARGET);

	    if (mailsuffix == null)
	      {
		mailsuffix = System.getProperty("ganymede.defaultmailsuffix");
	      }

	    if (mailsuffix == null)
	      {
		Ganymede.debug("Error in userCustom: couldn't find property ganymede.defaultmailsuffix!");
	      }

	    String oldMail = oldName + mailsuffix;

	    if (sf.containsElementLocal(oldMail))
	      {
		sf.deleteElementLocal(oldMail);
		sf.addElementLocal(value + mailsuffix);
	      }
	    else if (sf.size() == 0)
	      {
		sf.addElementLocal(value + mailsuffix);
	      }

	    inv = (InvidDBField) getField(PERSONAE);

	    if (inv == null)
	      {
		return null;	// success
	      }

	    // rename all the associated personae with the new user name

	    personaeInvids = inv.getValuesLocal();

	    for (int i = 0; i < personaeInvids.size(); i++)
	      {
		adminPersonaCustom adminObj = (adminPersonaCustom) getDBSession().editDBObject((Invid) personaeInvids.elementAt(i));

		adminObj.refreshLabelField(null, null, (String) value);
	      }
	  }
	finally
	  {
	    newUsername = null;
	  }
      }

    return null;		// success by default
  }

  /**
   * This method allows the DBEditObject to have executive approval of
   * any vector delete operation, and to take any special actions in
   * reaction to the delete.. if this method returns null or a success
   * code in its ReturnVal, the {@link arlut.csd.ganymede.server.DBField DBField}
   * that called us is guaranteed to proceed to
   * make the change to its vector.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.
   *
   * The &lt;field&gt; parameter identifies the field that is requesting
   * approval for item deletion, and the &lt;index&gt; parameter identifies
   * the element number that is to be deleted.
   *
   * The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under normal circumstances, we won't need
   * to do anything here.
   */

  public ReturnVal finalizeDeleteElement(DBField field, int index)
  {
    if (field.getID() == ALIASES)
      {
        String goneAlias = (String) getFieldElementLocal(field, index);

        String signatureAlias = (String) getFieldValueLocal(SIGNATURE);

        if (!StringUtils.stringEquals(goneAlias, signatureAlias))
          {
            return null;        // no worries
          }

        // okay, they're removing their signature alias from the
        // aliases field.  Let's force the signature alias back to
        // their username.

        String username = (String) getFieldValueLocal(USERNAME);

        ReturnVal retVal = setFieldValueLocal(SIGNATURE, username);

        return retVal;
      }

    return null;
  }

  /**
   * <p>This method calculates what time the password expiration field should be set to
   * if the password is being changed right now.</p>
   */

  private Date getNewPasswordExpirationDate()
  {
    Calendar myCal = new GregorianCalendar();
    myCal.add(Calendar.MONTH, 3);

    // if the expiration date will fall between Dec 20
    // and January 10, bump the date forward three
    // weeks to skip over the year-end holidays

    int month = myCal.get(Calendar.MONTH);
    int day = myCal.get(Calendar.DATE);

    if ((month == Calendar.DECEMBER && day >= 20) ||
	(month == Calendar.JANUARY && day < 10))
      {
	myCal.add(Calendar.DATE, 21);
      }

    return myCal.getTime();
  }

  /**
   * This method calculates what the maximum time the password
   * expiration field may be set to by a ganymede admin.
   */

  private Date getMaxPasswordExtension()
  {
    Calendar myCal = new GregorianCalendar();
    myCal.add(Calendar.DATE, 14);

    // if the maximum expiration date will fall between Dec 20
    // and January 10, bump the date forward three
    // weeks to skip over the year-end holidays

    int month = myCal.get(Calendar.MONTH);
    int day = myCal.get(Calendar.DATE);

    if ((month == Calendar.DECEMBER && day >= 20) ||
	(month == Calendar.JANUARY && day < 10))
      {
	myCal.add(Calendar.DATE, 21);
      }

    return myCal.getTime();
  }

  /**
   * This is the hook that DBEditObject subclasses use to interpose wizards whenever
   * a sensitive field is being changed.
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    userHomeGroupDelWizard groupWizard = null;
    userRenameWizard renameWizard = null;
    ReturnVal result;

    /* -- */

    // something's changed, forget that we've given a warning about
    // username/badge issues in IRIS

    this.IRISWarningGiven = false;

    // if the groups field is being changed, we may need to intervene

    if (debug)
      {
	System.err.println("userCustom ** entering wizardHook, field = " +
			   field.getName() + ", op= " + operation);
      }

    try
      {
	if (field.getID() == EMAILACCOUNTTYPE)
	  {
	    result = ReturnVal.success();

	    result.addRescanField(field.getObject().getInvid(), EXCHANGESTORE);

	    if (!"Exchange".equals(param1))
	      {
		return result.merge(((DBEditObject) field.getObject()).setFieldValueLocal(EXCHANGESTORE, null));
	      }
	    else
	      {
		return result;
	      }
	  }

	if (field.getID() == ALLOWEXTERNAL)
	  {
	    // a success ReturnVal is used to tell the Ganymede logic
	    // to go ahead and complete the operation normally.  In
	    // this case, it will take the rescan information as an
	    // extra to pass back to the client.

	    result = ReturnVal.success();

	    if (Boolean.TRUE.equals(param1))
	      {
		StringDBField usernameField = (StringDBField) getField(userSchema.MAILUSER);
		PasswordDBField passField = (PasswordDBField) getField(userSchema.MAILPASSWORD2);
		DateDBField dateField = (DateDBField) getField(userSchema.MAILEXPDATE);

		result = ReturnVal.merge(result, usernameField.setValueLocal(RandomUtils.getRandomUsername()));

		if (!ReturnVal.didSucceed(result))
		  {
		    return result;
		  }

		result = ReturnVal.merge(result, passField.setPlainTextPass(RandomUtils.getRandomPassword(20)));

		if (!ReturnVal.didSucceed(result))
		  {
		    return result;
		  }

		Calendar myCal = new GregorianCalendar();
		myCal.add(Calendar.DATE, 168); // 24 weeks

		result = ReturnVal.merge(result, dateField.setValueLocal(myCal.getTime()));

		if (!ReturnVal.didSucceed(result))
		  {
		    return result;
		  }
	      }

	    result.addRescanField(this.getInvid(), userSchema.MAILUSER);
	    result.addRescanField(this.getInvid(), userSchema.MAILPASSWORD2);
	    result.addRescanField(this.getInvid(), userSchema.MAILEXPDATE);

	    return result;
	  }

	// if we are changing the list of email aliases, we'll want
	// to update the list of choices for the signature field.

	if (field.getID() == ALIASES)
	  {
	    // the second true in the ReturnVal constructor makes the
	    // Ganymede logic go ahead and complete the operation
	    // normally, just taking the rescan information as an
	    // extra to pass back to the client.

	    result = ReturnVal.success();

	    result.addRescanField(this.getInvid(), userSchema.SIGNATURE);

	    return result;
	  }

	if (field.getID() == GROUPLIST)
	  {
	    switch (operation)
	      {
	      case ADDELEMENT:
	      case ADDELEMENTS:

		// ok, no big deal, but we will need to have the client
		// rescan the choice list for the home group field

		result = ReturnVal.success();
		result.addRescanField(this.getInvid(), HOMEGROUP);
		groupChoices = null;
		return result;

	      case DELELEMENT:

		if (isDeleting())
		  {
		    return null;
		  }

		// ok, this is more of a big deal.. first, see if the value
		// being deleted is the home group.  If not, still no big
		// deal.

		int index = ((Integer) param1).intValue();

		Vector valueAry = getFieldValuesLocal(GROUPLIST);
		Invid delVal = (Invid) valueAry.elementAt(index);

		if (debug)
		  {
		    System.err.println("userCustom: deleting group element " +
				       gSession.getDBSession().getObjectLabel(delVal));
		  }

		if (!delVal.equals(getFieldValueLocal(HOMEGROUP)))
		  {
		    // whew, no big deal.. they are not removing the
		    // home group.  The client will need to rescan,
		    // but no biggie.

		    if (debug)
		      {
			System.err.println("userCustom: I don't think " +
					   gSession.getDBSession().getObjectLabel(delVal) +
					   " is the home group");
		      }

		    result = ReturnVal.success();
		    result.addRescanField(this.getInvid(), HOMEGROUP);
		    groupChoices = null;
		    return result;
		  }

		if (gSession.isWizardActive() &&
		    gSession.getWizard() instanceof userHomeGroupDelWizard)
		  {
		    groupWizard = (userHomeGroupDelWizard) gSession.getWizard();

		    if (groupWizard.getState() == groupWizard.DONE)
		      {
			// ok, assume the wizard has taken care of getting everything prepped and
			// approved for us.  An active wizard has approved the operation

			groupWizard.unregister();

			return null;
		      }
		    else
		      {
			if (groupWizard.userObject != this)
			  {
			    System.err.println("userCustom.wizardHook(): bad object");
			  }

			if (groupWizard.getState() != groupWizard.DONE)
			  {
			    System.err.println("userCustom.wizardHook(): bad state: " +
					       groupWizard.getState());
			  }

			groupWizard.unregister();

			return Ganymede.createErrorDialog("User Object Error",
							  "The client is attempting to do an operation on " +
							  "a user object with an active wizard.");
		      }
		  }
		else if (gSession.isWizardActive() &&
			 !(gSession.getWizard() instanceof userHomeGroupDelWizard))
		  {
		    return Ganymede.createErrorDialog("User Object Error",
						      "The client is attempting to do an operation on " +
						      "a user object with mismatched active wizard.");
		  }

		// eek.  they are deleting the home group.  Why Lord, why?!

		try
		  {
		    groupWizard = new userHomeGroupDelWizard(this.gSession,
							     this,
							     param1);
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("Couldn't create userWizard " + ex.getMessage());
		  }

		// if we get here, the wizard was able to register itself.. go ahead
		// and return the initial dialog for the wizard.  The ReturnVal code
		// that wizard.getStartDialog() returns will have the success code
		// set to false, so whatever triggered us will prematurely exit,
		// returning the wizard's dialog.

		return groupWizard.respond(null);

	      case DELELEMENTS:

		// see if any of the values is the home group

		Vector valuesToDelete = (Vector) param1;

		if (!valuesToDelete.contains(getFieldValueLocal(HOMEGROUP)))
		  {
		    result = ReturnVal.success();
		    result.addRescanField(this.getInvid(), HOMEGROUP); // rebuild choice list
		    groupChoices = null;
		    return result;
		  }
		else
		  {
		    return Ganymede.createErrorDialog("User Validation Error",
						      "Can't remove home group in bulk transfer.");
		  }
	      }
	  }

	// if the user category is changed, we need to be sure and get
	// the expiration date set..

	if (field.getID() == CATEGORY)
	  {
	    if (gSession.isWizardActive() &&
		gSession.getWizard() instanceof userCategoryWizard)
	      {
		userCategoryWizard uw = (userCategoryWizard) gSession.getWizard();

		if (uw.getState() == uw.DONE)
		  {
		    // ok, assume the wizard has taken care of getting everything prepped and
		    // approved for us.  An active wizard has approved the operation

		    return null;
		  }
	      }

	    try
	      {
		if (param1 != null || !isDeleting())
		  {
		    return new userCategoryWizard(getGSession(), this,
						  (Invid) getFieldValueLocal(userSchema.CATEGORY),
						  (Invid) param1).respond(null);
		  }
		else
		  {
		    return null;
		  }
	      }
	    catch (RemoteException ex)
	      {
		return Ganymede.createErrorDialog("Server error",
						  "userCustom.wizardHook(): can't initialize userCategoryWizard.");
	      }
	  }

	if ((field.getID() != USERNAME) ||
	    (operation != SETVAL))
	  {
	    return null;		// by default, we just ok whatever else
	  }

	// ok, we're doing a user rename.. check to see if we need to do a
	// wizard

	// If this is a newly created user, we won't pester them about setting
	// or changing the user name field.

	if ((field.getValueLocal() == null) || (getStatus() == ObjectStatus.CREATING))
	  {
	    result = ReturnVal.success(); // have setValue() do the right thing

	    result.addRescanField(this.getInvid(), userSchema.HOMEDIR);
	    result.addRescanField(this.getInvid(), userSchema.ALIASES);
	    result.addRescanField(this.getInvid(), userSchema.SIGNATURE);
	    result.addRescanField(this.getInvid(), userSchema.VOLUMES);
	    result.addRescanField(this.getInvid(), userSchema.EMAILTARGET);

	    return result;
	  }

	String oldname = (String) field.getValueLocal();

	if (!gSession.enableWizards)
	  {
	    return null;		// no wizards if the user is non-interactive.
	  }

	// Huh!  Wizard time!  We'll check here to see if there is a
	// registered userRenameWizard in the system taking care of us.

	if (gSession.isWizardActive() && gSession.getWizard() instanceof userRenameWizard)
	  {
	    renameWizard = (userRenameWizard) gSession.getWizard();

	    if ((renameWizard.getState() == renameWizard.DONE) &&
		(renameWizard.field == field) &&
		(renameWizard.userObject == this) &&
		(renameWizard.newname == param1))
	      {
		// ok, assume the wizard has taken care of getting
		// everything prepped and approved for us.  An active
		// wizard has approved the operation

		renameWizard.unregister();

		// note that we don't have to return the rescan fields
		// directive here.. the active wizard is what is going to
		// respond directly to the user, we are presumably just
		// here because the wizard task-completion code went ahead
		// and called setValue on the user's name.. we'll trust
		// that code to return the rescan indicators.

		return null;
	      }
	    else
	      {
		if (renameWizard.field != field)
		  {
		    System.err.println("userCustom.wizardHook(): bad field");
		  }

		if (renameWizard.userObject != this)
		  {
		    System.err.println("userCustom.wizardHook(): bad object");
		  }

		if (renameWizard.newname != param1)
		  {
		    System.err.println("userCustom.wizardHook(): bad param");
		  }

		if (renameWizard.getState() != renameWizard.DONE)
		  {
		    System.err.println("userCustom.wizardHook(): bad state: " +
				       renameWizard.getState());
		  }

		renameWizard.unregister();
		return Ganymede.createErrorDialog("User Object Error",
						  "The client is attempting to do an operation on " +
						  "a user object with an active wizard.");
	      }
	  }
	else if (gSession.isWizardActive())
	  {
	    return Ganymede.createErrorDialog("User Object Error",
					      "The client is attempting to do an operation on " +
					      "a user object with mismatched active wizard.\n" +
					      "Wizard id: " + gSession.getWizard());
	  }
	else
	  {
	    // there's no wizard active, and this operation has to be approved by one.  Go ahead
	    // and set up the wizard and let the client play with it.

	    // if we're setting the field to null, don't need to pass it through
	    // a wizard.. we're probably just deleting this user.

	    if (isDeleting() && (param1 == null))
	      {
		return null;
	      }

	    try
	      {
		// Mike Jittlov is the Wizard of Speed and Time

		renameWizard = new userRenameWizard(this.gSession,
						    this,
						    field,
						    (String) param1,
						    oldname);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("Couldn't create userWizard " + ex.getMessage());
	      }

	    // if we get here, the wizard was able to register itself.. go ahead
	    // and return the initial dialog for the wizard.  The ReturnVal code
	    // that wizard.respond() returns will have the success code
	    // set to false, so whatever triggered us will prematurely exit,
	    // returning the wizard's dialog.

	    return renameWizard.respond(null);
	  }
      }
    finally
      {
	if (debug)
	  {
	    System.err.println("userCustom ** exiting wizardHook");
	  }
      }
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
	// the user never really existed.. no external actions required.
	break;

      case CREATING:

	// handle creating the user.. creating their home directory, setting
	// up their mail spool, etc., etc.

	createUserExternals();
	break;

      case DELETING:
	deleteUserExternals();
	break;

      case EDITING:

	// did the user's name change?

	String name = getLabel();
	String oldname = original.getLabel();

	if (!name.equals(oldname))
	  {
	    handleUserRename(oldname, name);
	  }

	// did we change home directory volumes?

	handleVolumeChanges();
      }

    return;
  }

  /**
   *
   * This method runs from userCustom's commitPhase2() and runs an external
   * script that can create the user's home directory, and anything else
   * that might need doing.
   *
   */

  private void createUserExternals()
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("userCustom: " + getLabel() + ", in createUserExternals().");
      }

    // get the volumes defined for the user on auto.home.default

    InvidDBField mapEntries = (InvidDBField) getField(userSchema.VOLUMES);
    Vector entries = mapEntries.getValuesLocal();

    if (entries.size() < 1)
      {
	System.err.println("Couldn't handle createUserExternals for user " + getLabel() +
			   ", because we don't have a volume defined");
	return;
      }

    for (int i = 0; i < entries.size(); i++)
      {
	user_added_to_vol((Invid) entries.elementAt(i));
      }
  }

  /**
   * Helper method to create a directory for a user
   */

  private void user_added_to_vol(Invid entryInvid)
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    DBObject entryObj = getDBSession().viewDBObject(entryInvid);

    Invid volumeInvid = (Invid) entryObj.getFieldValueLocal(mapEntrySchema.VOLUME);
    DBObject volumeObj = getDBSession().viewDBObject(volumeInvid);
    String volName = (String) volumeObj.getFieldValueLocal(volumeSchema.LABEL);

    Invid mapInvid = (Invid) entryObj.getFieldValueLocal(mapEntrySchema.MAP);
    DBObject mapObj = getDBSession().viewDBObject(mapInvid);
    String mapName = mapObj.getLabel();

    Integer id = (Integer) getFieldValueLocal(userSchema.UID);
    Invid homegroupInvid = (Invid) getFieldValueLocal(userSchema.HOMEGROUP);

    Vector ownerInvids = (Vector) this.getFieldValuesLocal(SchemaConstants.OwnerListField);
    String ownerName;

    if (ownerInvids != null && ownerInvids.size() > 0)
      {
	Invid ownerOne = (Invid) ownerInvids.elementAt(0);

	DBObject ownerObj = getDBSession().viewDBObject(ownerOne);
	ownerName = ownerObj.getLabel();

	// we want underscores to separate words, not spaces

	ownerName = ownerName.replace(' ', '_');
      }
    else
      {
	ownerName = "";
      }

    if (homegroupInvid == null)
      {
	// the user didn't completely fill out this user
	// object.. return silently and let the transaction logic tell
	// the user what the problem is.

	return;
      }

    DBObject homeGroup = getDBSession().viewDBObject(homegroupInvid);
    Integer gid = (Integer) homeGroup.getFieldValueLocal(groupSchema.GID);

    boolean success = false;

    try
      {
	if (createHandler == null)
	  {
	    if (debug)
	      {
		System.err.println("userCustom: createUserExternals: getting createFilename");
	      }

	    createFilename = System.getProperty("ganymede.builder.scriptlocation");

	    if (createFilename == null)
	      {
		Ganymede.debug("userCustom.createUserExternals(): Couldn't find " +
			       "ganymede.builder.scriptlocation property");
		return;
	      }

	    // make sure we've got the path separator at the end of
	    // createFilename, add our script name

	    createFilename = PathComplete.completePath(createFilename) + "scripts/directory_maker";

	    if (debug)
	      {
		System.err.println("userCustom: createUserExternals: createFilename = " +
				   createFilename);
	      }

	    createHandler = new File(createFilename);
	  }

	if (createHandler.exists())
	  {
	    try
	      {
		// we'll call our external script with the following
		//
		// parameters: <volumename/volume_directory> <username> <user id> <group id> <mapname> <owner>

		String execLine = createFilename + " " + volName + " " +
		  getLabel() + " " + id + " " + gid + " " + mapName + " " + ownerName;

		if (debug)
		  {
		    System.err.println("createUserExternals: running " + execLine);
		  }

		try
		  {
		    if (debug)
		      {
			System.err.println("createUserExternals: blocking ");
		      }

		    int result = FileOps.runProcess(execLine);

		    if (debug)
		      {
			System.err.println("createUserExternals: done ");
		      }

		    if (result != 0)
		      {
			Ganymede.debug("Couldn't handle externals for creating user " + getLabel() +
				       "\n" + createFilename + " returned a non-zero result: " + result);
		      }
		    else
		      {
			success = true;
		      }
		  }
		catch (InterruptedException ex)
		  {
		    Ganymede.debug("Couldn't handle externals for creating user " + getLabel() + "\n" +
				   ex.getMessage());
		  }
	      }
	    catch (IOException ex)
	      {
		Ganymede.debug("Couldn't handle externals for creating user " + getLabel() + "\n" +
			       ex.getMessage());
	      }
	  }
      }
    finally
      {
	mail_user_added_to_vol(entryInvid, !success);
      }
  }

  /**
   * Helper method to send out mail to owners of the system that the
   * user's home directory is being placed on.
   */

  private void mail_user_added_to_vol(Invid entryInvid, boolean need_to_create)
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    StringBuilder buffer = new StringBuilder();

    DBObject entryObj = getDBSession().viewDBObject(entryInvid);

    Invid mapInvid = (Invid) entryObj.getFieldValueLocal(mapEntrySchema.MAP);
    DBObject mapObj = getDBSession().viewDBObject(mapInvid);
    String mapName = mapObj.getLabel();

    Invid volumeInvid = (Invid) entryObj.getFieldValueLocal(mapEntrySchema.VOLUME);
    DBObject volumeObj = getDBSession().viewDBObject(volumeInvid);
    String volName = volumeObj.getLabel();
    String volPath = (String) volumeObj.getFieldValueLocal(volumeSchema.PATH);

    Invid sysInvid = (Invid) volumeObj.getFieldValueLocal(volumeSchema.HOST);
    DBObject sysObj = getDBSession().viewDBObject(sysInvid);
    String sysName = sysObj.getLabel();

    List<Invid> objects = new ArrayList<Invid>();
    objects.add(sysInvid);
    Set<String> addresses = DBLog.calculateOwnerAddresses(objects, getDBSession());

    String subject = null;

    if (need_to_create)
      {
	buffer.append("Hi.  User ");
	buffer.append(getLabel());
	buffer.append(" was added to volume ");
	buffer.append(volName);
	buffer.append(" in the ");
	buffer.append(mapName);
	buffer.append(" automounter home map.\n\nSince you are listed in the Ganymede");
	buffer.append(" system database as an administrator for a system contained in");
	buffer.append(" volume ");
	buffer.append(volName);
	buffer.append(", you need to take whatever action is appropriate to create a");
	buffer.append(" home directory for this user on ");
	buffer.append(sysName);
	buffer.append(", if one does not already exist..\n\n");
	buffer.append("Volume ");
	buffer.append(volName);
	buffer.append(" is currently defined as:\n");
	buffer.append(sysName);
	buffer.append(":");
	buffer.append(volPath);
	buffer.append("\n\nThanks for your cooperation.\nYour friend,\n\tGanymede.\n");

	subject = "User " + getLabel() + " needs a home directory on " + sysName;
      }
    else
      {
	buffer.append("A home directory for user ");
	buffer.append(getLabel());
	buffer.append(" has been constructed on volume ");
	buffer.append(volName);
	buffer.append(".  The user's home directory has been registered in the ");
	buffer.append(mapName);
	buffer.append(" automounter home map.\n");

	subject = "User " + getLabel() + " home directory created";
      }

    editset.logMail(addresses, subject, buffer.toString());
  }

  /**
   *
   * This method runs from userCustom's commitPhase2() and runs an external
   * script that can do whatever bookkeeping might be desired when a user
   * is taken out of the passwd/user_info file generated by Ganymede.  This
   * may include removing the user's mailbox, home directory, and files, or
   * simply notifying someone that the user is no longer valid.
   *
   */

  private void deleteUserExternals()
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("userCustom: " + getLabel() + ", in deleteUserExternals().");
      }

    handleUserDelete(getLabel());

    // get the volumes defined for the user on auto.home.default

    DBObject obj = getOriginal();

    InvidDBField mapEntries = (InvidDBField) obj.getField(userSchema.VOLUMES);
    Vector entries = mapEntries.getValuesLocal();

    if (entries.size() < 1)
      {
	System.err.println("Couldn't handle deleteUserExternals for user " + getLabel() +
			   ", because we don't have a volume defined");
	return;
      }

    for (int i = 0; i < entries.size(); i++)
      {
	mail_user_removed_from_vol((Invid) entries.elementAt(i));
      }
  }

  /**
   * Helper method to send out mail to owners of the system that the
   * user's home directory is being scrubbed from.
   */

  private void mail_user_removed_from_vol(Invid entryInvid)
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    StringBuilder buffer = new StringBuilder();

    DBObject entryObj = getDBSession().viewDBObject(entryInvid, true);

    Invid mapInvid = (Invid) entryObj.getFieldValueLocal(mapEntrySchema.MAP);
    DBObject mapObj = getDBSession().viewDBObject(mapInvid, true);

    String mapName = mapObj.getLabel();

    Invid volumeInvid = (Invid) entryObj.getFieldValueLocal(mapEntrySchema.VOLUME);
    DBObject volumeObj = getDBSession().viewDBObject(volumeInvid, true);
    String volName = volumeObj.getLabel();
    String volPath = (String) volumeObj.getFieldValueLocal(volumeSchema.PATH);

    Invid sysInvid = (Invid) volumeObj.getFieldValueLocal(volumeSchema.HOST);
    DBObject sysObj = getDBSession().viewDBObject(sysInvid, true);
    String sysName = sysObj.getLabel();

    List<Invid> objects = new ArrayList<Invid>();
    objects.add(sysInvid);
    Set<String> addresses = DBLog.calculateOwnerAddresses(objects, getDBSession());

    String subject = null;

    buffer.append("User ");
    buffer.append(getLabel());
    buffer.append(" has been removed from volume ");
    buffer.append(volName);
    buffer.append(" in the ");
    buffer.append(mapName);
    buffer.append(" automounter home map.\n\nSince you are listed in the Ganymede");
    buffer.append(" system database as an administrator for a system contained in");
    buffer.append(" volume ");
    buffer.append(volName);
    buffer.append(", you need to take whatever action is appropriate to remove this user");
    buffer.append(" from ");
    buffer.append(volName);
    buffer.append(" if you are sure that the user will no longer be using his or her directory");
    buffer.append(" on this volume.\n\n");
    buffer.append("Volume ");
    buffer.append(volName);
    buffer.append(" is currently defined as:\n");
    buffer.append(sysName);
    buffer.append(":");
    buffer.append(volPath);
    buffer.append("\n\nThanks for your cooperation.\nYour friend,\n\tGanymede.\n");

    subject = "User " + getLabel() + " needs to be removed on " + sysName;

    editset.logMail(addresses, subject, buffer.toString());
  }

  /**
   * This method handles external actions for deleting a user.
   */

  private void handleUserDelete(String name)
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    // This would be unusual for a delete, but..

    if (Ganymede.log == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("userCustom.handleUserDelete(): user " + name +
			   "is being deleted");
      }

    try
      {
	if (deleteHandler == null)
	  {
	    deleteFilename = System.getProperty("ganymede.builder.scriptlocation");

	    if (deleteFilename != null)
	      {
		// make sure we've got the path separator at the end of
		// deleteFilename, add our script name

		deleteFilename = PathComplete.completePath(deleteFilename) + "scripts/user_deleter";

		deleteHandler = new File(deleteFilename);
	      }
	    else
	      {
		Ganymede.debug("userCustom.handleUserDelete(): Couldn't find " +
			       "ganymede.builder.scriptlocation property");
	      }
	  }

	if (deleteHandler.exists())
	  {
	    try
	      {
		String execLine = deleteFilename + " " + name;

		if (debug)
		  {
		    System.err.println("handleUserDelete: running " + execLine);
		  }

		try
		  {
		    if (debug)
		      {
			System.err.println("handleUserDelete: blocking");
		      }

		    int result = FileOps.runProcess(execLine);

		    if (debug)
		      {
			System.err.println("handleUserDelete: done");
		      }

		    if (result != 0)
		      {
			Ganymede.debug("Couldn't handle externals for deleting user " + name +
				       "\n" + deleteFilename +
				       " returned a non-zero result: " + result);
		      }
		  }
		catch (InterruptedException ex)
		  {
		    Ganymede.debug("Couldn't handle externals for deleting user " + name + ": " +
				   ex.getMessage());
		  }
	      }
	    catch (IOException ex)
	      {
		Ganymede.debug("Couldn't handle externals for deleting user " + name + ": " +
			       ex.getMessage());
	      }
	  }
      }
    finally
      {
	Invid admin = getGSession().getPermManager().getPersonaInvid();
	String adminName = getGSession().getPermManager().getUserName();
	List<Invid> objects = new ArrayList<Invid>();
	objects.add(getInvid());

	StringBuilder buffer = new StringBuilder();

	buffer.append("User ");
	buffer.append(name);
	buffer.append(" has been expunged from the Ganymede database.\n\n");

	editset.logEvent("userdeleted",
			 buffer.toString(),
			 admin, adminName, objects, null);
      }
  }

  /**
   * This method is designed to send out mail notifying admins of changes
   * made to a user's volume mappings, if any
   */

  private void handleVolumeChanges()
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    Hashtable oldEntryMap = new Hashtable();
    Hashtable newEntryMap = new Hashtable();

    Hashtable oldVolMap = new Hashtable();
    Hashtable newVolMap = new Hashtable();

    Vector oldMapNames = new Vector();
    Vector newMapNames = new Vector();

    Vector oldVolumes = new Vector();
    Vector newVolumes = new Vector();

    Invid mapEntryInvid;
    Invid volumeId;

    DBObject mapEntryObj;
    mapEntryCustom mapEntry;
    String mapName;

    Vector oldEntries = original.getFieldValuesLocal(userSchema.VOLUMES);

    if (oldEntries != null)
      {
	for (int i = 0; i < oldEntries.size(); i++)
	  {
	    mapEntryInvid = (Invid) oldEntries.elementAt(i);

	    mapEntryObj = getDBSession().viewDBObject(mapEntryInvid);

	    if (mapEntryObj instanceof mapEntryCustom)
	      {
		mapEntry = (mapEntryCustom) mapEntryObj;

		mapName = mapEntry.getOriginalMapName();
		volumeId = mapEntry.getOriginalVolumeInvid();

		oldVolumes.addElement(volumeId);
		oldEntryMap.put(mapName, mapEntryInvid);
		oldMapNames.addElement(mapName);

		// if we see the same volume in multiple maps, we'll just
		// remember the last one seen.. doesn't matter much, for
		// our purposes

		oldVolMap.put(volumeId, mapEntryInvid);

		if (debug)
		  {
		    System.err.println("Old entry.. " + mapName + ", " + volumeId);
		  }
	      }
	  }
      }

    Vector newEntries = getFieldValuesLocal(userSchema.VOLUMES);

    if (newEntries != null)
      {
	for (int i = 0; i < newEntries.size(); i++)
	  {
	    mapEntryInvid = (Invid) newEntries.elementAt(i);
	    mapEntryObj = getDBSession().viewDBObject(mapEntryInvid);

	    if (mapEntryObj instanceof mapEntryCustom)
	      {
		mapEntry = (mapEntryCustom) mapEntryObj;

		mapName = mapEntry.getMapName();
		volumeId = mapEntry.getVolumeInvid();

		newVolumes.addElement(volumeId);
		newEntryMap.put(mapName, mapEntryInvid);
		newMapNames.addElement(mapName);

		// if we see the same volume in multiple maps, we'll just
		// remember the last one seen.. doesn't matter much, for
		// our purposes

		newVolMap.put(volumeId, mapEntryInvid);

		if (debug)
		  {
		    System.err.println("New entry.. " + mapName + ", " + volumeId);
		  }
	      }
	  }
      }

    Vector addedVolumes = VectorUtils.difference(newVolumes, oldVolumes);
    Vector deletedVolumes = VectorUtils.difference(oldVolumes, newVolumes);

    Vector keptMapNames = VectorUtils.intersection(newMapNames, oldMapNames);

    for (int i = 0; i < keptMapNames.size(); i++)
      {
	mapName = (String) keptMapNames.elementAt(i);

	if (debug)
	  {
	    System.err.println("Checking map " + mapName + " for a volume change");
	  }

	Invid oldMapEntryInvid = (Invid) oldEntryMap.get(mapName);
	Invid newMapEntryInvid = (Invid) newEntryMap.get(mapName);

	if (oldMapEntryInvid.equals(newMapEntryInvid))
	  {
	    // we know the map entry obj is an editing copy, don't
	    // need to check here

	    mapEntry = (mapEntryCustom) getDBSession().viewDBObject(oldMapEntryInvid);

	    Invid oldVolInvid = mapEntry.getOriginalVolumeInvid();
	    Invid newVolInvid = mapEntry.getVolumeInvid();

	    if (!oldVolInvid.equals(newVolInvid))
	      {
		if (debug)
		  {
		    System.err.println("In map " + mapName + ", old vol was " + oldVolInvid +
				       ", is now " + newVolInvid);
		  }

		// we have moved the user's home directory on this map.. we won't
		// try to create the new home directory ourselves

		user_moved_from_vol_to_vol(oldVolInvid, newVolInvid, mapName);

		// we've already handled notification for the moving
		// between these volumes, don't need to do anything more
		// for it

		deletedVolumes.removeElement(oldVolInvid);
		addedVolumes.removeElement(newVolInvid);
	      }
	  }
      }

    for (int i = 0; i < addedVolumes.size(); i++)
      {
	volumeId = (Invid) addedVolumes.elementAt(i);

	if (debug)
	  {
	    System.err.println("Gained volume " + volumeId);
	  }

	// the user might have the same volume registered on multiple
	// maps, but we don't care enough to send mail out for it

	user_added_to_vol((Invid) newVolMap.get(volumeId));
      }

    for (int i = 0; i < deletedVolumes.size(); i++)
      {
	volumeId = (Invid) deletedVolumes.elementAt(i);

	if (debug)
	  {
	    System.err.println("Lost volume " + volumeId);
	  }

	// the user might have had the same volume registered on
	// multiple maps, but we don't care enough to send mail out
	// for it

	mail_user_removed_from_vol((Invid) oldVolMap.get(volumeId));
      }
  }

  /**
   * <P>This method takes care of executing whatever external code is required
   * to handle this user being moved from volume to volume</P>
   *
   * @param oldVolume Invid for old volume listed on a given map
   * @param newVolume Invid for new volume listed on a given map
   * @param mapName Name of the map this user is being moved on.
   */

  private void user_moved_from_vol_to_vol(Invid oldVolume, Invid newVolume, String mapName)
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    DBObject volumeObj;
    DBObject sysObj;

    String oldVolName;
    String oldVolPath;
    Invid oldSysInvid;
    String oldSysName;

    String newVolName;
    String newVolPath;
    Invid newSysInvid;
    String newSysName;

    Vector objects = new Vector();

    StringBuilder buffer = new StringBuilder();

    /* -- */

    volumeObj = getDBSession().viewDBObject(oldVolume, true);

    oldVolName = volumeObj.getLabel();
    oldVolPath = (String) volumeObj.getFieldValueLocal(volumeSchema.PATH);
    oldSysInvid = (Invid) volumeObj.getFieldValueLocal(volumeSchema.HOST);
    objects.addElement(oldSysInvid);
    sysObj = getDBSession().viewDBObject(oldSysInvid, true);
    oldSysName = sysObj.getLabel();

    volumeObj = getDBSession().viewDBObject(newVolume);

    newVolName = volumeObj.getLabel();
    newVolPath = (String) volumeObj.getFieldValueLocal(volumeSchema.PATH);
    newSysInvid = (Invid) volumeObj.getFieldValueLocal(volumeSchema.HOST);
    objects.addElement(newSysInvid);
    sysObj = getDBSession().viewDBObject(newSysInvid);
    newSysName = sysObj.getLabel();

    Set<String> addresses = DBLog.calculateOwnerAddresses(objects, getDBSession());

    buffer.append("Hi.  User ");
    buffer.append(getLabel());
    buffer.append(" was moved from volume ");
    buffer.append(oldVolName);
    buffer.append(" to volume ");
    buffer.append(newVolName);
    buffer.append(" in the ");
    buffer.append(mapName);
    buffer.append(" automounter home map.\n\nSince you are listed in the Ganymede system database");
    buffer.append(" as an administrator for a system contained in volume ");
    buffer.append(oldVolName);
    buffer.append(", you need to take whatever action is appropriate to move this user's");
    buffer.append(" directory from ");
    buffer.append(oldVolName);
    buffer.append(" if you are sure that the user will no longer be using his or her directory");
    buffer.append(" on this volume.\n\n");
    buffer.append("Volume ");
    buffer.append(oldVolName);
    buffer.append(" is currently defined as:\n\t");
    buffer.append(oldSysName);
    buffer.append(":");
    buffer.append(oldVolPath);
    buffer.append("\n\n");
    buffer.append("Volume ");
    buffer.append(newVolName);
    buffer.append(" is currently defined as:\n\t");
    buffer.append(newSysName);
    buffer.append(":");
    buffer.append(newVolPath);
    buffer.append("\n\n");
    buffer.append("Thanks for your cooperation.\nYour friend,\n\tGanymede.\n");

    editset.logMail(addresses,
		    "User home directory on map " + mapName + " moved",
		    buffer.toString());
  }

  /**
   * This method handles external actions for renaming a user.
   */

  private void handleUserRename(String orig, String newname)
  {
    boolean success = false;

    /* -- */

    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("userCustom.handleUserRename(): user " + orig +
			   "is being renamed to " + newname);
      }

    try
      {
	if (renameHandler == null)
	  {
	    renameFilename = System.getProperty("ganymede.builder.scriptlocation");

	    if (renameFilename != null)
	      {
		// make sure we've got the path separator at the end of
		// renameFilename, add our script name

		renameFilename = PathComplete.completePath(renameFilename) + "scripts/directory_namer";

		renameHandler = new File(renameFilename);
	      }
	    else
	      {
		Ganymede.debug("userCustom.handleUserRename(): Couldn't find " +
			       "ganymede.builder.scriptlocation property");
	      }
	  }

	if (renameHandler.exists())
	  {
	    try
	      {
		String execLine = renameFilename + " " + orig + " " + newname;

		if (debug)
		  {
		    System.err.println("handleUserRename: running " + execLine);
		  }

		try
		  {
		    if (debug)
		      {
			System.err.println("handleUserRename: blocking");
		      }

		    int result = FileOps.runProcess(execLine);

		    if (debug)
		      {
			System.err.println("handleUserRename: done");
		      }

		    if (result != 0)
		      {
			Ganymede.debug("Couldn't handle externals for renaming user " + orig +
				       " to " + newname + "\n" + renameFilename +
				       " returned a non-zero result: " + result);
		      }
		    else
		      {
			success = true;
		      }
		  }
		catch (InterruptedException ex)
		  {
		    Ganymede.debug("Couldn't handle externals for renaming user " + orig +
				   " to " +
				   newname + "\n" +
				   ex.getMessage());
		  }
	      }
	    catch (IOException ex)
	      {
		Ganymede.debug("Couldn't handle externals for renaming user " + orig +
			       " to " +
			       newname + "\n" +
			       ex.getMessage());
	      }
	  }
      }
    finally
      {
	Invid admin = getGSession().getPermManager().getPersonaInvid();
	String adminName = getGSession().getPermManager().getUserName();
	Vector objects = new Vector();
	objects.addElement(getInvid());

	StringBuilder buffer = new StringBuilder();

	buffer.append("User ");
	buffer.append(orig);
	buffer.append(" has been renamed to ");
	buffer.append(newname);
	buffer.append(".\n\n");

	if (success)
	  {
	    buffer.append("The user's main home directory has been renamed.  You may need ");
	    buffer.append("to take some action to make sure that the user's account name change ");
	    buffer.append("doesn't cause problems in your local scripts, etc.\n\n");
	  }
	else
	  {
	    buffer.append("The user's main home directory was not able to be properly renamed ");
	    buffer.append("by Ganymede.  You should contact a systems administrator on the user's");
	    buffer.append("main server to make sure his or her home directory is renamed properly.\n\n");
	    buffer.append("In addition, you may need ");
	    buffer.append("to take some action to make sure that the user's account name change ");
	    buffer.append("doesn't cause problems in your local scripts, etc.\n\n");
	  }

	editset.logEvent("userrenamed",
			 buffer.toString(),
			 admin, adminName, objects, null);
      }
  }
}
