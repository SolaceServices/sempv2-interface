package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class VpnAuthenticationRequestData {

    @Expose
    private String authenticationBasicEnabled;
    @Expose
    private String authenticationClientCertEnabled;
    @Expose
    private String authenticationClientCertValidateDateEnabled;
    @Expose
    private Long creationTimestamp;
    @Expose
    private String id;
    @Expose
    private String type;

    public String getAuthenticationBasicEnabled() {
        return authenticationBasicEnabled;
    }

    public void setAuthenticationBasicEnabled(String authenticationBasicEnabled) {
        this.authenticationBasicEnabled = authenticationBasicEnabled;
    }

    public String getAuthenticationClientCertEnabled() {
        return authenticationClientCertEnabled;
    }

    public void setAuthenticationClientCertEnabled(String authenticationClientCertEnabled) {
        this.authenticationClientCertEnabled = authenticationClientCertEnabled;
    }

    public String getAuthenticationClientCertValidateDateEnabled() {
        return authenticationClientCertValidateDateEnabled;
    }

    public void setAuthenticationClientCertValidateDateEnabled(String authenticationClientCertValidateDateEnabled) {
        this.authenticationClientCertValidateDateEnabled = authenticationClientCertValidateDateEnabled;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
