package com.solace.psg.sempv2.admin.model;

public class MqttSubscription extends Subscription
{

	public MqttSubscription(String name)
	{
		this.name = name;
		type = "MQTT";
	}

	@Override
	public void setName(String name)
	{
		this.name = name;
	}
	
	public MqttSubscription(String name, SubscriptionDirection direction, SubscriptionType subscriptionType)
	{
		this.name = name;
		type = "SMF";
		this.subscriptionType = subscriptionType;
		this.direction = direction;
	}
}
