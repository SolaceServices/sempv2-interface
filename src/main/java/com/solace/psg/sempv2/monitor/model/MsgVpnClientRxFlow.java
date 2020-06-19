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
 * MsgVpnClientRxFlow
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-13T23:07:13.589Z")
public class MsgVpnClientRxFlow {
  @SerializedName("clientName")
  private String clientName = null;

  @SerializedName("connectTime")
  private Integer connectTime = null;

  @SerializedName("destinationGroupErrorDiscardedMsgCount")
  private Long destinationGroupErrorDiscardedMsgCount = null;

  @SerializedName("duplicateDiscardedMsgCount")
  private Long duplicateDiscardedMsgCount = null;

  @SerializedName("endpointDisabledDiscardedMsgCount")
  private Long endpointDisabledDiscardedMsgCount = null;

  @SerializedName("endpointUsageExceededDiscardedMsgCount")
  private Long endpointUsageExceededDiscardedMsgCount = null;

  @SerializedName("erroredDiscardedMsgCount")
  private Long erroredDiscardedMsgCount = null;

  @SerializedName("flowId")
  private Long flowId = null;

  @SerializedName("flowName")
  private String flowName = null;

  @SerializedName("guaranteedMsgCount")
  private Long guaranteedMsgCount = null;

  @SerializedName("lastRxMsgId")
  private Long lastRxMsgId = null;

  @SerializedName("localMsgCountExceededDiscardedMsgCount")
  private Long localMsgCountExceededDiscardedMsgCount = null;

  @SerializedName("lowPriorityMsgCongestionDiscardedMsgCount")
  private Long lowPriorityMsgCongestionDiscardedMsgCount = null;

  @SerializedName("maxMsgSizeExceededDiscardedMsgCount")
  private Long maxMsgSizeExceededDiscardedMsgCount = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("noEligibleDestinationsDiscardedMsgCount")
  private Long noEligibleDestinationsDiscardedMsgCount = null;

  @SerializedName("noLocalDeliveryDiscardedMsgCount")
  private Long noLocalDeliveryDiscardedMsgCount = null;

  @SerializedName("notCompatibleWithForwardingModeDiscardedMsgCount")
  private Long notCompatibleWithForwardingModeDiscardedMsgCount = null;

  @SerializedName("outOfOrderDiscardedMsgCount")
  private Long outOfOrderDiscardedMsgCount = null;

  @SerializedName("publishAclDeniedDiscardedMsgCount")
  private Long publishAclDeniedDiscardedMsgCount = null;

  @SerializedName("publisherId")
  private Long publisherId = null;

  @SerializedName("queueNotFoundDiscardedMsgCount")
  private Long queueNotFoundDiscardedMsgCount = null;

  @SerializedName("replicationStandbyDiscardedMsgCount")
  private Long replicationStandbyDiscardedMsgCount = null;

  @SerializedName("sessionName")
  private String sessionName = null;

  @SerializedName("smfTtlExceededDiscardedMsgCount")
  private Long smfTtlExceededDiscardedMsgCount = null;

  @SerializedName("spoolFileLimitExceededDiscardedMsgCount")
  private Long spoolFileLimitExceededDiscardedMsgCount = null;

  @SerializedName("spoolNotReadyDiscardedMsgCount")
  private Long spoolNotReadyDiscardedMsgCount = null;

  @SerializedName("spoolToAdbFailDiscardedMsgCount")
  private Long spoolToAdbFailDiscardedMsgCount = null;

  @SerializedName("spoolToDiskFailDiscardedMsgCount")
  private Long spoolToDiskFailDiscardedMsgCount = null;

  @SerializedName("spoolUsageExceededDiscardedMsgCount")
  private Long spoolUsageExceededDiscardedMsgCount = null;

  @SerializedName("syncReplicationIneligibleDiscardedMsgCount")
  private Long syncReplicationIneligibleDiscardedMsgCount = null;

  @SerializedName("userProfileDeniedGuaranteedDiscardedMsgCount")
  private Long userProfileDeniedGuaranteedDiscardedMsgCount = null;

  @SerializedName("windowSize")
  private Integer windowSize = null;

