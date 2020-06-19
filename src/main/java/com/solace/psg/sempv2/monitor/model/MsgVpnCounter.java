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
 * The counters for the Message VPN. Deprecated since 2.14. All attributes in this object have been moved to the MsgVpn object.
 */
@ApiModel(description = "The counters for the Message VPN. Deprecated since 2.14. All attributes in this object have been moved to the MsgVpn object.")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-13T23:07:13.589Z")
public class MsgVpnCounter {
  @SerializedName("controlRxByteCount")
  private Long controlRxByteCount = null;

  @SerializedName("controlRxMsgCount")
  private Long controlRxMsgCount = null;

  @SerializedName("controlTxByteCount")
  private Long controlTxByteCount = null;

  @SerializedName("controlTxMsgCount")
  private Long controlTxMsgCount = null;

  @SerializedName("dataRxByteCount")
  private Long dataRxByteCount = null;

  @SerializedName("dataRxMsgCount")
  private Long dataRxMsgCount = null;

  @SerializedName("dataTxByteCount")
  private Long dataTxByteCount = null;

  @SerializedName("dataTxMsgCount")
  private Long dataTxMsgCount = null;

  @SerializedName("discardedRxMsgCount")
  private Integer discardedRxMsgCount = null;

  @SerializedName("discardedTxMsgCount")
  private Integer discardedTxMsgCount = null;

  @SerializedName("loginRxMsgCount")
  private Long loginRxMsgCount = null;

  @SerializedName("loginTxMsgCount")
  private Long loginTxMsgCount = null;

  @SerializedName("msgSpoolRxMsgCount")
  private Long msgSpoolRxMsgCount = null;

  @SerializedName("msgSpoolTxMsgCount")
  private Long msgSpoolTxMsgCount = null;

  @SerializedName("tlsRxByteCount")
  private Integer tlsRxByteCount = null;

  @SerializedName("tlsTxByteCount")
  private Long tlsTxByteCount = null;

  public MsgVpnCounter controlRxByteCount(Long controlRxByteCount) {
    this.controlRxByteCount = controlRxByteCount;
    return this;
  }

   /**
   * The amount of client control messages received from clients by the Message VPN, in bytes (B). Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return controlRxByteCount
  **/
  @ApiModelProperty(value = "The amount of client control messages received from clients by the Message VPN, in bytes (B). Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getControlRxByteCount() {
    return controlRxByteCount;
  }

  public void setControlRxByteCount(Long controlRxByteCount) {
    this.controlRxByteCount = controlRxByteCount;
  }

  public MsgVpnCounter controlRxMsgCount(Long controlRxMsgCount) {
    this.controlRxMsgCount = controlRxMsgCount;
    return this;
  }

   /**
   * The number of client control messages received from clients by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return controlRxMsgCount
  **/
  @ApiModelProperty(value = "The number of client control messages received from clients by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getControlRxMsgCount() {
    return controlRxMsgCount;
  }

  public void setControlRxMsgCount(Long controlRxMsgCount) {
    this.controlRxMsgCount = controlRxMsgCount;
  }

  public MsgVpnCounter controlTxByteCount(Long controlTxByteCount) {
    this.controlTxByteCount = controlTxByteCount;
    return this;
  }

   /**
   * The amount of client control messages transmitted to clients by the Message VPN, in bytes (B). Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return controlTxByteCount
  **/
  @ApiModelProperty(value = "The amount of client control messages transmitted to clients by the Message VPN, in bytes (B). Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getControlTxByteCount() {
    return controlTxByteCount;
  }

  public void setControlTxByteCount(Long controlTxByteCount) {
    this.controlTxByteCount = controlTxByteCount;
  }

  public MsgVpnCounter controlTxMsgCount(Long controlTxMsgCount) {
    this.controlTxMsgCount = controlTxMsgCount;
    return this;
  }

   /**
   * The number of client control messages transmitted to clients by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return controlTxMsgCount
  **/
  @ApiModelProperty(value = "The number of client control messages transmitted to clients by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getControlTxMsgCount() {
    return controlTxMsgCount;
  }

  public void setControlTxMsgCount(Long controlTxMsgCount) {
    this.controlTxMsgCount = controlTxMsgCount;
  }

  public MsgVpnCounter dataRxByteCount(Long dataRxByteCount) {
    this.dataRxByteCount = dataRxByteCount;
    return this;
  }

   /**
   * The amount of client data messages received from clients by the Message VPN, in bytes (B). Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return dataRxByteCount
  **/
  @ApiModelProperty(value = "The amount of client data messages received from clients by the Message VPN, in bytes (B). Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getDataRxByteCount() {
    return dataRxByteCount;
  }

  public void setDataRxByteCount(Long dataRxByteCount) {
    this.dataRxByteCount = dataRxByteCount;
  }

  public MsgVpnCounter dataRxMsgCount(Long dataRxMsgCount) {
    this.dataRxMsgCount = dataRxMsgCount;
    return this;
  }

   /**
   * The number of client data messages received from clients by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return dataRxMsgCount
  **/
  @ApiModelProperty(value = "The number of client data messages received from clients by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getDataRxMsgCount() {
    return dataRxMsgCount;
  }

