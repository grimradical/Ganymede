/*

   querybox.java

   This class implements a modal dialog that is popped up to generate
   a Query object that will be used by the rest of the ganymede.client
   package to submit the query to the server for handling.

   Once an instance of querybox is constructed, the client code will
   call myShow() to pop up the dialog and retrieve the Query object.

   If the user chooses not to submit a Query after all, myShow() will
   return null.
   
   Created: 23 July 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Erik Grostic
              Jonathan Abbey

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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import arlut.csd.JDataComponent.JIPField;
import arlut.csd.JDataComponent.JdateField;
import arlut.csd.JDataComponent.JfloatField;
import arlut.csd.JDataComponent.JnumberField;
import arlut.csd.JDataComponent.JstringField;
import arlut.csd.JDataComponent.StringSelector;
import arlut.csd.JDataComponent.TimedKeySelectionManager;
import arlut.csd.JDataComponent.listHandle;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.Util.FixedListCompare;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.StringUtils;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.BaseDump;
import arlut.csd.ganymede.common.DumpResult;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryAndNode;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryDeRefNode;
import arlut.csd.ganymede.common.QueryNode;
import arlut.csd.ganymede.common.QueryNotNode;
import arlut.csd.ganymede.common.RegexpException;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.Base;

/*------------------------------------------------------------------------------
                                                                           class 
                                                                        querybox

------------------------------------------------------------------------------*/

/**
 * This class implements a modal dialog that is popped up to generate
 * a {@link arlut.csd.ganymede.common.Query Query} object that will be
 * used by the rest of the ganymede.client package to submit the query
 * to the server for handling.
 *
 * Once an instance of querybox is constructed, the client code will
 * call myShow() to pop up the dialog and retrieve the Query object.
 * 
 * If the user chooses not to submit a Query after all, myShow() will
 * return null.
 */

class querybox extends JDialog implements ActionListener, ItemListener {
  
  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.querybox");

  // ---

  JTabbedPane
    tabPane;

  OptionsPanel optionsPanel = null;	// to hold the frame that we popup to get a list of
				// desired fields in the query's results
  gclient gc = null;

  // the following hashes are accessed through a set of private accessor
  // methods to avoid confusion
  
  private Hashtable baseIDHash = new Hashtable();
  private Hashtable fieldHash = new Hashtable();
  private Hashtable nameHash = new Hashtable();
  private Hashtable myHash = new Hashtable();

  JButton 
    OkButton = new JButton(ts.l("global.submit_button")), // "Submit"
    CancelButton = new JButton(ts.l("global.cancel_button")), // "Cancel"
    addButton = new JButton(ts.l("global.add_choices_button")),	// "Add Choices"
    removeButton = new JButton(ts.l("global.remove_choices_button")); // "Remove Choices"

  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints gbc = new GridBagConstraints();

  JPanel 
    titledPanel,
    returnedPanel,
    query_Buttons,
    card_panel,
    query_panel = new JPanel();
  JPanel inner_choice = new JPanel();

  // "Only Return Editable Objects"
  JCheckBox editBox = new JCheckBox(ts.l("global.editable_only_checkbox"));

  // "Match All Objects Of This Type"
  JCheckBox allBox = new JCheckBox(ts.l("global.all_objects_checkbox"));

  JComboBox baseChoice = new JComboBox();

  // This is so we can hide the middle panel when the show all button is clicked

  CardLayout 
    card_layout;

  Vector
    fieldChoices = new Vector(), // A vector of strings for the field choice menus in QueryRow
    Rows = new Vector(),	// store the QueryRows
    fields;			// FieldTemplates for the selectedBase

  BaseDump selectedBase = null;

  boolean 
    editOnly = false,
    showAllItems = false;

  Query
    query;

  Image queryIcon;

  /* -- */

  /**
   *
   * Primary constructor.
   *
   * @param defaultBase The object base that will be initially selected.
   *                    May be null.
   *
   * @param gc A gclient used to get access to client caches
   *
   * @param parent The frame that this querybox is to be connected to.
   *
   * @param DialogTitle The title for this dialog.
   *
   */

