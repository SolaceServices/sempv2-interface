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

import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.config.model.MsgVpn;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfile;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileClientConnectException;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileClientConnectExceptionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileClientConnectExceptionsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfilePublishException;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfilePublishExceptionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfilePublishExceptionsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfilePublishTopicException;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfilePublishTopicExceptionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfilePublishTopicExceptionsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileSubscribeException;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileSubscribeExceptionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileSubscribeExceptionsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileSubscribeShareNameException;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileSubscribeShareNameExceptionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileSubscribeShareNameExceptionsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileSubscribeTopicException;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileSubscribeTopicExceptionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfileSubscribeTopicExceptionsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAclProfilesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAuthenticationOauthProvider;
import com.solace.psg.sempv2.config.model.MsgVpnAuthenticationOauthProviderResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAuthenticationOauthProvidersResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAuthorizationGroup;
import com.solace.psg.sempv2.config.model.MsgVpnAuthorizationGroupResponse;
import com.solace.psg.sempv2.config.model.MsgVpnAuthorizationGroupsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridge;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeRemoteMsgVpn;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeRemoteMsgVpnResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeRemoteMsgVpnsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeRemoteSubscription;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeRemoteSubscriptionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeRemoteSubscriptionsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeTlsTrustedCommonName;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeTlsTrustedCommonNameResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridgeTlsTrustedCommonNamesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnBridgesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnClientProfile;
import com.solace.psg.sempv2.config.model.MsgVpnClientProfileResponse;
import com.solace.psg.sempv2.config.model.MsgVpnClientProfilesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnClientUsername;
import com.solace.psg.sempv2.config.model.MsgVpnClientUsernameResponse;
import com.solace.psg.sempv2.config.model.MsgVpnClientUsernamesResponse;
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
import com.solace.psg.sempv2.config.model.MsgVpnDmrBridge;
import com.solace.psg.sempv2.config.model.MsgVpnDmrBridgeResponse;
import com.solace.psg.sempv2.config.model.MsgVpnDmrBridgesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnJndiConnectionFactoriesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnJndiConnectionFactory;
import com.solace.psg.sempv2.config.model.MsgVpnJndiConnectionFactoryResponse;
import com.solace.psg.sempv2.config.model.MsgVpnJndiQueue;
import com.solace.psg.sempv2.config.model.MsgVpnJndiQueueResponse;
import com.solace.psg.sempv2.config.model.MsgVpnJndiQueuesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnJndiTopic;
import com.solace.psg.sempv2.config.model.MsgVpnJndiTopicResponse;
import com.solace.psg.sempv2.config.model.MsgVpnJndiTopicsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnMqttRetainCache;
import com.solace.psg.sempv2.config.model.MsgVpnMqttRetainCacheResponse;
import com.solace.psg.sempv2.config.model.MsgVpnMqttRetainCachesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnMqttSession;
import com.solace.psg.sempv2.config.model.MsgVpnMqttSessionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnMqttSessionSubscription;
import com.solace.psg.sempv2.config.model.MsgVpnMqttSessionSubscriptionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnMqttSessionSubscriptionsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnMqttSessionsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnQueue;
import com.solace.psg.sempv2.config.model.MsgVpnQueueResponse;
import com.solace.psg.sempv2.config.model.MsgVpnQueueSubscription;
import com.solace.psg.sempv2.config.model.MsgVpnQueueSubscriptionResponse;
import com.solace.psg.sempv2.config.model.MsgVpnQueueSubscriptionsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnQueueTemplate;
import com.solace.psg.sempv2.config.model.MsgVpnQueueTemplateResponse;
import com.solace.psg.sempv2.config.model.MsgVpnQueueTemplatesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnQueuesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnReplayLog;
import com.solace.psg.sempv2.config.model.MsgVpnReplayLogResponse;
import com.solace.psg.sempv2.config.model.MsgVpnReplayLogsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnReplicatedTopic;
import com.solace.psg.sempv2.config.model.MsgVpnReplicatedTopicResponse;
import com.solace.psg.sempv2.config.model.MsgVpnReplicatedTopicsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnResponse;
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
import com.solace.psg.sempv2.config.model.MsgVpnSequencedTopic;
import com.solace.psg.sempv2.config.model.MsgVpnSequencedTopicResponse;
import com.solace.psg.sempv2.config.model.MsgVpnSequencedTopicsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnTopicEndpoint;
import com.solace.psg.sempv2.config.model.MsgVpnTopicEndpointResponse;
import com.solace.psg.sempv2.config.model.MsgVpnTopicEndpointTemplate;
import com.solace.psg.sempv2.config.model.MsgVpnTopicEndpointTemplateResponse;
import com.solace.psg.sempv2.config.model.MsgVpnTopicEndpointTemplatesResponse;
import com.solace.psg.sempv2.config.model.MsgVpnTopicEndpointsResponse;
import com.solace.psg.sempv2.config.model.MsgVpnsResponse;
import com.solace.psg.sempv2.config.model.SempMetaOnlyResponse;
import org.junit.Test;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for MsgVpnApi
 */
@Ignore
public class MsgVpnApiTest {

    private final MsgVpnApi api = new MsgVpnApi();

