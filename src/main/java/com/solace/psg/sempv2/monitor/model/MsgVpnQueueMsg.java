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
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * MsgVpnQueueMsg
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:57:21.016551900+01:00[Europe/London]")
public class MsgVpnQueueMsg {
  @SerializedName("attachmentSize")
  private Long attachmentSize = null;

  @SerializedName("contentSize")
  private Long contentSize = null;

  @SerializedName("dmqEligible")
  private Boolean dmqEligible = null;

  @SerializedName("expiryTime")
  private Integer expiryTime = null;

  @SerializedName("msgId")
  private Long msgId = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("priority")
  private Integer priority = null;

  @SerializedName("publisherId")
  private Long publisherId = null;

  @SerializedName("queueName")
  private String queueName = null;

  @SerializedName("redeliveryCount")
  private Integer redeliveryCount = null;

  @SerializedName("replicatedMateMsgId")
  private Long replicatedMateMsgId = null;

  @SerializedName("replicationState")
  private String replicationState = null;

  @SerializedName("spooledTime")
  private Integer spooledTime = null;

  @SerializedName("undelivered")
  private Boolean undelivered = null;

  public MsgVpnQueueMsg attachmentSize(Long attachmentSize) {
    this.attachmentSize = attachmentSize;
    return this;
  }

   /**
   * The size of the Message attachment, in bytes (B).
   * @return attachmentSize
  **/
  @Schema(description = "The size of the Message attachment, in bytes (B).")
  public Long getAttachmentSize() {
    return attachmentSize;
  }

  public void setAttachmentSize(Long attachmentSize) {
    this.attachmentSize = attachmentSize;
  }

  public MsgVpnQueueMsg contentSize(Long contentSize) {
    this.contentSize = contentSize;
    return this;
  }

   /**
   * The size of the Message content, in bytes (B).
   * @return contentSize
  **/
  @Schema(description = "The size of the Message content, in bytes (B).")
  public Long getContentSize() {
    return contentSize;
  }

  public void setContentSize(Long contentSize) {
    this.contentSize = contentSize;
  }

  public MsgVpnQueueMsg dmqEligible(Boolean dmqEligible) {
    this.dmqEligible = dmqEligible;
    return this;
  }

   /**
   * Indicates whether the Message is eligible for the Dead Message Queue (DMQ).
   * @return dmqEligible
  **/
  @Schema(description = "Indicates whether the Message is eligible for the Dead Message Queue (DMQ).")
  public Boolean isDmqEligible() {
    return dmqEligible;
  }

  public void setDmqEligible(Boolean dmqEligible) {
    this.dmqEligible = dmqEligible;
  }

  public MsgVpnQueueMsg expiryTime(Integer expiryTime) {
    this.expiryTime = expiryTime;
    return this;
  }

   /**
   * The timestamp of when the Message expires. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return expiryTime
  **/
  @Schema(description = "The timestamp of when the Message expires. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getExpiryTime() {
    return expiryTime;
  }

  public void setExpiryTime(Integer expiryTime) {
    this.expiryTime = expiryTime;
  }

  public MsgVpnQueueMsg msgId(Long msgId) {
    this.msgId = msgId;
    return this;
  }

   /**
   * The identifier (ID) of the Message.
   * @return msgId
  **/
  @Schema(description = "The identifier (ID) of the Message.")
  public Long getMsgId() {
    return msgId;
  }

  public void setMsgId(Long msgId) {
    this.msgId = msgId;
  }