  public querybox (BaseDump defaultBase, gclient gc,
		   Frame parent, String DialogTitle)
  {
    super(parent, DialogTitle, false); // the boolean value is to make the dialog non-modal

    // ---
    JPanel Choice_Buttons = new JPanel();
    JPanel base_panel = new JPanel();
    JPanel outer_choice = new JPanel();
    query_Buttons = new JPanel();
    JScrollPane choice_pane = new JScrollPane();
    JPanel contentPane = new JPanel();
 
    /* -- */

    tabPane = new JTabbedPane();

    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add("Center", tabPane);
    this.getContentPane().add("South", Choice_Buttons);

    // "Search Criteria"
    tabPane.addTab(ts.l("init.search_term_tab"), null, contentPane);
 
    // Main constructor for the querybox window
    
    this.gc = gc;

    // - Define the main window
    
    contentPane.setLayout(new BorderLayout());
    
    OkButton.addActionListener(this);
    CancelButton.addActionListener(this);
    Choice_Buttons.setLayout(new FlowLayout (FlowLayout.RIGHT));
    Choice_Buttons.add(OkButton);
    Choice_Buttons.add(CancelButton);
    
    editBox.addItemListener(this);
    editBox.setSelected(false);

    allBox.setSelected(false);
    allBox.addItemListener(this);

    query_panel.setLayout(new BorderLayout());
    contentPane.add("Center", query_panel); 

    // - Define the inner window with the query choice buttons

    addButton.addActionListener(this);
    removeButton.addActionListener(this);
    removeButton.setEnabled(false);

    query_Buttons.setLayout(new FlowLayout());
    query_Buttons.add(addButton);
    query_Buttons.add(removeButton);

    // - Define the two inner choice windows

    GridBagLayout bp_gbl = new GridBagLayout();
    GridBagConstraints bp_gbc = new GridBagConstraints();

    base_panel.setLayout(bp_gbl);

    // - Create the choice window containing the fields 

    Vector baseNames = new Vector();

    Enumeration en = gc.getBaseMap().elements();
      
    while (en.hasMoreElements())
      {
	BaseDump key = (BaseDump) en.nextElement();

	// we want to ignore embedded objects -- for now
	    
	if (key.isEmbedded())
	  {
	    // get a base that works...this embedded would cause
	    // problems [null pointer exceptions, that kind of thing]
		
	    continue;
	  }
	else
	  {
	    String choiceToAdd = key.getName();

	    baseNames.addElement(choiceToAdd);
	    mapNameToBase(choiceToAdd, key);
	  }
      }

    // load baseChoice combo box.
    
    baseChoice.setKeySelectionManager(new TimedKeySelectionManager());

    gc.sortStringVector(baseNames);
    
    for (int i = 0; i < baseNames.size(); i++)
      {
	baseChoice.addItem((String) baseNames.elementAt(i));
      }
      
    // set the selected base in the baseChoice before we add the item
    // listener

    if (defaultBase != null)
      {
	baseChoice.setSelectedItem(defaultBase.getName());
      }
    else 
      { 
	// no default given. take whatever the choice control picked
	
	defaultBase = getBaseFromName((String) baseChoice.getSelectedItem());
      }

    baseChoice.addItemListener(this);

    bp_gbc.anchor = bp_gbc.WEST;
    bp_gbc.weightx = 1.0;
    bp_gbc.gridx = 0;
    bp_gbl.setConstraints(baseChoice, bp_gbc);
    base_panel.add(baseChoice);

    queryIcon = PackageResources.getImageResource(this, "query.gif", getClass());
    JLabel queryPic = new JLabel(new ImageIcon(queryIcon));
    queryPic.setBorder(new EmptyBorder(new Insets(0,0,10,10)));
    
    bp_gbc.anchor = bp_gbc.EAST;
    bp_gbc.gridx = 1;
    bp_gbl.setConstraints(queryPic, bp_gbc);
    base_panel.add(queryPic);

    inner_choice.setLayout(gbl);
    
    outer_choice.setLayout(new FlowLayout());
    outer_choice.add(inner_choice);

    choice_pane.setViewportView(outer_choice);
    choice_pane.setBorder(new EmptyBorder(new Insets(0,0,0,0)));    

    card_layout = new CardLayout();
    card_panel = new JPanel(card_layout);
    card_panel.add(choice_pane, "main");
    card_panel.add(new JPanel(), "blank");
    card_layout.show(card_panel, "main");

    // hack for Swing 1.0.2 to prevent TitledBorder from trying to
    // be clever with colors when surrounding a scrollpane
    titledPanel = new JPanel();

    // "Match"
    titledPanel.setBorder(new TitledBorder(new EtchedBorder(), ts.l("init.match_box")));
    titledPanel.setLayout(new BorderLayout());
    titledPanel.add("Center", card_panel);
    titledPanel.add("South", query_Buttons);

    returnedPanel = new JPanel();
    returnedPanel.setBorder(new EmptyBorder(new Insets(0,5,5,0)));
    returnedPanel.setLayout(new FlowLayout());
    returnedPanel.add(allBox);
    returnedPanel.add(Box.createHorizontalStrut(20));
    returnedPanel.add(editBox);

    getContentPane().add("North", base_panel);

    // "Choose Object Type"
    base_panel.setBorder(new TitledBorder(new EtchedBorder(), ts.l("init.type_box")));

    query_panel.add("Center", titledPanel);
    titledPanel.add("North", returnedPanel);

    // switch to the defaultBase

    selectBase(defaultBase);

    optionsPanel = new OptionsPanel(this);

    // "Fields Returned"
    tabPane.addTab(ts.l("init.fields_returned_tab"), null, optionsPanel);

    this.pack();
  }

  /**
   *
   * Alternate Constructor. Used when no default query is provided 
   *
   * @param gc A gclient used to get access to client caches
   *
   * @param parent The frame that this querybox is to be connected to.
   *
   * @param DialogTitle The title for this dialog.
   *
   */

  public querybox (gclient gc,
                   Frame parent,
		   String myTitle) 
  {
    this(null, gc, parent, myTitle);
  } 

  ///////////////////////
  //   Public Methods  //
  ///////////////////////

  /**
   *
   * This is the main interface to the querybox, and is used to
   * synchronously display the querybox and return the Query
   * generated by it.<br><br>
   *
   */

  public void myshow()
  {
    // Method to set the querybox to visible or invisible
    
    setSize(800,400);
    setVisible(true);
  }

  ////////////////////////
  //   Private Methods  //
  ////////////////////////

  /**
   *
   * This method updates the fieldChoices vector to contain a list of
   * Strings corresponding to fields in the selectedBase that can
   * be chosen in QueryRow's.  We are a little fancy here, in that
   * we include fields from embedded objects.
   *
   */

  private void resetFieldChoices()
  {
    FieldTemplate template;
    Vector EIPfields = new Vector(); // edit-in-place
    Vector Embedded = new Vector();

    /* -- */

    synchronized (fieldChoices)
      {
	fieldChoices.removeAllElements();

	for (int i=0; fields != null && (i < fields.size()); i++) 
	  {
	    template = (FieldTemplate) fields.elementAt(i);

	    // ignore containing objects and the like...
	
	    if ((selectedBase.isEmbedded() && template.getID() == SchemaConstants.OwnerListField) ||
		template.getID() == SchemaConstants.BackLinksField)
	      {
		continue;
	      }

	    String name = template.getName();

	    if (template.isEditInPlace())
	      {
		// We're an edit in place.. we want to recurse down to the
		// bottom of this edit-in-place tree, and add all of the
		// nodes to our Embedded vector.
		//
		// The recursive method takes a Vector of
		// FieldTemplates as its first parameter.  We'll add
		// to and remove from EIPfields so that we can cheaply
		// re-use the Vector.

		EIPfields.addElement(template);
		getEmbedded(EIPfields, null, new Short(selectedBase.getTypeID()), Embedded);
		EIPfields.removeElement(template);

		if (!Embedded.isEmpty())
		  {
		    for (int j = 0; (j < Embedded.size()); j++)
		      {
			String embedName = (String) Embedded.elementAt(j);

			// Ok, let's do our string processing for our field name,
			// once and for all by removing the slashes and saving
			// the result. Erik again.

			String noSlash = embedName.substring(embedName.lastIndexOf("/") + 1,
							     embedName.length());

			// Add the slash-less name to the name hash, with the key
			// being the slash filled name

			mapEmbeddedToField(embedName, noSlash);

			// and finally add to fieldChoices

			fieldChoices.addElement(embedName);
		      }

		    // and we're done with Embedded.  Clear it out.

		    Embedded.removeAllElements();
		  }
	      }
	    else
	      {
		// Keep a shortcut for our later fieldname parsing
		// This was Erik's idea.. 
	    
		mapEmbeddedToField(name, name);

		// And keep a map from the elaborated field name to
		// the field template.
	    
		mapNameToTemplate(name, template);
	    
		// and to the base
	    
		mapNameToId(name, new Short(selectedBase.getTypeID()));

		// and finally add to fieldChoices
		fieldChoices.addElement(name);
	      }
	  }
    
	// If we wound up with any embedded (edit-in-place) fields from
	// contained objects, add those fields to our embedded map.
	//
	// Note that we don't try to get fancy with where these extra
	// field possibilities are added in the fieldChoices vector at
	// this point.  We'll sort them, after.

	// sort fieldChoices

	gc.sortStringVector(fieldChoices);

	// and reset the options panel checkboxes.

	if (optionsPanel != null)
	  {
	    optionsPanel.resetBoxes();
	  }
      }
  }

