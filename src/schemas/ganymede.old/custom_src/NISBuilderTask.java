/*

   NISBuilderTask.java

   This class is intended to dump the Ganymede datastore to NIS.
   
   Created: 18 February 1998
   Version: $Revision: 1.8 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  NISBuilderTask

------------------------------------------------------------------------------*/

/**
 *
 * This class is intended to dump the Ganymede datastore to NIS.
 *
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 */

public class NISBuilderTask extends GanymedeBuilderTask {

  private static String path = null;

  // ---

  private Date now = null;

  /* -- */

  /**
   *
   * This method runs with a dumpLock obtained for the builder task.
   *
   * Code run in builderPhase1() can call enumerateObjects() and
   * baseChanged().
   *
   * @return true if builderPhase1 made changes necessitating the
   * execution of builderPhase2.
   *
   */

  public boolean builderPhase1()
  {
    PrintWriter out;
    boolean result = false;

    /* -- */

    if (path == null)
      {
	path = System.getProperty("ganymede.nis.output");

	if (path == null)
	  {
	    throw new RuntimeException("NISBuilder not able to determine output directory.");
	  }

	if (!path.endsWith("/"))
	  {
	    path = path + "/";
	  }
      }

    now = null;

    // passwd

    if (baseChanged(SchemaConstants.UserBase))
      {
	Ganymede.debug("Need to build user map");

	out = null;

	try
	  {
	    out = openOutFile(path + "passwd");
	  }
	catch (IOException ex)
	  {
	    System.err.println("NISBuilderTask.builderPhase1(): couldn't open passwd file: " + ex);
	  }
	
	if (out != null)
	  {
	    DBObject user;
	    Enumeration users = enumerateObjects(SchemaConstants.UserBase);

	    while (users.hasMoreElements())
	      {
		user = (DBObject) users.nextElement();

		writeUserLine(user, out);
	      }

	    out.close();
	  }

	result = true;
      }

    // group

    if (baseChanged((short) 257))
      {
	Ganymede.debug("Need to build group map");

	out = null;

	try
	  {
	    out = openOutFile(path + "group");
	  }
	catch (IOException ex)
	  {
	    System.err.println("NISBuilderTask.builderPhase1(): couldn't open group file: " + ex);
	  }
	
	if (out != null)
	  {
	    DBObject group;
	    Enumeration groups = enumerateObjects((short) 257);

	    while (groups.hasMoreElements())
	      {
		group = (DBObject) groups.nextElement();

		writeGroupLine(group, out);
	      }

	    out.close();
	  }

	result = true;
      }

    if (baseChanged((short) 274) ||
	baseChanged((short) 275))
      {
	Ganymede.debug("Need to build aliases map");
	result = true;
      }

    if (baseChanged((short) 271) ||
	baseChanged((short) 270))
      {
	Ganymede.debug("Need to build netgroup map");

	if (writeNetgroupFile())
	  {
	    result = true;
	  }
      }

    if (baseChanged((short) 277) ||
	baseChanged((short) 276) ||
	baseChanged((short) 278))
      {
	Ganymede.debug("Need to build automounter maps");

	if (writeAutoMounterFiles())
	  {
	    result = true;
	  }
      }

    return result;
  }

  /**
   *
   * This method runs after this task's dumpLock has been
   * relinquished.  This method is intended to be used to finish off a
   * build process by running (probably external) code that does not
   * require direct access to the database.
   *
   * builderPhase2 is only run if builderPhase1 returns true.
   *
   */

  public boolean builderPhase2()
  {
    Ganymede.debug("Need to run external NIS build script");
    return true;
  }

  // ***
  //
  // The following private methods are used to support the NIS builder logic.
  //
  // ***

  /**
   *
   * This method opens the specified file for writing out a text stream.
   *
   * If there already exists a file by that name in the system, the old
   * version will be moved to a backup before the file is recreated for
   * writing.
   *
   */

  private PrintWriter openOutFile(String filename) throws IOException
  {
    File
      file,
      oldFile;

    /* -- */

    file = new File(filename);

    // back up the file if it exists.

    if (file.exists())
      {
	if (lastRunTime != null)
	  {
	    oldFile = new File(filename + lastRunTime.toString());
	  }
	else
	  {
	    if (now == null)
	      {
		now = new Date();
	      }

	    oldFile = new File(filename + now.toString());
	  }

	file.renameTo(oldFile);
      }

    return new PrintWriter(new BufferedWriter(new FileWriter(file)));
  }

