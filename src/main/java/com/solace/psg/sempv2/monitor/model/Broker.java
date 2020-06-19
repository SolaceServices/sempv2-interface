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
 * Broker
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-13T23:07:13.589Z")
public class Broker {
  /**
   * The client certificate revocation checking mode used when a client authenticates with a client certificate. The allowed values and their meaning are:  &lt;pre&gt; \&quot;none\&quot; - Do not perform any certificate revocation checking. \&quot;ocsp\&quot; - Use the Open Certificate Status Protcol (OCSP) for certificate revocation checking. \&quot;crl\&quot; - Use Certificate Revocation Lists (CRL) for certificate revocation checking. \&quot;ocsp-crl\&quot; - Use OCSP first, but if OCSP fails to return an unambiguous result, then check via CRL. &lt;/pre&gt; 
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

  @SerializedName("tlsBlockVersion10Enabled")
  private Boolean tlsBlockVersion10Enabled = null;

  @SerializedName("tlsBlockVersion11Enabled")
  private Boolean tlsBlockVersion11Enabled = null;

  @SerializedName("tlsCipherSuiteManagementDefaultList")
  private String tlsCipherSuiteManagementDefaultList = null;

  @SerializedName("tlsCipherSuiteManagementList")
  private String tlsCipherSuiteManagementList = null;

  @SerializedName("tlsCipherSuiteManagementSupportedList")
  private String tlsCipherSuiteManagementSupportedList = null;

  @SerializedName("tlsCipherSuiteMsgBackboneDefaultList")
  private String tlsCipherSuiteMsgBackboneDefaultList = null;

  @SerializedName("tlsCipherSuiteMsgBackboneList")
  private String tlsCipherSuiteMsgBackboneList = null;

  @SerializedName("tlsCipherSuiteMsgBackboneSupportedList")
  private String tlsCipherSuiteMsgBackboneSupportedList = null;

  @SerializedName("tlsCipherSuiteSecureShellDefaultList")
  private String tlsCipherSuiteSecureShellDefaultList = null;

  @SerializedName("tlsCipherSuiteSecureShellList")
  private String tlsCipherSuiteSecureShellList = null;

  @SerializedName("tlsCipherSuiteSecureShellSupportedList")
  private String tlsCipherSuiteSecureShellSupportedList = null;

  @SerializedName("tlsCrimeExploitProtectionEnabled")
  private Boolean tlsCrimeExploitProtectionEnabled = null;

  @SerializedName("tlsTicketLifetime")
  private Integer tlsTicketLifetime = null;

  @SerializedName("tlsVersionSupportedList")
  private String tlsVersionSupportedList = null;

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

  public Broker authClientCertRevocationCheckMode(AuthClientCertRevocationCheckModeEnum authClientCertRevocationCheckMode) {
    this.authClientCertRevocationCheckMode = authClientCertRevocationCheckMode;
    return this;
  }

   /**
   * The client certificate revocation checking mode used when a client authenticates with a client certificate. The allowed values and their meaning are:  &lt;pre&gt; \&quot;none\&quot; - Do not perform any certificate revocation checking. \&quot;ocsp\&quot; - Use the Open Certificate Status Protcol (OCSP) for certificate revocation checking. \&quot;crl\&quot; - Use Certificate Revocation Lists (CRL) for certificate revocation checking. \&quot;ocsp-crl\&quot; - Use OCSP first, but if OCSP fails to return an unambiguous result, then check via CRL. &lt;/pre&gt; 
   * @return authClientCertRevocationCheckMode
  **/
  @ApiModelProperty(value = "The client certificate revocation checking mode used when a client authenticates with a client certificate. The allowed values and their meaning are:  <pre> \"none\" - Do not perform any certificate revocation checking. \"ocsp\" - Use the Open Certificate Status Protcol (OCSP) for certificate revocation checking. \"crl\" - Use Certificate Revocation Lists (CRL) for certificate revocation checking. \"ocsp-crl\" - Use OCSP first, but if OCSP fails to return an unambiguous result, then check via CRL. </pre> ")
  public AuthClientCertRevocationCheckModeEnum getAuthClientCertRevocationCheckMode() {
    return authClientCertRevocationCheckMode;
  }

  public void setAuthClientCertRevocationCheckMode(AuthClientCertRevocationCheckModeEnum authClientCertRevocationCheckMode) {
    this.authClientCertRevocationCheckMode = authClientCertRevocationCheckMode;
  }

  public Broker averageRxByteRate(Long averageRxByteRate) {
    this.averageRxByteRate = averageRxByteRate;
    return this;
  }

   /**
   * The one minute average of the message rate received by the Broker, in bytes per second (B/sec). Available since 2.14.
   * @return averageRxByteRate
  **/
  @ApiModelProperty(value = "The one minute average of the message rate received by the Broker, in bytes per second (B/sec). Available since 2.14.")
  public Long getAverageRxByteRate() {
    return averageRxByteRate;
  }

  public void setAverageRxByteRate(Long averageRxByteRate) {
    this.averageRxByteRate = averageRxByteRate;
  }

  public Broker averageRxCompressedByteRate(Long averageRxCompressedByteRate) {
    this.averageRxCompressedByteRate = averageRxCompressedByteRate;
    return this;
  }

   /**
   * The one minute average of the compressed message rate received by the Broker, in bytes per second (B/sec). Available since 2.14.
   * @return averageRxCompressedByteRate
  **/
  @ApiModelProperty(value = "The one minute average of the compressed message rate received by the Broker, in bytes per second (B/sec). Available since 2.14.")
  public Long getAverageRxCompressedByteRate() {
    return averageRxCompressedByteRate;
  }

