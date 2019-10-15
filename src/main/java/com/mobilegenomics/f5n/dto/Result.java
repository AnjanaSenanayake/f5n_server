package com.mobilegenomics.f5n.dto;

public class Result {
    private Object object;

    public Result(Object object){
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}