  /**
   *
   * A companion to the prior resetFieldChoices method.
   * It allows fields with references to embedded objects
   * to display the appropriate sub-fields. 
   * 
   * It is a recursive method, and can handle any number
   * of layers of embedding. The fields are stored in
   * a 'global' vector (as strings)
   *
   * @param fields A Vector of FieldTemplate objects that we want
   * to iterate over, looking for embedded fields
   * @param basePrefix A String to prepend to field names we find
   * in the fields Vector, so that we can uniquely place the fields
   * in a single namespace.  Will be null upon the top-level recursive
   * call to getEmbedded().
   * @param lowestBase A Short representing the object type of the
   * object base that contains the FieldTemplates in the fields Vector.
   * @param Embedded A Vector in which we collect all the fully qualified
   * names of embedded fields during our recursion
   *
   */
  
  private void getEmbedded(Vector fields, String basePrefix, 
			   Short lowestBase, Vector Embedded)
  {
    FieldTemplate tempField;
    String myName;
    Short tempIDobj;
    short tempID;
      
    /* -- */
    
    // Examine each field and if it's not referring to an embedded,
    // then add its name + basePrefix to the string vector
    
    for (int j=0; fields != null && (j < fields.size()); j++)
      { 
	tempField = (FieldTemplate) fields.elementAt(j);

	// ignore containing objects and the like...

	if (tempField.getID() == SchemaConstants.OwnerListField ||
	    tempField.getID() == SchemaConstants.BackLinksField)
	  {
	    continue;
	  }

	if (basePrefix != null)
	  {
	    myName = basePrefix + "/" + tempField.getName();  // slap on the prefix
	  }
	else
	  {
	    myName = tempField.getName();
	  }

	// save the embedded information in our Embedded vector

	Embedded.addElement(myName);

	mapNameToTemplate(myName, tempField);

	// Also, save the information on the target base
	// in a hashtable

	// the ID will be used in creating the query for the 
	// edit-in-place

	mapNameToId(myName, lowestBase);

	if (tempField.isEditInPlace())
	  {
	    // we've got an edit-in-place invid field.  if it is
	    // constrained to point to a specific type, go ahead and
	    // recurse down to look at its children

	    tempID = tempField.getTargetBase();

	    if (tempID >= 0)
	      {
		tempIDobj = new Short(tempID);

		// process embedded fields for target

		getEmbedded(gc.getTemplateVector(tempID), 
			    myName, tempIDobj, Embedded);
	      }
	  }
      }
  }

  /**
   *
   * This internal method takes the current state of the rows in the
   * main query composition panel and generates an appropriate Query
   * structure from them.<br><br>
   *
   * Note that this is a private method.. our 'Ok' handler will call
   * this method before hiding this dialog, at which time myShow will
   * return the Query produced by this method.
   *  
   */
  
  private Query createQuery()
  {
    QueryNode myNode;
    QueryRow row;
    Vector qNodes;
    Query result = null;

    /* -- */

    // If showAllItems is true, then we need to show everything.

    if (showAllItems)
      {
	return new Query((String)baseChoice.getSelectedItem(), null, editOnly);
      }

    qNodes = new Vector();

    for (int i = 0; i < Rows.size(); i++)
      {
	row = (QueryRow) Rows.elementAt(i);

	qNodes.addElement(row.getQueryNode());
      }

    myNode = (QueryNode) qNodes.elementAt(0);

    for (int i = 1; i < qNodes.size(); i++)
      {
	myNode = new QueryAndNode(myNode, (QueryNode) qNodes.elementAt(i));
      }

    result = new Query(selectedBase.getName(), myNode, editOnly);

    return result;
  }

  /**
   *
   * This method sets what fields should be returned by the Query.  Note
   * that this should only be called if the user has explicitly requested
   * a non-standard list of return fields, as the server will automatically
   * hide a bunch of undesired fields if we have not called addField() on
   * a newly constructed Query object.
   *
   */
  
  public Query setFields(Query someQuery)
  {
    FieldTemplate tempField;
    String tempString;
    Vector fieldsToReturn = null;

    /* -- */

    if (optionsPanel != null)
      {
	fieldsToReturn = optionsPanel.getReturnFields();
      }
    
    if (fieldsToReturn == null)
      {
	return someQuery;
      }

    for (int i = 0; i < fieldsToReturn.size(); i++)
      {
	tempString = (String) fieldsToReturn.elementAt(i);
	tempField = getTemplateFromName(tempString);
	
	someQuery.addField(tempField.getID());
      }

    return someQuery;
  }

