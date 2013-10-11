package com.zhidaoba.loggersystem.common;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.bson.types.ObjectId;

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

	public static final String TOOL_DEVELOP_DATABASE_PATH = "jdbc:mysql://zhidao.ba/zhidaoba_tool_development?useUnicode=true&characterEncoding=utf8";

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
						.append(Constants.DIALOG_ID_FIELD,
								content.getDialog_id())
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
			DBObject sorted = new BasicDBObject();
			sorted.put("logtime", 1);
			cursor = coll.find(query).sort(sorted);
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
			// client = new MongoClient(ConfigHandler.getMongodbServer(),
			// ConfigHandler.getMongodbPort());
			// DB db = client.getDB(ConfigHandler.getMongodbName());
			client = new MongoClient("localhost", 27017);
			DB db = client.getDB("development");
			// boolean auth = db.authenticate(myUserName, myPassword);
			DBCollection coll = db.getCollection("users");
			BasicDBObject query = new BasicDBObject("_id", new ObjectId(userid));
			cursor = coll.find(query);
			if (cursor.hasNext()) {
				DBObject obj = cursor.next();
				DBObject updatedValue = new BasicDBObject();
				updatedValue.put("contribution",
						(Double) obj.get("contribution") + value);
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
			client = new MongoClient("localhost", 27017);
			DB db = client.getDB("development");
			DBCollection coll = db.getCollection("users");
			BasicDBObject query = new BasicDBObject("_id", new ObjectId(userid));
			cursor = coll.find(query);
			if (cursor.hasNext()) {
				DBObject obj = cursor.next();
				DBObject updatedValue = new BasicDBObject();
				updatedValue.put("consumption", (Double) obj.get("consumption")
						+ value);
				DBObject updateSetValue = new BasicDBObject("$set",
						updatedValue);
				coll.update(obj, updateSetValue);
			} else {
				// BasicDBObject doc = new BasicDBObject(Constants.USER_ID,
				// userid)
				// .append(Constants.VALUE_FIELD_IN_CONSUMATION_COLLECTION,
				// value);
				// coll.insert(doc);
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
	 * 从mongodb中查询某个字段的值
	 * 
	 * @param colname
	 * @param field
	 * @param value
	 * @return
	 */
	public static DBObject getItemFromMongoDB(String collectionname,
			String field, String value) {
		MongoClient client = null;
		DBCursor cursor = null;
		try {
			ConfigHandler.getLogger().info(
					"mongo sever=" + ConfigHandler.getMongodbServer()
							+ ",mongo port=" + ConfigHandler.getMongodbPort()
							+ ",name=" + ConfigHandler.getMongodbName());
			client = new MongoClient(ConfigHandler.getMongodbServer(),
					ConfigHandler.getMongodbPort());
			DB db = client.getDB(ConfigHandler.getMongodbName());
			ConfigHandler.getLogger().info(
					"collectionname=" + collectionname + ",field=" + field
							+ ",value=" + value);
			DBCollection coll = db.getCollection(collectionname);
			BasicDBObject query = null;
			if (field == "_id") {
				query = new BasicDBObject(field, new ObjectId(value));
			} else {
				query = new BasicDBObject(field, value);
			}
			cursor = coll.find(query);
			if (cursor.hasNext()) {
				DBObject object = cursor.next();
				cursor.close();
				client.close();
				return object;
			} else {
				ConfigHandler.getLogger().info("cannot find dialog_id");
				return null;
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
		return null;
	}

	/**
	 * 从relationship中找到与from相似的词赋予相似度
	 * 
	 * @param from
	 * @return
	 */
	public static ArrayList<String> getRelationshipsFromMysql(String from) {
		ArrayList<String> words = new ArrayList<String>();
		ResultSet result = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(
					ConfigHandler.getMysqlPath(),
					ConfigHandler.getMysqlUsername(),
					ConfigHandler.getMysqlPassword());
			stmt = conn.createStatement();

			String sqlstmt = "SELECT " + "`to`" + " from "
					+ Constants.TABLE_RELATIONSHIPS_NAME + " where `from` = "
					+ "\"" + from + "\" ORDER BY " + Constants.VALUE_FIELD
					+ " DESC limit 100";
			ConfigHandler.getLogger().info("sql=" + sqlstmt);
			result = stmt.executeQuery(sqlstmt);

			while (result != null && result.next()) {
				words.add(result.getString(Constants.TO_FIELD));
			}
			ConfigHandler.getLogger().info("words size=" + words.size());
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
		return words;

	}

	/**
	 * 更新专家与标签的相似度
	 * 
	 * @param user_id
	 * @param tags
	 * @param relevancies
	 */
	public static boolean updateRelevancyToMysql(String user_id, String[] tags,
			float[] relevancies) {
		System.out.println("update relevancy in mysql length=" + tags.length);
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(ConfigHandler.getMysqlPath(),
					ConfigHandler.getMysqlUsername(),
					ConfigHandler.getMysqlPassword());
			for (int i = 0; i < tags.length; i++) {
				String update = "insert into " + Constants.TABLE_RELEVANCY_NAME
						+ " values (?,?,?) on duplicate key update relevancy=?";
				ConfigHandler.getLogger().info("update sentence=" + update);
				stmt = conn.prepareStatement(update);
				stmt.setString(2, user_id);
				stmt.setString(1, tags[i]);
				stmt.setFloat(3, relevancies[i]);
				stmt.setFloat(4, relevancies[i]);
				boolean flag = stmt.execute();
				ConfigHandler.getLogger().info(
						"update relevancy success useid=" + user_id
								+ " tags id=" + tags[i] + " rele="
								+ relevancies[i]);
			}
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
				if (conn != null) {
					conn.close();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return true;
	}

	/**
	 * 查询资料是否在标准库 不存在返回-1 否则返回1
	 * 
	 * @param name
	 * @return
	 */
	public static int searchFromProfileStandard(String name, int actionType) {
		int type = -1;
		ResultSet result = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(
					TOOL_DEVELOP_DATABASE_PATH,
					ConfigHandler.getMysqlUsername(),
					ConfigHandler.getMysqlPassword());
			stmt = conn.createStatement();

			String sqlstmt = "SELECT * from profile_std_words where content='"
					+ name + "' and category=" + actionType;
			ConfigHandler.getLogger().info("sql=" + sqlstmt);
			result = stmt.executeQuery(sqlstmt);
			if (result.next()) {
				type = 1;

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
		return type;
	}

	/**
	 * 查询资料是否在参考库 不存在返回-1 否则返回标准库的id
	 * 
	 * @param name
	 * @return
	 */
	public static int searchFromProfileRef(String name, int actionType) {
		int type = -1;
		ResultSet result = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(
					TOOL_DEVELOP_DATABASE_PATH,
					ConfigHandler.getMysqlUsername(),
					ConfigHandler.getMysqlPassword());
			stmt = conn.createStatement();

			String sqlstmt = "SELECT * from profile_ref_words where content='"
					+ name + "'";
			ConfigHandler.getLogger().info("sql=" + sqlstmt);
			result = stmt.executeQuery(sqlstmt);
			if (result.next()) {
				type = result.getInt("profile_std_word_id");
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
		return type;
	}

	/**
	 * 查询资料是否在空值库 不存在返回-1 否则返回1
	 * 
	 * @param name
	 * @return
	 */
	public static int searchFromProfileNull(String name, int actionType) {
		int type = -1;
		ResultSet result = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(
					TOOL_DEVELOP_DATABASE_PATH,
					ConfigHandler.getMysqlUsername(),
					ConfigHandler.getMysqlPassword());
			stmt = conn.createStatement();

			String sqlstmt = "SELECT * from profile_null_words where content='"
					+ name + "'";
			ConfigHandler.getLogger().info("sql=" + sqlstmt);
			result = stmt.executeQuery(sqlstmt);
			if (result.next()) {
				type = 1;
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
		return type;
	}

	/**
	 * 添加新词到资料的空值库
	 * 
	 * @param user_id
	 * @param name
	 * @param actionType
	 * @return
	 */
	public static boolean addNewWordToProfile(String user_id, String name,
			int actionType) {
		System.out.println("write to profile new words " + name);
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(TOOL_DEVELOP_DATABASE_PATH,
					ConfigHandler.getMysqlUsername(),
					ConfigHandler.getMysqlPassword());
			String update = "insert into profile_new_words(content,category,user_id) values (?,?,?) ";
			ConfigHandler.getLogger().info("update sentence=" + update);
			stmt = conn.prepareStatement(update);
			stmt.setString(1, name);
			stmt.setInt(2, actionType);
			stmt.setString(3, user_id);
			boolean flag = stmt.execute();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
				if (conn != null) {
					conn.close();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return true;
	}

	/**
	 * 获取某个资料对应的关键词
	 * 
	 * @param name
	 * @param addTags
	 * @return
	 */
	public static boolean getProfileKeywords(String name, List<String> addTags) {
		ConfigHandler.getLogger().info("getProfileKeywords=" + name);
		ResultSet result = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(
					TOOL_DEVELOP_DATABASE_PATH,
					ConfigHandler.getMysqlUsername(),
					ConfigHandler.getMysqlPassword());
			stmt = conn.createStatement();
			String sqlstmt = "SELECT * from profile_relations where from_field='"
					+ name + "' and is_handled=1 order by links desc limit 10";
			result = stmt.executeQuery(sqlstmt);
			while (result.next()) {
				addTags.add(result.getString("to_field"));
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
		return true;
	}

	/**
	 * 根据id返回标准词记录
	 * @param id
	 * @return
	 */
	public static String getStdWordById(int id){
		ConfigHandler.getLogger().info("getStdWordById=" + id);
		ResultSet result = null;
		Statement stmt = null;
		String std_word=null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(
					TOOL_DEVELOP_DATABASE_PATH,
					ConfigHandler.getMysqlUsername(),
					ConfigHandler.getMysqlPassword());
			stmt = conn.createStatement();
			String sqlstmt = "SELECT * from profile_std_words where id="+id;
			result = stmt.executeQuery(sqlstmt);
			if (result.next()) {
				std_word=result.getString("content");
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
		return std_word;
	}
	public static void main(String[] args) {
	}
}
