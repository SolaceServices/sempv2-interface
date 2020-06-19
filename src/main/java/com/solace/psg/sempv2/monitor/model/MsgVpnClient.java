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


package com.solace.psg.sempv2.monitor.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * MsgVpnClient
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-13T23:07:13.589Z")
public class MsgVpnClient {
  @SerializedName("aclProfileName")
  private String aclProfileName = null;

  @SerializedName("aliasedFromMsgVpnName")
  private String aliasedFromMsgVpnName = null;

  @SerializedName("alreadyBoundBindFailureCount")
  private Long alreadyBoundBindFailureCount = null;

  @SerializedName("authorizationGroupName")
  private String authorizationGroupName = null;

  @SerializedName("averageRxByteRate")
  private Long averageRxByteRate = null;

  @SerializedName("averageRxMsgRate")
  private Long averageRxMsgRate = null;

  @SerializedName("averageTxByteRate")
  private Long averageTxByteRate = null;

  @SerializedName("averageTxMsgRate")
  private Long averageTxMsgRate = null;

  @SerializedName("bindRequestCount")
  private Long bindRequestCount = null;

  @SerializedName("bindSuccessCount")
  private Long bindSuccessCount = null;

  @SerializedName("clientAddress")
  private String clientAddress = null;

  @SerializedName("clientId")
  private Integer clientId = null;

  @SerializedName("clientName")
  private String clientName = null;

  @SerializedName("clientProfileName")
  private String clientProfileName = null;

  @SerializedName("clientUsername")
  private String clientUsername = null;

  @SerializedName("controlRxByteCount")
  private Long controlRxByteCount = null;

  @SerializedName("controlRxMsgCount")
  private Long controlRxMsgCount = null;

  @SerializedName("controlTxByteCount")
  private Long controlTxByteCount = null;

  @SerializedName("controlTxMsgCount")
  private Long controlTxMsgCount = null;

  @SerializedName("cutThroughDeniedBindFailureCount")
  private Long cutThroughDeniedBindFailureCount = null;

  @SerializedName("dataRxByteCount")
  private Long dataRxByteCount = null;

  @SerializedName("dataRxMsgCount")
  private Long dataRxMsgCount = null;

  @SerializedName("dataTxByteCount")
  private Long dataTxByteCount = null;

  @SerializedName("dataTxMsgCount")
  private Long dataTxMsgCount = null;

  @SerializedName("description")
  private String description = null;

  @SerializedName("disabledBindFailureCount")
  private Long disabledBindFailureCount = null;

  @SerializedName("dtoLocalPriority")
  private Integer dtoLocalPriority = null;

  @SerializedName("dtoNetworkPriority")
  private Integer dtoNetworkPriority = null;

  @SerializedName("eliding")
  private Boolean eliding = null;

  @SerializedName("elidingTopicCount")
  private Integer elidingTopicCount = null;

  @SerializedName("elidingTopicPeakCount")
  private Integer elidingTopicPeakCount = null;

  @SerializedName("guaranteedDeniedBindFailureCount")
  private Long guaranteedDeniedBindFailureCount = null;

  @SerializedName("invalidSelectorBindFailureCount")
  private Long invalidSelectorBindFailureCount = null;

  @SerializedName("largeMsgEventRaised")
  private Boolean largeMsgEventRaised = null;

  @SerializedName("loginRxMsgCount")
  private Long loginRxMsgCount = null;

  @SerializedName("loginTxMsgCount")
  private Long loginTxMsgCount = null;

  @SerializedName("maxBindCountExceededBindFailureCount")
  private Long maxBindCountExceededBindFailureCount = null;

  @SerializedName("maxElidingTopicCountEventRaised")
  private Boolean maxElidingTopicCountEventRaised = null;

  @SerializedName("mqttConnackErrorTxCount")
  private Long mqttConnackErrorTxCount = null;

  @SerializedName("mqttConnackTxCount")
  private Long mqttConnackTxCount = null;

  @SerializedName("mqttConnectRxCount")
  private Long mqttConnectRxCount = null;

  @SerializedName("mqttDisconnectRxCount")
  private Long mqttDisconnectRxCount = null;

  @SerializedName("mqttPingreqRxCount")
  private Long mqttPingreqRxCount = null;

  @SerializedName("mqttPingrespTxCount")
  private Long mqttPingrespTxCount = null;

  @SerializedName("mqttPubackRxCount")
  private Long mqttPubackRxCount = null;

  @SerializedName("mqttPubackTxCount")
  private Long mqttPubackTxCount = null;

  @SerializedName("mqttPubcompTxCount")
  private Long mqttPubcompTxCount = null;

  @SerializedName("mqttPublishQos0RxCount")
  private Long mqttPublishQos0RxCount = null;

  @SerializedName("mqttPublishQos0TxCount")
  private Long mqttPublishQos0TxCount = null;

  @SerializedName("mqttPublishQos1RxCount")
  private Long mqttPublishQos1RxCount = null;

  @SerializedName("mqttPublishQos1TxCount")
  private Long mqttPublishQos1TxCount = null;

  @SerializedName("mqttPublishQos2RxCount")
  private Long mqttPublishQos2RxCount = null;

  @SerializedName("mqttPubrecTxCount")
  private Long mqttPubrecTxCount = null;

  @SerializedName("mqttPubrelRxCount")
  private Long mqttPubrelRxCount = null;

  @SerializedName("mqttSubackErrorTxCount")
  private Long mqttSubackErrorTxCount = null;

  @SerializedName("mqttSubackTxCount")
  private Long mqttSubackTxCount = null;

  @SerializedName("mqttSubscribeRxCount")
  private Long mqttSubscribeRxCount = null;

  @SerializedName("mqttUnsubackTxCount")
  private Long mqttUnsubackTxCount = null;

  @SerializedName("mqttUnsubscribeRxCount")
  private Long mqttUnsubscribeRxCount = null;

  @SerializedName("msgSpoolCongestionRxDiscardedMsgCount")
  private Long msgSpoolCongestionRxDiscardedMsgCount = null;

  @SerializedName("msgSpoolRxDiscardedMsgCount")
  private Long msgSpoolRxDiscardedMsgCount = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("noLocalDelivery")
  private Boolean noLocalDelivery = null;

  @SerializedName("noSubscriptionMatchRxDiscardedMsgCount")
  private Long noSubscriptionMatchRxDiscardedMsgCount = null;

  @SerializedName("originalClientUsername")
  private String originalClientUsername = null;

  @SerializedName("otherBindFailureCount")
  private Long otherBindFailureCount = null;

  @SerializedName("platform")
  private String platform = null;

  @SerializedName("publishTopicAclRxDiscardedMsgCount")
  private Long publishTopicAclRxDiscardedMsgCount = null;

  @SerializedName("restHttpRequestRxByteCount")
  private Long restHttpRequestRxByteCount = null;

  @SerializedName("restHttpRequestRxMsgCount")
  private Long restHttpRequestRxMsgCount = null;

  @SerializedName("restHttpRequestTxByteCount")
  private Long restHttpRequestTxByteCount = null;

  @SerializedName("restHttpRequestTxMsgCount")
  private Long restHttpRequestTxMsgCount = null;

  @SerializedName("restHttpResponseErrorRxMsgCount")
  private Long restHttpResponseErrorRxMsgCount = null;

  @SerializedName("restHttpResponseErrorTxMsgCount")
  private Long restHttpResponseErrorTxMsgCount = null;

  @SerializedName("restHttpResponseRxByteCount")
  private Long restHttpResponseRxByteCount = null;

  @SerializedName("restHttpResponseRxMsgCount")
  private Long restHttpResponseRxMsgCount = null;

  @SerializedName("restHttpResponseSuccessRxMsgCount")
  private Long restHttpResponseSuccessRxMsgCount = null;

  @SerializedName("restHttpResponseSuccessTxMsgCount")
  private Long restHttpResponseSuccessTxMsgCount = null;

  @SerializedName("restHttpResponseTimeoutRxMsgCount")
  private Long restHttpResponseTimeoutRxMsgCount = null;

  @SerializedName("restHttpResponseTimeoutTxMsgCount")
  private Long restHttpResponseTimeoutTxMsgCount = null;

  @SerializedName("restHttpResponseTxByteCount")
  private Long restHttpResponseTxByteCount = null;

  @SerializedName("restHttpResponseTxMsgCount")
  private Long restHttpResponseTxMsgCount = null;

  @SerializedName("rxByteCount")
  private Long rxByteCount = null;

  @SerializedName("rxByteRate")
  private Long rxByteRate = null;

  @SerializedName("rxDiscardedMsgCount")
  private Long rxDiscardedMsgCount = null;

  @SerializedName("rxMsgCount")
  private Long rxMsgCount = null;

  @SerializedName("rxMsgRate")
  private Long rxMsgRate = null;

  @SerializedName("scheduledDisconnectTime")
  private Integer scheduledDisconnectTime = null;

  @SerializedName("slowSubscriber")
  private Boolean slowSubscriber = null;

  @SerializedName("softwareDate")
  private String softwareDate = null;

  @SerializedName("softwareVersion")
  private String softwareVersion = null;

  @SerializedName("tlsCipherDescription")
  private String tlsCipherDescription = null;

  @SerializedName("tlsDowngradedToPlainText")
  private Boolean tlsDowngradedToPlainText = null;

  @SerializedName("tlsVersion")
  private String tlsVersion = null;

  @SerializedName("topicParseErrorRxDiscardedMsgCount")
  private Long topicParseErrorRxDiscardedMsgCount = null;

  @SerializedName("txByteCount")
  private Long txByteCount = null;

  @SerializedName("txByteRate")
  private Long txByteRate = null;

  @SerializedName("txDiscardedMsgCount")
  private Long txDiscardedMsgCount = null;

  @SerializedName("txMsgCount")
  private Long txMsgCount = null;

  @SerializedName("txMsgRate")
  private Long txMsgRate = null;

  @SerializedName("uptime")
  private Integer uptime = null;

  @SerializedName("user")
  private String user = null;

  @SerializedName("virtualRouter")
  private String virtualRouter = null;

  @SerializedName("webInactiveTimeout")
  private Integer webInactiveTimeout = null;

  @SerializedName("webMaxPayload")
  private Long webMaxPayload = null;

  @SerializedName("webParseErrorRxDiscardedMsgCount")
  private Long webParseErrorRxDiscardedMsgCount = null;

  @SerializedName("webRemainingTimeout")
  private Integer webRemainingTimeout = null;

  @SerializedName("webRxByteCount")
  private Long webRxByteCount = null;

  @SerializedName("webRxEncoding")
  private String webRxEncoding = null;

  @SerializedName("webRxMsgCount")
  private Long webRxMsgCount = null;

  @SerializedName("webRxProtocol")
  private String webRxProtocol = null;

  @SerializedName("webRxRequestCount")
  private Long webRxRequestCount = null;

  @SerializedName("webRxResponseCount")
  private Long webRxResponseCount = null;

  @SerializedName("webRxTcpState")
  private String webRxTcpState = null;

  @SerializedName("webRxTlsCipherDescription")
  private String webRxTlsCipherDescription = null;

  @SerializedName("webRxTlsVersion")
  private String webRxTlsVersion = null;

  @SerializedName("webSessionId")
  private String webSessionId = null;

  @SerializedName("webTxByteCount")
  private Long webTxByteCount = null;

  @SerializedName("webTxEncoding")
  private String webTxEncoding = null;

  @SerializedName("webTxMsgCount")
  private Long webTxMsgCount = null;

  @SerializedName("webTxProtocol")
  private String webTxProtocol = null;

  @SerializedName("webTxRequestCount")
  private Long webTxRequestCount = null;

  @SerializedName("webTxResponseCount")
  private Long webTxResponseCount = null;

  @SerializedName("webTxTcpState")
  private String webTxTcpState = null;

  @SerializedName("webTxTlsCipherDescription")
  private String webTxTlsCipherDescription = null;

  @SerializedName("webTxTlsVersion")
  private String webTxTlsVersion = null;

  public MsgVpnClient aclProfileName(String aclProfileName) {
    this.aclProfileName = aclProfileName;
    return this;
  }

   /**
   * The name of the access control list (ACL) profile of the Client.
   * @return aclProfileName
  **/
  @ApiModelProperty(value = "The name of the access control list (ACL) profile of the Client.")
  public String getAclProfileName() {
    return aclProfileName;
  }

  public void setAclProfileName(String aclProfileName) {
    this.aclProfileName = aclProfileName;
  }

  public MsgVpnClient aliasedFromMsgVpnName(String aliasedFromMsgVpnName) {
    this.aliasedFromMsgVpnName = aliasedFromMsgVpnName;
    return this;
  }

   /**
   * The name of the original MsgVpn which the client signaled in. Available since 2.14.
   * @return aliasedFromMsgVpnName
  **/
  @ApiModelProperty(value = "The name of the original MsgVpn which the client signaled in. Available since 2.14.")
  public String getAliasedFromMsgVpnName() {
    return aliasedFromMsgVpnName;
  }

  public void setAliasedFromMsgVpnName(String aliasedFromMsgVpnName) {
    this.aliasedFromMsgVpnName = aliasedFromMsgVpnName;
  }

  public MsgVpnClient alreadyBoundBindFailureCount(Long alreadyBoundBindFailureCount) {
    this.alreadyBoundBindFailureCount = alreadyBoundBindFailureCount;
    return this;
  }

   /**
   * The number of Client bind failures due to endpoint being already bound.
   * @return alreadyBoundBindFailureCount
  **/
  @ApiModelProperty(value = "The number of Client bind failures due to endpoint being already bound.")
  public Long getAlreadyBoundBindFailureCount() {
    return alreadyBoundBindFailureCount;
  }

  public void setAlreadyBoundBindFailureCount(Long alreadyBoundBindFailureCount) {
    this.alreadyBoundBindFailureCount = alreadyBoundBindFailureCount;
  }

  public MsgVpnClient authorizationGroupName(String authorizationGroupName) {
    this.authorizationGroupName = authorizationGroupName;
    return this;
  }

   /**
   * The name of the authorization group of the Client.
   * @return authorizationGroupName
  **/
  @ApiModelProperty(value = "The name of the authorization group of the Client.")
  public String getAuthorizationGroupName() {
    return authorizationGroupName;
  }

  public void setAuthorizationGroupName(String authorizationGroupName) {
    this.authorizationGroupName = authorizationGroupName;
  }

  public MsgVpnClient averageRxByteRate(Long averageRxByteRate) {
    this.averageRxByteRate = averageRxByteRate;
    return this;
  }

   /**
   * The one minute average of the message rate received from the Client, in bytes per second (B/sec).
   * @return averageRxByteRate
  **/
  @ApiModelProperty(value = "The one minute average of the message rate received from the Client, in bytes per second (B/sec).")
  public Long getAverageRxByteRate() {
    return averageRxByteRate;
  }

  public void setAverageRxByteRate(Long averageRxByteRate) {
    this.averageRxByteRate = averageRxByteRate;
  }

  public MsgVpnClient averageRxMsgRate(Long averageRxMsgRate) {
    this.averageRxMsgRate = averageRxMsgRate;
    return this;
  }

   /**
   * The one minute average of the message rate received from the Client, in messages per second (msg/sec).
   * @return averageRxMsgRate
  **/
  @ApiModelProperty(value = "The one minute average of the message rate received from the Client, in messages per second (msg/sec).")
  public Long getAverageRxMsgRate() {
    return averageRxMsgRate;
  }

  public void setAverageRxMsgRate(Long averageRxMsgRate) {
    this.averageRxMsgRate = averageRxMsgRate;
  }

  public MsgVpnClient averageTxByteRate(Long averageTxByteRate) {
    this.averageTxByteRate = averageTxByteRate;
    return this;
  }

