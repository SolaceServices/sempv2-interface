/*
 * SEMP (Solace Element Management Protocol)
 * SEMP (starting in `v2`, see note 1) is a RESTful API for configuring, monitoring, and administering a Solace PubSub+ broker.  SEMP uses URIs to address manageable **resources** of the Solace PubSub+ broker. Resources are individual **objects**, **collections** of objects, or (exclusively in the action API) **actions**. This document applies to the following API:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Action|/SEMP/v2/action|Performing actions|See note 2    The following APIs are also available:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Configuration|/SEMP/v2/config|Reading and writing config state|See note 2 Monitoring|/SEMP/v2/monitor|Querying operational parameters|See note 2    Resources are always nouns, with individual objects being singular and collections being plural.  Objects within a collection are identified by an `obj-id`, which follows the collection name with the form `collection-name/obj-id`.  Actions within an object are identified by an `action-id`, which follows the object name with the form `obj-id/action-id`.  Some examples:  ``` /SEMP/v2/config/msgVpns                        ; MsgVpn collection /SEMP/v2/config/msgVpns/a                      ; MsgVpn object named \"a\" /SEMP/v2/config/msgVpns/a/queues               ; Queue collection in MsgVpn \"a\" /SEMP/v2/config/msgVpns/a/queues/b             ; Queue object named \"b\" in MsgVpn \"a\" /SEMP/v2/action/msgVpns/a/queues/b/startReplay ; Action that starts a replay on Queue \"b\" in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients             ; Client collection in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients/c           ; Client object named \"c\" in MsgVpn \"a\" ```  ## Collection Resources  Collections are unordered lists of objects (unless described as otherwise), and are described by JSON arrays. Each item in the array represents an object in the same manner as the individual object would normally be represented. In the configuration API, the creation of a new object is done through its collection resource.  ## Object and Action Resources  Objects are composed of attributes, actions, collections, and other objects. They are described by JSON objects as name/value pairs. The collections and actions of an object are not contained directly in the object's JSON content; rather the content includes an attribute containing a URI which points to the collections and actions. These contained resources must be managed through this URI. At a minimum, every object has one or more identifying attributes, and its own `uri` attribute which contains the URI pointing to itself.  Actions are also composed of attributes, and are described by JSON objects as name/value pairs. Unlike objects, however, they are not members of a collection and cannot be retrieved, only performed. Actions only exist in the action API.  Attributes in an object or action may have any (non-exclusively) of the following properties:   Property|Meaning|Comments :---|:---|:--- Identifying|Attribute is involved in unique identification of the object, and appears in its URI| Required|Attribute must be provided in the request| Read-Only|Attribute can only be read, not written|See note 3 Write-Only|Attribute can only be written, not read| Requires-Disable|Attribute can only be changed when object is disabled| Deprecated|Attribute is deprecated, and will disappear in the next SEMP version|    In some requests, certain attributes may only be provided in certain combinations with other attributes:   Relationship|Meaning :---|:--- Requires|Attribute may only be changed by a request if a particular attribute or combination of attributes is also provided in the request Conflicts|Attribute may only be provided in a request if a particular attribute or combination of attributes is not also provided in the request    ## HTTP Methods  The following HTTP methods manipulate resources in accordance with these general principles. Note that some methods are only used in certain APIs:   Method|Resource|Meaning|Request Body|Response Body|Missing Request Attributes :---|:---|:---|:---|:---|:--- POST|Collection|Create object|Initial attribute values|Object attributes and metadata|Set to default PUT|Object|Create or replace object|New attribute values|Object attributes and metadata|Set to default (but see note 4) PUT|Action|Performs action|Action arguments|Action metadata|N/A PATCH|Object|Update object|New attribute values|Object attributes and metadata|unchanged DELETE|Object|Delete object|Empty|Object metadata|N/A GET|Object|Get object|Empty|Object attributes and metadata|N/A GET|Collection|Get collection|Empty|Object attributes and collection metadata|N/A    ## Common Query Parameters  The following are some common query parameters that are supported by many method/URI combinations. Individual URIs may document additional parameters. Note that multiple query parameters can be used together in a single URI, separated by the ampersand character. For example:  ``` ; Request for the MsgVpns collection using two hypothetical query parameters \"q1\" and \"q2\" ; with values \"val1\" and \"val2\" respectively /SEMP/v2/action/msgVpns?q1=val1&q2=val2 ```  ### select  Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. Use this query parameter to limit the size of the returned data for each returned object, return only those fields that are desired, or exclude fields that are not desired.  The value of `select` is a comma-separated list of attribute names. If the list contains attribute names that are not prefaced by `-`, only those attributes are included in the response. If the list contains attribute names that are prefaced by `-`, those attributes are excluded from the response. If the list contains both types, then the difference of the first set of attributes and the second set of attributes is returned. If the list is empty (i.e. `select=`), no attributes are returned.  All attributes that are prefaced by `-` must follow all attributes that are not prefaced by `-`. In addition, each attribute name in the list must match at least one attribute in the object.  Names may include the `*` wildcard (zero or more characters). Nested attribute names are supported using periods (e.g. `parentName.childName`).  Some examples:  ``` ; List of all MsgVpn names /SEMP/v2/action/msgVpns?select=msgVpnName ; List of all MsgVpn and their attributes except for their names /SEMP/v2/action/msgVpns?select=-msgVpnName ; Authentication attributes of MsgVpn \"finance\" /SEMP/v2/action/msgVpns/finance?select=authentication* ; All attributes of MsgVpn \"finance\" except for authentication attributes /SEMP/v2/action/msgVpns/finance?select=-authentication* ; Access related attributes of Queue \"orderQ\" of MsgVpn \"finance\" /SEMP/v2/action/msgVpns/finance/queues/orderQ?select=owner,permission ```  ### where  Include in the response only objects where certain conditions are true. Use this query parameter to limit which objects are returned to those whose attribute values meet the given conditions.  The value of `where` is a comma-separated list of expressions. All expressions must be true for the object to be included in the response. Each expression takes the form:  ``` expression  = attribute-name OP value OP          = '==' | '!=' | '&lt;' | '&gt;' | '&lt;=' | '&gt;=' ```  `value` may be a number, string, `true`, or `false`, as appropriate for the type of `attribute-name`. Greater-than and less-than comparisons only work for numbers. A `*` in a string `value` is interpreted as a wildcard (zero or more characters). Some examples:  ``` ; Only enabled MsgVpns /SEMP/v2/action/msgVpns?where=enabled==true ; Only MsgVpns using basic non-LDAP authentication /SEMP/v2/action/msgVpns?where=authenticationBasicEnabled==true,authenticationBasicType!=ldap ; Only MsgVpns that allow more than 100 client connections /SEMP/v2/action/msgVpns?where=maxConnectionCount>100 ; Only MsgVpns with msgVpnName starting with \"B\": /SEMP/v2/action/msgVpns?where=msgVpnName==B* ```  ### count  Limit the count of objects in the response. This can be useful to limit the size of the response for large collections. The minimum value for `count` is `1` and the default is `10`. There is also a per-collection maximum value to limit request handling time. For example:  ``` ; Up to 25 MsgVpns /SEMP/v2/action/msgVpns?count=25 ```  ### cursor  The cursor, or position, for the next page of objects. Cursors are opaque data that should not be created or interpreted by SEMP clients, and should only be used as described below.  When a request is made for a collection and there may be additional objects available for retrieval that are not included in the initial response, the response will include a `cursorQuery` field containing a cursor. The value of this field can be specified in the `cursor` query parameter of a subsequent request to retrieve the next page of objects. For convenience, an appropriate URI is constructed automatically by the broker and included in the `nextPageUri` field of the response. This URI can be used directly to retrieve the next page of objects.  ## Notes  Note|Description :---:|:--- 1|This specification defines SEMP starting in \"v2\", and not the original SEMP \"v1\" interface. Request and response formats between \"v1\" and \"v2\" are entirely incompatible, although both protocols share a common port configuration on the Solace PubSub+ broker. They are differentiated by the initial portion of the URI path, one of either \"/SEMP/\" or \"/SEMP/v2/\" 2|This API is partially implemented. Only a subset of all objects are available. 3|Read-only attributes may appear in POST and PUT/PATCH requests. However, if a read-only attribute is not marked as identifying, it will be ignored during a PUT/PATCH. 4|For PUT, if the SEMP user is not authorized to modify the attribute, its value is left unchanged rather than set to default. In addition, the values of write-only attributes are not set to their defaults on a PUT. If the object does not exist, it is created first.    
 *
 * OpenAPI spec version: 9.4
 * Contact: support@solace.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package com.solace.psg.sempv2.action.api;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;

import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheClusterInstanceBackupCachedMsgs;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheClusterInstanceClearEvent;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheClusterInstanceClearStats;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheClusterInstanceDeleteMsgs;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheClusterInstanceResponse;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheClusterInstanceStart;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheClusterInstancesResponse;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheClusterResponse;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheClustersResponse;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCacheResponse;
import com.solace.psg.sempv2.action.model.MsgVpnDistributedCachesResponse;
import com.solace.psg.sempv2.action.model.SempMetaOnlyResponse;
import com.solace.psg.sempv2.apiclient.ApiCallback;
import com.solace.psg.sempv2.apiclient.ApiClient;
import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.apiclient.ApiResponse;
import com.solace.psg.sempv2.apiclient.Configuration;
import com.solace.psg.sempv2.apiclient.Pair;
import com.solace.psg.sempv2.apiclient.ProgressRequestBody;
import com.solace.psg.sempv2.apiclient.ProgressResponseBody;
import com.solace.psg.sempv2.auth.HttpBasicAuth;

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
     * Build call for doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgs
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Backup Cached Messages action&#39;s attributes. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgsCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceBackupCachedMsgs body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}/backupCachedMsgs"
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
    private com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgsValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceBackupCachedMsgs body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgs(Async)");
        }
        
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgs(Async)");
        }
        
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgs(Async)");
        }
        
        // verify the required parameter 'instanceName' is set
        if (instanceName == null) {
            throw new ApiException("Missing the required parameter 'instanceName' when calling doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgs(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgs(Async)");
        }
        

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgsCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Backup cached messages of the Cache Instance to disk.
     * Backup cached messages of the Cache Instance to disk.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Backup Cached Messages action&#39;s attributes. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgs(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceBackupCachedMsgs body) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgsWithHttpInfo(msgVpnName, cacheName, clusterName, instanceName, body);
        return resp.getData();
    }

    /**
     * Backup cached messages of the Cache Instance to disk.
     * Backup cached messages of the Cache Instance to disk.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Backup Cached Messages action&#39;s attributes. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgsWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceBackupCachedMsgs body) throws ApiException {
        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgsValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Backup cached messages of the Cache Instance to disk. (asynchronously)
     * Backup cached messages of the Cache Instance to disk.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Backup Cached Messages action&#39;s attributes. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgsAsync(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceBackupCachedMsgs body, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgsValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Cancel Backup Cached Messages action&#39;s attributes. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgsCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}/cancelBackupCachedMsgs"
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
    private com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgsValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs(Async)");
        }
        
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs(Async)");
        }
        
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs(Async)");
        }
        
        // verify the required parameter 'instanceName' is set
        if (instanceName == null) {
            throw new ApiException("Missing the required parameter 'instanceName' when calling doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs(Async)");
        }
        

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgsCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Cancel the backup of cached messages from the Cache Instance.
     * Cancel the backup of cached messages from the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Cancel Backup Cached Messages action&#39;s attributes. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs body) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgsWithHttpInfo(msgVpnName, cacheName, clusterName, instanceName, body);
        return resp.getData();
    }

    /**
     * Cancel the backup of cached messages from the Cache Instance.
     * Cancel the backup of cached messages from the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Cancel Backup Cached Messages action&#39;s attributes. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgsWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs body) throws ApiException {
        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgsValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Cancel the backup of cached messages from the Cache Instance. (asynchronously)
     * Cancel the backup of cached messages from the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Cancel Backup Cached Messages action&#39;s attributes. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgsAsync(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs body, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgsValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Cancel Restore Cached Messages action&#39;s attributes. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgsCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}/cancelRestoreCachedMsgs"
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
    private com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgsValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs(Async)");
        }
        
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs(Async)");
        }
        
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs(Async)");
        }
        
        // verify the required parameter 'instanceName' is set
        if (instanceName == null) {
            throw new ApiException("Missing the required parameter 'instanceName' when calling doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs(Async)");
        }
        

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgsCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Cancel the restore of cached messages to the Cache Instance.
     * Cancel the restore of cached messages to the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Cancel Restore Cached Messages action&#39;s attributes. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs body) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgsWithHttpInfo(msgVpnName, cacheName, clusterName, instanceName, body);
        return resp.getData();
    }

    /**
     * Cancel the restore of cached messages to the Cache Instance.
     * Cancel the restore of cached messages to the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Cancel Restore Cached Messages action&#39;s attributes. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgsWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs body) throws ApiException {
        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgsValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Cancel the restore of cached messages to the Cache Instance. (asynchronously)
     * Cancel the restore of cached messages to the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Cancel Restore Cached Messages action&#39;s attributes. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgsAsync(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs body, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgsValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for doMsgVpnDistributedCacheClusterInstanceClearEvent
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Clear Event action&#39;s attributes. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceClearEventCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceClearEvent body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}/clearEvent"
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
    private com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceClearEventValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceClearEvent body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling doMsgVpnDistributedCacheClusterInstanceClearEvent(Async)");
        }
        
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling doMsgVpnDistributedCacheClusterInstanceClearEvent(Async)");
        }
        
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling doMsgVpnDistributedCacheClusterInstanceClearEvent(Async)");
        }
        
        // verify the required parameter 'instanceName' is set
        if (instanceName == null) {
            throw new ApiException("Missing the required parameter 'instanceName' when calling doMsgVpnDistributedCacheClusterInstanceClearEvent(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling doMsgVpnDistributedCacheClusterInstanceClearEvent(Async)");
        }
        

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceClearEventCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Clear an event for the Cache Instance so it can be generated anew.
     * Clear an event for the Cache Instance so it can be generated anew.   Attribute|Required|Deprecated :---|:---:|:---: eventName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Clear Event action&#39;s attributes. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse doMsgVpnDistributedCacheClusterInstanceClearEvent(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceClearEvent body) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = doMsgVpnDistributedCacheClusterInstanceClearEventWithHttpInfo(msgVpnName, cacheName, clusterName, instanceName, body);
        return resp.getData();
    }

    /**
     * Clear an event for the Cache Instance so it can be generated anew.
     * Clear an event for the Cache Instance so it can be generated anew.   Attribute|Required|Deprecated :---|:---:|:---: eventName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Clear Event action&#39;s attributes. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> doMsgVpnDistributedCacheClusterInstanceClearEventWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceClearEvent body) throws ApiException {
        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceClearEventValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Clear an event for the Cache Instance so it can be generated anew. (asynchronously)
     * Clear an event for the Cache Instance so it can be generated anew.   Attribute|Required|Deprecated :---|:---:|:---: eventName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Clear Event action&#39;s attributes. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceClearEventAsync(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceClearEvent body, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceClearEventValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for doMsgVpnDistributedCacheClusterInstanceClearStats
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Clear Stats action&#39;s attributes. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceClearStatsCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceClearStats body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}/clearStats"
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
    private com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceClearStatsValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceClearStats body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling doMsgVpnDistributedCacheClusterInstanceClearStats(Async)");
        }
        
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling doMsgVpnDistributedCacheClusterInstanceClearStats(Async)");
        }
        
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling doMsgVpnDistributedCacheClusterInstanceClearStats(Async)");
        }
        
        // verify the required parameter 'instanceName' is set
        if (instanceName == null) {
            throw new ApiException("Missing the required parameter 'instanceName' when calling doMsgVpnDistributedCacheClusterInstanceClearStats(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling doMsgVpnDistributedCacheClusterInstanceClearStats(Async)");
        }
        

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceClearStatsCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Clear the statistics for the Cache Instance.
     * Clear the statistics for the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Clear Stats action&#39;s attributes. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse doMsgVpnDistributedCacheClusterInstanceClearStats(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceClearStats body) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = doMsgVpnDistributedCacheClusterInstanceClearStatsWithHttpInfo(msgVpnName, cacheName, clusterName, instanceName, body);
        return resp.getData();
    }

    /**
     * Clear the statistics for the Cache Instance.
     * Clear the statistics for the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Clear Stats action&#39;s attributes. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> doMsgVpnDistributedCacheClusterInstanceClearStatsWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceClearStats body) throws ApiException {
        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceClearStatsValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Clear the statistics for the Cache Instance. (asynchronously)
     * Clear the statistics for the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Clear Stats action&#39;s attributes. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceClearStatsAsync(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceClearStats body, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceClearStatsValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for doMsgVpnDistributedCacheClusterInstanceDeleteMsgs
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Delete Messages action&#39;s attributes. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceDeleteMsgsCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceDeleteMsgs body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}/deleteMsgs"
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
    private com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceDeleteMsgsValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceDeleteMsgs body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling doMsgVpnDistributedCacheClusterInstanceDeleteMsgs(Async)");
        }
        
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling doMsgVpnDistributedCacheClusterInstanceDeleteMsgs(Async)");
        }
        
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling doMsgVpnDistributedCacheClusterInstanceDeleteMsgs(Async)");
        }
        
        // verify the required parameter 'instanceName' is set
        if (instanceName == null) {
            throw new ApiException("Missing the required parameter 'instanceName' when calling doMsgVpnDistributedCacheClusterInstanceDeleteMsgs(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling doMsgVpnDistributedCacheClusterInstanceDeleteMsgs(Async)");
        }
        

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceDeleteMsgsCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete messages covered by the given topic in the Cache Instance.
     * Delete messages covered by the given topic in the Cache Instance.   Attribute|Required|Deprecated :---|:---:|:---: topic|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Delete Messages action&#39;s attributes. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse doMsgVpnDistributedCacheClusterInstanceDeleteMsgs(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceDeleteMsgs body) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = doMsgVpnDistributedCacheClusterInstanceDeleteMsgsWithHttpInfo(msgVpnName, cacheName, clusterName, instanceName, body);
        return resp.getData();
    }

    /**
     * Delete messages covered by the given topic in the Cache Instance.
     * Delete messages covered by the given topic in the Cache Instance.   Attribute|Required|Deprecated :---|:---:|:---: topic|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Delete Messages action&#39;s attributes. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> doMsgVpnDistributedCacheClusterInstanceDeleteMsgsWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceDeleteMsgs body) throws ApiException {
        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceDeleteMsgsValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete messages covered by the given topic in the Cache Instance. (asynchronously)
     * Delete messages covered by the given topic in the Cache Instance.   Attribute|Required|Deprecated :---|:---:|:---: topic|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Delete Messages action&#39;s attributes. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceDeleteMsgsAsync(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceDeleteMsgs body, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceDeleteMsgsValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Restore Cached Messages action&#39;s attributes. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgsCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}/restoreCachedMsgs"
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
    private com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgsValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs(Async)");
        }
        
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs(Async)");
        }
        
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs(Async)");
        }
        
        // verify the required parameter 'instanceName' is set
        if (instanceName == null) {
            throw new ApiException("Missing the required parameter 'instanceName' when calling doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs(Async)");
        }
        

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgsCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Restore cached messages for the Cache Instance from disk.
     * Restore cached messages for the Cache Instance from disk.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Restore Cached Messages action&#39;s attributes. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs body) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgsWithHttpInfo(msgVpnName, cacheName, clusterName, instanceName, body);
        return resp.getData();
    }

    /**
     * Restore cached messages for the Cache Instance from disk.
     * Restore cached messages for the Cache Instance from disk.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Restore Cached Messages action&#39;s attributes. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgsWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs body) throws ApiException {
        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgsValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Restore cached messages for the Cache Instance from disk. (asynchronously)
     * Restore cached messages for the Cache Instance from disk.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Restore Cached Messages action&#39;s attributes. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgsAsync(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs body, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgsValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for doMsgVpnDistributedCacheClusterInstanceStart
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Start Cache Instance action&#39;s attributes. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceStartCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceStart body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}/start"
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
    private com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceStartValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceStart body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling doMsgVpnDistributedCacheClusterInstanceStart(Async)");
        }
        
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling doMsgVpnDistributedCacheClusterInstanceStart(Async)");
        }
        
        // verify the required parameter 'clusterName' is set
        if (clusterName == null) {
            throw new ApiException("Missing the required parameter 'clusterName' when calling doMsgVpnDistributedCacheClusterInstanceStart(Async)");
        }
        
        // verify the required parameter 'instanceName' is set
        if (instanceName == null) {
            throw new ApiException("Missing the required parameter 'instanceName' when calling doMsgVpnDistributedCacheClusterInstanceStart(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling doMsgVpnDistributedCacheClusterInstanceStart(Async)");
        }
        

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceStartCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Start the Cache Instance.
     * Start the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Start Cache Instance action&#39;s attributes. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse doMsgVpnDistributedCacheClusterInstanceStart(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceStart body) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = doMsgVpnDistributedCacheClusterInstanceStartWithHttpInfo(msgVpnName, cacheName, clusterName, instanceName, body);
        return resp.getData();
    }

    /**
     * Start the Cache Instance.
     * Start the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Start Cache Instance action&#39;s attributes. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> doMsgVpnDistributedCacheClusterInstanceStartWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceStart body) throws ApiException {
        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceStartValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Start the Cache Instance. (asynchronously)
     * Start the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param body The Start Cache Instance action&#39;s attributes. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call doMsgVpnDistributedCacheClusterInstanceStartAsync(String msgVpnName, String cacheName, String clusterName, String instanceName, MsgVpnDistributedCacheClusterInstanceStart body, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = doMsgVpnDistributedCacheClusterInstanceStartValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, body, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCache
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheCall(String msgVpnName, String cacheName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheValidateBeforeCall(String msgVpnName, String cacheName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCache(Async)");
        }
        
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCache(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheCall(msgVpnName, cacheName, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Distributed Cache object.
     * Get a Distributed Cache object.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheResponse getMsgVpnDistributedCache(String msgVpnName, String cacheName, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheResponse> resp = getMsgVpnDistributedCacheWithHttpInfo(msgVpnName, cacheName, select);
        return resp.getData();
    }

    /**
     * Get a Distributed Cache object.
     * Get a Distributed Cache object.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheResponse> getMsgVpnDistributedCacheWithHttpInfo(String msgVpnName, String cacheName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheValidateBeforeCall(msgVpnName, cacheName, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Distributed Cache object. (asynchronously)
     * Get a Distributed Cache object.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheAsync(String msgVpnName, String cacheName, List<String> select, final ApiCallback<MsgVpnDistributedCacheResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheValidateBeforeCall(msgVpnName, cacheName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheCluster
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterCall(String msgVpnName, String cacheName, String clusterName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
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
        

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterCall(msgVpnName, cacheName, clusterName, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Cache Cluster object.
     * Get a Cache Cluster object.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterResponse getMsgVpnDistributedCacheCluster(String msgVpnName, String cacheName, String clusterName, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterResponse> resp = getMsgVpnDistributedCacheClusterWithHttpInfo(msgVpnName, cacheName, clusterName, select);
        return resp.getData();
    }

    /**
     * Get a Cache Cluster object.
     * Get a Cache Cluster object.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterResponse> getMsgVpnDistributedCacheClusterWithHttpInfo(String msgVpnName, String cacheName, String clusterName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterValidateBeforeCall(msgVpnName, cacheName, clusterName, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Cache Cluster object. (asynchronously)
     * Get a Cache Cluster object.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterAsync(String msgVpnName, String cacheName, String clusterName, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterValidateBeforeCall(msgVpnName, cacheName, clusterName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheClusterInstance
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterInstanceCall(String msgVpnName, String cacheName, String clusterName, String instanceName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/distributedCaches/{cacheName}/clusters/{clusterName}/instances/{instanceName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "cacheName" + "\\}", apiClient.escapeString(cacheName.toString()))
            .replaceAll("\\{" + "clusterName" + "\\}", apiClient.escapeString(clusterName.toString()))
            .replaceAll("\\{" + "instanceName" + "\\}", apiClient.escapeString(instanceName.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, String instanceName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
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
        

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterInstanceCall(msgVpnName, cacheName, clusterName, instanceName, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Cache Instance object.
     * Get a Cache Instance object.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| instanceName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterInstanceResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterInstanceResponse getMsgVpnDistributedCacheClusterInstance(String msgVpnName, String cacheName, String clusterName, String instanceName, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterInstanceResponse> resp = getMsgVpnDistributedCacheClusterInstanceWithHttpInfo(msgVpnName, cacheName, clusterName, instanceName, select);
        return resp.getData();
    }

    /**
     * Get a Cache Instance object.
     * Get a Cache Instance object.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| instanceName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterInstanceResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterInstanceResponse> getMsgVpnDistributedCacheClusterInstanceWithHttpInfo(String msgVpnName, String cacheName, String clusterName, String instanceName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstanceResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Cache Instance object. (asynchronously)
     * Get a Cache Instance object.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| instanceName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param instanceName The name of the Cache Instance. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterInstanceAsync(String msgVpnName, String cacheName, String clusterName, String instanceName, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterInstanceResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterInstanceValidateBeforeCall(msgVpnName, cacheName, clusterName, instanceName, select, progressListener, progressRequestListener);
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
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterInstancesCall(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterInstancesValidateBeforeCall(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
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
        

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterInstancesCall(msgVpnName, cacheName, clusterName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Cache Instance objects.
     * Get a list of Cache Instance objects.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| instanceName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClusterInstancesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClusterInstancesResponse getMsgVpnDistributedCacheClusterInstances(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClusterInstancesResponse> resp = getMsgVpnDistributedCacheClusterInstancesWithHttpInfo(msgVpnName, cacheName, clusterName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Cache Instance objects.
     * Get a list of Cache Instance objects.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| instanceName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClusterInstancesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClusterInstancesResponse> getMsgVpnDistributedCacheClusterInstancesWithHttpInfo(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterInstancesValidateBeforeCall(msgVpnName, cacheName, clusterName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstancesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Cache Instance objects. (asynchronously)
     * Get a list of Cache Instance objects.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| instanceName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param clusterName The name of the Cache Cluster. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClusterInstancesAsync(String msgVpnName, String cacheName, String clusterName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnDistributedCacheClusterInstancesResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClusterInstancesValidateBeforeCall(msgVpnName, cacheName, clusterName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClusterInstancesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCacheClusters
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClustersCall(String msgVpnName, String cacheName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCacheClustersValidateBeforeCall(String msgVpnName, String cacheName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCacheClusters(Async)");
        }
        
        // verify the required parameter 'cacheName' is set
        if (cacheName == null) {
            throw new ApiException("Missing the required parameter 'cacheName' when calling getMsgVpnDistributedCacheClusters(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClustersCall(msgVpnName, cacheName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Cache Cluster objects.
     * Get a list of Cache Cluster objects.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCacheClustersResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCacheClustersResponse getMsgVpnDistributedCacheClusters(String msgVpnName, String cacheName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCacheClustersResponse> resp = getMsgVpnDistributedCacheClustersWithHttpInfo(msgVpnName, cacheName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Cache Cluster objects.
     * Get a list of Cache Cluster objects.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCacheClustersResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCacheClustersResponse> getMsgVpnDistributedCacheClustersWithHttpInfo(String msgVpnName, String cacheName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClustersValidateBeforeCall(msgVpnName, cacheName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClustersResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Cache Cluster objects. (asynchronously)
     * Get a list of Cache Cluster objects.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param cacheName The name of the Distributed Cache. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCacheClustersAsync(String msgVpnName, String cacheName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnDistributedCacheClustersResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCacheClustersValidateBeforeCall(msgVpnName, cacheName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCacheClustersResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDistributedCaches
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
    public com.squareup.okhttp.Call getMsgVpnDistributedCachesCall(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
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
    private com.squareup.okhttp.Call getMsgVpnDistributedCachesValidateBeforeCall(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDistributedCaches(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnDistributedCachesCall(msgVpnName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Distributed Cache objects.
     * Get a list of Distributed Cache objects.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDistributedCachesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDistributedCachesResponse getMsgVpnDistributedCaches(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDistributedCachesResponse> resp = getMsgVpnDistributedCachesWithHttpInfo(msgVpnName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Distributed Cache objects.
     * Get a list of Distributed Cache objects.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDistributedCachesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDistributedCachesResponse> getMsgVpnDistributedCachesWithHttpInfo(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDistributedCachesValidateBeforeCall(msgVpnName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCachesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Distributed Cache objects. (asynchronously)
     * Get a list of Distributed Cache objects.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDistributedCachesAsync(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnDistributedCachesResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDistributedCachesValidateBeforeCall(msgVpnName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDistributedCachesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
