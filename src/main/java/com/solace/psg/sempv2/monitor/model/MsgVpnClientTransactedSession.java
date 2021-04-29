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
 * MsgVpnClientTransactedSession
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:57:21.016551900+01:00[Europe/London]")
public class MsgVpnClientTransactedSession {
  @SerializedName("clientName")
  private String clientName = null;

  @SerializedName("commitCount")
  private Long commitCount = null;

  @SerializedName("commitFailureCount")
  private Long commitFailureCount = null;

  @SerializedName("commitSuccessCount")
  private Long commitSuccessCount = null;

  @SerializedName("consumedMsgCount")
  private Long consumedMsgCount = null;

  @SerializedName("endFailFailureCount")
  private Long endFailFailureCount = null;

  @SerializedName("endFailSuccessCount")
  private Long endFailSuccessCount = null;

  @SerializedName("endFailureCount")
  private Long endFailureCount = null;

  @SerializedName("endRollbackFailureCount")
  private Long endRollbackFailureCount = null;

  @SerializedName("endRollbackSuccessCount")
  private Long endRollbackSuccessCount = null;

  @SerializedName("endSuccessCount")
  private Long endSuccessCount = null;

  @SerializedName("failureCount")
  private Long failureCount = null;

  @SerializedName("forgetFailureCount")
  private Long forgetFailureCount = null;

  @SerializedName("forgetSuccessCount")
  private Long forgetSuccessCount = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("onePhaseCommitFailureCount")
  private Long onePhaseCommitFailureCount = null;

  @SerializedName("onePhaseCommitSuccessCount")
  private Long onePhaseCommitSuccessCount = null;

  @SerializedName("pendingConsumedMsgCount")
  private Integer pendingConsumedMsgCount = null;

  @SerializedName("pendingPublishedMsgCount")
  private Integer pendingPublishedMsgCount = null;

  @SerializedName("prepareFailureCount")
  private Long prepareFailureCount = null;

  @SerializedName("prepareSuccessCount")
  private Long prepareSuccessCount = null;

  @SerializedName("previousTransactionState")
  private String previousTransactionState = null;

  @SerializedName("publishedMsgCount")
  private Long publishedMsgCount = null;

  @SerializedName("resumeFailureCount")
  private Long resumeFailureCount = null;

  @SerializedName("resumeSuccessCount")
  private Long resumeSuccessCount = null;

  @SerializedName("retrievedMsgCount")
  private Long retrievedMsgCount = null;

  @SerializedName("rollbackCount")
  private Long rollbackCount = null;

  @SerializedName("rollbackFailureCount")
  private Long rollbackFailureCount = null;

  @SerializedName("rollbackSuccessCount")
  private Long rollbackSuccessCount = null;

  @SerializedName("sessionName")
  private String sessionName = null;

  @SerializedName("spooledMsgCount")
  private Long spooledMsgCount = null;

  @SerializedName("startFailureCount")
  private Long startFailureCount = null;

  @SerializedName("startSuccessCount")
  private Long startSuccessCount = null;

  @SerializedName("successCount")
  private Long successCount = null;

  @SerializedName("suspendFailureCount")
  private Long suspendFailureCount = null;

  @SerializedName("suspendSuccessCount")
  private Long suspendSuccessCount = null;

  @SerializedName("transactionId")
  private Integer transactionId = null;

  @SerializedName("transactionState")
  private String transactionState = null;

  @SerializedName("twoPhaseCommitFailureCount")
  private Long twoPhaseCommitFailureCount = null;

  @SerializedName("twoPhaseCommitSuccessCount")
  private Long twoPhaseCommitSuccessCount = null;

  public MsgVpnClientTransactedSession clientName(String clientName) {
    this.clientName = clientName;
    return this;
  }

   /**
   * The name of the Client.
   * @return clientName
  **/
  @Schema(description = "The name of the Client.")
  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public MsgVpnClientTransactedSession commitCount(Long commitCount) {
    this.commitCount = commitCount;
    return this;
  }

   /**
   * The number of transactions committed within the Transacted Session.
   * @return commitCount
  **/
  @Schema(description = "The number of transactions committed within the Transacted Session.")
  public Long getCommitCount() {
    return commitCount;
  }

  public void setCommitCount(Long commitCount) {
    this.commitCount = commitCount;
  }

  public MsgVpnClientTransactedSession commitFailureCount(Long commitFailureCount) {
    this.commitFailureCount = commitFailureCount;
    return this;
  }

