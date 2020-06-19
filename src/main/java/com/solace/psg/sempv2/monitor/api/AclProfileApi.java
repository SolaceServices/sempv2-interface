/*
 * SEMP (Solace Element Management Protocol)
 * SEMP (starting in `v2`, see note 1) is a RESTful API for configuring, monitoring, and administering a Solace PubSub+ broker.  SEMP uses URIs to address manageable **resources** of the Solace PubSub+ broker. Resources are individual **objects**, **collections** of objects, or (exclusively in the action API) **actions**. This document applies to the following API:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Monitoring|/SEMP/v2/monitor|Querying operational parameters|See note 2    The following APIs are also available:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Action|/SEMP/v2/action|Performing actions|See note 2 Configuration|/SEMP/v2/config|Reading and writing config state|See note 2    Resources are always nouns, with individual objects being singular and collections being plural.  Objects within a collection are identified by an `obj-id`, which follows the collection name with the form `collection-name/obj-id`.  Actions within an object are identified by an `action-id`, which follows the object name with the form `obj-id/action-id`.  Some examples:  ``` /SEMP/v2/config/msgVpns                        ; MsgVpn collection /SEMP/v2/config/msgVpns/a                      ; MsgVpn object named \"a\" /SEMP/v2/config/msgVpns/a/queues               ; Queue collection in MsgVpn \"a\" /SEMP/v2/config/msgVpns/a/queues/b             ; Queue object named \"b\" in MsgVpn \"a\" /SEMP/v2/action/msgVpns/a/queues/b/startReplay ; Action that starts a replay on Queue \"b\" in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients             ; Client collection in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients/c           ; Client object named \"c\" in MsgVpn \"a\" ```  ## Collection Resources  Collections are unordered lists of objects (unless described as otherwise), and are described by JSON arrays. Each item in the array represents an object in the same manner as the individual object would normally be represented. In the configuration API, the creation of a new object is done through its collection resource.  ## Object and Action Resources  Objects are composed of attributes, actions, collections, and other objects. They are described by JSON objects as name/value pairs. The collections and actions of an object are not contained directly in the object's JSON content; rather the content includes an attribute containing a URI which points to the collections and actions. These contained resources must be managed through this URI. At a minimum, every object has one or more identifying attributes, and its own `uri` attribute which contains the URI pointing to itself.  Actions are also composed of attributes, and are described by JSON objects as name/value pairs. Unlike objects, however, they are not members of a collection and cannot be retrieved, only performed. Actions only exist in the action API.  Attributes in an object or action may have any (non-exclusively) of the following properties:   Property|Meaning|Comments :---|:---|:--- Identifying|Attribute is involved in unique identification of the object, and appears in its URI| Required|Attribute must be provided in the request| Read-Only|Attribute can only be read, not written|See note 3 Write-Only|Attribute can only be written, not read| Requires-Disable|Attribute can only be changed when object is disabled| Deprecated|Attribute is deprecated, and will disappear in the next SEMP version|    In some requests, certain attributes may only be provided in certain combinations with other attributes:   Relationship|Meaning :---|:--- Requires|Attribute may only be changed by a request if a particular attribute or combination of attributes is also provided in the request Conflicts|Attribute may only be provided in a request if a particular attribute or combination of attributes is not also provided in the request    ## HTTP Methods  The following HTTP methods manipulate resources in accordance with these general principles. Note that some methods are only used in certain APIs:   Method|Resource|Meaning|Request Body|Response Body|Missing Request Attributes :---|:---|:---|:---|:---|:--- POST|Collection|Create object|Initial attribute values|Object attributes and metadata|Set to default PUT|Object|Create or replace object|New attribute values|Object attributes and metadata|Set to default (but see note 4) PUT|Action|Performs action|Action arguments|Action metadata|N/A PATCH|Object|Update object|New attribute values|Object attributes and metadata|unchanged DELETE|Object|Delete object|Empty|Object metadata|N/A GET|Object|Get object|Empty|Object attributes and metadata|N/A GET|Collection|Get collection|Empty|Object attributes and collection metadata|N/A    ## Common Query Parameters  The following are some common query parameters that are supported by many method/URI combinations. Individual URIs may document additional parameters. Note that multiple query parameters can be used together in a single URI, separated by the ampersand character. For example:  ``` ; Request for the MsgVpns collection using two hypothetical query parameters \"q1\" and \"q2\" ; with values \"val1\" and \"val2\" respectively /SEMP/v2/monitor/msgVpns?q1=val1&q2=val2 ```  ### select  Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. Use this query parameter to limit the size of the returned data for each returned object, return only those fields that are desired, or exclude fields that are not desired.  The value of `select` is a comma-separated list of attribute names. If the list contains attribute names that are not prefaced by `-`, only those attributes are included in the response. If the list contains attribute names that are prefaced by `-`, those attributes are excluded from the response. If the list contains both types, then the difference of the first set of attributes and the second set of attributes is returned. If the list is empty (i.e. `select=`), no attributes are returned.  All attributes that are prefaced by `-` must follow all attributes that are not prefaced by `-`. In addition, each attribute name in the list must match at least one attribute in the object.  Names may include the `*` wildcard (zero or more characters). Nested attribute names are supported using periods (e.g. `parentName.childName`).  Some examples:  ``` ; List of all MsgVpn names /SEMP/v2/monitor/msgVpns?select=msgVpnName ; List of all MsgVpn and their attributes except for their names /SEMP/v2/monitor/msgVpns?select=-msgVpnName ; Authentication attributes of MsgVpn \"finance\" /SEMP/v2/monitor/msgVpns/finance?select=authentication* ; All attributes of MsgVpn \"finance\" except for authentication attributes /SEMP/v2/monitor/msgVpns/finance?select=-authentication* ; Access related attributes of Queue \"orderQ\" of MsgVpn \"finance\" /SEMP/v2/monitor/msgVpns/finance/queues/orderQ?select=owner,permission ```  ### where  Include in the response only objects where certain conditions are true. Use this query parameter to limit which objects are returned to those whose attribute values meet the given conditions.  The value of `where` is a comma-separated list of expressions. All expressions must be true for the object to be included in the response. Each expression takes the form:  ``` expression  = attribute-name OP value OP          = '==' | '!=' | '&lt;' | '&gt;' | '&lt;=' | '&gt;=' ```  `value` may be a number, string, `true`, or `false`, as appropriate for the type of `attribute-name`. Greater-than and less-than comparisons only work for numbers. A `*` in a string `value` is interpreted as a wildcard (zero or more characters). Some examples:  ``` ; Only enabled MsgVpns /SEMP/v2/monitor/msgVpns?where=enabled==true ; Only MsgVpns using basic non-LDAP authentication /SEMP/v2/monitor/msgVpns?where=authenticationBasicEnabled==true,authenticationBasicType!=ldap ; Only MsgVpns that allow more than 100 client connections /SEMP/v2/monitor/msgVpns?where=maxConnectionCount>100 ; Only MsgVpns with msgVpnName starting with \"B\": /SEMP/v2/monitor/msgVpns?where=msgVpnName==B* ```  ### count  Limit the count of objects in the response. This can be useful to limit the size of the response for large collections. The minimum value for `count` is `1` and the default is `10`. There is also a per-collection maximum value to limit request handling time. For example:  ``` ; Up to 25 MsgVpns /SEMP/v2/monitor/msgVpns?count=25 ```  ### cursor  The cursor, or position, for the next page of objects. Cursors are opaque data that should not be created or interpreted by SEMP clients, and should only be used as described below.  When a request is made for a collection and there may be additional objects available for retrieval that are not included in the initial response, the response will include a `cursorQuery` field containing a cursor. The value of this field can be specified in the `cursor` query parameter of a subsequent request to retrieve the next page of objects. For convenience, an appropriate URI is constructed automatically by the broker and included in the `nextPageUri` field of the response. This URI can be used directly to retrieve the next page of objects.  ## Notes  Note|Description :---:|:--- 1|This specification defines SEMP starting in \"v2\", and not the original SEMP \"v1\" interface. Request and response formats between \"v1\" and \"v2\" are entirely incompatible, although both protocols share a common port configuration on the Solace PubSub+ broker. They are differentiated by the initial portion of the URI path, one of either \"/SEMP/\" or \"/SEMP/v2/\" 2|This API is partially implemented. Only a subset of all objects are available. 3|Read-only attributes may appear in POST and PUT/PATCH requests. However, if a read-only attribute is not marked as identifying, it will be ignored during a PUT/PATCH. 4|For PUT, if the SEMP user is not authorized to modify the attribute, its value is left unchanged rather than set to default. In addition, the values of write-only attributes are not set to their defaults on a PUT. If the object does not exist, it is created first.    
 *
 * OpenAPI spec version: 9.4
 * Contact: support@solace.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package com.solace.psg.sempv2.monitor.api;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;

import com.solace.psg.sempv2.apiclient.ApiCallback;
import com.solace.psg.sempv2.apiclient.ApiClient;
import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.apiclient.ApiResponse;
import com.solace.psg.sempv2.apiclient.Configuration;
import com.solace.psg.sempv2.apiclient.Pair;
import com.solace.psg.sempv2.apiclient.ProgressRequestBody;
import com.solace.psg.sempv2.apiclient.ProgressResponseBody;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfileClientConnectExceptionResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfileClientConnectExceptionsResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfilePublishExceptionResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfilePublishExceptionsResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfilePublishTopicExceptionResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfilePublishTopicExceptionsResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfileResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfileSubscribeExceptionResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfileSubscribeExceptionsResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfileSubscribeShareNameExceptionResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfileSubscribeShareNameExceptionsResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfileSubscribeTopicExceptionResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfileSubscribeTopicExceptionsResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnAclProfilesResponse;
import com.solace.psg.sempv2.monitor.model.SempMetaOnlyResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AclProfileApi {
    private ApiClient apiClient;

    public AclProfileApi() {
        this(Configuration.getDefaultApiClient());
    }

    public AclProfileApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for getMsgVpnAclProfile
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileCall(String msgVpnName, String aclProfileName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfileValidateBeforeCall(String msgVpnName, String aclProfileName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfile(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfile(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfileCall(msgVpnName, aclProfileName, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get an ACL Profile object.
     * Get an ACL Profile object.  An ACL Profile controls whether an authenticated client is permitted to establish a connection with the message broker or permitted to publish and subscribe to specific topics.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfileResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnAclProfileResponse getMsgVpnAclProfile(String msgVpnName, String aclProfileName, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfileResponse> resp = getMsgVpnAclProfileWithHttpInfo(msgVpnName, aclProfileName, select);
        return resp.getData();
    }

    /**
     * Get an ACL Profile object.
     * Get an ACL Profile object.  An ACL Profile controls whether an authenticated client is permitted to establish a connection with the message broker or permitted to publish and subscribe to specific topics.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfileResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnAclProfileResponse> getMsgVpnAclProfileWithHttpInfo(String msgVpnName, String aclProfileName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfileValidateBeforeCall(msgVpnName, aclProfileName, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get an ACL Profile object. (asynchronously)
     * Get an ACL Profile object.  An ACL Profile controls whether an authenticated client is permitted to establish a connection with the message broker or permitted to publish and subscribe to specific topics.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileAsync(String msgVpnName, String aclProfileName, List<String> select, final ApiCallback<MsgVpnAclProfileResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfileValidateBeforeCall(msgVpnName, aclProfileName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfileClientConnectException
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param clientConnectExceptionAddress The IP address/netmask of the client connect exception in CIDR form. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileClientConnectExceptionCall(String msgVpnName, String aclProfileName, String clientConnectExceptionAddress, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}/clientConnectExceptions/{clientConnectExceptionAddress}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()))
            .replaceAll("\\{" + "clientConnectExceptionAddress" + "\\}", apiClient.escapeString(clientConnectExceptionAddress.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfileClientConnectExceptionValidateBeforeCall(String msgVpnName, String aclProfileName, String clientConnectExceptionAddress, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfileClientConnectException(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfileClientConnectException(Async)");
        }
        
        // verify the required parameter 'clientConnectExceptionAddress' is set
        if (clientConnectExceptionAddress == null) {
            throw new ApiException("Missing the required parameter 'clientConnectExceptionAddress' when calling getMsgVpnAclProfileClientConnectException(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfileClientConnectExceptionCall(msgVpnName, aclProfileName, clientConnectExceptionAddress, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Client Connect Exception object.
     * Get a Client Connect Exception object.  A Client Connect Exception is an exception to the default action to take when a client using the ACL Profile connects to the Message VPN. Exceptions must be expressed as an IP address/netmask in CIDR form.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| clientConnectExceptionAddress|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param clientConnectExceptionAddress The IP address/netmask of the client connect exception in CIDR form. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfileClientConnectExceptionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnAclProfileClientConnectExceptionResponse getMsgVpnAclProfileClientConnectException(String msgVpnName, String aclProfileName, String clientConnectExceptionAddress, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfileClientConnectExceptionResponse> resp = getMsgVpnAclProfileClientConnectExceptionWithHttpInfo(msgVpnName, aclProfileName, clientConnectExceptionAddress, select);
        return resp.getData();
    }

    /**
     * Get a Client Connect Exception object.
     * Get a Client Connect Exception object.  A Client Connect Exception is an exception to the default action to take when a client using the ACL Profile connects to the Message VPN. Exceptions must be expressed as an IP address/netmask in CIDR form.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| clientConnectExceptionAddress|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param clientConnectExceptionAddress The IP address/netmask of the client connect exception in CIDR form. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfileClientConnectExceptionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnAclProfileClientConnectExceptionResponse> getMsgVpnAclProfileClientConnectExceptionWithHttpInfo(String msgVpnName, String aclProfileName, String clientConnectExceptionAddress, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfileClientConnectExceptionValidateBeforeCall(msgVpnName, aclProfileName, clientConnectExceptionAddress, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileClientConnectExceptionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Client Connect Exception object. (asynchronously)
     * Get a Client Connect Exception object.  A Client Connect Exception is an exception to the default action to take when a client using the ACL Profile connects to the Message VPN. Exceptions must be expressed as an IP address/netmask in CIDR form.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| clientConnectExceptionAddress|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param clientConnectExceptionAddress The IP address/netmask of the client connect exception in CIDR form. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileClientConnectExceptionAsync(String msgVpnName, String aclProfileName, String clientConnectExceptionAddress, List<String> select, final ApiCallback<MsgVpnAclProfileClientConnectExceptionResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfileClientConnectExceptionValidateBeforeCall(msgVpnName, aclProfileName, clientConnectExceptionAddress, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileClientConnectExceptionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfileClientConnectExceptions
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileClientConnectExceptionsCall(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}/clientConnectExceptions"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (where != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "where", where));
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfileClientConnectExceptionsValidateBeforeCall(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfileClientConnectExceptions(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfileClientConnectExceptions(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfileClientConnectExceptionsCall(msgVpnName, aclProfileName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Client Connect Exception objects.
     * Get a list of Client Connect Exception objects.  A Client Connect Exception is an exception to the default action to take when a client using the ACL Profile connects to the Message VPN. Exceptions must be expressed as an IP address/netmask in CIDR form.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| clientConnectExceptionAddress|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfileClientConnectExceptionsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnAclProfileClientConnectExceptionsResponse getMsgVpnAclProfileClientConnectExceptions(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfileClientConnectExceptionsResponse> resp = getMsgVpnAclProfileClientConnectExceptionsWithHttpInfo(msgVpnName, aclProfileName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Client Connect Exception objects.
     * Get a list of Client Connect Exception objects.  A Client Connect Exception is an exception to the default action to take when a client using the ACL Profile connects to the Message VPN. Exceptions must be expressed as an IP address/netmask in CIDR form.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| clientConnectExceptionAddress|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfileClientConnectExceptionsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnAclProfileClientConnectExceptionsResponse> getMsgVpnAclProfileClientConnectExceptionsWithHttpInfo(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfileClientConnectExceptionsValidateBeforeCall(msgVpnName, aclProfileName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileClientConnectExceptionsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Client Connect Exception objects. (asynchronously)
     * Get a list of Client Connect Exception objects.  A Client Connect Exception is an exception to the default action to take when a client using the ACL Profile connects to the Message VPN. Exceptions must be expressed as an IP address/netmask in CIDR form.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| clientConnectExceptionAddress|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileClientConnectExceptionsAsync(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnAclProfileClientConnectExceptionsResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfileClientConnectExceptionsValidateBeforeCall(msgVpnName, aclProfileName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileClientConnectExceptionsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfilePublishException
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param topicSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param publishExceptionTopic The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @deprecated
     */
    @Deprecated
    public com.squareup.okhttp.Call getMsgVpnAclProfilePublishExceptionCall(String msgVpnName, String aclProfileName, String topicSyntax, String publishExceptionTopic, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}/publishExceptions/{topicSyntax},{publishExceptionTopic}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()))
            .replaceAll("\\{" + "topicSyntax" + "\\}", apiClient.escapeString(topicSyntax.toString()))
            .replaceAll("\\{" + "publishExceptionTopic" + "\\}", apiClient.escapeString(publishExceptionTopic.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @Deprecated
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfilePublishExceptionValidateBeforeCall(String msgVpnName, String aclProfileName, String topicSyntax, String publishExceptionTopic, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfilePublishException(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfilePublishException(Async)");
        }
        
        // verify the required parameter 'topicSyntax' is set
        if (topicSyntax == null) {
            throw new ApiException("Missing the required parameter 'topicSyntax' when calling getMsgVpnAclProfilePublishException(Async)");
        }
        
        // verify the required parameter 'publishExceptionTopic' is set
        if (publishExceptionTopic == null) {
            throw new ApiException("Missing the required parameter 'publishExceptionTopic' when calling getMsgVpnAclProfilePublishException(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfilePublishExceptionCall(msgVpnName, aclProfileName, topicSyntax, publishExceptionTopic, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Publish Topic Exception object.
     * Get a Publish Topic Exception object.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x|x msgVpnName|x|x publishExceptionTopic|x|x topicSyntax|x|x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by publishTopicExceptions.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param topicSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param publishExceptionTopic The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfilePublishExceptionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @deprecated
     */
    @Deprecated
    public MsgVpnAclProfilePublishExceptionResponse getMsgVpnAclProfilePublishException(String msgVpnName, String aclProfileName, String topicSyntax, String publishExceptionTopic, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfilePublishExceptionResponse> resp = getMsgVpnAclProfilePublishExceptionWithHttpInfo(msgVpnName, aclProfileName, topicSyntax, publishExceptionTopic, select);
        return resp.getData();
    }

    /**
     * Get a Publish Topic Exception object.
     * Get a Publish Topic Exception object.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x|x msgVpnName|x|x publishExceptionTopic|x|x topicSyntax|x|x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by publishTopicExceptions.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param topicSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param publishExceptionTopic The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfilePublishExceptionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @deprecated
     */
    @Deprecated
    public ApiResponse<MsgVpnAclProfilePublishExceptionResponse> getMsgVpnAclProfilePublishExceptionWithHttpInfo(String msgVpnName, String aclProfileName, String topicSyntax, String publishExceptionTopic, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfilePublishExceptionValidateBeforeCall(msgVpnName, aclProfileName, topicSyntax, publishExceptionTopic, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfilePublishExceptionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Publish Topic Exception object. (asynchronously)
     * Get a Publish Topic Exception object.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x|x msgVpnName|x|x publishExceptionTopic|x|x topicSyntax|x|x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by publishTopicExceptions.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param topicSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param publishExceptionTopic The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @deprecated
     */
    @Deprecated
    public com.squareup.okhttp.Call getMsgVpnAclProfilePublishExceptionAsync(String msgVpnName, String aclProfileName, String topicSyntax, String publishExceptionTopic, List<String> select, final ApiCallback<MsgVpnAclProfilePublishExceptionResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfilePublishExceptionValidateBeforeCall(msgVpnName, aclProfileName, topicSyntax, publishExceptionTopic, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfilePublishExceptionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfilePublishExceptions
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @deprecated
     */
    @Deprecated
    public com.squareup.okhttp.Call getMsgVpnAclProfilePublishExceptionsCall(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}/publishExceptions"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (where != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "where", where));
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @Deprecated
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfilePublishExceptionsValidateBeforeCall(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfilePublishExceptions(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfilePublishExceptions(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfilePublishExceptionsCall(msgVpnName, aclProfileName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Publish Topic Exception objects.
     * Get a list of Publish Topic Exception objects.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x|x msgVpnName|x|x publishExceptionTopic|x|x topicSyntax|x|x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by publishTopicExceptions.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfilePublishExceptionsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @deprecated
     */
    @Deprecated
    public MsgVpnAclProfilePublishExceptionsResponse getMsgVpnAclProfilePublishExceptions(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfilePublishExceptionsResponse> resp = getMsgVpnAclProfilePublishExceptionsWithHttpInfo(msgVpnName, aclProfileName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Publish Topic Exception objects.
     * Get a list of Publish Topic Exception objects.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x|x msgVpnName|x|x publishExceptionTopic|x|x topicSyntax|x|x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by publishTopicExceptions.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfilePublishExceptionsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @deprecated
     */
    @Deprecated
    public ApiResponse<MsgVpnAclProfilePublishExceptionsResponse> getMsgVpnAclProfilePublishExceptionsWithHttpInfo(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfilePublishExceptionsValidateBeforeCall(msgVpnName, aclProfileName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfilePublishExceptionsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Publish Topic Exception objects. (asynchronously)
     * Get a list of Publish Topic Exception objects.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x|x msgVpnName|x|x publishExceptionTopic|x|x topicSyntax|x|x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by publishTopicExceptions.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @deprecated
     */
    @Deprecated
    public com.squareup.okhttp.Call getMsgVpnAclProfilePublishExceptionsAsync(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnAclProfilePublishExceptionsResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfilePublishExceptionsValidateBeforeCall(msgVpnName, aclProfileName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfilePublishExceptionsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfilePublishTopicException
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param publishTopicExceptionSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param publishTopicException The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfilePublishTopicExceptionCall(String msgVpnName, String aclProfileName, String publishTopicExceptionSyntax, String publishTopicException, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}/publishTopicExceptions/{publishTopicExceptionSyntax},{publishTopicException}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()))
            .replaceAll("\\{" + "publishTopicExceptionSyntax" + "\\}", apiClient.escapeString(publishTopicExceptionSyntax.toString()))
            .replaceAll("\\{" + "publishTopicException" + "\\}", apiClient.escapeString(publishTopicException.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfilePublishTopicExceptionValidateBeforeCall(String msgVpnName, String aclProfileName, String publishTopicExceptionSyntax, String publishTopicException, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfilePublishTopicException(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfilePublishTopicException(Async)");
        }
        
        // verify the required parameter 'publishTopicExceptionSyntax' is set
        if (publishTopicExceptionSyntax == null) {
            throw new ApiException("Missing the required parameter 'publishTopicExceptionSyntax' when calling getMsgVpnAclProfilePublishTopicException(Async)");
        }
        
        // verify the required parameter 'publishTopicException' is set
        if (publishTopicException == null) {
            throw new ApiException("Missing the required parameter 'publishTopicException' when calling getMsgVpnAclProfilePublishTopicException(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfilePublishTopicExceptionCall(msgVpnName, aclProfileName, publishTopicExceptionSyntax, publishTopicException, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Publish Topic Exception object.
     * Get a Publish Topic Exception object.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| publishTopicException|x| publishTopicExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param publishTopicExceptionSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param publishTopicException The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfilePublishTopicExceptionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnAclProfilePublishTopicExceptionResponse getMsgVpnAclProfilePublishTopicException(String msgVpnName, String aclProfileName, String publishTopicExceptionSyntax, String publishTopicException, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfilePublishTopicExceptionResponse> resp = getMsgVpnAclProfilePublishTopicExceptionWithHttpInfo(msgVpnName, aclProfileName, publishTopicExceptionSyntax, publishTopicException, select);
        return resp.getData();
    }

    /**
     * Get a Publish Topic Exception object.
     * Get a Publish Topic Exception object.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| publishTopicException|x| publishTopicExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param publishTopicExceptionSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param publishTopicException The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfilePublishTopicExceptionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnAclProfilePublishTopicExceptionResponse> getMsgVpnAclProfilePublishTopicExceptionWithHttpInfo(String msgVpnName, String aclProfileName, String publishTopicExceptionSyntax, String publishTopicException, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfilePublishTopicExceptionValidateBeforeCall(msgVpnName, aclProfileName, publishTopicExceptionSyntax, publishTopicException, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfilePublishTopicExceptionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Publish Topic Exception object. (asynchronously)
     * Get a Publish Topic Exception object.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| publishTopicException|x| publishTopicExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param publishTopicExceptionSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param publishTopicException The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfilePublishTopicExceptionAsync(String msgVpnName, String aclProfileName, String publishTopicExceptionSyntax, String publishTopicException, List<String> select, final ApiCallback<MsgVpnAclProfilePublishTopicExceptionResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfilePublishTopicExceptionValidateBeforeCall(msgVpnName, aclProfileName, publishTopicExceptionSyntax, publishTopicException, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfilePublishTopicExceptionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfilePublishTopicExceptions
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfilePublishTopicExceptionsCall(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}/publishTopicExceptions"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (where != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "where", where));
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfilePublishTopicExceptionsValidateBeforeCall(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfilePublishTopicExceptions(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfilePublishTopicExceptions(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfilePublishTopicExceptionsCall(msgVpnName, aclProfileName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Publish Topic Exception objects.
     * Get a list of Publish Topic Exception objects.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| publishTopicException|x| publishTopicExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfilePublishTopicExceptionsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnAclProfilePublishTopicExceptionsResponse getMsgVpnAclProfilePublishTopicExceptions(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfilePublishTopicExceptionsResponse> resp = getMsgVpnAclProfilePublishTopicExceptionsWithHttpInfo(msgVpnName, aclProfileName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Publish Topic Exception objects.
     * Get a list of Publish Topic Exception objects.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| publishTopicException|x| publishTopicExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfilePublishTopicExceptionsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnAclProfilePublishTopicExceptionsResponse> getMsgVpnAclProfilePublishTopicExceptionsWithHttpInfo(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfilePublishTopicExceptionsValidateBeforeCall(msgVpnName, aclProfileName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfilePublishTopicExceptionsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Publish Topic Exception objects. (asynchronously)
     * Get a list of Publish Topic Exception objects.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| publishTopicException|x| publishTopicExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfilePublishTopicExceptionsAsync(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnAclProfilePublishTopicExceptionsResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfilePublishTopicExceptionsValidateBeforeCall(msgVpnName, aclProfileName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfilePublishTopicExceptionsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfileSubscribeException
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param topicSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param subscribeExceptionTopic The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @deprecated
     */
    @Deprecated
    public com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeExceptionCall(String msgVpnName, String aclProfileName, String topicSyntax, String subscribeExceptionTopic, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}/subscribeExceptions/{topicSyntax},{subscribeExceptionTopic}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()))
            .replaceAll("\\{" + "topicSyntax" + "\\}", apiClient.escapeString(topicSyntax.toString()))
            .replaceAll("\\{" + "subscribeExceptionTopic" + "\\}", apiClient.escapeString(subscribeExceptionTopic.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @Deprecated
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeExceptionValidateBeforeCall(String msgVpnName, String aclProfileName, String topicSyntax, String subscribeExceptionTopic, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfileSubscribeException(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfileSubscribeException(Async)");
        }
        
        // verify the required parameter 'topicSyntax' is set
        if (topicSyntax == null) {
            throw new ApiException("Missing the required parameter 'topicSyntax' when calling getMsgVpnAclProfileSubscribeException(Async)");
        }
        
        // verify the required parameter 'subscribeExceptionTopic' is set
        if (subscribeExceptionTopic == null) {
            throw new ApiException("Missing the required parameter 'subscribeExceptionTopic' when calling getMsgVpnAclProfileSubscribeException(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeExceptionCall(msgVpnName, aclProfileName, topicSyntax, subscribeExceptionTopic, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Subscribe Topic Exception object.
     * Get a Subscribe Topic Exception object.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x|x msgVpnName|x|x subscribeExceptionTopic|x|x topicSyntax|x|x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by subscribeTopicExceptions.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param topicSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param subscribeExceptionTopic The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfileSubscribeExceptionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @deprecated
     */
    @Deprecated
    public MsgVpnAclProfileSubscribeExceptionResponse getMsgVpnAclProfileSubscribeException(String msgVpnName, String aclProfileName, String topicSyntax, String subscribeExceptionTopic, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfileSubscribeExceptionResponse> resp = getMsgVpnAclProfileSubscribeExceptionWithHttpInfo(msgVpnName, aclProfileName, topicSyntax, subscribeExceptionTopic, select);
        return resp.getData();
    }

    /**
     * Get a Subscribe Topic Exception object.
     * Get a Subscribe Topic Exception object.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x|x msgVpnName|x|x subscribeExceptionTopic|x|x topicSyntax|x|x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by subscribeTopicExceptions.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param topicSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param subscribeExceptionTopic The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfileSubscribeExceptionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @deprecated
     */
    @Deprecated
    public ApiResponse<MsgVpnAclProfileSubscribeExceptionResponse> getMsgVpnAclProfileSubscribeExceptionWithHttpInfo(String msgVpnName, String aclProfileName, String topicSyntax, String subscribeExceptionTopic, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeExceptionValidateBeforeCall(msgVpnName, aclProfileName, topicSyntax, subscribeExceptionTopic, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileSubscribeExceptionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Subscribe Topic Exception object. (asynchronously)
     * Get a Subscribe Topic Exception object.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x|x msgVpnName|x|x subscribeExceptionTopic|x|x topicSyntax|x|x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by subscribeTopicExceptions.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param topicSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param subscribeExceptionTopic The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @deprecated
     */
    @Deprecated
    public com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeExceptionAsync(String msgVpnName, String aclProfileName, String topicSyntax, String subscribeExceptionTopic, List<String> select, final ApiCallback<MsgVpnAclProfileSubscribeExceptionResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeExceptionValidateBeforeCall(msgVpnName, aclProfileName, topicSyntax, subscribeExceptionTopic, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileSubscribeExceptionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfileSubscribeExceptions
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @deprecated
     */
    @Deprecated
    public com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeExceptionsCall(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}/subscribeExceptions"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (where != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "where", where));
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @Deprecated
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeExceptionsValidateBeforeCall(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfileSubscribeExceptions(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfileSubscribeExceptions(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeExceptionsCall(msgVpnName, aclProfileName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Subscribe Topic Exception objects.
     * Get a list of Subscribe Topic Exception objects.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x|x msgVpnName|x|x subscribeExceptionTopic|x|x topicSyntax|x|x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by subscribeTopicExceptions.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfileSubscribeExceptionsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @deprecated
     */
    @Deprecated
    public MsgVpnAclProfileSubscribeExceptionsResponse getMsgVpnAclProfileSubscribeExceptions(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfileSubscribeExceptionsResponse> resp = getMsgVpnAclProfileSubscribeExceptionsWithHttpInfo(msgVpnName, aclProfileName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Subscribe Topic Exception objects.
     * Get a list of Subscribe Topic Exception objects.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x|x msgVpnName|x|x subscribeExceptionTopic|x|x topicSyntax|x|x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by subscribeTopicExceptions.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfileSubscribeExceptionsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @deprecated
     */
    @Deprecated
    public ApiResponse<MsgVpnAclProfileSubscribeExceptionsResponse> getMsgVpnAclProfileSubscribeExceptionsWithHttpInfo(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeExceptionsValidateBeforeCall(msgVpnName, aclProfileName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileSubscribeExceptionsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Subscribe Topic Exception objects. (asynchronously)
     * Get a list of Subscribe Topic Exception objects.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x|x msgVpnName|x|x subscribeExceptionTopic|x|x topicSyntax|x|x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by subscribeTopicExceptions.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @deprecated
     */
    @Deprecated
    public com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeExceptionsAsync(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnAclProfileSubscribeExceptionsResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeExceptionsValidateBeforeCall(msgVpnName, aclProfileName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileSubscribeExceptionsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfileSubscribeShareNameException
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param subscribeShareNameExceptionSyntax The syntax of the subscribe share name for the exception to the default action taken. (required)
     * @param subscribeShareNameException The subscribe share name exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeShareNameExceptionCall(String msgVpnName, String aclProfileName, String subscribeShareNameExceptionSyntax, String subscribeShareNameException, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}/subscribeShareNameExceptions/{subscribeShareNameExceptionSyntax},{subscribeShareNameException}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()))
            .replaceAll("\\{" + "subscribeShareNameExceptionSyntax" + "\\}", apiClient.escapeString(subscribeShareNameExceptionSyntax.toString()))
            .replaceAll("\\{" + "subscribeShareNameException" + "\\}", apiClient.escapeString(subscribeShareNameException.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeShareNameExceptionValidateBeforeCall(String msgVpnName, String aclProfileName, String subscribeShareNameExceptionSyntax, String subscribeShareNameException, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfileSubscribeShareNameException(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfileSubscribeShareNameException(Async)");
        }
        
        // verify the required parameter 'subscribeShareNameExceptionSyntax' is set
        if (subscribeShareNameExceptionSyntax == null) {
            throw new ApiException("Missing the required parameter 'subscribeShareNameExceptionSyntax' when calling getMsgVpnAclProfileSubscribeShareNameException(Async)");
        }
        
        // verify the required parameter 'subscribeShareNameException' is set
        if (subscribeShareNameException == null) {
            throw new ApiException("Missing the required parameter 'subscribeShareNameException' when calling getMsgVpnAclProfileSubscribeShareNameException(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeShareNameExceptionCall(msgVpnName, aclProfileName, subscribeShareNameExceptionSyntax, subscribeShareNameException, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Subscribe Share Name Exception object.
     * Get a Subscribe Share Name Exception object.  A Subscribe Share Name Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a share-name subscription in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| subscribeShareNameException|x| subscribeShareNameExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param subscribeShareNameExceptionSyntax The syntax of the subscribe share name for the exception to the default action taken. (required)
     * @param subscribeShareNameException The subscribe share name exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfileSubscribeShareNameExceptionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnAclProfileSubscribeShareNameExceptionResponse getMsgVpnAclProfileSubscribeShareNameException(String msgVpnName, String aclProfileName, String subscribeShareNameExceptionSyntax, String subscribeShareNameException, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfileSubscribeShareNameExceptionResponse> resp = getMsgVpnAclProfileSubscribeShareNameExceptionWithHttpInfo(msgVpnName, aclProfileName, subscribeShareNameExceptionSyntax, subscribeShareNameException, select);
        return resp.getData();
    }

    /**
     * Get a Subscribe Share Name Exception object.
     * Get a Subscribe Share Name Exception object.  A Subscribe Share Name Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a share-name subscription in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| subscribeShareNameException|x| subscribeShareNameExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param subscribeShareNameExceptionSyntax The syntax of the subscribe share name for the exception to the default action taken. (required)
     * @param subscribeShareNameException The subscribe share name exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfileSubscribeShareNameExceptionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnAclProfileSubscribeShareNameExceptionResponse> getMsgVpnAclProfileSubscribeShareNameExceptionWithHttpInfo(String msgVpnName, String aclProfileName, String subscribeShareNameExceptionSyntax, String subscribeShareNameException, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeShareNameExceptionValidateBeforeCall(msgVpnName, aclProfileName, subscribeShareNameExceptionSyntax, subscribeShareNameException, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileSubscribeShareNameExceptionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Subscribe Share Name Exception object. (asynchronously)
     * Get a Subscribe Share Name Exception object.  A Subscribe Share Name Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a share-name subscription in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| subscribeShareNameException|x| subscribeShareNameExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param subscribeShareNameExceptionSyntax The syntax of the subscribe share name for the exception to the default action taken. (required)
     * @param subscribeShareNameException The subscribe share name exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeShareNameExceptionAsync(String msgVpnName, String aclProfileName, String subscribeShareNameExceptionSyntax, String subscribeShareNameException, List<String> select, final ApiCallback<MsgVpnAclProfileSubscribeShareNameExceptionResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeShareNameExceptionValidateBeforeCall(msgVpnName, aclProfileName, subscribeShareNameExceptionSyntax, subscribeShareNameException, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileSubscribeShareNameExceptionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfileSubscribeShareNameExceptions
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeShareNameExceptionsCall(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}/subscribeShareNameExceptions"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (where != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "where", where));
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeShareNameExceptionsValidateBeforeCall(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfileSubscribeShareNameExceptions(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfileSubscribeShareNameExceptions(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeShareNameExceptionsCall(msgVpnName, aclProfileName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Subscribe Share Name Exception objects.
     * Get a list of Subscribe Share Name Exception objects.  A Subscribe Share Name Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a share-name subscription in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| subscribeShareNameException|x| subscribeShareNameExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfileSubscribeShareNameExceptionsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnAclProfileSubscribeShareNameExceptionsResponse getMsgVpnAclProfileSubscribeShareNameExceptions(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfileSubscribeShareNameExceptionsResponse> resp = getMsgVpnAclProfileSubscribeShareNameExceptionsWithHttpInfo(msgVpnName, aclProfileName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Subscribe Share Name Exception objects.
     * Get a list of Subscribe Share Name Exception objects.  A Subscribe Share Name Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a share-name subscription in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| subscribeShareNameException|x| subscribeShareNameExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfileSubscribeShareNameExceptionsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnAclProfileSubscribeShareNameExceptionsResponse> getMsgVpnAclProfileSubscribeShareNameExceptionsWithHttpInfo(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeShareNameExceptionsValidateBeforeCall(msgVpnName, aclProfileName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileSubscribeShareNameExceptionsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Subscribe Share Name Exception objects. (asynchronously)
     * Get a list of Subscribe Share Name Exception objects.  A Subscribe Share Name Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a share-name subscription in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| subscribeShareNameException|x| subscribeShareNameExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeShareNameExceptionsAsync(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnAclProfileSubscribeShareNameExceptionsResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeShareNameExceptionsValidateBeforeCall(msgVpnName, aclProfileName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileSubscribeShareNameExceptionsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfileSubscribeTopicException
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param subscribeTopicExceptionSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param subscribeTopicException The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeTopicExceptionCall(String msgVpnName, String aclProfileName, String subscribeTopicExceptionSyntax, String subscribeTopicException, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}/subscribeTopicExceptions/{subscribeTopicExceptionSyntax},{subscribeTopicException}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()))
            .replaceAll("\\{" + "subscribeTopicExceptionSyntax" + "\\}", apiClient.escapeString(subscribeTopicExceptionSyntax.toString()))
            .replaceAll("\\{" + "subscribeTopicException" + "\\}", apiClient.escapeString(subscribeTopicException.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeTopicExceptionValidateBeforeCall(String msgVpnName, String aclProfileName, String subscribeTopicExceptionSyntax, String subscribeTopicException, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfileSubscribeTopicException(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfileSubscribeTopicException(Async)");
        }
        
        // verify the required parameter 'subscribeTopicExceptionSyntax' is set
        if (subscribeTopicExceptionSyntax == null) {
            throw new ApiException("Missing the required parameter 'subscribeTopicExceptionSyntax' when calling getMsgVpnAclProfileSubscribeTopicException(Async)");
        }
        
        // verify the required parameter 'subscribeTopicException' is set
        if (subscribeTopicException == null) {
            throw new ApiException("Missing the required parameter 'subscribeTopicException' when calling getMsgVpnAclProfileSubscribeTopicException(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeTopicExceptionCall(msgVpnName, aclProfileName, subscribeTopicExceptionSyntax, subscribeTopicException, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Subscribe Topic Exception object.
     * Get a Subscribe Topic Exception object.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| subscribeTopicException|x| subscribeTopicExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param subscribeTopicExceptionSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param subscribeTopicException The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfileSubscribeTopicExceptionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnAclProfileSubscribeTopicExceptionResponse getMsgVpnAclProfileSubscribeTopicException(String msgVpnName, String aclProfileName, String subscribeTopicExceptionSyntax, String subscribeTopicException, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfileSubscribeTopicExceptionResponse> resp = getMsgVpnAclProfileSubscribeTopicExceptionWithHttpInfo(msgVpnName, aclProfileName, subscribeTopicExceptionSyntax, subscribeTopicException, select);
        return resp.getData();
    }

    /**
     * Get a Subscribe Topic Exception object.
     * Get a Subscribe Topic Exception object.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| subscribeTopicException|x| subscribeTopicExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param subscribeTopicExceptionSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param subscribeTopicException The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfileSubscribeTopicExceptionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnAclProfileSubscribeTopicExceptionResponse> getMsgVpnAclProfileSubscribeTopicExceptionWithHttpInfo(String msgVpnName, String aclProfileName, String subscribeTopicExceptionSyntax, String subscribeTopicException, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeTopicExceptionValidateBeforeCall(msgVpnName, aclProfileName, subscribeTopicExceptionSyntax, subscribeTopicException, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileSubscribeTopicExceptionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Subscribe Topic Exception object. (asynchronously)
     * Get a Subscribe Topic Exception object.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| subscribeTopicException|x| subscribeTopicExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param subscribeTopicExceptionSyntax The syntax of the topic for the exception to the default action taken. (required)
     * @param subscribeTopicException The topic for the exception to the default action taken. May include wildcard characters. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeTopicExceptionAsync(String msgVpnName, String aclProfileName, String subscribeTopicExceptionSyntax, String subscribeTopicException, List<String> select, final ApiCallback<MsgVpnAclProfileSubscribeTopicExceptionResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeTopicExceptionValidateBeforeCall(msgVpnName, aclProfileName, subscribeTopicExceptionSyntax, subscribeTopicException, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileSubscribeTopicExceptionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfileSubscribeTopicExceptions
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeTopicExceptionsCall(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles/{aclProfileName}/subscribeTopicExceptions"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "aclProfileName" + "\\}", apiClient.escapeString(aclProfileName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (where != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "where", where));
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeTopicExceptionsValidateBeforeCall(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfileSubscribeTopicExceptions(Async)");
        }
        
        // verify the required parameter 'aclProfileName' is set
        if (aclProfileName == null) {
            throw new ApiException("Missing the required parameter 'aclProfileName' when calling getMsgVpnAclProfileSubscribeTopicExceptions(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeTopicExceptionsCall(msgVpnName, aclProfileName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Subscribe Topic Exception objects.
     * Get a list of Subscribe Topic Exception objects.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| subscribeTopicException|x| subscribeTopicExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfileSubscribeTopicExceptionsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnAclProfileSubscribeTopicExceptionsResponse getMsgVpnAclProfileSubscribeTopicExceptions(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfileSubscribeTopicExceptionsResponse> resp = getMsgVpnAclProfileSubscribeTopicExceptionsWithHttpInfo(msgVpnName, aclProfileName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Subscribe Topic Exception objects.
     * Get a list of Subscribe Topic Exception objects.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| subscribeTopicException|x| subscribeTopicExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfileSubscribeTopicExceptionsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnAclProfileSubscribeTopicExceptionsResponse> getMsgVpnAclProfileSubscribeTopicExceptionsWithHttpInfo(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeTopicExceptionsValidateBeforeCall(msgVpnName, aclProfileName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileSubscribeTopicExceptionsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Subscribe Topic Exception objects. (asynchronously)
     * Get a list of Subscribe Topic Exception objects.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x| subscribeTopicException|x| subscribeTopicExceptionSyntax|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param aclProfileName The name of the ACL Profile. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfileSubscribeTopicExceptionsAsync(String msgVpnName, String aclProfileName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnAclProfileSubscribeTopicExceptionsResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfileSubscribeTopicExceptionsValidateBeforeCall(msgVpnName, aclProfileName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfileSubscribeTopicExceptionsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnAclProfiles
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfilesCall(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/aclProfiles"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (where != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "where", where));
        if (select != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("csv", "select", select));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnAclProfilesValidateBeforeCall(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnAclProfiles(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnAclProfilesCall(msgVpnName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of ACL Profile objects.
     * Get a list of ACL Profile objects.  An ACL Profile controls whether an authenticated client is permitted to establish a connection with the message broker or permitted to publish and subscribe to specific topics.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnAclProfilesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnAclProfilesResponse getMsgVpnAclProfiles(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnAclProfilesResponse> resp = getMsgVpnAclProfilesWithHttpInfo(msgVpnName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of ACL Profile objects.
     * Get a list of ACL Profile objects.  An ACL Profile controls whether an authenticated client is permitted to establish a connection with the message broker or permitted to publish and subscribe to specific topics.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnAclProfilesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnAclProfilesResponse> getMsgVpnAclProfilesWithHttpInfo(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnAclProfilesValidateBeforeCall(msgVpnName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfilesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of ACL Profile objects. (asynchronously)
     * Get a list of ACL Profile objects.  An ACL Profile controls whether an authenticated client is permitted to establish a connection with the message broker or permitted to publish and subscribe to specific topics.   Attribute|Identifying|Deprecated :---|:---:|:---: aclProfileName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnAclProfilesAsync(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnAclProfilesResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getMsgVpnAclProfilesValidateBeforeCall(msgVpnName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnAclProfilesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