    /**
     * Create a Message VPN object.
     *
     * Create a Message VPN object. Any attribute missing from the request will be set to its default value.  Message VPNs (Virtual Private Networks) allow for the segregation of topic space and clients. They also group clients connecting to a network of message brokers, such that messages published within a particular group are only visible to that group&#x27;s clients.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| replicationBridgeAuthenticationBasicPassword||||x||x replicationBridgeAuthenticationClientCertContent||||x||x replicationBridgeAuthenticationClientCertPassword||||x|| replicationEnabledQueueBehavior||||x|| restTlsServerCertEnforceTrustedCommonNameEnabled|||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue| MsgVpn|authenticationBasicProfileName|authenticationBasicType| MsgVpn|authorizationProfileName|authorizationType| MsgVpn|eventPublishTopicFormatMqttEnabled|eventPublishTopicFormatSmfEnabled| MsgVpn|eventPublishTopicFormatSmfEnabled|eventPublishTopicFormatMqttEnabled| MsgVpn|replicationBridgeAuthenticationBasicClientUsername|replicationBridgeAuthenticationBasicPassword| MsgVpn|replicationBridgeAuthenticationBasicPassword|replicationBridgeAuthenticationBasicClientUsername| MsgVpn|replicationBridgeAuthenticationClientCertPassword|replicationBridgeAuthenticationClientCertContent| MsgVpn|replicationEnabledQueueBehavior|replicationEnabled|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnTest() throws ApiException {
        MsgVpn body = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnResponse response = api.createMsgVpn(body, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create an ACL Profile object.
     *
     * Create an ACL Profile object. Any attribute missing from the request will be set to its default value.  An ACL Profile controls whether an authenticated client is permitted to establish a connection with the message broker or permitted to publish and subscribe to specific topics.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName|x|x|||| msgVpnName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnAclProfileTest() throws ApiException {
        MsgVpnAclProfile body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfileResponse response = api.createMsgVpnAclProfile(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Client Connect Exception object.
     *
     * Create a Client Connect Exception object. Any attribute missing from the request will be set to its default value.  A Client Connect Exception is an exception to the default action to take when a client using the ACL Profile connects to the Message VPN. Exceptions must be expressed as an IP address/netmask in CIDR form.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName|x||x||| clientConnectExceptionAddress|x|x|||| msgVpnName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnAclProfileClientConnectExceptionTest() throws ApiException {
        MsgVpnAclProfileClientConnectException body = null;
        String msgVpnName = null;
        String aclProfileName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfileClientConnectExceptionResponse response = api.createMsgVpnAclProfileClientConnectException(body, msgVpnName, aclProfileName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Publish Topic Exception object.
     *
     * Create a Publish Topic Exception object. Any attribute missing from the request will be set to its default value.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName|x||x||x| msgVpnName|x||x||x| publishExceptionTopic|x|x|||x| topicSyntax|x|x|||x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by publishTopicExceptions.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnAclProfilePublishExceptionTest() throws ApiException {
        MsgVpnAclProfilePublishException body = null;
        String msgVpnName = null;
        String aclProfileName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfilePublishExceptionResponse response = api.createMsgVpnAclProfilePublishException(body, msgVpnName, aclProfileName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Publish Topic Exception object.
     *
     * Create a Publish Topic Exception object. Any attribute missing from the request will be set to its default value.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName|x||x||| msgVpnName|x||x||| publishTopicException|x|x|||| publishTopicExceptionSyntax|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnAclProfilePublishTopicExceptionTest() throws ApiException {
        MsgVpnAclProfilePublishTopicException body = null;
        String msgVpnName = null;
        String aclProfileName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfilePublishTopicExceptionResponse response = api.createMsgVpnAclProfilePublishTopicException(body, msgVpnName, aclProfileName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Subscribe Topic Exception object.
     *
     * Create a Subscribe Topic Exception object. Any attribute missing from the request will be set to its default value.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName|x||x||x| msgVpnName|x||x||x| subscribeExceptionTopic|x|x|||x| topicSyntax|x|x|||x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by subscribeTopicExceptions.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnAclProfileSubscribeExceptionTest() throws ApiException {
        MsgVpnAclProfileSubscribeException body = null;
        String msgVpnName = null;
        String aclProfileName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfileSubscribeExceptionResponse response = api.createMsgVpnAclProfileSubscribeException(body, msgVpnName, aclProfileName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Subscribe Share Name Exception object.
     *
     * Create a Subscribe Share Name Exception object. Any attribute missing from the request will be set to its default value.  A Subscribe Share Name Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a share-name subscription in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName|x||x||| msgVpnName|x||x||| subscribeShareNameException|x|x|||| subscribeShareNameExceptionSyntax|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnAclProfileSubscribeShareNameExceptionTest() throws ApiException {
        MsgVpnAclProfileSubscribeShareNameException body = null;
        String msgVpnName = null;
        String aclProfileName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfileSubscribeShareNameExceptionResponse response = api.createMsgVpnAclProfileSubscribeShareNameException(body, msgVpnName, aclProfileName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Subscribe Topic Exception object.
     *
     * Create a Subscribe Topic Exception object. Any attribute missing from the request will be set to its default value.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName|x||x||| msgVpnName|x||x||| subscribeTopicException|x|x|||| subscribeTopicExceptionSyntax|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnAclProfileSubscribeTopicExceptionTest() throws ApiException {
        MsgVpnAclProfileSubscribeTopicException body = null;
        String msgVpnName = null;
        String aclProfileName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfileSubscribeTopicExceptionResponse response = api.createMsgVpnAclProfileSubscribeTopicException(body, msgVpnName, aclProfileName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create an OAuth Provider object.
     *
     * Create an OAuth Provider object. Any attribute missing from the request will be set to its default value.  OAuth Providers contain information about the issuer of an OAuth token that is needed to validate the token and derive a client username from it.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| oauthProviderName|x|x|||| tokenIntrospectionPassword||||x||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnAuthenticationOauthProviderTest() throws ApiException {
        MsgVpnAuthenticationOauthProvider body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAuthenticationOauthProviderResponse response = api.createMsgVpnAuthenticationOauthProvider(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create an LDAP Authorization Group object.
     *
     * Create an LDAP Authorization Group object. Any attribute missing from the request will be set to its default value.  To use client authorization groups configured on an external LDAP server to provide client authorizations, LDAP Authorization Group objects must be created on the Message VPN that match the authorization groups provisioned on the LDAP server. These objects must be configured with the client profiles and ACL profiles that will be assigned to the clients that belong to those authorization groups. A newly created group is placed at the end of the group list which is the lowest priority.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: authorizationGroupName|x|x|||| msgVpnName|x||x||| orderAfterAuthorizationGroupName||||x|| orderBeforeAuthorizationGroupName||||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnAuthorizationGroup|orderAfterAuthorizationGroupName||orderBeforeAuthorizationGroupName MsgVpnAuthorizationGroup|orderBeforeAuthorizationGroupName||orderAfterAuthorizationGroupName    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnAuthorizationGroupTest() throws ApiException {
        MsgVpnAuthorizationGroup body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAuthorizationGroupResponse response = api.createMsgVpnAuthorizationGroup(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Bridge object.
     *
     * Create a Bridge object. Any attribute missing from the request will be set to its default value.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: bridgeName|x|x|||| bridgeVirtualRouter|x|x|||| msgVpnName|x||x||| remoteAuthenticationBasicPassword||||x||x remoteAuthenticationClientCertContent||||x||x remoteAuthenticationClientCertPassword||||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnBridge|remoteAuthenticationBasicClientUsername|remoteAuthenticationBasicPassword| MsgVpnBridge|remoteAuthenticationBasicPassword|remoteAuthenticationBasicClientUsername| MsgVpnBridge|remoteAuthenticationClientCertPassword|remoteAuthenticationClientCertContent|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnBridgeTest() throws ApiException {
        MsgVpnBridge body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnBridgeResponse response = api.createMsgVpnBridge(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Remote Message VPN object.
     *
     * Create a Remote Message VPN object. Any attribute missing from the request will be set to its default value.  The Remote Message VPN is the Message VPN that the Bridge connects to.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: bridgeName|x||x||| bridgeVirtualRouter|x||x||| msgVpnName|x||x||| password||||x||x remoteMsgVpnInterface|x||||| remoteMsgVpnLocation|x|x|||| remoteMsgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnBridgeRemoteMsgVpn|clientUsername|password| MsgVpnBridgeRemoteMsgVpn|password|clientUsername|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnBridgeRemoteMsgVpnTest() throws ApiException {
        MsgVpnBridgeRemoteMsgVpn body = null;
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnBridgeRemoteMsgVpnResponse response = api.createMsgVpnBridgeRemoteMsgVpn(body, msgVpnName, bridgeName, bridgeVirtualRouter, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Remote Subscription object.
     *
     * Create a Remote Subscription object. Any attribute missing from the request will be set to its default value.  A Remote Subscription is a topic subscription used by the Message VPN Bridge to attract messages from the remote message broker.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: bridgeName|x||x||| bridgeVirtualRouter|x||x||| deliverAlwaysEnabled||x|||| msgVpnName|x||x||| remoteSubscriptionTopic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnBridgeRemoteSubscriptionTest() throws ApiException {
        MsgVpnBridgeRemoteSubscription body = null;
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnBridgeRemoteSubscriptionResponse response = api.createMsgVpnBridgeRemoteSubscription(body, msgVpnName, bridgeName, bridgeVirtualRouter, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Trusted Common Name object.
     *
     * Create a Trusted Common Name object. Any attribute missing from the request will be set to its default value.  The Trusted Common Names for the Bridge are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#x27;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: bridgeName|x||x||| bridgeVirtualRouter|x||x||| msgVpnName|x||x||| tlsTrustedCommonName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnBridgeTlsTrustedCommonNameTest() throws ApiException {
        MsgVpnBridgeTlsTrustedCommonName body = null;
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnBridgeTlsTrustedCommonNameResponse response = api.createMsgVpnBridgeTlsTrustedCommonName(body, msgVpnName, bridgeName, bridgeVirtualRouter, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Client Profile object.
     *
     * Create a Client Profile object. Any attribute missing from the request will be set to its default value.  Client Profiles are used to assign common configuration properties to clients that have been successfully authorized.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: apiQueueManagementCopyFromOnCreateName|||||x| apiTopicEndpointManagementCopyFromOnCreateName|||||x| clientProfileName|x|x|||| msgVpnName|x||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnClientProfileTest() throws ApiException {
        MsgVpnClientProfile body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnClientProfileResponse response = api.createMsgVpnClientProfile(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Client Username object.
     *
     * Create a Client Username object. Any attribute missing from the request will be set to its default value.  A client is only authorized to connect to a Message VPN that is associated with a Client Username that the client has been assigned.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: clientUsername|x|x|||| msgVpnName|x||x||| password||||x||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnClientUsernameTest() throws ApiException {
        MsgVpnClientUsername body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnClientUsernameResponse response = api.createMsgVpnClientUsername(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Distributed Cache object.
     *
     * Create a Distributed Cache object. Any attribute missing from the request will be set to its default value.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnDistributedCache|scheduledDeleteMsgDayList|scheduledDeleteMsgTimeList| MsgVpnDistributedCache|scheduledDeleteMsgTimeList|scheduledDeleteMsgDayList|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnDistributedCacheTest() throws ApiException {
        MsgVpnDistributedCache body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheResponse response = api.createMsgVpnDistributedCache(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Cache Cluster object.
     *
     * Create a Cache Cluster object. Any attribute missing from the request will be set to its default value.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x|x|||| msgVpnName|x||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent| EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnDistributedCacheClusterTest() throws ApiException {
        MsgVpnDistributedCacheCluster body = null;
        String msgVpnName = null;
        String cacheName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterResponse response = api.createMsgVpnDistributedCacheCluster(body, msgVpnName, cacheName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Home Cache Cluster object.
     *
     * Create a Home Cache Cluster object. Any attribute missing from the request will be set to its default value.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| homeClusterName|x|x|||| msgVpnName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTest() throws ApiException {
        MsgVpnDistributedCacheClusterGlobalCachingHomeCluster body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse response = api.createMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(body, msgVpnName, cacheName, clusterName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Topic Prefix object.
     *
     * Create a Topic Prefix object. Any attribute missing from the request will be set to its default value.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| homeClusterName|x||x||| msgVpnName|x||x||| topicPrefix|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixTest() throws ApiException {
        MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String homeClusterName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse response = api.createMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(body, msgVpnName, cacheName, clusterName, homeClusterName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Cache Instance object.
     *
     * Create a Cache Instance object. Any attribute missing from the request will be set to its default value.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| instanceName|x|x|||| msgVpnName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnDistributedCacheClusterInstanceTest() throws ApiException {
        MsgVpnDistributedCacheClusterInstance body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterInstanceResponse response = api.createMsgVpnDistributedCacheClusterInstance(body, msgVpnName, cacheName, clusterName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Topic object.
     *
     * Create a Topic object. Any attribute missing from the request will be set to its default value.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x||x||| clusterName|x||x||| msgVpnName|x||x||| topic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnDistributedCacheClusterTopicTest() throws ApiException {
        MsgVpnDistributedCacheClusterTopic body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterTopicResponse response = api.createMsgVpnDistributedCacheClusterTopic(body, msgVpnName, cacheName, clusterName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a DMR Bridge object.
     *
     * Create a DMR Bridge object. Any attribute missing from the request will be set to its default value.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| remoteNodeName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnDmrBridgeTest() throws ApiException {
        MsgVpnDmrBridge body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDmrBridgeResponse response = api.createMsgVpnDmrBridge(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a JNDI Connection Factory object.
     *
     * Create a JNDI Connection Factory object. Any attribute missing from the request will be set to its default value.  The message broker provides an internal JNDI store for provisioned Connection Factory objects that clients can access through JNDI lookups.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: connectionFactoryName|x|x|||| msgVpnName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnJndiConnectionFactoryTest() throws ApiException {
        MsgVpnJndiConnectionFactory body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnJndiConnectionFactoryResponse response = api.createMsgVpnJndiConnectionFactory(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a JNDI Queue object.
     *
     * Create a JNDI Queue object. Any attribute missing from the request will be set to its default value.  The message broker provides an internal JNDI store for provisioned Queue objects that clients can access through JNDI lookups.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| queueName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnJndiQueueTest() throws ApiException {
        MsgVpnJndiQueue body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnJndiQueueResponse response = api.createMsgVpnJndiQueue(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a JNDI Topic object.
     *
     * Create a JNDI Topic object. Any attribute missing from the request will be set to its default value.  The message broker provides an internal JNDI store for provisioned Topic objects that clients can access through JNDI lookups.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| topicName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnJndiTopicTest() throws ApiException {
        MsgVpnJndiTopic body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnJndiTopicResponse response = api.createMsgVpnJndiTopic(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create an MQTT Retain Cache object.
     *
     * Create an MQTT Retain Cache object. Any attribute missing from the request will be set to its default value.  Using MQTT retained messages allows publishing MQTT clients to indicate that a message must be stored for later delivery to subscribing clients when those subscribing clients add subscriptions matching the retained message&#x27;s topic. An MQTT Retain Cache processes all retained messages for a Message VPN.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnMqttRetainCacheTest() throws ApiException {
        MsgVpnMqttRetainCache body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnMqttRetainCacheResponse response = api.createMsgVpnMqttRetainCache(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create an MQTT Session object.
     *
     * Create an MQTT Session object. Any attribute missing from the request will be set to its default value.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#x27;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x|||| mqttSessionVirtualRouter|x|x|||| msgVpnName|x||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnMqttSessionTest() throws ApiException {
        MsgVpnMqttSession body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnMqttSessionResponse response = api.createMsgVpnMqttSession(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Subscription object.
     *
     * Create a Subscription object. Any attribute missing from the request will be set to its default value.  An MQTT session contains a client&#x27;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x||x||| mqttSessionVirtualRouter|x||x||| msgVpnName|x||x||| subscriptionTopic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnMqttSessionSubscriptionTest() throws ApiException {
        MsgVpnMqttSessionSubscription body = null;
        String msgVpnName = null;
        String mqttSessionClientId = null;
        String mqttSessionVirtualRouter = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnMqttSessionSubscriptionResponse response = api.createMsgVpnMqttSessionSubscription(body, msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Queue object.
     *
     * Create a Queue object. Any attribute missing from the request will be set to its default value.  A Queue acts as both a destination that clients can publish messages to, and as an endpoint that clients can bind consumers to and consume messages from.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| queueName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnQueueTest() throws ApiException {
        MsgVpnQueue body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnQueueResponse response = api.createMsgVpnQueue(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Queue Subscription object.
     *
     * Create a Queue Subscription object. Any attribute missing from the request will be set to its default value.  One or more Queue Subscriptions can be added to a durable queue so that Guaranteed messages published to matching topics are also delivered to and spooled by the queue.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| queueName|x||x||| subscriptionTopic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnQueueSubscriptionTest() throws ApiException {
        MsgVpnQueueSubscription body = null;
        String msgVpnName = null;
        String queueName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnQueueSubscriptionResponse response = api.createMsgVpnQueueSubscription(body, msgVpnName, queueName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Queue Template object.
     *
     * Create a Queue Template object. Any attribute missing from the request will be set to its default value.  A Queue Template provides a mechanism for specifying the initial state for client created queues.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| queueTemplateName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnQueueTemplateTest() throws ApiException {
        MsgVpnQueueTemplate body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnQueueTemplateResponse response = api.createMsgVpnQueueTemplate(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Replay Log object.
     *
     * Create a Replay Log object. Any attribute missing from the request will be set to its default value.  When the Message Replay feature is enabled, message brokers store persistent messages in a Replay Log. These messages are kept until the log is full, after which the oldest messages are removed to free up space for new messages.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| replayLogName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.10.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnReplayLogTest() throws ApiException {
        MsgVpnReplayLog body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnReplayLogResponse response = api.createMsgVpnReplayLog(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Replicated Topic object.
     *
     * Create a Replicated Topic object. Any attribute missing from the request will be set to its default value.  To indicate which messages should be replicated between the active and standby site, a Replicated Topic subscription must be configured on a Message VPN. If a published message matches both a replicated topic and an endpoint on the active site, then the message is replicated to the standby site.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| replicatedTopic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnReplicatedTopicTest() throws ApiException {
        MsgVpnReplicatedTopic body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnReplicatedTopicResponse response = api.createMsgVpnReplicatedTopic(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a REST Delivery Point object.
     *
     * Create a REST Delivery Point object. Any attribute missing from the request will be set to its default value.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| restDeliveryPointName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnRestDeliveryPointTest() throws ApiException {
        MsgVpnRestDeliveryPoint body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointResponse response = api.createMsgVpnRestDeliveryPoint(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Queue Binding object.
     *
     * Create a Queue Binding object. Any attribute missing from the request will be set to its default value.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| queueBindingName|x|x|||| restDeliveryPointName|x||x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnRestDeliveryPointQueueBindingTest() throws ApiException {
        MsgVpnRestDeliveryPointQueueBinding body = null;
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointQueueBindingResponse response = api.createMsgVpnRestDeliveryPointQueueBinding(body, msgVpnName, restDeliveryPointName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a REST Consumer object.
     *
     * Create a REST Consumer object. Any attribute missing from the request will be set to its default value.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: authenticationClientCertContent||||x||x authenticationClientCertPassword||||x|| authenticationHttpBasicPassword||||x||x authenticationHttpHeaderValue||||x||x msgVpnName|x||x||| restConsumerName|x|x|||| restDeliveryPointName|x||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnRestDeliveryPointRestConsumer|authenticationClientCertPassword|authenticationClientCertContent| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicPassword|authenticationHttpBasicUsername| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicUsername|authenticationHttpBasicPassword| MsgVpnRestDeliveryPointRestConsumer|remotePort|tlsEnabled| MsgVpnRestDeliveryPointRestConsumer|tlsEnabled|remotePort|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnRestDeliveryPointRestConsumerTest() throws ApiException {
        MsgVpnRestDeliveryPointRestConsumer body = null;
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumerResponse response = api.createMsgVpnRestDeliveryPointRestConsumer(body, msgVpnName, restDeliveryPointName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Trusted Common Name object.
     *
     * Create a Trusted Common Name object. Any attribute missing from the request will be set to its default value.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#x27;s server certificate.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||x| restConsumerName|x||x||x| restDeliveryPointName|x||x||x| tlsTrustedCommonName|x|x|||x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been deprecated since (will be deprecated in next SEMP version). Common Name validation has been replaced by Server Certificate Name validation.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameTest() throws ApiException {
        MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName body = null;
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse response = api.createMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(body, msgVpnName, restDeliveryPointName, restConsumerName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Sequenced Topic object.
     *
     * Create a Sequenced Topic object. Any attribute missing from the request will be set to its default value.  A Sequenced Topic is a topic subscription for which any matching messages received on the Message VPN are assigned a sequence number that is monotonically increased by a value of one per message.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| sequencedTopic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnSequencedTopicTest() throws ApiException {
        MsgVpnSequencedTopic body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnSequencedTopicResponse response = api.createMsgVpnSequencedTopic(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Topic Endpoint object.
     *
     * Create a Topic Endpoint object. Any attribute missing from the request will be set to its default value.  A Topic Endpoint attracts messages published to a topic for which the Topic Endpoint has a matching topic subscription. The topic subscription for the Topic Endpoint is specified in the client request to bind a Flow to that Topic Endpoint. Queues are significantly more flexible than Topic Endpoints and are the recommended approach for most applications. The use of Topic Endpoints should be restricted to JMS applications.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| topicEndpointName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnTopicEndpointTest() throws ApiException {
        MsgVpnTopicEndpoint body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnTopicEndpointResponse response = api.createMsgVpnTopicEndpoint(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Create a Topic Endpoint Template object.
     *
     * Create a Topic Endpoint Template object. Any attribute missing from the request will be set to its default value.  A Topic Endpoint Template provides a mechanism for specifying the initial state for client created topic endpoints.   Attribute|Identifying|Required|Read-Only|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x||x||| topicEndpointTemplateName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void createMsgVpnTopicEndpointTemplateTest() throws ApiException {
        MsgVpnTopicEndpointTemplate body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnTopicEndpointTemplateResponse response = api.createMsgVpnTopicEndpointTemplate(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Delete a Message VPN object.
     *
     * Delete a Message VPN object.  Message VPNs (Virtual Private Networks) allow for the segregation of topic space and clients. They also group clients connecting to a network of message brokers, such that messages published within a particular group are only visible to that group&#x27;s clients.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnTest() throws ApiException {
        String msgVpnName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpn(msgVpnName);

        // TODO: test validations
    }
    /**
     * Delete an ACL Profile object.
     *
     * Delete an ACL Profile object.  An ACL Profile controls whether an authenticated client is permitted to establish a connection with the message broker or permitted to publish and subscribe to specific topics.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnAclProfileTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnAclProfile(msgVpnName, aclProfileName);

        // TODO: test validations
    }
    /**
     * Delete a Client Connect Exception object.
     *
     * Delete a Client Connect Exception object.  A Client Connect Exception is an exception to the default action to take when a client using the ACL Profile connects to the Message VPN. Exceptions must be expressed as an IP address/netmask in CIDR form.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnAclProfileClientConnectExceptionTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String clientConnectExceptionAddress = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnAclProfileClientConnectException(msgVpnName, aclProfileName, clientConnectExceptionAddress);

        // TODO: test validations
    }
    /**
     * Delete a Publish Topic Exception object.
     *
     * Delete a Publish Topic Exception object.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by publishTopicExceptions.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnAclProfilePublishExceptionTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String topicSyntax = null;
        String publishExceptionTopic = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnAclProfilePublishException(msgVpnName, aclProfileName, topicSyntax, publishExceptionTopic);

        // TODO: test validations
    }
    /**
     * Delete a Publish Topic Exception object.
     *
     * Delete a Publish Topic Exception object.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnAclProfilePublishTopicExceptionTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String publishTopicExceptionSyntax = null;
        String publishTopicException = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnAclProfilePublishTopicException(msgVpnName, aclProfileName, publishTopicExceptionSyntax, publishTopicException);

        // TODO: test validations
    }
    /**
     * Delete a Subscribe Topic Exception object.
     *
     * Delete a Subscribe Topic Exception object.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by subscribeTopicExceptions.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnAclProfileSubscribeExceptionTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String topicSyntax = null;
        String subscribeExceptionTopic = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnAclProfileSubscribeException(msgVpnName, aclProfileName, topicSyntax, subscribeExceptionTopic);

        // TODO: test validations
    }
    /**
     * Delete a Subscribe Share Name Exception object.
     *
     * Delete a Subscribe Share Name Exception object.  A Subscribe Share Name Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a share-name subscription in the Message VPN. Exceptions must be expressed as a topic.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnAclProfileSubscribeShareNameExceptionTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String subscribeShareNameExceptionSyntax = null;
        String subscribeShareNameException = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnAclProfileSubscribeShareNameException(msgVpnName, aclProfileName, subscribeShareNameExceptionSyntax, subscribeShareNameException);

        // TODO: test validations
    }
    /**
     * Delete a Subscribe Topic Exception object.
     *
     * Delete a Subscribe Topic Exception object.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnAclProfileSubscribeTopicExceptionTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String subscribeTopicExceptionSyntax = null;
        String subscribeTopicException = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnAclProfileSubscribeTopicException(msgVpnName, aclProfileName, subscribeTopicExceptionSyntax, subscribeTopicException);

        // TODO: test validations
    }
    /**
     * Delete an OAuth Provider object.
     *
     * Delete an OAuth Provider object.  OAuth Providers contain information about the issuer of an OAuth token that is needed to validate the token and derive a client username from it.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnAuthenticationOauthProviderTest() throws ApiException {
        String msgVpnName = null;
        String oauthProviderName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnAuthenticationOauthProvider(msgVpnName, oauthProviderName);

        // TODO: test validations
    }
    /**
     * Delete an LDAP Authorization Group object.
     *
     * Delete an LDAP Authorization Group object.  To use client authorization groups configured on an external LDAP server to provide client authorizations, LDAP Authorization Group objects must be created on the Message VPN that match the authorization groups provisioned on the LDAP server. These objects must be configured with the client profiles and ACL profiles that will be assigned to the clients that belong to those authorization groups. A newly created group is placed at the end of the group list which is the lowest priority.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnAuthorizationGroupTest() throws ApiException {
        String msgVpnName = null;
        String authorizationGroupName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnAuthorizationGroup(msgVpnName, authorizationGroupName);

        // TODO: test validations
    }
    /**
     * Delete a Bridge object.
     *
     * Delete a Bridge object.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnBridgeTest() throws ApiException {
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnBridge(msgVpnName, bridgeName, bridgeVirtualRouter);

        // TODO: test validations
    }
    /**
     * Delete a Remote Message VPN object.
     *
     * Delete a Remote Message VPN object.  The Remote Message VPN is the Message VPN that the Bridge connects to.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnBridgeRemoteMsgVpnTest() throws ApiException {
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String remoteMsgVpnName = null;
        String remoteMsgVpnLocation = null;
        String remoteMsgVpnInterface = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnBridgeRemoteMsgVpn(msgVpnName, bridgeName, bridgeVirtualRouter, remoteMsgVpnName, remoteMsgVpnLocation, remoteMsgVpnInterface);

        // TODO: test validations
    }
    /**
     * Delete a Remote Subscription object.
     *
     * Delete a Remote Subscription object.  A Remote Subscription is a topic subscription used by the Message VPN Bridge to attract messages from the remote message broker.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnBridgeRemoteSubscriptionTest() throws ApiException {
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String remoteSubscriptionTopic = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnBridgeRemoteSubscription(msgVpnName, bridgeName, bridgeVirtualRouter, remoteSubscriptionTopic);

        // TODO: test validations
    }
    /**
     * Delete a Trusted Common Name object.
     *
     * Delete a Trusted Common Name object.  The Trusted Common Names for the Bridge are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#x27;s server certificate or client certificate, depending upon the initiator of the connection.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnBridgeTlsTrustedCommonNameTest() throws ApiException {
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String tlsTrustedCommonName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnBridgeTlsTrustedCommonName(msgVpnName, bridgeName, bridgeVirtualRouter, tlsTrustedCommonName);

        // TODO: test validations
    }
    /**
     * Delete a Client Profile object.
     *
     * Delete a Client Profile object.  Client Profiles are used to assign common configuration properties to clients that have been successfully authorized.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnClientProfileTest() throws ApiException {
        String msgVpnName = null;
        String clientProfileName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnClientProfile(msgVpnName, clientProfileName);

        // TODO: test validations
    }
    /**
     * Delete a Client Username object.
     *
     * Delete a Client Username object.  A client is only authorized to connect to a Message VPN that is associated with a Client Username that the client has been assigned.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnClientUsernameTest() throws ApiException {
        String msgVpnName = null;
        String clientUsername = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnClientUsername(msgVpnName, clientUsername);

        // TODO: test validations
    }
    /**
     * Delete a Distributed Cache object.
     *
     * Delete a Distributed Cache object.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnDistributedCacheTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnDistributedCache(msgVpnName, cacheName);

        // TODO: test validations
    }
    /**
     * Delete a Cache Cluster object.
     *
     * Delete a Cache Cluster object.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnDistributedCacheClusterTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnDistributedCacheCluster(msgVpnName, cacheName, clusterName);

        // TODO: test validations
    }
    /**
     * Delete a Home Cache Cluster object.
     *
     * Delete a Home Cache Cluster object.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String homeClusterName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(msgVpnName, cacheName, clusterName, homeClusterName);

        // TODO: test validations
    }
    /**
     * Delete a Topic Prefix object.
     *
     * Delete a Topic Prefix object.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String homeClusterName = null;
        String topicPrefix = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(msgVpnName, cacheName, clusterName, homeClusterName, topicPrefix);

        // TODO: test validations
    }
    /**
     * Delete a Cache Instance object.
     *
     * Delete a Cache Instance object.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnDistributedCacheClusterInstanceTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String instanceName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnDistributedCacheClusterInstance(msgVpnName, cacheName, clusterName, instanceName);

        // TODO: test validations
    }
    /**
     * Delete a Topic object.
     *
     * Delete a Topic object.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnDistributedCacheClusterTopicTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String topic = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnDistributedCacheClusterTopic(msgVpnName, cacheName, clusterName, topic);

        // TODO: test validations
    }
    /**
     * Delete a DMR Bridge object.
     *
     * Delete a DMR Bridge object.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnDmrBridgeTest() throws ApiException {
        String msgVpnName = null;
        String remoteNodeName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnDmrBridge(msgVpnName, remoteNodeName);

        // TODO: test validations
    }
    /**
     * Delete a JNDI Connection Factory object.
     *
     * Delete a JNDI Connection Factory object.  The message broker provides an internal JNDI store for provisioned Connection Factory objects that clients can access through JNDI lookups.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnJndiConnectionFactoryTest() throws ApiException {
        String msgVpnName = null;
        String connectionFactoryName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnJndiConnectionFactory(msgVpnName, connectionFactoryName);

        // TODO: test validations
    }
    /**
     * Delete a JNDI Queue object.
     *
     * Delete a JNDI Queue object.  The message broker provides an internal JNDI store for provisioned Queue objects that clients can access through JNDI lookups.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnJndiQueueTest() throws ApiException {
        String msgVpnName = null;
        String queueName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnJndiQueue(msgVpnName, queueName);

        // TODO: test validations
    }
    /**
     * Delete a JNDI Topic object.
     *
     * Delete a JNDI Topic object.  The message broker provides an internal JNDI store for provisioned Topic objects that clients can access through JNDI lookups.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnJndiTopicTest() throws ApiException {
        String msgVpnName = null;
        String topicName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnJndiTopic(msgVpnName, topicName);

        // TODO: test validations
    }
    /**
     * Delete an MQTT Retain Cache object.
     *
     * Delete an MQTT Retain Cache object.  Using MQTT retained messages allows publishing MQTT clients to indicate that a message must be stored for later delivery to subscribing clients when those subscribing clients add subscriptions matching the retained message&#x27;s topic. An MQTT Retain Cache processes all retained messages for a Message VPN.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnMqttRetainCacheTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnMqttRetainCache(msgVpnName, cacheName);

        // TODO: test validations
    }
    /**
     * Delete an MQTT Session object.
     *
     * Delete an MQTT Session object.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#x27;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnMqttSessionTest() throws ApiException {
        String msgVpnName = null;
        String mqttSessionClientId = null;
        String mqttSessionVirtualRouter = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnMqttSession(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter);

        // TODO: test validations
    }
    /**
     * Delete a Subscription object.
     *
     * Delete a Subscription object.  An MQTT session contains a client&#x27;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnMqttSessionSubscriptionTest() throws ApiException {
        String msgVpnName = null;
        String mqttSessionClientId = null;
        String mqttSessionVirtualRouter = null;
        String subscriptionTopic = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnMqttSessionSubscription(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic);

        // TODO: test validations
    }
    /**
     * Delete a Queue object.
     *
     * Delete a Queue object.  A Queue acts as both a destination that clients can publish messages to, and as an endpoint that clients can bind consumers to and consume messages from.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnQueueTest() throws ApiException {
        String msgVpnName = null;
        String queueName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnQueue(msgVpnName, queueName);

        // TODO: test validations
    }
    /**
     * Delete a Queue Subscription object.
     *
     * Delete a Queue Subscription object.  One or more Queue Subscriptions can be added to a durable queue so that Guaranteed messages published to matching topics are also delivered to and spooled by the queue.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnQueueSubscriptionTest() throws ApiException {
        String msgVpnName = null;
        String queueName = null;
        String subscriptionTopic = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnQueueSubscription(msgVpnName, queueName, subscriptionTopic);

        // TODO: test validations
    }
    /**
     * Delete a Queue Template object.
     *
     * Delete a Queue Template object.  A Queue Template provides a mechanism for specifying the initial state for client created queues.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnQueueTemplateTest() throws ApiException {
        String msgVpnName = null;
        String queueTemplateName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnQueueTemplate(msgVpnName, queueTemplateName);

        // TODO: test validations
    }
    /**
     * Delete a Replay Log object.
     *
     * Delete a Replay Log object.  When the Message Replay feature is enabled, message brokers store persistent messages in a Replay Log. These messages are kept until the log is full, after which the oldest messages are removed to free up space for new messages.  A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.10.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnReplayLogTest() throws ApiException {
        String msgVpnName = null;
        String replayLogName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnReplayLog(msgVpnName, replayLogName);

        // TODO: test validations
    }
    /**
     * Delete a Replicated Topic object.
     *
     * Delete a Replicated Topic object.  To indicate which messages should be replicated between the active and standby site, a Replicated Topic subscription must be configured on a Message VPN. If a published message matches both a replicated topic and an endpoint on the active site, then the message is replicated to the standby site.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnReplicatedTopicTest() throws ApiException {
        String msgVpnName = null;
        String replicatedTopic = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnReplicatedTopic(msgVpnName, replicatedTopic);

        // TODO: test validations
    }
    /**
     * Delete a REST Delivery Point object.
     *
     * Delete a REST Delivery Point object.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnRestDeliveryPointTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnRestDeliveryPoint(msgVpnName, restDeliveryPointName);

        // TODO: test validations
    }
    /**
     * Delete a Queue Binding object.
     *
     * Delete a Queue Binding object.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnRestDeliveryPointQueueBindingTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String queueBindingName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnRestDeliveryPointQueueBinding(msgVpnName, restDeliveryPointName, queueBindingName);

        // TODO: test validations
    }
    /**
     * Delete a REST Consumer object.
     *
     * Delete a REST Consumer object.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnRestDeliveryPointRestConsumerTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnRestDeliveryPointRestConsumer(msgVpnName, restDeliveryPointName, restConsumerName);

        // TODO: test validations
    }
    /**
     * Delete a Trusted Common Name object.
     *
     * Delete a Trusted Common Name object.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#x27;s server certificate.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been deprecated since (will be deprecated in next SEMP version). Common Name validation has been replaced by Server Certificate Name validation.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        String tlsTrustedCommonName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(msgVpnName, restDeliveryPointName, restConsumerName, tlsTrustedCommonName);

        // TODO: test validations
    }
    /**
     * Delete a Sequenced Topic object.
     *
     * Delete a Sequenced Topic object.  A Sequenced Topic is a topic subscription for which any matching messages received on the Message VPN are assigned a sequence number that is monotonically increased by a value of one per message.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnSequencedTopicTest() throws ApiException {
        String msgVpnName = null;
        String sequencedTopic = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnSequencedTopic(msgVpnName, sequencedTopic);

        // TODO: test validations
    }
    /**
     * Delete a Topic Endpoint object.
     *
     * Delete a Topic Endpoint object.  A Topic Endpoint attracts messages published to a topic for which the Topic Endpoint has a matching topic subscription. The topic subscription for the Topic Endpoint is specified in the client request to bind a Flow to that Topic Endpoint. Queues are significantly more flexible than Topic Endpoints and are the recommended approach for most applications. The use of Topic Endpoints should be restricted to JMS applications.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnTopicEndpointTest() throws ApiException {
        String msgVpnName = null;
        String topicEndpointName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnTopicEndpoint(msgVpnName, topicEndpointName);

        // TODO: test validations
    }
    /**
     * Delete a Topic Endpoint Template object.
     *
     * Delete a Topic Endpoint Template object.  A Topic Endpoint Template provides a mechanism for specifying the initial state for client created topic endpoints.  A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void deleteMsgVpnTopicEndpointTemplateTest() throws ApiException {
        String msgVpnName = null;
        String topicEndpointTemplateName = null;
        SempMetaOnlyResponse response = api.deleteMsgVpnTopicEndpointTemplate(msgVpnName, topicEndpointTemplateName);

        // TODO: test validations
    }
    /**
     * Get a Message VPN object.
     *
     * Get a Message VPN object.  Message VPNs (Virtual Private Networks) allow for the segregation of topic space and clients. They also group clients connecting to a network of message brokers, such that messages published within a particular group are only visible to that group&#x27;s clients.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| replicationBridgeAuthenticationBasicPassword||x||x replicationBridgeAuthenticationClientCertContent||x||x replicationBridgeAuthenticationClientCertPassword||x|| replicationEnabledQueueBehavior||x|| restTlsServerCertEnforceTrustedCommonNameEnabled|||x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnTest() throws ApiException {
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnResponse response = api.getMsgVpn(msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get an ACL Profile object.
     *
     * Get an ACL Profile object.  An ACL Profile controls whether an authenticated client is permitted to establish a connection with the message broker or permitted to publish and subscribe to specific topics.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfileTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfileResponse response = api.getMsgVpnAclProfile(msgVpnName, aclProfileName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a Client Connect Exception object.
     *
     * Get a Client Connect Exception object.  A Client Connect Exception is an exception to the default action to take when a client using the ACL Profile connects to the Message VPN. Exceptions must be expressed as an IP address/netmask in CIDR form.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||| clientConnectExceptionAddress|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfileClientConnectExceptionTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String clientConnectExceptionAddress = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfileClientConnectExceptionResponse response = api.getMsgVpnAclProfileClientConnectException(msgVpnName, aclProfileName, clientConnectExceptionAddress, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Client Connect Exception objects.
     *
     * Get a list of Client Connect Exception objects.  A Client Connect Exception is an exception to the default action to take when a client using the ACL Profile connects to the Message VPN. Exceptions must be expressed as an IP address/netmask in CIDR form.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||| clientConnectExceptionAddress|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfileClientConnectExceptionsTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnAclProfileClientConnectExceptionsResponse response = api.getMsgVpnAclProfileClientConnectExceptions(msgVpnName, aclProfileName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Publish Topic Exception object.
     *
     * Get a Publish Topic Exception object.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||x| msgVpnName|x||x| publishExceptionTopic|x||x| topicSyntax|x||x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by publishTopicExceptions.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfilePublishExceptionTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String topicSyntax = null;
        String publishExceptionTopic = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfilePublishExceptionResponse response = api.getMsgVpnAclProfilePublishException(msgVpnName, aclProfileName, topicSyntax, publishExceptionTopic, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Publish Topic Exception objects.
     *
     * Get a list of Publish Topic Exception objects.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||x| msgVpnName|x||x| publishExceptionTopic|x||x| topicSyntax|x||x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by publishTopicExceptions.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfilePublishExceptionsTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnAclProfilePublishExceptionsResponse response = api.getMsgVpnAclProfilePublishExceptions(msgVpnName, aclProfileName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Publish Topic Exception object.
     *
     * Get a Publish Topic Exception object.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||| msgVpnName|x||| publishTopicException|x||| publishTopicExceptionSyntax|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfilePublishTopicExceptionTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String publishTopicExceptionSyntax = null;
        String publishTopicException = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfilePublishTopicExceptionResponse response = api.getMsgVpnAclProfilePublishTopicException(msgVpnName, aclProfileName, publishTopicExceptionSyntax, publishTopicException, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Publish Topic Exception objects.
     *
     * Get a list of Publish Topic Exception objects.  A Publish Topic Exception is an exception to the default action to take when a client using the ACL Profile publishes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||| msgVpnName|x||| publishTopicException|x||| publishTopicExceptionSyntax|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfilePublishTopicExceptionsTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnAclProfilePublishTopicExceptionsResponse response = api.getMsgVpnAclProfilePublishTopicExceptions(msgVpnName, aclProfileName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Subscribe Topic Exception object.
     *
     * Get a Subscribe Topic Exception object.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||x| msgVpnName|x||x| subscribeExceptionTopic|x||x| topicSyntax|x||x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by subscribeTopicExceptions.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfileSubscribeExceptionTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String topicSyntax = null;
        String subscribeExceptionTopic = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfileSubscribeExceptionResponse response = api.getMsgVpnAclProfileSubscribeException(msgVpnName, aclProfileName, topicSyntax, subscribeExceptionTopic, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Subscribe Topic Exception objects.
     *
     * Get a list of Subscribe Topic Exception objects.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||x| msgVpnName|x||x| subscribeExceptionTopic|x||x| topicSyntax|x||x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since 2.14. Replaced by subscribeTopicExceptions.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfileSubscribeExceptionsTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnAclProfileSubscribeExceptionsResponse response = api.getMsgVpnAclProfileSubscribeExceptions(msgVpnName, aclProfileName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Subscribe Share Name Exception object.
     *
     * Get a Subscribe Share Name Exception object.  A Subscribe Share Name Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a share-name subscription in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||| msgVpnName|x||| subscribeShareNameException|x||| subscribeShareNameExceptionSyntax|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfileSubscribeShareNameExceptionTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String subscribeShareNameExceptionSyntax = null;
        String subscribeShareNameException = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfileSubscribeShareNameExceptionResponse response = api.getMsgVpnAclProfileSubscribeShareNameException(msgVpnName, aclProfileName, subscribeShareNameExceptionSyntax, subscribeShareNameException, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Subscribe Share Name Exception objects.
     *
     * Get a list of Subscribe Share Name Exception objects.  A Subscribe Share Name Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a share-name subscription in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||| msgVpnName|x||| subscribeShareNameException|x||| subscribeShareNameExceptionSyntax|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfileSubscribeShareNameExceptionsTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnAclProfileSubscribeShareNameExceptionsResponse response = api.getMsgVpnAclProfileSubscribeShareNameExceptions(msgVpnName, aclProfileName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Subscribe Topic Exception object.
     *
     * Get a Subscribe Topic Exception object.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||| msgVpnName|x||| subscribeTopicException|x||| subscribeTopicExceptionSyntax|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfileSubscribeTopicExceptionTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        String subscribeTopicExceptionSyntax = null;
        String subscribeTopicException = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfileSubscribeTopicExceptionResponse response = api.getMsgVpnAclProfileSubscribeTopicException(msgVpnName, aclProfileName, subscribeTopicExceptionSyntax, subscribeTopicException, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Subscribe Topic Exception objects.
     *
     * Get a list of Subscribe Topic Exception objects.  A Subscribe Topic Exception is an exception to the default action to take when a client using the ACL Profile subscribes to a topic in the Message VPN. Exceptions must be expressed as a topic.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||| msgVpnName|x||| subscribeTopicException|x||| subscribeTopicExceptionSyntax|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfileSubscribeTopicExceptionsTest() throws ApiException {
        String msgVpnName = null;
        String aclProfileName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnAclProfileSubscribeTopicExceptionsResponse response = api.getMsgVpnAclProfileSubscribeTopicExceptions(msgVpnName, aclProfileName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of ACL Profile objects.
     *
     * Get a list of ACL Profile objects.  An ACL Profile controls whether an authenticated client is permitted to establish a connection with the message broker or permitted to publish and subscribe to specific topics.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: aclProfileName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAclProfilesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnAclProfilesResponse response = api.getMsgVpnAclProfiles(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get an OAuth Provider object.
     *
     * Get an OAuth Provider object.  OAuth Providers contain information about the issuer of an OAuth token that is needed to validate the token and derive a client username from it.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| oauthProviderName|x||| tokenIntrospectionPassword||x||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAuthenticationOauthProviderTest() throws ApiException {
        String msgVpnName = null;
        String oauthProviderName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAuthenticationOauthProviderResponse response = api.getMsgVpnAuthenticationOauthProvider(msgVpnName, oauthProviderName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of OAuth Provider objects.
     *
     * Get a list of OAuth Provider objects.  OAuth Providers contain information about the issuer of an OAuth token that is needed to validate the token and derive a client username from it.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| oauthProviderName|x||| tokenIntrospectionPassword||x||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAuthenticationOauthProvidersTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnAuthenticationOauthProvidersResponse response = api.getMsgVpnAuthenticationOauthProviders(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get an LDAP Authorization Group object.
     *
     * Get an LDAP Authorization Group object.  To use client authorization groups configured on an external LDAP server to provide client authorizations, LDAP Authorization Group objects must be created on the Message VPN that match the authorization groups provisioned on the LDAP server. These objects must be configured with the client profiles and ACL profiles that will be assigned to the clients that belong to those authorization groups. A newly created group is placed at the end of the group list which is the lowest priority.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: authorizationGroupName|x||| msgVpnName|x||| orderAfterAuthorizationGroupName||x|| orderBeforeAuthorizationGroupName||x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAuthorizationGroupTest() throws ApiException {
        String msgVpnName = null;
        String authorizationGroupName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAuthorizationGroupResponse response = api.getMsgVpnAuthorizationGroup(msgVpnName, authorizationGroupName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of LDAP Authorization Group objects.
     *
     * Get a list of LDAP Authorization Group objects.  To use client authorization groups configured on an external LDAP server to provide client authorizations, LDAP Authorization Group objects must be created on the Message VPN that match the authorization groups provisioned on the LDAP server. These objects must be configured with the client profiles and ACL profiles that will be assigned to the clients that belong to those authorization groups. A newly created group is placed at the end of the group list which is the lowest priority.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: authorizationGroupName|x||| msgVpnName|x||| orderAfterAuthorizationGroupName||x|| orderBeforeAuthorizationGroupName||x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnAuthorizationGroupsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnAuthorizationGroupsResponse response = api.getMsgVpnAuthorizationGroups(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Bridge object.
     *
     * Get a Bridge object.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: bridgeName|x||| bridgeVirtualRouter|x||| msgVpnName|x||| remoteAuthenticationBasicPassword||x||x remoteAuthenticationClientCertContent||x||x remoteAuthenticationClientCertPassword||x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnBridgeTest() throws ApiException {
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnBridgeResponse response = api.getMsgVpnBridge(msgVpnName, bridgeName, bridgeVirtualRouter, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a Remote Message VPN object.
     *
     * Get a Remote Message VPN object.  The Remote Message VPN is the Message VPN that the Bridge connects to.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: bridgeName|x||| bridgeVirtualRouter|x||| msgVpnName|x||| password||x||x remoteMsgVpnInterface|x||| remoteMsgVpnLocation|x||| remoteMsgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnBridgeRemoteMsgVpnTest() throws ApiException {
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String remoteMsgVpnName = null;
        String remoteMsgVpnLocation = null;
        String remoteMsgVpnInterface = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnBridgeRemoteMsgVpnResponse response = api.getMsgVpnBridgeRemoteMsgVpn(msgVpnName, bridgeName, bridgeVirtualRouter, remoteMsgVpnName, remoteMsgVpnLocation, remoteMsgVpnInterface, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Remote Message VPN objects.
     *
     * Get a list of Remote Message VPN objects.  The Remote Message VPN is the Message VPN that the Bridge connects to.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: bridgeName|x||| bridgeVirtualRouter|x||| msgVpnName|x||| password||x||x remoteMsgVpnInterface|x||| remoteMsgVpnLocation|x||| remoteMsgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnBridgeRemoteMsgVpnsTest() throws ApiException {
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnBridgeRemoteMsgVpnsResponse response = api.getMsgVpnBridgeRemoteMsgVpns(msgVpnName, bridgeName, bridgeVirtualRouter, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Remote Subscription object.
     *
     * Get a Remote Subscription object.  A Remote Subscription is a topic subscription used by the Message VPN Bridge to attract messages from the remote message broker.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: bridgeName|x||| bridgeVirtualRouter|x||| msgVpnName|x||| remoteSubscriptionTopic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnBridgeRemoteSubscriptionTest() throws ApiException {
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String remoteSubscriptionTopic = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnBridgeRemoteSubscriptionResponse response = api.getMsgVpnBridgeRemoteSubscription(msgVpnName, bridgeName, bridgeVirtualRouter, remoteSubscriptionTopic, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Remote Subscription objects.
     *
     * Get a list of Remote Subscription objects.  A Remote Subscription is a topic subscription used by the Message VPN Bridge to attract messages from the remote message broker.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: bridgeName|x||| bridgeVirtualRouter|x||| msgVpnName|x||| remoteSubscriptionTopic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnBridgeRemoteSubscriptionsTest() throws ApiException {
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnBridgeRemoteSubscriptionsResponse response = api.getMsgVpnBridgeRemoteSubscriptions(msgVpnName, bridgeName, bridgeVirtualRouter, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Trusted Common Name object.
     *
     * Get a Trusted Common Name object.  The Trusted Common Names for the Bridge are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#x27;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: bridgeName|x||| bridgeVirtualRouter|x||| msgVpnName|x||| tlsTrustedCommonName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnBridgeTlsTrustedCommonNameTest() throws ApiException {
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String tlsTrustedCommonName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnBridgeTlsTrustedCommonNameResponse response = api.getMsgVpnBridgeTlsTrustedCommonName(msgVpnName, bridgeName, bridgeVirtualRouter, tlsTrustedCommonName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Trusted Common Name objects.
     *
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the Bridge are used by encrypted transports to verify the name in the certificate presented by the remote node. They must include the common name of the remote node&#x27;s server certificate or client certificate, depending upon the initiator of the connection.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: bridgeName|x||| bridgeVirtualRouter|x||| msgVpnName|x||| tlsTrustedCommonName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnBridgeTlsTrustedCommonNamesTest() throws ApiException {
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnBridgeTlsTrustedCommonNamesResponse response = api.getMsgVpnBridgeTlsTrustedCommonNames(msgVpnName, bridgeName, bridgeVirtualRouter, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Bridge objects.
     *
     * Get a list of Bridge objects.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: bridgeName|x||| bridgeVirtualRouter|x||| msgVpnName|x||| remoteAuthenticationBasicPassword||x||x remoteAuthenticationClientCertContent||x||x remoteAuthenticationClientCertPassword||x||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnBridgesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnBridgesResponse response = api.getMsgVpnBridges(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Client Profile object.
     *
     * Get a Client Profile object.  Client Profiles are used to assign common configuration properties to clients that have been successfully authorized.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: apiQueueManagementCopyFromOnCreateName|||x| apiTopicEndpointManagementCopyFromOnCreateName|||x| clientProfileName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnClientProfileTest() throws ApiException {
        String msgVpnName = null;
        String clientProfileName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnClientProfileResponse response = api.getMsgVpnClientProfile(msgVpnName, clientProfileName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Client Profile objects.
     *
     * Get a list of Client Profile objects.  Client Profiles are used to assign common configuration properties to clients that have been successfully authorized.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: apiQueueManagementCopyFromOnCreateName|||x| apiTopicEndpointManagementCopyFromOnCreateName|||x| clientProfileName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnClientProfilesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnClientProfilesResponse response = api.getMsgVpnClientProfiles(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Client Username object.
     *
     * Get a Client Username object.  A client is only authorized to connect to a Message VPN that is associated with a Client Username that the client has been assigned.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: clientUsername|x||| msgVpnName|x||| password||x||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnClientUsernameTest() throws ApiException {
        String msgVpnName = null;
        String clientUsername = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnClientUsernameResponse response = api.getMsgVpnClientUsername(msgVpnName, clientUsername, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Client Username objects.
     *
     * Get a list of Client Username objects.  A client is only authorized to connect to a Message VPN that is associated with a Client Username that the client has been assigned.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: clientUsername|x||| msgVpnName|x||| password||x||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnClientUsernamesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnClientUsernamesResponse response = api.getMsgVpnClientUsernames(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Distributed Cache object.
     *
     * Get a Distributed Cache object.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheResponse response = api.getMsgVpnDistributedCache(msgVpnName, cacheName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a Cache Cluster object.
     *
     * Get a Cache Cluster object.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheClusterTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterResponse response = api.getMsgVpnDistributedCacheCluster(msgVpnName, cacheName, clusterName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a Home Cache Cluster object.
     *
     * Get a Home Cache Cluster object.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String homeClusterName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterGlobalCachingHomeClusterResponse response = api.getMsgVpnDistributedCacheClusterGlobalCachingHomeCluster(msgVpnName, cacheName, clusterName, homeClusterName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a Topic Prefix object.
     *
     * Get a Topic Prefix object.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x||| topicPrefix|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String homeClusterName = null;
        String topicPrefix = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixResponse response = api.getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefix(msgVpnName, cacheName, clusterName, homeClusterName, topicPrefix, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Topic Prefix objects.
     *
     * Get a list of Topic Prefix objects.  A Topic Prefix is a prefix for a global topic that is available from the containing Home Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x||| topicPrefix|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String homeClusterName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixesResponse response = api.getMsgVpnDistributedCacheClusterGlobalCachingHomeClusterTopicPrefixes(msgVpnName, cacheName, clusterName, homeClusterName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Home Cache Cluster objects.
     *
     * Get a list of Home Cache Cluster objects.  A Home Cache Cluster is a Cache Cluster that is the \&quot;definitive\&quot; Cache Cluster for a given topic in the context of the Global Caching feature.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| homeClusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheClusterGlobalCachingHomeClustersTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterGlobalCachingHomeClustersResponse response = api.getMsgVpnDistributedCacheClusterGlobalCachingHomeClusters(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Cache Instance object.
     *
     * Get a Cache Instance object.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| instanceName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterInstanceResponse response = api.getMsgVpnDistributedCacheClusterInstance(msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Cache Instance objects.
     *
     * Get a list of Cache Instance objects.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| instanceName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterInstancesResponse response = api.getMsgVpnDistributedCacheClusterInstances(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Topic object.
     *
     * Get a Topic object.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x||| topic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheClusterTopicTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String topic = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterTopicResponse response = api.getMsgVpnDistributedCacheClusterTopic(msgVpnName, cacheName, clusterName, topic, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Topic objects.
     *
     * Get a list of Topic objects.  The Cache Instances that belong to the containing Cache Cluster will cache any messages published to topics that match a Topic Subscription.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x||| topic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCacheClusterTopicsTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterTopicsResponse response = api.getMsgVpnDistributedCacheClusterTopics(msgVpnName, cacheName, clusterName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Cache Cluster objects.
     *
     * Get a list of Cache Cluster objects.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| clusterName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
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
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnDistributedCacheClustersResponse response = api.getMsgVpnDistributedCacheClusters(msgVpnName, cacheName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Distributed Cache objects.
     *
     * Get a list of Distributed Cache objects.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDistributedCachesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnDistributedCachesResponse response = api.getMsgVpnDistributedCaches(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a DMR Bridge object.
     *
     * Get a DMR Bridge object.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| remoteNodeName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDmrBridgeTest() throws ApiException {
        String msgVpnName = null;
        String remoteNodeName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDmrBridgeResponse response = api.getMsgVpnDmrBridge(msgVpnName, remoteNodeName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of DMR Bridge objects.
     *
     * Get a list of DMR Bridge objects.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| remoteNodeName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnDmrBridgesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnDmrBridgesResponse response = api.getMsgVpnDmrBridges(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of JNDI Connection Factory objects.
     *
     * Get a list of JNDI Connection Factory objects.  The message broker provides an internal JNDI store for provisioned Connection Factory objects that clients can access through JNDI lookups.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: connectionFactoryName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnJndiConnectionFactoriesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnJndiConnectionFactoriesResponse response = api.getMsgVpnJndiConnectionFactories(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a JNDI Connection Factory object.
     *
     * Get a JNDI Connection Factory object.  The message broker provides an internal JNDI store for provisioned Connection Factory objects that clients can access through JNDI lookups.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: connectionFactoryName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnJndiConnectionFactoryTest() throws ApiException {
        String msgVpnName = null;
        String connectionFactoryName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnJndiConnectionFactoryResponse response = api.getMsgVpnJndiConnectionFactory(msgVpnName, connectionFactoryName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a JNDI Queue object.
     *
     * Get a JNDI Queue object.  The message broker provides an internal JNDI store for provisioned Queue objects that clients can access through JNDI lookups.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| queueName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnJndiQueueTest() throws ApiException {
        String msgVpnName = null;
        String queueName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnJndiQueueResponse response = api.getMsgVpnJndiQueue(msgVpnName, queueName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of JNDI Queue objects.
     *
     * Get a list of JNDI Queue objects.  The message broker provides an internal JNDI store for provisioned Queue objects that clients can access through JNDI lookups.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| queueName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnJndiQueuesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnJndiQueuesResponse response = api.getMsgVpnJndiQueues(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a JNDI Topic object.
     *
     * Get a JNDI Topic object.  The message broker provides an internal JNDI store for provisioned Topic objects that clients can access through JNDI lookups.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| topicName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnJndiTopicTest() throws ApiException {
        String msgVpnName = null;
        String topicName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnJndiTopicResponse response = api.getMsgVpnJndiTopic(msgVpnName, topicName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of JNDI Topic objects.
     *
     * Get a list of JNDI Topic objects.  The message broker provides an internal JNDI store for provisioned Topic objects that clients can access through JNDI lookups.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| topicName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnJndiTopicsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnJndiTopicsResponse response = api.getMsgVpnJndiTopics(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get an MQTT Retain Cache object.
     *
     * Get an MQTT Retain Cache object.  Using MQTT retained messages allows publishing MQTT clients to indicate that a message must be stored for later delivery to subscribing clients when those subscribing clients add subscriptions matching the retained message&#x27;s topic. An MQTT Retain Cache processes all retained messages for a Message VPN.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnMqttRetainCacheTest() throws ApiException {
        String msgVpnName = null;
        String cacheName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnMqttRetainCacheResponse response = api.getMsgVpnMqttRetainCache(msgVpnName, cacheName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of MQTT Retain Cache objects.
     *
     * Get a list of MQTT Retain Cache objects.  Using MQTT retained messages allows publishing MQTT clients to indicate that a message must be stored for later delivery to subscribing clients when those subscribing clients add subscriptions matching the retained message&#x27;s topic. An MQTT Retain Cache processes all retained messages for a Message VPN.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: cacheName|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnMqttRetainCachesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnMqttRetainCachesResponse response = api.getMsgVpnMqttRetainCaches(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get an MQTT Session object.
     *
     * Get an MQTT Session object.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#x27;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: mqttSessionClientId|x||| mqttSessionVirtualRouter|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnMqttSessionTest() throws ApiException {
        String msgVpnName = null;
        String mqttSessionClientId = null;
        String mqttSessionVirtualRouter = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnMqttSessionResponse response = api.getMsgVpnMqttSession(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a Subscription object.
     *
     * Get a Subscription object.  An MQTT session contains a client&#x27;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: mqttSessionClientId|x||| mqttSessionVirtualRouter|x||| msgVpnName|x||| subscriptionTopic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnMqttSessionSubscriptionTest() throws ApiException {
        String msgVpnName = null;
        String mqttSessionClientId = null;
        String mqttSessionVirtualRouter = null;
        String subscriptionTopic = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnMqttSessionSubscriptionResponse response = api.getMsgVpnMqttSessionSubscription(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Subscription objects.
     *
     * Get a list of Subscription objects.  An MQTT session contains a client&#x27;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: mqttSessionClientId|x||| mqttSessionVirtualRouter|x||| msgVpnName|x||| subscriptionTopic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnMqttSessionSubscriptionsTest() throws ApiException {
        String msgVpnName = null;
        String mqttSessionClientId = null;
        String mqttSessionVirtualRouter = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnMqttSessionSubscriptionsResponse response = api.getMsgVpnMqttSessionSubscriptions(msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of MQTT Session objects.
     *
     * Get a list of MQTT Session objects.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#x27;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: mqttSessionClientId|x||| mqttSessionVirtualRouter|x||| msgVpnName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnMqttSessionsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnMqttSessionsResponse response = api.getMsgVpnMqttSessions(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Queue object.
     *
     * Get a Queue object.  A Queue acts as both a destination that clients can publish messages to, and as an endpoint that clients can bind consumers to and consume messages from.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| queueName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnQueueTest() throws ApiException {
        String msgVpnName = null;
        String queueName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnQueueResponse response = api.getMsgVpnQueue(msgVpnName, queueName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a Queue Subscription object.
     *
     * Get a Queue Subscription object.  One or more Queue Subscriptions can be added to a durable queue so that Guaranteed messages published to matching topics are also delivered to and spooled by the queue.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| queueName|x||| subscriptionTopic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnQueueSubscriptionTest() throws ApiException {
        String msgVpnName = null;
        String queueName = null;
        String subscriptionTopic = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnQueueSubscriptionResponse response = api.getMsgVpnQueueSubscription(msgVpnName, queueName, subscriptionTopic, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Queue Subscription objects.
     *
     * Get a list of Queue Subscription objects.  One or more Queue Subscriptions can be added to a durable queue so that Guaranteed messages published to matching topics are also delivered to and spooled by the queue.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| queueName|x||| subscriptionTopic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnQueueSubscriptionsTest() throws ApiException {
        String msgVpnName = null;
        String queueName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnQueueSubscriptionsResponse response = api.getMsgVpnQueueSubscriptions(msgVpnName, queueName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Queue Template object.
     *
     * Get a Queue Template object.  A Queue Template provides a mechanism for specifying the initial state for client created queues.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| queueTemplateName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnQueueTemplateTest() throws ApiException {
        String msgVpnName = null;
        String queueTemplateName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnQueueTemplateResponse response = api.getMsgVpnQueueTemplate(msgVpnName, queueTemplateName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Queue Template objects.
     *
     * Get a list of Queue Template objects.  A Queue Template provides a mechanism for specifying the initial state for client created queues.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| queueTemplateName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnQueueTemplatesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnQueueTemplatesResponse response = api.getMsgVpnQueueTemplates(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Queue objects.
     *
     * Get a list of Queue objects.  A Queue acts as both a destination that clients can publish messages to, and as an endpoint that clients can bind consumers to and consume messages from.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| queueName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnQueuesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnQueuesResponse response = api.getMsgVpnQueues(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Replay Log object.
     *
     * Get a Replay Log object.  When the Message Replay feature is enabled, message brokers store persistent messages in a Replay Log. These messages are kept until the log is full, after which the oldest messages are removed to free up space for new messages.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| replayLogName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.10.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnReplayLogTest() throws ApiException {
        String msgVpnName = null;
        String replayLogName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnReplayLogResponse response = api.getMsgVpnReplayLog(msgVpnName, replayLogName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Replay Log objects.
     *
     * Get a list of Replay Log objects.  When the Message Replay feature is enabled, message brokers store persistent messages in a Replay Log. These messages are kept until the log is full, after which the oldest messages are removed to free up space for new messages.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| replayLogName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.10.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnReplayLogsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnReplayLogsResponse response = api.getMsgVpnReplayLogs(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Replicated Topic object.
     *
     * Get a Replicated Topic object.  To indicate which messages should be replicated between the active and standby site, a Replicated Topic subscription must be configured on a Message VPN. If a published message matches both a replicated topic and an endpoint on the active site, then the message is replicated to the standby site.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| replicatedTopic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnReplicatedTopicTest() throws ApiException {
        String msgVpnName = null;
        String replicatedTopic = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnReplicatedTopicResponse response = api.getMsgVpnReplicatedTopic(msgVpnName, replicatedTopic, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Replicated Topic objects.
     *
     * Get a list of Replicated Topic objects.  To indicate which messages should be replicated between the active and standby site, a Replicated Topic subscription must be configured on a Message VPN. If a published message matches both a replicated topic and an endpoint on the active site, then the message is replicated to the standby site.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| replicatedTopic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnReplicatedTopicsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnReplicatedTopicsResponse response = api.getMsgVpnReplicatedTopics(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a REST Delivery Point object.
     *
     * Get a REST Delivery Point object.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| restDeliveryPointName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointResponse response = api.getMsgVpnRestDeliveryPoint(msgVpnName, restDeliveryPointName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a Queue Binding object.
     *
     * Get a Queue Binding object.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| queueBindingName|x||| restDeliveryPointName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointQueueBindingTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String queueBindingName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointQueueBindingResponse response = api.getMsgVpnRestDeliveryPointQueueBinding(msgVpnName, restDeliveryPointName, queueBindingName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Queue Binding objects.
     *
     * Get a list of Queue Binding objects.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| queueBindingName|x||| restDeliveryPointName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointQueueBindingsTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointQueueBindingsResponse response = api.getMsgVpnRestDeliveryPointQueueBindings(msgVpnName, restDeliveryPointName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a REST Consumer object.
     *
     * Get a REST Consumer object.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: authenticationClientCertContent||x||x authenticationClientCertPassword||x|| authenticationHttpBasicPassword||x||x authenticationHttpHeaderValue||x||x msgVpnName|x||| restConsumerName|x||| restDeliveryPointName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointRestConsumerTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumerResponse response = api.getMsgVpnRestDeliveryPointRestConsumer(msgVpnName, restDeliveryPointName, restConsumerName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a Trusted Common Name object.
     *
     * Get a Trusted Common Name object.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#x27;s server certificate.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||x| restConsumerName|x||x| restDeliveryPointName|x||x| tlsTrustedCommonName|x||x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since (will be deprecated in next SEMP version). Common Name validation has been replaced by Server Certificate Name validation.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        String tlsTrustedCommonName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse response = api.getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(msgVpnName, restDeliveryPointName, restConsumerName, tlsTrustedCommonName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Trusted Common Name objects.
     *
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#x27;s server certificate.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||x| restConsumerName|x||x| restDeliveryPointName|x||x| tlsTrustedCommonName|x||x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been deprecated since (will be deprecated in next SEMP version). Common Name validation has been replaced by Server Certificate Name validation.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesResponse response = api.getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNames(msgVpnName, restDeliveryPointName, restConsumerName, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of REST Consumer objects.
     *
     * Get a list of REST Consumer objects.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: authenticationClientCertContent||x||x authenticationClientCertPassword||x|| authenticationHttpBasicPassword||x||x authenticationHttpHeaderValue||x||x msgVpnName|x||| restConsumerName|x||| restDeliveryPointName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
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
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumersResponse response = api.getMsgVpnRestDeliveryPointRestConsumers(msgVpnName, restDeliveryPointName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of REST Delivery Point objects.
     *
     * Get a list of REST Delivery Point objects.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| restDeliveryPointName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointsResponse response = api.getMsgVpnRestDeliveryPoints(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Sequenced Topic object.
     *
     * Get a Sequenced Topic object.  A Sequenced Topic is a topic subscription for which any matching messages received on the Message VPN are assigned a sequence number that is monotonically increased by a value of one per message.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| sequencedTopic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnSequencedTopicTest() throws ApiException {
        String msgVpnName = null;
        String sequencedTopic = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnSequencedTopicResponse response = api.getMsgVpnSequencedTopic(msgVpnName, sequencedTopic, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Sequenced Topic objects.
     *
     * Get a list of Sequenced Topic objects.  A Sequenced Topic is a topic subscription for which any matching messages received on the Message VPN are assigned a sequence number that is monotonically increased by a value of one per message.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| sequencedTopic|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnSequencedTopicsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnSequencedTopicsResponse response = api.getMsgVpnSequencedTopics(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a Topic Endpoint object.
     *
     * Get a Topic Endpoint object.  A Topic Endpoint attracts messages published to a topic for which the Topic Endpoint has a matching topic subscription. The topic subscription for the Topic Endpoint is specified in the client request to bind a Flow to that Topic Endpoint. Queues are significantly more flexible than Topic Endpoints and are the recommended approach for most applications. The use of Topic Endpoints should be restricted to JMS applications.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| topicEndpointName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnTopicEndpointTest() throws ApiException {
        String msgVpnName = null;
        String topicEndpointName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnTopicEndpointResponse response = api.getMsgVpnTopicEndpoint(msgVpnName, topicEndpointName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a Topic Endpoint Template object.
     *
     * Get a Topic Endpoint Template object.  A Topic Endpoint Template provides a mechanism for specifying the initial state for client created topic endpoints.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| topicEndpointTemplateName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnTopicEndpointTemplateTest() throws ApiException {
        String msgVpnName = null;
        String topicEndpointTemplateName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnTopicEndpointTemplateResponse response = api.getMsgVpnTopicEndpointTemplate(msgVpnName, topicEndpointTemplateName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Get a list of Topic Endpoint Template objects.
     *
     * Get a list of Topic Endpoint Template objects.  A Topic Endpoint Template provides a mechanism for specifying the initial state for client created topic endpoints.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| topicEndpointTemplateName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnTopicEndpointTemplatesTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnTopicEndpointTemplatesResponse response = api.getMsgVpnTopicEndpointTemplates(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Topic Endpoint objects.
     *
     * Get a list of Topic Endpoint objects.  A Topic Endpoint attracts messages published to a topic for which the Topic Endpoint has a matching topic subscription. The topic subscription for the Topic Endpoint is specified in the client request to bind a Flow to that Topic Endpoint. Queues are significantly more flexible than Topic Endpoints and are the recommended approach for most applications. The use of Topic Endpoints should be restricted to JMS applications.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| topicEndpointName|x|||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnTopicEndpointsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnTopicEndpointsResponse response = api.getMsgVpnTopicEndpoints(msgVpnName, count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Get a list of Message VPN objects.
     *
     * Get a list of Message VPN objects.  Message VPNs (Virtual Private Networks) allow for the segregation of topic space and clients. They also group clients connecting to a network of message brokers, such that messages published within a particular group are only visible to that group&#x27;s clients.   Attribute|Identifying|Write-Only|Deprecated|Opaque :---|:---:|:---:|:---:|:---: msgVpnName|x||| replicationBridgeAuthenticationBasicPassword||x||x replicationBridgeAuthenticationClientCertContent||x||x replicationBridgeAuthenticationClientCertPassword||x|| replicationEnabledQueueBehavior||x|| restTlsServerCertEnforceTrustedCommonNameEnabled|||x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnsTest() throws ApiException {
        Integer count = null;
        String cursor = null;
        String opaquePassword = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnsResponse response = api.getMsgVpns(count, cursor, opaquePassword, where, select);

        // TODO: test validations
    }
    /**
     * Replace a Message VPN object.
     *
     * Replace a Message VPN object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  Message VPNs (Virtual Private Networks) allow for the segregation of topic space and clients. They also group clients connecting to a network of message brokers, such that messages published within a particular group are only visible to that group&#x27;s clients.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| replicationBridgeAuthenticationBasicPassword|||x|||x replicationBridgeAuthenticationClientCertContent|||x|||x replicationBridgeAuthenticationClientCertPassword|||x||| replicationEnabledQueueBehavior|||x||| restTlsServerCertEnforceTrustedCommonNameEnabled|||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue| MsgVpn|authenticationBasicProfileName|authenticationBasicType| MsgVpn|authorizationProfileName|authorizationType| MsgVpn|eventPublishTopicFormatMqttEnabled|eventPublishTopicFormatSmfEnabled| MsgVpn|eventPublishTopicFormatSmfEnabled|eventPublishTopicFormatMqttEnabled| MsgVpn|replicationBridgeAuthenticationBasicClientUsername|replicationBridgeAuthenticationBasicPassword| MsgVpn|replicationBridgeAuthenticationBasicPassword|replicationBridgeAuthenticationBasicClientUsername| MsgVpn|replicationBridgeAuthenticationClientCertPassword|replicationBridgeAuthenticationClientCertContent| MsgVpn|replicationEnabledQueueBehavior|replicationEnabled|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation. Requests which include the following attributes require greater access scope/level:   Attribute|Access Scope/Level :---|:---: alias|global/read-write authenticationBasicEnabled|global/read-write authenticationBasicProfileName|global/read-write authenticationBasicRadiusDomain|global/read-write authenticationBasicType|global/read-write authenticationClientCertAllowApiProvidedUsernameEnabled|global/read-write authenticationClientCertEnabled|global/read-write authenticationClientCertMaxChainDepth|global/read-write authenticationClientCertRevocationCheckMode|global/read-write authenticationClientCertUsernameSource|global/read-write authenticationClientCertValidateDateEnabled|global/read-write authenticationKerberosAllowApiProvidedUsernameEnabled|global/read-write authenticationKerberosEnabled|global/read-write authenticationOauthEnabled|global/read-write bridgingTlsServerCertEnforceTrustedCommonNameEnabled|global/read-write bridgingTlsServerCertMaxChainDepth|global/read-write bridgingTlsServerCertValidateDateEnabled|global/read-write dmrEnabled|global/read-write exportSubscriptionsEnabled|global/read-write maxConnectionCount|global/read-write maxEgressFlowCount|global/read-write maxEndpointCount|global/read-write maxIngressFlowCount|global/read-write maxMsgSpoolUsage|global/read-write maxSubscriptionCount|global/read-write maxTransactedSessionCount|global/read-write maxTransactionCount|global/read-write mqttRetainMaxMemory|global/read-write replicationBridgeAuthenticationBasicClientUsername|global/read-write replicationBridgeAuthenticationBasicPassword|global/read-write replicationBridgeAuthenticationClientCertContent|global/read-write replicationBridgeAuthenticationClientCertPassword|global/read-write replicationBridgeAuthenticationScheme|global/read-write replicationBridgeCompressedDataEnabled|global/read-write replicationBridgeEgressFlowWindowSize|global/read-write replicationBridgeRetryDelay|global/read-write replicationBridgeTlsEnabled|global/read-write replicationBridgeUnidirectionalClientProfileName|global/read-write replicationEnabled|global/read-write replicationEnabledQueueBehavior|global/read-write replicationQueueMaxMsgSpoolUsage|global/read-write replicationRole|global/read-write restTlsServerCertEnforceTrustedCommonNameEnabled|global/read-write restTlsServerCertMaxChainDepth|global/read-write restTlsServerCertValidateDateEnabled|global/read-write restTlsServerCertValidateNameEnabled|global/read-write sempOverMsgBusAdminClientEnabled|global/read-write sempOverMsgBusAdminDistributedCacheEnabled|global/read-write sempOverMsgBusAdminEnabled|global/read-write sempOverMsgBusEnabled|global/read-write sempOverMsgBusShowEnabled|global/read-write serviceAmqpMaxConnectionCount|global/read-write serviceAmqpPlainTextListenPort|global/read-write serviceAmqpTlsListenPort|global/read-write serviceMqttMaxConnectionCount|global/read-write serviceMqttPlainTextListenPort|global/read-write serviceMqttTlsListenPort|global/read-write serviceMqttTlsWebSocketListenPort|global/read-write serviceMqttWebSocketListenPort|global/read-write serviceRestIncomingMaxConnectionCount|global/read-write serviceRestIncomingPlainTextListenPort|global/read-write serviceRestIncomingTlsListenPort|global/read-write serviceRestOutgoingMaxConnectionCount|global/read-write serviceSmfMaxConnectionCount|global/read-write serviceWebMaxConnectionCount|global/read-write    This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnTest() throws ApiException {
        MsgVpn body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnResponse response = api.replaceMsgVpn(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace an ACL Profile object.
     *
     * Replace an ACL Profile object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  An ACL Profile controls whether an authenticated client is permitted to establish a connection with the message broker or permitted to publish and subscribe to specific topics.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnAclProfileTest() throws ApiException {
        MsgVpnAclProfile body = null;
        String msgVpnName = null;
        String aclProfileName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfileResponse response = api.replaceMsgVpnAclProfile(body, msgVpnName, aclProfileName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace an OAuth Provider object.
     *
     * Replace an OAuth Provider object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  OAuth Providers contain information about the issuer of an OAuth token that is needed to validate the token and derive a client username from it.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| oauthProviderName|x|x|||| tokenIntrospectionPassword|||x|||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnAuthenticationOauthProviderTest() throws ApiException {
        MsgVpnAuthenticationOauthProvider body = null;
        String msgVpnName = null;
        String oauthProviderName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAuthenticationOauthProviderResponse response = api.replaceMsgVpnAuthenticationOauthProvider(body, msgVpnName, oauthProviderName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace an LDAP Authorization Group object.
     *
     * Replace an LDAP Authorization Group object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  To use client authorization groups configured on an external LDAP server to provide client authorizations, LDAP Authorization Group objects must be created on the Message VPN that match the authorization groups provisioned on the LDAP server. These objects must be configured with the client profiles and ACL profiles that will be assigned to the clients that belong to those authorization groups. A newly created group is placed at the end of the group list which is the lowest priority.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName||||x|| authorizationGroupName|x|x|||| clientProfileName||||x|| msgVpnName|x|x|||| orderAfterAuthorizationGroupName|||x||| orderBeforeAuthorizationGroupName|||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnAuthorizationGroup|orderAfterAuthorizationGroupName||orderBeforeAuthorizationGroupName MsgVpnAuthorizationGroup|orderBeforeAuthorizationGroupName||orderAfterAuthorizationGroupName    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnAuthorizationGroupTest() throws ApiException {
        MsgVpnAuthorizationGroup body = null;
        String msgVpnName = null;
        String authorizationGroupName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAuthorizationGroupResponse response = api.replaceMsgVpnAuthorizationGroup(body, msgVpnName, authorizationGroupName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Bridge object.
     *
     * Replace a Bridge object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: bridgeName|x|x|||| bridgeVirtualRouter|x|x|||| maxTtl||||x|| msgVpnName|x|x|||| remoteAuthenticationBasicClientUsername||||x|| remoteAuthenticationBasicPassword|||x|x||x remoteAuthenticationClientCertContent|||x|x||x remoteAuthenticationClientCertPassword|||x|x|| remoteAuthenticationScheme||||x|| remoteDeliverToOnePriority||||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnBridge|remoteAuthenticationBasicClientUsername|remoteAuthenticationBasicPassword| MsgVpnBridge|remoteAuthenticationBasicPassword|remoteAuthenticationBasicClientUsername| MsgVpnBridge|remoteAuthenticationClientCertPassword|remoteAuthenticationClientCertContent|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnBridgeTest() throws ApiException {
        MsgVpnBridge body = null;
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnBridgeResponse response = api.replaceMsgVpnBridge(body, msgVpnName, bridgeName, bridgeVirtualRouter, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Remote Message VPN object.
     *
     * Replace a Remote Message VPN object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  The Remote Message VPN is the Message VPN that the Bridge connects to.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: bridgeName|x|x|||| bridgeVirtualRouter|x|x|||| clientUsername||||x|| compressedDataEnabled||||x|| egressFlowWindowSize||||x|| msgVpnName|x|x|||| password|||x|x||x remoteMsgVpnInterface|x|x|||| remoteMsgVpnLocation|x|x|||| remoteMsgVpnName|x|x|||| tlsEnabled||||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnBridgeRemoteMsgVpn|clientUsername|password| MsgVpnBridgeRemoteMsgVpn|password|clientUsername|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnBridgeRemoteMsgVpnTest() throws ApiException {
        MsgVpnBridgeRemoteMsgVpn body = null;
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String remoteMsgVpnName = null;
        String remoteMsgVpnLocation = null;
        String remoteMsgVpnInterface = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnBridgeRemoteMsgVpnResponse response = api.replaceMsgVpnBridgeRemoteMsgVpn(body, msgVpnName, bridgeName, bridgeVirtualRouter, remoteMsgVpnName, remoteMsgVpnLocation, remoteMsgVpnInterface, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Client Profile object.
     *
     * Replace a Client Profile object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  Client Profiles are used to assign common configuration properties to clients that have been successfully authorized.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: apiQueueManagementCopyFromOnCreateName|||||x| apiTopicEndpointManagementCopyFromOnCreateName|||||x| clientProfileName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnClientProfileTest() throws ApiException {
        MsgVpnClientProfile body = null;
        String msgVpnName = null;
        String clientProfileName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnClientProfileResponse response = api.replaceMsgVpnClientProfile(body, msgVpnName, clientProfileName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Client Username object.
     *
     * Replace a Client Username object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A client is only authorized to connect to a Message VPN that is associated with a Client Username that the client has been assigned.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName||||x|| clientProfileName||||x|| clientUsername|x|x|||| msgVpnName|x|x|||| password|||x|||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnClientUsernameTest() throws ApiException {
        MsgVpnClientUsername body = null;
        String msgVpnName = null;
        String clientUsername = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnClientUsernameResponse response = api.replaceMsgVpnClientUsername(body, msgVpnName, clientUsername, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Distributed Cache object.
     *
     * Replace a Distributed Cache object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnDistributedCache|scheduledDeleteMsgDayList|scheduledDeleteMsgTimeList| MsgVpnDistributedCache|scheduledDeleteMsgTimeList|scheduledDeleteMsgDayList|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnDistributedCacheTest() throws ApiException {
        MsgVpnDistributedCache body = null;
        String msgVpnName = null;
        String cacheName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheResponse response = api.replaceMsgVpnDistributedCache(body, msgVpnName, cacheName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Cache Cluster object.
     *
     * Replace a Cache Cluster object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent| EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnDistributedCacheClusterTest() throws ApiException {
        MsgVpnDistributedCacheCluster body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterResponse response = api.replaceMsgVpnDistributedCacheCluster(body, msgVpnName, cacheName, clusterName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Cache Instance object.
     *
     * Replace a Cache Instance object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| instanceName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnDistributedCacheClusterInstanceTest() throws ApiException {
        MsgVpnDistributedCacheClusterInstance body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String instanceName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterInstanceResponse response = api.replaceMsgVpnDistributedCacheClusterInstance(body, msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a DMR Bridge object.
     *
     * Replace a DMR Bridge object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| remoteNodeName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnDmrBridgeTest() throws ApiException {
        MsgVpnDmrBridge body = null;
        String msgVpnName = null;
        String remoteNodeName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDmrBridgeResponse response = api.replaceMsgVpnDmrBridge(body, msgVpnName, remoteNodeName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a JNDI Connection Factory object.
     *
     * Replace a JNDI Connection Factory object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  The message broker provides an internal JNDI store for provisioned Connection Factory objects that clients can access through JNDI lookups.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: connectionFactoryName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnJndiConnectionFactoryTest() throws ApiException {
        MsgVpnJndiConnectionFactory body = null;
        String msgVpnName = null;
        String connectionFactoryName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnJndiConnectionFactoryResponse response = api.replaceMsgVpnJndiConnectionFactory(body, msgVpnName, connectionFactoryName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a JNDI Queue object.
     *
     * Replace a JNDI Queue object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  The message broker provides an internal JNDI store for provisioned Queue objects that clients can access through JNDI lookups.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| queueName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnJndiQueueTest() throws ApiException {
        MsgVpnJndiQueue body = null;
        String msgVpnName = null;
        String queueName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnJndiQueueResponse response = api.replaceMsgVpnJndiQueue(body, msgVpnName, queueName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a JNDI Topic object.
     *
     * Replace a JNDI Topic object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  The message broker provides an internal JNDI store for provisioned Topic objects that clients can access through JNDI lookups.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| topicName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnJndiTopicTest() throws ApiException {
        MsgVpnJndiTopic body = null;
        String msgVpnName = null;
        String topicName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnJndiTopicResponse response = api.replaceMsgVpnJndiTopic(body, msgVpnName, topicName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace an MQTT Retain Cache object.
     *
     * Replace an MQTT Retain Cache object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  Using MQTT retained messages allows publishing MQTT clients to indicate that a message must be stored for later delivery to subscribing clients when those subscribing clients add subscriptions matching the retained message&#x27;s topic. An MQTT Retain Cache processes all retained messages for a Message VPN.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnMqttRetainCacheTest() throws ApiException {
        MsgVpnMqttRetainCache body = null;
        String msgVpnName = null;
        String cacheName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnMqttRetainCacheResponse response = api.replaceMsgVpnMqttRetainCache(body, msgVpnName, cacheName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace an MQTT Session object.
     *
     * Replace an MQTT Session object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#x27;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x|||| mqttSessionVirtualRouter|x|x|||| msgVpnName|x|x|||| owner||||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnMqttSessionTest() throws ApiException {
        MsgVpnMqttSession body = null;
        String msgVpnName = null;
        String mqttSessionClientId = null;
        String mqttSessionVirtualRouter = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnMqttSessionResponse response = api.replaceMsgVpnMqttSession(body, msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Subscription object.
     *
     * Replace a Subscription object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  An MQTT session contains a client&#x27;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x|||| mqttSessionVirtualRouter|x|x|||| msgVpnName|x|x|||| subscriptionTopic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnMqttSessionSubscriptionTest() throws ApiException {
        MsgVpnMqttSessionSubscription body = null;
        String msgVpnName = null;
        String mqttSessionClientId = null;
        String mqttSessionVirtualRouter = null;
        String subscriptionTopic = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnMqttSessionSubscriptionResponse response = api.replaceMsgVpnMqttSessionSubscription(body, msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Queue object.
     *
     * Replace a Queue object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Queue acts as both a destination that clients can publish messages to, and as an endpoint that clients can bind consumers to and consume messages from.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: accessType||||x|| msgVpnName|x|x|||| owner||||x|| permission||||x|| queueName|x|x|||| respectMsgPriorityEnabled||||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnQueueTest() throws ApiException {
        MsgVpnQueue body = null;
        String msgVpnName = null;
        String queueName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnQueueResponse response = api.replaceMsgVpnQueue(body, msgVpnName, queueName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Queue Template object.
     *
     * Replace a Queue Template object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Queue Template provides a mechanism for specifying the initial state for client created queues.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| queueTemplateName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnQueueTemplateTest() throws ApiException {
        MsgVpnQueueTemplate body = null;
        String msgVpnName = null;
        String queueTemplateName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnQueueTemplateResponse response = api.replaceMsgVpnQueueTemplate(body, msgVpnName, queueTemplateName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Replay Log object.
     *
     * Replace a Replay Log object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  When the Message Replay feature is enabled, message brokers store persistent messages in a Replay Log. These messages are kept until the log is full, after which the oldest messages are removed to free up space for new messages.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| replayLogName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.10.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnReplayLogTest() throws ApiException {
        MsgVpnReplayLog body = null;
        String msgVpnName = null;
        String replayLogName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnReplayLogResponse response = api.replaceMsgVpnReplayLog(body, msgVpnName, replayLogName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Replicated Topic object.
     *
     * Replace a Replicated Topic object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  To indicate which messages should be replicated between the active and standby site, a Replicated Topic subscription must be configured on a Message VPN. If a published message matches both a replicated topic and an endpoint on the active site, then the message is replicated to the standby site.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| replicatedTopic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnReplicatedTopicTest() throws ApiException {
        MsgVpnReplicatedTopic body = null;
        String msgVpnName = null;
        String replicatedTopic = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnReplicatedTopicResponse response = api.replaceMsgVpnReplicatedTopic(body, msgVpnName, replicatedTopic, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a REST Delivery Point object.
     *
     * Replace a REST Delivery Point object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: clientProfileName||||x|| msgVpnName|x|x|||| restDeliveryPointName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnRestDeliveryPointTest() throws ApiException {
        MsgVpnRestDeliveryPoint body = null;
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointResponse response = api.replaceMsgVpnRestDeliveryPoint(body, msgVpnName, restDeliveryPointName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Queue Binding object.
     *
     * Replace a Queue Binding object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| queueBindingName|x|x|||| restDeliveryPointName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnRestDeliveryPointQueueBindingTest() throws ApiException {
        MsgVpnRestDeliveryPointQueueBinding body = null;
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String queueBindingName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointQueueBindingResponse response = api.replaceMsgVpnRestDeliveryPointQueueBinding(body, msgVpnName, restDeliveryPointName, queueBindingName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a REST Consumer object.
     *
     * Replace a REST Consumer object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: authenticationClientCertContent|||x|x||x authenticationClientCertPassword|||x|x|| authenticationHttpBasicPassword|||x|x||x authenticationHttpBasicUsername||||x|| authenticationHttpHeaderValue|||x|||x authenticationScheme||||x|| msgVpnName|x|x|||| outgoingConnectionCount||||x|| remoteHost||||x|| remotePort||||x|| restConsumerName|x|x|||| restDeliveryPointName|x|x|||| tlsCipherSuiteList||||x|| tlsEnabled||||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnRestDeliveryPointRestConsumer|authenticationClientCertPassword|authenticationClientCertContent| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicPassword|authenticationHttpBasicUsername| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicUsername|authenticationHttpBasicPassword| MsgVpnRestDeliveryPointRestConsumer|remotePort|tlsEnabled| MsgVpnRestDeliveryPointRestConsumer|tlsEnabled|remotePort|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnRestDeliveryPointRestConsumerTest() throws ApiException {
        MsgVpnRestDeliveryPointRestConsumer body = null;
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumerResponse response = api.replaceMsgVpnRestDeliveryPointRestConsumer(body, msgVpnName, restDeliveryPointName, restConsumerName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Topic Endpoint object.
     *
     * Replace a Topic Endpoint object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Topic Endpoint attracts messages published to a topic for which the Topic Endpoint has a matching topic subscription. The topic subscription for the Topic Endpoint is specified in the client request to bind a Flow to that Topic Endpoint. Queues are significantly more flexible than Topic Endpoints and are the recommended approach for most applications. The use of Topic Endpoints should be restricted to JMS applications.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: accessType||||x|| msgVpnName|x|x|||| owner||||x|| permission||||x|| respectMsgPriorityEnabled||||x|| topicEndpointName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnTopicEndpointTest() throws ApiException {
        MsgVpnTopicEndpoint body = null;
        String msgVpnName = null;
        String topicEndpointName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnTopicEndpointResponse response = api.replaceMsgVpnTopicEndpoint(body, msgVpnName, topicEndpointName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Replace a Topic Endpoint Template object.
     *
     * Replace a Topic Endpoint Template object. Any attribute missing from the request will be set to its default value, subject to the exceptions in note 4.  A Topic Endpoint Template provides a mechanism for specifying the initial state for client created topic endpoints.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| topicEndpointTemplateName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void replaceMsgVpnTopicEndpointTemplateTest() throws ApiException {
        MsgVpnTopicEndpointTemplate body = null;
        String msgVpnName = null;
        String topicEndpointTemplateName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnTopicEndpointTemplateResponse response = api.replaceMsgVpnTopicEndpointTemplate(body, msgVpnName, topicEndpointTemplateName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Message VPN object.
     *
     * Update a Message VPN object. Any attribute missing from the request will be left unchanged.  Message VPNs (Virtual Private Networks) allow for the segregation of topic space and clients. They also group clients connecting to a network of message brokers, such that messages published within a particular group are only visible to that group&#x27;s clients.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| replicationBridgeAuthenticationBasicPassword|||x|||x replicationBridgeAuthenticationClientCertContent|||x|||x replicationBridgeAuthenticationClientCertPassword|||x||| replicationEnabledQueueBehavior|||x||| restTlsServerCertEnforceTrustedCommonNameEnabled|||||x|    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue| MsgVpn|authenticationBasicProfileName|authenticationBasicType| MsgVpn|authorizationProfileName|authorizationType| MsgVpn|eventPublishTopicFormatMqttEnabled|eventPublishTopicFormatSmfEnabled| MsgVpn|eventPublishTopicFormatSmfEnabled|eventPublishTopicFormatMqttEnabled| MsgVpn|replicationBridgeAuthenticationBasicClientUsername|replicationBridgeAuthenticationBasicPassword| MsgVpn|replicationBridgeAuthenticationBasicPassword|replicationBridgeAuthenticationBasicClientUsername| MsgVpn|replicationBridgeAuthenticationClientCertPassword|replicationBridgeAuthenticationClientCertContent| MsgVpn|replicationEnabledQueueBehavior|replicationEnabled|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation. Requests which include the following attributes require greater access scope/level:   Attribute|Access Scope/Level :---|:---: alias|global/read-write authenticationBasicEnabled|global/read-write authenticationBasicProfileName|global/read-write authenticationBasicRadiusDomain|global/read-write authenticationBasicType|global/read-write authenticationClientCertAllowApiProvidedUsernameEnabled|global/read-write authenticationClientCertEnabled|global/read-write authenticationClientCertMaxChainDepth|global/read-write authenticationClientCertRevocationCheckMode|global/read-write authenticationClientCertUsernameSource|global/read-write authenticationClientCertValidateDateEnabled|global/read-write authenticationKerberosAllowApiProvidedUsernameEnabled|global/read-write authenticationKerberosEnabled|global/read-write authenticationOauthEnabled|global/read-write bridgingTlsServerCertEnforceTrustedCommonNameEnabled|global/read-write bridgingTlsServerCertMaxChainDepth|global/read-write bridgingTlsServerCertValidateDateEnabled|global/read-write dmrEnabled|global/read-write exportSubscriptionsEnabled|global/read-write maxConnectionCount|global/read-write maxEgressFlowCount|global/read-write maxEndpointCount|global/read-write maxIngressFlowCount|global/read-write maxMsgSpoolUsage|global/read-write maxSubscriptionCount|global/read-write maxTransactedSessionCount|global/read-write maxTransactionCount|global/read-write mqttRetainMaxMemory|global/read-write replicationBridgeAuthenticationBasicClientUsername|global/read-write replicationBridgeAuthenticationBasicPassword|global/read-write replicationBridgeAuthenticationClientCertContent|global/read-write replicationBridgeAuthenticationClientCertPassword|global/read-write replicationBridgeAuthenticationScheme|global/read-write replicationBridgeCompressedDataEnabled|global/read-write replicationBridgeEgressFlowWindowSize|global/read-write replicationBridgeRetryDelay|global/read-write replicationBridgeTlsEnabled|global/read-write replicationBridgeUnidirectionalClientProfileName|global/read-write replicationEnabled|global/read-write replicationEnabledQueueBehavior|global/read-write replicationQueueMaxMsgSpoolUsage|global/read-write replicationRole|global/read-write restTlsServerCertEnforceTrustedCommonNameEnabled|global/read-write restTlsServerCertMaxChainDepth|global/read-write restTlsServerCertValidateDateEnabled|global/read-write restTlsServerCertValidateNameEnabled|global/read-write sempOverMsgBusAdminClientEnabled|global/read-write sempOverMsgBusAdminDistributedCacheEnabled|global/read-write sempOverMsgBusAdminEnabled|global/read-write sempOverMsgBusEnabled|global/read-write sempOverMsgBusShowEnabled|global/read-write serviceAmqpMaxConnectionCount|global/read-write serviceAmqpPlainTextListenPort|global/read-write serviceAmqpTlsListenPort|global/read-write serviceMqttMaxConnectionCount|global/read-write serviceMqttPlainTextListenPort|global/read-write serviceMqttTlsListenPort|global/read-write serviceMqttTlsWebSocketListenPort|global/read-write serviceMqttWebSocketListenPort|global/read-write serviceRestIncomingMaxConnectionCount|global/read-write serviceRestIncomingPlainTextListenPort|global/read-write serviceRestIncomingTlsListenPort|global/read-write serviceRestOutgoingMaxConnectionCount|global/read-write serviceSmfMaxConnectionCount|global/read-write serviceWebMaxConnectionCount|global/read-write    This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnTest() throws ApiException {
        MsgVpn body = null;
        String msgVpnName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnResponse response = api.updateMsgVpn(body, msgVpnName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update an ACL Profile object.
     *
     * Update an ACL Profile object. Any attribute missing from the request will be left unchanged.  An ACL Profile controls whether an authenticated client is permitted to establish a connection with the message broker or permitted to publish and subscribe to specific topics.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnAclProfileTest() throws ApiException {
        MsgVpnAclProfile body = null;
        String msgVpnName = null;
        String aclProfileName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAclProfileResponse response = api.updateMsgVpnAclProfile(body, msgVpnName, aclProfileName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update an OAuth Provider object.
     *
     * Update an OAuth Provider object. Any attribute missing from the request will be left unchanged.  OAuth Providers contain information about the issuer of an OAuth token that is needed to validate the token and derive a client username from it.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| oauthProviderName|x|x|||| tokenIntrospectionPassword|||x|||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.13.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnAuthenticationOauthProviderTest() throws ApiException {
        MsgVpnAuthenticationOauthProvider body = null;
        String msgVpnName = null;
        String oauthProviderName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAuthenticationOauthProviderResponse response = api.updateMsgVpnAuthenticationOauthProvider(body, msgVpnName, oauthProviderName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update an LDAP Authorization Group object.
     *
     * Update an LDAP Authorization Group object. Any attribute missing from the request will be left unchanged.  To use client authorization groups configured on an external LDAP server to provide client authorizations, LDAP Authorization Group objects must be created on the Message VPN that match the authorization groups provisioned on the LDAP server. These objects must be configured with the client profiles and ACL profiles that will be assigned to the clients that belong to those authorization groups. A newly created group is placed at the end of the group list which is the lowest priority.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName||||x|| authorizationGroupName|x|x|||| clientProfileName||||x|| msgVpnName|x|x|||| orderAfterAuthorizationGroupName|||x||| orderBeforeAuthorizationGroupName|||x|||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnAuthorizationGroup|orderAfterAuthorizationGroupName||orderBeforeAuthorizationGroupName MsgVpnAuthorizationGroup|orderBeforeAuthorizationGroupName||orderAfterAuthorizationGroupName    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnAuthorizationGroupTest() throws ApiException {
        MsgVpnAuthorizationGroup body = null;
        String msgVpnName = null;
        String authorizationGroupName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnAuthorizationGroupResponse response = api.updateMsgVpnAuthorizationGroup(body, msgVpnName, authorizationGroupName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Bridge object.
     *
     * Update a Bridge object. Any attribute missing from the request will be left unchanged.  Bridges can be used to link two Message VPNs so that messages published to one Message VPN that match the topic subscriptions set for the bridge are also delivered to the linked Message VPN.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: bridgeName|x|x|||| bridgeVirtualRouter|x|x|||| maxTtl||||x|| msgVpnName|x|x|||| remoteAuthenticationBasicClientUsername||||x|| remoteAuthenticationBasicPassword|||x|x||x remoteAuthenticationClientCertContent|||x|x||x remoteAuthenticationClientCertPassword|||x|x|| remoteAuthenticationScheme||||x|| remoteDeliverToOnePriority||||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnBridge|remoteAuthenticationBasicClientUsername|remoteAuthenticationBasicPassword| MsgVpnBridge|remoteAuthenticationBasicPassword|remoteAuthenticationBasicClientUsername| MsgVpnBridge|remoteAuthenticationClientCertPassword|remoteAuthenticationClientCertContent|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnBridgeTest() throws ApiException {
        MsgVpnBridge body = null;
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnBridgeResponse response = api.updateMsgVpnBridge(body, msgVpnName, bridgeName, bridgeVirtualRouter, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Remote Message VPN object.
     *
     * Update a Remote Message VPN object. Any attribute missing from the request will be left unchanged.  The Remote Message VPN is the Message VPN that the Bridge connects to.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: bridgeName|x|x|||| bridgeVirtualRouter|x|x|||| clientUsername||||x|| compressedDataEnabled||||x|| egressFlowWindowSize||||x|| msgVpnName|x|x|||| password|||x|x||x remoteMsgVpnInterface|x|x|||| remoteMsgVpnLocation|x|x|||| remoteMsgVpnName|x|x|||| tlsEnabled||||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnBridgeRemoteMsgVpn|clientUsername|password| MsgVpnBridgeRemoteMsgVpn|password|clientUsername|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnBridgeRemoteMsgVpnTest() throws ApiException {
        MsgVpnBridgeRemoteMsgVpn body = null;
        String msgVpnName = null;
        String bridgeName = null;
        String bridgeVirtualRouter = null;
        String remoteMsgVpnName = null;
        String remoteMsgVpnLocation = null;
        String remoteMsgVpnInterface = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnBridgeRemoteMsgVpnResponse response = api.updateMsgVpnBridgeRemoteMsgVpn(body, msgVpnName, bridgeName, bridgeVirtualRouter, remoteMsgVpnName, remoteMsgVpnLocation, remoteMsgVpnInterface, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Client Profile object.
     *
     * Update a Client Profile object. Any attribute missing from the request will be left unchanged.  Client Profiles are used to assign common configuration properties to clients that have been successfully authorized.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: apiQueueManagementCopyFromOnCreateName|||||x| apiTopicEndpointManagementCopyFromOnCreateName|||||x| clientProfileName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent|    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnClientProfileTest() throws ApiException {
        MsgVpnClientProfile body = null;
        String msgVpnName = null;
        String clientProfileName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnClientProfileResponse response = api.updateMsgVpnClientProfile(body, msgVpnName, clientProfileName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Client Username object.
     *
     * Update a Client Username object. Any attribute missing from the request will be left unchanged.  A client is only authorized to connect to a Message VPN that is associated with a Client Username that the client has been assigned.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: aclProfileName||||x|| clientProfileName||||x|| clientUsername|x|x|||| msgVpnName|x|x|||| password|||x|||x    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnClientUsernameTest() throws ApiException {
        MsgVpnClientUsername body = null;
        String msgVpnName = null;
        String clientUsername = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnClientUsernameResponse response = api.updateMsgVpnClientUsername(body, msgVpnName, clientUsername, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Distributed Cache object.
     *
     * Update a Distributed Cache object. Any attribute missing from the request will be left unchanged.  A Distributed Cache is a collection of one or more Cache Clusters that belong to the same Message VPN. Each Cache Cluster in a Distributed Cache is configured to subscribe to a different set of topics. This effectively divides up the configured topic space, to provide scaling to very large topic spaces or very high cached message throughput.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnDistributedCache|scheduledDeleteMsgDayList|scheduledDeleteMsgTimeList| MsgVpnDistributedCache|scheduledDeleteMsgTimeList|scheduledDeleteMsgDayList|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnDistributedCacheTest() throws ApiException {
        MsgVpnDistributedCache body = null;
        String msgVpnName = null;
        String cacheName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheResponse response = api.updateMsgVpnDistributedCache(body, msgVpnName, cacheName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Cache Cluster object.
     *
     * Update a Cache Cluster object. Any attribute missing from the request will be left unchanged.  A Cache Cluster is a collection of one or more Cache Instances that subscribe to exactly the same topics. Cache Instances are grouped together in a Cache Cluster for the purpose of fault tolerance and load balancing. As published messages are received, the message broker message bus sends these live data messages to the Cache Instances in the Cache Cluster. This enables client cache requests to be served by any of Cache Instances in the Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| msgVpnName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThresholdByPercent|clearPercent|setPercent| EventThresholdByPercent|setPercent|clearPercent| EventThresholdByValue|clearValue|setValue| EventThresholdByValue|setValue|clearValue|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnDistributedCacheClusterTest() throws ApiException {
        MsgVpnDistributedCacheCluster body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterResponse response = api.updateMsgVpnDistributedCacheCluster(body, msgVpnName, cacheName, clusterName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Cache Instance object.
     *
     * Update a Cache Instance object. Any attribute missing from the request will be left unchanged.  A Cache Instance is a single Cache process that belongs to a single Cache Cluster. A Cache Instance object provisioned on the broker is used to disseminate configuration information to the Cache process. Cache Instances listen for and cache live data messages that match the topic subscriptions configured for their parent Cache Cluster.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| clusterName|x|x|||| instanceName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnDistributedCacheClusterInstanceTest() throws ApiException {
        MsgVpnDistributedCacheClusterInstance body = null;
        String msgVpnName = null;
        String cacheName = null;
        String clusterName = null;
        String instanceName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDistributedCacheClusterInstanceResponse response = api.updateMsgVpnDistributedCacheClusterInstance(body, msgVpnName, cacheName, clusterName, instanceName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a DMR Bridge object.
     *
     * Update a DMR Bridge object. Any attribute missing from the request will be left unchanged.  A DMR Bridge is required to establish a data channel over a corresponding external link to the remote node for a given Message VPN. Each DMR Bridge identifies which external link the Message VPN should use, and what the name of the equivalent Message VPN at the remote node is.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| remoteNodeName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;global/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnDmrBridgeTest() throws ApiException {
        MsgVpnDmrBridge body = null;
        String msgVpnName = null;
        String remoteNodeName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnDmrBridgeResponse response = api.updateMsgVpnDmrBridge(body, msgVpnName, remoteNodeName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a JNDI Connection Factory object.
     *
     * Update a JNDI Connection Factory object. Any attribute missing from the request will be left unchanged.  The message broker provides an internal JNDI store for provisioned Connection Factory objects that clients can access through JNDI lookups.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: connectionFactoryName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnJndiConnectionFactoryTest() throws ApiException {
        MsgVpnJndiConnectionFactory body = null;
        String msgVpnName = null;
        String connectionFactoryName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnJndiConnectionFactoryResponse response = api.updateMsgVpnJndiConnectionFactory(body, msgVpnName, connectionFactoryName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a JNDI Queue object.
     *
     * Update a JNDI Queue object. Any attribute missing from the request will be left unchanged.  The message broker provides an internal JNDI store for provisioned Queue objects that clients can access through JNDI lookups.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| queueName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnJndiQueueTest() throws ApiException {
        MsgVpnJndiQueue body = null;
        String msgVpnName = null;
        String queueName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnJndiQueueResponse response = api.updateMsgVpnJndiQueue(body, msgVpnName, queueName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a JNDI Topic object.
     *
     * Update a JNDI Topic object. Any attribute missing from the request will be left unchanged.  The message broker provides an internal JNDI store for provisioned Topic objects that clients can access through JNDI lookups.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| topicName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.2.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnJndiTopicTest() throws ApiException {
        MsgVpnJndiTopic body = null;
        String msgVpnName = null;
        String topicName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnJndiTopicResponse response = api.updateMsgVpnJndiTopic(body, msgVpnName, topicName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update an MQTT Retain Cache object.
     *
     * Update an MQTT Retain Cache object. Any attribute missing from the request will be left unchanged.  Using MQTT retained messages allows publishing MQTT clients to indicate that a message must be stored for later delivery to subscribing clients when those subscribing clients add subscriptions matching the retained message&#x27;s topic. An MQTT Retain Cache processes all retained messages for a Message VPN.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: cacheName|x|x|||| msgVpnName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnMqttRetainCacheTest() throws ApiException {
        MsgVpnMqttRetainCache body = null;
        String msgVpnName = null;
        String cacheName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnMqttRetainCacheResponse response = api.updateMsgVpnMqttRetainCache(body, msgVpnName, cacheName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update an MQTT Session object.
     *
     * Update an MQTT Session object. Any attribute missing from the request will be left unchanged.  An MQTT Session object is a virtual representation of an MQTT client connection. An MQTT session holds the state of an MQTT client (that is, it is used to contain a client&#x27;s QoS 0 and QoS 1 subscription sets and any undelivered QoS 1 messages).   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x|||| mqttSessionVirtualRouter|x|x|||| msgVpnName|x|x|||| owner||||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnMqttSessionTest() throws ApiException {
        MsgVpnMqttSession body = null;
        String msgVpnName = null;
        String mqttSessionClientId = null;
        String mqttSessionVirtualRouter = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnMqttSessionResponse response = api.updateMsgVpnMqttSession(body, msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Subscription object.
     *
     * Update a Subscription object. Any attribute missing from the request will be left unchanged.  An MQTT session contains a client&#x27;s QoS 0 and QoS 1 subscription sets. On creation, a subscription defaults to QoS 0.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: mqttSessionClientId|x|x|||| mqttSessionVirtualRouter|x|x|||| msgVpnName|x|x|||| subscriptionTopic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnMqttSessionSubscriptionTest() throws ApiException {
        MsgVpnMqttSessionSubscription body = null;
        String msgVpnName = null;
        String mqttSessionClientId = null;
        String mqttSessionVirtualRouter = null;
        String subscriptionTopic = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnMqttSessionSubscriptionResponse response = api.updateMsgVpnMqttSessionSubscription(body, msgVpnName, mqttSessionClientId, mqttSessionVirtualRouter, subscriptionTopic, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Queue object.
     *
     * Update a Queue object. Any attribute missing from the request will be left unchanged.  A Queue acts as both a destination that clients can publish messages to, and as an endpoint that clients can bind consumers to and consume messages from.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: accessType||||x|| msgVpnName|x|x|||| owner||||x|| permission||||x|| queueName|x|x|||| respectMsgPriorityEnabled||||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnQueueTest() throws ApiException {
        MsgVpnQueue body = null;
        String msgVpnName = null;
        String queueName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnQueueResponse response = api.updateMsgVpnQueue(body, msgVpnName, queueName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Queue Template object.
     *
     * Update a Queue Template object. Any attribute missing from the request will be left unchanged.  A Queue Template provides a mechanism for specifying the initial state for client created queues.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| queueTemplateName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnQueueTemplateTest() throws ApiException {
        MsgVpnQueueTemplate body = null;
        String msgVpnName = null;
        String queueTemplateName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnQueueTemplateResponse response = api.updateMsgVpnQueueTemplate(body, msgVpnName, queueTemplateName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Replay Log object.
     *
     * Update a Replay Log object. Any attribute missing from the request will be left unchanged.  When the Message Replay feature is enabled, message brokers store persistent messages in a Replay Log. These messages are kept until the log is full, after which the oldest messages are removed to free up space for new messages.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| replayLogName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.10.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnReplayLogTest() throws ApiException {
        MsgVpnReplayLog body = null;
        String msgVpnName = null;
        String replayLogName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnReplayLogResponse response = api.updateMsgVpnReplayLog(body, msgVpnName, replayLogName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Replicated Topic object.
     *
     * Update a Replicated Topic object. Any attribute missing from the request will be left unchanged.  To indicate which messages should be replicated between the active and standby site, a Replicated Topic subscription must be configured on a Message VPN. If a published message matches both a replicated topic and an endpoint on the active site, then the message is replicated to the standby site.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| replicatedTopic|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnReplicatedTopicTest() throws ApiException {
        MsgVpnReplicatedTopic body = null;
        String msgVpnName = null;
        String replicatedTopic = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnReplicatedTopicResponse response = api.updateMsgVpnReplicatedTopic(body, msgVpnName, replicatedTopic, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a REST Delivery Point object.
     *
     * Update a REST Delivery Point object. Any attribute missing from the request will be left unchanged.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: clientProfileName||||x|| msgVpnName|x|x|||| restDeliveryPointName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnRestDeliveryPointTest() throws ApiException {
        MsgVpnRestDeliveryPoint body = null;
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointResponse response = api.updateMsgVpnRestDeliveryPoint(body, msgVpnName, restDeliveryPointName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Queue Binding object.
     *
     * Update a Queue Binding object. Any attribute missing from the request will be left unchanged.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| queueBindingName|x|x|||| restDeliveryPointName|x|x||||    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnRestDeliveryPointQueueBindingTest() throws ApiException {
        MsgVpnRestDeliveryPointQueueBinding body = null;
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String queueBindingName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointQueueBindingResponse response = api.updateMsgVpnRestDeliveryPointQueueBinding(body, msgVpnName, restDeliveryPointName, queueBindingName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a REST Consumer object.
     *
     * Update a REST Consumer object. Any attribute missing from the request will be left unchanged.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: authenticationClientCertContent|||x|x||x authenticationClientCertPassword|||x|x|| authenticationHttpBasicPassword|||x|x||x authenticationHttpBasicUsername||||x|| authenticationHttpHeaderValue|||x|||x authenticationScheme||||x|| msgVpnName|x|x|||| outgoingConnectionCount||||x|| remoteHost||||x|| remotePort||||x|| restConsumerName|x|x|||| restDeliveryPointName|x|x|||| tlsCipherSuiteList||||x|| tlsEnabled||||x||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- MsgVpnRestDeliveryPointRestConsumer|authenticationClientCertPassword|authenticationClientCertContent| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicPassword|authenticationHttpBasicUsername| MsgVpnRestDeliveryPointRestConsumer|authenticationHttpBasicUsername|authenticationHttpBasicPassword| MsgVpnRestDeliveryPointRestConsumer|remotePort|tlsEnabled| MsgVpnRestDeliveryPointRestConsumer|tlsEnabled|remotePort|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.0.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnRestDeliveryPointRestConsumerTest() throws ApiException {
        MsgVpnRestDeliveryPointRestConsumer body = null;
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumerResponse response = api.updateMsgVpnRestDeliveryPointRestConsumer(body, msgVpnName, restDeliveryPointName, restConsumerName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Topic Endpoint object.
     *
     * Update a Topic Endpoint object. Any attribute missing from the request will be left unchanged.  A Topic Endpoint attracts messages published to a topic for which the Topic Endpoint has a matching topic subscription. The topic subscription for the Topic Endpoint is specified in the client request to bind a Flow to that Topic Endpoint. Queues are significantly more flexible than Topic Endpoints and are the recommended approach for most applications. The use of Topic Endpoints should be restricted to JMS applications.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: accessType||||x|| msgVpnName|x|x|||| owner||||x|| permission||||x|| respectMsgPriorityEnabled||||x|| topicEndpointName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.1.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnTopicEndpointTest() throws ApiException {
        MsgVpnTopicEndpoint body = null;
        String msgVpnName = null;
        String topicEndpointName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnTopicEndpointResponse response = api.updateMsgVpnTopicEndpoint(body, msgVpnName, topicEndpointName, opaquePassword, select);

        // TODO: test validations
    }
    /**
     * Update a Topic Endpoint Template object.
     *
     * Update a Topic Endpoint Template object. Any attribute missing from the request will be left unchanged.  A Topic Endpoint Template provides a mechanism for specifying the initial state for client created topic endpoints.   Attribute|Identifying|Read-Only|Write-Only|Requires-Disable|Deprecated|Opaque :---|:---:|:---:|:---:|:---:|:---:|:---: msgVpnName|x|x|||| topicEndpointTemplateName|x|x||||    The following attributes in the request may only be provided in certain combinations with other attributes:   Class|Attribute|Requires|Conflicts :---|:---|:---|:--- EventThreshold|clearPercent|setPercent|clearValue, setValue EventThreshold|clearValue|setValue|clearPercent, setPercent EventThreshold|setPercent|clearPercent|clearValue, setValue EventThreshold|setValue|clearValue|clearPercent, setPercent    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-write\&quot; is required to perform this operation.  This has been available since 2.14.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void updateMsgVpnTopicEndpointTemplateTest() throws ApiException {
        MsgVpnTopicEndpointTemplate body = null;
        String msgVpnName = null;
        String topicEndpointTemplateName = null;
        String opaquePassword = null;
        List<String> select = null;
        MsgVpnTopicEndpointTemplateResponse response = api.updateMsgVpnTopicEndpointTemplate(body, msgVpnName, topicEndpointTemplateName, opaquePassword, select);

        // TODO: test validations
    }
}