  public void setAverageRxCompressedByteRate(Long averageRxCompressedByteRate) {
    this.averageRxCompressedByteRate = averageRxCompressedByteRate;
  }

  public Broker averageRxMsgRate(Long averageRxMsgRate) {
    this.averageRxMsgRate = averageRxMsgRate;
    return this;
  }

   /**
   * The one minute average of the message rate received by the Broker, in messages per second (msg/sec). Available since 2.14.
   * @return averageRxMsgRate
  **/
  @ApiModelProperty(value = "The one minute average of the message rate received by the Broker, in messages per second (msg/sec). Available since 2.14.")
  public Long getAverageRxMsgRate() {
    return averageRxMsgRate;
  }

  public void setAverageRxMsgRate(Long averageRxMsgRate) {
    this.averageRxMsgRate = averageRxMsgRate;
  }

  public Broker averageRxUncompressedByteRate(Long averageRxUncompressedByteRate) {
    this.averageRxUncompressedByteRate = averageRxUncompressedByteRate;
    return this;
  }

   /**
   * The one minute average of the uncompressed message rate received by the Broker, in bytes per second (B/sec). Available since 2.14.
   * @return averageRxUncompressedByteRate
  **/
  @ApiModelProperty(value = "The one minute average of the uncompressed message rate received by the Broker, in bytes per second (B/sec). Available since 2.14.")
  public Long getAverageRxUncompressedByteRate() {
    return averageRxUncompressedByteRate;
  }

  public void setAverageRxUncompressedByteRate(Long averageRxUncompressedByteRate) {
    this.averageRxUncompressedByteRate = averageRxUncompressedByteRate;
  }

  public Broker averageTxByteRate(Long averageTxByteRate) {
    this.averageTxByteRate = averageTxByteRate;
    return this;
  }

   /**
   * The one minute average of the message rate transmitted by the Broker, in bytes per second (B/sec). Available since 2.14.
   * @return averageTxByteRate
  **/
  @ApiModelProperty(value = "The one minute average of the message rate transmitted by the Broker, in bytes per second (B/sec). Available since 2.14.")
  public Long getAverageTxByteRate() {
    return averageTxByteRate;
  }

  public void setAverageTxByteRate(Long averageTxByteRate) {
    this.averageTxByteRate = averageTxByteRate;
  }

  public Broker averageTxCompressedByteRate(Long averageTxCompressedByteRate) {
    this.averageTxCompressedByteRate = averageTxCompressedByteRate;
    return this;
  }

   /**
   * The one minute average of the compressed message rate transmitted by the Broker, in bytes per second (B/sec). Available since 2.14.
   * @return averageTxCompressedByteRate
  **/
  @ApiModelProperty(value = "The one minute average of the compressed message rate transmitted by the Broker, in bytes per second (B/sec). Available since 2.14.")
  public Long getAverageTxCompressedByteRate() {
    return averageTxCompressedByteRate;
  }

  public void setAverageTxCompressedByteRate(Long averageTxCompressedByteRate) {
    this.averageTxCompressedByteRate = averageTxCompressedByteRate;
  }

  public Broker averageTxMsgRate(Long averageTxMsgRate) {
    this.averageTxMsgRate = averageTxMsgRate;
    return this;
  }

   /**
   * The one minute average of the message rate transmitted by the Broker, in messages per second (msg/sec). Available since 2.14.
   * @return averageTxMsgRate
  **/
  @ApiModelProperty(value = "The one minute average of the message rate transmitted by the Broker, in messages per second (msg/sec). Available since 2.14.")
  public Long getAverageTxMsgRate() {
    return averageTxMsgRate;
  }

  public void setAverageTxMsgRate(Long averageTxMsgRate) {
    this.averageTxMsgRate = averageTxMsgRate;
  }

  public Broker averageTxUncompressedByteRate(Long averageTxUncompressedByteRate) {
    this.averageTxUncompressedByteRate = averageTxUncompressedByteRate;
    return this;
  }

   /**
   * The one minute average of the uncompressed message rate transmitted by the Broker, in bytes per second (B/sec). Available since 2.14.
   * @return averageTxUncompressedByteRate
  **/
  @ApiModelProperty(value = "The one minute average of the uncompressed message rate transmitted by the Broker, in bytes per second (B/sec). Available since 2.14.")
  public Long getAverageTxUncompressedByteRate() {
    return averageTxUncompressedByteRate;
  }

  public void setAverageTxUncompressedByteRate(Long averageTxUncompressedByteRate) {
    this.averageTxUncompressedByteRate = averageTxUncompressedByteRate;
  }

  public Broker rxByteCount(Long rxByteCount) {
    this.rxByteCount = rxByteCount;
    return this;
  }

   /**
   * The amount of messages received from clients by the Broker, in bytes (B). Available since 2.14.
   * @return rxByteCount
  **/
  @ApiModelProperty(value = "The amount of messages received from clients by the Broker, in bytes (B). Available since 2.14.")
  public Long getRxByteCount() {
    return rxByteCount;
  }

  public void setRxByteCount(Long rxByteCount) {
    this.rxByteCount = rxByteCount;
  }

  public Broker rxByteRate(Long rxByteRate) {
    this.rxByteRate = rxByteRate;
    return this;
  }

