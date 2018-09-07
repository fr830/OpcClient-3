package com.lti.ow.lambda.forward;

import org.apache.log4j.Logger;

import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.lti.ow.lambda.property.Constants;
import com.lti.ow.lambda.property.ReadProperty;

public class SQSOperation {
	
	private static AmazonSQS sqsClient = null;
	private static final String URL = ReadProperty.getConfig(Constants.KEY_AWS_SQS_QUEUE_URL);
	static final Logger log = Logger.getLogger(SQSOperation.class);
	
	static {
        sqsClient = AmazonSQSClientBuilder.standard()
				.withRegion(ReadProperty.getConfig(Constants.KEY_AWS_REGION))
				.withCredentials(new PropertiesFileCredentialsProvider(ReadProperty.getConfig(Constants.KEY_AWS_SQS_CREDENTIAL_PATH)))
				.build();
	}
	
	
	/*
	 * Send message to queue
	 */
	public static void pushToQueue(String message) {
        log.info("Sending a message ");
        //sqsClient.sendMessage(new SendMessageRequest(SQSOperation.URL, message));
	}
	
}
