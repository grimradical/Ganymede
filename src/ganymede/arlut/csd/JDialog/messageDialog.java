/*

   messageDialog.java

   A dialog class used to display text and HTML with an optional
   image on the left side, used for the about.. and motd features
   in the Ganymede client.
   
   Created: 16 September 1998

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Mike Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
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

package arlut.csd.JDialog;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   messageDialog

------------------------------------------------------------------------------*/

/**
 *
 * A dialog class used to display text and HTML with an optional
 * image on the left side, used for the about.. and motd features
 * in the Ganymede client.
 *   
 * @version $Id$
 * @author Mike Mulvaney
 *
 */

public class messageDialog extends JCenterDialog implements ActionListener {

  private final static boolean debug = false;

  // ---

  JEditorPane
    text;

  JScrollPane
    scrollpane;

  JButton
    ok;

  /* -- */

  public messageDialog(JFrame frame, String title, Image image)
  {
    super(frame, title, false);	// not modal

    // There are three panels.  pane will become the content pane, so
    // it is the top container.  It contains two other panels:
    // topPanel, and buttonPanel.  topPanel contains everything but
    // the ok button, which is in the buttonPanel.  I do this because
    // it is hard to put a button in the center of a panel with the
    // GridBagLayout.

    JPanel topPanel = new JPanel(new BorderLayout()); // gbl?
    JPanel buttonPanel = new JPanel();
    JPanel pane = new JPanel(new BorderLayout());
    
    JLabel picture = null;

    if (image == null)
      {
	picture = new JLabel();
      }
    else
      {
	picture = new JLabel(new ImageIcon(image));
      }

    text = new JEditorPane();
    text.setEditable(false);
    text.setForeground(java.awt.Color.black);

    scrollpane = new JScrollPane(text);

    topPanel.add("Center", scrollpane);
    topPanel.add("West", picture);
    topPanel.add("South", new JSeparator());

    // Create the button Panel for the bottom
    ok = new JButton("Ok");
    ok.addActionListener(this);
    buttonPanel.add(ok);

    pane.add("Center", topPanel);
    pane.add("South", buttonPanel);
    this.setContentPane(pane);

    layout(450,200);
  }

  /**
   *
   * Load this message dialog with HTML content.
   *
   * @param s An HTML document held within a string.
   *
   */

  public void setHtmlText(String s)
  {
    if (debug)
      {
	System.out.println("Setting text to: " + s);
      }
    
    text.setContentType("text/html");
    text.setText(s);
    layout(550,400);
    scrollpane.getViewport().setViewPosition(new Point(0,0));
  }

  /**
   *
   * Load this message dialog with Unicode content.
   *
   * @param s The message content to be displayed.
   *
   */

  public void setPlainText(String s)
  {
    text.setContentType("text/plain");
    text.setText(s);
    layout(550,400);
    scrollpane.getViewport().setViewPosition(new Point(0,0));
  }

  public void setVisible(boolean state)
  {
    super.setVisible(state);

    if (state)
      {
        scrollpane.getViewport().setViewPosition(new Point(0,0));
	ok.requestFocus();
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    setVisible(false);
  }

  public void layout(int width, int height)
  {
    // Uses a special pack in JCenterDialog
    pack(width, height);
  }
}
