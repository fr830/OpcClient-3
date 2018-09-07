package com.lti.ow.lambda.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lti.ow.lambda.property.Constants;
import com.lti.ow.lambda.property.ReadProperty;

public class RestClient {
	
	static final Logger log = Logger.getLogger(RestClient.class);
	
	public static boolean restCall(String logType, String fileName, String bucket, String path) {
		String apiURL = ReadProperty.getConfig(Constants.KEY_CSV_LOG_VERIFIER_API);
		String requestFormat = ReadProperty.getConfig(Constants.KEY_CSV_LOG_VERIFIER_REQ_FORMAT);
		String requestBody = String.format(requestFormat, logType, fileName, bucket, path);
		
		try {
			URL url = new URL(apiURL);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "application/json");
			
			OutputStream output = conn.getOutputStream();
			output.write(requestBody.getBytes());
			output.flush();
			
			if(conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				log.error("http response is not OK " + conn.getResponseCode());
				return false;
			}
			
			InputStream in = conn.getInputStream();
			JsonNode node = new ObjectMapper().readTree(in);
			
			if(node.get(Constants.CSV_LOG_VERIFIER_API_RESPONSE_FIELD).asText()
					.equals(Constants.CSV_LOG_VERIFIER_API_RESPONSE_FIELD_VALUE)) {
				log.info("response received is " + node);
				return true;
			}else {
				log.error("error while calling the rest api " + node);
				return false;
			}
			
		}catch(Exception e) {
			log.error("error while calling the rest api " + e);
		}
		
		return false;
	}
}
