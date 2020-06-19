package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class Permission {

    @Expose
    private String id;
    @Expose
    private String name;
    @Expose
    private PermissionTags permissionTags;
    @Expose
    private String type;
    @Expose
    private Boolean visible;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PermissionTags getPermissionTags() {
        return permissionTags;
    }

    public void setPermissionTags(PermissionTags permissionTags) {
        this.permissionTags = permissionTags;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

}
