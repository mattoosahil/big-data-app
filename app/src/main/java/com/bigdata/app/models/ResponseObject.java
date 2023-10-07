package com.bigdata.app.models;

import lombok.Data;



@Data
public class ResponseObject {

    String msg;
    int stsCode;
    Object data;

    public ResponseObject(String msg,  int stsCode) {
        this.msg = msg;
        this.stsCode = stsCode;
    }

    public ResponseObject(String msg,  int stsCode, Object data) {
        this.msg = msg;
        this.stsCode = stsCode;
        this.data = data;
    }

    @Override
    public String toString() {
        return "ResponseObject{" +
                " message :'" + msg + '\'' +
                ", status_code :" + stsCode +
                ", Data :" + data +
                '}';
    }
}
