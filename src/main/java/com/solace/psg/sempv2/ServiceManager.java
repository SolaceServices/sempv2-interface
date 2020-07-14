/**
 * 
 */
package com.solace.psg.sempv2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.solace.psg.sempv2.admin.PubSubCloudConsoleApi;
import com.solace.psg.sempv2.apiclient.ApiClient;
import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.apiclient.ApiResponse;
import com.solace.psg.sempv2.admin.model.ClientProfile;
import com.solace.psg.sempv2.admin.model.ClientProfileAsyncResponse;
import com.solace.psg.sempv2.admin.model.DataCenter;
import com.solace.psg.sempv2.admin.model.Role;
import com.solace.psg.sempv2.admin.model.RoleRequest;
import com.solace.psg.sempv2.admin.model.Service;
import com.solace.psg.sempv2.admin.model.ServiceDetails;
import com.solace.psg.sempv2.admin.model.ServiceDetailsResponse;
import com.solace.psg.sempv2.admin.model.Service;
import com.solace.psg.sempv2.admin.model.ServicesResponse;
import com.solace.psg.sempv2.admin.model.User;
import com.solace.psg.sempv2.admin.model.UserRequest;
import com.solace.psg.sempv2.config.api.CertAuthorityApi;
import com.solace.psg.sempv2.config.model.CertAuthoritiesResponse;
import com.solace.psg.sempv2.config.model.CertAuthority;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfile;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfile.ClientConnectDefaultActionEnum;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfile.PublishTopicDefaultActionEnum;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfile.SubscribeShareNameDefaultActionEnum;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfile.SubscribeTopicDefaultActionEnum;
import com.solace.psg.sempv2.config.model.MsgVpnClientProfile;

/**
 * Class to handle various service operations.
 *  
 * @author VictorTsonkov
 *
 */
public class ServiceManager
{
	private int connectTimeout = 60000;
	
	private boolean debugging = false;
	
	private int defaultClientProfileAwait = 5000;
	
	/**
	 * Gets Connection Timeout
	 * @return the connectTimeout
	 */
	public int getConnectTimeout()
	{
		return connectTimeout;
	}

	/**
	 * Sets Connection Timeout
	 * @param connectTimeout the connectTimeout to set
	 */
	public void setConnectTimeout(int connectTimeout)
	{
		this.connectTimeout = connectTimeout;
	}

	/**
	 * Gets read Timeout
	 * @return the readTimeout
	 */
	public int getReadTimeout()
	{
		return readTimeout;
	}

	/**
	 * Sets read Timeout
	 * @param readTimeout the readTimeout to set
	 */
	public void setReadTimeout(int readTimeout)
	{
		this.readTimeout = readTimeout;
	}

	/**
	 * Gets base service URL
	 * @return the baseServiceAdminUrl
	 */
	public String getBaseServiceAdminUrl()
	{
		return tokenAdminUrl;
	}

	/**
	 * 
	 * @param baseServiceAdminUrl the baseServiceAdminUrl to set
	 */
	public void setBaseServiceAdminUrl(String baseServiceAdminUrl)
	{
		this.tokenAdminUrl = baseServiceAdminUrl;
	}

	private int readTimeout = 120000;
	
	private String rootAdminUrl = "https://api.solace.cloud/api/v0";

	private String tokenAdminUrl = rootAdminUrl + "/iam/tokens";
	
	private String serviceAdminUrl = rootAdminUrl + "/services";
	
	private String userAdminUrl = rootAdminUrl + "/users";
	
	private String roleAdminUrl = rootAdminUrl + "/organization/roles";
	
	private String dcAdminUrl = rootAdminUrl + "/datacenters";
	
	//private String orgAdminUrl = rootAdminUrl + "/user/organizations";
	
	private ApiClient apiHttpClient;
	
	private String accessToken;
	
	private String accountUsername;
	
	private String accountPassword;
	
