/*

   windowPanel.java

   The window that holds the frames in the client.
   
   Created: 11 July 1997
   Version: $Revision: 1.35 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import tablelayout.*;
import com.sun.java.swing.*;
import com.sun.java.swing.border.*;
import com.sun.java.swing.event.*;


import java.awt.*;
import java.beans.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import jdj.PackageResources;

import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     windowPanel

------------------------------------------------------------------------------*/

/** 
 * windowPanel is the top level window controlling the framePanels.  There is one
 * windowPanel for each client.  windowPanel is responsible for adding new windows,
 * and maintaining the window list in the menubar.  
 */

public class windowPanel extends JDesktopPane implements PropertyChangeListener, ActionListener{  
  boolean debug = true;
  
  // --

  gclient
    gc;

  int 
    topLayer = 0,
    windowCount = 0;

  Hashtable
    menuItems = new Hashtable(),
    Windows = new Hashtable(),
    windowList = new Hashtable();
  
  // This is used as the wait image in other classes.  Currently, it
  // returns the men at work animated gif.  Keep it here so each
  // subsequent pane doesn't have to load it.

  Image
    waitImage = null;

  JMenu
    windowMenu;

  // Load images for other packages
  ImageIcon
    // These are all for vectorPanel
    openIcon = new ImageIcon(PackageResources.getImageResource(this, "macdown.gif", getClass())),
    closeIcon = new ImageIcon(PackageResources.getImageResource(this, "macright.gif", getClass())),
    removeImageIcon = new ImageIcon(PackageResources.getImageResource(this, "x.gif", getClass()));

  LineBorder
    blackLineB = new LineBorder(Color.black);

  EmptyBorder
    emptyBorder3 = (EmptyBorder)BorderFactory.createEmptyBorder(3,3,3,3),
    emptyBorder5 = (EmptyBorder)BorderFactory.createEmptyBorder(5,5,5,5),
    emptyBorder10 = (EmptyBorder)BorderFactory.createEmptyBorder(10,10,10,10),
    emptyBorder15 = (EmptyBorder)BorderFactory.createEmptyBorder(15,15,15,15);
  
  //BevelBorder
  //raisedBorder = new BevelBorder(BevelBorder.RAISED);
  
  CompoundBorder
  //emptyButtonBorder = new CompoundBorder(emptyBorder15, raisedBorder),
    eWrapperBorder = new CompoundBorder(emptyBorder3, new LineBorder(ClientColor.vectorTitles, 2)),
    lineEmptyBorder = new CompoundBorder(blackLineB, emptyBorder15);

  /* -- */

  /**
   *
   * windowPanel constructor
   *
   */

  public windowPanel(gclient gc, JMenu windowMenu)
  {
    if (debug)
      {
	System.out.println("Initializing windowPanel");
      }

    this.gc = gc;
    debug = gc.debug;
    this.windowMenu = windowMenu;

    setBackground(ClientColor.background);

  }

  /**
   * Get the parent gclient
   */

  public gclient getgclient()
  {
    return gc;
  }

  /**
   * Returns an image used as a generic "wait" image.
   *
   * Currently returns the men-at-work image.
   */

  public Image getWaitImage()
  {
    if (waitImage == null)
      {
	waitImage = PackageResources.getImageResource(this, "atwork01.gif", getClass());
      }
    
    return waitImage;
  }


  /**
   *
   * Create a new view-only window in this windowPanel.
   *
   * @param object an individual object from the server to show
   * in this window
   *
   */

  public void addWindow(db_object object)
  {
    this.addWindow(object, false, null);
  }


  /**
   *
   * Create a new window in this windowPanel.
   *
   * @param object an individual object from the server to show
   * in this window
   * @param editable If true, the new window will be editable
   */

  public void addWindow(db_object object, boolean editable)
  {
    this.addWindow(object, editable, null);
  }

  /**
   *
   * Create a new editable or view-only window in this windowPanel.
   *
   * @param object an individual object from the server to show
   * in this window
   * @param editable if true, the object will be presented as editable
   * @param objectType Used for the title of the new window
   *
   */