   /**
   * The one minute average of the message rate transmitted to the Client, in bytes per second (B/sec).
   * @return averageTxByteRate
  **/
  @ApiModelProperty(value = "The one minute average of the message rate transmitted to the Client, in bytes per second (B/sec).")
  public Long getAverageTxByteRate() {
    return averageTxByteRate;
  }

  public void setAverageTxByteRate(Long averageTxByteRate) {
    this.averageTxByteRate = averageTxByteRate;
  }

  public MsgVpnClient averageTxMsgRate(Long averageTxMsgRate) {
    this.averageTxMsgRate = averageTxMsgRate;
    return this;
  }

   /**
   * The one minute average of the message rate transmitted to the Client, in messages per second (msg/sec).
   * @return averageTxMsgRate
  **/
  @ApiModelProperty(value = "The one minute average of the message rate transmitted to the Client, in messages per second (msg/sec).")
  public Long getAverageTxMsgRate() {
    return averageTxMsgRate;
  }

  public void setAverageTxMsgRate(Long averageTxMsgRate) {
    this.averageTxMsgRate = averageTxMsgRate;
  }

  public MsgVpnClient bindRequestCount(Long bindRequestCount) {
    this.bindRequestCount = bindRequestCount;
    return this;
  }

   /**
   * The number of Client requests to bind to an endpoint.
   * @return bindRequestCount
  **/
  @ApiModelProperty(value = "The number of Client requests to bind to an endpoint.")
  public Long getBindRequestCount() {
    return bindRequestCount;
  }

  public void setBindRequestCount(Long bindRequestCount) {
    this.bindRequestCount = bindRequestCount;
  }

  public MsgVpnClient bindSuccessCount(Long bindSuccessCount) {
    this.bindSuccessCount = bindSuccessCount;
    return this;
  }

   /**
   * The number of successful Client requests to bind to an endpoint.
   * @return bindSuccessCount
  **/
  @ApiModelProperty(value = "The number of successful Client requests to bind to an endpoint.")
  public Long getBindSuccessCount() {
    return bindSuccessCount;
  }

  public void setBindSuccessCount(Long bindSuccessCount) {
    this.bindSuccessCount = bindSuccessCount;
  }

  public MsgVpnClient clientAddress(String clientAddress) {
    this.clientAddress = clientAddress;
    return this;
  }

   /**
   * The IP address and port of the Client.
   * @return clientAddress
  **/
  @ApiModelProperty(value = "The IP address and port of the Client.")
  public String getClientAddress() {
    return clientAddress;
  }

  public void setClientAddress(String clientAddress) {
    this.clientAddress = clientAddress;
  }

  public MsgVpnClient clientId(Integer clientId) {
    this.clientId = clientId;
    return this;
  }

   /**
   * The identifier (ID) of the Client.
   * @return clientId
  **/
  @ApiModelProperty(value = "The identifier (ID) of the Client.")
  public Integer getClientId() {
    return clientId;
  }

  public void setClientId(Integer clientId) {
    this.clientId = clientId;
  }

  public MsgVpnClient clientName(String clientName) {
    this.clientName = clientName;
    return this;
  }

   /**
   * The name of the Client.
   * @return clientName
  **/
  @ApiModelProperty(value = "The name of the Client.")
  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public MsgVpnClient clientProfileName(String clientProfileName) {
    this.clientProfileName = clientProfileName;
    return this;
  }

   /**
   * The name of the client profile of the Client.
   * @return clientProfileName
  **/
  @ApiModelProperty(value = "The name of the client profile of the Client.")
  public String getClientProfileName() {
    return clientProfileName;
  }

  public void setClientProfileName(String clientProfileName) {
    this.clientProfileName = clientProfileName;
  }

  public MsgVpnClient clientUsername(String clientUsername) {
    this.clientUsername = clientUsername;
    return this;
  }

   /**
   * The client username of the Client used for authorization.
   * @return clientUsername
  **/
  @ApiModelProperty(value = "The client username of the Client used for authorization.")
  public String getClientUsername() {
    return clientUsername;
  }

  public void setClientUsername(String clientUsername) {
    this.clientUsername = clientUsername;
  }

  public MsgVpnClient controlRxByteCount(Long controlRxByteCount) {
    this.controlRxByteCount = controlRxByteCount;
    return this;
  }

   /**
   * The amount of client control messages received from the Client, in bytes (B).
   * @return controlRxByteCount
  **/
  @ApiModelProperty(value = "The amount of client control messages received from the Client, in bytes (B).")
  public Long getControlRxByteCount() {
    return controlRxByteCount;
  }

  public void setControlRxByteCount(Long controlRxByteCount) {
    this.controlRxByteCount = controlRxByteCount;
  }

  public MsgVpnClient controlRxMsgCount(Long controlRxMsgCount) {
    this.controlRxMsgCount = controlRxMsgCount;
    return this;
  }

   /**
   * The number of client control messages received from the Client.
   * @return controlRxMsgCount
  **/
  @ApiModelProperty(value = "The number of client control messages received from the Client.")
  public Long getControlRxMsgCount() {
    return controlRxMsgCount;
  }

  public void setControlRxMsgCount(Long controlRxMsgCount) {
    this.controlRxMsgCount = controlRxMsgCount;
  }

  public MsgVpnClient controlTxByteCount(Long controlTxByteCount) {
    this.controlTxByteCount = controlTxByteCount;
    return this;
  }

   /**
   * The amount of client control messages transmitted to the Client, in bytes (B).
   * @return controlTxByteCount
  **/
  @ApiModelProperty(value = "The amount of client control messages transmitted to the Client, in bytes (B).")
  public Long getControlTxByteCount() {
    return controlTxByteCount;
  }

  public void setControlTxByteCount(Long controlTxByteCount) {
    this.controlTxByteCount = controlTxByteCount;
  }

  public MsgVpnClient controlTxMsgCount(Long controlTxMsgCount) {
    this.controlTxMsgCount = controlTxMsgCount;
    return this;
  }

   /**
   * The number of client control messages transmitted to the Client.
   * @return controlTxMsgCount
  **/
  @ApiModelProperty(value = "The number of client control messages transmitted to the Client.")
  public Long getControlTxMsgCount() {
    return controlTxMsgCount;
  }

  public void setControlTxMsgCount(Long controlTxMsgCount) {
    this.controlTxMsgCount = controlTxMsgCount;
  }

  public MsgVpnClient cutThroughDeniedBindFailureCount(Long cutThroughDeniedBindFailureCount) {
    this.cutThroughDeniedBindFailureCount = cutThroughDeniedBindFailureCount;
    return this;
  }

   /**
   * The number of Client bind failures due to being denied cut-through forwarding.
   * @return cutThroughDeniedBindFailureCount
  **/
  @ApiModelProperty(value = "The number of Client bind failures due to being denied cut-through forwarding.")
  public Long getCutThroughDeniedBindFailureCount() {
    return cutThroughDeniedBindFailureCount;
  }

  public void setCutThroughDeniedBindFailureCount(Long cutThroughDeniedBindFailureCount) {
    this.cutThroughDeniedBindFailureCount = cutThroughDeniedBindFailureCount;
  }

  public MsgVpnClient dataRxByteCount(Long dataRxByteCount) {
    this.dataRxByteCount = dataRxByteCount;
    return this;
  }

   /**
   * The amount of client data messages received from the Client, in bytes (B).
   * @return dataRxByteCount
  **/
  @ApiModelProperty(value = "The amount of client data messages received from the Client, in bytes (B).")
  public Long getDataRxByteCount() {
    return dataRxByteCount;
  }

  public void setDataRxByteCount(Long dataRxByteCount) {
    this.dataRxByteCount = dataRxByteCount;
  }

  public MsgVpnClient dataRxMsgCount(Long dataRxMsgCount) {
    this.dataRxMsgCount = dataRxMsgCount;
    return this;
  }

   /**
   * The number of client data messages received from the Client.
   * @return dataRxMsgCount
  **/
  @ApiModelProperty(value = "The number of client data messages received from the Client.")
  public Long getDataRxMsgCount() {
    return dataRxMsgCount;
  }

  public void setDataRxMsgCount(Long dataRxMsgCount) {
    this.dataRxMsgCount = dataRxMsgCount;
  }

  public MsgVpnClient dataTxByteCount(Long dataTxByteCount) {
    this.dataTxByteCount = dataTxByteCount;
    return this;
  }

   /**
   * The amount of client data messages transmitted to the Client, in bytes (B).
   * @return dataTxByteCount
  **/
  @ApiModelProperty(value = "The amount of client data messages transmitted to the Client, in bytes (B).")
  public Long getDataTxByteCount() {
    return dataTxByteCount;
  }

  public void setDataTxByteCount(Long dataTxByteCount) {
    this.dataTxByteCount = dataTxByteCount;
  }

  public MsgVpnClient dataTxMsgCount(Long dataTxMsgCount) {
    this.dataTxMsgCount = dataTxMsgCount;
    return this;
  }

   /**
   * The number of client data messages transmitted to the Client.
   * @return dataTxMsgCount
  **/
  @ApiModelProperty(value = "The number of client data messages transmitted to the Client.")
  public Long getDataTxMsgCount() {
    return dataTxMsgCount;
  }

  public void setDataTxMsgCount(Long dataTxMsgCount) {
    this.dataTxMsgCount = dataTxMsgCount;
  }

  public MsgVpnClient description(String description) {
    this.description = description;
    return this;
  }

   /**
   * The description text of the Client.
   * @return description
  **/
  @ApiModelProperty(value = "The description text of the Client.")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public MsgVpnClient disabledBindFailureCount(Long disabledBindFailureCount) {
    this.disabledBindFailureCount = disabledBindFailureCount;
    return this;
  }

   /**
   * The number of Client bind failures due to endpoint being disabled.
   * @return disabledBindFailureCount
  **/
  @ApiModelProperty(value = "The number of Client bind failures due to endpoint being disabled.")
  public Long getDisabledBindFailureCount() {
    return disabledBindFailureCount;
  }

  public void setDisabledBindFailureCount(Long disabledBindFailureCount) {
    this.disabledBindFailureCount = disabledBindFailureCount;
  }

  public MsgVpnClient dtoLocalPriority(Integer dtoLocalPriority) {
    this.dtoLocalPriority = dtoLocalPriority;
    return this;
  }

   /**
   * The priority of the Client&#39;s subscriptions for receiving deliver-to-one (DTO) messages published on the local broker.
   * @return dtoLocalPriority
  **/
  @ApiModelProperty(value = "The priority of the Client's subscriptions for receiving deliver-to-one (DTO) messages published on the local broker.")
  public Integer getDtoLocalPriority() {
    return dtoLocalPriority;
  }

  public void setDtoLocalPriority(Integer dtoLocalPriority) {
    this.dtoLocalPriority = dtoLocalPriority;
  }

  public MsgVpnClient dtoNetworkPriority(Integer dtoNetworkPriority) {
    this.dtoNetworkPriority = dtoNetworkPriority;
    return this;
  }

   /**
   * The priority of the Client&#39;s subscriptions for receiving deliver-to-one (DTO) messages published on a remote broker.
   * @return dtoNetworkPriority
  **/
  @ApiModelProperty(value = "The priority of the Client's subscriptions for receiving deliver-to-one (DTO) messages published on a remote broker.")
  public Integer getDtoNetworkPriority() {
    return dtoNetworkPriority;
  }

  public void setDtoNetworkPriority(Integer dtoNetworkPriority) {
    this.dtoNetworkPriority = dtoNetworkPriority;
  }

  public MsgVpnClient eliding(Boolean eliding) {
    this.eliding = eliding;
    return this;
  }

   /**
   * Indicates whether message eliding is enabled for the Client.
   * @return eliding
  **/
  @ApiModelProperty(value = "Indicates whether message eliding is enabled for the Client.")
  public Boolean isEliding() {
    return eliding;
  }

  public void setEliding(Boolean eliding) {
    this.eliding = eliding;
  }

  public MsgVpnClient elidingTopicCount(Integer elidingTopicCount) {
    this.elidingTopicCount = elidingTopicCount;
    return this;
  }

   /**
   * The number of topics requiring message eliding for the Client.
   * @return elidingTopicCount
  **/
  @ApiModelProperty(value = "The number of topics requiring message eliding for the Client.")
  public Integer getElidingTopicCount() {
    return elidingTopicCount;
  }

  public void setElidingTopicCount(Integer elidingTopicCount) {
    this.elidingTopicCount = elidingTopicCount;
  }

  public MsgVpnClient elidingTopicPeakCount(Integer elidingTopicPeakCount) {
    this.elidingTopicPeakCount = elidingTopicPeakCount;
    return this;
  }

   /**
   * The peak number of topics requiring message eliding for the Client.
   * @return elidingTopicPeakCount
  **/
  @ApiModelProperty(value = "The peak number of topics requiring message eliding for the Client.")
  public Integer getElidingTopicPeakCount() {
    return elidingTopicPeakCount;
  }

  public void setElidingTopicPeakCount(Integer elidingTopicPeakCount) {
    this.elidingTopicPeakCount = elidingTopicPeakCount;
  }

  public MsgVpnClient guaranteedDeniedBindFailureCount(Long guaranteedDeniedBindFailureCount) {
    this.guaranteedDeniedBindFailureCount = guaranteedDeniedBindFailureCount;
    return this;
  }

   /**
   * The number of Client bind failures due to being denied guaranteed messaging.
   * @return guaranteedDeniedBindFailureCount
  **/
  @ApiModelProperty(value = "The number of Client bind failures due to being denied guaranteed messaging.")
  public Long getGuaranteedDeniedBindFailureCount() {
    return guaranteedDeniedBindFailureCount;
  }

  public void setGuaranteedDeniedBindFailureCount(Long guaranteedDeniedBindFailureCount) {
    this.guaranteedDeniedBindFailureCount = guaranteedDeniedBindFailureCount;
  }

  public MsgVpnClient invalidSelectorBindFailureCount(Long invalidSelectorBindFailureCount) {
    this.invalidSelectorBindFailureCount = invalidSelectorBindFailureCount;
    return this;
  }

   /**
   * The number of Client bind failures due to an invalid selector.
   * @return invalidSelectorBindFailureCount
  **/
  @ApiModelProperty(value = "The number of Client bind failures due to an invalid selector.")
  public Long getInvalidSelectorBindFailureCount() {
    return invalidSelectorBindFailureCount;
  }

  public void setInvalidSelectorBindFailureCount(Long invalidSelectorBindFailureCount) {
    this.invalidSelectorBindFailureCount = invalidSelectorBindFailureCount;
  }

  public MsgVpnClient largeMsgEventRaised(Boolean largeMsgEventRaised) {
    this.largeMsgEventRaised = largeMsgEventRaised;
    return this;
  }

   /**
   * Indicates whether the large-message event has been raised for the Client.
   * @return largeMsgEventRaised
  **/
  @ApiModelProperty(value = "Indicates whether the large-message event has been raised for the Client.")
  public Boolean isLargeMsgEventRaised() {
    return largeMsgEventRaised;
  }

  public void setLargeMsgEventRaised(Boolean largeMsgEventRaised) {
    this.largeMsgEventRaised = largeMsgEventRaised;
  }

  public MsgVpnClient loginRxMsgCount(Long loginRxMsgCount) {
    this.loginRxMsgCount = loginRxMsgCount;
    return this;
  }

   /**
   * The number of login request messages received from the Client.
   * @return loginRxMsgCount
  **/
  @ApiModelProperty(value = "The number of login request messages received from the Client.")
  public Long getLoginRxMsgCount() {
    return loginRxMsgCount;
  }

  public void setLoginRxMsgCount(Long loginRxMsgCount) {
    this.loginRxMsgCount = loginRxMsgCount;
  }

  public MsgVpnClient loginTxMsgCount(Long loginTxMsgCount) {
    this.loginTxMsgCount = loginTxMsgCount;
    return this;
  }

   /**
   * The number of login response messages transmitted to the Client.
   * @return loginTxMsgCount
  **/
  @ApiModelProperty(value = "The number of login response messages transmitted to the Client.")
  public Long getLoginTxMsgCount() {
    return loginTxMsgCount;
  }

