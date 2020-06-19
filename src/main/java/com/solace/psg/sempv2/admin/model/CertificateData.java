package com.solace.psg.sempv2.admin.model;


import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class CertificateData {

    @Expose
    private String adminProgress;
    @Expose
    private Certificate certificate;
    @Expose
    private Long creationTimestamp;
    @Expose
    private String id;
    @Expose
    private String type;

    public String getAdminProgress() {
        return adminProgress;
    }

    public void setAdminProgress(String adminProgress) {
        this.adminProgress = adminProgress;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
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

