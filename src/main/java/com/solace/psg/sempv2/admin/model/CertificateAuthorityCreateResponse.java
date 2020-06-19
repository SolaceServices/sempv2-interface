package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class CertificateAuthorityCreateResponse {

    @Expose
    private CertificateData data;

    public CertificateData getData() {
        return data;
    }

    public void setData(CertificateData data) {
        this.data = data;
    }

}
