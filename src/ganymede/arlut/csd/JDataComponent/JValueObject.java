
/*
   JValueObject.java

   This class is primary a holder object that is used to
   pass scalar and vector values and vector operation
   instructions using a callback method to the Object designated
   as an appropriate callback handler.

   Created: 28 Feb 1997
   Release: $Name:  $
   Version: $Revision: 1.13 $
   Last Mod Date: $Date: 2001/06/29 07:31:05 $
   Module By: Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.JDataComponent;

import java.awt.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    JValueObject

------------------------------------------------------------------------------*/

/**
 *
 * <p>A client-side message object used to pass status updates from
 * GUI components in the arlut.csd.JDataComponent package to their
 * containers.  JValueObject supports passing information about
 * scalar and vector value change operations, as well as pop-up
 * menus and error messages.</p>
 *
 * <p>Note that we came up with this message type before Sun introduced
 * the 1.1 AWT event model.  Great minds... ;-)</p>
 *   
 * @version $Revision: 1.13 $ $Date: 2001/06/29 07:31:05 $ $Name:  $
 * @author Navin Manohar 
 */

public class JValueObject {

  /**
   * Boundary guard for acceptable operation types.
   */

  public static final int FIRST = -1012;

  /**
   * Vector add/scalar operation.  Also used to indicate item selection in JstringListBox.
   */

  public static final int ADD = -1001;

  /**
   * Vector add/vector operation.  Also used to indicate item selection in JstringListBox.
   */

  public static final int ADDVECTOR = -1009;

  /**
   * Vector insert operation, requires both value and index
   * to be set.  Also used to indicate item double-click in JstringListBox.
   */

  public static final int INSERT = -1002;

  /**
   * Vector delete scalar operation, requires index to be set.
   */

  public static final int DELETE = -1003;

  /**
   * Vector delete vector operation, requires index to be set.
   */

  public static final int DELETEVECTOR = -1010;

  /**
   * Property set operation, value = key, parameter = value, parameter2
   * = old value, if we are changing an old key/value association.
   */

  public static final int SETPROPERTY = -1012;

  /**
   * Scalar value set operation.  Requires value to be set.
   */

  public static final int SET = -1004;

  /**
   * The operation value to use when you're not using a JValueObject.
   */

  public static final int NONE = -1005;

  /**
   * Error message return operation.  Requires value to be
   * set to a string describing the error.
   */

  public static final int ERROR = -1006;

  /**
   * Use this for those hacks
   */

  public static final int SPECIAL = -1007;  

  /**
   * Used to pass action commands (as from pop-up menu activity) from GUI
   * components.
   */

  public static final int PARAMETER = -1008;

  /**
   * Used to signal an item being moved in a list.
   */

  public static final int MOVE = -1011;

  /**
   * Boundary guard for acceptable operation types.
   */

  public static final int LAST = -1000;

  /**
   * The arlut.csd.JDataComponent GUI component that originated this message.
   */

  private Component source;

  /**
   * An enumerated operation type indicator.  Should be one of ADD, INSERT,
   * DELETE, SET, ERROR, SPECIAL, and PARAMETER.
   */

  private int operationValue = NONE;

  /**
   * A multi-purpose value object.  Interpreted differently for different operation types.
   */

  private Object value = null;

  /**
   * An auxiliary value used for some kinds of operation types.  Often used to indicate
   * ActionCommands for pop-up menus attached to arlut.csd.JDataComponent GUI components.
   */

  private Object parameter = null;

  /**
   * An additional auxiliary value used for some kinds of operation
   * types.  Used by the SETPROPERTY operation to record the old value of a property
   * if we are changing an existing property rather than adding a new one.
   */

  private Object parameter2 = null;

  /**
   * Index used to indicate what value of a vector is being modified by this message.
   */

  private int index;

  /* -- */

  /**
   *
   * Constructor for a simple value-set message.
   *
   * @param source arlut.csd.JDataComponent GUI component originating message
   * @param value Value being set by the originating GUI component
   *
   */

  public JValueObject(Component source, Object value)
  {
    this.source = source;
    this.value = value;
    operationValue = SET;
  }

  /**
   *
   * Constructor for a simple single-value message.
   *
   * @param source arlut.csd.JDataComponent GUI component originating message
   * @param value Value being set by the originating GUI component
   * @param operation Operation type, one of ADD, INSERT, DELETE, SET,
   * ERROR, SPECIAL, PARAMETER, MOVE.
   *
   */

  public JValueObject(Component source, Object value, int operation)
  {
    this.source = source;
    this.value = value;
    operationValue = operation;
  }

  /**
   *
   * Constructor for a simple vector change message.
   *
   * @param source arlut.csd.JDataComponent GUI component originating message
   * @param index index of vector connected to arlut.csd.JDataComponent GUI component being changed.
   * @param operation Operation type, one of ADD, INSERT, DELETE, SET,
   * ERROR, SPECIAL, PARAMETER, MOVE.
   *
   */
  
