package com.lti.ow.lambda.forward;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.amazonaws.greengrass.javasdk.IotDataClient;
import com.amazonaws.greengrass.javasdk.model.PublishRequest;
import com.lti.ow.lambda.util.AlertJson;

public class Publisher {
	
	static final Logger log = Logger.getLogger(Publisher.class);
		
	//publish payload to IoT hub via Greengrass
	public static void publishToGreengrass(String payload, String topic) {
		PublishRequest publishRequest = new PublishRequest().withTopic(topic)
							.withPayload(ByteBuffer.wrap(payload.getBytes()));
		
		log.info("publish data to IoT endpoint with topic " + topic + " and payload " + payload);
		
		try {
			//new IotDataClient().publish(publishRequest);
		} catch (Exception ex) {
			log.error("error while publish data " + ex);
			SQSOperation.pushToQueue(AlertJson.getAlertJson(ex));
		}
	}

}
