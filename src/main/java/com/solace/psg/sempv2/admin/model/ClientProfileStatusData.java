package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class ClientProfileStatusData {

    @Expose
    private String adminProgress;
    @Expose
    private ClientProfile clientProfile;
    @Expose
    private Long creationTimestamp;
    @Expose
    private String id;
    @Expose
    private String operation;
    @Expose
    private String type;

    public String getAdminProgress() {
        return adminProgress;
    }

    public void setAdminProgress(String adminProgress) {
        this.adminProgress = adminProgress;
    }

    public ClientProfile getClientProfile() {
        return clientProfile;
    }

    public void setClientProfile(ClientProfile clientProfile) {
        this.clientProfile = clientProfile;
    }

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
