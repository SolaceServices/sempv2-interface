package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class VpnAuthenticationStatusResponse {

    @Expose
    private VpnAuthenticationUpdateData data;
    @Expose
    private Meta meta;

    public VpnAuthenticationUpdateData getData() {
        return data;
    }

    public void setData(VpnAuthenticationUpdateData data) {
        this.data = data;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

}
