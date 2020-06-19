/**
 * 
 */
package com.solace.psg.sempv2.interfaces;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.solace.psg.sempv2.admin.model.ServiceDetails;
import com.solace.psg.sempv2.admin.model.Subscription;
import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.config.model.MsgVpnBridge;

/**
 * Class to handle a cluster of services.
 * @author VictorTsonkov
 *
 */
public class ClusterFacade 
{

	private List<ServiceDetails> clusterServiceList = new ArrayList<ServiceDetails>();
	
	/**
	 * @return the clusterServiceList
	 */
	public List<ServiceDetails> getClusterServiceList()
	{
		return clusterServiceList;
	}

	/**
	 * @param clusterServiceList the clusterServiceList to set
	 */
	public void setClusterServiceList(List<ServiceDetails> clusterServiceList)
	{
		this.clusterServiceList = clusterServiceList;
	}

	private ServiceFacade sf;
	//private String clusterAccessToken;

	/**
	 * Initialises a new instance of the class.
	 * @throws ApiException 
	 */
	public ClusterFacade(ServiceFacade sf, List<ServiceDetails
		> clusterServiceList) throws ApiException
	{	
		this.clusterServiceList = clusterServiceList;
		this.sf = sf;
	}
	
	/**
	 * Initialises a new instance of the class.
	 * @throws ApiException 
	 */
	public ClusterFacade(String clusterAccessToken) throws ApiException
	{		
		if (clusterAccessToken == null)
			throw new NullPointerException("Parameter clusterAccessToken cannot be null.");

		//this.clusterAccessToken = clusterAccessToken;
		this.sf = new ServiceFacade(clusterAccessToken);
		initClusterServiceList();
	}

	/**
	 * Calls GetServices and fills in all service details for the cluster.
	 * @throws ApiException 
	 */
	private void initClusterServiceList() throws ApiException
	{
		clusterServiceList = sf.getAllServiceDetails();
	}
	
	/**
	 * Gets all service details by iterating through the list of serviceData for a cluster and obtaining service details.
	 * @return List of ServiceDetails
	 * @throws ApiException 
	 */
	public List<ServiceDetails> getAllClusterServiceDetails() throws ApiException
	{		
		return clusterServiceList;
	}
	
	/**
	 * Creates a new service and iterates through the cluster list and adds a bridge between the newly added service and the rest of the services in the list.
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws ApiException 
	 */
	public ServiceDetails createClusterService(String serviceName, String serviceTypeId, String serviceClassId, String datacenterId, List<Subscription> subscriptions) throws ApiException, IOException, InterruptedException
	{	
		ServiceDetails newService = sf.createService(serviceName, serviceTypeId, serviceClassId, datacenterId);
		// For testing purposes not to recreate the service.
		//ServiceDetails newService = sf.getServiceDetails("3b4l2cu74vv");
		VpnFacade bridgeFacade = new VpnFacade(newService);
		
		// Iterate through the cluster service list and add a new bridge between the new service and the existing services.
		for (ServiceDetails targetService: clusterServiceList)
		{
			bridgeFacade.createBridge(targetService, subscriptions); 
		}
		
		// Add service to the clusterList
		clusterServiceList.add(newService);
		
		return newService;
	}

	/**
	 * Deletes a service from the cluster and removes the bridges from the cluster list.
	 * @return true if success, otherwise false
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws ApiException 
	 */
	public boolean deleteClusterService(String serviceName) throws ApiException, IOException, InterruptedException
	{
		ServiceDetails sd = sf.getServiceDetailsByName(serviceName);
		return deleteClusterService(sd);
	}

	/**
	 * Deletes a service from the cluster and removes the bridges from the cluster list.
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws ApiException 
	 
	public boolean deleteClusterService(String serviceId) throws ApiException, IOException, InterruptedException
	{	
		ServiceDetails sd = sf.getServiceDetails(serviceId);
		return deleteClusterService(sd);
	}*/
	
	/**
	 * Deletes a service from the cluster and removes the bridges from the cluster list.
	 * @return true if success, otherwise false
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws ApiException 
	 */
	public boolean deleteClusterService(ServiceDetails service) throws ApiException, IOException, InterruptedException
	{	
		VpnFacade vpnFacade = new VpnFacade(service);
		// Remove service to the clusterList
		removeServicefromClusterList(service);
		
		List<MsgVpnBridge> bridges = vpnFacade.getBridges();
		// Iterate through the cluster service list and delete the bridge between the new service and the existing services.
		for (MsgVpnBridge bridge: bridges)
		{		
			String localVpnName = bridge.getMsgVpnName();
			
			String bridgeName = bridge.getBridgeName();
			String remoteVpnName = vpnFacade.getRemoteVpnNameFromBridge(bridgeName, localVpnName);
			
			ServiceDetails sd = getServiceByVpnName(remoteVpnName);
			if (sd != null)
				vpnFacade.deleteBridge(sd); 
		}
		
		if (!sf.deleteService(service.getServiceId()))
			return false;
		
		// TODO: Potentially delete also the cluster DMR bridge for the VPN if the one. 
			
		return true;
	}
	
	/**
	 * Removed a service from the cluster list by ServiceId.
	 * @param service
	 */
	private void removeServicefromClusterList(ServiceDetails service)
	{
		for (int i = 0; i < clusterServiceList.size(); i++)
		{
			ServiceDetails sd = clusterServiceList.get(i);
			if (sd.getServiceId().equals(service.getServiceId()))
			{
				clusterServiceList.remove(i);
				break;
			}
		}
	}
	
	/**
	 * Gets ServiceDetails by VPN Name.
	 * @param vpnName
	 * @return
	 */
	private ServiceDetails getServiceByVpnName(String vpnName)
	{
		ServiceDetails result = null;
		for (ServiceDetails sd: clusterServiceList)
		{
			if (sd.getMsgVpnAttributes().getVpnName().equals(vpnName))
				result = sd;
		}
		
		return result;
	}
}
