package com.lti.ow.lambda.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.ItemState;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lti.ow.lambda.dao.SqliteOps;
import com.lti.ow.lambda.forward.SQSOperation;
import com.lti.ow.lambda.pojo.BatchPayload;
import com.lti.ow.lambda.pojo.BatchRecord;
import com.lti.ow.lambda.pojo.Timeseries;
import com.lti.ow.lambda.property.Constants;
import com.lti.ow.lambda.property.ReadProperty;
import com.lti.ow.lambda.property.ReadTagList;

public class TimeseriesToJson {

	private static List<Timeseries> analogTimeseriesList = new ArrayList<Timeseries>();
	private static List<Timeseries> digitalTimeseriesList = new ArrayList<Timeseries>();
	private static List<BatchRecord> batchRecordList = new ArrayList<BatchRecord>();
	private static Map<String, Integer> tagReadCounter = new HashMap<String, Integer>();
	private static Integer analogBatchTagCounter = 0;
	private static Integer digitalBatchTagCounter = 0;
	static final Logger log = Logger.getLogger(TimeseriesToJson.class);
	
	
	/* 
	 * prepare payload to be published at IoT hub
	 */
	public static String toBatchPayload(boolean isDigital) {
		log.info("creating payload for IoT enpoint...");
		
		BatchPayload batchPayload = new BatchPayload();
		BatchRecord batchRecord = new BatchRecord();
		
		if(isDigital) {
			if(digitalTimeseriesList.isEmpty()) {
				return null;
			}
			batchPayload.setTimeSeriesList(digitalTimeseriesList);
			batchPayload.setBatchType(ReadProperty.getConfig(Constants.KEY_TAG_IDENTIFIER_DIGITAL));
			
			batchRecord.setBatchType(batchPayload.getBatchType());
			batchRecord.setTagCount(String.valueOf(digitalBatchTagCounter));
		}else {
			if(analogTimeseriesList.isEmpty()) {
				return null;
			}
			batchPayload.setTimeSeriesList(analogTimeseriesList);
			batchPayload.setBatchType(ReadProperty.getConfig(Constants.KEY_TAG_IDENTIFIER_ANALOG));
			
			batchRecord.setBatchType(batchPayload.getBatchType());
			batchRecord.setTagCount(String.valueOf(analogBatchTagCounter));
		}
		
		batchPayload.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.TIMESTAMP_FORMAT)));
		batchRecord.setTimestamp(batchPayload.getTimestamp());
		
		batchRecordList.add(batchRecord);
		
		if(isDigital) {
			digitalBatchTagCounter = 0;
		}else {
			analogBatchTagCounter = 0;
		}
	
		try {
			return new ObjectMapper().setSerializationInclusion(Include.NON_NULL).writeValueAsString(batchPayload);
		} catch (JsonProcessingException e) {
			log.error("error while creating payload for IoT endpoint" + e);
			SQSOperation.pushToQueue(AlertJson.getAlertJson(e));
		}
		return null;
	}

	
	/*
	 * prepare json list of timeseries data
	 */
	public static void convertToJson(Item item, ItemState itemState, boolean isDigital) {
		Timeseries timeseries = new Timeseries();
		
		log.info("creating timeseries json..");
		try {
			timeseries.setTagValue(Formator.getParsedValue(itemState));
		} catch (JIException e1) {
			log.error("error while parsing value " + e1);
		}
		timeseries.setTimestamp(Formator.formatDate(itemState.getTimestamp()));
		timeseries.setTagId(ReadTagList.getTagIdMap().get(item.getId()));
		
		tagReadCounter.merge(item.getId(), 1, Integer::sum);
		SqliteOps.insertRow(timeseries.getTagId(), item.getId(), timeseries.getTagValue(), timeseries.getTimestamp());
		
		if(isDigital) {
			if(ReadTagList.getTagTypeMap().get(item.getId()).contains(ReadProperty.getConfig(Constants.KEY_TAG_IDENTIFIER_PUMP))) {
				String pumpStatus = itemState.getQuality().intValue() == 
						Integer.valueOf(ReadProperty.getConfig(Constants.KEY_TAG_THRESHOLD_QUALITY)) ? 
								Constants.TAG_READ_STATUS_GOOD : Constants.TAG_READ_STATUS_BAD;
				log.info("pump status: " + pumpStatus);
				timeseries.setStatus(pumpStatus);
			}
			digitalTimeseriesList.add(timeseries);
			digitalBatchTagCounter = digitalBatchTagCounter + 1;
		}else {
			analogTimeseriesList.add(timeseries);
			analogBatchTagCounter = analogBatchTagCounter + 1;
		}
	}

	
	/*
	 * clear timeseries list
	 */
	public static void clearAnalogTimeseriesList() {
		analogTimeseriesList.clear();
	}
	
	
	
	public static List<BatchRecord> getBatchRecordList() {
		return batchRecordList;
	}


	public static Map<String, Integer> getTagReadCounter() {
		return tagReadCounter;
	}


	public static void clearDigitalTimeseriesList() {
		digitalTimeseriesList.clear();
	}
	
}
