/*
   gclient.java

   Ganymede client main module

   Created: 24 Feb 1997

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Mike Mulvaney, Jonathan Abbey, and Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
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

package arlut.csd.ganymede.client;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicToolBarUI;

import org.python.core.PySystemState;

import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JErrorValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.LAFMenu;
import arlut.csd.JDataComponent.listHandle;
import arlut.csd.JDialog.DialogRsrc;
import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.JDialog.messageDialog;
import arlut.csd.JDialog.aboutGanyDialog;
import arlut.csd.JTree.treeCallback;
import arlut.csd.JTree.treeControl;
import arlut.csd.JTree.treeMenu;
import arlut.csd.JTree.treeNode;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.VecQuickSort;
import arlut.csd.ganymede.common.BaseDump;
import arlut.csd.ganymede.common.CatTreeNode;
import arlut.csd.ganymede.common.CategoryDump;
import arlut.csd.ganymede.common.CategoryTransport;
import arlut.csd.ganymede.common.DumpResult;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectHandle;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.RegexpException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.Category;
import arlut.csd.ganymede.rmi.CategoryNode;
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.db_object;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         gclient

------------------------------------------------------------------------------*/

/**
 * <p>Main ganymede client class.  When {@link
 * arlut.csd.ganymede.client.glogin glogin} is run and a user logs in
 * to the server, the client obtains a {@link
 * arlut.csd.ganymede.rmi.Session Session} reference that allows it to
 * talk to the server on behalf of a user, and a single instance of
 * this class is created to handle all client GUI and networking
 * operations for that user.</p>
 *
 * <p>gclient creates a {@link arlut.csd.ganymede.client.windowPanel
 * windowPanel} object to contain internal object ({@link
 * arlut.csd.ganymede.client.framePanel framePanel}) and query windows
 * on the right side of a Swing JSplitPane.  The left side contains a
 * custom {@link arlut.csd.JTree.treeControl treeControl} GUI
 * component displaying object categories, types, and instances for
 * the user to browse and edit.</p>
 *
 * @version $Id$
 * @author Mike Mulvaney, Jonathan Abbey, and Navin Manohar
 */

public class gclient extends JFrame implements treeCallback, ActionListener, JsetValueCallback {

  public static boolean debug = false;

  /**
   * we're only going to have one gclient at a time per running client (singleton pattern).
   */

  public static gclient client;

  // Image numbers

  static final int NUM_IMAGE = 17;
  
  static final int OPEN_BASE = 0;
  static final int CLOSED_BASE = 1;

  static final int OPEN_FIELD = 2;
  static final int OPEN_FIELD_DELETE = 3;
  static final int OPEN_FIELD_CREATE = 4;
  static final int OPEN_FIELD_CHANGED = 5;
  static final int OPEN_FIELD_REMOVESET = 6;
  static final int OPEN_FIELD_EXPIRESET = 7;
  static final int CLOSED_FIELD = 8;
  static final int CLOSED_FIELD_DELETE = 9;
  static final int CLOSED_FIELD_CREATE = 10;
  static final int CLOSED_FIELD_CHANGED = 11;
  static final int CLOSED_FIELD_REMOVESET = 12;
  static final int CLOSED_FIELD_EXPIRESET = 13;

  static final int OPEN_CAT = 14;
  static final int CLOSED_CAT = 15;

  static final int OBJECTNOWRITE = 16;

  /**
   * This is a convenience method used by the server to get a
   * stack trace from a throwable object in String form.
   */

  static public String stackTrace(Throwable thing)
  {
    StringWriter stringTarget = new StringWriter();
    PrintWriter writer = new PrintWriter(stringTarget);
    
    thing.printStackTrace(writer);
    writer.close();

    return stringTarget.toString();
  }

  // ---

  /**
   * Main remote interface for communications with the server.
   */
  
  Session session;

  /**
   * Reference to the applet which instantiated us.
   */

  glogin _myglogin;

  /**
   * Local copy of the category/object tree downloaded from
   * the server by the {@link arlut.csd.ganymede.client.gclient#buildTree() buildTree()}
   * method.
   */

  CategoryDump dump;

  /**
   * Name of the currently active persona.
   */

  String currentPersonaString;

  // set up a bunch of borders
  // Turns out we don't need to do this anyway, since the BorderFactory does it for us.

  public EmptyBorder
    emptyBorder5 = (EmptyBorder) BorderFactory.createEmptyBorder(5,5,5,5),
    emptyBorder10 = (EmptyBorder) BorderFactory.createEmptyBorder(10,10,10,10);

  public BevelBorder
    raisedBorder = (BevelBorder) BorderFactory.createBevelBorder(BevelBorder.RAISED),
    loweredBorder = (BevelBorder) BorderFactory.createBevelBorder(BevelBorder.LOWERED);
      
  public LineBorder
    lineBorder = (LineBorder) BorderFactory.createLineBorder(Color.black);

  public CompoundBorder
    statusBorder = BorderFactory.createCompoundBorder(loweredBorder, emptyBorder5),
    statusBorderRaised = BorderFactory.createCompoundBorder(raisedBorder, emptyBorder5);

  //
  // Yum, caches
  //

  /** 
   * Cache of {@link arlut.csd.ganymede.common.Invid invid}'s for objects
   * that might have been changed by the client.  The keys and the
   * values in this hash are the same.  The collection of tree nodes
   * corresponding to invid's listed in changedHash will be refreshed
   * by the client when a server is committed or cancelled.  
   */

  private Hashtable changedHash = new Hashtable();

  /** 
   * Mapping of {@link arlut.csd.ganymede.common.Invid invid}'s for objects
   * that the client has requested be deleted by the server to
   * {@link arlut.csd.ganymede.client.CacheInfo CacheInfo} objects
   * which hold information about the object used to make decisions
   * about managing the client's tree display.
   */

  private Hashtable deleteHash = new Hashtable();

  /**  
   * Mapping of {@link arlut.csd.ganymede.common.Invid invid}'s for objects
   * that the client has requested be created by the server to
   * {@link arlut.csd.ganymede.client.CacheInfo CacheInfo} objects
   * which hold information about the object used to make decisions
   * about managing the client's tree display.
   */

  private Hashtable createHash = new Hashtable();

  /**
   * Hash of {@link arlut.csd.ganymede.common.Invid invid}'s corresponding
   * to objects that have been created by the client but which have not
   * had nodes created in the client's tree display.  Once nodes are
   * created for these objects, the invid will be taken out of this
   * hash and put into createHash.
   */

  private Hashtable createdObjectsWithoutNodes = new Hashtable();

  /**
   * <p>Hash mapping Short {@link arlut.csd.ganymede.rmi.Base Base} id's to
   * the corresponding {@link arlut.csd.ganymede.client.BaseNode BaseNode}
   * displayed in the client's tree display.</p>
   */

  protected Hashtable shortToBaseNodeHash = new Hashtable();

  /**
   * <p>Hash mapping {@link arlut.csd.ganymede.common.Invid Invid}'s for objects
   * referenced by the client to the corresponding
   * {@link arlut.csd.ganymede.client.InvidNode InvidNode} displayed in the
   * client's tree display.</p>
   */

  protected Hashtable invidNodeHash = new Hashtable();

  /**
   * <p>Our main cache, keeps information about all objects we've learned
   * about via {@link arlut.csd.ganymede.common.QueryResult QueryResult}'s returned
   * to us by the server.</p>
   *
   * <p>We can get QueryResults from the server by doing direct
   * {@link arlut.csd.ganymede.rmi.Session#query(arlut.csd.ganymede.common.Query) query}
   * calls on the server, or by calling choices() on an 
   * {@link arlut.csd.ganymede.rmi.invid_field invid_field} or on a
   * {@link arlut.csd.ganymede.rmi.string_field string_field}.  Information from
   * both sources may be integrated into this cache.</p>
   */

  protected objectCache cachedLists = new objectCache();

  /**
   * Background processing thread, downloads information on
   * object and field types defined in the server when run.
   */
 
  Loader loader;
  
  //
  // Status tracking
  //

  private boolean
    toolToggle = true,
    showToolbar = true,       // Show the toolbar
    somethingChanged = false;  // This will be set to true if the user changes anything

  private int
    buildPhase = -1;		// unknown
  
  helpPanel
    help = null;

  messageDialog
    motd = null;

  aboutGanyDialog
    about = null;

  Vector
    personae,
    ownerGroups = null;  // Vector of owner groups

  // Dialog and GUI objects


  JToolBar 
    toolBar;
    
  JFilterDialog
    filterDialog = null;

  PersonaDialog
    personaDialog = null;

  JDefaultOwnerDialog
    defaultOwnerDialog = null;

  openObjectDialog
    openDialog;

  createObjectDialog
    createDialog = null;

  Image images[];

  JButton 
    commit,
    cancel;

  JPanel
    statusPanel = new JPanel(new BorderLayout());

  /**
   * Status field at the bottom of the client.
   */
  
  final JTextField
    statusLabel = new JTextField();

  /**
   * Build status field at the bottom of the client.
   */

  JLabel
    buildLabel = new JLabel();

  /**
   * The client's GUI tree component.
   */

  treeControl tree;

  /**
   * The currently selected node from the client's GUI tree.
   */

  treeNode
    selectedNode;

  // The top lines

  Image
    errorImage = null,
    questionImage = null,
    search,
    queryIcon,
    cloneIcon,
    pencil,
    personaIcon,
    inactivateIcon,
    treepencil,
    trash,
    treetrash,
    creation,
    treecreation,
    newToolbarIcon,
    ganymede_logo,
    createDialogImage;

  ImageIcon
    idleIcon, buildIcon, buildIcon2, buildUnknownIcon;

  /**
   * JDesktopPane on the right side of the client's display, contains
   * the object and query result internal windows that are created
   * during the client's execution.
   */

  windowPanel
    wp;

  //
  // Menu resources
  //

  treeMenu 
    objectViewPM,
    objectReactivatePM,
    objectInactivatePM,
    objectRemovePM;

  treeMenu 
    pMenuAll = new treeMenu(),
    pMenuEditable= new treeMenu(),
    pMenuEditableCreatable = new treeMenu(),
    pMenuAllCreatable = new treeMenu();
  
  JMenuBar 
    menubar;

  JMenuItem 
    logoutMI,
    clearTreeMI,
    filterQueryMI,
    defaultOwnerMI,
    showHelpMI,
    toggleToolBarMI;

  JCheckBoxMenuItem
    hideNonEditablesMI;

  /**
   * If true, the client will only display object types that the
   * user has permission to edit, and by default will only show objects
   * in the tree that the user can edit.  If false, all objects and
   * object types the the user has permission to view will be shown
   * in the tree.  Toggled by the user manipulating the hideNonEditablesMI
   * check box menu item.
   */

  boolean    hideNonEditables = true;

  boolean defaultOwnerChosen = false;

  JMenuItem
    changePersonaMI,
    editObjectMI,
    viewObjectMI,
    createObjectMI,
    deleteObjectMI,
    inactivateObjectMI,
    menubarQueryMI = null;

  String
    my_username;

  JMenu 
    actionMenu,
    windowMenu,
    fileMenu,
    helpMenu,
    PersonaMenu = null;
  
  LAFMenu
    LandFMenu;

  /**
   * Listener to react to persona dialog events
   */

  PersonaListener
    personaListener = null;

  /**
   * Query dialog that is displayed when the user chooses to perform
   * a query on the server.
   */

  querybox
    my_querybox = null;

  /**
   * <p>This thread is used to clear the statusLabel after some interval after
   * it is set.</p>
   *
   * <p>Whenever the gclient's
   * {@link arlut.csd.ganymede.client.gclient#setStatus(java.lang.String,int) setStatus}
   * method is called, this thread has a countdown timer started, which will
   * clear the status label if it is not reset by another call to setStatus.</p>
   */

  public StatusClearThread statusThread;

  /**
   * <p>This thread is set up to launder RMI build status updates from the server.</p>
   *
   * <p>In some versions of Sun's JDK, RMI callbacks are not allowed to manipulate
   * the GUI event queue.  To get around this, this securityThread is created
   * to launder these RMI callbacks so that the Swing event queue is messed with
   * by a client-local thread.</p>
   */

  public SecurityLaunderThread securityThread;

  /**
   * this is true during the handleReturnVal method, while a wizard is
   * active.  If a wizard is active, don't allow the window to close.
   */

  private int wizardActive = 0;

  /* -- */

  /**
   * <p>This is the main constructor for the gclient class.. it handles the
   * interactions between the user and the server once the user has
   * logged in.</p>
   *
   * @param s Connection to the server created for us by the glogin applet.
   * @param g The glogin applet which is creating us.
   */

  public gclient(Session s, glogin g)
  {
    JPanel
      leftP,
      leftTop,
      rightTop,
      mainPanel;   //Everything is in this, so it is double buffered

    /* -- */


    try
      {
	setTitle("Ganymede Client: " + s.getMyUserName() + " logged in");
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx);
      }

    client = this;

    if (!debug)
      {
	debug = g.debug;
      }

    if (debug)
      {
	System.out.println("Starting client");
      }

    enableEvents(AWTEvent.WINDOW_EVENT_MASK);

    if (s == null)
      {
	throw new IllegalArgumentException("Ganymede Error: Parameter for Session s is null");
      }

    session = s;
    _myglogin = g;
    my_username = g.getUserName().toLowerCase();
    currentPersonaString = my_username;

    mainPanel = new JPanel(true);
    mainPanel.setLayout(new BorderLayout());

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add("Center", mainPanel);

    if (debug)
      {
	System.out.println("Creating menu bar");
      }

    // Make the menu bar

    menubar = new JMenuBar();

    //menubar.setBorderPainted(true);
    
    // File menu

    fileMenu = new JMenu("File");
    fileMenu.setMnemonic('f');
    fileMenu.setDelay(0);

    toggleToolBarMI = new JMenuItem("Toggle Toolbar");
    toggleToolBarMI.setMnemonic('t');
    toggleToolBarMI.addActionListener(this);

    logoutMI = new JMenuItem("Logout");
    logoutMI.setMnemonic('l');
    logoutMI.addActionListener(this);

    clearTreeMI = new JMenuItem("Clear Tree");
    clearTreeMI.setMnemonic('c');
    clearTreeMI.addActionListener(this);

    filterQueryMI = new JMenuItem("Set Owner Filter");
    filterQueryMI.setMnemonic('f');
    filterQueryMI.addActionListener(this);

    defaultOwnerMI = new JMenuItem("Set Default Owner");
    defaultOwnerMI.setMnemonic('d');
    defaultOwnerMI.addActionListener(this);

    hideNonEditablesMI = new JCheckBoxMenuItem("Hide non-editable objects", true);
    hideNonEditablesMI.addActionListener(this);

    fileMenu.add(clearTreeMI);
    fileMenu.add(filterQueryMI);
    fileMenu.add(defaultOwnerMI);
    fileMenu.add(hideNonEditablesMI);
    fileMenu.addSeparator();
    fileMenu.add(logoutMI);

    // Action menu

    actionMenu = new JMenu("Actions");
    actionMenu.setMnemonic('a');

    createObjectMI = new JMenuItem("Create Object");
    createObjectMI.setMnemonic('c');
    createObjectMI.setActionCommand("create new object");
    createObjectMI.addActionListener(this);
    
    editObjectMI = new JMenuItem("Edit Object");
    editObjectMI.setMnemonic('e');
    editObjectMI.setActionCommand("open object for editing");
    editObjectMI.addActionListener(this);

    viewObjectMI = new JMenuItem("View Object");
    viewObjectMI.setMnemonic('v');
    viewObjectMI.setActionCommand("open object for viewing");
    viewObjectMI.addActionListener(this);
    
    deleteObjectMI = new JMenuItem("Delete Object");
    deleteObjectMI.setMnemonic('d');
    deleteObjectMI.setActionCommand("delete an object");
    deleteObjectMI.addActionListener(this);

    inactivateObjectMI = new JMenuItem("Inactivate Object");
    inactivateObjectMI.setMnemonic('i');
    inactivateObjectMI.setActionCommand("inactivate an object");
    inactivateObjectMI.addActionListener(this);

    menubarQueryMI = new JMenuItem("Query");
    menubarQueryMI.setMnemonic('q');
    menubarQueryMI.addActionListener(this);

   // Personae init

