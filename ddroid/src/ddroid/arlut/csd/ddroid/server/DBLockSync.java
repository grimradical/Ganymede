/*
   GASH 2

   DBLockSync.java

   The GANYMEDE object storage system.

   Created: 9 February 2000
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

package arlut.csd.ddroid.server;

import arlut.csd.ddroid.common.*;
import arlut.csd.ddroid.rmi.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DBLockSync

------------------------------------------------------------------------------*/

/**
 * <P>This class acts as a general synchronization object for
 * interlock coordination.  All global data required for coordinating
 * lock activity is stored here.</P>
 *
 * <p>Note that much code in the various {@link arlut.csd.ddroid.server.DBLock}
 * subclasses, and in the rest of the Ganymede server, establishes external
 * synchronization on the DBLockSync object referenced in
 * {@link arlut.csd.ddroid.server.DBStore#lockSync DBStore.lockSync}, so certain
 * methods in this class which do not appear synchronized may in fact be
 * dependent on external synchronization.</p>
 */

public class DBLockSync {

  /** 
   * <P>Identifier keys for current {@link arlut.csd.ddroid.server.DBLock
   * DBLocks}.</P>
   *
   * <P>This hash is used by the establish() method in various DBLock
   * subclasses to guarantee that only one lock will established by
   * a client at a time, to prevent any possibility of DBLock deadlock.</P>
   *
   * <P>The values in this hash may either be scalar DBLock objects, or
   * in the case of readers (where it is permissible for a single client
   * to have several distinct reader locks), a Vector of DBLocks.</P>
   */

  private Hashtable lockHash;

  /**
   * A count of how many {@link arlut.csd.ddroid.server.DBLock DBLocks} are
   * established on {@link arlut.csd.ddroid.server.DBObjectBase DBObjectBases}
   * in this DBStore.
   */

  private int locksHeld = 0;

  /* -- */

  public DBLockSync()
  {
    resetLockHash(0);
  }

  /**
   * <P>This method causes the DBLockSync object's lock owner hashtable
   * to be reset.  If count is not zero, that value will be used to set
   * the initial capacity for the hashtable.</P>
   */

  public synchronized void resetLockHash(int count)
  {
    if (count <= 0)
      {
	lockHash = new Hashtable(20); // default value
      }
    else
      {
	lockHash = new Hashtable(count);
      }

    locksHeld = 0;
  }

  /** 
   * <P>This method returns true if there is a lock held in care of
   * the given identifier in this DBLockSync object.</P> 
   */

  public boolean isLockHeld(Object key)
  {
    return lockHash.containsKey(key);
  }

  /**
   * <P>This method returns true if the lock associated with
   * key in the DBLockSync lockHash is a DBReadLock.</P>
   *
   * <P>If there is no lock associated with the key, or if
   * the lock associated with the key is not a read lock,
   * false will be returned.</P>
   */

  public synchronized boolean isReadLock(Object key)
  {
    Object result = lockHash.get(key);

    if (result == null || !(result instanceof Vector))
      {
	return false;
      }

    return true;
  }

  /**
   * <P>This method returns true if the lock associated with
   * key in the DBLockSync lockHash is a DBDumpLock.</P>
   *
   * <P>If there is no lock associated with the key, or if
   * the lock associated with the key is not a dump lock,
   * false will be returned.</P>
   */

  public synchronized boolean isDumpLock(Object key)
  {
    Object result = lockHash.get(key);

    if (result == null || !(result instanceof DBDumpLock))
      {
	return false;
      }

    return true;
  }

  /**
   * <P>This method returns true if the lock associated with
   * key in the DBLockSync lockHash is a DBWriteLock.</P>
   *
   * <P>If there is no lock associated with the key, or if
   * the lock associated with the key is not a write lock,
   * false will be returned.</P>
   */

  public synchronized boolean isWriteLock(Object key)
  {
    Object result = lockHash.get(key);

    if (result == null || !(result instanceof DBWriteLock))
      {
	return false;
      }

    return true;
  }

  /**
   * <P>This method associates a write lock with the given key.</P>
   */

  public void setWriteLockHeld(Object key, DBWriteLock lock)
  {
    if (lockHash.containsKey(key))
      {
	throw new IllegalStateException("There is already a lock associated with key " + key);
      }

    lockHash.put(key, lock);
  }

