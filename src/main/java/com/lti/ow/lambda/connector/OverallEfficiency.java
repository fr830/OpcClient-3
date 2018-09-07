package com.lti.ow.lambda.connector;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lti.ow.lambda.forward.Publisher;
import com.lti.ow.lambda.pojo.Timeseries;
import com.lti.ow.lambda.property.Constants;
import com.lti.ow.lambda.property.ReadProperty;
import com.lti.ow.lambda.property.ReadTagList;
import com.lti.ow.lambda.util.Formator;

public class OverallEfficiency {
	
	private static AccessBase overallEff;
	private static Integer tagCounter;
	private static Integer tagCounterTracker;
	
	private static Float rwphWt;
	private static Float wtpWt;
	private static Float cwph1Wt;
	private static Float cwph2Wt;
	private static Float cwph3Wt;
	private static Float cwph4Wt;
	
	static final Logger log = Logger.getLogger(OverallEfficiency.class);
	
	public static void setOverallEffAccess() {
		if (!OpcConnector.isConnected()) {
			return;
		}
		
		try {
			log.info("creating AccessBase object for efficiency calculation");
			overallEff = new SyncAccess(OpcConnector.getServer(), Integer.valueOf(ReadProperty.getConfig(Constants.KEY_TAG_READ_INTERVAL_CALCULATED)));
			
			overallEff.bind();
			
			tagCounter = 0;
			tagCounterTracker = 0;
			initializePlantWt();
			List<String> effTagList = ReadTagList.getCalTagList();
			
			if(effTagList.isEmpty()) {
				log.warn("tagList provided for efficiency is empty");
				return;
			}
			
			for(Short i = 0; i <= 1; i++) {
				effTagList = addEffTags(effTagList);
				
				if(effTagList.isEmpty()) {
					break;
				}
			}
			
			if(!effTagList.isEmpty()) {
				for(String effTag : effTagList) {
					log.warn("could not add: " + effTag);
				}
			}
			
		} catch (Exception e) {
			log.error("error while creating AccessBase object for efficiency calculation " + e);
		}
	}
	
	
	private static List<String> addEffTags(List<String> effTagList) {
		List<String> missedEffTags = new ArrayList<String>();
		
		try {
			for(String effTag : effTagList) {
				tagCounter = tagCounter + 1;
				try {
					overallEff.addItem(effTag, new DataCallback() {
						
						@Override
						public void changed(Item item, ItemState itemState) {
							calAndSendOverallEff(item, itemState);
						}
					});
				}catch(AddFailedException e) {
					tagCounter = tagCounter - 1;
					overallEff.removeItem(effTag);
					missedEffTags.add(effTag);
					log.error(e + " retrying to add efficiency tag " + effTag );
				}
			}
		}catch(Exception e) {
			log.error("error while adding tags for efficiency calculator " + e);
		}
		return missedEffTags;
	}
	
	
	private static void calAndSendOverallEff(Item item, ItemState itemState) {
		log.info("calculating efficiency for " + item.getId());
		
		tagCounterTracker = tagCounterTracker + 1;
		
		String value = null;
		try {
			value = Formator.getParsedValue(itemState);
		} catch (JIException e) {
			log.error("error while parsing value " + e);
		}
		
		if(value == null || itemState.getQuality().intValue() != Integer.valueOf(ReadProperty.getConfig(Constants.KEY_TAG_THRESHOLD_QUALITY))){
			return;
		}

		Map<String, Float> tagWtMap = ReadTagList.getTagWtMap();
		Map<String, String> tagPlantMap = ReadTagList.getTagPlantMap();
		
		if(tagWtMap.isEmpty() && tagPlantMap.isEmpty()) {
			return;
		}
		Float wt = tagWtMap.get(item.getId());
		String plant = tagPlantMap.get(item.getId());
		
		if(plant.equals(ReadProperty.getConfig(Constants.KEY_OVERALL_EFF_PLANT_ID_RWPH))) {
			rwphWt = rwphWt + wt;
		}else if(plant.equals(ReadProperty.getConfig(Constants.KEY_OVERALL_EFF_PLANT_ID_WTP))){
			wtpWt = wtpWt + wt;
		}else if(plant.equals(ReadProperty.getConfig(Constants.KEY_OVERALL_EFF_PLANT_ID_CWPH01))){
			cwph1Wt = cwph1Wt + wt;
		}else if(plant.equals(ReadProperty.getConfig(Constants.KEY_OVERALL_EFF_PLANT_ID_CWPH02))){
			cwph2Wt = cwph2Wt + wt;
		}else if(plant.equals(ReadProperty.getConfig(Constants.KEY_OVERALL_EFF_PLANT_ID_CWPH03))){
			cwph3Wt = cwph3Wt + wt;
		}else if(plant.equals(ReadProperty.getConfig(Constants.KEY_OVERALL_EFF_PLANT_ID_CWPH04))){
			cwph4Wt = cwph4Wt + wt;
		}
		
		if(tagCounterTracker == tagCounter) {
			tagCounterTracker = 0;
						
			String overallEff = String.format("%.4f", (rwphWt * Float.valueOf(ReadProperty.getConfig(Constants.KEY_RWPH_WT))/100) +
					(wtpWt * Float.valueOf(ReadProperty.getConfig(Constants.KEY_WTP_WT))/100) +
					(cwph1Wt * Float.valueOf(ReadProperty.getConfig(Constants.KEY_CWPH1_WT))/100) +
					(cwph2Wt * Float.valueOf(ReadProperty.getConfig(Constants.KEY_CWPH2_WT))/100) +
					(cwph3Wt * Float.valueOf(ReadProperty.getConfig(Constants.KEY_CWPH3_WT))/100) +
					(cwph4Wt * Float.valueOf(ReadProperty.getConfig(Constants.KEY_CWPH4_WT))/100) );
			
			Timeseries effPayload = new Timeseries();
			effPayload.setTagId(Integer.valueOf(ReadProperty.getConfig(Constants.KEY_OVERALL_EFF_TAG_ID)));
			effPayload.setTagValue(overallEff);
			effPayload.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.TIMESTAMP_FORMAT)));
			
			try {
				String effPayloadJson = new ObjectMapper().setSerializationInclusion(Include.NON_NULL).writeValueAsString(effPayload);
				Publisher.publishToGreengrass(effPayloadJson, ReadProperty.getConfig(Constants.KEY_OVERALL_EFF_TOPIC));
			} catch (Exception e) {
				log.error("error while publishing efficiency ");
			}
			
			initializePlantWt();
		}
	}
	
	
	private static void initializePlantWt() {
		rwphWt = (float)0;
		wtpWt = (float)0;
		cwph1Wt = (float)0;
		cwph2Wt = (float)0;
		cwph3Wt = (float)0;
		cwph4Wt = (float)0;
	}
	
	public static AccessBase getOverallEff() {
		return overallEff;
	}

}
