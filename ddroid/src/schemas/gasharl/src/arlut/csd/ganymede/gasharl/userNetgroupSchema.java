/*

   userNetgroupSchema.java

   An interface defining constants to be used by the user netgroup code.
   
   Created: 23 April 1998
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
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

package arlut.csd.ddroid.gasharl;


/*------------------------------------------------------------------------------
                                                                       interface
                                                              userNetgroupSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the user netgroup code.
 *
 */

public interface userNetgroupSchema {

  // field id's for the user netgroup object.  These should match the
  // current specs in the Directory Droid schema file precisely.  If
  // you change the schema for the user netgroup, you'll want to change
  // this file to match.

  final static short NETGROUPNAME=256;
  final static short USERS=257;
  final static short MEMBERGROUPS=258;
  final static short OWNERNETGROUPS=259;
}
