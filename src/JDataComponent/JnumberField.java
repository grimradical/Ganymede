/*
   JnumberField.java

   
   Created: 12 Jul 1996
   Version: 1.1 97/07/16
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin
*/

package arlut.csd.JDataComponent;

import com.sun.java.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.lang.Integer;
/*******************************************************************
                                                      JnumberField()
*******************************************************************/

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

    //enableEvents(AWTEvent.FOCUS_EVENT_MASK);
    enableEvents(AWTEvent.KEY_EVENT_MASK); 
  }

  /**
   * Constructor which uses default fonts,no parent,
   * default column size, and default foregound/background
   * colors.
   */
  public JnumberField()
  {
    this(JnumberField.DEFAULT_COLS,
	 true,
	 false,
	 0,Integer.MAX_VALUE);
  }

  public JnumberField(int width)
  {
    this(width,
	 true,
	 false,
	 0,0);
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
    if (((int)c > getMaxValue()) || ((int)c > getMinValue()))
      {
	return false;
      }

    if (allowedChars.indexOf(c) == -1)
      return false;

    return true;
  }


  /**
   * returns the value of this JnumberField as an Integer object
   */
  public Integer getValue()
  {
    Integer i = null;
    try
      {
	i = new Integer(getText());
      }
    catch (NumberFormatException e)
      {
	System.out.println("That's not a number.");
	i = null;
      }

    return i;
  }

  /**
   * sets the value of this JnumberField to num
   *
   * @param num the number to use
   */ 
  public void setValue(int num)
  {
    if (limited)
      {
	if (num > maxSize || num < minSize)
	  {
	    System.out.println("Invalid Parameter: number out of range");
	    return;
	  }
      }
    
    setValue(new Integer(num));

  }

  /**
   * sets the value of this JnumberField using a String object
   *
   * @param num the String object to use
   */

  public void setValue(String num)
  {
    if (num == null)
      return;

    if (num.equals(""))
      return;

    try
      {
	Integer number = new Integer(num);
	setValue(number);
      }
    catch (NumberFormatException e)
      {
	System.out.println("That's not a number.");
	if (allowCallback)
	  {
	    try
	      {
		my_parent.setValuePerformed(new JValueObject(this, 0,
							     JValueObject.ERROR,
							     "Invalid number format."));
	      }
	    catch (java.rmi.RemoteException rx)
	      {
		System.out.println("Could not send an error callback.");
	      }
	  }

	setValue(oldvalue);
	throw new IllegalArgumentException ("That String is not castable into an Integer. " + e);
      }
    
    oldvalue = getValue();

  }


  /**
   * sets the value of this JnumberField using an Integer object
   *
   * @param num the Integer object to use
   */
  public void setValue(Integer num)
  {
    if (num == null)
      return;

    setText(num.toString());
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
   */

  public void sendCallback()
  {
    Integer currentValue = getValue();
    if (currentValue == null)
      {
	System.out.println("Invalid number format.");
	if (allowCallback)
	  {
	    try
	      {
		my_parent.setValuePerformed(new JValueObject(this, 0,
							     JValueObject.ERROR,
							     "Invalid number format."));
	      }
	    catch (java.rmi.RemoteException rx)
	      {
		System.out.println("Could not send an error callback.");
	      }

	    setValue(oldvalue);
	  }
	return;
      }

    
    if ((oldvalue != null) && oldvalue.equals(currentValue))
      {
	if (debug)
	  {
	    System.out.println("The field was not changed.");
	  }
	return;
      }
    

    changed = false;

    try
      {
	setValue(Integer.valueOf(getText()).intValue());
      }
    catch (NumberFormatException ex)
      {
	if (oldvalue == null)
	  {
	    setText("");
	  }
	else
	  {
	    setValue(oldvalue.intValue());
	  }
      }
    catch (IllegalArgumentException iae)
      {
	if (oldvalue == null)
	  {
	    setText("");
	  }
	else
	  setValue(oldvalue.intValue());
      }

    if (currentValue != null && allowCallback)
      {
	//Do a callback

	System.out.println("Sending callback");

	boolean b = false;
	
	try {

	b = my_parent.setValuePerformed(new JValueObject(this,currentValue));

	}
	catch (java.rmi.RemoteException re) {

	  
	}

	if (!b) 
	  {
	    
	    if (oldvalue == null)
	      {
		setText("");
	      }
	    else
	      {
		setValue(oldvalue.intValue());
	      }
	  }
	else
	  {

	    oldvalue = currentValue;
	    changed = false;

	  }
      }
    }
}
