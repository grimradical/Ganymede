/*
   GASHAdminDispatch.java

   Logical interface class that provides the connectivity between the
   Ganymede admin console and the server, bidirectionally.

   Created: 28 May 1996

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

package arlut.csd.ganymede.admin;

import java.awt.Color;
import java.awt.Dialog;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.text.NumberFormat;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import arlut.csd.JDialog.DialogRsrc;
import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.JDialog.StandardDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.JTable.rowTable;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.AdminEntry;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.adminAsyncMessage;
import arlut.csd.ganymede.common.scheduleHandle;
import arlut.csd.ganymede.rmi.AdminAsyncResponder;
import arlut.csd.ganymede.rmi.SchemaEdit;
import arlut.csd.ganymede.rmi.Server;
import arlut.csd.ganymede.rmi.adminSession;

/*------------------------------------------------------------------------------
                                                                           class
                                                               GASHAdminDispatch

------------------------------------------------------------------------------*/

/**
 * Logical interface class that provides the connectivity between the
 * Ganymede admin console and the server, bidirectionally.  This class
 * is part of the Ganymede admin console code, not the Ganymede
 * server.
 */

class GASHAdminDispatch implements Runnable {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.admin.GASHAdminDispatch");

  private GASHAdminFrame frame = null;
  private Server server = null;	// remote reference
  private adminSession aSession = null;	// remote reference

  private boolean tasksLoaded = false;

  private AdminAsyncResponder asyncPort = null;	// remote reference

  private Thread asyncPollThread = null;
  private volatile boolean okayToPoll = false;

  private ImageIcon errorBallIcon = null;
  private ImageIcon playIcon = null;
  private ImageIcon okayIcon = null;

  NumberFormat numberFormatter = NumberFormat.getInstance();

  /* -- */

  public GASHAdminDispatch(Server server)
  {
    this.server = server;
  }

  /**
   * This method connects the admin console to the server RMI
   * reference that was provided to the GASHAdminDispatch constructor.
   * If the server returns a failure message, connect() will pop up a
   * dialog providing the error text.  If the connection failed
   * through a RemoteException, it will be passed up for the calling
   * code to handle.
   */

  public boolean connect(String name, String pass) throws RemoteException
  { 
    ReturnVal retVal = handleReturnVal(server.admin(name, pass));

    if (retVal == null || !retVal.didSucceed())
      {
	return false;
      }

    aSession = retVal.getAdminSession();

    // if we get a null session despite having a
    // success-encoding ReturnVal, throw an exception.  The
    // server *should* pass back a proper error report in the
    // ReturnVal, and this NullPointerException should
    // never be thrown.
	
    if (aSession == null)
      {
	throw new NullPointerException("Bad null valued admin session received from server");
      }

    if (debug)
      {
	System.err.println("Got Admin");
      }

    return true;
  }

  public void setFrame(GASHAdminFrame f)
  {
    if (frame == null)
      {
	frame = f;
      }
    else
      {
	System.err.println("I already have a frame, thank you very much.");
      }
  }

  public void startAsyncPoller() throws RemoteException
  {
    asyncPort = aSession.getAsyncPort();

    if (asyncPort == null)
      {
	throw new RemoteException("Couldn't find admin console asyncPort");
      }

    okayToPoll = true;
    asyncPollThread = new Thread(this, "Async Server Poll Thread");
    asyncPollThread.start();
  }

  public void stopAsyncPoller()
  {
    okayToPoll = false;
  }

  /**
   * This method spins continuously, polling the server for {@link
   * arlut.csd.ganymede.common.adminAsyncMessage adminAsyncMessages}.
   * The server will block until something happens, then download a
   * set of adminAsyncMessages.  This run method will then dispatch
   * those messages to the appropriate GASHAdminDispatch methods for
   * propagation into the admin console's GUI.
   */

