package com.curlymo.departurenotifications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

// Use IntentService which will queue each call to startService(Intent) through onHandleIntent and then shutdown
//
// NOTE that this implementation intentionally doesn't use PowerManager/WakeLock or deal with power issues
// (if the device is asleep, AlarmManager wakes up for BroadcastReceiver onReceive, but then might sleep again)
// (can use PowerManager and obtain WakeLock here, but STILL might not work, there is a gap)
// (this can be mitigated but for this example this complication is not needed)
// (it's not critical if user doesn't see new deals until phone is awake and notification is sent, both)
public class AlarmService extends Service {

	Geocoder myGeocoder;
	private final int RESPONSE_LIMIT = 3;
	LocationManager locationManager;
	LocationListener locationListener;

	//private static int count;

   @Override
   public void onStart(Intent intent, int startId) {
      super.onStart(intent, startId);
   }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
	   
     locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
     locationListener = new myLocationListener();
     //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener,Looper.getMainLooper());
     locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener,Looper.getMainLooper());
     System.out.println("onHandleIntent");
     
	 return START_STICKY;
   }
   
	@Override
	public void onDestroy() {
   	locationManager.removeUpdates(locationListener);
		super.onDestroy();
	     System.out.println("Destroyed");

	}


	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}


   private void createNotification(Event event, String displayString) {
		NotificationManager notificationManager = (NotificationManager) 
				getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.time_flies,
				event.title, System.currentTimeMillis());
		// Hide the notification after its selected
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		Intent intent;
		if(event.location!=null){
			String directionUrl = "http://maps.google.com/maps?daddr=";
			if(event.location.getAddressLine(0)!=null)
				directionUrl+=event.location.getAddressLine(0);
			if(event.location.getAddressLine(1)!=null)
				directionUrl+= " " +event.location.getAddressLine(1);
			if(event.location.getAddressLine(2)!=null)
				directionUrl+= " " +event.location.getAddressLine(2);
		
			intent = new Intent(android.content.Intent.ACTION_VIEW,Uri.parse(directionUrl));
		}else{
			intent = new Intent();
		}
		PendingIntent activity = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.custom_notification);
		//contentView.setImageViewResource(R.id.image, R.drawable.ic_launcher);
		contentView.setTextViewText(R.id.title, event.title);
		contentView.setTextViewText(R.id.text, displayString);
		notification.contentView = contentView;
		notification.contentIntent = activity;
		
		//notification.setLatestEventInfo(this, event.title, message, activity);
		//notification.number += 1;
	    //notification.defaults = Notification.DEFAULT_ALL;
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		String vibratePref = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("vibrate", "");
		if(vibratePref.equals("default")){
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		}else if(vibratePref.equals("long")){
			long[] vibrate = {0,600};
			notification.vibrate = vibrate;
		}else if(vibratePref.equals("short")){
			long[] vibrate = {0, 200};
			notification.vibrate = vibrate;
		}else if(vibratePref.equals("shortshortshort")){
			long[] vibrate = {0, 150, 75, 150, 75, 150};
			notification.vibrate = vibrate;
		}else if(vibratePref.equals("pattern")){
			long[] vibrate = {0, 250, 200, 250, 150, 150, 75, 150, 75, 150};
			notification.vibrate = vibrate;
		}else if(vibratePref.equals("twobits")){
			long[] vibrate = {0,100,200,100,100,100,100,100,200,100,500,100,225, 100};
			notification.vibrate = vibrate;
		}else if(vibratePref.equals("victory")){
			long[] vibrate = {0,50,100,50,100,50,100,400,100,300,100,350,50,200 ,100,100,50,600};
			notification.vibrate = vibrate;
		}
	    notification.sound = Uri.parse(PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("ringtone", ""));
	    //notification.vibrate = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getLong("vibrate", 4);
	    
		notificationManager.notify(event.title.hashCode(), notification);

   }
   
public String getDisplayString(Event event, Date departureTime){
	String display = "";
	int departHours = departureTime.getHours();
	int arriveHours = event.startTime.getHours();
	int estimatedHours = (int) (event.estimatedTime/3600);
	if(!PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("display24Hour", true)){
		if(departHours>12){departHours -=12;}
		if(arriveHours>12){arriveHours -=12;}
		//if(estimatedHours>12){estimatedHours -=12;}
	}
	int departMinutes = departureTime.getMinutes();
	int arriveMinutes = event.startTime.getMinutes();
	int estimatedMinutes = (int) (event.estimatedTime/60)%60;
	
	String departureTimeString;
	String arrivalTimeString;
	if(departMinutes>0){
		departureTimeString = String.format("%2d:%02d", departHours,departMinutes);
	}else{
		departureTimeString = String.format("%02d:%02d", departHours,departMinutes);
	}
	if(departMinutes>0){
		arrivalTimeString = String.format("%2d:%02d", arriveHours,arriveMinutes);
	}else{
		arrivalTimeString = String.format("%02d:%02d", arriveHours,arriveMinutes);
	}
	String travelTimeString = String.format("%02d:%02d", estimatedHours,estimatedMinutes);
    
	display = String.format("Starts-%s", arrivalTimeString);
	if(event.location!=null){
		display += String.format(" Traveltime-%s", travelTimeString);
		display += String.format(" Leave by-%s", departureTimeString);
	}
    
    return display;
}



