/*

   VectorUtils.java

   Convenience methods for working with Vectors.. provides efficient Union,
   Intersection, and Difference methods.
   
   Created: 21 July 1998

   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   Last Mod Date: $Date$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Directory Management System
 
   Copyright (C) 1996 - 2005
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

package arlut.csd.Util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     VectorUtils

------------------------------------------------------------------------------*/

/**
 * <P>Convenience methods for working with Vectors.. provides efficient Union,
 * Intersection, and Difference methods.</P>
 */

public class VectorUtils {

  /**
   * <P>This method returns a Vector containing the union of the objects
   * contained in vectA and vectB.  The resulting Vector will not
   * contain any duplicates, even if vectA or vectB themselves contain
   * repeated items.</P>
   *
   * <P>This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.</P>
   */

  public static Vector union(Vector vectA, Vector vectB)
  {
    int threshold = vectSize(vectA) + vectSize(vectB);

    if (threshold < 10)		// I pulled 10 out of my ass
      {
	Vector result = new Vector(threshold);

	if (vectA != null)
	  {
	    for (int i = 0; i < vectA.size(); i++)
	      {
		Object obj = vectA.elementAt(i);
		
		if (!result.contains(obj))
		  {
		    result.addElement(obj);
		  }
	      }
	  }

	if (vectB != null)
	  {
	    for (int i = 0; i < vectB.size(); i++)
	      {
		Object obj = vectB.elementAt(i);
		
		if (!result.contains(obj))
		  {
		    result.addElement(obj);
		  }
	      }
	  }

	return result;
      }
    else
      {
	// If we have a big enough set of elements to union, use a
	// temporary hashtable so that we have better scalability for
	// item lookup.

	HashMap workSet = new HashMap(vectSize(vectA) + vectSize(vectB));
	Vector result = new Vector(threshold);
	Iterator iter;
	Object item;

	/* -- */

	if (vectA != null)
	  {
	    iter = vectA.iterator();

	    while (iter.hasNext())
	      {
		item = iter.next();
		workSet.put(item, item);
	      }
	  }
    
	if (vectB != null)
	  {
	    iter = vectB.iterator();

	    while (iter.hasNext())
	      {
		item = iter.next();
		workSet.put(item, item);
	      }
	  }

	iter = workSet.values().iterator();

	while (iter.hasNext())
	  {
	    result.addElement(iter.next());
	  }

	// we're big enough to be over threshold, so lets go ahead and
	// take the time to trim to size

	result.trimToSize();

	return result;
      }
  }

  /**
   * <P>This method adds obj to vect if and only if vect does not
   * already contain obj.</P>
   */

  public static void unionAdd(Vector vect, Object obj)
  {
    if (obj == null)
      {
	return;
      }

    if (vect.contains(obj))
      {
	return;
      }

    vect.addElement(obj);
  }

  /**
   * <P>Returns true if vectA and vectB have any elements in
   * common.</P> 
   */

  public static boolean overlaps(Vector vectA, Vector vectB)
  {
    if (vectA == null || vectB == null || vectA.size() == 0 || vectB.size() == 0)
      {
	return false;
      }

    if ((vectA.size() + vectB.size()) > 20)		// ass, again
      {
	HashMap workSet = new HashMap(vectA.size());
	
	for (int i = 0; i < vectA.size(); i++)
	  {
	    workSet.put(vectA.elementAt(i), vectA.elementAt(i));
	  }
	
	for (int i = 0; i < vectB.size(); i++)
	  {
	    if (workSet.containsKey(vectB.elementAt(i)))
	      {
		return true;
	      }
	  }
      }
    else 
      {
	if (vectA.size() > vectB.size())
	  {
	    for (int i = 0; i < vectA.size(); i++)
	      {
		if (vectB.contains(vectA.elementAt(i)))
		  {
		    return true;
		  }
	      }
	  }
	else
	  {
	    for (int i = 0; i < vectB.size(); i++)
	      {
		if (vectA.contains(vectB.elementAt(i)))
		  {
		    return true;
		  }
	      }
	  }
      }

    return false;
  }

