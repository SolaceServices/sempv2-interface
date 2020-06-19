package com.solace.psg.sempv2.admin.model;

import java.util.List;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class DataCenter
{

    @Expose
    private String accessType;
    @Expose
    private String adminState;
    @Expose
    private Boolean available;
    @Expose
    private String cloudType;
    @Expose
    private String continent;
    @Expose
    private String displayName;
    @Expose
    private String id;
    @Expose
    private Boolean isPrivate;
    @Expose
    private String lat;
    @Expose
    private String lng;
    @Expose
    private String name;
    @Expose
    private String provider;
    @Expose
    private Boolean resourceGroupScoped;
    @Expose
    private String serverCertificateId;
    @Expose
    private List<String> supportedServiceClasses;
    @Expose
    private String type;
    @Expose
    private Boolean upgrading;

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getAdminState() {
        return adminState;
    }

    public void setAdminState(String adminState) {
        this.adminState = adminState;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getCloudType() {
        return cloudType;
    }

    public void setCloudType(String cloudType) {
        this.cloudType = cloudType;
    }

    public String getContinent() {
        return continent;
    }

    public void setContinent(String continent) {
        this.continent = continent;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Boolean getResourceGroupScoped() {
        return resourceGroupScoped;
    }

    public void setResourceGroupScoped(Boolean resourceGroupScoped) {
        this.resourceGroupScoped = resourceGroupScoped;
    }

    public String getServerCertificateId() {
        return serverCertificateId;
    }

    public void setServerCertificateId(String serverCertificateId) {
        this.serverCertificateId = serverCertificateId;
    }

    public List<String> getSupportedServiceClasses() {
        return supportedServiceClasses;
    }

    public void setSupportedServiceClasses(List<String> supportedServiceClasses) {
        this.supportedServiceClasses = supportedServiceClasses;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getUpgrading() {
        return upgrading;
    }

    public void setUpgrading(Boolean upgrading) {
        this.upgrading = upgrading;
    }


}