public int getEstimate(String from, String to){
	int duration;
	StringBuilder urlString = new StringBuilder();
	urlString.append("http://maps.google.com/maps/api/directions/json?");
	urlString.append("origin=");//from
	urlString.append(from);
	urlString.append("&destination=");//to
	urlString.append(to);
	urlString.append("&mode=");//to
	urlString.append(PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("travelMode", "driving"));
	urlString.append("&sensor=false");
	HttpClient httpclient = new DefaultHttpClient();
    HttpGet httpget = new HttpGet(urlString.toString());

    try {

        HttpResponse response = httpclient.execute(httpget);
        String content = convertStreamToString(response.getEntity().getContent());

        JSONObject jsonObject;
		try {
			jsonObject = new JSONObject(content);// parse response into json object
            JSONObject routeObject = jsonObject.getJSONArray("routes").getJSONObject(0); // pull out the "route" object
            JSONObject legObject = routeObject.getJSONArray("legs").getJSONObject(0);
            JSONObject durationObject = legObject.getJSONObject("duration"); // pull out the "duration" object

            String durationString = durationObject.getString("value");
            duration = Integer.valueOf(durationString);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			duration = -1;
		}     
        
    } catch (ClientProtocolException e) {
        // TODO Auto-generated catch block
		duration = -1;
    } catch (IOException e) {
        // TODO Auto-generated catch block
		duration = -1;
    }
    return duration;
}

private static String convertStreamToString(InputStream is) {

    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();

    String line = null;
    try {
        while ((line = reader.readLine()) != null) {
            sb.append((line + "\n"));
        }
    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    return sb.toString();
}


private List<Event> getEvents(){
	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	String[] calendarArray = ListPreferenceMultiSelect.parseStoredValue(prefs.getString("calendars", ""));
	List<String> selectedCalendars;
	if(calendarArray==null){ return new ArrayList<Event>();}
		
	selectedCalendars = Arrays.asList(calendarArray);
	
	HashSet<String> calendarIds = new HashSet<String>();
	
	List<Event> events = new ArrayList<Event>();
    ContentResolver contentResolver = getApplicationContext().getContentResolver();

	// Fetch a list of all calendars synced with the device, their display names and whether the
	// user has them selected for display.
	
	Cursor cursor;
	if (Build.VERSION.SDK_INT >= 14) {
		cursor = contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
				(new String[] { CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.VISIBLE }), null, null, null);
	}else{
		cursor = contentResolver.query(Uri.parse("content://com.android.calendar/calendars"),
				(new String[] { "_id", "displayName", "selected" }), null, null, null);
	}
	
	while (cursor!=null&&cursor.moveToNext()) {

		final String id = cursor.getString(0);
		//final String displayName = cursor.getString(1);
		final Boolean selected = !cursor.getString(2).equals("0");
		
		//System.out.println("Id: " + _id + " Display Name: " + displayName + " Selected: " + selected);
		if(selected && selectedCalendars.contains(id)){
			calendarIds.add(id);
		}
	}
	
	// For each calendar, display all the events from the previous week to the end of next week.		
	for (String id : calendarIds) {
		Uri.Builder builder;
		if (Build.VERSION.SDK_INT >= 14) {
			builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
		}else{
			builder = Uri.parse("content://com.android.calendar/instances/when").buildUpon();
		}
		long now = new Date().getTime();
		ContentUris.appendId(builder, now);
		ContentUris.appendId(builder, now + 3* DateUtils.DAY_IN_MILLIS);

		Cursor eventCursor;
		if (Build.VERSION.SDK_INT >= 14) {
			eventCursor= contentResolver.query(builder.build(),
					new String[] { "title", "begin", "end", "allDay", "eventLocation", "hasAlarm"}, "calendar_id=" + id,
					null, "startDay ASC, startMinute ASC"); 
		}else{
			eventCursor= contentResolver.query(builder.build(),
				new String[] { "title", "begin", "end", "allDay", "eventLocation", "hasAlarm"}, "Calendars._id=" + id,
				null, "startDay ASC, startMinute ASC"); 
		}
		// For a full list of available columns see http://tinyurl.com/yfbg76w
		while (eventCursor.moveToNext()) {
			final String title = eventCursor.getString(0);
			final Date begin = new Date(eventCursor.getLong(1));
			//final Date end = new Date(eventCursor.getLong(2));
			//final Boolean allDay = !eventCursor.getString(3).equals("0");
			String location = eventCursor.getString(4);
			Address address = null;
			
			myGeocoder = new Geocoder(this, Locale.getDefault());
	        int responseCount = 0;
			List<Address> addressList = null;
	        // geocoder retry loop when google has issues
	        while (location!=null && addressList == null && responseCount <= RESPONSE_LIMIT) {
	            try {
	              // populate address list from query and return
	                addressList = myGeocoder.getFromLocationName(location, 1);
	            } catch (SocketTimeoutException e) {
	                addressList = null;
	            }  catch (IOException e) {
	                addressList = null;
	            }  	
	            if(responseCount == RESPONSE_LIMIT) {
	                //return n;
	            } else {
	                responseCount++;
	            }
	        }
	        if(addressList==null ||addressList.isEmpty()){
	        	
	        }else{
	        	address = addressList.get(0);
	        	location = address.toString();
	        }
			
	        events.add(new Event(title,begin,address));
			//System.out.println("Title: " + title + " Begin: " + begin + "Location:" + location);
		}
	}
	
	return events;
}




