package com.zhidaoba.loggersystem.relevancy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

import com.google.gson.Gson;
import com.zhidaoba.loggersystem.common.ConfigHandler;
import com.zhidaoba.loggersystem.common.Constants;

public class RelevancyThread {
	// changelist是分析日志数据得到的将要写到日志数据库的对象列表
	private ArrayBlockingQueue<RelevancyObject> changeList;
	// 存储用户和标签的相关度
	private Map<String, Map<String, Float>> expertTagRelevancy;
	private Gson gson = new Gson();
	// TODO how to update everyday
	private Map<String, Map<String, Integer>> userAskedTimesToday = new HashMap<String, Map<String, Integer>>();
	private Map<String, Map<String, Integer>> userAnsweredTimesToday = new HashMap<String, Map<String, Integer>>();

	private Calendar today = Calendar.getInstance();

	// String(dialog_id):ArrayList(0:ASKER, 1:ANSWERER, 2: PUSH_TIME,
	// 3:ACCEPT_CHAT_TIME, 4:ASK_WORD_LENGTH,
	// 5:ANSWER_WORD_LENGTH, 6:ASK_Timestamp, 7:ANSWER_Timestamp), 8:
	// LAST_UPDATE_TIME, 9:FIRST_ANSWER_TIME
	// 10:ASK_PAUSE_TIME, 11:ANSWER_PAUSE_TIME, 12:ASK_STAR_NUM,
	// 13:ANSWER_STAR_NUM, 14:ASK_IS_COMMENT,15:ANSWER_IS_COMMENT,
	// 16:ASK_SHARE, 17:ANSWER_SHARE
	private Map<String, ArrayList<Object>> dialogInfos = new HashMap<String, ArrayList<Object>>();

	public void init() {
		Map<String, Map<String, Float>> expertTagRelevancy = new HashMap<String, Map<String, Float>>();
		DatabaseHandler.getRelevancyFromMysql(expertTagRelevancy);
		ConfigHandler.getLogger().info("expertTag size=" + expertTagRelevancy);
	}

	public void run() {
		this.init();
		this.update();
	}

