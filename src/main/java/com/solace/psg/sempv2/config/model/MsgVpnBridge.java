/*
 * SEMP (Solace Element Management Protocol)
 * SEMP (starting in `v2`, see note 1) is a RESTful API for configuring, monitoring, and administering a Solace PubSub+ broker.  SEMP uses URIs to address manageable **resources** of the Solace PubSub+ broker. Resources are individual **objects**, **collections** of objects, or (exclusively in the action API) **actions**. This document applies to the following API:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Configuration|/SEMP/v2/config|Reading and writing config state|See note 2    The following APIs are also available:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Action|/SEMP/v2/action|Performing actions|See note 2 Monitoring|/SEMP/v2/monitor|Querying operational parameters|See note 2    Resources are always nouns, with individual objects being singular and collections being plural.  Objects within a collection are identified by an `obj-id`, which follows the collection name with the form `collection-name/obj-id`.  Actions within an object are identified by an `action-id`, which follows the object name with the form `obj-id/action-id`.  Some examples:  ``` /SEMP/v2/config/msgVpns                        ; MsgVpn collection /SEMP/v2/config/msgVpns/a                      ; MsgVpn object named \"a\" /SEMP/v2/config/msgVpns/a/queues               ; Queue collection in MsgVpn \"a\" /SEMP/v2/config/msgVpns/a/queues/b             ; Queue object named \"b\" in MsgVpn \"a\" /SEMP/v2/action/msgVpns/a/queues/b/startReplay ; Action that starts a replay on Queue \"b\" in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients             ; Client collection in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients/c           ; Client object named \"c\" in MsgVpn \"a\" ```  ## Collection Resources  Collections are unordered lists of objects (unless described as otherwise), and are described by JSON arrays. Each item in the array represents an object in the same manner as the individual object would normally be represented. In the configuration API, the creation of a new object is done through its collection resource.  ## Object and Action Resources  Objects are composed of attributes, actions, collections, and other objects. They are described by JSON objects as name/value pairs. The collections and actions of an object are not contained directly in the object's JSON content; rather the content includes an attribute containing a URI which points to the collections and actions. These contained resources must be managed through this URI. At a minimum, every object has one or more identifying attributes, and its own `uri` attribute which contains the URI pointing to itself.  Actions are also composed of attributes, and are described by JSON objects as name/value pairs. Unlike objects, however, they are not members of a collection and cannot be retrieved, only performed. Actions only exist in the action API.  Attributes in an object or action may have any (non-exclusively) of the following properties:   Property|Meaning|Comments :---|:---|:--- Identifying|Attribute is involved in unique identification of the object, and appears in its URI| Required|Attribute must be provided in the request| Read-Only|Attribute can only be read, not written|See note 3 Write-Only|Attribute can only be written, not read| Requires-Disable|Attribute can only be changed when object is disabled| Deprecated|Attribute is deprecated, and will disappear in the next SEMP version|    In some requests, certain attributes may only be provided in certain combinations with other attributes:   Relationship|Meaning :---|:--- Requires|Attribute may only be changed by a request if a particular attribute or combination of attributes is also provided in the request Conflicts|Attribute may only be provided in a request if a particular attribute or combination of attributes is not also provided in the request    ## HTTP Methods  The following HTTP methods manipulate resources in accordance with these general principles. Note that some methods are only used in certain APIs:   Method|Resource|Meaning|Request Body|Response Body|Missing Request Attributes :---|:---|:---|:---|:---|:--- POST|Collection|Create object|Initial attribute values|Object attributes and metadata|Set to default PUT|Object|Create or replace object|New attribute values|Object attributes and metadata|Set to default (but see note 4) PUT|Action|Performs action|Action arguments|Action metadata|N/A PATCH|Object|Update object|New attribute values|Object attributes and metadata|unchanged DELETE|Object|Delete object|Empty|Object metadata|N/A GET|Object|Get object|Empty|Object attributes and metadata|N/A GET|Collection|Get collection|Empty|Object attributes and collection metadata|N/A    ## Common Query Parameters  The following are some common query parameters that are supported by many method/URI combinations. Individual URIs may document additional parameters. Note that multiple query parameters can be used together in a single URI, separated by the ampersand character. For example:  ``` ; Request for the MsgVpns collection using two hypothetical query parameters \"q1\" and \"q2\" ; with values \"val1\" and \"val2\" respectively /SEMP/v2/config/msgVpns?q1=val1&q2=val2 ```  ### select  Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. Use this query parameter to limit the size of the returned data for each returned object, return only those fields that are desired, or exclude fields that are not desired.  The value of `select` is a comma-separated list of attribute names. If the list contains attribute names that are not prefaced by `-`, only those attributes are included in the response. If the list contains attribute names that are prefaced by `-`, those attributes are excluded from the response. If the list contains both types, then the difference of the first set of attributes and the second set of attributes is returned. If the list is empty (i.e. `select=`), no attributes are returned.  All attributes that are prefaced by `-` must follow all attributes that are not prefaced by `-`. In addition, each attribute name in the list must match at least one attribute in the object.  Names may include the `*` wildcard (zero or more characters). Nested attribute names are supported using periods (e.g. `parentName.childName`).  Some examples:  ``` ; List of all MsgVpn names /SEMP/v2/config/msgVpns?select=msgVpnName ; List of all MsgVpn and their attributes except for their names /SEMP/v2/config/msgVpns?select=-msgVpnName ; Authentication attributes of MsgVpn \"finance\" /SEMP/v2/config/msgVpns/finance?select=authentication* ; All attributes of MsgVpn \"finance\" except for authentication attributes /SEMP/v2/config/msgVpns/finance?select=-authentication* ; Access related attributes of Queue \"orderQ\" of MsgVpn \"finance\" /SEMP/v2/config/msgVpns/finance/queues/orderQ?select=owner,permission ```  ### where  Include in the response only objects where certain conditions are true. Use this query parameter to limit which objects are returned to those whose attribute values meet the given conditions.  The value of `where` is a comma-separated list of expressions. All expressions must be true for the object to be included in the response. Each expression takes the form:  ``` expression  = attribute-name OP value OP          = '==' | '!=' | '&lt;' | '&gt;' | '&lt;=' | '&gt;=' ```  `value` may be a number, string, `true`, or `false`, as appropriate for the type of `attribute-name`. Greater-than and less-than comparisons only work for numbers. A `*` in a string `value` is interpreted as a wildcard (zero or more characters). Some examples:  ``` ; Only enabled MsgVpns /SEMP/v2/config/msgVpns?where=enabled==true ; Only MsgVpns using basic non-LDAP authentication /SEMP/v2/config/msgVpns?where=authenticationBasicEnabled==true,authenticationBasicType!=ldap ; Only MsgVpns that allow more than 100 client connections /SEMP/v2/config/msgVpns?where=maxConnectionCount>100 ; Only MsgVpns with msgVpnName starting with \"B\": /SEMP/v2/config/msgVpns?where=msgVpnName==B* ```  ### count  Limit the count of objects in the response. This can be useful to limit the size of the response for large collections. The minimum value for `count` is `1` and the default is `10`. There is also a per-collection maximum value to limit request handling time. For example:  ``` ; Up to 25 MsgVpns /SEMP/v2/config/msgVpns?count=25 ```  ### cursor  The cursor, or position, for the next page of objects. Cursors are opaque data that should not be created or interpreted by SEMP clients, and should only be used as described below.  When a request is made for a collection and there may be additional objects available for retrieval that are not included in the initial response, the response will include a `cursorQuery` field containing a cursor. The value of this field can be specified in the `cursor` query parameter of a subsequent request to retrieve the next page of objects. For convenience, an appropriate URI is constructed automatically by the broker and included in the `nextPageUri` field of the response. This URI can be used directly to retrieve the next page of objects.  ## Notes  Note|Description :---:|:--- 1|This specification defines SEMP starting in \"v2\", and not the original SEMP \"v1\" interface. Request and response formats between \"v1\" and \"v2\" are entirely incompatible, although both protocols share a common port configuration on the Solace PubSub+ broker. They are differentiated by the initial portion of the URI path, one of either \"/SEMP/\" or \"/SEMP/v2/\" 2|This API is partially implemented. Only a subset of all objects are available. 3|Read-only attributes may appear in POST and PUT/PATCH requests. However, if a read-only attribute is not marked as identifying, it will be ignored during a PUT/PATCH. 4|For PUT, if the SEMP user is not authorized to modify the attribute, its value is left unchanged rather than set to default. In addition, the values of write-only attributes are not set to their defaults on a PUT. If the object does not exist, it is created first.    
 *
 * OpenAPI spec version: 2.14
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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * MsgVpnBridge
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-12T16:43:32.646Z")
public class MsgVpnBridge {
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

  @SerializedName("enabled")
  private Boolean enabled = null;

  @SerializedName("maxTtl")
  private Long maxTtl = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("remoteAuthenticationBasicClientUsername")
  private String remoteAuthenticationBasicClientUsername = null;

  @SerializedName("remoteAuthenticationBasicPassword")
  private String remoteAuthenticationBasicPassword = null;

  @SerializedName("remoteAuthenticationClientCertContent")
  private String remoteAuthenticationClientCertContent = null;

  @SerializedName("remoteAuthenticationClientCertPassword")
  private String remoteAuthenticationClientCertPassword = null;

  /**
   * The authentication scheme for the remote Message VPN. The default value is &#x60;\&quot;basic\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;basic\&quot; - Basic Authentication Scheme (via username and password). \&quot;client-certificate\&quot; - Client Certificate Authentication Scheme (via certificate file or content). &lt;/pre&gt; 
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
        String value = jsonReader.nextString();
        return RemoteAuthenticationSchemeEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("remoteAuthenticationScheme")
  private RemoteAuthenticationSchemeEnum remoteAuthenticationScheme = null;

  @SerializedName("remoteConnectionRetryCount")
  private Long remoteConnectionRetryCount = null;

  @SerializedName("remoteConnectionRetryDelay")
  private Long remoteConnectionRetryDelay = null;

  /**
   * The priority for deliver-to-one (DTO) messages transmitted from the remote Message VPN. The default value is &#x60;\&quot;p1\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;p1\&quot; - The 1st or highest priority. \&quot;p2\&quot; - The 2nd highest priority. \&quot;p3\&quot; - The 3rd highest priority. \&quot;p4\&quot; - The 4th highest priority. \&quot;da\&quot; - Ignore priority and deliver always. &lt;/pre&gt; 
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
        String value = jsonReader.nextString();
        return RemoteDeliverToOnePriorityEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("remoteDeliverToOnePriority")
  private RemoteDeliverToOnePriorityEnum remoteDeliverToOnePriority = null;

  @SerializedName("tlsCipherSuiteList")
  private String tlsCipherSuiteList = null;

  public MsgVpnBridge bridgeName(String bridgeName) {
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

  public MsgVpnBridge bridgeVirtualRouter(BridgeVirtualRouterEnum bridgeVirtualRouter) {
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

  public MsgVpnBridge enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

   /**
   * Enable or disable the Bridge. The default value is &#x60;false&#x60;.
   * @return enabled
  **/
  @ApiModelProperty(value = "Enable or disable the Bridge. The default value is `false`.")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public MsgVpnBridge maxTtl(Long maxTtl) {
    this.maxTtl = maxTtl;
    return this;
  }

   /**
   * The maximum time-to-live (TTL) in hops. Messages are discarded if their TTL exceeds this value. The default value is &#x60;8&#x60;.
   * @return maxTtl
  **/
  @ApiModelProperty(value = "The maximum time-to-live (TTL) in hops. Messages are discarded if their TTL exceeds this value. The default value is `8`.")
  public Long getMaxTtl() {
    return maxTtl;
  }

  public void setMaxTtl(Long maxTtl) {
    this.maxTtl = maxTtl;
  }

  public MsgVpnBridge msgVpnName(String msgVpnName) {
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

  public MsgVpnBridge remoteAuthenticationBasicClientUsername(String remoteAuthenticationBasicClientUsername) {
    this.remoteAuthenticationBasicClientUsername = remoteAuthenticationBasicClientUsername;
    return this;
  }

   /**
   * The Client Username the Bridge uses to login to the remote Message VPN. The default value is &#x60;\&quot;\&quot;&#x60;.
   * @return remoteAuthenticationBasicClientUsername
  **/
  @ApiModelProperty(value = "The Client Username the Bridge uses to login to the remote Message VPN. The default value is `\"\"`.")
  public String getRemoteAuthenticationBasicClientUsername() {
    return remoteAuthenticationBasicClientUsername;
  }

  public void setRemoteAuthenticationBasicClientUsername(String remoteAuthenticationBasicClientUsername) {
    this.remoteAuthenticationBasicClientUsername = remoteAuthenticationBasicClientUsername;
  }

  public MsgVpnBridge remoteAuthenticationBasicPassword(String remoteAuthenticationBasicPassword) {
    this.remoteAuthenticationBasicPassword = remoteAuthenticationBasicPassword;
    return this;
  }

   /**
   * The password for the Client Username. This attribute is absent from a GET and not updated when absent in a PUT. The default is to have no &#x60;remoteAuthenticationBasicPassword&#x60;.
   * @return remoteAuthenticationBasicPassword
  **/
  @ApiModelProperty(value = "The password for the Client Username. This attribute is absent from a GET and not updated when absent in a PUT. The default is to have no `remoteAuthenticationBasicPassword`.")
  public String getRemoteAuthenticationBasicPassword() {
    return remoteAuthenticationBasicPassword;
  }

  public void setRemoteAuthenticationBasicPassword(String remoteAuthenticationBasicPassword) {
    this.remoteAuthenticationBasicPassword = remoteAuthenticationBasicPassword;
  }

  public MsgVpnBridge remoteAuthenticationClientCertContent(String remoteAuthenticationClientCertContent) {
    this.remoteAuthenticationClientCertContent = remoteAuthenticationClientCertContent;
    return this;
  }

   /**
   * The PEM formatted content for the client certificate used by the Bridge to login to the remote Message VPN. It must consist of a private key and between one and three certificates comprising the certificate trust chain. This attribute is absent from a GET and not updated when absent in a PUT. Changing this attribute requires an HTTPS connection. The default value is &#x60;\&quot;\&quot;&#x60;. Available since 2.9.
   * @return remoteAuthenticationClientCertContent
  **/
  @ApiModelProperty(value = "The PEM formatted content for the client certificate used by the Bridge to login to the remote Message VPN. It must consist of a private key and between one and three certificates comprising the certificate trust chain. This attribute is absent from a GET and not updated when absent in a PUT. Changing this attribute requires an HTTPS connection. The default value is `\"\"`. Available since 2.9.")
  public String getRemoteAuthenticationClientCertContent() {
    return remoteAuthenticationClientCertContent;
  }

  public void setRemoteAuthenticationClientCertContent(String remoteAuthenticationClientCertContent) {
    this.remoteAuthenticationClientCertContent = remoteAuthenticationClientCertContent;
  }

  public MsgVpnBridge remoteAuthenticationClientCertPassword(String remoteAuthenticationClientCertPassword) {
    this.remoteAuthenticationClientCertPassword = remoteAuthenticationClientCertPassword;
    return this;
  }

   /**
   * The password for the client certificate. This attribute is absent from a GET and not updated when absent in a PUT. Changing this attribute requires an HTTPS connection. The default value is &#x60;\&quot;\&quot;&#x60;. Available since 2.9.
   * @return remoteAuthenticationClientCertPassword
  **/
  @ApiModelProperty(value = "The password for the client certificate. This attribute is absent from a GET and not updated when absent in a PUT. Changing this attribute requires an HTTPS connection. The default value is `\"\"`. Available since 2.9.")
  public String getRemoteAuthenticationClientCertPassword() {
    return remoteAuthenticationClientCertPassword;
  }

  public void setRemoteAuthenticationClientCertPassword(String remoteAuthenticationClientCertPassword) {
    this.remoteAuthenticationClientCertPassword = remoteAuthenticationClientCertPassword;
  }

  public MsgVpnBridge remoteAuthenticationScheme(RemoteAuthenticationSchemeEnum remoteAuthenticationScheme) {
    this.remoteAuthenticationScheme = remoteAuthenticationScheme;
    return this;
  }

   /**
   * The authentication scheme for the remote Message VPN. The default value is &#x60;\&quot;basic\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;basic\&quot; - Basic Authentication Scheme (via username and password). \&quot;client-certificate\&quot; - Client Certificate Authentication Scheme (via certificate file or content). &lt;/pre&gt; 
   * @return remoteAuthenticationScheme
  **/
  @ApiModelProperty(value = "The authentication scheme for the remote Message VPN. The default value is `\"basic\"`. The allowed values and their meaning are:  <pre> \"basic\" - Basic Authentication Scheme (via username and password). \"client-certificate\" - Client Certificate Authentication Scheme (via certificate file or content). </pre> ")
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
   * The maximum number of retry attempts to establish a connection to the remote Message VPN. A value of 0 means to retry forever. The default value is &#x60;0&#x60;.
   * @return remoteConnectionRetryCount
  **/
  @ApiModelProperty(value = "The maximum number of retry attempts to establish a connection to the remote Message VPN. A value of 0 means to retry forever. The default value is `0`.")
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
   * The number of seconds to delay before retrying to connect to the remote Message VPN. The default value is &#x60;3&#x60;.
   * @return remoteConnectionRetryDelay
  **/
  @ApiModelProperty(value = "The number of seconds to delay before retrying to connect to the remote Message VPN. The default value is `3`.")
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
   * The priority for deliver-to-one (DTO) messages transmitted from the remote Message VPN. The default value is &#x60;\&quot;p1\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;p1\&quot; - The 1st or highest priority. \&quot;p2\&quot; - The 2nd highest priority. \&quot;p3\&quot; - The 3rd highest priority. \&quot;p4\&quot; - The 4th highest priority. \&quot;da\&quot; - Ignore priority and deliver always. &lt;/pre&gt; 
   * @return remoteDeliverToOnePriority
  **/
  @ApiModelProperty(value = "The priority for deliver-to-one (DTO) messages transmitted from the remote Message VPN. The default value is `\"p1\"`. The allowed values and their meaning are:  <pre> \"p1\" - The 1st or highest priority. \"p2\" - The 2nd highest priority. \"p3\" - The 3rd highest priority. \"p4\" - The 4th highest priority. \"da\" - Ignore priority and deliver always. </pre> ")
  public RemoteDeliverToOnePriorityEnum getRemoteDeliverToOnePriority() {
    return remoteDeliverToOnePriority;
  }

  public void setRemoteDeliverToOnePriority(RemoteDeliverToOnePriorityEnum remoteDeliverToOnePriority) {
    this.remoteDeliverToOnePriority = remoteDeliverToOnePriority;
  }

  public MsgVpnBridge tlsCipherSuiteList(String tlsCipherSuiteList) {
    this.tlsCipherSuiteList = tlsCipherSuiteList;
    return this;
  }

   /**
   * The colon-separated list of cipher suites supported for TLS connections to the remote Message VPN. The value \&quot;default\&quot; implies all supported suites ordered from most secure to least secure. The default value is &#x60;\&quot;default\&quot;&#x60;.
   * @return tlsCipherSuiteList
  **/
  @ApiModelProperty(value = "The colon-separated list of cipher suites supported for TLS connections to the remote Message VPN. The value \"default\" implies all supported suites ordered from most secure to least secure. The default value is `\"default\"`.")
  public String getTlsCipherSuiteList() {
    return tlsCipherSuiteList;
  }

  public void setTlsCipherSuiteList(String tlsCipherSuiteList) {
    this.tlsCipherSuiteList = tlsCipherSuiteList;
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
    return Objects.equals(this.bridgeName, msgVpnBridge.bridgeName) &&
        Objects.equals(this.bridgeVirtualRouter, msgVpnBridge.bridgeVirtualRouter) &&
        Objects.equals(this.enabled, msgVpnBridge.enabled) &&
        Objects.equals(this.maxTtl, msgVpnBridge.maxTtl) &&
        Objects.equals(this.msgVpnName, msgVpnBridge.msgVpnName) &&
        Objects.equals(this.remoteAuthenticationBasicClientUsername, msgVpnBridge.remoteAuthenticationBasicClientUsername) &&
        Objects.equals(this.remoteAuthenticationBasicPassword, msgVpnBridge.remoteAuthenticationBasicPassword) &&
        Objects.equals(this.remoteAuthenticationClientCertContent, msgVpnBridge.remoteAuthenticationClientCertContent) &&
        Objects.equals(this.remoteAuthenticationClientCertPassword, msgVpnBridge.remoteAuthenticationClientCertPassword) &&
        Objects.equals(this.remoteAuthenticationScheme, msgVpnBridge.remoteAuthenticationScheme) &&
        Objects.equals(this.remoteConnectionRetryCount, msgVpnBridge.remoteConnectionRetryCount) &&
        Objects.equals(this.remoteConnectionRetryDelay, msgVpnBridge.remoteConnectionRetryDelay) &&
        Objects.equals(this.remoteDeliverToOnePriority, msgVpnBridge.remoteDeliverToOnePriority) &&
        Objects.equals(this.tlsCipherSuiteList, msgVpnBridge.tlsCipherSuiteList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bridgeName, bridgeVirtualRouter, enabled, maxTtl, msgVpnName, remoteAuthenticationBasicClientUsername, remoteAuthenticationBasicPassword, remoteAuthenticationClientCertContent, remoteAuthenticationClientCertPassword, remoteAuthenticationScheme, remoteConnectionRetryCount, remoteConnectionRetryDelay, remoteDeliverToOnePriority, tlsCipherSuiteList);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnBridge {\n");
    
    sb.append("    bridgeName: ").append(toIndentedString(bridgeName)).append("\n");
    sb.append("    bridgeVirtualRouter: ").append(toIndentedString(bridgeVirtualRouter)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    maxTtl: ").append(toIndentedString(maxTtl)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    remoteAuthenticationBasicClientUsername: ").append(toIndentedString(remoteAuthenticationBasicClientUsername)).append("\n");
    sb.append("    remoteAuthenticationBasicPassword: ").append(toIndentedString(remoteAuthenticationBasicPassword)).append("\n");
    sb.append("    remoteAuthenticationClientCertContent: ").append(toIndentedString(remoteAuthenticationClientCertContent)).append("\n");
    sb.append("    remoteAuthenticationClientCertPassword: ").append(toIndentedString(remoteAuthenticationClientCertPassword)).append("\n");
    sb.append("    remoteAuthenticationScheme: ").append(toIndentedString(remoteAuthenticationScheme)).append("\n");
    sb.append("    remoteConnectionRetryCount: ").append(toIndentedString(remoteConnectionRetryCount)).append("\n");
    sb.append("    remoteConnectionRetryDelay: ").append(toIndentedString(remoteConnectionRetryDelay)).append("\n");
    sb.append("    remoteDeliverToOnePriority: ").append(toIndentedString(remoteDeliverToOnePriority)).append("\n");
    sb.append("    tlsCipherSuiteList: ").append(toIndentedString(tlsCipherSuiteList)).append("\n");
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

