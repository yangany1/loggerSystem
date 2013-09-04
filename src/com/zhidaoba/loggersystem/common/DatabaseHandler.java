package com.zhidaoba.loggersystem.common;

import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.zhidaoba.loggersystem.relevancy.RelevancyObject;

/**
 * 处理与数据库交互的类
 * 包括mysql数据和Mongodb数据
 * @author luo
 *
 */
public class DatabaseHandler {
	
	/**
	 * 将日志数据写入mongo数据库
	 * @param changeList
	 */
	public static void writeLogToMongoDB(ArrayBlockingQueue<RelevancyObject> changeList) {
        MongoClient client = null;
        try {
            client = new MongoClient(
                    ConfigHandler.getMongodbServer(),
                    ConfigHandler.getMongodbPort());
            DB db = client.getDB(ConfigHandler.getMongodbName());
            DBCollection coll = db.getCollection("userlogs");
            for (RelevancyObject content : changeList) {
                BasicDBObject doc = new BasicDBObject(Constants.USER_ID, content.getUserId()).
                        append(Constants.LOGTIME_FIELD, content.getTime()).
                        append(Constants.ACTION_FIELD, content.getAction()).
                        append(Constants.CONTENT_FIELD, content.getContent()).append(Constants.ISHANDLED_FIELD, false);
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
}
