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
import com.solace.psg.sempv2.config.model.MsgVpnRestDeliveryPoint;
import com.solace.psg.sempv2.config.model.MsgVpnRestDeliveryPointQueueBinding;
import com.solace.psg.sempv2.config.model.MsgVpnRestDeliveryPointQueueBindingResponse;
import com.solace.psg.sempv2.config.model.MsgVpnRestDeliveryPointQueueBindingsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnRestDeliveryPointResponse;
import com.solace.psg.sempv2.config.model.MsgVpnRestDeliveryPointRestConsumer;
import com.solace.psg.sempv2.config.model.MsgVpnRestDeliveryPointRestConsumerResponse;
import com.solace.psg.sempv2.config.model.MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName;
import com.solace.psg.sempv2.config.model.MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse;
import com.solace.psg.sempv2.config.model.MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnRestDeliveryPointRestConsumersResponse;
import com.solace.psg.sempv2.config.model.MsgVpnRestDeliveryPointsResponse;
import com.solace.psg.sempv2.config.model.SempMetaOnlyResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestDeliveryPointApi {
    private ApiClient apiClient;

    public RestDeliveryPointApi() {
        this(Configuration.getDefaultApiClient());
    }

    public RestDeliveryPointApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for createMsgVpnRestDeliveryPoint
     * @param msgVpnName The name of the Message VPN. (required)
     * @param body The REST Delivery Point object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnRestDeliveryPointCall(String msgVpnName, MsgVpnRestDeliveryPoint body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints"
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
    private com.squareup.okhttp.Call createMsgVpnRestDeliveryPointValidateBeforeCall(String msgVpnName, MsgVpnRestDeliveryPoint body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnRestDeliveryPoint(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnRestDeliveryPoint(Async)");
        }
        

        com.squareup.okhttp.Call call = createMsgVpnRestDeliveryPointCall(msgVpnName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Create a REST Delivery Point object.
     * Create a REST Delivery Point object. Any attribute missing from the request will be set to its default value.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x|| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param body The REST Delivery Point object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointResponse createMsgVpnRestDeliveryPoint(String msgVpnName, MsgVpnRestDeliveryPoint body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointResponse> resp = createMsgVpnRestDeliveryPointWithHttpInfo(msgVpnName, body, select);
        return resp.getData();
    }

    /**
     * Create a REST Delivery Point object.
     * Create a REST Delivery Point object. Any attribute missing from the request will be set to its default value.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x|| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param body The REST Delivery Point object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointResponse> createMsgVpnRestDeliveryPointWithHttpInfo(String msgVpnName, MsgVpnRestDeliveryPoint body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnRestDeliveryPointValidateBeforeCall(msgVpnName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a REST Delivery Point object. (asynchronously)
     * Create a REST Delivery Point object. Any attribute missing from the request will be set to its default value.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x|| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param body The REST Delivery Point object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnRestDeliveryPointAsync(String msgVpnName, MsgVpnRestDeliveryPoint body, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnRestDeliveryPointValidateBeforeCall(msgVpnName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for createMsgVpnRestDeliveryPointQueueBinding
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The Queue Binding object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnRestDeliveryPointQueueBindingCall(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/queueBindings"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()));

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
    private com.squareup.okhttp.Call createMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling createMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        

        com.squareup.okhttp.Call call = createMsgVpnRestDeliveryPointQueueBindingCall(msgVpnName, restDeliveryPointName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Create a Queue Binding object.
     * Create a Queue Binding object. Any attribute missing from the request will be set to its default value.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x|| queueBindingName|x|x||| restDeliveryPointName|x||x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The Queue Binding object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointQueueBindingResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointQueueBindingResponse createMsgVpnRestDeliveryPointQueueBinding(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointQueueBindingResponse> resp = createMsgVpnRestDeliveryPointQueueBindingWithHttpInfo(msgVpnName, restDeliveryPointName, body, select);
        return resp.getData();
    }

    /**
     * Create a Queue Binding object.
     * Create a Queue Binding object. Any attribute missing from the request will be set to its default value.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x|| queueBindingName|x|x||| restDeliveryPointName|x||x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The Queue Binding object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointQueueBindingResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointQueueBindingResponse> createMsgVpnRestDeliveryPointQueueBindingWithHttpInfo(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(msgVpnName, restDeliveryPointName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointQueueBindingResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Queue Binding object. (asynchronously)
     * Create a Queue Binding object. Any attribute missing from the request will be set to its default value.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x|| queueBindingName|x|x||| restDeliveryPointName|x||x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The Queue Binding object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnRestDeliveryPointQueueBindingAsync(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointQueueBindingResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(msgVpnName, restDeliveryPointName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointQueueBindingResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for createMsgVpnRestDeliveryPointRestConsumer
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The REST Consumer object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnRestDeliveryPointRestConsumerCall(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/restConsumers"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()));

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
    private com.squareup.okhttp.Call createMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling createMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        

        com.squareup.okhttp.Call call = createMsgVpnRestDeliveryPointRestConsumerCall(msgVpnName, restDeliveryPointName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Create a REST Consumer object.
     * Create a REST Consumer object. Any attribute missing from the request will be set to its default value.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationClientCertContent||||x| authenticationClientCertPassword||||x| authenticationHttpBasicPassword||||x| msgVpnName|x||x|| restConsumerName|x|x||| restDeliveryPointName|x||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnRestDeliveryPointRestConsumer|authenticationClientCertPassword|authenticationClientCertContent| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicPassword|authenticationHttpBasicUsername| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicUsername|authenticationHttpBasicPassword| MsgVpnRestDeliveryPointRestConsumer|remotePort|tlsEnabled| MsgVpnRestDeliveryPointRestConsumer|tlsEnabled|remotePort|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The REST Consumer object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointRestConsumerResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointRestConsumerResponse createMsgVpnRestDeliveryPointRestConsumer(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointRestConsumerResponse> resp = createMsgVpnRestDeliveryPointRestConsumerWithHttpInfo(msgVpnName, restDeliveryPointName, body, select);
        return resp.getData();
    }

    /**
     * Create a REST Consumer object.
     * Create a REST Consumer object. Any attribute missing from the request will be set to its default value.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationClientCertContent||||x| authenticationClientCertPassword||||x| authenticationHttpBasicPassword||||x| msgVpnName|x||x|| restConsumerName|x|x||| restDeliveryPointName|x||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnRestDeliveryPointRestConsumer|authenticationClientCertPassword|authenticationClientCertContent| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicPassword|authenticationHttpBasicUsername| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicUsername|authenticationHttpBasicPassword| MsgVpnRestDeliveryPointRestConsumer|remotePort|tlsEnabled| MsgVpnRestDeliveryPointRestConsumer|tlsEnabled|remotePort|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The REST Consumer object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointRestConsumerResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointRestConsumerResponse> createMsgVpnRestDeliveryPointRestConsumerWithHttpInfo(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(msgVpnName, restDeliveryPointName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a REST Consumer object. (asynchronously)
     * Create a REST Consumer object. Any attribute missing from the request will be set to its default value.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationClientCertContent||||x| authenticationClientCertPassword||||x| authenticationHttpBasicPassword||||x| msgVpnName|x||x|| restConsumerName|x|x||| restDeliveryPointName|x||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnRestDeliveryPointRestConsumer|authenticationClientCertPassword|authenticationClientCertContent| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicPassword|authenticationHttpBasicUsername| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicUsername|authenticationHttpBasicPassword| MsgVpnRestDeliveryPointRestConsumer|remotePort|tlsEnabled| MsgVpnRestDeliveryPointRestConsumer|tlsEnabled|remotePort|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The REST Consumer object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnRestDeliveryPointRestConsumerAsync(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointRestConsumerResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(msgVpnName, restDeliveryPointName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param body The Trusted Common Name object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/restConsumers/{restConsumerName}/tlsTrustedCommonNames"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()))
            .replaceAll("\\{" + "restConsumerName" + "\\}", apiClient.escapeString(restConsumerName.toString()));

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
    private com.squareup.okhttp.Call createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameValidateBeforeCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'restConsumerName' is set
        if (restConsumerName == null) {
            throw new ApiException("Missing the required parameter 'restConsumerName' when calling createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(Async)");
        }
        

        com.squareup.okhttp.Call call = createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameCall(msgVpnName, restDeliveryPointName, restConsumerName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Create a Trusted Common Name object.
     * Create a Trusted Common Name object. Any attribute missing from the request will be set to its default value.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x|| restConsumerName|x||x|| restDeliveryPointName|x||x|| tlsTrustedCommonName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param body The Trusted Common Name object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse> resp = createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameWithHttpInfo(msgVpnName, restDeliveryPointName, restConsumerName, body, select);
        return resp.getData();
    }

    /**
     * Create a Trusted Common Name object.
     * Create a Trusted Common Name object. Any attribute missing from the request will be set to its default value.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x|| restConsumerName|x||x|| restDeliveryPointName|x||x|| tlsTrustedCommonName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param body The Trusted Common Name object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse> createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameWithHttpInfo(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Create a Trusted Common Name object. (asynchronously)
     * Create a Trusted Common Name object. Any attribute missing from the request will be set to its default value.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x|| restConsumerName|x||x|| restDeliveryPointName|x||x|| tlsTrustedCommonName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param body The Trusted Common Name object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameAsync(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName body, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnRestDeliveryPoint
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnRestDeliveryPointCall(String msgVpnName, String restDeliveryPointName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()));

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
    private com.squareup.okhttp.Call deleteMsgVpnRestDeliveryPointValidateBeforeCall(String msgVpnName, String restDeliveryPointName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnRestDeliveryPoint(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling deleteMsgVpnRestDeliveryPoint(Async)");
        }
        

        com.squareup.okhttp.Call call = deleteMsgVpnRestDeliveryPointCall(msgVpnName, restDeliveryPointName, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete a REST Delivery Point object.
     * Delete a REST Delivery Point object.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnRestDeliveryPoint(String msgVpnName, String restDeliveryPointName) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnRestDeliveryPointWithHttpInfo(msgVpnName, restDeliveryPointName);
        return resp.getData();
    }

    /**
     * Delete a REST Delivery Point object.
     * Delete a REST Delivery Point object.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnRestDeliveryPointWithHttpInfo(String msgVpnName, String restDeliveryPointName) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnRestDeliveryPointValidateBeforeCall(msgVpnName, restDeliveryPointName, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a REST Delivery Point object. (asynchronously)
     * Delete a REST Delivery Point object.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnRestDeliveryPointAsync(String msgVpnName, String restDeliveryPointName, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnRestDeliveryPointValidateBeforeCall(msgVpnName, restDeliveryPointName, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnRestDeliveryPointQueueBinding
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnRestDeliveryPointQueueBindingCall(String msgVpnName, String restDeliveryPointName, String queueBindingName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/queueBindings/{queueBindingName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()))
            .replaceAll("\\{" + "queueBindingName" + "\\}", apiClient.escapeString(queueBindingName.toString()));

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
    private com.squareup.okhttp.Call deleteMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(String msgVpnName, String restDeliveryPointName, String queueBindingName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling deleteMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        
        // verify the required parameter 'queueBindingName' is set
        if (queueBindingName == null) {
            throw new ApiException("Missing the required parameter 'queueBindingName' when calling deleteMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        

        com.squareup.okhttp.Call call = deleteMsgVpnRestDeliveryPointQueueBindingCall(msgVpnName, restDeliveryPointName, queueBindingName, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete a Queue Binding object.
     * Delete a Queue Binding object.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnRestDeliveryPointQueueBinding(String msgVpnName, String restDeliveryPointName, String queueBindingName) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnRestDeliveryPointQueueBindingWithHttpInfo(msgVpnName, restDeliveryPointName, queueBindingName);
        return resp.getData();
    }

    /**
     * Delete a Queue Binding object.
     * Delete a Queue Binding object.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnRestDeliveryPointQueueBindingWithHttpInfo(String msgVpnName, String restDeliveryPointName, String queueBindingName) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(msgVpnName, restDeliveryPointName, queueBindingName, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Queue Binding object. (asynchronously)
     * Delete a Queue Binding object.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnRestDeliveryPointQueueBindingAsync(String msgVpnName, String restDeliveryPointName, String queueBindingName, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(msgVpnName, restDeliveryPointName, queueBindingName, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnRestDeliveryPointRestConsumer
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnRestDeliveryPointRestConsumerCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/restConsumers/{restConsumerName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()))
            .replaceAll("\\{" + "restConsumerName" + "\\}", apiClient.escapeString(restConsumerName.toString()));

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
    private com.squareup.okhttp.Call deleteMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling deleteMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        
        // verify the required parameter 'restConsumerName' is set
        if (restConsumerName == null) {
            throw new ApiException("Missing the required parameter 'restConsumerName' when calling deleteMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        

        com.squareup.okhttp.Call call = deleteMsgVpnRestDeliveryPointRestConsumerCall(msgVpnName, restDeliveryPointName, restConsumerName, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete a REST Consumer object.
     * Delete a REST Consumer object.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnRestDeliveryPointRestConsumer(String msgVpnName, String restDeliveryPointName, String restConsumerName) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnRestDeliveryPointRestConsumerWithHttpInfo(msgVpnName, restDeliveryPointName, restConsumerName);
        return resp.getData();
    }

    /**
     * Delete a REST Consumer object.
     * Delete a REST Consumer object.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnRestDeliveryPointRestConsumerWithHttpInfo(String msgVpnName, String restDeliveryPointName, String restConsumerName) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a REST Consumer object. (asynchronously)
     * Delete a REST Consumer object.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnRestDeliveryPointRestConsumerAsync(String msgVpnName, String restDeliveryPointName, String restConsumerName, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, String tlsTrustedCommonName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/restConsumers/{restConsumerName}/tlsTrustedCommonNames/{tlsTrustedCommonName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()))
            .replaceAll("\\{" + "restConsumerName" + "\\}", apiClient.escapeString(restConsumerName.toString()))
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
    private com.squareup.okhttp.Call deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameValidateBeforeCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, String tlsTrustedCommonName, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'restConsumerName' is set
        if (restConsumerName == null) {
            throw new ApiException("Missing the required parameter 'restConsumerName' when calling deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'tlsTrustedCommonName' is set
        if (tlsTrustedCommonName == null) {
            throw new ApiException("Missing the required parameter 'tlsTrustedCommonName' when calling deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(Async)");
        }
        

        com.squareup.okhttp.Call call = deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameCall(msgVpnName, restDeliveryPointName, restConsumerName, tlsTrustedCommonName, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Delete a Trusted Common Name object.
     * Delete a Trusted Common Name object.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @return SempMetaOnlyResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SempMetaOnlyResponse deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(String msgVpnName, String restDeliveryPointName, String restConsumerName, String tlsTrustedCommonName) throws ApiException {
        ApiResponse<SempMetaOnlyResponse> resp = deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameWithHttpInfo(msgVpnName, restDeliveryPointName, restConsumerName, tlsTrustedCommonName);
        return resp.getData();
    }

    /**
     * Delete a Trusted Common Name object.
     * Delete a Trusted Common Name object.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @return ApiResponse&lt;SempMetaOnlyResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SempMetaOnlyResponse> deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameWithHttpInfo(String msgVpnName, String restDeliveryPointName, String restConsumerName, String tlsTrustedCommonName) throws ApiException {
        com.squareup.okhttp.Call call = deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, tlsTrustedCommonName, null, null);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete a Trusted Common Name object. (asynchronously)
     * Delete a Trusted Common Name object.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameAsync(String msgVpnName, String restDeliveryPointName, String restConsumerName, String tlsTrustedCommonName, final ApiCallback<SempMetaOnlyResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, tlsTrustedCommonName, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<SempMetaOnlyResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnRestDeliveryPoint
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointCall(String msgVpnName, String restDeliveryPointName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnRestDeliveryPointValidateBeforeCall(String msgVpnName, String restDeliveryPointName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnRestDeliveryPoint(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling getMsgVpnRestDeliveryPoint(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointCall(msgVpnName, restDeliveryPointName, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a REST Delivery Point object.
     * Get a REST Delivery Point object.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointResponse getMsgVpnRestDeliveryPoint(String msgVpnName, String restDeliveryPointName, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointResponse> resp = getMsgVpnRestDeliveryPointWithHttpInfo(msgVpnName, restDeliveryPointName, select);
        return resp.getData();
    }

    /**
     * Get a REST Delivery Point object.
     * Get a REST Delivery Point object.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointResponse> getMsgVpnRestDeliveryPointWithHttpInfo(String msgVpnName, String restDeliveryPointName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointValidateBeforeCall(msgVpnName, restDeliveryPointName, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a REST Delivery Point object. (asynchronously)
     * Get a REST Delivery Point object.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointAsync(String msgVpnName, String restDeliveryPointName, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointValidateBeforeCall(msgVpnName, restDeliveryPointName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnRestDeliveryPointQueueBinding
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointQueueBindingCall(String msgVpnName, String restDeliveryPointName, String queueBindingName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/queueBindings/{queueBindingName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()))
            .replaceAll("\\{" + "queueBindingName" + "\\}", apiClient.escapeString(queueBindingName.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(String msgVpnName, String restDeliveryPointName, String queueBindingName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling getMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        
        // verify the required parameter 'queueBindingName' is set
        if (queueBindingName == null) {
            throw new ApiException("Missing the required parameter 'queueBindingName' when calling getMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointQueueBindingCall(msgVpnName, restDeliveryPointName, queueBindingName, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Queue Binding object.
     * Get a Queue Binding object.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| queueBindingName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointQueueBindingResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointQueueBindingResponse getMsgVpnRestDeliveryPointQueueBinding(String msgVpnName, String restDeliveryPointName, String queueBindingName, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointQueueBindingResponse> resp = getMsgVpnRestDeliveryPointQueueBindingWithHttpInfo(msgVpnName, restDeliveryPointName, queueBindingName, select);
        return resp.getData();
    }

    /**
     * Get a Queue Binding object.
     * Get a Queue Binding object.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| queueBindingName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointQueueBindingResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointQueueBindingResponse> getMsgVpnRestDeliveryPointQueueBindingWithHttpInfo(String msgVpnName, String restDeliveryPointName, String queueBindingName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(msgVpnName, restDeliveryPointName, queueBindingName, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointQueueBindingResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Queue Binding object. (asynchronously)
     * Get a Queue Binding object.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| queueBindingName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointQueueBindingAsync(String msgVpnName, String restDeliveryPointName, String queueBindingName, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointQueueBindingResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(msgVpnName, restDeliveryPointName, queueBindingName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointQueueBindingResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnRestDeliveryPointQueueBindings
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointQueueBindingsCall(String msgVpnName, String restDeliveryPointName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/queueBindings"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnRestDeliveryPointQueueBindingsValidateBeforeCall(String msgVpnName, String restDeliveryPointName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnRestDeliveryPointQueueBindings(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling getMsgVpnRestDeliveryPointQueueBindings(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointQueueBindingsCall(msgVpnName, restDeliveryPointName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Queue Binding objects.
     * Get a list of Queue Binding objects.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| queueBindingName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointQueueBindingsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointQueueBindingsResponse getMsgVpnRestDeliveryPointQueueBindings(String msgVpnName, String restDeliveryPointName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointQueueBindingsResponse> resp = getMsgVpnRestDeliveryPointQueueBindingsWithHttpInfo(msgVpnName, restDeliveryPointName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Queue Binding objects.
     * Get a list of Queue Binding objects.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| queueBindingName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointQueueBindingsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointQueueBindingsResponse> getMsgVpnRestDeliveryPointQueueBindingsWithHttpInfo(String msgVpnName, String restDeliveryPointName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointQueueBindingsValidateBeforeCall(msgVpnName, restDeliveryPointName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointQueueBindingsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Queue Binding objects. (asynchronously)
     * Get a list of Queue Binding objects.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| queueBindingName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointQueueBindingsAsync(String msgVpnName, String restDeliveryPointName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointQueueBindingsResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointQueueBindingsValidateBeforeCall(msgVpnName, restDeliveryPointName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointQueueBindingsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnRestDeliveryPointRestConsumer
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointRestConsumerCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/restConsumers/{restConsumerName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()))
            .replaceAll("\\{" + "restConsumerName" + "\\}", apiClient.escapeString(restConsumerName.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling getMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        
        // verify the required parameter 'restConsumerName' is set
        if (restConsumerName == null) {
            throw new ApiException("Missing the required parameter 'restConsumerName' when calling getMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointRestConsumerCall(msgVpnName, restDeliveryPointName, restConsumerName, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a REST Consumer object.
     * Get a REST Consumer object.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationClientCertContent||x| authenticationClientCertPassword||x| authenticationHttpBasicPassword||x| msgVpnName|x|| restConsumerName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointRestConsumerResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointRestConsumerResponse getMsgVpnRestDeliveryPointRestConsumer(String msgVpnName, String restDeliveryPointName, String restConsumerName, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointRestConsumerResponse> resp = getMsgVpnRestDeliveryPointRestConsumerWithHttpInfo(msgVpnName, restDeliveryPointName, restConsumerName, select);
        return resp.getData();
    }

    /**
     * Get a REST Consumer object.
     * Get a REST Consumer object.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationClientCertContent||x| authenticationClientCertPassword||x| authenticationHttpBasicPassword||x| msgVpnName|x|| restConsumerName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointRestConsumerResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointRestConsumerResponse> getMsgVpnRestDeliveryPointRestConsumerWithHttpInfo(String msgVpnName, String restDeliveryPointName, String restConsumerName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a REST Consumer object. (asynchronously)
     * Get a REST Consumer object.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationClientCertContent||x| authenticationClientCertPassword||x| authenticationHttpBasicPassword||x| msgVpnName|x|| restConsumerName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointRestConsumerAsync(String msgVpnName, String restDeliveryPointName, String restConsumerName, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointRestConsumerResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, String tlsTrustedCommonName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/restConsumers/{restConsumerName}/tlsTrustedCommonNames/{tlsTrustedCommonName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()))
            .replaceAll("\\{" + "restConsumerName" + "\\}", apiClient.escapeString(restConsumerName.toString()))
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
    private com.squareup.okhttp.Call getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameValidateBeforeCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, String tlsTrustedCommonName, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'restConsumerName' is set
        if (restConsumerName == null) {
            throw new ApiException("Missing the required parameter 'restConsumerName' when calling getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(Async)");
        }
        
        // verify the required parameter 'tlsTrustedCommonName' is set
        if (tlsTrustedCommonName == null) {
            throw new ApiException("Missing the required parameter 'tlsTrustedCommonName' when calling getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameCall(msgVpnName, restDeliveryPointName, restConsumerName, tlsTrustedCommonName, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a Trusted Common Name object.
     * Get a Trusted Common Name object.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| restConsumerName|x|| restDeliveryPointName|x|| tlsTrustedCommonName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(String msgVpnName, String restDeliveryPointName, String restConsumerName, String tlsTrustedCommonName, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse> resp = getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameWithHttpInfo(msgVpnName, restDeliveryPointName, restConsumerName, tlsTrustedCommonName, select);
        return resp.getData();
    }

    /**
     * Get a Trusted Common Name object.
     * Get a Trusted Common Name object.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| restConsumerName|x|| restDeliveryPointName|x|| tlsTrustedCommonName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse> getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameWithHttpInfo(String msgVpnName, String restDeliveryPointName, String restConsumerName, String tlsTrustedCommonName, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, tlsTrustedCommonName, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a Trusted Common Name object. (asynchronously)
     * Get a Trusted Common Name object.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| restConsumerName|x|| restDeliveryPointName|x|| tlsTrustedCommonName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param tlsTrustedCommonName The expected trusted common name of the remote certificate. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameAsync(String msgVpnName, String restDeliveryPointName, String restConsumerName, String tlsTrustedCommonName, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, tlsTrustedCommonName, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNames
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/restConsumers/{restConsumerName}/tlsTrustedCommonNames"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()))
            .replaceAll("\\{" + "restConsumerName" + "\\}", apiClient.escapeString(restConsumerName.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesValidateBeforeCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNames(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNames(Async)");
        }
        
        // verify the required parameter 'restConsumerName' is set
        if (restConsumerName == null) {
            throw new ApiException("Missing the required parameter 'restConsumerName' when calling getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNames(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesCall(msgVpnName, restDeliveryPointName, restConsumerName, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of Trusted Common Name objects.
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| restConsumerName|x|| restDeliveryPointName|x|| tlsTrustedCommonName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesResponse getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNames(String msgVpnName, String restDeliveryPointName, String restConsumerName, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesResponse> resp = getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesWithHttpInfo(msgVpnName, restDeliveryPointName, restConsumerName, where, select);
        return resp.getData();
    }

    /**
     * Get a list of Trusted Common Name objects.
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| restConsumerName|x|| restDeliveryPointName|x|| tlsTrustedCommonName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesResponse> getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesWithHttpInfo(String msgVpnName, String restDeliveryPointName, String restConsumerName, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of Trusted Common Name objects. (asynchronously)
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| restConsumerName|x|| restDeliveryPointName|x|| tlsTrustedCommonName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesAsync(String msgVpnName, String restDeliveryPointName, String restConsumerName, List<String> where, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnRestDeliveryPointRestConsumers
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointRestConsumersCall(String msgVpnName, String restDeliveryPointName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/restConsumers"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()));

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
    private com.squareup.okhttp.Call getMsgVpnRestDeliveryPointRestConsumersValidateBeforeCall(String msgVpnName, String restDeliveryPointName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnRestDeliveryPointRestConsumers(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling getMsgVpnRestDeliveryPointRestConsumers(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointRestConsumersCall(msgVpnName, restDeliveryPointName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of REST Consumer objects.
     * Get a list of REST Consumer objects.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationClientCertContent||x| authenticationClientCertPassword||x| authenticationHttpBasicPassword||x| msgVpnName|x|| restConsumerName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointRestConsumersResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointRestConsumersResponse getMsgVpnRestDeliveryPointRestConsumers(String msgVpnName, String restDeliveryPointName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointRestConsumersResponse> resp = getMsgVpnRestDeliveryPointRestConsumersWithHttpInfo(msgVpnName, restDeliveryPointName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of REST Consumer objects.
     * Get a list of REST Consumer objects.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationClientCertContent||x| authenticationClientCertPassword||x| authenticationHttpBasicPassword||x| msgVpnName|x|| restConsumerName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointRestConsumersResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointRestConsumersResponse> getMsgVpnRestDeliveryPointRestConsumersWithHttpInfo(String msgVpnName, String restDeliveryPointName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointRestConsumersValidateBeforeCall(msgVpnName, restDeliveryPointName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumersResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of REST Consumer objects. (asynchronously)
     * Get a list of REST Consumer objects.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: authenticationClientCertContent||x| authenticationClientCertPassword||x| authenticationHttpBasicPassword||x| msgVpnName|x|| restConsumerName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointRestConsumersAsync(String msgVpnName, String restDeliveryPointName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointRestConsumersResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointRestConsumersValidateBeforeCall(msgVpnName, restDeliveryPointName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumersResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getMsgVpnRestDeliveryPoints
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
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointsCall(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints"
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
    private com.squareup.okhttp.Call getMsgVpnRestDeliveryPointsValidateBeforeCall(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling getMsgVpnRestDeliveryPoints(Async)");
        }
        

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointsCall(msgVpnName, count, cursor, where, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get a list of REST Delivery Point objects.
     * Get a list of REST Delivery Point objects.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointsResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointsResponse getMsgVpnRestDeliveryPoints(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointsResponse> resp = getMsgVpnRestDeliveryPointsWithHttpInfo(msgVpnName, count, cursor, where, select);
        return resp.getData();
    }

    /**
     * Get a list of REST Delivery Point objects.
     * Get a list of REST Delivery Point objects.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointsResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointsResponse> getMsgVpnRestDeliveryPointsWithHttpInfo(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointsValidateBeforeCall(msgVpnName, count, cursor, where, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointsResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get a list of REST Delivery Point objects. (asynchronously)
     * Get a list of REST Delivery Point objects.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Write-Only|Deprecated :---|:---:|:---:|:---: msgVpnName|x|| restDeliveryPointName|x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param count Limit the count of objects in the response. See the documentation for the &#x60;count&#x60; parameter. (optional, default to 10)
     * @param cursor The cursor, or position, for the next page of objects. See the documentation for the &#x60;cursor&#x60; parameter. (optional)
     * @param where Include in the response only objects where certain conditions are true. See the the documentation for the &#x60;where&#x60; parameter. (optional)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getMsgVpnRestDeliveryPointsAsync(String msgVpnName, Integer count, String cursor, List<String> where, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointsResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = getMsgVpnRestDeliveryPointsValidateBeforeCall(msgVpnName, count, cursor, where, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointsResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for replaceMsgVpnRestDeliveryPoint
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The REST Delivery Point object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnRestDeliveryPointCall(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPoint body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()));

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
    private com.squareup.okhttp.Call replaceMsgVpnRestDeliveryPointValidateBeforeCall(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPoint body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling replaceMsgVpnRestDeliveryPoint(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling replaceMsgVpnRestDeliveryPoint(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling replaceMsgVpnRestDeliveryPoint(Async)");
        }
        

        com.squareup.okhttp.Call call = replaceMsgVpnRestDeliveryPointCall(msgVpnName, restDeliveryPointName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Replace a REST Delivery Point object.
     * Replace a REST Delivery Point object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: clientProfileName||||x| msgVpnName|x|x||| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The REST Delivery Point object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointResponse replaceMsgVpnRestDeliveryPoint(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPoint body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointResponse> resp = replaceMsgVpnRestDeliveryPointWithHttpInfo(msgVpnName, restDeliveryPointName, body, select);
        return resp.getData();
    }

    /**
     * Replace a REST Delivery Point object.
     * Replace a REST Delivery Point object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: clientProfileName||||x| msgVpnName|x|x||| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The REST Delivery Point object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointResponse> replaceMsgVpnRestDeliveryPointWithHttpInfo(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPoint body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = replaceMsgVpnRestDeliveryPointValidateBeforeCall(msgVpnName, restDeliveryPointName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Replace a REST Delivery Point object. (asynchronously)
     * Replace a REST Delivery Point object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: clientProfileName||||x| msgVpnName|x|x||| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The REST Delivery Point object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnRestDeliveryPointAsync(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPoint body, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = replaceMsgVpnRestDeliveryPointValidateBeforeCall(msgVpnName, restDeliveryPointName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for replaceMsgVpnRestDeliveryPointQueueBinding
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param body The Queue Binding object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnRestDeliveryPointQueueBindingCall(String msgVpnName, String restDeliveryPointName, String queueBindingName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/queueBindings/{queueBindingName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()))
            .replaceAll("\\{" + "queueBindingName" + "\\}", apiClient.escapeString(queueBindingName.toString()));

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
    private com.squareup.okhttp.Call replaceMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(String msgVpnName, String restDeliveryPointName, String queueBindingName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling replaceMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling replaceMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        
        // verify the required parameter 'queueBindingName' is set
        if (queueBindingName == null) {
            throw new ApiException("Missing the required parameter 'queueBindingName' when calling replaceMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling replaceMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        

        com.squareup.okhttp.Call call = replaceMsgVpnRestDeliveryPointQueueBindingCall(msgVpnName, restDeliveryPointName, queueBindingName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Replace a Queue Binding object.
     * Replace a Queue Binding object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x||| queueBindingName|x|x||| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param body The Queue Binding object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointQueueBindingResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointQueueBindingResponse replaceMsgVpnRestDeliveryPointQueueBinding(String msgVpnName, String restDeliveryPointName, String queueBindingName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointQueueBindingResponse> resp = replaceMsgVpnRestDeliveryPointQueueBindingWithHttpInfo(msgVpnName, restDeliveryPointName, queueBindingName, body, select);
        return resp.getData();
    }

    /**
     * Replace a Queue Binding object.
     * Replace a Queue Binding object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x||| queueBindingName|x|x||| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param body The Queue Binding object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointQueueBindingResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointQueueBindingResponse> replaceMsgVpnRestDeliveryPointQueueBindingWithHttpInfo(String msgVpnName, String restDeliveryPointName, String queueBindingName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = replaceMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(msgVpnName, restDeliveryPointName, queueBindingName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointQueueBindingResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Replace a Queue Binding object. (asynchronously)
     * Replace a Queue Binding object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x||| queueBindingName|x|x||| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param body The Queue Binding object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnRestDeliveryPointQueueBindingAsync(String msgVpnName, String restDeliveryPointName, String queueBindingName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointQueueBindingResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = replaceMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(msgVpnName, restDeliveryPointName, queueBindingName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointQueueBindingResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for replaceMsgVpnRestDeliveryPointRestConsumer
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param body The REST Consumer object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnRestDeliveryPointRestConsumerCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/restConsumers/{restConsumerName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()))
            .replaceAll("\\{" + "restConsumerName" + "\\}", apiClient.escapeString(restConsumerName.toString()));

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
    private com.squareup.okhttp.Call replaceMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling replaceMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling replaceMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        
        // verify the required parameter 'restConsumerName' is set
        if (restConsumerName == null) {
            throw new ApiException("Missing the required parameter 'restConsumerName' when calling replaceMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling replaceMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        

        com.squareup.okhttp.Call call = replaceMsgVpnRestDeliveryPointRestConsumerCall(msgVpnName, restDeliveryPointName, restConsumerName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Replace a REST Consumer object.
     * Replace a REST Consumer object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationClientCertContent|||x|x| authenticationClientCertPassword|||x|x| authenticationHttpBasicPassword|||x|x| authenticationHttpBasicUsername||||x| authenticationScheme||||x| msgVpnName|x|x||| outgoingConnectionCount||||x| remoteHost||||x| remotePort||||x| restConsumerName|x|x||| restDeliveryPointName|x|x||| tlsCipherSuiteList||||x| tlsEnabled||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnRestDeliveryPointRestConsumer|authenticationClientCertPassword|authenticationClientCertContent| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicPassword|authenticationHttpBasicUsername| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicUsername|authenticationHttpBasicPassword| MsgVpnRestDeliveryPointRestConsumer|remotePort|tlsEnabled| MsgVpnRestDeliveryPointRestConsumer|tlsEnabled|remotePort|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param body The REST Consumer object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointRestConsumerResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointRestConsumerResponse replaceMsgVpnRestDeliveryPointRestConsumer(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointRestConsumerResponse> resp = replaceMsgVpnRestDeliveryPointRestConsumerWithHttpInfo(msgVpnName, restDeliveryPointName, restConsumerName, body, select);
        return resp.getData();
    }

    /**
     * Replace a REST Consumer object.
     * Replace a REST Consumer object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationClientCertContent|||x|x| authenticationClientCertPassword|||x|x| authenticationHttpBasicPassword|||x|x| authenticationHttpBasicUsername||||x| authenticationScheme||||x| msgVpnName|x|x||| outgoingConnectionCount||||x| remoteHost||||x| remotePort||||x| restConsumerName|x|x||| restDeliveryPointName|x|x||| tlsCipherSuiteList||||x| tlsEnabled||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnRestDeliveryPointRestConsumer|authenticationClientCertPassword|authenticationClientCertContent| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicPassword|authenticationHttpBasicUsername| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicUsername|authenticationHttpBasicPassword| MsgVpnRestDeliveryPointRestConsumer|remotePort|tlsEnabled| MsgVpnRestDeliveryPointRestConsumer|tlsEnabled|remotePort|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param body The REST Consumer object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointRestConsumerResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointRestConsumerResponse> replaceMsgVpnRestDeliveryPointRestConsumerWithHttpInfo(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = replaceMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Replace a REST Consumer object. (asynchronously)
     * Replace a REST Consumer object. Any attribute missing from the request will be set to its default value, unless the user is not authorized to change its value or the attribute is write-only, in which case the missing attribute will be left unchanged.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationClientCertContent|||x|x| authenticationClientCertPassword|||x|x| authenticationHttpBasicPassword|||x|x| authenticationHttpBasicUsername||||x| authenticationScheme||||x| msgVpnName|x|x||| outgoingConnectionCount||||x| remoteHost||||x| remotePort||||x| restConsumerName|x|x||| restDeliveryPointName|x|x||| tlsCipherSuiteList||||x| tlsEnabled||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnRestDeliveryPointRestConsumer|authenticationClientCertPassword|authenticationClientCertContent| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicPassword|authenticationHttpBasicUsername| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicUsername|authenticationHttpBasicPassword| MsgVpnRestDeliveryPointRestConsumer|remotePort|tlsEnabled| MsgVpnRestDeliveryPointRestConsumer|tlsEnabled|remotePort|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param body The REST Consumer object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call replaceMsgVpnRestDeliveryPointRestConsumerAsync(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointRestConsumerResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = replaceMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for updateMsgVpnRestDeliveryPoint
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The REST Delivery Point object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnRestDeliveryPointCall(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPoint body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()));

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
    private com.squareup.okhttp.Call updateMsgVpnRestDeliveryPointValidateBeforeCall(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPoint body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling updateMsgVpnRestDeliveryPoint(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling updateMsgVpnRestDeliveryPoint(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling updateMsgVpnRestDeliveryPoint(Async)");
        }
        

        com.squareup.okhttp.Call call = updateMsgVpnRestDeliveryPointCall(msgVpnName, restDeliveryPointName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Update a REST Delivery Point object.
     * Update a REST Delivery Point object. Any attribute missing from the request will be left unchanged.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: clientProfileName||||x| msgVpnName|x|x||| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The REST Delivery Point object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointResponse updateMsgVpnRestDeliveryPoint(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPoint body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointResponse> resp = updateMsgVpnRestDeliveryPointWithHttpInfo(msgVpnName, restDeliveryPointName, body, select);
        return resp.getData();
    }

    /**
     * Update a REST Delivery Point object.
     * Update a REST Delivery Point object. Any attribute missing from the request will be left unchanged.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: clientProfileName||||x| msgVpnName|x|x||| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The REST Delivery Point object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointResponse> updateMsgVpnRestDeliveryPointWithHttpInfo(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPoint body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = updateMsgVpnRestDeliveryPointValidateBeforeCall(msgVpnName, restDeliveryPointName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Update a REST Delivery Point object. (asynchronously)
     * Update a REST Delivery Point object. Any attribute missing from the request will be left unchanged.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: clientProfileName||||x| msgVpnName|x|x||| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param body The REST Delivery Point object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnRestDeliveryPointAsync(String msgVpnName, String restDeliveryPointName, MsgVpnRestDeliveryPoint body, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = updateMsgVpnRestDeliveryPointValidateBeforeCall(msgVpnName, restDeliveryPointName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for updateMsgVpnRestDeliveryPointQueueBinding
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param body The Queue Binding object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnRestDeliveryPointQueueBindingCall(String msgVpnName, String restDeliveryPointName, String queueBindingName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/queueBindings/{queueBindingName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()))
            .replaceAll("\\{" + "queueBindingName" + "\\}", apiClient.escapeString(queueBindingName.toString()));

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
    private com.squareup.okhttp.Call updateMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(String msgVpnName, String restDeliveryPointName, String queueBindingName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling updateMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling updateMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        
        // verify the required parameter 'queueBindingName' is set
        if (queueBindingName == null) {
            throw new ApiException("Missing the required parameter 'queueBindingName' when calling updateMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling updateMsgVpnRestDeliveryPointQueueBinding(Async)");
        }
        

        com.squareup.okhttp.Call call = updateMsgVpnRestDeliveryPointQueueBindingCall(msgVpnName, restDeliveryPointName, queueBindingName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Update a Queue Binding object.
     * Update a Queue Binding object. Any attribute missing from the request will be left unchanged.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x||| queueBindingName|x|x||| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param body The Queue Binding object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointQueueBindingResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointQueueBindingResponse updateMsgVpnRestDeliveryPointQueueBinding(String msgVpnName, String restDeliveryPointName, String queueBindingName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointQueueBindingResponse> resp = updateMsgVpnRestDeliveryPointQueueBindingWithHttpInfo(msgVpnName, restDeliveryPointName, queueBindingName, body, select);
        return resp.getData();
    }

    /**
     * Update a Queue Binding object.
     * Update a Queue Binding object. Any attribute missing from the request will be left unchanged.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x||| queueBindingName|x|x||| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param body The Queue Binding object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointQueueBindingResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointQueueBindingResponse> updateMsgVpnRestDeliveryPointQueueBindingWithHttpInfo(String msgVpnName, String restDeliveryPointName, String queueBindingName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = updateMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(msgVpnName, restDeliveryPointName, queueBindingName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointQueueBindingResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Update a Queue Binding object. (asynchronously)
     * Update a Queue Binding object. Any attribute missing from the request will be left unchanged.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x||| queueBindingName|x|x||| restDeliveryPointName|x|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param queueBindingName The name of a queue in the Message VPN. (required)
     * @param body The Queue Binding object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnRestDeliveryPointQueueBindingAsync(String msgVpnName, String restDeliveryPointName, String queueBindingName, MsgVpnRestDeliveryPointQueueBinding body, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointQueueBindingResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = updateMsgVpnRestDeliveryPointQueueBindingValidateBeforeCall(msgVpnName, restDeliveryPointName, queueBindingName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointQueueBindingResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for updateMsgVpnRestDeliveryPointRestConsumer
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param body The REST Consumer object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnRestDeliveryPointRestConsumerCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/msgVpns/{msgVpnName}/restDeliveryPoints/{restDeliveryPointName}/restConsumers/{restConsumerName}"
            .replaceAll("\\{" + "msgVpnName" + "\\}", apiClient.escapeString(msgVpnName.toString()))
            .replaceAll("\\{" + "restDeliveryPointName" + "\\}", apiClient.escapeString(restDeliveryPointName.toString()))
            .replaceAll("\\{" + "restConsumerName" + "\\}", apiClient.escapeString(restConsumerName.toString()));

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
    private com.squareup.okhttp.Call updateMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'msgVpnName' is set
        if (msgVpnName == null) {
            throw new ApiException("Missing the required parameter 'msgVpnName' when calling updateMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        
        // verify the required parameter 'restDeliveryPointName' is set
        if (restDeliveryPointName == null) {
            throw new ApiException("Missing the required parameter 'restDeliveryPointName' when calling updateMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        
        // verify the required parameter 'restConsumerName' is set
        if (restConsumerName == null) {
            throw new ApiException("Missing the required parameter 'restConsumerName' when calling updateMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling updateMsgVpnRestDeliveryPointRestConsumer(Async)");
        }
        

        com.squareup.okhttp.Call call = updateMsgVpnRestDeliveryPointRestConsumerCall(msgVpnName, restDeliveryPointName, restConsumerName, body, select, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Update a REST Consumer object.
     * Update a REST Consumer object. Any attribute missing from the request will be left unchanged.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationClientCertContent|||x|x| authenticationClientCertPassword|||x|x| authenticationHttpBasicPassword|||x|x| authenticationHttpBasicUsername||||x| authenticationScheme||||x| msgVpnName|x|x||| outgoingConnectionCount||||x| remoteHost||||x| remotePort||||x| restConsumerName|x|x||| restDeliveryPointName|x|x||| tlsCipherSuiteList||||x| tlsEnabled||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnRestDeliveryPointRestConsumer|authenticationClientCertPassword|authenticationClientCertContent| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicPassword|authenticationHttpBasicUsername| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicUsername|authenticationHttpBasicPassword| MsgVpnRestDeliveryPointRestConsumer|remotePort|tlsEnabled| MsgVpnRestDeliveryPointRestConsumer|tlsEnabled|remotePort|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param body The REST Consumer object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return MsgVpnRestDeliveryPointRestConsumerResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public MsgVpnRestDeliveryPointRestConsumerResponse updateMsgVpnRestDeliveryPointRestConsumer(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select) throws ApiException {
        ApiResponse<MsgVpnRestDeliveryPointRestConsumerResponse> resp = updateMsgVpnRestDeliveryPointRestConsumerWithHttpInfo(msgVpnName, restDeliveryPointName, restConsumerName, body, select);
        return resp.getData();
    }

    /**
     * Update a REST Consumer object.
     * Update a REST Consumer object. Any attribute missing from the request will be left unchanged.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationClientCertContent|||x|x| authenticationClientCertPassword|||x|x| authenticationHttpBasicPassword|||x|x| authenticationHttpBasicUsername||||x| authenticationScheme||||x| msgVpnName|x|x||| outgoingConnectionCount||||x| remoteHost||||x| remotePort||||x| restConsumerName|x|x||| restDeliveryPointName|x|x||| tlsCipherSuiteList||||x| tlsEnabled||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnRestDeliveryPointRestConsumer|authenticationClientCertPassword|authenticationClientCertContent| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicPassword|authenticationHttpBasicUsername| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicUsername|authenticationHttpBasicPassword| MsgVpnRestDeliveryPointRestConsumer|remotePort|tlsEnabled| MsgVpnRestDeliveryPointRestConsumer|tlsEnabled|remotePort|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param body The REST Consumer object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @return ApiResponse&lt;MsgVpnRestDeliveryPointRestConsumerResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<MsgVpnRestDeliveryPointRestConsumerResponse> updateMsgVpnRestDeliveryPointRestConsumerWithHttpInfo(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select) throws ApiException {
        com.squareup.okhttp.Call call = updateMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, body, select, null, null);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerResponse>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Update a REST Consumer object. (asynchronously)
     * Update a REST Consumer object. Any attribute missing from the request will be left unchanged.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated :---|:---:|:---:|:---:|:---:|:---: authenticationClientCertContent|||x|x| authenticationClientCertPassword|||x|x| authenticationHttpBasicPassword|||x|x| authenticationHttpBasicUsername||||x| authenticationScheme||||x| msgVpnName|x|x||| outgoingConnectionCount||||x| remoteHost||||x| remotePort||||x| restConsumerName|x|x||| restDeliveryPointName|x|x||| tlsCipherSuiteList||||x| tlsEnabled||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnRestDeliveryPointRestConsumer|authenticationClientCertPassword|authenticationClientCertContent| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicPassword|authenticationHttpBasicUsername| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicUsername|authenticationHttpBasicPassword| MsgVpnRestDeliveryPointRestConsumer|remotePort|tlsEnabled| MsgVpnRestDeliveryPointRestConsumer|tlsEnabled|remotePort|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     * @param msgVpnName The name of the Message VPN. (required)
     * @param restDeliveryPointName The name of the REST Delivery Point. (required)
     * @param restConsumerName The name of the REST Consumer. (required)
     * @param body The REST Consumer object&#39;s attributes. (required)
     * @param select Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. See the documentation for the &#x60;select&#x60; parameter. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call updateMsgVpnRestDeliveryPointRestConsumerAsync(String msgVpnName, String restDeliveryPointName, String restConsumerName, MsgVpnRestDeliveryPointRestConsumer body, List<String> select, final ApiCallback<MsgVpnRestDeliveryPointRestConsumerResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = updateMsgVpnRestDeliveryPointRestConsumerValidateBeforeCall(msgVpnName, restDeliveryPointName, restConsumerName, body, select, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<MsgVpnRestDeliveryPointRestConsumerResponse>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
