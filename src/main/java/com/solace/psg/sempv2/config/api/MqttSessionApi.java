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
import com.solace.psg.sempv2.config.model.MsgVpnMqttSession;
import com.solace.psg.sempv2.config.model.MsgVpnMqttSessionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnMqttSessionSubscription;
import com.solace.psg.sempv2.config.model.MsgVpnMqttSessionSubscriptionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnMqttSessionSubscriptionsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnMqttSessionsResponse;
import com.solace.psg.sempv2.config.model.SempMetaOnlyResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MqttSessionApi {
    private ApiClient apiClient;

    public MqttSessionApi() {
        this(Configuration.getDefaultApiClient());
    }

    public MqttSessionApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for createMsgVpnMqttSession
     * @param msgVpnName The name of the Message VPN. (required)
     * @param body The MQTT Session object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnMqttSessionCall(String msgVpnName, MsgVpnMqttSession body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/mqttSessions"
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
    private com.squareup.okhttp.Call createMsgVpnMqttSessionValidateBeforeCall(String msgVpnName, MsgVpnMqttSession body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnMqttSession(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnMqttSession(Async)");
        }
        

        com.squareup.okhttp.Call call = createMsgVpnMqttSessionCall(msgVpnName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Create an MQTT Session object.
     * Create an MQTT Session object. Any attribute missing from the request will be set to its default value.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param body The MQTT Session object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnMqttSessionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnMqttSessionResponse createMsgVpnMqttSession(String msgVpnName, MsgVpnMqttSession body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnMqttSessionResponse> resp = createMsgVpnMqttSessionWithHttpInfo(msgVpnName, body, select);
        return resp.getData();
    }

    /**
     * Create an MQTT Session object.
     * Create an MQTT Session object. Any attribute missing from the request will be set to its default value.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param body The MQTT Session object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnMqttSessionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnMqttSessionResponse> createMsgVpnMqttSessionWithHttpInfo(String msgVpnName, MsgVpnMqttSession body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnMqttSessionValidateBeforeCall(msgVpnName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create an MQTT Session object. (asynchronously)
     * Create an MQTT Session object. Any attribute missing from the request will be set to its default value.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param body The MQTT Session object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnMqttSessionAsync(String msgVpnName, MsgVpnMqttSession body, List<String> select, final ApiCallback<MsgVpnMqttSessionResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnMqttSessionValidateBeforeCall(msgVpnName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for createMsgVpnMqttSessionSubscription
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param body The Subscription object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnMqttSessionSubscriptionCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSessionSubscription body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/mqttSessions/{mqttSessionClientId},{mqttSessionVirtualRouter}/subscriptions"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "mqttSessionClientId" + "\\}", apiClient.escapeString(mqttSessionClientId.toString()))
            .replaceAll("\\{" + "mqttSessionVirtualRouter" + "\\}", apiClient.escapeString(mqttSessionVirtualRouter.toString()));

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
    private com.squareup.okhttp.Call createMsgVpnMqttSessionSubscriptionValidateBeforeCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSessionSubscription body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'mqttSessionClientId' is set
        if (mqttSessionClientId == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionClientId' when calling createMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'mqttSessionVirtualRouter' is set
        if (mqttSessionVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionVirtualRouter' when calling createMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnMqttSessionSubscription(Async)");
        }
        

        com.squareup.okhttp.Call call = createMsgVpnMqttSessionSubscriptionCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Create a Subscription object.
     * Create a Subscription object. Any attribute missing from the request will be set to its default value.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x||x|| mqttSessionVirtualRouter|x||x|| msgVpnName|x||x|| subscriptionTopic|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param body The Subscription object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnMqttSessionSubscriptionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnMqttSessionSubscriptionResponse createMsgVpnMqttSessionSubscription(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSessionSubscription body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnMqttSessionSubscriptionResponse> resp = createMsgVpnMqttSessionSubscriptionWithHttpInfo(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, body, select);
        return resp.getData();
    }

    /**
     * Create a Subscription object.
     * Create a Subscription object. Any attribute missing from the request will be set to its default value.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x||x|| mqttSessionVirtualRouter|x||x|| msgVpnName|x||x|| subscriptionTopic|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param body The Subscription object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnMqttSessionSubscriptionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnMqttSessionSubscriptionResponse> createMsgVpnMqttSessionSubscriptionWithHttpInfo(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSessionSubscription body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnMqttSessionSubscriptionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionSubscriptionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Subscription object. (asynchronously)
     * Create a Subscription object. Any attribute missing from the request will be set to its default value.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x||x|| mqttSessionVirtualRouter|x||x|| msgVpnName|x||x|| subscriptionTopic|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param body The Subscription object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnMqttSessionSubscriptionAsync(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSessionSubscription body, List<String> select, final ApiCallback<MsgVpnMqttSessionSubscriptionResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnMqttSessionSubscriptionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionSubscriptionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnMqttSession
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnMqttSessionCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/mqttSessions/{mqttSessionClientId},{mqttSessionVirtualRouter}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "mqttSessionClientId" + "\\}", apiClient.escapeString(mqttSessionClientId.toString()))
            .replaceAll("\\{" + "mqttSessionVirtualRouter" + "\\}", apiClient.escapeString(mqttSessionVirtualRouter.toString()));

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
    private com.squareup.okhttp.Call deleteMsgVpnMqttSessionValidateBeforeCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnMqttSession(Async)");
        }
        
        // verify the required parameter 'mqttSessionClientId' is set
        if (mqttSessionClientId == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionClientId' when calling deleteMsgVpnMqttSession(Async)");
        }
        
        // verify the required parameter 'mqttSessionVirtualRouter' is set
        if (mqttSessionVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionVirtualRouter' when calling deleteMsgVpnMqttSession(Async)");
        }
        

        com.squareup.okhttp.Call call = deleteMsgVpnMqttSessionCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete an MQTT Session object.
     * Delete an MQTT Session object.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnMqttSession(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnMqttSessionWithHttpInfo(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter);
        return resp.getData();
    }

    /**
     * Delete an MQTT Session object.
     * Delete an MQTT Session object.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnMqttSessionWithHttpInfo(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnMqttSessionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete an MQTT Session object. (asynchronously)
     * Delete an MQTT Session object.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnMqttSessionAsync(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnMqttSessionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnMqttSessionSubscription
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnMqttSessionSubscriptionCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/mqttSessions/{mqttSessionClientId},{mqttSessionVirtualRouter}/subscriptions/{subscriptionTopic}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "mqttSessionClientId" + "\\}", apiClient.escapeString(mqttSessionClientId.toString()))
            .replaceAll("\\{" + "mqttSessionVirtualRouter" + "\\}", apiClient.escapeString(mqttSessionVirtualRouter.toString()))
            .replaceAll("\\{" + "subscriptionTopic" + "\\}", apiClient.escapeString(subscriptionTopic.toString()));

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
    private com.squareup.okhttp.Call deleteMsgVpnMqttSessionSubscriptionValidateBeforeCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'mqttSessionClientId' is set
        if (mqttSessionClientId == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionClientId' when calling deleteMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'mqttSessionVirtualRouter' is set
        if (mqttSessionVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionVirtualRouter' when calling deleteMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'subscriptionTopic' is set
        if (subscriptionTopic == null) {
            throw new ApiException("Missing the required parameter 'subscriptionTopic' when calling deleteMsgVpnMqttSessionSubscription(Async)");
        }
        

        com.squareup.okhttp.Call call = deleteMsgVpnMqttSessionSubscriptionCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete a Subscription object.
     * Delete a Subscription object.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnMqttSessionSubscription(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnMqttSessionSubscriptionWithHttpInfo(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic);
        return resp.getData();
    }

    /**
     * Delete a Subscription object.
     * Delete a Subscription object.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnMqttSessionSubscriptionWithHttpInfo(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnMqttSessionSubscriptionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Subscription object. (asynchronously)
     * Delete a Subscription object.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnMqttSessionSubscriptionAsync(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnMqttSessionSubscriptionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnMqttSession
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnMqttSessionCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/mqttSessions/{mqttSessionClientId},{mqttSessionVirtualRouter}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "mqttSessionClientId" + "\\}", apiClient.escapeString(mqttSessionClientId.toString()))
            .replaceAll("\\{" + "mqttSessionVirtualRouter" + "\\}", apiClient.escapeString(mqttSessionVirtualRouter.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnMqttSessionValidateBeforeCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnMqttSession(Async)");
        }
        
        // verify the required parameter 'mqttSessionClientId' is set
        if (mqttSessionClientId == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionClientId' when calling getMsgVpnMqttSession(Async)");
        }
        
        // verify the required parameter 'mqttSessionVirtualRouter' is set
        if (mqttSessionVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionVirtualRouter' when calling getMsgVpnMqttSession(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnMqttSessionCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get an MQTT Session object.
     * Get an MQTT Session object.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: mqttSessionClientId|x|| mqttSessionVirtualRouter|x|| msgVpnName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnMqttSessionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnMqttSessionResponse getMsgVpnMqttSession(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, List<String> select) throws ApiException {
        ApiResponse<MsgVpnMqttSessionResponse> resp = getMsgVpnMqttSessionWithHttpInfo(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, select);
        return resp.getData();
    }

    /**
     * Get an MQTT Session object.
     * Get an MQTT Session object.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: mqttSessionClientId|x|| mqttSessionVirtualRouter|x|| msgVpnName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnMqttSessionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnMqttSessionResponse> getMsgVpnMqttSessionWithHttpInfo(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnMqttSessionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get an MQTT Session object. (asynchronously)
     * Get an MQTT Session object.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: mqttSessionClientId|x|| mqttSessionVirtualRouter|x|| msgVpnName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnMqttSessionAsync(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, List<String> select, final ApiCallback<MsgVpnMqttSessionResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnMqttSessionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnMqttSessionSubscription
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnMqttSessionSubscriptionCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/mqttSessions/{mqttSessionClientId},{mqttSessionVirtualRouter}/subscriptions/{subscriptionTopic}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "mqttSessionClientId" + "\\}", apiClient.escapeString(mqttSessionClientId.toString()))
            .replaceAll("\\{" + "mqttSessionVirtualRouter" + "\\}", apiClient.escapeString(mqttSessionVirtualRouter.toString()))
            .replaceAll("\\{" + "subscriptionTopic" + "\\}", apiClient.escapeString(subscriptionTopic.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnMqttSessionSubscriptionValidateBeforeCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'mqttSessionClientId' is set
        if (mqttSessionClientId == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionClientId' when calling getMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'mqttSessionVirtualRouter' is set
        if (mqttSessionVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionVirtualRouter' when calling getMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'subscriptionTopic' is set
        if (subscriptionTopic == null) {
            throw new ApiException("Missing the required parameter 'subscriptionTopic' when calling getMsgVpnMqttSessionSubscription(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnMqttSessionSubscriptionCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Subscription object.
     * Get a Subscription object.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: mqttSessionClientId|x|| mqttSessionVirtualRouter|x|| msgVpnName|x|| subscriptionTopic|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnMqttSessionSubscriptionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnMqttSessionSubscriptionResponse getMsgVpnMqttSessionSubscription(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, List<String> select) throws ApiException {
        ApiResponse<MsgVpnMqttSessionSubscriptionResponse> resp = getMsgVpnMqttSessionSubscriptionWithHttpInfo(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, select);
        return resp.getData();
    }

    /**
     * Get a Subscription object.
     * Get a Subscription object.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: mqttSessionClientId|x|| mqttSessionVirtualRouter|x|| msgVpnName|x|| subscriptionTopic|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnMqttSessionSubscriptionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnMqttSessionSubscriptionResponse> getMsgVpnMqttSessionSubscriptionWithHttpInfo(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnMqttSessionSubscriptionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionSubscriptionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Subscription object. (asynchronously)
     * Get a Subscription object.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: mqttSessionClientId|x|| mqttSessionVirtualRouter|x|| msgVpnName|x|| subscriptionTopic|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnMqttSessionSubscriptionAsync(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, List<String> select, final ApiCallback<MsgVpnMqttSessionSubscriptionResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnMqttSessionSubscriptionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionSubscriptionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnMqttSessionSubscriptions
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnMqttSessionSubscriptionsCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/mqttSessions/{mqttSessionClientId},{mqttSessionVirtualRouter}/subscriptions"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "mqttSessionClientId" + "\\}", apiClient.escapeString(mqttSessionClientId.toString()))
            .replaceAll("\\{" + "mqttSessionVirtualRouter" + "\\}", apiClient.escapeString(mqttSessionVirtualRouter.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnMqttSessionSubscriptionsValidateBeforeCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnMqttSessionSubscriptions(Async)");
        }
        
        // verify the required parameter 'mqttSessionClientId' is set
        if (mqttSessionClientId == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionClientId' when calling getMsgVpnMqttSessionSubscriptions(Async)");
        }
        
        // verify the required parameter 'mqttSessionVirtualRouter' is set
        if (mqttSessionVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionVirtualRouter' when calling getMsgVpnMqttSessionSubscriptions(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnMqttSessionSubscriptionsCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Subscription objects.
     * Get a list of Subscription objects.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: mqttSessionClientId|x|| mqttSessionVirtualRouter|x|| msgVpnName|x|| subscriptionTopic|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnMqttSessionSubscriptionsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnMqttSessionSubscriptionsResponse getMsgVpnMqttSessionSubscriptions(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnMqttSessionSubscriptionsResponse> resp = getMsgVpnMqttSessionSubscriptionsWithHttpInfo(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Subscription objects.
     * Get a list of Subscription objects.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: mqttSessionClientId|x|| mqttSessionVirtualRouter|x|| msgVpnName|x|| subscriptionTopic|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnMqttSessionSubscriptionsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnMqttSessionSubscriptionsResponse> getMsgVpnMqttSessionSubscriptionsWithHttpInfo(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnMqttSessionSubscriptionsValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionSubscriptionsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Subscription objects. (asynchronously)
     * Get a list of Subscription objects.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: mqttSessionClientId|x|| mqttSessionVirtualRouter|x|| msgVpnName|x|| subscriptionTopic|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnMqttSessionSubscriptionsAsync(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnMqttSessionSubscriptionsResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnMqttSessionSubscriptionsValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionSubscriptionsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnMqttSessions
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
    public com.squareup.okhttp.Call getMsgVpnMqttSessionsCall(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/mqttSessions"
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
    private com.squareup.okhttp.Call getMsgVpnMqttSessionsValidateBeforeCall(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnMqttSessions(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnMqttSessionsCall(msgVpnName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of MQTT Session objects.
     * Get a list of MQTT Session objects.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: mqttSessionClientId|x|| mqttSessionVirtualRouter|x|| msgVpnName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnMqttSessionsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnMqttSessionsResponse getMsgVpnMqttSessions(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnMqttSessionsResponse> resp = getMsgVpnMqttSessionsWithHttpInfo(msgVpnName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of MQTT Session objects.
     * Get a list of MQTT Session objects.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: mqttSessionClientId|x|| mqttSessionVirtualRouter|x|| msgVpnName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnMqttSessionsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnMqttSessionsResponse> getMsgVpnMqttSessionsWithHttpInfo(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnMqttSessionsValidateBeforeCall(msgVpnName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of MQTT Session objects. (asynchronously)
     * Get a list of MQTT Session objects.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: mqttSessionClientId|x|| mqttSessionVirtualRouter|x|| msgVpnName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnMqttSessionsAsync(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnMqttSessionsResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnMqttSessionsValidateBeforeCall(msgVpnName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for replaceMsgVpnMqttSession
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param body The MQTT Session object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnMqttSessionCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSession body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/mqttSessions/{mqttSessionClientId},{mqttSessionVirtualRouter}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "mqttSessionClientId" + "\\}", apiClient.escapeString(mqttSessionClientId.toString()))
            .replaceAll("\\{" + "mqttSessionVirtualRouter" + "\\}", apiClient.escapeString(mqttSessionVirtualRouter.toString()));

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
    private com.squareup.okhttp.Call replaceMsgVpnMqttSessionValidateBeforeCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSession body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling replaceMsgVpnMqttSession(Async)");
        }
        
        // verify the required parameter 'mqttSessionClientId' is set
        if (mqttSessionClientId == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionClientId' when calling replaceMsgVpnMqttSession(Async)");
        }
        
        // verify the required parameter 'mqttSessionVirtualRouter' is set
        if (mqttSessionVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionVirtualRouter' when calling replaceMsgVpnMqttSession(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling replaceMsgVpnMqttSession(Async)");
        }
        

        com.squareup.okhttp.Call call = replaceMsgVpnMqttSessionCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Replace an MQTT Session object.
     * Replace an MQTT Session object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x|x||| owner||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param body The MQTT Session object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnMqttSessionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnMqttSessionResponse replaceMsgVpnMqttSession(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSession body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnMqttSessionResponse> resp = replaceMsgVpnMqttSessionWithHttpInfo(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, body, select);
        return resp.getData();
    }

    /**
     * Replace an MQTT Session object.
     * Replace an MQTT Session object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x|x||| owner||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param body The MQTT Session object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnMqttSessionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnMqttSessionResponse> replaceMsgVpnMqttSessionWithHttpInfo(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSession body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = replaceMsgVpnMqttSessionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Replace an MQTT Session object. (asynchronously)
     * Replace an MQTT Session object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x|x||| owner||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param body The MQTT Session object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnMqttSessionAsync(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSession body, List<String> select, final ApiCallback<MsgVpnMqttSessionResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = replaceMsgVpnMqttSessionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for replaceMsgVpnMqttSessionSubscription
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param body The Subscription object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnMqttSessionSubscriptionCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, MsgVpnMqttSessionSubscription body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/mqttSessions/{mqttSessionClientId},{mqttSessionVirtualRouter}/subscriptions/{subscriptionTopic}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "mqttSessionClientId" + "\\}", apiClient.escapeString(mqttSessionClientId.toString()))
            .replaceAll("\\{" + "mqttSessionVirtualRouter" + "\\}", apiClient.escapeString(mqttSessionVirtualRouter.toString()))
            .replaceAll("\\{" + "subscriptionTopic" + "\\}", apiClient.escapeString(subscriptionTopic.toString()));

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
    private com.squareup.okhttp.Call replaceMsgVpnMqttSessionSubscriptionValidateBeforeCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, MsgVpnMqttSessionSubscription body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling replaceMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'mqttSessionClientId' is set
        if (mqttSessionClientId == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionClientId' when calling replaceMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'mqttSessionVirtualRouter' is set
        if (mqttSessionVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionVirtualRouter' when calling replaceMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'subscriptionTopic' is set
        if (subscriptionTopic == null) {
            throw new ApiException("Missing the required parameter 'subscriptionTopic' when calling replaceMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling replaceMsgVpnMqttSessionSubscription(Async)");
        }
        

        com.squareup.okhttp.Call call = replaceMsgVpnMqttSessionSubscriptionCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Replace a Subscription object.
     * Replace a Subscription object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x|x||| subscriptionTopic|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param body The Subscription object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnMqttSessionSubscriptionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnMqttSessionSubscriptionResponse replaceMsgVpnMqttSessionSubscription(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, MsgVpnMqttSessionSubscription body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnMqttSessionSubscriptionResponse> resp = replaceMsgVpnMqttSessionSubscriptionWithHttpInfo(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, body, select);
        return resp.getData();
    }

    /**
     * Replace a Subscription object.
     * Replace a Subscription object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x|x||| subscriptionTopic|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param body The Subscription object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnMqttSessionSubscriptionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnMqttSessionSubscriptionResponse> replaceMsgVpnMqttSessionSubscriptionWithHttpInfo(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, MsgVpnMqttSessionSubscription body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = replaceMsgVpnMqttSessionSubscriptionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionSubscriptionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Replace a Subscription object. (asynchronously)
     * Replace a Subscription object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x|x||| subscriptionTopic|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param body The Subscription object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnMqttSessionSubscriptionAsync(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, MsgVpnMqttSessionSubscription body, List<String> select, final ApiCallback<MsgVpnMqttSessionSubscriptionResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = replaceMsgVpnMqttSessionSubscriptionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionSubscriptionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for updateMsgVpnMqttSession
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param body The MQTT Session object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnMqttSessionCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSession body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/mqttSessions/{mqttSessionClientId},{mqttSessionVirtualRouter}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "mqttSessionClientId" + "\\}", apiClient.escapeString(mqttSessionClientId.toString()))
            .replaceAll("\\{" + "mqttSessionVirtualRouter" + "\\}", apiClient.escapeString(mqttSessionVirtualRouter.toString()));

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
    private com.squareup.okhttp.Call updateMsgVpnMqttSessionValidateBeforeCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSession body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling updateMsgVpnMqttSession(Async)");
        }
        
        // verify the required parameter 'mqttSessionClientId' is set
        if (mqttSessionClientId == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionClientId' when calling updateMsgVpnMqttSession(Async)");
        }
        
        // verify the required parameter 'mqttSessionVirtualRouter' is set
        if (mqttSessionVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionVirtualRouter' when calling updateMsgVpnMqttSession(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling updateMsgVpnMqttSession(Async)");
        }
        

        com.squareup.okhttp.Call call = updateMsgVpnMqttSessionCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Update an MQTT Session object.
     * Update an MQTT Session object. Any attribute missing from the request will be left unchanged.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x|x||| owner||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param body The MQTT Session object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnMqttSessionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnMqttSessionResponse updateMsgVpnMqttSession(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSession body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnMqttSessionResponse> resp = updateMsgVpnMqttSessionWithHttpInfo(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, body, select);
        return resp.getData();
    }

    /**
     * Update an MQTT Session object.
     * Update an MQTT Session object. Any attribute missing from the request will be left unchanged.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x|x||| owner||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param body The MQTT Session object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnMqttSessionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnMqttSessionResponse> updateMsgVpnMqttSessionWithHttpInfo(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSession body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = updateMsgVpnMqttSessionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Update an MQTT Session object. (asynchronously)
     * Update an MQTT Session object. Any attribute missing from the request will be left unchanged.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#39;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x|x||| owner||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param body The MQTT Session object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnMqttSessionAsync(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, MsgVpnMqttSession body, List<String> select, final ApiCallback<MsgVpnMqttSessionResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = updateMsgVpnMqttSessionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for updateMsgVpnMqttSessionSubscription
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param body The Subscription object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnMqttSessionSubscriptionCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, MsgVpnMqttSessionSubscription body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/mqttSessions/{mqttSessionClientId},{mqttSessionVirtualRouter}/subscriptions/{subscriptionTopic}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "mqttSessionClientId" + "\\}", apiClient.escapeString(mqttSessionClientId.toString()))
            .replaceAll("\\{" + "mqttSessionVirtualRouter" + "\\}", apiClient.escapeString(mqttSessionVirtualRouter.toString()))
            .replaceAll("\\{" + "subscriptionTopic" + "\\}", apiClient.escapeString(subscriptionTopic.toString()));

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
    private com.squareup.okhttp.Call updateMsgVpnMqttSessionSubscriptionValidateBeforeCall(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, MsgVpnMqttSessionSubscription body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling updateMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'mqttSessionClientId' is set
        if (mqttSessionClientId == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionClientId' when calling updateMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'mqttSessionVirtualRouter' is set
        if (mqttSessionVirtualRouter == null) {
            throw new ApiException("Missing the required parameter 'mqttSessionVirtualRouter' when calling updateMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'subscriptionTopic' is set
        if (subscriptionTopic == null) {
            throw new ApiException("Missing the required parameter 'subscriptionTopic' when calling updateMsgVpnMqttSessionSubscription(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling updateMsgVpnMqttSessionSubscription(Async)");
        }
        

        com.squareup.okhttp.Call call = updateMsgVpnMqttSessionSubscriptionCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Update a Subscription object.
     * Update a Subscription object. Any attribute missing from the request will be left unchanged.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x|x||| subscriptionTopic|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param body The Subscription object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnMqttSessionSubscriptionResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnMqttSessionSubscriptionResponse updateMsgVpnMqttSessionSubscription(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, MsgVpnMqttSessionSubscription body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnMqttSessionSubscriptionResponse> resp = updateMsgVpnMqttSessionSubscriptionWithHttpInfo(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, body, select);
        return resp.getData();
    }

    /**
     * Update a Subscription object.
     * Update a Subscription object. Any attribute missing from the request will be left unchanged.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x|x||| subscriptionTopic|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param body The Subscription object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnMqttSessionSubscriptionResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnMqttSessionSubscriptionResponse> updateMsgVpnMqttSessionSubscriptionWithHttpInfo(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, MsgVpnMqttSessionSubscription body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = updateMsgVpnMqttSessionSubscriptionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionSubscriptionResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Update a Subscription object. (asynchronously)
     * Update a Subscription object. Any attribute missing from the request will be left unchanged.  An MQTT session contains a client&#39;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x||| mqttSessionVirtualRouter|x|x||| msgVpnName|x|x||| subscriptionTopic|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.8.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param mqttSessionClientId The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet. (required)
     * @param mqttSessionVirtualRouter The virtual router of the MQTT Session. (required)
     * @param subscriptionTopic The MQTT subscription topic. (required)
     * @param body The Subscription object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnMqttSessionSubscriptionAsync(String msgVpnName, String mqttSessionClientId, String mqttSessionVirtualRouter, String subscriptionTopic, MsgVpnMqttSessionSubscription body, List<String> select, final ApiCallback<MsgVpnMqttSessionSubscriptionResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = updateMsgVpnMqttSessionSubscriptionValidateBeforeCall(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnMqttSessionSubscriptionResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
