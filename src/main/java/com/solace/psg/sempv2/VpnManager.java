package com.solace.psg.sempv2;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.solace.psg.sempv2.admin.model.ServiceDetails;
import com.solace.psg.sempv2.admin.model.ServiceManagementContext;
import com.solace.psg.sempv2.admin.model.Subscription;
import com.solace.psg.sempv2.admin.model.SubscriptionDirection;
import com.solace.psg.sempv2.admin.model.SubscriptionType;
import com.solace.psg.sempv2.apiclient.ApiClient;
import com.solace.psg.sempv2.apiclient.ApiException;

import com.solace.psg.sempv2.apiclient.Pair;
import com.solace.psg.sempv2.auth.Authentication;



import com.solace.psg.sempv2.config.api.MsgVpnApi;


import com.solace.psg.sempv2.config.model.MsgVpnAclProfile;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfilesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridge;
import com.solace.psg.sempv2.config.model.MsgVpnBridge.BridgeVirtualRouterEnum;
import com.solace.psg.sempv2.config.model.MsgVpnBridge.RemoteAuthenticationSchemeEnum;
import com.squareup.okhttp.Call;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeRemoteMsgVpn;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeRemoteMsgVpnResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeRemoteSubscription;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeRemoteSubscriptionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeTlsTrustedCommonName;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeTlsTrustedCommonNameResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridgesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnClientProfile;
import com.solace.psg.sempv2.config.model.MsgVpnClientProfileResponse;
import com.solace.psg.sempv2.config.model.MsgVpnClientProfilesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnClientUsername;
import com.solace.psg.sempv2.config.model.MsgVpnClientUsernameResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDmrBridge;
import com.solace.psg.sempv2.config.model.MsgVpnDmrBridgeResponse;
import com.solace.psg.sempv2.config.model.MsgVpnQueue;
import com.solace.psg.sempv2.config.model.MsgVpnQueueResponse;
import com.solace.psg.sempv2.config.model.MsgVpnQueueSubscription;
import com.solace.psg.sempv2.config.model.MsgVpnQueueSubscriptionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnQueuesResponse;
import com.solace.psg.sempv2.config.model.SempMetaOnlyResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfile.ClientConnectDefaultActionEnum;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfile.PublishTopicDefaultActionEnum;

import com.solace.psg.sempv2.config.model.MsgVpnAclProfile.SubscribeTopicDefaultActionEnum;

/**
 * Class to handle various VPN operations.
 *
 */
public class VpnManager
{
	/**
	 * The details of the Service VPN.
	 */
	private ServiceDetails localService;
	
	/**
	 * Management context of the local VPN.
	 */
	private ServiceManagementContext localContext;
	
	//private String certAuthorities = "/certAuthorities";
	
	/**
	 * Pattern of the bridge names. 
	 */
	private String bridgePattern = "<LocalVpnName>_<RemoteVpnName>";
	
	/**
	 * Pattern of the bridge queue. 
	 */
	private String bridgeQueuePattern = bridgePattern + "_Queue";
	
	/**
	 * Number of ACLs to be returned with get.
	 */
	private int defaultAclCount = 30;
	
	/**
	 * Count of bridges to be fetched. 
	 */
	private int defaultBridgeCount = 50;
	
	private int defaultQueueCount = 100;
	
	private long bridgeEgressWorkflowSize = 255L;
	
	private String unidirectionalClientProfile = "#client-profile";

	private long connectionRetryDelay = 3L;

	private long connectionRetryCount = 0L;

	private long maxTtl = 8L;
	
	// Default Solace Trusted Common Name
	private String defaultSolaceTCN = "*.messaging.solace.cloud";
	
	private String opaquePassword = "ago4156_do78";
	
	/**
	 * The Client config API.
	 */
	private MsgVpnApi configApi;

	/**
	 * The Client monitor API.
	 */
	private com.solace.psg.sempv2.monitor.api.MsgVpnApi monitorApi;
	
	/**
	 * Initialises a new instance of the class.
	 */
	public VpnManager(ServiceDetails service)
	{
		configApi = new MsgVpnApi();
		monitorApi = new com.solace.psg.sempv2.monitor.api.MsgVpnApi();
		
		this.localService = service;
		this.localContext = new ServiceManagementContext(service);
		
		this.opaquePassword = "" + service.hashCode();
		
		setDefaultVpnContext();
	}
	
	/**
	 * Sets the Client API to the VPN context with config endpoint.
	 */
	public void setDefaultVpnContext()
	{
		setVpnContext(this.localContext);
	}
	
	/**
	 * Gets the current opaque password.
	 * @return
	 */
	public String getOpaquePassword()
	{
		return opaquePassword;
	}
	
	/*
	 * Gets the Client API to the VPN context.
	 */
	public ServiceManagementContext getDefaultVpnContext()
	{
		return this.localContext;
	}
	
