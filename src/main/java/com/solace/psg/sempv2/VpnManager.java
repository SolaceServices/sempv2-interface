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
import com.solace.psg.sempv2.config.model.MsgVpnBridgesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnClientProfile;
import com.solace.psg.sempv2.config.model.MsgVpnClientProfileResponse;
import com.solace.psg.sempv2.config.model.MsgVpnClientProfilesResponse;
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
	
	/**
	 * The Client API.
	 */
	private MsgVpnApi api;

	/**
	 * Initialises a new instance of the class.
	 */
	public VpnManager(ServiceDetails service)
	{
		api = new MsgVpnApi();
		this.localService = service;
		this.localContext = new ServiceManagementContext(service);
		
		setDefaultVpnContext();
	}
	
	/**
	 * Sets the Client API to the VPN context.
	 */
	public void setDefaultVpnContext()
	{
		setVpnContext(this.localContext);
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
		api.getApiClient().setBasePath(context.getSempUrl());
		api.getApiClient().setUsername(context.getSempUsername());
		api.getApiClient().setPassword(context.getSempPassword());
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
		SempMetaOnlyResponse response = api.deleteMsgVpnAclProfile(localContext.getVpnName(), profileName);
		
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
		MsgVpnAclProfileResponse response = api.createMsgVpnAclProfile(localContext.getVpnName(), profile, select);
		return response.getData();
	}
	
	/**
	 * Gets ACL profiles for the VPN.
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	public List<MsgVpnAclProfile> getAclProfiles() throws ApiException
	{
		MsgVpnAclProfilesResponse response = api.getMsgVpnAclProfiles(localContext.getVpnName(), defaultAclCount, null, null, null);
		return response.getData();
	}

	/**
	 * Gets client profile for the VPN.
	 * @return the profile data.
	 * @throws ApiException
	 */
	public MsgVpnClientProfile getClientProfile(String clientProfileName) throws ApiException
	{
		MsgVpnClientProfileResponse response = api.getMsgVpnClientProfile(localContext.getVpnName(), clientProfileName, null);
		return response.getData();
	}

	/**
	 * List client profiles for the VPN.
	 * @return TList of profiles.
	 * @throws ApiException
	 */
	public List<MsgVpnClientProfile> listClientProfiles() throws ApiException
	{
		MsgVpnClientProfilesResponse response = api.getMsgVpnClientProfiles(localContext.getVpnName(), defaultAclCount,  null, null, null);
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
		
		MsgVpnDmrBridgeResponse response = api.createMsgVpnDmrBridge(localContext.getVpnName(), request, null);
		
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
		return createBridge(remoteVpnName, null, null, true);
	}
	
	/**
	 * Adds a bridge with a list of subscriptions.
	 * @param remoteService
	 * @param subscriptions
	 * @return True if successful, otherwise false.
	 * @throws ApiException 
	 */
	public boolean createBridge(ServiceDetails remoteService, List<Subscription> subscriptions) throws ApiException
	{
		String bridgeName = getBridgeName(remoteService.getMsgVpnAttributes().getVpnName());
		return createBridge(remoteService, bridgeName, subscriptions, true);
	}
	
	/**
	 * Adds a bridge with a list of subscriptions.
	 * @param remoteService
	 * @param subscriptions
	 * @return True if successful, otherwise false.
	 * @throws ApiException 
	 */
	public boolean createBridge(ServiceDetails remoteService, String bridgeName, List<Subscription> subscriptions, boolean ignoreExists) throws ApiException
	{
		boolean result = false;
		ServiceManagementContext remoteContext = new ServiceManagementContext(remoteService);
		
		if (localContext.getSmfUrl() == null || localContext.getSmfUrl() == null)
			throw new IllegalArgumentException("SMF URL is null. Bridge cannot be created. Enable SMF or create a TLS bridge.");
		
		String queueName = getBridgeQueueName(remoteContext.getVpnName());
		
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
		
		// Add local bridge queue, local bridge, remote bridge and subscriptions
		if (!addBridgeQueue(localContext, queueName, ignoreExists))
			return false;
		
		MsgVpnBridge request = new MsgVpnBridge();
		request.setBridgeName(bridgeName);
		request.setBridgeVirtualRouter(BridgeVirtualRouterEnum.AUTO);
		request.enabled(true);
		request.setRemoteAuthenticationBasicClientUsername(remoteContext.getUserUsername());
		request.setRemoteAuthenticationBasicPassword(remoteContext.getUserPassword());
		request.setRemoteAuthenticationScheme(RemoteAuthenticationSchemeEnum.BASIC);
		request.setMaxTtl(maxTtl);
		request.setRemoteConnectionRetryDelay(connectionRetryDelay);
		request.remoteConnectionRetryCount(connectionRetryCount);
		request.setRemoteDeliverToOnePriority(MsgVpnBridge.RemoteDeliverToOnePriorityEnum.P1);
		
		if (!createLocalBridge(localContext, request))
			return false;
		
		// This a workaround bit to make the bridge created but not work. In this case the bridge should be created with TLS.
		String smfUrl = remoteContext.getSmfUrl();
		/*if (smfUrl == null)
			smfUrl = remoteContext.getSecureSmfUrl();*/
		
		if (!createRemoteVpn(localContext, smfUrl, remoteContext.getVpnName(), bridgeName, queueName))
			return false;
				
		if (localDirectSubscriptions != null)
		{
			for (Subscription subscription : localDirectSubscriptions)
			{
 				if (!applyBridgeSubscription(localContext, bridgeName, subscription))
					return false;					
			}
			for (Subscription subscription : localGuaranteedSubscriptions)
			{
 				if (!applyQueueSubscription(localContext, queueName, subscription))
					return false;					
			}
		}
		try // add remote queue, bridge, remote bridge and subscriptions to create the bidirectional bridge
		{
			setVpnContext(remoteContext);
			if (!addBridgeQueue(remoteContext, queueName, ignoreExists))
				return false;
			request.setRemoteAuthenticationBasicClientUsername(localContext.getUserUsername());
			request.setRemoteAuthenticationBasicPassword(localContext.getUserPassword());
			if (!createLocalBridge(remoteContext, request))
				return false;
			String remoteMsgVpnLocation = "v:" + localContext.getPrimaryRouterName();
			if (!createRemoteVpn(remoteContext, remoteMsgVpnLocation, localContext.getVpnName(), bridgeName, queueName))
				return false;
			
			if (remoteDirectSubscriptions != null)
			{
				for (Subscription subscription : remoteDirectSubscriptions)
				{
	 				if (!applyBridgeSubscription(remoteContext, bridgeName, subscription))
						return false;					
				}
				for (Subscription subscription : remoteGuaranteedSubscriptions)
				{
	 				if (!applyQueueSubscription(remoteContext, queueName, subscription))
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
	 * Create remote VPN for a bridge.
	 * @param context
	 * @param remoteMsgVpnLocation
	 * @param remoteVpnName
	 * @param bridgeName
	 * @param queueName
	 * @return True if successful, otherwise false.
	 * @throws ApiException
	 */
	private boolean createRemoteVpn(ServiceManagementContext context, String remoteMsgVpnLocation, String remoteVpnName, String bridgeName, String queueName) throws ApiException
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
		request.setTlsEnabled(false);
		request.setUnidirectionalClientProfile(unidirectionalClientProfile);
		
		MsgVpnBridgeRemoteMsgVpnResponse response = api.createMsgVpnBridgeRemoteMsgVpn(context.getVpnName(), bridgeName, "auto", request, null);
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
		MsgVpnBridgeResponse response = api.createMsgVpnBridge(context.getVpnName(), request, null);
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
	 * Adds a bridge queue. 
	 * @return True if successful, otherwise false.
	 * @throws ApiException 
	 */
	public boolean addQueue(MsgVpnQueue queueRequest) throws ApiException
	{
		return addQueue(localContext, queueRequest, false);
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
		return api.getMsgVpnQueue(localContext.getVpnName(), queueName, null).getData();
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
			MsgVpnQueueResponse response = api.createMsgVpnQueue(context.getVpnName(), queueRequest, null);
			result = (response.getMeta().getResponseCode() == 200);
		}
		catch(ApiException ex)
		{
			if (ex.getResponseBody().contains("alrady exists") && ignoreExists)
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
		MsgVpnQueuesResponse response = api.getMsgVpnQueues(localContext.getVpnName(), defaultQueueCount, null, null, null);
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
		
		MsgVpnBridgeRemoteSubscriptionResponse response = api.createMsgVpnBridgeRemoteSubscription(context.getVpnName(), bridgeName, "auto", request, null);
		
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
		
		MsgVpnQueueSubscriptionResponse response = api.createMsgVpnQueueSubscription(context.getVpnName(), queueName, request, null);
		
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
		
		SempMetaOnlyResponse response = api.deleteMsgVpnBridge(localService.getMsgVpnAttributes().getVpnName(), bridgeName, virtualRouterName);
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
		SempMetaOnlyResponse response = api.deleteMsgVpnQueue(vpnName, queueName);
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
		SempMetaOnlyResponse response = api.deleteMsgVpnQueue(localContext.getVpnName(), queueName);
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
		
			SempMetaOnlyResponse response = api.deleteMsgVpnBridge(remoteService.getMsgVpnAttributes().getVpnName(), bridgeName, virtualRouterName);
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
		MsgVpnBridgesResponse response = api.getMsgVpnBridges(localService.getMsgVpnAttributes().getVpnName(), defaultBridgeCount, null, null, null);
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