  /**
   *
   * This method writes out a line to the passwd NIS source file.
   *
   * The lines in this file look like the following.
   *
   * broccol:393T6k3e/9/w2:12003:12010:Jonathan Abbey,S321 CSD,3199,8343915:/home/broccol:/bin/tcsh
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this user line
   *
   */

  private void writeUserLine(DBObject object, PrintWriter writer)
  {
    String username;
    String cryptedPass;
    int uid;
    int gid;
    String name;
    String room;
    String div;
    String officePhone;
    String homePhone;
    String directory;
    String shell;

    PasswordDBField passField;
    Vector invids;
    Invid groupInvid;
    DBObject group;

    StringBuffer result = new StringBuffer();

    /* -- */

    username = (String) object.getFieldValueLocal(SchemaConstants.UserUserName);

    passField = (PasswordDBField) object.getField(SchemaConstants.UserPassword);

    if (passField != null)
      {
	cryptedPass = passField.getUNIXCryptText();
      }
    else
      {
	System.err.println("NISBuilder.writeUserLine(): null password for user " + username);
	cryptedPass = "**Nopass**";
      }

    uid = ((Integer) object.getFieldValueLocal((short) 256)).intValue();

    // get the gid
    
    groupInvid = (Invid) object.getFieldValueLocal((short) 265); // home group

    if (groupInvid == null)
      {
	System.err.println("NISBuilder.writeUserLine(): null gid for user " + username);
	gid = -1;
      }
    else
      {
	group = getObject(groupInvid);
	gid = ((Integer) group.getFieldValueLocal((short) 258)).intValue();
      }

    name = (String) object.getFieldValueLocal((short) 257);
    room = (String) object.getFieldValueLocal((short) 259);
    div = (String) object.getFieldValueLocal((short) 258);
    officePhone = (String) object.getFieldValueLocal((short) 260);
    homePhone = (String) object.getFieldValueLocal((short) 261);
    directory = (String) object.getFieldValueLocal((short) 262);
    shell = (String) object.getFieldValueLocal((short) 263);

    // now build our output line

    result.append(username);
    result.append(":");
    result.append(cryptedPass);
    result.append(":");
    result.append(Integer.toString(uid));
    result.append(":");
    result.append(Integer.toString(gid));
    result.append(":");
    result.append(name);
    result.append(",");
    result.append(room);
    result.append(" ");
    result.append(div);
    result.append(",");
    result.append(officePhone);

    if (homePhone != null && !homePhone.equals(""))
      {
	result.append(",");
	result.append(homePhone);
      }

    result.append(":");
    result.append(directory);
    result.append(":");
    result.append(shell);

    if (result.length() > 1024)
      {
	System.err.println("NISBuilder.writeGroupLine(): Warning!  user " + 
			   username + " overflows the NIS line length!");
      }

    writer.println(result.toString());
  }

  /**
   *
   * This method writes out a line to the passwd NIS source file.
   *
   * The lines in this file look like the following.
   *
   * adgacc:ZzZz:4015:hammp,jgeorge,dd,doodle,dhoss,corbett,monk
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this user line
   *
   */

  private void writeGroupLine(DBObject object, PrintWriter writer)
  {
    String groupname;
    String pass;
    int gid;
    Vector users = new Vector();

    PasswordDBField passField;
    Vector invids;
    Invid userInvid;
    String userName;

    StringBuffer result = new StringBuffer();

    /* -- */

    groupname = (String) object.getFieldValueLocal((short) 256);

    // currently in the Ganymede schema, group passwords aren't in passfields.

    pass = (String) object.getFieldValueLocal((short) 257);
    gid = ((Integer) object.getFieldValueLocal((short) 258)).intValue();
    
    // we currently don't explicitly record the home group.. just take the first group
    // that the user is in.

    invids = object.getFieldValuesLocal((short) 261);

    if (invids == null)
      {
	// System.err.println("NISBuilder.writeUserLine(): null user list for group " + groupname);
      }
    else
      {
	for (int i = 0; i < invids.size(); i++)
	  {
	    userInvid = (Invid) invids.elementAt(i);
	    
	    userName = getLabel(userInvid);

	    if (userName != null)
	      {
		users.addElement(userName);
	      }
	  }
      }

    // now build our output line

    result.append(groupname);
    result.append(":");
    result.append(pass);
    result.append(":");
    result.append(Integer.toString(gid));
    result.append(":");

    for (int i = 0; i < users.size(); i++)
      {
	if (i > 0)
	  {
	    result.append(",");
	  }

	result.append((String) users.elementAt(i));
      }

    if (result.length() > 1024)
      {
	System.err.println("NISBuilder.writeGroupLine(): Warning!  group " + 
			   groupname + " overflows the NIS line length!");
      }

    writer.println(result.toString());
  }