	/**
	 * Sets the context of the local VPN details for the API Client.
	 */
	public void setVpnContext(ServiceManagementContext context)
	{
		configApi.getApiClient().setBasePath(context.getSempUrl());
		configApi.getApiClient().setUsername(context.getSempUsername());
		configApi.getApiClient().setPassword(context.getSempPassword());
		
		//monitorApi.getApiClient().setBasePath(context.getSempMonitorUrl());
	}

	private enum ApiContext
	{
		ACTION,
		CONFIG,
		MONITOR
	}
	
	/**
	 * Sets the API context.
	 * @param apiContext
	 */
	private void setApiContext(ApiContext apiContext) 
	{
		switch (apiContext)
		{
			case MONITOR:
			{
				configApi.getApiClient().setBasePath(localContext.getSempMonitorUrl());
				break;
			}
			case ACTION:
			{
				configApi.getApiClient().setBasePath(localContext.getSempActionUrl());
				break;
			}
			case CONFIG:
			default:
				configApi.getApiClient().setBasePath(localContext.getSempUrl());
		}
	}
	
	/**
	 * Sets default API URL context to config. 
	 */
	private void setDefaultApiContext() 
	{
		configApi.getApiClient().setBasePath(localContext.getSempUrl());
	}
	
	/**
	 * Add a new ACL Profile to a VPN (Service). Do not set ShareNameDefaultAction parameter.
	 * 
	 * @param profileName
	 * @param clientConnectDefaultAction
	 * @param publishTopicDefaultAction
	 * @param subscribeTopicDefaultAction
	 * @return ACL profile
	 * @throws ApiException
	 */
	public MsgVpnAclProfile addAclProfile(String profileName, String clientConnectDefaultAction, String publishTopicDefaultAction,
			 String subscribeTopicDefaultAction) throws ApiException
	{
		MsgVpnAclProfile profile = new MsgVpnAclProfile();
		profile.setAclProfileName(profileName);
		profile.setClientConnectDefaultAction(ClientConnectDefaultActionEnum.fromValue(publishTopicDefaultAction));
		profile.setPublishTopicDefaultAction(PublishTopicDefaultActionEnum.fromValue(publishTopicDefaultAction));
		profile.setSubscribeTopicDefaultAction(SubscribeTopicDefaultActionEnum.fromValue(subscribeTopicDefaultAction));
		
		profile.setMsgVpnName(localContext.getVpnName());

		return addAclProfile(profile);
	}
	
	/**
	 * Deletes an ACL Profile by name. 
	 * @param profileName
	 * @return
	 * @throws ApiException
	 */
	public boolean deleteAclProfile(String profileName) throws ApiException
	{
		SempMetaOnlyResponse response = configApi.deleteMsgVpnAclProfile(localContext.getVpnName(), profileName);
		
		return (response.getMeta().getResponseCode() == 200);
	}

	/**
	 * Add a new ACL Profile to a VPN (Service).
	 * 
	 * @param profile
	 * @return ACL profile
	 * @throws ApiException
	 */
	public MsgVpnAclProfile addAclProfile(MsgVpnAclProfile profile) throws ApiException
	{
		return addAclProfile(profile, null);
	}

	/**
	 * Add a new ACL Profile to a VPN (Service). Do not set ShareNameDefaultAction parameter.
	 * 
	 * @param profile
	 * @param select  parameters to be excluded from the response
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	public MsgVpnAclProfile addAclProfile(MsgVpnAclProfile profile, List<String> select) throws ApiException
	{
		MsgVpnAclProfileResponse response = configApi.createMsgVpnAclProfile(profile, localContext.getVpnName(), opaquePassword, select);
		return response.getData();
	}
	
	/**
	 * Gets ACL profiles for the VPN.
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	public List<MsgVpnAclProfile> getAclProfiles() throws ApiException
	{
		MsgVpnAclProfilesResponse response = configApi.getMsgVpnAclProfiles(localContext.getVpnName(), defaultAclCount, null, opaquePassword, null, null);
		return response.getData();
	}

	/**
	 * Gets client profile for the VPN.
	 * @return the profile data.
	 * @throws ApiException
	 */
	public MsgVpnClientProfile getClientProfile(String clientProfileName) throws ApiException
	{
		MsgVpnClientProfileResponse response = configApi.getMsgVpnClientProfile(localContext.getVpnName(), clientProfileName, opaquePassword, null);
		return response.getData();
	}

	/**
	 * List client profiles for the VPN.
	 * @return TList of profiles.
	 * @throws ApiException
	 */
	public List<MsgVpnClientProfile> listClientProfiles() throws ApiException
	{
		MsgVpnClientProfilesResponse response = configApi.getMsgVpnClientProfiles(localContext.getVpnName(), defaultAclCount,  null, opaquePassword, null, null);
		return response.getData();
	}
	
