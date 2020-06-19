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
import com.solace.psg.sempv2.monitor.model.MsgVpnBridgeLocalSubscriptionResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnBridgeLocalSubscriptionsResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnBridgeRemoteMsgVpnResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnBridgeRemoteMsgVpnsResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnBridgeRemoteSubscriptionResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnBridgeRemoteSubscriptionsResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnBridgeResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnBridgeTlsTrustedCommonNameResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnBridgeTlsTrustedCommonNamesResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnBridgesResponse;
import com.solace.psg.sempv2.monitor.model.SempMetaOnlyResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BridgeApi {
    private ApiClient apiClient;

    public BridgeApi() {
        this(Configuration.getDefaultApiClient());
    }

    public BridgeApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for getMsgVpnBridge
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/bridges/{bridgeName},{bridgeVirtualRouter}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "bridgeName" + "\\}", apiClient.escapeString(bridgeName.toString()))
            .replaceAll("\\{" + "bridgeVirtualRouter" + "\\}", apiClient.escapeString(bridgeVirtualRouter.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnBridgeValidateBeforeCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnBridge(Async)");
        }
        
        // verify the required parameter 'bridgeName' is set
        if (bridgeName == null) {
            throw new ApiException("Missing the required parameter 'bridgeName' when calling getMsgVpnBridge(Async)");
        }
        
        // verify the required parameter 'bridgeVirtualRouter' is set
        if (bridgeVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'bridgeVirtualRouter' when calling getMsgVpnBridge(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnBridgeCall(msgVpnName, bridgeName, bridgeVirtualRouter, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Bridge object.
     * Get a Bridge object.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| counter.controlRxByteCount||x counter.controlRxMsgCount||x counter.controlTxByteCount||x counter.controlTxMsgCount||x counter.dataRxByteCount||x counter.dataRxMsgCount||x counter.dataTxByteCount||x counter.dataTxMsgCount||x counter.discardedRxMsgCount||x counter.discardedTxMsgCount||x counter.loginRxMsgCount||x counter.loginTxMsgCount||x counter.msgSpoolRxMsgCount||x counter.rxByteCount||x counter.rxMsgCount||x counter.txByteCount||x counter.txMsgCount||x msgVpnName|x| rate.averageRxByteRate||x rate.averageRxMsgRate||x rate.averageTxByteRate||x rate.averageTxMsgRate||x rate.rxByteRate||x rate.rxMsgRate||x rate.txByteRate||x rate.txMsgRate||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnBridgeResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnBridgeResponse getMsgVpnBridge(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> select) throws ApiException {
        ApiResponse<MsgVpnBridgeResponse> resp = getMsgVpnBridgeWithHttpInfo(msgVpnName, bridgeName, bridgeVirtualRouter, select);
        return resp.getData();
    }

    /**
     * Get a Bridge object.
     * Get a Bridge object.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| counter.controlRxByteCount||x counter.controlRxMsgCount||x counter.controlTxByteCount||x counter.controlTxMsgCount||x counter.dataRxByteCount||x counter.dataRxMsgCount||x counter.dataTxByteCount||x counter.dataTxMsgCount||x counter.discardedRxMsgCount||x counter.discardedTxMsgCount||x counter.loginRxMsgCount||x counter.loginTxMsgCount||x counter.msgSpoolRxMsgCount||x counter.rxByteCount||x counter.rxMsgCount||x counter.txByteCount||x counter.txMsgCount||x msgVpnName|x| rate.averageRxByteRate||x rate.averageRxMsgRate||x rate.averageTxByteRate||x rate.averageTxMsgRate||x rate.rxByteRate||x rate.rxMsgRate||x rate.txByteRate||x rate.txMsgRate||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnBridgeResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnBridgeResponse> getMsgVpnBridgeWithHttpInfo(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnBridgeValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Bridge object. (asynchronously)
     * Get a Bridge object.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| counter.controlRxByteCount||x counter.controlRxMsgCount||x counter.controlTxByteCount||x counter.controlTxMsgCount||x counter.dataRxByteCount||x counter.dataRxMsgCount||x counter.dataTxByteCount||x counter.dataTxMsgCount||x counter.discardedRxMsgCount||x counter.discardedTxMsgCount||x counter.loginRxMsgCount||x counter.loginTxMsgCount||x counter.msgSpoolRxMsgCount||x counter.rxByteCount||x counter.rxMsgCount||x counter.txByteCount||x counter.txMsgCount||x msgVpnName|x| rate.averageRxByteRate||x rate.averageRxMsgRate||x rate.averageTxByteRate||x rate.averageTxMsgRate||x rate.rxByteRate||x rate.rxMsgRate||x rate.txByteRate||x rate.txMsgRate||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeAsync(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> select, final ApiCallback<MsgVpnBridgeResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnBridgeValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnBridgeLocalSubscription
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param localSubscriptionTopic The topic of the Bridge local subscription. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeLocalSubscriptionCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String localSubscriptionTopic, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/bridges/{bridgeName},{bridgeVirtualRouter}/localSubscriptions/{localSubscriptionTopic}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "bridgeName" + "\\}", apiClient.escapeString(bridgeName.toString()))
            .replaceAll("\\{" + "bridgeVirtualRouter" + "\\}", apiClient.escapeString(bridgeVirtualRouter.toString()))
            .replaceAll("\\{" + "localSubscriptionTopic" + "\\}", apiClient.escapeString(localSubscriptionTopic.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnBridgeLocalSubscriptionValidateBeforeCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String localSubscriptionTopic, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnBridgeLocalSubscription(Async)");
        }
        
        // verify the required parameter 'bridgeName' is set
        if (bridgeName == null) {
            throw new ApiException("Missing the required parameter 'bridgeName' when calling getMsgVpnBridgeLocalSubscription(Async)");
        }
        
        // verify the required parameter 'bridgeVirtualRouter' is set
        if (bridgeVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'bridgeVirtualRouter' when calling getMsgVpnBridgeLocalSubscription(Async)");
        }
        
        // verify the required parameter 'localSubscriptionTopic' is set
        if (localSubscriptionTopic == null) {
            throw new ApiException("Missing the required parameter 'localSubscriptionTopic' when calling getMsgVpnBridgeLocalSubscription(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnBridgeLocalSubscriptionCall(msgVpnName, bridgeName, bridgeVirtualRouter, localSubscriptionTopic, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Bridge Local Subscriptions object.
     * Get a Bridge Local Subscriptions object.  A Local Subscription is a topic subscription used by a remote Message VPN Bridge to attract messages from this broker.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| localSubscriptionTopic|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param localSubscriptionTopic The topic of the Bridge local subscription. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnBridgeLocalSubscriptionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnBridgeLocalSubscriptionResponse getMsgVpnBridgeLocalSubscription(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String localSubscriptionTopic, List<String> select) throws ApiException {
        ApiResponse<MsgVpnBridgeLocalSubscriptionResponse> resp = getMsgVpnBridgeLocalSubscriptionWithHttpInfo(msgVpnName, bridgeName, bridgeVirtualRouter, localSubscriptionTopic, select);
        return resp.getData();
    }

    /**
     * Get a Bridge Local Subscriptions object.
     * Get a Bridge Local Subscriptions object.  A Local Subscription is a topic subscription used by a remote Message VPN Bridge to attract messages from this broker.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| localSubscriptionTopic|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param localSubscriptionTopic The topic of the Bridge local subscription. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnBridgeLocalSubscriptionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnBridgeLocalSubscriptionResponse> getMsgVpnBridgeLocalSubscriptionWithHttpInfo(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String localSubscriptionTopic, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnBridgeLocalSubscriptionValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, localSubscriptionTopic, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeLocalSubscriptionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Bridge Local Subscriptions object. (asynchronously)
     * Get a Bridge Local Subscriptions object.  A Local Subscription is a topic subscription used by a remote Message VPN Bridge to attract messages from this broker.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| localSubscriptionTopic|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param localSubscriptionTopic The topic of the Bridge local subscription. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeLocalSubscriptionAsync(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String localSubscriptionTopic, List<String> select, final ApiCallback<MsgVpnBridgeLocalSubscriptionResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnBridgeLocalSubscriptionValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, localSubscriptionTopic, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeLocalSubscriptionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnBridgeLocalSubscriptions
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeLocalSubscriptionsCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/bridges/{bridgeName},{bridgeVirtualRouter}/localSubscriptions"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "bridgeName" + "\\}", apiClient.escapeString(bridgeName.toString()))
            .replaceAll("\\{" + "bridgeVirtualRouter" + "\\}", apiClient.escapeString(bridgeVirtualRouter.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnBridgeLocalSubscriptionsValidateBeforeCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnBridgeLocalSubscriptions(Async)");
        }
        
        // verify the required parameter 'bridgeName' is set
        if (bridgeName == null) {
            throw new ApiException("Missing the required parameter 'bridgeName' when calling getMsgVpnBridgeLocalSubscriptions(Async)");
        }
        
        // verify the required parameter 'bridgeVirtualRouter' is set
        if (bridgeVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'bridgeVirtualRouter' when calling getMsgVpnBridgeLocalSubscriptions(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnBridgeLocalSubscriptionsCall(msgVpnName, bridgeName, bridgeVirtualRouter, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Bridge Local Subscriptions objects.
     * Get a list of Bridge Local Subscriptions objects.  A Local Subscription is a topic subscription used by a remote Message VPN Bridge to attract messages from this broker.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| localSubscriptionTopic|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnBridgeLocalSubscriptionsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnBridgeLocalSubscriptionsResponse getMsgVpnBridgeLocalSubscriptions(String msgVpnName, String bridgeName, String bridgeVirtualRouter, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnBridgeLocalSubscriptionsResponse> resp = getMsgVpnBridgeLocalSubscriptionsWithHttpInfo(msgVpnName, bridgeName, bridgeVirtualRouter, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Bridge Local Subscriptions objects.
     * Get a list of Bridge Local Subscriptions objects.  A Local Subscription is a topic subscription used by a remote Message VPN Bridge to attract messages from this broker.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| localSubscriptionTopic|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnBridgeLocalSubscriptionsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnBridgeLocalSubscriptionsResponse> getMsgVpnBridgeLocalSubscriptionsWithHttpInfo(String msgVpnName, String bridgeName, String bridgeVirtualRouter, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnBridgeLocalSubscriptionsValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeLocalSubscriptionsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Bridge Local Subscriptions objects. (asynchronously)
     * Get a list of Bridge Local Subscriptions objects.  A Local Subscription is a topic subscription used by a remote Message VPN Bridge to attract messages from this broker.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| localSubscriptionTopic|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeLocalSubscriptionsAsync(String msgVpnName, String bridgeName, String bridgeVirtualRouter, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnBridgeLocalSubscriptionsResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnBridgeLocalSubscriptionsValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeLocalSubscriptionsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnBridgeRemoteMsgVpn
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param remoteMsgVpnName The name of the remote Message VPN. (required)
     * @param remoteMsgVpnLocation The location of the remote Message VPN as either an FQDN with port, IP address with port, or virtual router name (starting with \&quot;v:\&quot;). (required)
     * @param remoteMsgVpnInterface The physical interface on the local Message VPN host for connecting to the remote Message VPN. By default, an interface is chosen automatically (recommended), but if specified, &#x60;remoteMsgVpnLocation&#x60; must not be a virtual router name. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeRemoteMsgVpnCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String remoteMsgVpnName, String remoteMsgVpnLocation, String remoteMsgVpnInterface, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/bridges/{bridgeName},{bridgeVirtualRouter}/remoteMsgVpns/{remoteMsgVpnName},{remoteMsgVpnLocation},{remoteMsgVpnInterface}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "bridgeName" + "\\}", apiClient.escapeString(bridgeName.toString()))
            .replaceAll("\\{" + "bridgeVirtualRouter" + "\\}", apiClient.escapeString(bridgeVirtualRouter.toString()))
            .replaceAll("\\{" + "remoteMsgVpnName" + "\\}", apiClient.escapeString(remoteMsgVpnName.toString()))
            .replaceAll("\\{" + "remoteMsgVpnLocation" + "\\}", apiClient.escapeString(remoteMsgVpnLocation.toString()))
            .replaceAll("\\{" + "remoteMsgVpnInterface" + "\\}", apiClient.escapeString(remoteMsgVpnInterface.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnBridgeRemoteMsgVpnValidateBeforeCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String remoteMsgVpnName, String remoteMsgVpnLocation, String remoteMsgVpnInterface, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnBridgeRemoteMsgVpn(Async)");
        }
        
        // verify the required parameter 'bridgeName' is set
        if (bridgeName == null) {
            throw new ApiException("Missing the required parameter 'bridgeName' when calling getMsgVpnBridgeRemoteMsgVpn(Async)");
        }
        
        // verify the required parameter 'bridgeVirtualRouter' is set
        if (bridgeVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'bridgeVirtualRouter' when calling getMsgVpnBridgeRemoteMsgVpn(Async)");
        }
        
        // verify the required parameter 'remoteMsgVpnName' is set
        if (remoteMsgVpnName == null) {
            throw new ApiException("Missing the required parameter 'remoteMsgVpnName' when calling getMsgVpnBridgeRemoteMsgVpn(Async)");
        }
        
        // verify the required parameter 'remoteMsgVpnLocation' is set
        if (remoteMsgVpnLocation == null) {
            throw new ApiException("Missing the required parameter 'remoteMsgVpnLocation' when calling getMsgVpnBridgeRemoteMsgVpn(Async)");
        }
        
        // verify the required parameter 'remoteMsgVpnInterface' is set
        if (remoteMsgVpnInterface == null) {
            throw new ApiException("Missing the required parameter 'remoteMsgVpnInterface' when calling getMsgVpnBridgeRemoteMsgVpn(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnBridgeRemoteMsgVpnCall(msgVpnName, bridgeName, bridgeVirtualRouter, remoteMsgVpnName, remoteMsgVpnLocation, remoteMsgVpnInterface, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Remote Message VPN object.
     * Get a Remote Message VPN object.  The Remote Message VPN is the Message VPN that the Bridge connects to.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| remoteMsgVpnInterface|x| remoteMsgVpnLocation|x| remoteMsgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param remoteMsgVpnName The name of the remote Message VPN. (required)
     * @param remoteMsgVpnLocation The location of the remote Message VPN as either an FQDN with port, IP address with port, or virtual router name (starting with \&quot;v:\&quot;). (required)
     * @param remoteMsgVpnInterface The physical interface on the local Message VPN host for connecting to the remote Message VPN. By default, an interface is chosen automatically (recommended), but if specified, &#x60;remoteMsgVpnLocation&#x60; must not be a virtual router name. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnBridgeRemoteMsgVpnResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnBridgeRemoteMsgVpnResponse getMsgVpnBridgeRemoteMsgVpn(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String remoteMsgVpnName, String remoteMsgVpnLocation, String remoteMsgVpnInterface, List<String> select) throws ApiException {
        ApiResponse<MsgVpnBridgeRemoteMsgVpnResponse> resp = getMsgVpnBridgeRemoteMsgVpnWithHttpInfo(msgVpnName, bridgeName, bridgeVirtualRouter, remoteMsgVpnName, remoteMsgVpnLocation, remoteMsgVpnInterface, select);
        return resp.getData();
    }

    /**
     * Get a Remote Message VPN object.
     * Get a Remote Message VPN object.  The Remote Message VPN is the Message VPN that the Bridge connects to.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| remoteMsgVpnInterface|x| remoteMsgVpnLocation|x| remoteMsgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param remoteMsgVpnName The name of the remote Message VPN. (required)
     * @param remoteMsgVpnLocation The location of the remote Message VPN as either an FQDN with port, IP address with port, or virtual router name (starting with \&quot;v:\&quot;). (required)
     * @param remoteMsgVpnInterface The physical interface on the local Message VPN host for connecting to the remote Message VPN. By default, an interface is chosen automatically (recommended), but if specified, &#x60;remoteMsgVpnLocation&#x60; must not be a virtual router name. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnBridgeRemoteMsgVpnResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnBridgeRemoteMsgVpnResponse> getMsgVpnBridgeRemoteMsgVpnWithHttpInfo(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String remoteMsgVpnName, String remoteMsgVpnLocation, String remoteMsgVpnInterface, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnBridgeRemoteMsgVpnValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, remoteMsgVpnName, remoteMsgVpnLocation, remoteMsgVpnInterface, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeRemoteMsgVpnResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Remote Message VPN object. (asynchronously)
     * Get a Remote Message VPN object.  The Remote Message VPN is the Message VPN that the Bridge connects to.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| remoteMsgVpnInterface|x| remoteMsgVpnLocation|x| remoteMsgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param remoteMsgVpnName The name of the remote Message VPN. (required)
     * @param remoteMsgVpnLocation The location of the remote Message VPN as either an FQDN with port, IP address with port, or virtual router name (starting with \&quot;v:\&quot;). (required)
     * @param remoteMsgVpnInterface The physical interface on the local Message VPN host for connecting to the remote Message VPN. By default, an interface is chosen automatically (recommended), but if specified, &#x60;remoteMsgVpnLocation&#x60; must not be a virtual router name. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeRemoteMsgVpnAsync(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String remoteMsgVpnName, String remoteMsgVpnLocation, String remoteMsgVpnInterface, List<String> select, final ApiCallback<MsgVpnBridgeRemoteMsgVpnResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnBridgeRemoteMsgVpnValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, remoteMsgVpnName, remoteMsgVpnLocation, remoteMsgVpnInterface, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeRemoteMsgVpnResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnBridgeRemoteMsgVpns
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeRemoteMsgVpnsCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/bridges/{bridgeName},{bridgeVirtualRouter}/remoteMsgVpns"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "bridgeName" + "\\}", apiClient.escapeString(bridgeName.toString()))
            .replaceAll("\\{" + "bridgeVirtualRouter" + "\\}", apiClient.escapeString(bridgeVirtualRouter.toString()));

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

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnBridgeRemoteMsgVpnsValidateBeforeCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnBridgeRemoteMsgVpns(Async)");
        }
        
        // verify the required parameter 'bridgeName' is set
        if (bridgeName == null) {
            throw new ApiException("Missing the required parameter 'bridgeName' when calling getMsgVpnBridgeRemoteMsgVpns(Async)");
        }
        
        // verify the required parameter 'bridgeVirtualRouter' is set
        if (bridgeVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'bridgeVirtualRouter' when calling getMsgVpnBridgeRemoteMsgVpns(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnBridgeRemoteMsgVpnsCall(msgVpnName, bridgeName, bridgeVirtualRouter, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Remote Message VPN objects.
     * Get a list of Remote Message VPN objects.  The Remote Message VPN is the Message VPN that the Bridge connects to.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| remoteMsgVpnInterface|x| remoteMsgVpnLocation|x| remoteMsgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnBridgeRemoteMsgVpnsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnBridgeRemoteMsgVpnsResponse getMsgVpnBridgeRemoteMsgVpns(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnBridgeRemoteMsgVpnsResponse> resp = getMsgVpnBridgeRemoteMsgVpnsWithHttpInfo(msgVpnName, bridgeName, bridgeVirtualRouter, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Remote Message VPN objects.
     * Get a list of Remote Message VPN objects.  The Remote Message VPN is the Message VPN that the Bridge connects to.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| remoteMsgVpnInterface|x| remoteMsgVpnLocation|x| remoteMsgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnBridgeRemoteMsgVpnsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnBridgeRemoteMsgVpnsResponse> getMsgVpnBridgeRemoteMsgVpnsWithHttpInfo(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnBridgeRemoteMsgVpnsValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeRemoteMsgVpnsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Remote Message VPN objects. (asynchronously)
     * Get a list of Remote Message VPN objects.  The Remote Message VPN is the Message VPN that the Bridge connects to.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| remoteMsgVpnInterface|x| remoteMsgVpnLocation|x| remoteMsgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeRemoteMsgVpnsAsync(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> where, List<String> select, final ApiCallback<MsgVpnBridgeRemoteMsgVpnsResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnBridgeRemoteMsgVpnsValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeRemoteMsgVpnsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnBridgeRemoteSubscription
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param remoteSubscriptionTopic The topic of the Bridge remote subscription. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeRemoteSubscriptionCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String remoteSubscriptionTopic, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/bridges/{bridgeName},{bridgeVirtualRouter}/remoteSubscriptions/{remoteSubscriptionTopic}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "bridgeName" + "\\}", apiClient.escapeString(bridgeName.toString()))
            .replaceAll("\\{" + "bridgeVirtualRouter" + "\\}", apiClient.escapeString(bridgeVirtualRouter.toString()))
            .replaceAll("\\{" + "remoteSubscriptionTopic" + "\\}", apiClient.escapeString(remoteSubscriptionTopic.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnBridgeRemoteSubscriptionValidateBeforeCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String remoteSubscriptionTopic, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnBridgeRemoteSubscription(Async)");
        }
        
        // verify the required parameter 'bridgeName' is set
        if (bridgeName == null) {
            throw new ApiException("Missing the required parameter 'bridgeName' when calling getMsgVpnBridgeRemoteSubscription(Async)");
        }
        
        // verify the required parameter 'bridgeVirtualRouter' is set
        if (bridgeVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'bridgeVirtualRouter' when calling getMsgVpnBridgeRemoteSubscription(Async)");
        }
        
        // verify the required parameter 'remoteSubscriptionTopic' is set
        if (remoteSubscriptionTopic == null) {
            throw new ApiException("Missing the required parameter 'remoteSubscriptionTopic' when calling getMsgVpnBridgeRemoteSubscription(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnBridgeRemoteSubscriptionCall(msgVpnName, bridgeName, bridgeVirtualRouter, remoteSubscriptionTopic, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Remote Subscription object.
     * Get a Remote Subscription object.  A Remote Subscription is a topic subscription used by the Message VPN Bridge to attract messages from the remote message broker.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| remoteSubscriptionTopic|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param remoteSubscriptionTopic The topic of the Bridge remote subscription. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnBridgeRemoteSubscriptionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnBridgeRemoteSubscriptionResponse getMsgVpnBridgeRemoteSubscription(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String remoteSubscriptionTopic, List<String> select) throws ApiException {
        ApiResponse<MsgVpnBridgeRemoteSubscriptionResponse> resp = getMsgVpnBridgeRemoteSubscriptionWithHttpInfo(msgVpnName, bridgeName, bridgeVirtualRouter, remoteSubscriptionTopic, select);
        return resp.getData();
    }

    /**
     * Get a Remote Subscription object.
     * Get a Remote Subscription object.  A Remote Subscription is a topic subscription used by the Message VPN Bridge to attract messages from the remote message broker.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| remoteSubscriptionTopic|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param remoteSubscriptionTopic The topic of the Bridge remote subscription. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnBridgeRemoteSubscriptionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnBridgeRemoteSubscriptionResponse> getMsgVpnBridgeRemoteSubscriptionWithHttpInfo(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String remoteSubscriptionTopic, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnBridgeRemoteSubscriptionValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, remoteSubscriptionTopic, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeRemoteSubscriptionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Remote Subscription object. (asynchronously)
     * Get a Remote Subscription object.  A Remote Subscription is a topic subscription used by the Message VPN Bridge to attract messages from the remote message broker.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| remoteSubscriptionTopic|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param remoteSubscriptionTopic The topic of the Bridge remote subscription. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeRemoteSubscriptionAsync(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String remoteSubscriptionTopic, List<String> select, final ApiCallback<MsgVpnBridgeRemoteSubscriptionResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnBridgeRemoteSubscriptionValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, remoteSubscriptionTopic, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeRemoteSubscriptionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnBridgeRemoteSubscriptions
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeRemoteSubscriptionsCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/bridges/{bridgeName},{bridgeVirtualRouter}/remoteSubscriptions"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "bridgeName" + "\\}", apiClient.escapeString(bridgeName.toString()))
            .replaceAll("\\{" + "bridgeVirtualRouter" + "\\}", apiClient.escapeString(bridgeVirtualRouter.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnBridgeRemoteSubscriptionsValidateBeforeCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnBridgeRemoteSubscriptions(Async)");
        }
        
        // verify the required parameter 'bridgeName' is set
        if (bridgeName == null) {
            throw new ApiException("Missing the required parameter 'bridgeName' when calling getMsgVpnBridgeRemoteSubscriptions(Async)");
        }
        
        // verify the required parameter 'bridgeVirtualRouter' is set
        if (bridgeVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'bridgeVirtualRouter' when calling getMsgVpnBridgeRemoteSubscriptions(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnBridgeRemoteSubscriptionsCall(msgVpnName, bridgeName, bridgeVirtualRouter, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Remote Subscription objects.
     * Get a list of Remote Subscription objects.  A Remote Subscription is a topic subscription used by the Message VPN Bridge to attract messages from the remote message broker.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| remoteSubscriptionTopic|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnBridgeRemoteSubscriptionsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnBridgeRemoteSubscriptionsResponse getMsgVpnBridgeRemoteSubscriptions(String msgVpnName, String bridgeName, String bridgeVirtualRouter, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnBridgeRemoteSubscriptionsResponse> resp = getMsgVpnBridgeRemoteSubscriptionsWithHttpInfo(msgVpnName, bridgeName, bridgeVirtualRouter, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Remote Subscription objects.
     * Get a list of Remote Subscription objects.  A Remote Subscription is a topic subscription used by the Message VPN Bridge to attract messages from the remote message broker.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| remoteSubscriptionTopic|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnBridgeRemoteSubscriptionsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnBridgeRemoteSubscriptionsResponse> getMsgVpnBridgeRemoteSubscriptionsWithHttpInfo(String msgVpnName, String bridgeName, String bridgeVirtualRouter, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnBridgeRemoteSubscriptionsValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeRemoteSubscriptionsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Remote Subscription objects. (asynchronously)
     * Get a list of Remote Subscription objects.  A Remote Subscription is a topic subscription used by the Message VPN Bridge to attract messages from the remote message broker.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| remoteSubscriptionTopic|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeRemoteSubscriptionsAsync(String msgVpnName, String bridgeName, String bridgeVirtualRouter, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnBridgeRemoteSubscriptionsResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnBridgeRemoteSubscriptionsValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeRemoteSubscriptionsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnBridgeTlsTrustedCommonName
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeTlsTrustedCommonNameCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String tlsTrustedCommonName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/bridges/{bridgeName},{bridgeVirtualRouter}/tlsTrustedCommonNames/{tlsTrustedCommonName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "bridgeName" + "\\}", apiClient.escapeString(bridgeName.toString()))
            .replaceAll("\\{" + "bridgeVirtualRouter" + "\\}", apiClient.escapeString(bridgeVirtualRouter.toString()))
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

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnBridgeTlsTrustedCommonNameValidateBeforeCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String tlsTrustedCommonName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnBridgeTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'bridgeName' is set
        if (bridgeName == null) {
            throw new ApiException("Missing the required parameter 'bridgeName' when calling getMsgVpnBridgeTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'bridgeVirtualRouter' is set
        if (bridgeVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'bridgeVirtualRouter' when calling getMsgVpnBridgeTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'tlsTrustedCommonName' is set
        if (tlsTrustedCommonName == null) {
            throw new ApiException("Missing the required parameter 'tlsTrustedCommonName' when calling getMsgVpnBridgeTlsTrustedCommonName(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnBridgeTlsTrustedCommonNameCall(msgVpnName, bridgeName, bridgeVirtualRouter, tlsTrustedCommonName, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Trusted Common Name object.
     * Get a Trusted Common Name object.  The Trusted Common Names for the Bridge are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnBridgeTlsTrustedCommonNameResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnBridgeTlsTrustedCommonNameResponse getMsgVpnBridgeTlsTrustedCommonName(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String tlsTrustedCommonName, List<String> select) throws ApiException {
        ApiResponse<MsgVpnBridgeTlsTrustedCommonNameResponse> resp = getMsgVpnBridgeTlsTrustedCommonNameWithHttpInfo(msgVpnName, bridgeName, bridgeVirtualRouter, tlsTrustedCommonName, select);
        return resp.getData();
    }

    /**
     * Get a Trusted Common Name object.
     * Get a Trusted Common Name object.  The Trusted Common Names for the Bridge are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnBridgeTlsTrustedCommonNameResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnBridgeTlsTrustedCommonNameResponse> getMsgVpnBridgeTlsTrustedCommonNameWithHttpInfo(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String tlsTrustedCommonName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnBridgeTlsTrustedCommonNameValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, tlsTrustedCommonName, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeTlsTrustedCommonNameResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Trusted Common Name object. (asynchronously)
     * Get a Trusted Common Name object.  The Trusted Common Names for the Bridge are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeTlsTrustedCommonNameAsync(String msgVpnName, String bridgeName, String bridgeVirtualRouter, String tlsTrustedCommonName, List<String> select, final ApiCallback<MsgVpnBridgeTlsTrustedCommonNameResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnBridgeTlsTrustedCommonNameValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, tlsTrustedCommonName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeTlsTrustedCommonNameResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnBridgeTlsTrustedCommonNames
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeTlsTrustedCommonNamesCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/bridges/{bridgeName},{bridgeVirtualRouter}/tlsTrustedCommonNames"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "bridgeName" + "\\}", apiClient.escapeString(bridgeName.toString()))
            .replaceAll("\\{" + "bridgeVirtualRouter" + "\\}", apiClient.escapeString(bridgeVirtualRouter.toString()));

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

        String[] localVarAuthNames = new String[] { "basicAuth" };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getMsgVpnBridgeTlsTrustedCommonNamesValidateBeforeCall(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnBridgeTlsTrustedCommonNames(Async)");
        }
        
        // verify the required parameter 'bridgeName' is set
        if (bridgeName == null) {
            throw new ApiException("Missing the required parameter 'bridgeName' when calling getMsgVpnBridgeTlsTrustedCommonNames(Async)");
        }
        
        // verify the required parameter 'bridgeVirtualRouter' is set
        if (bridgeVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'bridgeVirtualRouter' when calling getMsgVpnBridgeTlsTrustedCommonNames(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnBridgeTlsTrustedCommonNamesCall(msgVpnName, bridgeName, bridgeVirtualRouter, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Trusted Common Name objects.
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the Bridge are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnBridgeTlsTrustedCommonNamesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnBridgeTlsTrustedCommonNamesResponse getMsgVpnBridgeTlsTrustedCommonNames(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnBridgeTlsTrustedCommonNamesResponse> resp = getMsgVpnBridgeTlsTrustedCommonNamesWithHttpInfo(msgVpnName, bridgeName, bridgeVirtualRouter, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Trusted Common Name objects.
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the Bridge are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnBridgeTlsTrustedCommonNamesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnBridgeTlsTrustedCommonNamesResponse> getMsgVpnBridgeTlsTrustedCommonNamesWithHttpInfo(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnBridgeTlsTrustedCommonNamesValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeTlsTrustedCommonNamesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Trusted Common Name objects. (asynchronously)
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the Bridge are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#39;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param bridgeName The name of the Bridge. (required)
     * @param bridgeVirtualRouter The virtual router of the Bridge. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgeTlsTrustedCommonNamesAsync(String msgVpnName, String bridgeName, String bridgeVirtualRouter, List<String> where, List<String> select, final ApiCallback<MsgVpnBridgeTlsTrustedCommonNamesResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnBridgeTlsTrustedCommonNamesValidateBeforeCall(msgVpnName, bridgeName, bridgeVirtualRouter, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnBridgeTlsTrustedCommonNamesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnBridges
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
    public com.squareup.okhttp.Call getMsgVpnBridgesCall(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/bridges"
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
    private com.squareup.okhttp.Call getMsgVpnBridgesValidateBeforeCall(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnBridges(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnBridgesCall(msgVpnName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Bridge objects.
     * Get a list of Bridge objects.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| counter.controlRxByteCount||x counter.controlRxMsgCount||x counter.controlTxByteCount||x counter.controlTxMsgCount||x counter.dataRxByteCount||x counter.dataRxMsgCount||x counter.dataTxByteCount||x counter.dataTxMsgCount||x counter.discardedRxMsgCount||x counter.discardedTxMsgCount||x counter.loginRxMsgCount||x counter.loginTxMsgCount||x counter.msgSpoolRxMsgCount||x counter.rxByteCount||x counter.rxMsgCount||x counter.txByteCount||x counter.txMsgCount||x msgVpnName|x| rate.averageRxByteRate||x rate.averageRxMsgRate||x rate.averageTxByteRate||x rate.averageTxMsgRate||x rate.rxByteRate||x rate.rxMsgRate||x rate.txByteRate||x rate.txMsgRate||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnBridgesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnBridgesResponse getMsgVpnBridges(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnBridgesResponse> resp = getMsgVpnBridgesWithHttpInfo(msgVpnName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Bridge objects.
     * Get a list of Bridge objects.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| counter.controlRxByteCount||x counter.controlRxMsgCount||x counter.controlTxByteCount||x counter.controlTxMsgCount||x counter.dataRxByteCount||x counter.dataRxMsgCount||x counter.dataTxByteCount||x counter.dataTxMsgCount||x counter.discardedRxMsgCount||x counter.discardedTxMsgCount||x counter.loginRxMsgCount||x counter.loginTxMsgCount||x counter.msgSpoolRxMsgCount||x counter.rxByteCount||x counter.rxMsgCount||x counter.txByteCount||x counter.txMsgCount||x msgVpnName|x| rate.averageRxByteRate||x rate.averageRxMsgRate||x rate.averageTxByteRate||x rate.averageTxMsgRate||x rate.rxByteRate||x rate.rxMsgRate||x rate.txByteRate||x rate.txMsgRate||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnBridgesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnBridgesResponse> getMsgVpnBridgesWithHttpInfo(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnBridgesValidateBeforeCall(msgVpnName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnBridgesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Bridge objects. (asynchronously)
     * Get a list of Bridge objects.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| counter.controlRxByteCount||x counter.controlRxMsgCount||x counter.controlTxByteCount||x counter.controlTxMsgCount||x counter.dataRxByteCount||x counter.dataRxMsgCount||x counter.dataTxByteCount||x counter.dataTxMsgCount||x counter.discardedRxMsgCount||x counter.discardedTxMsgCount||x counter.loginRxMsgCount||x counter.loginTxMsgCount||x counter.msgSpoolRxMsgCount||x counter.rxByteCount||x counter.rxMsgCount||x counter.txByteCount||x counter.txMsgCount||x msgVpnName|x| rate.averageRxByteRate||x rate.averageRxMsgRate||x rate.averageTxByteRate||x rate.averageTxMsgRate||x rate.rxByteRate||x rate.rxMsgRate||x rate.txByteRate||x rate.txMsgRate||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnBridgesAsync(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnBridgesResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnBridgesValidateBeforeCall(msgVpnName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnBridgesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