    try
      {
	personae = session.getPersonae();
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx, "Could not load personas");
      }

    personaListener = new PersonaListener(session, this);

    if ((personae != null) && personae.size() > 1)
      {
	changePersonaMI = new JMenuItem("Change Persona");
	changePersonaMI.setMnemonic('p');
	changePersonaMI.setActionCommand("change persona");
	changePersonaMI.addActionListener(this);
	actionMenu.add(changePersonaMI);
      }

    actionMenu.add(menubarQueryMI);
    actionMenu.addSeparator();
    actionMenu.add(viewObjectMI);
    actionMenu.add(createObjectMI);
    actionMenu.add(editObjectMI);
    actionMenu.add(deleteObjectMI);
    actionMenu.add(inactivateObjectMI);

    if (debug)
      {
	JMenuItem viewAnInvid = new JMenuItem("Show me an Invid");
	viewAnInvid.addActionListener(this);
	actionMenu.addSeparator();
	actionMenu.add(viewAnInvid);
      }

    // windowMenu

    windowMenu = new JMenu("Windows");
    windowMenu.setMnemonic('w');
    windowMenu.add(toggleToolBarMI);
   
    // Look and Feel menu

    LandFMenu = new arlut.csd.JDataComponent.LAFMenu(this);
    LandFMenu.setMnemonic('l');
    LandFMenu.setCallback(this);

    // Help menu

    helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('h');

    // we don't have anything done for help.. disable the help menu for now.

    //    showHelpMI = new JMenuItem("Help");
    //    showHelpMI.setMnemonic('h');  // swing can't handle menu and menuitem with same mnemonic
    //    showHelpMI.addActionListener(this);
    //    helpMenu.add(showHelpMI);
    //
    //    helpMenu.addSeparator();

    // This uses action commands, so you don't need to globally declare these

    JMenuItem showAboutMI = new JMenuItem("About Ganymede");
    showAboutMI.setMnemonic('a');
    showAboutMI.addActionListener(this);
    helpMenu.add(showAboutMI);

    JMenuItem showCreditsMI = new JMenuItem("Credits");
    showCreditsMI.setMnemonic('c');
    showCreditsMI.addActionListener(this);
    helpMenu.add(showCreditsMI);

    JMenuItem showMOTDMI = new JMenuItem("Message of the day");
    showMOTDMI.setMnemonic('m');
    showMOTDMI.addActionListener(this);
    helpMenu.add(showMOTDMI);

    menubar.add(fileMenu);
    menubar.add(LandFMenu);
    menubar.add(actionMenu);
    menubar.add(windowMenu);

    menubar.add(Box.createGlue());
    menubar.add(helpMenu);    
    setJMenuBar(menubar);

    // Create menus for the tree

    pMenuAll.add(new JMenuItem("Hide Non-Editables"));
    pMenuAll.add(new JMenuItem("Query"));
    pMenuAll.add(new JMenuItem("Report editable"));
    pMenuAll.add(new JMenuItem("Report all"));

    pMenuEditable.add(new JMenuItem("Show Non-Editables"));
    pMenuEditable.add(new JMenuItem("Query"));
    pMenuEditable.add(new JMenuItem("Report editable"));
    pMenuEditable.add(new JMenuItem("Report all"));

    pMenuAllCreatable.add(new JMenuItem("Hide Non-Editables"));
    pMenuAllCreatable.add(new JMenuItem("Query"));
    pMenuAllCreatable.add(new JMenuItem("Report editable"));
    pMenuAllCreatable.add(new JMenuItem("Report all"));
    pMenuAllCreatable.add(new JMenuItem("Create"));

    pMenuEditableCreatable.add(new JMenuItem("Show Non-Editables"));
    pMenuEditableCreatable.add(new JMenuItem("Query"));
    pMenuEditableCreatable.add(new JMenuItem("Report editable"));
    pMenuEditableCreatable.add(new JMenuItem("Report all"));
    pMenuEditableCreatable.add(new JMenuItem("Create"));

    if (debug)
      {
	System.out.println("Loading images for tree");
      }

    ganymede_logo = _myglogin.ganymede_logo;

    Image openFolder = PackageResources.getImageResource(this, "openfolder.gif", getClass());
    Image closedFolder = PackageResources.getImageResource(this, "folder.gif", getClass());
    Image list = PackageResources.getImageResource(this, "list.gif", getClass());
    Image listnowrite = PackageResources.getImageResource(this, "listnowrite.gif", getClass());
    Image redOpenFolder = PackageResources.getImageResource(this, "openfolder-red.gif", getClass());
    Image redClosedFolder = PackageResources.getImageResource(this, "folder-red.gif", getClass());
    
    search = PackageResources.getImageResource(this, "srchfol2.gif", getClass());
    queryIcon = PackageResources.getImageResource(this, "query.gif", getClass());
    cloneIcon = PackageResources.getImageResource(this, "clone.gif", getClass());
    idleIcon = new ImageIcon(PackageResources.getImageResource(this, "nobuild.gif", getClass()));
    buildUnknownIcon = new ImageIcon(PackageResources.getImageResource(this, "buildunknown.gif", getClass()));
    buildIcon = new ImageIcon(PackageResources.getImageResource(this, "build1.gif", getClass()));
    buildIcon2 = new ImageIcon(PackageResources.getImageResource(this, "build2.gif", getClass()));
    trash = PackageResources.getImageResource(this, "trash.gif", getClass());
    creation = PackageResources.getImageResource(this, "creation.gif", getClass());
    newToolbarIcon = PackageResources.getImageResource(this, "newicon.gif", getClass());
    pencil = PackageResources.getImageResource(this, "pencil.gif", getClass());
    //    inactivateIcon = PackageResources.getImageResource(this, "inactivate.gif", getClass());
    personaIcon = PackageResources.getImageResource(this, "persona.gif", getClass());
    setIconImage(pencil);
    createDialogImage = PackageResources.getImageResource(this, "wiz3b.gif", getClass());

    treepencil = PackageResources.getImageResource(this, "treepencil.gif", getClass());
    treetrash = PackageResources.getImageResource(this, "treetrash.gif", getClass());
    treecreation = PackageResources.getImageResource(this, "treenewicon.gif", getClass());

    Image remove = PackageResources.getImageResource(this, "remove.gif", getClass());
    Image expire = PackageResources.getImageResource(this, "expire.gif", getClass());

    images = new Image[NUM_IMAGE];
    images[OPEN_BASE] =  openFolder;
    images[CLOSED_BASE ] = closedFolder;
    
    images[OPEN_FIELD] = list;
    images[OPEN_FIELD_DELETE] = treetrash;
    images[OPEN_FIELD_CREATE] = treecreation;
    images[OPEN_FIELD_CHANGED] = treepencil;
    images[OPEN_FIELD_EXPIRESET] = expire;
    images[OPEN_FIELD_REMOVESET] = remove;
    images[CLOSED_FIELD] = list;
    images[CLOSED_FIELD_DELETE] = treetrash;
    images[CLOSED_FIELD_CREATE] = treecreation;
    images[CLOSED_FIELD_CHANGED] = treepencil;
    images[CLOSED_FIELD_EXPIRESET] = expire;
    images[CLOSED_FIELD_REMOVESET] = remove;
    
    images[OPEN_CAT] = redOpenFolder;
    images[CLOSED_CAT] = redClosedFolder;

    images[OBJECTNOWRITE] = listnowrite;

    tree = new treeControl(new Font("SansSerif", Font.BOLD, 12),
			   Color.black, Color.white, this, images,
			   null);

    tree.setMinimumWidth(200);

    if (debug)
      {
	System.out.println("Adding left and right panels");
      }

    //    Box leftBox = new Box(tree, "Objects");

    leftP = new JPanel(false);
    leftP.setLayout(new BorderLayout());
    leftP.add("Center", tree);
    
    if (!showToolbar)
      {
	leftTop = new JPanel(false);
	leftTop.setBorder(statusBorderRaised);
	
	leftTop.setLayout(new BorderLayout());

	leftTop.add("Center", new JLabel("Objects"));
	
	leftP.add("North", leftTop);
      }

    if (debug)
      {
	System.out.println("Creating pop up menus");
      }

    objectViewPM = new treeMenu();
    objectViewPM.add(new JMenuItem("View Object"));

    objectRemovePM = new treeMenu();
    objectRemovePM.add(new JMenuItem("View Object"));
    objectRemovePM.add(new JMenuItem("Edit Object"));
    objectRemovePM.add(new JMenuItem("Clone Object"));
    objectRemovePM.add(new JMenuItem("Delete Object"));

    objectInactivatePM = new treeMenu();
    objectInactivatePM.add(new JMenuItem("View Object"));
    objectInactivatePM.add(new JMenuItem("Edit Object"));
    objectInactivatePM.add(new JMenuItem("Clone Object"));
    objectInactivatePM.add(new JMenuItem("Delete Object"));
    objectInactivatePM.add(new JMenuItem("Inactivate Object"));

    objectReactivatePM = new treeMenu();
    objectReactivatePM.add(new JMenuItem("View Object"));
    objectReactivatePM.add(new JMenuItem("Edit Object"));
    objectReactivatePM.add(new JMenuItem("Clone Object"));
    objectReactivatePM.add(new JMenuItem("Delete Object"));
    objectReactivatePM.add(new JMenuItem("Reactivate Object"));

    try
      {
	buildTree();
      }
    catch (Exception ex)
      {
	processExceptionRethrow(ex, "caught remote exception in buildTree");
      }

    // The right panel which will contain the windowPanel

    JPanel rightP = new JPanel(true);
    //    rightP.setBackground(ClientColor.background);
    rightP.setLayout(new BorderLayout());

    wp = new windowPanel(this, windowMenu);

    rightP.add("Center", wp);
    rightTop = new JPanel(false);
    rightTop.setBorder(statusBorderRaised);
    rightTop.setLayout(new BorderLayout());
    
    toolBar = createToolbar();

    if (showToolbar)
      {
	getContentPane().add("North", toolBar);
      }
    
    commit = new JButton("Commit");
    commit.setEnabled(false);
    commit.setOpaque(true);
    commit.setToolTipText("Click this to commit all changes to database");
    commit.addActionListener(this);

    cancel = new JButton("Cancel");
    cancel.setEnabled(false);
    cancel.setOpaque(true);
    cancel.setToolTipText("Click this to cancel all changes");
    cancel.addActionListener(this);

    // Button bar at bottom, includes commit/cancel panel and taskbar

    JPanel bottomButtonP = new JPanel(false);

    bottomButtonP.add(commit);
    bottomButtonP.add(cancel);
    bottomButtonP.setBorder(loweredBorder);

    // Create the pane splitter

    JSplitPane sPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftP, rightP);
    sPane.setOneTouchExpandable(true);
   
    mainPanel.add("Center",sPane);

    // Create the bottomBar, for the bottom of the window

    JPanel bottomBar = new JPanel(false);
    bottomBar.setLayout(new BorderLayout());

    statusLabel.setEditable(false);
    statusLabel.setOpaque(false);
    statusLabel.setBorder(statusBorder);

    statusThread = new StatusClearThread(statusLabel);
    statusThread.start();

    // start the securityThread to launder RMI calls from the server
    
    securityThread = new SecurityLaunderThread(this);
    securityThread.start();

    JPanel lP = new JPanel(new BorderLayout());
    lP.setBorder(statusBorder);
    lP.add("Center", buildLabel);

    bottomBar.add("West", lP);
    bottomBar.add("Center", statusLabel);
    bottomBar.add("East", bottomButtonP);

    mainPanel.add("South", bottomBar);

    setStatus("Starting up");

    // Since we're logged in and have a session established, create the
    // background loader thread to read in object and field type information

    loader = new Loader(session, debug);
    loader.start();

    pack();
    setSize(800, 600);
    this.setVisible(true);

    // Adjust size of toolbar buttons to that of largest button
    // Must be done after components are displayed. Otherwise, 
    // getWidth & getHeight return 0's.

    // Not sure about status of "uniform buttons" so
    // using toggle to save having to comment out and in.

    boolean sameSize = true;

    if (sameSize) 
      { 
	int width=0;
	int height=0;

	// Get width/height for biggest button
      
	for (int i = 0; i<toolBar.getComponentCount(); i++) 
	  {
	    JButton b = (JButton)toolBar.getComponent(i);
	
	    int temp = b.getWidth();

	    if (temp > width) 
	      {
		width = temp;
	      }
	
	    int temp2 = b.getHeight();
	  
	    if (temp2 > height) 
	      {
		height = temp2;
	      }
	  }
      
	Dimension buttonSize = new Dimension(width,height);    

	// Set width/height of all buttons to that of biggest
        
	for (int j = 0; j<toolBar.getComponentCount(); j++) 
	  {
	    JButton b = (JButton)toolBar.getComponent(j);

	    b.setMaximumSize(buttonSize);
	    b.setMinimumSize(buttonSize);
	    b.setPreferredSize(buttonSize);
	  }
      }

    getContentPane().validate();
  }

  /**
   *
   * This method handles the start-up tasks after the gclient
   * has gotten initialized.  Called by glogin.
   * 
   */

  public void start()
  {
    // open an initial transaction, in case the user doesn't change
    // personae

    try
      {
	ReturnVal rv = session.openTransaction("Ganymede GUI Client");
	rv = handleReturnVal(rv);

	if ((rv != null) && (!rv.didSucceed()))
	  {
	    throw new RuntimeException("Could not open transaction.");
	  }
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx, "Could not open transaction");
      }

    // If user has multiple personas, ask which to start with.

    if ((personae != null)  && personae.size() > 1)
      {
	// changePersona will block until the user does something
	// with the persona selection dialog

	changePersona(false);
	personaDialog.updatePassField(currentPersonaString);
      }

    // Check for MOTD on another thread

    Thread motdThread = new Thread(new Runnable() {
      public void run() {
	try
	  {
	    setStatus("Checking MOTD");

	    StringBuffer m;
	    boolean html = true;

	    m = session.getMessageHTML("motd", true);

	    // if there wasn't an html motd message, maybe there's a
	    // txt message?

	    if (m == null)
	      {
		m = session.getMessage("motd", true);
		html = false;
	      }

	    // if we didn't get any message, fold it up, we're done

	    if (m == null)
	      {
		return;
	      }

	    // and pop up the motd box back on the main GUI thread

	    // create final locals to bridge the gap into another
	    // method in the runnable to go on the GUI thread

	    final String textString = m.toString();
	    final boolean doHTML = html;

	    SwingUtilities.invokeLater(new Runnable() {
	      public void run() {
		showMOTD(textString, doHTML);
	      }
	    });
	  }
	catch (Exception rx)
	  {
	    processExceptionRethrow(rx, "Could not get motd");
	  }
      }
    });

    motdThread.start();
    
    setStatus("Ready.", 0);
  }
  
  /**
   * <p>Returns a vector of 
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}'s.</p>
   *
   * @param id Object type id to retrieve field information for.
   */

  public Vector getTemplateVector(short id)
  {
    return loader.getTemplateVector(new Short(id));
  }

  /**
   * <p>Returns a {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}
   * based on the short type id for the containing object and the
   * short field id for the field.</p>
   */

  public FieldTemplate getFieldTemplate(short objType, short fieldId)
  {
    Vector vect = loader.getTemplateVector(new Short(objType));

    for (int i = 0; i < vect.size(); i++)
      {
	FieldTemplate template = (FieldTemplate) vect.elementAt(i);

	if (template.getID() == fieldId)
	  {
	    return template;
	  }
      }

    return null;
  }

  /**
   * <p>Returns a vector of 
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}'s
   * listing fields and field informaton for the object type identified by 
   * id.</p>
   *
   * @param id The id number of the object type to be returned the base id.
   */

  public Vector getTemplateVector(Short id)
  {
    return loader.getTemplateVector(id);
  }

  /**
   * <p>Clears out the client's 
   * {@link arlut.csd.ganymede.client.objectCache objectCache},
   * which holds object labels, and activation status for invid's returned 
   * by various query and {@link arlut.csd.ganymede.rmi.db_field db_field} 
   * choices() operations.</p>
   */

  public void clearCaches()
  {
    if (debug)
      {
        System.out.println("Clearing caches");
      }

    cachedLists.clearCaches();
  }

  /**
   * <p>Gets a list of objects from the server, in
   * a form appropriate for use in constructing a list of nodes in the
   * tree under an object type (object base) folder.</p>
   *
   * <p>This method supports client-side caching.. if the list required
   * has already been retrieved, the cached list will be returned.  If
   * it hasn't, getObjectList() will get the list from the server and
   * save a local copy in an 
   * {@link arlut.csd.ganymede.client.objectCache objectCache}
   * for future requests.</p>
   */

  public objectList getObjectList(Short id, boolean showAll)
  {
    objectList objectlist = null;

    /* -- */

    if (cachedLists.containsList(id))
      {
	if (debug)
	  {
	    System.err.println("gclient.getObjectList(" + id + ", " + showAll +
			       ") getting objectlist from the cachedLists.");
	  }

	objectlist = cachedLists.getList(id);

	// If we are being asked for a *complete* list of objects of
	// the given type and we only have editable objects of this
	// type cached, we may need to go back to the server to
	// get the full list.

	if (showAll && !objectlist.containsNonEditable())
	  {
	    if (debug)
	      {
		System.err.println("gclient.getObjectList(" + id + ", " + showAll +
				   ") objectList incomplete, downloading non-editables.");
	      }

	    try
	      {
		Query objQuery = new Query(id.shortValue(), null, false);
		objQuery.setFiltered(true);

		QueryResult qr = session.query(objQuery);
		
		if (qr != null)
		  {
		    if (debug)
		      {
			System.out.println("gclient.getObjectList(): augmenting");
		      }
		    
		    objectlist.augmentListWithNonEditables(qr);
		  }
	      }
	    catch (Exception rx)
	      {
		processExceptionRethrow(rx, "Could not do the query");
	      }
	    
	    cachedLists.putList(id, objectlist);
	  }
      }
    else
      {
	if (debug)
	  {
	    System.out.println("gclient.getObjectList(" + id + ", " + showAll +
			       ") downloading objectlist from the server.");
	  }

	try
	  {
	    Query objQuery = new Query(id.shortValue(), null, !showAll);
	    objQuery.setFiltered(true);

	    QueryResult qr = session.query(objQuery);

	    if (debug)
	      {
		System.out.println("gclient.getObjectList(): caching copy");
	      }
	    
	    objectlist = new objectList(qr);
	    cachedLists.putList(id, objectlist);
	  }
	catch (Exception rx)
	  {
	    processExceptionRethrow(rx, "Could not get dump");
	  }
      }

    return objectlist;
  }

  /**
   * <p>Public accessor for the SecurityLaunderThread</p>
   */

  public int getBuildPhase()
  {
    return buildPhase;
  }

  /**
   * By overriding update(), we can eliminate the annoying flash as
   * the default update() method clears the frame before rendering.
   */

  public void update(Graphics g)
  {
    paint(g);
  }

  /** 
   * Get the session
   */

  public final Session getSession()
  {
    return session;
  }

  /**
   * <p>Loads and returns the error Image for use in client dialogs.</p>
   * 
   * <p>Once the image is loaded, it is cached for future calls to 
   * getErrorImage().</p>
   */

  public final Image getErrorImage()
  {
    if (errorImage == null)
      {
	errorImage = PackageResources.getImageResource(this, "error.gif", getClass());
      }
    
    return errorImage;
  }

  /**
   * <p>Loads and returns the question-mark Image for use in client dialogs.</p>
   * 
   * <p>Once the image is loaded, it is cached for future calls to 
   * getQuestionmage().</p>
   */

  public final Image getQuestionImage()
  {
    if (questionImage == null)
      {
	questionImage = PackageResources.getImageResource(this, "question.gif", getClass());
      }
    
    return questionImage;
  }

  /**
   * <p>Returns a hash mapping {@link arlut.csd.ganymede.common.BaseDump BaseDump}
   * references to their title.</p>
   *
   * <p>Checks to see if the baseNames was loaded, and if not, it loads it.
   * Always use this instead of trying to access baseNames directly.</p>
   */

  public final Hashtable getBaseNames()
  {
    return loader.getBaseNames();
  }

  /**
   * <p>Returns a Vector of {@link arlut.csd.ganymede.common.BaseDump BaseDump} objects,
   * providing a local cache of {@link arlut.csd.ganymede.rmi.Base Base}
   * references that the client consults during operations.</p>
   *
   * <p>Checks to see if the baseList was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseList
   * directly.</p>
   */

  public final synchronized Vector getBaseList()
  {
    return loader.getBaseList();
  }

  /**
   * <p>Returns a hash mapping Short {@link arlut.csd.ganymede.rmi.Base Base} id's to
   * {@link arlut.csd.ganymede.common.BaseDump BaseDump} objects.</p>
   *
   * <p>Checks to see if the baseMap was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseMap
   * directly.</p>
   */

  public Hashtable getBaseMap()
  {
    return loader.getBaseMap();
  }

  /**
   * <p>Returns a hashtable mapping {@link arlut.csd.ganymede.common.BaseDump BaseDump}
   * references to their object type id in Short form.  This is
   * a holdover from a time when the client didn't create local copies
   * of the server's Base references.</p>
   *
   * <p>Checks to see if the basetoShort was loaded, and if not, it loads it.
   * Always use this instead of trying to access the baseToShort
   * directly.</p>
   */

  public Hashtable getBaseToShort()
  {
    return loader.getBaseToShort();
  }

  /**
   * <p>Returns the type name for a given object.</p>
   *
   * <p>If the loader thread hasn't yet downloaded that information, this
   * method will block until the information is available.</p>
   */

  public String getObjectType(Invid objId)
  {
    return loader.getObjectType(objId);
  }

  /**
   * <p>This method returns a concatenated string made up of the object type
   * and object name.</p>
   */

  public String getObjectTitle(Invid objId)
  {
    ObjectHandle h = getObjectHandle(objId, null);
    return getObjectType(objId) + " " + h.getLabel();
  }

  /**
   * <p>Pulls a object handle for an invid out of the
   * client's cache, if it has been cached.</p>
   *
   * <p>If no handle for this invid has been cached, this method
   * will attempt to retrieve one from the server.</p>
   */

  public ObjectHandle getObjectHandle(Invid invid)
  {
    return this.getObjectHandle(invid, null);
  }

  /**
   * <p>Pulls a object handle for an invid out of the
   * client's cache, if it has been cached.</p>
   *
   * <p>If no handle for this invid has been cached, this method will
   * attempt to retrieve one from the server.</p>
   *
   * <p>The Short type parameter is just a micro-optimizing
   * convenience for code that already has such a Short
   * constructed.  This method will work perfectly well if
   * the type parameter is null.</p>
   */

  public ObjectHandle getObjectHandle(Invid invid, Short type)
  {
    ObjectHandle handle = null;

    if (type == null)
      {
	type = new Short(invid.getType());
      }

    if (cachedLists.containsList(type))
      {
	handle = cachedLists.getInvidHandle(type, invid);

	if (handle != null)
	  {
	    return handle;
	  }
      }

    // okay, we haven't found it.  try to pull this invid down from the
    // server, and cache it

    Vector paramVec = new Vector();

    paramVec.addElement(invid);

    try
      {
	QueryResult result = session.queryInvids(paramVec);

	Vector handleList = result.getHandles();

	if (handleList.size() > 0)
	  {
	    handle = (ObjectHandle) handleList.elementAt(0);
	  }
      }
    catch (Exception ex)
      {
	processExceptionRethrow(ex);
      }

    return handle;
  }

  /**
   * <p>Sets text in the status bar, with a 5 second countdown before
   * the status bar is cleared.</p>
   *
   * @param status The text to display
   */

  public final void setStatus(String status)
  {
    setStatus(status, 5);
  }

  /**
   * <p>Sets text in the status bar, with a defined countdown before
   * the status bar is cleared.</p>
   *
   * @param status The text to display
   * @param timeToLive Number of seconds to wait until clearing the status bar.
   * If zero or negative, the status bar timer will not clear the field until
   * the status bar is changed by another call to setStatus.
   */

  public final void setStatus(String status, int timeToLive)
  {
    if (debug)
      {
	System.out.println("Setting status: " + status);
      }

    final String fStatus = status;

    // use SwingUtilities.invokeLater so that we play nice
    // with the Java display thread
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	statusLabel.setText(fStatus);
	statusLabel.paintImmediately(statusLabel.getVisibleRect());
      }
    });

    statusThread.setClock(timeToLive);
  }

  /**
   * <p>This method is triggered by the Ganymede server if the client
   * is idle long enough.  This method will downgrade the user's
   * login to a minimum privilege level if possible, requiring
   * the user to enter their admin password again to regain
   * admin privileges.</p>
   */

  public final void softTimeout()
  {
    // we use invokeLater so that we free up the RMI thread
    // which messaged us, and so we play nice with the Java
    // display thread
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	personaListener.softTimeOutHandler();
      }
    });
  }

  /**
   * <p>Sets text in the build status bar</p>
   *
   * @param status The text to display
   */

  public final void setBuildStatus(String status)
  {
    if (debug)
      {
	System.out.println("Setting build status: " + status);
      }

    if (status.equals("idle"))
      {
	buildPhase = 0;
      }
    else if (status.equals("building"))
      {
	buildPhase = 1;
      }
    else if (status.equals("building2"))
      {
	buildPhase = 2;
      }
    else
      {
	buildPhase = -1;
      }

    try
      {
	securityThread.setBuildStatus(buildPhase);
      }
    catch (NullPointerException ex)
      {
      }
  }

  /**
   * <p>Returns the node of the object currently selected in the tree, if
   * any.  Returns null if there are no nodes selected in the tree, of
   * if the node selected in the tree is not an object node.</p>
   */

  public InvidNode getSelectedObjectNode()
  {
    // get our own copy of the current node so
    // that we don't get tripped up by threading

    treeNode myNode = selectedNode;

    if ((myNode == null) ||
	!(myNode instanceof InvidNode))
      {
	return null;
      }
    else
      {
	return (InvidNode) myNode;
      }
  }

  /**
   * Get the current text from the client's status field
   */

  public String getStatus()
  {
    return statusLabel.getText();
  }
  
  /**
   * <p>Show the help window.</p>
   *
   * <p>This might someday take an argument, which would show a starting page
   * or some more specific help.</p>
   */
   
  public void showHelpWindow()
  {
    if (help == null)
      {
	help = new helpPanel(this);
      }
    else
      {
	help.setVisible(true);
      }
  }

  /**
   * Shows the About... dialog.
   */

  public void showAboutMessage()
  {
    if (about == null)
      {
	about = new aboutGanyDialog(this, "About Ganymede");
      }

    about.loadAboutText();
    about.setVisible(true);
  }

  /**
   * Shows the credits dialog.
   */

  public void showCredits()
  {
    if (about == null)
      {
	about = new aboutGanyDialog(this, "About Ganymede");
      }

    about.loadCreditsText();
    about.setVisible(true);
  }

  /**
   * Shows the server's message of the day in a dialog.
   */

  public void showMOTD()
  {
    // This will always get the MOTD, even if we've seen it

    StringBuffer m;

    try
      {
	m = session.getMessageHTML("motd", false);

	if (m == null)
	  {
	    m = session.getMessage("motd", false);

	    if (m != null)
	      {
		showMOTD(m.toString(), false);
	      }
	  }
	else
	  {
	    showMOTD(m.toString(), true);
	  }
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx, "Could not get motd");
      }
  }

  /**
   * This method generates a message-of-the-day dialog.
   *
   * @param message The message to display.  May be multiline.  
   * @param html If true, showMOTD() will display the motd with a html
   * renderer, in Swing 1.1b2 and later.
   */

  public void showMOTD(String message, boolean html)
  {
    if (motd == null)
      {
	motd = new messageDialog(client, "MOTD", null);
      }
    
    if (html)
      {
	motd.setHtmlText(message);
      }
    else
      {
	motd.setPlainText(message);
      }

    motd.setVisible(true);
  }

  /**
   * <p>This method is used to display an error dialog for the given exception,
   * and to rethrow it as a RuntimeException.</p>
   *
   * <p>Potentially useful when catching RemoteExceptions from the server.</p>
   */

  public final void processExceptionRethrow(Exception ex)
  {
    processException(ex);

    throw new RuntimeException(ex);
  }

  /**
   * <p>This method is used to display an error dialog for the given exception,
   * and to rethrow it as a RuntimeException.</p>
   *
   * <p>Potentially useful when catching RemoteExceptions from the server.</p>
   */

  public final void processExceptionRethrow(Exception ex, String message)
  {
    processExceptionRethrow(ex, message);

    throw new RuntimeException(ex);
  }

  /**
   * <p>This method is used to display an error dialog for the given
   * exception.</p>
   */

  public final void processException(Exception ex)
  {
    processException(ex, null);
  }

  /**
   * <p>This method is used to display an error dialog for the given
   * exception.</p>
   */

  public final void processException(Exception ex, String message)
  {
    StringWriter stringTarget = new StringWriter();
    PrintWriter writer = new PrintWriter(stringTarget);
    
    ex.printStackTrace(writer);
    writer.close();

    if (ex instanceof NotLoggedInException)
      {
	showNotLoggedIn();
      }
    else
      {
	if (ex instanceof RegexpException)
	  {
	    // don't bother showing them the stack trace if they
	    // entered a bad regexp into a dialog

	    showErrorMessage(ex.getMessage());
	  }
	else
	  {
	    String text;

	    if (message != null)
	      {
		text = message + "\n" + stringTarget.toString();
	      }
	    else
	      {
		text = stringTarget.toString();
	      }

	    showErrorMessage("Exception", text);
	  }
      }

    setNormalCursor();
  }

  public final void showNotLoggedIn()
  {
    showErrorMessage("Error", "Not logged in to the server");
  }

  /**
   * Pops up an error dialog with the default title.
   */

  public final void showErrorMessage(String message)
  {
    showErrorMessage("Error", message);
  }

  /**
   * Pops up an error dialog.
   */

  public final void showErrorMessage(String title, String message)
  {
    showErrorMessage(title, message, getErrorImage());
  }

  /** 
   * Show an error dialog.
   *
   * @param title title of dialog.
   * @param message Text of dialog.
   * @param icon optional icon to display.
   */

  public final void showErrorMessage(String title, String message, Image icon)
  {
    if (debug)
      {
	System.out.println("Error message: " + message);
      }

    final gclient gc = this;
    final String Title = title;
    final String Message = message;
    final Image fIcon = icon;

    SwingUtilities.invokeLater(new Runnable() 
			       {
				 public void run()
				   {
				     new JErrorDialog(gc, Title, Message, fIcon); // implicit show
				   }
			       });

    setStatus(title + ": " + message, 10);
  }


  /**
   * Set the cursor to a wait cursor(usually a watch.)
   */

  public void setWaitCursor()
  {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  /**
   * <p>Set the cursor to the normal cursor(usually a pointer.)</p>
   *
   * <p>This is dependent on the operating system.</p>
   */

  public void setNormalCursor()
  {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  /**
   * <p>This indeicates that something in the database was changed, so
   * cancelling this transaction will have consequences.</p>
   *
   * <p>This should be called whenever the client makes any changes to
   * the database.  That includes creating objects, editting fields of
   * objects, removing objects, renaming, expiring, deleting,
   * inactivating, and so on.  It is very important to call this
   * whenever something might have changed. </p> 
   */

  public final void somethingChanged()
  {
    commit.setEnabled(true);
    cancel.setEnabled(true);
    setSomethingChanged(true);
  }

  /**
   * Sets or clears the client's somethingChanged flag.
   */
  private void setSomethingChanged(boolean state)
  {
    if (debug)
      {
	System.out.println("Setting somethingChanged to " + state);
      }

    somethingChanged = state;
  }

  /**
   * True if something has been changed since the last commit/cancel
   *
   */

  public boolean getSomethingChanged()
  {
    return somethingChanged;
  }

  /**
   * True if we are in an applet context, meaning we don't have access
   * to local files, etc.
   */

  public boolean isApplet()
  {
    return _myglogin.isApplet();
  }

  /**
   * <p>This method takes a ReturnVal object from the server and, if
   * necessary, runs through a wizard interaction sequence, possibly
   * displaying several dialogs before finally returning a final
   * result code.</p>
   *
   * <p>Use the ReturnVal returned from this function after this
   * function is called to determine the ultimate success or failure
   * of any operation which returns ReturnVal, because a wizard
   * sequence may determine the ultimate result.</p>
   *
   * <p>This method should not be synchronized, since handleReturnVal
   * may pop up modal (thread-blocking) dialogs, and if we we
   * synchronize this, some Swing or AWT code seems to block on our
   * synchronization when we do pop-up dialogs.  It's not any of my
   * code, so I assume that AWT tries to synchronize on the frame when
   * parenting a new dialog.</p> 
   */

  public ReturnVal handleReturnVal(ReturnVal retVal)
  {
    Hashtable dialogResults;

    /* -- */

    if (debug)
      {
	System.err.println("gclient.handleReturnVal(): Entering");
      }

    wizardActive++;

    try
      {
	while ((retVal != null) && (retVal.getDialog() != null))
	  {
	    if (debug)
	      {
		System.err.println("gclient.handleReturnVal(): retrieving dialog");
	      }

	    JDialogBuff jdialog = retVal.getDialog();

	    if (debug)
	      {
		System.err.println("gclient.handleReturnVal(): extracting dialog");
	      }

	    DialogRsrc resource = jdialog.extractDialogRsrc(this, null);

	    if (debug)
	      {
		System.err.println("gclient.handleReturnVal(): constructing dialog");
	      }

	    StringDialog dialog = new StringDialog(resource);

	    if (debug)
	      {
		System.err.println("gclient.handleReturnVal(): displaying dialog");
	      }

	    setWaitCursor();

	    try
	      {
		// display the Dialog sent to us by the server, get the
		// result of the user's interaction with it.
	    
		dialogResults = dialog.DialogShow();
	      }
	    finally
	      {
		setNormalCursor();
	      }

	    if (debug)
	      {
		System.err.println("gclient.handleReturnVal(): dialog done");
	      }

	    if (retVal.getCallback() != null)
	      {
		try
		  {
		    if (debug)
		      {
			System.out.println("gclient.handleReturnVal(): Sending result to callback: " + dialogResults);
		      }

		    // send the dialog results to the server

		    retVal = retVal.getCallback().respond(dialogResults);

		    if (debug)
		      {
			System.out.println("gclient.handleReturnVal(): Received result from callback.");
		      }
		  }
		catch (Exception ex)
		  {
		    processExceptionRethrow(ex);
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("gclient.handleReturnVal(): No callback, breaking");
		  }

		break;		// we're done
	      }
	  }
      }
    finally
      {
	wizardActive--;
      }
    
    if (debug)
      {
	System.out.println("gclient.handleReturnVal(): Done with wizards, checking retVal for rescan.");
      }

    // Check for objects that need to be rescanned

    if (retVal == null || !retVal.doRescan())
      {
	return retVal;
      }

    if (debug)
      {
	System.err.println("gclient.handleReturnVal(): rescan dump: " + retVal.dumpRescanInfo());
      }

    Vector objects = retVal.getRescanObjectsList();
	    
    if (objects == null)
      {
	if (debug)
	  {
	    System.err.println("gclient.handleReturnVal(): Odd, was told to rescan, but there's nothing there!");
	  }

	return retVal;
      }
    
    if (debug)
      {
	System.out.println("gclient.handleReturnVal(): Rescanning " + objects.size() + " objects.");
      }
    
    Enumeration invids = objects.elements();

    // Loop over all the invids, and try to find
    // containerPanels for them.
    
    while (invids.hasMoreElements())
      {
	Invid invid = (Invid) invids.nextElement();
		
	if (debug)
	  {
	    System.out.println("gclient.handleReturnVal(): updating invid: " + invid);
	  }

	wp.refreshObjectWindows(invid, retVal);
      }

    if (debug)
      {
	System.err.println("gclient.handleReturnVal(): Exiting handleReturnVal");
      }

    return retVal;
  }

  // Private methods

  /**
   * Creates and initializes the client's toolbar.
   */

  JToolBar createToolbar()
  {
    Insets insets = new Insets(0,0,0,0);
    JToolBar toolBarTemp = new JToolBar();

    toolBarTemp.setBorderPainted(true);
    toolBarTemp.setFloatable(true);
    toolBarTemp.setMargin(insets);

    JButton b = new JButton("Create", new ImageIcon(newToolbarIcon));
    b.setMargin(insets);
    b.setActionCommand("create new object");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Create a new object");
    b.addActionListener(this);
    toolBarTemp.add(b);

    b = new JButton("Edit", new ImageIcon(pencil));
    b.setMargin(insets);
    b.setActionCommand("open object for editing");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Edit an object");
    b.addActionListener(this);
    toolBarTemp.add(b);

    b = new JButton("Delete", new ImageIcon(trash));
    b.setMargin(insets);
    b.setActionCommand("delete an object");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Delete an object");
    b.addActionListener(this);
    toolBarTemp.add(b);

    b = new JButton("Clone", new ImageIcon(cloneIcon));
    b.setMargin(insets);
    b.setActionCommand("clone an object");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Clone an object");
    b.addActionListener(this);
    toolBarTemp.add(b);

    b = new JButton("View", new ImageIcon(search));
    b.setMargin(insets);
    b.setActionCommand("open object for viewing");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("View an object");
    b.addActionListener(this);
    toolBarTemp.add(b);

    b = new JButton("Query", new ImageIcon(queryIcon));
    b.setMargin(insets);
    b.setActionCommand("compose a query");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Compose a query");
    b.addActionListener(this);
    toolBarTemp.add(b);

    // If we decide to have an inactivate-type button on toolbar...
//     b = new JButton("Inactivate", new ImageIcon(inactivateIcon));
//     //b = new JButton(new ImageIcon(inactivateIcon));
//     b.setMargin(insets);
//     b.setActionCommand("inactivate an object");
//     b.setVerticalTextPosition(b.BOTTOM);
//     b.setHorizontalTextPosition(b.CENTER);
//     b.setToolTipText("Inactivate an object");
//     b.addActionListener(this);
//     toolBarTemp.add(b);

    if ((personae != null)  && personae.size() > 1)
      {
	b = new JButton("Persona", new ImageIcon(personaIcon));  
	b.setMargin(insets);
	b.setActionCommand("change persona");
	b.setVerticalTextPosition(b.BOTTOM);
	b.setHorizontalTextPosition(b.CENTER);
	b.setToolTipText("Change Persona");
	b.addActionListener(this);
	toolBarTemp.add(b);
      }
   
    return toolBarTemp;
  }


  ///////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////
  ////            Tree Stuff
  ////
  ///////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////

  /**
   * <p>Clears out the client's tree.</p>
   *
   * <p>All Nodes will be removed, and the Category and BaseNodes will
   * be rebuilt.  No InvidNodes will be added.</P>
   */

  void clearTree()
  {
    tree.clearTree();

    try
      {
	buildTree();
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx, "Could not rebuild tree");
      }
  }

  /**
   * <p>This method builds the initial data structures for the object
   * selection tree, using the base information in the baseHash
   * hashtable gained from the {@link arlut.csd.ganymede.client.Loader Loader}
   * thread.</p>
   */

  void buildTree() throws RemoteException
  {
    if (debug)
      {
	System.out.println("gclient.buildTree(): Building tree");
      }

    // clear the invidNodeHash

    invidNodeHash.clear();

    CategoryTransport transport = session.getCategoryTree(hideNonEditables);

    // get the category dump, save it

    dump = transport.getTree();

    if (debug)
      {
	System.out.println("gclient.buildTree(): got root category: " + dump.getName());
      }

    recurseDownCategories(null, dump);

    if (debug)
      {
	System.out.println("gclient.buildTree(): Refreshing tree");
      }

    tree.refresh();

    if (debug)
      {
	System.out.println("gclient.buildTree(): Done building tree,");
      }
  }

  /**
   * <p>Recurses down the category tree obtained from the server, loading
   * the client's tree with category and object folder nodes.</p>
   */

  void recurseDownCategories(CatTreeNode node, Category c) throws RemoteException
  {
    Vector
      children;

    CategoryNode cNode;

    treeNode 
      prevNode;

    /* -- */
      
    children = c.getNodes();

    prevNode = null;

    for (int i = 0; i < children.size(); i++)
      {
	// find the CategoryNode at this point in the server's category tree

	cNode = (CategoryNode) children.elementAt(i);

	if (cNode instanceof Base)
	  {
	    Base base = (Base) cNode;
	    
	    if (base.isEmbedded())
	      {
		continue;	// we don't want to present embedded objects
	      }
	  }

	// if we have a single category at this level, we don't want
	// to bodily insert it into the tree.. we'll just continue to
	// recurse down with it.

	if ((cNode instanceof Category) && (children.size() == 1))
	  {
	    recurseDownCategories(node, (Category) cNode);
	  }
	else
	  {
	    prevNode = insertCategoryNode(cNode, prevNode, node);

	    if (prevNode instanceof CatTreeNode)
	      {
		recurseDownCategories((CatTreeNode)prevNode, (Category) cNode);
	      }
	  }
      }
  }

  /**
   * Helper method for building tree
   */

  treeNode insertCategoryNode(CategoryNode node, treeNode prevNode, treeNode parentNode) throws RemoteException
  {
    treeNode newNode = null;
      
    if (node instanceof Base)
      {
	Base base = (Base)node;
	boolean canCreate = base.canCreate(getSession());

	newNode = new BaseNode(parentNode, base.getName(), base, prevNode,
			       true, 
			       OPEN_BASE, 
			       CLOSED_BASE,
			       canCreate ? pMenuEditableCreatable : pMenuEditable,
			       canCreate);

	((BaseNode) newNode).showAll(!hideNonEditables);

	shortToBaseNodeHash.put(((BaseNode)newNode).getTypeID(), newNode);
      }
    else if (node instanceof Category)
      {
	Category category = (Category)node;
	newNode = new CatTreeNode(parentNode, category.getName(), category,
				  prevNode, true, 
				  OPEN_CAT, 
				  CLOSED_CAT, 
				  null);
      }
    else if (debug)
      {
	System.out.println("gclient.insertCategoryNode(): Unknown instance: " + node);
      }

    if ((newNode.getParent() == null) && (newNode.getPrevSibling() == null))
      {
	tree.setRoot(newNode);
      }
    else
      {
	tree.insertNode(newNode, false);
      }
    
    return newNode;
  }

  /**
   * <p>This method is used to update the list of object nodes under a given
   * base node in our object selection tree, synchronizing the tree with
   * the actual objects on the server.</p>
   *
   * @param node Tree node corresponding to the object type being refreshed
   * in the client's tree.
   * @param doRefresh If true, causes the tree to update its display.
   */
  
  void refreshObjects(BaseNode node, boolean doRefresh) throws RemoteException
  {
    Invid invid = null;
    String label = null;
    InvidNode oldNode, newNode, fNode;

    ObjectHandle handle = null;
    Vector objectHandles;
    objectList objectlist = null;

    Short Id;

    /* -- */

    Id = node.getTypeID();

    // get the object list.. this call will automatically handle
    // caching for us.

    objectlist = getObjectList(Id, node.isShowAll());

    objectHandles = objectlist.getObjectHandles(true, node.isShowAll()); // include inactives

    // **
    //
    // The loop below goes over the sorted list of objectHandles and
    // the sorted list of nodes in the tree under this particular baseNode,
    // comparing as the loop progresses, adding or removing nodes from the
    // tree to match the current contents of the objectHandles list
    //
    // The important variables in the loop are fNode, which points to the
    // current node in the subtree that we're examining, and i, which
    // is counting our way through the objectHandles Vector.
    // 
    // **

    oldNode = null;
    fNode = (InvidNode) node.getChild();
    int i = 0;
	
    while ((i < objectHandles.size()) || (fNode != null))
      {
	if (i < objectHandles.size())
	  {
	    handle = (ObjectHandle) objectHandles.elementAt(i);

	    if (!node.isShowAll() && !handle.isEditable())
	      {
		i++;		// skip this one, we're not showing non-editables
		continue;
	      }

	    invid = handle.getInvid();
	    label = handle.getLabel();
	  }
	else
	  {
	    // We've gone past the end of the list of objects in this
	    // object list.. from here on out, we're going to wind up
	    // removing anything we find in this subtree

	    handle = null;
	    label = null;
	    invid = null;
	  }

	// insert a new node in the tree, change the label, or remove
	// a node

	if ((fNode == null) ||
	    ((invid != null) && 
	     ((label.compareTo(fNode.getText())) < 0)))
	  {
	    // If we have an invid/label in the object list that's not
	    // in the tree, we need to insert it

	    InvidNode objNode = new InvidNode(node, 
					      handle.isInactive() ? (label + " (inactive)") :label,
					      invid,
					      oldNode, false,
					      handle.isEditable() ? OPEN_FIELD : OBJECTNOWRITE,
					      handle.isEditable() ? CLOSED_FIELD : OBJECTNOWRITE,
					      handle.isEditable() ? (node.canInactivate()
								     ? objectInactivatePM : objectRemovePM) : objectViewPM,
					       
					      handle);
	    
	    if (invid != null)
	      {
		invidNodeHash.put(invid, objNode);

		if (createdObjectsWithoutNodes.containsKey(invid))
		  {
		    if (false)
		      {
			System.out.println("Found this object in the creating objectsWithoutNodes hash: " + 
					   handle.getLabel());
		      }
		    		    
		    createHash.put(invid, new CacheInfo(node.getTypeID(),
							(handle.getLabel() == null) ? "New Object" : handle.getLabel(),
							null, handle));
		    createdObjectsWithoutNodes.remove(invid);
		  }
		
		setIconForNode(invid);
	      }

	    tree.insertNode(objNode, false);

	    oldNode = objNode;
	    fNode = (InvidNode) oldNode.getNextSibling();
	    
	    i++;
	  }
	else if ((invid == null) ||
		 ((label.compareTo(fNode.getText())) > 0))
	  {
	    // We've found a node in the tree without a matching
	    // node in the object list.  Delete it!

	    newNode = (InvidNode) fNode.getNextSibling();
	    tree.deleteNode(fNode, false);

	    fNode = newNode;
	  }
	else if (fNode.getInvid().equals(invid))
	  {
	    // we've got a node in the tree that matches the
	    // invid of the current object in the object list,
	    // but the label may possibly have changed, so we'll
	    // go ahead and re-set the label, just to be sure

	    if (handle.isInactive())
	      {
		fNode.setText(label + " (inactive)");
	      }
	    else
	      {
		fNode.setText(label);
	      }

	    oldNode = fNode;
	    fNode = (InvidNode) oldNode.getNextSibling();

	    setIconForNode(invid);

	    i++;
	  }
      }

    if (doRefresh)
      {
	tree.refresh();
      }
  }

  /**
   * <p>Updates the tree for the nodes that might have changed.</p>
   *
   * <p>This method fixes all the icons, removing icons that were
   * marked as to-be-deleted or dropped, and cleans out the various
   * hashes.  Only call this when commit is clicked.  This replaces
   * refreshTree(boolean committed), because all the refreshing to be
   * done after a cancel is now handled in the cancelTransaction()
   * method directly.</p>
   *
   * <p>This method is precisely analagous in function to
   * {@link arlut.csd.ganymede.client.gclient#cleanUpAfterCancel() cleanUpAfterCancel()},
   * except for use after a commit.</p> 
   */

  void refreshTreeAfterCommit() throws RemoteException
  {
    Invid invid = null;
    InvidNode node = null;

    /* -- */

    //
    // First get rid of deleted nodes
    //

    synchronized (deleteHash)
      {
	Enumeration deleted = deleteHash.keys();

	while (deleted.hasMoreElements())
	  {
	    invid = (Invid)deleted.nextElement();
	    node = (InvidNode)invidNodeHash.get(invid);
	    
	    if (node != null)
	      {
		if (debug)
		  {
		    System.out.println("gclient.refreshTreeAfterCommit(): Deleteing node: " + node.getText());
		  }
		
		tree.deleteNode(node, false);
		invidNodeHash.remove(invid);
	      }
	  }
    
	deleteHash.clear();
      }

    //
    // Now change the created nodes
    //

    invid = null;

    Vector changedInvids = new Vector();
    Enumeration created = createHash.keys();

    while (created.hasMoreElements())
      {
	invid = (Invid) created.nextElement();

	changedInvids.addElement(invid);
      }

    createHash.clear();
    createdObjectsWithoutNodes.clear();

    invid = null;
    Enumeration changed = changedHash.keys();
    
    while (changed.hasMoreElements())
      {
	invid = (Invid) changed.nextElement();

	changedInvids.addElement(invid);
      }

    changedHash.clear();

    if (changedInvids.size() > 0)
      {
	if (debug)
	  {
	    System.err.println("gclient.refreshTreeAfterCommit(): refreshing created objects");
	  }

	refreshChangedObjectHandles(changedInvids, true);

	if (debug)
	  {
	    System.err.println("gclient.refreshTreeAfterCommit(): done refreshing created objects");
	  }
      }

    tree.refresh();
  }

  /**
   * <p>Queries the server for status information on a vector of 
   * {@link arlut.csd.ganymede.common.Invid invid}'s that were touched
   * in some way by the client during the recent transaction.
   * The results from the queries are used to update the icons
   * in the tree.</p>
   *
   * <p>Called by refreshTreeAfterCommit().</p>
   *
   * <p>This method is called from
   * {@link arlut.csd.ganymede.client.gclient#refreshTreeAfterCommit() refreshTreeAfterCommit()}.</p>
   *
   * @param paramVect Vector of invid's to refresh.  
   * @param afterCommit If true, this method will update the client's status
   * bar as it progresses.
   */

  public void refreshChangedObjectHandles(Vector paramVect, boolean afterCommit)
  {
    Invid invid;
    Short objectTypeKey = null;

    /* -- */

    if (afterCommit)
      {
	setStatus("refreshing object handles after commit");
      }

    try
      {
	QueryResult result = session.queryInvids(paramVect);

	// now get the results
	    
	Vector handleList = result.getHandles();
	    
	// and update anything we've got in the tree
	
	if (afterCommit)
	  {
	    setStatus("Updating object handles in tree");
	  }
	    
	for (int i = 0; i < handleList.size(); i++)
	  {
	    ObjectHandle newHandle = (ObjectHandle) handleList.elementAt(i);
	    invid = newHandle.getInvid();

	    objectTypeKey = new Short(invid.getType());

	    InvidNode nodeToUpdate = (InvidNode) invidNodeHash.get(invid);
		
	    if (nodeToUpdate != null)
	      {
		if (debug)
		  {
		    System.err.println("gclient.refreshChangedObjectHandles(): got object handle refresh for " + 
				       newHandle.debugDump());
		  }
		
		nodeToUpdate.setHandle(newHandle);
		    
		if (paramVect == null)
		  {
		    changedHash.remove(newHandle.getInvid());
		  }

		setIconForNode(newHandle.getInvid());
	      }
	    else if (debug)
	      {
		System.err.println("gclient.refreshChangedObjectHandles(): null node for " + 
				   newHandle.debugDump());
	      }
	    
	    // and update our tree cache for this item

	    objectList list = cachedLists.getList(objectTypeKey);

	    if (list != null)
	      {
		list.removeInvid(newHandle.getInvid());
		list.addObjectHandle(newHandle);
	      }
	  }
      }
    catch (Exception ex)
      {
	processExceptionRethrow(ex, "Couldn't get object handle vector refresh");
      }

    if (afterCommit)
      {
	setStatus("Completed refresh of changed objects in the tree.");
      }
  }

  /**
   * This method does the same thing as refreshChangedObjectHandles(), but
   * for a single object only.
   */

  public void refreshChangedObject(Invid invid)
  {
    Vector paramVec = new Vector();

    paramVec.addElement(invid);

    refreshChangedObjectHandles(paramVec, false);
  }

  /**
   * <p>Updates a database object's icon in the tree display.  This method
   * uses the various client-side caches and hashes to determine the proper
   * icon for the node.</p>
   *
   * <p>This method does not actually induce the tree to refresh itself,
   * and may be called in bulk for a lot of nodes efficiently.</p>
   */

  public void setIconForNode(Invid invid)
  {
    boolean treeNodeDebug = false;

    InvidNode node = (InvidNode) invidNodeHash.get(invid);

    if (node == null)
      {
	return;
      }

    if (node == null)
      {
	if (debug)
	  {
	    System.out.println("gclient.setIconForNode(): There is no node for this invid, silly!");
	  }
      }
    else
      {
	ObjectHandle handle = node.getHandle();

	// if we can't edit it, assume it'll never be anything other
	// than inaccessible

	if (!handle.isEditable())
	  {
	    node.setImages(OBJECTNOWRITE, OBJECTNOWRITE);
	    node.setMenu(objectViewPM);
	    return;
	  }

	// The order here matters, because it might be in more than
	// one hash.  So put the most important stuff first

	if (deleteHash.containsKey(invid))
	  {
	    if (treeNodeDebug)
	      {
		System.out.println("Setting icon to delete.");
	      }

	    node.setImages(OPEN_FIELD_DELETE, CLOSED_FIELD_DELETE);
	  }
	else if (createHash.containsKey(invid))
	  {
	    if (treeNodeDebug)
	      {
		System.out.println("Setting icon to create.");
	      }

	    node.setImages(OPEN_FIELD_CREATE, CLOSED_FIELD_CREATE);
	  }
	else if (handle != null)
	  {
	    if (handle.isInactive())
	      {
		if (treeNodeDebug)
		  {
		    System.out.println("inactivate");
		  }

		node.setText(handle.getLabel() + " (inactive)");

		node.setMenu(objectReactivatePM);
		node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
	      }
	    else
	      {
		node.setText(handle.getLabel());

		BaseDump bd = (BaseDump) getBaseMap().get(new Short(node.getInvid().getType()));

		if (bd.canInactivate())
		  {
		    node.setMenu(objectInactivatePM);
		  }
		else 
		  {
		    node.setMenu(objectRemovePM);
		  }

		// now take care of the rest of the menus.

		if (handle.isExpirationSet())
		  {
		    if (treeNodeDebug)
		      {
			System.out.println("isExpirationSet");
		      }

		    node.setImages(OPEN_FIELD_EXPIRESET, CLOSED_FIELD_EXPIRESET);
		  }
		else if (handle.isRemovalSet())
		  {
		    if (treeNodeDebug)
		      {
			System.out.println("isRemovalSet()");
		      }

		    node.setMenu(objectReactivatePM);
		    node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
		  }
		else if (changedHash.containsKey(invid))
		  {
		    if (treeNodeDebug)
		      {
			System.out.println("Setting icon to edit.");
		      }
		
		    node.setImages(OPEN_FIELD_CHANGED, CLOSED_FIELD_CHANGED);
		  }
		else // nothing special in handle
		  {
		    node.setImages(OPEN_FIELD, CLOSED_FIELD);
		  } 
	      }
	  }
	else // no handle
	  {
	    if (changedHash.containsKey(invid))
	      {
		if (treeNodeDebug)
		  {
		    System.out.println("Setting icon to edit.");
		  }
		
		node.setImages(OPEN_FIELD_CHANGED, CLOSED_FIELD_CHANGED);
	      }
	    else
	      {
		if (treeNodeDebug)
		  {
		    System.out.println("normal");
		  }

		node.setImages(OPEN_FIELD, CLOSED_FIELD);
	      }
	  }
      }
  }

  /********************************************************************************
   *
   * actions on objects.
   *
   *
   * These are the methods to use to do something to an object.
   *
   ********************************************************************************/

  /** 
   * <p>Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel} 
   * window to allow the user to edit an object.</p>
   *
   * <p>Use this to edit objects, so gclient can keep track of the
   * caches, tree nodes, and all the other dirty work.  This should be
   * the only place windowPanel.addWindow() is called for editing
   * purposes.</p>
   *
   * @param invid id for the object to be edited in the new window.  */

  public void editObject(Invid invid)
  {
    editObject(invid, null);
  }

  /**
   * <p>Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel}
   * window to allow the user to edit an object.</p>
   *
   * <p>Use this to edit objects, so gclient can keep track of the
   * caches, tree nodes, and all the other dirty work.  This should be
   * the only place windowPanel.addWindow() is called for editing
   * purposes.</p>
   *
   * @param invid id for the object to be edited in the new window.
   * @param objectType String describing the kind of object being edited,
   * used in the titlebar of the window created.
   */

  public void editObject(Invid invid, String objectType)
  {
    if (deleteHash.containsKey(invid))
      {
	showErrorMessage("Client Warning",
			 getObjectTitle(invid) + " has already been deleted.\n\n" +
			 "Cancel this transaction if you do not wish to delete this object after all.",
			 getErrorImage());
	return;
      }

    if (wp.isOpenForEdit(invid))
      {
	showErrorMessage("Object already being edited", 
			 "You already have a window open to edit " + getObjectTitle(invid) + ".", 
			 getErrorImage());
	return;
      }

    if (objectType == null || objectType.equals(""))
      {
	objectType = getObjectType(invid);
      }

    ObjectHandle handle = getObjectHandle(invid);
    
    if (handle != null && handle.isInactive())
      {
	Hashtable dialogResults = null;

	DialogRsrc rsrc = new DialogRsrc(this,
					 "Edit or Reactivate?", 
					 "Warning, " + getObjectTitle(invid) + 
					 " is currently inactivated.  If you are seeking to reactivate this object, " +
					 "it is recommended that you use the server's reactivation wizard rather " +
					 "than manually editing it.\n\nCan I go ahead and shift you over " +
					 "to the server's reactivation wizard?",
					 "Yes, Reactivate", "No, I want to Edit it!",
					 "question.gif", null);

	StringDialog verifyDialog = new StringDialog(rsrc);

	dialogResults = verifyDialog.DialogShow();

	if (dialogResults != null)
	  {
	    reactivateObject(invid);
	    return;
	  }
      }
    
    try
      {
	ReturnVal rv = handleReturnVal(session.edit_db_object(invid));

	db_object o = (db_object) rv.getObject();

	if (o == null)
	  {
	    // handleReturnVal threw up a dialog for us if needed

	    return;
	  }

	wp.addWindow(invid, o, true, objectType);
	  
	changedHash.put(invid, invid);

	// we don't need to do a full refresh of it, since we've just
	// checked it out..

	setIconForNode(invid);
	tree.refresh();
      }
    catch(Exception rx)
      {
	processExceptionRethrow(rx, "Could not edit object");
      }
  }

  /** 
   * <p>Creates a new object on the server and opens a new
   * client {@link arlut.csd.ganymede.client.framePanel framePanel}
   * window to allow the user to edit the new object.</p>
   *
   * @param type Type of object to be created
   */

  public void cloneObject(Invid origInvid)
  {
    Invid invid = null;
    db_object obj = null;

    /* -- */

    if (deleteHash.containsKey(origInvid))
      {
	showErrorMessage("Can't clone a deleted object",
			 getObjectTitle(origInvid) + " has already been deleted.\n\n" +
			 "Cancel this transaction if you do not wish to delete this object after all.",
			 getErrorImage());
	return; 
      }

    // if the admin is a member of more than one owner group, ask what
    // owner groups they want new objects to be placed in

    if (!defaultOwnerChosen)
      {
	chooseDefaultOwner(false);
      }
    
    setWaitCursor();

    try
      {
	try
	  {
	    ReturnVal rv = handleReturnVal(session.clone_db_object(origInvid));
	    obj = (db_object) rv.getObject();
	  }
	catch (Exception rx)
	  {
	    processExceptionRethrow(rx, "Exception creating new object");
	  }

	// we'll depend on handleReturnVal() above showing the user a rejection
	// dialog if the object create was rejected

	if (obj == null)
	  {
	    return;
	  }

	try
	  {
	    invid = obj.getInvid();
	  }
	catch (Exception rx)
	  {
	    processExceptionRethrow(rx, "Could not get invid");
	  }

	ObjectHandle handle = new ObjectHandle("New Object", invid, false, false, false, true);
       
	wp.addWindow(invid, obj, true, null, true);

	Short typeShort = new Short(invid.getType());
    
	if (cachedLists.containsList(typeShort))
	  {
	    objectList list = cachedLists.getList(typeShort);
	    list.addObjectHandle(handle);
	  }
    
	// If the base node is open, deal with the node.

	BaseNode baseN = null;

	if (shortToBaseNodeHash.containsKey(typeShort))
	  {
	    baseN = (BaseNode)shortToBaseNodeHash.get(typeShort);

	    if (baseN.isLoaded())
	      {
		InvidNode objNode = new InvidNode(baseN, 
						  handle.getLabel(),
						  invid,
						  null, false,
						  OPEN_FIELD_CREATE,
						  CLOSED_FIELD_CREATE,
						  baseN.canInactivate() ? objectInactivatePM : objectRemovePM,
						  handle);
	    
		createHash.put(invid, new CacheInfo(typeShort, handle.getLabel(), null, handle));

		invidNodeHash.put(invid, objNode);
		setIconForNode(invid);

		tree.insertNode(objNode, true);  // the true means the tree will refresh
	      }
	    else
	      {
		// this hash is used when creating the node for the object
		// in the tree.  This way, if a new object is created
		// before the base node is expanded, the new object will
		// have the correct icon.

		createdObjectsWithoutNodes.put(invid, baseN);
	      }
	  }
	
	somethingChanged();
      }
    finally
      {
	setNormalCursor();
      }
  }

  /** 
   * <p>Creates a new object on the server and opens a new
   * client {@link arlut.csd.ganymede.client.framePanel framePanel}
   * window to allow the user to edit the new object.</p>
   *
   * @param type Type of object to be created
   */

  public db_object createObject(short type)
  {
    Invid invid = null;
    db_object obj = null;

    /* -- */

    // if the admin is a member of more than one owner group, ask what
    // owner groups they want new objects to be placed in

    if (!defaultOwnerChosen)
      {
	chooseDefaultOwner(false);
      }
    
    setWaitCursor();

    try
      {
	try
	  {
	    ReturnVal rv = handleReturnVal(session.create_db_object(type));
	    obj = (db_object) rv.getObject();
	  }
	catch (Exception rx)
	  {
	    processExceptionRethrow(rx, "Exception creating new object");
	  }

	// we'll depend on handleReturnVal() above showing the user a rejection
	// dialog if the object create was rejected

	if (obj == null)
	  {
	    return null;
	  }

	try
	  {
	    invid = obj.getInvid();
	  }
	catch (Exception rx)
	  {
	    processExceptionRethrow(rx, "Could not get invid");
	  }

	ObjectHandle handle = new ObjectHandle("New Object", invid, false, false, false, true);
       
	wp.addWindow(invid, obj, true, null, true);

	Short typeShort = new Short(type);
    
	if (cachedLists.containsList(typeShort))
	  {
	    objectList list = cachedLists.getList(typeShort);
	    list.addObjectHandle(handle);
	  }
    
	// If the base node is open, deal with the node.

	BaseNode baseN = null;

	if (shortToBaseNodeHash.containsKey(typeShort))
	  {
	    baseN = (BaseNode)shortToBaseNodeHash.get(typeShort);

	    if (baseN.isLoaded())
	      {
		InvidNode objNode = new InvidNode(baseN, 
						  handle.getLabel(),
						  invid,
						  null, false,
						  OPEN_FIELD_CREATE,
						  CLOSED_FIELD_CREATE,
						  baseN.canInactivate() ? objectInactivatePM : objectRemovePM,
						  handle);
	    
		createHash.put(invid, new CacheInfo(typeShort, handle.getLabel(), null, handle));

		invidNodeHash.put(invid, objNode);
		setIconForNode(invid);

		tree.insertNode(objNode, true);  // the true means the tree will refresh
	      }
	    else
	      {
		// this hash is used when creating the node for the object
		// in the tree.  This way, if a new object is created
		// before the base node is expanded, the new object will
		// have the correct icon.

		createdObjectsWithoutNodes.put(invid, baseN);
	      }
	  }
	
	somethingChanged();
      }
    finally
      {
	setNormalCursor();
      }

    return obj;
  }

  /**
   * <p>Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel} 
   * window to view the object corresponding to the given invid.</p>
   */

  public void viewObject(Invid invid)
  {
    viewObject(invid, null);
  }

  /**
   * <p>Opens a new {@link arlut.csd.ganymede.client.framePanel framePanel}
   * window to view the object corresponding to the given invid.</p>
   *
   * @param objectType Type of the object to be viewed.. if this is
   * null, the server will be queried to determine the type of object
   * for the title-bar of the view object window.  By providing it
   * here from a local cache, and server-call can be saved.
   */

  public void viewObject(Invid invid, String objectType)
  {
    if (deleteHash.containsKey(invid))
      {
	// This one has been deleted
	showErrorMessage("Client Warning",
			 "This object has already been deleted.\n\n" +
			 "Cancel this transaction if you do not wish to delete this object after all.",
			 getErrorImage());
	return;
      }

    try
      {
	ReturnVal rv = handleReturnVal(session.view_db_object(invid));
	db_object object = (db_object) rv.getObject();

	// we'll assume handleReturnVal() will display any rejection
	// dialogs from the server

	if (object == null)
	  {
	    return;
	  }

	wp.addWindow(invid, object, false, objectType);
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx, "Could not view object");
      }
  }

  /**
   * <p>Marks an object on the server as deleted.  The object will not
   * actually be removed from the database until the transaction is
   * committed.</p>
   *
   * <p>This method does a fair amount of internal bookkeeping to manage
   * the client's tree display, status caching, etc.</p>
   *
   * @param invid The object invid identifier to be deleted
   * @param showDialog If true, we'll show a dialog box asking the user
   * if they are sure they want to delete the object in question.
   */

  public void deleteObject(Invid invid, boolean showDialog)
  {
    ReturnVal retVal;
    boolean ok = false;

    /* -- */

    if (deleteHash.containsKey(invid))
      {
	showErrorMessage("Object already deleted",
			 getObjectTitle(invid) + " has already been marked as deleted.\n\n" +
			 "You can hit the commit button to permanently get rid of this object, or you can hit " +
			 "the cancel button to undo everything.",
			 getErrorImage());
	return; 
      }
    
  
    // we can delete objects if they are newly created.. the server has support
    // for discarding newly created objects, in fact

    if (wp.isOpenForEdit(invid) && !wp.isApprovedForClosing(invid))
      {	
	showErrorMessage("Object being edited",
			 "You are currently editing " + getObjectTitle(invid) + ".  I can't delete this object while you are editing it.",
			 getErrorImage());
	return;
      }
    
    if (showDialog)
      {
	StringDialog d = new StringDialog(this, "Verify deletion", 
					  "Are you sure you want to delete " + 
					  getObjectTitle(invid) + "?",
					  "Yes", "No", getQuestionImage());
	Hashtable result = d.DialogShow();
	
	if (result == null)
	  {
	    setStatus("Cancelled!");
	    
	    return;
	  }
      }

    setWaitCursor();

    try
      {
	Short id = new Short(invid.getType());

	if (debug)
	  {
	    System.out.println("Deleting invid= " + invid);
	  }

	// Delete the object

	retVal = session.remove_db_object(invid);

	ok = (retVal == null) ? true : retVal.didSucceed();

	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);
	  }

	if (ok)
	  {
	    // InvidNode node = (InvidNode)invidNodeHash.get(invid);

	    // Check out the deleteHash.  If this one is already on there,
	    // then I don't know what to do.  If it isn't, then add a new
	    // cache info.  I guess maybe update the name or something,
	    // if it is on there.

	    CacheInfo info = null;

	    // Take this object out of the cachedLists, if it is in there

	    if (cachedLists.containsList(id))
	      {
		String label = session.viewObjectLabel(invid);

		if (debug)
		  {
		    System.out.println("This base has been hashed.  Removing: " + label);
		  }

		objectList list = cachedLists.getList(id);

		ObjectHandle h = list.getObjectHandle(invid);
		list.removeInvid(invid);

		info = new CacheInfo(id, label, null, h);
	      }
	    else
	      {
		String label = session.viewObjectLabel(invid);
		info = new CacheInfo(id, label, null);
	      }

	    if (deleteHash.containsKey(invid))
	      {
		if (debug)
		  {
		    System.out.println("already deleted, nothing to change, right?");
		  }
	      }
	    else
	      {
		deleteHash.put(invid, info);
	      }

	    if (invidNodeHash.containsKey(invid))
	      {
		setIconForNode(invid);
		tree.refresh();
	      }

	    setStatus("Object will be deleted when commit is clicked.");
	    somethingChanged();
	  }
	else
	  {
	    setStatus("Delete Failed.");
	  }
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx, "Could not delete object");
      }
    finally
      {
	setNormalCursor();
      }

    return;
  }

  /** 
   * <p>Marks an object on the server as inactivated.  The object will not
   * actually be removed from the database until the transaction is
   * committed.  Note that the inactivation request will typically cause
   * a dialog to come back from the server requesting the user fill in
   * parameters describing how the object is to be inactivated.</p>
   *
   * <p>This method does a fair amount of internal bookkeeping to manage
   * the client's tree display, status caching, etc.</p>
   */

  public void inactivateObject(Invid invid)
  {
    boolean ok = false;
    ReturnVal retVal;

    /* -- */

    if (deleteHash.containsKey(invid))
      {
	showErrorMessage("Client Warning",
			 getObjectTitle(invid) + " has already been deleted.\n\n" +
			 "Cancel this transaction if you do not wish to delete this object after all.",
			 getErrorImage());
	return;
      }

    if (wp.isOpenForEdit(invid))
      {
	showErrorMessage("Object already being edited", 
			 "I can't inactivate this object while you have a window open to edit " + getObjectTitle(invid) + ".", 
			 getErrorImage());
	return;
      }

    setStatus("inactivating " + invid);
    setWaitCursor();

    try
      {
	retVal = session.inactivate_db_object(invid);
	    
	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);

	    if (retVal == null)
	      {
		ok = true;
	      }
	    else
	      {
		ok = retVal.didSucceed();
	      }
	  }
	else
	  {
	    ok = true;
	  }

	if (ok)
	  {
	    // remember that we changed this object for the refreshChangedObjectHandles

	    changedHash.put(invid, invid);

	    // refresh it now
		
	    refreshChangedObject(invid);

	    // and update the tree

	    tree.refresh();
	    setStatus("Object inactivated.");
	    somethingChanged();
	  }
	else
	  {
	    setStatus("Could not inactivate object.");
	  }
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx, "Could not inactivate object");
      }
    finally
      {
	setNormalCursor();
      }
  }

  /**
   * <p>Reactivates an object that was previously inactivated. The
   * object's status will not actually be changed in the database
   * until the transaction is committed.  Note that the reactivation
   * request will typically cause a dialog to come back from the
   * server requesting the user fill in parameters describing how the
   * object is to be reactivated.</p>
   *
   * <p>Typically reactivating an object involves clearing the removal
   * date from I think you should call this from the expiration date
   * panel if the date is cleared.</p>
   */

  public boolean reactivateObject(Invid invid)
  {
    ReturnVal retVal;
    boolean ok = false;

    /* -- */

    try
      {
	setWaitCursor();
	retVal = session.reactivate_db_object(invid);

	if (retVal == null)
	  {
	    // It worked
	    ok = true;
	  }
	else
	  {
	    retVal = handleReturnVal(retVal);

	    if (retVal == null)
	      {
		ok = true;
	      }
	    else
	      {
		ok = retVal.didSucceed();
	      }
	  }
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx, "Could not reactivate object");
      }

    if (ok)
      {
	somethingChanged();
	setStatus("Object reactivated.");

	// remember that this invid has been edited, and will need
	// to be refreshed on commit

	changedHash.put(invid, invid);

	refreshChangedObject(invid);

	tree.refresh();
	setNormalCursor();
      }

    return ok;    
  }

  /**
   * Show the create object dialog, let the user choose
   * to create or not create an object.
   */

  void createObjectDialog()
  {
    // The dialog is modal, and will set itself visible when created.
    // If we have already created it, we'll just pack it and make it
    // visible

    if (createDialog == null)
      {
	createDialog = new createObjectDialog(this);
      }
    else
      {
	createDialog.pack();	// force it to re-center itself.
	createDialog.setVisible(true);
      }
  }
  
  /**
   * <p>Opens a dialog to let the user choose an object for editing, and 
   * if cancel is not chosen, the object is opened for editing.</p>
   *
   * <p>If an object node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected node.</p>
   */

  void editObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }
    else
      {
	// if we have a node selected, recreate the dialog so that it
	// will get re-initialized.

	if (selectedNode != null && selectedNode instanceof InvidNode)
	  {
	    openDialog.dispose();
	    openDialog = new openObjectDialog(this);	    
	  }
      }

    openDialog.setText("Open object for editing");
    openDialog.setIcon(new ImageIcon(pencil));
    openDialog.setReturnEditableOnly(true);
    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	if (debug)
	  {
	    System.out.println("Canceled");
	  }
      }
    else
      {
	editObject(invid, openDialog.getTypeString());
      }
  }

  /**
   * <p>Opens a dialog to let the user choose an object for viewing,
   * and if cancel is not chosen, the object is opened for viewing.</p>
   *
   * <p>If an object node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected node.</p>
   */

  void viewObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }
    else
      {
	// if we have a node selected, recreate the dialog so that it
	// will get re-initialized.

	if (selectedNode != null && selectedNode instanceof InvidNode)
	  {
	    openDialog.dispose();
	    openDialog = new openObjectDialog(this);	    
	  }
      }

    openDialog.setText("Open object for viewing");
    openDialog.setIcon(new ImageIcon(search));
    openDialog.setReturnEditableOnly(false);

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	if (debug)
	  {
	    System.out.println("Canceled");
	  }
      }
    else
      {
	viewObject(invid);
      }
  }

  /**
   * <p>Opens a dialog to let the user choose an object for inactivation,
   * and if cancel is not chosen, the object is opened for inactivation.</p>
   *
   * <p>If an object node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected node.</p>
   */

  void inactivateObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }
    else
      {
	// if we have a node selected, recreate the dialog so that it
	// will get re-initialized.

	if (selectedNode != null && selectedNode instanceof InvidNode)
	  {
	    openDialog.dispose();
	    openDialog = new openObjectDialog(this);	    
	  }
      }

    openDialog.setText("Choose object to be inactivated");
    openDialog.setIcon(null);
    openDialog.setReturnEditableOnly(true);

    Invid invid = openDialog.chooseInvid();
    
    inactivateObject(invid);
  }

  /**
   * <p>Opens a dialog to let the user choose an object for deletion,
   * and if cancel is not chosen, the object is opened for deletion.</p>
   *
   * <p>If a node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected object.</p>
   */

  void deleteObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }
    else
      {
	// if we have a node selected, recreate the dialog so that it
	// will get re-initialized.

	if (selectedNode != null && selectedNode instanceof InvidNode)
	  {
	    openDialog.dispose();
	    openDialog = new openObjectDialog(this);	    
	  }
      }

    openDialog.setText("Choose object to be deleted");
    openDialog.setIcon(new ImageIcon(trash));
    openDialog.setReturnEditableOnly(true);

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	if (debug)
	  {
	    System.out.println("Canceled");
	  }
      }
    else
      {
	deleteObject(invid, true);
      }
  }

  /**
   * <p>Opens a dialog to let the user choose an object for cloning,
   * and if cancel is not chosen, the object is opened for cloning.</p>
   *
   * <p>If a node is selected in the client's tree, the dialog will
   * be pre-loaded with the type and name of the selected object.</p>
   */

  void cloneObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }
    else
      {
	// if we have a node selected, recreate the dialog so that it
	// will get re-initialized.

	if (selectedNode != null && selectedNode instanceof InvidNode)
	  {
	    openDialog.dispose();
	    openDialog = new openObjectDialog(this);	    
	  }
      }

    openDialog.setText("Choose object to be cloned");
    openDialog.setIcon(new ImageIcon(cloneIcon));
    openDialog.setReturnEditableOnly(true);

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	if (debug)
	  {
	    System.out.println("Canceled");
	  }
      }
    else
      {
	cloneObject(invid);
      }
  }

  /**
   * <p>Creates and presents a dialog to let the user change their selected persona.</p>
   *
   * <p>gclient's personaListener reacts to events from the persona change
   * dialog and will react appropriately as needed.  This method doesn't
   * actually do anything other than display the dialog.</p>
   *
   * <p>PersonaDialog is modal, however, so this method will block until the
   * user makes a choice in the dialog box.</p>
   */

  void changePersona(boolean requirePassword)
  {
    personaDialog = new PersonaDialog(client, requirePassword);
    personaDialog.pack();	// force it to re-center itself.
    personaDialog.setVisible(true); // block
  }

  /**
   * <p>Returns a reference to the most recently created persona dialog.</p>
   */

  PersonaDialog getPersonaDialog()
  {
    return personaDialog;
  }

  /**
   * <p>Logs out from the client.</p>
   *
   * <p>This method does not do any checking, it just logs out.</p>
   */

  void logout()
  {
    // glogin's logout method will call our cleanUp() method on the
    // GUI thread.

    _myglogin.logout();
  }

  /**
   * <p>Create a custom query filter.</p>
   *
   * <p>The filter is used to limit the output on a query, so that
   * supergash can see the world through the eyes of a less-privileged
   * persona.  This seemed like a good idea at one point, not sure how
   * valuable this really is anymore.</p>
   */

  public void chooseFilter()
  {
    if (filterDialog == null)
      {
	filterDialog = new JFilterDialog(this);
      }
    else
      {
	filterDialog.setVisible(true);
      }
  }

  /**
   * <p>This method is called by the {@link
   * arlut.csd.ganymede.client.JFilterDialog JFilterDialog} class when
   * the owner list filter is changed, to refresh the tree's display
   * of all object lists loaded into the client so that only those
   * objects matching the owner list filter are visible.</p>
   */

  public void updateAfterFilterChange()
  {
    clearCaches();
    updateTreeAfterFilterChange(tree.getRoot());
    tree.refresh();

    // update all our object editing windows so that we can refresh
    // the choice lists.. ? -- not sure why this logic is being done
    // here, actually.. this dates back to revision 4596, Oct 30, 2001

    wp.refreshObjectWindows(null, null);
  }

  /**
   * <p>This method updates all category and base nodes at or under
   * the given node, and all category and base nodes that are nextSiblings
   * to the given node.</p>
   */

  private void updateTreeAfterFilterChange(treeNode node)
  {
    if (node == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("updateAfterFilterChange examining: " + node);
      }

    if (node instanceof BaseNode)
      {
	while (node instanceof BaseNode)
	  {
	    if (node.getChild() != null)
	      {
		if (debug)
		  {
		    System.err.println("Updating " + node);
		  }

		try
		  {
		    refreshObjects((BaseNode) node, false);
		  }
		catch (Exception ex)
		  {
		    processExceptionRethrow(ex, "Could not refresh base");
		  }
	      }

	    node = node.getNextSibling();
	  }
      }

    if (node instanceof CatTreeNode)
      {
	updateTreeAfterFilterChange(node.getChild());
	updateTreeAfterFilterChange(node.getNextSibling());
      }
  }

  /**
   * <p>Chooses the default owner group for a newly created object.</p>
   *
   * <p>This must be called before Session.create_db_object is called.</p>
   */

  public void chooseDefaultOwner(boolean forcePopup)
  {
    ReturnVal retVal = null;
    
    if (ownerGroups == null)
      {
	try
	  {
	    ownerGroups = session.getOwnerGroups().getListHandles();
	  }
	catch (Exception rx)
	  {
	    processExceptionRethrow(rx, "Couldn't load owner groups");
	  }

	if (ownerGroups == null)
	  {
	    throw new RuntimeException("Whoa!  groups is null");
	  }
      }

    if (ownerGroups.size() == 0)
      {
	showErrorMessage("Permissions Error",
			 "You don't have access to \nany owner groups.",
			 getErrorImage());
	return;
      }
    else if (ownerGroups.size() == 1)
      {
	if (!forcePopup) //Otherwise, just show the dialog.
	  {
	    defaultOwnerChosen = true;

	    Vector owners = new Vector();
	    for (int i = 0; i < ownerGroups.size(); i++)
	      {
		owners.addElement(((listHandle)ownerGroups.elementAt(i)).getObject());
	      }
	    try
	      {
		retVal = session.setDefaultOwner(owners);
	      }
	    catch (Exception rx)
	      {
		processExceptionRethrow(rx, "Could not set default owner");
	      }
	    return;
	  }
      }
    
    defaultOwnerDialog = new JDefaultOwnerDialog(this, ownerGroups);

    retVal = defaultOwnerDialog.chooseOwner();

    handleReturnVal(retVal);

    if ((retVal == null) || (retVal.didSucceed()))
      {
	defaultOwnerChosen =  true;
      }
    else
      {
	defaultOwnerChosen = false;
      }
  }

  /**
   * True if a default owner has already been chosen.
   */

  public boolean defaultOwnerChosen()
  {
    return defaultOwnerChosen;
  }

  /**
   * <p>Check for changes in the database before logging out.</p>
   *
   * <p>This checks to see if anything has been changed.  Basically, if edit panels are
   * open and have been changed in any way, then somethingChanged will be true and 
   * the user will be warned.  If edit panels are open but have not been changed, then
   * it will return true(it is ok to proceed).</p>
   */

  boolean OKToProceed()
  {
    if (wizardActive > 0)
      {
	if (debug)
	  {
	    System.out.println("gclient: wizard is active, not ok to logout.");
	  }

	return false;
      }

    if (getSomethingChanged())
      {
	StringDialog dialog = new StringDialog(this, 
					       "Warning: changes have been made",
					       "You have made changes in objects without commiting " +
					       "those changes.  If you continue, those changes will be lost",
					       "Discard Changes",
					       "Cancel");

	// if DialogShow is null, cancel was clicked So return will be
	// false if cancel was clicked

	return (dialog.DialogShow() != null);
      }
    else
      {
	return true;
      }
  }

  /**
   * <p>Updates the note panels in the open windows.</p>
   *
   * <p>The note panel doesn't have a listener on the TextArea, so when a transaction is
   * committed, this must be called on each notePanel in order to update the server.</p>
   *
   * <p>This basically does a field.setValue(notesArea.getValue()) on each notesPanel.</p>
   *
   * <p>THIS IS A PRETTY BIG HACK.</p>
   */

  void updateNotePanels()
  {
    Vector windows = wp.getEditables();

    for (int i = 0; i < windows.size(); i++)
      {
	if (debug)
	  {
	    System.out.println("Updating window number " + i);
	  }

	framePanel fp = (framePanel)windows.elementAt(i);

	if (fp == null)
	  {
	    if (debug)
	      {
		System.out.println("null frame panel in updateNotesPanels");
	      }
	  }
	else
	  {
	    notesPanel np = fp.getNotesPanel();

	    if (np == null)
	      {
		if (debug)
		  {
		    System.out.println("null notes panel in frame panel");
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("Calling update notes.");
		  }

		np.updateNotes();
	      }
	  }
      }
  }

  /** 
   * <p>Commits the currently open transaction on the server.  All
   * changes made by the user since the last openNewTransaction() call
   * will be integrated into the database on the Ganymede server.</p>
   *
   * <p>For various reasons, the server may reject the transaction as
   * incomplete.  Usually this will be a non-fatal error.. the user
   * will see a dialog telling him what else needs to be filled out in
   * order to commit the transaction.  In this case,
   * commitTransaction() will have had no effect and the user is free
   * to try again.</p>
   *
   * <p>If the transaction is committed successfully, the relevant
   * object nodes in the tree will be fixed up to reflect their state
   * after the transaction is committed.  commitTransaction() will
   * close all open editing windows, and will call openNewTransaction()
   * to prepare the server for further changes by the user.</p> 
   */

  public void commitTransaction()
  {
    ReturnVal retVal = null;
    boolean succeeded = false;

    /* -- */

    setWaitCursor();

    try
      {
	// We need to check to see if any notes panels need to
	// have their text flushed to the server.. 

	updateNotePanels();
	
	retVal = session.commitTransaction();

	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);
	  }

	succeeded = ((retVal == null) || retVal.didSucceed());

	// if we succeed, we clean up.  If we don't,
	// retVal.doNormalProcessing can be false, in which case the
	// serve aborted our transaction utterly.  If
	// retVal.doNormalProcessing is true, the user can do
	// something to make the transaction able to complete
	// successfully.  In this case, handleReturnVal() will have
	// displayed a dialog telling the user what needs to be done.

	if (succeeded)
	  {
	    setStatus("Transaction successfully committed.");
	    wp.closeEditables();
	
	    wp.refreshTableWindows();

	    openNewTransaction();

	    //
	    // This fixes all the icons in the tree
	    //

	    refreshTreeAfterCommit();

	    if (debug)
	      {
		System.out.println("Done committing");
	      }
	  }
	else if (!retVal.doNormalProcessing)
	  {
	    setStatus("Transaction could not successfully commit.");

	    // This is just like a cancel.  Something went wrong, and
	    // the server cancelled our transaction.  We don't need to
	    // call cancelTransaction ourselves.

	    showErrorMessage("Error: commit failed", 
			     "Could not commit your changes.",
			     getErrorImage());
	  }
      }
    catch (Exception ex)
      {
	processExceptionRethrow(ex, "Caught exception during commit");
      }
    finally
      {
	setNormalCursor();

	if (!succeeded && (retVal == null || !retVal.doNormalProcessing))
	  {
	    wp.closeEditables();
	    cleanUpAfterCancel();
	    openNewTransaction();
	  }
      }
  }

  /**
   * <p>Cancels the current transaction.  Any changes made by the user since
   * the last openNewTransaction() call will be forgotten as if they
   * never happened.  The client's tree display will be reverted to the
   * state it was when the transaction was started, and all open windows
   * will be closed.</p>
   */

  public synchronized void cancelTransaction()
  {
    ReturnVal retVal;

    /* -- */

    // close all the client windows.. this causes the windows to cancel
    // their loading activity

    wp.closeAll(true);

    try
      {
	retVal = session.abortTransaction();

	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);
	  }

	if (retVal == null)
	  {
	    setStatus("Transaction cancelled.");

	    if (debug)
	      {
		System.out.println("Cancel succeeded");
	      }
	  }
	else
	  {
	    if (retVal.didSucceed())
	      {
		setStatus("Transaction cancelled.");
		
		if (debug)
		  {
		    System.out.println("Cancel succeeded");
		  }
	      }
	    else
	      {
		setStatus("Error on server, transaction cancel failed.");

		if (debug)
		  {
		    System.out.println("Everytime I think I'm out, they pull me back in! " +
				       "Something went wrong with the cancel.");
		  }

		return;
	      }
	  }
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx, "Error while cancelling transaction");
      }

    cleanUpAfterCancel();

    openNewTransaction();
  }

  /**
   * <p>Cleans up the tree and gclient's caches.</p>
   *
   * <p>This method is precisely analagous in function to
   * {@link arlut.csd.ganymede.client.gclient#refreshTreeAfterCommit() refreshTreeAfterCommit()},
   * except for use after a cancel, when nodes marked as deleted are not removed from the tree,
   * and nodes marked as created are not kept.</p>
   */

  private synchronized void cleanUpAfterCancel()
  {
    ObjectHandle handle;
    Invid invid;
    InvidNode node;
    objectList list;
    CacheInfo info;

    /* -- */

    synchronized (deleteHash)
      {
	Enumeration dels = deleteHash.keys();
    
	while (dels.hasMoreElements())
	  {
	    invid = (Invid)dels.nextElement();
	    info = (CacheInfo)deleteHash.get(invid);
	
	    list = cachedLists.getList(info.getBaseID());	    
	
	    if (list != null)
	      {
		if (createHash.containsKey(invid))
		  {
		    if (debug)
		      {
			System.out.println("Can't fool me: you just created this object!");
		      }
		  }
		else
		  {
		    if (debug)
		      {
			System.out.println("This one is hashed, sticking it back in.");
		      }
		
		    handle = info.getOriginalObjectHandle();

		    if (handle != null)
		      {
			list.addObjectHandle(handle);
			node = (InvidNode)invidNodeHash.get(invid);
			
			if (node != null)
			  {
			    node.setHandle(handle);
			  }
		      }
		  }
	      }
	    
	    deleteHash.remove(invid);
	    setIconForNode(invid);
	  }
      }
    
    node = null;
    invid = null;
    list = null;
    info = null;

    // Next up is created list: remove all the added stuff.

    synchronized (createHash)
      {
	Enumeration created = createHash.keys();
    
	while (created.hasMoreElements())
	  {
	    invid = (Invid) created.nextElement();
	    info = (CacheInfo)createHash.get(invid);
	    
	    list = cachedLists.getList(info.getBaseID());
	    
	    if (list != null)
	      {
		if (debug)
		  {
		    System.out.println("This one is hashed, taking a created object out.");
		  }
	    
		list.removeInvid(invid);
	      }
	    
	    createHash.remove(invid);
	    
	    node = (InvidNode)invidNodeHash.get(invid);
	    
	    if (node != null)
	      {
		tree.deleteNode(node, false);
		invidNodeHash.remove(invid);
	      }
	  }
      }

    createdObjectsWithoutNodes.clear();
    
    // Now go through changed list and revert any names that may be needed

    Vector changedInvids = new Vector();

    synchronized (changedHash)
      {
	Enumeration changed = changedHash.keys();

	while (changed.hasMoreElements())
	  {
	    changedInvids.addElement(changed.nextElement());
	  }

	changedHash.clear();
      }

    refreshChangedObjectHandles(changedInvids, true);

    if (debug && createHash.isEmpty() && deleteHash.isEmpty())
      {
	System.out.println("Woo-woo the hashes are all empty");
      }

    tree.refresh(); // To catch all the icon changing.
  }

  /**
   * Initializes a new transaction on the server
   */

  private void openNewTransaction()
  {
    try
      {
	ReturnVal rv = session.openTransaction("Ganymede GUI Client");
	
	handleReturnVal(rv);
	if ((rv != null) && (!rv.didSucceed()))
	  {
	    showErrorMessage("Server Error",
			     "Could not open new transaction.",
			     getErrorImage());
	  }
	
	tree.refresh();
      }
    catch (Exception rx)
      {
	processExceptionRethrow(rx, "Could not open new transaction");
      }

    setSomethingChanged(false);
    cancel.setEnabled(false);
    commit.setEnabled(false);

    clearCaches();
  }

  /**
   * toggles the toolbar on and off
   */

  void toggleToolBar()
  {
    if (toolToggle == true) 
      {
	if (((BasicToolBarUI)toolBar.getUI()).isFloating()) 
	  {
	    ((BasicToolBarUI)toolBar.getUI()).setFloating(false, new Point(0,0));
	  }
	
	toolBar.setVisible(false);
	toolToggle = false;
      } 
    else if (toolToggle == false)
      { 
	toolBar.setVisible(true);
	toolToggle = true;
      }

    getContentPane().validate();
  }
  
  // ActionListener Methods

  /**
   * <p>Handles button and menu picks.  Includes logic for threading
   * out queries and message panels to avoid locking the Java GUI
   * thread.</p>
   */
  
  public void actionPerformed(java.awt.event.ActionEvent event)
  {
    Object source = event.getSource();
    String command = event.getActionCommand();

    /* -- */

    if (debug)
      {
	System.out.println("Action: " + command);
      }

    if (source == cancel)
      {
	if (debug)
	  {
	    System.out.println("cancel button clicked");
	  }
	
	cancelTransaction();
      }
    else if (source == commit)
      {
	if (debug)
	  {
	    System.out.println("commit button clicked");
	  }
	
	commitTransaction();
      }
    else if ((source == menubarQueryMI) || (command.equals("compose a query")))
      {
	postQuery(null);
      }
    else if (source == clearTreeMI)
      {
	clearTree();
      }
    else if (source == hideNonEditablesMI)
      {
	hideNonEditables = hideNonEditablesMI.getState();
	clearTree();
      }
    else if (source == logoutMI)
      {
	if (OKToProceed())
	  {
	    logout();
	  }
      }
    else if (source == toggleToolBarMI)
      {
	toggleToolBar();
      }
    else if (command.equals("change persona"))
      {
	changePersona(false);
      }
    else if (command.equals("create new object"))
      {
	createObjectDialog();
      }
    else if (command.equals("open object for editing"))
      {
	editObjectDialog();
      }
    else if (command.equals("open object for viewing"))
      {
	viewObjectDialog();
      }
    else if (command.equals("delete an object"))
      {
	deleteObjectDialog();
      }
    else if (command.equals("clone an object"))
      {
	cloneObjectDialog();
      }
    else if (command.equals("inactivate an object"))
      {
	inactivateObjectDialog();
      }
    else if (command.equals("Show me an Invid"))
      {
	openAnInvid();
      }
    else if (command.equals("Set Owner Filter"))
      {
	chooseFilter();
      }
    else if (command.equals("Set Default Owner"))
      {
	chooseDefaultOwner(true);
      }
    else if (command.equals("Help"))
      {
	showHelpWindow();
      }
    else if (command.equals("About Ganymede"))
      {
	Thread thread = new Thread(new Runnable() {
	  public void run() {
	    showAboutMessage();
	  }});
	thread.start();
      }
    else if (command.equals("Credits"))
      {
	Thread thread = new Thread(new Runnable() {
	  public void run() {
	    showCredits();
	  }});
	thread.start();
      }
    else if (command.equals("Message of the day"))
      {
	Thread thread = new Thread(new Runnable() {
	  public void run() {
	    showMOTD();
	  }});
	thread.start();
      }
    else
      {
	System.err.println("Unknown action event generated");
      }
  }

  /**
   * <p>Pop up the query box</p>
   */

  void postQuery(BaseDump base)
  {
    if (my_querybox == null)
      {
	my_querybox = new querybox(base, this, this, "Query Panel");
      }
    else if (my_querybox.isVisible())
      {
	return;
      }
    
    if (base != null)
      {
	my_querybox.selectBase(base);
      }

    my_querybox.myshow();
  }

  /**
   * This is a debugging hook, to allow the user to enter an invid in 
   * string form for direct viewing.
   */

  void openAnInvid()
  {
    DialogRsrc r = new DialogRsrc(this,
				  "Open an invid",
				  "This will open an invid by number.  This is for " +
				  "debugging purposes only.  Invid's have the format " +
				  "number:number, like 21:423");
    r.addString("Invid number:");
    StringDialog d = new StringDialog(r);
    
    Hashtable result = d.DialogShow();

    /* -- */

    if (result == null)
      {
	if (debug)
	  {
	    System.out.println("Ok, nevermind.");
	  }
	return;
      }

    String invidString = (String)result.get("Invid number:");

    if (invidString == null)
      {
	if (debug)
	  {
	    System.out.println("Ok, nevermind.");
	  }

	return;
      }

    viewObject(Invid.createInvid(invidString));
  }

  public void addTableWindow(Session session, Query query, DumpResult buffer)
  {
    wp.addTableWindow(session, query, buffer);
  }
  
  protected void processWindowEvent(WindowEvent e) 
  {
    if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
	if (debug)
	  {
	    System.out.println("Window closing");
	  }

	if (OKToProceed())
	  {
	    if (debug)
	      {
		System.out.println("It's ok to log out.");
	      }

	    logout();
	    super.processWindowEvent(e);
	  }
	else if (debug)
	  {
	    System.out.println("No log out!");
	  }
      }
    else
      {
	super.processWindowEvent(e);
      }
  }

  // Callbacks

  /**
   * <p>This method comprises the JsetValueCallback interface, and is how
   * some data-carrying components notify us when something changes.</p>
   *
   * @see arlut.csd.JDataComponent.JsetValueCallback
   * @see arlut.csd.JDataComponent.JValueObject
   */

  public boolean setValuePerformed(JValueObject o)
  {
    if (o instanceof JErrorValueObject)
      {
	showErrorMessage("Client Error",
			 (String)o.getValue(),
			 getErrorImage());
      }
    else
      {
	if (debug)
	  {
	    System.out.println("I don't know what to do with this setValuePerformed: " + o);
	  }

	return false;
      }
    return true;

  }

  // treeCallback methods

  /**
   * Called when a node is expanded, to allow the
   * user of the tree to dynamically load the information
   * at that time.
   *
   * @param node The node opened in the tree.
   *
   * @see arlut.csd.JTree.treeCanvas
   */

  public void treeNodeExpanded(treeNode node)
  {
    if (node instanceof BaseNode && !((BaseNode) node).isLoaded())
      {
	setStatus("Loading objects for base " + node.getText(), 0);
	setWaitCursor();

	try
	  {
	    refreshObjects((BaseNode)node, true);
	  }
	catch (Exception ex)
	  {
	    processExceptionRethrow(ex, "Remote exception loading objects for base " + node.getText());
	  }

	setStatus("Done loading objects for base " + node.getText());

	((BaseNode) node).markLoaded();
	setNormalCursor();
      }
  }

  /**
   * Called when a node is closed.
   *
   * @see arlut.csd.JTree.treeCanvas
   */

  public void treeNodeContracted(treeNode node)
  {
  }

  /**
   * Called when an item in the tree is unselected
   *
   * @param node The node selected in the tree.
   * @param someNodeSelected If true, this node is being unselected by the selection
   *                         of another node.
   *
   * @see arlut.csd.JTree.treeCanvas
   */

  public void treeNodeSelected(treeNode node)
  {
    selectedNode = node;
    validate();
  }

  public void treeNodeDoubleClicked(treeNode node)
  {
    if (node instanceof InvidNode)
      {
	viewObject(((InvidNode)node).getInvid());
      }
  }

  public void treeNodeUnSelected(treeNode node, boolean otherNode)
  {
  }

  /**
   *
   * Called when a popup menu item is selected
   * on a treeNode
   *
   * @param node The node selected in the tree.
   *
   * @see arlut.csd.JTree.treeCanvas
   */

  public void treeNodeMenuPerformed(treeNode node,
				    java.awt.event.ActionEvent event)
  {
    boolean treeMenuDebug = false;
    
    if (event.getActionCommand().equals("Create"))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("createMI");
	  }

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;

	    short id = baseN.getTypeID().shortValue();
	    
	    createObject(id);
	  }
	else
	  {
	    System.err.println("not a base node, can't create");
	  }
      }
    else if ((event.getActionCommand().equals("Report editable")) ||
	     (event.getActionCommand().equals("Report all")))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("viewMI/viewAllMI");
	  }

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;

	    Query listQuery = null;

	    if (event.getActionCommand().equals("Report editable"))
	      {
		listQuery = baseN.getEditableQuery();
	      }
	    else
	      {
		listQuery = baseN.getAllQuery();
	      }

	    // we still want to filter

	    listQuery.setFiltered(true);

	    // inner classes can only refer to final method variables,
	    // so we'll make some final references to keep our inner
	    // class happy.

	    final Query q = listQuery;
	    final gclient thisGclient = this;
	    final String tempText = node.getText();

	    Thread t = new Thread(new Runnable() {
	      public void run() {
		
		thisGclient.wp.addWaitWindow(this);
		DumpResult buffer = null;

		try
		  {
		    try
		      {
			buffer = thisGclient.getSession().dump(q);
		      }
		    catch (Exception ex)
		      {
			processExceptionRethrow(ex);
		      }
		    catch (Error ex)
		      {
			new JErrorDialog(thisGclient, 
					 "Could not complete query.. may have run out of memory.\n\n" +
					 ex.getMessage());
			throw ex;
		      }
		    
		    if (buffer == null)
		      {
			setStatus("No results from list operation on base " + tempText);
		      }
		    else
		      {
			setStatus("List returned from server on base " + tempText +
				  " - building table");
		    
			thisGclient.wp.addTableWindow(thisGclient.getSession(), q, buffer);
		      }
		  }
		finally
		  {
		    thisGclient.wp.removeWaitWindow(this);
		  }
	      }});

	    t.start();
	    
	    setStatus("Sending query for base " + node.getText() + " to server", 0);
	  }
	else
	  {
	    System.out.println("viewMI from a node other than a BaseNode");
	  }
      }
    else if (event.getActionCommand().equals("Query"))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("queryMI");
	  }

	if (node instanceof BaseNode)
	  {
	    setWaitCursor();
	    BaseDump base = (BaseDump)((BaseNode) node).getBase();
	    setNormalCursor();

	    postQuery(base);
	  }
      }
    else if (event.getActionCommand().equals("Show Non-Editables"))
      {
	BaseNode bn = (BaseNode) node;

	/* -- */

	if (treeMenuDebug)
	  {
	    System.out.println("show all objects");
	  }

	setWaitCursor();
	
	try
	  {
	    bn.showAll(true);
	    node.setMenu(((BaseNode)node).canCreate() ? pMenuAllCreatable : pMenuAll);

	    if (bn.isOpen())
	      {
		try
		  {
		    if (treeMenuDebug)
		      {
			System.out.println("Refreshing objects");
		      }

		    refreshObjects(bn, true);
		  }
		catch (Exception ex)
		  {
		    processExceptionRethrow(ex);
		  }
	      }
	  }
	finally
	  {
	    setNormalCursor();
	  }
      }
    else if (event.getActionCommand().equals("Hide Non-Editables"))
      {
	BaseNode bn = (BaseNode) node;

	/* -- */

	bn.showAll(false);
	bn.setMenu(((BaseNode)node).canCreate() ? pMenuEditableCreatable : pMenuEditable);

	if (bn.isOpen())
	  {
	    // this makes the ratchet operation in refreshObjects() faster
	    // in the common case where there are many more editables than
	    // non-editables.

	    tree.removeChildren(bn, false);
	    tree.expandNode(bn, false);
	    
	    try
	      {
		refreshObjects(bn, true);
	      }
	    catch (Exception ex)
	      {
		processExceptionRethrow(ex);
	      }
	  }
      }
    else if (event.getActionCommand().equals("View Object"))
      {
	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;

	    viewObject(invidN.getInvid(), invidN.getTypeText());
	  }
      }
    else if (event.getActionCommand().equals("Edit Object"))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("objEditMI");
	  }

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;

	    editObject(invidN.getInvid(), invidN.getTypeText());
	  }
      }
    else if (event.getActionCommand().equals("Clone Object"))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("objEditMI");
	  }

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;

	    cloneObject(invidN.getInvid());
	  }
      }
    else if (event.getActionCommand().equals("Delete Object"))
      {
	// Need to change the icon on the tree to an X or something to show that it is deleted

	if (treeMenuDebug)
	  {
	    System.out.println("Deleting object");
	  }

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;
	    Invid invid = invidN.getInvid();

	    deleteObject(invid, true);
	  }
      }
    else if(event.getActionCommand().equals("Inactivate Object"))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("objInactivateMI");
	  }

	if (node instanceof InvidNode)
	  {
	    inactivateObject(((InvidNode)node).getInvid());
	  }
      }
    else if (event.getActionCommand().equals("Reactivate Object"))
      {
	if (treeMenuDebug)
	  {
	    System.out.println("Reactivate item.");
	  }

	if (node instanceof InvidNode)
	  {
	    reactivateObject(((InvidNode)node).getInvid());
	  }
      }
    else
      {
	System.err.println("Unknown MI chosen");
      }
  }

  // Utilities

  /**
   * sort a vector of listHandles
   *
   * @param v Vector to be sorted
   * @return Vector of sorted listHandles(sorted by label)
   */

  public Vector sortListHandleVector(Vector v)
  {
    (new VecQuickSort(v, 
		      new Comparator() {
      public int compare(Object a, Object b) 
	{
	  listHandle aF, bF;
	  
	  aF = (listHandle) a;
	  bF = (listHandle) b;
	  int comp = 0;
	  
	  comp =  aF.toString().compareTo(bF.toString());
	  
	  if (comp < 0)
	    {
	      return -1;
	    }
	  else if (comp > 0)
	    { 
	      return 1;
	    } 
	  else
	    { 
	      return 0;
	    }
	}
    })).sort();
    
    return v;
  }

  /**
   * Sort a vector of Strings
   *
   * @return Vector of sorted Strings.
   */

  public Vector sortStringVector(Vector v)
  {
    new VecQuickSort(v, null).sort();
    
    return v;
  }

  /**
   * <p>This method does all the clean up required to let garbage
   * collection tear everything completely down.</p>
   *
   * <p>This method must be called from the Java GUI thread.</p>
   */

  public void cleanUp()
  {
    if (debug)
      {
	System.err.println("gclient.cleanUp()");
      }

    this.removeAll();

    client = null;

    session = null;
    _myglogin = null;
    dump = null;
    currentPersonaString = null;
    emptyBorder5 = null;
    emptyBorder10 = null;
    raisedBorder = null;
    loweredBorder = null;
    lineBorder = null;
    statusBorder = null;
    statusBorderRaised = null;

    if (changedHash != null)
      {
	changedHash.clear();
	changedHash = null;
      }

    if (deleteHash != null)
      {
	deleteHash.clear();
	deleteHash = null;
      }
    
    if (createHash != null)
      {
	createHash.clear();
	createHash = null;
      }

    if (createdObjectsWithoutNodes != null)
      {
	createdObjectsWithoutNodes.clear();
	createdObjectsWithoutNodes = null;
      }

    if (shortToBaseNodeHash != null)
      {
	shortToBaseNodeHash.clear();
	shortToBaseNodeHash = null;
      }

    if (invidNodeHash != null)
      {
	invidNodeHash.clear();
	invidNodeHash = null;
      }

    if (cachedLists != null)
      {
	cachedLists.clearCaches();
	cachedLists = null;
      }

    if (loader != null)
      {
	loader.cleanUp();
	loader = null;
      }

    help = null;
    motd = null;
    about = null;

    if (personae != null)
      {
	personae.setSize(0);
	personae = null;
      }

    if (ownerGroups != null)
      {
	ownerGroups.setSize(0);
	ownerGroups = null;
      }

    toolBar = null;

    if (filterDialog != null)
      {
	filterDialog.dispose();
	filterDialog = null;
      }

    if (personaDialog != null)
      {
	personaDialog.dispose();
	personaDialog = null;
      }

    if (defaultOwnerDialog != null)
      {
	defaultOwnerDialog.dispose();
	defaultOwnerDialog = null;
      }

    if (openDialog != null)
      {
	openDialog.dispose();
	openDialog = null;
      }

    if (createDialog != null)
      {
	createDialog.dispose();
	createDialog = null;
      }

    images = null;
    commit = null;
    cancel = null;

    if (statusPanel != null)
      {
	statusPanel.removeAll();
	statusPanel = null;
      }

    buildLabel = null;
    tree = null;
    selectedNode = null;

    errorImage = null;
    questionImage = null;
    search = null;
    queryIcon = null;
    cloneIcon = null;
    pencil = null;
    personaIcon = null;
    inactivateIcon = null;
    treepencil = null;
    trash = null;
    treetrash = null;
    creation = null;
    treecreation = null;
    newToolbarIcon = null;
    ganymede_logo = null;
    createDialogImage = null;

    idleIcon = null;
    buildIcon = null;
    buildIcon2 = null;
    
    wp.closeAll(true);
    wp = null;

    objectViewPM = null;
    objectReactivatePM = null;
    objectInactivatePM = null;
    objectRemovePM = null;

    pMenuAll = null;
    pMenuEditable= null;
    pMenuEditableCreatable = null;
    pMenuAllCreatable = null;

    menubar = null;

    logoutMI = null;
    clearTreeMI = null;
    filterQueryMI = null;
    defaultOwnerMI = null;
    showHelpMI = null;
    toggleToolBarMI = null;

    hideNonEditablesMI = null;

    changePersonaMI = null;
    editObjectMI = null;
    viewObjectMI = null;
    createObjectMI = null;
    deleteObjectMI = null;
    inactivateObjectMI = null;
    menubarQueryMI = null;

    my_username = null;

    if (actionMenu != null)
      {
	actionMenu.removeAll();
	actionMenu = null;
      }

    if (windowMenu != null)
      {
	windowMenu.removeAll();
	windowMenu = null;
      }

    if (fileMenu != null)
      {
	fileMenu.removeAll();
	fileMenu = null;
      }

    if (helpMenu != null)
      {
	helpMenu.removeAll();
	helpMenu = null;
      }

    if (PersonaMenu != null)
      {
	PersonaMenu.removeAll();
	PersonaMenu = null;
      }

    if (LandFMenu != null)
      {
	LandFMenu.removeAll();
	LandFMenu = null;
      }

    personaListener = null;

    if (my_querybox != null)
      {
	my_querybox.dispose();
	my_querybox = null;
      }

    if (statusThread != null)
      {
	try
	  {
	    statusThread.shutdown();
	  }
	catch (NullPointerException ex)
	  {
	  }

	statusThread = null;
      }

    if (securityThread != null)
      {
	try
	  {
	    securityThread.shutdown();
	  }
	catch (NullPointerException ex)
	  {
	  }

	securityThread = null;
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 PersonaListener

------------------------------------------------------------------------------*/

/**
 * Listener class to handle interaction with the client's persona selection
 * dialog.
 */

class PersonaListener implements ActionListener {

  Session session;

  DialogRsrc
    resource = null;

  gclient
    gc;

  boolean debug = false;

  boolean
    listen = true;

  // ---

  PersonaListener(Session session, gclient parent)
  {
    this.session = session;
    this.gc = parent;
  }

  public void actionPerformed(ActionEvent event)
  {
    if (debug) { System.out.println("personaListener: action Performed!"); }
    
    // Check to see if we need to commit the transaction first.
    
    String newPersona = null;
    
    if (event.getSource() instanceof JRadioButton)
      {
	if (debug) { System.out.println("From radiobutton"); }
	
	newPersona = event.getActionCommand();
	gc.getPersonaDialog().updatePassField(newPersona);

	if (debug) { System.out.println("radiobutton says: " + newPersona); }
      }
    else if (event.getSource() instanceof JButton)
      {    
	newPersona = gc.getPersonaDialog().getNewPersona();

	if (!gc.getPersonaDialog().requirePassword && newPersona.equals(gc.currentPersonaString))
	  {
	    if (debug) {gc.showErrorMessage("You are already in that persona."); }
	    return;
	  }

	if (gc.getPersonaDialog().debug)
	  {
	    System.err.println("personaListener processing jbutton");
	  }

	// Deal with trying to change w/ uncommitted transactions
	if (gc.getSomethingChanged() && !gc.getPersonaDialog().requirePassword)
	  {
	    if (gc.getPersonaDialog().debug)
	      {
		System.err.println("personaListener attempting to verify transaction commit/cancel");
	      }

	    // need to ask: commit, cancel, abort?
	    StringDialog d = new StringDialog(gc,
					      "Changing personas",
					      "Before changing personas, the transaction must " +
					      "be closed.  Would you like to commit your changes?",
					      "Commit",
					      "Cancel");
	    Hashtable result = d.DialogShow();
	    
	    if (result == null)
	      {
		gc.setStatus("Persona change cancelled");
		return;
	      }
	    else
	      {
		gc.setStatus("Committing transaction.");
		gc.commitTransaction();
	      }
	  }
	
	// Now change the persona
	
	String password = null;
	
	// All admin level personas have a : in them.  Only admin level
	// personas need passwords, unless we are forcing a password
	
	if (gc.getPersonaDialog().requirePassword || newPersona.indexOf(":") > 0)
	  {
	    password = gc.getPersonaDialog().getPasswordField();
	  }
	
	if (gc.getPersonaDialog().debug)
	  {
	    System.err.println("personaListener attempting to set the persona");
	  }

	if (!setPersona(newPersona, password))
	  {
	    if (gc.getPersonaDialog().requirePassword)
	      {
		if (gc.getPersonaDialog().debug)
		  {
		    System.err.println("personaListener requirePassword=true, failed to setPersona");
		  }

		return;
		//		gc.showErrorMessage("Wrong password"); 
	      }
	    else
	      {
		gc.showErrorMessage("Error: could not change persona", 
				    "Perhaps the password was wrong.", 
				    gc.getErrorImage());
	      }
	  }
	else
	  {
	    if (gc.getPersonaDialog().debug)
	      {
		System.err.println("personaListener succeeded setPersona");
	      }

	    gc.getPersonaDialog().changedOK = true;
	    gc.getPersonaDialog().setHidden(true);

	    if (gc.getPersonaDialog().debug)
	      {
		System.err.println("personaListener called setHidden");
	      }
	  }
      }
    else
      {
	System.out.println("Persona Listener doesn't understand that action.");
      }
  }

  public synchronized boolean setPersona(String newPersona, String password)
  {
    boolean personaChangeSuccessful = false;	

    if (gc.getPersonaDialog().debug)
      {
	System.err.println("personaListener setPersona()");
      }

    try
      {
	if (gc.getPersonaDialog().debug)
	  {
	    System.err.println("personaListener setPersona() calling server");
	  }

	personaChangeSuccessful = session.selectPersona(newPersona, password);

	// when we change personas, we lose our filter.  Clear the
	// reference to our filterDialog so that we will recreate it
	// from scratch if we need to.

	gc.filterDialog = null;

	if (gc.getPersonaDialog().debug)
	  {
	    System.err.println("personaListener setPersona() called server");
	  }
	
	if (personaChangeSuccessful)
	  {
	    if (gc.getPersonaDialog().debug)
	      {
		System.err.println("personaListener setPersona() succeeded");
	      }

	    gc.setWaitCursor();
	    gc.setStatus("Changing persona.");
		
	    // List of creatable object types might have changed.
		
	    gc.createDialog = null;
	    gc.setTitle("Ganymede Client: " + newPersona + " logged in.");
		
	    gc.ownerGroups = null;
	    gc.clearCaches();
	    gc.loader.clear();  // This reloads the hashes on a new background thread
	    gc.cancelTransaction();
	    gc.buildTree();
	    gc.currentPersonaString = newPersona;
	    gc.defaultOwnerChosen = false; // force a new default owner to be chosen
	    gc.setNormalCursor();
		
	    gc.setStatus("Successfully changed persona to " + newPersona);

	    return true;
	  }
	else
	  {
	    if (gc.getPersonaDialog().debug)
	      {
		System.err.println("personaListener setPersona() failed");
	      }

	    return false;
	  }
      }
    catch (Exception rx)
      {
	gc.processException(rx, "Could not set persona to " + newPersona + ": ");
	return false;
      }
  }

  public void softTimeOutHandler()
  {
    setPersona(gc.my_username, null);
    gc.changePersona(true);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       CacheInfo

------------------------------------------------------------------------------*/

/**
 * Client-side cache object, used by the
 * {@link arlut.csd.ganymede.client.gclient gclient} class to track object status for
 * nodes in the client tree display.
 */

class CacheInfo {

  private String
    originalLabel,
    currentLabel;

  private Short
    baseID;

  private ObjectHandle
    originalHandle = null,
    handle;

  private final boolean debug = false;

  /* -- */

  public CacheInfo(Short baseID, String originalLabel, String currentLabel)
  {
    this(baseID, originalLabel, currentLabel, null);
  }

  public CacheInfo(Short baseID, String originalLabel, String currentLabel, ObjectHandle handle)
  {
    this(baseID, originalLabel, currentLabel, handle, null);

    if (handle != null)
      {
	try
	  {
	    originalHandle = (ObjectHandle) handle.clone();

	    if (debug) 
	      {
		System.out.println("a cloned handle.");
	      }
	  }
	catch (Exception x)
	  {
	    originalHandle = null;

	    if (debug)
	      {
		System.out.println("Clone is not supported: " + x);
	      }
	  }
      }
    else
      {
	originalHandle = null;

	if (debug)
	  {
	    System.out.println("a null handle.");
	  }
      }
  }

  public CacheInfo(Short baseID, String originalLabel, 
		   String currentLabel, ObjectHandle handle, ObjectHandle originalHandle)
  {
    this.baseID = baseID;
    this.originalLabel = originalLabel;
    this.currentLabel = currentLabel;
    this.handle = handle;
    this.originalHandle = originalHandle;
  }

  public void setOriginalLabel(String label)
  {
    originalLabel = label;
  }

  public void changeLabel(String newLabel)
  {
    currentLabel = newLabel;
  }
    
  public Short getBaseID()
  {
    return baseID;
  }

  public String getOriginalLabel()
  {
    return originalLabel;
  }

  public String getCurrentLabel()
  {
    return currentLabel;
  }

  public ObjectHandle getObjectHandle()
  {
    return handle;
  }

  public ObjectHandle getOriginalObjectHandle()
  {
    return originalHandle;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                               StatusClearThread

------------------------------------------------------------------------------*/

/**
 * Background thread designed to clear the status label in 
 * {@link arlut.csd.ganymede.client.gclient gclient}
 * some seconds after the setClock() method is called.
 */

class StatusClearThread extends Thread {

  final static boolean debug = false;

  boolean done = false;
  boolean resetClock = false;

  JTextField statusLabel;

  int sleepSecs = 0;

  /* -- */

  public StatusClearThread(JTextField statusLabel)
  {
    this.statusLabel = statusLabel;
  }

  public synchronized void run()
  {
    while (!done)
      {
	if (debug)
	  {
	    System.err.println("StatusClearThread.run(): entering loop");
	  }

	resetClock = false;

	try
	  {
	    if (sleepSecs > 0)
	      {
		if (debug)
		  {
		    System.err.println("StatusClearThread.run(): waiting " + sleepSecs + " seconds");
		  }

		wait(sleepSecs * 1000);
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("StatusClearThread.run(): waiting indefinitely");
		  }

		wait();
	      }
	  }
	catch (InterruptedException ex)
	  {
	  }

	if (!resetClock && !done)
	  {
	    // this has to be invokeLater or else we'll risk getting
	    // deadlocked.

	    if (debug)
	      {
		System.err.println("StatusClearThread.run(): invoking label clear");
	      }

	    SwingUtilities.invokeLater(new Runnable() {
	      public void run() {
		statusLabel.setText("");
		statusLabel.paintImmediately(statusLabel.getVisibleRect());
	      }
	    });

	    sleepSecs = 0;
	  }
      }
  }

  /**
   * <p>This method resets the clock in the StatusClearThread, such that
   * the status label will be cleared in countDown seconds, unless
   * another setClock follows on closely enough to interrupt the
   * countdown, effectively.</p>
   *
   * @param countDown seconds to wait before clearing the status field.  If
   * countDown is zero or negative, the timer will suspend until a later
   * call to setClock sets a positive countdown.
   */

  public synchronized void setClock(int countDown)
  {
    if (debug)
      {
	System.err.println("StatusClearThread.setClock(" + countDown + ")");
      }

    resetClock = true;
    sleepSecs = countDown;
    notifyAll();

    if (debug)
      {
	System.err.println("StatusClearThread.setClock(" + countDown + ") - done");
      }
  }

  /**
   * <p>This method causes the run() method to gracefully terminate
   * without taking any further action.</p>
   */

  public synchronized void shutdown()
  {
    this.done = true;
    notifyAll();
  }
}


/*------------------------------------------------------------------------------
                                                                           class
                                                           SecurityLaunderThread

------------------------------------------------------------------------------*/

/**
 * Background client thread designed to launder build status messages
 * from the server on a non-RMI thread.  We do this so that RMI calls
 * from the server are granted permission to put events on the GUI
 * thread for apropriately synchronized icon setitng.  Set up and torn
 * down by the {@link arlut.csd.ganymede.client.gclient gclient}
 * class.
 */

class SecurityLaunderThread extends Thread {

  gclient client;
  boolean done = false;
  boolean messageSet = false;
  int buildPhase = -1;		// unknown

  /* -- */

  public SecurityLaunderThread(gclient client)
  {
    this.client = client;

    // assume we were constructed on the GUI thread by the main
    // gclient constructor

    switch (client.getBuildPhase())
      {
      case 0:
	client.buildLabel.setIcon(client.idleIcon);
	break;
	
      case 1:
	client.buildLabel.setIcon(client.buildIcon);
	break;
	
      case 2:
	client.buildLabel.setIcon(client.buildIcon2);
	break;
	
      default:
	client.buildLabel.setIcon(client.buildUnknownIcon);
      }
    
    client.buildLabel.validate();
  }

  public synchronized void run()
  {
    while (!done)
      {
	try
	  {
	    wait();
	  }
	catch (InterruptedException ex)
	  {
	  }

	if (messageSet)
	  {
	    SwingUtilities.invokeLater(new Runnable() {
	      public void run() {

		switch (buildPhase)
		  {
		  case 0:
		    client.buildLabel.setIcon(client.idleIcon);
		    break;
		    
		  case 1:
		    client.buildLabel.setIcon(client.buildIcon);
		    break;

		  case 2:
		    client.buildLabel.setIcon(client.buildIcon2);
		    break;

		  default:
		    client.buildLabel.setIcon(client.buildUnknownIcon);
		  }
		  
		client.buildLabel.validate();
	      }
	    });	

	    messageSet = false;
	  }
      }

    // done!

    client = null;
  }

  /**
   * <p>This method is called to trigger a build status icon update.</p>
   * Called by {@link arlut.csd.ganymede.client.gclient#setBuildStatus(java.lang.String) gclient.setBuildStatus()}.
   */

  public synchronized void setBuildStatus(int phase)
  {
    this.messageSet = true;
    this.buildPhase = phase;

    this.notifyAll();		// wakey-wakey!
  }

  /**
   * <p>This method causes the run() method to gracefully terminate
   * without taking any further action.</p>
   */

  public synchronized void shutdown()
  {
    this.done = true;
    notifyAll();
  }
}
