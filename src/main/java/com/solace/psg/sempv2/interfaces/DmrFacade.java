/**
 * 
 */
package com.solace.psg.sempv2.interfaces;

import java.util.List;

import com.solace.psg.sempv2.admin.model.ServiceDetails;
import com.solace.psg.sempv2.admin.model.ServiceManagementContext;
import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.config.api.DmrBridgeApi;
import com.solace.psg.sempv2.config.api.DmrClusterApi;
import com.solace.psg.sempv2.config.model.DmrCluster;
import com.solace.psg.sempv2.config.model.DmrClusterLink;
import com.solace.psg.sempv2.config.model.DmrClusterLink.AuthenticationSchemeEnum;
import com.solace.psg.sempv2.config.model.DmrClusterLink.InitiatorEnum;
import com.solace.psg.sempv2.config.model.DmrClusterLinkRemoteAddress;
import com.solace.psg.sempv2.config.model.DmrClusterLinkRemoteAddressResponse;
import com.solace.psg.sempv2.config.model.DmrClusterLinkResponse;
import com.solace.psg.sempv2.config.model.DmrClusterResponse;
import com.solace.psg.sempv2.config.model.DmrClustersResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDmrBridge;
import com.solace.psg.sempv2.config.model.MsgVpnDmrBridgeResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDmrBridgesResponse;
import com.solace.psg.sempv2.config.model.SempMetaOnlyResponse;

/**
 * Class to handle API calls for DMR cluster and Bridges.
 * @author VictorTsonkov
 *
 */
public class DmrFacade
{
	/**
	 * Management context of the local vpn.
	 */
	private ServiceManagementContext localContext;
	
	private ServiceManagementContext remoteContext;
	
	private int getClustersCount = 10;
	private int getBridgesCount = 10;
	
	private DmrBridgeApi dmrBridgeApi;
	private DmrClusterApi dmrClusterApi;
	
	//private String localNodeName;
	//private String remoteNodeName;
	
	/**
	 * Initialises a new instance of the class.
	 * @throws ApiException 
	 */
	public DmrFacade(ServiceDetails localService, ServiceDetails remoteService) throws ApiException
	{
		this(new ServiceManagementContext(localService), new ServiceManagementContext(remoteService));
	}
	
	/**
	 * Initialises a new instance of the class.
	 * @throws ApiException 
	 */
	public DmrFacade(ServiceManagementContext localContext, ServiceManagementContext remoteContext) throws ApiException
	{
		dmrBridgeApi = new DmrBridgeApi();
		dmrClusterApi = new DmrClusterApi();
		this.localContext = localContext;
		this.remoteContext = remoteContext;
		setVpnContext(localContext);
		//initNodeNames();
	}
	
	/**
	private void initNodeNames() throws ApiException
	{
		localNodeName = getDMRNodeName();
		try
		{
			setVpnContext(remoteContext);
			remoteNodeName = getDMRNodeName();
		}
		finally
		{
			setVpnContext(localContext);
		}
	}*/

	/**
	 * Sets the context of the local VPN details for the API Client.
	 */
	public void setVpnContext(ServiceManagementContext context)
	{
		dmrBridgeApi.getApiClient().setBasePath(context.getSempUrl());
		dmrBridgeApi.getApiClient().setUsername(context.getSempUsername());
		dmrBridgeApi.getApiClient().setPassword(context.getSempPassword());
		
		dmrClusterApi.getApiClient().setBasePath(context.getSempUrl());
		dmrClusterApi.getApiClient().setUsername(context.getSempUsername());
		dmrClusterApi.getApiClient().setPassword(context.getSempPassword());
	}
	
	/**
	 * Gets DMR cluster data for a name.
	 * 
	 * @param dmrClusterName
	 * @return DmrCluster
	 * @throws ApiException
	 */
	public DmrCluster getDMRCluster(String dmrClusterName) throws ApiException
	{
		DmrClusterResponse response = dmrClusterApi.getDmrCluster(dmrClusterName, null);
		return response.getData();
	}
	
	/**
	 * Gets DMR clusters.
	 * 
	 * @return List<DmrCluster>
	 * @throws ApiException
	 */
	public List<DmrCluster> getDMRClusters() throws ApiException
	{
		DmrClustersResponse response = dmrClusterApi.getDmrClusters(getClustersCount, null, null, null);
		return response.getData();
	}
	
	/**
	 * Gets Node name of from the first cluster in the list.
	 * 
	 * @return node name
	 * @throws ApiException
	 */
	public String getDMRNodeName() throws ApiException
	{
		String result = null;
		List<DmrCluster> resp = getDMRClusters();
		if (resp != null && resp.size() > 0 )
		{
			result = resp.get(0).getNodeName();
		}
		
		return result;
	}
	
