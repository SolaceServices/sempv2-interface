package com.solace.psg.sempv2.admin.model;

public class SmfSubscription extends Subscription
{
	/**
	 * Initialises a new instance of the class.
	 */
	public SmfSubscription(String name, SubscriptionDirection direction, SubscriptionType subscriptionType)
	{
		this.name = name;
		type = "SMF";
		this.subscriptionType = subscriptionType;
		this.direction = direction;
	}
}
