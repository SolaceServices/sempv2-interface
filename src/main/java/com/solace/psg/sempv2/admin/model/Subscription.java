/**
 * 
 */
package com.solace.psg.sempv2.admin.model;

/**
 * Class to represent a subscription.
 *
 */
public abstract class Subscription
{ 
	protected String name;
	protected String type;
	protected SubscriptionDirection direction;
	protected SubscriptionType subscriptionType;
	
	/**
	 * Gets the name of the topic.
	 * @return
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Sets the name of the topic.
	 * @param name
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * Gets the subscription type like SMF or MQTT if custom functionality is needed.
	 * @return
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * Gets direction.
	 * @return the direction
	 */
	public SubscriptionDirection getDirection()
	{
		return direction;
	}

	/**
	 * Sets direction.
	 * @param direction the direction to set
	 */
	public void setDirection(SubscriptionDirection direction)
	{
		this.direction = direction;
	}

	/**
	 * Gets the subscription type.
	 * @return the subscriptionType
	 */
	public SubscriptionType getSubscriptionType()
	{
		return subscriptionType;
	}

	/**
	 * Sets the subscription type.
	 * @param subscriptionType the subscriptionType to set
	 */
	public void setSubscriptionType(SubscriptionType subscriptionType)
	{
		this.subscriptionType = subscriptionType;
	}
}
