package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class RoleResponse {

    @Expose
    private Role data;

    public Role getData() {
        return data;
    }

    public void setData(Role data) {
        this.data = data;
    }

}
