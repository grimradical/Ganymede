/* 
   Resource class for use with StringDialog.java
*/

package arlut.csd.JDialog;

import java.lang.String;

public class booleanThing {

  String booleanLabel;
  boolean Default;

  public booleanThing(String label)
    {
      this(label, false);
    }

  public booleanThing(String label, boolean Default)
    {
       this.booleanLabel = label;
       this.Default = Default;
    }


  public String getLabel()
    {
      return booleanLabel;
    }

  public Boolean getDefault()
    {
      Boolean answer = new Boolean(Default);
      return answer;
    }

  public boolean getValue()
    {
      return Default;
    }
}
