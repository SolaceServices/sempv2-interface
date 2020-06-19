package com.solace.psg.sempv2.admin.model;

import java.util.List;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class AccountingLimit {

    @Expose
    private String id;
    @Expose
    private List<Threshold> thresholds;
    @Expose
    private String unit;
    @Expose
    private String value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Threshold> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<Threshold> thresholds) {
        this.thresholds = thresholds;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
