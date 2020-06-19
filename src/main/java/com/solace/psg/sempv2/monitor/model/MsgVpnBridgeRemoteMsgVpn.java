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
 * MsgVpnBridgeRemoteMsgVpn
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-13T23:07:13.589Z")
public class MsgVpnBridgeRemoteMsgVpn {
  @SerializedName("boundToQueue")
  private Boolean boundToQueue = null;

  @SerializedName("bridgeName")
  private String bridgeName = null;

  /**
   * The virtual router of the Bridge. The allowed values and their meaning are:  &lt;pre&gt; \&quot;primary\&quot; - The Bridge is used for the primary virtual router. \&quot;backup\&quot; - The Bridge is used for the backup virtual router. \&quot;auto\&quot; - The Bridge is automatically assigned a virtual router at creation, depending on the broker&#39;s active-standby role. &lt;/pre&gt; 
   */
  @JsonAdapter(BridgeVirtualRouterEnum.Adapter.class)
  public enum BridgeVirtualRouterEnum {
    PRIMARY("primary"),
    
    BACKUP("backup"),
    
    AUTO("auto");

    private String value;

    BridgeVirtualRouterEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static BridgeVirtualRouterEnum fromValue(String text) {
      for (BridgeVirtualRouterEnum b : BridgeVirtualRouterEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<BridgeVirtualRouterEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final BridgeVirtualRouterEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public BridgeVirtualRouterEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return BridgeVirtualRouterEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("bridgeVirtualRouter")
  private BridgeVirtualRouterEnum bridgeVirtualRouter = null;

  @SerializedName("clientUsername")
  private String clientUsername = null;

  @SerializedName("compressedDataEnabled")
  private Boolean compressedDataEnabled = null;

  @SerializedName("connectOrder")
  private Integer connectOrder = null;

  @SerializedName("egressFlowWindowSize")
  private Long egressFlowWindowSize = null;

  @SerializedName("enabled")
  private Boolean enabled = null;

  @SerializedName("lastConnectionFailureReason")
  private String lastConnectionFailureReason = null;

  @SerializedName("lastQueueBindFailureReason")
  private String lastQueueBindFailureReason = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("queueBinding")
  private String queueBinding = null;

  @SerializedName("queueBoundUptime")
  private Integer queueBoundUptime = null;

  @SerializedName("remoteMsgVpnInterface")
  private String remoteMsgVpnInterface = null;

  @SerializedName("remoteMsgVpnLocation")
  private String remoteMsgVpnLocation = null;

  @SerializedName("remoteMsgVpnName")
  private String remoteMsgVpnName = null;

  @SerializedName("tlsEnabled")
  private Boolean tlsEnabled = null;

  @SerializedName("unidirectionalClientProfile")
  private String unidirectionalClientProfile = null;

  @SerializedName("up")
  private Boolean up = null;

  public MsgVpnBridgeRemoteMsgVpn boundToQueue(Boolean boundToQueue) {
    this.boundToQueue = boundToQueue;
    return this;
  }

   /**
   * Indicates whether the Bridge is bound to the queue in the remote Message VPN.
   * @return boundToQueue
  **/
  @ApiModelProperty(value = "Indicates whether the Bridge is bound to the queue in the remote Message VPN.")
  public Boolean isBoundToQueue() {
    return boundToQueue;
  }

  public void setBoundToQueue(Boolean boundToQueue) {
    this.boundToQueue = boundToQueue;
  }

  public MsgVpnBridgeRemoteMsgVpn bridgeName(String bridgeName) {
    this.bridgeName = bridgeName;
    return this;
  }

   /**
   * The name of the Bridge.
   * @return bridgeName
  **/
  @ApiModelProperty(value = "The name of the Bridge.")
  public String getBridgeName() {
    return bridgeName;
  }

  public void setBridgeName(String bridgeName) {
    this.bridgeName = bridgeName;
  }

  public MsgVpnBridgeRemoteMsgVpn bridgeVirtualRouter(BridgeVirtualRouterEnum bridgeVirtualRouter) {
    this.bridgeVirtualRouter = bridgeVirtualRouter;
    return this;
  }

   /**
   * The virtual router of the Bridge. The allowed values and their meaning are:  &lt;pre&gt; \&quot;primary\&quot; - The Bridge is used for the primary virtual router. \&quot;backup\&quot; - The Bridge is used for the backup virtual router. \&quot;auto\&quot; - The Bridge is automatically assigned a virtual router at creation, depending on the broker&#39;s active-standby role. &lt;/pre&gt; 
   * @return bridgeVirtualRouter
  **/
  @ApiModelProperty(value = "The virtual router of the Bridge. The allowed values and their meaning are:  <pre> \"primary\" - The Bridge is used for the primary virtual router. \"backup\" - The Bridge is used for the backup virtual router. \"auto\" - The Bridge is automatically assigned a virtual router at creation, depending on the broker's active-standby role. </pre> ")
  public BridgeVirtualRouterEnum getBridgeVirtualRouter() {
    return bridgeVirtualRouter;
  }

  public void setBridgeVirtualRouter(BridgeVirtualRouterEnum bridgeVirtualRouter) {
    this.bridgeVirtualRouter = bridgeVirtualRouter;
  }

  public MsgVpnBridgeRemoteMsgVpn clientUsername(String clientUsername) {
    this.clientUsername = clientUsername;
    return this;
  }

   /**
   * The Client Username the Bridge uses to login to the remote Message VPN. This per remote Message VPN value overrides the value provided for the Bridge overall.
   * @return clientUsername
  **/
  @ApiModelProperty(value = "The Client Username the Bridge uses to login to the remote Message VPN. This per remote Message VPN value overrides the value provided for the Bridge overall.")
  public String getClientUsername() {
    return clientUsername;
  }

  public void setClientUsername(String clientUsername) {
    this.clientUsername = clientUsername;
  }

  public MsgVpnBridgeRemoteMsgVpn compressedDataEnabled(Boolean compressedDataEnabled) {
    this.compressedDataEnabled = compressedDataEnabled;
    return this;
  }

   /**
   * Indicates whether data compression is enabled for the remote Message VPN connection.
   * @return compressedDataEnabled
  **/
  @ApiModelProperty(value = "Indicates whether data compression is enabled for the remote Message VPN connection.")
  public Boolean isCompressedDataEnabled() {
    return compressedDataEnabled;
  }

  public void setCompressedDataEnabled(Boolean compressedDataEnabled) {
    this.compressedDataEnabled = compressedDataEnabled;
  }

  public MsgVpnBridgeRemoteMsgVpn connectOrder(Integer connectOrder) {
    this.connectOrder = connectOrder;
    return this;
  }

   /**
   * The preference given to incoming connections from remote Message VPN hosts, from 1 (highest priority) to 4 (lowest priority).
   * @return connectOrder
  **/
  @ApiModelProperty(value = "The preference given to incoming connections from remote Message VPN hosts, from 1 (highest priority) to 4 (lowest priority).")
  public Integer getConnectOrder() {
    return connectOrder;
  }

  public void setConnectOrder(Integer connectOrder) {
    this.connectOrder = connectOrder;
  }

  public MsgVpnBridgeRemoteMsgVpn egressFlowWindowSize(Long egressFlowWindowSize) {
    this.egressFlowWindowSize = egressFlowWindowSize;
    return this;
  }

   /**
   * The number of outstanding guaranteed messages that can be transmitted over the remote Message VPN connection before an acknowledgement is received.
   * @return egressFlowWindowSize
  **/
  @ApiModelProperty(value = "The number of outstanding guaranteed messages that can be transmitted over the remote Message VPN connection before an acknowledgement is received.")
  public Long getEgressFlowWindowSize() {
    return egressFlowWindowSize;
  }

  public void setEgressFlowWindowSize(Long egressFlowWindowSize) {
    this.egressFlowWindowSize = egressFlowWindowSize;
  }

  public MsgVpnBridgeRemoteMsgVpn enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

   /**
   * Indicates whether the remote Message VPN is enabled.
   * @return enabled
  **/
  @ApiModelProperty(value = "Indicates whether the remote Message VPN is enabled.")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public MsgVpnBridgeRemoteMsgVpn lastConnectionFailureReason(String lastConnectionFailureReason) {
    this.lastConnectionFailureReason = lastConnectionFailureReason;
    return this;
  }

   /**
   * The reason for the last connection failure to the remote Message VPN.
   * @return lastConnectionFailureReason
  **/
  @ApiModelProperty(value = "The reason for the last connection failure to the remote Message VPN.")
  public String getLastConnectionFailureReason() {
    return lastConnectionFailureReason;
  }

  public void setLastConnectionFailureReason(String lastConnectionFailureReason) {
    this.lastConnectionFailureReason = lastConnectionFailureReason;
  }

  public MsgVpnBridgeRemoteMsgVpn lastQueueBindFailureReason(String lastQueueBindFailureReason) {
    this.lastQueueBindFailureReason = lastQueueBindFailureReason;
    return this;
  }

   /**
   * The reason for the last queue bind failure in the remote Message VPN.
   * @return lastQueueBindFailureReason
  **/
  @ApiModelProperty(value = "The reason for the last queue bind failure in the remote Message VPN.")
  public String getLastQueueBindFailureReason() {
    return lastQueueBindFailureReason;
  }

  public void setLastQueueBindFailureReason(String lastQueueBindFailureReason) {
    this.lastQueueBindFailureReason = lastQueueBindFailureReason;
  }

  public MsgVpnBridgeRemoteMsgVpn msgVpnName(String msgVpnName) {
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

  public MsgVpnBridgeRemoteMsgVpn queueBinding(String queueBinding) {
    this.queueBinding = queueBinding;
    return this;
  }

   /**
   * The queue binding of the Bridge in the remote Message VPN.
   * @return queueBinding
  **/
  @ApiModelProperty(value = "The queue binding of the Bridge in the remote Message VPN.")
  public String getQueueBinding() {
    return queueBinding;
  }

  public void setQueueBinding(String queueBinding) {
    this.queueBinding = queueBinding;
  }

  public MsgVpnBridgeRemoteMsgVpn queueBoundUptime(Integer queueBoundUptime) {
    this.queueBoundUptime = queueBoundUptime;
    return this;
  }

   /**
   * The amount of time in seconds since the queue was bound in the remote Message VPN.
   * @return queueBoundUptime
  **/
  @ApiModelProperty(value = "The amount of time in seconds since the queue was bound in the remote Message VPN.")
  public Integer getQueueBoundUptime() {
    return queueBoundUptime;
  }

  public void setQueueBoundUptime(Integer queueBoundUptime) {
    this.queueBoundUptime = queueBoundUptime;
  }

  public MsgVpnBridgeRemoteMsgVpn remoteMsgVpnInterface(String remoteMsgVpnInterface) {
    this.remoteMsgVpnInterface = remoteMsgVpnInterface;
    return this;
  }

   /**
   * The physical interface on the local Message VPN host for connecting to the remote Message VPN. By default, an interface is chosen automatically (recommended), but if specified, &#x60;remoteMsgVpnLocation&#x60; must not be a virtual router name.
   * @return remoteMsgVpnInterface
  **/
  @ApiModelProperty(value = "The physical interface on the local Message VPN host for connecting to the remote Message VPN. By default, an interface is chosen automatically (recommended), but if specified, `remoteMsgVpnLocation` must not be a virtual router name.")
  public String getRemoteMsgVpnInterface() {
    return remoteMsgVpnInterface;
  }

  public void setRemoteMsgVpnInterface(String remoteMsgVpnInterface) {
    this.remoteMsgVpnInterface = remoteMsgVpnInterface;
  }

  public MsgVpnBridgeRemoteMsgVpn remoteMsgVpnLocation(String remoteMsgVpnLocation) {
    this.remoteMsgVpnLocation = remoteMsgVpnLocation;
    return this;
  }

   /**
   * The location of the remote Message VPN as either an FQDN with port, IP address with port, or virtual router name (starting with \&quot;v:\&quot;).
   * @return remoteMsgVpnLocation
  **/
  @ApiModelProperty(value = "The location of the remote Message VPN as either an FQDN with port, IP address with port, or virtual router name (starting with \"v:\").")
  public String getRemoteMsgVpnLocation() {
    return remoteMsgVpnLocation;
  }

  public void setRemoteMsgVpnLocation(String remoteMsgVpnLocation) {
    this.remoteMsgVpnLocation = remoteMsgVpnLocation;
  }

  public MsgVpnBridgeRemoteMsgVpn remoteMsgVpnName(String remoteMsgVpnName) {
    this.remoteMsgVpnName = remoteMsgVpnName;
    return this;
  }

   /**
   * The name of the remote Message VPN.
   * @return remoteMsgVpnName
  **/
  @ApiModelProperty(value = "The name of the remote Message VPN.")
  public String getRemoteMsgVpnName() {
    return remoteMsgVpnName;
  }

  public void setRemoteMsgVpnName(String remoteMsgVpnName) {
    this.remoteMsgVpnName = remoteMsgVpnName;
  }

  public MsgVpnBridgeRemoteMsgVpn tlsEnabled(Boolean tlsEnabled) {
    this.tlsEnabled = tlsEnabled;
    return this;
  }

   /**
   * Indicates whether encryption (TLS) is enabled for the remote Message VPN connection.
   * @return tlsEnabled
  **/
  @ApiModelProperty(value = "Indicates whether encryption (TLS) is enabled for the remote Message VPN connection.")
  public Boolean isTlsEnabled() {
    return tlsEnabled;
  }

  public void setTlsEnabled(Boolean tlsEnabled) {
    this.tlsEnabled = tlsEnabled;
  }

  public MsgVpnBridgeRemoteMsgVpn unidirectionalClientProfile(String unidirectionalClientProfile) {
    this.unidirectionalClientProfile = unidirectionalClientProfile;
    return this;
  }

   /**
   * The Client Profile for the unidirectional Bridge of the remote Message VPN. The Client Profile must exist in the local Message VPN, and it is used only for the TCP parameters. Note that the default client profile has a TCP maximum window size of 2MB.
   * @return unidirectionalClientProfile
  **/
  @ApiModelProperty(value = "The Client Profile for the unidirectional Bridge of the remote Message VPN. The Client Profile must exist in the local Message VPN, and it is used only for the TCP parameters. Note that the default client profile has a TCP maximum window size of 2MB.")
  public String getUnidirectionalClientProfile() {
    return unidirectionalClientProfile;
  }

  public void setUnidirectionalClientProfile(String unidirectionalClientProfile) {
    this.unidirectionalClientProfile = unidirectionalClientProfile;
  }

  public MsgVpnBridgeRemoteMsgVpn up(Boolean up) {
    this.up = up;
    return this;
  }

   /**
   * Indicates whether the connection to the remote Message VPN is up.
   * @return up
  **/
  @ApiModelProperty(value = "Indicates whether the connection to the remote Message VPN is up.")
  public Boolean isUp() {
    return up;
  }

  public void setUp(Boolean up) {
    this.up = up;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnBridgeRemoteMsgVpn msgVpnBridgeRemoteMsgVpn = (MsgVpnBridgeRemoteMsgVpn) o;
    return Objects.equals(this.boundToQueue, msgVpnBridgeRemoteMsgVpn.boundToQueue) &&
        Objects.equals(this.bridgeName, msgVpnBridgeRemoteMsgVpn.bridgeName) &&
        Objects.equals(this.bridgeVirtualRouter, msgVpnBridgeRemoteMsgVpn.bridgeVirtualRouter) &&
        Objects.equals(this.clientUsername, msgVpnBridgeRemoteMsgVpn.clientUsername) &&
        Objects.equals(this.compressedDataEnabled, msgVpnBridgeRemoteMsgVpn.compressedDataEnabled) &&
        Objects.equals(this.connectOrder, msgVpnBridgeRemoteMsgVpn.connectOrder) &&
        Objects.equals(this.egressFlowWindowSize, msgVpnBridgeRemoteMsgVpn.egressFlowWindowSize) &&
        Objects.equals(this.enabled, msgVpnBridgeRemoteMsgVpn.enabled) &&
        Objects.equals(this.lastConnectionFailureReason, msgVpnBridgeRemoteMsgVpn.lastConnectionFailureReason) &&
        Objects.equals(this.lastQueueBindFailureReason, msgVpnBridgeRemoteMsgVpn.lastQueueBindFailureReason) &&
        Objects.equals(this.msgVpnName, msgVpnBridgeRemoteMsgVpn.msgVpnName) &&
        Objects.equals(this.queueBinding, msgVpnBridgeRemoteMsgVpn.queueBinding) &&
        Objects.equals(this.queueBoundUptime, msgVpnBridgeRemoteMsgVpn.queueBoundUptime) &&
        Objects.equals(this.remoteMsgVpnInterface, msgVpnBridgeRemoteMsgVpn.remoteMsgVpnInterface) &&
        Objects.equals(this.remoteMsgVpnLocation, msgVpnBridgeRemoteMsgVpn.remoteMsgVpnLocation) &&
        Objects.equals(this.remoteMsgVpnName, msgVpnBridgeRemoteMsgVpn.remoteMsgVpnName) &&
        Objects.equals(this.tlsEnabled, msgVpnBridgeRemoteMsgVpn.tlsEnabled) &&
        Objects.equals(this.unidirectionalClientProfile, msgVpnBridgeRemoteMsgVpn.unidirectionalClientProfile) &&
        Objects.equals(this.up, msgVpnBridgeRemoteMsgVpn.up);
  }

  @Override
  public int hashCode() {
    return Objects.hash(boundToQueue, bridgeName, bridgeVirtualRouter, clientUsername, compressedDataEnabled, connectOrder, egressFlowWindowSize, enabled, lastConnectionFailureReason, lastQueueBindFailureReason, msgVpnName, queueBinding, queueBoundUptime, remoteMsgVpnInterface, remoteMsgVpnLocation, remoteMsgVpnName, tlsEnabled, unidirectionalClientProfile, up);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnBridgeRemoteMsgVpn {\n");
    
    sb.append("    boundToQueue: ").append(toIndentedString(boundToQueue)).append("\n");
    sb.append("    bridgeName: ").append(toIndentedString(bridgeName)).append("\n");
    sb.append("    bridgeVirtualRouter: ").append(toIndentedString(bridgeVirtualRouter)).append("\n");
    sb.append("    clientUsername: ").append(toIndentedString(clientUsername)).append("\n");
    sb.append("    compressedDataEnabled: ").append(toIndentedString(compressedDataEnabled)).append("\n");
    sb.append("    connectOrder: ").append(toIndentedString(connectOrder)).append("\n");
    sb.append("    egressFlowWindowSize: ").append(toIndentedString(egressFlowWindowSize)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    lastConnectionFailureReason: ").append(toIndentedString(lastConnectionFailureReason)).append("\n");
    sb.append("    lastQueueBindFailureReason: ").append(toIndentedString(lastQueueBindFailureReason)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    queueBinding: ").append(toIndentedString(queueBinding)).append("\n");
    sb.append("    queueBoundUptime: ").append(toIndentedString(queueBoundUptime)).append("\n");
    sb.append("    remoteMsgVpnInterface: ").append(toIndentedString(remoteMsgVpnInterface)).append("\n");
    sb.append("    remoteMsgVpnLocation: ").append(toIndentedString(remoteMsgVpnLocation)).append("\n");
    sb.append("    remoteMsgVpnName: ").append(toIndentedString(remoteMsgVpnName)).append("\n");
    sb.append("    tlsEnabled: ").append(toIndentedString(tlsEnabled)).append("\n");
    sb.append("    unidirectionalClientProfile: ").append(toIndentedString(unidirectionalClientProfile)).append("\n");
    sb.append("    up: ").append(toIndentedString(up)).append("\n");
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

