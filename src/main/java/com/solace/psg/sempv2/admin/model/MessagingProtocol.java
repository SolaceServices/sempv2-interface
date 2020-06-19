package com.solace.psg.sempv2.admin.model;

import java.util.List;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class MessagingProtocol {

    @Expose
    private List<EndPoint> endPoints;
    @Expose
    private Limits limits;
    @Expose
    private String name;
    @Expose
    private String password;
    @Expose
    private String username;

    public List<EndPoint> getEndPoints() {
        return endPoints;
    }

    public void setEndPoints(List<EndPoint> endPoints) {
        this.endPoints = endPoints;
    }

    public Limits getLimits() {
        return limits;
    }

    public void setLimits(Limits limits) {
        this.limits = limits;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
