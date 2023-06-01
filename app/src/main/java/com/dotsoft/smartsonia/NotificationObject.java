package com.dotsoft.smartsonia;


/**
 * NOTIFICATION OBJECT CLASS
 */
public class NotificationObject {

    private String title,body,id;

    NotificationObject(String title, String body,String id){
        this.title = title;
        this.body = body;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getId(){ return  id;}
}