/*

   stringThing.java

   Resource class for use with StringDialog.java
   
   Created: 16 June 1997
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 1999/06/15 02:46:49 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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

package arlut.csd.JDialog;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     stringThing

------------------------------------------------------------------------------*/

/**
 * <P>Serializable object to describe a string field for passing to the
 * client as part of a {@link arlut.csd.JDialog.JDialogBuff JDialogBuff}
 * or {@link arlut.csd.JDialog.StringDialog StringDialog}.</P> 
 */

public class stringThing implements java.io.Serializable {

  String stringLabel;
  String value;
  boolean multiline;

  /* -- */

  public stringThing(String label, String value, boolean multiline)
  {
    this.value = value;
    this.stringLabel = label;
    this.multiline = multiline;
  }

  public String getLabel()
  {
    return stringLabel;
  }

  public String getValue()
  {
    return value;
  }

  public boolean isMultiline()
  {
    return multiline;
  }
}