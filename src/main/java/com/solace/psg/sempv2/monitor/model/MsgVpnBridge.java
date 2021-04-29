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
import com.solace.psg.sempv2.monitor.model.MsgVpnBridgeCounter;
import com.solace.psg.sempv2.monitor.model.MsgVpnBridgeRate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * MsgVpnBridge
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:57:21.016551900+01:00[Europe/London]")
public class MsgVpnBridge {
  @SerializedName("averageRxByteRate")
  private Long averageRxByteRate = null;

  @SerializedName("averageRxMsgRate")
  private Long averageRxMsgRate = null;

  @SerializedName("averageTxByteRate")
  private Long averageTxByteRate = null;

  @SerializedName("averageTxMsgRate")
  private Long averageTxMsgRate = null;

  @SerializedName("boundToQueue")
  private Boolean boundToQueue = null;

  @SerializedName("bridgeName")
  private String bridgeName = null;

  /**
   * The virtual router of the Bridge. The allowed values and their meaning are:  &lt;pre&gt; \&quot;primary\&quot; - The Bridge is used for the primary virtual router. \&quot;backup\&quot; - The Bridge is used for the backup virtual router. \&quot;auto\&quot; - The Bridge is automatically assigned a virtual router at creation, depending on the broker&#x27;s active-standby role. &lt;/pre&gt; 
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
        Object value = jsonReader.nextString();
        return BridgeVirtualRouterEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("bridgeVirtualRouter")
  private BridgeVirtualRouterEnum bridgeVirtualRouter = null;

  @SerializedName("clientName")
  private String clientName = null;

  @SerializedName("compressed")
  private Boolean compressed = null;

  @SerializedName("controlRxByteCount")
  private Long controlRxByteCount = null;

  @SerializedName("controlRxMsgCount")
  private Long controlRxMsgCount = null;

  @SerializedName("controlTxByteCount")
  private Long controlTxByteCount = null;

  @SerializedName("controlTxMsgCount")
  private Long controlTxMsgCount = null;

  @SerializedName("counter")
  private MsgVpnBridgeCounter counter = null;

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

  @SerializedName("enabled")
  private Boolean enabled = null;

  @SerializedName("encrypted")
  private Boolean encrypted = null;

  @SerializedName("establisher")
  private String establisher = null;

  @SerializedName("inboundFailureReason")
  private String inboundFailureReason = null;

  @SerializedName("inboundState")
  private String inboundState = null;

  @SerializedName("lastTxMsgId")
  private Long lastTxMsgId = null;

  @SerializedName("localInterface")
  private String localInterface = null;

  @SerializedName("localQueueName")
  private String localQueueName = null;

  @SerializedName("loginRxMsgCount")
  private Long loginRxMsgCount = null;

  @SerializedName("loginTxMsgCount")
  private Long loginTxMsgCount = null;

  @SerializedName("maxTtl")
  private Long maxTtl = null;

  @SerializedName("msgSpoolRxMsgCount")
  private Long msgSpoolRxMsgCount = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("outboundState")
  private String outboundState = null;

  @SerializedName("rate")
  private MsgVpnBridgeRate rate = null;

  @SerializedName("remoteAddress")
  private String remoteAddress = null;

  @SerializedName("remoteAuthenticationBasicClientUsername")
  private String remoteAuthenticationBasicClientUsername = null;

  /**
   * The authentication scheme for the remote Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;basic\&quot; - Basic Authentication Scheme (via username and password). \&quot;client-certificate\&quot; - Client Certificate Authentication Scheme (via certificate file or content). &lt;/pre&gt; 
   */
  @JsonAdapter(RemoteAuthenticationSchemeEnum.Adapter.class)
  public enum RemoteAuthenticationSchemeEnum {
    BASIC("basic"),
    CLIENT_CERTIFICATE("client-certificate");

    private String value;

    RemoteAuthenticationSchemeEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static RemoteAuthenticationSchemeEnum fromValue(String text) {
      for (RemoteAuthenticationSchemeEnum b : RemoteAuthenticationSchemeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<RemoteAuthenticationSchemeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final RemoteAuthenticationSchemeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public RemoteAuthenticationSchemeEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return RemoteAuthenticationSchemeEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("remoteAuthenticationScheme")
  private RemoteAuthenticationSchemeEnum remoteAuthenticationScheme = null;

  @SerializedName("remoteConnectionRetryCount")
  private Long remoteConnectionRetryCount = null;

  @SerializedName("remoteConnectionRetryDelay")
  private Long remoteConnectionRetryDelay = null;

  /**
   * The priority for deliver-to-one (DTO) messages transmitted from the remote Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;p1\&quot; - The 1st or highest priority. \&quot;p2\&quot; - The 2nd highest priority. \&quot;p3\&quot; - The 3rd highest priority. \&quot;p4\&quot; - The 4th highest priority. \&quot;da\&quot; - Ignore priority and deliver always. &lt;/pre&gt; 
   */
  @JsonAdapter(RemoteDeliverToOnePriorityEnum.Adapter.class)
  public enum RemoteDeliverToOnePriorityEnum {
    P1("p1"),
    P2("p2"),
    P3("p3"),
    P4("p4"),
    DA("da");

    private String value;

    RemoteDeliverToOnePriorityEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static RemoteDeliverToOnePriorityEnum fromValue(String text) {
      for (RemoteDeliverToOnePriorityEnum b : RemoteDeliverToOnePriorityEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<RemoteDeliverToOnePriorityEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final RemoteDeliverToOnePriorityEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public RemoteDeliverToOnePriorityEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return RemoteDeliverToOnePriorityEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("remoteDeliverToOnePriority")
  private RemoteDeliverToOnePriorityEnum remoteDeliverToOnePriority = null;

  @SerializedName("remoteMsgVpnName")
  private String remoteMsgVpnName = null;

  @SerializedName("remoteRouterName")
  private String remoteRouterName = null;

  @SerializedName("remoteTxFlowId")
  private Integer remoteTxFlowId = null;

  @SerializedName("rxByteCount")
  private Long rxByteCount = null;

  @SerializedName("rxByteRate")
  private Long rxByteRate = null;

  @SerializedName("rxMsgCount")
  private Integer rxMsgCount = null;

  @SerializedName("rxMsgRate")
  private Long rxMsgRate = null;

  @SerializedName("tlsCipherSuiteList")
  private String tlsCipherSuiteList = null;

  @SerializedName("tlsDefaultCipherSuiteList")
  private Boolean tlsDefaultCipherSuiteList = null;

  @SerializedName("ttlExceededEventRaised")
  private Boolean ttlExceededEventRaised = null;

  @SerializedName("txByteCount")
  private Long txByteCount = null;

  @SerializedName("txByteRate")
  private Long txByteRate = null;

  @SerializedName("txMsgCount")
  private Long txMsgCount = null;

  @SerializedName("txMsgRate")
  private Long txMsgRate = null;

  @SerializedName("uptime")
  private Long uptime = null;

  public MsgVpnBridge averageRxByteRate(Long averageRxByteRate) {
    this.averageRxByteRate = averageRxByteRate;
    return this;
  }

   /**
   * The one minute average of the message rate received from the Bridge, in bytes per second (B/sec). Available since 2.13.
   * @return averageRxByteRate
  **/
  @Schema(description = "The one minute average of the message rate received from the Bridge, in bytes per second (B/sec). Available since 2.13.")
  public Long getAverageRxByteRate() {
    return averageRxByteRate;
  }

  public void setAverageRxByteRate(Long averageRxByteRate) {
    this.averageRxByteRate = averageRxByteRate;
  }

  public MsgVpnBridge averageRxMsgRate(Long averageRxMsgRate) {
    this.averageRxMsgRate = averageRxMsgRate;
    return this;
  }

   /**
   * The one minute average of the message rate received from the Bridge, in messages per second (msg/sec). Available since 2.13.
   * @return averageRxMsgRate
  **/
  @Schema(description = "The one minute average of the message rate received from the Bridge, in messages per second (msg/sec). Available since 2.13.")
  public Long getAverageRxMsgRate() {
    return averageRxMsgRate;
  }

  public void setAverageRxMsgRate(Long averageRxMsgRate) {
    this.averageRxMsgRate = averageRxMsgRate;
  }

  public MsgVpnBridge averageTxByteRate(Long averageTxByteRate) {
    this.averageTxByteRate = averageTxByteRate;
    return this;
  }

   /**
   * The one minute average of the message rate transmitted to the Bridge, in bytes per second (B/sec). Available since 2.13.
   * @return averageTxByteRate
  **/
  @Schema(description = "The one minute average of the message rate transmitted to the Bridge, in bytes per second (B/sec). Available since 2.13.")
  public Long getAverageTxByteRate() {
    return averageTxByteRate;
  }

  public void setAverageTxByteRate(Long averageTxByteRate) {
    this.averageTxByteRate = averageTxByteRate;
  }

  public MsgVpnBridge averageTxMsgRate(Long averageTxMsgRate) {
    this.averageTxMsgRate = averageTxMsgRate;
    return this;
  }

   /**
   * The one minute average of the message rate transmitted to the Bridge, in messages per second (msg/sec). Available since 2.13.
   * @return averageTxMsgRate
  **/
  @Schema(description = "The one minute average of the message rate transmitted to the Bridge, in messages per second (msg/sec). Available since 2.13.")
  public Long getAverageTxMsgRate() {
    return averageTxMsgRate;
  }

  public void setAverageTxMsgRate(Long averageTxMsgRate) {
    this.averageTxMsgRate = averageTxMsgRate;
  }

  public MsgVpnBridge boundToQueue(Boolean boundToQueue) {
    this.boundToQueue = boundToQueue;
    return this;
  }

   /**
   * Indicates whether the Bridge is bound to the queue in the remote Message VPN.
   * @return boundToQueue
  **/
  @Schema(description = "Indicates whether the Bridge is bound to the queue in the remote Message VPN.")
  public Boolean isBoundToQueue() {
    return boundToQueue;
  }

  public void setBoundToQueue(Boolean boundToQueue) {
    this.boundToQueue = boundToQueue;
  }

  public MsgVpnBridge bridgeName(String bridgeName) {
    this.bridgeName = bridgeName;
    return this;
  }

   /**
   * The name of the Bridge.
   * @return bridgeName
  **/
  @Schema(description = "The name of the Bridge.")
  public String getBridgeName() {
    return bridgeName;
  }

  public void setBridgeName(String bridgeName) {
    this.bridgeName = bridgeName;
  }

  public MsgVpnBridge bridgeVirtualRouter(BridgeVirtualRouterEnum bridgeVirtualRouter) {
    this.bridgeVirtualRouter = bridgeVirtualRouter;
    return this;
  }

   /**
   * The virtual router of the Bridge. The allowed values and their meaning are:  &lt;pre&gt; \&quot;primary\&quot; - The Bridge is used for the primary virtual router. \&quot;backup\&quot; - The Bridge is used for the backup virtual router. \&quot;auto\&quot; - The Bridge is automatically assigned a virtual router at creation, depending on the broker&#x27;s active-standby role. &lt;/pre&gt; 
   * @return bridgeVirtualRouter
  **/
  @Schema(description = "The virtual router of the Bridge. The allowed values and their meaning are:  <pre> \"primary\" - The Bridge is used for the primary virtual router. \"backup\" - The Bridge is used for the backup virtual router. \"auto\" - The Bridge is automatically assigned a virtual router at creation, depending on the broker's active-standby role. </pre> ")
  public BridgeVirtualRouterEnum getBridgeVirtualRouter() {
    return bridgeVirtualRouter;
  }

  public void setBridgeVirtualRouter(BridgeVirtualRouterEnum bridgeVirtualRouter) {
    this.bridgeVirtualRouter = bridgeVirtualRouter;
  }

  public MsgVpnBridge clientName(String clientName) {
    this.clientName = clientName;
    return this;
  }

   /**
   * The name of the Client for the Bridge.
   * @return clientName
  **/
  @Schema(description = "The name of the Client for the Bridge.")
  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public MsgVpnBridge compressed(Boolean compressed) {
    this.compressed = compressed;
    return this;
  }

   /**
   * Indicates whether messages transmitted over the Bridge are compressed.
   * @return compressed
  **/
  @Schema(description = "Indicates whether messages transmitted over the Bridge are compressed.")
  public Boolean isCompressed() {
    return compressed;
  }

  public void setCompressed(Boolean compressed) {
    this.compressed = compressed;
  }

  public MsgVpnBridge controlRxByteCount(Long controlRxByteCount) {
    this.controlRxByteCount = controlRxByteCount;
    return this;
  }

   /**
   * The amount of client control messages received from the Bridge, in bytes (B). Available since 2.13.
   * @return controlRxByteCount
  **/
  @Schema(description = "The amount of client control messages received from the Bridge, in bytes (B). Available since 2.13.")
  public Long getControlRxByteCount() {
    return controlRxByteCount;
  }

  public void setControlRxByteCount(Long controlRxByteCount) {
    this.controlRxByteCount = controlRxByteCount;
  }

  public MsgVpnBridge controlRxMsgCount(Long controlRxMsgCount) {
    this.controlRxMsgCount = controlRxMsgCount;
    return this;
  }

   /**
   * The number of client control messages received from the Bridge. Available since 2.13.
   * @return controlRxMsgCount
  **/
  @Schema(description = "The number of client control messages received from the Bridge. Available since 2.13.")
  public Long getControlRxMsgCount() {
    return controlRxMsgCount;
  }

  public void setControlRxMsgCount(Long controlRxMsgCount) {
    this.controlRxMsgCount = controlRxMsgCount;
  }

  public MsgVpnBridge controlTxByteCount(Long controlTxByteCount) {
    this.controlTxByteCount = controlTxByteCount;
    return this;
  }

   /**
   * The amount of client control messages transmitted to the Bridge, in bytes (B). Available since 2.13.
   * @return controlTxByteCount
  **/
  @Schema(description = "The amount of client control messages transmitted to the Bridge, in bytes (B). Available since 2.13.")
  public Long getControlTxByteCount() {
    return controlTxByteCount;
  }

  public void setControlTxByteCount(Long controlTxByteCount) {
    this.controlTxByteCount = controlTxByteCount;
  }

  public MsgVpnBridge controlTxMsgCount(Long controlTxMsgCount) {
    this.controlTxMsgCount = controlTxMsgCount;
    return this;
  }

   /**
   * The number of client control messages transmitted to the Bridge. Available since 2.13.
   * @return controlTxMsgCount
  **/
  @Schema(description = "The number of client control messages transmitted to the Bridge. Available since 2.13.")
  public Long getControlTxMsgCount() {
    return controlTxMsgCount;
  }

  public void setControlTxMsgCount(Long controlTxMsgCount) {
    this.controlTxMsgCount = controlTxMsgCount;
  }

  public MsgVpnBridge counter(MsgVpnBridgeCounter counter) {
    this.counter = counter;
    return this;
  }

   /**
   * Get counter
   * @return counter
  **/
  @Schema(description = "")
  public MsgVpnBridgeCounter getCounter() {
    return counter;
  }

  public void setCounter(MsgVpnBridgeCounter counter) {
    this.counter = counter;
  }

  public MsgVpnBridge dataRxByteCount(Long dataRxByteCount) {
    this.dataRxByteCount = dataRxByteCount;
    return this;
  }

   /**
   * The amount of client data messages received from the Bridge, in bytes (B). Available since 2.13.
   * @return dataRxByteCount
  **/
  @Schema(description = "The amount of client data messages received from the Bridge, in bytes (B). Available since 2.13.")
  public Long getDataRxByteCount() {
    return dataRxByteCount;
  }

  public void setDataRxByteCount(Long dataRxByteCount) {
    this.dataRxByteCount = dataRxByteCount;
  }

  public MsgVpnBridge dataRxMsgCount(Long dataRxMsgCount) {
    this.dataRxMsgCount = dataRxMsgCount;
    return this;
  }

   /**
   * The number of client data messages received from the Bridge. Available since 2.13.
   * @return dataRxMsgCount
  **/
  @Schema(description = "The number of client data messages received from the Bridge. Available since 2.13.")
  public Long getDataRxMsgCount() {
    return dataRxMsgCount;
  }

  public void setDataRxMsgCount(Long dataRxMsgCount) {
    this.dataRxMsgCount = dataRxMsgCount;
  }

  public MsgVpnBridge dataTxByteCount(Long dataTxByteCount) {
    this.dataTxByteCount = dataTxByteCount;
    return this;
  }

   /**
   * The amount of client data messages transmitted to the Bridge, in bytes (B). Available since 2.13.
   * @return dataTxByteCount
  **/
  @Schema(description = "The amount of client data messages transmitted to the Bridge, in bytes (B). Available since 2.13.")
  public Long getDataTxByteCount() {
    return dataTxByteCount;
  }

  public void setDataTxByteCount(Long dataTxByteCount) {
    this.dataTxByteCount = dataTxByteCount;
  }

  public MsgVpnBridge dataTxMsgCount(Long dataTxMsgCount) {
    this.dataTxMsgCount = dataTxMsgCount;
    return this;
  }

   /**
   * The number of client data messages transmitted to the Bridge. Available since 2.13.
   * @return dataTxMsgCount
  **/
  @Schema(description = "The number of client data messages transmitted to the Bridge. Available since 2.13.")
  public Long getDataTxMsgCount() {
    return dataTxMsgCount;
  }

  public void setDataTxMsgCount(Long dataTxMsgCount) {
    this.dataTxMsgCount = dataTxMsgCount;
  }

  public MsgVpnBridge discardedRxMsgCount(Integer discardedRxMsgCount) {
    this.discardedRxMsgCount = discardedRxMsgCount;
    return this;
  }

   /**
   * The number of messages discarded during reception from the Bridge. Available since 2.13.
   * @return discardedRxMsgCount
  **/
  @Schema(description = "The number of messages discarded during reception from the Bridge. Available since 2.13.")
  public Integer getDiscardedRxMsgCount() {
    return discardedRxMsgCount;
  }

  public void setDiscardedRxMsgCount(Integer discardedRxMsgCount) {
    this.discardedRxMsgCount = discardedRxMsgCount;
  }

  public MsgVpnBridge discardedTxMsgCount(Integer discardedTxMsgCount) {
    this.discardedTxMsgCount = discardedTxMsgCount;
    return this;
  }

   /**
   * The number of messages discarded during transmission to the Bridge. Available since 2.13.
   * @return discardedTxMsgCount
  **/
  @Schema(description = "The number of messages discarded during transmission to the Bridge. Available since 2.13.")
  public Integer getDiscardedTxMsgCount() {
    return discardedTxMsgCount;
  }

  public void setDiscardedTxMsgCount(Integer discardedTxMsgCount) {
    this.discardedTxMsgCount = discardedTxMsgCount;
  }

  public MsgVpnBridge enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

   /**
   * Indicates whether the Bridge is enabled.
   * @return enabled
  **/
  @Schema(description = "Indicates whether the Bridge is enabled.")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public MsgVpnBridge encrypted(Boolean encrypted) {
    this.encrypted = encrypted;
    return this;
  }

   /**
   * Indicates whether messages transmitted over the Bridge are encrypted with TLS.
   * @return encrypted
  **/
  @Schema(description = "Indicates whether messages transmitted over the Bridge are encrypted with TLS.")
  public Boolean isEncrypted() {
    return encrypted;
  }

  public void setEncrypted(Boolean encrypted) {
    this.encrypted = encrypted;
  }

  public MsgVpnBridge establisher(String establisher) {
    this.establisher = establisher;
    return this;
  }

   /**
   * The establisher of the Bridge connection. The allowed values and their meaning are:  &lt;pre&gt; \&quot;local\&quot; - The Bridge connection was established by the local Message VPN. \&quot;remote\&quot; - The Bridge connection was established by the remote Message VPN. &lt;/pre&gt; 
   * @return establisher
  **/
  @Schema(description = "The establisher of the Bridge connection. The allowed values and their meaning are:  <pre> \"local\" - The Bridge connection was established by the local Message VPN. \"remote\" - The Bridge connection was established by the remote Message VPN. </pre> ")
  public String getEstablisher() {
    return establisher;
  }

  public void setEstablisher(String establisher) {
    this.establisher = establisher;
  }

  public MsgVpnBridge inboundFailureReason(String inboundFailureReason) {
    this.inboundFailureReason = inboundFailureReason;
    return this;
  }

   /**
   * The reason for the inbound connection failure from the Bridge.
   * @return inboundFailureReason
  **/
  @Schema(description = "The reason for the inbound connection failure from the Bridge.")
  public String getInboundFailureReason() {
    return inboundFailureReason;
  }

  public void setInboundFailureReason(String inboundFailureReason) {
    this.inboundFailureReason = inboundFailureReason;
  }

  public MsgVpnBridge inboundState(String inboundState) {
    this.inboundState = inboundState;
    return this;
  }

   /**
   * The state of the inbound connection from the Bridge. The allowed values and their meaning are:  &lt;pre&gt; \&quot;init\&quot; - The connection is initializing. \&quot;disabled\&quot; - The connection is disabled by configuration. \&quot;enabled\&quot; - The connection is enabled by configuration. \&quot;prepare\&quot; - The connection is operationally down. \&quot;prepare-wait-to-connect\&quot; - The connection is waiting to connect. \&quot;prepare-fetching-dns\&quot; - The domain name of the destination node is being resolved. \&quot;not-ready\&quot; - The connection is operationally down. \&quot;not-ready-connecting\&quot; - The connection is trying to connect. \&quot;not-ready-handshaking\&quot; - The connection is handshaking. \&quot;not-ready-wait-next\&quot; - The connection failed to connect and is waiting to retry. \&quot;not-ready-wait-reuse\&quot; - The connection is closing in order to reuse an existing connection. \&quot;not-ready-wait-bridge-version-mismatch\&quot; - The connection is closing because of a version mismatch. \&quot;not-ready-wait-cleanup\&quot; - The connection is closed and cleaning up. \&quot;ready\&quot; - The connection is operationally up. \&quot;ready-subscribing\&quot; - The connection is up and synchronizing subscriptions. \&quot;ready-in-sync\&quot; - The connection is up and subscriptions are synchronized. &lt;/pre&gt; 
   * @return inboundState
  **/
  @Schema(description = "The state of the inbound connection from the Bridge. The allowed values and their meaning are:  <pre> \"init\" - The connection is initializing. \"disabled\" - The connection is disabled by configuration. \"enabled\" - The connection is enabled by configuration. \"prepare\" - The connection is operationally down. \"prepare-wait-to-connect\" - The connection is waiting to connect. \"prepare-fetching-dns\" - The domain name of the destination node is being resolved. \"not-ready\" - The connection is operationally down. \"not-ready-connecting\" - The connection is trying to connect. \"not-ready-handshaking\" - The connection is handshaking. \"not-ready-wait-next\" - The connection failed to connect and is waiting to retry. \"not-ready-wait-reuse\" - The connection is closing in order to reuse an existing connection. \"not-ready-wait-bridge-version-mismatch\" - The connection is closing because of a version mismatch. \"not-ready-wait-cleanup\" - The connection is closed and cleaning up. \"ready\" - The connection is operationally up. \"ready-subscribing\" - The connection is up and synchronizing subscriptions. \"ready-in-sync\" - The connection is up and subscriptions are synchronized. </pre> ")
  public String getInboundState() {
    return inboundState;
  }

  public void setInboundState(String inboundState) {
    this.inboundState = inboundState;
  }

  public MsgVpnBridge lastTxMsgId(Long lastTxMsgId) {
    this.lastTxMsgId = lastTxMsgId;
    return this;
  }

   /**
   * The ID of the last message transmitted to the Bridge.
   * @return lastTxMsgId
  **/
  @Schema(description = "The ID of the last message transmitted to the Bridge.")
  public Long getLastTxMsgId() {
    return lastTxMsgId;
  }

  public void setLastTxMsgId(Long lastTxMsgId) {
    this.lastTxMsgId = lastTxMsgId;
  }

  public MsgVpnBridge localInterface(String localInterface) {
    this.localInterface = localInterface;
    return this;
  }

   /**
   * The physical interface on the local Message VPN host for connecting to the remote Message VPN.
   * @return localInterface
  **/
  @Schema(description = "The physical interface on the local Message VPN host for connecting to the remote Message VPN.")
  public String getLocalInterface() {
    return localInterface;
  }

  public void setLocalInterface(String localInterface) {
    this.localInterface = localInterface;
  }

  public MsgVpnBridge localQueueName(String localQueueName) {
    this.localQueueName = localQueueName;
    return this;
  }

   /**
   * The name of the local queue for the Bridge.
   * @return localQueueName
  **/
  @Schema(description = "The name of the local queue for the Bridge.")
  public String getLocalQueueName() {
    return localQueueName;
  }

  public void setLocalQueueName(String localQueueName) {
    this.localQueueName = localQueueName;
  }

  public MsgVpnBridge loginRxMsgCount(Long loginRxMsgCount) {
    this.loginRxMsgCount = loginRxMsgCount;
    return this;
  }

   /**
   * The number of login request messages received from the Bridge. Available since 2.13.
   * @return loginRxMsgCount
  **/
  @Schema(description = "The number of login request messages received from the Bridge. Available since 2.13.")
  public Long getLoginRxMsgCount() {
    return loginRxMsgCount;
  }

  public void setLoginRxMsgCount(Long loginRxMsgCount) {
    this.loginRxMsgCount = loginRxMsgCount;
  }

  public MsgVpnBridge loginTxMsgCount(Long loginTxMsgCount) {
    this.loginTxMsgCount = loginTxMsgCount;
    return this;
  }

   /**
   * The number of login response messages transmitted to the Bridge. Available since 2.13.
   * @return loginTxMsgCount
  **/
  @Schema(description = "The number of login response messages transmitted to the Bridge. Available since 2.13.")
  public Long getLoginTxMsgCount() {
    return loginTxMsgCount;
  }

  public void setLoginTxMsgCount(Long loginTxMsgCount) {
    this.loginTxMsgCount = loginTxMsgCount;
  }

  public MsgVpnBridge maxTtl(Long maxTtl) {
    this.maxTtl = maxTtl;
    return this;
  }

   /**
   * The maximum time-to-live (TTL) in hops. Messages are discarded if their TTL exceeds this value.
   * @return maxTtl
  **/
  @Schema(description = "The maximum time-to-live (TTL) in hops. Messages are discarded if their TTL exceeds this value.")
  public Long getMaxTtl() {
    return maxTtl;
  }

  public void setMaxTtl(Long maxTtl) {
    this.maxTtl = maxTtl;
  }

  public MsgVpnBridge msgSpoolRxMsgCount(Long msgSpoolRxMsgCount) {
    this.msgSpoolRxMsgCount = msgSpoolRxMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages received from the Bridge. Available since 2.13.
   * @return msgSpoolRxMsgCount
  **/
  @Schema(description = "The number of guaranteed messages received from the Bridge. Available since 2.13.")
  public Long getMsgSpoolRxMsgCount() {
    return msgSpoolRxMsgCount;
  }

  public void setMsgSpoolRxMsgCount(Long msgSpoolRxMsgCount) {
    this.msgSpoolRxMsgCount = msgSpoolRxMsgCount;
  }

  public MsgVpnBridge msgVpnName(String msgVpnName) {
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

  public MsgVpnBridge outboundState(String outboundState) {
    this.outboundState = outboundState;
    return this;
  }

   /**
   * The state of the outbound connection to the Bridge. The allowed values and their meaning are:  &lt;pre&gt; \&quot;init\&quot; - The connection is initializing. \&quot;disabled\&quot; - The connection is disabled by configuration. \&quot;enabled\&quot; - The connection is enabled by configuration. \&quot;prepare\&quot; - The connection is operationally down. \&quot;prepare-wait-to-connect\&quot; - The connection is waiting to connect. \&quot;prepare-fetching-dns\&quot; - The domain name of the destination node is being resolved. \&quot;not-ready\&quot; - The connection is operationally down. \&quot;not-ready-connecting\&quot; - The connection is trying to connect. \&quot;not-ready-handshaking\&quot; - The connection is handshaking. \&quot;not-ready-wait-next\&quot; - The connection failed to connect and is waiting to retry. \&quot;not-ready-wait-reuse\&quot; - The connection is closing in order to reuse an existing connection. \&quot;not-ready-wait-bridge-version-mismatch\&quot; - The connection is closing because of a version mismatch. \&quot;not-ready-wait-cleanup\&quot; - The connection is closed and cleaning up. \&quot;ready\&quot; - The connection is operationally up. \&quot;ready-subscribing\&quot; - The connection is up and synchronizing subscriptions. \&quot;ready-in-sync\&quot; - The connection is up and subscriptions are synchronized. &lt;/pre&gt; 
   * @return outboundState
  **/
  @Schema(description = "The state of the outbound connection to the Bridge. The allowed values and their meaning are:  <pre> \"init\" - The connection is initializing. \"disabled\" - The connection is disabled by configuration. \"enabled\" - The connection is enabled by configuration. \"prepare\" - The connection is operationally down. \"prepare-wait-to-connect\" - The connection is waiting to connect. \"prepare-fetching-dns\" - The domain name of the destination node is being resolved. \"not-ready\" - The connection is operationally down. \"not-ready-connecting\" - The connection is trying to connect. \"not-ready-handshaking\" - The connection is handshaking. \"not-ready-wait-next\" - The connection failed to connect and is waiting to retry. \"not-ready-wait-reuse\" - The connection is closing in order to reuse an existing connection. \"not-ready-wait-bridge-version-mismatch\" - The connection is closing because of a version mismatch. \"not-ready-wait-cleanup\" - The connection is closed and cleaning up. \"ready\" - The connection is operationally up. \"ready-subscribing\" - The connection is up and synchronizing subscriptions. \"ready-in-sync\" - The connection is up and subscriptions are synchronized. </pre> ")
  public String getOutboundState() {
    return outboundState;
  }

  public void setOutboundState(String outboundState) {
    this.outboundState = outboundState;
  }

  public MsgVpnBridge rate(MsgVpnBridgeRate rate) {
    this.rate = rate;
    return this;
  }

   /**
   * Get rate
   * @return rate
  **/
  @Schema(description = "")
  public MsgVpnBridgeRate getRate() {
    return rate;
  }

  public void setRate(MsgVpnBridgeRate rate) {
    this.rate = rate;
  }

  public MsgVpnBridge remoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

   /**
   * The FQDN or IP address of the remote Message VPN.
   * @return remoteAddress
  **/
  @Schema(description = "The FQDN or IP address of the remote Message VPN.")
  public String getRemoteAddress() {
    return remoteAddress;
  }

  public void setRemoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
  }

  public MsgVpnBridge remoteAuthenticationBasicClientUsername(String remoteAuthenticationBasicClientUsername) {
    this.remoteAuthenticationBasicClientUsername = remoteAuthenticationBasicClientUsername;
    return this;
  }

   /**
   * The Client Username the Bridge uses to login to the remote Message VPN.
   * @return remoteAuthenticationBasicClientUsername
  **/
  @Schema(description = "The Client Username the Bridge uses to login to the remote Message VPN.")
  public String getRemoteAuthenticationBasicClientUsername() {
    return remoteAuthenticationBasicClientUsername;
  }

  public void setRemoteAuthenticationBasicClientUsername(String remoteAuthenticationBasicClientUsername) {
    this.remoteAuthenticationBasicClientUsername = remoteAuthenticationBasicClientUsername;
  }

  public MsgVpnBridge remoteAuthenticationScheme(RemoteAuthenticationSchemeEnum remoteAuthenticationScheme) {
    this.remoteAuthenticationScheme = remoteAuthenticationScheme;
    return this;
  }

   /**
   * The authentication scheme for the remote Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;basic\&quot; - Basic Authentication Scheme (via username and password). \&quot;client-certificate\&quot; - Client Certificate Authentication Scheme (via certificate file or content). &lt;/pre&gt; 
   * @return remoteAuthenticationScheme
  **/
  @Schema(description = "The authentication scheme for the remote Message VPN. The allowed values and their meaning are:  <pre> \"basic\" - Basic Authentication Scheme (via username and password). \"client-certificate\" - Client Certificate Authentication Scheme (via certificate file or content). </pre> ")
  public RemoteAuthenticationSchemeEnum getRemoteAuthenticationScheme() {
    return remoteAuthenticationScheme;
  }

  public void setRemoteAuthenticationScheme(RemoteAuthenticationSchemeEnum remoteAuthenticationScheme) {
    this.remoteAuthenticationScheme = remoteAuthenticationScheme;
  }

  public MsgVpnBridge remoteConnectionRetryCount(Long remoteConnectionRetryCount) {
    this.remoteConnectionRetryCount = remoteConnectionRetryCount;
    return this;
  }

   /**
   * The maximum number of retry attempts to establish a connection to the remote Message VPN. A value of 0 means to retry forever.
   * @return remoteConnectionRetryCount
  **/
  @Schema(description = "The maximum number of retry attempts to establish a connection to the remote Message VPN. A value of 0 means to retry forever.")
  public Long getRemoteConnectionRetryCount() {
    return remoteConnectionRetryCount;
  }

  public void setRemoteConnectionRetryCount(Long remoteConnectionRetryCount) {
    this.remoteConnectionRetryCount = remoteConnectionRetryCount;
  }

  public MsgVpnBridge remoteConnectionRetryDelay(Long remoteConnectionRetryDelay) {
    this.remoteConnectionRetryDelay = remoteConnectionRetryDelay;
    return this;
  }

   /**
   * The number of seconds the broker waits for the bridge connection to be established before attempting a new connection.
   * @return remoteConnectionRetryDelay
  **/
  @Schema(description = "The number of seconds the broker waits for the bridge connection to be established before attempting a new connection.")
  public Long getRemoteConnectionRetryDelay() {
    return remoteConnectionRetryDelay;
  }

  public void setRemoteConnectionRetryDelay(Long remoteConnectionRetryDelay) {
    this.remoteConnectionRetryDelay = remoteConnectionRetryDelay;
  }

  public MsgVpnBridge remoteDeliverToOnePriority(RemoteDeliverToOnePriorityEnum remoteDeliverToOnePriority) {
    this.remoteDeliverToOnePriority = remoteDeliverToOnePriority;
    return this;
  }

   /**
   * The priority for deliver-to-one (DTO) messages transmitted from the remote Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;p1\&quot; - The 1st or highest priority. \&quot;p2\&quot; - The 2nd highest priority. \&quot;p3\&quot; - The 3rd highest priority. \&quot;p4\&quot; - The 4th highest priority. \&quot;da\&quot; - Ignore priority and deliver always. &lt;/pre&gt; 
   * @return remoteDeliverToOnePriority
  **/
  @Schema(description = "The priority for deliver-to-one (DTO) messages transmitted from the remote Message VPN. The allowed values and their meaning are:  <pre> \"p1\" - The 1st or highest priority. \"p2\" - The 2nd highest priority. \"p3\" - The 3rd highest priority. \"p4\" - The 4th highest priority. \"da\" - Ignore priority and deliver always. </pre> ")
  public RemoteDeliverToOnePriorityEnum getRemoteDeliverToOnePriority() {
    return remoteDeliverToOnePriority;
  }

  public void setRemoteDeliverToOnePriority(RemoteDeliverToOnePriorityEnum remoteDeliverToOnePriority) {
    this.remoteDeliverToOnePriority = remoteDeliverToOnePriority;
  }

  public MsgVpnBridge remoteMsgVpnName(String remoteMsgVpnName) {
    this.remoteMsgVpnName = remoteMsgVpnName;
    return this;
  }

   /**
   * The name of the remote Message VPN.
   * @return remoteMsgVpnName
  **/
  @Schema(description = "The name of the remote Message VPN.")
  public String getRemoteMsgVpnName() {
    return remoteMsgVpnName;
  }

  public void setRemoteMsgVpnName(String remoteMsgVpnName) {
    this.remoteMsgVpnName = remoteMsgVpnName;
  }

  public MsgVpnBridge remoteRouterName(String remoteRouterName) {
    this.remoteRouterName = remoteRouterName;
    return this;
  }

   /**
   * The name of the remote router.
   * @return remoteRouterName
  **/
  @Schema(description = "The name of the remote router.")
  public String getRemoteRouterName() {
    return remoteRouterName;
  }

  public void setRemoteRouterName(String remoteRouterName) {
    this.remoteRouterName = remoteRouterName;
  }

  public MsgVpnBridge remoteTxFlowId(Integer remoteTxFlowId) {
    this.remoteTxFlowId = remoteTxFlowId;
    return this;
  }

   /**
   * The ID of the transmit flow for the connected remote Message VPN.
   * @return remoteTxFlowId
  **/
  @Schema(description = "The ID of the transmit flow for the connected remote Message VPN.")
  public Integer getRemoteTxFlowId() {
    return remoteTxFlowId;
  }

  public void setRemoteTxFlowId(Integer remoteTxFlowId) {
    this.remoteTxFlowId = remoteTxFlowId;
  }

  public MsgVpnBridge rxByteCount(Long rxByteCount) {
    this.rxByteCount = rxByteCount;
    return this;
  }

   /**
   * The amount of messages received from the Bridge, in bytes (B). Available since 2.13.
   * @return rxByteCount
  **/
  @Schema(description = "The amount of messages received from the Bridge, in bytes (B). Available since 2.13.")
  public Long getRxByteCount() {
    return rxByteCount;
  }

  public void setRxByteCount(Long rxByteCount) {
    this.rxByteCount = rxByteCount;
  }

  public MsgVpnBridge rxByteRate(Long rxByteRate) {
    this.rxByteRate = rxByteRate;
    return this;
  }

   /**
   * The current message rate received from the Bridge, in bytes per second (B/sec). Available since 2.13.
   * @return rxByteRate
  **/
  @Schema(description = "The current message rate received from the Bridge, in bytes per second (B/sec). Available since 2.13.")
  public Long getRxByteRate() {
    return rxByteRate;
  }

  public void setRxByteRate(Long rxByteRate) {
    this.rxByteRate = rxByteRate;
  }

  public MsgVpnBridge rxMsgCount(Integer rxMsgCount) {
    this.rxMsgCount = rxMsgCount;
    return this;
  }

   /**
   * The number of messages received from the Bridge. Available since 2.13.
   * @return rxMsgCount
  **/
  @Schema(description = "The number of messages received from the Bridge. Available since 2.13.")
  public Integer getRxMsgCount() {
    return rxMsgCount;
  }

  public void setRxMsgCount(Integer rxMsgCount) {
    this.rxMsgCount = rxMsgCount;
  }

  public MsgVpnBridge rxMsgRate(Long rxMsgRate) {
    this.rxMsgRate = rxMsgRate;
    return this;
  }

   /**
   * The current message rate received from the Bridge, in messages per second (msg/sec). Available since 2.13.
   * @return rxMsgRate
  **/
  @Schema(description = "The current message rate received from the Bridge, in messages per second (msg/sec). Available since 2.13.")
  public Long getRxMsgRate() {
    return rxMsgRate;
  }

  public void setRxMsgRate(Long rxMsgRate) {
    this.rxMsgRate = rxMsgRate;
  }

  public MsgVpnBridge tlsCipherSuiteList(String tlsCipherSuiteList) {
    this.tlsCipherSuiteList = tlsCipherSuiteList;
    return this;
  }

   /**
   * The colon-separated list of cipher suites supported for TLS connections to the remote Message VPN. The value \&quot;default\&quot; implies all supported suites ordered from most secure to least secure.
   * @return tlsCipherSuiteList
  **/
  @Schema(description = "The colon-separated list of cipher suites supported for TLS connections to the remote Message VPN. The value \"default\" implies all supported suites ordered from most secure to least secure.")
  public String getTlsCipherSuiteList() {
    return tlsCipherSuiteList;
  }

  public void setTlsCipherSuiteList(String tlsCipherSuiteList) {
    this.tlsCipherSuiteList = tlsCipherSuiteList;
  }

  public MsgVpnBridge tlsDefaultCipherSuiteList(Boolean tlsDefaultCipherSuiteList) {
    this.tlsDefaultCipherSuiteList = tlsDefaultCipherSuiteList;
    return this;
  }

   /**
   * Indicates whether the Bridge is configured to use the default cipher-suite list.
   * @return tlsDefaultCipherSuiteList
  **/
  @Schema(description = "Indicates whether the Bridge is configured to use the default cipher-suite list.")
  public Boolean isTlsDefaultCipherSuiteList() {
    return tlsDefaultCipherSuiteList;
  }

  public void setTlsDefaultCipherSuiteList(Boolean tlsDefaultCipherSuiteList) {
    this.tlsDefaultCipherSuiteList = tlsDefaultCipherSuiteList;
  }

  public MsgVpnBridge ttlExceededEventRaised(Boolean ttlExceededEventRaised) {
    this.ttlExceededEventRaised = ttlExceededEventRaised;
    return this;
  }

   /**
   * Indicates whether the TTL (hops) exceeded event has been raised.
   * @return ttlExceededEventRaised
  **/
  @Schema(description = "Indicates whether the TTL (hops) exceeded event has been raised.")
  public Boolean isTtlExceededEventRaised() {
    return ttlExceededEventRaised;
  }

  public void setTtlExceededEventRaised(Boolean ttlExceededEventRaised) {
    this.ttlExceededEventRaised = ttlExceededEventRaised;
  }

  public MsgVpnBridge txByteCount(Long txByteCount) {
    this.txByteCount = txByteCount;
    return this;
  }

   /**
   * The amount of messages transmitted to the Bridge, in bytes (B). Available since 2.13.
   * @return txByteCount
  **/
  @Schema(description = "The amount of messages transmitted to the Bridge, in bytes (B). Available since 2.13.")
  public Long getTxByteCount() {
    return txByteCount;
  }

  public void setTxByteCount(Long txByteCount) {
    this.txByteCount = txByteCount;
  }

  public MsgVpnBridge txByteRate(Long txByteRate) {
    this.txByteRate = txByteRate;
    return this;
  }

   /**
   * The current message rate transmitted to the Bridge, in bytes per second (B/sec). Available since 2.13.
   * @return txByteRate
  **/
  @Schema(description = "The current message rate transmitted to the Bridge, in bytes per second (B/sec). Available since 2.13.")
  public Long getTxByteRate() {
    return txByteRate;
  }

  public void setTxByteRate(Long txByteRate) {
    this.txByteRate = txByteRate;
  }

  public MsgVpnBridge txMsgCount(Long txMsgCount) {
    this.txMsgCount = txMsgCount;
    return this;
  }

   /**
   * The number of messages transmitted to the Bridge. Available since 2.13.
   * @return txMsgCount
  **/
  @Schema(description = "The number of messages transmitted to the Bridge. Available since 2.13.")
  public Long getTxMsgCount() {
    return txMsgCount;
  }

  public void setTxMsgCount(Long txMsgCount) {
    this.txMsgCount = txMsgCount;
  }

  public MsgVpnBridge txMsgRate(Long txMsgRate) {
    this.txMsgRate = txMsgRate;
    return this;
  }

   /**
   * The current message rate transmitted to the Bridge, in messages per second (msg/sec). Available since 2.13.
   * @return txMsgRate
  **/
  @Schema(description = "The current message rate transmitted to the Bridge, in messages per second (msg/sec). Available since 2.13.")
  public Long getTxMsgRate() {
    return txMsgRate;
  }

  public void setTxMsgRate(Long txMsgRate) {
    this.txMsgRate = txMsgRate;
  }

  public MsgVpnBridge uptime(Long uptime) {
    this.uptime = uptime;
    return this;
  }

   /**
   * The amount of time in seconds since the Bridge connected to the remote Message VPN.
   * @return uptime
  **/
  @Schema(description = "The amount of time in seconds since the Bridge connected to the remote Message VPN.")
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
    MsgVpnBridge msgVpnBridge = (MsgVpnBridge) o;
    return Objects.equals(this.averageRxByteRate, msgVpnBridge.averageRxByteRate) &&
        Objects.equals(this.averageRxMsgRate, msgVpnBridge.averageRxMsgRate) &&
        Objects.equals(this.averageTxByteRate, msgVpnBridge.averageTxByteRate) &&
        Objects.equals(this.averageTxMsgRate, msgVpnBridge.averageTxMsgRate) &&
        Objects.equals(this.boundToQueue, msgVpnBridge.boundToQueue) &&
        Objects.equals(this.bridgeName, msgVpnBridge.bridgeName) &&
        Objects.equals(this.bridgeVirtualRouter, msgVpnBridge.bridgeVirtualRouter) &&
        Objects.equals(this.clientName, msgVpnBridge.clientName) &&
        Objects.equals(this.compressed, msgVpnBridge.compressed) &&
        Objects.equals(this.controlRxByteCount, msgVpnBridge.controlRxByteCount) &&
        Objects.equals(this.controlRxMsgCount, msgVpnBridge.controlRxMsgCount) &&
        Objects.equals(this.controlTxByteCount, msgVpnBridge.controlTxByteCount) &&
        Objects.equals(this.controlTxMsgCount, msgVpnBridge.controlTxMsgCount) &&
        Objects.equals(this.counter, msgVpnBridge.counter) &&
        Objects.equals(this.dataRxByteCount, msgVpnBridge.dataRxByteCount) &&
        Objects.equals(this.dataRxMsgCount, msgVpnBridge.dataRxMsgCount) &&
        Objects.equals(this.dataTxByteCount, msgVpnBridge.dataTxByteCount) &&
        Objects.equals(this.dataTxMsgCount, msgVpnBridge.dataTxMsgCount) &&
        Objects.equals(this.discardedRxMsgCount, msgVpnBridge.discardedRxMsgCount) &&
        Objects.equals(this.discardedTxMsgCount, msgVpnBridge.discardedTxMsgCount) &&
        Objects.equals(this.enabled, msgVpnBridge.enabled) &&
        Objects.equals(this.encrypted, msgVpnBridge.encrypted) &&
        Objects.equals(this.establisher, msgVpnBridge.establisher) &&
        Objects.equals(this.inboundFailureReason, msgVpnBridge.inboundFailureReason) &&
        Objects.equals(this.inboundState, msgVpnBridge.inboundState) &&
        Objects.equals(this.lastTxMsgId, msgVpnBridge.lastTxMsgId) &&
        Objects.equals(this.localInterface, msgVpnBridge.localInterface) &&
        Objects.equals(this.localQueueName, msgVpnBridge.localQueueName) &&
        Objects.equals(this.loginRxMsgCount, msgVpnBridge.loginRxMsgCount) &&
        Objects.equals(this.loginTxMsgCount, msgVpnBridge.loginTxMsgCount) &&
        Objects.equals(this.maxTtl, msgVpnBridge.maxTtl) &&
        Objects.equals(this.msgSpoolRxMsgCount, msgVpnBridge.msgSpoolRxMsgCount) &&
        Objects.equals(this.msgVpnName, msgVpnBridge.msgVpnName) &&
        Objects.equals(this.outboundState, msgVpnBridge.outboundState) &&
        Objects.equals(this.rate, msgVpnBridge.rate) &&
        Objects.equals(this.remoteAddress, msgVpnBridge.remoteAddress) &&
        Objects.equals(this.remoteAuthenticationBasicClientUsername, msgVpnBridge.remoteAuthenticationBasicClientUsername) &&
        Objects.equals(this.remoteAuthenticationScheme, msgVpnBridge.remoteAuthenticationScheme) &&
        Objects.equals(this.remoteConnectionRetryCount, msgVpnBridge.remoteConnectionRetryCount) &&
        Objects.equals(this.remoteConnectionRetryDelay, msgVpnBridge.remoteConnectionRetryDelay) &&
        Objects.equals(this.remoteDeliverToOnePriority, msgVpnBridge.remoteDeliverToOnePriority) &&
        Objects.equals(this.remoteMsgVpnName, msgVpnBridge.remoteMsgVpnName) &&
        Objects.equals(this.remoteRouterName, msgVpnBridge.remoteRouterName) &&
        Objects.equals(this.remoteTxFlowId, msgVpnBridge.remoteTxFlowId) &&
        Objects.equals(this.rxByteCount, msgVpnBridge.rxByteCount) &&
        Objects.equals(this.rxByteRate, msgVpnBridge.rxByteRate) &&
        Objects.equals(this.rxMsgCount, msgVpnBridge.rxMsgCount) &&
        Objects.equals(this.rxMsgRate, msgVpnBridge.rxMsgRate) &&
        Objects.equals(this.tlsCipherSuiteList, msgVpnBridge.tlsCipherSuiteList) &&
        Objects.equals(this.tlsDefaultCipherSuiteList, msgVpnBridge.tlsDefaultCipherSuiteList) &&
        Objects.equals(this.ttlExceededEventRaised, msgVpnBridge.ttlExceededEventRaised) &&
        Objects.equals(this.txByteCount, msgVpnBridge.txByteCount) &&
        Objects.equals(this.txByteRate, msgVpnBridge.txByteRate) &&
        Objects.equals(this.txMsgCount, msgVpnBridge.txMsgCount) &&
        Objects.equals(this.txMsgRate, msgVpnBridge.txMsgRate) &&
        Objects.equals(this.uptime, msgVpnBridge.uptime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(averageRxByteRate, averageRxMsgRate, averageTxByteRate, averageTxMsgRate, boundToQueue, bridgeName, bridgeVirtualRouter, clientName, compressed, controlRxByteCount, controlRxMsgCount, controlTxByteCount, controlTxMsgCount, counter, dataRxByteCount, dataRxMsgCount, dataTxByteCount, dataTxMsgCount, discardedRxMsgCount, discardedTxMsgCount, enabled, encrypted, establisher, inboundFailureReason, inboundState, lastTxMsgId, localInterface, localQueueName, loginRxMsgCount, loginTxMsgCount, maxTtl, msgSpoolRxMsgCount, msgVpnName, outboundState, rate, remoteAddress, remoteAuthenticationBasicClientUsername, remoteAuthenticationScheme, remoteConnectionRetryCount, remoteConnectionRetryDelay, remoteDeliverToOnePriority, remoteMsgVpnName, remoteRouterName, remoteTxFlowId, rxByteCount, rxByteRate, rxMsgCount, rxMsgRate, tlsCipherSuiteList, tlsDefaultCipherSuiteList, ttlExceededEventRaised, txByteCount, txByteRate, txMsgCount, txMsgRate, uptime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnBridge {\n");
    
    sb.append("    averageRxByteRate: ").append(toIndentedString(averageRxByteRate)).append("\n");
    sb.append("    averageRxMsgRate: ").append(toIndentedString(averageRxMsgRate)).append("\n");
    sb.append("    averageTxByteRate: ").append(toIndentedString(averageTxByteRate)).append("\n");
    sb.append("    averageTxMsgRate: ").append(toIndentedString(averageTxMsgRate)).append("\n");
    sb.append("    boundToQueue: ").append(toIndentedString(boundToQueue)).append("\n");
    sb.append("    bridgeName: ").append(toIndentedString(bridgeName)).append("\n");
    sb.append("    bridgeVirtualRouter: ").append(toIndentedString(bridgeVirtualRouter)).append("\n");
    sb.append("    clientName: ").append(toIndentedString(clientName)).append("\n");
    sb.append("    compressed: ").append(toIndentedString(compressed)).append("\n");
    sb.append("    controlRxByteCount: ").append(toIndentedString(controlRxByteCount)).append("\n");
    sb.append("    controlRxMsgCount: ").append(toIndentedString(controlRxMsgCount)).append("\n");
    sb.append("    controlTxByteCount: ").append(toIndentedString(controlTxByteCount)).append("\n");
    sb.append("    controlTxMsgCount: ").append(toIndentedString(controlTxMsgCount)).append("\n");
    sb.append("    counter: ").append(toIndentedString(counter)).append("\n");
    sb.append("    dataRxByteCount: ").append(toIndentedString(dataRxByteCount)).append("\n");
    sb.append("    dataRxMsgCount: ").append(toIndentedString(dataRxMsgCount)).append("\n");
    sb.append("    dataTxByteCount: ").append(toIndentedString(dataTxByteCount)).append("\n");
    sb.append("    dataTxMsgCount: ").append(toIndentedString(dataTxMsgCount)).append("\n");
    sb.append("    discardedRxMsgCount: ").append(toIndentedString(discardedRxMsgCount)).append("\n");
    sb.append("    discardedTxMsgCount: ").append(toIndentedString(discardedTxMsgCount)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    encrypted: ").append(toIndentedString(encrypted)).append("\n");
    sb.append("    establisher: ").append(toIndentedString(establisher)).append("\n");
    sb.append("    inboundFailureReason: ").append(toIndentedString(inboundFailureReason)).append("\n");
    sb.append("    inboundState: ").append(toIndentedString(inboundState)).append("\n");
    sb.append("    lastTxMsgId: ").append(toIndentedString(lastTxMsgId)).append("\n");
    sb.append("    localInterface: ").append(toIndentedString(localInterface)).append("\n");
    sb.append("    localQueueName: ").append(toIndentedString(localQueueName)).append("\n");
    sb.append("    loginRxMsgCount: ").append(toIndentedString(loginRxMsgCount)).append("\n");
    sb.append("    loginTxMsgCount: ").append(toIndentedString(loginTxMsgCount)).append("\n");
    sb.append("    maxTtl: ").append(toIndentedString(maxTtl)).append("\n");
    sb.append("    msgSpoolRxMsgCount: ").append(toIndentedString(msgSpoolRxMsgCount)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    outboundState: ").append(toIndentedString(outboundState)).append("\n");
    sb.append("    rate: ").append(toIndentedString(rate)).append("\n");
    sb.append("    remoteAddress: ").append(toIndentedString(remoteAddress)).append("\n");
    sb.append("    remoteAuthenticationBasicClientUsername: ").append(toIndentedString(remoteAuthenticationBasicClientUsername)).append("\n");
    sb.append("    remoteAuthenticationScheme: ").append(toIndentedString(remoteAuthenticationScheme)).append("\n");
    sb.append("    remoteConnectionRetryCount: ").append(toIndentedString(remoteConnectionRetryCount)).append("\n");
    sb.append("    remoteConnectionRetryDelay: ").append(toIndentedString(remoteConnectionRetryDelay)).append("\n");
    sb.append("    remoteDeliverToOnePriority: ").append(toIndentedString(remoteDeliverToOnePriority)).append("\n");
    sb.append("    remoteMsgVpnName: ").append(toIndentedString(remoteMsgVpnName)).append("\n");
    sb.append("    remoteRouterName: ").append(toIndentedString(remoteRouterName)).append("\n");
    sb.append("    remoteTxFlowId: ").append(toIndentedString(remoteTxFlowId)).append("\n");
    sb.append("    rxByteCount: ").append(toIndentedString(rxByteCount)).append("\n");
    sb.append("    rxByteRate: ").append(toIndentedString(rxByteRate)).append("\n");
    sb.append("    rxMsgCount: ").append(toIndentedString(rxMsgCount)).append("\n");
    sb.append("    rxMsgRate: ").append(toIndentedString(rxMsgRate)).append("\n");
    sb.append("    tlsCipherSuiteList: ").append(toIndentedString(tlsCipherSuiteList)).append("\n");
    sb.append("    tlsDefaultCipherSuiteList: ").append(toIndentedString(tlsDefaultCipherSuiteList)).append("\n");
    sb.append("    ttlExceededEventRaised: ").append(toIndentedString(ttlExceededEventRaised)).append("\n");
    sb.append("    txByteCount: ").append(toIndentedString(txByteCount)).append("\n");
    sb.append("    txByteRate: ").append(toIndentedString(txByteRate)).append("\n");
    sb.append("    txMsgCount: ").append(toIndentedString(txMsgCount)).append("\n");
    sb.append("    txMsgRate: ").append(toIndentedString(txMsgRate)).append("\n");
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