   /**
   * The current message rate received by the Broker, in bytes per second (B/sec). Available since 2.14.
   * @return rxByteRate
  **/
  @ApiModelProperty(value = "The current message rate received by the Broker, in bytes per second (B/sec). Available since 2.14.")
  public Long getRxByteRate() {
    return rxByteRate;
  }

  public void setRxByteRate(Long rxByteRate) {
    this.rxByteRate = rxByteRate;
  }

  public Broker rxCompressedByteCount(Long rxCompressedByteCount) {
    this.rxCompressedByteCount = rxCompressedByteCount;
    return this;
  }

   /**
   * The amount of compressed messages received by the Broker, in bytes (B). Available since 2.14.
   * @return rxCompressedByteCount
  **/
  @ApiModelProperty(value = "The amount of compressed messages received by the Broker, in bytes (B). Available since 2.14.")
  public Long getRxCompressedByteCount() {
    return rxCompressedByteCount;
  }

  public void setRxCompressedByteCount(Long rxCompressedByteCount) {
    this.rxCompressedByteCount = rxCompressedByteCount;
  }

  public Broker rxCompressedByteRate(Long rxCompressedByteRate) {
    this.rxCompressedByteRate = rxCompressedByteRate;
    return this;
  }

   /**
   * The current compressed message rate received by the Broker, in bytes per second (B/sec). Available since 2.14.
   * @return rxCompressedByteRate
  **/
  @ApiModelProperty(value = "The current compressed message rate received by the Broker, in bytes per second (B/sec). Available since 2.14.")
  public Long getRxCompressedByteRate() {
    return rxCompressedByteRate;
  }

  public void setRxCompressedByteRate(Long rxCompressedByteRate) {
    this.rxCompressedByteRate = rxCompressedByteRate;
  }

  public Broker rxCompressionRatio(String rxCompressionRatio) {
    this.rxCompressionRatio = rxCompressionRatio;
    return this;
  }

   /**
   * The compression ratio for messages received by the Broker. Available since 2.14.
   * @return rxCompressionRatio
  **/
  @ApiModelProperty(value = "The compression ratio for messages received by the Broker. Available since 2.14.")
  public String getRxCompressionRatio() {
    return rxCompressionRatio;
  }

  public void setRxCompressionRatio(String rxCompressionRatio) {
    this.rxCompressionRatio = rxCompressionRatio;
  }

  public Broker rxMsgCount(Long rxMsgCount) {
    this.rxMsgCount = rxMsgCount;
    return this;
  }

   /**
   * The number of messages received from clients by the Broker. Available since 2.14.
   * @return rxMsgCount
  **/
  @ApiModelProperty(value = "The number of messages received from clients by the Broker. Available since 2.14.")
  public Long getRxMsgCount() {
    return rxMsgCount;
  }

  public void setRxMsgCount(Long rxMsgCount) {
    this.rxMsgCount = rxMsgCount;
  }

  public Broker rxMsgRate(Long rxMsgRate) {
    this.rxMsgRate = rxMsgRate;
    return this;
  }

   /**
   * The current message rate received by the Broker, in messages per second (msg/sec). Available since 2.14.
   * @return rxMsgRate
  **/
  @ApiModelProperty(value = "The current message rate received by the Broker, in messages per second (msg/sec). Available since 2.14.")
  public Long getRxMsgRate() {
    return rxMsgRate;
  }

  public void setRxMsgRate(Long rxMsgRate) {
    this.rxMsgRate = rxMsgRate;
  }

  public Broker rxUncompressedByteCount(Long rxUncompressedByteCount) {
    this.rxUncompressedByteCount = rxUncompressedByteCount;
    return this;
  }

   /**
   * The amount of uncompressed messages received by the Broker, in bytes (B). Available since 2.14.
   * @return rxUncompressedByteCount
  **/
  @ApiModelProperty(value = "The amount of uncompressed messages received by the Broker, in bytes (B). Available since 2.14.")
  public Long getRxUncompressedByteCount() {
    return rxUncompressedByteCount;
  }

  public void setRxUncompressedByteCount(Long rxUncompressedByteCount) {
    this.rxUncompressedByteCount = rxUncompressedByteCount;
  }

  public Broker rxUncompressedByteRate(Long rxUncompressedByteRate) {
    this.rxUncompressedByteRate = rxUncompressedByteRate;
    return this;
  }

   /**
   * The current uncompressed message rate received by the Broker, in bytes per second (B/sec). Available since 2.14.
   * @return rxUncompressedByteRate
  **/
  @ApiModelProperty(value = "The current uncompressed message rate received by the Broker, in bytes per second (B/sec). Available since 2.14.")
  public Long getRxUncompressedByteRate() {
    return rxUncompressedByteRate;
  }

  public void setRxUncompressedByteRate(Long rxUncompressedByteRate) {
    this.rxUncompressedByteRate = rxUncompressedByteRate;
  }

  public Broker tlsBlockVersion10Enabled(Boolean tlsBlockVersion10Enabled) {
    this.tlsBlockVersion10Enabled = tlsBlockVersion10Enabled;
    return this;
  }

