/*
   gclient.java

   Ganymede client main module

   Created: 24 Feb 1997
   Version: $Revision: 1.57 $ %D%
   Module By: Mike Mulvaney, Jonathan Abbey, and Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

import jdj.*;

import arlut.csd.JDialog.*;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.JDataComponent.*;
import arlut.csd.Util.*;
import arlut.csd.JTree.*;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         gclient

------------------------------------------------------------------------------*/

public class gclient extends JFrame implements treeCallback,ActionListener {

  // we're only going to have one gclient at a time per running client

  public static gclient client;

  // ---
  
  // Image numbers
  final int NUM_IMAGE = 16;
  
  final int OPEN_BASE = 0;
  final int CLOSED_BASE = 1;

  final int OPEN_FIELD = 2;
  final int OPEN_FIELD_DELETE = 3;
  final int OPEN_FIELD_CREATE = 4;
  final int OPEN_FIELD_CHANGED = 5;
  final int OPEN_FIELD_REMOVESET = 6;
  final int OPEN_FIELD_EXPIRESET = 7;
  final int CLOSED_FIELD = 8;
  final int CLOSED_FIELD_DELETE = 9;
  final int CLOSED_FIELD_CREATE = 10;
  final int CLOSED_FIELD_CHANGED = 11;
  final int CLOSED_FIELD_REMOVESET = 12;
  final int CLOSED_FIELD_EXPIRESET = 13;


  final int OPEN_CAT = 14;
  final int CLOSED_CAT = 15;

  final boolean debug = true;

  Session session;
  glogin _myglogin;

  long lastClick = 0;

  // set up a bunch of borders

  public EmptyBorder
    emptyBorder5 = (EmptyBorder)BorderFactory.createEmptyBorder(5,5,5,5),
    emptyBorder10 = (EmptyBorder)BorderFactory.createEmptyBorder(10,10,10,10);

  public BevelBorder
    raisedBorder = new BevelBorder(BevelBorder.RAISED),
    loweredBorder = new BevelBorder(BevelBorder.LOWERED);
      
  public LineBorder
    lineBorder = new LineBorder(Color.black);

  public CompoundBorder
    statusBorder = new CompoundBorder(loweredBorder, emptyBorder5),
    statusBorderRaised = new CompoundBorder(raisedBorder, emptyBorder5);

  private int
    containerPanelCount = 0;

  //
  // Yum, caches
  //

  private Vector
    containerPanels = new Vector(),
    baseList;            // List of base types.  Vector of Bases.

  private Hashtable
    baseNames = null,                // used to map Base -> Base.getName(String)
    baseHash = null,	             // used to reduce the time required to get listings
                                     // of bases and fields.. keys are Bases, values
		      	             // are vectors of fields
    baseMap = null,                  // Hash of Short to Base
    changedHash = new Hashtable(),   // Hash of objects that might have changed
    deleteHash = new Hashtable(),    // Hash of objects waiting to be deleted
    createHash = new Hashtable(),    // Hash of objects waiting to be created
    inactivateHash = new Hashtable(),
    expireHash = new Hashtable(),
    removeHash = new Hashtable(),
                                     // Create and Delete are pending on the Commit button. 
    baseToShort = null;              // Map of Base to Short
   
  protected Hashtable
    shortToBaseNodeHash = new Hashtable(),
    invidNodeHash = new Hashtable(),
    templateHash;

  // our main cache, keeps information on all objects we've had
  // references returned to us via QueryResult

  protected objectCache 
    cachedLists = new objectCache();

  // 
  //  Background processing thread
  //

  Loader 
    loader;      // Use this to do start up stuff in a thread
  
  //
  // Status tracking
  //

  private boolean
    showToolbar = false,       // Show the toolbar
    somethingChanged = false;  // This will be set to true if the user changes anything
  
  helpPanel
    help = null;

  protected Vector
    filter = new Vector();    // List of owner groups to show, these are listHandles

  Vector
    personae,
    ownerGroups = null;  // Vector of owner groups

  // Dialog and GUI objects

  JComboBox
    personaCombo = null;  // ComboBox showing current persona on the toolbar

  JFilterDialog
    filterDialog = null;

  JDefaultOwnerDialog
    defaultOwnerDialog = null;

  openObjectDialog
    openDialog;

  Image images[];

  JButton 
    commit,
    cancel;
  
  JTextField
    statusLabel;

  JSplitPane
    sPane;

  treeControl tree;

  // The top lines

  JPanel
    leftP,
    leftTop,
    rightTop,
    mainPanel;   //Everything is in this, so it is double buffered

  Image
    search,
    pencil,
    trash,
    creation;

  public JLabel
    leftL,
    rightL,
    timerLabel;

  //
  // Another background thread, to maintain a display of
  // time connected
  //

  connectedTimer
    timer;

  windowPanel
    wp;

  //
  // Menu resources
  //

  treeMenu 
    objectReactivatePM,
    objectInactivatePM,
    objectRemovePM;

  MenuItem
    objViewMI,
    objEditMI,
    objCloneMI,
    objInactivateMI,
    objDeleteMI,
    objReactivateMI;

  treeMenu 
    pMenu = new treeMenu();
  
  MenuItem 
    createMI = null,
    viewMI = null,
    viewAllMI = null,
    queryMI = null;

  JMenuBar 
    menubar;

  JMenuItem 
    logoutMI,
    removeAllMI,
    rebuildTreeMI,
    filterQueryMI,
    defaultOwnerMI,
    showHelpMI;

  private boolean
    defaultOwnerChosen = false;

  JMenuItem
    editObjectMI,
    viewObjectMI,
    cloneObjectMI,
    deleteObjectMI,
    inactivateObjectMI,
    menubarQueryMI = null;

  JCheckBoxMenuItem
    javaLFMI,
    motifMI,
    win95MI;

  String
    my_username;

  JMenu 
    actionMenu,
    windowMenu,
    fileMenu,
    helpMenu,
    LandFMenu,
    PersonaMenu = null;

  PersonaListener
    personaListener = null;

  WindowBar
    windowBar;

  /* -- */

  /**
   *
   * This is the main constructor for the gclient class.. it handles the
   * interactions between the user and the server once the user has
   * logged in.
   *
   */

