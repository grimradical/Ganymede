/*
   GASH 2

   BigPipedInputStream

   The GANYMEDE object storage system.

   Created: 7 March 2000

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2004
   The University of Texas at Austin

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

package arlut.csd.Util;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       XMLReader

------------------------------------------------------------------------------*/

/**
 * This class is a simple subclassing of java.io.PipedInputStream that specifies
 * a much larger buffer size, in order to make server-side XML parsing efficient.
 */

public class BigPipedInputStream extends PipedInputStream {

  protected byte buffer[] = new byte[1048576]; // 16 * 64k

  /* -- */

  public BigPipedInputStream(PipedOutputStream src) throws IOException {
    connect(src);
  }
}