   /**
   * Indicates whether incoming TLS version 1.0 connections are blocked. When blocked, existing TLS 1.0 connections from Clients and SEMP users remain connected while new connections are blocked. Note that support for TLS 1.0 will eventually be discontinued, at which time TLS 1.0 connections will be blocked regardless of this setting.
   * @return tlsBlockVersion10Enabled
  **/
  @ApiModelProperty(value = "Indicates whether incoming TLS version 1.0 connections are blocked. When blocked, existing TLS 1.0 connections from Clients and SEMP users remain connected while new connections are blocked. Note that support for TLS 1.0 will eventually be discontinued, at which time TLS 1.0 connections will be blocked regardless of this setting.")
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
   * Indicates whether TLS version 1.1 connections are blocked. When blocked, all existing incoming and outgoing TLS 1.1 connections with Clients, SEMP users, and LDAP servers remain connected while new connections are blocked. Note that support for TLS 1.1 will eventually be discontinued, at which time TLS 1.1 connections will be blocked regardless of this setting.
   * @return tlsBlockVersion11Enabled
  **/
  @ApiModelProperty(value = "Indicates whether TLS version 1.1 connections are blocked. When blocked, all existing incoming and outgoing TLS 1.1 connections with Clients, SEMP users, and LDAP servers remain connected while new connections are blocked. Note that support for TLS 1.1 will eventually be discontinued, at which time TLS 1.1 connections will be blocked regardless of this setting.")
  public Boolean isTlsBlockVersion11Enabled() {
    return tlsBlockVersion11Enabled;
  }

  public void setTlsBlockVersion11Enabled(Boolean tlsBlockVersion11Enabled) {
    this.tlsBlockVersion11Enabled = tlsBlockVersion11Enabled;
  }

  public Broker tlsCipherSuiteManagementDefaultList(String tlsCipherSuiteManagementDefaultList) {
    this.tlsCipherSuiteManagementDefaultList = tlsCipherSuiteManagementDefaultList;
    return this;
  }

   /**
   * The colon-separated list of default cipher suites for TLS management connections.
   * @return tlsCipherSuiteManagementDefaultList
  **/
  @ApiModelProperty(value = "The colon-separated list of default cipher suites for TLS management connections.")
  public String getTlsCipherSuiteManagementDefaultList() {
    return tlsCipherSuiteManagementDefaultList;
  }

  public void setTlsCipherSuiteManagementDefaultList(String tlsCipherSuiteManagementDefaultList) {
    this.tlsCipherSuiteManagementDefaultList = tlsCipherSuiteManagementDefaultList;
  }

  public Broker tlsCipherSuiteManagementList(String tlsCipherSuiteManagementList) {
    this.tlsCipherSuiteManagementList = tlsCipherSuiteManagementList;
    return this;
  }

   /**
   * The colon-separated list of cipher suites used for TLS management connections (e.g. SEMP, LDAP). The value \&quot;default\&quot; implies all supported suites ordered from most secure to least secure.
   * @return tlsCipherSuiteManagementList
  **/
  @ApiModelProperty(value = "The colon-separated list of cipher suites used for TLS management connections (e.g. SEMP, LDAP). The value \"default\" implies all supported suites ordered from most secure to least secure.")
  public String getTlsCipherSuiteManagementList() {
    return tlsCipherSuiteManagementList;
  }

  public void setTlsCipherSuiteManagementList(String tlsCipherSuiteManagementList) {
    this.tlsCipherSuiteManagementList = tlsCipherSuiteManagementList;
  }

  public Broker tlsCipherSuiteManagementSupportedList(String tlsCipherSuiteManagementSupportedList) {
    this.tlsCipherSuiteManagementSupportedList = tlsCipherSuiteManagementSupportedList;
    return this;
  }

   /**
   * The colon-separated list of supported cipher suites for TLS management connections.
   * @return tlsCipherSuiteManagementSupportedList
  **/
  @ApiModelProperty(value = "The colon-separated list of supported cipher suites for TLS management connections.")
  public String getTlsCipherSuiteManagementSupportedList() {
    return tlsCipherSuiteManagementSupportedList;
  }

  public void setTlsCipherSuiteManagementSupportedList(String tlsCipherSuiteManagementSupportedList) {
    this.tlsCipherSuiteManagementSupportedList = tlsCipherSuiteManagementSupportedList;
  }

  public Broker tlsCipherSuiteMsgBackboneDefaultList(String tlsCipherSuiteMsgBackboneDefaultList) {
    this.tlsCipherSuiteMsgBackboneDefaultList = tlsCipherSuiteMsgBackboneDefaultList;
    return this;
  }

   /**
   * The colon-separated list of default cipher suites for TLS data connections.
   * @return tlsCipherSuiteMsgBackboneDefaultList
  **/
  @ApiModelProperty(value = "The colon-separated list of default cipher suites for TLS data connections.")
  public String getTlsCipherSuiteMsgBackboneDefaultList() {
    return tlsCipherSuiteMsgBackboneDefaultList;
  }

  public void setTlsCipherSuiteMsgBackboneDefaultList(String tlsCipherSuiteMsgBackboneDefaultList) {
    this.tlsCipherSuiteMsgBackboneDefaultList = tlsCipherSuiteMsgBackboneDefaultList;
  }

  public Broker tlsCipherSuiteMsgBackboneList(String tlsCipherSuiteMsgBackboneList) {
    this.tlsCipherSuiteMsgBackboneList = tlsCipherSuiteMsgBackboneList;
    return this;
  }

   /**
   * The colon-separated list of cipher suites used for TLS data connections (e.g. client pub/sub). The value \&quot;default\&quot; implies all supported suites ordered from most secure to least secure.
   * @return tlsCipherSuiteMsgBackboneList
  **/
  @ApiModelProperty(value = "The colon-separated list of cipher suites used for TLS data connections (e.g. client pub/sub). The value \"default\" implies all supported suites ordered from most secure to least secure.")
  public String getTlsCipherSuiteMsgBackboneList() {
    return tlsCipherSuiteMsgBackboneList;
  }

