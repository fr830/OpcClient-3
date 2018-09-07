package com.lti.ow.lambda.pojo;

import java.util.List;

public class CloudPayload {
	private String timestamp;
	private List<Timeseries> timeSeriesList;
	
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public List<Timeseries> getTimeSeriesList() {
		return timeSeriesList;
	}
	public void setTimeSeriesList(List<Timeseries> timeSeriesList) {
		this.timeSeriesList = timeSeriesList;
	}
	

}
