/*

   fieldDeltaRec.java

   The fieldDeltaRec class is used to record the changes that have
   been made to a particular field in a DBObject.  This class is used
   by the DBObjectDeltaRec class to keep track of changes to fields.
   
   Created: 7 July 1998
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
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

package arlut.csd.ddroid.server;

import arlut.csd.ddroid.common.*;
import arlut.csd.ddroid.rmi.*;

import arlut.csd.Util.VectorUtils;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   fieldDeltaRec

------------------------------------------------------------------------------*/

/**
 *
 * The fieldDeltaRec class is used to record the changes that have
 * been made to a particular field in a DBObject.  This class is used
 * by the DBObjectDeltaRec class to keep track of changes to fields.
 *
 * @see arlut.csd.ddroid.server.DBObjectDeltaRec
 * @see arlut.csd.ddroid.server.DBField
 * 
 */

class fieldDeltaRec {

  short fieldcode;
  boolean vector;
  DBField scalarValue = null;
  Vector addValues = null;
  Vector delValues = null;

  /* -- */

  /**
   * Scalar value constructor.  This constructor may actually be used
   * for vector fields when those vector fields are newly defined.. in
   * this case, we are actually doing a complete definition of the
   * field, rather than just a vector add/remove record.<br><br>
   *
   * If &lt;scalar&gt; is null, this fieldDeltaRec is recording the
   * deletion of a field.
   * 
   */

  fieldDeltaRec(short fieldcode, DBField scalar)
  {
    this.fieldcode = fieldcode;
    this.scalarValue = scalar;
    vector = false;
  }

  /**
   *
   * Vector constructor.  This constructor is used when we are doing a
   * vector differential record.
   *
   */

  fieldDeltaRec(short fieldcode)
  {
    this.fieldcode = fieldcode;
    vector = true;
    addValues = new Vector();
    delValues = new Vector();
  }

  /**
   *
   * This method is used to record a value that has been added
   * to this vector field.
   * 
   */

  void addValue(Object value)
  {
    if (value instanceof Byte[])
      {
	value = new IPwrap((Byte []) value);
      }

    if (delValues.contains(value))
      {
	delValues.removeElement(value);
      }
    else if (!addValues.contains(value))
      {
	addValues.addElement(value);
      }
  }

  /**
   *
   * This method is used to record a value that has been removed
   * from this vector field.
   * 
   */

  void delValue(Object value)
  {
    if (value instanceof Byte[])
      {
	value = new IPwrap((Byte []) value);
      }

    if (addValues.contains(value))
      {
	addValues.removeElement(value);
      }
    else if (!delValues.contains(value))
      {
	delValues.addElement(value);
      }
  }

  /**
   * <p>This method generates a diagnostic representation of this fieldDeltaRec.</p>
   *
   * <p>This method will probably fail with an exception if the field 'scalarValue'
   * has not been initialized with a plausible owner.</p>
   */

  public String toString()
  {
    if (!vector)
      {
	if (scalarValue == null)
	  {
	    return "<field: " + fieldcode + ", *deleting*>";
	  }

	return "<field: " + fieldcode + ", new val = " + scalarValue.getValueString() + ">";
      }

    StringBuffer result = new StringBuffer();

    result.append("<field: ");
    result.append(fieldcode);
    result.append(", adding: ");
    result.append(VectorUtils.vectorString(addValues));
    result.append(", deleting: ");
    result.append(VectorUtils.vectorString(delValues));

    return result.toString();
  }
}
