package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

public class ServiceCreateResponse
{

    @Expose
    private Service data;

    public Service getData() {
        return data;
    }

    public void setData(Service data) {
        this.data = data;
    }

}
