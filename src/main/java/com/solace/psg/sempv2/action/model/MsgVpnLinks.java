/*
 * SEMP (Solace Element Management Protocol)
 * SEMP (starting in `v2`, see note 1) is a RESTful API for configuring, monitoring, and administering a Solace PubSub+ broker.  SEMP uses URIs to address manageable **resources** of the Solace PubSub+ broker. Resources are individual **objects**, **collections** of objects, or (exclusively in the action API) **actions**. This document applies to the following API:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Action|/SEMP/v2/action|Performing actions|See note 2    The following APIs are also available:   API|Base Path|Purpose|Comments :---|:---|:---|:--- Configuration|/SEMP/v2/config|Reading and writing config state|See note 2 Monitoring|/SEMP/v2/monitor|Querying operational parameters|See note 2    Resources are always nouns, with individual objects being singular and collections being plural.  Objects within a collection are identified by an `obj-id`, which follows the collection name with the form `collection-name/obj-id`.  Actions within an object are identified by an `action-id`, which follows the object name with the form `obj-id/action-id`.  Some examples:  ``` /SEMP/v2/config/msgVpns                        ; MsgVpn collection /SEMP/v2/config/msgVpns/a                      ; MsgVpn object named \"a\" /SEMP/v2/config/msgVpns/a/queues               ; Queue collection in MsgVpn \"a\" /SEMP/v2/config/msgVpns/a/queues/b             ; Queue object named \"b\" in MsgVpn \"a\" /SEMP/v2/action/msgVpns/a/queues/b/startReplay ; Action that starts a replay on Queue \"b\" in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients             ; Client collection in MsgVpn \"a\" /SEMP/v2/monitor/msgVpns/a/clients/c           ; Client object named \"c\" in MsgVpn \"a\" ```  ## Collection Resources  Collections are unordered lists of objects (unless described as otherwise), and are described by JSON arrays. Each item in the array represents an object in the same manner as the individual object would normally be represented. In the configuration API, the creation of a new object is done through its collection resource.  ## Object and Action Resources  Objects are composed of attributes, actions, collections, and other objects. They are described by JSON objects as name/value pairs. The collections and actions of an object are not contained directly in the object's JSON content; rather the content includes an attribute containing a URI which points to the collections and actions. These contained resources must be managed through this URI. At a minimum, every object has one or more identifying attributes, and its own `uri` attribute which contains the URI pointing to itself.  Actions are also composed of attributes, and are described by JSON objects as name/value pairs. Unlike objects, however, they are not members of a collection and cannot be retrieved, only performed. Actions only exist in the action API.  Attributes in an object or action may have any combination of the following properties:   Property|Meaning|Comments :---|:---|:--- Identifying|Attribute is involved in unique identification of the object, and appears in its URI| Required|Attribute must be provided in the request| Read-Only|Attribute can only be read, not written.|See note 3 Write-Only|Attribute can only be written, not read, unless the attribute is also opaque|See the documentation for the opaque property Requires-Disable|Attribute can only be changed when object is disabled| Deprecated|Attribute is deprecated, and will disappear in the next SEMP version| Opaque|Attribute can be set or retrieved in opaque form when the `opaquePassword` query parameter is present|See the `opaquePassword` query parameter documentation    In some requests, certain attributes may only be provided in certain combinations with other attributes:   Relationship|Meaning :---|:--- Requires|Attribute may only be changed by a request if a particular attribute or combination of attributes is also provided in the request Conflicts|Attribute may only be provided in a request if a particular attribute or combination of attributes is not also provided in the request    In the monitoring API, any non-identifying attribute may not be returned in a GET.  ## HTTP Methods  The following HTTP methods manipulate resources in accordance with these general principles. Note that some methods are only used in certain APIs:   Method|Resource|Meaning|Request Body|Response Body|Missing Request Attributes :---|:---|:---|:---|:---|:--- POST|Collection|Create object|Initial attribute values|Object attributes and metadata|Set to default PUT|Object|Create or replace object (see note 5)|New attribute values|Object attributes and metadata|Set to default, with certain exceptions (see note 4) PUT|Action|Performs action|Action arguments|Action metadata|N/A PATCH|Object|Update object|New attribute values|Object attributes and metadata|unchanged DELETE|Object|Delete object|Empty|Object metadata|N/A GET|Object|Get object|Empty|Object attributes and metadata|N/A GET|Collection|Get collection|Empty|Object attributes and collection metadata|N/A    ## Common Query Parameters  The following are some common query parameters that are supported by many method/URI combinations. Individual URIs may document additional parameters. Note that multiple query parameters can be used together in a single URI, separated by the ampersand character. For example:  ``` ; Request for the MsgVpns collection using two hypothetical query parameters ; \"q1\" and \"q2\" with values \"val1\" and \"val2\" respectively /SEMP/v2/action/msgVpns?q1=val1&q2=val2 ```  ### select  Include in the response only selected attributes of the object, or exclude from the response selected attributes of the object. Use this query parameter to limit the size of the returned data for each returned object, return only those fields that are desired, or exclude fields that are not desired.  The value of `select` is a comma-separated list of attribute names. If the list contains attribute names that are not prefaced by `-`, only those attributes are included in the response. If the list contains attribute names that are prefaced by `-`, those attributes are excluded from the response. If the list contains both types, then the difference of the first set of attributes and the second set of attributes is returned. If the list is empty (i.e. `select=`), no attributes are returned.  All attributes that are prefaced by `-` must follow all attributes that are not prefaced by `-`. In addition, each attribute name in the list must match at least one attribute in the object.  Names may include the `*` wildcard (zero or more characters). Nested attribute names are supported using periods (e.g. `parentName.childName`).  Some examples:  ``` ; List of all MsgVpn names /SEMP/v2/action/msgVpns?select=msgVpnName ; List of all MsgVpn and their attributes except for their names /SEMP/v2/action/msgVpns?select=-msgVpnName ; Authentication attributes of MsgVpn \"finance\" /SEMP/v2/action/msgVpns/finance?select=authentication* ; All attributes of MsgVpn \"finance\" except for authentication attributes /SEMP/v2/action/msgVpns/finance?select=-authentication* ; Access related attributes of Queue \"orderQ\" of MsgVpn \"finance\" /SEMP/v2/action/msgVpns/finance/queues/orderQ?select=owner,permission ```  ### where  Include in the response only objects where certain conditions are true. Use this query parameter to limit which objects are returned to those whose attribute values meet the given conditions.  The value of `where` is a comma-separated list of expressions. All expressions must be true for the object to be included in the response. Each expression takes the form:  ``` expression  = attribute-name OP value OP          = '==' | '!=' | '&lt;' | '&gt;' | '&lt;=' | '&gt;=' ```  `value` may be a number, string, `true`, or `false`, as appropriate for the type of `attribute-name`. Greater-than and less-than comparisons only work for numbers. A `*` in a string `value` is interpreted as a wildcard (zero or more characters). Some examples:  ``` ; Only enabled MsgVpns /SEMP/v2/action/msgVpns?where=enabled==true ; Only MsgVpns using basic non-LDAP authentication /SEMP/v2/action/msgVpns?where=authenticationBasicEnabled==true,authenticationBasicType!=ldap ; Only MsgVpns that allow more than 100 client connections /SEMP/v2/action/msgVpns?where=maxConnectionCount>100 ; Only MsgVpns with msgVpnName starting with \"B\": /SEMP/v2/action/msgVpns?where=msgVpnName==B* ```  ### count  Limit the count of objects in the response. This can be useful to limit the size of the response for large collections. The minimum value for `count` is `1` and the default is `10`. There is also a per-collection maximum value to limit request handling time. For example:  ``` ; Up to 25 MsgVpns /SEMP/v2/action/msgVpns?count=25 ```  ### cursor  The cursor, or position, for the next page of objects. Cursors are opaque data that should not be created or interpreted by SEMP clients, and should only be used as described below.  When a request is made for a collection and there may be additional objects available for retrieval that are not included in the initial response, the response will include a `cursorQuery` field containing a cursor. The value of this field can be specified in the `cursor` query parameter of a subsequent request to retrieve the next page of objects. For convenience, an appropriate URI is constructed automatically by the broker and included in the `nextPageUri` field of the response. This URI can be used directly to retrieve the next page of objects.  ### opaquePassword  Attributes with the opaque property are also write-only and so cannot normally be retrieved in a GET. However, when a password is provided in the `opaquePassword` query parameter, attributes with the opaque property are retrieved in a GET in opaque form, encrypted with this password. The query parameter can also be used on a POST, PATCH, or PUT to set opaque attributes using opaque attribute values retrieved in a GET, so long as:  1. the same password that was used to retrieve the opaque attribute values is provided; and  2. the broker to which the request is being sent has the same major and minor SEMP version as the broker that produced the opaque attribute values.  The password provided in the query parameter must be a minimum of 8 characters and a maximum of 128 characters.  The query parameter can only be used in the configuration API, and only over HTTPS.  ## Help  Visit [our website](https://solace.com) to learn more about Solace.  You can also download the SEMP API specifications by clicking [here](https://solace.com/downloads/).  If you need additional support, please contact us at [support@solace.com](mailto:support@solace.com).  ## Notes  Note|Description :---:|:--- 1|This specification defines SEMP starting in \"v2\", and not the original SEMP \"v1\" interface. Request and response formats between \"v1\" and \"v2\" are entirely incompatible, although both protocols share a common port configuration on the Solace PubSub+ broker. They are differentiated by the initial portion of the URI path, one of either \"/SEMP/\" or \"/SEMP/v2/\" 2|This API is partially implemented. Only a subset of all objects are available. 3|Read-only attributes may appear in POST and PUT/PATCH requests. However, if a read-only attribute is not marked as identifying, it will be ignored during a PUT/PATCH. 4|On a PUT, if the SEMP user is not authorized to modify the attribute, its value is left unchanged rather than set to default. In addition, the values of write-only attributes are not set to their defaults on a PUT, except in the following two cases: there is a mutual requires relationship with another non-write-only attribute and both attributes are absent from the request; or the attribute is also opaque and the `opaquePassword` query parameter is provided in the request. 5|On a PUT, if the object does not exist, it is created first.  
 *
 * OpenAPI spec version: 2.17
 * Contact: support@solace.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.solace.psg.sempv2.action.model;

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
 * MsgVpnLinks
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:04:14.610964700+01:00[Europe/London]")
public class MsgVpnLinks {
  @SerializedName("authenticationOauthProvidersUri")
  private String authenticationOauthProvidersUri = null;

  @SerializedName("bridgesUri")
  private String bridgesUri = null;

  @SerializedName("clearMsgSpoolStatsUri")
  private String clearMsgSpoolStatsUri = null;

  @SerializedName("clearReplicationStatsUri")
  private String clearReplicationStatsUri = null;

  @SerializedName("clearServiceStatsUri")
  private String clearServiceStatsUri = null;

  @SerializedName("clearStatsUri")
  private String clearStatsUri = null;

  @SerializedName("clientsUri")
  private String clientsUri = null;

  @SerializedName("distributedCachesUri")
  private String distributedCachesUri = null;

  @SerializedName("mqttSessionsUri")
  private String mqttSessionsUri = null;

  @SerializedName("queuesUri")
  private String queuesUri = null;

  @SerializedName("replayLogsUri")
  private String replayLogsUri = null;

  @SerializedName("restDeliveryPointsUri")
  private String restDeliveryPointsUri = null;

  @SerializedName("topicEndpointsUri")
  private String topicEndpointsUri = null;

  @SerializedName("transactionsUri")
  private String transactionsUri = null;

  @SerializedName("uri")
  private String uri = null;

  public MsgVpnLinks authenticationOauthProvidersUri(String authenticationOauthProvidersUri) {
    this.authenticationOauthProvidersUri = authenticationOauthProvidersUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s collection of OAuth Provider objects. Available since 2.13.
   * @return authenticationOauthProvidersUri
  **/
  @Schema(description = "The URI of this Message VPN's collection of OAuth Provider objects. Available since 2.13.")
  public String getAuthenticationOauthProvidersUri() {
    return authenticationOauthProvidersUri;
  }

  public void setAuthenticationOauthProvidersUri(String authenticationOauthProvidersUri) {
    this.authenticationOauthProvidersUri = authenticationOauthProvidersUri;
  }

  public MsgVpnLinks bridgesUri(String bridgesUri) {
    this.bridgesUri = bridgesUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s collection of Bridge objects.
   * @return bridgesUri
  **/
  @Schema(description = "The URI of this Message VPN's collection of Bridge objects.")
  public String getBridgesUri() {
    return bridgesUri;
  }

  public void setBridgesUri(String bridgesUri) {
    this.bridgesUri = bridgesUri;
  }

  public MsgVpnLinks clearMsgSpoolStatsUri(String clearMsgSpoolStatsUri) {
    this.clearMsgSpoolStatsUri = clearMsgSpoolStatsUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s Clear Stats action.
   * @return clearMsgSpoolStatsUri
  **/
  @Schema(description = "The URI of this Message VPN's Clear Stats action.")
  public String getClearMsgSpoolStatsUri() {
    return clearMsgSpoolStatsUri;
  }

  public void setClearMsgSpoolStatsUri(String clearMsgSpoolStatsUri) {
    this.clearMsgSpoolStatsUri = clearMsgSpoolStatsUri;
  }

  public MsgVpnLinks clearReplicationStatsUri(String clearReplicationStatsUri) {
    this.clearReplicationStatsUri = clearReplicationStatsUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s Clear Stats action.
   * @return clearReplicationStatsUri
  **/
  @Schema(description = "The URI of this Message VPN's Clear Stats action.")
  public String getClearReplicationStatsUri() {
    return clearReplicationStatsUri;
  }

  public void setClearReplicationStatsUri(String clearReplicationStatsUri) {
    this.clearReplicationStatsUri = clearReplicationStatsUri;
  }

  public MsgVpnLinks clearServiceStatsUri(String clearServiceStatsUri) {
    this.clearServiceStatsUri = clearServiceStatsUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s Clear Stats action.
   * @return clearServiceStatsUri
  **/
  @Schema(description = "The URI of this Message VPN's Clear Stats action.")
  public String getClearServiceStatsUri() {
    return clearServiceStatsUri;
  }

  public void setClearServiceStatsUri(String clearServiceStatsUri) {
    this.clearServiceStatsUri = clearServiceStatsUri;
  }

  public MsgVpnLinks clearStatsUri(String clearStatsUri) {
    this.clearStatsUri = clearStatsUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s Clear Stats action.
   * @return clearStatsUri
  **/
  @Schema(description = "The URI of this Message VPN's Clear Stats action.")
  public String getClearStatsUri() {
    return clearStatsUri;
  }

  public void setClearStatsUri(String clearStatsUri) {
    this.clearStatsUri = clearStatsUri;
  }

  public MsgVpnLinks clientsUri(String clientsUri) {
    this.clientsUri = clientsUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s collection of Client objects.
   * @return clientsUri
  **/
  @Schema(description = "The URI of this Message VPN's collection of Client objects.")
  public String getClientsUri() {
    return clientsUri;
  }

  public void setClientsUri(String clientsUri) {
    this.clientsUri = clientsUri;
  }

  public MsgVpnLinks distributedCachesUri(String distributedCachesUri) {
    this.distributedCachesUri = distributedCachesUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s collection of Distributed Cache objects.
   * @return distributedCachesUri
  **/
  @Schema(description = "The URI of this Message VPN's collection of Distributed Cache objects.")
  public String getDistributedCachesUri() {
    return distributedCachesUri;
  }

  public void setDistributedCachesUri(String distributedCachesUri) {
    this.distributedCachesUri = distributedCachesUri;
  }

  public MsgVpnLinks mqttSessionsUri(String mqttSessionsUri) {
    this.mqttSessionsUri = mqttSessionsUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s collection of MQTT Session objects.
   * @return mqttSessionsUri
  **/
  @Schema(description = "The URI of this Message VPN's collection of MQTT Session objects.")
  public String getMqttSessionsUri() {
    return mqttSessionsUri;
  }

  public void setMqttSessionsUri(String mqttSessionsUri) {
    this.mqttSessionsUri = mqttSessionsUri;
  }

  public MsgVpnLinks queuesUri(String queuesUri) {
    this.queuesUri = queuesUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s collection of Queue objects.
   * @return queuesUri
  **/
  @Schema(description = "The URI of this Message VPN's collection of Queue objects.")
  public String getQueuesUri() {
    return queuesUri;
  }

  public void setQueuesUri(String queuesUri) {
    this.queuesUri = queuesUri;
  }

  public MsgVpnLinks replayLogsUri(String replayLogsUri) {
    this.replayLogsUri = replayLogsUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s collection of Replay Log objects.
   * @return replayLogsUri
  **/
  @Schema(description = "The URI of this Message VPN's collection of Replay Log objects.")
  public String getReplayLogsUri() {
    return replayLogsUri;
  }

  public void setReplayLogsUri(String replayLogsUri) {
    this.replayLogsUri = replayLogsUri;
  }

  public MsgVpnLinks restDeliveryPointsUri(String restDeliveryPointsUri) {
    this.restDeliveryPointsUri = restDeliveryPointsUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s collection of REST Delivery Point objects.
   * @return restDeliveryPointsUri
  **/
  @Schema(description = "The URI of this Message VPN's collection of REST Delivery Point objects.")
  public String getRestDeliveryPointsUri() {
    return restDeliveryPointsUri;
  }

  public void setRestDeliveryPointsUri(String restDeliveryPointsUri) {
    this.restDeliveryPointsUri = restDeliveryPointsUri;
  }

  public MsgVpnLinks topicEndpointsUri(String topicEndpointsUri) {
    this.topicEndpointsUri = topicEndpointsUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s collection of Topic Endpoint objects.
   * @return topicEndpointsUri
  **/
  @Schema(description = "The URI of this Message VPN's collection of Topic Endpoint objects.")
  public String getTopicEndpointsUri() {
    return topicEndpointsUri;
  }

  public void setTopicEndpointsUri(String topicEndpointsUri) {
    this.topicEndpointsUri = topicEndpointsUri;
  }

  public MsgVpnLinks transactionsUri(String transactionsUri) {
    this.transactionsUri = transactionsUri;
    return this;
  }

   /**
   * The URI of this Message VPN&#x27;s collection of Replicated Local Transaction or XA Transaction objects. Available since 2.12.
   * @return transactionsUri
  **/
  @Schema(description = "The URI of this Message VPN's collection of Replicated Local Transaction or XA Transaction objects. Available since 2.12.")
  public String getTransactionsUri() {
    return transactionsUri;
  }

  public void setTransactionsUri(String transactionsUri) {
    this.transactionsUri = transactionsUri;
  }

  public MsgVpnLinks uri(String uri) {
    this.uri = uri;
    return this;
  }

   /**
   * The URI of this Message VPN object.
   * @return uri
  **/
  @Schema(description = "The URI of this Message VPN object.")
  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnLinks msgVpnLinks = (MsgVpnLinks) o;
    return Objects.equals(this.authenticationOauthProvidersUri, msgVpnLinks.authenticationOauthProvidersUri) &&
        Objects.equals(this.bridgesUri, msgVpnLinks.bridgesUri) &&
        Objects.equals(this.clearMsgSpoolStatsUri, msgVpnLinks.clearMsgSpoolStatsUri) &&
        Objects.equals(this.clearReplicationStatsUri, msgVpnLinks.clearReplicationStatsUri) &&
        Objects.equals(this.clearServiceStatsUri, msgVpnLinks.clearServiceStatsUri) &&
        Objects.equals(this.clearStatsUri, msgVpnLinks.clearStatsUri) &&
        Objects.equals(this.clientsUri, msgVpnLinks.clientsUri) &&
        Objects.equals(this.distributedCachesUri, msgVpnLinks.distributedCachesUri) &&
        Objects.equals(this.mqttSessionsUri, msgVpnLinks.mqttSessionsUri) &&
        Objects.equals(this.queuesUri, msgVpnLinks.queuesUri) &&
        Objects.equals(this.replayLogsUri, msgVpnLinks.replayLogsUri) &&
        Objects.equals(this.restDeliveryPointsUri, msgVpnLinks.restDeliveryPointsUri) &&
        Objects.equals(this.topicEndpointsUri, msgVpnLinks.topicEndpointsUri) &&
        Objects.equals(this.transactionsUri, msgVpnLinks.transactionsUri) &&
        Objects.equals(this.uri, msgVpnLinks.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authenticationOauthProvidersUri, bridgesUri, clearMsgSpoolStatsUri, clearReplicationStatsUri, clearServiceStatsUri, clearStatsUri, clientsUri, distributedCachesUri, mqttSessionsUri, queuesUri, replayLogsUri, restDeliveryPointsUri, topicEndpointsUri, transactionsUri, uri);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnLinks {\n");
    
    sb.append("    authenticationOauthProvidersUri: ").append(toIndentedString(authenticationOauthProvidersUri)).append("\n");
    sb.append("    bridgesUri: ").append(toIndentedString(bridgesUri)).append("\n");
    sb.append("    clearMsgSpoolStatsUri: ").append(toIndentedString(clearMsgSpoolStatsUri)).append("\n");
    sb.append("    clearReplicationStatsUri: ").append(toIndentedString(clearReplicationStatsUri)).append("\n");
    sb.append("    clearServiceStatsUri: ").append(toIndentedString(clearServiceStatsUri)).append("\n");
    sb.append("    clearStatsUri: ").append(toIndentedString(clearStatsUri)).append("\n");
    sb.append("    clientsUri: ").append(toIndentedString(clientsUri)).append("\n");
    sb.append("    distributedCachesUri: ").append(toIndentedString(distributedCachesUri)).append("\n");
    sb.append("    mqttSessionsUri: ").append(toIndentedString(mqttSessionsUri)).append("\n");
    sb.append("    queuesUri: ").append(toIndentedString(queuesUri)).append("\n");
    sb.append("    replayLogsUri: ").append(toIndentedString(replayLogsUri)).append("\n");
    sb.append("    restDeliveryPointsUri: ").append(toIndentedString(restDeliveryPointsUri)).append("\n");
    sb.append("    topicEndpointsUri: ").append(toIndentedString(topicEndpointsUri)).append("\n");
    sb.append("    transactionsUri: ").append(toIndentedString(transactionsUri)).append("\n");
    sb.append("    uri: ").append(toIndentedString(uri)).append("\n");
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
