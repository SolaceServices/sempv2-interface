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
 * MsgVpnRestDeliveryPointQueueBinding
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-13T23:07:13.589Z")
public class MsgVpnRestDeliveryPointQueueBinding {
  @SerializedName("gatewayReplaceTargetAuthorityEnabled")
  private Boolean gatewayReplaceTargetAuthorityEnabled = null;

  @SerializedName("lastFailureReason")
  private String lastFailureReason = null;

  @SerializedName("lastFailureTime")
  private Integer lastFailureTime = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("postRequestTarget")
  private String postRequestTarget = null;

  @SerializedName("queueBindingName")
  private String queueBindingName = null;

  @SerializedName("restDeliveryPointName")
  private String restDeliveryPointName = null;

  @SerializedName("up")
  private Boolean up = null;

  @SerializedName("uptime")
  private Long uptime = null;

  public MsgVpnRestDeliveryPointQueueBinding gatewayReplaceTargetAuthorityEnabled(Boolean gatewayReplaceTargetAuthorityEnabled) {
    this.gatewayReplaceTargetAuthorityEnabled = gatewayReplaceTargetAuthorityEnabled;
    return this;
  }

   /**
   * Indicates whether the authority for the request-target is replaced with that configured for the REST Consumer remote.
   * @return gatewayReplaceTargetAuthorityEnabled
  **/
  @ApiModelProperty(value = "Indicates whether the authority for the request-target is replaced with that configured for the REST Consumer remote.")
  public Boolean isGatewayReplaceTargetAuthorityEnabled() {
    return gatewayReplaceTargetAuthorityEnabled;
  }

  public void setGatewayReplaceTargetAuthorityEnabled(Boolean gatewayReplaceTargetAuthorityEnabled) {
    this.gatewayReplaceTargetAuthorityEnabled = gatewayReplaceTargetAuthorityEnabled;
  }

  public MsgVpnRestDeliveryPointQueueBinding lastFailureReason(String lastFailureReason) {
    this.lastFailureReason = lastFailureReason;
    return this;
  }

   /**
   * The reason for the last REST Delivery Point queue binding failure.
   * @return lastFailureReason
  **/
  @ApiModelProperty(value = "The reason for the last REST Delivery Point queue binding failure.")
  public String getLastFailureReason() {
    return lastFailureReason;
  }

  public void setLastFailureReason(String lastFailureReason) {
    this.lastFailureReason = lastFailureReason;
  }

  public MsgVpnRestDeliveryPointQueueBinding lastFailureTime(Integer lastFailureTime) {
    this.lastFailureTime = lastFailureTime;
    return this;
  }

   /**
   * The timestamp of the last REST Delivery Point queue binding failure. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return lastFailureTime
  **/
  @ApiModelProperty(value = "The timestamp of the last REST Delivery Point queue binding failure. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getLastFailureTime() {
    return lastFailureTime;
  }

  public void setLastFailureTime(Integer lastFailureTime) {
    this.lastFailureTime = lastFailureTime;
  }

