/*
   GASH 2

   DBNameSpace.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision$
   Last Mod Date: $Date$
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System

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

package arlut.csd.ganymede.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import arlut.csd.Util.NamedStack;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.rmi.NameSpace;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     DBNameSpace

------------------------------------------------------------------------------*/

/**
 * <p>DBNameSpaces are the objects used to manage unique value
 * tracking in {@link arlut.csd.ganymede.server.DBField DBFields} that are
 * unique value constrained.  DBNameSpace is smart enough to
 * coordinate unique value allocation and management during object
 * editing across concurrent transactions.</p>
 *
 * <p>In general, transactions in Ganymede are not able to affect each other
 * at all, save through the acquisition of exclusive editing locks on
 * invidivual objects, and through the atomic acquisition of values
 * for unique value constrained DBFields.  Once a transaction allocates
 * a unique value using either the {@link arlut.csd.ganymede.server.DBNameSpace#mark(arlut.csd.ganymede.server.DBEditSet,java.lang.Object,arlut.csd.ganymede.server.DBField) mark()},
 * {@link arlut.csd.ganymede.server.DBNameSpace#unmark(arlut.csd.ganymede.server.DBEditSet,java.lang.Object) unmark()},
 * or {@link arlut.csd.ganymede.server.DBNameSpace#reserve(arlut.csd.ganymede.server.DBEditSet,java.lang.Object) reserve()}
 * methods, no other transaction can allocate that value, until the first transaction
 * calls the {@link arlut.csd.ganymede.server.DBNameSpace#commit(arlut.csd.ganymede.server.DBEditSet) commit()},
 * {@link arlut.csd.ganymede.server.DBNameSpace#abort(arlut.csd.ganymede.server.DBEditSet) abort()},
 * or {@link arlut.csd.ganymede.server.DBNameSpace#rollback(arlut.csd.ganymede.server.DBEditSet,java.lang.String) rollback()}
 * methods.</p>
 *
 * <p>In order to perform this unique value management, DBNameSpace maintains
 * a private Hashtable, {@link arlut.csd.ganymede.server.DBNameSpace#uniqueHash uniqueHash},
 * that associates the allocated vales in the namespace with {@link arlut.csd.ganymede.server.DBNameSpaceHandle DBNameSpaceHandle}
 * objects which track the transaction that is manipulating the value, if any, as well
 * as the DBField object in the database that is checked in with that value.  The
 * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession} query logic takes
 * advantage of this to do optimized, hashed look-ups of values for unique value
 * constrained fields to locate objects in the database rather than having to iterate
 * over all objects of a given type to find a particular match.</p>
 *
 * <p>DBNameSpaces may be defined in the server's schema editor to be either case sensitive
 * or case insensitive.  The DBNameSpace class uses the {@link arlut.csd.ganymede.server.GHashtable GHashtable}
 * class to handle the representational issues in the unique value hash for this, as well as
 * for things like IP address representation.</p>
 */

public final class DBNameSpace implements NameSpace {

  static final boolean debug = false;

  /**
   * <p>The number of simultaneous transactions in progress
   * that we will size the transactions hash for initially.</p>
   */

  static final int TRANSCOUNT = 30;

  /**
   * <p>treat differently-cased Strings as the same for key?</p>
   */

  private boolean caseInsensitive;

  /**
   * <p>the name of this namespace</p>
   */

  private String name;

  /**
   * <p>Hashtable mapping values allocated (permanently, for objects checked in to the database, or
   * temporarily, for objects being manipulated by active transactions) in this namespace to
   * {@link arlut.csd.ganymede.server.DBNameSpaceHandle DBNameSpaceHandle} objects that track
   * the current status of the values.</p>
   */

  private Hashtable uniqueHash;

  /**
   * <p>During schema editing, we keep a copy of the uniqueHash that we had
   * when schema edited started.  If fields are detached or attached to this
   * namespace during schema editing, we will make the appropriate changes
   * to the uniqueHash.  If the schema edit is committed, uniqueHash is kept
   * and saveHash is cleared.  If the schema edit is cancelled, uniqueHash
   * is set back to saveHash and the saveHash reference is cleared.  saveHash
   * will always be null except during schema editing.</p>
   */

  private Hashtable saveHash = null;

  /**
   * <p>Hashtable mapping {@link arlut.csd.ganymede.server.DBEditSet
   * DBEditSet's} currently active modifying values in this namespace
   * to {@link arlut.csd.ganymede.DBNameSpaceTransaction
   * DBNameSpaceTransaction} objects.</p>
   */

  private Hashtable transactions;

  /* -- */

  // constructors

  /**
   *
   * Create a new DBNameSpace object from a stream definition.
   *
   */

  public DBNameSpace(DataInput in) throws IOException, RemoteException
  {
    receive(in);
    uniqueHash = new GHashtable(caseInsensitive); // size?
    transactions = new Hashtable(TRANSCOUNT);

    Ganymede.rmi.publishObject(this);
  }

  /**
   *
   * Constructor for new DBNameSpace for DBStore initialization.
   *
   * @param name Name for this name space
   * @param caseInsensitive If true, case is disregarded in this namespace
   */

