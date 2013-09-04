package com.zhidaoba.loggersystem.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 读取系统配置信息的类
 * 从文件中读取
 * @author luo
 *
 */
public class ConfigHandler {

	private final static Logger LOGGER = Logger
			.getLogger("zhidaoba.LoggerSystem");

	/**
	 * 默认的参数配置
	 */
	private final static String DEFAULT_MYSQL_PATH = "jdbc:mysql://localhost/tag?useUnicode=true&characterEncoding=utf8";
	private final static String DEFAULT_MYSQL_USERNAME = "root";
	private final static String DEFAULT_MYSQL_PASSWORD = "";

	private final static String DEFAULT_LOG_FILE = "log/loggersystem.log";
	private final static String DEFAULT_LOG_DIR = "log/";
	private final static int DEFAULT_LOG_QUEUE_SIZE = 10000000;
	
	private final static long DEFAULT_REV_LOG_LAST_READ_TIME = 0;
	private final static String DEFAULT_REV_LOG_LAST_READ_FILE_NAME = "";
	private final static int DEFAULT_REV_LOG_LAST_READ_LINE_NUMBER = 0;

	private final static String DEFAULT_MONGODB_SERVER = "127.0.0.1";
	private final static int DEFAULT_MONGODB_PORT = 27017;
	private final static String DEFAULT_MONGODB_DBNAME = "development";

	
	/**
	 * 用户的设置参数
	 */
	// 配置文件名
	private final static String FILENAME = "loggerSystem.properties";

	// mysql参数
	private final static String MYSQL_PATH_STRING = "mysql_path";
	private final static String MYSQL_USERNAME_STRING = "mysql_username";
	private final static String MYSQL_PASSWORD_STRING = "mysql_password";

	// mongodb参数
	private final static String MONGODB_DBNAME_STRING = "mongodb_dbname";
	private final static String MONGODB_SERVER_STRING = "mongodb_server";
	private final static String MONGODB_PORT_STRING = "mongodb_port";

	private final static String LOG_QUEUE_SIZE_STRING = "log_queue_size";
	private final static String LOG_FILE_STRING = "self_log";
	private final static String LOG_DIR_STRING = "log_dir";
	private final static String REV_LOG_LAST_READ_TIME_STRING = "rev_log_last_read_time";
	private final static String REV_LOG_LAST_READ_FILE_NAME_STRING = "rev_log_last_read_file_name";
	private final static String REV_LOG_LAST_READ_LINE_NUMBER_STRING = "rev_log_last_read_line_number";

	/**
	 * 变量
	 */
	private static String MYSQL_PATH;
	private static String MYSQL_USERNAME;
	private static String MYSQL_PASSWORD;
	private static String MONGODB_SERVER;
	private static int MONGODB_PORT;
	private static String MONGODB_DBNAME;
	private static String LOG_DIR;
	private static String LOG_FILE;
	private static long REV_LOG_LAST_READ_TIME;
	private static String REV_LOG_LAST_READ_FILE_NAME;
	private static int REV_LOG_LAST_READ_LINE_NUMBER;
	private static int LOG_QUEUE_SIZE;
	
	private ConfigHandler() {
	}

	// 如果配置文件里没有参数，则设为默认参数
	public static void load() {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(FILENAME));

			MYSQL_PATH = prop
					.getProperty(MYSQL_PATH_STRING, DEFAULT_MYSQL_PATH);
			MYSQL_USERNAME = prop.getProperty(MYSQL_USERNAME_STRING,
					DEFAULT_MYSQL_USERNAME);
			MYSQL_PASSWORD = prop.getProperty(MYSQL_PASSWORD_STRING,
					DEFAULT_MYSQL_PASSWORD);

			MONGODB_SERVER = prop.getProperty(MONGODB_SERVER_STRING,
					DEFAULT_MONGODB_SERVER);
			MONGODB_PORT = Integer
					.valueOf(prop.getProperty(MONGODB_PORT_STRING,
							Integer.toString(DEFAULT_MONGODB_PORT)));
			MONGODB_DBNAME = prop.getProperty(MONGODB_DBNAME_STRING,
					DEFAULT_MONGODB_DBNAME);

			LOG_DIR = prop.getProperty(LOG_DIR_STRING, DEFAULT_LOG_DIR);
			LOG_FILE = prop.getProperty(LOG_FILE_STRING, DEFAULT_LOG_FILE);
			LOG_QUEUE_SIZE = Integer.valueOf(prop.getProperty(LOG_QUEUE_SIZE_STRING, Integer.toString(DEFAULT_LOG_QUEUE_SIZE)));
			
