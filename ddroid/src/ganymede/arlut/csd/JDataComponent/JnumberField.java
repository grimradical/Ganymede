/*
   JnumberField.java

   
   Created: 12 Jul 1996

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Navin Manohar, Jonathan Abbey, Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2004
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

package arlut.csd.JDataComponent;


/*------------------------------------------------------------------------------
                                                                           class
                                                                    JnumberField

------------------------------------------------------------------------------*/

/**
 * This class defines an entry field that is capable of handling
 * integers.  The maximum and minimum bounds for the range of 
 * integers that can be entered into this JnumberField can also
 * be preset.
 */

public class JnumberField extends JentryField {

  public static int DEFAULT_COLS = 20;

  public static String allowedChars = new String("0123456789-");

  private Integer oldvalue;

  private boolean limited = false;

  private boolean processingCallback = false;

  private int maxSize;
  private int minSize;

  ///////////////////
  //  Constructors //
  ///////////////////

 /**
   * Base constructor for JnumberField
   * 
   * @param columns number of colums in the JnumberField
   * @param is_editable true if this JnumberField is editable
   * @param islimited true if there is a restriction on the range of values
   * @param minsize the minimum limit on the range of values
   * @param maxsize the maximum limit on the range of values
   */ 
  public JnumberField(int columns,
		      boolean iseditable,
		      boolean islimited,
		      int minsize,
		      int maxsize)
  {
    super(columns);
    
    if (islimited)
      {
	limited = true;
	
	maxSize = maxsize;
	minSize = minsize;
      }
    
    setEditable(iseditable);  // will this JnumberField be editable or not?
  }

  /**
   * Constructor which uses takes a number of columns, and everything
   * else default.
   */

  public JnumberField(int width)
  {
    this(width,
	 true,
	 false,
	 0,Integer.MAX_VALUE);
  }

  /**
   * Constructor which uses default everything.
   */

  public JnumberField()
  {
    this(JnumberField.DEFAULT_COLS);
  }
 
 /**
  * Constructor that allows for the creation of a JnumberField
  * that knows about its parent and can invoke a callback method.
  *  
  * @param columns number of colums in the JnumberField
  * @param is_editable true if this JnumberField is editable
  * @param islimited true if there is a restriction on the range of values
  * @param minsize the minimum limit on the range of values
  * @param maxsize the maximum limit on the range of values
  * @param parent the container within which this JnumberField is contained
  *        (This container will implement an interface that will utilize the
  *         data contained within this JnumberField.)
  *
  */ 
  public JnumberField(int columns,
		     boolean iseditable,
		     boolean limited,
		     int minsize,
		     int maxsize,
		     JsetValueCallback parent)
  {
    this(columns,iseditable,limited,minsize,maxsize);
    
    setCallback(parent);
  }


  ///////////////////
  // Class Methods //
  ///////////////////
 
  /**
   * returns true if <c> is a valid numerical
   * digit.
   *
   * @param c the character to check
   */

  public boolean isAllowed(char c)
  {
    if (allowedChars.indexOf(c) == -1)
      {
	if (debug)
	  {
	    System.err.println("JnumberField.isAllowed(): ruling NO WAY on char '" + c + "'");
	  }

	return false;
      }

    return true;
  }

  /**
   * returns the value of this JnumberField as an Integer object
   *
   * If this field is empty, will return null. If this field is
   * not empty and has a non-numeric string, will throw a
   * NumberFormatException.
   *
   */

  public Integer getValue() throws NumberFormatException
  {
    String str = getText();

    if (str == null || str.equals(""))
      {
	return null;
      }

    return new Integer(str);
  }

  /**
   * sets the value of this JnumberField to num
   *
   * This method does not trigger a callback to our container.. we
   * only callback as a result of loss-of-focus brought on by the
   * user.
   *
   * @param num the number to use
   */ 

  public void setValue(int num)
  {
    setValue(new Integer(num));
  }

  /**
   * sets the value of this JnumberField using an Integer object.
   *
   * This method does not trigger a callback to our container.. we
   * only callback as a result of loss-of-focus brought on by the
   * user.
   *
   * @param num the Integer object to use
   */