  /**
   *
   * This is the standard ActionListener callback method.  This method
   * catches events from the various buttons used by querybox.
   *
   * @see java.awt.event.ActionListener
   * 
   */
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == OkButton) 
      {
	query = createQuery();
	query = setFields(query);
	query.setFiltered(true); // filter against the owner list filters

	unregister();
	setVisible(false);	// close down
	doQuery();
      } 
    
    if (e.getSource() == CancelButton)
      {
	if (debug)
	  {
	    System.out.println("Cancel was pushed");
	  }

	query = null;
	unregister();
	setVisible(false);
      } 

    if (e.getSource() == addButton)
      {
	addRow();
	// If delete button is disabled b/c there's only one row,
	// enable it 'cause there is now something to delete.
	if (!removeButton.isEnabled()) {
	  removeButton.setEnabled(true);
	}
      }
    
    if (e.getSource() == removeButton)
      {
	if (Rows.size() <= 1)
	  {
	    // need some sort of gui notify here
	    System.out.println("Error: cannot remove any more rows");
	  }  
	else
	  {
	    removeRow();
	    // If only one row left then disable delete button
	    if (Rows.size() <= 1) {
	      removeButton.setEnabled(false);
	    }
	  }
      }

    // if the user hit enter in one of the operand components that we have
    // registered ourselves for, submit the query

    if (e.getSource() instanceof JstringField ||
	e.getSource() instanceof JnumberField ||
	e.getSource() instanceof JfloatField)
      {
	OkButton.doClick();
      }
  }

  private void doQuery()
  {
    if (query == null)
      {
	return;
      }

    Thread t = new Thread(new Runnable() {
      public void run() {

	final Runnable runnableKey = this;

	EventQueue.invokeLater(new Runnable() {
	  public void run() {
	    gc.wp.addWaitWindow(runnableKey);
	  }
	});

	DumpResult buffer = null;
		
	try
	  {
	    try
	      {
		buffer = gc.session.dump(query);
	      }
	    catch (Exception ex)
	      {
		gc.processExceptionRethrow(ex);
	      }
	    catch (Error ex)
	      {
		// "Could not complete query.  Possible memory exhaustion problem.\n\n{0}"
		new JErrorDialog(gc,
				 ts.l("doQuery.error_caught",
				      ex.getMessage()));
		throw ex;
	      }
	    
	    final DumpResult bufferRef = buffer;

	    EventQueue.invokeLater(new Runnable() {
	      public void run() {
		gc.wp.addTableWindow(gc.session, query, bufferRef);
	      }
	    });
	  }
	finally
	  {
	    EventQueue.invokeLater(new Runnable() {
	      public void run() {
		gc.wp.removeWaitWindow(runnableKey);
	      }
	    });
	  }
      }});

    t.setPriority(Thread.NORM_PRIORITY);
    t.start();
  }

  /**
   *
   * This method makes sure that any JdateField's contained in
   * the querybox pop down their calendar dialogs.
   *
   */

  private synchronized void unregister()
  {
    for (int i = 0; i < Rows.size(); i++)
      {
	QueryRow row = (QueryRow) Rows.elementAt(i);
	
	if ((row.operand != null) &&
	    (row.operand instanceof JdateField))
	  {
	    ((JdateField) row.operand).unregister();
	  }
      }
  }

  private void removeRow()
  {
    QueryRow row = (QueryRow) Rows.lastElement();
    row.removeRow();
    Rows.removeElementAt(Rows.size()-1);
    inner_choice.revalidate();
  }

  private void addRow()
  {
    Rows.addElement(new QueryRow(inner_choice, this));
  }

  /**
   *
   * This is the standard ItemListener callback method.  This method
   * catches events from Checkboxes and various choice components.
   * 
   * @see java.awt.event.ItemListener
   *
   */
  
  public void itemStateChanged(ItemEvent e)
  {
    /* -- */

    if (e.getSource() == editBox)
      {
	this.editOnly = editBox.isSelected();

	if (debug)
	  {
	    System.out.println("Edit Box Clicked: " + editOnly);
	  }
      }

    else if (e.getSource() == allBox)
      {
	this.showAllItems = allBox.isSelected();

	if (debug)
	  {
	    System.out.println("Show all items is selected: " + allBox.isSelected());
	  }

	if (showAllItems)
	  {
	    if (debug)
	      {
		System.out.println("Showing blank");
	      }

	    card_layout.show(card_panel, "blank");
	    query_Buttons.setVisible(false);
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("Showing main.");
	      }

	    card_layout.show(card_panel, "main");
	    query_Buttons.setVisible(true);
	  }
	getContentPane().invalidate();
	getContentPane().validate();
      }
    else if (e.getSource() == baseChoice)
      {
	if (debug)
	  {
	    System.out.println("Base selected");
	  }

	selectBase(getBaseFromName((String) baseChoice.getSelectedItem()));
      }
  }

  /**
   * This method causes the query box to present itself for
   * queries on the given object type.
   */

  public void selectBase(BaseDump base)
  {
    if (base == null)
      {
	throw new IllegalArgumentException();
      }

    if (selectedBase == base)
      {
	return;
      }

    selectedBase = base;

    mapBaseNamesToTemplates(selectedBase.getTypeID());
    
    // update the fieldChoices vector
    
    resetFieldChoices();

    // remove all rows in vector of component arrays
    
    while (Rows.size() > 0)
      {
	removeRow();
      }
    
    addRow();

    // select the right item

    baseChoice.removeItemListener(this);

    baseChoice.setSelectedItem(selectedBase.getName());

    baseChoice.addItemListener(this);
  }

  // ***
  //
  // private convenience methods
  //
  // ***

  // we have a map from base name to base id

  /**
   *
   * This method maps the name of a (possibly embedded)
   * field to the Short id of the Base that it
   * belongs to.<br><br>
   *
   * This is used to support embedded fields.. as
   * getEmbedded() recurses down through the
   * embedded base hierarchy under selectedBase,
   * it records the Base for each embedded field
   * as it goes along creating names for the
   * embedded fields.
   *
   */

  private void mapNameToId(String name, Short id)
  {
    if (id != null)
      {
	baseIDHash.put(name, id);
      }
  }

  /**
   *
   * This method returns the Short id of the Base
   * that corresponds to the field with name
   * &lt;name&gt;.<br><br>
   *
   * This is used to support embedded fields.. as
   * getEmbedded() recurses down through the
   * embedded base hierarchy under selectedBase,
   * it records the Base for each embedded field
   * as it goes along creating names for the
   * embedded fields.
   *
   */

  Short getIdFromName(String name)
  {
    return (Short) baseIDHash.get(name);
  }

  private void mapBaseNamesToTemplates(short id)
  {
    FieldTemplate template;

    /* -- */

    fields = gc.getTemplateVector(id);
    
    if (fields != null)
      {
	fieldHash.clear();

	for (int i = 0; i < fields.size(); i++)
	  {
	    template = (FieldTemplate) fields.elementAt(i);
	    mapNameToTemplate(template.getName(), template);
	  }
      }
  }

  // we have a map from fieldname to field template

  void mapNameToTemplate(String name, FieldTemplate template)
  {
    fieldHash.put(name, template);
  }

  FieldTemplate getTemplateFromName(String name)
  {
    return (FieldTemplate) fieldHash.get(name);
  }

  // we have a map from embedded fieldname (with slashes) to the name
  // template after the last slash

  void mapEmbeddedToField(String name, String fieldName)
  {
    nameHash.put(name, fieldName);
  }

  String getFieldFromEmbedded(String name)
  {
    return (String) nameHash.get(name);
  }

  // we have a map from base names to Base

  void mapNameToBase(String name, BaseDump base)
  {
    myHash.put(name, base);
  }

  BaseDump getBaseFromName(String name)
  {
    return (BaseDump) myHash.get(name);
  }

  BaseDump getBaseFromShort(Short id)
  {
    return (BaseDump) gc.getBaseMap().get(id);
  }

  BaseDump getBaseFromShort(short id)
  {
    return (BaseDump) gc.getBaseMap().get(new Short(id));
  }
}

/*------------------------------------------------------------------------------
                                                                           class 
                                                                        QueryRow

------------------------------------------------------------------------------*/

class QueryRow implements ItemListener {

  static final boolean debug = false;

  // ---

  querybox parent;
  JPanel panel;
  FieldTemplate field = null;

  Vector fields;		// FieldTemplate Vector for the selectedBase

  JComboBox
    fieldChoice = new JComboBox(),
    boolChoice = new JComboBox(),
    compareChoice = new JComboBox();

  JPanel operandContainer= new JPanel();
  JComponent operand = null;

  String fieldName;

  boolean showDoes = false;

  /* -- */

