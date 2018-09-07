package com.lti.ow.lambda.property;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ReadProperty {

	private static final String FILE_PATH = Constants.CONFIG_FILE_PATH;
	
	static final Logger log = Logger.getLogger(ReadProperty.class);
	
	/*
	 * getter for property value in string format
	 */
	public static String getConfig(String key) {
		InputStream input = null;
		
		try {
			input = new FileInputStream(FILE_PATH);
			Properties props = new Properties();
			props.load(input);
			return props.getProperty(key);
		} catch (Exception e) {
			log.error("error while reading property " + e);
		}finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    log.error("error while closing the property reader " + ex);
                }
            }
        }
		return null;
	}

}
