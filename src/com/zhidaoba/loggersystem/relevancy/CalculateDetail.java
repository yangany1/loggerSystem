package com.zhidaoba.loggersystem.relevancy;

import java.util.ArrayList;
import java.util.Map;

import com.zhidaoba.loggersystem.common.Constants;

public class CalculateDetail {
	private Map<String, ArrayList<Object>> dialogInfos;

	public CalculateDetail(Map<String, ArrayList<Object>> dialogInfos) {
		this.dialogInfos = dialogInfos;
	}

	/**
	 * 计算提问者的消耗量 消耗量=消耗量基数*消耗量权值 消耗量权值跟对方评价有关=总得分*对方评价 总得分=2*（此问题的贡献量）-此问题的消耗值
	 * 此问题的消耗值跟提问者的平均响应时间等因素有关
	 * 
	 * @param userid
	 * @param dialogid
	 * @return
	 */
	public double getConsumation(String dialogid) {
		return getConsumationBaseIndex(dialogid)
				* getConsumationWeight(dialogid);
	}

	// 计算消耗量基数
	public double getConsumationBaseIndex(String dialogid) {
		return 1;
	}

	// 计算消耗量的权值
	// 跟回答者对提问者的评价有关
	public double getConsumationWeight(String dialogid) {
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
	public double getConsumationTotalScore(String dialogid) {
		return 2 * getContribution(dialogid) - getComsumationScore(dialogid);
	}

	/**
	 * 计算提问者抵扣得分 聊天中平均响应时间[不包含暂停的的时间]（30%）、 聊天过程总字数（40%）、 自己是否填写评价内容（10%）、
	 * 是否进行分享（10%）、 关键词分（10%）、 附加分（5%）。
	 * 
	 * @return
	 */
	public double getComsumationScore(String dialogid) {
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
	public float getAverageAskResponseScore(String dialogid) {
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
	public float getAskTotalWordsScore(String dialogid) {
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
	public float getAskSelfCommentScore(String dialogid) {
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
	public float getAskShareScore(String dialogid) {
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
	public float getKeywordScore(String dialogid) {
		return 0;
	}

	/**
	 * 暂停附加分
	 * 
	 * @param dialogid
	 * @return
	 */
	public float getAskPauseScore(String dialogid) {
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
	public double getContribution(String dialogid) {
		// 提问者对此问题的评价
		int askerStar = (Integer) this.dialogInfos.get(dialogid).get(
				Constants.DIALOG_ASK_STAR_NUM);
		if (askerStar == 1) {
			return -0.5;
		} else if (askerStar == 2) {
			return getContributionBase(dialogid)
					* getContributionScore(dialogid) * 0.6;
		} else if (askerStar == 3) {
			return getContributionBase(dialogid)
					* getContributionScore(dialogid) * 1.0;
		}
		return 0;

	}

	/**
	 * 贡献量基数
	 * 
	 * @param dialog
	 * @return
	 */
	public double getContributionBase(String dialog) {
		return 1.0;
	}

	/**
	 * 贡献量权值
	 * @param dialogid
	 * @return
	 */
	public double getContributionWeight(String dialogid) {
		// 提问者对此问题的评价
		int askerStar = (Integer) this.dialogInfos.get(dialogid).get(
				Constants.DIALOG_ASK_STAR_NUM);
		if (askerStar == 1) {
			return 0;
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
	public double getContributionScore(String dialogid) {
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
	public float getAnswerResponseScore(String dialogid) {
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
	public float getAverageAnswerResponseScore(String dialogid) {
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
	public float getAnswerTotalWordsScore(String dialogid) {
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
	public float getAnswerSelfCommentScore(String dialogid) {
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
	public float getAnswerShareScore(String dialogid) {
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
	public float getLevelDistanceScore(String dialogid) {
		return 0;
	}

	/**
	 * 回答者暂停附加分
	 * 
	 * @param dialogid
	 * @return
	 */
	public float getAnswerPauseScore(String dialogid) {
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
