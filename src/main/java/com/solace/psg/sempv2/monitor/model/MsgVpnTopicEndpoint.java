/*
 * SEMP (Solace Element Management Protocol)
 * SEMP (starting in `v2`, see note 1) is a RESTful API for configuring, monitoring, and administering a Solace PubSub+ broker.  SEMP uses URIs to address manageable **resources** of the Solace PubSub+ broker. Resources are individual **objects**, **collections** of objects, or (exclusively in the action API) **actions**. This document applies to the following API:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Monitoring|/SEMP/v2/monitor|Querying operational parameters|See note 2    The following APIs are also available:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Action|/SEMP/v2/action|Performing actions|See note 2 Configuration|/SEMP/v2/config|Reading and writing config state|See note 2    Resources are always nouns, with individual objects being singular and collections being plural.  Objects within a collection are identified by an `obj-id`, which follows the collection name with the form `collection-name/obj-id`.  Actions within an object are identified by an `action-id`, which follows the object name with the form `obj-id/action-id`.  Some examples:  ``` /SEMP/v2/config/msgVpns                        ; MsgVpn collection /SEMP/v2/config/msgVpns/a                      ; MsgVpn object named \"a\" /SEMP/v2/config/msgVpns/a/queues               ; Queue collection in MsgVpn \"a\" /SEMP/v2/config/msgVpns/a/queues/b             ; Queue object named \"b\" in MsgVpn \"a\" /SEMP/v2/action/msgVpns/a/queues/b/startReplay ; Action that starts a replay on Queue \"b\" in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients             ; Client collection in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients/c           ; Client object named \"c\" in MsgVpn \"a\" ```  ## Collection Resources  Collections are unordered lists of objects (unless described as otherwise), and are described by JSON arrays. Each item in the array represents an object in the same manner as the individual object would normally be represented. In the configuration API, the creation of a new object is done through its collection resource.  ## Object and Action Resources  Objects are composed of attributes, actions, collections, and other objects. They are described by JSON objects as name/value pairs. The collections and actions of an object are not contained directly in the object's JSON content; rather the content includes an attribute containing a URI which points to the collections and actions. These contained resources must be managed through this URI. At a minimum, every object has one or more identifying attributes, and its own `uri` attribute which contains the URI pointing to itself.  Actions are also composed of attributes, and are described by JSON objects as name/value pairs. Unlike objects, however, they are not members of a collection and cannot be retrieved, only performed. Actions only exist in the action API.  Attributes in an object or action may have any combination of the following properties:   Property|Meaning|Comments :---|:---|:--- Identifying|Attribute is involved in unique identification of the object, and appears in its URI| Required|Attribute must be provided in the request| Read-Only|Attribute can only be read, not written.|See note 3 Write-Only|Attribute can only be written, not read, unless the attribute is also opaque|See the documentation for the opaque property Requires-Disable|Attribute can only be changed when object is disabled| Deprecated|Attribute is deprecated, and will disappear in the next SEMP version| Opaque|Attribute can be set or retrieved in opaque form when the `opaquePassword` query parameter is present|See the `opaquePassword` query parameter documentation    In some requests, certain attributes may only be provided in certain combinations with other attributes:   Relationship|Meaning :---|:--- Requires|Attribute may only be changed by a request if a particular attribute or combination of attributes is also provided in the request Conflicts|Attribute may only be provided in a request if a particular attribute or combination of attributes is not also provided in the request    In the monitoring API, any non-identifying attribute may not be returned in a GET.  ## HTTP Methods  The following HTTP methods manipulate resources in accordance with these general principles. Note that some methods are only used in certain APIs:   Method|Resource|Meaning|Request Body|Response Body|Missing Request Attributes :---|:---|:---|:---|:---|:--- POST|Collection|Create object|Initial attribute values|Object attributes and metadata|Set to default PUT|Object|Create or replace object (see note 5)|New attribute values|Object attributes and metadata|Set to default, with certain exceptions (see note 4) PUT|Action|Performs action|Action arguments|Action metadata|N/A PATCH|Object|Update object|New attribute values|Object attributes and metadata|unchanged DELETE|Object|Delete object|Empty|Object metadata|N/A GET|Object|Get object|Empty|Object attributes and metadata|N/A GET|Collection|Get collection|Empty|Object attributes and collection metadata|N/A    ## Common Query Parameters  The following are some common query parameters that are supported by many method/URI combinations. Individual URIs may document additional parameters. Note that multiple query parameters can be used together in a single URI, separated by the ampersand character. For example:  ``` ; Request for the MsgVpns collection using two hypothetical query parameters ; \"q1\" and \"q2\" with values \"val1\" and \"val2\" respectively /SEMP/v2/monitor/msgVpns?q1=val1&q2=val2 ```  ### select  Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. Use this query parameter to limit the size of the returned data for each returned object, return only those fields that are desired, or exclude fields that are not desired.  The value of `select` is a comma-separated list of attribute names. If the list contains attribute names that are not prefaced by `-`, only those attributes are included in the response. If the list contains attribute names that are prefaced by `-`, those attributes are excluded from the response. If the list contains both types, then the difference of the first set of attributes and the second set of attributes is returned. If the list is empty (i.e. `select=`), no attributes are returned.  All attributes that are prefaced by `-` must follow all attributes that are not prefaced by `-`. In addition, each attribute name in the list must match at least one attribute in the object.  Names may include the `*` wildcard (zero or more characters). Nested attribute names are supported using periods (e.g. `parentName.childName`).  Some examples:  ``` ; List of all MsgVpn names /SEMP/v2/monitor/msgVpns?select=msgVpnName ; List of all MsgVpn and their attributes except for their names /SEMP/v2/monitor/msgVpns?select=-msgVpnName ; Authentication attributes of MsgVpn \"finance\" /SEMP/v2/monitor/msgVpns/finance?select=authentication* ; All attributes of MsgVpn \"finance\" except for authentication attributes /SEMP/v2/monitor/msgVpns/finance?select=-authentication* ; Access related attributes of Queue \"orderQ\" of MsgVpn \"finance\" /SEMP/v2/monitor/msgVpns/finance/queues/orderQ?select=owner,permission ```  ### where  Include in the response only objects where certain conditions are true. Use this query parameter to limit which objects are returned to those whose attribute values meet the given conditions.  The value of `where` is a comma-separated list of expressions. All expressions must be true for the object to be included in the response. Each expression takes the form:  ``` expression  = attribute-name OP value OP          = '==' | '!=' | '&lt;' | '&gt;' | '&lt;=' | '&gt;=' ```  `value` may be a number, string, `true`, or `false`, as appropriate for the type of `attribute-name`. Greater-than and less-than comparisons only work for numbers. A `*` in a string `value` is interpreted as a wildcard (zero or more characters). Some examples:  ``` ; Only enabled MsgVpns /SEMP/v2/monitor/msgVpns?where=enabled==true ; Only MsgVpns using basic non-LDAP authentication /SEMP/v2/monitor/msgVpns?where=authenticationBasicEnabled==true,authenticationBasicType!=ldap ; Only MsgVpns that allow more than 100 client connections /SEMP/v2/monitor/msgVpns?where=maxConnectionCount>100 ; Only MsgVpns with msgVpnName starting with \"B\": /SEMP/v2/monitor/msgVpns?where=msgVpnName==B* ```  ### count  Limit the count of objects in the response. This can be useful to limit the size of the response for large collections. The minimum value for `count` is `1` and the default is `10`. There is also a per-collection maximum value to limit request handling time. For example:  ``` ; Up to 25 MsgVpns /SEMP/v2/monitor/msgVpns?count=25 ```  ### cursor  The cursor, or position, for the next page of objects. Cursors are opaque data that should not be created or interpreted by SEMP clients, and should only be used as described below.  When a request is made for a collection and there may be additional objects available for retrieval that are not included in the initial response, the response will include a `cursorQuery` field containing a cursor. The value of this field can be specified in the `cursor` query parameter of a subsequent request to retrieve the next page of objects. For convenience, an appropriate URI is constructed automatically by the broker and included in the `nextPageUri` field of the response. This URI can be used directly to retrieve the next page of objects.  ### opaquePassword  Attributes with the opaque property are also write-only and so cannot normally be retrieved in a GET. However, when a password is provided in the `opaquePassword` query parameter, attributes with the opaque property are retrieved in a GET in opaque form, encrypted with this password. The query parameter can also be used on a POST, PATCH, or PUT to set opaque attributes using opaque attribute values retrieved in a GET, so long as:  1. the same password that was used to retrieve the opaque attribute values is provided; and  2. the broker to which the request is being sent has the same major and minor SEMP version as the broker that produced the opaque attribute values.  The password provided in the query parameter must be a minimum of 8 characters and a maximum of 128 characters.  The query parameter can only be used in the configuration API, and only over HTTPS.  ## Help  Visit [our website](https://solace.com) to learn more about Solace.  You can also download the SEMP API specifications by clicking [here](https://solace.com/downloads/).  If you need additional support, please contact us at [support@solace.com](mailto:support@solace.com).  ## Notes  Note|Description :---:|:--- 1|This specification defines SEMP starting in \"v2\", and not the original SEMP \"v1\" interface. Request and response formats between \"v1\" and \"v2\" are entirely incompatible, although both protocols share a common port configuration on the Solace PubSub+ broker. They are differentiated by the initial portion of the URI path, one of either \"/SEMP/\" or \"/SEMP/v2/\" 2|This API is partially implemented. Only a subset of all objects are available. 3|Read-only attributes may appear in POST and PUT/PATCH requests. However, if a read-only attribute is not marked as identifying, it will be ignored during a PUT/PATCH. 4|On a PUT, if the SEMP user is not authorized to modify the attribute, its value is left unchanged rather than set to default. In addition, the values of write-only attributes are not set to their defaults on a PUT, except in the following two cases: there is a mutual requires relationship with another non-write-only attribute and both attributes are absent from the request; or the attribute is also opaque and the `opaquePassword` query parameter is provided in the request. 5|On a PUT, if the object does not exist, it is created first.  
 *
 * OpenAPI spec version: 2.17
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
import com.solace.psg.sempv2.monitor.model.EventThreshold;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * MsgVpnTopicEndpoint
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:57:21.016551900+01:00[Europe/London]")
public class MsgVpnTopicEndpoint {
  /**
   * The access type for delivering messages to consumer flows bound to the Topic Endpoint. The allowed values and their meaning are:  &lt;pre&gt; \&quot;exclusive\&quot; - Exclusive delivery of messages to the first bound consumer flow. \&quot;non-exclusive\&quot; - Non-exclusive delivery of messages to all bound consumer flows in a round-robin fashion. &lt;/pre&gt; 
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
        Object value = jsonReader.nextString();
        return AccessTypeEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("accessType")
  private AccessTypeEnum accessType = null;

  @SerializedName("alreadyBoundBindFailureCount")
  private Long alreadyBoundBindFailureCount = null;

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

  @SerializedName("bindTimeForwardingMode")
  private String bindTimeForwardingMode = null;

  @SerializedName("clientProfileDeniedDiscardedMsgCount")
  private Long clientProfileDeniedDiscardedMsgCount = null;

  @SerializedName("consumerAckPropagationEnabled")
  private Boolean consumerAckPropagationEnabled = null;

  @SerializedName("createdByManagement")
  private Boolean createdByManagement = null;

  @SerializedName("deadMsgQueue")
  private String deadMsgQueue = null;

  @SerializedName("deletedMsgCount")
  private Long deletedMsgCount = null;

  @SerializedName("destinationGroupErrorDiscardedMsgCount")
  private Long destinationGroupErrorDiscardedMsgCount = null;

  @SerializedName("destinationTopic")
  private String destinationTopic = null;

  @SerializedName("disabledBindFailureCount")
  private Long disabledBindFailureCount = null;

  @SerializedName("disabledDiscardedMsgCount")
  private Long disabledDiscardedMsgCount = null;

  @SerializedName("durable")
  private Boolean durable = null;

  @SerializedName("egressEnabled")
  private Boolean egressEnabled = null;

  @SerializedName("eventBindCountThreshold")
  private EventThreshold eventBindCountThreshold = null;

  @SerializedName("eventRejectLowPriorityMsgLimitThreshold")
  private EventThreshold eventRejectLowPriorityMsgLimitThreshold = null;

  @SerializedName("eventSpoolUsageThreshold")
  private EventThreshold eventSpoolUsageThreshold = null;

  @SerializedName("highestAckedMsgId")
  private Long highestAckedMsgId = null;

  @SerializedName("highestMsgId")
  private Long highestMsgId = null;

  @SerializedName("inProgressAckMsgCount")
  private Long inProgressAckMsgCount = null;

  @SerializedName("ingressEnabled")
  private Boolean ingressEnabled = null;

  @SerializedName("invalidSelectorBindFailureCount")
  private Long invalidSelectorBindFailureCount = null;

  @SerializedName("lastReplayCompleteTime")
  private Integer lastReplayCompleteTime = null;

  @SerializedName("lastReplayFailureReason")
  private String lastReplayFailureReason = null;

  @SerializedName("lastReplayFailureTime")
  private Integer lastReplayFailureTime = null;

  @SerializedName("lastReplayStartTime")
  private Integer lastReplayStartTime = null;

  @SerializedName("lastReplayedMsgTxTime")
  private Integer lastReplayedMsgTxTime = null;

  @SerializedName("lastSelectorExaminedMsgId")
  private Long lastSelectorExaminedMsgId = null;

  @SerializedName("lastSpooledMsgId")
  private Long lastSpooledMsgId = null;

  @SerializedName("lowPriorityMsgCongestionDiscardedMsgCount")
  private Long lowPriorityMsgCongestionDiscardedMsgCount = null;

  @SerializedName("lowPriorityMsgCongestionState")
  private String lowPriorityMsgCongestionState = null;

  @SerializedName("lowestAckedMsgId")
  private Long lowestAckedMsgId = null;

  @SerializedName("lowestMsgId")
  private Long lowestMsgId = null;

  @SerializedName("maxBindCount")
  private Long maxBindCount = null;

  @SerializedName("maxBindCountExceededBindFailureCount")
  private Long maxBindCountExceededBindFailureCount = null;

  @SerializedName("maxDeliveredUnackedMsgsPerFlow")
  private Long maxDeliveredUnackedMsgsPerFlow = null;

  @SerializedName("maxEffectiveBindCount")
  private Integer maxEffectiveBindCount = null;

  @SerializedName("maxMsgSize")
  private Integer maxMsgSize = null;

  @SerializedName("maxMsgSizeExceededDiscardedMsgCount")
  private Long maxMsgSizeExceededDiscardedMsgCount = null;

  @SerializedName("maxMsgSpoolUsageExceededDiscardedMsgCount")
  private Long maxMsgSpoolUsageExceededDiscardedMsgCount = null;

  @SerializedName("maxRedeliveryCount")
  private Long maxRedeliveryCount = null;

  @SerializedName("maxRedeliveryExceededDiscardedMsgCount")
  private Long maxRedeliveryExceededDiscardedMsgCount = null;

  @SerializedName("maxRedeliveryExceededToDmqFailedMsgCount")
  private Long maxRedeliveryExceededToDmqFailedMsgCount = null;

  @SerializedName("maxRedeliveryExceededToDmqMsgCount")
  private Long maxRedeliveryExceededToDmqMsgCount = null;

  @SerializedName("maxSpoolUsage")
  private Long maxSpoolUsage = null;

  @SerializedName("maxTtl")
  private Long maxTtl = null;

  @SerializedName("maxTtlExceededDiscardedMsgCount")
  private Long maxTtlExceededDiscardedMsgCount = null;

  @SerializedName("maxTtlExpiredDiscardedMsgCount")
  private Long maxTtlExpiredDiscardedMsgCount = null;

  @SerializedName("maxTtlExpiredToDmqFailedMsgCount")
  private Long maxTtlExpiredToDmqFailedMsgCount = null;

  @SerializedName("maxTtlExpiredToDmqMsgCount")
  private Long maxTtlExpiredToDmqMsgCount = null;

  @SerializedName("msgSpoolPeakUsage")
  private Long msgSpoolPeakUsage = null;

  @SerializedName("msgSpoolUsage")
  private Long msgSpoolUsage = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("networkTopic")
  private String networkTopic = null;

  @SerializedName("noLocalDeliveryDiscardedMsgCount")
  private Long noLocalDeliveryDiscardedMsgCount = null;

  @SerializedName("otherBindFailureCount")
  private Long otherBindFailureCount = null;

  @SerializedName("owner")
  private String owner = null;

  /**
   * The permission level for all consumers of the Topic Endpoint, excluding the owner. The allowed values and their meaning are:  &lt;pre&gt; \&quot;no-access\&quot; - Disallows all access. \&quot;read-only\&quot; - Read-only access to the messages. \&quot;consume\&quot; - Consume (read and remove) messages. \&quot;modify-topic\&quot; - Consume messages or modify the topic/selector. \&quot;delete\&quot; - Consume messages, modify the topic/selector or delete the Client created endpoint altogether. &lt;/pre&gt; 
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
        Object value = jsonReader.nextString();
        return PermissionEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("permission")
  private PermissionEnum permission = null;

  @SerializedName("redeliveredMsgCount")
  private Long redeliveredMsgCount = null;

  @SerializedName("rejectLowPriorityMsgEnabled")
  private Boolean rejectLowPriorityMsgEnabled = null;

  @SerializedName("rejectLowPriorityMsgLimit")
  private Long rejectLowPriorityMsgLimit = null;

  /**
   * Determines when to return negative acknowledgements (NACKs) to sending clients on message discards. Note that NACKs cause the message to not be delivered to any destination and Transacted Session commits to fail. The allowed values and their meaning are:  &lt;pre&gt; \&quot;always\&quot; - Always return a negative acknowledgment (NACK) to the sending client on message discard. \&quot;when-topic-endpoint-enabled\&quot; - Only return a negative acknowledgment (NACK) to the sending client on message discard when the Topic Endpoint is enabled. \&quot;never\&quot; - Never return a negative acknowledgment (NACK) to the sending client on message discard. &lt;/pre&gt; 
   */
  @JsonAdapter(RejectMsgToSenderOnDiscardBehaviorEnum.Adapter.class)
  public enum RejectMsgToSenderOnDiscardBehaviorEnum {
    ALWAYS("always"),
    WHEN_TOPIC_ENDPOINT_ENABLED("when-topic-endpoint-enabled"),
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
        Object value = jsonReader.nextString();
        return RejectMsgToSenderOnDiscardBehaviorEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("rejectMsgToSenderOnDiscardBehavior")
  private RejectMsgToSenderOnDiscardBehaviorEnum rejectMsgToSenderOnDiscardBehavior = null;

  @SerializedName("replayFailureCount")
  private Long replayFailureCount = null;

  @SerializedName("replayStartCount")
  private Long replayStartCount = null;

  @SerializedName("replayState")
  private String replayState = null;

  @SerializedName("replaySuccessCount")
  private Long replaySuccessCount = null;

  @SerializedName("replayedAckedMsgCount")
  private Long replayedAckedMsgCount = null;

  @SerializedName("replayedTxMsgCount")
  private Long replayedTxMsgCount = null;

  @SerializedName("replicationActiveAckPropTxMsgCount")
  private Long replicationActiveAckPropTxMsgCount = null;

  @SerializedName("replicationStandbyAckPropRxMsgCount")
  private Long replicationStandbyAckPropRxMsgCount = null;

  @SerializedName("replicationStandbyAckedByAckPropMsgCount")
  private Long replicationStandbyAckedByAckPropMsgCount = null;

  @SerializedName("replicationStandbyRxMsgCount")
  private Long replicationStandbyRxMsgCount = null;

  @SerializedName("respectMsgPriorityEnabled")
  private Boolean respectMsgPriorityEnabled = null;

  @SerializedName("respectTtlEnabled")
  private Boolean respectTtlEnabled = null;

  @SerializedName("rxByteRate")
  private Integer rxByteRate = null;

  @SerializedName("rxMsgRate")
  private Long rxMsgRate = null;

  @SerializedName("rxSelector")
  private Boolean rxSelector = null;

  @SerializedName("selector")
  private String selector = null;

  @SerializedName("selectorExaminedMsgCount")
  private Long selectorExaminedMsgCount = null;

  @SerializedName("selectorMatchedMsgCount")
  private Long selectorMatchedMsgCount = null;

  @SerializedName("selectorNotMatchedMsgCount")
  private Long selectorNotMatchedMsgCount = null;

  @SerializedName("spooledByteCount")
  private Long spooledByteCount = null;

  @SerializedName("spooledMsgCount")
  private Long spooledMsgCount = null;

  @SerializedName("topicEndpointName")
  private String topicEndpointName = null;

  @SerializedName("txByteRate")
  private Long txByteRate = null;

  @SerializedName("txMsgRate")
  private Long txMsgRate = null;

  @SerializedName("txUnackedMsgCount")
  private Long txUnackedMsgCount = null;

  @SerializedName("virtualRouter")
  private String virtualRouter = null;

  public MsgVpnTopicEndpoint accessType(AccessTypeEnum accessType) {
    this.accessType = accessType;
    return this;
  }

   /**
   * The access type for delivering messages to consumer flows bound to the Topic Endpoint. The allowed values and their meaning are:  &lt;pre&gt; \&quot;exclusive\&quot; - Exclusive delivery of messages to the first bound consumer flow. \&quot;non-exclusive\&quot; - Non-exclusive delivery of messages to all bound consumer flows in a round-robin fashion. &lt;/pre&gt; 
   * @return accessType
  **/
  @Schema(description = "The access type for delivering messages to consumer flows bound to the Topic Endpoint. The allowed values and their meaning are:  <pre> \"exclusive\" - Exclusive delivery of messages to the first bound consumer flow. \"non-exclusive\" - Non-exclusive delivery of messages to all bound consumer flows in a round-robin fashion. </pre> ")
  public AccessTypeEnum getAccessType() {
    return accessType;
  }

  public void setAccessType(AccessTypeEnum accessType) {
    this.accessType = accessType;
  }

  public MsgVpnTopicEndpoint alreadyBoundBindFailureCount(Long alreadyBoundBindFailureCount) {
    this.alreadyBoundBindFailureCount = alreadyBoundBindFailureCount;
    return this;
  }

   /**
   * The number of Topic Endpoint bind failures due to being already bound.
   * @return alreadyBoundBindFailureCount
  **/
  @Schema(description = "The number of Topic Endpoint bind failures due to being already bound.")
  public Long getAlreadyBoundBindFailureCount() {
    return alreadyBoundBindFailureCount;
  }

  public void setAlreadyBoundBindFailureCount(Long alreadyBoundBindFailureCount) {
    this.alreadyBoundBindFailureCount = alreadyBoundBindFailureCount;
  }

  public MsgVpnTopicEndpoint averageRxByteRate(Long averageRxByteRate) {
    this.averageRxByteRate = averageRxByteRate;
    return this;
  }

   /**
   * The one minute average of the message rate received by the Topic Endpoint, in bytes per second (B/sec).
   * @return averageRxByteRate
  **/
  @Schema(description = "The one minute average of the message rate received by the Topic Endpoint, in bytes per second (B/sec).")
  public Long getAverageRxByteRate() {
    return averageRxByteRate;
  }

  public void setAverageRxByteRate(Long averageRxByteRate) {
    this.averageRxByteRate = averageRxByteRate;
  }

  public MsgVpnTopicEndpoint averageRxMsgRate(Long averageRxMsgRate) {
    this.averageRxMsgRate = averageRxMsgRate;
    return this;
  }

   /**
   * The one minute average of the message rate received by the Topic Endpoint, in messages per second (msg/sec).
   * @return averageRxMsgRate
  **/
  @Schema(description = "The one minute average of the message rate received by the Topic Endpoint, in messages per second (msg/sec).")
  public Long getAverageRxMsgRate() {
    return averageRxMsgRate;
  }

  public void setAverageRxMsgRate(Long averageRxMsgRate) {
    this.averageRxMsgRate = averageRxMsgRate;
  }

  public MsgVpnTopicEndpoint averageTxByteRate(Long averageTxByteRate) {
    this.averageTxByteRate = averageTxByteRate;
    return this;
  }

   /**
   * The one minute average of the message rate transmitted by the Topic Endpoint, in bytes per second (B/sec).
   * @return averageTxByteRate
  **/
  @Schema(description = "The one minute average of the message rate transmitted by the Topic Endpoint, in bytes per second (B/sec).")
  public Long getAverageTxByteRate() {
    return averageTxByteRate;
  }

  public void setAverageTxByteRate(Long averageTxByteRate) {
    this.averageTxByteRate = averageTxByteRate;
  }

  public MsgVpnTopicEndpoint averageTxMsgRate(Long averageTxMsgRate) {
    this.averageTxMsgRate = averageTxMsgRate;
    return this;
  }

   /**
   * The one minute average of the message rate transmitted by the Topic Endpoint, in messages per second (msg/sec).
   * @return averageTxMsgRate
  **/
  @Schema(description = "The one minute average of the message rate transmitted by the Topic Endpoint, in messages per second (msg/sec).")
  public Long getAverageTxMsgRate() {
    return averageTxMsgRate;
  }

  public void setAverageTxMsgRate(Long averageTxMsgRate) {
    this.averageTxMsgRate = averageTxMsgRate;
  }

  public MsgVpnTopicEndpoint bindRequestCount(Long bindRequestCount) {
    this.bindRequestCount = bindRequestCount;
    return this;
  }

   /**
   * The number of consumer requests to bind to the Topic Endpoint.
   * @return bindRequestCount
  **/
  @Schema(description = "The number of consumer requests to bind to the Topic Endpoint.")
  public Long getBindRequestCount() {
    return bindRequestCount;
  }

  public void setBindRequestCount(Long bindRequestCount) {
    this.bindRequestCount = bindRequestCount;
  }

  public MsgVpnTopicEndpoint bindSuccessCount(Long bindSuccessCount) {
    this.bindSuccessCount = bindSuccessCount;
    return this;
  }

   /**
   * The number of successful consumer requests to bind to the Topic Endpoint.
   * @return bindSuccessCount
  **/
  @Schema(description = "The number of successful consumer requests to bind to the Topic Endpoint.")
  public Long getBindSuccessCount() {
    return bindSuccessCount;
  }

  public void setBindSuccessCount(Long bindSuccessCount) {
    this.bindSuccessCount = bindSuccessCount;
  }

  public MsgVpnTopicEndpoint bindTimeForwardingMode(String bindTimeForwardingMode) {
    this.bindTimeForwardingMode = bindTimeForwardingMode;
    return this;
  }

   /**
   * The forwarding mode of the Topic Endpoint at bind time. The allowed values and their meaning are:  &lt;pre&gt; \&quot;store-and-forward\&quot; - Deliver messages using the guaranteed data path. \&quot;cut-through\&quot; - Deliver messages using the direct and guaranteed data paths for lower latency. &lt;/pre&gt; 
   * @return bindTimeForwardingMode
  **/
  @Schema(description = "The forwarding mode of the Topic Endpoint at bind time. The allowed values and their meaning are:  <pre> \"store-and-forward\" - Deliver messages using the guaranteed data path. \"cut-through\" - Deliver messages using the direct and guaranteed data paths for lower latency. </pre> ")
  public String getBindTimeForwardingMode() {
    return bindTimeForwardingMode;
  }

  public void setBindTimeForwardingMode(String bindTimeForwardingMode) {
    this.bindTimeForwardingMode = bindTimeForwardingMode;
  }

  public MsgVpnTopicEndpoint clientProfileDeniedDiscardedMsgCount(Long clientProfileDeniedDiscardedMsgCount) {
    this.clientProfileDeniedDiscardedMsgCount = clientProfileDeniedDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages discarded by the Topic Endpoint due to being denied by the Client Profile.
   * @return clientProfileDeniedDiscardedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages discarded by the Topic Endpoint due to being denied by the Client Profile.")
  public Long getClientProfileDeniedDiscardedMsgCount() {
    return clientProfileDeniedDiscardedMsgCount;
  }

  public void setClientProfileDeniedDiscardedMsgCount(Long clientProfileDeniedDiscardedMsgCount) {
    this.clientProfileDeniedDiscardedMsgCount = clientProfileDeniedDiscardedMsgCount;
  }

  public MsgVpnTopicEndpoint consumerAckPropagationEnabled(Boolean consumerAckPropagationEnabled) {
    this.consumerAckPropagationEnabled = consumerAckPropagationEnabled;
    return this;
  }

   /**
   * Indicates whether the propagation of consumer acknowledgements (ACKs) received on the active replication Message VPN to the standby replication Message VPN is enabled.
   * @return consumerAckPropagationEnabled
  **/
  @Schema(description = "Indicates whether the propagation of consumer acknowledgements (ACKs) received on the active replication Message VPN to the standby replication Message VPN is enabled.")
  public Boolean isConsumerAckPropagationEnabled() {
    return consumerAckPropagationEnabled;
  }

  public void setConsumerAckPropagationEnabled(Boolean consumerAckPropagationEnabled) {
    this.consumerAckPropagationEnabled = consumerAckPropagationEnabled;
  }

  public MsgVpnTopicEndpoint createdByManagement(Boolean createdByManagement) {
    this.createdByManagement = createdByManagement;
    return this;
  }

   /**
   * Indicates whether the Topic Endpoint was created by a management API (CLI or SEMP).
   * @return createdByManagement
  **/
  @Schema(description = "Indicates whether the Topic Endpoint was created by a management API (CLI or SEMP).")
  public Boolean isCreatedByManagement() {
    return createdByManagement;
  }

  public void setCreatedByManagement(Boolean createdByManagement) {
    this.createdByManagement = createdByManagement;
  }

  public MsgVpnTopicEndpoint deadMsgQueue(String deadMsgQueue) {
    this.deadMsgQueue = deadMsgQueue;
    return this;
  }

   /**
   * The name of the Dead Message Queue (DMQ) used by the Topic Endpoint.
   * @return deadMsgQueue
  **/
  @Schema(description = "The name of the Dead Message Queue (DMQ) used by the Topic Endpoint.")
  public String getDeadMsgQueue() {
    return deadMsgQueue;
  }

  public void setDeadMsgQueue(String deadMsgQueue) {
    this.deadMsgQueue = deadMsgQueue;
  }

  public MsgVpnTopicEndpoint deletedMsgCount(Long deletedMsgCount) {
    this.deletedMsgCount = deletedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages deleted from the Topic Endpoint.
   * @return deletedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages deleted from the Topic Endpoint.")
  public Long getDeletedMsgCount() {
    return deletedMsgCount;
  }

  public void setDeletedMsgCount(Long deletedMsgCount) {
    this.deletedMsgCount = deletedMsgCount;
  }

  public MsgVpnTopicEndpoint destinationGroupErrorDiscardedMsgCount(Long destinationGroupErrorDiscardedMsgCount) {
    this.destinationGroupErrorDiscardedMsgCount = destinationGroupErrorDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages discarded by the Topic Endpoint due to a destination group error.
   * @return destinationGroupErrorDiscardedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages discarded by the Topic Endpoint due to a destination group error.")
  public Long getDestinationGroupErrorDiscardedMsgCount() {
    return destinationGroupErrorDiscardedMsgCount;
  }

  public void setDestinationGroupErrorDiscardedMsgCount(Long destinationGroupErrorDiscardedMsgCount) {
    this.destinationGroupErrorDiscardedMsgCount = destinationGroupErrorDiscardedMsgCount;
  }

  public MsgVpnTopicEndpoint destinationTopic(String destinationTopic) {
    this.destinationTopic = destinationTopic;
    return this;
  }

   /**
   * The destination topic of the Topic Endpoint.
   * @return destinationTopic
  **/
  @Schema(description = "The destination topic of the Topic Endpoint.")
  public String getDestinationTopic() {
    return destinationTopic;
  }

  public void setDestinationTopic(String destinationTopic) {
    this.destinationTopic = destinationTopic;
  }

  public MsgVpnTopicEndpoint disabledBindFailureCount(Long disabledBindFailureCount) {
    this.disabledBindFailureCount = disabledBindFailureCount;
    return this;
  }

   /**
   * The number of Topic Endpoint bind failures due to being disabled.
   * @return disabledBindFailureCount
  **/
  @Schema(description = "The number of Topic Endpoint bind failures due to being disabled.")
  public Long getDisabledBindFailureCount() {
    return disabledBindFailureCount;
  }

  public void setDisabledBindFailureCount(Long disabledBindFailureCount) {
    this.disabledBindFailureCount = disabledBindFailureCount;
  }

  public MsgVpnTopicEndpoint disabledDiscardedMsgCount(Long disabledDiscardedMsgCount) {
    this.disabledDiscardedMsgCount = disabledDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages discarded by the Topic Endpoint due to it being disabled.
   * @return disabledDiscardedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages discarded by the Topic Endpoint due to it being disabled.")
  public Long getDisabledDiscardedMsgCount() {
    return disabledDiscardedMsgCount;
  }

  public void setDisabledDiscardedMsgCount(Long disabledDiscardedMsgCount) {
    this.disabledDiscardedMsgCount = disabledDiscardedMsgCount;
  }

  public MsgVpnTopicEndpoint durable(Boolean durable) {
    this.durable = durable;
    return this;
  }

   /**
   * Indicates whether the Topic Endpoint is durable and not temporary.
   * @return durable
  **/
  @Schema(description = "Indicates whether the Topic Endpoint is durable and not temporary.")
  public Boolean isDurable() {
    return durable;
  }

  public void setDurable(Boolean durable) {
    this.durable = durable;
  }

  public MsgVpnTopicEndpoint egressEnabled(Boolean egressEnabled) {
    this.egressEnabled = egressEnabled;
    return this;
  }

   /**
   * Indicates whether the transmission of messages from the Topic Endpoint is enabled.
   * @return egressEnabled
  **/
  @Schema(description = "Indicates whether the transmission of messages from the Topic Endpoint is enabled.")
  public Boolean isEgressEnabled() {
    return egressEnabled;
  }

  public void setEgressEnabled(Boolean egressEnabled) {
    this.egressEnabled = egressEnabled;
  }

  public MsgVpnTopicEndpoint eventBindCountThreshold(EventThreshold eventBindCountThreshold) {
    this.eventBindCountThreshold = eventBindCountThreshold;
    return this;
  }

   /**
   * Get eventBindCountThreshold
   * @return eventBindCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventBindCountThreshold() {
    return eventBindCountThreshold;
  }

  public void setEventBindCountThreshold(EventThreshold eventBindCountThreshold) {
    this.eventBindCountThreshold = eventBindCountThreshold;
  }

  public MsgVpnTopicEndpoint eventRejectLowPriorityMsgLimitThreshold(EventThreshold eventRejectLowPriorityMsgLimitThreshold) {
    this.eventRejectLowPriorityMsgLimitThreshold = eventRejectLowPriorityMsgLimitThreshold;
    return this;
  }

   /**
   * Get eventRejectLowPriorityMsgLimitThreshold
   * @return eventRejectLowPriorityMsgLimitThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventRejectLowPriorityMsgLimitThreshold() {
    return eventRejectLowPriorityMsgLimitThreshold;
  }

  public void setEventRejectLowPriorityMsgLimitThreshold(EventThreshold eventRejectLowPriorityMsgLimitThreshold) {
    this.eventRejectLowPriorityMsgLimitThreshold = eventRejectLowPriorityMsgLimitThreshold;
  }

  public MsgVpnTopicEndpoint eventSpoolUsageThreshold(EventThreshold eventSpoolUsageThreshold) {
    this.eventSpoolUsageThreshold = eventSpoolUsageThreshold;
    return this;
  }

   /**
   * Get eventSpoolUsageThreshold
   * @return eventSpoolUsageThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventSpoolUsageThreshold() {
    return eventSpoolUsageThreshold;
  }

  public void setEventSpoolUsageThreshold(EventThreshold eventSpoolUsageThreshold) {
    this.eventSpoolUsageThreshold = eventSpoolUsageThreshold;
  }

  public MsgVpnTopicEndpoint highestAckedMsgId(Long highestAckedMsgId) {
    this.highestAckedMsgId = highestAckedMsgId;
    return this;
  }

   /**
   * The highest identifier (ID) of guaranteed messages in the Topic Endpoint that were acknowledged.
   * @return highestAckedMsgId
  **/
  @Schema(description = "The highest identifier (ID) of guaranteed messages in the Topic Endpoint that were acknowledged.")
  public Long getHighestAckedMsgId() {
    return highestAckedMsgId;
  }

  public void setHighestAckedMsgId(Long highestAckedMsgId) {
    this.highestAckedMsgId = highestAckedMsgId;
  }

  public MsgVpnTopicEndpoint highestMsgId(Long highestMsgId) {
    this.highestMsgId = highestMsgId;
    return this;
  }

   /**
   * The highest identifier (ID) of guaranteed messages in the Topic Endpoint.
   * @return highestMsgId
  **/
  @Schema(description = "The highest identifier (ID) of guaranteed messages in the Topic Endpoint.")
  public Long getHighestMsgId() {
    return highestMsgId;
  }

  public void setHighestMsgId(Long highestMsgId) {
    this.highestMsgId = highestMsgId;
  }

  public MsgVpnTopicEndpoint inProgressAckMsgCount(Long inProgressAckMsgCount) {
    this.inProgressAckMsgCount = inProgressAckMsgCount;
    return this;
  }

   /**
   * The number of acknowledgement messages received by the Topic Endpoint that are in the process of updating and deleting associated guaranteed messages.
   * @return inProgressAckMsgCount
  **/
  @Schema(description = "The number of acknowledgement messages received by the Topic Endpoint that are in the process of updating and deleting associated guaranteed messages.")
  public Long getInProgressAckMsgCount() {
    return inProgressAckMsgCount;
  }

  public void setInProgressAckMsgCount(Long inProgressAckMsgCount) {
    this.inProgressAckMsgCount = inProgressAckMsgCount;
  }

  public MsgVpnTopicEndpoint ingressEnabled(Boolean ingressEnabled) {
    this.ingressEnabled = ingressEnabled;
    return this;
  }

   /**
   * Indicates whether the reception of messages to the Topic Endpoint is enabled.
   * @return ingressEnabled
  **/
  @Schema(description = "Indicates whether the reception of messages to the Topic Endpoint is enabled.")
  public Boolean isIngressEnabled() {
    return ingressEnabled;
  }

  public void setIngressEnabled(Boolean ingressEnabled) {
    this.ingressEnabled = ingressEnabled;
  }

  public MsgVpnTopicEndpoint invalidSelectorBindFailureCount(Long invalidSelectorBindFailureCount) {
    this.invalidSelectorBindFailureCount = invalidSelectorBindFailureCount;
    return this;
  }

   /**
   * The number of Topic Endpoint bind failures due to an invalid selector.
   * @return invalidSelectorBindFailureCount
  **/
  @Schema(description = "The number of Topic Endpoint bind failures due to an invalid selector.")
  public Long getInvalidSelectorBindFailureCount() {
    return invalidSelectorBindFailureCount;
  }

  public void setInvalidSelectorBindFailureCount(Long invalidSelectorBindFailureCount) {
    this.invalidSelectorBindFailureCount = invalidSelectorBindFailureCount;
  }

  public MsgVpnTopicEndpoint lastReplayCompleteTime(Integer lastReplayCompleteTime) {
    this.lastReplayCompleteTime = lastReplayCompleteTime;
    return this;
  }

   /**
   * The timestamp of the last completed replay for the Topic Endpoint. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return lastReplayCompleteTime
  **/
  @Schema(description = "The timestamp of the last completed replay for the Topic Endpoint. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getLastReplayCompleteTime() {
    return lastReplayCompleteTime;
  }

  public void setLastReplayCompleteTime(Integer lastReplayCompleteTime) {
    this.lastReplayCompleteTime = lastReplayCompleteTime;
  }

  public MsgVpnTopicEndpoint lastReplayFailureReason(String lastReplayFailureReason) {
    this.lastReplayFailureReason = lastReplayFailureReason;
    return this;
  }

   /**
   * The reason for the last replay failure for the Topic Endpoint.
   * @return lastReplayFailureReason
  **/
  @Schema(description = "The reason for the last replay failure for the Topic Endpoint.")
  public String getLastReplayFailureReason() {
    return lastReplayFailureReason;
  }

  public void setLastReplayFailureReason(String lastReplayFailureReason) {
    this.lastReplayFailureReason = lastReplayFailureReason;
  }

  public MsgVpnTopicEndpoint lastReplayFailureTime(Integer lastReplayFailureTime) {
    this.lastReplayFailureTime = lastReplayFailureTime;
    return this;
  }

   /**
   * The timestamp of the last replay failure for the Topic Endpoint. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return lastReplayFailureTime
  **/
  @Schema(description = "The timestamp of the last replay failure for the Topic Endpoint. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getLastReplayFailureTime() {
    return lastReplayFailureTime;
  }

  public void setLastReplayFailureTime(Integer lastReplayFailureTime) {
    this.lastReplayFailureTime = lastReplayFailureTime;
  }

  public MsgVpnTopicEndpoint lastReplayStartTime(Integer lastReplayStartTime) {
    this.lastReplayStartTime = lastReplayStartTime;
    return this;
  }

   /**
   * The timestamp of the last replay started for the Topic Endpoint. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return lastReplayStartTime
  **/
  @Schema(description = "The timestamp of the last replay started for the Topic Endpoint. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getLastReplayStartTime() {
    return lastReplayStartTime;
  }

  public void setLastReplayStartTime(Integer lastReplayStartTime) {
    this.lastReplayStartTime = lastReplayStartTime;
  }

  public MsgVpnTopicEndpoint lastReplayedMsgTxTime(Integer lastReplayedMsgTxTime) {
    this.lastReplayedMsgTxTime = lastReplayedMsgTxTime;
    return this;
  }

   /**
   * The timestamp of the last replayed message transmitted by the Topic Endpoint. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return lastReplayedMsgTxTime
  **/
  @Schema(description = "The timestamp of the last replayed message transmitted by the Topic Endpoint. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getLastReplayedMsgTxTime() {
    return lastReplayedMsgTxTime;
  }

  public void setLastReplayedMsgTxTime(Integer lastReplayedMsgTxTime) {
    this.lastReplayedMsgTxTime = lastReplayedMsgTxTime;
  }

  public MsgVpnTopicEndpoint lastSelectorExaminedMsgId(Long lastSelectorExaminedMsgId) {
    this.lastSelectorExaminedMsgId = lastSelectorExaminedMsgId;
    return this;
  }

   /**
   * The identifier (ID) of the last message examined by the Topic Endpoint selector.
   * @return lastSelectorExaminedMsgId
  **/
  @Schema(description = "The identifier (ID) of the last message examined by the Topic Endpoint selector.")
  public Long getLastSelectorExaminedMsgId() {
    return lastSelectorExaminedMsgId;
  }

  public void setLastSelectorExaminedMsgId(Long lastSelectorExaminedMsgId) {
    this.lastSelectorExaminedMsgId = lastSelectorExaminedMsgId;
  }

  public MsgVpnTopicEndpoint lastSpooledMsgId(Long lastSpooledMsgId) {
    this.lastSpooledMsgId = lastSpooledMsgId;
    return this;
  }

   /**
   * The identifier (ID) of the last guaranteed message spooled in the Topic Endpoint.
   * @return lastSpooledMsgId
  **/
  @Schema(description = "The identifier (ID) of the last guaranteed message spooled in the Topic Endpoint.")
  public Long getLastSpooledMsgId() {
    return lastSpooledMsgId;
  }

  public void setLastSpooledMsgId(Long lastSpooledMsgId) {
    this.lastSpooledMsgId = lastSpooledMsgId;
  }

  public MsgVpnTopicEndpoint lowPriorityMsgCongestionDiscardedMsgCount(Long lowPriorityMsgCongestionDiscardedMsgCount) {
    this.lowPriorityMsgCongestionDiscardedMsgCount = lowPriorityMsgCongestionDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages discarded by the Topic Endpoint due to low priority message congestion control.
   * @return lowPriorityMsgCongestionDiscardedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages discarded by the Topic Endpoint due to low priority message congestion control.")
  public Long getLowPriorityMsgCongestionDiscardedMsgCount() {
    return lowPriorityMsgCongestionDiscardedMsgCount;
  }

  public void setLowPriorityMsgCongestionDiscardedMsgCount(Long lowPriorityMsgCongestionDiscardedMsgCount) {
    this.lowPriorityMsgCongestionDiscardedMsgCount = lowPriorityMsgCongestionDiscardedMsgCount;
  }

  public MsgVpnTopicEndpoint lowPriorityMsgCongestionState(String lowPriorityMsgCongestionState) {
    this.lowPriorityMsgCongestionState = lowPriorityMsgCongestionState;
    return this;
  }

   /**
   * The state of the low priority message congestion in the Topic Endpoint. The allowed values and their meaning are:  &lt;pre&gt; \&quot;disabled\&quot; - Messages are not being checked for priority. \&quot;not-congested\&quot; - Low priority messages are being stored and delivered. \&quot;congested\&quot; - Low priority messages are being discarded. &lt;/pre&gt; 
   * @return lowPriorityMsgCongestionState
  **/
  @Schema(description = "The state of the low priority message congestion in the Topic Endpoint. The allowed values and their meaning are:  <pre> \"disabled\" - Messages are not being checked for priority. \"not-congested\" - Low priority messages are being stored and delivered. \"congested\" - Low priority messages are being discarded. </pre> ")
  public String getLowPriorityMsgCongestionState() {
    return lowPriorityMsgCongestionState;
  }

  public void setLowPriorityMsgCongestionState(String lowPriorityMsgCongestionState) {
    this.lowPriorityMsgCongestionState = lowPriorityMsgCongestionState;
  }

  public MsgVpnTopicEndpoint lowestAckedMsgId(Long lowestAckedMsgId) {
    this.lowestAckedMsgId = lowestAckedMsgId;
    return this;
  }

   /**
   * The lowest identifier (ID) of guaranteed messages in the Topic Endpoint that were acknowledged.
   * @return lowestAckedMsgId
  **/
  @Schema(description = "The lowest identifier (ID) of guaranteed messages in the Topic Endpoint that were acknowledged.")
  public Long getLowestAckedMsgId() {
    return lowestAckedMsgId;
  }

  public void setLowestAckedMsgId(Long lowestAckedMsgId) {
    this.lowestAckedMsgId = lowestAckedMsgId;
  }

  public MsgVpnTopicEndpoint lowestMsgId(Long lowestMsgId) {
    this.lowestMsgId = lowestMsgId;
    return this;
  }

   /**
   * The lowest identifier (ID) of guaranteed messages in the Topic Endpoint.
   * @return lowestMsgId
  **/
  @Schema(description = "The lowest identifier (ID) of guaranteed messages in the Topic Endpoint.")
  public Long getLowestMsgId() {
    return lowestMsgId;
  }

  public void setLowestMsgId(Long lowestMsgId) {
    this.lowestMsgId = lowestMsgId;
  }

  public MsgVpnTopicEndpoint maxBindCount(Long maxBindCount) {
    this.maxBindCount = maxBindCount;
    return this;
  }

   /**
   * The maximum number of consumer flows that can bind to the Topic Endpoint.
   * @return maxBindCount
  **/
  @Schema(description = "The maximum number of consumer flows that can bind to the Topic Endpoint.")
  public Long getMaxBindCount() {
    return maxBindCount;
  }

  public void setMaxBindCount(Long maxBindCount) {
    this.maxBindCount = maxBindCount;
  }

  public MsgVpnTopicEndpoint maxBindCountExceededBindFailureCount(Long maxBindCountExceededBindFailureCount) {
    this.maxBindCountExceededBindFailureCount = maxBindCountExceededBindFailureCount;
    return this;
  }

   /**
   * The number of Topic Endpoint bind failures due to the maximum bind count being exceeded.
   * @return maxBindCountExceededBindFailureCount
  **/
  @Schema(description = "The number of Topic Endpoint bind failures due to the maximum bind count being exceeded.")
  public Long getMaxBindCountExceededBindFailureCount() {
    return maxBindCountExceededBindFailureCount;
  }

  public void setMaxBindCountExceededBindFailureCount(Long maxBindCountExceededBindFailureCount) {
    this.maxBindCountExceededBindFailureCount = maxBindCountExceededBindFailureCount;
  }

  public MsgVpnTopicEndpoint maxDeliveredUnackedMsgsPerFlow(Long maxDeliveredUnackedMsgsPerFlow) {
    this.maxDeliveredUnackedMsgsPerFlow = maxDeliveredUnackedMsgsPerFlow;
    return this;
  }

   /**
   * The maximum number of messages delivered but not acknowledged per flow for the Topic Endpoint.
   * @return maxDeliveredUnackedMsgsPerFlow
  **/
  @Schema(description = "The maximum number of messages delivered but not acknowledged per flow for the Topic Endpoint.")
  public Long getMaxDeliveredUnackedMsgsPerFlow() {
    return maxDeliveredUnackedMsgsPerFlow;
  }

  public void setMaxDeliveredUnackedMsgsPerFlow(Long maxDeliveredUnackedMsgsPerFlow) {
    this.maxDeliveredUnackedMsgsPerFlow = maxDeliveredUnackedMsgsPerFlow;
  }

  public MsgVpnTopicEndpoint maxEffectiveBindCount(Integer maxEffectiveBindCount) {
    this.maxEffectiveBindCount = maxEffectiveBindCount;
    return this;
  }

   /**
   * The effective maximum number of consumer flows that can bind to the Topic Endpoint.
   * @return maxEffectiveBindCount
  **/
  @Schema(description = "The effective maximum number of consumer flows that can bind to the Topic Endpoint.")
  public Integer getMaxEffectiveBindCount() {
    return maxEffectiveBindCount;
  }

  public void setMaxEffectiveBindCount(Integer maxEffectiveBindCount) {
    this.maxEffectiveBindCount = maxEffectiveBindCount;
  }

  public MsgVpnTopicEndpoint maxMsgSize(Integer maxMsgSize) {
    this.maxMsgSize = maxMsgSize;
    return this;
  }

   /**
   * The maximum message size allowed in the Topic Endpoint, in bytes (B).
   * @return maxMsgSize
  **/
  @Schema(description = "The maximum message size allowed in the Topic Endpoint, in bytes (B).")
  public Integer getMaxMsgSize() {
    return maxMsgSize;
  }

  public void setMaxMsgSize(Integer maxMsgSize) {
    this.maxMsgSize = maxMsgSize;
  }

  public MsgVpnTopicEndpoint maxMsgSizeExceededDiscardedMsgCount(Long maxMsgSizeExceededDiscardedMsgCount) {
    this.maxMsgSizeExceededDiscardedMsgCount = maxMsgSizeExceededDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages discarded by the Topic Endpoint due to the maximum message size being exceeded.
   * @return maxMsgSizeExceededDiscardedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages discarded by the Topic Endpoint due to the maximum message size being exceeded.")
  public Long getMaxMsgSizeExceededDiscardedMsgCount() {
    return maxMsgSizeExceededDiscardedMsgCount;
  }

  public void setMaxMsgSizeExceededDiscardedMsgCount(Long maxMsgSizeExceededDiscardedMsgCount) {
    this.maxMsgSizeExceededDiscardedMsgCount = maxMsgSizeExceededDiscardedMsgCount;
  }

  public MsgVpnTopicEndpoint maxMsgSpoolUsageExceededDiscardedMsgCount(Long maxMsgSpoolUsageExceededDiscardedMsgCount) {
    this.maxMsgSpoolUsageExceededDiscardedMsgCount = maxMsgSpoolUsageExceededDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages discarded by the Topic Endpoint due to the maximum message spool usage being exceeded.
   * @return maxMsgSpoolUsageExceededDiscardedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages discarded by the Topic Endpoint due to the maximum message spool usage being exceeded.")
  public Long getMaxMsgSpoolUsageExceededDiscardedMsgCount() {
    return maxMsgSpoolUsageExceededDiscardedMsgCount;
  }

  public void setMaxMsgSpoolUsageExceededDiscardedMsgCount(Long maxMsgSpoolUsageExceededDiscardedMsgCount) {
    this.maxMsgSpoolUsageExceededDiscardedMsgCount = maxMsgSpoolUsageExceededDiscardedMsgCount;
  }

  public MsgVpnTopicEndpoint maxRedeliveryCount(Long maxRedeliveryCount) {
    this.maxRedeliveryCount = maxRedeliveryCount;
    return this;
  }

   /**
   * The maximum number of times the Topic Endpoint will attempt redelivery of a message prior to it being discarded or moved to the DMQ. A value of 0 means to retry forever.
   * @return maxRedeliveryCount
  **/
  @Schema(description = "The maximum number of times the Topic Endpoint will attempt redelivery of a message prior to it being discarded or moved to the DMQ. A value of 0 means to retry forever.")
  public Long getMaxRedeliveryCount() {
    return maxRedeliveryCount;
  }

  public void setMaxRedeliveryCount(Long maxRedeliveryCount) {
    this.maxRedeliveryCount = maxRedeliveryCount;
  }

  public MsgVpnTopicEndpoint maxRedeliveryExceededDiscardedMsgCount(Long maxRedeliveryExceededDiscardedMsgCount) {
    this.maxRedeliveryExceededDiscardedMsgCount = maxRedeliveryExceededDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages discarded by the Topic Endpoint due to the maximum redelivery attempts being exceeded.
   * @return maxRedeliveryExceededDiscardedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages discarded by the Topic Endpoint due to the maximum redelivery attempts being exceeded.")
  public Long getMaxRedeliveryExceededDiscardedMsgCount() {
    return maxRedeliveryExceededDiscardedMsgCount;
  }

  public void setMaxRedeliveryExceededDiscardedMsgCount(Long maxRedeliveryExceededDiscardedMsgCount) {
    this.maxRedeliveryExceededDiscardedMsgCount = maxRedeliveryExceededDiscardedMsgCount;
  }

  public MsgVpnTopicEndpoint maxRedeliveryExceededToDmqFailedMsgCount(Long maxRedeliveryExceededToDmqFailedMsgCount) {
    this.maxRedeliveryExceededToDmqFailedMsgCount = maxRedeliveryExceededToDmqFailedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages discarded by the Topic Endpoint due to the maximum redelivery attempts being exceeded and failing to move to the Dead Message Queue (DMQ).
   * @return maxRedeliveryExceededToDmqFailedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages discarded by the Topic Endpoint due to the maximum redelivery attempts being exceeded and failing to move to the Dead Message Queue (DMQ).")
  public Long getMaxRedeliveryExceededToDmqFailedMsgCount() {
    return maxRedeliveryExceededToDmqFailedMsgCount;
  }

  public void setMaxRedeliveryExceededToDmqFailedMsgCount(Long maxRedeliveryExceededToDmqFailedMsgCount) {
    this.maxRedeliveryExceededToDmqFailedMsgCount = maxRedeliveryExceededToDmqFailedMsgCount;
  }

  public MsgVpnTopicEndpoint maxRedeliveryExceededToDmqMsgCount(Long maxRedeliveryExceededToDmqMsgCount) {
    this.maxRedeliveryExceededToDmqMsgCount = maxRedeliveryExceededToDmqMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages moved to the Dead Message Queue (DMQ) by the Topic Endpoint due to the maximum redelivery attempts being exceeded.
   * @return maxRedeliveryExceededToDmqMsgCount
  **/
  @Schema(description = "The number of guaranteed messages moved to the Dead Message Queue (DMQ) by the Topic Endpoint due to the maximum redelivery attempts being exceeded.")
  public Long getMaxRedeliveryExceededToDmqMsgCount() {
    return maxRedeliveryExceededToDmqMsgCount;
  }

  public void setMaxRedeliveryExceededToDmqMsgCount(Long maxRedeliveryExceededToDmqMsgCount) {
    this.maxRedeliveryExceededToDmqMsgCount = maxRedeliveryExceededToDmqMsgCount;
  }

  public MsgVpnTopicEndpoint maxSpoolUsage(Long maxSpoolUsage) {
    this.maxSpoolUsage = maxSpoolUsage;
    return this;
  }

   /**
   * The maximum message spool usage allowed by the Topic Endpoint, in megabytes (MB). A value of 0 only allows spooling of the last message received and disables quota checking.
   * @return maxSpoolUsage
  **/
  @Schema(description = "The maximum message spool usage allowed by the Topic Endpoint, in megabytes (MB). A value of 0 only allows spooling of the last message received and disables quota checking.")
  public Long getMaxSpoolUsage() {
    return maxSpoolUsage;
  }

  public void setMaxSpoolUsage(Long maxSpoolUsage) {
    this.maxSpoolUsage = maxSpoolUsage;
  }

  public MsgVpnTopicEndpoint maxTtl(Long maxTtl) {
    this.maxTtl = maxTtl;
    return this;
  }

   /**
   * The maximum time in seconds a message can stay in the Topic Endpoint when &#x60;respectTtlEnabled&#x60; is &#x60;\&quot;true\&quot;&#x60;. A message expires when the lesser of the sender assigned time-to-live (TTL) in the message and the &#x60;maxTtl&#x60; configured for the Topic Endpoint, is exceeded. A value of 0 disables expiry.
   * @return maxTtl
  **/
  @Schema(description = "The maximum time in seconds a message can stay in the Topic Endpoint when `respectTtlEnabled` is `\"true\"`. A message expires when the lesser of the sender assigned time-to-live (TTL) in the message and the `maxTtl` configured for the Topic Endpoint, is exceeded. A value of 0 disables expiry.")
  public Long getMaxTtl() {
    return maxTtl;
  }

  public void setMaxTtl(Long maxTtl) {
    this.maxTtl = maxTtl;
  }

  public MsgVpnTopicEndpoint maxTtlExceededDiscardedMsgCount(Long maxTtlExceededDiscardedMsgCount) {
    this.maxTtlExceededDiscardedMsgCount = maxTtlExceededDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages discarded by the Topic Endpoint due to the maximum time-to-live (TTL) in hops being exceeded. The TTL hop count is incremented when the message crosses a bridge.
   * @return maxTtlExceededDiscardedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages discarded by the Topic Endpoint due to the maximum time-to-live (TTL) in hops being exceeded. The TTL hop count is incremented when the message crosses a bridge.")
  public Long getMaxTtlExceededDiscardedMsgCount() {
    return maxTtlExceededDiscardedMsgCount;
  }

  public void setMaxTtlExceededDiscardedMsgCount(Long maxTtlExceededDiscardedMsgCount) {
    this.maxTtlExceededDiscardedMsgCount = maxTtlExceededDiscardedMsgCount;
  }

  public MsgVpnTopicEndpoint maxTtlExpiredDiscardedMsgCount(Long maxTtlExpiredDiscardedMsgCount) {
    this.maxTtlExpiredDiscardedMsgCount = maxTtlExpiredDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages discarded by the Topic Endpoint due to the maximum time-to-live (TTL) timestamp expiring.
   * @return maxTtlExpiredDiscardedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages discarded by the Topic Endpoint due to the maximum time-to-live (TTL) timestamp expiring.")
  public Long getMaxTtlExpiredDiscardedMsgCount() {
    return maxTtlExpiredDiscardedMsgCount;
  }

  public void setMaxTtlExpiredDiscardedMsgCount(Long maxTtlExpiredDiscardedMsgCount) {
    this.maxTtlExpiredDiscardedMsgCount = maxTtlExpiredDiscardedMsgCount;
  }

  public MsgVpnTopicEndpoint maxTtlExpiredToDmqFailedMsgCount(Long maxTtlExpiredToDmqFailedMsgCount) {
    this.maxTtlExpiredToDmqFailedMsgCount = maxTtlExpiredToDmqFailedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages discarded by the Topic Endpoint due to the maximum time-to-live (TTL) timestamp expiring and failing to move to the Dead Message Queue (DMQ).
   * @return maxTtlExpiredToDmqFailedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages discarded by the Topic Endpoint due to the maximum time-to-live (TTL) timestamp expiring and failing to move to the Dead Message Queue (DMQ).")
  public Long getMaxTtlExpiredToDmqFailedMsgCount() {
    return maxTtlExpiredToDmqFailedMsgCount;
  }

  public void setMaxTtlExpiredToDmqFailedMsgCount(Long maxTtlExpiredToDmqFailedMsgCount) {
    this.maxTtlExpiredToDmqFailedMsgCount = maxTtlExpiredToDmqFailedMsgCount;
  }

  public MsgVpnTopicEndpoint maxTtlExpiredToDmqMsgCount(Long maxTtlExpiredToDmqMsgCount) {
    this.maxTtlExpiredToDmqMsgCount = maxTtlExpiredToDmqMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages moved to the Dead Message Queue (DMQ) by the Topic Endpoint due to the maximum time-to-live (TTL) timestamp expiring.
   * @return maxTtlExpiredToDmqMsgCount
  **/
  @Schema(description = "The number of guaranteed messages moved to the Dead Message Queue (DMQ) by the Topic Endpoint due to the maximum time-to-live (TTL) timestamp expiring.")
  public Long getMaxTtlExpiredToDmqMsgCount() {
    return maxTtlExpiredToDmqMsgCount;
  }

  public void setMaxTtlExpiredToDmqMsgCount(Long maxTtlExpiredToDmqMsgCount) {
    this.maxTtlExpiredToDmqMsgCount = maxTtlExpiredToDmqMsgCount;
  }

  public MsgVpnTopicEndpoint msgSpoolPeakUsage(Long msgSpoolPeakUsage) {
    this.msgSpoolPeakUsage = msgSpoolPeakUsage;
    return this;
  }

   /**
   * The message spool peak usage by the Topic Endpoint, in bytes (B).
   * @return msgSpoolPeakUsage
  **/
  @Schema(description = "The message spool peak usage by the Topic Endpoint, in bytes (B).")
  public Long getMsgSpoolPeakUsage() {
    return msgSpoolPeakUsage;
  }

  public void setMsgSpoolPeakUsage(Long msgSpoolPeakUsage) {
    this.msgSpoolPeakUsage = msgSpoolPeakUsage;
  }

  public MsgVpnTopicEndpoint msgSpoolUsage(Long msgSpoolUsage) {
    this.msgSpoolUsage = msgSpoolUsage;
    return this;
  }

   /**
   * The message spool usage by the Topic Endpoint, in bytes (B).
   * @return msgSpoolUsage
  **/
  @Schema(description = "The message spool usage by the Topic Endpoint, in bytes (B).")
  public Long getMsgSpoolUsage() {
    return msgSpoolUsage;
  }

  public void setMsgSpoolUsage(Long msgSpoolUsage) {
    this.msgSpoolUsage = msgSpoolUsage;
  }

  public MsgVpnTopicEndpoint msgVpnName(String msgVpnName) {
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

  public MsgVpnTopicEndpoint networkTopic(String networkTopic) {
    this.networkTopic = networkTopic;
    return this;
  }

   /**
   * The name of the network topic for the Topic Endpoint.
   * @return networkTopic
  **/
  @Schema(description = "The name of the network topic for the Topic Endpoint.")
  public String getNetworkTopic() {
    return networkTopic;
  }

  public void setNetworkTopic(String networkTopic) {
    this.networkTopic = networkTopic;
  }

  public MsgVpnTopicEndpoint noLocalDeliveryDiscardedMsgCount(Long noLocalDeliveryDiscardedMsgCount) {
    this.noLocalDeliveryDiscardedMsgCount = noLocalDeliveryDiscardedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages discarded by the Topic Endpoint due to no local delivery being requested.
   * @return noLocalDeliveryDiscardedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages discarded by the Topic Endpoint due to no local delivery being requested.")
  public Long getNoLocalDeliveryDiscardedMsgCount() {
    return noLocalDeliveryDiscardedMsgCount;
  }

  public void setNoLocalDeliveryDiscardedMsgCount(Long noLocalDeliveryDiscardedMsgCount) {
    this.noLocalDeliveryDiscardedMsgCount = noLocalDeliveryDiscardedMsgCount;
  }

  public MsgVpnTopicEndpoint otherBindFailureCount(Long otherBindFailureCount) {
    this.otherBindFailureCount = otherBindFailureCount;
    return this;
  }

   /**
   * The number of Topic Endpoint bind failures due to other reasons.
   * @return otherBindFailureCount
  **/
  @Schema(description = "The number of Topic Endpoint bind failures due to other reasons.")
  public Long getOtherBindFailureCount() {
    return otherBindFailureCount;
  }

  public void setOtherBindFailureCount(Long otherBindFailureCount) {
    this.otherBindFailureCount = otherBindFailureCount;
  }

  public MsgVpnTopicEndpoint owner(String owner) {
    this.owner = owner;
    return this;
  }

   /**
   * The Client Username that owns the Topic Endpoint and has permission equivalent to &#x60;\&quot;delete\&quot;&#x60;.
   * @return owner
  **/
  @Schema(description = "The Client Username that owns the Topic Endpoint and has permission equivalent to `\"delete\"`.")
  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public MsgVpnTopicEndpoint permission(PermissionEnum permission) {
    this.permission = permission;
    return this;
  }

   /**
   * The permission level for all consumers of the Topic Endpoint, excluding the owner. The allowed values and their meaning are:  &lt;pre&gt; \&quot;no-access\&quot; - Disallows all access. \&quot;read-only\&quot; - Read-only access to the messages. \&quot;consume\&quot; - Consume (read and remove) messages. \&quot;modify-topic\&quot; - Consume messages or modify the topic/selector. \&quot;delete\&quot; - Consume messages, modify the topic/selector or delete the Client created endpoint altogether. &lt;/pre&gt; 
   * @return permission
  **/
  @Schema(description = "The permission level for all consumers of the Topic Endpoint, excluding the owner. The allowed values and their meaning are:  <pre> \"no-access\" - Disallows all access. \"read-only\" - Read-only access to the messages. \"consume\" - Consume (read and remove) messages. \"modify-topic\" - Consume messages or modify the topic/selector. \"delete\" - Consume messages, modify the topic/selector or delete the Client created endpoint altogether. </pre> ")
  public PermissionEnum getPermission() {
    return permission;
  }

  public void setPermission(PermissionEnum permission) {
    this.permission = permission;
  }

  public MsgVpnTopicEndpoint redeliveredMsgCount(Long redeliveredMsgCount) {
    this.redeliveredMsgCount = redeliveredMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages transmitted by the Topic Endpoint for redelivery.
   * @return redeliveredMsgCount
  **/
  @Schema(description = "The number of guaranteed messages transmitted by the Topic Endpoint for redelivery.")
  public Long getRedeliveredMsgCount() {
    return redeliveredMsgCount;
  }

  public void setRedeliveredMsgCount(Long redeliveredMsgCount) {
    this.redeliveredMsgCount = redeliveredMsgCount;
  }

  public MsgVpnTopicEndpoint rejectLowPriorityMsgEnabled(Boolean rejectLowPriorityMsgEnabled) {
    this.rejectLowPriorityMsgEnabled = rejectLowPriorityMsgEnabled;
    return this;
  }

   /**
   * Indicates whether the checking of low priority messages against the &#x60;rejectLowPriorityMsgLimit&#x60; is enabled.
   * @return rejectLowPriorityMsgEnabled
  **/
  @Schema(description = "Indicates whether the checking of low priority messages against the `rejectLowPriorityMsgLimit` is enabled.")
  public Boolean isRejectLowPriorityMsgEnabled() {
    return rejectLowPriorityMsgEnabled;
  }

  public void setRejectLowPriorityMsgEnabled(Boolean rejectLowPriorityMsgEnabled) {
    this.rejectLowPriorityMsgEnabled = rejectLowPriorityMsgEnabled;
  }

  public MsgVpnTopicEndpoint rejectLowPriorityMsgLimit(Long rejectLowPriorityMsgLimit) {
    this.rejectLowPriorityMsgLimit = rejectLowPriorityMsgLimit;
    return this;
  }

   /**
   * The number of messages of any priority in the Topic Endpoint above which low priority messages are not admitted but higher priority messages are allowed.
   * @return rejectLowPriorityMsgLimit
  **/
  @Schema(description = "The number of messages of any priority in the Topic Endpoint above which low priority messages are not admitted but higher priority messages are allowed.")
  public Long getRejectLowPriorityMsgLimit() {
    return rejectLowPriorityMsgLimit;
  }

  public void setRejectLowPriorityMsgLimit(Long rejectLowPriorityMsgLimit) {
    this.rejectLowPriorityMsgLimit = rejectLowPriorityMsgLimit;
  }

  public MsgVpnTopicEndpoint rejectMsgToSenderOnDiscardBehavior(RejectMsgToSenderOnDiscardBehaviorEnum rejectMsgToSenderOnDiscardBehavior) {
    this.rejectMsgToSenderOnDiscardBehavior = rejectMsgToSenderOnDiscardBehavior;
    return this;
  }

   /**
   * Determines when to return negative acknowledgements (NACKs) to sending clients on message discards. Note that NACKs cause the message to not be delivered to any destination and Transacted Session commits to fail. The allowed values and their meaning are:  &lt;pre&gt; \&quot;always\&quot; - Always return a negative acknowledgment (NACK) to the sending client on message discard. \&quot;when-topic-endpoint-enabled\&quot; - Only return a negative acknowledgment (NACK) to the sending client on message discard when the Topic Endpoint is enabled. \&quot;never\&quot; - Never return a negative acknowledgment (NACK) to the sending client on message discard. &lt;/pre&gt; 
   * @return rejectMsgToSenderOnDiscardBehavior
  **/
  @Schema(description = "Determines when to return negative acknowledgements (NACKs) to sending clients on message discards. Note that NACKs cause the message to not be delivered to any destination and Transacted Session commits to fail. The allowed values and their meaning are:  <pre> \"always\" - Always return a negative acknowledgment (NACK) to the sending client on message discard. \"when-topic-endpoint-enabled\" - Only return a negative acknowledgment (NACK) to the sending client on message discard when the Topic Endpoint is enabled. \"never\" - Never return a negative acknowledgment (NACK) to the sending client on message discard. </pre> ")
  public RejectMsgToSenderOnDiscardBehaviorEnum getRejectMsgToSenderOnDiscardBehavior() {
    return rejectMsgToSenderOnDiscardBehavior;
  }

  public void setRejectMsgToSenderOnDiscardBehavior(RejectMsgToSenderOnDiscardBehaviorEnum rejectMsgToSenderOnDiscardBehavior) {
    this.rejectMsgToSenderOnDiscardBehavior = rejectMsgToSenderOnDiscardBehavior;
  }

  public MsgVpnTopicEndpoint replayFailureCount(Long replayFailureCount) {
    this.replayFailureCount = replayFailureCount;
    return this;
  }

   /**
   * The number of replays that failed for the Topic Endpoint.
   * @return replayFailureCount
  **/
  @Schema(description = "The number of replays that failed for the Topic Endpoint.")
  public Long getReplayFailureCount() {
    return replayFailureCount;
  }

  public void setReplayFailureCount(Long replayFailureCount) {
    this.replayFailureCount = replayFailureCount;
  }

  public MsgVpnTopicEndpoint replayStartCount(Long replayStartCount) {
    this.replayStartCount = replayStartCount;
    return this;
  }

   /**
   * The number of replays started for the Topic Endpoint.
   * @return replayStartCount
  **/
  @Schema(description = "The number of replays started for the Topic Endpoint.")
  public Long getReplayStartCount() {
    return replayStartCount;
  }

  public void setReplayStartCount(Long replayStartCount) {
    this.replayStartCount = replayStartCount;
  }

  public MsgVpnTopicEndpoint replayState(String replayState) {
    this.replayState = replayState;
    return this;
  }

   /**
   * The state of replay for the Topic Endpoint. The allowed values and their meaning are:  &lt;pre&gt; \&quot;initializing\&quot; - All messages are being deleted from the endpoint before replay starts. \&quot;active\&quot; - Subscription matching logged messages are being replayed to the endpoint. \&quot;pending-complete\&quot; - Replay is complete, but final accounting is in progress. \&quot;complete\&quot; - Replay and all related activities are complete. \&quot;failed\&quot; - Replay has failed and is waiting for an unbind response. &lt;/pre&gt; 
   * @return replayState
  **/
  @Schema(description = "The state of replay for the Topic Endpoint. The allowed values and their meaning are:  <pre> \"initializing\" - All messages are being deleted from the endpoint before replay starts. \"active\" - Subscription matching logged messages are being replayed to the endpoint. \"pending-complete\" - Replay is complete, but final accounting is in progress. \"complete\" - Replay and all related activities are complete. \"failed\" - Replay has failed and is waiting for an unbind response. </pre> ")
  public String getReplayState() {
    return replayState;
  }

  public void setReplayState(String replayState) {
    this.replayState = replayState;
  }

  public MsgVpnTopicEndpoint replaySuccessCount(Long replaySuccessCount) {
    this.replaySuccessCount = replaySuccessCount;
    return this;
  }

   /**
   * The number of replays that succeeded for the Topic Endpoint.
   * @return replaySuccessCount
  **/
  @Schema(description = "The number of replays that succeeded for the Topic Endpoint.")
  public Long getReplaySuccessCount() {
    return replaySuccessCount;
  }

  public void setReplaySuccessCount(Long replaySuccessCount) {
    this.replaySuccessCount = replaySuccessCount;
  }

  public MsgVpnTopicEndpoint replayedAckedMsgCount(Long replayedAckedMsgCount) {
    this.replayedAckedMsgCount = replayedAckedMsgCount;
    return this;
  }

   /**
   * The number of replayed messages transmitted by the Topic Endpoint and acked by all consumers.
   * @return replayedAckedMsgCount
  **/
  @Schema(description = "The number of replayed messages transmitted by the Topic Endpoint and acked by all consumers.")
  public Long getReplayedAckedMsgCount() {
    return replayedAckedMsgCount;
  }

  public void setReplayedAckedMsgCount(Long replayedAckedMsgCount) {
    this.replayedAckedMsgCount = replayedAckedMsgCount;
  }

  public MsgVpnTopicEndpoint replayedTxMsgCount(Long replayedTxMsgCount) {
    this.replayedTxMsgCount = replayedTxMsgCount;
    return this;
  }

   /**
   * The number of replayed messages transmitted by the Topic Endpoint.
   * @return replayedTxMsgCount
  **/
  @Schema(description = "The number of replayed messages transmitted by the Topic Endpoint.")
  public Long getReplayedTxMsgCount() {
    return replayedTxMsgCount;
  }

  public void setReplayedTxMsgCount(Long replayedTxMsgCount) {
    this.replayedTxMsgCount = replayedTxMsgCount;
  }

  public MsgVpnTopicEndpoint replicationActiveAckPropTxMsgCount(Long replicationActiveAckPropTxMsgCount) {
    this.replicationActiveAckPropTxMsgCount = replicationActiveAckPropTxMsgCount;
    return this;
  }

   /**
   * The number of acknowledgement messages propagated by the Topic Endpoint to the replication standby remote Message VPN.
   * @return replicationActiveAckPropTxMsgCount
  **/
  @Schema(description = "The number of acknowledgement messages propagated by the Topic Endpoint to the replication standby remote Message VPN.")
  public Long getReplicationActiveAckPropTxMsgCount() {
    return replicationActiveAckPropTxMsgCount;
  }

  public void setReplicationActiveAckPropTxMsgCount(Long replicationActiveAckPropTxMsgCount) {
    this.replicationActiveAckPropTxMsgCount = replicationActiveAckPropTxMsgCount;
  }

  public MsgVpnTopicEndpoint replicationStandbyAckPropRxMsgCount(Long replicationStandbyAckPropRxMsgCount) {
    this.replicationStandbyAckPropRxMsgCount = replicationStandbyAckPropRxMsgCount;
    return this;
  }

   /**
   * The number of propagated acknowledgement messages received by the Topic Endpoint from the replication active remote Message VPN.
   * @return replicationStandbyAckPropRxMsgCount
  **/
  @Schema(description = "The number of propagated acknowledgement messages received by the Topic Endpoint from the replication active remote Message VPN.")
  public Long getReplicationStandbyAckPropRxMsgCount() {
    return replicationStandbyAckPropRxMsgCount;
  }

  public void setReplicationStandbyAckPropRxMsgCount(Long replicationStandbyAckPropRxMsgCount) {
    this.replicationStandbyAckPropRxMsgCount = replicationStandbyAckPropRxMsgCount;
  }

  public MsgVpnTopicEndpoint replicationStandbyAckedByAckPropMsgCount(Long replicationStandbyAckedByAckPropMsgCount) {
    this.replicationStandbyAckedByAckPropMsgCount = replicationStandbyAckedByAckPropMsgCount;
    return this;
  }

   /**
   * The number of messages acknowledged in the Topic Endpoint by acknowledgement propagation from the replication active remote Message VPN.
   * @return replicationStandbyAckedByAckPropMsgCount
  **/
  @Schema(description = "The number of messages acknowledged in the Topic Endpoint by acknowledgement propagation from the replication active remote Message VPN.")
  public Long getReplicationStandbyAckedByAckPropMsgCount() {
    return replicationStandbyAckedByAckPropMsgCount;
  }

  public void setReplicationStandbyAckedByAckPropMsgCount(Long replicationStandbyAckedByAckPropMsgCount) {
    this.replicationStandbyAckedByAckPropMsgCount = replicationStandbyAckedByAckPropMsgCount;
  }

  public MsgVpnTopicEndpoint replicationStandbyRxMsgCount(Long replicationStandbyRxMsgCount) {
    this.replicationStandbyRxMsgCount = replicationStandbyRxMsgCount;
    return this;
  }

   /**
   * The number of messages received by the Topic Endpoint from the replication active remote Message VPN.
   * @return replicationStandbyRxMsgCount
  **/
  @Schema(description = "The number of messages received by the Topic Endpoint from the replication active remote Message VPN.")
  public Long getReplicationStandbyRxMsgCount() {
    return replicationStandbyRxMsgCount;
  }

  public void setReplicationStandbyRxMsgCount(Long replicationStandbyRxMsgCount) {
    this.replicationStandbyRxMsgCount = replicationStandbyRxMsgCount;
  }

  public MsgVpnTopicEndpoint respectMsgPriorityEnabled(Boolean respectMsgPriorityEnabled) {
    this.respectMsgPriorityEnabled = respectMsgPriorityEnabled;
    return this;
  }

   /**
   * Indicates whether message priorities are respected. When enabled, messages contained in the Topic Endpoint are delivered in priority order, from 9 (highest) to 0 (lowest).
   * @return respectMsgPriorityEnabled
  **/
  @Schema(description = "Indicates whether message priorities are respected. When enabled, messages contained in the Topic Endpoint are delivered in priority order, from 9 (highest) to 0 (lowest).")
  public Boolean isRespectMsgPriorityEnabled() {
    return respectMsgPriorityEnabled;
  }

  public void setRespectMsgPriorityEnabled(Boolean respectMsgPriorityEnabled) {
    this.respectMsgPriorityEnabled = respectMsgPriorityEnabled;
  }

  public MsgVpnTopicEndpoint respectTtlEnabled(Boolean respectTtlEnabled) {
    this.respectTtlEnabled = respectTtlEnabled;
    return this;
  }

   /**
   * Indicates whether the time-to-live (TTL) for messages in the Topic Endpoint is respected. When enabled, expired messages are discarded or moved to the DMQ.
   * @return respectTtlEnabled
  **/
  @Schema(description = "Indicates whether the time-to-live (TTL) for messages in the Topic Endpoint is respected. When enabled, expired messages are discarded or moved to the DMQ.")
  public Boolean isRespectTtlEnabled() {
    return respectTtlEnabled;
  }

  public void setRespectTtlEnabled(Boolean respectTtlEnabled) {
    this.respectTtlEnabled = respectTtlEnabled;
  }

  public MsgVpnTopicEndpoint rxByteRate(Integer rxByteRate) {
    this.rxByteRate = rxByteRate;
    return this;
  }

   /**
   * The current message rate received by the Topic Endpoint, in bytes per second (B/sec).
   * @return rxByteRate
  **/
  @Schema(description = "The current message rate received by the Topic Endpoint, in bytes per second (B/sec).")
  public Integer getRxByteRate() {
    return rxByteRate;
  }

  public void setRxByteRate(Integer rxByteRate) {
    this.rxByteRate = rxByteRate;
  }

  public MsgVpnTopicEndpoint rxMsgRate(Long rxMsgRate) {
    this.rxMsgRate = rxMsgRate;
    return this;
  }

   /**
   * The current message rate received by the Topic Endpoint, in messages per second (msg/sec).
   * @return rxMsgRate
  **/
  @Schema(description = "The current message rate received by the Topic Endpoint, in messages per second (msg/sec).")
  public Long getRxMsgRate() {
    return rxMsgRate;
  }

  public void setRxMsgRate(Long rxMsgRate) {
    this.rxMsgRate = rxMsgRate;
  }

  public MsgVpnTopicEndpoint rxSelector(Boolean rxSelector) {
    this.rxSelector = rxSelector;
    return this;
  }

   /**
   * Indicates whether the Topic Endpoint has a selector to filter received messages.
   * @return rxSelector
  **/
  @Schema(description = "Indicates whether the Topic Endpoint has a selector to filter received messages.")
  public Boolean isRxSelector() {
    return rxSelector;
  }

  public void setRxSelector(Boolean rxSelector) {
    this.rxSelector = rxSelector;
  }

  public MsgVpnTopicEndpoint selector(String selector) {
    this.selector = selector;
    return this;
  }

   /**
   * The value of the receive selector for the Topic Endpoint.
   * @return selector
  **/
  @Schema(description = "The value of the receive selector for the Topic Endpoint.")
  public String getSelector() {
    return selector;
  }

  public void setSelector(String selector) {
    this.selector = selector;
  }

  public MsgVpnTopicEndpoint selectorExaminedMsgCount(Long selectorExaminedMsgCount) {
    this.selectorExaminedMsgCount = selectorExaminedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages examined by the Topic Endpoint selector.
   * @return selectorExaminedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages examined by the Topic Endpoint selector.")
  public Long getSelectorExaminedMsgCount() {
    return selectorExaminedMsgCount;
  }

  public void setSelectorExaminedMsgCount(Long selectorExaminedMsgCount) {
    this.selectorExaminedMsgCount = selectorExaminedMsgCount;
  }

  public MsgVpnTopicEndpoint selectorMatchedMsgCount(Long selectorMatchedMsgCount) {
    this.selectorMatchedMsgCount = selectorMatchedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages for which the Topic Endpoint selector matched.
   * @return selectorMatchedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages for which the Topic Endpoint selector matched.")
  public Long getSelectorMatchedMsgCount() {
    return selectorMatchedMsgCount;
  }

  public void setSelectorMatchedMsgCount(Long selectorMatchedMsgCount) {
    this.selectorMatchedMsgCount = selectorMatchedMsgCount;
  }

  public MsgVpnTopicEndpoint selectorNotMatchedMsgCount(Long selectorNotMatchedMsgCount) {
    this.selectorNotMatchedMsgCount = selectorNotMatchedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages for which the Topic Endpoint selector did not match.
   * @return selectorNotMatchedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages for which the Topic Endpoint selector did not match.")
  public Long getSelectorNotMatchedMsgCount() {
    return selectorNotMatchedMsgCount;
  }

  public void setSelectorNotMatchedMsgCount(Long selectorNotMatchedMsgCount) {
    this.selectorNotMatchedMsgCount = selectorNotMatchedMsgCount;
  }

  public MsgVpnTopicEndpoint spooledByteCount(Long spooledByteCount) {
    this.spooledByteCount = spooledByteCount;
    return this;
  }

   /**
   * The amount of guaranteed messages that were spooled in the Topic Endpoint, in bytes (B).
   * @return spooledByteCount
  **/
  @Schema(description = "The amount of guaranteed messages that were spooled in the Topic Endpoint, in bytes (B).")
  public Long getSpooledByteCount() {
    return spooledByteCount;
  }

  public void setSpooledByteCount(Long spooledByteCount) {
    this.spooledByteCount = spooledByteCount;
  }

  public MsgVpnTopicEndpoint spooledMsgCount(Long spooledMsgCount) {
    this.spooledMsgCount = spooledMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages that were spooled in the Topic Endpoint.
   * @return spooledMsgCount
  **/
  @Schema(description = "The number of guaranteed messages that were spooled in the Topic Endpoint.")
  public Long getSpooledMsgCount() {
    return spooledMsgCount;
  }

  public void setSpooledMsgCount(Long spooledMsgCount) {
    this.spooledMsgCount = spooledMsgCount;
  }

  public MsgVpnTopicEndpoint topicEndpointName(String topicEndpointName) {
    this.topicEndpointName = topicEndpointName;
    return this;
  }

   /**
   * The name of the Topic Endpoint.
   * @return topicEndpointName
  **/
  @Schema(description = "The name of the Topic Endpoint.")
  public String getTopicEndpointName() {
    return topicEndpointName;
  }

  public void setTopicEndpointName(String topicEndpointName) {
    this.topicEndpointName = topicEndpointName;
  }

  public MsgVpnTopicEndpoint txByteRate(Long txByteRate) {
    this.txByteRate = txByteRate;
    return this;
  }

   /**
   * The current message rate transmitted by the Topic Endpoint, in bytes per second (B/sec).
   * @return txByteRate
  **/
  @Schema(description = "The current message rate transmitted by the Topic Endpoint, in bytes per second (B/sec).")
  public Long getTxByteRate() {
    return txByteRate;
  }

  public void setTxByteRate(Long txByteRate) {
    this.txByteRate = txByteRate;
  }

  public MsgVpnTopicEndpoint txMsgRate(Long txMsgRate) {
    this.txMsgRate = txMsgRate;
    return this;
  }

   /**
   * The current message rate transmitted by the Topic Endpoint, in messages per second (msg/sec).
   * @return txMsgRate
  **/
  @Schema(description = "The current message rate transmitted by the Topic Endpoint, in messages per second (msg/sec).")
  public Long getTxMsgRate() {
    return txMsgRate;
  }

  public void setTxMsgRate(Long txMsgRate) {
    this.txMsgRate = txMsgRate;
  }

  public MsgVpnTopicEndpoint txUnackedMsgCount(Long txUnackedMsgCount) {
    this.txUnackedMsgCount = txUnackedMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages in the Topic Endpoint that have been transmitted but not acknowledged by all consumers.
   * @return txUnackedMsgCount
  **/
  @Schema(description = "The number of guaranteed messages in the Topic Endpoint that have been transmitted but not acknowledged by all consumers.")
  public Long getTxUnackedMsgCount() {
    return txUnackedMsgCount;
  }

  public void setTxUnackedMsgCount(Long txUnackedMsgCount) {
    this.txUnackedMsgCount = txUnackedMsgCount;
  }

  public MsgVpnTopicEndpoint virtualRouter(String virtualRouter) {
    this.virtualRouter = virtualRouter;
    return this;
  }

   /**
   * The virtual router used by the Topic Endpoint. The allowed values and their meaning are:  &lt;pre&gt; \&quot;primary\&quot; - The endpoint belongs to the primary virtual router. \&quot;backup\&quot; - The endpoint belongs to the backup virtual router. &lt;/pre&gt; 
   * @return virtualRouter
  **/
  @Schema(description = "The virtual router used by the Topic Endpoint. The allowed values and their meaning are:  <pre> \"primary\" - The endpoint belongs to the primary virtual router. \"backup\" - The endpoint belongs to the backup virtual router. </pre> ")
  public String getVirtualRouter() {
    return virtualRouter;
  }

  public void setVirtualRouter(String virtualRouter) {
    this.virtualRouter = virtualRouter;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnTopicEndpoint msgVpnTopicEndpoint = (MsgVpnTopicEndpoint) o;
    return Objects.equals(this.accessType, msgVpnTopicEndpoint.accessType) &&
        Objects.equals(this.alreadyBoundBindFailureCount, msgVpnTopicEndpoint.alreadyBoundBindFailureCount) &&
        Objects.equals(this.averageRxByteRate, msgVpnTopicEndpoint.averageRxByteRate) &&
        Objects.equals(this.averageRxMsgRate, msgVpnTopicEndpoint.averageRxMsgRate) &&
        Objects.equals(this.averageTxByteRate, msgVpnTopicEndpoint.averageTxByteRate) &&
        Objects.equals(this.averageTxMsgRate, msgVpnTopicEndpoint.averageTxMsgRate) &&
        Objects.equals(this.bindRequestCount, msgVpnTopicEndpoint.bindRequestCount) &&
        Objects.equals(this.bindSuccessCount, msgVpnTopicEndpoint.bindSuccessCount) &&
        Objects.equals(this.bindTimeForwardingMode, msgVpnTopicEndpoint.bindTimeForwardingMode) &&
        Objects.equals(this.clientProfileDeniedDiscardedMsgCount, msgVpnTopicEndpoint.clientProfileDeniedDiscardedMsgCount) &&
        Objects.equals(this.consumerAckPropagationEnabled, msgVpnTopicEndpoint.consumerAckPropagationEnabled) &&
        Objects.equals(this.createdByManagement, msgVpnTopicEndpoint.createdByManagement) &&
        Objects.equals(this.deadMsgQueue, msgVpnTopicEndpoint.deadMsgQueue) &&
        Objects.equals(this.deletedMsgCount, msgVpnTopicEndpoint.deletedMsgCount) &&
        Objects.equals(this.destinationGroupErrorDiscardedMsgCount, msgVpnTopicEndpoint.destinationGroupErrorDiscardedMsgCount) &&
        Objects.equals(this.destinationTopic, msgVpnTopicEndpoint.destinationTopic) &&
        Objects.equals(this.disabledBindFailureCount, msgVpnTopicEndpoint.disabledBindFailureCount) &&
        Objects.equals(this.disabledDiscardedMsgCount, msgVpnTopicEndpoint.disabledDiscardedMsgCount) &&
        Objects.equals(this.durable, msgVpnTopicEndpoint.durable) &&
        Objects.equals(this.egressEnabled, msgVpnTopicEndpoint.egressEnabled) &&
        Objects.equals(this.eventBindCountThreshold, msgVpnTopicEndpoint.eventBindCountThreshold) &&
        Objects.equals(this.eventRejectLowPriorityMsgLimitThreshold, msgVpnTopicEndpoint.eventRejectLowPriorityMsgLimitThreshold) &&
        Objects.equals(this.eventSpoolUsageThreshold, msgVpnTopicEndpoint.eventSpoolUsageThreshold) &&
        Objects.equals(this.highestAckedMsgId, msgVpnTopicEndpoint.highestAckedMsgId) &&
        Objects.equals(this.highestMsgId, msgVpnTopicEndpoint.highestMsgId) &&
        Objects.equals(this.inProgressAckMsgCount, msgVpnTopicEndpoint.inProgressAckMsgCount) &&
        Objects.equals(this.ingressEnabled, msgVpnTopicEndpoint.ingressEnabled) &&
        Objects.equals(this.invalidSelectorBindFailureCount, msgVpnTopicEndpoint.invalidSelectorBindFailureCount) &&
        Objects.equals(this.lastReplayCompleteTime, msgVpnTopicEndpoint.lastReplayCompleteTime) &&
        Objects.equals(this.lastReplayFailureReason, msgVpnTopicEndpoint.lastReplayFailureReason) &&
        Objects.equals(this.lastReplayFailureTime, msgVpnTopicEndpoint.lastReplayFailureTime) &&
        Objects.equals(this.lastReplayStartTime, msgVpnTopicEndpoint.lastReplayStartTime) &&
        Objects.equals(this.lastReplayedMsgTxTime, msgVpnTopicEndpoint.lastReplayedMsgTxTime) &&
        Objects.equals(this.lastSelectorExaminedMsgId, msgVpnTopicEndpoint.lastSelectorExaminedMsgId) &&
        Objects.equals(this.lastSpooledMsgId, msgVpnTopicEndpoint.lastSpooledMsgId) &&
        Objects.equals(this.lowPriorityMsgCongestionDiscardedMsgCount, msgVpnTopicEndpoint.lowPriorityMsgCongestionDiscardedMsgCount) &&
        Objects.equals(this.lowPriorityMsgCongestionState, msgVpnTopicEndpoint.lowPriorityMsgCongestionState) &&
        Objects.equals(this.lowestAckedMsgId, msgVpnTopicEndpoint.lowestAckedMsgId) &&
        Objects.equals(this.lowestMsgId, msgVpnTopicEndpoint.lowestMsgId) &&
        Objects.equals(this.maxBindCount, msgVpnTopicEndpoint.maxBindCount) &&
        Objects.equals(this.maxBindCountExceededBindFailureCount, msgVpnTopicEndpoint.maxBindCountExceededBindFailureCount) &&
        Objects.equals(this.maxDeliveredUnackedMsgsPerFlow, msgVpnTopicEndpoint.maxDeliveredUnackedMsgsPerFlow) &&
        Objects.equals(this.maxEffectiveBindCount, msgVpnTopicEndpoint.maxEffectiveBindCount) &&
        Objects.equals(this.maxMsgSize, msgVpnTopicEndpoint.maxMsgSize) &&
        Objects.equals(this.maxMsgSizeExceededDiscardedMsgCount, msgVpnTopicEndpoint.maxMsgSizeExceededDiscardedMsgCount) &&
        Objects.equals(this.maxMsgSpoolUsageExceededDiscardedMsgCount, msgVpnTopicEndpoint.maxMsgSpoolUsageExceededDiscardedMsgCount) &&
        Objects.equals(this.maxRedeliveryCount, msgVpnTopicEndpoint.maxRedeliveryCount) &&
        Objects.equals(this.maxRedeliveryExceededDiscardedMsgCount, msgVpnTopicEndpoint.maxRedeliveryExceededDiscardedMsgCount) &&
        Objects.equals(this.maxRedeliveryExceededToDmqFailedMsgCount, msgVpnTopicEndpoint.maxRedeliveryExceededToDmqFailedMsgCount) &&
        Objects.equals(this.maxRedeliveryExceededToDmqMsgCount, msgVpnTopicEndpoint.maxRedeliveryExceededToDmqMsgCount) &&
        Objects.equals(this.maxSpoolUsage, msgVpnTopicEndpoint.maxSpoolUsage) &&
        Objects.equals(this.maxTtl, msgVpnTopicEndpoint.maxTtl) &&
        Objects.equals(this.maxTtlExceededDiscardedMsgCount, msgVpnTopicEndpoint.maxTtlExceededDiscardedMsgCount) &&
        Objects.equals(this.maxTtlExpiredDiscardedMsgCount, msgVpnTopicEndpoint.maxTtlExpiredDiscardedMsgCount) &&
        Objects.equals(this.maxTtlExpiredToDmqFailedMsgCount, msgVpnTopicEndpoint.maxTtlExpiredToDmqFailedMsgCount) &&
        Objects.equals(this.maxTtlExpiredToDmqMsgCount, msgVpnTopicEndpoint.maxTtlExpiredToDmqMsgCount) &&
        Objects.equals(this.msgSpoolPeakUsage, msgVpnTopicEndpoint.msgSpoolPeakUsage) &&
        Objects.equals(this.msgSpoolUsage, msgVpnTopicEndpoint.msgSpoolUsage) &&
        Objects.equals(this.msgVpnName, msgVpnTopicEndpoint.msgVpnName) &&
        Objects.equals(this.networkTopic, msgVpnTopicEndpoint.networkTopic) &&
        Objects.equals(this.noLocalDeliveryDiscardedMsgCount, msgVpnTopicEndpoint.noLocalDeliveryDiscardedMsgCount) &&
        Objects.equals(this.otherBindFailureCount, msgVpnTopicEndpoint.otherBindFailureCount) &&
        Objects.equals(this.owner, msgVpnTopicEndpoint.owner) &&
        Objects.equals(this.permission, msgVpnTopicEndpoint.permission) &&
        Objects.equals(this.redeliveredMsgCount, msgVpnTopicEndpoint.redeliveredMsgCount) &&
        Objects.equals(this.rejectLowPriorityMsgEnabled, msgVpnTopicEndpoint.rejectLowPriorityMsgEnabled) &&
        Objects.equals(this.rejectLowPriorityMsgLimit, msgVpnTopicEndpoint.rejectLowPriorityMsgLimit) &&
        Objects.equals(this.rejectMsgToSenderOnDiscardBehavior, msgVpnTopicEndpoint.rejectMsgToSenderOnDiscardBehavior) &&
        Objects.equals(this.replayFailureCount, msgVpnTopicEndpoint.replayFailureCount) &&
        Objects.equals(this.replayStartCount, msgVpnTopicEndpoint.replayStartCount) &&
        Objects.equals(this.replayState, msgVpnTopicEndpoint.replayState) &&
        Objects.equals(this.replaySuccessCount, msgVpnTopicEndpoint.replaySuccessCount) &&
        Objects.equals(this.replayedAckedMsgCount, msgVpnTopicEndpoint.replayedAckedMsgCount) &&
        Objects.equals(this.replayedTxMsgCount, msgVpnTopicEndpoint.replayedTxMsgCount) &&
        Objects.equals(this.replicationActiveAckPropTxMsgCount, msgVpnTopicEndpoint.replicationActiveAckPropTxMsgCount) &&
        Objects.equals(this.replicationStandbyAckPropRxMsgCount, msgVpnTopicEndpoint.replicationStandbyAckPropRxMsgCount) &&
        Objects.equals(this.replicationStandbyAckedByAckPropMsgCount, msgVpnTopicEndpoint.replicationStandbyAckedByAckPropMsgCount) &&
        Objects.equals(this.replicationStandbyRxMsgCount, msgVpnTopicEndpoint.replicationStandbyRxMsgCount) &&
        Objects.equals(this.respectMsgPriorityEnabled, msgVpnTopicEndpoint.respectMsgPriorityEnabled) &&
        Objects.equals(this.respectTtlEnabled, msgVpnTopicEndpoint.respectTtlEnabled) &&
        Objects.equals(this.rxByteRate, msgVpnTopicEndpoint.rxByteRate) &&
        Objects.equals(this.rxMsgRate, msgVpnTopicEndpoint.rxMsgRate) &&
        Objects.equals(this.rxSelector, msgVpnTopicEndpoint.rxSelector) &&
        Objects.equals(this.selector, msgVpnTopicEndpoint.selector) &&
        Objects.equals(this.selectorExaminedMsgCount, msgVpnTopicEndpoint.selectorExaminedMsgCount) &&
        Objects.equals(this.selectorMatchedMsgCount, msgVpnTopicEndpoint.selectorMatchedMsgCount) &&
        Objects.equals(this.selectorNotMatchedMsgCount, msgVpnTopicEndpoint.selectorNotMatchedMsgCount) &&
        Objects.equals(this.spooledByteCount, msgVpnTopicEndpoint.spooledByteCount) &&
        Objects.equals(this.spooledMsgCount, msgVpnTopicEndpoint.spooledMsgCount) &&
        Objects.equals(this.topicEndpointName, msgVpnTopicEndpoint.topicEndpointName) &&
        Objects.equals(this.txByteRate, msgVpnTopicEndpoint.txByteRate) &&
        Objects.equals(this.txMsgRate, msgVpnTopicEndpoint.txMsgRate) &&
        Objects.equals(this.txUnackedMsgCount, msgVpnTopicEndpoint.txUnackedMsgCount) &&
        Objects.equals(this.virtualRouter, msgVpnTopicEndpoint.virtualRouter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessType, alreadyBoundBindFailureCount, averageRxByteRate, averageRxMsgRate, averageTxByteRate, averageTxMsgRate, bindRequestCount, bindSuccessCount, bindTimeForwardingMode, clientProfileDeniedDiscardedMsgCount, consumerAckPropagationEnabled, createdByManagement, deadMsgQueue, deletedMsgCount, destinationGroupErrorDiscardedMsgCount, destinationTopic, disabledBindFailureCount, disabledDiscardedMsgCount, durable, egressEnabled, eventBindCountThreshold, eventRejectLowPriorityMsgLimitThreshold, eventSpoolUsageThreshold, highestAckedMsgId, highestMsgId, inProgressAckMsgCount, ingressEnabled, invalidSelectorBindFailureCount, lastReplayCompleteTime, lastReplayFailureReason, lastReplayFailureTime, lastReplayStartTime, lastReplayedMsgTxTime, lastSelectorExaminedMsgId, lastSpooledMsgId, lowPriorityMsgCongestionDiscardedMsgCount, lowPriorityMsgCongestionState, lowestAckedMsgId, lowestMsgId, maxBindCount, maxBindCountExceededBindFailureCount, maxDeliveredUnackedMsgsPerFlow, maxEffectiveBindCount, maxMsgSize, maxMsgSizeExceededDiscardedMsgCount, maxMsgSpoolUsageExceededDiscardedMsgCount, maxRedeliveryCount, maxRedeliveryExceededDiscardedMsgCount, maxRedeliveryExceededToDmqFailedMsgCount, maxRedeliveryExceededToDmqMsgCount, maxSpoolUsage, maxTtl, maxTtlExceededDiscardedMsgCount, maxTtlExpiredDiscardedMsgCount, maxTtlExpiredToDmqFailedMsgCount, maxTtlExpiredToDmqMsgCount, msgSpoolPeakUsage, msgSpoolUsage, msgVpnName, networkTopic, noLocalDeliveryDiscardedMsgCount, otherBindFailureCount, owner, permission, redeliveredMsgCount, rejectLowPriorityMsgEnabled, rejectLowPriorityMsgLimit, rejectMsgToSenderOnDiscardBehavior, replayFailureCount, replayStartCount, replayState, replaySuccessCount, replayedAckedMsgCount, replayedTxMsgCount, replicationActiveAckPropTxMsgCount, replicationStandbyAckPropRxMsgCount, replicationStandbyAckedByAckPropMsgCount, replicationStandbyRxMsgCount, respectMsgPriorityEnabled, respectTtlEnabled, rxByteRate, rxMsgRate, rxSelector, selector, selectorExaminedMsgCount, selectorMatchedMsgCount, selectorNotMatchedMsgCount, spooledByteCount, spooledMsgCount, topicEndpointName, txByteRate, txMsgRate, txUnackedMsgCount, virtualRouter);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnTopicEndpoint {\n");
    
    sb.append("    accessType: ").append(toIndentedString(accessType)).append("\n");
    sb.append("    alreadyBoundBindFailureCount: ").append(toIndentedString(alreadyBoundBindFailureCount)).append("\n");
    sb.append("    averageRxByteRate: ").append(toIndentedString(averageRxByteRate)).append("\n");
    sb.append("    averageRxMsgRate: ").append(toIndentedString(averageRxMsgRate)).append("\n");
    sb.append("    averageTxByteRate: ").append(toIndentedString(averageTxByteRate)).append("\n");
    sb.append("    averageTxMsgRate: ").append(toIndentedString(averageTxMsgRate)).append("\n");
    sb.append("    bindRequestCount: ").append(toIndentedString(bindRequestCount)).append("\n");
    sb.append("    bindSuccessCount: ").append(toIndentedString(bindSuccessCount)).append("\n");
    sb.append("    bindTimeForwardingMode: ").append(toIndentedString(bindTimeForwardingMode)).append("\n");
    sb.append("    clientProfileDeniedDiscardedMsgCount: ").append(toIndentedString(clientProfileDeniedDiscardedMsgCount)).append("\n");
    sb.append("    consumerAckPropagationEnabled: ").append(toIndentedString(consumerAckPropagationEnabled)).append("\n");
    sb.append("    createdByManagement: ").append(toIndentedString(createdByManagement)).append("\n");
    sb.append("    deadMsgQueue: ").append(toIndentedString(deadMsgQueue)).append("\n");
    sb.append("    deletedMsgCount: ").append(toIndentedString(deletedMsgCount)).append("\n");
    sb.append("    destinationGroupErrorDiscardedMsgCount: ").append(toIndentedString(destinationGroupErrorDiscardedMsgCount)).append("\n");
    sb.append("    destinationTopic: ").append(toIndentedString(destinationTopic)).append("\n");
    sb.append("    disabledBindFailureCount: ").append(toIndentedString(disabledBindFailureCount)).append("\n");
    sb.append("    disabledDiscardedMsgCount: ").append(toIndentedString(disabledDiscardedMsgCount)).append("\n");
    sb.append("    durable: ").append(toIndentedString(durable)).append("\n");
    sb.append("    egressEnabled: ").append(toIndentedString(egressEnabled)).append("\n");
    sb.append("    eventBindCountThreshold: ").append(toIndentedString(eventBindCountThreshold)).append("\n");
    sb.append("    eventRejectLowPriorityMsgLimitThreshold: ").append(toIndentedString(eventRejectLowPriorityMsgLimitThreshold)).append("\n");
    sb.append("    eventSpoolUsageThreshold: ").append(toIndentedString(eventSpoolUsageThreshold)).append("\n");
    sb.append("    highestAckedMsgId: ").append(toIndentedString(highestAckedMsgId)).append("\n");
    sb.append("    highestMsgId: ").append(toIndentedString(highestMsgId)).append("\n");
    sb.append("    inProgressAckMsgCount: ").append(toIndentedString(inProgressAckMsgCount)).append("\n");
    sb.append("    ingressEnabled: ").append(toIndentedString(ingressEnabled)).append("\n");
    sb.append("    invalidSelectorBindFailureCount: ").append(toIndentedString(invalidSelectorBindFailureCount)).append("\n");
    sb.append("    lastReplayCompleteTime: ").append(toIndentedString(lastReplayCompleteTime)).append("\n");
    sb.append("    lastReplayFailureReason: ").append(toIndentedString(lastReplayFailureReason)).append("\n");
    sb.append("    lastReplayFailureTime: ").append(toIndentedString(lastReplayFailureTime)).append("\n");
    sb.append("    lastReplayStartTime: ").append(toIndentedString(lastReplayStartTime)).append("\n");
    sb.append("    lastReplayedMsgTxTime: ").append(toIndentedString(lastReplayedMsgTxTime)).append("\n");
    sb.append("    lastSelectorExaminedMsgId: ").append(toIndentedString(lastSelectorExaminedMsgId)).append("\n");
    sb.append("    lastSpooledMsgId: ").append(toIndentedString(lastSpooledMsgId)).append("\n");
    sb.append("    lowPriorityMsgCongestionDiscardedMsgCount: ").append(toIndentedString(lowPriorityMsgCongestionDiscardedMsgCount)).append("\n");
    sb.append("    lowPriorityMsgCongestionState: ").append(toIndentedString(lowPriorityMsgCongestionState)).append("\n");
    sb.append("    lowestAckedMsgId: ").append(toIndentedString(lowestAckedMsgId)).append("\n");
    sb.append("    lowestMsgId: ").append(toIndentedString(lowestMsgId)).append("\n");
    sb.append("    maxBindCount: ").append(toIndentedString(maxBindCount)).append("\n");
    sb.append("    maxBindCountExceededBindFailureCount: ").append(toIndentedString(maxBindCountExceededBindFailureCount)).append("\n");
    sb.append("    maxDeliveredUnackedMsgsPerFlow: ").append(toIndentedString(maxDeliveredUnackedMsgsPerFlow)).append("\n");
    sb.append("    maxEffectiveBindCount: ").append(toIndentedString(maxEffectiveBindCount)).append("\n");
    sb.append("    maxMsgSize: ").append(toIndentedString(maxMsgSize)).append("\n");
    sb.append("    maxMsgSizeExceededDiscardedMsgCount: ").append(toIndentedString(maxMsgSizeExceededDiscardedMsgCount)).append("\n");
    sb.append("    maxMsgSpoolUsageExceededDiscardedMsgCount: ").append(toIndentedString(maxMsgSpoolUsageExceededDiscardedMsgCount)).append("\n");
    sb.append("    maxRedeliveryCount: ").append(toIndentedString(maxRedeliveryCount)).append("\n");
    sb.append("    maxRedeliveryExceededDiscardedMsgCount: ").append(toIndentedString(maxRedeliveryExceededDiscardedMsgCount)).append("\n");
    sb.append("    maxRedeliveryExceededToDmqFailedMsgCount: ").append(toIndentedString(maxRedeliveryExceededToDmqFailedMsgCount)).append("\n");
    sb.append("    maxRedeliveryExceededToDmqMsgCount: ").append(toIndentedString(maxRedeliveryExceededToDmqMsgCount)).append("\n");
    sb.append("    maxSpoolUsage: ").append(toIndentedString(maxSpoolUsage)).append("\n");
    sb.append("    maxTtl: ").append(toIndentedString(maxTtl)).append("\n");
    sb.append("    maxTtlExceededDiscardedMsgCount: ").append(toIndentedString(maxTtlExceededDiscardedMsgCount)).append("\n");
    sb.append("    maxTtlExpiredDiscardedMsgCount: ").append(toIndentedString(maxTtlExpiredDiscardedMsgCount)).append("\n");
    sb.append("    maxTtlExpiredToDmqFailedMsgCount: ").append(toIndentedString(maxTtlExpiredToDmqFailedMsgCount)).append("\n");
    sb.append("    maxTtlExpiredToDmqMsgCount: ").append(toIndentedString(maxTtlExpiredToDmqMsgCount)).append("\n");
    sb.append("    msgSpoolPeakUsage: ").append(toIndentedString(msgSpoolPeakUsage)).append("\n");
    sb.append("    msgSpoolUsage: ").append(toIndentedString(msgSpoolUsage)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    networkTopic: ").append(toIndentedString(networkTopic)).append("\n");
    sb.append("    noLocalDeliveryDiscardedMsgCount: ").append(toIndentedString(noLocalDeliveryDiscardedMsgCount)).append("\n");
    sb.append("    otherBindFailureCount: ").append(toIndentedString(otherBindFailureCount)).append("\n");
    sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
    sb.append("    permission: ").append(toIndentedString(permission)).append("\n");
    sb.append("    redeliveredMsgCount: ").append(toIndentedString(redeliveredMsgCount)).append("\n");
    sb.append("    rejectLowPriorityMsgEnabled: ").append(toIndentedString(rejectLowPriorityMsgEnabled)).append("\n");
    sb.append("    rejectLowPriorityMsgLimit: ").append(toIndentedString(rejectLowPriorityMsgLimit)).append("\n");
    sb.append("    rejectMsgToSenderOnDiscardBehavior: ").append(toIndentedString(rejectMsgToSenderOnDiscardBehavior)).append("\n");
    sb.append("    replayFailureCount: ").append(toIndentedString(replayFailureCount)).append("\n");
    sb.append("    replayStartCount: ").append(toIndentedString(replayStartCount)).append("\n");
    sb.append("    replayState: ").append(toIndentedString(replayState)).append("\n");
    sb.append("    replaySuccessCount: ").append(toIndentedString(replaySuccessCount)).append("\n");
    sb.append("    replayedAckedMsgCount: ").append(toIndentedString(replayedAckedMsgCount)).append("\n");
    sb.append("    replayedTxMsgCount: ").append(toIndentedString(replayedTxMsgCount)).append("\n");
    sb.append("    replicationActiveAckPropTxMsgCount: ").append(toIndentedString(replicationActiveAckPropTxMsgCount)).append("\n");
    sb.append("    replicationStandbyAckPropRxMsgCount: ").append(toIndentedString(replicationStandbyAckPropRxMsgCount)).append("\n");
    sb.append("    replicationStandbyAckedByAckPropMsgCount: ").append(toIndentedString(replicationStandbyAckedByAckPropMsgCount)).append("\n");
    sb.append("    replicationStandbyRxMsgCount: ").append(toIndentedString(replicationStandbyRxMsgCount)).append("\n");
    sb.append("    respectMsgPriorityEnabled: ").append(toIndentedString(respectMsgPriorityEnabled)).append("\n");
    sb.append("    respectTtlEnabled: ").append(toIndentedString(respectTtlEnabled)).append("\n");
    sb.append("    rxByteRate: ").append(toIndentedString(rxByteRate)).append("\n");
    sb.append("    rxMsgRate: ").append(toIndentedString(rxMsgRate)).append("\n");
    sb.append("    rxSelector: ").append(toIndentedString(rxSelector)).append("\n");
    sb.append("    selector: ").append(toIndentedString(selector)).append("\n");
    sb.append("    selectorExaminedMsgCount: ").append(toIndentedString(selectorExaminedMsgCount)).append("\n");
    sb.append("    selectorMatchedMsgCount: ").append(toIndentedString(selectorMatchedMsgCount)).append("\n");
    sb.append("    selectorNotMatchedMsgCount: ").append(toIndentedString(selectorNotMatchedMsgCount)).append("\n");
    sb.append("    spooledByteCount: ").append(toIndentedString(spooledByteCount)).append("\n");
    sb.append("    spooledMsgCount: ").append(toIndentedString(spooledMsgCount)).append("\n");
    sb.append("    topicEndpointName: ").append(toIndentedString(topicEndpointName)).append("\n");
    sb.append("    txByteRate: ").append(toIndentedString(txByteRate)).append("\n");
    sb.append("    txMsgRate: ").append(toIndentedString(txMsgRate)).append("\n");
    sb.append("    txUnackedMsgCount: ").append(toIndentedString(txUnackedMsgCount)).append("\n");
    sb.append("    virtualRouter: ").append(toIndentedString(virtualRouter)).append("\n");
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
