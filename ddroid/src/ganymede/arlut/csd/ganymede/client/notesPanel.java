  /*

   notesPanel.java

   The frame containing the notes panel
   
   Created: 4 September 1997

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
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

package arlut.csd.ganymede.client;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.rmi.RemoteException;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import arlut.csd.ganymede.rmi.string_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      notesPanel

------------------------------------------------------------------------------*/

/**
 * Notes panel for use in {@link arlut.csd.ganymede.client.framePanel framePanel}'s
 * in the client's display.  This panel is only created when a user clicks on
 * a Notes tab in a viewed or edited object window in the client.  Unlike most
 * GUI components in the client that are connected to database fields on the server,
 * the notesPanel doesn't automatically update the server on focus loss.  Instead,
 * notesPanel currently depends on gclient's 
 * {@link arlut.csd.ganymede.client.gclient#commitTransaction() commitTransaction()}
 * method to poll all notesPanels open and active for their contents.  This really
 * should be changed, as it means that currently an edit object window which is
 * manually closed will not have its notes field updated on transaction commit.
 */

public class notesPanel extends JPanel implements KeyListener {

  boolean debug = false;
  
  JTextArea
    notesArea;

  framePanel 
    fp;

  string_field
    notes_field;

  private boolean local_editable;

  /*--*/

  public notesPanel(string_field notes_field, boolean editable, framePanel fp)
  {
    debug = fp.debug;

    if (debug)
      {
	System.out.println("Creating notes panel");
      }
      
    this.fp = fp;
    this.notes_field = notes_field;

    setBorder(fp.wp.emptyBorder5);
      
    setLayout(new BorderLayout());
    
    notesArea = new JTextArea();
    EmptyBorder eb = fp.wp.emptyBorder5;
    TitledBorder tb = new TitledBorder("Notes");
    notesArea.setBorder(new CompoundBorder(tb,eb));
    
    local_editable = editable;

    if (local_editable)
      {
	try
	  {
	    local_editable = notes_field.isEditable();
	  }
	catch (RemoteException ex)
	  {
	    local_editable = false;
	  }
      }

    notesArea.setEditable(local_editable);

    if (local_editable)
      {
	notesArea.addKeyListener(this);
      }
    
    JScrollPane notesScroll = new JScrollPane(notesArea);
    add(BorderLayout.CENTER, notesScroll);
    
    if (notes_field != null)
      {
	try
	  {
	    String s = (String)notes_field.getValue();

	    if (s != null)
	      {
		notesArea.append(s);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get note text: " + rx);
	  }
      }
  }

  /**
   * Transmit the contents of this notes panel to the server to update
   * the notes field for the proper database object.
   */

  public void updateNotes()
  {
    if (!local_editable)
      {
	return;
      }

    try
      {
	if (notes_field != null)
	  {
	    if (debug)
	      {
		System.out.println("Updating notes: " + notesArea.getText().trim());
	      }

	    notes_field.setValue(notesArea.getText().trim());
	  }
	else if (debug)
	  {
	    System.out.println("notes_field is null, not updating.");
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not set notes field: " + rx);
      }
  }

  public void keyPressed(KeyEvent e)
  {
    fp.gc.somethingChanged();
    notesArea.removeKeyListener(this);
  }
  
  public void keyReleased(KeyEvent e) {}
  public void keyTyped(KeyEvent e) {}
}