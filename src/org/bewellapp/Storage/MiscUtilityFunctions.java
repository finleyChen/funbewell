package org.bewellapp.Storage;

import java.util.Calendar;
import java.text.SimpleDateFormat;

public class MiscUtilityFunctions {
  public static final String DATE_FORMAT_NOW = "yyyy-MM-dd_HH-mm-ss";

  //get the current date
  public static String now() {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
    return sdf.format(cal.getTime());
  }

}