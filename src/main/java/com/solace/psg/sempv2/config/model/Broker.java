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
 * Broker
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-12T16:43:32.646Z")
public class Broker {
  /**
   * The client certificate revocation checking mode used when a client authenticates with a client certificate. The default value is &#x60;\&quot;none\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;none\&quot; - Do not perform any certificate revocation checking. \&quot;ocsp\&quot; - Use the Open Certificate Status Protcol (OCSP) for certificate revocation checking. \&quot;crl\&quot; - Use Certificate Revocation Lists (CRL) for certificate revocation checking. \&quot;ocsp-crl\&quot; - Use OCSP first, but if OCSP fails to return an unambiguous result, then check via CRL. &lt;/pre&gt; 
   */
  @JsonAdapter(AuthClientCertRevocationCheckModeEnum.Adapter.class)
  public enum AuthClientCertRevocationCheckModeEnum {
    NONE("none"),
    
    OCSP("ocsp"),
    
    CRL("crl"),
    
    OCSP_CRL("ocsp-crl");

    private String value;

    AuthClientCertRevocationCheckModeEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static AuthClientCertRevocationCheckModeEnum fromValue(String text) {
      for (AuthClientCertRevocationCheckModeEnum b : AuthClientCertRevocationCheckModeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<AuthClientCertRevocationCheckModeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final AuthClientCertRevocationCheckModeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public AuthClientCertRevocationCheckModeEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return AuthClientCertRevocationCheckModeEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("authClientCertRevocationCheckMode")
  private AuthClientCertRevocationCheckModeEnum authClientCertRevocationCheckMode = null;

  @SerializedName("tlsBlockVersion10Enabled")
  private Boolean tlsBlockVersion10Enabled = null;

  @SerializedName("tlsBlockVersion11Enabled")
  private Boolean tlsBlockVersion11Enabled = null;

  @SerializedName("tlsCipherSuiteManagementList")
  private String tlsCipherSuiteManagementList = null;

  @SerializedName("tlsCipherSuiteMsgBackboneList")
  private String tlsCipherSuiteMsgBackboneList = null;

  @SerializedName("tlsCipherSuiteSecureShellList")
  private String tlsCipherSuiteSecureShellList = null;

  @SerializedName("tlsCrimeExploitProtectionEnabled")
  private Boolean tlsCrimeExploitProtectionEnabled = null;

  @SerializedName("tlsServerCertContent")
  private String tlsServerCertContent = null;

  @SerializedName("tlsServerCertPassword")
  private String tlsServerCertPassword = null;

  @SerializedName("tlsTicketLifetime")
  private Integer tlsTicketLifetime = null;

  public Broker authClientCertRevocationCheckMode(AuthClientCertRevocationCheckModeEnum authClientCertRevocationCheckMode) {
    this.authClientCertRevocationCheckMode = authClientCertRevocationCheckMode;
    return this;
  }

   /**
   * The client certificate revocation checking mode used when a client authenticates with a client certificate. The default value is &#x60;\&quot;none\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;none\&quot; - Do not perform any certificate revocation checking. \&quot;ocsp\&quot; - Use the Open Certificate Status Protcol (OCSP) for certificate revocation checking. \&quot;crl\&quot; - Use Certificate Revocation Lists (CRL) for certificate revocation checking. \&quot;ocsp-crl\&quot; - Use OCSP first, but if OCSP fails to return an unambiguous result, then check via CRL. &lt;/pre&gt; 
   * @return authClientCertRevocationCheckMode
  **/
  @ApiModelProperty(value = "The client certificate revocation checking mode used when a client authenticates with a client certificate. The default value is `\"none\"`. The allowed values and their meaning are:  <pre> \"none\" - Do not perform any certificate revocation checking. \"ocsp\" - Use the Open Certificate Status Protcol (OCSP) for certificate revocation checking. \"crl\" - Use Certificate Revocation Lists (CRL) for certificate revocation checking. \"ocsp-crl\" - Use OCSP first, but if OCSP fails to return an unambiguous result, then check via CRL. </pre> ")
  public AuthClientCertRevocationCheckModeEnum getAuthClientCertRevocationCheckMode() {
    return authClientCertRevocationCheckMode;
  }

  public void setAuthClientCertRevocationCheckMode(AuthClientCertRevocationCheckModeEnum authClientCertRevocationCheckMode) {
    this.authClientCertRevocationCheckMode = authClientCertRevocationCheckMode;
  }

  public Broker tlsBlockVersion10Enabled(Boolean tlsBlockVersion10Enabled) {
    this.tlsBlockVersion10Enabled = tlsBlockVersion10Enabled;
    return this;
  }

   /**
   * Enable or disable the blocking of incoming TLS version 1.0 connections. When blocked, existing TLS 1.0 connections from Clients and SEMP users remain connected while new connections are blocked. Note that support for TLS 1.0 will eventually be discontinued, at which time TLS 1.0 connections will be blocked regardless of this setting. The default value is &#x60;true&#x60;.
   * @return tlsBlockVersion10Enabled
  **/
  @ApiModelProperty(value = "Enable or disable the blocking of incoming TLS version 1.0 connections. When blocked, existing TLS 1.0 connections from Clients and SEMP users remain connected while new connections are blocked. Note that support for TLS 1.0 will eventually be discontinued, at which time TLS 1.0 connections will be blocked regardless of this setting. The default value is `true`.")
  public Boolean isTlsBlockVersion10Enabled() {
    return tlsBlockVersion10Enabled;
  }

  public void setTlsBlockVersion10Enabled(Boolean tlsBlockVersion10Enabled) {
    this.tlsBlockVersion10Enabled = tlsBlockVersion10Enabled;
  }

  public Broker tlsBlockVersion11Enabled(Boolean tlsBlockVersion11Enabled) {
    this.tlsBlockVersion11Enabled = tlsBlockVersion11Enabled;
    return this;
  }

   /**
   * Enable or disable the blocking of TLS version 1.1 connections. When blocked, all existing incoming and outgoing TLS 1.1 connections with Clients, SEMP users, and LDAP servers remain connected while new connections are blocked. Note that support for TLS 1.1 will eventually be discontinued, at which time TLS 1.1 connections will be blocked regardless of this setting. The default value is &#x60;false&#x60;.
   * @return tlsBlockVersion11Enabled
  **/
  @ApiModelProperty(value = "Enable or disable the blocking of TLS version 1.1 connections. When blocked, all existing incoming and outgoing TLS 1.1 connections with Clients, SEMP users, and LDAP servers remain connected while new connections are blocked. Note that support for TLS 1.1 will eventually be discontinued, at which time TLS 1.1 connections will be blocked regardless of this setting. The default value is `false`.")
  public Boolean isTlsBlockVersion11Enabled() {
    return tlsBlockVersion11Enabled;
  }

  public void setTlsBlockVersion11Enabled(Boolean tlsBlockVersion11Enabled) {
    this.tlsBlockVersion11Enabled = tlsBlockVersion11Enabled;
  }

  public Broker tlsCipherSuiteManagementList(String tlsCipherSuiteManagementList) {
    this.tlsCipherSuiteManagementList = tlsCipherSuiteManagementList;
    return this;
  }

   /**
   * The colon-separated list of cipher suites used for TLS management connections (e.g. SEMP, LDAP). The value \&quot;default\&quot; implies all supported suites ordered from most secure to least secure. The default value is &#x60;\&quot;default\&quot;&#x60;.
   * @return tlsCipherSuiteManagementList
  **/
  @ApiModelProperty(value = "The colon-separated list of cipher suites used for TLS management connections (e.g. SEMP, LDAP). The value \"default\" implies all supported suites ordered from most secure to least secure. The default value is `\"default\"`.")
  public String getTlsCipherSuiteManagementList() {
    return tlsCipherSuiteManagementList;
  }

  public void setTlsCipherSuiteManagementList(String tlsCipherSuiteManagementList) {
    this.tlsCipherSuiteManagementList = tlsCipherSuiteManagementList;
  }

  public Broker tlsCipherSuiteMsgBackboneList(String tlsCipherSuiteMsgBackboneList) {
    this.tlsCipherSuiteMsgBackboneList = tlsCipherSuiteMsgBackboneList;
    return this;
  }

   /**
   * The colon-separated list of cipher suites used for TLS data connections (e.g. client pub/sub). The value \&quot;default\&quot; implies all supported suites ordered from most secure to least secure. The default value is &#x60;\&quot;default\&quot;&#x60;.
   * @return tlsCipherSuiteMsgBackboneList
  **/
  @ApiModelProperty(value = "The colon-separated list of cipher suites used for TLS data connections (e.g. client pub/sub). The value \"default\" implies all supported suites ordered from most secure to least secure. The default value is `\"default\"`.")
  public String getTlsCipherSuiteMsgBackboneList() {
    return tlsCipherSuiteMsgBackboneList;
  }

  public void setTlsCipherSuiteMsgBackboneList(String tlsCipherSuiteMsgBackboneList) {
    this.tlsCipherSuiteMsgBackboneList = tlsCipherSuiteMsgBackboneList;
  }

  public Broker tlsCipherSuiteSecureShellList(String tlsCipherSuiteSecureShellList) {
    this.tlsCipherSuiteSecureShellList = tlsCipherSuiteSecureShellList;
    return this;
  }

   /**
   * The colon-separated list of cipher suites used for TLS secure shell connections (e.g. SSH, SFTP, SCP). The value \&quot;default\&quot; implies all supported suites ordered from most secure to least secure. The default value is &#x60;\&quot;default\&quot;&#x60;.
   * @return tlsCipherSuiteSecureShellList
  **/
  @ApiModelProperty(value = "The colon-separated list of cipher suites used for TLS secure shell connections (e.g. SSH, SFTP, SCP). The value \"default\" implies all supported suites ordered from most secure to least secure. The default value is `\"default\"`.")
  public String getTlsCipherSuiteSecureShellList() {
    return tlsCipherSuiteSecureShellList;
  }

  public void setTlsCipherSuiteSecureShellList(String tlsCipherSuiteSecureShellList) {
    this.tlsCipherSuiteSecureShellList = tlsCipherSuiteSecureShellList;
  }

  public Broker tlsCrimeExploitProtectionEnabled(Boolean tlsCrimeExploitProtectionEnabled) {
    this.tlsCrimeExploitProtectionEnabled = tlsCrimeExploitProtectionEnabled;
    return this;
  }

   /**
   * Enable or disable protection against the CRIME exploit. When enabled, TLS+compressed messaging performance is degraded. This protection should only be disabled if sufficient ACL and authentication features are being employed such that a potential attacker does not have sufficient access to trigger the exploit. The default value is &#x60;true&#x60;.
   * @return tlsCrimeExploitProtectionEnabled
  **/
  @ApiModelProperty(value = "Enable or disable protection against the CRIME exploit. When enabled, TLS+compressed messaging performance is degraded. This protection should only be disabled if sufficient ACL and authentication features are being employed such that a potential attacker does not have sufficient access to trigger the exploit. The default value is `true`.")
  public Boolean isTlsCrimeExploitProtectionEnabled() {
    return tlsCrimeExploitProtectionEnabled;
  }

  public void setTlsCrimeExploitProtectionEnabled(Boolean tlsCrimeExploitProtectionEnabled) {
    this.tlsCrimeExploitProtectionEnabled = tlsCrimeExploitProtectionEnabled;
  }

  public Broker tlsServerCertContent(String tlsServerCertContent) {
    this.tlsServerCertContent = tlsServerCertContent;
    return this;
  }

   /**
   * The PEM formatted content for the server certificate used for TLS connections. It must consist of a private key and between one and three certificates comprising the certificate trust chain. This attribute is absent from a GET and not updated when absent in a PUT. Changing this attribute requires an HTTPS connection. The default value is &#x60;\&quot;\&quot;&#x60;.
   * @return tlsServerCertContent
  **/
  @ApiModelProperty(value = "The PEM formatted content for the server certificate used for TLS connections. It must consist of a private key and between one and three certificates comprising the certificate trust chain. This attribute is absent from a GET and not updated when absent in a PUT. Changing this attribute requires an HTTPS connection. The default value is `\"\"`.")
  public String getTlsServerCertContent() {
    return tlsServerCertContent;
  }

  public void setTlsServerCertContent(String tlsServerCertContent) {
    this.tlsServerCertContent = tlsServerCertContent;
  }

  public Broker tlsServerCertPassword(String tlsServerCertPassword) {
    this.tlsServerCertPassword = tlsServerCertPassword;
    return this;
  }

   /**
   * The password for the server certificate used for TLS connections. This attribute is absent from a GET and not updated when absent in a PUT. Changing this attribute requires an HTTPS connection. The default value is &#x60;\&quot;\&quot;&#x60;.
   * @return tlsServerCertPassword
  **/
  @ApiModelProperty(value = "The password for the server certificate used for TLS connections. This attribute is absent from a GET and not updated when absent in a PUT. Changing this attribute requires an HTTPS connection. The default value is `\"\"`.")
  public String getTlsServerCertPassword() {
    return tlsServerCertPassword;
  }

  public void setTlsServerCertPassword(String tlsServerCertPassword) {
    this.tlsServerCertPassword = tlsServerCertPassword;
  }

  public Broker tlsTicketLifetime(Integer tlsTicketLifetime) {
    this.tlsTicketLifetime = tlsTicketLifetime;
    return this;
  }

   /**
   * The TLS ticket lifetime in seconds. When a client connects with TLS, a session with a session ticket is created using the TLS ticket lifetime which determines how long the client has to resume the session. The default value is &#x60;86400&#x60;.
   * @return tlsTicketLifetime
  **/
  @ApiModelProperty(value = "The TLS ticket lifetime in seconds. When a client connects with TLS, a session with a session ticket is created using the TLS ticket lifetime which determines how long the client has to resume the session. The default value is `86400`.")
  public Integer getTlsTicketLifetime() {
    return tlsTicketLifetime;
  }

  public void setTlsTicketLifetime(Integer tlsTicketLifetime) {
    this.tlsTicketLifetime = tlsTicketLifetime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Broker broker = (Broker) o;
    return Objects.equals(this.authClientCertRevocationCheckMode, broker.authClientCertRevocationCheckMode) &&
        Objects.equals(this.tlsBlockVersion10Enabled, broker.tlsBlockVersion10Enabled) &&
        Objects.equals(this.tlsBlockVersion11Enabled, broker.tlsBlockVersion11Enabled) &&
        Objects.equals(this.tlsCipherSuiteManagementList, broker.tlsCipherSuiteManagementList) &&
        Objects.equals(this.tlsCipherSuiteMsgBackboneList, broker.tlsCipherSuiteMsgBackboneList) &&
        Objects.equals(this.tlsCipherSuiteSecureShellList, broker.tlsCipherSuiteSecureShellList) &&
        Objects.equals(this.tlsCrimeExploitProtectionEnabled, broker.tlsCrimeExploitProtectionEnabled) &&
        Objects.equals(this.tlsServerCertContent, broker.tlsServerCertContent) &&
        Objects.equals(this.tlsServerCertPassword, broker.tlsServerCertPassword) &&
        Objects.equals(this.tlsTicketLifetime, broker.tlsTicketLifetime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authClientCertRevocationCheckMode, tlsBlockVersion10Enabled, tlsBlockVersion11Enabled, tlsCipherSuiteManagementList, tlsCipherSuiteMsgBackboneList, tlsCipherSuiteSecureShellList, tlsCrimeExploitProtectionEnabled, tlsServerCertContent, tlsServerCertPassword, tlsTicketLifetime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Broker {\n");
    
    sb.append("    authClientCertRevocationCheckMode: ").append(toIndentedString(authClientCertRevocationCheckMode)).append("\n");
    sb.append("    tlsBlockVersion10Enabled: ").append(toIndentedString(tlsBlockVersion10Enabled)).append("\n");
    sb.append("    tlsBlockVersion11Enabled: ").append(toIndentedString(tlsBlockVersion11Enabled)).append("\n");
    sb.append("    tlsCipherSuiteManagementList: ").append(toIndentedString(tlsCipherSuiteManagementList)).append("\n");
    sb.append("    tlsCipherSuiteMsgBackboneList: ").append(toIndentedString(tlsCipherSuiteMsgBackboneList)).append("\n");
    sb.append("    tlsCipherSuiteSecureShellList: ").append(toIndentedString(tlsCipherSuiteSecureShellList)).append("\n");
    sb.append("    tlsCrimeExploitProtectionEnabled: ").append(toIndentedString(tlsCrimeExploitProtectionEnabled)).append("\n");
    sb.append("    tlsServerCertContent: ").append(toIndentedString(tlsServerCertContent)).append("\n");
    sb.append("    tlsServerCertPassword: ").append(toIndentedString(tlsServerCertPassword)).append("\n");
    sb.append("    tlsTicketLifetime: ").append(toIndentedString(tlsTicketLifetime)).append("\n");
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