  /**
   *
   * This method generates a Netgroup file.
   *
   */

  private boolean writeNetgroupFile()
  {
    PrintWriter netgroupFile = null;
    DBObject netgroup;
    Enumeration netgroups;

    /* -- */

    try
      {
	netgroupFile = openOutFile(path + "netgroup");
      }
    catch (IOException ex)
      {
	System.err.println("NISBuilderTask.writeNetgroup(): couldn't open netgroup file: " + ex);
      }

    // first the user netgroups

    netgroups = enumerateObjects((short) 270);

    while (netgroups.hasMoreElements())
      {
	netgroup = (DBObject) netgroups.nextElement();
	
	writeUserNetgroup(netgroup, netgroupFile);
      }

    // now the system netgroups
    
    netgroups = enumerateObjects((short) 271);

    while (netgroups.hasMoreElements())
      {
	netgroup = (DBObject) netgroups.nextElement();
	
	writeSystemNetgroup(netgroup, netgroupFile);
      }

    netgroupFile.close();

    return true;
  }

  /**
   *
   * This method writes out a single user netgroup out to disk,
   * wrapping the netgroup if it gets too long.
   *
   * omg-u	csd-u (-,broccol,) (-,gomod,) (-,etcrh,)
   *
   */

  private void writeUserNetgroup(DBObject object, PrintWriter writer)
  {
    StringBuffer buffer = new StringBuffer();

    String name;
    Vector users;
    Vector memberNetgroups;

    Invid ref;
    String refLabel;

    int lengthlimit;
    int subgroup = 2;
    String subname;

    /* -- */

    name = (String) object.getFieldValueLocal((short) 256);
    users = object.getFieldValuesLocal((short) 257);
    memberNetgroups = object.getFieldValuesLocal((short) 258);

    // NIS limits the length of a line to 1024 characters.
    // If the line looks like it'll go over, we'll truncate
    // it, put in an entry to link the netgroup with a
    // sub netgroup for continuation.

    // Thus, we want to save enough space to be able to put
    // the link information at the end.  We reduce it by
    // a further 6 chars to leave space for the per-entry
    // syntax.

    lengthlimit = 1024 - name.length() - 6;

    buffer.append(name);

    if (memberNetgroups != null)
      {
	for (int i = 0; i < memberNetgroups.size(); i++)
	  {
	    ref = (Invid) memberNetgroups.elementAt(i);
	    refLabel = getLabel(ref);
	    
	    if (buffer.length() + refLabel.length() > lengthlimit)
	      {
		subname = name + subgroup;
		
		buffer.append(" ");
		buffer.append(subname);
		buffer.append("\n");
		buffer.append(subname);
		subgroup++;
	      }
	    
	    buffer.append(" ");
	    buffer.append(refLabel);
	  }
      }

    if (users != null)
      {
	for (int i = 0; i < users.size(); i++)
	  {
	    ref = (Invid) users.elementAt(i);
	    refLabel = getLabel(ref);

	    if (buffer.length() + refLabel.length() > lengthlimit)
	      {
		subname = name + subgroup;

		buffer.append(" ");
		buffer.append(subname);
		buffer.append("\n");
		buffer.append(subname);
		subgroup++;
	      }

	    buffer.append(" ");
	    buffer.append("(-,");
	    buffer.append(refLabel);
	    buffer.append(",)");
	  }
      }

    writer.println(buffer.toString());
  }

  /**
   *
   * This method writes out a single user netgroup out to disk,
   * wrapping the netgroup if it gets too long.
   *
   * omg-s	csd-s (csdsun1.arlut.utexas.edu,-,) (ns1.arlut.utexas.edu,-,)
   *
   */