  public gclient(Session s, glogin g) 
  {
    super("Ganymede Client: "+g.my_client.getName()+" logged in");

    client = this;

    System.out.println("Shortcut key mask: " + KeyEvent.getKeyText(Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    System.out.println("Starting gclient");


    enableEvents(AWTEvent.WINDOW_EVENT_MASK);

    if (s == null)
      {
	throw new IllegalArgumentException("Ganymede Error: Parameter for Session s is null");;
      }

    session = s;
    _myglogin = g;
    my_username = g.getUserName();

    mainPanel = new JPanel(true);
    mainPanel.setLayout(new BorderLayout());

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add("Center", mainPanel);

    templateHash = new Hashtable();

    if (debug)
      {
	System.out.println("Creating menu bar");
      }

    // Make the menu bar

    menubar = new JMenuBar();

    //menubar.setBorderPainted(true);
    
    // File menu

    fileMenu = new JMenu("File");
    logoutMI = new JMenuItem("Logout", new MenuShortcut(KeyEvent.VK_L));
    logoutMI.addActionListener(this);

    removeAllMI = new JMenuItem("Remove All Windows");
    removeAllMI.addActionListener(this);
    rebuildTreeMI = new JMenuItem("Rebuild Tree", new MenuShortcut(KeyEvent.VK_R));
    rebuildTreeMI.addActionListener(this);

    filterQueryMI = new JMenuItem("Filter Query");
    filterQueryMI.addActionListener(this);
    defaultOwnerMI = new JMenuItem("Set Default Owner");
    defaultOwnerMI.addActionListener(this);

    fileMenu.add(rebuildTreeMI);
    fileMenu.add(removeAllMI);
    fileMenu.add(filterQueryMI);
    fileMenu.add(defaultOwnerMI);
    fileMenu.addSeparator();
    fileMenu.add(logoutMI);

    // Action menu

    actionMenu = new JMenu("Actions");

    editObjectMI = new JMenuItem("Edit Object", new MenuShortcut(KeyEvent.VK_E));
    editObjectMI.setActionCommand("open object for editing");
    editObjectMI.addActionListener(this);

    viewObjectMI = new JMenuItem("View Object", new MenuShortcut(KeyEvent.VK_V));
    viewObjectMI.setActionCommand("open object for viewing");
    viewObjectMI.addActionListener(this);
    
    cloneObjectMI = new JMenuItem("Clone Object", new MenuShortcut(KeyEvent.VK_C));
    cloneObjectMI.setActionCommand("choose an object for cloning");
    cloneObjectMI.addActionListener(this);

    deleteObjectMI = new JMenuItem("Delete Object", new MenuShortcut(KeyEvent.VK_D));
    deleteObjectMI.setActionCommand("delete an object");
    deleteObjectMI.addActionListener(this);

    inactivateObjectMI = new JMenuItem("Inactivate Object", new MenuShortcut(KeyEvent.VK_I));
    inactivateObjectMI.setActionCommand("inactivate an object");
    inactivateObjectMI.addActionListener(this);

    menubarQueryMI = new JMenuItem("Query", new MenuShortcut(KeyEvent.VK_Q));
    menubarQueryMI.addActionListener(this);

    actionMenu.add(menubarQueryMI);
    actionMenu.addSeparator();
    actionMenu.add(editObjectMI);
    actionMenu.add(cloneObjectMI);
    actionMenu.add(viewObjectMI);
    actionMenu.add(deleteObjectMI);
    actionMenu.add(inactivateObjectMI);

    // windowMenu

    windowMenu = new JMenu("Windows");

    // Look and Feel menu

    LandFMenu = new arlut.csd.JDataComponent.LAFMenu(this);

    // Personae menu

    boolean personasExist = false;

    try
      {
	personae = session.getPersonae();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not load personas: " + rx);
      }

    personaListener = new PersonaListener(session, this);

    if (personae != null)
      {
	PersonaMenu = new JMenu("Persona");
	ButtonGroup personaGroup = new ButtonGroup();
	
	for (int i = 0; i < personae.size(); i++)
	  {
	    String p = (String)personae.elementAt(i);
	    JCheckBoxMenuItem mi = new JCheckBoxMenuItem(p, false);

	    if (p.equals(my_username))
	      {
		mi.setState(true);
	      }

	    personaGroup.add(mi);
	    mi.addActionListener(personaListener);
	    PersonaMenu.add(mi);
	  }

	personasExist = true;
      }

    // Help menu

    helpMenu = new JMenu("Help");
    showHelpMI = new JMenuItem("Help", new MenuShortcut(KeyEvent.VK_H));
    showHelpMI.addActionListener(this);
    helpMenu.add(showHelpMI);

    menubar.add(fileMenu);
    menubar.add(LandFMenu);
    menubar.add(actionMenu);
    menubar.add(windowMenu);
    menubar.add(helpMenu);
    menubar.setHelpMenu(helpMenu);

    if (personasExist)
      {
	menubar.add(PersonaMenu);
      }
    
    setJMenuBar(menubar);

    // Create menus for the tree

    createMI = new MenuItem("Create");
    viewMI = new MenuItem("List editable");
    viewAllMI = new MenuItem("List all");
    queryMI = new MenuItem("Query");

    pMenu.add(viewMI);
    pMenu.add(viewAllMI);
    pMenu.add(createMI);
    pMenu.add(queryMI);

    if (debug)
      {
	System.out.println("Loading images for tree");
      }

    Image openFolder = PackageResources.getImageResource(this, "openfolder.gif", getClass());
    Image closedFolder = PackageResources.getImageResource(this, "folder.gif", getClass());
    Image list = PackageResources.getImageResource(this, "list.gif", getClass());
    Image redOpenFolder = PackageResources.getImageResource(this, "openfolder-red.gif", getClass());
    Image redClosedFolder = PackageResources.getImageResource(this, "folder-red.gif", getClass());
    
    search = PackageResources.getImageResource(this, "srchfol2.gif", getClass());
    trash = PackageResources.getImageResource(this, "trash.gif", getClass());
    creation = PackageResources.getImageResource(this, "creation.gif", getClass());
    pencil = PackageResources.getImageResource(this, "pencil.gif", getClass());

    Image remove = PackageResources.getImageResource(this, "remove.gif", getClass());
    Image expire = PackageResources.getImageResource(this, "expire.gif", getClass());

    images = new Image[NUM_IMAGE];
    images[OPEN_BASE] =  openFolder;
    images[CLOSED_BASE ] = closedFolder;
    
    images[OPEN_FIELD] = list;
    images[OPEN_FIELD_DELETE] = trash;
    images[OPEN_FIELD_CREATE] = creation;
    images[OPEN_FIELD_CHANGED] = pencil;
    images[OPEN_FIELD_EXPIRESET] = expire;
    images[OPEN_FIELD_REMOVESET] = remove;
    images[CLOSED_FIELD] = list;
    images[CLOSED_FIELD_DELETE] = trash;
    images[CLOSED_FIELD_CREATE] = creation;
    images[CLOSED_FIELD_CHANGED] = pencil;
    images[CLOSED_FIELD_EXPIRESET] = expire;
    images[CLOSED_FIELD_REMOVESET] = remove;
    
    images[OPEN_CAT] = redOpenFolder;
    images[CLOSED_CAT] = redClosedFolder;

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
	
	leftL = new JLabel("Objects");
	leftTop.setLayout(new BorderLayout());
	//leftTop.setBackground(ClientColor.menu);
	//leftTop.setForeground(ClientColor.menuText);
	leftTop.add("Center", leftL);
	
	leftP.add("North", leftTop);
      }

    if (debug)
      {
	System.out.println("Creating pop up menus");
      }

    objViewMI = new MenuItem("View Object");
    objEditMI = new MenuItem("Edit Object");
    objCloneMI = new MenuItem("Clone Object");

    objectRemovePM = new treeMenu();
    objDeleteMI = new MenuItem("Delete Object");
    objectRemovePM.add(objViewMI);
    objectRemovePM.add(objEditMI);
    objectRemovePM.add(objCloneMI);
    objectRemovePM.add(objDeleteMI);

    objectInactivatePM = new treeMenu();
    objInactivateMI = new MenuItem("Inactivate Object");
    objectInactivatePM.add(new MenuItem("View Object"));
    objectInactivatePM.add(new MenuItem("Edit Object"));
    objectInactivatePM.add(new MenuItem("Clone Object"));
    objectInactivatePM.add(objInactivateMI);

    objectReactivatePM = new treeMenu();
    objReactivateMI = new MenuItem("Reactivate Object");
    objectReactivatePM.add(new MenuItem("View Object"));
    objectReactivatePM.add(new MenuItem("Edit Object"));;
    objectReactivatePM.add(new MenuItem("Clone Object"));
    objectReactivatePM.add(objReactivateMI);

    try
      {
	buildTree();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception in buildTree: " + ex);
      }

    // The right panel which will contain the windowPanel

    JPanel rightP = new JPanel(true);
    rightP.setBackground(ClientColor.background);
    rightP.setLayout(new BorderLayout());

    wp = new windowPanel(this, windowMenu);

    rightP.add("Center", wp);
    rightTop = new JPanel(false);
    rightTop.setBorder(statusBorderRaised);
    rightTop.setLayout(new BorderLayout());
    
    if (showToolbar)
      {
	getContentPane().add("North", createToolbar());
      }
    else
      {
	rightL = new JLabel("Open objects");
	
	rightTop.add("West", rightL);
	//timerLabel = new JLabel("                                       ", JLabel.RIGHT);
	timerLabel = new JLabel("00:00:00", JLabel.RIGHT);
	timer = new connectedTimer(timerLabel, 5000, true);
	timerLabel.setMinimumSize(new Dimension(200, timerLabel.getPreferredSize().height));
	rightTop.add("East", timerLabel);

	rightP.add("North", rightTop);	
      }

    commit = new JButton("Commit");
    commit.setOpaque(true);
    commit.setBackground(Color.lightGray);
    commit.setForeground(Color.black);
    commit.setToolTipText("Click this to commit all changes to database");
    commit.addActionListener(this);

    cancel = new JButton("Cancel");
    cancel.setOpaque(true);
    cancel.setBackground(Color.lightGray);
    cancel.setForeground(Color.black);
    cancel.setToolTipText("Click this to cancel all changes");
    cancel.addActionListener(this);

    // Button bar at bottom, includes commit/cancel panel and taskbar

    JPanel bottomButtonP = new JPanel(false);

    if (showToolbar)
      {
	rightTop.add("East", bottomButtonP);
      }
    else
      {
	//rightP.add(bottomButtonP,"South");
      }

    bottomButtonP.add(commit);
    bottomButtonP.add(cancel);

    // Create the pane splitter

    sPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftP, rightP);
   
    mainPanel.add("Center",sPane);

    // Create the bottomBar, for the bottom of the window

    JPanel bottomBar = new JPanel(false);
    bottomBar.setLayout(new BorderLayout());

    statusLabel = new JTextField();
    statusLabel.setEditable(false);
    statusLabel.setBorder(statusBorder);

    JLabel l = new JLabel("Status: ");
    JPanel lP = new JPanel(new BorderLayout());
    lP.setBorder(statusBorder);
    lP.add("Center", l);

    bottomBar.add("West", lP);
    bottomBar.add("Center", statusLabel);
    bottomBar.add("East", bottomButtonP);
    mainPanel.add("South", bottomBar);

    setStatus("Starting up");

    try
      {
	session.openTransaction("gclient");
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not open transaction: " + rx);
      }

    timer.start();

    loader = new Loader(session);
    loader.start();

    pack();
    setSize(800, 600);
    show();
    
    setStatus("Ready.");
  }