  /**
   * <P>This method associates a dump lock with the given key.</P>
   */

  public void setDumpLockHeld(Object key, DBDumpLock lock)
  {
    if (lockHash.containsKey(key))
      {
	throw new IllegalStateException("There is already a lock associated with key " + key);
      }

    lockHash.put(key, lock);
  }

  /**
   * <P>This method associates a new DBReadLock with the given
   * key, if possible.  Multiple read locks may be associated
   * with a single key in DBLockSync, but not if there is a
   * write lock or dump lock associated with the key.</P>
   *
   * <P>If there is already a dump or write lock associated with
   * the key, an IllegalStateException will be thrown.</P>
   */

  public synchronized void addReadLock(Object key, DBReadLock lock)
  {
    Object obj = lockHash.get(key);

    if (obj != null && !(obj instanceof Vector))
      {
	throw new IllegalStateException("Error, can't add a read lock while there is a " + obj + 
					" associated with key " + key);
      }

    Vector lockList = (Vector) obj;

    if (lockList == null)
      {
	lockList = new Vector();
	lockHash.put(key, lockList);
      }

    lockList.addElement(lock);
  }

  /**
   * <P>This method disassociates a DBReadLock from the given
   * key, if possible.</P>
   *
   * <P>If there are no read locks associated with the given
   * key, an IllegalStateException will be thrown.</P>
   */

  public synchronized void delReadLock(Object key, DBReadLock lock)
  {
    Object obj = lockHash.get(key);

    if (obj != null && !(obj instanceof Vector))
      {
	throw new IllegalStateException("Error, can't remove a read lock while there is a " + 
					obj + 
					" associated with key " + key + 
					".. there are no readlocks here.");
      }

    if (obj == null)
      {
	throw new IllegalStateException("Error, can't remove a read lock for key " +
					key + ".. there are no readlocks here.");
      }

    Vector lockList = (Vector) obj;

    lockList.removeElement(lock);

    if (lockList.size() == 0)
      {
	lockHash.remove(key);	// that was the last read lock on this key
      }
  }

  /**
   * <P>This method returns a Vector of DBReadLock objects associated
   * with key, if any.  If there is no DBReadLock vector associated
   * with the key, an IllegalStateException will be thrown.</P>
   *
   * <P>The Vector returned is part of DBLockSync's internal
   * data structures, and should only be browsed in a
   * block synchronized on this DBLockSync object.</P>
   *
   * <P>The Vector returned should not be modified by external
   * code.</P>
   */

  public synchronized Vector getReadLockVector(Object key)
  {
    Object o = lockHash.get(key);

    if (o == null)
      {
	return null;
      }

    if (o instanceof Vector)
      {
	return (Vector) o;
      }

    throw new IllegalStateException("DBLockSync does not contain a readlock vector for key " + key);
  }

  /**
   * <P>This method returns a DBLock associated with the
   * given key, if any.</P> 
   *
   * <P>This method will only ever return a DBWriteLock or
   * a DBDumpLock.  If the key is associated with a Vector
   * of DBReadLocks, null will be returned.</P>
   */

  public synchronized DBLock getLockHeld(Object key)
  {
    Object obj = lockHash.get(key);

    if (obj == null || (obj instanceof DBLock))
      {
	return (DBLock) obj;
      }

    return null; // Can't return a single lock, this key has a readlock vector.
  }

  /**
   * <P>This method clears out a lock associated with the given key.</P>
   */

  public void clearLockHeld(Object key)
  {
    lockHash.remove(key);
  }

  /**
   * <P>Increments the count of held locks for the admin consoles.</P>
   */

  public synchronized void addLock()
  {
    locksHeld++;
    GanymedeAdmin.updateLocksHeld();
  }

  /**
   * <P>Decrements the count of held locks for the admin consoles.</P>
   */

  public synchronized void removeLock()
  {
    locksHeld--;
    GanymedeAdmin.updateLocksHeld();

    if (locksHeld < 0)
      {
	throw new RuntimeException("Locks held has gone negative"); 
      }
  }

  /**
   * <P>Returns the number of locks presently held in the database.</P>
   */

  public int getLockCount()
  {
    return locksHeld;
  }
}
