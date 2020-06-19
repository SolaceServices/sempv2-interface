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
 * MsgVpnQueueTxFlow
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-13T23:07:13.589Z")
public class MsgVpnQueueTxFlow {
  @SerializedName("ackedMsgCount")
  private Long ackedMsgCount = null;

  @SerializedName("activationTime")
  private Integer activationTime = null;

  @SerializedName("activityState")
  private String activityState = null;

  @SerializedName("activityUpdateState")
  private String activityUpdateState = null;

  @SerializedName("bindTime")
  private Integer bindTime = null;

  @SerializedName("clientName")
  private String clientName = null;

  @SerializedName("consumerRedeliveryRequestAllowed")
  private Boolean consumerRedeliveryRequestAllowed = null;

  @SerializedName("cutThroughAckedMsgCount")
  private Long cutThroughAckedMsgCount = null;

  @SerializedName("deliveryState")
  private String deliveryState = null;

  @SerializedName("flowId")
  private Long flowId = null;

  @SerializedName("highestAckPendingMsgId")
  private Long highestAckPendingMsgId = null;

  @SerializedName("lastAckedMsgId")
  private Long lastAckedMsgId = null;

  @SerializedName("lastSelectorExaminedMsgId")
  private Long lastSelectorExaminedMsgId = null;

  @SerializedName("lowestAckPendingMsgId")
  private Long lowestAckPendingMsgId = null;

  @SerializedName("maxUnackedMsgsExceededMsgCount")
  private Long maxUnackedMsgsExceededMsgCount = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("noLocalDelivery")
  private Boolean noLocalDelivery = null;

  @SerializedName("queueName")
  private String queueName = null;

  @SerializedName("redeliveredMsgCount")
  private Long redeliveredMsgCount = null;

  @SerializedName("redeliveryRequestCount")
  private Long redeliveryRequestCount = null;

  @SerializedName("selector")
  private String selector = null;

  @SerializedName("selectorExaminedMsgCount")
  private Long selectorExaminedMsgCount = null;

  @SerializedName("selectorMatchedMsgCount")
  private Long selectorMatchedMsgCount = null;

  @SerializedName("selectorNotMatchedMsgCount")
  private Long selectorNotMatchedMsgCount = null;

  @SerializedName("sessionName")
  private String sessionName = null;

  @SerializedName("storeAndForwardAckedMsgCount")
  private Long storeAndForwardAckedMsgCount = null;

  @SerializedName("unackedMsgCount")
  private Long unackedMsgCount = null;

  @SerializedName("usedWindowSize")
  private Integer usedWindowSize = null;

  @SerializedName("windowClosedCount")
  private Long windowClosedCount = null;

  @SerializedName("windowSize")
  private Long windowSize = null;

