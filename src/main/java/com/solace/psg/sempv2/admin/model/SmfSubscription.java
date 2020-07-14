package com.solace.psg.sempv2.admin.model;

public class SmfSubscription extends Subscription
{

	public SmfSubscription(String name, SubscriptionDirection direction, SubscriptionType subscriptionType)
	{
		this.name = name;
		type = "SMF";
		this.subscriptionType = subscriptionType;
		this.direction = direction;
	}
}
