package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class VpnAuthenticationUpdateRequest {

    @Expose
    private String authenticationBasicEnabled;
    @Expose
    private String authenticationClientCertEnabled;
    @Expose
    private String authenticationClientCertValidateDateEnabled;

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

}