	/**
	 * Gets the current access token set for this Service Facade.
	 * @return the token.
	 */
	public String getCurrentAccessToken()
	{
		return accessToken;
	}
	
	/**
	 * Gets Admin Token URL.
	 * @return the tokenAdminUrl
	 */
	public String getTokenAdminUrl()
	{
		return tokenAdminUrl;
	}

	/**
	 * Sets Admin Token URL.
	 * @param tokenAdminUrl the tokenAdminUrl to set
	 */
	public void setTokenAdminUrl(String tokenAdminUrl)
	{
		this.tokenAdminUrl = tokenAdminUrl;
	}

	/**
	 * Gets User Token URL.
	 * @return the userAdminUrl
	 */
	public String getUserAdminUrl()
	{
		return userAdminUrl;
	}

	/**
	 * Sets User Token URL.
	 * @param userAdminUrl the userAdminUrl to set
	 */
	public void setUserAdminUrl(String userAdminUrl)
	{
		this.userAdminUrl = userAdminUrl;
	}

	/**
	 * Gets Admin Service Token URL.
	 * @return the serviceAdminUrl
	 */
	public String getServiceAdminUrl()
	{
		return serviceAdminUrl;
	}

	/**
	 * Sets User Service Admin URL.
	 * @param serviceAdmiUrl the serviceAdmiUrl to set
	 */
	public void setServiceAdminUrl(String serviceAdmiUrl)
	{
		this.serviceAdminUrl = serviceAdmiUrl;
	}

	private void init()
	{
		apiHttpClient = new ApiClient();
		
		apiHttpClient.setConnectTimeout(connectTimeout);
		
		apiHttpClient.setDebugging(debugging);
	}

	/**
	 * Initialises a new class with a given list of cluster services.  
	 */
	public ServiceManager(List<ServiceDetails> clusterServiceList)
	{
		init();
	}
	
	/**
	 * Initialises a new class with a given list of cluster services.  
	 * @throws ApiException 
	 */
	public ServiceManager(String clusterAccessToken) throws ApiException
	{
		if (clusterAccessToken == null)
			throw new NullPointerException("Parameter clusterAccessToken cannot be null.");
		
		
		this.accessToken = clusterAccessToken;
		init();
	}
	
	/**
	 * Initialises a new class with a given list of cluster services.  
	 * @param accountUsername
	 * @param accountPassword
	 * @throws IOException 
	 * @throws ApiException 
	 */
	public ServiceManager(String accountUsername, String accountPassword) throws ApiException, IOException
	{
		this(accountUsername, accountPassword, true);
	}
	
	/**
	 * Initialises a new class with a given list of cluster services.  
	 * @throws IOException 
	 * @throws ApiException 
	 */
	public ServiceManager(String accountUsername, String accountPassword, boolean obtainAccessToken) throws ApiException, IOException
	{
		if (accountUsername == null)
			throw new NullPointerException("Parameter accountUsername cannot be null.");
		if (accountPassword == null)
			throw new NullPointerException("Parameter accountPassword cannot be null.");	
		
		this.accountUsername = accountUsername;
		this.accountPassword = accountPassword;
		
		init();
		
		if (obtainAccessToken)
			accessToken = getApiToken(accountUsername, accountPassword);
	}	

	/**
	 * Gets all services details.
	 * 
	 * URL: https://api.solace.cloud/api/v0/services
	 * 
	 * @return List<ServiceDetails>
	 * @throws ApiException 
	 */
	public List<ServiceDetails> getAllServiceDetails() throws ApiException
	{				
		List<ServiceDetails> result = new ArrayList<ServiceDetails>();
		
		List<Service> serviceList = getAllServices();
		for (Service service : serviceList)
		{
			ServiceDetails sd = getServiceDetails(service.getServiceId());
			result.add(sd);
		}
		
		return result;
	}
	
