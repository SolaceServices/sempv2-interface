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
import com.solace.psg.sempv2.monitor.model.EventThreshold;
import com.solace.psg.sempv2.monitor.model.MsgVpnMqttSessionCounter;
import java.io.IOException;

/**
 * MsgVpnMqttSession
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-13T23:07:13.589Z")
public class MsgVpnMqttSession {
  @SerializedName("clean")
  private Boolean clean = null;

  @SerializedName("clientName")
  private String clientName = null;

  @SerializedName("counter")
  private MsgVpnMqttSessionCounter counter = null;

  @SerializedName("createdByManagement")
  private Boolean createdByManagement = null;

  @SerializedName("enabled")
  private Boolean enabled = null;

  @SerializedName("mqttConnackErrorTxCount")
  private Long mqttConnackErrorTxCount = null;

  @SerializedName("mqttConnackTxCount")
  private Long mqttConnackTxCount = null;

  @SerializedName("mqttConnectRxCount")
  private Integer mqttConnectRxCount = null;

  @SerializedName("mqttDisconnectRxCount")
  private Long mqttDisconnectRxCount = null;

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
        String value = jsonReader.nextString();
        return MqttSessionVirtualRouterEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("mqttSessionVirtualRouter")
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

  @SerializedName("queueName")
  private String queueName = null;

  @SerializedName("queueRejectLowPriorityMsgEnabled")
  private Boolean queueRejectLowPriorityMsgEnabled = null;

  @SerializedName("queueRejectLowPriorityMsgLimit")
  private Long queueRejectLowPriorityMsgLimit = null;

  /**
   * Indicates whether negative acknowledgements (NACKs) are returned to sending clients on message discards. Note that NACKs cause the message to not be delivered to any destination and Transacted Session commits to fail. The allowed values and their meaning are:  &lt;pre&gt; \&quot;always\&quot; - Always return a negative acknowledgment (NACK) to the sending client on message discard. \&quot;when-queue-enabled\&quot; - Only return a negative acknowledgment (NACK) to the sending client on message discard when the Queue is enabled. \&quot;never\&quot; - Never return a negative acknowledgment (NACK) to the sending client on message discard. &lt;/pre&gt;  Available since 2.14.
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
        String value = jsonReader.nextString();
        return QueueRejectMsgToSenderOnDiscardBehaviorEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("queueRejectMsgToSenderOnDiscardBehavior")
  private QueueRejectMsgToSenderOnDiscardBehaviorEnum queueRejectMsgToSenderOnDiscardBehavior = null;

  @SerializedName("queueRespectTtlEnabled")
  private Boolean queueRespectTtlEnabled = null;

  @SerializedName("will")
  private Boolean will = null;

  public MsgVpnMqttSession clean(Boolean clean) {
    this.clean = clean;
    return this;
  }

   /**
   * Indicates whether the Client requested a clean (newly created) MQTT Session when connecting. If not clean (already existing), then previously stored messages for QoS 1 subscriptions are delivered.
   * @return clean
  **/
  @ApiModelProperty(value = "Indicates whether the Client requested a clean (newly created) MQTT Session when connecting. If not clean (already existing), then previously stored messages for QoS 1 subscriptions are delivered.")
  public Boolean isClean() {
    return clean;
  }

  public void setClean(Boolean clean) {
    this.clean = clean;
  }

  public MsgVpnMqttSession clientName(String clientName) {
    this.clientName = clientName;
    return this;
  }

   /**
   * The name of the MQTT Session Client.
   * @return clientName
  **/
  @ApiModelProperty(value = "The name of the MQTT Session Client.")
  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public MsgVpnMqttSession counter(MsgVpnMqttSessionCounter counter) {
    this.counter = counter;
    return this;
  }

   /**
   * Get counter
   * @return counter
  **/
  @ApiModelProperty(value = "")
  public MsgVpnMqttSessionCounter getCounter() {
    return counter;
  }

  public void setCounter(MsgVpnMqttSessionCounter counter) {
    this.counter = counter;
  }

  public MsgVpnMqttSession createdByManagement(Boolean createdByManagement) {
    this.createdByManagement = createdByManagement;
    return this;
  }

   /**
   * Indicates whether the MQTT Session was created by a Management API.
   * @return createdByManagement
  **/
  @ApiModelProperty(value = "Indicates whether the MQTT Session was created by a Management API.")
  public Boolean isCreatedByManagement() {
    return createdByManagement;
  }

  public void setCreatedByManagement(Boolean createdByManagement) {
    this.createdByManagement = createdByManagement;
  }

  public MsgVpnMqttSession enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

   /**
   * Indicates whether the MQTT Session is enabled.
   * @return enabled
  **/
  @ApiModelProperty(value = "Indicates whether the MQTT Session is enabled.")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public MsgVpnMqttSession mqttConnackErrorTxCount(Long mqttConnackErrorTxCount) {
    this.mqttConnackErrorTxCount = mqttConnackErrorTxCount;
    return this;
  }

   /**
   * The number of MQTT connect acknowledgment (CONNACK) refused response packets transmitted to the Client. Available since 2.13.
   * @return mqttConnackErrorTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT connect acknowledgment (CONNACK) refused response packets transmitted to the Client. Available since 2.13.")
  public Long getMqttConnackErrorTxCount() {
    return mqttConnackErrorTxCount;
  }

  public void setMqttConnackErrorTxCount(Long mqttConnackErrorTxCount) {
    this.mqttConnackErrorTxCount = mqttConnackErrorTxCount;
  }

  public MsgVpnMqttSession mqttConnackTxCount(Long mqttConnackTxCount) {
    this.mqttConnackTxCount = mqttConnackTxCount;
    return this;
  }

   /**
   * The number of MQTT connect acknowledgment (CONNACK) accepted response packets transmitted to the Client. Available since 2.13.
   * @return mqttConnackTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT connect acknowledgment (CONNACK) accepted response packets transmitted to the Client. Available since 2.13.")
  public Long getMqttConnackTxCount() {
    return mqttConnackTxCount;
  }

  public void setMqttConnackTxCount(Long mqttConnackTxCount) {
    this.mqttConnackTxCount = mqttConnackTxCount;
  }

  public MsgVpnMqttSession mqttConnectRxCount(Integer mqttConnectRxCount) {
    this.mqttConnectRxCount = mqttConnectRxCount;
    return this;
  }

   /**
   * The number of MQTT connect (CONNECT) request packets received from the Client. Available since 2.13.
   * @return mqttConnectRxCount
  **/
  @ApiModelProperty(value = "The number of MQTT connect (CONNECT) request packets received from the Client. Available since 2.13.")
  public Integer getMqttConnectRxCount() {
    return mqttConnectRxCount;
  }

  public void setMqttConnectRxCount(Integer mqttConnectRxCount) {
    this.mqttConnectRxCount = mqttConnectRxCount;
  }

  public MsgVpnMqttSession mqttDisconnectRxCount(Long mqttDisconnectRxCount) {
    this.mqttDisconnectRxCount = mqttDisconnectRxCount;
    return this;
  }

   /**
   * The number of MQTT disconnect (DISCONNECT) request packets received from the Client. Available since 2.13.
   * @return mqttDisconnectRxCount
  **/
  @ApiModelProperty(value = "The number of MQTT disconnect (DISCONNECT) request packets received from the Client. Available since 2.13.")
  public Long getMqttDisconnectRxCount() {
    return mqttDisconnectRxCount;
  }

  public void setMqttDisconnectRxCount(Long mqttDisconnectRxCount) {
    this.mqttDisconnectRxCount = mqttDisconnectRxCount;
  }

  public MsgVpnMqttSession mqttPubcompTxCount(Long mqttPubcompTxCount) {
    this.mqttPubcompTxCount = mqttPubcompTxCount;
    return this;
  }

   /**
   * The number of MQTT publish complete (PUBCOMP) packets transmitted to the Client in response to a PUBREL packet. These packets are the fourth and final packet of a QoS 2 protocol exchange. Available since 2.13.
   * @return mqttPubcompTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish complete (PUBCOMP) packets transmitted to the Client in response to a PUBREL packet. These packets are the fourth and final packet of a QoS 2 protocol exchange. Available since 2.13.")
  public Long getMqttPubcompTxCount() {
    return mqttPubcompTxCount;
  }

  public void setMqttPubcompTxCount(Long mqttPubcompTxCount) {
    this.mqttPubcompTxCount = mqttPubcompTxCount;
  }

  public MsgVpnMqttSession mqttPublishQos0RxCount(Long mqttPublishQos0RxCount) {
    this.mqttPublishQos0RxCount = mqttPublishQos0RxCount;
    return this;
  }

   /**
   * The number of MQTT publish message (PUBLISH) request packets received from the Client for QoS 0 message delivery. Available since 2.13.
   * @return mqttPublishQos0RxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish message (PUBLISH) request packets received from the Client for QoS 0 message delivery. Available since 2.13.")
  public Long getMqttPublishQos0RxCount() {
    return mqttPublishQos0RxCount;
  }

  public void setMqttPublishQos0RxCount(Long mqttPublishQos0RxCount) {
    this.mqttPublishQos0RxCount = mqttPublishQos0RxCount;
  }

  public MsgVpnMqttSession mqttPublishQos0TxCount(Long mqttPublishQos0TxCount) {
    this.mqttPublishQos0TxCount = mqttPublishQos0TxCount;
    return this;
  }

   /**
   * The number of MQTT publish message (PUBLISH) request packets transmitted to the Client for QoS 0 message delivery. Available since 2.13.
   * @return mqttPublishQos0TxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish message (PUBLISH) request packets transmitted to the Client for QoS 0 message delivery. Available since 2.13.")
  public Long getMqttPublishQos0TxCount() {
    return mqttPublishQos0TxCount;
  }

  public void setMqttPublishQos0TxCount(Long mqttPublishQos0TxCount) {
    this.mqttPublishQos0TxCount = mqttPublishQos0TxCount;
  }

  public MsgVpnMqttSession mqttPublishQos1RxCount(Long mqttPublishQos1RxCount) {
    this.mqttPublishQos1RxCount = mqttPublishQos1RxCount;
    return this;
  }

   /**
   * The number of MQTT publish message (PUBLISH) request packets received from the Client for QoS 1 message delivery. Available since 2.13.
   * @return mqttPublishQos1RxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish message (PUBLISH) request packets received from the Client for QoS 1 message delivery. Available since 2.13.")
  public Long getMqttPublishQos1RxCount() {
    return mqttPublishQos1RxCount;
  }

  public void setMqttPublishQos1RxCount(Long mqttPublishQos1RxCount) {
    this.mqttPublishQos1RxCount = mqttPublishQos1RxCount;
  }

  public MsgVpnMqttSession mqttPublishQos1TxCount(Long mqttPublishQos1TxCount) {
    this.mqttPublishQos1TxCount = mqttPublishQos1TxCount;
    return this;
  }

   /**
   * The number of MQTT publish message (PUBLISH) request packets transmitted to the Client for QoS 1 message delivery. Available since 2.13.
   * @return mqttPublishQos1TxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish message (PUBLISH) request packets transmitted to the Client for QoS 1 message delivery. Available since 2.13.")
  public Long getMqttPublishQos1TxCount() {
    return mqttPublishQos1TxCount;
  }

  public void setMqttPublishQos1TxCount(Long mqttPublishQos1TxCount) {
    this.mqttPublishQos1TxCount = mqttPublishQos1TxCount;
  }

  public MsgVpnMqttSession mqttPublishQos2RxCount(Long mqttPublishQos2RxCount) {
    this.mqttPublishQos2RxCount = mqttPublishQos2RxCount;
    return this;
  }

   /**
   * The number of MQTT publish message (PUBLISH) request packets received from the Client for QoS 2 message delivery. Available since 2.13.
   * @return mqttPublishQos2RxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish message (PUBLISH) request packets received from the Client for QoS 2 message delivery. Available since 2.13.")
  public Long getMqttPublishQos2RxCount() {
    return mqttPublishQos2RxCount;
  }

  public void setMqttPublishQos2RxCount(Long mqttPublishQos2RxCount) {
    this.mqttPublishQos2RxCount = mqttPublishQos2RxCount;
  }

  public MsgVpnMqttSession mqttPubrecTxCount(Long mqttPubrecTxCount) {
    this.mqttPubrecTxCount = mqttPubrecTxCount;
    return this;
  }

   /**
   * The number of MQTT publish received (PUBREC) packets transmitted to the Client in response to a PUBLISH packet with QoS 2. These packets are the second packet of a QoS 2 protocol exchange. Available since 2.13.
   * @return mqttPubrecTxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish received (PUBREC) packets transmitted to the Client in response to a PUBLISH packet with QoS 2. These packets are the second packet of a QoS 2 protocol exchange. Available since 2.13.")
  public Long getMqttPubrecTxCount() {
    return mqttPubrecTxCount;
  }

  public void setMqttPubrecTxCount(Long mqttPubrecTxCount) {
    this.mqttPubrecTxCount = mqttPubrecTxCount;
  }

  public MsgVpnMqttSession mqttPubrelRxCount(Long mqttPubrelRxCount) {
    this.mqttPubrelRxCount = mqttPubrelRxCount;
    return this;
  }

   /**
   * The number of MQTT publish release (PUBREL) packets received from the Client in response to a PUBREC packet. These packets are the third packet of a QoS 2 protocol exchange. Available since 2.13.
   * @return mqttPubrelRxCount
  **/
  @ApiModelProperty(value = "The number of MQTT publish release (PUBREL) packets received from the Client in response to a PUBREC packet. These packets are the third packet of a QoS 2 protocol exchange. Available since 2.13.")
  public Long getMqttPubrelRxCount() {
    return mqttPubrelRxCount;
  }

  public void setMqttPubrelRxCount(Long mqttPubrelRxCount) {
    this.mqttPubrelRxCount = mqttPubrelRxCount;
  }

  public MsgVpnMqttSession mqttSessionClientId(String mqttSessionClientId) {
    this.mqttSessionClientId = mqttSessionClientId;
    return this;
  }

   /**
   * The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet.
   * @return mqttSessionClientId
  **/
  @ApiModelProperty(value = "The Client ID of the MQTT Session, which corresponds to the ClientId provided in the MQTT CONNECT packet.")
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
  @ApiModelProperty(value = "The virtual router of the MQTT Session. The allowed values and their meaning are:  <pre> \"primary\" - The MQTT Session belongs to the primary virtual router. \"backup\" - The MQTT Session belongs to the backup virtual router. </pre> ")
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
  @ApiModelProperty(value = "The name of the Message VPN.")
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
   * The Client Username which owns the MQTT Session.
   * @return owner
  **/
  @ApiModelProperty(value = "The Client Username which owns the MQTT Session.")
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
   * Indicates whether consumer acknowledgements (ACKs) received on the active replication Message VPN  are propagated to the standby replication Message VPN. Available since 2.14.
   * @return queueConsumerAckPropagationEnabled
  **/
  @ApiModelProperty(value = "Indicates whether consumer acknowledgements (ACKs) received on the active replication Message VPN  are propagated to the standby replication Message VPN. Available since 2.14.")
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
   * The name of the Dead Message Queue (DMQ) used by the MQTT Session Queue. Available since 2.14.
   * @return queueDeadMsgQueue
  **/
  @ApiModelProperty(value = "The name of the Dead Message Queue (DMQ) used by the MQTT Session Queue. Available since 2.14.")
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
  @ApiModelProperty(value = "")
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
  @ApiModelProperty(value = "")
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
  @ApiModelProperty(value = "")
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
   * The maximum number of consumer flows that can bind to the MQTT Session Queue. Available since 2.14.
   * @return queueMaxBindCount
  **/
  @ApiModelProperty(value = "The maximum number of consumer flows that can bind to the MQTT Session Queue. Available since 2.14.")
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
   * The maximum number of messages delivered but not acknowledged per flow for the MQTT Session Queue. Available since 2.14.
   * @return queueMaxDeliveredUnackedMsgsPerFlow
  **/
  @ApiModelProperty(value = "The maximum number of messages delivered but not acknowledged per flow for the MQTT Session Queue. Available since 2.14.")
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
   * The maximum message size allowed in the MQTT Session Queue, in bytes (B). Available since 2.14.
   * @return queueMaxMsgSize
  **/
  @ApiModelProperty(value = "The maximum message size allowed in the MQTT Session Queue, in bytes (B). Available since 2.14.")
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
   * The maximum message spool usage allowed by the MQTT Session Queue, in megabytes (MB). A value of 0 only allows spooling of the last message received and disables quota checking. Available since 2.14.
   * @return queueMaxMsgSpoolUsage
  **/
  @ApiModelProperty(value = "The maximum message spool usage allowed by the MQTT Session Queue, in megabytes (MB). A value of 0 only allows spooling of the last message received and disables quota checking. Available since 2.14.")
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
   * The maximum number of times the MQTT Session Queue will attempt redelivery of a message prior to it being discarded or moved to the DMQ. A value of 0 means to retry forever. Available since 2.14.
   * @return queueMaxRedeliveryCount
  **/
  @ApiModelProperty(value = "The maximum number of times the MQTT Session Queue will attempt redelivery of a message prior to it being discarded or moved to the DMQ. A value of 0 means to retry forever. Available since 2.14.")
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
   * The maximum time in seconds a message can stay in the MQTT Session Queue when &#x60;queueRespectTtlEnabled&#x60; is &#x60;\&quot;true\&quot;&#x60;. A message expires when the lesser of the sender assigned time-to-live (TTL) in the message and the &#x60;queueMaxTtl&#x60; configured for the MQTT Session Queue, is exceeded. A value of 0 disables expiry. Available since 2.14.
   * @return queueMaxTtl
  **/
  @ApiModelProperty(value = "The maximum time in seconds a message can stay in the MQTT Session Queue when `queueRespectTtlEnabled` is `\"true\"`. A message expires when the lesser of the sender assigned time-to-live (TTL) in the message and the `queueMaxTtl` configured for the MQTT Session Queue, is exceeded. A value of 0 disables expiry. Available since 2.14.")
  public Long getQueueMaxTtl() {
    return queueMaxTtl;
  }

  public void setQueueMaxTtl(Long queueMaxTtl) {
    this.queueMaxTtl = queueMaxTtl;
  }

  public MsgVpnMqttSession queueName(String queueName) {
    this.queueName = queueName;
    return this;
  }

   /**
   * The name of the MQTT Session Queue.
   * @return queueName
  **/
  @ApiModelProperty(value = "The name of the MQTT Session Queue.")
  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public MsgVpnMqttSession queueRejectLowPriorityMsgEnabled(Boolean queueRejectLowPriorityMsgEnabled) {
    this.queueRejectLowPriorityMsgEnabled = queueRejectLowPriorityMsgEnabled;
    return this;
  }

   /**
   * Indicates whether to return negative acknowledgements (NACKs) to sending clients on message discards. Note that NACKs cause the message to not be delivered to any destination and Transacted Session commits to fail. Available since 2.14.
   * @return queueRejectLowPriorityMsgEnabled
  **/
  @ApiModelProperty(value = "Indicates whether to return negative acknowledgements (NACKs) to sending clients on message discards. Note that NACKs cause the message to not be delivered to any destination and Transacted Session commits to fail. Available since 2.14.")
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
   * The number of messages of any priority in the MQTT Session Queue above which low priority messages are not admitted but higher priority messages are allowed. Available since 2.14.
   * @return queueRejectLowPriorityMsgLimit
  **/
  @ApiModelProperty(value = "The number of messages of any priority in the MQTT Session Queue above which low priority messages are not admitted but higher priority messages are allowed. Available since 2.14.")
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
   * Indicates whether negative acknowledgements (NACKs) are returned to sending clients on message discards. Note that NACKs cause the message to not be delivered to any destination and Transacted Session commits to fail. The allowed values and their meaning are:  &lt;pre&gt; \&quot;always\&quot; - Always return a negative acknowledgment (NACK) to the sending client on message discard. \&quot;when-queue-enabled\&quot; - Only return a negative acknowledgment (NACK) to the sending client on message discard when the Queue is enabled. \&quot;never\&quot; - Never return a negative acknowledgment (NACK) to the sending client on message discard. &lt;/pre&gt;  Available since 2.14.
   * @return queueRejectMsgToSenderOnDiscardBehavior
  **/
  @ApiModelProperty(value = "Indicates whether negative acknowledgements (NACKs) are returned to sending clients on message discards. Note that NACKs cause the message to not be delivered to any destination and Transacted Session commits to fail. The allowed values and their meaning are:  <pre> \"always\" - Always return a negative acknowledgment (NACK) to the sending client on message discard. \"when-queue-enabled\" - Only return a negative acknowledgment (NACK) to the sending client on message discard when the Queue is enabled. \"never\" - Never return a negative acknowledgment (NACK) to the sending client on message discard. </pre>  Available since 2.14.")
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
   * Indicates whether the time-to-live (TTL) for messages in the MQTT Session Queue is respected. When enabled, expired messages are discarded or moved to the DMQ. Available since 2.14.
   * @return queueRespectTtlEnabled
  **/
  @ApiModelProperty(value = "Indicates whether the time-to-live (TTL) for messages in the MQTT Session Queue is respected. When enabled, expired messages are discarded or moved to the DMQ. Available since 2.14.")
  public Boolean isQueueRespectTtlEnabled() {
    return queueRespectTtlEnabled;
  }

  public void setQueueRespectTtlEnabled(Boolean queueRespectTtlEnabled) {
    this.queueRespectTtlEnabled = queueRespectTtlEnabled;
  }

  public MsgVpnMqttSession will(Boolean will) {
    this.will = will;
    return this;
  }

   /**
   * Indicates whether the MQTT Session has the Will message specified by the Client. The Will message is published if the Client disconnects without sending the MQTT DISCONNECT packet.
   * @return will
  **/
  @ApiModelProperty(value = "Indicates whether the MQTT Session has the Will message specified by the Client. The Will message is published if the Client disconnects without sending the MQTT DISCONNECT packet.")
  public Boolean isWill() {
    return will;
  }

  public void setWill(Boolean will) {
    this.will = will;
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
    return Objects.equals(this.clean, msgVpnMqttSession.clean) &&
        Objects.equals(this.clientName, msgVpnMqttSession.clientName) &&
        Objects.equals(this.counter, msgVpnMqttSession.counter) &&
        Objects.equals(this.createdByManagement, msgVpnMqttSession.createdByManagement) &&
        Objects.equals(this.enabled, msgVpnMqttSession.enabled) &&
        Objects.equals(this.mqttConnackErrorTxCount, msgVpnMqttSession.mqttConnackErrorTxCount) &&
        Objects.equals(this.mqttConnackTxCount, msgVpnMqttSession.mqttConnackTxCount) &&
        Objects.equals(this.mqttConnectRxCount, msgVpnMqttSession.mqttConnectRxCount) &&
        Objects.equals(this.mqttDisconnectRxCount, msgVpnMqttSession.mqttDisconnectRxCount) &&
        Objects.equals(this.mqttPubcompTxCount, msgVpnMqttSession.mqttPubcompTxCount) &&
        Objects.equals(this.mqttPublishQos0RxCount, msgVpnMqttSession.mqttPublishQos0RxCount) &&
        Objects.equals(this.mqttPublishQos0TxCount, msgVpnMqttSession.mqttPublishQos0TxCount) &&
        Objects.equals(this.mqttPublishQos1RxCount, msgVpnMqttSession.mqttPublishQos1RxCount) &&
        Objects.equals(this.mqttPublishQos1TxCount, msgVpnMqttSession.mqttPublishQos1TxCount) &&
        Objects.equals(this.mqttPublishQos2RxCount, msgVpnMqttSession.mqttPublishQos2RxCount) &&
        Objects.equals(this.mqttPubrecTxCount, msgVpnMqttSession.mqttPubrecTxCount) &&
        Objects.equals(this.mqttPubrelRxCount, msgVpnMqttSession.mqttPubrelRxCount) &&
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
        Objects.equals(this.queueName, msgVpnMqttSession.queueName) &&
        Objects.equals(this.queueRejectLowPriorityMsgEnabled, msgVpnMqttSession.queueRejectLowPriorityMsgEnabled) &&
        Objects.equals(this.queueRejectLowPriorityMsgLimit, msgVpnMqttSession.queueRejectLowPriorityMsgLimit) &&
        Objects.equals(this.queueRejectMsgToSenderOnDiscardBehavior, msgVpnMqttSession.queueRejectMsgToSenderOnDiscardBehavior) &&
        Objects.equals(this.queueRespectTtlEnabled, msgVpnMqttSession.queueRespectTtlEnabled) &&
        Objects.equals(this.will, msgVpnMqttSession.will);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clean, clientName, counter, createdByManagement, enabled, mqttConnackErrorTxCount, mqttConnackTxCount, mqttConnectRxCount, mqttDisconnectRxCount, mqttPubcompTxCount, mqttPublishQos0RxCount, mqttPublishQos0TxCount, mqttPublishQos1RxCount, mqttPublishQos1TxCount, mqttPublishQos2RxCount, mqttPubrecTxCount, mqttPubrelRxCount, mqttSessionClientId, mqttSessionVirtualRouter, msgVpnName, owner, queueConsumerAckPropagationEnabled, queueDeadMsgQueue, queueEventBindCountThreshold, queueEventMsgSpoolUsageThreshold, queueEventRejectLowPriorityMsgLimitThreshold, queueMaxBindCount, queueMaxDeliveredUnackedMsgsPerFlow, queueMaxMsgSize, queueMaxMsgSpoolUsage, queueMaxRedeliveryCount, queueMaxTtl, queueName, queueRejectLowPriorityMsgEnabled, queueRejectLowPriorityMsgLimit, queueRejectMsgToSenderOnDiscardBehavior, queueRespectTtlEnabled, will);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnMqttSession {\n");
    
    sb.append("    clean: ").append(toIndentedString(clean)).append("\n");
    sb.append("    clientName: ").append(toIndentedString(clientName)).append("\n");
    sb.append("    counter: ").append(toIndentedString(counter)).append("\n");
    sb.append("    createdByManagement: ").append(toIndentedString(createdByManagement)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    mqttConnackErrorTxCount: ").append(toIndentedString(mqttConnackErrorTxCount)).append("\n");
    sb.append("    mqttConnackTxCount: ").append(toIndentedString(mqttConnackTxCount)).append("\n");
    sb.append("    mqttConnectRxCount: ").append(toIndentedString(mqttConnectRxCount)).append("\n");
    sb.append("    mqttDisconnectRxCount: ").append(toIndentedString(mqttDisconnectRxCount)).append("\n");
    sb.append("    mqttPubcompTxCount: ").append(toIndentedString(mqttPubcompTxCount)).append("\n");
    sb.append("    mqttPublishQos0RxCount: ").append(toIndentedString(mqttPublishQos0RxCount)).append("\n");
    sb.append("    mqttPublishQos0TxCount: ").append(toIndentedString(mqttPublishQos0TxCount)).append("\n");
    sb.append("    mqttPublishQos1RxCount: ").append(toIndentedString(mqttPublishQos1RxCount)).append("\n");
    sb.append("    mqttPublishQos1TxCount: ").append(toIndentedString(mqttPublishQos1TxCount)).append("\n");
    sb.append("    mqttPublishQos2RxCount: ").append(toIndentedString(mqttPublishQos2RxCount)).append("\n");
    sb.append("    mqttPubrecTxCount: ").append(toIndentedString(mqttPubrecTxCount)).append("\n");
    sb.append("    mqttPubrelRxCount: ").append(toIndentedString(mqttPubrelRxCount)).append("\n");
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
    sb.append("    queueName: ").append(toIndentedString(queueName)).append("\n");
    sb.append("    queueRejectLowPriorityMsgEnabled: ").append(toIndentedString(queueRejectLowPriorityMsgEnabled)).append("\n");
    sb.append("    queueRejectLowPriorityMsgLimit: ").append(toIndentedString(queueRejectLowPriorityMsgLimit)).append("\n");
    sb.append("    queueRejectMsgToSenderOnDiscardBehavior: ").append(toIndentedString(queueRejectMsgToSenderOnDiscardBehavior)).append("\n");
    sb.append("    queueRespectTtlEnabled: ").append(toIndentedString(queueRespectTtlEnabled)).append("\n");
    sb.append("    will: ").append(toIndentedString(will)).append("\n");
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