  public void setLoginTxMsgCount(Long loginTxMsgCount) {
    this.loginTxMsgCount = loginTxMsgCount;
  }

  public MsgVpnClient maxBindCountExceededBindFailureCount(Long maxBindCountExceededBindFailureCount) {
    this.maxBindCountExceededBindFailureCount = maxBindCountExceededBindFailureCount;
    return this;
  }

   /**
   * The number of Client bind failures due to the endpoint maximum bind count being exceeded.
   * @return maxBindCountExceededBindFailureCount
  **/
  @ApiModelProperty(value = "The number of Client bind failures due to the endpoint maximum bind count being exceeded.")
  public Long getMaxBindCountExceededBindFailureCount() {
    return maxBindCountExceededBindFailureCount;
  }

  public void setMaxBindCountExceededBindFailureCount(Long maxBindCountExceededBindFailureCount) {
    this.maxBindCountExceededBindFailureCount = maxBindCountExceededBindFailureCount;
  }

  public MsgVpnClient maxElidingTopicCountEventRaised(Boolean maxElidingTopicCountEventRaised) {
    this.maxElidingTopicCountEventRaised = maxElidingTopicCountEventRaised;
    return this;
  }

   /**
   * Indicates whether the max-eliding-topic-count event has been raised for the Client.
   * @return maxElidingTopicCountEventRaised
  **/
  @ApiModelProperty(value = "Indicates whether the max-eliding-topic-count event has been raised for the Client.")
  public Boolean isMaxElidingTopicCountEventRaised() {
    return maxElidingTopicCountEventRaised;
  }

  public void setMaxElidingTopicCountEventRaised(Boolean maxElidingTopicCountEventRaised) {
    this.maxElidingTopicCountEventRaised = maxElidingTopicCountEventRaised;
  }

  public MsgVpnClient mqttConnackErrorTxCount(Long mqttConnackErrorTxCount) {
    this.mqttConnackErrorTxCount = mqttConnackErrorTxCount;
    return this;
  }

   /**
   * The number of MQTT connect acknowledgment (CONNACK) refused response packets transmitted to the Client.
   * @return mqttConnackErrorTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT connect acknowledgment (CONNACK) refused response packets transmitted to the Client.")
  public Long getMqttConnackErrorTxCount() {
    return mqttConnackErrorTxCount;
  }

  public void setMqttConnackErrorTxCount(Long mqttConnackErrorTxCount) {
    this.mqttConnackErrorTxCount = mqttConnackErrorTxCount;
  }

  public MsgVpnClient mqttConnackTxCount(Long mqttConnackTxCount) {
    this.mqttConnackTxCount = mqttConnackTxCount;
    return this;
  }

   /**
   * The number of MQTT connect acknowledgment (CONNACK) accepted response packets transmitted to the Client.
   * @return mqttConnackTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT connect acknowledgment (CONNACK) accepted response packets transmitted to the Client.")
  public Long getMqttConnackTxCount() {
    return mqttConnackTxCount;
  }

  public void setMqttConnackTxCount(Long mqttConnackTxCount) {
    this.mqttConnackTxCount = mqttConnackTxCount;
  }

  public MsgVpnClient mqttConnectRxCount(Long mqttConnectRxCount) {
    this.mqttConnectRxCount = mqttConnectRxCount;
    return this;
  }

   /**
   * The number of MQTT connect (CONNECT) request packets received from the Client.
   * @return mqttConnectRxCount
  **/
  @ApiModelProperty(value = "The number of MQTT connect (CONNECT) request packets received from the Client.")
  public Long getMqttConnectRxCount() {
    return mqttConnectRxCount;
  }

  public void setMqttConnectRxCount(Long mqttConnectRxCount) {
    this.mqttConnectRxCount = mqttConnectRxCount;
  }

  public MsgVpnClient mqttDisconnectRxCount(Long mqttDisconnectRxCount) {
    this.mqttDisconnectRxCount = mqttDisconnectRxCount;
    return this;
  }

   /**
   * The number of MQTT disconnect (DISCONNECT) request packets received from the Client.
   * @return mqttDisconnectRxCount
  **/
  @ApiModelProperty(value = "The number of MQTT disconnect (DISCONNECT) request packets received from the Client.")
  public Long getMqttDisconnectRxCount() {
    return mqttDisconnectRxCount;
  }

  public void setMqttDisconnectRxCount(Long mqttDisconnectRxCount) {
    this.mqttDisconnectRxCount = mqttDisconnectRxCount;
  }

  public MsgVpnClient mqttPingreqRxCount(Long mqttPingreqRxCount) {
    this.mqttPingreqRxCount = mqttPingreqRxCount;
    return this;
  }

   /**
   * The number of MQTT ping request (PINGREQ) packets received from the Client.
   * @return mqttPingreqRxCount
  **/
  @ApiModelProperty(value = "The number of MQTT ping request (PINGREQ) packets received from the Client.")
  public Long getMqttPingreqRxCount() {
    return mqttPingreqRxCount;
  }

  public void setMqttPingreqRxCount(Long mqttPingreqRxCount) {
    this.mqttPingreqRxCount = mqttPingreqRxCount;
  }

  public MsgVpnClient mqttPingrespTxCount(Long mqttPingrespTxCount) {
    this.mqttPingrespTxCount = mqttPingrespTxCount;
    return this;
  }

   /**
   * The number of MQTT ping response (PINGRESP) packets transmitted to the Client.
   * @return mqttPingrespTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT ping response (PINGRESP) packets transmitted to the Client.")
  public Long getMqttPingrespTxCount() {
    return mqttPingrespTxCount;
  }

  public void setMqttPingrespTxCount(Long mqttPingrespTxCount) {
    this.mqttPingrespTxCount = mqttPingrespTxCount;
  }

  public MsgVpnClient mqttPubackRxCount(Long mqttPubackRxCount) {
    this.mqttPubackRxCount = mqttPubackRxCount;
    return this;
  }

   /**
   * The number of MQTT publish acknowledgement (PUBACK) response packets received from the Client.
   * @return mqttPubackRxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish acknowledgement (PUBACK) response packets received from the Client.")
  public Long getMqttPubackRxCount() {
    return mqttPubackRxCount;
  }

  public void setMqttPubackRxCount(Long mqttPubackRxCount) {
    this.mqttPubackRxCount = mqttPubackRxCount;
  }

  public MsgVpnClient mqttPubackTxCount(Long mqttPubackTxCount) {
    this.mqttPubackTxCount = mqttPubackTxCount;
    return this;
  }

   /**
   * The number of MQTT publish acknowledgement (PUBACK) response packets transmitted to the Client.
   * @return mqttPubackTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish acknowledgement (PUBACK) response packets transmitted to the Client.")
  public Long getMqttPubackTxCount() {
    return mqttPubackTxCount;
  }

  public void setMqttPubackTxCount(Long mqttPubackTxCount) {
    this.mqttPubackTxCount = mqttPubackTxCount;
  }

  public MsgVpnClient mqttPubcompTxCount(Long mqttPubcompTxCount) {
    this.mqttPubcompTxCount = mqttPubcompTxCount;
    return this;
  }

   /**
   * The number of MQTT publish complete (PUBCOMP) packets transmitted to the Client in response to a PUBREL packet. These packets are the fourth and final packet of a QoS 2 protocol exchange.
   * @return mqttPubcompTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish complete (PUBCOMP) packets transmitted to the Client in response to a PUBREL packet. These packets are the fourth and final packet of a QoS 2 protocol exchange.")
  public Long getMqttPubcompTxCount() {
    return mqttPubcompTxCount;
  }

  public void setMqttPubcompTxCount(Long mqttPubcompTxCount) {
    this.mqttPubcompTxCount = mqttPubcompTxCount;
  }

  public MsgVpnClient mqttPublishQos0RxCount(Long mqttPublishQos0RxCount) {
    this.mqttPublishQos0RxCount = mqttPublishQos0RxCount;
    return this;
  }

   /**
   * The number of MQTT publish message (PUBLISH) request packets received from the Client for QoS 0 message delivery.
   * @return mqttPublishQos0RxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish message (PUBLISH) request packets received from the Client for QoS 0 message delivery.")
  public Long getMqttPublishQos0RxCount() {
    return mqttPublishQos0RxCount;
  }

  public void setMqttPublishQos0RxCount(Long mqttPublishQos0RxCount) {
    this.mqttPublishQos0RxCount = mqttPublishQos0RxCount;
  }

  public MsgVpnClient mqttPublishQos0TxCount(Long mqttPublishQos0TxCount) {
    this.mqttPublishQos0TxCount = mqttPublishQos0TxCount;
    return this;
  }

   /**
   * The number of MQTT publish message (PUBLISH) request packets transmitted to the Client for QoS 0 message delivery.
   * @return mqttPublishQos0TxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish message (PUBLISH) request packets transmitted to the Client for QoS 0 message delivery.")
  public Long getMqttPublishQos0TxCount() {
    return mqttPublishQos0TxCount;
  }

  public void setMqttPublishQos0TxCount(Long mqttPublishQos0TxCount) {
    this.mqttPublishQos0TxCount = mqttPublishQos0TxCount;
  }

  public MsgVpnClient mqttPublishQos1RxCount(Long mqttPublishQos1RxCount) {
    this.mqttPublishQos1RxCount = mqttPublishQos1RxCount;
    return this;
  }

   /**
   * The number of MQTT publish message (PUBLISH) request packets received from the Client for QoS 1 message delivery.
   * @return mqttPublishQos1RxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish message (PUBLISH) request packets received from the Client for QoS 1 message delivery.")
  public Long getMqttPublishQos1RxCount() {
    return mqttPublishQos1RxCount;
  }

  public void setMqttPublishQos1RxCount(Long mqttPublishQos1RxCount) {
    this.mqttPublishQos1RxCount = mqttPublishQos1RxCount;
  }

  public MsgVpnClient mqttPublishQos1TxCount(Long mqttPublishQos1TxCount) {
    this.mqttPublishQos1TxCount = mqttPublishQos1TxCount;
    return this;
  }

   /**
   * The number of MQTT publish message (PUBLISH) request packets transmitted to the Client for QoS 1 message delivery.
   * @return mqttPublishQos1TxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish message (PUBLISH) request packets transmitted to the Client for QoS 1 message delivery.")
  public Long getMqttPublishQos1TxCount() {
    return mqttPublishQos1TxCount;
  }

  public void setMqttPublishQos1TxCount(Long mqttPublishQos1TxCount) {
    this.mqttPublishQos1TxCount = mqttPublishQos1TxCount;
  }

  public MsgVpnClient mqttPublishQos2RxCount(Long mqttPublishQos2RxCount) {
    this.mqttPublishQos2RxCount = mqttPublishQos2RxCount;
    return this;
  }

   /**
   * The number of MQTT publish message (PUBLISH) request packets received from the Client for QoS 2 message delivery.
   * @return mqttPublishQos2RxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish message (PUBLISH) request packets received from the Client for QoS 2 message delivery.")
  public Long getMqttPublishQos2RxCount() {
    return mqttPublishQos2RxCount;
  }

  public void setMqttPublishQos2RxCount(Long mqttPublishQos2RxCount) {
    this.mqttPublishQos2RxCount = mqttPublishQos2RxCount;
  }

  public MsgVpnClient mqttPubrecTxCount(Long mqttPubrecTxCount) {
    this.mqttPubrecTxCount = mqttPubrecTxCount;
    return this;
  }

   /**
   * The number of MQTT publish received (PUBREC) packets transmitted to the Client in response to a PUBLISH packet with QoS 2. These packets are the second packet of a QoS 2 protocol exchange.
   * @return mqttPubrecTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish received (PUBREC) packets transmitted to the Client in response to a PUBLISH packet with QoS 2. These packets are the second packet of a QoS 2 protocol exchange.")
  public Long getMqttPubrecTxCount() {
    return mqttPubrecTxCount;
  }

  public void setMqttPubrecTxCount(Long mqttPubrecTxCount) {
    this.mqttPubrecTxCount = mqttPubrecTxCount;
  }

  public MsgVpnClient mqttPubrelRxCount(Long mqttPubrelRxCount) {
    this.mqttPubrelRxCount = mqttPubrelRxCount;
    return this;
  }

   /**
   * The number of MQTT publish release (PUBREL) packets received from the Client in response to a PUBREC packet. These packets are the third packet of a QoS 2 protocol exchange.
   * @return mqttPubrelRxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish release (PUBREL) packets received from the Client in response to a PUBREC packet. These packets are the third packet of a QoS 2 protocol exchange.")
  public Long getMqttPubrelRxCount() {
    return mqttPubrelRxCount;
  }

  public void setMqttPubrelRxCount(Long mqttPubrelRxCount) {
    this.mqttPubrelRxCount = mqttPubrelRxCount;
  }

  public MsgVpnClient mqttSubackErrorTxCount(Long mqttSubackErrorTxCount) {
    this.mqttSubackErrorTxCount = mqttSubackErrorTxCount;
    return this;
  }

   /**
   * The number of MQTT subscribe acknowledgement (SUBACK) failure response packets transmitted to the Client.
   * @return mqttSubackErrorTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT subscribe acknowledgement (SUBACK) failure response packets transmitted to the Client.")
  public Long getMqttSubackErrorTxCount() {
    return mqttSubackErrorTxCount;
  }

  public void setMqttSubackErrorTxCount(Long mqttSubackErrorTxCount) {
    this.mqttSubackErrorTxCount = mqttSubackErrorTxCount;
  }

  public MsgVpnClient mqttSubackTxCount(Long mqttSubackTxCount) {
    this.mqttSubackTxCount = mqttSubackTxCount;
    return this;
  }

   /**
   * The number of MQTT subscribe acknowledgement (SUBACK) response packets transmitted to the Client.
   * @return mqttSubackTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT subscribe acknowledgement (SUBACK) response packets transmitted to the Client.")
  public Long getMqttSubackTxCount() {
    return mqttSubackTxCount;
  }

  public void setMqttSubackTxCount(Long mqttSubackTxCount) {
    this.mqttSubackTxCount = mqttSubackTxCount;
  }

  public MsgVpnClient mqttSubscribeRxCount(Long mqttSubscribeRxCount) {
    this.mqttSubscribeRxCount = mqttSubscribeRxCount;
    return this;
  }

   /**
   * The number of MQTT subscribe (SUBSCRIBE) request packets received from the Client to create one or more topic subscriptions.
   * @return mqttSubscribeRxCount
  **/
  @ApiModelProperty(value = "The number of MQTT subscribe (SUBSCRIBE) request packets received from the Client to create one or more topic subscriptions.")
  public Long getMqttSubscribeRxCount() {
    return mqttSubscribeRxCount;
  }

  public void setMqttSubscribeRxCount(Long mqttSubscribeRxCount) {
    this.mqttSubscribeRxCount = mqttSubscribeRxCount;
  }

  public MsgVpnClient mqttUnsubackTxCount(Long mqttUnsubackTxCount) {
    this.mqttUnsubackTxCount = mqttUnsubackTxCount;
    return this;
  }

   /**
   * The number of MQTT unsubscribe acknowledgement (UNSUBACK) response packets transmitted to the Client.
   * @return mqttUnsubackTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT unsubscribe acknowledgement (UNSUBACK) response packets transmitted to the Client.")
  public Long getMqttUnsubackTxCount() {
    return mqttUnsubackTxCount;
  }

  public void setMqttUnsubackTxCount(Long mqttUnsubackTxCount) {
    this.mqttUnsubackTxCount = mqttUnsubackTxCount;
  }

  public MsgVpnClient mqttUnsubscribeRxCount(Long mqttUnsubscribeRxCount) {
    this.mqttUnsubscribeRxCount = mqttUnsubscribeRxCount;
    return this;
  }

   /**
   * The number of MQTT unsubscribe (UNSUBSCRIBE) request packets received from the Client to remove one or more topic subscriptions.
   * @return mqttUnsubscribeRxCount
  **/
  @ApiModelProperty(value = "The number of MQTT unsubscribe (UNSUBSCRIBE) request packets received from the Client to remove one or more topic subscriptions.")
  public Long getMqttUnsubscribeRxCount() {
    return mqttUnsubscribeRxCount;
  }

