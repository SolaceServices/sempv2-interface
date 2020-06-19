package com.solace.psg.sempv2.admin.model;


import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class ServiceDetailsResponse {

    @Expose
    private ServiceDetails data;
    @Expose
    private Meta meta;

    public ServiceDetails getData() {
        return data;
    }

    public void setData(ServiceDetails data) {
        this.data = data;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

}
