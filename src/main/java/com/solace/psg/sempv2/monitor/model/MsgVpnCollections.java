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
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsAclprofiles;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsAuthenticationoauthproviders;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsAuthorizationgroups;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsBridges;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsClientprofiles;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsClients;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsClientusernames;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsConfigsyncremotenodes;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsDistributedcaches;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsDmrbridges;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsJndiconnectionfactories;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsJndiqueues;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsJnditopics;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsMqttretaincaches;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsMqttsessions;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsQueues;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsQueuetemplates;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsReplaylogs;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsReplicatedtopics;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsRestdeliverypoints;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsTopicendpoints;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsTopicendpointtemplates;
import com.solace.psg.sempv2.monitor.model.MsgVpnCollectionsTransactions;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * MsgVpnCollections
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:57:21.016551900+01:00[Europe/London]")
public class MsgVpnCollections {
  @SerializedName("aclProfiles")
  private MsgVpnCollectionsAclprofiles aclProfiles = null;

  @SerializedName("authenticationOauthProviders")
  private MsgVpnCollectionsAuthenticationoauthproviders authenticationOauthProviders = null;

  @SerializedName("authorizationGroups")
  private MsgVpnCollectionsAuthorizationgroups authorizationGroups = null;

  @SerializedName("bridges")
  private MsgVpnCollectionsBridges bridges = null;

  @SerializedName("clientProfiles")
  private MsgVpnCollectionsClientprofiles clientProfiles = null;

  @SerializedName("clientUsernames")
  private MsgVpnCollectionsClientusernames clientUsernames = null;

  @SerializedName("clients")
  private MsgVpnCollectionsClients clients = null;

  @SerializedName("configSyncRemoteNodes")
  private MsgVpnCollectionsConfigsyncremotenodes configSyncRemoteNodes = null;

  @SerializedName("distributedCaches")
  private MsgVpnCollectionsDistributedcaches distributedCaches = null;

  @SerializedName("dmrBridges")
  private MsgVpnCollectionsDmrbridges dmrBridges = null;

  @SerializedName("jndiConnectionFactories")
  private MsgVpnCollectionsJndiconnectionfactories jndiConnectionFactories = null;

  @SerializedName("jndiQueues")
  private MsgVpnCollectionsJndiqueues jndiQueues = null;

  @SerializedName("jndiTopics")
  private MsgVpnCollectionsJnditopics jndiTopics = null;

  @SerializedName("mqttRetainCaches")
  private MsgVpnCollectionsMqttretaincaches mqttRetainCaches = null;

  @SerializedName("mqttSessions")
  private MsgVpnCollectionsMqttsessions mqttSessions = null;

  @SerializedName("queueTemplates")
  private MsgVpnCollectionsQueuetemplates queueTemplates = null;

  @SerializedName("queues")
  private MsgVpnCollectionsQueues queues = null;

  @SerializedName("replayLogs")
  private MsgVpnCollectionsReplaylogs replayLogs = null;

  @SerializedName("replicatedTopics")
  private MsgVpnCollectionsReplicatedtopics replicatedTopics = null;

  @SerializedName("restDeliveryPoints")
  private MsgVpnCollectionsRestdeliverypoints restDeliveryPoints = null;

  @SerializedName("topicEndpointTemplates")
  private MsgVpnCollectionsTopicendpointtemplates topicEndpointTemplates = null;

  @SerializedName("topicEndpoints")
  private MsgVpnCollectionsTopicendpoints topicEndpoints = null;

  @SerializedName("transactions")
  private MsgVpnCollectionsTransactions transactions = null;

  public MsgVpnCollections aclProfiles(MsgVpnCollectionsAclprofiles aclProfiles) {
    this.aclProfiles = aclProfiles;
    return this;
  }

   /**
   * Get aclProfiles
   * @return aclProfiles
  **/
  @Schema(description = "")
  public MsgVpnCollectionsAclprofiles getAclProfiles() {
    return aclProfiles;
  }

  public void setAclProfiles(MsgVpnCollectionsAclprofiles aclProfiles) {
    this.aclProfiles = aclProfiles;
  }

  public MsgVpnCollections authenticationOauthProviders(MsgVpnCollectionsAuthenticationoauthproviders authenticationOauthProviders) {
    this.authenticationOauthProviders = authenticationOauthProviders;
    return this;
  }

