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
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * CertAuthority
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:57:21.016551900+01:00[Europe/London]")
public class CertAuthority {
  @SerializedName("certAuthorityName")
  private String certAuthorityName = null;

  @SerializedName("certContent")
  private String certContent = null;

  @SerializedName("crlDayList")
  private String crlDayList = null;

  @SerializedName("crlLastDownloadTime")
  private Integer crlLastDownloadTime = null;

  @SerializedName("crlLastFailureReason")
  private String crlLastFailureReason = null;

  @SerializedName("crlLastFailureTime")
  private Integer crlLastFailureTime = null;

  @SerializedName("crlNextDownloadTime")
  private Integer crlNextDownloadTime = null;

  @SerializedName("crlTimeList")
  private String crlTimeList = null;

  @SerializedName("crlUp")
  private Boolean crlUp = null;

  @SerializedName("crlUrl")
  private String crlUrl = null;

  @SerializedName("ocspLastFailureReason")
  private String ocspLastFailureReason = null;

  @SerializedName("ocspLastFailureTime")
  private Integer ocspLastFailureTime = null;

  @SerializedName("ocspLastFailureUrl")
  private String ocspLastFailureUrl = null;

  @SerializedName("ocspNonResponderCertEnabled")
  private Boolean ocspNonResponderCertEnabled = null;

  @SerializedName("ocspOverrideUrl")
  private String ocspOverrideUrl = null;

  @SerializedName("ocspTimeout")
  private Long ocspTimeout = null;

  @SerializedName("revocationCheckEnabled")
  private Boolean revocationCheckEnabled = null;

  public CertAuthority certAuthorityName(String certAuthorityName) {
    this.certAuthorityName = certAuthorityName;
    return this;
  }

   /**
   * The name of the Certificate Authority.
   * @return certAuthorityName
  **/
  @Schema(description = "The name of the Certificate Authority.")
  public String getCertAuthorityName() {
    return certAuthorityName;
  }

  public void setCertAuthorityName(String certAuthorityName) {
    this.certAuthorityName = certAuthorityName;
  }

  public CertAuthority certContent(String certContent) {
    this.certContent = certContent;
    return this;
  }

   /**
   * The PEM formatted content for the trusted root certificate of a Certificate Authority. Available since 2.17.
   * @return certContent
  **/
  @Schema(description = "The PEM formatted content for the trusted root certificate of a Certificate Authority. Available since 2.17.")
  public String getCertContent() {
    return certContent;
  }

  public void setCertContent(String certContent) {
    this.certContent = certContent;
  }

  public CertAuthority crlDayList(String crlDayList) {
    this.crlDayList = crlDayList;
    return this;
  }

   /**
   * The scheduled CRL refresh day(s), specified as \&quot;daily\&quot; or a comma-separated list of days. Days must be specified as \&quot;Sun\&quot;, \&quot;Mon\&quot;, \&quot;Tue\&quot;, \&quot;Wed\&quot;, \&quot;Thu\&quot;, \&quot;Fri\&quot;, or \&quot;Sat\&quot;, with no spaces, and in sorted order from Sunday to Saturday.
   * @return crlDayList
  **/
  @Schema(description = "The scheduled CRL refresh day(s), specified as \"daily\" or a comma-separated list of days. Days must be specified as \"Sun\", \"Mon\", \"Tue\", \"Wed\", \"Thu\", \"Fri\", or \"Sat\", with no spaces, and in sorted order from Sunday to Saturday.")
  public String getCrlDayList() {
    return crlDayList;
  }

  public void setCrlDayList(String crlDayList) {
    this.crlDayList = crlDayList;
  }

  public CertAuthority crlLastDownloadTime(Integer crlLastDownloadTime) {
    this.crlLastDownloadTime = crlLastDownloadTime;
    return this;
  }

   /**
   * The timestamp of the last successful CRL download. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return crlLastDownloadTime
  **/
  @Schema(description = "The timestamp of the last successful CRL download. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getCrlLastDownloadTime() {
    return crlLastDownloadTime;
  }

