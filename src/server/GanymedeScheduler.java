/*

   GanymedeScheduler.java

   This class is designed to be run in a thread.. it allows the main server
   to register tasks to be run on a periodic basis.
   
   Created: 26 January 1998
   Version: $Revision: 1.10 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               GanymedeScheduler

------------------------------------------------------------------------------*/

/**
 *
 * This class is designed to act as a background task scheduler for the
 * Ganymede server.  It is similar in function and behavior to the UNIX
 * cron facility, except that it is not currently as fancy as cron is
 * in terms of specifying periodicity of task scheduling, and in that
 * it provides some operations for run-time management of scheduled
 * and running tasks.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 */

public class GanymedeScheduler extends Thread {

  static final int minsPerDay = 1440;
  static final boolean debug = false;

  /**
   *
   * Debug rig
   *
   */

  public static void main(String[] argv)
  {
    GanymedeScheduler scheduler = new GanymedeScheduler(false);
    new Thread(scheduler).start();

    Date time, currentTime;
    Calendar cal = Calendar.getInstance();

    currentTime = new Date();

    cal.setTime(currentTime);

    cal.add(Calendar.MINUTE, 1);

    scheduler.addAction(cal.getTime(), 
			new sampleTask("sample task 1"), 
			"sample task 1");

    scheduler.addPeriodicAction(cal.get(Calendar.HOUR_OF_DAY), 
				cal.get(Calendar.MINUTE), 1,
				new sampleTask("sample task 2"), "sample task 2");

    cal.add(Calendar.MINUTE, 1);

    scheduler.addAction(cal.getTime(), 
			new sampleTask("sample task 3"),
			"sample task 3");

    cal.add(Calendar.MINUTE, 1);

    scheduler.addPeriodicAction(cal.get(Calendar.HOUR_OF_DAY), 
				cal.get(Calendar.MINUTE), 1,
				new sampleTask("sample task 4"), "sample task 4");
  }

  // --- end statics

  Date nextAction = null;
  private Hashtable currentlyScheduled = new Hashtable();
  private Hashtable currentlyRunning = new Hashtable();
  private Hashtable onDemand = new Hashtable();
  private Vector taskList = new Vector();	// for reporting to admin consoles
  private boolean taskListInitialized = false;
  private boolean reportTasks;

  /**
   *
   * Constructor
   *
   * @param reportTasks if true, the scheduler will attempt to notify
   *                    the GanymedeAdmin class when tasks are scheduled
   *                    and/or completed.
   *
   */

  public GanymedeScheduler(boolean reportTasks)
  {
    this.reportTasks = reportTasks;
  }

  /**
   *
   * This method is used to add a task to the scheduler that will not
   * be scheduled until specifically requested. 
   *
   *
   */

  public synchronized void addActionOnDemand(Runnable task,
					     String name)
  {
    scheduleHandle handle;

    /* -- */

    if (task == null || name == null)
      {
	throw new IllegalArgumentException("bad params to GanymedeScheduler.addAction()");
      }

    if (currentlyRunning.containsKey(name) || 
	currentlyScheduled.containsKey(name) ||
	onDemand.containsKey(name))
      {
	throw new IllegalArgumentException("error, task " + name + " already registered with scheduler");
      }

    handle = new scheduleHandle(this, null, 0, task, name);

    onDemand.put(handle.name, handle);
  }

  /**
   *
   * This method is used to add an action to be run once, at a specific time.
   *
   */

  public synchronized void addAction(Date time, 
				     Runnable task,
				     String name)
  {
    scheduleHandle handle;

    /* -- */

    if (time == null || task == null || name == null)
      {
	throw new IllegalArgumentException("bad params to GanymedeScheduler.addAction()");
      }

    if (currentlyRunning.containsKey(name) || currentlyScheduled.containsKey(name) ||
	onDemand.containsKey(name))
      {
	throw new IllegalArgumentException("error, task " + name + " already registered with scheduler");
      }

    handle = new scheduleHandle(this, time, 0, task, name);
    scheduleTask(handle);
    
    System.err.println("Ganymede Scheduler: Scheduled task " + name + " for execution at " + time);
  }

