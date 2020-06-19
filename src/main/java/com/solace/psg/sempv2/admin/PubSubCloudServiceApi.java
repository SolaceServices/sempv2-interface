/**
 * 
 */
package com.solace.psg.sempv2.admin;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.solace.psg.sempv2.admin.model.DataCenter;
import com.solace.psg.sempv2.admin.model.DataCentersResponse;
import com.solace.psg.sempv2.admin.model.AdminProgressState;
import com.solace.psg.sempv2.admin.model.ApiTokenRequest;
import com.solace.psg.sempv2.admin.model.ApiTokenResponse;
import com.solace.psg.sempv2.admin.model.Certificate;
import com.solace.psg.sempv2.admin.model.CertificateAuthorityCreateResponse;
import com.solace.psg.sempv2.admin.model.CertificateAuthorityRequest;
import com.solace.psg.sempv2.admin.model.CertificateAuthorityStatusResponse;
import com.solace.psg.sempv2.admin.model.ClientProfile;
import com.solace.psg.sempv2.admin.model.ClientProfileAsyncRequest;
import com.solace.psg.sempv2.admin.model.ClientProfileAsyncResponse;
import com.solace.psg.sempv2.admin.model.ClientProfileStatusResponse;
import com.solace.psg.sempv2.admin.model.EmptyResponse;
import com.solace.psg.sempv2.admin.model.Role;
import com.solace.psg.sempv2.admin.model.RoleRequest;
import com.solace.psg.sempv2.admin.model.RolesResponse;
import com.solace.psg.sempv2.admin.model.Service;
import com.solace.psg.sempv2.admin.model.ServiceCreateRequest;
import com.solace.psg.sempv2.admin.model.ServiceCreateResponse;
import com.solace.psg.sempv2.admin.model.ServiceDetails;
import com.solace.psg.sempv2.admin.model.ServiceDetailsResponse;
import com.solace.psg.sempv2.admin.model.ServiceState;
import com.solace.psg.sempv2.admin.model.ServicesResponse;
import com.solace.psg.sempv2.admin.model.User;
import com.solace.psg.sempv2.admin.model.UserRequest;
import com.solace.psg.sempv2.admin.model.UserResponse;
import com.solace.psg.sempv2.admin.model.UsersResponse;
import com.solace.psg.sempv2.admin.model.VpnAuthenticationStatusResponse;
import com.solace.psg.sempv2.admin.model.VpnAuthenticationUpdateRequest;
import com.solace.psg.sempv2.admin.model.VpnAuthenticationUpdateResponse;
import com.solace.psg.sempv2.apiclient.ApiClient;
import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.apiclient.ApiResponse;
import com.solace.psg.sempv2.apiclient.Configuration;
import com.solace.psg.sempv2.apiclient.Pair;
import com.solace.psg.sempv2.auth.Authentication;
import com.solace.psg.sempv2.auth.BearerTokenAuth;
import com.solace.psg.sempv2.auth.HttpBasicAuth;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Response;


/**
 * Service API for Solace PubSub Cloud.
 * 
 * @author VictorTsonkov
 *
 */
public class PubSubCloudServiceApi
{
	private int defaultServiceTimeout = 2000;
	private int defaultServiceRetry = 500; // waiting for around 16 mins
	
	private int defaultCertRetry = 60;
	
	private String certAuthorityPath = "/requests/serviceCertificateAuthorityRequests";
	
	private String serviceRequests = "/requests/";

	private String serviceAuthenticationRequests = "/requests/updateAuthenticationRequests";

	private ApiClient apiClient;

	public ApiClient getApiClient()
	{
		return apiClient;
	}

	/**
	 * Initialises a new instance of the class.
	 */
	public PubSubCloudServiceApi()
	{
		this(Configuration.getDefaultApiClient());
	}

	/**
	 * Initialises a new instance of the class.
	 */
	public PubSubCloudServiceApi(ApiClient apiClient)
	{
		this.apiClient = apiClient;
	}

