package com.zhidaoba.loggersystem.relevancy;

public enum UserAction {
    LOGIN("/users/login", 1),
    LOGOUT("/users/logout", 2), 
    CREATE_QUESTION("/questions/create_question", 3),
    COMMENT("/dialogs/comment", 4), 
    AGREE("/questions/agree", 5), 
    DETAIL("/dialogs/detail", 6),
    ADD_SCHOOL_INFO("/users/add_college_info", 7),
    UPDATE_COMPANY_INFO("/users/update_company_info", 8), 
    ACCEPT_CHAT("/notification/accept_chat", 9),
    SEND_MESSAGE("/dialogs/send_message", 10), 
    REMOVE_SCHOOL_INFO("/users/remove_college_info", 11),
    EVALUATE("/dialogs/evaluate", 12), 
    NOTIFICATION_LIST("/notification/list", 13),
    CONTINUE("/dialogs/continue", 14),
    PERSONAL_INFO_ADD_COLLEGE("personal_info_add_college", 15),
    PERSONAL_INFO_ADD_MAJOR("personal_info_add_major", 16),
    PERSONAL_INFO_ADD_COMPANY("personal_info_add_company", 17),
    PERSONAL_INFO_ADD_TITLE("personal_info_add_title", 18),
    ADD_TAG("/v1.1/users/add_tags", 19),
    REGISTER_PROFILE("/users/register_profile",20),
    CLICK("click", 21), NOT_USE_NOW("null", 22);

    private String name;
    private int index;

    private UserAction(String name, int index) {
        this.name = name;
        this.index = index;
    }

    public static String getName(int index) {
        for (UserAction c : UserAction.values()) {
            if (c.getIndex() == index) {
                return c.name;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