class myLocationListener implements LocationListener{

	public void onLocationChanged(Location location) {
		System.out.println("location Chang-ed");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		long WarningTime = Long.parseLong(prefs.getString("warningTime", String.valueOf(Constants.FIVE_MINUTES)));
		long NoLocationWarningTime = Long.parseLong(prefs.getString("noLocationWarningTime", String.valueOf(0)));
		List<Event> events = getEvents();
    	String geoFrom = location.getLatitude() + "," + location.getLongitude();

        for(Event event : events){
        	long estimatedTime;
        	if(event.location!=null){
        		
	        	String geoTo = event.location.getLatitude() + "," + event.location.getLongitude();
	            estimatedTime = getEstimate(geoFrom,geoTo);
	            event.setEstimate(estimatedTime);
        	}  
        	else{
        		if(NoLocationWarningTime == 0){
        			continue;
        		}
        		if(WarningTime < NoLocationWarningTime)
        			estimatedTime = (NoLocationWarningTime - WarningTime);
        		else
        			estimatedTime = NoLocationWarningTime;
        		estimatedTime = estimatedTime/1000;
        		event.setEstimate(estimatedTime);
        	}
        	
	            Date now = new Date();
	            long timeUntil = (event.startTime.getTime() - now.getTime());
	            long timeUntilDeparture = (timeUntil - estimatedTime*1000);
	            Date departureTime = new Date(event.startTime.getTime() - estimatedTime*1000);	

	            
	            if(timeUntilDeparture > 0){
		            if(timeUntilDeparture <= WarningTime + Constants.MINUTE){
		        		SharedPreferences pref = getSharedPreferences(Constants.PREFERENCE_FILE,MODE_PRIVATE);
		            	if(!pref.contains(event.uniqueID)){
		            		createNotification(event, getDisplayString(event,departureTime));
		            		pref.edit().putBoolean(event.uniqueID, true).commit();
		            	}
		            }
		            else if(timeUntilDeparture < Constants.ALARM_INTERVAL + WarningTime){
		            	scheduleAlarmReceiver((timeUntilDeparture - WarningTime)/2);
		            }
	            }
	            else{
	        		SharedPreferences pref = getSharedPreferences(Constants.PREFERENCE_FILE,MODE_PRIVATE);
	        		if(pref.contains(event.uniqueID))
	        			pref.edit().remove(event.uniqueID).commit();
	            }
        		//createNotification(event, getDisplayString(event,departureTime));

	            System.out.println(event.title + ": estimate=" +estimatedTime + " timeUntil=" + (event.startTime.getTime() - now.getTime()));
	            System.out.println("TimeUntilDeparture: " + timeUntilDeparture);


        	
        }
        	
		locationManager.removeUpdates(this);
		stopSelf();
	}
	public void onProviderDisabled(String provider) {
	}
	public void onProviderEnabled(String provider) {
	}
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
 
 }


   private void scheduleAlarmReceiver(long scheduleTime) {
	      AlarmManager alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
	      PendingIntent pendingIntent =
	               PendingIntent.getBroadcast(this, 0, new Intent(this, AlarmReceiver.class),
	                        PendingIntent.FLAG_ONE_SHOT);
	      long triggerAtTime = SystemClock.elapsedRealtime() +scheduleTime;
	      alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pendingIntent);
   }
   


}
