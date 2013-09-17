package com.zhidaoba.emergency;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;


public class EmergencyConfigHandler {

	private final static Logger LOGGER = Logger.getLogger("Emergency.class");
	private final static String FILENAME = "EmergencySystem.properties";

	private final static String MONGODB_DBNAME_STRING = "mongodb_dbname";
	private final static String MONGODB_COLLECTION_STRING = "mongodb_collection";
	private final static String MONGODB_SERVER_STRING = "mongodb_server";
	private final static String MONGODB_PORT_STRING = "mongodb_port";
	private final static String MANUAL_OPTION_STRING = "manual_option";
	private final static String LOG_FILE_STRING = "log_file";

	private final static String DEFAULT_MONGODB_DBNAME = "users";
	private final static String DEFAULT_MONGODB_COLLECTION = "experts";
	private final static String DEFAULT_MONGODB_SERVER = "192.168.0.253";
	private final static int DEFAULT_MONGODB_PORT = 27017;
	private static final boolean DEFAULT_MANUAL_OPTION = false;
	private final static String DEFAULT_LOG_FILE = "log/Recommend.log";

	private static String MONGODB_DBNAME;
	private static String MONGODB_SERVER;
	private static int MONGODB_PORT;
	private static String MONGODB_COLLECTION;
	private static boolean MANUAL_OPTION;
	private static String LOG_FILE;

	private EmergencyConfigHandler() {
	}

	public static void save() {
		Properties prop = new Properties();
		try {
			prop.store(new FileOutputStream(FILENAME), null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void load() {

		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(FILENAME));
			MONGODB_DBNAME = prop.getProperty(MONGODB_DBNAME_STRING,
					DEFAULT_MONGODB_DBNAME);
			MONGODB_COLLECTION = prop.getProperty(MONGODB_COLLECTION_STRING,
					DEFAULT_MONGODB_COLLECTION);
			System.out.println("mongo collection:"+MONGODB_COLLECTION);
			MONGODB_SERVER = prop.getProperty(MONGODB_SERVER_STRING,
					DEFAULT_MONGODB_SERVER);
			MONGODB_PORT = Integer
					.valueOf(prop.getProperty(MONGODB_PORT_STRING,
							Integer.toString(DEFAULT_MONGODB_PORT)));

			MANUAL_OPTION = Boolean.valueOf(prop.getProperty(
					MANUAL_OPTION_STRING,
					Boolean.toString(DEFAULT_MANUAL_OPTION)));
			if (LOG_FILE == null
					|| !(LOG_FILE.equals(prop.getProperty(LOG_FILE_STRING,
							DEFAULT_LOG_FILE)))) {
				// System.out.println(LOG_FILE);
				LOG_FILE = prop.getProperty(LOG_FILE_STRING, DEFAULT_LOG_FILE);
				addLogFile();
			}

		} catch (Exception e) {
			MONGODB_DBNAME = DEFAULT_MONGODB_DBNAME;
			MONGODB_COLLECTION = DEFAULT_MONGODB_COLLECTION;
			MONGODB_SERVER = DEFAULT_MONGODB_SERVER;
			MONGODB_PORT = DEFAULT_MONGODB_PORT;
			LOG_FILE = DEFAULT_LOG_FILE;
			e.printStackTrace();
		}
		try {
			LOGGER.setLevel(Level.INFO);
			FileHandler logFile = new FileHandler(LOG_FILE + "."
					+ System.currentTimeMillis());
			LOGGER.addHandler(logFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addLogFile() {
		try {
			LOGGER.setLevel(Level.INFO);
			mkdir(LOG_FILE);
			FileHandler logFile = new FileHandler(LOG_FILE + "."
					+ System.currentTimeMillis());
			LOGGER.addHandler(logFile);
			EmergencyConfigHandler.getLogger().info("Add New Log File success!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void mkdir(String log_file) {
		String dirname = log_file.substring(0, log_file.lastIndexOf("/"));
		try {
			File file = new File(dirname);
			if (!file.exists()) {
				file.mkdirs();
				// System.out.println("create new log directory success!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static String getMongodbName() {
		return MONGODB_DBNAME;
	}


	public static String getMongodbServer() {
		return MONGODB_SERVER;
	}

	public static int getMongodbPort() {
		return MONGODB_PORT;
	}

	public static String getMongodbCollection() {
		return MONGODB_COLLECTION;
	}

	public static boolean getManualOption() {
		return MANUAL_OPTION;
	}

	public static Logger getLogger() {
		return LOGGER;
	}
}
