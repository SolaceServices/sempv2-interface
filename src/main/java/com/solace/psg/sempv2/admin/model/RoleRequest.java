/**
 * 
 */
package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author VictorTsonkov
 *
 */
@javax.annotation.Resource
public class RoleRequest
{
	@SerializedName("id")
	private String userRoleId;
	
	public RoleRequest(String userRoleId)
	{
		this.userRoleId = userRoleId;
	}
	
	/**
	 * @return the userRoleId
	 */
	public String getUserRoleId()
	{
		return userRoleId;
	}

	/**
	 * @param userRoleId the userRoleId to set
	 */
	public void setUserRoleId(String userRoleId)
	{
		this.userRoleId = userRoleId;
	}

}
