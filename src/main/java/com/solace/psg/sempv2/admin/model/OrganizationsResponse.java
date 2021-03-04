package com.solace.psg.sempv2.admin.model;

import java.util.List;

import com.google.gson.annotations.Expose;

public class OrganizationsResponse
{
    @Expose
    private List<Organization> data;

    public List<Organization> getData() {
        return data;
    }

    public void setData(List<Organization> data) {
        this.data = data;
    }
}