  QueryRow(JPanel panel, querybox parent)
  {
    this.panel = panel;
    this.parent = parent;

    try
      {
	fields = parent.gc.getTemplateVector(parent.selectedBase.getTypeID());
	resetFieldChoices();
      }
    catch (Exception ex)
      {
	parent.gc.processExceptionRethrow(ex, "Remote exception in QueryRow constructor: ");
      }

    GridBagConstraints gbc = parent.gbc;
    GridBagLayout gbl = parent.gbl;

    gbc.anchor = gbc.WEST;
    gbc.gridy = parent.Rows.size();
    gbc.gridx = gbc.RELATIVE;
    gbc.gridheight = 1;
    gbc.gridwidth = 1;

    fieldChoice.setKeySelectionManager(new TimedKeySelectionManager());

    gbl.setConstraints(fieldChoice, gbc);
    panel.add(fieldChoice);

    boolChoice.setKeySelectionManager(new TimedKeySelectionManager());

    gbl.setConstraints(boolChoice, gbc);
    panel.add(boolChoice);

    compareChoice.setKeySelectionManager(new TimedKeySelectionManager());

    gbl.setConstraints(compareChoice, gbc);
    panel.add(compareChoice);

    // we have to wrap the operand component in a container so that
    // we can change the operand component later

    operandContainer.setOpaque(false);
    operandContainer.add(operand);

    gbl.setConstraints(operandContainer, gbc);
    panel.add(operandContainer);
  }

  /**
   *
   * Internal method to return a choice menu containing the fields for
   * a particular base
   *
   */
    
  private void resetFieldChoices() throws RemoteException
  {
    // we don't want to be bothered while we configure our components

    fieldChoice.removeItemListener(this);

    // ok, refresh fieldChoice

    if (fieldChoice.getItemCount() > 0)
      {
	try
	  {
	    fieldChoice.removeAllItems();
	  }
	catch (IndexOutOfBoundsException e)
	  {
	    System.out.println("IndexOfOutBounds: " + e);
	  }
      }

    // we want to be able to allow the user to search on fields in
    // embedded objects

    for (int i = 0; i < parent.fieldChoices.size(); i++)
      {
	if (debug)
	  {
	    System.err.println("QueryRow: adding field choice <" + i + ">:" + 
			       parent.fieldChoices.elementAt(i));
	  }

	fieldChoice.addItem(parent.fieldChoices.elementAt(i));
      }

    // now, what field wound up being shown?

    String fieldName = (String) fieldChoice.getSelectedItem();
    FieldTemplate field = parent.getTemplateFromName(fieldName);

    setField(field, fieldName);

    fieldChoice.addItemListener(this);
  }

  /**
   *
   * This method takes care of matters when we change or set our
   * field combo box.  Note that we don't set the fieldChoice
   * contents here, as we assume it will be done by the user
   * or by resetFieldChoices().
   *
   */

  void setField(FieldTemplate field, String fieldName)
  {
    // ok, now update our is/is not, comparator, and operand fields

    this.field = field;

    if (fieldName == null)
      {
	this.fieldName = field.getName();
      }
    else
      {
	this.fieldName = fieldName;
      }

    if (debug)
      {
	System.err.println("QueryRow.setField(" + fieldName + ")");
      }

    resetCompare(field);
    resetBoolean(field, (String) compareChoice.getSelectedItem());
    resetOperand(field, (String) compareChoice.getSelectedItem());
    panel.revalidate();
  }

  /**
   *
   * This method sets up the boolean choice combobox.
   *
   */

  void resetBoolean(FieldTemplate field, String opName)
  {
    boolean does;

    /* -- */

    if (debug)
      {
	System.err.println("QueryRow.resetBoolean(" + field.getName() + ", " + opName + ")");
      }

    // don't show us changing it

    // Do we have a phrase in the operator box which requires does
    // instead of is?

    does = opName.equalsIgnoreCase("Start With") ||
      opName.equalsIgnoreCase("End With") ||
      opName.equalsIgnoreCase("Contain") ||
      opName.equalsIgnoreCase("Contain Matching") ||
      opName.equalsIgnoreCase("Contain Matching [Case Insensitive]");

    if (does && (!showDoes || boolChoice.getItemCount() == 0))
      {
	boolChoice.setVisible(false);

	if (boolChoice.getItemCount() > 0)
	  {
	    boolChoice.removeAllItems();
	  }

	boolChoice.addItem("does");
	boolChoice.addItem("does not");

	boolChoice.setVisible(true);
	showDoes = true;
      }
    else if (!does && (showDoes || boolChoice.getItemCount() == 0))
      {
	boolChoice.setVisible(false);
	if (boolChoice.getItemCount() > 0)
	  {
	    boolChoice.removeAllItems();
	  }

	boolChoice.addItem("is");
	boolChoice.addItem("is not");

	boolChoice.setVisible(true);
	showDoes = false;
      }
  }

  /**
   *
   * This method sets up the comparison operator combobox.
   *
   */

  void resetCompare(FieldTemplate field)
  {
    if (debug)
      {
	System.err.println("QueryRow.resetCompare(" + field.getName() + ")");
      }

    compareChoice.removeItemListener(this);

    // don't show us changing it

    compareChoice.setVisible(false);
    
    if (compareChoice.getItemCount() > 0)
      {
	compareChoice.removeAllItems();
      }

    if (field.isEditInPlace())
      {
	compareChoice.addItem("Length <");
	compareChoice.addItem("Length >");
	compareChoice.addItem("Length ==");
      }
    else if (field.isArray())
      {
	compareChoice.addItem("Contain");
	
	if (field.isString() || field.isInvid() || field.isIP())
	  {
	    compareChoice.addItem("Contain Matching [Case Insensitive]");
	    compareChoice.addItem("Contain Matching");
	  }

	compareChoice.addItem("Length <");
	compareChoice.addItem("Length >");
	compareChoice.addItem("Length ==");
      }
    else if (field.isDate())
      {
	compareChoice.addItem("Before");
	compareChoice.addItem("After");
	compareChoice.addItem("Same Day As");
	compareChoice.addItem("Same Week As");
	compareChoice.addItem("Same Month As");
      }
    else if (field.isNumeric() || field.isFloat())
      {
	compareChoice.addItem("==");
	compareChoice.addItem("<");
	compareChoice.addItem(">");
	compareChoice.addItem("<=");
	compareChoice.addItem(">=");
      }
    else if (field.isBoolean())
      {
	compareChoice.addItem("==");
      }
    else if (field.isIP())
      {
	compareChoice.addItem("==");
	compareChoice.addItem("matching [Case Insensitive]");
	compareChoice.addItem("matching");
	compareChoice.addItem("Start With");
	compareChoice.addItem("End With");
      }
    else if (field.isString() || field.isInvid())
      {
	compareChoice.addItem("matching [Case Insensitive]");
	compareChoice.addItem("matching");
	compareChoice.addItem("==");
	compareChoice.addItem("== [Case Insensitive]");
	compareChoice.addItem("<");
	compareChoice.addItem(">");
	compareChoice.addItem("<=");
	compareChoice.addItem(">=");
	compareChoice.addItem("Start With");
	compareChoice.addItem("End With");
      }

    compareChoice.addItem("Defined");
    compareChoice.setVisible(true);
    compareChoice.addItemListener(this);
  }