  public void setValue(Integer num)
  {
    if (limited)
      {
	if (num != null)
	  {
	    if (num.intValue() > maxSize || num.intValue() < minSize)
	      {
		System.err.println("Invalid Parameter: number out of range");
		return;
	      }
	  }
      }

    // remember the value that is being set.

    oldvalue = num;

    // and set the text field

    if (num != null)
      {
	setText(num.toString());
      }
    else
      {
	setText("");
      }
  }

  /**
   * Sets the limited/non-limited status of this JnumberField
   * If setLimited is given a true value as a parameter, then
   * certain bounds will be imposed on the range of possible 
   * values.
   *
   * @param bool true if a limit is to be set on the range of values
   */

  public void setLimited(boolean bool)
  {
    limited = bool;
  }

  /**
   *  sets the maximum value in the range of possible values.
   *
   * @param n the number to use when setting the maximum value
   */
  public void setMaxValue(int n)
  {
    limited = true;

    maxSize = n;
  }
  
  /**
   *  sets the minimum value in the range of possible values.
   *
   * @param n the number to use when setting the minimum value
   */
  public void setMinValue(int n)
  {
    limited = true;

    minSize = n;
  }

  /**
   * returns true if there is a bound on the range of values that
   * can be entered into this JnumberField
   */
  public boolean isLimited()
  {
    return limited;
  }

  /**
   * returns the maximum value in the range of valid values for this
   * JnumberField
   */
  public int getMaxValue()
  {
    return maxSize;
  }

  /**
   * returns the minimum value in the range of valid values for this
   * JnumberField
   */
  public int getMinValue()
  {
    return minSize;
  }

  /**
   * overrides JentryField.sendCallback().
   *
   * This is called when the number field loses focus.
   *
   * sendCallback is called when focus is lost, or when we are otherwise
   * triggered.
   *
   * @returns -1 on change rejected, 0 on no change required, 1 on change approved
   */

  public int sendCallback()
  {
    boolean success = false;

    /* -- */

    synchronized (this)
      {
	if (processingCallback)
	  {
	    return -1;
	  }
	
	processingCallback = true;
      }

    try
      {
	Integer currentValue;

	try
	  {
	    currentValue = getValue();
	  }
	catch (NumberFormatException ex)
	  {
	    reportError(getText() + " is not a valid number.");

	    // revert the text field

	    setValue(oldvalue);
	    return -1;
	  }

	if ((currentValue == null && oldvalue == null) ||
	    (oldvalue != null && oldvalue.equals(currentValue)))
	  {
	    if (debug)
	      {
		System.err.println("The field was not changed.");
	      }

	    return 0;
	  }

	// check to see if it's in bounds, if we have bounds set.

	if (limited)
	  {
	    int value = currentValue.intValue();

	    if ((value > maxSize) || (value < minSize))
	      {
		// nope, revert.

		reportError(getText() + " must be between " + minSize + " and  " + maxSize + ".");

		// revert

		setValue(oldvalue);
		return -1;
	      }
	  }

	// now, tell somebody, if we need to.

	if (allowCallback)
	  {
	    // Do a callback

	    if (debug)
	      {
		System.err.println("Sending callback");
	      }

	    success = false;

	    try
	      {
		success = my_parent.setValuePerformed(new JSetValueObject(this,currentValue));
	      }
	    catch (java.rmi.RemoteException re)
	      {
		// success will still be false, that's good enough for us.
	      }

	    if (!success)
	      {
		// revert

		setValue(oldvalue);
		return -1;
	      }
	    else
	      {
		// good to go.  We've already got the text set in the text
		// field, the user did that for us.  Remember the value of
		// it, so we can revert if we need to later.

		oldvalue = currentValue;
		return 1;
	      }
	  }
	else
	  {
	    // no one to say no.  Odd, guess nobody cares.. remember our
	    // value anyway.

	    oldvalue = currentValue;
	    return 1;
	  }
      }
    finally
      {
	processingCallback = false;
      }
  }

  /**
   * <p>This private helper method relays a descriptive error message to
   * our callback interface.</p>
   */

  private void reportError(String errorString)
  {
    if (allowCallback)
      {
	try
	  {
	    my_parent.setValuePerformed(new JErrorValueObject(this, errorString));
	  }
	catch (java.rmi.RemoteException rx)
	  {
	    System.err.println("Could not send an error callback.");
	  }
      }
  }
}