	/**
	 * 定时更新线程
	 * 1、定时读取日志文件并写入数据库
	 * 2、分析用户的行为。计算关联度
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

	private void analysisFromMongoDB() {
		MongoClient client = null;
		DBCursor cursor = null;
		try {
			System.out.println("step1");
			client = new MongoClient(ConfigHandler.getMongodbServer(),
					ConfigHandler.getMongodbPort());
			DB db = client.getDB(ConfigHandler.getMongodbName());
			// boolean auth = db.authenticate(myUserName, myPassword);
			DBCollection coll = db
					.getCollection(Constants.MONGODB_USER_LOGS_COLLECTION);
			// DBObject myDoc = coll.findOne();
			DBObject query = new BasicDBObject();
			query.put(
					Constants.ISHANDLED_FIELD_IN_MONGODB_USER_LOGS_COLLECTION,
					false);

			cursor = coll.find(query);
			while (cursor.hasNext()) {

				DBObject obj = cursor.next();
				System.out.println("obj=" + obj);
				// if (!(Boolean)
				// obj.get(ISHANDLED_FIELD_IN_MONGODB_USER_LOGS_COLLECTION)) {
				LogInfo content = new LogInfo(
						(String) obj
								.get(Constants.USER_ID_FIELD_IN_MONGODB_USER_LOGS_COLLECTION),
						(Long) obj
								.get(Constants.LOGTIME_FIELD_IN_MONGODB_USER_LOGS_COLLECTION),
						(String) obj
								.get(Constants.ACTION_FIELD_IN_MONGODB_USER_LOGS_COLLECTION),
						(String) obj
								.get(Constants.CONTENT_FIELD_IN_MONGODB_USER_LOGS_COLLECTION));
				ConfigHandler.getLogger().info("logInfo object=" + content);
				analysis(content);
				ConfigHandler.getLogger().info("analysis end");
				DBObject updatedValue = new BasicDBObject();
				updatedValue
						.put(Constants.ISHANDLED_FIELD_IN_MONGODB_USER_LOGS_COLLECTION,
								true);
				DBObject updateSetValue = new BasicDBObject("$set",
						updatedValue);
				coll.update(obj, updateSetValue);
				ConfigHandler.getLogger().info("update to handled ");
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("thread " + Thread.currentThread().getName()
					+ " is releaseing ");
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
	}

	private int getRatioByProfile(String userid) {
		ConfigHandler.getLogger().info("getRatioByProfile");
		DBObject obj = DatabaseHandler
				.getItemFromMongoDB(
						Constants.MONGODB_USER_INFO_COLLECTION,
						Constants.USER_ID_FIELD_IN_MONGODB_USER_INFO_COLLECTION,
						userid);
		try {
			int level = (Integer) obj
					.get(Constants.PROFILE_FIELD_IN_MONGODB_USER_INFO_COLLECTION);
			if (level <= 15) {
				return 1;
			} else if (level <= 20) {
				return 2;
			} else if (level <= 25) {
				return 3;
			} else if (level <= 30) {
				return 4;
			} else if (level <= 35) {
				return 5;
			}
			return 1;
		} catch (Exception e) {
			ConfigHandler.getLogger().warning(
					"User " + userid + " Not Exists in user collection!");
			return 1;
		}

	}

	private void updateKnowledge(UserAction action, String uid,
			float incrementKnowledge, ContentParser content) {
		ConfigHandler.getLogger().info("update knowledge");
		float beta = 0;
		String[] tags;
		float weight = 0;
		switch (action) {
		case CREATE_QUESTION:
			DBObject obj1 = DatabaseHandler
					.getItemFromMongoDB(
							Constants.MONGODB_DIALOG_KEYWORDS_COLLECTION,
							Constants.DIALOG_ID_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION,
							content.getDialogID());
			tags = ((String) obj1
					.get(Constants.STD_KEYWORDS_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION))
					.split(Constants.TAG_SPLITER);
			weight = incrementKnowledge / tags.length;
			beta = 0.1f;
			break;
		case ACCEPT_CHAT:
			DBObject obj2 = DatabaseHandler
					.getItemFromMongoDB(
							Constants.MONGODB_DIALOG_KEYWORDS_COLLECTION,
							Constants.DIALOG_ID_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION,
							content.getDialogID());
			tags = ((String) obj2
					.get(Constants.STD_KEYWORDS_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION))
					.split(Constants.TAG_SPLITER);
			weight = incrementKnowledge / tags.length;
			beta = 0.08f;
			break;
		case UPDATE_TAG:
			tags = content.getTags().split(Constants.TAG_SPLITER);
			weight = incrementKnowledge;
			beta = 0.2f;
			break;
		case PERSONAL_INFO_ADD_COLLEGE:
			beta = 0.1f;
			tags = DatabaseHandler.getPersonalInfoTagsFromMysql(action,
					content.getCollegeName());
			weight = incrementKnowledge / tags.length;
			break;
		case PERSONAL_INFO_ADD_MAJOR:
			beta = 0.1f;
			tags = DatabaseHandler.getPersonalInfoTagsFromMysql(action,
					content.getMajorName());
			weight = incrementKnowledge / tags.length;
			break;
		case PERSONAL_INFO_ADD_COMPANY:
			beta = 0.1f;
			tags = DatabaseHandler.getPersonalInfoTagsFromMysql(action,
					content.getCompanyName());
			weight = incrementKnowledge / tags.length;
			break;
		case PERSONAL_INFO_ADD_TITLE:
			beta = 0.1f;
			tags = DatabaseHandler.getPersonalInfoTagsFromMysql(action,
					content.getPositionTitle());
			weight = incrementKnowledge / tags.length;
			break;
		case AGREE:
			DBObject obj3 = DatabaseHandler
					.getItemFromMongoDB(
							Constants.MONGODB_DIALOG_KEYWORDS_COLLECTION,
							Constants.DIALOG_ID_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION,
							(String) DatabaseHandler
									.getItemFromMongoDB(
											Constants.MONGODB_DIALOG_COLLECTION,
											Constants.DIALOG_ID_FIELD_IN_MONGODB_DIALOG_COLLECTION,
											content.getQuestionID())
									.get(Constants.DIALOG_ID_FIELD_IN_MONGODB_DIALOG_COLLECTION));
			tags = ((String) obj3
					.get(Constants.STD_KEYWORDS_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION))
					.split(Constants.TAG_SPLITER);
			weight = incrementKnowledge / tags.length;
			beta = 0.03f;
			break;
		case DETAIL:
			DBObject obj4 = DatabaseHandler
					.getItemFromMongoDB(
							Constants.MONGODB_DIALOG_KEYWORDS_COLLECTION,
							Constants.DIALOG_ID_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION,
							content.getDialogID());
			tags = ((String) obj4
					.get(Constants.STD_KEYWORDS_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION))
					.split(Constants.TAG_SPLITER);
			weight = incrementKnowledge / tags.length;
			beta = 0.02f;
			break;
		case COMMENT:
			DBObject obj5 = DatabaseHandler
					.getItemFromMongoDB(
							Constants.MONGODB_DIALOG_KEYWORDS_COLLECTION,
							Constants.DIALOG_ID_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION,
							content.getDialogID());
			tags = ((String) obj5
					.get(Constants.STD_KEYWORDS_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION))
					.split(Constants.TAG_SPLITER);
			weight = incrementKnowledge / tags.length;
			beta = 0.05f;
			break;
		default:
			tags = new String[0];
			break;
		}

		ArrayList<String> toAddTags = new ArrayList<String>();
		ArrayList<Float> toAddRelevancies = new ArrayList<Float>();
		for (String tag : tags) {
			ArrayList<String> relaWords = DatabaseHandler
					.getRelationshipsFromMysql(tag);
			relaWords.add(0, tag);
			float s = weight;
			for (String word : relaWords) {
				try {
					float alpha = this.expertTagRelevancy.get(uid).get(word);
					float added = (1 - alpha) * beta;
					if (alpha < s * beta) {
						added = alpha;
					}
					float curr = alpha + added;
					this.expertTagRelevancy.get(uid).put(word, curr);
					if (toAddTags.contains(word)) {
						toAddRelevancies.set(toAddTags.indexOf(word), curr);
					} else {
						toAddRelevancies.add(curr);
						toAddTags.add(word);
					}
					s = s - added;
					if (s <= 0) {
						break;
					}
				} catch (Exception e) {
					continue;
				}
			}
		}
		ConfigHandler.getLogger().info("Update relevancy uid=" + uid);
		DatabaseHandler.updateRelevancyToMysql(uid, (String[]) toAddTags
				.toArray(), ArrayUtils.toPrimitive(
				(Float[]) toAddRelevancies.toArray(), 0.0f));

		// this.recommendList.add(new RecommendPostContent(uid,
		// (String[]) toAddTags.toArray(), ArrayUtils.toPrimitive((Float[])
		// toAddRelevancies.toArray(), 0.0f)));
	}

	

	// String(dialog_id):ArrayList(0:ASKER, 1:ANSWERER, 2: PUSH_TIME,
	// 3:ACCEPT_CHAT_TIME, 4:ASK_WORD_LENGTH,
	// 5:ANSWER_WORD_LENGTH, 6:ASK_Timestamp, 7:ANSWER_Timestamp), 8:
	// LAST_UPDATE_TIME, 9:FIRST_ANSWER_TIME
	// 10:ASK_PAUSE_TIME, 11:ANSWER_PAUSE_TIME, 12:ASK_STAR_NUM,
	// 13:ANSWER_STAR_NUM, 14:ASK_IS_COMMENT,15:ANSWER_IS_COMMENT,
	// 16:ASK_SHARE, 17:ANSWER_SHARE, 18:ASK_EVALUATE, 19:ANSWER_EVALUATE
	private void analysis(LogInfo log) throws Exception {
		try {
			String userid = log.getUserId();
			Calendar tmp = Calendar.getInstance();
			ConfigHandler.getLogger().info("log content=" + log.getContent());
			ContentParser content = gson.fromJson(log.getContent(),
					ContentParser.class);
			float ratio = getRatioByProfile(userid);
			switch (log.actionType()) {
			case EVALUATE:
				ConfigHandler.getLogger().info("evaluate");
				// ASKER
				if (log.getUserId().equals(
						this.dialogInfos.get(content.getDialogID()).get(0))) {
					this.dialogInfos.get(content.getDialogID()).set(12,
							content.getStarNumber());
					this.dialogInfos.get(content.getDialogID()).set(14,
							content.getContent().isEmpty());
					this.dialogInfos.get(content.getDialogID()).set(16,
							content.getIsShare());
					this.dialogInfos.get(content.getDialogID()).set(18, true);
				}
				// ANSWERER
				else if (log.getUserId().equals(
						this.dialogInfos.get(content.getDialogID()).get(1))) {
					this.dialogInfos.get(content.getDialogID()).set(13,
							content.getStarNumber());
					this.dialogInfos.get(content.getDialogID()).set(15,
							content.getContent().isEmpty());
					this.dialogInfos.get(content.getDialogID()).set(17,
							content.getIsShare());
					this.dialogInfos.get(content.getDialogID()).set(19, true);
				}
				if ((Boolean) this.dialogInfos.get(content.getDialogID()).get(
						18)
						&& (Boolean) this.dialogInfos
								.get(content.getDialogID()).get(19)) {
					this.updateKnowledge(
							UserAction.CREATE_QUESTION,
							(String) this.dialogInfos
									.get(content.getDialogID()).get(0),
							ratio
									* this.getContributeTotalScore(content
											.getDialogID()) * 0.5f, content);
					this.updateKnowledge(
							UserAction.ACCEPT_CHAT,
							(String) this.dialogInfos
									.get(content.getDialogID()).get(1),
							ratio
									* this.getContributeQuantityFromComment(content
											.getDialogID()) * 2, content);
					this.saveContribution(
							(String) this.dialogInfos
									.get(content.getDialogID()).get(1), content
									.getDialogID());
					this.saveContribution(
							(String) this.dialogInfos
									.get(content.getDialogID()).get(0), content
									.getDialogID());
					this.dialogInfos.remove(content.getDialogID());
				}
				break;
			case CREATE_QUESTION:
				ConfigHandler.getLogger().info("CREATE_QUESTION");
				Map<String, Integer> tmp1;
				if (!this.userAskedTimesToday.containsKey(userid)) {
					tmp1 = new HashMap<String, Integer>();
					tmp1.put(content.getDialogID(), 1);
				} else {
					tmp1 = this.userAskedTimesToday.get(userid);
					tmp1.put(content.getDialogID(), this.userAskedTimesToday
							.get(userid).size() + 1);
				}
				this.userAskedTimesToday.put(userid, tmp1);
				if (this.dialogInfos.containsKey(content.getDialogID())) {
					ConfigHandler.getLogger().warning(
							"DIALOG_ID DUPLICATED:" + content.getDialogID());
				} else {
					this.dialogInfos.put(content.getDialogID(),
							new ArrayList<Object>(12));
					// ArrayList must add and then set
					for (int i = 0; i < 12; i++) {
						this.dialogInfos.get(content.getDialogID()).add(
								new Object());
					}
					this.dialogInfos.get(content.getDialogID()).set(0,
							log.getUserId());
					this.dialogInfos.get(content.getDialogID()).set(10, 0);
					this.dialogInfos.get(content.getDialogID()).set(11, 0);
				}
				break;
			case NOTIFICATION_LIST:
				ConfigHandler.getLogger().info("NOTIFICATION_LIST");
				if (content.getType().equals(
						Constants.CHAT_INVITATION_NOTIFICATION)) {
					this.dialogInfos.get(content.getDialogID()).set(2,
							log.getTime());
					this.dialogInfos.get(content.getDialogID()).set(8,
							log.getTime());
				}
				break;
			case ACCEPT_CHAT:
				ConfigHandler.getLogger().info("ACCEPT_CHAT");
				Map<String, Integer> tmp2;
				if (!this.userAnsweredTimesToday.containsKey(userid)) {
					tmp2 = new HashMap<String, Integer>();
					tmp2.put(content.getDialogID(), 1);
				} else {
					tmp2 = this.userAnsweredTimesToday.get(userid);
					tmp2.put(content.getDialogID(), this.userAnsweredTimesToday
							.get(userid).size() + 1);
				}
				this.userAskedTimesToday.put(userid, tmp2);
				this.dialogInfos.get(content.getDialogID()).set(1,
						log.getUserId());
				this.dialogInfos
						.get(DatabaseHandler
								.getItemFromMongoDB(
										Constants.MONGODB_NOTIFICATION_BAKS_COLLECTION,
										Constants.NOTIFICATION_BAS_ID_IN_MONGODB_NOTIFICATION_BAKS_COLLECTION,
										content.getNotificationID())
								.get(Constants.DIALOG_ID_IN_MONGODB_NOTIFICATION_BAKS_COLLECTION))
						.set(3, log.getTime());
				break;
			case SEND_MESSAGE:
				ConfigHandler.getLogger().info("SEND_MESSAGE");
				if (null == this.dialogInfos.get(content.getDialogID())) {
					this.dialogInfos.put(content.getDialogID(),
							new ArrayList<Object>(12));
					// ArrayList must add and then set
					for (int i = 0; i < 12; i++) {
						this.dialogInfos.get(content.getDialogID()).add(null);
					}
					if (null == this.dialogInfos.get(content.getDialogID())
							.get(8)) {

						this.dialogInfos.get(content.getDialogID()).set(8,
								Long.valueOf(log.getTime()));
					}
				}
				ConfigHandler.getLogger().info(
						"dialoginfo size="
								+ this.dialogInfos.get(content.getDialogID())
										.size());
				long diff = (log.getTime() - (Long) this.dialogInfos.get(
						content.getDialogID()).get(8)) / 1000;
				ConfigHandler.getLogger().info("diff=" + diff);
				this.dialogInfos.get(content.getDialogID()).set(8,
						log.getTime());
				// ASKER
				if (log.getUserId().equals(
						this.dialogInfos.get(content.getDialogID()).get(0))) {
					ConfigHandler.getLogger().info("ASKER");
					this.dialogInfos.get(content.getDialogID()).set(
							4,
							(Integer) this.dialogInfos.get(
									content.getDialogID()).get(4)
									+ content.getContent().length());
					this.dialogInfos.get(content.getDialogID()).set(
							6,
							(Integer) this.dialogInfos.get(
									content.getDialogID()).get(6)
									+ diff);
				}
				// ANSWERER
				else if (log.getUserId().equals(
						this.dialogInfos.get(content.getDialogID()).get(1))) {
					ConfigHandler.getLogger().info("ANSWERER");
					if (this.dialogInfos.get(content.getDialogID()).get(9) != null) {
						this.dialogInfos.get(content.getDialogID()).set(9,
								log.getTime());
					}
					this.dialogInfos.get(content.getDialogID()).set(
							5,
							(Integer) this.dialogInfos.get(
									content.getDialogID()).get(5)
									+ content.getContent().length());
					this.dialogInfos.get(content.getDialogID()).set(
							7,
							(Integer) this.dialogInfos.get(
									content.getDialogID()).get(7)
									+ diff);
				}
				break;
			case LOGOUT:
				// ASKER
				if (log.getUserId().equals(
						this.dialogInfos.get(content.getDialogID()).get(0))) {
					this.dialogInfos.get(content.getDialogID()).set(
							10,
							(Integer) this.dialogInfos.get(
									content.getDialogID()).get(10) + 1);
				}
				// ANSWERER
				else if (log.getUserId().equals(
						this.dialogInfos.get(content.getDialogID()).get(1))) {
					this.dialogInfos.get(content.getDialogID()).set(
							11,
							(Integer) this.dialogInfos.get(
									content.getDialogID()).get(11) + 1);
				}
				break;
			case CONTINUE:
				this.dialogInfos.get(content.getDialogID()).set(8,
						log.getTime());
				break;

			case UPDATE_TAG:
				this.updateKnowledge(UserAction.UPDATE_TAG, log.getUserId(),
						10, content);
				break;
			case ADD_SCHOOL_INFO:
				this.updateKnowledge(UserAction.PERSONAL_INFO_ADD_COLLEGE,
						log.getUserId(), 20, content);// school
				this.updateKnowledge(UserAction.PERSONAL_INFO_ADD_MAJOR,
						log.getUserId(), 20, content);// major
				break;
			case UPDATE_COMPANY_INFO:
				this.updateKnowledge(UserAction.PERSONAL_INFO_ADD_COMPANY,
						log.getUserId(), 20, content);// company
				this.updateKnowledge(UserAction.PERSONAL_INFO_ADD_TITLE,
						log.getUserId(), 20, content);// position
				break;
			case AGREE:
				this.updateKnowledge(UserAction.AGREE, log.getUserId(),
						ratio * 0.5f, content);
				break;
			case DETAIL:
				this.updateKnowledge(UserAction.DETAIL, log.getUserId(),
						ratio * 0.2f, content);
				break;
			case COMMENT:
				this.updateKnowledge(UserAction.COMMENT, log.getUserId(), 0.8f,
						content);
			case REMOVE_SCHOOL_INFO:
				break;
			default:
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	// String(dialog_id):ArrayList(0:ASKER, 1:ANSWERER, 2: PUSH_TIME,
	// 3:ACCEPT_CHAT_TIME, 4:ASK_WORD_LENGTH,
	// 5:ANSWER_WORD_LENGTH, 6:ASK_Timestamp, 7:ANSWER_Timestamp, 8:
	// LAST_UPDATE_TIME, 9:FIRST_ANSWER_TIME
	// 10:ASK_PAUSE_TIME, 11:ANSWER_PAUSE_TIME, 12:ASK_STAR_NUM,
	// 13:ANSWER_STAR_NUM, 14:ASK_IS_COMMENT,15:ANSWER_IS_COMMENT,
	// 16:ASK_SHARE, 17:ANSWER_SHARE, 18:ASK_EVALUATE, 19:ANSWER_EVALUATE

	private float getAnswerContributeBase(String userid, String dialogid) {
		int times = this.userAnsweredTimesToday.get(userid).get(dialogid);
		if (times <= 3) {
			return 1;
		} else if (times <= 5) {
			return 1.05f;
		} else if (times <= 7) {
			return 1;
		} else if (times <= 10) {
			return 0.9f;
		} else if (times <= 20) {
			return 0.8f;
		} else if (times <= 50) {
			return 0.5f;
		}
		return 0;
	}

	private float getContributeQuantityFromComment(String dialogid) {
		int stars = (Integer) this.dialogInfos.get(dialogid).get(13);
		if (stars == 1) {
			return -0.5f;
		} else if (stars == 2) {
			return this.getContributeTotalScore(dialogid) * 0.6f;
		} else if (stars == 3) {
			return this.getContributeTotalScore(dialogid) * 1;
		}
		return 0;
	}

	private float getContributeTotalScore(String dialogid) {
		return this.getResponseScore(dialogid)
				+ this.getAverageResponseScore(dialogid)
				+ this.getTotalWordsScore(dialogid)
				+ this.getSelfCommentScore(dialogid)
				+ this.getShareScore(dialogid)
				+ this.getLevelDistanceScore(dialogid)
				+ this.getPauseScore(dialogid);
	}

	// float time: seconds
	private float getResponseScore(String dialogid) {
		long time = ((Long) this.dialogInfos.get(dialogid).get(3) - (Long) this.dialogInfos
				.get(dialogid).get(2)) / 1000;
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
		return 0;
	}

	private float getAverageResponseScore(String dialogid) {
		float time = (Long) this.dialogInfos.get(dialogid).get(7)
				/ (Integer) this.dialogInfos.get(dialogid).get(5) / 1000;
		if (time <= 3) {
			return 0.3f;
		} else if (time <= 6) {
			return 0.25f;
		} else if (time <= 9) {
			return 0.20f;
		} else if (time <= 12) {
			return 0.10f;
		}
		return 0;
	}

	private float getTotalWordsScore(String dialogid) {
		int num = (Integer) this.dialogInfos.get(dialogid).get(5);
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
	}

	private float getSelfCommentScore(String dialogid) {
		boolean isComment = (Boolean) this.dialogInfos.get(dialogid).get(15);
		boolean isStar = (Integer) this.dialogInfos.get(dialogid).get(13) > 0;
		if (isComment && isStar) {
			return 0.05f;
		} else if (isStar) {
			return 0.03f;
		}
		return 0;
	}

	private float getShareScore(String dialogid) {
		boolean isShare = (Boolean) this.dialogInfos.get(dialogid).get(17);
		if (isShare) {
			return 0.06f;
		}
		/*
		 * if (type == 12) { return 0.05f; } else if (type == 11) { return
		 * 0.04f; } else if (type == 10) { return 0.03f; } else if (type == 02)
		 * { return 0.02f; } else if (type == 01) { return 0.01f; } else if
		 * (type == 0) { return 0; }
		 */
		return 0;
	}

	private float getLevelDistanceScore(String dialogid) {
		/*
		 * if (diff == 0) { return 0; } else if (diff <= 5) { return 0.005f; }
		 * else if (diff <= 10) { return 0.015f; } else if (diff <= 15) { return
		 * 0.025f; } else if (diff <= 20) { return 0.035f; } else if (diff <=
		 * 25) { return 0.04f; } else if (diff <= 30) { return 0.045f; } else if
		 * (diff <= 35) { return 0.05f; }
		 */
		return 0;
	}

	private float getPauseScore(String dialogid) {
		int times = (Integer) this.dialogInfos.get(dialogid).get(11);
		if (times == 0) {
			return 0.05f;
		} else if (times == 1) {
			return 0.025f;
		} else {
			return 0;
		}
	}

	private float getAskConsumeBase(String userid, String dialogid) {
		int times = this.userAskedTimesToday.get(userid).get(dialogid);
		if (times <= 3) {
			return 1;
		} else if (times <= 5) {
			return 1.1f;
		} else if (times <= 7) {
			return 1.2f;
		} else if (times <= 10) {
			return 1.25f;
		} else if (times <= 15) {
			return 1.5f;
		} else {
			return 2.0f;
		}
	}

	private float getConsumeQuantityFromComment(String dialogid) {
		int stars = (Integer) this.dialogInfos.get(dialogid).get(12);
		if (stars == 1) {
			return this.getConsumeTotalScore(dialogid) * 2.0f;
		} else if (stars == 2) {
			return this.getConsumeTotalScore(dialogid) * 1.5f;
		} else if (stars == 3) {
			return this.getConsumeTotalScore(dialogid) * 1;
		}
		return 0;
	}

	private float getConsumeTotalScore(String dialogid) {
		return 2
				* this.getContributeTotalScore(dialogid)
				- (this.getAverageAskResponseScore(dialogid)
						+ this.getAskTotalWordsScore(dialogid)
						+ this.getConsumeSelfCommentScore(dialogid)
						+ this.getConsumeShareScore(dialogid) + this
							.getPauseScore(dialogid))
				+ this.getConsumeKeywordScore(dialogid);
	}

	private float getAverageAskResponseScore(String dialogid) {
		float time = (Long) this.dialogInfos.get(dialogid).get(6)
				/ (Integer) this.dialogInfos.get(dialogid).get(4) / 1000;
		if (time <= 3) {
			return 0.3f;
		} else if (time <= 6) {
			return 0.25f;
		} else if (time <= 9) {
			return 0.20f;
		} else if (time <= 12) {
			return 0.10f;
		}
		return 0;
	}

	private float getAskTotalWordsScore(String dialogid) {
		int num = (Integer) this.dialogInfos.get(dialogid).get(4);
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
	}

	private float getConsumeSelfCommentScore(String dialogid) {
		boolean isComment = (Boolean) this.dialogInfos.get(dialogid).get(14);
		boolean isStar = (Integer) this.dialogInfos.get(dialogid).get(12) > 0;
		if (isComment && isStar) {
			return 0.1f;
		} else if (isStar) {
			return 0.06f;
		}
		return 0;
	}

	private float getConsumeShareScore(String dialogid) {
		boolean isShare = (Boolean) this.dialogInfos.get(dialogid).get(16);
		if (isShare) {
			return 0.06f;
		}
		return 0;
	}

	private float getConsumeKeywordScore(String dialogid) {
		DBObject obj = DatabaseHandler
				.getItemFromMongoDB(
						Constants.MONGODB_DIALOG_KEYWORDS_COLLECTION,
						Constants.DIALOG_ID_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION,
						dialogid);
		String[] origin_keywords = ((String) obj
				.get(Constants.ORIGIN_KEYWORDS_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION))
				.split(Constants.TAG_SPLITER);
		String[] std_keywords = ((String) obj
				.get(Constants.STD_KEYWORDS_FIELD_IN_MONGODB_DIALOG_KEYWORDS_COLLECTION))
				.split(Constants.TAG_SPLITER);
		int times = 0;
		for (String keyword : std_keywords) {
			if (Arrays.binarySearch(origin_keywords, keyword) < 0) {
				times++;
			}
		}
		if (times == 0) {
			return 0.1f;
		} else if (times == 1) {
			return 0.8f;
		} else if (times == 2) {
			return 0.5f;
		} else if (times < origin_keywords.length) {
			return 0.2f;
		}
		return 0;
	}

	private void saveContribution(String userid, String dialogid) {
		MongoClient client = null;
		DBCursor cursor = null;
		try {
			client = new MongoClient(ConfigHandler.getMongodbServer(),
					ConfigHandler.getMongodbPort());
			DB db = client.getDB(ConfigHandler.getMongodbName());
			// boolean auth = db.authenticate(myUserName, myPassword);
			DBCollection coll = db
					.getCollection(Constants.MONGODB_CONTRIBUTION_COLLECTION);
			BasicDBObject query = new BasicDBObject(
					Constants.USER_ID_FIELD_IN_MONGODB_CONTRIBUTION_COLLECTION,
					userid);
			cursor = coll.find(query);
			if (cursor.hasNext()) {
				DBObject obj = cursor.next();
				DBObject updatedValue = new BasicDBObject();
				updatedValue
						.put(Constants.USER_ID_FIELD_IN_MONGODB_CONTRIBUTION_COLLECTION,
								(Float) obj
										.get(Constants.VALUE_FIELD_IN_MONGODB_CONTRIBUTION_COLLECTION)
										+ this.getAnswerContributeBase(userid,
												dialogid)
										* this.getContributeQuantityFromComment(dialogid));
				DBObject updateSetValue = new BasicDBObject("$set",
						updatedValue);
				coll.update(obj, updateSetValue);
			} else {
				BasicDBObject doc = new BasicDBObject(
						Constants.USER_ID_FIELD_IN_MONGODB_CONTRIBUTION_COLLECTION,
						userid)
						.append(Constants.VALUE_FIELD_IN_MONGODB_CONTRIBUTION_COLLECTION,
								this.getAnswerContributeBase(userid, dialogid)
										* this.getContributeQuantityFromComment(dialogid));
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
	}

	private void saveConsumation(String userid, String dialogid) {
		MongoClient client = null;
		DBCursor cursor = null;
		try {
			client = new MongoClient(ConfigHandler.getMongodbServer(),
					ConfigHandler.getMongodbPort());
			DB db = client.getDB(ConfigHandler.getMongodbName());
			// boolean auth = db.authenticate(myUserName, myPassword);
			DBCollection coll = db
					.getCollection(Constants.MONGODB_CONSUMATION_COLLECTION);
			BasicDBObject query = new BasicDBObject(
					Constants.USER_ID_FIELD_IN_MONGODB_CONSUMATION_COLLECTION,
					userid);
			cursor = coll.find(query);
			if (cursor.hasNext()) {
				DBObject obj = cursor.next();
				DBObject updatedValue = new BasicDBObject();
				updatedValue
						.put(Constants.USER_ID_FIELD_IN_MONGODB_CONSUMATION_COLLECTION,
								(Float) obj
										.get(Constants.VALUE_FIELD_IN_MONGODB_CONSUMATION_COLLECTION)
										+ this.getAskConsumeBase(userid,
												dialogid)
										* this.getConsumeQuantityFromComment(dialogid));
				DBObject updateSetValue = new BasicDBObject("$set",
						updatedValue);
				coll.update(obj, updateSetValue);
			} else {
				BasicDBObject doc = new BasicDBObject(
						Constants.USER_ID_FIELD_IN_MONGODB_CONSUMATION_COLLECTION,
						userid)
						.append(Constants.VALUE_FIELD_IN_MONGODB_CONSUMATION_COLLECTION,
								this.getAskConsumeBase(userid, dialogid)
										* this.getConsumeQuantityFromComment(dialogid));
				coll.insert(doc);
			}
			cursor.close();
			client.close();
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
	}
}
