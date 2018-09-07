package com.lti.ow.lambda.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.log4j.Logger;

import com.lti.ow.lambda.property.Constants;
import com.lti.ow.lambda.property.ReadProperty;

public class AlertJson {
	
	static final Logger log = Logger.getLogger(AlertJson.class);
	
	private static final String ALERT_FORMAT =  ReadProperty.getConfig(Constants.KEY_SQS_MESSAGE_FORMAT);
	
	public static String getAlertJson() {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.TIMESTAMP_FORMAT));
		log.info("sending alert to sqs ");
		return String.format(ALERT_FORMAT, "Connection lost with SCADA", "Unknown", timestamp, "System", "None", "Critical", "None", "None");
		
	}
	
	
	public static String getAlertJson(Exception e) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.TIMESTAMP_FORMAT));
		String alertDesc = e.getMessage().toString();
		String alertReason = e.getCause().toString();
		
		log.info("sending alert to sqs " + alertDesc +" "+ alertReason);
		
		return String.format(ALERT_FORMAT, alertDesc, alertReason, timestamp, "System", "None", "Critical", "None", "None");
	}

}
