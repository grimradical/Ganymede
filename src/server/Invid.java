/*

   Invid.java

   Non-remote object;  used as local on client and server,
   passed as value object.

   Invid's are intended to be immutable once created.

   Data type for invid objects;
   
   Created: 11 April 1996
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

public class Invid {

  //  static final int FIRST = 1;
  //  static final int LAST = 9;

  private short type;
  private int num;

  // constructors

  public Invid(short type, int num) 
  {
    //    if ((type < FIRST) ||
    //	(type > LAST))
    //      {
    //	throw new IndexOutOfBoundsException("type out of range " + type);
    //      }

    this.type = type;
    this.num = num;
  }

  // equals

  public boolean equals(Invid invid)
  {
    if ((invid.type == type) &&
	(invid.num == num))
      {
	return true;
      }

    return false;
  }

  // hashcode

  public int hashCode()
  {

    return num;			// simplistic, different types of invid's with
				// same number will hash to same bucket, but
				// this is probably ok for our uses, where we
				// will generally not have multiple types of
				// invid's in a particular hash.
  }

  // pull the values

  public short getType() 
  {
    return type;
  }

  public int getNum() 
  {
    return num;
  }

  public String toString()
  {
    return type + ":" + num;
  }
}