	/**
	 * Create a DMR bridge between local VPN and a remote VPN.
	 * @param remoteService
	 * @param remoteNodeName
	 * @return true if success, otherwise false
	 * @throws ApiException
	 */
	public boolean createDMRBridge(ServiceDetails remoteService, String remoteNodeName) throws ApiException
	{
		ServiceManagementContext remoteContext = new ServiceManagementContext(remoteService);
		
		MsgVpnDmrBridge request = new MsgVpnDmrBridge();
		request.setMsgVpnName(localContext.getVpnName());
		request.setRemoteMsgVpnName(remoteContext.getVpnName());
		request.setRemoteNodeName(remoteNodeName);
		
		MsgVpnDmrBridgeResponse response = configApi.createMsgVpnDmrBridge(request , localContext.getVpnName(), opaquePassword, null);
		
		return (response.getMeta().getResponseCode() == 200);
	}

	/**
	 * Adds bridge without subscriptions.
	 * @param remoteVpnName
	 * @return True if successful, otherwise false.
	 * @throws ApiException 
	 */
	public boolean createBridge(ServiceDetails remoteVpnName) throws ApiException
	{
		return createBridge(remoteVpnName, null, null, true, true, false, null, null, null, null, null, null);
	}
	
	/**
	 * Adds a bridge with a list of subscriptions.
	 * @param remoteService
	 * @param subscriptions
	 * @return True if successful, otherwise false.
	 * @throws ApiException 
	 */
	public boolean createBridge(ServiceDetails remoteService, List<Subscription> subscriptions, boolean rollback, boolean cert, String localUsername, String localPassword, String remoteUsername, String remotePassword, String localTcn, String remoteTcn) throws ApiException
	{
		String bridgeName = getBridgeName(remoteService.getMsgVpnAttributes().getVpnName());
		return createBridge(remoteService, bridgeName, subscriptions, true, rollback, cert, localUsername, localPassword, remoteUsername, remotePassword, localTcn, remoteTcn);
	}
	
