/*
   JfloatField.java

   
   Created: 29 October 1999
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 2002/10/05 07:26:45 $
   Module By: Navin Manohar, Jonathan Abbey, Michael Mulvaney, John Knutson

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002
   The University of Texas at Austin.

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

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.lang.Double;

/*-----------------------------------------------------------------------------
                                                                          class
                                                                    JfloatField

-----------------------------------------------------------------------------*/

/**
 * This class defines a GUI field component that is capable of
 * handling Java doubles.  The maximum and minimum bounds for the
 * range of doubles that can be entered into this JfloatField can also
 * be preset.  
 */

public class JfloatField extends JentryField {

  public static int DEFAULT_COLS = 20;

  public static String allowedChars = new String("0123456789-.");

  private Double storedValue;

  private boolean limited = false;

  private boolean processingCallback = false;

  private double maxSize;
  private double minSize;

  ///////////////////
  //  Constructors //
  ///////////////////

  /**
   * Base constructor for JfloatField
   * 
   * @param columns number of columns in the JfloatField
   * @param is_editable true if this JfloatField is editable
   * @param islimited true if there is a restriction on the range of values
   * @param minsize the minimum limit on the range of values
   * @param maxsize the maximum limit on the range of values
   */ 
  public JfloatField(int columns,
		     boolean iseditable,
		     boolean islimited,
		     double minsize,
		     double maxsize)
  {
    super(columns);
    
    if (islimited)
      {
	limited = true;
	
	maxSize = maxsize;
	minSize = minsize;
      }
    
    setEditable(iseditable);  // will this JfloatField be editable or not?
  }

  /**
   * Constructor which uses takes a number of columns, and everything
   * else default.
   */

  public JfloatField(int width)
  {
    this(width,
	 true,
	 false,
	 Double.MIN_VALUE,Double.MAX_VALUE);
  }

  /**
   * Constructor which uses default everything.
   */

  public JfloatField()
  {
    this(JfloatField.DEFAULT_COLS);
  }
 
  /**
   * Constructor that allows for the creation of a JfloatField
   * that knows about its parent and can invoke a callback method.
   *  
   * @param columns number of columns in the JfloatField
   * @param is_editable true if this JfloatField is editable
   * @param islimited true if there is a restriction on the range of values
   * @param minsize the minimum limit on the range of values
   * @param maxsize the maximum limit on the range of values
   * @param parent the container within which this JfloatField is contained
   *        (This container will implement an interface that will utilize the
   *         data contained within this JfloatField.)
   *
   */ 
  public JfloatField(int columns,
		     boolean iseditable,
		     boolean limited,
		     double minsize,
		     double maxsize,
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
	    System.err.println("JfloatField.isAllowed(): ruling NO WAY on char '" + c + "'");
	  }

	return false;
      }

    return true;
  }

  /**
   * returns the value of this JfloatField as an Integer object
   *
   * If this field is empty, will return null. If this field is
   * not empty and has a non-numeric string, will throw a
   * NumberFormatException.
   *
   */

  public Double getValue() throws NumberFormatException
  {
    String str = getText();

    if (str == null || str.equals(""))
      {
	return null;
      }

    return new Double(str);
  }

  /**
   * sets the value of this JfloatField to num
   *
   * This method does not trigger a callback to our container.. we
   * only callback as a result of loss-of-focus brought on by the
   * user.
   *
   * @param num the number to use
   */ 

  public void setValue(double num)
  {
    setValue(new Double(num));
  }

  /**
   * sets the value of this JfloatField using an Double object.
   *
   * This method does not trigger a callback to our container.. we
   * only callback as a result of loss-of-focus brought on by the
   * user.
   *
   * @param num the Double object to use
   */

  public void setValue(Double num)
  {
    if (limited)
      {
	if (num != null)
	  {
	    if (num.doubleValue() > maxSize || num.doubleValue() < minSize)
	      {
		System.out.println("Invalid Parameter: float out of range");
		return;
	      }
	  }
      }

    // remember the value that is being set.
    
    storedValue = num;
    
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
   * Sets the limited/non-limited status of this JfloatField
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
  public void setMaxValue(double n)
  {
    limited = true;
    
    maxSize = n;
  }
  
  /**
   *  sets the minimum value in the range of possible values.
   *
   * @param n the number to use when setting the minimum value
   */
  public void setMinValue(double n)
  {
    limited = true;
    
    minSize = n;
  }

  /**
   * returns true if there is a bound on the range of values that
   * can be entered into this JfloatField
   */
  public boolean isLimited()
  {
    return limited;
  }
  
  /**
   * returns the maximum value in the range of valid values for this
   * JfloatField
   */
  public double getMaxValue()
    {
      return maxSize;
    }

  /**
   * returns the minimum value in the range of valid values for this
   * JfloatField
   */
  public double getMinValue()
  {
    return minSize;
  }

  /**
   * overrides JentryField.sendCallback().
   *
   * This is called when the float field loses focus.
   *
   * sendCallback is called when focus is lost, or when we are otherwise
   * triggered.
   *
   * @returns -1 on change rejected, 0 on no change required, 1 on change approved
   */

  public int sendCallback()
  {
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
	Double currentValue;

	try
	  {
	    currentValue = getValue();
	  }
	catch (NumberFormatException ex)
	  {
	    reportError(getText() + " is not a properly formatted floating point number.");

	    // revert the text field

	    setValue(storedValue);
	    return -1;
	  }

	if ((currentValue == null && storedValue == null) ||
	    (storedValue != null && storedValue.equals(currentValue)))
	  {
	    if (debug)
	      {
		System.out.println("The field was not changed.");
	      }

	    return 0;
	  }

	// check to see if it's in bounds, if we have bounds set.

	if (limited)
	  {
	    double value = currentValue.doubleValue();

	    if ((value > maxSize) || (value < minSize))
	      {
		// nope, revert.

		reportError(getText() + " must be between " + minSize + " and  " + maxSize + ".");

		setValue(storedValue);
		return -1;
	      }
	  }

	// now, tell somebody, if we need to.

	try
	  {
	    if (!allowCallback || my_parent.setValuePerformed(new JSetValueObject(this,currentValue)))
	      {
		// good to go.  We've already got the text set in the text
		// field, the user did that for us.  Remember the value of
		// it, so we can revert if we need to later.
		
		storedValue = currentValue;
		return 1;
	      }
	    else
	      {
		setValue(storedValue);
		return -1;
	      }
	  }
	catch (java.rmi.RemoteException re)
	  {
	    return -1;
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
	    System.out.println("Could not send an error callback.");
	  }
      }
  }
}
