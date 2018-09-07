package com.lti.ow.lambda.forward;

import java.io.File;

import org.apache.log4j.Logger;

import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.lti.ow.lambda.property.Constants;
import com.lti.ow.lambda.property.ReadProperty;

public class S3Operation {
	
	private static AmazonS3 s3Client = null;
	
	static final Logger log = Logger.getLogger(S3Operation.class);
	
	static {
		s3Client = AmazonS3ClientBuilder.standard()
				.withRegion(ReadProperty.getConfig(Constants.KEY_AWS_REGION))
				.withCredentials(new PropertiesFileCredentialsProvider(ReadProperty.getConfig(Constants.KEY_AWS_SQS_CREDENTIAL_PATH)))
				.build();
	}
	
	public static void sendToS3(String file_path, String bucket_path ) {
		try {
			File local_file = new File(file_path);
			//s3Client.putObject(bucket_path, local_file.getName(), local_file);
		}catch(Exception e) {
			log.error(e + " error while sending file to s3");
		}
		
	}

}
