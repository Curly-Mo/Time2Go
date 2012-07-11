package com.curlymo.departurenotifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {

   @Override
   public void onReceive(Context context, Intent intent) {
	  if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("onOffSwitch", false)){
	      //Log.i(Constants.LOG_TAG, "DealBootReceiver invoked, configuring AlarmManager");

	      AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	      PendingIntent pendingIntent =
	               PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceiver.class), 0);
	
	      // use inexact repeating which is easier on battery (system can phase events and not wake at exact times)
	      alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 120000,
	               Constants.ALARM_INTERVAL, pendingIntent);
	   }
   }
}