/*

   VecQuickSort.java

   A Vector implementation of the QuickSort algorithm.
   
   Created: 12 August 1997

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2004
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

import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    VecQuickSort

------------------------------------------------------------------------------*/

/**
 * <P>QuickSort implementation for Vector.  Uses the
 * {@link arlut.csd.Util.Compare Compare} interface for item
 * comparisons.</P>
 *
 * <P>Based on code by Eric van Bezooijen (eric@logrus.berkeley.edu)
 * and Roedy Green (roedy@bix.com).</P>
 *
 * @version $Id$
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT 
 */

public class VecQuickSort implements Compare {

  Vector objects;
  Compare comparator;

  /* -- */

  /**
   *
   * VecQuickSort constructor.
   *
   * @param objects Vector of objects to be sorted in place
   * @param comparator Compare object.. if null, standard string compare
   * will be done.
   *
   */

  public VecQuickSort(Vector objects, Compare comparator)
  {
    this.objects = objects;

    if (comparator == null)
      {
	this.comparator = this;
      }
    else
      {
	this.comparator = comparator;
      }
  }

  private void quick(int first, int last)
  {
    int j;

    /* -- */

    if (first < last)
      {
	j = partition(first, last);
	
	if (j == last) 
	  {
	    j--;
	  }
	
	quick(first,j);
	quick(j+1,last);
      }
  }

  /**
   * <P>Partition by splitting this chunk to sort in two and
   * get all big elements on one side of the pivot and all
   * the small elements on the other.</P>
   */

  private int partition(int first, int last)
  {
    Object pivot = objects.elementAt(first);

    while (true)
      {
	while (comparator.compare(objects.elementAt(last), pivot) >= 0 &&
	       last > first)
	  {
	    last--;
	  }

	while (comparator.compare(objects.elementAt(first), pivot) < 0 &&
	       first < last)
	  {
	    first++;
	  }

	if (first < last)
	  {
	    swap(first, last);	// exchange objects on either side of the pivot
	  } 
	else
	  {
	    return last;
	  }
      }
  }

  private void swap(int i, int j)
  {
    Object tmp = objects.elementAt(i);
    Object tmp2 = objects.elementAt(j);

    objects.setElementAt(tmp, j);
    objects.setElementAt(tmp2, i);
  }

  /**
   * <P>Sort the elements</P>
   */

  public void sort()
  {
    if (objects.size() < 2)
      {
	return;
      }
    
    quick(0, objects.size()-1);
  }

  /**
   * <p>Default comparator, does a string comparison on the
   * toString() output of the objects for ordering.</p>
   */

  public int compare(Object a, Object b)
  {
    return a.toString().compareTo(b.toString());
  }
}