   /**
   * Get authenticationOauthProviders
   * @return authenticationOauthProviders
  **/
  @Schema(description = "")
  public MsgVpnCollectionsAuthenticationoauthproviders getAuthenticationOauthProviders() {
    return authenticationOauthProviders;
  }

  public void setAuthenticationOauthProviders(MsgVpnCollectionsAuthenticationoauthproviders authenticationOauthProviders) {
    this.authenticationOauthProviders = authenticationOauthProviders;
  }

  public MsgVpnCollections authorizationGroups(MsgVpnCollectionsAuthorizationgroups authorizationGroups) {
    this.authorizationGroups = authorizationGroups;
    return this;
  }

   /**
   * Get authorizationGroups
   * @return authorizationGroups
  **/
  @Schema(description = "")
  public MsgVpnCollectionsAuthorizationgroups getAuthorizationGroups() {
    return authorizationGroups;
  }

  public void setAuthorizationGroups(MsgVpnCollectionsAuthorizationgroups authorizationGroups) {
    this.authorizationGroups = authorizationGroups;
  }

  public MsgVpnCollections bridges(MsgVpnCollectionsBridges bridges) {
    this.bridges = bridges;
    return this;
  }

   /**
   * Get bridges
   * @return bridges
  **/
  @Schema(description = "")
  public MsgVpnCollectionsBridges getBridges() {
    return bridges;
  }

  public void setBridges(MsgVpnCollectionsBridges bridges) {
    this.bridges = bridges;
  }

  public MsgVpnCollections clientProfiles(MsgVpnCollectionsClientprofiles clientProfiles) {
    this.clientProfiles = clientProfiles;
    return this;
  }

   /**
   * Get clientProfiles
   * @return clientProfiles
  **/
  @Schema(description = "")
  public MsgVpnCollectionsClientprofiles getClientProfiles() {
    return clientProfiles;
  }

  public void setClientProfiles(MsgVpnCollectionsClientprofiles clientProfiles) {
    this.clientProfiles = clientProfiles;
  }

  public MsgVpnCollections clientUsernames(MsgVpnCollectionsClientusernames clientUsernames) {
    this.clientUsernames = clientUsernames;
    return this;
  }

   /**
   * Get clientUsernames
   * @return clientUsernames
  **/
  @Schema(description = "")
  public MsgVpnCollectionsClientusernames getClientUsernames() {
    return clientUsernames;
  }

  public void setClientUsernames(MsgVpnCollectionsClientusernames clientUsernames) {
    this.clientUsernames = clientUsernames;
  }

  public MsgVpnCollections clients(MsgVpnCollectionsClients clients) {
    this.clients = clients;
    return this;
  }

   /**
   * Get clients
   * @return clients
  **/
  @Schema(description = "")
  public MsgVpnCollectionsClients getClients() {
    return clients;
  }

  public void setClients(MsgVpnCollectionsClients clients) {
    this.clients = clients;
  }

  public MsgVpnCollections configSyncRemoteNodes(MsgVpnCollectionsConfigsyncremotenodes configSyncRemoteNodes) {
    this.configSyncRemoteNodes = configSyncRemoteNodes;
    return this;
  }

   /**
   * Get configSyncRemoteNodes
   * @return configSyncRemoteNodes
  **/
  @Schema(description = "")
  public MsgVpnCollectionsConfigsyncremotenodes getConfigSyncRemoteNodes() {
    return configSyncRemoteNodes;
  }

  public void setConfigSyncRemoteNodes(MsgVpnCollectionsConfigsyncremotenodes configSyncRemoteNodes) {
    this.configSyncRemoteNodes = configSyncRemoteNodes;
  }

  public MsgVpnCollections distributedCaches(MsgVpnCollectionsDistributedcaches distributedCaches) {
    this.distributedCaches = distributedCaches;
    return this;
  }

   /**
   * Get distributedCaches
   * @return distributedCaches
  **/
  @Schema(description = "")
  public MsgVpnCollectionsDistributedcaches getDistributedCaches() {
    return distributedCaches;
  }

  public void setDistributedCaches(MsgVpnCollectionsDistributedcaches distributedCaches) {
    this.distributedCaches = distributedCaches;
  }

