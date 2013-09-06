package com.zhidaoba.loggersystem.relevancy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

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

	public void init() {
		expertTagRelevancy = new HashMap<String, Map<String, Float>>();
		DatabaseHandler.getRelevancyFromMysql(expertTagRelevancy);
		ConfigHandler.getLogger().info(
				"expertTag size=" + expertTagRelevancy.size());
	}

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
					(String) obj.get(Constants.LOG_CONTENT_FIELD));
			try {
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
		try {
			Calendar tmp = Calendar.getInstance();
			ConfigHandler.getLogger().info("log content=" + log.getContent());
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
			// case LOGOUT:
			// // ASKER
			// if (log.getUserId().equals(
			// this.dialogInfos.get(content.getDialogID()).get(0))) {
			// this.dialogInfos.get(content.getDialogID()).set(
			// 10,
			// (Integer) this.dialogInfos.get(
			// content.getDialogID()).get(10) + 1);
			// }
			// // ANSWERER
			// else if (log.getUserId().equals(
			// this.dialogInfos.get(content.getDialogID()).get(1))) {
			// this.dialogInfos.get(content.getDialogID()).set(
			// 11,
			// (Integer) this.dialogInfos.get(
			// content.getDialogID()).get(11) + 1);
			// }
			// break;
			// case CONTINUE:
			// this.dialogInfos.get(content.getDialogID()).set(8,
			// log.getTime());
			// break;
			//
			// case UPDATE_TAG:
			// this.updateKnowledge(UserAction.UPDATE_TAG, log.getUserId(),
			// 10, content);
			// break;
			// case ADD_SCHOOL_INFO:
			// this.updateKnowledge(UserAction.PERSONAL_INFO_ADD_COLLEGE,
			// log.getUserId(), 20, content);// school
			// this.updateKnowledge(UserAction.PERSONAL_INFO_ADD_MAJOR,
			// log.getUserId(), 20, content);// major
			// break;
			// case UPDATE_COMPANY_INFO:
			// this.updateKnowledge(UserAction.PERSONAL_INFO_ADD_COMPANY,
			// log.getUserId(), 20, content);// company
			// this.updateKnowledge(UserAction.PERSONAL_INFO_ADD_TITLE,
			// log.getUserId(), 20, content);// position
			// break;
			// case AGREE:
			// this.updateKnowledge(UserAction.AGREE, log.getUserId(),
			// ratio * 0.5f, content);
			// break;
			// case DETAIL:
			// this.updateKnowledge(UserAction.DETAIL, log.getUserId(),
			// ratio * 0.2f, content);
			// break;
			// case COMMENT:
			// this.updateKnowledge(UserAction.COMMENT, log.getUserId(), 0.8f,
			// content);
			// case REMOVE_SCHOOL_INFO:
			// break;
			default:
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * 评价的日志分析
	 * 
	 * @param log
	 * @param content
	 * @return
	 */
	public boolean evaluationEvent(RelevancyObject log, ContentParser content) {
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
			this.dialogInfos.get(content.getDialogID()).set(18, true);

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
			this.updateConsumation(log.getUserId(), content.getDialogID());
			// 更新回答者的贡献值
			this.updateContribution(log.getUserId(), content.getDialogID());
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
				Constants.DIALOG_ASK_TIMESTAMP, 0);
		// 记录回答者的响应时间
		this.dialogInfos.get(content.getDialogID()).set(
				Constants.DIALOG_ANSWER_TIMESTAMP, 0);
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

		try {
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
				this.dialogInfos.get(content.getDialogID()).get(0))) {
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
					(Integer) this.dialogInfos.get(content.getDialogID()).get(
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
					(Integer) this.dialogInfos.get(content.getDialogID()).get(
							Constants.DIALOG_ANSWER_TIMESTAMP)
							+ diff);
		}
		return true;
	}

	/**
	 * 更新提问者的消耗量
	 */
	private boolean updateContribution(String userid, String dialogid) {
		double contributerValue = getContribution(dialogid);
		DatabaseHandler.updateContribution(userid, contributerValue);
		ConfigHandler.getLogger().info(
				"update contribution userid=" + userid + ",value="
						+ contributerValue);
		return true;
	}

	/**
	 * 更新提问者的消耗量
	 */
	private boolean updateConsumation(String userid, String dialogid) {
		double consumerValue = getConsumation(userid, dialogid);
		DatabaseHandler.updateConsumation(userid, consumerValue);
		ConfigHandler.getLogger().info(
				"update consumation userid=" + userid + ",value="
						+ consumerValue);
		return true;
	}

	/**
	 * 计算提问者的消耗量 消耗量=消耗量基数*消耗量权值 消耗量权值跟对方评价有关=总得分*对方评价 总得分=2*（此问题的贡献量）-此问题的消耗值
	 * 此问题的消耗值跟提问者的平均响应时间等因素有关
	 * 
	 * @param userid
	 * @param dialogid
	 * @return
	 */
	private double getConsumation(String userid, String dialogid) {
		return getConsumationBaseIndex(userid, dialogid)
				* getConsumationWeight(userid, dialogid);
	}

	// 计算消耗量基数
	private double getConsumationBaseIndex(String userid, String dialogid) {
		return 1;
	}

	// 计算消耗量的权值
	// 跟回答者对提问者的评价有关
	private double getConsumationWeight(String userid, String dialogid) {
		double weight = 0;
		// 回答者的评价
		int answerStar = (Integer) this.dialogInfos.get(dialogid).get(
				Constants.DIALOG_ANSWER_STAR_NUM);
		if (answerStar == 1) {
			weight = 2.0 * getConsumationTotalScore(dialogid);
		} else if (answerStar == 2) {
			weight = 1.5 * getConsumationTotalScore(dialogid);
		} else if (answerStar == 3) {
			weight = 1.0 * getConsumationTotalScore(dialogid);
		}
		return weight;
	}

	/**
	 * 计算消耗值的总得分 总得分=2*（此问题的贡献量）-此问题的消耗值
	 * 
	 * @param dialogid
	 * @return
	 */
	private double getConsumationTotalScore(String dialogid) {
		return 2 * getContribution(dialogid) - getComsumationScore(dialogid);
	}

	/**
	 * 计算提问者抵扣得分 聊天中平均响应时间[不包含暂停的的时间]（30%）、 聊天过程总字数（40%）、 自己是否填写评价内容（10%）、
	 * 是否进行分享（10%）、 关键词分（10%）、 附加分（5%）。
	 * 
	 * @return
	 */
	private double getComsumationScore(String dialogid) {
		return 0.3 * this.getAverageAskResponseScore(dialogid) + 0.4
				* this.getAskTotalWordsScore(dialogid) + 0.1
				* this.getAskSelfCommentScore(dialogid) + 0.1
				* this.getAskShareScore(dialogid) + 0.1
				* getKeywordScore(dialogid) + 0.05
				* this.getAskPauseScore(dialogid);
	}

	/**
	 * 提问者聊天中平均响应时间
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getAverageAskResponseScore(String dialogid) {
		try {
			float time = ((Long) this.dialogInfos.get(dialogid).get(
					Constants.DIALOG_ASK_TIMESTAMP) / 1000)
					/ (Integer) this.dialogInfos.get(dialogid).get(
							Constants.DIALOG_ASK_WORD_LENGTH);
			if (time <= 3) {
				return 0.3f;
			} else if (time <= 6) {
				return 0.25f;
			} else if (time <= 9) {
				return 0.20f;
			} else if (time <= 12) {
				return 0.10f;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;

	}

	/**
	 * 提问者聊天过程总字数
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getAskTotalWordsScore(String dialogid) {
		try {
			int num = (Integer) this.dialogInfos.get(dialogid).get(
					Constants.DIALOG_ASK_WORD_LENGTH);
			if (num == 0) {
				return 0;
			} else if (num <= 35) {
				return 0.05f;
			} else if (num <= 70) {
				return 0.1f;
			} else if (num <= 210) {
				return 0.14f;
			} else if (num <= 420) {
				return 0.18f;
			} else {
				return 0.2f;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;

	}

	/**
	 * 提问者自己是否填写评价内容
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getAskSelfCommentScore(String dialogid) {
		try {
			boolean isComment = (Boolean) this.dialogInfos.get(dialogid).get(
					Constants.DIALOG_ASK_IS_COMMENT);
			boolean isStar = (Integer) this.dialogInfos.get(dialogid).get(
					Constants.DIALOG_ASK_STAR_NUM) > 0;
			if (isComment && isStar) {
				return 0.1f;
			} else if (isStar) {
				return 0.06f;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 提问者是否进行分享
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getAskShareScore(String dialogid) {
		boolean isShare = (Boolean) this.dialogInfos.get(dialogid).get(
				Constants.DIALOG_ASK_SHARE);
		if (isShare) {
			return 0.06f;
		}
		return 0;
	}

	/**
	 * 关键词分,未实现
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getKeywordScore(String dialogid) {
		return 0;
	}

	/**
	 * 暂停附加分
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getAskPauseScore(String dialogid) {
		try {
			int times = (Integer) this.dialogInfos.get(dialogid).get(
					Constants.DIALOG_ASK_PAUSE_TIME);
			if (times == 0) {
				return 0.05f;
			} else if (times == 1) {
				return 0.025f;
			} else {
				return 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 计算问题的贡献量 跟提问者对此问题的评价有关
	 * 
	 * @param dialogid
	 * @return
	 */
	private double getContribution(String dialogid) {
		// 提问者对此问题的评价
		int askerStar = (Integer) this.dialogInfos.get(dialogid).get(
				Constants.DIALOG_ASK_STAR_NUM);
		if (askerStar == 1) {
			return -0.5;
		} else if (askerStar == 2) {
			return getContributionScore(dialogid) * 0.6;
		} else if (askerStar == 3) {
			return getContributionScore(dialogid) * 1.0;
		}
		return 0;

	}

	/**
	 * 计算贡献的总得分 总得分由以下几个部分组成： 响应新消息速度/主动回答（20%）、 聊天中平均响应时间[不包含暂停的的时间]（30%）、
	 * 聊天过程总字数（40%）、 自己是否填写评价内容（5%）、 是否进行分享（5%）、 用户等级附加分（5%）、 暂停附加分（5%）。
	 * 
	 * @param dialogid
	 * @return
	 */
	private double getContributionScore(String dialogid) {
		return 0.2 * this.getAnswerResponseScore(dialogid) + 0.3
				* this.getAverageAnswerResponseScore(dialogid) + 0.4
				* this.getAnswerTotalWordsScore(dialogid) + 0.05
				* this.getAnswerSelfCommentScore(dialogid) + 0.05
				* this.getAnswerShareScore(dialogid) + 0.05
				* this.getLevelDistanceScore(dialogid) + 0.05
				* this.getAnswerPauseScore(dialogid);
	}

	/**
	 * 回答者的响应新消息的速度
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getAnswerResponseScore(String dialogid) {
		try {
			long time = ((Long) this.dialogInfos.get(dialogid).get(
					Constants.DIALOG_ACCEPT_CHAT_TIME) - (Long) this.dialogInfos
					.get(dialogid).get(Constants.DIALOG_PUSH_TIME)) / 1000;
			if (time == 0) {
				return 0.2f;
			} else if (time <= 20) {
				return 0.2f;
			} else if (time <= 60) {
				return 0.18f;
			} else if (time <= 180) {
				return 0.15f;
			} else if (time <= 600) {
				return 0.10f;
			} else if (time <= 1200) {
				return 0.05f;
			} else if (time <= 1800) {
				return 0.01f;
			}
		} catch (Exception e) {
			// e.printStackTrace();
			return 0;
		}
		return 0;
	}

	/**
	 * 回答者的平均响应时间
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getAverageAnswerResponseScore(String dialogid) {
		try {
			float time = ((Long) this.dialogInfos.get(dialogid).get(
					Constants.DIALOG_ANSWER_TIMESTAMP) / 1000)
					/ (Integer) this.dialogInfos.get(dialogid).get(
							Constants.DIALOG_ANSWER_WORD_LENGTH);
			if (time <= 3) {
				return 0.3f;
			} else if (time <= 6) {
				return 0.25f;
			} else if (time <= 9) {
				return 0.20f;
			} else if (time <= 12) {
				return 0.10f;
			} else {
				return 0.0f;
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return 0.0f;

	}

	/**
	 * 回答者聊天过程总字数
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getAnswerTotalWordsScore(String dialogid) {
		try {
			int num = (Integer) this.dialogInfos.get(dialogid).get(
					Constants.DIALOG_ANSWER_WORD_LENGTH);
			if (num == 0) {
				return 0;
			} else if (num <= 35) {
				return 0.05f;
			} else if (num <= 70) {
				return 0.1f;
			} else if (num <= 210) {
				return 0.14f;
			} else if (num <= 420) {
				return 0.18f;
			} else {
				return 0.2f;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 回答者是否填写评价内容
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getAnswerSelfCommentScore(String dialogid) {
		try {
			boolean isComment = (Boolean) this.dialogInfos.get(dialogid).get(
					Constants.DIALOG_ANSWER_IS_COMMENT);
			boolean isStar = (Integer) this.dialogInfos.get(dialogid).get(
					Constants.DIALOG_ANSWER_STAR_NUM) > 0;
			if (isComment && isStar) {
				return 0.05f;
			} else if (isStar) {
				return 0.03f;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 回答者是否分享
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getAnswerShareScore(String dialogid) {
		try {
			boolean isShare = (Boolean) this.dialogInfos.get(dialogid).get(
					Constants.DIALOG_ANSWER_SHARE);
			if (isShare) {
				return 0.06f;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 用户等级附加分（还没实现）
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getLevelDistanceScore(String dialogid) {
		return 0;
	}

	/**
	 * 回答者暂停附加分
	 * 
	 * @param dialogid
	 * @return
	 */
	private float getAnswerPauseScore(String dialogid) {
		int times = (Integer) this.dialogInfos.get(dialogid).get(
				Constants.DIALOG_ANSWER_PAUSE_TIME);
		if (times == 0) {
			return 0.05f;
		} else if (times == 1) {
			return 0.025f;
		} else {
			return 0;
		}
	}
}
