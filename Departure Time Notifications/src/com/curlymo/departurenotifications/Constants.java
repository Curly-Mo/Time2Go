package com.curlymo.departurenotifications;

import android.app.AlarmManager;
import android.text.format.DateUtils;

public class Constants {


   public static final String PREFERENCE_FILE = "myPreferenceFile";
   
   
   //public static final long WARNING_TIME = 5*DateUtils.MINUTE_IN_MILLIS;
   public static final long FIVE_MINUTES = 5*DateUtils.MINUTE_IN_MILLIS;
   public static final long MINUTE = DateUtils.MINUTE_IN_MILLIS;
   
   // In real life, use AlarmManager.INTERVALs with longer periods of time 
   // for dev you can shorten this to 10000 or such, but deals don't change often anyway
   // (better yet, allow user to set and use PreferenceActivity)
   //public static final long ALARM_INTERVAL = 30000;
  // public static final long ALARM_INTERVAL = AlarmManager.INTERVAL_HALF_HOUR;
   public static final long ALARM_INTERVAL = AlarmManager.INTERVAL_HOUR;
   //public static final long ALARM_TRIGGER_AT_TIME = SystemClock.elapsedRealtime() + 30000;

}