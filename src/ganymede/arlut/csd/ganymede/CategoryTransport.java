/*

   CategoryTransport.java

   This class is intended to provide a serializable object that
   can be used to bulk-dump a static description of the category
   and base structures on the server to the client.
   
   Created: 12 February 1998
   Release: $Name:  $
   Version: $Revision: 1.15 $
   Last Mod Date: $Date: 2000/11/21 12:57:24 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               CategoryTransport

------------------------------------------------------------------------------*/

/**
 *
 * This class is intended to provide a serializable object that
 * can be used to bulk-dump a static description of the category
 * and base structures on the server to the client.
 *
 */

public class CategoryTransport implements java.io.Serializable {

  static final long serialVersionUID = 4104856725462391453L;

  // ---

  StringBuffer buffer;

  /**
   * <p>This is really a GanymedeSession object, but by defining this
   * as a generic Object we avoid having the client attempt to load
   * the GanymedeSession.class unless it calls the server-side constructor
   * methods, which it should not do.</p>
   */

  transient Object session = null;

  /**
   * <p>If true and the session is a user client, only editable 
   * object types will be included in the generated CategoryTransport.</p>
   */

  transient private boolean hideNonEditables;

  /* -- */

  /**
   *
   * Server side constructor for the full category tree
   *
   */

  public CategoryTransport(DBBaseCategory root)
  {
    addCategoryInfo(root);
  }

  /**
   *
   * Server side constructor for the viewable subset of the category tree
   *
   */

  public CategoryTransport(DBBaseCategory root, GanymedeSession session)
  {
    this(root, session, true);
  }

  /**
   *
   * Server side constructor for the viewable subset of the category tree
   *
   */

  public CategoryTransport(DBBaseCategory root, GanymedeSession session, boolean hideNonEditables)
  {
    this.session = session;
    this.hideNonEditables = hideNonEditables;
    addCategoryInfo(root);
  }

  /**
   *
   * Client side accessor
   *
   */

  public CategoryDump getTree()
  {
    return new CategoryDump(null, buffer.toString().toCharArray(), 0);
  }

  // ***
  //
  // private methods, server side
  //
  // ***

  private void addCategoryInfo(DBBaseCategory category)
  {
    Vector contents;
    CategoryNode node;
    boolean result = false;

    /* -- */

    if (category == null)
      {
	throw new IllegalArgumentException("null category");
      }

    addChunk("cat");
    addChunk(category.getName());

    contents = category.getNodes();

    if (contents.size() > 0)
      {
	addChunk("<");

	for (int i = 0; i < contents.size(); i++)
	  {
	    node = (CategoryNode) contents.elementAt(i);
	    
	    if (node instanceof DBObjectBase)
	      {
		DBObjectBase base = (DBObjectBase) node;

		if ((session == null) ||
		    (hideNonEditables && ((GanymedeSession) session).getPerm(base.getTypeID(), true).isEditable()) ||
		    (!hideNonEditables && ((GanymedeSession) session).getPerm(base.getTypeID(), true).isVisible()))
		  {
		    result = true;
		    addBaseInfo(base);
		  }
	      }
	    else if (node instanceof DBBaseCategory)
	      {
		if ((session == null) ||
		    (hideNonEditables & containsEditableBase((DBBaseCategory) node)) ||
		    (!hideNonEditables && containsVisibleBase((DBBaseCategory) node)))
		  {
		    addCategoryInfo((DBBaseCategory) node);
		  }
	      }
	  }
      }

    // terminate this category record

    addChunk(">");
  }

  private boolean containsEditableBase(DBBaseCategory category)
  {
    Vector contents;
    CategoryNode node;
    boolean result = false;

    /* -- */

    if (session == null)
      {
	return true;		// we're not filtering, return true immediately
      }

    contents = category.getNodes();

    if (contents.size() > 0)
      {
	for (int i = 0; !result && i < contents.size(); i++)
	  {
	    node = (CategoryNode) contents.elementAt(i);
	    
	    if (node instanceof DBObjectBase)
	      {
		DBObjectBase base = (DBObjectBase) node;

		if (((GanymedeSession) session).getPerm(base.getTypeID(), true).isEditable())
		  {
		    result = true;
		  }
	      }
	    else if (node instanceof DBBaseCategory)
	      {
		result = containsEditableBase((DBBaseCategory) node);
	      }
	  }
      }

    return result;
  }

  private boolean containsVisibleBase(DBBaseCategory category)
  {
    Vector contents;
    CategoryNode node;
    boolean result = false;

    /* -- */

    if (session == null)
      {
	return true;		// we're not filtering, return true immediately
      }

    contents = category.getNodes();

    if (contents.size() > 0)
      {
	for (int i = 0; !result && i < contents.size(); i++)
	  {
	    node = (CategoryNode) contents.elementAt(i);
	    
	    if (node instanceof DBObjectBase)
	      {
		DBObjectBase base = (DBObjectBase) node;

		if (((GanymedeSession) session).getPerm(base.getTypeID(), true).isVisible())
		  {
		    result = true;
		  }
	      }
	    else if (node instanceof DBBaseCategory)
	      {
		result = containsVisibleBase((DBBaseCategory) node);
	      }
	  }
      }

    return result;
  }

  private void addBaseInfo(DBObjectBase node)
  {
    addChunk("base");
    addChunk(node.getName());
    addChunk(node.getPath());
    addChunk(String.valueOf(node.getTypeID()));
    addChunk(String.valueOf(node.getLabelField()));
    addChunk(node.getLabelFieldName());
    addChunk(String.valueOf(node.canInactivate()));
    addChunk(String.valueOf(node.canCreate(((GanymedeSession) session))));
    addChunk(String.valueOf(node.isEmbedded()));
  }

  private void addChunk(String text)
  {
    char[] chars;

    /* -- */

    //    System.err.println("Server adding chunk " + label + ":" + operand);

    if (buffer == null)
      {
	buffer = new StringBuffer();
      }

    // add our label

    if (text != null)
      {
	chars = text.toCharArray();
      }
    else
      {
	buffer.append("|");
	return;
      }
	
    for (int j = 0; j < chars.length; j++)
      {
	if (chars[j] == '|')
	  {
	    buffer.append("\\|");
	  }
	else if (chars[j] == '\\')
	  {
	    buffer.append("\\\\");
	  }
	else
	  {
	    buffer.append(chars[j]);
	  }
      }

    buffer.append("|");
  }
}