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
 * MsgVpnClientConnection
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:57:21.016551900+01:00[Europe/London]")
public class MsgVpnClientConnection {
  @SerializedName("clientAddress")
  private String clientAddress = null;

  @SerializedName("clientName")
  private String clientName = null;

  @SerializedName("compression")
  private Boolean compression = null;

  @SerializedName("encryption")
  private Boolean encryption = null;

  @SerializedName("fastRetransmitCount")
  private Integer fastRetransmitCount = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("rxQueueByteCount")
  private Integer rxQueueByteCount = null;

  @SerializedName("segmentReceivedOutOfOrderCount")
  private Integer segmentReceivedOutOfOrderCount = null;

  @SerializedName("smoothedRoundTripTime")
  private Long smoothedRoundTripTime = null;

  @SerializedName("tcpState")
  private String tcpState = null;

  @SerializedName("timedRetransmitCount")
  private Integer timedRetransmitCount = null;

  @SerializedName("txQueueByteCount")
  private Integer txQueueByteCount = null;

  @SerializedName("uptime")
  private Integer uptime = null;

  public MsgVpnClientConnection clientAddress(String clientAddress) {
    this.clientAddress = clientAddress;
    return this;
  }

   /**
   * The IP address and TCP port on the Client side of the Client Connection.
   * @return clientAddress
  **/
  @Schema(description = "The IP address and TCP port on the Client side of the Client Connection.")
  public String getClientAddress() {
    return clientAddress;
  }

  public void setClientAddress(String clientAddress) {
    this.clientAddress = clientAddress;
  }

  public MsgVpnClientConnection clientName(String clientName) {
    this.clientName = clientName;
    return this;
  }

   /**
   * The name of the Client.
   * @return clientName
  **/
  @Schema(description = "The name of the Client.")
  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public MsgVpnClientConnection compression(Boolean compression) {
    this.compression = compression;
    return this;
  }

   /**
   * Indicates whether compression is enabled for the Client Connection.
   * @return compression
  **/
  @Schema(description = "Indicates whether compression is enabled for the Client Connection.")
  public Boolean isCompression() {
    return compression;
  }

  public void setCompression(Boolean compression) {
    this.compression = compression;
  }

  public MsgVpnClientConnection encryption(Boolean encryption) {
    this.encryption = encryption;
    return this;
  }

   /**
   * Indicates whether encryption (TLS) is enabled for the Client Connection.
   * @return encryption
  **/
  @Schema(description = "Indicates whether encryption (TLS) is enabled for the Client Connection.")
  public Boolean isEncryption() {
    return encryption;
  }

  public void setEncryption(Boolean encryption) {
    this.encryption = encryption;
  }

  public MsgVpnClientConnection fastRetransmitCount(Integer fastRetransmitCount) {
    this.fastRetransmitCount = fastRetransmitCount;
    return this;
  }

   /**
   * The number of TCP fast retransmits due to duplicate acknowledgments (ACKs). See RFC 5681 for further details.
   * @return fastRetransmitCount
  **/
  @Schema(description = "The number of TCP fast retransmits due to duplicate acknowledgments (ACKs). See RFC 5681 for further details.")
  public Integer getFastRetransmitCount() {
    return fastRetransmitCount;
  }

  public void setFastRetransmitCount(Integer fastRetransmitCount) {
    this.fastRetransmitCount = fastRetransmitCount;
  }

