/*
 * SEMP (Solace Element Management Protocol)
 * SEMP (starting in `v2`, see note 1) is a RESTful API for configuring, monitoring, and administering a Solace PubSub+ broker.  SEMP uses URIs to address manageable **resources** of the Solace PubSub+ broker. Resources are individual **objects**, **collections** of objects, or (exclusively in the action API) **actions**. This document applies to the following API:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Configuration|/SEMP/v2/config|Reading and writing config state|See note 2    The following APIs are also available:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Action|/SEMP/v2/action|Performing actions|See note 2 Monitoring|/SEMP/v2/monitor|Querying operational parameters|See note 2    Resources are always nouns, with individual objects being singular and collections being plural.  Objects within a collection are identified by an `obj-id`, which follows the collection name with the form `collection-name/obj-id`.  Actions within an object are identified by an `action-id`, which follows the object name with the form `obj-id/action-id`.  Some examples:  ``` /SEMP/v2/config/msgVpns                        ; MsgVpn collection /SEMP/v2/config/msgVpns/a                      ; MsgVpn object named \"a\" /SEMP/v2/config/msgVpns/a/queues               ; Queue collection in MsgVpn \"a\" /SEMP/v2/config/msgVpns/a/queues/b             ; Queue object named \"b\" in MsgVpn \"a\" /SEMP/v2/action/msgVpns/a/queues/b/startReplay ; Action that starts a replay on Queue \"b\" in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients             ; Client collection in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients/c           ; Client object named \"c\" in MsgVpn \"a\" ```  ## Collection Resources  Collections are unordered lists of objects (unless described as otherwise), and are described by JSON arrays. Each item in the array represents an object in the same manner as the individual object would normally be represented. In the configuration API, the creation of a new object is done through its collection resource.  ## Object and Action Resources  Objects are composed of attributes, actions, collections, and other objects. They are described by JSON objects as name/value pairs. The collections and actions of an object are not contained directly in the object's JSON content; rather the content includes an attribute containing a URI which points to the collections and actions. These contained resources must be managed through this URI. At a minimum, every object has one or more identifying attributes, and its own `uri` attribute which contains the URI pointing to itself.  Actions are also composed of attributes, and are described by JSON objects as name/value pairs. Unlike objects, however, they are not members of a collection and cannot be retrieved, only performed. Actions only exist in the action API.  Attributes in an object or action may have any combination of the following properties:   Property|Meaning|Comments :---|:---|:--- Identifying|Attribute is involved in unique identification of the object, and appears in its URI| Required|Attribute must be provided in the request| Read-Only|Attribute can only be read, not written.|See note 3 Write-Only|Attribute can only be written, not read, unless the attribute is also opaque|See the documentation for the opaque property Requires-Disable|Attribute can only be changed when object is disabled| Deprecated|Attribute is deprecated, and will disappear in the next SEMP version| Opaque|Attribute can be set or retrieved in opaque form when the `opaquePassword` query parameter is present|See the `opaquePassword` query parameter documentation    In some requests, certain attributes may only be provided in certain combinations with other attributes:   Relationship|Meaning :---|:--- Requires|Attribute may only be changed by a request if a particular attribute or combination of attributes is also provided in the request Conflicts|Attribute may only be provided in a request if a particular attribute or combination of attributes is not also provided in the request    In the monitoring API, any non-identifying attribute may not be returned in a GET.  ## HTTP Methods  The following HTTP methods manipulate resources in accordance with these general principles. Note that some methods are only used in certain APIs:   Method|Resource|Meaning|Request Body|Response Body|Missing Request Attributes :---|:---|:---|:---|:---|:--- POST|Collection|Create object|Initial attribute values|Object attributes and metadata|Set to default PUT|Object|Create or replace object (see note 5)|New attribute values|Object attributes and metadata|Set to default, with certain exceptions (see note 4) PUT|Action|Performs action|Action arguments|Action metadata|N/A PATCH|Object|Update object|New attribute values|Object attributes and metadata|unchanged DELETE|Object|Delete object|Empty|Object metadata|N/A GET|Object|Get object|Empty|Object attributes and metadata|N/A GET|Collection|Get collection|Empty|Object attributes and collection metadata|N/A    ## Common Query Parameters  The following are some common query parameters that are supported by many method/URI combinations. Individual URIs may document additional parameters. Note that multiple query parameters can be used together in a single URI, separated by the ampersand character. For example:  ``` ; Request for the MsgVpns collection using two hypothetical query parameters ; \"q1\" and \"q2\" with values \"val1\" and \"val2\" respectively /SEMP/v2/config/msgVpns?q1=val1&q2=val2 ```  ### select  Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. Use this query parameter to limit the size of the returned data for each returned object, return only those fields that are desired, or exclude fields that are not desired.  The value of `select` is a comma-separated list of attribute names. If the list contains attribute names that are not prefaced by `-`, only those attributes are included in the response. If the list contains attribute names that are prefaced by `-`, those attributes are excluded from the response. If the list contains both types, then the difference of the first set of attributes and the second set of attributes is returned. If the list is empty (i.e. `select=`), no attributes are returned.  All attributes that are prefaced by `-` must follow all attributes that are not prefaced by `-`. In addition, each attribute name in the list must match at least one attribute in the object.  Names may include the `*` wildcard (zero or more characters). Nested attribute names are supported using periods (e.g. `parentName.childName`).  Some examples:  ``` ; List of all MsgVpn names /SEMP/v2/config/msgVpns?select=msgVpnName ; List of all MsgVpn and their attributes except for their names /SEMP/v2/config/msgVpns?select=-msgVpnName ; Authentication attributes of MsgVpn \"finance\" /SEMP/v2/config/msgVpns/finance?select=authentication* ; All attributes of MsgVpn \"finance\" except for authentication attributes /SEMP/v2/config/msgVpns/finance?select=-authentication* ; Access related attributes of Queue \"orderQ\" of MsgVpn \"finance\" /SEMP/v2/config/msgVpns/finance/queues/orderQ?select=owner,permission ```  ### where  Include in the response only objects where certain conditions are true. Use this query parameter to limit which objects are returned to those whose attribute values meet the given conditions.  The value of `where` is a comma-separated list of expressions. All expressions must be true for the object to be included in the response. Each expression takes the form:  ``` expression  = attribute-name OP value OP          = '==' | '!=' | '&lt;' | '&gt;' | '&lt;=' | '&gt;=' ```  `value` may be a number, string, `true`, or `false`, as appropriate for the type of `attribute-name`. Greater-than and less-than comparisons only work for numbers. A `*` in a string `value` is interpreted as a wildcard (zero or more characters). Some examples:  ``` ; Only enabled MsgVpns /SEMP/v2/config/msgVpns?where=enabled==true ; Only MsgVpns using basic non-LDAP authentication /SEMP/v2/config/msgVpns?where=authenticationBasicEnabled==true,authenticationBasicType!=ldap ; Only MsgVpns that allow more than 100 client connections /SEMP/v2/config/msgVpns?where=maxConnectionCount>100 ; Only MsgVpns with msgVpnName starting with \"B\": /SEMP/v2/config/msgVpns?where=msgVpnName==B* ```  ### count  Limit the count of objects in the response. This can be useful to limit the size of the response for large collections. The minimum value for `count` is `1` and the default is `10`. There is also a per-collection maximum value to limit request handling time. For example:  ``` ; Up to 25 MsgVpns /SEMP/v2/config/msgVpns?count=25 ```  ### cursor  The cursor, or position, for the next page of objects. Cursors are opaque data that should not be created or interpreted by SEMP clients, and should only be used as described below.  When a request is made for a collection and there may be additional objects available for retrieval that are not included in the initial response, the response will include a `cursorQuery` field containing a cursor. The value of this field can be specified in the `cursor` query parameter of a subsequent request to retrieve the next page of objects. For convenience, an appropriate URI is constructed automatically by the broker and included in the `nextPageUri` field of the response. This URI can be used directly to retrieve the next page of objects.  ### opaquePassword  Attributes with the opaque property are also write-only and so cannot normally be retrieved in a GET. However, when a password is provided in the `opaquePassword` query parameter, attributes with the opaque property are retrieved in a GET in opaque form, encrypted with this password. The query parameter can also be used on a POST, PATCH, or PUT to set opaque attributes using opaque attribute values retrieved in a GET, so long as:  1. the same password that was used to retrieve the opaque attribute values is provided; and  2. the broker to which the request is being sent has the same major and minor SEMP version as the broker that produced the opaque attribute values.  The password provided in the query parameter must be a minimum of 8 characters and a maximum of 128 characters.  The query parameter can only be used in the configuration API, and only over HTTPS.  ## Help  Visit [our website](https://solace.com) to learn more about Solace.  You can also download the SEMP API specifications by clicking [here](https://solace.com/downloads/).  If you need additional support, please contact us at [support@solace.com](mailto:support@solace.com).  ## Notes  Note|Description :---:|:--- 1|This specification defines SEMP starting in \"v2\", and not the original SEMP \"v1\" interface. Request and response formats between \"v1\" and \"v2\" are entirely incompatible, although both protocols share a common port configuration on the Solace PubSub+ broker. They are differentiated by the initial portion of the URI path, one of either \"/SEMP/\" or \"/SEMP/v2/\" 2|This API is partially implemented. Only a subset of all objects are available. 3|Read-only attributes may appear in POST and PUT/PATCH requests. However, if a read-only attribute is not marked as identifying, it will be ignored during a PUT/PATCH. 4|On a PUT, if the SEMP user is not authorized to modify the attribute, its value is left unchanged rather than set to default. In addition, the values of write-only attributes are not set to their defaults on a PUT, except in the following two cases: there is a mutual requires relationship with another non-write-only attribute and both attributes are absent from the request; or the attribute is also opaque and the `opaquePassword` query parameter is provided in the request. 5|On a PUT, if the object does not exist, it is created first.  
 *
 * OpenAPI spec version: 2.17
 * Contact: support@solace.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.solace.psg.sempv2.config.api;

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


import com.solace.psg.sempv2.config.model.MsgVpnDistributedCache;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheCluster;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterGlobalCachingHomeCluster;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterGlobalCachingHomeClustersResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterInstance;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterInstanceResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterInstancesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterTopic;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterTopicResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClusterTopicsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheClustersResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCacheResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDistributedCachesResponse;
import com.solace.psg.sempv2.config.model.SempMetaOnlyResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DistributedCacheApi {
    private ApiClient apiClient;

    public DistributedCacheApi() {
        this(Configuration.getDefaultApiClient());
    }

    public DistributedCacheApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for createMsgVpnDistributedCache
     * @param body The Distributed Cache object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDistributedCacheCall(MsgVpnDistributedCache body, String msgVpnName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call createMsgVpnDistributedCacheValidateBeforeCall(MsgVpnDistributedCache body, String msgVpnName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnDistributedCache(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnDistributedCache(Async)");
        }
        
        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheCall(body, msgVpnName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Create a Distributed Cache object.
     * Create a Distributed Cache object. Any attribute missing from the request will be set to its default value.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnDistributedCache|scheduledDeleteMsgDayList|scheduledDeleteMsgTimeList| MsgVpnDistributedCache|scheduledDeleteMsgTimeList|scheduledDeleteMsgDayList|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Distributed Cache object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheResponse createMsgVpnDistributedCache(MsgVpnDistributedCache body, String msgVpnName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheResponse> resp = createMsgVpnDistributedCacheWithHttpInfo(body, msgVpnName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Create a Distributed Cache object.
     * Create a Distributed Cache object. Any attribute missing from the request will be set to its default value.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnDistributedCache|scheduledDeleteMsgDayList|scheduledDeleteMsgTimeList| MsgVpnDistributedCache|scheduledDeleteMsgTimeList|scheduledDeleteMsgDayList|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Distributed Cache object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheResponse> createMsgVpnDistributedCacheWithHttpInfo(MsgVpnDistributedCache body, String msgVpnName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheValidateBeforeCall(body, msgVpnName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Distributed Cache object. (asynchronously)
     * Create a Distributed Cache object. Any attribute missing from the request will be set to its default value.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnDistributedCache|scheduledDeleteMsgDayList|scheduledDeleteMsgTimeList| MsgVpnDistributedCache|scheduledDeleteMsgTimeList|scheduledDeleteMsgDayList|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Distributed Cache object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDistributedCacheAsync(MsgVpnDistributedCache body, String msgVpnName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheValidateBeforeCall(body, msgVpnName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for createMsgVpnDistributedCacheCluster
     * @param body The Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterCall(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterValidateBeforeCall(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnDistributedCacheCluster(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnDistributedCacheCluster(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling createMsgVpnDistributedCacheCluster(Async)");
        }
        
        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterCall(body, msgVpnName, cacheName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Create a Cache Cluster object.
     * Create a Cache Cluster object. Any attribute missing from the request will be set to its default value.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x|x|||| msgVpnName|x||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent| EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterResponse createMsgVpnDistributedCacheCluster(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterResponse> resp = createMsgVpnDistributedCacheClusterWithHttpInfo(body, msgVpnName, cacheName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Create a Cache Cluster object.
     * Create a Cache Cluster object. Any attribute missing from the request will be set to its default value.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x|x|||| msgVpnName|x||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent| EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterResponse> createMsgVpnDistributedCacheClusterWithHttpInfo(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterValidateBeforeCall(body, msgVpnName, cacheName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Cache Cluster object. (asynchronously)
     * Create a Cache Cluster object. Any attribute missing from the request will be set to its default value.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x|x|||| msgVpnName|x||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent| EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterAsync(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterValidateBeforeCall(body, msgVpnName, cacheName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for createMsgVpnDistributedCacheClusterGlobalCachingHomeCluster
     * @param body The Home Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterCall(MsgVpnDistributedCacheClusterGlobalCachingHomeCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/globalCachingHomeClusters"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterValidateBeforeCall(MsgVpnDistributedCacheClusterGlobalCachingHomeCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling createMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling createMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(Async)");
        }
        
        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Create a Home Cache Cluster object.
     * Create a Home Cache Cluster object. Any attribute missing from the request will be set to its default value.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| homeClusterName|x|x|||| msgVpnName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Home Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse createMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(MsgVpnDistributedCacheClusterGlobalCachingHomeCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse> resp = createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterWithHttpInfo(body, msgVpnName, cacheName, clusterName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Create a Home Cache Cluster object.
     * Create a Home Cache Cluster object. Any attribute missing from the request will be set to its default value.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| homeClusterName|x|x|||| msgVpnName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Home Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse> createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterWithHttpInfo(MsgVpnDistributedCacheClusterGlobalCachingHomeCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterValidateBeforeCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Home Cache Cluster object. (asynchronously)
     * Create a Home Cache Cluster object. Any attribute missing from the request will be set to its default value.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| homeClusterName|x|x|||| msgVpnName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Home Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterAsync(MsgVpnDistributedCacheClusterGlobalCachingHomeCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterValidateBeforeCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix
     * @param body The Topic Prefix object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixCall(MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix body, String msgVpnName, String cacheName, String clusterName, String homeClusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/globalCachingHomeClusters/{homeClusterName}/topicPrefixes"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "homeClusterName" + "\\}", apiClient.escapeString(homeClusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixValidateBeforeCall(MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix body, String msgVpnName, String cacheName, String clusterName, String homeClusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        // verify the required parameter 'homeClusterName' is set
        if (homeClusterName == null) {
            throw new ApiException("Missing the required parameter 'homeClusterName' when calling createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        
        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixCall(body, msgVpnName, cacheName, clusterName, homeClusterName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Create a Topic Prefix object.
     * Create a Topic Prefix object. Any attribute missing from the request will be set to its default value.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| homeClusterName|x||x||| msgVpnName|x||x||| topicPrefix|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Topic Prefix object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix body, String msgVpnName, String cacheName, String clusterName, String homeClusterName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse> resp = createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixWithHttpInfo(body, msgVpnName, cacheName, clusterName, homeClusterName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Create a Topic Prefix object.
     * Create a Topic Prefix object. Any attribute missing from the request will be set to its default value.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| homeClusterName|x||x||| msgVpnName|x||x||| topicPrefix|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Topic Prefix object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse> createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixWithHttpInfo(MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix body, String msgVpnName, String cacheName, String clusterName, String homeClusterName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixValidateBeforeCall(body, msgVpnName, cacheName, clusterName, homeClusterName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Topic Prefix object. (asynchronously)
     * Create a Topic Prefix object. Any attribute missing from the request will be set to its default value.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| homeClusterName|x||x||| msgVpnName|x||x||| topicPrefix|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Topic Prefix object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixAsync(MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix body, String msgVpnName, String cacheName, String clusterName, String homeClusterName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixValidateBeforeCall(body, msgVpnName, cacheName, clusterName, homeClusterName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for createMsgVpnDistributedCacheClusterInstance
     * @param body The Cache Instance object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterInstanceCall(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling createMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling createMsgVpnDistributedCacheClusterInstance(Async)");
        }
        
        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterInstanceCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Create a Cache Instance object.
     * Create a Cache Instance object. Any attribute missing from the request will be set to its default value.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| instanceName|x|x|||| msgVpnName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Instance object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterInstanceResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterInstanceResponse createMsgVpnDistributedCacheClusterInstance(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterInstanceResponse> resp = createMsgVpnDistributedCacheClusterInstanceWithHttpInfo(body, msgVpnName, cacheName, clusterName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Create a Cache Instance object.
     * Create a Cache Instance object. Any attribute missing from the request will be set to its default value.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| instanceName|x|x|||| msgVpnName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Instance object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterInstanceResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterInstanceResponse> createMsgVpnDistributedCacheClusterInstanceWithHttpInfo(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstanceResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Cache Instance object. (asynchronously)
     * Create a Cache Instance object. Any attribute missing from the request will be set to its default value.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| instanceName|x|x|||| msgVpnName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Instance object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterInstanceAsync(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterInstanceResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstanceResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for createMsgVpnDistributedCacheClusterTopic
     * @param body The Topic object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterTopicCall(MsgVpnDistributedCacheClusterTopic body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/topics"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterTopicValidateBeforeCall(MsgVpnDistributedCacheClusterTopic body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnDistributedCacheClusterTopic(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnDistributedCacheClusterTopic(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling createMsgVpnDistributedCacheClusterTopic(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling createMsgVpnDistributedCacheClusterTopic(Async)");
        }
        
        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterTopicCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Create a Topic object.
     * Create a Topic object. Any attribute missing from the request will be set to its default value.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| msgVpnName|x||x||| topic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Topic object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterTopicResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterTopicResponse createMsgVpnDistributedCacheClusterTopic(MsgVpnDistributedCacheClusterTopic body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterTopicResponse> resp = createMsgVpnDistributedCacheClusterTopicWithHttpInfo(body, msgVpnName, cacheName, clusterName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Create a Topic object.
     * Create a Topic object. Any attribute missing from the request will be set to its default value.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| msgVpnName|x||x||| topic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Topic object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterTopicResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterTopicResponse> createMsgVpnDistributedCacheClusterTopicWithHttpInfo(MsgVpnDistributedCacheClusterTopic body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterTopicValidateBeforeCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterTopicResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Topic object. (asynchronously)
     * Create a Topic object. Any attribute missing from the request will be set to its default value.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| msgVpnName|x||x||| topic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Topic object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDistributedCacheClusterTopicAsync(MsgVpnDistributedCacheClusterTopic body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterTopicResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnDistributedCacheClusterTopicValidateBeforeCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterTopicResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnDistributedCache
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDistributedCacheCall(String msgVpnName, String cacheName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();

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
        return apiClient.buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call deleteMsgVpnDistributedCacheValidateBeforeCall(String msgVpnName, String cacheName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnDistributedCache(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling deleteMsgVpnDistributedCache(Async)");
        }
        
        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheCall(msgVpnName, cacheName, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Delete a Distributed Cache object.
     * Delete a Distributed Cache object.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnDistributedCache(String msgVpnName, String cacheName) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnDistributedCacheWithHttpInfo(msgVpnName, cacheName);
        return resp.getData();
    }

    /**
     * Delete a Distributed Cache object.
     * Delete a Distributed Cache object.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnDistributedCacheWithHttpInfo(String msgVpnName, String cacheName) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheValidateBeforeCall(msgVpnName, cacheName, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Distributed Cache object. (asynchronously)
     * Delete a Distributed Cache object.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDistributedCacheAsync(String msgVpnName, String cacheName, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheValidateBeforeCall(msgVpnName, cacheName, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnDistributedCacheCluster
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterCall(String msgVpnName, String cacheName, String clusterName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();

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
        return apiClient.buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnDistributedCacheCluster(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling deleteMsgVpnDistributedCacheCluster(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling deleteMsgVpnDistributedCacheCluster(Async)");
        }
        
        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterCall(msgVpnName, cacheName, clusterName, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Delete a Cache Cluster object.
     * Delete a Cache Cluster object.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnDistributedCacheCluster(String msgVpnName, String cacheName, String clusterName) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnDistributedCacheClusterWithHttpInfo(msgVpnName, cacheName, clusterName);
        return resp.getData();
    }

    /**
     * Delete a Cache Cluster object.
     * Delete a Cache Cluster object.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnDistributedCacheClusterWithHttpInfo(String msgVpnName, String cacheName, String clusterName) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterValidateBeforeCall(msgVpnName, cacheName, clusterName, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Cache Cluster object. (asynchronously)
     * Delete a Cache Cluster object.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterAsync(String msgVpnName, String cacheName, String clusterName, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterValidateBeforeCall(msgVpnName, cacheName, clusterName, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnDistributedCacheClusterGlobalCachingHomeCluster
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterCall(String msgVpnName, String cacheName, String clusterName, String homeClusterName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/globalCachingHomeClusters/{homeClusterName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "homeClusterName" + "\\}", apiClient.escapeString(homeClusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();

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
        return apiClient.buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String homeClusterName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling deleteMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling deleteMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(Async)");
        }
        // verify the required parameter 'homeClusterName' is set
        if (homeClusterName == null) {
            throw new ApiException("Missing the required parameter 'homeClusterName' when calling deleteMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(Async)");
        }
        
        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterCall(msgVpnName, cacheName, clusterName, homeClusterName, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Delete a Home Cache Cluster object.
     * Delete a Home Cache Cluster object.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(String msgVpnName, String cacheName, String clusterName, String homeClusterName) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterWithHttpInfo(msgVpnName, cacheName, clusterName, homeClusterName);
        return resp.getData();
    }

    /**
     * Delete a Home Cache Cluster object.
     * Delete a Home Cache Cluster object.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String homeClusterName) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterValidateBeforeCall(msgVpnName, cacheName, clusterName, homeClusterName, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Home Cache Cluster object. (asynchronously)
     * Delete a Home Cache Cluster object.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterAsync(String msgVpnName, String cacheName, String clusterName, String homeClusterName, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterValidateBeforeCall(msgVpnName, cacheName, clusterName, homeClusterName, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param topicPrefix A topic prefix for global topics available from the remote Home Cache Cluster. A wildcard (/&gt;) is implied at the end of the prefix. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixCall(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String topicPrefix, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/globalCachingHomeClusters/{homeClusterName}/topicPrefixes/{topicPrefix}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "homeClusterName" + "\\}", apiClient.escapeString(homeClusterName.toString()))
            .replaceAll("\\{" + "topicPrefix" + "\\}", apiClient.escapeString(topicPrefix.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();

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
        return apiClient.buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String topicPrefix, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        // verify the required parameter 'homeClusterName' is set
        if (homeClusterName == null) {
            throw new ApiException("Missing the required parameter 'homeClusterName' when calling deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        // verify the required parameter 'topicPrefix' is set
        if (topicPrefix == null) {
            throw new ApiException("Missing the required parameter 'topicPrefix' when calling deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        
        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixCall(msgVpnName, cacheName, clusterName, homeClusterName, topicPrefix, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Delete a Topic Prefix object.
     * Delete a Topic Prefix object.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param topicPrefix A topic prefix for global topics available from the remote Home Cache Cluster. A wildcard (/&gt;) is implied at the end of the prefix. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String topicPrefix) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixWithHttpInfo(msgVpnName, cacheName, clusterName, homeClusterName, topicPrefix);
        return resp.getData();
    }

    /**
     * Delete a Topic Prefix object.
     * Delete a Topic Prefix object.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param topicPrefix A topic prefix for global topics available from the remote Home Cache Cluster. A wildcard (/&gt;) is implied at the end of the prefix. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String topicPrefix) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixValidateBeforeCall(msgVpnName, cacheName, clusterName, homeClusterName, topicPrefix, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Topic Prefix object. (asynchronously)
     * Delete a Topic Prefix object.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param topicPrefix A topic prefix for global topics available from the remote Home Cache Cluster. A wildcard (/&gt;) is implied at the end of the prefix. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixAsync(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String topicPrefix, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixValidateBeforeCall(msgVpnName, cacheName, clusterName, homeClusterName, topicPrefix, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnDistributedCacheClusterInstance
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterInstanceCall(String msgVpnName, String cacheName, String clusterName, String instanceName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "instanceName" + "\\}", apiClient.escapeString(instanceName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();

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
        return apiClient.buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String instanceName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling deleteMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling deleteMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'instanceName' is set
        if (instanceName == null) {
            throw new ApiException("Missing the required parameter 'instanceName' when calling deleteMsgVpnDistributedCacheClusterInstance(Async)");
        }
        
        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterInstanceCall(msgVpnName, cacheName, clusterName, instanceName, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Delete a Cache Instance object.
     * Delete a Cache Instance object.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnDistributedCacheClusterInstance(String msgVpnName, String cacheName, String clusterName, String instanceName) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnDistributedCacheClusterInstanceWithHttpInfo(msgVpnName, cacheName, clusterName, instanceName);
        return resp.getData();
    }

    /**
     * Delete a Cache Instance object.
     * Delete a Cache Instance object.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnDistributedCacheClusterInstanceWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String instanceName) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Cache Instance object. (asynchronously)
     * Delete a Cache Instance object.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterInstanceAsync(String msgVpnName, String cacheName, String clusterName, String instanceName, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnDistributedCacheClusterTopic
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param topic The value of the Topic in the form a/b/c. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterTopicCall(String msgVpnName, String cacheName, String clusterName, String topic, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/topics/{topic}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "topic" + "\\}", apiClient.escapeString(topic.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();

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
        return apiClient.buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterTopicValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String topic, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnDistributedCacheClusterTopic(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling deleteMsgVpnDistributedCacheClusterTopic(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling deleteMsgVpnDistributedCacheClusterTopic(Async)");
        }
        // verify the required parameter 'topic' is set
        if (topic == null) {
            throw new ApiException("Missing the required parameter 'topic' when calling deleteMsgVpnDistributedCacheClusterTopic(Async)");
        }
        
        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterTopicCall(msgVpnName, cacheName, clusterName, topic, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Delete a Topic object.
     * Delete a Topic object.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param topic The value of the Topic in the form a/b/c. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnDistributedCacheClusterTopic(String msgVpnName, String cacheName, String clusterName, String topic) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnDistributedCacheClusterTopicWithHttpInfo(msgVpnName, cacheName, clusterName, topic);
        return resp.getData();
    }

    /**
     * Delete a Topic object.
     * Delete a Topic object.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param topic The value of the Topic in the form a/b/c. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnDistributedCacheClusterTopicWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String topic) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterTopicValidateBeforeCall(msgVpnName, cacheName, clusterName, topic, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Topic object. (asynchronously)
     * Delete a Topic object.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param topic The value of the Topic in the form a/b/c. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDistributedCacheClusterTopicAsync(String msgVpnName, String cacheName, String clusterName, String topic, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnDistributedCacheClusterTopicValidateBeforeCall(msgVpnName, cacheName, clusterName, topic, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCache
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheCall(String msgVpnName, String cacheName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheValidateBeforeCall(String msgVpnName, String cacheName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCache(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCache(Async)");
        }
        
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheCall(msgVpnName, cacheName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a Distributed Cache object.
     * Get a Distributed Cache object.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheResponse getMsgVpnDistributedCache(String msgVpnName, String cacheName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheResponse> resp = getMsgVpnDistributedCacheWithHttpInfo(msgVpnName, cacheName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Get a Distributed Cache object.
     * Get a Distributed Cache object.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheResponse> getMsgVpnDistributedCacheWithHttpInfo(String msgVpnName, String cacheName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheValidateBeforeCall(msgVpnName, cacheName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Distributed Cache object. (asynchronously)
     * Get a Distributed Cache object.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheAsync(String msgVpnName, String cacheName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheValidateBeforeCall(msgVpnName, cacheName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheCluster
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterCall(String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCacheCluster(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCacheCluster(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling getMsgVpnDistributedCacheCluster(Async)");
        }
        
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterCall(msgVpnName, cacheName, clusterName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a Cache Cluster object.
     * Get a Cache Cluster object.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterResponse getMsgVpnDistributedCacheCluster(String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterResponse> resp = getMsgVpnDistributedCacheClusterWithHttpInfo(msgVpnName, cacheName, clusterName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Get a Cache Cluster object.
     * Get a Cache Cluster object.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterResponse> getMsgVpnDistributedCacheClusterWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterValidateBeforeCall(msgVpnName, cacheName, clusterName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Cache Cluster object. (asynchronously)
     * Get a Cache Cluster object.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterAsync(String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterValidateBeforeCall(msgVpnName, cacheName, clusterName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheClusterGlobalCachingHomeCluster
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterCall(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/globalCachingHomeClusters/{homeClusterName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "homeClusterName" + "\\}", apiClient.escapeString(homeClusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(Async)");
        }
        // verify the required parameter 'homeClusterName' is set
        if (homeClusterName == null) {
            throw new ApiException("Missing the required parameter 'homeClusterName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(Async)");
        }
        
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterCall(msgVpnName, cacheName, clusterName, homeClusterName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a Home Cache Cluster object.
     * Get a Home Cache Cluster object.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse getMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse> resp = getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterWithHttpInfo(msgVpnName, cacheName, clusterName, homeClusterName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Get a Home Cache Cluster object.
     * Get a Home Cache Cluster object.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse> getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterValidateBeforeCall(msgVpnName, cacheName, clusterName, homeClusterName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Home Cache Cluster object. (asynchronously)
     * Get a Home Cache Cluster object.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterAsync(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterValidateBeforeCall(msgVpnName, cacheName, clusterName, homeClusterName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param topicPrefix A topic prefix for global topics available from the remote Home Cache Cluster. A wildcard (/&gt;) is implied at the end of the prefix. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixCall(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String topicPrefix, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/globalCachingHomeClusters/{homeClusterName}/topicPrefixes/{topicPrefix}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "homeClusterName" + "\\}", apiClient.escapeString(homeClusterName.toString()))
            .replaceAll("\\{" + "topicPrefix" + "\\}", apiClient.escapeString(topicPrefix.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String topicPrefix, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        // verify the required parameter 'homeClusterName' is set
        if (homeClusterName == null) {
            throw new ApiException("Missing the required parameter 'homeClusterName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        // verify the required parameter 'topicPrefix' is set
        if (topicPrefix == null) {
            throw new ApiException("Missing the required parameter 'topicPrefix' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(Async)");
        }
        
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixCall(msgVpnName, cacheName, clusterName, homeClusterName, topicPrefix, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a Topic Prefix object.
     * Get a Topic Prefix object.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x||| topicPrefix|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param topicPrefix A topic prefix for global topics available from the remote Home Cache Cluster. A wildcard (/&gt;) is implied at the end of the prefix. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String topicPrefix, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse> resp = getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixWithHttpInfo(msgVpnName, cacheName, clusterName, homeClusterName, topicPrefix, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Get a Topic Prefix object.
     * Get a Topic Prefix object.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x||| topicPrefix|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param topicPrefix A topic prefix for global topics available from the remote Home Cache Cluster. A wildcard (/&gt;) is implied at the end of the prefix. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse> getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String topicPrefix, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixValidateBeforeCall(msgVpnName, cacheName, clusterName, homeClusterName, topicPrefix, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Topic Prefix object. (asynchronously)
     * Get a Topic Prefix object.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x||| topicPrefix|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param topicPrefix A topic prefix for global topics available from the remote Home Cache Cluster. A wildcard (/&gt;) is implied at the end of the prefix. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixAsync(String msgVpnName, String cacheName, String clusterName, String homeClusterName, String topicPrefix, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixValidateBeforeCall(msgVpnName, cacheName, clusterName, homeClusterName, topicPrefix, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixes
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesCall(String msgVpnName, String cacheName, String clusterName, String homeClusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/globalCachingHomeClusters/{homeClusterName}/topicPrefixes"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "homeClusterName" + "\\}", apiClient.escapeString(homeClusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String homeClusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixes(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixes(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixes(Async)");
        }
        // verify the required parameter 'homeClusterName' is set
        if (homeClusterName == null) {
            throw new ApiException("Missing the required parameter 'homeClusterName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixes(Async)");
        }
        
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesCall(msgVpnName, cacheName, clusterName, homeClusterName, count, cursor, opaquePassword, where, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a list of Topic Prefix objects.
     * Get a list of Topic Prefix objects.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x||| topicPrefix|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesResponse getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixes(String msgVpnName, String cacheName, String clusterName, String homeClusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesResponse> resp = getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesWithHttpInfo(msgVpnName, cacheName, clusterName, homeClusterName, count, cursor, opaquePassword, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Topic Prefix objects.
     * Get a list of Topic Prefix objects.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x||| topicPrefix|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesResponse> getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String homeClusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesValidateBeforeCall(msgVpnName, cacheName, clusterName, homeClusterName, count, cursor, opaquePassword, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Topic Prefix objects. (asynchronously)
     * Get a list of Topic Prefix objects.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x||| topicPrefix|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param homeClusterName The name of the remote Home Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesAsync(String msgVpnName, String cacheName, String clusterName, String homeClusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesValidateBeforeCall(msgVpnName, cacheName, clusterName, homeClusterName, count, cursor, opaquePassword, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheClusterGlobalCachingHomeClusters
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterGlobalCachingHomeClustersCall(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/globalCachingHomeClusters"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterGlobalCachingHomeClustersValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeClusters(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeClusters(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling getMsgVpnDistributedCacheClusterGlobalCachingHomeClusters(Async)");
        }
        
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterGlobalCachingHomeClustersCall(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a list of Home Cache Cluster objects.
     * Get a list of Home Cache Cluster objects.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterGlobalCachingHomeClustersResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterGlobalCachingHomeClustersResponse getMsgVpnDistributedCacheClusterGlobalCachingHomeClusters(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterGlobalCachingHomeClustersResponse> resp = getMsgVpnDistributedCacheClusterGlobalCachingHomeClustersWithHttpInfo(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Home Cache Cluster objects.
     * Get a list of Home Cache Cluster objects.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterGlobalCachingHomeClustersResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterGlobalCachingHomeClustersResponse> getMsgVpnDistributedCacheClusterGlobalCachingHomeClustersWithHttpInfo(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterGlobalCachingHomeClustersValidateBeforeCall(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterGlobalCachingHomeClustersResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Home Cache Cluster objects. (asynchronously)
     * Get a list of Home Cache Cluster objects.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterGlobalCachingHomeClustersAsync(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterGlobalCachingHomeClustersResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterGlobalCachingHomeClustersValidateBeforeCall(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterGlobalCachingHomeClustersResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheClusterInstance
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterInstanceCall(String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "instanceName" + "\\}", apiClient.escapeString(instanceName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling getMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'instanceName' is set
        if (instanceName == null) {
            throw new ApiException("Missing the required parameter 'instanceName' when calling getMsgVpnDistributedCacheClusterInstance(Async)");
        }
        
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterInstanceCall(msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a Cache Instance object.
     * Get a Cache Instance object.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| instanceName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterInstanceResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterInstanceResponse getMsgVpnDistributedCacheClusterInstance(String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterInstanceResponse> resp = getMsgVpnDistributedCacheClusterInstanceWithHttpInfo(msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Get a Cache Instance object.
     * Get a Cache Instance object.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| instanceName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterInstanceResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterInstanceResponse> getMsgVpnDistributedCacheClusterInstanceWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstanceResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Cache Instance object. (asynchronously)
     * Get a Cache Instance object.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| instanceName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterInstanceAsync(String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterInstanceResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstanceResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheClusterInstances
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterInstancesCall(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterInstancesValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCacheClusterInstances(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCacheClusterInstances(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling getMsgVpnDistributedCacheClusterInstances(Async)");
        }
        
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterInstancesCall(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a list of Cache Instance objects.
     * Get a list of Cache Instance objects.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| instanceName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterInstancesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterInstancesResponse getMsgVpnDistributedCacheClusterInstances(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterInstancesResponse> resp = getMsgVpnDistributedCacheClusterInstancesWithHttpInfo(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Cache Instance objects.
     * Get a list of Cache Instance objects.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| instanceName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterInstancesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterInstancesResponse> getMsgVpnDistributedCacheClusterInstancesWithHttpInfo(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterInstancesValidateBeforeCall(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstancesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Cache Instance objects. (asynchronously)
     * Get a list of Cache Instance objects.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| instanceName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterInstancesAsync(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterInstancesResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterInstancesValidateBeforeCall(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstancesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheClusterTopic
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param topic The value of the Topic in the form a/b/c. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterTopicCall(String msgVpnName, String cacheName, String clusterName, String topic, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/topics/{topic}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "topic" + "\\}", apiClient.escapeString(topic.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterTopicValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String topic, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCacheClusterTopic(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCacheClusterTopic(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling getMsgVpnDistributedCacheClusterTopic(Async)");
        }
        // verify the required parameter 'topic' is set
        if (topic == null) {
            throw new ApiException("Missing the required parameter 'topic' when calling getMsgVpnDistributedCacheClusterTopic(Async)");
        }
        
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterTopicCall(msgVpnName, cacheName, clusterName, topic, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a Topic object.
     * Get a Topic object.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x||| topic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param topic The value of the Topic in the form a/b/c. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterTopicResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterTopicResponse getMsgVpnDistributedCacheClusterTopic(String msgVpnName, String cacheName, String clusterName, String topic, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterTopicResponse> resp = getMsgVpnDistributedCacheClusterTopicWithHttpInfo(msgVpnName, cacheName, clusterName, topic, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Get a Topic object.
     * Get a Topic object.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x||| topic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param topic The value of the Topic in the form a/b/c. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterTopicResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterTopicResponse> getMsgVpnDistributedCacheClusterTopicWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String topic, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterTopicValidateBeforeCall(msgVpnName, cacheName, clusterName, topic, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterTopicResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Topic object. (asynchronously)
     * Get a Topic object.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x||| topic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param topic The value of the Topic in the form a/b/c. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterTopicAsync(String msgVpnName, String cacheName, String clusterName, String topic, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterTopicResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterTopicValidateBeforeCall(msgVpnName, cacheName, clusterName, topic, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterTopicResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheClusterTopics
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterTopicsCall(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/topics"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterTopicsValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCacheClusterTopics(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCacheClusterTopics(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling getMsgVpnDistributedCacheClusterTopics(Async)");
        }
        
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterTopicsCall(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a list of Topic objects.
     * Get a list of Topic objects.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x||| topic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterTopicsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterTopicsResponse getMsgVpnDistributedCacheClusterTopics(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterTopicsResponse> resp = getMsgVpnDistributedCacheClusterTopicsWithHttpInfo(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Topic objects.
     * Get a list of Topic objects.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x||| topic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterTopicsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterTopicsResponse> getMsgVpnDistributedCacheClusterTopicsWithHttpInfo(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterTopicsValidateBeforeCall(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterTopicsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Topic objects. (asynchronously)
     * Get a list of Topic objects.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x||| topic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterTopicsAsync(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterTopicsResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterTopicsValidateBeforeCall(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterTopicsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheClusters
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClustersCall(String msgVpnName, String cacheName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClustersValidateBeforeCall(String msgVpnName, String cacheName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCacheClusters(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCacheClusters(Async)");
        }
        
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClustersCall(msgVpnName, cacheName, count, cursor, opaquePassword, where, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a list of Cache Cluster objects.
     * Get a list of Cache Cluster objects.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClustersResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClustersResponse getMsgVpnDistributedCacheClusters(String msgVpnName, String cacheName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClustersResponse> resp = getMsgVpnDistributedCacheClustersWithHttpInfo(msgVpnName, cacheName, count, cursor, opaquePassword, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Cache Cluster objects.
     * Get a list of Cache Cluster objects.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClustersResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClustersResponse> getMsgVpnDistributedCacheClustersWithHttpInfo(String msgVpnName, String cacheName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClustersValidateBeforeCall(msgVpnName, cacheName, count, cursor, opaquePassword, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClustersResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Cache Cluster objects. (asynchronously)
     * Get a list of Cache Cluster objects.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClustersAsync(String msgVpnName, String cacheName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ApiCallback<MsgVpnDistributedCacheClustersResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClustersValidateBeforeCall(msgVpnName, cacheName, count, cursor, opaquePassword, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClustersResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCaches
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCachesCall(String msgVpnName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (count != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("count", count));
        if (cursor != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("cursor", cursor));
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCachesValidateBeforeCall(String msgVpnName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCaches(Async)");
        }
        
        com.squareup.okhttp.Call call = getMsgVpnDistributedCachesCall(msgVpnName, count, cursor, opaquePassword, where, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Get a list of Distributed Cache objects.
     * Get a list of Distributed Cache objects.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCachesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCachesResponse getMsgVpnDistributedCaches(String msgVpnName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCachesResponse> resp = getMsgVpnDistributedCachesWithHttpInfo(msgVpnName, count, cursor, opaquePassword, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Distributed Cache objects.
     * Get a list of Distributed Cache objects.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCachesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCachesResponse> getMsgVpnDistributedCachesWithHttpInfo(String msgVpnName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCachesValidateBeforeCall(msgVpnName, count, cursor, opaquePassword, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCachesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Distributed Cache objects. (asynchronously)
     * Get a list of Distributed Cache objects.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCachesAsync(String msgVpnName, Integer count, String cursor, String opaquePassword, List<String> where, List<String> select, final ApiCallback<MsgVpnDistributedCachesResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCachesValidateBeforeCall(msgVpnName, count, cursor, opaquePassword, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCachesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for replaceMsgVpnDistributedCache
     * @param body The Distributed Cache object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnDistributedCacheCall(MsgVpnDistributedCache body, String msgVpnName, String cacheName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
        return apiClient.buildCall(localVarPath, "PUT", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call replaceMsgVpnDistributedCacheValidateBeforeCall(MsgVpnDistributedCache body, String msgVpnName, String cacheName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling replaceMsgVpnDistributedCache(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling replaceMsgVpnDistributedCache(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling replaceMsgVpnDistributedCache(Async)");
        }
        
        com.squareup.okhttp.Call call = replaceMsgVpnDistributedCacheCall(body, msgVpnName, cacheName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Replace a Distributed Cache object.
     * Replace a Distributed Cache object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnDistributedCache|scheduledDeleteMsgDayList|scheduledDeleteMsgTimeList| MsgVpnDistributedCache|scheduledDeleteMsgTimeList|scheduledDeleteMsgDayList|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Distributed Cache object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheResponse replaceMsgVpnDistributedCache(MsgVpnDistributedCache body, String msgVpnName, String cacheName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheResponse> resp = replaceMsgVpnDistributedCacheWithHttpInfo(body, msgVpnName, cacheName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Replace a Distributed Cache object.
     * Replace a Distributed Cache object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnDistributedCache|scheduledDeleteMsgDayList|scheduledDeleteMsgTimeList| MsgVpnDistributedCache|scheduledDeleteMsgTimeList|scheduledDeleteMsgDayList|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Distributed Cache object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheResponse> replaceMsgVpnDistributedCacheWithHttpInfo(MsgVpnDistributedCache body, String msgVpnName, String cacheName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = replaceMsgVpnDistributedCacheValidateBeforeCall(body, msgVpnName, cacheName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Replace a Distributed Cache object. (asynchronously)
     * Replace a Distributed Cache object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnDistributedCache|scheduledDeleteMsgDayList|scheduledDeleteMsgTimeList| MsgVpnDistributedCache|scheduledDeleteMsgTimeList|scheduledDeleteMsgDayList|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Distributed Cache object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnDistributedCacheAsync(MsgVpnDistributedCache body, String msgVpnName, String cacheName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = replaceMsgVpnDistributedCacheValidateBeforeCall(body, msgVpnName, cacheName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for replaceMsgVpnDistributedCacheCluster
     * @param body The Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnDistributedCacheClusterCall(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
        return apiClient.buildCall(localVarPath, "PUT", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call replaceMsgVpnDistributedCacheClusterValidateBeforeCall(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling replaceMsgVpnDistributedCacheCluster(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling replaceMsgVpnDistributedCacheCluster(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling replaceMsgVpnDistributedCacheCluster(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling replaceMsgVpnDistributedCacheCluster(Async)");
        }
        
        com.squareup.okhttp.Call call = replaceMsgVpnDistributedCacheClusterCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Replace a Cache Cluster object.
     * Replace a Cache Cluster object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent| EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterResponse replaceMsgVpnDistributedCacheCluster(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterResponse> resp = replaceMsgVpnDistributedCacheClusterWithHttpInfo(body, msgVpnName, cacheName, clusterName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Replace a Cache Cluster object.
     * Replace a Cache Cluster object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent| EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterResponse> replaceMsgVpnDistributedCacheClusterWithHttpInfo(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = replaceMsgVpnDistributedCacheClusterValidateBeforeCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Replace a Cache Cluster object. (asynchronously)
     * Replace a Cache Cluster object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent| EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnDistributedCacheClusterAsync(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = replaceMsgVpnDistributedCacheClusterValidateBeforeCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for replaceMsgVpnDistributedCacheClusterInstance
     * @param body The Cache Instance object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnDistributedCacheClusterInstanceCall(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "instanceName" + "\\}", apiClient.escapeString(instanceName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
        return apiClient.buildCall(localVarPath, "PUT", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call replaceMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling replaceMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling replaceMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling replaceMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling replaceMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'instanceName' is set
        if (instanceName == null) {
            throw new ApiException("Missing the required parameter 'instanceName' when calling replaceMsgVpnDistributedCacheClusterInstance(Async)");
        }
        
        com.squareup.okhttp.Call call = replaceMsgVpnDistributedCacheClusterInstanceCall(body, msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Replace a Cache Instance object.
     * Replace a Cache Instance object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| instanceName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Instance object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterInstanceResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterInstanceResponse replaceMsgVpnDistributedCacheClusterInstance(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterInstanceResponse> resp = replaceMsgVpnDistributedCacheClusterInstanceWithHttpInfo(body, msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Replace a Cache Instance object.
     * Replace a Cache Instance object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| instanceName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Instance object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterInstanceResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterInstanceResponse> replaceMsgVpnDistributedCacheClusterInstanceWithHttpInfo(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = replaceMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(body, msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstanceResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Replace a Cache Instance object. (asynchronously)
     * Replace a Cache Instance object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| instanceName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Instance object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnDistributedCacheClusterInstanceAsync(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterInstanceResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = replaceMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(body, msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstanceResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for updateMsgVpnDistributedCache
     * @param body The Distributed Cache object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnDistributedCacheCall(MsgVpnDistributedCache body, String msgVpnName, String cacheName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
        return apiClient.buildCall(localVarPath, "PATCH", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call updateMsgVpnDistributedCacheValidateBeforeCall(MsgVpnDistributedCache body, String msgVpnName, String cacheName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling updateMsgVpnDistributedCache(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling updateMsgVpnDistributedCache(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling updateMsgVpnDistributedCache(Async)");
        }
        
        com.squareup.okhttp.Call call = updateMsgVpnDistributedCacheCall(body, msgVpnName, cacheName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Update a Distributed Cache object.
     * Update a Distributed Cache object. Any attribute missing from the request will be left unchanged.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnDistributedCache|scheduledDeleteMsgDayList|scheduledDeleteMsgTimeList| MsgVpnDistributedCache|scheduledDeleteMsgTimeList|scheduledDeleteMsgDayList|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Distributed Cache object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheResponse updateMsgVpnDistributedCache(MsgVpnDistributedCache body, String msgVpnName, String cacheName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheResponse> resp = updateMsgVpnDistributedCacheWithHttpInfo(body, msgVpnName, cacheName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Update a Distributed Cache object.
     * Update a Distributed Cache object. Any attribute missing from the request will be left unchanged.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnDistributedCache|scheduledDeleteMsgDayList|scheduledDeleteMsgTimeList| MsgVpnDistributedCache|scheduledDeleteMsgTimeList|scheduledDeleteMsgDayList|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Distributed Cache object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheResponse> updateMsgVpnDistributedCacheWithHttpInfo(MsgVpnDistributedCache body, String msgVpnName, String cacheName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = updateMsgVpnDistributedCacheValidateBeforeCall(body, msgVpnName, cacheName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Update a Distributed Cache object. (asynchronously)
     * Update a Distributed Cache object. Any attribute missing from the request will be left unchanged.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnDistributedCache|scheduledDeleteMsgDayList|scheduledDeleteMsgTimeList| MsgVpnDistributedCache|scheduledDeleteMsgTimeList|scheduledDeleteMsgDayList|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Distributed Cache object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnDistributedCacheAsync(MsgVpnDistributedCache body, String msgVpnName, String cacheName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = updateMsgVpnDistributedCacheValidateBeforeCall(body, msgVpnName, cacheName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for updateMsgVpnDistributedCacheCluster
     * @param body The Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnDistributedCacheClusterCall(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
        return apiClient.buildCall(localVarPath, "PATCH", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call updateMsgVpnDistributedCacheClusterValidateBeforeCall(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling updateMsgVpnDistributedCacheCluster(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling updateMsgVpnDistributedCacheCluster(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling updateMsgVpnDistributedCacheCluster(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling updateMsgVpnDistributedCacheCluster(Async)");
        }
        
        com.squareup.okhttp.Call call = updateMsgVpnDistributedCacheClusterCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Update a Cache Cluster object.
     * Update a Cache Cluster object. Any attribute missing from the request will be left unchanged.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent| EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterResponse updateMsgVpnDistributedCacheCluster(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterResponse> resp = updateMsgVpnDistributedCacheClusterWithHttpInfo(body, msgVpnName, cacheName, clusterName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Update a Cache Cluster object.
     * Update a Cache Cluster object. Any attribute missing from the request will be left unchanged.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent| EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterResponse> updateMsgVpnDistributedCacheClusterWithHttpInfo(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = updateMsgVpnDistributedCacheClusterValidateBeforeCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Update a Cache Cluster object. (asynchronously)
     * Update a Cache Cluster object. Any attribute missing from the request will be left unchanged.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent| EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Cluster object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnDistributedCacheClusterAsync(MsgVpnDistributedCacheCluster body, String msgVpnName, String cacheName, String clusterName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = updateMsgVpnDistributedCacheClusterValidateBeforeCall(body, msgVpnName, cacheName, clusterName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for updateMsgVpnDistributedCacheClusterInstance
     * @param body The Cache Instance object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnDistributedCacheClusterInstanceCall(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;
        
        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "instanceName" + "\\}", apiClient.escapeString(instanceName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (opaquePassword != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("opaquePassword", opaquePassword));
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
        return apiClient.buildCall(localVarPath, "PATCH", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call updateMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling updateMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling updateMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling updateMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling updateMsgVpnDistributedCacheClusterInstance(Async)");
        }
        // verify the required parameter 'instanceName' is set
        if (instanceName == null) {
            throw new ApiException("Missing the required parameter 'instanceName' when calling updateMsgVpnDistributedCacheClusterInstance(Async)");
        }
        
        com.squareup.okhttp.Call call = updateMsgVpnDistributedCacheClusterInstanceCall(body, msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Update a Cache Instance object.
     * Update a Cache Instance object. Any attribute missing from the request will be left unchanged.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| instanceName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Instance object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterInstanceResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterInstanceResponse updateMsgVpnDistributedCacheClusterInstance(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterInstanceResponse> resp = updateMsgVpnDistributedCacheClusterInstanceWithHttpInfo(body, msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select);
        return resp.getData();
    }

    /**
     * Update a Cache Instance object.
     * Update a Cache Instance object. Any attribute missing from the request will be left unchanged.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| instanceName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Instance object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterInstanceResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterInstanceResponse> updateMsgVpnDistributedCacheClusterInstanceWithHttpInfo(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = updateMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(body, msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstanceResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Update a Cache Instance object. (asynchronously)
     * Update a Cache Instance object. Any attribute missing from the request will be left unchanged.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| instanceName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cache Instance object&#x27;s attributes. (required)
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param opaquePassword Accept opaque attributes in the request or return opaque attributes in the response, encrypted with the specified password. See that documentation for the &#x60;opaquePassword&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnDistributedCacheClusterInstanceAsync(MsgVpnDistributedCacheClusterInstance body, String msgVpnName, String cacheName, String clusterName, String instanceName, String opaquePassword, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterInstanceResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = updateMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(body, msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstanceResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