	/**
	 * Creates a new DmrCluster.
	 * @param dmrClusterName
	 * @return DmrCluster
	 * @throws ApiException
	 */
	public DmrCluster createDMRCluster(String dmrClusterName, String clusterPassword) throws ApiException
	{
		DmrCluster request = new DmrCluster();
		request.setAuthenticationBasicEnabled(true);
		request.setDmrClusterName(dmrClusterName);
		request.setAuthenticationBasicPassword(clusterPassword);
		DmrClusterResponse response = dmrClusterApi.createDmrCluster(request, null);
		return response.getData();
	}
	
	/**
	 * Creates a new DmrCluster link.
	 * @param dmrClusterName
	 * @param remoteClusterPassword
	 * @param remoteNodeName
	 * @return DmrCluster
	 * @throws ApiException
	 */
	public DmrClusterLink createDMRClusterLink(String dmrClusterName, String remoteClusterPassword, String remoteNodeName, InitiatorEnum initiator, boolean enabled) throws ApiException
	{
		DmrClusterLink request = new DmrClusterLink();
		request.authenticationBasicPassword(remoteClusterPassword);
		request.setAuthenticationScheme(AuthenticationSchemeEnum.BASIC);
		request.setRemoteNodeName(remoteNodeName);
		request.enabled(enabled);
		request.setDmrClusterName(dmrClusterName);
		request.setInitiator(initiator);
		
		DmrClusterLinkResponse response = dmrClusterApi.createDmrClusterLink(dmrClusterName, request, null);
		return response.getData();
	}

	/**
	 * Deletes a new DmrCluster link.
	 * @param dmrClusterName
	 * @param remoteNodeName
	 * @return true if success
	 * @throws ApiException
	 */
	public boolean deleteDMRClusterLink(String dmrClusterName, String remoteNodeName) throws ApiException
	{	
		SempMetaOnlyResponse response = dmrClusterApi.deleteDmrClusterLink(dmrClusterName, remoteNodeName);
		return (response.getMeta().getResponseCode() == 200);
	}

	/**
	 * Enables a created DMR Cluster Link.
	 * @param dmrClusterName
	 * @param remoteNodeName
	 * @param request
	 * @return
	 * @throws ApiException
	 */
	private DmrClusterLink enableDMRClusterLink(String dmrClusterName, String remoteNodeName, DmrClusterLink request) throws ApiException
	{	
		request.setEnabled(true);
		DmrClusterLinkResponse response = dmrClusterApi.updateDmrClusterLink(dmrClusterName, remoteNodeName, request, null);
		return response.getData();
	}
	
	/**
	 * Create a Bidirectional Link for a cluster.
	 * @return true if success
	 * @throws ApiException
	 */
	public boolean createBiDmrClusterLink() throws ApiException
	{
		boolean result = false;
		
		try
		{
			DmrClusterLink resp =  createDMRClusterLink(localContext.getClusterName(), remoteContext.getClusterPassword(), remoteContext.getNodeName(), InitiatorEnum.LOCAL, false);
			if (resp == null)
				return false;
			DmrClusterLinkRemoteAddressResponse  resp2 = addDMRClusterLinkRemoteAddress(localContext.getClusterName(), remoteContext.getNodeName(), remoteContext.getSmfUrlHost());
			if (resp2 == null)
				return false;
			resp = enableDMRClusterLink(localContext.getClusterName(), remoteContext.getNodeName(), resp);
			if (resp == null)
				return false;
			
			setVpnContext(remoteContext);
			resp =  createDMRClusterLink(remoteContext.getClusterName(), localContext.getClusterPassword(), localContext.getNodeName(), InitiatorEnum.REMOTE, true);
			if (resp != null)
				result =  true;
		}
		finally
		{
			setVpnContext(localContext);
		}
		
		return result;
	}	
	
	/**
	 * Deletes a Bidirectional Link for a cluster.
	 * @return true if success
	 * @throws ApiException
	 */
	public boolean deleteBiDmrClusterLink() throws ApiException
	{
		boolean result = false;
		
		try
		{
			boolean resp =  deleteDMRClusterLink(localContext.getClusterName(), remoteContext.getNodeName());
			if (resp != true)
				return false;
			
			setVpnContext(remoteContext);
			result =  deleteDMRClusterLink(remoteContext.getClusterName(), localContext.getNodeName());

		}
		finally
		{
			setVpnContext(localContext);
		}
		
		return result;
	}	

	
	/**
	 * Adds a remote address to a cluster link. 
	 * @param dmrClusterName
	 * @param remoteNodeName
	 * @param remoteAddress
	 * @return
	 * @throws ApiException
	 */
	public DmrClusterLinkRemoteAddressResponse addDMRClusterLinkRemoteAddress(String dmrClusterName, String remoteNodeName, String remoteAddress) throws ApiException
	{
		DmrClusterLinkRemoteAddress request = new DmrClusterLinkRemoteAddress();
		request.setDmrClusterName(dmrClusterName);
		request.setRemoteAddress(remoteAddress);
		request.setRemoteNodeName(remoteNodeName);
		return dmrClusterApi.createDmrClusterLinkRemoteAddress(dmrClusterName, remoteNodeName, request, null);
	}