  /**
   *
   * This method sets up the operand GUI component.
   *
   */

  void resetOperand(FieldTemplate field, String opName)
  {
    boolean addOperand = false;

    /* -- */

    if (debug)
      {
	System.err.println("QueryRow.resetOperand(" + field.getName() + ", " + opName + ")");
      }

    // when we test for defined, we won't have an operand value

    if (opName.equals("Defined"))
      {
	removeOperand();
	return;
      }

    if (opName.startsWith("Length"))
      {
	if (!(operand instanceof JnumberField))
	  {
	    removeOperand();

	    operand = new JnumberField();
	    ((JnumberField) operand).setEnterHandler(parent);
	    addOperand = true;
	  }
      }
    else if (field.isDate())
      {
	if (!(operand instanceof JdateField))
	  {
	    removeOperand();

	    operand = new JdateField(new Date(), true, false, null, null);
	    addOperand = true;
	  }
      }
    else if (field.isString())
      {
	if (!(operand instanceof JstringField))
	  {
	    removeOperand();

	    operand = new JstringField();
	    ((JstringField) operand).setEnterHandler(parent);
	    addOperand = true;
	  }
      }
    else if (field.isNumeric())
      {
	if (!(operand instanceof JnumberField))
	  {
	    removeOperand();

	    operand = new JnumberField();
	    ((JnumberField) operand).setEnterHandler(parent);
	    addOperand = true;
	  }
      }
    else if (field.isFloat())
      {
 	if (!(operand instanceof JfloatField))
 	  {
	    removeOperand();

 	    operand = new JfloatField();
	    ((JfloatField) operand).setEnterHandler(parent);
 	    addOperand = true;
 	  }
      }
    else if (field.isBoolean())
      {
	if (!(operand instanceof JCheckBox))
	  {
	    removeOperand();

	    operand = new JCheckBox();
	    addOperand = true;
	  }
      }
    else if (field.isInvid())
      {
	short targetBase = field.getTargetBase();

	// right now we have this commented out because we don't want
	// to always force the JInvidChooser.. it will often take
	// longer to use the chooser to select a user name (for
	// instance), not to mention the query time to get the list
	// from the server. thankfully, the server can use either a
	// label string or an invid for invid fields

	// in the long run, i'm not sure what to do here.. use the
	// chooser sometimes, but not others based on some criteria?

	if (true /* (targetBase < 0) */)
	  {
	    if (!(operand instanceof JstringField))
	      {
		removeOperand();

		operand= new JstringField();
		((JstringField) operand).setEnterHandler(parent);
		addOperand = true;
	      }
	  }
	else
	  {
	    Short Target = new Short(targetBase);
	    objectList list;
	    JInvidChooser invidChooser;
	    Vector choices;

	    /* -- */

	    // we always want to reset the invid chooser

	    removeOperand();

	    // get a fully expanded (non-editables included) list of objects
	    // from our parent.

	    list = parent.gc.getObjectList(Target, true);

	    choices = list.getListHandles(false, true); // no inactives, but do want non-editables
	    choices = parent.gc.sortListHandleVector(choices);

	    operand = invidChooser = new JInvidChooser(choices, null, targetBase);

	    addOperand = true;

	    invidChooser.setMaximumRowCount(12);
	    invidChooser.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
	    invidChooser.setEditable(false);
	  }
      }
    else if (field.isIP())
      {
	if (!(operand instanceof JstringField))
	  {
	    removeOperand();
	    
	    operand = new JstringField();
	    ((JstringField) operand).setEnterHandler(parent);
	    addOperand = true;
	  }
      }

    if (operand == null)
      {
	throw new NullPointerException("null operand");
      }

    if (addOperand)
      {
	operandContainer.add(operand);
	operand.setVisible(true);
      }
  }

  /**
   * Private helper method to remove the operand component and
   * handle any unregistration required.
   */

  private void removeOperand()
  {
    if (operand != null)
      {
	if (operand instanceof JdateField)
	  {
	    ((JdateField) operand).unregister();
	  }

	if (operand instanceof JstringField)
	  {
	    ((JstringField) operand).setEnterHandler(null);
	  }

	if (operand instanceof JnumberField)
	  {
	    ((JnumberField) operand).setEnterHandler(null);
	  }

	if (operand instanceof JfloatField)
	  {
	    ((JfloatField) operand).setEnterHandler(null);
	  }
	
	operand.setVisible(false);
	operandContainer.remove(operand);
	operand = null;
      }
  }

  /**
   *
   * This method is called when the querybox wants to remove this row.
   * This method takes care of removing all components from panel, but
   * does not take care of removing itself from the querybox Rows Vector.
   * */

  void removeRow()
  {
    panel.remove(fieldChoice);
    panel.remove(boolChoice);
    panel.remove(compareChoice);

    if (operand instanceof JdateField)
      {
	// pop down a popped up calendar

	((JdateField) operand).unregister();
      }

    panel.remove(operandContainer);
  }

  /**
   *
   * This method returns a reference to the Base that this QueryRow
   * is set to search on.  The Base that an individual QueryRow is
   * set to search on may differ from the selectedBase in parent
   * because we allow searches on fields contained in embedded
   * objects.
   * 
   */

  public Base getBase()
  {
    if (debug)
      {
	System.err.println("querybox: getBase(): fieldName = " + fieldName);
	System.err.println("Id = " + parent.getIdFromName(fieldName));
      }

    return parent.getBaseFromShort(parent.getIdFromName(fieldName));
  }

  /**
   *
   * This method returns a QueryNode corresponding to the current
   * configuration of this QueryRow.
   * 
   */