	/**
	 * Adds a bridge with a list of subscriptions.
	 * @param remoteService
	 * @param subscriptions
	 * @return True if successful, otherwise false.
	 * @throws ApiException 
	 */
	public boolean createBridge(ServiceDetails remoteService, String bridgeName, List<Subscription> subscriptions, boolean ignoreExists, boolean rollback, boolean cert, String localUsername, String localPassword, String remoteUsername, String remotePassword, String localTcn, String remoteTcn) throws ApiException
	{
		boolean result = false;
		
		ServiceManagementContext remoteContext = new ServiceManagementContext(remoteService);

		if ((localUsername == null) || (localUsername.isEmpty()))
		{
			localUsername = localContext.getUserUsername(); 
			localPassword = localContext.getUserPassword(); 
		}
		if ((remoteUsername == null) || (remoteUsername.isEmpty()))
		{
			remoteUsername = remoteContext.getUserUsername(); 
			remotePassword = remoteContext.getUserPassword(); 	
		}
		
		if (localContext.getSmfUrl() == null && localContext.getSecureSmfUrl() == null)
			throw new IllegalArgumentException("SMF URL is null. Bridge cannot be created. Enable SMF or create a TLS bridge.");
		
		String bridgeQeueName = getBridgeQueueName(remoteContext.getVpnName());
		
		// Split subscriptions
		ArrayList<Subscription> localDirectSubscriptions = new ArrayList<Subscription>();
		ArrayList<Subscription> localGuaranteedSubscriptions = new ArrayList<Subscription>();
		ArrayList<Subscription> remoteDirectSubscriptions = new ArrayList<Subscription>();
		ArrayList<Subscription> remoteGuaranteedSubscriptions = new ArrayList<Subscription>();
		if (subscriptions != null)
		{
			for (Subscription subscription : subscriptions)
			{
				if (subscription.getDirection().equals(SubscriptionDirection.Ingoing))
				{
					if (subscription.getSubscriptionType().equals(SubscriptionType.Guaranteed))
						remoteGuaranteedSubscriptions.add(subscription);
					else
						localDirectSubscriptions.add(subscription);
				}
				else //outgoing
				{
					if (subscription.getSubscriptionType().equals(SubscriptionType.Guaranteed))
						localGuaranteedSubscriptions.add(subscription);
					else
						remoteDirectSubscriptions.add(subscription);
				}
			}
		}
				
		MsgVpnBridge request = new MsgVpnBridge();
		request.setBridgeName(bridgeName);
		request.setBridgeVirtualRouter(BridgeVirtualRouterEnum.AUTO);
		request.enabled(false);		
		request.setMaxTtl(maxTtl);
		request.setRemoteConnectionRetryDelay(connectionRetryDelay);
		request.remoteConnectionRetryCount(connectionRetryCount);
		request.setRemoteDeliverToOnePriority(MsgVpnBridge.RemoteDeliverToOnePriorityEnum.P1);
		
		if (cert)
		{
			request.setRemoteAuthenticationScheme(RemoteAuthenticationSchemeEnum.CLIENT_CERTIFICATE);
			request.setRemoteAuthenticationClientCertContent(remoteUsername);
			request.setRemoteAuthenticationClientCertPassword(remotePassword);
		}
		else
		{
			request.setRemoteAuthenticationBasicClientUsername(remoteUsername);
			request.setRemoteAuthenticationBasicPassword(remotePassword);
			request.setRemoteAuthenticationScheme(RemoteAuthenticationSchemeEnum.BASIC);
		}

		// Add local bridge queue, local bridge, remote bridge and subscriptions
		if (!addBridgeQueue(localContext, bridgeQeueName, ignoreExists))
			return false;

		// Create a local bridge
		if (!createLocalBridge(localContext, request))
		{
			if (rollback)
				deleteQueue(localContext.getVpnName(), bridgeQeueName);
			return false;
		}
		
		// Add remote VPN to the bridge
		if (!createRemoteVpn(localContext, remoteContext.getSecureSmfUrl(), remoteContext.getVpnName(), bridgeName, bridgeQeueName, true))
		{
			if (rollback)
			{
				configApi.deleteMsgVpnBridge(localContext.getVpnName(), bridgeName, "auto");
				deleteQueue(localContext.getVpnName(), bridgeQeueName);
			}
			return false;
		}
		
		// Add default TCN to the bridge
		if (!addBridgeTCN(localContext.getVpnName(), bridgeName, defaultSolaceTCN))
		{
			if (rollback)
			{
				configApi.deleteMsgVpnBridge(localContext.getVpnName(), bridgeName, "auto");
				deleteQueue(localContext.getVpnName(), bridgeQeueName);
			}
			return false;
		}

		// Add remote TCN to the bridge
		if (cert)
			if (!addBridgeTCN(localContext.getVpnName(), bridgeName, remoteTcn))
			{
				if (rollback)
				{
					configApi.deleteMsgVpnBridge(localContext.getVpnName(), bridgeName, "auto");
					deleteQueue(localContext.getVpnName(), bridgeQeueName);
				}
				return false;
			}

		// Enable bridge
		if (!enableLocalBridge(localContext, bridgeName))
		{
			if (rollback)
			{
				configApi.deleteMsgVpnBridge(localContext.getVpnName(), bridgeName, "auto");
				deleteQueue(localContext.getVpnName(), bridgeQeueName);
			}
			return false;
		}
				
		// Add bridge subscriptions	
		if (localDirectSubscriptions != null)
		{
			for (Subscription subscription : localDirectSubscriptions)
			{
 				if (!applyBridgeSubscription(localContext, bridgeName, subscription))
					return false;					
			}
			for (Subscription subscription : localGuaranteedSubscriptions)
			{
 				if (!applyQueueSubscription(localContext, bridgeQeueName, subscription))
					return false;					
			}
		}
		try // add remote queue, bridge, remote bridge and subscriptions to create the bidirectional bridge
		{
			setVpnContext(remoteContext);
			if (!addBridgeQueue(remoteContext, bridgeQeueName, ignoreExists))
			{
				if (rollback)
				{
					configApi.deleteMsgVpnBridge(localContext.getVpnName(), bridgeName, "auto");
					deleteQueue(localContext.getVpnName(), bridgeQeueName);
				}
				return false;
			}
			
			// Change request properties for the remote bridge to use the local bridge user.
			if (cert)
			{
				request.setRemoteAuthenticationClientCertContent(localUsername);
				request.setRemoteAuthenticationClientCertPassword(localPassword);
			}
			else
			{
				request.setRemoteAuthenticationBasicClientUsername(localUsername);
				request.setRemoteAuthenticationBasicPassword(localPassword);
			}
			
			if (!createLocalBridge(remoteContext, request))
			{
				if (rollback)
				{
					deleteQueue(remoteContext.getVpnName(), bridgeQeueName);
					configApi.deleteMsgVpnBridge(localContext.getVpnName(), bridgeName, "auto");
					deleteQueue(localContext.getVpnName(), bridgeQeueName);
				}
				return false;
			}
			String remoteMsgVpnLocation = "v:" + localContext.getPrimaryRouterName();
			if (!createRemoteVpn(remoteContext, remoteMsgVpnLocation, localContext.getVpnName(), bridgeName, bridgeQeueName, true))
			{
				if (rollback)
				{
					deleteQueue(remoteContext.getVpnName(), bridgeQeueName);
					deleteRemoteBridge(remoteService, bridgeName, "auto", bridgeQeueName);
					configApi.deleteMsgVpnBridge(localContext.getVpnName(), bridgeName, "auto");
					deleteQueue(localContext.getVpnName(), bridgeQeueName);
				}
				return false;
			}

			// Add TCMs to the bridge
			if (!addBridgeTCN(remoteContext.getVpnName(), bridgeName, defaultSolaceTCN))
			{
				if (rollback)
				{
					deleteQueue(remoteContext.getVpnName(), bridgeQeueName);
					deleteRemoteBridge(remoteService, bridgeName, "auto", bridgeQeueName);
					configApi.deleteMsgVpnBridge(localContext.getVpnName(), bridgeName, "auto");
					deleteQueue(localContext.getVpnName(), bridgeQeueName);
				}
				return false;
			}

			if (cert)
				// Add TCMs to the bridge
				if (!addBridgeTCN(remoteContext.getVpnName(), bridgeName, localTcn))
				{
					if (rollback)
					{
						deleteQueue(remoteContext.getVpnName(), bridgeQeueName);
						deleteRemoteBridge(remoteService, bridgeName, "auto", bridgeQeueName);
						configApi.deleteMsgVpnBridge(localContext.getVpnName(), bridgeName, "auto");
						deleteQueue(localContext.getVpnName(), bridgeQeueName);
					}
					return false;
				}

			// Enable bridge
			if (!enableLocalBridge(remoteContext, bridgeName))
			{
				if (rollback)
				{
					deleteQueue(remoteContext.getVpnName(), bridgeQeueName);
					deleteRemoteBridge(remoteService, bridgeName, "auto", bridgeQeueName);
					configApi.deleteMsgVpnBridge(localContext.getVpnName(), bridgeName, "auto");
					deleteQueue(localContext.getVpnName(), bridgeQeueName);
				}
				return false;
			}

			if (remoteDirectSubscriptions != null)
			{
				for (Subscription subscription : remoteDirectSubscriptions)
				{
	 				if (!applyBridgeSubscription(remoteContext, bridgeName, subscription))
						return false;					
				}
				for (Subscription subscription : remoteGuaranteedSubscriptions)
				{
	 				if (!applyQueueSubscription(remoteContext, bridgeQeueName, subscription))
						return false;					
				}
			}
			result = true;
		}
		finally
		{
			setDefaultVpnContext();
		}
		
		return result;
	}