   /**
   * The number of transaction commit operations that failed.
   * @return commitFailureCount
  **/
  @Schema(description = "The number of transaction commit operations that failed.")
  public Long getCommitFailureCount() {
    return commitFailureCount;
  }

  public void setCommitFailureCount(Long commitFailureCount) {
    this.commitFailureCount = commitFailureCount;
  }

  public MsgVpnClientTransactedSession commitSuccessCount(Long commitSuccessCount) {
    this.commitSuccessCount = commitSuccessCount;
    return this;
  }

   /**
   * The number of transaction commit operations that succeeded.
   * @return commitSuccessCount
  **/
  @Schema(description = "The number of transaction commit operations that succeeded.")
  public Long getCommitSuccessCount() {
    return commitSuccessCount;
  }

  public void setCommitSuccessCount(Long commitSuccessCount) {
    this.commitSuccessCount = commitSuccessCount;
  }

  public MsgVpnClientTransactedSession consumedMsgCount(Long consumedMsgCount) {
    this.consumedMsgCount = consumedMsgCount;
    return this;
  }

   /**
   * The number of messages consumed within the Transacted Session.
   * @return consumedMsgCount
  **/
  @Schema(description = "The number of messages consumed within the Transacted Session.")
  public Long getConsumedMsgCount() {
    return consumedMsgCount;
  }

  public void setConsumedMsgCount(Long consumedMsgCount) {
    this.consumedMsgCount = consumedMsgCount;
  }

  public MsgVpnClientTransactedSession endFailFailureCount(Long endFailFailureCount) {
    this.endFailFailureCount = endFailFailureCount;
    return this;
  }

   /**
   * The number of transaction end fail operations that failed.
   * @return endFailFailureCount
  **/
  @Schema(description = "The number of transaction end fail operations that failed.")
  public Long getEndFailFailureCount() {
    return endFailFailureCount;
  }

  public void setEndFailFailureCount(Long endFailFailureCount) {
    this.endFailFailureCount = endFailFailureCount;
  }

  public MsgVpnClientTransactedSession endFailSuccessCount(Long endFailSuccessCount) {
    this.endFailSuccessCount = endFailSuccessCount;
    return this;
  }

   /**
   * The number of transaction end fail operations that succeeded.
   * @return endFailSuccessCount
  **/
  @Schema(description = "The number of transaction end fail operations that succeeded.")
  public Long getEndFailSuccessCount() {
    return endFailSuccessCount;
  }

  public void setEndFailSuccessCount(Long endFailSuccessCount) {
    this.endFailSuccessCount = endFailSuccessCount;
  }

  public MsgVpnClientTransactedSession endFailureCount(Long endFailureCount) {
    this.endFailureCount = endFailureCount;
    return this;
  }

   /**
   * The number of transaction end operations that failed.
   * @return endFailureCount
  **/
  @Schema(description = "The number of transaction end operations that failed.")
  public Long getEndFailureCount() {
    return endFailureCount;
  }

  public void setEndFailureCount(Long endFailureCount) {
    this.endFailureCount = endFailureCount;
  }

  public MsgVpnClientTransactedSession endRollbackFailureCount(Long endRollbackFailureCount) {
    this.endRollbackFailureCount = endRollbackFailureCount;
    return this;
  }

   /**
   * The number of transaction end rollback operations that failed.
   * @return endRollbackFailureCount
  **/
  @Schema(description = "The number of transaction end rollback operations that failed.")
  public Long getEndRollbackFailureCount() {
    return endRollbackFailureCount;
  }

  public void setEndRollbackFailureCount(Long endRollbackFailureCount) {
    this.endRollbackFailureCount = endRollbackFailureCount;
  }

  public MsgVpnClientTransactedSession endRollbackSuccessCount(Long endRollbackSuccessCount) {
    this.endRollbackSuccessCount = endRollbackSuccessCount;
    return this;
  }

   /**
   * The number of transaction end rollback operations that succeeded.
   * @return endRollbackSuccessCount
  **/
  @Schema(description = "The number of transaction end rollback operations that succeeded.")
  public Long getEndRollbackSuccessCount() {
    return endRollbackSuccessCount;
  }

  public void setEndRollbackSuccessCount(Long endRollbackSuccessCount) {
    this.endRollbackSuccessCount = endRollbackSuccessCount;
  }

  public MsgVpnClientTransactedSession endSuccessCount(Long endSuccessCount) {
    this.endSuccessCount = endSuccessCount;
    return this;
  }