  public void setMqttUnsubscribeRxCount(Long mqttUnsubscribeRxCount) {
    this.mqttUnsubscribeRxCount = mqttUnsubscribeRxCount;
  }

  public MsgVpnClient msgSpoolCongestionRxDiscardedMsgCount(Long msgSpoolCongestionRxDiscardedMsgCount) {
    this.msgSpoolCongestionRxDiscardedMsgCount = msgSpoolCongestionRxDiscardedMsgCount;
    return this;
  }

   /**
   * The number of messages from the Client discarded due to message spool congestion primarily caused by message promotion.
   * @return msgSpoolCongestionRxDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of messages from the Client discarded due to message spool congestion primarily caused by message promotion.")
  public Long getMsgSpoolCongestionRxDiscardedMsgCount() {
    return msgSpoolCongestionRxDiscardedMsgCount;
  }

  public void setMsgSpoolCongestionRxDiscardedMsgCount(Long msgSpoolCongestionRxDiscardedMsgCount) {
    this.msgSpoolCongestionRxDiscardedMsgCount = msgSpoolCongestionRxDiscardedMsgCount;
  }

  public MsgVpnClient msgSpoolRxDiscardedMsgCount(Long msgSpoolRxDiscardedMsgCount) {
    this.msgSpoolRxDiscardedMsgCount = msgSpoolRxDiscardedMsgCount;
    return this;
  }

   /**
   * The number of messages from the Client discarded by the message spool.
   * @return msgSpoolRxDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of messages from the Client discarded by the message spool.")
  public Long getMsgSpoolRxDiscardedMsgCount() {
    return msgSpoolRxDiscardedMsgCount;
  }

  public void setMsgSpoolRxDiscardedMsgCount(Long msgSpoolRxDiscardedMsgCount) {
    this.msgSpoolRxDiscardedMsgCount = msgSpoolRxDiscardedMsgCount;
  }

  public MsgVpnClient msgVpnName(String msgVpnName) {
    this.msgVpnName = msgVpnName;
    return this;
  }

   /**
   * The name of the Message VPN.
   * @return msgVpnName
  **/
  @ApiModelProperty(value = "The name of the Message VPN.")
  public String getMsgVpnName() {
    return msgVpnName;
  }

  public void setMsgVpnName(String msgVpnName) {
    this.msgVpnName = msgVpnName;
  }

  public MsgVpnClient noLocalDelivery(Boolean noLocalDelivery) {
    this.noLocalDelivery = noLocalDelivery;
    return this;
  }

   /**
   * Indicates whether not to deliver messages to the Client if it published them.
   * @return noLocalDelivery
  **/
  @ApiModelProperty(value = "Indicates whether not to deliver messages to the Client if it published them.")
  public Boolean isNoLocalDelivery() {
    return noLocalDelivery;
  }

  public void setNoLocalDelivery(Boolean noLocalDelivery) {
    this.noLocalDelivery = noLocalDelivery;
  }

  public MsgVpnClient noSubscriptionMatchRxDiscardedMsgCount(Long noSubscriptionMatchRxDiscardedMsgCount) {
    this.noSubscriptionMatchRxDiscardedMsgCount = noSubscriptionMatchRxDiscardedMsgCount;
    return this;
  }

   /**
   * The number of messages from the Client discarded due to no matching subscription found.
   * @return noSubscriptionMatchRxDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of messages from the Client discarded due to no matching subscription found.")
  public Long getNoSubscriptionMatchRxDiscardedMsgCount() {
    return noSubscriptionMatchRxDiscardedMsgCount;
  }

  public void setNoSubscriptionMatchRxDiscardedMsgCount(Long noSubscriptionMatchRxDiscardedMsgCount) {
    this.noSubscriptionMatchRxDiscardedMsgCount = noSubscriptionMatchRxDiscardedMsgCount;
  }

  public MsgVpnClient originalClientUsername(String originalClientUsername) {
    this.originalClientUsername = originalClientUsername;
    return this;
  }

   /**
   * The original value of the client username used for Client authentication.
   * @return originalClientUsername
  **/
  @ApiModelProperty(value = "The original value of the client username used for Client authentication.")
  public String getOriginalClientUsername() {
    return originalClientUsername;
  }

  public void setOriginalClientUsername(String originalClientUsername) {
    this.originalClientUsername = originalClientUsername;
  }

  public MsgVpnClient otherBindFailureCount(Long otherBindFailureCount) {
    this.otherBindFailureCount = otherBindFailureCount;
    return this;
  }

   /**
   * The number of Client bind failures due to other reasons.
   * @return otherBindFailureCount
  **/
  @ApiModelProperty(value = "The number of Client bind failures due to other reasons.")
  public Long getOtherBindFailureCount() {
    return otherBindFailureCount;
  }

  public void setOtherBindFailureCount(Long otherBindFailureCount) {
    this.otherBindFailureCount = otherBindFailureCount;
  }

  public MsgVpnClient platform(String platform) {
    this.platform = platform;
    return this;
  }

   /**
   * The platform the Client application software was built for, which may include the OS and API type.
   * @return platform
  **/
  @ApiModelProperty(value = "The platform the Client application software was built for, which may include the OS and API type.")
  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public MsgVpnClient publishTopicAclRxDiscardedMsgCount(Long publishTopicAclRxDiscardedMsgCount) {
    this.publishTopicAclRxDiscardedMsgCount = publishTopicAclRxDiscardedMsgCount;
    return this;
  }

   /**
   * The number of messages from the Client discarded due to the publish topic being denied by the Access Control List (ACL) profile.
   * @return publishTopicAclRxDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of messages from the Client discarded due to the publish topic being denied by the Access Control List (ACL) profile.")
  public Long getPublishTopicAclRxDiscardedMsgCount() {
    return publishTopicAclRxDiscardedMsgCount;
  }

  public void setPublishTopicAclRxDiscardedMsgCount(Long publishTopicAclRxDiscardedMsgCount) {
    this.publishTopicAclRxDiscardedMsgCount = publishTopicAclRxDiscardedMsgCount;
  }

  public MsgVpnClient restHttpRequestRxByteCount(Long restHttpRequestRxByteCount) {
    this.restHttpRequestRxByteCount = restHttpRequestRxByteCount;
    return this;
  }

   /**
   * The amount of HTTP request messages received from the Client, in bytes (B).
   * @return restHttpRequestRxByteCount
  **/
  @ApiModelProperty(value = "The amount of HTTP request messages received from the Client, in bytes (B).")
  public Long getRestHttpRequestRxByteCount() {
    return restHttpRequestRxByteCount;
  }

  public void setRestHttpRequestRxByteCount(Long restHttpRequestRxByteCount) {
    this.restHttpRequestRxByteCount = restHttpRequestRxByteCount;
  }

  public MsgVpnClient restHttpRequestRxMsgCount(Long restHttpRequestRxMsgCount) {
    this.restHttpRequestRxMsgCount = restHttpRequestRxMsgCount;
    return this;
  }

   /**
   * The number of HTTP request messages received from the Client.
   * @return restHttpRequestRxMsgCount
  **/
  @ApiModelProperty(value = "The number of HTTP request messages received from the Client.")
  public Long getRestHttpRequestRxMsgCount() {
    return restHttpRequestRxMsgCount;
  }

  public void setRestHttpRequestRxMsgCount(Long restHttpRequestRxMsgCount) {
    this.restHttpRequestRxMsgCount = restHttpRequestRxMsgCount;
  }

  public MsgVpnClient restHttpRequestTxByteCount(Long restHttpRequestTxByteCount) {
    this.restHttpRequestTxByteCount = restHttpRequestTxByteCount;
    return this;
  }

   /**
   * The amount of HTTP request messages transmitted to the Client, in bytes (B).
   * @return restHttpRequestTxByteCount
  **/
  @ApiModelProperty(value = "The amount of HTTP request messages transmitted to the Client, in bytes (B).")
  public Long getRestHttpRequestTxByteCount() {
    return restHttpRequestTxByteCount;
  }

  public void setRestHttpRequestTxByteCount(Long restHttpRequestTxByteCount) {
    this.restHttpRequestTxByteCount = restHttpRequestTxByteCount;
  }

  public MsgVpnClient restHttpRequestTxMsgCount(Long restHttpRequestTxMsgCount) {
    this.restHttpRequestTxMsgCount = restHttpRequestTxMsgCount;
    return this;
  }

   /**
   * The number of HTTP request messages transmitted to the Client.
   * @return restHttpRequestTxMsgCount
  **/
  @ApiModelProperty(value = "The number of HTTP request messages transmitted to the Client.")
  public Long getRestHttpRequestTxMsgCount() {
    return restHttpRequestTxMsgCount;
  }

  public void setRestHttpRequestTxMsgCount(Long restHttpRequestTxMsgCount) {
    this.restHttpRequestTxMsgCount = restHttpRequestTxMsgCount;
  }

  public MsgVpnClient restHttpResponseErrorRxMsgCount(Long restHttpResponseErrorRxMsgCount) {
    this.restHttpResponseErrorRxMsgCount = restHttpResponseErrorRxMsgCount;
    return this;
  }

   /**
   * The number of HTTP client/server error response messages received from the Client.
   * @return restHttpResponseErrorRxMsgCount
  **/
  @ApiModelProperty(value = "The number of HTTP client/server error response messages received from the Client.")
  public Long getRestHttpResponseErrorRxMsgCount() {
    return restHttpResponseErrorRxMsgCount;
  }

  public void setRestHttpResponseErrorRxMsgCount(Long restHttpResponseErrorRxMsgCount) {
    this.restHttpResponseErrorRxMsgCount = restHttpResponseErrorRxMsgCount;
  }

  public MsgVpnClient restHttpResponseErrorTxMsgCount(Long restHttpResponseErrorTxMsgCount) {
    this.restHttpResponseErrorTxMsgCount = restHttpResponseErrorTxMsgCount;
    return this;
  }

   /**
   * The number of HTTP client/server error response messages transmitted to the Client.
   * @return restHttpResponseErrorTxMsgCount
  **/
  @ApiModelProperty(value = "The number of HTTP client/server error response messages transmitted to the Client.")
  public Long getRestHttpResponseErrorTxMsgCount() {
    return restHttpResponseErrorTxMsgCount;
  }

  public void setRestHttpResponseErrorTxMsgCount(Long restHttpResponseErrorTxMsgCount) {
    this.restHttpResponseErrorTxMsgCount = restHttpResponseErrorTxMsgCount;
  }

  public MsgVpnClient restHttpResponseRxByteCount(Long restHttpResponseRxByteCount) {
    this.restHttpResponseRxByteCount = restHttpResponseRxByteCount;
    return this;
  }

   /**
   * The amount of HTTP response messages received from the Client, in bytes (B).
   * @return restHttpResponseRxByteCount
  **/
  @ApiModelProperty(value = "The amount of HTTP response messages received from the Client, in bytes (B).")
  public Long getRestHttpResponseRxByteCount() {
    return restHttpResponseRxByteCount;
  }

  public void setRestHttpResponseRxByteCount(Long restHttpResponseRxByteCount) {
    this.restHttpResponseRxByteCount = restHttpResponseRxByteCount;
  }

  public MsgVpnClient restHttpResponseRxMsgCount(Long restHttpResponseRxMsgCount) {
    this.restHttpResponseRxMsgCount = restHttpResponseRxMsgCount;
    return this;
  }

   /**
   * The number of HTTP response messages received from the Client.
   * @return restHttpResponseRxMsgCount
  **/
  @ApiModelProperty(value = "The number of HTTP response messages received from the Client.")
  public Long getRestHttpResponseRxMsgCount() {
    return restHttpResponseRxMsgCount;
  }

  public void setRestHttpResponseRxMsgCount(Long restHttpResponseRxMsgCount) {
    this.restHttpResponseRxMsgCount = restHttpResponseRxMsgCount;
  }

  public MsgVpnClient restHttpResponseSuccessRxMsgCount(Long restHttpResponseSuccessRxMsgCount) {
    this.restHttpResponseSuccessRxMsgCount = restHttpResponseSuccessRxMsgCount;
    return this;
  }

   /**
   * The number of HTTP successful response messages received from the Client.
   * @return restHttpResponseSuccessRxMsgCount
  **/
  @ApiModelProperty(value = "The number of HTTP successful response messages received from the Client.")
  public Long getRestHttpResponseSuccessRxMsgCount() {
    return restHttpResponseSuccessRxMsgCount;
  }

  public void setRestHttpResponseSuccessRxMsgCount(Long restHttpResponseSuccessRxMsgCount) {
    this.restHttpResponseSuccessRxMsgCount = restHttpResponseSuccessRxMsgCount;
  }

  public MsgVpnClient restHttpResponseSuccessTxMsgCount(Long restHttpResponseSuccessTxMsgCount) {
    this.restHttpResponseSuccessTxMsgCount = restHttpResponseSuccessTxMsgCount;
    return this;
  }

   /**
   * The number of HTTP successful response messages transmitted to the Client.
   * @return restHttpResponseSuccessTxMsgCount
  **/
  @ApiModelProperty(value = "The number of HTTP successful response messages transmitted to the Client.")
  public Long getRestHttpResponseSuccessTxMsgCount() {
    return restHttpResponseSuccessTxMsgCount;
  }

  public void setRestHttpResponseSuccessTxMsgCount(Long restHttpResponseSuccessTxMsgCount) {
    this.restHttpResponseSuccessTxMsgCount = restHttpResponseSuccessTxMsgCount;
  }

  public MsgVpnClient restHttpResponseTimeoutRxMsgCount(Long restHttpResponseTimeoutRxMsgCount) {
    this.restHttpResponseTimeoutRxMsgCount = restHttpResponseTimeoutRxMsgCount;
    return this;
  }

   /**
   * The number of HTTP wait for reply timeout response messages received from the Client.
   * @return restHttpResponseTimeoutRxMsgCount
  **/
  @ApiModelProperty(value = "The number of HTTP wait for reply timeout response messages received from the Client.")
  public Long getRestHttpResponseTimeoutRxMsgCount() {
    return restHttpResponseTimeoutRxMsgCount;
  }

  public void setRestHttpResponseTimeoutRxMsgCount(Long restHttpResponseTimeoutRxMsgCount) {
    this.restHttpResponseTimeoutRxMsgCount = restHttpResponseTimeoutRxMsgCount;
  }

  public MsgVpnClient restHttpResponseTimeoutTxMsgCount(Long restHttpResponseTimeoutTxMsgCount) {
    this.restHttpResponseTimeoutTxMsgCount = restHttpResponseTimeoutTxMsgCount;
    return this;
  }

   /**
   * The number of HTTP wait for reply timeout response messages transmitted to the Client.
   * @return restHttpResponseTimeoutTxMsgCount
  **/
  @ApiModelProperty(value = "The number of HTTP wait for reply timeout response messages transmitted to the Client.")
  public Long getRestHttpResponseTimeoutTxMsgCount() {
    return restHttpResponseTimeoutTxMsgCount;
  }

  public void setRestHttpResponseTimeoutTxMsgCount(Long restHttpResponseTimeoutTxMsgCount) {
    this.restHttpResponseTimeoutTxMsgCount = restHttpResponseTimeoutTxMsgCount;
  }

  public MsgVpnClient restHttpResponseTxByteCount(Long restHttpResponseTxByteCount) {
    this.restHttpResponseTxByteCount = restHttpResponseTxByteCount;
    return this;
  }