  /**
   * Returns the templateHash.
   *
   * Template Hash is a hash of object type ID's (Short) -> Vector of FieldTemplates
   * Use this instead of templateHash directly, because you never know where we will
   * get it from(evil grin).
   */

  public Hashtable getTemplateHash()
  {
    return templateHash;
  }
  
  /**
   * Returns a vector of FieldTemplates.
   *
   * The id number is the base id.
   */

  public Vector getTemplateVector(Short id)
  {
    Vector result = null;
    Hashtable th = getTemplateHash();

    if (th.containsKey(id))
      {
	if (debug)
	  {
	    System.out.println("Found the template, using cache.");
	  }
	result = (Vector)th.get(id);

      }
    else
      {
	try
	  {
	    if (debug)
	      {
		System.out.println("template not found, downloading and caching: " + id);
	      }

	    result = session.getFieldTemplateVector(id.shortValue());
	    th.put(id, result);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get field templates: " + rx);
	  }
      }
    
    return result;
  }

  public void clearCaches()
  {
    if (debug)
      {
        System.out.println("Clearing caches");
      }

    cachedLists.clearCaches();
  }

  public void update(Graphics g)
  {
    paint(g);
  }

  /** 
   * Get the session
   */

  public Session getSession()
  {
    return session;
  }
  
  /**
   * Returns the base hash.
   *
   * Checks to see if the baseHash was loaded, and if not, it loads it.
   *
   */

  public final Hashtable getBaseHash()
  {
    if (baseHash == null)
      {
	baseHash = loader.getBaseHash();
      }

    return baseHash;
  }

  public final Hashtable getBaseNames()
  {
    if (baseNames == null)
      {
	baseNames = loader.getBaseNames();
      }

    return baseNames;
  }

  public final Vector getBaseList()
  {
    if (baseList == null)
      {
	baseList = loader.getBaseList();
      }

    return baseList;
  }

  public Hashtable getBaseMap()
  {
    if (baseMap == null)
      {
	baseMap = loader.getBaseMap();
      }

    return baseMap;
  }

  public Hashtable getBaseToShort()
  {
    if (baseToShort == null)
      {
	baseToShort = loader.getBaseToShort();
      }
    
    return baseToShort;
  }

  /**
   * Change the text in the status bar
   *
   * @param status The text to display
   */

  public final void setStatus(String status)
  {
    if (debug)
      {
	System.out.println("Setting status: " + status);
      }

    statusLabel.setText(status);
    statusLabel.paintImmediately(statusLabel.getVisibleRect());
  }

  /**
   * Get the status line for the window
   */

  public String getStatus()
  {
    return statusLabel.getText();
  }
  
  /**
   * Show the help window.
   *
   * This might someday take an argument, which would show a starting page
   * or some more specific help.
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

  public void showErrorMessage(String message)
  {
    showErrorMessage("Error", message);
  }

  /**
   * Popup an error dialog.
   */

  public void showErrorMessage(String title, String message)
  {
    //JOptionPane.showInternalMessageDialog(mainPanel, message, title, JOptionPane.ERROR_MESSAGE);
    JErrorDialog d = new JErrorDialog(this, title, message);
  }

