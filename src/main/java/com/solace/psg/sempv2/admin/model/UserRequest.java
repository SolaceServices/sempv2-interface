/**
 * 
 */
package com.solace.psg.sempv2.admin.model;

import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * A User Request class to invite a user with a list of roles.
 * @author VictorTsonkov
 *
 */
public class UserRequest
{
	@Expose
	private String email;
	
	@Expose
	private List<String> roles;
	
	public UserRequest(String email, List<String> roles)
	{
		this.email = email;
		this.roles = roles;
	}

	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	public List<String> getRoles()
	{
		return roles;
	}

	public void setRoles(List<String> roles)
	{
		this.roles = roles;
	}
}