  public QueryNode getQueryNode()
  {
    QueryDeRefNode rootNode = null;
    QueryDeRefNode deRefNode = null;
    QueryNode terminalNode = null;

    Object value = null;
    String localFieldName = parent.getFieldFromEmbedded(fieldName);
    String pathElements[] = null;

    /* -- */

    // first we need to see if we are working with an embedded field..

    // XXX this is based on the presumption (currently enforced by
    // DBObjectBaseField.setName()) that a field cannot have slashes
    // (/'s) in its name.

    if (this.fieldName.indexOf('/') > -1)
      {
	pathElements = StringUtils.split(this.fieldName, "/");

	for (int i = 0; i < (pathElements.length-1); i++)
	  {
	    if (rootNode == null)
	      {
		rootNode = new QueryDeRefNode(pathElements[i], null);
		deRefNode = rootNode;
	      }
	    else
	      {
		QueryDeRefNode nextLinkNode = new QueryDeRefNode(pathElements[i], null);
		deRefNode.queryTree = nextLinkNode;
		deRefNode = nextLinkNode;
	      }
	  }
      }

    if (operand instanceof JnumberField)
      {
	JnumberField numField = (JnumberField) operand;
	value = numField.getValue(); // Integer
      }
    else if (operand instanceof JfloatField)
      {
	JfloatField floatField = (JfloatField) operand;
	value = floatField.getValue(); // Double
      }
    else if (operand instanceof JdateField)
      {
	JdateField dateField = (JdateField) operand;
	value = dateField.getDate(); // java.util.Date
      }
    else if (operand instanceof JCheckBox)
      {
	JCheckBox boolField = (JCheckBox) operand;
	value = new Boolean(boolField.isSelected());
      }
    else if (operand instanceof JstringField)
      { 
	JstringField stringField = (JstringField) operand;
	value = stringField.getValue();

	String strValue = (String) value;

	if (field.isIP())
	  {
	    String opName = (String) compareChoice.getSelectedItem();

	    // we only do a string operation if our operator is
	    // "matching" or "matching [Case Insensitive]", otherwise
	    // we'll send a binary array of Byte objects up to the
	    // server for the IP match.

	    if (!opName.equals("matching") && 
		!opName.equals("matching [Case Insensitive]") &&
		!opName.equals("Contain Matching [Case Insensitive]") &&
		!opName.equals("Contain Matching"))
	      {
		if (strValue.indexOf(':') != -1)
		  {
		    try
		      {
			value = JIPField.genIPV6bytes(strValue);
		      }
		    catch (IllegalArgumentException ex)
		      {
		      }
		  }
		else
		  {
		    try
		      {
			value = JIPField.genIPV4bytes(strValue);
		      }
		    catch (IllegalArgumentException ex)
		      {
		      }
		  }
	      }
	  }
      }
    else if (operand instanceof JInvidChooser)
      {
	JInvidChooser invidChooser = (JInvidChooser) operand;
	value = invidChooser.getSelectedInvid();
      }
    else if (operand != null)
      {
	System.err.println("Couldn't get a value.. unknown operand type! " + operand.getClass().toString());
      }

    String operator = (String) compareChoice.getSelectedItem();
    byte opValue = QueryDataNode.NONE;
    byte arrayOp = QueryDataNode.NONE;
    
    if (field.isArray())
      {
	if (operator.equals("Contain"))
	  {
	    opValue = QueryDataNode.EQUALS;
	    arrayOp = QueryDataNode.CONTAINS;
	  }
	else if (operator.equals("Contain Matching"))
	  {
	    opValue = QueryDataNode.MATCHES;
	    arrayOp = QueryDataNode.CONTAINS;
	  }
	else if (operator.equals("Contain Matching [Case Insensitive]"))
	  {
	    opValue = QueryDataNode.NOCASEMATCHES;
	    arrayOp = QueryDataNode.CONTAINS;
	  }
	else if (operator.equals("Length =="))
	  {
	    arrayOp = QueryDataNode.LENGTHEQ;
	  } 
	else if (operator.equals("Length >"))
	  {
	    arrayOp = QueryDataNode.LENGTHGR;
	  } 
	else if (operator.equals("Length <"))
	  {
	    arrayOp = QueryDataNode.LENGTHLE;
	  }
	else if (operator.equals("Defined"))
	  {
	    opValue = QueryDataNode.DEFINED;
	  }

	if (opValue == QueryDataNode.NONE && arrayOp == QueryDataNode.NONE)
	  {
	    System.err.println("QueryRow.getQueryNode(): Unknown array comparator");
	    return null;
	  }

	terminalNode = new QueryDataNode(localFieldName, opValue, arrayOp, value);

	if (debug)
	  {
	    System.err.println("QueryDataNode: " + terminalNode.toString());
	  }
	    
	// -- if not is true then add a not node
	    
	if (isNot())
	  {
	    terminalNode = new QueryNotNode(terminalNode); // if NOT then add NOT node
	  } 

	if (deRefNode != null)
	  {
	    deRefNode.queryTree = terminalNode;
	    return rootNode;
	  }
	else
	  {
	    return terminalNode;
	  }
      }
    else if (!operator.startsWith("Same"))
       {
	// ok, normal scalar field, not a time window comparison

	if (operator.equals("=="))
	  {
	    opValue = QueryDataNode.EQUALS;
	  } 
	else if (operator.equals("<") || operator.equals("Before"))
	  {
	    opValue = QueryDataNode.LESS;
	  } 
	else if (operator.equals("<="))
	  {
	    opValue = QueryDataNode.LESSEQ;
	  } 
	else if (operator.equals(">") || operator.equals("After"))
	  {
	    opValue = QueryDataNode.GREAT;
	  } 
	else if (operator.equals(">="))
	  {
	    opValue = QueryDataNode.GREATEQ;
	  } 
	else if (operator.equals("== [Case Insensitive]"))
	  {
	    opValue = QueryDataNode.NOCASEEQ;
	  }
	else if (operator.equals("Start With"))
	  {
	    opValue = QueryDataNode.STARTSWITH;
	  }
	else if (operator.equals("End With"))
	  {
	    opValue = QueryDataNode.ENDSWITH;
	  }
	else if (operator.equals("Defined"))
	  {
	    opValue = QueryDataNode.DEFINED;
	  }
	else if (operator.equals("matching"))
	  {
	    opValue = QueryDataNode.MATCHES;
	  }
	else if (operator.equals("matching [Case Insensitive]"))
	  {
	    opValue = QueryDataNode.NOCASEMATCHES;
	  }

	if (opValue == 0)
	  {
	    System.err.println("QueryRow.getQueryNode(): Unknown scalar comparator");
	    return null;
	  }
	    
	terminalNode = new QueryDataNode(localFieldName, opValue, value);

	if (debug)
	  {
	    System.err.println("QueryDataNode: " + terminalNode.toString());
	  }
	    
	// -- if not is true then add a not node
	    
	if (isNot())
	  {
	    terminalNode = new QueryNotNode(terminalNode); // if NOT then add NOT node
	  } 

	if (deRefNode != null)
	  {
	    deRefNode.queryTree = terminalNode;
	    return rootNode;
	  }
	else
	  {
	    return terminalNode;
	  }
      }
    else
      {
	if (!(value instanceof Date))
	  {
	    System.err.println("QueryRow.getQueryNode(): Don't have a proper date value");
	    return null;
	  }

	Date
	  lowDate,
	  hiDate,
	  dateValue = (Date) value;

	Calendar cal = Calendar.getInstance();

	cal.setTime(dateValue);

	cal.set(Calendar.HOUR, 0);
	cal.set(Calendar.MINUTE, 0);
	cal.set(Calendar.SECOND, 0);
	cal.set(Calendar.MILLISECOND, 0);

	if (operator.equals("Same Day As"))
	  {
	    lowDate = cal.getTime();

	    cal.roll(Calendar.DATE, true);

	    hiDate = cal.getTime();
	  }
	else if (operator.equals("Same Week As"))
	  {
	    cal.set(Calendar.DAY_OF_WEEK, 0);
	    lowDate = cal.getTime();
		
	    cal.roll(Calendar.WEEK_OF_YEAR, true);

	    hiDate = cal.getTime();
	  }
	else if (operator.equals("Same Month As"))
	  {
	    cal.set(Calendar.DAY_OF_MONTH, 0);
	    lowDate = cal.getTime();
		
	    cal.roll(Calendar.MONTH, true);

	    hiDate = cal.getTime();
	  }
	else
	  {
	    System.err.println("QueryRow.getQueryNode(): Don't have a proper date comparator");
	    return null;
	  }

	terminalNode = new QueryAndNode(new QueryDataNode(localFieldName, QueryDataNode.GREATEQ, lowDate),
					new QueryDataNode(localFieldName, QueryDataNode.LESS, hiDate));
	    
	// -- if not is true then add a not node
	    
	if (isNot())
	  {
	    terminalNode = new QueryNotNode(terminalNode); // if NOT then add NOT node
	  } 
	    
	if (deRefNode != null)
	  {
	    deRefNode.queryTree = terminalNode;
	    return rootNode;
	  }
	else
	  {
	    return terminalNode;
	  }
      }
  }

