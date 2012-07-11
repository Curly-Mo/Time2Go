package com.curlymo.departurenotifications;

import java.util.ArrayList;
import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.CalendarContract;
 
public class SettingsActivity extends PreferenceActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                addPreferencesFromResource(R.xml.preferences);
                setContentView(R.layout.preference_layout);
                
                setCalendarPreference();

                final Preference myPref = (Preference) findPreference("onOffSwitch");
                
                if(myPref.getSharedPreferences().getBoolean("onOffSwitch", false)){
                	scheduleAlarmReceiver();
                }
                
                myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                             public boolean onPreferenceClick(Preference preference) {
                                 if(!myPref.getSharedPreferences().getBoolean("onOffSwitch", true)){
                                	 System.out.println("turned off");

                                     PendingIntent pendingIntent =
                                             PendingIntent.getBroadcast( getApplicationContext(), 0, new Intent(getApplicationContext(), AlarmReceiver.class),
                                                      PendingIntent.FLAG_CANCEL_CURRENT);
                                     pendingIntent.cancel();
                                 }
                                 else{
                                	 System.out.println("turned on");

                                     scheduleAlarmReceiver();
                                 }
								return true;
                             }
                         });
                
        }
        
        private void setCalendarPreference(){
        	List<String> calendarIds = new ArrayList<String>();
        	List<String> calendarNames = new ArrayList<String>();
            ContentResolver contentResolver = getApplicationContext().getContentResolver();
            Cursor cursor;
        	if (Build.VERSION.SDK_INT >= 14) {
        		cursor = contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
        				(new String[] { CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.VISIBLE }), null, null, null);

        	}else{
        		cursor = contentResolver.query(Uri.parse("content://com.android.calendar/calendars"),
        			(new String[] { "_id", "displayName", "selected" }), null, null, null);
        	}

        	if(cursor==null){
        		calendarIds.add("0");
        		calendarNames.add("Unable to read Calendar");
        		calendarIds.add("1");
        		calendarNames.add("Please let me know what version of");
        		calendarIds.add("2");
        		calendarNames.add("Android you have so I can fix this issue.");
        	}
        	else{
	        	while (cursor.moveToNext()) {
	
	        		final String id = cursor.getString(0);
	        		final String displayName = cursor.getString(1);
	        		final Boolean selected = !cursor.getString(2).equals("0");
	        		
	        		//System.out.println("Id: " + _id + " Display Name: " + displayName + " Selected: " + selected);
	        		if(selected){
	        			calendarIds.add(id);
	        			calendarNames.add(displayName);
	        		}
	        	}	
        	}
        	
            CharSequence[] entries = calendarNames.toArray(new CharSequence[calendarNames.size()]);
            CharSequence[] entryValues = calendarIds.toArray(new CharSequence[calendarIds.size()]);

            ListPreferenceMultiSelect lp = (ListPreferenceMultiSelect) findPreference("calendars");
            //lp.setDefaultValue(entryValues);
            lp.setEntries(entries);
            lp.setEntryValues(entryValues);
            
            //setDefault
        	if(lp.getValue().equals("totallyawesomedefaultstring")){
	        	StringBuffer value = new StringBuffer();
	        	for ( int i=0; i<entryValues.length; i++ ) {
	        		value.append(entryValues[i]).append(ListPreferenceMultiSelect.SEPARATOR);
	        	}
	        	String val = value.toString();
	        	if ( val.length() > 0 )
	        		val = val.substring(0, val.length()-ListPreferenceMultiSelect.SEPARATOR.length());
	        	lp.setValue(val);
        	}
            
        }
        

        
       /* private List<String> getCalendarsICS(String[] projection){
        	List<String> values = new ArrayList<String>();
        	
    		Cursor cursor = getApplicationContext().getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, projection,null,null,null);
            try {
                while (cursor != null && cursor.moveToNext()) {
                    values.add(cursor.getString(0));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        	
        	return values;
        }*/
        
        
        
        // Schedule AlarmManager to invoke AlarmReceiver and cancel any existing current PendingIntent
        // we do this because we *also* invoke the receiver from a BOOT_COMPLETED receiver
        // so that we make sure the service runs either when app is installed/started, or when device boots
        private void scheduleAlarmReceiver() {
           AlarmManager alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
           PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(this, 0, new Intent(this, AlarmReceiver.class),
                             PendingIntent.FLAG_CANCEL_CURRENT);

           // Use inexact repeating which is easier on battery (system can phase events and not wake at exact times)
           alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 60000*2,
                    Constants.ALARM_INTERVAL, pendingIntent);
        }
        
}