/*

   systemCustom.java

   This file is a management class for system objects in Ganymede.
   
   Created: 15 October 1997
   Version: $Revision: 1.8 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    systemCustom

------------------------------------------------------------------------------*/

public class systemCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;
  static QueryResult shellChoices = new QueryResult();
  static Date shellChoiceStamp = null;

  // ---

  /**
   * vector of ip network Object Handles in current room
   */

  Vector netsInRoom = null;

  /**
   * vector of ip network Object Handles free in current room
   */

  Vector freeNets = null;

  /**
   * map of IPNet Invids to addresses
   */

  Hashtable ipAddresses = new Hashtable();

  /**
   *
   * Customization Constructor
   *
   */

  public systemCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public systemCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public systemCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);

    initializeNets((Invid) getFieldValueLocal(systemSchema.ROOM));
  }

  /**
   *
   * Returns a vector of ObjectHandle objects describing the I.P. nets
   * available for this system to be connected to.<br><br>
   *
   * Used by the interfaceCustom object to provide a list of network
   * choices.
   *
   */

  public Vector getAvailableNets()
  {
    System.err.println("systemCustom: returning freeNets");
    return freeNets;
  }

  /**
   *
   * This method returns an IPv4 address for an embedded interface
   * based on the network invid passed in.
   *
   */

  public Byte[] getAddress(Invid netInvid)
  {
    System.err.println("systemCustom: returning address for net " + getGSession().viewObjectLabel(netInvid));
    return (Byte[]) ipAddresses.get(netInvid);
  }

  /**
   *
   * Marks a network in the current room that was previously used as
   * available for an interface attached to this system to be
   * connected to.<br><br>
   *
   * Used by the interfaceCustom object to provide a list of network
   * choices.
   * 
   */

  public synchronized boolean freeNet(Invid netInvid)
  {
    ObjectHandle handle;
    boolean found = false;

    /* -- */

    // do we already have this net in our free list?

    for (int i = 0; i < freeNets.size(); i++)
      {
	handle = (ObjectHandle) freeNets.elementAt(i);
	
	if (handle.getInvid().equals(netInvid))
	  {
	    found = true;
	  }
      }

    if (found)
      {
	return false;
      }

    for (int i = 0; i < netsInRoom.size(); i++)
      {
	handle = (ObjectHandle) netsInRoom.elementAt(i);
	
	if (handle.getInvid().equals(netInvid))
	  {
	    freeNets.addElement(handle);
	    System.err.println("systemCustom.freeNet(" + handle.getLabel() + ")");
	    return true;
	  }
      }

    return false;
  }

  /**
   *
   * Checks out a network for use by an interface in the current room.
   * <br><br>
   *
   * Used by the interfaceCustom object to provide a list of network
   * choices.
   * 
   */

  public synchronized boolean allocNet(Invid netInvid)
  {
    ObjectHandle handle;

    /* -- */
    
    for (int i = 0; i < freeNets.size(); i++)
      {
	handle = (ObjectHandle) freeNets.elementAt(i);
	
	if (handle.getInvid().equals(netInvid))
	  {
	    System.err.println("systemCustom.allocNet(" + handle.getLabel() + ")");
	    freeNets.removeElementAt(i);
	    return true;
	  }
      }

    return false;
  }

  /**
   *
   * private helper method to initialize our network choices
   * that our interface code uses.  This method will load our
   * netsInRoom vector with a list of object handles suitable
   * for use as network choices for our embedded interfaces,
   * will reset our freeNets vector with a list of object
   * handles that are available for new interfaces, or to
   * change an existing interface to, and builds the ipAddresses
   * hash to give us a quick way of picking an ip address for
   * a given subnet.
   *
   */

  private void initializeNets(Invid roomInvid)
  {
    DBObject interfaceObj;
    Invid netInvid;
    String label;
    ObjectHandle handle;
    boolean usingNet = false;
    Hashtable localAddresses = new Hashtable();
    Byte[] address;

    /* -- */

    System.err.println("systemCustom.initializeNets(" + getGSession().viewObjectLabel(roomInvid)+")");

    if (netsInRoom == null)
      {
	netsInRoom = new Vector();
      }
    else
      {
	netsInRoom.removeAllElements();
      }

    if (freeNets == null)
      {
	freeNets = new Vector();
      }
    else
      {
	freeNets.removeAllElements();
      }

    if (roomInvid == null)
      {
	return;
      }

    // what embedded interfaces do we have right now?

    Vector interfaces = getFieldValuesLocal(systemSchema.INTERFACES);

    // get the room information

    DBObject roomObj = getSession().viewDBObject(roomInvid);
    Vector nets = roomObj.getFieldValuesLocal(roomSchema.NETWORKS);
    
    for (int i = 0; i < nets.size(); i++)
      {
	netInvid = (Invid) nets.elementAt(i);
	label = getGSession().viewObjectLabel(netInvid);
	handle = new ObjectHandle(label, netInvid, false, false, false, true);

	netsInRoom.addElement(handle);

	// find out what nets are new with this new room invid and which we
	// were already using

	if (!ipAddresses.containsKey(netInvid))
	  {
	    usingNet = false;

	    if (interfaces != null)
	      {
		for (int j = 0; j < interfaces.size(); j++)
		  {
		    interfaceObj = getSession().viewDBObject((Invid) interfaces.elementAt(j));

		    // interfaceObj damn well shouldn't be null

		    Invid netInvid2 = (Invid) interfaceObj.getFieldValueLocal(interfaceSchema.IPNET);

		    if (netInvid2 != null && netInvid2.equals(netInvid))
		      {
			usingNet = true;

			// remember this address for this net

			address = (Byte[]) interfaceObj.getFieldValueLocal(interfaceSchema.ADDRESS);
			
			ipAddresses.put(netInvid, address);
			break;
		      }
		  }
	      }

	    // if we didn't find an interface using this net, we'll need to generate
	    // a new address for this network.

	    if (!usingNet)
	      {
		localAddresses.put(netInvid, netInvid);
	      }
	  }
      }

    // okay, now localAddresses has a map for the nets that we were
    // not previously on.
    //
    // now we need to get an IP address on each net.. if any of the
    // nets are full, we'll remove that network from netsInRoom to
    // mark that net as not being usable for a new address


    Enumeration enum = localAddresses.keys();

    while (enum.hasMoreElements())
      {
	netInvid = (Invid) enum.nextElement();

	address = getIPAddress(netInvid);

	if (address != null)
	  {
	    // we've allocated a new address for a net that
	    // we don't yet have an address for.. store
	    // the address we allocated and add the net
	    // to the freeNets vector.

	    ipAddresses.put(netInvid, address);

	    for (int i = 0; i < netsInRoom.size(); i++)
	      {
		handle = (ObjectHandle) netsInRoom.elementAt(i);

		if (handle.getInvid().equals(netInvid))
		  {
		    System.err.println("systemCustom.initializeNets(): freeing net " + handle.getLabel());
		    freeNets.addElement(handle);
		    break;
		  }
	      }
	  }
	else
	  {
	    // we couldn't get an address for netInvid.. take the net
	    // out of our netsInRoom vector.

	    for (int i = 0; i < netsInRoom.size(); i++)
	      {
		handle = (ObjectHandle) netsInRoom.elementAt(i);

		if (handle.getInvid().equals(netInvid))
		  {
		    netsInRoom.removeElementAt(i);
		    break;
		  }
	      }
	  }
      }
  }

  /**
   *
   * This method is designed to allocated a free I.P. address for the
   * given net.
   *
   */

  private Byte[] getIPAddress(Invid netInvid)
  {
    DBNameSpace namespace = Ganymede.db.getNameSpace("IPspace");

    // default IP host-byte scan pattern 

    int start = 1;
    int stop = 254;

    /* -- */

    if (namespace == null)
      {
	System.err.println("systemCustom.setIPAddress(): couldn't get IP namespace");
	return null;
      }

    DBObject netObj = getSession().viewDBObject(netInvid);
    Byte[] netNum = (Byte[]) netObj.getFieldValueLocal(networkSchema.NETNUMBER);
    
    if (netNum.length != 4)
      {
	Ganymede.debug("Error, " + netObj.getLabel() + 
		       " has an improper network number for the GASH schema.");
	return null;
      }

    Byte[] address = new Byte[4];

    for (int i = 0; i < netNum.length; i++)
      {
	address[i] = netNum[i];
      }

    // ok, we've got our net prefix.. try to find an open slot..

    // this really should use the IP range specified in our system
    // type.. we can elaborate that once this is working ok

    try
      {
	Invid systemTypeInvid = (Invid) getFieldValueLocal(systemSchema.SYSTEMTYPE);
	DBObject systemTypeInfo = getSession().viewDBObject(systemTypeInvid);
	
	if (systemTypeInfo != null)
	  {
	    start = ((Integer) systemTypeInfo.getFieldValueLocal(systemTypeSchema.STARTIP)).intValue();
	    stop = ((Integer) systemTypeInfo.getFieldValueLocal(systemTypeSchema.STOPIP)).intValue();

	    System.err.println("systemCustom.getIPAddress(): found start and stop for this type: " + start + "->" + stop);
	  }
      }
    catch (NullPointerException ex)
      {
	System.err.println("systemCustom.getIPAddress(): null pointer exception trying to get system type info");
      }

    int i = start;
    address[3] = new Byte(u2s(i));

    if (start > stop)
      {
	while (i > stop && !namespace.reserve(editset, address))
	  {
	    address[3] = new Byte(u2s(--i));
	  }
      }
    else
      {
	while (i < stop && !namespace.reserve(editset, address))
	  {
	    address[3] = new Byte(u2s(++i));
	  }
      }


    // see if we really did wind up with an acceptable address

    if (!namespace.reserve(editset, address))
      {
	return null;
      }
    else
      {
	System.err.print("systemCustom.getIPAddress(): returning ");
	
	for (int j = 0; j < address.length; j++)
	  {
	    if (j > 0)
	      {
		System.err.print(".");
	      }

	    System.err.print(s2u(address[j].byteValue()));
	  }

	System.err.println();

	return address;
      }
  }

  /**
   *
   * This method maps an int value between 0 and 255 inclusive
   * to a legal signed byte value.
   *
   */

  public final static byte u2s(int x)
  {
    if ((x < 0) || (x > 255))
      {
	throw new IllegalArgumentException("Out of range: " + x);
      }

    return (byte) (x - 128);
  }

  /**
   *
   * This method maps a u2s-encoded signed byte value to an
   * int value between 0 and 255 inclusive.
   *
   */

  public final static short s2u(byte b)
  {
    return (short) (b + 128);
  }

  /**
   *
   * Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
      case systemSchema.SYSTEMNAME:
      case systemSchema.INTERFACES:
      case systemSchema.DNSDOMAIN:
      case systemSchema.SYSTEMTYPE:
      case systemSchema.ROOM:
	return true;
      }

    return false;
  }

  /**
   *
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.<br><br>
   *
   * If there is no caching key, this method will return null.
   *
   */

  public Object obtainChoicesKey(DBField field)
  {
    DBObjectBase base = Ganymede.db.getObjectBase((short) 272);	// system types

    /* -- */

    if (field.getID() == systemSchema.VOLUMES)
      {
	return null;		// no choices for volumes
      }
    
    if (field.getID() != systemSchema.SYSTEMTYPE)	// system type field
      {
	return super.obtainChoicesKey(field);
      }
    else
      {
	// we put a time stamp on here so the client
	// will know to call obtainChoiceList() afresh if the
	// system types base has been modified

	return "System Type:" + base.getTimeStamp();
      }
  }

  /**
   *
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.
   * 
   */

  public QueryResult obtainChoiceList(DBField field)
  {
    if (field.getID() == systemSchema.VOLUMES)
      {
	return null;		// no choices for volumes
      }

    if (field.getID() != systemSchema.SYSTEMTYPE) // system type field
      {
	return super.obtainChoiceList(field);
      }

    Query query = new Query((short) 272, null, false); // list all system types

    query.setFiltered(false);	// don't care if we own the system types

    return editset.getSession().getGSession().query(query);
  }

  /**
   *
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()
   *
   */

  public boolean mustChoose(DBField field)
  {
    if (field.getID() == systemSchema.SYSTEMTYPE)
      {
	return true;
      }

    return super.mustChoose(field);
  }
  
  /**
   *
   * This method allows the DBEditObject to have executive approval
   * of any scalar set operation, and to take any special actions
   * in reaction to the set.. if this method returns true, the
   * DBField that called us will proceed to make the change to
   * it's value.  If this method returns false, the DBField
   * that called us will not make the change, and the field
   * will be left unchanged.<br><br>
   *
   * The DBField that called us will take care of all possible checks
   * on the operation (including a call to our own verifyNewValue()
   * method.  Under normal circumstances, we won't need to do anything
   * here.<br><br>
   *
   * If we do return false, we should set editset.setLastError to
   * provide feedback to the client about what we disapproved of.
   *  
   */

  public boolean finalizeSetValue(DBField field, Object value)
  {
    if (field.getID() == systemSchema.ROOM)
      {
	// need to update the net information from this room

	System.err.println("systemCustom: room changed to " + getGSession().viewObjectLabel((Invid) value));
	
	if (gSession.enableOversight)
	  {
	    initializeNets((Invid) value);
	  }
      }
    
    if (field.getID() == systemSchema.SYSTEMTYPE)
      {
	// need to update the ip addresses pre-allocated for this system

	System.err.println("systemCustom: system type changed to " + getGSession().viewObjectLabel((Invid) value));
	
	if (gSession.enableOversight)
	  {
	    initializeNets((Invid) getFieldValueLocal(systemSchema.ROOM));
	  }
      }

    return true;
  }

  /**
   *
   * This is the hook that DBEditObject subclasses use to interpose wizards whenever
   * a sensitive field is being changed.
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    if (field.getID() == systemSchema.ROOM)
      {
	// we need to generate a returnval that will cause all our interfaces' ipnet
	// fields to be rescanned.

	Vector interfaces = getFieldValuesLocal(systemSchema.INTERFACES);

	if (interfaces == null)
	  {
	    return null;
	  }

	ReturnVal interfaceRescan = new ReturnVal(true, true);
	interfaceRescan.addRescanField(interfaceSchema.IPNET);

	ReturnVal result = new ReturnVal(true, true);

	for (int i = 0; i < interfaces.size(); i++)
	  {
	    result.addRescanObject((Invid) interfaces.elementAt(i), interfaceRescan);
	  }

	return result;
      }
    
    return null;
  }
  /**
   *
   * Hook to have this object create a new embedded object
   * in the given field.  
   *
   */

  public Invid createNewEmbeddedObject(InvidDBField field)
  {
    DBEditObject newObject;
    DBObjectBase targetBase;
    DBObjectBaseField fieldDef;

    /* -- */

    if (field.getID() == systemSchema.INTERFACES) // interface field
      {
	fieldDef = field.getFieldDef();
	
	if (fieldDef.getTargetBase() > -1)
	  {
	    newObject = getSession().createDBObject(fieldDef.getTargetBase(), null, null);

	    // link it in

	    newObject.setFieldValue(SchemaConstants.ContainerField, getInvid());
	    
	    return newObject.getInvid();
	  }
	else
	  {
	    throw new RuntimeException("error in schema.. interface field target base not restricted..");
	  }
      }
    else
      {
	return null;		// default
      }
  }
}
