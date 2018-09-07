package com.lti.ow.lambda.pojo;

public class Timeseries {
	
	private String tagValue;
	private String timestamp;
	private Integer tagId;
	private String status;
	
	public String getTagValue() {
		return tagValue;
	}
	public void setTagValue(String value) {
		this.tagValue = value;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
/*	public String getTagName() {
		return tagName;
	}
	public void setTagName(String tagName) {
		this.tagName = tagName;
	}*/
	public Integer getTagId() {
		return tagId;
	}
	public void setTagId(Integer tagId) {
		this.tagId = tagId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	

}