  public DBNameSpace(String name, boolean caseInsensitive) throws RemoteException
  {
    this.name = name;
    this.caseInsensitive = caseInsensitive;
    uniqueHash = new GHashtable(caseInsensitive); // size?
    transactions = new Hashtable(TRANSCOUNT);

    Ganymede.rmi.publishObject(this);
  }

  /**
   *
   * Read in a namespace definition from a DataInput stream.
   *
   */

  public void receive(DataInput in) throws IOException
  {
    name = in.readUTF();
    caseInsensitive = in.readBoolean();
  }

  /**
   *
   * Write out a namespace definition to a DataOutput stream.
   *
   */

  public void emit(DataOutput out) throws IOException
  {
    out.writeUTF(name);
    out.writeBoolean(caseInsensitive);
  }

  /**
   * <P>Write out an XML entity for this namespace.</P>
   */

  public void emitXML(XMLDumpContext xDump) throws IOException
  {
    xDump.startElementIndent("namespace");
    xDump.attribute("name", getName());

    if (!caseInsensitive)
      {
	xDump.attribute("case-sensitive", "true");
      }

    xDump.endElement("namespace");
  }

  /**
   *
   * Returns the name of this namespace.
   *
   * @see arlut.csd.ganymede.rmi.NameSpace
   */

  public String getName()
  {
    return name;
  }

  /**
   *
   * Sets the name of this namespace.  Returns false
   * if the name is already taken by another namespace
   *
   * @see arlut.csd.ganymede.rmi.NameSpace
   */

  public boolean setName(String newName)
  {
    // need to make sure this new name isn't in conflict
    // with existing names

    name = newName;
    return true;
  }

  /**
   *
   * Returns true if case is to be disregarded in comparing
   * entries in namespace managed fields.
   *
   * @see arlut.csd.ganymede.rmi.NameSpace
   */

  public boolean isCaseInsensitive()
  {
    return caseInsensitive;
  }

  /**
   *
   * Turns case sensitivity on/off.  If b is true, case will be
   * disregarded in comparing entries in namespace managed fields.
   *
   * @see arlut.csd.ganymede.rmi.NameSpace 
   */

  public void setInsensitive(boolean b)
  {
    if (b == caseInsensitive)
      {
	return;
      }

    // if we are changing our case sensitivity, we need to construct
    // a new GHashtable of the appropriate case sensitivity and copy
    // over the old hashtable entries.. this loop should check for
    // membership before inserting a new value in the case that
    // we are changing from case insensitivity to case sensitivity

    // right now, of course, we do none of this

    caseInsensitive = b;
  }

  /**
   * <p>This method returns the {@link arlut.csd.ganymede.DBNameSpaceTransaction DBNameSpaceTransaction}
   * associated with the given transaction, creating one if one was not previously
   * so associated.</p>
   *
   * <p>This method will always return a valid DBNameSpaceTransaction record.</p>
   */

  public synchronized DBNameSpaceTransaction getTransactionRecord(DBEditSet transaction)
  {
    DBNameSpaceTransaction transRecord = (DBNameSpaceTransaction) transactions.get(transaction);

    if (transRecord == null)
      {
	transRecord = new DBNameSpaceTransaction(transaction);
	transactions.put(transaction, transRecord);
      }

    return transRecord;
  }

  /**
   * <p>Returns true if this namespace has value allocated.</p>
   */

  public boolean containsKey(Object value)
  {
    return uniqueHash.containsKey(value);
  }

  /**
   * <p>Returns the DBNameSpaceHandle associated with the value
   * in this namespace, or null if the value is not allocated in
   * this namespace.</p>
   */

  public DBNameSpaceHandle getHandle(Object value)
  {
    return (DBNameSpaceHandle) uniqueHash.get(value);
  }

  /**
   * <p>Publicly accessible function used to associate the given
   * {@link arlut.csd.ganymede.server.DBNameSpaceHandle DBNameSpaceHandle}
   * with a value in this namespace in a non-transactional fashion.</p>
   *
   * <p>Used by the {@link arlut.csd.ganymede.server.DBObject DBObject}
   * receive() method and the {@link arlut.csd.ganymede.server.DBJournal
   * DBJournal}'s {@link arlut.csd.ganymede.server.JournalEntry JournalEntry}
   * class' process() method to build up the namespace during server
   * start-up.</p>
   */

  public void putHandle(Object value, DBNameSpaceHandle handle)
  {
    uniqueHash.put(value, handle);
  }

  /**
   * <p>Publicly accessible function used to clear the given
   * value from this namespace in a non-transactional fashion.</p>
   *
   * <p>Used by {@link arlut.csd.ganymede.server.DBJournal DBJournal}'s
   * {@link arlut.csd.ganymede.server.JournalEntry JournalEntry} class'
   * process() method to rectify the namespace during server
   * start-up.</p>
   */
  
