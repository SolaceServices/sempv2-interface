package com.solace.psg.sempv2.admin.model;


import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class VpnAuthenticationUpdateResponse {

    @Expose
    private VpnAuthenticationRequestData data;

    public VpnAuthenticationRequestData getData() {
        return data;
    }

    public void setData(VpnAuthenticationRequestData data) {
        this.data = data;
    }

}
