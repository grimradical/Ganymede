/*

   SchemaEdit.java

   Client side interface for schema editing
   
   Created: 17 April 1997
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                      SchemaEdit

------------------------------------------------------------------------------*/

/**
 *
 * Client side interface definition for the the Ganymede Schema Editing class.
 *
 */

public interface SchemaEdit extends Remote {

  public Category getRootCategory() throws RemoteException;

  public Base[] getBases(boolean embedded) throws RemoteException;
  public Base[] getBases() throws RemoteException;

  public Base getBase(short id) throws RemoteException;
  public Base getBase(String baseName) throws RemoteException;

  public Base createNewBase(Category category, boolean embedded) throws RemoteException;
  public void deleteBase(Base b) throws RemoteException;

  public NameSpace[] getNameSpaces() throws RemoteException;
  public NameSpace getNameSpace(String spaceName) throws RemoteException;

  public NameSpace createNewNameSpace(String name, boolean caseInsensitive) throws RemoteException;
  public boolean deleteNameSpace(String name) throws RemoteException;

  public void commit() throws RemoteException;
  public void release() throws RemoteException;
}