  public MsgVpnCollections dmrBridges(MsgVpnCollectionsDmrbridges dmrBridges) {
    this.dmrBridges = dmrBridges;
    return this;
  }

   /**
   * Get dmrBridges
   * @return dmrBridges
  **/
  @Schema(description = "")
  public MsgVpnCollectionsDmrbridges getDmrBridges() {
    return dmrBridges;
  }

  public void setDmrBridges(MsgVpnCollectionsDmrbridges dmrBridges) {
    this.dmrBridges = dmrBridges;
  }

  public MsgVpnCollections jndiConnectionFactories(MsgVpnCollectionsJndiconnectionfactories jndiConnectionFactories) {
    this.jndiConnectionFactories = jndiConnectionFactories;
    return this;
  }

   /**
   * Get jndiConnectionFactories
   * @return jndiConnectionFactories
  **/
  @Schema(description = "")
  public MsgVpnCollectionsJndiconnectionfactories getJndiConnectionFactories() {
    return jndiConnectionFactories;
  }

  public void setJndiConnectionFactories(MsgVpnCollectionsJndiconnectionfactories jndiConnectionFactories) {
    this.jndiConnectionFactories = jndiConnectionFactories;
  }

  public MsgVpnCollections jndiQueues(MsgVpnCollectionsJndiqueues jndiQueues) {
    this.jndiQueues = jndiQueues;
    return this;
  }

   /**
   * Get jndiQueues
   * @return jndiQueues
  **/
  @Schema(description = "")
  public MsgVpnCollectionsJndiqueues getJndiQueues() {
    return jndiQueues;
  }

  public void setJndiQueues(MsgVpnCollectionsJndiqueues jndiQueues) {
    this.jndiQueues = jndiQueues;
  }

  public MsgVpnCollections jndiTopics(MsgVpnCollectionsJnditopics jndiTopics) {
    this.jndiTopics = jndiTopics;
    return this;
  }

   /**
   * Get jndiTopics
   * @return jndiTopics
  **/
  @Schema(description = "")
  public MsgVpnCollectionsJnditopics getJndiTopics() {
    return jndiTopics;
  }

  public void setJndiTopics(MsgVpnCollectionsJnditopics jndiTopics) {
    this.jndiTopics = jndiTopics;
  }

  public MsgVpnCollections mqttRetainCaches(MsgVpnCollectionsMqttretaincaches mqttRetainCaches) {
    this.mqttRetainCaches = mqttRetainCaches;
    return this;
  }

   /**
   * Get mqttRetainCaches
   * @return mqttRetainCaches
  **/
  @Schema(description = "")
  public MsgVpnCollectionsMqttretaincaches getMqttRetainCaches() {
    return mqttRetainCaches;
  }

  public void setMqttRetainCaches(MsgVpnCollectionsMqttretaincaches mqttRetainCaches) {
    this.mqttRetainCaches = mqttRetainCaches;
  }

  public MsgVpnCollections mqttSessions(MsgVpnCollectionsMqttsessions mqttSessions) {
    this.mqttSessions = mqttSessions;
    return this;
  }

   /**
   * Get mqttSessions
   * @return mqttSessions
  **/
  @Schema(description = "")
  public MsgVpnCollectionsMqttsessions getMqttSessions() {
    return mqttSessions;
  }

  public void setMqttSessions(MsgVpnCollectionsMqttsessions mqttSessions) {
    this.mqttSessions = mqttSessions;
  }

  public MsgVpnCollections queueTemplates(MsgVpnCollectionsQueuetemplates queueTemplates) {
    this.queueTemplates = queueTemplates;
    return this;
  }

   /**
   * Get queueTemplates
   * @return queueTemplates
  **/
  @Schema(description = "")
  public MsgVpnCollectionsQueuetemplates getQueueTemplates() {
    return queueTemplates;
  }

  public void setQueueTemplates(MsgVpnCollectionsQueuetemplates queueTemplates) {
    this.queueTemplates = queueTemplates;
  }

  public MsgVpnCollections queues(MsgVpnCollectionsQueues queues) {
    this.queues = queues;
    return this;
  }

   /**
   * Get queues
   * @return queues
  **/
  @Schema(description = "")
  public MsgVpnCollectionsQueues getQueues() {
    return queues;
  }

