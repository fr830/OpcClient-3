package com.lti.ow.lambda.connector;

import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.openscada.opc.lib.common.AlreadyConnectedException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.AutoReconnectController;
import org.openscada.opc.lib.da.AutoReconnectListener;
import org.openscada.opc.lib.da.AutoReconnectState;
import org.openscada.opc.lib.da.Server;

import com.lti.ow.lambda.forward.SQSOperation;
import com.lti.ow.lambda.property.Constants;
import com.lti.ow.lambda.property.ReadProperty;
import com.lti.ow.lambda.util.AlertJson;

public final class OpcConnector {

	private static OpcConnector conn = null;
	private static ConnectionInformation ci = null;
	private static Server server = null;
	private static AutoReconnectState connectionState = AutoReconnectState.DISCONNECTED;
	
	static final Logger log = Logger.getLogger(OpcConnector.class);

	/*
	 * private constructor to prevent outside call 
	 */
	private OpcConnector() {}

	/*
	 * connect to opc server
	 */
	public static boolean connectToServer() {
		log.info("trying to connect OPC server......");
		
		server = new Server(setParam(), Executors.newSingleThreadScheduledExecutor());
		autoConnect();
		
		try {
			server.connect();
			return true;
		} catch (AlreadyConnectedException e) {
			log.info("already connected " + e);
			return true;
		} catch (Exception e) {
			log.error("error while connecting OPC server " + e);
			SQSOperation.pushToQueue(AlertJson.getAlertJson(e));
			return false;
		}
	}
	
	
	/*
	 * OPC server auto connection
	 */
	public static void autoConnect() {
		log.info("making provisions for server auto reconnection");
		
		server = new Server(setParam(), Executors.newSingleThreadScheduledExecutor());
		AutoReconnectController controller = new AutoReconnectController(server);
		controller.addListener(new AutoReconnectListener() {
			@Override
			public void stateChanged(AutoReconnectState state) {
				log.info("OPC server connection state changed to : " + state);
				connectionState = state;
			}
		});
		
		if(connectionState != AutoReconnectState.CONNECTED) {
			controller.connect();
		}
	}
	
	
	public static boolean isConnected() {
		if(connectionState == AutoReconnectState.CONNECTED) {
			return true;
		}
		
		return false;
	}
	
	public static Server getServer() {
		return server;
	}

	
	/*
	 * getter for this class object
	 */
	public static OpcConnector getOpcConnectorObj() {
		if (conn == null) {
			conn = new OpcConnector();
			return conn;
		}
		return conn;
	}

	
	/*
	 * setter for parameters used for connecting opc server
	 */
	private static ConnectionInformation setParam() {
		String Opc_server_ip = ReadProperty.getConfig(Constants.KEY_OPC_SERVER_IP);
		String Opc_server_user = ReadProperty.getConfig(Constants.KEY_OPC_SERVER_USER);
		String Opc_server_pwd = ReadProperty.getConfig(Constants.KEY_OPC_SERVER_PWD);
		String Opc_server_domain = ReadProperty.getConfig(Constants.KEY_OPC_SERVER_DOMAIN);
		String Opc_server_clsid = ReadProperty.getConfig(Constants.KEY_OPC_SERVER_CLSID);
		String Opc_server_progId = ReadProperty.getConfig(Constants.KEY_OPC_SERVER_PROGID);
		
		ci = new ConnectionInformation();
		ci.setHost(Opc_server_ip);
		ci.setDomain(Opc_server_domain);
		ci.setUser(Opc_server_user);
		ci.setPassword(Opc_server_pwd);
		ci.setProgId(Opc_server_progId);
		/*
		 * if ProgId is not working, 
		 * try it using the Clsid = "F8582CF2-88FB-11D0-B850-00C0F0104305" instead
		 */
		ci.setClsid(Opc_server_clsid); 

		log.info("Host: " + Opc_server_ip +"\n"+ " Domain: " + Opc_server_domain +"\n"+ 
		                    " User: " + Opc_server_user +"\n"+ " Password: " + Opc_server_pwd +"\n"+
		                    " Clsid: " + Opc_server_clsid + "\n" + "ProgId :" + Opc_server_progId);
		return ci;
	}

}