	/**
	 * Adds a new services to the cluster with a provided name.
	 * @param token
	 * @param serviceName
	 * @return Service
	 * @throws ApiException 
	 * @throws IOException 
	 */
	public Service createServiceAsync(String accessToken, String serviceName, String serviceTypeId, String serviceClassId, String datacenterId) throws ApiException, IOException
	{
		return createServiceAsync(accessToken, serviceName, serviceTypeId, serviceClassId, datacenterId, "default", ServiceState.start);
	}
	
	/**
	 * Adds a new services to the cluster with a provided name.
	 * @param token
	 * @param serviceName
	 * @return Service
	 * @throws ApiException 
	 * @throws IOException 
	 */
	public Service createServiceAsync(String accessToken, String serviceName, String serviceTypeId, String serviceClassId, String datacenterId, String partitionId, ServiceState adminState) throws ApiException, IOException
	{
		ServiceCreateRequest request = new ServiceCreateRequest(serviceName, serviceTypeId, serviceClassId, datacenterId, partitionId, adminState.toString());
		return createServiceAsync(accessToken, request);
	}
	
	/**
	 * Adds a new services to the cluster with a provided name.
	 * @param token
	 * @param serviceName
	 * @return Service
	 * @throws ApiException 
	 * @throws IOException 
	 */
	public Service createServiceAsync(String accessToken, ServiceCreateRequest request) throws ApiException, IOException
	{
		ApiResponse<ServiceCreateResponse> result = null;
		
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);

		Type returnType = new TypeToken<ServiceCreateResponse>(){}.getType();
		Call call = getJsonCall("POST", "", request, auth);	

		result = apiClient.execute(call, returnType);		