  /**
   * <P>This method returns a Vector containing the intersection of the
   * objects contained in vectA and vectB.</P>
   *
   * <P>This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.</P>
   */

  public static Vector intersection(Vector vectA, Vector vectB)
  {
    HashMap 
      workSetA = new HashMap(),
      workSetB = new HashMap(),
      resultSet = new HashMap();

    Vector result = new Vector();
    Iterator iter;
    Object item;

    /* -- */

    if (vectA != null)
      {
	iter = vectA.iterator();

	while (iter.hasNext())
	  {
	    item = iter.next();
	    workSetA.put(item, item);
	  }
      }
    
    if (vectB != null)
      {
	iter = vectB.iterator();

	while (iter.hasNext())
	  {
	    item = iter.next();
	    workSetB.put(item, item);
	  }
      }

    iter = workSetA.values().iterator();

    while (iter.hasNext())
      {
	item = iter.next();

	if (workSetB.containsKey(item))
	  {
	    resultSet.put(item, item);
	  }
      }

    iter = workSetB.values().iterator();

    while (iter.hasNext())
      {
	item = iter.next();

	if (workSetA.containsKey(item))
	  {
	    resultSet.put(item, item);
	  }
      }

    iter = resultSet.values().iterator();

    while (iter.hasNext())
      {
	result.addElement(iter.next());
      }

    return result;
  }

  /**
   * <P>This method returns a Vector containing the set of objects
   * contained in vectA that are not contained in vectB.</P>
   *
   * <P>This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.</P>
   */

  public static Vector difference(Vector vectA, Vector vectB)
  {
    Vector result = new Vector();
    Object item;

    /* -- */

    if (vectA == null)
      {
	return result;
      }

    if (vectB == null)
      {
	return (Vector) vectA.clone();
      }

    if (vectA.size() + vectB.size() > 10) // ass
      {
	HashMap workSetB = new HashMap(vectA.size() + vectB.size());
	Iterator iter;

	/* -- */

	iter = vectB.iterator();
    
	while (iter.hasNext())
	  {
	    item = iter.next();
	    workSetB.put(item, item);
	  }
	
	iter = vectA.iterator();
	
	while (iter.hasNext())
	  {
	    item = iter.next();
	    
	    if (!workSetB.containsKey(item))
	      {
		result.addElement(item);
	      }
	  }
      }
    else
      {
	for (int i = 0; i < vectA.size(); i++)
	  {
	    item = vectA.elementAt(i);

	    if (!vectB.contains(item))
	      {
		result.addElement(item);
	      }
	  }
      }

    return result;
  }

  /**
   * <P>This method returns a Vector of items that appeared in the 
   * vector parameter more than once.</P>
   *
   * <P>If no duplicates are found or if vector is null, this method
   * returns null.
   */

  public static Vector duplicates(Vector vector)
  {
    if (vector == null)
      {
	return null;
      }

    Vector result = null;
    HashMap found = new HashMap();

    for (int i = 0; i < vector.size(); i++)
      {
	Object item = vector.elementAt(i);

	if (found.containsKey(item))
	  {
	    if (result == null)
	      {
		result = new Vector();
	      }

	    unionAdd(result, item);
	  }

	found.put(item, item);
      }

    return result;
  }

  /**
   * <P>This method returns a Vector containing the elements of vectA minus
   * the elements of vectB.  If vectA has an element in the Vector 5 times
   * and vectB has it 3 times, the result will have it two times.</P>
   *
   * <P>This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.</P>
   */

  public static Vector minus(Vector vectA, Vector vectB)
  {
    Vector result = new Vector();
    Iterator iter;
    Object item;

    /* -- */

    if (vectA == null)
      {
	return result;
      }

    result = (Vector) vectA.clone();

    if (vectB != null)
      {
	iter = vectB.iterator();

	while (iter.hasNext())
	  {
	    item = iter.next();

	    if (result.contains(item))
	      {
		result.removeElement(item);
	      }
	  }
      }

    return result;
  }