  public MsgVpnQueueMsg msgVpnName(String msgVpnName) {
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

  public MsgVpnQueueMsg priority(Integer priority) {
    this.priority = priority;
    return this;
  }

   /**
   * The priority level of the Message, from 9 (highest) to 0 (lowest).
   * @return priority
  **/
  @Schema(description = "The priority level of the Message, from 9 (highest) to 0 (lowest).")
  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public MsgVpnQueueMsg publisherId(Long publisherId) {
    this.publisherId = publisherId;
    return this;
  }

   /**
   * The identifier (ID) of the Message publisher.
   * @return publisherId
  **/
  @Schema(description = "The identifier (ID) of the Message publisher.")
  public Long getPublisherId() {
    return publisherId;
  }

  public void setPublisherId(Long publisherId) {
    this.publisherId = publisherId;
  }

  public MsgVpnQueueMsg queueName(String queueName) {
    this.queueName = queueName;
    return this;
  }

   /**
   * The name of the Queue.
   * @return queueName
  **/
  @Schema(description = "The name of the Queue.")
  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public MsgVpnQueueMsg redeliveryCount(Integer redeliveryCount) {
    this.redeliveryCount = redeliveryCount;
    return this;
  }

   /**
   * The number of times the Message has been redelivered.
   * @return redeliveryCount
  **/
  @Schema(description = "The number of times the Message has been redelivered.")
  public Integer getRedeliveryCount() {
    return redeliveryCount;
  }

  public void setRedeliveryCount(Integer redeliveryCount) {
    this.redeliveryCount = redeliveryCount;
  }

  public MsgVpnQueueMsg replicatedMateMsgId(Long replicatedMateMsgId) {
    this.replicatedMateMsgId = replicatedMateMsgId;
    return this;
  }

   /**
   * The Message identifier (ID) on the replication mate. Applicable only to replicated messages.
   * @return replicatedMateMsgId
  **/
  @Schema(description = "The Message identifier (ID) on the replication mate. Applicable only to replicated messages.")
  public Long getReplicatedMateMsgId() {
    return replicatedMateMsgId;
  }

  public void setReplicatedMateMsgId(Long replicatedMateMsgId) {
    this.replicatedMateMsgId = replicatedMateMsgId;
  }

  public MsgVpnQueueMsg replicationState(String replicationState) {
    this.replicationState = replicationState;
    return this;
  }

   /**
   * The replication state of the Message. The allowed values and their meaning are:  &lt;pre&gt; \&quot;replicated\&quot; - The Message is replicated to the remote Message VPN. \&quot;not-replicated\&quot; - The Message is not being replicated to the remote Message VPN. \&quot;pending-replication\&quot; - The Message is queued for replication to the remote Message VPN. &lt;/pre&gt; 
   * @return replicationState
  **/
  @Schema(description = "The replication state of the Message. The allowed values and their meaning are:  <pre> \"replicated\" - The Message is replicated to the remote Message VPN. \"not-replicated\" - The Message is not being replicated to the remote Message VPN. \"pending-replication\" - The Message is queued for replication to the remote Message VPN. </pre> ")
  public String getReplicationState() {
    return replicationState;
  }

  public void setReplicationState(String replicationState) {
    this.replicationState = replicationState;
  }

  public MsgVpnQueueMsg spooledTime(Integer spooledTime) {
    this.spooledTime = spooledTime;
    return this;
  }

   /**
   * The timestamp of when the Message was spooled in the Queue. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return spooledTime
  **/
  @Schema(description = "The timestamp of when the Message was spooled in the Queue. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getSpooledTime() {
    return spooledTime;
  }

  public void setSpooledTime(Integer spooledTime) {
    this.spooledTime = spooledTime;
  }

  public MsgVpnQueueMsg undelivered(Boolean undelivered) {
    this.undelivered = undelivered;
    return this;
  }

   /**
   * Indicates whether delivery of the Message has never been attempted.
   * @return undelivered
  **/
  @Schema(description = "Indicates whether delivery of the Message has never been attempted.")
  public Boolean isUndelivered() {
    return undelivered;
  }

  public void setUndelivered(Boolean undelivered) {
    this.undelivered = undelivered;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnQueueMsg msgVpnQueueMsg = (MsgVpnQueueMsg) o;
    return Objects.equals(this.attachmentSize, msgVpnQueueMsg.attachmentSize) &&
        Objects.equals(this.contentSize, msgVpnQueueMsg.contentSize) &&
        Objects.equals(this.dmqEligible, msgVpnQueueMsg.dmqEligible) &&
        Objects.equals(this.expiryTime, msgVpnQueueMsg.expiryTime) &&
        Objects.equals(this.msgId, msgVpnQueueMsg.msgId) &&
        Objects.equals(this.msgVpnName, msgVpnQueueMsg.msgVpnName) &&
        Objects.equals(this.priority, msgVpnQueueMsg.priority) &&
        Objects.equals(this.publisherId, msgVpnQueueMsg.publisherId) &&
        Objects.equals(this.queueName, msgVpnQueueMsg.queueName) &&
        Objects.equals(this.redeliveryCount, msgVpnQueueMsg.redeliveryCount) &&
        Objects.equals(this.replicatedMateMsgId, msgVpnQueueMsg.replicatedMateMsgId) &&
        Objects.equals(this.replicationState, msgVpnQueueMsg.replicationState) &&
        Objects.equals(this.spooledTime, msgVpnQueueMsg.spooledTime) &&
        Objects.equals(this.undelivered, msgVpnQueueMsg.undelivered);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attachmentSize, contentSize, dmqEligible, expiryTime, msgId, msgVpnName, priority, publisherId, queueName, redeliveryCount, replicatedMateMsgId, replicationState, spooledTime, undelivered);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnQueueMsg {\n");
    
    sb.append("    attachmentSize: ").append(toIndentedString(attachmentSize)).append("\n");
    sb.append("    contentSize: ").append(toIndentedString(contentSize)).append("\n");
    sb.append("    dmqEligible: ").append(toIndentedString(dmqEligible)).append("\n");
    sb.append("    expiryTime: ").append(toIndentedString(expiryTime)).append("\n");
    sb.append("    msgId: ").append(toIndentedString(msgId)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    publisherId: ").append(toIndentedString(publisherId)).append("\n");
    sb.append("    queueName: ").append(toIndentedString(queueName)).append("\n");
    sb.append("    redeliveryCount: ").append(toIndentedString(redeliveryCount)).append("\n");
    sb.append("    replicatedMateMsgId: ").append(toIndentedString(replicatedMateMsgId)).append("\n");
    sb.append("    replicationState: ").append(toIndentedString(replicationState)).append("\n");
    sb.append("    spooledTime: ").append(toIndentedString(spooledTime)).append("\n");
    sb.append("    undelivered: ").append(toIndentedString(undelivered)).append("\n");
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
