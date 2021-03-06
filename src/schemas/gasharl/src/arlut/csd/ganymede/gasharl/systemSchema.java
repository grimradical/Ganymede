/*

   systemSchema.java

   An interface defining constants to be used by the system code.
   
   Created: 23 April 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.gasharl;


/*------------------------------------------------------------------------------
                                                                       interface
                                                                    systemSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the system code.
 *
 */

public interface systemSchema {

  // field id's for the system object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the system, you'll want to change
  // this file to match.

  final static short BASE=263;

  final static short OS=256;
  final static short MANUFACTURER=257;
  final static short MODEL=258;
  final static short INTERFACES=260;
  final static short SYSTEMNAME=261;
  final static short SYSTEMALIASES=262;
  final static short ROOM=264;
  final static short NETGROUPS=265;
  final static short SYSTEMTYPE=266;
  final static short PRIMARYUSER=267;
  final static short VOLUMES=268;
  final static short VIRTUAL=269;
  final static short DHCPGROUPS=270;
  final static short DHCPOPTIONS=271;
}
