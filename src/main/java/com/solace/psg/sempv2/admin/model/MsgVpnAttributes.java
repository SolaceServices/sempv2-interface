package com.solace.psg.sempv2.admin.model;


import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class MsgVpnAttributes {

    @Expose
    private String authenticationBasicEnabled;
    @Expose
    private String authenticationClientCertEnabled;
    @Expose
    private String authenticationClientCertValidateDateEnabled;
    @Expose
    private String subDomainName;
    @Expose
    private String truststoreUri;
    @Expose
    private String vmrVersion;
    @Expose
    private String vpnAdminPassword;
    @Expose
    private String vpnAdminUsername;
    @Expose
    private String vpnEnabled;
    @Expose
    private String vpnEventLargeMsgThreshold;
    @Expose
    private String vpnMaxConnectionCount;
    @Expose
    private String vpnMaxEgressFlowCount;
    @Expose
    private String vpnMaxEndpointCount;
    @Expose
    private String vpnMaxIngressFlowCount;
    @Expose
    private String vpnMaxMsgSpoolUsage;
    @Expose
    private String vpnMaxSubscriptionCount;
    @Expose
    private String vpnMaxTransactedSessionCount;
    @Expose
    private String vpnMaxTransactionCount;
    @Expose
    private String vpnName;

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

    public String getSubDomainName() {
        return subDomainName;
    }

    public void setSubDomainName(String subDomainName) {
        this.subDomainName = subDomainName;
    }

    public String getTruststoreUri() {
        return truststoreUri;
    }

    public void setTruststoreUri(String truststoreUri) {
        this.truststoreUri = truststoreUri;
    }

    public String getVmrVersion() {
        return vmrVersion;
    }

    public void setVmrVersion(String vmrVersion) {
        this.vmrVersion = vmrVersion;
    }

    public String getVpnAdminPassword() {
        return vpnAdminPassword;
    }

    public void setVpnAdminPassword(String vpnAdminPassword) {
        this.vpnAdminPassword = vpnAdminPassword;
    }

    public String getVpnAdminUsername() {
        return vpnAdminUsername;
    }

    public void setVpnAdminUsername(String vpnAdminUsername) {
        this.vpnAdminUsername = vpnAdminUsername;
    }

    public String getVpnEnabled() {
        return vpnEnabled;
    }

    public void setVpnEnabled(String vpnEnabled) {
        this.vpnEnabled = vpnEnabled;
    }

    public String getVpnEventLargeMsgThreshold() {
        return vpnEventLargeMsgThreshold;
    }

    public void setVpnEventLargeMsgThreshold(String vpnEventLargeMsgThreshold) {
        this.vpnEventLargeMsgThreshold = vpnEventLargeMsgThreshold;
    }

    public String getVpnMaxConnectionCount() {
        return vpnMaxConnectionCount;
    }

    public void setVpnMaxConnectionCount(String vpnMaxConnectionCount) {
        this.vpnMaxConnectionCount = vpnMaxConnectionCount;
    }

    public String getVpnMaxEgressFlowCount() {
        return vpnMaxEgressFlowCount;
    }

    public void setVpnMaxEgressFlowCount(String vpnMaxEgressFlowCount) {
        this.vpnMaxEgressFlowCount = vpnMaxEgressFlowCount;
    }

    public String getVpnMaxEndpointCount() {
        return vpnMaxEndpointCount;
    }

    public void setVpnMaxEndpointCount(String vpnMaxEndpointCount) {
        this.vpnMaxEndpointCount = vpnMaxEndpointCount;
    }

    public String getVpnMaxIngressFlowCount() {
        return vpnMaxIngressFlowCount;
    }

    public void setVpnMaxIngressFlowCount(String vpnMaxIngressFlowCount) {
        this.vpnMaxIngressFlowCount = vpnMaxIngressFlowCount;
    }

    public String getVpnMaxMsgSpoolUsage() {
        return vpnMaxMsgSpoolUsage;
    }

    public void setVpnMaxMsgSpoolUsage(String vpnMaxMsgSpoolUsage) {
        this.vpnMaxMsgSpoolUsage = vpnMaxMsgSpoolUsage;
    }

    public String getVpnMaxSubscriptionCount() {
        return vpnMaxSubscriptionCount;
    }

    public void setVpnMaxSubscriptionCount(String vpnMaxSubscriptionCount) {
        this.vpnMaxSubscriptionCount = vpnMaxSubscriptionCount;
    }

    public String getVpnMaxTransactedSessionCount() {
        return vpnMaxTransactedSessionCount;
    }

    public void setVpnMaxTransactedSessionCount(String vpnMaxTransactedSessionCount) {
        this.vpnMaxTransactedSessionCount = vpnMaxTransactedSessionCount;
    }

    public String getVpnMaxTransactionCount() {
        return vpnMaxTransactionCount;
    }

    public void setVpnMaxTransactionCount(String vpnMaxTransactionCount) {
        this.vpnMaxTransactionCount = vpnMaxTransactionCount;
    }

    public String getVpnName() {
        return vpnName;
    }

    public void setVpnName(String vpnName) {
        this.vpnName = vpnName;
    }

}