   /**
   * The amount of HTTP response messages transmitted to the Client, in bytes (B).
   * @return restHttpResponseTxByteCount
  **/
  @ApiModelProperty(value = "The amount of HTTP response messages transmitted to the Client, in bytes (B).")
  public Long getRestHttpResponseTxByteCount() {
    return restHttpResponseTxByteCount;
  }

  public void setRestHttpResponseTxByteCount(Long restHttpResponseTxByteCount) {
    this.restHttpResponseTxByteCount = restHttpResponseTxByteCount;
  }

  public MsgVpnClient restHttpResponseTxMsgCount(Long restHttpResponseTxMsgCount) {
    this.restHttpResponseTxMsgCount = restHttpResponseTxMsgCount;
    return this;
  }

   /**
   * The number of HTTP response messages transmitted to the Client.
   * @return restHttpResponseTxMsgCount
  **/
  @ApiModelProperty(value = "The number of HTTP response messages transmitted to the Client.")
  public Long getRestHttpResponseTxMsgCount() {
    return restHttpResponseTxMsgCount;
  }

  public void setRestHttpResponseTxMsgCount(Long restHttpResponseTxMsgCount) {
    this.restHttpResponseTxMsgCount = restHttpResponseTxMsgCount;
  }

  public MsgVpnClient rxByteCount(Long rxByteCount) {
    this.rxByteCount = rxByteCount;
    return this;
  }

   /**
   * The amount of messages received from the Client, in bytes (B).
   * @return rxByteCount
  **/
  @ApiModelProperty(value = "The amount of messages received from the Client, in bytes (B).")
  public Long getRxByteCount() {
    return rxByteCount;
  }

  public void setRxByteCount(Long rxByteCount) {
    this.rxByteCount = rxByteCount;
  }

  public MsgVpnClient rxByteRate(Long rxByteRate) {
    this.rxByteRate = rxByteRate;
    return this;
  }

   /**
   * The current message rate received from the Client, in bytes per second (B/sec).
   * @return rxByteRate
  **/
  @ApiModelProperty(value = "The current message rate received from the Client, in bytes per second (B/sec).")
  public Long getRxByteRate() {
    return rxByteRate;
  }

  public void setRxByteRate(Long rxByteRate) {
    this.rxByteRate = rxByteRate;
  }

  public MsgVpnClient rxDiscardedMsgCount(Long rxDiscardedMsgCount) {
    this.rxDiscardedMsgCount = rxDiscardedMsgCount;
    return this;
  }

   /**
   * The number of messages discarded during reception from the Client.
   * @return rxDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of messages discarded during reception from the Client.")
  public Long getRxDiscardedMsgCount() {
    return rxDiscardedMsgCount;
  }

  public void setRxDiscardedMsgCount(Long rxDiscardedMsgCount) {
    this.rxDiscardedMsgCount = rxDiscardedMsgCount;
  }

  public MsgVpnClient rxMsgCount(Long rxMsgCount) {
    this.rxMsgCount = rxMsgCount;
    return this;
  }

   /**
   * The number of messages received from the Client.
   * @return rxMsgCount
  **/
  @ApiModelProperty(value = "The number of messages received from the Client.")
  public Long getRxMsgCount() {
    return rxMsgCount;
  }

  public void setRxMsgCount(Long rxMsgCount) {
    this.rxMsgCount = rxMsgCount;
  }

  public MsgVpnClient rxMsgRate(Long rxMsgRate) {
    this.rxMsgRate = rxMsgRate;
    return this;
  }

   /**
   * The current message rate received from the Client, in messages per second (msg/sec).
   * @return rxMsgRate
  **/
  @ApiModelProperty(value = "The current message rate received from the Client, in messages per second (msg/sec).")
  public Long getRxMsgRate() {
    return rxMsgRate;
  }

  public void setRxMsgRate(Long rxMsgRate) {
    this.rxMsgRate = rxMsgRate;
  }

  public MsgVpnClient scheduledDisconnectTime(Integer scheduledDisconnectTime) {
    this.scheduledDisconnectTime = scheduledDisconnectTime;
    return this;
  }

   /**
   * The timestamp of when the Client will be disconnected by the broker. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time). Available since 2.13.
   * @return scheduledDisconnectTime
  **/
  @ApiModelProperty(value = "The timestamp of when the Client will be disconnected by the broker. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time). Available since 2.13.")
  public Integer getScheduledDisconnectTime() {
    return scheduledDisconnectTime;
  }

  public void setScheduledDisconnectTime(Integer scheduledDisconnectTime) {
    this.scheduledDisconnectTime = scheduledDisconnectTime;
  }

  public MsgVpnClient slowSubscriber(Boolean slowSubscriber) {
    this.slowSubscriber = slowSubscriber;
    return this;
  }

   /**
   * Indicates whether the Client is a slow subscriber and blocks for a few seconds when receiving messages.
   * @return slowSubscriber
  **/
  @ApiModelProperty(value = "Indicates whether the Client is a slow subscriber and blocks for a few seconds when receiving messages.")
  public Boolean isSlowSubscriber() {
    return slowSubscriber;
  }

  public void setSlowSubscriber(Boolean slowSubscriber) {
    this.slowSubscriber = slowSubscriber;
  }

  public MsgVpnClient softwareDate(String softwareDate) {
    this.softwareDate = softwareDate;
    return this;
  }

   /**
   * The date the Client application software was built.
   * @return softwareDate
  **/
  @ApiModelProperty(value = "The date the Client application software was built.")
  public String getSoftwareDate() {
    return softwareDate;
  }

  public void setSoftwareDate(String softwareDate) {
    this.softwareDate = softwareDate;
  }

  public MsgVpnClient softwareVersion(String softwareVersion) {
    this.softwareVersion = softwareVersion;
    return this;
  }

   /**
   * The version of the Client application software.
   * @return softwareVersion
  **/
  @ApiModelProperty(value = "The version of the Client application software.")
  public String getSoftwareVersion() {
    return softwareVersion;
  }

  public void setSoftwareVersion(String softwareVersion) {
    this.softwareVersion = softwareVersion;
  }

  public MsgVpnClient tlsCipherDescription(String tlsCipherDescription) {
    this.tlsCipherDescription = tlsCipherDescription;
    return this;
  }

   /**
   * The description of the TLS cipher used by the Client, which may include cipher name, key exchange and encryption algorithms.
   * @return tlsCipherDescription
  **/
  @ApiModelProperty(value = "The description of the TLS cipher used by the Client, which may include cipher name, key exchange and encryption algorithms.")
  public String getTlsCipherDescription() {
    return tlsCipherDescription;
  }

  public void setTlsCipherDescription(String tlsCipherDescription) {
    this.tlsCipherDescription = tlsCipherDescription;
  }

  public MsgVpnClient tlsDowngradedToPlainText(Boolean tlsDowngradedToPlainText) {
    this.tlsDowngradedToPlainText = tlsDowngradedToPlainText;
    return this;
  }

   /**
   * Indicates whether the Client TLS connection was downgraded to plain-text to increase performance.
   * @return tlsDowngradedToPlainText
  **/
  @ApiModelProperty(value = "Indicates whether the Client TLS connection was downgraded to plain-text to increase performance.")
  public Boolean isTlsDowngradedToPlainText() {
    return tlsDowngradedToPlainText;
  }

  public void setTlsDowngradedToPlainText(Boolean tlsDowngradedToPlainText) {
    this.tlsDowngradedToPlainText = tlsDowngradedToPlainText;
  }

  public MsgVpnClient tlsVersion(String tlsVersion) {
    this.tlsVersion = tlsVersion;
    return this;
  }

   /**
   * The version of TLS used by the Client.
   * @return tlsVersion
  **/
  @ApiModelProperty(value = "The version of TLS used by the Client.")
  public String getTlsVersion() {
    return tlsVersion;
  }

  public void setTlsVersion(String tlsVersion) {
    this.tlsVersion = tlsVersion;
  }

  public MsgVpnClient topicParseErrorRxDiscardedMsgCount(Long topicParseErrorRxDiscardedMsgCount) {
    this.topicParseErrorRxDiscardedMsgCount = topicParseErrorRxDiscardedMsgCount;
    return this;
  }

   /**
   * The number of messages from the Client discarded due to an error while parsing the publish topic.
   * @return topicParseErrorRxDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of messages from the Client discarded due to an error while parsing the publish topic.")
  public Long getTopicParseErrorRxDiscardedMsgCount() {
    return topicParseErrorRxDiscardedMsgCount;
  }

  public void setTopicParseErrorRxDiscardedMsgCount(Long topicParseErrorRxDiscardedMsgCount) {
    this.topicParseErrorRxDiscardedMsgCount = topicParseErrorRxDiscardedMsgCount;
  }

  public MsgVpnClient txByteCount(Long txByteCount) {
    this.txByteCount = txByteCount;
    return this;
  }

   /**
   * The amount of messages transmitted to the Client, in bytes (B).
   * @return txByteCount
  **/
  @ApiModelProperty(value = "The amount of messages transmitted to the Client, in bytes (B).")
  public Long getTxByteCount() {
    return txByteCount;
  }

  public void setTxByteCount(Long txByteCount) {
    this.txByteCount = txByteCount;
  }

  public MsgVpnClient txByteRate(Long txByteRate) {
    this.txByteRate = txByteRate;
    return this;
  }

   /**
   * The current message rate transmitted to the Client, in bytes per second (B/sec).
   * @return txByteRate
  **/
  @ApiModelProperty(value = "The current message rate transmitted to the Client, in bytes per second (B/sec).")
  public Long getTxByteRate() {
    return txByteRate;
  }

  public void setTxByteRate(Long txByteRate) {
    this.txByteRate = txByteRate;
  }

  public MsgVpnClient txDiscardedMsgCount(Long txDiscardedMsgCount) {
    this.txDiscardedMsgCount = txDiscardedMsgCount;
    return this;
  }

   /**
   * The number of messages discarded during transmission to the Client.
   * @return txDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of messages discarded during transmission to the Client.")
  public Long getTxDiscardedMsgCount() {
    return txDiscardedMsgCount;
  }

  public void setTxDiscardedMsgCount(Long txDiscardedMsgCount) {
    this.txDiscardedMsgCount = txDiscardedMsgCount;
  }

  public MsgVpnClient txMsgCount(Long txMsgCount) {
    this.txMsgCount = txMsgCount;
    return this;
  }

   /**
   * The number of messages transmitted to the Client.
   * @return txMsgCount
  **/
  @ApiModelProperty(value = "The number of messages transmitted to the Client.")
  public Long getTxMsgCount() {
    return txMsgCount;
  }

  public void setTxMsgCount(Long txMsgCount) {
    this.txMsgCount = txMsgCount;
  }

  public MsgVpnClient txMsgRate(Long txMsgRate) {
    this.txMsgRate = txMsgRate;
    return this;
  }

   /**
   * The current message rate transmitted to the Client, in messages per second (msg/sec).
   * @return txMsgRate
  **/
  @ApiModelProperty(value = "The current message rate transmitted to the Client, in messages per second (msg/sec).")
  public Long getTxMsgRate() {
    return txMsgRate;
  }

  public void setTxMsgRate(Long txMsgRate) {
    this.txMsgRate = txMsgRate;
  }

  public MsgVpnClient uptime(Integer uptime) {
    this.uptime = uptime;
    return this;
  }

   /**
   * The amount of time in seconds since the Client connected.
   * @return uptime
  **/
  @ApiModelProperty(value = "The amount of time in seconds since the Client connected.")
  public Integer getUptime() {
    return uptime;
  }

  public void setUptime(Integer uptime) {
    this.uptime = uptime;
  }

  public MsgVpnClient user(String user) {
    this.user = user;
    return this;
  }

   /**
   * The user description for the Client, which may include computer name and process ID.
   * @return user
  **/
  @ApiModelProperty(value = "The user description for the Client, which may include computer name and process ID.")
  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public MsgVpnClient virtualRouter(String virtualRouter) {
    this.virtualRouter = virtualRouter;
    return this;
  }

   /**
   * The virtual router used by the Client. The allowed values and their meaning are:  &lt;pre&gt; \&quot;primary\&quot; - The Client is using the primary virtual router. \&quot;backup\&quot; - The Client is using the backup virtual router. \&quot;internal\&quot; - The Client is using the internal virtual router. \&quot;unknown\&quot; - The Client virtual router is unknown. &lt;/pre&gt; 
   * @return virtualRouter
  **/
  @ApiModelProperty(value = "The virtual router used by the Client. The allowed values and their meaning are:  <pre> \"primary\" - The Client is using the primary virtual router. \"backup\" - The Client is using the backup virtual router. \"internal\" - The Client is using the internal virtual router. \"unknown\" - The Client virtual router is unknown. </pre> ")
  public String getVirtualRouter() {
    return virtualRouter;
  }

  public void setVirtualRouter(String virtualRouter) {
    this.virtualRouter = virtualRouter;
  }

  public MsgVpnClient webInactiveTimeout(Integer webInactiveTimeout) {
    this.webInactiveTimeout = webInactiveTimeout;
    return this;
  }

   /**
   * The maximum web transport timeout for the Client being inactive, in seconds.
   * @return webInactiveTimeout
  **/
  @ApiModelProperty(value = "The maximum web transport timeout for the Client being inactive, in seconds.")
  public Integer getWebInactiveTimeout() {
    return webInactiveTimeout;
  }

  public void setWebInactiveTimeout(Integer webInactiveTimeout) {
    this.webInactiveTimeout = webInactiveTimeout;
  }

  public MsgVpnClient webMaxPayload(Long webMaxPayload) {
    this.webMaxPayload = webMaxPayload;
    return this;
  }

   /**
   * The maximum web transport message payload size which excludes the size of the message header, in bytes.
   * @return webMaxPayload
  **/
  @ApiModelProperty(value = "The maximum web transport message payload size which excludes the size of the message header, in bytes.")
  public Long getWebMaxPayload() {
    return webMaxPayload;
  }

  public void setWebMaxPayload(Long webMaxPayload) {
    this.webMaxPayload = webMaxPayload;
  }

  public MsgVpnClient webParseErrorRxDiscardedMsgCount(Long webParseErrorRxDiscardedMsgCount) {
    this.webParseErrorRxDiscardedMsgCount = webParseErrorRxDiscardedMsgCount;
    return this;
  }

   /**
   * The number of messages from the Client discarded due to an error while parsing the web message.
   * @return webParseErrorRxDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of messages from the Client discarded due to an error while parsing the web message.")
  public Long getWebParseErrorRxDiscardedMsgCount() {
    return webParseErrorRxDiscardedMsgCount;
  }

  public void setWebParseErrorRxDiscardedMsgCount(Long webParseErrorRxDiscardedMsgCount) {
    this.webParseErrorRxDiscardedMsgCount = webParseErrorRxDiscardedMsgCount;
  }

  public MsgVpnClient webRemainingTimeout(Integer webRemainingTimeout) {
    this.webRemainingTimeout = webRemainingTimeout;
    return this;
  }

   /**
   * The remaining web transport timeout for the Client being inactive, in seconds.
   * @return webRemainingTimeout
  **/
  @ApiModelProperty(value = "The remaining web transport timeout for the Client being inactive, in seconds.")
  public Integer getWebRemainingTimeout() {
    return webRemainingTimeout;
  }

  public void setWebRemainingTimeout(Integer webRemainingTimeout) {
    this.webRemainingTimeout = webRemainingTimeout;
  }

  public MsgVpnClient webRxByteCount(Long webRxByteCount) {
    this.webRxByteCount = webRxByteCount;
    return this;
  }

   /**
   * The amount of web transport messages received from the Client, in bytes (B).
   * @return webRxByteCount
  **/
  @ApiModelProperty(value = "The amount of web transport messages received from the Client, in bytes (B).")
  public Long getWebRxByteCount() {
    return webRxByteCount;
  }

  public void setWebRxByteCount(Long webRxByteCount) {
    this.webRxByteCount = webRxByteCount;
  }

