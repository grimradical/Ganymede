/*

   Invid.java

   Non-remote object;  used as local on client and server,
   passed as value object.

   Invid's are intended to be immutable once created.

   Data type for invid objects;
   
   Created: 11 April 1996
   Version: $Revision: 1.11 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

public final class Invid implements java.io.Serializable {

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

  /**
   *
   * This is the string constructor.. string should be
   * a pair of colon separated numbers, in the form
   *
   * 5:134 where the first number is the short type
   * and the second is the int object number
   *
   */

  public Invid(String string)
  {
    String first = string.substring(0, string.indexOf(':'));
    String last = string.substring(string.indexOf(':')+1);

    try
      {
	this.type = Short.valueOf(first).shortValue();
	this.num = Integer.valueOf(last).intValue();
      }
    catch (NumberFormatException ex)
      {
	throw new IllegalArgumentException("bad string format " + ex);
      }
  }

  // equals

  public boolean equals(Object obj)
  {
    if (obj instanceof Invid)
      {
	return equals((Invid) obj);
      }
    else
      {
	return false;
      }
  }

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
