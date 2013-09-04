package com.zhidaoba.loggersystem.common;

public class Constants {
	public final static int LOG_READ_FREQUENCY = 1000*60;
	
	public final static String USER_ID = "user_id";
	public final static String LOGTIME_FIELD = "logtime";
	public final static String ACTION_FIELD = "action";
	public final static String CONTENT_FIELD = "content";
	public final static String ISHANDLED_FIELD = "isHandled";
	
	public static final String LOG_BEGIN = "the request begin at ";
	public static final String LOG_ACTION = "visit: ";
	public static final String LOG_CONTENT = "params:";
	public static final String LOG_UID = "current user id is:";
	public static final String LOG_END = "the response over at ";
	public static final String LOG_UNIQUE_ID = "the unique id is ";
	public static final String LOG_ACTION_SPLIT_LEFT = "/:version";
	public static final String LOG_ACTION_SPLIT_RIGHT = "(.:format)";

}