	/**
	 * Gets all services from a cluster for a provided token.
	 * 
	 * URL: https://api.solace.cloud/api/v0/services
	 * 
	 * @return List<Service>
	 * @throws ApiException 
	 */
	public List<Service> getAllServices() throws ApiException
	{
		ServicesResponse result = null;
		
		apiHttpClient.setBasePath(serviceAdminUrl);
		
		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		result = api.getAllServices(accessToken);		
		
		return result.getData();
	}
	
	/**
	 * Gets detailed information on a service like URLS, Ports, credentials, cluster info. 
	 * 
	 * URL: https://api.solace.cloud/api/v0/services/{{serviceId}}
	 * 
	 * @param serviceId
	 * @return ServiceDetails
	 * @throws ApiException 
	 */
	public ServiceDetails getServiceDetails(String serviceId) throws ApiException
	{
		ServiceDetails result = null;
		apiHttpClient.setBasePath(serviceAdminUrl);
		
		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		result = api.getServiceDetails(accessToken, serviceId).getData();		
		
		return result;
	}
	
	/**
	 * Gets Service by name.
	 * @param serviceName
	 * @return Service
	 * @throws ApiException 
	 */
	public Service getServiceByName(String serviceName) throws ApiException
	{
		Service result = null;
		List<Service> services = getAllServices();
		
		for (Service service: services)
		{
			if (service.getName().equals(serviceName))
				result = service;
		}
		
		return result;
	}
	
	/**
	 * Gets Service by ID.
	 * @param serviceId
	 * @return Service
	 * @throws ApiException 
	 */
	public Service getServiceById(String serviceId) throws ApiException
	{
		Service result = null;
		List<Service> services = getAllServices();
		
		for (Service service: services)
		{
			if (service.getServiceId().equals(serviceId))
				result = service;
		}
		
		return result;
	}
	
	/**
	 * Gets Service by name.
	 * @param serviceName
	 * @return ServiceDetails
	 * @throws ApiException 
	 */
	public ServiceDetails getServiceDetailsByName(String serviceName) throws ApiException
	{
		ServiceDetails result = null;
		List<Service> services = getAllServices();
		
		for (Service service: services)
		{
			if (service.getName().equals(serviceName))
			{
				result = getServiceDetails(service.getServiceId());
			}
		}
		
		return result;
	}
	
	/**
	 * Create a certificate authority for a service. 
	 * @param serviceId
	 * @param caName
	 * @param caContent
	 * @return true if success, otherwise false
	 * @throws InterruptedException 
	 * @throws ApiException 
	 */
	public boolean addClientCertificateAuthority(String serviceId, String caName, String caContent) throws ApiException, InterruptedException
	{
		boolean result = false;
		apiHttpClient.setBasePath(serviceAdminUrl);
		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		result = api.addCertificateAuthoritySync(accessToken, serviceId, caName, caContent);

		return result;
	}
	
	/**
	 * Update authentication of a VPN.
	 * 
	 * URL: https://api.solace.cloud/api/v0/services/<serviceId>/requests/updateAuthenticationRequests
	 *   
	 * @param serviceId the service ID
	 * @param basicEnabled true if basic authentication should be enabled. This shoud be usually always true.
	 * @param clientCertEnabled true if client certificate authentication should be enabled.
	 * @param validateCertDate true if certificate should be validated.
	 * @return true if successful
	 * @throws ApiException
	 * @throws InterruptedException 
	 */
	public boolean updateServiceAuthentication(String serviceId, boolean basicEnabled, boolean clientCertEnabled, boolean validateCertDate) throws ApiException, InterruptedException
	{
		boolean result = false;
		apiHttpClient.setBasePath(serviceAdminUrl);
		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		result = api.updateServiceAuthenticationSync(accessToken, serviceId, basicEnabled, clientCertEnabled, validateCertDate);

		return result;
	}
	