  /**
   * <P>This method returns a string containing all the elements in vec
   * concatenated together, comma separated.</P>
   */

  public static String vectorString(Vector vec)
  {
    if (vec == null)
      {
	return "";
      }

    StringBuffer temp = new StringBuffer();

    for (int i = 0; i < vec.size(); i++)
      {
	if (i > 0)
	  {
	    temp.append(",");
	  }

	temp.append(vec.elementAt(i));
      }

    return temp.toString();
  }

  /**
   * <P>This method takes a sepChars-separated string and converts it to
   * a vector of fields.  i.e., "gomod,jonabbey" -> a vector whose
   * elements are "gomod" and "jonabbey".</P>
   *
   * <P>NOTE: this method will omit 'degenerate' fields from the output
   * vector.  That is, if input is "gomod,,,  jonabbey" and sepChars
   * is ", ", then the result vector will still only have "gomod"
   * and "jonabbey" as elements, even though one might wish to
   * explicitly know about the blanks between commas.  This method
   * is intended mostly for creating email list vectors, rather than
   * general file-parsing vectors.</P>
   *
   * @param input the sepChars-separated string to test.
   *
   * @param sepChars a string containing a list of characters which
   * may occur as field separators.  Any two fields in the input may
   * be separated by one or many of the characters present in sepChars.
   */

  public static Vector stringVector(String input, String sepChars)
  {
    Vector results = new Vector();
    int index = 0;
    int oldindex = 0;
    String temp;
    char inputAry[] = input.toCharArray();

    /* -- */

    while (index != -1)
      {
	// skip any leading field-separator chars

	for (; oldindex < input.length(); oldindex++)
	  {
	    if (sepChars.indexOf(inputAry[oldindex]) == -1)
	      {
		break;
	      }
	  }

	if (oldindex == input.length())
	  {
	    break;
	  }

	index = findNextSep(input, oldindex, sepChars);

	if (index == -1)
	  {
	    temp = input.substring(oldindex);

	    // System.err.println("+ " + temp + " +");

	    results.addElement(temp);
	  }
	else
	  {
	    temp = input.substring(oldindex, index);

	    // System.err.println("* " + temp + " *");

	    results.addElement(temp);

	    oldindex = index + 1;
	  }
      }
    
    return results;
  }

  /**
   * <P>findNextSep() takes a string, a starting position, and a string of
   * characters to be considered field separators, and returns the
   * first index after startDex whose char is in sepChars.</P>
   *
   * <P>If there are no chars in sepChars past startdex in input, findNextSep()
   * returns -1.</P>
   */

  private static int findNextSep(String input, int startDex, String sepChars)
  {
    int currentIndex = input.length();
    char sepAry[] = sepChars.toCharArray();
    boolean foundSep = false;

    /* -- */
    
    // find the next separator

    for (int i = 0; i < sepAry.length; i++)
      {
	int tempdex = input.indexOf(sepAry[i], startDex);

	if (tempdex > -1 && tempdex <= currentIndex)
	  {
	    currentIndex = tempdex;
	    foundSep = true;
	  }
      }

    if (foundSep)
      {
	return currentIndex;
      }
    else
      {
	return -1;
      }
  }

  private static int vectSize(Vector x)
  {
    if (x == null)
      {
	return 0;
      }
    else
      {
	return x.size();
      }
  }

  // debug rig.

  public static void main(String[] args)
  {
    // String testString = "jon, beth   ross,,,darren,anna";

    String testString = "jon, beth   ross,,,darren,anna,,,,,";
    
    Vector results = stringVector(testString, ", ");

    for (int i = 0; i < results.size(); i++)
      {
	System.out.println(i + ": " + results.elementAt(i));
      }
  }
}
