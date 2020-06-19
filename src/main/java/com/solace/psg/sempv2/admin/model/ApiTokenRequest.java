/**
 * 
 */
package com.solace.psg.sempv2.admin.model;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.ApiModelProperty;

/**
 * API token request model
 * @author VictorTsonkov
 *
 */
@javax.annotation.Resource
public class ApiTokenRequest
{
	@SerializedName("username")
	private String username;
	
	@SerializedName("password")
	private String password;
	
	/**
	 * @return the username
	*/
	@ApiModelProperty(value = "Account username")
	public String getUsername()
	{
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username)
	{
		this.username = username;
	}

	/**
	 * @return the password
	 */
	@ApiModelProperty(value = "Account password")
	public String getPassword()
	{
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password)
	{
		this.password = password;
	}

	/**
	 * Initialises a new instance of the class.
	 */
	public ApiTokenRequest(String username, String password)
	{
		this.username = username;
		this.password = password;
	}

	@Override
	public boolean equals(java.lang.Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		ApiTokenRequest apiToken = (ApiTokenRequest) o;
		return Objects.equals(this.username, apiToken.username) && Objects.equals(this.password, apiToken.password);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(username, password);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("class ApiTokenRequest {\n");

		sb.append("    username: ").append(toIndentedString(username)).append("\n");
		sb.append("    password: ").append(toIndentedString(password)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o)
	{
		if (o == null)
		{
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}
}