   /**
   * The number of transaction end operations that succeeded.
   * @return endSuccessCount
  **/
  @Schema(description = "The number of transaction end operations that succeeded.")
  public Long getEndSuccessCount() {
    return endSuccessCount;
  }

  public void setEndSuccessCount(Long endSuccessCount) {
    this.endSuccessCount = endSuccessCount;
  }

  public MsgVpnClientTransactedSession failureCount(Long failureCount) {
    this.failureCount = failureCount;
    return this;
  }

   /**
   * The number of transactions that failed within the Transacted Session.
   * @return failureCount
  **/
  @Schema(description = "The number of transactions that failed within the Transacted Session.")
  public Long getFailureCount() {
    return failureCount;
  }

  public void setFailureCount(Long failureCount) {
    this.failureCount = failureCount;
  }

  public MsgVpnClientTransactedSession forgetFailureCount(Long forgetFailureCount) {
    this.forgetFailureCount = forgetFailureCount;
    return this;
  }

   /**
   * The number of transaction forget operations that failed.
   * @return forgetFailureCount
  **/
  @Schema(description = "The number of transaction forget operations that failed.")
  public Long getForgetFailureCount() {
    return forgetFailureCount;
  }

  public void setForgetFailureCount(Long forgetFailureCount) {
    this.forgetFailureCount = forgetFailureCount;
  }

  public MsgVpnClientTransactedSession forgetSuccessCount(Long forgetSuccessCount) {
    this.forgetSuccessCount = forgetSuccessCount;
    return this;
  }

   /**
   * The number of transaction forget operations that succeeded.
   * @return forgetSuccessCount
  **/
  @Schema(description = "The number of transaction forget operations that succeeded.")
  public Long getForgetSuccessCount() {
    return forgetSuccessCount;
  }

  public void setForgetSuccessCount(Long forgetSuccessCount) {
    this.forgetSuccessCount = forgetSuccessCount;
  }

