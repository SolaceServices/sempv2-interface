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
import com.solace.psg.sempv2.monitor.model.MsgVpnRestDeliveryPointRestConsumerCounter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * MsgVpnRestDeliveryPointRestConsumer
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:57:21.016551900+01:00[Europe/London]")
public class MsgVpnRestDeliveryPointRestConsumer {
  @SerializedName("authenticationHttpBasicUsername")
  private String authenticationHttpBasicUsername = null;

  @SerializedName("authenticationHttpHeaderName")
  private String authenticationHttpHeaderName = null;

  /**
   * The authentication scheme used by the REST Consumer to login to the REST host. The allowed values and their meaning are:  &lt;pre&gt; \&quot;none\&quot; - Login with no authentication. This may be useful for anonymous connections or when a REST Consumer does not require authentication. \&quot;http-basic\&quot; - Login with a username and optional password according to HTTP Basic authentication as per RFC2616. \&quot;client-certificate\&quot; - Login with a client TLS certificate as per RFC5246. Client certificate authentication is only available on TLS connections. \&quot;http-header\&quot; - Login with a specified HTTP header. &lt;/pre&gt; 
   */
  @JsonAdapter(AuthenticationSchemeEnum.Adapter.class)
  public enum AuthenticationSchemeEnum {
    NONE("none"),
    HTTP_BASIC("http-basic"),
    CLIENT_CERTIFICATE("client-certificate"),
    HTTP_HEADER("http-header");

    private String value;

    AuthenticationSchemeEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static AuthenticationSchemeEnum fromValue(String text) {
      for (AuthenticationSchemeEnum b : AuthenticationSchemeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<AuthenticationSchemeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final AuthenticationSchemeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public AuthenticationSchemeEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return AuthenticationSchemeEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("authenticationScheme")
  private AuthenticationSchemeEnum authenticationScheme = null;

  @SerializedName("counter")
  private MsgVpnRestDeliveryPointRestConsumerCounter counter = null;

  @SerializedName("enabled")
  private Boolean enabled = null;

  /**
   * The HTTP method to use (POST or PUT). This is used only when operating in the REST service \&quot;messaging\&quot; mode and is ignored in \&quot;gateway\&quot; mode. The allowed values and their meaning are:  &lt;pre&gt; \&quot;post\&quot; - Use the POST HTTP method. \&quot;put\&quot; - Use the PUT HTTP method. &lt;/pre&gt;  Available since 2.17.
   */
  @JsonAdapter(HttpMethodEnum.Adapter.class)
  public enum HttpMethodEnum {
    POST("post"),
    PUT("put");

    private String value;

    HttpMethodEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static HttpMethodEnum fromValue(String text) {
      for (HttpMethodEnum b : HttpMethodEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<HttpMethodEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final HttpMethodEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public HttpMethodEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return HttpMethodEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("httpMethod")
  private HttpMethodEnum httpMethod = null;

  @SerializedName("httpRequestConnectionCloseTxMsgCount")
  private Long httpRequestConnectionCloseTxMsgCount = null;

  @SerializedName("httpRequestOutstandingTxMsgCount")
  private Long httpRequestOutstandingTxMsgCount = null;

  @SerializedName("httpRequestTimedOutTxMsgCount")
  private Long httpRequestTimedOutTxMsgCount = null;

  @SerializedName("httpRequestTxByteCount")
  private Long httpRequestTxByteCount = null;

  @SerializedName("httpRequestTxMsgCount")
  private Integer httpRequestTxMsgCount = null;

  @SerializedName("httpResponseErrorRxMsgCount")
  private Long httpResponseErrorRxMsgCount = null;

  @SerializedName("httpResponseRxByteCount")
  private Long httpResponseRxByteCount = null;

  @SerializedName("httpResponseRxMsgCount")
  private Long httpResponseRxMsgCount = null;

  @SerializedName("httpResponseSuccessRxMsgCount")
  private Long httpResponseSuccessRxMsgCount = null;

  @SerializedName("lastConnectionFailureLocalEndpoint")
  private String lastConnectionFailureLocalEndpoint = null;

  @SerializedName("lastConnectionFailureReason")
  private String lastConnectionFailureReason = null;

  @SerializedName("lastConnectionFailureRemoteEndpoint")
  private String lastConnectionFailureRemoteEndpoint = null;

  @SerializedName("lastConnectionFailureTime")
  private Integer lastConnectionFailureTime = null;

  @SerializedName("lastFailureReason")
  private String lastFailureReason = null;

  @SerializedName("lastFailureTime")
  private Integer lastFailureTime = null;

  @SerializedName("localInterface")
  private String localInterface = null;

  @SerializedName("maxPostWaitTime")
  private Integer maxPostWaitTime = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("outgoingConnectionCount")
  private Integer outgoingConnectionCount = null;

  @SerializedName("remoteHost")
  private String remoteHost = null;

  @SerializedName("remoteOutgoingConnectionUpCount")
  private Long remoteOutgoingConnectionUpCount = null;

  @SerializedName("remotePort")
  private Long remotePort = null;

  @SerializedName("restConsumerName")
  private String restConsumerName = null;

  @SerializedName("restDeliveryPointName")
  private String restDeliveryPointName = null;

  @SerializedName("retryDelay")
  private Integer retryDelay = null;

  @SerializedName("tlsCipherSuiteList")
  private String tlsCipherSuiteList = null;

  @SerializedName("tlsEnabled")
  private Boolean tlsEnabled = null;

  @SerializedName("up")
  private Boolean up = null;

  public MsgVpnRestDeliveryPointRestConsumer authenticationHttpBasicUsername(String authenticationHttpBasicUsername) {
    this.authenticationHttpBasicUsername = authenticationHttpBasicUsername;
    return this;
  }

   /**
   * The username that the REST Consumer will use to login to the REST host.
   * @return authenticationHttpBasicUsername
  **/
  @Schema(description = "The username that the REST Consumer will use to login to the REST host.")
  public String getAuthenticationHttpBasicUsername() {
    return authenticationHttpBasicUsername;
  }

  public void setAuthenticationHttpBasicUsername(String authenticationHttpBasicUsername) {
    this.authenticationHttpBasicUsername = authenticationHttpBasicUsername;
  }

  public MsgVpnRestDeliveryPointRestConsumer authenticationHttpHeaderName(String authenticationHttpHeaderName) {
    this.authenticationHttpHeaderName = authenticationHttpHeaderName;
    return this;
  }

   /**
   * The authentication header name. Available since 2.15.
   * @return authenticationHttpHeaderName
  **/
  @Schema(description = "The authentication header name. Available since 2.15.")
  public String getAuthenticationHttpHeaderName() {
    return authenticationHttpHeaderName;
  }

  public void setAuthenticationHttpHeaderName(String authenticationHttpHeaderName) {
    this.authenticationHttpHeaderName = authenticationHttpHeaderName;
  }

  public MsgVpnRestDeliveryPointRestConsumer authenticationScheme(AuthenticationSchemeEnum authenticationScheme) {
    this.authenticationScheme = authenticationScheme;
    return this;
  }

   /**
   * The authentication scheme used by the REST Consumer to login to the REST host. The allowed values and their meaning are:  &lt;pre&gt; \&quot;none\&quot; - Login with no authentication. This may be useful for anonymous connections or when a REST Consumer does not require authentication. \&quot;http-basic\&quot; - Login with a username and optional password according to HTTP Basic authentication as per RFC2616. \&quot;client-certificate\&quot; - Login with a client TLS certificate as per RFC5246. Client certificate authentication is only available on TLS connections. \&quot;http-header\&quot; - Login with a specified HTTP header. &lt;/pre&gt; 
   * @return authenticationScheme
  **/
  @Schema(description = "The authentication scheme used by the REST Consumer to login to the REST host. The allowed values and their meaning are:  <pre> \"none\" - Login with no authentication. This may be useful for anonymous connections or when a REST Consumer does not require authentication. \"http-basic\" - Login with a username and optional password according to HTTP Basic authentication as per RFC2616. \"client-certificate\" - Login with a client TLS certificate as per RFC5246. Client certificate authentication is only available on TLS connections. \"http-header\" - Login with a specified HTTP header. </pre> ")
  public AuthenticationSchemeEnum getAuthenticationScheme() {
    return authenticationScheme;
  }

  public void setAuthenticationScheme(AuthenticationSchemeEnum authenticationScheme) {
    this.authenticationScheme = authenticationScheme;
  }

  public MsgVpnRestDeliveryPointRestConsumer counter(MsgVpnRestDeliveryPointRestConsumerCounter counter) {
    this.counter = counter;
    return this;
  }

   /**
   * Get counter
   * @return counter
  **/
  @Schema(description = "")
  public MsgVpnRestDeliveryPointRestConsumerCounter getCounter() {
    return counter;
  }

  public void setCounter(MsgVpnRestDeliveryPointRestConsumerCounter counter) {
    this.counter = counter;
  }

  public MsgVpnRestDeliveryPointRestConsumer enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

   /**
   * Indicates whether the REST Consumer is enabled.
   * @return enabled
  **/
  @Schema(description = "Indicates whether the REST Consumer is enabled.")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public MsgVpnRestDeliveryPointRestConsumer httpMethod(HttpMethodEnum httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

   /**
   * The HTTP method to use (POST or PUT). This is used only when operating in the REST service \&quot;messaging\&quot; mode and is ignored in \&quot;gateway\&quot; mode. The allowed values and their meaning are:  &lt;pre&gt; \&quot;post\&quot; - Use the POST HTTP method. \&quot;put\&quot; - Use the PUT HTTP method. &lt;/pre&gt;  Available since 2.17.
   * @return httpMethod
  **/
  @Schema(description = "The HTTP method to use (POST or PUT). This is used only when operating in the REST service \"messaging\" mode and is ignored in \"gateway\" mode. The allowed values and their meaning are:  <pre> \"post\" - Use the POST HTTP method. \"put\" - Use the PUT HTTP method. </pre>  Available since 2.17.")
  public HttpMethodEnum getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(HttpMethodEnum httpMethod) {
    this.httpMethod = httpMethod;
  }

  public MsgVpnRestDeliveryPointRestConsumer httpRequestConnectionCloseTxMsgCount(Long httpRequestConnectionCloseTxMsgCount) {
    this.httpRequestConnectionCloseTxMsgCount = httpRequestConnectionCloseTxMsgCount;
    return this;
  }

   /**
   * The number of HTTP request messages transmitted to the REST Consumer to close the connection. Available since 2.13.
   * @return httpRequestConnectionCloseTxMsgCount
  **/
  @Schema(description = "The number of HTTP request messages transmitted to the REST Consumer to close the connection. Available since 2.13.")
  public Long getHttpRequestConnectionCloseTxMsgCount() {
    return httpRequestConnectionCloseTxMsgCount;
  }

  public void setHttpRequestConnectionCloseTxMsgCount(Long httpRequestConnectionCloseTxMsgCount) {
    this.httpRequestConnectionCloseTxMsgCount = httpRequestConnectionCloseTxMsgCount;
  }

  public MsgVpnRestDeliveryPointRestConsumer httpRequestOutstandingTxMsgCount(Long httpRequestOutstandingTxMsgCount) {
    this.httpRequestOutstandingTxMsgCount = httpRequestOutstandingTxMsgCount;
    return this;
  }

   /**
   * The number of HTTP request messages transmitted to the REST Consumer that are waiting for a response. Available since 2.13.
   * @return httpRequestOutstandingTxMsgCount
  **/
  @Schema(description = "The number of HTTP request messages transmitted to the REST Consumer that are waiting for a response. Available since 2.13.")
  public Long getHttpRequestOutstandingTxMsgCount() {
    return httpRequestOutstandingTxMsgCount;
  }

  public void setHttpRequestOutstandingTxMsgCount(Long httpRequestOutstandingTxMsgCount) {
    this.httpRequestOutstandingTxMsgCount = httpRequestOutstandingTxMsgCount;
  }

  public MsgVpnRestDeliveryPointRestConsumer httpRequestTimedOutTxMsgCount(Long httpRequestTimedOutTxMsgCount) {
    this.httpRequestTimedOutTxMsgCount = httpRequestTimedOutTxMsgCount;
    return this;
  }

   /**
   * The number of HTTP request messages transmitted to the REST Consumer that have timed out. Available since 2.13.
   * @return httpRequestTimedOutTxMsgCount
  **/
  @Schema(description = "The number of HTTP request messages transmitted to the REST Consumer that have timed out. Available since 2.13.")
  public Long getHttpRequestTimedOutTxMsgCount() {
    return httpRequestTimedOutTxMsgCount;
  }

  public void setHttpRequestTimedOutTxMsgCount(Long httpRequestTimedOutTxMsgCount) {
    this.httpRequestTimedOutTxMsgCount = httpRequestTimedOutTxMsgCount;
  }

  public MsgVpnRestDeliveryPointRestConsumer httpRequestTxByteCount(Long httpRequestTxByteCount) {
    this.httpRequestTxByteCount = httpRequestTxByteCount;
    return this;
  }

   /**
   * The amount of HTTP request messages transmitted to the REST Consumer, in bytes (B). Available since 2.13.
   * @return httpRequestTxByteCount
  **/
  @Schema(description = "The amount of HTTP request messages transmitted to the REST Consumer, in bytes (B). Available since 2.13.")
  public Long getHttpRequestTxByteCount() {
    return httpRequestTxByteCount;
  }

  public void setHttpRequestTxByteCount(Long httpRequestTxByteCount) {
    this.httpRequestTxByteCount = httpRequestTxByteCount;
  }

  public MsgVpnRestDeliveryPointRestConsumer httpRequestTxMsgCount(Integer httpRequestTxMsgCount) {
    this.httpRequestTxMsgCount = httpRequestTxMsgCount;
    return this;
  }

   /**
   * The number of HTTP request messages transmitted to the REST Consumer. Available since 2.13.
   * @return httpRequestTxMsgCount
  **/
  @Schema(description = "The number of HTTP request messages transmitted to the REST Consumer. Available since 2.13.")
  public Integer getHttpRequestTxMsgCount() {
    return httpRequestTxMsgCount;
  }

  public void setHttpRequestTxMsgCount(Integer httpRequestTxMsgCount) {
    this.httpRequestTxMsgCount = httpRequestTxMsgCount;
  }

  public MsgVpnRestDeliveryPointRestConsumer httpResponseErrorRxMsgCount(Long httpResponseErrorRxMsgCount) {
    this.httpResponseErrorRxMsgCount = httpResponseErrorRxMsgCount;
    return this;
  }

   /**
   * The number of HTTP client/server error response messages received from the REST Consumer. Available since 2.13.
   * @return httpResponseErrorRxMsgCount
  **/
  @Schema(description = "The number of HTTP client/server error response messages received from the REST Consumer. Available since 2.13.")
  public Long getHttpResponseErrorRxMsgCount() {
    return httpResponseErrorRxMsgCount;
  }

  public void setHttpResponseErrorRxMsgCount(Long httpResponseErrorRxMsgCount) {
    this.httpResponseErrorRxMsgCount = httpResponseErrorRxMsgCount;
  }

  public MsgVpnRestDeliveryPointRestConsumer httpResponseRxByteCount(Long httpResponseRxByteCount) {
    this.httpResponseRxByteCount = httpResponseRxByteCount;
    return this;
  }

   /**
   * The amount of HTTP response messages received from the REST Consumer, in bytes (B). Available since 2.13.
   * @return httpResponseRxByteCount
  **/
  @Schema(description = "The amount of HTTP response messages received from the REST Consumer, in bytes (B). Available since 2.13.")
  public Long getHttpResponseRxByteCount() {
    return httpResponseRxByteCount;
  }

  public void setHttpResponseRxByteCount(Long httpResponseRxByteCount) {
    this.httpResponseRxByteCount = httpResponseRxByteCount;
  }

  public MsgVpnRestDeliveryPointRestConsumer httpResponseRxMsgCount(Long httpResponseRxMsgCount) {
    this.httpResponseRxMsgCount = httpResponseRxMsgCount;
    return this;
  }

   /**
   * The number of HTTP response messages received from the REST Consumer. Available since 2.13.
   * @return httpResponseRxMsgCount
  **/
  @Schema(description = "The number of HTTP response messages received from the REST Consumer. Available since 2.13.")
  public Long getHttpResponseRxMsgCount() {
    return httpResponseRxMsgCount;
  }

  public void setHttpResponseRxMsgCount(Long httpResponseRxMsgCount) {
    this.httpResponseRxMsgCount = httpResponseRxMsgCount;
  }

  public MsgVpnRestDeliveryPointRestConsumer httpResponseSuccessRxMsgCount(Long httpResponseSuccessRxMsgCount) {
    this.httpResponseSuccessRxMsgCount = httpResponseSuccessRxMsgCount;
    return this;
  }

   /**
   * The number of HTTP successful response messages received from the REST Consumer. Available since 2.13.
   * @return httpResponseSuccessRxMsgCount
  **/
  @Schema(description = "The number of HTTP successful response messages received from the REST Consumer. Available since 2.13.")
  public Long getHttpResponseSuccessRxMsgCount() {
    return httpResponseSuccessRxMsgCount;
  }

  public void setHttpResponseSuccessRxMsgCount(Long httpResponseSuccessRxMsgCount) {
    this.httpResponseSuccessRxMsgCount = httpResponseSuccessRxMsgCount;
  }

  public MsgVpnRestDeliveryPointRestConsumer lastConnectionFailureLocalEndpoint(String lastConnectionFailureLocalEndpoint) {
    this.lastConnectionFailureLocalEndpoint = lastConnectionFailureLocalEndpoint;
    return this;
  }

   /**
   * The local endpoint at the time of the last connection failure.
   * @return lastConnectionFailureLocalEndpoint
  **/
  @Schema(description = "The local endpoint at the time of the last connection failure.")
  public String getLastConnectionFailureLocalEndpoint() {
    return lastConnectionFailureLocalEndpoint;
  }

  public void setLastConnectionFailureLocalEndpoint(String lastConnectionFailureLocalEndpoint) {
    this.lastConnectionFailureLocalEndpoint = lastConnectionFailureLocalEndpoint;
  }

  public MsgVpnRestDeliveryPointRestConsumer lastConnectionFailureReason(String lastConnectionFailureReason) {
    this.lastConnectionFailureReason = lastConnectionFailureReason;
    return this;
  }

   /**
   * The reason for the last connection failure between local and remote endpoints.
   * @return lastConnectionFailureReason
  **/
  @Schema(description = "The reason for the last connection failure between local and remote endpoints.")
  public String getLastConnectionFailureReason() {
    return lastConnectionFailureReason;
  }

  public void setLastConnectionFailureReason(String lastConnectionFailureReason) {
    this.lastConnectionFailureReason = lastConnectionFailureReason;
  }

  public MsgVpnRestDeliveryPointRestConsumer lastConnectionFailureRemoteEndpoint(String lastConnectionFailureRemoteEndpoint) {
    this.lastConnectionFailureRemoteEndpoint = lastConnectionFailureRemoteEndpoint;
    return this;
  }

   /**
   * The remote endpoint at the time of the last connection failure.
   * @return lastConnectionFailureRemoteEndpoint
  **/
  @Schema(description = "The remote endpoint at the time of the last connection failure.")
  public String getLastConnectionFailureRemoteEndpoint() {
    return lastConnectionFailureRemoteEndpoint;
  }

  public void setLastConnectionFailureRemoteEndpoint(String lastConnectionFailureRemoteEndpoint) {
    this.lastConnectionFailureRemoteEndpoint = lastConnectionFailureRemoteEndpoint;
  }

  public MsgVpnRestDeliveryPointRestConsumer lastConnectionFailureTime(Integer lastConnectionFailureTime) {
    this.lastConnectionFailureTime = lastConnectionFailureTime;
    return this;
  }

   /**
   * The timestamp of the last connection failure between local and remote endpoints. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return lastConnectionFailureTime
  **/
  @Schema(description = "The timestamp of the last connection failure between local and remote endpoints. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getLastConnectionFailureTime() {
    return lastConnectionFailureTime;
  }

  public void setLastConnectionFailureTime(Integer lastConnectionFailureTime) {
    this.lastConnectionFailureTime = lastConnectionFailureTime;
  }

  public MsgVpnRestDeliveryPointRestConsumer lastFailureReason(String lastFailureReason) {
    this.lastFailureReason = lastFailureReason;
    return this;
  }

   /**
   * The reason for the last REST Consumer failure.
   * @return lastFailureReason
  **/
  @Schema(description = "The reason for the last REST Consumer failure.")
  public String getLastFailureReason() {
    return lastFailureReason;
  }

  public void setLastFailureReason(String lastFailureReason) {
    this.lastFailureReason = lastFailureReason;
  }

  public MsgVpnRestDeliveryPointRestConsumer lastFailureTime(Integer lastFailureTime) {
    this.lastFailureTime = lastFailureTime;
    return this;
  }

   /**
   * The timestamp of the last REST Consumer failure. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return lastFailureTime
  **/
  @Schema(description = "The timestamp of the last REST Consumer failure. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getLastFailureTime() {
    return lastFailureTime;
  }

  public void setLastFailureTime(Integer lastFailureTime) {
    this.lastFailureTime = lastFailureTime;
  }

  public MsgVpnRestDeliveryPointRestConsumer localInterface(String localInterface) {
    this.localInterface = localInterface;
    return this;
  }

   /**
   * The interface that will be used for all outgoing connections associated with the REST Consumer. When unspecified, an interface is automatically chosen.
   * @return localInterface
  **/
  @Schema(description = "The interface that will be used for all outgoing connections associated with the REST Consumer. When unspecified, an interface is automatically chosen.")
  public String getLocalInterface() {
    return localInterface;
  }

  public void setLocalInterface(String localInterface) {
    this.localInterface = localInterface;
  }

  public MsgVpnRestDeliveryPointRestConsumer maxPostWaitTime(Integer maxPostWaitTime) {
    this.maxPostWaitTime = maxPostWaitTime;
    return this;
  }

   /**
   * The maximum amount of time (in seconds) to wait for an HTTP POST response from the REST Consumer. Once this time is exceeded, the TCP connection is reset.
   * @return maxPostWaitTime
  **/
  @Schema(description = "The maximum amount of time (in seconds) to wait for an HTTP POST response from the REST Consumer. Once this time is exceeded, the TCP connection is reset.")
  public Integer getMaxPostWaitTime() {
    return maxPostWaitTime;
  }

  public void setMaxPostWaitTime(Integer maxPostWaitTime) {
    this.maxPostWaitTime = maxPostWaitTime;
  }

  public MsgVpnRestDeliveryPointRestConsumer msgVpnName(String msgVpnName) {
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

  public MsgVpnRestDeliveryPointRestConsumer outgoingConnectionCount(Integer outgoingConnectionCount) {
    this.outgoingConnectionCount = outgoingConnectionCount;
    return this;
  }

   /**
   * The number of concurrent TCP connections open to the REST Consumer.
   * @return outgoingConnectionCount
  **/
  @Schema(description = "The number of concurrent TCP connections open to the REST Consumer.")
  public Integer getOutgoingConnectionCount() {
    return outgoingConnectionCount;
  }

  public void setOutgoingConnectionCount(Integer outgoingConnectionCount) {
    this.outgoingConnectionCount = outgoingConnectionCount;
  }

  public MsgVpnRestDeliveryPointRestConsumer remoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
    return this;
  }

   /**
   * The IP address or DNS name for the REST Consumer.
   * @return remoteHost
  **/
  @Schema(description = "The IP address or DNS name for the REST Consumer.")
  public String getRemoteHost() {
    return remoteHost;
  }

  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public MsgVpnRestDeliveryPointRestConsumer remoteOutgoingConnectionUpCount(Long remoteOutgoingConnectionUpCount) {
    this.remoteOutgoingConnectionUpCount = remoteOutgoingConnectionUpCount;
    return this;
  }

   /**
   * The number of outgoing connections for the REST Consumer that are up.
   * @return remoteOutgoingConnectionUpCount
  **/
  @Schema(description = "The number of outgoing connections for the REST Consumer that are up.")
  public Long getRemoteOutgoingConnectionUpCount() {
    return remoteOutgoingConnectionUpCount;
  }

  public void setRemoteOutgoingConnectionUpCount(Long remoteOutgoingConnectionUpCount) {
    this.remoteOutgoingConnectionUpCount = remoteOutgoingConnectionUpCount;
  }

  public MsgVpnRestDeliveryPointRestConsumer remotePort(Long remotePort) {
    this.remotePort = remotePort;
    return this;
  }

   /**
   * The port associated with the host of the REST Consumer.
   * @return remotePort
  **/
  @Schema(description = "The port associated with the host of the REST Consumer.")
  public Long getRemotePort() {
    return remotePort;
  }

  public void setRemotePort(Long remotePort) {
    this.remotePort = remotePort;
  }

  public MsgVpnRestDeliveryPointRestConsumer restConsumerName(String restConsumerName) {
    this.restConsumerName = restConsumerName;
    return this;
  }

   /**
   * The name of the REST Consumer.
   * @return restConsumerName
  **/
  @Schema(description = "The name of the REST Consumer.")
  public String getRestConsumerName() {
    return restConsumerName;
  }

  public void setRestConsumerName(String restConsumerName) {
    this.restConsumerName = restConsumerName;
  }

  public MsgVpnRestDeliveryPointRestConsumer restDeliveryPointName(String restDeliveryPointName) {
    this.restDeliveryPointName = restDeliveryPointName;
    return this;
  }

   /**
   * The name of the REST Delivery Point.
   * @return restDeliveryPointName
  **/
  @Schema(description = "The name of the REST Delivery Point.")
  public String getRestDeliveryPointName() {
    return restDeliveryPointName;
  }

  public void setRestDeliveryPointName(String restDeliveryPointName) {
    this.restDeliveryPointName = restDeliveryPointName;
  }

  public MsgVpnRestDeliveryPointRestConsumer retryDelay(Integer retryDelay) {
    this.retryDelay = retryDelay;
    return this;
  }

   /**
   * The number of seconds that must pass before retrying the remote REST Consumer connection.
   * @return retryDelay
  **/
  @Schema(description = "The number of seconds that must pass before retrying the remote REST Consumer connection.")
  public Integer getRetryDelay() {
    return retryDelay;
  }

  public void setRetryDelay(Integer retryDelay) {
    this.retryDelay = retryDelay;
  }

  public MsgVpnRestDeliveryPointRestConsumer tlsCipherSuiteList(String tlsCipherSuiteList) {
    this.tlsCipherSuiteList = tlsCipherSuiteList;
    return this;
  }

   /**
   * The colon-separated list of cipher suites the REST Consumer uses in its encrypted connection. The value &#x60;\&quot;default\&quot;&#x60; implies all supported suites ordered from most secure to least secure. The list of default cipher suites is available in the &#x60;tlsCipherSuiteMsgBackboneDefaultList&#x60; attribute of the Broker object in the Monitoring API. The REST Consumer should choose the first suite from this list that it supports.
   * @return tlsCipherSuiteList
  **/
  @Schema(description = "The colon-separated list of cipher suites the REST Consumer uses in its encrypted connection. The value `\"default\"` implies all supported suites ordered from most secure to least secure. The list of default cipher suites is available in the `tlsCipherSuiteMsgBackboneDefaultList` attribute of the Broker object in the Monitoring API. The REST Consumer should choose the first suite from this list that it supports.")
  public String getTlsCipherSuiteList() {
    return tlsCipherSuiteList;
  }

  public void setTlsCipherSuiteList(String tlsCipherSuiteList) {
    this.tlsCipherSuiteList = tlsCipherSuiteList;
  }

  public MsgVpnRestDeliveryPointRestConsumer tlsEnabled(Boolean tlsEnabled) {
    this.tlsEnabled = tlsEnabled;
    return this;
  }

   /**
   * Indicates whether encryption (TLS) is enabled for the REST Consumer.
   * @return tlsEnabled
  **/
  @Schema(description = "Indicates whether encryption (TLS) is enabled for the REST Consumer.")
  public Boolean isTlsEnabled() {
    return tlsEnabled;
  }

  public void setTlsEnabled(Boolean tlsEnabled) {
    this.tlsEnabled = tlsEnabled;
  }

  public MsgVpnRestDeliveryPointRestConsumer up(Boolean up) {
    this.up = up;
    return this;
  }

   /**
   * Indicates whether the operational state of the REST Consumer is up.
   * @return up
  **/
  @Schema(description = "Indicates whether the operational state of the REST Consumer is up.")
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
    MsgVpnRestDeliveryPointRestConsumer msgVpnRestDeliveryPointRestConsumer = (MsgVpnRestDeliveryPointRestConsumer) o;
    return Objects.equals(this.authenticationHttpBasicUsername, msgVpnRestDeliveryPointRestConsumer.authenticationHttpBasicUsername) &&
        Objects.equals(this.authenticationHttpHeaderName, msgVpnRestDeliveryPointRestConsumer.authenticationHttpHeaderName) &&
        Objects.equals(this.authenticationScheme, msgVpnRestDeliveryPointRestConsumer.authenticationScheme) &&
        Objects.equals(this.counter, msgVpnRestDeliveryPointRestConsumer.counter) &&
        Objects.equals(this.enabled, msgVpnRestDeliveryPointRestConsumer.enabled) &&
        Objects.equals(this.httpMethod, msgVpnRestDeliveryPointRestConsumer.httpMethod) &&
        Objects.equals(this.httpRequestConnectionCloseTxMsgCount, msgVpnRestDeliveryPointRestConsumer.httpRequestConnectionCloseTxMsgCount) &&
        Objects.equals(this.httpRequestOutstandingTxMsgCount, msgVpnRestDeliveryPointRestConsumer.httpRequestOutstandingTxMsgCount) &&
        Objects.equals(this.httpRequestTimedOutTxMsgCount, msgVpnRestDeliveryPointRestConsumer.httpRequestTimedOutTxMsgCount) &&
        Objects.equals(this.httpRequestTxByteCount, msgVpnRestDeliveryPointRestConsumer.httpRequestTxByteCount) &&
        Objects.equals(this.httpRequestTxMsgCount, msgVpnRestDeliveryPointRestConsumer.httpRequestTxMsgCount) &&
        Objects.equals(this.httpResponseErrorRxMsgCount, msgVpnRestDeliveryPointRestConsumer.httpResponseErrorRxMsgCount) &&
        Objects.equals(this.httpResponseRxByteCount, msgVpnRestDeliveryPointRestConsumer.httpResponseRxByteCount) &&
        Objects.equals(this.httpResponseRxMsgCount, msgVpnRestDeliveryPointRestConsumer.httpResponseRxMsgCount) &&
        Objects.equals(this.httpResponseSuccessRxMsgCount, msgVpnRestDeliveryPointRestConsumer.httpResponseSuccessRxMsgCount) &&
        Objects.equals(this.lastConnectionFailureLocalEndpoint, msgVpnRestDeliveryPointRestConsumer.lastConnectionFailureLocalEndpoint) &&
        Objects.equals(this.lastConnectionFailureReason, msgVpnRestDeliveryPointRestConsumer.lastConnectionFailureReason) &&
        Objects.equals(this.lastConnectionFailureRemoteEndpoint, msgVpnRestDeliveryPointRestConsumer.lastConnectionFailureRemoteEndpoint) &&
        Objects.equals(this.lastConnectionFailureTime, msgVpnRestDeliveryPointRestConsumer.lastConnectionFailureTime) &&
        Objects.equals(this.lastFailureReason, msgVpnRestDeliveryPointRestConsumer.lastFailureReason) &&
        Objects.equals(this.lastFailureTime, msgVpnRestDeliveryPointRestConsumer.lastFailureTime) &&
        Objects.equals(this.localInterface, msgVpnRestDeliveryPointRestConsumer.localInterface) &&
        Objects.equals(this.maxPostWaitTime, msgVpnRestDeliveryPointRestConsumer.maxPostWaitTime) &&
        Objects.equals(this.msgVpnName, msgVpnRestDeliveryPointRestConsumer.msgVpnName) &&
        Objects.equals(this.outgoingConnectionCount, msgVpnRestDeliveryPointRestConsumer.outgoingConnectionCount) &&
        Objects.equals(this.remoteHost, msgVpnRestDeliveryPointRestConsumer.remoteHost) &&
        Objects.equals(this.remoteOutgoingConnectionUpCount, msgVpnRestDeliveryPointRestConsumer.remoteOutgoingConnectionUpCount) &&
        Objects.equals(this.remotePort, msgVpnRestDeliveryPointRestConsumer.remotePort) &&
        Objects.equals(this.restConsumerName, msgVpnRestDeliveryPointRestConsumer.restConsumerName) &&
        Objects.equals(this.restDeliveryPointName, msgVpnRestDeliveryPointRestConsumer.restDeliveryPointName) &&
        Objects.equals(this.retryDelay, msgVpnRestDeliveryPointRestConsumer.retryDelay) &&
        Objects.equals(this.tlsCipherSuiteList, msgVpnRestDeliveryPointRestConsumer.tlsCipherSuiteList) &&
        Objects.equals(this.tlsEnabled, msgVpnRestDeliveryPointRestConsumer.tlsEnabled) &&
        Objects.equals(this.up, msgVpnRestDeliveryPointRestConsumer.up);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authenticationHttpBasicUsername, authenticationHttpHeaderName, authenticationScheme, counter, enabled, httpMethod, httpRequestConnectionCloseTxMsgCount, httpRequestOutstandingTxMsgCount, httpRequestTimedOutTxMsgCount, httpRequestTxByteCount, httpRequestTxMsgCount, httpResponseErrorRxMsgCount, httpResponseRxByteCount, httpResponseRxMsgCount, httpResponseSuccessRxMsgCount, lastConnectionFailureLocalEndpoint, lastConnectionFailureReason, lastConnectionFailureRemoteEndpoint, lastConnectionFailureTime, lastFailureReason, lastFailureTime, localInterface, maxPostWaitTime, msgVpnName, outgoingConnectionCount, remoteHost, remoteOutgoingConnectionUpCount, remotePort, restConsumerName, restDeliveryPointName, retryDelay, tlsCipherSuiteList, tlsEnabled, up);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnRestDeliveryPointRestConsumer {\n");
    
    sb.append("    authenticationHttpBasicUsername: ").append(toIndentedString(authenticationHttpBasicUsername)).append("\n");
    sb.append("    authenticationHttpHeaderName: ").append(toIndentedString(authenticationHttpHeaderName)).append("\n");
    sb.append("    authenticationScheme: ").append(toIndentedString(authenticationScheme)).append("\n");
    sb.append("    counter: ").append(toIndentedString(counter)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    httpMethod: ").append(toIndentedString(httpMethod)).append("\n");
    sb.append("    httpRequestConnectionCloseTxMsgCount: ").append(toIndentedString(httpRequestConnectionCloseTxMsgCount)).append("\n");
    sb.append("    httpRequestOutstandingTxMsgCount: ").append(toIndentedString(httpRequestOutstandingTxMsgCount)).append("\n");
    sb.append("    httpRequestTimedOutTxMsgCount: ").append(toIndentedString(httpRequestTimedOutTxMsgCount)).append("\n");
    sb.append("    httpRequestTxByteCount: ").append(toIndentedString(httpRequestTxByteCount)).append("\n");
    sb.append("    httpRequestTxMsgCount: ").append(toIndentedString(httpRequestTxMsgCount)).append("\n");
    sb.append("    httpResponseErrorRxMsgCount: ").append(toIndentedString(httpResponseErrorRxMsgCount)).append("\n");
    sb.append("    httpResponseRxByteCount: ").append(toIndentedString(httpResponseRxByteCount)).append("\n");
    sb.append("    httpResponseRxMsgCount: ").append(toIndentedString(httpResponseRxMsgCount)).append("\n");
    sb.append("    httpResponseSuccessRxMsgCount: ").append(toIndentedString(httpResponseSuccessRxMsgCount)).append("\n");
    sb.append("    lastConnectionFailureLocalEndpoint: ").append(toIndentedString(lastConnectionFailureLocalEndpoint)).append("\n");
    sb.append("    lastConnectionFailureReason: ").append(toIndentedString(lastConnectionFailureReason)).append("\n");
    sb.append("    lastConnectionFailureRemoteEndpoint: ").append(toIndentedString(lastConnectionFailureRemoteEndpoint)).append("\n");
    sb.append("    lastConnectionFailureTime: ").append(toIndentedString(lastConnectionFailureTime)).append("\n");
    sb.append("    lastFailureReason: ").append(toIndentedString(lastFailureReason)).append("\n");
    sb.append("    lastFailureTime: ").append(toIndentedString(lastFailureTime)).append("\n");
    sb.append("    localInterface: ").append(toIndentedString(localInterface)).append("\n");
    sb.append("    maxPostWaitTime: ").append(toIndentedString(maxPostWaitTime)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    outgoingConnectionCount: ").append(toIndentedString(outgoingConnectionCount)).append("\n");
    sb.append("    remoteHost: ").append(toIndentedString(remoteHost)).append("\n");
    sb.append("    remoteOutgoingConnectionUpCount: ").append(toIndentedString(remoteOutgoingConnectionUpCount)).append("\n");
    sb.append("    remotePort: ").append(toIndentedString(remotePort)).append("\n");
    sb.append("    restConsumerName: ").append(toIndentedString(restConsumerName)).append("\n");
    sb.append("    restDeliveryPointName: ").append(toIndentedString(restDeliveryPointName)).append("\n");
    sb.append("    retryDelay: ").append(toIndentedString(retryDelay)).append("\n");
    sb.append("    tlsCipherSuiteList: ").append(toIndentedString(tlsCipherSuiteList)).append("\n");
    sb.append("    tlsEnabled: ").append(toIndentedString(tlsEnabled)).append("\n");
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