	/**
	 * Adds a TCM for a specified bridge. This is until version 9.6 only, From version 9.7 Server Certificate Name validation replaces the TCM:
	 * https://docs.solace.com/Configuring-and-Managing/Configuring-Server-Cert-Validation.htm
	 * @param vpnName the VPN name.
	 * @param bridgeName the bridge name
	 * @param tcm the TCM
	 * @return true if successful
	 * @throws ApiException
	 */
	public boolean addBridgeTCN(String vpnName, String bridgeName, String tcm) throws ApiException
	{
		MsgVpnBridgeTlsTrustedCommonName body = new MsgVpnBridgeTlsTrustedCommonName();
		body.setTlsTrustedCommonName(tcm);
		//body.setBridgeName(bridgeName);
		//body.setMsgVpnName(vpnName);
		
		MsgVpnBridgeTlsTrustedCommonNameResponse response = configApi.createMsgVpnBridgeTlsTrustedCommonName(body, vpnName, bridgeName, "auto", opaquePassword, null);
	
		return (response.getMeta().getResponseCode() == 200);
	}
		
	/**
	 * Create remote VPN for a bridge.
	 * @param context
	 * @param remoteMsgVpnLocation
	 * @param remoteVpnName
	 * @param bridgeName
	 * @param queueName
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	private boolean createRemoteVpn(ServiceManagementContext context, String remoteMsgVpnLocation, String remoteVpnName, String bridgeName, String queueName, boolean tlsEnabled) throws ApiException
	{
		MsgVpnBridgeRemoteMsgVpn request = new MsgVpnBridgeRemoteMsgVpn();
		request.setBridgeName(bridgeName);
		request.setBridgeVirtualRouter(MsgVpnBridgeRemoteMsgVpn.BridgeVirtualRouterEnum.AUTO);
		request.setEnabled(true);
		request.setMsgVpnName(context.getVpnName());
		request.setClientUsername("");
		request.setPassword(context.getUserPassword());
		request.setQueueBinding(queueName);
		request.setRemoteMsgVpnLocation(remoteMsgVpnLocation);
		request.setCompressedDataEnabled(false);
		request.setEgressFlowWindowSize(bridgeEgressWorkflowSize);
		request.setRemoteMsgVpnName(remoteVpnName);
		request.setTlsEnabled(tlsEnabled);
		request.setUnidirectionalClientProfile(unidirectionalClientProfile);
		
		//MsgVpnBridgeRemoteMsgVpnResponse response = configApi.createMsgVpnBridgeRemoteMsgVpn(request, context.getVpnName(), bridgeName, "auto", opaquePassword, null);
		MsgVpnBridgeRemoteMsgVpnResponse response = configApi.createMsgVpnBridgeRemoteMsgVpn(request, context.getVpnName(), bridgeName, "auto", null, null);
		return (response.getMeta().getResponseCode() == 200);
	}

	/**
	 * Enables a local bridge.
	 * @param request
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	private boolean enableLocalBridge(ServiceManagementContext context, String bridgeName) throws ApiException
	{
		MsgVpnBridge request = new MsgVpnBridge();
		request.setEnabled(true);
		
		MsgVpnBridgeResponse response = configApi.updateMsgVpnBridge(request, context.getVpnName(), bridgeName, "auto", opaquePassword, null);
		return (response.getMeta().getResponseCode() == 200);
	}

	
	/**
	 * Creates a local bridge.
	 * @param request
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	private boolean createLocalBridge(ServiceManagementContext context, MsgVpnBridge request) throws ApiException
	{
		//MsgVpnBridgeResponse response = configApi.createMsgVpnBridge(request, context.getVpnName(), opaquePassword, null);
		MsgVpnBridgeResponse response = configApi.createMsgVpnBridge(request, context.getVpnName(), null, null);
		return (response.getMeta().getResponseCode() == 200);
	}

	/**
	 * Adds a bridge queue. 
	 * @param queueName
	 * @throws ApiException 
	 */
	private boolean addBridgeQueue(ServiceManagementContext context, String queueName, boolean ignoreExists) throws ApiException
	{
		MsgVpnQueue queueRequest = new MsgVpnQueue();
		queueRequest.setQueueName(queueName);
		queueRequest.setAccessType(MsgVpnQueue.AccessTypeEnum.EXCLUSIVE);
		queueRequest.setMsgVpnName(context.getVpnName());
		queueRequest.setOwner("solace-cloud-client");
		queueRequest.setPermission(MsgVpnQueue.PermissionEnum.CONSUME);
		queueRequest.setIngressEnabled(true);
		queueRequest.setEgressEnabled(true);
	
		return addQueue(context, queueRequest, ignoreExists);
	}

