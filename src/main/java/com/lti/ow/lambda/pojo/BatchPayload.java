package com.lti.ow.lambda.pojo;

import java.util.List;

public class BatchPayload {
	private String timestamp;
	private String batchType;
	private List<Timeseries> timeSeriesList;
	
	public String getBatchType() {
		return batchType;
	}
	public void setBatchType(String batchType) {
		this.batchType = batchType;
	}
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
