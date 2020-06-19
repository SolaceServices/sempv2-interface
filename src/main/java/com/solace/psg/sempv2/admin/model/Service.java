package com.solace.psg.sempv2.admin.model;

import java.util.List;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class Service {

    @Expose
    protected List<AccountingLimit> accountingLimits;
    @Expose
    protected String adminProgress;
    @Expose
    protected String adminState;
    @Expose
    protected Attributes attributes;
    @Expose
    protected List<String> certificateAuthorities;
    @Expose
    protected List<String> clientProfiles;
    @Expose
    protected Long created;
    @Expose
    protected String creationState;
    @Expose
    protected String datacenterId;
    @Expose
    protected Boolean locked;
    @Expose
    protected Long messagingStorage;
    @Expose
    protected MsgVpnAttributes msgVpnAttributes;
    @Expose
    protected String name;
    @Expose
    protected ServiceClassDisplayedAttributes serviceClassDisplayedAttributes;
    @Expose
    protected String serviceClassId;
    @Expose
    protected String serviceId;
    @Expose
    protected String servicePackageId;
    @Expose
    protected String serviceStage;
    @Expose
    protected String serviceTypeId;
    @Expose
    protected Long timestamp;
    @Expose
    protected String type;
    @Expose
    protected String userId;

    public List<AccountingLimit> getAccountingLimits() {
        return accountingLimits;
    }

    public void setAccountingLimits(List<AccountingLimit> accountingLimits) {
        this.accountingLimits = accountingLimits;
    }

    public String getAdminProgress() {
        return adminProgress;
    }

    public void setAdminProgress(String adminProgress) {
        this.adminProgress = adminProgress;
    }

    public String getAdminState() {
        return adminState;
    }

    public void setAdminState(String adminState) {
        this.adminState = adminState;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public List<String> getCertificateAuthorities() {
        return certificateAuthorities;
    }

    public void setCertificateAuthorities(List<String> certificateAuthorities) {
        this.certificateAuthorities = certificateAuthorities;
    }

    public List<String> getClientProfiles() {
        return clientProfiles;
    }

    public void setClientProfiles(List<String> clientProfiles) {
        this.clientProfiles = clientProfiles;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getCreationState() {
        return creationState;
    }

    public void setCreationState(String creationState) {
        this.creationState = creationState;
    }

    public String getDatacenterId() {
        return datacenterId;
    }

    public void setDatacenterId(String datacenterId) {
        this.datacenterId = datacenterId;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Long getMessagingStorage() {
        return messagingStorage;
    }

    public void setMessagingStorage(Long messagingStorage) {
        this.messagingStorage = messagingStorage;
    }

    public MsgVpnAttributes getMsgVpnAttributes() {
        return msgVpnAttributes;
    }

    public void setMsgVpnAttributes(MsgVpnAttributes msgVpnAttributes) {
        this.msgVpnAttributes = msgVpnAttributes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceClassDisplayedAttributes getServiceClassDisplayedAttributes() {
        return serviceClassDisplayedAttributes;
    }

    public void setServiceClassDisplayedAttributes(ServiceClassDisplayedAttributes serviceClassDisplayedAttributes) {
        this.serviceClassDisplayedAttributes = serviceClassDisplayedAttributes;
    }

    public String getServiceClassId() {
        return serviceClassId;
    }

    public void setServiceClassId(String serviceClassId) {
        this.serviceClassId = serviceClassId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServicePackageId() {
        return servicePackageId;
    }

    public void setServicePackageId(String servicePackageId) {
        this.servicePackageId = servicePackageId;
    }

    public String getServiceStage() {
        return serviceStage;
    }

    public void setServiceStage(String serviceStage) {
        this.serviceStage = serviceStage;
    }

    public String getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(String serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