	/**
	 * Adds a queue. 
	 * @return True if successful, otherwise false.
	 * @throws ApiException 
	 */
	public boolean addQueue(MsgVpnQueue queueRequest) throws ApiException
	{
		return addQueue(localContext, queueRequest, false);
	}

	/**
	 * Adds a client user name. 
	 * @return True if successful, otherwise false.
	 * @throws ApiException 
	 */
	public boolean addClientUsername(MsgVpnClientUsername request) throws ApiException
	{
		boolean result = false;
		try
		{
			MsgVpnClientUsernameResponse response = configApi.createMsgVpnClientUsername(request, localContext.getVpnName(), null, null);
			result = (response.getMeta().getResponseCode() == 200);
		}
		catch(ApiException ex)
		{
			if (ex.getResponseBody().contains("alrady exists"))
				result = true;
		}
		
		return result;
	}

	/**
	 * Adds a bridge queue. 
	 * @param context
	 * @return True if successful, otherwise false.
	 * @throws ApiException 
	 */
	public boolean addQueue(ServiceManagementContext context, MsgVpnQueue queueRequest) throws ApiException
	{
		return addQueue(context, queueRequest, false);
	}
	
	/**
	 * Gets queue details.
	 * @param queueName the queue name.
	 * @return 
	 */
	public MsgVpnQueue getQueue(String queueName) throws ApiException
	{
		return configApi.getMsgVpnQueue(localContext.getVpnName(), queueName, opaquePassword, null).getData();
	}

	/**
	 * Gets queue details.
	 * @param queueName the queue name.
	 * @return 
	 */
	public com.solace.psg.sempv2.monitor.model.MsgVpnQueue getQueueStats(String queueName) throws ApiException
	{
		com.solace.psg.sempv2.monitor.model.MsgVpnQueue result = null;
		
		setApiContext(ApiContext.MONITOR);
		result =  monitorApi.getMsgVpnQueue(localContext.getVpnName(), queueName, null).getData();
		setDefaultApiContext();
		
		return result;
	}

	/**
	 * Adds a bridge queue. 
	 * @param context
	 * @param ignoreExists
	 * @return True if successful, otherwise false.
	 * @throws ApiException 
	 */
	public boolean addQueue(ServiceManagementContext context, MsgVpnQueue queueRequest, boolean ignoreExists) throws ApiException
	{	
		boolean result = false;
		try
		{
			MsgVpnQueueResponse response = configApi.createMsgVpnQueue(queueRequest, context.getVpnName(), opaquePassword, null);
			result = (response.getMeta().getResponseCode() == 200);
		}
		catch(ApiException ex)
		{
			if (ex.getResponseBody().contains("already exists") && ignoreExists)
				result = true;
		}
		
		return result;
	}
	
	/**
	 * Lists (the first (n) set by value) the queues of the localContext service.
	 * @return the queue list
	 * @throws ApiException
	 */
	public List<MsgVpnQueue> listQueues() throws ApiException
	{
		MsgVpnQueuesResponse response = configApi.getMsgVpnQueues(localContext.getVpnName(), defaultQueueCount, null, opaquePassword, null, null);
		return response.getData();
	}
	
