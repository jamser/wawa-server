package com.wawa.api.play.dto;

/**
 * Created by Administrator on 2017/11/10.
 */
public class QiygAssignDTO {

    private String device_id;
    private String log_id;
    private Integer time;
    private String ws_url;

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getLog_id() {
        return log_id;
    }

    public void setLog_id(String log_id) {
        this.log_id = log_id;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public String getWs_url() {
        return ws_url;
    }

    public void setWs_url(String ws_url) {
        this.ws_url = ws_url;
    }
}
