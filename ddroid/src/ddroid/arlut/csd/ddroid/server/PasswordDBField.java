/*
   GASH 2

   PasswordDBField.java

   The GANYMEDE object storage system.

   Created: 21 July 1997

   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   Last Mod Date: $Date$
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

package arlut.csd.ddroid.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jcrypt.jcrypt;
import md5.MD5Crypt;
import arlut.csd.crypto.SSHA;
import arlut.csd.crypto.smbencrypt;
import arlut.csd.ddroid.common.ReturnVal;
import arlut.csd.ddroid.rmi.pass_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 PasswordDBField

------------------------------------------------------------------------------*/

/**
 * <P>PasswordDBField is a subclass of {@link arlut.csd.ddroid.server.DBField DBField}
 * for the storage and handling of password
 * fields in the {@link arlut.csd.ddroid.server.DBStore DBStore} on the Ganymede
 * server.</P>
 *
 * <P>The Directory Droid client talks to PasswordDBFields through the
 * {@link arlut.csd.ddroid.rmi.pass_field pass_field} RMI interface.</P> 
 *
 * <p>This class differs a bit from most subclasses of {@link
 * arlut.csd.ddroid.server.DBField DBField} in that the normal setValue()/getValue()
 * methods are non-functional.  Instead, there are special methods used to set or
 * access password information in crypted and non-crypted forms.</p>
 *
 * <p>Crypted passwords are stored in the UNIX crypt() format.  See the
 * {@link jcrypt jcrypt} class for details on the crypt hashing.</p>
 *
 * <p>There are no methods provided to allow remote access to password
 * information..  server-side code must locally access the {@link
 * arlut.csd.ddroid.server.PasswordDBField#getUNIXCryptText()
 * getUNIXCryptText()} and {@link
 * arlut.csd.ddroid.server.PasswordDBField#getPlainText() getPlainText()}
 * methods to get access to the password information.  Generally, even
 * in that case, only crypted password information will be available.
 * If this password field was configured to store encrypted passwords
 * by way of its {@link arlut.csd.ddroid.server.DBObjectBaseField
 * DBObjectBaseField}, this password field will never emit() the
 * plaintext to disk.  Instead, the crypt()'ed password information
 * will be retained for user authentication.  The plaintext of the
 * password <b>may</b> be retained in memory for the purpose of
 * replicating to systems that do not use the UNIX crypt() format for
 * password hashing, but only on a temporary basis, for those
 * passwords whose plaintext was provided to the server during its
 * operation.  Basically, it's for custom builder tasks that
 * need to be able to provide the plaintext of a stored password
 * for replication to a system with an incompatible hash format.</P>
 *
 * @see arlut.csd.ddroid.rmi.BaseField#setCrypted(boolean)
 * @see arlut.csd.ddroid.rmi.BaseField#setPlainText(boolean)
 */

public class PasswordDBField extends DBField implements pass_field {

  static final boolean debug = false;

  // ---

  /**
   * <p>Traditional Unix crypt()'ed pass</p>
   */

  private String cryptedPass;

  /**
   * <p>The complex md5crypt()'ed password, as in
   * OpenBSD, FreeBSD, Linux PAM, etc.</p>
   */

  private String md5CryptPass;

  /**
   * <p>The complex md5crypt()'ed password, with the magic string
   * used by Apache for their htpasswd file format.</p>
   */

  private String apacheMd5CryptPass;

  /**
   * <p>Plaintext password.. will never be saved to
   * disk if we have cryptedPass or md5CryptPass.</p>
   */

  private String uncryptedPass;

  /**
   * <p>Samba LANMAN hash, for Win95 clients</p>
   */

  private String lanHash;

  /**
   * <p>Samba md4 Unicode hash, for WinNT/2k clients</p>
   */

  private String ntHash;

  /**
   * <p>SSHA hash, for LDAP</p>
   */

  private String sshaHash;

  /* -- */

  /**
   * <p>Receive constructor.  Used to create a PasswordDBField from a DBStore/DBJournal
   * DataInput stream.</p>
   */

  PasswordDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = null;
    this.owner = owner;
    this.fieldcode = definition.getID();
    receive(in, definition);
  }

  /** 
   * <p>No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the {@link
   * arlut.csd.ddroid.server.DBObjectBase DBObjectBase} definition
   * indicates that a given field may be present, but for which no
   * value has been stored in the {@link arlut.csd.ddroid.server.DBStore
   * DBStore}.</p>
   *
   * <p>Used to provide the client a template for 'creating' this
   * field if so desired.</p> 
   */

  PasswordDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.fieldcode = definition.getID();
    
    value = null;
  }

  /**
   * Copy constructor.
   */

  public PasswordDBField(DBObject owner, PasswordDBField field)
  {
    this.owner = owner;
    this.fieldcode = field.getID();

    cryptedPass = field.cryptedPass;
    md5CryptPass = field.md5CryptPass;
    apacheMd5CryptPass = field.apacheMd5CryptPass;
    uncryptedPass = field.uncryptedPass;
    lanHash = field.lanHash;
    ntHash = field.ntHash;
    sshaHash = field.sshaHash;
  }

  /**
   * <p>Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.</p>
   *
   * @see arlut.csd.ddroid.rmi.db_field
   */

  public boolean isDefined()
  {
    return (cryptedPass != null || md5CryptPass != null ||
	    apacheMd5CryptPass != null || uncryptedPass != null || lanHash != null
	    || ntHash != null || sshaHash != null);
  }

  /**
   * <p>This method is used to mark a field as undefined when it is
   * checked out for editing.  Different subclasses of DBField will
   * implement this in different ways.  Any namespace values claimed
   * by the field will be released, and when the transaction is
   * committed, this field will be released.</p>
   */

  public synchronized ReturnVal setUndefined(boolean local)
  {
    if (isEditable(local))
      {
	clear_stored();

	return null;
      }

    return Ganymede.createErrorDialog("Permissions Error",
				      "Don't have permission to clear this password field\n" +
				      getName());
  }

  /**
   * <p>private helper to clear all stored password information in this field</p>
   */

  private synchronized final void clear_stored()
  {
    cryptedPass = null;
    md5CryptPass = null;
    apacheMd5CryptPass = null;
    uncryptedPass = null;
    ntHash = null;
    lanHash = null;
    sshaHash = null;
  }

  /**
   * <p>Returns true if obj is a field with the same value(s) as
   * this one.</p>
   *
   * <p>This method is ok to be synchronized because it does not
   * call synchronized methods on any other object.</p>
   */

  public synchronized boolean equals(Object obj)
  {
    if (!(obj.getClass().equals(this.getClass())))
      {
	return false;
      }

    PasswordDBField origP = (PasswordDBField) obj;

    return (streq(cryptedPass, origP.cryptedPass) &&
	    streq(md5CryptPass, origP.md5CryptPass) &&
	    streq(apacheMd5CryptPass, origP.apacheMd5CryptPass) &&
	    streq(uncryptedPass, origP.uncryptedPass) && 
	    streq(lanHash, origP.lanHash) && 
	    streq(ntHash, origP.ntHash) &&
	    streq(sshaHash, origP.sshaHash));
  }

  /**
   * <p>Convenience null-friendly string comparison helper.</p>
   */

  private final boolean streq(String str1, String str2)
  {
    if (str1 == null && str2 == null)
      {
	return true;
      }

    if (str1 == null || str2 == null)
      {
	return false;
      }

    return str1.equals(str2);
  }

  /**
   * <p>This method copies the current value of this DBField
   * to target.  The target DBField must be contained within a
   * checked-out DBEditObject in order to be updated.  Any actions
   * that would normally occur from a user manually setting a value
   * into the field will occur.</p>
   *
   * <p>NOTE: this method is mainly used in cloning objects, and
   * {@link arlut.csd.ddroid.server.DBEditObject#cloneFromObject(arlut.csd.ddroid.server.DBSession, arlut.csd.ddroid.server.DBObject, boolean) cloneFromObject}
   * doesn't allow cloning of password fields by default.</p>
   *
   * @param target The DBField to copy this field's contents to.
   * @param local If true, permissions checking is skipped.
   */

  public synchronized ReturnVal copyFieldTo(PasswordDBField target, boolean local)
  {
    if (!local)
      {
	if (!verifyReadPermission())
	  {
	    return Ganymede.createErrorDialog("Copy field error",
					      "Can't copy field " + getName() + ", no read privileges");
	  }
      }
	
    if (!target.isEditable(local))
      {
	return Ganymede.createErrorDialog("Copy field error",
					  "Can't copy field " + getName() + ", no write privileges");
      }

    target.cryptedPass = cryptedPass;
    target.md5CryptPass = md5CryptPass;
    target.apacheMd5CryptPass = apacheMd5CryptPass;
    target.lanHash = lanHash;
    target.ntHash = ntHash;
    target.uncryptedPass = uncryptedPass;
    target.sshaHash = sshaHash;

    return null;		// simple success value
  }

  /**
   * <p>Object value of DBField.  Used to represent value in value hashes.
   * Subclasses need to override this method in subclass.</p>
   */

  public Object key()
  {
    throw new IllegalArgumentException("PasswordDBFields may not be tracked in namespaces");
  }

  /**
   * <p>This method is used to return a copy of this field, with the field's owner
   * set to newOwner.</p>
   */

  public DBField getCopy(DBObject newOwner)
  {
    return new PasswordDBField(newOwner, this);
  }

  public Object clone()
  {
    return new PasswordDBField(owner, this);
  }

  void emit(DataOutput out) throws IOException
  {
    boolean wrote_hash = false;

    /* -- */

    // at 2.1 we write out all hashes all the time, and the
    // plaintext if we are told to, or if we don't have any
    // hashed form of it to use

    if (getFieldDef().isCrypted())
      {
	cryptedPass = getUNIXCryptText();

	if (cryptedPass == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(cryptedPass);
	    wrote_hash = true;
	  }
      }
    else
      {
	out.writeUTF("");
      }

    if (getFieldDef().isMD5Crypted())
      {
	md5CryptPass = getMD5CryptText();

	if (md5CryptPass == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(md5CryptPass);
	    wrote_hash = true;
	  }
      }
    else
      {
	out.writeUTF("");
      }

    if (getFieldDef().isApacheMD5Crypted())
      {
	apacheMd5CryptPass = getApacheMD5CryptText();

	if (apacheMd5CryptPass == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(apacheMd5CryptPass);
	    wrote_hash = true;
	  }
      }
    else
      {
	out.writeUTF("");
      }

    if (getFieldDef().isWinHashed())
      {
	lanHash = getLANMANCryptText();

	if (lanHash == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(lanHash);
	    wrote_hash = true;
	  }

	ntHash = getNTUNICODECryptText();

	if (ntHash == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(ntHash);
	    wrote_hash = true;
	  }
      } 
    else
      {
	out.writeUTF("");
	out.writeUTF("");
      }

    if (getFieldDef().isSSHAHashed())
      {
	sshaHash = getSSHAHashText();

	if (sshaHash == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(sshaHash);
	    wrote_hash = true;
	  }
      }
    else
      {
	out.writeUTF("");
      }

    // at file version 2.1, we write out plaintext if the field
    // definition requires it, or if we were not able to write
    // out any crypttext

    if (getFieldDef().isPlainText() || !wrote_hash)
      {
	if (uncryptedPass == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(uncryptedPass);
	  }
      }
    else
      {
	out.writeUTF("");
      }
  }

  void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    // we radically simplified PasswordDBField's on-disk format at
    // file version 2.1

    if (Ganymede.db.isAtLeast(2,1))
      {
	cryptedPass = in.readUTF();

	if (cryptedPass.equals(""))
	  {
	    cryptedPass = null;
	  }

	md5CryptPass = in.readUTF();

	if (md5CryptPass.equals(""))
	  {
	    md5CryptPass = null;
	  }

	// at file format 2.4 we added the Apache-hashed password format

	if (Ganymede.db.isAtLeast(2,4))
	  {
	    apacheMd5CryptPass = in.readUTF();
	    
	    if (apacheMd5CryptPass.equals(""))
	      {
		apacheMd5CryptPass = null;
	      }
	  }

	lanHash = in.readUTF();

	if (lanHash.equals(""))
	  {
	    lanHash = null;
	  }

	ntHash = in.readUTF();

	if (ntHash.equals(""))
	  {
	    ntHash = null;
	  }

	// we added SSHA Hash at file format 2.5

	if (Ganymede.db.isAtLeast(2,5))
	  {
	    sshaHash = in.readUTF();
	    
	    if (sshaHash.equals(""))
	      {
		sshaHash = null;
	      }
	  }

	uncryptedPass = in.readUTF();

	if (uncryptedPass.equals(""))
	  {
	    uncryptedPass = null;
	  }

	return;
      }

    // From here on down we do things the old, hard way

    // at file format 1.10, we were keeping both crypted and unecrypted
    // passwords on disk.  Since then, we have decided to only write
    // out encrypted passwords if we are using them.

    if (Ganymede.db.isAtRev(1,10))
      {
	cryptedPass = in.readUTF();

	if (cryptedPass.equals(""))
	  {
	    cryptedPass = null;
	  }
	
	uncryptedPass = in.readUTF();

	if (uncryptedPass.equals(""))
	  {
	    uncryptedPass = null;
	  }

	return;
      }

    // if we're not looking at file version 1.10, the crypted password is
    // the first thing we'll see, if the field definition specifies the
    // use of it

    if (definition.isCrypted())
      {
	cryptedPass = in.readUTF();

	if (cryptedPass.equals(""))
	  {
	    cryptedPass = null;
	  }

	if (Ganymede.db.isBetweenRevs(1,13,1,16))
	  {
	    in.readUTF();	// skip old-style (buggy) md5 pass
	  }
      }

    // now we see if we expect to see an MD5Crypt()'ed  password

    // note that even though we test for >= 1.16, we won't get to this point
    // if we are using the >= 2.1 logic
	
    if (Ganymede.db.isAtLeast(1,16))
      {
	if (definition.isMD5Crypted())
	  {
	    md5CryptPass = in.readUTF();
		
	    if (md5CryptPass.equals(""))
	      {
		md5CryptPass = null;
	      }
	  }
	else
	  {
	    md5CryptPass = null;
	  }
      }

    if (definition.isCrypted() || definition.isMD5Crypted())
      {
	uncryptedPass = null;
      }
    else
      {
	uncryptedPass = in.readUTF();
	
	if (uncryptedPass.equals(""))
	  {
	    uncryptedPass = null;
	  }
	
	cryptedPass = null;
	md5CryptPass = null;
      }
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().</p>
   */

  synchronized void emitXML(XMLDumpContext dump) throws IOException
  {
    dump.indent();

    dump.startElement(this.getXMLName());
    dump.startElement("password");
    
    if (uncryptedPass != null && 
	(dump.doDumpPlaintext() || (md5CryptPass == null && cryptedPass == null)))
      {
	dump.attribute("plaintext", uncryptedPass);
      }

    if (cryptedPass != null)
      {
	dump.attribute("crypt", cryptedPass);
      }
	
    if (md5CryptPass != null)
      {
	dump.attribute("md5crypt", md5CryptPass);
      }

    if (apacheMd5CryptPass != null)
      {
	dump.attribute("apachemd5crypt", apacheMd5CryptPass);
      }

    if (lanHash != null)
      {
	dump.attribute("lanman", lanHash);
      }

    if (ntHash != null)
      {
	dump.attribute("ntmd4", ntHash);
      }

    if (sshaHash != null)
      {
	dump.attribute("ssha", sshaHash);
      }

    dump.endElement("password");
    dump.endElement(this.getXMLName());
  }

  /**
   * <p>Standard {@link arlut.csd.ddroid.rmi.db_field db_field} method
   * to retrieve the value of this field.  Because we are holding sensitive
   * password information, this method always returns null.. we don't want
   * to make password values available to the client under any circumstances.
   */

  public Object getValue()
  {
    return null;
  }

  /** 
   * <p>Returns an Object carrying the value held in this field.</p>
   *
   * <p>This is intended to be used within the Directory Droid server, it bypasses
   * the permissions checking that getValues() does.</p>
   *
   * <p>Note that this method will always return null, as you need to use
   * the special Password-specific value accessors to get access to the
   * password information in crypted or non-crypted form.</p>
   */

  public Object getValueLocal()
  {
    return null;
  }

  // ****
  //
  // type specific value accessors
  //
  // ****

  /**
   * <P>This method returns a text encoded value for this DBField
   * without checking permissions.</P>
   *
   * <P>This method avoids checking permissions because it is used on
   * the server side only and because it is involved in the 
   * {@link arlut.csd.ddroid.server.DBObject#getLabel() getLabel()}
   * logic for {@link arlut.csd.ddroid.server.DBObject DBObject}, 
   * which is invoked from {@link arlut.csd.ddroid.server.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ddroid.server.GanymedeSession#getPerm(arlut.csd.ddroid.server.DBObject) getPerm()} 
   * method.</P>
   *
   * <P>If this method checked permissions and the getPerm() method
   * failed for some reason and tried to report the failure using
   * object.getLabel(), as it does at present, the server could get
   * into an infinite loop.</P>
   */

  public synchronized String getValueString()
  {
    if (this.isDefined())
      {
	StringBuffer result = new StringBuffer();

	result.append("< ");

	if (cryptedPass != null)
	  {
	    result.append("crypt ");
	  }

	if (md5CryptPass != null)
	  {
	    result.append("md5crypt ");
	  }
	
	if (apacheMd5CryptPass != null)
	  {
	    result.append("apachemd5crypt ");
	  }
	
	if (lanHash != null)
	  {
	    result.append("lanman ");
	  }

	if (ntHash != null)
	  {
	    result.append("ntmd4 ");
	  }

	if (sshaHash != null)
	  {
	    result.append("ssha ");
	  }

	if (uncryptedPass != null)
	  {
	    result.append("text ");
	  }

	result.append(">");

	return result.toString();
      }
    else
      {
	return null;
      }
  }

  /**
   * The default getValueString() encoding is acceptable.
   */

  public String getEncodingString()
  {
    return getValueString();
  }

  /**
   * <p>Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.</p>
   *
   * <p>If there is no change in the field, null will be returned.</p>
   */

  public String getDiffString(DBField orig)
  {
    PasswordDBField origP;

    /* -- */

    if (!(orig instanceof PasswordDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origP = (PasswordDBField) orig;

    if (!this.equals(origP))
      {
	return "\tPassword changed\n";
      }
    else
      {
	return null;
      }
  }

  // ****
  //
  // pass_field methods 
  //
  // ****

  /**
   * <p>Returns the maximum acceptable string length
   * for this field.</p>
   *
   * @see arlut.csd.ddroid.rmi.pass_field
   */

  public int maxSize()
  {
    return getFieldDef().getMaxLength();
  }

  /**
   * <p>Returns the minimum acceptable string length
   * for this field.</p>
   *
   * @see arlut.csd.ddroid.rmi.pass_field
   */

  public int minSize()
  {
    return getFieldDef().getMinLength();
  }

  /**
   * <p>Returns a string containing the list of acceptable characters.
   * If the string is null, it should be interpreted as meaning all
   * characters not listed in disallowedChars() are allowable by
   * default.</p>
   *
   * @see arlut.csd.ddroid.rmi.pass_field
   */

  public String allowedChars()
  {
    return getFieldDef().getOKChars();
  }

  /**
   * <p>Returns a string containing the list of forbidden
   * characters for this field.  If the string is null,
   * it should be interpreted as meaning that no characters
   * are specifically disallowed.</p>
   *
   * @see arlut.csd.ddroid.rmi.pass_field
   */

  public String disallowedChars()
  {
    return getFieldDef().getBadChars();
  }

  /**
   * <p>Convenience method to identify if a particular
   * character is acceptable in this field.</p>
   *
   * @see arlut.csd.ddroid.rmi.pass_field
   */

  public boolean allowed(char c)
  {
    if (allowedChars() != null && (allowedChars().indexOf(c) == -1))
      {
	return false;
      }

    if (disallowedChars() != null && (disallowedChars().indexOf(c) != -1))
      {
	return false;
      }
    
    return true;
  }

  /**
   * <p>Returns true if the password stored in this field is hash-crypted.</p>
   *
   * @see arlut.csd.ddroid.rmi.pass_field
   */

  public boolean crypted()
  {
    return (getFieldDef().isCrypted());
  }

  /**
   * <p>This method is used for authenticating a provided plaintext
   * password against the stored contents of this password field.  The
   * password field may have stored the password in plaintext, or in
   * any of a variety of cryptographic hash formats.  matchPlainText()
   * will perform whatever operation on the provided plaintext as is
   * required to determine whether or not it matches with the stored
   * password data.</p>
   *
   * @return true if the given plaintext matches the stored password
   *
   * @see arlut.csd.ddroid.rmi.pass_field
   */

  public synchronized boolean matchPlainText(String plaintext)
  {
    boolean success = false;

    /* -- */
    
    if (plaintext == null || !this.isDefined())
      {
	return false;
      }

    // test against our hashes in decreasing order of hashing fidelity

    if (uncryptedPass != null)
      {
	success = uncryptedPass.equals(plaintext); // most accurate
      }
    else if (sshaHash != null)
      {
	success = SSHA.matchSHAHash(sshaHash, plaintext);
      }
    else if (md5CryptPass != null)
      {
	success = md5CryptPass.equals(MD5Crypt.crypt(plaintext, getMD5Salt()));
      }
    else if (apacheMd5CryptPass != null)
      {
	success = apacheMd5CryptPass.equals(MD5Crypt.apacheCrypt(plaintext, getApacheMD5Salt()));
      }
    else if (ntHash != null)
      {
	success = ntHash.equals(smbencrypt.NTUNICODEHash(plaintext));
      }
    else if (lanHash != null)
      {
	success = lanHash.equals(smbencrypt.LANMANHash(plaintext));
      }
    else if (cryptedPass != null)
      {
	success = cryptedPass.equals(jcrypt.crypt(getSalt(), plaintext));
      }

    return success;
  }

  /**
   * <p>This server-side only method returns the UNIX-encrypted password text.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
   */

  public String getUNIXCryptText()
  {
    if (cryptedPass != null)
      {
	return cryptedPass;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return jcrypt.crypt(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /** 
   * <p>This server-side only method returns the md5crypt()-encrypted
   * hashed password text.</p>
   *
   * <p>This method is never meant to be available remotely.</p> 
   */

  public String getMD5CryptText()
  {
    if (md5CryptPass != null)
      {
	return md5CryptPass;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return MD5Crypt.crypt(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /** 
   * <p>This server-side only method returns the Apache md5crypt()-encrypted
   * hashed password text.</p>
   *
   * <p>This method is never meant to be available remotely.</p> 
   */

  public String getApacheMD5CryptText()
  {
    if (apacheMd5CryptPass != null)
      {
	return apacheMd5CryptPass;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return MD5Crypt.apacheCrypt(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /** 
   * <p>This server-side only method returns the LANMAN-compatible
   * password hash of the password data held in this field.</p>
   *
   * <p>This method is never meant to be available remotely.</p> 
   */

  public String getLANMANCryptText()
  {
    if (lanHash != null)
      {
	return lanHash;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return smbencrypt.LANMANHash(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /** 
   * <p>This server-side only method returns the Windows NT 4
   * SP3-compatible md4/Unicode password hash of the password data
   * held in this field.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
   */

  public String getNTUNICODECryptText()
  {
    if (ntHash != null)
      {
	return ntHash;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return smbencrypt.NTUNICODEHash(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /** 
   * <p>This server-side only method returns the Netscape SSHA (salted
   * SHA) LDAP hash of the password data held in this field.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
   */

  public String getSSHAHashText()
  {
    if (sshaHash != null)
      {
	return sshaHash;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return SSHA.getLDAPSSHAHash(uncryptedPass, null);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /**
   * <p>This server-side only method returns the plaintext password text,
   * if available.</p>
   */

  public String getPlainText()
  {
    return uncryptedPass;
  }

  /** 
   * <p>Method to obtain the SALT for a stored crypted password.  If
   * the client is going to submit a pre-crypted password for
   * comparison via matchCryptText(), it must be salted by the salt
   * returned by this method.</p>
   * 
   * <p>If the password is not stored in crypt() form, null will be
   * returned.</p> 
   */

  public String getSalt()
  {
    if (getFieldDef().isCrypted() && cryptedPass != null)
      {
	return cryptedPass.substring(0,2);
      }
    else
      {
	return null;
      }
  }

  /** 
   * <p>Method to obtain the SALT for a stored OpenBSD-style
   * md5crypt()'ed password.  If the client is going to submit a
   * pre-crypted password for comparison via matchMD5CryptText(), it
   * must be salted by the salt returned by this method.</p>
   *
   * <p>If the password is not stored in md5crypt() form,
   * null will be returned.</p>
   */

  public String getMD5Salt()
  {
    if (getFieldDef().isMD5Crypted() && md5CryptPass != null)
      {
	String salt = md5CryptPass;
	String magic = "$1$";

	if (salt.startsWith(magic))
	  {
	    salt = salt.substring(magic.length());
	  }
	
	/* It stops at the first '$', max 8 chars */
	
	if (salt.indexOf('$') != -1)
	  {
	    salt = salt.substring(0, salt.indexOf('$'));
	  }

	if (salt.length() > 8)
	  {
	    salt = salt.substring(0, 8);
	  }

	return salt;
      }
    else
      {
	return null;
      }
  }

  /** 
   * <p>Method to obtain the SALT for a stored Apache-style
   * md5crypt()'ed password.  If the client is going to submit a
   * pre-crypted Apache password for comparison via
   * matchMD5CryptText(), it must be salted by the salt returned by
   * this method.</p>
   *
   * <p>If the password is not stored in apacheMd5crypt() form,
   * null will be returned.</p>
   */

  public String getApacheMD5Salt()
  {
    if (getFieldDef().isApacheMD5Crypted() && apacheMd5CryptPass != null)
      {
	String salt = apacheMd5CryptPass;
	String magic = "$apr1$";

	if (salt.startsWith(magic))
	  {
	    salt = salt.substring(magic.length());
	  }
	
	/* It stops at the first '$', max 8 chars */
	
	if (salt.indexOf('$') != -1)
	  {
	    salt = salt.substring(0, salt.indexOf('$'));
	  }

	if (salt.length() > 8)
	  {
	    salt = salt.substring(0, 8);
	  }

	return salt;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>Sets the value of this field, if a scalar.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.</p> 
   */

  public ReturnVal setValue(Object value, boolean local, boolean noWizards)
  {
    throw new IllegalArgumentException("can't directly set the value on a password field");
  }

  /** 
   * <p>This method is used to set the password for this field,
   * crypting it in various ways if this password field is stored
   * crypted.</p>
   *
   * @see arlut.csd.ddroid.rmi.pass_field
   */

  public synchronized ReturnVal setPlainTextPass(String plaintext)
  {
    return setPlainTextPass(plaintext, false, false);
  }

  /** 
   * <p>This method is used to set the password for this field,
   * crypting it in various ways if this password field is stored
   * crypted.</p>
   */

  public synchronized ReturnVal setPlainTextPass(String plaintext, boolean local, boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;

    /* -- */

    retVal = verifyNewValue(plaintext);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	retVal = eObj.wizardHook(this, DBEditObject.SETPASSPLAIN, plaintext, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ((DBEditObject) owner).finalizeSetValue(this, null);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // reset all hashes to start things off

    clear_stored();

    // if we've got an empty string, clear the plaintext, too

    if (plaintext == null || plaintext.equals(""))
      {
	uncryptedPass = null;

	return retVal;
      }

    // else, go ahead and set everything

    uncryptedPass = plaintext;

    if (getFieldDef().isCrypted())
      {
	cryptedPass = jcrypt.crypt(plaintext);
      }

    if (getFieldDef().isMD5Crypted())
      {
	md5CryptPass = MD5Crypt.crypt(plaintext);
      }

    if (getFieldDef().isApacheMD5Crypted())
      {
	apacheMd5CryptPass = MD5Crypt.apacheCrypt(plaintext);
      }

    if (getFieldDef().isWinHashed())
      {
	lanHash = smbencrypt.LANMANHash(plaintext);
	ntHash = smbencrypt.NTUNICODEHash(plaintext);
      }

    if (getFieldDef().isSSHAHashed())
      {
	sshaHash = SSHA.getLDAPSSHAHash(plaintext, null);
      }

    return retVal;
  }

  /**
   * <p>This method is used to set a pre-crypted password for this field.</p>
   *
   * <p>This method will return an error dialog if this field does not store
   * passwords in UNIX crypted format.</p>
   *
   * <p>Because the UNIX crypt() hashing is not reversible, any MD5 and plain text
   * password information stored in this field will be lost.</p>
   *
   * @see arlut.csd.ddroid.rmi.pass_field
   */

  public ReturnVal setCryptPass(String text)
  {
    return setCryptPass(text, false, false);
  }

  /**
   * <p>This method is used to set a pre-crypted password for this field.</p>
   *
   * <p>This method will return an error dialog if this field does not store
   * passwords in UNIX crypted format.</p>
   *
   * <p>Because the UNIX crypt() hashing is not reversible, any MD5 and plain text
   * password information stored in this field will be lost.</p>
   */

  public ReturnVal setCryptPass(String text, boolean local, boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    if (!getFieldDef().isCrypted())
      {
	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setCryptTextPass()",
					  "Can't set a pre-crypted value into a plaintext-only password field");
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	retVal = eObj.wizardHook(this, DBEditObject.SETPASSCRYPT, text, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ((DBEditObject)owner).finalizeSetValue(this, null);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the crypt password is directly set, we lose 
	// plaintext and alternate hashes

	clear_stored();

	if ((text == null) || (text.equals("")))
	  {
	    cryptedPass = null;
	  }
	else
	  {
	    cryptedPass = text;
	  }
      }

    return retVal;
  }

  /**
   * <p>This method is used to set a pre-crypted OpenBSD-style
   * MD5Crypt password for this field.  This method will return
   * false if this password field is not stored crypted.</p>
   *
   * @see arlut.csd.ddroid.rmi.pass_field
   */

  public ReturnVal setMD5CryptedPass(String text)
  {
    return setMD5CryptedPass(text, false, false);
  }

  /**
   * <p>This method is used to set a pre-crypted OpenBSD-style
   * MD5Crypt password for this field.  This method will return
   * false if this password field is not stored crypted.</p>
   */

  public ReturnVal setMD5CryptedPass(String text, boolean local, boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    if (!getFieldDef().isMD5Crypted())
      {
	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setMD5CryptedPass()",
					  "Can't set a pre-crypted MD5Crypt value into a non-MD5Crypted password field");
      }

    if (text != null && !text.equals("") && (!text.startsWith("$1$") || (text.indexOf('$', 3) == -1)))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "setMD5CryptedPass() called with an improperly " +
					  "formatted FreeBSD-style password entry.");
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	retVal = eObj.wizardHook(this, DBEditObject.SETPASSMD5, text, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ((DBEditObject)owner).finalizeSetValue(this, null);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the md5CryptPass password is directly set, we lose 
	// plaintext and alternate hashes

	clear_stored();

	if ((text == null) || (text.equals("")))
	  {
	    md5CryptPass = null;
	  }
	else
	  {
	    md5CryptPass = text;
	  }
      }

    return retVal;
  }

  /**
   * <p>This method is used to set a pre-crypted Apache-style
   * MD5Crypt password for this field.  This method will return
   * false if this password field is not stored crypted.</p>
   *
   * @see arlut.csd.ddroid.rmi.pass_field
   */

  public ReturnVal setApacheMD5CryptedPass(String text)
  {
    return setApacheMD5CryptedPass(text, false, false);
  }

  /**
   * <p>This method is used to set a pre-crypted Apache-style
   * MD5Crypt password for this field.  This method will return
   * false if this password field is not stored crypted.</p>
   */

  public ReturnVal setApacheMD5CryptedPass(String text, boolean local, boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    if (!getFieldDef().isApacheMD5Crypted())
      {
	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setApacheMD5CryptedPass()",
					  "Can't set a pre-crypted ApacheMD5Crypt value into a non-ApacheMD5Crypted password field");
      }

    if (text != null && !text.equals("") && (!text.startsWith("$apr1$") || (text.indexOf('$', 6) == -1)))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "setApacheMD5CryptedPass() called with an improperly " +
					  "formatted Apache-style password entry.");
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	retVal = eObj.wizardHook(this, DBEditObject.SETPASSAPACHEMD5, text, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ((DBEditObject)owner).finalizeSetValue(this, null);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the apacheMd5CryptPass password is directly set, we lose 
	// plaintext and alternate hashes

	clear_stored();

	if ((text == null) || (text.equals("")))
	  {
	    apacheMd5CryptPass = null;
	  }
	else
	  {
	    apacheMd5CryptPass = text;
	  }
      }

    return retVal;
  }

  /**
   * <p>This method is used to set a pre-crypted OpenBSD-style
   * MD5Crypt password for this field.  This method will return
   * false if this password field is not stored crypted.</p>
   *
   * @see arlut.csd.ddroid.rmi.pass_field
   */

  public ReturnVal setWinCryptedPass(String LANMAN, String NTUnicodeMD4)
  {
    return setWinCryptedPass(LANMAN, NTUnicodeMD4, false, false);
  }

  /**
   * <p>This method is used to set a pre-crypted OpenBSD-style
   * MD5Crypt password for this field.  This method will return
   * false if this password field is not stored crypted.</p>
   */

  public ReturnVal setWinCryptedPass(String LANMAN, String NTUnicodeMD4, boolean local, boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    if (!getFieldDef().isWinHashed())
      {
	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setWinCryptedPass()",
					  "Can't set pre-crypted Samba hash values into a non-WinCrypted password field");
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.SETPASSWINHASHES, LANMAN, NTUnicodeMD4);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ((DBEditObject)owner).finalizeSetValue(this, null);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the windows hashes are set directly, we lose 
	// plaintext and alternate hashes

	clear_stored();

	if ((LANMAN == null) || (LANMAN.equals("")))
	  {
	    lanHash = null;
	  }
	else
	  {
	    lanHash = LANMAN;
	  }

	if ((NTUnicodeMD4 == null) || (NTUnicodeMD4.equals("")))
	  {
	    ntHash = null;
	  }
	else
	  {
	    ntHash = NTUnicodeMD4;
	  }
      }

    return retVal;
  }

  /**
   * <p>This method is used to force all known hashes into this password
   * field.  Directory Droid does no verifications to insure that all of these
   * hashes really match the same password, so caveat emptor.  If any of
   * these hashes are null or empty string, those hashes will be cleared.</p>
   *
   * <p>Calling this method will clear the password's stored plaintext,
   * if any.</p>
   *
   * <p>If this password field is not configured to support any of the
   * various hash formats in the Directory Droid schema, an error will be returned.</p>
   */

  public ReturnVal setAllHashes(String crypt,
				String md5crypt,
				String apacheMd5Crypt,
				String LANMAN, 
				String NTUnicodeMD4, 
				boolean local, 
				boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;
    boolean settingCrypt, settingMD5, settingApacheMD5, settingWin;

    /* -- */

    if (!isEditable(local))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    settingCrypt = (crypt != null && !crypt.equals(""));
    settingMD5 = (md5crypt != null && !md5crypt.equals(""));
    settingApacheMD5 = (apacheMd5Crypt != null && !apacheMd5Crypt.equals(""));
    settingWin = (LANMAN != null && !LANMAN.equals("")) || (NTUnicodeMD4 != null && !NTUnicodeMD4.equals(""));

    if (!settingCrypt && !settingWin && !settingMD5)
      {
	// clear it!

	return setPlainTextPass(null);
      }

    // nope, we're setting something.. let's find out what

    if (settingWin && !getFieldDef().isWinHashed())
      {
	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setAllHashes()",
					  "Can't set pre-crypted Samba hash values into a non-WinCrypted password field");
      }

    if (settingMD5 && !getFieldDef().isMD5Crypted())
      {
	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setAllHashes()",
					  "Can't set pre-crypted md5 hash values into a non-MD5Crypted password field");
      }

    if (settingMD5 && !getFieldDef().isMD5Crypted())
      {
	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setAllHashes()",
					  "Can't set pre-crypted md5 hash values into a non-MD5Crypted password field");
      }

    // it's easy to sanity-check md5 hashes

    if (settingApacheMD5 && (!apacheMd5Crypt.startsWith("$apr1$") || (md5crypt.indexOf('$', 6) == -1)))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "setAllHashes() called with an improperly " +
					  "formatted Apache-style md5Crypt password entry: " + apacheMd5Crypt);
      }

    if (settingCrypt && !getFieldDef().isCrypted())
      {
	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setAllHashes()",
					  "Can't set pre-crypted hash values into a non-Crypted password field");
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	if (settingWin)
	  {
	    retVal = eObj.wizardHook(this, DBEditObject.SETPASSWINHASHES, LANMAN, NTUnicodeMD4);

	    // if a wizard intercedes, we are going to let it take the ball.
	    
	    if (retVal != null && !retVal.doNormalProcessing)
	      {
		return retVal;
	      }
	  }

	if (settingMD5)
	  {
	    retVal = eObj.wizardHook(this, DBEditObject.SETPASSMD5, md5crypt, null);

	    // if a wizard intercedes, we are going to let it take the ball.
	    
	    if (retVal != null && !retVal.doNormalProcessing)
	      {
		return retVal;
	      }
	  }

	if (settingApacheMD5)
	  {
	    retVal = eObj.wizardHook(this, DBEditObject.SETPASSAPACHEMD5, apacheMd5Crypt, null);

	    // if a wizard intercedes, we are going to let it take the ball.
	    
	    if (retVal != null && !retVal.doNormalProcessing)
	      {
		return retVal;
	      }
	  }

	if (settingCrypt)
	  {
	    retVal = eObj.wizardHook(this, DBEditObject.SETPASSCRYPT, crypt, null);

	    // if a wizard intercedes, we are going to let it take the ball.
	    
	    if (retVal != null && !retVal.doNormalProcessing)
	      {
		return retVal;
	      }
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ((DBEditObject)owner).finalizeSetValue(this, null);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the hashes are set directly, we lose 
	// plaintext and alternate hashes

	clear_stored();

	if ((LANMAN == null) || (LANMAN.equals("")))
	  {
	    lanHash = null;
	  }
	else
	  {
	    lanHash = LANMAN;
	  }

	if ((NTUnicodeMD4 == null) || (NTUnicodeMD4.equals("")))
	  {
	    ntHash = null;
	  }
	else
	  {
	    ntHash = NTUnicodeMD4;
	  }

	if (settingCrypt)
	  {
	    cryptedPass = crypt;
	  }

	if (settingMD5)
	  {
	    md5CryptPass = md5crypt;
	  }

	if (settingApacheMD5)
	  {
	    apacheMd5CryptPass = apacheMd5Crypt;
	  }
      }

    return retVal;    
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof String));
  }

  /**
   * Generally only for when we get a plaintext submission..
   */

  public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj;
    String s;
    
    /* -- */

    if (!isEditable(true))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Submitted value " + o + " is not a string!  Major client error while" +
					  " trying to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    if (o == null)
      {
	return null; // assume we can null out this field
      }

    s = (String) o;

    if (s.length() > maxSize())
      {
	// string too long

	return Ganymede.createErrorDialog("Password Field Error",
					  "Submitted password" +
					  " is too long for field " + 
					  getName() + " in object " +
					  owner.getLabel() +
					  ", which has a length limit of " + 
					  maxSize());
      }

    if (s.length() < minSize())
      {
	return Ganymede.createErrorDialog("Password Field Error",
					  "Submitted password" +
					  " is too short for field " + 
					  getName() + " in object " +
					  owner.getLabel() +
					  ", which has a minimum length of " + 
					  minSize());
      }
    
    if (allowedChars() != null)
      {
	String okChars = allowedChars();
	
	for (int i = 0; i < s.length(); i++)
	  {
	    if (okChars.indexOf(s.charAt(i)) == -1)
	      {
		return Ganymede.createErrorDialog("Password Field Error",
						  "Submitted password" +
						  " contains the unacceptable character '" +
						  s.charAt(i) + "' for field " +
						  getName() + " in object " +
						  owner.getLabel() + ".");
	      }
	  }
      }
    
    if (disallowedChars() != null)
      {
	String badChars = disallowedChars();
	
	for (int i = 0; i < s.length(); i++)
	  {
	    if (badChars.indexOf(s.charAt(i)) != -1)
	      {
		return Ganymede.createErrorDialog("Password Field Error",
						  "Submitted password" +
						  " contains the unacceptable character '" +
						  s.charAt(i) + "' for field " +
						  getName() + " in object " +
						  owner.getLabel() + ".");
	      }
	  }
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, s);
  }
}