  public void setDataRxMsgCount(Long dataRxMsgCount) {
    this.dataRxMsgCount = dataRxMsgCount;
  }

  public MsgVpnCounter dataTxByteCount(Long dataTxByteCount) {
    this.dataTxByteCount = dataTxByteCount;
    return this;
  }

   /**
   * The amount of client data messages transmitted to clients by the Message VPN, in bytes (B). Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return dataTxByteCount
  **/
  @ApiModelProperty(value = "The amount of client data messages transmitted to clients by the Message VPN, in bytes (B). Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getDataTxByteCount() {
    return dataTxByteCount;
  }

  public void setDataTxByteCount(Long dataTxByteCount) {
    this.dataTxByteCount = dataTxByteCount;
  }

  public MsgVpnCounter dataTxMsgCount(Long dataTxMsgCount) {
    this.dataTxMsgCount = dataTxMsgCount;
    return this;
  }

   /**
   * The number of client data messages transmitted to clients by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return dataTxMsgCount
  **/
  @ApiModelProperty(value = "The number of client data messages transmitted to clients by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getDataTxMsgCount() {
    return dataTxMsgCount;
  }

  public void setDataTxMsgCount(Long dataTxMsgCount) {
    this.dataTxMsgCount = dataTxMsgCount;
  }

  public MsgVpnCounter discardedRxMsgCount(Integer discardedRxMsgCount) {
    this.discardedRxMsgCount = discardedRxMsgCount;
    return this;
  }

   /**
   * The number of messages discarded during reception by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return discardedRxMsgCount
  **/
  @ApiModelProperty(value = "The number of messages discarded during reception by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Integer getDiscardedRxMsgCount() {
    return discardedRxMsgCount;
  }

  public void setDiscardedRxMsgCount(Integer discardedRxMsgCount) {
    this.discardedRxMsgCount = discardedRxMsgCount;
  }

  public MsgVpnCounter discardedTxMsgCount(Integer discardedTxMsgCount) {
    this.discardedTxMsgCount = discardedTxMsgCount;
    return this;
  }

   /**
   * The number of messages discarded during transmission by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return discardedTxMsgCount
  **/
  @ApiModelProperty(value = "The number of messages discarded during transmission by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Integer getDiscardedTxMsgCount() {
    return discardedTxMsgCount;
  }

  public void setDiscardedTxMsgCount(Integer discardedTxMsgCount) {
    this.discardedTxMsgCount = discardedTxMsgCount;
  }

  public MsgVpnCounter loginRxMsgCount(Long loginRxMsgCount) {
    this.loginRxMsgCount = loginRxMsgCount;
    return this;
  }

   /**
   * The number of login request messages received by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return loginRxMsgCount
  **/
  @ApiModelProperty(value = "The number of login request messages received by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getLoginRxMsgCount() {
    return loginRxMsgCount;
  }

  public void setLoginRxMsgCount(Long loginRxMsgCount) {
    this.loginRxMsgCount = loginRxMsgCount;
  }

  public MsgVpnCounter loginTxMsgCount(Long loginTxMsgCount) {
    this.loginTxMsgCount = loginTxMsgCount;
    return this;
  }

   /**
   * The number of login response messages transmitted by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return loginTxMsgCount
  **/
  @ApiModelProperty(value = "The number of login response messages transmitted by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getLoginTxMsgCount() {
    return loginTxMsgCount;
  }

  public void setLoginTxMsgCount(Long loginTxMsgCount) {
    this.loginTxMsgCount = loginTxMsgCount;
  }

