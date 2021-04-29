/*
 * SEMP (Solace Element Management Protocol)
 * SEMP (starting in `v2`, see note 1) is a RESTful API for configuring, monitoring, and administering a Solace PubSub+ broker.  SEMP uses URIs to address manageable **resources** of the Solace PubSub+ broker. Resources are individual **objects**, **collections** of objects, or (exclusively in the action API) **actions**. This document applies to the following API:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Configuration|/SEMP/v2/config|Reading and writing config state|See note 2    The following APIs are also available:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Action|/SEMP/v2/action|Performing actions|See note 2 Monitoring|/SEMP/v2/monitor|Querying operational parameters|See note 2    Resources are always nouns, with individual objects being singular and collections being plural.  Objects within a collection are identified by an `obj-id`, which follows the collection name with the form `collection-name/obj-id`.  Actions within an object are identified by an `action-id`, which follows the object name with the form `obj-id/action-id`.  Some examples:  ``` /SEMP/v2/config/msgVpns                        ; MsgVpn collection /SEMP/v2/config/msgVpns/a                      ; MsgVpn object named \"a\" /SEMP/v2/config/msgVpns/a/queues               ; Queue collection in MsgVpn \"a\" /SEMP/v2/config/msgVpns/a/queues/b             ; Queue object named \"b\" in MsgVpn \"a\" /SEMP/v2/action/msgVpns/a/queues/b/startReplay ; Action that starts a replay on Queue \"b\" in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients             ; Client collection in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients/c           ; Client object named \"c\" in MsgVpn \"a\" ```  ## Collection Resources  Collections are unordered lists of objects (unless described as otherwise), and are described by JSON arrays. Each item in the array represents an object in the same manner as the individual object would normally be represented. In the configuration API, the creation of a new object is done through its collection resource.  ## Object and Action Resources  Objects are composed of attributes, actions, collections, and other objects. They are described by JSON objects as name/value pairs. The collections and actions of an object are not contained directly in the object's JSON content; rather the content includes an attribute containing a URI which points to the collections and actions. These contained resources must be managed through this URI. At a minimum, every object has one or more identifying attributes, and its own `uri` attribute which contains the URI pointing to itself.  Actions are also composed of attributes, and are described by JSON objects as name/value pairs. Unlike objects, however, they are not members of a collection and cannot be retrieved, only performed. Actions only exist in the action API.  Attributes in an object or action may have any combination of the following properties:   Property|Meaning|Comments :---|:---|:--- Identifying|Attribute is involved in unique identification of the object, and appears in its URI| Required|Attribute must be provided in the request| Read-Only|Attribute can only be read, not written.|See note 3 Write-Only|Attribute can only be written, not read, unless the attribute is also opaque|See the documentation for the opaque property Requires-Disable|Attribute can only be changed when object is disabled| Deprecated|Attribute is deprecated, and will disappear in the next SEMP version| Opaque|Attribute can be set or retrieved in opaque form when the `opaquePassword` query parameter is present|See the `opaquePassword` query parameter documentation    In some requests, certain attributes may only be provided in certain combinations with other attributes:   Relationship|Meaning :---|:--- Requires|Attribute may only be changed by a request if a particular attribute or combination of attributes is also provided in the request Conflicts|Attribute may only be provided in a request if a particular attribute or combination of attributes is not also provided in the request    In the monitoring API, any non-identifying attribute may not be returned in a GET.  ## HTTP Methods  The following HTTP methods manipulate resources in accordance with these general principles. Note that some methods are only used in certain APIs:   Method|Resource|Meaning|Request Body|Response Body|Missing Request Attributes :---|:---|:---|:---|:---|:--- POST|Collection|Create object|Initial attribute values|Object attributes and metadata|Set to default PUT|Object|Create or replace object (see note 5)|New attribute values|Object attributes and metadata|Set to default, with certain exceptions (see note 4) PUT|Action|Performs action|Action arguments|Action metadata|N/A PATCH|Object|Update object|New attribute values|Object attributes and metadata|unchanged DELETE|Object|Delete object|Empty|Object metadata|N/A GET|Object|Get object|Empty|Object attributes and metadata|N/A GET|Collection|Get collection|Empty|Object attributes and collection metadata|N/A    ## Common Query Parameters  The following are some common query parameters that are supported by many method/URI combinations. Individual URIs may document additional parameters. Note that multiple query parameters can be used together in a single URI, separated by the ampersand character. For example:  ``` ; Request for the MsgVpns collection using two hypothetical query parameters ; \"q1\" and \"q2\" with values \"val1\" and \"val2\" respectively /SEMP/v2/config/msgVpns?q1=val1&q2=val2 ```  ### select  Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. Use this query parameter to limit the size of the returned data for each returned object, return only those fields that are desired, or exclude fields that are not desired.  The value of `select` is a comma-separated list of attribute names. If the list contains attribute names that are not prefaced by `-`, only those attributes are included in the response. If the list contains attribute names that are prefaced by `-`, those attributes are excluded from the response. If the list contains both types, then the difference of the first set of attributes and the second set of attributes is returned. If the list is empty (i.e. `select=`), no attributes are returned.  All attributes that are prefaced by `-` must follow all attributes that are not prefaced by `-`. In addition, each attribute name in the list must match at least one attribute in the object.  Names may include the `*` wildcard (zero or more characters). Nested attribute names are supported using periods (e.g. `parentName.childName`).  Some examples:  ``` ; List of all MsgVpn names /SEMP/v2/config/msgVpns?select=msgVpnName ; List of all MsgVpn and their attributes except for their names /SEMP/v2/config/msgVpns?select=-msgVpnName ; Authentication attributes of MsgVpn \"finance\" /SEMP/v2/config/msgVpns/finance?select=authentication* ; All attributes of MsgVpn \"finance\" except for authentication attributes /SEMP/v2/config/msgVpns/finance?select=-authentication* ; Access related attributes of Queue \"orderQ\" of MsgVpn \"finance\" /SEMP/v2/config/msgVpns/finance/queues/orderQ?select=owner,permission ```  ### where  Include in the response only objects where certain conditions are true. Use this query parameter to limit which objects are returned to those whose attribute values meet the given conditions.  The value of `where` is a comma-separated list of expressions. All expressions must be true for the object to be included in the response. Each expression takes the form:  ``` expression  = attribute-name OP value OP          = '==' | '!=' | '&lt;' | '&gt;' | '&lt;=' | '&gt;=' ```  `value` may be a number, string, `true`, or `false`, as appropriate for the type of `attribute-name`. Greater-than and less-than comparisons only work for numbers. A `*` in a string `value` is interpreted as a wildcard (zero or more characters). Some examples:  ``` ; Only enabled MsgVpns /SEMP/v2/config/msgVpns?where=enabled==true ; Only MsgVpns using basic non-LDAP authentication /SEMP/v2/config/msgVpns?where=authenticationBasicEnabled==true,authenticationBasicType!=ldap ; Only MsgVpns that allow more than 100 client connections /SEMP/v2/config/msgVpns?where=maxConnectionCount>100 ; Only MsgVpns with msgVpnName starting with \"B\": /SEMP/v2/config/msgVpns?where=msgVpnName==B* ```  ### count  Limit the count of objects in the response. This can be useful to limit the size of the response for large collections. The minimum value for `count` is `1` and the default is `10`. There is also a per-collection maximum value to limit request handling time. For example:  ``` ; Up to 25 MsgVpns /SEMP/v2/config/msgVpns?count=25 ```  ### cursor  The cursor, or position, for the next page of objects. Cursors are opaque data that should not be created or interpreted by SEMP clients, and should only be used as described below.  When a request is made for a collection and there may be additional objects available for retrieval that are not included in the initial response, the response will include a `cursorQuery` field containing a cursor. The value of this field can be specified in the `cursor` query parameter of a subsequent request to retrieve the next page of objects. For convenience, an appropriate URI is constructed automatically by the broker and included in the `nextPageUri` field of the response. This URI can be used directly to retrieve the next page of objects.  ### opaquePassword  Attributes with the opaque property are also write-only and so cannot normally be retrieved in a GET. However, when a password is provided in the `opaquePassword` query parameter, attributes with the opaque property are retrieved in a GET in opaque form, encrypted with this password. The query parameter can also be used on a POST, PATCH, or PUT to set opaque attributes using opaque attribute values retrieved in a GET, so long as:  1. the same password that was used to retrieve the opaque attribute values is provided; and  2. the broker to which the request is being sent has the same major and minor SEMP version as the broker that produced the opaque attribute values.  The password provided in the query parameter must be a minimum of 8 characters and a maximum of 128 characters.  The query parameter can only be used in the configuration API, and only over HTTPS.  ## Help  Visit [our website](https://solace.com) to learn more about Solace.  You can also download the SEMP API specifications by clicking [here](https://solace.com/downloads/).  If you need additional support, please contact us at [support@solace.com](mailto:support@solace.com).  ## Notes  Note|Description :---:|:--- 1|This specification defines SEMP starting in \"v2\", and not the original SEMP \"v1\" interface. Request and response formats between \"v1\" and \"v2\" are entirely incompatible, although both protocols share a common port configuration on the Solace PubSub+ broker. They are differentiated by the initial portion of the URI path, one of either \"/SEMP/\" or \"/SEMP/v2/\" 2|This API is partially implemented. Only a subset of all objects are available. 3|Read-only attributes may appear in POST and PUT/PATCH requests. However, if a read-only attribute is not marked as identifying, it will be ignored during a PUT/PATCH. 4|On a PUT, if the SEMP user is not authorized to modify the attribute, its value is left unchanged rather than set to default. In addition, the values of write-only attributes are not set to their defaults on a PUT, except in the following two cases: there is a mutual requires relationship with another non-write-only attribute and both attributes are absent from the request; or the attribute is also opaque and the `opaquePassword` query parameter is provided in the request. 5|On a PUT, if the object does not exist, it is created first.  
 *
 * OpenAPI spec version: 2.17
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
import com.solace.psg.sempv2.config.model.EventThreshold;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * Broker
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:49:16.603913+01:00[Europe/London]")
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
        Object value = jsonReader.nextString();
        return AuthClientCertRevocationCheckModeEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("authClientCertRevocationCheckMode")
  private AuthClientCertRevocationCheckModeEnum authClientCertRevocationCheckMode = null;

  @SerializedName("serviceAmqpEnabled")
  private Boolean serviceAmqpEnabled = null;

  @SerializedName("serviceAmqpTlsListenPort")
  private Long serviceAmqpTlsListenPort = null;

  @SerializedName("serviceEventConnectionCountThreshold")
  private EventThreshold serviceEventConnectionCountThreshold = null;

  @SerializedName("serviceHealthCheckEnabled")
  private Boolean serviceHealthCheckEnabled = null;

  @SerializedName("serviceHealthCheckListenPort")
  private Long serviceHealthCheckListenPort = null;

  @SerializedName("serviceMateLinkEnabled")
  private Boolean serviceMateLinkEnabled = null;

  @SerializedName("serviceMateLinkListenPort")
  private Long serviceMateLinkListenPort = null;

  @SerializedName("serviceMqttEnabled")
  private Boolean serviceMqttEnabled = null;

  @SerializedName("serviceMsgBackboneEnabled")
  private Boolean serviceMsgBackboneEnabled = null;

  @SerializedName("serviceRedundancyEnabled")
  private Boolean serviceRedundancyEnabled = null;

  @SerializedName("serviceRedundancyFirstListenPort")
  private Long serviceRedundancyFirstListenPort = null;

  @SerializedName("serviceRestEventOutgoingConnectionCountThreshold")
  private EventThreshold serviceRestEventOutgoingConnectionCountThreshold = null;

  @SerializedName("serviceRestIncomingEnabled")
  private Boolean serviceRestIncomingEnabled = null;

  @SerializedName("serviceRestOutgoingEnabled")
  private Boolean serviceRestOutgoingEnabled = null;

  @SerializedName("serviceSempPlainTextEnabled")
  private Boolean serviceSempPlainTextEnabled = null;

  @SerializedName("serviceSempPlainTextListenPort")
  private Long serviceSempPlainTextListenPort = null;

  @SerializedName("serviceSempTlsEnabled")
  private Boolean serviceSempTlsEnabled = null;

  @SerializedName("serviceSempTlsListenPort")
  private Long serviceSempTlsListenPort = null;

  @SerializedName("serviceSmfCompressionListenPort")
  private Long serviceSmfCompressionListenPort = null;

  @SerializedName("serviceSmfEnabled")
  private Boolean serviceSmfEnabled = null;

  @SerializedName("serviceSmfEventConnectionCountThreshold")
  private EventThreshold serviceSmfEventConnectionCountThreshold = null;

  @SerializedName("serviceSmfPlainTextListenPort")
  private Long serviceSmfPlainTextListenPort = null;

  @SerializedName("serviceSmfRoutingControlListenPort")
  private Long serviceSmfRoutingControlListenPort = null;

  @SerializedName("serviceSmfTlsListenPort")
  private Long serviceSmfTlsListenPort = null;

  @SerializedName("serviceTlsEventConnectionCountThreshold")
  private EventThreshold serviceTlsEventConnectionCountThreshold = null;

  @SerializedName("serviceWebTransportEnabled")
  private Boolean serviceWebTransportEnabled = null;

  @SerializedName("serviceWebTransportPlainTextListenPort")
  private Long serviceWebTransportPlainTextListenPort = null;

  @SerializedName("serviceWebTransportTlsListenPort")
  private Long serviceWebTransportTlsListenPort = null;

  @SerializedName("serviceWebTransportWebUrlSuffix")
  private String serviceWebTransportWebUrlSuffix = null;

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
  @Schema(description = "The client certificate revocation checking mode used when a client authenticates with a client certificate. The default value is `\"none\"`. The allowed values and their meaning are:  <pre> \"none\" - Do not perform any certificate revocation checking. \"ocsp\" - Use the Open Certificate Status Protcol (OCSP) for certificate revocation checking. \"crl\" - Use Certificate Revocation Lists (CRL) for certificate revocation checking. \"ocsp-crl\" - Use OCSP first, but if OCSP fails to return an unambiguous result, then check via CRL. </pre> ")
  public AuthClientCertRevocationCheckModeEnum getAuthClientCertRevocationCheckMode() {
    return authClientCertRevocationCheckMode;
  }

  public void setAuthClientCertRevocationCheckMode(AuthClientCertRevocationCheckModeEnum authClientCertRevocationCheckMode) {
    this.authClientCertRevocationCheckMode = authClientCertRevocationCheckMode;
  }

  public Broker serviceAmqpEnabled(Boolean serviceAmqpEnabled) {
    this.serviceAmqpEnabled = serviceAmqpEnabled;
    return this;
  }

   /**
   * Enable or disable the AMQP service. When disabled new AMQP Clients may not connect through the global or per-VPN AMQP listen-ports, and all currently connected AMQP Clients are immediately disconnected. The default value is &#x60;false&#x60;. Available since 2.17.
   * @return serviceAmqpEnabled
  **/
  @Schema(description = "Enable or disable the AMQP service. When disabled new AMQP Clients may not connect through the global or per-VPN AMQP listen-ports, and all currently connected AMQP Clients are immediately disconnected. The default value is `false`. Available since 2.17.")
  public Boolean isServiceAmqpEnabled() {
    return serviceAmqpEnabled;
  }

  public void setServiceAmqpEnabled(Boolean serviceAmqpEnabled) {
    this.serviceAmqpEnabled = serviceAmqpEnabled;
  }

  public Broker serviceAmqpTlsListenPort(Long serviceAmqpTlsListenPort) {
    this.serviceAmqpTlsListenPort = serviceAmqpTlsListenPort;
    return this;
  }

   /**
   * TCP port number that AMQP clients can use to connect to the broker using raw TCP over TLS. The default value is &#x60;0&#x60;. Available since 2.17.
   * @return serviceAmqpTlsListenPort
  **/
  @Schema(description = "TCP port number that AMQP clients can use to connect to the broker using raw TCP over TLS. The default value is `0`. Available since 2.17.")
  public Long getServiceAmqpTlsListenPort() {
    return serviceAmqpTlsListenPort;
  }

  public void setServiceAmqpTlsListenPort(Long serviceAmqpTlsListenPort) {
    this.serviceAmqpTlsListenPort = serviceAmqpTlsListenPort;
  }

  public Broker serviceEventConnectionCountThreshold(EventThreshold serviceEventConnectionCountThreshold) {
    this.serviceEventConnectionCountThreshold = serviceEventConnectionCountThreshold;
    return this;
  }

   /**
   * Get serviceEventConnectionCountThreshold
   * @return serviceEventConnectionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getServiceEventConnectionCountThreshold() {
    return serviceEventConnectionCountThreshold;
  }

  public void setServiceEventConnectionCountThreshold(EventThreshold serviceEventConnectionCountThreshold) {
    this.serviceEventConnectionCountThreshold = serviceEventConnectionCountThreshold;
  }

  public Broker serviceHealthCheckEnabled(Boolean serviceHealthCheckEnabled) {
    this.serviceHealthCheckEnabled = serviceHealthCheckEnabled;
    return this;
  }

   /**
   * Enable or disable the health-check service. The default value is &#x60;false&#x60;. Available since 2.17.
   * @return serviceHealthCheckEnabled
  **/
  @Schema(description = "Enable or disable the health-check service. The default value is `false`. Available since 2.17.")
  public Boolean isServiceHealthCheckEnabled() {
    return serviceHealthCheckEnabled;
  }

  public void setServiceHealthCheckEnabled(Boolean serviceHealthCheckEnabled) {
    this.serviceHealthCheckEnabled = serviceHealthCheckEnabled;
  }

  public Broker serviceHealthCheckListenPort(Long serviceHealthCheckListenPort) {
    this.serviceHealthCheckListenPort = serviceHealthCheckListenPort;
    return this;
  }

   /**
   * The port number for the health-check service. The port must be unique across the message backbone. The health-check service must be disabled to change the port. The default value is &#x60;5550&#x60;. Available since 2.17.
   * @return serviceHealthCheckListenPort
  **/
  @Schema(description = "The port number for the health-check service. The port must be unique across the message backbone. The health-check service must be disabled to change the port. The default value is `5550`. Available since 2.17.")
  public Long getServiceHealthCheckListenPort() {
    return serviceHealthCheckListenPort;
  }

  public void setServiceHealthCheckListenPort(Long serviceHealthCheckListenPort) {
    this.serviceHealthCheckListenPort = serviceHealthCheckListenPort;
  }

  public Broker serviceMateLinkEnabled(Boolean serviceMateLinkEnabled) {
    this.serviceMateLinkEnabled = serviceMateLinkEnabled;
    return this;
  }

   /**
   * Enable or disable the mate-link service. The default value is &#x60;true&#x60;. Available since 2.17.
   * @return serviceMateLinkEnabled
  **/
  @Schema(description = "Enable or disable the mate-link service. The default value is `true`. Available since 2.17.")
  public Boolean isServiceMateLinkEnabled() {
    return serviceMateLinkEnabled;
  }

  public void setServiceMateLinkEnabled(Boolean serviceMateLinkEnabled) {
    this.serviceMateLinkEnabled = serviceMateLinkEnabled;
  }

  public Broker serviceMateLinkListenPort(Long serviceMateLinkListenPort) {
    this.serviceMateLinkListenPort = serviceMateLinkListenPort;
    return this;
  }

   /**
   * The port number for the mate-link service. The port must be unique across the message backbone. The mate-link service must be disabled to change the port. The default value is &#x60;8741&#x60;. Available since 2.17.
   * @return serviceMateLinkListenPort
  **/
  @Schema(description = "The port number for the mate-link service. The port must be unique across the message backbone. The mate-link service must be disabled to change the port. The default value is `8741`. Available since 2.17.")
  public Long getServiceMateLinkListenPort() {
    return serviceMateLinkListenPort;
  }

  public void setServiceMateLinkListenPort(Long serviceMateLinkListenPort) {
    this.serviceMateLinkListenPort = serviceMateLinkListenPort;
  }

  public Broker serviceMqttEnabled(Boolean serviceMqttEnabled) {
    this.serviceMqttEnabled = serviceMqttEnabled;
    return this;
  }

   /**
   * Enable or disable the MQTT service. When disabled new MQTT Clients may not connect through the per-VPN MQTT listen-ports, and all currently connected MQTT Clients are immediately disconnected. The default value is &#x60;false&#x60;. Available since 2.17.
   * @return serviceMqttEnabled
  **/
  @Schema(description = "Enable or disable the MQTT service. When disabled new MQTT Clients may not connect through the per-VPN MQTT listen-ports, and all currently connected MQTT Clients are immediately disconnected. The default value is `false`. Available since 2.17.")
  public Boolean isServiceMqttEnabled() {
    return serviceMqttEnabled;
  }

  public void setServiceMqttEnabled(Boolean serviceMqttEnabled) {
    this.serviceMqttEnabled = serviceMqttEnabled;
  }

  public Broker serviceMsgBackboneEnabled(Boolean serviceMsgBackboneEnabled) {
    this.serviceMsgBackboneEnabled = serviceMsgBackboneEnabled;
    return this;
  }

   /**
   * Enable or disable the msg-backbone service. When disabled new Clients may not connect through global or per-VPN listen-ports, and all currently connected Clients are immediately disconnected. The default value is &#x60;true&#x60;. Available since 2.17.
   * @return serviceMsgBackboneEnabled
  **/
  @Schema(description = "Enable or disable the msg-backbone service. When disabled new Clients may not connect through global or per-VPN listen-ports, and all currently connected Clients are immediately disconnected. The default value is `true`. Available since 2.17.")
  public Boolean isServiceMsgBackboneEnabled() {
    return serviceMsgBackboneEnabled;
  }

  public void setServiceMsgBackboneEnabled(Boolean serviceMsgBackboneEnabled) {
    this.serviceMsgBackboneEnabled = serviceMsgBackboneEnabled;
  }

  public Broker serviceRedundancyEnabled(Boolean serviceRedundancyEnabled) {
    this.serviceRedundancyEnabled = serviceRedundancyEnabled;
    return this;
  }

   /**
   * Enable or disable the redundancy service. The default value is &#x60;true&#x60;. Available since 2.17.
   * @return serviceRedundancyEnabled
  **/
  @Schema(description = "Enable or disable the redundancy service. The default value is `true`. Available since 2.17.")
  public Boolean isServiceRedundancyEnabled() {
    return serviceRedundancyEnabled;
  }

  public void setServiceRedundancyEnabled(Boolean serviceRedundancyEnabled) {
    this.serviceRedundancyEnabled = serviceRedundancyEnabled;
  }

  public Broker serviceRedundancyFirstListenPort(Long serviceRedundancyFirstListenPort) {
    this.serviceRedundancyFirstListenPort = serviceRedundancyFirstListenPort;
    return this;
  }

   /**
   * The first listen-port used for the redundancy service. Redundancy uses this port and the subsequent 2 ports. These port must be unique across the message backbone. The redundancy service must be disabled to change this port. The default value is &#x60;8300&#x60;. Available since 2.17.
   * @return serviceRedundancyFirstListenPort
  **/
  @Schema(description = "The first listen-port used for the redundancy service. Redundancy uses this port and the subsequent 2 ports. These port must be unique across the message backbone. The redundancy service must be disabled to change this port. The default value is `8300`. Available since 2.17.")
  public Long getServiceRedundancyFirstListenPort() {
    return serviceRedundancyFirstListenPort;
  }

  public void setServiceRedundancyFirstListenPort(Long serviceRedundancyFirstListenPort) {
    this.serviceRedundancyFirstListenPort = serviceRedundancyFirstListenPort;
  }

  public Broker serviceRestEventOutgoingConnectionCountThreshold(EventThreshold serviceRestEventOutgoingConnectionCountThreshold) {
    this.serviceRestEventOutgoingConnectionCountThreshold = serviceRestEventOutgoingConnectionCountThreshold;
    return this;
  }

   /**
   * Get serviceRestEventOutgoingConnectionCountThreshold
   * @return serviceRestEventOutgoingConnectionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getServiceRestEventOutgoingConnectionCountThreshold() {
    return serviceRestEventOutgoingConnectionCountThreshold;
  }

  public void setServiceRestEventOutgoingConnectionCountThreshold(EventThreshold serviceRestEventOutgoingConnectionCountThreshold) {
    this.serviceRestEventOutgoingConnectionCountThreshold = serviceRestEventOutgoingConnectionCountThreshold;
  }

  public Broker serviceRestIncomingEnabled(Boolean serviceRestIncomingEnabled) {
    this.serviceRestIncomingEnabled = serviceRestIncomingEnabled;
    return this;
  }

   /**
   * Enable or disable the REST service incoming connections on the router. The default value is &#x60;false&#x60;. Available since 2.17.
   * @return serviceRestIncomingEnabled
  **/
  @Schema(description = "Enable or disable the REST service incoming connections on the router. The default value is `false`. Available since 2.17.")
  public Boolean isServiceRestIncomingEnabled() {
    return serviceRestIncomingEnabled;
  }

  public void setServiceRestIncomingEnabled(Boolean serviceRestIncomingEnabled) {
    this.serviceRestIncomingEnabled = serviceRestIncomingEnabled;
  }

  public Broker serviceRestOutgoingEnabled(Boolean serviceRestOutgoingEnabled) {
    this.serviceRestOutgoingEnabled = serviceRestOutgoingEnabled;
    return this;
  }

   /**
   * Enable or disable the REST service outgoing connections on the router. The default value is &#x60;false&#x60;. Available since 2.17.
   * @return serviceRestOutgoingEnabled
  **/
  @Schema(description = "Enable or disable the REST service outgoing connections on the router. The default value is `false`. Available since 2.17.")
  public Boolean isServiceRestOutgoingEnabled() {
    return serviceRestOutgoingEnabled;
  }

  public void setServiceRestOutgoingEnabled(Boolean serviceRestOutgoingEnabled) {
    this.serviceRestOutgoingEnabled = serviceRestOutgoingEnabled;
  }

  public Broker serviceSempPlainTextEnabled(Boolean serviceSempPlainTextEnabled) {
    this.serviceSempPlainTextEnabled = serviceSempPlainTextEnabled;
    return this;
  }

   /**
   * Enable or disable plain-text SEMP service. The default value is &#x60;true&#x60;. Available since 2.17.
   * @return serviceSempPlainTextEnabled
  **/
  @Schema(description = "Enable or disable plain-text SEMP service. The default value is `true`. Available since 2.17.")
  public Boolean isServiceSempPlainTextEnabled() {
    return serviceSempPlainTextEnabled;
  }

  public void setServiceSempPlainTextEnabled(Boolean serviceSempPlainTextEnabled) {
    this.serviceSempPlainTextEnabled = serviceSempPlainTextEnabled;
  }

  public Broker serviceSempPlainTextListenPort(Long serviceSempPlainTextListenPort) {
    this.serviceSempPlainTextListenPort = serviceSempPlainTextListenPort;
    return this;
  }

   /**
   * The TCP port for plain-text SEMP client connections. The default value is &#x60;80&#x60;. Available since 2.17.
   * @return serviceSempPlainTextListenPort
  **/
  @Schema(description = "The TCP port for plain-text SEMP client connections. The default value is `80`. Available since 2.17.")
  public Long getServiceSempPlainTextListenPort() {
    return serviceSempPlainTextListenPort;
  }

  public void setServiceSempPlainTextListenPort(Long serviceSempPlainTextListenPort) {
    this.serviceSempPlainTextListenPort = serviceSempPlainTextListenPort;
  }

  public Broker serviceSempTlsEnabled(Boolean serviceSempTlsEnabled) {
    this.serviceSempTlsEnabled = serviceSempTlsEnabled;
    return this;
  }

   /**
   * Enable or disable TLS SEMP service. The default value is &#x60;true&#x60;. Available since 2.17.
   * @return serviceSempTlsEnabled
  **/
  @Schema(description = "Enable or disable TLS SEMP service. The default value is `true`. Available since 2.17.")
  public Boolean isServiceSempTlsEnabled() {
    return serviceSempTlsEnabled;
  }

  public void setServiceSempTlsEnabled(Boolean serviceSempTlsEnabled) {
    this.serviceSempTlsEnabled = serviceSempTlsEnabled;
  }

  public Broker serviceSempTlsListenPort(Long serviceSempTlsListenPort) {
    this.serviceSempTlsListenPort = serviceSempTlsListenPort;
    return this;
  }

   /**
   * The TCP port for TLS SEMP client connections. The default value is &#x60;1943&#x60;. Available since 2.17.
   * @return serviceSempTlsListenPort
  **/
  @Schema(description = "The TCP port for TLS SEMP client connections. The default value is `1943`. Available since 2.17.")
  public Long getServiceSempTlsListenPort() {
    return serviceSempTlsListenPort;
  }

  public void setServiceSempTlsListenPort(Long serviceSempTlsListenPort) {
    this.serviceSempTlsListenPort = serviceSempTlsListenPort;
  }

  public Broker serviceSmfCompressionListenPort(Long serviceSmfCompressionListenPort) {
    this.serviceSmfCompressionListenPort = serviceSmfCompressionListenPort;
    return this;
  }

   /**
   * TCP port number that SMF clients can use to connect to the broker using raw compression TCP. The default value is &#x60;55003&#x60;. Available since 2.17.
   * @return serviceSmfCompressionListenPort
  **/
  @Schema(description = "TCP port number that SMF clients can use to connect to the broker using raw compression TCP. The default value is `55003`. Available since 2.17.")
  public Long getServiceSmfCompressionListenPort() {
    return serviceSmfCompressionListenPort;
  }

  public void setServiceSmfCompressionListenPort(Long serviceSmfCompressionListenPort) {
    this.serviceSmfCompressionListenPort = serviceSmfCompressionListenPort;
  }

  public Broker serviceSmfEnabled(Boolean serviceSmfEnabled) {
    this.serviceSmfEnabled = serviceSmfEnabled;
    return this;
  }

   /**
   * Enable or disable the SMF service. When disabled new SMF Clients may not connect through the global listen-ports, and all currently connected SMF Clients are immediately disconnected. The default value is &#x60;true&#x60;. Available since 2.17.
   * @return serviceSmfEnabled
  **/
  @Schema(description = "Enable or disable the SMF service. When disabled new SMF Clients may not connect through the global listen-ports, and all currently connected SMF Clients are immediately disconnected. The default value is `true`. Available since 2.17.")
  public Boolean isServiceSmfEnabled() {
    return serviceSmfEnabled;
  }

  public void setServiceSmfEnabled(Boolean serviceSmfEnabled) {
    this.serviceSmfEnabled = serviceSmfEnabled;
  }

  public Broker serviceSmfEventConnectionCountThreshold(EventThreshold serviceSmfEventConnectionCountThreshold) {
    this.serviceSmfEventConnectionCountThreshold = serviceSmfEventConnectionCountThreshold;
    return this;
  }

   /**
   * Get serviceSmfEventConnectionCountThreshold
   * @return serviceSmfEventConnectionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getServiceSmfEventConnectionCountThreshold() {
    return serviceSmfEventConnectionCountThreshold;
  }

  public void setServiceSmfEventConnectionCountThreshold(EventThreshold serviceSmfEventConnectionCountThreshold) {
    this.serviceSmfEventConnectionCountThreshold = serviceSmfEventConnectionCountThreshold;
  }

  public Broker serviceSmfPlainTextListenPort(Long serviceSmfPlainTextListenPort) {
    this.serviceSmfPlainTextListenPort = serviceSmfPlainTextListenPort;
    return this;
  }

   /**
   * TCP port number that SMF clients can use to connect to the broker using raw TCP. The default value is &#x60;55555&#x60;. Available since 2.17.
   * @return serviceSmfPlainTextListenPort
  **/
  @Schema(description = "TCP port number that SMF clients can use to connect to the broker using raw TCP. The default value is `55555`. Available since 2.17.")
  public Long getServiceSmfPlainTextListenPort() {
    return serviceSmfPlainTextListenPort;
  }

  public void setServiceSmfPlainTextListenPort(Long serviceSmfPlainTextListenPort) {
    this.serviceSmfPlainTextListenPort = serviceSmfPlainTextListenPort;
  }

  public Broker serviceSmfRoutingControlListenPort(Long serviceSmfRoutingControlListenPort) {
    this.serviceSmfRoutingControlListenPort = serviceSmfRoutingControlListenPort;
    return this;
  }

   /**
   * TCP port number that SMF clients can use to connect to the broker using raw routing control TCP. The default value is &#x60;55556&#x60;. Available since 2.17.
   * @return serviceSmfRoutingControlListenPort
  **/
  @Schema(description = "TCP port number that SMF clients can use to connect to the broker using raw routing control TCP. The default value is `55556`. Available since 2.17.")
  public Long getServiceSmfRoutingControlListenPort() {
    return serviceSmfRoutingControlListenPort;
  }

  public void setServiceSmfRoutingControlListenPort(Long serviceSmfRoutingControlListenPort) {
    this.serviceSmfRoutingControlListenPort = serviceSmfRoutingControlListenPort;
  }

  public Broker serviceSmfTlsListenPort(Long serviceSmfTlsListenPort) {
    this.serviceSmfTlsListenPort = serviceSmfTlsListenPort;
    return this;
  }

   /**
   * TCP port number that SMF clients can use to connect to the broker using raw TCP over TLS. The default value is &#x60;55443&#x60;. Available since 2.17.
   * @return serviceSmfTlsListenPort
  **/
  @Schema(description = "TCP port number that SMF clients can use to connect to the broker using raw TCP over TLS. The default value is `55443`. Available since 2.17.")
  public Long getServiceSmfTlsListenPort() {
    return serviceSmfTlsListenPort;
  }

  public void setServiceSmfTlsListenPort(Long serviceSmfTlsListenPort) {
    this.serviceSmfTlsListenPort = serviceSmfTlsListenPort;
  }

  public Broker serviceTlsEventConnectionCountThreshold(EventThreshold serviceTlsEventConnectionCountThreshold) {
    this.serviceTlsEventConnectionCountThreshold = serviceTlsEventConnectionCountThreshold;
    return this;
  }

   /**
   * Get serviceTlsEventConnectionCountThreshold
   * @return serviceTlsEventConnectionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getServiceTlsEventConnectionCountThreshold() {
    return serviceTlsEventConnectionCountThreshold;
  }

  public void setServiceTlsEventConnectionCountThreshold(EventThreshold serviceTlsEventConnectionCountThreshold) {
    this.serviceTlsEventConnectionCountThreshold = serviceTlsEventConnectionCountThreshold;
  }

  public Broker serviceWebTransportEnabled(Boolean serviceWebTransportEnabled) {
    this.serviceWebTransportEnabled = serviceWebTransportEnabled;
    return this;
  }

   /**
   * Enable or disable the web-transport service. When disabled new web-transport Clients may not connect through the global listen-ports, and all currently connected web-transport Clients are immediately disconnected. The default value is &#x60;false&#x60;. Available since 2.17.
   * @return serviceWebTransportEnabled
  **/
  @Schema(description = "Enable or disable the web-transport service. When disabled new web-transport Clients may not connect through the global listen-ports, and all currently connected web-transport Clients are immediately disconnected. The default value is `false`. Available since 2.17.")
  public Boolean isServiceWebTransportEnabled() {
    return serviceWebTransportEnabled;
  }

  public void setServiceWebTransportEnabled(Boolean serviceWebTransportEnabled) {
    this.serviceWebTransportEnabled = serviceWebTransportEnabled;
  }

  public Broker serviceWebTransportPlainTextListenPort(Long serviceWebTransportPlainTextListenPort) {
    this.serviceWebTransportPlainTextListenPort = serviceWebTransportPlainTextListenPort;
    return this;
  }

   /**
   * The TCP port for plain-text WEB client connections. The default value is &#x60;8008&#x60;. Available since 2.17.
   * @return serviceWebTransportPlainTextListenPort
  **/
  @Schema(description = "The TCP port for plain-text WEB client connections. The default value is `8008`. Available since 2.17.")
  public Long getServiceWebTransportPlainTextListenPort() {
    return serviceWebTransportPlainTextListenPort;
  }

  public void setServiceWebTransportPlainTextListenPort(Long serviceWebTransportPlainTextListenPort) {
    this.serviceWebTransportPlainTextListenPort = serviceWebTransportPlainTextListenPort;
  }

  public Broker serviceWebTransportTlsListenPort(Long serviceWebTransportTlsListenPort) {
    this.serviceWebTransportTlsListenPort = serviceWebTransportTlsListenPort;
    return this;
  }

   /**
   * The TCP port for TLS WEB client connections. The default value is &#x60;1443&#x60;. Available since 2.17.
   * @return serviceWebTransportTlsListenPort
  **/
  @Schema(description = "The TCP port for TLS WEB client connections. The default value is `1443`. Available since 2.17.")
  public Long getServiceWebTransportTlsListenPort() {
    return serviceWebTransportTlsListenPort;
  }

  public void setServiceWebTransportTlsListenPort(Long serviceWebTransportTlsListenPort) {
    this.serviceWebTransportTlsListenPort = serviceWebTransportTlsListenPort;
  }

  public Broker serviceWebTransportWebUrlSuffix(String serviceWebTransportWebUrlSuffix) {
    this.serviceWebTransportWebUrlSuffix = serviceWebTransportWebUrlSuffix;
    return this;
  }

   /**
   * Used to specify the Web URL suffix that will be used by Web clients when communicating with the broker. The default value is &#x60;\&quot;\&quot;&#x60;. Available since 2.17.
   * @return serviceWebTransportWebUrlSuffix
  **/
  @Schema(description = "Used to specify the Web URL suffix that will be used by Web clients when communicating with the broker. The default value is `\"\"`. Available since 2.17.")
  public String getServiceWebTransportWebUrlSuffix() {
    return serviceWebTransportWebUrlSuffix;
  }

  public void setServiceWebTransportWebUrlSuffix(String serviceWebTransportWebUrlSuffix) {
    this.serviceWebTransportWebUrlSuffix = serviceWebTransportWebUrlSuffix;
  }

  public Broker tlsBlockVersion11Enabled(Boolean tlsBlockVersion11Enabled) {
    this.tlsBlockVersion11Enabled = tlsBlockVersion11Enabled;
    return this;
  }

   /**
   * Enable or disable the blocking of TLS version 1.1 connections. When blocked, all existing incoming and outgoing TLS 1.1 connections with Clients, SEMP users, and LDAP servers remain connected while new connections are blocked. Note that support for TLS 1.1 will eventually be discontinued, at which time TLS 1.1 connections will be blocked regardless of this setting. The default value is &#x60;false&#x60;.
   * @return tlsBlockVersion11Enabled
  **/
  @Schema(description = "Enable or disable the blocking of TLS version 1.1 connections. When blocked, all existing incoming and outgoing TLS 1.1 connections with Clients, SEMP users, and LDAP servers remain connected while new connections are blocked. Note that support for TLS 1.1 will eventually be discontinued, at which time TLS 1.1 connections will be blocked regardless of this setting. The default value is `false`.")
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
  @Schema(description = "The colon-separated list of cipher suites used for TLS management connections (e.g. SEMP, LDAP). The value \"default\" implies all supported suites ordered from most secure to least secure. The default value is `\"default\"`.")
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
  @Schema(description = "The colon-separated list of cipher suites used for TLS data connections (e.g. client pub/sub). The value \"default\" implies all supported suites ordered from most secure to least secure. The default value is `\"default\"`.")
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
  @Schema(description = "The colon-separated list of cipher suites used for TLS secure shell connections (e.g. SSH, SFTP, SCP). The value \"default\" implies all supported suites ordered from most secure to least secure. The default value is `\"default\"`.")
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
  @Schema(description = "Enable or disable protection against the CRIME exploit. When enabled, TLS+compressed messaging performance is degraded. This protection should only be disabled if sufficient ACL and authentication features are being employed such that a potential attacker does not have sufficient access to trigger the exploit. The default value is `true`.")
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
   * The PEM formatted content for the server certificate used for TLS connections. It must consist of a private key and between one and three certificates comprising the certificate trust chain. This attribute is absent from a GET and not updated when absent in a PUT, subject to the exceptions in note 4. Changing this attribute requires an HTTPS connection. The default value is &#x60;\&quot;\&quot;&#x60;.
   * @return tlsServerCertContent
  **/
  @Schema(description = "The PEM formatted content for the server certificate used for TLS connections. It must consist of a private key and between one and three certificates comprising the certificate trust chain. This attribute is absent from a GET and not updated when absent in a PUT, subject to the exceptions in note 4. Changing this attribute requires an HTTPS connection. The default value is `\"\"`.")
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
   * The password for the server certificate used for TLS connections. This attribute is absent from a GET and not updated when absent in a PUT, subject to the exceptions in note 4. Changing this attribute requires an HTTPS connection. The default value is &#x60;\&quot;\&quot;&#x60;.
   * @return tlsServerCertPassword
  **/
  @Schema(description = "The password for the server certificate used for TLS connections. This attribute is absent from a GET and not updated when absent in a PUT, subject to the exceptions in note 4. Changing this attribute requires an HTTPS connection. The default value is `\"\"`.")
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
  @Schema(description = "The TLS ticket lifetime in seconds. When a client connects with TLS, a session with a session ticket is created using the TLS ticket lifetime which determines how long the client has to resume the session. The default value is `86400`.")
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
        Objects.equals(this.serviceAmqpEnabled, broker.serviceAmqpEnabled) &&
        Objects.equals(this.serviceAmqpTlsListenPort, broker.serviceAmqpTlsListenPort) &&
        Objects.equals(this.serviceEventConnectionCountThreshold, broker.serviceEventConnectionCountThreshold) &&
        Objects.equals(this.serviceHealthCheckEnabled, broker.serviceHealthCheckEnabled) &&
        Objects.equals(this.serviceHealthCheckListenPort, broker.serviceHealthCheckListenPort) &&
        Objects.equals(this.serviceMateLinkEnabled, broker.serviceMateLinkEnabled) &&
        Objects.equals(this.serviceMateLinkListenPort, broker.serviceMateLinkListenPort) &&
        Objects.equals(this.serviceMqttEnabled, broker.serviceMqttEnabled) &&
        Objects.equals(this.serviceMsgBackboneEnabled, broker.serviceMsgBackboneEnabled) &&
        Objects.equals(this.serviceRedundancyEnabled, broker.serviceRedundancyEnabled) &&
        Objects.equals(this.serviceRedundancyFirstListenPort, broker.serviceRedundancyFirstListenPort) &&
        Objects.equals(this.serviceRestEventOutgoingConnectionCountThreshold, broker.serviceRestEventOutgoingConnectionCountThreshold) &&
        Objects.equals(this.serviceRestIncomingEnabled, broker.serviceRestIncomingEnabled) &&
        Objects.equals(this.serviceRestOutgoingEnabled, broker.serviceRestOutgoingEnabled) &&
        Objects.equals(this.serviceSempPlainTextEnabled, broker.serviceSempPlainTextEnabled) &&
        Objects.equals(this.serviceSempPlainTextListenPort, broker.serviceSempPlainTextListenPort) &&
        Objects.equals(this.serviceSempTlsEnabled, broker.serviceSempTlsEnabled) &&
        Objects.equals(this.serviceSempTlsListenPort, broker.serviceSempTlsListenPort) &&
        Objects.equals(this.serviceSmfCompressionListenPort, broker.serviceSmfCompressionListenPort) &&
        Objects.equals(this.serviceSmfEnabled, broker.serviceSmfEnabled) &&
        Objects.equals(this.serviceSmfEventConnectionCountThreshold, broker.serviceSmfEventConnectionCountThreshold) &&
        Objects.equals(this.serviceSmfPlainTextListenPort, broker.serviceSmfPlainTextListenPort) &&
        Objects.equals(this.serviceSmfRoutingControlListenPort, broker.serviceSmfRoutingControlListenPort) &&
        Objects.equals(this.serviceSmfTlsListenPort, broker.serviceSmfTlsListenPort) &&
        Objects.equals(this.serviceTlsEventConnectionCountThreshold, broker.serviceTlsEventConnectionCountThreshold) &&
        Objects.equals(this.serviceWebTransportEnabled, broker.serviceWebTransportEnabled) &&
        Objects.equals(this.serviceWebTransportPlainTextListenPort, broker.serviceWebTransportPlainTextListenPort) &&
        Objects.equals(this.serviceWebTransportTlsListenPort, broker.serviceWebTransportTlsListenPort) &&
        Objects.equals(this.serviceWebTransportWebUrlSuffix, broker.serviceWebTransportWebUrlSuffix) &&
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
    return Objects.hash(authClientCertRevocationCheckMode, serviceAmqpEnabled, serviceAmqpTlsListenPort, serviceEventConnectionCountThreshold, serviceHealthCheckEnabled, serviceHealthCheckListenPort, serviceMateLinkEnabled, serviceMateLinkListenPort, serviceMqttEnabled, serviceMsgBackboneEnabled, serviceRedundancyEnabled, serviceRedundancyFirstListenPort, serviceRestEventOutgoingConnectionCountThreshold, serviceRestIncomingEnabled, serviceRestOutgoingEnabled, serviceSempPlainTextEnabled, serviceSempPlainTextListenPort, serviceSempTlsEnabled, serviceSempTlsListenPort, serviceSmfCompressionListenPort, serviceSmfEnabled, serviceSmfEventConnectionCountThreshold, serviceSmfPlainTextListenPort, serviceSmfRoutingControlListenPort, serviceSmfTlsListenPort, serviceTlsEventConnectionCountThreshold, serviceWebTransportEnabled, serviceWebTransportPlainTextListenPort, serviceWebTransportTlsListenPort, serviceWebTransportWebUrlSuffix, tlsBlockVersion11Enabled, tlsCipherSuiteManagementList, tlsCipherSuiteMsgBackboneList, tlsCipherSuiteSecureShellList, tlsCrimeExploitProtectionEnabled, tlsServerCertContent, tlsServerCertPassword, tlsTicketLifetime);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Broker {\n");
    
    sb.append("    authClientCertRevocationCheckMode: ").append(toIndentedString(authClientCertRevocationCheckMode)).append("\n");
    sb.append("    serviceAmqpEnabled: ").append(toIndentedString(serviceAmqpEnabled)).append("\n");
    sb.append("    serviceAmqpTlsListenPort: ").append(toIndentedString(serviceAmqpTlsListenPort)).append("\n");
    sb.append("    serviceEventConnectionCountThreshold: ").append(toIndentedString(serviceEventConnectionCountThreshold)).append("\n");
    sb.append("    serviceHealthCheckEnabled: ").append(toIndentedString(serviceHealthCheckEnabled)).append("\n");
    sb.append("    serviceHealthCheckListenPort: ").append(toIndentedString(serviceHealthCheckListenPort)).append("\n");
    sb.append("    serviceMateLinkEnabled: ").append(toIndentedString(serviceMateLinkEnabled)).append("\n");
    sb.append("    serviceMateLinkListenPort: ").append(toIndentedString(serviceMateLinkListenPort)).append("\n");
    sb.append("    serviceMqttEnabled: ").append(toIndentedString(serviceMqttEnabled)).append("\n");
    sb.append("    serviceMsgBackboneEnabled: ").append(toIndentedString(serviceMsgBackboneEnabled)).append("\n");
    sb.append("    serviceRedundancyEnabled: ").append(toIndentedString(serviceRedundancyEnabled)).append("\n");
    sb.append("    serviceRedundancyFirstListenPort: ").append(toIndentedString(serviceRedundancyFirstListenPort)).append("\n");
    sb.append("    serviceRestEventOutgoingConnectionCountThreshold: ").append(toIndentedString(serviceRestEventOutgoingConnectionCountThreshold)).append("\n");
    sb.append("    serviceRestIncomingEnabled: ").append(toIndentedString(serviceRestIncomingEnabled)).append("\n");
    sb.append("    serviceRestOutgoingEnabled: ").append(toIndentedString(serviceRestOutgoingEnabled)).append("\n");
    sb.append("    serviceSempPlainTextEnabled: ").append(toIndentedString(serviceSempPlainTextEnabled)).append("\n");
    sb.append("    serviceSempPlainTextListenPort: ").append(toIndentedString(serviceSempPlainTextListenPort)).append("\n");
    sb.append("    serviceSempTlsEnabled: ").append(toIndentedString(serviceSempTlsEnabled)).append("\n");
    sb.append("    serviceSempTlsListenPort: ").append(toIndentedString(serviceSempTlsListenPort)).append("\n");
    sb.append("    serviceSmfCompressionListenPort: ").append(toIndentedString(serviceSmfCompressionListenPort)).append("\n");
    sb.append("    serviceSmfEnabled: ").append(toIndentedString(serviceSmfEnabled)).append("\n");
    sb.append("    serviceSmfEventConnectionCountThreshold: ").append(toIndentedString(serviceSmfEventConnectionCountThreshold)).append("\n");
    sb.append("    serviceSmfPlainTextListenPort: ").append(toIndentedString(serviceSmfPlainTextListenPort)).append("\n");
    sb.append("    serviceSmfRoutingControlListenPort: ").append(toIndentedString(serviceSmfRoutingControlListenPort)).append("\n");
    sb.append("    serviceSmfTlsListenPort: ").append(toIndentedString(serviceSmfTlsListenPort)).append("\n");
    sb.append("    serviceTlsEventConnectionCountThreshold: ").append(toIndentedString(serviceTlsEventConnectionCountThreshold)).append("\n");
    sb.append("    serviceWebTransportEnabled: ").append(toIndentedString(serviceWebTransportEnabled)).append("\n");
    sb.append("    serviceWebTransportPlainTextListenPort: ").append(toIndentedString(serviceWebTransportPlainTextListenPort)).append("\n");
    sb.append("    serviceWebTransportTlsListenPort: ").append(toIndentedString(serviceWebTransportTlsListenPort)).append("\n");
    sb.append("    serviceWebTransportWebUrlSuffix: ").append(toIndentedString(serviceWebTransportWebUrlSuffix)).append("\n");
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