  public void setWaitCursor()
  {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  public void setNormalCursor()
  {
    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }

  /**
   * This indeicates that something in the database was changed, so canceling this transaction will have consequences.
   *
   * This should be called whenever the client makes any changes to the database.
   */
  public final void somethingChanged()
  {
      somethingChanged = true;
  }

  public boolean getSomethingChanged()
  {
    return somethingChanged;

  }

  /**
   *
   * This method takes a ReturnVal object from the server and, if necessary,
   * runs through a wizard interaction sequence, possibly displaying several
   * dialogs before finally returning a final result code.
   *
   * Use the ReturnVal returned from this function after this function is
   * called.  Always.
   */

  public ReturnVal handleReturnVal(ReturnVal retVal)
  {
    System.err.println("** gclient: Entering handleReturnVal");

    while ((retVal != null) && (retVal.getDialog() != null))
      {
	JDialogBuff jdialog = retVal.getDialog();

	StringDialog dialog = new StringDialog(jdialog.extractDialogRsrc(this));

	Hashtable result = dialog.DialogShow();

	if (retVal.getCallback() != null)
	  {
	    try
	      {
		System.out.println("Sending result to callback: " + result);
		retVal = retVal.getCallback().respond(result);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("Caught remote exception: " + ex.getMessage());
	      }
	  }
	else
	  {
	    System.out.println("No callback, breaking");
	    break;		// we're done
	  }
      }

    if ((retVal == null) || retVal.didSucceed()) 
      {
	somethingChanged(); 
      }

    System.err.println("** gclient: Exiting handleReturnVal");

    return retVal;
  }

  // Private methods

  /**
   * Note that this actually returns a JPanel.
   *
   * That's so I can put the ComboBox in.
   */

  JPanel createToolbar()
  {
    JPanel panel = new JPanel(new FlowLayout());
    JToolBar toolBar = new JToolBar();
    toolBar.setBorderPainted(false);
    Insets insets = new Insets(0,0,0,0);
    
    toolBar.setMargin(insets);

    JButton b = new JButton(new ImageIcon(pencil));
    b.setActionCommand("open object for editing");
    b.setToolTipText("Edit an object");
    b.addActionListener(this);
    b.setMargin(insets);
    toolBar.add(b);

    b = new JButton(new ImageIcon(trash));
    b.setActionCommand("delete an object");
    b.setToolTipText("Delete an object");
    b.addActionListener(this);
    b.setMargin(insets);
    toolBar.add(b);

    b = new JButton(new ImageIcon(search));
    b.setActionCommand("open object for viewing");
    b.setToolTipText("View an object");
    b.setMargin(insets);
    b.addActionListener(this);
    toolBar.add(b);

    panel.add(toolBar);
    
    if ((personae != null)  && personae.size() > 0)
      {
	System.out.println("Adding persona stuff");
	
	personaCombo = new JComboBox();
	for(int i =0; i< personae.size(); i++)
	  {
	    personaCombo.addItem((String)personae.elementAt(i));
	  }
	personaCombo.setSelectedItem(my_username);

	personaCombo.addActionListener(personaListener);

	panel.add(new JLabel("Persona:"));
	panel.add(personaCombo);

      }
    else
      {
	System.out.println("No personas.");
      }

    // Now the connected timer.
    timerLabel = new JLabel("00:00:00", JLabel.RIGHT);
    timer = new connectedTimer(timerLabel, 5000, true);
    timerLabel.setMinimumSize(new Dimension(200,timerLabel.getPreferredSize().height));
    panel.add(timerLabel);

    return panel;
  }

  public void setPersonaCombo(String persona)
  {
    if (personaCombo != null)
      {
	personaCombo.setSelectedItem(persona);
      }
  }

  /**
   * This clears out the tree and completely rebuilds it.
   */

  void rebuildTree()
  {
    tree.clearTree();

    try
      {
	buildTree();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not rebuild tree: " + rx);
      }
  }

  /**
   *
   * This method builds the initial data structures for the object
   * selection tree, using the base information in the baseHash
   * hashtable.
   * 
   */

  void buildTree() throws RemoteException
  {
    if (debug)
      {
	System.out.println("Building tree");
      }

    CategoryTransport transport = session.getCategoryTree();
    Category firstCat = transport.getTree();

    System.out.println("got root category: " + firstCat.getName());

    CatTreeNode firstNode = new CatTreeNode(null, firstCat.getName(), firstCat,
					    null, true, 
					    OPEN_CAT, CLOSED_CAT, null);
    tree.setRoot(firstNode);

    try
      {
	recurseDownCategories(firstNode);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Cound't recurse down catagories: " + rx);
      }

    if (debug)
      {
	System.out.println("Refreshing tree");
      }

    tree.refresh();

    if (debug)
      {
	System.out.println("Done building tree,");
      }
  }

  void recurseDownCategories(CatTreeNode node) throws RemoteException
  {
    Vector
      children;

    Category c;
    CategoryNode cNode;

    treeNode 
      thisNode,
      prevNode;

    /* -- */
      
    c = node.getCategory();
    
    node.setText(c.getName());
    
    children = c.getNodes();

    prevNode = null;
    thisNode = node.getChild();

    for (int i = 0; i < children.size(); i++)
      {
	// find the CategoryNode at this point in the server's category tree
	cNode = (CategoryNode)children.elementAt(i);

	if (cNode instanceof Base)
	  {
	    Base base = (Base) cNode;
	    
	    if (base.isEmbedded())
	      {
		continue;	// we don't want to present embedded objects
	      }
	  }
	  
	prevNode = insertCategoryNode(cNode, prevNode, node);

	if (prevNode instanceof CatTreeNode)
	  {
	    recurseDownCategories((CatTreeNode)prevNode);
	  }
      }
  }

  /**
   *
   * Helper method for building tree
   *
   */

  treeNode insertCategoryNode(CategoryNode node, treeNode prevNode, treeNode parentNode) throws RemoteException
  {
    treeNode newNode = null;
      
    if (node instanceof Base)
      {
	Base base = (Base)node;
	newNode = new BaseNode(parentNode, base.getName(), base, prevNode,
			       true, 
			       OPEN_BASE, 
			       CLOSED_BASE,
			       pMenu);
	shortToBaseNodeHash.put(new Short(base.getTypeID()), newNode);
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
    else
      {
	System.out.println("Unknown instance: " + node);
      }

    tree.insertNode(newNode, true);
      
    //    if (newNode instanceof BaseNode)
    //      {
    //	refreshObjects((BaseNode)newNode, false);
    //      }
    
    return newNode;
  }

  /**
   *
   * This method updates the tree for the nodes that might have changed.
   *
   * @param committed True if commit was clicked, false if cancel was clicked.
   */

  void refreshTree(boolean committed) throws RemoteException
  {
    // First get rid of deleted nodes

    Enumeration deleted = deleteHash.keys();

    while (deleted.hasMoreElements())
      {
	Invid invid = (Invid)deleted.nextElement();
	InvidNode node = (InvidNode)invidNodeHash.get(invid);
	if (node != null)
	  {
	    if (committed)
	      {

		System.out.println("Deleteing node: " + node.getText());
		tree.deleteNode(node, false);
		invidNodeHash.remove(invid);
	      }
	    else
	      {
		System.out.println("Canceling the delete");
		// Change icon back
		node.setImages(OPEN_FIELD, CLOSED_FIELD);
	      }
	  }
      }
    
    deleteHash.clear();

    //
    // Now change the created nodes
    //

    Enumeration created = createHash.keys();
    while (created.hasMoreElements())
      {
	Invid invid = (Invid)created.nextElement();
	InvidNode node = (InvidNode)invidNodeHash.get(invid);
	if (node != null)
	  {
	    if (committed)
	      {
		System.out.println("Committing created node: " + node.getText());
		// change the icon
		node.setImages(OPEN_FIELD, CLOSED_FIELD);
		node.setText(session.viewObjectLabel(invid));
	      }
	    else
	      {
		System.out.println("Canceling created node: " + node.getText());
		tree.deleteNode(node, false);
		invidNodeHash.remove(invid);
	      }
	  }
      }
    createHash.clear();

    //
    // Last change the changed nodes.
    //

    Enumeration changed = changedHash.keys();
    while (changed.hasMoreElements())
      {
	Invid invid = (Invid)changed.nextElement();
	InvidNode node = (InvidNode)invidNodeHash.get(invid);
	if (node != null)
	  {
	    if (committed)
	      {
		if (debug)
		  {
		    // This shouldn't be the original label.
		    System.out.println("Updating node: " + node.getText());
		  }
		try
		  {
		    node.setText(session.viewObjectLabel(invid));
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not get label: " + rx);
		  }
	      }
	    else
	      {
		System.out.println("Cancelled, no change to object?");
		// Don't know what to do here, maybe change back?  then
		// don't need the change up there either.
	      }
	    node.setImages(OPEN_FIELD, CLOSED_FIELD);
	  }

      }

    changedHash.clear();

    tree.refresh();
  }

  /**
   *
   * This method is used to update the list of object nodes under a given
   * base node in our object selection tree.
   *
   */
  
  void refreshObjects(BaseNode node, boolean doRefresh) throws RemoteException
  {
    Base base;
    Invid invid = null;
    String label = null;
    Vector vect;
    BaseNode parentNode;
    InvidNode oldNode, newNode, fNode;
    Query _query = null;

    ObjectHandle handle = null;
    Vector objectHandles;
    objectList objectlist = null;

    short id;

    /* -- */

    base = node.getBase();    

    try
      {
	id = base.getTypeID();
	//Now get all the children
	_query = new Query(id);
	node.setQuery(_query);
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("It's the Query! " + rx);
      }

    Short Id = new Short(id);

    if (cachedLists.containsList(Id))
      {
	objectlist = cachedLists.getList(Id);
      }
    else
      {
	try
	  {
	    Query q = new Query(id);
	    QueryResult qr = session.query(q);

	    if (qr != null)
	      {
		System.out.println("Caching copy");
		objectlist = new objectList(qr);
		cachedLists.putList(Id, objectlist);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get dump: " + rx);
	  }
      }
    
    objectHandles = objectlist.getObjectHandles(true); // include inactives

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

    parentNode = node;
    oldNode = null;
    fNode = (InvidNode) node.getChild();
    int i = 0;
	
    while ((i < objectHandles.size()) || (fNode != null))
      {
	//System.out.println("Looking at the next node");
	//System.out.println("i = " + i + " length = " + unsorted_objects.length);
	
	if (i < objectHandles.size())
	  {
	    handle = (ObjectHandle) objectHandles.elementAt(i);

	    invid = handle.getInvid();
	    label = handle.getLabel();
	  }
	else
	  {
	    // We've gone past the end of the list of objects in this
	    // object list.. from here on out, we're going to wind up
	    // removing anything we find in this subtree

	    //System.out.println("Object is null");

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
					      OPEN_FIELD,
					      CLOSED_FIELD,
					      node.canInactivate() ? objectInactivatePM : objectRemovePM,
					      handle);
	    
	    invidNodeHash.put(invid, objNode);
	    setIconForNode(invid);
	   
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

	    // System.out.println("Removing this node");
	    // System.err.println("Deleting: " + fNode.getText());

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

	    // System.err.println("Setting: " + object.getName());

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
   *
   * This method changes the icon for the tree node for the
   * provided invid.
   *
   */

  public void setIconForNode(Invid invid)
  {
    InvidNode node = (InvidNode)invidNodeHash.get(invid);
    ObjectHandle handle = node.getHandle();

    if (node == null)
      {
	System.out.println("There is no node for this invid, silly!");
      }
    else
      {
	if (deleteHash.containsKey(invid))
	  {
	    if (debug)
	      {
		System.out.print("Setting icon to delete.");
	      }
	    node.setImages(OPEN_FIELD_DELETE, CLOSED_FIELD_DELETE);
	  }
	else if (createHash.containsKey(invid))
	  {
	    if (debug)
	      {
		System.out.print("Setting icon to create.");
	      }
	    node.setImages(OPEN_FIELD_CREATE, CLOSED_FIELD_CREATE);
	  }
	else if (removeHash.containsKey(invid))
	  {
	    node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
	  }
	else if (expireHash.containsKey(invid))
	  {
	    node.setImages(OPEN_FIELD_EXPIRESET, CLOSED_FIELD_EXPIRESET);
	  }
	else if (inactivateHash.containsKey(invid))
	  {
	    node.setMenu(objectReactivatePM);
	    String text = node.getText();
	    if (text.indexOf("Inactivated") > 0)
	      {
		System.out.println("It already says inactivated.");
	      }
	    else
	      {
		node.setText(node.getText() + " (Inactivated)");
	      }

	    node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
	    tree.refresh();
	  }
	else if (changedHash.containsKey(invid))
	  {
	    if (debug)
	      {
		System.out.print("Setting icon to edit.");
	      }
	    node.setImages(OPEN_FIELD_CHANGED, CLOSED_FIELD_CHANGED);
	  }
	else if (handle != null)
	  {
	    if (handle.isExpirationSet())
	      {
		node.setImages(OPEN_FIELD_EXPIRESET, CLOSED_FIELD_EXPIRESET);
	      }
	    else if (handle.isRemovalSet())
	      {
		node.setImages(OPEN_FIELD_REMOVESET, CLOSED_FIELD_REMOVESET);
	      }
	    else // nothing special in handle
	      {
		node.setImages(OPEN_FIELD, CLOSED_FIELD);
	      } 
	  }
	else // no handle
	  {
	    node.setImages(OPEN_FIELD, CLOSED_FIELD);
	  }
      }
  }

  public void addToExpireHash(Invid invid)
  {
    if (! expireHash.containsKey(invid))
      {
	try
	  {
	    expireHash.put(invid, new CacheInfo(new Short(invid.getType()), session.viewObjectLabel(invid), null));
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("coult not update expireHash: " + rx);
	  }
      }
    setIconForNode(invid);
  }

  public void addToRemoveHash(Invid invid)
  {
    if ( ! removeHash.containsKey(invid))
      {
	try
	  {
	    removeHash.put(invid, new CacheInfo(new Short(invid.getType()), session.viewObjectLabel(invid), null));
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not update removeHash: " + rx);
	  }
      }
    setIconForNode(invid);

  }

  /********************************************************************************
   *
   * actions on objects.
   *
   *
   * These are the methods to use to do something to an object.
   *
   ********************************************************************************/

  public void editObject(Invid invid)
  {
    editObject(invid, null);
  }

  /**
   * open a new window to edit the object.
   *
   * Use this to edit objects, so gclient can keep track of the caches, tree nodes,
   * and all the other dirty work.  This should be the only place windowPanel.addWindow
   * is called for editing purposes.
   */
  public void editObject(Invid invid, String objectType)
  {
    try
      {
	db_object o = session.edit_db_object(invid);
	if (o == null)
	  {
	    setStatus("edit_db_object returned a null pointer, aborting");
	    return;
	  }
	wp.addWindow(o, true, objectType);
	InvidNode node = null;
	if (invidNodeHash.containsKey(invid))
	  {
	    node = (InvidNode)invidNodeHash.get(invid);
	  }

	changedHash.put(invid, new CacheInfo(new Short(o.getTypeID()), session.viewObjectLabel(invid), null));

	setIconForNode(invid);
	tree.refresh();
      }
    catch(RemoteException rx)
      {
	throw new RuntimeException("Could not edit object: " + rx);
      }
  }

  /** 
   * Open a new window with a newly created object.
   *
   * @type Type of object to be created
   * @showNow if true, a new window will be shown.  If false, just return the object.
   *
   * Call showNewlyCreatedObject() to show it later.
   */

  public db_object createObject(short type, boolean showNow)
  {
    Invid invid = null;
    db_object obj = null;

    /* -- */

    if (!defaultOwnerChosen)
      {
	chooseDefaultOwner(false);
      }
    try
      {
	obj = session.create_db_object(type);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Exception creating new object: " + rx);
      }

    if (obj == null)
      {
	throw new RuntimeException("Could not create object for some reason.  Check the Admin console.");
      }

    if (showNow)
      {
	showNewlyCreatedObject(obj, invid, new Short(type));
      }

    somethingChanged();

    return obj;
  }

  /**
   * Add a new window and everything for a new object.
   *
   * obj can be null!  If it is, then this will create a new object of the type.
   *
   * @param obj the object created, can be null.  If you give a non-null object, 
   *            then this method will not create a new object.
   *
   * @param type The type of the object, used in creating.
   */

  public void showNewlyCreatedObject(db_object obj, Invid invid, Short type)
  {

    ObjectHandle handle = new ObjectHandle("New Object", invid, false, false, false);

       
    wp.addWindow(obj, true);
    
    if (invid == null)
      {
	try
	  {
	    invid = obj.getInvid();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get invid: " + rx);
	  }
      }

    if (cachedLists.containsList(type))
      {
	objectList list = cachedLists.getList(type);
	list.addObjectHandle(handle);
      }
    
    // If the base node is open, deal with the node.

    BaseNode baseN = null;

    if (shortToBaseNodeHash.containsKey(type))
      {
	baseN = (BaseNode)shortToBaseNodeHash.get(type);
	if (baseN.isLoaded())
	  {

	    InvidNode objNode = new InvidNode(baseN, 
					      "New Object", 
					      invid,
					      null, false,
					      OPEN_FIELD_CREATE,
					      CLOSED_FIELD_CREATE,
					      baseN.canInactivate() ? objectInactivatePM : objectRemovePM,
					      handle);
	    
	    createHash.put(invid, new CacheInfo(type, "New Object", null));
	    
	    invidNodeHash.put(invid, objNode);
	    setIconForNode(invid);
	    
	    tree.insertNode(objNode, true);
	  }
      }
  }

  /**
   * Open a view window on this object.
   */

  public void viewObject(Invid invid)
  {
    viewObject(invid, null);
  }

  /**
   * Open a new window to view the current object.
   */

  public void viewObject(Invid invid, String objectType)
  {
    try
      {
	wp.addWindow(session.view_db_object(invid), false, objectType);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not edit object: " + rx);
      }
  }

  /**
   * Delete the object.
   *
   * Also takes care of caches and treeNodes.  
   */

  public boolean deleteObject(Invid invid)
  {
    ReturnVal retVal;
    boolean ok = false;

    /* -- */

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
	    //InvidNode node = (InvidNode)invidNodeHash.get(invid);

	    // Check out the deleteHash.  If this one is already on there,
	    // then I don't know what to do.  If it isn't, then add a new
	    // cache info.  I guess maybe update the name or something,
	    // if it is on there.

	    CacheInfo info = null;

	    // Take this object out of the cachedLists, if it is in there

	    if (cachedLists.containsList(id))
	      {
		String label = session.viewObjectLabel(invid);

		System.out.println("This base has been hashed.  Removing: " + label);

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
		System.out.println("already deleted, nothing to change, right?");
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
    catch(RemoteException rx)
      {
	throw new RuntimeException("Could not delete base: " + rx);
      }

    return ok;
  }

  public boolean inactivateObject(Invid invid)
  {
    boolean ok = false;
    ReturnVal retVal;

    if (invid == null)
      {
	System.out.println("Canceled");
      }
    else
      {
	try
	  {
	    StringDialog d = new StringDialog(this, 
					      "Verify invalidation", 
					      "Are you sure you want to inactivate " + 
					      session.viewObjectLabel(invid), "Yes", "No");
	    Hashtable result = d.DialogShow();

	    if (result == null)
	      {
		setStatus("Cancelled!");
	      }
	    else
	      {
		setStatus("inactivating " + invid);

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
		    inactivateHash.put(invid, new CacheInfo(new Short(invid.getType()), session.viewObjectLabel(invid), null));
		    setIconForNode(invid);
		    setStatus("Object inactivated.");
		  }
		else
		  {
		    setStatus("Could not inactivate object.");
		  }
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not verify invid to be inactivated: " + rx);
	  }
      }

    return ok;
  }

  public boolean reactivateObject(Invid invid)
  {
    ReturnVal retVal;
    boolean ok = false;

    try
      {
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
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not reactivate object: " + rx);
      }

    return ok;
  }
  /**
   * Show a panel which takes a string, and a combo box of types.
   *
   */

  void editObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }

    openDialog.setText("Open object for editing");

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	System.out.println("Canceled");
      }
    else
      {
	editObject(invid, openDialog.getTypeString());
      }
  }

  /**
   * Open an object for viewing.
   *
   * This displays a window with a chooser for the base and field for the name.
   *
   */

  void viewObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }

    openDialog.setText("Open object for viewing");

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	System.out.println("Canceled");
      }
    else
      {
	viewObject(invid);
      }
  }

