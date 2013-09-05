package com.zhidaoba.loggersystem.relevancy;

import com.google.gson.annotations.SerializedName;

public class ContentParser {
	@SerializedName("dialog_id")
    private String dialog_id;

    @SerializedName("question_id")
    private String question_id;

    @SerializedName("type")
    private String type;

    @SerializedName("notification_id")
    private String notification_id;

    @SerializedName("content")
    private String content;

    @SerializedName("star_number")
    private int star_number;

    @SerializedName("is_share")
    private boolean is_share;

    @SerializedName("tags")
    private String tags;

    @SerializedName("college_name")
    private String college_name;

    @SerializedName("major_name")
    private String major_name;

    @SerializedName("company_name")
    private String company_name;

    @SerializedName("position_title")
    private String position_title;

    @SerializedName("time")
    private long time;

    @SerializedName("action_id")
    private int action_id;

    public ContentParser() {
    }

    public String getDialogID() {
        return this.dialog_id;
    }

    public String getQuestionID() {
        return this.question_id;
    }

    public String getType() {
        return this.type;
    }

    public String getNotificationID() {
        return this.notification_id;
    }

    public String getContent() {
        return this.content;
    }

    public int getStarNumber() {
        return this.star_number;
    }

    public boolean getIsShare() {
        return this.is_share;
    }

    public String getTags() {
        return this.tags;
    }

    public String getCollegeName() {
        return this.college_name;
    }

    public String getMajorName() {
        return this.major_name;
    }

    public String getCompanyName() {
        return this.company_name;
    }

    public String getPositionTitle() {
        return this.position_title;
    }

    public long getTime() {
        return this.time;
    }

    public int getActionID() {
        return this.action_id;
    }

    public void setDialog_id(String dialog_id) {
        this.dialog_id = dialog_id;
    }

    public void setQuestion_id(String question_id) {
        this.question_id = question_id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setNotification_id(String notification_id) {
        this.notification_id = notification_id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setStar_number(int star_number) {
        this.star_number = star_number;
    }

    public void setIs_share(boolean is_share) {
        this.is_share = is_share;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setCollege_name(String college_name) {
        this.college_name = college_name;
    }

    public void setMajor_name(String major_name) {
        this.major_name = major_name;
    }

    public void setCompany_name(String company_name) {
        this.company_name = company_name;
    }

    public void setPosition_title(String position_title) {
        this.position_title = position_title;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setAction_id(int action_id) {
        this.action_id = action_id;
    }
}