  public void setQueues(MsgVpnCollectionsQueues queues) {
    this.queues = queues;
  }

  public MsgVpnCollections replayLogs(MsgVpnCollectionsReplaylogs replayLogs) {
    this.replayLogs = replayLogs;
    return this;
  }

   /**
   * Get replayLogs
   * @return replayLogs
  **/
  @Schema(description = "")
  public MsgVpnCollectionsReplaylogs getReplayLogs() {
    return replayLogs;
  }

  public void setReplayLogs(MsgVpnCollectionsReplaylogs replayLogs) {
    this.replayLogs = replayLogs;
  }

  public MsgVpnCollections replicatedTopics(MsgVpnCollectionsReplicatedtopics replicatedTopics) {
    this.replicatedTopics = replicatedTopics;
    return this;
  }

   /**
   * Get replicatedTopics
   * @return replicatedTopics
  **/
  @Schema(description = "")
  public MsgVpnCollectionsReplicatedtopics getReplicatedTopics() {
    return replicatedTopics;
  }

  public void setReplicatedTopics(MsgVpnCollectionsReplicatedtopics replicatedTopics) {
    this.replicatedTopics = replicatedTopics;
  }

  public MsgVpnCollections restDeliveryPoints(MsgVpnCollectionsRestdeliverypoints restDeliveryPoints) {
    this.restDeliveryPoints = restDeliveryPoints;
    return this;
  }

   /**
   * Get restDeliveryPoints
   * @return restDeliveryPoints
  **/
  @Schema(description = "")
  public MsgVpnCollectionsRestdeliverypoints getRestDeliveryPoints() {
    return restDeliveryPoints;
  }

  public void setRestDeliveryPoints(MsgVpnCollectionsRestdeliverypoints restDeliveryPoints) {
    this.restDeliveryPoints = restDeliveryPoints;
  }

  public MsgVpnCollections topicEndpointTemplates(MsgVpnCollectionsTopicendpointtemplates topicEndpointTemplates) {
    this.topicEndpointTemplates = topicEndpointTemplates;
    return this;
  }

   /**
   * Get topicEndpointTemplates
   * @return topicEndpointTemplates
  **/
  @Schema(description = "")
  public MsgVpnCollectionsTopicendpointtemplates getTopicEndpointTemplates() {
    return topicEndpointTemplates;
  }

  public void setTopicEndpointTemplates(MsgVpnCollectionsTopicendpointtemplates topicEndpointTemplates) {
    this.topicEndpointTemplates = topicEndpointTemplates;
  }

  public MsgVpnCollections topicEndpoints(MsgVpnCollectionsTopicendpoints topicEndpoints) {
    this.topicEndpoints = topicEndpoints;
    return this;
  }

   /**
   * Get topicEndpoints
   * @return topicEndpoints
  **/
  @Schema(description = "")
  public MsgVpnCollectionsTopicendpoints getTopicEndpoints() {
    return topicEndpoints;
  }

  public void setTopicEndpoints(MsgVpnCollectionsTopicendpoints topicEndpoints) {
    this.topicEndpoints = topicEndpoints;
  }

  public MsgVpnCollections transactions(MsgVpnCollectionsTransactions transactions) {
    this.transactions = transactions;
    return this;
  }

   /**
   * Get transactions
   * @return transactions
  **/
  @Schema(description = "")
  public MsgVpnCollectionsTransactions getTransactions() {
    return transactions;
  }

