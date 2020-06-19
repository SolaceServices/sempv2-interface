package com.solace.psg.sempv2.admin.model;

import java.util.List;
import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class User {

    @Expose
    private String company;
    @Expose
    private String email;
    @Expose
    private String firstName;
    @Expose
    private String lastName;
    @Expose
    private String organizationId;
    @Expose
    private List<String> roles;
    @Expose
    private String state;
    @Expose
    private Long timestamp;
    @Expose
    private Boolean tipsEmails;
    @Expose
    private String type;
    @Expose
    private UserAttributes userAttributes;
    @Expose
    private String userId;

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getTipsEmails() {
        return tipsEmails;
    }

    public void setTipsEmails(Boolean tipsEmails) {
        this.tipsEmails = tipsEmails;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UserAttributes getUserAttributes() {
        return userAttributes;
    }

    public void setUserAttributes(UserAttributes userAttributes) {
        this.userAttributes = userAttributes;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
