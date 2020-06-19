package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.Expose;

/**
 * Class to encapsulate JSON request for Service create 
 * @author VictorTsonkov
 *
 */
public class ServiceCreateRequest
{
    @Expose
    private String name;

    @Expose
    private String serviceTypeId;

    @Expose
    private String serviceClassId;

    @Expose
    private String datacenterId;

    @Expose
    private String partitionId;

    @Expose
    private String adminState;
    
    @Expose
    private String msgVpnName;

    /**
	 * @return the msgVpnName
	 */
	public String getMsgVpnName()
	{
		return msgVpnName;
	}

	/**
	 * @param msgVpnName the msgVpnName to set
	 */
	public void setMsgVpnName(String msgVpnName)
	{
		this.msgVpnName = msgVpnName;
	}

	public ServiceCreateRequest(String name, String serviceTypeId, String serviceClassId, String datacenterId, String partitionId, String adminState)
    {
    	this.name = name;
    	this.serviceTypeId = serviceTypeId;
    	this.serviceClassId = serviceClassId;
    	this.datacenterId = datacenterId;
    	this.partitionId = partitionId;
    	this.adminState = adminState;
    	
    	this.msgVpnName = name;
    }
    
    public ServiceCreateRequest() 
    {
    }
    
	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @return the serviceTypeId
	 */
	public String getServiceTypeId()
	{
		return serviceTypeId;
	}

	/**
	 * @param serviceTypeId the serviceTypeId to set
	 */
	public void setServiceTypeId(String serviceTypeId)
	{
		this.serviceTypeId = serviceTypeId;
	}

	/**
	 * @return the serviceClassId
	 */
	public String getServiceClassId()
	{
		return serviceClassId;
	}

	/**
	 * @param serviceClassId the serviceClassId to set
	 */
	public void setServiceClassId(String serviceClassId)
	{
		this.serviceClassId = serviceClassId;
	}

	/**
	 * @return the datacenterId
	 */
	public String getDatacenterId()
	{
		return datacenterId;
	}

	/**
	 * @param datacenterId the datacenterId to set
	 */
	public void setDatacenterId(String datacenterId)
	{
		this.datacenterId = datacenterId;
	}

	/**
	 * @return the partitionId
	 */
	public String getPartitionId()
	{
		return partitionId;
	}

	/**
	 * @param partitionId the partitionId to set
	 */
	public void setPartitionId(String partitionId)
	{
		this.partitionId = partitionId;
	}

	/**
	 * @return the adminState
	 */
	public String getAdminState()
	{
		return adminState;
	}

	/**
	 * @param adminState the adminState to set
	 */
	public void setAdminState(String adminState)
	{
		this.adminState = adminState;
	}
}