  public MsgVpnQueueTxFlow ackedMsgCount(Long ackedMsgCount) {
    this.ackedMsgCount = ackedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages delivered and acknowledged by the consumer.
   * @return ackedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages delivered and acknowledged by the consumer.")
  public Long getAckedMsgCount() {
    return ackedMsgCount;
  }

  public void setAckedMsgCount(Long ackedMsgCount) {
    this.ackedMsgCount = ackedMsgCount;
  }

  public MsgVpnQueueTxFlow activationTime(Integer activationTime) {
    this.activationTime = activationTime;
    return this;
  }

   /**
   * The timestamp of when the bound Flow became active. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return activationTime
  **/
  @ApiModelProperty(value = "The timestamp of when the bound Flow became active. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getActivationTime() {
    return activationTime;
  }

  public void setActivationTime(Integer activationTime) {
    this.activationTime = activationTime;
  }

  public MsgVpnQueueTxFlow activityState(String activityState) {
    this.activityState = activityState;
    return this;
  }

   /**
   * The activity state of the Flow. The allowed values and their meaning are:  &lt;pre&gt; \&quot;active-browser\&quot; - The Flow is active as a browser. \&quot;active-consumer\&quot; - The Flow is active as a consumer. \&quot;inactive\&quot; - The Flow is inactive. &lt;/pre&gt; 
   * @return activityState
  **/
  @ApiModelProperty(value = "The activity state of the Flow. The allowed values and their meaning are:  <pre> \"active-browser\" - The Flow is active as a browser. \"active-consumer\" - The Flow is active as a consumer. \"inactive\" - The Flow is inactive. </pre> ")
  public String getActivityState() {
    return activityState;
  }

  public void setActivityState(String activityState) {
    this.activityState = activityState;
  }

  public MsgVpnQueueTxFlow activityUpdateState(String activityUpdateState) {
    this.activityUpdateState = activityUpdateState;
    return this;
  }

   /**
   * The state of updating the consumer with the Flow activity. The allowed values and their meaning are:  &lt;pre&gt; \&quot;in-progress\&quot; - The Flow is in the process of updating the client with its activity state. \&quot;synchronized\&quot; - The Flow has updated the client with its activity state. \&quot;not-requested\&quot; - The Flow has not been requested by the client to provide activity updates. &lt;/pre&gt; 
   * @return activityUpdateState
  **/
  @ApiModelProperty(value = "The state of updating the consumer with the Flow activity. The allowed values and their meaning are:  <pre> \"in-progress\" - The Flow is in the process of updating the client with its activity state. \"synchronized\" - The Flow has updated the client with its activity state. \"not-requested\" - The Flow has not been requested by the client to provide activity updates. </pre> ")
  public String getActivityUpdateState() {
    return activityUpdateState;
  }

  public void setActivityUpdateState(String activityUpdateState) {
    this.activityUpdateState = activityUpdateState;
  }

  public MsgVpnQueueTxFlow bindTime(Integer bindTime) {
    this.bindTime = bindTime;
    return this;
  }

   /**
   * The timestamp of when the Flow bound to the Queue. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return bindTime
  **/
  @ApiModelProperty(value = "The timestamp of when the Flow bound to the Queue. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getBindTime() {
    return bindTime;
  }

  public void setBindTime(Integer bindTime) {
    this.bindTime = bindTime;
  }

  public MsgVpnQueueTxFlow clientName(String clientName) {
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

  public MsgVpnQueueTxFlow consumerRedeliveryRequestAllowed(Boolean consumerRedeliveryRequestAllowed) {
    this.consumerRedeliveryRequestAllowed = consumerRedeliveryRequestAllowed;
    return this;
  }

   /**
   * Indicates whether redelivery requests can be received as negative acknowledgements (NACKs) from the consumer. Applicable only to REST consumers.
   * @return consumerRedeliveryRequestAllowed
  **/
  @ApiModelProperty(value = "Indicates whether redelivery requests can be received as negative acknowledgements (NACKs) from the consumer. Applicable only to REST consumers.")
  public Boolean isConsumerRedeliveryRequestAllowed() {
    return consumerRedeliveryRequestAllowed;
  }

  public void setConsumerRedeliveryRequestAllowed(Boolean consumerRedeliveryRequestAllowed) {
    this.consumerRedeliveryRequestAllowed = consumerRedeliveryRequestAllowed;
  }

  public MsgVpnQueueTxFlow cutThroughAckedMsgCount(Long cutThroughAckedMsgCount) {
    this.cutThroughAckedMsgCount = cutThroughAckedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages that used cut-through delivery and are acknowledged by the consumer.
   * @return cutThroughAckedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages that used cut-through delivery and are acknowledged by the consumer.")
  public Long getCutThroughAckedMsgCount() {
    return cutThroughAckedMsgCount;
  }

  public void setCutThroughAckedMsgCount(Long cutThroughAckedMsgCount) {
    this.cutThroughAckedMsgCount = cutThroughAckedMsgCount;
  }

  public MsgVpnQueueTxFlow deliveryState(String deliveryState) {
    this.deliveryState = deliveryState;
    return this;
  }

   /**
   * The delivery state of the Flow. The allowed values and their meaning are:  &lt;pre&gt; \&quot;closed\&quot; - The Flow is unbound. \&quot;opened\&quot; - The Flow is bound but inactive. \&quot;unbinding\&quot; - The Flow received an unbind request. \&quot;handshaking\&quot; - The Flow is handshaking to become active. \&quot;deliver-cut-through\&quot; - The Flow is streaming messages using direct+guaranteed delivery. \&quot;deliver-from-input-stream\&quot; - The Flow is streaming messages using guaranteed delivery. \&quot;deliver-from-memory\&quot; - The Flow throttled causing message delivery from memory (RAM). \&quot;deliver-from-spool\&quot; - The Flow stalled causing message delivery from spool (ADB or disk). &lt;/pre&gt; 
   * @return deliveryState
  **/
  @ApiModelProperty(value = "The delivery state of the Flow. The allowed values and their meaning are:  <pre> \"closed\" - The Flow is unbound. \"opened\" - The Flow is bound but inactive. \"unbinding\" - The Flow received an unbind request. \"handshaking\" - The Flow is handshaking to become active. \"deliver-cut-through\" - The Flow is streaming messages using direct+guaranteed delivery. \"deliver-from-input-stream\" - The Flow is streaming messages using guaranteed delivery. \"deliver-from-memory\" - The Flow throttled causing message delivery from memory (RAM). \"deliver-from-spool\" - The Flow stalled causing message delivery from spool (ADB or disk). </pre> ")
  public String getDeliveryState() {
    return deliveryState;
  }

  public void setDeliveryState(String deliveryState) {
    this.deliveryState = deliveryState;
  }

  public MsgVpnQueueTxFlow flowId(Long flowId) {
    this.flowId = flowId;
    return this;
  }

   /**
   * The identifier (ID) of the Flow.
   * @return flowId
  **/
  @ApiModelProperty(value = "The identifier (ID) of the Flow.")
  public Long getFlowId() {
    return flowId;
  }

  public void setFlowId(Long flowId) {
    this.flowId = flowId;
  }

  public MsgVpnQueueTxFlow highestAckPendingMsgId(Long highestAckPendingMsgId) {
    this.highestAckPendingMsgId = highestAckPendingMsgId;
    return this;
  }

   /**
   * The highest identifier (ID) of message transmitted and waiting for acknowledgement.
   * @return highestAckPendingMsgId
  **/
  @ApiModelProperty(value = "The highest identifier (ID) of message transmitted and waiting for acknowledgement.")
  public Long getHighestAckPendingMsgId() {
    return highestAckPendingMsgId;
  }

  public void setHighestAckPendingMsgId(Long highestAckPendingMsgId) {
    this.highestAckPendingMsgId = highestAckPendingMsgId;
  }

  public MsgVpnQueueTxFlow lastAckedMsgId(Long lastAckedMsgId) {
    this.lastAckedMsgId = lastAckedMsgId;
    return this;
  }

   /**
   * The identifier (ID) of the last message transmitted and acknowledged by the consumer.
   * @return lastAckedMsgId
  **/
  @ApiModelProperty(value = "The identifier (ID) of the last message transmitted and acknowledged by the consumer.")
  public Long getLastAckedMsgId() {
    return lastAckedMsgId;
  }

  public void setLastAckedMsgId(Long lastAckedMsgId) {
    this.lastAckedMsgId = lastAckedMsgId;
  }

  public MsgVpnQueueTxFlow lastSelectorExaminedMsgId(Long lastSelectorExaminedMsgId) {
    this.lastSelectorExaminedMsgId = lastSelectorExaminedMsgId;
    return this;
  }

   /**
   * The identifier (ID) of the last message examined by the Flow selector.
   * @return lastSelectorExaminedMsgId
  **/
  @ApiModelProperty(value = "The identifier (ID) of the last message examined by the Flow selector.")
  public Long getLastSelectorExaminedMsgId() {
    return lastSelectorExaminedMsgId;
  }

  public void setLastSelectorExaminedMsgId(Long lastSelectorExaminedMsgId) {
    this.lastSelectorExaminedMsgId = lastSelectorExaminedMsgId;
  }

  public MsgVpnQueueTxFlow lowestAckPendingMsgId(Long lowestAckPendingMsgId) {
    this.lowestAckPendingMsgId = lowestAckPendingMsgId;
    return this;
  }

   /**
   * The lowest identifier (ID) of message transmitted and waiting for acknowledgement.
   * @return lowestAckPendingMsgId
  **/
  @ApiModelProperty(value = "The lowest identifier (ID) of message transmitted and waiting for acknowledgement.")
  public Long getLowestAckPendingMsgId() {
    return lowestAckPendingMsgId;
  }

  public void setLowestAckPendingMsgId(Long lowestAckPendingMsgId) {
    this.lowestAckPendingMsgId = lowestAckPendingMsgId;
  }

  public MsgVpnQueueTxFlow maxUnackedMsgsExceededMsgCount(Long maxUnackedMsgsExceededMsgCount) {
    this.maxUnackedMsgsExceededMsgCount = maxUnackedMsgsExceededMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages that exceeded the maximum number of delivered unacknowledged messages.
   * @return maxUnackedMsgsExceededMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages that exceeded the maximum number of delivered unacknowledged messages.")
  public Long getMaxUnackedMsgsExceededMsgCount() {
    return maxUnackedMsgsExceededMsgCount;
  }

  public void setMaxUnackedMsgsExceededMsgCount(Long maxUnackedMsgsExceededMsgCount) {
    this.maxUnackedMsgsExceededMsgCount = maxUnackedMsgsExceededMsgCount;
  }

  public MsgVpnQueueTxFlow msgVpnName(String msgVpnName) {
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

  public MsgVpnQueueTxFlow noLocalDelivery(Boolean noLocalDelivery) {
    this.noLocalDelivery = noLocalDelivery;
    return this;
  }

   /**
   * Indicates whether not to deliver messages to a consumer that published them.
   * @return noLocalDelivery
  **/
  @ApiModelProperty(value = "Indicates whether not to deliver messages to a consumer that published them.")
  public Boolean isNoLocalDelivery() {
    return noLocalDelivery;
  }

  public void setNoLocalDelivery(Boolean noLocalDelivery) {
    this.noLocalDelivery = noLocalDelivery;
  }

  public MsgVpnQueueTxFlow queueName(String queueName) {
    this.queueName = queueName;
    return this;
  }

   /**
   * The name of the Queue.
   * @return queueName
  **/
  @ApiModelProperty(value = "The name of the Queue.")
  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public MsgVpnQueueTxFlow redeliveredMsgCount(Long redeliveredMsgCount) {
    this.redeliveredMsgCount = redeliveredMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages that were redelivered.
   * @return redeliveredMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages that were redelivered.")
  public Long getRedeliveredMsgCount() {
    return redeliveredMsgCount;
  }

  public void setRedeliveredMsgCount(Long redeliveredMsgCount) {
    this.redeliveredMsgCount = redeliveredMsgCount;
  }

  public MsgVpnQueueTxFlow redeliveryRequestCount(Long redeliveryRequestCount) {
    this.redeliveryRequestCount = redeliveryRequestCount;
    return this;
  }

   /**
   * The number of consumer requests via negative acknowledgements (NACKs) to redeliver guaranteed messages.
   * @return redeliveryRequestCount
  **/
  @ApiModelProperty(value = "The number of consumer requests via negative acknowledgements (NACKs) to redeliver guaranteed messages.")
  public Long getRedeliveryRequestCount() {
    return redeliveryRequestCount;
  }

  public void setRedeliveryRequestCount(Long redeliveryRequestCount) {
    this.redeliveryRequestCount = redeliveryRequestCount;
  }

  public MsgVpnQueueTxFlow selector(String selector) {
    this.selector = selector;
    return this;
  }

   /**
   * The value of the Flow selector.
   * @return selector
  **/
  @ApiModelProperty(value = "The value of the Flow selector.")
  public String getSelector() {
    return selector;
  }

  public void setSelector(String selector) {
    this.selector = selector;
  }

  public MsgVpnQueueTxFlow selectorExaminedMsgCount(Long selectorExaminedMsgCount) {
    this.selectorExaminedMsgCount = selectorExaminedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages examined by the Flow selector.
   * @return selectorExaminedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages examined by the Flow selector.")
  public Long getSelectorExaminedMsgCount() {
    return selectorExaminedMsgCount;
  }

  public void setSelectorExaminedMsgCount(Long selectorExaminedMsgCount) {
    this.selectorExaminedMsgCount = selectorExaminedMsgCount;
  }

  public MsgVpnQueueTxFlow selectorMatchedMsgCount(Long selectorMatchedMsgCount) {
    this.selectorMatchedMsgCount = selectorMatchedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages for which the Flow selector matched.
   * @return selectorMatchedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages for which the Flow selector matched.")
  public Long getSelectorMatchedMsgCount() {
    return selectorMatchedMsgCount;
  }

  public void setSelectorMatchedMsgCount(Long selectorMatchedMsgCount) {
    this.selectorMatchedMsgCount = selectorMatchedMsgCount;
  }

  public MsgVpnQueueTxFlow selectorNotMatchedMsgCount(Long selectorNotMatchedMsgCount) {
    this.selectorNotMatchedMsgCount = selectorNotMatchedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages for which the Flow selector did not match.
   * @return selectorNotMatchedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages for which the Flow selector did not match.")
  public Long getSelectorNotMatchedMsgCount() {
    return selectorNotMatchedMsgCount;
  }

  public void setSelectorNotMatchedMsgCount(Long selectorNotMatchedMsgCount) {
    this.selectorNotMatchedMsgCount = selectorNotMatchedMsgCount;
  }

  public MsgVpnQueueTxFlow sessionName(String sessionName) {
    this.sessionName = sessionName;
    return this;
  }

   /**
   * The name of the Transacted Session for the Flow.
   * @return sessionName
  **/
  @ApiModelProperty(value = "The name of the Transacted Session for the Flow.")
  public String getSessionName() {
    return sessionName;
  }

  public void setSessionName(String sessionName) {
    this.sessionName = sessionName;
  }

  public MsgVpnQueueTxFlow storeAndForwardAckedMsgCount(Long storeAndForwardAckedMsgCount) {
    this.storeAndForwardAckedMsgCount = storeAndForwardAckedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages that used store and forward delivery and are acknowledged by the consumer.
   * @return storeAndForwardAckedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages that used store and forward delivery and are acknowledged by the consumer.")
  public Long getStoreAndForwardAckedMsgCount() {
    return storeAndForwardAckedMsgCount;
  }

  public void setStoreAndForwardAckedMsgCount(Long storeAndForwardAckedMsgCount) {
    this.storeAndForwardAckedMsgCount = storeAndForwardAckedMsgCount;
  }

  public MsgVpnQueueTxFlow unackedMsgCount(Long unackedMsgCount) {
    this.unackedMsgCount = unackedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages delivered but not yet acknowledged by the consumer.
   * @return unackedMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages delivered but not yet acknowledged by the consumer.")
  public Long getUnackedMsgCount() {
    return unackedMsgCount;
  }

  public void setUnackedMsgCount(Long unackedMsgCount) {
    this.unackedMsgCount = unackedMsgCount;
  }

  public MsgVpnQueueTxFlow usedWindowSize(Integer usedWindowSize) {
    this.usedWindowSize = usedWindowSize;
    return this;
  }

   /**
   * The number of guaranteed messages using the available window size.
   * @return usedWindowSize
  **/
  @ApiModelProperty(value = "The number of guaranteed messages using the available window size.")
  public Integer getUsedWindowSize() {
    return usedWindowSize;
  }

  public void setUsedWindowSize(Integer usedWindowSize) {
    this.usedWindowSize = usedWindowSize;
  }

  public MsgVpnQueueTxFlow windowClosedCount(Long windowClosedCount) {
    this.windowClosedCount = windowClosedCount;
    return this;
  }

   /**
   * The number of times the window for guaranteed messages was filled and closed before an acknowledgement was received.
   * @return windowClosedCount
  **/
  @ApiModelProperty(value = "The number of times the window for guaranteed messages was filled and closed before an acknowledgement was received.")
  public Long getWindowClosedCount() {
    return windowClosedCount;
  }

  public void setWindowClosedCount(Long windowClosedCount) {
    this.windowClosedCount = windowClosedCount;
  }

  public MsgVpnQueueTxFlow windowSize(Long windowSize) {
    this.windowSize = windowSize;
    return this;
  }

   /**
   * The number of outstanding guaranteed messages that can be transmitted over the Flow before an acknowledgement is received.
   * @return windowSize
  **/
  @ApiModelProperty(value = "The number of outstanding guaranteed messages that can be transmitted over the Flow before an acknowledgement is received.")
  public Long getWindowSize() {
    return windowSize;
  }

  public void setWindowSize(Long windowSize) {
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
    MsgVpnQueueTxFlow msgVpnQueueTxFlow = (MsgVpnQueueTxFlow) o;
    return Objects.equals(this.ackedMsgCount, msgVpnQueueTxFlow.ackedMsgCount) &&
        Objects.equals(this.activationTime, msgVpnQueueTxFlow.activationTime) &&
        Objects.equals(this.activityState, msgVpnQueueTxFlow.activityState) &&
        Objects.equals(this.activityUpdateState, msgVpnQueueTxFlow.activityUpdateState) &&
        Objects.equals(this.bindTime, msgVpnQueueTxFlow.bindTime) &&
        Objects.equals(this.clientName, msgVpnQueueTxFlow.clientName) &&
        Objects.equals(this.consumerRedeliveryRequestAllowed, msgVpnQueueTxFlow.consumerRedeliveryRequestAllowed) &&
        Objects.equals(this.cutThroughAckedMsgCount, msgVpnQueueTxFlow.cutThroughAckedMsgCount) &&
        Objects.equals(this.deliveryState, msgVpnQueueTxFlow.deliveryState) &&
        Objects.equals(this.flowId, msgVpnQueueTxFlow.flowId) &&
        Objects.equals(this.highestAckPendingMsgId, msgVpnQueueTxFlow.highestAckPendingMsgId) &&
        Objects.equals(this.lastAckedMsgId, msgVpnQueueTxFlow.lastAckedMsgId) &&
        Objects.equals(this.lastSelectorExaminedMsgId, msgVpnQueueTxFlow.lastSelectorExaminedMsgId) &&
        Objects.equals(this.lowestAckPendingMsgId, msgVpnQueueTxFlow.lowestAckPendingMsgId) &&
        Objects.equals(this.maxUnackedMsgsExceededMsgCount, msgVpnQueueTxFlow.maxUnackedMsgsExceededMsgCount) &&
        Objects.equals(this.msgVpnName, msgVpnQueueTxFlow.msgVpnName) &&
        Objects.equals(this.noLocalDelivery, msgVpnQueueTxFlow.noLocalDelivery) &&
        Objects.equals(this.queueName, msgVpnQueueTxFlow.queueName) &&
        Objects.equals(this.redeliveredMsgCount, msgVpnQueueTxFlow.redeliveredMsgCount) &&
        Objects.equals(this.redeliveryRequestCount, msgVpnQueueTxFlow.redeliveryRequestCount) &&
        Objects.equals(this.selector, msgVpnQueueTxFlow.selector) &&
        Objects.equals(this.selectorExaminedMsgCount, msgVpnQueueTxFlow.selectorExaminedMsgCount) &&
        Objects.equals(this.selectorMatchedMsgCount, msgVpnQueueTxFlow.selectorMatchedMsgCount) &&
        Objects.equals(this.selectorNotMatchedMsgCount, msgVpnQueueTxFlow.selectorNotMatchedMsgCount) &&
        Objects.equals(this.sessionName, msgVpnQueueTxFlow.sessionName) &&
        Objects.equals(this.storeAndForwardAckedMsgCount, msgVpnQueueTxFlow.storeAndForwardAckedMsgCount) &&
        Objects.equals(this.unackedMsgCount, msgVpnQueueTxFlow.unackedMsgCount) &&
        Objects.equals(this.usedWindowSize, msgVpnQueueTxFlow.usedWindowSize) &&
        Objects.equals(this.windowClosedCount, msgVpnQueueTxFlow.windowClosedCount) &&
        Objects.equals(this.windowSize, msgVpnQueueTxFlow.windowSize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ackedMsgCount, activationTime, activityState, activityUpdateState, bindTime, clientName, consumerRedeliveryRequestAllowed, cutThroughAckedMsgCount, deliveryState, flowId, highestAckPendingMsgId, lastAckedMsgId, lastSelectorExaminedMsgId, lowestAckPendingMsgId, maxUnackedMsgsExceededMsgCount, msgVpnName, noLocalDelivery, queueName, redeliveredMsgCount, redeliveryRequestCount, selector, selectorExaminedMsgCount, selectorMatchedMsgCount, selectorNotMatchedMsgCount, sessionName, storeAndForwardAckedMsgCount, unackedMsgCount, usedWindowSize, windowClosedCount, windowSize);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnQueueTxFlow {\n");
    
    sb.append("    ackedMsgCount: ").append(toIndentedString(ackedMsgCount)).append("\n");
    sb.append("    activationTime: ").append(toIndentedString(activationTime)).append("\n");
    sb.append("    activityState: ").append(toIndentedString(activityState)).append("\n");
    sb.append("    activityUpdateState: ").append(toIndentedString(activityUpdateState)).append("\n");
    sb.append("    bindTime: ").append(toIndentedString(bindTime)).append("\n");
    sb.append("    clientName: ").append(toIndentedString(clientName)).append("\n");
    sb.append("    consumerRedeliveryRequestAllowed: ").append(toIndentedString(consumerRedeliveryRequestAllowed)).append("\n");
    sb.append("    cutThroughAckedMsgCount: ").append(toIndentedString(cutThroughAckedMsgCount)).append("\n");
    sb.append("    deliveryState: ").append(toIndentedString(deliveryState)).append("\n");
    sb.append("    flowId: ").append(toIndentedString(flowId)).append("\n");
    sb.append("    highestAckPendingMsgId: ").append(toIndentedString(highestAckPendingMsgId)).append("\n");
    sb.append("    lastAckedMsgId: ").append(toIndentedString(lastAckedMsgId)).append("\n");
    sb.append("    lastSelectorExaminedMsgId: ").append(toIndentedString(lastSelectorExaminedMsgId)).append("\n");
    sb.append("    lowestAckPendingMsgId: ").append(toIndentedString(lowestAckPendingMsgId)).append("\n");
    sb.append("    maxUnackedMsgsExceededMsgCount: ").append(toIndentedString(maxUnackedMsgsExceededMsgCount)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    noLocalDelivery: ").append(toIndentedString(noLocalDelivery)).append("\n");
    sb.append("    queueName: ").append(toIndentedString(queueName)).append("\n");
    sb.append("    redeliveredMsgCount: ").append(toIndentedString(redeliveredMsgCount)).append("\n");
    sb.append("    redeliveryRequestCount: ").append(toIndentedString(redeliveryRequestCount)).append("\n");
    sb.append("    selector: ").append(toIndentedString(selector)).append("\n");
    sb.append("    selectorExaminedMsgCount: ").append(toIndentedString(selectorExaminedMsgCount)).append("\n");
    sb.append("    selectorMatchedMsgCount: ").append(toIndentedString(selectorMatchedMsgCount)).append("\n");
    sb.append("    selectorNotMatchedMsgCount: ").append(toIndentedString(selectorNotMatchedMsgCount)).append("\n");
    sb.append("    sessionName: ").append(toIndentedString(sessionName)).append("\n");
    sb.append("    storeAndForwardAckedMsgCount: ").append(toIndentedString(storeAndForwardAckedMsgCount)).append("\n");
    sb.append("    unackedMsgCount: ").append(toIndentedString(unackedMsgCount)).append("\n");
    sb.append("    usedWindowSize: ").append(toIndentedString(usedWindowSize)).append("\n");
    sb.append("    windowClosedCount: ").append(toIndentedString(windowClosedCount)).append("\n");
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