  public void setCrlLastDownloadTime(Integer crlLastDownloadTime) {
    this.crlLastDownloadTime = crlLastDownloadTime;
  }

  public CertAuthority crlLastFailureReason(String crlLastFailureReason) {
    this.crlLastFailureReason = crlLastFailureReason;
    return this;
  }

   /**
   * The reason for the last CRL failure.
   * @return crlLastFailureReason
  **/
  @Schema(description = "The reason for the last CRL failure.")
  public String getCrlLastFailureReason() {
    return crlLastFailureReason;
  }

  public void setCrlLastFailureReason(String crlLastFailureReason) {
    this.crlLastFailureReason = crlLastFailureReason;
  }

  public CertAuthority crlLastFailureTime(Integer crlLastFailureTime) {
    this.crlLastFailureTime = crlLastFailureTime;
    return this;
  }

   /**
   * The timestamp of the last CRL failure. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return crlLastFailureTime
  **/
  @Schema(description = "The timestamp of the last CRL failure. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getCrlLastFailureTime() {
    return crlLastFailureTime;
  }

  public void setCrlLastFailureTime(Integer crlLastFailureTime) {
    this.crlLastFailureTime = crlLastFailureTime;
  }

  public CertAuthority crlNextDownloadTime(Integer crlNextDownloadTime) {
    this.crlNextDownloadTime = crlNextDownloadTime;
    return this;
  }

   /**
   * The scheduled time of the next CRL download. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return crlNextDownloadTime
  **/
  @Schema(description = "The scheduled time of the next CRL download. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getCrlNextDownloadTime() {
    return crlNextDownloadTime;
  }

  public void setCrlNextDownloadTime(Integer crlNextDownloadTime) {
    this.crlNextDownloadTime = crlNextDownloadTime;
  }

  public CertAuthority crlTimeList(String crlTimeList) {
    this.crlTimeList = crlTimeList;
    return this;
  }

   /**
   * The scheduled CRL refresh time(s), specified as \&quot;hourly\&quot; or a comma-separated list of 24-hour times in the form hh:mm, or h:mm. There must be no spaces, and times must be in sorted order from 0:00 to 23:59.
   * @return crlTimeList
  **/
  @Schema(description = "The scheduled CRL refresh time(s), specified as \"hourly\" or a comma-separated list of 24-hour times in the form hh:mm, or h:mm. There must be no spaces, and times must be in sorted order from 0:00 to 23:59.")
  public String getCrlTimeList() {
    return crlTimeList;
  }

  public void setCrlTimeList(String crlTimeList) {
    this.crlTimeList = crlTimeList;
  }

  public CertAuthority crlUp(Boolean crlUp) {
    this.crlUp = crlUp;
    return this;
  }

   /**
   * Indicates whether CRL revocation checking is operationally up.
   * @return crlUp
  **/
  @Schema(description = "Indicates whether CRL revocation checking is operationally up.")
  public Boolean isCrlUp() {
    return crlUp;
  }

  public void setCrlUp(Boolean crlUp) {
    this.crlUp = crlUp;
  }

  public CertAuthority crlUrl(String crlUrl) {
    this.crlUrl = crlUrl;
    return this;
  }

   /**
   * The URL for the CRL source. This is a required attribute for CRL to be operational and the URL must be complete with http:// included.
   * @return crlUrl
  **/
  @Schema(description = "The URL for the CRL source. This is a required attribute for CRL to be operational and the URL must be complete with http:// included.")
  public String getCrlUrl() {
    return crlUrl;
  }

  public void setCrlUrl(String crlUrl) {
    this.crlUrl = crlUrl;
  }

  public CertAuthority ocspLastFailureReason(String ocspLastFailureReason) {
    this.ocspLastFailureReason = ocspLastFailureReason;
    return this;
  }

   /**
   * The reason for the last OCSP failure.
   * @return ocspLastFailureReason
  **/
  @Schema(description = "The reason for the last OCSP failure.")
  public String getOcspLastFailureReason() {
    return ocspLastFailureReason;
  }