	/**
	 * Applies a subscription to a service.
	 * @param subscription
	 * @return True if successful, otherwise false.
	 * @throws ApiException 
	 */
	public boolean applyBridgeSubscription(ServiceManagementContext context, String bridgeName, Subscription subscription) throws ApiException
	{
		MsgVpnBridgeRemoteSubscription request = new MsgVpnBridgeRemoteSubscription();
		if (subscription.getSubscriptionType().equals(SubscriptionType.DirectDeliverAlways))
			request.setDeliverAlwaysEnabled(true);
		else
			request.setDeliverAlwaysEnabled(false);
		request.setBridgeName(bridgeName);
		request.setMsgVpnName(context.getVpnName());
		request.setRemoteSubscriptionTopic(subscription.getName());
		
		MsgVpnBridgeRemoteSubscriptionResponse response = configApi.createMsgVpnBridgeRemoteSubscription(request, context.getVpnName(), bridgeName, "auto", opaquePassword, null);
		
		return (response.getMeta().getResponseCode() == 200);
	}

	/**
	 * Applies a subscription to a queue.
	 * @param subscription
	 * @return True if successful, otherwise false.
	 * @throws ApiException 
	 */
	public boolean applyQueueSubscription(ServiceManagementContext context, String queueName, Subscription subscription) throws ApiException
	{
		MsgVpnQueueSubscription request = new MsgVpnQueueSubscription();
		request.setMsgVpnName(context.getVpnName());
		request.setQueueName(queueName);
		request.setSubscriptionTopic(subscription.getName());
		
		MsgVpnQueueSubscriptionResponse response = configApi.createMsgVpnQueueSubscription(request, context.getVpnName(), queueName, opaquePassword, null);
		
		return (response.getMeta().getResponseCode() == 200);
	}
	
	/**
	 * Lists all CAs.
	 * @param sercviceId
	 * @return List of CAs
	 * @throws ApiException
	 */
	public List<String> listCAs()  throws ApiException
	{
		return localService.getCertificateAuthorities();
	}
	
	/**
	 * Creates a generic JSON call for ApiClient.
	 * @param verb GET/POST, etc.
	 * @param subPath "/subPath" or "" if no subpath
	 * @param jsonRequest Object or null if no request
	 * @param auth Object of interface Authentication
	 * @return Call
	 * @throws ApiException
	 */
	private Call getJsonCall(ApiClient apiClient, String verb, String subPath, Object jsonRequest, Authentication auth) throws ApiException
	{
		Object localVarPostBody = jsonRequest;

		List<Pair> localVarQueryParams = new ArrayList<Pair>();
		List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();

		Map<String, String> localVarHeaderParams = new HashMap<String, String>();

		Map<String, Object> localVarFormParams = new HashMap<String, Object>();

		final String[] localVarAccepts =
		{ "application/json" };
		final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
		if (localVarAccept != null)
			localVarHeaderParams.put("Accept", localVarAccept);

		final String[] localVarContentTypes =
		{ "application/json" };
		final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
		localVarHeaderParams.put("Content-Type", localVarContentType);

		String[] localVarAuthNames = new String[]
		{ auth.getAuthType() };
		return apiClient.buildCall(subPath, verb, localVarQueryParams, localVarCollectionQueryParams, localVarPostBody,
				localVarHeaderParams, localVarFormParams, localVarAuthNames, null);		
	}
	
	
	/**
	 * Deletes a bidirectional bridge to a remote service.
	 * @param remoteService
	 * @param bidirectional 
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	public boolean deleteBridge(ServiceDetails remoteService, String bridgeName, String virtualRouterName, boolean bidirectional) throws ApiException
	{
		return deleteBridge(remoteService, bridgeName, virtualRouterName, bidirectional, null);
	}
	
	/**
	 * Deletes a bidirectional bridge to a remote service.
	 * @param remoteService
	 * @param bidirectional 
	 * @param bridgeQueue queue to be deleted
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	public boolean deleteBridge(ServiceDetails remoteService, String bridgeName, String virtualRouterName, boolean bidirectional, String bridgeQueue) throws ApiException
	{
		boolean result = false;
		
		SempMetaOnlyResponse response = configApi.deleteMsgVpnBridge(localService.getMsgVpnAttributes().getVpnName(), bridgeName, virtualRouterName);
		if (response.getMeta().getResponseCode() == 200)
		{
			if (bidirectional)
			{
				result = deleteRemoteBridge(remoteService, bridgeName, virtualRouterName, bridgeQueue);
			}
			if (bridgeQueue != null && (!bridgeQueue.equals("")))
			{
				result = deleteQueue(localContext.getVpnName(), bridgeQueue);
			}
			else
			{
				result = true;
			}
		}
		
		return result;
	}
	
	/**
	 * Deletes a queue.
	 * @param vpnName The VPN
	 * @param queueName Queue name to be deleted.
	 * @return true if successful, otherwise false.
	 * @throws ApiException
	 */
	public boolean deleteQueue(String vpnName, String queueName) throws ApiException
	{
		SempMetaOnlyResponse response = configApi.deleteMsgVpnQueue(vpnName, queueName);
		return (response.getMeta().getResponseCode() == 200);
	}