  public MsgVpnClientTransactedSession msgVpnName(String msgVpnName) {
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

  public MsgVpnClientTransactedSession onePhaseCommitFailureCount(Long onePhaseCommitFailureCount) {
    this.onePhaseCommitFailureCount = onePhaseCommitFailureCount;
    return this;
  }

   /**
   * The number of transaction one-phase commit operations that failed.
   * @return onePhaseCommitFailureCount
  **/
  @Schema(description = "The number of transaction one-phase commit operations that failed.")
  public Long getOnePhaseCommitFailureCount() {
    return onePhaseCommitFailureCount;
  }

  public void setOnePhaseCommitFailureCount(Long onePhaseCommitFailureCount) {
    this.onePhaseCommitFailureCount = onePhaseCommitFailureCount;
  }

  public MsgVpnClientTransactedSession onePhaseCommitSuccessCount(Long onePhaseCommitSuccessCount) {
    this.onePhaseCommitSuccessCount = onePhaseCommitSuccessCount;
    return this;
  }

   /**
   * The number of transaction one-phase commit operations that succeeded.
   * @return onePhaseCommitSuccessCount
  **/
  @Schema(description = "The number of transaction one-phase commit operations that succeeded.")
  public Long getOnePhaseCommitSuccessCount() {
    return onePhaseCommitSuccessCount;
  }

  public void setOnePhaseCommitSuccessCount(Long onePhaseCommitSuccessCount) {
    this.onePhaseCommitSuccessCount = onePhaseCommitSuccessCount;
  }

  public MsgVpnClientTransactedSession pendingConsumedMsgCount(Integer pendingConsumedMsgCount) {
    this.pendingConsumedMsgCount = pendingConsumedMsgCount;
    return this;
  }

   /**
   * The number of messages to be consumed when the transaction is committed.
   * @return pendingConsumedMsgCount
  **/
  @Schema(description = "The number of messages to be consumed when the transaction is committed.")
  public Integer getPendingConsumedMsgCount() {
    return pendingConsumedMsgCount;
  }

  public void setPendingConsumedMsgCount(Integer pendingConsumedMsgCount) {
    this.pendingConsumedMsgCount = pendingConsumedMsgCount;
  }

  public MsgVpnClientTransactedSession pendingPublishedMsgCount(Integer pendingPublishedMsgCount) {
    this.pendingPublishedMsgCount = pendingPublishedMsgCount;
    return this;
  }

   /**
   * The number of messages to be published when the transaction is committed.
   * @return pendingPublishedMsgCount
  **/
  @Schema(description = "The number of messages to be published when the transaction is committed.")
  public Integer getPendingPublishedMsgCount() {
    return pendingPublishedMsgCount;
  }

  public void setPendingPublishedMsgCount(Integer pendingPublishedMsgCount) {
    this.pendingPublishedMsgCount = pendingPublishedMsgCount;
  }

  public MsgVpnClientTransactedSession prepareFailureCount(Long prepareFailureCount) {
    this.prepareFailureCount = prepareFailureCount;
    return this;
  }

   /**
   * The number of transaction prepare operations that failed.
   * @return prepareFailureCount
  **/
  @Schema(description = "The number of transaction prepare operations that failed.")
  public Long getPrepareFailureCount() {
    return prepareFailureCount;
  }

  public void setPrepareFailureCount(Long prepareFailureCount) {
    this.prepareFailureCount = prepareFailureCount;
  }

  public MsgVpnClientTransactedSession prepareSuccessCount(Long prepareSuccessCount) {
    this.prepareSuccessCount = prepareSuccessCount;
    return this;
  }

   /**
   * The number of transaction prepare operations that succeeded.
   * @return prepareSuccessCount
  **/
  @Schema(description = "The number of transaction prepare operations that succeeded.")
  public Long getPrepareSuccessCount() {
    return prepareSuccessCount;
  }

  public void setPrepareSuccessCount(Long prepareSuccessCount) {
    this.prepareSuccessCount = prepareSuccessCount;
  }

  public MsgVpnClientTransactedSession previousTransactionState(String previousTransactionState) {
    this.previousTransactionState = previousTransactionState;
    return this;
  }

   /**
   * The state of the previous transaction. The allowed values and their meaning are:  &lt;pre&gt; \&quot;none\&quot; - The previous transaction had no state. \&quot;committed\&quot; - The previous transaction was committed. \&quot;rolled-back\&quot; - The previous transaction was rolled back. \&quot;failed\&quot; - The previous transaction failed. &lt;/pre&gt; 
   * @return previousTransactionState
  **/
  @Schema(description = "The state of the previous transaction. The allowed values and their meaning are:  <pre> \"none\" - The previous transaction had no state. \"committed\" - The previous transaction was committed. \"rolled-back\" - The previous transaction was rolled back. \"failed\" - The previous transaction failed. </pre> ")
  public String getPreviousTransactionState() {
    return previousTransactionState;
  }

  public void setPreviousTransactionState(String previousTransactionState) {
    this.previousTransactionState = previousTransactionState;
  }

  public MsgVpnClientTransactedSession publishedMsgCount(Long publishedMsgCount) {
    this.publishedMsgCount = publishedMsgCount;
    return this;
  }

   /**
   * The number of messages published within the Transacted Session.
   * @return publishedMsgCount
  **/
  @Schema(description = "The number of messages published within the Transacted Session.")
  public Long getPublishedMsgCount() {
    return publishedMsgCount;
  }

  public void setPublishedMsgCount(Long publishedMsgCount) {
    this.publishedMsgCount = publishedMsgCount;
  }

  public MsgVpnClientTransactedSession resumeFailureCount(Long resumeFailureCount) {
    this.resumeFailureCount = resumeFailureCount;
    return this;
  }

   /**
   * The number of transaction resume operations that failed.
   * @return resumeFailureCount
  **/
  @Schema(description = "The number of transaction resume operations that failed.")
  public Long getResumeFailureCount() {
    return resumeFailureCount;
  }

  public void setResumeFailureCount(Long resumeFailureCount) {
    this.resumeFailureCount = resumeFailureCount;
  }

  public MsgVpnClientTransactedSession resumeSuccessCount(Long resumeSuccessCount) {
    this.resumeSuccessCount = resumeSuccessCount;
    return this;
  }

   /**
   * The number of transaction resume operations that succeeded.
   * @return resumeSuccessCount
  **/
  @Schema(description = "The number of transaction resume operations that succeeded.")
  public Long getResumeSuccessCount() {
    return resumeSuccessCount;
  }

  public void setResumeSuccessCount(Long resumeSuccessCount) {
    this.resumeSuccessCount = resumeSuccessCount;
  }

  public MsgVpnClientTransactedSession retrievedMsgCount(Long retrievedMsgCount) {
    this.retrievedMsgCount = retrievedMsgCount;
    return this;
  }

   /**
   * The number of messages retrieved within the Transacted Session.
   * @return retrievedMsgCount
  **/
  @Schema(description = "The number of messages retrieved within the Transacted Session.")
  public Long getRetrievedMsgCount() {
    return retrievedMsgCount;
  }

  public void setRetrievedMsgCount(Long retrievedMsgCount) {
    this.retrievedMsgCount = retrievedMsgCount;
  }

  public MsgVpnClientTransactedSession rollbackCount(Long rollbackCount) {
    this.rollbackCount = rollbackCount;
    return this;
  }

   /**
   * The number of transactions rolled back within the Transacted Session.
   * @return rollbackCount
  **/
  @Schema(description = "The number of transactions rolled back within the Transacted Session.")
  public Long getRollbackCount() {
    return rollbackCount;
  }

  public void setRollbackCount(Long rollbackCount) {
    this.rollbackCount = rollbackCount;
  }

  public MsgVpnClientTransactedSession rollbackFailureCount(Long rollbackFailureCount) {
    this.rollbackFailureCount = rollbackFailureCount;
    return this;
  }

   /**
   * The number of transaction rollback operations that failed.
   * @return rollbackFailureCount
  **/
  @Schema(description = "The number of transaction rollback operations that failed.")
  public Long getRollbackFailureCount() {
    return rollbackFailureCount;
  }

  public void setRollbackFailureCount(Long rollbackFailureCount) {
    this.rollbackFailureCount = rollbackFailureCount;
  }

  public MsgVpnClientTransactedSession rollbackSuccessCount(Long rollbackSuccessCount) {
    this.rollbackSuccessCount = rollbackSuccessCount;
    return this;
  }

   /**
   * The number of transaction rollback operations that succeeded.
   * @return rollbackSuccessCount
  **/
  @Schema(description = "The number of transaction rollback operations that succeeded.")
  public Long getRollbackSuccessCount() {
    return rollbackSuccessCount;
  }

  public void setRollbackSuccessCount(Long rollbackSuccessCount) {
    this.rollbackSuccessCount = rollbackSuccessCount;
  }

  public MsgVpnClientTransactedSession sessionName(String sessionName) {
    this.sessionName = sessionName;
    return this;
  }

   /**
   * The name of the Transacted Session.
   * @return sessionName
  **/
  @Schema(description = "The name of the Transacted Session.")
  public String getSessionName() {
    return sessionName;
  }

  public void setSessionName(String sessionName) {
    this.sessionName = sessionName;
  }

  public MsgVpnClientTransactedSession spooledMsgCount(Long spooledMsgCount) {
    this.spooledMsgCount = spooledMsgCount;
    return this;
  }

   /**
   * The number of messages spooled within the Transacted Session.
   * @return spooledMsgCount
  **/
  @Schema(description = "The number of messages spooled within the Transacted Session.")
  public Long getSpooledMsgCount() {
    return spooledMsgCount;
  }

  public void setSpooledMsgCount(Long spooledMsgCount) {
    this.spooledMsgCount = spooledMsgCount;
  }

  public MsgVpnClientTransactedSession startFailureCount(Long startFailureCount) {
    this.startFailureCount = startFailureCount;
    return this;
  }

   /**
   * The number of transaction start operations that failed.
   * @return startFailureCount
  **/
  @Schema(description = "The number of transaction start operations that failed.")
  public Long getStartFailureCount() {
    return startFailureCount;
  }

  public void setStartFailureCount(Long startFailureCount) {
    this.startFailureCount = startFailureCount;
  }

  public MsgVpnClientTransactedSession startSuccessCount(Long startSuccessCount) {
    this.startSuccessCount = startSuccessCount;
    return this;
  }

   /**
   * The number of transaction start operations that succeeded.
   * @return startSuccessCount
  **/
  @Schema(description = "The number of transaction start operations that succeeded.")
  public Long getStartSuccessCount() {
    return startSuccessCount;
  }

  public void setStartSuccessCount(Long startSuccessCount) {
    this.startSuccessCount = startSuccessCount;
  }

  public MsgVpnClientTransactedSession successCount(Long successCount) {
    this.successCount = successCount;
    return this;
  }

   /**
   * The number of transactions that succeeded within the Transacted Session.
   * @return successCount
  **/
  @Schema(description = "The number of transactions that succeeded within the Transacted Session.")
  public Long getSuccessCount() {
    return successCount;
  }

  public void setSuccessCount(Long successCount) {
    this.successCount = successCount;
  }

  public MsgVpnClientTransactedSession suspendFailureCount(Long suspendFailureCount) {
    this.suspendFailureCount = suspendFailureCount;
    return this;
  }

   /**
   * The number of transaction suspend operations that failed.
   * @return suspendFailureCount
  **/
  @Schema(description = "The number of transaction suspend operations that failed.")
  public Long getSuspendFailureCount() {
    return suspendFailureCount;
  }

  public void setSuspendFailureCount(Long suspendFailureCount) {
    this.suspendFailureCount = suspendFailureCount;
  }

  public MsgVpnClientTransactedSession suspendSuccessCount(Long suspendSuccessCount) {
    this.suspendSuccessCount = suspendSuccessCount;
    return this;
  }

   /**
   * The number of transaction suspend operations that succeeded.
   * @return suspendSuccessCount
  **/
  @Schema(description = "The number of transaction suspend operations that succeeded.")
  public Long getSuspendSuccessCount() {
    return suspendSuccessCount;
  }

  public void setSuspendSuccessCount(Long suspendSuccessCount) {
    this.suspendSuccessCount = suspendSuccessCount;
  }

  public MsgVpnClientTransactedSession transactionId(Integer transactionId) {
    this.transactionId = transactionId;
    return this;
  }

   /**
   * The identifier (ID) of the transaction in the Transacted Session.
   * @return transactionId
  **/
  @Schema(description = "The identifier (ID) of the transaction in the Transacted Session.")
  public Integer getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(Integer transactionId) {
    this.transactionId = transactionId;
  }

  public MsgVpnClientTransactedSession transactionState(String transactionState) {
    this.transactionState = transactionState;
    return this;
  }

   /**
   * The state of the current transaction. The allowed values and their meaning are:  &lt;pre&gt; \&quot;in-progress\&quot; - The current transaction is in progress. \&quot;committing\&quot; - The current transaction is committing. \&quot;rolling-back\&quot; - The current transaction is rolling back. \&quot;failing\&quot; - The current transaction is failing. &lt;/pre&gt; 
   * @return transactionState
  **/
  @Schema(description = "The state of the current transaction. The allowed values and their meaning are:  <pre> \"in-progress\" - The current transaction is in progress. \"committing\" - The current transaction is committing. \"rolling-back\" - The current transaction is rolling back. \"failing\" - The current transaction is failing. </pre> ")
  public String getTransactionState() {
    return transactionState;
  }

  public void setTransactionState(String transactionState) {
    this.transactionState = transactionState;
  }

  public MsgVpnClientTransactedSession twoPhaseCommitFailureCount(Long twoPhaseCommitFailureCount) {
    this.twoPhaseCommitFailureCount = twoPhaseCommitFailureCount;
    return this;
  }

   /**
   * The number of transaction two-phase commit operations that failed.
   * @return twoPhaseCommitFailureCount
  **/
  @Schema(description = "The number of transaction two-phase commit operations that failed.")
  public Long getTwoPhaseCommitFailureCount() {
    return twoPhaseCommitFailureCount;
  }

  public void setTwoPhaseCommitFailureCount(Long twoPhaseCommitFailureCount) {
    this.twoPhaseCommitFailureCount = twoPhaseCommitFailureCount;
  }

  public MsgVpnClientTransactedSession twoPhaseCommitSuccessCount(Long twoPhaseCommitSuccessCount) {
    this.twoPhaseCommitSuccessCount = twoPhaseCommitSuccessCount;
    return this;
  }

   /**
   * The number of transaction two-phase commit operations that succeeded.
   * @return twoPhaseCommitSuccessCount
  **/
  @Schema(description = "The number of transaction two-phase commit operations that succeeded.")
  public Long getTwoPhaseCommitSuccessCount() {
    return twoPhaseCommitSuccessCount;
  }

  public void setTwoPhaseCommitSuccessCount(Long twoPhaseCommitSuccessCount) {
    this.twoPhaseCommitSuccessCount = twoPhaseCommitSuccessCount;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnClientTransactedSession msgVpnClientTransactedSession = (MsgVpnClientTransactedSession) o;
    return Objects.equals(this.clientName, msgVpnClientTransactedSession.clientName) &&
        Objects.equals(this.commitCount, msgVpnClientTransactedSession.commitCount) &&
        Objects.equals(this.commitFailureCount, msgVpnClientTransactedSession.commitFailureCount) &&
        Objects.equals(this.commitSuccessCount, msgVpnClientTransactedSession.commitSuccessCount) &&
        Objects.equals(this.consumedMsgCount, msgVpnClientTransactedSession.consumedMsgCount) &&
        Objects.equals(this.endFailFailureCount, msgVpnClientTransactedSession.endFailFailureCount) &&
        Objects.equals(this.endFailSuccessCount, msgVpnClientTransactedSession.endFailSuccessCount) &&
        Objects.equals(this.endFailureCount, msgVpnClientTransactedSession.endFailureCount) &&
        Objects.equals(this.endRollbackFailureCount, msgVpnClientTransactedSession.endRollbackFailureCount) &&
        Objects.equals(this.endRollbackSuccessCount, msgVpnClientTransactedSession.endRollbackSuccessCount) &&
        Objects.equals(this.endSuccessCount, msgVpnClientTransactedSession.endSuccessCount) &&
        Objects.equals(this.failureCount, msgVpnClientTransactedSession.failureCount) &&
        Objects.equals(this.forgetFailureCount, msgVpnClientTransactedSession.forgetFailureCount) &&
        Objects.equals(this.forgetSuccessCount, msgVpnClientTransactedSession.forgetSuccessCount) &&
        Objects.equals(this.msgVpnName, msgVpnClientTransactedSession.msgVpnName) &&
        Objects.equals(this.onePhaseCommitFailureCount, msgVpnClientTransactedSession.onePhaseCommitFailureCount) &&
        Objects.equals(this.onePhaseCommitSuccessCount, msgVpnClientTransactedSession.onePhaseCommitSuccessCount) &&
        Objects.equals(this.pendingConsumedMsgCount, msgVpnClientTransactedSession.pendingConsumedMsgCount) &&
        Objects.equals(this.pendingPublishedMsgCount, msgVpnClientTransactedSession.pendingPublishedMsgCount) &&
        Objects.equals(this.prepareFailureCount, msgVpnClientTransactedSession.prepareFailureCount) &&
        Objects.equals(this.prepareSuccessCount, msgVpnClientTransactedSession.prepareSuccessCount) &&
        Objects.equals(this.previousTransactionState, msgVpnClientTransactedSession.previousTransactionState) &&
        Objects.equals(this.publishedMsgCount, msgVpnClientTransactedSession.publishedMsgCount) &&
        Objects.equals(this.resumeFailureCount, msgVpnClientTransactedSession.resumeFailureCount) &&
        Objects.equals(this.resumeSuccessCount, msgVpnClientTransactedSession.resumeSuccessCount) &&
        Objects.equals(this.retrievedMsgCount, msgVpnClientTransactedSession.retrievedMsgCount) &&
        Objects.equals(this.rollbackCount, msgVpnClientTransactedSession.rollbackCount) &&
        Objects.equals(this.rollbackFailureCount, msgVpnClientTransactedSession.rollbackFailureCount) &&
        Objects.equals(this.rollbackSuccessCount, msgVpnClientTransactedSession.rollbackSuccessCount) &&
        Objects.equals(this.sessionName, msgVpnClientTransactedSession.sessionName) &&
        Objects.equals(this.spooledMsgCount, msgVpnClientTransactedSession.spooledMsgCount) &&
        Objects.equals(this.startFailureCount, msgVpnClientTransactedSession.startFailureCount) &&
        Objects.equals(this.startSuccessCount, msgVpnClientTransactedSession.startSuccessCount) &&
        Objects.equals(this.successCount, msgVpnClientTransactedSession.successCount) &&
        Objects.equals(this.suspendFailureCount, msgVpnClientTransactedSession.suspendFailureCount) &&
        Objects.equals(this.suspendSuccessCount, msgVpnClientTransactedSession.suspendSuccessCount) &&
        Objects.equals(this.transactionId, msgVpnClientTransactedSession.transactionId) &&
        Objects.equals(this.transactionState, msgVpnClientTransactedSession.transactionState) &&
        Objects.equals(this.twoPhaseCommitFailureCount, msgVpnClientTransactedSession.twoPhaseCommitFailureCount) &&
        Objects.equals(this.twoPhaseCommitSuccessCount, msgVpnClientTransactedSession.twoPhaseCommitSuccessCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clientName, commitCount, commitFailureCount, commitSuccessCount, consumedMsgCount, endFailFailureCount, endFailSuccessCount, endFailureCount, endRollbackFailureCount, endRollbackSuccessCount, endSuccessCount, failureCount, forgetFailureCount, forgetSuccessCount, msgVpnName, onePhaseCommitFailureCount, onePhaseCommitSuccessCount, pendingConsumedMsgCount, pendingPublishedMsgCount, prepareFailureCount, prepareSuccessCount, previousTransactionState, publishedMsgCount, resumeFailureCount, resumeSuccessCount, retrievedMsgCount, rollbackCount, rollbackFailureCount, rollbackSuccessCount, sessionName, spooledMsgCount, startFailureCount, startSuccessCount, successCount, suspendFailureCount, suspendSuccessCount, transactionId, transactionState, twoPhaseCommitFailureCount, twoPhaseCommitSuccessCount);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnClientTransactedSession {\n");
    
    sb.append("    clientName: ").append(toIndentedString(clientName)).append("\n");
    sb.append("    commitCount: ").append(toIndentedString(commitCount)).append("\n");
    sb.append("    commitFailureCount: ").append(toIndentedString(commitFailureCount)).append("\n");
    sb.append("    commitSuccessCount: ").append(toIndentedString(commitSuccessCount)).append("\n");
    sb.append("    consumedMsgCount: ").append(toIndentedString(consumedMsgCount)).append("\n");
    sb.append("    endFailFailureCount: ").append(toIndentedString(endFailFailureCount)).append("\n");
    sb.append("    endFailSuccessCount: ").append(toIndentedString(endFailSuccessCount)).append("\n");
    sb.append("    endFailureCount: ").append(toIndentedString(endFailureCount)).append("\n");
    sb.append("    endRollbackFailureCount: ").append(toIndentedString(endRollbackFailureCount)).append("\n");
    sb.append("    endRollbackSuccessCount: ").append(toIndentedString(endRollbackSuccessCount)).append("\n");
    sb.append("    endSuccessCount: ").append(toIndentedString(endSuccessCount)).append("\n");
    sb.append("    failureCount: ").append(toIndentedString(failureCount)).append("\n");
    sb.append("    forgetFailureCount: ").append(toIndentedString(forgetFailureCount)).append("\n");
    sb.append("    forgetSuccessCount: ").append(toIndentedString(forgetSuccessCount)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    onePhaseCommitFailureCount: ").append(toIndentedString(onePhaseCommitFailureCount)).append("\n");
    sb.append("    onePhaseCommitSuccessCount: ").append(toIndentedString(onePhaseCommitSuccessCount)).append("\n");
    sb.append("    pendingConsumedMsgCount: ").append(toIndentedString(pendingConsumedMsgCount)).append("\n");
    sb.append("    pendingPublishedMsgCount: ").append(toIndentedString(pendingPublishedMsgCount)).append("\n");
    sb.append("    prepareFailureCount: ").append(toIndentedString(prepareFailureCount)).append("\n");
    sb.append("    prepareSuccessCount: ").append(toIndentedString(prepareSuccessCount)).append("\n");
    sb.append("    previousTransactionState: ").append(toIndentedString(previousTransactionState)).append("\n");
    sb.append("    publishedMsgCount: ").append(toIndentedString(publishedMsgCount)).append("\n");
    sb.append("    resumeFailureCount: ").append(toIndentedString(resumeFailureCount)).append("\n");
    sb.append("    resumeSuccessCount: ").append(toIndentedString(resumeSuccessCount)).append("\n");
    sb.append("    retrievedMsgCount: ").append(toIndentedString(retrievedMsgCount)).append("\n");
    sb.append("    rollbackCount: ").append(toIndentedString(rollbackCount)).append("\n");
    sb.append("    rollbackFailureCount: ").append(toIndentedString(rollbackFailureCount)).append("\n");
    sb.append("    rollbackSuccessCount: ").append(toIndentedString(rollbackSuccessCount)).append("\n");
    sb.append("    sessionName: ").append(toIndentedString(sessionName)).append("\n");
    sb.append("    spooledMsgCount: ").append(toIndentedString(spooledMsgCount)).append("\n");
    sb.append("    startFailureCount: ").append(toIndentedString(startFailureCount)).append("\n");
    sb.append("    startSuccessCount: ").append(toIndentedString(startSuccessCount)).append("\n");
    sb.append("    successCount: ").append(toIndentedString(successCount)).append("\n");
    sb.append("    suspendFailureCount: ").append(toIndentedString(suspendFailureCount)).append("\n");
    sb.append("    suspendSuccessCount: ").append(toIndentedString(suspendSuccessCount)).append("\n");
    sb.append("    transactionId: ").append(toIndentedString(transactionId)).append("\n");
    sb.append("    transactionState: ").append(toIndentedString(transactionState)).append("\n");
    sb.append("    twoPhaseCommitFailureCount: ").append(toIndentedString(twoPhaseCommitFailureCount)).append("\n");
    sb.append("    twoPhaseCommitSuccessCount: ").append(toIndentedString(twoPhaseCommitSuccessCount)).append("\n");
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
