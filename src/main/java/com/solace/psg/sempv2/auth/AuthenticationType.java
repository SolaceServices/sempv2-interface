/**
 * 
 */
package com.solace.psg.sempv2.auth;

/**
 * Enumerates different authentication Types supported by the ApiClient 
 * 
 * 
 *
 */
public enum AuthenticationType
{
	None,
	ApiKey,
	Basic,
	BearerToken,
	Digest,
	OAuth1_0,
	OAuth2_0,
	Hawk,
	AWSSignature,
	AkamaiEdgeGrid,
	NTLM
}