	/**
	 * Deletes a certificate authority for a service. 
	 * @param serviceId
	 * @param caName
	 * @return true if success
	 * @throws ApiException
	 * @throws InterruptedException
	 */
	public boolean deleteClientCertificateAuthority(String serviceId, String caName) throws ApiException, InterruptedException
	{
		boolean result = false;
		apiHttpClient.setBasePath(serviceAdminUrl);
		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		result = api.deleteCertificateAuthoritySync(accessToken, serviceId, caName);

		return result;
	}
	
	/**
	 * Creates a new service with a provided name.
	 * 
	 * URL: https://api.solace.cloud/api/v0/services
	 * 
	 * @param serviceName
	 * @return ServiceDetails
	 * @throws IOException 
	 * @throws ApiException 
	 * @throws InterruptedException 
	 */
	public ServiceDetails createService(String serviceName, String serviceTypeId, String serviceClassId, String datacenterId) throws ApiException, IOException, InterruptedException
	{
		ServiceDetails result = null;
		
		apiHttpClient.setBasePath(serviceAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		result = api.createServiceSync(accessToken, serviceName, serviceTypeId, serviceClassId, datacenterId);		
		
		return result;
	}

	/**
	 * Creates a new service with a provided name.
	 * 
	 * URL: https://api.solace.cloud/api/v0/services
	 * 
	 * @param serviceName
	 * @return Service
	 * @throws IOException 
	 * @throws ApiException 
	 * @throws InterruptedException 
	 */
	public Service createServiceAsync(String serviceName, String serviceTypeId, String serviceClassId, String datacenterId) throws ApiException, IOException, InterruptedException
	{
		Service result = null;
		
		apiHttpClient.setBasePath(serviceAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		result = api.createServiceAsync(accessToken, serviceName, serviceTypeId, serviceClassId, datacenterId);		
		
		return result;
	}

	/**
	 * Gets all users data.
	 * @return List of Users
	 * @throws ApiException
	 */
	public List<User> getAllUsers() throws ApiException
	{
		apiHttpClient.setBasePath(userAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		
		return api.getAllUsers(accessToken);			
	}
	
	/**
	 * Gets a user by email.
	 * @return List of Users
	 * @throws ApiException
	 */
	public User getUserByEmail(String email) throws ApiException
	{
		User result = null;

		apiHttpClient.setBasePath(userAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 

		List<User> users = api.getAllUsers(accessToken);	
		for (User user : users)
		{
			if (user.getEmail().equals(email))
				result = user;
		}
		
		return result;
	}
	
	/**
	 * Gets data centers.
	 * @return List of Users
	 * @throws ApiException
	 */
	public List<DataCenter> getDataCenters() throws ApiException
	{
		apiHttpClient.setBasePath(dcAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		
		return api.getDataCenters(accessToken);			
	}
	
	/**
	 * Gets user's data.
	 * @return User by id
	 * @throws ApiException
	 */
	public User getUser(String userId) throws ApiException
	{		
		apiHttpClient.setBasePath(userAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		
		return api.getUser(accessToken, userId);			
	}
	
	/* This functionality is currently not supported and exposed for normal users.
	 * 
	public User getUserOrganizations() throws ApiException
	{		
		apiHttpClient.setBasePath(orgAdminUrl);

		PubSubCloudServiceApi api = new PubSubCloudServiceApi(apiHttpClient); 
		
		return api.getUserOgranizations(accessToken);			
	}*/
	
	/**
	 * Delete a user.
	 * @return true if success, otherwise false
	 * @throws ApiException
	 */
	public boolean deleteUser(String userId) throws ApiException
	{		
		apiHttpClient.setBasePath(userAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		return api.deleteUser(accessToken, userId);
	}

	/**
	 * Invites a new user.
	 * 
	 * @param email
	 * @param roles List of String from type UserRoles
	 * @return User the suer object
	 * @throws ApiException 
	 */
	public User addUser(String email, List<String> roles) throws ApiException
	{
		apiHttpClient.setBasePath(userAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		
		return api.addUser(accessToken, email, roles);			
	}

	/**
	 * Invites a new user.
	 * 
	 * @param request the user request
	 * @return User the user object
	 * @throws ApiException 
	 */
	public User addUser(UserRequest request) throws ApiException
	{
		apiHttpClient.setBasePath(userAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		
		return api.addUser(accessToken, request);			
	}

	/**
	 * Deletes a service by its service ID.
	 * @param serviceId
	 * @return true if success, otherwise false
	 * @throws ApiException 
	 * @throws IOException 
	 */
	public boolean deleteService(String serviceId) throws ApiException, IOException
	{
		apiHttpClient.setBasePath(serviceAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		return api.deleteService(accessToken, serviceId);
	}
	
	/**
	 * Gets all organisation roles.
	 * @return List of roles
	 * @throws ApiException 
	 */
	public List<Role> getAllOrganizationRoles() throws ApiException
	{
		apiHttpClient.setBasePath(roleAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		
		return api.getAllOrganizationRoles(accessToken);	
	}
	
	/**
	 * Adds roles to a user.
	 * @param userId
	 * @param roles
	 * @return List of roles
	 * @throws ApiException 
	 */
	public List<Role> addRoleToUser(String userId, List<RoleRequest> roles) throws ApiException
	{
		apiHttpClient.setBasePath(userAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		
		return api.addRoleToUser(accessToken, userId, roles);	
	}
	
	/**
	 * Gets an API management token configured for the cluster by using an account username and password.
	 * 
	 * URL: https://api.solace.cloud/api/v0/iam/tokens
	 * 
	 * @param accountUsername
	 * @param accountPassword
	 * @return token
	 * @throws IOException 
	 * @throws ApiException 
	 */
	public String getApiToken(String accountUsername, String accountPassword) throws ApiException, IOException
	{
		String result = null;
		
		apiHttpClient.setBasePath(tokenAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		result = api.getToken(accountUsername, accountPassword);		
		
		return result;
	}
			
	/**
	 * Add a new Client Profile to a VPN (Service).
	 * @param serviceId
	 * @param clientProfileName
	 * @return true if success, otherwise false
	 * @throws ApiException 
	 * @throws InterruptedException 
	 */
	public boolean addClientProfile(String serviceId, String clientProfileName) throws InterruptedException, ApiException
	{
		ClientProfile profile = new ClientProfile();
		
		profile.setClientProfileName(clientProfileName);
				
		return addClientProfile(serviceId, profile);
	}

	/**
	 * Add a new Client Profile to a VPN (Service).
	 * @param serviceId
	 * @param clientProfileName
	 * @return true if success, otherwise false
	 * @throws ApiException 
	 * @throws InterruptedException 
	 */
	public boolean deleteClientProfile(String serviceId, String clientProfileName) throws InterruptedException, ApiException
	{
		ClientProfile profile = new ClientProfile();
		profile.setClientProfileName(clientProfileName);
		
		apiHttpClient.setBasePath(serviceAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		
		return api.deleteClientProfileSync(accessToken, serviceId, profile);	
	}
	
	/**
	 * Add a new Client Profile to a VPN (Service). 
	 * This operation is asynchronous and takes usually a bit of time to be created so a delay is needed to obtain result.
	 * @param serviceId
	 * @param profile
	 * @return true if success, otherwise false
	 * @throws ApiException 
	 * @throws InterruptedException 
	 */
	public boolean addClientProfile(String serviceId, ClientProfile profile) throws InterruptedException, ApiException
	{
		boolean result = false;
		apiHttpClient.setBasePath(serviceAdminUrl);

		PubSubCloudConsoleApi api = new PubSubCloudConsoleApi(apiHttpClient); 
		result = api.addClientProfileSync(accessToken, serviceId, profile, defaultClientProfileAwait);	
		
		return result;
	}
	
}
