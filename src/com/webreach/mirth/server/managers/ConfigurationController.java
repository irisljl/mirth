/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Mirth.
 *
 * The Initial Developer of the Original Code is
 * WebReach, Inc.
 * Portions created by the Initial Developer are Copyright (C) 2006
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Gerald Bortis <geraldb@webreachinc.com>
 *
 * ***** END LICENSE BLOCK ***** */

package com.webreach.mirth.server.managers;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.webreach.mirth.model.Transport;
import com.webreach.mirth.model.converters.DocumentSerializer;
import com.webreach.mirth.server.Command;
import com.webreach.mirth.server.CommandQueue;
import com.webreach.mirth.server.core.MuleConfigurationBuilder;
import com.webreach.mirth.server.core.util.DatabaseConnection;
import com.webreach.mirth.server.core.util.DatabaseUtil;
import com.webreach.mirth.server.core.util.PropertyLoader;

/**
 * The ConfigurationController provides access to the Mirth configuration.
 * 
 * @author geraldb
 * 
 */
public class ConfigurationController {
	private Logger logger = Logger.getLogger(ConfigurationController.class);
	private DatabaseConnection dbConnection;

	/**
	 * Returns a List containing the transport with the specified
	 * <code>id</code>. If the <code>id</code> is <code>null</code>, all
	 * transports are returned.
	 * 
	 * @return
	 * @throws ControllerException
	 */
	public Map<String, Transport> getTransports() throws ControllerException {
		logger.debug("retrieving transport list");

		ResultSet result = null;

		try {
			dbConnection = new DatabaseConnection();
			StringBuffer query = new StringBuffer();
			query.append("SELECT NAME, DISPLAY_NAME, CLASS_NAME, PROTOCOL, TRANSFORMERS FROM TRANSPORTS;");
			result = dbConnection.query(query.toString());
			return getTransports(result);
		} catch (SQLException e) {
			throw new ControllerException(e);
		} finally {
			DatabaseUtil.close(result);
			dbConnection.close();
		}
	}
	
	/**
	 * Returns a Map of Transports given a ResultSet.
	 * 
	 * @param result
	 * @return
	 * @throws SQLException
	 */
	private Map<String, Transport> getTransports(ResultSet result) throws SQLException {
		Map<String, Transport> transports = new HashMap<String, Transport>();

		while (result.next()) {
			Transport transport = new Transport();
			transport.setName(result.getString("NAME"));
			transport.setDisplayName(result.getString("DISPLAY_NAME"));
			transport.setClassName(result.getString("CLASS_NAME"));
			transport.setProtocol(result.getString("PROTOCOL"));
			transport.setTransformers(result.getString("TRANSFORMERS"));
			transports.put(transport.getName(), transport);
		}

		return transports;
	}

	public Properties getServerProperties() throws ControllerException {
		logger.debug("retrieving properties");

		Properties properties = PropertyLoader.loadProperties("mirth");

		if (properties == null) {
			throw new ControllerException("Could not load properties.");
		} else {
			return properties;
		}
	}

	public void updateServerProperties(Properties properties) throws ControllerException {
		logger.debug("updating server properties");

		try {
			FileOutputStream fos = new FileOutputStream("mirth.properties");
			properties.store(fos, null);
		} catch (Exception e) {
			throw new ControllerException(e);
		}
	}

	public int getNextId() throws ControllerException {
		logger.debug("retrieving next id");

		dbConnection = new DatabaseConnection();
		ResultSet result = null;
		int id = -1;

		try {
			result = dbConnection.query("SELECT NEXT VALUE FOR SEQ_CONFIGURATION FROM INFORMATION_SCHEMA.SYSTEM_SEQUENCES WHERE SEQUENCE_NAME='SEQ_CONFIGURATION';");
			result.next();

			if (result.getInt(1) > 0) {
				id = result.getInt(1);
			}
		} catch (SQLException e) {
			throw new ControllerException("Could not generate next unique identifier.", e);
		} finally {
			DatabaseUtil.close(result);
			dbConnection.close();
		}

		return id;
	}

	/**
	 * Creates a new configuration and restarts the Mule engine.
	 * 
	 * @throws ControllerException
	 */
	public void deployChannels() throws ControllerException {
		logger.debug("deploying channels");

		try {
			ChannelController channelController = new ChannelController();
			CommandQueue queue = CommandQueue.getInstance();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			DocumentSerializer docSerializer = new DocumentSerializer();

			// instantiate a new configuration builder given the current channel
			// and transport list
			MuleConfigurationBuilder builder = new MuleConfigurationBuilder(channelController.getChannels(null), getTransports());
			// generate the new configuraton and serialize it to XML
			docSerializer.serialize(builder.getConfiguration(), MuleConfigurationBuilder.cDataElements, os);
			// add the newly generated configuration to the database
			addConfiguration(os.toString());
			// restart the mule engine which will grab the latest configuration
			// from the database
			queue.addCommand(new Command(Command.CMD_RESTART_MULE));
		} catch (Exception e) {
			throw new ControllerException(e);
		}
	}

	/**
	 * Returns a File with the latest Mule configuration.
	 * 
	 * @return
	 * @throws ControllerException
	 */
	public File getLatestConfiguration() throws ControllerException {
		logger.debug("retrieving latest configuration");

		Properties properties = PropertyLoader.loadProperties("mirth");
		ResultSet result = null;
		try {
			dbConnection = new DatabaseConnection();
			result = dbConnection.query("SELECT ID, DATE_CREATED, DATA FROM CONFIGURATIONS WHERE DATE_CREATED IN (SELECT MAX(DATE_CREATED) FROM CONFIGURATIONS);");

			while (result.next()) {
				logger.debug("using configuration ID" + result.getInt("ID") + " created @ " + result.getTimestamp("DATE_CREATED").toString());
				String data = result.getString("DATA");
				BufferedWriter out = new BufferedWriter(new FileWriter(properties.getProperty("mule.config")));
				out.write(data);
				out.close();
				return new File(properties.getProperty("mule.config"));
			}

			logger.debug("no configuration found, using default boot file");
			return new File(properties.getProperty("mule.boot"));
		} catch (Exception e) {
			return null;
		} finally {
			DatabaseUtil.close(result);
			dbConnection.close();
		}
	}

	/**
	 * Adds a new configuraiton to the database.
	 * 
	 * @param data
	 * @throws ControllerException
	 */
	private void addConfiguration(String data) throws ControllerException {
		logger.debug("adding configuration");

		try {
			dbConnection = new DatabaseConnection();
			StringBuffer insert = new StringBuffer();
			insert.append("INSERT INTO CONFIGURATIONS (DATE_CREATED, DATA) VALUES (");
			insert.append("'" + DatabaseUtil.getNowTimestamp() + "',");
			insert.append("'" + data + "');");
			dbConnection.update(insert.toString());
		} catch (Exception e) {
			throw new ControllerException(e);
		} finally {
			dbConnection.close();
		}
	}

}