			REV_LOG_LAST_READ_TIME = Long.valueOf(prop.getProperty(
					REV_LOG_LAST_READ_TIME_STRING,
					Long.toString(DEFAULT_REV_LOG_LAST_READ_TIME)));
			REV_LOG_LAST_READ_FILE_NAME = prop.getProperty(
					REV_LOG_LAST_READ_FILE_NAME_STRING,
					DEFAULT_REV_LOG_LAST_READ_FILE_NAME);
			if (REV_LOG_LAST_READ_FILE_NAME.equals("None")) {
				REV_LOG_LAST_READ_FILE_NAME = "";
			}
			REV_LOG_LAST_READ_LINE_NUMBER = Integer.valueOf(prop.getProperty(
					REV_LOG_LAST_READ_LINE_NUMBER_STRING,
					Integer.toString(DEFAULT_REV_LOG_LAST_READ_LINE_NUMBER)));

		} catch (Exception e) {
			MYSQL_PATH = DEFAULT_MYSQL_PATH;
			MYSQL_USERNAME = DEFAULT_MYSQL_USERNAME;
			MYSQL_PASSWORD = DEFAULT_MYSQL_PASSWORD;
			MONGODB_DBNAME = DEFAULT_MONGODB_DBNAME;
			MONGODB_SERVER = DEFAULT_MONGODB_SERVER;
			MONGODB_PORT = DEFAULT_MONGODB_PORT;
			LOG_QUEUE_SIZE = DEFAULT_LOG_QUEUE_SIZE;
			LOG_DIR = DEFAULT_LOG_DIR;
			LOG_FILE = DEFAULT_LOG_FILE;
			REV_LOG_LAST_READ_TIME = 0;
			REV_LOG_LAST_READ_FILE_NAME = "";
			REV_LOG_LAST_READ_LINE_NUMBER = 0;
			e.printStackTrace();
		} finally {
			// 创建存储日志的文件夹
			File logFile = null;
			try {
				logFile = new File(LOG_DIR);
				if (!(logFile.exists()) && !(logFile.isDirectory())) {
					logFile.mkdirs();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			// 创建日志文件
			LOGGER.setLevel(Level.INFO);
			FileHandler logFile = new FileHandler(LOG_FILE + "."
					+ System.currentTimeMillis());
			LOGGER.addHandler(logFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// 保存参数到配置文件
	public static void save() {
		Properties prop = new Properties();
		try {
			prop.setProperty(MYSQL_PATH_STRING, MYSQL_PATH);
			prop.setProperty(MYSQL_USERNAME_STRING, MYSQL_USERNAME);
			prop.setProperty(MYSQL_PASSWORD_STRING, MYSQL_PASSWORD);
			prop.setProperty(MONGODB_DBNAME_STRING, MONGODB_DBNAME);
			prop.setProperty(MONGODB_SERVER_STRING, MONGODB_SERVER);
			prop.setProperty(MONGODB_PORT_STRING,
					Integer.toString(MONGODB_PORT));
			prop.setProperty(LOG_DIR_STRING, LOG_DIR);
			prop.setProperty(LOG_FILE_STRING, LOG_FILE);
			prop.setProperty(LOG_QUEUE_SIZE_STRING, Integer.toString(LOG_QUEUE_SIZE));
			prop.setProperty(REV_LOG_LAST_READ_TIME_STRING,
					Long.toString(REV_LOG_LAST_READ_TIME));
			prop.setProperty(REV_LOG_LAST_READ_FILE_NAME_STRING,
					REV_LOG_LAST_READ_FILE_NAME);
			prop.setProperty(REV_LOG_LAST_READ_LINE_NUMBER_STRING,
					Integer.toString(REV_LOG_LAST_READ_LINE_NUMBER));
			prop.store(new FileOutputStream(FILENAME), null);

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static String getMysqlPath() {
		return MYSQL_PATH;
	}

	public static String getMysqlUsername() {
		return MYSQL_USERNAME;
	}

	public static String getMysqlPassword() {
		return MYSQL_PASSWORD;
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

	public static String getLogFile() {
		return LOG_FILE;
	}

	public static Logger getLogger() {
		return LOGGER;
	}

	public static String getLogDir() {
		return LOG_DIR;
	}

	public static long getRevLogLastReadTime() {
		return REV_LOG_LAST_READ_TIME;
	}

	public static void setRevLogLastReadTime(long time) {
		REV_LOG_LAST_READ_TIME = time;
	}

	public static int getRevLogLastReadLineNumber() {
		return REV_LOG_LAST_READ_LINE_NUMBER;
	}

	public static void setRevLogLastReadLineNumber(int number) {
		REV_LOG_LAST_READ_LINE_NUMBER = number;
	}

	public static String getRevLogLastReadFileName() {
		return REV_LOG_LAST_READ_FILE_NAME;
	}

	public static void setRevLogLastReadFileName(String name) {
		REV_LOG_LAST_READ_FILE_NAME = name;
	}

	public static int getLogQueueSize() {
        return LOG_QUEUE_SIZE;
    }
}
