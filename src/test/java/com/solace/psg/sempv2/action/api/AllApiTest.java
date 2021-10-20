/*
 * SEMP (Solace Element Management Protocol)
 * SEMP (starting in `v2`, see note 1) is a RESTful API for configuring, monitoring, and administering a Solace PubSub+ broker.  SEMP uses URIs to address manageable **resources** of the Solace PubSub+ broker. Resources are individual **objects**, **collections** of objects, or (exclusively in the action API) **actions**. This document applies to the following API:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Action|/SEMP/v2/action|Performing actions|See note 2    The following APIs are also available:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Configuration|/SEMP/v2/config|Reading and writing config state|See note 2 Monitoring|/SEMP/v2/monitor|Querying operational parameters|See note 2    Resources are always nouns, with individual objects being singular and collections being plural.  Objects within a collection are identified by an `obj-id`, which follows the collection name with the form `collection-name/obj-id`.  Actions within an object are identified by an `action-id`, which follows the object name with the form `obj-id/action-id`.  Some examples:  ``` /SEMP/v2/config/msgVpns                        ; MsgVpn collection /SEMP/v2/config/msgVpns/a                      ; MsgVpn object named \"a\" /SEMP/v2/config/msgVpns/a/queues               ; Queue collection in MsgVpn \"a\" /SEMP/v2/config/msgVpns/a/queues/b             ; Queue object named \"b\" in MsgVpn \"a\" /SEMP/v2/action/msgVpns/a/queues/b/startReplay ; Action that starts a replay on Queue \"b\" in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients             ; Client collection in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients/c           ; Client object named \"c\" in MsgVpn \"a\" ```  ## Collection Resources  Collections are unordered lists of objects (unless described as otherwise), and are described by JSON arrays. Each item in the array represents an object in the same manner as the individual object would normally be represented. In the configuration API, the creation of a new object is done through its collection resource.  ## Object and Action Resources  Objects are composed of attributes, actions, collections, and other objects. They are described by JSON objects as name/value pairs. The collections and actions of an object are not contained directly in the object's JSON content; rather the content includes an attribute containing a URI which points to the collections and actions. These contained resources must be managed through this URI. At a minimum, every object has one or more identifying attributes, and its own `uri` attribute which contains the URI pointing to itself.  Actions are also composed of attributes, and are described by JSON objects as name/value pairs. Unlike objects, however, they are not members of a collection and cannot be retrieved, only performed. Actions only exist in the action API.  Attributes in an object or action may have any combination of the following properties:   Property|Meaning|Comments :---|:---|:--- Identifying|Attribute is involved in unique identification of the object, and appears in its URI| Required|Attribute must be provided in the request| Read-Only|Attribute can only be read, not written.|See note 3 Write-Only|Attribute can only be written, not read, unless the attribute is also opaque|See the documentation for the opaque property Requires-Disable|Attribute can only be changed when object is disabled| Deprecated|Attribute is deprecated, and will disappear in the next SEMP version| Opaque|Attribute can be set or retrieved in opaque form when the `opaquePassword` query parameter is present|See the `opaquePassword` query parameter documentation    In some requests, certain attributes may only be provided in certain combinations with other attributes:   Relationship|Meaning :---|:--- Requires|Attribute may only be changed by a request if a particular attribute or combination of attributes is also provided in the request Conflicts|Attribute may only be provided in a request if a particular attribute or combination of attributes is not also provided in the request    In the monitoring API, any non-identifying attribute may not be returned in a GET.  ## HTTP Methods  The following HTTP methods manipulate resources in accordance with these general principles. Note that some methods are only used in certain APIs:   Method|Resource|Meaning|Request Body|Response Body|Missing Request Attributes :---|:---|:---|:---|:---|:--- POST|Collection|Create object|Initial attribute values|Object attributes and metadata|Set to default PUT|Object|Create or replace object (see note 5)|New attribute values|Object attributes and metadata|Set to default, with certain exceptions (see note 4) PUT|Action|Performs action|Action arguments|Action metadata|N/A PATCH|Object|Update object|New attribute values|Object attributes and metadata|unchanged DELETE|Object|Delete object|Empty|Object metadata|N/A GET|Object|Get object|Empty|Object attributes and metadata|N/A GET|Collection|Get collection|Empty|Object attributes and collection metadata|N/A    ## Common Query Parameters  The following are some common query parameters that are supported by many method/URI combinations. Individual URIs may document additional parameters. Note that multiple query parameters can be used together in a single URI, separated by the ampersand character. For example:  ``` ; Request for the MsgVpns collection using two hypothetical query parameters ; \"q1\" and \"q2\" with values \"val1\" and \"val2\" respectively /SEMP/v2/action/msgVpns?q1=val1&q2=val2 ```  ### select  Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. Use this query parameter to limit the size of the returned data for each returned object, return only those fields that are desired, or exclude fields that are not desired.  The value of `select` is a comma-separated list of attribute names. If the list contains attribute names that are not prefaced by `-`, only those attributes are included in the response. If the list contains attribute names that are prefaced by `-`, those attributes are excluded from the response. If the list contains both types, then the difference of the first set of attributes and the second set of attributes is returned. If the list is empty (i.e. `select=`), no attributes are returned.  All attributes that are prefaced by `-` must follow all attributes that are not prefaced by `-`. In addition, each attribute name in the list must match at least one attribute in the object.  Names may include the `*` wildcard (zero or more characters). Nested attribute names are supported using periods (e.g. `parentName.childName`).  Some examples:  ``` ; List of all MsgVpn names /SEMP/v2/action/msgVpns?select=msgVpnName ; List of all MsgVpn and their attributes except for their names /SEMP/v2/action/msgVpns?select=-msgVpnName ; Authentication attributes of MsgVpn \"finance\" /SEMP/v2/action/msgVpns/finance?select=authentication* ; All attributes of MsgVpn \"finance\" except for authentication attributes /SEMP/v2/action/msgVpns/finance?select=-authentication* ; Access related attributes of Queue \"orderQ\" of MsgVpn \"finance\" /SEMP/v2/action/msgVpns/finance/queues/orderQ?select=owner,permission ```  ### where  Include in the response only objects where certain conditions are true. Use this query parameter to limit which objects are returned to those whose attribute values meet the given conditions.  The value of `where` is a comma-separated list of expressions. All expressions must be true for the object to be included in the response. Each expression takes the form:  ``` expression  = attribute-name OP value OP          = '==' | '!=' | '&lt;' | '&gt;' | '&lt;=' | '&gt;=' ```  `value` may be a number, string, `true`, or `false`, as appropriate for the type of `attribute-name`. Greater-than and less-than comparisons only work for numbers. A `*` in a string `value` is interpreted as a wildcard (zero or more characters). Some examples:  ``` ; Only enabled MsgVpns /SEMP/v2/action/msgVpns?where=enabled==true ; Only MsgVpns using basic non-LDAP authentication /SEMP/v2/action/msgVpns?where=authenticationBasicEnabled==true,authenticationBasicType!=ldap ; Only MsgVpns that allow more than 100 client connections /SEMP/v2/action/msgVpns?where=maxConnectionCount>100 ; Only MsgVpns with msgVpnName starting with \"B\": /SEMP/v2/action/msgVpns?where=msgVpnName==B* ```  ### count  Limit the count of objects in the response. This can be useful to limit the size of the response for large collections. The minimum value for `count` is `1` and the default is `10`. There is also a per-collection maximum value to limit request handling time. For example:  ``` ; Up to 25 MsgVpns /SEMP/v2/action/msgVpns?count=25 ```  ### cursor  The cursor, or position, for the next page of objects. Cursors are opaque data that should not be created or interpreted by SEMP clients, and should only be used as described below.  When a request is made for a collection and there may be additional objects available for retrieval that are not included in the initial response, the response will include a `cursorQuery` field containing a cursor. The value of this field can be specified in the `cursor` query parameter of a subsequent request to retrieve the next page of objects. For convenience, an appropriate URI is constructed automatically by the broker and included in the `nextPageUri` field of the response. This URI can be used directly to retrieve the next page of objects.  ### opaquePassword  Attributes with the opaque property are also write-only and so cannot normally be retrieved in a GET. However, when a password is provided in the `opaquePassword` query parameter, attributes with the opaque property are retrieved in a GET in opaque form, encrypted with this password. The query parameter can also be used on a POST, PATCH, or PUT to set opaque attributes using opaque attribute values retrieved in a GET, so long as:  1. the same password that was used to retrieve the opaque attribute values is provided; and  2. the broker to which the request is being sent has the same major and minor SEMP version as the broker that produced the opaque attribute values.  The password provided in the query parameter must be a minimum of 8 characters and a maximum of 128 characters.  The query parameter can only be used in the configuration API, and only over HTTPS.  ## Help  Visit [our website](https://solace.com) to learn more about Solace.  You can also download the SEMP API specifications by clicking [here](https://solace.com/downloads/).  If you need additional support, please contact us at [support@solace.com](mailto:support@solace.com).  ## Notes  Note|Description :---:|:--- 1|This specification defines SEMP starting in \"v2\", and not the original SEMP \"v1\" interface. Request and response formats between \"v1\" and \"v2\" are entirely incompatible, although both protocols share a common port configuration on the Solace PubSub+ broker. They are differentiated by the initial portion of the URI path, one of either \"/SEMP/\" or \"/SEMP/v2/\" 2|This API is partially implemented. Only a subset of all objects are available. 3|Read-only attributes may appear in POST and PUT/PATCH requests. However, if a read-only attribute is not marked as identifying, it will be ignored during a PUT/PATCH. 4|On a PUT, if the SEMP user is not authorized to modify the attribute, its value is left unchanged rather than set to default. In addition, the values of write-only attributes are not set to their defaults on a PUT, except in the following two cases: there is a mutual requires relationship with another non-write-only attribute and both attributes are absent from the request; or the attribute is also opaque and the `opaquePassword` query parameter is provided in the request. 5|On a PUT, if the object does not exist, it is created first.  
 *
 * OpenAPI spec version: 2.17
 * Contact: support@solace.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.solace.psg.sempv2.action.api;

import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.action.model.AboutApiResponse;
import com.solace.psg.sempv2.action.model.AboutResponse;
import com.solace.psg.sempv2.action.model.AboutUserMsgVpnResponse;
import com.solace.psg.sempv2.action.model.AboutUserMsgVpnsResponse;
import com.solace.psg.sempv2.action.model.AboutUserResponse;
import com.solace.psg.sempv2.action.model.BrokerResponse;
import com.solace.psg.sempv2.action.model.CertAuthoritiesResponse;
import com.solace.psg.sempv2.action.model.CertAuthorityRefreshCrl;
import com.solace.psg.sempv2.action.model.CertAuthorityResponse;
import com.solace.psg.sempv2.action.model.MsgVpnAuthenticationOauthProviderClearStats;
import com.solace.psg.sempv2.action.model.MsgVpnAuthenticationOauthProviderResponse;
import com.solace.psg.sempv2.action.model.MsgVpnAuthenticationOauthProvidersResponse;
import com.solace.psg.sempv2.action.model.MsgVpnBridgeClearEvent;
import com.solace.psg.sempv2.action.model.MsgVpnBridgeClearStats;
import com.solace.psg.sempv2.action.model.MsgVpnBridgeDisconnect;
import com.solace.psg.sempv2.action.model.MsgVpnBridgeResponse;
import com.solace.psg.sempv2.action.model.MsgVpnBridgesResponse;
import com.solace.psg.sempv2.action.model.MsgVpnClearMsgSpoolStats;
import com.solace.psg.sempv2.action.model.MsgVpnClearReplicationStats;
import com.solace.psg.sempv2.action.model.MsgVpnClearServiceStats;
import com.solace.psg.sempv2.action.model.MsgVpnClearStats;
import com.solace.psg.sempv2.action.model.MsgVpnClientClearEvent;
import com.solace.psg.sempv2.action.model.MsgVpnClientClearStats;
import com.solace.psg.sempv2.action.model.MsgVpnClientDisconnect;
import com.solace.psg.sempv2.action.model.MsgVpnClientResponse;
import com.solace.psg.sempv2.action.model.MsgVpnClientTransactedSessionDelete;
import com.solace.psg.sempv2.action.model.MsgVpnClientTransactedSessionResponse;
import com.solace.psg.sempv2.action.model.MsgVpnClientTransactedSessionsResponse;
import com.solace.psg.sempv2.action.model.MsgVpnClientsResponse;
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
import com.solace.psg.sempv2.action.model.MsgVpnMqttSessionClearStats;
import com.solace.psg.sempv2.action.model.MsgVpnMqttSessionResponse;
import com.solace.psg.sempv2.action.model.MsgVpnMqttSessionsResponse;
import com.solace.psg.sempv2.action.model.MsgVpnQueueCancelReplay;
import com.solace.psg.sempv2.action.model.MsgVpnQueueClearStats;
import com.solace.psg.sempv2.action.model.MsgVpnQueueMsgDelete;
import com.solace.psg.sempv2.action.model.MsgVpnQueueMsgResponse;
import com.solace.psg.sempv2.action.model.MsgVpnQueueMsgsResponse;
import com.solace.psg.sempv2.action.model.MsgVpnQueueResponse;
import com.solace.psg.sempv2.action.model.MsgVpnQueueStartReplay;
import com.solace.psg.sempv2.action.model.MsgVpnQueuesResponse;
import com.solace.psg.sempv2.action.model.MsgVpnReplayLogResponse;
import com.solace.psg.sempv2.action.model.MsgVpnReplayLogTrimLoggedMsgs;
import com.solace.psg.sempv2.action.model.MsgVpnReplayLogsResponse;
import com.solace.psg.sempv2.action.model.MsgVpnResponse;
import com.solace.psg.sempv2.action.model.MsgVpnRestDeliveryPointResponse;
import com.solace.psg.sempv2.action.model.MsgVpnRestDeliveryPointRestConsumerClearStats;
import com.solace.psg.sempv2.action.model.MsgVpnRestDeliveryPointRestConsumerResponse;
import com.solace.psg.sempv2.action.model.MsgVpnRestDeliveryPointRestConsumersResponse;
import com.solace.psg.sempv2.action.model.MsgVpnRestDeliveryPointsResponse;
import com.solace.psg.sempv2.action.model.MsgVpnTopicEndpointCancelReplay;
import com.solace.psg.sempv2.action.model.MsgVpnTopicEndpointClearStats;
import com.solace.psg.sempv2.action.model.MsgVpnTopicEndpointMsgDelete;
import com.solace.psg.sempv2.action.model.MsgVpnTopicEndpointMsgResponse;
import com.solace.psg.sempv2.action.model.MsgVpnTopicEndpointMsgsResponse;
import com.solace.psg.sempv2.action.model.MsgVpnTopicEndpointResponse;
import com.solace.psg.sempv2.action.model.MsgVpnTopicEndpointStartReplay;
import com.solace.psg.sempv2.action.model.MsgVpnTopicEndpointsResponse;
import com.solace.psg.sempv2.action.model.MsgVpnTransactionCommit;
import com.solace.psg.sempv2.action.model.MsgVpnTransactionDelete;
import com.solace.psg.sempv2.action.model.MsgVpnTransactionResponse;
import com.solace.psg.sempv2.action.model.MsgVpnTransactionRollback;
import com.solace.psg.sempv2.action.model.MsgVpnTransactionsResponse;
import com.solace.psg.sempv2.action.model.MsgVpnsResponse;
import com.solace.psg.sempv2.action.model.SempMetaOnlyResponse;
import org.junit.Test;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for AllApi
 */