  public MsgVpnCounter msgSpoolRxMsgCount(Long msgSpoolRxMsgCount) {
    this.msgSpoolRxMsgCount = msgSpoolRxMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages received by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return msgSpoolRxMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages received by the Message VPN. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getMsgSpoolRxMsgCount() {
    return msgSpoolRxMsgCount;
  }

  public void setMsgSpoolRxMsgCount(Long msgSpoolRxMsgCount) {
    this.msgSpoolRxMsgCount = msgSpoolRxMsgCount;
  }

  public MsgVpnCounter msgSpoolTxMsgCount(Long msgSpoolTxMsgCount) {
    this.msgSpoolTxMsgCount = msgSpoolTxMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages transmitted by the Message VPN. One message to multiple clients is counted as one message. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return msgSpoolTxMsgCount
  **/
  @ApiModelProperty(value = "The number of guaranteed messages transmitted by the Message VPN. One message to multiple clients is counted as one message. Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getMsgSpoolTxMsgCount() {
    return msgSpoolTxMsgCount;
  }

  public void setMsgSpoolTxMsgCount(Long msgSpoolTxMsgCount) {
    this.msgSpoolTxMsgCount = msgSpoolTxMsgCount;
  }

  public MsgVpnCounter tlsRxByteCount(Integer tlsRxByteCount) {
    this.tlsRxByteCount = tlsRxByteCount;
    return this;
  }

   /**
   * The amount of TLS messages received by the Message VPN, in bytes (B). Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return tlsRxByteCount
  **/
  @ApiModelProperty(value = "The amount of TLS messages received by the Message VPN, in bytes (B). Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Integer getTlsRxByteCount() {
    return tlsRxByteCount;
  }

  public void setTlsRxByteCount(Integer tlsRxByteCount) {
    this.tlsRxByteCount = tlsRxByteCount;
  }

  public MsgVpnCounter tlsTxByteCount(Long tlsTxByteCount) {
    this.tlsTxByteCount = tlsTxByteCount;
    return this;
  }

   /**
   * The amount of TLS messages transmitted by the Message VPN, in bytes (B). Deprecated since 2.14. This attribute has been moved to the MsgVpn object.
   * @return tlsTxByteCount
  **/
  @ApiModelProperty(value = "The amount of TLS messages transmitted by the Message VPN, in bytes (B). Deprecated since 2.14. This attribute has been moved to the MsgVpn object.")
  public Long getTlsTxByteCount() {
    return tlsTxByteCount;
  }

  public void setTlsTxByteCount(Long tlsTxByteCount) {
    this.tlsTxByteCount = tlsTxByteCount;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnCounter msgVpnCounter = (MsgVpnCounter) o;
    return Objects.equals(this.controlRxByteCount, msgVpnCounter.controlRxByteCount) &&
        Objects.equals(this.controlRxMsgCount, msgVpnCounter.controlRxMsgCount) &&
        Objects.equals(this.controlTxByteCount, msgVpnCounter.controlTxByteCount) &&
        Objects.equals(this.controlTxMsgCount, msgVpnCounter.controlTxMsgCount) &&
        Objects.equals(this.dataRxByteCount, msgVpnCounter.dataRxByteCount) &&
        Objects.equals(this.dataRxMsgCount, msgVpnCounter.dataRxMsgCount) &&
        Objects.equals(this.dataTxByteCount, msgVpnCounter.dataTxByteCount) &&
        Objects.equals(this.dataTxMsgCount, msgVpnCounter.dataTxMsgCount) &&
        Objects.equals(this.discardedRxMsgCount, msgVpnCounter.discardedRxMsgCount) &&
        Objects.equals(this.discardedTxMsgCount, msgVpnCounter.discardedTxMsgCount) &&
        Objects.equals(this.loginRxMsgCount, msgVpnCounter.loginRxMsgCount) &&
        Objects.equals(this.loginTxMsgCount, msgVpnCounter.loginTxMsgCount) &&
        Objects.equals(this.msgSpoolRxMsgCount, msgVpnCounter.msgSpoolRxMsgCount) &&
        Objects.equals(this.msgSpoolTxMsgCount, msgVpnCounter.msgSpoolTxMsgCount) &&
        Objects.equals(this.tlsRxByteCount, msgVpnCounter.tlsRxByteCount) &&
        Objects.equals(this.tlsTxByteCount, msgVpnCounter.tlsTxByteCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(controlRxByteCount, controlRxMsgCount, controlTxByteCount, controlTxMsgCount, dataRxByteCount, dataRxMsgCount, dataTxByteCount, dataTxMsgCount, discardedRxMsgCount, discardedTxMsgCount, loginRxMsgCount, loginTxMsgCount, msgSpoolRxMsgCount, msgSpoolTxMsgCount, tlsRxByteCount, tlsTxByteCount);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnCounter {\n");
    
    sb.append("    controlRxByteCount: ").append(toIndentedString(controlRxByteCount)).append("\n");
    sb.append("    controlRxMsgCount: ").append(toIndentedString(controlRxMsgCount)).append("\n");
    sb.append("    controlTxByteCount: ").append(toIndentedString(controlTxByteCount)).append("\n");
    sb.append("    controlTxMsgCount: ").append(toIndentedString(controlTxMsgCount)).append("\n");
    sb.append("    dataRxByteCount: ").append(toIndentedString(dataRxByteCount)).append("\n");
    sb.append("    dataRxMsgCount: ").append(toIndentedString(dataRxMsgCount)).append("\n");
    sb.append("    dataTxByteCount: ").append(toIndentedString(dataTxByteCount)).append("\n");
    sb.append("    dataTxMsgCount: ").append(toIndentedString(dataTxMsgCount)).append("\n");
    sb.append("    discardedRxMsgCount: ").append(toIndentedString(discardedRxMsgCount)).append("\n");
    sb.append("    discardedTxMsgCount: ").append(toIndentedString(discardedTxMsgCount)).append("\n");
    sb.append("    loginRxMsgCount: ").append(toIndentedString(loginRxMsgCount)).append("\n");
    sb.append("    loginTxMsgCount: ").append(toIndentedString(loginTxMsgCount)).append("\n");
    sb.append("    msgSpoolRxMsgCount: ").append(toIndentedString(msgSpoolRxMsgCount)).append("\n");
    sb.append("    msgSpoolTxMsgCount: ").append(toIndentedString(msgSpoolTxMsgCount)).append("\n");
    sb.append("    tlsRxByteCount: ").append(toIndentedString(tlsRxByteCount)).append("\n");
    sb.append("    tlsTxByteCount: ").append(toIndentedString(tlsTxByteCount)).append("\n");
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

