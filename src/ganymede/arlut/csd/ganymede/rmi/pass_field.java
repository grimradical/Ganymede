/*

   pass_field.java

   Remote interface definition.

   Created: 21 July 1997

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

package arlut.csd.ganymede.rmi;

import java.rmi.RemoteException;

import arlut.csd.ganymede.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                      pass_field

------------------------------------------------------------------------------*/

public interface pass_field extends db_field {

  int maxSize() throws RemoteException;
  int minSize() throws RemoteException;

  /**
   * Returns a string containing the list of acceptable characters.
   * If the string is null, it should be interpreted as meaning all
   * characters not listed in disallowedChars() are allowable by
   * default.
   */

  String allowedChars() throws RemoteException;

  /**
   * Returns a string containing the list of forbidden
   * characters for this field.  If the string is null,
   * it should be interpreted as meaning that no characters
   * are specifically disallowed.
   */

  String disallowedChars() throws RemoteException;

  /**
   * Convenience method to identify if a particular
   * character is acceptable in this field.
   */

  boolean allowed(char c) throws RemoteException;

  /**
   * Returns true if the password stored in this field is hash-crypted
   * using the traditional Unix Crypt algorithm.
   */

  boolean crypted() throws RemoteException;

  /**
   * This method is used for authenticating a provided plaintext
   * password against the stored contents of this password field.  The
   * password field may have stored the password in plaintext, or in
   * any of a variety of cryptographic hash formats.  matchPlainText()
   * will perform whatever operation on the provided plaintext as is
   * required to determine whether or not it matches with the stored
   * password data.
   *
   * @return true if the given plaintext matches the stored password
   */

  boolean matchPlainText(String text) throws RemoteException;

  /** 
   * This method is used to set the password for this field,
   * crypting it in various ways if this password field is stored
   * crypted.
   */

  ReturnVal setPlainTextPass(String text) throws RemoteException;

  /**
   * This method is used to set a pre-hashed password for this field,
   * using the traditional (weak) Unix Crypt algorithm.
   *
   * This method will return an error code if this password field is
   * not configured to store Crypt hashed password text.
   */

  ReturnVal setCryptPass(String text) throws RemoteException;

  /**
   * This method is used to set a pre-crypted FreeBSD-style MD5Crypt
   * password for this field.
   *
   * This method will return an error code if this password field is
   * not configured to store MD5Crypt hashed password text.
   */

  ReturnVal setMD5CryptedPass(String text) throws RemoteException;

  /**
   * This method is used to set a pre-crypted Apache-style MD5Crypt
   * password for this field.
   *
   * This method will return an error code if this password field is
   * not configured to store Apache-style MD5Crypt hashed password
   * text.
   */

  ReturnVal setApacheMD5CryptedPass(String text) throws RemoteException;

  /**
   * This method is used to set pre-crypted Windows-style password
   * hashes for this field.  These strings are formatted as used in
   * Samba's encrypted password files.
   *
   * This method will return an error code if this password field is
   * not configured to accept Windows-hashed password strings.
   */

  ReturnVal setWinCryptedPass(String LANMAN, String NTUnicodeMD4) throws RemoteException;

  /**
   * This method is used to set a pre-crypted OpenLDAP/Netscape
   * Directory Server Salted SHA (SSHA) password for this field.
   *
   * This method will return an error code if this password field is
   * not configured to store SSHA hashed password text.
   */

  ReturnVal setSSHAPass(String text) throws RemoteException;

  /**
   * This method is used to set a pre-crypted Sha256Crypt or
   * Sha512Crypt password for this field.
   *
   * This method will return an error code if this password field is
   * not configured to store ShaCrypt hashed password text.
   *
   * The hashText submitted to this method must match one of the
   * following four forms:
   *
   * $5$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   * $5$rounds=&lt;round-count&gt;$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   *
   * $6$&lt;saltstring&gt;$&lt;64 bytes of hash text, base 64 encoded&gt;
   * $6$rounds=&lt;round-count&gt;$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   *
   * If the round count is specified using the '$rounds=n' syntax, the
   * higher the round count, the more computational work will be
   * required to verify passwords against this hash text.
   *
   * See http://people.redhat.com/drepper/sha-crypt.html for full
   * details of the hash format this method is expecting.
   */

  ReturnVal setShaUnixCryptPass(String hashText) throws RemoteException;
}
