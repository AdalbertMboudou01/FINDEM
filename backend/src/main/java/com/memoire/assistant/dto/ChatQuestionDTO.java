package com.memoire.assistant.dto;

public class ChatQuestionDTO {
    private String key;
    private String text;
    private String answerType;
    private int order;
    private boolean required;
    private String category;
    
    // Constructors
    public ChatQuestionDTO() {}
    
    public ChatQuestionDTO(String key, String text, String answerType, int order, boolean required, String category) {
        this.key = key;
        this.text = text;
        this.answerType = answerType;
        this.order = order;
        this.required = required;
        this.category = category;
    }
    
    // Getters & Setters
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getAnswerType() {
        return answerType;
    }
    
    public void setAnswerType(String answerType) {
        this.answerType = answerType;
    }
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public boolean isRequired() {
        return required;
    }
    
    public void setRequired(boolean required) {
        this.required = required;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
}
