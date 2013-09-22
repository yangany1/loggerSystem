package com.zhidaoba.loggersystem.relevancy;

public class RelevancyObject {

	private String user_id;
	private long time;
	private String plain_action;
	private String content;
	private String dialog_id;

	public RelevancyObject() {
	}

	public RelevancyObject(String user_id, long time, String plain_action,
			String content, String dialog_id) {
		this.user_id = user_id;
		this.time = time;
		this.plain_action = plain_action;
		this.content = content;
		this.dialog_id = dialog_id;
	}

	public UserAction actionType() {
		for (UserAction c : UserAction.values()) {
			if (c.getName().equals(this.plain_action)) {
				return c;
			}
		}
		return UserAction.NOT_USE_NOW;
	}

	public String getAction() {
		return this.plain_action;
	}

	public String getContent() {
		return this.content;
	}

	public String getUserId() {
		return this.user_id;
	}

	public long getTime() {
		return this.time;
	}

	public String getDialog_id() {
		return this.dialog_id;
	}

	public String toString() {
		String result = "user_id: " + this.user_id + "\n";
		result += "time:" + this.time + "\n";
		result += "action:" + this.plain_action + "\n";
		result += "content:" + this.content + "\n";
		return result;
	}
}