  public void setTlsCipherSuiteMsgBackboneList(String tlsCipherSuiteMsgBackboneList) {
    this.tlsCipherSuiteMsgBackboneList = tlsCipherSuiteMsgBackboneList;
  }

  public Broker tlsCipherSuiteMsgBackboneSupportedList(String tlsCipherSuiteMsgBackboneSupportedList) {
    this.tlsCipherSuiteMsgBackboneSupportedList = tlsCipherSuiteMsgBackboneSupportedList;
    return this;
  }

   /**
   * The colon-separated list of supported cipher suites for TLS data connections.
   * @return tlsCipherSuiteMsgBackboneSupportedList
  **/
  @ApiModelProperty(value = "The colon-separated list of supported cipher suites for TLS data connections.")
  public String getTlsCipherSuiteMsgBackboneSupportedList() {
    return tlsCipherSuiteMsgBackboneSupportedList;
  }

  public void setTlsCipherSuiteMsgBackboneSupportedList(String tlsCipherSuiteMsgBackboneSupportedList) {
    this.tlsCipherSuiteMsgBackboneSupportedList = tlsCipherSuiteMsgBackboneSupportedList;
  }

  public Broker tlsCipherSuiteSecureShellDefaultList(String tlsCipherSuiteSecureShellDefaultList) {
    this.tlsCipherSuiteSecureShellDefaultList = tlsCipherSuiteSecureShellDefaultList;
    return this;
  }

   /**
   * The colon-separated list of default cipher suites for TLS secure shell connections.
   * @return tlsCipherSuiteSecureShellDefaultList
  **/
  @ApiModelProperty(value = "The colon-separated list of default cipher suites for TLS secure shell connections.")
  public String getTlsCipherSuiteSecureShellDefaultList() {
    return tlsCipherSuiteSecureShellDefaultList;
  }

  public void setTlsCipherSuiteSecureShellDefaultList(String tlsCipherSuiteSecureShellDefaultList) {
    this.tlsCipherSuiteSecureShellDefaultList = tlsCipherSuiteSecureShellDefaultList;
  }

  public Broker tlsCipherSuiteSecureShellList(String tlsCipherSuiteSecureShellList) {
    this.tlsCipherSuiteSecureShellList = tlsCipherSuiteSecureShellList;
    return this;
  }

   /**
   * The colon-separated list of cipher suites used for TLS secure shell connections (e.g. SSH, SFTP, SCP). The value \&quot;default\&quot; implies all supported suites ordered from most secure to least secure.
   * @return tlsCipherSuiteSecureShellList
  **/
  @ApiModelProperty(value = "The colon-separated list of cipher suites used for TLS secure shell connections (e.g. SSH, SFTP, SCP). The value \"default\" implies all supported suites ordered from most secure to least secure.")
  public String getTlsCipherSuiteSecureShellList() {
    return tlsCipherSuiteSecureShellList;
  }

  public void setTlsCipherSuiteSecureShellList(String tlsCipherSuiteSecureShellList) {
    this.tlsCipherSuiteSecureShellList = tlsCipherSuiteSecureShellList;
  }

  public Broker tlsCipherSuiteSecureShellSupportedList(String tlsCipherSuiteSecureShellSupportedList) {
    this.tlsCipherSuiteSecureShellSupportedList = tlsCipherSuiteSecureShellSupportedList;
    return this;
  }

   /**
   * The colon-separated list of supported cipher suites for TLS secure shell connections.
   * @return tlsCipherSuiteSecureShellSupportedList
  **/
  @ApiModelProperty(value = "The colon-separated list of supported cipher suites for TLS secure shell connections.")
  public String getTlsCipherSuiteSecureShellSupportedList() {
    return tlsCipherSuiteSecureShellSupportedList;
  }

  public void setTlsCipherSuiteSecureShellSupportedList(String tlsCipherSuiteSecureShellSupportedList) {
    this.tlsCipherSuiteSecureShellSupportedList = tlsCipherSuiteSecureShellSupportedList;
  }

  public Broker tlsCrimeExploitProtectionEnabled(Boolean tlsCrimeExploitProtectionEnabled) {
    this.tlsCrimeExploitProtectionEnabled = tlsCrimeExploitProtectionEnabled;
    return this;
  }

   /**
   * Indicates whether protection against the CRIME exploit is enabled. When enabled, TLS+compressed messaging performance is degraded. This protection should only be disabled if sufficient ACL and authentication features are being employed such that a potential attacker does not have sufficient access to trigger the exploit.
   * @return tlsCrimeExploitProtectionEnabled
  **/
  @ApiModelProperty(value = "Indicates whether protection against the CRIME exploit is enabled. When enabled, TLS+compressed messaging performance is degraded. This protection should only be disabled if sufficient ACL and authentication features are being employed such that a potential attacker does not have sufficient access to trigger the exploit.")
  public Boolean isTlsCrimeExploitProtectionEnabled() {
    return tlsCrimeExploitProtectionEnabled;
  }

  public void setTlsCrimeExploitProtectionEnabled(Boolean tlsCrimeExploitProtectionEnabled) {
    this.tlsCrimeExploitProtectionEnabled = tlsCrimeExploitProtectionEnabled;
  }

  public Broker tlsTicketLifetime(Integer tlsTicketLifetime) {
    this.tlsTicketLifetime = tlsTicketLifetime;
    return this;
  }

