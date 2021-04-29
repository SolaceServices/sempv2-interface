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
import com.solace.psg.sempv2.monitor.model.MsgVpnDistributedCacheClusterInstanceCounter;
import com.solace.psg.sempv2.monitor.model.MsgVpnDistributedCacheClusterInstanceRate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * MsgVpnDistributedCacheClusterInstance
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:57:21.016551900+01:00[Europe/London]")
public class MsgVpnDistributedCacheClusterInstance {
  @SerializedName("autoStartEnabled")
  private Boolean autoStartEnabled = null;

  @SerializedName("averageDataRxBytePeakRate")
  private Long averageDataRxBytePeakRate = null;

  @SerializedName("averageDataRxByteRate")
  private Long averageDataRxByteRate = null;

  @SerializedName("averageDataRxMsgPeakRate")
  private Long averageDataRxMsgPeakRate = null;

  @SerializedName("averageDataRxMsgRate")
  private Long averageDataRxMsgRate = null;

  @SerializedName("averageDataTxMsgPeakRate")
  private Long averageDataTxMsgPeakRate = null;

  @SerializedName("averageDataTxMsgRate")
  private Long averageDataTxMsgRate = null;

  @SerializedName("averageRequestRxPeakRate")
  private Long averageRequestRxPeakRate = null;

  @SerializedName("averageRequestRxRate")
  private Long averageRequestRxRate = null;

  @SerializedName("cacheName")
  private String cacheName = null;

  @SerializedName("clusterName")
  private String clusterName = null;

  @SerializedName("counter")
  private MsgVpnDistributedCacheClusterInstanceCounter counter = null;

  @SerializedName("dataRxBytePeakRate")
  private Long dataRxBytePeakRate = null;

  @SerializedName("dataRxByteRate")
  private Long dataRxByteRate = null;

  @SerializedName("dataRxMsgPeakRate")
  private Long dataRxMsgPeakRate = null;

  @SerializedName("dataRxMsgRate")
  private Long dataRxMsgRate = null;

  @SerializedName("dataTxMsgPeakRate")
  private Long dataTxMsgPeakRate = null;

  @SerializedName("dataTxMsgRate")
  private Long dataTxMsgRate = null;

  @SerializedName("enabled")
  private Boolean enabled = null;

  @SerializedName("instanceName")
  private String instanceName = null;

  @SerializedName("lastRegisteredTime")
  private Integer lastRegisteredTime = null;

  @SerializedName("lastRxHeartbeatTime")
  private Integer lastRxHeartbeatTime = null;

  @SerializedName("lastRxSetLostMsgTime")
  private Integer lastRxSetLostMsgTime = null;

  @SerializedName("lastStoppedReason")
  private String lastStoppedReason = null;

  @SerializedName("lastStoppedTime")
  private Integer lastStoppedTime = null;

  @SerializedName("lastTxClearLostMsgTime")
  private Integer lastTxClearLostMsgTime = null;

  @SerializedName("memoryUsage")
  private Integer memoryUsage = null;

  @SerializedName("msgCount")
  private Long msgCount = null;

  @SerializedName("msgPeakCount")
  private Long msgPeakCount = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("msgsLost")
  private Boolean msgsLost = null;

  @SerializedName("rate")
  private MsgVpnDistributedCacheClusterInstanceRate rate = null;

  @SerializedName("requestQueueDepthCount")
  private Long requestQueueDepthCount = null;

  @SerializedName("requestQueueDepthPeakCount")
  private Long requestQueueDepthPeakCount = null;

  @SerializedName("requestRxPeakRate")
  private Long requestRxPeakRate = null;

  @SerializedName("requestRxRate")
  private Long requestRxRate = null;

  @SerializedName("state")
  private String state = null;

  @SerializedName("stopOnLostMsgEnabled")
  private Boolean stopOnLostMsgEnabled = null;

  @SerializedName("topicCount")
  private Long topicCount = null;

  @SerializedName("topicPeakCount")
  private Long topicPeakCount = null;

  public MsgVpnDistributedCacheClusterInstance autoStartEnabled(Boolean autoStartEnabled) {
    this.autoStartEnabled = autoStartEnabled;
    return this;
  }

   /**
   * Indicates whether auto-start for the Cache Instance is enabled, and the Cache Instance will automatically attempt to transition from the Stopped operational state to Up whenever it restarts or reconnects to the message broker.
   * @return autoStartEnabled
  **/
  @Schema(description = "Indicates whether auto-start for the Cache Instance is enabled, and the Cache Instance will automatically attempt to transition from the Stopped operational state to Up whenever it restarts or reconnects to the message broker.")
  public Boolean isAutoStartEnabled() {
    return autoStartEnabled;
  }

  public void setAutoStartEnabled(Boolean autoStartEnabled) {
    this.autoStartEnabled = autoStartEnabled;
  }

  public MsgVpnDistributedCacheClusterInstance averageDataRxBytePeakRate(Long averageDataRxBytePeakRate) {
    this.averageDataRxBytePeakRate = averageDataRxBytePeakRate;
    return this;
  }

