package com.solace.psg.sempv2.admin.model;


import com.google.gson.annotations.SerializedName;

@javax.annotation.Resource
public class ServiceClassDisplayedAttributes 
{

    @SerializedName("Clients")
    private String clients;
    @SerializedName("High Availability")
    private String highAvailability;
    @SerializedName("Message Broker Tenancy")
    private String messageBrokerTenancy;
    @SerializedName("Network Speed")
    private String networkSpeed;
    @SerializedName("Network Usage")
    private String networkUsage;
    @SerializedName("Queues")
    private String queues;
    @SerializedName("Storage")
    private String storage;

    public String getClients() {
        return clients;
    }

    public void setClients(String clients) {
        this.clients = clients;
    }

    public String getHighAvailability() {
        return highAvailability;
    }

    public void setHighAvailability(String highAvailability) {
        this.highAvailability = highAvailability;
    }

    public String getMessageBrokerTenancy() {
        return messageBrokerTenancy;
    }

    public void setMessageBrokerTenancy(String messageBrokerTenancy) {
        this.messageBrokerTenancy = messageBrokerTenancy;
    }

    public String getNetworkSpeed() {
        return networkSpeed;
    }

    public void setNetworkSpeed(String networkSpeed) {
        this.networkSpeed = networkSpeed;
    }

    public String getNetworkUsage() {
        return networkUsage;
    }

    public void setNetworkUsage(String networkUsage) {
        this.networkUsage = networkUsage;
    }

    public String getQueues() {
        return queues;
    }

    public void setQueues(String queues) {
        this.queues = queues;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

}
