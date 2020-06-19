/*
 * SEMP (Solace Element Management Protocol)
 * SEMP (starting in `v2`, see note 1) is a RESTful API for configuring, monitoring, and administering a Solace PubSub+ broker.  SEMP uses URIs to address manageable **resources** of the Solace PubSub+ broker. Resources are individual **objects**, **collections** of objects, or (exclusively in the action API) **actions**. This document applies to the following API:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Configuration|/SEMP/v2/config|Reading and writing config state|See note 2    The following APIs are also available:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Action|/SEMP/v2/action|Performing actions|See note 2 Monitoring|/SEMP/v2/monitor|Querying operational parameters|See note 2    Resources are always nouns, with individual objects being singular and collections being plural.  Objects within a collection are identified by an `obj-id`, which follows the collection name with the form `collection-name/obj-id`.  Actions within an object are identified by an `action-id`, which follows the object name with the form `obj-id/action-id`.  Some examples:  ``` /SEMP/v2/config/msgVpns                        ; MsgVpn collection /SEMP/v2/config/msgVpns/a                      ; MsgVpn object named \"a\" /SEMP/v2/config/msgVpns/a/queues               ; Queue collection in MsgVpn \"a\" /SEMP/v2/config/msgVpns/a/queues/b             ; Queue object named \"b\" in MsgVpn \"a\" /SEMP/v2/action/msgVpns/a/queues/b/startReplay ; Action that starts a replay on Queue \"b\" in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients             ; Client collection in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients/c           ; Client object named \"c\" in MsgVpn \"a\" ```  ## Collection Resources  Collections are unordered lists of objects (unless described as otherwise), and are described by JSON arrays. Each item in the array represents an object in the same manner as the individual object would normally be represented. In the configuration API, the creation of a new object is done through its collection resource.  ## Object and Action Resources  Objects are composed of attributes, actions, collections, and other objects. They are described by JSON objects as name/value pairs. The collections and actions of an object are not contained directly in the object's JSON content; rather the content includes an attribute containing a URI which points to the collections and actions. These contained resources must be managed through this URI. At a minimum, every object has one or more identifying attributes, and its own `uri` attribute which contains the URI pointing to itself.  Actions are also composed of attributes, and are described by JSON objects as name/value pairs. Unlike objects, however, they are not members of a collection and cannot be retrieved, only performed. Actions only exist in the action API.  Attributes in an object or action may have any (non-exclusively) of the following properties:   Property|Meaning|Comments :---|:---|:--- Identifying|Attribute is involved in unique identification of the object, and appears in its URI| Required|Attribute must be provided in the request| Read-Only|Attribute can only be read, not written|See note 3 Write-Only|Attribute can only be written, not read| Requires-Disable|Attribute can only be changed when object is disabled| Deprecated|Attribute is deprecated, and will disappear in the next SEMP version|    In some requests, certain attributes may only be provided in certain combinations with other attributes:   Relationship|Meaning :---|:--- Requires|Attribute may only be changed by a request if a particular attribute or combination of attributes is also provided in the request Conflicts|Attribute may only be provided in a request if a particular attribute or combination of attributes is not also provided in the request    ## HTTP Methods  The following HTTP methods manipulate resources in accordance with these general principles. Note that some methods are only used in certain APIs:   Method|Resource|Meaning|Request Body|Response Body|Missing Request Attributes :---|:---|:---|:---|:---|:--- POST|Collection|Create object|Initial attribute values|Object attributes and metadata|Set to default PUT|Object|Create or replace object|New attribute values|Object attributes and metadata|Set to default (but see note 4) PUT|Action|Performs action|Action arguments|Action metadata|N/A PATCH|Object|Update object|New attribute values|Object attributes and metadata|unchanged DELETE|Object|Delete object|Empty|Object metadata|N/A GET|Object|Get object|Empty|Object attributes and metadata|N/A GET|Collection|Get collection|Empty|Object attributes and collection metadata|N/A    ## Common Query Parameters  The following are some common query parameters that are supported by many method/URI combinations. Individual URIs may document additional parameters. Note that multiple query parameters can be used together in a single URI, separated by the ampersand character. For example:  ``` ; Request for the MsgVpns collection using two hypothetical query parameters \"q1\" and \"q2\" ; with values \"val1\" and \"val2\" respectively /SEMP/v2/config/msgVpns?q1=val1&q2=val2 ```  ### select  Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. Use this query parameter to limit the size of the returned data for each returned object, return only those fields that are desired, or exclude fields that are not desired.  The value of `select` is a comma-separated list of attribute names. If the list contains attribute names that are not prefaced by `-`, only those attributes are included in the response. If the list contains attribute names that are prefaced by `-`, those attributes are excluded from the response. If the list contains both types, then the difference of the first set of attributes and the second set of attributes is returned. If the list is empty (i.e. `select=`), no attributes are returned.  All attributes that are prefaced by `-` must follow all attributes that are not prefaced by `-`. In addition, each attribute name in the list must match at least one attribute in the object.  Names may include the `*` wildcard (zero or more characters). Nested attribute names are supported using periods (e.g. `parentName.childName`).  Some examples:  ``` ; List of all MsgVpn names /SEMP/v2/config/msgVpns?select=msgVpnName ; List of all MsgVpn and their attributes except for their names /SEMP/v2/config/msgVpns?select=-msgVpnName ; Authentication attributes of MsgVpn \"finance\" /SEMP/v2/config/msgVpns/finance?select=authentication* ; All attributes of MsgVpn \"finance\" except for authentication attributes /SEMP/v2/config/msgVpns/finance?select=-authentication* ; Access related attributes of Queue \"orderQ\" of MsgVpn \"finance\" /SEMP/v2/config/msgVpns/finance/queues/orderQ?select=owner,permission ```  ### where  Include in the response only objects where certain conditions are true. Use this query parameter to limit which objects are returned to those whose attribute values meet the given conditions.  The value of `where` is a comma-separated list of expressions. All expressions must be true for the object to be included in the response. Each expression takes the form:  ``` expression  = attribute-name OP value OP          = '==' | '!=' | '&lt;' | '&gt;' | '&lt;=' | '&gt;=' ```  `value` may be a number, string, `true`, or `false`, as appropriate for the type of `attribute-name`. Greater-than and less-than comparisons only work for numbers. A `*` in a string `value` is interpreted as a wildcard (zero or more characters). Some examples:  ``` ; Only enabled MsgVpns /SEMP/v2/config/msgVpns?where=enabled==true ; Only MsgVpns using basic non-LDAP authentication /SEMP/v2/config/msgVpns?where=authenticationBasicEnabled==true,authenticationBasicType!=ldap ; Only MsgVpns that allow more than 100 client connections /SEMP/v2/config/msgVpns?where=maxConnectionCount>100 ; Only MsgVpns with msgVpnName starting with \"B\": /SEMP/v2/config/msgVpns?where=msgVpnName==B* ```  ### count  Limit the count of objects in the response. This can be useful to limit the size of the response for large collections. The minimum value for `count` is `1` and the default is `10`. There is also a per-collection maximum value to limit request handling time. For example:  ``` ; Up to 25 MsgVpns /SEMP/v2/config/msgVpns?count=25 ```  ### cursor  The cursor, or position, for the next page of objects. Cursors are opaque data that should not be created or interpreted by SEMP clients, and should only be used as described below.  When a request is made for a collection and there may be additional objects available for retrieval that are not included in the initial response, the response will include a `cursorQuery` field containing a cursor. The value of this field can be specified in the `cursor` query parameter of a subsequent request to retrieve the next page of objects. For convenience, an appropriate URI is constructed automatically by the broker and included in the `nextPageUri` field of the response. This URI can be used directly to retrieve the next page of objects.  ## Notes  Note|Description :---:|:--- 1|This specification defines SEMP starting in \"v2\", and not the original SEMP \"v1\" interface. Request and response formats between \"v1\" and \"v2\" are entirely incompatible, although both protocols share a common port configuration on the Solace PubSub+ broker. They are differentiated by the initial portion of the URI path, one of either \"/SEMP/\" or \"/SEMP/v2/\" 2|This API is partially implemented. Only a subset of all objects are available. 3|Read-only attributes may appear in POST and PUT/PATCH requests. However, if a read-only attribute is not marked as identifying, it will be ignored during a PUT/PATCH. 4|For PUT, if the SEMP user is not authorized to modify the attribute, its value is left unchanged rather than set to default. In addition, the values of write-only attributes are not set to their defaults on a PUT. If the object does not exist, it is created first.    
 *
 * OpenAPI spec version: 2.14
 * Contact: support@solace.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package com.solace.psg.sempv2.config.api;

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
import com.solace.psg.sempv2.auth.HttpBasicAuth;
import com.solace.psg.sempv2.config.model.DmrCluster;
import com.solace.psg.sempv2.config.model.DmrClusterLink;
import com.solace.psg.sempv2.config.model.DmrClusterLinkRemoteAddress;
import com.solace.psg.sempv2.config.model.DmrClusterLinkRemoteAddressResponse;
import com.solace.psg.sempv2.config.model.DmrClusterLinkRemoteAddressesResponse;
import com.solace.psg.sempv2.config.model.DmrClusterLinkResponse;
import com.solace.psg.sempv2.config.model.DmrClusterLinkTlsTrustedCommonName;
import com.solace.psg.sempv2.config.model.DmrClusterLinkTlsTrustedCommonNameResponse;
import com.solace.psg.sempv2.config.model.DmrClusterLinkTlsTrustedCommonNamesResponse;
import com.solace.psg.sempv2.config.model.DmrClusterLinksResponse;
import com.solace.psg.sempv2.config.model.DmrClusterResponse;
import com.solace.psg.sempv2.config.model.DmrClustersResponse;
import com.solace.psg.sempv2.config.model.SempMetaOnlyResponse;

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
     * Build call for createDmrCluster
     * @param body The Cluster object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createDmrClusterCall(DmrCluster body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/dmrClusters";

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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call createDmrClusterValidateBeforeCall(DmrCluster body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createDmrCluster(Async)");
        }
        

        com.squareup.okhttp.Call call = createDmrClusterCall(body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Create a Cluster object.
     * Create a Cluster object. Any attribute missing from the request will be set to its default value.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword||||x| authenticationClientCertContent||||x| authenticationClientCertPassword||||x| dmrClusterName|x|x||| nodeName|||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- DmrCluster|authenticationClientCertPassword|authenticationClientCertContent|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cluster object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterResponse createDmrCluster(DmrCluster body, List<String> select) throws ApiException {
        ApiResponse<DmrClusterResponse> resp = createDmrClusterWithHttpInfo(body, select);
        return resp.getData();
    }

    /**
     * Create a Cluster object.
     * Create a Cluster object. Any attribute missing from the request will be set to its default value.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword||||x| authenticationClientCertContent||||x| authenticationClientCertPassword||||x| dmrClusterName|x|x||| nodeName|||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- DmrCluster|authenticationClientCertPassword|authenticationClientCertContent|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cluster object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterResponse> createDmrClusterWithHttpInfo(DmrCluster body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createDmrClusterValidateBeforeCall(body, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Cluster object. (asynchronously)
     * Create a Cluster object. Any attribute missing from the request will be set to its default value.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword||||x| authenticationClientCertContent||||x| authenticationClientCertPassword||||x| dmrClusterName|x|x||| nodeName|||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- DmrCluster|authenticationClientCertPassword|authenticationClientCertContent|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param body The Cluster object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createDmrClusterAsync(DmrCluster body, List<String> select, final ApiCallback<DmrClusterResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createDmrClusterValidateBeforeCall(body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for createDmrClusterLink
     * @param dmrClusterName The name of the Cluster. (required)
     * @param body The Link object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createDmrClusterLinkCall(String dmrClusterName, DmrClusterLink body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links"
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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call createDmrClusterLinkValidateBeforeCall(String dmrClusterName, DmrClusterLink body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling createDmrClusterLink(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createDmrClusterLink(Async)");
        }
        

        com.squareup.okhttp.Call call = createDmrClusterLinkCall(dmrClusterName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Create a Link object.
     * Create a Link object. Any attribute missing from the request will be set to its default value.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword||||x| dmrClusterName|x||x|| remoteNodeName|x|x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param body The Link object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinkResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinkResponse createDmrClusterLink(String dmrClusterName, DmrClusterLink body, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinkResponse> resp = createDmrClusterLinkWithHttpInfo(dmrClusterName, body, select);
        return resp.getData();
    }

    /**
     * Create a Link object.
     * Create a Link object. Any attribute missing from the request will be set to its default value.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword||||x| dmrClusterName|x||x|| remoteNodeName|x|x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param body The Link object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinkResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinkResponse> createDmrClusterLinkWithHttpInfo(String dmrClusterName, DmrClusterLink body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createDmrClusterLinkValidateBeforeCall(dmrClusterName, body, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinkResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Link object. (asynchronously)
     * Create a Link object. Any attribute missing from the request will be set to its default value.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword||||x| dmrClusterName|x||x|| remoteNodeName|x|x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param body The Link object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createDmrClusterLinkAsync(String dmrClusterName, DmrClusterLink body, List<String> select, final ApiCallback<DmrClusterLinkResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createDmrClusterLinkValidateBeforeCall(dmrClusterName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinkResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for createDmrClusterLinkRemoteAddress
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Remote Address object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createDmrClusterLinkRemoteAddressCall(String dmrClusterName, String remoteNodeName, DmrClusterLinkRemoteAddress body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links/{remoteNodeName}/remoteAddresses"
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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call createDmrClusterLinkRemoteAddressValidateBeforeCall(String dmrClusterName, String remoteNodeName, DmrClusterLinkRemoteAddress body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling createDmrClusterLinkRemoteAddress(Async)");
        }
        
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling createDmrClusterLinkRemoteAddress(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createDmrClusterLinkRemoteAddress(Async)");
        }
        

        com.squareup.okhttp.Call call = createDmrClusterLinkRemoteAddressCall(dmrClusterName, remoteNodeName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Create a Remote Address object.
     * Create a Remote Address object. Any attribute missing from the request will be set to its default value.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: dmrClusterName|x||x|| remoteAddress|x|x||| remoteNodeName|x||x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Remote Address object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinkRemoteAddressResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinkRemoteAddressResponse createDmrClusterLinkRemoteAddress(String dmrClusterName, String remoteNodeName, DmrClusterLinkRemoteAddress body, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinkRemoteAddressResponse> resp = createDmrClusterLinkRemoteAddressWithHttpInfo(dmrClusterName, remoteNodeName, body, select);
        return resp.getData();
    }

    /**
     * Create a Remote Address object.
     * Create a Remote Address object. Any attribute missing from the request will be set to its default value.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: dmrClusterName|x||x|| remoteAddress|x|x||| remoteNodeName|x||x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Remote Address object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinkRemoteAddressResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinkRemoteAddressResponse> createDmrClusterLinkRemoteAddressWithHttpInfo(String dmrClusterName, String remoteNodeName, DmrClusterLinkRemoteAddress body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createDmrClusterLinkRemoteAddressValidateBeforeCall(dmrClusterName, remoteNodeName, body, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinkRemoteAddressResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Remote Address object. (asynchronously)
     * Create a Remote Address object. Any attribute missing from the request will be set to its default value.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: dmrClusterName|x||x|| remoteAddress|x|x||| remoteNodeName|x||x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Remote Address object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createDmrClusterLinkRemoteAddressAsync(String dmrClusterName, String remoteNodeName, DmrClusterLinkRemoteAddress body, List<String> select, final ApiCallback<DmrClusterLinkRemoteAddressResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createDmrClusterLinkRemoteAddressValidateBeforeCall(dmrClusterName, remoteNodeName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinkRemoteAddressResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for createDmrClusterLinkTlsTrustedCommonName
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Trusted Common Name object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createDmrClusterLinkTlsTrustedCommonNameCall(String dmrClusterName, String remoteNodeName, DmrClusterLinkTlsTrustedCommonName body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links/{remoteNodeName}/tlsTrustedCommonNames"
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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call createDmrClusterLinkTlsTrustedCommonNameValidateBeforeCall(String dmrClusterName, String remoteNodeName, DmrClusterLinkTlsTrustedCommonName body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling createDmrClusterLinkTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling createDmrClusterLinkTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createDmrClusterLinkTlsTrustedCommonName(Async)");
        }
        

        com.squareup.okhttp.Call call = createDmrClusterLinkTlsTrustedCommonNameCall(dmrClusterName, remoteNodeName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Create a Trusted Common Name object.
     * Create a Trusted Common Name object. Any attribute missing from the request will be set to its default value.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: dmrClusterName|x||x|| remoteNodeName|x||x|| tlsTrustedCommonName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Trusted Common Name object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinkTlsTrustedCommonNameResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinkTlsTrustedCommonNameResponse createDmrClusterLinkTlsTrustedCommonName(String dmrClusterName, String remoteNodeName, DmrClusterLinkTlsTrustedCommonName body, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinkTlsTrustedCommonNameResponse> resp = createDmrClusterLinkTlsTrustedCommonNameWithHttpInfo(dmrClusterName, remoteNodeName, body, select);
        return resp.getData();
    }

    /**
     * Create a Trusted Common Name object.
     * Create a Trusted Common Name object. Any attribute missing from the request will be set to its default value.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: dmrClusterName|x||x|| remoteNodeName|x||x|| tlsTrustedCommonName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Trusted Common Name object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinkTlsTrustedCommonNameResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinkTlsTrustedCommonNameResponse> createDmrClusterLinkTlsTrustedCommonNameWithHttpInfo(String dmrClusterName, String remoteNodeName, DmrClusterLinkTlsTrustedCommonName body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createDmrClusterLinkTlsTrustedCommonNameValidateBeforeCall(dmrClusterName, remoteNodeName, body, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinkTlsTrustedCommonNameResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Trusted Common Name object. (asynchronously)
     * Create a Trusted Common Name object. Any attribute missing from the request will be set to its default value.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: dmrClusterName|x||x|| remoteNodeName|x||x|| tlsTrustedCommonName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Trusted Common Name object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createDmrClusterLinkTlsTrustedCommonNameAsync(String dmrClusterName, String remoteNodeName, DmrClusterLinkTlsTrustedCommonName body, List<String> select, final ApiCallback<DmrClusterLinkTlsTrustedCommonNameResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createDmrClusterLinkTlsTrustedCommonNameValidateBeforeCall(dmrClusterName, remoteNodeName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinkTlsTrustedCommonNameResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteDmrCluster
     * @param dmrClusterName The name of the Cluster. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteDmrClusterCall(String dmrClusterName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()));

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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call deleteDmrClusterValidateBeforeCall(String dmrClusterName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling deleteDmrCluster(Async)");
        }
        

        com.squareup.okhttp.Call call = deleteDmrClusterCall(dmrClusterName, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete a Cluster object.
     * Delete a Cluster object.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteDmrCluster(String dmrClusterName) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteDmrClusterWithHttpInfo(dmrClusterName);
        return resp.getData();
    }

    /**
     * Delete a Cluster object.
     * Delete a Cluster object.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteDmrClusterWithHttpInfo(String dmrClusterName) throws ApiException {
        com.squareup.okhttp.Call call = deleteDmrClusterValidateBeforeCall(dmrClusterName, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Cluster object. (asynchronously)
     * Delete a Cluster object.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteDmrClusterAsync(String dmrClusterName, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteDmrClusterValidateBeforeCall(dmrClusterName, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteDmrClusterLink
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteDmrClusterLinkCall(String dmrClusterName, String remoteNodeName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links/{remoteNodeName}"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()))
            .replaceAll("\\{" + "remoteNodeName" + "\\}", apiClient.escapeString(remoteNodeName.toString()));

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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call deleteDmrClusterLinkValidateBeforeCall(String dmrClusterName, String remoteNodeName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling deleteDmrClusterLink(Async)");
        }
        
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling deleteDmrClusterLink(Async)");
        }
        

        com.squareup.okhttp.Call call = deleteDmrClusterLinkCall(dmrClusterName, remoteNodeName, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete a Link object.
     * Delete a Link object.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteDmrClusterLink(String dmrClusterName, String remoteNodeName) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteDmrClusterLinkWithHttpInfo(dmrClusterName, remoteNodeName);
        return resp.getData();
    }

    /**
     * Delete a Link object.
     * Delete a Link object.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteDmrClusterLinkWithHttpInfo(String dmrClusterName, String remoteNodeName) throws ApiException {
        com.squareup.okhttp.Call call = deleteDmrClusterLinkValidateBeforeCall(dmrClusterName, remoteNodeName, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Link object. (asynchronously)
     * Delete a Link object.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteDmrClusterLinkAsync(String dmrClusterName, String remoteNodeName, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteDmrClusterLinkValidateBeforeCall(dmrClusterName, remoteNodeName, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteDmrClusterLinkRemoteAddress
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param remoteAddress The FQDN or IP address (and optional port) of the remote node. If a port is not provided, it will vary based on the transport encoding: 55555 (plain-text), 55443 (encrypted), or 55003 (compressed). (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteDmrClusterLinkRemoteAddressCall(String dmrClusterName, String remoteNodeName, String remoteAddress, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links/{remoteNodeName}/remoteAddresses/{remoteAddress}"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()))
            .replaceAll("\\{" + "remoteNodeName" + "\\}", apiClient.escapeString(remoteNodeName.toString()))
            .replaceAll("\\{" + "remoteAddress" + "\\}", apiClient.escapeString(remoteAddress.toString()));

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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call deleteDmrClusterLinkRemoteAddressValidateBeforeCall(String dmrClusterName, String remoteNodeName, String remoteAddress, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling deleteDmrClusterLinkRemoteAddress(Async)");
        }
        
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling deleteDmrClusterLinkRemoteAddress(Async)");
        }
        
        // verify the required parameter 'remoteAddress' is set
        if (remoteAddress == null) {
            throw new ApiException("Missing the required parameter 'remoteAddress' when calling deleteDmrClusterLinkRemoteAddress(Async)");
        }
        

        com.squareup.okhttp.Call call = deleteDmrClusterLinkRemoteAddressCall(dmrClusterName, remoteNodeName, remoteAddress, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete a Remote Address object.
     * Delete a Remote Address object.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param remoteAddress The FQDN or IP address (and optional port) of the remote node. If a port is not provided, it will vary based on the transport encoding: 55555 (plain-text), 55443 (encrypted), or 55003 (compressed). (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteDmrClusterLinkRemoteAddress(String dmrClusterName, String remoteNodeName, String remoteAddress) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteDmrClusterLinkRemoteAddressWithHttpInfo(dmrClusterName, remoteNodeName, remoteAddress);
        return resp.getData();
    }

    /**
     * Delete a Remote Address object.
     * Delete a Remote Address object.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param remoteAddress The FQDN or IP address (and optional port) of the remote node. If a port is not provided, it will vary based on the transport encoding: 55555 (plain-text), 55443 (encrypted), or 55003 (compressed). (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteDmrClusterLinkRemoteAddressWithHttpInfo(String dmrClusterName, String remoteNodeName, String remoteAddress) throws ApiException {
        com.squareup.okhttp.Call call = deleteDmrClusterLinkRemoteAddressValidateBeforeCall(dmrClusterName, remoteNodeName, remoteAddress, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Remote Address object. (asynchronously)
     * Delete a Remote Address object.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param remoteAddress The FQDN or IP address (and optional port) of the remote node. If a port is not provided, it will vary based on the transport encoding: 55555 (plain-text), 55443 (encrypted), or 55003 (compressed). (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteDmrClusterLinkRemoteAddressAsync(String dmrClusterName, String remoteNodeName, String remoteAddress, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteDmrClusterLinkRemoteAddressValidateBeforeCall(dmrClusterName, remoteNodeName, remoteAddress, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteDmrClusterLinkTlsTrustedCommonName
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteDmrClusterLinkTlsTrustedCommonNameCall(String dmrClusterName, String remoteNodeName, String tlsTrustedCommonName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/dmrClusters/{dmrClusterName}/links/{remoteNodeName}/tlsTrustedCommonNames/{tlsTrustedCommonName}"
            .replaceAll("\\{" + "dmrClusterName" + "\\}", apiClient.escapeString(dmrClusterName.toString()))
            .replaceAll("\\{" + "remoteNodeName" + "\\}", apiClient.escapeString(remoteNodeName.toString()))
            .replaceAll("\\{" + "tlsTrustedCommonName" + "\\}", apiClient.escapeString(tlsTrustedCommonName.toString()));

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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call deleteDmrClusterLinkTlsTrustedCommonNameValidateBeforeCall(String dmrClusterName, String remoteNodeName, String tlsTrustedCommonName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling deleteDmrClusterLinkTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling deleteDmrClusterLinkTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'tlsTrustedCommonName' is set
        if (tlsTrustedCommonName == null) {
            throw new ApiException("Missing the required parameter 'tlsTrustedCommonName' when calling deleteDmrClusterLinkTlsTrustedCommonName(Async)");
        }
        

        com.squareup.okhttp.Call call = deleteDmrClusterLinkTlsTrustedCommonNameCall(dmrClusterName, remoteNodeName, tlsTrustedCommonName, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete a Trusted Common Name object.
     * Delete a Trusted Common Name object.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteDmrClusterLinkTlsTrustedCommonName(String dmrClusterName, String remoteNodeName, String tlsTrustedCommonName) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteDmrClusterLinkTlsTrustedCommonNameWithHttpInfo(dmrClusterName, remoteNodeName, tlsTrustedCommonName);
        return resp.getData();
    }

    /**
     * Delete a Trusted Common Name object.
     * Delete a Trusted Common Name object.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteDmrClusterLinkTlsTrustedCommonNameWithHttpInfo(String dmrClusterName, String remoteNodeName, String tlsTrustedCommonName) throws ApiException {
        com.squareup.okhttp.Call call = deleteDmrClusterLinkTlsTrustedCommonNameValidateBeforeCall(dmrClusterName, remoteNodeName, tlsTrustedCommonName, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Trusted Common Name object. (asynchronously)
     * Delete a Trusted Common Name object.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteDmrClusterLinkTlsTrustedCommonNameAsync(String dmrClusterName, String remoteNodeName, String tlsTrustedCommonName, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteDmrClusterLinkTlsTrustedCommonNameValidateBeforeCall(dmrClusterName, remoteNodeName, tlsTrustedCommonName, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
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
     * Get a Cluster object.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationBasicPassword||x| authenticationClientCertContent||x| authenticationClientCertPassword||x| dmrClusterName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a Cluster object.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationBasicPassword||x| authenticationClientCertContent||x| authenticationClientCertPassword||x| dmrClusterName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a Cluster object.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationBasicPassword||x| authenticationClientCertContent||x| authenticationClientCertPassword||x| dmrClusterName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
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
     * Get a Link object.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationBasicPassword||x| dmrClusterName|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a Link object.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationBasicPassword||x| dmrClusterName|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a Link object.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationBasicPassword||x| dmrClusterName|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
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
     * Get a Remote Address object.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: dmrClusterName|x|| remoteAddress|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a Remote Address object.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: dmrClusterName|x|| remoteAddress|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a Remote Address object.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: dmrClusterName|x|| remoteAddress|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
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
     * Get a list of Remote Address objects.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: dmrClusterName|x|| remoteAddress|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a list of Remote Address objects.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: dmrClusterName|x|| remoteAddress|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a list of Remote Address objects.  Each Remote Address, consisting of a FQDN or IP address and optional port, is used to connect to the remote node for this Link. Up to 4 addresses may be provided for each Link, and will be tried on a round-robin basis.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: dmrClusterName|x|| remoteAddress|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
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
     * Get a Trusted Common Name object.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: dmrClusterName|x|| remoteNodeName|x|| tlsTrustedCommonName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a Trusted Common Name object.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: dmrClusterName|x|| remoteNodeName|x|| tlsTrustedCommonName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a Trusted Common Name object.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: dmrClusterName|x|| remoteNodeName|x|| tlsTrustedCommonName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
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
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: dmrClusterName|x|| remoteNodeName|x|| tlsTrustedCommonName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: dmrClusterName|x|| remoteNodeName|x|| tlsTrustedCommonName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the Link are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: dmrClusterName|x|| remoteNodeName|x|| tlsTrustedCommonName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
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
     * Get a list of Link objects.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationBasicPassword||x| dmrClusterName|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a list of Link objects.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationBasicPassword||x| dmrClusterName|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a list of Link objects.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationBasicPassword||x| dmrClusterName|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getDmrClustersValidateBeforeCall(Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        

        com.squareup.okhttp.Call call = getDmrClustersCall(count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Cluster objects.
     * Get a list of Cluster objects.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationBasicPassword||x| authenticationClientCertContent||x| authenticationClientCertPassword||x| dmrClusterName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a list of Cluster objects.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationBasicPassword||x| authenticationClientCertContent||x| authenticationClientCertPassword||x| dmrClusterName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
     * Get a list of Cluster objects.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationBasicPassword||x| authenticationClientCertContent||x| authenticationClientCertPassword||x| dmrClusterName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
    /**
     * Build call for replaceDmrCluster
     * @param dmrClusterName The name of the Cluster. (required)
     * @param body The Cluster object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call replaceDmrClusterCall(String dmrClusterName, DmrCluster body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "PUT", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call replaceDmrClusterValidateBeforeCall(String dmrClusterName, DmrCluster body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling replaceDmrCluster(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling replaceDmrCluster(Async)");
        }
        

        com.squareup.okhttp.Call call = replaceDmrClusterCall(dmrClusterName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Replace a Cluster object.
     * Replace a Cluster object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword|||x|x| authenticationClientCertContent|||x|x| authenticationClientCertPassword|||x|x| directOnlyEnabled||x||| dmrClusterName|x|x||| nodeName||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- DmrCluster|authenticationClientCertPassword|authenticationClientCertContent|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param body The Cluster object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterResponse replaceDmrCluster(String dmrClusterName, DmrCluster body, List<String> select) throws ApiException {
        ApiResponse<DmrClusterResponse> resp = replaceDmrClusterWithHttpInfo(dmrClusterName, body, select);
        return resp.getData();
    }

    /**
     * Replace a Cluster object.
     * Replace a Cluster object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword|||x|x| authenticationClientCertContent|||x|x| authenticationClientCertPassword|||x|x| directOnlyEnabled||x||| dmrClusterName|x|x||| nodeName||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- DmrCluster|authenticationClientCertPassword|authenticationClientCertContent|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param body The Cluster object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterResponse> replaceDmrClusterWithHttpInfo(String dmrClusterName, DmrCluster body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = replaceDmrClusterValidateBeforeCall(dmrClusterName, body, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Replace a Cluster object. (asynchronously)
     * Replace a Cluster object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword|||x|x| authenticationClientCertContent|||x|x| authenticationClientCertPassword|||x|x| directOnlyEnabled||x||| dmrClusterName|x|x||| nodeName||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- DmrCluster|authenticationClientCertPassword|authenticationClientCertContent|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param body The Cluster object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call replaceDmrClusterAsync(String dmrClusterName, DmrCluster body, List<String> select, final ApiCallback<DmrClusterResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = replaceDmrClusterValidateBeforeCall(dmrClusterName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for replaceDmrClusterLink
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Link object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call replaceDmrClusterLinkCall(String dmrClusterName, String remoteNodeName, DmrClusterLink body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "PUT", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call replaceDmrClusterLinkValidateBeforeCall(String dmrClusterName, String remoteNodeName, DmrClusterLink body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling replaceDmrClusterLink(Async)");
        }
        
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling replaceDmrClusterLink(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling replaceDmrClusterLink(Async)");
        }
        

        com.squareup.okhttp.Call call = replaceDmrClusterLinkCall(dmrClusterName, remoteNodeName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Replace a Link object.
     * Replace a Link object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword|||x|x| authenticationScheme||||x| dmrClusterName|x|x||| egressFlowWindowSize||||x| initiator||||x| remoteNodeName|x|x||| span||||x| transportCompressedEnabled||||x| transportTlsEnabled||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Link object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinkResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinkResponse replaceDmrClusterLink(String dmrClusterName, String remoteNodeName, DmrClusterLink body, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinkResponse> resp = replaceDmrClusterLinkWithHttpInfo(dmrClusterName, remoteNodeName, body, select);
        return resp.getData();
    }

    /**
     * Replace a Link object.
     * Replace a Link object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword|||x|x| authenticationScheme||||x| dmrClusterName|x|x||| egressFlowWindowSize||||x| initiator||||x| remoteNodeName|x|x||| span||||x| transportCompressedEnabled||||x| transportTlsEnabled||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Link object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinkResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinkResponse> replaceDmrClusterLinkWithHttpInfo(String dmrClusterName, String remoteNodeName, DmrClusterLink body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = replaceDmrClusterLinkValidateBeforeCall(dmrClusterName, remoteNodeName, body, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinkResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Replace a Link object. (asynchronously)
     * Replace a Link object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword|||x|x| authenticationScheme||||x| dmrClusterName|x|x||| egressFlowWindowSize||||x| initiator||||x| remoteNodeName|x|x||| span||||x| transportCompressedEnabled||||x| transportTlsEnabled||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Link object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call replaceDmrClusterLinkAsync(String dmrClusterName, String remoteNodeName, DmrClusterLink body, List<String> select, final ApiCallback<DmrClusterLinkResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = replaceDmrClusterLinkValidateBeforeCall(dmrClusterName, remoteNodeName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinkResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for updateDmrCluster
     * @param dmrClusterName The name of the Cluster. (required)
     * @param body The Cluster object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call updateDmrClusterCall(String dmrClusterName, DmrCluster body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "PATCH", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call updateDmrClusterValidateBeforeCall(String dmrClusterName, DmrCluster body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling updateDmrCluster(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling updateDmrCluster(Async)");
        }
        

        com.squareup.okhttp.Call call = updateDmrClusterCall(dmrClusterName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Update a Cluster object.
     * Update a Cluster object. Any attribute missing from the request will be left unchanged.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword|||x|x| authenticationClientCertContent|||x|x| authenticationClientCertPassword|||x|x| directOnlyEnabled||x||| dmrClusterName|x|x||| nodeName||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- DmrCluster|authenticationClientCertPassword|authenticationClientCertContent|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param body The Cluster object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterResponse updateDmrCluster(String dmrClusterName, DmrCluster body, List<String> select) throws ApiException {
        ApiResponse<DmrClusterResponse> resp = updateDmrClusterWithHttpInfo(dmrClusterName, body, select);
        return resp.getData();
    }

    /**
     * Update a Cluster object.
     * Update a Cluster object. Any attribute missing from the request will be left unchanged.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword|||x|x| authenticationClientCertContent|||x|x| authenticationClientCertPassword|||x|x| directOnlyEnabled||x||| dmrClusterName|x|x||| nodeName||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- DmrCluster|authenticationClientCertPassword|authenticationClientCertContent|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param body The Cluster object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterResponse> updateDmrClusterWithHttpInfo(String dmrClusterName, DmrCluster body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = updateDmrClusterValidateBeforeCall(dmrClusterName, body, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Update a Cluster object. (asynchronously)
     * Update a Cluster object. Any attribute missing from the request will be left unchanged.  A Cluster is a provisioned object on a message broker that contains global DMR configuration parameters.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword|||x|x| authenticationClientCertContent|||x|x| authenticationClientCertPassword|||x|x| directOnlyEnabled||x||| dmrClusterName|x|x||| nodeName||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- DmrCluster|authenticationClientCertPassword|authenticationClientCertContent|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param body The Cluster object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call updateDmrClusterAsync(String dmrClusterName, DmrCluster body, List<String> select, final ApiCallback<DmrClusterResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = updateDmrClusterValidateBeforeCall(dmrClusterName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for updateDmrClusterLink
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Link object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call updateDmrClusterLinkCall(String dmrClusterName, String remoteNodeName, DmrClusterLink body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

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

        String[] localVarAuthNames = new String[] { HttpBasicAuth.AUTH_TYPE };
        return apiClient.buildCall(localVarPath, "PATCH", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call updateDmrClusterLinkValidateBeforeCall(String dmrClusterName, String remoteNodeName, DmrClusterLink body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'dmrClusterName' is set
        if (dmrClusterName == null) {
            throw new ApiException("Missing the required parameter 'dmrClusterName' when calling updateDmrClusterLink(Async)");
        }
        
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling updateDmrClusterLink(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling updateDmrClusterLink(Async)");
        }
        

        com.squareup.okhttp.Call call = updateDmrClusterLinkCall(dmrClusterName, remoteNodeName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Update a Link object.
     * Update a Link object. Any attribute missing from the request will be left unchanged.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword|||x|x| authenticationScheme||||x| dmrClusterName|x|x||| egressFlowWindowSize||||x| initiator||||x| remoteNodeName|x|x||| span||||x| transportCompressedEnabled||||x| transportTlsEnabled||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Link object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return DmrClusterLinkResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DmrClusterLinkResponse updateDmrClusterLink(String dmrClusterName, String remoteNodeName, DmrClusterLink body, List<String> select) throws ApiException {
        ApiResponse<DmrClusterLinkResponse> resp = updateDmrClusterLinkWithHttpInfo(dmrClusterName, remoteNodeName, body, select);
        return resp.getData();
    }

    /**
     * Update a Link object.
     * Update a Link object. Any attribute missing from the request will be left unchanged.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword|||x|x| authenticationScheme||||x| dmrClusterName|x|x||| egressFlowWindowSize||||x| initiator||||x| remoteNodeName|x|x||| span||||x| transportCompressedEnabled||||x| transportTlsEnabled||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Link object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;DmrClusterLinkResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DmrClusterLinkResponse> updateDmrClusterLinkWithHttpInfo(String dmrClusterName, String remoteNodeName, DmrClusterLink body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = updateDmrClusterLinkValidateBeforeCall(dmrClusterName, remoteNodeName, body, select, null, null);
        Type localVarReturnType = new TypeToken<DmrClusterLinkResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Update a Link object. (asynchronously)
     * Update a Link object. Any attribute missing from the request will be left unchanged.  A Link connects nodes (either within a Cluster or between two different Clusters) and allows them to exchange topology information, subscriptions and data.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationBasicPassword|||x|x| authenticationScheme||||x| dmrClusterName|x|x||| egressFlowWindowSize||||x| initiator||||x| remoteNodeName|x|x||| span||||x| transportCompressedEnabled||||x| transportTlsEnabled||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param dmrClusterName The name of the Cluster. (required)
     * @param remoteNodeName The name of the node at the remote end of the Link. (required)
     * @param body The Link object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call updateDmrClusterLinkAsync(String dmrClusterName, String remoteNodeName, DmrClusterLink body, List<String> select, final ApiCallback<DmrClusterLinkResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = updateDmrClusterLinkValidateBeforeCall(dmrClusterName, remoteNodeName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<DmrClusterLinkResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
