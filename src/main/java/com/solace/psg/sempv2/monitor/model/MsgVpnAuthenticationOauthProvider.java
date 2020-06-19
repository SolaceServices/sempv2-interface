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
 * MsgVpnAuthenticationOauthProvider
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-03-13T23:07:13.589Z")
public class MsgVpnAuthenticationOauthProvider {
  @SerializedName("audienceClaimName")
  private String audienceClaimName = null;

  /**
   * The audience claim source, indicating where to search for the audience value. The allowed values and their meaning are:  &lt;pre&gt; \&quot;access-token\&quot; - Search the access type JWT for the audience value. \&quot;id-token\&quot; - Search the ID type JWT for the audience value. \&quot;introspection\&quot; - Introspect the access token and search the result for the audience value. &lt;/pre&gt; 
   */
  @JsonAdapter(AudienceClaimSourceEnum.Adapter.class)
  public enum AudienceClaimSourceEnum {
    ACCESS_TOKEN("access-token"),
    
    ID_TOKEN("id-token"),
    
    INTROSPECTION("introspection");

    private String value;

    AudienceClaimSourceEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static AudienceClaimSourceEnum fromValue(String text) {
      for (AudienceClaimSourceEnum b : AudienceClaimSourceEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<AudienceClaimSourceEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final AudienceClaimSourceEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public AudienceClaimSourceEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return AudienceClaimSourceEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("audienceClaimSource")
  private AudienceClaimSourceEnum audienceClaimSource = null;

  @SerializedName("audienceClaimValue")
  private String audienceClaimValue = null;

  @SerializedName("audienceValidationEnabled")
  private Boolean audienceValidationEnabled = null;

  @SerializedName("authenticationSuccessCount")
  private Long authenticationSuccessCount = null;

  @SerializedName("authorizationGroupClaimName")
  private String authorizationGroupClaimName = null;

  /**
   * The authorization group claim source, indicating where to search for the authorization group name. The allowed values and their meaning are:  &lt;pre&gt; \&quot;access-token\&quot; - Search the access type JWT for the authorization group name. \&quot;id-token\&quot; - Search the ID type JWT for the authorization group name. \&quot;introspection\&quot; - Introspect the access token and search the result for the authorization group name. &lt;/pre&gt; 
   */
  @JsonAdapter(AuthorizationGroupClaimSourceEnum.Adapter.class)
  public enum AuthorizationGroupClaimSourceEnum {
    ACCESS_TOKEN("access-token"),
    
    ID_TOKEN("id-token"),
    
    INTROSPECTION("introspection");

    private String value;

    AuthorizationGroupClaimSourceEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static AuthorizationGroupClaimSourceEnum fromValue(String text) {
      for (AuthorizationGroupClaimSourceEnum b : AuthorizationGroupClaimSourceEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<AuthorizationGroupClaimSourceEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final AuthorizationGroupClaimSourceEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public AuthorizationGroupClaimSourceEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return AuthorizationGroupClaimSourceEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("authorizationGroupClaimSource")
  private AuthorizationGroupClaimSourceEnum authorizationGroupClaimSource = null;

  @SerializedName("authorizationGroupEnabled")
  private Boolean authorizationGroupEnabled = null;

  @SerializedName("disconnectOnTokenExpirationEnabled")
  private Boolean disconnectOnTokenExpirationEnabled = null;

  @SerializedName("enabled")
  private Boolean enabled = null;

  @SerializedName("jwksLastRefreshFailureReason")
  private String jwksLastRefreshFailureReason = null;

  @SerializedName("jwksLastRefreshFailureTime")
  private Integer jwksLastRefreshFailureTime = null;

  @SerializedName("jwksLastRefreshTime")
  private Integer jwksLastRefreshTime = null;

  @SerializedName("jwksNextScheduledRefreshTime")
  private Integer jwksNextScheduledRefreshTime = null;

  @SerializedName("jwksRefreshFailureCount")
  private Long jwksRefreshFailureCount = null;

  @SerializedName("jwksRefreshInterval")
  private Integer jwksRefreshInterval = null;

  @SerializedName("jwksUri")
  private String jwksUri = null;

  @SerializedName("loginFailureIncorrectAudienceValueCount")
  private Long loginFailureIncorrectAudienceValueCount = null;

  @SerializedName("loginFailureInvalidAudienceValueCount")
  private Long loginFailureInvalidAudienceValueCount = null;

  @SerializedName("loginFailureInvalidAuthorizationGroupValueCount")
  private Long loginFailureInvalidAuthorizationGroupValueCount = null;

  @SerializedName("loginFailureInvalidJwtSignatureCount")
  private Long loginFailureInvalidJwtSignatureCount = null;

  @SerializedName("loginFailureInvalidUsernameValueCount")
  private Long loginFailureInvalidUsernameValueCount = null;

  @SerializedName("loginFailureMismatchedUsernameCount")
  private Long loginFailureMismatchedUsernameCount = null;

  @SerializedName("loginFailureMissingAudienceCount")
  private Long loginFailureMissingAudienceCount = null;

  @SerializedName("loginFailureMissingJwkCount")
  private Long loginFailureMissingJwkCount = null;

  @SerializedName("loginFailureMissingOrInvalidTokenCount")
  private Long loginFailureMissingOrInvalidTokenCount = null;

  @SerializedName("loginFailureMissingUsernameCount")
  private Long loginFailureMissingUsernameCount = null;

  @SerializedName("loginFailureTokenExpiredCount")
  private Long loginFailureTokenExpiredCount = null;

  @SerializedName("loginFailureTokenIntrospectionErroredCount")
  private Long loginFailureTokenIntrospectionErroredCount = null;

  @SerializedName("loginFailureTokenIntrospectionFailureCount")
  private Long loginFailureTokenIntrospectionFailureCount = null;

  @SerializedName("loginFailureTokenIntrospectionHttpsErrorCount")
  private Long loginFailureTokenIntrospectionHttpsErrorCount = null;

  @SerializedName("loginFailureTokenIntrospectionInvalidCount")
  private Long loginFailureTokenIntrospectionInvalidCount = null;

  @SerializedName("loginFailureTokenIntrospectionTimeoutCount")
  private Long loginFailureTokenIntrospectionTimeoutCount = null;

  @SerializedName("loginFailureTokenNotValidYetCount")
  private Long loginFailureTokenNotValidYetCount = null;

  @SerializedName("loginFailureUnsupportedAlgCount")
  private Long loginFailureUnsupportedAlgCount = null;

  @SerializedName("missingAuthorizationGroupCount")
  private Long missingAuthorizationGroupCount = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("oauthProviderName")
  private String oauthProviderName = null;

  @SerializedName("tokenIgnoreTimeLimitsEnabled")
  private Boolean tokenIgnoreTimeLimitsEnabled = null;

  @SerializedName("tokenIntrospectionAverageTime")
  private Integer tokenIntrospectionAverageTime = null;

  @SerializedName("tokenIntrospectionLastFailureReason")
  private String tokenIntrospectionLastFailureReason = null;

  @SerializedName("tokenIntrospectionLastFailureTime")
  private Integer tokenIntrospectionLastFailureTime = null;

  @SerializedName("tokenIntrospectionParameterName")
  private String tokenIntrospectionParameterName = null;

  @SerializedName("tokenIntrospectionSuccessCount")
  private Long tokenIntrospectionSuccessCount = null;

  @SerializedName("tokenIntrospectionTimeout")
  private Integer tokenIntrospectionTimeout = null;

  @SerializedName("tokenIntrospectionUri")
  private String tokenIntrospectionUri = null;

  @SerializedName("tokenIntrospectionUsername")
  private String tokenIntrospectionUsername = null;

  @SerializedName("usernameClaimName")
  private String usernameClaimName = null;

  /**
   * The username claim source, indicating where to search for the username value. The allowed values and their meaning are:  &lt;pre&gt; \&quot;access-token\&quot; - Search the access type JWT for the username value. \&quot;id-token\&quot; - Search the ID type JWT for the username value. \&quot;introspection\&quot; - Introspect the access token and search the result for the username value. &lt;/pre&gt; 
   */
  @JsonAdapter(UsernameClaimSourceEnum.Adapter.class)
  public enum UsernameClaimSourceEnum {
    ACCESS_TOKEN("access-token"),
    
    ID_TOKEN("id-token"),
    
    INTROSPECTION("introspection");

    private String value;

    UsernameClaimSourceEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static UsernameClaimSourceEnum fromValue(String text) {
      for (UsernameClaimSourceEnum b : UsernameClaimSourceEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<UsernameClaimSourceEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final UsernameClaimSourceEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public UsernameClaimSourceEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return UsernameClaimSourceEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("usernameClaimSource")
  private UsernameClaimSourceEnum usernameClaimSource = null;

  @SerializedName("usernameValidateEnabled")
  private Boolean usernameValidateEnabled = null;

  public MsgVpnAuthenticationOauthProvider audienceClaimName(String audienceClaimName) {
    this.audienceClaimName = audienceClaimName;
    return this;
  }

   /**
   * The audience claim name, indicating which part of the object to use for determining the audience.
   * @return audienceClaimName
  **/
  @ApiModelProperty(value = "The audience claim name, indicating which part of the object to use for determining the audience.")
  public String getAudienceClaimName() {
    return audienceClaimName;
  }

  public void setAudienceClaimName(String audienceClaimName) {
    this.audienceClaimName = audienceClaimName;
  }

  public MsgVpnAuthenticationOauthProvider audienceClaimSource(AudienceClaimSourceEnum audienceClaimSource) {
    this.audienceClaimSource = audienceClaimSource;
    return this;
  }

   /**
   * The audience claim source, indicating where to search for the audience value. The allowed values and their meaning are:  &lt;pre&gt; \&quot;access-token\&quot; - Search the access type JWT for the audience value. \&quot;id-token\&quot; - Search the ID type JWT for the audience value. \&quot;introspection\&quot; - Introspect the access token and search the result for the audience value. &lt;/pre&gt; 
   * @return audienceClaimSource
  **/
  @ApiModelProperty(value = "The audience claim source, indicating where to search for the audience value. The allowed values and their meaning are:  <pre> \"access-token\" - Search the access type JWT for the audience value. \"id-token\" - Search the ID type JWT for the audience value. \"introspection\" - Introspect the access token and search the result for the audience value. </pre> ")
  public AudienceClaimSourceEnum getAudienceClaimSource() {
    return audienceClaimSource;
  }

  public void setAudienceClaimSource(AudienceClaimSourceEnum audienceClaimSource) {
    this.audienceClaimSource = audienceClaimSource;
  }

  public MsgVpnAuthenticationOauthProvider audienceClaimValue(String audienceClaimValue) {
    this.audienceClaimValue = audienceClaimValue;
    return this;
  }

   /**
   * The required audience value for a token to be considered valid.
   * @return audienceClaimValue
  **/
  @ApiModelProperty(value = "The required audience value for a token to be considered valid.")
  public String getAudienceClaimValue() {
    return audienceClaimValue;
  }

  public void setAudienceClaimValue(String audienceClaimValue) {
    this.audienceClaimValue = audienceClaimValue;
  }

  public MsgVpnAuthenticationOauthProvider audienceValidationEnabled(Boolean audienceValidationEnabled) {
    this.audienceValidationEnabled = audienceValidationEnabled;
    return this;
  }

   /**
   * Indicates whether audience validation is enabled.
   * @return audienceValidationEnabled
  **/
  @ApiModelProperty(value = "Indicates whether audience validation is enabled.")
  public Boolean isAudienceValidationEnabled() {
    return audienceValidationEnabled;
  }

  public void setAudienceValidationEnabled(Boolean audienceValidationEnabled) {
    this.audienceValidationEnabled = audienceValidationEnabled;
  }

  public MsgVpnAuthenticationOauthProvider authenticationSuccessCount(Long authenticationSuccessCount) {
    this.authenticationSuccessCount = authenticationSuccessCount;
    return this;
  }

   /**
   * The number of OAuth Provider client authentications that succeeded.
   * @return authenticationSuccessCount
  **/
  @ApiModelProperty(value = "The number of OAuth Provider client authentications that succeeded.")
  public Long getAuthenticationSuccessCount() {
    return authenticationSuccessCount;
  }

  public void setAuthenticationSuccessCount(Long authenticationSuccessCount) {
    this.authenticationSuccessCount = authenticationSuccessCount;
  }

  public MsgVpnAuthenticationOauthProvider authorizationGroupClaimName(String authorizationGroupClaimName) {
    this.authorizationGroupClaimName = authorizationGroupClaimName;
    return this;
  }

   /**
   * The authorization group claim name, indicating which part of the object to use for determining the authorization group.
   * @return authorizationGroupClaimName
  **/
  @ApiModelProperty(value = "The authorization group claim name, indicating which part of the object to use for determining the authorization group.")
  public String getAuthorizationGroupClaimName() {
    return authorizationGroupClaimName;
  }

  public void setAuthorizationGroupClaimName(String authorizationGroupClaimName) {
    this.authorizationGroupClaimName = authorizationGroupClaimName;
  }

  public MsgVpnAuthenticationOauthProvider authorizationGroupClaimSource(AuthorizationGroupClaimSourceEnum authorizationGroupClaimSource) {
    this.authorizationGroupClaimSource = authorizationGroupClaimSource;
    return this;
  }

   /**
   * The authorization group claim source, indicating where to search for the authorization group name. The allowed values and their meaning are:  &lt;pre&gt; \&quot;access-token\&quot; - Search the access type JWT for the authorization group name. \&quot;id-token\&quot; - Search the ID type JWT for the authorization group name. \&quot;introspection\&quot; - Introspect the access token and search the result for the authorization group name. &lt;/pre&gt; 
   * @return authorizationGroupClaimSource
  **/
  @ApiModelProperty(value = "The authorization group claim source, indicating where to search for the authorization group name. The allowed values and their meaning are:  <pre> \"access-token\" - Search the access type JWT for the authorization group name. \"id-token\" - Search the ID type JWT for the authorization group name. \"introspection\" - Introspect the access token and search the result for the authorization group name. </pre> ")
  public AuthorizationGroupClaimSourceEnum getAuthorizationGroupClaimSource() {
    return authorizationGroupClaimSource;
  }

  public void setAuthorizationGroupClaimSource(AuthorizationGroupClaimSourceEnum authorizationGroupClaimSource) {
    this.authorizationGroupClaimSource = authorizationGroupClaimSource;
  }

  public MsgVpnAuthenticationOauthProvider authorizationGroupEnabled(Boolean authorizationGroupEnabled) {
    this.authorizationGroupEnabled = authorizationGroupEnabled;
    return this;
  }

   /**
   * Indicates whether OAuth based authorization is enabled and the configured authorization type for OAuth clients is overridden.
   * @return authorizationGroupEnabled
  **/
  @ApiModelProperty(value = "Indicates whether OAuth based authorization is enabled and the configured authorization type for OAuth clients is overridden.")
  public Boolean isAuthorizationGroupEnabled() {
    return authorizationGroupEnabled;
  }

  public void setAuthorizationGroupEnabled(Boolean authorizationGroupEnabled) {
    this.authorizationGroupEnabled = authorizationGroupEnabled;
  }

  public MsgVpnAuthenticationOauthProvider disconnectOnTokenExpirationEnabled(Boolean disconnectOnTokenExpirationEnabled) {
    this.disconnectOnTokenExpirationEnabled = disconnectOnTokenExpirationEnabled;
    return this;
  }

   /**
   * Indicates whether clients are disconnected when their tokens expire.
   * @return disconnectOnTokenExpirationEnabled
  **/
  @ApiModelProperty(value = "Indicates whether clients are disconnected when their tokens expire.")
  public Boolean isDisconnectOnTokenExpirationEnabled() {
    return disconnectOnTokenExpirationEnabled;
  }

  public void setDisconnectOnTokenExpirationEnabled(Boolean disconnectOnTokenExpirationEnabled) {
    this.disconnectOnTokenExpirationEnabled = disconnectOnTokenExpirationEnabled;
  }

  public MsgVpnAuthenticationOauthProvider enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

   /**
   * Indicates whether OAuth Provider client authentication is enabled.
   * @return enabled
  **/
  @ApiModelProperty(value = "Indicates whether OAuth Provider client authentication is enabled.")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public MsgVpnAuthenticationOauthProvider jwksLastRefreshFailureReason(String jwksLastRefreshFailureReason) {
    this.jwksLastRefreshFailureReason = jwksLastRefreshFailureReason;
    return this;
  }

   /**
   * The reason for the last JWKS public key refresh failure.
   * @return jwksLastRefreshFailureReason
  **/
  @ApiModelProperty(value = "The reason for the last JWKS public key refresh failure.")
  public String getJwksLastRefreshFailureReason() {
    return jwksLastRefreshFailureReason;
  }

  public void setJwksLastRefreshFailureReason(String jwksLastRefreshFailureReason) {
    this.jwksLastRefreshFailureReason = jwksLastRefreshFailureReason;
  }

  public MsgVpnAuthenticationOauthProvider jwksLastRefreshFailureTime(Integer jwksLastRefreshFailureTime) {
    this.jwksLastRefreshFailureTime = jwksLastRefreshFailureTime;
    return this;
  }

   /**
   * The timestamp of the last JWKS public key refresh failure. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return jwksLastRefreshFailureTime
  **/
  @ApiModelProperty(value = "The timestamp of the last JWKS public key refresh failure. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getJwksLastRefreshFailureTime() {
    return jwksLastRefreshFailureTime;
  }

  public void setJwksLastRefreshFailureTime(Integer jwksLastRefreshFailureTime) {
    this.jwksLastRefreshFailureTime = jwksLastRefreshFailureTime;
  }

  public MsgVpnAuthenticationOauthProvider jwksLastRefreshTime(Integer jwksLastRefreshTime) {
    this.jwksLastRefreshTime = jwksLastRefreshTime;
    return this;
  }

   /**
   * The timestamp of the last JWKS public key refresh success. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return jwksLastRefreshTime
  **/
  @ApiModelProperty(value = "The timestamp of the last JWKS public key refresh success. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getJwksLastRefreshTime() {
    return jwksLastRefreshTime;
  }

  public void setJwksLastRefreshTime(Integer jwksLastRefreshTime) {
    this.jwksLastRefreshTime = jwksLastRefreshTime;
  }

  public MsgVpnAuthenticationOauthProvider jwksNextScheduledRefreshTime(Integer jwksNextScheduledRefreshTime) {
    this.jwksNextScheduledRefreshTime = jwksNextScheduledRefreshTime;
    return this;
  }

   /**
   * The timestamp of the next scheduled JWKS public key refresh. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return jwksNextScheduledRefreshTime
  **/
  @ApiModelProperty(value = "The timestamp of the next scheduled JWKS public key refresh. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getJwksNextScheduledRefreshTime() {
    return jwksNextScheduledRefreshTime;
  }

  public void setJwksNextScheduledRefreshTime(Integer jwksNextScheduledRefreshTime) {
    this.jwksNextScheduledRefreshTime = jwksNextScheduledRefreshTime;
  }

  public MsgVpnAuthenticationOauthProvider jwksRefreshFailureCount(Long jwksRefreshFailureCount) {
    this.jwksRefreshFailureCount = jwksRefreshFailureCount;
    return this;
  }

   /**
   * The number of JWKS public key refresh failures.
   * @return jwksRefreshFailureCount
  **/
  @ApiModelProperty(value = "The number of JWKS public key refresh failures.")
  public Long getJwksRefreshFailureCount() {
    return jwksRefreshFailureCount;
  }

  public void setJwksRefreshFailureCount(Long jwksRefreshFailureCount) {
    this.jwksRefreshFailureCount = jwksRefreshFailureCount;
  }

  public MsgVpnAuthenticationOauthProvider jwksRefreshInterval(Integer jwksRefreshInterval) {
    this.jwksRefreshInterval = jwksRefreshInterval;
    return this;
  }

   /**
   * The number of seconds between forced JWKS public key refreshing.
   * @return jwksRefreshInterval
  **/
  @ApiModelProperty(value = "The number of seconds between forced JWKS public key refreshing.")
  public Integer getJwksRefreshInterval() {
    return jwksRefreshInterval;
  }

  public void setJwksRefreshInterval(Integer jwksRefreshInterval) {
    this.jwksRefreshInterval = jwksRefreshInterval;
  }

  public MsgVpnAuthenticationOauthProvider jwksUri(String jwksUri) {
    this.jwksUri = jwksUri;
    return this;
  }

   /**
   * The URI where the OAuth provider publishes its JWKS public keys.
   * @return jwksUri
  **/
  @ApiModelProperty(value = "The URI where the OAuth provider publishes its JWKS public keys.")
  public String getJwksUri() {
    return jwksUri;
  }

  public void setJwksUri(String jwksUri) {
    this.jwksUri = jwksUri;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureIncorrectAudienceValueCount(Long loginFailureIncorrectAudienceValueCount) {
    this.loginFailureIncorrectAudienceValueCount = loginFailureIncorrectAudienceValueCount;
    return this;
  }

   /**
   * The number of login failures due to an incorrect audience value.
   * @return loginFailureIncorrectAudienceValueCount
  **/
  @ApiModelProperty(value = "The number of login failures due to an incorrect audience value.")
  public Long getLoginFailureIncorrectAudienceValueCount() {
    return loginFailureIncorrectAudienceValueCount;
  }

  public void setLoginFailureIncorrectAudienceValueCount(Long loginFailureIncorrectAudienceValueCount) {
    this.loginFailureIncorrectAudienceValueCount = loginFailureIncorrectAudienceValueCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureInvalidAudienceValueCount(Long loginFailureInvalidAudienceValueCount) {
    this.loginFailureInvalidAudienceValueCount = loginFailureInvalidAudienceValueCount;
    return this;
  }

   /**
   * The number of login failures due to an invalid audience value.
   * @return loginFailureInvalidAudienceValueCount
  **/
  @ApiModelProperty(value = "The number of login failures due to an invalid audience value.")
  public Long getLoginFailureInvalidAudienceValueCount() {
    return loginFailureInvalidAudienceValueCount;
  }

  public void setLoginFailureInvalidAudienceValueCount(Long loginFailureInvalidAudienceValueCount) {
    this.loginFailureInvalidAudienceValueCount = loginFailureInvalidAudienceValueCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureInvalidAuthorizationGroupValueCount(Long loginFailureInvalidAuthorizationGroupValueCount) {
    this.loginFailureInvalidAuthorizationGroupValueCount = loginFailureInvalidAuthorizationGroupValueCount;
    return this;
  }

   /**
   * The number of login failures due to an invalid authorization group value (zero-length or non-string).
   * @return loginFailureInvalidAuthorizationGroupValueCount
  **/
  @ApiModelProperty(value = "The number of login failures due to an invalid authorization group value (zero-length or non-string).")
  public Long getLoginFailureInvalidAuthorizationGroupValueCount() {
    return loginFailureInvalidAuthorizationGroupValueCount;
  }

  public void setLoginFailureInvalidAuthorizationGroupValueCount(Long loginFailureInvalidAuthorizationGroupValueCount) {
    this.loginFailureInvalidAuthorizationGroupValueCount = loginFailureInvalidAuthorizationGroupValueCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureInvalidJwtSignatureCount(Long loginFailureInvalidJwtSignatureCount) {
    this.loginFailureInvalidJwtSignatureCount = loginFailureInvalidJwtSignatureCount;
    return this;
  }

   /**
   * The number of login failures due to an invalid JWT signature.
   * @return loginFailureInvalidJwtSignatureCount
  **/
  @ApiModelProperty(value = "The number of login failures due to an invalid JWT signature.")
  public Long getLoginFailureInvalidJwtSignatureCount() {
    return loginFailureInvalidJwtSignatureCount;
  }

  public void setLoginFailureInvalidJwtSignatureCount(Long loginFailureInvalidJwtSignatureCount) {
    this.loginFailureInvalidJwtSignatureCount = loginFailureInvalidJwtSignatureCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureInvalidUsernameValueCount(Long loginFailureInvalidUsernameValueCount) {
    this.loginFailureInvalidUsernameValueCount = loginFailureInvalidUsernameValueCount;
    return this;
  }

   /**
   * The number of login failures due to an invalid username value.
   * @return loginFailureInvalidUsernameValueCount
  **/
  @ApiModelProperty(value = "The number of login failures due to an invalid username value.")
  public Long getLoginFailureInvalidUsernameValueCount() {
    return loginFailureInvalidUsernameValueCount;
  }

  public void setLoginFailureInvalidUsernameValueCount(Long loginFailureInvalidUsernameValueCount) {
    this.loginFailureInvalidUsernameValueCount = loginFailureInvalidUsernameValueCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureMismatchedUsernameCount(Long loginFailureMismatchedUsernameCount) {
    this.loginFailureMismatchedUsernameCount = loginFailureMismatchedUsernameCount;
    return this;
  }

   /**
   * The number of login failures due to a mismatched username.
   * @return loginFailureMismatchedUsernameCount
  **/
  @ApiModelProperty(value = "The number of login failures due to a mismatched username.")
  public Long getLoginFailureMismatchedUsernameCount() {
    return loginFailureMismatchedUsernameCount;
  }

  public void setLoginFailureMismatchedUsernameCount(Long loginFailureMismatchedUsernameCount) {
    this.loginFailureMismatchedUsernameCount = loginFailureMismatchedUsernameCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureMissingAudienceCount(Long loginFailureMissingAudienceCount) {
    this.loginFailureMissingAudienceCount = loginFailureMissingAudienceCount;
    return this;
  }

   /**
   * The number of login failures due to a missing audience claim.
   * @return loginFailureMissingAudienceCount
  **/
  @ApiModelProperty(value = "The number of login failures due to a missing audience claim.")
  public Long getLoginFailureMissingAudienceCount() {
    return loginFailureMissingAudienceCount;
  }

  public void setLoginFailureMissingAudienceCount(Long loginFailureMissingAudienceCount) {
    this.loginFailureMissingAudienceCount = loginFailureMissingAudienceCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureMissingJwkCount(Long loginFailureMissingJwkCount) {
    this.loginFailureMissingJwkCount = loginFailureMissingJwkCount;
    return this;
  }

   /**
   * The number of login failures due to a missing JSON Web Key (JWK).
   * @return loginFailureMissingJwkCount
  **/
  @ApiModelProperty(value = "The number of login failures due to a missing JSON Web Key (JWK).")
  public Long getLoginFailureMissingJwkCount() {
    return loginFailureMissingJwkCount;
  }

  public void setLoginFailureMissingJwkCount(Long loginFailureMissingJwkCount) {
    this.loginFailureMissingJwkCount = loginFailureMissingJwkCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureMissingOrInvalidTokenCount(Long loginFailureMissingOrInvalidTokenCount) {
    this.loginFailureMissingOrInvalidTokenCount = loginFailureMissingOrInvalidTokenCount;
    return this;
  }

   /**
   * The number of login failures due to a missing or invalid token.
   * @return loginFailureMissingOrInvalidTokenCount
  **/
  @ApiModelProperty(value = "The number of login failures due to a missing or invalid token.")
  public Long getLoginFailureMissingOrInvalidTokenCount() {
    return loginFailureMissingOrInvalidTokenCount;
  }

  public void setLoginFailureMissingOrInvalidTokenCount(Long loginFailureMissingOrInvalidTokenCount) {
    this.loginFailureMissingOrInvalidTokenCount = loginFailureMissingOrInvalidTokenCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureMissingUsernameCount(Long loginFailureMissingUsernameCount) {
    this.loginFailureMissingUsernameCount = loginFailureMissingUsernameCount;
    return this;
  }

   /**
   * The number of login failures due to a missing username.
   * @return loginFailureMissingUsernameCount
  **/
  @ApiModelProperty(value = "The number of login failures due to a missing username.")
  public Long getLoginFailureMissingUsernameCount() {
    return loginFailureMissingUsernameCount;
  }

  public void setLoginFailureMissingUsernameCount(Long loginFailureMissingUsernameCount) {
    this.loginFailureMissingUsernameCount = loginFailureMissingUsernameCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureTokenExpiredCount(Long loginFailureTokenExpiredCount) {
    this.loginFailureTokenExpiredCount = loginFailureTokenExpiredCount;
    return this;
  }

   /**
   * The number of login failures due to a token being expired.
   * @return loginFailureTokenExpiredCount
  **/
  @ApiModelProperty(value = "The number of login failures due to a token being expired.")
  public Long getLoginFailureTokenExpiredCount() {
    return loginFailureTokenExpiredCount;
  }

  public void setLoginFailureTokenExpiredCount(Long loginFailureTokenExpiredCount) {
    this.loginFailureTokenExpiredCount = loginFailureTokenExpiredCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureTokenIntrospectionErroredCount(Long loginFailureTokenIntrospectionErroredCount) {
    this.loginFailureTokenIntrospectionErroredCount = loginFailureTokenIntrospectionErroredCount;
    return this;
  }

   /**
   * The number of login failures due to a token introspection error response.
   * @return loginFailureTokenIntrospectionErroredCount
  **/
  @ApiModelProperty(value = "The number of login failures due to a token introspection error response.")
  public Long getLoginFailureTokenIntrospectionErroredCount() {
    return loginFailureTokenIntrospectionErroredCount;
  }

  public void setLoginFailureTokenIntrospectionErroredCount(Long loginFailureTokenIntrospectionErroredCount) {
    this.loginFailureTokenIntrospectionErroredCount = loginFailureTokenIntrospectionErroredCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureTokenIntrospectionFailureCount(Long loginFailureTokenIntrospectionFailureCount) {
    this.loginFailureTokenIntrospectionFailureCount = loginFailureTokenIntrospectionFailureCount;
    return this;
  }

   /**
   * The number of login failures due to a failure to complete the token introspection.
   * @return loginFailureTokenIntrospectionFailureCount
  **/
  @ApiModelProperty(value = "The number of login failures due to a failure to complete the token introspection.")
  public Long getLoginFailureTokenIntrospectionFailureCount() {
    return loginFailureTokenIntrospectionFailureCount;
  }

  public void setLoginFailureTokenIntrospectionFailureCount(Long loginFailureTokenIntrospectionFailureCount) {
    this.loginFailureTokenIntrospectionFailureCount = loginFailureTokenIntrospectionFailureCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureTokenIntrospectionHttpsErrorCount(Long loginFailureTokenIntrospectionHttpsErrorCount) {
    this.loginFailureTokenIntrospectionHttpsErrorCount = loginFailureTokenIntrospectionHttpsErrorCount;
    return this;
  }

   /**
   * The number of login failures due to a token introspection HTTPS error.
   * @return loginFailureTokenIntrospectionHttpsErrorCount
  **/
  @ApiModelProperty(value = "The number of login failures due to a token introspection HTTPS error.")
  public Long getLoginFailureTokenIntrospectionHttpsErrorCount() {
    return loginFailureTokenIntrospectionHttpsErrorCount;
  }

  public void setLoginFailureTokenIntrospectionHttpsErrorCount(Long loginFailureTokenIntrospectionHttpsErrorCount) {
    this.loginFailureTokenIntrospectionHttpsErrorCount = loginFailureTokenIntrospectionHttpsErrorCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureTokenIntrospectionInvalidCount(Long loginFailureTokenIntrospectionInvalidCount) {
    this.loginFailureTokenIntrospectionInvalidCount = loginFailureTokenIntrospectionInvalidCount;
    return this;
  }

   /**
   * The number of login failures due to a token introspection response being invalid.
   * @return loginFailureTokenIntrospectionInvalidCount
  **/
  @ApiModelProperty(value = "The number of login failures due to a token introspection response being invalid.")
  public Long getLoginFailureTokenIntrospectionInvalidCount() {
    return loginFailureTokenIntrospectionInvalidCount;
  }

  public void setLoginFailureTokenIntrospectionInvalidCount(Long loginFailureTokenIntrospectionInvalidCount) {
    this.loginFailureTokenIntrospectionInvalidCount = loginFailureTokenIntrospectionInvalidCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureTokenIntrospectionTimeoutCount(Long loginFailureTokenIntrospectionTimeoutCount) {
    this.loginFailureTokenIntrospectionTimeoutCount = loginFailureTokenIntrospectionTimeoutCount;
    return this;
  }

   /**
   * The number of login failures due to a token introspection timeout.
   * @return loginFailureTokenIntrospectionTimeoutCount
  **/
  @ApiModelProperty(value = "The number of login failures due to a token introspection timeout.")
  public Long getLoginFailureTokenIntrospectionTimeoutCount() {
    return loginFailureTokenIntrospectionTimeoutCount;
  }

  public void setLoginFailureTokenIntrospectionTimeoutCount(Long loginFailureTokenIntrospectionTimeoutCount) {
    this.loginFailureTokenIntrospectionTimeoutCount = loginFailureTokenIntrospectionTimeoutCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureTokenNotValidYetCount(Long loginFailureTokenNotValidYetCount) {
    this.loginFailureTokenNotValidYetCount = loginFailureTokenNotValidYetCount;
    return this;
  }

   /**
   * The number of login failures due to a token not being valid yet.
   * @return loginFailureTokenNotValidYetCount
  **/
  @ApiModelProperty(value = "The number of login failures due to a token not being valid yet.")
  public Long getLoginFailureTokenNotValidYetCount() {
    return loginFailureTokenNotValidYetCount;
  }

  public void setLoginFailureTokenNotValidYetCount(Long loginFailureTokenNotValidYetCount) {
    this.loginFailureTokenNotValidYetCount = loginFailureTokenNotValidYetCount;
  }

  public MsgVpnAuthenticationOauthProvider loginFailureUnsupportedAlgCount(Long loginFailureUnsupportedAlgCount) {
    this.loginFailureUnsupportedAlgCount = loginFailureUnsupportedAlgCount;
    return this;
  }

   /**
   * The number of login failures due to an unsupported algorithm.
   * @return loginFailureUnsupportedAlgCount
  **/
  @ApiModelProperty(value = "The number of login failures due to an unsupported algorithm.")
  public Long getLoginFailureUnsupportedAlgCount() {
    return loginFailureUnsupportedAlgCount;
  }

  public void setLoginFailureUnsupportedAlgCount(Long loginFailureUnsupportedAlgCount) {
    this.loginFailureUnsupportedAlgCount = loginFailureUnsupportedAlgCount;
  }

  public MsgVpnAuthenticationOauthProvider missingAuthorizationGroupCount(Long missingAuthorizationGroupCount) {
    this.missingAuthorizationGroupCount = missingAuthorizationGroupCount;
    return this;
  }

   /**
   * The number of clients that did not provide an authorization group claim value when expected.
   * @return missingAuthorizationGroupCount
  **/
  @ApiModelProperty(value = "The number of clients that did not provide an authorization group claim value when expected.")
  public Long getMissingAuthorizationGroupCount() {
    return missingAuthorizationGroupCount;
  }

  public void setMissingAuthorizationGroupCount(Long missingAuthorizationGroupCount) {
    this.missingAuthorizationGroupCount = missingAuthorizationGroupCount;
  }

  public MsgVpnAuthenticationOauthProvider msgVpnName(String msgVpnName) {
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

  public MsgVpnAuthenticationOauthProvider oauthProviderName(String oauthProviderName) {
    this.oauthProviderName = oauthProviderName;
    return this;
  }

   /**
   * The name of the OAuth Provider.
   * @return oauthProviderName
  **/
  @ApiModelProperty(value = "The name of the OAuth Provider.")
  public String getOauthProviderName() {
    return oauthProviderName;
  }

  public void setOauthProviderName(String oauthProviderName) {
    this.oauthProviderName = oauthProviderName;
  }

  public MsgVpnAuthenticationOauthProvider tokenIgnoreTimeLimitsEnabled(Boolean tokenIgnoreTimeLimitsEnabled) {
    this.tokenIgnoreTimeLimitsEnabled = tokenIgnoreTimeLimitsEnabled;
    return this;
  }

   /**
   * Indicates whether to ignore time limits and accept tokens that are not yet valid or are no longer valid.
   * @return tokenIgnoreTimeLimitsEnabled
  **/
  @ApiModelProperty(value = "Indicates whether to ignore time limits and accept tokens that are not yet valid or are no longer valid.")
  public Boolean isTokenIgnoreTimeLimitsEnabled() {
    return tokenIgnoreTimeLimitsEnabled;
  }

  public void setTokenIgnoreTimeLimitsEnabled(Boolean tokenIgnoreTimeLimitsEnabled) {
    this.tokenIgnoreTimeLimitsEnabled = tokenIgnoreTimeLimitsEnabled;
  }

  public MsgVpnAuthenticationOauthProvider tokenIntrospectionAverageTime(Integer tokenIntrospectionAverageTime) {
    this.tokenIntrospectionAverageTime = tokenIntrospectionAverageTime;
    return this;
  }

   /**
   * The one minute average of the time required to complete a token introspection, in milliseconds (ms).
   * @return tokenIntrospectionAverageTime
  **/
  @ApiModelProperty(value = "The one minute average of the time required to complete a token introspection, in milliseconds (ms).")
  public Integer getTokenIntrospectionAverageTime() {
    return tokenIntrospectionAverageTime;
  }

  public void setTokenIntrospectionAverageTime(Integer tokenIntrospectionAverageTime) {
    this.tokenIntrospectionAverageTime = tokenIntrospectionAverageTime;
  }

  public MsgVpnAuthenticationOauthProvider tokenIntrospectionLastFailureReason(String tokenIntrospectionLastFailureReason) {
    this.tokenIntrospectionLastFailureReason = tokenIntrospectionLastFailureReason;
    return this;
  }

   /**
   * The reason for the last token introspection failure.
   * @return tokenIntrospectionLastFailureReason
  **/
  @ApiModelProperty(value = "The reason for the last token introspection failure.")
  public String getTokenIntrospectionLastFailureReason() {
    return tokenIntrospectionLastFailureReason;
  }

  public void setTokenIntrospectionLastFailureReason(String tokenIntrospectionLastFailureReason) {
    this.tokenIntrospectionLastFailureReason = tokenIntrospectionLastFailureReason;
  }

  public MsgVpnAuthenticationOauthProvider tokenIntrospectionLastFailureTime(Integer tokenIntrospectionLastFailureTime) {
    this.tokenIntrospectionLastFailureTime = tokenIntrospectionLastFailureTime;
    return this;
  }

   /**
   * The timestamp of the last token introspection failure. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return tokenIntrospectionLastFailureTime
  **/
  @ApiModelProperty(value = "The timestamp of the last token introspection failure. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getTokenIntrospectionLastFailureTime() {
    return tokenIntrospectionLastFailureTime;
  }

  public void setTokenIntrospectionLastFailureTime(Integer tokenIntrospectionLastFailureTime) {
    this.tokenIntrospectionLastFailureTime = tokenIntrospectionLastFailureTime;
  }

  public MsgVpnAuthenticationOauthProvider tokenIntrospectionParameterName(String tokenIntrospectionParameterName) {
    this.tokenIntrospectionParameterName = tokenIntrospectionParameterName;
    return this;
  }

   /**
   * The parameter name used to identify the token during access token introspection. A standards compliant OAuth introspection server expects \&quot;token\&quot;.
   * @return tokenIntrospectionParameterName
  **/
  @ApiModelProperty(value = "The parameter name used to identify the token during access token introspection. A standards compliant OAuth introspection server expects \"token\".")
  public String getTokenIntrospectionParameterName() {
    return tokenIntrospectionParameterName;
  }

  public void setTokenIntrospectionParameterName(String tokenIntrospectionParameterName) {
    this.tokenIntrospectionParameterName = tokenIntrospectionParameterName;
  }

  public MsgVpnAuthenticationOauthProvider tokenIntrospectionSuccessCount(Long tokenIntrospectionSuccessCount) {
    this.tokenIntrospectionSuccessCount = tokenIntrospectionSuccessCount;
    return this;
  }

   /**
   * The number of token introspection successes.
   * @return tokenIntrospectionSuccessCount
  **/
  @ApiModelProperty(value = "The number of token introspection successes.")
  public Long getTokenIntrospectionSuccessCount() {
    return tokenIntrospectionSuccessCount;
  }

  public void setTokenIntrospectionSuccessCount(Long tokenIntrospectionSuccessCount) {
    this.tokenIntrospectionSuccessCount = tokenIntrospectionSuccessCount;
  }

  public MsgVpnAuthenticationOauthProvider tokenIntrospectionTimeout(Integer tokenIntrospectionTimeout) {
    this.tokenIntrospectionTimeout = tokenIntrospectionTimeout;
    return this;
  }

   /**
   * The maximum time in seconds a token introspection is allowed to take.
   * @return tokenIntrospectionTimeout
  **/
  @ApiModelProperty(value = "The maximum time in seconds a token introspection is allowed to take.")
  public Integer getTokenIntrospectionTimeout() {
    return tokenIntrospectionTimeout;
  }

  public void setTokenIntrospectionTimeout(Integer tokenIntrospectionTimeout) {
    this.tokenIntrospectionTimeout = tokenIntrospectionTimeout;
  }

  public MsgVpnAuthenticationOauthProvider tokenIntrospectionUri(String tokenIntrospectionUri) {
    this.tokenIntrospectionUri = tokenIntrospectionUri;
    return this;
  }

   /**
   * The token introspection URI of the OAuth authentication server.
   * @return tokenIntrospectionUri
  **/
  @ApiModelProperty(value = "The token introspection URI of the OAuth authentication server.")
  public String getTokenIntrospectionUri() {
    return tokenIntrospectionUri;
  }

  public void setTokenIntrospectionUri(String tokenIntrospectionUri) {
    this.tokenIntrospectionUri = tokenIntrospectionUri;
  }

  public MsgVpnAuthenticationOauthProvider tokenIntrospectionUsername(String tokenIntrospectionUsername) {
    this.tokenIntrospectionUsername = tokenIntrospectionUsername;
    return this;
  }

   /**
   * The username to use when logging into the token introspection URI.
   * @return tokenIntrospectionUsername
  **/
  @ApiModelProperty(value = "The username to use when logging into the token introspection URI.")
  public String getTokenIntrospectionUsername() {
    return tokenIntrospectionUsername;
  }

  public void setTokenIntrospectionUsername(String tokenIntrospectionUsername) {
    this.tokenIntrospectionUsername = tokenIntrospectionUsername;
  }

  public MsgVpnAuthenticationOauthProvider usernameClaimName(String usernameClaimName) {
    this.usernameClaimName = usernameClaimName;
    return this;
  }

   /**
   * The username claim name, indicating which part of the object to use for determining the username.
   * @return usernameClaimName
  **/
  @ApiModelProperty(value = "The username claim name, indicating which part of the object to use for determining the username.")
  public String getUsernameClaimName() {
    return usernameClaimName;
  }

  public void setUsernameClaimName(String usernameClaimName) {
    this.usernameClaimName = usernameClaimName;
  }

  public MsgVpnAuthenticationOauthProvider usernameClaimSource(UsernameClaimSourceEnum usernameClaimSource) {
    this.usernameClaimSource = usernameClaimSource;
    return this;
  }

   /**
   * The username claim source, indicating where to search for the username value. The allowed values and their meaning are:  &lt;pre&gt; \&quot;access-token\&quot; - Search the access type JWT for the username value. \&quot;id-token\&quot; - Search the ID type JWT for the username value. \&quot;introspection\&quot; - Introspect the access token and search the result for the username value. &lt;/pre&gt; 
   * @return usernameClaimSource
  **/
  @ApiModelProperty(value = "The username claim source, indicating where to search for the username value. The allowed values and their meaning are:  <pre> \"access-token\" - Search the access type JWT for the username value. \"id-token\" - Search the ID type JWT for the username value. \"introspection\" - Introspect the access token and search the result for the username value. </pre> ")
  public UsernameClaimSourceEnum getUsernameClaimSource() {
    return usernameClaimSource;
  }

  public void setUsernameClaimSource(UsernameClaimSourceEnum usernameClaimSource) {
    this.usernameClaimSource = usernameClaimSource;
  }

  public MsgVpnAuthenticationOauthProvider usernameValidateEnabled(Boolean usernameValidateEnabled) {
    this.usernameValidateEnabled = usernameValidateEnabled;
    return this;
  }

   /**
   * Indicates whether the API provided username will be validated against the username calculated from the token(s).
   * @return usernameValidateEnabled
  **/
  @ApiModelProperty(value = "Indicates whether the API provided username will be validated against the username calculated from the token(s).")
  public Boolean isUsernameValidateEnabled() {
    return usernameValidateEnabled;
  }

  public void setUsernameValidateEnabled(Boolean usernameValidateEnabled) {
    this.usernameValidateEnabled = usernameValidateEnabled;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnAuthenticationOauthProvider msgVpnAuthenticationOauthProvider = (MsgVpnAuthenticationOauthProvider) o;
    return Objects.equals(this.audienceClaimName, msgVpnAuthenticationOauthProvider.audienceClaimName) &&
        Objects.equals(this.audienceClaimSource, msgVpnAuthenticationOauthProvider.audienceClaimSource) &&
        Objects.equals(this.audienceClaimValue, msgVpnAuthenticationOauthProvider.audienceClaimValue) &&
        Objects.equals(this.audienceValidationEnabled, msgVpnAuthenticationOauthProvider.audienceValidationEnabled) &&
        Objects.equals(this.authenticationSuccessCount, msgVpnAuthenticationOauthProvider.authenticationSuccessCount) &&
        Objects.equals(this.authorizationGroupClaimName, msgVpnAuthenticationOauthProvider.authorizationGroupClaimName) &&
        Objects.equals(this.authorizationGroupClaimSource, msgVpnAuthenticationOauthProvider.authorizationGroupClaimSource) &&
        Objects.equals(this.authorizationGroupEnabled, msgVpnAuthenticationOauthProvider.authorizationGroupEnabled) &&
        Objects.equals(this.disconnectOnTokenExpirationEnabled, msgVpnAuthenticationOauthProvider.disconnectOnTokenExpirationEnabled) &&
        Objects.equals(this.enabled, msgVpnAuthenticationOauthProvider.enabled) &&
        Objects.equals(this.jwksLastRefreshFailureReason, msgVpnAuthenticationOauthProvider.jwksLastRefreshFailureReason) &&
        Objects.equals(this.jwksLastRefreshFailureTime, msgVpnAuthenticationOauthProvider.jwksLastRefreshFailureTime) &&
        Objects.equals(this.jwksLastRefreshTime, msgVpnAuthenticationOauthProvider.jwksLastRefreshTime) &&
        Objects.equals(this.jwksNextScheduledRefreshTime, msgVpnAuthenticationOauthProvider.jwksNextScheduledRefreshTime) &&
        Objects.equals(this.jwksRefreshFailureCount, msgVpnAuthenticationOauthProvider.jwksRefreshFailureCount) &&
        Objects.equals(this.jwksRefreshInterval, msgVpnAuthenticationOauthProvider.jwksRefreshInterval) &&
        Objects.equals(this.jwksUri, msgVpnAuthenticationOauthProvider.jwksUri) &&
        Objects.equals(this.loginFailureIncorrectAudienceValueCount, msgVpnAuthenticationOauthProvider.loginFailureIncorrectAudienceValueCount) &&
        Objects.equals(this.loginFailureInvalidAudienceValueCount, msgVpnAuthenticationOauthProvider.loginFailureInvalidAudienceValueCount) &&
        Objects.equals(this.loginFailureInvalidAuthorizationGroupValueCount, msgVpnAuthenticationOauthProvider.loginFailureInvalidAuthorizationGroupValueCount) &&
        Objects.equals(this.loginFailureInvalidJwtSignatureCount, msgVpnAuthenticationOauthProvider.loginFailureInvalidJwtSignatureCount) &&
        Objects.equals(this.loginFailureInvalidUsernameValueCount, msgVpnAuthenticationOauthProvider.loginFailureInvalidUsernameValueCount) &&
        Objects.equals(this.loginFailureMismatchedUsernameCount, msgVpnAuthenticationOauthProvider.loginFailureMismatchedUsernameCount) &&
        Objects.equals(this.loginFailureMissingAudienceCount, msgVpnAuthenticationOauthProvider.loginFailureMissingAudienceCount) &&
        Objects.equals(this.loginFailureMissingJwkCount, msgVpnAuthenticationOauthProvider.loginFailureMissingJwkCount) &&
        Objects.equals(this.loginFailureMissingOrInvalidTokenCount, msgVpnAuthenticationOauthProvider.loginFailureMissingOrInvalidTokenCount) &&
        Objects.equals(this.loginFailureMissingUsernameCount, msgVpnAuthenticationOauthProvider.loginFailureMissingUsernameCount) &&
        Objects.equals(this.loginFailureTokenExpiredCount, msgVpnAuthenticationOauthProvider.loginFailureTokenExpiredCount) &&
        Objects.equals(this.loginFailureTokenIntrospectionErroredCount, msgVpnAuthenticationOauthProvider.loginFailureTokenIntrospectionErroredCount) &&
        Objects.equals(this.loginFailureTokenIntrospectionFailureCount, msgVpnAuthenticationOauthProvider.loginFailureTokenIntrospectionFailureCount) &&
        Objects.equals(this.loginFailureTokenIntrospectionHttpsErrorCount, msgVpnAuthenticationOauthProvider.loginFailureTokenIntrospectionHttpsErrorCount) &&
        Objects.equals(this.loginFailureTokenIntrospectionInvalidCount, msgVpnAuthenticationOauthProvider.loginFailureTokenIntrospectionInvalidCount) &&
        Objects.equals(this.loginFailureTokenIntrospectionTimeoutCount, msgVpnAuthenticationOauthProvider.loginFailureTokenIntrospectionTimeoutCount) &&
        Objects.equals(this.loginFailureTokenNotValidYetCount, msgVpnAuthenticationOauthProvider.loginFailureTokenNotValidYetCount) &&
        Objects.equals(this.loginFailureUnsupportedAlgCount, msgVpnAuthenticationOauthProvider.loginFailureUnsupportedAlgCount) &&
        Objects.equals(this.missingAuthorizationGroupCount, msgVpnAuthenticationOauthProvider.missingAuthorizationGroupCount) &&
        Objects.equals(this.msgVpnName, msgVpnAuthenticationOauthProvider.msgVpnName) &&
        Objects.equals(this.oauthProviderName, msgVpnAuthenticationOauthProvider.oauthProviderName) &&
        Objects.equals(this.tokenIgnoreTimeLimitsEnabled, msgVpnAuthenticationOauthProvider.tokenIgnoreTimeLimitsEnabled) &&
        Objects.equals(this.tokenIntrospectionAverageTime, msgVpnAuthenticationOauthProvider.tokenIntrospectionAverageTime) &&
        Objects.equals(this.tokenIntrospectionLastFailureReason, msgVpnAuthenticationOauthProvider.tokenIntrospectionLastFailureReason) &&
        Objects.equals(this.tokenIntrospectionLastFailureTime, msgVpnAuthenticationOauthProvider.tokenIntrospectionLastFailureTime) &&
        Objects.equals(this.tokenIntrospectionParameterName, msgVpnAuthenticationOauthProvider.tokenIntrospectionParameterName) &&
        Objects.equals(this.tokenIntrospectionSuccessCount, msgVpnAuthenticationOauthProvider.tokenIntrospectionSuccessCount) &&
        Objects.equals(this.tokenIntrospectionTimeout, msgVpnAuthenticationOauthProvider.tokenIntrospectionTimeout) &&
        Objects.equals(this.tokenIntrospectionUri, msgVpnAuthenticationOauthProvider.tokenIntrospectionUri) &&
        Objects.equals(this.tokenIntrospectionUsername, msgVpnAuthenticationOauthProvider.tokenIntrospectionUsername) &&
        Objects.equals(this.usernameClaimName, msgVpnAuthenticationOauthProvider.usernameClaimName) &&
        Objects.equals(this.usernameClaimSource, msgVpnAuthenticationOauthProvider.usernameClaimSource) &&
        Objects.equals(this.usernameValidateEnabled, msgVpnAuthenticationOauthProvider.usernameValidateEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(audienceClaimName, audienceClaimSource, audienceClaimValue, audienceValidationEnabled, authenticationSuccessCount, authorizationGroupClaimName, authorizationGroupClaimSource, authorizationGroupEnabled, disconnectOnTokenExpirationEnabled, enabled, jwksLastRefreshFailureReason, jwksLastRefreshFailureTime, jwksLastRefreshTime, jwksNextScheduledRefreshTime, jwksRefreshFailureCount, jwksRefreshInterval, jwksUri, loginFailureIncorrectAudienceValueCount, loginFailureInvalidAudienceValueCount, loginFailureInvalidAuthorizationGroupValueCount, loginFailureInvalidJwtSignatureCount, loginFailureInvalidUsernameValueCount, loginFailureMismatchedUsernameCount, loginFailureMissingAudienceCount, loginFailureMissingJwkCount, loginFailureMissingOrInvalidTokenCount, loginFailureMissingUsernameCount, loginFailureTokenExpiredCount, loginFailureTokenIntrospectionErroredCount, loginFailureTokenIntrospectionFailureCount, loginFailureTokenIntrospectionHttpsErrorCount, loginFailureTokenIntrospectionInvalidCount, loginFailureTokenIntrospectionTimeoutCount, loginFailureTokenNotValidYetCount, loginFailureUnsupportedAlgCount, missingAuthorizationGroupCount, msgVpnName, oauthProviderName, tokenIgnoreTimeLimitsEnabled, tokenIntrospectionAverageTime, tokenIntrospectionLastFailureReason, tokenIntrospectionLastFailureTime, tokenIntrospectionParameterName, tokenIntrospectionSuccessCount, tokenIntrospectionTimeout, tokenIntrospectionUri, tokenIntrospectionUsername, usernameClaimName, usernameClaimSource, usernameValidateEnabled);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnAuthenticationOauthProvider {\n");
    
    sb.append("    audienceClaimName: ").append(toIndentedString(audienceClaimName)).append("\n");
    sb.append("    audienceClaimSource: ").append(toIndentedString(audienceClaimSource)).append("\n");
    sb.append("    audienceClaimValue: ").append(toIndentedString(audienceClaimValue)).append("\n");
    sb.append("    audienceValidationEnabled: ").append(toIndentedString(audienceValidationEnabled)).append("\n");
    sb.append("    authenticationSuccessCount: ").append(toIndentedString(authenticationSuccessCount)).append("\n");
    sb.append("    authorizationGroupClaimName: ").append(toIndentedString(authorizationGroupClaimName)).append("\n");
    sb.append("    authorizationGroupClaimSource: ").append(toIndentedString(authorizationGroupClaimSource)).append("\n");
    sb.append("    authorizationGroupEnabled: ").append(toIndentedString(authorizationGroupEnabled)).append("\n");
    sb.append("    disconnectOnTokenExpirationEnabled: ").append(toIndentedString(disconnectOnTokenExpirationEnabled)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    jwksLastRefreshFailureReason: ").append(toIndentedString(jwksLastRefreshFailureReason)).append("\n");
    sb.append("    jwksLastRefreshFailureTime: ").append(toIndentedString(jwksLastRefreshFailureTime)).append("\n");
    sb.append("    jwksLastRefreshTime: ").append(toIndentedString(jwksLastRefreshTime)).append("\n");
    sb.append("    jwksNextScheduledRefreshTime: ").append(toIndentedString(jwksNextScheduledRefreshTime)).append("\n");
    sb.append("    jwksRefreshFailureCount: ").append(toIndentedString(jwksRefreshFailureCount)).append("\n");
    sb.append("    jwksRefreshInterval: ").append(toIndentedString(jwksRefreshInterval)).append("\n");
    sb.append("    jwksUri: ").append(toIndentedString(jwksUri)).append("\n");
    sb.append("    loginFailureIncorrectAudienceValueCount: ").append(toIndentedString(loginFailureIncorrectAudienceValueCount)).append("\n");
    sb.append("    loginFailureInvalidAudienceValueCount: ").append(toIndentedString(loginFailureInvalidAudienceValueCount)).append("\n");
    sb.append("    loginFailureInvalidAuthorizationGroupValueCount: ").append(toIndentedString(loginFailureInvalidAuthorizationGroupValueCount)).append("\n");
    sb.append("    loginFailureInvalidJwtSignatureCount: ").append(toIndentedString(loginFailureInvalidJwtSignatureCount)).append("\n");
    sb.append("    loginFailureInvalidUsernameValueCount: ").append(toIndentedString(loginFailureInvalidUsernameValueCount)).append("\n");
    sb.append("    loginFailureMismatchedUsernameCount: ").append(toIndentedString(loginFailureMismatchedUsernameCount)).append("\n");
    sb.append("    loginFailureMissingAudienceCount: ").append(toIndentedString(loginFailureMissingAudienceCount)).append("\n");
    sb.append("    loginFailureMissingJwkCount: ").append(toIndentedString(loginFailureMissingJwkCount)).append("\n");
    sb.append("    loginFailureMissingOrInvalidTokenCount: ").append(toIndentedString(loginFailureMissingOrInvalidTokenCount)).append("\n");
    sb.append("    loginFailureMissingUsernameCount: ").append(toIndentedString(loginFailureMissingUsernameCount)).append("\n");
    sb.append("    loginFailureTokenExpiredCount: ").append(toIndentedString(loginFailureTokenExpiredCount)).append("\n");
    sb.append("    loginFailureTokenIntrospectionErroredCount: ").append(toIndentedString(loginFailureTokenIntrospectionErroredCount)).append("\n");
    sb.append("    loginFailureTokenIntrospectionFailureCount: ").append(toIndentedString(loginFailureTokenIntrospectionFailureCount)).append("\n");
    sb.append("    loginFailureTokenIntrospectionHttpsErrorCount: ").append(toIndentedString(loginFailureTokenIntrospectionHttpsErrorCount)).append("\n");
    sb.append("    loginFailureTokenIntrospectionInvalidCount: ").append(toIndentedString(loginFailureTokenIntrospectionInvalidCount)).append("\n");
    sb.append("    loginFailureTokenIntrospectionTimeoutCount: ").append(toIndentedString(loginFailureTokenIntrospectionTimeoutCount)).append("\n");
    sb.append("    loginFailureTokenNotValidYetCount: ").append(toIndentedString(loginFailureTokenNotValidYetCount)).append("\n");
    sb.append("    loginFailureUnsupportedAlgCount: ").append(toIndentedString(loginFailureUnsupportedAlgCount)).append("\n");
    sb.append("    missingAuthorizationGroupCount: ").append(toIndentedString(missingAuthorizationGroupCount)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    oauthProviderName: ").append(toIndentedString(oauthProviderName)).append("\n");
    sb.append("    tokenIgnoreTimeLimitsEnabled: ").append(toIndentedString(tokenIgnoreTimeLimitsEnabled)).append("\n");
    sb.append("    tokenIntrospectionAverageTime: ").append(toIndentedString(tokenIntrospectionAverageTime)).append("\n");
    sb.append("    tokenIntrospectionLastFailureReason: ").append(toIndentedString(tokenIntrospectionLastFailureReason)).append("\n");
    sb.append("    tokenIntrospectionLastFailureTime: ").append(toIndentedString(tokenIntrospectionLastFailureTime)).append("\n");
    sb.append("    tokenIntrospectionParameterName: ").append(toIndentedString(tokenIntrospectionParameterName)).append("\n");
    sb.append("    tokenIntrospectionSuccessCount: ").append(toIndentedString(tokenIntrospectionSuccessCount)).append("\n");
    sb.append("    tokenIntrospectionTimeout: ").append(toIndentedString(tokenIntrospectionTimeout)).append("\n");
    sb.append("    tokenIntrospectionUri: ").append(toIndentedString(tokenIntrospectionUri)).append("\n");
    sb.append("    tokenIntrospectionUsername: ").append(toIndentedString(tokenIntrospectionUsername)).append("\n");
    sb.append("    usernameClaimName: ").append(toIndentedString(usernameClaimName)).append("\n");
    sb.append("    usernameClaimSource: ").append(toIndentedString(usernameClaimSource)).append("\n");
    sb.append("    usernameValidateEnabled: ").append(toIndentedString(usernameValidateEnabled)).append("\n");
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

