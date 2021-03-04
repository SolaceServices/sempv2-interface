package com.solace.psg.sempv2.admin.model;

import java.util.List;
import com.google.gson.annotations.Expose;

@javax.annotation.Resource
public class Organization {

    @Expose
    private String organizationId;
    @Expose
    private String name;
    @Expose
    private String organizationType;
    @Expose
    private List<String> policies;
    @Expose
    private String type;
    @Expose
    private Long createdTimestamp;
    @Expose
    private Long deletedTimestamp;
    
    public Long getDeletedTimestamp()
	{
		return deletedTimestamp;
	}

	public void setDeletedTimestamp(Long deletedTimestamp)
	{
		this.deletedTimestamp = deletedTimestamp;
	}

	public String getOrganizationType()
	{
		return organizationType;
	}

	public void setOrganizationType(String organizationType)
	{
		this.organizationType = organizationType;
	}

	public Long getCreatedTimestamp()
	{
		return createdTimestamp;
	}

	public void setCreatedTimestamp(Long timestamp)
	{
		this.createdTimestamp = timestamp;
	}

	public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String id) {
        this.organizationId = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPolicies() {
        return policies;
    }

    public void setPermissions(List<String> policies) {
        this.policies = policies;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
