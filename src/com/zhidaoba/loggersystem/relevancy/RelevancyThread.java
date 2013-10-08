package com.zhidaoba.loggersystem.relevancy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.commons.lang3.ArrayUtils;
import com.google.gson.Gson;
import com.mongodb.DBObject;
import com.zhidaoba.loggersystem.common.ConfigHandler;
import com.zhidaoba.loggersystem.common.Constants;
import com.zhidaoba.loggersystem.common.DatabaseHandler;

public class RelevancyThread {
	// 存储用户和标签的相关度
	private Map<String, Map<String, Float>> expertTagRelevancy;
	private Gson gson = new Gson();
	// private Calendar today = Calendar.getInstance();
	private Map<String, ArrayList<Object>> dialogInfos = new HashMap<String, ArrayList<Object>>();
	private CalculateDetail cal = null;
	private Calendar calendar = null;
	// 记录用户每天提问或者回答的问题个数
	private Map<String, Map<String, Integer>> userAskedTimesToday = new HashMap<String, Map<String, Integer>>();
	private Map<String, Map<String, Integer>> userAnsweredTimesToday = new HashMap<String, Map<String, Integer>>();

	public void init() {
		expertTagRelevancy = new HashMap<String, Map<String, Float>>();
		DatabaseHandler.getRelevancyFromMysql(expertTagRelevancy);
		ConfigHandler.getLogger().info(
				"expertTag size=" + expertTagRelevancy.size());
		cal = new CalculateDetail(dialogInfos, userAskedTimesToday,
				userAnsweredTimesToday);
	}

	/**
	 * 主线程
	 */
	public void run() {
		this.init();
		this.update();
	}

