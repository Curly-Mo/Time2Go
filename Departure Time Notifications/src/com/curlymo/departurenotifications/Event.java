package com.curlymo.departurenotifications;

import java.util.Date;

import android.location.Address;

public class Event {
	String title;
	Date startTime;
	Address location;
	long estimatedTime;
	String uniqueID;
	Event(String tit, Date start, Address loc){
		title = tit;
		startTime = start;
		location = loc;
		uniqueID = title + startTime;
	}

	public void setEstimate(long estimatedTime2){
		estimatedTime = estimatedTime2;
	}
}