		return result.getData().getData();
	}
	
	/**
	 * Adds a new service to the cluster with a provided name and waits until state changes from Pending to running.
	 * @param token
	 * @param serviceName
	 * @return
	 * @throws ApiException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public ServiceDetails createServiceSync(String accessToken, String serviceName, String serviceTypeId, String serviceClassId, String datacenterId) throws ApiException, IOException, InterruptedException
	{
		ServiceDetails result = null;
		
		Service createdService = createServiceAsync(accessToken, serviceName, serviceTypeId, serviceClassId, datacenterId, "default", ServiceState.start);
		
		int retry = 0;
		do // Wait until Service is fully started
		{
			Thread.sleep(defaultServiceTimeout);
			result = getServiceDetails(accessToken, createdService.getServiceId()).getData();
			retry += 1;
		}
		while (!result.getAdminProgress().equalsIgnoreCase(AdminProgressState.Completed) && retry < defaultServiceRetry);
		if (retry >= defaultServiceRetry)
			throw new ApiException("Creating service had not succeded with state in within " + retry*defaultServiceTimeout + " seconds") ;
		
		return result;
	}
	
	/**
	 * Deletes a service by its service ID.
	 * @param serviceId
	 * @throws ApiException 
	 * @throws IOException 
	 */
	public boolean deleteService(String accessToken, String serviceId) throws ApiException, IOException
	{
		boolean result = false;
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);

		Type returnType = new TypeToken<EmptyResponse>(){}.getType();
		String subPath = "/" + serviceId;
		Call call = getJsonCall("DELETE", subPath, null, auth);	

		//Response rawResponse = call.execute();
		//EmptyResponse response = apiClient.handleResponse(rawResponse, returnType);	
		ApiResponse<EmptyResponse> response = apiClient.execute(call, returnType);	
		if (response.getStatusCode() == 200)
			result = true;

		return result;
	}
	
	/**
	 * Gets an organization token for an account name. 
	 * @param accountName
	 * @param accountPassword
	 * @param organization
	 * @return
	 * @throws ApiException 
	 * @throws IOException 
	 
	public String getOrganizationToken(String accountName, String accountPassword, String organization) throws ApiException, IOException
	{
		ApiTokenRequest request =  new ApiTokenRequest(accountName, accountPassword);
		String subPath = "/iam/tokens" + organization;
		Call call =getJsonCall("GET", subPath, request, null);
		Type returnType = new TypeToken<ApiTokenResponse>()
		{
		}.getType();
		Response rawResponse = call.execute();
		ApiTokenResponse response = apiClient.handleResponse(rawResponse, returnType);

		return response.getToken();		
	}*/
		
	/**
	 * Add client profile without waiting and checking for result.
	 * @param token
	 * @param serviceId
	 * @param profile
	 * @throws ApiException 
	 */
	public ApiResponse<ClientProfileAsyncResponse> addClientProfileAsync(String accessToken, String serviceId, ClientProfile profile) throws ApiException
	{
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		ClientProfileAsyncRequest request = new ClientProfileAsyncRequest();
		request.setClientProfile(profile);
		request.setOperation("create");
		
		Call call = getAddClientProfileCall(serviceId, request);
		Type returnType = new TypeToken<ClientProfileAsyncResponse>(){}.getType();
		ApiResponse<ClientProfileAsyncResponse> response = apiClient.execute(call, returnType);
		
		return response;
	}
	
	/**
	 * Add client profile without waiting and checking for result.
	 * @param token
	 * @param serviceId
	 * @param profile
	 * @throws ApiException 
	 */
	public ApiResponse<ClientProfileAsyncResponse> deleteClientProfileAsync(String accessToken, String serviceId, ClientProfile profile) throws ApiException
	{
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		ClientProfileAsyncRequest request = new ClientProfileAsyncRequest();
		request.setClientProfile(profile);
		request.setOperation("delete");
		
		Call call = getAddClientProfileCall(serviceId, request);
		Type returnType = new TypeToken<ClientProfileAsyncResponse>(){}.getType();
		ApiResponse<ClientProfileAsyncResponse> response = apiClient.execute(call, returnType);
		
		return response;
	}
	
	/**
	 * Add client profile without waiting and checking for result.
	 * @param token
	 * @param serviceId
	 * @param profile
	 * @throws ApiException 
	 * @throws InterruptedException 
	 */
	public boolean deleteClientProfileSync(String accessToken, String serviceId, ClientProfile profile) throws ApiException, InterruptedException
	{
		boolean result = false;
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);

		ApiResponse<ClientProfileAsyncResponse> response = deleteClientProfileAsync(accessToken, serviceId, profile);

		String subpath = "/" + serviceId + serviceRequests + response.getData().getData().getId();

		Call call = getJsonCall("GET", subpath, null, auth);
		Type returnType = new TypeToken<ClientProfileStatusResponse>(){}.getType();
		String status = "";
		
		int retries = 0;
		do 
		{
			call = getJsonCall("GET", subpath, null, auth);
			ApiResponse<ClientProfileStatusResponse> statusResponse = apiClient.execute(call, returnType);
			status = statusResponse.getData().getData().getAdminProgress();
			retries += 1; 
			Thread.sleep(1000);
		}
		while (!status.equals("completed") && !status.equals("failed") && retries < defaultCertRetry);
		if (status.equals("completed"))
			result = true;
		
		return result;
	}
	
	/**
	 * Add client profile without waiting and checking for result.
	 * @param token
	 * @param serviceId
	 * @param profile
	 * @throws InterruptedException 
	 * @throws ApiException 
	 */
	public boolean addClientProfileSync(String accessToken, String serviceId, ClientProfile profile, int awaitTimeout) throws InterruptedException, ApiException
	{
		boolean result = false;
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);

		ApiResponse<ClientProfileAsyncResponse> response = addClientProfileAsync(accessToken, serviceId, profile);

		String subpath = "/" + serviceId + serviceRequests + response.getData().getData().getId();

		Call call = getJsonCall("GET", subpath, null, auth);
		Type returnType = new TypeToken<ClientProfileStatusResponse>(){}.getType();
		String status = "";
		
		int retries = 0;
		do 
		{
			call = getJsonCall("GET", subpath, null, auth);
			ApiResponse<ClientProfileStatusResponse> statusResponse = apiClient.execute(call, returnType);
			status = statusResponse.getData().getData().getAdminProgress();
			retries += 1; 
			Thread.sleep(1000);
		}
		while (!status.equals("completed") && !status.equals("failed") && retries < defaultCertRetry);
		if (status.equals("completed"))
			result = true;
		
		return result;
	}	
	
	/**
	 * Gets token
	 * 
	 * @param username
	 * @param password
	 * @return
	 * @throws ApiException
	 * @throws IOException
	 */
	public String getToken(String username, String password) throws ApiException, IOException
	{
		Call call = getTokenCall(username, password);
		Type returnType = new TypeToken<ApiTokenResponse>()
		{
		}.getType();
		Response rawResponse = call.execute();
		ApiTokenResponse response = apiClient.handleResponse(rawResponse, returnType);

		return response.getToken();
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
	public Call getJsonCall(String verb, String subPath, Object jsonRequest, Authentication auth) throws ApiException
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
	 * Create a request call.
	 * 
	 * @param username
	 * @param password
	 * @return Call
	 * @throws ApiException
	 */
	public Call getTokenCall(String username, String password) throws ApiException
	{
		Object localVarPostBody = new ApiTokenRequest(username, password);

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
		{ HttpBasicAuth.AUTH_TYPE };
		return apiClient.buildCall("", "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody,
				localVarHeaderParams, localVarFormParams, localVarAuthNames, null);
	}
	
	/**
	 * Gets data for all services for a cluster/environment managed by a token.
	 * @param accessToken
	 * @return ServicesResponse
	 * @throws ApiException
	 */
	public ServicesResponse getAllServices(String accessToken) throws ApiException
	{		
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		Call call = getAllServicesCall();
		Type returnType = new TypeToken<ServicesResponse>(){}.getType();
		ApiResponse<ServicesResponse> response = apiClient.execute(call, returnType);

		if (response.getStatusCode() == 200)
			return response.getData();
		else
			throw new ApiException("Response returned:" + response.getStatusCode());		
	}
	
	/**
	 * Update authentication of a VPN.
	 * 
	 * URL: https://api.solace.cloud/api/v0/services/<serviceId>/requests/updateAuthenticationRequests
	 * @param accessToken The access token
	 * @param serviceId the service ID
	 * @param basicEnabled true if basic authentication should be enabled. This shoud be usually always true.
	 * @param clientCertEnabled true if client certificate authentication should be enabled.
	 * @param validateCertDate true if certificate should be validated.
	 * @return VpnAuthenticationUpdateResponse
	 * @throws ApiException 
	 */
	public VpnAuthenticationUpdateResponse updateServiceAuthenticationAsync(String accessToken, String serviceId, boolean basicEnabled, boolean clientCertEnabled, boolean validateCertDate) throws ApiException
	{
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		String subpath = "/" + serviceId + serviceAuthenticationRequests;
		
		VpnAuthenticationUpdateRequest request = new VpnAuthenticationUpdateRequest();
		request.setAuthenticationBasicEnabled(String.valueOf(basicEnabled));
		request.setAuthenticationClientCertEnabled(String.valueOf(clientCertEnabled));
		request.setAuthenticationClientCertValidateDateEnabled(String.valueOf(validateCertDate));
		
		Call call = getJsonCall("POST", subpath, request, auth);
		Type returnType = new TypeToken<VpnAuthenticationUpdateResponse>(){}.getType();
		ApiResponse<VpnAuthenticationUpdateResponse> response = apiClient.execute(call, returnType);
		
		return response.getData();
	}
	
	/**
	 * Update authentication of a VPN.
	 * @param accessToken The access token
	 * @param serviceId the service ID
	 * @param basicEnabled true if basic authentication should be enabled. This shoud be usually always true.
	 * @param clientCertEnabled true if client certificate authentication should be enabled.
	 * @param validateCertDate true if certificate should be validated.
	 * @return true if successful
	 * @throws ApiException
	 * @throws InterruptedException 
	 */
	public boolean updateServiceAuthenticationSync(String accessToken, String serviceId, boolean basicEnabled, boolean clientCertEnabled, boolean validateCertDate) throws ApiException, InterruptedException
	{
		boolean result = false;
		
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);

		VpnAuthenticationUpdateResponse response = updateServiceAuthenticationAsync(accessToken, serviceId, basicEnabled, clientCertEnabled, validateCertDate);

		String subpath = "/" + serviceId + serviceRequests + response.getData().getId();

		Call call = getJsonCall("GET", subpath, null, auth);
		Type returnType = new TypeToken<VpnAuthenticationStatusResponse>(){}.getType();
		String status = "";
		
		int retries = 0;
		do 
		{
			call = getJsonCall("GET", subpath, null, auth);
			ApiResponse<VpnAuthenticationStatusResponse> statusResponse = apiClient.execute(call, returnType);
			status = statusResponse.getData().getData().getAdminProgress();
			retries += 1; 
			Thread.sleep(1000);
		}
		while (!status.equals("completed") && !status.equals("failed") && retries < defaultCertRetry);
		if (status.equals("completed"))
			result = true;
		
		return result;
	}
	
	/**
	 * Adds a Certificate Authority and checks for a result.
	 * @param accessToken The user token
	 * @param serviceId Service ID
	 * @param certificateName Certificate Authority name
	 * @param certificateContent Certificate's content starting with -BEGIN CERTIFICATE and ending with END CERTIFICATE- 
	 * @return
	 * @throws ApiException 
	 */
	public CertificateAuthorityCreateResponse addCertificateAuthorityAsync(String accessToken, String serviceId, String certificateName, String certificateContent) throws ApiException
	{	
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		String subpath = "/" + serviceId + certAuthorityPath;
		
		Certificate cert = new Certificate();
		cert.setAction("create");
		cert.setContent(certificateContent);
		cert.setName(certificateName);
		CertificateAuthorityRequest request = new CertificateAuthorityRequest();
		request.setCertificate(cert);
				
		Call call = getJsonCall("POST", subpath, request, auth);
		Type returnType = new TypeToken<CertificateAuthorityCreateResponse>(){}.getType();
		ApiResponse<CertificateAuthorityCreateResponse> response = apiClient.execute(call, returnType);
		
		return response.getData();
	}
	
	/**
	 * Add a certificate authority and checks for a status. 
	 * @param accessToken
	 * @param serviceId
	 * @param certificateName
	 * @param certificateContent
	 * @return
	 * @throws ApiException
	 * @throws InterruptedException
	 */
	public boolean addCertificateAuthoritySync(String accessToken, String serviceId, String certificateName, String certificateContent) throws ApiException, InterruptedException
	{
		boolean result = false;
		
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);

		CertificateAuthorityCreateResponse response = addCertificateAuthorityAsync(accessToken, serviceId, certificateName, certificateContent);

		String subpath = "/" + serviceId + serviceRequests + response.getData().getId();

		Call call = getJsonCall("GET", subpath, null, auth);
		Type returnType = new TypeToken<CertificateAuthorityStatusResponse>(){}.getType();
		String status = "";
		
		int retries = 0;
		do 
		{
			call = getJsonCall("GET", subpath, null, auth);
			ApiResponse<CertificateAuthorityStatusResponse> statusResponse = apiClient.execute(call, returnType);
			status = statusResponse.getData().getData().getAdminProgress();
			retries += 1; 
			Thread.sleep(1000);
		}
		while (!status.equals("completed") && !status.equals("failed") && retries < defaultCertRetry);
		if (status.equals("completed"))
			result = true;
		
		return result;
	}

	/**
	 * Deletes a certificate.
	 * @param accessToken
	 * @param serviceId
	 * @param certificateName
	 * @return
	 * @throws ApiException
	 */
	public CertificateAuthorityCreateResponse deleteCertificateAuthorityAsync(String accessToken, String serviceId, String certificateName) throws ApiException
	{
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		Certificate cert = new Certificate();
		cert.setAction("delete");
		cert.setName(certificateName);
		CertificateAuthorityRequest request = new CertificateAuthorityRequest();
		request.setCertificate(cert);

		String subpath = "/" + serviceId + certAuthorityPath;
		
		Call call = getJsonCall("POST", subpath, request, auth);
		Type returnType = new TypeToken<CertificateAuthorityCreateResponse>(){}.getType();
		ApiResponse<CertificateAuthorityCreateResponse> response = apiClient.execute(call, returnType);
		
		return response.getData();
	}
	
	/**
	 * Deletes a certificate and wait for result.
	 * @param accessToken
	 * @param serviceId
	 * @param certificateName
	 * @return
	 * @throws ApiException
	 * @throws InterruptedException 
	 */
	public boolean deleteCertificateAuthoritySync(String accessToken, String serviceId, String certificateName) throws ApiException, InterruptedException
	{
		boolean result = false;
		
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);

		CertificateAuthorityCreateResponse response = deleteCertificateAuthorityAsync(accessToken, serviceId, certificateName);

		String subpath = "/" + serviceId + serviceRequests + response.getData().getId();

		Call call = getJsonCall("GET", subpath, null, auth);
		Type returnType = new TypeToken<CertificateAuthorityStatusResponse>(){}.getType();
		String status = "";
		
		int retries = 0;
		do 
		{
			call = getJsonCall("GET", subpath, null, auth);
			ApiResponse<CertificateAuthorityStatusResponse> statusResponse = apiClient.execute(call, returnType);
			status = statusResponse.getData().getData().getAdminProgress();
			retries += 1; 
			Thread.sleep(1000);
		}
		while (!status.equals("completed") && !status.equals("failed") && retries < defaultCertRetry);
		if (status.equals("completed"))
			result = true;
		
		return result;
	}

	/**
	 * Gets datacenters.
	 * @param accessToken
	 * @return List of Users
	 * @throws ApiException
	 */
	public List<DataCenter> getDataCenters(String accessToken) throws ApiException
	{
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		Call call = getJsonCall("GET", "", null, auth);
		Type returnType = new TypeToken<DataCentersResponse>(){}.getType();
		ApiResponse<DataCentersResponse> response = apiClient.execute(call, returnType);
	
		return response.getData().getData();
	}
	
	/**
	 * Gets all users data.
	 * @param accessToken
	 * @return List of Users
	 * @throws ApiException
	 */
	public List<User> getAllUsers(String accessToken) throws ApiException
	{		
		List<User> users = null;
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		Call call = getJsonCall("GET", "", null, auth);
		Type returnType = new TypeToken<UsersResponse>(){}.getType();
		ApiResponse<UsersResponse> response = apiClient.execute(call, returnType);
		
		users = response.getData().getData();
		
		// Get all users is using paging to return results.
		for (int i = 1; i < response.getData().getMeta().getPages().getTotalPages(); i++)
		{
			String query = "?page-number=" + i;
			call = getJsonCall("GET", query, null, auth);
			response = apiClient.execute(call, returnType);
			users.addAll(response.getData().getData());
		}
		
		return users;
	}

	/**
	 * Gets user's data.
	 * @param accessToken
	 * @return User by id
	 * @throws ApiException
	 */
	public User getUser(String accessToken, String userId) throws ApiException
	{		
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		String subPath = "/" + userId;
		
		Call call = getJsonCall("GET", subPath, null, auth);
		Type returnType = new TypeToken<UserResponse>(){}.getType();
		ApiResponse<UserResponse> response = apiClient.execute(call, returnType);
		
		return response.getData().getData();
	}
	
	/**
	 * Delete a user.
	 * @param accessToken
	 * @return true if success
	 * @throws ApiException
	 */
	public boolean deleteUser(String accessToken, String userId) throws ApiException
	{		
		boolean result = false;
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		String subPath = "/" + userId;
	
		Call call = getJsonCall("DELETE", subPath, null, auth);
		Type returnType = new TypeToken<EmptyResponse>(){}.getType();

		ApiResponse<EmptyResponse> response = apiClient.execute(call, returnType);	
		if (response.getStatusCode() == 200)
			result = true;

		return result;
	}
	
	/**
	 * Invites a new user.
	 * 
	 * @param accessToken
	 * @param email
	 * @param roles List of String from type UserRoles
	 * @throws ApiException 
	 */
	public User addUser(String accessToken, String email, List<String> roles) throws ApiException
	{
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		UserRequest request = new UserRequest(email, roles);
		
		Call call = getJsonCall("POST", "", request, auth);
		Type returnType = new TypeToken<UserResponse>(){}.getType();
		ApiResponse<UserResponse> response = apiClient.execute(call, returnType);
		
		return response.getData().getData();
	}
	
	/**
	 * Gets all organization roles.
	 * @param accessToken
	 * @return List<Role>
	 * @throws ApiException 
	 */
	public List<Role> getAllOrganizationRoles(String accessToken) throws ApiException
	{
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		Call call = getJsonCall("GET", "", null, auth);
		Type returnType = new TypeToken<RolesResponse>(){}.getType();
		ApiResponse<RolesResponse> response = apiClient.execute(call, returnType);
		
		return response.getData().getData();		
	}
	
	/**
	 * Adds roles to a user.
	 * @param accessToken
	 * @param userId
	 * @param role
	 * @return List<Role>
	 * @throws ApiException 
	 */
	public List<Role> addRoleToUser(String accessToken, String userId, List<RoleRequest> roles) throws ApiException
	{
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		String subPath = "/" + userId + "/roles";
		
		Call call = getJsonCall("PUT", subPath, roles, auth);
		Type returnType = new TypeToken<RolesResponse>(){}.getType();
		ApiResponse<RolesResponse> response = apiClient.execute(call, returnType);
		
		return response.getData().getData();
	}
	
	/**
	 * Gets detailed information on a service like URLS, Ports, credentials, cluster info. 
	 * 
	 * @param accessToken
	 * @param serviceId
	 * @return ServiceDetailsResponse
	 * @throws ApiException 
	 */
	public ServiceDetailsResponse getServiceDetails(String accessToken, String serviceId) throws ApiException
	{
		BearerTokenAuth auth = new BearerTokenAuth(accessToken);
		apiClient.setAuthentication(auth);
		
		Call call = getServiceDetailsCall(serviceId);

		Type returnType = new TypeToken<ServiceDetailsResponse>(){}.getType();
		ApiResponse<ServiceDetailsResponse> response = apiClient.execute(call, returnType);

		if (response.getStatusCode() == 200)
			return response.getData();
		else
			throw new ApiException("Response returned:" + response.getStatusCode());		
	}
	
	/**
	 * Create a request call.
	 * 
	 * @param username
	 * @param password
	 * @return Call
	 * @throws ApiException
	 */
	public Call getAllServicesCall() throws ApiException
	{
		Object localVarPostBody = null;

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
		{ BearerTokenAuth.AUTH_TYPE };
		return apiClient.buildCall("", "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody,
				localVarHeaderParams, localVarFormParams, localVarAuthNames, null);
	}

	/**
	 * Create a request call.
	 * 
	 * @param username
	 * @param password
	 * @return Call
	 * @throws ApiException
	 */
	public Call getAddClientProfileCall(String serviceId, ClientProfileAsyncRequest request) throws ApiException
	{
		Object localVarPostBody = request;

        String localVarPath = "/{serviceId}/requests/clientProfileRequests"
                .replaceAll("\\{" + "serviceId" + "\\}", apiClient.escapeString(serviceId.toString()));

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
		{ BearerTokenAuth.AUTH_TYPE };
		return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody,
				localVarHeaderParams, localVarFormParams, localVarAuthNames, null);
	}

	/**
	 * Create a request call.
	 * 
	 * @param username
	 * @param password
	 * @return Call
	 * @throws ApiException
	 */
	public Call getServiceDetailsCall(String serviceId) throws ApiException
	{
		Object localVarPostBody = null;

        String localVarPath = "/{serviceId}"
                .replaceAll("\\{" + "serviceId" + "\\}", apiClient.escapeString(serviceId.toString()));

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
		{ BearerTokenAuth.AUTH_TYPE };
		return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody,
				localVarHeaderParams, localVarFormParams, localVarAuthNames, null);
	}
}
