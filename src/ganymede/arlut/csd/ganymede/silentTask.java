/*

   silentTask.java

   This is a simple labeling interface used to indicate to the
   GanymedeScheduler that tasks implementing this interface don't need
   to have their execution be announced to the Ganymede server's
   stdout.

   Created: 8 February 2001
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2001/02/09 00:04:45 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                      silentTask

------------------------------------------------------------------------------*/

/**
 * <p>This is a simple labeling interface used to indicate to the
 * {@link arlut.csd.ganymede.GanymedeScheduler GanymedeScheduler} that
 * tasks implementing this interface don't need to have their
 * execution be announced to the Ganymede server's stdout.</p>
 */

public interface silentTask {
}