  void cloneObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }

    openDialog.setText("Choose object to be cloned");

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	System.out.println("Canceled");
      }
    else
      {
	try
	  {
	    wp.addWindow(session.clone_db_object(invid), true);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not edit object: " + rx);
	  }
      }
  }

  void inactivateObjectDialog()
  {

    /* -- */

    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }

    openDialog.setText("Choose object to be inactivated");

    Invid invid = openDialog.chooseInvid();
    
    inactivateObject(invid);

  }

  void deleteObjectDialog()
  {
    if (openDialog == null)
      {
	openDialog = new openObjectDialog(this);
      }

    openDialog.setText("Choose object to be deleted");

    Invid invid = openDialog.chooseInvid();

    if (invid == null)
      {
	System.out.println("Canceled");
      }
    else
      {
	try
	  {
	    StringDialog d = new StringDialog(this, "Verify deletion", 
					      "Are you sure you want to delete " + 
					      session.viewObjectLabel(invid), 
					      "Yes", "No");
	    Hashtable result = d.DialogShow();

	    if (result == null)
	      {
		setStatus("Cancelled!");
	      }
	    else
	      {
		deleteObject(invid);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not verify invid to be inactivated: " + rx);
	  }
      }
  }

  void logout()
  {
    try
      {
	timer.stop();
	session.logout();
	this.dispose();
	_myglogin.enableButtons(true);
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("could not logout: " + rx);
      }
  }

  public void chooseFilter()
  {
    // This could be moved, only cache if filter is changed?
    clearCaches();
    if (filterDialog == null)
      {
	filterDialog = new JFilterDialog(this);
      }
    else
      {
	filterDialog.setVisible(true);
      }
    rebuildTree();
    
  }

  public void chooseDefaultOwner(boolean forcePopup)
  {
    // What to do here?  don't check for null, because maybe forcePopup was false.
    // Have to think about this one, maybe keep groups Vector in gclient (ie not local here)

    if (ownerGroups == null)
      {
	try
	  {
	    ownerGroups = session.getOwnerGroups().getListHandles();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Can't figure out owner groups: " + rx);
	  }
	if (ownerGroups == null)
	  {
	    throw new RuntimeException("Whoa!  groups is null");
	  }
      }

    if (ownerGroups.size() == 0)
      {
	throw new RuntimeException("Whoa!  groups is empty");
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
		session.setDefaultOwner(owners);
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not set default owner: " + rx);
	      }
	    return;
	  }
      }
  	
    if (defaultOwnerDialog == null)
      {
	defaultOwnerDialog = new JDefaultOwnerDialog(this, ownerGroups);
      }
    else
      {
	defaultOwnerDialog.setVisible(true);
      }

    defaultOwnerChosen =  true;

  }

  public boolean defaultOwnerChosen()
  {
    return defaultOwnerChosen;
  }

  public synchronized void registerNewContainerPanel(containerPanel cp)
  {
    containerPanels.addElement(cp);
    System.out.println(" inc. containerPanelCount = " + containerPanelCount);
  }

  public synchronized void containerPanelFinished(containerPanel cp)
  {
    containerPanels.removeElement(cp);
    System.out.println(" dec. containerPanelCount = " + containerPanelCount);
    this.notify();
  }

  /*
   * This checks to see if anything has been changed.  Basically, if edit panels are
   * open and have been changed in any way, then somethingChanged will be true and 
   * the user will be warned.  If edit panels are open but have not been changed, then
   * it will return true(it is ok to proceed)
   */

  boolean OKToProceed()
  {
    if (somethingChanged)
      {
	StringDialog dialog = new StringDialog(this, 
					       "Warning: changes have been made",
					       "You have made changes in objects without \ncommiting those changes.  If you continue, \nthose changes will be lost",
					       "Discard Changes",
					       "Cancel");
	// if DialogShow is null, cancel was clicked
	// So return will be false if cancel was clicked
	return (dialog.DialogShow() != null);
	  
      }
    else
      {
	  return true;
      }
  }

  void updateNotePanels()
  {
    Vector windows = wp.getEditables();

    for (int i = 0; i < windows.size(); i++)
      {
	framePanel fp = (framePanel)windows.elementAt(i);

	if (fp == null)
	  {
	    System.out.println("null frame panel in updateNotesPanels");
	  }
	else
	  {
	    notesPanel np = fp.getNotesPanel();
	    if (np == null)
	      {
		System.out.println("null notes panel in frame panel");
	      }
	    else
	      {
		np.updateNotes();
	      }
	  }
      }
  }

  public void commitTransaction()
  {
    ReturnVal retVal;

    /* -- */

    try
      {
	this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	updateNotePanels();
	
	wp.closeEditables();
	somethingChanged = false;

	retVal = session.commitTransaction();

	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);
	  }

	boolean succeeded = (retVal == null) ? true : retVal.didSucceed();

	if (succeeded)
	  {
	    /*
	    if (somethingChanged)
	      {
		// Might need to fix the tree nodes
		// Now go through changed list and revert any names that may be needed

		Enumeration changed = changedHash.keys();

		while (changed.hasMoreElements())
		  {
		    Invid invid = (Invid)changed.nextElement();
		    CacheInfo info = (CacheInfo)changedHash.get(invid);
		    String label = null;

		    try
		      {
			label = session.viewObjectLabel(invid);
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not get label: " + rx);
		      }
		
		    InvidNode node = (InvidNode)invidNodeHash.get(invid);

		    if (node != null)
		      {
			node.setText(label);
		      }
		  }	    

	      }
	    */

	    wp.refreshTableWindows();
	    session.openTransaction("gclient");

	    System.out.println("Done committing");
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

	    refreshTree(true);

	    cachedLists.clearCaches();
	
	    wp.resetWindowCount();
	  }
	else
	  {
	    showErrorMessage("Error: commit failed", "Could not commit your changes.");
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not commit transaction" + rx);
      }
  }

  public synchronized void cancelTransaction()
  {
    ObjectHandle handle;
    ReturnVal retVal;

    /* -- */

    try
      {
	// First we need to tell all the container Panels to stop loading

	for (int i = 0; i < containerPanels.size(); i++)
	  {
	    System.out.println("Shutting down containerPanel");
	    containerPanel cp = (containerPanel)containerPanels.elementAt(i);
	    
	    cp.stopLoading();
	  }
	
	long startTime = System.currentTimeMillis(); // Only going to wait 10 seconds

	while ((containerPanels.size() > 0) && (System.currentTimeMillis() - startTime < 10000))
	  {
	    try
	      {
		System.out.println("Waiting for containerPanels to shut down.");
		this.wait(1000);
	      }
	    catch (InterruptedException x)
	      {
		throw new RuntimeException("Interrupted while waiting for container panels to stop: " + x);
	      }
	  }

	if (debug && (containerPanels.size() > 0))
	  {
	    System.out.println("Hmm, containerPanels is still not empty, the timeout must have kicked in.  Oh well.");
	  }
	
	wp.closeEditables();

	retVal = session.abortTransaction();

	if (retVal != null)
	  {
	    retVal = handleReturnVal(retVal);
	  }

	// Now we need to fix up the caches, and clean up all the changes made
	// during the transaction

	// any objects that we 'deleted' we'll clear the deleted bit

	Enumeration dels = deleteHash.keys();

	while (dels.hasMoreElements())
	  {
	    Invid invid = (Invid)dels.nextElement();
	    CacheInfo info = (CacheInfo)deleteHash.get(invid);

	    if (cachedLists.containsList(info.getBaseID()))
	      {
		if (createHash.containsKey(invid))
		  {
		    System.out.println("Can't fool me: you just created this object!");
		  }
		else
		  {
		    System.out.println("This one is hashed, sticking it back in.");

		    objectList list = cachedLists.getList(info.getBaseID());

		    handle = info.getObjectHandle();
		    if (handle != null)
		      {
			list.addObjectHandle(handle);
		      }
		  }
	      }
	    deleteHash.remove(invid);
	    setIconForNode(invid);
	  }
	
	// Next up is created list: remove all the added stuff.

	Enumeration created = createHash.keys();

	while (created.hasMoreElements())
	  {
	    Invid invid = (Invid)created.nextElement();
	    CacheInfo info = (CacheInfo)createHash.get(invid);

	    if (cachedLists.containsList(info.getBaseID()))
	      {
		System.out.println("This one is hashed, taking a created object out.");

		objectList list = cachedLists.getList(info.getBaseID());

		list.removeInvid(invid);
	      }
	    createHash.remove(invid);
	    setIconForNode(invid);
	  }

	// Now go through changed list and revert any names that may be needed

	Enumeration changed = changedHash.keys();

	while (changed.hasMoreElements())
	  {
	    Invid invid = (Invid)changed.nextElement();
	    CacheInfo info = (CacheInfo)changedHash.get(invid);

	    if (cachedLists.containsList(info.getBaseID()))
	      {
		System.out.println("This changed base is cached, fixing it back.");

		objectList list = cachedLists.getList(info.getBaseID());
		
		list.relabelObject(invid, info.getOriginalLabel());
	      }
	    changedHash.remove(invid);
	    setIconForNode(invid);
	  }

	changed = inactivateHash.keys();
	while (changed.hasMoreElements())
	  {
	    Invid invid = (Invid)changed.nextElement();
	    
	    InvidNode node = (InvidNode)invidNodeHash.get(invid);
	    if (node.getText().indexOf("Inactivated") > 0)
	      {
		System.out.println("Fixing this one: " + node.getText()); 
		node.setText(node.getText().substring(0, node.getText().indexOf("(Inactivated)")));
	      }

	    inactivateHash.remove(invid);
	    setIconForNode(invid);
	  }

	somethingChanged = false;
	session.openTransaction("glient");

	// This will fix up the tree (remove the trash cans), and 
	// clear the created/changed/deleted hashes

	if (createHash.isEmpty() && deleteHash.isEmpty() && changedHash.isEmpty() && inactivateHash.isEmpty())
	  {
	    System.out.println("Woo-woo the hashes are all empty");
	  }
	refreshTree(false);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not abort transaction" + rx);
      }
  }

  // ActionListener Methods
  
  public void actionPerformed(java.awt.event.ActionEvent event)
  {
    Object source = event.getSource();
    String command = event.getActionCommand();
    System.out.println("Action: " + command);
    
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
    else if (source == menubarQueryMI)
      {
	querybox box = new querybox(getBaseHash(), getBaseMap(), this, "Query Panel");
	Query q = box.myshow();

	if (q != null)
	  {
	    DumpResult buffer = null;

	    try
	      {
		buffer = session.dump(q);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("caught remote: " + ex);
	      }

	    wp.addTableWindow(session, q, buffer, "Query Results");
	  }
      }
    else if (source == removeAllMI)
      {
	if (OKToProceed())
	  {
	    wp.closeAll();
	  }
      }
    else if (source == rebuildTreeMI)
      {
	rebuildTree();
      }
    else if (source == logoutMI)
      {
	if (OKToProceed())
	  {
	    logout();
	  }
      }
    else if (command.equals("open object for editing"))
      {
	editObjectDialog();
      }
    else if (command.equals("open object for viewing"))
      {
	viewObjectDialog();
      }
    else if (command.equals("choose an object for cloning"))
      {
	cloneObjectDialog();
      }
    else if (command.equals("delete an object"))
      {
	deleteObjectDialog();
      }
    else if (command.equals("inactivate an object"))
      {
	inactivateObjectDialog();
      }
    else if (command.equals("Filter Query"))
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
    else
      {
	System.err.println("Unknown action event generated");
      }
  }
  
  protected void processWindowEvent(WindowEvent e) 
  {
    super.processWindowEvent(e);

    if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
	System.out.println("Window closing");

	if (OKToProceed())
	  {
	    if (debug)
	      {
		System.out.println("It's ok to log out.");
	      }
	    logout();
	  }
	else if (debug)
	  {
	    System.out.println("No log out!");
	  }
      }
  }

  // treeCallback methods

  public void treeNodeExpanded(treeNode node)
  {
    if (node instanceof BaseNode && !((BaseNode) node).isLoaded())
      {
	setStatus("Loading objects for base " + node.getText());
	setWaitCursor();

	try
	  {
	    refreshObjects((BaseNode)node, true);
	  }
	catch (RemoteException ex)
	  {
	    setStatus("Remote exception loading objects for base " + node.getText());
	    throw new RuntimeException("remote exception in trying to fill this base " + ex);
	  }

	setStatus("Done loading objects for base " + node.getText());

	((BaseNode) node).markLoaded();
	setNormalCursor();
      }
  }

  public void treeNodeContracted(treeNode node)
  {
  }

  public void treeNodeSelected(treeNode node)
  {
    validate();
  }

  public void treeNodeUnSelected(treeNode node, boolean otherNode)
  {
  }

  public void treeNodeMenuPerformed(treeNode node,
				    java.awt.event.ActionEvent event)
  {
    // Unclear why the following bit was here.. I've commented it out for
    // now.  As far as I can see, the only possible impact to commenting this
    // out is to cause object creation to not have the new node updated properly..
    // this is a minor thing compared to unnecessary delay as we load the object nodes
    //
    // 23 Jan 1998 - Jon

    //    if (node instanceof BaseNode)
    //      {
    //	// make sure we've got the list updated
    //
    //	treeNodeExpanded(node);
    //      }
    
    if (event.getSource() == createMI)
      {
	System.out.println("createMI");

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;

	    try
	      {
		short id = baseN.getBase().getTypeID();
		createObject(id, true);

	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not create object: " + rx);
	      }
	  }
	else
	  {
	    System.err.println("not a base node, can't create");
	  }
      }
    else if (event.getSource() ==  viewMI)
      {
	System.out.println("viewMI");

	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;

	    try
	      {
		Query _query = new Query(baseN.getBase().getTypeID(), null, true);

		setStatus("Sending query for base " + node.getText() + " to server");

		DumpResult buffer = session.dump(_query);

		if (buffer == null)
		  {
		    setStatus("results == null");
		    System.out.println("results == null");
		  }
		else
		  {
		    setStatus("Server returned results for query on base " + node.getText() + " - building table");

		    System.out.println();

		    wp.addTableWindow(session, baseN.getQuery(), buffer, "Query Results");
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not get query: " + rx);
	      }
	  }
	else
	  {
	    System.out.println("viewMI from a node other than a BaseNode");
	  }
      }
    
    else if (event.getSource() ==  viewAllMI)
      {
	if (debug)
	  {
	    System.out.println("viewAllMI");
	  }
	
	if (node instanceof BaseNode)
	  {
	    BaseNode baseN = (BaseNode)node;
	    
	    try
	      {
		Query _query = new Query(baseN.getBase().getTypeID(), null, false);

		setStatus("Sending query for base " + node.getText() + " to server");

		DumpResult buffer = session.dump(_query);
		
		if (buffer == null)
		  {
		    setStatus("results == null");
		    System.out.println("results == null");
		  }
		else
		  {
		    setStatus("Server returned results for query on base " + node.getText() + " - building table");

		    System.out.println();
		    
		    wp.addTableWindow(session, baseN.getQuery(), buffer, "Query Results");
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not get query: " + rx);
	      }
	  }
	else
	  {
	    System.out.println("viewAllMI from a node other than a BaseNode");
	  }
      }
    else if (event.getSource() ==  queryMI)
      {
	System.out.println("queryMI");

	if (node instanceof BaseNode)
	  {
	    Base base = ((BaseNode) node).getBase();

	    querybox box = new querybox(base, getBaseHash(), getBaseMap(),  this, "Query Panel");

	    Query q = box.myshow();

	    if (q != null)
	      {
		DumpResult buffer = null;

		try
		  {
		    setStatus("Sending query for base " + node.getText() + " to server");

		    buffer = session.dump(q);
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("caught remote: " + ex);
		  }

		if (buffer != null)
		  {
		    setStatus("Server returned results for query on base " + 
			      node.getText() + " - building table");
		    
		    wp.addTableWindow(session, q, buffer, "Query Results");
		  }
		else
		  {
		    setStatus("results == null");
		    System.out.println("results == null");
		  }
	      }
	  }
      }
    else if (event.getActionCommand().equals("View Object"))
      {
	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;
	    if (deleteHash.containsKey(invidN.getInvid()))
	      {
		// This one has been deleted
		showErrorMessage("This object has already been deleted.");
	      }
	    else
	      {
		viewObject(invidN.getInvid(), invidN.getTypeText());
	      }
	  }
	else
	  {
	    System.err.println("not a base node, can't create");
	  }
      }
    else if (event.getActionCommand().equals("Edit Object"))
      {
	System.out.println("objEditMI");

	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;

	    if (deleteHash.containsKey(invidN.getInvid()))
	      {
		showErrorMessage("This object has already been deleted.");
	      }
	    else
	      {
		Invid invid = invidN.getInvid();
		
		editObject(invid, invidN.getTypeText());
	      }
	  }
	else
	  {
	    System.err.println("not a base node, can't create");
	  }
      }
    else if (event.getActionCommand().equals("Delete Object"))
      {
	// Need to change the icon on the tree to an X or something to show that it is deleted
	System.out.println("Deleting object");
	if (node instanceof InvidNode)
	  {
	    InvidNode invidN = (InvidNode)node;
	    Invid invid = invidN.getInvid();

	    deleteObject(invid);

	  }
	else  // Should never get here, but just in case...
	  {
	    System.out.println("Not a InvidNode node, can't delete this.");
	  }
      }
    else if (event.getActionCommand().equals("Clone Object"))
      {
	System.out.println("objCloneMI");
      }
    else if(event.getActionCommand().equals("Inactivate Object"))
      {
	System.out.println("objInactivateMI");
	if (node instanceof InvidNode)
	  {
	    inactivateObject(((InvidNode)node).getInvid());
	  }
      }
    else if (event.getActionCommand().equals("Reactivate Object"))
      {
	System.out.println("Reactivate item.");
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

  Vector sortListHandleVector(Vector v)
  {
    (new VecQuickSort(v, 
		      new arlut.csd.Util.Compare() {
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

  Vector sortStringVector(Vector v)
  {
    (new VecQuickSort(v, 
		      new arlut.csd.Util.Compare() {
      public int compare(Object a, Object b) 
	{
	  String aF, bF;
	  
	  aF = (String) a;
	  bF = (String) b;
	  int comp = 0;
	  
	  comp =  aF.compareTo(bF);
	  
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


  // why is this here, Mike??

  // Hey, don't blame me.  -Mike

  public void start() throws Exception 
  {
  }
}

/*---------------------------------------------------------------------
                                                                  class 
                                                              InvidNode

---------------------------------------------------------------------*/

class InvidNode extends arlut.csd.JTree.treeNode {

  final static boolean debug = true;

  private Invid invid;

  private String typeText;

  private ObjectHandle handle;

  public InvidNode(treeNode parent, String text, Invid invid, treeNode insertAfter,
		    boolean expandable, int openImage, int closedImage, treeMenu menu,
		   ObjectHandle handle)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);

    this.invid = invid;
    this.typeText = parent.getText();
    this.handle = handle;

    if (debug)
      {
	if (invid == null)
	  {
	    System.out.println(" null invid in InvidNode: " + text);
	  }
      }
  }

  public Invid getInvid()
  {
    return invid;
  }

  // Can't think of why you would ever want this

  public void setInvid(Invid invid)
  {
    this.invid = invid;
  }

  public String getTypeText()
  {
    return typeText;
  }  

  public ObjectHandle getHandle()
  {
    return handle;
  }
}

/*------------------------------------------------------------------------------
                                                                           Class
                                                                        BaseNode

------------------------------------------------------------------------------*/

class BaseNode extends arlut.csd.JTree.treeNode {

  private Base base;
  private Query query;
  private boolean loaded = false;
  private boolean canBeInactivated = false;

  /* -- */

  BaseNode(treeNode parent, String text, Base base, treeNode insertAfter,
	   boolean expandable, int openImage, int closedImage, treeMenu menu)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.base = base;
    
    try
      {
	canBeInactivated = base.canInactivate();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check inactivate.");
      }
  }

  public Base getBase()
  {
    return base;
  }

  public boolean canInactivate()
  {
    return canBeInactivated;
  }

  public void setBase(Base base)
  {
    this.base = base;
  }

  public void setQuery(Query query)
  {
    this.query = query;
  }

  public Query getQuery()
  {
    return query;
  }

  public boolean isLoaded()
  {
    return loaded;
  }

  public void markLoaded()
  {
    loaded = true;
  }

  public void markUnLoaded()
  {
    loaded = false;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 PersonaListener

------------------------------------------------------------------------------*/

class PersonaListener implements ActionListener{

  Session session;

  DialogRsrc
    resource = null;

  gclient
    gc;

  PersonaListener(Session session, gclient parent)
    {
      this.session = session;
      this.gc = parent;
    }

  public void actionPerformed(ActionEvent event)
  {
    //Check to see if we need to commit the transaction first.
    String newPersona = null;

    if (event.getSource() instanceof JMenuItem)
      {
	System.out.println("From menu");
	//JMenuItem good
	newPersona = event.getActionCommand();
      }
    else if (event.getSource() instanceof JComboBox)
      {
	System.out.println("From box");
	//JComboBox bad
	newPersona = (String)((JComboBox)event.getSource()).getSelectedItem();
      }
    else
      {
	System.out.println("Persona Listener doesn't understand that action.");
      }
    
    if (gc.getSomethingChanged())
      {
	// need to ask: commit, cancel, abort?
	StringDialog d = new StringDialog(gc,
					  "Changing personas",
					  "Before changing personas, the transaction must be closed.  Would you like to commit your changes?",
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

    boolean personaChangeSuccessful = false;

    if (resource == null)
      {
	resource = new DialogRsrc(gc, "Change Persona", "Enter the persona password:");
	resource.addPassword("Password:");
      }

      System.out.println("MenuItem action command: " + newPersona);
      
      Hashtable result = null;
      String password = null;

      //Hey, this is no good!

      //if (newPersona.indexOf(":") > 0)

      if (true)
	{
	  StringDialog d = new StringDialog(resource);
	  result = d.DialogShow();
	  password = (String)result.get("Password:");
	}
      else
	{
	  System.out.println("No :, no workie.");
	  return;
	}

      if (password != null)
	{
	  try
	    {	      
	      personaChangeSuccessful = session.selectPersona(newPersona, password);
	      
	      if (personaChangeSuccessful)
		{
		  gc.setStatus("Successfully changed persona.");
		  gc.setTitle("Ganymede Client: " + newPersona + " logged in.");
		  //gc.setPersonaCombo(newPersona);
		  gc.ownerGroups = null;
		  gc.clearCaches();
		  gc.commitTransaction();
		  gc.rebuildTree();
		}
	      else
		{
		  gc.setStatus("Danger Danger!");
		  (new StringDialog(gc, "Error: persona no changie", 
				    "Could not change persona.",
				    false)).DialogShow();

		  gc.setStatus("Persona change failed");
		}
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not set persona to " + newPersona + ": " + rx);
	    }

	}
    }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       CacheInfo

  This may not be needed any more, since there are more hashes.

------------------------------------------------------------------------------*/

class CacheInfo {

  private String
    originalLabel,
    currentLabel;

  private Short
    baseID;

  private ObjectHandle
    handle;

  /* -- */

  public CacheInfo(Short baseID, String originalLabel, String currentLabel)
  {
    this(baseID, originalLabel, currentLabel, null);
  }

  public CacheInfo(Short baseID, String originalLabel, String currentLabel, ObjectHandle handle)
  {
    this.baseID = baseID;
    this.originalLabel = originalLabel;
    this.currentLabel = currentLabel;
    this.handle = handle;
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
}