  public MsgVpnClient webRxEncoding(String webRxEncoding) {
    this.webRxEncoding = webRxEncoding;
    return this;
  }

   /**
   * The type of encoding used during reception from the Client. The allowed values and their meaning are:  &lt;pre&gt; \&quot;binary\&quot; - The Client is using binary encoding. \&quot;base64\&quot; - The Client is using base64 encoding. \&quot;illegal\&quot; - The Client is using an illegal encoding type. &lt;/pre&gt; 
   * @return webRxEncoding
  **/
  @ApiModelProperty(value = "The type of encoding used during reception from the Client. The allowed values and their meaning are:  <pre> \"binary\" - The Client is using binary encoding. \"base64\" - The Client is using base64 encoding. \"illegal\" - The Client is using an illegal encoding type. </pre> ")
  public String getWebRxEncoding() {
    return webRxEncoding;
  }

  public void setWebRxEncoding(String webRxEncoding) {
    this.webRxEncoding = webRxEncoding;
  }

  public MsgVpnClient webRxMsgCount(Long webRxMsgCount) {
    this.webRxMsgCount = webRxMsgCount;
    return this;
  }

   /**
   * The number of web transport messages received from the Client.
   * @return webRxMsgCount
  **/
  @ApiModelProperty(value = "The number of web transport messages received from the Client.")
  public Long getWebRxMsgCount() {
    return webRxMsgCount;
  }

  public void setWebRxMsgCount(Long webRxMsgCount) {
    this.webRxMsgCount = webRxMsgCount;
  }

  public MsgVpnClient webRxProtocol(String webRxProtocol) {
    this.webRxProtocol = webRxProtocol;
    return this;
  }

   /**
   * The type of web transport used during reception from the Client. The allowed values and their meaning are:  &lt;pre&gt; \&quot;ws-binary\&quot; - The Client is using WebSocket binary transport. \&quot;http-binary-streaming\&quot; - The Client is using HTTP binary streaming transport. \&quot;http-binary\&quot; - The Client is using HTTP binary transport. \&quot;http-base64\&quot; - The Client is using HTTP base64 transport. &lt;/pre&gt; 
   * @return webRxProtocol
  **/
  @ApiModelProperty(value = "The type of web transport used during reception from the Client. The allowed values and their meaning are:  <pre> \"ws-binary\" - The Client is using WebSocket binary transport. \"http-binary-streaming\" - The Client is using HTTP binary streaming transport. \"http-binary\" - The Client is using HTTP binary transport. \"http-base64\" - The Client is using HTTP base64 transport. </pre> ")
  public String getWebRxProtocol() {
    return webRxProtocol;
  }

  public void setWebRxProtocol(String webRxProtocol) {
    this.webRxProtocol = webRxProtocol;
  }

  public MsgVpnClient webRxRequestCount(Long webRxRequestCount) {
    this.webRxRequestCount = webRxRequestCount;
    return this;
  }

   /**
   * The number of web transport requests received from the Client (HTTP only). Not available for WebSockets.
   * @return webRxRequestCount
  **/
  @ApiModelProperty(value = "The number of web transport requests received from the Client (HTTP only). Not available for WebSockets.")
  public Long getWebRxRequestCount() {
    return webRxRequestCount;
  }

  public void setWebRxRequestCount(Long webRxRequestCount) {
    this.webRxRequestCount = webRxRequestCount;
  }

  public MsgVpnClient webRxResponseCount(Long webRxResponseCount) {
    this.webRxResponseCount = webRxResponseCount;
    return this;
  }

   /**
   * The number of web transport responses transmitted to the Client on the receive connection (HTTP only). Not available for WebSockets.
   * @return webRxResponseCount
  **/
  @ApiModelProperty(value = "The number of web transport responses transmitted to the Client on the receive connection (HTTP only). Not available for WebSockets.")
  public Long getWebRxResponseCount() {
    return webRxResponseCount;
  }

  public void setWebRxResponseCount(Long webRxResponseCount) {
    this.webRxResponseCount = webRxResponseCount;
  }

  public MsgVpnClient webRxTcpState(String webRxTcpState) {
    this.webRxTcpState = webRxTcpState;
    return this;
  }

   /**
   * The TCP state of the receive connection from the Client. When fully operational, should be: established. See RFC 793 for further details. The allowed values and their meaning are:  &lt;pre&gt; \&quot;closed\&quot; - No connection state at all. \&quot;listen\&quot; - Waiting for a connection request from any remote TCP and port. \&quot;syn-sent\&quot; - Waiting for a matching connection request after having sent a connection request. \&quot;syn-received\&quot; - Waiting for a confirming connection request acknowledgment after having both received and sent a connection request. \&quot;established\&quot; - An open connection, data received can be delivered to the user. \&quot;close-wait\&quot; - Waiting for a connection termination request from the local user. \&quot;fin-wait-1\&quot; - Waiting for a connection termination request from the remote TCP, or an acknowledgment of the connection termination request previously sent. \&quot;closing\&quot; - Waiting for a connection termination request acknowledgment from the remote TCP. \&quot;last-ack\&quot; - Waiting for an acknowledgment of the connection termination request previously sent to the remote TCP. \&quot;fin-wait-2\&quot; - Waiting for a connection termination request from the remote TCP. \&quot;time-wait\&quot; - Waiting for enough time to pass to be sure the remote TCP received the acknowledgment of its connection termination request. &lt;/pre&gt; 
   * @return webRxTcpState
  **/
  @ApiModelProperty(value = "The TCP state of the receive connection from the Client. When fully operational, should be: established. See RFC 793 for further details. The allowed values and their meaning are:  <pre> \"closed\" - No connection state at all. \"listen\" - Waiting for a connection request from any remote TCP and port. \"syn-sent\" - Waiting for a matching connection request after having sent a connection request. \"syn-received\" - Waiting for a confirming connection request acknowledgment after having both received and sent a connection request. \"established\" - An open connection, data received can be delivered to the user. \"close-wait\" - Waiting for a connection termination request from the local user. \"fin-wait-1\" - Waiting for a connection termination request from the remote TCP, or an acknowledgment of the connection termination request previously sent. \"closing\" - Waiting for a connection termination request acknowledgment from the remote TCP. \"last-ack\" - Waiting for an acknowledgment of the connection termination request previously sent to the remote TCP. \"fin-wait-2\" - Waiting for a connection termination request from the remote TCP. \"time-wait\" - Waiting for enough time to pass to be sure the remote TCP received the acknowledgment of its connection termination request. </pre> ")
  public String getWebRxTcpState() {
    return webRxTcpState;
  }

  public void setWebRxTcpState(String webRxTcpState) {
    this.webRxTcpState = webRxTcpState;
  }

  public MsgVpnClient webRxTlsCipherDescription(String webRxTlsCipherDescription) {
    this.webRxTlsCipherDescription = webRxTlsCipherDescription;
    return this;
  }

   /**
   * The description of the TLS cipher received from the Client, which may include cipher name, key exchange and encryption algorithms.
   * @return webRxTlsCipherDescription
  **/
  @ApiModelProperty(value = "The description of the TLS cipher received from the Client, which may include cipher name, key exchange and encryption algorithms.")
  public String getWebRxTlsCipherDescription() {
    return webRxTlsCipherDescription;
  }

  public void setWebRxTlsCipherDescription(String webRxTlsCipherDescription) {
    this.webRxTlsCipherDescription = webRxTlsCipherDescription;
  }

  public MsgVpnClient webRxTlsVersion(String webRxTlsVersion) {
    this.webRxTlsVersion = webRxTlsVersion;
    return this;
  }

   /**
   * The version of TLS used during reception from the Client.
   * @return webRxTlsVersion
  **/
  @ApiModelProperty(value = "The version of TLS used during reception from the Client.")
  public String getWebRxTlsVersion() {
    return webRxTlsVersion;
  }

  public void setWebRxTlsVersion(String webRxTlsVersion) {
    this.webRxTlsVersion = webRxTlsVersion;
  }

  public MsgVpnClient webSessionId(String webSessionId) {
    this.webSessionId = webSessionId;
    return this;
  }

   /**
   * The identifier (ID) of the web transport session for the Client.
   * @return webSessionId
  **/
  @ApiModelProperty(value = "The identifier (ID) of the web transport session for the Client.")
  public String getWebSessionId() {
    return webSessionId;
  }

  public void setWebSessionId(String webSessionId) {
    this.webSessionId = webSessionId;
  }

  public MsgVpnClient webTxByteCount(Long webTxByteCount) {
    this.webTxByteCount = webTxByteCount;
    return this;
  }

   /**
   * The amount of web transport messages transmitted to the Client, in bytes (B).
   * @return webTxByteCount
  **/
  @ApiModelProperty(value = "The amount of web transport messages transmitted to the Client, in bytes (B).")
  public Long getWebTxByteCount() {
    return webTxByteCount;
  }

  public void setWebTxByteCount(Long webTxByteCount) {
    this.webTxByteCount = webTxByteCount;
  }

  public MsgVpnClient webTxEncoding(String webTxEncoding) {
    this.webTxEncoding = webTxEncoding;
    return this;
  }

   /**
   * The type of encoding used during transmission to the Client. The allowed values and their meaning are:  &lt;pre&gt; \&quot;binary\&quot; - The Client is using binary encoding. \&quot;base64\&quot; - The Client is using base64 encoding. \&quot;illegal\&quot; - The Client is using an illegal encoding type. &lt;/pre&gt; 
   * @return webTxEncoding
  **/
  @ApiModelProperty(value = "The type of encoding used during transmission to the Client. The allowed values and their meaning are:  <pre> \"binary\" - The Client is using binary encoding. \"base64\" - The Client is using base64 encoding. \"illegal\" - The Client is using an illegal encoding type. </pre> ")
  public String getWebTxEncoding() {
    return webTxEncoding;
  }

  public void setWebTxEncoding(String webTxEncoding) {
    this.webTxEncoding = webTxEncoding;
  }

  public MsgVpnClient webTxMsgCount(Long webTxMsgCount) {
    this.webTxMsgCount = webTxMsgCount;
    return this;
  }

   /**
   * The number of web transport messages transmitted to the Client.
   * @return webTxMsgCount
  **/
  @ApiModelProperty(value = "The number of web transport messages transmitted to the Client.")
  public Long getWebTxMsgCount() {
    return webTxMsgCount;
  }

  public void setWebTxMsgCount(Long webTxMsgCount) {
    this.webTxMsgCount = webTxMsgCount;
  }

  public MsgVpnClient webTxProtocol(String webTxProtocol) {
    this.webTxProtocol = webTxProtocol;
    return this;
  }

   /**
   * The type of web transport used during transmission to the Client. The allowed values and their meaning are:  &lt;pre&gt; \&quot;ws-binary\&quot; - The Client is using WebSocket binary transport. \&quot;http-binary-streaming\&quot; - The Client is using HTTP binary streaming transport. \&quot;http-binary\&quot; - The Client is using HTTP binary transport. \&quot;http-base64\&quot; - The Client is using HTTP base64 transport. &lt;/pre&gt; 
   * @return webTxProtocol
  **/
  @ApiModelProperty(value = "The type of web transport used during transmission to the Client. The allowed values and their meaning are:  <pre> \"ws-binary\" - The Client is using WebSocket binary transport. \"http-binary-streaming\" - The Client is using HTTP binary streaming transport. \"http-binary\" - The Client is using HTTP binary transport. \"http-base64\" - The Client is using HTTP base64 transport. </pre> ")
  public String getWebTxProtocol() {
    return webTxProtocol;
  }

  public void setWebTxProtocol(String webTxProtocol) {
    this.webTxProtocol = webTxProtocol;
  }

  public MsgVpnClient webTxRequestCount(Long webTxRequestCount) {
    this.webTxRequestCount = webTxRequestCount;
    return this;
  }

   /**
   * The number of web transport requests transmitted to the Client (HTTP only). Not available for WebSockets.
   * @return webTxRequestCount
  **/
  @ApiModelProperty(value = "The number of web transport requests transmitted to the Client (HTTP only). Not available for WebSockets.")
  public Long getWebTxRequestCount() {
    return webTxRequestCount;
  }

  public void setWebTxRequestCount(Long webTxRequestCount) {
    this.webTxRequestCount = webTxRequestCount;
  }

  public MsgVpnClient webTxResponseCount(Long webTxResponseCount) {
    this.webTxResponseCount = webTxResponseCount;
    return this;
  }

   /**
   * The number of web transport responses received from the Client on the transmit connection (HTTP only). Not available for WebSockets.
   * @return webTxResponseCount
  **/
  @ApiModelProperty(value = "The number of web transport responses received from the Client on the transmit connection (HTTP only). Not available for WebSockets.")
  public Long getWebTxResponseCount() {
    return webTxResponseCount;
  }

  public void setWebTxResponseCount(Long webTxResponseCount) {
    this.webTxResponseCount = webTxResponseCount;
  }

  public MsgVpnClient webTxTcpState(String webTxTcpState) {
    this.webTxTcpState = webTxTcpState;
    return this;
  }

   /**
   * The TCP state of the transmit connection to the Client. When fully operational, should be: established. See RFC 793 for further details. The allowed values and their meaning are:  &lt;pre&gt; \&quot;closed\&quot; - No connection state at all. \&quot;listen\&quot; - Waiting for a connection request from any remote TCP and port. \&quot;syn-sent\&quot; - Waiting for a matching connection request after having sent a connection request. \&quot;syn-received\&quot; - Waiting for a confirming connection request acknowledgment after having both received and sent a connection request. \&quot;established\&quot; - An open connection, data received can be delivered to the user. \&quot;close-wait\&quot; - Waiting for a connection termination request from the local user. \&quot;fin-wait-1\&quot; - Waiting for a connection termination request from the remote TCP, or an acknowledgment of the connection termination request previously sent. \&quot;closing\&quot; - Waiting for a connection termination request acknowledgment from the remote TCP. \&quot;last-ack\&quot; - Waiting for an acknowledgment of the connection termination request previously sent to the remote TCP. \&quot;fin-wait-2\&quot; - Waiting for a connection termination request from the remote TCP. \&quot;time-wait\&quot; - Waiting for enough time to pass to be sure the remote TCP received the acknowledgment of its connection termination request. &lt;/pre&gt; 
   * @return webTxTcpState
  **/
  @ApiModelProperty(value = "The TCP state of the transmit connection to the Client. When fully operational, should be: established. See RFC 793 for further details. The allowed values and their meaning are:  <pre> \"closed\" - No connection state at all. \"listen\" - Waiting for a connection request from any remote TCP and port. \"syn-sent\" - Waiting for a matching connection request after having sent a connection request. \"syn-received\" - Waiting for a confirming connection request acknowledgment after having both received and sent a connection request. \"established\" - An open connection, data received can be delivered to the user. \"close-wait\" - Waiting for a connection termination request from the local user. \"fin-wait-1\" - Waiting for a connection termination request from the remote TCP, or an acknowledgment of the connection termination request previously sent. \"closing\" - Waiting for a connection termination request acknowledgment from the remote TCP. \"last-ack\" - Waiting for an acknowledgment of the connection termination request previously sent to the remote TCP. \"fin-wait-2\" - Waiting for a connection termination request from the remote TCP. \"time-wait\" - Waiting for enough time to pass to be sure the remote TCP received the acknowledgment of its connection termination request. </pre> ")
  public String getWebTxTcpState() {
    return webTxTcpState;
  }

  public void setWebTxTcpState(String webTxTcpState) {
    this.webTxTcpState = webTxTcpState;
  }

  public MsgVpnClient webTxTlsCipherDescription(String webTxTlsCipherDescription) {
    this.webTxTlsCipherDescription = webTxTlsCipherDescription;
    return this;
  }

