package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class ClientProfile94
{
    @Expose
    private String allowBridgeConnectionsEnabled = "false";
    @Expose
    private String allowGuaranteedEndpointCreateEnabled = "false";
    @Expose
    private String allowGuaranteedMsgReceiveEnabled = "false";
    @Expose
    private String allowGuaranteedMsgSendEnabled = "false";
    @Expose
    private String allowTransactedSessionsEnabled = "false";
    @Expose
    private String allowSharedSubscriptionsEnabled = "false";

    /**
	 * @return the allowSharedSubscriptionsEnabled
	 */
	public String getAllowSharedSubscriptionsEnabled()
	{
		return allowSharedSubscriptionsEnabled;
	}

	/**
	 * @param allowSharedSubscriptionsEnabled the allowSharedSubscriptionsEnabled to set
	 */
	public void setAllowSharedSubscriptionsEnabled(String allowSharedSubscriptionsEnabled)
	{
		this.allowSharedSubscriptionsEnabled = allowSharedSubscriptionsEnabled;
	}

	@Expose   
    private String clientProfileName;
    @Expose
    private String id;
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

    public String getAllowTransactedSessionsEnabled() {
        return allowTransactedSessionsEnabled;
    }

    public void setAllowTransactedSessionsEnabled(String allowTransactedSessionsEnabled) {
        this.allowTransactedSessionsEnabled = allowTransactedSessionsEnabled;
    }

    public String getClientProfileName() {
        return clientProfileName;
    }

    public void setClientProfileName(String clientProfileName) {
        this.clientProfileName = clientProfileName;
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