  private void writeSystemNetgroup(DBObject object, PrintWriter writer)
  {
    StringBuffer buffer = new StringBuffer();

    String name;
    Vector systems;
    Vector memberNetgroups;

    Invid ref;
    String refLabel;

    int lengthlimit;
    int subgroup = 2;
    String subname;

    /* -- */

    name = (String) object.getFieldValueLocal((short) 256);
    systems = object.getFieldValuesLocal((short) 257);
    memberNetgroups = object.getFieldValuesLocal((short) 258);

    // NIS limits the length of a line to 1024 characters.
    // If the line looks like it'll go over, we'll truncate
    // it, put in an entry to link the netgroup with a
    // sub netgroup for continuation.

    // Thus, we want to save enough space to be able to put
    // the link information at the end.  We reduce it by
    // a further 6 chars to leave space for the per-entry
    // syntax.

    lengthlimit = 1024 - name.length() - 6;

    buffer.append(name);

    if (memberNetgroups != null)
      {
	for (int i = 0; i < memberNetgroups.size(); i++)
	  {
	    ref = (Invid) memberNetgroups.elementAt(i);
	    refLabel = getLabel(ref);

	    if (buffer.length() + refLabel.length() > lengthlimit)
	      {
		subname = name + subgroup;

		buffer.append(" ");
		buffer.append(subname);
		buffer.append("\n");
		buffer.append(subname);
		subgroup++;
	      }

	    buffer.append(" ");
	    buffer.append(refLabel);
	  }
      }

    if (systems != null)
      {
	for (int i = 0; i < systems.size(); i++)
	  {
	    ref = (Invid) systems.elementAt(i);
	    refLabel = getLabel(ref);

	    if (buffer.length() + refLabel.length() > lengthlimit)
	      {
		subname = name + subgroup;

		buffer.append(" ");
		buffer.append(subname);
		buffer.append("\n");
		buffer.append(subname);
		subgroup++;
	      }

	    buffer.append(" ");
	    buffer.append("(");
	    buffer.append(refLabel);
	    buffer.append(",-,)");
	  }
      }

    writer.println(buffer.toString());
  }

  /**
   *
   * This method generates an auto.vol file, along with auto.home.*
   * files for all automounter records in the Ganymede database.
   *
   */

  private boolean writeAutoMounterFiles()
  {
    PrintWriter autoFile = null;
    DBObject map, obj, user;
    Enumeration vols, maps, entries;
    StringBuffer buf = new StringBuffer();
    String mountopts, mapname;
    Vector tempVect;
    Invid ref, userRef;

    /* -- */

    // first, write out the auto.vol file

    try
      {
	autoFile = openOutFile(path + "auto.vol");
      }
    catch (IOException ex)
      {
	System.err.println("NISBuilderTask.writeAutoMounterFiles(): couldn't open auto.vol: " + ex);
      }

    // find the volume definitions

    vols = enumerateObjects((short) 276);

    while (vols.hasMoreElements())
      {
	obj = (DBObject) vols.nextElement();

	buf.setLength(0);
	buf.append((String) obj.getFieldValueLocal((short) 256)); // volume label
	buf.append("\t\t");

	mountopts = (String) obj.getFieldValueLocal((short) 260); // mount options.. NeXT's like this.  Ugh.

	if (mountopts != null && !mountopts.equals(""))
	  {
	    buf.append(mountopts);
	    buf.append(" ");
	  }

	buf.append(getLabel((Invid) obj.getFieldValueLocal((short) 257))); // hostname
	buf.append(":");
	buf.append((String) obj.getFieldValueLocal((short) 258)); // mount path

	autoFile.println(buf.toString());
      }

    autoFile.close();

    // second, write out all the auto.home.* files mapping user name
    // to volume name.  We depend on the GASH build scripts to convert
    // these to the form that NIS will actually use.. we could and possibly
    // will change this to write out the combined auto.home/auto.vol info
    // rather than forcing it to be done after-the-fact via perl.

    maps = enumerateObjects((short) 277);

    while (maps.hasMoreElements())
      {
	map = (DBObject) maps.nextElement();

	mapname = (String) map.getFieldValueLocal((short) 256);

	try
	  {
	    autoFile = openOutFile(path + mapname);
	  }
	catch (IOException ex)
	  {
	    System.err.println("NISBuilderTask.writeAutoMounterFiles(): couldn't open " + mapname + ": " + ex);
	  }

	tempVect = map.getFieldValuesLocal((short) 257);

	if (tempVect == null)
	  {
	    autoFile.close();
	    continue;
	  }

	entries = tempVect.elements();

	while (entries.hasMoreElements())
	  {
	    ref = (Invid) entries.nextElement();
	    obj = getObject(ref);

	    // the entry is embedded in the user's record.. get the user' id and label

	    userRef = (Invid) obj.getFieldValueLocal((short) 0);

	    if (userRef.getType() != SchemaConstants.UserBase)
	      {
		throw new RuntimeException("Schema and/or database error");
	      }

	    buf.setLength(0);
	    
	    buf.append(getLabel(userRef)); // the user's name
	    buf.append("\t");

	    ref = (Invid) obj.getFieldValueLocal((short) 257); // nfs volume for this entry

	    if (ref == null || ref.getType() != (short) 276)
	      {
		throw new RuntimeException("Schema and/or database error");
	      }

	    buf.append(getLabel(ref));

	    autoFile.println(buf.toString());
	  }

	autoFile.close();
      }

    return true;
  }
}
