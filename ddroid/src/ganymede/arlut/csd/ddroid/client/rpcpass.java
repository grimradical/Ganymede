/*
   GASH 2

   rpcpass.java

   Command line application to update user account password, shell, and/or
   gecos information based on interaction with the rpc.yppasswdd daemon
   from the Linux NIS kit.

   Created: 6 May 1999
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

package arlut.csd.ddroid.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.Properties;

import arlut.csd.Util.ParseArgs;
import arlut.csd.ddroid.common.ReturnVal;
import arlut.csd.ddroid.rmi.Session;
import arlut.csd.ddroid.rmi.db_object;
import arlut.csd.ddroid.rmi.pass_field;
import arlut.csd.ddroid.rmi.string_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         rpcpass

------------------------------------------------------------------------------*/

/**
 * <p>Command line application to update user account password, shell, and/or
 * gecos information based on interaction with the rpc.yppasswdd daemon
 * from the Linux NIS kit, versions 1.3.6.92 and above, using the -x option.</p>
 *
 * <p>This client uses the {@link arlut.csd.ddroid.client.ClientBase ClientBase}
 * client stub for communications with the server.</p>
 *
 * <p>This code should be considered quasi-deprecatd, because the NIS
 * passwd daemon can't pass plaintext to Directory Droid, making it useless
 * for updating Samba, md5 or NT password hashes.</p> 
 */

public class rpcpass implements ClientListener {

  public static String serverHostProperty = null;
  public static String rootname = null;
  public static int    registryPortProperty = 1099;
  public static String username = null;
  public static String oldpass = null;
  public static String cryptedpass = null;
  public static String shell = null;
  public static String gecos = null;

  /**
   * RMI object to handle getting us logged into the server, and to handle
   * asynchronous callbacks from the server on our behalf.
   */

  private static ClientBase my_client;

  // ---

  public static void main(String argv[])
  {
    String propFilename = null;
    String server_url;
    String input = null;
    Session session = null;
    db_object userObj;
    ReturnVal attempt = null;

    /* -- */

    propFilename = ParseArgs.getArg("properties", argv);

    if (propFilename == null)
      {
	System.out.println("Directory Droid rpcpass: Error, invalid command line parameters");
 	System.out.println("Usage: java rpcpass properties=<property file>");
	System.exit(1);
      }

    try
      {
	BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
	input = inputReader.readLine();
	inputReader.close();
      }
    catch (IOException ex)
      {
	System.out.println("Directory Droid rcpass: Could not read input from rpc.yppasswdd: " + ex.getMessage());
      }

    input = input.trim();

    if (input.length() != 0)
      {
	StringBuffer namebuf = new StringBuffer();
	int pos = 0;

	while (pos < input.length() && !Character.isWhitespace(input.charAt(pos)))
	  {
	    namebuf.append(input.charAt(pos++));
	  }

	username = namebuf.toString();

	pos = input.indexOf("o:");

	if (pos != -1)
	  {
	    pos += 2;
	   
	    namebuf = new StringBuffer();

	    while (pos < input.length() && !Character.isWhitespace(input.charAt(pos)))
	      {
		namebuf.append(input.charAt(pos++));
	      }
 
	    oldpass = namebuf.toString();
	  }

	pos = input.indexOf("p:");

	if (pos != -1)
	  {
	    pos += 2;
	   
	    namebuf = new StringBuffer();

	    while (pos < input.length() && !Character.isWhitespace(input.charAt(pos)))
	      {
		namebuf.append(input.charAt(pos++));
	      }
 
	    cryptedpass = namebuf.toString();
	  }

	pos = input.indexOf("s:");

	if (pos != -1)
	  {
	    pos += 2;
	   
	    namebuf = new StringBuffer();

	    while (pos < input.length() && !Character.isWhitespace(input.charAt(pos)))
	      {
		namebuf.append(input.charAt(pos++));
	      }
 
	    shell = namebuf.toString();
	  }

	pos = input.indexOf("g:");

	if (pos != -1)
	  {
	    pos += 2;
	   
	    namebuf = new StringBuffer();

	    while (pos < input.length() && !Character.isWhitespace(input.charAt(pos)))
	      {
		namebuf.append(input.charAt(pos++));
	      }
 
	    gecos = namebuf.toString();
	  }
      }

    if (username == null || oldpass == null || 
	(cryptedpass == null && shell == null && gecos == null))
      {
	System.out.println("Directory Droid rpcpass: Error, information missing.");
	System.exit(1);
      }

    if (!loadProperties(propFilename))
      {
	System.out.println("Directory Droid rpcpass: Error, couldn't successfully load properties from file " + 
			   propFilename + ".");
	System.exit(1);
      }

    server_url = "rmi://" + serverHostProperty + ":" + registryPortProperty + "/ganymede.server";

    // after the ClientBase is constructed, we'll be an active RMI
    // server, so we need to always do System.exit() to shut down the
    // VM.

    try
      {
	my_client = new ClientBase(server_url, new rpcpass());
      }
    catch (RemoteException ex)
      {
	System.out.println("Could not connect to server" + ex.getMessage());
	System.exit(1);
      }

    try
      {
	session = my_client.login(username, oldpass);
      }
    catch (RemoteException ex)
      {
	System.out.println("Directory Droid rpcpass: couldn't log in for username " + username);
	System.exit(1);
      }

    try
      {
	try
	  {
	    attempt = session.openTransaction("rpcpass client (" + username + ")");

	    if (attempt != null && !attempt.didSucceed())
	      {
		if (attempt.getDialog() != null)
		  {
		    System.out.println("Directory Droid rpcpass: couldn't open transaction " + username +
				       ": " + attempt.getDialog().getText());
		  }
		else
		  {
		    System.out.println("Directory Droid rpcpass: couldn't open transaction " + username);
		  }

		return;
	      }
	  }
	catch (RemoteException ex)
	  {
	    System.out.println("Directory Droid rpcpass: couldn't open transaction " + 
			       username + ": " + ex.getMessage());
	    return;
	  }

	try
	  {
	    attempt = session.edit_db_object(session.findLabeledObject(username, (short) 3));

	    if (attempt.didSucceed())
	      {
		userObj = (db_object) attempt.getObject();
	      }
	    else
	      {
		if (attempt.getDialog() != null)
		  {
		    System.out.println("Directory Droid rpcpass: couldn't edit user " + username +
				       ": " + attempt.getDialog().getText());
		    return;
		  }
		else
		  {
		    System.out.println("Directory Droid rpcpass: couldn't edit user " + username);
		    return;
		  }
	      }

	    if (cryptedpass != null)
	      {
		pass_field field = (pass_field) userObj.getField((short) 101);

		attempt = field.setCryptPass(cryptedpass);

		if (attempt != null && !attempt.didSucceed())
		  {
		    if (attempt.getDialog() != null)
		      {
			System.out.println("Directory Droid rpcpass: couldn't set password for user " + username +
					   ": " + attempt.getDialog().getText());
			return;
		      }
		    else
		      {
			System.out.println("Directory Droid rpcpass: couldn't set password for user " + username);
			return;
		      }
		  }
	      }

	    if (shell != null)
	      {
		string_field field = (string_field) userObj.getField((short) 263);

		attempt = field.setValue(shell);

		if (attempt != null && !attempt.didSucceed())
		  {
		    if (attempt.getDialog() != null)
		      {
			System.out.println("Directory Droid rpcpass: couldn't set shell for user " + username +
					   ": " + attempt.getDialog().getText());
			return;
		      }
		    else
		      {
			System.out.println("Directory Droid rpcpass: couldn't set shell for user " + username);
			return;
		      }
		  }
	      }

	    attempt = session.commitTransaction(true);

	    if (attempt != null && !attempt.didSucceed())
	      {
		if (attempt.getDialog() != null)
		  {
		    System.out.println("Directory Droid rpcpass: couldn't commit transaction " + username +
				       ": " + attempt.getDialog().getText());
		    return;
		  }
		else
		  {
		    System.out.println("Directory Droid rpcpass: couldn't commit transaction " + username);
		    return;
		  }
	      }

	    System.out.println("OK");
	    
	  }
	catch (RemoteException ex)
	  {
	    System.out.println("Ganymede.rpcpass: remote exception " + ex.getMessage());
	  }
      }
    finally
      {
	try
	  {
	    my_client.disconnect();
	  }
	catch (RemoteException ex)
	  {
	  }
	finally
	  {
	    System.exit(0);
	  }
      }
  }

