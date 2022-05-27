/**
 * 
 */
package com.solace.psg.sempv2.auth;


import java.util.List;
import java.util.Map;

import com.solace.psg.sempv2.apiclient.Pair;


/**
 * BearerToken authentication type implementation.
 * 
 * 
 *
 */
public class BearerTokenAuth implements Authentication
{
	private String token;

	public static String AUTH_TYPE = "BearerToken";
	
	/**
	 * @return the token
	 */
	public String getToken()
	{
		return token;
	}

	/**
	 * @param token the token to set
	 */
	public void setToken(String token)
	{
		this.token = token;
	}

	/**
	 * Initialises a new instance of the class.
	 */
	public BearerTokenAuth()
	{
	}

	/**
	 * Initialises a new instance of the class.
	 */
	public BearerTokenAuth(String token)
	{
		this.token = token;
	}

	@Override
	public void applyToParams(List<Pair> queryParams, Map<String, String> headerParams)
	{
		if (token == null)
		{
			return;
		}
		/*
		 * byte[] bytes; try { bytes = token.getBytes("ISO-8859-1"); } catch
		 * (UnsupportedEncodingException e) { throw new AssertionError(); } String
		 * encoded = ByteString.of(bytes).base64();
		 * 
		 * headerParams.put("Authorization", "Bearer " + encoded);
		 */
		headerParams.put("Authorization", "Bearer " + token);
	}

	@Override
	public String getAuthType()
	{
		return AUTH_TYPE;
	}


}
