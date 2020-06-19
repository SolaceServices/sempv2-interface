package com.solace.psg.sempv2.admin.model;

import java.util.List;
import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class Cluster {

    @Expose
    private String backupRouterName;
    @Expose
    private String monitoringRouterName;
    @Expose
    private String name;
    @Expose
    private String password;
    @Expose
    private String primaryRouterName;
    @Expose
    private String remoteAddress;
    @Expose
    private List<String> supportedAuthenticationMode;

    public String getBackupRouterName() {
        return backupRouterName;
    }

    public void setBackupRouterName(String backupRouterName) {
        this.backupRouterName = backupRouterName;
    }

    public String getMonitoringRouterName() {
        return monitoringRouterName;
    }

    public void setMonitoringRouterName(String monitoringRouterName) {
        this.monitoringRouterName = monitoringRouterName;
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

    public String getPrimaryRouterName() {
        return primaryRouterName;
    }

    public void setPrimaryRouterName(String primaryRouterName) {
        this.primaryRouterName = primaryRouterName;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public List<String> getSupportedAuthenticationMode() {
        return supportedAuthenticationMode;
    }

    public void setSupportedAuthenticationMode(List<String> supportedAuthenticationMode) {
        this.supportedAuthenticationMode = supportedAuthenticationMode;
    }

}
