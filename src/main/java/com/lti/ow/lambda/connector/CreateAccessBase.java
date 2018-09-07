package com.lti.ow.lambda.connector;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.da.AccessBase;
import org.openscada.opc.lib.da.AddFailedException;
import org.openscada.opc.lib.da.DataCallback;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.ItemState;
import org.openscada.opc.lib.da.SyncAccess;

import com.lti.ow.lambda.dao.SqliteOps;
import com.lti.ow.lambda.forward.Publisher;
import com.lti.ow.lambda.forward.SQSOperation;
import com.lti.ow.lambda.property.Constants;
import com.lti.ow.lambda.property.ReadProperty;
import com.lti.ow.lambda.property.ReadTagList;
import com.lti.ow.lambda.util.AlertJson;
import com.lti.ow.lambda.util.Formator;
import com.lti.ow.lambda.util.TimeseriesToJson;

public final class CreateAccessBase {

	private static AccessBase digitalAccess = null;
	private static AccessBase analogAccess = null;
	private static Map<String, String> tagValue = null;
	private static int analogTagCounter = 0;
	private static int analogTagCounterTracker = 0;
	private static int digitalTagCounter = 0;
	private static int digitalTagCounterTracker = 0;
	private static String lastPurgeDate = null;
	static final Logger log = Logger.getLogger(CreateAccessBase.class);

	/*
	 * preparing separate AccessBase objects for digital and analog tags
	 */
	public static void setAccessBase() {
		if (!OpcConnector.isConnected()) {
			return;
		}
		
		try {
			log.info("creating AccessBase object for digital and analog");

			digitalAccess = new SyncAccess(OpcConnector.getServer(),
					Integer.valueOf(ReadProperty.getConfig(Constants.KEY_TAG_READ_INTERVAL_DIGITAL)));
			analogAccess = new SyncAccess(OpcConnector.getServer(),
					Integer.valueOf(ReadProperty.getConfig(Constants.KEY_TAG_READ_INTERVAL_ANALOG)));

			digitalAccess.bind();
			analogAccess.bind();

			List<String> tagList = ReadTagList.getScadaTagList();
			if (tagList.isEmpty()) {
				log.warn("tag list provided is empty");
				return;
			}

			for (Short i = 0; i <= 1; i++) {
				tagList = addTags(tagList);

				if (tagList.isEmpty()) {
					break;
				}
			}

			if (!tagList.isEmpty()) {
				for (String tagName : tagList) {
					log.warn("could not add: " + tagName);
				}
			}
		} catch (Exception e) {
			log.error("error while creating accessBase " + e);
			SQSOperation.pushToQueue(AlertJson.getAlertJson(e));
		}
	}
	
