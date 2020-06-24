/**
 * 
 */
package com.solace.psg.sempv2.admin.model;

/**
 * Class to return a small set of subelements required for management of most tasks.
 * @author VictorTsonkov
 *
 */
public class ServiceManagementContext
{
	private ServiceDetails service;
	private String sempUsername;
	private String sempPassword;
	private String userUsername;
	private String userPassword;
	private String sempUrl;
	private String sempV1Url;
	private String smfUrl;
	private String ssmfUrl;
	
	/**
	 * The TCP protocol prefix for SMF.
	 */
	public static final String SMF_PREFIX = "tcp://";
	
	/**
	 * The secure TCP protocol prefix for SMF.
	 */
	public static final String SECURE_SMF_PREFIX = "tcps://";
	
	
	/**
	 * Gets the Secure SMF String.
	 * @return the ssmfUrl
	 */
	public String getSecureSmfUrl()
	{
		return ssmfUrl;
	}

	private String clusterPassword;
	private String clusterName;
	
	/**
	 * @return the clusterPassword
	 */
	public String getClusterPassword()
	{
		return clusterPassword;
	}

	/**
	 * @return the clusterName
	 */
	public String getClusterName()
	{
		return clusterName;
	}

	/**
	 * @return the smfUrl
	 */
	public String getSmfUrl()
	{
		return smfUrl;
	}

	/**
	 * @param smfUrl the smfUrl to set
	 */
	public void setSmfUrl(String smfUrl)
	{
		this.smfUrl = smfUrl;
	}

	private String serviceName;
	private String vpnName;
	
	// Also cluster Node Name.
	private String primaryRouterName;
	
	/**
	 * Initialises a new instance of the class.
	 */
	public ServiceManagementContext(ServiceDetails service)
	{
		this.service = service;
		initContext();
	}
	
	/**
	 * Gets the SMF username.
	 * @return the userUsername
	 */
	public String getUserUsername()
	{
		return userUsername;
	}

	/**
	 * Sets SMF username.
	 * @param userUsername the userUsername to set
	 */
	public void setUserUsername(String userUsername)
	{
		this.userUsername = userUsername;
	}

	/**
	 * Gets SMF password.
	 * @return the userPassword
	 */
	public String getUserPassword()
	{
		return userPassword;
	}
	
	/**
	 * Gets the SMF URL Host.
	 * @return
	 */
	public String getSmfUrlHost()
	{
		String[] smfParts = smfUrl.split(":");
		return smfParts[0];
	}

	/**
	 * Gets the SMF URL Port.
	 * @return
	 */
	public String getSmfUrlPort()
	{
		String[] smfParts = smfUrl.split(":");
		return smfParts[1];
	}
	
	/**
	 * @param userPassword the userPassword to set
	 */
	public void setUserPassword(String userPassword)
	{
		this.userPassword = userPassword;
	}

	/**
	 * Set variables.
	 */
	private void initContext()
	{
		serviceName = service.getName();
		vpnName = service.getMsgVpnAttributes().getVpnName();
		this.primaryRouterName = service.getCluster().getPrimaryRouterName();
		this.clusterPassword = service.getCluster().getPassword();
		this.clusterName = service.getCluster().getName();
		
		for (ManagementProtocol mp : service.getManagementProtocols())
		{
			if (mp.getName().equals("SolAdmin")) // Get username and password
			{
				sempUsername = mp.getUsername();
				sempPassword = mp.getPassword();
			}
			else if (mp.getName().equalsIgnoreCase("SEMP")) // Get SEMP URL
			{
				for (EndPoint ep : mp.getEndPoints())
				{
					if (ep.getName().equalsIgnoreCase("SEMP Config"))
					{
							sempV1Url = sempUrl = ep.getUris().get(0);
							if (sempUrl.endsWith("/v2/config"))
							{
								sempV1Url = sempUrl.substring(0, sempUrl.indexOf("/v2/config"));
							}
					}
				}
			}
		}
		for (MessagingProtocol mesp : service.getMessagingProtocols())
		{
			if (mesp.getName().equals("SMF")) // Get username and password
			{
				userUsername = mesp.getUsername();
				userPassword = mesp.getPassword();
			}
			for (EndPoint ep : mesp.getEndPoints())
			{
				if (ep.getName().equals("SMF"))
				{
					smfUrl = ep.getUris().get(0).replace(SMF_PREFIX, "");
				}
				else if (ep.getName().equals("Secured SMF"))
				{
					ssmfUrl = ep.getUris().get(0).replace(SECURE_SMF_PREFIX, "");
				}
			}
		}
	}

	/**
	 * Gets primary router name. 
	 * @return the primaryRouterName
	 */
	public String getPrimaryRouterName()
	{
		return primaryRouterName;
	}
	
	/**
	 * Gets primary router name which is the node name. 
	 * @return the primaryRouterName
	 */
	public String getNodeName()
	{
		return primaryRouterName;
	}
	
	/**
	 * Sets primary router name.
	 * @param primaryRouterName the primaryRouterName to set
	 */
	public void setPrimaryRouterName(String primaryRouterName)
	{
		this.primaryRouterName = primaryRouterName;
	}

	/**
	 * Gets the service of the context.
	 * @return the service
	 */
	public ServiceDetails getService()
	{
		return service;
	}

	/**
	 * Sets the service of the context.
	 * @param service the service to set
	 */
	public void setService(ServiceDetails service)
	{
		this.service = service;
		initContext();
	}

	/**
	 * Gets the SEMP username.
	 * @return the sempUsername
	 */
	public String getSempUsername()
	{
		return sempUsername;
	}

	/**
	 * Sets the SEMP username.
	 * @param sempUsername the sempUsername to set
	 */
	public void setSempUsername(String sempUsername)
	{
		this.sempUsername = sempUsername;
	}

	/**
	 * Gets the SEMP password.
	 * @return the sempPassword
	 */
	public String getSempPassword()
	{
		return sempPassword;
	}

	/**
	 * Sets the SEMP password.
	 * @param sempPassword the sempPassword to set
	 */
	public void setSempPassword(String sempPassword)
	{
		this.sempPassword = sempPassword;
	}

	/**
	 * @return the sempUrl
	 */
	public String getSempUrl()
	{
		return sempUrl;
	}

	/**
	 * @return the sempV1Url
	 */
	public String getSempV1Url()
	{
		return sempV1Url;
	}
	
	/**
	 * Sets the SEMP Url.
	 * @param sempUrl the sempUrl to set
	 */
	public void setSempUrl(String sempUrl)
	{
		this.sempUrl = sempUrl;
	}

	/**
	 * Gets the service name.
	 * @return the serviceName
	 */
	public String getServiceName()
	{
		return serviceName;
	}

	/**
	 * Sets the service name.
	 * @param serviceName the serviceName to set
	 */
	public void setServiceName(String serviceName)
	{
		this.serviceName = serviceName;
	}

	/**
	 * Gets the VPN name.
	 * @return the vpnName
	 */
	public String getVpnName()
	{
		return vpnName;
	}

	/**
	 * Sets the VPN Name.
	 * @param vpnName the vpnName to set
	 */
	public void setVpnName(String vpnName)
	{
		this.vpnName = vpnName;
	}

}
