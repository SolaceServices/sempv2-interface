/**
 * 
 */
package com.solace.psg.sempv2.admin.model;

/**
 * Class to represent User role types
 *
 */
public class UserRoles
{
	public static final String Administrator = "administrator";

	public static final String BillingAdministrator = "billing-administrator";
	
	public static final String MessagingServiceViewer = "messaging-service-viewer";
	
	public static final String MessagingServiceEditor = "messaging-service-editor";
		
	/**
	 * Should not be instantiated. 
	 */
	private UserRoles()
	{
	}

}
