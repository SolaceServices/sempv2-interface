package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class CertificateAuthorityStatusResponse {

    @Expose
    private CertificateData data;
    @Expose
    private Meta meta;

    public CertificateData getData() {
        return data;
    }

    public void setData(CertificateData data) {
        this.data = data;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

}
