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
import com.solace.psg.sempv2.monitor.model.EventThreshold;
import com.solace.psg.sempv2.monitor.model.EventThresholdByValue;
import com.solace.psg.sempv2.monitor.model.MsgVpnCounter;
import com.solace.psg.sempv2.monitor.model.MsgVpnRate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * MsgVpn
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:57:21.016551900+01:00[Europe/London]")
public class MsgVpn {
  @SerializedName("alias")
  private String alias = null;

  @SerializedName("authenticationBasicEnabled")
  private Boolean authenticationBasicEnabled = null;

  @SerializedName("authenticationBasicProfileName")
  private String authenticationBasicProfileName = null;

  @SerializedName("authenticationBasicRadiusDomain")
  private String authenticationBasicRadiusDomain = null;

  /**
   * The type of basic authentication to use for clients connecting to the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;internal\&quot; - Internal database. Authentication is against Client Usernames. \&quot;ldap\&quot; - LDAP authentication. An LDAP profile name must be provided. \&quot;radius\&quot; - RADIUS authentication. A RADIUS profile name must be provided. \&quot;none\&quot; - No authentication. Anonymous login allowed. &lt;/pre&gt; 
   */
  @JsonAdapter(AuthenticationBasicTypeEnum.Adapter.class)
  public enum AuthenticationBasicTypeEnum {
    INTERNAL("internal"),
    LDAP("ldap"),
    RADIUS("radius"),
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
        Object value = jsonReader.nextString();
        return AuthenticationBasicTypeEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("authenticationBasicType")
  private AuthenticationBasicTypeEnum authenticationBasicType = null;

  @SerializedName("authenticationClientCertAllowApiProvidedUsernameEnabled")
  private Boolean authenticationClientCertAllowApiProvidedUsernameEnabled = null;

  @SerializedName("authenticationClientCertEnabled")
  private Boolean authenticationClientCertEnabled = null;

  @SerializedName("authenticationClientCertMaxChainDepth")
  private Long authenticationClientCertMaxChainDepth = null;

  /**
   * The desired behavior for client certificate revocation checking. The allowed values and their meaning are:  &lt;pre&gt; \&quot;allow-all\&quot; - Allow the client to authenticate, the result of client certificate revocation check is ignored. \&quot;allow-unknown\&quot; - Allow the client to authenticate even if the revocation status of his certificate cannot be determined. \&quot;allow-valid\&quot; - Allow the client to authenticate only when the revocation check returned an explicit positive response. &lt;/pre&gt; 
   */
  @JsonAdapter(AuthenticationClientCertRevocationCheckModeEnum.Adapter.class)
  public enum AuthenticationClientCertRevocationCheckModeEnum {
    ALL("allow-all"),
    UNKNOWN("allow-unknown"),
    VALID("allow-valid");

    private String value;

    AuthenticationClientCertRevocationCheckModeEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static AuthenticationClientCertRevocationCheckModeEnum fromValue(String text) {
      for (AuthenticationClientCertRevocationCheckModeEnum b : AuthenticationClientCertRevocationCheckModeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<AuthenticationClientCertRevocationCheckModeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final AuthenticationClientCertRevocationCheckModeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public AuthenticationClientCertRevocationCheckModeEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return AuthenticationClientCertRevocationCheckModeEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("authenticationClientCertRevocationCheckMode")
  private AuthenticationClientCertRevocationCheckModeEnum authenticationClientCertRevocationCheckMode = null;

  /**
   * The field from the client certificate to use as the client username. The allowed values and their meaning are:  &lt;pre&gt; \&quot;common-name\&quot; - The username is extracted from the certificate&#x27;s Common Name. \&quot;subject-alternate-name-msupn\&quot; - The username is extracted from the certificate&#x27;s Other Name type of the Subject Alternative Name and must have the msUPN signature. &lt;/pre&gt; 
   */
  @JsonAdapter(AuthenticationClientCertUsernameSourceEnum.Adapter.class)
  public enum AuthenticationClientCertUsernameSourceEnum {
    COMMON_NAME("common-name"),
    SUBJECT_ALTERNATE_NAME_MSUPN("subject-alternate-name-msupn");

    private String value;

    AuthenticationClientCertUsernameSourceEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static AuthenticationClientCertUsernameSourceEnum fromValue(String text) {
      for (AuthenticationClientCertUsernameSourceEnum b : AuthenticationClientCertUsernameSourceEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<AuthenticationClientCertUsernameSourceEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final AuthenticationClientCertUsernameSourceEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public AuthenticationClientCertUsernameSourceEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return AuthenticationClientCertUsernameSourceEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("authenticationClientCertUsernameSource")
  private AuthenticationClientCertUsernameSourceEnum authenticationClientCertUsernameSource = null;

  @SerializedName("authenticationClientCertValidateDateEnabled")
  private Boolean authenticationClientCertValidateDateEnabled = null;

  @SerializedName("authenticationKerberosAllowApiProvidedUsernameEnabled")
  private Boolean authenticationKerberosAllowApiProvidedUsernameEnabled = null;

  @SerializedName("authenticationKerberosEnabled")
  private Boolean authenticationKerberosEnabled = null;

  @SerializedName("authenticationOauthDefaultProviderName")
  private String authenticationOauthDefaultProviderName = null;

  @SerializedName("authenticationOauthEnabled")
  private Boolean authenticationOauthEnabled = null;

  @SerializedName("authorizationLdapGroupMembershipAttributeName")
  private String authorizationLdapGroupMembershipAttributeName = null;

  @SerializedName("authorizationLdapTrimClientUsernameDomainEnabled")
  private Boolean authorizationLdapTrimClientUsernameDomainEnabled = null;

  @SerializedName("authorizationProfileName")
  private String authorizationProfileName = null;

  /**
   * The type of authorization to use for clients connecting to the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;ldap\&quot; - LDAP authorization. \&quot;internal\&quot; - Internal authorization. &lt;/pre&gt; 
   */
  @JsonAdapter(AuthorizationTypeEnum.Adapter.class)
  public enum AuthorizationTypeEnum {
    LDAP("ldap"),
    INTERNAL("internal");

    private String value;

    AuthorizationTypeEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static AuthorizationTypeEnum fromValue(String text) {
      for (AuthorizationTypeEnum b : AuthorizationTypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<AuthorizationTypeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final AuthorizationTypeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public AuthorizationTypeEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return AuthorizationTypeEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("authorizationType")
  private AuthorizationTypeEnum authorizationType = null;

  @SerializedName("averageRxByteRate")
  private Long averageRxByteRate = null;

  @SerializedName("averageRxCompressedByteRate")
  private Long averageRxCompressedByteRate = null;

  @SerializedName("averageRxMsgRate")
  private Long averageRxMsgRate = null;

  @SerializedName("averageRxUncompressedByteRate")
  private Long averageRxUncompressedByteRate = null;

  @SerializedName("averageTxByteRate")
  private Long averageTxByteRate = null;

  @SerializedName("averageTxCompressedByteRate")
  private Long averageTxCompressedByteRate = null;

  @SerializedName("averageTxMsgRate")
  private Long averageTxMsgRate = null;

  @SerializedName("averageTxUncompressedByteRate")
  private Long averageTxUncompressedByteRate = null;

  @SerializedName("bridgingTlsServerCertEnforceTrustedCommonNameEnabled")
  private Boolean bridgingTlsServerCertEnforceTrustedCommonNameEnabled = null;

  @SerializedName("bridgingTlsServerCertMaxChainDepth")
  private Long bridgingTlsServerCertMaxChainDepth = null;

  @SerializedName("bridgingTlsServerCertValidateDateEnabled")
  private Boolean bridgingTlsServerCertValidateDateEnabled = null;

  @SerializedName("configSyncLocalKey")
  private String configSyncLocalKey = null;

  @SerializedName("configSyncLocalLastResult")
  private String configSyncLocalLastResult = null;

  @SerializedName("configSyncLocalRole")
  private String configSyncLocalRole = null;

  @SerializedName("configSyncLocalState")
  private String configSyncLocalState = null;

  @SerializedName("configSyncLocalTimeInState")
  private Integer configSyncLocalTimeInState = null;

  @SerializedName("controlRxByteCount")
  private Long controlRxByteCount = null;

  @SerializedName("controlRxMsgCount")
  private Long controlRxMsgCount = null;

  @SerializedName("controlTxByteCount")
  private Long controlTxByteCount = null;

  @SerializedName("controlTxMsgCount")
  private Long controlTxMsgCount = null;

  @SerializedName("counter")
  private MsgVpnCounter counter = null;

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

  @SerializedName("distributedCacheManagementEnabled")
  private Boolean distributedCacheManagementEnabled = null;

  @SerializedName("dmrEnabled")
  private Boolean dmrEnabled = null;

  @SerializedName("enabled")
  private Boolean enabled = null;

  @SerializedName("eventConnectionCountThreshold")
  private EventThreshold eventConnectionCountThreshold = null;

  @SerializedName("eventEgressFlowCountThreshold")
  private EventThreshold eventEgressFlowCountThreshold = null;

  @SerializedName("eventEgressMsgRateThreshold")
  private EventThresholdByValue eventEgressMsgRateThreshold = null;

  @SerializedName("eventEndpointCountThreshold")
  private EventThreshold eventEndpointCountThreshold = null;

  @SerializedName("eventIngressFlowCountThreshold")
  private EventThreshold eventIngressFlowCountThreshold = null;

  @SerializedName("eventIngressMsgRateThreshold")
  private EventThresholdByValue eventIngressMsgRateThreshold = null;

  @SerializedName("eventLargeMsgThreshold")
  private Long eventLargeMsgThreshold = null;

  @SerializedName("eventLogTag")
  private String eventLogTag = null;

  @SerializedName("eventMsgSpoolUsageThreshold")
  private EventThreshold eventMsgSpoolUsageThreshold = null;

  @SerializedName("eventPublishClientEnabled")
  private Boolean eventPublishClientEnabled = null;

  @SerializedName("eventPublishMsgVpnEnabled")
  private Boolean eventPublishMsgVpnEnabled = null;

  /**
   * The mode of subscription Events published in the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;off\&quot; - Disable client level event message publishing. \&quot;on-with-format-v1\&quot; - Enable client level event message publishing with format v1. \&quot;on-with-no-unsubscribe-events-on-disconnect-format-v1\&quot; - As \&quot;on-with-format-v1\&quot;, but unsubscribe events are not generated when a client disconnects. Unsubscribe events are still raised when a client explicitly unsubscribes from its subscriptions. \&quot;on-with-format-v2\&quot; - Enable client level event message publishing with format v2. \&quot;on-with-no-unsubscribe-events-on-disconnect-format-v2\&quot; - As \&quot;on-with-format-v2\&quot;, but unsubscribe events are not generated when a client disconnects. Unsubscribe events are still raised when a client explicitly unsubscribes from its subscriptions. &lt;/pre&gt; 
   */
  @JsonAdapter(EventPublishSubscriptionModeEnum.Adapter.class)
  public enum EventPublishSubscriptionModeEnum {
    OFF("off"),
    ON_WITH_FORMAT_V1("on-with-format-v1"),
    ON_WITH_NO_UNSUBSCRIBE_EVENTS_ON_DISCONNECT_FORMAT_V1("on-with-no-unsubscribe-events-on-disconnect-format-v1"),
    ON_WITH_FORMAT_V2("on-with-format-v2"),
    ON_WITH_NO_UNSUBSCRIBE_EVENTS_ON_DISCONNECT_FORMAT_V2("on-with-no-unsubscribe-events-on-disconnect-format-v2");

    private String value;

    EventPublishSubscriptionModeEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static EventPublishSubscriptionModeEnum fromValue(String text) {
      for (EventPublishSubscriptionModeEnum b : EventPublishSubscriptionModeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<EventPublishSubscriptionModeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final EventPublishSubscriptionModeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public EventPublishSubscriptionModeEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return EventPublishSubscriptionModeEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("eventPublishSubscriptionMode")
  private EventPublishSubscriptionModeEnum eventPublishSubscriptionMode = null;

  @SerializedName("eventPublishTopicFormatMqttEnabled")
  private Boolean eventPublishTopicFormatMqttEnabled = null;

  @SerializedName("eventPublishTopicFormatSmfEnabled")
  private Boolean eventPublishTopicFormatSmfEnabled = null;

  @SerializedName("eventServiceAmqpConnectionCountThreshold")
  private EventThreshold eventServiceAmqpConnectionCountThreshold = null;

  @SerializedName("eventServiceMqttConnectionCountThreshold")
  private EventThreshold eventServiceMqttConnectionCountThreshold = null;

  @SerializedName("eventServiceRestIncomingConnectionCountThreshold")
  private EventThreshold eventServiceRestIncomingConnectionCountThreshold = null;

  @SerializedName("eventServiceSmfConnectionCountThreshold")
  private EventThreshold eventServiceSmfConnectionCountThreshold = null;

  @SerializedName("eventServiceWebConnectionCountThreshold")
  private EventThreshold eventServiceWebConnectionCountThreshold = null;

  @SerializedName("eventSubscriptionCountThreshold")
  private EventThreshold eventSubscriptionCountThreshold = null;

  @SerializedName("eventTransactedSessionCountThreshold")
  private EventThreshold eventTransactedSessionCountThreshold = null;

  @SerializedName("eventTransactionCountThreshold")
  private EventThreshold eventTransactionCountThreshold = null;

  @SerializedName("exportSubscriptionsEnabled")
  private Boolean exportSubscriptionsEnabled = null;

  @SerializedName("failureReason")
  private String failureReason = null;

  @SerializedName("jndiEnabled")
  private Boolean jndiEnabled = null;

  @SerializedName("loginRxMsgCount")
  private Long loginRxMsgCount = null;

  @SerializedName("loginTxMsgCount")
  private Long loginTxMsgCount = null;

  @SerializedName("maxConnectionCount")
  private Long maxConnectionCount = null;

  @SerializedName("maxEffectiveEndpointCount")
  private Integer maxEffectiveEndpointCount = null;

  @SerializedName("maxEffectiveRxFlowCount")
  private Integer maxEffectiveRxFlowCount = null;

  @SerializedName("maxEffectiveSubscriptionCount")
  private Long maxEffectiveSubscriptionCount = null;

  @SerializedName("maxEffectiveTransactedSessionCount")
  private Integer maxEffectiveTransactedSessionCount = null;

  @SerializedName("maxEffectiveTransactionCount")
  private Integer maxEffectiveTransactionCount = null;

  @SerializedName("maxEffectiveTxFlowCount")
  private Integer maxEffectiveTxFlowCount = null;

  @SerializedName("maxEgressFlowCount")
  private Long maxEgressFlowCount = null;

  @SerializedName("maxEndpointCount")
  private Long maxEndpointCount = null;

  @SerializedName("maxIngressFlowCount")
  private Long maxIngressFlowCount = null;

  @SerializedName("maxMsgSpoolUsage")
  private Long maxMsgSpoolUsage = null;

  @SerializedName("maxSubscriptionCount")
  private Long maxSubscriptionCount = null;

  @SerializedName("maxTransactedSessionCount")
  private Long maxTransactedSessionCount = null;

  @SerializedName("maxTransactionCount")
  private Long maxTransactionCount = null;

  @SerializedName("mqttRetainMaxMemory")
  private Integer mqttRetainMaxMemory = null;

  @SerializedName("msgReplayActiveCount")
  private Integer msgReplayActiveCount = null;

  @SerializedName("msgReplayFailedCount")
  private Integer msgReplayFailedCount = null;

  @SerializedName("msgReplayInitializingCount")
  private Integer msgReplayInitializingCount = null;

  @SerializedName("msgReplayPendingCompleteCount")
  private Integer msgReplayPendingCompleteCount = null;

  @SerializedName("msgSpoolMsgCount")
  private Long msgSpoolMsgCount = null;

  @SerializedName("msgSpoolRxMsgCount")
  private Long msgSpoolRxMsgCount = null;

  @SerializedName("msgSpoolTxMsgCount")
  private Long msgSpoolTxMsgCount = null;

  @SerializedName("msgSpoolUsage")
  private Long msgSpoolUsage = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("rate")
  private MsgVpnRate rate = null;

  @SerializedName("replicationAckPropagationIntervalMsgCount")
  private Long replicationAckPropagationIntervalMsgCount = null;

  @SerializedName("replicationActiveAckPropTxMsgCount")
  private Long replicationActiveAckPropTxMsgCount = null;

  @SerializedName("replicationActiveAsyncQueuedMsgCount")
  private Long replicationActiveAsyncQueuedMsgCount = null;

  @SerializedName("replicationActiveLocallyConsumedMsgCount")
  private Long replicationActiveLocallyConsumedMsgCount = null;

  @SerializedName("replicationActiveMateFlowCongestedPeakTime")
  private Integer replicationActiveMateFlowCongestedPeakTime = null;

  @SerializedName("replicationActiveMateFlowNotCongestedPeakTime")
  private Integer replicationActiveMateFlowNotCongestedPeakTime = null;

  @SerializedName("replicationActivePromotedQueuedMsgCount")
  private Long replicationActivePromotedQueuedMsgCount = null;

  @SerializedName("replicationActiveReconcileRequestRxMsgCount")
  private Long replicationActiveReconcileRequestRxMsgCount = null;

  @SerializedName("replicationActiveSyncEligiblePeakTime")
  private Integer replicationActiveSyncEligiblePeakTime = null;

  @SerializedName("replicationActiveSyncIneligiblePeakTime")
  private Integer replicationActiveSyncIneligiblePeakTime = null;

  @SerializedName("replicationActiveSyncQueuedAsAsyncMsgCount")
  private Long replicationActiveSyncQueuedAsAsyncMsgCount = null;

  @SerializedName("replicationActiveSyncQueuedMsgCount")
  private Long replicationActiveSyncQueuedMsgCount = null;

  @SerializedName("replicationActiveTransitionToSyncIneligibleCount")
  private Long replicationActiveTransitionToSyncIneligibleCount = null;

  @SerializedName("replicationBridgeAuthenticationBasicClientUsername")
  private String replicationBridgeAuthenticationBasicClientUsername = null;

  /**
   * The authentication scheme for the replication Bridge in the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;basic\&quot; - Basic Authentication Scheme (via username and password). \&quot;client-certificate\&quot; - Client Certificate Authentication Scheme (via certificate file or content). &lt;/pre&gt;  Available since 2.12.
   */
  @JsonAdapter(ReplicationBridgeAuthenticationSchemeEnum.Adapter.class)
  public enum ReplicationBridgeAuthenticationSchemeEnum {
    BASIC("basic"),
    CLIENT_CERTIFICATE("client-certificate");

    private String value;

    ReplicationBridgeAuthenticationSchemeEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static ReplicationBridgeAuthenticationSchemeEnum fromValue(String text) {
      for (ReplicationBridgeAuthenticationSchemeEnum b : ReplicationBridgeAuthenticationSchemeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<ReplicationBridgeAuthenticationSchemeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final ReplicationBridgeAuthenticationSchemeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public ReplicationBridgeAuthenticationSchemeEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return ReplicationBridgeAuthenticationSchemeEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("replicationBridgeAuthenticationScheme")
  private ReplicationBridgeAuthenticationSchemeEnum replicationBridgeAuthenticationScheme = null;

  @SerializedName("replicationBridgeBoundToQueue")
  private Boolean replicationBridgeBoundToQueue = null;

  @SerializedName("replicationBridgeCompressedDataEnabled")
  private Boolean replicationBridgeCompressedDataEnabled = null;

  @SerializedName("replicationBridgeEgressFlowWindowSize")
  private Long replicationBridgeEgressFlowWindowSize = null;

  @SerializedName("replicationBridgeName")
  private String replicationBridgeName = null;

  @SerializedName("replicationBridgeRetryDelay")
  private Long replicationBridgeRetryDelay = null;

  @SerializedName("replicationBridgeTlsEnabled")
  private Boolean replicationBridgeTlsEnabled = null;

  @SerializedName("replicationBridgeUnidirectionalClientProfileName")
  private String replicationBridgeUnidirectionalClientProfileName = null;

  @SerializedName("replicationBridgeUp")
  private Boolean replicationBridgeUp = null;

  @SerializedName("replicationEnabled")
  private Boolean replicationEnabled = null;

  @SerializedName("replicationQueueBound")
  private Boolean replicationQueueBound = null;

  @SerializedName("replicationQueueMaxMsgSpoolUsage")
  private Long replicationQueueMaxMsgSpoolUsage = null;

  @SerializedName("replicationQueueRejectMsgToSenderOnDiscardEnabled")
  private Boolean replicationQueueRejectMsgToSenderOnDiscardEnabled = null;

  @SerializedName("replicationRejectMsgWhenSyncIneligibleEnabled")
  private Boolean replicationRejectMsgWhenSyncIneligibleEnabled = null;

  @SerializedName("replicationRemoteBridgeName")
  private String replicationRemoteBridgeName = null;

  @SerializedName("replicationRemoteBridgeUp")
  private Boolean replicationRemoteBridgeUp = null;

  /**
   * The replication role for the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;active\&quot; - Assume the Active role in replication for the Message VPN. \&quot;standby\&quot; - Assume the Standby role in replication for the Message VPN. &lt;/pre&gt;  Available since 2.12.
   */
  @JsonAdapter(ReplicationRoleEnum.Adapter.class)
  public enum ReplicationRoleEnum {
    ACTIVE("active"),
    STANDBY("standby");

    private String value;

    ReplicationRoleEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static ReplicationRoleEnum fromValue(String text) {
      for (ReplicationRoleEnum b : ReplicationRoleEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<ReplicationRoleEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final ReplicationRoleEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public ReplicationRoleEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return ReplicationRoleEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("replicationRole")
  private ReplicationRoleEnum replicationRole = null;

  @SerializedName("replicationStandbyAckPropOutOfSeqRxMsgCount")
  private Long replicationStandbyAckPropOutOfSeqRxMsgCount = null;

  @SerializedName("replicationStandbyAckPropRxMsgCount")
  private Long replicationStandbyAckPropRxMsgCount = null;

  @SerializedName("replicationStandbyReconcileRequestTxMsgCount")
  private Long replicationStandbyReconcileRequestTxMsgCount = null;

  @SerializedName("replicationStandbyRxMsgCount")
  private Long replicationStandbyRxMsgCount = null;

  @SerializedName("replicationStandbyTransactionRequestCount")
  private Long replicationStandbyTransactionRequestCount = null;

  @SerializedName("replicationStandbyTransactionRequestFailureCount")
  private Long replicationStandbyTransactionRequestFailureCount = null;

  @SerializedName("replicationStandbyTransactionRequestSuccessCount")
  private Long replicationStandbyTransactionRequestSuccessCount = null;

  @SerializedName("replicationSyncEligible")
  private Boolean replicationSyncEligible = null;

  /**
   * Indicates whether synchronous or asynchronous replication mode is used for all transactions within the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;sync\&quot; - Messages are acknowledged when replicated (spooled remotely). \&quot;async\&quot; - Messages are acknowledged when pending replication (spooled locally). &lt;/pre&gt;  Available since 2.12.
   */
  @JsonAdapter(ReplicationTransactionModeEnum.Adapter.class)
  public enum ReplicationTransactionModeEnum {
    SYNC("sync"),
    ASYNC("async");

    private String value;

    ReplicationTransactionModeEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static ReplicationTransactionModeEnum fromValue(String text) {
      for (ReplicationTransactionModeEnum b : ReplicationTransactionModeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<ReplicationTransactionModeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final ReplicationTransactionModeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public ReplicationTransactionModeEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return ReplicationTransactionModeEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("replicationTransactionMode")
  private ReplicationTransactionModeEnum replicationTransactionMode = null;

  @SerializedName("restTlsServerCertEnforceTrustedCommonNameEnabled")
  private Boolean restTlsServerCertEnforceTrustedCommonNameEnabled = null;

  @SerializedName("restTlsServerCertMaxChainDepth")
  private Long restTlsServerCertMaxChainDepth = null;

  @SerializedName("restTlsServerCertValidateDateEnabled")
  private Boolean restTlsServerCertValidateDateEnabled = null;

  @SerializedName("restTlsServerCertValidateNameEnabled")
  private Boolean restTlsServerCertValidateNameEnabled = null;

  @SerializedName("rxByteCount")
  private Long rxByteCount = null;

  @SerializedName("rxByteRate")
  private Long rxByteRate = null;

  @SerializedName("rxCompressedByteCount")
  private Long rxCompressedByteCount = null;

  @SerializedName("rxCompressedByteRate")
  private Long rxCompressedByteRate = null;

  @SerializedName("rxCompressionRatio")
  private String rxCompressionRatio = null;

  @SerializedName("rxMsgCount")
  private Long rxMsgCount = null;

  @SerializedName("rxMsgRate")
  private Long rxMsgRate = null;

  @SerializedName("rxUncompressedByteCount")
  private Long rxUncompressedByteCount = null;

  @SerializedName("rxUncompressedByteRate")
  private Long rxUncompressedByteRate = null;

  @SerializedName("sempOverMsgBusAdminClientEnabled")
  private Boolean sempOverMsgBusAdminClientEnabled = null;

  @SerializedName("sempOverMsgBusAdminDistributedCacheEnabled")
  private Boolean sempOverMsgBusAdminDistributedCacheEnabled = null;

  @SerializedName("sempOverMsgBusAdminEnabled")
  private Boolean sempOverMsgBusAdminEnabled = null;

  @SerializedName("sempOverMsgBusEnabled")
  private Boolean sempOverMsgBusEnabled = null;

  @SerializedName("sempOverMsgBusShowEnabled")
  private Boolean sempOverMsgBusShowEnabled = null;

  @SerializedName("serviceAmqpMaxConnectionCount")
  private Long serviceAmqpMaxConnectionCount = null;

  @SerializedName("serviceAmqpPlainTextCompressed")
  private Boolean serviceAmqpPlainTextCompressed = null;

  @SerializedName("serviceAmqpPlainTextEnabled")
  private Boolean serviceAmqpPlainTextEnabled = null;

  @SerializedName("serviceAmqpPlainTextFailureReason")
  private String serviceAmqpPlainTextFailureReason = null;

  @SerializedName("serviceAmqpPlainTextListenPort")
  private Long serviceAmqpPlainTextListenPort = null;

  @SerializedName("serviceAmqpPlainTextUp")
  private Boolean serviceAmqpPlainTextUp = null;

  @SerializedName("serviceAmqpTlsCompressed")
  private Boolean serviceAmqpTlsCompressed = null;

  @SerializedName("serviceAmqpTlsEnabled")
  private Boolean serviceAmqpTlsEnabled = null;

  @SerializedName("serviceAmqpTlsFailureReason")
  private String serviceAmqpTlsFailureReason = null;

  @SerializedName("serviceAmqpTlsListenPort")
  private Long serviceAmqpTlsListenPort = null;

  @SerializedName("serviceAmqpTlsUp")
  private Boolean serviceAmqpTlsUp = null;

  @SerializedName("serviceMqttMaxConnectionCount")
  private Long serviceMqttMaxConnectionCount = null;

  @SerializedName("serviceMqttPlainTextCompressed")
  private Boolean serviceMqttPlainTextCompressed = null;

  @SerializedName("serviceMqttPlainTextEnabled")
  private Boolean serviceMqttPlainTextEnabled = null;

  @SerializedName("serviceMqttPlainTextFailureReason")
  private String serviceMqttPlainTextFailureReason = null;

  @SerializedName("serviceMqttPlainTextListenPort")
  private Long serviceMqttPlainTextListenPort = null;

  @SerializedName("serviceMqttPlainTextUp")
  private Boolean serviceMqttPlainTextUp = null;

  @SerializedName("serviceMqttTlsCompressed")
  private Boolean serviceMqttTlsCompressed = null;

  @SerializedName("serviceMqttTlsEnabled")
  private Boolean serviceMqttTlsEnabled = null;

  @SerializedName("serviceMqttTlsFailureReason")
  private String serviceMqttTlsFailureReason = null;

  @SerializedName("serviceMqttTlsListenPort")
  private Long serviceMqttTlsListenPort = null;

  @SerializedName("serviceMqttTlsUp")
  private Boolean serviceMqttTlsUp = null;

  @SerializedName("serviceMqttTlsWebSocketCompressed")
  private Boolean serviceMqttTlsWebSocketCompressed = null;

  @SerializedName("serviceMqttTlsWebSocketEnabled")
  private Boolean serviceMqttTlsWebSocketEnabled = null;

  @SerializedName("serviceMqttTlsWebSocketFailureReason")
  private String serviceMqttTlsWebSocketFailureReason = null;

  @SerializedName("serviceMqttTlsWebSocketListenPort")
  private Long serviceMqttTlsWebSocketListenPort = null;

  @SerializedName("serviceMqttTlsWebSocketUp")
  private Boolean serviceMqttTlsWebSocketUp = null;

  @SerializedName("serviceMqttWebSocketCompressed")
  private Boolean serviceMqttWebSocketCompressed = null;

  @SerializedName("serviceMqttWebSocketEnabled")
  private Boolean serviceMqttWebSocketEnabled = null;

  @SerializedName("serviceMqttWebSocketFailureReason")
  private String serviceMqttWebSocketFailureReason = null;

  @SerializedName("serviceMqttWebSocketListenPort")
  private Long serviceMqttWebSocketListenPort = null;

  @SerializedName("serviceMqttWebSocketUp")
  private Boolean serviceMqttWebSocketUp = null;

  @SerializedName("serviceRestIncomingMaxConnectionCount")
  private Long serviceRestIncomingMaxConnectionCount = null;

  @SerializedName("serviceRestIncomingPlainTextCompressed")
  private Boolean serviceRestIncomingPlainTextCompressed = null;

  @SerializedName("serviceRestIncomingPlainTextEnabled")
  private Boolean serviceRestIncomingPlainTextEnabled = null;

  @SerializedName("serviceRestIncomingPlainTextFailureReason")
  private String serviceRestIncomingPlainTextFailureReason = null;

  @SerializedName("serviceRestIncomingPlainTextListenPort")
  private Long serviceRestIncomingPlainTextListenPort = null;

  @SerializedName("serviceRestIncomingPlainTextUp")
  private Boolean serviceRestIncomingPlainTextUp = null;

  @SerializedName("serviceRestIncomingTlsCompressed")
  private Boolean serviceRestIncomingTlsCompressed = null;

  @SerializedName("serviceRestIncomingTlsEnabled")
  private Boolean serviceRestIncomingTlsEnabled = null;

  @SerializedName("serviceRestIncomingTlsFailureReason")
  private String serviceRestIncomingTlsFailureReason = null;

  @SerializedName("serviceRestIncomingTlsListenPort")
  private Long serviceRestIncomingTlsListenPort = null;

  @SerializedName("serviceRestIncomingTlsUp")
  private Boolean serviceRestIncomingTlsUp = null;

  /**
   * The REST service mode for incoming REST clients that connect to the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;gateway\&quot; - Act as a message gateway through which REST messages are propagated. \&quot;messaging\&quot; - Act as a message broker on which REST messages are queued. &lt;/pre&gt; 
   */
  @JsonAdapter(ServiceRestModeEnum.Adapter.class)
  public enum ServiceRestModeEnum {
    GATEWAY("gateway"),
    MESSAGING("messaging");

    private String value;

    ServiceRestModeEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static ServiceRestModeEnum fromValue(String text) {
      for (ServiceRestModeEnum b : ServiceRestModeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<ServiceRestModeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final ServiceRestModeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public ServiceRestModeEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return ServiceRestModeEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("serviceRestMode")
  private ServiceRestModeEnum serviceRestMode = null;

  @SerializedName("serviceRestOutgoingMaxConnectionCount")
  private Long serviceRestOutgoingMaxConnectionCount = null;

  @SerializedName("serviceSmfMaxConnectionCount")
  private Long serviceSmfMaxConnectionCount = null;

  @SerializedName("serviceSmfPlainTextEnabled")
  private Boolean serviceSmfPlainTextEnabled = null;

  @SerializedName("serviceSmfPlainTextFailureReason")
  private String serviceSmfPlainTextFailureReason = null;

  @SerializedName("serviceSmfPlainTextUp")
  private Boolean serviceSmfPlainTextUp = null;

  @SerializedName("serviceSmfTlsEnabled")
  private Boolean serviceSmfTlsEnabled = null;

  @SerializedName("serviceSmfTlsFailureReason")
  private String serviceSmfTlsFailureReason = null;

  @SerializedName("serviceSmfTlsUp")
  private Boolean serviceSmfTlsUp = null;

  @SerializedName("serviceWebMaxConnectionCount")
  private Long serviceWebMaxConnectionCount = null;

  @SerializedName("serviceWebPlainTextEnabled")
  private Boolean serviceWebPlainTextEnabled = null;

  @SerializedName("serviceWebPlainTextFailureReason")
  private String serviceWebPlainTextFailureReason = null;

  @SerializedName("serviceWebPlainTextUp")
  private Boolean serviceWebPlainTextUp = null;

  @SerializedName("serviceWebTlsEnabled")
  private Boolean serviceWebTlsEnabled = null;

  @SerializedName("serviceWebTlsFailureReason")
  private String serviceWebTlsFailureReason = null;

  @SerializedName("serviceWebTlsUp")
  private Boolean serviceWebTlsUp = null;

  @SerializedName("state")
  private String state = null;

  @SerializedName("subscriptionExportProgress")
  private Long subscriptionExportProgress = null;

  @SerializedName("systemManager")
  private Boolean systemManager = null;

  @SerializedName("tlsAllowDowngradeToPlainTextEnabled")
  private Boolean tlsAllowDowngradeToPlainTextEnabled = null;

  @SerializedName("tlsAverageRxByteRate")
  private Long tlsAverageRxByteRate = null;

  @SerializedName("tlsAverageTxByteRate")
  private Long tlsAverageTxByteRate = null;

  @SerializedName("tlsRxByteCount")
  private Long tlsRxByteCount = null;

  @SerializedName("tlsRxByteRate")
  private Long tlsRxByteRate = null;

  @SerializedName("tlsTxByteCount")
  private Long tlsTxByteCount = null;

  @SerializedName("tlsTxByteRate")
  private Long tlsTxByteRate = null;

  @SerializedName("txByteCount")
  private Long txByteCount = null;

  @SerializedName("txByteRate")
  private Long txByteRate = null;

  @SerializedName("txCompressedByteCount")
  private Long txCompressedByteCount = null;

  @SerializedName("txCompressedByteRate")
  private Long txCompressedByteRate = null;

  @SerializedName("txCompressionRatio")
  private String txCompressionRatio = null;

  @SerializedName("txMsgCount")
  private Long txMsgCount = null;

  @SerializedName("txMsgRate")
  private Long txMsgRate = null;

  @SerializedName("txUncompressedByteCount")
  private Long txUncompressedByteCount = null;

  @SerializedName("txUncompressedByteRate")
  private Long txUncompressedByteRate = null;

  public MsgVpn alias(String alias) {
    this.alias = alias;
    return this;
  }

   /**
   * The name of another Message VPN which this Message VPN is an alias for. Available since 2.14.
   * @return alias
  **/
  @Schema(description = "The name of another Message VPN which this Message VPN is an alias for. Available since 2.14.")
  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public MsgVpn authenticationBasicEnabled(Boolean authenticationBasicEnabled) {
    this.authenticationBasicEnabled = authenticationBasicEnabled;
    return this;
  }

   /**
   * Indicates whether basic authentication is enabled for clients connecting to the Message VPN.
   * @return authenticationBasicEnabled
  **/
  @Schema(description = "Indicates whether basic authentication is enabled for clients connecting to the Message VPN.")
  public Boolean isAuthenticationBasicEnabled() {
    return authenticationBasicEnabled;
  }

  public void setAuthenticationBasicEnabled(Boolean authenticationBasicEnabled) {
    this.authenticationBasicEnabled = authenticationBasicEnabled;
  }

  public MsgVpn authenticationBasicProfileName(String authenticationBasicProfileName) {
    this.authenticationBasicProfileName = authenticationBasicProfileName;
    return this;
  }

   /**
   * The name of the RADIUS or LDAP Profile to use for basic authentication.
   * @return authenticationBasicProfileName
  **/
  @Schema(description = "The name of the RADIUS or LDAP Profile to use for basic authentication.")
  public String getAuthenticationBasicProfileName() {
    return authenticationBasicProfileName;
  }

  public void setAuthenticationBasicProfileName(String authenticationBasicProfileName) {
    this.authenticationBasicProfileName = authenticationBasicProfileName;
  }

  public MsgVpn authenticationBasicRadiusDomain(String authenticationBasicRadiusDomain) {
    this.authenticationBasicRadiusDomain = authenticationBasicRadiusDomain;
    return this;
  }

   /**
   * The RADIUS domain to use for basic authentication.
   * @return authenticationBasicRadiusDomain
  **/
  @Schema(description = "The RADIUS domain to use for basic authentication.")
  public String getAuthenticationBasicRadiusDomain() {
    return authenticationBasicRadiusDomain;
  }

  public void setAuthenticationBasicRadiusDomain(String authenticationBasicRadiusDomain) {
    this.authenticationBasicRadiusDomain = authenticationBasicRadiusDomain;
  }

  public MsgVpn authenticationBasicType(AuthenticationBasicTypeEnum authenticationBasicType) {
    this.authenticationBasicType = authenticationBasicType;
    return this;
  }

   /**
   * The type of basic authentication to use for clients connecting to the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;internal\&quot; - Internal database. Authentication is against Client Usernames. \&quot;ldap\&quot; - LDAP authentication. An LDAP profile name must be provided. \&quot;radius\&quot; - RADIUS authentication. A RADIUS profile name must be provided. \&quot;none\&quot; - No authentication. Anonymous login allowed. &lt;/pre&gt; 
   * @return authenticationBasicType
  **/
  @Schema(description = "The type of basic authentication to use for clients connecting to the Message VPN. The allowed values and their meaning are:  <pre> \"internal\" - Internal database. Authentication is against Client Usernames. \"ldap\" - LDAP authentication. An LDAP profile name must be provided. \"radius\" - RADIUS authentication. A RADIUS profile name must be provided. \"none\" - No authentication. Anonymous login allowed. </pre> ")
  public AuthenticationBasicTypeEnum getAuthenticationBasicType() {
    return authenticationBasicType;
  }

  public void setAuthenticationBasicType(AuthenticationBasicTypeEnum authenticationBasicType) {
    this.authenticationBasicType = authenticationBasicType;
  }

  public MsgVpn authenticationClientCertAllowApiProvidedUsernameEnabled(Boolean authenticationClientCertAllowApiProvidedUsernameEnabled) {
    this.authenticationClientCertAllowApiProvidedUsernameEnabled = authenticationClientCertAllowApiProvidedUsernameEnabled;
    return this;
  }

   /**
   * Indicates whether a client is allowed to specify a Client Username via the API connect method. When disabled, the certificate CN (Common Name) is always used.
   * @return authenticationClientCertAllowApiProvidedUsernameEnabled
  **/
  @Schema(description = "Indicates whether a client is allowed to specify a Client Username via the API connect method. When disabled, the certificate CN (Common Name) is always used.")
  public Boolean isAuthenticationClientCertAllowApiProvidedUsernameEnabled() {
    return authenticationClientCertAllowApiProvidedUsernameEnabled;
  }

  public void setAuthenticationClientCertAllowApiProvidedUsernameEnabled(Boolean authenticationClientCertAllowApiProvidedUsernameEnabled) {
    this.authenticationClientCertAllowApiProvidedUsernameEnabled = authenticationClientCertAllowApiProvidedUsernameEnabled;
  }

  public MsgVpn authenticationClientCertEnabled(Boolean authenticationClientCertEnabled) {
    this.authenticationClientCertEnabled = authenticationClientCertEnabled;
    return this;
  }

   /**
   * Indicates whether client certificate authentication is enabled in the Message VPN.
   * @return authenticationClientCertEnabled
  **/
  @Schema(description = "Indicates whether client certificate authentication is enabled in the Message VPN.")
  public Boolean isAuthenticationClientCertEnabled() {
    return authenticationClientCertEnabled;
  }

  public void setAuthenticationClientCertEnabled(Boolean authenticationClientCertEnabled) {
    this.authenticationClientCertEnabled = authenticationClientCertEnabled;
  }

  public MsgVpn authenticationClientCertMaxChainDepth(Long authenticationClientCertMaxChainDepth) {
    this.authenticationClientCertMaxChainDepth = authenticationClientCertMaxChainDepth;
    return this;
  }

   /**
   * The maximum depth for a client certificate chain. The depth of a chain is defined as the number of signing CA certificates that are present in the chain back to a trusted self-signed root CA certificate.
   * @return authenticationClientCertMaxChainDepth
  **/
  @Schema(description = "The maximum depth for a client certificate chain. The depth of a chain is defined as the number of signing CA certificates that are present in the chain back to a trusted self-signed root CA certificate.")
  public Long getAuthenticationClientCertMaxChainDepth() {
    return authenticationClientCertMaxChainDepth;
  }

  public void setAuthenticationClientCertMaxChainDepth(Long authenticationClientCertMaxChainDepth) {
    this.authenticationClientCertMaxChainDepth = authenticationClientCertMaxChainDepth;
  }

  public MsgVpn authenticationClientCertRevocationCheckMode(AuthenticationClientCertRevocationCheckModeEnum authenticationClientCertRevocationCheckMode) {
    this.authenticationClientCertRevocationCheckMode = authenticationClientCertRevocationCheckMode;
    return this;
  }

   /**
   * The desired behavior for client certificate revocation checking. The allowed values and their meaning are:  &lt;pre&gt; \&quot;allow-all\&quot; - Allow the client to authenticate, the result of client certificate revocation check is ignored. \&quot;allow-unknown\&quot; - Allow the client to authenticate even if the revocation status of his certificate cannot be determined. \&quot;allow-valid\&quot; - Allow the client to authenticate only when the revocation check returned an explicit positive response. &lt;/pre&gt; 
   * @return authenticationClientCertRevocationCheckMode
  **/
  @Schema(description = "The desired behavior for client certificate revocation checking. The allowed values and their meaning are:  <pre> \"allow-all\" - Allow the client to authenticate, the result of client certificate revocation check is ignored. \"allow-unknown\" - Allow the client to authenticate even if the revocation status of his certificate cannot be determined. \"allow-valid\" - Allow the client to authenticate only when the revocation check returned an explicit positive response. </pre> ")
  public AuthenticationClientCertRevocationCheckModeEnum getAuthenticationClientCertRevocationCheckMode() {
    return authenticationClientCertRevocationCheckMode;
  }

  public void setAuthenticationClientCertRevocationCheckMode(AuthenticationClientCertRevocationCheckModeEnum authenticationClientCertRevocationCheckMode) {
    this.authenticationClientCertRevocationCheckMode = authenticationClientCertRevocationCheckMode;
  }

  public MsgVpn authenticationClientCertUsernameSource(AuthenticationClientCertUsernameSourceEnum authenticationClientCertUsernameSource) {
    this.authenticationClientCertUsernameSource = authenticationClientCertUsernameSource;
    return this;
  }

   /**
   * The field from the client certificate to use as the client username. The allowed values and their meaning are:  &lt;pre&gt; \&quot;common-name\&quot; - The username is extracted from the certificate&#x27;s Common Name. \&quot;subject-alternate-name-msupn\&quot; - The username is extracted from the certificate&#x27;s Other Name type of the Subject Alternative Name and must have the msUPN signature. &lt;/pre&gt; 
   * @return authenticationClientCertUsernameSource
  **/
  @Schema(description = "The field from the client certificate to use as the client username. The allowed values and their meaning are:  <pre> \"common-name\" - The username is extracted from the certificate's Common Name. \"subject-alternate-name-msupn\" - The username is extracted from the certificate's Other Name type of the Subject Alternative Name and must have the msUPN signature. </pre> ")
  public AuthenticationClientCertUsernameSourceEnum getAuthenticationClientCertUsernameSource() {
    return authenticationClientCertUsernameSource;
  }

  public void setAuthenticationClientCertUsernameSource(AuthenticationClientCertUsernameSourceEnum authenticationClientCertUsernameSource) {
    this.authenticationClientCertUsernameSource = authenticationClientCertUsernameSource;
  }

  public MsgVpn authenticationClientCertValidateDateEnabled(Boolean authenticationClientCertValidateDateEnabled) {
    this.authenticationClientCertValidateDateEnabled = authenticationClientCertValidateDateEnabled;
    return this;
  }

   /**
   * Indicates whether the \&quot;Not Before\&quot; and \&quot;Not After\&quot; validity dates in the client certificate are checked.
   * @return authenticationClientCertValidateDateEnabled
  **/
  @Schema(description = "Indicates whether the \"Not Before\" and \"Not After\" validity dates in the client certificate are checked.")
  public Boolean isAuthenticationClientCertValidateDateEnabled() {
    return authenticationClientCertValidateDateEnabled;
  }

  public void setAuthenticationClientCertValidateDateEnabled(Boolean authenticationClientCertValidateDateEnabled) {
    this.authenticationClientCertValidateDateEnabled = authenticationClientCertValidateDateEnabled;
  }

  public MsgVpn authenticationKerberosAllowApiProvidedUsernameEnabled(Boolean authenticationKerberosAllowApiProvidedUsernameEnabled) {
    this.authenticationKerberosAllowApiProvidedUsernameEnabled = authenticationKerberosAllowApiProvidedUsernameEnabled;
    return this;
  }

   /**
   * Indicates whether a client is allowed to specify a Client Username via the API connect method. When disabled, the Kerberos Principal name is always used.
   * @return authenticationKerberosAllowApiProvidedUsernameEnabled
  **/
  @Schema(description = "Indicates whether a client is allowed to specify a Client Username via the API connect method. When disabled, the Kerberos Principal name is always used.")
  public Boolean isAuthenticationKerberosAllowApiProvidedUsernameEnabled() {
    return authenticationKerberosAllowApiProvidedUsernameEnabled;
  }

  public void setAuthenticationKerberosAllowApiProvidedUsernameEnabled(Boolean authenticationKerberosAllowApiProvidedUsernameEnabled) {
    this.authenticationKerberosAllowApiProvidedUsernameEnabled = authenticationKerberosAllowApiProvidedUsernameEnabled;
  }

  public MsgVpn authenticationKerberosEnabled(Boolean authenticationKerberosEnabled) {
    this.authenticationKerberosEnabled = authenticationKerberosEnabled;
    return this;
  }

   /**
   * Indicates whether Kerberos authentication is enabled in the Message VPN.
   * @return authenticationKerberosEnabled
  **/
  @Schema(description = "Indicates whether Kerberos authentication is enabled in the Message VPN.")
  public Boolean isAuthenticationKerberosEnabled() {
    return authenticationKerberosEnabled;
  }

  public void setAuthenticationKerberosEnabled(Boolean authenticationKerberosEnabled) {
    this.authenticationKerberosEnabled = authenticationKerberosEnabled;
  }

  public MsgVpn authenticationOauthDefaultProviderName(String authenticationOauthDefaultProviderName) {
    this.authenticationOauthDefaultProviderName = authenticationOauthDefaultProviderName;
    return this;
  }

   /**
   * The name of the provider to use when the client does not supply a provider name. Available since 2.13.
   * @return authenticationOauthDefaultProviderName
  **/
  @Schema(description = "The name of the provider to use when the client does not supply a provider name. Available since 2.13.")
  public String getAuthenticationOauthDefaultProviderName() {
    return authenticationOauthDefaultProviderName;
  }

  public void setAuthenticationOauthDefaultProviderName(String authenticationOauthDefaultProviderName) {
    this.authenticationOauthDefaultProviderName = authenticationOauthDefaultProviderName;
  }

  public MsgVpn authenticationOauthEnabled(Boolean authenticationOauthEnabled) {
    this.authenticationOauthEnabled = authenticationOauthEnabled;
    return this;
  }

   /**
   * Indicates whether OAuth authentication is enabled. Available since 2.13.
   * @return authenticationOauthEnabled
  **/
  @Schema(description = "Indicates whether OAuth authentication is enabled. Available since 2.13.")
  public Boolean isAuthenticationOauthEnabled() {
    return authenticationOauthEnabled;
  }

  public void setAuthenticationOauthEnabled(Boolean authenticationOauthEnabled) {
    this.authenticationOauthEnabled = authenticationOauthEnabled;
  }

  public MsgVpn authorizationLdapGroupMembershipAttributeName(String authorizationLdapGroupMembershipAttributeName) {
    this.authorizationLdapGroupMembershipAttributeName = authorizationLdapGroupMembershipAttributeName;
    return this;
  }

   /**
   * The name of the attribute that is retrieved from the LDAP server as part of the LDAP search when authorizing a client connecting to the Message VPN.
   * @return authorizationLdapGroupMembershipAttributeName
  **/
  @Schema(description = "The name of the attribute that is retrieved from the LDAP server as part of the LDAP search when authorizing a client connecting to the Message VPN.")
  public String getAuthorizationLdapGroupMembershipAttributeName() {
    return authorizationLdapGroupMembershipAttributeName;
  }

  public void setAuthorizationLdapGroupMembershipAttributeName(String authorizationLdapGroupMembershipAttributeName) {
    this.authorizationLdapGroupMembershipAttributeName = authorizationLdapGroupMembershipAttributeName;
  }

  public MsgVpn authorizationLdapTrimClientUsernameDomainEnabled(Boolean authorizationLdapTrimClientUsernameDomainEnabled) {
    this.authorizationLdapTrimClientUsernameDomainEnabled = authorizationLdapTrimClientUsernameDomainEnabled;
    return this;
  }

   /**
   * Indicates whether client-username domain trimming for LDAP lookups of client connections is enabled. Available since 2.13.
   * @return authorizationLdapTrimClientUsernameDomainEnabled
  **/
  @Schema(description = "Indicates whether client-username domain trimming for LDAP lookups of client connections is enabled. Available since 2.13.")
  public Boolean isAuthorizationLdapTrimClientUsernameDomainEnabled() {
    return authorizationLdapTrimClientUsernameDomainEnabled;
  }

  public void setAuthorizationLdapTrimClientUsernameDomainEnabled(Boolean authorizationLdapTrimClientUsernameDomainEnabled) {
    this.authorizationLdapTrimClientUsernameDomainEnabled = authorizationLdapTrimClientUsernameDomainEnabled;
  }

  public MsgVpn authorizationProfileName(String authorizationProfileName) {
    this.authorizationProfileName = authorizationProfileName;
    return this;
  }

   /**
   * The name of the LDAP Profile to use for client authorization.
   * @return authorizationProfileName
  **/
  @Schema(description = "The name of the LDAP Profile to use for client authorization.")
  public String getAuthorizationProfileName() {
    return authorizationProfileName;
  }

  public void setAuthorizationProfileName(String authorizationProfileName) {
    this.authorizationProfileName = authorizationProfileName;
  }

  public MsgVpn authorizationType(AuthorizationTypeEnum authorizationType) {
    this.authorizationType = authorizationType;
    return this;
  }

   /**
   * The type of authorization to use for clients connecting to the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;ldap\&quot; - LDAP authorization. \&quot;internal\&quot; - Internal authorization. &lt;/pre&gt; 
   * @return authorizationType
  **/
  @Schema(description = "The type of authorization to use for clients connecting to the Message VPN. The allowed values and their meaning are:  <pre> \"ldap\" - LDAP authorization. \"internal\" - Internal authorization. </pre> ")
  public AuthorizationTypeEnum getAuthorizationType() {
    return authorizationType;
  }

  public void setAuthorizationType(AuthorizationTypeEnum authorizationType) {
    this.authorizationType = authorizationType;
  }

  public MsgVpn averageRxByteRate(Long averageRxByteRate) {
    this.averageRxByteRate = averageRxByteRate;
    return this;
  }

   /**
   * The one minute average of the message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.13.
   * @return averageRxByteRate
  **/
  @Schema(description = "The one minute average of the message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.13.")
  public Long getAverageRxByteRate() {
    return averageRxByteRate;
  }

  public void setAverageRxByteRate(Long averageRxByteRate) {
    this.averageRxByteRate = averageRxByteRate;
  }

  public MsgVpn averageRxCompressedByteRate(Long averageRxCompressedByteRate) {
    this.averageRxCompressedByteRate = averageRxCompressedByteRate;
    return this;
  }

   /**
   * The one minute average of the compressed message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.12.
   * @return averageRxCompressedByteRate
  **/
  @Schema(description = "The one minute average of the compressed message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.12.")
  public Long getAverageRxCompressedByteRate() {
    return averageRxCompressedByteRate;
  }

  public void setAverageRxCompressedByteRate(Long averageRxCompressedByteRate) {
    this.averageRxCompressedByteRate = averageRxCompressedByteRate;
  }

  public MsgVpn averageRxMsgRate(Long averageRxMsgRate) {
    this.averageRxMsgRate = averageRxMsgRate;
    return this;
  }

   /**
   * The one minute average of the message rate received by the Message VPN, in messages per second (msg/sec). Available since 2.13.
   * @return averageRxMsgRate
  **/
  @Schema(description = "The one minute average of the message rate received by the Message VPN, in messages per second (msg/sec). Available since 2.13.")
  public Long getAverageRxMsgRate() {
    return averageRxMsgRate;
  }

  public void setAverageRxMsgRate(Long averageRxMsgRate) {
    this.averageRxMsgRate = averageRxMsgRate;
  }

  public MsgVpn averageRxUncompressedByteRate(Long averageRxUncompressedByteRate) {
    this.averageRxUncompressedByteRate = averageRxUncompressedByteRate;
    return this;
  }

   /**
   * The one minute average of the uncompressed message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.12.
   * @return averageRxUncompressedByteRate
  **/
  @Schema(description = "The one minute average of the uncompressed message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.12.")
  public Long getAverageRxUncompressedByteRate() {
    return averageRxUncompressedByteRate;
  }

  public void setAverageRxUncompressedByteRate(Long averageRxUncompressedByteRate) {
    this.averageRxUncompressedByteRate = averageRxUncompressedByteRate;
  }

  public MsgVpn averageTxByteRate(Long averageTxByteRate) {
    this.averageTxByteRate = averageTxByteRate;
    return this;
  }

   /**
   * The one minute average of the message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.13.
   * @return averageTxByteRate
  **/
  @Schema(description = "The one minute average of the message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.13.")
  public Long getAverageTxByteRate() {
    return averageTxByteRate;
  }

  public void setAverageTxByteRate(Long averageTxByteRate) {
    this.averageTxByteRate = averageTxByteRate;
  }

  public MsgVpn averageTxCompressedByteRate(Long averageTxCompressedByteRate) {
    this.averageTxCompressedByteRate = averageTxCompressedByteRate;
    return this;
  }

   /**
   * The one minute average of the compressed message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.12.
   * @return averageTxCompressedByteRate
  **/
  @Schema(description = "The one minute average of the compressed message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.12.")
  public Long getAverageTxCompressedByteRate() {
    return averageTxCompressedByteRate;
  }

  public void setAverageTxCompressedByteRate(Long averageTxCompressedByteRate) {
    this.averageTxCompressedByteRate = averageTxCompressedByteRate;
  }

  public MsgVpn averageTxMsgRate(Long averageTxMsgRate) {
    this.averageTxMsgRate = averageTxMsgRate;
    return this;
  }

   /**
   * The one minute average of the message rate transmitted by the Message VPN, in messages per second (msg/sec). Available since 2.13.
   * @return averageTxMsgRate
  **/
  @Schema(description = "The one minute average of the message rate transmitted by the Message VPN, in messages per second (msg/sec). Available since 2.13.")
  public Long getAverageTxMsgRate() {
    return averageTxMsgRate;
  }

  public void setAverageTxMsgRate(Long averageTxMsgRate) {
    this.averageTxMsgRate = averageTxMsgRate;
  }

  public MsgVpn averageTxUncompressedByteRate(Long averageTxUncompressedByteRate) {
    this.averageTxUncompressedByteRate = averageTxUncompressedByteRate;
    return this;
  }

   /**
   * The one minute average of the uncompressed message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.12.
   * @return averageTxUncompressedByteRate
  **/
  @Schema(description = "The one minute average of the uncompressed message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.12.")
  public Long getAverageTxUncompressedByteRate() {
    return averageTxUncompressedByteRate;
  }

  public void setAverageTxUncompressedByteRate(Long averageTxUncompressedByteRate) {
    this.averageTxUncompressedByteRate = averageTxUncompressedByteRate;
  }

  public MsgVpn bridgingTlsServerCertEnforceTrustedCommonNameEnabled(Boolean bridgingTlsServerCertEnforceTrustedCommonNameEnabled) {
    this.bridgingTlsServerCertEnforceTrustedCommonNameEnabled = bridgingTlsServerCertEnforceTrustedCommonNameEnabled;
    return this;
  }

   /**
   * Indicates whether the Common Name (CN) in the server certificate from the remote broker is validated for the Bridge.
   * @return bridgingTlsServerCertEnforceTrustedCommonNameEnabled
  **/
  @Schema(description = "Indicates whether the Common Name (CN) in the server certificate from the remote broker is validated for the Bridge.")
  public Boolean isBridgingTlsServerCertEnforceTrustedCommonNameEnabled() {
    return bridgingTlsServerCertEnforceTrustedCommonNameEnabled;
  }

  public void setBridgingTlsServerCertEnforceTrustedCommonNameEnabled(Boolean bridgingTlsServerCertEnforceTrustedCommonNameEnabled) {
    this.bridgingTlsServerCertEnforceTrustedCommonNameEnabled = bridgingTlsServerCertEnforceTrustedCommonNameEnabled;
  }

  public MsgVpn bridgingTlsServerCertMaxChainDepth(Long bridgingTlsServerCertMaxChainDepth) {
    this.bridgingTlsServerCertMaxChainDepth = bridgingTlsServerCertMaxChainDepth;
    return this;
  }

   /**
   * The maximum depth for a server certificate chain. The depth of a chain is defined as the number of signing CA certificates that are present in the chain back to a trusted self-signed root CA certificate.
   * @return bridgingTlsServerCertMaxChainDepth
  **/
  @Schema(description = "The maximum depth for a server certificate chain. The depth of a chain is defined as the number of signing CA certificates that are present in the chain back to a trusted self-signed root CA certificate.")
  public Long getBridgingTlsServerCertMaxChainDepth() {
    return bridgingTlsServerCertMaxChainDepth;
  }

  public void setBridgingTlsServerCertMaxChainDepth(Long bridgingTlsServerCertMaxChainDepth) {
    this.bridgingTlsServerCertMaxChainDepth = bridgingTlsServerCertMaxChainDepth;
  }

  public MsgVpn bridgingTlsServerCertValidateDateEnabled(Boolean bridgingTlsServerCertValidateDateEnabled) {
    this.bridgingTlsServerCertValidateDateEnabled = bridgingTlsServerCertValidateDateEnabled;
    return this;
  }

   /**
   * Indicates whether the \&quot;Not Before\&quot; and \&quot;Not After\&quot; validity dates in the server certificate are checked.
   * @return bridgingTlsServerCertValidateDateEnabled
  **/
  @Schema(description = "Indicates whether the \"Not Before\" and \"Not After\" validity dates in the server certificate are checked.")
  public Boolean isBridgingTlsServerCertValidateDateEnabled() {
    return bridgingTlsServerCertValidateDateEnabled;
  }

  public void setBridgingTlsServerCertValidateDateEnabled(Boolean bridgingTlsServerCertValidateDateEnabled) {
    this.bridgingTlsServerCertValidateDateEnabled = bridgingTlsServerCertValidateDateEnabled;
  }

  public MsgVpn configSyncLocalKey(String configSyncLocalKey) {
    this.configSyncLocalKey = configSyncLocalKey;
    return this;
  }

   /**
   * The key for the config sync table of the local Message VPN. Available since 2.12.
   * @return configSyncLocalKey
  **/
  @Schema(description = "The key for the config sync table of the local Message VPN. Available since 2.12.")
  public String getConfigSyncLocalKey() {
    return configSyncLocalKey;
  }

  public void setConfigSyncLocalKey(String configSyncLocalKey) {
    this.configSyncLocalKey = configSyncLocalKey;
  }

  public MsgVpn configSyncLocalLastResult(String configSyncLocalLastResult) {
    this.configSyncLocalLastResult = configSyncLocalLastResult;
    return this;
  }

   /**
   * The result of the last operation on the config sync table of the local Message VPN. Available since 2.12.
   * @return configSyncLocalLastResult
  **/
  @Schema(description = "The result of the last operation on the config sync table of the local Message VPN. Available since 2.12.")
  public String getConfigSyncLocalLastResult() {
    return configSyncLocalLastResult;
  }

  public void setConfigSyncLocalLastResult(String configSyncLocalLastResult) {
    this.configSyncLocalLastResult = configSyncLocalLastResult;
  }

  public MsgVpn configSyncLocalRole(String configSyncLocalRole) {
    this.configSyncLocalRole = configSyncLocalRole;
    return this;
  }

   /**
   * The role of the config sync table of the local Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;unknown\&quot; - The role is unknown. \&quot;primary\&quot; - Acts as the primary source of config data. \&quot;replica\&quot; - Acts as a replica of the primary config data. &lt;/pre&gt;  Available since 2.12.
   * @return configSyncLocalRole
  **/
  @Schema(description = "The role of the config sync table of the local Message VPN. The allowed values and their meaning are:  <pre> \"unknown\" - The role is unknown. \"primary\" - Acts as the primary source of config data. \"replica\" - Acts as a replica of the primary config data. </pre>  Available since 2.12.")
  public String getConfigSyncLocalRole() {
    return configSyncLocalRole;
  }

  public void setConfigSyncLocalRole(String configSyncLocalRole) {
    this.configSyncLocalRole = configSyncLocalRole;
  }

  public MsgVpn configSyncLocalState(String configSyncLocalState) {
    this.configSyncLocalState = configSyncLocalState;
    return this;
  }

   /**
   * The state of the config sync table of the local Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;unknown\&quot; - The state is unknown. \&quot;in-sync\&quot; - The config data is synchronized between Message VPNs. \&quot;reconciling\&quot; - The config data is reconciling between Message VPNs. \&quot;blocked\&quot; - The config data is blocked from reconciling due to an error. \&quot;out-of-sync\&quot; - The config data is out of sync between Message VPNs. \&quot;down\&quot; - The state is down due to configuration. &lt;/pre&gt;  Available since 2.12.
   * @return configSyncLocalState
  **/
  @Schema(description = "The state of the config sync table of the local Message VPN. The allowed values and their meaning are:  <pre> \"unknown\" - The state is unknown. \"in-sync\" - The config data is synchronized between Message VPNs. \"reconciling\" - The config data is reconciling between Message VPNs. \"blocked\" - The config data is blocked from reconciling due to an error. \"out-of-sync\" - The config data is out of sync between Message VPNs. \"down\" - The state is down due to configuration. </pre>  Available since 2.12.")
  public String getConfigSyncLocalState() {
    return configSyncLocalState;
  }

  public void setConfigSyncLocalState(String configSyncLocalState) {
    this.configSyncLocalState = configSyncLocalState;
  }

  public MsgVpn configSyncLocalTimeInState(Integer configSyncLocalTimeInState) {
    this.configSyncLocalTimeInState = configSyncLocalTimeInState;
    return this;
  }

   /**
   * The amount of time in seconds the config sync table of the local Message VPN has been in the current state. Available since 2.12.
   * @return configSyncLocalTimeInState
  **/
  @Schema(description = "The amount of time in seconds the config sync table of the local Message VPN has been in the current state. Available since 2.12.")
  public Integer getConfigSyncLocalTimeInState() {
    return configSyncLocalTimeInState;
  }

  public void setConfigSyncLocalTimeInState(Integer configSyncLocalTimeInState) {
    this.configSyncLocalTimeInState = configSyncLocalTimeInState;
  }

  public MsgVpn controlRxByteCount(Long controlRxByteCount) {
    this.controlRxByteCount = controlRxByteCount;
    return this;
  }

   /**
   * The amount of client control messages received from clients by the Message VPN, in bytes (B). Available since 2.13.
   * @return controlRxByteCount
  **/
  @Schema(description = "The amount of client control messages received from clients by the Message VPN, in bytes (B). Available since 2.13.")
  public Long getControlRxByteCount() {
    return controlRxByteCount;
  }

  public void setControlRxByteCount(Long controlRxByteCount) {
    this.controlRxByteCount = controlRxByteCount;
  }

  public MsgVpn controlRxMsgCount(Long controlRxMsgCount) {
    this.controlRxMsgCount = controlRxMsgCount;
    return this;
  }

   /**
   * The number of client control messages received from clients by the Message VPN. Available since 2.13.
   * @return controlRxMsgCount
  **/
  @Schema(description = "The number of client control messages received from clients by the Message VPN. Available since 2.13.")
  public Long getControlRxMsgCount() {
    return controlRxMsgCount;
  }

  public void setControlRxMsgCount(Long controlRxMsgCount) {
    this.controlRxMsgCount = controlRxMsgCount;
  }

  public MsgVpn controlTxByteCount(Long controlTxByteCount) {
    this.controlTxByteCount = controlTxByteCount;
    return this;
  }

   /**
   * The amount of client control messages transmitted to clients by the Message VPN, in bytes (B). Available since 2.13.
   * @return controlTxByteCount
  **/
  @Schema(description = "The amount of client control messages transmitted to clients by the Message VPN, in bytes (B). Available since 2.13.")
  public Long getControlTxByteCount() {
    return controlTxByteCount;
  }

  public void setControlTxByteCount(Long controlTxByteCount) {
    this.controlTxByteCount = controlTxByteCount;
  }

  public MsgVpn controlTxMsgCount(Long controlTxMsgCount) {
    this.controlTxMsgCount = controlTxMsgCount;
    return this;
  }

   /**
   * The number of client control messages transmitted to clients by the Message VPN. Available since 2.13.
   * @return controlTxMsgCount
  **/
  @Schema(description = "The number of client control messages transmitted to clients by the Message VPN. Available since 2.13.")
  public Long getControlTxMsgCount() {
    return controlTxMsgCount;
  }

  public void setControlTxMsgCount(Long controlTxMsgCount) {
    this.controlTxMsgCount = controlTxMsgCount;
  }

  public MsgVpn counter(MsgVpnCounter counter) {
    this.counter = counter;
    return this;
  }

   /**
   * Get counter
   * @return counter
  **/
  @Schema(description = "")
  public MsgVpnCounter getCounter() {
    return counter;
  }

  public void setCounter(MsgVpnCounter counter) {
    this.counter = counter;
  }

  public MsgVpn dataRxByteCount(Long dataRxByteCount) {
    this.dataRxByteCount = dataRxByteCount;
    return this;
  }

   /**
   * The amount of client data messages received from clients by the Message VPN, in bytes (B). Available since 2.13.
   * @return dataRxByteCount
  **/
  @Schema(description = "The amount of client data messages received from clients by the Message VPN, in bytes (B). Available since 2.13.")
  public Long getDataRxByteCount() {
    return dataRxByteCount;
  }

  public void setDataRxByteCount(Long dataRxByteCount) {
    this.dataRxByteCount = dataRxByteCount;
  }

  public MsgVpn dataRxMsgCount(Long dataRxMsgCount) {
    this.dataRxMsgCount = dataRxMsgCount;
    return this;
  }

   /**
   * The number of client data messages received from clients by the Message VPN. Available since 2.13.
   * @return dataRxMsgCount
  **/
  @Schema(description = "The number of client data messages received from clients by the Message VPN. Available since 2.13.")
  public Long getDataRxMsgCount() {
    return dataRxMsgCount;
  }

  public void setDataRxMsgCount(Long dataRxMsgCount) {
    this.dataRxMsgCount = dataRxMsgCount;
  }

  public MsgVpn dataTxByteCount(Long dataTxByteCount) {
    this.dataTxByteCount = dataTxByteCount;
    return this;
  }

   /**
   * The amount of client data messages transmitted to clients by the Message VPN, in bytes (B). Available since 2.13.
   * @return dataTxByteCount
  **/
  @Schema(description = "The amount of client data messages transmitted to clients by the Message VPN, in bytes (B). Available since 2.13.")
  public Long getDataTxByteCount() {
    return dataTxByteCount;
  }

  public void setDataTxByteCount(Long dataTxByteCount) {
    this.dataTxByteCount = dataTxByteCount;
  }

  public MsgVpn dataTxMsgCount(Long dataTxMsgCount) {
    this.dataTxMsgCount = dataTxMsgCount;
    return this;
  }

   /**
   * The number of client data messages transmitted to clients by the Message VPN. Available since 2.13.
   * @return dataTxMsgCount
  **/
  @Schema(description = "The number of client data messages transmitted to clients by the Message VPN. Available since 2.13.")
  public Long getDataTxMsgCount() {
    return dataTxMsgCount;
  }

  public void setDataTxMsgCount(Long dataTxMsgCount) {
    this.dataTxMsgCount = dataTxMsgCount;
  }

  public MsgVpn discardedRxMsgCount(Integer discardedRxMsgCount) {
    this.discardedRxMsgCount = discardedRxMsgCount;
    return this;
  }

   /**
   * The number of messages discarded during reception by the Message VPN. Available since 2.13.
   * @return discardedRxMsgCount
  **/
  @Schema(description = "The number of messages discarded during reception by the Message VPN. Available since 2.13.")
  public Integer getDiscardedRxMsgCount() {
    return discardedRxMsgCount;
  }

  public void setDiscardedRxMsgCount(Integer discardedRxMsgCount) {
    this.discardedRxMsgCount = discardedRxMsgCount;
  }

  public MsgVpn discardedTxMsgCount(Integer discardedTxMsgCount) {
    this.discardedTxMsgCount = discardedTxMsgCount;
    return this;
  }

   /**
   * The number of messages discarded during transmission by the Message VPN. Available since 2.13.
   * @return discardedTxMsgCount
  **/
  @Schema(description = "The number of messages discarded during transmission by the Message VPN. Available since 2.13.")
  public Integer getDiscardedTxMsgCount() {
    return discardedTxMsgCount;
  }

  public void setDiscardedTxMsgCount(Integer discardedTxMsgCount) {
    this.discardedTxMsgCount = discardedTxMsgCount;
  }

  public MsgVpn distributedCacheManagementEnabled(Boolean distributedCacheManagementEnabled) {
    this.distributedCacheManagementEnabled = distributedCacheManagementEnabled;
    return this;
  }

   /**
   * Indicates whether managing of cache instances over the message bus is enabled in the Message VPN.
   * @return distributedCacheManagementEnabled
  **/
  @Schema(description = "Indicates whether managing of cache instances over the message bus is enabled in the Message VPN.")
  public Boolean isDistributedCacheManagementEnabled() {
    return distributedCacheManagementEnabled;
  }

  public void setDistributedCacheManagementEnabled(Boolean distributedCacheManagementEnabled) {
    this.distributedCacheManagementEnabled = distributedCacheManagementEnabled;
  }

  public MsgVpn dmrEnabled(Boolean dmrEnabled) {
    this.dmrEnabled = dmrEnabled;
    return this;
  }

   /**
   * Indicates whether Dynamic Message Routing (DMR) is enabled for the Message VPN.
   * @return dmrEnabled
  **/
  @Schema(description = "Indicates whether Dynamic Message Routing (DMR) is enabled for the Message VPN.")
  public Boolean isDmrEnabled() {
    return dmrEnabled;
  }

  public void setDmrEnabled(Boolean dmrEnabled) {
    this.dmrEnabled = dmrEnabled;
  }

  public MsgVpn enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

   /**
   * Indicates whether the Message VPN is enabled.
   * @return enabled
  **/
  @Schema(description = "Indicates whether the Message VPN is enabled.")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public MsgVpn eventConnectionCountThreshold(EventThreshold eventConnectionCountThreshold) {
    this.eventConnectionCountThreshold = eventConnectionCountThreshold;
    return this;
  }

   /**
   * Get eventConnectionCountThreshold
   * @return eventConnectionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventConnectionCountThreshold() {
    return eventConnectionCountThreshold;
  }

  public void setEventConnectionCountThreshold(EventThreshold eventConnectionCountThreshold) {
    this.eventConnectionCountThreshold = eventConnectionCountThreshold;
  }

  public MsgVpn eventEgressFlowCountThreshold(EventThreshold eventEgressFlowCountThreshold) {
    this.eventEgressFlowCountThreshold = eventEgressFlowCountThreshold;
    return this;
  }

   /**
   * Get eventEgressFlowCountThreshold
   * @return eventEgressFlowCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventEgressFlowCountThreshold() {
    return eventEgressFlowCountThreshold;
  }

  public void setEventEgressFlowCountThreshold(EventThreshold eventEgressFlowCountThreshold) {
    this.eventEgressFlowCountThreshold = eventEgressFlowCountThreshold;
  }

  public MsgVpn eventEgressMsgRateThreshold(EventThresholdByValue eventEgressMsgRateThreshold) {
    this.eventEgressMsgRateThreshold = eventEgressMsgRateThreshold;
    return this;
  }

   /**
   * Get eventEgressMsgRateThreshold
   * @return eventEgressMsgRateThreshold
  **/
  @Schema(description = "")
  public EventThresholdByValue getEventEgressMsgRateThreshold() {
    return eventEgressMsgRateThreshold;
  }

  public void setEventEgressMsgRateThreshold(EventThresholdByValue eventEgressMsgRateThreshold) {
    this.eventEgressMsgRateThreshold = eventEgressMsgRateThreshold;
  }

  public MsgVpn eventEndpointCountThreshold(EventThreshold eventEndpointCountThreshold) {
    this.eventEndpointCountThreshold = eventEndpointCountThreshold;
    return this;
  }

   /**
   * Get eventEndpointCountThreshold
   * @return eventEndpointCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventEndpointCountThreshold() {
    return eventEndpointCountThreshold;
  }

  public void setEventEndpointCountThreshold(EventThreshold eventEndpointCountThreshold) {
    this.eventEndpointCountThreshold = eventEndpointCountThreshold;
  }

  public MsgVpn eventIngressFlowCountThreshold(EventThreshold eventIngressFlowCountThreshold) {
    this.eventIngressFlowCountThreshold = eventIngressFlowCountThreshold;
    return this;
  }

   /**
   * Get eventIngressFlowCountThreshold
   * @return eventIngressFlowCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventIngressFlowCountThreshold() {
    return eventIngressFlowCountThreshold;
  }

  public void setEventIngressFlowCountThreshold(EventThreshold eventIngressFlowCountThreshold) {
    this.eventIngressFlowCountThreshold = eventIngressFlowCountThreshold;
  }

  public MsgVpn eventIngressMsgRateThreshold(EventThresholdByValue eventIngressMsgRateThreshold) {
    this.eventIngressMsgRateThreshold = eventIngressMsgRateThreshold;
    return this;
  }

   /**
   * Get eventIngressMsgRateThreshold
   * @return eventIngressMsgRateThreshold
  **/
  @Schema(description = "")
  public EventThresholdByValue getEventIngressMsgRateThreshold() {
    return eventIngressMsgRateThreshold;
  }

  public void setEventIngressMsgRateThreshold(EventThresholdByValue eventIngressMsgRateThreshold) {
    this.eventIngressMsgRateThreshold = eventIngressMsgRateThreshold;
  }

  public MsgVpn eventLargeMsgThreshold(Long eventLargeMsgThreshold) {
    this.eventLargeMsgThreshold = eventLargeMsgThreshold;
    return this;
  }

   /**
   * Exceeding this message size in kilobytes (KB) triggers a corresponding Event in the Message VPN.
   * @return eventLargeMsgThreshold
  **/
  @Schema(description = "Exceeding this message size in kilobytes (KB) triggers a corresponding Event in the Message VPN.")
  public Long getEventLargeMsgThreshold() {
    return eventLargeMsgThreshold;
  }

  public void setEventLargeMsgThreshold(Long eventLargeMsgThreshold) {
    this.eventLargeMsgThreshold = eventLargeMsgThreshold;
  }

  public MsgVpn eventLogTag(String eventLogTag) {
    this.eventLogTag = eventLogTag;
    return this;
  }

   /**
   * The value of the prefix applied to all published Events in the Message VPN.
   * @return eventLogTag
  **/
  @Schema(description = "The value of the prefix applied to all published Events in the Message VPN.")
  public String getEventLogTag() {
    return eventLogTag;
  }

  public void setEventLogTag(String eventLogTag) {
    this.eventLogTag = eventLogTag;
  }

  public MsgVpn eventMsgSpoolUsageThreshold(EventThreshold eventMsgSpoolUsageThreshold) {
    this.eventMsgSpoolUsageThreshold = eventMsgSpoolUsageThreshold;
    return this;
  }

   /**
   * Get eventMsgSpoolUsageThreshold
   * @return eventMsgSpoolUsageThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventMsgSpoolUsageThreshold() {
    return eventMsgSpoolUsageThreshold;
  }

  public void setEventMsgSpoolUsageThreshold(EventThreshold eventMsgSpoolUsageThreshold) {
    this.eventMsgSpoolUsageThreshold = eventMsgSpoolUsageThreshold;
  }

  public MsgVpn eventPublishClientEnabled(Boolean eventPublishClientEnabled) {
    this.eventPublishClientEnabled = eventPublishClientEnabled;
    return this;
  }

   /**
   * Indicates whether client Events are published in the Message VPN.
   * @return eventPublishClientEnabled
  **/
  @Schema(description = "Indicates whether client Events are published in the Message VPN.")
  public Boolean isEventPublishClientEnabled() {
    return eventPublishClientEnabled;
  }

  public void setEventPublishClientEnabled(Boolean eventPublishClientEnabled) {
    this.eventPublishClientEnabled = eventPublishClientEnabled;
  }

  public MsgVpn eventPublishMsgVpnEnabled(Boolean eventPublishMsgVpnEnabled) {
    this.eventPublishMsgVpnEnabled = eventPublishMsgVpnEnabled;
    return this;
  }

   /**
   * Indicates whether Message VPN Events are published in the Message VPN.
   * @return eventPublishMsgVpnEnabled
  **/
  @Schema(description = "Indicates whether Message VPN Events are published in the Message VPN.")
  public Boolean isEventPublishMsgVpnEnabled() {
    return eventPublishMsgVpnEnabled;
  }

  public void setEventPublishMsgVpnEnabled(Boolean eventPublishMsgVpnEnabled) {
    this.eventPublishMsgVpnEnabled = eventPublishMsgVpnEnabled;
  }

  public MsgVpn eventPublishSubscriptionMode(EventPublishSubscriptionModeEnum eventPublishSubscriptionMode) {
    this.eventPublishSubscriptionMode = eventPublishSubscriptionMode;
    return this;
  }

   /**
   * The mode of subscription Events published in the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;off\&quot; - Disable client level event message publishing. \&quot;on-with-format-v1\&quot; - Enable client level event message publishing with format v1. \&quot;on-with-no-unsubscribe-events-on-disconnect-format-v1\&quot; - As \&quot;on-with-format-v1\&quot;, but unsubscribe events are not generated when a client disconnects. Unsubscribe events are still raised when a client explicitly unsubscribes from its subscriptions. \&quot;on-with-format-v2\&quot; - Enable client level event message publishing with format v2. \&quot;on-with-no-unsubscribe-events-on-disconnect-format-v2\&quot; - As \&quot;on-with-format-v2\&quot;, but unsubscribe events are not generated when a client disconnects. Unsubscribe events are still raised when a client explicitly unsubscribes from its subscriptions. &lt;/pre&gt; 
   * @return eventPublishSubscriptionMode
  **/
  @Schema(description = "The mode of subscription Events published in the Message VPN. The allowed values and their meaning are:  <pre> \"off\" - Disable client level event message publishing. \"on-with-format-v1\" - Enable client level event message publishing with format v1. \"on-with-no-unsubscribe-events-on-disconnect-format-v1\" - As \"on-with-format-v1\", but unsubscribe events are not generated when a client disconnects. Unsubscribe events are still raised when a client explicitly unsubscribes from its subscriptions. \"on-with-format-v2\" - Enable client level event message publishing with format v2. \"on-with-no-unsubscribe-events-on-disconnect-format-v2\" - As \"on-with-format-v2\", but unsubscribe events are not generated when a client disconnects. Unsubscribe events are still raised when a client explicitly unsubscribes from its subscriptions. </pre> ")
  public EventPublishSubscriptionModeEnum getEventPublishSubscriptionMode() {
    return eventPublishSubscriptionMode;
  }

  public void setEventPublishSubscriptionMode(EventPublishSubscriptionModeEnum eventPublishSubscriptionMode) {
    this.eventPublishSubscriptionMode = eventPublishSubscriptionMode;
  }

  public MsgVpn eventPublishTopicFormatMqttEnabled(Boolean eventPublishTopicFormatMqttEnabled) {
    this.eventPublishTopicFormatMqttEnabled = eventPublishTopicFormatMqttEnabled;
    return this;
  }

   /**
   * Indicates whether Message VPN Events are published in the MQTT format.
   * @return eventPublishTopicFormatMqttEnabled
  **/
  @Schema(description = "Indicates whether Message VPN Events are published in the MQTT format.")
  public Boolean isEventPublishTopicFormatMqttEnabled() {
    return eventPublishTopicFormatMqttEnabled;
  }

  public void setEventPublishTopicFormatMqttEnabled(Boolean eventPublishTopicFormatMqttEnabled) {
    this.eventPublishTopicFormatMqttEnabled = eventPublishTopicFormatMqttEnabled;
  }

  public MsgVpn eventPublishTopicFormatSmfEnabled(Boolean eventPublishTopicFormatSmfEnabled) {
    this.eventPublishTopicFormatSmfEnabled = eventPublishTopicFormatSmfEnabled;
    return this;
  }

   /**
   * Indicates whether Message VPN Events are published in the SMF format.
   * @return eventPublishTopicFormatSmfEnabled
  **/
  @Schema(description = "Indicates whether Message VPN Events are published in the SMF format.")
  public Boolean isEventPublishTopicFormatSmfEnabled() {
    return eventPublishTopicFormatSmfEnabled;
  }

  public void setEventPublishTopicFormatSmfEnabled(Boolean eventPublishTopicFormatSmfEnabled) {
    this.eventPublishTopicFormatSmfEnabled = eventPublishTopicFormatSmfEnabled;
  }

  public MsgVpn eventServiceAmqpConnectionCountThreshold(EventThreshold eventServiceAmqpConnectionCountThreshold) {
    this.eventServiceAmqpConnectionCountThreshold = eventServiceAmqpConnectionCountThreshold;
    return this;
  }

   /**
   * Get eventServiceAmqpConnectionCountThreshold
   * @return eventServiceAmqpConnectionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventServiceAmqpConnectionCountThreshold() {
    return eventServiceAmqpConnectionCountThreshold;
  }

  public void setEventServiceAmqpConnectionCountThreshold(EventThreshold eventServiceAmqpConnectionCountThreshold) {
    this.eventServiceAmqpConnectionCountThreshold = eventServiceAmqpConnectionCountThreshold;
  }

  public MsgVpn eventServiceMqttConnectionCountThreshold(EventThreshold eventServiceMqttConnectionCountThreshold) {
    this.eventServiceMqttConnectionCountThreshold = eventServiceMqttConnectionCountThreshold;
    return this;
  }

   /**
   * Get eventServiceMqttConnectionCountThreshold
   * @return eventServiceMqttConnectionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventServiceMqttConnectionCountThreshold() {
    return eventServiceMqttConnectionCountThreshold;
  }

  public void setEventServiceMqttConnectionCountThreshold(EventThreshold eventServiceMqttConnectionCountThreshold) {
    this.eventServiceMqttConnectionCountThreshold = eventServiceMqttConnectionCountThreshold;
  }

  public MsgVpn eventServiceRestIncomingConnectionCountThreshold(EventThreshold eventServiceRestIncomingConnectionCountThreshold) {
    this.eventServiceRestIncomingConnectionCountThreshold = eventServiceRestIncomingConnectionCountThreshold;
    return this;
  }

   /**
   * Get eventServiceRestIncomingConnectionCountThreshold
   * @return eventServiceRestIncomingConnectionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventServiceRestIncomingConnectionCountThreshold() {
    return eventServiceRestIncomingConnectionCountThreshold;
  }

  public void setEventServiceRestIncomingConnectionCountThreshold(EventThreshold eventServiceRestIncomingConnectionCountThreshold) {
    this.eventServiceRestIncomingConnectionCountThreshold = eventServiceRestIncomingConnectionCountThreshold;
  }

  public MsgVpn eventServiceSmfConnectionCountThreshold(EventThreshold eventServiceSmfConnectionCountThreshold) {
    this.eventServiceSmfConnectionCountThreshold = eventServiceSmfConnectionCountThreshold;
    return this;
  }

   /**
   * Get eventServiceSmfConnectionCountThreshold
   * @return eventServiceSmfConnectionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventServiceSmfConnectionCountThreshold() {
    return eventServiceSmfConnectionCountThreshold;
  }

  public void setEventServiceSmfConnectionCountThreshold(EventThreshold eventServiceSmfConnectionCountThreshold) {
    this.eventServiceSmfConnectionCountThreshold = eventServiceSmfConnectionCountThreshold;
  }

  public MsgVpn eventServiceWebConnectionCountThreshold(EventThreshold eventServiceWebConnectionCountThreshold) {
    this.eventServiceWebConnectionCountThreshold = eventServiceWebConnectionCountThreshold;
    return this;
  }

   /**
   * Get eventServiceWebConnectionCountThreshold
   * @return eventServiceWebConnectionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventServiceWebConnectionCountThreshold() {
    return eventServiceWebConnectionCountThreshold;
  }

  public void setEventServiceWebConnectionCountThreshold(EventThreshold eventServiceWebConnectionCountThreshold) {
    this.eventServiceWebConnectionCountThreshold = eventServiceWebConnectionCountThreshold;
  }

  public MsgVpn eventSubscriptionCountThreshold(EventThreshold eventSubscriptionCountThreshold) {
    this.eventSubscriptionCountThreshold = eventSubscriptionCountThreshold;
    return this;
  }

   /**
   * Get eventSubscriptionCountThreshold
   * @return eventSubscriptionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventSubscriptionCountThreshold() {
    return eventSubscriptionCountThreshold;
  }

  public void setEventSubscriptionCountThreshold(EventThreshold eventSubscriptionCountThreshold) {
    this.eventSubscriptionCountThreshold = eventSubscriptionCountThreshold;
  }

  public MsgVpn eventTransactedSessionCountThreshold(EventThreshold eventTransactedSessionCountThreshold) {
    this.eventTransactedSessionCountThreshold = eventTransactedSessionCountThreshold;
    return this;
  }

   /**
   * Get eventTransactedSessionCountThreshold
   * @return eventTransactedSessionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventTransactedSessionCountThreshold() {
    return eventTransactedSessionCountThreshold;
  }

  public void setEventTransactedSessionCountThreshold(EventThreshold eventTransactedSessionCountThreshold) {
    this.eventTransactedSessionCountThreshold = eventTransactedSessionCountThreshold;
  }

  public MsgVpn eventTransactionCountThreshold(EventThreshold eventTransactionCountThreshold) {
    this.eventTransactionCountThreshold = eventTransactionCountThreshold;
    return this;
  }

   /**
   * Get eventTransactionCountThreshold
   * @return eventTransactionCountThreshold
  **/
  @Schema(description = "")
  public EventThreshold getEventTransactionCountThreshold() {
    return eventTransactionCountThreshold;
  }

  public void setEventTransactionCountThreshold(EventThreshold eventTransactionCountThreshold) {
    this.eventTransactionCountThreshold = eventTransactionCountThreshold;
  }

  public MsgVpn exportSubscriptionsEnabled(Boolean exportSubscriptionsEnabled) {
    this.exportSubscriptionsEnabled = exportSubscriptionsEnabled;
    return this;
  }

   /**
   * Indicates whether exports of subscriptions to other routers in the network over neighbour links is enabled in the Message VPN.
   * @return exportSubscriptionsEnabled
  **/
  @Schema(description = "Indicates whether exports of subscriptions to other routers in the network over neighbour links is enabled in the Message VPN.")
  public Boolean isExportSubscriptionsEnabled() {
    return exportSubscriptionsEnabled;
  }

  public void setExportSubscriptionsEnabled(Boolean exportSubscriptionsEnabled) {
    this.exportSubscriptionsEnabled = exportSubscriptionsEnabled;
  }

  public MsgVpn failureReason(String failureReason) {
    this.failureReason = failureReason;
    return this;
  }

   /**
   * The reason for the Message VPN failure.
   * @return failureReason
  **/
  @Schema(description = "The reason for the Message VPN failure.")
  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public MsgVpn jndiEnabled(Boolean jndiEnabled) {
    this.jndiEnabled = jndiEnabled;
    return this;
  }

   /**
   * Indicates whether the JNDI access for clients is enabled in the Message VPN.
   * @return jndiEnabled
  **/
  @Schema(description = "Indicates whether the JNDI access for clients is enabled in the Message VPN.")
  public Boolean isJndiEnabled() {
    return jndiEnabled;
  }

  public void setJndiEnabled(Boolean jndiEnabled) {
    this.jndiEnabled = jndiEnabled;
  }

  public MsgVpn loginRxMsgCount(Long loginRxMsgCount) {
    this.loginRxMsgCount = loginRxMsgCount;
    return this;
  }

   /**
   * The number of login request messages received by the Message VPN. Available since 2.13.
   * @return loginRxMsgCount
  **/
  @Schema(description = "The number of login request messages received by the Message VPN. Available since 2.13.")
  public Long getLoginRxMsgCount() {
    return loginRxMsgCount;
  }

  public void setLoginRxMsgCount(Long loginRxMsgCount) {
    this.loginRxMsgCount = loginRxMsgCount;
  }

  public MsgVpn loginTxMsgCount(Long loginTxMsgCount) {
    this.loginTxMsgCount = loginTxMsgCount;
    return this;
  }

   /**
   * The number of login response messages transmitted by the Message VPN. Available since 2.13.
   * @return loginTxMsgCount
  **/
  @Schema(description = "The number of login response messages transmitted by the Message VPN. Available since 2.13.")
  public Long getLoginTxMsgCount() {
    return loginTxMsgCount;
  }

  public void setLoginTxMsgCount(Long loginTxMsgCount) {
    this.loginTxMsgCount = loginTxMsgCount;
  }

  public MsgVpn maxConnectionCount(Long maxConnectionCount) {
    this.maxConnectionCount = maxConnectionCount;
    return this;
  }

   /**
   * The maximum number of client connections to the Message VPN.
   * @return maxConnectionCount
  **/
  @Schema(description = "The maximum number of client connections to the Message VPN.")
  public Long getMaxConnectionCount() {
    return maxConnectionCount;
  }

  public void setMaxConnectionCount(Long maxConnectionCount) {
    this.maxConnectionCount = maxConnectionCount;
  }

  public MsgVpn maxEffectiveEndpointCount(Integer maxEffectiveEndpointCount) {
    this.maxEffectiveEndpointCount = maxEffectiveEndpointCount;
    return this;
  }

   /**
   * The effective maximum number of Queues and Topic Endpoints allowed in the Message VPN.
   * @return maxEffectiveEndpointCount
  **/
  @Schema(description = "The effective maximum number of Queues and Topic Endpoints allowed in the Message VPN.")
  public Integer getMaxEffectiveEndpointCount() {
    return maxEffectiveEndpointCount;
  }

  public void setMaxEffectiveEndpointCount(Integer maxEffectiveEndpointCount) {
    this.maxEffectiveEndpointCount = maxEffectiveEndpointCount;
  }

  public MsgVpn maxEffectiveRxFlowCount(Integer maxEffectiveRxFlowCount) {
    this.maxEffectiveRxFlowCount = maxEffectiveRxFlowCount;
    return this;
  }

   /**
   * The effective maximum number of receive flows allowed in the Message VPN.
   * @return maxEffectiveRxFlowCount
  **/
  @Schema(description = "The effective maximum number of receive flows allowed in the Message VPN.")
  public Integer getMaxEffectiveRxFlowCount() {
    return maxEffectiveRxFlowCount;
  }

  public void setMaxEffectiveRxFlowCount(Integer maxEffectiveRxFlowCount) {
    this.maxEffectiveRxFlowCount = maxEffectiveRxFlowCount;
  }

  public MsgVpn maxEffectiveSubscriptionCount(Long maxEffectiveSubscriptionCount) {
    this.maxEffectiveSubscriptionCount = maxEffectiveSubscriptionCount;
    return this;
  }

   /**
   * The effective maximum number of subscriptions allowed in the Message VPN.
   * @return maxEffectiveSubscriptionCount
  **/
  @Schema(description = "The effective maximum number of subscriptions allowed in the Message VPN.")
  public Long getMaxEffectiveSubscriptionCount() {
    return maxEffectiveSubscriptionCount;
  }

  public void setMaxEffectiveSubscriptionCount(Long maxEffectiveSubscriptionCount) {
    this.maxEffectiveSubscriptionCount = maxEffectiveSubscriptionCount;
  }

  public MsgVpn maxEffectiveTransactedSessionCount(Integer maxEffectiveTransactedSessionCount) {
    this.maxEffectiveTransactedSessionCount = maxEffectiveTransactedSessionCount;
    return this;
  }

   /**
   * The effective maximum number of transacted sessions allowed in the Message VPN.
   * @return maxEffectiveTransactedSessionCount
  **/
  @Schema(description = "The effective maximum number of transacted sessions allowed in the Message VPN.")
  public Integer getMaxEffectiveTransactedSessionCount() {
    return maxEffectiveTransactedSessionCount;
  }

  public void setMaxEffectiveTransactedSessionCount(Integer maxEffectiveTransactedSessionCount) {
    this.maxEffectiveTransactedSessionCount = maxEffectiveTransactedSessionCount;
  }

  public MsgVpn maxEffectiveTransactionCount(Integer maxEffectiveTransactionCount) {
    this.maxEffectiveTransactionCount = maxEffectiveTransactionCount;
    return this;
  }

   /**
   * The effective maximum number of transactions allowed in the Message VPN.
   * @return maxEffectiveTransactionCount
  **/
  @Schema(description = "The effective maximum number of transactions allowed in the Message VPN.")
  public Integer getMaxEffectiveTransactionCount() {
    return maxEffectiveTransactionCount;
  }

  public void setMaxEffectiveTransactionCount(Integer maxEffectiveTransactionCount) {
    this.maxEffectiveTransactionCount = maxEffectiveTransactionCount;
  }

  public MsgVpn maxEffectiveTxFlowCount(Integer maxEffectiveTxFlowCount) {
    this.maxEffectiveTxFlowCount = maxEffectiveTxFlowCount;
    return this;
  }

   /**
   * The effective maximum number of transmit flows allowed in the Message VPN.
   * @return maxEffectiveTxFlowCount
  **/
  @Schema(description = "The effective maximum number of transmit flows allowed in the Message VPN.")
  public Integer getMaxEffectiveTxFlowCount() {
    return maxEffectiveTxFlowCount;
  }

  public void setMaxEffectiveTxFlowCount(Integer maxEffectiveTxFlowCount) {
    this.maxEffectiveTxFlowCount = maxEffectiveTxFlowCount;
  }

  public MsgVpn maxEgressFlowCount(Long maxEgressFlowCount) {
    this.maxEgressFlowCount = maxEgressFlowCount;
    return this;
  }

   /**
   * The maximum number of transmit flows that can be created in the Message VPN.
   * @return maxEgressFlowCount
  **/
  @Schema(description = "The maximum number of transmit flows that can be created in the Message VPN.")
  public Long getMaxEgressFlowCount() {
    return maxEgressFlowCount;
  }

  public void setMaxEgressFlowCount(Long maxEgressFlowCount) {
    this.maxEgressFlowCount = maxEgressFlowCount;
  }

  public MsgVpn maxEndpointCount(Long maxEndpointCount) {
    this.maxEndpointCount = maxEndpointCount;
    return this;
  }

   /**
   * The maximum number of Queues and Topic Endpoints that can be created in the Message VPN.
   * @return maxEndpointCount
  **/
  @Schema(description = "The maximum number of Queues and Topic Endpoints that can be created in the Message VPN.")
  public Long getMaxEndpointCount() {
    return maxEndpointCount;
  }

  public void setMaxEndpointCount(Long maxEndpointCount) {
    this.maxEndpointCount = maxEndpointCount;
  }

  public MsgVpn maxIngressFlowCount(Long maxIngressFlowCount) {
    this.maxIngressFlowCount = maxIngressFlowCount;
    return this;
  }

   /**
   * The maximum number of receive flows that can be created in the Message VPN.
   * @return maxIngressFlowCount
  **/
  @Schema(description = "The maximum number of receive flows that can be created in the Message VPN.")
  public Long getMaxIngressFlowCount() {
    return maxIngressFlowCount;
  }

  public void setMaxIngressFlowCount(Long maxIngressFlowCount) {
    this.maxIngressFlowCount = maxIngressFlowCount;
  }

  public MsgVpn maxMsgSpoolUsage(Long maxMsgSpoolUsage) {
    this.maxMsgSpoolUsage = maxMsgSpoolUsage;
    return this;
  }

   /**
   * The maximum message spool usage by the Message VPN, in megabytes.
   * @return maxMsgSpoolUsage
  **/
  @Schema(description = "The maximum message spool usage by the Message VPN, in megabytes.")
  public Long getMaxMsgSpoolUsage() {
    return maxMsgSpoolUsage;
  }

  public void setMaxMsgSpoolUsage(Long maxMsgSpoolUsage) {
    this.maxMsgSpoolUsage = maxMsgSpoolUsage;
  }

  public MsgVpn maxSubscriptionCount(Long maxSubscriptionCount) {
    this.maxSubscriptionCount = maxSubscriptionCount;
    return this;
  }

   /**
   * The maximum number of local client subscriptions (both primary and backup) that can be added to the Message VPN.
   * @return maxSubscriptionCount
  **/
  @Schema(description = "The maximum number of local client subscriptions (both primary and backup) that can be added to the Message VPN.")
  public Long getMaxSubscriptionCount() {
    return maxSubscriptionCount;
  }

  public void setMaxSubscriptionCount(Long maxSubscriptionCount) {
    this.maxSubscriptionCount = maxSubscriptionCount;
  }

  public MsgVpn maxTransactedSessionCount(Long maxTransactedSessionCount) {
    this.maxTransactedSessionCount = maxTransactedSessionCount;
    return this;
  }

   /**
   * The maximum number of transacted sessions that can be created in the Message VPN.
   * @return maxTransactedSessionCount
  **/
  @Schema(description = "The maximum number of transacted sessions that can be created in the Message VPN.")
  public Long getMaxTransactedSessionCount() {
    return maxTransactedSessionCount;
  }

  public void setMaxTransactedSessionCount(Long maxTransactedSessionCount) {
    this.maxTransactedSessionCount = maxTransactedSessionCount;
  }

  public MsgVpn maxTransactionCount(Long maxTransactionCount) {
    this.maxTransactionCount = maxTransactionCount;
    return this;
  }

   /**
   * The maximum number of transactions that can be created in the Message VPN.
   * @return maxTransactionCount
  **/
  @Schema(description = "The maximum number of transactions that can be created in the Message VPN.")
  public Long getMaxTransactionCount() {
    return maxTransactionCount;
  }

  public void setMaxTransactionCount(Long maxTransactionCount) {
    this.maxTransactionCount = maxTransactionCount;
  }

  public MsgVpn mqttRetainMaxMemory(Integer mqttRetainMaxMemory) {
    this.mqttRetainMaxMemory = mqttRetainMaxMemory;
    return this;
  }

   /**
   * The maximum total memory usage of the MQTT Retain feature for this Message VPN, in MB. If the maximum memory is reached, any arriving retain messages that require more memory are discarded. A value of -1 indicates that the memory is bounded only by the global max memory limit. A value of 0 prevents MQTT Retain from becoming operational.
   * @return mqttRetainMaxMemory
  **/
  @Schema(description = "The maximum total memory usage of the MQTT Retain feature for this Message VPN, in MB. If the maximum memory is reached, any arriving retain messages that require more memory are discarded. A value of -1 indicates that the memory is bounded only by the global max memory limit. A value of 0 prevents MQTT Retain from becoming operational.")
  public Integer getMqttRetainMaxMemory() {
    return mqttRetainMaxMemory;
  }

  public void setMqttRetainMaxMemory(Integer mqttRetainMaxMemory) {
    this.mqttRetainMaxMemory = mqttRetainMaxMemory;
  }

  public MsgVpn msgReplayActiveCount(Integer msgReplayActiveCount) {
    this.msgReplayActiveCount = msgReplayActiveCount;
    return this;
  }

   /**
   * The number of message replays that are currently active in the Message VPN.
   * @return msgReplayActiveCount
  **/
  @Schema(description = "The number of message replays that are currently active in the Message VPN.")
  public Integer getMsgReplayActiveCount() {
    return msgReplayActiveCount;
  }

  public void setMsgReplayActiveCount(Integer msgReplayActiveCount) {
    this.msgReplayActiveCount = msgReplayActiveCount;
  }

  public MsgVpn msgReplayFailedCount(Integer msgReplayFailedCount) {
    this.msgReplayFailedCount = msgReplayFailedCount;
    return this;
  }

   /**
   * The number of message replays that are currently failed in the Message VPN.
   * @return msgReplayFailedCount
  **/
  @Schema(description = "The number of message replays that are currently failed in the Message VPN.")
  public Integer getMsgReplayFailedCount() {
    return msgReplayFailedCount;
  }

  public void setMsgReplayFailedCount(Integer msgReplayFailedCount) {
    this.msgReplayFailedCount = msgReplayFailedCount;
  }

  public MsgVpn msgReplayInitializingCount(Integer msgReplayInitializingCount) {
    this.msgReplayInitializingCount = msgReplayInitializingCount;
    return this;
  }

   /**
   * The number of message replays that are currently initializing in the Message VPN.
   * @return msgReplayInitializingCount
  **/
  @Schema(description = "The number of message replays that are currently initializing in the Message VPN.")
  public Integer getMsgReplayInitializingCount() {
    return msgReplayInitializingCount;
  }

  public void setMsgReplayInitializingCount(Integer msgReplayInitializingCount) {
    this.msgReplayInitializingCount = msgReplayInitializingCount;
  }

  public MsgVpn msgReplayPendingCompleteCount(Integer msgReplayPendingCompleteCount) {
    this.msgReplayPendingCompleteCount = msgReplayPendingCompleteCount;
    return this;
  }

   /**
   * The number of message replays that are pending complete in the Message VPN.
   * @return msgReplayPendingCompleteCount
  **/
  @Schema(description = "The number of message replays that are pending complete in the Message VPN.")
  public Integer getMsgReplayPendingCompleteCount() {
    return msgReplayPendingCompleteCount;
  }

  public void setMsgReplayPendingCompleteCount(Integer msgReplayPendingCompleteCount) {
    this.msgReplayPendingCompleteCount = msgReplayPendingCompleteCount;
  }

  public MsgVpn msgSpoolMsgCount(Long msgSpoolMsgCount) {
    this.msgSpoolMsgCount = msgSpoolMsgCount;
    return this;
  }

   /**
   * The current number of messages spooled (persisted in the Message Spool) in the Message VPN. Available since 2.14.
   * @return msgSpoolMsgCount
  **/
  @Schema(description = "The current number of messages spooled (persisted in the Message Spool) in the Message VPN. Available since 2.14.")
  public Long getMsgSpoolMsgCount() {
    return msgSpoolMsgCount;
  }

  public void setMsgSpoolMsgCount(Long msgSpoolMsgCount) {
    this.msgSpoolMsgCount = msgSpoolMsgCount;
  }

  public MsgVpn msgSpoolRxMsgCount(Long msgSpoolRxMsgCount) {
    this.msgSpoolRxMsgCount = msgSpoolRxMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages received by the Message VPN. Available since 2.13.
   * @return msgSpoolRxMsgCount
  **/
  @Schema(description = "The number of guaranteed messages received by the Message VPN. Available since 2.13.")
  public Long getMsgSpoolRxMsgCount() {
    return msgSpoolRxMsgCount;
  }

  public void setMsgSpoolRxMsgCount(Long msgSpoolRxMsgCount) {
    this.msgSpoolRxMsgCount = msgSpoolRxMsgCount;
  }

  public MsgVpn msgSpoolTxMsgCount(Long msgSpoolTxMsgCount) {
    this.msgSpoolTxMsgCount = msgSpoolTxMsgCount;
    return this;
  }

   /**
   * The number of guaranteed messages transmitted by the Message VPN. One message to multiple clients is counted as one message. Available since 2.13.
   * @return msgSpoolTxMsgCount
  **/
  @Schema(description = "The number of guaranteed messages transmitted by the Message VPN. One message to multiple clients is counted as one message. Available since 2.13.")
  public Long getMsgSpoolTxMsgCount() {
    return msgSpoolTxMsgCount;
  }

  public void setMsgSpoolTxMsgCount(Long msgSpoolTxMsgCount) {
    this.msgSpoolTxMsgCount = msgSpoolTxMsgCount;
  }

  public MsgVpn msgSpoolUsage(Long msgSpoolUsage) {
    this.msgSpoolUsage = msgSpoolUsage;
    return this;
  }

   /**
   * The current message spool usage by the Message VPN, in bytes (B).
   * @return msgSpoolUsage
  **/
  @Schema(description = "The current message spool usage by the Message VPN, in bytes (B).")
  public Long getMsgSpoolUsage() {
    return msgSpoolUsage;
  }

  public void setMsgSpoolUsage(Long msgSpoolUsage) {
    this.msgSpoolUsage = msgSpoolUsage;
  }

  public MsgVpn msgVpnName(String msgVpnName) {
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

  public MsgVpn rate(MsgVpnRate rate) {
    this.rate = rate;
    return this;
  }

   /**
   * Get rate
   * @return rate
  **/
  @Schema(description = "")
  public MsgVpnRate getRate() {
    return rate;
  }

  public void setRate(MsgVpnRate rate) {
    this.rate = rate;
  }

  public MsgVpn replicationAckPropagationIntervalMsgCount(Long replicationAckPropagationIntervalMsgCount) {
    this.replicationAckPropagationIntervalMsgCount = replicationAckPropagationIntervalMsgCount;
    return this;
  }

   /**
   * The acknowledgement (ACK) propagation interval for the replication Bridge, in number of replicated messages. Available since 2.12.
   * @return replicationAckPropagationIntervalMsgCount
  **/
  @Schema(description = "The acknowledgement (ACK) propagation interval for the replication Bridge, in number of replicated messages. Available since 2.12.")
  public Long getReplicationAckPropagationIntervalMsgCount() {
    return replicationAckPropagationIntervalMsgCount;
  }

  public void setReplicationAckPropagationIntervalMsgCount(Long replicationAckPropagationIntervalMsgCount) {
    this.replicationAckPropagationIntervalMsgCount = replicationAckPropagationIntervalMsgCount;
  }

  public MsgVpn replicationActiveAckPropTxMsgCount(Long replicationActiveAckPropTxMsgCount) {
    this.replicationActiveAckPropTxMsgCount = replicationActiveAckPropTxMsgCount;
    return this;
  }

   /**
   * The number of acknowledgement messages propagated to the replication standby remote Message VPN. Available since 2.12.
   * @return replicationActiveAckPropTxMsgCount
  **/
  @Schema(description = "The number of acknowledgement messages propagated to the replication standby remote Message VPN. Available since 2.12.")
  public Long getReplicationActiveAckPropTxMsgCount() {
    return replicationActiveAckPropTxMsgCount;
  }

  public void setReplicationActiveAckPropTxMsgCount(Long replicationActiveAckPropTxMsgCount) {
    this.replicationActiveAckPropTxMsgCount = replicationActiveAckPropTxMsgCount;
  }

  public MsgVpn replicationActiveAsyncQueuedMsgCount(Long replicationActiveAsyncQueuedMsgCount) {
    this.replicationActiveAsyncQueuedMsgCount = replicationActiveAsyncQueuedMsgCount;
    return this;
  }

   /**
   * The number of async messages queued to the replication standby remote Message VPN. Available since 2.12.
   * @return replicationActiveAsyncQueuedMsgCount
  **/
  @Schema(description = "The number of async messages queued to the replication standby remote Message VPN. Available since 2.12.")
  public Long getReplicationActiveAsyncQueuedMsgCount() {
    return replicationActiveAsyncQueuedMsgCount;
  }

  public void setReplicationActiveAsyncQueuedMsgCount(Long replicationActiveAsyncQueuedMsgCount) {
    this.replicationActiveAsyncQueuedMsgCount = replicationActiveAsyncQueuedMsgCount;
  }

  public MsgVpn replicationActiveLocallyConsumedMsgCount(Long replicationActiveLocallyConsumedMsgCount) {
    this.replicationActiveLocallyConsumedMsgCount = replicationActiveLocallyConsumedMsgCount;
    return this;
  }

   /**
   * The number of messages consumed in the replication active local Message VPN. Available since 2.12.
   * @return replicationActiveLocallyConsumedMsgCount
  **/
  @Schema(description = "The number of messages consumed in the replication active local Message VPN. Available since 2.12.")
  public Long getReplicationActiveLocallyConsumedMsgCount() {
    return replicationActiveLocallyConsumedMsgCount;
  }

  public void setReplicationActiveLocallyConsumedMsgCount(Long replicationActiveLocallyConsumedMsgCount) {
    this.replicationActiveLocallyConsumedMsgCount = replicationActiveLocallyConsumedMsgCount;
  }

  public MsgVpn replicationActiveMateFlowCongestedPeakTime(Integer replicationActiveMateFlowCongestedPeakTime) {
    this.replicationActiveMateFlowCongestedPeakTime = replicationActiveMateFlowCongestedPeakTime;
    return this;
  }

   /**
   * The peak amount of time in seconds the message flow has been congested to the replication standby remote Message VPN. Available since 2.12.
   * @return replicationActiveMateFlowCongestedPeakTime
  **/
  @Schema(description = "The peak amount of time in seconds the message flow has been congested to the replication standby remote Message VPN. Available since 2.12.")
  public Integer getReplicationActiveMateFlowCongestedPeakTime() {
    return replicationActiveMateFlowCongestedPeakTime;
  }

  public void setReplicationActiveMateFlowCongestedPeakTime(Integer replicationActiveMateFlowCongestedPeakTime) {
    this.replicationActiveMateFlowCongestedPeakTime = replicationActiveMateFlowCongestedPeakTime;
  }

  public MsgVpn replicationActiveMateFlowNotCongestedPeakTime(Integer replicationActiveMateFlowNotCongestedPeakTime) {
    this.replicationActiveMateFlowNotCongestedPeakTime = replicationActiveMateFlowNotCongestedPeakTime;
    return this;
  }

   /**
   * The peak amount of time in seconds the message flow has not been congested to the replication standby remote Message VPN. Available since 2.12.
   * @return replicationActiveMateFlowNotCongestedPeakTime
  **/
  @Schema(description = "The peak amount of time in seconds the message flow has not been congested to the replication standby remote Message VPN. Available since 2.12.")
  public Integer getReplicationActiveMateFlowNotCongestedPeakTime() {
    return replicationActiveMateFlowNotCongestedPeakTime;
  }

  public void setReplicationActiveMateFlowNotCongestedPeakTime(Integer replicationActiveMateFlowNotCongestedPeakTime) {
    this.replicationActiveMateFlowNotCongestedPeakTime = replicationActiveMateFlowNotCongestedPeakTime;
  }

  public MsgVpn replicationActivePromotedQueuedMsgCount(Long replicationActivePromotedQueuedMsgCount) {
    this.replicationActivePromotedQueuedMsgCount = replicationActivePromotedQueuedMsgCount;
    return this;
  }

   /**
   * The number of promoted messages queued to the replication standby remote Message VPN. Available since 2.12.
   * @return replicationActivePromotedQueuedMsgCount
  **/
  @Schema(description = "The number of promoted messages queued to the replication standby remote Message VPN. Available since 2.12.")
  public Long getReplicationActivePromotedQueuedMsgCount() {
    return replicationActivePromotedQueuedMsgCount;
  }

  public void setReplicationActivePromotedQueuedMsgCount(Long replicationActivePromotedQueuedMsgCount) {
    this.replicationActivePromotedQueuedMsgCount = replicationActivePromotedQueuedMsgCount;
  }

  public MsgVpn replicationActiveReconcileRequestRxMsgCount(Long replicationActiveReconcileRequestRxMsgCount) {
    this.replicationActiveReconcileRequestRxMsgCount = replicationActiveReconcileRequestRxMsgCount;
    return this;
  }

   /**
   * The number of reconcile request messages received from the replication standby remote Message VPN. Available since 2.12.
   * @return replicationActiveReconcileRequestRxMsgCount
  **/
  @Schema(description = "The number of reconcile request messages received from the replication standby remote Message VPN. Available since 2.12.")
  public Long getReplicationActiveReconcileRequestRxMsgCount() {
    return replicationActiveReconcileRequestRxMsgCount;
  }

  public void setReplicationActiveReconcileRequestRxMsgCount(Long replicationActiveReconcileRequestRxMsgCount) {
    this.replicationActiveReconcileRequestRxMsgCount = replicationActiveReconcileRequestRxMsgCount;
  }

  public MsgVpn replicationActiveSyncEligiblePeakTime(Integer replicationActiveSyncEligiblePeakTime) {
    this.replicationActiveSyncEligiblePeakTime = replicationActiveSyncEligiblePeakTime;
    return this;
  }

   /**
   * The peak amount of time in seconds sync replication has been eligible to the replication standby remote Message VPN. Available since 2.12.
   * @return replicationActiveSyncEligiblePeakTime
  **/
  @Schema(description = "The peak amount of time in seconds sync replication has been eligible to the replication standby remote Message VPN. Available since 2.12.")
  public Integer getReplicationActiveSyncEligiblePeakTime() {
    return replicationActiveSyncEligiblePeakTime;
  }

  public void setReplicationActiveSyncEligiblePeakTime(Integer replicationActiveSyncEligiblePeakTime) {
    this.replicationActiveSyncEligiblePeakTime = replicationActiveSyncEligiblePeakTime;
  }

  public MsgVpn replicationActiveSyncIneligiblePeakTime(Integer replicationActiveSyncIneligiblePeakTime) {
    this.replicationActiveSyncIneligiblePeakTime = replicationActiveSyncIneligiblePeakTime;
    return this;
  }

   /**
   * The peak amount of time in seconds sync replication has been ineligible to the replication standby remote Message VPN. Available since 2.12.
   * @return replicationActiveSyncIneligiblePeakTime
  **/
  @Schema(description = "The peak amount of time in seconds sync replication has been ineligible to the replication standby remote Message VPN. Available since 2.12.")
  public Integer getReplicationActiveSyncIneligiblePeakTime() {
    return replicationActiveSyncIneligiblePeakTime;
  }

  public void setReplicationActiveSyncIneligiblePeakTime(Integer replicationActiveSyncIneligiblePeakTime) {
    this.replicationActiveSyncIneligiblePeakTime = replicationActiveSyncIneligiblePeakTime;
  }

  public MsgVpn replicationActiveSyncQueuedAsAsyncMsgCount(Long replicationActiveSyncQueuedAsAsyncMsgCount) {
    this.replicationActiveSyncQueuedAsAsyncMsgCount = replicationActiveSyncQueuedAsAsyncMsgCount;
    return this;
  }

   /**
   * The number of sync messages queued as async to the replication standby remote Message VPN. Available since 2.12.
   * @return replicationActiveSyncQueuedAsAsyncMsgCount
  **/
  @Schema(description = "The number of sync messages queued as async to the replication standby remote Message VPN. Available since 2.12.")
  public Long getReplicationActiveSyncQueuedAsAsyncMsgCount() {
    return replicationActiveSyncQueuedAsAsyncMsgCount;
  }

  public void setReplicationActiveSyncQueuedAsAsyncMsgCount(Long replicationActiveSyncQueuedAsAsyncMsgCount) {
    this.replicationActiveSyncQueuedAsAsyncMsgCount = replicationActiveSyncQueuedAsAsyncMsgCount;
  }

  public MsgVpn replicationActiveSyncQueuedMsgCount(Long replicationActiveSyncQueuedMsgCount) {
    this.replicationActiveSyncQueuedMsgCount = replicationActiveSyncQueuedMsgCount;
    return this;
  }

   /**
   * The number of sync messages queued to the replication standby remote Message VPN. Available since 2.12.
   * @return replicationActiveSyncQueuedMsgCount
  **/
  @Schema(description = "The number of sync messages queued to the replication standby remote Message VPN. Available since 2.12.")
  public Long getReplicationActiveSyncQueuedMsgCount() {
    return replicationActiveSyncQueuedMsgCount;
  }

  public void setReplicationActiveSyncQueuedMsgCount(Long replicationActiveSyncQueuedMsgCount) {
    this.replicationActiveSyncQueuedMsgCount = replicationActiveSyncQueuedMsgCount;
  }

  public MsgVpn replicationActiveTransitionToSyncIneligibleCount(Long replicationActiveTransitionToSyncIneligibleCount) {
    this.replicationActiveTransitionToSyncIneligibleCount = replicationActiveTransitionToSyncIneligibleCount;
    return this;
  }

   /**
   * The number of sync replication ineligible transitions to the replication standby remote Message VPN. Available since 2.12.
   * @return replicationActiveTransitionToSyncIneligibleCount
  **/
  @Schema(description = "The number of sync replication ineligible transitions to the replication standby remote Message VPN. Available since 2.12.")
  public Long getReplicationActiveTransitionToSyncIneligibleCount() {
    return replicationActiveTransitionToSyncIneligibleCount;
  }

  public void setReplicationActiveTransitionToSyncIneligibleCount(Long replicationActiveTransitionToSyncIneligibleCount) {
    this.replicationActiveTransitionToSyncIneligibleCount = replicationActiveTransitionToSyncIneligibleCount;
  }

  public MsgVpn replicationBridgeAuthenticationBasicClientUsername(String replicationBridgeAuthenticationBasicClientUsername) {
    this.replicationBridgeAuthenticationBasicClientUsername = replicationBridgeAuthenticationBasicClientUsername;
    return this;
  }

   /**
   * The Client Username the replication Bridge uses to login to the remote Message VPN. Available since 2.12.
   * @return replicationBridgeAuthenticationBasicClientUsername
  **/
  @Schema(description = "The Client Username the replication Bridge uses to login to the remote Message VPN. Available since 2.12.")
  public String getReplicationBridgeAuthenticationBasicClientUsername() {
    return replicationBridgeAuthenticationBasicClientUsername;
  }

  public void setReplicationBridgeAuthenticationBasicClientUsername(String replicationBridgeAuthenticationBasicClientUsername) {
    this.replicationBridgeAuthenticationBasicClientUsername = replicationBridgeAuthenticationBasicClientUsername;
  }

  public MsgVpn replicationBridgeAuthenticationScheme(ReplicationBridgeAuthenticationSchemeEnum replicationBridgeAuthenticationScheme) {
    this.replicationBridgeAuthenticationScheme = replicationBridgeAuthenticationScheme;
    return this;
  }

   /**
   * The authentication scheme for the replication Bridge in the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;basic\&quot; - Basic Authentication Scheme (via username and password). \&quot;client-certificate\&quot; - Client Certificate Authentication Scheme (via certificate file or content). &lt;/pre&gt;  Available since 2.12.
   * @return replicationBridgeAuthenticationScheme
  **/
  @Schema(description = "The authentication scheme for the replication Bridge in the Message VPN. The allowed values and their meaning are:  <pre> \"basic\" - Basic Authentication Scheme (via username and password). \"client-certificate\" - Client Certificate Authentication Scheme (via certificate file or content). </pre>  Available since 2.12.")
  public ReplicationBridgeAuthenticationSchemeEnum getReplicationBridgeAuthenticationScheme() {
    return replicationBridgeAuthenticationScheme;
  }

  public void setReplicationBridgeAuthenticationScheme(ReplicationBridgeAuthenticationSchemeEnum replicationBridgeAuthenticationScheme) {
    this.replicationBridgeAuthenticationScheme = replicationBridgeAuthenticationScheme;
  }

  public MsgVpn replicationBridgeBoundToQueue(Boolean replicationBridgeBoundToQueue) {
    this.replicationBridgeBoundToQueue = replicationBridgeBoundToQueue;
    return this;
  }

   /**
   * Indicates whether the local replication Bridge is bound to the Queue in the remote Message VPN. Available since 2.12.
   * @return replicationBridgeBoundToQueue
  **/
  @Schema(description = "Indicates whether the local replication Bridge is bound to the Queue in the remote Message VPN. Available since 2.12.")
  public Boolean isReplicationBridgeBoundToQueue() {
    return replicationBridgeBoundToQueue;
  }

  public void setReplicationBridgeBoundToQueue(Boolean replicationBridgeBoundToQueue) {
    this.replicationBridgeBoundToQueue = replicationBridgeBoundToQueue;
  }

  public MsgVpn replicationBridgeCompressedDataEnabled(Boolean replicationBridgeCompressedDataEnabled) {
    this.replicationBridgeCompressedDataEnabled = replicationBridgeCompressedDataEnabled;
    return this;
  }

   /**
   * Indicates whether compression is used for the replication Bridge. Available since 2.12.
   * @return replicationBridgeCompressedDataEnabled
  **/
  @Schema(description = "Indicates whether compression is used for the replication Bridge. Available since 2.12.")
  public Boolean isReplicationBridgeCompressedDataEnabled() {
    return replicationBridgeCompressedDataEnabled;
  }

  public void setReplicationBridgeCompressedDataEnabled(Boolean replicationBridgeCompressedDataEnabled) {
    this.replicationBridgeCompressedDataEnabled = replicationBridgeCompressedDataEnabled;
  }

  public MsgVpn replicationBridgeEgressFlowWindowSize(Long replicationBridgeEgressFlowWindowSize) {
    this.replicationBridgeEgressFlowWindowSize = replicationBridgeEgressFlowWindowSize;
    return this;
  }

   /**
   * The size of the window used for guaranteed messages published to the replication Bridge, in messages. Available since 2.12.
   * @return replicationBridgeEgressFlowWindowSize
  **/
  @Schema(description = "The size of the window used for guaranteed messages published to the replication Bridge, in messages. Available since 2.12.")
  public Long getReplicationBridgeEgressFlowWindowSize() {
    return replicationBridgeEgressFlowWindowSize;
  }

  public void setReplicationBridgeEgressFlowWindowSize(Long replicationBridgeEgressFlowWindowSize) {
    this.replicationBridgeEgressFlowWindowSize = replicationBridgeEgressFlowWindowSize;
  }

  public MsgVpn replicationBridgeName(String replicationBridgeName) {
    this.replicationBridgeName = replicationBridgeName;
    return this;
  }

   /**
   * The name of the local replication Bridge in the Message VPN. Available since 2.12.
   * @return replicationBridgeName
  **/
  @Schema(description = "The name of the local replication Bridge in the Message VPN. Available since 2.12.")
  public String getReplicationBridgeName() {
    return replicationBridgeName;
  }

  public void setReplicationBridgeName(String replicationBridgeName) {
    this.replicationBridgeName = replicationBridgeName;
  }

  public MsgVpn replicationBridgeRetryDelay(Long replicationBridgeRetryDelay) {
    this.replicationBridgeRetryDelay = replicationBridgeRetryDelay;
    return this;
  }

   /**
   * The number of seconds that must pass before retrying the replication Bridge connection. Available since 2.12.
   * @return replicationBridgeRetryDelay
  **/
  @Schema(description = "The number of seconds that must pass before retrying the replication Bridge connection. Available since 2.12.")
  public Long getReplicationBridgeRetryDelay() {
    return replicationBridgeRetryDelay;
  }

  public void setReplicationBridgeRetryDelay(Long replicationBridgeRetryDelay) {
    this.replicationBridgeRetryDelay = replicationBridgeRetryDelay;
  }

  public MsgVpn replicationBridgeTlsEnabled(Boolean replicationBridgeTlsEnabled) {
    this.replicationBridgeTlsEnabled = replicationBridgeTlsEnabled;
    return this;
  }

   /**
   * Indicates whether encryption (TLS) is enabled for the replication Bridge connection. Available since 2.12.
   * @return replicationBridgeTlsEnabled
  **/
  @Schema(description = "Indicates whether encryption (TLS) is enabled for the replication Bridge connection. Available since 2.12.")
  public Boolean isReplicationBridgeTlsEnabled() {
    return replicationBridgeTlsEnabled;
  }

  public void setReplicationBridgeTlsEnabled(Boolean replicationBridgeTlsEnabled) {
    this.replicationBridgeTlsEnabled = replicationBridgeTlsEnabled;
  }

  public MsgVpn replicationBridgeUnidirectionalClientProfileName(String replicationBridgeUnidirectionalClientProfileName) {
    this.replicationBridgeUnidirectionalClientProfileName = replicationBridgeUnidirectionalClientProfileName;
    return this;
  }

   /**
   * The Client Profile for the unidirectional replication Bridge in the Message VPN. It is used only for the TCP parameters. Available since 2.12.
   * @return replicationBridgeUnidirectionalClientProfileName
  **/
  @Schema(description = "The Client Profile for the unidirectional replication Bridge in the Message VPN. It is used only for the TCP parameters. Available since 2.12.")
  public String getReplicationBridgeUnidirectionalClientProfileName() {
    return replicationBridgeUnidirectionalClientProfileName;
  }

  public void setReplicationBridgeUnidirectionalClientProfileName(String replicationBridgeUnidirectionalClientProfileName) {
    this.replicationBridgeUnidirectionalClientProfileName = replicationBridgeUnidirectionalClientProfileName;
  }

  public MsgVpn replicationBridgeUp(Boolean replicationBridgeUp) {
    this.replicationBridgeUp = replicationBridgeUp;
    return this;
  }

   /**
   * Indicates whether the local replication Bridge is operationally up in the Message VPN. Available since 2.12.
   * @return replicationBridgeUp
  **/
  @Schema(description = "Indicates whether the local replication Bridge is operationally up in the Message VPN. Available since 2.12.")
  public Boolean isReplicationBridgeUp() {
    return replicationBridgeUp;
  }

  public void setReplicationBridgeUp(Boolean replicationBridgeUp) {
    this.replicationBridgeUp = replicationBridgeUp;
  }

  public MsgVpn replicationEnabled(Boolean replicationEnabled) {
    this.replicationEnabled = replicationEnabled;
    return this;
  }

   /**
   * Indicates whether replication is enabled for the Message VPN. Available since 2.12.
   * @return replicationEnabled
  **/
  @Schema(description = "Indicates whether replication is enabled for the Message VPN. Available since 2.12.")
  public Boolean isReplicationEnabled() {
    return replicationEnabled;
  }

  public void setReplicationEnabled(Boolean replicationEnabled) {
    this.replicationEnabled = replicationEnabled;
  }

  public MsgVpn replicationQueueBound(Boolean replicationQueueBound) {
    this.replicationQueueBound = replicationQueueBound;
    return this;
  }

   /**
   * Indicates whether the remote replication Bridge is bound to the Queue in the Message VPN. Available since 2.12.
   * @return replicationQueueBound
  **/
  @Schema(description = "Indicates whether the remote replication Bridge is bound to the Queue in the Message VPN. Available since 2.12.")
  public Boolean isReplicationQueueBound() {
    return replicationQueueBound;
  }

  public void setReplicationQueueBound(Boolean replicationQueueBound) {
    this.replicationQueueBound = replicationQueueBound;
  }

  public MsgVpn replicationQueueMaxMsgSpoolUsage(Long replicationQueueMaxMsgSpoolUsage) {
    this.replicationQueueMaxMsgSpoolUsage = replicationQueueMaxMsgSpoolUsage;
    return this;
  }

   /**
   * The maximum message spool usage by the replication Bridge local Queue (quota), in megabytes. Available since 2.12.
   * @return replicationQueueMaxMsgSpoolUsage
  **/
  @Schema(description = "The maximum message spool usage by the replication Bridge local Queue (quota), in megabytes. Available since 2.12.")
  public Long getReplicationQueueMaxMsgSpoolUsage() {
    return replicationQueueMaxMsgSpoolUsage;
  }

  public void setReplicationQueueMaxMsgSpoolUsage(Long replicationQueueMaxMsgSpoolUsage) {
    this.replicationQueueMaxMsgSpoolUsage = replicationQueueMaxMsgSpoolUsage;
  }

  public MsgVpn replicationQueueRejectMsgToSenderOnDiscardEnabled(Boolean replicationQueueRejectMsgToSenderOnDiscardEnabled) {
    this.replicationQueueRejectMsgToSenderOnDiscardEnabled = replicationQueueRejectMsgToSenderOnDiscardEnabled;
    return this;
  }

   /**
   * Indicates whether messages discarded on this replication Bridge Queue are rejected back to the sender. Available since 2.12.
   * @return replicationQueueRejectMsgToSenderOnDiscardEnabled
  **/
  @Schema(description = "Indicates whether messages discarded on this replication Bridge Queue are rejected back to the sender. Available since 2.12.")
  public Boolean isReplicationQueueRejectMsgToSenderOnDiscardEnabled() {
    return replicationQueueRejectMsgToSenderOnDiscardEnabled;
  }

  public void setReplicationQueueRejectMsgToSenderOnDiscardEnabled(Boolean replicationQueueRejectMsgToSenderOnDiscardEnabled) {
    this.replicationQueueRejectMsgToSenderOnDiscardEnabled = replicationQueueRejectMsgToSenderOnDiscardEnabled;
  }

  public MsgVpn replicationRejectMsgWhenSyncIneligibleEnabled(Boolean replicationRejectMsgWhenSyncIneligibleEnabled) {
    this.replicationRejectMsgWhenSyncIneligibleEnabled = replicationRejectMsgWhenSyncIneligibleEnabled;
    return this;
  }

   /**
   * Indicates whether guaranteed messages published to synchronously replicated Topics are rejected back to the sender when synchronous replication becomes ineligible. Available since 2.12.
   * @return replicationRejectMsgWhenSyncIneligibleEnabled
  **/
  @Schema(description = "Indicates whether guaranteed messages published to synchronously replicated Topics are rejected back to the sender when synchronous replication becomes ineligible. Available since 2.12.")
  public Boolean isReplicationRejectMsgWhenSyncIneligibleEnabled() {
    return replicationRejectMsgWhenSyncIneligibleEnabled;
  }

  public void setReplicationRejectMsgWhenSyncIneligibleEnabled(Boolean replicationRejectMsgWhenSyncIneligibleEnabled) {
    this.replicationRejectMsgWhenSyncIneligibleEnabled = replicationRejectMsgWhenSyncIneligibleEnabled;
  }

  public MsgVpn replicationRemoteBridgeName(String replicationRemoteBridgeName) {
    this.replicationRemoteBridgeName = replicationRemoteBridgeName;
    return this;
  }

   /**
   * The name of the remote replication Bridge in the Message VPN. Available since 2.12.
   * @return replicationRemoteBridgeName
  **/
  @Schema(description = "The name of the remote replication Bridge in the Message VPN. Available since 2.12.")
  public String getReplicationRemoteBridgeName() {
    return replicationRemoteBridgeName;
  }

  public void setReplicationRemoteBridgeName(String replicationRemoteBridgeName) {
    this.replicationRemoteBridgeName = replicationRemoteBridgeName;
  }

  public MsgVpn replicationRemoteBridgeUp(Boolean replicationRemoteBridgeUp) {
    this.replicationRemoteBridgeUp = replicationRemoteBridgeUp;
    return this;
  }

   /**
   * Indicates whether the remote replication Bridge is operationally up in the Message VPN. Available since 2.12.
   * @return replicationRemoteBridgeUp
  **/
  @Schema(description = "Indicates whether the remote replication Bridge is operationally up in the Message VPN. Available since 2.12.")
  public Boolean isReplicationRemoteBridgeUp() {
    return replicationRemoteBridgeUp;
  }

  public void setReplicationRemoteBridgeUp(Boolean replicationRemoteBridgeUp) {
    this.replicationRemoteBridgeUp = replicationRemoteBridgeUp;
  }

  public MsgVpn replicationRole(ReplicationRoleEnum replicationRole) {
    this.replicationRole = replicationRole;
    return this;
  }

   /**
   * The replication role for the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;active\&quot; - Assume the Active role in replication for the Message VPN. \&quot;standby\&quot; - Assume the Standby role in replication for the Message VPN. &lt;/pre&gt;  Available since 2.12.
   * @return replicationRole
  **/
  @Schema(description = "The replication role for the Message VPN. The allowed values and their meaning are:  <pre> \"active\" - Assume the Active role in replication for the Message VPN. \"standby\" - Assume the Standby role in replication for the Message VPN. </pre>  Available since 2.12.")
  public ReplicationRoleEnum getReplicationRole() {
    return replicationRole;
  }

  public void setReplicationRole(ReplicationRoleEnum replicationRole) {
    this.replicationRole = replicationRole;
  }

  public MsgVpn replicationStandbyAckPropOutOfSeqRxMsgCount(Long replicationStandbyAckPropOutOfSeqRxMsgCount) {
    this.replicationStandbyAckPropOutOfSeqRxMsgCount = replicationStandbyAckPropOutOfSeqRxMsgCount;
    return this;
  }

   /**
   * The number of acknowledgement messages received out of sequence from the replication active remote Message VPN. Available since 2.12.
   * @return replicationStandbyAckPropOutOfSeqRxMsgCount
  **/
  @Schema(description = "The number of acknowledgement messages received out of sequence from the replication active remote Message VPN. Available since 2.12.")
  public Long getReplicationStandbyAckPropOutOfSeqRxMsgCount() {
    return replicationStandbyAckPropOutOfSeqRxMsgCount;
  }

  public void setReplicationStandbyAckPropOutOfSeqRxMsgCount(Long replicationStandbyAckPropOutOfSeqRxMsgCount) {
    this.replicationStandbyAckPropOutOfSeqRxMsgCount = replicationStandbyAckPropOutOfSeqRxMsgCount;
  }

  public MsgVpn replicationStandbyAckPropRxMsgCount(Long replicationStandbyAckPropRxMsgCount) {
    this.replicationStandbyAckPropRxMsgCount = replicationStandbyAckPropRxMsgCount;
    return this;
  }

   /**
   * The number of acknowledgement messages received from the replication active remote Message VPN. Available since 2.12.
   * @return replicationStandbyAckPropRxMsgCount
  **/
  @Schema(description = "The number of acknowledgement messages received from the replication active remote Message VPN. Available since 2.12.")
  public Long getReplicationStandbyAckPropRxMsgCount() {
    return replicationStandbyAckPropRxMsgCount;
  }

  public void setReplicationStandbyAckPropRxMsgCount(Long replicationStandbyAckPropRxMsgCount) {
    this.replicationStandbyAckPropRxMsgCount = replicationStandbyAckPropRxMsgCount;
  }

  public MsgVpn replicationStandbyReconcileRequestTxMsgCount(Long replicationStandbyReconcileRequestTxMsgCount) {
    this.replicationStandbyReconcileRequestTxMsgCount = replicationStandbyReconcileRequestTxMsgCount;
    return this;
  }

   /**
   * The number of reconcile request messages transmitted to the replication active remote Message VPN. Available since 2.12.
   * @return replicationStandbyReconcileRequestTxMsgCount
  **/
  @Schema(description = "The number of reconcile request messages transmitted to the replication active remote Message VPN. Available since 2.12.")
  public Long getReplicationStandbyReconcileRequestTxMsgCount() {
    return replicationStandbyReconcileRequestTxMsgCount;
  }

  public void setReplicationStandbyReconcileRequestTxMsgCount(Long replicationStandbyReconcileRequestTxMsgCount) {
    this.replicationStandbyReconcileRequestTxMsgCount = replicationStandbyReconcileRequestTxMsgCount;
  }

  public MsgVpn replicationStandbyRxMsgCount(Long replicationStandbyRxMsgCount) {
    this.replicationStandbyRxMsgCount = replicationStandbyRxMsgCount;
    return this;
  }

   /**
   * The number of messages received from the replication active remote Message VPN. Available since 2.12.
   * @return replicationStandbyRxMsgCount
  **/
  @Schema(description = "The number of messages received from the replication active remote Message VPN. Available since 2.12.")
  public Long getReplicationStandbyRxMsgCount() {
    return replicationStandbyRxMsgCount;
  }

  public void setReplicationStandbyRxMsgCount(Long replicationStandbyRxMsgCount) {
    this.replicationStandbyRxMsgCount = replicationStandbyRxMsgCount;
  }

  public MsgVpn replicationStandbyTransactionRequestCount(Long replicationStandbyTransactionRequestCount) {
    this.replicationStandbyTransactionRequestCount = replicationStandbyTransactionRequestCount;
    return this;
  }

   /**
   * The number of transaction requests received from the replication active remote Message VPN. Available since 2.12.
   * @return replicationStandbyTransactionRequestCount
  **/
  @Schema(description = "The number of transaction requests received from the replication active remote Message VPN. Available since 2.12.")
  public Long getReplicationStandbyTransactionRequestCount() {
    return replicationStandbyTransactionRequestCount;
  }

  public void setReplicationStandbyTransactionRequestCount(Long replicationStandbyTransactionRequestCount) {
    this.replicationStandbyTransactionRequestCount = replicationStandbyTransactionRequestCount;
  }

  public MsgVpn replicationStandbyTransactionRequestFailureCount(Long replicationStandbyTransactionRequestFailureCount) {
    this.replicationStandbyTransactionRequestFailureCount = replicationStandbyTransactionRequestFailureCount;
    return this;
  }

   /**
   * The number of transaction requests received from the replication active remote Message VPN that failed. Available since 2.12.
   * @return replicationStandbyTransactionRequestFailureCount
  **/
  @Schema(description = "The number of transaction requests received from the replication active remote Message VPN that failed. Available since 2.12.")
  public Long getReplicationStandbyTransactionRequestFailureCount() {
    return replicationStandbyTransactionRequestFailureCount;
  }

  public void setReplicationStandbyTransactionRequestFailureCount(Long replicationStandbyTransactionRequestFailureCount) {
    this.replicationStandbyTransactionRequestFailureCount = replicationStandbyTransactionRequestFailureCount;
  }

  public MsgVpn replicationStandbyTransactionRequestSuccessCount(Long replicationStandbyTransactionRequestSuccessCount) {
    this.replicationStandbyTransactionRequestSuccessCount = replicationStandbyTransactionRequestSuccessCount;
    return this;
  }

   /**
   * The number of transaction requests received from the replication active remote Message VPN that succeeded. Available since 2.12.
   * @return replicationStandbyTransactionRequestSuccessCount
  **/
  @Schema(description = "The number of transaction requests received from the replication active remote Message VPN that succeeded. Available since 2.12.")
  public Long getReplicationStandbyTransactionRequestSuccessCount() {
    return replicationStandbyTransactionRequestSuccessCount;
  }

  public void setReplicationStandbyTransactionRequestSuccessCount(Long replicationStandbyTransactionRequestSuccessCount) {
    this.replicationStandbyTransactionRequestSuccessCount = replicationStandbyTransactionRequestSuccessCount;
  }

  public MsgVpn replicationSyncEligible(Boolean replicationSyncEligible) {
    this.replicationSyncEligible = replicationSyncEligible;
    return this;
  }

   /**
   * Indicates whether sync replication is eligible in the Message VPN. Available since 2.12.
   * @return replicationSyncEligible
  **/
  @Schema(description = "Indicates whether sync replication is eligible in the Message VPN. Available since 2.12.")
  public Boolean isReplicationSyncEligible() {
    return replicationSyncEligible;
  }

  public void setReplicationSyncEligible(Boolean replicationSyncEligible) {
    this.replicationSyncEligible = replicationSyncEligible;
  }

  public MsgVpn replicationTransactionMode(ReplicationTransactionModeEnum replicationTransactionMode) {
    this.replicationTransactionMode = replicationTransactionMode;
    return this;
  }

   /**
   * Indicates whether synchronous or asynchronous replication mode is used for all transactions within the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;sync\&quot; - Messages are acknowledged when replicated (spooled remotely). \&quot;async\&quot; - Messages are acknowledged when pending replication (spooled locally). &lt;/pre&gt;  Available since 2.12.
   * @return replicationTransactionMode
  **/
  @Schema(description = "Indicates whether synchronous or asynchronous replication mode is used for all transactions within the Message VPN. The allowed values and their meaning are:  <pre> \"sync\" - Messages are acknowledged when replicated (spooled remotely). \"async\" - Messages are acknowledged when pending replication (spooled locally). </pre>  Available since 2.12.")
  public ReplicationTransactionModeEnum getReplicationTransactionMode() {
    return replicationTransactionMode;
  }

  public void setReplicationTransactionMode(ReplicationTransactionModeEnum replicationTransactionMode) {
    this.replicationTransactionMode = replicationTransactionMode;
  }

  public MsgVpn restTlsServerCertEnforceTrustedCommonNameEnabled(Boolean restTlsServerCertEnforceTrustedCommonNameEnabled) {
    this.restTlsServerCertEnforceTrustedCommonNameEnabled = restTlsServerCertEnforceTrustedCommonNameEnabled;
    return this;
  }

   /**
   * Indicates whether the Common Name (CN) in the server certificate from the remote REST Consumer is validated. Deprecated since 2.17. Common Name validation has been replaced by Server Certificate Name validation.
   * @return restTlsServerCertEnforceTrustedCommonNameEnabled
  **/
  @Schema(description = "Indicates whether the Common Name (CN) in the server certificate from the remote REST Consumer is validated. Deprecated since 2.17. Common Name validation has been replaced by Server Certificate Name validation.")
  public Boolean isRestTlsServerCertEnforceTrustedCommonNameEnabled() {
    return restTlsServerCertEnforceTrustedCommonNameEnabled;
  }

  public void setRestTlsServerCertEnforceTrustedCommonNameEnabled(Boolean restTlsServerCertEnforceTrustedCommonNameEnabled) {
    this.restTlsServerCertEnforceTrustedCommonNameEnabled = restTlsServerCertEnforceTrustedCommonNameEnabled;
  }

  public MsgVpn restTlsServerCertMaxChainDepth(Long restTlsServerCertMaxChainDepth) {
    this.restTlsServerCertMaxChainDepth = restTlsServerCertMaxChainDepth;
    return this;
  }

   /**
   * The maximum depth for a REST Consumer server certificate chain. The depth of a chain is defined as the number of signing CA certificates that are present in the chain back to a trusted self-signed root CA certificate.
   * @return restTlsServerCertMaxChainDepth
  **/
  @Schema(description = "The maximum depth for a REST Consumer server certificate chain. The depth of a chain is defined as the number of signing CA certificates that are present in the chain back to a trusted self-signed root CA certificate.")
  public Long getRestTlsServerCertMaxChainDepth() {
    return restTlsServerCertMaxChainDepth;
  }

  public void setRestTlsServerCertMaxChainDepth(Long restTlsServerCertMaxChainDepth) {
    this.restTlsServerCertMaxChainDepth = restTlsServerCertMaxChainDepth;
  }

  public MsgVpn restTlsServerCertValidateDateEnabled(Boolean restTlsServerCertValidateDateEnabled) {
    this.restTlsServerCertValidateDateEnabled = restTlsServerCertValidateDateEnabled;
    return this;
  }

   /**
   * Indicates whether the \&quot;Not Before\&quot; and \&quot;Not After\&quot; validity dates in the REST Consumer server certificate are checked.
   * @return restTlsServerCertValidateDateEnabled
  **/
  @Schema(description = "Indicates whether the \"Not Before\" and \"Not After\" validity dates in the REST Consumer server certificate are checked.")
  public Boolean isRestTlsServerCertValidateDateEnabled() {
    return restTlsServerCertValidateDateEnabled;
  }

  public void setRestTlsServerCertValidateDateEnabled(Boolean restTlsServerCertValidateDateEnabled) {
    this.restTlsServerCertValidateDateEnabled = restTlsServerCertValidateDateEnabled;
  }

  public MsgVpn restTlsServerCertValidateNameEnabled(Boolean restTlsServerCertValidateNameEnabled) {
    this.restTlsServerCertValidateNameEnabled = restTlsServerCertValidateNameEnabled;
    return this;
  }

   /**
   * Enable or disable the TLS authentication mechanism of verifying the name used to connect to the remote REST Consumer. If enabled, the name used to connect to the remote REST Consumer is checked against the names specified in the certificate returned by the remote router. Common Name validation is not performed if Server Certificate Name Validation is enabled, even if Common Name validation is also enabled. Available since 2.17.
   * @return restTlsServerCertValidateNameEnabled
  **/
  @Schema(description = "Enable or disable the TLS authentication mechanism of verifying the name used to connect to the remote REST Consumer. If enabled, the name used to connect to the remote REST Consumer is checked against the names specified in the certificate returned by the remote router. Common Name validation is not performed if Server Certificate Name Validation is enabled, even if Common Name validation is also enabled. Available since 2.17.")
  public Boolean isRestTlsServerCertValidateNameEnabled() {
    return restTlsServerCertValidateNameEnabled;
  }

  public void setRestTlsServerCertValidateNameEnabled(Boolean restTlsServerCertValidateNameEnabled) {
    this.restTlsServerCertValidateNameEnabled = restTlsServerCertValidateNameEnabled;
  }

  public MsgVpn rxByteCount(Long rxByteCount) {
    this.rxByteCount = rxByteCount;
    return this;
  }

   /**
   * The amount of messages received from clients by the Message VPN, in bytes (B). Available since 2.12.
   * @return rxByteCount
  **/
  @Schema(description = "The amount of messages received from clients by the Message VPN, in bytes (B). Available since 2.12.")
  public Long getRxByteCount() {
    return rxByteCount;
  }

  public void setRxByteCount(Long rxByteCount) {
    this.rxByteCount = rxByteCount;
  }

  public MsgVpn rxByteRate(Long rxByteRate) {
    this.rxByteRate = rxByteRate;
    return this;
  }

   /**
   * The current message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.13.
   * @return rxByteRate
  **/
  @Schema(description = "The current message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.13.")
  public Long getRxByteRate() {
    return rxByteRate;
  }

  public void setRxByteRate(Long rxByteRate) {
    this.rxByteRate = rxByteRate;
  }

  public MsgVpn rxCompressedByteCount(Long rxCompressedByteCount) {
    this.rxCompressedByteCount = rxCompressedByteCount;
    return this;
  }

   /**
   * The amount of compressed messages received by the Message VPN, in bytes (B). Available since 2.12.
   * @return rxCompressedByteCount
  **/
  @Schema(description = "The amount of compressed messages received by the Message VPN, in bytes (B). Available since 2.12.")
  public Long getRxCompressedByteCount() {
    return rxCompressedByteCount;
  }

  public void setRxCompressedByteCount(Long rxCompressedByteCount) {
    this.rxCompressedByteCount = rxCompressedByteCount;
  }

  public MsgVpn rxCompressedByteRate(Long rxCompressedByteRate) {
    this.rxCompressedByteRate = rxCompressedByteRate;
    return this;
  }

   /**
   * The current compressed message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.12.
   * @return rxCompressedByteRate
  **/
  @Schema(description = "The current compressed message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.12.")
  public Long getRxCompressedByteRate() {
    return rxCompressedByteRate;
  }

  public void setRxCompressedByteRate(Long rxCompressedByteRate) {
    this.rxCompressedByteRate = rxCompressedByteRate;
  }

  public MsgVpn rxCompressionRatio(String rxCompressionRatio) {
    this.rxCompressionRatio = rxCompressionRatio;
    return this;
  }

   /**
   * The compression ratio for messages received by the message VPN. Available since 2.12.
   * @return rxCompressionRatio
  **/
  @Schema(description = "The compression ratio for messages received by the message VPN. Available since 2.12.")
  public String getRxCompressionRatio() {
    return rxCompressionRatio;
  }

  public void setRxCompressionRatio(String rxCompressionRatio) {
    this.rxCompressionRatio = rxCompressionRatio;
  }

  public MsgVpn rxMsgCount(Long rxMsgCount) {
    this.rxMsgCount = rxMsgCount;
    return this;
  }

   /**
   * The number of messages received from clients by the Message VPN. Available since 2.12.
   * @return rxMsgCount
  **/
  @Schema(description = "The number of messages received from clients by the Message VPN. Available since 2.12.")
  public Long getRxMsgCount() {
    return rxMsgCount;
  }

  public void setRxMsgCount(Long rxMsgCount) {
    this.rxMsgCount = rxMsgCount;
  }

  public MsgVpn rxMsgRate(Long rxMsgRate) {
    this.rxMsgRate = rxMsgRate;
    return this;
  }

   /**
   * The current message rate received by the Message VPN, in messages per second (msg/sec). Available since 2.13.
   * @return rxMsgRate
  **/
  @Schema(description = "The current message rate received by the Message VPN, in messages per second (msg/sec). Available since 2.13.")
  public Long getRxMsgRate() {
    return rxMsgRate;
  }

  public void setRxMsgRate(Long rxMsgRate) {
    this.rxMsgRate = rxMsgRate;
  }

  public MsgVpn rxUncompressedByteCount(Long rxUncompressedByteCount) {
    this.rxUncompressedByteCount = rxUncompressedByteCount;
    return this;
  }

   /**
   * The amount of uncompressed messages received by the Message VPN, in bytes (B). Available since 2.12.
   * @return rxUncompressedByteCount
  **/
  @Schema(description = "The amount of uncompressed messages received by the Message VPN, in bytes (B). Available since 2.12.")
  public Long getRxUncompressedByteCount() {
    return rxUncompressedByteCount;
  }

  public void setRxUncompressedByteCount(Long rxUncompressedByteCount) {
    this.rxUncompressedByteCount = rxUncompressedByteCount;
  }

  public MsgVpn rxUncompressedByteRate(Long rxUncompressedByteRate) {
    this.rxUncompressedByteRate = rxUncompressedByteRate;
    return this;
  }

   /**
   * The current uncompressed message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.12.
   * @return rxUncompressedByteRate
  **/
  @Schema(description = "The current uncompressed message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.12.")
  public Long getRxUncompressedByteRate() {
    return rxUncompressedByteRate;
  }

  public void setRxUncompressedByteRate(Long rxUncompressedByteRate) {
    this.rxUncompressedByteRate = rxUncompressedByteRate;
  }

  public MsgVpn sempOverMsgBusAdminClientEnabled(Boolean sempOverMsgBusAdminClientEnabled) {
    this.sempOverMsgBusAdminClientEnabled = sempOverMsgBusAdminClientEnabled;
    return this;
  }

   /**
   * Indicates whether the \&quot;admin\&quot; level \&quot;client\&quot; commands are enabled for SEMP over the message bus in the Message VPN.
   * @return sempOverMsgBusAdminClientEnabled
  **/
  @Schema(description = "Indicates whether the \"admin\" level \"client\" commands are enabled for SEMP over the message bus in the Message VPN.")
  public Boolean isSempOverMsgBusAdminClientEnabled() {
    return sempOverMsgBusAdminClientEnabled;
  }

  public void setSempOverMsgBusAdminClientEnabled(Boolean sempOverMsgBusAdminClientEnabled) {
    this.sempOverMsgBusAdminClientEnabled = sempOverMsgBusAdminClientEnabled;
  }

  public MsgVpn sempOverMsgBusAdminDistributedCacheEnabled(Boolean sempOverMsgBusAdminDistributedCacheEnabled) {
    this.sempOverMsgBusAdminDistributedCacheEnabled = sempOverMsgBusAdminDistributedCacheEnabled;
    return this;
  }

   /**
   * Indicates whether the \&quot;admin\&quot; level \&quot;Distributed Cache\&quot; commands are enabled for SEMP over the message bus in the Message VPN.
   * @return sempOverMsgBusAdminDistributedCacheEnabled
  **/
  @Schema(description = "Indicates whether the \"admin\" level \"Distributed Cache\" commands are enabled for SEMP over the message bus in the Message VPN.")
  public Boolean isSempOverMsgBusAdminDistributedCacheEnabled() {
    return sempOverMsgBusAdminDistributedCacheEnabled;
  }

  public void setSempOverMsgBusAdminDistributedCacheEnabled(Boolean sempOverMsgBusAdminDistributedCacheEnabled) {
    this.sempOverMsgBusAdminDistributedCacheEnabled = sempOverMsgBusAdminDistributedCacheEnabled;
  }

  public MsgVpn sempOverMsgBusAdminEnabled(Boolean sempOverMsgBusAdminEnabled) {
    this.sempOverMsgBusAdminEnabled = sempOverMsgBusAdminEnabled;
    return this;
  }

   /**
   * Indicates whether the \&quot;admin\&quot; level commands are enabled for SEMP over the message bus in the Message VPN.
   * @return sempOverMsgBusAdminEnabled
  **/
  @Schema(description = "Indicates whether the \"admin\" level commands are enabled for SEMP over the message bus in the Message VPN.")
  public Boolean isSempOverMsgBusAdminEnabled() {
    return sempOverMsgBusAdminEnabled;
  }

  public void setSempOverMsgBusAdminEnabled(Boolean sempOverMsgBusAdminEnabled) {
    this.sempOverMsgBusAdminEnabled = sempOverMsgBusAdminEnabled;
  }

  public MsgVpn sempOverMsgBusEnabled(Boolean sempOverMsgBusEnabled) {
    this.sempOverMsgBusEnabled = sempOverMsgBusEnabled;
    return this;
  }

   /**
   * Indicates whether SEMP over the message bus is enabled in the Message VPN.
   * @return sempOverMsgBusEnabled
  **/
  @Schema(description = "Indicates whether SEMP over the message bus is enabled in the Message VPN.")
  public Boolean isSempOverMsgBusEnabled() {
    return sempOverMsgBusEnabled;
  }

  public void setSempOverMsgBusEnabled(Boolean sempOverMsgBusEnabled) {
    this.sempOverMsgBusEnabled = sempOverMsgBusEnabled;
  }

  public MsgVpn sempOverMsgBusShowEnabled(Boolean sempOverMsgBusShowEnabled) {
    this.sempOverMsgBusShowEnabled = sempOverMsgBusShowEnabled;
    return this;
  }

   /**
   * Indicates whether the \&quot;show\&quot; level commands are enabled for SEMP over the message bus in the Message VPN.
   * @return sempOverMsgBusShowEnabled
  **/
  @Schema(description = "Indicates whether the \"show\" level commands are enabled for SEMP over the message bus in the Message VPN.")
  public Boolean isSempOverMsgBusShowEnabled() {
    return sempOverMsgBusShowEnabled;
  }

  public void setSempOverMsgBusShowEnabled(Boolean sempOverMsgBusShowEnabled) {
    this.sempOverMsgBusShowEnabled = sempOverMsgBusShowEnabled;
  }

  public MsgVpn serviceAmqpMaxConnectionCount(Long serviceAmqpMaxConnectionCount) {
    this.serviceAmqpMaxConnectionCount = serviceAmqpMaxConnectionCount;
    return this;
  }

   /**
   * The maximum number of AMQP client connections that can be simultaneously connected to the Message VPN. This value may be higher than supported by the platform.
   * @return serviceAmqpMaxConnectionCount
  **/
  @Schema(description = "The maximum number of AMQP client connections that can be simultaneously connected to the Message VPN. This value may be higher than supported by the platform.")
  public Long getServiceAmqpMaxConnectionCount() {
    return serviceAmqpMaxConnectionCount;
  }

  public void setServiceAmqpMaxConnectionCount(Long serviceAmqpMaxConnectionCount) {
    this.serviceAmqpMaxConnectionCount = serviceAmqpMaxConnectionCount;
  }

  public MsgVpn serviceAmqpPlainTextCompressed(Boolean serviceAmqpPlainTextCompressed) {
    this.serviceAmqpPlainTextCompressed = serviceAmqpPlainTextCompressed;
    return this;
  }

   /**
   * Indicates whether the AMQP Service is compressed in the Message VPN.
   * @return serviceAmqpPlainTextCompressed
  **/
  @Schema(description = "Indicates whether the AMQP Service is compressed in the Message VPN.")
  public Boolean isServiceAmqpPlainTextCompressed() {
    return serviceAmqpPlainTextCompressed;
  }

  public void setServiceAmqpPlainTextCompressed(Boolean serviceAmqpPlainTextCompressed) {
    this.serviceAmqpPlainTextCompressed = serviceAmqpPlainTextCompressed;
  }

  public MsgVpn serviceAmqpPlainTextEnabled(Boolean serviceAmqpPlainTextEnabled) {
    this.serviceAmqpPlainTextEnabled = serviceAmqpPlainTextEnabled;
    return this;
  }

   /**
   * Indicates whether the AMQP Service is enabled in the Message VPN.
   * @return serviceAmqpPlainTextEnabled
  **/
  @Schema(description = "Indicates whether the AMQP Service is enabled in the Message VPN.")
  public Boolean isServiceAmqpPlainTextEnabled() {
    return serviceAmqpPlainTextEnabled;
  }

  public void setServiceAmqpPlainTextEnabled(Boolean serviceAmqpPlainTextEnabled) {
    this.serviceAmqpPlainTextEnabled = serviceAmqpPlainTextEnabled;
  }

  public MsgVpn serviceAmqpPlainTextFailureReason(String serviceAmqpPlainTextFailureReason) {
    this.serviceAmqpPlainTextFailureReason = serviceAmqpPlainTextFailureReason;
    return this;
  }

   /**
   * The reason for the AMQP Service failure in the Message VPN.
   * @return serviceAmqpPlainTextFailureReason
  **/
  @Schema(description = "The reason for the AMQP Service failure in the Message VPN.")
  public String getServiceAmqpPlainTextFailureReason() {
    return serviceAmqpPlainTextFailureReason;
  }

  public void setServiceAmqpPlainTextFailureReason(String serviceAmqpPlainTextFailureReason) {
    this.serviceAmqpPlainTextFailureReason = serviceAmqpPlainTextFailureReason;
  }

  public MsgVpn serviceAmqpPlainTextListenPort(Long serviceAmqpPlainTextListenPort) {
    this.serviceAmqpPlainTextListenPort = serviceAmqpPlainTextListenPort;
    return this;
  }

   /**
   * The port number for plain-text AMQP clients that connect to the Message VPN. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.
   * @return serviceAmqpPlainTextListenPort
  **/
  @Schema(description = "The port number for plain-text AMQP clients that connect to the Message VPN. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.")
  public Long getServiceAmqpPlainTextListenPort() {
    return serviceAmqpPlainTextListenPort;
  }

  public void setServiceAmqpPlainTextListenPort(Long serviceAmqpPlainTextListenPort) {
    this.serviceAmqpPlainTextListenPort = serviceAmqpPlainTextListenPort;
  }

  public MsgVpn serviceAmqpPlainTextUp(Boolean serviceAmqpPlainTextUp) {
    this.serviceAmqpPlainTextUp = serviceAmqpPlainTextUp;
    return this;
  }

   /**
   * Indicates whether the AMQP Service is operationally up in the Message VPN.
   * @return serviceAmqpPlainTextUp
  **/
  @Schema(description = "Indicates whether the AMQP Service is operationally up in the Message VPN.")
  public Boolean isServiceAmqpPlainTextUp() {
    return serviceAmqpPlainTextUp;
  }

  public void setServiceAmqpPlainTextUp(Boolean serviceAmqpPlainTextUp) {
    this.serviceAmqpPlainTextUp = serviceAmqpPlainTextUp;
  }

  public MsgVpn serviceAmqpTlsCompressed(Boolean serviceAmqpTlsCompressed) {
    this.serviceAmqpTlsCompressed = serviceAmqpTlsCompressed;
    return this;
  }

   /**
   * Indicates whether the TLS related AMQP Service is compressed in the Message VPN.
   * @return serviceAmqpTlsCompressed
  **/
  @Schema(description = "Indicates whether the TLS related AMQP Service is compressed in the Message VPN.")
  public Boolean isServiceAmqpTlsCompressed() {
    return serviceAmqpTlsCompressed;
  }

  public void setServiceAmqpTlsCompressed(Boolean serviceAmqpTlsCompressed) {
    this.serviceAmqpTlsCompressed = serviceAmqpTlsCompressed;
  }

  public MsgVpn serviceAmqpTlsEnabled(Boolean serviceAmqpTlsEnabled) {
    this.serviceAmqpTlsEnabled = serviceAmqpTlsEnabled;
    return this;
  }

   /**
   * Indicates whether encryption (TLS) is enabled for AMQP clients in the Message VPN.
   * @return serviceAmqpTlsEnabled
  **/
  @Schema(description = "Indicates whether encryption (TLS) is enabled for AMQP clients in the Message VPN.")
  public Boolean isServiceAmqpTlsEnabled() {
    return serviceAmqpTlsEnabled;
  }

  public void setServiceAmqpTlsEnabled(Boolean serviceAmqpTlsEnabled) {
    this.serviceAmqpTlsEnabled = serviceAmqpTlsEnabled;
  }

  public MsgVpn serviceAmqpTlsFailureReason(String serviceAmqpTlsFailureReason) {
    this.serviceAmqpTlsFailureReason = serviceAmqpTlsFailureReason;
    return this;
  }

   /**
   * The reason for the TLS related AMQP Service failure in the Message VPN.
   * @return serviceAmqpTlsFailureReason
  **/
  @Schema(description = "The reason for the TLS related AMQP Service failure in the Message VPN.")
  public String getServiceAmqpTlsFailureReason() {
    return serviceAmqpTlsFailureReason;
  }

  public void setServiceAmqpTlsFailureReason(String serviceAmqpTlsFailureReason) {
    this.serviceAmqpTlsFailureReason = serviceAmqpTlsFailureReason;
  }

  public MsgVpn serviceAmqpTlsListenPort(Long serviceAmqpTlsListenPort) {
    this.serviceAmqpTlsListenPort = serviceAmqpTlsListenPort;
    return this;
  }

   /**
   * The port number for AMQP clients that connect to the Message VPN over TLS. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.
   * @return serviceAmqpTlsListenPort
  **/
  @Schema(description = "The port number for AMQP clients that connect to the Message VPN over TLS. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.")
  public Long getServiceAmqpTlsListenPort() {
    return serviceAmqpTlsListenPort;
  }

  public void setServiceAmqpTlsListenPort(Long serviceAmqpTlsListenPort) {
    this.serviceAmqpTlsListenPort = serviceAmqpTlsListenPort;
  }

  public MsgVpn serviceAmqpTlsUp(Boolean serviceAmqpTlsUp) {
    this.serviceAmqpTlsUp = serviceAmqpTlsUp;
    return this;
  }

   /**
   * Indicates whether the TLS related AMQP Service is operationally up in the Message VPN.
   * @return serviceAmqpTlsUp
  **/
  @Schema(description = "Indicates whether the TLS related AMQP Service is operationally up in the Message VPN.")
  public Boolean isServiceAmqpTlsUp() {
    return serviceAmqpTlsUp;
  }

  public void setServiceAmqpTlsUp(Boolean serviceAmqpTlsUp) {
    this.serviceAmqpTlsUp = serviceAmqpTlsUp;
  }

  public MsgVpn serviceMqttMaxConnectionCount(Long serviceMqttMaxConnectionCount) {
    this.serviceMqttMaxConnectionCount = serviceMqttMaxConnectionCount;
    return this;
  }

   /**
   * The maximum number of MQTT client connections that can be simultaneously connected to the Message VPN.
   * @return serviceMqttMaxConnectionCount
  **/
  @Schema(description = "The maximum number of MQTT client connections that can be simultaneously connected to the Message VPN.")
  public Long getServiceMqttMaxConnectionCount() {
    return serviceMqttMaxConnectionCount;
  }

  public void setServiceMqttMaxConnectionCount(Long serviceMqttMaxConnectionCount) {
    this.serviceMqttMaxConnectionCount = serviceMqttMaxConnectionCount;
  }

  public MsgVpn serviceMqttPlainTextCompressed(Boolean serviceMqttPlainTextCompressed) {
    this.serviceMqttPlainTextCompressed = serviceMqttPlainTextCompressed;
    return this;
  }

   /**
   * Indicates whether the MQTT Service is compressed in the Message VPN.
   * @return serviceMqttPlainTextCompressed
  **/
  @Schema(description = "Indicates whether the MQTT Service is compressed in the Message VPN.")
  public Boolean isServiceMqttPlainTextCompressed() {
    return serviceMqttPlainTextCompressed;
  }

  public void setServiceMqttPlainTextCompressed(Boolean serviceMqttPlainTextCompressed) {
    this.serviceMqttPlainTextCompressed = serviceMqttPlainTextCompressed;
  }

  public MsgVpn serviceMqttPlainTextEnabled(Boolean serviceMqttPlainTextEnabled) {
    this.serviceMqttPlainTextEnabled = serviceMqttPlainTextEnabled;
    return this;
  }

   /**
   * Indicates whether the MQTT Service is enabled in the Message VPN.
   * @return serviceMqttPlainTextEnabled
  **/
  @Schema(description = "Indicates whether the MQTT Service is enabled in the Message VPN.")
  public Boolean isServiceMqttPlainTextEnabled() {
    return serviceMqttPlainTextEnabled;
  }

  public void setServiceMqttPlainTextEnabled(Boolean serviceMqttPlainTextEnabled) {
    this.serviceMqttPlainTextEnabled = serviceMqttPlainTextEnabled;
  }

  public MsgVpn serviceMqttPlainTextFailureReason(String serviceMqttPlainTextFailureReason) {
    this.serviceMqttPlainTextFailureReason = serviceMqttPlainTextFailureReason;
    return this;
  }

   /**
   * The reason for the MQTT Service failure in the Message VPN.
   * @return serviceMqttPlainTextFailureReason
  **/
  @Schema(description = "The reason for the MQTT Service failure in the Message VPN.")
  public String getServiceMqttPlainTextFailureReason() {
    return serviceMqttPlainTextFailureReason;
  }

  public void setServiceMqttPlainTextFailureReason(String serviceMqttPlainTextFailureReason) {
    this.serviceMqttPlainTextFailureReason = serviceMqttPlainTextFailureReason;
  }

  public MsgVpn serviceMqttPlainTextListenPort(Long serviceMqttPlainTextListenPort) {
    this.serviceMqttPlainTextListenPort = serviceMqttPlainTextListenPort;
    return this;
  }

   /**
   * The port number for plain-text MQTT clients that connect to the Message VPN. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.
   * @return serviceMqttPlainTextListenPort
  **/
  @Schema(description = "The port number for plain-text MQTT clients that connect to the Message VPN. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.")
  public Long getServiceMqttPlainTextListenPort() {
    return serviceMqttPlainTextListenPort;
  }

  public void setServiceMqttPlainTextListenPort(Long serviceMqttPlainTextListenPort) {
    this.serviceMqttPlainTextListenPort = serviceMqttPlainTextListenPort;
  }

  public MsgVpn serviceMqttPlainTextUp(Boolean serviceMqttPlainTextUp) {
    this.serviceMqttPlainTextUp = serviceMqttPlainTextUp;
    return this;
  }

   /**
   * Indicates whether the MQTT Service is operationally up in the Message VPN.
   * @return serviceMqttPlainTextUp
  **/
  @Schema(description = "Indicates whether the MQTT Service is operationally up in the Message VPN.")
  public Boolean isServiceMqttPlainTextUp() {
    return serviceMqttPlainTextUp;
  }

  public void setServiceMqttPlainTextUp(Boolean serviceMqttPlainTextUp) {
    this.serviceMqttPlainTextUp = serviceMqttPlainTextUp;
  }

  public MsgVpn serviceMqttTlsCompressed(Boolean serviceMqttTlsCompressed) {
    this.serviceMqttTlsCompressed = serviceMqttTlsCompressed;
    return this;
  }

   /**
   * Indicates whether the TLS related MQTT Service is compressed in the Message VPN.
   * @return serviceMqttTlsCompressed
  **/
  @Schema(description = "Indicates whether the TLS related MQTT Service is compressed in the Message VPN.")
  public Boolean isServiceMqttTlsCompressed() {
    return serviceMqttTlsCompressed;
  }

  public void setServiceMqttTlsCompressed(Boolean serviceMqttTlsCompressed) {
    this.serviceMqttTlsCompressed = serviceMqttTlsCompressed;
  }

  public MsgVpn serviceMqttTlsEnabled(Boolean serviceMqttTlsEnabled) {
    this.serviceMqttTlsEnabled = serviceMqttTlsEnabled;
    return this;
  }

   /**
   * Indicates whether encryption (TLS) is enabled for MQTT clients in the Message VPN.
   * @return serviceMqttTlsEnabled
  **/
  @Schema(description = "Indicates whether encryption (TLS) is enabled for MQTT clients in the Message VPN.")
  public Boolean isServiceMqttTlsEnabled() {
    return serviceMqttTlsEnabled;
  }

  public void setServiceMqttTlsEnabled(Boolean serviceMqttTlsEnabled) {
    this.serviceMqttTlsEnabled = serviceMqttTlsEnabled;
  }

  public MsgVpn serviceMqttTlsFailureReason(String serviceMqttTlsFailureReason) {
    this.serviceMqttTlsFailureReason = serviceMqttTlsFailureReason;
    return this;
  }

   /**
   * The reason for the TLS related MQTT Service failure in the Message VPN.
   * @return serviceMqttTlsFailureReason
  **/
  @Schema(description = "The reason for the TLS related MQTT Service failure in the Message VPN.")
  public String getServiceMqttTlsFailureReason() {
    return serviceMqttTlsFailureReason;
  }

  public void setServiceMqttTlsFailureReason(String serviceMqttTlsFailureReason) {
    this.serviceMqttTlsFailureReason = serviceMqttTlsFailureReason;
  }

  public MsgVpn serviceMqttTlsListenPort(Long serviceMqttTlsListenPort) {
    this.serviceMqttTlsListenPort = serviceMqttTlsListenPort;
    return this;
  }

   /**
   * The port number for MQTT clients that connect to the Message VPN over TLS. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.
   * @return serviceMqttTlsListenPort
  **/
  @Schema(description = "The port number for MQTT clients that connect to the Message VPN over TLS. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.")
  public Long getServiceMqttTlsListenPort() {
    return serviceMqttTlsListenPort;
  }

  public void setServiceMqttTlsListenPort(Long serviceMqttTlsListenPort) {
    this.serviceMqttTlsListenPort = serviceMqttTlsListenPort;
  }

  public MsgVpn serviceMqttTlsUp(Boolean serviceMqttTlsUp) {
    this.serviceMqttTlsUp = serviceMqttTlsUp;
    return this;
  }

   /**
   * Indicates whether the TLS related MQTT Service is operationally up in the Message VPN.
   * @return serviceMqttTlsUp
  **/
  @Schema(description = "Indicates whether the TLS related MQTT Service is operationally up in the Message VPN.")
  public Boolean isServiceMqttTlsUp() {
    return serviceMqttTlsUp;
  }

  public void setServiceMqttTlsUp(Boolean serviceMqttTlsUp) {
    this.serviceMqttTlsUp = serviceMqttTlsUp;
  }

  public MsgVpn serviceMqttTlsWebSocketCompressed(Boolean serviceMqttTlsWebSocketCompressed) {
    this.serviceMqttTlsWebSocketCompressed = serviceMqttTlsWebSocketCompressed;
    return this;
  }

   /**
   * Indicates whether the TLS related Web transport MQTT Service is compressed in the Message VPN.
   * @return serviceMqttTlsWebSocketCompressed
  **/
  @Schema(description = "Indicates whether the TLS related Web transport MQTT Service is compressed in the Message VPN.")
  public Boolean isServiceMqttTlsWebSocketCompressed() {
    return serviceMqttTlsWebSocketCompressed;
  }

  public void setServiceMqttTlsWebSocketCompressed(Boolean serviceMqttTlsWebSocketCompressed) {
    this.serviceMqttTlsWebSocketCompressed = serviceMqttTlsWebSocketCompressed;
  }

  public MsgVpn serviceMqttTlsWebSocketEnabled(Boolean serviceMqttTlsWebSocketEnabled) {
    this.serviceMqttTlsWebSocketEnabled = serviceMqttTlsWebSocketEnabled;
    return this;
  }

   /**
   * Indicates whether encryption (TLS) is enabled for MQTT Web clients in the Message VPN.
   * @return serviceMqttTlsWebSocketEnabled
  **/
  @Schema(description = "Indicates whether encryption (TLS) is enabled for MQTT Web clients in the Message VPN.")
  public Boolean isServiceMqttTlsWebSocketEnabled() {
    return serviceMqttTlsWebSocketEnabled;
  }

  public void setServiceMqttTlsWebSocketEnabled(Boolean serviceMqttTlsWebSocketEnabled) {
    this.serviceMqttTlsWebSocketEnabled = serviceMqttTlsWebSocketEnabled;
  }

  public MsgVpn serviceMqttTlsWebSocketFailureReason(String serviceMqttTlsWebSocketFailureReason) {
    this.serviceMqttTlsWebSocketFailureReason = serviceMqttTlsWebSocketFailureReason;
    return this;
  }

   /**
   * The reason for the TLS related Web transport MQTT Service failure in the Message VPN.
   * @return serviceMqttTlsWebSocketFailureReason
  **/
  @Schema(description = "The reason for the TLS related Web transport MQTT Service failure in the Message VPN.")
  public String getServiceMqttTlsWebSocketFailureReason() {
    return serviceMqttTlsWebSocketFailureReason;
  }

  public void setServiceMqttTlsWebSocketFailureReason(String serviceMqttTlsWebSocketFailureReason) {
    this.serviceMqttTlsWebSocketFailureReason = serviceMqttTlsWebSocketFailureReason;
  }

  public MsgVpn serviceMqttTlsWebSocketListenPort(Long serviceMqttTlsWebSocketListenPort) {
    this.serviceMqttTlsWebSocketListenPort = serviceMqttTlsWebSocketListenPort;
    return this;
  }

   /**
   * The port number for MQTT clients that connect to the Message VPN using WebSocket over TLS. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.
   * @return serviceMqttTlsWebSocketListenPort
  **/
  @Schema(description = "The port number for MQTT clients that connect to the Message VPN using WebSocket over TLS. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.")
  public Long getServiceMqttTlsWebSocketListenPort() {
    return serviceMqttTlsWebSocketListenPort;
  }

  public void setServiceMqttTlsWebSocketListenPort(Long serviceMqttTlsWebSocketListenPort) {
    this.serviceMqttTlsWebSocketListenPort = serviceMqttTlsWebSocketListenPort;
  }

  public MsgVpn serviceMqttTlsWebSocketUp(Boolean serviceMqttTlsWebSocketUp) {
    this.serviceMqttTlsWebSocketUp = serviceMqttTlsWebSocketUp;
    return this;
  }

   /**
   * Indicates whether the TLS related Web transport MQTT Service is operationally up in the Message VPN.
   * @return serviceMqttTlsWebSocketUp
  **/
  @Schema(description = "Indicates whether the TLS related Web transport MQTT Service is operationally up in the Message VPN.")
  public Boolean isServiceMqttTlsWebSocketUp() {
    return serviceMqttTlsWebSocketUp;
  }

  public void setServiceMqttTlsWebSocketUp(Boolean serviceMqttTlsWebSocketUp) {
    this.serviceMqttTlsWebSocketUp = serviceMqttTlsWebSocketUp;
  }

  public MsgVpn serviceMqttWebSocketCompressed(Boolean serviceMqttWebSocketCompressed) {
    this.serviceMqttWebSocketCompressed = serviceMqttWebSocketCompressed;
    return this;
  }

   /**
   * Indicates whether the Web transport related MQTT Service is compressed in the Message VPN.
   * @return serviceMqttWebSocketCompressed
  **/
  @Schema(description = "Indicates whether the Web transport related MQTT Service is compressed in the Message VPN.")
  public Boolean isServiceMqttWebSocketCompressed() {
    return serviceMqttWebSocketCompressed;
  }

  public void setServiceMqttWebSocketCompressed(Boolean serviceMqttWebSocketCompressed) {
    this.serviceMqttWebSocketCompressed = serviceMqttWebSocketCompressed;
  }

  public MsgVpn serviceMqttWebSocketEnabled(Boolean serviceMqttWebSocketEnabled) {
    this.serviceMqttWebSocketEnabled = serviceMqttWebSocketEnabled;
    return this;
  }

   /**
   * Indicates whether the Web transport for the SMF Service is enabled in the Message VPN.
   * @return serviceMqttWebSocketEnabled
  **/
  @Schema(description = "Indicates whether the Web transport for the SMF Service is enabled in the Message VPN.")
  public Boolean isServiceMqttWebSocketEnabled() {
    return serviceMqttWebSocketEnabled;
  }

  public void setServiceMqttWebSocketEnabled(Boolean serviceMqttWebSocketEnabled) {
    this.serviceMqttWebSocketEnabled = serviceMqttWebSocketEnabled;
  }

  public MsgVpn serviceMqttWebSocketFailureReason(String serviceMqttWebSocketFailureReason) {
    this.serviceMqttWebSocketFailureReason = serviceMqttWebSocketFailureReason;
    return this;
  }

   /**
   * The reason for the Web transport related MQTT Service failure in the Message VPN.
   * @return serviceMqttWebSocketFailureReason
  **/
  @Schema(description = "The reason for the Web transport related MQTT Service failure in the Message VPN.")
  public String getServiceMqttWebSocketFailureReason() {
    return serviceMqttWebSocketFailureReason;
  }

  public void setServiceMqttWebSocketFailureReason(String serviceMqttWebSocketFailureReason) {
    this.serviceMqttWebSocketFailureReason = serviceMqttWebSocketFailureReason;
  }

  public MsgVpn serviceMqttWebSocketListenPort(Long serviceMqttWebSocketListenPort) {
    this.serviceMqttWebSocketListenPort = serviceMqttWebSocketListenPort;
    return this;
  }

   /**
   * The port number for plain-text MQTT clients that connect to the Message VPN using WebSocket. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.
   * @return serviceMqttWebSocketListenPort
  **/
  @Schema(description = "The port number for plain-text MQTT clients that connect to the Message VPN using WebSocket. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.")
  public Long getServiceMqttWebSocketListenPort() {
    return serviceMqttWebSocketListenPort;
  }

  public void setServiceMqttWebSocketListenPort(Long serviceMqttWebSocketListenPort) {
    this.serviceMqttWebSocketListenPort = serviceMqttWebSocketListenPort;
  }

  public MsgVpn serviceMqttWebSocketUp(Boolean serviceMqttWebSocketUp) {
    this.serviceMqttWebSocketUp = serviceMqttWebSocketUp;
    return this;
  }

   /**
   * Indicates whether the Web transport related MQTT Service is operationally up in the Message VPN.
   * @return serviceMqttWebSocketUp
  **/
  @Schema(description = "Indicates whether the Web transport related MQTT Service is operationally up in the Message VPN.")
  public Boolean isServiceMqttWebSocketUp() {
    return serviceMqttWebSocketUp;
  }

  public void setServiceMqttWebSocketUp(Boolean serviceMqttWebSocketUp) {
    this.serviceMqttWebSocketUp = serviceMqttWebSocketUp;
  }

  public MsgVpn serviceRestIncomingMaxConnectionCount(Long serviceRestIncomingMaxConnectionCount) {
    this.serviceRestIncomingMaxConnectionCount = serviceRestIncomingMaxConnectionCount;
    return this;
  }

   /**
   * The maximum number of REST incoming client connections that can be simultaneously connected to the Message VPN. This value may be higher than supported by the platform.
   * @return serviceRestIncomingMaxConnectionCount
  **/
  @Schema(description = "The maximum number of REST incoming client connections that can be simultaneously connected to the Message VPN. This value may be higher than supported by the platform.")
  public Long getServiceRestIncomingMaxConnectionCount() {
    return serviceRestIncomingMaxConnectionCount;
  }

  public void setServiceRestIncomingMaxConnectionCount(Long serviceRestIncomingMaxConnectionCount) {
    this.serviceRestIncomingMaxConnectionCount = serviceRestIncomingMaxConnectionCount;
  }

  public MsgVpn serviceRestIncomingPlainTextCompressed(Boolean serviceRestIncomingPlainTextCompressed) {
    this.serviceRestIncomingPlainTextCompressed = serviceRestIncomingPlainTextCompressed;
    return this;
  }

   /**
   * Indicates whether the incoming REST Service is compressed in the Message VPN.
   * @return serviceRestIncomingPlainTextCompressed
  **/
  @Schema(description = "Indicates whether the incoming REST Service is compressed in the Message VPN.")
  public Boolean isServiceRestIncomingPlainTextCompressed() {
    return serviceRestIncomingPlainTextCompressed;
  }

  public void setServiceRestIncomingPlainTextCompressed(Boolean serviceRestIncomingPlainTextCompressed) {
    this.serviceRestIncomingPlainTextCompressed = serviceRestIncomingPlainTextCompressed;
  }

  public MsgVpn serviceRestIncomingPlainTextEnabled(Boolean serviceRestIncomingPlainTextEnabled) {
    this.serviceRestIncomingPlainTextEnabled = serviceRestIncomingPlainTextEnabled;
    return this;
  }

   /**
   * Indicates whether the REST Service is enabled in the Message VPN for incoming clients.
   * @return serviceRestIncomingPlainTextEnabled
  **/
  @Schema(description = "Indicates whether the REST Service is enabled in the Message VPN for incoming clients.")
  public Boolean isServiceRestIncomingPlainTextEnabled() {
    return serviceRestIncomingPlainTextEnabled;
  }

  public void setServiceRestIncomingPlainTextEnabled(Boolean serviceRestIncomingPlainTextEnabled) {
    this.serviceRestIncomingPlainTextEnabled = serviceRestIncomingPlainTextEnabled;
  }

  public MsgVpn serviceRestIncomingPlainTextFailureReason(String serviceRestIncomingPlainTextFailureReason) {
    this.serviceRestIncomingPlainTextFailureReason = serviceRestIncomingPlainTextFailureReason;
    return this;
  }

   /**
   * The reason for the incoming REST Service failure in the Message VPN.
   * @return serviceRestIncomingPlainTextFailureReason
  **/
  @Schema(description = "The reason for the incoming REST Service failure in the Message VPN.")
  public String getServiceRestIncomingPlainTextFailureReason() {
    return serviceRestIncomingPlainTextFailureReason;
  }

  public void setServiceRestIncomingPlainTextFailureReason(String serviceRestIncomingPlainTextFailureReason) {
    this.serviceRestIncomingPlainTextFailureReason = serviceRestIncomingPlainTextFailureReason;
  }

  public MsgVpn serviceRestIncomingPlainTextListenPort(Long serviceRestIncomingPlainTextListenPort) {
    this.serviceRestIncomingPlainTextListenPort = serviceRestIncomingPlainTextListenPort;
    return this;
  }

   /**
   * The port number for incoming plain-text REST clients that connect to the Message VPN. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.
   * @return serviceRestIncomingPlainTextListenPort
  **/
  @Schema(description = "The port number for incoming plain-text REST clients that connect to the Message VPN. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.")
  public Long getServiceRestIncomingPlainTextListenPort() {
    return serviceRestIncomingPlainTextListenPort;
  }

  public void setServiceRestIncomingPlainTextListenPort(Long serviceRestIncomingPlainTextListenPort) {
    this.serviceRestIncomingPlainTextListenPort = serviceRestIncomingPlainTextListenPort;
  }

  public MsgVpn serviceRestIncomingPlainTextUp(Boolean serviceRestIncomingPlainTextUp) {
    this.serviceRestIncomingPlainTextUp = serviceRestIncomingPlainTextUp;
    return this;
  }

   /**
   * Indicates whether the incoming REST Service is operationally up in the Message VPN.
   * @return serviceRestIncomingPlainTextUp
  **/
  @Schema(description = "Indicates whether the incoming REST Service is operationally up in the Message VPN.")
  public Boolean isServiceRestIncomingPlainTextUp() {
    return serviceRestIncomingPlainTextUp;
  }

  public void setServiceRestIncomingPlainTextUp(Boolean serviceRestIncomingPlainTextUp) {
    this.serviceRestIncomingPlainTextUp = serviceRestIncomingPlainTextUp;
  }

  public MsgVpn serviceRestIncomingTlsCompressed(Boolean serviceRestIncomingTlsCompressed) {
    this.serviceRestIncomingTlsCompressed = serviceRestIncomingTlsCompressed;
    return this;
  }

   /**
   * Indicates whether the TLS related incoming REST Service is compressed in the Message VPN.
   * @return serviceRestIncomingTlsCompressed
  **/
  @Schema(description = "Indicates whether the TLS related incoming REST Service is compressed in the Message VPN.")
  public Boolean isServiceRestIncomingTlsCompressed() {
    return serviceRestIncomingTlsCompressed;
  }

  public void setServiceRestIncomingTlsCompressed(Boolean serviceRestIncomingTlsCompressed) {
    this.serviceRestIncomingTlsCompressed = serviceRestIncomingTlsCompressed;
  }

  public MsgVpn serviceRestIncomingTlsEnabled(Boolean serviceRestIncomingTlsEnabled) {
    this.serviceRestIncomingTlsEnabled = serviceRestIncomingTlsEnabled;
    return this;
  }

   /**
   * Indicates whether encryption (TLS) is enabled for incoming REST clients in the Message VPN.
   * @return serviceRestIncomingTlsEnabled
  **/
  @Schema(description = "Indicates whether encryption (TLS) is enabled for incoming REST clients in the Message VPN.")
  public Boolean isServiceRestIncomingTlsEnabled() {
    return serviceRestIncomingTlsEnabled;
  }

  public void setServiceRestIncomingTlsEnabled(Boolean serviceRestIncomingTlsEnabled) {
    this.serviceRestIncomingTlsEnabled = serviceRestIncomingTlsEnabled;
  }

  public MsgVpn serviceRestIncomingTlsFailureReason(String serviceRestIncomingTlsFailureReason) {
    this.serviceRestIncomingTlsFailureReason = serviceRestIncomingTlsFailureReason;
    return this;
  }

   /**
   * The reason for the TLS related incoming REST Service failure in the Message VPN.
   * @return serviceRestIncomingTlsFailureReason
  **/
  @Schema(description = "The reason for the TLS related incoming REST Service failure in the Message VPN.")
  public String getServiceRestIncomingTlsFailureReason() {
    return serviceRestIncomingTlsFailureReason;
  }

  public void setServiceRestIncomingTlsFailureReason(String serviceRestIncomingTlsFailureReason) {
    this.serviceRestIncomingTlsFailureReason = serviceRestIncomingTlsFailureReason;
  }

  public MsgVpn serviceRestIncomingTlsListenPort(Long serviceRestIncomingTlsListenPort) {
    this.serviceRestIncomingTlsListenPort = serviceRestIncomingTlsListenPort;
    return this;
  }

   /**
   * The port number for incoming REST clients that connect to the Message VPN over TLS. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.
   * @return serviceRestIncomingTlsListenPort
  **/
  @Schema(description = "The port number for incoming REST clients that connect to the Message VPN over TLS. The port must be unique across the message backbone. A value of 0 means that the listen-port is unassigned and cannot be enabled.")
  public Long getServiceRestIncomingTlsListenPort() {
    return serviceRestIncomingTlsListenPort;
  }

  public void setServiceRestIncomingTlsListenPort(Long serviceRestIncomingTlsListenPort) {
    this.serviceRestIncomingTlsListenPort = serviceRestIncomingTlsListenPort;
  }

  public MsgVpn serviceRestIncomingTlsUp(Boolean serviceRestIncomingTlsUp) {
    this.serviceRestIncomingTlsUp = serviceRestIncomingTlsUp;
    return this;
  }

   /**
   * Indicates whether the TLS related incoming REST Service is operationally up in the Message VPN.
   * @return serviceRestIncomingTlsUp
  **/
  @Schema(description = "Indicates whether the TLS related incoming REST Service is operationally up in the Message VPN.")
  public Boolean isServiceRestIncomingTlsUp() {
    return serviceRestIncomingTlsUp;
  }

  public void setServiceRestIncomingTlsUp(Boolean serviceRestIncomingTlsUp) {
    this.serviceRestIncomingTlsUp = serviceRestIncomingTlsUp;
  }

  public MsgVpn serviceRestMode(ServiceRestModeEnum serviceRestMode) {
    this.serviceRestMode = serviceRestMode;
    return this;
  }

   /**
   * The REST service mode for incoming REST clients that connect to the Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;gateway\&quot; - Act as a message gateway through which REST messages are propagated. \&quot;messaging\&quot; - Act as a message broker on which REST messages are queued. &lt;/pre&gt; 
   * @return serviceRestMode
  **/
  @Schema(description = "The REST service mode for incoming REST clients that connect to the Message VPN. The allowed values and their meaning are:  <pre> \"gateway\" - Act as a message gateway through which REST messages are propagated. \"messaging\" - Act as a message broker on which REST messages are queued. </pre> ")
  public ServiceRestModeEnum getServiceRestMode() {
    return serviceRestMode;
  }

  public void setServiceRestMode(ServiceRestModeEnum serviceRestMode) {
    this.serviceRestMode = serviceRestMode;
  }

  public MsgVpn serviceRestOutgoingMaxConnectionCount(Long serviceRestOutgoingMaxConnectionCount) {
    this.serviceRestOutgoingMaxConnectionCount = serviceRestOutgoingMaxConnectionCount;
    return this;
  }

   /**
   * The maximum number of REST Consumer (outgoing) client connections that can be simultaneously connected to the Message VPN.
   * @return serviceRestOutgoingMaxConnectionCount
  **/
  @Schema(description = "The maximum number of REST Consumer (outgoing) client connections that can be simultaneously connected to the Message VPN.")
  public Long getServiceRestOutgoingMaxConnectionCount() {
    return serviceRestOutgoingMaxConnectionCount;
  }

  public void setServiceRestOutgoingMaxConnectionCount(Long serviceRestOutgoingMaxConnectionCount) {
    this.serviceRestOutgoingMaxConnectionCount = serviceRestOutgoingMaxConnectionCount;
  }

  public MsgVpn serviceSmfMaxConnectionCount(Long serviceSmfMaxConnectionCount) {
    this.serviceSmfMaxConnectionCount = serviceSmfMaxConnectionCount;
    return this;
  }

   /**
   * The maximum number of SMF client connections that can be simultaneously connected to the Message VPN. This value may be higher than supported by the platform.
   * @return serviceSmfMaxConnectionCount
  **/
  @Schema(description = "The maximum number of SMF client connections that can be simultaneously connected to the Message VPN. This value may be higher than supported by the platform.")
  public Long getServiceSmfMaxConnectionCount() {
    return serviceSmfMaxConnectionCount;
  }

  public void setServiceSmfMaxConnectionCount(Long serviceSmfMaxConnectionCount) {
    this.serviceSmfMaxConnectionCount = serviceSmfMaxConnectionCount;
  }

  public MsgVpn serviceSmfPlainTextEnabled(Boolean serviceSmfPlainTextEnabled) {
    this.serviceSmfPlainTextEnabled = serviceSmfPlainTextEnabled;
    return this;
  }

   /**
   * Indicates whether the SMF Service is enabled in the Message VPN.
   * @return serviceSmfPlainTextEnabled
  **/
  @Schema(description = "Indicates whether the SMF Service is enabled in the Message VPN.")
  public Boolean isServiceSmfPlainTextEnabled() {
    return serviceSmfPlainTextEnabled;
  }

  public void setServiceSmfPlainTextEnabled(Boolean serviceSmfPlainTextEnabled) {
    this.serviceSmfPlainTextEnabled = serviceSmfPlainTextEnabled;
  }

  public MsgVpn serviceSmfPlainTextFailureReason(String serviceSmfPlainTextFailureReason) {
    this.serviceSmfPlainTextFailureReason = serviceSmfPlainTextFailureReason;
    return this;
  }

   /**
   * The reason for the SMF Service failure in the Message VPN.
   * @return serviceSmfPlainTextFailureReason
  **/
  @Schema(description = "The reason for the SMF Service failure in the Message VPN.")
  public String getServiceSmfPlainTextFailureReason() {
    return serviceSmfPlainTextFailureReason;
  }

  public void setServiceSmfPlainTextFailureReason(String serviceSmfPlainTextFailureReason) {
    this.serviceSmfPlainTextFailureReason = serviceSmfPlainTextFailureReason;
  }

  public MsgVpn serviceSmfPlainTextUp(Boolean serviceSmfPlainTextUp) {
    this.serviceSmfPlainTextUp = serviceSmfPlainTextUp;
    return this;
  }

   /**
   * Indicates whether the SMF Service is operationally up in the Message VPN.
   * @return serviceSmfPlainTextUp
  **/
  @Schema(description = "Indicates whether the SMF Service is operationally up in the Message VPN.")
  public Boolean isServiceSmfPlainTextUp() {
    return serviceSmfPlainTextUp;
  }

  public void setServiceSmfPlainTextUp(Boolean serviceSmfPlainTextUp) {
    this.serviceSmfPlainTextUp = serviceSmfPlainTextUp;
  }

  public MsgVpn serviceSmfTlsEnabled(Boolean serviceSmfTlsEnabled) {
    this.serviceSmfTlsEnabled = serviceSmfTlsEnabled;
    return this;
  }

   /**
   * Indicates whether encryption (TLS) is enabled for SMF clients in the Message VPN.
   * @return serviceSmfTlsEnabled
  **/
  @Schema(description = "Indicates whether encryption (TLS) is enabled for SMF clients in the Message VPN.")
  public Boolean isServiceSmfTlsEnabled() {
    return serviceSmfTlsEnabled;
  }

  public void setServiceSmfTlsEnabled(Boolean serviceSmfTlsEnabled) {
    this.serviceSmfTlsEnabled = serviceSmfTlsEnabled;
  }

  public MsgVpn serviceSmfTlsFailureReason(String serviceSmfTlsFailureReason) {
    this.serviceSmfTlsFailureReason = serviceSmfTlsFailureReason;
    return this;
  }

   /**
   * The reason for the TLS related SMF Service failure in the Message VPN.
   * @return serviceSmfTlsFailureReason
  **/
  @Schema(description = "The reason for the TLS related SMF Service failure in the Message VPN.")
  public String getServiceSmfTlsFailureReason() {
    return serviceSmfTlsFailureReason;
  }

  public void setServiceSmfTlsFailureReason(String serviceSmfTlsFailureReason) {
    this.serviceSmfTlsFailureReason = serviceSmfTlsFailureReason;
  }

  public MsgVpn serviceSmfTlsUp(Boolean serviceSmfTlsUp) {
    this.serviceSmfTlsUp = serviceSmfTlsUp;
    return this;
  }

   /**
   * Indicates whether the TLS related SMF Service is operationally up in the Message VPN.
   * @return serviceSmfTlsUp
  **/
  @Schema(description = "Indicates whether the TLS related SMF Service is operationally up in the Message VPN.")
  public Boolean isServiceSmfTlsUp() {
    return serviceSmfTlsUp;
  }

  public void setServiceSmfTlsUp(Boolean serviceSmfTlsUp) {
    this.serviceSmfTlsUp = serviceSmfTlsUp;
  }

  public MsgVpn serviceWebMaxConnectionCount(Long serviceWebMaxConnectionCount) {
    this.serviceWebMaxConnectionCount = serviceWebMaxConnectionCount;
    return this;
  }

   /**
   * The maximum number of Web Transport client connections that can be simultaneously connected to the Message VPN. This value may be higher than supported by the platform.
   * @return serviceWebMaxConnectionCount
  **/
  @Schema(description = "The maximum number of Web Transport client connections that can be simultaneously connected to the Message VPN. This value may be higher than supported by the platform.")
  public Long getServiceWebMaxConnectionCount() {
    return serviceWebMaxConnectionCount;
  }

  public void setServiceWebMaxConnectionCount(Long serviceWebMaxConnectionCount) {
    this.serviceWebMaxConnectionCount = serviceWebMaxConnectionCount;
  }

  public MsgVpn serviceWebPlainTextEnabled(Boolean serviceWebPlainTextEnabled) {
    this.serviceWebPlainTextEnabled = serviceWebPlainTextEnabled;
    return this;
  }

   /**
   * Indicates whether the Web transport for the SMF Service is enabled in the Message VPN.
   * @return serviceWebPlainTextEnabled
  **/
  @Schema(description = "Indicates whether the Web transport for the SMF Service is enabled in the Message VPN.")
  public Boolean isServiceWebPlainTextEnabled() {
    return serviceWebPlainTextEnabled;
  }

  public void setServiceWebPlainTextEnabled(Boolean serviceWebPlainTextEnabled) {
    this.serviceWebPlainTextEnabled = serviceWebPlainTextEnabled;
  }

  public MsgVpn serviceWebPlainTextFailureReason(String serviceWebPlainTextFailureReason) {
    this.serviceWebPlainTextFailureReason = serviceWebPlainTextFailureReason;
    return this;
  }

   /**
   * The reason for the Web transport related SMF Service failure in the Message VPN.
   * @return serviceWebPlainTextFailureReason
  **/
  @Schema(description = "The reason for the Web transport related SMF Service failure in the Message VPN.")
  public String getServiceWebPlainTextFailureReason() {
    return serviceWebPlainTextFailureReason;
  }

  public void setServiceWebPlainTextFailureReason(String serviceWebPlainTextFailureReason) {
    this.serviceWebPlainTextFailureReason = serviceWebPlainTextFailureReason;
  }

  public MsgVpn serviceWebPlainTextUp(Boolean serviceWebPlainTextUp) {
    this.serviceWebPlainTextUp = serviceWebPlainTextUp;
    return this;
  }

   /**
   * Indicates whether the Web transport for the SMF Service is operationally up in the Message VPN.
   * @return serviceWebPlainTextUp
  **/
  @Schema(description = "Indicates whether the Web transport for the SMF Service is operationally up in the Message VPN.")
  public Boolean isServiceWebPlainTextUp() {
    return serviceWebPlainTextUp;
  }

  public void setServiceWebPlainTextUp(Boolean serviceWebPlainTextUp) {
    this.serviceWebPlainTextUp = serviceWebPlainTextUp;
  }

  public MsgVpn serviceWebTlsEnabled(Boolean serviceWebTlsEnabled) {
    this.serviceWebTlsEnabled = serviceWebTlsEnabled;
    return this;
  }

   /**
   * Indicates whether TLS is enabled for SMF clients in the Message VPN that use the Web transport.
   * @return serviceWebTlsEnabled
  **/
  @Schema(description = "Indicates whether TLS is enabled for SMF clients in the Message VPN that use the Web transport.")
  public Boolean isServiceWebTlsEnabled() {
    return serviceWebTlsEnabled;
  }

  public void setServiceWebTlsEnabled(Boolean serviceWebTlsEnabled) {
    this.serviceWebTlsEnabled = serviceWebTlsEnabled;
  }

  public MsgVpn serviceWebTlsFailureReason(String serviceWebTlsFailureReason) {
    this.serviceWebTlsFailureReason = serviceWebTlsFailureReason;
    return this;
  }

   /**
   * The reason for the TLS related Web transport SMF Service failure in the Message VPN.
   * @return serviceWebTlsFailureReason
  **/
  @Schema(description = "The reason for the TLS related Web transport SMF Service failure in the Message VPN.")
  public String getServiceWebTlsFailureReason() {
    return serviceWebTlsFailureReason;
  }

  public void setServiceWebTlsFailureReason(String serviceWebTlsFailureReason) {
    this.serviceWebTlsFailureReason = serviceWebTlsFailureReason;
  }

  public MsgVpn serviceWebTlsUp(Boolean serviceWebTlsUp) {
    this.serviceWebTlsUp = serviceWebTlsUp;
    return this;
  }

   /**
   * Indicates whether the TLS related Web transport SMF Service is operationally up in the Message VPN.
   * @return serviceWebTlsUp
  **/
  @Schema(description = "Indicates whether the TLS related Web transport SMF Service is operationally up in the Message VPN.")
  public Boolean isServiceWebTlsUp() {
    return serviceWebTlsUp;
  }

  public void setServiceWebTlsUp(Boolean serviceWebTlsUp) {
    this.serviceWebTlsUp = serviceWebTlsUp;
  }

  public MsgVpn state(String state) {
    this.state = state;
    return this;
  }

   /**
   * The operational state of the local Message VPN. The allowed values and their meaning are:  &lt;pre&gt; \&quot;up\&quot; - The Message VPN is operationally up. \&quot;down\&quot; - The Message VPN is operationally down. \&quot;standby\&quot; - The Message VPN is operationally replication standby. &lt;/pre&gt; 
   * @return state
  **/
  @Schema(description = "The operational state of the local Message VPN. The allowed values and their meaning are:  <pre> \"up\" - The Message VPN is operationally up. \"down\" - The Message VPN is operationally down. \"standby\" - The Message VPN is operationally replication standby. </pre> ")
  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public MsgVpn subscriptionExportProgress(Long subscriptionExportProgress) {
    this.subscriptionExportProgress = subscriptionExportProgress;
    return this;
  }

   /**
   * The progress of the subscription export task, in percent complete.
   * @return subscriptionExportProgress
  **/
  @Schema(description = "The progress of the subscription export task, in percent complete.")
  public Long getSubscriptionExportProgress() {
    return subscriptionExportProgress;
  }

  public void setSubscriptionExportProgress(Long subscriptionExportProgress) {
    this.subscriptionExportProgress = subscriptionExportProgress;
  }

  public MsgVpn systemManager(Boolean systemManager) {
    this.systemManager = systemManager;
    return this;
  }

   /**
   * Indicates whether the Message VPN is the system manager for handling system level SEMP get requests and system level event publishing.
   * @return systemManager
  **/
  @Schema(description = "Indicates whether the Message VPN is the system manager for handling system level SEMP get requests and system level event publishing.")
  public Boolean isSystemManager() {
    return systemManager;
  }

  public void setSystemManager(Boolean systemManager) {
    this.systemManager = systemManager;
  }

  public MsgVpn tlsAllowDowngradeToPlainTextEnabled(Boolean tlsAllowDowngradeToPlainTextEnabled) {
    this.tlsAllowDowngradeToPlainTextEnabled = tlsAllowDowngradeToPlainTextEnabled;
    return this;
  }

   /**
   * Indicates whether SMF clients connected to the Message VPN are allowed to downgrade their connections from TLS to plain text.
   * @return tlsAllowDowngradeToPlainTextEnabled
  **/
  @Schema(description = "Indicates whether SMF clients connected to the Message VPN are allowed to downgrade their connections from TLS to plain text.")
  public Boolean isTlsAllowDowngradeToPlainTextEnabled() {
    return tlsAllowDowngradeToPlainTextEnabled;
  }

  public void setTlsAllowDowngradeToPlainTextEnabled(Boolean tlsAllowDowngradeToPlainTextEnabled) {
    this.tlsAllowDowngradeToPlainTextEnabled = tlsAllowDowngradeToPlainTextEnabled;
  }

  public MsgVpn tlsAverageRxByteRate(Long tlsAverageRxByteRate) {
    this.tlsAverageRxByteRate = tlsAverageRxByteRate;
    return this;
  }

   /**
   * The one minute average of the TLS message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.13.
   * @return tlsAverageRxByteRate
  **/
  @Schema(description = "The one minute average of the TLS message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.13.")
  public Long getTlsAverageRxByteRate() {
    return tlsAverageRxByteRate;
  }

  public void setTlsAverageRxByteRate(Long tlsAverageRxByteRate) {
    this.tlsAverageRxByteRate = tlsAverageRxByteRate;
  }

  public MsgVpn tlsAverageTxByteRate(Long tlsAverageTxByteRate) {
    this.tlsAverageTxByteRate = tlsAverageTxByteRate;
    return this;
  }

   /**
   * The one minute average of the TLS message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.13.
   * @return tlsAverageTxByteRate
  **/
  @Schema(description = "The one minute average of the TLS message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.13.")
  public Long getTlsAverageTxByteRate() {
    return tlsAverageTxByteRate;
  }

  public void setTlsAverageTxByteRate(Long tlsAverageTxByteRate) {
    this.tlsAverageTxByteRate = tlsAverageTxByteRate;
  }

  public MsgVpn tlsRxByteCount(Long tlsRxByteCount) {
    this.tlsRxByteCount = tlsRxByteCount;
    return this;
  }

   /**
   * The amount of TLS messages received by the Message VPN, in bytes (B). Available since 2.13.
   * @return tlsRxByteCount
  **/
  @Schema(description = "The amount of TLS messages received by the Message VPN, in bytes (B). Available since 2.13.")
  public Long getTlsRxByteCount() {
    return tlsRxByteCount;
  }

  public void setTlsRxByteCount(Long tlsRxByteCount) {
    this.tlsRxByteCount = tlsRxByteCount;
  }

  public MsgVpn tlsRxByteRate(Long tlsRxByteRate) {
    this.tlsRxByteRate = tlsRxByteRate;
    return this;
  }

   /**
   * The current TLS message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.13.
   * @return tlsRxByteRate
  **/
  @Schema(description = "The current TLS message rate received by the Message VPN, in bytes per second (B/sec). Available since 2.13.")
  public Long getTlsRxByteRate() {
    return tlsRxByteRate;
  }

  public void setTlsRxByteRate(Long tlsRxByteRate) {
    this.tlsRxByteRate = tlsRxByteRate;
  }

  public MsgVpn tlsTxByteCount(Long tlsTxByteCount) {
    this.tlsTxByteCount = tlsTxByteCount;
    return this;
  }

   /**
   * The amount of TLS messages transmitted by the Message VPN, in bytes (B). Available since 2.13.
   * @return tlsTxByteCount
  **/
  @Schema(description = "The amount of TLS messages transmitted by the Message VPN, in bytes (B). Available since 2.13.")
  public Long getTlsTxByteCount() {
    return tlsTxByteCount;
  }

  public void setTlsTxByteCount(Long tlsTxByteCount) {
    this.tlsTxByteCount = tlsTxByteCount;
  }

  public MsgVpn tlsTxByteRate(Long tlsTxByteRate) {
    this.tlsTxByteRate = tlsTxByteRate;
    return this;
  }

   /**
   * The current TLS message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.13.
   * @return tlsTxByteRate
  **/
  @Schema(description = "The current TLS message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.13.")
  public Long getTlsTxByteRate() {
    return tlsTxByteRate;
  }

  public void setTlsTxByteRate(Long tlsTxByteRate) {
    this.tlsTxByteRate = tlsTxByteRate;
  }

  public MsgVpn txByteCount(Long txByteCount) {
    this.txByteCount = txByteCount;
    return this;
  }

   /**
   * The amount of messages transmitted to clients by the Message VPN, in bytes (B). Available since 2.12.
   * @return txByteCount
  **/
  @Schema(description = "The amount of messages transmitted to clients by the Message VPN, in bytes (B). Available since 2.12.")
  public Long getTxByteCount() {
    return txByteCount;
  }

  public void setTxByteCount(Long txByteCount) {
    this.txByteCount = txByteCount;
  }

  public MsgVpn txByteRate(Long txByteRate) {
    this.txByteRate = txByteRate;
    return this;
  }

   /**
   * The current message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.13.
   * @return txByteRate
  **/
  @Schema(description = "The current message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.13.")
  public Long getTxByteRate() {
    return txByteRate;
  }

  public void setTxByteRate(Long txByteRate) {
    this.txByteRate = txByteRate;
  }

  public MsgVpn txCompressedByteCount(Long txCompressedByteCount) {
    this.txCompressedByteCount = txCompressedByteCount;
    return this;
  }

   /**
   * The amount of compressed messages transmitted by the Message VPN, in bytes (B). Available since 2.12.
   * @return txCompressedByteCount
  **/
  @Schema(description = "The amount of compressed messages transmitted by the Message VPN, in bytes (B). Available since 2.12.")
  public Long getTxCompressedByteCount() {
    return txCompressedByteCount;
  }

  public void setTxCompressedByteCount(Long txCompressedByteCount) {
    this.txCompressedByteCount = txCompressedByteCount;
  }

  public MsgVpn txCompressedByteRate(Long txCompressedByteRate) {
    this.txCompressedByteRate = txCompressedByteRate;
    return this;
  }

   /**
   * The current compressed message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.12.
   * @return txCompressedByteRate
  **/
  @Schema(description = "The current compressed message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.12.")
  public Long getTxCompressedByteRate() {
    return txCompressedByteRate;
  }

  public void setTxCompressedByteRate(Long txCompressedByteRate) {
    this.txCompressedByteRate = txCompressedByteRate;
  }

  public MsgVpn txCompressionRatio(String txCompressionRatio) {
    this.txCompressionRatio = txCompressionRatio;
    return this;
  }

   /**
   * The compression ratio for messages transmitted by the message VPN. Available since 2.12.
   * @return txCompressionRatio
  **/
  @Schema(description = "The compression ratio for messages transmitted by the message VPN. Available since 2.12.")
  public String getTxCompressionRatio() {
    return txCompressionRatio;
  }

  public void setTxCompressionRatio(String txCompressionRatio) {
    this.txCompressionRatio = txCompressionRatio;
  }

  public MsgVpn txMsgCount(Long txMsgCount) {
    this.txMsgCount = txMsgCount;
    return this;
  }

   /**
   * The number of messages transmitted to clients by the Message VPN. Available since 2.12.
   * @return txMsgCount
  **/
  @Schema(description = "The number of messages transmitted to clients by the Message VPN. Available since 2.12.")
  public Long getTxMsgCount() {
    return txMsgCount;
  }

  public void setTxMsgCount(Long txMsgCount) {
    this.txMsgCount = txMsgCount;
  }

  public MsgVpn txMsgRate(Long txMsgRate) {
    this.txMsgRate = txMsgRate;
    return this;
  }

   /**
   * The current message rate transmitted by the Message VPN, in messages per second (msg/sec). Available since 2.13.
   * @return txMsgRate
  **/
  @Schema(description = "The current message rate transmitted by the Message VPN, in messages per second (msg/sec). Available since 2.13.")
  public Long getTxMsgRate() {
    return txMsgRate;
  }

  public void setTxMsgRate(Long txMsgRate) {
    this.txMsgRate = txMsgRate;
  }

  public MsgVpn txUncompressedByteCount(Long txUncompressedByteCount) {
    this.txUncompressedByteCount = txUncompressedByteCount;
    return this;
  }

   /**
   * The amount of uncompressed messages transmitted by the Message VPN, in bytes (B). Available since 2.12.
   * @return txUncompressedByteCount
  **/
  @Schema(description = "The amount of uncompressed messages transmitted by the Message VPN, in bytes (B). Available since 2.12.")
  public Long getTxUncompressedByteCount() {
    return txUncompressedByteCount;
  }

  public void setTxUncompressedByteCount(Long txUncompressedByteCount) {
    this.txUncompressedByteCount = txUncompressedByteCount;
  }

  public MsgVpn txUncompressedByteRate(Long txUncompressedByteRate) {
    this.txUncompressedByteRate = txUncompressedByteRate;
    return this;
  }

   /**
   * The current uncompressed message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.12.
   * @return txUncompressedByteRate
  **/
  @Schema(description = "The current uncompressed message rate transmitted by the Message VPN, in bytes per second (B/sec). Available since 2.12.")
  public Long getTxUncompressedByteRate() {
    return txUncompressedByteRate;
  }

  public void setTxUncompressedByteRate(Long txUncompressedByteRate) {
    this.txUncompressedByteRate = txUncompressedByteRate;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpn msgVpn = (MsgVpn) o;
    return Objects.equals(this.alias, msgVpn.alias) &&
        Objects.equals(this.authenticationBasicEnabled, msgVpn.authenticationBasicEnabled) &&
        Objects.equals(this.authenticationBasicProfileName, msgVpn.authenticationBasicProfileName) &&
        Objects.equals(this.authenticationBasicRadiusDomain, msgVpn.authenticationBasicRadiusDomain) &&
        Objects.equals(this.authenticationBasicType, msgVpn.authenticationBasicType) &&
        Objects.equals(this.authenticationClientCertAllowApiProvidedUsernameEnabled, msgVpn.authenticationClientCertAllowApiProvidedUsernameEnabled) &&
        Objects.equals(this.authenticationClientCertEnabled, msgVpn.authenticationClientCertEnabled) &&
        Objects.equals(this.authenticationClientCertMaxChainDepth, msgVpn.authenticationClientCertMaxChainDepth) &&
        Objects.equals(this.authenticationClientCertRevocationCheckMode, msgVpn.authenticationClientCertRevocationCheckMode) &&
        Objects.equals(this.authenticationClientCertUsernameSource, msgVpn.authenticationClientCertUsernameSource) &&
        Objects.equals(this.authenticationClientCertValidateDateEnabled, msgVpn.authenticationClientCertValidateDateEnabled) &&
        Objects.equals(this.authenticationKerberosAllowApiProvidedUsernameEnabled, msgVpn.authenticationKerberosAllowApiProvidedUsernameEnabled) &&
        Objects.equals(this.authenticationKerberosEnabled, msgVpn.authenticationKerberosEnabled) &&
        Objects.equals(this.authenticationOauthDefaultProviderName, msgVpn.authenticationOauthDefaultProviderName) &&
        Objects.equals(this.authenticationOauthEnabled, msgVpn.authenticationOauthEnabled) &&
        Objects.equals(this.authorizationLdapGroupMembershipAttributeName, msgVpn.authorizationLdapGroupMembershipAttributeName) &&
        Objects.equals(this.authorizationLdapTrimClientUsernameDomainEnabled, msgVpn.authorizationLdapTrimClientUsernameDomainEnabled) &&
        Objects.equals(this.authorizationProfileName, msgVpn.authorizationProfileName) &&
        Objects.equals(this.authorizationType, msgVpn.authorizationType) &&
        Objects.equals(this.averageRxByteRate, msgVpn.averageRxByteRate) &&
        Objects.equals(this.averageRxCompressedByteRate, msgVpn.averageRxCompressedByteRate) &&
        Objects.equals(this.averageRxMsgRate, msgVpn.averageRxMsgRate) &&
        Objects.equals(this.averageRxUncompressedByteRate, msgVpn.averageRxUncompressedByteRate) &&
        Objects.equals(this.averageTxByteRate, msgVpn.averageTxByteRate) &&
        Objects.equals(this.averageTxCompressedByteRate, msgVpn.averageTxCompressedByteRate) &&
        Objects.equals(this.averageTxMsgRate, msgVpn.averageTxMsgRate) &&
        Objects.equals(this.averageTxUncompressedByteRate, msgVpn.averageTxUncompressedByteRate) &&
        Objects.equals(this.bridgingTlsServerCertEnforceTrustedCommonNameEnabled, msgVpn.bridgingTlsServerCertEnforceTrustedCommonNameEnabled) &&
        Objects.equals(this.bridgingTlsServerCertMaxChainDepth, msgVpn.bridgingTlsServerCertMaxChainDepth) &&
        Objects.equals(this.bridgingTlsServerCertValidateDateEnabled, msgVpn.bridgingTlsServerCertValidateDateEnabled) &&
        Objects.equals(this.configSyncLocalKey, msgVpn.configSyncLocalKey) &&
        Objects.equals(this.configSyncLocalLastResult, msgVpn.configSyncLocalLastResult) &&
        Objects.equals(this.configSyncLocalRole, msgVpn.configSyncLocalRole) &&
        Objects.equals(this.configSyncLocalState, msgVpn.configSyncLocalState) &&
        Objects.equals(this.configSyncLocalTimeInState, msgVpn.configSyncLocalTimeInState) &&
        Objects.equals(this.controlRxByteCount, msgVpn.controlRxByteCount) &&
        Objects.equals(this.controlRxMsgCount, msgVpn.controlRxMsgCount) &&
        Objects.equals(this.controlTxByteCount, msgVpn.controlTxByteCount) &&
        Objects.equals(this.controlTxMsgCount, msgVpn.controlTxMsgCount) &&
        Objects.equals(this.counter, msgVpn.counter) &&
        Objects.equals(this.dataRxByteCount, msgVpn.dataRxByteCount) &&
        Objects.equals(this.dataRxMsgCount, msgVpn.dataRxMsgCount) &&
        Objects.equals(this.dataTxByteCount, msgVpn.dataTxByteCount) &&
        Objects.equals(this.dataTxMsgCount, msgVpn.dataTxMsgCount) &&
        Objects.equals(this.discardedRxMsgCount, msgVpn.discardedRxMsgCount) &&
        Objects.equals(this.discardedTxMsgCount, msgVpn.discardedTxMsgCount) &&
        Objects.equals(this.distributedCacheManagementEnabled, msgVpn.distributedCacheManagementEnabled) &&
        Objects.equals(this.dmrEnabled, msgVpn.dmrEnabled) &&
        Objects.equals(this.enabled, msgVpn.enabled) &&
        Objects.equals(this.eventConnectionCountThreshold, msgVpn.eventConnectionCountThreshold) &&
        Objects.equals(this.eventEgressFlowCountThreshold, msgVpn.eventEgressFlowCountThreshold) &&
        Objects.equals(this.eventEgressMsgRateThreshold, msgVpn.eventEgressMsgRateThreshold) &&
        Objects.equals(this.eventEndpointCountThreshold, msgVpn.eventEndpointCountThreshold) &&
        Objects.equals(this.eventIngressFlowCountThreshold, msgVpn.eventIngressFlowCountThreshold) &&
        Objects.equals(this.eventIngressMsgRateThreshold, msgVpn.eventIngressMsgRateThreshold) &&
        Objects.equals(this.eventLargeMsgThreshold, msgVpn.eventLargeMsgThreshold) &&
        Objects.equals(this.eventLogTag, msgVpn.eventLogTag) &&
        Objects.equals(this.eventMsgSpoolUsageThreshold, msgVpn.eventMsgSpoolUsageThreshold) &&
        Objects.equals(this.eventPublishClientEnabled, msgVpn.eventPublishClientEnabled) &&
        Objects.equals(this.eventPublishMsgVpnEnabled, msgVpn.eventPublishMsgVpnEnabled) &&
        Objects.equals(this.eventPublishSubscriptionMode, msgVpn.eventPublishSubscriptionMode) &&
        Objects.equals(this.eventPublishTopicFormatMqttEnabled, msgVpn.eventPublishTopicFormatMqttEnabled) &&
        Objects.equals(this.eventPublishTopicFormatSmfEnabled, msgVpn.eventPublishTopicFormatSmfEnabled) &&
        Objects.equals(this.eventServiceAmqpConnectionCountThreshold, msgVpn.eventServiceAmqpConnectionCountThreshold) &&
        Objects.equals(this.eventServiceMqttConnectionCountThreshold, msgVpn.eventServiceMqttConnectionCountThreshold) &&
        Objects.equals(this.eventServiceRestIncomingConnectionCountThreshold, msgVpn.eventServiceRestIncomingConnectionCountThreshold) &&
        Objects.equals(this.eventServiceSmfConnectionCountThreshold, msgVpn.eventServiceSmfConnectionCountThreshold) &&
        Objects.equals(this.eventServiceWebConnectionCountThreshold, msgVpn.eventServiceWebConnectionCountThreshold) &&
        Objects.equals(this.eventSubscriptionCountThreshold, msgVpn.eventSubscriptionCountThreshold) &&
        Objects.equals(this.eventTransactedSessionCountThreshold, msgVpn.eventTransactedSessionCountThreshold) &&
        Objects.equals(this.eventTransactionCountThreshold, msgVpn.eventTransactionCountThreshold) &&
        Objects.equals(this.exportSubscriptionsEnabled, msgVpn.exportSubscriptionsEnabled) &&
        Objects.equals(this.failureReason, msgVpn.failureReason) &&
        Objects.equals(this.jndiEnabled, msgVpn.jndiEnabled) &&
        Objects.equals(this.loginRxMsgCount, msgVpn.loginRxMsgCount) &&
        Objects.equals(this.loginTxMsgCount, msgVpn.loginTxMsgCount) &&
        Objects.equals(this.maxConnectionCount, msgVpn.maxConnectionCount) &&
        Objects.equals(this.maxEffectiveEndpointCount, msgVpn.maxEffectiveEndpointCount) &&
        Objects.equals(this.maxEffectiveRxFlowCount, msgVpn.maxEffectiveRxFlowCount) &&
        Objects.equals(this.maxEffectiveSubscriptionCount, msgVpn.maxEffectiveSubscriptionCount) &&
        Objects.equals(this.maxEffectiveTransactedSessionCount, msgVpn.maxEffectiveTransactedSessionCount) &&
        Objects.equals(this.maxEffectiveTransactionCount, msgVpn.maxEffectiveTransactionCount) &&
        Objects.equals(this.maxEffectiveTxFlowCount, msgVpn.maxEffectiveTxFlowCount) &&
        Objects.equals(this.maxEgressFlowCount, msgVpn.maxEgressFlowCount) &&
        Objects.equals(this.maxEndpointCount, msgVpn.maxEndpointCount) &&
        Objects.equals(this.maxIngressFlowCount, msgVpn.maxIngressFlowCount) &&
        Objects.equals(this.maxMsgSpoolUsage, msgVpn.maxMsgSpoolUsage) &&
        Objects.equals(this.maxSubscriptionCount, msgVpn.maxSubscriptionCount) &&
        Objects.equals(this.maxTransactedSessionCount, msgVpn.maxTransactedSessionCount) &&
        Objects.equals(this.maxTransactionCount, msgVpn.maxTransactionCount) &&
        Objects.equals(this.mqttRetainMaxMemory, msgVpn.mqttRetainMaxMemory) &&
        Objects.equals(this.msgReplayActiveCount, msgVpn.msgReplayActiveCount) &&
        Objects.equals(this.msgReplayFailedCount, msgVpn.msgReplayFailedCount) &&
        Objects.equals(this.msgReplayInitializingCount, msgVpn.msgReplayInitializingCount) &&
        Objects.equals(this.msgReplayPendingCompleteCount, msgVpn.msgReplayPendingCompleteCount) &&
        Objects.equals(this.msgSpoolMsgCount, msgVpn.msgSpoolMsgCount) &&
        Objects.equals(this.msgSpoolRxMsgCount, msgVpn.msgSpoolRxMsgCount) &&
        Objects.equals(this.msgSpoolTxMsgCount, msgVpn.msgSpoolTxMsgCount) &&
        Objects.equals(this.msgSpoolUsage, msgVpn.msgSpoolUsage) &&
        Objects.equals(this.msgVpnName, msgVpn.msgVpnName) &&
        Objects.equals(this.rate, msgVpn.rate) &&
        Objects.equals(this.replicationAckPropagationIntervalMsgCount, msgVpn.replicationAckPropagationIntervalMsgCount) &&
        Objects.equals(this.replicationActiveAckPropTxMsgCount, msgVpn.replicationActiveAckPropTxMsgCount) &&
        Objects.equals(this.replicationActiveAsyncQueuedMsgCount, msgVpn.replicationActiveAsyncQueuedMsgCount) &&
        Objects.equals(this.replicationActiveLocallyConsumedMsgCount, msgVpn.replicationActiveLocallyConsumedMsgCount) &&
        Objects.equals(this.replicationActiveMateFlowCongestedPeakTime, msgVpn.replicationActiveMateFlowCongestedPeakTime) &&
        Objects.equals(this.replicationActiveMateFlowNotCongestedPeakTime, msgVpn.replicationActiveMateFlowNotCongestedPeakTime) &&
        Objects.equals(this.replicationActivePromotedQueuedMsgCount, msgVpn.replicationActivePromotedQueuedMsgCount) &&
        Objects.equals(this.replicationActiveReconcileRequestRxMsgCount, msgVpn.replicationActiveReconcileRequestRxMsgCount) &&
        Objects.equals(this.replicationActiveSyncEligiblePeakTime, msgVpn.replicationActiveSyncEligiblePeakTime) &&
        Objects.equals(this.replicationActiveSyncIneligiblePeakTime, msgVpn.replicationActiveSyncIneligiblePeakTime) &&
        Objects.equals(this.replicationActiveSyncQueuedAsAsyncMsgCount, msgVpn.replicationActiveSyncQueuedAsAsyncMsgCount) &&
        Objects.equals(this.replicationActiveSyncQueuedMsgCount, msgVpn.replicationActiveSyncQueuedMsgCount) &&
        Objects.equals(this.replicationActiveTransitionToSyncIneligibleCount, msgVpn.replicationActiveTransitionToSyncIneligibleCount) &&
        Objects.equals(this.replicationBridgeAuthenticationBasicClientUsername, msgVpn.replicationBridgeAuthenticationBasicClientUsername) &&
        Objects.equals(this.replicationBridgeAuthenticationScheme, msgVpn.replicationBridgeAuthenticationScheme) &&
        Objects.equals(this.replicationBridgeBoundToQueue, msgVpn.replicationBridgeBoundToQueue) &&
        Objects.equals(this.replicationBridgeCompressedDataEnabled, msgVpn.replicationBridgeCompressedDataEnabled) &&
        Objects.equals(this.replicationBridgeEgressFlowWindowSize, msgVpn.replicationBridgeEgressFlowWindowSize) &&
        Objects.equals(this.replicationBridgeName, msgVpn.replicationBridgeName) &&
        Objects.equals(this.replicationBridgeRetryDelay, msgVpn.replicationBridgeRetryDelay) &&
        Objects.equals(this.replicationBridgeTlsEnabled, msgVpn.replicationBridgeTlsEnabled) &&
        Objects.equals(this.replicationBridgeUnidirectionalClientProfileName, msgVpn.replicationBridgeUnidirectionalClientProfileName) &&
        Objects.equals(this.replicationBridgeUp, msgVpn.replicationBridgeUp) &&
        Objects.equals(this.replicationEnabled, msgVpn.replicationEnabled) &&
        Objects.equals(this.replicationQueueBound, msgVpn.replicationQueueBound) &&
        Objects.equals(this.replicationQueueMaxMsgSpoolUsage, msgVpn.replicationQueueMaxMsgSpoolUsage) &&
        Objects.equals(this.replicationQueueRejectMsgToSenderOnDiscardEnabled, msgVpn.replicationQueueRejectMsgToSenderOnDiscardEnabled) &&
        Objects.equals(this.replicationRejectMsgWhenSyncIneligibleEnabled, msgVpn.replicationRejectMsgWhenSyncIneligibleEnabled) &&
        Objects.equals(this.replicationRemoteBridgeName, msgVpn.replicationRemoteBridgeName) &&
        Objects.equals(this.replicationRemoteBridgeUp, msgVpn.replicationRemoteBridgeUp) &&
        Objects.equals(this.replicationRole, msgVpn.replicationRole) &&
        Objects.equals(this.replicationStandbyAckPropOutOfSeqRxMsgCount, msgVpn.replicationStandbyAckPropOutOfSeqRxMsgCount) &&
        Objects.equals(this.replicationStandbyAckPropRxMsgCount, msgVpn.replicationStandbyAckPropRxMsgCount) &&
        Objects.equals(this.replicationStandbyReconcileRequestTxMsgCount, msgVpn.replicationStandbyReconcileRequestTxMsgCount) &&
        Objects.equals(this.replicationStandbyRxMsgCount, msgVpn.replicationStandbyRxMsgCount) &&
        Objects.equals(this.replicationStandbyTransactionRequestCount, msgVpn.replicationStandbyTransactionRequestCount) &&
        Objects.equals(this.replicationStandbyTransactionRequestFailureCount, msgVpn.replicationStandbyTransactionRequestFailureCount) &&
        Objects.equals(this.replicationStandbyTransactionRequestSuccessCount, msgVpn.replicationStandbyTransactionRequestSuccessCount) &&
        Objects.equals(this.replicationSyncEligible, msgVpn.replicationSyncEligible) &&
        Objects.equals(this.replicationTransactionMode, msgVpn.replicationTransactionMode) &&
        Objects.equals(this.restTlsServerCertEnforceTrustedCommonNameEnabled, msgVpn.restTlsServerCertEnforceTrustedCommonNameEnabled) &&
        Objects.equals(this.restTlsServerCertMaxChainDepth, msgVpn.restTlsServerCertMaxChainDepth) &&
        Objects.equals(this.restTlsServerCertValidateDateEnabled, msgVpn.restTlsServerCertValidateDateEnabled) &&
        Objects.equals(this.restTlsServerCertValidateNameEnabled, msgVpn.restTlsServerCertValidateNameEnabled) &&
        Objects.equals(this.rxByteCount, msgVpn.rxByteCount) &&
        Objects.equals(this.rxByteRate, msgVpn.rxByteRate) &&
        Objects.equals(this.rxCompressedByteCount, msgVpn.rxCompressedByteCount) &&
        Objects.equals(this.rxCompressedByteRate, msgVpn.rxCompressedByteRate) &&
        Objects.equals(this.rxCompressionRatio, msgVpn.rxCompressionRatio) &&
        Objects.equals(this.rxMsgCount, msgVpn.rxMsgCount) &&
        Objects.equals(this.rxMsgRate, msgVpn.rxMsgRate) &&
        Objects.equals(this.rxUncompressedByteCount, msgVpn.rxUncompressedByteCount) &&
        Objects.equals(this.rxUncompressedByteRate, msgVpn.rxUncompressedByteRate) &&
        Objects.equals(this.sempOverMsgBusAdminClientEnabled, msgVpn.sempOverMsgBusAdminClientEnabled) &&
        Objects.equals(this.sempOverMsgBusAdminDistributedCacheEnabled, msgVpn.sempOverMsgBusAdminDistributedCacheEnabled) &&
        Objects.equals(this.sempOverMsgBusAdminEnabled, msgVpn.sempOverMsgBusAdminEnabled) &&
        Objects.equals(this.sempOverMsgBusEnabled, msgVpn.sempOverMsgBusEnabled) &&
        Objects.equals(this.sempOverMsgBusShowEnabled, msgVpn.sempOverMsgBusShowEnabled) &&
        Objects.equals(this.serviceAmqpMaxConnectionCount, msgVpn.serviceAmqpMaxConnectionCount) &&
        Objects.equals(this.serviceAmqpPlainTextCompressed, msgVpn.serviceAmqpPlainTextCompressed) &&
        Objects.equals(this.serviceAmqpPlainTextEnabled, msgVpn.serviceAmqpPlainTextEnabled) &&
        Objects.equals(this.serviceAmqpPlainTextFailureReason, msgVpn.serviceAmqpPlainTextFailureReason) &&
        Objects.equals(this.serviceAmqpPlainTextListenPort, msgVpn.serviceAmqpPlainTextListenPort) &&
        Objects.equals(this.serviceAmqpPlainTextUp, msgVpn.serviceAmqpPlainTextUp) &&
        Objects.equals(this.serviceAmqpTlsCompressed, msgVpn.serviceAmqpTlsCompressed) &&
        Objects.equals(this.serviceAmqpTlsEnabled, msgVpn.serviceAmqpTlsEnabled) &&
        Objects.equals(this.serviceAmqpTlsFailureReason, msgVpn.serviceAmqpTlsFailureReason) &&
        Objects.equals(this.serviceAmqpTlsListenPort, msgVpn.serviceAmqpTlsListenPort) &&
        Objects.equals(this.serviceAmqpTlsUp, msgVpn.serviceAmqpTlsUp) &&
        Objects.equals(this.serviceMqttMaxConnectionCount, msgVpn.serviceMqttMaxConnectionCount) &&
        Objects.equals(this.serviceMqttPlainTextCompressed, msgVpn.serviceMqttPlainTextCompressed) &&
        Objects.equals(this.serviceMqttPlainTextEnabled, msgVpn.serviceMqttPlainTextEnabled) &&
        Objects.equals(this.serviceMqttPlainTextFailureReason, msgVpn.serviceMqttPlainTextFailureReason) &&
        Objects.equals(this.serviceMqttPlainTextListenPort, msgVpn.serviceMqttPlainTextListenPort) &&
        Objects.equals(this.serviceMqttPlainTextUp, msgVpn.serviceMqttPlainTextUp) &&
        Objects.equals(this.serviceMqttTlsCompressed, msgVpn.serviceMqttTlsCompressed) &&
        Objects.equals(this.serviceMqttTlsEnabled, msgVpn.serviceMqttTlsEnabled) &&
        Objects.equals(this.serviceMqttTlsFailureReason, msgVpn.serviceMqttTlsFailureReason) &&
        Objects.equals(this.serviceMqttTlsListenPort, msgVpn.serviceMqttTlsListenPort) &&
        Objects.equals(this.serviceMqttTlsUp, msgVpn.serviceMqttTlsUp) &&
        Objects.equals(this.serviceMqttTlsWebSocketCompressed, msgVpn.serviceMqttTlsWebSocketCompressed) &&
        Objects.equals(this.serviceMqttTlsWebSocketEnabled, msgVpn.serviceMqttTlsWebSocketEnabled) &&
        Objects.equals(this.serviceMqttTlsWebSocketFailureReason, msgVpn.serviceMqttTlsWebSocketFailureReason) &&
        Objects.equals(this.serviceMqttTlsWebSocketListenPort, msgVpn.serviceMqttTlsWebSocketListenPort) &&
        Objects.equals(this.serviceMqttTlsWebSocketUp, msgVpn.serviceMqttTlsWebSocketUp) &&
        Objects.equals(this.serviceMqttWebSocketCompressed, msgVpn.serviceMqttWebSocketCompressed) &&
        Objects.equals(this.serviceMqttWebSocketEnabled, msgVpn.serviceMqttWebSocketEnabled) &&
        Objects.equals(this.serviceMqttWebSocketFailureReason, msgVpn.serviceMqttWebSocketFailureReason) &&
        Objects.equals(this.serviceMqttWebSocketListenPort, msgVpn.serviceMqttWebSocketListenPort) &&
        Objects.equals(this.serviceMqttWebSocketUp, msgVpn.serviceMqttWebSocketUp) &&
        Objects.equals(this.serviceRestIncomingMaxConnectionCount, msgVpn.serviceRestIncomingMaxConnectionCount) &&
        Objects.equals(this.serviceRestIncomingPlainTextCompressed, msgVpn.serviceRestIncomingPlainTextCompressed) &&
        Objects.equals(this.serviceRestIncomingPlainTextEnabled, msgVpn.serviceRestIncomingPlainTextEnabled) &&
        Objects.equals(this.serviceRestIncomingPlainTextFailureReason, msgVpn.serviceRestIncomingPlainTextFailureReason) &&
        Objects.equals(this.serviceRestIncomingPlainTextListenPort, msgVpn.serviceRestIncomingPlainTextListenPort) &&
        Objects.equals(this.serviceRestIncomingPlainTextUp, msgVpn.serviceRestIncomingPlainTextUp) &&
        Objects.equals(this.serviceRestIncomingTlsCompressed, msgVpn.serviceRestIncomingTlsCompressed) &&
        Objects.equals(this.serviceRestIncomingTlsEnabled, msgVpn.serviceRestIncomingTlsEnabled) &&
        Objects.equals(this.serviceRestIncomingTlsFailureReason, msgVpn.serviceRestIncomingTlsFailureReason) &&
        Objects.equals(this.serviceRestIncomingTlsListenPort, msgVpn.serviceRestIncomingTlsListenPort) &&
        Objects.equals(this.serviceRestIncomingTlsUp, msgVpn.serviceRestIncomingTlsUp) &&
        Objects.equals(this.serviceRestMode, msgVpn.serviceRestMode) &&
        Objects.equals(this.serviceRestOutgoingMaxConnectionCount, msgVpn.serviceRestOutgoingMaxConnectionCount) &&
        Objects.equals(this.serviceSmfMaxConnectionCount, msgVpn.serviceSmfMaxConnectionCount) &&
        Objects.equals(this.serviceSmfPlainTextEnabled, msgVpn.serviceSmfPlainTextEnabled) &&
        Objects.equals(this.serviceSmfPlainTextFailureReason, msgVpn.serviceSmfPlainTextFailureReason) &&
        Objects.equals(this.serviceSmfPlainTextUp, msgVpn.serviceSmfPlainTextUp) &&
        Objects.equals(this.serviceSmfTlsEnabled, msgVpn.serviceSmfTlsEnabled) &&
        Objects.equals(this.serviceSmfTlsFailureReason, msgVpn.serviceSmfTlsFailureReason) &&
        Objects.equals(this.serviceSmfTlsUp, msgVpn.serviceSmfTlsUp) &&
        Objects.equals(this.serviceWebMaxConnectionCount, msgVpn.serviceWebMaxConnectionCount) &&
        Objects.equals(this.serviceWebPlainTextEnabled, msgVpn.serviceWebPlainTextEnabled) &&
        Objects.equals(this.serviceWebPlainTextFailureReason, msgVpn.serviceWebPlainTextFailureReason) &&
        Objects.equals(this.serviceWebPlainTextUp, msgVpn.serviceWebPlainTextUp) &&
        Objects.equals(this.serviceWebTlsEnabled, msgVpn.serviceWebTlsEnabled) &&
        Objects.equals(this.serviceWebTlsFailureReason, msgVpn.serviceWebTlsFailureReason) &&
        Objects.equals(this.serviceWebTlsUp, msgVpn.serviceWebTlsUp) &&
        Objects.equals(this.state, msgVpn.state) &&
        Objects.equals(this.subscriptionExportProgress, msgVpn.subscriptionExportProgress) &&
        Objects.equals(this.systemManager, msgVpn.systemManager) &&
        Objects.equals(this.tlsAllowDowngradeToPlainTextEnabled, msgVpn.tlsAllowDowngradeToPlainTextEnabled) &&
        Objects.equals(this.tlsAverageRxByteRate, msgVpn.tlsAverageRxByteRate) &&
        Objects.equals(this.tlsAverageTxByteRate, msgVpn.tlsAverageTxByteRate) &&
        Objects.equals(this.tlsRxByteCount, msgVpn.tlsRxByteCount) &&
        Objects.equals(this.tlsRxByteRate, msgVpn.tlsRxByteRate) &&
        Objects.equals(this.tlsTxByteCount, msgVpn.tlsTxByteCount) &&
        Objects.equals(this.tlsTxByteRate, msgVpn.tlsTxByteRate) &&
        Objects.equals(this.txByteCount, msgVpn.txByteCount) &&
        Objects.equals(this.txByteRate, msgVpn.txByteRate) &&
        Objects.equals(this.txCompressedByteCount, msgVpn.txCompressedByteCount) &&
        Objects.equals(this.txCompressedByteRate, msgVpn.txCompressedByteRate) &&
        Objects.equals(this.txCompressionRatio, msgVpn.txCompressionRatio) &&
        Objects.equals(this.txMsgCount, msgVpn.txMsgCount) &&
        Objects.equals(this.txMsgRate, msgVpn.txMsgRate) &&
        Objects.equals(this.txUncompressedByteCount, msgVpn.txUncompressedByteCount) &&
        Objects.equals(this.txUncompressedByteRate, msgVpn.txUncompressedByteRate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(alias, authenticationBasicEnabled, authenticationBasicProfileName, authenticationBasicRadiusDomain, authenticationBasicType, authenticationClientCertAllowApiProvidedUsernameEnabled, authenticationClientCertEnabled, authenticationClientCertMaxChainDepth, authenticationClientCertRevocationCheckMode, authenticationClientCertUsernameSource, authenticationClientCertValidateDateEnabled, authenticationKerberosAllowApiProvidedUsernameEnabled, authenticationKerberosEnabled, authenticationOauthDefaultProviderName, authenticationOauthEnabled, authorizationLdapGroupMembershipAttributeName, authorizationLdapTrimClientUsernameDomainEnabled, authorizationProfileName, authorizationType, averageRxByteRate, averageRxCompressedByteRate, averageRxMsgRate, averageRxUncompressedByteRate, averageTxByteRate, averageTxCompressedByteRate, averageTxMsgRate, averageTxUncompressedByteRate, bridgingTlsServerCertEnforceTrustedCommonNameEnabled, bridgingTlsServerCertMaxChainDepth, bridgingTlsServerCertValidateDateEnabled, configSyncLocalKey, configSyncLocalLastResult, configSyncLocalRole, configSyncLocalState, configSyncLocalTimeInState, controlRxByteCount, controlRxMsgCount, controlTxByteCount, controlTxMsgCount, counter, dataRxByteCount, dataRxMsgCount, dataTxByteCount, dataTxMsgCount, discardedRxMsgCount, discardedTxMsgCount, distributedCacheManagementEnabled, dmrEnabled, enabled, eventConnectionCountThreshold, eventEgressFlowCountThreshold, eventEgressMsgRateThreshold, eventEndpointCountThreshold, eventIngressFlowCountThreshold, eventIngressMsgRateThreshold, eventLargeMsgThreshold, eventLogTag, eventMsgSpoolUsageThreshold, eventPublishClientEnabled, eventPublishMsgVpnEnabled, eventPublishSubscriptionMode, eventPublishTopicFormatMqttEnabled, eventPublishTopicFormatSmfEnabled, eventServiceAmqpConnectionCountThreshold, eventServiceMqttConnectionCountThreshold, eventServiceRestIncomingConnectionCountThreshold, eventServiceSmfConnectionCountThreshold, eventServiceWebConnectionCountThreshold, eventSubscriptionCountThreshold, eventTransactedSessionCountThreshold, eventTransactionCountThreshold, exportSubscriptionsEnabled, failureReason, jndiEnabled, loginRxMsgCount, loginTxMsgCount, maxConnectionCount, maxEffectiveEndpointCount, maxEffectiveRxFlowCount, maxEffectiveSubscriptionCount, maxEffectiveTransactedSessionCount, maxEffectiveTransactionCount, maxEffectiveTxFlowCount, maxEgressFlowCount, maxEndpointCount, maxIngressFlowCount, maxMsgSpoolUsage, maxSubscriptionCount, maxTransactedSessionCount, maxTransactionCount, mqttRetainMaxMemory, msgReplayActiveCount, msgReplayFailedCount, msgReplayInitializingCount, msgReplayPendingCompleteCount, msgSpoolMsgCount, msgSpoolRxMsgCount, msgSpoolTxMsgCount, msgSpoolUsage, msgVpnName, rate, replicationAckPropagationIntervalMsgCount, replicationActiveAckPropTxMsgCount, replicationActiveAsyncQueuedMsgCount, replicationActiveLocallyConsumedMsgCount, replicationActiveMateFlowCongestedPeakTime, replicationActiveMateFlowNotCongestedPeakTime, replicationActivePromotedQueuedMsgCount, replicationActiveReconcileRequestRxMsgCount, replicationActiveSyncEligiblePeakTime, replicationActiveSyncIneligiblePeakTime, replicationActiveSyncQueuedAsAsyncMsgCount, replicationActiveSyncQueuedMsgCount, replicationActiveTransitionToSyncIneligibleCount, replicationBridgeAuthenticationBasicClientUsername, replicationBridgeAuthenticationScheme, replicationBridgeBoundToQueue, replicationBridgeCompressedDataEnabled, replicationBridgeEgressFlowWindowSize, replicationBridgeName, replicationBridgeRetryDelay, replicationBridgeTlsEnabled, replicationBridgeUnidirectionalClientProfileName, replicationBridgeUp, replicationEnabled, replicationQueueBound, replicationQueueMaxMsgSpoolUsage, replicationQueueRejectMsgToSenderOnDiscardEnabled, replicationRejectMsgWhenSyncIneligibleEnabled, replicationRemoteBridgeName, replicationRemoteBridgeUp, replicationRole, replicationStandbyAckPropOutOfSeqRxMsgCount, replicationStandbyAckPropRxMsgCount, replicationStandbyReconcileRequestTxMsgCount, replicationStandbyRxMsgCount, replicationStandbyTransactionRequestCount, replicationStandbyTransactionRequestFailureCount, replicationStandbyTransactionRequestSuccessCount, replicationSyncEligible, replicationTransactionMode, restTlsServerCertEnforceTrustedCommonNameEnabled, restTlsServerCertMaxChainDepth, restTlsServerCertValidateDateEnabled, restTlsServerCertValidateNameEnabled, rxByteCount, rxByteRate, rxCompressedByteCount, rxCompressedByteRate, rxCompressionRatio, rxMsgCount, rxMsgRate, rxUncompressedByteCount, rxUncompressedByteRate, sempOverMsgBusAdminClientEnabled, sempOverMsgBusAdminDistributedCacheEnabled, sempOverMsgBusAdminEnabled, sempOverMsgBusEnabled, sempOverMsgBusShowEnabled, serviceAmqpMaxConnectionCount, serviceAmqpPlainTextCompressed, serviceAmqpPlainTextEnabled, serviceAmqpPlainTextFailureReason, serviceAmqpPlainTextListenPort, serviceAmqpPlainTextUp, serviceAmqpTlsCompressed, serviceAmqpTlsEnabled, serviceAmqpTlsFailureReason, serviceAmqpTlsListenPort, serviceAmqpTlsUp, serviceMqttMaxConnectionCount, serviceMqttPlainTextCompressed, serviceMqttPlainTextEnabled, serviceMqttPlainTextFailureReason, serviceMqttPlainTextListenPort, serviceMqttPlainTextUp, serviceMqttTlsCompressed, serviceMqttTlsEnabled, serviceMqttTlsFailureReason, serviceMqttTlsListenPort, serviceMqttTlsUp, serviceMqttTlsWebSocketCompressed, serviceMqttTlsWebSocketEnabled, serviceMqttTlsWebSocketFailureReason, serviceMqttTlsWebSocketListenPort, serviceMqttTlsWebSocketUp, serviceMqttWebSocketCompressed, serviceMqttWebSocketEnabled, serviceMqttWebSocketFailureReason, serviceMqttWebSocketListenPort, serviceMqttWebSocketUp, serviceRestIncomingMaxConnectionCount, serviceRestIncomingPlainTextCompressed, serviceRestIncomingPlainTextEnabled, serviceRestIncomingPlainTextFailureReason, serviceRestIncomingPlainTextListenPort, serviceRestIncomingPlainTextUp, serviceRestIncomingTlsCompressed, serviceRestIncomingTlsEnabled, serviceRestIncomingTlsFailureReason, serviceRestIncomingTlsListenPort, serviceRestIncomingTlsUp, serviceRestMode, serviceRestOutgoingMaxConnectionCount, serviceSmfMaxConnectionCount, serviceSmfPlainTextEnabled, serviceSmfPlainTextFailureReason, serviceSmfPlainTextUp, serviceSmfTlsEnabled, serviceSmfTlsFailureReason, serviceSmfTlsUp, serviceWebMaxConnectionCount, serviceWebPlainTextEnabled, serviceWebPlainTextFailureReason, serviceWebPlainTextUp, serviceWebTlsEnabled, serviceWebTlsFailureReason, serviceWebTlsUp, state, subscriptionExportProgress, systemManager, tlsAllowDowngradeToPlainTextEnabled, tlsAverageRxByteRate, tlsAverageTxByteRate, tlsRxByteCount, tlsRxByteRate, tlsTxByteCount, tlsTxByteRate, txByteCount, txByteRate, txCompressedByteCount, txCompressedByteRate, txCompressionRatio, txMsgCount, txMsgRate, txUncompressedByteCount, txUncompressedByteRate);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpn {\n");
    
    sb.append("    alias: ").append(toIndentedString(alias)).append("\n");
    sb.append("    authenticationBasicEnabled: ").append(toIndentedString(authenticationBasicEnabled)).append("\n");
    sb.append("    authenticationBasicProfileName: ").append(toIndentedString(authenticationBasicProfileName)).append("\n");
    sb.append("    authenticationBasicRadiusDomain: ").append(toIndentedString(authenticationBasicRadiusDomain)).append("\n");
    sb.append("    authenticationBasicType: ").append(toIndentedString(authenticationBasicType)).append("\n");
    sb.append("    authenticationClientCertAllowApiProvidedUsernameEnabled: ").append(toIndentedString(authenticationClientCertAllowApiProvidedUsernameEnabled)).append("\n");
    sb.append("    authenticationClientCertEnabled: ").append(toIndentedString(authenticationClientCertEnabled)).append("\n");
    sb.append("    authenticationClientCertMaxChainDepth: ").append(toIndentedString(authenticationClientCertMaxChainDepth)).append("\n");
    sb.append("    authenticationClientCertRevocationCheckMode: ").append(toIndentedString(authenticationClientCertRevocationCheckMode)).append("\n");
    sb.append("    authenticationClientCertUsernameSource: ").append(toIndentedString(authenticationClientCertUsernameSource)).append("\n");
    sb.append("    authenticationClientCertValidateDateEnabled: ").append(toIndentedString(authenticationClientCertValidateDateEnabled)).append("\n");
    sb.append("    authenticationKerberosAllowApiProvidedUsernameEnabled: ").append(toIndentedString(authenticationKerberosAllowApiProvidedUsernameEnabled)).append("\n");
    sb.append("    authenticationKerberosEnabled: ").append(toIndentedString(authenticationKerberosEnabled)).append("\n");
    sb.append("    authenticationOauthDefaultProviderName: ").append(toIndentedString(authenticationOauthDefaultProviderName)).append("\n");
    sb.append("    authenticationOauthEnabled: ").append(toIndentedString(authenticationOauthEnabled)).append("\n");
    sb.append("    authorizationLdapGroupMembershipAttributeName: ").append(toIndentedString(authorizationLdapGroupMembershipAttributeName)).append("\n");
    sb.append("    authorizationLdapTrimClientUsernameDomainEnabled: ").append(toIndentedString(authorizationLdapTrimClientUsernameDomainEnabled)).append("\n");
    sb.append("    authorizationProfileName: ").append(toIndentedString(authorizationProfileName)).append("\n");
    sb.append("    authorizationType: ").append(toIndentedString(authorizationType)).append("\n");
    sb.append("    averageRxByteRate: ").append(toIndentedString(averageRxByteRate)).append("\n");
    sb.append("    averageRxCompressedByteRate: ").append(toIndentedString(averageRxCompressedByteRate)).append("\n");
    sb.append("    averageRxMsgRate: ").append(toIndentedString(averageRxMsgRate)).append("\n");
    sb.append("    averageRxUncompressedByteRate: ").append(toIndentedString(averageRxUncompressedByteRate)).append("\n");
    sb.append("    averageTxByteRate: ").append(toIndentedString(averageTxByteRate)).append("\n");
    sb.append("    averageTxCompressedByteRate: ").append(toIndentedString(averageTxCompressedByteRate)).append("\n");
    sb.append("    averageTxMsgRate: ").append(toIndentedString(averageTxMsgRate)).append("\n");
    sb.append("    averageTxUncompressedByteRate: ").append(toIndentedString(averageTxUncompressedByteRate)).append("\n");
    sb.append("    bridgingTlsServerCertEnforceTrustedCommonNameEnabled: ").append(toIndentedString(bridgingTlsServerCertEnforceTrustedCommonNameEnabled)).append("\n");
    sb.append("    bridgingTlsServerCertMaxChainDepth: ").append(toIndentedString(bridgingTlsServerCertMaxChainDepth)).append("\n");
    sb.append("    bridgingTlsServerCertValidateDateEnabled: ").append(toIndentedString(bridgingTlsServerCertValidateDateEnabled)).append("\n");
    sb.append("    configSyncLocalKey: ").append(toIndentedString(configSyncLocalKey)).append("\n");
    sb.append("    configSyncLocalLastResult: ").append(toIndentedString(configSyncLocalLastResult)).append("\n");
    sb.append("    configSyncLocalRole: ").append(toIndentedString(configSyncLocalRole)).append("\n");
    sb.append("    configSyncLocalState: ").append(toIndentedString(configSyncLocalState)).append("\n");
    sb.append("    configSyncLocalTimeInState: ").append(toIndentedString(configSyncLocalTimeInState)).append("\n");
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
    sb.append("    distributedCacheManagementEnabled: ").append(toIndentedString(distributedCacheManagementEnabled)).append("\n");
    sb.append("    dmrEnabled: ").append(toIndentedString(dmrEnabled)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    eventConnectionCountThreshold: ").append(toIndentedString(eventConnectionCountThreshold)).append("\n");
    sb.append("    eventEgressFlowCountThreshold: ").append(toIndentedString(eventEgressFlowCountThreshold)).append("\n");
    sb.append("    eventEgressMsgRateThreshold: ").append(toIndentedString(eventEgressMsgRateThreshold)).append("\n");
    sb.append("    eventEndpointCountThreshold: ").append(toIndentedString(eventEndpointCountThreshold)).append("\n");
    sb.append("    eventIngressFlowCountThreshold: ").append(toIndentedString(eventIngressFlowCountThreshold)).append("\n");
    sb.append("    eventIngressMsgRateThreshold: ").append(toIndentedString(eventIngressMsgRateThreshold)).append("\n");
    sb.append("    eventLargeMsgThreshold: ").append(toIndentedString(eventLargeMsgThreshold)).append("\n");
    sb.append("    eventLogTag: ").append(toIndentedString(eventLogTag)).append("\n");
    sb.append("    eventMsgSpoolUsageThreshold: ").append(toIndentedString(eventMsgSpoolUsageThreshold)).append("\n");
    sb.append("    eventPublishClientEnabled: ").append(toIndentedString(eventPublishClientEnabled)).append("\n");
    sb.append("    eventPublishMsgVpnEnabled: ").append(toIndentedString(eventPublishMsgVpnEnabled)).append("\n");
    sb.append("    eventPublishSubscriptionMode: ").append(toIndentedString(eventPublishSubscriptionMode)).append("\n");
    sb.append("    eventPublishTopicFormatMqttEnabled: ").append(toIndentedString(eventPublishTopicFormatMqttEnabled)).append("\n");
    sb.append("    eventPublishTopicFormatSmfEnabled: ").append(toIndentedString(eventPublishTopicFormatSmfEnabled)).append("\n");
    sb.append("    eventServiceAmqpConnectionCountThreshold: ").append(toIndentedString(eventServiceAmqpConnectionCountThreshold)).append("\n");
    sb.append("    eventServiceMqttConnectionCountThreshold: ").append(toIndentedString(eventServiceMqttConnectionCountThreshold)).append("\n");
    sb.append("    eventServiceRestIncomingConnectionCountThreshold: ").append(toIndentedString(eventServiceRestIncomingConnectionCountThreshold)).append("\n");
    sb.append("    eventServiceSmfConnectionCountThreshold: ").append(toIndentedString(eventServiceSmfConnectionCountThreshold)).append("\n");
    sb.append("    eventServiceWebConnectionCountThreshold: ").append(toIndentedString(eventServiceWebConnectionCountThreshold)).append("\n");
    sb.append("    eventSubscriptionCountThreshold: ").append(toIndentedString(eventSubscriptionCountThreshold)).append("\n");
    sb.append("    eventTransactedSessionCountThreshold: ").append(toIndentedString(eventTransactedSessionCountThreshold)).append("\n");
    sb.append("    eventTransactionCountThreshold: ").append(toIndentedString(eventTransactionCountThreshold)).append("\n");
    sb.append("    exportSubscriptionsEnabled: ").append(toIndentedString(exportSubscriptionsEnabled)).append("\n");
    sb.append("    failureReason: ").append(toIndentedString(failureReason)).append("\n");
    sb.append("    jndiEnabled: ").append(toIndentedString(jndiEnabled)).append("\n");
    sb.append("    loginRxMsgCount: ").append(toIndentedString(loginRxMsgCount)).append("\n");
    sb.append("    loginTxMsgCount: ").append(toIndentedString(loginTxMsgCount)).append("\n");
    sb.append("    maxConnectionCount: ").append(toIndentedString(maxConnectionCount)).append("\n");
    sb.append("    maxEffectiveEndpointCount: ").append(toIndentedString(maxEffectiveEndpointCount)).append("\n");
    sb.append("    maxEffectiveRxFlowCount: ").append(toIndentedString(maxEffectiveRxFlowCount)).append("\n");
    sb.append("    maxEffectiveSubscriptionCount: ").append(toIndentedString(maxEffectiveSubscriptionCount)).append("\n");
    sb.append("    maxEffectiveTransactedSessionCount: ").append(toIndentedString(maxEffectiveTransactedSessionCount)).append("\n");
    sb.append("    maxEffectiveTransactionCount: ").append(toIndentedString(maxEffectiveTransactionCount)).append("\n");
    sb.append("    maxEffectiveTxFlowCount: ").append(toIndentedString(maxEffectiveTxFlowCount)).append("\n");
    sb.append("    maxEgressFlowCount: ").append(toIndentedString(maxEgressFlowCount)).append("\n");
    sb.append("    maxEndpointCount: ").append(toIndentedString(maxEndpointCount)).append("\n");
    sb.append("    maxIngressFlowCount: ").append(toIndentedString(maxIngressFlowCount)).append("\n");
    sb.append("    maxMsgSpoolUsage: ").append(toIndentedString(maxMsgSpoolUsage)).append("\n");
    sb.append("    maxSubscriptionCount: ").append(toIndentedString(maxSubscriptionCount)).append("\n");
    sb.append("    maxTransactedSessionCount: ").append(toIndentedString(maxTransactedSessionCount)).append("\n");
    sb.append("    maxTransactionCount: ").append(toIndentedString(maxTransactionCount)).append("\n");
    sb.append("    mqttRetainMaxMemory: ").append(toIndentedString(mqttRetainMaxMemory)).append("\n");
    sb.append("    msgReplayActiveCount: ").append(toIndentedString(msgReplayActiveCount)).append("\n");
    sb.append("    msgReplayFailedCount: ").append(toIndentedString(msgReplayFailedCount)).append("\n");
    sb.append("    msgReplayInitializingCount: ").append(toIndentedString(msgReplayInitializingCount)).append("\n");
    sb.append("    msgReplayPendingCompleteCount: ").append(toIndentedString(msgReplayPendingCompleteCount)).append("\n");
    sb.append("    msgSpoolMsgCount: ").append(toIndentedString(msgSpoolMsgCount)).append("\n");
    sb.append("    msgSpoolRxMsgCount: ").append(toIndentedString(msgSpoolRxMsgCount)).append("\n");
    sb.append("    msgSpoolTxMsgCount: ").append(toIndentedString(msgSpoolTxMsgCount)).append("\n");
    sb.append("    msgSpoolUsage: ").append(toIndentedString(msgSpoolUsage)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    rate: ").append(toIndentedString(rate)).append("\n");
    sb.append("    replicationAckPropagationIntervalMsgCount: ").append(toIndentedString(replicationAckPropagationIntervalMsgCount)).append("\n");
    sb.append("    replicationActiveAckPropTxMsgCount: ").append(toIndentedString(replicationActiveAckPropTxMsgCount)).append("\n");
    sb.append("    replicationActiveAsyncQueuedMsgCount: ").append(toIndentedString(replicationActiveAsyncQueuedMsgCount)).append("\n");
    sb.append("    replicationActiveLocallyConsumedMsgCount: ").append(toIndentedString(replicationActiveLocallyConsumedMsgCount)).append("\n");
    sb.append("    replicationActiveMateFlowCongestedPeakTime: ").append(toIndentedString(replicationActiveMateFlowCongestedPeakTime)).append("\n");
    sb.append("    replicationActiveMateFlowNotCongestedPeakTime: ").append(toIndentedString(replicationActiveMateFlowNotCongestedPeakTime)).append("\n");
    sb.append("    replicationActivePromotedQueuedMsgCount: ").append(toIndentedString(replicationActivePromotedQueuedMsgCount)).append("\n");
    sb.append("    replicationActiveReconcileRequestRxMsgCount: ").append(toIndentedString(replicationActiveReconcileRequestRxMsgCount)).append("\n");
    sb.append("    replicationActiveSyncEligiblePeakTime: ").append(toIndentedString(replicationActiveSyncEligiblePeakTime)).append("\n");
    sb.append("    replicationActiveSyncIneligiblePeakTime: ").append(toIndentedString(replicationActiveSyncIneligiblePeakTime)).append("\n");
    sb.append("    replicationActiveSyncQueuedAsAsyncMsgCount: ").append(toIndentedString(replicationActiveSyncQueuedAsAsyncMsgCount)).append("\n");
    sb.append("    replicationActiveSyncQueuedMsgCount: ").append(toIndentedString(replicationActiveSyncQueuedMsgCount)).append("\n");
    sb.append("    replicationActiveTransitionToSyncIneligibleCount: ").append(toIndentedString(replicationActiveTransitionToSyncIneligibleCount)).append("\n");
    sb.append("    replicationBridgeAuthenticationBasicClientUsername: ").append(toIndentedString(replicationBridgeAuthenticationBasicClientUsername)).append("\n");
    sb.append("    replicationBridgeAuthenticationScheme: ").append(toIndentedString(replicationBridgeAuthenticationScheme)).append("\n");
    sb.append("    replicationBridgeBoundToQueue: ").append(toIndentedString(replicationBridgeBoundToQueue)).append("\n");
    sb.append("    replicationBridgeCompressedDataEnabled: ").append(toIndentedString(replicationBridgeCompressedDataEnabled)).append("\n");
    sb.append("    replicationBridgeEgressFlowWindowSize: ").append(toIndentedString(replicationBridgeEgressFlowWindowSize)).append("\n");
    sb.append("    replicationBridgeName: ").append(toIndentedString(replicationBridgeName)).append("\n");
    sb.append("    replicationBridgeRetryDelay: ").append(toIndentedString(replicationBridgeRetryDelay)).append("\n");
    sb.append("    replicationBridgeTlsEnabled: ").append(toIndentedString(replicationBridgeTlsEnabled)).append("\n");
    sb.append("    replicationBridgeUnidirectionalClientProfileName: ").append(toIndentedString(replicationBridgeUnidirectionalClientProfileName)).append("\n");
    sb.append("    replicationBridgeUp: ").append(toIndentedString(replicationBridgeUp)).append("\n");
    sb.append("    replicationEnabled: ").append(toIndentedString(replicationEnabled)).append("\n");
    sb.append("    replicationQueueBound: ").append(toIndentedString(replicationQueueBound)).append("\n");
    sb.append("    replicationQueueMaxMsgSpoolUsage: ").append(toIndentedString(replicationQueueMaxMsgSpoolUsage)).append("\n");
    sb.append("    replicationQueueRejectMsgToSenderOnDiscardEnabled: ").append(toIndentedString(replicationQueueRejectMsgToSenderOnDiscardEnabled)).append("\n");
    sb.append("    replicationRejectMsgWhenSyncIneligibleEnabled: ").append(toIndentedString(replicationRejectMsgWhenSyncIneligibleEnabled)).append("\n");
    sb.append("    replicationRemoteBridgeName: ").append(toIndentedString(replicationRemoteBridgeName)).append("\n");
    sb.append("    replicationRemoteBridgeUp: ").append(toIndentedString(replicationRemoteBridgeUp)).append("\n");
    sb.append("    replicationRole: ").append(toIndentedString(replicationRole)).append("\n");
    sb.append("    replicationStandbyAckPropOutOfSeqRxMsgCount: ").append(toIndentedString(replicationStandbyAckPropOutOfSeqRxMsgCount)).append("\n");
    sb.append("    replicationStandbyAckPropRxMsgCount: ").append(toIndentedString(replicationStandbyAckPropRxMsgCount)).append("\n");
    sb.append("    replicationStandbyReconcileRequestTxMsgCount: ").append(toIndentedString(replicationStandbyReconcileRequestTxMsgCount)).append("\n");
    sb.append("    replicationStandbyRxMsgCount: ").append(toIndentedString(replicationStandbyRxMsgCount)).append("\n");
    sb.append("    replicationStandbyTransactionRequestCount: ").append(toIndentedString(replicationStandbyTransactionRequestCount)).append("\n");
    sb.append("    replicationStandbyTransactionRequestFailureCount: ").append(toIndentedString(replicationStandbyTransactionRequestFailureCount)).append("\n");
    sb.append("    replicationStandbyTransactionRequestSuccessCount: ").append(toIndentedString(replicationStandbyTransactionRequestSuccessCount)).append("\n");
    sb.append("    replicationSyncEligible: ").append(toIndentedString(replicationSyncEligible)).append("\n");
    sb.append("    replicationTransactionMode: ").append(toIndentedString(replicationTransactionMode)).append("\n");
    sb.append("    restTlsServerCertEnforceTrustedCommonNameEnabled: ").append(toIndentedString(restTlsServerCertEnforceTrustedCommonNameEnabled)).append("\n");
    sb.append("    restTlsServerCertMaxChainDepth: ").append(toIndentedString(restTlsServerCertMaxChainDepth)).append("\n");
    sb.append("    restTlsServerCertValidateDateEnabled: ").append(toIndentedString(restTlsServerCertValidateDateEnabled)).append("\n");
    sb.append("    restTlsServerCertValidateNameEnabled: ").append(toIndentedString(restTlsServerCertValidateNameEnabled)).append("\n");
    sb.append("    rxByteCount: ").append(toIndentedString(rxByteCount)).append("\n");
    sb.append("    rxByteRate: ").append(toIndentedString(rxByteRate)).append("\n");
    sb.append("    rxCompressedByteCount: ").append(toIndentedString(rxCompressedByteCount)).append("\n");
    sb.append("    rxCompressedByteRate: ").append(toIndentedString(rxCompressedByteRate)).append("\n");
    sb.append("    rxCompressionRatio: ").append(toIndentedString(rxCompressionRatio)).append("\n");
    sb.append("    rxMsgCount: ").append(toIndentedString(rxMsgCount)).append("\n");
    sb.append("    rxMsgRate: ").append(toIndentedString(rxMsgRate)).append("\n");
    sb.append("    rxUncompressedByteCount: ").append(toIndentedString(rxUncompressedByteCount)).append("\n");
    sb.append("    rxUncompressedByteRate: ").append(toIndentedString(rxUncompressedByteRate)).append("\n");
    sb.append("    sempOverMsgBusAdminClientEnabled: ").append(toIndentedString(sempOverMsgBusAdminClientEnabled)).append("\n");
    sb.append("    sempOverMsgBusAdminDistributedCacheEnabled: ").append(toIndentedString(sempOverMsgBusAdminDistributedCacheEnabled)).append("\n");
    sb.append("    sempOverMsgBusAdminEnabled: ").append(toIndentedString(sempOverMsgBusAdminEnabled)).append("\n");
    sb.append("    sempOverMsgBusEnabled: ").append(toIndentedString(sempOverMsgBusEnabled)).append("\n");
    sb.append("    sempOverMsgBusShowEnabled: ").append(toIndentedString(sempOverMsgBusShowEnabled)).append("\n");
    sb.append("    serviceAmqpMaxConnectionCount: ").append(toIndentedString(serviceAmqpMaxConnectionCount)).append("\n");
    sb.append("    serviceAmqpPlainTextCompressed: ").append(toIndentedString(serviceAmqpPlainTextCompressed)).append("\n");
    sb.append("    serviceAmqpPlainTextEnabled: ").append(toIndentedString(serviceAmqpPlainTextEnabled)).append("\n");
    sb.append("    serviceAmqpPlainTextFailureReason: ").append(toIndentedString(serviceAmqpPlainTextFailureReason)).append("\n");
    sb.append("    serviceAmqpPlainTextListenPort: ").append(toIndentedString(serviceAmqpPlainTextListenPort)).append("\n");
    sb.append("    serviceAmqpPlainTextUp: ").append(toIndentedString(serviceAmqpPlainTextUp)).append("\n");
    sb.append("    serviceAmqpTlsCompressed: ").append(toIndentedString(serviceAmqpTlsCompressed)).append("\n");
    sb.append("    serviceAmqpTlsEnabled: ").append(toIndentedString(serviceAmqpTlsEnabled)).append("\n");
    sb.append("    serviceAmqpTlsFailureReason: ").append(toIndentedString(serviceAmqpTlsFailureReason)).append("\n");
    sb.append("    serviceAmqpTlsListenPort: ").append(toIndentedString(serviceAmqpTlsListenPort)).append("\n");
    sb.append("    serviceAmqpTlsUp: ").append(toIndentedString(serviceAmqpTlsUp)).append("\n");
    sb.append("    serviceMqttMaxConnectionCount: ").append(toIndentedString(serviceMqttMaxConnectionCount)).append("\n");
    sb.append("    serviceMqttPlainTextCompressed: ").append(toIndentedString(serviceMqttPlainTextCompressed)).append("\n");
    sb.append("    serviceMqttPlainTextEnabled: ").append(toIndentedString(serviceMqttPlainTextEnabled)).append("\n");
    sb.append("    serviceMqttPlainTextFailureReason: ").append(toIndentedString(serviceMqttPlainTextFailureReason)).append("\n");
    sb.append("    serviceMqttPlainTextListenPort: ").append(toIndentedString(serviceMqttPlainTextListenPort)).append("\n");
    sb.append("    serviceMqttPlainTextUp: ").append(toIndentedString(serviceMqttPlainTextUp)).append("\n");
    sb.append("    serviceMqttTlsCompressed: ").append(toIndentedString(serviceMqttTlsCompressed)).append("\n");
    sb.append("    serviceMqttTlsEnabled: ").append(toIndentedString(serviceMqttTlsEnabled)).append("\n");
    sb.append("    serviceMqttTlsFailureReason: ").append(toIndentedString(serviceMqttTlsFailureReason)).append("\n");
    sb.append("    serviceMqttTlsListenPort: ").append(toIndentedString(serviceMqttTlsListenPort)).append("\n");
    sb.append("    serviceMqttTlsUp: ").append(toIndentedString(serviceMqttTlsUp)).append("\n");
    sb.append("    serviceMqttTlsWebSocketCompressed: ").append(toIndentedString(serviceMqttTlsWebSocketCompressed)).append("\n");
    sb.append("    serviceMqttTlsWebSocketEnabled: ").append(toIndentedString(serviceMqttTlsWebSocketEnabled)).append("\n");
    sb.append("    serviceMqttTlsWebSocketFailureReason: ").append(toIndentedString(serviceMqttTlsWebSocketFailureReason)).append("\n");
    sb.append("    serviceMqttTlsWebSocketListenPort: ").append(toIndentedString(serviceMqttTlsWebSocketListenPort)).append("\n");
    sb.append("    serviceMqttTlsWebSocketUp: ").append(toIndentedString(serviceMqttTlsWebSocketUp)).append("\n");
    sb.append("    serviceMqttWebSocketCompressed: ").append(toIndentedString(serviceMqttWebSocketCompressed)).append("\n");
    sb.append("    serviceMqttWebSocketEnabled: ").append(toIndentedString(serviceMqttWebSocketEnabled)).append("\n");
    sb.append("    serviceMqttWebSocketFailureReason: ").append(toIndentedString(serviceMqttWebSocketFailureReason)).append("\n");
    sb.append("    serviceMqttWebSocketListenPort: ").append(toIndentedString(serviceMqttWebSocketListenPort)).append("\n");
    sb.append("    serviceMqttWebSocketUp: ").append(toIndentedString(serviceMqttWebSocketUp)).append("\n");
    sb.append("    serviceRestIncomingMaxConnectionCount: ").append(toIndentedString(serviceRestIncomingMaxConnectionCount)).append("\n");
    sb.append("    serviceRestIncomingPlainTextCompressed: ").append(toIndentedString(serviceRestIncomingPlainTextCompressed)).append("\n");
    sb.append("    serviceRestIncomingPlainTextEnabled: ").append(toIndentedString(serviceRestIncomingPlainTextEnabled)).append("\n");
    sb.append("    serviceRestIncomingPlainTextFailureReason: ").append(toIndentedString(serviceRestIncomingPlainTextFailureReason)).append("\n");
    sb.append("    serviceRestIncomingPlainTextListenPort: ").append(toIndentedString(serviceRestIncomingPlainTextListenPort)).append("\n");
    sb.append("    serviceRestIncomingPlainTextUp: ").append(toIndentedString(serviceRestIncomingPlainTextUp)).append("\n");
    sb.append("    serviceRestIncomingTlsCompressed: ").append(toIndentedString(serviceRestIncomingTlsCompressed)).append("\n");
    sb.append("    serviceRestIncomingTlsEnabled: ").append(toIndentedString(serviceRestIncomingTlsEnabled)).append("\n");
    sb.append("    serviceRestIncomingTlsFailureReason: ").append(toIndentedString(serviceRestIncomingTlsFailureReason)).append("\n");
    sb.append("    serviceRestIncomingTlsListenPort: ").append(toIndentedString(serviceRestIncomingTlsListenPort)).append("\n");
    sb.append("    serviceRestIncomingTlsUp: ").append(toIndentedString(serviceRestIncomingTlsUp)).append("\n");
    sb.append("    serviceRestMode: ").append(toIndentedString(serviceRestMode)).append("\n");
    sb.append("    serviceRestOutgoingMaxConnectionCount: ").append(toIndentedString(serviceRestOutgoingMaxConnectionCount)).append("\n");
    sb.append("    serviceSmfMaxConnectionCount: ").append(toIndentedString(serviceSmfMaxConnectionCount)).append("\n");
    sb.append("    serviceSmfPlainTextEnabled: ").append(toIndentedString(serviceSmfPlainTextEnabled)).append("\n");
    sb.append("    serviceSmfPlainTextFailureReason: ").append(toIndentedString(serviceSmfPlainTextFailureReason)).append("\n");
    sb.append("    serviceSmfPlainTextUp: ").append(toIndentedString(serviceSmfPlainTextUp)).append("\n");
    sb.append("    serviceSmfTlsEnabled: ").append(toIndentedString(serviceSmfTlsEnabled)).append("\n");
    sb.append("    serviceSmfTlsFailureReason: ").append(toIndentedString(serviceSmfTlsFailureReason)).append("\n");
    sb.append("    serviceSmfTlsUp: ").append(toIndentedString(serviceSmfTlsUp)).append("\n");
    sb.append("    serviceWebMaxConnectionCount: ").append(toIndentedString(serviceWebMaxConnectionCount)).append("\n");
    sb.append("    serviceWebPlainTextEnabled: ").append(toIndentedString(serviceWebPlainTextEnabled)).append("\n");
    sb.append("    serviceWebPlainTextFailureReason: ").append(toIndentedString(serviceWebPlainTextFailureReason)).append("\n");
    sb.append("    serviceWebPlainTextUp: ").append(toIndentedString(serviceWebPlainTextUp)).append("\n");
    sb.append("    serviceWebTlsEnabled: ").append(toIndentedString(serviceWebTlsEnabled)).append("\n");
    sb.append("    serviceWebTlsFailureReason: ").append(toIndentedString(serviceWebTlsFailureReason)).append("\n");
    sb.append("    serviceWebTlsUp: ").append(toIndentedString(serviceWebTlsUp)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    subscriptionExportProgress: ").append(toIndentedString(subscriptionExportProgress)).append("\n");
    sb.append("    systemManager: ").append(toIndentedString(systemManager)).append("\n");
    sb.append("    tlsAllowDowngradeToPlainTextEnabled: ").append(toIndentedString(tlsAllowDowngradeToPlainTextEnabled)).append("\n");
    sb.append("    tlsAverageRxByteRate: ").append(toIndentedString(tlsAverageRxByteRate)).append("\n");
    sb.append("    tlsAverageTxByteRate: ").append(toIndentedString(tlsAverageTxByteRate)).append("\n");
    sb.append("    tlsRxByteCount: ").append(toIndentedString(tlsRxByteCount)).append("\n");
    sb.append("    tlsRxByteRate: ").append(toIndentedString(tlsRxByteRate)).append("\n");
    sb.append("    tlsTxByteCount: ").append(toIndentedString(tlsTxByteCount)).append("\n");
    sb.append("    tlsTxByteRate: ").append(toIndentedString(tlsTxByteRate)).append("\n");
    sb.append("    txByteCount: ").append(toIndentedString(txByteCount)).append("\n");
    sb.append("    txByteRate: ").append(toIndentedString(txByteRate)).append("\n");
    sb.append("    txCompressedByteCount: ").append(toIndentedString(txCompressedByteCount)).append("\n");
    sb.append("    txCompressedByteRate: ").append(toIndentedString(txCompressedByteRate)).append("\n");
    sb.append("    txCompressionRatio: ").append(toIndentedString(txCompressionRatio)).append("\n");
    sb.append("    txMsgCount: ").append(toIndentedString(txMsgCount)).append("\n");
    sb.append("    txMsgRate: ").append(toIndentedString(txMsgRate)).append("\n");
    sb.append("    txUncompressedByteCount: ").append(toIndentedString(txUncompressedByteCount)).append("\n");
    sb.append("    txUncompressedByteRate: ").append(toIndentedString(txUncompressedByteRate)).append("\n");
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
