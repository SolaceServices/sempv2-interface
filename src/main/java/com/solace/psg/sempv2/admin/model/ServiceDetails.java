package com.solace.psg.sempv2.admin.model;

import java.util.List;
import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class ServiceDetails extends Service
{
    @Expose
    private Cluster cluster;
    @Expose
    private List<ManagementProtocol> managementProtocols;
    @Expose
    private List<MessagingProtocol> messagingProtocols;


    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

 
    public List<ManagementProtocol> getManagementProtocols() {
        return managementProtocols;
    }

    public void setManagementProtocols(List<ManagementProtocol> managementProtocols) {
        this.managementProtocols = managementProtocols;
    }

    public List<MessagingProtocol> getMessagingProtocols() {
        return messagingProtocols;
    }

    public void setMessagingProtocols(List<MessagingProtocol> messagingProtocols) {
        this.messagingProtocols = messagingProtocols;
    }
}
