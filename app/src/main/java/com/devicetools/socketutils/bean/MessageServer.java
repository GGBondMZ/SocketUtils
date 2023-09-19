package com.devicetools.socketutils.bean;

/**
 * Created by mz on 2023/09/19.
 * Time: 09:10
 * Description: Server
 */

public class MessageServer {

    private String msg;

    public MessageServer(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
