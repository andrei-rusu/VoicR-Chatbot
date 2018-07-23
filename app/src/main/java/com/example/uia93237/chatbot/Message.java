package com.example.uia93237.chatbot;

public class Message {

    private String msgText;
    private String msgUser;

    public Message(String msgText, String msgUser) {
        this.msgText = msgText;
        this.msgUser = msgUser;
    }

    public Message() {

    }

    public String getMsgText() {
        return msgText;
    }

    public void setMsgText(String msgText) {
        this.msgText = msgText;
    }

    public String getMsgUser() {
        return msgUser;
    }

    public void setMsgUser(String msgUser) {
        this.msgUser = msgUser;
    }
}
