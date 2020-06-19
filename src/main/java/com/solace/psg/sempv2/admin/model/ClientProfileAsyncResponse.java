package com.solace.psg.sempv2.admin.model;


import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class ClientProfileAsyncResponse {

    @Expose
    private ClientProfileAsyncResponseData data;

    public ClientProfileAsyncResponseData getData() {
        return data;
    }

    public void setData(ClientProfileAsyncResponseData data) {
        this.data = data;
    }

}