  public void setOcspLastFailureReason(String ocspLastFailureReason) {
    this.ocspLastFailureReason = ocspLastFailureReason;
  }

  public CertAuthority ocspLastFailureTime(Integer ocspLastFailureTime) {
    this.ocspLastFailureTime = ocspLastFailureTime;
    return this;
  }

   /**
   * The timestamp of the last OCSP failure. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return ocspLastFailureTime
  **/
  @Schema(description = "The timestamp of the last OCSP failure. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getOcspLastFailureTime() {
    return ocspLastFailureTime;
  }

  public void setOcspLastFailureTime(Integer ocspLastFailureTime) {
    this.ocspLastFailureTime = ocspLastFailureTime;
  }

  public CertAuthority ocspLastFailureUrl(String ocspLastFailureUrl) {
    this.ocspLastFailureUrl = ocspLastFailureUrl;
    return this;
  }

   /**
   * The URL involved in the last OCSP failure.
   * @return ocspLastFailureUrl
  **/
  @Schema(description = "The URL involved in the last OCSP failure.")
  public String getOcspLastFailureUrl() {
    return ocspLastFailureUrl;
  }

  public void setOcspLastFailureUrl(String ocspLastFailureUrl) {
    this.ocspLastFailureUrl = ocspLastFailureUrl;
  }

  public CertAuthority ocspNonResponderCertEnabled(Boolean ocspNonResponderCertEnabled) {
    this.ocspNonResponderCertEnabled = ocspNonResponderCertEnabled;
    return this;
  }

   /**
   * Indicates whether a non-responder certificate is allowed to sign an OCSP response. Typically used with an OCSP override URL in cases where a single certificate is used to sign client certificates and OCSP responses.
   * @return ocspNonResponderCertEnabled
  **/
  @Schema(description = "Indicates whether a non-responder certificate is allowed to sign an OCSP response. Typically used with an OCSP override URL in cases where a single certificate is used to sign client certificates and OCSP responses.")
  public Boolean isOcspNonResponderCertEnabled() {
    return ocspNonResponderCertEnabled;
  }

  public void setOcspNonResponderCertEnabled(Boolean ocspNonResponderCertEnabled) {
    this.ocspNonResponderCertEnabled = ocspNonResponderCertEnabled;
  }

  public CertAuthority ocspOverrideUrl(String ocspOverrideUrl) {
    this.ocspOverrideUrl = ocspOverrideUrl;
    return this;
  }

   /**
   * The OCSP responder URL to use for overriding the one supplied in the client certificate. The URL must be complete with http:// included.
   * @return ocspOverrideUrl
  **/
  @Schema(description = "The OCSP responder URL to use for overriding the one supplied in the client certificate. The URL must be complete with http:// included.")
  public String getOcspOverrideUrl() {
    return ocspOverrideUrl;
  }

  public void setOcspOverrideUrl(String ocspOverrideUrl) {
    this.ocspOverrideUrl = ocspOverrideUrl;
  }

  public CertAuthority ocspTimeout(Long ocspTimeout) {
    this.ocspTimeout = ocspTimeout;
    return this;
  }

   /**
   * The timeout in seconds to receive a response from the OCSP responder after sending a request or making the initial connection attempt.
   * @return ocspTimeout
  **/
  @Schema(description = "The timeout in seconds to receive a response from the OCSP responder after sending a request or making the initial connection attempt.")
  public Long getOcspTimeout() {
    return ocspTimeout;
  }

  public void setOcspTimeout(Long ocspTimeout) {
    this.ocspTimeout = ocspTimeout;
  }

  public CertAuthority revocationCheckEnabled(Boolean revocationCheckEnabled) {
    this.revocationCheckEnabled = revocationCheckEnabled;
    return this;
  }

   /**
   * Indicates whether Certificate Authority revocation checking is enabled.
   * @return revocationCheckEnabled
  **/
  @Schema(description = "Indicates whether Certificate Authority revocation checking is enabled.")
  public Boolean isRevocationCheckEnabled() {
    return revocationCheckEnabled;
  }

