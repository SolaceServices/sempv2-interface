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
 * DmrClusterLinkChannel
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-13T23:07:13.589Z")
public class DmrClusterLinkChannel {
  @SerializedName("bridgeName")
  private String bridgeName = null;

  @SerializedName("clientName")
  private String clientName = null;

  @SerializedName("dmrClusterName")
  private String dmrClusterName = null;

  @SerializedName("establisher")
  private Boolean establisher = null;

  @SerializedName("failureReason")
  private String failureReason = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("queueName")
  private String queueName = null;

  @SerializedName("remoteAddress")
  private String remoteAddress = null;

  @SerializedName("remoteNodeName")
  private String remoteNodeName = null;

  @SerializedName("up")
  private Boolean up = null;

  @SerializedName("uptime")
  private Long uptime = null;

  public DmrClusterLinkChannel bridgeName(String bridgeName) {
    this.bridgeName = bridgeName;
    return this;
  }

   /**
   * The name of the Bridge used by the Channel.
   * @return bridgeName
  **/
  @ApiModelProperty(value = "The name of the Bridge used by the Channel.")
  public String getBridgeName() {
    return bridgeName;
  }

  public void setBridgeName(String bridgeName) {
    this.bridgeName = bridgeName;
  }

  public DmrClusterLinkChannel clientName(String clientName) {
    this.clientName = clientName;
    return this;
  }

   /**
   * The name of the Client used by the Channel.
   * @return clientName
  **/
  @ApiModelProperty(value = "The name of the Client used by the Channel.")
  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public DmrClusterLinkChannel dmrClusterName(String dmrClusterName) {
    this.dmrClusterName = dmrClusterName;
    return this;
  }

   /**
   * The name of the Cluster.
   * @return dmrClusterName
  **/
  @ApiModelProperty(value = "The name of the Cluster.")
  public String getDmrClusterName() {
    return dmrClusterName;
  }

  public void setDmrClusterName(String dmrClusterName) {
    this.dmrClusterName = dmrClusterName;
  }

  public DmrClusterLinkChannel establisher(Boolean establisher) {
    this.establisher = establisher;
    return this;
  }

   /**
   * Indicates whether the local node established the Channel.
   * @return establisher
  **/
  @ApiModelProperty(value = "Indicates whether the local node established the Channel.")
  public Boolean isEstablisher() {
    return establisher;
  }

  public void setEstablisher(Boolean establisher) {
    this.establisher = establisher;
  }

  public DmrClusterLinkChannel failureReason(String failureReason) {
    this.failureReason = failureReason;
    return this;
  }

   /**
   * The failure reason for the Channel being down.
   * @return failureReason
  **/
  @ApiModelProperty(value = "The failure reason for the Channel being down.")
  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public DmrClusterLinkChannel msgVpnName(String msgVpnName) {
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

  public DmrClusterLinkChannel queueName(String queueName) {
    this.queueName = queueName;
    return this;
  }

   /**
   * The name of the Queue used by the Channel.
   * @return queueName
  **/
  @ApiModelProperty(value = "The name of the Queue used by the Channel.")
  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  public DmrClusterLinkChannel remoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

   /**
   * The FQDN or IP address (and optional port) of the remote node.
   * @return remoteAddress
  **/
  @ApiModelProperty(value = "The FQDN or IP address (and optional port) of the remote node.")
  public String getRemoteAddress() {
    return remoteAddress;
  }

  public void setRemoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
  }

  public DmrClusterLinkChannel remoteNodeName(String remoteNodeName) {
    this.remoteNodeName = remoteNodeName;
    return this;
  }

   /**
   * The name of the node at the remote end of the Link.
   * @return remoteNodeName
  **/
  @ApiModelProperty(value = "The name of the node at the remote end of the Link.")
  public String getRemoteNodeName() {
    return remoteNodeName;
  }

  public void setRemoteNodeName(String remoteNodeName) {
    this.remoteNodeName = remoteNodeName;
  }

  public DmrClusterLinkChannel up(Boolean up) {
    this.up = up;
    return this;
  }

   /**
   * Indicates whether the Channel is operationally up.
   * @return up
  **/
  @ApiModelProperty(value = "Indicates whether the Channel is operationally up.")
  public Boolean isUp() {
    return up;
  }

  public void setUp(Boolean up) {
    this.up = up;
  }

  public DmrClusterLinkChannel uptime(Long uptime) {
    this.uptime = uptime;
    return this;
  }

   /**
   * The amount of time in seconds since the Channel was up.
   * @return uptime
  **/
  @ApiModelProperty(value = "The amount of time in seconds since the Channel was up.")
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
    DmrClusterLinkChannel dmrClusterLinkChannel = (DmrClusterLinkChannel) o;
    return Objects.equals(this.bridgeName, dmrClusterLinkChannel.bridgeName) &&
        Objects.equals(this.clientName, dmrClusterLinkChannel.clientName) &&
        Objects.equals(this.dmrClusterName, dmrClusterLinkChannel.dmrClusterName) &&
        Objects.equals(this.establisher, dmrClusterLinkChannel.establisher) &&
        Objects.equals(this.failureReason, dmrClusterLinkChannel.failureReason) &&
        Objects.equals(this.msgVpnName, dmrClusterLinkChannel.msgVpnName) &&
        Objects.equals(this.queueName, dmrClusterLinkChannel.queueName) &&
        Objects.equals(this.remoteAddress, dmrClusterLinkChannel.remoteAddress) &&
        Objects.equals(this.remoteNodeName, dmrClusterLinkChannel.remoteNodeName) &&
        Objects.equals(this.up, dmrClusterLinkChannel.up) &&
        Objects.equals(this.uptime, dmrClusterLinkChannel.uptime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bridgeName, clientName, dmrClusterName, establisher, failureReason, msgVpnName, queueName, remoteAddress, remoteNodeName, up, uptime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DmrClusterLinkChannel {\n");
    
    sb.append("    bridgeName: ").append(toIndentedString(bridgeName)).append("\n");
    sb.append("    clientName: ").append(toIndentedString(clientName)).append("\n");
    sb.append("    dmrClusterName: ").append(toIndentedString(dmrClusterName)).append("\n");
    sb.append("    establisher: ").append(toIndentedString(establisher)).append("\n");
    sb.append("    failureReason: ").append(toIndentedString(failureReason)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    queueName: ").append(toIndentedString(queueName)).append("\n");
    sb.append("    remoteAddress: ").append(toIndentedString(remoteAddress)).append("\n");
    sb.append("    remoteNodeName: ").append(toIndentedString(remoteNodeName)).append("\n");
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

