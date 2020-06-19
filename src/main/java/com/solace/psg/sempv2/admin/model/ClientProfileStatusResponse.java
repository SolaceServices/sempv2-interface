package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class ClientProfileStatusResponse {

    @Expose
    private ClientProfileStatusData data;
    @Expose
    private Meta meta;

    public ClientProfileStatusData getData() {
        return data;
    }

    public void setData(ClientProfileStatusData data) {
        this.data = data;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

}
