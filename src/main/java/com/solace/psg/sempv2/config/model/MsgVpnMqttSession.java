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

package com.solace.psg.sempv2.config.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.solace.psg.sempv2.config.model.EventThreshold;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * MsgVpnMqttSession
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:49:16.603913+01:00[Europe/London]")
public class MsgVpnMqttSession {
  @SerializedName("enabled")
  private Boolean enabled = null;

  @SerializedName("mqttSessionClientId")
  private String mqttSessionClientId = null;

  /**
   * The virtual router of the MQTT Session. The allowed values and their meaning are:  &lt;pre&gt; \&quot;primary\&quot; - The MQTT Session belongs to the primary virtual router. \&quot;backup\&quot; - The MQTT Session belongs to the backup virtual router. &lt;/pre&gt; 
   */
  @JsonAdapter(MqttSessionVirtualRouterEnum.Adapter.class)
  public enum MqttSessionVirtualRouterEnum {
    PRIMARY("primary"),
    BACKUP("backup");

    private String value;

    MqttSessionVirtualRouterEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static MqttSessionVirtualRouterEnum fromValue(String text) {
      for (MqttSessionVirtualRouterEnum b : MqttSessionVirtualRouterEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<MqttSessionVirtualRouterEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final MqttSessionVirtualRouterEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public MqttSessionVirtualRouterEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return MqttSessionVirtualRouterEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("mqttSessionVirtualRouter")
  private MqttSessionVirtualRouterEnum mqttSessionVirtualRouter = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("owner")
  private String owner = null;

  @SerializedName("queueConsumerAckPropagationEnabled")
  private Boolean queueConsumerAckPropagationEnabled = null;

  @SerializedName("queueDeadMsgQueue")
  private String queueDeadMsgQueue = null;

  @SerializedName("queueEventBindCountThreshold")
  private EventThreshold queueEventBindCountThreshold = null;

  @SerializedName("queueEventMsgSpoolUsageThreshold")
  private EventThreshold queueEventMsgSpoolUsageThreshold = null;

  @SerializedName("queueEventRejectLowPriorityMsgLimitThreshold")
  private EventThreshold queueEventRejectLowPriorityMsgLimitThreshold = null;

  @SerializedName("queueMaxBindCount")
  private Long queueMaxBindCount = null;

  @SerializedName("queueMaxDeliveredUnackedMsgsPerFlow")
  private Long queueMaxDeliveredUnackedMsgsPerFlow = null;

  @SerializedName("queueMaxMsgSize")
  private Integer queueMaxMsgSize = null;

  @SerializedName("queueMaxMsgSpoolUsage")
  private Long queueMaxMsgSpoolUsage = null;

  @SerializedName("queueMaxRedeliveryCount")
  private Long queueMaxRedeliveryCount = null;

  @SerializedName("queueMaxTtl")
  private Long queueMaxTtl = null;

  @SerializedName("queueRejectLowPriorityMsgEnabled")
  private Boolean queueRejectLowPriorityMsgEnabled = null;

  @SerializedName("queueRejectLowPriorityMsgLimit")
  private Long queueRejectLowPriorityMsgLimit = null;

  /**
   * Determines when to return negative acknowledgements (NACKs) to sending clients on message discards. Note that NACKs cause the message to not be delivered to any destination and Transacted Session commits to fail. The default value is &#x60;\&quot;when-queue-enabled\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;always\&quot; - Always return a negative acknowledgment (NACK) to the sending client on message discard. \&quot;when-queue-enabled\&quot; - Only return a negative acknowledgment (NACK) to the sending client on message discard when the Queue is enabled. \&quot;never\&quot; - Never return a negative acknowledgment (NACK) to the sending client on message discard. &lt;/pre&gt;  Available since 2.14.
   */
  @JsonAdapter(QueueRejectMsgToSenderOnDiscardBehaviorEnum.Adapter.class)
  public enum QueueRejectMsgToSenderOnDiscardBehaviorEnum {
    ALWAYS("always"),
    WHEN_QUEUE_ENABLED("when-queue-enabled"),
    NEVER("never");

    private String value;

    QueueRejectMsgToSenderOnDiscardBehaviorEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static QueueRejectMsgToSenderOnDiscardBehaviorEnum fromValue(String text) {
      for (QueueRejectMsgToSenderOnDiscardBehaviorEnum b : QueueRejectMsgToSenderOnDiscardBehaviorEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<QueueRejectMsgToSenderOnDiscardBehaviorEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final QueueRejectMsgToSenderOnDiscardBehaviorEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public QueueRejectMsgToSenderOnDiscardBehaviorEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return QueueRejectMsgToSenderOnDiscardBehaviorEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("queueRejectMsgToSenderOnDiscardBehavior")
  private QueueRejectMsgToSenderOnDiscardBehaviorEnum queueRejectMsgToSenderOnDiscardBehavior = null;

  @SerializedName("queueRespectTtlEnabled")
  private Boolean queueRespectTtlEnabled = null;

  public MsgVpnMqttSession enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

   /**
   * Enable or disable the MQTT Session. When disabled, the client is disconnected, new messages matching QoS 0 subscriptions are discarded, and new messages matching QoS 1 subscriptions are stored for future delivery. The default value is &#x60;false&#x60;.
   * @return enabled
  **/
  @Schema(description = "Enable or disable the MQTT Session. When disabled, the client is disconnected, new messages matching QoS 0 subscriptions are discarded, and new messages matching QoS 1 subscriptions are stored for future delivery. The default value is `false`.")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public MsgVpnMqttSession mqttSessionClientId(String mqttSessionClientId) {
    this.mqttSessionClientId = mqttSessionClientId;
    return this;
  }

   /**
   * The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet.
   * @return mqttSessionClientId
  **/
  @Schema(description = "The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet.")
  public String getMqttSessionClientId() {
    return mqttSessionClientId;
  }

  public void setMqttSessionClientId(String mqttSessionClientId) {
    this.mqttSessionClientId = mqttSessionClientId;
  }

  public MsgVpnMqttSession mqttSessionVirtualRouter(MqttSessionVirtualRouterEnum mqttSessionVirtualRouter) {
    this.mqttSessionVirtualRouter = mqttSessionVirtualRouter;
    return this;
  }

   /**
   * The virtual router of the MQTT Session. The allowed values and their meaning are:  &lt;pre&gt; \&quot;primary\&quot; - The MQTT Session belongs to the primary virtual router. \&quot;backup\&quot; - The MQTT Session belongs to the backup virtual router. &lt;/pre&gt; 
   * @return mqttSessionVirtualRouter
  **/
  @Schema(description = "The virtual router of the MQTT Session. The allowed values and their meaning are:  <pre> \"primary\" - The MQTT Session belongs to the primary virtual router. \"backup\" - The MQTT Session belongs to the backup virtual router. </pre> ")
  public MqttSessionVirtualRouterEnum getMqttSessionVirtualRouter() {
    return mqttSessionVirtualRouter;
  }

  public void setMqttSessionVirtualRouter(MqttSessionVirtualRouterEnum mqttSessionVirtualRouter) {
    this.mqttSessionVirtualRouter = mqttSessionVirtualRouter;
  }

  public MsgVpnMqttSession msgVpnName(String msgVpnName) {
    this.msgVpnName = msgVpnName;
    return this;
  }

   /**
   * The name of the Message VPN.
   * @return msgVpnName
  **/
  @Schema(description = "The name of the Message VPN.")
  public String getMsgVpnName() {
    return msgVpnName;
  }

  public void setMsgVpnName(String msgVpnName) {
    this.msgVpnName = msgVpnName;
  }

  public MsgVpnMqttSession owner(String owner) {
    this.owner = owner;
    return this;
  }

   /**
   * The owner of the MQTT Session. For externally-created sessions this defaults to the Client Username of the connecting client. For management-created sessions this defaults to empty. The default value is &#x60;\&quot;\&quot;&#x60;.
   * @return owner
  **/
  @Schema(description = "The owner of the MQTT Session. For externally-created sessions this defaults to the Client Username of the connecting client. For management-created sessions this defaults to empty. The default value is `\"\"`.")
  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public MsgVpnMqttSession queueConsumerAckPropagationEnabled(Boolean queueConsumerAckPropagationEnabled) {
    this.queueConsumerAckPropagationEnabled = queueConsumerAckPropagationEnabled;
    return this;
  }

   /**
   * Enable or disable the propagation of consumer acknowledgements (ACKs) received on the active replication Message VPN to the standby replication Message VPN. The default value is &#x60;true&#x60;. Available since 2.14.
   * @return queueConsumerAckPropagationEnabled
  **/
  @Schema(description = "Enable or disable the propagation of consumer acknowledgements (ACKs) received on the active replication Message VPN to the standby replication Message VPN. The default value is `true`. Available since 2.14.")
  public Boolean isQueueConsumerAckPropagationEnabled() {
    return queueConsumerAckPropagationEnabled;
  }

  public void setQueueConsumerAckPropagationEnabled(Boolean queueConsumerAckPropagationEnabled) {
    this.queueConsumerAckPropagationEnabled = queueConsumerAckPropagationEnabled;
  }

  public MsgVpnMqttSession queueDeadMsgQueue(String queueDeadMsgQueue) {
    this.queueDeadMsgQueue = queueDeadMsgQueue;
    return this;
  }

   /**
   * The name of the Dead Message Queue (DMQ) used by the MQTT Session Queue. The default value is &#x60;\&quot;#DEAD_MSG_QUEUE\&quot;&#x60;. Available since 2.14.
   * @return queueDeadMsgQueue
  **/
  @Schema(description = "The name of the Dead Message Queue (DMQ) used by the MQTT Session Queue. The default value is `\"#DEAD_MSG_QUEUE\"`. Available since 2.14.")
  public String getQueueDeadMsgQueue() {
    return queueDeadMsgQueue;
  }

  public void setQueueDeadMsgQueue(String queueDeadMsgQueue) {
    this.queueDeadMsgQueue = queueDeadMsgQueue;
  }

  public MsgVpnMqttSession queueEventBindCountThreshold(EventThreshold queueEventBindCountThreshold) {
    this.queueEventBindCountThreshold = queueEventBindCountThreshold;
    return this;
  }

   /**
   * Get queueEventBindCountThreshold
   * @return queueEventBindCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getQueueEventBindCountThreshold() {
    return queueEventBindCountThreshold;
  }

  public void setQueueEventBindCountThreshold(EventThreshold queueEventBindCountThreshold) {
    this.queueEventBindCountThreshold = queueEventBindCountThreshold;
  }

  public MsgVpnMqttSession queueEventMsgSpoolUsageThreshold(EventThreshold queueEventMsgSpoolUsageThreshold) {
    this.queueEventMsgSpoolUsageThreshold = queueEventMsgSpoolUsageThreshold;
    return this;
  }

   /**
   * Get queueEventMsgSpoolUsageThreshold
   * @return queueEventMsgSpoolUsageThreshold
  **/
  @Schema(description = "")
  public EventThreshold getQueueEventMsgSpoolUsageThreshold() {
    return queueEventMsgSpoolUsageThreshold;
  }

  public void setQueueEventMsgSpoolUsageThreshold(EventThreshold queueEventMsgSpoolUsageThreshold) {
    this.queueEventMsgSpoolUsageThreshold = queueEventMsgSpoolUsageThreshold;
  }

  public MsgVpnMqttSession queueEventRejectLowPriorityMsgLimitThreshold(EventThreshold queueEventRejectLowPriorityMsgLimitThreshold) {
    this.queueEventRejectLowPriorityMsgLimitThreshold = queueEventRejectLowPriorityMsgLimitThreshold;
    return this;
  }

   /**
   * Get queueEventRejectLowPriorityMsgLimitThreshold
   * @return queueEventRejectLowPriorityMsgLimitThreshold
  **/
  @Schema(description = "")
  public EventThreshold getQueueEventRejectLowPriorityMsgLimitThreshold() {
    return queueEventRejectLowPriorityMsgLimitThreshold;
  }

  public void setQueueEventRejectLowPriorityMsgLimitThreshold(EventThreshold queueEventRejectLowPriorityMsgLimitThreshold) {
    this.queueEventRejectLowPriorityMsgLimitThreshold = queueEventRejectLowPriorityMsgLimitThreshold;
  }

  public MsgVpnMqttSession queueMaxBindCount(Long queueMaxBindCount) {
    this.queueMaxBindCount = queueMaxBindCount;
    return this;
  }

   /**
   * The maximum number of consumer flows that can bind to the MQTT Session Queue. The default value is &#x60;1000&#x60;. Available since 2.14.
   * @return queueMaxBindCount
  **/
  @Schema(description = "The maximum number of consumer flows that can bind to the MQTT Session Queue. The default value is `1000`. Available since 2.14.")
  public Long getQueueMaxBindCount() {
    return queueMaxBindCount;
  }

  public void setQueueMaxBindCount(Long queueMaxBindCount) {
    this.queueMaxBindCount = queueMaxBindCount;
  }

  public MsgVpnMqttSession queueMaxDeliveredUnackedMsgsPerFlow(Long queueMaxDeliveredUnackedMsgsPerFlow) {
    this.queueMaxDeliveredUnackedMsgsPerFlow = queueMaxDeliveredUnackedMsgsPerFlow;
    return this;
  }

   /**
   * The maximum number of messages delivered but not acknowledged per flow for the MQTT Session Queue. The default value is &#x60;10000&#x60;. Available since 2.14.
   * @return queueMaxDeliveredUnackedMsgsPerFlow
  **/
  @Schema(description = "The maximum number of messages delivered but not acknowledged per flow for the MQTT Session Queue. The default value is `10000`. Available since 2.14.")
  public Long getQueueMaxDeliveredUnackedMsgsPerFlow() {
    return queueMaxDeliveredUnackedMsgsPerFlow;
  }

  public void setQueueMaxDeliveredUnackedMsgsPerFlow(Long queueMaxDeliveredUnackedMsgsPerFlow) {
    this.queueMaxDeliveredUnackedMsgsPerFlow = queueMaxDeliveredUnackedMsgsPerFlow;
  }

  public MsgVpnMqttSession queueMaxMsgSize(Integer queueMaxMsgSize) {
    this.queueMaxMsgSize = queueMaxMsgSize;
    return this;
  }

   /**
   * The maximum message size allowed in the MQTT Session Queue, in bytes (B). The default value is &#x60;10000000&#x60;. Available since 2.14.
   * @return queueMaxMsgSize
  **/
  @Schema(description = "The maximum message size allowed in the MQTT Session Queue, in bytes (B). The default value is `10000000`. Available since 2.14.")
  public Integer getQueueMaxMsgSize() {
    return queueMaxMsgSize;
  }

  public void setQueueMaxMsgSize(Integer queueMaxMsgSize) {
    this.queueMaxMsgSize = queueMaxMsgSize;
  }

  public MsgVpnMqttSession queueMaxMsgSpoolUsage(Long queueMaxMsgSpoolUsage) {
    this.queueMaxMsgSpoolUsage = queueMaxMsgSpoolUsage;
    return this;
  }

   /**
   * The maximum message spool usage allowed by the MQTT Session Queue, in megabytes (MB). A value of 0 only allows spooling of the last message received and disables quota checking. The default value is &#x60;1500&#x60;. Available since 2.14.
   * @return queueMaxMsgSpoolUsage
  **/
  @Schema(description = "The maximum message spool usage allowed by the MQTT Session Queue, in megabytes (MB). A value of 0 only allows spooling of the last message received and disables quota checking. The default value is `1500`. Available since 2.14.")
  public Long getQueueMaxMsgSpoolUsage() {
    return queueMaxMsgSpoolUsage;
  }

  public void setQueueMaxMsgSpoolUsage(Long queueMaxMsgSpoolUsage) {
    this.queueMaxMsgSpoolUsage = queueMaxMsgSpoolUsage;
  }

  public MsgVpnMqttSession queueMaxRedeliveryCount(Long queueMaxRedeliveryCount) {
    this.queueMaxRedeliveryCount = queueMaxRedeliveryCount;
    return this;
  }

   /**
   * The maximum number of times the MQTT Session Queue will attempt redelivery of a message prior to it being discarded or moved to the DMQ. A value of 0 means to retry forever. The default value is &#x60;0&#x60;. Available since 2.14.
   * @return queueMaxRedeliveryCount
  **/
  @Schema(description = "The maximum number of times the MQTT Session Queue will attempt redelivery of a message prior to it being discarded or moved to the DMQ. A value of 0 means to retry forever. The default value is `0`. Available since 2.14.")
  public Long getQueueMaxRedeliveryCount() {
    return queueMaxRedeliveryCount;
  }

  public void setQueueMaxRedeliveryCount(Long queueMaxRedeliveryCount) {
    this.queueMaxRedeliveryCount = queueMaxRedeliveryCount;
  }

  public MsgVpnMqttSession queueMaxTtl(Long queueMaxTtl) {
    this.queueMaxTtl = queueMaxTtl;
    return this;
  }

   /**
   * The maximum time in seconds a message can stay in the MQTT Session Queue when &#x60;queueRespectTtlEnabled&#x60; is &#x60;\&quot;true\&quot;&#x60;. A message expires when the lesser of the sender assigned time-to-live (TTL) in the message and the &#x60;queueMaxTtl&#x60; configured for the MQTT Session Queue, is exceeded. A value of 0 disables expiry. The default value is &#x60;0&#x60;. Available since 2.14.
   * @return queueMaxTtl
  **/
  @Schema(description = "The maximum time in seconds a message can stay in the MQTT Session Queue when `queueRespectTtlEnabled` is `\"true\"`. A message expires when the lesser of the sender assigned time-to-live (TTL) in the message and the `queueMaxTtl` configured for the MQTT Session Queue, is exceeded. A value of 0 disables expiry. The default value is `0`. Available since 2.14.")
  public Long getQueueMaxTtl() {
    return queueMaxTtl;
  }

  public void setQueueMaxTtl(Long queueMaxTtl) {
    this.queueMaxTtl = queueMaxTtl;
  }

  public MsgVpnMqttSession queueRejectLowPriorityMsgEnabled(Boolean queueRejectLowPriorityMsgEnabled) {
    this.queueRejectLowPriorityMsgEnabled = queueRejectLowPriorityMsgEnabled;
    return this;
  }

   /**
   * Enable or disable the checking of low priority messages against the &#x60;queueRejectLowPriorityMsgLimit&#x60;. This may only be enabled if &#x60;queueRejectMsgToSenderOnDiscardBehavior&#x60; does not have a value of &#x60;\&quot;never\&quot;&#x60;. The default value is &#x60;false&#x60;. Available since 2.14.
   * @return queueRejectLowPriorityMsgEnabled
  **/
  @Schema(description = "Enable or disable the checking of low priority messages against the `queueRejectLowPriorityMsgLimit`. This may only be enabled if `queueRejectMsgToSenderOnDiscardBehavior` does not have a value of `\"never\"`. The default value is `false`. Available since 2.14.")
  public Boolean isQueueRejectLowPriorityMsgEnabled() {
    return queueRejectLowPriorityMsgEnabled;
  }

  public void setQueueRejectLowPriorityMsgEnabled(Boolean queueRejectLowPriorityMsgEnabled) {
    this.queueRejectLowPriorityMsgEnabled = queueRejectLowPriorityMsgEnabled;
  }

  public MsgVpnMqttSession queueRejectLowPriorityMsgLimit(Long queueRejectLowPriorityMsgLimit) {
    this.queueRejectLowPriorityMsgLimit = queueRejectLowPriorityMsgLimit;
    return this;
  }

   /**
   * The number of messages of any priority in the MQTT Session Queue above which low priority messages are not admitted but higher priority messages are allowed. The default value is &#x60;0&#x60;. Available since 2.14.
   * @return queueRejectLowPriorityMsgLimit
  **/
  @Schema(description = "The number of messages of any priority in the MQTT Session Queue above which low priority messages are not admitted but higher priority messages are allowed. The default value is `0`. Available since 2.14.")
  public Long getQueueRejectLowPriorityMsgLimit() {
    return queueRejectLowPriorityMsgLimit;
  }

  public void setQueueRejectLowPriorityMsgLimit(Long queueRejectLowPriorityMsgLimit) {
    this.queueRejectLowPriorityMsgLimit = queueRejectLowPriorityMsgLimit;
  }

  public MsgVpnMqttSession queueRejectMsgToSenderOnDiscardBehavior(QueueRejectMsgToSenderOnDiscardBehaviorEnum queueRejectMsgToSenderOnDiscardBehavior) {
    this.queueRejectMsgToSenderOnDiscardBehavior = queueRejectMsgToSenderOnDiscardBehavior;
    return this;
  }

   /**
   * Determines when to return negative acknowledgements (NACKs) to sending clients on message discards. Note that NACKs cause the message to not be delivered to any destination and Transacted Session commits to fail. The default value is &#x60;\&quot;when-queue-enabled\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;always\&quot; - Always return a negative acknowledgment (NACK) to the sending client on message discard. \&quot;when-queue-enabled\&quot; - Only return a negative acknowledgment (NACK) to the sending client on message discard when the Queue is enabled. \&quot;never\&quot; - Never return a negative acknowledgment (NACK) to the sending client on message discard. &lt;/pre&gt;  Available since 2.14.
   * @return queueRejectMsgToSenderOnDiscardBehavior
  **/
  @Schema(description = "Determines when to return negative acknowledgements (NACKs) to sending clients on message discards. Note that NACKs cause the message to not be delivered to any destination and Transacted Session commits to fail. The default value is `\"when-queue-enabled\"`. The allowed values and their meaning are:  <pre> \"always\" - Always return a negative acknowledgment (NACK) to the sending client on message discard. \"when-queue-enabled\" - Only return a negative acknowledgment (NACK) to the sending client on message discard when the Queue is enabled. \"never\" - Never return a negative acknowledgment (NACK) to the sending client on message discard. </pre>  Available since 2.14.")
  public QueueRejectMsgToSenderOnDiscardBehaviorEnum getQueueRejectMsgToSenderOnDiscardBehavior() {
    return queueRejectMsgToSenderOnDiscardBehavior;
  }

  public void setQueueRejectMsgToSenderOnDiscardBehavior(QueueRejectMsgToSenderOnDiscardBehaviorEnum queueRejectMsgToSenderOnDiscardBehavior) {
    this.queueRejectMsgToSenderOnDiscardBehavior = queueRejectMsgToSenderOnDiscardBehavior;
  }

  public MsgVpnMqttSession queueRespectTtlEnabled(Boolean queueRespectTtlEnabled) {
    this.queueRespectTtlEnabled = queueRespectTtlEnabled;
    return this;
  }

   /**
   * Enable or disable the respecting of the time-to-live (TTL) for messages in the MQTT Session Queue. When enabled, expired messages are discarded or moved to the DMQ. The default value is &#x60;false&#x60;. Available since 2.14.
   * @return queueRespectTtlEnabled
  **/
  @Schema(description = "Enable or disable the respecting of the time-to-live (TTL) for messages in the MQTT Session Queue. When enabled, expired messages are discarded or moved to the DMQ. The default value is `false`. Available since 2.14.")
  public Boolean isQueueRespectTtlEnabled() {
    return queueRespectTtlEnabled;
  }

  public void setQueueRespectTtlEnabled(Boolean queueRespectTtlEnabled) {
    this.queueRespectTtlEnabled = queueRespectTtlEnabled;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnMqttSession msgVpnMqttSession = (MsgVpnMqttSession) o;
    return Objects.equals(this.enabled, msgVpnMqttSession.enabled) &&
        Objects.equals(this.mqttSessionClientId, msgVpnMqttSession.mqttSessionClientId) &&
        Objects.equals(this.mqttSessionVirtualRouter, msgVpnMqttSession.mqttSessionVirtualRouter) &&
        Objects.equals(this.msgVpnName, msgVpnMqttSession.msgVpnName) &&
        Objects.equals(this.owner, msgVpnMqttSession.owner) &&
        Objects.equals(this.queueConsumerAckPropagationEnabled, msgVpnMqttSession.queueConsumerAckPropagationEnabled) &&
        Objects.equals(this.queueDeadMsgQueue, msgVpnMqttSession.queueDeadMsgQueue) &&
        Objects.equals(this.queueEventBindCountThreshold, msgVpnMqttSession.queueEventBindCountThreshold) &&
        Objects.equals(this.queueEventMsgSpoolUsageThreshold, msgVpnMqttSession.queueEventMsgSpoolUsageThreshold) &&
        Objects.equals(this.queueEventRejectLowPriorityMsgLimitThreshold, msgVpnMqttSession.queueEventRejectLowPriorityMsgLimitThreshold) &&
        Objects.equals(this.queueMaxBindCount, msgVpnMqttSession.queueMaxBindCount) &&
        Objects.equals(this.queueMaxDeliveredUnackedMsgsPerFlow, msgVpnMqttSession.queueMaxDeliveredUnackedMsgsPerFlow) &&
        Objects.equals(this.queueMaxMsgSize, msgVpnMqttSession.queueMaxMsgSize) &&
        Objects.equals(this.queueMaxMsgSpoolUsage, msgVpnMqttSession.queueMaxMsgSpoolUsage) &&
        Objects.equals(this.queueMaxRedeliveryCount, msgVpnMqttSession.queueMaxRedeliveryCount) &&
        Objects.equals(this.queueMaxTtl, msgVpnMqttSession.queueMaxTtl) &&
        Objects.equals(this.queueRejectLowPriorityMsgEnabled, msgVpnMqttSession.queueRejectLowPriorityMsgEnabled) &&
        Objects.equals(this.queueRejectLowPriorityMsgLimit, msgVpnMqttSession.queueRejectLowPriorityMsgLimit) &&
        Objects.equals(this.queueRejectMsgToSenderOnDiscardBehavior, msgVpnMqttSession.queueRejectMsgToSenderOnDiscardBehavior) &&
        Objects.equals(this.queueRespectTtlEnabled, msgVpnMqttSession.queueRespectTtlEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, mqttSessionClientId, mqttSessionVirtualRouter, msgVpnName, owner, queueConsumerAckPropagationEnabled, queueDeadMsgQueue, queueEventBindCountThreshold, queueEventMsgSpoolUsageThreshold, queueEventRejectLowPriorityMsgLimitThreshold, queueMaxBindCount, queueMaxDeliveredUnackedMsgsPerFlow, queueMaxMsgSize, queueMaxMsgSpoolUsage, queueMaxRedeliveryCount, queueMaxTtl, queueRejectLowPriorityMsgEnabled, queueRejectLowPriorityMsgLimit, queueRejectMsgToSenderOnDiscardBehavior, queueRespectTtlEnabled);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnMqttSession {\n");
    
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    mqttSessionClientId: ").append(toIndentedString(mqttSessionClientId)).append("\n");
    sb.append("    mqttSessionVirtualRouter: ").append(toIndentedString(mqttSessionVirtualRouter)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
    sb.append("    queueConsumerAckPropagationEnabled: ").append(toIndentedString(queueConsumerAckPropagationEnabled)).append("\n");
    sb.append("    queueDeadMsgQueue: ").append(toIndentedString(queueDeadMsgQueue)).append("\n");
    sb.append("    queueEventBindCountThreshold: ").append(toIndentedString(queueEventBindCountThreshold)).append("\n");
    sb.append("    queueEventMsgSpoolUsageThreshold: ").append(toIndentedString(queueEventMsgSpoolUsageThreshold)).append("\n");
    sb.append("    queueEventRejectLowPriorityMsgLimitThreshold: ").append(toIndentedString(queueEventRejectLowPriorityMsgLimitThreshold)).append("\n");
    sb.append("    queueMaxBindCount: ").append(toIndentedString(queueMaxBindCount)).append("\n");
    sb.append("    queueMaxDeliveredUnackedMsgsPerFlow: ").append(toIndentedString(queueMaxDeliveredUnackedMsgsPerFlow)).append("\n");
    sb.append("    queueMaxMsgSize: ").append(toIndentedString(queueMaxMsgSize)).append("\n");
    sb.append("    queueMaxMsgSpoolUsage: ").append(toIndentedString(queueMaxMsgSpoolUsage)).append("\n");
    sb.append("    queueMaxRedeliveryCount: ").append(toIndentedString(queueMaxRedeliveryCount)).append("\n");
    sb.append("    queueMaxTtl: ").append(toIndentedString(queueMaxTtl)).append("\n");
    sb.append("    queueRejectLowPriorityMsgEnabled: ").append(toIndentedString(queueRejectLowPriorityMsgEnabled)).append("\n");
    sb.append("    queueRejectLowPriorityMsgLimit: ").append(toIndentedString(queueRejectLowPriorityMsgLimit)).append("\n");
    sb.append("    queueRejectMsgToSenderOnDiscardBehavior: ").append(toIndentedString(queueRejectMsgToSenderOnDiscardBehavior)).append("\n");
    sb.append("    queueRespectTtlEnabled: ").append(toIndentedString(queueRespectTtlEnabled)).append("\n");
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