	/**
	 * 定时更新线程 1、定时读取日志文件并写入数据库 2、分析用户的行为。计算关联度
	 */
	public void update() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				System.out.println("run anlysis");
				CollectLog.readLog();
				long time1 = System.currentTimeMillis();
				analysisFromMongoDB();
				System.out.println("time consure:"
						+ (System.currentTimeMillis() - time1));
			}
		};
		Timer timer = new Timer();
		timer.schedule(task, 0, Constants.LOG_READ_FREQUENCY);
	}

	/**
	 * 对每个log记录进行分析操作
	 * 
	 * @return
	 */
	private boolean analysisFromMongoDB() {
		List<DBObject> logList = DatabaseHandler
				.getRelevancyObjectsFromMongoDB();
		for (DBObject obj : logList) {
			RelevancyObject r = new RelevancyObject(
					(String) obj.get(Constants.LOG_USERID_FIELD),
					(Long) obj.get(Constants.LOG_LOGTIME_FIELD),
					(String) obj.get(Constants.LOG_ACTION_FIELD),
					(String) obj.get(Constants.LOG_CONTENT_FIELD),
					(String) obj.get(Constants.LOG_DIALOG_ID_FIELD));
			try {
				ConfigHandler.getLogger().info("logtime=" + r.getTime());
				analysis(r);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			DatabaseHandler.updateLogState(obj);
		}

		return true;
	}

	/**
	 * Create_Question需要做的事情： 1、新建一个dialog的map 2、记录提问者的id 3、初始化提问者的平均响应时间
	 * 4、初始化回答者的平均响应时间 5、记录问题推送的时间 6、初始化提问者的对话平均长度 7、初始化回答者的对话平均长度
	 * 
	 * Accept_Chat需要做的事情： 1、记录回答者的id 2、记录回答者接受会话的时间
	 * 
	 * Send_Message需要做的事情： a 对提问者： 1、更新提问者的平均回答长度 2、更新提问者的平均响应时间 b 对回答者：
	 * 1、记录回答者第一次回答的时间 2、更新回答者的平均响应时间 3、更新回答者的平均回答长度
	 * 
	 * @param log
	 * @throws Exception
	 */
	private void analysis(RelevancyObject log) throws Exception {
		// calendar存数当然正在分析的log的时间
		if (calendar == null) {
			calendar = Calendar.getInstance();
			calendar.setTimeInMillis(log.getTime());
		}
		try {
			ConfigHandler.getLogger().info(
					"log content=" + log.getContent() + "userid="
							+ log.getUserId());
			ContentParser content = gson.fromJson(log.getContent(),
					ContentParser.class);
			switch (log.actionType()) {
			case EVALUATE:
				ConfigHandler.getLogger().info("EVALUATE");
				evaluationEvent(log, content);
				break;
			case CREATE_QUESTION:
				ConfigHandler.getLogger().info("CREATE_QUESTION");
				createQuestionEvent(log, content);
				break;
			case ACCEPT_CHAT:
				ConfigHandler.getLogger().info("ACCEPT_CHAT");
				acceptChatEvent(log, content);
				break;
			case SEND_MESSAGE:
				ConfigHandler.getLogger().info("SEND_MESSAGE");
				sendMessageEvent(log, content);
				break;
			case LOGOUT:
				ConfigHandler.getLogger().info("LOG OUT");
				LogoutEvent(log, content);
				break;
			case ADD_TAG:
				ConfigHandler.getLogger().info("ADD_TAG");
				this.updateKnowledge(UserAction.ADD_TAG, log.getUserId(), 10,
						content);
				break;
			case ADD_SCHOOL_INFO:
				ConfigHandler.getLogger().info("ADD_SCHOOL_INFO");
				this.updateKnowledge(UserAction.PERSONAL_INFO_ADD_COLLEGE,
						log.getUserId(), 20, content);// school
				this.updateKnowledge(UserAction.PERSONAL_INFO_ADD_MAJOR,
						log.getUserId(), 20, content);// major
				break;
			case UPDATE_COMPANY_INFO:
				ConfigHandler.getLogger().info("UPDATE_COMPANY_INFO");
				this.updateKnowledge(UserAction.PERSONAL_INFO_ADD_COMPANY,
						log.getUserId(), 20, content);// company
				this.updateKnowledge(UserAction.PERSONAL_INFO_ADD_TITLE,
						log.getUserId(), 20, content);// position
				break;
			case AGREE:
				ConfigHandler.getLogger().info("AGREE");
				// 此处是针对log处理的
				if (!log.getDialog_id().equals("")) {
					content.setDialog_id(log.getDialog_id());
					this.updateKnowledge(UserAction.AGREE, log.getUserId(),
							0.5f, content);
				}
				break;
			case DETAIL:
				ConfigHandler.getLogger().info("DETAIL");
				this.updateKnowledge(UserAction.DETAIL, log.getUserId(), 0.2f,
						content);
				break;
			case COMMENT:
				ConfigHandler.getLogger().info("COMMENT");
				this.updateKnowledge(UserAction.COMMENT, log.getUserId(), 0.8f,
						content);
				break;
			case REGISTER_PROFILE:
				ConfigHandler.getLogger().info("REGISTER_PROFILE");
				this.dealWithRegisterProfile(log, content);
			default:
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * 处理用户刚注册时填写的信息，增加知识量
	 * 
	 * @param userid
	 * @param content
	 * @return
	 */
	public boolean dealWithRegisterProfile(RelevancyObject log,
			ContentParser content) {
		if (content.getCollegeName() != null && content.getCollegeName() != "") {
			updateProfileValue(log, content.getCollegeName(),
					Constants.TYPE_SCHOOL);
		}
		if (content.getMajorName() != null && content.getMajorName() != "") {
			updateProfileValue(log, content.getMajorName(),
					Constants.TYPE_MAJOR);
		}
		if (content.getPositionTitle() != null
				&& content.getPositionTitle() != "") {
			updateProfileValue(log, content.getPositionTitle(),
					Constants.TYPE_POSITION);
		}
		if (content.getCompanyName() != null && content.getCompanyName() != "") {
			updateProfileValue(log, content.getCompanyName(),
					Constants.TYPE_COMPANY);
		}

		return true;
	}

	/**
	 * 更新某个资料的知识量
	 * 
	 * @param log
	 * @param name
	 * @return
	 */
	public boolean updateProfileValue(RelevancyObject log, String name,
			int actionType) {
		//查询标准库
		System.out.println("update profile =" + name + "  type is:"
				+ actionType);
		int type = DatabaseHandler.searchFromProfileStandard(name, actionType);

		//存在标准库，更新权值
		if (type == 1) {
			System.out.println(name+" exist");
//			DatabaseHandler.addToProfileNewWord(name, actionType);
		}
		//不存在标准库，查看参考库
		else {
			int r=DatabaseHandler.searchFromProfileRef(name,actionType);
			//存在参考库，返回标准库词的id
		    if(r!=-1){
		    	System.out.println(name+" exist in ref table "+r);
		    }
		    //不存在参考库，查看空值库
		    else{
		    	r=DatabaseHandler.searchFromProfileNull(name, actionType);
		    	//在空值库，忽略
		    	if(r==1){
		    		return true;
		    	}
		    	//不在空值库，加入新词库
		    	else{
		    		DatabaseHandler.addNewWordToProfileNull(log.getUserId(), name, actionType);
		    	}
		    }
		    	
		}

		return true;
	}

	/**
	 * 评价的日志分析
	 * 
	 * @param log
	 * @param content
	 * @return
	 */
	public boolean evaluationEvent(RelevancyObject log, ContentParser content) {
		if (log.getUserId() == null)
			return false;
		System.out.println(log.getUserId());
		System.out.println("test dialog_id=" + content.getDialogID());
		this.dialogInfos.get(content.getDialogID()).get(Constants.DIALOG_ASKER);
		// 对于提问者
		if (log.getUserId().equals(
				this.dialogInfos.get(content.getDialogID()).get(
						Constants.DIALOG_ASKER))) {
			// 记录提问者的打分
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ASK_STAR_NUM, content.getStarNumber());
			// 记录提问者是否评价
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ASK_IS_COMMENT,
					content.getContent().isEmpty());
			// 记录提问者是否分享
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ASK_SHARE, content.getIsShare());
			// 标记提问者已经评价
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ASK_EVALUATE, true);

		}
		// 对于回答者
		else if (log.getUserId().equals(
				this.dialogInfos.get(content.getDialogID()).get(
						Constants.DIALOG_ANSWER))) {
			// 记录回答者的打分
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ANSWER_STAR_NUM, content.getStarNumber());
			// 记录回答者是否评论
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ANSWER_IS_COMMENT,
					content.getContent().isEmpty());
			// 记录回答者是否分享
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ANSWER_SHARE, content.getIsShare());
			// 记录回答者是否评价
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ANSWER_EVALUATE, true);
		}
		// 如果双方都评价过
		if ((Boolean) this.dialogInfos.get(content.getDialogID()).get(
				Constants.DIALOG_ASK_EVALUATE)
				&& (Boolean) this.dialogInfos.get(content.getDialogID()).get(
						Constants.DIALOG_ANSWER_EVALUATE)) {
			// 更新提问者的消耗值
			this.updateConsumation(content.getDialogID());
			// 更新回答者的贡献值
			this.updateContribution(content.getDialogID());
			// 更新提问者的知识量
			this.updateKnowledge(UserAction.EVALUATE, (String) this.dialogInfos
					.get(content.getDialogID()).get(Constants.DIALOG_ASKER),
					cal.getContribution(content.getDialogID()) * 0.5, content);
			// 更新回答者的知识量
			this.updateKnowledge(UserAction.EVALUATE, (String) this.dialogInfos
					.get(content.getDialogID()).get(Constants.DIALOG_ANSWER),
					cal.getContributionWeight(content.getDialogID()) * 2,
					content);
			// 删除这条对话
			this.dialogInfos.remove(content.getDialogID());
		}
		return true;
	}

	/**
	 * 分析创建问题的log
	 * 
	 * @param content
	 * @param userid
	 * @return
	 */
	public boolean createQuestionEvent(RelevancyObject log,
			ContentParser content) {
		// 此处是针对log处理的
		content.setDialog_id(log.getDialog_id());
		// 已经分析过这个dialog
		if (this.dialogInfos.containsKey(content.getDialogID())) {
			ConfigHandler.getLogger().warning(
					"DIALOG_ID DUPLICATED:" + content.getDialogID());

			return true;
		} else {
			addDialogToList(content.getDialogID());
			initCreateQuesion(log, content);
		}
		return true;
	}

	private boolean initCreateQuesion(RelevancyObject log, ContentParser content) {
		// 增加提问者的当天的提问问题个数
		Calendar current = Calendar.getInstance();
		current.setTimeInMillis(log.getTime());
		// 如果是新的一天，则清空当天提问和回答的数据结构
		if (calendar.get(Calendar.DAY_OF_MONTH) != current
				.get(Calendar.DAY_OF_MONTH)) {
			clearUserAskQuestion(log.getUserId(), content.getDialogID());
			clearUserAnswerQuestion(log.getUserId(), content.getDialogID());
			calendar = current;
		} else {
			updateUserAskQuestion(log.getUserId(), content.getDialogID());
		}
		ConfigHandler.getLogger().info(
				"create question set question askid=" + log.getUserId()
						+ ",dialogid=" + content.getDialogID());
		// 记录提问者的id
		this.dialogInfos.get(content.getDialogID()).set(Constants.DIALOG_ASKER,
				log.getUserId());
		// 初始化pushtime
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_PUSH_TIME, log.getTime());
		// 初始化accept chat time
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ACCEPT_CHAT_TIME, log.getTime());
		// 记录提问者的平均长度
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ASK_WORD_LENGTH, 0);
		// 记录回答者的平均长度
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ANSWER_WORD_LENGTH, 0);
		// 记录提问者的响应时间
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ASK_TIMESTAMP, new Long(0));
		// 记录回答者的响应时间
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ANSWER_TIMESTAMP, new Long(0));
		// 记录回答者第一次回答的时间
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_FIRST_ANSWER_TIME, log.getTime());
		// 初始化提问者和回答者的暂停次数
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ASK_PAUSE_TIME, 0);
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ANSWER_PAUSE_TIME, 0);
		// 初始提问者者评价的星级
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ASK_STAR_NUM, 0);
		// 初始回答者者评价的星级
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ANSWER_STAR_NUM, 0);
		// 初始提问者者是否评价
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ASK_IS_COMMENT, false);
		// 初始回答者是否评价
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ANSWER_IS_COMMENT, false);
		// 初始提问者者是否分享
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ASK_SHARE, false);
		// 初始回答者是否分享
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ANSWER_SHARE, false);
		// 设置提问者的上次活动时间
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ASK_UPDATE_TIME, log.getTime());
		// 初始化提问者是否评价
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ASK_EVALUATE, false);
		// 初始化回答者是否评价
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ANSWER_EVALUATE, false);
		return true;
	}

	/**
	 * 增加dialogInfos中一个dialog，并且初始化
	 */
	public boolean addDialogToList(String dialogid) {
		ArrayList<Object> list = new ArrayList<Object>();
		for (int i = 0; i < Constants.EVENT_KIND; i++) {
			list.add(null);
		}
		this.dialogInfos.put(dialogid, list);
		return true;
	}

	/**
	 * 接受问题的事件
	 * 
	 * @param log
	 * @param content
	 * @return
	 */
	public boolean acceptChatEvent(RelevancyObject log, ContentParser content) {
		// 此处针对日志进行的处理
		if (log.getDialog_id().equals("")) {
			return false;
		}
		content.setDialog_id(log.getDialog_id());
		try {
			Calendar current = Calendar.getInstance();
			current.setTimeInMillis(log.getTime());
			// 如果是新的一天，则清空当天回答和提问的数据结构
			if (calendar.get(Calendar.DAY_OF_MONTH) != current
					.get(Calendar.DAY_OF_MONTH)) {
				clearUserAskQuestion(log.getUserId(), content.getDialogID());
				clearUserAnswerQuestion(log.getUserId(), content.getDialogID());
				calendar = current;
			} else {
				updateUserAnswerQuestion(log.getUserId(), content.getDialogID());
			}
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ANSWER, log.getUserId());
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ACCEPT_CHAT_TIME, log.getTime());
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ANSWER_UPDATE_TIME, log.getTime());
			// 第一次回答的时间
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_FIRST_ANSWER_TIME, log.getTime());

		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 发送消息的事件
	 * 
	 * @param log
	 * @param content
	 * @return
	 */
	public boolean sendMessageEvent(RelevancyObject log, ContentParser content) {

		// 提问者的处理
		if (log.getUserId().equals(
				this.dialogInfos.get(content.getDialogID()).get(
						Constants.DIALOG_ASKER))) {
			ConfigHandler.getLogger().info("ASKER");
			// diff是响应时间,提问者的平均响应时间
			long diff = (log.getTime() - (Long) this.dialogInfos.get(
					content.getDialogID())
					.get(Constants.DIALOG_ASK_UPDATE_TIME)) / 1000;
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ASK_UPDATE_TIME, log.getTime());
			// 提问者的平均回答长度
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ASK_WORD_LENGTH,
					(Integer) this.dialogInfos.get(content.getDialogID()).get(
							Constants.DIALOG_ASK_WORD_LENGTH)
							+ content.getContent().length());

			// 提问者回答的平均响应时间
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ASK_TIMESTAMP,
					(Long) this.dialogInfos.get(content.getDialogID()).get(
							Constants.DIALOG_ASK_TIMESTAMP)
							+ diff);
		}
		// 回答者的处理
		else if (log.getUserId().equals(
				this.dialogInfos.get(content.getDialogID()).get(
						Constants.DIALOG_ANSWER))) {
			ConfigHandler.getLogger().info("ANSWERER");
			// 设置回答者的平均响应时间
			long diff = (log.getTime() - (Long) this.dialogInfos.get(
					content.getDialogID()).get(
					Constants.DIALOG_ANSWER_UPDATE_TIME)) / 1000;
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ANSWER_UPDATE_TIME, log.getTime());

			// 设置回答内容的长度
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ANSWER_WORD_LENGTH,
					(Integer) this.dialogInfos.get(content.getDialogID()).get(
							Constants.DIALOG_ANSWER_WORD_LENGTH)
							+ content.getContent().length());
			// 设置回答的平均响应时间
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ANSWER_TIMESTAMP,
					(Long) this.dialogInfos.get(content.getDialogID()).get(
							Constants.DIALOG_ANSWER_TIMESTAMP)
							+ diff);
		}
		return true;
	}

	/**
	 * 退出登录的event
	 * 
	 * @param log
	 * @param content
	 * @return
	 */
	public boolean LogoutEvent(RelevancyObject log, ContentParser content) {
		// 此处是针对log处理的
		content.setDialog_id(log.getDialog_id());
		if (log.getDialog_id() == null) {
			return false;
		}
		// 对于提问者,增加暂停次数
		if (log.getUserId().equals(
				log.getUserId().equals(
						this.dialogInfos.get(content.getDialogID()).get(
								Constants.DIALOG_ASKER)))) {
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ASK_PAUSE_TIME,
					(Integer) this.dialogInfos.get(content.getDialogID()).get(
							Constants.DIALOG_ASK_PAUSE_TIME) + 1);
		}
		// 对于回答者者,增加暂停次数
		if (log.getUserId().equals(
				log.getUserId().equals(
						this.dialogInfos.get(content.getDialogID()).get(
								Constants.DIALOG_ANSWER)))) {
			this.dialogInfos.get(content.getDialogID()).set(
					Constants.DIALOG_ANSWER_PAUSE_TIME,
					(Integer) this.dialogInfos.get(content.getDialogID()).get(
							Constants.DIALOG_ANSWER_PAUSE_TIME) + 1);
		}
		return true;
	}

	/**
	 * 更新回答者的贡献量
	 */
	private boolean updateContribution(String dialogid) {
		double contributerValue = cal.getContribution(dialogid);
		String answerid = (String) this.dialogInfos.get(dialogid).get(
				Constants.DIALOG_ANSWER);
		DatabaseHandler.updateContribution(answerid, contributerValue);
		ConfigHandler.getLogger().info(
				"update contribution userid=" + answerid + ",value="
						+ contributerValue);
		return true;
	}

	/**
	 * 更新提问者的消耗量
	 */
	private boolean updateConsumation(String dialogid) {
		double consumerValue = cal.getConsumation(dialogid);
		String askid = (String) this.dialogInfos.get(dialogid).get(
				Constants.DIALOG_ASKER);
		DatabaseHandler.updateConsumation(askid, consumerValue);
		ConfigHandler.getLogger().info(
				"update consumation userid=" + askid + ",value="
						+ consumerValue);
		return true;
	}

	private boolean updateKnowledge(UserAction action, String userid,
			double addValue, ContentParser content) {
		ConfigHandler.getLogger().info("update knowledge");
		float beta = 0;
		String[] tags;
		switch (action) {
		case EVALUATE:
			DBObject obj = DatabaseHandler.getItemFromMongoDB(
					Constants.DIALOG_KEYWORDS_COLLECTION, Constants.DIALOG_ID,
					content.getDialogID());
			if (null != obj) {
				tags = ((String) obj.get(Constants.STD_KEYWORDS_FIELD))
						.split(Constants.TAG_SPLITER);
				beta = 0.08f;
				updateExpertRelevancy(userid, tags, addValue, beta);
			}
			break;
		case ADD_TAG:
			tags = content.getTags().split(Constants.TAG_SPLITER);
			beta = 0.2f;
			updateExpertRelevancy(userid, tags, addValue, beta);
			break;
		case ADD_SCHOOL_INFO:
			beta = 0.1f;
			break;
		case UPDATE_COMPANY_INFO:
			beta = 0.1f;
			break;
		// 此处有问题，需要修改
		case AGREE:
			DBObject agreeobj = DatabaseHandler.getItemFromMongoDB(
					Constants.DIALOG_KEYWORDS_COLLECTION, Constants.DIALOG_ID,
					content.getDialogID());
			if (null != agreeobj) {
				tags = ((String) agreeobj.get(Constants.STD_KEYWORDS_FIELD))
						.split(Constants.TAG_SPLITER);
				beta = 0.03f;
				updateExpertRelevancy(userid, tags, addValue, beta);
			}
			break;
		case DETAIL:
			DBObject detailobj = DatabaseHandler.getItemFromMongoDB(
					Constants.DIALOG_KEYWORDS_COLLECTION, Constants.DIALOG_ID,
					content.getDialogID());
			if (null != detailobj) {
				tags = ((String) detailobj.get(Constants.STD_KEYWORDS_FIELD))
						.split(Constants.TAG_SPLITER);
				beta = 0.02f;
				updateExpertRelevancy(userid, tags, addValue, beta);
			}
			break;
		case COMMENT:
			DBObject commentobj = DatabaseHandler.getItemFromMongoDB(
					Constants.DIALOG_KEYWORDS_COLLECTION, Constants.DIALOG_ID,
					content.getDialogID());
			if (null != commentobj) {
				tags = ((String) commentobj.get(Constants.STD_KEYWORDS_FIELD))
						.split(Constants.TAG_SPLITER);
				beta = 0.05f;
				updateExpertRelevancy(userid, tags, addValue, beta);
			}
			break;
		default:
			break;
		}
		return true;
	}

	/**
	 * 更新用户标签的相关度
	 * 
	 * @param userid
	 * @param tags
	 * @param addValue
	 * @param beta
	 * @return
	 */
	private boolean updateExpertRelevancy(String userid, String[] tags,
			double addValue, double beta) {
		// tags[0]是“$$**”,需要变成"**",所以要删除前面2个字符
		tags[0] = tags[0].substring(2);
		double weight = addValue / tags.length;
		ArrayList<String> toAddTags = new ArrayList<String>();
		ArrayList<Float> toAddRelevancies = new ArrayList<Float>();
		for (String tag : tags) {
			ArrayList<String> relaWords = DatabaseHandler
					.getRelationshipsFromMysql(tag);
			ConfigHandler.getLogger().info(
					"relaWords number=" + relaWords.size());
			relaWords.add(0, tag);
			float s = (float) weight;
			for (String word : relaWords) {
				try {
					if (!this.expertTagRelevancy.containsKey(userid)) {
						Map<String, Float> maps = new HashMap<String, Float>();
						maps.put(word, (float) 0.0);
						this.expertTagRelevancy.put(userid, maps);
					}
					if (!this.expertTagRelevancy.get(userid).containsKey(word)) {
						this.expertTagRelevancy.get(userid).put(word,
								(float) 0.0);
					}
					float alpha = this.expertTagRelevancy.get(userid).get(word);
					float added = (float) ((1 - alpha) * beta);
					if (1 < beta) {
						added = alpha;
					}
					float curr = alpha + added;
					this.expertTagRelevancy.get(userid).put(word, curr);
					if (toAddTags.contains(word)) {
						toAddRelevancies.set(toAddTags.indexOf(word), curr);
					} else {
						toAddRelevancies.add(curr);
						toAddTags.add(word);
					}
					ConfigHandler.getLogger().info("s=" + s);
					s = s - added;
					if (s <= 0) {
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		}
		ConfigHandler.getLogger()
				.info("to add tags number=" + toAddTags.size());
		// ArrayList to array
		String[] stag = new String[toAddTags.size()];
		int i = 0;
		for (String tag : toAddTags) {
			stag[i++] = tag;
		}
		Float[] frele = new Float[toAddRelevancies.size()];
		i = 0;
		for (Float r : toAddRelevancies) {
			frele[i++] = r;
		}
		DatabaseHandler.updateRelevancyToMysql(userid, stag,
				ArrayUtils.toPrimitive(frele, 0.0f));
		return true;
	}

	/**
	 * 清空userAskedTimesToday数据结构，表示新的一天
	 * 
	 * @param userid
	 * @param dialogid
	 * @return
	 */
	private boolean clearUserAskQuestion(String userid, String dialogid) {
		userAskedTimesToday.clear();
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put(dialogid, 1);
		userAskedTimesToday.put(userid, map);
		return true;
	}

	/**
	 * 清空userAnsweredTimesToday数据结构，表示新的一天
	 * 
	 * @param userid
	 * @param dialogid
	 * @return
	 */
	private boolean clearUserAnswerQuestion(String userid, String dialogid) {
		userAnsweredTimesToday.clear();
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put(dialogid, 1);
		userAnsweredTimesToday.put(userid, map);
		return true;
	}

	/**
	 * 更新userAskedTimesToday
	 * 
	 * @param userid
	 * @param dialogid
	 * @return
	 */
	private boolean updateUserAskQuestion(String userid, String dialogid) {
		// 如果map里没有这个用户，将这个问题设置为该用户当天提问的第一个问题
		if (!userAskedTimesToday.containsKey(userid)) {
			Map<String, Integer> map = new HashMap<String, Integer>();
			map.put(dialogid, 1);
			userAskedTimesToday.put(userid, map);
		} else {
			// 得到用户当天提问的问题数，然后将此问题加1
			int currentNum = userAskedTimesToday.get(userid).size();
			userAskedTimesToday.get(userid).put(dialogid, currentNum + 1);
		}
		return true;
	}

	/**
	 * 更新userAnsweredTimesToday
	 * 
	 * @param userid
	 * @param dialogid
	 * @return
	 */
	private boolean updateUserAnswerQuestion(String userid, String dialogid) {
		// 如果map里没有这个用户，将这个问题设置为该用户当天提问的第一个问题
		if (!userAnsweredTimesToday.containsKey(userid)) {
			Map<String, Integer> map = new HashMap<String, Integer>();
			map.put(dialogid, 1);
			userAnsweredTimesToday.put(userid, map);
		} else {
			// 得到用户当天提问的问题数，然后将此问题加1
			int currentNum = userAnsweredTimesToday.get(userid).size();
			userAnsweredTimesToday.get(userid).put(dialogid, currentNum + 1);
		}
		return true;
	}
}
