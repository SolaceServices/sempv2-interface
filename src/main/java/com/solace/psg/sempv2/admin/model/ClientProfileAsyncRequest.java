/**
 * 
 */
package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

/**
 * Class to handle Client Profile request payload.
 * 
 * @author VictorTsonkov
 *
 */
public class ClientProfileAsyncRequest
{
	
    @Expose
    private ClientProfile clientProfile;
    @Expose
    private String operation = "create";

    public ClientProfile getClientProfile() {
        return clientProfile;
    }

    public void setClientProfile(ClientProfile clientProfile) {
        this.clientProfile = clientProfile;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

}
