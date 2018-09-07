package com.lti.ow.lambda.pojo;

public class Tags {
	
	private String scadaTag;
	private String tagId;
	private String type;

	public Tags() {
		//Default constructor required for csv reader 
	}
	
	public Tags(String scadaTag, String tagId, String type) {
		this.scadaTag = scadaTag;
		this.tagId = tagId;
		this.type = type;
	}

	public String getScadaTag() {
		return scadaTag;
	}

	public void setScadaTag(String scadaTag) {
		this.scadaTag = scadaTag;
	}

	public String getTagId() {
		return tagId;
	}

	public void setTagId(String tagId) {
		this.tagId = tagId;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
