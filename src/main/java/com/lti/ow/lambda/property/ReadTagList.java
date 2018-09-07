package com.lti.ow.lambda.property;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import com.lti.ow.lambda.forward.SQSOperation;
import com.lti.ow.lambda.pojo.CalculatedTags;
import com.lti.ow.lambda.pojo.Tags;
import com.lti.ow.lambda.util.AlertJson;

public class ReadTagList {
	
	private static ICsvBeanReader beanReader1 = null;
	private static ICsvBeanReader beanReader2 = null;
	private static List<String> scadaTagList = new ArrayList<String>();
    private static Map<String, Integer> tagIdMap = new HashMap<String, Integer>();
    private static Map<String, String> tagTypeMap = new HashMap<String, String>();
	private static List<String> calTagList = new ArrayList<String>();
    private static Map<String, Float> tagWtMap = new HashMap<String, Float>();
    private static Map<String, String> tagPlantMap = new HashMap<String, String>();
    
    private static final String TAGLIST_FILE_PATH = ReadProperty.getConfig(Constants.KEY_TAG_LIST_PATH);
    private static final String TAGLIST_CAL_FILE_PATH = ReadProperty.getConfig(Constants.KEY_TAG_LIST_EFF_PATH);
    
	static final Logger log = Logger.getLogger(ReadTagList.class);

    
    static {
    	try {
			beanReader1 = new CsvBeanReader(new FileReader(TAGLIST_FILE_PATH), CsvPreference.STANDARD_PREFERENCE);
			beanReader2 = new CsvBeanReader(new FileReader(TAGLIST_CAL_FILE_PATH), CsvPreference.STANDARD_PREFERENCE);
		} catch (FileNotFoundException e) {
			log.error("error while reading csv file" + e);
			SQSOperation.pushToQueue(AlertJson.getAlertJson(e));
		}
    }
    
    
    /*
     * create tag record one by one from csv file
     */
    private static void createScadaTagRecord() {
        Tags tag = null;
        
        log.info("creating scadaTagList, tagIdMap, tagTypeMap");
        
        try {
            String[] header = beanReader1.getHeader(true);

            while ((tag = beanReader1.read(Tags.class, header)) != null) {
            	scadaTagList.add(tag.getScadaTag());
            	tagIdMap.put(tag.getScadaTag(), Integer.parseInt(tag.getTagId()));
            	tagTypeMap.put(tag.getScadaTag(), tag.getType());
            }   
        } catch (Exception ex) {
            log.error("error while creating tag mapping " + ex);
            SQSOperation.pushToQueue(AlertJson.getAlertJson(ex));
        }finally {
            if (beanReader1 != null) {
                try {
                	beanReader1.close();
                } catch (IOException ex) {
                    log.error("error while closing the csv reader " + ex);
                    SQSOperation.pushToQueue(AlertJson.getAlertJson(ex));
                }
            }
        }
    }
        
	
    
    public static List<String> getScadaTagList(){
    	if(scadaTagList.isEmpty()) {
    		createScadaTagRecord();
    	}
    	return scadaTagList;
    }
    
    
    /*
	 * get map for tag id against tag name from csv file
	 */
    public static Map<String, Integer> getTagIdMap() {
    	if(tagIdMap.isEmpty()) {
    		createScadaTagRecord();
    	}
    	return tagIdMap;
    }
    
    
	/*
	 * get map for tag type against tag name from csv file
	 */
    public static Map<String,String> getTagTypeMap(){
    	if(tagTypeMap.isEmpty()) {
    		createScadaTagRecord();
    	}
    	return tagTypeMap;
    }
    
    
    /*
     * create calculated tag list
     */
    private static void createCalTagRecord() {
    	
    	CalculatedTags calTag = null;
        
        log.info("creating calTagList, tagWtMap, tagPlantMap");
        
        try {
            String[] header = beanReader2.getHeader(true);
            
            calTagList = new ArrayList<String>();
            tagWtMap = new HashMap<String, Float>();
            tagPlantMap = new HashMap<String, String>();

            while ((calTag = beanReader2.read(CalculatedTags.class, header)) != null) {
            	calTagList.add(calTag.getTagName());
            	tagWtMap.put(calTag.getTagName(), Float.valueOf(calTag.getWeight()));
            	tagPlantMap.put(calTag.getTagName(), calTag.getPlantId());
            }   
        } catch (Exception ex) {
            log.error("error while creating calTag mapping " + ex);
            SQSOperation.pushToQueue(AlertJson.getAlertJson(ex));
        }finally {
            if (beanReader2 != null) {
                try {
                	beanReader2.close();
                } catch (IOException ex) {
                    log.error("error while closing the csv reader " + ex);
                    SQSOperation.pushToQueue(AlertJson.getAlertJson(ex));
                }
            }
        }
    }
    
    
    
    public static List<String> getCalTagList(){
    	if(calTagList.isEmpty()) {
    		createCalTagRecord();
    	}
    	return calTagList;
    }
    
    
    /*
     * get map for tagName against tag wt. from csv file
     */
    public static Map<String, Float> getTagWtMap(){
    	if(tagWtMap.isEmpty()) {
    		createCalTagRecord();
    	}
    	return tagWtMap;
    }
    
    /*
     * get map for tagName against plant from csv file
     */
    public static Map<String, String> getTagPlantMap(){
    	if(tagPlantMap.isEmpty()) {
    		createCalTagRecord();
    	}
    	return tagPlantMap;
    }
    
    

}
