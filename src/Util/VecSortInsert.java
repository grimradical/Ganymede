/*

   VecSortInsert.java

   This class is used to do an ordered insert using a binary
   search.  It's designed for speed.
   
   Created: 6 February 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.Util;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   VecSortInsert

------------------------------------------------------------------------------*/

public class VecSortInsert {

  Compare comparator;
  static boolean debug = false;

  /* -- */

  // debug rig
  
  public static void main(String[] argv)
  {
    debug = true;
    Vector test = new Vector();

    test.addElement("B");
    test.addElement("C");
    test.addElement("E");
    test.addElement("H");
    test.addElement("J");
    test.addElement("N");
    test.addElement("O");
    test.addElement("Q");
    test.addElement("X");
    test.addElement("Y");

    VecSortInsert inserter = new VecSortInsert(new arlut.csd.Util.Compare() 
					       {
						 public int compare(Object o_a, Object o_b) 
						   {
						     String a, b;
						     
						     a = (String) o_a;
						     b = (String) o_b;
						     int comp = 0;
						     
						     comp = a.compareTo(b);
						     
						     if (comp < 0)
						       {
							 return -1;
						       }
						     else if (comp > 0)
						       { 
							 return 1;
						       } 
						     else
						       { 
							 return 0;
						       }
						   }
					       });

    System.out.println("Start: ");
    printTest(test);

    System.out.println("\nInserting A");

    inserter.insert(test, "A");

    System.out.println("Result: ");
    printTest(test);

    System.out.println("\nInserting K");

    inserter.insert(test, "K");

    System.out.println("Result: ");
    printTest(test);

    System.out.println("\nInserting G");

    inserter.insert(test, "G");

    System.out.println("Result: ");
    printTest(test);

    System.out.println("\nInserting I");

    inserter.insert(test, "I");

    System.out.println("Result: ");
    printTest(test);

    System.out.println("\nInserting Z");

    inserter.insert(test, "Z");

    System.out.println("Result: ");
    printTest(test);

    System.out.println("\nInserting K");

    inserter.insert(test, "K");

    System.out.println("Result: ");
    printTest(test);
  }

  static void printTest(Vector vec)
  {
    for (int i = 0; i < vec.size(); i++)
      {
	System.out.print(vec.elementAt(i));
	System.out.print("  ");
      }

    System.out.println();
  }

  static void printTest(int size, int low, int med, int high)
  {
    for (int i = 0; i < size; i++)
      {
	if (i == low)
	  {
	    System.out.print("l");
	  }
	else
	  {
	    System.out.print(" ");
	  }

	if (i == med)
	  {
	    System.out.print("m");
	  }
	else
	  {
	    System.out.print(" ");
	  }

	if (i == high)
	  {
	    System.out.print("h");
	  }
	else
	  {
	    System.out.print(" ");
	  }
      }

    System.out.println();
  }

  public VecSortInsert(Compare comparator)
  {
    this.comparator = comparator;
  }

  /**
   *
   * This method does the work.
   *
   */

  public void insert(Vector objects, Object element)
  {
    int low, high, mid;

    /* -- */

    if (objects.size() == 0)
      {
	objects.addElement(element);
	return;
      }

    // java integer division rounds towards zero

    low = 0;
    high = objects.size()-1;
    
    mid = (low + high) / 2;

    while (low < high)
      {
	if (debug)
	  {
	    printTest(objects.size(), low, mid, high);
	  }

	if (comparator.compare(element,objects.elementAt(mid)) < 0)
	  {
	    high = mid;
	  }
	else
	  {
	    low = mid + 1;
	  }

	mid = (low + high) / 2;
      }

    if (debug)
      {
	printTest(objects.size(), low, mid, high);
      }

    if (mid >= objects.size() - 1)
      {
	objects.addElement(element);
      }
    else
      {
	objects.insertElementAt(element, mid);
      }
  }
}
