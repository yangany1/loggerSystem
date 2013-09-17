package com.zhidaoba.emergency;

import java.util.Date;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.zhidaoba.loggersystem.common.ConfigHandler;
import com.zhidaoba.loggersystem.common.*;
public class EmergencyDatabaseHandler {
	public static MongoClient mg = null;
	public static DB db = null;
	public static DBCollection collection;

	public static DBCollection getDBCollection() {
		
		if (mg == null) {
			try {
				ConfigHandler.load();
				mg = new MongoClient(ConfigHandler.getMongodbServer(),
						ConfigHandler.getMongodbPort());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (db == null) {
			db = mg.getDB(ConfigHandler.getMongodbName());
		}
		return db.getCollection(EmergencyConfigHandler.getMongodbCollection());
	}

	/**
	 * 数据库读取紧急问题
	 * 
	 * @param elist
	 *            紧急库的列表
	 */
	public static void getEmergencyFromMongo(List<Emergency> elist) {

		try {
			elist.clear();
			DBCollection coll = getDBCollection();
			DBCursor cursor = coll.find();

			while (cursor.hasNext()) {
				DBObject user = cursor.next();
				Emergency em;
				try{
				em = new Emergency(user.get("question_id").toString(),
						(Date) user.get("created_at"),
						(Date) user.get("updated_at"),
						(Integer) user.get("showed_number"),
						(Double) user.get("mean_matching_degree"), 0);
				}
				catch(Exception e){
					e.printStackTrace();
					ConfigHandler.getLogger().warning("question_id为"+user.get("question_id")+"的数据格式不正确: ");
					continue;
				}
				elist.add(em);
			}
			cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 从紧急库删除某个记录
	 * 
	 * @param question_id
	 * @return 成功返回true，失败返回false
	 */
	public static boolean deleteEmergencyFromMongo(String question_id) {
		try {
			DBCollection coll = getDBCollection();
			DBObject deletePig = new BasicDBObject();
			deletePig.put("question_id", question_id);
			coll.remove(deletePig);
			System.out.println("quesion_id为" + question_id + "已被删除！");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	/**
	 * 向紧急库添加一个问题
	 * 
	 * @param question_id
	 * @param mean_matching_degree
	 * @return
	 */
	public static boolean addNewEmergency(String question_id,
			double mean_matching_degree) {
		long enter_time = System.currentTimeMillis()
				/ Constants.MILLSECONDSTOMINUTE;
		int emerge_showed = 0;
		try {
			DBCollection coll = getDBCollection();
			DBObject insertData = new BasicDBObject();
			insertData.put("question_id", question_id);
			insertData.put("enter_time", enter_time);
			insertData.put("emerge_showed", emerge_showed);
			insertData.put("mean_matching_degree", mean_matching_degree);
			coll.insert(insertData);
			System.out.println("quesion_id为" + question_id + "已被添加到紧急库！");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 更新某个紧急事件
	 * 
	 * @param e
	 * @return
	 */
	public static boolean updateEmergency(Emergency e) {
		try {
			DBCollection coll = getDBCollection();
			DBObject updateObj = new BasicDBObject();
			updateObj.put("question_id", e.getQuestion_id());
			updateObj.put("created_at", e.getCreated_at());
			updateObj.put("updated_at", e.getUpdated_at());
			updateObj.put("showed_number", e.getShowed_number());
			updateObj.put("mean_matching_degree", e.getMean_matching_degree());
			updateObj.put("order_value", e.getOrder_value());

			// 更新

			coll.update(new BasicDBObject("question_id", e.getQuestion_id()),
					updateObj, false, false);
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}

	}
}
