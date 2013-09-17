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

	/**
	 * 从mongodb中查询某个字段的值
	 * @param colname
	 * @param field
	 * @param value
	 * @return
	 */
	public static DBObject getItemFromMongoDB(String collectionname, String field, String value) {
        MongoClient client = null;
        DBCursor cursor = null;
        try {
            ConfigHandler.getLogger().info("mongo sever=" + ConfigHandler.getMongodbServer() + ",mongo port=" + ConfigHandler.getMongodbPort() + ",name=" + ConfigHandler.getMongodbName());
            client = new MongoClient(
                    ConfigHandler.getMongodbServer(),
                    ConfigHandler.getMongodbPort());
            DB db = client.getDB(ConfigHandler.getMongodbName());
            ConfigHandler.getLogger().info("collectionname=" + collectionname + ",field=" + field + ",value=" + value);
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

            String sqlstmt = "SELECT " + "`to`"
                    + " from " + Constants.TABLE_RELATIONSHIPS_NAME
                    + " where `from` = " + "\"" + from + "\" ORDER BY " + Constants.VALUE_FIELD + " DESC limit 100";
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
	 * @param user_id
	 * @param tags
	 * @param relevancies
	 */
	 public static boolean
	    updateRelevancyToMysql(String user_id, String[] tags, float[] relevancies) {
	        System.out.println("update relevancy in mysql length=" + tags.length);
	        Connection conn = null;
	        PreparedStatement stmt = null;
	        try {
	            Class.forName("com.mysql.jdbc.Driver");
	            conn = DriverManager.getConnection(
	                    ConfigHandler.getMysqlPath(),
	                    ConfigHandler.getMysqlUsername(),
	                    ConfigHandler.getMysqlPassword());
	            for (int i = 0; i < tags.length; i++) {
	                String update = "insert into " +Constants.TABLE_RELEVANCY_NAME + " values (?,?,?) on duplicate key update relevancy=?";
	                ConfigHandler.getLogger().info("update sentence=" + update);
	                stmt = conn.prepareStatement(update);
	                stmt.setString(2, user_id);
	                stmt.setString(1, tags[i]);
	                stmt.setFloat(3, relevancies[i]);
	                stmt.setFloat(4, relevancies[i]);
	                boolean flag = stmt.execute();
	                ConfigHandler.getLogger().info("update relevancy success useid=" + user_id + " tags id=" + tags[i] + " rele=" + relevancies[i]);
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
}
