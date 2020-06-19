package com.solace.psg.sempv2.admin.model;

import java.util.List;
import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class RolesResponse {

    @Expose
    private List<Role> data;

    public List<Role> getData() {
        return data;
    }

    public void setData(List<Role> data) {
        this.data = data;
    }

}
