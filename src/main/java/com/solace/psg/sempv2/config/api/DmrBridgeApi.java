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
import com.solace.psg.sempv2.config.model.MsgVpnDmrBridge;
import com.solace.psg.sempv2.config.model.MsgVpnDmrBridgeResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDmrBridgesResponse;
import com.solace.psg.sempv2.config.model.SempMetaOnlyResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DmrBridgeApi {
    private ApiClient apiClient;

    public DmrBridgeApi() {
        this(Configuration.getDefaultApiClient());
    }

    public DmrBridgeApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for createMsgVpnDmrBridge
     * @param msgVpnName The name of the Message VPN. (required)
     * @param body The DMR Bridge object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDmrBridgeCall(String msgVpnName, MsgVpnDmrBridge body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/dmrBridges"
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
    private com.squareup.okhttp.Call createMsgVpnDmrBridgeValidateBeforeCall(String msgVpnName, MsgVpnDmrBridge body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnDmrBridge(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnDmrBridge(Async)");
        }
        

        com.squareup.okhttp.Call call = createMsgVpnDmrBridgeCall(msgVpnName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Create a DMR Bridge object.
     * Create a DMR Bridge object. Any attribute missing from the request will be set to its default value.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x|| remoteNodeName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param body The DMR Bridge object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDmrBridgeResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDmrBridgeResponse createMsgVpnDmrBridge(String msgVpnName, MsgVpnDmrBridge body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDmrBridgeResponse> resp = createMsgVpnDmrBridgeWithHttpInfo(msgVpnName, body, select);
        return resp.getData();
    }

    /**
     * Create a DMR Bridge object.
     * Create a DMR Bridge object. Any attribute missing from the request will be set to its default value.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x|| remoteNodeName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param body The DMR Bridge object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDmrBridgeResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDmrBridgeResponse> createMsgVpnDmrBridgeWithHttpInfo(String msgVpnName, MsgVpnDmrBridge body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnDmrBridgeValidateBeforeCall(msgVpnName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDmrBridgeResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a DMR Bridge object. (asynchronously)
     * Create a DMR Bridge object. Any attribute missing from the request will be set to its default value.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x|| remoteNodeName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param body The DMR Bridge object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnDmrBridgeAsync(String msgVpnName, MsgVpnDmrBridge body, List<String> select, final ApiCallback<MsgVpnDmrBridgeResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnDmrBridgeValidateBeforeCall(msgVpnName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDmrBridgeResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnDmrBridge
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDmrBridgeCall(String msgVpnName, String remoteNodeName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/dmrBridges/{remoteNodeName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
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
    private com.squareup.okhttp.Call deleteMsgVpnDmrBridgeValidateBeforeCall(String msgVpnName, String remoteNodeName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnDmrBridge(Async)");
        }
        
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling deleteMsgVpnDmrBridge(Async)");
        }
        

        com.squareup.okhttp.Call call = deleteMsgVpnDmrBridgeCall(msgVpnName, remoteNodeName, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete a DMR Bridge object.
     * Delete a DMR Bridge object.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnDmrBridge(String msgVpnName, String remoteNodeName) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnDmrBridgeWithHttpInfo(msgVpnName, remoteNodeName);
        return resp.getData();
    }

    /**
     * Delete a DMR Bridge object.
     * Delete a DMR Bridge object.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnDmrBridgeWithHttpInfo(String msgVpnName, String remoteNodeName) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnDmrBridgeValidateBeforeCall(msgVpnName, remoteNodeName, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a DMR Bridge object. (asynchronously)
     * Delete a DMR Bridge object.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnDmrBridgeAsync(String msgVpnName, String remoteNodeName, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnDmrBridgeValidateBeforeCall(msgVpnName, remoteNodeName, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDmrBridge
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDmrBridgeCall(String msgVpnName, String remoteNodeName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/dmrBridges/{remoteNodeName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
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
    private com.squareup.okhttp.Call getMsgVpnDmrBridgeValidateBeforeCall(String msgVpnName, String remoteNodeName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDmrBridge(Async)");
        }
        
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling getMsgVpnDmrBridge(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnDmrBridgeCall(msgVpnName, remoteNodeName, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a DMR Bridge object.
     * Get a DMR Bridge object.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDmrBridgeResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDmrBridgeResponse getMsgVpnDmrBridge(String msgVpnName, String remoteNodeName, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDmrBridgeResponse> resp = getMsgVpnDmrBridgeWithHttpInfo(msgVpnName, remoteNodeName, select);
        return resp.getData();
    }

    /**
     * Get a DMR Bridge object.
     * Get a DMR Bridge object.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDmrBridgeResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDmrBridgeResponse> getMsgVpnDmrBridgeWithHttpInfo(String msgVpnName, String remoteNodeName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDmrBridgeValidateBeforeCall(msgVpnName, remoteNodeName, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDmrBridgeResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a DMR Bridge object. (asynchronously)
     * Get a DMR Bridge object.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDmrBridgeAsync(String msgVpnName, String remoteNodeName, List<String> select, final ApiCallback<MsgVpnDmrBridgeResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDmrBridgeValidateBeforeCall(msgVpnName, remoteNodeName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDmrBridgeResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnDmrBridges
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
    public com.squareup.okhttp.Call getMsgVpnDmrBridgesCall(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/dmrBridges"
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
    private com.squareup.okhttp.Call getMsgVpnDmrBridgesValidateBeforeCall(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnDmrBridges(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnDmrBridgesCall(msgVpnName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of DMR Bridge objects.
     * Get a list of DMR Bridge objects.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDmrBridgesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDmrBridgesResponse getMsgVpnDmrBridges(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDmrBridgesResponse> resp = getMsgVpnDmrBridgesWithHttpInfo(msgVpnName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of DMR Bridge objects.
     * Get a list of DMR Bridge objects.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDmrBridgesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDmrBridgesResponse> getMsgVpnDmrBridgesWithHttpInfo(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnDmrBridgesValidateBeforeCall(msgVpnName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDmrBridgesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of DMR Bridge objects. (asynchronously)
     * Get a list of DMR Bridge objects.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| remoteNodeName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnDmrBridgesAsync(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnDmrBridgesResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnDmrBridgesValidateBeforeCall(msgVpnName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDmrBridgesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for replaceMsgVpnDmrBridge
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param body The DMR Bridge object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnDmrBridgeCall(String msgVpnName, String remoteNodeName, MsgVpnDmrBridge body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/dmrBridges/{remoteNodeName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
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
    private com.squareup.okhttp.Call replaceMsgVpnDmrBridgeValidateBeforeCall(String msgVpnName, String remoteNodeName, MsgVpnDmrBridge body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling replaceMsgVpnDmrBridge(Async)");
        }
        
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling replaceMsgVpnDmrBridge(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling replaceMsgVpnDmrBridge(Async)");
        }
        

        com.squareup.okhttp.Call call = replaceMsgVpnDmrBridgeCall(msgVpnName, remoteNodeName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Replace a DMR Bridge object.
     * Replace a DMR Bridge object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x||| remoteNodeName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param body The DMR Bridge object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDmrBridgeResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDmrBridgeResponse replaceMsgVpnDmrBridge(String msgVpnName, String remoteNodeName, MsgVpnDmrBridge body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDmrBridgeResponse> resp = replaceMsgVpnDmrBridgeWithHttpInfo(msgVpnName, remoteNodeName, body, select);
        return resp.getData();
    }

    /**
     * Replace a DMR Bridge object.
     * Replace a DMR Bridge object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x||| remoteNodeName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param body The DMR Bridge object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDmrBridgeResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDmrBridgeResponse> replaceMsgVpnDmrBridgeWithHttpInfo(String msgVpnName, String remoteNodeName, MsgVpnDmrBridge body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = replaceMsgVpnDmrBridgeValidateBeforeCall(msgVpnName, remoteNodeName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDmrBridgeResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Replace a DMR Bridge object. (asynchronously)
     * Replace a DMR Bridge object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x||| remoteNodeName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param body The DMR Bridge object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnDmrBridgeAsync(String msgVpnName, String remoteNodeName, MsgVpnDmrBridge body, List<String> select, final ApiCallback<MsgVpnDmrBridgeResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = replaceMsgVpnDmrBridgeValidateBeforeCall(msgVpnName, remoteNodeName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDmrBridgeResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for updateMsgVpnDmrBridge
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param body The DMR Bridge object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnDmrBridgeCall(String msgVpnName, String remoteNodeName, MsgVpnDmrBridge body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/dmrBridges/{remoteNodeName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
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
    private com.squareup.okhttp.Call updateMsgVpnDmrBridgeValidateBeforeCall(String msgVpnName, String remoteNodeName, MsgVpnDmrBridge body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling updateMsgVpnDmrBridge(Async)");
        }
        
        // verify the required parameter 'remoteNodeName' is set
        if (remoteNodeName == null) {
            throw new ApiException("Missing the required parameter 'remoteNodeName' when calling updateMsgVpnDmrBridge(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling updateMsgVpnDmrBridge(Async)");
        }
        

        com.squareup.okhttp.Call call = updateMsgVpnDmrBridgeCall(msgVpnName, remoteNodeName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Update a DMR Bridge object.
     * Update a DMR Bridge object. Any attribute missing from the request will be left unchanged.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x||| remoteNodeName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param body The DMR Bridge object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnDmrBridgeResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnDmrBridgeResponse updateMsgVpnDmrBridge(String msgVpnName, String remoteNodeName, MsgVpnDmrBridge body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnDmrBridgeResponse> resp = updateMsgVpnDmrBridgeWithHttpInfo(msgVpnName, remoteNodeName, body, select);
        return resp.getData();
    }

    /**
     * Update a DMR Bridge object.
     * Update a DMR Bridge object. Any attribute missing from the request will be left unchanged.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x||| remoteNodeName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param body The DMR Bridge object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnDmrBridgeResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnDmrBridgeResponse> updateMsgVpnDmrBridgeWithHttpInfo(String msgVpnName, String remoteNodeName, MsgVpnDmrBridge body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = updateMsgVpnDmrBridgeValidateBeforeCall(msgVpnName, remoteNodeName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnDmrBridgeResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Update a DMR Bridge object. (asynchronously)
     * Update a DMR Bridge object. Any attribute missing from the request will be left unchanged.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x||| remoteNodeName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param remoteNodeName The name of the node at the remote end of the DMR Bridge. (required)
     * @param body The DMR Bridge object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnDmrBridgeAsync(String msgVpnName, String remoteNodeName, MsgVpnDmrBridge body, List<String> select, final ApiCallback<MsgVpnDmrBridgeResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = updateMsgVpnDmrBridgeValidateBeforeCall(msgVpnName, remoteNodeName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnDmrBridgeResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
