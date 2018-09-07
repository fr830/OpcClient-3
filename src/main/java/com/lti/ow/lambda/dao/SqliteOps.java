package com.lti.ow.lambda.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.lti.ow.lambda.property.Constants;
import com.lti.ow.lambda.property.ReadProperty;

public class SqliteOps {
	
	private static Connection conn = null;	
	
	static final Logger log = Logger.getLogger(SqliteOps.class);
	
	static {
		try {
			log.info("connecting database.....");
			conn = DriverManager.getConnection(ReadProperty.getConfig(Constants.KEY_SQLITE_DB_URL));
			if(conn != null) {
				createTable();
			}
		}catch(Exception e) {
			log.error("error while connecting database " + e);
		}
	}
	

	
	public Connection getConn() {
		if(conn == null) {
			this.createDB();
			return SqliteOps.conn;
		}else {
			return SqliteOps.conn;
		}	
	}
	
	
	
	public void createDB() {
		if(conn == null) {
			log.info("creating/connecting database.....");
			try {
				conn = DriverManager.getConnection(ReadProperty.getConfig(Constants.KEY_SQLITE_DB_URL));
			}catch(Exception e) {
				log.error("error while creating/connecting database " + e);
			}
		}
	}
	
	
	
	
	public static void createTable() {
		if(conn != null) {
			log.info("creating table....");
			try {
				Statement stm = conn.createStatement();
				stm.execute(ReadProperty.getConfig(Constants.KEY_SQLITE_OPS_CREATE));
				//conn.commit();
			}catch(Exception e) {
				log.error("error while creating table " + e);
			}
		}
	}
	
	
	
	public static void insertRow(Integer tagId, String tagName, String value, String timestamp) {
		if(conn != null) {
			log.info("insert row " + tagId +" " +" " + tagName +" " + value + " " + timestamp);
			try {
				PreparedStatement pstm = conn.prepareStatement(ReadProperty.getConfig(Constants.KEY_SQLITE_OPS_INSERT));
				pstm.setInt(1, tagId);
				pstm.setString(2, tagName);
				pstm.setString(3, value);
				pstm.setString(4, timestamp);
				
				pstm.executeUpdate();
				//conn.commit();
			}catch(Exception e) {
				log.error("error while inserting row " + e);
			}
		}
		
	}
	

	public void deleteRow(String timestamp) {
		if(conn != null) {
			log.info("deleting row...");
			try {
				PreparedStatement pstm = conn.prepareStatement(ReadProperty.getConfig(Constants.KEY_SQLITE_OPS_DELETE));
				pstm.setString(1, timestamp);
				
				pstm.executeUpdate();
				//conn.commit();
			}catch(Exception e) {
				log.error("error while deleting row " + e);
			}
		}
	}

}
