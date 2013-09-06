package com.zhidaoba.loggersystem.common;

public class Constants {
	public final static int LOG_READ_FREQUENCY = 1000 * 60;

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

	public static final String USER_LOGS = "userlogs";
	public static final String ISHANDLED_LOG = "isHandled";
	public static final String LOG_USERID_FIELD = "user_id";
	public static final String LOG_LOGTIME_FIELD = "logtime";
	public static final String LOG_ACTION_FIELD = "action";
	public static final String LOG_CONTENT_FIELD = "content";
	
	public final static String TABLE_RELEVANCY_NAME = "tag_expert_relevancy";
	public final static String TAG = "tag";
	public final static String RELEVANCY = "relevancy";
	
	public final static int DIALOG_ASKER=0;
	public final static int DIALOG_ANSWER=1;
	public final static int DIALOG_PUSH_TIME=2;
	public final static int DIALOG_ACCEPT_CHAT_TIME=3;
	public final static int DIALOG_ASK_WORD_LENGTH=4;
	public final static int DIALOG_ANSWER_WORD_LENGTH=5;
	public final static int DIALOG_ASK_TIMESTAMP=6;
	public final static int DIALOG_ANSWER_TIMESTAMP=7;
	public final static int DIALOG_LAST_UPDATE_TIME=8;
	public final static int DIALOG_FIRST_ANSWER_TIME=9;
	public final static int DIALOG_ASK_PAUSE_TIME=10;
	public final static int DIALOG_ANSWER_PAUSE_TIME=11;
	public final static int DIALOG_ASK_STAR_NUM=12;
	public final static int DIALOG_ANSWER_STAR_NUM=13;
	public final static int DIALOG_ASK_IS_COMMENT=14;
	public final static int DIALOG_ANSWER_IS_COMMENT=15;
	public final static int DIALOG_ASK_SHARE=16;
	public final static int DIALOG_ANSWER_SHARE=17;
	public final static int DIALOG_ASK_EVALUATE=18;
	public final static int DIALOG_ANSWER_EVALUATE=19;
	public final static int DIALOG_ASK_UPDATE_TIME=20;
	public final static int DIALOG_ANSWER_UPDATE_TIME=21;
	public final static int EVENT_KIND=22;
	
	
	public static final String CONTRIBUTION_COLLECTION = "contribution";
    public static final String VALUE_FIELD_IN_CONTRIBUTION_COLLECTION = "value";
    public static final String ORIGIN_KEYWORDS_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION = "origin_keywords";
   
    public static final String CONSUMATION_COLLECTION = "consumation";
    public static final String VALUE_FIELD_IN_CONSUMATION_COLLECTION = "value";
}
