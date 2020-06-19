package com.solace.psg.sempv2.admin.model;

public class MqttSubscription extends Subscription
{
	/**
	 * Initialises a new instance of the class.
	 */
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

}
