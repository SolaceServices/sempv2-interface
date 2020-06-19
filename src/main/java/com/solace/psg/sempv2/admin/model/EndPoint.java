package com.solace.psg.sempv2.admin.model;

import java.util.List;
import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class EndPoint {

    @Expose
    private String compressed;
    @Expose
    private String name;
    @Expose
    private String secured;
    @Expose
    private String transport;
    @Expose
    private List<String> uris;

    public String getCompressed() {
        return compressed;
    }

    public void setCompressed(String compressed) {
        this.compressed = compressed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSecured() {
        return secured;
    }

    public void setSecured(String secured) {
        this.secured = secured;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

}
