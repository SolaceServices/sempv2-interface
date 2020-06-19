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
 * DmrCluster
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-13T23:07:13.589Z")
public class DmrCluster {
  @SerializedName("authenticationBasicEnabled")
  private Boolean authenticationBasicEnabled = null;

  /**
   * The type of basic authentication to use for Cluster Links. The allowed values and their meaning are:  &lt;pre&gt; \&quot;internal\&quot; - Use locally configured password. \&quot;none\&quot; - No authentication. &lt;/pre&gt; 
   */
  @JsonAdapter(AuthenticationBasicTypeEnum.Adapter.class)
  public enum AuthenticationBasicTypeEnum {
    INTERNAL("internal"),
    
    NONE("none");

    private String value;

    AuthenticationBasicTypeEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static AuthenticationBasicTypeEnum fromValue(String text) {
      for (AuthenticationBasicTypeEnum b : AuthenticationBasicTypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<AuthenticationBasicTypeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final AuthenticationBasicTypeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public AuthenticationBasicTypeEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return AuthenticationBasicTypeEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("authenticationBasicType")
  private AuthenticationBasicTypeEnum authenticationBasicType = null;

  @SerializedName("authenticationClientCertEnabled")
  private Boolean authenticationClientCertEnabled = null;

  @SerializedName("directOnlyEnabled")
  private Boolean directOnlyEnabled = null;

  @SerializedName("dmrClusterName")
  private String dmrClusterName = null;

  @SerializedName("enabled")
  private Boolean enabled = null;

  @SerializedName("failureReason")
  private String failureReason = null;

  @SerializedName("nodeName")
  private String nodeName = null;

  @SerializedName("tlsServerCertEnforceTrustedCommonNameEnabled")
  private Boolean tlsServerCertEnforceTrustedCommonNameEnabled = null;

  @SerializedName("tlsServerCertMaxChainDepth")
  private Long tlsServerCertMaxChainDepth = null;

  @SerializedName("tlsServerCertValidateDateEnabled")
  private Boolean tlsServerCertValidateDateEnabled = null;

  @SerializedName("up")
  private Boolean up = null;

  @SerializedName("uptime")
  private Long uptime = null;

  public DmrCluster authenticationBasicEnabled(Boolean authenticationBasicEnabled) {
    this.authenticationBasicEnabled = authenticationBasicEnabled;
    return this;
  }

   /**
   * Indicates whether basic authentication is enabled for Cluster Links.
   * @return authenticationBasicEnabled
  **/
  @ApiModelProperty(value = "Indicates whether basic authentication is enabled for Cluster Links.")
  public Boolean isAuthenticationBasicEnabled() {
    return authenticationBasicEnabled;
  }

  public void setAuthenticationBasicEnabled(Boolean authenticationBasicEnabled) {
    this.authenticationBasicEnabled = authenticationBasicEnabled;
  }

  public DmrCluster authenticationBasicType(AuthenticationBasicTypeEnum authenticationBasicType) {
    this.authenticationBasicType = authenticationBasicType;
    return this;
  }

   /**
   * The type of basic authentication to use for Cluster Links. The allowed values and their meaning are:  &lt;pre&gt; \&quot;internal\&quot; - Use locally configured password. \&quot;none\&quot; - No authentication. &lt;/pre&gt; 
   * @return authenticationBasicType
  **/
  @ApiModelProperty(value = "The type of basic authentication to use for Cluster Links. The allowed values and their meaning are:  <pre> \"internal\" - Use locally configured password. \"none\" - No authentication. </pre> ")
  public AuthenticationBasicTypeEnum getAuthenticationBasicType() {
    return authenticationBasicType;
  }

  public void setAuthenticationBasicType(AuthenticationBasicTypeEnum authenticationBasicType) {
    this.authenticationBasicType = authenticationBasicType;
  }

  public DmrCluster authenticationClientCertEnabled(Boolean authenticationClientCertEnabled) {
    this.authenticationClientCertEnabled = authenticationClientCertEnabled;
    return this;
  }

   /**
   * Indicates whether client certificate authentication is enabled for Cluster Links.
   * @return authenticationClientCertEnabled
  **/
  @ApiModelProperty(value = "Indicates whether client certificate authentication is enabled for Cluster Links.")
  public Boolean isAuthenticationClientCertEnabled() {
    return authenticationClientCertEnabled;
  }

  public void setAuthenticationClientCertEnabled(Boolean authenticationClientCertEnabled) {
    this.authenticationClientCertEnabled = authenticationClientCertEnabled;
  }

  public DmrCluster directOnlyEnabled(Boolean directOnlyEnabled) {
    this.directOnlyEnabled = directOnlyEnabled;
    return this;
  }

   /**
   * Indicates whether this cluster only supports direct messaging. If true, guaranteed messages will not be transmitted through the cluster.
   * @return directOnlyEnabled
  **/
  @ApiModelProperty(value = "Indicates whether this cluster only supports direct messaging. If true, guaranteed messages will not be transmitted through the cluster.")
  public Boolean isDirectOnlyEnabled() {
    return directOnlyEnabled;
  }

  public void setDirectOnlyEnabled(Boolean directOnlyEnabled) {
    this.directOnlyEnabled = directOnlyEnabled;
  }

  public DmrCluster dmrClusterName(String dmrClusterName) {
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

  public DmrCluster enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

   /**
   * Indicates whether the Cluster is enabled.
   * @return enabled
  **/
  @ApiModelProperty(value = "Indicates whether the Cluster is enabled.")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public DmrCluster failureReason(String failureReason) {
    this.failureReason = failureReason;
    return this;
  }

   /**
   * The failure reason for the Cluster being down.
   * @return failureReason
  **/
  @ApiModelProperty(value = "The failure reason for the Cluster being down.")
  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public DmrCluster nodeName(String nodeName) {
    this.nodeName = nodeName;
    return this;
  }

   /**
   * The name of this node in the Cluster. This is the name that this broker (or redundant group of brokers) is know by to other nodes in the Cluster. The name is chosen automatically to be either this broker&#39;s Router Name or Mate Router Name, depending on which Active Standby Role (primary or backup) this broker plays in its redundancy group.
   * @return nodeName
  **/
  @ApiModelProperty(value = "The name of this node in the Cluster. This is the name that this broker (or redundant group of brokers) is know by to other nodes in the Cluster. The name is chosen automatically to be either this broker's Router Name or Mate Router Name, depending on which Active Standby Role (primary or backup) this broker plays in its redundancy group.")
  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  public DmrCluster tlsServerCertEnforceTrustedCommonNameEnabled(Boolean tlsServerCertEnforceTrustedCommonNameEnabled) {
    this.tlsServerCertEnforceTrustedCommonNameEnabled = tlsServerCertEnforceTrustedCommonNameEnabled;
    return this;
  }

   /**
   * Indicates whether the common name provided by the remote broker is enforced against the list of trusted common names configured for the Link. If enabled, the certificate&#39;s common name must match one of the trusted common names for the Link to be accepted.
   * @return tlsServerCertEnforceTrustedCommonNameEnabled
  **/
  @ApiModelProperty(value = "Indicates whether the common name provided by the remote broker is enforced against the list of trusted common names configured for the Link. If enabled, the certificate's common name must match one of the trusted common names for the Link to be accepted.")
  public Boolean isTlsServerCertEnforceTrustedCommonNameEnabled() {
    return tlsServerCertEnforceTrustedCommonNameEnabled;
  }

  public void setTlsServerCertEnforceTrustedCommonNameEnabled(Boolean tlsServerCertEnforceTrustedCommonNameEnabled) {
    this.tlsServerCertEnforceTrustedCommonNameEnabled = tlsServerCertEnforceTrustedCommonNameEnabled;
  }

  public DmrCluster tlsServerCertMaxChainDepth(Long tlsServerCertMaxChainDepth) {
    this.tlsServerCertMaxChainDepth = tlsServerCertMaxChainDepth;
    return this;
  }

   /**
   * The maximum allowed depth of a certificate chain. The depth of a chain is defined as the number of signing CA certificates that are present in the chain back to a trusted self-signed root CA certificate.
   * @return tlsServerCertMaxChainDepth
  **/
  @ApiModelProperty(value = "The maximum allowed depth of a certificate chain. The depth of a chain is defined as the number of signing CA certificates that are present in the chain back to a trusted self-signed root CA certificate.")
  public Long getTlsServerCertMaxChainDepth() {
    return tlsServerCertMaxChainDepth;
  }

  public void setTlsServerCertMaxChainDepth(Long tlsServerCertMaxChainDepth) {
    this.tlsServerCertMaxChainDepth = tlsServerCertMaxChainDepth;
  }

  public DmrCluster tlsServerCertValidateDateEnabled(Boolean tlsServerCertValidateDateEnabled) {
    this.tlsServerCertValidateDateEnabled = tlsServerCertValidateDateEnabled;
    return this;
  }

   /**
   * Indicates whether validation of the \&quot;Not Before\&quot; and \&quot;Not After\&quot; validity dates in the certificate is enabled. When disabled, the certificate is accepted even if the certificate is not valid based on these dates.
   * @return tlsServerCertValidateDateEnabled
  **/
  @ApiModelProperty(value = "Indicates whether validation of the \"Not Before\" and \"Not After\" validity dates in the certificate is enabled. When disabled, the certificate is accepted even if the certificate is not valid based on these dates.")
  public Boolean isTlsServerCertValidateDateEnabled() {
    return tlsServerCertValidateDateEnabled;
  }

  public void setTlsServerCertValidateDateEnabled(Boolean tlsServerCertValidateDateEnabled) {
    this.tlsServerCertValidateDateEnabled = tlsServerCertValidateDateEnabled;
  }

  public DmrCluster up(Boolean up) {
    this.up = up;
    return this;
  }

   /**
   * Indicates whether the Cluster is operationally up.
   * @return up
  **/
  @ApiModelProperty(value = "Indicates whether the Cluster is operationally up.")
  public Boolean isUp() {
    return up;
  }

  public void setUp(Boolean up) {
    this.up = up;
  }

  public DmrCluster uptime(Long uptime) {
    this.uptime = uptime;
    return this;
  }

   /**
   * The amount of time in seconds since the Cluster was up.
   * @return uptime
  **/
  @ApiModelProperty(value = "The amount of time in seconds since the Cluster was up.")
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
    DmrCluster dmrCluster = (DmrCluster) o;
    return Objects.equals(this.authenticationBasicEnabled, dmrCluster.authenticationBasicEnabled) &&
        Objects.equals(this.authenticationBasicType, dmrCluster.authenticationBasicType) &&
        Objects.equals(this.authenticationClientCertEnabled, dmrCluster.authenticationClientCertEnabled) &&
        Objects.equals(this.directOnlyEnabled, dmrCluster.directOnlyEnabled) &&
        Objects.equals(this.dmrClusterName, dmrCluster.dmrClusterName) &&
        Objects.equals(this.enabled, dmrCluster.enabled) &&
        Objects.equals(this.failureReason, dmrCluster.failureReason) &&
        Objects.equals(this.nodeName, dmrCluster.nodeName) &&
        Objects.equals(this.tlsServerCertEnforceTrustedCommonNameEnabled, dmrCluster.tlsServerCertEnforceTrustedCommonNameEnabled) &&
        Objects.equals(this.tlsServerCertMaxChainDepth, dmrCluster.tlsServerCertMaxChainDepth) &&
        Objects.equals(this.tlsServerCertValidateDateEnabled, dmrCluster.tlsServerCertValidateDateEnabled) &&
        Objects.equals(this.up, dmrCluster.up) &&
        Objects.equals(this.uptime, dmrCluster.uptime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authenticationBasicEnabled, authenticationBasicType, authenticationClientCertEnabled, directOnlyEnabled, dmrClusterName, enabled, failureReason, nodeName, tlsServerCertEnforceTrustedCommonNameEnabled, tlsServerCertMaxChainDepth, tlsServerCertValidateDateEnabled, up, uptime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DmrCluster {\n");
    
    sb.append("    authenticationBasicEnabled: ").append(toIndentedString(authenticationBasicEnabled)).append("\n");
    sb.append("    authenticationBasicType: ").append(toIndentedString(authenticationBasicType)).append("\n");
    sb.append("    authenticationClientCertEnabled: ").append(toIndentedString(authenticationClientCertEnabled)).append("\n");
    sb.append("    directOnlyEnabled: ").append(toIndentedString(directOnlyEnabled)).append("\n");
    sb.append("    dmrClusterName: ").append(toIndentedString(dmrClusterName)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    failureReason: ").append(toIndentedString(failureReason)).append("\n");
    sb.append("    nodeName: ").append(toIndentedString(nodeName)).append("\n");
    sb.append("    tlsServerCertEnforceTrustedCommonNameEnabled: ").append(toIndentedString(tlsServerCertEnforceTrustedCommonNameEnabled)).append("\n");
    sb.append("    tlsServerCertMaxChainDepth: ").append(toIndentedString(tlsServerCertMaxChainDepth)).append("\n");
    sb.append("    tlsServerCertValidateDateEnabled: ").append(toIndentedString(tlsServerCertValidateDateEnabled)).append("\n");
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