  public MsgVpnClientRxFlow clientName(String clientName) {
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

  public MsgVpnClientRxFlow connectTime(Integer connectTime) {
    this.connectTime = connectTime;
    return this;
  }

   /**
   * The timestamp of when the Flow from the Client connected.
   * @return connectTime
  **/
  @ApiModelProperty(value = "The timestamp of when the Flow from the Client connected.")
  public Integer getConnectTime() {
    return connectTime;
  }

  public void setConnectTime(Integer connectTime) {
    this.connectTime = connectTime;
  }

  public MsgVpnClientRxFlow destinationGroupErrorDiscardedMsgCount(Long destinationGroupErrorDiscardedMsgCount) {
    this.destinationGroupErrorDiscardedMsgCount = destinationGroupErrorDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to a destination group error.
   * @return destinationGroupErrorDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to a destination group error.")
  public Long getDestinationGroupErrorDiscardedMsgCount() {
    return destinationGroupErrorDiscardedMsgCount;
  }

  public void setDestinationGroupErrorDiscardedMsgCount(Long destinationGroupErrorDiscardedMsgCount) {
    this.destinationGroupErrorDiscardedMsgCount = destinationGroupErrorDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow duplicateDiscardedMsgCount(Long duplicateDiscardedMsgCount) {
    this.duplicateDiscardedMsgCount = duplicateDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to being a duplicate.
   * @return duplicateDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to being a duplicate.")
  public Long getDuplicateDiscardedMsgCount() {
    return duplicateDiscardedMsgCount;
  }

  public void setDuplicateDiscardedMsgCount(Long duplicateDiscardedMsgCount) {
    this.duplicateDiscardedMsgCount = duplicateDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow endpointDisabledDiscardedMsgCount(Long endpointDisabledDiscardedMsgCount) {
    this.endpointDisabledDiscardedMsgCount = endpointDisabledDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to an eligible endpoint destination being disabled.
   * @return endpointDisabledDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to an eligible endpoint destination being disabled.")
  public Long getEndpointDisabledDiscardedMsgCount() {
    return endpointDisabledDiscardedMsgCount;
  }

  public void setEndpointDisabledDiscardedMsgCount(Long endpointDisabledDiscardedMsgCount) {
    this.endpointDisabledDiscardedMsgCount = endpointDisabledDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow endpointUsageExceededDiscardedMsgCount(Long endpointUsageExceededDiscardedMsgCount) {
    this.endpointUsageExceededDiscardedMsgCount = endpointUsageExceededDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to an eligible endpoint destination having its maximum message spool usage exceeded.
   * @return endpointUsageExceededDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to an eligible endpoint destination having its maximum message spool usage exceeded.")
  public Long getEndpointUsageExceededDiscardedMsgCount() {
    return endpointUsageExceededDiscardedMsgCount;
  }

  public void setEndpointUsageExceededDiscardedMsgCount(Long endpointUsageExceededDiscardedMsgCount) {
    this.endpointUsageExceededDiscardedMsgCount = endpointUsageExceededDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow erroredDiscardedMsgCount(Long erroredDiscardedMsgCount) {
    this.erroredDiscardedMsgCount = erroredDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to errors being detected.
   * @return erroredDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to errors being detected.")
  public Long getErroredDiscardedMsgCount() {
    return erroredDiscardedMsgCount;
  }

  public void setErroredDiscardedMsgCount(Long erroredDiscardedMsgCount) {
    this.erroredDiscardedMsgCount = erroredDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow flowId(Long flowId) {
    this.flowId = flowId;
    return this;
  }

   /**
   * The identifier (ID) of the flow.
   * @return flowId
  **/
  @ApiModelProperty(value = "The identifier (ID) of the flow.")
  public Long getFlowId() {
    return flowId;
  }

  public void setFlowId(Long flowId) {
    this.flowId = flowId;
  }

  public MsgVpnClientRxFlow flowName(String flowName) {
    this.flowName = flowName;
    return this;
  }

   /**
   * The name of the Flow.
   * @return flowName
  **/
  @ApiModelProperty(value = "The name of the Flow.")
  public String getFlowName() {
    return flowName;
  }

  public void setFlowName(String flowName) {
    this.flowName = flowName;
  }

  public MsgVpnClientRxFlow guaranteedMsgCount(Long guaranteedMsgCount) {
    this.guaranteedMsgCount = guaranteedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow.
   * @return guaranteedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow.")
  public Long getGuaranteedMsgCount() {
    return guaranteedMsgCount;
  }

  public void setGuaranteedMsgCount(Long guaranteedMsgCount) {
    this.guaranteedMsgCount = guaranteedMsgCount;
  }

  public MsgVpnClientRxFlow lastRxMsgId(Long lastRxMsgId) {
    this.lastRxMsgId = lastRxMsgId;
    return this;
  }

   /**
   * The identifier (ID) of the last message received on the Flow.
   * @return lastRxMsgId
  **/
  @ApiModelProperty(value = "The identifier (ID) of the last message received on the Flow.")
  public Long getLastRxMsgId() {
    return lastRxMsgId;
  }

  public void setLastRxMsgId(Long lastRxMsgId) {
    this.lastRxMsgId = lastRxMsgId;
  }

  public MsgVpnClientRxFlow localMsgCountExceededDiscardedMsgCount(Long localMsgCountExceededDiscardedMsgCount) {
    this.localMsgCountExceededDiscardedMsgCount = localMsgCountExceededDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to the maximum number of messages allowed on the broker being exceeded.
   * @return localMsgCountExceededDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to the maximum number of messages allowed on the broker being exceeded.")
  public Long getLocalMsgCountExceededDiscardedMsgCount() {
    return localMsgCountExceededDiscardedMsgCount;
  }

  public void setLocalMsgCountExceededDiscardedMsgCount(Long localMsgCountExceededDiscardedMsgCount) {
    this.localMsgCountExceededDiscardedMsgCount = localMsgCountExceededDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow lowPriorityMsgCongestionDiscardedMsgCount(Long lowPriorityMsgCongestionDiscardedMsgCount) {
    this.lowPriorityMsgCongestionDiscardedMsgCount = lowPriorityMsgCongestionDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to congestion of low priority messages.
   * @return lowPriorityMsgCongestionDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to congestion of low priority messages.")
  public Long getLowPriorityMsgCongestionDiscardedMsgCount() {
    return lowPriorityMsgCongestionDiscardedMsgCount;
  }

  public void setLowPriorityMsgCongestionDiscardedMsgCount(Long lowPriorityMsgCongestionDiscardedMsgCount) {
    this.lowPriorityMsgCongestionDiscardedMsgCount = lowPriorityMsgCongestionDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow maxMsgSizeExceededDiscardedMsgCount(Long maxMsgSizeExceededDiscardedMsgCount) {
    this.maxMsgSizeExceededDiscardedMsgCount = maxMsgSizeExceededDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to the maximum allowed message size being exceeded.
   * @return maxMsgSizeExceededDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to the maximum allowed message size being exceeded.")
  public Long getMaxMsgSizeExceededDiscardedMsgCount() {
    return maxMsgSizeExceededDiscardedMsgCount;
  }

  public void setMaxMsgSizeExceededDiscardedMsgCount(Long maxMsgSizeExceededDiscardedMsgCount) {
    this.maxMsgSizeExceededDiscardedMsgCount = maxMsgSizeExceededDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow msgVpnName(String msgVpnName) {
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

  public MsgVpnClientRxFlow noEligibleDestinationsDiscardedMsgCount(Long noEligibleDestinationsDiscardedMsgCount) {
    this.noEligibleDestinationsDiscardedMsgCount = noEligibleDestinationsDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to there being no eligible endpoint destination.
   * @return noEligibleDestinationsDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to there being no eligible endpoint destination.")
  public Long getNoEligibleDestinationsDiscardedMsgCount() {
    return noEligibleDestinationsDiscardedMsgCount;
  }

  public void setNoEligibleDestinationsDiscardedMsgCount(Long noEligibleDestinationsDiscardedMsgCount) {
    this.noEligibleDestinationsDiscardedMsgCount = noEligibleDestinationsDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow noLocalDeliveryDiscardedMsgCount(Long noLocalDeliveryDiscardedMsgCount) {
    this.noLocalDeliveryDiscardedMsgCount = noLocalDeliveryDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to no local delivery being requested.
   * @return noLocalDeliveryDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to no local delivery being requested.")
  public Long getNoLocalDeliveryDiscardedMsgCount() {
    return noLocalDeliveryDiscardedMsgCount;
  }

  public void setNoLocalDeliveryDiscardedMsgCount(Long noLocalDeliveryDiscardedMsgCount) {
    this.noLocalDeliveryDiscardedMsgCount = noLocalDeliveryDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow notCompatibleWithForwardingModeDiscardedMsgCount(Long notCompatibleWithForwardingModeDiscardedMsgCount) {
    this.notCompatibleWithForwardingModeDiscardedMsgCount = notCompatibleWithForwardingModeDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to being incompatible with the forwarding mode of an eligible endpoint destination.
   * @return notCompatibleWithForwardingModeDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to being incompatible with the forwarding mode of an eligible endpoint destination.")
  public Long getNotCompatibleWithForwardingModeDiscardedMsgCount() {
    return notCompatibleWithForwardingModeDiscardedMsgCount;
  }

  public void setNotCompatibleWithForwardingModeDiscardedMsgCount(Long notCompatibleWithForwardingModeDiscardedMsgCount) {
    this.notCompatibleWithForwardingModeDiscardedMsgCount = notCompatibleWithForwardingModeDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow outOfOrderDiscardedMsgCount(Long outOfOrderDiscardedMsgCount) {
    this.outOfOrderDiscardedMsgCount = outOfOrderDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to being received out of order.
   * @return outOfOrderDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to being received out of order.")
  public Long getOutOfOrderDiscardedMsgCount() {
    return outOfOrderDiscardedMsgCount;
  }

  public void setOutOfOrderDiscardedMsgCount(Long outOfOrderDiscardedMsgCount) {
    this.outOfOrderDiscardedMsgCount = outOfOrderDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow publishAclDeniedDiscardedMsgCount(Long publishAclDeniedDiscardedMsgCount) {
    this.publishAclDeniedDiscardedMsgCount = publishAclDeniedDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to being denied by the access control list (ACL) profile for the published topic.
   * @return publishAclDeniedDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to being denied by the access control list (ACL) profile for the published topic.")
  public Long getPublishAclDeniedDiscardedMsgCount() {
    return publishAclDeniedDiscardedMsgCount;
  }

  public void setPublishAclDeniedDiscardedMsgCount(Long publishAclDeniedDiscardedMsgCount) {
    this.publishAclDeniedDiscardedMsgCount = publishAclDeniedDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow publisherId(Long publisherId) {
    this.publisherId = publisherId;
    return this;
  }

   /**
   * The identifier (ID) of the publisher for the Flow.
   * @return publisherId
  **/
  @ApiModelProperty(value = "The identifier (ID) of the publisher for the Flow.")
  public Long getPublisherId() {
    return publisherId;
  }

  public void setPublisherId(Long publisherId) {
    this.publisherId = publisherId;
  }

  public MsgVpnClientRxFlow queueNotFoundDiscardedMsgCount(Long queueNotFoundDiscardedMsgCount) {
    this.queueNotFoundDiscardedMsgCount = queueNotFoundDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to the destination queue not being found.
   * @return queueNotFoundDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to the destination queue not being found.")
  public Long getQueueNotFoundDiscardedMsgCount() {
    return queueNotFoundDiscardedMsgCount;
  }

  public void setQueueNotFoundDiscardedMsgCount(Long queueNotFoundDiscardedMsgCount) {
    this.queueNotFoundDiscardedMsgCount = queueNotFoundDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow replicationStandbyDiscardedMsgCount(Long replicationStandbyDiscardedMsgCount) {
    this.replicationStandbyDiscardedMsgCount = replicationStandbyDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to the Message VPN being in the replication standby state.
   * @return replicationStandbyDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to the Message VPN being in the replication standby state.")
  public Long getReplicationStandbyDiscardedMsgCount() {
    return replicationStandbyDiscardedMsgCount;
  }

  public void setReplicationStandbyDiscardedMsgCount(Long replicationStandbyDiscardedMsgCount) {
    this.replicationStandbyDiscardedMsgCount = replicationStandbyDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow sessionName(String sessionName) {
    this.sessionName = sessionName;
    return this;
  }

   /**
   * The name of the transacted session on the Flow.
   * @return sessionName
  **/
  @ApiModelProperty(value = "The name of the transacted session on the Flow.")
  public String getSessionName() {
    return sessionName;
  }

  public void setSessionName(String sessionName) {
    this.sessionName = sessionName;
  }

  public MsgVpnClientRxFlow smfTtlExceededDiscardedMsgCount(Long smfTtlExceededDiscardedMsgCount) {
    this.smfTtlExceededDiscardedMsgCount = smfTtlExceededDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to the message time-to-live (TTL) count being exceeded. The message TTL count is the maximum number of times the message can cross a bridge between Message VPNs.
   * @return smfTtlExceededDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to the message time-to-live (TTL) count being exceeded. The message TTL count is the maximum number of times the message can cross a bridge between Message VPNs.")
  public Long getSmfTtlExceededDiscardedMsgCount() {
    return smfTtlExceededDiscardedMsgCount;
  }

  public void setSmfTtlExceededDiscardedMsgCount(Long smfTtlExceededDiscardedMsgCount) {
    this.smfTtlExceededDiscardedMsgCount = smfTtlExceededDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow spoolFileLimitExceededDiscardedMsgCount(Long spoolFileLimitExceededDiscardedMsgCount) {
    this.spoolFileLimitExceededDiscardedMsgCount = spoolFileLimitExceededDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to all available message spool file resources being used.
   * @return spoolFileLimitExceededDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to all available message spool file resources being used.")
  public Long getSpoolFileLimitExceededDiscardedMsgCount() {
    return spoolFileLimitExceededDiscardedMsgCount;
  }

  public void setSpoolFileLimitExceededDiscardedMsgCount(Long spoolFileLimitExceededDiscardedMsgCount) {
    this.spoolFileLimitExceededDiscardedMsgCount = spoolFileLimitExceededDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow spoolNotReadyDiscardedMsgCount(Long spoolNotReadyDiscardedMsgCount) {
    this.spoolNotReadyDiscardedMsgCount = spoolNotReadyDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to the message spool being not ready.
   * @return spoolNotReadyDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to the message spool being not ready.")
  public Long getSpoolNotReadyDiscardedMsgCount() {
    return spoolNotReadyDiscardedMsgCount;
  }

  public void setSpoolNotReadyDiscardedMsgCount(Long spoolNotReadyDiscardedMsgCount) {
    this.spoolNotReadyDiscardedMsgCount = spoolNotReadyDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow spoolToAdbFailDiscardedMsgCount(Long spoolToAdbFailDiscardedMsgCount) {
    this.spoolToAdbFailDiscardedMsgCount = spoolToAdbFailDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to a failure while spooling to the Assured Delivery Blade (ADB).
   * @return spoolToAdbFailDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to a failure while spooling to the Assured Delivery Blade (ADB).")
  public Long getSpoolToAdbFailDiscardedMsgCount() {
    return spoolToAdbFailDiscardedMsgCount;
  }

  public void setSpoolToAdbFailDiscardedMsgCount(Long spoolToAdbFailDiscardedMsgCount) {
    this.spoolToAdbFailDiscardedMsgCount = spoolToAdbFailDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow spoolToDiskFailDiscardedMsgCount(Long spoolToDiskFailDiscardedMsgCount) {
    this.spoolToDiskFailDiscardedMsgCount = spoolToDiskFailDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to a failure while spooling to the disk.
   * @return spoolToDiskFailDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to a failure while spooling to the disk.")
  public Long getSpoolToDiskFailDiscardedMsgCount() {
    return spoolToDiskFailDiscardedMsgCount;
  }

  public void setSpoolToDiskFailDiscardedMsgCount(Long spoolToDiskFailDiscardedMsgCount) {
    this.spoolToDiskFailDiscardedMsgCount = spoolToDiskFailDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow spoolUsageExceededDiscardedMsgCount(Long spoolUsageExceededDiscardedMsgCount) {
    this.spoolUsageExceededDiscardedMsgCount = spoolUsageExceededDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to the maximum message spool usage being exceeded.
   * @return spoolUsageExceededDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to the maximum message spool usage being exceeded.")
  public Long getSpoolUsageExceededDiscardedMsgCount() {
    return spoolUsageExceededDiscardedMsgCount;
  }

  public void setSpoolUsageExceededDiscardedMsgCount(Long spoolUsageExceededDiscardedMsgCount) {
    this.spoolUsageExceededDiscardedMsgCount = spoolUsageExceededDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow syncReplicationIneligibleDiscardedMsgCount(Long syncReplicationIneligibleDiscardedMsgCount) {
    this.syncReplicationIneligibleDiscardedMsgCount = syncReplicationIneligibleDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to synchronous replication being ineligible.
   * @return syncReplicationIneligibleDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to synchronous replication being ineligible.")
  public Long getSyncReplicationIneligibleDiscardedMsgCount() {
    return syncReplicationIneligibleDiscardedMsgCount;
  }

  public void setSyncReplicationIneligibleDiscardedMsgCount(Long syncReplicationIneligibleDiscardedMsgCount) {
    this.syncReplicationIneligibleDiscardedMsgCount = syncReplicationIneligibleDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow userProfileDeniedGuaranteedDiscardedMsgCount(Long userProfileDeniedGuaranteedDiscardedMsgCount) {
    this.userProfileDeniedGuaranteedDiscardedMsgCount = userProfileDeniedGuaranteedDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages from the Flow discarded due to being denied by the client profile.
   * @return userProfileDeniedGuaranteedDiscardedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages from the Flow discarded due to being denied by the client profile.")
  public Long getUserProfileDeniedGuaranteedDiscardedMsgCount() {
    return userProfileDeniedGuaranteedDiscardedMsgCount;
  }

  public void setUserProfileDeniedGuaranteedDiscardedMsgCount(Long userProfileDeniedGuaranteedDiscardedMsgCount) {
    this.userProfileDeniedGuaranteedDiscardedMsgCount = userProfileDeniedGuaranteedDiscardedMsgCount;
  }

  public MsgVpnClientRxFlow windowSize(Integer windowSize) {
    this.windowSize = windowSize;
    return this;
  }

   /**
   * The size of the window used for guaranteed messages sent on the Flow, in messages.
   * @return windowSize
  **/
  @ApiModelProperty(value = "The size of the window used for guaranteed messages sent on the Flow, in messages.")
  public Integer getWindowSize() {
    return windowSize;
  }

  public void setWindowSize(Integer windowSize) {
    this.windowSize = windowSize;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnClientRxFlow msgVpnClientRxFlow = (MsgVpnClientRxFlow) o;
    return Objects.equals(this.clientName, msgVpnClientRxFlow.clientName) &&
        Objects.equals(this.connectTime, msgVpnClientRxFlow.connectTime) &&
        Objects.equals(this.destinationGroupErrorDiscardedMsgCount, msgVpnClientRxFlow.destinationGroupErrorDiscardedMsgCount) &&
        Objects.equals(this.duplicateDiscardedMsgCount, msgVpnClientRxFlow.duplicateDiscardedMsgCount) &&
        Objects.equals(this.endpointDisabledDiscardedMsgCount, msgVpnClientRxFlow.endpointDisabledDiscardedMsgCount) &&
        Objects.equals(this.endpointUsageExceededDiscardedMsgCount, msgVpnClientRxFlow.endpointUsageExceededDiscardedMsgCount) &&
        Objects.equals(this.erroredDiscardedMsgCount, msgVpnClientRxFlow.erroredDiscardedMsgCount) &&
        Objects.equals(this.flowId, msgVpnClientRxFlow.flowId) &&
        Objects.equals(this.flowName, msgVpnClientRxFlow.flowName) &&
        Objects.equals(this.guaranteedMsgCount, msgVpnClientRxFlow.guaranteedMsgCount) &&
        Objects.equals(this.lastRxMsgId, msgVpnClientRxFlow.lastRxMsgId) &&
        Objects.equals(this.localMsgCountExceededDiscardedMsgCount, msgVpnClientRxFlow.localMsgCountExceededDiscardedMsgCount) &&
        Objects.equals(this.lowPriorityMsgCongestionDiscardedMsgCount, msgVpnClientRxFlow.lowPriorityMsgCongestionDiscardedMsgCount) &&
        Objects.equals(this.maxMsgSizeExceededDiscardedMsgCount, msgVpnClientRxFlow.maxMsgSizeExceededDiscardedMsgCount) &&
        Objects.equals(this.msgVpnName, msgVpnClientRxFlow.msgVpnName) &&
        Objects.equals(this.noEligibleDestinationsDiscardedMsgCount, msgVpnClientRxFlow.noEligibleDestinationsDiscardedMsgCount) &&
        Objects.equals(this.noLocalDeliveryDiscardedMsgCount, msgVpnClientRxFlow.noLocalDeliveryDiscardedMsgCount) &&
        Objects.equals(this.notCompatibleWithForwardingModeDiscardedMsgCount, msgVpnClientRxFlow.notCompatibleWithForwardingModeDiscardedMsgCount) &&
        Objects.equals(this.outOfOrderDiscardedMsgCount, msgVpnClientRxFlow.outOfOrderDiscardedMsgCount) &&
        Objects.equals(this.publishAclDeniedDiscardedMsgCount, msgVpnClientRxFlow.publishAclDeniedDiscardedMsgCount) &&
        Objects.equals(this.publisherId, msgVpnClientRxFlow.publisherId) &&
        Objects.equals(this.queueNotFoundDiscardedMsgCount, msgVpnClientRxFlow.queueNotFoundDiscardedMsgCount) &&
        Objects.equals(this.replicationStandbyDiscardedMsgCount, msgVpnClientRxFlow.replicationStandbyDiscardedMsgCount) &&
        Objects.equals(this.sessionName, msgVpnClientRxFlow.sessionName) &&
        Objects.equals(this.smfTtlExceededDiscardedMsgCount, msgVpnClientRxFlow.smfTtlExceededDiscardedMsgCount) &&
        Objects.equals(this.spoolFileLimitExceededDiscardedMsgCount, msgVpnClientRxFlow.spoolFileLimitExceededDiscardedMsgCount) &&
        Objects.equals(this.spoolNotReadyDiscardedMsgCount, msgVpnClientRxFlow.spoolNotReadyDiscardedMsgCount) &&
        Objects.equals(this.spoolToAdbFailDiscardedMsgCount, msgVpnClientRxFlow.spoolToAdbFailDiscardedMsgCount) &&
        Objects.equals(this.spoolToDiskFailDiscardedMsgCount, msgVpnClientRxFlow.spoolToDiskFailDiscardedMsgCount) &&
        Objects.equals(this.spoolUsageExceededDiscardedMsgCount, msgVpnClientRxFlow.spoolUsageExceededDiscardedMsgCount) &&
        Objects.equals(this.syncReplicationIneligibleDiscardedMsgCount, msgVpnClientRxFlow.syncReplicationIneligibleDiscardedMsgCount) &&
        Objects.equals(this.userProfileDeniedGuaranteedDiscardedMsgCount, msgVpnClientRxFlow.userProfileDeniedGuaranteedDiscardedMsgCount) &&
        Objects.equals(this.windowSize, msgVpnClientRxFlow.windowSize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clientName, connectTime, destinationGroupErrorDiscardedMsgCount, duplicateDiscardedMsgCount, endpointDisabledDiscardedMsgCount, endpointUsageExceededDiscardedMsgCount, erroredDiscardedMsgCount, flowId, flowName, guaranteedMsgCount, lastRxMsgId, localMsgCountExceededDiscardedMsgCount, lowPriorityMsgCongestionDiscardedMsgCount, maxMsgSizeExceededDiscardedMsgCount, msgVpnName, noEligibleDestinationsDiscardedMsgCount, noLocalDeliveryDiscardedMsgCount, notCompatibleWithForwardingModeDiscardedMsgCount, outOfOrderDiscardedMsgCount, publishAclDeniedDiscardedMsgCount, publisherId, queueNotFoundDiscardedMsgCount, replicationStandbyDiscardedMsgCount, sessionName, smfTtlExceededDiscardedMsgCount, spoolFileLimitExceededDiscardedMsgCount, spoolNotReadyDiscardedMsgCount, spoolToAdbFailDiscardedMsgCount, spoolToDiskFailDiscardedMsgCount, spoolUsageExceededDiscardedMsgCount, syncReplicationIneligibleDiscardedMsgCount, userProfileDeniedGuaranteedDiscardedMsgCount, windowSize);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnClientRxFlow {\n");
    
    sb.append("    clientName: ").append(toIndentedString(clientName)).append("\n");
    sb.append("    connectTime: ").append(toIndentedString(connectTime)).append("\n");
    sb.append("    destinationGroupErrorDiscardedMsgCount: ").append(toIndentedString(destinationGroupErrorDiscardedMsgCount)).append("\n");
    sb.append("    duplicateDiscardedMsgCount: ").append(toIndentedString(duplicateDiscardedMsgCount)).append("\n");
    sb.append("    endpointDisabledDiscardedMsgCount: ").append(toIndentedString(endpointDisabledDiscardedMsgCount)).append("\n");
    sb.append("    endpointUsageExceededDiscardedMsgCount: ").append(toIndentedString(endpointUsageExceededDiscardedMsgCount)).append("\n");
    sb.append("    erroredDiscardedMsgCount: ").append(toIndentedString(erroredDiscardedMsgCount)).append("\n");
    sb.append("    flowId: ").append(toIndentedString(flowId)).append("\n");
    sb.append("    flowName: ").append(toIndentedString(flowName)).append("\n");
    sb.append("    guaranteedMsgCount: ").append(toIndentedString(guaranteedMsgCount)).append("\n");
    sb.append("    lastRxMsgId: ").append(toIndentedString(lastRxMsgId)).append("\n");
    sb.append("    localMsgCountExceededDiscardedMsgCount: ").append(toIndentedString(localMsgCountExceededDiscardedMsgCount)).append("\n");
    sb.append("    lowPriorityMsgCongestionDiscardedMsgCount: ").append(toIndentedString(lowPriorityMsgCongestionDiscardedMsgCount)).append("\n");
    sb.append("    maxMsgSizeExceededDiscardedMsgCount: ").append(toIndentedString(maxMsgSizeExceededDiscardedMsgCount)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    noEligibleDestinationsDiscardedMsgCount: ").append(toIndentedString(noEligibleDestinationsDiscardedMsgCount)).append("\n");
    sb.append("    noLocalDeliveryDiscardedMsgCount: ").append(toIndentedString(noLocalDeliveryDiscardedMsgCount)).append("\n");
    sb.append("    notCompatibleWithForwardingModeDiscardedMsgCount: ").append(toIndentedString(notCompatibleWithForwardingModeDiscardedMsgCount)).append("\n");
    sb.append("    outOfOrderDiscardedMsgCount: ").append(toIndentedString(outOfOrderDiscardedMsgCount)).append("\n");
    sb.append("    publishAclDeniedDiscardedMsgCount: ").append(toIndentedString(publishAclDeniedDiscardedMsgCount)).append("\n");
    sb.append("    publisherId: ").append(toIndentedString(publisherId)).append("\n");
    sb.append("    queueNotFoundDiscardedMsgCount: ").append(toIndentedString(queueNotFoundDiscardedMsgCount)).append("\n");
    sb.append("    replicationStandbyDiscardedMsgCount: ").append(toIndentedString(replicationStandbyDiscardedMsgCount)).append("\n");
    sb.append("    sessionName: ").append(toIndentedString(sessionName)).append("\n");
    sb.append("    smfTtlExceededDiscardedMsgCount: ").append(toIndentedString(smfTtlExceededDiscardedMsgCount)).append("\n");
    sb.append("    spoolFileLimitExceededDiscardedMsgCount: ").append(toIndentedString(spoolFileLimitExceededDiscardedMsgCount)).append("\n");
    sb.append("    spoolNotReadyDiscardedMsgCount: ").append(toIndentedString(spoolNotReadyDiscardedMsgCount)).append("\n");
    sb.append("    spoolToAdbFailDiscardedMsgCount: ").append(toIndentedString(spoolToAdbFailDiscardedMsgCount)).append("\n");
    sb.append("    spoolToDiskFailDiscardedMsgCount: ").append(toIndentedString(spoolToDiskFailDiscardedMsgCount)).append("\n");
    sb.append("    spoolUsageExceededDiscardedMsgCount: ").append(toIndentedString(spoolUsageExceededDiscardedMsgCount)).append("\n");
    sb.append("    syncReplicationIneligibleDiscardedMsgCount: ").append(toIndentedString(syncReplicationIneligibleDiscardedMsgCount)).append("\n");
    sb.append("    userProfileDeniedGuaranteedDiscardedMsgCount: ").append(toIndentedString(userProfileDeniedGuaranteedDiscardedMsgCount)).append("\n");
    sb.append("    windowSize: ").append(toIndentedString(windowSize)).append("\n");
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