  public MsgVpnRestDeliveryPointQueueBinding msgVpnName(String msgVpnName) {
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

  public MsgVpnRestDeliveryPointQueueBinding postRequestTarget(String postRequestTarget) {
    this.postRequestTarget = postRequestTarget;
    return this;
  }

   /**
   * The POST request-target string being used when sending POST requests to a REST Consumer.
   * @return postRequestTarget
  **/
  @ApiModelProperty(value = "The POST request-target string being used when sending POST requests to a REST Consumer.")
  public String getPostRequestTarget() {
    return postRequestTarget;
  }

  public void setPostRequestTarget(String postRequestTarget) {
    this.postRequestTarget = postRequestTarget;
  }

  public MsgVpnRestDeliveryPointQueueBinding queueBindingName(String queueBindingName) {
    this.queueBindingName = queueBindingName;
    return this;
  }

   /**
   * The name of a queue in the Message VPN.
   * @return queueBindingName
  **/
  @ApiModelProperty(value = "The name of a queue in the Message VPN.")
  public String getQueueBindingName() {
    return queueBindingName;
  }

  public void setQueueBindingName(String queueBindingName) {
    this.queueBindingName = queueBindingName;
  }

  public MsgVpnRestDeliveryPointQueueBinding restDeliveryPointName(String restDeliveryPointName) {
    this.restDeliveryPointName = restDeliveryPointName;
    return this;
  }

   /**
   * The name of the REST Delivery Point.
   * @return restDeliveryPointName
  **/
  @ApiModelProperty(value = "The name of the REST Delivery Point.")
  public String getRestDeliveryPointName() {
    return restDeliveryPointName;
  }

  public void setRestDeliveryPointName(String restDeliveryPointName) {
    this.restDeliveryPointName = restDeliveryPointName;
  }

  public MsgVpnRestDeliveryPointQueueBinding up(Boolean up) {
    this.up = up;
    return this;
  }

   /**
   * Indicates whether the operational state of the REST Delivery Point queue binding is up.
   * @return up
  **/
  @ApiModelProperty(value = "Indicates whether the operational state of the REST Delivery Point queue binding is up.")
  public Boolean isUp() {
    return up;
  }

  public void setUp(Boolean up) {
    this.up = up;
  }

  public MsgVpnRestDeliveryPointQueueBinding uptime(Long uptime) {
    this.uptime = uptime;
    return this;
  }

   /**
   * The amount of time in seconds since the REST Delivery Point queue binding was up.
   * @return uptime
  **/
  @ApiModelProperty(value = "The amount of time in seconds since the REST Delivery Point queue binding was up.")
  public Long getUptime() {
    return uptime;
  }

  public void setUptime(Long uptime) {
    this.uptime = uptime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnRestDeliveryPointQueueBinding msgVpnRestDeliveryPointQueueBinding = (MsgVpnRestDeliveryPointQueueBinding) o;
    return Objects.equals(this.gatewayReplaceTargetAuthorityEnabled, msgVpnRestDeliveryPointQueueBinding.gatewayReplaceTargetAuthorityEnabled) &&
        Objects.equals(this.lastFailureReason, msgVpnRestDeliveryPointQueueBinding.lastFailureReason) &&
        Objects.equals(this.lastFailureTime, msgVpnRestDeliveryPointQueueBinding.lastFailureTime) &&
        Objects.equals(this.msgVpnName, msgVpnRestDeliveryPointQueueBinding.msgVpnName) &&
        Objects.equals(this.postRequestTarget, msgVpnRestDeliveryPointQueueBinding.postRequestTarget) &&
        Objects.equals(this.queueBindingName, msgVpnRestDeliveryPointQueueBinding.queueBindingName) &&
        Objects.equals(this.restDeliveryPointName, msgVpnRestDeliveryPointQueueBinding.restDeliveryPointName) &&
        Objects.equals(this.up, msgVpnRestDeliveryPointQueueBinding.up) &&
        Objects.equals(this.uptime, msgVpnRestDeliveryPointQueueBinding.uptime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gatewayReplaceTargetAuthorityEnabled, lastFailureReason, lastFailureTime, msgVpnName, postRequestTarget, queueBindingName, restDeliveryPointName, up, uptime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnRestDeliveryPointQueueBinding {\n");
    
    sb.append("    gatewayReplaceTargetAuthorityEnabled: ").append(toIndentedString(gatewayReplaceTargetAuthorityEnabled)).append("\n");
    sb.append("    lastFailureReason: ").append(toIndentedString(lastFailureReason)).append("\n");
    sb.append("    lastFailureTime: ").append(toIndentedString(lastFailureTime)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    postRequestTarget: ").append(toIndentedString(postRequestTarget)).append("\n");
    sb.append("    queueBindingName: ").append(toIndentedString(queueBindingName)).append("\n");
    sb.append("    restDeliveryPointName: ").append(toIndentedString(restDeliveryPointName)).append("\n");
    sb.append("    up: ").append(toIndentedString(up)).append("\n");
    sb.append("    uptime: ").append(toIndentedString(uptime)).append("\n");
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

