/*

   ip_field.java

   Remote interface definition.

   Created: 4 Sep 1997
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/22 18:05:58 $
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

package arlut.csd.ganymede;

import java.rmi.*;

public interface ip_field extends db_field {

  /**
   *
   * Returns true if this field is permitted to hold IPv6 addresses.
   *
   */

  boolean v6Allowed() throws RemoteException;
  
  /**
   *
   * Returns true if the (scalar) value stored in this IP field is an
   * IPV6 address
   * 
   */

  boolean isIPV6() throws RemoteException;

  /**
   *
   * Returns true if the (scalar) value stored in this IP field is an
   * IPV6 address
   * 
   */

  boolean isIPV4() throws RemoteException;

  /**
   *
   * Returns true if the value stored in the given element of this IP
   * field is an IPV6 address.
   *
   * @param index Array index for the value to be checked
   *
   */

  boolean isIPV6(short index) throws RemoteException;

  /**
   *
   * Returns true if the value stored in the given element of this IP
   * field is an IPV4 address.
   *
   * @param index Array index for the value to be checked
   *
   */

  boolean isIPV4(short index) throws RemoteException;
}