	/**
	 * Deletes a queue.
	 * @param queueName Queue name to be deleted.
	 * @return true if successful, otherwise false.
	 * @throws ApiException
	 */
	public boolean deleteQueue(String queueName) throws ApiException
	{
		SempMetaOnlyResponse response = configApi.deleteMsgVpnQueue(localContext.getVpnName(), queueName);
		return (response.getMeta().getResponseCode() == 200);
	}

	/**
	 * Deletes a remote service bridge.
	 * @param remoteService
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	public boolean deleteRemoteBridge(ServiceDetails remoteService, String bridgeName, String virtualRouterName) throws ApiException
	{
		return deleteRemoteBridge(remoteService, bridgeName, virtualRouterName, null);
	}
	
	/**
	 * Deletes a remote service bridge.
	 * @param remoteService
	 * @param bidirectional 
	 * @param bridgeQueue queue to be deleted
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	private boolean deleteRemoteBridge(ServiceDetails remoteService, String bridgeName, String virtualRouterName, String bridgeQueue) throws ApiException
	{
		boolean result = false;
	
		try
		{
			ServiceManagementContext remoteContext = new ServiceManagementContext(remoteService);
			setVpnContext(remoteContext);
		
			SempMetaOnlyResponse response = configApi.deleteMsgVpnBridge(remoteService.getMsgVpnAttributes().getVpnName(), bridgeName, virtualRouterName);
			if (response.getMeta().getResponseCode() == 200)
				result = true;	
			if (bridgeQueue != null && (!bridgeQueue.equals("")))
				result = deleteQueue(remoteContext.getVpnName(), bridgeQueue);
		}
		finally
		{
			setDefaultVpnContext();
		}
		
		return result;
	}

	/**
	 * Deletes a bidirectional bridge to a remote service.
	 * @param remoteService
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	public boolean deleteBridge(ServiceDetails remoteService) throws ApiException
	{
		if (remoteService == null)
			throw new IllegalArgumentException("Argument remoteService cannot be null.");
		
		String bridgeName = getBridgeName(remoteService.getMsgVpnAttributes().getVpnName());
		String queueName = getBridgeQueueName(remoteService.getMsgVpnAttributes().getVpnName());
		
		return deleteBridge(remoteService, bridgeName, "auto", true, queueName);
	}

	/**
	 * Deletes a bidirectional bridge to a remote service.
	 * @param remoteService
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	public boolean deleteBridge(ServiceDetails remoteService, String bridgeName, String bridgeQueue) throws ApiException
	{
		return deleteBridge(remoteService, bridgeName, "auto", true, bridgeQueue);
	}
	
	/**
	 * Deletes a bidirectional bridge to a remote service.
	 * @param remoteService
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	public boolean deleteBridge(ServiceDetails remoteService, String bridgeName, boolean bidirectional, String bridgeQueue) throws ApiException
	{
		return deleteBridge(remoteService, bridgeName, "auto", bidirectional, bridgeQueue);
	}
	
	/**
	 * Gets bridges for the VPN (Service).
	 * @return
	 * @throws ApiException 
	 */
	public List<MsgVpnBridge> getBridges() throws ApiException
	{
		// get msgVpnName instead as currently they can be different.
		MsgVpnBridgesResponse response = configApi.getMsgVpnBridges(localService.getMsgVpnAttributes().getVpnName(), defaultBridgeCount, null, opaquePassword, null, null);
		return response.getData();
	}
	
	/**
	 * Gets a bridge name from pattern.
	 * @param remoteVpnName
	 * @return Bridge name.
	 */
	private String getBridgeName(String remoteVpnName)
	{
		return bridgePattern.replace("<LocalVpnName>", localService.getName()).replace("<RemoteVpnName>", remoteVpnName);
	}
	
	/**
	 * Gets VpnName from bridhe4 name pattern <localName>_<remoteName>.
	 * @param bridgeName
	 * @param localVpnName
	 * @return Bridge name.
	 */
	public String getRemoteVpnNameFromBridge(String bridgeName, String localVpnName)
	{
		String result = null;
		//if (!bridgeName.contains("_"))
		//	throw new IllegalArgumentException("BridgeName must have thye pattern <localName>_<remoteName>");
		String[] vpns = bridgeName.split("_");
		if (vpns != null  && vpns.length == 2)
		{
			if (vpns[0].equals(localVpnName))
				result = vpns[1];
			else if (vpns[1].equals(localVpnName))
				result = vpns[0];
		}
		
		return result;
	}
	/**
	 * Gets a bridge queue name from pattern.
	 * @param remoteVpnName
	 * @return Bridge queue name.
	 */
	private String getBridgeQueueName(String remoteVpnName)
	{
		return bridgeQueuePattern.replace("<LocalVpnName>", localService.getName()).replace("<RemoteVpnName>", remoteVpnName);
	}
}
