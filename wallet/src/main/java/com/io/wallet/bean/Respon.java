package com.io.wallet.bean;

import com.io.wallet.utils.Strings;

public class Respon<T> {
    private String status;
    private T data;

    public Respon(String status, T data) {
        this.status = status;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String toJson() {
        return Strings.serializer.toJson(this);
    }

}