  /**
   *
   * This method is used to add an action to be run every day at a specific time.
   *
   */

  public synchronized void addDailyAction(int hour, int minute, 
					  Runnable task,
					  String name)
  {
    scheduleHandle handle;
    Date time, currentTime;
    Calendar cal = Calendar.getInstance();

    /* -- */

    if (task == null || name == null)
      {
	throw new IllegalArgumentException("bad params to GanymedeScheduler.addAction()");
      }

    if (currentlyRunning.containsKey(name) || currentlyScheduled.containsKey(name) ||
	onDemand.containsKey(name))
      {
	throw new IllegalArgumentException("error, task " + name + " already registered with scheduler");
      }

    currentTime = new Date();

    cal.setTime(currentTime);

    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, minute);

    time = cal.getTime();

    if (time.before(currentTime))
      {
	cal.add(Calendar.DATE, 1); // advance to this time tomorrow
      }

    time = cal.getTime();

    handle = new scheduleHandle(this, time, minsPerDay, task, name);
    scheduleTask(handle);

    System.err.println("Ganymede Scheduler: Scheduled task " + name + " for daily execution at " + time);
  }

  /**
   * This method is used to add an action to be run at a specific
   * initial time, and every <intervalMinutes> thereafter.  
   *
   * The scheduler will not reschedule a task until the last scheduled
   * instance of the task has completed.
   *
   */

  public synchronized void addPeriodicAction(int hour, int minute, 
					     int intervalMinutes, 
					     Runnable task,
					     String name)
  {
    scheduleHandle handle;
    Date time, currentTime;
    Calendar cal = Calendar.getInstance();

    /* -- */

    if (task == null || name == null)
      {
	throw new IllegalArgumentException("bad params to GanymedeScheduler.addAction()");
      }

    if (currentlyRunning.containsKey(name) || currentlyScheduled.containsKey(name) ||
	onDemand.containsKey(name))
      {
	throw new IllegalArgumentException("error, task " + name + " already registered with scheduler");
      }

    currentTime = new Date();

    cal.setTime(currentTime);

    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, minute);

    time = cal.getTime();

    if (time.before(currentTime))
      {
	cal.add(Calendar.DATE, 1); // advance to this time tomorrow
      }

    time = cal.getTime();

    handle = new scheduleHandle(this, time, intervalMinutes, task, name);

    scheduleTask(handle);

    System.err.println("Ganymede Scheduler: Scheduled task " + name + " for periodic execution at " + time);
    System.err.println("                    Task will repeat every " + intervalMinutes + " minutes");
  }

  /**
   *
   * This method is provided to allow an admin console to cause a registered
   * task to be immediately spawned.
   *
   * @return true if the task is either currently running or was started, 
   *         or false if the task could not be found in the list of currently
   *         registered tasks.
   *
   */

  public synchronized boolean runTaskNow(String name)
  {
    if (currentlyRunning.containsKey(name))
      {
	return true;		// it's already running
      }
    else
      {
	scheduleHandle handle = (scheduleHandle) currentlyScheduled.get(name);

	if (handle == null)
	  {
	    handle = (scheduleHandle) onDemand.get(name);
	  }

	if (handle == null)
	  {
	    return false;
	  }

	runTask(handle);

	return true;
      }
  }

  /**
   *
   * This method is provided to allow the server to request that a task
   * listed as being registered 'on-demand' be run as soon as possible.
   *
   * If the task is currently running, it will be flagged to run again
   * as soon as the current run completes.  This is intended to support
   * the need for the server to be able to do back-to-back nis/dns builds.
   *
   * @return false if the task name could not be found on the on-demand
   *         or currently running lists.
   *
   */

  public synchronized boolean demandTask(String name)
  {
    if (!currentlyRunning.containsKey(name) &&
	!onDemand.containsKey(name))
      {
	return false;
      }
    else
      {
	scheduleHandle handle = (scheduleHandle) currentlyRunning.get(name);

	if (handle != null)
	  {
	    handle.rerun = true;
	    updateTaskInfo(true);
	    return true;
	  }

	handle = (scheduleHandle) onDemand.get(name);

	if (handle == null)
	  {
	    return false;
	  }

	runTask(handle);

	return true;
      }
  }
  
  /**
   *
   * This method is provided to allow an admin console to put an
   * immediate halt to a running background task.
   *
   * @return true if the task was either not running, or was
   *         running and was told to stop.
   *
   */

  public synchronized boolean stopTask(String name)
  {
    if (!currentlyRunning.containsKey(name))
      {
	return true;		// it's not running
      }
    else
      {
	scheduleHandle handle = (scheduleHandle) currentlyRunning.get(name);

	if (handle == null)
	  {
	    return false;	// couldn't find task
	  }

	handle.stop();

	updateTaskInfo(true);

	return true;
      }
  }

  /**
   *
   * This method is provided to allow an admin console to specify
   * that a task be suspended.  Suspended tasks will not be
   * run until later enabled.  If the task is currently running,
   * it will not be interfered with, but the task will not be
   * issued for execution in future until re-enabled.
   *
   * @return true if the task was found and disabled
   *
   */

  public synchronized boolean disableTask(String name)
  {
    scheduleHandle handle = null;

    /* -- */

    handle = (scheduleHandle) currentlyRunning.get(name);

    if (handle == null)
      {
	handle = (scheduleHandle) currentlyScheduled.get(name);
      }

    if (handle == null)
      {
	return false;		// couldn't find it
      }
    else
      {
	handle.disable();
	updateTaskInfo(true);
	return true;
      }
  }

  /**
   *
   * This method is provided to allow an admin console to specify
   * that a task be re-enabled after a suspension.
   *
   * A re-enabled task will be scheduled for execution according
   * to its original scheduled, with any runtimes that would have
   * been issued during the time the task was suspended simply
   * skipped.
   *
   * @return true if the task was found and enabled
   *
   */

  public synchronized boolean enableTask(String name)
  {
    scheduleHandle handle = null;

    /* -- */

    handle = (scheduleHandle) currentlyRunning.get(name);

    if (handle == null)
      {
	handle = (scheduleHandle) currentlyScheduled.get(name);
      }

    if (handle == null)
      {
	return false;		// couldn't find it
      }
    else
      {
	handle.enable();

	updateTaskInfo(true);
	return true;
      }
  }

  /**
   *
   * This method is provided to allow an admin console to reschedule
   * the next invocation time of a named task, and to change the
   * interval in minutes between invocations of this task.
   *
   * @param name The name of the task to be rescheduled
   * @param time The desired time of the next invocation.  If the task
   * is currently running, this time will be overridden by the task's
   * prescribed runtime interval.
   * @param interval The time, in minutes, between invocations of this
   * task.  If interval is less than 1, the interval for this task
   * will not be changed.
   *
   *
   * @return true if the task was found and rescheduled
   *
   */

  public synchronized boolean rescheduleTask(String name, Date time, int interval)
  {
    scheduleHandle handle = null;
    boolean changed = false;

    /* -- */

    handle = (scheduleHandle) currentlyRunning.get(name);

    if (handle == null)
      {
	handle = (scheduleHandle) currentlyScheduled.get(name);
      }

    if (handle == null)
      {
	return false;		// couldn't find it
      }
    else
      {
	if (time != null)
	  {
	    handle.startTime = time;
	    
	    if (time.getTime() < nextAction.getTime())
	      {
		changed = true;
		nextAction.setTime(time.getTime());
		notify();	// let the scheduler know about our newly scheduled event
	      }
	  }

	if (interval > 0)
	  {
	    changed = true;
	    handle.setInterval(interval);
	  }

	if (changed)
	  {
	    updateTaskInfo(true);
	  }

	return changed;
      }
  }

  /**
   *
   * This method is responsible for carrying out the scheduling
   * work of this class.
   *
   * The basic logic is to wait until the next action is due to run,
   * move the task from our scheduled list to our running list, and
   * run it.  When the task completes, it will call our reschedule()
   * method if necessary
   *
   */

  public synchronized void run()
  {
    long currentTime, sleepTime;
    scheduleHandle handle;

    /* -- */

    try
      {
	System.err.println("Ganymede Scheduler: scheduling task started");

	while (true)
	  {
	    if (debug)
	      {
		System.err.println("loop");
	      }

	    if (nextAction == null)
	      {
		try
		  {
		    if (debug)
		      {
			System.err.println("*** snooze");
		      }

		    wait();
		  }
		catch (InterruptedException ex)
		  {
		    System.err.println("Scheduler interrupted");
		    return;	// jump to finally, then return
		  }

		if (debug)
		  {
		    System.err.println("*** snort?");
		  }
	      }
	    else
	      {
		currentTime = System.currentTimeMillis();

		if (currentTime < nextAction.getTime())
		  {
		    sleepTime = nextAction.getTime() - currentTime;

		    if (sleepTime > 0)
		      {
			try
			  {
			    if (debug)
			      {
				System.err.println("*** snooze");
			      }

			    wait(sleepTime);
			  }
			catch (InterruptedException ex)
			  {
			    System.err.println("Scheduler interrupted");
			    return; // jump to finally, then return
			  }

			if (debug)
			  {
			    System.err.println("*** snort?");
			  }
		      }
		  }
		else
		  {
		    if (debug)
		      {
			System.err.println("XX: Next action was scheduled at " + nextAction);
			System.err.println("XX: Processing current actions");
		      }
		  }

		currentTime = System.currentTimeMillis();
		
		if (currentTime >= nextAction.getTime())
		  {
		    Vector toRun = new Vector();
		    Date nextRun = null;
		    Enumeration enum;
		    
		    enum = currentlyScheduled.elements();

		    while (enum.hasMoreElements())
		      {
			handle = (scheduleHandle) enum.nextElement();
			
			if (handle.startTime.getTime() <= currentTime)
			  {
			    toRun.addElement(handle);
			  }
			else
			  {
			    if (nextRun == null)
			      {
				nextRun = new Date(handle.startTime.getTime());
			      }
			    else if (handle.startTime.before(nextRun))
			      {
				nextRun.setTime(handle.startTime.getTime());
			      }
			  }
		      }

		    nextAction = nextRun;

		    for (int i = 0; i < toRun.size(); i++)
		      {
			handle = (scheduleHandle) toRun.elementAt(i);

			runTask(handle);
		      }
		  }
	      }
	  }
      }
    finally 
      {
	System.err.println("Ganymede Scheduler going down");
	cleanUp();
	System.err.println("Ganymede Scheduler exited");
      }
  }

  /**
   *
   * This private method is used by the GanymedeScheduler thread's main
   * loop to put a task in the scheduled hash onto the run hash
   *
   */

  private synchronized void runTask(scheduleHandle handle)
  {
    if ((currentlyScheduled.remove(handle.name) != null) ||
	(onDemand.remove(handle.name) != null))
      {
	System.err.println("Ganymede Scheduler: running " + handle.name);

	currentlyRunning.put(handle.name, handle);
	handle.runTask();
	updateTaskInfo(true);
      }
  }

  /**
   *
   * This method is used by instances of scheduleHandle to let the
   * GanymedeScheduler thread know when their tasks have run to
   * completion.  This method is responsible for rescheduling
   * the task if it is a periodic task.
   *
   */

  synchronized void notifyCompletion(scheduleHandle handle)
  {
    if (currentlyRunning.remove(handle.name) != null)
      {
	System.err.println("Ganymede Scheduler: " + handle.name + " completed");

	// we need to check to see if the task was ordinarily scheduled to
	// start at some time in the future to handle the case where a
	// console forced us to run a task early.. if the task wasn't
	// yet due to run, we don't want to make it skip its normally
	// scheduled next run

	if (handle.startTime != null && handle.startTime.after(new Date()))
	  {
	    scheduleTask(handle);
	  }
	else
	  {
	    if (handle.reschedule())
	      {
		System.err.println("Ganymede Scheduler: rescheduling task " + handle.name + " for " + handle.startTime);
		
		scheduleTask(handle);
	      }
	    else if (handle.startTime == null)
	      {
		if (handle.runAgain())
		  {
		    runTask(handle);
		  }
		else
		  {
		    onDemand.put(handle.name, handle); // put it back on the onDemand track
		  }
	      }
	  }

	updateTaskInfo(true);
      }
    else
      {
	System.err.println("Ganymede Scheduler: confusion! Couldn't find task " + handle.name + " on the runnng list");
      }
  }

  /**
   *
   * This private method takes a task that needs to be scheduled and
   * adds it to the scheduler.
   *
   */

  private synchronized void scheduleTask(scheduleHandle handle)
  {
    currentlyScheduled.put(handle.name, handle);

    if (debug)
      {
	System.err.println("Ganymede Scheduler: scheduled task " + handle.name + 
			   " for initial execution at " + handle.startTime);
      }

    if (nextAction == null)
      {
	nextAction = new Date(handle.startTime.getTime());
	notify();
	return;
      }

    if (handle.startTime.getTime() < nextAction.getTime())
      {
	nextAction.setTime(handle.startTime.getTime());
	notify();	// let the scheduler know about our newly scheduled event
      }
  }

  /**
   *
   * This method is run when the GanymedeScheduler thread is
   * terminated.  It kills off any background processes currently
   * running.  Those threads should have a finally clause that can
   * handle abrupt termination.
   * 
   */

  private synchronized void cleanUp()
  {
    scheduleHandle handle;
    Enumeration enum;

    /* -- */

    enum = currentlyRunning.elements();

    while (enum.hasMoreElements())
      {
	handle = (scheduleHandle) enum.nextElement();

	handle.stop();
      }
  }

  /**
   *
   * This method is used to report to the Ganymede server (and thence
   * the admin console(s) the status of background tasks scheduled
   * and/or running.
   *
   */

  private synchronized void updateTaskInfo(boolean updateConsoles)
  {
    Enumeration enum;

    /* -- */

    if (reportTasks)
      {    
	taskList.setSize(0);

	enum = currentlyScheduled.elements();
	while (enum.hasMoreElements())
	  {
	    taskList.addElement(enum.nextElement());
	  }
    
	enum = currentlyRunning.elements();
	while (enum.hasMoreElements())
	  {
	    taskList.addElement(enum.nextElement());
	  }

	enum = onDemand.elements();
	while (enum.hasMoreElements())
	  {
	    taskList.addElement(enum.nextElement());
	  }

	taskListInitialized = true;

	if (updateConsoles)
	  {
	    GanymedeAdmin.refreshTasks();
	  }
      }
  }

  synchronized Vector reportTaskInfo()
  {
    if (!taskListInitialized)
      {
	updateTaskInfo(false);
      }

    return taskList;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      sampleTask

------------------------------------------------------------------------------*/

class sampleTask implements Runnable {

  String name;

  /* -- */

  public sampleTask(String name)
  {
    this.name = name;
  }

  public synchronized void run()
  {
    System.err.println(name + " reporting in: " + new Date());

    try
      {
	wait(1000);
      }
    catch (InterruptedException ex)
      {
	return;
      }
  }
}