   /**
   * The peak of the one minute average of the data message rate received by the Cache Instance, in bytes per second (B/sec). Available since 2.13.
   * @return averageDataRxBytePeakRate
  **/
  @Schema(description = "The peak of the one minute average of the data message rate received by the Cache Instance, in bytes per second (B/sec). Available since 2.13.")
  public Long getAverageDataRxBytePeakRate() {
    return averageDataRxBytePeakRate;
  }

  public void setAverageDataRxBytePeakRate(Long averageDataRxBytePeakRate) {
    this.averageDataRxBytePeakRate = averageDataRxBytePeakRate;
  }

  public MsgVpnDistributedCacheClusterInstance averageDataRxByteRate(Long averageDataRxByteRate) {
    this.averageDataRxByteRate = averageDataRxByteRate;
    return this;
  }

   /**
   * The one minute average of the data message rate received by the Cache Instance, in bytes per second (B/sec). Available since 2.13.
   * @return averageDataRxByteRate
  **/
  @Schema(description = "The one minute average of the data message rate received by the Cache Instance, in bytes per second (B/sec). Available since 2.13.")
  public Long getAverageDataRxByteRate() {
    return averageDataRxByteRate;
  }

  public void setAverageDataRxByteRate(Long averageDataRxByteRate) {
    this.averageDataRxByteRate = averageDataRxByteRate;
  }

  public MsgVpnDistributedCacheClusterInstance averageDataRxMsgPeakRate(Long averageDataRxMsgPeakRate) {
    this.averageDataRxMsgPeakRate = averageDataRxMsgPeakRate;
    return this;
  }

   /**
   * The peak of the one minute average of the data message rate received by the Cache Instance, in messages per second (msg/sec). Available since 2.13.
   * @return averageDataRxMsgPeakRate
  **/
  @Schema(description = "The peak of the one minute average of the data message rate received by the Cache Instance, in messages per second (msg/sec). Available since 2.13.")
  public Long getAverageDataRxMsgPeakRate() {
    return averageDataRxMsgPeakRate;
  }

  public void setAverageDataRxMsgPeakRate(Long averageDataRxMsgPeakRate) {
    this.averageDataRxMsgPeakRate = averageDataRxMsgPeakRate;
  }

  public MsgVpnDistributedCacheClusterInstance averageDataRxMsgRate(Long averageDataRxMsgRate) {
    this.averageDataRxMsgRate = averageDataRxMsgRate;
    return this;
  }

   /**
   * The one minute average of the data message rate received by the Cache Instance, in messages per second (msg/sec). Available since 2.13.
   * @return averageDataRxMsgRate
  **/
  @Schema(description = "The one minute average of the data message rate received by the Cache Instance, in messages per second (msg/sec). Available since 2.13.")
  public Long getAverageDataRxMsgRate() {
    return averageDataRxMsgRate;
  }

  public void setAverageDataRxMsgRate(Long averageDataRxMsgRate) {
    this.averageDataRxMsgRate = averageDataRxMsgRate;
  }

  public MsgVpnDistributedCacheClusterInstance averageDataTxMsgPeakRate(Long averageDataTxMsgPeakRate) {
    this.averageDataTxMsgPeakRate = averageDataTxMsgPeakRate;
    return this;
  }

   /**
   * The peak of the one minute average of the data message rate transmitted by the Cache Instance, in messages per second (msg/sec). Available since 2.13.
   * @return averageDataTxMsgPeakRate
  **/
  @Schema(description = "The peak of the one minute average of the data message rate transmitted by the Cache Instance, in messages per second (msg/sec). Available since 2.13.")
  public Long getAverageDataTxMsgPeakRate() {
    return averageDataTxMsgPeakRate;
  }

  public void setAverageDataTxMsgPeakRate(Long averageDataTxMsgPeakRate) {
    this.averageDataTxMsgPeakRate = averageDataTxMsgPeakRate;
  }

  public MsgVpnDistributedCacheClusterInstance averageDataTxMsgRate(Long averageDataTxMsgRate) {
    this.averageDataTxMsgRate = averageDataTxMsgRate;
    return this;
  }

   /**
   * The one minute average of the data message rate transmitted by the Cache Instance, in messages per second (msg/sec). Available since 2.13.
   * @return averageDataTxMsgRate
  **/
  @Schema(description = "The one minute average of the data message rate transmitted by the Cache Instance, in messages per second (msg/sec). Available since 2.13.")
  public Long getAverageDataTxMsgRate() {
    return averageDataTxMsgRate;
  }

  public void setAverageDataTxMsgRate(Long averageDataTxMsgRate) {
    this.averageDataTxMsgRate = averageDataTxMsgRate;
  }

  public MsgVpnDistributedCacheClusterInstance averageRequestRxPeakRate(Long averageRequestRxPeakRate) {
    this.averageRequestRxPeakRate = averageRequestRxPeakRate;
    return this;
  }

   /**
   * The peak of the one minute average of the request rate received by the Cache Instance, in requests per second (req/sec). Available since 2.13.
   * @return averageRequestRxPeakRate
  **/
  @Schema(description = "The peak of the one minute average of the request rate received by the Cache Instance, in requests per second (req/sec). Available since 2.13.")
  public Long getAverageRequestRxPeakRate() {
    return averageRequestRxPeakRate;
  }