  /**
   * <p>This method loads properties from the ganymede.properties
   * file.</p>
   *
   * <p>This method is public so that loader code linked with the
   * Directory Droid server code can initialize the properties without
   * going through Ganymede.main().</p>
   */

  public static boolean loadProperties(String filename)
  {
    Properties props = new Properties(System.getProperties());
    boolean success = true;

    /* -- */

    try
      {
	props.load(new BufferedInputStream(new FileInputStream(filename)));
      }
    catch (IOException ex)
      {
	return false;
      }

    // make the combined properties file accessible throughout our server
    // code.

    System.setProperties(props);

    serverHostProperty = System.getProperty("ganymede.serverhost");
    rootname = System.getProperty("ganymede.rootname");

    if (serverHostProperty == null)
      {
	System.err.println("Couldn't get the server host property");
	success = false;
      }

    if (rootname == null)
      {
	System.err.println("Couldn't get the root name property");
	success = false;
      }

    // get the registry port number

    String registryPort = System.getProperty("ganymede.registryPort");

    if (registryPort != null)
      {
	try
	  {
	    registryPortProperty = java.lang.Integer.parseInt(registryPort);
	  }
	catch (NumberFormatException ex)
	  {
	    System.err.println("Couldn't get a valid registry port number from ganymede properties file: " + 
			       registryPort);
	  }
      }

    return success;
  }

  /**
   * <p>Called when the server forces a disconnect.</p>
   *
   * <p>Call getMessage() on the
   * {@link arlut.csd.ddroid.client.ClientEvent ClientEvent} 
   * to get the reason for the disconnect.</p>
   */

  public void disconnected(ClientEvent e)
  {
  }

  /**
   * <p>Called when the ClientBase needs to report something
   * to the client.  The client is expected to then put
   * up a dialog or do whatever else is appropriate.</p>
   *
   * <p>Call getMessage() on the
   * {@link arlut.csd.ddroid.client.ClientEvent ClientEvent} 
   * to get the reason for the disconnect.</p>
   */

  public void messageReceived(ClientEvent e)
  {
  }
}

