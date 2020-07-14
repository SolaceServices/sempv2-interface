/**
 * 
 */
package com.solace.psg.sempv2.admin.model;

import java.util.List;

import com.google.gson.annotations.Expose;

public class UserRequest
{
	@Expose
	private String email;

	@Expose
	private String firstName;
	
	@Expose
	private String lastName;

	@Expose
	private String company;
	
	/**
	 * @return the firstName
	 */
	public String getFirstName()
	{
		return firstName;
	}

	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName()
	{
		return lastName;
	}

	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	/**
	 * @return the company
	 */
	public String getCompany()
	{
		return company;
	}

	/**
	 * @param company the company to set
	 */
	public void setCompany(String company)
	{
		this.company = company;
	}

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
