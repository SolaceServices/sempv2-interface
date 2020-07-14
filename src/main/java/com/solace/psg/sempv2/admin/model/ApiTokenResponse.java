/**
 * 
 */
package com.solace.psg.sempv2.admin.model;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.ApiModelProperty;

/**
 * Class to encapsulate ApiToken Response.
 *
 */
@javax.annotation.Resource
public class ApiTokenResponse
{
	@SerializedName("token")
	String token;
	

	@ApiModelProperty(value = "")
	public String getToken()
	{
		return token;
	}

	/**
	 * Initialises a new instance of the class.
	 */
	public ApiTokenResponse()
	{
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
		ApiTokenResponse response = (ApiTokenResponse) o;
		return Objects.equals(this.token, response.token);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(token);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("class ApiTokenResponse {\n");

		sb.append("    token: ").append(toIndentedString(token)).append("\n");
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