  public void setAverageRequestRxPeakRate(Long averageRequestRxPeakRate) {
    this.averageRequestRxPeakRate = averageRequestRxPeakRate;
  }

  public MsgVpnDistributedCacheClusterInstance averageRequestRxRate(Long averageRequestRxRate) {
    this.averageRequestRxRate = averageRequestRxRate;
    return this;
  }

   /**
   * The one minute average of the request rate received by the Cache Instance, in requests per second (req/sec). Available since 2.13.
   * @return averageRequestRxRate
  **/
  @Schema(description = "The one minute average of the request rate received by the Cache Instance, in requests per second (req/sec). Available since 2.13.")
  public Long getAverageRequestRxRate() {
    return averageRequestRxRate;
  }

  public void setAverageRequestRxRate(Long averageRequestRxRate) {
    this.averageRequestRxRate = averageRequestRxRate;
  }

  public MsgVpnDistributedCacheClusterInstance cacheName(String cacheName) {
    this.cacheName = cacheName;
    return this;
  }

   /**
   * The name of the Distributed Cache.
   * @return cacheName
  **/
  @Schema(description = "The name of the Distributed Cache.")
  public String getCacheName() {
    return cacheName;
  }

  public void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }

  public MsgVpnDistributedCacheClusterInstance clusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

   /**
   * The name of the Cache Cluster.
   * @return clusterName
  **/
  @Schema(description = "The name of the Cache Cluster.")
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public MsgVpnDistributedCacheClusterInstance counter(MsgVpnDistributedCacheClusterInstanceCounter counter) {
    this.counter = counter;
    return this;
  }

   /**
   * Get counter
   * @return counter
  **/
  @Schema(description = "")
  public MsgVpnDistributedCacheClusterInstanceCounter getCounter() {
    return counter;
  }

  public void setCounter(MsgVpnDistributedCacheClusterInstanceCounter counter) {
    this.counter = counter;
  }

  public MsgVpnDistributedCacheClusterInstance dataRxBytePeakRate(Long dataRxBytePeakRate) {
    this.dataRxBytePeakRate = dataRxBytePeakRate;
    return this;
  }

   /**
   * The data message peak rate received by the Cache Instance, in bytes per second (B/sec). Available since 2.13.
   * @return dataRxBytePeakRate
  **/
  @Schema(description = "The data message peak rate received by the Cache Instance, in bytes per second (B/sec). Available since 2.13.")
  public Long getDataRxBytePeakRate() {
    return dataRxBytePeakRate;
  }

  public void setDataRxBytePeakRate(Long dataRxBytePeakRate) {
    this.dataRxBytePeakRate = dataRxBytePeakRate;
  }

  public MsgVpnDistributedCacheClusterInstance dataRxByteRate(Long dataRxByteRate) {
    this.dataRxByteRate = dataRxByteRate;
    return this;
  }

   /**
   * The data message rate received by the Cache Instance, in bytes per second (B/sec). Available since 2.13.
   * @return dataRxByteRate
  **/
  @Schema(description = "The data message rate received by the Cache Instance, in bytes per second (B/sec). Available since 2.13.")
  public Long getDataRxByteRate() {
    return dataRxByteRate;
  }

  public void setDataRxByteRate(Long dataRxByteRate) {
    this.dataRxByteRate = dataRxByteRate;
  }

  public MsgVpnDistributedCacheClusterInstance dataRxMsgPeakRate(Long dataRxMsgPeakRate) {
    this.dataRxMsgPeakRate = dataRxMsgPeakRate;
    return this;
  }

   /**
   * The data message peak rate received by the Cache Instance, in messages per second (msg/sec). Available since 2.13.
   * @return dataRxMsgPeakRate
  **/
  @Schema(description = "The data message peak rate received by the Cache Instance, in messages per second (msg/sec). Available since 2.13.")
  public Long getDataRxMsgPeakRate() {
    return dataRxMsgPeakRate;
  }

  public void setDataRxMsgPeakRate(Long dataRxMsgPeakRate) {
    this.dataRxMsgPeakRate = dataRxMsgPeakRate;
  }

  public MsgVpnDistributedCacheClusterInstance dataRxMsgRate(Long dataRxMsgRate) {
    this.dataRxMsgRate = dataRxMsgRate;
    return this;
  }

   /**
   * The data message rate received by the Cache Instance, in messages per second (msg/sec). Available since 2.13.
   * @return dataRxMsgRate
  **/
  @Schema(description = "The data message rate received by the Cache Instance, in messages per second (msg/sec). Available since 2.13.")
  public Long getDataRxMsgRate() {
    return dataRxMsgRate;
  }

  public void setDataRxMsgRate(Long dataRxMsgRate) {
    this.dataRxMsgRate = dataRxMsgRate;
  }

  public MsgVpnDistributedCacheClusterInstance dataTxMsgPeakRate(Long dataTxMsgPeakRate) {
    this.dataTxMsgPeakRate = dataTxMsgPeakRate;
    return this;
  }

   /**
   * The data message peak rate transmitted by the Cache Instance, in messages per second (msg/sec). Available since 2.13.
   * @return dataTxMsgPeakRate
  **/
  @Schema(description = "The data message peak rate transmitted by the Cache Instance, in messages per second (msg/sec). Available since 2.13.")
  public Long getDataTxMsgPeakRate() {
    return dataTxMsgPeakRate;
  }

  public void setDataTxMsgPeakRate(Long dataTxMsgPeakRate) {
    this.dataTxMsgPeakRate = dataTxMsgPeakRate;
  }

  public MsgVpnDistributedCacheClusterInstance dataTxMsgRate(Long dataTxMsgRate) {
    this.dataTxMsgRate = dataTxMsgRate;
    return this;
  }

   /**
   * The data message rate transmitted by the Cache Instance, in messages per second (msg/sec). Available since 2.13.
   * @return dataTxMsgRate
  **/
  @Schema(description = "The data message rate transmitted by the Cache Instance, in messages per second (msg/sec). Available since 2.13.")
  public Long getDataTxMsgRate() {
    return dataTxMsgRate;
  }

  public void setDataTxMsgRate(Long dataTxMsgRate) {
    this.dataTxMsgRate = dataTxMsgRate;
  }

  public MsgVpnDistributedCacheClusterInstance enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

   /**
   * Indicates whether the Cache Instance is enabled.
   * @return enabled
  **/
  @Schema(description = "Indicates whether the Cache Instance is enabled.")
  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public MsgVpnDistributedCacheClusterInstance instanceName(String instanceName) {
    this.instanceName = instanceName;
    return this;
  }

   /**
   * The name of the Cache Instance.
   * @return instanceName
  **/
  @Schema(description = "The name of the Cache Instance.")
  public String getInstanceName() {
    return instanceName;
  }

  public void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }

  public MsgVpnDistributedCacheClusterInstance lastRegisteredTime(Integer lastRegisteredTime) {
    this.lastRegisteredTime = lastRegisteredTime;
    return this;
  }

   /**
   * The timestamp of when the Cache Instance last registered with the message broker. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return lastRegisteredTime
  **/
  @Schema(description = "The timestamp of when the Cache Instance last registered with the message broker. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getLastRegisteredTime() {
    return lastRegisteredTime;
  }

  public void setLastRegisteredTime(Integer lastRegisteredTime) {
    this.lastRegisteredTime = lastRegisteredTime;
  }

  public MsgVpnDistributedCacheClusterInstance lastRxHeartbeatTime(Integer lastRxHeartbeatTime) {
    this.lastRxHeartbeatTime = lastRxHeartbeatTime;
    return this;
  }

   /**
   * The timestamp of the last heartbeat message received from the Cache Instance. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return lastRxHeartbeatTime
  **/
  @Schema(description = "The timestamp of the last heartbeat message received from the Cache Instance. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getLastRxHeartbeatTime() {
    return lastRxHeartbeatTime;
  }

  public void setLastRxHeartbeatTime(Integer lastRxHeartbeatTime) {
    this.lastRxHeartbeatTime = lastRxHeartbeatTime;
  }

  public MsgVpnDistributedCacheClusterInstance lastRxSetLostMsgTime(Integer lastRxSetLostMsgTime) {
    this.lastRxSetLostMsgTime = lastRxSetLostMsgTime;
    return this;
  }

   /**
   * The timestamp of the last request for setting the lost message indication received from the Cache Instance. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return lastRxSetLostMsgTime
  **/
  @Schema(description = "The timestamp of the last request for setting the lost message indication received from the Cache Instance. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getLastRxSetLostMsgTime() {
    return lastRxSetLostMsgTime;
  }

  public void setLastRxSetLostMsgTime(Integer lastRxSetLostMsgTime) {
    this.lastRxSetLostMsgTime = lastRxSetLostMsgTime;
  }

  public MsgVpnDistributedCacheClusterInstance lastStoppedReason(String lastStoppedReason) {
    this.lastStoppedReason = lastStoppedReason;
    return this;
  }

   /**
   * The reason why the Cache Instance was last stopped.
   * @return lastStoppedReason
  **/
  @Schema(description = "The reason why the Cache Instance was last stopped.")
  public String getLastStoppedReason() {
    return lastStoppedReason;
  }

  public void setLastStoppedReason(String lastStoppedReason) {
    this.lastStoppedReason = lastStoppedReason;
  }

  public MsgVpnDistributedCacheClusterInstance lastStoppedTime(Integer lastStoppedTime) {
    this.lastStoppedTime = lastStoppedTime;
    return this;
  }

   /**
   * The timestamp of when the Cache Instance was last stopped. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return lastStoppedTime
  **/
  @Schema(description = "The timestamp of when the Cache Instance was last stopped. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getLastStoppedTime() {
    return lastStoppedTime;
  }

  public void setLastStoppedTime(Integer lastStoppedTime) {
    this.lastStoppedTime = lastStoppedTime;
  }

  public MsgVpnDistributedCacheClusterInstance lastTxClearLostMsgTime(Integer lastTxClearLostMsgTime) {
    this.lastTxClearLostMsgTime = lastTxClearLostMsgTime;
    return this;
  }

   /**
   * The timestamp of the last request for clearing the lost message indication transmitted to the Cache Instance. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).
   * @return lastTxClearLostMsgTime
  **/
  @Schema(description = "The timestamp of the last request for clearing the lost message indication transmitted to the Cache Instance. This value represents the number of seconds since 1970-01-01 00:00:00 UTC (Unix time).")
  public Integer getLastTxClearLostMsgTime() {
    return lastTxClearLostMsgTime;
  }

  public void setLastTxClearLostMsgTime(Integer lastTxClearLostMsgTime) {
    this.lastTxClearLostMsgTime = lastTxClearLostMsgTime;
  }

  public MsgVpnDistributedCacheClusterInstance memoryUsage(Integer memoryUsage) {
    this.memoryUsage = memoryUsage;
    return this;
  }

   /**
   * The memory usage of the Cache Instance, in megabytes (MB).
   * @return memoryUsage
  **/
  @Schema(description = "The memory usage of the Cache Instance, in megabytes (MB).")
  public Integer getMemoryUsage() {
    return memoryUsage;
  }

  public void setMemoryUsage(Integer memoryUsage) {
    this.memoryUsage = memoryUsage;
  }

  public MsgVpnDistributedCacheClusterInstance msgCount(Long msgCount) {
    this.msgCount = msgCount;
    return this;
  }

   /**
   * The number of messages cached for the Cache Instance. Available since 2.13.
   * @return msgCount
  **/
  @Schema(description = "The number of messages cached for the Cache Instance. Available since 2.13.")
  public Long getMsgCount() {
    return msgCount;
  }

  public void setMsgCount(Long msgCount) {
    this.msgCount = msgCount;
  }

  public MsgVpnDistributedCacheClusterInstance msgPeakCount(Long msgPeakCount) {
    this.msgPeakCount = msgPeakCount;
    return this;
  }

   /**
   * The number of messages cached peak for the Cache Instance. Available since 2.13.
   * @return msgPeakCount
  **/
  @Schema(description = "The number of messages cached peak for the Cache Instance. Available since 2.13.")
  public Long getMsgPeakCount() {
    return msgPeakCount;
  }

  public void setMsgPeakCount(Long msgPeakCount) {
    this.msgPeakCount = msgPeakCount;
  }

  public MsgVpnDistributedCacheClusterInstance msgVpnName(String msgVpnName) {
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

  public MsgVpnDistributedCacheClusterInstance msgsLost(Boolean msgsLost) {
    this.msgsLost = msgsLost;
    return this;
  }

   /**
   * Indicates whether one or more messages were lost by the Cache Instance.
   * @return msgsLost
  **/
  @Schema(description = "Indicates whether one or more messages were lost by the Cache Instance.")
  public Boolean isMsgsLost() {
    return msgsLost;
  }

  public void setMsgsLost(Boolean msgsLost) {
    this.msgsLost = msgsLost;
  }

  public MsgVpnDistributedCacheClusterInstance rate(MsgVpnDistributedCacheClusterInstanceRate rate) {
    this.rate = rate;
    return this;
  }

   /**
   * Get rate
   * @return rate
  **/
  @Schema(description = "")
  public MsgVpnDistributedCacheClusterInstanceRate getRate() {
    return rate;
  }

  public void setRate(MsgVpnDistributedCacheClusterInstanceRate rate) {
    this.rate = rate;
  }

  public MsgVpnDistributedCacheClusterInstance requestQueueDepthCount(Long requestQueueDepthCount) {
    this.requestQueueDepthCount = requestQueueDepthCount;
    return this;
  }

   /**
   * The received request message queue depth for the Cache Instance. Available since 2.13.
   * @return requestQueueDepthCount
  **/
  @Schema(description = "The received request message queue depth for the Cache Instance. Available since 2.13.")
  public Long getRequestQueueDepthCount() {
    return requestQueueDepthCount;
  }

  public void setRequestQueueDepthCount(Long requestQueueDepthCount) {
    this.requestQueueDepthCount = requestQueueDepthCount;
  }

  public MsgVpnDistributedCacheClusterInstance requestQueueDepthPeakCount(Long requestQueueDepthPeakCount) {
    this.requestQueueDepthPeakCount = requestQueueDepthPeakCount;
    return this;
  }

   /**
   * The received request message queue depth peak for the Cache Instance. Available since 2.13.
   * @return requestQueueDepthPeakCount
  **/
  @Schema(description = "The received request message queue depth peak for the Cache Instance. Available since 2.13.")
  public Long getRequestQueueDepthPeakCount() {
    return requestQueueDepthPeakCount;
  }

  public void setRequestQueueDepthPeakCount(Long requestQueueDepthPeakCount) {
    this.requestQueueDepthPeakCount = requestQueueDepthPeakCount;
  }

  public MsgVpnDistributedCacheClusterInstance requestRxPeakRate(Long requestRxPeakRate) {
    this.requestRxPeakRate = requestRxPeakRate;
    return this;
  }

   /**
   * The request peak rate received by the Cache Instance, in requests per second (req/sec). Available since 2.13.
   * @return requestRxPeakRate
  **/
  @Schema(description = "The request peak rate received by the Cache Instance, in requests per second (req/sec). Available since 2.13.")
  public Long getRequestRxPeakRate() {
    return requestRxPeakRate;
  }

  public void setRequestRxPeakRate(Long requestRxPeakRate) {
    this.requestRxPeakRate = requestRxPeakRate;
  }

  public MsgVpnDistributedCacheClusterInstance requestRxRate(Long requestRxRate) {
    this.requestRxRate = requestRxRate;
    return this;
  }

   /**
   * The request rate received by the Cache Instance, in requests per second (req/sec). Available since 2.13.
   * @return requestRxRate
  **/
  @Schema(description = "The request rate received by the Cache Instance, in requests per second (req/sec). Available since 2.13.")
  public Long getRequestRxRate() {
    return requestRxRate;
  }

  public void setRequestRxRate(Long requestRxRate) {
    this.requestRxRate = requestRxRate;
  }

  public MsgVpnDistributedCacheClusterInstance state(String state) {
    this.state = state;
    return this;
  }

   /**
   * The operational state of the Cache Instance. The allowed values and their meaning are:  &lt;pre&gt; \&quot;invalid\&quot; - The Cache Instance state is invalid. \&quot;down\&quot; - The Cache Instance is operationally down. \&quot;stopped\&quot; - The Cache Instance has stopped processing cache requests. \&quot;stopped-lost-msg\&quot; - The Cache Instance has stopped due to a lost message. \&quot;register\&quot; - The Cache Instance is registering with the broker. \&quot;config-sync\&quot; - The Cache Instance is synchronizing its configuration with the broker. \&quot;cluster-sync\&quot; - The Cache Instance is synchronizing its messages with the Cache Cluster. \&quot;up\&quot; - The Cache Instance is operationally up. \&quot;backup\&quot; - The Cache Instance is backing up its messages to disk. \&quot;restore\&quot; - The Cache Instance is restoring its messages from disk. \&quot;not-available\&quot; - The Cache Instance state is not available. &lt;/pre&gt; 
   * @return state
  **/
  @Schema(description = "The operational state of the Cache Instance. The allowed values and their meaning are:  <pre> \"invalid\" - The Cache Instance state is invalid. \"down\" - The Cache Instance is operationally down. \"stopped\" - The Cache Instance has stopped processing cache requests. \"stopped-lost-msg\" - The Cache Instance has stopped due to a lost message. \"register\" - The Cache Instance is registering with the broker. \"config-sync\" - The Cache Instance is synchronizing its configuration with the broker. \"cluster-sync\" - The Cache Instance is synchronizing its messages with the Cache Cluster. \"up\" - The Cache Instance is operationally up. \"backup\" - The Cache Instance is backing up its messages to disk. \"restore\" - The Cache Instance is restoring its messages from disk. \"not-available\" - The Cache Instance state is not available. </pre> ")
  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public MsgVpnDistributedCacheClusterInstance stopOnLostMsgEnabled(Boolean stopOnLostMsgEnabled) {
    this.stopOnLostMsgEnabled = stopOnLostMsgEnabled;
    return this;
  }

   /**
   * Indicates whether stop-on-lost-message is enabled, and the Cache Instance will transition to the Stopped operational state upon losing a message. When Stopped, it cannot accept or respond to cache requests, but continues to cache messages.
   * @return stopOnLostMsgEnabled
  **/
  @Schema(description = "Indicates whether stop-on-lost-message is enabled, and the Cache Instance will transition to the Stopped operational state upon losing a message. When Stopped, it cannot accept or respond to cache requests, but continues to cache messages.")
  public Boolean isStopOnLostMsgEnabled() {
    return stopOnLostMsgEnabled;
  }

  public void setStopOnLostMsgEnabled(Boolean stopOnLostMsgEnabled) {
    this.stopOnLostMsgEnabled = stopOnLostMsgEnabled;
  }

  public MsgVpnDistributedCacheClusterInstance topicCount(Long topicCount) {
    this.topicCount = topicCount;
    return this;
  }

   /**
   * The number of topics cached for the Cache Instance. Available since 2.13.
   * @return topicCount
  **/
  @Schema(description = "The number of topics cached for the Cache Instance. Available since 2.13.")
  public Long getTopicCount() {
    return topicCount;
  }

  public void setTopicCount(Long topicCount) {
    this.topicCount = topicCount;
  }

  public MsgVpnDistributedCacheClusterInstance topicPeakCount(Long topicPeakCount) {
    this.topicPeakCount = topicPeakCount;
    return this;
  }

   /**
   * The number of topics cached peak for the Cache Instance. Available since 2.13.
   * @return topicPeakCount
  **/
  @Schema(description = "The number of topics cached peak for the Cache Instance. Available since 2.13.")
  public Long getTopicPeakCount() {
    return topicPeakCount;
  }

  public void setTopicPeakCount(Long topicPeakCount) {
    this.topicPeakCount = topicPeakCount;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnDistributedCacheClusterInstance msgVpnDistributedCacheClusterInstance = (MsgVpnDistributedCacheClusterInstance) o;
    return Objects.equals(this.autoStartEnabled, msgVpnDistributedCacheClusterInstance.autoStartEnabled) &&
        Objects.equals(this.averageDataRxBytePeakRate, msgVpnDistributedCacheClusterInstance.averageDataRxBytePeakRate) &&
        Objects.equals(this.averageDataRxByteRate, msgVpnDistributedCacheClusterInstance.averageDataRxByteRate) &&
        Objects.equals(this.averageDataRxMsgPeakRate, msgVpnDistributedCacheClusterInstance.averageDataRxMsgPeakRate) &&
        Objects.equals(this.averageDataRxMsgRate, msgVpnDistributedCacheClusterInstance.averageDataRxMsgRate) &&
        Objects.equals(this.averageDataTxMsgPeakRate, msgVpnDistributedCacheClusterInstance.averageDataTxMsgPeakRate) &&
        Objects.equals(this.averageDataTxMsgRate, msgVpnDistributedCacheClusterInstance.averageDataTxMsgRate) &&
        Objects.equals(this.averageRequestRxPeakRate, msgVpnDistributedCacheClusterInstance.averageRequestRxPeakRate) &&
        Objects.equals(this.averageRequestRxRate, msgVpnDistributedCacheClusterInstance.averageRequestRxRate) &&
        Objects.equals(this.cacheName, msgVpnDistributedCacheClusterInstance.cacheName) &&
        Objects.equals(this.clusterName, msgVpnDistributedCacheClusterInstance.clusterName) &&
        Objects.equals(this.counter, msgVpnDistributedCacheClusterInstance.counter) &&
        Objects.equals(this.dataRxBytePeakRate, msgVpnDistributedCacheClusterInstance.dataRxBytePeakRate) &&
        Objects.equals(this.dataRxByteRate, msgVpnDistributedCacheClusterInstance.dataRxByteRate) &&
        Objects.equals(this.dataRxMsgPeakRate, msgVpnDistributedCacheClusterInstance.dataRxMsgPeakRate) &&
        Objects.equals(this.dataRxMsgRate, msgVpnDistributedCacheClusterInstance.dataRxMsgRate) &&
        Objects.equals(this.dataTxMsgPeakRate, msgVpnDistributedCacheClusterInstance.dataTxMsgPeakRate) &&
        Objects.equals(this.dataTxMsgRate, msgVpnDistributedCacheClusterInstance.dataTxMsgRate) &&
        Objects.equals(this.enabled, msgVpnDistributedCacheClusterInstance.enabled) &&
        Objects.equals(this.instanceName, msgVpnDistributedCacheClusterInstance.instanceName) &&
        Objects.equals(this.lastRegisteredTime, msgVpnDistributedCacheClusterInstance.lastRegisteredTime) &&
        Objects.equals(this.lastRxHeartbeatTime, msgVpnDistributedCacheClusterInstance.lastRxHeartbeatTime) &&
        Objects.equals(this.lastRxSetLostMsgTime, msgVpnDistributedCacheClusterInstance.lastRxSetLostMsgTime) &&
        Objects.equals(this.lastStoppedReason, msgVpnDistributedCacheClusterInstance.lastStoppedReason) &&
        Objects.equals(this.lastStoppedTime, msgVpnDistributedCacheClusterInstance.lastStoppedTime) &&
        Objects.equals(this.lastTxClearLostMsgTime, msgVpnDistributedCacheClusterInstance.lastTxClearLostMsgTime) &&
        Objects.equals(this.memoryUsage, msgVpnDistributedCacheClusterInstance.memoryUsage) &&
        Objects.equals(this.msgCount, msgVpnDistributedCacheClusterInstance.msgCount) &&
        Objects.equals(this.msgPeakCount, msgVpnDistributedCacheClusterInstance.msgPeakCount) &&
        Objects.equals(this.msgVpnName, msgVpnDistributedCacheClusterInstance.msgVpnName) &&
        Objects.equals(this.msgsLost, msgVpnDistributedCacheClusterInstance.msgsLost) &&
        Objects.equals(this.rate, msgVpnDistributedCacheClusterInstance.rate) &&
        Objects.equals(this.requestQueueDepthCount, msgVpnDistributedCacheClusterInstance.requestQueueDepthCount) &&
        Objects.equals(this.requestQueueDepthPeakCount, msgVpnDistributedCacheClusterInstance.requestQueueDepthPeakCount) &&
        Objects.equals(this.requestRxPeakRate, msgVpnDistributedCacheClusterInstance.requestRxPeakRate) &&
        Objects.equals(this.requestRxRate, msgVpnDistributedCacheClusterInstance.requestRxRate) &&
        Objects.equals(this.state, msgVpnDistributedCacheClusterInstance.state) &&
        Objects.equals(this.stopOnLostMsgEnabled, msgVpnDistributedCacheClusterInstance.stopOnLostMsgEnabled) &&
        Objects.equals(this.topicCount, msgVpnDistributedCacheClusterInstance.topicCount) &&
        Objects.equals(this.topicPeakCount, msgVpnDistributedCacheClusterInstance.topicPeakCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(autoStartEnabled, averageDataRxBytePeakRate, averageDataRxByteRate, averageDataRxMsgPeakRate, averageDataRxMsgRate, averageDataTxMsgPeakRate, averageDataTxMsgRate, averageRequestRxPeakRate, averageRequestRxRate, cacheName, clusterName, counter, dataRxBytePeakRate, dataRxByteRate, dataRxMsgPeakRate, dataRxMsgRate, dataTxMsgPeakRate, dataTxMsgRate, enabled, instanceName, lastRegisteredTime, lastRxHeartbeatTime, lastRxSetLostMsgTime, lastStoppedReason, lastStoppedTime, lastTxClearLostMsgTime, memoryUsage, msgCount, msgPeakCount, msgVpnName, msgsLost, rate, requestQueueDepthCount, requestQueueDepthPeakCount, requestRxPeakRate, requestRxRate, state, stopOnLostMsgEnabled, topicCount, topicPeakCount);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnDistributedCacheClusterInstance {\n");
    
    sb.append("    autoStartEnabled: ").append(toIndentedString(autoStartEnabled)).append("\n");
    sb.append("    averageDataRxBytePeakRate: ").append(toIndentedString(averageDataRxBytePeakRate)).append("\n");
    sb.append("    averageDataRxByteRate: ").append(toIndentedString(averageDataRxByteRate)).append("\n");
    sb.append("    averageDataRxMsgPeakRate: ").append(toIndentedString(averageDataRxMsgPeakRate)).append("\n");
    sb.append("    averageDataRxMsgRate: ").append(toIndentedString(averageDataRxMsgRate)).append("\n");
    sb.append("    averageDataTxMsgPeakRate: ").append(toIndentedString(averageDataTxMsgPeakRate)).append("\n");
    sb.append("    averageDataTxMsgRate: ").append(toIndentedString(averageDataTxMsgRate)).append("\n");
    sb.append("    averageRequestRxPeakRate: ").append(toIndentedString(averageRequestRxPeakRate)).append("\n");
    sb.append("    averageRequestRxRate: ").append(toIndentedString(averageRequestRxRate)).append("\n");
    sb.append("    cacheName: ").append(toIndentedString(cacheName)).append("\n");
    sb.append("    clusterName: ").append(toIndentedString(clusterName)).append("\n");
    sb.append("    counter: ").append(toIndentedString(counter)).append("\n");
    sb.append("    dataRxBytePeakRate: ").append(toIndentedString(dataRxBytePeakRate)).append("\n");
    sb.append("    dataRxByteRate: ").append(toIndentedString(dataRxByteRate)).append("\n");
    sb.append("    dataRxMsgPeakRate: ").append(toIndentedString(dataRxMsgPeakRate)).append("\n");
    sb.append("    dataRxMsgRate: ").append(toIndentedString(dataRxMsgRate)).append("\n");
    sb.append("    dataTxMsgPeakRate: ").append(toIndentedString(dataTxMsgPeakRate)).append("\n");
    sb.append("    dataTxMsgRate: ").append(toIndentedString(dataTxMsgRate)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    instanceName: ").append(toIndentedString(instanceName)).append("\n");
    sb.append("    lastRegisteredTime: ").append(toIndentedString(lastRegisteredTime)).append("\n");
    sb.append("    lastRxHeartbeatTime: ").append(toIndentedString(lastRxHeartbeatTime)).append("\n");
    sb.append("    lastRxSetLostMsgTime: ").append(toIndentedString(lastRxSetLostMsgTime)).append("\n");
    sb.append("    lastStoppedReason: ").append(toIndentedString(lastStoppedReason)).append("\n");
    sb.append("    lastStoppedTime: ").append(toIndentedString(lastStoppedTime)).append("\n");
    sb.append("    lastTxClearLostMsgTime: ").append(toIndentedString(lastTxClearLostMsgTime)).append("\n");
    sb.append("    memoryUsage: ").append(toIndentedString(memoryUsage)).append("\n");
    sb.append("    msgCount: ").append(toIndentedString(msgCount)).append("\n");
    sb.append("    msgPeakCount: ").append(toIndentedString(msgPeakCount)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    msgsLost: ").append(toIndentedString(msgsLost)).append("\n");
    sb.append("    rate: ").append(toIndentedString(rate)).append("\n");
    sb.append("    requestQueueDepthCount: ").append(toIndentedString(requestQueueDepthCount)).append("\n");
    sb.append("    requestQueueDepthPeakCount: ").append(toIndentedString(requestQueueDepthPeakCount)).append("\n");
    sb.append("    requestRxPeakRate: ").append(toIndentedString(requestRxPeakRate)).append("\n");
    sb.append("    requestRxRate: ").append(toIndentedString(requestRxRate)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    stopOnLostMsgEnabled: ").append(toIndentedString(stopOnLostMsgEnabled)).append("\n");
    sb.append("    topicCount: ").append(toIndentedString(topicCount)).append("\n");
    sb.append("    topicPeakCount: ").append(toIndentedString(topicPeakCount)).append("\n");
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
