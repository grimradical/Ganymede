/*

   IRISLink.java

   This class uses JDBC to connect to ARL:UT's Oracle databases to
   pull data from IRIS.

   Created: 6 June 2009

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2009
   The University of Texas at Austin

   Contact information

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

package arlut.csd.ganymede.gasharl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.sql.*;
import javax.sql.*;
import com.mchange.v2.c3p0.*;

import arlut.csd.ganymede.server.Ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        IRISLink

------------------------------------------------------------------------------*/

/**
 * This class uses JDBC to connect to ARL:UT's Oracle databases to
 * pull data from IRIS, so that we can do verifications for unique
 * user id over historical time, pull biographical details from the HR
 * databases, and etc.
 *
 * This class is obviously very specific to the ARL:UT environment,
 * and I don't imagine it would ever be of use to outside parties.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class IRISLink {

  static private boolean debug = false;
  static private ComboPooledDataSource source = null;
  static private Pattern numericBadgePattern = Pattern.compile("^\\d+$");

  /* -- */

  private synchronized static ComboPooledDataSource getSource()
  {
    if (source != null)
      {
	return source;
      }

    source = new ComboPooledDataSource();

    try
      {
	source.setDriverClass("oracle.jdbc.OracleDriver");
      }
    catch (java.beans.PropertyVetoException ex)
      {
	throw new RuntimeException(ex);
      }

    String debugProperty = System.getProperty("iris.debug");

    if (debugProperty != null)
      {
	debug = true;
      }

    String hostProperty = System.getProperty("iris.host");

    if (hostProperty == null)
      {
	throw new NullPointerException("iris.host not found");
      }

    String portProperty = System.getProperty("iris.port");

    if (portProperty == null)
      {
	throw new NullPointerException("iris.port not found");
      }

    String schemaProperty = System.getProperty("iris.schema");

    if (schemaProperty == null)
      {
	throw new NullPointerException("iris.schema not found");
      }

    StringBuffer url = new StringBuffer("jdbc:oracle:thin:");

    url.append("@");
    url.append(hostProperty);
    url.append(":");
    url.append(portProperty);
    url.append(":");
    url.append(schemaProperty);

    if (debug)
      {
	System.err.println("**** ---- ****");
	System.err.println("**** URL is " + url + " ****");
	System.err.println("**** ---- ****");
      }

    source.setJdbcUrl(url.toString());

    String usernameProperty = System.getProperty("iris.username");

    if (usernameProperty == null)
      {
	throw new NullPointerException("iris.username not found");
      }

    source.setUser(usernameProperty);

    String passwordProperty = System.getProperty("iris.password");

    if (passwordProperty == null)
      {
	throw new NullPointerException("iris.password not found");
      }

    source.setPassword(passwordProperty);

    source.setInitialPoolSize(3);
    source.setMinPoolSize(3);
    source.setMaxPoolSize(10);
    source.setMaxStatements(5);

    return source;
  }

  private static Connection getConnection()
  {
    try
      {
	return getSource().getConnection();
      }
    catch (SQLException ex)
      {
	ex.printStackTrace();

	return null;
      }
  }

  /**
   * This method returns a badge code for the given username, if that
   * username is either currently in use or was ever in use over the
   * period covered by the IRIS database record.
   *
   * We expect that truly old user names will eventually be aged out
   * of the database, but we want to avoid re-using usernames for at
   * least 5 years.
   *
   * If the username was not found in the database, a null value will
   * be returned.
   */

  public static String findHistoricalBadge(String username)
  {
    Connection myConn = null;

    try
      {
	myConn = getConnection();

	String queryString = "select BADGE_NUMBER from HR_EMPLOYEES_GA_VW where NETWORK_USER_ID = ?";
	PreparedStatement queryName = myConn.prepareStatement(queryString);

	queryName.setString(1, username);

	ResultSet rs = queryName.executeQuery();

	try
	  {
	    if (rs.next())
	      {
		return rs.getString(1);
	      }
	    else
	      {
		return null;
	      }
	  }
	finally
	  {
	    rs.close();
	  }
      }
    catch (Exception ex)
      {
	ex.printStackTrace();

	throw new RuntimeException(ex);
      }
    finally
      {
	try
	  {
	    myConn.close();
	  }
	catch (SQLException ex)
	  {
	  }
      }
  }

  /**
   * This method returns a username for the given badge code, if that
   * badge code is on record in the IRIS database and has a network id
   * (a Ganymede username) associated with it.
   *
   * We expect that truly old user names will eventually be aged out
   * of the database, but we want to avoid re-using usernames for at
   * least 5 years.
   *
   * If the badge code was not found in the database, or if no network
   * id was retained for that badge code, a null value will be
   * returned.
   */

  public static String findHistoricalUsername(String badge)
  {
    Connection myConn = null;

    try
      {
	myConn = getConnection();

	String queryString = "select NETWORK_USER_ID from HR_EMPLOYEES_GA_VW where BADGE_NUMBER = ?";
	PreparedStatement queryName = myConn.prepareStatement(queryString);

	queryName.setString(1, badge);

	ResultSet rs = queryName.executeQuery();

	try
	  {
	    if (rs.next())
	      {
		return rs.getString(1);
	      }
	    else
	      {
		return null;
	      }
	  }
	finally
	  {
	    rs.close();
	  }
      }
    catch (SQLException ex)
      {
	ex.printStackTrace();

	throw new RuntimeException(ex);
      }
    finally
      {
	try
	  {
	    myConn.close();
	  }
	catch (SQLException ex)
	  {
	  }
      }
  }

  /**
   * This method returns true if the given username is either not
   * present in the IRIS database, or if it is present and the badge
   * identifier matches.
   *
   * This method will return false if the given username is present in
   * the database with a different badge code.
   */

  public static boolean okayToUseName(String username, String badge)
  {
    String badgeString = findHistoricalBadge(username);

    if (badgeString == null)
      {
	return true;
      }

    if (badge.equals(badgeString))
      {
	return true;
      }

    // Okay, we don't have a badge string match.  Is this due to one
    // or more leading 0's?  Convert to integers and see if we have
    // equal values.

    Matcher m = numericBadgePattern.matcher(badgeString);
    Matcher m2 = numericBadgePattern.matcher(badge);

    if (m.matches() && m2.matches())
      {
	try
	  {
	    int historicalInt = Integer.valueOf(badgeString);
	    int currentInt = Integer.valueOf(badge);

	    if (historicalInt == currentInt)
	      {
		return true;
	      }
	  }
	catch (NumberFormatException ex)
	  {
	    Ganymede.logError(ex);

	    throw ex;
	  }
      }

    return false;
  }

  public static void main(String args[])
  {
    debug = true;

    IRISLink.okayToUseName(args[0], args[1]);
  }
}