  public void setTransactions(MsgVpnCollectionsTransactions transactions) {
    this.transactions = transactions;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnCollections msgVpnCollections = (MsgVpnCollections) o;
    return Objects.equals(this.aclProfiles, msgVpnCollections.aclProfiles) &&
        Objects.equals(this.authenticationOauthProviders, msgVpnCollections.authenticationOauthProviders) &&
        Objects.equals(this.authorizationGroups, msgVpnCollections.authorizationGroups) &&
        Objects.equals(this.bridges, msgVpnCollections.bridges) &&
        Objects.equals(this.clientProfiles, msgVpnCollections.clientProfiles) &&
        Objects.equals(this.clientUsernames, msgVpnCollections.clientUsernames) &&
        Objects.equals(this.clients, msgVpnCollections.clients) &&
        Objects.equals(this.configSyncRemoteNodes, msgVpnCollections.configSyncRemoteNodes) &&
        Objects.equals(this.distributedCaches, msgVpnCollections.distributedCaches) &&
        Objects.equals(this.dmrBridges, msgVpnCollections.dmrBridges) &&
        Objects.equals(this.jndiConnectionFactories, msgVpnCollections.jndiConnectionFactories) &&
        Objects.equals(this.jndiQueues, msgVpnCollections.jndiQueues) &&
        Objects.equals(this.jndiTopics, msgVpnCollections.jndiTopics) &&
        Objects.equals(this.mqttRetainCaches, msgVpnCollections.mqttRetainCaches) &&
        Objects.equals(this.mqttSessions, msgVpnCollections.mqttSessions) &&
        Objects.equals(this.queueTemplates, msgVpnCollections.queueTemplates) &&
        Objects.equals(this.queues, msgVpnCollections.queues) &&
        Objects.equals(this.replayLogs, msgVpnCollections.replayLogs) &&
        Objects.equals(this.replicatedTopics, msgVpnCollections.replicatedTopics) &&
        Objects.equals(this.restDeliveryPoints, msgVpnCollections.restDeliveryPoints) &&
        Objects.equals(this.topicEndpointTemplates, msgVpnCollections.topicEndpointTemplates) &&
        Objects.equals(this.topicEndpoints, msgVpnCollections.topicEndpoints) &&
        Objects.equals(this.transactions, msgVpnCollections.transactions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(aclProfiles, authenticationOauthProviders, authorizationGroups, bridges, clientProfiles, clientUsernames, clients, configSyncRemoteNodes, distributedCaches, dmrBridges, jndiConnectionFactories, jndiQueues, jndiTopics, mqttRetainCaches, mqttSessions, queueTemplates, queues, replayLogs, replicatedTopics, restDeliveryPoints, topicEndpointTemplates, topicEndpoints, transactions);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnCollections {\n");
    
    sb.append("    aclProfiles: ").append(toIndentedString(aclProfiles)).append("\n");
    sb.append("    authenticationOauthProviders: ").append(toIndentedString(authenticationOauthProviders)).append("\n");
    sb.append("    authorizationGroups: ").append(toIndentedString(authorizationGroups)).append("\n");
    sb.append("    bridges: ").append(toIndentedString(bridges)).append("\n");
    sb.append("    clientProfiles: ").append(toIndentedString(clientProfiles)).append("\n");
    sb.append("    clientUsernames: ").append(toIndentedString(clientUsernames)).append("\n");
    sb.append("    clients: ").append(toIndentedString(clients)).append("\n");
    sb.append("    configSyncRemoteNodes: ").append(toIndentedString(configSyncRemoteNodes)).append("\n");
    sb.append("    distributedCaches: ").append(toIndentedString(distributedCaches)).append("\n");
    sb.append("    dmrBridges: ").append(toIndentedString(dmrBridges)).append("\n");
    sb.append("    jndiConnectionFactories: ").append(toIndentedString(jndiConnectionFactories)).append("\n");
    sb.append("    jndiQueues: ").append(toIndentedString(jndiQueues)).append("\n");
    sb.append("    jndiTopics: ").append(toIndentedString(jndiTopics)).append("\n");
    sb.append("    mqttRetainCaches: ").append(toIndentedString(mqttRetainCaches)).append("\n");
    sb.append("    mqttSessions: ").append(toIndentedString(mqttSessions)).append("\n");
    sb.append("    queueTemplates: ").append(toIndentedString(queueTemplates)).append("\n");
    sb.append("    queues: ").append(toIndentedString(queues)).append("\n");
    sb.append("    replayLogs: ").append(toIndentedString(replayLogs)).append("\n");
    sb.append("    replicatedTopics: ").append(toIndentedString(replicatedTopics)).append("\n");
    sb.append("    restDeliveryPoints: ").append(toIndentedString(restDeliveryPoints)).append("\n");
    sb.append("    topicEndpointTemplates: ").append(toIndentedString(topicEndpointTemplates)).append("\n");
    sb.append("    topicEndpoints: ").append(toIndentedString(topicEndpoints)).append("\n");
    sb.append("    transactions: ").append(toIndentedString(transactions)).append("\n");
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