  public void clearHandle(Object value)
  {
    uniqueHash.remove(value);
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                        lookup()

  ----------------------------------------------------------------------------*/

  /**
   *
   * <p>This method allows the namespace to be used as a unique valued 
   * search index.</p>
   *
   * <p>Note that this lookup only works for precise equality lookup.. i.e., 
   * strings must be the same capitalization and the whole works.</p>
   *
   * <p>As well, this method is really probably useful in the context of
   * a DBReadLock, but we're not doing anything to enforce this requirement 
   * at this point.</p>
   *
   * @param value The value to search for in the namespace hash.
   *
   */

  public synchronized DBField lookup(Object value)
  {
    DBNameSpaceHandle _handle;

    /* -- */

    _handle = (DBNameSpaceHandle) uniqueHash.get(value);

    if (_handle == null)
      {
	return null;
      }

    return _handle.getField(Ganymede.internalSession);
  }

  /**
   *
   * <p>This method allows the namespace to be used as a unique valued 
   * search index.</p>
   *
   * <p>Note that this lookup only works for precise equality lookup.. i.e., 
   * strings must be the same capitalization and the whole works.</p>
   *
   * <p>As well, this method is really probably useful in the context of
   * a DBReadLock, but we're not doing anything to enforce this requirement 
   * at this point.</p>
   *
   * @param session The GanymedeSession to use to lookup the containing object..
   * useful when a GanymedeSession is doing the looking up of value
   * @param value The value to search for in the namespace hash.
   *
   */

  public synchronized DBField lookup(GanymedeSession session, Object value)
  {
    DBNameSpaceHandle _handle;

    /* -- */

    _handle = (DBNameSpaceHandle) uniqueHash.get(value);

    if (_handle == null)
      {
	return null;
      }

    return _handle.getField(session);
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                      testmark()

  ----------------------------------------------------------------------------*/

  /**
   *
   * <p>This method tests to see whether a value in the namespace can
   * be marked as in use.  Such a marking is done to allow an editset
   * to have the ability to juggle values in namespace associated
   * fields without allowing another editset to acquire a value
   * needed if the editset transaction is aborted.</p>
   *
   * <p>For array db fields, all elements in the array should be
   * testmark'ed in the context of a synchronized block on the
   * namespace before going back and marking each value (while still
   * in the synchronized block on the * namespace).. this ensures
   * that we won't mark several values in an array before discovering
   * that one of the values in a DBArrayField is already taken.</p>
   *
   * @param editSet The transaction testing permission to claim value.
   * @param value The unique value desired by editSet.
   * 
   */

  public synchronized boolean testmark(DBEditSet editSet, Object value)
  {
    if (this.saveHash != null)
      {
	throw new RuntimeException("still in schema edit");
      }

    DBNameSpaceHandle handle;

    /* -- */

    if (uniqueHash.containsKey(value))
      {
	handle = (DBNameSpaceHandle) uniqueHash.get(value);

	if (handle.owner != editSet)
	  {
	    return false;	// we don't owns it
	  }
	else
	  {
	    return !handle.inuse;
	  }
      }
    
    return true;
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                          mark()

  ----------------------------------------------------------------------------*/

  /**
   *
   * <p>This method marks a value as being used in this namespace.  Marked
   * values are held in editSet's, which are free to shuffle these
   * reserved values around during processing.  The inuse and original
   * fields in the namespace object are used during unique value searches
   * to do direct hash lookups without getting confused by any shuffling
   * being performed by a current thread.</p>
   *
   * @param editset The transaction claiming the unique value <value>
   * @param value The unique value that transaction editset is attempting to claim
   * @param field The DBField which will take the unique value.  Used to provide an index.
   *  
   */

  public synchronized boolean mark(DBEditSet editSet, Object value, DBField field)
  {
    if (this.saveHash != null)
      {
	throw new RuntimeException("still in schema edit");
      }

    DBNameSpaceHandle handle;
    
    /* -- */

    if (debug)
      {
	System.err.println(editSet.getSession().getKey() + ": DBNameSpace.mark(): enter");
      }

    if (this.saveHash != null)
      {
	throw new RuntimeException("still in schema edit");
      }

    if (uniqueHash.containsKey(value))
      {
	if (debug)
	  {
	    System.err.println(editSet.session.key + ": DBNameSpace.mark(): uniqueHash contains key " + value);
	  }
	
	handle = (DBNameSpaceHandle) uniqueHash.get(value);

	if (handle.owner != editSet)
	  {
	    // either someone else is manipulating this value, or an object
	    // stored in the database holds this value.  We would need to
	    // pull that object out of the database and unmark the value on
	    // that object before we could mark that value someplace else.

	    if (debug)
	      {
		System.err.println(editSet.session.key + ": DBNameSpace.mark(): we don't own handle");
	      }

	    return false;	// somebody else owns it
	  }
	else
	  {
	    // we own it

	    // if this namespace value is still being used in the
	    // namespace, we can't mark this value.  they need to
	    // unmark the value in one place before they can mark it
	    // in another.

	    if (handle.inuse)
	      {
		return false;
	      }

	    handle.inuse = true;
	    handle.shadowField = field;

	    // we don't have to have the transaction record remember
	    // the value since we should already have this value noted
	    // in the editset
	  }
      }
    else
      {
	if (debug)
	  {
	    System.err.println(editSet.session.key + ": DBNameSpace.mark(): value not in uniqueHash");
	  }

	// we're creating a new value.. previous value
	// is false

	handle = new DBNameSpaceHandle(editSet, false, null);
	handle.inuse = true;
	handle.shadowField = field;
	
	uniqueHash.put(value, handle);

	remember(editSet, value);
      }

    if (debug)
      {
	System.err.println(editSet.session.key + ": DBNameSpace.mark(): mark obtained succesfully");
      }

    return true;
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                       reserve()

  ----------------------------------------------------------------------------*/

  /**
   * <p>This method reserves a value so that the given editSet is
   * assured of being able to use this value at some point before the
   * transaction is commited or canceled.  reserve() is different from
   * mark() in that there is no field specified to be holding the
   * value, and that when the transaction is committed or canceled,
   * the value will be returned to the available list.  During the
   * transaction, the transaction code can mark the value at any time
   * with assurance that they will be able to do so.</p>
   *
   * <p>If a transaction attempts to reserve() a
   * value that is already being held by an object in the transaction,
   * reserve() will return true, even though a subsequent mark()
   * attempt would fail.</p>
   *
   * @param editset The transaction claiming the unique value <value>
   * @param value The unique value that transaction editset is attempting to claim
   *
   * @return true if the value could be reserved in the given editSet.  
   */

  public boolean reserve(DBEditSet editSet, Object value)
  {
    return reserve(editSet, value, false);
  }

  /**
   * <p>This method reserves a value so that the given editSet is
   * assured of being able to use this value at some point before the
   * transaction is commited or canceled.  reserve() is different from
   * mark() in that there is no field specified to be holding the
   * value, and that when the transaction is committed or canceled,
   * the value will be returned to the available list.  During the
   * transaction, the transaction code can mark the value at any time
   * with assurance that they will be able to do so.</p>
   *
   * <p>If onlyUsed is false and a transaction attempts to reserve() a
   * value that is already being held by an object in the transaction,
   * reserve() will return true, even though a subsequent mark()
   * attempt would fail.</p>
   *
   * @param editset The transaction claiming the unique value <value>
   * @param value The unique value that transaction editset is attempting to claim
   * @param onlyUnused If true, reserve() will return false if the value is already
   * attached to a field connected to this namespace, even if in an object attached
   * to the editSet provided.
   *
   * @return true if the value could be reserved in the given editSet.  
   */

  public synchronized boolean reserve(DBEditSet editSet, Object value, boolean onlyUnused)
  {
    if (this.saveHash != null)
      {
	throw new RuntimeException("still in schema edit");
      }

    DBNameSpaceHandle handle;
    
    /* -- */

    if (debug)
      {
	System.err.println(editSet.getSession().getKey() +
			   ": DBNameSpace.reserve(): enter");
      }

    // Is this value already taken?

    if (uniqueHash.containsKey(value))
      {
	if (debug)
	  {
	    System.err.println(editSet.session.key +
			       ": DBNameSpace.reserve(): uniqueHash contains key " + 
			       value);
	  }
	
	handle = (DBNameSpaceHandle) uniqueHash.get(value);

	if (handle.owner != editSet)
	  {
	    // either someone else is manipulating this value, or an object
	    // stored in the database holds this value.  We would need to
	    // pull that object out of the database and unmark the value on
	    // that object before we could mark that value someplace else.

	    if (debug)
	      {
		System.err.println(editSet.session.key +
				   ": DBNameSpace.reserve(): we don't own handle");
	      }

	    return false;	// somebody else owns it
	  }
	else
	  {
	    // we own it.. it may or may not be in use in an object
	    // already, but it's ours, at least

	    if (onlyUnused && handle.inuse)
	      {
		return false;
	      }

	    return true;
	  }
      }
    else
      {
	if (debug)
	  {
	    System.err.println(editSet.session.key +
			       ": DBNameSpace.reserve(): value not in uniqueHash");
	  }

	// we're creating a new value, the current value isn't held in the namespace

	handle = new DBNameSpaceHandle(editSet, false, null);

	// we're reserving it now, but it's not actually in use yet.

	handle.inuse = false;
	handle.shadowField = null;
	
	uniqueHash.put(value, handle);

	remember(editSet, value);
      }

    if (debug)
      {
	System.err.println(editSet.session.key +
			   ": DBNameSpace.reserve(): mark obtained succesfully");
      }

    return true;
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                    testunmark()

  ----------------------------------------------------------------------------*/

  /**
   *
   *  <p>This method tests to see whether a value in the namespace can
   *  be marked as not in use.  Such a marking is done to allow an
   *  editset to have the ability to juggle values in namespace
   *  associated fields without allowing another editset to acquire a
   *  value needed if the editset transaction is aborted.</p>
   *
   *  <p>For array db fields, all elements in the array should be
   *  testunmark'ed in the context of a synchronized block on the
   *  namespace before actually unmarking all the values.  See the
   *  comments in testmark() for the logic here.  Note that
   *  testunmark() is less useful than testmark() because we really
   *  aren't expecting anything to prevent us from unmarking()
   *  something.</p>
   *
   * @param editSet The transaction that is determining whether value can be freed.
   * @param value The unique value being tested.
   *
   */

  public synchronized boolean testunmark(DBEditSet editSet, Object value)
  {
    if (this.saveHash != null)
      {
	throw new RuntimeException("still in schema edit");
      }

    DBNameSpaceHandle handle;

    /* -- */

    if (uniqueHash.containsKey(value))
      {
	handle = (DBNameSpaceHandle) uniqueHash.get(value);

	if (handle.owner == null)
	  {
	    return true;
	  }
	else
	  {
	    if (handle.owner != editSet)
	      {
		return false;	// somebody else owns it
	      }
	    else
	      {
		return handle.inuse;
	      }
	  }
      }

    return false;		// if the value isn't already in the name
				// space, it doesn't make sense for us to
				// be unmarking it
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                        unmark()

  ----------------------------------------------------------------------------*/

  /**
   *
   * <p>Used to mark a value as not used in the namespace.  Unmarked
   * values are not available for other threads / editset's until
   * commit is called on this namespace on behalf of this editset.</p>
   *
   * @param editSet The transaction that is freeing value.
   * @param value The unique value being tentatively marked as unused.
   * 
   */

  public synchronized boolean unmark(DBEditSet editSet, Object value)
  {
    if (this.saveHash != null)
      {
	throw new RuntimeException("still in schema edit");
      }

    DBNameSpaceHandle handle;
    
    /* -- */

    if (uniqueHash.containsKey(value))
      {
	handle = (DBNameSpaceHandle) uniqueHash.get(value);

	if (handle.owner == null)
	  {
	    // no one has claimed this, give it to the editSet.
	    // Note that we are assuming here that the editset
	    // has properly checked out the object containing
	    // this value.  The namespace code does not check
	    // to make sure that the editset really has the
	    // right to acquire this value.  The DBEditObject
	    // code should be set up to always do the right
	    // thing.  Probably the mark methods in DBNameSpace
	    // should not be public.

	    // note that since this handle has no owner,
	    // we know that the original state should be true,
	    // else it wouldn't have been in the hash to begin
	    // with

	    handle.owner = editSet;
	    handle.original = true;
	    handle.inuse = false;
	    handle.shadowField = null;

	    remember(editSet, value);
	  }
	else
	  {
	    if (handle.owner != editSet)
	      {
		return false;	// somebody else owns it
	      }
	    else
	      {
		// we own it, but we don't want to change the original
		// value, which we will need if we abort this editset

		handle.inuse = false;
		handle.shadowField = null;
	      }
	  }
      }
    else
      {
	// we're creating a new value.. previous value
	// is false

	handle = new DBNameSpaceHandle(editSet, false, null);
	handle.inuse = true;
	handle.shadowField = null;
	uniqueHash.put(value, handle);

	remember(editSet, value);
      }

    return true;
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                    checkpoint()

  ----------------------------------------------------------------------------*/

  /**
   * <p>Method to checkpoint namespace changes made by a specific transaction
   * so that state can be rolled back if necessary at a later time.</p>
   *
   * @param editSet The transaction that needs to be checkpointed.
   * @param name The name of the checkpoint to be marked.
   */

  public synchronized void checkpoint(DBEditSet editSet, String name)
  {
    if (this.saveHash != null)
      {
	throw new RuntimeException("still in schema edit");
      }

    getTransactionRecord(editSet).pushCheckpoint(name, new DBNameSpaceCkPoint(this, editSet));
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                 popCheckpoint()

  ----------------------------------------------------------------------------*/

  /**
   *
   * <p>Method to remove a checkpoint from this namespace's DBNameSpaceCkPoint
   * hash.</p>
   *
   * <p>This method really isn't very important, because when the transaction
   * is committed or aborted, the checkpoints hashtable will be cleared of
   * editSet anyway.</p>
   *
   * @param editSet The transaction that is requesting the checkpoint pop.
   * @param name The name of the checkpoint to be popped.
   *
   */

  public synchronized void popCheckpoint(DBEditSet editSet, String name)
  {
    if (this.saveHash != null)
      {
	throw new RuntimeException("still in schema edit");
      }

    getTransactionRecord(editSet).popCheckpoint(name);
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                      rollback()

  ----------------------------------------------------------------------------*/

  /**
   *
   * <p>Method to rollback namespace changes made by a specific transaction
   * to a checkpoint.</p>
   *
   * @param editSet The transaction that needs to be checkpointed.
   * @param name The name of the checkpoint to be rolled back to.
   *
   * @return false if the checkpoint could not be found.
   *
   */

  public synchronized boolean rollback(DBEditSet editSet, String name)
  {
    if (this.saveHash != null)
      {
	throw new RuntimeException("still in schema edit");
      }

    Object value;
    Vector elementsToRemove = new Vector();
    DBNameSpaceTransaction tRecord; 
    DBNameSpaceCkPoint point;
    DBNameSpaceHandle handle;
    Enumeration en;

    /* -- */

    tRecord = getTransactionRecord(editSet);

    // try to pop off the named checkpoint we're looking for

    point = tRecord.popCheckpoint(name);

    if (point == null)
      {
	System.err.println("DBNameSpace.rollback(): couldn't find checkpoint for " + name);
	System.err.println("In transaction " + editSet.description);
	System.err.println("\nCurrently registered checkpoints:");
	System.err.println(tRecord.getCheckpointStack());
	return false;
      }

    // now we need to set about reconstituting the state of the
    // namespace for the values that were recorded at the time the
    // point was established.  Once a DBNameSpace reserves a value on
    // behalf of a transaction, it won't be forgotten no matter what
    // happens, so we don't need to worry about values that were
    // reserved in the checkpoint that are not reserved anymore; we'll
    // just iterate over the values that are currently reserved for
    // the transaction, and check them against the checkpoint we are
    // reverting to.

    en = tRecord.getReservedEnum();

    while (en.hasMoreElements())
      {
	value = en.nextElement();
	handle = (DBNameSpaceHandle) uniqueHash.get(value);

	// ok, we've got a value that is in our current reserved list.
	// Now, was it in our checkpoint?

	if (!point.containsValue(value))
	  {
	    if (handle.original)
	      {
		// remember, shadowField is just for our use while
		// we're manipulating a namespace allocation within a
		// transaction.  we're not screwing with
		// handle.fieldInvid or handle.fieldId here, which are
		// still left standing so that the namespace-optimized
		// query mechanism in GanymedeSession can track down
		// the field bound to the namespace value.

		handle.shadowField = null;
		handle.owner = null;
		handle.inuse = true;
	      }
	    else
	      {
		// the value was not reserved in the namespace before
		// this transaction allocated it, so we can just
		// forget it entirely, and allow other transactions to
		// use it.

		uniqueHash.remove(value);
	      }

	    elementsToRemove.addElement(value);
	  }
	else
	  {
	    // the checkpoint did have value reserved.  we need to
	    // revert the DBNameSpaceHandle for that value to that
	    // stored in the checkpoint.

	    uniqueHash.put(value, point.getValueHandle(value));
	  }
      }

    // now clean out the vector of values for this editset, removing
    // any values that are being freed up by this rollback.  Note
    // that we have to use the elementsToRemove vector and do the
    // forgetting in a separate loop to avoid screwing with the
    // enumeration we were using.

    for (int i = 0; i < elementsToRemove.size(); i++)
      {
	tRecord.forget(elementsToRemove.elementAt(i));
      }

    return true;
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                         abort()

  ----------------------------------------------------------------------------*/

  /**
   * <p>Method to revert an editSet's namespace modifications to its
   * original state.  Used when a transaction is rolled back.</p>
   *
   * @param editSet The transaction whose claimed values in this namespace need to be freed.
   *
   */

  public synchronized void abort(DBEditSet editSet)
  {
    if (this.saveHash != null)
      {
	throw new RuntimeException("still in schema edit");
      }

    DBNameSpaceTransaction tRecord;
    Enumeration en;
    Object value;
    DBNameSpaceHandle handle;

    /* -- */

    tRecord = getTransactionRecord(editSet);

    en = tRecord.getReservedEnum();

    // loop over the values in the namespace that were changed or affected
    // by this editset, and revert them to their checked-in status, or
    // get rid of them entirely if they weren't allocated in this namespace
    // before this transaction allocated them

    while (en.hasMoreElements())
      {
	value = en.nextElement();
	handle = getHandle(value);

	if (handle.original)
	  {
	    handle.owner = null;
	    handle.shadowField = null;
	    handle.inuse = true;
	  }
	else
	  {
	    uniqueHash.remove(value);
	  }
      }

    // we're done with this transaction

    tRecord.cleanup();

    transactions.remove(editSet);
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                        commit()
  ----------------------------------------------------------------------------*/

  /**
   * <p>Method to put the editSet's current namespace modifications into
   * final effect and to make any abandoned values available for other
   * namespaces.</p>
   *
   * <p>Note that a NameSpace should really never fail here.  We assume that
   * all NameSpace management code up to this point has functioned properly..
   * at this point, the EditSet has already committed changes to the DBStore
   * and to any external processes, we're just doing paperwork at this point.</p>
   *
   * @param editSet The transaction being committed.
   */

  public synchronized void commit(DBEditSet editSet)
  {
    if (this.saveHash != null)
      {
	throw new RuntimeException("still in schema edit");
      }

    DBNameSpaceTransaction tRecord;
    Enumeration en;
    Object value;
    DBNameSpaceHandle handle;

    /* -- */

    tRecord = getTransactionRecord(editSet);

    en = tRecord.getReservedEnum();

    // loop over the values in the namespace that were changed or
    // affected by this editset, and commit them into the database,
    // copying the shadowField DBField into the handle's permanent
    // field pointer, or getting rid of them entirely if they are not
    // in use anymore

    while (en.hasMoreElements())
      {
	value = en.nextElement();
	handle = getHandle(value);

	if (handle.inuse)
	  {
	    handle.owner = null;
	    handle.setField(handle.shadowField);
	    handle.shadowField = null;
	  }
	else
	  {
	    uniqueHash.remove(value);
	  }
      }

    // we're done with this transaction

    tRecord.cleanup();

    transactions.remove(editSet);
  }  

  /*----------------------------------------------------------------------------
                                                                          method
                                                                      remember()

  Remember that this editSet has changed the location/status of this value.

  This is a private convenience method.

  ----------------------------------------------------------------------------*/
  private void remember(DBEditSet editSet, Object value)
  {
    getTransactionRecord(editSet).remember(value);
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                   dumpNameSpace

  ----------------------------------------------------------------------------*/

  /**
   * <p>This method is just a debug instrument, it prints to stderr a list of
   * the contents of this namespace's unique value hash.</p>
   */

  private void dumpNameSpace()
  {
    Enumeration en;
    Object key;
    
    /* -- */

    en = uniqueHash.keys();

    while (en.hasMoreElements())
      {
	key = en.nextElement();
	System.err.println("key: " + key + ", value: " + uniqueHash.get(key));
      }
  }

  public String toString()
  {
    return name;
  }

  /**
   * <p>This method prepares this DBNameSpace for changes to be made during schema editing.
   * A back-up copy of the uniqueHash is made, to allow reversion in case the schema
   * edit is aborted.  Between schemaEditCheckout() and either schemaEditCommit() or
   * schemaEditAbort(), the schemaEditRegister() and schemaEditUnregister() methods
   * may be called to handle attaching/detaching fields to the namespace.</p>
   */

  public synchronized void schemaEditCheckout()
  {
    if (this.saveHash != null)
      {
	throw new RuntimeException("non-null savehash");
      }

    this.saveHash = this.uniqueHash;

    uniqueHash = new GHashtable(saveHash.size(), caseInsensitive);

    Enumeration en = saveHash.keys();

    while (en.hasMoreElements())
      {
	Object key = en.nextElement();

	DBNameSpaceHandle handle = (DBNameSpaceHandle) saveHash.get(key);

	DBNameSpaceHandle handleCopy = (DBNameSpaceHandle) handle.clone();

	// a DBNameSpace copy should only be constructed during schema
	// editing

	if (handleCopy.owner != null)
	  {
	    throw new RuntimeException("Error, non-null handle owner found during copy of namespace " + 
				       this.toString() + " for key: " + key);
	  }

	if (handleCopy.shadowField != null)
	  {
	    throw new RuntimeException("Error, non-null handle shadowField found during copy of namespace " + 
				       this.toString() + " for key: " + key);
	  }

	this.uniqueHash.put(key, handleCopy);
      }
  }

  /**
   * <p>Returns true if this namespace has already been checked out for schema editing.</p>
   */

  public synchronized boolean isSchemaEditInProgress()
  {
    return (this.saveHash != null);
  }

  /**
   * <p>This method locks in any changes made after schema editing is complete.</p>
   */

  public synchronized void schemaEditCommit()
  {
    if (this.saveHash == null)
      {
	return;
      }

    this.saveHash = null;
  }

  /**
   * <p>This method aborts any changes made during schema editing.</p>
   */

  public synchronized void schemaEditAbort()
  {
    if (this.saveHash == null)
      {
	return;
      }

    this.uniqueHash = this.saveHash;
    this.saveHash = null;
  }

  /**
   * <p>This method links the given value to the specified field.  If the
   * value has already been allocated, schemaEditRegister will return false
   * and the value won't be attached to the field in question.</p>
   *
   * @return true if the value could be registered with the specified field,
   * false otherwise
   */

  public synchronized boolean schemaEditRegister(Object value, DBField field)
  {
    if (this.saveHash == null)
      {
	throw new RuntimeException("not in schema edit");
      }

    if (uniqueHash.containsKey(value))
      {
	return false;
      }

    DBNameSpaceHandle handle = new DBNameSpaceHandle(null, true, field);

    uniqueHash.put(value, handle);

    return true;
  }

  /**
   * <p>This method unlinks a single item from this namespace index, if and only
   * if the value is associated with the object and field id specified.</p>
   *
   * @return true if the value was associated with the object and field specified,
   * false otherwise.  If false is returned, no value was removed from this
   * namespace.
   */

  public synchronized boolean schemaEditUnregister(Object value, Invid objid, short field)
  {
    if (this.saveHash == null)
      {
	throw new RuntimeException("not in schema edit");
      }

    DBNameSpaceHandle handle = (DBNameSpaceHandle) uniqueHash.get(value);

    if (handle == null)
      {
	return false;
      }

    if (!handle.matches(objid, field))
      {
	return false;
      }

    uniqueHash.remove(value);

    return true;
  }

  /**
   * <p>This method unlinks all values that are associated with the specified
   * object type and field id from this namespace.</p>
   */

  public synchronized void schemaEditUnregister(short objectType, short fieldId)
  {
    if (this.saveHash == null)
      {
	throw new RuntimeException("not in schema edit");
      }

    Vector elementsToRemove = new Vector();
    Enumeration en = this.uniqueHash.keys();

    while (en.hasMoreElements())
      {
	Object value = en.nextElement();
	DBNameSpaceHandle handle = (DBNameSpaceHandle) this.uniqueHash.get(value);

	if (handle.matches(objectType, fieldId))
	  {
	    elementsToRemove.addElement(value);
	  }
      }

    for (int i = 0; i < elementsToRemove.size(); i++)
      {
	this.uniqueHash.remove(elementsToRemove.elementAt(i));
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                          DBNameSpaceTransaction

------------------------------------------------------------------------------*/

/**
 * <P>This class holds information associated with an active transaction (a
 * {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}) in care of a 
 * {@link arlut.csd.ganymede.server.DBNameSpace DBNameSpace}.</p>
 */

class DBNameSpaceTransaction {

  private NamedStack checkpointStack;
  private Hashtable reservedValues;
  private DBEditSet transaction;

  /* -- */

  DBNameSpaceTransaction(DBEditSet transaction)
  {
    this.transaction = transaction;
    this.reservedValues = new Hashtable();
    this.checkpointStack = new NamedStack();
  }

  public synchronized void remember(Object value)
  {
    if (reservedValues.containsKey(value))
      {
	try
	  {
	    throw new RuntimeException("DBNameSpaceTransaction.remember(): transaction " + transaction +
				       " already contains value " + value);
	  }
	catch (RuntimeException ex)
	  {
	    Ganymede.debug(Ganymede.stackTrace(ex));
	  }

	return;
      }

    reservedValues.put(value, value);
  }

  public synchronized void forget(Object value)
  {
    if (!reservedValues.containsKey(value))
      {
	try
	  {
	    throw new RuntimeException("DBNameSpaceTransaction.forget(): transaction " + transaction +
				       " does not contain value " + value);
	  }
	catch (RuntimeException ex)
	  {
	    Ganymede.debug(Ganymede.stackTrace(ex));
	  }

	return;
      }
    
    reservedValues.remove(value);
  }

  /**
   * <p>This method dissolves everything referenced by this DBNameSpaceTransaction,
   * in order to facilitate speedy garbage collection.</p>
   */

  public synchronized void cleanup()
  {
    if (checkpointStack != null)
      {
	while (checkpointStack.size() != 0)
	  {
	    DBNameSpaceCkPoint ckpoint = (DBNameSpaceCkPoint) checkpointStack.pop();
	    ckpoint.cleanup();
	  }

	checkpointStack = null;
      }

    if (reservedValues != null)
      {
	reservedValues.clear();
	reservedValues = null;
      }

    transaction = null;
  }

  public Enumeration getReservedEnum()
  {
    return reservedValues.elements();
  }

  public Hashtable getReservedHash()
  {
    return reservedValues;
  }

  public DBEditSet getDBEditSet()
  {
    return transaction;
  }

  public void pushCheckpoint(String name, DBNameSpaceCkPoint cPoint)
  {
    checkpointStack.push(name, cPoint);
  }

  public DBNameSpaceCkPoint popCheckpoint(String name)
  {
    DBNameSpaceCkPoint point = (DBNameSpaceCkPoint) checkpointStack.pop(name);

    if (point == null)
      {
	try
	  {
	    throw new RuntimeException("DBNameSpaceTransaction.popCheckpoint(): transaction " + transaction +
				       " does not contain a checkpoint named " + name);
	  }
	catch (RuntimeException ex)
	  {
	    Ganymede.debug(Ganymede.stackTrace(ex));
	  }
      }

    return point;
  }

  public NamedStack getCheckpointStack()
  {
    return checkpointStack;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                              DBNameSpaceCkPoint

------------------------------------------------------------------------------*/

class DBNameSpaceCkPoint {

  Hashtable reserved;
  Hashtable uniqueHash;

  /* -- */

  DBNameSpaceCkPoint(DBNameSpace space, DBEditSet transaction)
  {
    DBNameSpaceTransaction tRecord = space.getTransactionRecord(transaction);

    reserved = tRecord.getReservedHash();

    if (reserved != null)
      {
	// clone the hash to avoid sync problems with other threads

	reserved = (Hashtable) reserved.clone();

	if (reserved.size() > 0)
	  {
	    // size a hashtable for the elements we need to retain

	    uniqueHash = new Hashtable(reserved.size(), 1.0f);
	  }
	else
	  {
	    // just create a small one

	    uniqueHash = new Hashtable(10);
	  }

	// now copy our hash to preserve the namespace handles
	
	Enumeration en = reserved.elements();
	
	while (en.hasMoreElements())
	  {
	    Object value = en.nextElement();
		
	    DBNameSpaceHandle handle = space.getHandle(value);
		
	    handle = (DBNameSpaceHandle) handle.clone();
		
	    uniqueHash.put(value, handle);
	  }
      }
  }

  public boolean containsValue(Object value)
  {
    return reserved.containsKey(value);
  }

  public DBNameSpaceHandle getValueHandle(Object value)
  {
    return (DBNameSpaceHandle) uniqueHash.get(value);
  }

  /**
   * <p>This method dissolves everything referenced by this DBNameSpaceCkPoint
   * in order to facilitate speedy garbage collection.</p>
   */

  public synchronized void cleanup()
  {
    if (reserved != null)
      {
	reserved.clear();
	reserved = null;
      }

    if (uniqueHash != null)
      {
	Enumeration en = uniqueHash.elements();

	while (en.hasMoreElements())
	  {
	    DBNameSpaceHandle handle = (DBNameSpaceHandle) en.nextElement();

	    handle.cleanup();
	  }

	uniqueHash.clear();
	uniqueHash = null;
      }
  }
}
