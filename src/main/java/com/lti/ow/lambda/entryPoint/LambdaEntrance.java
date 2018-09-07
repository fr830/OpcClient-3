package com.lti.ow.lambda.entryPoint;

import org.apache.log4j.PropertyConfigurator;

import com.amazonaws.services.lambda.runtime.Context;
import com.lti.ow.lambda.property.Constants;
import com.lti.ow.lambda.property.ReadProperty;

public class LambdaEntrance {
	/*
	 * lambda starting point
	 */
	static {
		PropertyConfigurator.configure(ReadProperty.getConfig(Constants.KEY_LOG4J_CONFIG_PATH));
		OpcReader.startReading();
	}
	
	public void handler(Object input, Context context) {}
	
	public static void main(String[] args) {}

}
