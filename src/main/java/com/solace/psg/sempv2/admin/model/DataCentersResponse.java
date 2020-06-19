package com.solace.psg.sempv2.admin.model;

import java.util.List;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class DataCentersResponse
{

    @Expose
    private List<DataCenter> data;

    public List<DataCenter> getData() {
        return data;
    }

    public void setData(List<DataCenter> data) {
        this.data = data;
    }

}
