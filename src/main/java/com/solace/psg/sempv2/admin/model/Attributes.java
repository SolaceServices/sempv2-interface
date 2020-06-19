package com.solace.psg.sempv2.admin.model;


import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class Attributes {

    @Expose
    private CustomizedMessagingPorts customizedMessagingPorts;
    @Expose
    private CustomizedResourceNames customizedResourceNames;

    public CustomizedMessagingPorts getCustomizedMessagingPorts() {
        return customizedMessagingPorts;
    }

    public void setCustomizedMessagingPorts(CustomizedMessagingPorts customizedMessagingPorts) {
        this.customizedMessagingPorts = customizedMessagingPorts;
    }

    public CustomizedResourceNames getCustomizedResourceNames() {
        return customizedResourceNames;
    }

    public void setCustomizedResourceNames(CustomizedResourceNames customizedResourceNames) {
        this.customizedResourceNames = customizedResourceNames;
    }

}