@Ignore
public class AllApiTest {

    private final AllApi api = new AllApi();

    /**
     * Refresh the CRL file for the Certificate Authority.
     *
     * Refresh the CRL file for the Certificate Authority.    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doCertAuthorityRefreshCrlTest() throws ApiException {
        CertAuthorityRefreshCrl body = null;
        String certAuthorityName = null;
        SempMetaOnlyResponse response = api.doCertAuthorityRefreshCrl(body, certAuthorityName);

        // TODO: test validations
    }
    /**
     * Clear the statistics for the OAuth Provider.
     *
     * Clear the statistics for the OAuth Provider.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnAuthenticationOauthProviderClearStatsTest() throws ApiException {
        MsgVpnAuthenticationOauthProviderClearStats body = null;
        String msgVpnName = null;
        String oauthProviderName = null;
        SempMetaOnlyResponse response = api.doMsgVpnAuthenticationOauthProviderClearStats(body, msgVpnName, oauthProviderName);

        // TODO: test validations
    }
    /**
     * Clear an event for the Bridge so it can be generated anew.
     *
     * Clear an event for the Bridge so it can be generated anew.   Attribute|Required|Deprecated :---|:---:|:---: eventName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnBridgeClearEventTest() throws ApiException {
        MsgVpnBridgeClearEvent body = null;
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        SempMetaOnlyResponse response = api.doMsgVpnBridgeClearEvent(body, msgVpnName, bridgeName, bridgeVirtualRouter);

        // TODO: test validations
    }
    /**
     * Clear the statistics for the Bridge.
     *
     * Clear the statistics for the Bridge.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnBridgeClearStatsTest() throws ApiException {
        MsgVpnBridgeClearStats body = null;
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        SempMetaOnlyResponse response = api.doMsgVpnBridgeClearStats(body, msgVpnName, bridgeName, bridgeVirtualRouter);

        // TODO: test validations
    }
    /**
     * Disconnect the Bridge.
     *
     * Disconnect the Bridge.    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnBridgeDisconnectTest() throws ApiException {
        MsgVpnBridgeDisconnect body = null;
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        SempMetaOnlyResponse response = api.doMsgVpnBridgeDisconnect(body, msgVpnName, bridgeName, bridgeVirtualRouter);

        // TODO: test validations
    }
    /**
     * Clear the message spool statistics for the Message VPN.
     *
     * Clear the message spool statistics for the Message VPN.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnClearMsgSpoolStatsTest() throws ApiException {
        MsgVpnClearMsgSpoolStats body = null;
        String msgVpnName = null;
        SempMetaOnlyResponse response = api.doMsgVpnClearMsgSpoolStats(body, msgVpnName);

        // TODO: test validations
    }
    /**
     * Clear the replication statistics for the Message VPN.
     *
     * Clear the replication statistics for the Message VPN.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnClearReplicationStatsTest() throws ApiException {
        MsgVpnClearReplicationStats body = null;
        String msgVpnName = null;
        SempMetaOnlyResponse response = api.doMsgVpnClearReplicationStats(body, msgVpnName);

        // TODO: test validations
    }
    /**
     * Clear the service statistics for the Message VPN.
     *
     * Clear the service statistics for the Message VPN.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnClearServiceStatsTest() throws ApiException {
        MsgVpnClearServiceStats body = null;
        String msgVpnName = null;
        SempMetaOnlyResponse response = api.doMsgVpnClearServiceStats(body, msgVpnName);

        // TODO: test validations
    }
    /**
     * Clear the client statistics for the Message VPN.
     *
     * Clear the client statistics for the Message VPN.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnClearStatsTest() throws ApiException {
        MsgVpnClearStats body = null;
        String msgVpnName = null;
        SempMetaOnlyResponse response = api.doMsgVpnClearStats(body, msgVpnName);

        // TODO: test validations
    }
    /**
     * Clear an event for the Client so it can be generated anew.
     *
     * Clear an event for the Client so it can be generated anew.   Attribute|Required|Deprecated :---|:---:|:---: eventName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.12.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnClientClearEventTest() throws ApiException {
        MsgVpnClientClearEvent body = null;
        String msgVpnName = null;
        String clientName = null;
        SempMetaOnlyResponse response = api.doMsgVpnClientClearEvent(body, msgVpnName, clientName);

        // TODO: test validations
    }
    /**
     * Clear the statistics for the Client.
     *
     * Clear the statistics for the Client.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnClientClearStatsTest() throws ApiException {
        MsgVpnClientClearStats body = null;
        String msgVpnName = null;
        String clientName = null;
        SempMetaOnlyResponse response = api.doMsgVpnClientClearStats(body, msgVpnName, clientName);

        // TODO: test validations
    }
    /**
     * Disconnect the Client.
     *
     * Disconnect the Client.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnClientDisconnectTest() throws ApiException {
        MsgVpnClientDisconnect body = null;
        String msgVpnName = null;
        String clientName = null;
        SempMetaOnlyResponse response = api.doMsgVpnClientDisconnect(body, msgVpnName, clientName);

        // TODO: test validations
    }
    /**
     * Delete the Transacted Session.
     *
     * Delete the Transacted Session.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnClientTransactedSessionDeleteTest() throws ApiException {
        MsgVpnClientTransactedSessionDelete body = null;
        String msgVpnName = null;
        String clientName = null;
        String sessionName = null;
        SempMetaOnlyResponse response = api.doMsgVpnClientTransactedSessionDelete(body, msgVpnName, clientName, sessionName);

        // TODO: test validations
    }
    /**
     * Backup cached messages of the Cache Instance to disk.
     *
     * Backup cached messages of the Cache Instance to disk.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgsTest() throws ApiException {
        MsgVpnDistributedCacheClusterInstanceBackupCachedMsgs body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String instanceName = null;
        SempMetaOnlyResponse response = api.doMsgVpnDistributedCacheClusterInstanceBackupCachedMsgs(body, msgVpnName, cacheName, clusterName, instanceName);

        // TODO: test validations
    }
    /**
     * Cancel the backup of cached messages from the Cache Instance.
     *
     * Cancel the backup of cached messages from the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgsTest() throws ApiException {
        MsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String instanceName = null;
        SempMetaOnlyResponse response = api.doMsgVpnDistributedCacheClusterInstanceCancelBackupCachedMsgs(body, msgVpnName, cacheName, clusterName, instanceName);

        // TODO: test validations
    }
    /**
     * Cancel the restore of cached messages to the Cache Instance.
     *
     * Cancel the restore of cached messages to the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgsTest() throws ApiException {
        MsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String instanceName = null;
        SempMetaOnlyResponse response = api.doMsgVpnDistributedCacheClusterInstanceCancelRestoreCachedMsgs(body, msgVpnName, cacheName, clusterName, instanceName);

        // TODO: test validations
    }
    /**
     * Clear an event for the Cache Instance so it can be generated anew.
     *
     * Clear an event for the Cache Instance so it can be generated anew.   Attribute|Required|Deprecated :---|:---:|:---: eventName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnDistributedCacheClusterInstanceClearEventTest() throws ApiException {
        MsgVpnDistributedCacheClusterInstanceClearEvent body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String instanceName = null;
        SempMetaOnlyResponse response = api.doMsgVpnDistributedCacheClusterInstanceClearEvent(body, msgVpnName, cacheName, clusterName, instanceName);

        // TODO: test validations
    }
    /**
     * Clear the statistics for the Cache Instance.
     *
     * Clear the statistics for the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnDistributedCacheClusterInstanceClearStatsTest() throws ApiException {
        MsgVpnDistributedCacheClusterInstanceClearStats body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String instanceName = null;
        SempMetaOnlyResponse response = api.doMsgVpnDistributedCacheClusterInstanceClearStats(body, msgVpnName, cacheName, clusterName, instanceName);

        // TODO: test validations
    }
    /**
     * Delete messages covered by the given topic in the Cache Instance.
     *
     * Delete messages covered by the given topic in the Cache Instance.   Attribute|Required|Deprecated :---|:---:|:---: topic|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnDistributedCacheClusterInstanceDeleteMsgsTest() throws ApiException {
        MsgVpnDistributedCacheClusterInstanceDeleteMsgs body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String instanceName = null;
        SempMetaOnlyResponse response = api.doMsgVpnDistributedCacheClusterInstanceDeleteMsgs(body, msgVpnName, cacheName, clusterName, instanceName);

        // TODO: test validations
    }
    /**
     * Restore cached messages for the Cache Instance from disk.
     *
     * Restore cached messages for the Cache Instance from disk.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgsTest() throws ApiException {
        MsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String instanceName = null;
        SempMetaOnlyResponse response = api.doMsgVpnDistributedCacheClusterInstanceRestoreCachedMsgs(body, msgVpnName, cacheName, clusterName, instanceName);

        // TODO: test validations
    }
    /**
     * Start the Cache Instance.
     *
     * Start the Cache Instance.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnDistributedCacheClusterInstanceStartTest() throws ApiException {
        MsgVpnDistributedCacheClusterInstanceStart body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String instanceName = null;
        SempMetaOnlyResponse response = api.doMsgVpnDistributedCacheClusterInstanceStart(body, msgVpnName, cacheName, clusterName, instanceName);

        // TODO: test validations
    }
    /**
     * Clear the statistics for the MQTT Session.
     *
     * Clear the statistics for the MQTT Session.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnMqttSessionClearStatsTest() throws ApiException {
        MsgVpnMqttSessionClearStats body = null;
        String msgVpnName = null;
        String mqttSessionClientId = null;
        String mqttSessionVirtualRouter = null;
        SempMetaOnlyResponse response = api.doMsgVpnMqttSessionClearStats(body, msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter);

        // TODO: test validations
    }
    /**
     * Cancel the replay of messages to the Queue.
     *
     * Cancel the replay of messages to the Queue.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnQueueCancelReplayTest() throws ApiException {
        MsgVpnQueueCancelReplay body = null;
        String msgVpnName = null;
        String queueName = null;
        SempMetaOnlyResponse response = api.doMsgVpnQueueCancelReplay(body, msgVpnName, queueName);

        // TODO: test validations
    }
    /**
     * Clear the statistics for the Queue.
     *
     * Clear the statistics for the Queue.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnQueueClearStatsTest() throws ApiException {
        MsgVpnQueueClearStats body = null;
        String msgVpnName = null;
        String queueName = null;
        SempMetaOnlyResponse response = api.doMsgVpnQueueClearStats(body, msgVpnName, queueName);

        // TODO: test validations
    }
    /**
     * Delete the Message from the Queue.
     *
     * Delete the Message from the Queue.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnQueueMsgDeleteTest() throws ApiException {
        MsgVpnQueueMsgDelete body = null;
        String msgVpnName = null;
        String queueName = null;
        String msgId = null;
        SempMetaOnlyResponse response = api.doMsgVpnQueueMsgDelete(body, msgVpnName, queueName, msgId);

        // TODO: test validations
    }
    /**
     * Start the replay of messages to the Queue.
     *
     * Start the replay of messages to the Queue.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnQueueStartReplayTest() throws ApiException {
        MsgVpnQueueStartReplay body = null;
        String msgVpnName = null;
        String queueName = null;
        SempMetaOnlyResponse response = api.doMsgVpnQueueStartReplay(body, msgVpnName, queueName);

        // TODO: test validations
    }
    /**
     * Trim (delete) messages from the Replay Log.
     *
     * Trim (delete) messages from the Replay Log.   Attribute|Required|Deprecated :---|:---:|:---: olderThanTime|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnReplayLogTrimLoggedMsgsTest() throws ApiException {
        MsgVpnReplayLogTrimLoggedMsgs body = null;
        String msgVpnName = null;
        String replayLogName = null;
        SempMetaOnlyResponse response = api.doMsgVpnReplayLogTrimLoggedMsgs(body, msgVpnName, replayLogName);

        // TODO: test validations
    }
    /**
     * Clear the statistics for the REST Consumer.
     *
     * Clear the statistics for the REST Consumer.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnRestDeliveryPointRestConsumerClearStatsTest() throws ApiException {
        MsgVpnRestDeliveryPointRestConsumerClearStats body = null;
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        SempMetaOnlyResponse response = api.doMsgVpnRestDeliveryPointRestConsumerClearStats(body, msgVpnName, restDeliveryPointName, restConsumerName);

        // TODO: test validations
    }
    /**
     * Cancel the replay of messages to the Topic Endpoint.
     *
     * Cancel the replay of messages to the Topic Endpoint.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnTopicEndpointCancelReplayTest() throws ApiException {
        MsgVpnTopicEndpointCancelReplay body = null;
        String msgVpnName = null;
        String topicEndpointName = null;
        SempMetaOnlyResponse response = api.doMsgVpnTopicEndpointCancelReplay(body, msgVpnName, topicEndpointName);

        // TODO: test validations
    }
    /**
     * Clear the statistics for the Topic Endpoint.
     *
     * Clear the statistics for the Topic Endpoint.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnTopicEndpointClearStatsTest() throws ApiException {
        MsgVpnTopicEndpointClearStats body = null;
        String msgVpnName = null;
        String topicEndpointName = null;
        SempMetaOnlyResponse response = api.doMsgVpnTopicEndpointClearStats(body, msgVpnName, topicEndpointName);

        // TODO: test validations
    }
    /**
     * Delete the Message from the Topic Endpoint.
     *
     * Delete the Message from the Topic Endpoint.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnTopicEndpointMsgDeleteTest() throws ApiException {
        MsgVpnTopicEndpointMsgDelete body = null;
        String msgVpnName = null;
        String topicEndpointName = null;
        String msgId = null;
        SempMetaOnlyResponse response = api.doMsgVpnTopicEndpointMsgDelete(body, msgVpnName, topicEndpointName, msgId);

        // TODO: test validations
    }
    /**
     * Start the replay of messages to the Topic Endpoint.
     *
     * Start the replay of messages to the Topic Endpoint.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnTopicEndpointStartReplayTest() throws ApiException {
        MsgVpnTopicEndpointStartReplay body = null;
        String msgVpnName = null;
        String topicEndpointName = null;
        SempMetaOnlyResponse response = api.doMsgVpnTopicEndpointStartReplay(body, msgVpnName, topicEndpointName);

        // TODO: test validations
    }
    /**
     * Commit the Transaction.
     *
     * Commit the Transaction.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.12.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnTransactionCommitTest() throws ApiException {
        MsgVpnTransactionCommit body = null;
        String msgVpnName = null;
        String xid = null;
        SempMetaOnlyResponse response = api.doMsgVpnTransactionCommit(body, msgVpnName, xid);

        // TODO: test validations
    }
    /**
     * Delete the Transaction.
     *
     * Delete the Transaction.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.12.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnTransactionDeleteTest() throws ApiException {
        MsgVpnTransactionDelete body = null;
        String msgVpnName = null;
        String xid = null;
        SempMetaOnlyResponse response = api.doMsgVpnTransactionDelete(body, msgVpnName, xid);

        // TODO: test validations
    }
    /**
     * Rollback the Transaction.
     *
     * Rollback the Transaction.    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.12.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void doMsgVpnTransactionRollbackTest() throws ApiException {
        MsgVpnTransactionRollback body = null;
        String msgVpnName = null;
        String xid = null;
        SempMetaOnlyResponse response = api.doMsgVpnTransactionRollback(body, msgVpnName, xid);

        // TODO: test validations
    }
    /**
     * Get an About object.
     *
     * Get an About object.  This provides metadata about the SEMP API, such as the version of the API supported by the broker.    A SEMP client authorized with a minimum access scope/level of \&quot;global/none\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getAboutTest() throws ApiException {
        List<String> select = null;
        AboutResponse response = api.getAbout(select);

        // TODO: test validations
    }
    /**
     * Get an API Description object.
     *
     * Get an API Description object. The API Description object provides metadata about the SEMP API.  A SEMP client authorized with a minimum access scope/level of \&quot;global/none\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getAboutApiTest() throws ApiException {
        AboutApiResponse response = api.getAboutApi();

        // TODO: test validations
    }
    /**
     * Get a User object.
     *
     * Get a User object.  This provides information about the access level for the username used to access the SEMP API.    A SEMP client authorized with a minimum access scope/level of \&quot;global/none\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getAboutUserTest() throws ApiException {
        List<String> select = null;
        AboutUserResponse response = api.getAboutUser(select);

        // TODO: test validations
    }
    /**
     * Get a User Message VPN object.
     *
     * Get a User Message VPN object.  This provides information about the Message VPN access level for the username used to access the SEMP API.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/none\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getAboutUserMsgVpnTest() throws ApiException {
        String msgVpnName = null;
        List<String> select = null;
        AboutUserMsgVpnResponse response = api.getAboutUserMsgVpn(msgVpnName, select);

        // TODO: test validations
    }
    /**
     * Get a list of User Message VPN objects.
     *
     * Get a list of User Message VPN objects.  This provides information about the Message VPN access level for the username used to access the SEMP API.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/none\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getAboutUserMsgVpnsTest() throws ApiException {
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        AboutUserMsgVpnsResponse response = api.getAboutUserMsgVpns(count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a Broker object.
     *
     * Get a Broker object.  This object contains global configuration for the message broker.    A SEMP client authorized with a minimum access scope/level of \&quot;global/none\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getBrokerTest() throws ApiException {
        List<String> select = null;
        BrokerResponse response = api.getBroker(select);

        // TODO: test validations
    }
    /**
     * Get a list of Certificate Authority objects.
     *
     * Get a list of Certificate Authority objects.  Clients can authenticate with the message broker over TLS by presenting a valid client certificate. The message broker authenticates the client certificate by constructing a full certificate chain (from the client certificate to intermediate CAs to a configured root CA). The intermediate CAs in this chain can be provided by the client, or configured in the message broker. The root CA must be configured on the message broker.   Attribute|Identifying|Deprecated :---|:---:|:---: certAuthorityName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getCertAuthoritiesTest() throws ApiException {
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        CertAuthoritiesResponse response = api.getCertAuthorities(count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a Certificate Authority object.
     *
     * Get a Certificate Authority object.  Clients can authenticate with the message broker over TLS by presenting a valid client certificate. The message broker authenticates the client certificate by constructing a full certificate chain (from the client certificate to intermediate CAs to a configured root CA). The intermediate CAs in this chain can be provided by the client, or configured in the message broker. The root CA must be configured on the message broker.   Attribute|Identifying|Deprecated :---|:---:|:---: certAuthorityName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-only\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getCertAuthorityTest() throws ApiException {
        String certAuthorityName = null;
        List<String> select = null;
        CertAuthorityResponse response = api.getCertAuthority(certAuthorityName, select);

        // TODO: test validations
    }
    /**
     * Get a Message VPN object.
     *
     * Get a Message VPN object.  Message VPNs (Virtual Private Networks) allow for the segregation of topic space and clients. They also group clients connecting to a network of message brokers, such that messages published within a particular group are only visible to that group&#x27;s clients.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnTest() throws ApiException {
        String msgVpnName = null;
        List<String> select = null;
        MsgVpnResponse response = api.getMsgVpn(msgVpnName, select);

        // TODO: test validations
    }
    /**
     * Get an OAuth Provider object.
     *
     * Get an OAuth Provider object.  OAuth Providers contain information about the issuer of an OAuth token that is needed to validate the token and derive a client username from it.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| oauthProviderName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAuthenticationOauthProviderTest() throws ApiException {
        String msgVpnName = null;
        String oauthProviderName = null;
        List<String> select = null;
        MsgVpnAuthenticationOauthProviderResponse response = api.getMsgVpnAuthenticationOauthProvider(msgVpnName, oauthProviderName, select);

        // TODO: test validations
    }
    /**
     * Get a list of OAuth Provider objects.
     *
     * Get a list of OAuth Provider objects.  OAuth Providers contain information about the issuer of an OAuth token that is needed to validate the token and derive a client username from it.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| oauthProviderName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAuthenticationOauthProvidersTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnAuthenticationOauthProvidersResponse response = api.getMsgVpnAuthenticationOauthProviders(msgVpnName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a Bridge object.
     *
     * Get a Bridge object.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnBridgeTest() throws ApiException {
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        List<String> select = null;
        MsgVpnBridgeResponse response = api.getMsgVpnBridge(msgVpnName, bridgeName, bridgeVirtualRouter, select);

        // TODO: test validations
    }
    /**
     * Get a list of Bridge objects.
     *
     * Get a list of Bridge objects.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Deprecated :---|:---:|:---: bridgeName|x| bridgeVirtualRouter|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnBridgesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnBridgesResponse response = api.getMsgVpnBridges(msgVpnName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a Client object.
     *
     * Get a Client object.  Applications or devices that connect to message brokers to send and/or receive messages are represented as Clients.   Attribute|Identifying|Deprecated :---|:---:|:---: clientName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnClientTest() throws ApiException {
        String msgVpnName = null;
        String clientName = null;
        List<String> select = null;
        MsgVpnClientResponse response = api.getMsgVpnClient(msgVpnName, clientName, select);

        // TODO: test validations
    }
    /**
     * Get a Client Transacted Session object.
     *
     * Get a Client Transacted Session object.  Transacted Sessions enable clients to group multiple message send and/or receive operations together in single, atomic units known as local transactions.   Attribute|Identifying|Deprecated :---|:---:|:---: clientName|x| msgVpnName|x| sessionName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnClientTransactedSessionTest() throws ApiException {
        String msgVpnName = null;
        String clientName = null;
        String sessionName = null;
        List<String> select = null;
        MsgVpnClientTransactedSessionResponse response = api.getMsgVpnClientTransactedSession(msgVpnName, clientName, sessionName, select);

        // TODO: test validations
    }
    /**
     * Get a list of Client Transacted Session objects.
     *
     * Get a list of Client Transacted Session objects.  Transacted Sessions enable clients to group multiple message send and/or receive operations together in single, atomic units known as local transactions.   Attribute|Identifying|Deprecated :---|:---:|:---: clientName|x| msgVpnName|x| sessionName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnClientTransactedSessionsTest() throws ApiException {
        String msgVpnName = null;
        String clientName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnClientTransactedSessionsResponse response = api.getMsgVpnClientTransactedSessions(msgVpnName, clientName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Client objects.
     *
     * Get a list of Client objects.  Applications or devices that connect to message brokers to send and/or receive messages are represented as Clients.   Attribute|Identifying|Deprecated :---|:---:|:---: clientName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnClientsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnClientsResponse response = api.getMsgVpnClients(msgVpnName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a Distributed Cache object.
     *
     * Get a Distributed Cache object.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        List<String> select = null;
        MsgVpnDistributedCacheResponse response = api.getMsgVpnDistributedCache(msgVpnName, cacheName, select);

        // TODO: test validations
    }
    /**
     * Get a Cache Cluster object.
     *
     * Get a Cache Cluster object.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheClusterTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterResponse response = api.getMsgVpnDistributedCacheCluster(msgVpnName, cacheName, clusterName, select);

        // TODO: test validations
    }
    /**
     * Get a Cache Instance object.
     *
     * Get a Cache Instance object.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| instanceName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheClusterInstanceTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String instanceName = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterInstanceResponse response = api.getMsgVpnDistributedCacheClusterInstance(msgVpnName, cacheName, clusterName, instanceName, select);

        // TODO: test validations
    }
    /**
     * Get a list of Cache Instance objects.
     *
     * Get a list of Cache Instance objects.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| instanceName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheClusterInstancesTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterInstancesResponse response = api.getMsgVpnDistributedCacheClusterInstances(msgVpnName, cacheName, clusterName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Cache Cluster objects.
     *
     * Get a list of Cache Cluster objects.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| clusterName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheClustersTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnDistributedCacheClustersResponse response = api.getMsgVpnDistributedCacheClusters(msgVpnName, cacheName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Distributed Cache objects.
     *
     * Get a list of Distributed Cache objects.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Deprecated :---|:---:|:---: cacheName|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCachesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnDistributedCachesResponse response = api.getMsgVpnDistributedCaches(msgVpnName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get an MQTT Session object.
     *
     * Get an MQTT Session object.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#x27;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Deprecated :---|:---:|:---: mqttSessionClientId|x| mqttSessionVirtualRouter|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnMqttSessionTest() throws ApiException {
        String msgVpnName = null;
        String mqttSessionClientId = null;
        String mqttSessionVirtualRouter = null;
        List<String> select = null;
        MsgVpnMqttSessionResponse response = api.getMsgVpnMqttSession(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, select);

        // TODO: test validations
    }
    /**
     * Get a list of MQTT Session objects.
     *
     * Get a list of MQTT Session objects.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#x27;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Deprecated :---|:---:|:---: mqttSessionClientId|x| mqttSessionVirtualRouter|x| msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnMqttSessionsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnMqttSessionsResponse response = api.getMsgVpnMqttSessions(msgVpnName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a Queue object.
     *
     * Get a Queue object.  A Queue acts as both a destination that clients can publish messages to, and as an endpoint that clients can bind consumers to and consume messages from.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| queueName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnQueueTest() throws ApiException {
        String msgVpnName = null;
        String queueName = null;
        List<String> select = null;
        MsgVpnQueueResponse response = api.getMsgVpnQueue(msgVpnName, queueName, select);

        // TODO: test validations
    }
    /**
     * Get a Queue Message object.
     *
     * Get a Queue Message object.  A Queue Message is a packet of information sent from producers to consumers using the Queue.   Attribute|Identifying|Deprecated :---|:---:|:---: msgId|x| msgVpnName|x| queueName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnQueueMsgTest() throws ApiException {
        String msgVpnName = null;
        String queueName = null;
        String msgId = null;
        List<String> select = null;
        MsgVpnQueueMsgResponse response = api.getMsgVpnQueueMsg(msgVpnName, queueName, msgId, select);

        // TODO: test validations
    }
    /**
     * Get a list of Queue Message objects.
     *
     * Get a list of Queue Message objects.  A Queue Message is a packet of information sent from producers to consumers using the Queue.   Attribute|Identifying|Deprecated :---|:---:|:---: msgId|x| msgVpnName|x| queueName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnQueueMsgsTest() throws ApiException {
        String msgVpnName = null;
        String queueName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnQueueMsgsResponse response = api.getMsgVpnQueueMsgs(msgVpnName, queueName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Queue objects.
     *
     * Get a list of Queue objects.  A Queue acts as both a destination that clients can publish messages to, and as an endpoint that clients can bind consumers to and consume messages from.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| queueName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnQueuesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnQueuesResponse response = api.getMsgVpnQueues(msgVpnName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a Replay Log object.
     *
     * Get a Replay Log object.  When the Message Replay feature is enabled, message brokers store persistent messages in a Replay Log. These messages are kept until the log is full, after which the oldest messages are removed to free up space for new messages.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| replayLogName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnReplayLogTest() throws ApiException {
        String msgVpnName = null;
        String replayLogName = null;
        List<String> select = null;
        MsgVpnReplayLogResponse response = api.getMsgVpnReplayLog(msgVpnName, replayLogName, select);

        // TODO: test validations
    }
    /**
     * Get a list of Replay Log objects.
     *
     * Get a list of Replay Log objects.  When the Message Replay feature is enabled, message brokers store persistent messages in a Replay Log. These messages are kept until the log is full, after which the oldest messages are removed to free up space for new messages.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| replayLogName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnReplayLogsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnReplayLogsResponse response = api.getMsgVpnReplayLogs(msgVpnName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a REST Delivery Point object.
     *
     * Get a REST Delivery Point object.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| restDeliveryPointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointResponse response = api.getMsgVpnRestDeliveryPoint(msgVpnName, restDeliveryPointName, select);

        // TODO: test validations
    }
    /**
     * Get a REST Consumer object.
     *
     * Get a REST Consumer object.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| restConsumerName|x| restDeliveryPointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointRestConsumerTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumerResponse response = api.getMsgVpnRestDeliveryPointRestConsumer(msgVpnName, restDeliveryPointName, restConsumerName, select);

        // TODO: test validations
    }
    /**
     * Get a list of REST Consumer objects.
     *
     * Get a list of REST Consumer objects.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| restConsumerName|x| restDeliveryPointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointRestConsumersTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumersResponse response = api.getMsgVpnRestDeliveryPointRestConsumers(msgVpnName, restDeliveryPointName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of REST Delivery Point objects.
     *
     * Get a list of REST Delivery Point objects.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| restDeliveryPointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointsResponse response = api.getMsgVpnRestDeliveryPoints(msgVpnName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a Topic Endpoint object.
     *
     * Get a Topic Endpoint object.  A Topic Endpoint attracts messages published to a topic for which the Topic Endpoint has a matching topic subscription. The topic subscription for the Topic Endpoint is specified in the client request to bind a Flow to that Topic Endpoint. Queues are significantly more flexible than Topic Endpoints and are the recommended approach for most applications. The use of Topic Endpoints should be restricted to JMS applications.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| topicEndpointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnTopicEndpointTest() throws ApiException {
        String msgVpnName = null;
        String topicEndpointName = null;
        List<String> select = null;
        MsgVpnTopicEndpointResponse response = api.getMsgVpnTopicEndpoint(msgVpnName, topicEndpointName, select);

        // TODO: test validations
    }
    /**
     * Get a Topic Endpoint Message object.
     *
     * Get a Topic Endpoint Message object.  A Topic Endpoint Message is a packet of information sent from producers to consumers using the Topic Endpoint.   Attribute|Identifying|Deprecated :---|:---:|:---: msgId|x| msgVpnName|x| topicEndpointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnTopicEndpointMsgTest() throws ApiException {
        String msgVpnName = null;
        String topicEndpointName = null;
        String msgId = null;
        List<String> select = null;
        MsgVpnTopicEndpointMsgResponse response = api.getMsgVpnTopicEndpointMsg(msgVpnName, topicEndpointName, msgId, select);

        // TODO: test validations
    }
    /**
     * Get a list of Topic Endpoint Message objects.
     *
     * Get a list of Topic Endpoint Message objects.  A Topic Endpoint Message is a packet of information sent from producers to consumers using the Topic Endpoint.   Attribute|Identifying|Deprecated :---|:---:|:---: msgId|x| msgVpnName|x| topicEndpointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnTopicEndpointMsgsTest() throws ApiException {
        String msgVpnName = null;
        String topicEndpointName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnTopicEndpointMsgsResponse response = api.getMsgVpnTopicEndpointMsgs(msgVpnName, topicEndpointName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Topic Endpoint objects.
     *
     * Get a list of Topic Endpoint objects.  A Topic Endpoint attracts messages published to a topic for which the Topic Endpoint has a matching topic subscription. The topic subscription for the Topic Endpoint is specified in the client request to bind a Flow to that Topic Endpoint. Queues are significantly more flexible than Topic Endpoints and are the recommended approach for most applications. The use of Topic Endpoints should be restricted to JMS applications.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| topicEndpointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnTopicEndpointsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnTopicEndpointsResponse response = api.getMsgVpnTopicEndpoints(msgVpnName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a Replicated Local Transaction or XA Transaction object.
     *
     * Get a Replicated Local Transaction or XA Transaction object.  Transactions can be used to group a set of Guaranteed messages to be published or consumed or both as an atomic unit of work.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| xid|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.12.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnTransactionTest() throws ApiException {
        String msgVpnName = null;
        String xid = null;
        List<String> select = null;
        MsgVpnTransactionResponse response = api.getMsgVpnTransaction(msgVpnName, xid, select);

        // TODO: test validations
    }
    /**
     * Get a list of Replicated Local Transaction or XA Transaction objects.
     *
     * Get a list of Replicated Local Transaction or XA Transaction objects.  Transactions can be used to group a set of Guaranteed messages to be published or consumed or both as an atomic unit of work.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| xid|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.12.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnTransactionsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnTransactionsResponse response = api.getMsgVpnTransactions(msgVpnName, count, cursor, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Message VPN objects.
     *
     * Get a list of Message VPN objects.  Message VPNs (Virtual Private Networks) allow for the segregation of topic space and clients. They also group clients connecting to a network of message brokers, such that messages published within a particular group are only visible to that group&#x27;s clients.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnsTest() throws ApiException {
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnsResponse response = api.getMsgVpns(count, cursor, where, select);

        // TODO: test validations
    }
}
