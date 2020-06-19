package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class UserResponse
{
    @Expose
    private User data;

    public User getData() {
        return data;
    }

    public void setData(User data) {
        this.data = data;
    }

}