   /**
   * The TLS ticket lifetime in seconds. When a client connects with TLS, a session with a session ticket is created using the TLS ticket lifetime which determines how long the client has to resume the session.
   * @return tlsTicketLifetime
  **/
  @ApiModelProperty(value = "The TLS ticket lifetime in seconds. When a client connects with TLS, a session with a session ticket is created using the TLS ticket lifetime which determines how long the client has to resume the session.")
  public Integer getTlsTicketLifetime() {
    return tlsTicketLifetime;
  }

  public void setTlsTicketLifetime(Integer tlsTicketLifetime) {
    this.tlsTicketLifetime = tlsTicketLifetime;
  }

  public Broker tlsVersionSupportedList(String tlsVersionSupportedList) {
    this.tlsVersionSupportedList = tlsVersionSupportedList;
    return this;
  }

   /**
   * The comma-separated list of supported TLS versions.
   * @return tlsVersionSupportedList
  **/
  @ApiModelProperty(value = "The comma-separated list of supported TLS versions.")
  public String getTlsVersionSupportedList() {
    return tlsVersionSupportedList;
  }

  public void setTlsVersionSupportedList(String tlsVersionSupportedList) {
    this.tlsVersionSupportedList = tlsVersionSupportedList;
  }

  public Broker txByteCount(Long txByteCount) {
    this.txByteCount = txByteCount;
    return this;
  }

   /**
   * The amount of messages transmitted to clients by the Broker, in bytes (B). Available since 2.14.
   * @return txByteCount
  **/
  @ApiModelProperty(value = "The amount of messages transmitted to clients by the Broker, in bytes (B). Available since 2.14.")
  public Long getTxByteCount() {
    return txByteCount;
  }

  public void setTxByteCount(Long txByteCount) {
    this.txByteCount = txByteCount;
  }

  public Broker txByteRate(Long txByteRate) {
    this.txByteRate = txByteRate;
    return this;
  }

   /**
   * The current message rate transmitted by the Broker, in bytes per second (B/sec). Available since 2.14.
   * @return txByteRate
  **/
  @ApiModelProperty(value = "The current message rate transmitted by the Broker, in bytes per second (B/sec). Available since 2.14.")
  public Long getTxByteRate() {
    return txByteRate;
  }

  public void setTxByteRate(Long txByteRate) {
    this.txByteRate = txByteRate;
  }

  public Broker txCompressedByteCount(Long txCompressedByteCount) {
    this.txCompressedByteCount = txCompressedByteCount;
    return this;
  }

   /**
   * The amount of compressed messages transmitted by the Broker, in bytes (B). Available since 2.14.
   * @return txCompressedByteCount
  **/
  @ApiModelProperty(value = "The amount of compressed messages transmitted by the Broker, in bytes (B). Available since 2.14.")
  public Long getTxCompressedByteCount() {
    return txCompressedByteCount;
  }

  public void setTxCompressedByteCount(Long txCompressedByteCount) {
    this.txCompressedByteCount = txCompressedByteCount;
  }

  public Broker txCompressedByteRate(Long txCompressedByteRate) {
    this.txCompressedByteRate = txCompressedByteRate;
    return this;
  }

   /**
   * The current compressed message rate transmitted by the Broker, in bytes per second (B/sec). Available since 2.14.
   * @return txCompressedByteRate
  **/
  @ApiModelProperty(value = "The current compressed message rate transmitted by the Broker, in bytes per second (B/sec). Available since 2.14.")
  public Long getTxCompressedByteRate() {
    return txCompressedByteRate;
  }

  public void setTxCompressedByteRate(Long txCompressedByteRate) {
    this.txCompressedByteRate = txCompressedByteRate;
  }

  public Broker txCompressionRatio(String txCompressionRatio) {
    this.txCompressionRatio = txCompressionRatio;
    return this;
  }

   /**
   * The compression ratio for messages transmitted by the Broker. Available since 2.14.
   * @return txCompressionRatio
  **/
  @ApiModelProperty(value = "The compression ratio for messages transmitted by the Broker. Available since 2.14.")
  public String getTxCompressionRatio() {
    return txCompressionRatio;
  }

  public void setTxCompressionRatio(String txCompressionRatio) {
    this.txCompressionRatio = txCompressionRatio;
  }

  public Broker txMsgCount(Long txMsgCount) {
    this.txMsgCount = txMsgCount;
    return this;
  }

   /**
   * The number of messages transmitted to clients by the Broker. Available since 2.14.
   * @return txMsgCount
  **/
  @ApiModelProperty(value = "The number of messages transmitted to clients by the Broker. Available since 2.14.")
  public Long getTxMsgCount() {
    return txMsgCount;
  }

  public void setTxMsgCount(Long txMsgCount) {
    this.txMsgCount = txMsgCount;
  }

  public Broker txMsgRate(Long txMsgRate) {
    this.txMsgRate = txMsgRate;
    return this;
  }

   /**
   * The current message rate transmitted by the Broker, in messages per second (msg/sec). Available since 2.14.
   * @return txMsgRate
  **/
  @ApiModelProperty(value = "The current message rate transmitted by the Broker, in messages per second (msg/sec). Available since 2.14.")
  public Long getTxMsgRate() {
    return txMsgRate;
  }

  public void setTxMsgRate(Long txMsgRate) {
    this.txMsgRate = txMsgRate;
  }

