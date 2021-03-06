/*

   DBObjectTable.java

   A customized variant of the java.util.Hashtable class that is
   tuned for use as Ganymede's object hashes.
   
   Created: 9 June 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2009
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

package arlut.csd.ganymede.server;

import java.lang.Iterable;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   DBObjectTable

------------------------------------------------------------------------------*/

/**
 * <P>A customized variant of the java.util.Hashtable class that is
 * tuned for use in managing {@link arlut.csd.ganymede.server.DBObject DBObject}s
 * in a Ganymede {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}.</P>
 * 
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class DBObjectTable implements Iterable<DBObject> {

  /**
   * The hash table data.
   */

  private transient DBObject table[];

  /**
   * The total number of entries in the hash table.
   */

  private transient int count;

  /**
   * Rehashes the table when count exceeds this threshold.
   */

  private int threshold;

  /**
   * The load factor for the hashtable.
   */

  private float loadFactor;

  /**
   * Constructs a new, empty DBObjectTable with the specified initial 
   * capacity and the specified load factor. 
   *
   * @param      initialCapacity   the initial capacity of the hashtable.
   * @param      loadFactor        a number between 0.0 and 1.0.
   * @exception  IllegalArgumentException  if the initial capacity is less
   *               than or equal to zero, or if the load factor is less than
   *               or equal to zero.
   */

  public DBObjectTable(int initialCapacity, float loadFactor) 
  {
    if ((initialCapacity <= 0) || (loadFactor <= 0.0) || (loadFactor > 1.0)) 
      {
	throw new IllegalArgumentException();
      }

    this.loadFactor = loadFactor;
    table = new DBObject[initialCapacity];
    threshold = (int)(initialCapacity * loadFactor);
  }

  /**
   * Constructs a new, empty DBObjectTable with the specified initial capacity
   * and default load factor.
   *
   * @param   initialCapacity   the initial capacity of the hashtable.
   */

  public DBObjectTable(int initialCapacity) 
  {
    this(initialCapacity, 0.75f);
  }

  /**
   * Constructs a new, empty DBObjectTable with a default capacity and load
   * factor. 
   *
   */

  public DBObjectTable() 
  {
    this(101, 0.75f);
  }

  /**
   * Returns the number of objects in this DBObjectTable.
   *
   * @return  the number of objects in this DBObjectTable.
   *
   */

  public int size() 
  {
    return count;
  }

  /**
   * Tests if this DBObjectTable contains no objects.
   *
   * @return  <code>true</code> if this DBObjectTable contains no values;
   *          <code>false</code> otherwise.
   *
   */

  public boolean isEmpty() 
  {
    return count == 0;
  }

  /**
   * Returns an Iterator of the objects in this DBObjectTable.
   *
   * Use the Iterator methods on the returned object to fetch the
   * elements sequentially.
   *
   * This method allows DBObjectTable to support the Java 5 foreach
   * loop construct.
   *
   * @return  an Iterator of the objects in this DBObjectTable.
   * @see     java.util.Iterator
   */

  public synchronized Iterator<DBObject> iterator()
  {
    return new DBObjectTableIterator(table);
  }

  /**
   * Returns an enumeration of the objects in this DBObjectTable.
   * Use the Enumeration methods on the returned object to fetch the elements
   * sequentially.
   *
   * @return  an enumeration of the objects in this DBObjectTable.
   * @see     java.util.Enumeration
   *
   */

  public synchronized Enumeration elements()
  {
    return new DBObjectTableEnumerator(table);
  }

  /**
   * Tests if the DBObject value is contained in this DBObjectTable.
   *
   * @param      value   a DBObject to search for.
   * @exception  NullPointerException  if the value is <code>null</code>.
   *
   */

  public boolean contains(DBObject value) 
  {
    if (value == null) 
      {
	throw new NullPointerException();
      }

    return containsKey(value.hashCode());
  }

  /**
   * Tests if a DBObject with the specified object id is in this DBObjectTable.
   * 
   * @param   key   possible object id.
   *
   */

  public synchronized boolean containsKey(int key) 
  {
    DBObject tab[] = table;

    int index = (key & 0x7FFFFFFF) % tab.length;

    for (DBObject e = tab[index] ; e != null ; e = e.next) 
      {
	if (e.hashCode() == key)
	  {
	    return true;
	  }
      }
    
    return false;
  }

  /**
   *
   * Returns the DBObject with the specified key from this DBObjectTable, or
   * null if no object with that id is in this table.
   *
   */

  public DBObject getNoSync(int key) 
  {
    DBObject tab[] = table;

    int index = (key & 0x7FFFFFFF) % tab.length;

    for (DBObject e = tab[index] ; e != null ; e = e.next) 
      {
	if (e.hashCode() == key)
	  {
	    return e;
	  }
      }

    return null;
  }

  /**
   *
   * Returns the DBObject with the specified key from this DBObjectTable, or
   * null if no object with that id is in this table.
   *
   */

  public synchronized DBObject get(int key) 
  {
    DBObject tab[] = table;

    int index = (key & 0x7FFFFFFF) % tab.length;

    for (DBObject e = tab[index] ; e != null ; e = e.next) 
      {
	if (e.hashCode() == key)
	  {
	    return e;
	  }
      }

    return null;
  }

  /**
   *
   * Rehashes the contents of the DBObjectTable into a DBObjectTable
   * with a larger capacity. This method is called automatically when
   * the number of keys in the hashtable exceeds this DBObjectTable's
   * capacity and load factor.
   * 
   */

  protected void rehash() 
  {
    int oldCapacity = table.length;
    DBObject oldTable[] = table;

    int newCapacity = oldCapacity * 2 + 1;
    DBObject newTable[] = new DBObject[newCapacity];

    threshold = (int)(newCapacity * loadFactor);
    table = newTable;

    //System.out.println("rehash old=" + oldCapacity + ", new=" +
    //newCapacity + ", thresh=" + threshold + ", count=" + count);

    for (int i = oldCapacity ; i-- > 0 ;) 
      {
	for (DBObject old = oldTable[i] ; old != null ; ) 
	  {
	    DBObject e = old;
	    old = old.next;
	    
	    int index = (e.hashCode() & 0x7FFFFFFF) % newCapacity;
	    e.next = newTable[index];
	    newTable[index] = e;
	  }
      }
  }

  /**
   *
   * Inserts a DBObject into this DBObjectTable.
   *
   * This put is not sync'ed, and should only be used with
   * higher level sync provisions.
   *
   */

  public void putNoSync(DBObject value) 
  {
    // Make sure the value is not null

    if (value == null) 
      {
	throw new NullPointerException();
      }

    // Makes sure the object is not already in the hashtable.
    
    removeNoSync(value.hashCode());
    
    DBObject tab[] = table;
    int hash = value.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;

    if (count > threshold) 
      {
	rehash();
	putNoSync(value);

	return;
      } 

    // Insert the new entry.

    value.next = tab[index];
    tab[index] = value;
    count++;
    return;
  }

  /**
   *
   * Inserts a DBObject into this DBObjectTable
   *
   */

  public synchronized void put(DBObject value) 
  {
    // Make sure the value is not null

    if (value == null) 
      {
	throw new NullPointerException();
      }

    // Makes sure the object is not already in the hashtable.
    // Note that we are sync'ed, so we can use the non-sync'ed
    // removeNoSync().
    
    removeNoSync(value.hashCode());

    if (count > threshold) 
      {
	rehash();
      }

    DBObject tab[] = table;
    int hash = value.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;

    // Insert the new entry.
    
    value.next = tab[index];
    tab[index] = value;
    count++;
    
    return;
  }

  /**
   *
   * Inserts a DBObject into this DBObjectTable.
   *
   * This put is not sync'ed, and should only be used with
   * higher level sync provisions.
   *
   */

  public void putNoSyncNoRemove(DBObject value) 
  {
    // Make sure the value is not null

    if (value == null) 
      {
	throw new NullPointerException();
      }

    DBObject tab[] = table;
    int hash = value.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;

    if (count > threshold) 
      {
	rehash();
	putNoSync(value);

	return;
      } 

    // Insert the new entry.

    value.next = tab[index];
    tab[index] = value;
    count++;
    return;
  }

  /**
   *
   * Removes the DBObject with the given id from this DBObjectTable.
   *
   */

  public void removeNoSync(int key) 
  {
    DBObject tab[] = table;
    int index = (key & 0x7FFFFFFF) % tab.length;

    for (DBObject e = tab[index], prev = null ; e != null ; prev = e, e = e.next) 
      {
	if (e.hashCode() == key)
	  {
	    if (prev != null) 
	      {
		prev.next = e.next;
	      } 
	    else
	      {
		tab[index] = e.next;
	      }

	    count--;

	    return;
	  }
      }

    return;
  }

  /**
   *
   * Removes the DBObject with the given id from this DBObjectTable.
   *
   */

  public synchronized void remove(int key) 
  {
    DBObject tab[] = table;
    int index = (key & 0x7FFFFFFF) % tab.length;

    for (DBObject e = tab[index], prev = null ; e != null ; prev = e, e = e.next) 
      {
	if (e.hashCode() == key)
	  {
	    if (prev != null) 
	      {
		prev.next = e.next;
	      } 
	    else
	      {
		tab[index] = e.next;
	      }

	    count--;

	    return;
	  }
      }

    return;
  }

  /**
   *
   * Clears this DBObjectTable.
   *
   */

  public synchronized void clear() 
  {
    DBObject tab[] = table;

    /* -- */

    for (int index = tab.length; --index >= 0; )
      {
	tab[index] = null;
      }

    count = 0;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                         DBObjectTableEnumerator

------------------------------------------------------------------------------*/

/**
 * <P>A {@link arlut.csd.ganymede.server.DBObjectTable DBObjectTable} enumerator class.
 * This class should remain opaque to the client, which will use the
 * Enumeration interface.</P>
 */

class DBObjectTableEnumerator implements Enumeration {

  int index;
  DBObject table[];
  DBObject entry;

  /* -- */

  DBObjectTableEnumerator(DBObject table[]) 
  {
    this.table = table;
    this.index = table.length;
  }
	
  public boolean hasMoreElements() 
  {
    if (entry != null) 
      {
	return true;
      }

    while (index-- > 0) 
      {
	if ((entry = table[index]) != null) 
	  {
	    return true;
	  }
      }

    return false;
  }

  public Object nextElement() 
  {
    if (entry == null) 
      {
	while ((index-- > 0) && ((entry = table[index]) == null));
      }

    if (entry != null) 
      {
	DBObject e = entry;
	entry = e.next;
	return e;
      }

    throw new NoSuchElementException("HashtableEnumerator");
  }
}


/*------------------------------------------------------------------------------
                                                                           class
                                                           DBObjectTableIterator

------------------------------------------------------------------------------*/

/**
 * <P>A {@link arlut.csd.ganymede.server.DBObjectTable DBObjectTable}
 * Iterator class.  This class should remain opaque to the client,
 * which will use the Iterator interface.</P>
 */

class DBObjectTableIterator implements Iterator<DBObject> {

  int index;
  DBObject table[];
  DBObject entry;

  /* -- */

  DBObjectTableIterator(DBObject table[]) 
  {
    this.table = table;
    this.index = table.length;
  }
	
  public boolean hasNext() 
  {
    if (entry != null) 
      {
	return true;
      }

    while (index-- > 0) 
      {
	if ((entry = table[index]) != null) 
	  {
	    return true;
	  }
      }

    return false;
  }

  public DBObject next() 
  {
    if (entry == null) 
      {
	while ((index-- > 0) && ((entry = table[index]) == null));
      }

    if (entry != null) 
      {
	DBObject e = entry;
	entry = e.next;
	return e;
      }

    throw new NoSuchElementException("HashtableEnumerator");
  }

  public void remove()
  {
    throw new UnsupportedOperationException();
  }
}
