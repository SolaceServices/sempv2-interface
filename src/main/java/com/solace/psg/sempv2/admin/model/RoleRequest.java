/**
 * 
 */
package com.solace.psg.sempv2.admin.model;

import com.google.gson.annotations.SerializedName;


@javax.annotation.Resource
public class RoleRequest
{
	@SerializedName("id")
	private String userRoleId;
	
	public RoleRequest(String userRoleId)
	{
		this.userRoleId = userRoleId;
	}
	

	public String getUserRoleId()
	{
		return userRoleId;
	}


	public void setUserRoleId(String userRoleId)
	{
		this.userRoleId = userRoleId;
	}

}
