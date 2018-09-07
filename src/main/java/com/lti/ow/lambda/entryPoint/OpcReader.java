package com.lti.ow.lambda.entryPoint;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;

import org.apache.log4j.Logger;
import com.lti.ow.lambda.connector.CreateAccessBase;
import com.lti.ow.lambda.connector.OpcConnector;
import com.lti.ow.lambda.connector.OverallEfficiency;
import com.lti.ow.lambda.property.Constants;
import com.lti.ow.lambda.property.ReadProperty;
import com.lti.ow.lambda.util.CsvLogTimerTask;

public class OpcReader {
	
	static final Logger log = Logger.getLogger(OpcReader.class);
	
	private OpcReader() {
		/*Not accessible outside*/
	}
	
	/*
	 * execution sequence stater
	 */
	public static void startReading() {
		log.info("application intialisation has started.....");
		
		OpcConnector.autoConnect();
		
		while(!OpcConnector.isConnected()) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log.error("error while making connection with OPC server", e);
			}
		}
		
		CreateAccessBase.setAccessBase();
		OverallEfficiency.setOverallEffAccess();
		//createCsvLogScheduler();
		
		try {
			while(true) {
				Thread.sleep(Long.MAX_VALUE);
			}
		}catch(InterruptedException e) {
			log.error("error while starting to read " + e);
		}
	}
	
	private static void createCsvLogScheduler() {
		Calendar startTime = Calendar.getInstance();
		startTime.set(Calendar.HOUR_OF_DAY, Integer.valueOf(ReadProperty.getConfig(Constants.KEY_CSV_LOG_HOURS)));
		startTime.set(Calendar.MINUTE, Integer.valueOf(ReadProperty.getConfig(Constants.KEY_CSV_LOG_MINUTES)));
		
		if(new Date().after(startTime.getTime())) {
			startTime.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			log.error("error while creating csv log scheduler " + e);
		}
		
		new Timer().schedule(new CsvLogTimerTask(), startTime.getTime(), Long.valueOf(ReadProperty.getConfig(Constants.KEY_CSV_LOG_MILIS_INTERVAL)));
		
		log.info("csv log scheduler has been scheduled for " + startTime.getTime().toString());
	}
}
