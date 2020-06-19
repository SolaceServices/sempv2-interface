package com.solace.psg.sempv2.admin.model;

import java.util.List;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class ServicesResponse {

    @Expose
    private List<Service> data;
    @Expose
    private Meta meta;

    public List<Service> getData() {
        return data;
    }

    public void setData(List<Service> data) {
        this.data = data;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

}
