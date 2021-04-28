package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class ClientProfile {

	
    @Expose
    private String allowBridgeConnectionsEnabled = "false";
    @Expose
    private String allowGuaranteedEndpointCreateEnabled = "false";
    @Expose
    private String allowGuaranteedMsgReceiveEnabled  = "false";
    @Expose
    private String allowGuaranteedMsgSendEnabled = "false";
    @Expose
    private String allowSharedSubscriptionsEnabled = "false";
    @Expose
    private String allowTransactedSessionsEnabled = "false";
    @Expose
    private Object apiQueueManagementCopyFromOnCreateName;
    @Expose
    private Object apiTopicEndpointManagementCopyFromOnCreateName;
    @Expose
    private String clientProfileName;
    @Expose
    private String elidingEnabled = "false";
    @Expose
    private Object id;
    @Expose
    private String maxConnectionCountPerClientUsername;
    @Expose
    private Object maxEgressFlowCount;
    @Expose
    private Object maxEndpointCountPerClientUsername;
    @Expose
    private Object maxIngressFlowCount;
    @Expose
    private Object maxSubscriptionCount;
    @Expose
    private Object maxTransactedSessionCount;
    @Expose
    private Object maxTransactionCount;
    @Expose
    private Object queueGuaranteed1MinMsgBurst;
    @Expose
    private Object serviceSmfMaxConnectionCountPerClientUsername;
    @Expose
    private Object serviceWebMaxConnectionCountPerClientUsername;
    @Expose
    private Object tcpCongestionWindowSize;
    @Expose
    private Object tcpKeepaliveCount;
    @Expose
    private Object tcpKeepaliveIdleTime;
    @Expose
    private Object tcpKeepaliveInterval;
    @Expose
    private Object tcpMaxSegmentSize;
    @Expose
    private Object tcpMaxWindowSize;
    @Expose
    private String type;

    public String getAllowBridgeConnectionsEnabled() {
        return allowBridgeConnectionsEnabled;
    }

    public void setAllowBridgeConnectionsEnabled(String allowBridgeConnectionsEnabled) {
        this.allowBridgeConnectionsEnabled = allowBridgeConnectionsEnabled;
    }

    public String getAllowGuaranteedEndpointCreateEnabled() {
        return allowGuaranteedEndpointCreateEnabled;
    }

    public void setAllowGuaranteedEndpointCreateEnabled(String allowGuaranteedEndpointCreateEnabled) {
        this.allowGuaranteedEndpointCreateEnabled = allowGuaranteedEndpointCreateEnabled;
    }

    public String getAllowGuaranteedMsgReceiveEnabled() {
        return allowGuaranteedMsgReceiveEnabled;
    }

    public void setAllowGuaranteedMsgReceiveEnabled(String allowGuaranteedMsgReceiveEnabled) {
        this.allowGuaranteedMsgReceiveEnabled = allowGuaranteedMsgReceiveEnabled;
    }

    public String getAllowGuaranteedMsgSendEnabled() {
        return allowGuaranteedMsgSendEnabled;
    }

    public void setAllowGuaranteedMsgSendEnabled(String allowGuaranteedMsgSendEnabled) {
        this.allowGuaranteedMsgSendEnabled = allowGuaranteedMsgSendEnabled;
    }

    public String getAllowSharedSubscriptionsEnabled() {
        return allowSharedSubscriptionsEnabled;
    }

    public void setAllowSharedSubscriptionsEnabled(String allowSharedSubscriptionsEnabled) {
        this.allowSharedSubscriptionsEnabled = allowSharedSubscriptionsEnabled;
    }

    public String getAllowTransactedSessionsEnabled() {
        return allowTransactedSessionsEnabled;
    }

    public void setAllowTransactedSessionsEnabled(String allowTransactedSessionsEnabled) {
        this.allowTransactedSessionsEnabled = allowTransactedSessionsEnabled;
    }

    public Object getApiQueueManagementCopyFromOnCreateName() {
        return apiQueueManagementCopyFromOnCreateName;
    }

    public void setApiQueueManagementCopyFromOnCreateName(Object apiQueueManagementCopyFromOnCreateName) {
        this.apiQueueManagementCopyFromOnCreateName = apiQueueManagementCopyFromOnCreateName;
    }

    public Object getApiTopicEndpointManagementCopyFromOnCreateName() {
        return apiTopicEndpointManagementCopyFromOnCreateName;
    }

    public void setApiTopicEndpointManagementCopyFromOnCreateName(Object apiTopicEndpointManagementCopyFromOnCreateName) {
        this.apiTopicEndpointManagementCopyFromOnCreateName = apiTopicEndpointManagementCopyFromOnCreateName;
    }

    public String getClientProfileName() {
        return clientProfileName;
    }

    public void setClientProfileName(String clientProfileName) {
        this.clientProfileName = clientProfileName;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getMaxConnectionCountPerClientUsername() {
        return maxConnectionCountPerClientUsername;
    }

    public void setMaxConnectionCountPerClientUsername(String maxConnectionCountPerClientUsername) {
        this.maxConnectionCountPerClientUsername = maxConnectionCountPerClientUsername;
    }

    public Object getMaxEgressFlowCount() {
        return maxEgressFlowCount;
    }

    public void setMaxEgressFlowCount(Object maxEgressFlowCount) {
        this.maxEgressFlowCount = maxEgressFlowCount;
    }

    public Object getMaxEndpointCountPerClientUsername() {
        return maxEndpointCountPerClientUsername;
    }

    public void setMaxEndpointCountPerClientUsername(Object maxEndpointCountPerClientUsername) {
        this.maxEndpointCountPerClientUsername = maxEndpointCountPerClientUsername;
    }

    public Object getMaxIngressFlowCount() {
        return maxIngressFlowCount;
    }

    public void setMaxIngressFlowCount(Object maxIngressFlowCount) {
        this.maxIngressFlowCount = maxIngressFlowCount;
    }

    public Object getMaxSubscriptionCount() {
        return maxSubscriptionCount;
    }

    public void setMaxSubscriptionCount(Object maxSubscriptionCount) {
        this.maxSubscriptionCount = maxSubscriptionCount;
    }

    public Object getMaxTransactedSessionCount() {
        return maxTransactedSessionCount;
    }

    public void setMaxTransactedSessionCount(Object maxTransactedSessionCount) {
        this.maxTransactedSessionCount = maxTransactedSessionCount;
    }

    public Object getMaxTransactionCount() {
        return maxTransactionCount;
    }

    public void setMaxTransactionCount(Object maxTransactionCount) {
        this.maxTransactionCount = maxTransactionCount;
    }

    public Object getQueueGuaranteed1MinMsgBurst() {
        return queueGuaranteed1MinMsgBurst;
    }

    public void setQueueGuaranteed1MinMsgBurst(Object queueGuaranteed1MinMsgBurst) {
        this.queueGuaranteed1MinMsgBurst = queueGuaranteed1MinMsgBurst;
    }

    public Object getServiceSmfMaxConnectionCountPerClientUsername() {
        return serviceSmfMaxConnectionCountPerClientUsername;
    }

    public void setServiceSmfMaxConnectionCountPerClientUsername(Object serviceSmfMaxConnectionCountPerClientUsername) {
        this.serviceSmfMaxConnectionCountPerClientUsername = serviceSmfMaxConnectionCountPerClientUsername;
    }

    public Object getServiceWebMaxConnectionCountPerClientUsername() {
        return serviceWebMaxConnectionCountPerClientUsername;
    }

    public void setServiceWebMaxConnectionCountPerClientUsername(Object serviceWebMaxConnectionCountPerClientUsername) {
        this.serviceWebMaxConnectionCountPerClientUsername = serviceWebMaxConnectionCountPerClientUsername;
    }

    public Object getTcpCongestionWindowSize() {
        return tcpCongestionWindowSize;
    }

    public void setTcpCongestionWindowSize(Object tcpCongestionWindowSize) {
        this.tcpCongestionWindowSize = tcpCongestionWindowSize;
    }

    public Object getTcpKeepaliveCount() {
        return tcpKeepaliveCount;
    }

    public void setTcpKeepaliveCount(Object tcpKeepaliveCount) {
        this.tcpKeepaliveCount = tcpKeepaliveCount;
    }

    public Object getTcpKeepaliveIdleTime() {
        return tcpKeepaliveIdleTime;
    }

    public void setTcpKeepaliveIdleTime(Object tcpKeepaliveIdleTime) {
        this.tcpKeepaliveIdleTime = tcpKeepaliveIdleTime;
    }

    public Object getTcpKeepaliveInterval() {
        return tcpKeepaliveInterval;
    }

    public void setTcpKeepaliveInterval(Object tcpKeepaliveInterval) {
        this.tcpKeepaliveInterval = tcpKeepaliveInterval;
    }

    public Object getTcpMaxSegmentSize() {
        return tcpMaxSegmentSize;
    }

    public void setTcpMaxSegmentSize(Object tcpMaxSegmentSize) {
        this.tcpMaxSegmentSize = tcpMaxSegmentSize;
    }

    public Object getTcpMaxWindowSize() {
        return tcpMaxWindowSize;
    }

    public void setTcpMaxWindowSize(Object tcpMaxWindowSize) {
        this.tcpMaxWindowSize = tcpMaxWindowSize;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
