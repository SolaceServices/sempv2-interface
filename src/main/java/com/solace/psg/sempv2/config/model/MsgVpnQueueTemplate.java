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


package com.solace.psg.sempv2.config.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.solace.psg.sempv2.config.model.EventThreshold;
import java.io.IOException;

/**
 * MsgVpnQueueTemplate
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-12T16:43:32.646Z")
public class MsgVpnQueueTemplate {
  /**
   * The access type for delivering messages to consumer flows. The default value is &#x60;\&quot;exclusive\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;exclusive\&quot; - Exclusive delivery of messages to the first bound consumer flow. \&quot;non-exclusive\&quot; - Non-exclusive delivery of messages to all bound consumer flows in a round-robin fashion. &lt;/pre&gt; 
   */
  @JsonAdapter(AccessTypeEnum.Adapter.class)
  public enum AccessTypeEnum {
    EXCLUSIVE("exclusive"),
    
    NON_EXCLUSIVE("non-exclusive");

    private String value;

    AccessTypeEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static AccessTypeEnum fromValue(String text) {
      for (AccessTypeEnum b : AccessTypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<AccessTypeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final AccessTypeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public AccessTypeEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return AccessTypeEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("accessType")
  private AccessTypeEnum accessType = null;

  @SerializedName("consumerAckPropagationEnabled")
  private Boolean consumerAckPropagationEnabled = null;

  @SerializedName("deadMsgQueue")
  private String deadMsgQueue = null;

  /**
   * Controls the durability of queues created from this template. If non-durable, the created queue will be non-durable, regardless of the specified durability. If none, the created queue will have the requested durability. The default value is &#x60;\&quot;none\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;none\&quot; - The durability of the endpoint will be as requested on create. \&quot;non-durable\&quot; - The durability of the created queue will be non-durable, regardless of what was requested. &lt;/pre&gt; 
   */
  @JsonAdapter(DurabilityOverrideEnum.Adapter.class)
  public enum DurabilityOverrideEnum {
    NONE("none"),
    
    NON_DURABLE("non-durable");

    private String value;

    DurabilityOverrideEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static DurabilityOverrideEnum fromValue(String text) {
      for (DurabilityOverrideEnum b : DurabilityOverrideEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<DurabilityOverrideEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final DurabilityOverrideEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public DurabilityOverrideEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return DurabilityOverrideEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("durabilityOverride")
  private DurabilityOverrideEnum durabilityOverride = null;

  @SerializedName("eventBindCountThreshold")
  private EventThreshold eventBindCountThreshold = null;

  @SerializedName("eventMsgSpoolUsageThreshold")
  private EventThreshold eventMsgSpoolUsageThreshold = null;

  @SerializedName("eventRejectLowPriorityMsgLimitThreshold")
  private EventThreshold eventRejectLowPriorityMsgLimitThreshold = null;

  @SerializedName("maxBindCount")
  private Long maxBindCount = null;

  @SerializedName("maxDeliveredUnackedMsgsPerFlow")
  private Long maxDeliveredUnackedMsgsPerFlow = null;

  @SerializedName("maxMsgSize")
  private Integer maxMsgSize = null;

  @SerializedName("maxMsgSpoolUsage")
  private Long maxMsgSpoolUsage = null;

  @SerializedName("maxRedeliveryCount")
  private Long maxRedeliveryCount = null;

  @SerializedName("maxTtl")
  private Long maxTtl = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  /**
   * The permission level for all consumers, excluding the owner. The default value is &#x60;\&quot;no-access\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;no-access\&quot; - Disallows all access. \&quot;read-only\&quot; - Read-only access to the messages. \&quot;consume\&quot; - Consume (read and remove) messages. \&quot;modify-topic\&quot; - Consume messages or modify the topic/selector. \&quot;delete\&quot; - Consume messages, modify the topic/selector or delete the Client created endpoint altogether. &lt;/pre&gt; 
   */
  @JsonAdapter(PermissionEnum.Adapter.class)
  public enum PermissionEnum {
    NO_ACCESS("no-access"),
    
    READ_ONLY("read-only"),
    
    CONSUME("consume"),
    
    MODIFY_TOPIC("modify-topic"),
    
    DELETE("delete");

    private String value;

    PermissionEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static PermissionEnum fromValue(String text) {
      for (PermissionEnum b : PermissionEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<PermissionEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final PermissionEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public PermissionEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return PermissionEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("permission")
  private PermissionEnum permission = null;

  @SerializedName("queueNameFilter")
  private String queueNameFilter = null;

  @SerializedName("queueTemplateName")
  private String queueTemplateName = null;

  @SerializedName("rejectLowPriorityMsgEnabled")
  private Boolean rejectLowPriorityMsgEnabled = null;

  @SerializedName("rejectLowPriorityMsgLimit")
  private Long rejectLowPriorityMsgLimit = null;

  /**
   * Determines when to return negative acknowledgements (NACKs) to sending clients on message discards. Note that NACKs prevent the message from being delivered to any destination and Transacted Session commits to fail. The default value is &#x60;\&quot;when-queue-enabled\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;always\&quot; - Always return a negative acknowledgment (NACK) to the sending client on message discard. \&quot;when-queue-enabled\&quot; - Only return a negative acknowledgment (NACK) to the sending client on message discard when the Queue is enabled. \&quot;never\&quot; - Never return a negative acknowledgment (NACK) to the sending client on message discard. &lt;/pre&gt; 
   */
  @JsonAdapter(RejectMsgToSenderOnDiscardBehaviorEnum.Adapter.class)
  public enum RejectMsgToSenderOnDiscardBehaviorEnum {
    ALWAYS("always"),
    
    WHEN_QUEUE_ENABLED("when-queue-enabled"),
    
    NEVER("never");

    private String value;

    RejectMsgToSenderOnDiscardBehaviorEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static RejectMsgToSenderOnDiscardBehaviorEnum fromValue(String text) {
      for (RejectMsgToSenderOnDiscardBehaviorEnum b : RejectMsgToSenderOnDiscardBehaviorEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<RejectMsgToSenderOnDiscardBehaviorEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final RejectMsgToSenderOnDiscardBehaviorEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public RejectMsgToSenderOnDiscardBehaviorEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return RejectMsgToSenderOnDiscardBehaviorEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("rejectMsgToSenderOnDiscardBehavior")
  private RejectMsgToSenderOnDiscardBehaviorEnum rejectMsgToSenderOnDiscardBehavior = null;

  @SerializedName("respectMsgPriorityEnabled")
  private Boolean respectMsgPriorityEnabled = null;

  @SerializedName("respectTtlEnabled")
  private Boolean respectTtlEnabled = null;

  public MsgVpnQueueTemplate accessType(AccessTypeEnum accessType) {
    this.accessType = accessType;
    return this;
  }

   /**
   * The access type for delivering messages to consumer flows. The default value is &#x60;\&quot;exclusive\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;exclusive\&quot; - Exclusive delivery of messages to the first bound consumer flow. \&quot;non-exclusive\&quot; - Non-exclusive delivery of messages to all bound consumer flows in a round-robin fashion. &lt;/pre&gt; 
   * @return accessType
  **/
  @ApiModelProperty(value = "The access type for delivering messages to consumer flows. The default value is `\"exclusive\"`. The allowed values and their meaning are:  <pre> \"exclusive\" - Exclusive delivery of messages to the first bound consumer flow. \"non-exclusive\" - Non-exclusive delivery of messages to all bound consumer flows in a round-robin fashion. </pre> ")
  public AccessTypeEnum getAccessType() {
    return accessType;
  }

  public void setAccessType(AccessTypeEnum accessType) {
    this.accessType = accessType;
  }

  public MsgVpnQueueTemplate consumerAckPropagationEnabled(Boolean consumerAckPropagationEnabled) {
    this.consumerAckPropagationEnabled = consumerAckPropagationEnabled;
    return this;
  }

   /**
   * Enable or disable the propagation of consumer acknowledgements (ACKs) received on the active replication Message VPN to the standby replication Message VPN. The default value is &#x60;true&#x60;.
   * @return consumerAckPropagationEnabled
  **/
  @ApiModelProperty(value = "Enable or disable the propagation of consumer acknowledgements (ACKs) received on the active replication Message VPN to the standby replication Message VPN. The default value is `true`.")
  public Boolean isConsumerAckPropagationEnabled() {
    return consumerAckPropagationEnabled;
  }

  public void setConsumerAckPropagationEnabled(Boolean consumerAckPropagationEnabled) {
    this.consumerAckPropagationEnabled = consumerAckPropagationEnabled;
  }

  public MsgVpnQueueTemplate deadMsgQueue(String deadMsgQueue) {
    this.deadMsgQueue = deadMsgQueue;
    return this;
  }

   /**
   * The name of the Dead Message Queue (DMQ). The default value is &#x60;\&quot;#DEAD_MSG_QUEUE\&quot;&#x60;.
   * @return deadMsgQueue
  **/
  @ApiModelProperty(value = "The name of the Dead Message Queue (DMQ). The default value is `\"#DEAD_MSG_QUEUE\"`.")
  public String getDeadMsgQueue() {
    return deadMsgQueue;
  }

  public void setDeadMsgQueue(String deadMsgQueue) {
    this.deadMsgQueue = deadMsgQueue;
  }

  public MsgVpnQueueTemplate durabilityOverride(DurabilityOverrideEnum durabilityOverride) {
    this.durabilityOverride = durabilityOverride;
    return this;
  }

   /**
   * Controls the durability of queues created from this template. If non-durable, the created queue will be non-durable, regardless of the specified durability. If none, the created queue will have the requested durability. The default value is &#x60;\&quot;none\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;none\&quot; - The durability of the endpoint will be as requested on create. \&quot;non-durable\&quot; - The durability of the created queue will be non-durable, regardless of what was requested. &lt;/pre&gt; 
   * @return durabilityOverride
  **/
  @ApiModelProperty(value = "Controls the durability of queues created from this template. If non-durable, the created queue will be non-durable, regardless of the specified durability. If none, the created queue will have the requested durability. The default value is `\"none\"`. The allowed values and their meaning are:  <pre> \"none\" - The durability of the endpoint will be as requested on create. \"non-durable\" - The durability of the created queue will be non-durable, regardless of what was requested. </pre> ")
  public DurabilityOverrideEnum getDurabilityOverride() {
    return durabilityOverride;
  }

  public void setDurabilityOverride(DurabilityOverrideEnum durabilityOverride) {
    this.durabilityOverride = durabilityOverride;
  }

  public MsgVpnQueueTemplate eventBindCountThreshold(EventThreshold eventBindCountThreshold) {
    this.eventBindCountThreshold = eventBindCountThreshold;
    return this;
  }

   /**
   * Get eventBindCountThreshold
   * @return eventBindCountThreshold
  **/
  @ApiModelProperty(value = "")
  public EventThreshold getEventBindCountThreshold() {
    return eventBindCountThreshold;
  }

  public void setEventBindCountThreshold(EventThreshold eventBindCountThreshold) {
    this.eventBindCountThreshold = eventBindCountThreshold;
  }

  public MsgVpnQueueTemplate eventMsgSpoolUsageThreshold(EventThreshold eventMsgSpoolUsageThreshold) {
    this.eventMsgSpoolUsageThreshold = eventMsgSpoolUsageThreshold;
    return this;
  }

   /**
   * Get eventMsgSpoolUsageThreshold
   * @return eventMsgSpoolUsageThreshold
  **/
  @ApiModelProperty(value = "")
  public EventThreshold getEventMsgSpoolUsageThreshold() {
    return eventMsgSpoolUsageThreshold;
  }

  public void setEventMsgSpoolUsageThreshold(EventThreshold eventMsgSpoolUsageThreshold) {
    this.eventMsgSpoolUsageThreshold = eventMsgSpoolUsageThreshold;
  }

  public MsgVpnQueueTemplate eventRejectLowPriorityMsgLimitThreshold(EventThreshold eventRejectLowPriorityMsgLimitThreshold) {
    this.eventRejectLowPriorityMsgLimitThreshold = eventRejectLowPriorityMsgLimitThreshold;
    return this;
  }

   /**
   * Get eventRejectLowPriorityMsgLimitThreshold
   * @return eventRejectLowPriorityMsgLimitThreshold
  **/
  @ApiModelProperty(value = "")
  public EventThreshold getEventRejectLowPriorityMsgLimitThreshold() {
    return eventRejectLowPriorityMsgLimitThreshold;
  }

  public void setEventRejectLowPriorityMsgLimitThreshold(EventThreshold eventRejectLowPriorityMsgLimitThreshold) {
    this.eventRejectLowPriorityMsgLimitThreshold = eventRejectLowPriorityMsgLimitThreshold;
  }

  public MsgVpnQueueTemplate maxBindCount(Long maxBindCount) {
    this.maxBindCount = maxBindCount;
    return this;
  }

   /**
   * The maximum number of consumer flows that can bind. The default value is &#x60;1000&#x60;.
   * @return maxBindCount
  **/
  @ApiModelProperty(value = "The maximum number of consumer flows that can bind. The default value is `1000`.")
  public Long getMaxBindCount() {
    return maxBindCount;
  }

  public void setMaxBindCount(Long maxBindCount) {
    this.maxBindCount = maxBindCount;
  }

  public MsgVpnQueueTemplate maxDeliveredUnackedMsgsPerFlow(Long maxDeliveredUnackedMsgsPerFlow) {
    this.maxDeliveredUnackedMsgsPerFlow = maxDeliveredUnackedMsgsPerFlow;
    return this;
  }

   /**
   * The maximum number of messages delivered but not acknowledged per flow. The default is the maximum value supported by the platform.
   * @return maxDeliveredUnackedMsgsPerFlow
  **/
  @ApiModelProperty(value = "The maximum number of messages delivered but not acknowledged per flow. The default is the maximum value supported by the platform.")
  public Long getMaxDeliveredUnackedMsgsPerFlow() {
    return maxDeliveredUnackedMsgsPerFlow;
  }

  public void setMaxDeliveredUnackedMsgsPerFlow(Long maxDeliveredUnackedMsgsPerFlow) {
    this.maxDeliveredUnackedMsgsPerFlow = maxDeliveredUnackedMsgsPerFlow;
  }

  public MsgVpnQueueTemplate maxMsgSize(Integer maxMsgSize) {
    this.maxMsgSize = maxMsgSize;
    return this;
  }

   /**
   * The maximum message size allowed, in bytes (B). The default value is &#x60;10000000&#x60;.
   * @return maxMsgSize
  **/
  @ApiModelProperty(value = "The maximum message size allowed, in bytes (B). The default value is `10000000`.")
  public Integer getMaxMsgSize() {
    return maxMsgSize;
  }

  public void setMaxMsgSize(Integer maxMsgSize) {
    this.maxMsgSize = maxMsgSize;
  }

  public MsgVpnQueueTemplate maxMsgSpoolUsage(Long maxMsgSpoolUsage) {
    this.maxMsgSpoolUsage = maxMsgSpoolUsage;
    return this;
  }

   /**
   * The maximum message spool usage allowed, in megabytes (MB). A value of 0 only allows spooling of the last message received and disables quota checking. The default varies by platform.
   * @return maxMsgSpoolUsage
  **/
  @ApiModelProperty(value = "The maximum message spool usage allowed, in megabytes (MB). A value of 0 only allows spooling of the last message received and disables quota checking. The default varies by platform.")
  public Long getMaxMsgSpoolUsage() {
    return maxMsgSpoolUsage;
  }

  public void setMaxMsgSpoolUsage(Long maxMsgSpoolUsage) {
    this.maxMsgSpoolUsage = maxMsgSpoolUsage;
  }

  public MsgVpnQueueTemplate maxRedeliveryCount(Long maxRedeliveryCount) {
    this.maxRedeliveryCount = maxRedeliveryCount;
    return this;
  }

   /**
   * The maximum number of message redelivery attempts that will occur prior to the message being discarded or moved to the DMQ. A value of 0 means to retry forever. The default value is &#x60;0&#x60;.
   * @return maxRedeliveryCount
  **/
  @ApiModelProperty(value = "The maximum number of message redelivery attempts that will occur prior to the message being discarded or moved to the DMQ. A value of 0 means to retry forever. The default value is `0`.")
  public Long getMaxRedeliveryCount() {
    return maxRedeliveryCount;
  }

  public void setMaxRedeliveryCount(Long maxRedeliveryCount) {
    this.maxRedeliveryCount = maxRedeliveryCount;
  }

  public MsgVpnQueueTemplate maxTtl(Long maxTtl) {
    this.maxTtl = maxTtl;
    return this;
  }

   /**
   * The maximum time in seconds a message can stay in a Queue when &#x60;respectTtlEnabled&#x60; is &#x60;\&quot;true\&quot;&#x60;. A message expires when the lesser of the sender assigned time-to-live (TTL) in the message and the &#x60;maxTtl&#x60; configured for the Queue, is exceeded. A value of 0 disables expiry. The default value is &#x60;0&#x60;.
   * @return maxTtl
  **/
  @ApiModelProperty(value = "The maximum time in seconds a message can stay in a Queue when `respectTtlEnabled` is `\"true\"`. A message expires when the lesser of the sender assigned time-to-live (TTL) in the message and the `maxTtl` configured for the Queue, is exceeded. A value of 0 disables expiry. The default value is `0`.")
  public Long getMaxTtl() {
    return maxTtl;
  }

  public void setMaxTtl(Long maxTtl) {
    this.maxTtl = maxTtl;
  }

  public MsgVpnQueueTemplate msgVpnName(String msgVpnName) {
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

  public MsgVpnQueueTemplate permission(PermissionEnum permission) {
    this.permission = permission;
    return this;
  }

   /**
   * The permission level for all consumers, excluding the owner. The default value is &#x60;\&quot;no-access\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;no-access\&quot; - Disallows all access. \&quot;read-only\&quot; - Read-only access to the messages. \&quot;consume\&quot; - Consume (read and remove) messages. \&quot;modify-topic\&quot; - Consume messages or modify the topic/selector. \&quot;delete\&quot; - Consume messages, modify the topic/selector or delete the Client created endpoint altogether. &lt;/pre&gt; 
   * @return permission
  **/
  @ApiModelProperty(value = "The permission level for all consumers, excluding the owner. The default value is `\"no-access\"`. The allowed values and their meaning are:  <pre> \"no-access\" - Disallows all access. \"read-only\" - Read-only access to the messages. \"consume\" - Consume (read and remove) messages. \"modify-topic\" - Consume messages or modify the topic/selector. \"delete\" - Consume messages, modify the topic/selector or delete the Client created endpoint altogether. </pre> ")
  public PermissionEnum getPermission() {
    return permission;
  }

  public void setPermission(PermissionEnum permission) {
    this.permission = permission;
  }

  public MsgVpnQueueTemplate queueNameFilter(String queueNameFilter) {
    this.queueNameFilter = queueNameFilter;
    return this;
  }

   /**
   * A wildcardable pattern used to determine which Queues use settings from this Template. Two different wildcards are supported: * and &gt;. Similar to topic filters or subscription patterns, a &gt; matches anything (but only when used at the end), and a * matches zero or more characters but never a slash (/). A &gt; is only a wildcard when used at the end, after a /. A * is only allowed at the end, after a slash (/). The default value is &#x60;\&quot;\&quot;&#x60;.
   * @return queueNameFilter
  **/
  @ApiModelProperty(value = "A wildcardable pattern used to determine which Queues use settings from this Template. Two different wildcards are supported: * and >. Similar to topic filters or subscription patterns, a > matches anything (but only when used at the end), and a * matches zero or more characters but never a slash (/). A > is only a wildcard when used at the end, after a /. A * is only allowed at the end, after a slash (/). The default value is `\"\"`.")
  public String getQueueNameFilter() {
    return queueNameFilter;
  }

  public void setQueueNameFilter(String queueNameFilter) {
    this.queueNameFilter = queueNameFilter;
  }

  public MsgVpnQueueTemplate queueTemplateName(String queueTemplateName) {
    this.queueTemplateName = queueTemplateName;
    return this;
  }

   /**
   * The name of the Queue Template.
   * @return queueTemplateName
  **/
  @ApiModelProperty(value = "The name of the Queue Template.")
  public String getQueueTemplateName() {
    return queueTemplateName;
  }

  public void setQueueTemplateName(String queueTemplateName) {
    this.queueTemplateName = queueTemplateName;
  }

  public MsgVpnQueueTemplate rejectLowPriorityMsgEnabled(Boolean rejectLowPriorityMsgEnabled) {
    this.rejectLowPriorityMsgEnabled = rejectLowPriorityMsgEnabled;
    return this;
  }

   /**
   * Enable or disable the checking of low priority messages against the &#x60;rejectLowPriorityMsgLimit&#x60;. This may only be enabled if &#x60;rejectMsgToSenderOnDiscardBehavior&#x60; does not have a value of &#x60;\&quot;never\&quot;&#x60;. The default value is &#x60;false&#x60;.
   * @return rejectLowPriorityMsgEnabled
  **/
  @ApiModelProperty(value = "Enable or disable the checking of low priority messages against the `rejectLowPriorityMsgLimit`. This may only be enabled if `rejectMsgToSenderOnDiscardBehavior` does not have a value of `\"never\"`. The default value is `false`.")
  public Boolean isRejectLowPriorityMsgEnabled() {
    return rejectLowPriorityMsgEnabled;
  }

  public void setRejectLowPriorityMsgEnabled(Boolean rejectLowPriorityMsgEnabled) {
    this.rejectLowPriorityMsgEnabled = rejectLowPriorityMsgEnabled;
  }

  public MsgVpnQueueTemplate rejectLowPriorityMsgLimit(Long rejectLowPriorityMsgLimit) {
    this.rejectLowPriorityMsgLimit = rejectLowPriorityMsgLimit;
    return this;
  }

   /**
   * The number of messages of any priority above which low priority messages are not admitted but higher priority messages are allowed. The default value is &#x60;0&#x60;.
   * @return rejectLowPriorityMsgLimit
  **/
  @ApiModelProperty(value = "The number of messages of any priority above which low priority messages are not admitted but higher priority messages are allowed. The default value is `0`.")
  public Long getRejectLowPriorityMsgLimit() {
    return rejectLowPriorityMsgLimit;
  }

  public void setRejectLowPriorityMsgLimit(Long rejectLowPriorityMsgLimit) {
    this.rejectLowPriorityMsgLimit = rejectLowPriorityMsgLimit;
  }

  public MsgVpnQueueTemplate rejectMsgToSenderOnDiscardBehavior(RejectMsgToSenderOnDiscardBehaviorEnum rejectMsgToSenderOnDiscardBehavior) {
    this.rejectMsgToSenderOnDiscardBehavior = rejectMsgToSenderOnDiscardBehavior;
    return this;
  }

   /**
   * Determines when to return negative acknowledgements (NACKs) to sending clients on message discards. Note that NACKs prevent the message from being delivered to any destination and Transacted Session commits to fail. The default value is &#x60;\&quot;when-queue-enabled\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;always\&quot; - Always return a negative acknowledgment (NACK) to the sending client on message discard. \&quot;when-queue-enabled\&quot; - Only return a negative acknowledgment (NACK) to the sending client on message discard when the Queue is enabled. \&quot;never\&quot; - Never return a negative acknowledgment (NACK) to the sending client on message discard. &lt;/pre&gt; 
   * @return rejectMsgToSenderOnDiscardBehavior
  **/
  @ApiModelProperty(value = "Determines when to return negative acknowledgements (NACKs) to sending clients on message discards. Note that NACKs prevent the message from being delivered to any destination and Transacted Session commits to fail. The default value is `\"when-queue-enabled\"`. The allowed values and their meaning are:  <pre> \"always\" - Always return a negative acknowledgment (NACK) to the sending client on message discard. \"when-queue-enabled\" - Only return a negative acknowledgment (NACK) to the sending client on message discard when the Queue is enabled. \"never\" - Never return a negative acknowledgment (NACK) to the sending client on message discard. </pre> ")
  public RejectMsgToSenderOnDiscardBehaviorEnum getRejectMsgToSenderOnDiscardBehavior() {
    return rejectMsgToSenderOnDiscardBehavior;
  }

  public void setRejectMsgToSenderOnDiscardBehavior(RejectMsgToSenderOnDiscardBehaviorEnum rejectMsgToSenderOnDiscardBehavior) {
    this.rejectMsgToSenderOnDiscardBehavior = rejectMsgToSenderOnDiscardBehavior;
  }

  public MsgVpnQueueTemplate respectMsgPriorityEnabled(Boolean respectMsgPriorityEnabled) {
    this.respectMsgPriorityEnabled = respectMsgPriorityEnabled;
    return this;
  }

   /**
   * Enable or disable the respecting of message priority. When enabled, messages are delivered in priority order, from 9 (highest) to 0 (lowest). The default value is &#x60;false&#x60;.
   * @return respectMsgPriorityEnabled
  **/
  @ApiModelProperty(value = "Enable or disable the respecting of message priority. When enabled, messages are delivered in priority order, from 9 (highest) to 0 (lowest). The default value is `false`.")
  public Boolean isRespectMsgPriorityEnabled() {
    return respectMsgPriorityEnabled;
  }

  public void setRespectMsgPriorityEnabled(Boolean respectMsgPriorityEnabled) {
    this.respectMsgPriorityEnabled = respectMsgPriorityEnabled;
  }

  public MsgVpnQueueTemplate respectTtlEnabled(Boolean respectTtlEnabled) {
    this.respectTtlEnabled = respectTtlEnabled;
    return this;
  }

   /**
   * Enable or disable the respecting of the time-to-live (TTL) for messages. When enabled, expired messages are discarded or moved to the DMQ. The default value is &#x60;false&#x60;.
   * @return respectTtlEnabled
  **/
  @ApiModelProperty(value = "Enable or disable the respecting of the time-to-live (TTL) for messages. When enabled, expired messages are discarded or moved to the DMQ. The default value is `false`.")
  public Boolean isRespectTtlEnabled() {
    return respectTtlEnabled;
  }

  public void setRespectTtlEnabled(Boolean respectTtlEnabled) {
    this.respectTtlEnabled = respectTtlEnabled;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnQueueTemplate msgVpnQueueTemplate = (MsgVpnQueueTemplate) o;
    return Objects.equals(this.accessType, msgVpnQueueTemplate.accessType) &&
        Objects.equals(this.consumerAckPropagationEnabled, msgVpnQueueTemplate.consumerAckPropagationEnabled) &&
        Objects.equals(this.deadMsgQueue, msgVpnQueueTemplate.deadMsgQueue) &&
        Objects.equals(this.durabilityOverride, msgVpnQueueTemplate.durabilityOverride) &&
        Objects.equals(this.eventBindCountThreshold, msgVpnQueueTemplate.eventBindCountThreshold) &&
        Objects.equals(this.eventMsgSpoolUsageThreshold, msgVpnQueueTemplate.eventMsgSpoolUsageThreshold) &&
        Objects.equals(this.eventRejectLowPriorityMsgLimitThreshold, msgVpnQueueTemplate.eventRejectLowPriorityMsgLimitThreshold) &&
        Objects.equals(this.maxBindCount, msgVpnQueueTemplate.maxBindCount) &&
        Objects.equals(this.maxDeliveredUnackedMsgsPerFlow, msgVpnQueueTemplate.maxDeliveredUnackedMsgsPerFlow) &&
        Objects.equals(this.maxMsgSize, msgVpnQueueTemplate.maxMsgSize) &&
        Objects.equals(this.maxMsgSpoolUsage, msgVpnQueueTemplate.maxMsgSpoolUsage) &&
        Objects.equals(this.maxRedeliveryCount, msgVpnQueueTemplate.maxRedeliveryCount) &&
        Objects.equals(this.maxTtl, msgVpnQueueTemplate.maxTtl) &&
        Objects.equals(this.msgVpnName, msgVpnQueueTemplate.msgVpnName) &&
        Objects.equals(this.permission, msgVpnQueueTemplate.permission) &&
        Objects.equals(this.queueNameFilter, msgVpnQueueTemplate.queueNameFilter) &&
        Objects.equals(this.queueTemplateName, msgVpnQueueTemplate.queueTemplateName) &&
        Objects.equals(this.rejectLowPriorityMsgEnabled, msgVpnQueueTemplate.rejectLowPriorityMsgEnabled) &&
        Objects.equals(this.rejectLowPriorityMsgLimit, msgVpnQueueTemplate.rejectLowPriorityMsgLimit) &&
        Objects.equals(this.rejectMsgToSenderOnDiscardBehavior, msgVpnQueueTemplate.rejectMsgToSenderOnDiscardBehavior) &&
        Objects.equals(this.respectMsgPriorityEnabled, msgVpnQueueTemplate.respectMsgPriorityEnabled) &&
        Objects.equals(this.respectTtlEnabled, msgVpnQueueTemplate.respectTtlEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessType, consumerAckPropagationEnabled, deadMsgQueue, durabilityOverride, eventBindCountThreshold, eventMsgSpoolUsageThreshold, eventRejectLowPriorityMsgLimitThreshold, maxBindCount, maxDeliveredUnackedMsgsPerFlow, maxMsgSize, maxMsgSpoolUsage, maxRedeliveryCount, maxTtl, msgVpnName, permission, queueNameFilter, queueTemplateName, rejectLowPriorityMsgEnabled, rejectLowPriorityMsgLimit, rejectMsgToSenderOnDiscardBehavior, respectMsgPriorityEnabled, respectTtlEnabled);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnQueueTemplate {\n");
    
    sb.append("    accessType: ").append(toIndentedString(accessType)).append("\n");
    sb.append("    consumerAckPropagationEnabled: ").append(toIndentedString(consumerAckPropagationEnabled)).append("\n");
    sb.append("    deadMsgQueue: ").append(toIndentedString(deadMsgQueue)).append("\n");
    sb.append("    durabilityOverride: ").append(toIndentedString(durabilityOverride)).append("\n");
    sb.append("    eventBindCountThreshold: ").append(toIndentedString(eventBindCountThreshold)).append("\n");
    sb.append("    eventMsgSpoolUsageThreshold: ").append(toIndentedString(eventMsgSpoolUsageThreshold)).append("\n");
    sb.append("    eventRejectLowPriorityMsgLimitThreshold: ").append(toIndentedString(eventRejectLowPriorityMsgLimitThreshold)).append("\n");
    sb.append("    maxBindCount: ").append(toIndentedString(maxBindCount)).append("\n");
    sb.append("    maxDeliveredUnackedMsgsPerFlow: ").append(toIndentedString(maxDeliveredUnackedMsgsPerFlow)).append("\n");
    sb.append("    maxMsgSize: ").append(toIndentedString(maxMsgSize)).append("\n");
    sb.append("    maxMsgSpoolUsage: ").append(toIndentedString(maxMsgSpoolUsage)).append("\n");
    sb.append("    maxRedeliveryCount: ").append(toIndentedString(maxRedeliveryCount)).append("\n");
    sb.append("    maxTtl: ").append(toIndentedString(maxTtl)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    permission: ").append(toIndentedString(permission)).append("\n");
    sb.append("    queueNameFilter: ").append(toIndentedString(queueNameFilter)).append("\n");
    sb.append("    queueTemplateName: ").append(toIndentedString(queueTemplateName)).append("\n");
    sb.append("    rejectLowPriorityMsgEnabled: ").append(toIndentedString(rejectLowPriorityMsgEnabled)).append("\n");
    sb.append("    rejectLowPriorityMsgLimit: ").append(toIndentedString(rejectLowPriorityMsgLimit)).append("\n");
    sb.append("    rejectMsgToSenderOnDiscardBehavior: ").append(toIndentedString(rejectMsgToSenderOnDiscardBehavior)).append("\n");
    sb.append("    respectMsgPriorityEnabled: ").append(toIndentedString(respectMsgPriorityEnabled)).append("\n");
    sb.append("    respectTtlEnabled: ").append(toIndentedString(respectTtlEnabled)).append("\n");
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

