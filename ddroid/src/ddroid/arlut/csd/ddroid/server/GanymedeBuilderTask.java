/*

   GanymedeBuilderTask.java

   This class provides a template for code to be attached to the server to
   handle propagating data from the Directory Droid object store into the wide
   world, via NIS, DNS, NIS+, LDAP, JNDI, JDBC, X, Y, Z, etc.
   
   Created: 17 February 1998
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;

import arlut.csd.Util.FileOps;
import arlut.csd.Util.PathComplete;
import arlut.csd.Util.zipIt;
import arlut.csd.ddroid.common.Invid;
import arlut.csd.ddroid.common.SchemaConstants;

/*------------------------------------------------------------------------------
                                                                           class
                                                             GanymedeBuilderTask

------------------------------------------------------------------------------*/

/**
 * <P>This class provides a template for code to be attached to the server to
 * handle propagating data from the Directory Droid object store into the wide
 * world, via NIS, DNS, NIS+, LDAP, JNDI, JDBC, X, Y, Z, etc.</P>
 *
 * <P>Subclasses of GanymedeBuilderTask need to implement builderPhase1()
 * and builderPhase2().  builderPhase1() is run while a dumpLock is established
 * on {@link arlut.csd.ddroid.server.DBStore DBStore}, guaranteeing a 
 * transaction-consistent database state.  builderPhase1() should do whatever
 * is required to write out files or otherwise propagate data out from the
 * database.  If builderPhase1() returns true, the dump lock is released and
 * builderPhase2() is run.  This method is intended to run external scripts 
 * and/or code that can process the files/data written out by builderPhase1()
 * without needing the database to remain locked.</P>
 *
 * <P>All subclasses of GanymedeBuilderTask need to be registered in the Ganymede
 * database via the task object type.  GanymedeBuilderTasks registered to be
 * run on database commit will automatically be issued by the
 * {@link arlut.csd.ddroid.server.GanymedeScheduler GanymedeScheduler} when transactions
 * commit.  The GanymedeScheduler is designed so that it will not re-issue a specific
 * task while a previous instance of the task is still running, so you don't have
 * to worry about builderPhase2() taking a fair amount of time.  builderPhase1() should
 * be as fast as possible, however, as no additional transactions will be able to
 * be committed until builderPhase1() completes.</P>
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public abstract class GanymedeBuilderTask implements Runnable {

  private static final boolean debug = false;

  private static String currentBackUpDirectory = null;
  private static String oldBackUpDirectory = null;

  /**
   * <p>This hashtable maps directory paths to an Integer
   * counting the number of tasks that are currently
   * copying backup files to it.  If the current day's
   * directory path changes and this count goes to
   * zero, the old directory will be zipped up and
   * deleted.</p>
   */

  private static Hashtable backupsBusy = new Hashtable();
  private static String basePath = null;
  private static long rollunderTime = 0;
  private static long rolloverTime = 0;
  private static boolean firstRun = true;
  private static int id = 0;

  /**
   * <P>Count of the number of builder tasks currently
   * running in phase 1.</P>
   */

  private static int phase1Count = 0;

  /**
   * <P>Count of the number of builder tasks currently
   * running in phase 1.</P>
   */

  private static int phase2Count = 0;

  /* --- */

  protected Date lastRunTime;
  protected Date oldLastRunTime;
  GanymedeSession session = null;
  DBDumpLock lock;
  Vector optionsCache = null;

  /**
   * <p>If this flag is true, baseChanged() will always return true,
   * as a way of forcing consideration of all databases that might
   * be examined by GanymedeBuilderTask subclasses.</p>
   */

  private boolean forceAllBases;

  /**
   * <p>Must be protected so subclasses in a different package can
   * set this.</p>
   */

  protected Invid taskDefObjInvid = null;

  /* -- */

  /**
   * <P>This method is the main entry point for the GanymedeBuilderTask.  It
   * is responsible for setting up the environment for a builder task to
   * operate under, and for actually invoking the builder method.</P>
   */

  public final void run()
  {
    this.run(null);
  }

  /**
   * <P>This method is the main entry point for the GanymedeBuilderTask.  It
   * is responsible for setting up the environment for a builder task to
   * operate under, and for actually invoking the builder method.</P>
   */
    
  public final void run(Object options[])
  {
    String label = null;
    Thread currentThread = java.lang.Thread.currentThread();
    boolean
      success1 = false;
    boolean alreadyDecdCount = false;

    /* -- */

    String shutdownState = GanymedeServer.shutdownSemaphore.checkEnabled();

    if (shutdownState != null)
      {
	Ganymede.debug("Aborting builder task " + this.getClass().getName() + 
		       " for shutdown condition: " + shutdownState);
	return;
      }

    if (options == null)
      {
	this.forceAllBases = false;
      }
    else
      {
	for (int i = 0; i < options.length; i++)
	  {
	    if (options[i] instanceof String)
	      {
		String x = (String) options[i];

		if (x.equals("forcebuild"))
		  {
		    this.forceAllBases = true;
		  }
	      }
	  }
      }

    try
      {
	// the scheduler should make sure we are never in progress
	// more than once concurrently, but it won't hurt to clear the
	// optionsCache up front just in case

	optionsCache = null;

	incPhase1(true);

	try
	  {
	    // we need a unique label for our session so that multiple
	    // builder tasks can have their own lock keys.. our label will
	    // start at builder:0 and work our way up as we go along
	    // during the server's lifetime

	    synchronized (GanymedeBuilderTask.class) 
	      {
		label = "builder:" + id++;
	      }

	    try
	      {
		session = new GanymedeSession(label);
	      }
	    catch (java.rmi.RemoteException ex)
	      {
		throw new RuntimeException("bizarro local remote exception " + ex);
	      }

	    try
	      {
		lock = session.getSession().openDumpLock();
	      }
	    catch (InterruptedException ex)
	      {
		Ganymede.debug("Could not run task " + this.getClass().getName() + ", couldn't get dump lock");
		return;
	      }

	    // update our time as soon as possible, so that any changes
	    // that are made in the database after we release the dump
	    // lock will have a time stamp after our 'last build' time
	    // stamp.

	    if (lastRunTime == null)
	      {
		lastRunTime = new Date();
	      }
	    else
	      {
		if (oldLastRunTime == null)
		  {
		    oldLastRunTime = new Date(lastRunTime.getTime());
		  }
		else
		  {
		    oldLastRunTime.setTime(lastRunTime.getTime());
		  }

		lastRunTime.setTime(System.currentTimeMillis());
	      }

	    success1 = this.builderPhase1();
	  }
	catch (Exception ex)
	  {
	    decPhase1(true);
	    alreadyDecdCount = true;

	    ex.printStackTrace();
	    return;
	  }
	finally
	  {
	    if (!alreadyDecdCount)
	      {
		decPhase1(false); // false since we don't want to force stat update yet
	      }

	    // release the lock, and so on

	    if (session != null)
	      {
		session.logout();	// will clear the dump lock
		session = null;
		lock = null;
	      }
	  }
	
	if (currentThread.isInterrupted())
	  {
	    Ganymede.debug("Builder task " + this.getClass().getName() + 
			   " interrupted, not doing network build.");
	    Ganymede.updateBuildStatus();
	    return;
	  }

	try
	  {
	    incPhase2(true);

	    if (success1)
	      {
		try
		  {
		    shutdownState = GanymedeServer.shutdownSemaphore.increment(0);

		    if (shutdownState != null)
		      {
			Ganymede.debug("Aborting builder task " + this.getClass().getName() + 
				       " build for shutdown condition: " + shutdownState);
			return;
		      }
		  }
		catch (InterruptedException ex)
		  {
		    // will never happen, since we are giving 0 to
		    // increment
		  }
		
		try
		  {
		    this.builderPhase2();
		  }
		finally
		  {
		    GanymedeServer.shutdownSemaphore.decrement();
		  }
	      }
	  }
	finally
	  {
	    decPhase2(true);
	  }
      }
    finally
      {
	// we need the finally in case our thread is stopped

	if (session != null)
	  {
	    try
	      {
		session.logout();	// this will clear the dump lock if need be.
	      }
	    finally
	      {
		session = null;
		lock = null;
	      }
	  }
	
	// and again, just in case
	
	optionsCache = null;
      }
  }

  /**
   * <P>This method is used by subclasses of GanymedeBuilderTask to
   * determine whether a particular base has had any modifications
   * made to it since the last time this builder task was run.</P>
   *
   * @param baseid The id number of the base to be checked
   */

  protected final boolean baseChanged(short baseid)
  {
    if (forceAllBases || (oldLastRunTime == null))
      {
	return true;
      }
    else
      {
	DBObjectBase base = Ganymede.db.getObjectBase(baseid);
	
	if (base.lastChange == null)
	  {
	    return false;
	  }
	else 
	  {
	    return base.lastChange.after(oldLastRunTime);
	  }
      }
  }

  /**
   * <P>This method is used by subclasses of GanymedeBuilderTask to
   * obtain a list of DBObject references of the requested
   * type.</P>
   *
   * <P>Note that the Enumeration returned by this method MUST NOT
   * be used after builderPhase1() returns.  This Enumeration is
   * only valid while the base in question is locked with the
   * global dumpLock obtained before builderPhase1() is run and
   * which is released after builderPhase1() returns.</P>
   *
   * @param baseid The id number of the base to be listed
   *
   * @return An Enumeration of {@link arlut.csd.ddroid.server.DBObject DBObject} references
   */

  protected final Enumeration enumerateObjects(short baseid)
  {
    // this works only because we've already got our lock
    // established..  otherwise, we'd have to use the query system.

    if (lock == null)
      {
	throw new IllegalArgumentException("Can't call enumerateObjects without a lock");
      }

    DBObjectBase base = Ganymede.db.getObjectBase(baseid);

    return base.objectTable.elements();
  }

  /**
   * <P>This method is used by subclasses of GanymedeBuilderTask to
   * obtain a reference to a {@link arlut.csd.ddroid.server.DBObject DBObject}
   * matching a given invid.</P>
   *
   * @param invid The object id of the object to be viewed
   */

  protected final DBObject getObject(Invid invid)
  {
    return session.session.viewDBObject(invid);
  }

  /**
   * <P>This method is used by subclasses of GanymedeBuilderTask to
   * obtain the label for an object.</P>
   *
   * @param baseid The object id of the object label to be retrieved
   */

  protected final String getLabel(Invid invid)
  {
    return session.viewObjectLabel(invid);
  }

  /**
   * <P>This method is intended to be overridden by subclasses of
   * GanymedeBuilderTask.</P>
   *
   * <P>This method runs with a dumpLock obtained for the builder task.</P>
   *
   * <P>Code run in builderPhase1() can call enumerateObjects() and
   * baseChanged().  Note that the Enumeration of objects returned
   * by enumerateObjects() is only valid and should only be consulted
   * while builderPhase1 is running.. as soon as builderPhase1 returns,
   * the dumpLock used to make the enumerateObjects() call safe to
   * use is relinquished, and any Enumerations obtained will then
   * be unsafe to depend on.</P>
   *
   * @return true if builderPhase1 made changes necessitating the
   * execution of builderPhase2.
   */

  abstract public boolean builderPhase1();

  /**
   * <P>This method is intended to be overridden by subclasses of
   * GanymedeBuilderTask.</P>
   *
   * <P>This method runs after this task's dumpLock has been
   * relinquished.  This method is intended to be used to finish off a
   * build process by running (probably external) code that does not
   * require direct access to the database.</P>
   *
   * <P>For instance, for an NIS builder task, builderPhase1() would scan
   * the Directory Droid object store and write out NIS-compatible source
   * files.  builderPhase1() would return, the run() method drops the
   * dump lock so that other transactions can be committed, and then
   * builderPhase2() can be run to turn those on-disk files written by
   * builderPhase1() into NIS maps.  This generally involves executing
   * an external Makefile, which can take an indeterminate period of
   * time.</P>
   *
   * <P>By releasing the dumpLock before we get to that point, we
   * minimize contention for users of the system.</P>
   *
   * <P>As a result of having dropped the dumpLock, enumerateObjects()
   * cannot be called by this method.</P>
   *
   * <P>builderPhase2 is only run if builderPhase1 returns true.</P>
   */

  abstract public boolean builderPhase2();

  /**
   * <P>This method looks in the optionStrings field in the task
   * object associated with this task and determines whether the given
   * option name is present in the field.  This works only if this
   * builder task was registered with taskDefObjInvid set by a
   * subclass whose constructor takes an Invid parameter and which
   * sets taskDefObjInvid in GanymedeBuilderTask.</P>
   *
   * <P>That is, if the task object for this task has an option strings
   * vector with the following contents:</P>
   *
   * <PRE>
   *  useMD5
   *  useShadow
   * </PRE>
   *
   * <P>then a call to isOptionSet with 'useMD5' or 'useShadow', of
   * any capitalization, will return true.  Any other parameter
   * provided to isOptionSet() will cause null to be returned.</P>
   *
   */

  protected boolean isOptionSet(String option)
  {
    if (option != null && !option.equals(""))
      {
	Vector options = optionsCache;

	if (options == null)
	  {
	    optionsCache = getOptionStrings();
	    options = optionsCache;
	  }

	if (options == null)
	  {
	    return false;
	  }

	for (int i = 0; i < options.size(); i++)
	  {
	    String x = (String) options.elementAt(i);

	    if (x.equalsIgnoreCase(option))
	      {
		return true;
	      }
	  }
      }

    return false;
  }

  /**
   * <P>This method retrieves the value associated with the provided
   * option name if this builder task was registered with taskDefObjInvid
   * set by a subclass whose constructor takes an Invid parameter and which
   * sets taskDefObjInvid in GanymedeBuilderTask.</P>
   *
   * <P>getOptionValue() will search through the option strings for
   * the task object associated with this task and return the
   * substring after the '=' character, if the option name is found on
   * the left.</P>
   *
   * <P>That is, if the task object for this task has an option strings
   * vector with the following contents:</P>
   *
   * <PRE>
   *  useMD5
   *  buildPath=/var/ganymede/schema/NT
   *  useShadow
   * </PRE>
   *
   * <P>then a call to getOptionValue() with 'buildPath', of any capitalization,
   * as the parameter will return '/var/ganymede/schema/NT'.</P>
   *
   * <P>Any other parameter provided to getOptionValue() will cause null to 
   * be returned.</P> 
   */

  protected String getOptionValue(String option)
  {
    if (option != null && !option.equals(""))
      {
	Vector options = optionsCache;

	if (options == null)
	  {
	    optionsCache = getOptionStrings();
	    options = optionsCache;
	  }

	if (options == null)
	  {
	    return null;
	  }

	// get the prefix we'll search for

	String matchPat = option + "=";

	// and spin til we find it

	for (int i = 0; i < options.size(); i++)
	  {
	    String x = (String) options.elementAt(i);

	    if (x.startsWith(matchPat))
	      {
		return x.substring(matchPat.length());
	      }
	  }
      }

    return null;
  }

  /**
   * <P>This method returns the Vector of option strings registered
   * for this task object in the Directory Droid database, or null if no
   * option strings are defined.</P>
   */

  final Vector getOptionStrings()
  {
    if (taskDefObjInvid != null)
      {
	DBObject taskDefObj = getObject(taskDefObjInvid);

	Vector options = taskDefObj.getFieldValues(SchemaConstants.TaskOptionStrings);

	if (options == null || options.size() == 0)
	  {
	    return null;
	  }

	// dup the vector for safety, since we are getting direct
	// access to the Vector in the database

	return (Vector) options.clone();
      }

    return null;
  }

  /**
   *
   * This method opens the specified file for writing out a text stream.
   *
   * If the files have not yet been backed up this run time, openOutFile()
   * will cause the files in Ganymede's output directory to be zipped up
   * before overwriting any files.
   *
   */

  protected synchronized PrintWriter openOutFile(String filename) throws IOException
  {
    return openOutFile(filename, null);
  }

  /**
   *
   * This method opens the specified file for writing out a text stream.
   *
   * If the files have not yet been backed up this run time, openOutFile()
   * will cause the files in Ganymede's output directory to be zipped up
   * before overwriting any files.
   *
   * @param filename The name of the file to open
   * @param taskName The name of the builder task that is writing this file.  Used
   * to create a unique name (across tasks) for the backup copy of the file when
   * we overwrite an existing file.
   *
   */

  protected synchronized PrintWriter openOutFile(String filename, String taskName) throws IOException
  {
    String backupFileName = null;
    File file, backupFile;
    String directory;

    /* -- */

    openBackupDirectory(filename);

    synchronized (GanymedeBuilderTask.class)
      {
	directory = currentBackUpDirectory;

	if (directory != null && !directory.equals(""))
	  {
	    incBusy(directory);
	  }
      }

    if (directory != null && !directory.equals(""))
      {
	try
	  {
	    // see if we have a file by the given name in the backup directory..
	    // if we do, we can't overwrite it

	    file = new File(filename);

	    if (file.exists())
	      {
		Date oldTime = new Date(file.lastModified());
		
		DateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH:mm:ss", 
							    java.util.Locale.US);

		String label = formatter.format(oldTime);

		if (taskName != null)
		  {
		    backupFileName = directory + File.separator + taskName + "_" + label + "_" + file.getName();
		  }
		else
		  {
		    backupFileName = directory + File.separator + label + "_" + file.getName();
		  }

		backupFile = new File(backupFileName);

		// now, we could in principle have more than one copy
		// of a given file name written out in the same
		// second, so just for grins we'll make sure to
		// distinguish

		char subSec = 'a';

		while (backupFile.exists())
		  {
		    String extName = backupFileName + subSec++;

		    backupFile = new File(extName);
		  }

		if (!arlut.csd.Util.FileOps.copyFile(filename, backupFile.getCanonicalPath()))
		  {
		    return null;
		  }
	      }
	  }
	finally
	  {
	    decBusy(currentBackUpDirectory);
	  }
      }

    // we'll go ahead and write over the file if it exists.. that
    // way, we preserve directory permissions

    return new PrintWriter(new BufferedWriter(new FileWriter(filename)));
  }

  //
  // static methods
  // 

  private static synchronized void incBusy(String path)
  {
    Integer x = (Integer) backupsBusy.get(path);

    if (x == null)
      {
	backupsBusy.put(path, new Integer(1));
      }
    else
      {
	backupsBusy.put(path, new Integer(x.intValue() + 1));
      }
  }

  private static synchronized void decBusy(String path)
  {
    Integer x = (Integer) backupsBusy.get(path);

    // we never should get a null value here.. go ahead
    // and throw NullPointerException if we do

    int val = x.intValue();

    if (val == 1)
      {
	backupsBusy.remove(path);
      }
    else
      {
	backupsBusy.put(path, new Integer(val - 1));
      }

    if (oldBackUpDirectory != null && oldBackUpDirectory.equals(path))
      {
	if (val == 0)
	  {
	    // ah, no one is busy writing back ups into
	    // path any more

	    String zipName = oldBackUpDirectory + ".zip";
	    
	    try
	      {
		if (zipIt.zipDirectory(oldBackUpDirectory, zipName))
		  {
		    FileOps.deleteDirectory(oldBackUpDirectory);
		  }

		oldBackUpDirectory = null;
	      }
	    catch (IOException ex)
	      {
	      }
	  }
      }
  }

  private static synchronized int busyCount(String path)
  {
    Integer x = (Integer) backupsBusy.get(path);

    if (x == null)
      {
	return 0;
      }

    return x.intValue();
  }

  /**
   * <P>This method is called before the server's builder
   * tasks are run and creates a backup directory for
   * files to be copied to.</P>
   */

  private static synchronized void openBackupDirectory(String filename) throws IOException
  {
    if (basePath == null)
      {
	basePath = System.getProperty("ganymede.builder.backups");

	if (basePath == null || basePath.equals(""))
	  {
	    Ganymede.debug("GanymedeBuilder not able to determine backups directory.");
	    return;
	  }
	
	basePath = PathComplete.completePath(basePath);
      }

    File directory = new File(basePath);

    if (!directory.exists())
      {
	Ganymede.debug("Warning, can't find ganymede.builder.backup directory " + 
		       basePath + ", not backing up " + filename);
	return;
      }

    if (!directory.isDirectory())
      {
	Ganymede.debug("Warning, ganymede.builder.backup path " + basePath +
		       " is not a directory, not backing up " + filename);
	return;
      }

    if (!directory.canWrite())
      {
	Ganymede.debug("Warning, can't write to ganymede.builder.backup path " + 
		       basePath +
		       ", not backing up " + filename);
	return;
      }

    // okay, we've located our backup directory.. now make sure we
    // know what subdirectory thereunder we're going to use for
    // backups

    if ((currentBackUpDirectory == null) ||
	(System.currentTimeMillis() > rolloverTime) ||
	(System.currentTimeMillis() < rollunderTime))
      {
	Calendar nowCal = new GregorianCalendar();

	int year = nowCal.get(Calendar.YEAR);
	int month = nowCal.get(Calendar.MONTH);
	int day = nowCal.get(Calendar.DAY_OF_MONTH);

	// get a calendar representing 12am midnight local time

	Calendar cal = new GregorianCalendar(year, month, day);

	// first get our roll under time, in case the system
	// clock is ever set back

	Date todayMidnight = cal.getTime();
	rollunderTime = todayMidnight.getTime();

	// and now our roll over time

	cal.add(Calendar.DATE, 1);
	
	Date tomorrowMidnight = cal.getTime();
	rolloverTime = tomorrowMidnight.getTime();

	// if this is our first run of a builder task's file io prep,
	// sweep through the backup directory and zip up any directories
	// that match our pattern for day directories, before we create
	// one for today's date
	
	if (firstRun)
	  {
	    try
	      {
		cleanBackupDirectory();
	      }
	    finally
	      {
		firstRun = false;
	      }
	  }

	// okay, we've got our goal posts fixed, now handle the
	// old directory and get a label for the new

	DateFormat formatter = new SimpleDateFormat("yyyy_MM_dd", java.util.Locale.US);

	oldBackUpDirectory = currentBackUpDirectory;
	currentBackUpDirectory = basePath + File.separator + formatter.format(todayMidnight);

	File newDirectory = new File(currentBackUpDirectory);
	
	if (!newDirectory.exists())
	  {
	    newDirectory.mkdir();
	  }
      }

    // if we haven't zipped up our old directory, do that
    
    if (oldBackUpDirectory != null)
      {
	try
	  {
	    if (busyCount(oldBackUpDirectory) == 0)
	      {
		String zipName = oldBackUpDirectory + ".zip";
		
		Ganymede.debug("GanymedeBuilderTask.openBackupDirectory(): trying to zip " + oldBackUpDirectory);
		
		if (zipIt.zipDirectory(oldBackUpDirectory, zipName))
		  {
		    Ganymede.debug("GanymedeBuilderTask.openBackupDirectory(): zipped " + zipName);
		    FileOps.deleteDirectory(oldBackUpDirectory);
		  }
		else
		  {
		    File dirFile = new File(oldBackUpDirectory);
		    
		    if (dirFile.canRead())
		      {
			String[] list = dirFile.list();
			
			if (list == null || list.length == 0)
			  {
			    Ganymede.debug("GanymedeBuilderTask.openBackupDirectory(): directory " + 
					   oldBackUpDirectory + " is empty, deleting");
			    FileOps.deleteDirectory(oldBackUpDirectory);
			  }
		      }
		  }
	      }
	  }
	finally
	  {
	    oldBackUpDirectory = null;
	  }
      }
  }

  /**
   * <P> This static method is run before the first time a builder task
   * writes any file on server start-up.  It is responsible for sweeping
   * through the system backup directory and zipping up any day directories
   * that are lingering from earlier runs.</P>
   */

  private static synchronized void cleanBackupDirectory() throws IOException
  {
    if (basePath == null || basePath.equals(""))
      {
	return;
      }

    File directory = new File(basePath);

    if (!directory.exists() || !directory.isDirectory() || !directory.canWrite())
      {
	return;
      }

    gnu.regexp.RE regexp = null;

    try
      {
	regexp = new gnu.regexp.RE("(\\d\\d\\d\\d)_(\\d\\d)_(\\d\\d)");
      }
    catch (gnu.regexp.REException ex)
      {
	// assuming we get the pattern right, this shouldn't happen

	ex.printStackTrace();
	return;
      }

    String names[] = directory.list();

    for (int i = 0; i < names.length; i++)
      {
	String dirName = basePath + names[i];

	if (names[i].endsWith(".zip"))
	  {
	    continue;
	  }

	File test = new File(directory, names[i]);

	if (!test.isDirectory())
	  {
	    continue;
	  }

	if (debug)
	  {
	    Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): trying to match " + names[i]);
	  }

	gnu.regexp.REMatch match = regexp.getMatch(names[i]);

	if (match == null)
	  {
	    continue;
	  }

	if (debug)
	  {
	    Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): matched " + names[i]);
	  }

	String yearString, monthString, dateString;

	if (match.getStartIndex(1) != -1)
	  {
	    yearString = names[i].substring(match.getStartIndex(1),
					    match.getEndIndex(1));
	  }
	else
	  {
	    if (debug)
	      {
		Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): " + names[i] + " could not match on year");
	      }

	    continue;
	  }

	if (match.getStartIndex(2) != -1)
	  {
	    monthString = names[i].substring(match.getStartIndex(2),
					     match.getEndIndex(2));
	  }
	else
	  {
	    if (debug)
	      {
		Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): " + names[i] + " could not match on month");
	      }

	    continue;
	  }

	if (match.getStartIndex(3) != -1)
	  {
	    dateString = names[i].substring(match.getStartIndex(3),
					    match.getEndIndex(3));
	  }
	else
	  {
	    if (debug)
	      {
		Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): " + names[i] + " could not match on date");
	      }

	    continue;
	  }

	if (debug)
	  {
	    Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): yearString " + yearString);
	    Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): monthString " + monthString);
	    Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): dateString " + dateString);
	  }

	try
	  {
	    if (debug)
	      {
		Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): trying to zip " + basePath + names[i]);
	      }

	    int year = Integer.parseInt(yearString);
	    int month = Integer.parseInt(monthString);
	    int date = Integer.parseInt(dateString);

	    Calendar cal = new GregorianCalendar(year, month-1, date-1); // midnight start of day
	    cal.add(Calendar.DATE, 1); // end of day

	    if (debug)
	      {
		Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): old directory time is " + cal.getTime());
		Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): rollunder time is " + new Date(rollunderTime));
	      }

	    if (cal.getTime().getTime() < rollunderTime)
	      {
		String zipName = dirName + ".zip";

		Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): zipping " + dirName);

		// it is conceivable that we have successfully zipped
		// a directory before, but did not delete the
		// directory for some reason.. if so, just leave
		// everything alone so that a human can deal with it

		File zipFile = new File(zipName);

		if (!zipFile.exists())
		  {
		    if (zipIt.zipDirectory(dirName, zipName))
		      {
			Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): zipped " + zipName);

			try
			  {
			    FileOps.deleteDirectory(dirName);
			  }
			catch (IOException ex)
			  {
			    Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): could not remove " + 
					   dirName);
			  }
		      }
		    else
		      {
			File dirFile = new File(dirName);
			
			if (dirFile.canRead())
			  {
			    String[] list = dirFile.list();
			
			    if (list == null || list.length == 0)
			      {
				Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): directory " + 
					       dirName + " is empty, deleting");
				FileOps.deleteDirectory(dirName);
			      }
			  }
		      }
		  }
		else
		  {
		    Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): " + dirName + 
				   " zip file already exists, not deleting.");
		  }
	      }
	    else
	      {
		Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): don't need to zip " + 
			       dirName + " yet.");
	      }
	  }
	catch (NumberFormatException ex)
	  {
	    continue;
	  }
      }
  }

  /**
   * <P>This is public for GanymedeSession.openTransaction(), as a
   * hack to support proper updating of the client's status icon on
   * client connect.</P>
   */
  
  public static int getPhase1Count()
  {
    return phase1Count;
  }

  /**
   * <P>This is public for GanymedeSession.openTransaction(), as a
   * hack to support proper updating of the client's status icon on
   * client connect.</P>
   */

  public static int getPhase2Count()
  {
    return phase2Count;
  }

  static synchronized void incPhase1(boolean update)
  {
    phase1Count++;

    if (update)
      {
	updateBuildStatus();
      }
  }

  static synchronized void decPhase1(boolean update)
  {
    phase1Count--;

    if (update)
      {
	updateBuildStatus();
      }
  }

  static synchronized void incPhase2(boolean update)
  {
    phase2Count++;

    if (update)
      {
	updateBuildStatus();
      }
  }

  static synchronized void decPhase2(boolean update)
  {
    phase2Count--;

    if (update)
      {
	updateBuildStatus();
      }
  }

  /**
   * </P>This method is called by the GanymedeBuilderTask base class to
   * record that the server is processing a build.</P>
   */

  static synchronized void updateBuildStatus()
  {
    // phase 1 can have the database locked, so show that
    // for preference

    if (phase1Count > 0)
      {
	GanymedeServer.sendMessageToRemoteSessions(1, "building");
      }
    else if (phase2Count > 0)
      {
	GanymedeServer.sendMessageToRemoteSessions(1, "building2");
      }
    else
      {
	GanymedeServer.sendMessageToRemoteSessions(1, "idle");
      }
  }
}