  public void run()
  {
    adminAsyncMessage events[] = null;
    adminAsyncMessage event = null;

    /* -- */

    if (asyncPort == null)
      {
	return;
      }

    try
      {
	while (okayToPoll)
	  {
	    events = asyncPort.getNextMsgs(); // will block on server

	    if (events == null || events.length == 0)
	      {
		return;
	      }

	    for (int i = 0; i < events.length; i++)
	      {
		event = events[i];

		switch (event.getMethod())
		  {
		  case adminAsyncMessage.SETSERVERSTART:
		    setServerStart((Date) event.getParam(0));
		    break;

		  case adminAsyncMessage.SETLASTDUMPTIME:
		    setLastDumpTime((Date) event.getParam(0));
		    break;

		  case adminAsyncMessage.SETTRANSACTIONS:
		    setTransactionsInJournal(event.getInt(0));
		    break;

		  case adminAsyncMessage.SETOBJSCHECKOUT:
		    setObjectsCheckedOut(event.getInt(0));
		    break;

		  case adminAsyncMessage.SETLOCKSHELD:
		    setLocksHeld(event.getInt(0));
		    break;

		  case adminAsyncMessage.CHANGESTATE:
		    changeState(event.getString(0));
		    break;

		  case adminAsyncMessage.LOGAPPEND:
		    logAppend(((StringBuffer) event.getParam(0)).toString());
		    break;

		  case adminAsyncMessage.CHANGEADMINS:
		    changeAdmins(event.getString(0));
		    break;

		  case adminAsyncMessage.CHANGEUSERS:
		    changeUsers(event.getParams());
		    break;

		  case adminAsyncMessage.CHANGETASKS:
		    changeTasks(event.getParams());
		    break;

		  case adminAsyncMessage.SETMEMORYSTATE:
		    setMemoryState(event.getLong(0), event.getLong(1));
		    break;

		  case adminAsyncMessage.FORCEDISCONNECT:
		    forceDisconnect(event.getString(0));
		    break;

		  default:
		    System.err.println("Unrecognized adminAsyncMessage: " + event);
		  }
	      }
	  }
      }
    catch (RemoteException ex)
      {
      }
    finally
      {
	asyncPort = null;
      }
  }

  /**
   * This method is remotely called by the Ganymede server to set the
   * server start date in the admin console.
   */

