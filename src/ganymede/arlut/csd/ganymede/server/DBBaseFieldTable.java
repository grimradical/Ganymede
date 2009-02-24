/*

   DBBaseFieldTable.java

   A customized variant of the java.util.Hashtable class that is
   tuned for use as Ganymede's base field hashes.
   
   Created: 9 June 1998
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import java.lang.Iterable;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

/*------------------------------------------------------------------------------
                                                                           class
                                                                DBBaseFieldTable

------------------------------------------------------------------------------*/

/**
 * <P>A customized variant of the java.util.Hashtable class that is
 * tuned for use in managing
 * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField}s
 * in a Ganymede {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}.</P>
 * 
 * @version $Id$
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class DBBaseFieldTable implements Iterable<DBObjectBaseField> {

  /**
   * The hash table data.
   */

  private transient DBObjectBaseField table[];

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
   * Constructs a new, empty DBBaseFieldTable with the specified initial 
   * capacity and the specified load factor. 
   *
   * @param      initialCapacity   the initial capacity of the hashtable.
   * @param      loadFactor        a number between 0.0 and 1.0.
   * @exception  IllegalArgumentException  if the initial capacity is less
   *               than or equal to zero, or if the load factor is less than
   *               or equal to zero.
   */

  public DBBaseFieldTable(int initialCapacity, float loadFactor) 
  {
    if ((initialCapacity <= 0) || (loadFactor <= 0.0) || (loadFactor > 1.0)) 
      {
	throw new IllegalArgumentException();
      }

    this.loadFactor = loadFactor;
    table = new DBObjectBaseField[initialCapacity];
    threshold = (int)(initialCapacity * loadFactor);
  }

  /**
   * Constructs a new, empty DBBaseFieldTable with the specified initial capacity
   * and default load factor.
   *
   * @param   initialCapacity   the initial capacity of the hashtable.
   */

  public DBBaseFieldTable(int initialCapacity) 
  {
    this(initialCapacity, 0.75f);
  }

  /**
   * Constructs a new, empty DBBaseFieldTable with a default capacity and load
   * factor. 
   *
   */

  public DBBaseFieldTable() 
  {
    this(101, 0.75f);
  }

  /**
   * Returns the number of objects in this DBBaseFieldTable.
   *
   * @return  the number of objects in this DBBaseFieldTable.
   *
   */

  public int size() 
  {
    return count;
  }

  /**
   * Tests if this DBBaseFieldTable contains no objects.
   *
   * @return  <code>true</code> if this DBBaseFieldTable contains no values;
   *          <code>false</code> otherwise.
   *
   */

  public boolean isEmpty() 
  {
    return count == 0;
  }

  /**
   * Returns an Iterator of the objects in this DBBaseFieldTable.
   *
   * Use the Iterator methods on the returned object to fetch the
   * elements sequentially.
   *
   * This method allows DBBaseFieldTable to support the Java 5 foreach
   * loop construct.
   *
   * @return  an Iterator of the objects in this DBObjectTable.
   * @see     java.util.Iterator
   */

  public synchronized Iterator<DBObjectBaseField> iterator()
  {
    return new DBBaseFieldTableIterator(table);
  }

  /**
   * Returns an enumeration of the objects in this DBBaseFieldTable.
   * Use the Enumeration methods on the returned object to fetch the elements
   * sequentially.
   *
   * @return  an enumeration of the objects in this DBBaseFieldTable.
   * @see     java.util.Enumeration
   *
   */

  public synchronized Enumeration elements()
  {
    return new DBBaseFieldTableEnumerator(table);
  }

  /**
   * Tests if the DBObjectBaseField value is contained in this DBBaseFieldTable.
   *
   * @param      value   a DBObjectBaseField to search for.
   * @exception  NullPointerException  if the value is <code>null</code>.
   *
   */

  public boolean contains(DBObjectBaseField value) 
  {
    if (value == null) 
      {
	throw new NullPointerException();
      }

    return containsKey(value.getID());
  }

  /**
   * Tests if a DBObjectBaseField with the specified object id is in this DBBaseFieldTable.
   * 
   * @param   key   possible object id.
   *
   */

  public boolean containsKey(Short key) 
  {
    return containsKey(key.shortValue());
  }

  /**
   * Tests if a DBObjectBaseField with the specified object id is in this DBBaseFieldTable.
   * 
   * @param   key   possible object id.
   *
   */

  public synchronized boolean containsKey(short key) 
  {
    DBObjectBaseField tab[] = table;

    short index = (short) ((key & 0x7FFF) % tab.length);

    for (DBObjectBaseField e = tab[index] ; e != null ; e = e.next) 
      {
	if (e.getID() == key)
	  {
	    return true;
	  }
      }
    
    return false;
  }

  /**
   *
   * Returns the DBObjectBaseField with the specified key from this DBBaseFieldTable, or
   * null if no object with that id is in this table.
   *
   */

  public DBObjectBaseField getNoSync(short key) 
  {
    DBObjectBaseField tab[] = table;

    short index = (short) ((key & 0x7FFF) % tab.length);

    for (DBObjectBaseField e = tab[index] ; e != null ; e = e.next) 
      {
	if (e.getID() == key)
	  {
	    return e;
	  }
      }

    return null;
  }

  /**
   *
   * Returns the DBObjectBaseField with the specified key from this DBBaseFieldTable, or
   * null if no object with that id is in this table.
   *
   */

  public synchronized DBObjectBaseField get(short key) 
  {
    DBObjectBaseField tab[] = table;

    short index = (short) ((key & 0x7FFF) % tab.length);

    for (DBObjectBaseField e = tab[index] ; e != null ; e = e.next) 
      {
	if (e.getID() == key)
	  {
	    return e;
	  }
      }

    return null;
  }

  /**
   *
   * Returns the DBObjectBaseField with the specified name from this
   * DBBaseFieldTable, or null if no object with that name is in this
   * table.
   *
   * This method is unprotected by synchronization, so you must be
   * sure to use higher level synchronization to use this safely.
   */

  public DBObjectBaseField getNoSync(String name) 
  {
    return this.findByName(name);
  }

  /**
   * Returns the DBObjectBaseField with the specified name from this
   * DBBaseFieldTable, or null if no object with that name is in this
   * table.
   *
   * The comparisons done in this method are case insensitive.
   */

  public synchronized DBObjectBaseField get(String name) 
  {
    return this.findByName(name);
  }

  /**
   *
   * Rehashes the contents of the DBBaseFieldTable into a DBBaseFieldTable
   * with a larger capacity. This method is called automatically when
   * the number of keys in the hashtable exceeds this DBBaseFieldTable's
   * capacity and load factor.
   * 
   */

  protected void rehash() 
  {
    int oldCapacity = table.length;
    DBObjectBaseField oldTable[] = table;

    int newCapacity = oldCapacity * 2 + 1;
    DBObjectBaseField newTable[] = new DBObjectBaseField[newCapacity];

    threshold = (int) (newCapacity * loadFactor);
    table = newTable;

    //System.out.println("rehash old=" + oldCapacity + ", new=" +
    //newCapacity + ", thresh=" + threshold + ", count=" + count);

    for (int i = oldCapacity ; i-- > 0 ;) 
      {
	for (DBObjectBaseField old = oldTable[i] ; old != null ; ) 
	  {
	    DBObjectBaseField e = old;
	    old = old.next;
	    
	    short index = (short) ((e.getID() & 0x7FFF) % newCapacity);
	    e.next = newTable[index];
	    newTable[index] = e;
	  }
      }
  }

  /**
   *
   * Inserts a DBObjectBaseField into this DBBaseFieldTable.
   *
   * This put is not sync'ed, and should only be used with
   * higher level sync provisions.
   *
   */

  public void putNoSync(DBObjectBaseField value) 
  {
    // Make sure the value is not null

    if (value == null) 
      {
	throw new NullPointerException();
      }

    // Makes sure the object is not already in the hashtable.
    
    removeNoSync(value.getID());
    
    DBObjectBaseField tab[] = table;
    short hash = value.getID();
    short index = (short) ((hash & 0x7FFF) % tab.length);

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
   * Inserts a DBObjectBaseField into this DBBaseFieldTable
   *
   */

  public synchronized void put(DBObjectBaseField value) 
  {
    // Make sure the value is not null

    if (value == null) 
      {
	throw new NullPointerException();
      }

    // Makes sure the object is not already in the hashtable.
    // Note that we are sync'ed, so we can use the non-sync'ed
    // removeNoSync().
    
    removeNoSync(value.getID());

    if (count > threshold) 
      {
	rehash();
      }

    DBObjectBaseField tab[] = table;
    short hash = value.getID();
    short index = (short) ((hash & 0x7FFF) % tab.length);

    // Insert the new entry.
    
    value.next = tab[index];
    tab[index] = value;
    count++;
    
    return;
  }

  /**
   *
   * Inserts a DBObjectBaseField into this DBBaseFieldTable.
   *
   * This put is not sync'ed, and should only be used with
   * higher level sync provisions.
   *
   */

  public void putNoSyncNoRemove(DBObjectBaseField value) 
  {
    // Make sure the value is not null

    if (value == null) 
      {
	throw new NullPointerException();
      }

    DBObjectBaseField tab[] = table;
    short hash = value.getID();
    short index = (short) ((hash & 0x7FFF) % tab.length);

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
   * Removes the DBObjectBaseField with the given id from this DBBaseFieldTable.
   *
   */

  public void removeNoSync(short key) 
  {
    DBObjectBaseField tab[] = table;
    short index = (short) ((key & 0x7FFF) % tab.length);

    for (DBObjectBaseField e = tab[index], prev = null ; e != null ; prev = e, e = e.next) 
      {
	if (e.getID() == key)
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
   * Removes the DBObjectBaseField with the given id from this DBBaseFieldTable.
   *
   */

  public synchronized void remove(short key) 
  {
    DBObjectBaseField tab[] = table;
    short index = (short) ((key & 0x7FFF) % tab.length);

    for (DBObjectBaseField e = tab[index], prev = null ; e != null ; prev = e, e = e.next) 
      {
	if (e.getID() == key)
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
   * Clears this DBBaseFieldTable.
   *
   */

  public synchronized void clear() 
  {
    DBObjectBaseField tab[] = table;

    /* -- */

    for (int index = tab.length; --index >= 0; )
      {
	tab[index] = null;
      }

    count = 0;
  }

  /**
   * This unsynchronized private helper method looks up
   * DBObjectBaseFields by name, using a case-insensitive comparison.
   */

  private final DBObjectBaseField findByName(String name)
  {
    for (int i = 0; i < table.length; i++)
      {
	DBObjectBaseField e = table[i];

	while (e != null)
	  {
	    String eName = e.getName();

	    if (eName != null && eName.equalsIgnoreCase(name))
	      {
		return e;
	      }

	    e = e.next;
	  }
      }

    return null;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                      DBBaseFieldTableEnumerator

------------------------------------------------------------------------------*/

/**
 * A DBBaseFieldTable enumerator class.  This class should remain opaque 
 * to the client. It will use the Enumeration interface. 
 */

class DBBaseFieldTableEnumerator implements Enumeration {

  short index;
  DBObjectBaseField table[];
  DBObjectBaseField entry;

  /* -- */

  DBBaseFieldTableEnumerator(DBObjectBaseField table[]) 
  {
    this.table = table;
    this.index = (short) table.length;
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
	DBObjectBaseField e = entry;
	entry = e.next;
	return e;
      }

    throw new NoSuchElementException("HashtableEnumerator");
  }
}


/*------------------------------------------------------------------------------
                                                                           class
                                                        DBBaseFieldTableIterator

------------------------------------------------------------------------------*/

/**
 * A DBBaseFieldTable Iterator class.  This class should remain opaque 
 * to the client. It will use the Iterator interface. 
 */

class DBBaseFieldTableIterator implements Iterator<DBObjectBaseField> {

  short index;
  DBObjectBaseField table[];
  DBObjectBaseField entry;

  /* -- */

  DBBaseFieldTableIterator(DBObjectBaseField table[]) 
  {
    this.table = table;
    this.index = (short) table.length;
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

  public DBObjectBaseField next() 
  {
    if (entry == null) 
      {
	while ((index-- > 0) && ((entry = table[index]) == null));
      }

    if (entry != null) 
      {
	DBObjectBaseField e = entry;
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