  public void setRevocationCheckEnabled(Boolean revocationCheckEnabled) {
    this.revocationCheckEnabled = revocationCheckEnabled;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CertAuthority certAuthority = (CertAuthority) o;
    return Objects.equals(this.certAuthorityName, certAuthority.certAuthorityName) &&
        Objects.equals(this.certContent, certAuthority.certContent) &&
        Objects.equals(this.crlDayList, certAuthority.crlDayList) &&
        Objects.equals(this.crlLastDownloadTime, certAuthority.crlLastDownloadTime) &&
        Objects.equals(this.crlLastFailureReason, certAuthority.crlLastFailureReason) &&
        Objects.equals(this.crlLastFailureTime, certAuthority.crlLastFailureTime) &&
        Objects.equals(this.crlNextDownloadTime, certAuthority.crlNextDownloadTime) &&
        Objects.equals(this.crlTimeList, certAuthority.crlTimeList) &&
        Objects.equals(this.crlUp, certAuthority.crlUp) &&
        Objects.equals(this.crlUrl, certAuthority.crlUrl) &&
        Objects.equals(this.ocspLastFailureReason, certAuthority.ocspLastFailureReason) &&
        Objects.equals(this.ocspLastFailureTime, certAuthority.ocspLastFailureTime) &&
        Objects.equals(this.ocspLastFailureUrl, certAuthority.ocspLastFailureUrl) &&
        Objects.equals(this.ocspNonResponderCertEnabled, certAuthority.ocspNonResponderCertEnabled) &&
        Objects.equals(this.ocspOverrideUrl, certAuthority.ocspOverrideUrl) &&
        Objects.equals(this.ocspTimeout, certAuthority.ocspTimeout) &&
        Objects.equals(this.revocationCheckEnabled, certAuthority.revocationCheckEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(certAuthorityName, certContent, crlDayList, crlLastDownloadTime, crlLastFailureReason, crlLastFailureTime, crlNextDownloadTime, crlTimeList, crlUp, crlUrl, ocspLastFailureReason, ocspLastFailureTime, ocspLastFailureUrl, ocspNonResponderCertEnabled, ocspOverrideUrl, ocspTimeout, revocationCheckEnabled);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CertAuthority {\n");
    
    sb.append("    certAuthorityName: ").append(toIndentedString(certAuthorityName)).append("\n");
    sb.append("    certContent: ").append(toIndentedString(certContent)).append("\n");
    sb.append("    crlDayList: ").append(toIndentedString(crlDayList)).append("\n");
    sb.append("    crlLastDownloadTime: ").append(toIndentedString(crlLastDownloadTime)).append("\n");
    sb.append("    crlLastFailureReason: ").append(toIndentedString(crlLastFailureReason)).append("\n");
    sb.append("    crlLastFailureTime: ").append(toIndentedString(crlLastFailureTime)).append("\n");
    sb.append("    crlNextDownloadTime: ").append(toIndentedString(crlNextDownloadTime)).append("\n");
    sb.append("    crlTimeList: ").append(toIndentedString(crlTimeList)).append("\n");
    sb.append("    crlUp: ").append(toIndentedString(crlUp)).append("\n");
    sb.append("    crlUrl: ").append(toIndentedString(crlUrl)).append("\n");
    sb.append("    ocspLastFailureReason: ").append(toIndentedString(ocspLastFailureReason)).append("\n");
    sb.append("    ocspLastFailureTime: ").append(toIndentedString(ocspLastFailureTime)).append("\n");
    sb.append("    ocspLastFailureUrl: ").append(toIndentedString(ocspLastFailureUrl)).append("\n");
    sb.append("    ocspNonResponderCertEnabled: ").append(toIndentedString(ocspNonResponderCertEnabled)).append("\n");
    sb.append("    ocspOverrideUrl: ").append(toIndentedString(ocspOverrideUrl)).append("\n");
    sb.append("    ocspTimeout: ").append(toIndentedString(ocspTimeout)).append("\n");
    sb.append("    revocationCheckEnabled: ").append(toIndentedString(revocationCheckEnabled)).append("\n");
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