  public Broker txUncompressedByteCount(Long txUncompressedByteCount) {
    this.txUncompressedByteCount = txUncompressedByteCount;
    return this;
  }

   /**
   * The amount of uncompressed messages transmitted by the Broker, in bytes (B). Available since 2.14.
   * @return txUncompressedByteCount
  **/
  @ApiModelProperty(value = "The amount of uncompressed messages transmitted by the Broker, in bytes (B). Available since 2.14.")
  public Long getTxUncompressedByteCount() {
    return txUncompressedByteCount;
  }

  public void setTxUncompressedByteCount(Long txUncompressedByteCount) {
    this.txUncompressedByteCount = txUncompressedByteCount;
  }

  public Broker txUncompressedByteRate(Long txUncompressedByteRate) {
    this.txUncompressedByteRate = txUncompressedByteRate;
    return this;
  }

   /**
   * The current uncompressed message rate transmitted by the Broker, in bytes per second (B/sec). Available since 2.14.
   * @return txUncompressedByteRate
  **/
  @ApiModelProperty(value = "The current uncompressed message rate transmitted by the Broker, in bytes per second (B/sec). Available since 2.14.")
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
    Broker broker = (Broker) o;
    return Objects.equals(this.authClientCertRevocationCheckMode, broker.authClientCertRevocationCheckMode) &&
        Objects.equals(this.averageRxByteRate, broker.averageRxByteRate) &&
        Objects.equals(this.averageRxCompressedByteRate, broker.averageRxCompressedByteRate) &&
        Objects.equals(this.averageRxMsgRate, broker.averageRxMsgRate) &&
        Objects.equals(this.averageRxUncompressedByteRate, broker.averageRxUncompressedByteRate) &&
        Objects.equals(this.averageTxByteRate, broker.averageTxByteRate) &&
        Objects.equals(this.averageTxCompressedByteRate, broker.averageTxCompressedByteRate) &&
        Objects.equals(this.averageTxMsgRate, broker.averageTxMsgRate) &&
        Objects.equals(this.averageTxUncompressedByteRate, broker.averageTxUncompressedByteRate) &&
        Objects.equals(this.rxByteCount, broker.rxByteCount) &&
        Objects.equals(this.rxByteRate, broker.rxByteRate) &&
        Objects.equals(this.rxCompressedByteCount, broker.rxCompressedByteCount) &&
        Objects.equals(this.rxCompressedByteRate, broker.rxCompressedByteRate) &&
        Objects.equals(this.rxCompressionRatio, broker.rxCompressionRatio) &&
        Objects.equals(this.rxMsgCount, broker.rxMsgCount) &&
        Objects.equals(this.rxMsgRate, broker.rxMsgRate) &&
        Objects.equals(this.rxUncompressedByteCount, broker.rxUncompressedByteCount) &&
        Objects.equals(this.rxUncompressedByteRate, broker.rxUncompressedByteRate) &&
        Objects.equals(this.tlsBlockVersion10Enabled, broker.tlsBlockVersion10Enabled) &&
        Objects.equals(this.tlsBlockVersion11Enabled, broker.tlsBlockVersion11Enabled) &&
        Objects.equals(this.tlsCipherSuiteManagementDefaultList, broker.tlsCipherSuiteManagementDefaultList) &&
        Objects.equals(this.tlsCipherSuiteManagementList, broker.tlsCipherSuiteManagementList) &&
        Objects.equals(this.tlsCipherSuiteManagementSupportedList, broker.tlsCipherSuiteManagementSupportedList) &&
        Objects.equals(this.tlsCipherSuiteMsgBackboneDefaultList, broker.tlsCipherSuiteMsgBackboneDefaultList) &&
        Objects.equals(this.tlsCipherSuiteMsgBackboneList, broker.tlsCipherSuiteMsgBackboneList) &&
        Objects.equals(this.tlsCipherSuiteMsgBackboneSupportedList, broker.tlsCipherSuiteMsgBackboneSupportedList) &&
        Objects.equals(this.tlsCipherSuiteSecureShellDefaultList, broker.tlsCipherSuiteSecureShellDefaultList) &&
        Objects.equals(this.tlsCipherSuiteSecureShellList, broker.tlsCipherSuiteSecureShellList) &&
        Objects.equals(this.tlsCipherSuiteSecureShellSupportedList, broker.tlsCipherSuiteSecureShellSupportedList) &&
        Objects.equals(this.tlsCrimeExploitProtectionEnabled, broker.tlsCrimeExploitProtectionEnabled) &&
        Objects.equals(this.tlsTicketLifetime, broker.tlsTicketLifetime) &&
        Objects.equals(this.tlsVersionSupportedList, broker.tlsVersionSupportedList) &&
        Objects.equals(this.txByteCount, broker.txByteCount) &&
        Objects.equals(this.txByteRate, broker.txByteRate) &&
        Objects.equals(this.txCompressedByteCount, broker.txCompressedByteCount) &&
        Objects.equals(this.txCompressedByteRate, broker.txCompressedByteRate) &&
        Objects.equals(this.txCompressionRatio, broker.txCompressionRatio) &&
        Objects.equals(this.txMsgCount, broker.txMsgCount) &&
        Objects.equals(this.txMsgRate, broker.txMsgRate) &&
        Objects.equals(this.txUncompressedByteCount, broker.txUncompressedByteCount) &&
        Objects.equals(this.txUncompressedByteRate, broker.txUncompressedByteRate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authClientCertRevocationCheckMode, averageRxByteRate, averageRxCompressedByteRate, averageRxMsgRate, averageRxUncompressedByteRate, averageTxByteRate, averageTxCompressedByteRate, averageTxMsgRate, averageTxUncompressedByteRate, rxByteCount, rxByteRate, rxCompressedByteCount, rxCompressedByteRate, rxCompressionRatio, rxMsgCount, rxMsgRate, rxUncompressedByteCount, rxUncompressedByteRate, tlsBlockVersion10Enabled, tlsBlockVersion11Enabled, tlsCipherSuiteManagementDefaultList, tlsCipherSuiteManagementList, tlsCipherSuiteManagementSupportedList, tlsCipherSuiteMsgBackboneDefaultList, tlsCipherSuiteMsgBackboneList, tlsCipherSuiteMsgBackboneSupportedList, tlsCipherSuiteSecureShellDefaultList, tlsCipherSuiteSecureShellList, tlsCipherSuiteSecureShellSupportedList, tlsCrimeExploitProtectionEnabled, tlsTicketLifetime, tlsVersionSupportedList, txByteCount, txByteRate, txCompressedByteCount, txCompressedByteRate, txCompressionRatio, txMsgCount, txMsgRate, txUncompressedByteCount, txUncompressedByteRate);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Broker {\n");
    
    sb.append("    authClientCertRevocationCheckMode: ").append(toIndentedString(authClientCertRevocationCheckMode)).append("\n");
    sb.append("    averageRxByteRate: ").append(toIndentedString(averageRxByteRate)).append("\n");
    sb.append("    averageRxCompressedByteRate: ").append(toIndentedString(averageRxCompressedByteRate)).append("\n");
    sb.append("    averageRxMsgRate: ").append(toIndentedString(averageRxMsgRate)).append("\n");
    sb.append("    averageRxUncompressedByteRate: ").append(toIndentedString(averageRxUncompressedByteRate)).append("\n");
    sb.append("    averageTxByteRate: ").append(toIndentedString(averageTxByteRate)).append("\n");
    sb.append("    averageTxCompressedByteRate: ").append(toIndentedString(averageTxCompressedByteRate)).append("\n");
    sb.append("    averageTxMsgRate: ").append(toIndentedString(averageTxMsgRate)).append("\n");
    sb.append("    averageTxUncompressedByteRate: ").append(toIndentedString(averageTxUncompressedByteRate)).append("\n");
    sb.append("    rxByteCount: ").append(toIndentedString(rxByteCount)).append("\n");
    sb.append("    rxByteRate: ").append(toIndentedString(rxByteRate)).append("\n");
    sb.append("    rxCompressedByteCount: ").append(toIndentedString(rxCompressedByteCount)).append("\n");
    sb.append("    rxCompressedByteRate: ").append(toIndentedString(rxCompressedByteRate)).append("\n");
    sb.append("    rxCompressionRatio: ").append(toIndentedString(rxCompressionRatio)).append("\n");
    sb.append("    rxMsgCount: ").append(toIndentedString(rxMsgCount)).append("\n");
    sb.append("    rxMsgRate: ").append(toIndentedString(rxMsgRate)).append("\n");
    sb.append("    rxUncompressedByteCount: ").append(toIndentedString(rxUncompressedByteCount)).append("\n");
    sb.append("    rxUncompressedByteRate: ").append(toIndentedString(rxUncompressedByteRate)).append("\n");
    sb.append("    tlsBlockVersion10Enabled: ").append(toIndentedString(tlsBlockVersion10Enabled)).append("\n");
    sb.append("    tlsBlockVersion11Enabled: ").append(toIndentedString(tlsBlockVersion11Enabled)).append("\n");
    sb.append("    tlsCipherSuiteManagementDefaultList: ").append(toIndentedString(tlsCipherSuiteManagementDefaultList)).append("\n");
    sb.append("    tlsCipherSuiteManagementList: ").append(toIndentedString(tlsCipherSuiteManagementList)).append("\n");
    sb.append("    tlsCipherSuiteManagementSupportedList: ").append(toIndentedString(tlsCipherSuiteManagementSupportedList)).append("\n");
    sb.append("    tlsCipherSuiteMsgBackboneDefaultList: ").append(toIndentedString(tlsCipherSuiteMsgBackboneDefaultList)).append("\n");
    sb.append("    tlsCipherSuiteMsgBackboneList: ").append(toIndentedString(tlsCipherSuiteMsgBackboneList)).append("\n");
    sb.append("    tlsCipherSuiteMsgBackboneSupportedList: ").append(toIndentedString(tlsCipherSuiteMsgBackboneSupportedList)).append("\n");
    sb.append("    tlsCipherSuiteSecureShellDefaultList: ").append(toIndentedString(tlsCipherSuiteSecureShellDefaultList)).append("\n");
    sb.append("    tlsCipherSuiteSecureShellList: ").append(toIndentedString(tlsCipherSuiteSecureShellList)).append("\n");
    sb.append("    tlsCipherSuiteSecureShellSupportedList: ").append(toIndentedString(tlsCipherSuiteSecureShellSupportedList)).append("\n");
    sb.append("    tlsCrimeExploitProtectionEnabled: ").append(toIndentedString(tlsCrimeExploitProtectionEnabled)).append("\n");
    sb.append("    tlsTicketLifetime: ").append(toIndentedString(tlsTicketLifetime)).append("\n");
    sb.append("    tlsVersionSupportedList: ").append(toIndentedString(tlsVersionSupportedList)).append("\n");
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