  public void setServerStart(Date date)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.setServerStart()");
      }

    if (frame == null)
      {
	return;
      }

    final Date lDate = date;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.startField.setText(lDate.toString());
      }
    });
  }

  /**
   * This method is remotely called by the Ganymede server to set the
   * last dump date in the admin console.
   */

  public void setLastDumpTime(Date date)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.setLastDumpTime()");
      }

    if (frame == null)
      {
	return;
      }

    final Date lDate = date;
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	if (lDate == null)
	  {
	    frame.dumpField.setText("no dump since server start");
	  }
	else
	  {
	    frame.dumpField.setText(lDate.toString());
	  }
      }
    });
  }

  /**
   * This method is remotely called by the Ganymede server to set the
   * number of transactions in the server's journal in the admin
   * console.
   */

  public void setTransactionsInJournal(int trans)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.setTransactionsInJournal()");
      }

    if (frame == null)
      {
	return;
      }

    final int lTrans = trans;
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.journalField.setText("" + lTrans);
      }
    });
  }

  /**
   * This method is remotely called by the Ganymede server to set the
   * number of objects checked out in the admin console.
   */

  public void setObjectsCheckedOut(int objs)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.setObjectsCheckedOut()");
      }

    if (frame == null)
      {
	return;
      }

    final int lObjs = objs;
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.checkedOutField.setText("" + lObjs);
      }
    });
  }

  /**
   * This method is remotely called by the Ganymede server to set the
   * number of locks held in the admin console.
   */

  public void setLocksHeld(int locks)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.setLocksHeld()");
      }

    if (frame == null)
      {
	return;
      }

    final int lLocks = locks;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.locksField.setText("" + lLocks);
      }
    });
  }

  /**
   * This method is remotely called by the Ganymede server to update
   * the memory statistics display in the admin console.
   */

  public void setMemoryState(long freeMemory, long totalMemory)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.setMemoryState()");
      }

    if (frame == null)
      {
	return;
      }

    final long lFreeMemory = freeMemory;
    final long lTotalMemory = totalMemory;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	String inuse = numberFormatter.format(lTotalMemory - lFreeMemory);
	String free = numberFormatter.format(lFreeMemory);
	String total = numberFormatter.format(lTotalMemory);

	frame.usedMemField.setText(inuse);
	frame.freeMemField.setText(free);
	frame.totalMemField.setText(total);
      }
    });
  }

  /**
   * This method is remotely called by the Ganymede server to add to
   * the admin console's log display.
   *
   * @param status A string to add to the console's log display, with
   * the trailing newline included.
   */

  public void logAppend(String status)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.logAppend()");
      }

    if (frame == null)
      {
	return;
      }

    final String lStatus = status;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.appendStyledLogText(lStatus);
      }
    });
  }

  /**
   * This method is remotely called by the Ganymede server to update
   * the number of admin consoles attached to the server.
   */

  public void changeAdmins(String adminStatus)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.changeAdmins()");
      }

    if (frame == null)
      {
	return;
      }

    final String lAdminStatus = adminStatus;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.adminField.setText(lAdminStatus);
      }
    });
  }

  /**
   * This method is remotely called by the Ganymede server to update
   * the admin console's server state display.
   */

  public void changeState(String state)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.changeState()");
      }

    if (frame == null)
      {
	return;
      }

    final String lState = state;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.stateField.setText(lState);
      }
    });
  }

  /**
   * This method is remotely called by the Ganymede server to update
   * the admin console's connected user table.
   *
   * @param entries a Vector of {@link arlut.csd.ganymede.common.AdminEntry AdminEntry}
   * login description objects.
   */

  public void changeUsers(Object entries[])
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.changeUsers()");
      }

    if (frame == null)
      {
	return;
      }

    /* -- */

    final Object localEntries[] = entries;

    // And refresh our table.. we'll wait until this succeeds so we
    // don't get the server sending us more updates before the table
    // finishes drawing

    try
      {
	SwingUtilities.invokeAndWait(new Runnable() {
	  public void run() {

	    AdminEntry e;
	    frame.table.clearCells();
    
	    // Process entries from the server

	    for (int i = 0; i < localEntries.length; i++)
	      {
		e = (AdminEntry) localEntries[i];

		frame.table.newRow(e.sessionName);

		// the sessionName from the AdminEntry is constant,
		// and gives us a unique session name for the session.
		//
		// if we don't have a persona name, we'll display that
		// unique session name, otherwise we'll show the
		// user's persona name, so as to show the user's
		// active privilege level.
		//
		// either way, the table tracks the unique session
		// name for us, so that when the user right-clicks on
		// the entry to kick a user off, the proper session is
		// targeted.

		if (e.personaName == null || e.personaName.equals(""))
		  {
		    frame.table.setCellText(e.sessionName, 0, e.sessionName, false);
		  }
		else
		  {
		    frame.table.setCellText(e.sessionName, 0, e.personaName, false);
		  }

		frame.table.setCellText(e.sessionName, 1, e.hostname, false);
		frame.table.setCellText(e.sessionName, 2, e.status, false);
		frame.table.setCellText(e.sessionName, 3, e.connecttime, false);
		frame.table.setCellText(e.sessionName, 4, e.event, false);
		frame.table.setCellText(e.sessionName, 5, Integer.toString(e.objectsCheckedOut), false);
	      }

	    frame.table.refreshTable();
	  }
	});
      }
    catch (InvocationTargetException ite)
      {
	ite.printStackTrace();
      }
    catch (InterruptedException ie)
      {
	ie.printStackTrace();
      }
  }

  /**
   * This method is remotely called by the Ganymede server to update
   * the admin console's task table.
   *
   * @param tasks an array of {@link
   * arlut.csd.ganymede.common.scheduleHandle scheduleHandle} objects
   * describing the tasks registered in the Ganymede server.
   */

  public void changeTasks(Object tasks[])
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.changeTasks()");
      }

    if (frame == null)
      {
	return;
      }

    scheduleHandle handle;

    /* -- */

    if (!tasksLoaded)
      {
	// System.err.println("changeTasks: tasks size = " + tasks.size());
    
	// Sort entries according to their incep date,
	// to prevent confusion if new tasks are put into
	// the server-side hashes, and as they are shuffled
	// from hash to hash
	
	java.util.Arrays.sort(tasks,
			      new Comparator() 
			      {
				public int compare(Object a, Object b) 
				{
				  scheduleHandle aH, bH;
				  
				  aH = (scheduleHandle) a;
				  bH = (scheduleHandle) b;
				  
				  if (aH.incepDate.before(bH.incepDate))
				    {
				      return -1;
				    }
				  else if (aH.incepDate.after(bH.incepDate))
				    {
				      return 1;
				    }
				  else
				    {
				      return 0;
				    }
				}
			      }
			      );
      }

    Vector<scheduleHandle> syncTasks = new Vector<scheduleHandle>();
    Vector<scheduleHandle> scheduledTasks = new Vector<scheduleHandle>();
    Vector<scheduleHandle> manualTasks = new Vector<scheduleHandle>();

    for (int i = 0; i < tasks.length; i++)
      {
	handle = (scheduleHandle) tasks[i];

	switch (handle.getTaskType())
	  {
	  case SCHEDULED:
	    scheduledTasks.addElement(handle);
	    break;

	  case MANUAL:
	    manualTasks.addElement(handle);
	    break;

	  case BUILDER:
	  case UNSCHEDULEDBUILDER:
	  case SYNCINCREMENTAL:
	  case SYNCFULLSTATE:
	  case SYNCMANUAL:
	    syncTasks.addElement(handle);
	  }
      }

    updateSyncTaskTable(frame.syncTaskTable, syncTasks);
    updateTaskTable(frame.taskTable, scheduledTasks);
    updateTaskTable(frame.manualTaskTable, manualTasks);
  }

  private void updateTaskTable(rowTable table, Vector<scheduleHandle> tasks)
  {
    Vector taskNames = new Vector();

    /* -- */

    // now reload the table with the current stats

    for (scheduleHandle handle: tasks)
      {
	taskNames.addElement(handle.name);

	if (!table.containsKey(handle.name))
	  {
	    table.newRow(handle.name);
	  }

	table.setCellText(handle.name, 0, handle.name, false); // task name

	if (handle.isRunning() && handle.isSuspended())
	  {
	    // "Suspended upon completion"
	    table.setCellText(handle.name, 1, ts.l("changeTasks.runningSuspendedState"), false);
	    table.setCellColor(handle.name, 1, Color.red, false);
	    table.setCellBackColor(handle.name, 1, Color.white, false);
	  }
	else if (handle.isRunning())
	  {
	    // "Running"
	    table.setCellText(handle.name, 1, ts.l("changeTasks.runningState"), false);
	    table.setCellColor(handle.name, 1, Color.blue, false);
	    table.setCellBackColor(handle.name, 1, Color.white, false);
	  }
	else if (handle.isSuspended())
	  {
	    // "Suspended"
	    table.setCellText(handle.name, 1, ts.l("changeTasks.suspendedState"), false);
	    table.setCellColor(handle.name, 1, Color.red, false);
	    table.setCellBackColor(handle.name, 1, Color.white, false);
	  }
	else if (handle.startTime != null)
	  {
	    // "Scheduled"
	    table.setCellText(handle.name, 1, ts.l("changeTasks.scheduledState"), false);
	    table.setCellColor(handle.name, 1, Color.black, false);
	    table.setCellBackColor(handle.name, 1, Color.white, false);
	  }
	else
	  {
	    // "Waiting"
	    table.setCellText(handle.name, 1, ts.l("changeTasks.waitingState"), false);
	    table.setCellColor(handle.name, 1, Color.black, false);
	    table.setCellBackColor(handle.name, 1, Color.white, false);
	  }

	if (handle.lastTime != null)
	  {
	    table.setCellText(handle.name, 2, handle.lastTime.toString(), false);
	  }

	if (table.getColCount() > 3)
	  {
	    if (handle.startTime != null)
	      {
		table.setCellText(handle.name, 3, handle.startTime.toString(), false);
	      }
	    else
	      {
		// "On Demand"
		table.setCellText(handle.name, 3, ts.l("changeTasks.onDemandState"), false);
	      }

	    table.setCellText(handle.name, 4, handle.intervalString, false);
	  }
      }

    // and take any rows out that are gone

    Vector tasksKnown = new Vector();
    Enumeration en = table.keys();

    while (en.hasMoreElements())
      {
	tasksKnown.addElement(en.nextElement());
      }

    Vector removedTasks = VectorUtils.difference(tasksKnown, taskNames);
    
    for (int i = 0; i < removedTasks.size(); i++)
      {
	table.deleteRow(removedTasks.elementAt(i), false);
      }

    // And refresh our table.. we'll wait until this succeeds so we
    // don't get the server sending us more updates before the table
    // finishes drawing

    final rowTable localTableRef = table;

    try
      {
	SwingUtilities.invokeAndWait(new Runnable() {
	  public void run() {
	    localTableRef.refreshTable();
	  }
	});
      }
    catch (InvocationTargetException ite)
      {
	ite.printStackTrace();
      }
    catch (InterruptedException ie)
      {
	ie.printStackTrace();
      }
  }

  private void updateSyncTaskTable(rowTable table, Vector<scheduleHandle> tasks)
  {
    Vector taskNames = new Vector();

    /* -- */

    boolean error_seen = false;
    boolean running = false;

    // now reload the table with the current stats

    for (scheduleHandle handle: tasks)
      {
	taskNames.addElement(handle.name);

	if (!table.containsKey(handle.name))
	  {
	    table.newRow(handle.name);
	  }

	table.setCellText(handle.name, 0, handle.name, false); // task name

	table.setCellText(handle.name, 1, handle.getTaskType().toString(), false);

	if (handle.isRunning() && handle.isSuspended())
	  {
	    // "Suspended upon completion"
	    table.setCellText(handle.name, 3, ts.l("changeTasks.runningSuspendedState"), false);
	    table.setCellColor(handle.name, 3, Color.red, false);
	    table.setCellBackColor(handle.name, 3, Color.white, false);
	  }
	else if (handle.isRunning())
	  {
	    // "Running"
	    table.setCellText(handle.name, 3, ts.l("changeTasks.runningState"), false);
	    table.setCellColor(handle.name, 3, Color.blue, false);
	    table.setCellBackColor(handle.name, 3, Color.white, false);

	    running = true;
	  }
	else if (handle.isSuspended())
	  {
	    // "Suspended"
	    table.setCellText(handle.name, 3, ts.l("changeTasks.suspendedState"), false);
	    table.setCellColor(handle.name, 3, Color.red, false);
	    table.setCellBackColor(handle.name, 3, Color.white, false);
	  }
	else if (handle.startTime != null)
	  {
	    // "Scheduled"
	    table.setCellText(handle.name, 3, ts.l("changeTasks.scheduledState"), false);
	    table.setCellColor(handle.name, 3, Color.black, false);
	    table.setCellBackColor(handle.name, 3, Color.white, false);
	  }
	else
	  {
	    // "Waiting"
	    table.setCellText(handle.name, 3, ts.l("changeTasks.waitingState"), false);
	    table.setCellColor(handle.name, 3, Color.black, false);
	    table.setCellBackColor(handle.name, 3, Color.white, false);
	  }

	table.setCellText(handle.name, 2, handle.getTaskStatus().getMessage(handle.queueSize, handle.condition), false);

	switch (handle.getTaskStatus())
	  {
	  case OK:
	  case EMPTYQUEUE:
	  case NONEMPTYQUEUE:
	    table.setCellColor(handle.name, 2, Color.black, false);
	    table.setCellBackColor(handle.name, 2, Color.white, false);
	    break;
	    
	  default:
	    table.setCellColor(handle.name, 2, Color.white, false);
	    table.setCellBackColor(handle.name, 2, Color.red, false);
	    error_seen = true;
	  }

	if (handle.lastTime != null)
	  {
	    table.setCellText(handle.name, 4, handle.lastTime.toString(), false);
	  }
      }

    if (running)
      {
	frame.tabPane.setIconAt(1, getPlayIcon());
      }
    else if (error_seen)
      {
	frame.tabPane.setIconAt(1, getErrorBall());
      }
    else
      {
	frame.tabPane.setIconAt(1, getOkIcon());
      }

    // and take any rows out that are gone

    Vector tasksKnown = new Vector();
    Enumeration en = table.keys();

    while (en.hasMoreElements())
      {
	tasksKnown.addElement(en.nextElement());
      }

    Vector removedTasks = VectorUtils.difference(tasksKnown, taskNames);
    
    for (int i = 0; i < removedTasks.size(); i++)
      {
	table.deleteRow(removedTasks.elementAt(i), false);
      }

    // And refresh our table.. we'll wait until this succeeds so we
    // don't get the server sending us more updates before the table
    // finishes drawing

    final rowTable localTableRef = table;

    try
      {
	SwingUtilities.invokeAndWait(new Runnable() {
	  public void run() {
	    localTableRef.refreshTable();
	  }
	});
      }
    catch (InvocationTargetException ite)
      {
	ite.printStackTrace();
      }
    catch (InterruptedException ie)
      {
	ie.printStackTrace();
      }
  }

  /**
   * This method is called by admin console code to force a complete
   * rebuild of all external builds.  This means that all databases
   * will have their last modification timestamp cleared and all
   * builder tasks will be scheduled for immediate execution.
   */

  public void forceBuild() throws RemoteException
  {
    handleReturnVal(aSession.forceBuild());
  }

  public void disconnect() throws RemoteException
  {
    aSession.logout();
  }

  /**
   * Callback: The server can tell us to disconnect if the server is 
   * going down.
   */

  public void forceDisconnect(String reason)
  {
    // "Disconnected: {0}\n"
    logAppend(ts.l("forceDisconnect.disconnectMessage", reason));
    server = null;
  }

  // ------------------------------------------------------------
  // convenience methods for our GASHAdminFrame
  // ------------------------------------------------------------

  void refreshMe() throws RemoteException
  {
    aSession.refreshMe();
  }

  void kill(String username) throws RemoteException
  {
    handleReturnVal(aSession.kill(username));
  }

  void runTaskNow(String taskName) throws RemoteException
  {
    handleReturnVal(aSession.runTaskNow(taskName));
  }

  void stopTask(String taskName) throws RemoteException
  {
    handleReturnVal(aSession.stopTask(taskName));
  }

  void disableTask(String taskName) throws RemoteException
  {
    handleReturnVal(aSession.disableTask(taskName));
  }

  void enableTask(String taskName) throws RemoteException
  {
    handleReturnVal(aSession.enableTask(taskName));
  }

  void killAll() throws RemoteException
  {
    handleReturnVal(aSession.killAll());
  }

  boolean shutdown(boolean waitForUsers) throws RemoteException
  {
    ReturnVal retVal = handleReturnVal(aSession.shutdown(waitForUsers));

    return (retVal == null || retVal.didSucceed());
  }

  void dumpDB() throws RemoteException
  {
    handleReturnVal(aSession.dumpDB());
  }

  void runInvidTest() throws RemoteException
  {
    handleReturnVal(aSession.runInvidTest());
  }

  void runInvidSweep() throws RemoteException
  {
    handleReturnVal(aSession.runInvidSweep());
  }

  void runEmbeddedTest() throws RemoteException
  {
    handleReturnVal(aSession.runEmbeddedTest());
  }

  void runEmbeddedSweep() throws RemoteException
  {
    handleReturnVal(aSession.runEmbeddedSweep());
  }

  GASHSchema pullSchema() throws RemoteException
  {
    SchemaEdit editor = null;

    /* -- */

    if (debug)
      {
	System.err.println("Trying to get SchemaEdit handle");
      }

    try
      {
	editor = aSession.editSchema();
      }
    catch (RemoteException ex)
      {
	System.err.println("editSchema() exception: " + ex);
      }

    if (editor == null)
      {
	System.err.println("null editor handle");
	return null;
      }
    else
      {
	if (debug)
	  {
	    System.err.println("Got SchemaEdit handle");
	  }

	// the GASHSchema constructor pops itself up at the end of
	// initialization

	// "Schema Editor"	
	return new GASHSchema(ts.l("pullSchema.schemaEditingTitle"), editor, this);
      }
  }

  /**
   * This method is designed to be called by the GASHSchema schema editor when
   * it closes down.
   */

  public void clearSchemaEditor()
  {
    frame.schemaMI.setEnabled(true);
    frame.schemaEditor = null;
  }

  /**
   * This method takes a ReturnVal object from the server and, if
   * necessary, runs through a wizard interaction sequence, possibly
   * displaying several dialogs before finally returning a final
   * result code.
   *
   * Use the ReturnVal returned from this function after this function
   * is called to determine the ultimate success or failure of any
   * operation which returns ReturnVal, because a wizard sequence may
   * determine the ultimate result.
   *
   * This method should not be synchronized, since handleReturnVal may
   * pop up modal (thread-blocking) dialogs, and if we we synchronize
   * this, some Swing or AWT code seems to block on our
   * synchronization when we do pop-up dialogs.  It's not any of my
   * code, so I assume that AWT tries to synchronize on the frame when
   * parenting a new dialog.
   */

  public ReturnVal handleReturnVal(ReturnVal retVal)
  {
    Hashtable dialogResults;
    DialogRsrc resource;

    /* -- */

    if (debug)
      {
	System.err.println("GASHAdminDispatch.handleReturnVal(): Entering");

	try
	  {
	    throw new RuntimeException("TRACE");
	  }
	catch (RuntimeException ex)
	  {
	    ex.printStackTrace();
	  }
      }

    while ((retVal != null) && (retVal.getDialog() != null))
      {
	if (debug)
	  {
	    System.err.println("GASHAdminDispatch.handleReturnVal(): retrieving dialog");
	  }

	JDialogBuff jdialog = retVal.getDialog();

	if (debug)
	  {
	    System.err.println("GASHAdminDispatch.handleReturnVal(): extracting dialog");
	  }

	if (frame == null)
	  {
	    if (debug)
	      {
		System.err.println("GASHAdminDispatch.handleReturnVal(): null frame");
	      }

	    resource = jdialog.extractDialogRsrc(new JFrame(), this.getClass());
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("GASHAdminDispatch.handleReturnVal(): good frame");
	      }

	    resource = jdialog.extractDialogRsrc(frame, null);
	  }

	if (debug)
	  {
	    System.err.println("GASHAdminDispatch.handleReturnVal(): constructing dialog");
	  }

	StringDialog dialog = new StringDialog(resource, StandardDialog.ModalityType.DOCUMENT_MODAL);

	if (debug)
	  {
	    System.err.println("GASHAdminDispatch.handleReturnVal(): displaying dialog");
	  }

	// display the Dialog sent to us by the server, get the
	// result of the user's interaction with it.
	    
	dialogResults = dialog.showDialog();

	if (debug)
	  {
	    System.err.println("GASHAdminDispatch.handleReturnVal(): dialog done");
	  }

	if (retVal.getCallback() != null)
	  {
	    try
	      {
		if (debug)
		  {
		    System.err.println("GASHAdminDispatch.handleReturnVal(): Sending result to callback: " + dialogResults);
		  }

		// send the dialog results to the server

		retVal = retVal.getCallback().respond(dialogResults);

		if (debug)
		  {
		    System.err.println("GASHAdminDispatch.handleReturnVal(): Received result from callback.");
		  }
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("Caught remote exception: " + ex.getMessage());
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("GASHAdminDispatch.handleReturnVal(): No callback, breaking");
	      }

	    break;		// we're done
	  }
      }

    if (debug)
      {
	if (retVal != null)
	  {
	    if (retVal.didSucceed())
	      {
		System.err.println("GASHAdminDispatch.handleReturnVal(): returning success code");
	      }
	    else
	      {
		System.err.println("GASHAdminDispatch.handleReturnVal(): returning failure code");
	      }
	  }
	else
	  {
	    System.err.println("GASHAdminDispatch.handleReturnVal(): returning null retVal (success)");
	  }
      }

    return retVal;
  }

  public ImageIcon getErrorBall()
  {
    if (errorBallIcon == null)
      {
	errorBallIcon = new ImageIcon(PackageResources.getImageResource(frame, "error-ball.png", getClass()));
      }

    return errorBallIcon;
  }

  public ImageIcon getPlayIcon()
  {
    if (playIcon == null)
      {
	playIcon = new ImageIcon(PackageResources.getImageResource(frame, "playing-icon.png", getClass()));
      }

    return playIcon;
  }

  public ImageIcon getOkIcon()
  {
    if (okayIcon == null)
      {
	okayIcon = new ImageIcon(PackageResources.getImageResource(frame, "okay-status.png", getClass()));
      }

    return okayIcon;
  }
}