  public void addWindow(db_object object, boolean editable, String objectType)
  {
    String temp, title;

    if (object == null)
      {
	gc.showErrorMessage("null object passed to addWindow.");
	return;
      }

    if (editable)
      {
	gc.cancel.setEnabled(true);
      }

    gc.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    if (editable)
      {
	setStatus("Opening object for edit");
      }
    else
      {
	setStatus("Opening object for viewing");
      }

    // First figure out the title, and put it in the hash
    
    try
      {
	if (objectType == null)
	  {
	    title = object.getLabel();
	  }
	else
	  {
	    title = objectType + " - " + object.getLabel();
	  }

	// If we can't get a label, assume this is a newly created
	// object.  Is that a safe assumption?
	if ((title == null) || (title.equals("null")) || (title.equals("")))
	  {
	    if (objectType == null)
	      {
		title = "Create: New Object";
	      }
	    else
	      {
		title = "Create: " + objectType + " - " + "New Object";
	      }
	  }
	else
	  {
	    if (editable)
	      {
		title = "Edit: " + title;
	      }
	    else
	      {
		title = "View: " + title;
	      }
	  }

	if (debug)
	  {
	    System.out.println("Setting title to: " + title);
	  }

      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get label of object: " + rx);
      }
    
    // Create a unique title for the new window

    temp = title;
    int num = 2;

    while (windowList.containsKey(title))
      {
	title = temp + num++;
      }

    framePanel w = new framePanel(object, editable, this, title);

    windowList.put(title, w);
      
    if (windowCount > 10)
      {
	windowCount = 0;
      }
    else
      {
	windowCount++;
      }

    w.setBounds(windowCount*20, windowCount*20, 500,350);

    w.setLayer(new Integer(topLayer));
    
    add(w);
    setSelectedWindow(w);

    updateMenu();
    
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  public void setSelectedWindow(JInternalFrame window)
  {
    Enumeration windows = windowList.keys();
    
    while (windows.hasMoreElements())
      {
	JInternalFrame w = (JInternalFrame)windowList.get(windows.nextElement());
	try
	  {
	    w.setSelected(false);
	  }
	catch (java.beans.PropertyVetoException e)
	  {
	    System.out.println("Could not set selected false.  sorry.");
	  }
      }


    window.moveToFront();
    
    try
      {
	getDesktopManager().deiconifyFrame(window);
	window.setSelected(true);
	window.toFront();
      }
    catch (java.beans.PropertyVetoException e)
      {
	System.out.println("Could not set selected false.  sorry.");
      }
  }

  /**
   * Calls gclient.setStatus
   */

  public final void setStatus(String s)
  {
    gc.setStatus(s);
  }

  public void resetWindowCount()
  {
    windowCount = 0;
  }

  /**
   * Add a table window.  Usually the output of a query.
   */

  public void addTableWindow(Session session, Query query, DumpResult results, String title)
  {
    gResultTable 
      rt = null;

    String 
      temp = title;

    int
      num;

    /* -- */

    setStatus("Querying object types");
    gc.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    try
      {
	rt = new gResultTable(this, session, query, results);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("could not make results table: " + rx);
      }

    if (rt == null)
      {
	if (debug)
	  {
	    System.out.println("rt == null");
	  }

	setStatus("Could not get the result table.");
      }
    else
      {
	rt.setLayer(new Integer(topLayer));
	rt.setBounds(windowCount*20, windowCount*20, 500,500);
	rt.setResizable(true);
	rt.setClosable(true);
	rt.setMaximizable(true);
	rt.setIconifiable(true);

	rt.addPropertyChangeListener(this);

	if (windowCount > 10)
	  {
	    windowCount = 0;
	  }
	else
	  {
	    windowCount++;
	  }

	// Figure out the title

	temp = title;
	num = 2;

	while (windowList.containsKey(title))
	  {
	    title = temp + num++;
	  }
	  
	// System.out.println("Setting title for query table to " + title);

	rt.setTitle(title);

	windowList.put(title, rt);
	  
	add(rt);
	setSelectedWindow(rt);

	updateMenu();
	setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	setStatus("Done.");
      }
    
  }

  // This makes a menu bar for the top of the JInternalFrames
  JMenuBar createMenuBar(boolean editable, db_object object, JInternalFrame w)
  {
    // Adding a menu bar, checking it out
    JMenuBar menuBar = new JMenuBar();
    menuBar.setBorderPainted(true);
    //menuBar.setBackground(ClientColor.WindowBG.darker());
    
    JMenu fileM = new JMenu("File");
    JMenu editM = new JMenu("Edit");
    menuBar.add(fileM);
    menuBar.add(editM);
    
    JMenuItem iconifyMI = new JMenuItem("Iconify");
    menuItems.put(iconifyMI, object);
    Windows.put(iconifyMI, w);
    iconifyMI.addActionListener(this);

    JMenuItem closeMI = null;
    closeMI = new JMenuItem("Close");
    menuItems.put(closeMI , object);
    Windows.put(closeMI, w);
    closeMI.addActionListener(this);

    JMenu deleteM = new JMenu("Delete");
    JMenuItem reallyDeleteMI = new JMenuItem("Yes, I'm sure");
    deleteM.add(reallyDeleteMI);
    menuItems.put(reallyDeleteMI, object);
    Windows.put(reallyDeleteMI, w);
    reallyDeleteMI.setActionCommand("ReallyDelete");
    reallyDeleteMI.addActionListener(this);
      
    JMenuItem inactivateMI = new JMenuItem("Inactivate");
    Windows.put(inactivateMI, w);
    menuItems.put(inactivateMI, object);
    inactivateMI.addActionListener(this);

    JMenuItem refreshMI = new JMenuItem("Refresh");
    Windows.put(refreshMI, w);
    menuItems.put(refreshMI, object);
    refreshMI.addActionListener(this);

    JMenuItem setExpirationMI = null;
    JMenuItem setRemovalMI = null;
    if (editable)
      {
	setExpirationMI = new JMenuItem("Set Expiration Date");
	Windows.put(setExpirationMI, w);
	menuItems.put(setExpirationMI, object);
	setExpirationMI.addActionListener(this);
	
        setRemovalMI = new JMenuItem("Set Removal Date");
	Windows.put(setRemovalMI, w);
	menuItems.put(setRemovalMI, object);
	setRemovalMI.addActionListener(this);
      }

    JMenuItem printMI = new JMenuItem("Print");
    Windows.put(printMI, w);
    menuItems.put(printMI, object);
    printMI.addActionListener(this);

    fileM.add(inactivateMI);
    fileM.add(iconifyMI);
    fileM.add(deleteM);
    fileM.add(printMI);
    fileM.add(refreshMI);
    if (editable)
      {
	fileM.addSeparator();
	fileM.add(setExpirationMI);
	fileM.add(setRemovalMI);
      }

    fileM.addSeparator();
    fileM.add(closeMI);

    JMenuItem queryMI = new JMenuItem("Query");
    Windows.put(queryMI, w);
    queryMI.addActionListener(this);
    menuItems.put(queryMI , object);
    JMenuItem editMI = new JMenuItem("Edit");
    Windows.put(editMI, w);
    menuItems.put(editMI , object);
    editMI.setEnabled(!editable);
    editMI.addActionListener(this);
    
    editM.add(queryMI);
    editM.add(editMI);

    if (debug)
      {
	System.out.println("Returning menubar.");
      }
    
    return menuBar;
  }

  /**
   * Closes all the windows
   *
   */

  public void closeAll()
  {
    Enumeration windows = windowList.keys();      

    try
      {
	while (windows.hasMoreElements())
	  {
	    JInternalFrame w = (JInternalFrame)windowList.get(windows.nextElement());
	    w.setClosed(true);
	  }
      }
    catch (java.beans.PropertyVetoException ex)
      {
	throw new RuntimeException("beans? " + ex);
      }
  }

  /**
   * Returns a vector of framePanels of all the editable windows.
   */

  public Vector getEditables()
  {
    Vector editables = new Vector();
    JInternalFrame w;
    Enumeration windows;

    /* -- */
    
    windows = windowList.keys();      

    while (windows.hasMoreElements())
      {
	w = (JInternalFrame)windowList.get(windows.nextElement());
	  
	// This seems backwards, but only non-editable windows are closable.
	// So if isClosable is false, then it is editable, and we should
	// close it.

	if (w.isClosable())
	  {
	    //This is a view window
	  }
	else
	  {
	    editables.addElement(w);
	  }
      }
  
    return editables;
  }

  /**
   * Closes all windows that are open for editing.  
   *
   * <p>This should be called by the parent when the transaction is canceled, to get rid of
   * windows that might confuse the user.</p>
   */

  public void closeEditables()
  {
    Enumeration windows;

    /* -- */
    
    windows = windowList.keys();      

    while (windows.hasMoreElements())
      {
	Object o  = windowList.get(windows.nextElement());
	if (o instanceof framePanel)
	  {

	    
	    framePanel w = (framePanel)o;
	    
	    if (w.editable)
	      {
		try
		  {
		    w.setClosed(true);
		  }
		catch (java.beans.PropertyVetoException ex)
		  {
		    throw new RuntimeException("beans? " + ex);
		  }
	      }
	  }
      }
  }
  
  /**
   * Closes all non-editable windows
   *
   */

  public void closeNonEditables()
  {
    JInternalFrame w;
    Enumeration windows;

    /* -- */

    windows = windowList.keys();

    while (windows.hasMoreElements())
      {
	w = (JInternalFrame)windowList.get(windows.nextElement());

	if (w.isClosable())
	  {
	    try
	      {
		w.setClosed(true);
	      }
	    catch (java.beans.PropertyVetoException ex)
	      {
		throw new RuntimeException("beans? " + ex);
	      }
	  }
      }
  }

  public void closeWindow(String title)
  {
    JInternalFrame w;
    Enumeration windows;
      
    /* -- */
    
    setStatus("Closing a window");
    
    windows = windowList.keys();
    
    while (windows.hasMoreElements())
      {
	w = (JInternalFrame)windowList.get(windows.nextElement());
	  
	if (w.getTitle().equals(title))
	  {
	    if (w.isClosable())
	      {
		try 
		  {
		    w.setClosed(true);
		  }
		catch (java.beans.PropertyVetoException ex)
		  {
		    throw new RuntimeException("beans? " + ex);
		  }
	      }
	    else
	      {
		setStatus("You can't close that window.");
	      }
	    break;
	  }
      }
      
    setStatus("Done");
  }

  public JMenu updateMenu()
  {
    Enumeration windows;
    Object obj;
    JMenuItem MI;

    /* -- */

    windowMenu.removeAll();
    windows = windowList.keys();      

    while (windows.hasMoreElements())
      {
	obj = windowList.get(windows.nextElement());
	MI = null;

	if (obj instanceof framePanel)
	  {
	    if (debug)
	      {
		System.out.println("Adding menu item(fp): " + ((framePanel)obj).getTitle());
	      }

	    MI = new JMenuItem(((framePanel)obj).getTitle());
	  }
	else if (obj instanceof gResultTable)
	  {
	    if (debug)
	      {
		System.out.println("Adding menu item: " + ((gResultTable)obj).getTitle());
	      }

	    MI = new JMenuItem(((gResultTable)obj).getTitle());
	  }

	if (MI != null)
	  {
	    MI.setActionCommand("showWindow");
	    MI.addActionListener(this);
	    windowMenu.add(MI);
	  }
      }

    return windowMenu;
  }

  public void showWindow(String title)
  {
    Object obj = windowList.get(title);

    if (obj instanceof framePanel)
      {
	setSelectedWindow((framePanel)obj);
	/*
	((framePanel)obj).moveToFront();
	try
	  {
	    ((framePanel)obj).setSelected(true);
	  }
	catch ( java.beans.PropertyVetoException e)
	  {
	    System.out.println("Couldn't select the window.");
	  }
	*/
      }
    else if (obj instanceof gResultTable)
      {
	setSelectedWindow((gResultTable)obj);
	/*
	((gResultTable)obj).moveToFront();
	try
	  {
	    ((gResultTable)obj).setSelected(true);
	  }
	catch ( java.beans.PropertyVetoException e)
	  {
	    System.out.println("Couldn't select the window.");
	    }*/
      }
    else 
      {
	System.out.println("Hmm, don't know what kind of window this is.");
      }
  }

  public void refreshTableWindows()
  {
    Object obj;
    Enumeration enum = windowList.keys();

    while (enum.hasMoreElements())
      {
	obj = windowList.get(enum.nextElement());
	if (obj instanceof gResultTable)
	  {
	    ((gResultTable)obj).refreshQuery();
	  }
      }
  }

  // Event handlers

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() instanceof JMenuItem)
      {
	if (debug)
	  {
	    System.out.println("Menu item action: " + e.getActionCommand());
	  }

	JMenuItem MI = (JMenuItem)e.getSource();

	if (e.getActionCommand().equals("showWindow"))
	  {
	    showWindow(MI.getText());
	  }
	else if (e.getActionCommand().equals("Edit"))
	  {
	    if (debug)
	      {
		System.out.println("edit button clicked");
	      }
	    try
	      {
		if (debug)
		  {
		    System.out.println("Opening new edit window");
		  }
		//addWindow(gc.session.edit_db_object(((db_object)menuItems.get(MI)).getInvid()), true);
		gc.editObject(((db_object)menuItems.get(MI)).getInvid());
	      }
	    catch (RemoteException rx)
	      {
		setStatus("Something went wrong on the server.");
		throw new RuntimeException("Could not open object for edit: " + rx);
	      }
	  }
	else if (e.getActionCommand().equals("Clone"))
	  {
	    try
	      {
		if (debug)
		  {
		    System.out.println("Opening new edit window on the cloned object");
		  }
		//addWindow(parent.session.clone_db_object(((db_object)Buttons.get(button)).getInvid()), true);
		System.out.println("clone_db_object not there yet");
	      }
	    //catch (RemoteException rx)
	    catch (Exception rx)
	      {
		setStatus("Something went wrong on the server.");
		throw new RuntimeException("Could not clone object: " + rx);
	      }
	  }
	else if (e.getActionCommand().equals("ReallyDelete"))
	  {
	    try
	      {
		if (debug)
		  {
		    System.out.println("Deleting object");
		  }
		gc.deleteObject(((db_object)menuItems.get(MI)).getInvid());
		try
		  {
		    ((JInternalFrame)Windows.get(MI)).setClosed(true);

		  }
		catch (PropertyVetoException ex)
		  {
		    throw new RuntimeException("JInternalFrame will not close: " + ex);
		  }
	      }
	    catch (RemoteException rx)
	      {
		setStatus("Something went wrong on the server.");
		throw new RuntimeException("Could not delete object: " + rx);
	      }
	  }
	else if (e.getActionCommand().equals("Close"))
	  {
	    try
	      {
		((JInternalFrame)Windows.get(MI)).setClosed(true);
		
	      }
	    catch (PropertyVetoException ex)
	      {
		throw new RuntimeException("JInternalFrame will not close: " + ex);
	      }
	  }
	else if (e.getActionCommand().equals("Iconify"))
	  {
	    try
	      {
		((JInternalFrame)Windows.get(MI)).setIcon(true);
	      }
	    catch (PropertyVetoException ex)
	      {
		throw new RuntimeException("JInternalFrame will not close: " + ex);
	      }
	  }
	else if (e.getActionCommand().equals("Print"))
	  {
	    ((framePanel)Windows.get(MI)).printObject();
	  }
	else if (e.getActionCommand().equals("Refresh"))
	  {
	    framePanel fp = (framePanel)Windows.get(MI);
	    if (fp != null)
	      {
		fp.refresh();
	      }
	    else
	      {
		System.out.println("-!- Could not find the window for this menuitem.");
	      }
	  }
      	else if (e.getActionCommand().equals("Inactivate"))
	  {
	    framePanel fp = (framePanel)Windows.get(MI);

	    boolean success = gc.inactivateObject(fp.invid);

	    if (success)
	      {
		try
		  {
		    ((JInternalFrame)Windows.get(MI)).setClosed(true);
		    
		  }
		catch (PropertyVetoException ex)
		  {
		    throw new RuntimeException("JInternalFrame will not close: " + ex);
		  }		
	      }
	    else
	      {
		gc.showErrorMessage("Could not inactivate object.");
	      }

	  }
	else if (e.getActionCommand().equals("Query"))
	  {
	    System.out.println("Not sure what a query should do");
	  }
	else if (e.getActionCommand().equals("Set Expiration Date"))
	  {
	    framePanel fp = (framePanel)Windows.get(MI);
	    fp.addExpirationDatePanel();
	    fp.showTab(fp.expiration_date_index);
	  }
	else if (e.getActionCommand().equals("Set Removal Date"))
	  {
	    framePanel fp = (framePanel)Windows.get(MI);
	    fp.addRemovalDatePanel();
	    fp.showTab(fp.removal_date_index);
	  }
      }
    else
      {
	System.err.println("Unknown ActionEvent in windowPanel");
      }
  }

  /*
  public void invalidate()
  {
    System.out.println("-- Invalidate windowPanel");
    super.invalidate();
  }

  public void validate()
  {
    System.out.println("-- validate windowPanel");
    super.validate();
  }
  */    


  // This is for the beans, when a JInternalFrame closes

  public void propertyChange(java.beans.PropertyChangeEvent event)
  {
    //System.out.println("propertyChange: " + event.getSource());
    //System.out.println("getPropertyName: " + event.getPropertyName());
    //System.out.println("getNewValue: " + event.getNewValue());

    if ((event.getPropertyName().equals("isClosed")) && ((Boolean)event.getNewValue()).booleanValue())
      {
	if (event.getSource() instanceof JInternalFrame)
	  {
	    if (debug)
	      {
		System.out.println("Closing an internal frame");
	      }

	    if (event.getSource() instanceof framePanel)
	      {
		((framePanel)event.getSource()).stopLoading();
	      }
	    //System.out.println("It's a JInternalFrame");
	    String oldTitle = ((JInternalFrame)event.getSource()).getTitle();
	      
	    if (oldTitle == null)
	      {
		System.out.println("Title is null");
	      }
	    else
	      {
		//System.out.println(" Removing button- " + oldTitle);
		  
		windowList.remove(oldTitle);

		updateMenu();
	      }
	  }
	else
	  {
	    System.out.println("propertyChange from something other than a JInternalFrame");
	  }
      }
  }
 
  void addRow(JComponent parent, Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);
    parent.add("0 " + row + " lthwHW", l);
    parent.add("1 " + row + " lthwHW", comp);
  }
}