  public MsgVpnClientConnection msgVpnName(String msgVpnName) {
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

  public MsgVpnClientConnection rxQueueByteCount(Integer rxQueueByteCount) {
    this.rxQueueByteCount = rxQueueByteCount;
    return this;
  }

   /**
   * The number of bytes currently in the receive queue for the Client Connection.
   * @return rxQueueByteCount
  **/
  @Schema(description = "The number of bytes currently in the receive queue for the Client Connection.")
  public Integer getRxQueueByteCount() {
    return rxQueueByteCount;
  }

  public void setRxQueueByteCount(Integer rxQueueByteCount) {
    this.rxQueueByteCount = rxQueueByteCount;
  }

  public MsgVpnClientConnection segmentReceivedOutOfOrderCount(Integer segmentReceivedOutOfOrderCount) {
    this.segmentReceivedOutOfOrderCount = segmentReceivedOutOfOrderCount;
    return this;
  }

   /**
   * The number of TCP segments received from the Client Connection out of order.
   * @return segmentReceivedOutOfOrderCount
  **/
  @Schema(description = "The number of TCP segments received from the Client Connection out of order.")
  public Integer getSegmentReceivedOutOfOrderCount() {
    return segmentReceivedOutOfOrderCount;
  }

  public void setSegmentReceivedOutOfOrderCount(Integer segmentReceivedOutOfOrderCount) {
    this.segmentReceivedOutOfOrderCount = segmentReceivedOutOfOrderCount;
  }

  public MsgVpnClientConnection smoothedRoundTripTime(Long smoothedRoundTripTime) {
    this.smoothedRoundTripTime = smoothedRoundTripTime;
    return this;
  }

   /**
   * The TCP smoothed round-trip time (SRTT) for the Client Connection, in nanoseconds. See RFC 2988 for further details.
   * @return smoothedRoundTripTime
  **/
  @Schema(description = "The TCP smoothed round-trip time (SRTT) for the Client Connection, in nanoseconds. See RFC 2988 for further details.")
  public Long getSmoothedRoundTripTime() {
    return smoothedRoundTripTime;
  }

  public void setSmoothedRoundTripTime(Long smoothedRoundTripTime) {
    this.smoothedRoundTripTime = smoothedRoundTripTime;
  }

  public MsgVpnClientConnection tcpState(String tcpState) {
    this.tcpState = tcpState;
    return this;
  }

   /**
   * The TCP state of the Client Connection. When fully operational, should be: established. See RFC 793 for further details. The allowed values and their meaning are:  &lt;pre&gt; \&quot;closed\&quot; - No connection state at all. \&quot;listen\&quot; - Waiting for a connection request from any remote TCP and port. \&quot;syn-sent\&quot; - Waiting for a matching connection request after having sent a connection request. \&quot;syn-received\&quot; - Waiting for a confirming connection request acknowledgment after having both received and sent a connection request. \&quot;established\&quot; - An open connection, data received can be delivered to the user. \&quot;close-wait\&quot; - Waiting for a connection termination request from the local user. \&quot;fin-wait-1\&quot; - Waiting for a connection termination request from the remote TCP, or an acknowledgment of the connection termination request previously sent. \&quot;closing\&quot; - Waiting for a connection termination request acknowledgment from the remote TCP. \&quot;last-ack\&quot; - Waiting for an acknowledgment of the connection termination request previously sent to the remote TCP. \&quot;fin-wait-2\&quot; - Waiting for a connection termination request from the remote TCP. \&quot;time-wait\&quot; - Waiting for enough time to pass to be sure the remote TCP received the acknowledgment of its connection termination request. &lt;/pre&gt; 
   * @return tcpState
  **/
  @Schema(description = "The TCP state of the Client Connection. When fully operational, should be: established. See RFC 793 for further details. The allowed values and their meaning are:  <pre> \"closed\" - No connection state at all. \"listen\" - Waiting for a connection request from any remote TCP and port. \"syn-sent\" - Waiting for a matching connection request after having sent a connection request. \"syn-received\" - Waiting for a confirming connection request acknowledgment after having both received and sent a connection request. \"established\" - An open connection, data received can be delivered to the user. \"close-wait\" - Waiting for a connection termination request from the local user. \"fin-wait-1\" - Waiting for a connection termination request from the remote TCP, or an acknowledgment of the connection termination request previously sent. \"closing\" - Waiting for a connection termination request acknowledgment from the remote TCP. \"last-ack\" - Waiting for an acknowledgment of the connection termination request previously sent to the remote TCP. \"fin-wait-2\" - Waiting for a connection termination request from the remote TCP. \"time-wait\" - Waiting for enough time to pass to be sure the remote TCP received the acknowledgment of its connection termination request. </pre> ")
  public String getTcpState() {
    return tcpState;
  }

  public void setTcpState(String tcpState) {
    this.tcpState = tcpState;
  }

  public MsgVpnClientConnection timedRetransmitCount(Integer timedRetransmitCount) {
    this.timedRetransmitCount = timedRetransmitCount;
    return this;
  }

   /**
   * The number of TCP segments retransmitted due to timeout awaiting an acknowledgement (ACK). See RFC 793 for further details.
   * @return timedRetransmitCount
  **/
  @Schema(description = "The number of TCP segments retransmitted due to timeout awaiting an acknowledgement (ACK). See RFC 793 for further details.")
  public Integer getTimedRetransmitCount() {
    return timedRetransmitCount;
  }

  public void setTimedRetransmitCount(Integer timedRetransmitCount) {
    this.timedRetransmitCount = timedRetransmitCount;
  }

  public MsgVpnClientConnection txQueueByteCount(Integer txQueueByteCount) {
    this.txQueueByteCount = txQueueByteCount;
    return this;
  }

   /**
   * The number of bytes currently in the transmit queue for the Client Connection.
   * @return txQueueByteCount
  **/
  @Schema(description = "The number of bytes currently in the transmit queue for the Client Connection.")
  public Integer getTxQueueByteCount() {
    return txQueueByteCount;
  }

  public void setTxQueueByteCount(Integer txQueueByteCount) {
    this.txQueueByteCount = txQueueByteCount;
  }

  public MsgVpnClientConnection uptime(Integer uptime) {
    this.uptime = uptime;
    return this;
  }

   /**
   * The amount of time in seconds since the Client Connection was established.
   * @return uptime
  **/
  @Schema(description = "The amount of time in seconds since the Client Connection was established.")
  public Integer getUptime() {
    return uptime;
  }

  public void setUptime(Integer uptime) {
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
    MsgVpnClientConnection msgVpnClientConnection = (MsgVpnClientConnection) o;
    return Objects.equals(this.clientAddress, msgVpnClientConnection.clientAddress) &&
        Objects.equals(this.clientName, msgVpnClientConnection.clientName) &&
        Objects.equals(this.compression, msgVpnClientConnection.compression) &&
        Objects.equals(this.encryption, msgVpnClientConnection.encryption) &&
        Objects.equals(this.fastRetransmitCount, msgVpnClientConnection.fastRetransmitCount) &&
        Objects.equals(this.msgVpnName, msgVpnClientConnection.msgVpnName) &&
        Objects.equals(this.rxQueueByteCount, msgVpnClientConnection.rxQueueByteCount) &&
        Objects.equals(this.segmentReceivedOutOfOrderCount, msgVpnClientConnection.segmentReceivedOutOfOrderCount) &&
        Objects.equals(this.smoothedRoundTripTime, msgVpnClientConnection.smoothedRoundTripTime) &&
        Objects.equals(this.tcpState, msgVpnClientConnection.tcpState) &&
        Objects.equals(this.timedRetransmitCount, msgVpnClientConnection.timedRetransmitCount) &&
        Objects.equals(this.txQueueByteCount, msgVpnClientConnection.txQueueByteCount) &&
        Objects.equals(this.uptime, msgVpnClientConnection.uptime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clientAddress, clientName, compression, encryption, fastRetransmitCount, msgVpnName, rxQueueByteCount, segmentReceivedOutOfOrderCount, smoothedRoundTripTime, tcpState, timedRetransmitCount, txQueueByteCount, uptime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnClientConnection {\n");
    
    sb.append("    clientAddress: ").append(toIndentedString(clientAddress)).append("\n");
    sb.append("    clientName: ").append(toIndentedString(clientName)).append("\n");
    sb.append("    compression: ").append(toIndentedString(compression)).append("\n");
    sb.append("    encryption: ").append(toIndentedString(encryption)).append("\n");
    sb.append("    fastRetransmitCount: ").append(toIndentedString(fastRetransmitCount)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    rxQueueByteCount: ").append(toIndentedString(rxQueueByteCount)).append("\n");
    sb.append("    segmentReceivedOutOfOrderCount: ").append(toIndentedString(segmentReceivedOutOfOrderCount)).append("\n");
    sb.append("    smoothedRoundTripTime: ").append(toIndentedString(smoothedRoundTripTime)).append("\n");
    sb.append("    tcpState: ").append(toIndentedString(tcpState)).append("\n");
    sb.append("    timedRetransmitCount: ").append(toIndentedString(timedRetransmitCount)).append("\n");
    sb.append("    txQueueByteCount: ").append(toIndentedString(txQueueByteCount)).append("\n");
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