	/*
	 * adding tags into digital and analog AccessBase objects
	 */
	private static List<String> addTags(List<String> tagList) {
		List<String> missedTagList = new ArrayList<String>();
		tagValue = new HashMap<String, String>();
		
		try {
			for(String tagName : tagList) {
				tagValue.put(tagName, null);

				if (ReadTagList.getTagTypeMap().get(tagName).contains(ReadProperty.getConfig(Constants.KEY_TAG_IDENTIFIER_DIGITAL)) ||
						ReadTagList.getTagTypeMap().get(tagName).contains(ReadProperty.getConfig(Constants.KEY_TAG_IDENTIFIER_PUMP))) {
					digitalTagCounter = digitalTagCounter + 1;
					try {
						digitalAccess.addItem(tagName, new DataCallback() {
							@Override
							public void changed(Item item, ItemState itemState) {
								validatePrepareAndSendDigital(item, itemState);
							}
						});
					} catch(AddFailedException e) {
						digitalTagCounter = digitalTagCounter - 1;
						digitalAccess.removeItem(tagName);
						missedTagList.add(tagName);
						log.warn(e + " retrying to add: " + tagName);
					}
					
				} else if(ReadTagList.getTagTypeMap().get(tagName).contains(ReadProperty.getConfig(Constants.KEY_TAG_IDENTIFIER_ANALOG))) {
					analogTagCounter = analogTagCounter + 1;
					try {
						analogAccess.addItem(tagName, new DataCallback() {
							@Override
							public void changed(Item item, ItemState itemState) {
								validatePrepareAndSendAnalog(item, itemState);
								purgeDB();
							}
						});	
					}catch(AddFailedException e) {
						analogTagCounter = analogTagCounter - 1;
						analogAccess.removeItem(tagName);
						missedTagList.add(tagName);
						log.warn(e + " retrying to add: " + tagName);
					}
				}else {
					log.warn(tagName + " is not valid"); 
				}
			}
		} catch (Exception e) {
			log.error("error while adding tags " + e);
			SQSOperation.pushToQueue(AlertJson.getAlertJson(e));
		}
		
		return missedTagList;
	}

	
	/*
	 * getter for digital Access object
	 */
	public static AccessBase getDigitalAccess() {
		if (digitalAccess == null) {
			setAccessBase();
		}
		return digitalAccess;
	}

	
	/*
	 * getter for analog Access object
	 */
	public static AccessBase getAnalogAccess() {
		if (analogAccess == null) {
			setAccessBase();
		}
		return analogAccess;
	}
	
	
	/*
	 * check for the value if it has changed
	 */
	private static boolean hasValueChanged(Item item, String value) {
		if (value.equals((tagValue.get(item.getId())))) {
			return false;
		} else {
			tagValue.put(item.getId(), value);
			return true;
		}
	}
	
	
	/*
	 * validate quality
	 */
	private static boolean isValid(Item item, ItemState itemState) {
		String value = null;
		try {
			value = Formator.getParsedValue(itemState);
		} catch (JIException e) {
			log.error("error while parsing value " + e);
		}
		
		if(value != null && hasValueChanged(item, value)) {
			if (ReadTagList.getTagTypeMap().get(item.getId()).contains(ReadProperty.getConfig(Constants.KEY_TAG_IDENTIFIER_PUMP))) {
				return true;
			}
			
			if (itemState.getQuality().intValue() == Integer.valueOf(ReadProperty.getConfig(Constants.KEY_TAG_THRESHOLD_QUALITY))) {
				return true;
			}
			
		}else {
			return false;
		}
		
		return false;
	}

	
	/*
	 * helper for preparing digital data and publishing to IoT hub
	 */
	private static void validatePrepareAndSendDigital(Item item, ItemState itemState) {
		digitalTagCounterTracker = digitalTagCounterTracker + 1;
		
		if(isValid(item, itemState)) {
			TimeseriesToJson.convertToJson(item, itemState, true);
		}
		
		if (digitalTagCounterTracker == digitalTagCounter || digitalTagCounterTracker == Integer.valueOf(ReadProperty.getConfig(Constants.KEY_TAG_THRESHOLD_JSON_ARRAY))) {
			
			if(digitalTagCounterTracker == digitalTagCounter) {
				digitalTagCounterTracker = 0;
			}
			
			String digitalPayload = TimeseriesToJson.toBatchPayload(true);
			
			if(digitalPayload != null) {
				log.info("sending payload for digital ");
				Publisher.publishToGreengrass(digitalPayload, ReadProperty.getConfig(Constants.KEY_TAG_PUBLISH_TOPIC_DIGITAL));
				TimeseriesToJson.clearDigitalTimeseriesList();
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					log.error("error while creating digital batch" + e);
				}
			}
		}
	}

	
	/*
	 * helper for preparing analog data and publishing to IoT hub
	 */
	private static void validatePrepareAndSendAnalog(Item item, ItemState itemState) {
		analogTagCounterTracker = analogTagCounterTracker + 1;
		
		if(isValid(item, itemState)) {
			TimeseriesToJson.convertToJson(item, itemState, false);
		}
		
		if (analogTagCounterTracker == analogTagCounter || analogTagCounterTracker == Integer.valueOf(ReadProperty.getConfig(Constants.KEY_TAG_THRESHOLD_JSON_ARRAY))) {
			
			if(analogTagCounterTracker == analogTagCounter) {
				analogTagCounterTracker = 0;
			}
			
			String analogPayload = TimeseriesToJson.toBatchPayload(false);
			
			if(analogPayload != null) {
				log.info("sending payload for analog ");
				Publisher.publishToGreengrass(analogPayload, ReadProperty.getConfig(Constants.KEY_TAG_PUBLISH_TOPIC_ANALOG));
				TimeseriesToJson.clearAnalogTimeseriesList();
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					log.error("error while creating analog batch" + e);
				}
			}
		}
	}
	
		
	/*
	 * Purge database for data older than purge date
	 */
	private static void purgeDB() {
		String purgeDate = LocalDateTime.now().
							minusDays(Integer.valueOf(ReadProperty.getConfig(Constants.KEY_SQLITE_DB_PURGE_INTERVAL))).
							format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		
		if(!purgeDate.equals(lastPurgeDate)) {
			log.info("purging data older than " + purgeDate);
			new SqliteOps().deleteRow(purgeDate);
			lastPurgeDate = purgeDate;
		}
	}
}
