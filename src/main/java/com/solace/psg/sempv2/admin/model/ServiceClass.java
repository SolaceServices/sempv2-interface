/**
 * 
 */
package com.solace.psg.sempv2.admin.model;

/**
 * Enumerates service classes.
 * @author VictorTsonkov
 *
 */
public class ServiceClass
{
	/**
	 * Used with service type developer
	 */
	public static final String DEVELOPER = "developer";
	
	/**
	 * Used with service type enterprise.
	 */
	public static final String ENTERPRISE_KILO = "enterprise-kilo";

	/**
	 * Used with service type enterprise.
	 */
	public static final String ENTERPRISE_MEGA = "enterprise-mega";
	
	/**
	 * Used with service type enterprise.
	 */
	public static final String ENTERPRISE_GIGA = "enterprise-giga";
	
	/**
	 * Used with service type enterprise.
	 */
	public static final String ENTERPRISE_TERA_50K = "enterprise-tera-50k"; // may not be the correct value
	
	/**
	 * Used with service type enterprise.
	 */
	public static final String ENTERPRISE_TERA_100K = "enterprise-tera-100k";	// may not be the correct value
	
	public static String fromValue(String value)
	{
		if (value.equals(DEVELOPER))
			return DEVELOPER;
		else if (value.equals(ENTERPRISE_KILO))
			return ENTERPRISE_KILO;
		else if (value.equals(ENTERPRISE_MEGA))
			return ENTERPRISE_MEGA;
		else if (value.equals(ENTERPRISE_GIGA))
			return ENTERPRISE_GIGA;
		else if (value.equals(ENTERPRISE_TERA_50K))
			return ENTERPRISE_TERA_50K;
		else if (value.equals(ENTERPRISE_TERA_100K))
			return ENTERPRISE_TERA_100K;
		else throw new IllegalArgumentException("Unknown value:" + value);
	}
}