   /**
   * The description of the TLS cipher transmitted to the Client, which may include cipher name, key exchange and encryption algorithms.
   * @return webTxTlsCipherDescription
  **/
  @ApiModelProperty(value = "The description of the TLS cipher transmitted to the Client, which may include cipher name, key exchange and encryption algorithms.")
  public String getWebTxTlsCipherDescription() {
    return webTxTlsCipherDescription;
  }

  public void setWebTxTlsCipherDescription(String webTxTlsCipherDescription) {
    this.webTxTlsCipherDescription = webTxTlsCipherDescription;
  }

  public MsgVpnClient webTxTlsVersion(String webTxTlsVersion) {
    this.webTxTlsVersion = webTxTlsVersion;
    return this;
  }

   /**
   * The version of TLS used during transmission to the Client.
   * @return webTxTlsVersion
  **/
  @ApiModelProperty(value = "The version of TLS used during transmission to the Client.")
  public String getWebTxTlsVersion() {
    return webTxTlsVersion;
  }

  public void setWebTxTlsVersion(String webTxTlsVersion) {
    this.webTxTlsVersion = webTxTlsVersion;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnClient msgVpnClient = (MsgVpnClient) o;
    return Objects.equals(this.aclProfileName, msgVpnClient.aclProfileName) &&
        Objects.equals(this.aliasedFromMsgVpnName, msgVpnClient.aliasedFromMsgVpnName) &&
        Objects.equals(this.alreadyBoundBindFailureCount, msgVpnClient.alreadyBoundBindFailureCount) &&
        Objects.equals(this.authorizationGroupName, msgVpnClient.authorizationGroupName) &&
        Objects.equals(this.averageRxByteRate, msgVpnClient.averageRxByteRate) &&
        Objects.equals(this.averageRxMsgRate, msgVpnClient.averageRxMsgRate) &&
        Objects.equals(this.averageTxByteRate, msgVpnClient.averageTxByteRate) &&
        Objects.equals(this.averageTxMsgRate, msgVpnClient.averageTxMsgRate) &&
        Objects.equals(this.bindRequestCount, msgVpnClient.bindRequestCount) &&
        Objects.equals(this.bindSuccessCount, msgVpnClient.bindSuccessCount) &&
        Objects.equals(this.clientAddress, msgVpnClient.clientAddress) &&
        Objects.equals(this.clientId, msgVpnClient.clientId) &&
        Objects.equals(this.clientName, msgVpnClient.clientName) &&
        Objects.equals(this.clientProfileName, msgVpnClient.clientProfileName) &&
        Objects.equals(this.clientUsername, msgVpnClient.clientUsername) &&
        Objects.equals(this.controlRxByteCount, msgVpnClient.controlRxByteCount) &&
        Objects.equals(this.controlRxMsgCount, msgVpnClient.controlRxMsgCount) &&
        Objects.equals(this.controlTxByteCount, msgVpnClient.controlTxByteCount) &&
        Objects.equals(this.controlTxMsgCount, msgVpnClient.controlTxMsgCount) &&
        Objects.equals(this.cutThroughDeniedBindFailureCount, msgVpnClient.cutThroughDeniedBindFailureCount) &&
        Objects.equals(this.dataRxByteCount, msgVpnClient.dataRxByteCount) &&
        Objects.equals(this.dataRxMsgCount, msgVpnClient.dataRxMsgCount) &&
        Objects.equals(this.dataTxByteCount, msgVpnClient.dataTxByteCount) &&
        Objects.equals(this.dataTxMsgCount, msgVpnClient.dataTxMsgCount) &&
        Objects.equals(this.description, msgVpnClient.description) &&
        Objects.equals(this.disabledBindFailureCount, msgVpnClient.disabledBindFailureCount) &&
        Objects.equals(this.dtoLocalPriority, msgVpnClient.dtoLocalPriority) &&
        Objects.equals(this.dtoNetworkPriority, msgVpnClient.dtoNetworkPriority) &&
        Objects.equals(this.eliding, msgVpnClient.eliding) &&
        Objects.equals(this.elidingTopicCount, msgVpnClient.elidingTopicCount) &&
        Objects.equals(this.elidingTopicPeakCount, msgVpnClient.elidingTopicPeakCount) &&
        Objects.equals(this.guaranteedDeniedBindFailureCount, msgVpnClient.guaranteedDeniedBindFailureCount) &&
        Objects.equals(this.invalidSelectorBindFailureCount, msgVpnClient.invalidSelectorBindFailureCount) &&
        Objects.equals(this.largeMsgEventRaised, msgVpnClient.largeMsgEventRaised) &&
        Objects.equals(this.loginRxMsgCount, msgVpnClient.loginRxMsgCount) &&
        Objects.equals(this.loginTxMsgCount, msgVpnClient.loginTxMsgCount) &&
        Objects.equals(this.maxBindCountExceededBindFailureCount, msgVpnClient.maxBindCountExceededBindFailureCount) &&
        Objects.equals(this.maxElidingTopicCountEventRaised, msgVpnClient.maxElidingTopicCountEventRaised) &&
        Objects.equals(this.mqttConnackErrorTxCount, msgVpnClient.mqttConnackErrorTxCount) &&
        Objects.equals(this.mqttConnackTxCount, msgVpnClient.mqttConnackTxCount) &&
        Objects.equals(this.mqttConnectRxCount, msgVpnClient.mqttConnectRxCount) &&
        Objects.equals(this.mqttDisconnectRxCount, msgVpnClient.mqttDisconnectRxCount) &&
        Objects.equals(this.mqttPingreqRxCount, msgVpnClient.mqttPingreqRxCount) &&
        Objects.equals(this.mqttPingrespTxCount, msgVpnClient.mqttPingrespTxCount) &&
        Objects.equals(this.mqttPubackRxCount, msgVpnClient.mqttPubackRxCount) &&
        Objects.equals(this.mqttPubackTxCount, msgVpnClient.mqttPubackTxCount) &&
        Objects.equals(this.mqttPubcompTxCount, msgVpnClient.mqttPubcompTxCount) &&
        Objects.equals(this.mqttPublishQos0RxCount, msgVpnClient.mqttPublishQos0RxCount) &&
        Objects.equals(this.mqttPublishQos0TxCount, msgVpnClient.mqttPublishQos0TxCount) &&
        Objects.equals(this.mqttPublishQos1RxCount, msgVpnClient.mqttPublishQos1RxCount) &&
        Objects.equals(this.mqttPublishQos1TxCount, msgVpnClient.mqttPublishQos1TxCount) &&
        Objects.equals(this.mqttPublishQos2RxCount, msgVpnClient.mqttPublishQos2RxCount) &&
        Objects.equals(this.mqttPubrecTxCount, msgVpnClient.mqttPubrecTxCount) &&
        Objects.equals(this.mqttPubrelRxCount, msgVpnClient.mqttPubrelRxCount) &&
        Objects.equals(this.mqttSubackErrorTxCount, msgVpnClient.mqttSubackErrorTxCount) &&
        Objects.equals(this.mqttSubackTxCount, msgVpnClient.mqttSubackTxCount) &&
        Objects.equals(this.mqttSubscribeRxCount, msgVpnClient.mqttSubscribeRxCount) &&
        Objects.equals(this.mqttUnsubackTxCount, msgVpnClient.mqttUnsubackTxCount) &&
        Objects.equals(this.mqttUnsubscribeRxCount, msgVpnClient.mqttUnsubscribeRxCount) &&
        Objects.equals(this.msgSpoolCongestionRxDiscardedMsgCount, msgVpnClient.msgSpoolCongestionRxDiscardedMsgCount) &&
        Objects.equals(this.msgSpoolRxDiscardedMsgCount, msgVpnClient.msgSpoolRxDiscardedMsgCount) &&
        Objects.equals(this.msgVpnName, msgVpnClient.msgVpnName) &&
        Objects.equals(this.noLocalDelivery, msgVpnClient.noLocalDelivery) &&
        Objects.equals(this.noSubscriptionMatchRxDiscardedMsgCount, msgVpnClient.noSubscriptionMatchRxDiscardedMsgCount) &&
        Objects.equals(this.originalClientUsername, msgVpnClient.originalClientUsername) &&
        Objects.equals(this.otherBindFailureCount, msgVpnClient.otherBindFailureCount) &&
        Objects.equals(this.platform, msgVpnClient.platform) &&
        Objects.equals(this.publishTopicAclRxDiscardedMsgCount, msgVpnClient.publishTopicAclRxDiscardedMsgCount) &&
        Objects.equals(this.restHttpRequestRxByteCount, msgVpnClient.restHttpRequestRxByteCount) &&
        Objects.equals(this.restHttpRequestRxMsgCount, msgVpnClient.restHttpRequestRxMsgCount) &&
        Objects.equals(this.restHttpRequestTxByteCount, msgVpnClient.restHttpRequestTxByteCount) &&
        Objects.equals(this.restHttpRequestTxMsgCount, msgVpnClient.restHttpRequestTxMsgCount) &&
        Objects.equals(this.restHttpResponseErrorRxMsgCount, msgVpnClient.restHttpResponseErrorRxMsgCount) &&
        Objects.equals(this.restHttpResponseErrorTxMsgCount, msgVpnClient.restHttpResponseErrorTxMsgCount) &&
        Objects.equals(this.restHttpResponseRxByteCount, msgVpnClient.restHttpResponseRxByteCount) &&
        Objects.equals(this.restHttpResponseRxMsgCount, msgVpnClient.restHttpResponseRxMsgCount) &&
        Objects.equals(this.restHttpResponseSuccessRxMsgCount, msgVpnClient.restHttpResponseSuccessRxMsgCount) &&
        Objects.equals(this.restHttpResponseSuccessTxMsgCount, msgVpnClient.restHttpResponseSuccessTxMsgCount) &&
        Objects.equals(this.restHttpResponseTimeoutRxMsgCount, msgVpnClient.restHttpResponseTimeoutRxMsgCount) &&
        Objects.equals(this.restHttpResponseTimeoutTxMsgCount, msgVpnClient.restHttpResponseTimeoutTxMsgCount) &&
        Objects.equals(this.restHttpResponseTxByteCount, msgVpnClient.restHttpResponseTxByteCount) &&
        Objects.equals(this.restHttpResponseTxMsgCount, msgVpnClient.restHttpResponseTxMsgCount) &&
        Objects.equals(this.rxByteCount, msgVpnClient.rxByteCount) &&
        Objects.equals(this.rxByteRate, msgVpnClient.rxByteRate) &&
        Objects.equals(this.rxDiscardedMsgCount, msgVpnClient.rxDiscardedMsgCount) &&
        Objects.equals(this.rxMsgCount, msgVpnClient.rxMsgCount) &&
        Objects.equals(this.rxMsgRate, msgVpnClient.rxMsgRate) &&
        Objects.equals(this.scheduledDisconnectTime, msgVpnClient.scheduledDisconnectTime) &&
        Objects.equals(this.slowSubscriber, msgVpnClient.slowSubscriber) &&
        Objects.equals(this.softwareDate, msgVpnClient.softwareDate) &&
        Objects.equals(this.softwareVersion, msgVpnClient.softwareVersion) &&
        Objects.equals(this.tlsCipherDescription, msgVpnClient.tlsCipherDescription) &&
        Objects.equals(this.tlsDowngradedToPlainText, msgVpnClient.tlsDowngradedToPlainText) &&
        Objects.equals(this.tlsVersion, msgVpnClient.tlsVersion) &&
        Objects.equals(this.topicParseErrorRxDiscardedMsgCount, msgVpnClient.topicParseErrorRxDiscardedMsgCount) &&
        Objects.equals(this.txByteCount, msgVpnClient.txByteCount) &&
        Objects.equals(this.txByteRate, msgVpnClient.txByteRate) &&
        Objects.equals(this.txDiscardedMsgCount, msgVpnClient.txDiscardedMsgCount) &&
        Objects.equals(this.txMsgCount, msgVpnClient.txMsgCount) &&
        Objects.equals(this.txMsgRate, msgVpnClient.txMsgRate) &&
        Objects.equals(this.uptime, msgVpnClient.uptime) &&
        Objects.equals(this.user, msgVpnClient.user) &&
        Objects.equals(this.virtualRouter, msgVpnClient.virtualRouter) &&
        Objects.equals(this.webInactiveTimeout, msgVpnClient.webInactiveTimeout) &&
        Objects.equals(this.webMaxPayload, msgVpnClient.webMaxPayload) &&
        Objects.equals(this.webParseErrorRxDiscardedMsgCount, msgVpnClient.webParseErrorRxDiscardedMsgCount) &&
        Objects.equals(this.webRemainingTimeout, msgVpnClient.webRemainingTimeout) &&
        Objects.equals(this.webRxByteCount, msgVpnClient.webRxByteCount) &&
        Objects.equals(this.webRxEncoding, msgVpnClient.webRxEncoding) &&
        Objects.equals(this.webRxMsgCount, msgVpnClient.webRxMsgCount) &&
        Objects.equals(this.webRxProtocol, msgVpnClient.webRxProtocol) &&
        Objects.equals(this.webRxRequestCount, msgVpnClient.webRxRequestCount) &&
        Objects.equals(this.webRxResponseCount, msgVpnClient.webRxResponseCount) &&
        Objects.equals(this.webRxTcpState, msgVpnClient.webRxTcpState) &&
        Objects.equals(this.webRxTlsCipherDescription, msgVpnClient.webRxTlsCipherDescription) &&
        Objects.equals(this.webRxTlsVersion, msgVpnClient.webRxTlsVersion) &&
        Objects.equals(this.webSessionId, msgVpnClient.webSessionId) &&
        Objects.equals(this.webTxByteCount, msgVpnClient.webTxByteCount) &&
        Objects.equals(this.webTxEncoding, msgVpnClient.webTxEncoding) &&
        Objects.equals(this.webTxMsgCount, msgVpnClient.webTxMsgCount) &&
        Objects.equals(this.webTxProtocol, msgVpnClient.webTxProtocol) &&
        Objects.equals(this.webTxRequestCount, msgVpnClient.webTxRequestCount) &&
        Objects.equals(this.webTxResponseCount, msgVpnClient.webTxResponseCount) &&
        Objects.equals(this.webTxTcpState, msgVpnClient.webTxTcpState) &&
        Objects.equals(this.webTxTlsCipherDescription, msgVpnClient.webTxTlsCipherDescription) &&
        Objects.equals(this.webTxTlsVersion, msgVpnClient.webTxTlsVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(aclProfileName, aliasedFromMsgVpnName, alreadyBoundBindFailureCount, authorizationGroupName, averageRxByteRate, averageRxMsgRate, averageTxByteRate, averageTxMsgRate, bindRequestCount, bindSuccessCount, clientAddress, clientId, clientName, clientProfileName, clientUsername, controlRxByteCount, controlRxMsgCount, controlTxByteCount, controlTxMsgCount, cutThroughDeniedBindFailureCount, dataRxByteCount, dataRxMsgCount, dataTxByteCount, dataTxMsgCount, description, disabledBindFailureCount, dtoLocalPriority, dtoNetworkPriority, eliding, elidingTopicCount, elidingTopicPeakCount, guaranteedDeniedBindFailureCount, invalidSelectorBindFailureCount, largeMsgEventRaised, loginRxMsgCount, loginTxMsgCount, maxBindCountExceededBindFailureCount, maxElidingTopicCountEventRaised, mqttConnackErrorTxCount, mqttConnackTxCount, mqttConnectRxCount, mqttDisconnectRxCount, mqttPingreqRxCount, mqttPingrespTxCount, mqttPubackRxCount, mqttPubackTxCount, mqttPubcompTxCount, mqttPublishQos0RxCount, mqttPublishQos0TxCount, mqttPublishQos1RxCount, mqttPublishQos1TxCount, mqttPublishQos2RxCount, mqttPubrecTxCount, mqttPubrelRxCount, mqttSubackErrorTxCount, mqttSubackTxCount, mqttSubscribeRxCount, mqttUnsubackTxCount, mqttUnsubscribeRxCount, msgSpoolCongestionRxDiscardedMsgCount, msgSpoolRxDiscardedMsgCount, msgVpnName, noLocalDelivery, noSubscriptionMatchRxDiscardedMsgCount, originalClientUsername, otherBindFailureCount, platform, publishTopicAclRxDiscardedMsgCount, restHttpRequestRxByteCount, restHttpRequestRxMsgCount, restHttpRequestTxByteCount, restHttpRequestTxMsgCount, restHttpResponseErrorRxMsgCount, restHttpResponseErrorTxMsgCount, restHttpResponseRxByteCount, restHttpResponseRxMsgCount, restHttpResponseSuccessRxMsgCount, restHttpResponseSuccessTxMsgCount, restHttpResponseTimeoutRxMsgCount, restHttpResponseTimeoutTxMsgCount, restHttpResponseTxByteCount, restHttpResponseTxMsgCount, rxByteCount, rxByteRate, rxDiscardedMsgCount, rxMsgCount, rxMsgRate, scheduledDisconnectTime, slowSubscriber, softwareDate, softwareVersion, tlsCipherDescription, tlsDowngradedToPlainText, tlsVersion, topicParseErrorRxDiscardedMsgCount, txByteCount, txByteRate, txDiscardedMsgCount, txMsgCount, txMsgRate, uptime, user, virtualRouter, webInactiveTimeout, webMaxPayload, webParseErrorRxDiscardedMsgCount, webRemainingTimeout, webRxByteCount, webRxEncoding, webRxMsgCount, webRxProtocol, webRxRequestCount, webRxResponseCount, webRxTcpState, webRxTlsCipherDescription, webRxTlsVersion, webSessionId, webTxByteCount, webTxEncoding, webTxMsgCount, webTxProtocol, webTxRequestCount, webTxResponseCount, webTxTcpState, webTxTlsCipherDescription, webTxTlsVersion);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnClient {\n");
    
    sb.append("    aclProfileName: ").append(toIndentedString(aclProfileName)).append("\n");
    sb.append("    aliasedFromMsgVpnName: ").append(toIndentedString(aliasedFromMsgVpnName)).append("\n");
    sb.append("    alreadyBoundBindFailureCount: ").append(toIndentedString(alreadyBoundBindFailureCount)).append("\n");
    sb.append("    authorizationGroupName: ").append(toIndentedString(authorizationGroupName)).append("\n");
    sb.append("    averageRxByteRate: ").append(toIndentedString(averageRxByteRate)).append("\n");
    sb.append("    averageRxMsgRate: ").append(toIndentedString(averageRxMsgRate)).append("\n");
    sb.append("    averageTxByteRate: ").append(toIndentedString(averageTxByteRate)).append("\n");
    sb.append("    averageTxMsgRate: ").append(toIndentedString(averageTxMsgRate)).append("\n");
    sb.append("    bindRequestCount: ").append(toIndentedString(bindRequestCount)).append("\n");
    sb.append("    bindSuccessCount: ").append(toIndentedString(bindSuccessCount)).append("\n");
    sb.append("    clientAddress: ").append(toIndentedString(clientAddress)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    clientName: ").append(toIndentedString(clientName)).append("\n");
    sb.append("    clientProfileName: ").append(toIndentedString(clientProfileName)).append("\n");
    sb.append("    clientUsername: ").append(toIndentedString(clientUsername)).append("\n");
    sb.append("    controlRxByteCount: ").append(toIndentedString(controlRxByteCount)).append("\n");
    sb.append("    controlRxMsgCount: ").append(toIndentedString(controlRxMsgCount)).append("\n");
    sb.append("    controlTxByteCount: ").append(toIndentedString(controlTxByteCount)).append("\n");
    sb.append("    controlTxMsgCount: ").append(toIndentedString(controlTxMsgCount)).append("\n");
    sb.append("    cutThroughDeniedBindFailureCount: ").append(toIndentedString(cutThroughDeniedBindFailureCount)).append("\n");
    sb.append("    dataRxByteCount: ").append(toIndentedString(dataRxByteCount)).append("\n");
    sb.append("    dataRxMsgCount: ").append(toIndentedString(dataRxMsgCount)).append("\n");
    sb.append("    dataTxByteCount: ").append(toIndentedString(dataTxByteCount)).append("\n");
    sb.append("    dataTxMsgCount: ").append(toIndentedString(dataTxMsgCount)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    disabledBindFailureCount: ").append(toIndentedString(disabledBindFailureCount)).append("\n");
    sb.append("    dtoLocalPriority: ").append(toIndentedString(dtoLocalPriority)).append("\n");
    sb.append("    dtoNetworkPriority: ").append(toIndentedString(dtoNetworkPriority)).append("\n");
    sb.append("    eliding: ").append(toIndentedString(eliding)).append("\n");
    sb.append("    elidingTopicCount: ").append(toIndentedString(elidingTopicCount)).append("\n");
    sb.append("    elidingTopicPeakCount: ").append(toIndentedString(elidingTopicPeakCount)).append("\n");
    sb.append("    guaranteedDeniedBindFailureCount: ").append(toIndentedString(guaranteedDeniedBindFailureCount)).append("\n");
    sb.append("    invalidSelectorBindFailureCount: ").append(toIndentedString(invalidSelectorBindFailureCount)).append("\n");
    sb.append("    largeMsgEventRaised: ").append(toIndentedString(largeMsgEventRaised)).append("\n");
    sb.append("    loginRxMsgCount: ").append(toIndentedString(loginRxMsgCount)).append("\n");
    sb.append("    loginTxMsgCount: ").append(toIndentedString(loginTxMsgCount)).append("\n");
    sb.append("    maxBindCountExceededBindFailureCount: ").append(toIndentedString(maxBindCountExceededBindFailureCount)).append("\n");
    sb.append("    maxElidingTopicCountEventRaised: ").append(toIndentedString(maxElidingTopicCountEventRaised)).append("\n");
    sb.append("    mqttConnackErrorTxCount: ").append(toIndentedString(mqttConnackErrorTxCount)).append("\n");
    sb.append("    mqttConnackTxCount: ").append(toIndentedString(mqttConnackTxCount)).append("\n");
    sb.append("    mqttConnectRxCount: ").append(toIndentedString(mqttConnectRxCount)).append("\n");
    sb.append("    mqttDisconnectRxCount: ").append(toIndentedString(mqttDisconnectRxCount)).append("\n");
    sb.append("    mqttPingreqRxCount: ").append(toIndentedString(mqttPingreqRxCount)).append("\n");
    sb.append("    mqttPingrespTxCount: ").append(toIndentedString(mqttPingrespTxCount)).append("\n");
    sb.append("    mqttPubackRxCount: ").append(toIndentedString(mqttPubackRxCount)).append("\n");
    sb.append("    mqttPubackTxCount: ").append(toIndentedString(mqttPubackTxCount)).append("\n");
    sb.append("    mqttPubcompTxCount: ").append(toIndentedString(mqttPubcompTxCount)).append("\n");
    sb.append("    mqttPublishQos0RxCount: ").append(toIndentedString(mqttPublishQos0RxCount)).append("\n");
    sb.append("    mqttPublishQos0TxCount: ").append(toIndentedString(mqttPublishQos0TxCount)).append("\n");
    sb.append("    mqttPublishQos1RxCount: ").append(toIndentedString(mqttPublishQos1RxCount)).append("\n");
    sb.append("    mqttPublishQos1TxCount: ").append(toIndentedString(mqttPublishQos1TxCount)).append("\n");
    sb.append("    mqttPublishQos2RxCount: ").append(toIndentedString(mqttPublishQos2RxCount)).append("\n");
    sb.append("    mqttPubrecTxCount: ").append(toIndentedString(mqttPubrecTxCount)).append("\n");
    sb.append("    mqttPubrelRxCount: ").append(toIndentedString(mqttPubrelRxCount)).append("\n");
    sb.append("    mqttSubackErrorTxCount: ").append(toIndentedString(mqttSubackErrorTxCount)).append("\n");
    sb.append("    mqttSubackTxCount: ").append(toIndentedString(mqttSubackTxCount)).append("\n");
    sb.append("    mqttSubscribeRxCount: ").append(toIndentedString(mqttSubscribeRxCount)).append("\n");
    sb.append("    mqttUnsubackTxCount: ").append(toIndentedString(mqttUnsubackTxCount)).append("\n");
    sb.append("    mqttUnsubscribeRxCount: ").append(toIndentedString(mqttUnsubscribeRxCount)).append("\n");
    sb.append("    msgSpoolCongestionRxDiscardedMsgCount: ").append(toIndentedString(msgSpoolCongestionRxDiscardedMsgCount)).append("\n");
    sb.append("    msgSpoolRxDiscardedMsgCount: ").append(toIndentedString(msgSpoolRxDiscardedMsgCount)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    noLocalDelivery: ").append(toIndentedString(noLocalDelivery)).append("\n");
    sb.append("    noSubscriptionMatchRxDiscardedMsgCount: ").append(toIndentedString(noSubscriptionMatchRxDiscardedMsgCount)).append("\n");
    sb.append("    originalClientUsername: ").append(toIndentedString(originalClientUsername)).append("\n");
    sb.append("    otherBindFailureCount: ").append(toIndentedString(otherBindFailureCount)).append("\n");
    sb.append("    platform: ").append(toIndentedString(platform)).append("\n");
    sb.append("    publishTopicAclRxDiscardedMsgCount: ").append(toIndentedString(publishTopicAclRxDiscardedMsgCount)).append("\n");
    sb.append("    restHttpRequestRxByteCount: ").append(toIndentedString(restHttpRequestRxByteCount)).append("\n");
    sb.append("    restHttpRequestRxMsgCount: ").append(toIndentedString(restHttpRequestRxMsgCount)).append("\n");
    sb.append("    restHttpRequestTxByteCount: ").append(toIndentedString(restHttpRequestTxByteCount)).append("\n");
    sb.append("    restHttpRequestTxMsgCount: ").append(toIndentedString(restHttpRequestTxMsgCount)).append("\n");
    sb.append("    restHttpResponseErrorRxMsgCount: ").append(toIndentedString(restHttpResponseErrorRxMsgCount)).append("\n");
    sb.append("    restHttpResponseErrorTxMsgCount: ").append(toIndentedString(restHttpResponseErrorTxMsgCount)).append("\n");
    sb.append("    restHttpResponseRxByteCount: ").append(toIndentedString(restHttpResponseRxByteCount)).append("\n");
    sb.append("    restHttpResponseRxMsgCount: ").append(toIndentedString(restHttpResponseRxMsgCount)).append("\n");
    sb.append("    restHttpResponseSuccessRxMsgCount: ").append(toIndentedString(restHttpResponseSuccessRxMsgCount)).append("\n");
    sb.append("    restHttpResponseSuccessTxMsgCount: ").append(toIndentedString(restHttpResponseSuccessTxMsgCount)).append("\n");
    sb.append("    restHttpResponseTimeoutRxMsgCount: ").append(toIndentedString(restHttpResponseTimeoutRxMsgCount)).append("\n");
    sb.append("    restHttpResponseTimeoutTxMsgCount: ").append(toIndentedString(restHttpResponseTimeoutTxMsgCount)).append("\n");
    sb.append("    restHttpResponseTxByteCount: ").append(toIndentedString(restHttpResponseTxByteCount)).append("\n");
    sb.append("    restHttpResponseTxMsgCount: ").append(toIndentedString(restHttpResponseTxMsgCount)).append("\n");
    sb.append("    rxByteCount: ").append(toIndentedString(rxByteCount)).append("\n");
    sb.append("    rxByteRate: ").append(toIndentedString(rxByteRate)).append("\n");
    sb.append("    rxDiscardedMsgCount: ").append(toIndentedString(rxDiscardedMsgCount)).append("\n");
    sb.append("    rxMsgCount: ").append(toIndentedString(rxMsgCount)).append("\n");
    sb.append("    rxMsgRate: ").append(toIndentedString(rxMsgRate)).append("\n");
    sb.append("    scheduledDisconnectTime: ").append(toIndentedString(scheduledDisconnectTime)).append("\n");
    sb.append("    slowSubscriber: ").append(toIndentedString(slowSubscriber)).append("\n");
    sb.append("    softwareDate: ").append(toIndentedString(softwareDate)).append("\n");
    sb.append("    softwareVersion: ").append(toIndentedString(softwareVersion)).append("\n");
    sb.append("    tlsCipherDescription: ").append(toIndentedString(tlsCipherDescription)).append("\n");
    sb.append("    tlsDowngradedToPlainText: ").append(toIndentedString(tlsDowngradedToPlainText)).append("\n");
    sb.append("    tlsVersion: ").append(toIndentedString(tlsVersion)).append("\n");
    sb.append("    topicParseErrorRxDiscardedMsgCount: ").append(toIndentedString(topicParseErrorRxDiscardedMsgCount)).append("\n");
    sb.append("    txByteCount: ").append(toIndentedString(txByteCount)).append("\n");
    sb.append("    txByteRate: ").append(toIndentedString(txByteRate)).append("\n");
    sb.append("    txDiscardedMsgCount: ").append(toIndentedString(txDiscardedMsgCount)).append("\n");
    sb.append("    txMsgCount: ").append(toIndentedString(txMsgCount)).append("\n");
    sb.append("    txMsgRate: ").append(toIndentedString(txMsgRate)).append("\n");
    sb.append("    uptime: ").append(toIndentedString(uptime)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    virtualRouter: ").append(toIndentedString(virtualRouter)).append("\n");
    sb.append("    webInactiveTimeout: ").append(toIndentedString(webInactiveTimeout)).append("\n");
    sb.append("    webMaxPayload: ").append(toIndentedString(webMaxPayload)).append("\n");
    sb.append("    webParseErrorRxDiscardedMsgCount: ").append(toIndentedString(webParseErrorRxDiscardedMsgCount)).append("\n");
    sb.append("    webRemainingTimeout: ").append(toIndentedString(webRemainingTimeout)).append("\n");
    sb.append("    webRxByteCount: ").append(toIndentedString(webRxByteCount)).append("\n");
    sb.append("    webRxEncoding: ").append(toIndentedString(webRxEncoding)).append("\n");
    sb.append("    webRxMsgCount: ").append(toIndentedString(webRxMsgCount)).append("\n");
    sb.append("    webRxProtocol: ").append(toIndentedString(webRxProtocol)).append("\n");
    sb.append("    webRxRequestCount: ").append(toIndentedString(webRxRequestCount)).append("\n");
    sb.append("    webRxResponseCount: ").append(toIndentedString(webRxResponseCount)).append("\n");
    sb.append("    webRxTcpState: ").append(toIndentedString(webRxTcpState)).append("\n");
    sb.append("    webRxTlsCipherDescription: ").append(toIndentedString(webRxTlsCipherDescription)).append("\n");
    sb.append("    webRxTlsVersion: ").append(toIndentedString(webRxTlsVersion)).append("\n");
    sb.append("    webSessionId: ").append(toIndentedString(webSessionId)).append("\n");
    sb.append("    webTxByteCount: ").append(toIndentedString(webTxByteCount)).append("\n");
    sb.append("    webTxEncoding: ").append(toIndentedString(webTxEncoding)).append("\n");
    sb.append("    webTxMsgCount: ").append(toIndentedString(webTxMsgCount)).append("\n");
    sb.append("    webTxProtocol: ").append(toIndentedString(webTxProtocol)).append("\n");
    sb.append("    webTxRequestCount: ").append(toIndentedString(webTxRequestCount)).append("\n");
    sb.append("    webTxResponseCount: ").append(toIndentedString(webTxResponseCount)).append("\n");
    sb.append("    webTxTcpState: ").append(toIndentedString(webTxTcpState)).append("\n");
    sb.append("    webTxTlsCipherDescription: ").append(toIndentedString(webTxTlsCipherDescription)).append("\n");
    sb.append("    webTxTlsVersion: ").append(toIndentedString(webTxTlsVersion)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

