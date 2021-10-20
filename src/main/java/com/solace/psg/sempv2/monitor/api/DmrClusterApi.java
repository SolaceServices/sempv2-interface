/*
 * SEMP (Solace Element Management Protocol)
 * SEMP (starting in `v2`, see note 1) is a RESTful API for configuring, monitoring, and administering a Solace PubSub+ broker.  SEMP uses URIs to address manageable **resources** of the Solace PubSub+ broker. Resources are individual **objects**, **collections** of objects, or (exclusively in the action API) **actions**. This document applies to the following API:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Monitoring|/SEMP/v2/monitor|Querying operational parameters|See note 2    The following APIs are also available:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Action|/SEMP/v2/action|Performing actions|See note 2 Configuration|/SEMP/v2/config|Reading and writing config state|See note 2    Resources are always nouns, with individual objects being singular and collections being plural.  Objects within a collection are identified by an `obj-id`, which follows the collection name with the form `collection-name/obj-id`.  Actions within an object are identified by an `action-id`, which follows the object name with the form `obj-id/action-id`.  Some examples:  ``` /SEMP/v2/config/msgVpns                        ; MsgVpn collection /SEMP/v2/config/msgVpns/a                      ; MsgVpn object named \"a\" /SEMP/v2/config/msgVpns/a/queues               ; Queue collection in MsgVpn \"a\" /SEMP/v2/config/msgVpns/a/queues/b             ; Queue object named \"b\" in MsgVpn \"a\" /SEMP/v2/action/msgVpns/a/queues/b/startReplay ; Action that starts a replay on Queue \"b\" in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients             ; Client collection in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients/c           ; Client object named \"c\" in MsgVpn \"a\" ```  ## Collection Resources  Collections are unordered lists of objects (unless described as otherwise), and are described by JSON arrays. Each item in the array represents an object in the same manner as the individual object would normally be represented. In the configuration API, the creation of a new object is done through its collection resource.  ## Object and Action Resources  Objects are composed of attributes, actions, collections, and other objects. They are described by JSON objects as name/value pairs. The collections and actions of an object are not contained directly in the object's JSON content; rather the content includes an attribute containing a URI which points to the collections and actions. These contained resources must be managed through this URI. At a minimum, every object has one or more identifying attributes, and its own `uri` attribute which contains the URI pointing to itself.  Actions are also composed of attributes, and are described by JSON objects as name/value pairs. Unlike objects, however, they are not members of a collection and cannot be retrieved, only performed. Actions only exist in the action API.  Attributes in an object or action may have any combination of the following properties:   Property|Meaning|Comments :---|:---|:--- Identifying|Attribute is involved in unique identification of the object, and appears in its URI| Required|Attribute must be provided in the request| Read-Only|Attribute can only be read, not written.|See note 3 Write-Only|Attribute can only be written, not read, unless the attribute is also opaque|See the documentation for the opaque property Requires-Disable|Attribute can only be changed when object is disabled| Deprecated|Attribute is deprecated, and will disappear in the next SEMP version| Opaque|Attribute can be set or retrieved in opaque form when the `opaquePassword` query parameter is present|See the `opaquePassword` query parameter documentation    In some requests, certain attributes may only be provided in certain combinations with other attributes:   Relationship|Meaning :---|:--- Requires|Attribute may only be changed by a request if a particular attribute or combination of attributes is also provided in the request Conflicts|Attribute may only be provided in a request if a particular attribute or combination of attributes is not also provided in the request    In the monitoring API, any non-identifying attribute may not be returned in a GET.  ## HTTP Methods  The following HTTP methods manipulate resources in accordance with these general principles. Note that some methods are only used in certain APIs:   Method|Resource|Meaning|Request Body|Response Body|Missing Request Attributes :---|:---|:---|:---|:---|:--- POST|Collection|Create object|Initial attribute values|Object attributes and metadata|Set to default PUT|Object|Create or replace object (see note 5)|New attribute values|Object attributes and metadata|Set to default, with certain exceptions (see note 4) PUT|Action|Performs action|Action arguments|Action metadata|N/A PATCH|Object|Update object|New attribute values|Object attributes and metadata|unchanged DELETE|Object|Delete object|Empty|Object metadata|N/A GET|Object|Get object|Empty|Object attributes and metadata|N/A GET|Collection|Get collection|Empty|Object attributes and collection metadata|N/A    ## Common Query Parameters  The following are some common query parameters that are supported by many method/URI combinations. Individual URIs may document additional parameters. Note that multiple query parameters can be used together in a single URI, separated by the ampersand character. For example:  ``` ; Request for the MsgVpns collection using two hypothetical query parameters ; \"q1\" and \"q2\" with values \"val1\" and \"val2\" respectively /SEMP/v2/monitor/msgVpns?q1=val1&q2=val2 ```  ### select  Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. Use this query parameter to limit the size of the returned data for each returned object, return only those fields that are desired, or exclude fields that are not desired.  The value of `select` is a comma-separated list of attribute names. If the list contains attribute names that are not prefaced by `-`, only those attributes are included in the response. If the list contains attribute names that are prefaced by `-`, those attributes are excluded from the response. If the list contains both types, then the difference of the first set of attributes and the second set of attributes is returned. If the list is empty (i.e. `select=`), no attributes are returned.  All attributes that are prefaced by `-` must follow all attributes that are not prefaced by `-`. In addition, each attribute name in the list must match at least one attribute in the object.  Names may include the `*` wildcard (zero or more characters). Nested attribute names are supported using periods (e.g. `parentName.childName`).  Some examples:  ``` ; List of all MsgVpn names /SEMP/v2/monitor/msgVpns?select=msgVpnName ; List of all MsgVpn and their attributes except for their names /SEMP/v2/monitor/msgVpns?select=-msgVpnName ; Authentication attributes of MsgVpn \"finance\" /SEMP/v2/monitor/msgVpns/finance?select=authentication* ; All attributes of MsgVpn \"finance\" except for authentication attributes /SEMP/v2/monitor/msgVpns/finance?select=-authentication* ; Access related attributes of Queue \"orderQ\" of MsgVpn \"finance\" /SEMP/v2/monitor/msgVpns/finance/queues/orderQ?select=owner,permission ```  ### where  Include in the response only objects where certain conditions are true. Use this query parameter to limit which objects are returned to those whose attribute values meet the given conditions.  The value of `where` is a comma-separated list of expressions. All expressions must be true for the object to be included in the response. Each expression takes the form:  ``` expression  = attribute-name OP value OP          = '==' | '!=' | '&lt;' | '&gt;' | '&lt;=' | '&gt;=' ```  `value` may be a number, string, `true`, or `false`, as appropriate for the type of `attribute-name`. Greater-than and less-than comparisons only work for numbers. A `*` in a string `value` is interpreted as a wildcard (zero or more characters). Some examples:  ``` ; Only enabled MsgVpns /SEMP/v2/monitor/msgVpns?where=enabled==true ; Only MsgVpns using basic non-LDAP authentication /SEMP/v2/monitor/msgVpns?where=authenticationBasicEnabled==true,authenticationBasicType!=ldap ; Only MsgVpns that allow more than 100 client connections /SEMP/v2/monitor/msgVpns?where=maxConnectionCount>100 ; Only MsgVpns with msgVpnName starting with \"B\": /SEMP/v2/monitor/msgVpns?where=msgVpnName==B* ```  ### count  Limit the count of objects in the response. This can be useful to limit the size of the response for large collections. The minimum value for `count` is `1` and the default is `10`. There is also a per-collection maximum value to limit request handling time. For example:  ``` ; Up to 25 MsgVpns /SEMP/v2/monitor/msgVpns?count=25 ```  ### cursor  The cursor, or position, for the next page of objects. Cursors are opaque data that should not be created or interpreted by SEMP clients, and should only be used as described below.  When a request is made for a collection and there may be additional objects available for retrieval that are not included in the initial response, the response will include a `cursorQuery` field containing a cursor. The value of this field can be specified in the `cursor` query parameter of a subsequent request to retrieve the next page of objects. For convenience, an appropriate URI is constructed automatically by the broker and included in the `nextPageUri` field of the response. This URI can be used directly to retrieve the next page of objects.  ### opaquePassword  Attributes with the opaque property are also write-only and so cannot normally be retrieved in a GET. However, when a password is provided in the `opaquePassword` query parameter, attributes with the opaque property are retrieved in a GET in opaque form, encrypted with this password. The query parameter can also be used on a POST, PATCH, or PUT to set opaque attributes using opaque attribute values retrieved in a GET, so long as:  1. the same password that was used to retrieve the opaque attribute values is provided; and  2. the broker to which the request is being sent has the same major and minor SEMP version as the broker that produced the opaque attribute values.  The password provided in the query parameter must be a minimum of 8 characters and a maximum of 128 characters.  The query parameter can only be used in the configuration API, and only over HTTPS.  ## Help  Visit [our website](https://solace.com) to learn more about Solace.  You can also download the SEMP API specifications by clicking [here](https://solace.com/downloads/).  If you need additional support, please contact us at [support@solace.com](mailto:support@solace.com).  ## Notes  Note|Description :---:|:--- 1|This specification defines SEMP starting in \"v2\", and not the original SEMP \"v1\" interface. Request and response formats between \"v1\" and \"v2\" are entirely incompatible, although both protocols share a common port configuration on the Solace PubSub+ broker. They are differentiated by the initial portion of the URI path, one of either \"/SEMP/\" or \"/SEMP/v2/\" 2|This API is partially implemented. Only a subset of all objects are available. 3|Read-only attributes may appear in POST and PUT/PATCH requests. However, if a read-only attribute is not marked as identifying, it will be ignored during a PUT/PATCH. 4|On a PUT, if the SEMP user is not authorized to modify the attribute, its value is left unchanged rather than set to default. In addition, the values of write-only attributes are not set to their defaults on a PUT, except in the following two cases: there is a mutual requires relationship with another non-write-only attribute and both attributes are absent from the request; or the attribute is also opaque and the `opaquePassword` query parameter is provided in the request. 5|On a PUT, if the object does not exist, it is created first.  
 *
 * OpenAPI spec version: 2.17
 * Contact: support@solace.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.solace.psg.sempv2.monitor.api;

import com.solace.psg.sempv2.apiclient.ApiCallback;
import com.solace.psg.sempv2.apiclient.ApiClient;
import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.apiclient.ApiResponse;
import com.solace.psg.sempv2.apiclient.Configuration;
import com.solace.psg.sempv2.apiclient.Pair;
import com.solace.psg.sempv2.apiclient.ProgressRequestBody;
import com.solace.psg.sempv2.apiclient.ProgressResponseBody;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;


import com.solace.psg.sempv2.monitor.model.DmrClusterLinkChannelResponse;
import com.solace.psg.sempv2.monitor.model.DmrClusterLinkChannelsResponse;
import com.solace.psg.sempv2.monitor.model.DmrClusterLinkRemoteAddressResponse;
import com.solace.psg.sempv2.monitor.model.DmrClusterLinkRemoteAddressesResponse;
import com.solace.psg.sempv2.monitor.model.DmrClusterLinkResponse;
import com.solace.psg.sempv2.monitor.model.DmrClusterLinkTlsTrustedCommonNameResponse;
import com.solace.psg.sempv2.monitor.model.DmrClusterLinkTlsTrustedCommonNamesResponse;
import com.solace.psg.sempv2.monitor.model.DmrClusterLinksResponse;
import com.solace.psg.sempv2.monitor.model.DmrClusterResponse;
import com.solace.psg.sempv2.monitor.model.DmrClusterTopologyIssueResponse;
import com.solace.psg.sempv2.monitor.model.DmrClusterTopologyIssuesResponse;
import com.solace.psg.sempv2.monitor.model.DmrClustersResponse;
import com.solace.psg.sempv2.monitor.model.SempMetaOnlyResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DmrClusterApi {
    private ApiClient apiClient;

    public DmrClusterApi() {
        this(Configuration.getDefaultApiClient());
    }

    public DmrClusterApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for getDmrCluster
     * @param dmrClusterName The name of the Cluster. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterCall(String dmrClusterName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()));

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
    private com.squareup.okhttp.Call getDmrClusterValidateBeforeCall(String dmrClusterName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling getDmrCluster(Async)");
        }
        
        com.squareup.okhttp.Call call = getDmrClusterCall(dmrClusterName, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a Cluster object.
     * Get a Cluster object.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterResponse getDmrCluster(String dmrClusterName, List<String> select) throws ApiException {
        ApiResponse<DmrClusterResponse> resp = getDmrClusterWithHttpInfo(dmrClusterName, select);
        return resp.getData();
    }

    /**
     * Get a Cluster object.
     * Get a Cluster object.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterResponse> getDmrClusterWithHttpInfo(String dmrClusterName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getDmrClusterValidateBeforeCall(dmrClusterName, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Cluster object. (asynchronously)
     * Get a Cluster object.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterAsync(String dmrClusterName, List<String> select, final ApiCallback<DmrClusterResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getDmrClusterValidateBeforeCall(dmrClusterName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getDmrClusterLink
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkCall(String dmrClusterName, String remoteNodeName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links/{remoteNodeName}"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()))
            .replaceAll("\\{" + "remoteNodeName" + "\\}", apiClient.escapeString(remoteNodeName.toString()));

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
    private com.squareup.okhttp.Call getDmrClusterLinkValidateBeforeCall(String dmrClusterName, String remoteNodeName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling getDmrClusterLink(Async)");
        }
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling getDmrClusterLink(Async)");
        }
        
        com.squareup.okhttp.Call call = getDmrClusterLinkCall(dmrClusterName, remoteNodeName, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a Link object.
     * Get a Link object.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinkResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinkResponse getDmrClusterLink(String dmrClusterName, String remoteNodeName, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinkResponse> resp = getDmrClusterLinkWithHttpInfo(dmrClusterName, remoteNodeName, select);
        return resp.getData();
    }

    /**
     * Get a Link object.
     * Get a Link object.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinkResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinkResponse> getDmrClusterLinkWithHttpInfo(String dmrClusterName, String remoteNodeName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getDmrClusterLinkValidateBeforeCall(dmrClusterName, remoteNodeName, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinkResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Link object. (asynchronously)
     * Get a Link object.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkAsync(String dmrClusterName, String remoteNodeName, List<String> select, final ApiCallback<DmrClusterLinkResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getDmrClusterLinkValidateBeforeCall(dmrClusterName, remoteNodeName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinkResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getDmrClusterLinkChannel
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkChannelCall(String dmrClusterName, String remoteNodeName, String msgVpnName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links/{remoteNodeName}/channels/{msgVpnName}"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()))
            .replaceAll("\\{" + "remoteNodeName" + "\\}", apiClient.escapeString(remoteNodeName.toString()))
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()));

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
    private com.squareup.okhttp.Call getDmrClusterLinkChannelValidateBeforeCall(String dmrClusterName, String remoteNodeName, String msgVpnName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling getDmrClusterLinkChannel(Async)");
        }
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling getDmrClusterLinkChannel(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getDmrClusterLinkChannel(Async)");
        }
        
        com.squareup.okhttp.Call call = getDmrClusterLinkChannelCall(dmrClusterName, remoteNodeName, msgVpnName, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a Cluster Link Channels object.
     * Get a Cluster Link Channels object.  A Channel is a connection between this broker and a remote node in the Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| msgVpnName|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinkChannelResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinkChannelResponse getDmrClusterLinkChannel(String dmrClusterName, String remoteNodeName, String msgVpnName, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinkChannelResponse> resp = getDmrClusterLinkChannelWithHttpInfo(dmrClusterName, remoteNodeName, msgVpnName, select);
        return resp.getData();
    }

    /**
     * Get a Cluster Link Channels object.
     * Get a Cluster Link Channels object.  A Channel is a connection between this broker and a remote node in the Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| msgVpnName|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinkChannelResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinkChannelResponse> getDmrClusterLinkChannelWithHttpInfo(String dmrClusterName, String remoteNodeName, String msgVpnName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getDmrClusterLinkChannelValidateBeforeCall(dmrClusterName, remoteNodeName, msgVpnName, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinkChannelResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Cluster Link Channels object. (asynchronously)
     * Get a Cluster Link Channels object.  A Channel is a connection between this broker and a remote node in the Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| msgVpnName|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkChannelAsync(String dmrClusterName, String remoteNodeName, String msgVpnName, List<String> select, final ApiCallback<DmrClusterLinkChannelResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getDmrClusterLinkChannelValidateBeforeCall(dmrClusterName, remoteNodeName, msgVpnName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinkChannelResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getDmrClusterLinkChannels
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkChannelsCall(String dmrClusterName, String remoteNodeName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links/{remoteNodeName}/channels"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()))
            .replaceAll("\\{" + "remoteNodeName" + "\\}", apiClient.escapeString(remoteNodeName.toString()));

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
    private com.squareup.okhttp.Call getDmrClusterLinkChannelsValidateBeforeCall(String dmrClusterName, String remoteNodeName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling getDmrClusterLinkChannels(Async)");
        }
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling getDmrClusterLinkChannels(Async)");
        }
        
        com.squareup.okhttp.Call call = getDmrClusterLinkChannelsCall(dmrClusterName, remoteNodeName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a list of Cluster Link Channels objects.
     * Get a list of Cluster Link Channels objects.  A Channel is a connection between this broker and a remote node in the Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| msgVpnName|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinkChannelsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinkChannelsResponse getDmrClusterLinkChannels(String dmrClusterName, String remoteNodeName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinkChannelsResponse> resp = getDmrClusterLinkChannelsWithHttpInfo(dmrClusterName, remoteNodeName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Cluster Link Channels objects.
     * Get a list of Cluster Link Channels objects.  A Channel is a connection between this broker and a remote node in the Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| msgVpnName|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinkChannelsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinkChannelsResponse> getDmrClusterLinkChannelsWithHttpInfo(String dmrClusterName, String remoteNodeName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getDmrClusterLinkChannelsValidateBeforeCall(dmrClusterName, remoteNodeName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinkChannelsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Cluster Link Channels objects. (asynchronously)
     * Get a list of Cluster Link Channels objects.  A Channel is a connection between this broker and a remote node in the Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| msgVpnName|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkChannelsAsync(String dmrClusterName, String remoteNodeName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<DmrClusterLinkChannelsResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getDmrClusterLinkChannelsValidateBeforeCall(dmrClusterName, remoteNodeName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinkChannelsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getDmrClusterLinkRemoteAddress
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param remoteAddress The FQDN or IP address (and optional port) of the remote node. If a port is not provided, it will vary based on the transport encoding: 55555 (plain-text), 55443 (encrypted), or 55003 (compressed). (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkRemoteAddressCall(String dmrClusterName, String remoteNodeName, String remoteAddress, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links/{remoteNodeName}/remoteAddresses/{remoteAddress}"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()))
            .replaceAll("\\{" + "remoteNodeName" + "\\}", apiClient.escapeString(remoteNodeName.toString()))
            .replaceAll("\\{" + "remoteAddress" + "\\}", apiClient.escapeString(remoteAddress.toString()));

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
    private com.squareup.okhttp.Call getDmrClusterLinkRemoteAddressValidateBeforeCall(String dmrClusterName, String remoteNodeName, String remoteAddress, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling getDmrClusterLinkRemoteAddress(Async)");
        }
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling getDmrClusterLinkRemoteAddress(Async)");
        }
        // verify the required parameter 'remoteAddress' is set
        if (remoteAddress == null) {
            throw new ApiException("Missing the required parameter 'remoteAddress' when calling getDmrClusterLinkRemoteAddress(Async)");
        }
        
        com.squareup.okhttp.Call call = getDmrClusterLinkRemoteAddressCall(dmrClusterName, remoteNodeName, remoteAddress, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a Remote Address object.
     * Get a Remote Address object.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteAddress|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param remoteAddress The FQDN or IP address (and optional port) of the remote node. If a port is not provided, it will vary based on the transport encoding: 55555 (plain-text), 55443 (encrypted), or 55003 (compressed). (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinkRemoteAddressResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinkRemoteAddressResponse getDmrClusterLinkRemoteAddress(String dmrClusterName, String remoteNodeName, String remoteAddress, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinkRemoteAddressResponse> resp = getDmrClusterLinkRemoteAddressWithHttpInfo(dmrClusterName, remoteNodeName, remoteAddress, select);
        return resp.getData();
    }

    /**
     * Get a Remote Address object.
     * Get a Remote Address object.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteAddress|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param remoteAddress The FQDN or IP address (and optional port) of the remote node. If a port is not provided, it will vary based on the transport encoding: 55555 (plain-text), 55443 (encrypted), or 55003 (compressed). (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinkRemoteAddressResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinkRemoteAddressResponse> getDmrClusterLinkRemoteAddressWithHttpInfo(String dmrClusterName, String remoteNodeName, String remoteAddress, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getDmrClusterLinkRemoteAddressValidateBeforeCall(dmrClusterName, remoteNodeName, remoteAddress, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinkRemoteAddressResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Remote Address object. (asynchronously)
     * Get a Remote Address object.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteAddress|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param remoteAddress The FQDN or IP address (and optional port) of the remote node. If a port is not provided, it will vary based on the transport encoding: 55555 (plain-text), 55443 (encrypted), or 55003 (compressed). (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkRemoteAddressAsync(String dmrClusterName, String remoteNodeName, String remoteAddress, List<String> select, final ApiCallback<DmrClusterLinkRemoteAddressResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getDmrClusterLinkRemoteAddressValidateBeforeCall(dmrClusterName, remoteNodeName, remoteAddress, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinkRemoteAddressResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getDmrClusterLinkRemoteAddresses
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkRemoteAddressesCall(String dmrClusterName, String remoteNodeName, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links/{remoteNodeName}/remoteAddresses"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()))
            .replaceAll("\\{" + "remoteNodeName" + "\\}", apiClient.escapeString(remoteNodeName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
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
    private com.squareup.okhttp.Call getDmrClusterLinkRemoteAddressesValidateBeforeCall(String dmrClusterName, String remoteNodeName, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling getDmrClusterLinkRemoteAddresses(Async)");
        }
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling getDmrClusterLinkRemoteAddresses(Async)");
        }
        
        com.squareup.okhttp.Call call = getDmrClusterLinkRemoteAddressesCall(dmrClusterName, remoteNodeName, where, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a list of Remote Address objects.
     * Get a list of Remote Address objects.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteAddress|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinkRemoteAddressesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinkRemoteAddressesResponse getDmrClusterLinkRemoteAddresses(String dmrClusterName, String remoteNodeName, List<String> where, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinkRemoteAddressesResponse> resp = getDmrClusterLinkRemoteAddressesWithHttpInfo(dmrClusterName, remoteNodeName, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Remote Address objects.
     * Get a list of Remote Address objects.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteAddress|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinkRemoteAddressesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinkRemoteAddressesResponse> getDmrClusterLinkRemoteAddressesWithHttpInfo(String dmrClusterName, String remoteNodeName, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getDmrClusterLinkRemoteAddressesValidateBeforeCall(dmrClusterName, remoteNodeName, where, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinkRemoteAddressesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Remote Address objects. (asynchronously)
     * Get a list of Remote Address objects.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteAddress|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkRemoteAddressesAsync(String dmrClusterName, String remoteNodeName, List<String> where, List<String> select, final ApiCallback<DmrClusterLinkRemoteAddressesResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getDmrClusterLinkRemoteAddressesValidateBeforeCall(dmrClusterName, remoteNodeName, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinkRemoteAddressesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getDmrClusterLinkTlsTrustedCommonName
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkTlsTrustedCommonNameCall(String dmrClusterName, String remoteNodeName, String tlsTrustedCommonName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links/{remoteNodeName}/tlsTrustedCommonNames/{tlsTrustedCommonName}"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()))
            .replaceAll("\\{" + "remoteNodeName" + "\\}", apiClient.escapeString(remoteNodeName.toString()))
            .replaceAll("\\{" + "tlsTrustedCommonName" + "\\}", apiClient.escapeString(tlsTrustedCommonName.toString()));

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
    private com.squareup.okhttp.Call getDmrClusterLinkTlsTrustedCommonNameValidateBeforeCall(String dmrClusterName, String remoteNodeName, String tlsTrustedCommonName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling getDmrClusterLinkTlsTrustedCommonName(Async)");
        }
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling getDmrClusterLinkTlsTrustedCommonName(Async)");
        }
        // verify the required parameter 'tlsTrustedCommonName' is set
        if (tlsTrustedCommonName == null) {
            throw new ApiException("Missing the required parameter 'tlsTrustedCommonName' when calling getDmrClusterLinkTlsTrustedCommonName(Async)");
        }
        
        com.squareup.okhttp.Call call = getDmrClusterLinkTlsTrustedCommonNameCall(dmrClusterName, remoteNodeName, tlsTrustedCommonName, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a Trusted Common Name object.
     * Get a Trusted Common Name object.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#x27;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteNodeName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinkTlsTrustedCommonNameResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinkTlsTrustedCommonNameResponse getDmrClusterLinkTlsTrustedCommonName(String dmrClusterName, String remoteNodeName, String tlsTrustedCommonName, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinkTlsTrustedCommonNameResponse> resp = getDmrClusterLinkTlsTrustedCommonNameWithHttpInfo(dmrClusterName, remoteNodeName, tlsTrustedCommonName, select);
        return resp.getData();
    }

    /**
     * Get a Trusted Common Name object.
     * Get a Trusted Common Name object.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#x27;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteNodeName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinkTlsTrustedCommonNameResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinkTlsTrustedCommonNameResponse> getDmrClusterLinkTlsTrustedCommonNameWithHttpInfo(String dmrClusterName, String remoteNodeName, String tlsTrustedCommonName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getDmrClusterLinkTlsTrustedCommonNameValidateBeforeCall(dmrClusterName, remoteNodeName, tlsTrustedCommonName, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinkTlsTrustedCommonNameResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Trusted Common Name object. (asynchronously)
     * Get a Trusted Common Name object.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#x27;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteNodeName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkTlsTrustedCommonNameAsync(String dmrClusterName, String remoteNodeName, String tlsTrustedCommonName, List<String> select, final ApiCallback<DmrClusterLinkTlsTrustedCommonNameResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getDmrClusterLinkTlsTrustedCommonNameValidateBeforeCall(dmrClusterName, remoteNodeName, tlsTrustedCommonName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinkTlsTrustedCommonNameResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getDmrClusterLinkTlsTrustedCommonNames
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkTlsTrustedCommonNamesCall(String dmrClusterName, String remoteNodeName, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links/{remoteNodeName}/tlsTrustedCommonNames"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()))
            .replaceAll("\\{" + "remoteNodeName" + "\\}", apiClient.escapeString(remoteNodeName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
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
    private com.squareup.okhttp.Call getDmrClusterLinkTlsTrustedCommonNamesValidateBeforeCall(String dmrClusterName, String remoteNodeName, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling getDmrClusterLinkTlsTrustedCommonNames(Async)");
        }
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling getDmrClusterLinkTlsTrustedCommonNames(Async)");
        }
        
        com.squareup.okhttp.Call call = getDmrClusterLinkTlsTrustedCommonNamesCall(dmrClusterName, remoteNodeName, where, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a list of Trusted Common Name objects.
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#x27;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteNodeName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinkTlsTrustedCommonNamesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinkTlsTrustedCommonNamesResponse getDmrClusterLinkTlsTrustedCommonNames(String dmrClusterName, String remoteNodeName, List<String> where, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinkTlsTrustedCommonNamesResponse> resp = getDmrClusterLinkTlsTrustedCommonNamesWithHttpInfo(dmrClusterName, remoteNodeName, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Trusted Common Name objects.
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#x27;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteNodeName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinkTlsTrustedCommonNamesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinkTlsTrustedCommonNamesResponse> getDmrClusterLinkTlsTrustedCommonNamesWithHttpInfo(String dmrClusterName, String remoteNodeName, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getDmrClusterLinkTlsTrustedCommonNamesValidateBeforeCall(dmrClusterName, remoteNodeName, where, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinkTlsTrustedCommonNamesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Trusted Common Name objects. (asynchronously)
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#x27;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteNodeName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinkTlsTrustedCommonNamesAsync(String dmrClusterName, String remoteNodeName, List<String> where, List<String> select, final ApiCallback<DmrClusterLinkTlsTrustedCommonNamesResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getDmrClusterLinkTlsTrustedCommonNamesValidateBeforeCall(dmrClusterName, remoteNodeName, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinkTlsTrustedCommonNamesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getDmrClusterLinks
     * @param dmrClusterName The name of the Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinksCall(String dmrClusterName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()));

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
    private com.squareup.okhttp.Call getDmrClusterLinksValidateBeforeCall(String dmrClusterName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling getDmrClusterLinks(Async)");
        }
        
        com.squareup.okhttp.Call call = getDmrClusterLinksCall(dmrClusterName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a list of Link objects.
     * Get a list of Link objects.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinksResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinksResponse getDmrClusterLinks(String dmrClusterName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinksResponse> resp = getDmrClusterLinksWithHttpInfo(dmrClusterName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Link objects.
     * Get a list of Link objects.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinksResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinksResponse> getDmrClusterLinksWithHttpInfo(String dmrClusterName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getDmrClusterLinksValidateBeforeCall(dmrClusterName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinksResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Link objects. (asynchronously)
     * Get a list of Link objects.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| remoteNodeName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterLinksAsync(String dmrClusterName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<DmrClusterLinksResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getDmrClusterLinksValidateBeforeCall(dmrClusterName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinksResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getDmrClusterTopologyIssue
     * @param dmrClusterName The name of the Cluster. (required)
     * @param topologyIssue The topology issue discovered in the Cluster. A topology issue indicates incorrect or inconsistent configuration within the DMR network. Such issues will cause messages to be misdelivered or lost. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterTopologyIssueCall(String dmrClusterName, String topologyIssue, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/topologyIssues/{topologyIssue}"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()))
            .replaceAll("\\{" + "topologyIssue" + "\\}", apiClient.escapeString(topologyIssue.toString()));

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
    private com.squareup.okhttp.Call getDmrClusterTopologyIssueValidateBeforeCall(String dmrClusterName, String topologyIssue, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling getDmrClusterTopologyIssue(Async)");
        }
        // verify the required parameter 'topologyIssue' is set
        if (topologyIssue == null) {
            throw new ApiException("Missing the required parameter 'topologyIssue' when calling getDmrClusterTopologyIssue(Async)");
        }
        
        com.squareup.okhttp.Call call = getDmrClusterTopologyIssueCall(dmrClusterName, topologyIssue, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a Cluster Topology Issue object.
     * Get a Cluster Topology Issue object.  A Cluster Topology Issue indicates incorrect or inconsistent configuration within the DMR network. Such issues will cause messages to be misdelivered or lost.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| topologyIssue|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param topologyIssue The topology issue discovered in the Cluster. A topology issue indicates incorrect or inconsistent configuration within the DMR network. Such issues will cause messages to be misdelivered or lost. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterTopologyIssueResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterTopologyIssueResponse getDmrClusterTopologyIssue(String dmrClusterName, String topologyIssue, List<String> select) throws ApiException {
        ApiResponse<DmrClusterTopologyIssueResponse> resp = getDmrClusterTopologyIssueWithHttpInfo(dmrClusterName, topologyIssue, select);
        return resp.getData();
    }

    /**
     * Get a Cluster Topology Issue object.
     * Get a Cluster Topology Issue object.  A Cluster Topology Issue indicates incorrect or inconsistent configuration within the DMR network. Such issues will cause messages to be misdelivered or lost.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| topologyIssue|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param topologyIssue The topology issue discovered in the Cluster. A topology issue indicates incorrect or inconsistent configuration within the DMR network. Such issues will cause messages to be misdelivered or lost. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterTopologyIssueResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterTopologyIssueResponse> getDmrClusterTopologyIssueWithHttpInfo(String dmrClusterName, String topologyIssue, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getDmrClusterTopologyIssueValidateBeforeCall(dmrClusterName, topologyIssue, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterTopologyIssueResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Cluster Topology Issue object. (asynchronously)
     * Get a Cluster Topology Issue object.  A Cluster Topology Issue indicates incorrect or inconsistent configuration within the DMR network. Such issues will cause messages to be misdelivered or lost.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| topologyIssue|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param topologyIssue The topology issue discovered in the Cluster. A topology issue indicates incorrect or inconsistent configuration within the DMR network. Such issues will cause messages to be misdelivered or lost. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterTopologyIssueAsync(String dmrClusterName, String topologyIssue, List<String> select, final ApiCallback<DmrClusterTopologyIssueResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getDmrClusterTopologyIssueValidateBeforeCall(dmrClusterName, topologyIssue, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterTopologyIssueResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getDmrClusterTopologyIssues
     * @param dmrClusterName The name of the Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterTopologyIssuesCall(String dmrClusterName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/topologyIssues"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()));

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
    private com.squareup.okhttp.Call getDmrClusterTopologyIssuesValidateBeforeCall(String dmrClusterName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling getDmrClusterTopologyIssues(Async)");
        }
        
        com.squareup.okhttp.Call call = getDmrClusterTopologyIssuesCall(dmrClusterName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a list of Cluster Topology Issue objects.
     * Get a list of Cluster Topology Issue objects.  A Cluster Topology Issue indicates incorrect or inconsistent configuration within the DMR network. Such issues will cause messages to be misdelivered or lost.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| topologyIssue|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterTopologyIssuesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterTopologyIssuesResponse getDmrClusterTopologyIssues(String dmrClusterName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<DmrClusterTopologyIssuesResponse> resp = getDmrClusterTopologyIssuesWithHttpInfo(dmrClusterName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Cluster Topology Issue objects.
     * Get a list of Cluster Topology Issue objects.  A Cluster Topology Issue indicates incorrect or inconsistent configuration within the DMR network. Such issues will cause messages to be misdelivered or lost.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| topologyIssue|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterTopologyIssuesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterTopologyIssuesResponse> getDmrClusterTopologyIssuesWithHttpInfo(String dmrClusterName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getDmrClusterTopologyIssuesValidateBeforeCall(dmrClusterName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterTopologyIssuesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Cluster Topology Issue objects. (asynchronously)
     * Get a list of Cluster Topology Issue objects.  A Cluster Topology Issue indicates incorrect or inconsistent configuration within the DMR network. Such issues will cause messages to be misdelivered or lost.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x| topologyIssue|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDmrClusterTopologyIssuesAsync(String dmrClusterName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<DmrClusterTopologyIssuesResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getDmrClusterTopologyIssuesValidateBeforeCall(dmrClusterName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterTopologyIssuesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getDmrClusters
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDmrClustersCall(Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/dmrClusters";

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
    private com.squareup.okhttp.Call getDmrClustersValidateBeforeCall(Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        com.squareup.okhttp.Call call = getDmrClustersCall(count, cursor, where, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a list of Cluster objects.
     * Get a list of Cluster objects.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClustersResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClustersResponse getDmrClusters(Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<DmrClustersResponse> resp = getDmrClustersWithHttpInfo(count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Cluster objects.
     * Get a list of Cluster objects.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClustersResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClustersResponse> getDmrClustersWithHttpInfo(Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getDmrClustersValidateBeforeCall(count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClustersResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Cluster objects. (asynchronously)
     * Get a list of Cluster objects.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Deprecated :---|:---:|:---: dmrClusterName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDmrClustersAsync(Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<DmrClustersResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getDmrClustersValidateBeforeCall(count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClustersResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
