/*

    containerPanel.java

    This is the container for all the information in a field.  Used in window Panels.

    Created:  11 August 1997
    Version: $Revision: 1.34 $ %D%
    Module By: Michael Mulvaney
    Applied Research Laboratories, The University of Texas at Austin

*/
package arlut.csd.ganymede.client;

//import tablelayout.*;
import com.sun.java.swing.*;
import com.sun.java.swing.event.*;


import java.awt.*;
import java.beans.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  containerPanel

------------------------------------------------------------------------------*/

public class containerPanel extends JPanel implements ActionListener, JsetValueCallback, ItemListener{  

  static final boolean debug = true;

  // -- 
  
  gclient
    gc;			// our interface to the server

  db_object
    object;			// the object we're editing
  
  windowPanel
    winP;			// for interacting with our containing context

  protected framePanel
    frame;

  Vector
    vectorPanelList = new Vector();

  Hashtable
    rowHash, 
    objectHash;
  
  //  TableLayout 
  // layout;

  GridBagLayout
    gbl;
  
  GridBagConstraints
    gbc;
  
  JViewport
    vp;

  Vector 
    infoVector = null,
    templates = null;

  int row = 0;			// we'll use this to keep track of rows added as we go along

  boolean
    editable;

  JProgressBar
    progressBar;

  boolean
    isEmbedded,
    loaded = false;

  short 
    type;

  /* -- */

  /**
   *
   * Constructor for containerPanel
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param parent   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   *
   */
  public containerPanel(db_object object, boolean editable, gclient gc, windowPanel window, framePanel frame)
  {
    this(object, editable, gc, window, frame, null, true);
  }

  /**
   *
   * Constructor for containerPanel
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param parent   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param progressBar JProgressBar to be updated, can be null
   */
  public containerPanel(db_object object, boolean editable, gclient gc, windowPanel window, framePanel frame, JProgressBar progressBar)
  {
    this(object, editable, gc, window, frame, progressBar, true);
  }

  /**
   *
   * Main constructor for containerPanel
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param parent   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param progressBar JProgressBar to be updated, can be null
   * @param loadNow  If true, container panel will be loaded immediately
   *
   */

  public containerPanel(db_object object, boolean editable, gclient gc, windowPanel window, framePanel frame, JProgressBar progressBar, boolean loadNow)
  {
    super(false);
    /* -- */

    this.gc = gc;

    if (object == null)
      {
	System.err.println("null object passed to containerPanel");
	setStatus("Could not get object.  Someone else might be editting it.  Try again at a later time.");
	return;
      }

    this.winP = window;
    this.object = object;
    this.editable = editable;
    this.frame = frame;
    this.progressBar = progressBar;

    if (loadNow)
      {
	load();
      }
  }
  
