package com.lti.ow.lambda.util;

import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import com.lti.ow.lambda.forward.S3Operation;
import com.lti.ow.lambda.pojo.BatchRecord;
import com.lti.ow.lambda.pojo.TagRecord;
import com.lti.ow.lambda.property.Constants;
import com.lti.ow.lambda.property.ReadProperty;
import com.lti.ow.lambda.property.ReadTagList;

public class CsvLogTimerTask extends TimerTask {
	
	static final Logger log = Logger.getLogger(CsvLogTimerTask.class);
	
	public void run() {
		try {
			log.info("preparing to send csv logs to s3");
			
			String bucket = ReadProperty.getConfig(Constants.KEY_AWS_S3_BUCKET);
			String batchLogFolderPath = ReadProperty.getConfig(Constants.KEY_AWS_S3_BUCKET_BATCH_PATH);
			String batchLogPath = bucket + "/" + batchLogFolderPath;
			String tagLogFolderPath = ReadProperty.getConfig(Constants.KEY_AWS_S3_BUCKET_TAG_PATH);
			String tagLogPath = bucket + "/" + tagLogFolderPath;
			String batchLogFilePath = createAndSendBatchLog();
			String tagLogFilePath = createAndSendTagLog();
			
			InetAddress host = InetAddress.getByName("aws.amazon.com");
			while(!host.isReachable(10000)){
				log.info("connection not available. Logs will be sent after connection is resumed");
				Thread.sleep(3000000);
			}
			
			log.info("internet connection is availble ======================== files are being transfered");
			S3Operation.sendToS3(batchLogFilePath, batchLogPath);
			if(RestClient.restCall(Constants.CSV_LOG_TYPE_BATCH, new File(batchLogFilePath).getName(), bucket, batchLogFolderPath)) {
				log.info("csv log verifier successfully executed for " + batchLogFilePath);
			}
			
			S3Operation.sendToS3(tagLogFilePath, tagLogPath);
			if(RestClient.restCall(Constants.CSV_LOG_TYPE_TAG, new File(tagLogFilePath).getName(), bucket, tagLogFolderPath)) {
				log.info("csv log verifier successfully executed for " + tagLogFilePath);
			}
			
		}catch(Exception e) {
			log.error(e + " error while creating batch or tag Logs"); 
		}finally {
			log.info("trying to delete old log files");
			
			String oldBatchLogFilePath = String.format(Constants.BATCH_LOG_FILE_PATH, 
					LocalDateTime.now().minusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
			String oldTagLogFilepath = String.format(Constants.TAG_LOG_FILE_PATH, 
					LocalDateTime.now().minusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
			String oldAppLogFilePath = String.format(Constants.APP_LOG_FILE_PATH, 
					LocalDateTime.now().minusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
			
			File batchLogFile = new File(oldBatchLogFilePath);
			if(batchLogFile.exists()) {
				batchLogFile.delete();
			}
			
			File tagLogFile = new File(oldTagLogFilepath);
			if(tagLogFile.exists()) {
				tagLogFile.delete();
			}
			
			File appLogFile = new File(oldAppLogFilePath);
			if(appLogFile.exists()) {
				appLogFile.delete();
			}
			
			log.info("log files deleted are " + oldBatchLogFilePath + ", " + oldTagLogFilepath + ", " + oldAppLogFilePath);
		}
	}
	
	
	private String createAndSendBatchLog() throws Exception {
		final String FILE_PATH = String.format(Constants.BATCH_LOG_FILE_PATH, 
				LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		final String[] header = new String[] { Constants.BATCH_LOG_COULUM1, Constants.BATCH_LOG_COULUM2,
				Constants.BATCH_LOG_COULUM3 };
		
		ICsvBeanWriter beanWriter = null;

		try {
			beanWriter = new CsvBeanWriter(new FileWriter(FILE_PATH), CsvPreference.STANDARD_PREFERENCE);
			beanWriter.writeHeader(header);
			
			List<BatchRecord> batchRecordList = TimeseriesToJson.getBatchRecordList();
			
			for(BatchRecord batchRecord : batchRecordList) {
				beanWriter.write(batchRecord, header);
			}
			

		} finally {
			if (beanWriter != null) {
				beanWriter.close();
				log.info("sending batch log to s3");
				TimeseriesToJson.getBatchRecordList().clear();
			}
		}
		return FILE_PATH;
	}
	
	
	
	private String createAndSendTagLog() throws Exception {
		final Map<String, Integer> tagIdMap = ReadTagList.getTagIdMap();
		final Map<String, String> tagTypeMap = ReadTagList.getTagTypeMap();
		final String[] header = new String[] { Constants.TAG_LOG_COULUM1, Constants.TAG_LOG_COULUM2,
				Constants.TAG_LOG_COULUM3, Constants.TAG_LOG_COULUM4 };
		final String FILE_PATH = String.format(Constants.TAG_LOG_FILE_PATH, 
				LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

		ICsvBeanWriter beanWriter = null;

		try {
			beanWriter = new CsvBeanWriter(new FileWriter(FILE_PATH), CsvPreference.STANDARD_PREFERENCE);
			beanWriter.writeHeader(header);
			
			Map<String, Integer> tagsReadCount = TimeseriesToJson.getTagReadCounter();
			List<String> scadaTagList = ReadTagList.getScadaTagList();

			for (String tagName : scadaTagList) {
				TagRecord tagRecord = new TagRecord();

				tagRecord.setTagId(String.valueOf(tagIdMap.get(tagName)));
				tagRecord.setTagName(tagName);
				tagRecord.setTagType(tagTypeMap.get(tagName));

				if (tagsReadCount.containsKey(tagName)) {
					tagRecord.setReadCount(String.valueOf(tagsReadCount.get(tagName)));
				} else {
					tagRecord.setReadCount(String.valueOf(0));
				}

				beanWriter.write(tagRecord, header);
			}
		} finally {
			if (beanWriter != null) {
				beanWriter.close();
				log.info("sending tag read count log to s3");
				TimeseriesToJson.getTagReadCounter().clear();
			}
		}
		return FILE_PATH;
	}
}