  public JValueObject(Component source, int index, int operation)
  {
    this.source = source;
    this.index = index;

    if ((operation > LAST) || (operation < FIRST))
      {
	throw new IllegalArgumentException("Illegal Argument: operation has invalid value: " + operation);
      }
    
    this.operationValue = operation;
  }

  /**
   *
   * Constructor for a simple vector change message.
   *
   * @param source arlut.csd.JDataComponent GUI component originating message
   * @param index index of vector connected to arlut.csd.JDataComponent GUI component being changed.
   * @param value Value being set by the originating GUI component
   * @param operation Operation type, one of ADD, INSERT, DELETE, SET,
   * ERROR, SPECIAL, PARAMETER, MOVE.
   *
   */

  public JValueObject(Component source, int index, int operation, Object value)
  {
    this(source, index, operation, value, null);
  }

  /**
   *
   * Constructor for a vector move message.
   *
   * @param source arlut.csd.JDataComponent GUI component originating message
   * @param index index of vector connected to arlut.csd.JDataComponent GUI component being changed.
   * @param value Value being set by the originating GUI component
   * @param operation Operation type, one of ADD, INSERT, DELETE, SET,
   * ERROR, SPECIAL, PARAMETER, MOVE.
   *
   */

  public JValueObject(Component source, int index, int operation, int targetIndex)
  {
    this(source, index, operation, new Integer(targetIndex), null);
  }

  /**
   *
   * Generic constructor
   *
   * @param source arlut.csd.JDataComponent GUI component originating message
   * @param index index of vector connected to arlut.csd.JDataComponent GUI component being changed.
   * @param value Value being set by the originating GUI component
   * @param operation Operation type, one of ADD, INSERT, DELETE, SET,
   * ERROR, SPECIAL, PARAMETER.
   * @param parameter Auxiliary object value, used when passing pop-up menu information.
   *
   */

  public JValueObject(Component source, int index, int operation, Object value, Object parameter)
  {
    this.source = source;
    this.index = index;
    this.parameter = parameter;
    
    if ((operation < FIRST) || (operation > LAST))
      {
	throw new IllegalArgumentException("Illegal Argument: operation has invalid value: " + operation);
      }
    
    this.operationValue = operation;
    
    this.value = value;
  }


  /**
   *
   * Generic constructor
   *
   * @param source arlut.csd.JDataComponent GUI component originating message
   * @param index index of vector connected to arlut.csd.JDataComponent GUI component being changed.
   * @param value Value being set by the originating GUI component, or the key in a property operation
   * @param operation Operation type, one of ADD, INSERT, DELETE, SET,
   * ERROR, SPECIAL, PARAMETER, SETPROPERTY
   * @param parameter Auxiliary object value, used when passing pop-up menu information, or the value
   * in a property operation
   * @param parameter2 Additional auxiliary object value, used to represent the old value
   * when a SETPROPERTY operation is replacing an old value.
   *
   */

  public JValueObject(Component source, int index, int operation, Object value, Object parameter,
		      Object parameter2)
  {
    this.source = source;
    this.index = index;
    this.parameter = parameter;
    this.parameter2 = parameter2;
    
    if ((operation < FIRST) || (operation > LAST))
      {
	throw new IllegalArgumentException("Illegal Argument: operation has invalid value: " + operation);
      }
    
    this.operationValue = operation;
    
    this.value = value;
  }

  /**
   * Returns the arlut.csd.JDataComponent GUI component that originated this message.
   */

  public Component getSource()
  {
    return source;
  }

  /**
   * Returns an auxiliary value.  Used for passing information about pop-up menu items, but may
   * be used for different purposes if needed.
   */

  public Object getParameter() 
  {
    return parameter;
  }

  /**
   * Returns an auditional auxiliary value.  Used for passing the old
   * value of a property's value when a property entry is being
   * changed (rather than adding a new property/value pair).
   */

  public Object getParameter2()
  {
    return parameter2;
  }

  /**
   * Returns the index of an item operated on in a vector component.
   */

  public int getIndex() 
  {
    return index;
  }

  /**
   * Returns the value of the object being affected by this message.
   */

  public Object getValue() 
  {
    return value;
  }

  /**
   *
   * Returns the type of operation encoded by this message.
   *
   */

  public int getOperationType() 
  {
    return operationValue;
  }

  /**
   *
   * Method to get a human-readable description of the event carried
   * by this object
   * 
   */

  public String toString()
  {
    String result;

    /* -- */

    result = source.toString();

    switch (operationValue)
      {
      case ADD:
	
	result += " add element " + index;
	
	break;

      case INSERT:

	result += " insert element " + index;

	break;

      case DELETE:

	result += " delete element " + index;

	break;

      case SET:

	result += " set value ";
	result += String.valueOf(value);

	break;

      case SETPROPERTY:

	result += " set property";
	result += " key: " + String.valueOf(value);
	result += ", new value: " + String.valueOf(parameter);

	if (parameter2 != null)
	  {
	    result += ", old value: " + String.valueOf(parameter2);
	  }

	break;

      case NONE:

	result += " none";

	break;
      }

    return result;
  }
}