  /**
   *
   * @return true if this QueryRow negates the basic comparison
   *
   */

  private boolean isNot()
  {
    return (boolChoice.getSelectedItem().equals("is not") ||
	    boolChoice.getSelectedItem().equals("does not"));
  }

  /**
   *
   * This is the standard ItemListener callback method.  This method
   * catches events from Checkboxes and various choice components.
   *
   * @see java.awt.event.ItemListener
   * 
   */
  
  public void itemStateChanged(ItemEvent e)
  {
    // we want to ignore deselect events

    if (e.getStateChange() == e.DESELECTED)
      {
	return;
      }

    if (e.getSource() == fieldChoice)
      {
	setField(parent.getTemplateFromName((String) fieldChoice.getSelectedItem()), 
		 (String) fieldChoice.getSelectedItem());
      }
    else if (e.getSource() == compareChoice)
      {
	String compareOperator = (String) compareChoice.getSelectedItem();
	resetBoolean(field, compareOperator);
	resetOperand(field, compareOperator);
	panel.revalidate();
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class 
                                                                    OptionsFrame

------------------------------------------------------------------------------*/

class OptionsPanel extends JPanel {

  static final boolean debug = false;

  // ---

  querybox parent;

  JPanel builtInPanel = new JPanel();
  JPanel customPanel = new JPanel();

  StringSelector builtInSelector, 
		 customSelector;

  int numBuiltInChoices,
      numCustomChoices;

  /* -- */

  /**
   *
   * This internal method is used to create a frame which will
   * present a matrix of checkboxes corresponding to the fields
   * available in the specified object base.  The user will
   * be able to select various checkboxes to control which fields
   * are to be returned by the query generated by this querybox.
   *
   */

  OptionsPanel(querybox parent)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints(); 

    /* -- */

    this.parent = parent;

    setLayout(gbl);

    gbc.fill=gbc.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;

    customPanel.setBorder(new TitledBorder(new EtchedBorder(),"Custom Fields"));
    customPanel.setLayout(new BorderLayout());

    gbc.gridx = 0;
    gbl.setConstraints(customPanel, gbc);

    add(customPanel);

    builtInPanel.setBorder(new TitledBorder(new EtchedBorder(),"Built-In Fields"));
    builtInPanel.setLayout(new BorderLayout());

    gbc.gridx = 1;
    gbl.setConstraints(builtInPanel, gbc);

    add(builtInPanel);

    resetBoxes();
  }

  /**
   *
   * This method clears out the checkboxes in the 'fields returned'
   * panel.
   * 
   */

  public void resetBoxes()
  {
    FieldTemplate template;
    Vector fields;

    Vector
      builtInItems_Vect = new Vector(),
      customItems_Vect = new Vector();

    /* -- */

    customPanel.removeAll();
    builtInPanel.removeAll();

    fields = parent.gc.getTemplateVector(parent.selectedBase.getTypeID());

    for (int i=0; fields != null && (i < fields.size()); i++) 
      {	
	template = (FieldTemplate) fields.elementAt(i);

	if (template.isBuiltIn())
	  {
	    builtInItems_Vect.addElement( template.getName() );
	  }
	else
	  {
	    customItems_Vect.addElement( template.getName() );
	  }
	
      }

    numBuiltInChoices = builtInItems_Vect.size();
    numCustomChoices = customItems_Vect.size();

    // create and load the StringSelector for the built in fields
      
    builtInSelector = new StringSelector(builtInPanel, true, true, true);
    
    Vector builtInHandles = new Vector(builtInItems_Vect.size());

    for (int i = 0; i < builtInItems_Vect.size(); i++)
      {
	String x = (String) builtInItems_Vect.elementAt(i);

	builtInHandles.addElement(new listHandle(x, x));
      }

    FixedListCompare builtInComparator = new FixedListCompare(builtInHandles, null);

    builtInSelector.update(builtInItems_Vect, true, builtInComparator, 
			   new Vector(), true, builtInComparator);

    // create and load the StringSelector for the custom fields

    customSelector = new StringSelector(customPanel, true, true, true);

    Vector customHandles = new Vector(customItems_Vect.size());

    for (int i = 0; i < customItems_Vect.size(); i++)
      {
	String x = (String) customItems_Vect.elementAt(i);

	customHandles.addElement(new listHandle(x, x));
      }

    FixedListCompare customComparator = new FixedListCompare(customHandles, null);

    customSelector.update(new Vector(), true, customComparator, 
			  customItems_Vect, true, customComparator);

    builtInPanel.add( builtInSelector, BorderLayout.CENTER );
    customPanel.add( customSelector, BorderLayout.CENTER );
  }

  /**
   *
   * This method returns a Vector of Strings indicating the fields that
   * the user has requested be returned by the query engine, or null
   * if the user has left the field checkboxes at the default settings.
   *
   */

  public Vector getReturnFields()
  {
    Vector fieldsToReturn = new Vector();

    /* -- */

    // the Vector.addAll() methods are not present in JDK 1.1, which
    // we are still supporting in the client, so we revert to manual
    // field addition here.

    Vector vectA = builtInSelector.getChosenStrings();
    Vector vectB = customSelector.getChosenStrings();

    for (int i = 0; i < vectA.size(); i++)
      {
	fieldsToReturn.addElement(vectA.elementAt(i));
      }

    for (int i = 0; i < vectB.size(); i++)
      {
	fieldsToReturn.addElement(vectB.elementAt(i));
      }

    // if we are returning all fields, we can use null to indicate that

    if ( fieldsToReturn.size() == (numBuiltInChoices + numCustomChoices) ) 
	fieldsToReturn = null; 

    return fieldsToReturn; 
  }
}