	/**
	 * Creates a BiDirectional Cluster with Links and Bridges between local and remote services.
	 * @return true if success
	 */
	public boolean createBiDMRCluster() throws ApiException
	{
		boolean result = false;
		
		// Create cluster if if doesn't exist.
	    // Currently services come with Cluster pre-created.	
		
		// Create BiDMRLink.
		result = createBiDmrClusterLink();

		// Create BiDMR Bridges.
		if (result)
			result = createBiDmrBridge();
		
		return result;
	}
	
	
	/**
	 * Deletes a BiDirectional Cluster with Links and Bridges between local and remote services.
	 * @return true if success
	 */
	public boolean deleteBiDMRCluster() throws ApiException
	{
		boolean result = false;
		
		// Delete cluster if if doesn't exist.
		// Currently services come with Cluster pre-created so no need to delete it.
		
		// Delete BiDMR Bridges.
		result = deleteBiDmrBridge();

		// Delete BiDMRLink.
		if (result)
			result = deleteBiDmrClusterLink();
		
		return result;
	}
	
	/**
	 * Deletes a DMR cluster.
	 * @param dmrClusterName
	 * @return True if successful
	 * @throws ApiException
	 */
	public boolean deleteDMRCluster(String dmrClusterName) throws ApiException
	{
		SempMetaOnlyResponse response = dmrClusterApi.deleteDmrCluster(dmrClusterName);
		return (response.getMeta().getResponseCode() == 200);
	}

	/**
	 * Create a Bidirectional DMR Bridge between local and remote context.
	 * @return true if success
	 * @throws ApiException
	 */
	public boolean createBiDmrBridge() throws ApiException
	{
		boolean result = false;
		
		try
		{
			MsgVpnDmrBridge resp =  createDmrBridge(localContext.getVpnName(), remoteContext.getVpnName(), remoteContext.getNodeName());
			if (resp == null)
				return false;
			setVpnContext(remoteContext);
			resp =  createDmrBridge(remoteContext.getVpnName(), localContext.getVpnName(), localContext.getNodeName());
			if (resp != null)
				result =  true;
		}
		finally
		{
			setVpnContext(localContext);
		}
		
		return result;
	}	
	
	/**
	 * Create a DMR Bridge with a remote VPN.
	 * @param remoteMsgVpnName
	 * @param remoteNodeName
	 * @return
	 * @throws ApiException
	 */
	public MsgVpnDmrBridge createDmrBridge(String localVpnName, String remoteMsgVpnName, String remoteNodeName) throws ApiException
	{
		MsgVpnDmrBridge request = new MsgVpnDmrBridge();
		request.setMsgVpnName(localVpnName);
		request.setRemoteMsgVpnName(remoteMsgVpnName);
		request.setRemoteNodeName(remoteNodeName);
		MsgVpnDmrBridgeResponse response = dmrBridgeApi.createMsgVpnDmrBridge(localVpnName, request, null);
		return response.getData();
	}	
	
	/**
	 * Gets the DMR Bridges. 
	 * 
	 * @return
	 * @throws ApiException
	 */
	public List<MsgVpnDmrBridge> getDmrBridges() throws ApiException
	{
		MsgVpnDmrBridgesResponse response = dmrBridgeApi.getMsgVpnDmrBridges(localContext.getVpnName(), getBridgesCount, null, null, null);

		return response.getData();
	}

	/**
	 * Deletes a DMR bridge.
	 * @return
	 * @throws ApiException
	 */
	public boolean deleteBiDmrBridge() throws ApiException
	{
		boolean result = false;
		SempMetaOnlyResponse response = dmrBridgeApi.deleteMsgVpnDmrBridge(localContext.getVpnName(), remoteContext.getNodeName());
		result = (response.getMeta().getResponseCode() == 200);
		if (result == false)
			return result;
		try
		{
			setVpnContext(remoteContext);
			response = dmrBridgeApi.deleteMsgVpnDmrBridge(remoteContext.getVpnName(), localContext.getNodeName());
			result = (response.getMeta().getResponseCode() == 200);
		}
		finally
		{
			setVpnContext(localContext);
		}
		
		return result;
	}
	
	/**
	 * Deletes a DMR bridge.
	 * @param remoteNodeName
	 * @return
	 * @throws ApiException
	 */
	public boolean deleteDmrBridge(String remoteNodeName) throws ApiException
	{
		SempMetaOnlyResponse response = dmrBridgeApi.deleteMsgVpnDmrBridge(localContext.getVpnName(), remoteNodeName);
		return (response.getMeta().getResponseCode() == 200);
	}
}