  public void load() 
  {
    if (loaded)
      {
	System.out.println("Container panel is already loaded!");
	return;
      }
    if (debug)
      {
	System.out.println("Loading container panel");
      }

    objectHash = new Hashtable();
    rowHash = new Hashtable();

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();
    
    setLayout(gbl);
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.insets = new Insets(4,4,4,4);
    
    if (progressBar != null)
      {
	progressBar.setMinimum(0);
	progressBar.setMaximum(15);
	progressBar.setValue(0);
      }
      
    // Get the list of fields

    if (debug)
      {
	System.out.println("Getting list of fields");
      }
    
    try
      {
	type = object.getTypeID();
	if (progressBar != null)
	  {
	    progressBar.setValue(1);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get the fields: " + rx);
      }

    Short Type = new Short(type);

    templates = gc.getTemplateVector(Type);

    if (progressBar != null)
      {
	progressBar.setValue(2);
      }

    try
      {
	infoVector = object.getFieldInfoVector(true);  // Just gets the custom ones
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get FieldInfoVector: " + rx);
      }

    if (progressBar != null)
      {
	progressBar.setMaximum(infoVector.size());
	progressBar.setValue(3);
      }




    if (debug)
      {
	System.out.println("Entering big loop");
      }
      
    if (templates != null)
      {
	int infoSize = infoVector.size();
	FieldInfo fieldInfo = null;
	FieldTemplate fieldTemplate = null;
	
	for (int i = 0; i < infoSize ; i++)
	  {
	    if (progressBar != null)
	      {
		progressBar.setValue(i + 4);
	      }

	    try
	      {
		// Skip some fields.  custom panels hold the built ins, and a few others.
		fieldInfo = (FieldInfo)infoVector.elementAt(i);
		// Find the template
		boolean found = false;
		int tSize = templates.size();
		for (int k = 0; k < tSize; k++)
		  {
		    fieldTemplate = (FieldTemplate)templates.elementAt(k);
		    if (fieldTemplate.getID() == fieldInfo.getID())
		      {
			found = true;
			break;
		      }
		  }
		
		if (! found)
		  {
		    throw new RuntimeException("Could not find the template for this field: " + fieldInfo.getField());
		  }

		short ID = fieldTemplate.getID();
		if (((type== SchemaConstants.OwnerBase) && (ID == SchemaConstants.OwnerObjectsOwned)) 
		    ||  (ID == SchemaConstants.BackLinksField)
		    || ((type == SchemaConstants.UserBase) && (ID == SchemaConstants.UserAdminPersonae))
		    || ((ID == SchemaConstants.ContainerField) && object.isEmbedded()))
		  {
		    if (debug)
		      {
			System.out.println("Skipping a special field: " + fieldTemplate.getName());
		      }
		  }
		else
		  {
		    //System.out.println("- number " + i + ".  tSize = " + tSize);
		    //if (i == tSize - 1)  // This is the last component
		    //  {
		    //System.out.println("setting the weighty!");
		    //gbc.weighty = 1.0;  // Make it strectch to fill up the space
		    //}
		    addFieldComponent(fieldInfo.getField(), fieldInfo, fieldTemplate);
		  }
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("caught remote exception adding field " + ex);
	      }
	  }
      }
    
    if (debug)
      {
	System.out.println("Done with loop");
      }

    //setViewportView(panel);

    setStatus("Finished loading containerPanel");

    loaded = true;
  }

  public boolean isLoaded()
  {
    return loaded;
  }

  /**
   * Goes through all the components and checks to see if they should be visible,
   * and updates their contents.
   *
   * !!! This will not currently work, because some of the components (JComboBox)
   * !!! generate events when setItem is used.  So it will send a bunch of extra
   * !!! calls.  No good!
   */

  public void update()
  {
    if (debug)
      {
	System.out.println("Updating container panel");
      }

    gc.setWaitCursor();

    Enumeration enum = objectHash.keys();

    while (enum.hasMoreElements())
      {
	Component comp = (Component)enum.nextElement();

	System.out.println("Updating: " + comp);

	try
	  {
	    db_field field = (db_field)objectHash.get(comp);
	    setRowVisible(comp, field.isVisible());

	    if (comp instanceof JstringField)
	      {
		((JstringField)comp).setText((String)field.getValue());
	      }
	    else if (comp instanceof JdateField)
	      {
		((JdateField)comp).setDate((Date)field.getValue());
	      }
	    else if (comp instanceof JnumberField)
	      {
		((JnumberField)comp).setText(((Integer)field.getValue()).toString());
	      }
	    else if (comp instanceof JcheckboxField)
	      {
		((JcheckboxField)comp).setSelected(((Boolean)field.getValue()).booleanValue());
	      }
	    else if (comp instanceof JCheckBox)
	      {
		((JCheckBox)comp).setSelected(((Boolean)field.getValue()).booleanValue());
	      }
	    else if (comp instanceof JComboBox)
	      {
		Object o = field.getValue();
		if (o instanceof String)
		  {
		    Vector choiceHandles = null;
		    Vector choices = null;
		    string_field s_field = (string_field)field;

		    Object key = s_field.choicesKey();
		    if (key == null)
		      {
			if (debug)
			  {
			    System.out.println("key is null, getting new copy.");
			  }
			choices = s_field.choices().getLabels();
		      }
		    else
		      {
			if (debug)
			  {
			    System.out.println("key = " + key);
			  }
		
			if (gc.cachedLists.containsKey(key))
			  {
			    if (debug)
			      {
				System.out.println("key in there, using cached list");
			      }
			    choiceHandles = (Vector)gc.cachedLists.get(key);

			  }
			else
			  {
			    if (debug)
			      {
				System.out.println("It's not in there, downloading a new one.");
			      }
			    choiceHandles = s_field.choices().getListHandles();
			    gc.cachedLists.put(key, choiceHandles);

			  }
	    
			for (int j = 0; j < choiceHandles.size() ; j++)
			  {
			    choices.addElement(((listHandle)choiceHandles.elementAt(j)).getLabel());
			  }		
		      }    

		    JComboBox cb = (JComboBox)comp;
		    cb.removeAllItems();
		    for (int i = 0; i < choices.size(); i++)
		      {
			cb.addItem((String)choices.elementAt(i));
		      }
		    
		    cb.addItem("<none>");

		    // Do I need to check to make sure that this one is possible?
		    cb.setSelectedItem((String)o);
		  }
		else if (o instanceof Invid)
		  {
		    // Still need to rebuild list here.
		    listHandle lh = new listHandle(gc.getSession().viewObjectLabel((Invid)o), o);
		    ((JComboBox)comp).setSelectedItem(lh);
		  }
		else
		  {
		    // This might be null.  Which means we should choose <none>.  But do
		    // we choose (string)<none> or (listHandle)<none>?
		    if (field instanceof string_field)
		      {
			((JComboBox)comp).setSelectedItem("<none>");
		      }
		    else if (field instanceof invid_field)
		      {
			((JComboBox)comp).setSelectedItem(new listHandle("<none>", null));
		      }
		    else
		      {
			System.out.println("I am not expecting this type in JComboBox: " + field);
		      }
		  }
	      }
	    else if (comp instanceof JLabel)
	      {
		((JLabel)comp).setText((String)field.getValue());
	      }
	    else if (comp instanceof JpassField)
	      {
		System.out.println("Passfield, ingnoring");
	      }
	    else if (comp instanceof tStringSelector)
	      {
		System.out.println("Skipping over tStringSelector.");
	      }
	    else 
	      {
		System.err.println("field of unknown type: " + comp);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not check visibility");
	  }
      }

    if (debug)
      {
	System.out.println("Done updating container panel");
      }

    gc.setNormalCursor();
  }

  /**
   *
   * This method does causes the hierarchy of containers above
   * us to be recalculated from the bottom (us) on up.  Normally
   * the validate process works from the top-most container down,
   * which isn't what we want at all in this context.
   *
   */

  public void invalidateRight()
  {
    Component c;

    c = this;

    while ((c != null) && !(c instanceof JViewport))
      {
	System.out.println("contianer panel doLayout on " + c);

	c.doLayout();
	c = c.getParent();
      }
  }

  public boolean setValuePerformed(JValueObject v)
  {
    if (v.getOperationType() == JValueObject.ERROR)
      {
	setStatus((String)v.getValue());
	return true;
      }

    boolean returnValue = false;

    /* -- */

    if (v.getSource() instanceof JstringField)
      {
	if (debug)
	  {
	    System.out.println((String)v.getValue());
	  }
	db_field field = (db_field)objectHash.get(v.getSource());

	try
	  {
	    if (debug)
	      {
		System.out.println(field.getTypeDesc() + " trying to set to " + v.getValue());
	      }

	    if (field.setValue(v.getValue()))
	      {
		returnValue = true;
	      }
	    else
	      {
		setStatus("Change failed: " + gc.getSession().getLastError());
		returnValue = false;
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
      }
    else if (v.getSource() instanceof JpassField)
      {
	if (debug)
	  {
	    System.out.println((String)v.getValue());
	  }

	pass_field field = (pass_field)objectHash.get(v.getSource());

	try
	  {
	    if (debug)
	      {
		System.out.println(field.getTypeDesc() + " trying to set to " + v.getValue());
	      }

	    if (field.setPlainTextPass((String)v.getValue()))
	      {
		returnValue = true;
	      }
	    else
	      {
		setStatus("Change failed: " + gc.getSession().getLastError());
		returnValue =  false;
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
 
      }
    else if (v.getSource() instanceof JdateField)
      {
	if (debug)
	  {
	    System.out.println("date field changed");
	  }

	db_field field = (db_field)objectHash.get(v.getSource());

	try
	  {
	    returnValue =  field.setValue(v.getValue());
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
      }
    else if (v.getSource() instanceof vectorPanel)
      {
	System.out.println("Something happened in the vector panel");
      }
    else if (v.getSource() instanceof tStringSelector)
      {
	if (debug)
	  {
	    System.out.println("value performed from tStringSelector");
	  }
	if (v.getOperationType() == JValueObject.ERROR)
	  {
	    setStatus((String)v.getValue());
	  }
	else if (v.getValue() instanceof Invid)
	  {
	    db_field field = (db_field)objectHash.get(v.getSource());

	    if (field == null)
	      {
		throw new RuntimeException("Could not find field in objectHash");
	      }

	    Invid invid = (Invid)v.getValue();
	    int index = v.getIndex();

	    try
	      {
		if (v.getOperationType() == JValueObject.ADD)
		  {
		    if (debug)
		      {
			System.out.println("Adding new value to string selector");
		      }
		    returnValue = (field.addElement(invid));
		  }
		else if (v.getOperationType() == JValueObject.DELETE)
		  {
		    if (debug)
		      {
			System.out.println("Removing value from field(strig selector)");
		      }
		    returnValue = (field.deleteElement(invid));
		  }
		if (debug)
		  {
		    if (returnValue)
		      {
			System.out.println("returned true");
		      }
		    else
		      {
			System.out.println("returned false");
		      }
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not change owner field: " + rx);
	      }
	  }
	else if (v.getValue() instanceof String)
	  {
	    System.out.println("String tStringSelector callback, not implemented yet");
	    string_field field = (string_field)objectHash.get(v.getSource());
	    
	    if (v.getOperationType() == JValueObject.ADD)
	      {
		try
		  {
		    returnValue = field.addElement((String)v.getValue());
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not add string to string_field: " + rx);
		  }
	      }
	    else if (v.getOperationType() == JValueObject.DELETE)
	      {
		try
		  {
		    returnValue = field.deleteElement((String)v.getValue());
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not remove string from string_field: " + rx);
		  }
	      }
	  }
	else
	  {
	    System.out.println("Not an Invid in string selector.");
	  }
      }
    else if (v.getSource() instanceof JIPField)
      {
	if (debug)
	  {
	    System.out.println("ip field changed");
	  }

	db_field field = (db_field)objectHash.get(v.getSource());

	try
	  {
	    returnValue =  field.setValue(v.getValue());
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set ip field value: " + rx);
	  }
      }
    else if (v.getOperationType() == JValueObject.PARAMETER)
      {
	System.out.println("MenuItem selected in a tStringSelector");
	String command = (String)v.getParameter();

	if (command.equals("Edit object"))
	  {
	    System.out.println("Edit object: " + v.getValue());
	    if (v.getValue() instanceof listHandle)
	      {
		Invid invid = (Invid)((listHandle)v.getValue()).getObject();
		    
		gc.editObject(invid);
	      }
	    else if (v.getValue() instanceof Invid)
	      {
		System.out.println("It's an invid!");
		Invid invid = (Invid)v.getValue();
		    
		gc.viewObject(invid);
		
	      }
	    returnValue = true;
	  }
	else if (command.equals("View object"))
	  {

	    System.out.println("View object: " + v.getValue());
	    if (v.getValue() instanceof Invid)
	      {
		try
		  {
		    Invid invid = (Invid)v.getValue();
		    
		    winP.addWindow(gc.getSession().view_db_object(invid));
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not view object: " + rx);
		  }
	      }
	    returnValue = true;
	  }
	else
	  {
	    System.out.println("Unknown action command from popup: " + command);
	  }
      }
    else
      {
	System.out.println("Value performed from unknown source");
      }

    // Check to see if anything needs updating.

    try
      {
	if (object.shouldRescan())
	  {
	    update();
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not call shouldRescan(): " + rx);
      }

    System.out.println("returnValue: " + returnValue);
    
    // Only set somethingChanged to true if something Changed; never set it to false
    if (returnValue)
      {
	gc.somethingChanged = true;
      }

    return returnValue;
  }


  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() instanceof JCheckBox)
      {
	db_field field = (db_field)objectHash.get(e.getSource());
	  
	try
	  {
	      
	    if (field.setValue(new Boolean(((JCheckBox)e.getSource()).isSelected())))
	      {
		gc.somethingChanged = true;
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("Could not change checkbox, resetting it now");
		  }
		((JCheckBox)e.getSource()).setSelected(((Boolean)field.getValue()).booleanValue());
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
      }
    else
      {
	System.err.println("Unknown ActionEvent in containerPanel");
      }
    
    // Check to see if anything needs updating.

    try
      {
	if (object.shouldRescan())
	  {
	    update();
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not call shouldRescan(): " + rx);
      }
  }

  public void itemStateChanged(ItemEvent e)
  {
    if (debug)
      {
	System.out.println("Item changed: " + e.getItem());
      }

    if (e.getSource() instanceof JComboBox)
      {
	db_field field = (db_field)objectHash.get(e.getSource());

	try
	  {
	    boolean ok = false;
	    Object item = e.getItem();
	    if (item instanceof String)
	      {
		ok = field.setValue((String)e.getItem());
	      }
	    else if (item instanceof listHandle)
	      {
		ok = field.setValue(((Invid) ((listHandle)e.getItem()).getObject() ));

	      }
	    else 
	      {
		System.out.println("Unknown type from JComboBox: " + item);
	      }
	    if (ok)
	      {
		gc.somethingChanged = true;
		if (debug)
		  {
		    System.out.println("field setValue returned true");
		  }
	      }
	    else if (debug)
	      {
		System.out.println("field setValue returned FALSE!!");
	      }
	    
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not set combo box value: " + rx);
	  }
      }
    else
      {
	System.out.println("Not from a JCombobox");
      }
  }

  void addVectorRow(Component comp, String label, boolean visible)
  {

    JLabel l = new JLabel("");
    rowHash.put(comp, l);
    
    gbc.gridwidth = 2;
    gbc.gridx = 0;
    gbc.gridy = row;

    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(comp, gbc);
    add(comp);

    row++;

    //setRowVisible(comp, visible);
  }
  
  void addRow(Component comp,  String label, boolean visible)
  {
    JLabel l = new JLabel(label);
    rowHash.put(comp, l);

    //comp.setBackground(ClientColor.ComponentBG);

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;

    gbc.weightx = 0.0;
    gbc.gridx = 0;
    gbc.gridy = row;
    gbl.setConstraints(l, gbc);
    add(l);
    
    gbc.gridx = 1;
    gbc.weightx = 1.0;
    gbl.setConstraints(comp, gbc);
    add(comp);

    row++;

    //setRowVisible(comp, visible);
  }

  void setRowVisible(Component comp, boolean b)
  {
    Component c = (Component) rowHash.get(comp);

    if (c == null)
      {
	return;
      }

    comp.setVisible(b);
    c.setVisible(b);

    //invalidateRight();
  }

  /**
   *
   * Helper method to add a component during constructor operation
   *
   */

  private void addFieldComponent(db_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    short fieldType;
    String name = null;
    boolean isVector;
    boolean isEditInPlace;

    /* -- */

    if (field == null)
      {
	throw new IllegalArgumentException("null field");
      }

    fieldType = fieldTemplate.getType();
    isVector = fieldTemplate.isArray();


    if (debug)
      {
	System.out.println("Name: " + fieldTemplate.getName() + " Field type desc: " + fieldType);
      }
    
    if (isVector)
      {
	if (fieldType == FieldType.STRING)
	  {
	    addStringVector((string_field) field, fieldInfo, fieldTemplate);
	  }
	else if (fieldType == FieldType.INVID && !fieldTemplate.isEditInPlace())
	  {
	    addInvidVector((invid_field) field, fieldInfo, fieldTemplate);
	  }
	else			// generic vector
	  {
	    addVectorPanel(field, fieldInfo, fieldTemplate);
	  }

      }
    else
      {
	// plain old component

	switch (fieldType)
	  {
	  case -1:
	    System.err.println("Could not get field information");
	    break;
		      
	  case FieldType.STRING:
	    addStringField((string_field) field, fieldInfo, fieldTemplate);
	    break;
		      
	  case FieldType.PASSWORD:
	    addPasswordField((pass_field) field, fieldInfo, fieldTemplate);
	    break;
		      
	  case FieldType.NUMERIC:
	    addNumericField(field, fieldInfo, fieldTemplate);
	    break;
		      
	  case FieldType.DATE:
	    addDateField(field, fieldInfo, fieldTemplate);
	    break;
		      
	  case FieldType.BOOLEAN:
	    addBooleanField(field, fieldInfo, fieldTemplate);
	    break;
		      
	  case FieldType.PERMISSIONMATRIX:
	    addPermissionField(field, fieldInfo, fieldTemplate);
	    break;
		      
	  case FieldType.INVID:
	    addInvidField((invid_field)field, fieldInfo, fieldTemplate);
	    break;

	  case FieldType.IP:
	    addIPField((ip_field) field, fieldInfo, fieldTemplate);
	    break;
		      
	  default:
	    JLabel label = new JLabel("(Unknown)Field type ID = " + fieldType);
	    addRow( label, fieldTemplate.getName(), true);
	  }
      }
  }

  /**
   *
   * private helper method to instantiate a string vector in this
   * container panel
   *
   */

  private void addStringVector(string_field field, FieldInfo fieldInfo,FieldTemplate fieldTemplate) throws RemoteException
  {
    if (debug)
      {
	System.out.println("Adding StringSelector, its a vector of strings!");
      }

    if (field == null)
      {
	System.out.println("Hey, this is a null field! " + fieldTemplate.getName());

      }

    if (editable)
      {
	QueryResult qr = null;
	
	if (debug)
	  {
	    System.out.println("Getting choicesKey()");
	  }
	Object id = field.choicesKey();
	if (id == null)
	  {
	    if (debug)
	      {
		System.out.println("Getting choices");
	      }
	    qr = field.choices();
	  }
	else
	  {
	    if (gc.cachedLists.containsKey(id))
	      {
		qr = (QueryResult)gc.cachedLists.get(id);
	      }
	    else
	      {	
		if (debug)
		  {
		    System.out.println("Getting QueryResult now");
		  }

		qr =field.choices();
		if (qr != null)
		  {
		    gc.cachedLists.put(id, qr);
		  }
	      }
	  }
    


	if (qr == null)
	  {
	    tStringSelector ss = new tStringSelector(null,
						     (Vector)fieldInfo.getValue(), 
						     this,
						     editable,
						     false,  //canChoose
						     false,  //mustChoose
						     160);
	    objectHash.put(ss, field);
	    if (editable)
	      {
		ss.setCallback(this);
	      }
	    addRow( ss, fieldTemplate.getName(), fieldInfo.isVisible()); 
	  }
	else
	  {
	    tStringSelector ss = new tStringSelector(qr.getLabels(),
						     (Vector)fieldInfo.getValue(), 
						     this,
						     editable,
						     true,   //canChoose
						     false,  //mustChoose
						     160);
	    objectHash.put(ss, field);
	    if (editable)
	      {
		ss.setCallback(this);
	      }

	    addRow( ss, fieldTemplate.getName(), fieldInfo.isVisible()); 
	  }
      }
    else  //not editable, don't need whole list of things
      {
	    tStringSelector ss = new tStringSelector(null,
						     (Vector)fieldInfo.getValue(), 
						     this,
						     editable,
						     false,   //canChoose
						     false,  //mustChoose
						     160);
	    objectHash.put(ss, field);
	    addRow( ss, fieldTemplate.getName(), fieldInfo.isVisible()); 
      }
  }

  /**
   *
   * private helper method to instantiate an invid vector in this
   * container panel
   *
   */

  private void addInvidVector(invid_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    QueryResult
      valueResults = null,
      choiceResults = null;

    Vector
      valueHandles = null,
      choiceHandles = null;

    /* -- */

    if (debug)
      {
	System.out.println("Adding StringSelector, its a vector of invids!");
      }

    valueHandles = field.encodedValues().getListHandles();

    if (editable)
      {
	Object key = field.choicesKey();

	if (key == null)
	  {
	    if (debug)
	      {
		System.out.println("key is null, downloading new copy");
	      }
	    QueryResult choices = field.choices();
	    if (choices != null)
	      {
		choiceHandles = choices.getListHandles();
	      }
	    else
	      { 
		if (debug)
		  {
		    System.out.println("choicse is null");
		  }
		choiceHandles = null;
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("key= " + key);
	      }

	    if (gc.cachedLists.containsKey(key))
	      {
		if (debug)
		  {
		    System.out.println("It's in there, using cached list");
		  }
		choiceHandles = (Vector)gc.cachedLists.get(key);
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("It's not in there, downloading anew.");
		  }

		choiceHandles = field.choices().getListHandles();
		gc.cachedLists.put(key, choiceHandles);
	      }
	      
	  }
      }
    else
      { 
	if (debug)
	  {
	    System.out.println("Not editable, not downloading choices");
	  }
      }

    // ss is canChoose, mustChoose
    JPopupMenu invidTablePopup = new JPopupMenu();
    JMenuItem editO = new JMenuItem("Edit object");
    JMenuItem viewO = new JMenuItem("View object");
    invidTablePopup.add(editO);
    invidTablePopup.add(viewO);
    
    JPopupMenu invidTablePopup2 = new JPopupMenu();
    JMenuItem editO2 = new JMenuItem("Edit object");
    JMenuItem viewO2 = new JMenuItem("View object");
    invidTablePopup2.add(editO2);
    invidTablePopup2.add(viewO2);

    tStringSelector ss = new tStringSelector(choiceHandles, valueHandles, this, editable, true, true, 160, "Selected", "Available", invidTablePopup, invidTablePopup2);
    objectHash.put(ss, field);
    if (editable)
      {
	ss.setCallback(this);
      }
    addRow( ss, fieldTemplate.getName(), fieldInfo.isVisible()); 
  }

  /*
  public void validate()
  {
    System.out.println("--Validate: containerPanel: " + this);
    super.validate();
  }
  public void invalidate()
  {
    System.out.println("--inValidate: containerPanel: " + this);
    super.invalidate();
  }

  public void doLayout()
  {
      System.out.println(">> containerP.doLayout(): " + this);
      super.doLayout();
      System.out.println("<< cp doLayout done");
  }
  */

  private final void setStatus(String s)
  {
    gc.setStatus(s);
  }

  /**
   *
   * private helper method to instantiate a vector panel in this
   * container panel
   *
   */

  private void addVectorPanel(db_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    boolean isEditInPlace = fieldTemplate.isEditInPlace();

    /* -- */

    if (debug)
      {
	if (isEditInPlace)
	  {
	    System.out.println("Adding editInPlace vector panel");
	  }
	else
	  {
	    System.out.println("Adding normal vector panel");
	  }
      }

    vectorPanel vp = new vectorPanel(field, winP, editable, isEditInPlace, this);
    vectorPanelList.addElement(vp);

    addVectorRow( vp, fieldTemplate.getName(), fieldInfo.isVisible());
    
  }

  /**
   *
   * private helper method to instantiate a string field in this
   * container panel
   *
   */

  private void addStringField(string_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    JstringField
      sf;

    /* -- */

    if (field.canChoose())
      {
	if (debug)
	  {
	    System.out.println("You can choose");
	  }
	    
	JComboBox combo = new JComboBox();

	Vector choiceHandles = null;
	Vector choices = null;

	Object key = field.choicesKey();
	if (key == null)
	  {
	    if (debug)
	      {
		System.out.println("key is null, getting new copy.");
	      }
	    choices = field.choices().getLabels();
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("key = " + key);
	      }
		
	    if (gc.cachedLists.containsKey(key))
	      {
		if (debug)
		  {
		    System.out.println("key in there, using cached list");
		  }
		choiceHandles = (Vector)gc.cachedLists.get(key);

	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("It's not in there, downloading a new one.");
		  }
		choiceHandles = field.choices().getListHandles();
		gc.cachedLists.put(key, choiceHandles);

	      }
	    
	    for (int j = 0; j < choiceHandles.size() ; j++)
	      {
		choices.addElement(((listHandle)choiceHandles.elementAt(j)).getLabel());
	      }		
	  }    

	String currentChoice = (String) fieldInfo.getValue();
	boolean found = false;
	    
	for (int j = 0; j < choices.size(); j++)
	  {
	    String thisChoice = (String)choices.elementAt(j);
	    combo.addItem(thisChoice);
		
	    if (!found && (currentChoice != null))
	      {
		if (thisChoice.equals(currentChoice))
		  {
		    found = true;
		  }
	      }
		
	    /*if (debug)
	      {
	      System.out.println("Adding " + (String)choices.elementAt(j));
	      }*/
	  }
	    
	// if the current value wasn't in the choice, add it in now
	    
	if (!found && (currentChoice != null))
	  {
	    combo.addItem(currentChoice);
	  }
	    
	combo.setMaximumRowCount(8);
	combo.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
	try
	  {
	    boolean mustChoose = field.mustChoose();
	    combo.setEditable(mustChoose); // this should be setEditable(mustChoose());
	    if (debug)
	      {
		System.out.println("Setting editable to + " + mustChoose);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not check to see if field was mustChoose.");
	  }
	//combo.setVisible(true);  // This line is not necessary, right?
	    
	if (currentChoice != null)
	  {
	    combo.setSelectedItem(currentChoice);
	  }

	if (debug)
	  {
	    System.out.println("Setting current value: " + currentChoice);
	  }	  

	if (editable)
	  {
	    combo.addItemListener(this); // register callback
	  }

	objectHash.put(combo, field);
	if (debug)
	  {
	    System.out.println("Adding to panel");
	  }
	    
	addRow( combo, fieldTemplate.getName(), fieldInfo.isVisible());
	    	    
      }
    else
      {
	// It's not a choice
      
	sf = new JstringField(20,
			      field.maxSize(),
			      new JcomponentAttr(null,
						 new Font("Helvetica",Font.PLAIN,12),
						 Color.black,Color.white),
			      editable,
			      false,
			      fieldTemplate.getOKChars(),
			      fieldTemplate.getBadChars(),
			      this);
			      
	objectHash.put(sf, field);
			      
	sf.setText((String)fieldInfo.getValue());
	    			
	if (editable)
	  {
	    sf.setCallback(this);
	  }

	sf.setEditable(editable);

	sf.setToolTipText(fieldTemplate.getComment());

	addRow( sf, fieldTemplate.getName(), fieldInfo.isVisible());
      }
  }

  /**
   *
   * private helper method to instantiate a password field in this
   * container panel
   *
   */

  private void addPasswordField(pass_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    JstringField sf;

    /* -- */

    if (editable)
      {
	JpassField pf = new JpassField(gc, true, 10, 8, editable);
	objectHash.put(pf, field);
			
	if (editable)
	  {
	    pf.setCallback(this);
	  }
	  
	addRow( pf, field.getName(), field.isVisible());
	
      }
    else
      {
	sf = new JstringField(20,
			      field.maxSize(),
			      new JcomponentAttr(null,
						 new Font("Helvetica",Font.PLAIN,12),
						 Color.black,Color.white),
			      true,
			      false,
			      null,
			      null);

	objectHash.put(sf, field);
			  
	// the server won't give us an unencrypted password, we're clear here
			  
	sf.setText((String)fieldInfo.getValue());
	
		      
	sf.setEditable(false);

	sf.setToolTipText(fieldTemplate.getComment());
	
	addRow( sf, fieldTemplate.getName(), fieldInfo.isVisible());
	
      }
  }

  /**
   *
   * private helper method to instantiate a numeric field in this
   * container panel
   *
   */

  private void addNumericField(db_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    if (debug)
      {
	System.out.println("Adding numeric field");
      }
      
    JnumberField nf = new JnumberField();

			      
    objectHash.put(nf, field);
	
		      
    Integer value = (Integer)fieldInfo.getValue();
    if (value != null)
      {
	nf.setValue(value.intValue());
      }

    
    if (editable)
      {
	nf.setCallback(this);
      }

    nf.setEditable(editable);
    nf.setColumns(20);
    
    nf.setToolTipText(fieldTemplate.getComment());
    
    addRow( nf, fieldTemplate.getName(), fieldInfo.isVisible());
  
    
  }

  /**
   *
   * private helper method to instantiate a date field in this
   * container panel
   *
   */

  private void addDateField(db_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    JdateField df = new JdateField();
		      
    objectHash.put(df, field);
    df.setEditable(editable);

    Date date = ((Date)fieldInfo.getValue());
    
    if (date != null)
      {
	df.setDate(date);
      }

    // note that we set the callback after we initially set the
    // date, to avoid having the callback triggered on a listing

    if (editable)
      {
	df.setCallback(this);
      }

    addRow( df, fieldTemplate.getName(), fieldInfo.isVisible());
  }

  /**
   *
   * private helper method to instantiate a boolean field in this
   * container panel
   *
   */

  private void addBooleanField(db_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    //JcheckboxField cb = new JcheckboxField();

    JCheckBox cb = new JCheckBox();
    objectHash.put(cb, field);
    cb.setEnabled(editable);
    if (editable)
      {
	cb.addActionListener(this);	// register callback
      }
    try
      {
	cb.setSelected(((Boolean)fieldInfo.getValue()).booleanValue());
      }
    catch (NullPointerException ex)
      {
	if (debug)
	  {
	    System.out.println("Null pointer setting selected choice: " + ex);
	  }
      }

    addRow( cb, fieldTemplate.getName(), fieldInfo.isVisible());
    
  }

  /**
   *
   * private helper method to instantiate a permission matrix field in this
   * container panel
   *
   */

  private void addPermissionField(db_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    if (debug)
      {
	System.out.println("Adding perm matrix");
      }

    // note that the permissions editor does its own callbacks to
    // the server, albeit using our transaction / session.

    perm_button pb = new perm_button(gc, 
				     (perm_field) field,
				     editable,
				     gc.getBaseHash());
    
    addRow( pb, fieldTemplate.getName(), fieldInfo.isVisible());
    
  }

  /**
   *
   * private helper method to instantiate an invid field in this
   * container panel
   *
   */

  private void addInvidField(invid_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    if (fieldTemplate.isEditInPlace())
      {
	if (debug)
	  {
	    System.out.println("Hey, " + fieldTemplate.getName() + " is edit in place but not a vector, what gives?");
	  }
	addRow(new JLabel("edit in place non-vector"), fieldTemplate.getName(), fieldInfo.isVisible());
	return;
      }

    if (editable && fieldInfo.isEditable())
      {
	Object key = field.choicesKey();
	
	Vector choices = null;

	if (key == null)
	  {
	    if (debug)
	      {
		System.out.println("key is null");
	      }

	    choices = field.choices().getListHandles();
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("key = " + key);
	      }

	    if (gc.cachedLists.containsKey(key))
	      {
		if (debug)
		  {
		    System.out.println("Got it from the cachedLists");
		  }
		choices = (Vector)gc.cachedLists.get(key);
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("Damn, it's not in there, downloading a new one.");
		  }
		choices = field.choices().getListHandles();
		gc.cachedLists.put(key, choices);
	      }
	  }
        Invid currentChoice = (Invid) fieldInfo.getValue();
	listHandle currentListHandle = null;
	listHandle noneHandle = new listHandle("<none>", null);
	boolean found = false;
	JComboBox combo = new JComboBox();
	
	/* -- */

	combo.addItem(noneHandle);
	
	for (int j = 0; j < choices.size(); j++)
	  {
	    listHandle thisChoice = (listHandle) choices.elementAt(j);
	    combo.addItem(thisChoice);
	    
	    if (!found && (currentChoice != null))
	      {
		if (thisChoice.getObject().equals(currentChoice))
		  {
		    if (debug)
		      {
			System.out.println("Found the current object in the list!");
		      }
		    currentListHandle = thisChoice;
		    found = true;
		  }
	      }
	    
	    /*	    if (debug)
	      {
		System.out.println("Adding " + (listHandle)choices.elementAt(j));
	      }*/
	  }
	
	// if the current value wasn't in the choice, add it in now
	
	if (!found)
	  {
	    if (currentChoice != null)
	      {
		currentListHandle = new listHandle(gc.getSession().viewObjectLabel(currentChoice), currentChoice);
		combo.addItem(currentListHandle);
	      }
	  }
	
	combo.setMaximumRowCount(12);
	combo.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
	combo.setEditable(false); // This should be true
	combo.setVisible(true);

	if (currentChoice != null)
	  {
	    if (debug)
	      {
		System.out.println("setting current choice: " + currentChoice);
	      }
	    combo.setSelectedItem(currentListHandle);
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("currentChoice is null");
	      }
	    combo.setSelectedItem(noneHandle);
	  }	  

	if (editable)
	  {
	    combo.addItemListener(this); // register callback
	  }

	objectHash.put(combo, field);

	if (debug)
	  {
	    System.out.println("Adding to panel");
	  }
	
	addRow( combo, fieldTemplate.getName(), fieldInfo.isVisible());
	
      }
    else
      {
	if (fieldInfo.getValue() != null)
	  {
	    String label = (String)gc.getSession().view_db_object((Invid)fieldInfo.getValue()).getLabel();
	    JstringField sf = new JstringField(20, false);
	    sf.setText(label);
	    addRow(sf, fieldTemplate.getName(), fieldInfo.isVisible());
	  }
	else
	  {
	    addRow( new JTextField("null invid"), fieldTemplate.getName(), fieldInfo.isVisible());
	  }
      }
  }

  /**
   *
   * private helper method to instantiate an ip field in this
   * container panel
   *
   */

  private void addIPField(ip_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    JIPField
      ipf;

    Byte[] bytes;

    /* -- */

    try
      {
	ipf = new JIPField(new JcomponentAttr(null,
					      new Font("Helvetica",Font.PLAIN,12),
					      Color.black,Color.white),
			   editable,
			   field.v6Allowed());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get determine v6Allowed for ip field: " + rx);
      }
    
    objectHash.put(ipf, field);
    
    bytes = (Byte[]) fieldInfo.getValue();

	if (bytes != null)
	  {
	    ipf.setValue(bytes);
	  }
	
    ipf.setCallback(this);

    ipf.setToolTipText(fieldTemplate.getComment());
		
    addRow( ipf, fieldTemplate.getName(), fieldInfo.isVisible());
    
  }

  /**
   * The idea here is that you could use this in a catch from a RemoteException
   *
   * like this:
   * try
   *   {
   *      whatever;
   *   }
   * catch (RemoteException rx)
   *   {
   *      error("Something went wrong", rx);
   */
  private void error(String label, RemoteException rx)
    {
      try
	{
	  System.out.println("Last error: " + gc.getSession().getLastError());
	}
      catch (RemoteException ex)
	{
	  System.out.println("Exception getting last error: " + rx);
	}
      throw new RuntimeException(label + rx);
    }


}
