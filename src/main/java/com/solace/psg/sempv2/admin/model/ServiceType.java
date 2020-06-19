/**
 * 
 */
package com.solace.psg.sempv2.admin.model;

/**
 * Enumerated service types
 * @author VictorTsonkov
 *
 */
public class ServiceType
{
	/**
	 * Service type - enterprise.
	 */
	public static final String ENTERPRISE = "enterprise";

	/**
	 * Service type - developer.
	 */
	public static final String DEVELOPER = "developer";
	
	public static String fromValue(String value)
	{
		if (value.equals(DEVELOPER))
			return DEVELOPER;
		else if (value.equals(ENTERPRISE))
			return ENTERPRISE;
		else throw new IllegalArgumentException("Unknown value:" + value);
	}

}
