package com.solace.psg.sempv2.admin.model;

import java.util.List;
import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class Role {

    @Expose
    private Boolean defaultRole;
    @Expose
    private String id;
    @Expose
    private String name;
    @Expose
    private List<Permission> permissions;
    @Expose
    private String type;

    public Boolean getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(Boolean defaultRole) {
        this.defaultRole = defaultRole;
    }

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

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
