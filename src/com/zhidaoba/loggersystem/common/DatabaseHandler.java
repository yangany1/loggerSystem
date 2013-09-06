package com.zhidaoba.loggersystem.common;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.zhidaoba.loggersystem.relevancy.RelevancyObject;

/**
 * 处理与数据库交互的类 包括mysql数据和Mongodb数据
 * 
 * @author luo
 * 
 */
public class DatabaseHandler {

	/**
	 * 将日志数据写入mongo数据库
	 * 
	 * @param changeList
	 */
	public static void writeLogToMongoDB(
			ArrayBlockingQueue<RelevancyObject> changeList) {
		MongoClient client = null;
		try {
			client = new MongoClient(ConfigHandler.getMongodbServer(),
					ConfigHandler.getMongodbPort());
			DB db = client.getDB(ConfigHandler.getMongodbName());
			DBCollection coll = db.getCollection("userlogs");
			for (RelevancyObject content : changeList) {
				BasicDBObject doc = new BasicDBObject(Constants.USER_ID,
						content.getUserId())
						.append(Constants.LOGTIME_FIELD, content.getTime())
						.append(Constants.ACTION_FIELD, content.getAction())
						.append(Constants.CONTENT_FIELD, content.getContent())
						.append(Constants.ISHANDLED_FIELD, false);
				coll.insert(doc);
				changeList.remove(content);
			}
			client.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} finally {
			if (client != null) {
				client.close();
			}
		}
	}

	/**
	 * 查询mongodb的log数据库，返回所有未分析过的数据记录
	 * 
	 * @return
	 */
	public static List<DBObject> getRelevancyObjectsFromMongoDB() {
		MongoClient client = null;
		DBCursor cursor = null;
		List<DBObject> rlist = new ArrayList<DBObject>();
		try {
			client = new MongoClient(ConfigHandler.getMongodbServer(),
					ConfigHandler.getMongodbPort());
			DB db = client.getDB(ConfigHandler.getMongodbName());
			// boolean auth = db.authenticate(myUserName, myPassword);
			DBCollection coll = db.getCollection(Constants.USER_LOGS);
			DBObject query = new BasicDBObject();
			query.put(Constants.ISHANDLED_LOG, false);
			cursor = coll.find(query);
			while (cursor.hasNext()) {
				DBObject obj = cursor.next();
				rlist.add(obj);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				if (cursor != null) {
					cursor.close();
				}
				if (client != null) {
					client.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		ConfigHandler.getLogger().info("getRelevancyObjectsFromMongoDB end");
		return rlist;
	}

	/**
	 * 更新数据库log记录状态，标记为已处理
	 */
	public static boolean updateLogState(DBObject r) {
		MongoClient client = null;
		DBCursor cursor = null;
		try {
			client = new MongoClient(ConfigHandler.getMongodbServer(),
					ConfigHandler.getMongodbPort());
			DB db = client.getDB(ConfigHandler.getMongodbName());
			// boolean auth = db.authenticate(myUserName, myPassword);
			DBCollection coll = db.getCollection(Constants.USER_LOGS);
			DBObject updatedValue = new BasicDBObject();
			updatedValue.put(Constants.ISHANDLED_FIELD, true);
			DBObject updateSetValue = new BasicDBObject("$set", updatedValue);
			coll.update(r, updateSetValue);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (cursor != null) {
					cursor.close();
				}
				if (client != null) {
					client.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	/**
	 * 查询mysql，得到当前的用户和标签的相关度
	 * 
	 * @param expertTagRelevancy
	 */
	public static void getRelevancyFromMysql(
			Map<String, Map<String, Float>> expertTagRelevancy) {
		ConfigHandler.getLogger().info("begin getRelevancyFromMysql");
		ResultSet result = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(
					ConfigHandler.getMysqlPath(),
					ConfigHandler.getMysqlUsername(),
					ConfigHandler.getMysqlPassword());

			stmt = conn.createStatement();
			result = stmt.executeQuery("SELECT * from "
					+ Constants.TABLE_RELEVANCY_NAME);
			while (result != null && result.next()) {
				String tag = result.getString(Constants.TAG);
				String user_id = result.getString(Constants.USER_ID);
				float relevancy = result.getFloat(Constants.RELEVANCY);
				if (!expertTagRelevancy.containsKey(user_id)) {
					expertTagRelevancy.put(user_id,
							new HashMap<String, Float>());
				}
				expertTagRelevancy.get(user_id).put(tag, relevancy);
			}

			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (result != null) {
				try {
					result.close();
				} catch (SQLException sqlEx) {
				}
				result = null;
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) {
				}
				stmt = null;
			}
		}

	}

	/**
	 * 更新用户的贡献值
	 * 
	 * @param userid
	 * @param value
	 * @return
	 */
	public static boolean updateContribution(String userid, double value) {
		MongoClient client = null;
		DBCursor cursor = null;
		try {
			client = new MongoClient(ConfigHandler.getMongodbServer(),
					ConfigHandler.getMongodbPort());
			DB db = client.getDB(ConfigHandler.getMongodbName());
			// boolean auth = db.authenticate(myUserName, myPassword);
			DBCollection coll = db
					.getCollection(Constants.CONTRIBUTION_COLLECTION);
			BasicDBObject query = new BasicDBObject(Constants.USER_ID, userid);
			cursor = coll.find(query);
			if (cursor.hasNext()) {
				DBObject obj = cursor.next();
				DBObject updatedValue = new BasicDBObject();
				updatedValue
						.put(Constants.VALUE_FIELD_IN_CONTRIBUTION_COLLECTION,
								(Double) obj
										.get(Constants.VALUE_FIELD_IN_CONTRIBUTION_COLLECTION)
										+ value);
				DBObject updateSetValue = new BasicDBObject("$set",
						updatedValue);
				coll.update(obj, updateSetValue);
			} else {
				BasicDBObject doc = new BasicDBObject(Constants.USER_ID, userid)
						.append(Constants.VALUE_FIELD_IN_CONTRIBUTION_COLLECTION,
								value);
				coll.insert(doc);
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
			if (client != null) {
				client.close();
			}
		}
		return true;
	}

	/**
	 * 更新用户的消耗值
	 * 
	 * @param userid
	 * @param value
	 * @return
	 */
	public static boolean updateConsumation(String userid, double value) {
		MongoClient client = null;
		DBCursor cursor = null;
		try {
			client = new MongoClient(ConfigHandler.getMongodbServer(),
					ConfigHandler.getMongodbPort());
			DB db = client.getDB(ConfigHandler.getMongodbName());
			DBCollection coll = db
					.getCollection(Constants.CONSUMATION_COLLECTION);
			BasicDBObject query = new BasicDBObject(Constants.USER_ID, userid);
			cursor = coll.find(query);
			if (cursor.hasNext()) {
				DBObject obj = cursor.next();
				DBObject updatedValue = new BasicDBObject();
				updatedValue
						.put(Constants.VALUE_FIELD_IN_CONSUMATION_COLLECTION,
								(Double) obj
										.get(Constants.VALUE_FIELD_IN_CONSUMATION_COLLECTION)
										+ value);
				DBObject updateSetValue = new BasicDBObject("$set",
						updatedValue);
				coll.update(obj, updateSetValue);
			} else {
				BasicDBObject doc = new BasicDBObject(Constants.USER_ID, userid)
						.append(Constants.VALUE_FIELD_IN_CONSUMATION_COLLECTION,
								value);
				coll.insert(doc);
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
			if (client != null) {
				client.close();
			}
		}
		return true;
	}

}
