package com.curlymo.departurenotifications;

import java.util.Date;

import android.location.Address;

public class Event {
	String title;
	Date startTime;
	Address location;
	long estimatedTime;//time of departure for Public Transit
	Date departureTime;
	String uniqueID;
	Boolean isTransit;
	Event(String tit, Date start, Address loc){
		title = tit;
		startTime = start;
		location = loc;
		uniqueID = title + startTime;
		isTransit = false;
	}

	public void setEstimate(long estimatedTime2){
		estimatedTime = estimatedTime2;
	}
	public void setTransit(Boolean transit){
		isTransit = transit;
	}
	public void setDepartureTime(Date departure){
		departureTime = departure;
	}
}
