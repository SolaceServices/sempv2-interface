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
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * MsgVpnJndiConnectionFactory
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2021-04-29T21:49:16.603913+01:00[Europe/London]")
public class MsgVpnJndiConnectionFactory {
  @SerializedName("allowDuplicateClientIdEnabled")
  private Boolean allowDuplicateClientIdEnabled = null;

  @SerializedName("clientDescription")
  private String clientDescription = null;

  @SerializedName("clientId")
  private String clientId = null;

  @SerializedName("connectionFactoryName")
  private String connectionFactoryName = null;

  @SerializedName("dtoReceiveOverrideEnabled")
  private Boolean dtoReceiveOverrideEnabled = null;

  @SerializedName("dtoReceiveSubscriberLocalPriority")
  private Integer dtoReceiveSubscriberLocalPriority = null;

  @SerializedName("dtoReceiveSubscriberNetworkPriority")
  private Integer dtoReceiveSubscriberNetworkPriority = null;

  @SerializedName("dtoSendEnabled")
  private Boolean dtoSendEnabled = null;

  @SerializedName("dynamicEndpointCreateDurableEnabled")
  private Boolean dynamicEndpointCreateDurableEnabled = null;

  @SerializedName("dynamicEndpointRespectTtlEnabled")
  private Boolean dynamicEndpointRespectTtlEnabled = null;

  @SerializedName("guaranteedReceiveAckTimeout")
  private Integer guaranteedReceiveAckTimeout = null;

  @SerializedName("guaranteedReceiveReconnectRetryCount")
  private Integer guaranteedReceiveReconnectRetryCount = null;

  @SerializedName("guaranteedReceiveReconnectRetryWait")
  private Integer guaranteedReceiveReconnectRetryWait = null;

  @SerializedName("guaranteedReceiveWindowSize")
  private Integer guaranteedReceiveWindowSize = null;

  @SerializedName("guaranteedReceiveWindowSizeAckThreshold")
  private Integer guaranteedReceiveWindowSizeAckThreshold = null;

  @SerializedName("guaranteedSendAckTimeout")
  private Integer guaranteedSendAckTimeout = null;

  @SerializedName("guaranteedSendWindowSize")
  private Integer guaranteedSendWindowSize = null;

  /**
   * The default delivery mode for messages sent by the Publisher (Producer). The default value is &#x60;\&quot;persistent\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;persistent\&quot; - The broker spools messages (persists in the Message Spool) as part of the send operation. \&quot;non-persistent\&quot; - The broker does not spool messages (does not persist in the Message Spool) as part of the send operation. &lt;/pre&gt; 
   */
  @JsonAdapter(MessagingDefaultDeliveryModeEnum.Adapter.class)
  public enum MessagingDefaultDeliveryModeEnum {
    PERSISTENT("persistent"),
    NON_PERSISTENT("non-persistent");

    private String value;

    MessagingDefaultDeliveryModeEnum(String value) {
      this.value = value;
    }
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }
    public static MessagingDefaultDeliveryModeEnum fromValue(String text) {
      for (MessagingDefaultDeliveryModeEnum b : MessagingDefaultDeliveryModeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
    public static class Adapter extends TypeAdapter<MessagingDefaultDeliveryModeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final MessagingDefaultDeliveryModeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public MessagingDefaultDeliveryModeEnum read(final JsonReader jsonReader) throws IOException {
        Object value = jsonReader.nextString();
        return MessagingDefaultDeliveryModeEnum.fromValue(String.valueOf(value));
      }
    }
  }  @SerializedName("messagingDefaultDeliveryMode")
  private MessagingDefaultDeliveryModeEnum messagingDefaultDeliveryMode = null;

  @SerializedName("messagingDefaultDmqEligibleEnabled")
  private Boolean messagingDefaultDmqEligibleEnabled = null;

  @SerializedName("messagingDefaultElidingEligibleEnabled")
  private Boolean messagingDefaultElidingEligibleEnabled = null;

  @SerializedName("messagingJmsxUserIdEnabled")
  private Boolean messagingJmsxUserIdEnabled = null;

  @SerializedName("messagingTextInXmlPayloadEnabled")
  private Boolean messagingTextInXmlPayloadEnabled = null;

  @SerializedName("msgVpnName")
  private String msgVpnName = null;

  @SerializedName("transportCompressionLevel")
  private Integer transportCompressionLevel = null;

  @SerializedName("transportConnectRetryCount")
  private Integer transportConnectRetryCount = null;

  @SerializedName("transportConnectRetryPerHostCount")
  private Integer transportConnectRetryPerHostCount = null;

  @SerializedName("transportConnectTimeout")
  private Integer transportConnectTimeout = null;

  @SerializedName("transportDirectTransportEnabled")
  private Boolean transportDirectTransportEnabled = null;

  @SerializedName("transportKeepaliveCount")
  private Integer transportKeepaliveCount = null;

  @SerializedName("transportKeepaliveEnabled")
  private Boolean transportKeepaliveEnabled = null;

  @SerializedName("transportKeepaliveInterval")
  private Integer transportKeepaliveInterval = null;

  @SerializedName("transportMsgCallbackOnIoThreadEnabled")
  private Boolean transportMsgCallbackOnIoThreadEnabled = null;

  @SerializedName("transportOptimizeDirectEnabled")
  private Boolean transportOptimizeDirectEnabled = null;

  @SerializedName("transportPort")
  private Integer transportPort = null;

  @SerializedName("transportReadTimeout")
  private Integer transportReadTimeout = null;

  @SerializedName("transportReceiveBufferSize")
  private Integer transportReceiveBufferSize = null;

  @SerializedName("transportReconnectRetryCount")
  private Integer transportReconnectRetryCount = null;

  @SerializedName("transportReconnectRetryWait")
  private Integer transportReconnectRetryWait = null;

  @SerializedName("transportSendBufferSize")
  private Integer transportSendBufferSize = null;

  @SerializedName("transportTcpNoDelayEnabled")
  private Boolean transportTcpNoDelayEnabled = null;

  @SerializedName("xaEnabled")
  private Boolean xaEnabled = null;

  public MsgVpnJndiConnectionFactory allowDuplicateClientIdEnabled(Boolean allowDuplicateClientIdEnabled) {
    this.allowDuplicateClientIdEnabled = allowDuplicateClientIdEnabled;
    return this;
  }

   /**
   * Enable or disable whether new JMS connections can use the same Client identifier (ID) as an existing connection. The default value is &#x60;false&#x60;. Available since 2.3.
   * @return allowDuplicateClientIdEnabled
  **/
  @Schema(description = "Enable or disable whether new JMS connections can use the same Client identifier (ID) as an existing connection. The default value is `false`. Available since 2.3.")
  public Boolean isAllowDuplicateClientIdEnabled() {
    return allowDuplicateClientIdEnabled;
  }

  public void setAllowDuplicateClientIdEnabled(Boolean allowDuplicateClientIdEnabled) {
    this.allowDuplicateClientIdEnabled = allowDuplicateClientIdEnabled;
  }

  public MsgVpnJndiConnectionFactory clientDescription(String clientDescription) {
    this.clientDescription = clientDescription;
    return this;
  }

   /**
   * The description of the Client. The default value is &#x60;\&quot;\&quot;&#x60;.
   * @return clientDescription
  **/
  @Schema(description = "The description of the Client. The default value is `\"\"`.")
  public String getClientDescription() {
    return clientDescription;
  }

  public void setClientDescription(String clientDescription) {
    this.clientDescription = clientDescription;
  }

  public MsgVpnJndiConnectionFactory clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

   /**
   * The Client identifier (ID). If not specified, a unique value for it will be generated. The default value is &#x60;\&quot;\&quot;&#x60;.
   * @return clientId
  **/
  @Schema(description = "The Client identifier (ID). If not specified, a unique value for it will be generated. The default value is `\"\"`.")
  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public MsgVpnJndiConnectionFactory connectionFactoryName(String connectionFactoryName) {
    this.connectionFactoryName = connectionFactoryName;
    return this;
  }

   /**
   * The name of the JMS Connection Factory.
   * @return connectionFactoryName
  **/
  @Schema(description = "The name of the JMS Connection Factory.")
  public String getConnectionFactoryName() {
    return connectionFactoryName;
  }

  public void setConnectionFactoryName(String connectionFactoryName) {
    this.connectionFactoryName = connectionFactoryName;
  }

  public MsgVpnJndiConnectionFactory dtoReceiveOverrideEnabled(Boolean dtoReceiveOverrideEnabled) {
    this.dtoReceiveOverrideEnabled = dtoReceiveOverrideEnabled;
    return this;
  }

   /**
   * Enable or disable overriding by the Subscriber (Consumer) of the deliver-to-one (DTO) property on messages. When enabled, the Subscriber can receive all DTO tagged messages. The default value is &#x60;true&#x60;.
   * @return dtoReceiveOverrideEnabled
  **/
  @Schema(description = "Enable or disable overriding by the Subscriber (Consumer) of the deliver-to-one (DTO) property on messages. When enabled, the Subscriber can receive all DTO tagged messages. The default value is `true`.")
  public Boolean isDtoReceiveOverrideEnabled() {
    return dtoReceiveOverrideEnabled;
  }

  public void setDtoReceiveOverrideEnabled(Boolean dtoReceiveOverrideEnabled) {
    this.dtoReceiveOverrideEnabled = dtoReceiveOverrideEnabled;
  }

  public MsgVpnJndiConnectionFactory dtoReceiveSubscriberLocalPriority(Integer dtoReceiveSubscriberLocalPriority) {
    this.dtoReceiveSubscriberLocalPriority = dtoReceiveSubscriberLocalPriority;
    return this;
  }

   /**
   * The priority for receiving deliver-to-one (DTO) messages by the Subscriber (Consumer) if the messages are published on the local broker that the Subscriber is directly connected to. The default value is &#x60;1&#x60;.
   * @return dtoReceiveSubscriberLocalPriority
  **/
  @Schema(description = "The priority for receiving deliver-to-one (DTO) messages by the Subscriber (Consumer) if the messages are published on the local broker that the Subscriber is directly connected to. The default value is `1`.")
  public Integer getDtoReceiveSubscriberLocalPriority() {
    return dtoReceiveSubscriberLocalPriority;
  }

  public void setDtoReceiveSubscriberLocalPriority(Integer dtoReceiveSubscriberLocalPriority) {
    this.dtoReceiveSubscriberLocalPriority = dtoReceiveSubscriberLocalPriority;
  }

  public MsgVpnJndiConnectionFactory dtoReceiveSubscriberNetworkPriority(Integer dtoReceiveSubscriberNetworkPriority) {
    this.dtoReceiveSubscriberNetworkPriority = dtoReceiveSubscriberNetworkPriority;
    return this;
  }

   /**
   * The priority for receiving deliver-to-one (DTO) messages by the Subscriber (Consumer) if the messages are published on a remote broker. The default value is &#x60;1&#x60;.
   * @return dtoReceiveSubscriberNetworkPriority
  **/
  @Schema(description = "The priority for receiving deliver-to-one (DTO) messages by the Subscriber (Consumer) if the messages are published on a remote broker. The default value is `1`.")
  public Integer getDtoReceiveSubscriberNetworkPriority() {
    return dtoReceiveSubscriberNetworkPriority;
  }

  public void setDtoReceiveSubscriberNetworkPriority(Integer dtoReceiveSubscriberNetworkPriority) {
    this.dtoReceiveSubscriberNetworkPriority = dtoReceiveSubscriberNetworkPriority;
  }

  public MsgVpnJndiConnectionFactory dtoSendEnabled(Boolean dtoSendEnabled) {
    this.dtoSendEnabled = dtoSendEnabled;
    return this;
  }

   /**
   * Enable or disable the deliver-to-one (DTO) property on messages sent by the Publisher (Producer). The default value is &#x60;false&#x60;.
   * @return dtoSendEnabled
  **/
  @Schema(description = "Enable or disable the deliver-to-one (DTO) property on messages sent by the Publisher (Producer). The default value is `false`.")
  public Boolean isDtoSendEnabled() {
    return dtoSendEnabled;
  }

  public void setDtoSendEnabled(Boolean dtoSendEnabled) {
    this.dtoSendEnabled = dtoSendEnabled;
  }

  public MsgVpnJndiConnectionFactory dynamicEndpointCreateDurableEnabled(Boolean dynamicEndpointCreateDurableEnabled) {
    this.dynamicEndpointCreateDurableEnabled = dynamicEndpointCreateDurableEnabled;
    return this;
  }

   /**
   * Enable or disable whether a durable endpoint will be dynamically created on the broker when the client calls \&quot;Session.createDurableSubscriber()\&quot; or \&quot;Session.createQueue()\&quot;. The created endpoint respects the message time-to-live (TTL) according to the \&quot;dynamicEndpointRespectTtlEnabled\&quot; property. The default value is &#x60;false&#x60;.
   * @return dynamicEndpointCreateDurableEnabled
  **/
  @Schema(description = "Enable or disable whether a durable endpoint will be dynamically created on the broker when the client calls \"Session.createDurableSubscriber()\" or \"Session.createQueue()\". The created endpoint respects the message time-to-live (TTL) according to the \"dynamicEndpointRespectTtlEnabled\" property. The default value is `false`.")
  public Boolean isDynamicEndpointCreateDurableEnabled() {
    return dynamicEndpointCreateDurableEnabled;
  }

  public void setDynamicEndpointCreateDurableEnabled(Boolean dynamicEndpointCreateDurableEnabled) {
    this.dynamicEndpointCreateDurableEnabled = dynamicEndpointCreateDurableEnabled;
  }

  public MsgVpnJndiConnectionFactory dynamicEndpointRespectTtlEnabled(Boolean dynamicEndpointRespectTtlEnabled) {
    this.dynamicEndpointRespectTtlEnabled = dynamicEndpointRespectTtlEnabled;
    return this;
  }

   /**
   * Enable or disable whether dynamically created durable and non-durable endpoints respect the message time-to-live (TTL) property. The default value is &#x60;true&#x60;.
   * @return dynamicEndpointRespectTtlEnabled
  **/
  @Schema(description = "Enable or disable whether dynamically created durable and non-durable endpoints respect the message time-to-live (TTL) property. The default value is `true`.")
  public Boolean isDynamicEndpointRespectTtlEnabled() {
    return dynamicEndpointRespectTtlEnabled;
  }

  public void setDynamicEndpointRespectTtlEnabled(Boolean dynamicEndpointRespectTtlEnabled) {
    this.dynamicEndpointRespectTtlEnabled = dynamicEndpointRespectTtlEnabled;
  }

  public MsgVpnJndiConnectionFactory guaranteedReceiveAckTimeout(Integer guaranteedReceiveAckTimeout) {
    this.guaranteedReceiveAckTimeout = guaranteedReceiveAckTimeout;
    return this;
  }

   /**
   * The timeout for sending the acknowledgement (ACK) for guaranteed messages received by the Subscriber (Consumer), in milliseconds. The default value is &#x60;1000&#x60;.
   * @return guaranteedReceiveAckTimeout
  **/
  @Schema(description = "The timeout for sending the acknowledgement (ACK) for guaranteed messages received by the Subscriber (Consumer), in milliseconds. The default value is `1000`.")
  public Integer getGuaranteedReceiveAckTimeout() {
    return guaranteedReceiveAckTimeout;
  }

  public void setGuaranteedReceiveAckTimeout(Integer guaranteedReceiveAckTimeout) {
    this.guaranteedReceiveAckTimeout = guaranteedReceiveAckTimeout;
  }

  public MsgVpnJndiConnectionFactory guaranteedReceiveReconnectRetryCount(Integer guaranteedReceiveReconnectRetryCount) {
    this.guaranteedReceiveReconnectRetryCount = guaranteedReceiveReconnectRetryCount;
    return this;
  }

   /**
   * The maximum number of attempts to reconnect to the host or list of hosts after the guaranteed  messaging connection has been lost. The value \&quot;-1\&quot; means to retry forever. The default value is &#x60;-1&#x60;. Available since 2.14.
   * @return guaranteedReceiveReconnectRetryCount
  **/
  @Schema(description = "The maximum number of attempts to reconnect to the host or list of hosts after the guaranteed  messaging connection has been lost. The value \"-1\" means to retry forever. The default value is `-1`. Available since 2.14.")
  public Integer getGuaranteedReceiveReconnectRetryCount() {
    return guaranteedReceiveReconnectRetryCount;
  }

  public void setGuaranteedReceiveReconnectRetryCount(Integer guaranteedReceiveReconnectRetryCount) {
    this.guaranteedReceiveReconnectRetryCount = guaranteedReceiveReconnectRetryCount;
  }

  public MsgVpnJndiConnectionFactory guaranteedReceiveReconnectRetryWait(Integer guaranteedReceiveReconnectRetryWait) {
    this.guaranteedReceiveReconnectRetryWait = guaranteedReceiveReconnectRetryWait;
    return this;
  }

   /**
   * The amount of time to wait before making another attempt to connect or reconnect to the host after the guaranteed messaging connection has been lost, in milliseconds. The default value is &#x60;3000&#x60;. Available since 2.14.
   * @return guaranteedReceiveReconnectRetryWait
  **/
  @Schema(description = "The amount of time to wait before making another attempt to connect or reconnect to the host after the guaranteed messaging connection has been lost, in milliseconds. The default value is `3000`. Available since 2.14.")
  public Integer getGuaranteedReceiveReconnectRetryWait() {
    return guaranteedReceiveReconnectRetryWait;
  }

  public void setGuaranteedReceiveReconnectRetryWait(Integer guaranteedReceiveReconnectRetryWait) {
    this.guaranteedReceiveReconnectRetryWait = guaranteedReceiveReconnectRetryWait;
  }

  public MsgVpnJndiConnectionFactory guaranteedReceiveWindowSize(Integer guaranteedReceiveWindowSize) {
    this.guaranteedReceiveWindowSize = guaranteedReceiveWindowSize;
    return this;
  }

   /**
   * The size of the window for guaranteed messages received by the Subscriber (Consumer), in messages. The default value is &#x60;18&#x60;.
   * @return guaranteedReceiveWindowSize
  **/
  @Schema(description = "The size of the window for guaranteed messages received by the Subscriber (Consumer), in messages. The default value is `18`.")
  public Integer getGuaranteedReceiveWindowSize() {
    return guaranteedReceiveWindowSize;
  }

  public void setGuaranteedReceiveWindowSize(Integer guaranteedReceiveWindowSize) {
    this.guaranteedReceiveWindowSize = guaranteedReceiveWindowSize;
  }

  public MsgVpnJndiConnectionFactory guaranteedReceiveWindowSizeAckThreshold(Integer guaranteedReceiveWindowSizeAckThreshold) {
    this.guaranteedReceiveWindowSizeAckThreshold = guaranteedReceiveWindowSizeAckThreshold;
    return this;
  }

   /**
   * The threshold for sending the acknowledgement (ACK) for guaranteed messages received by the Subscriber (Consumer) as a percentage of &#x60;guaranteedReceiveWindowSize&#x60;. The default value is &#x60;60&#x60;.
   * @return guaranteedReceiveWindowSizeAckThreshold
  **/
  @Schema(description = "The threshold for sending the acknowledgement (ACK) for guaranteed messages received by the Subscriber (Consumer) as a percentage of `guaranteedReceiveWindowSize`. The default value is `60`.")
  public Integer getGuaranteedReceiveWindowSizeAckThreshold() {
    return guaranteedReceiveWindowSizeAckThreshold;
  }

  public void setGuaranteedReceiveWindowSizeAckThreshold(Integer guaranteedReceiveWindowSizeAckThreshold) {
    this.guaranteedReceiveWindowSizeAckThreshold = guaranteedReceiveWindowSizeAckThreshold;
  }

  public MsgVpnJndiConnectionFactory guaranteedSendAckTimeout(Integer guaranteedSendAckTimeout) {
    this.guaranteedSendAckTimeout = guaranteedSendAckTimeout;
    return this;
  }

   /**
   * The timeout for receiving the acknowledgement (ACK) for guaranteed messages sent by the Publisher (Producer), in milliseconds. The default value is &#x60;2000&#x60;.
   * @return guaranteedSendAckTimeout
  **/
  @Schema(description = "The timeout for receiving the acknowledgement (ACK) for guaranteed messages sent by the Publisher (Producer), in milliseconds. The default value is `2000`.")
  public Integer getGuaranteedSendAckTimeout() {
    return guaranteedSendAckTimeout;
  }

  public void setGuaranteedSendAckTimeout(Integer guaranteedSendAckTimeout) {
    this.guaranteedSendAckTimeout = guaranteedSendAckTimeout;
  }

  public MsgVpnJndiConnectionFactory guaranteedSendWindowSize(Integer guaranteedSendWindowSize) {
    this.guaranteedSendWindowSize = guaranteedSendWindowSize;
    return this;
  }

   /**
   * The size of the window for non-persistent guaranteed messages sent by the Publisher (Producer), in messages. For persistent messages the window size is fixed at 1. The default value is &#x60;255&#x60;.
   * @return guaranteedSendWindowSize
  **/
  @Schema(description = "The size of the window for non-persistent guaranteed messages sent by the Publisher (Producer), in messages. For persistent messages the window size is fixed at 1. The default value is `255`.")
  public Integer getGuaranteedSendWindowSize() {
    return guaranteedSendWindowSize;
  }

  public void setGuaranteedSendWindowSize(Integer guaranteedSendWindowSize) {
    this.guaranteedSendWindowSize = guaranteedSendWindowSize;
  }

  public MsgVpnJndiConnectionFactory messagingDefaultDeliveryMode(MessagingDefaultDeliveryModeEnum messagingDefaultDeliveryMode) {
    this.messagingDefaultDeliveryMode = messagingDefaultDeliveryMode;
    return this;
  }

   /**
   * The default delivery mode for messages sent by the Publisher (Producer). The default value is &#x60;\&quot;persistent\&quot;&#x60;. The allowed values and their meaning are:  &lt;pre&gt; \&quot;persistent\&quot; - The broker spools messages (persists in the Message Spool) as part of the send operation. \&quot;non-persistent\&quot; - The broker does not spool messages (does not persist in the Message Spool) as part of the send operation. &lt;/pre&gt; 
   * @return messagingDefaultDeliveryMode
  **/
  @Schema(description = "The default delivery mode for messages sent by the Publisher (Producer). The default value is `\"persistent\"`. The allowed values and their meaning are:  <pre> \"persistent\" - The broker spools messages (persists in the Message Spool) as part of the send operation. \"non-persistent\" - The broker does not spool messages (does not persist in the Message Spool) as part of the send operation. </pre> ")
  public MessagingDefaultDeliveryModeEnum getMessagingDefaultDeliveryMode() {
    return messagingDefaultDeliveryMode;
  }

  public void setMessagingDefaultDeliveryMode(MessagingDefaultDeliveryModeEnum messagingDefaultDeliveryMode) {
    this.messagingDefaultDeliveryMode = messagingDefaultDeliveryMode;
  }

  public MsgVpnJndiConnectionFactory messagingDefaultDmqEligibleEnabled(Boolean messagingDefaultDmqEligibleEnabled) {
    this.messagingDefaultDmqEligibleEnabled = messagingDefaultDmqEligibleEnabled;
    return this;
  }

   /**
   * Enable or disable whether messages sent by the Publisher (Producer) are Dead Message Queue (DMQ) eligible by default. The default value is &#x60;false&#x60;.
   * @return messagingDefaultDmqEligibleEnabled
  **/
  @Schema(description = "Enable or disable whether messages sent by the Publisher (Producer) are Dead Message Queue (DMQ) eligible by default. The default value is `false`.")
  public Boolean isMessagingDefaultDmqEligibleEnabled() {
    return messagingDefaultDmqEligibleEnabled;
  }

  public void setMessagingDefaultDmqEligibleEnabled(Boolean messagingDefaultDmqEligibleEnabled) {
    this.messagingDefaultDmqEligibleEnabled = messagingDefaultDmqEligibleEnabled;
  }

  public MsgVpnJndiConnectionFactory messagingDefaultElidingEligibleEnabled(Boolean messagingDefaultElidingEligibleEnabled) {
    this.messagingDefaultElidingEligibleEnabled = messagingDefaultElidingEligibleEnabled;
    return this;
  }

   /**
   * Enable or disable whether messages sent by the Publisher (Producer) are Eliding eligible by default. The default value is &#x60;false&#x60;.
   * @return messagingDefaultElidingEligibleEnabled
  **/
  @Schema(description = "Enable or disable whether messages sent by the Publisher (Producer) are Eliding eligible by default. The default value is `false`.")
  public Boolean isMessagingDefaultElidingEligibleEnabled() {
    return messagingDefaultElidingEligibleEnabled;
  }

  public void setMessagingDefaultElidingEligibleEnabled(Boolean messagingDefaultElidingEligibleEnabled) {
    this.messagingDefaultElidingEligibleEnabled = messagingDefaultElidingEligibleEnabled;
  }

  public MsgVpnJndiConnectionFactory messagingJmsxUserIdEnabled(Boolean messagingJmsxUserIdEnabled) {
    this.messagingJmsxUserIdEnabled = messagingJmsxUserIdEnabled;
    return this;
  }

   /**
   * Enable or disable inclusion (adding or replacing) of the JMSXUserID property in messages sent by the Publisher (Producer). The default value is &#x60;false&#x60;.
   * @return messagingJmsxUserIdEnabled
  **/
  @Schema(description = "Enable or disable inclusion (adding or replacing) of the JMSXUserID property in messages sent by the Publisher (Producer). The default value is `false`.")
  public Boolean isMessagingJmsxUserIdEnabled() {
    return messagingJmsxUserIdEnabled;
  }

  public void setMessagingJmsxUserIdEnabled(Boolean messagingJmsxUserIdEnabled) {
    this.messagingJmsxUserIdEnabled = messagingJmsxUserIdEnabled;
  }

  public MsgVpnJndiConnectionFactory messagingTextInXmlPayloadEnabled(Boolean messagingTextInXmlPayloadEnabled) {
    this.messagingTextInXmlPayloadEnabled = messagingTextInXmlPayloadEnabled;
    return this;
  }

   /**
   * Enable or disable encoding of JMS text messages in Publisher (Producer) messages as XML payload. When disabled, JMS text messages are encoded as a binary attachment. The default value is &#x60;true&#x60;.
   * @return messagingTextInXmlPayloadEnabled
  **/
  @Schema(description = "Enable or disable encoding of JMS text messages in Publisher (Producer) messages as XML payload. When disabled, JMS text messages are encoded as a binary attachment. The default value is `true`.")
  public Boolean isMessagingTextInXmlPayloadEnabled() {
    return messagingTextInXmlPayloadEnabled;
  }

  public void setMessagingTextInXmlPayloadEnabled(Boolean messagingTextInXmlPayloadEnabled) {
    this.messagingTextInXmlPayloadEnabled = messagingTextInXmlPayloadEnabled;
  }

  public MsgVpnJndiConnectionFactory msgVpnName(String msgVpnName) {
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

  public MsgVpnJndiConnectionFactory transportCompressionLevel(Integer transportCompressionLevel) {
    this.transportCompressionLevel = transportCompressionLevel;
    return this;
  }

   /**
   * The ZLIB compression level for the connection to the broker. The value \&quot;0\&quot; means no compression, and the value \&quot;-1\&quot; means the compression level is specified in the JNDI Properties file. The default value is &#x60;-1&#x60;.
   * @return transportCompressionLevel
  **/
  @Schema(description = "The ZLIB compression level for the connection to the broker. The value \"0\" means no compression, and the value \"-1\" means the compression level is specified in the JNDI Properties file. The default value is `-1`.")
  public Integer getTransportCompressionLevel() {
    return transportCompressionLevel;
  }

  public void setTransportCompressionLevel(Integer transportCompressionLevel) {
    this.transportCompressionLevel = transportCompressionLevel;
  }

  public MsgVpnJndiConnectionFactory transportConnectRetryCount(Integer transportConnectRetryCount) {
    this.transportConnectRetryCount = transportConnectRetryCount;
    return this;
  }

   /**
   * The maximum number of retry attempts to establish an initial connection to the host or list of hosts. The value \&quot;0\&quot; means a single attempt (no retries), and the value \&quot;-1\&quot; means to retry forever. The default value is &#x60;0&#x60;.
   * @return transportConnectRetryCount
  **/
  @Schema(description = "The maximum number of retry attempts to establish an initial connection to the host or list of hosts. The value \"0\" means a single attempt (no retries), and the value \"-1\" means to retry forever. The default value is `0`.")
  public Integer getTransportConnectRetryCount() {
    return transportConnectRetryCount;
  }

  public void setTransportConnectRetryCount(Integer transportConnectRetryCount) {
    this.transportConnectRetryCount = transportConnectRetryCount;
  }

  public MsgVpnJndiConnectionFactory transportConnectRetryPerHostCount(Integer transportConnectRetryPerHostCount) {
    this.transportConnectRetryPerHostCount = transportConnectRetryPerHostCount;
    return this;
  }

   /**
   * The maximum number of retry attempts to establish an initial connection to each host on the list of hosts. The value \&quot;0\&quot; means a single attempt (no retries), and the value \&quot;-1\&quot; means to retry forever. The default value is &#x60;0&#x60;.
   * @return transportConnectRetryPerHostCount
  **/
  @Schema(description = "The maximum number of retry attempts to establish an initial connection to each host on the list of hosts. The value \"0\" means a single attempt (no retries), and the value \"-1\" means to retry forever. The default value is `0`.")
  public Integer getTransportConnectRetryPerHostCount() {
    return transportConnectRetryPerHostCount;
  }

  public void setTransportConnectRetryPerHostCount(Integer transportConnectRetryPerHostCount) {
    this.transportConnectRetryPerHostCount = transportConnectRetryPerHostCount;
  }

  public MsgVpnJndiConnectionFactory transportConnectTimeout(Integer transportConnectTimeout) {
    this.transportConnectTimeout = transportConnectTimeout;
    return this;
  }

   /**
   * The timeout for establishing an initial connection to the broker, in milliseconds. The default value is &#x60;30000&#x60;.
   * @return transportConnectTimeout
  **/
  @Schema(description = "The timeout for establishing an initial connection to the broker, in milliseconds. The default value is `30000`.")
  public Integer getTransportConnectTimeout() {
    return transportConnectTimeout;
  }

  public void setTransportConnectTimeout(Integer transportConnectTimeout) {
    this.transportConnectTimeout = transportConnectTimeout;
  }

  public MsgVpnJndiConnectionFactory transportDirectTransportEnabled(Boolean transportDirectTransportEnabled) {
    this.transportDirectTransportEnabled = transportDirectTransportEnabled;
    return this;
  }

   /**
   * Enable or disable usage of the Direct Transport mode for sending non-persistent messages. When disabled, the Guaranteed Transport mode is used. The default value is &#x60;true&#x60;.
   * @return transportDirectTransportEnabled
  **/
  @Schema(description = "Enable or disable usage of the Direct Transport mode for sending non-persistent messages. When disabled, the Guaranteed Transport mode is used. The default value is `true`.")
  public Boolean isTransportDirectTransportEnabled() {
    return transportDirectTransportEnabled;
  }

  public void setTransportDirectTransportEnabled(Boolean transportDirectTransportEnabled) {
    this.transportDirectTransportEnabled = transportDirectTransportEnabled;
  }

  public MsgVpnJndiConnectionFactory transportKeepaliveCount(Integer transportKeepaliveCount) {
    this.transportKeepaliveCount = transportKeepaliveCount;
    return this;
  }

   /**
   * The maximum number of consecutive application-level keepalive messages sent without the broker response before the connection to the broker is closed. The default value is &#x60;3&#x60;.
   * @return transportKeepaliveCount
  **/
  @Schema(description = "The maximum number of consecutive application-level keepalive messages sent without the broker response before the connection to the broker is closed. The default value is `3`.")
  public Integer getTransportKeepaliveCount() {
    return transportKeepaliveCount;
  }

  public void setTransportKeepaliveCount(Integer transportKeepaliveCount) {
    this.transportKeepaliveCount = transportKeepaliveCount;
  }

  public MsgVpnJndiConnectionFactory transportKeepaliveEnabled(Boolean transportKeepaliveEnabled) {
    this.transportKeepaliveEnabled = transportKeepaliveEnabled;
    return this;
  }

   /**
   * Enable or disable usage of application-level keepalive messages to maintain a connection with the broker. The default value is &#x60;true&#x60;.
   * @return transportKeepaliveEnabled
  **/
  @Schema(description = "Enable or disable usage of application-level keepalive messages to maintain a connection with the broker. The default value is `true`.")
  public Boolean isTransportKeepaliveEnabled() {
    return transportKeepaliveEnabled;
  }

  public void setTransportKeepaliveEnabled(Boolean transportKeepaliveEnabled) {
    this.transportKeepaliveEnabled = transportKeepaliveEnabled;
  }

  public MsgVpnJndiConnectionFactory transportKeepaliveInterval(Integer transportKeepaliveInterval) {
    this.transportKeepaliveInterval = transportKeepaliveInterval;
    return this;
  }

   /**
   * The interval between application-level keepalive messages, in milliseconds. The default value is &#x60;3000&#x60;.
   * @return transportKeepaliveInterval
  **/
  @Schema(description = "The interval between application-level keepalive messages, in milliseconds. The default value is `3000`.")
  public Integer getTransportKeepaliveInterval() {
    return transportKeepaliveInterval;
  }

  public void setTransportKeepaliveInterval(Integer transportKeepaliveInterval) {
    this.transportKeepaliveInterval = transportKeepaliveInterval;
  }

  public MsgVpnJndiConnectionFactory transportMsgCallbackOnIoThreadEnabled(Boolean transportMsgCallbackOnIoThreadEnabled) {
    this.transportMsgCallbackOnIoThreadEnabled = transportMsgCallbackOnIoThreadEnabled;
    return this;
  }

   /**
   * Enable or disable delivery of asynchronous messages directly from the I/O thread. Contact Solace Support before enabling this property. The default value is &#x60;false&#x60;.
   * @return transportMsgCallbackOnIoThreadEnabled
  **/
  @Schema(description = "Enable or disable delivery of asynchronous messages directly from the I/O thread. Contact Solace Support before enabling this property. The default value is `false`.")
  public Boolean isTransportMsgCallbackOnIoThreadEnabled() {
    return transportMsgCallbackOnIoThreadEnabled;
  }

  public void setTransportMsgCallbackOnIoThreadEnabled(Boolean transportMsgCallbackOnIoThreadEnabled) {
    this.transportMsgCallbackOnIoThreadEnabled = transportMsgCallbackOnIoThreadEnabled;
  }

  public MsgVpnJndiConnectionFactory transportOptimizeDirectEnabled(Boolean transportOptimizeDirectEnabled) {
    this.transportOptimizeDirectEnabled = transportOptimizeDirectEnabled;
    return this;
  }

   /**
   * Enable or disable optimization for the Direct Transport delivery mode. If enabled, the client application is limited to one Publisher (Producer) and one non-durable Subscriber (Consumer). The default value is &#x60;false&#x60;.
   * @return transportOptimizeDirectEnabled
  **/
  @Schema(description = "Enable or disable optimization for the Direct Transport delivery mode. If enabled, the client application is limited to one Publisher (Producer) and one non-durable Subscriber (Consumer). The default value is `false`.")
  public Boolean isTransportOptimizeDirectEnabled() {
    return transportOptimizeDirectEnabled;
  }

  public void setTransportOptimizeDirectEnabled(Boolean transportOptimizeDirectEnabled) {
    this.transportOptimizeDirectEnabled = transportOptimizeDirectEnabled;
  }

  public MsgVpnJndiConnectionFactory transportPort(Integer transportPort) {
    this.transportPort = transportPort;
    return this;
  }

   /**
   * The connection port number on the broker for SMF clients. The value \&quot;-1\&quot; means the port is specified in the JNDI Properties file. The default value is &#x60;-1&#x60;.
   * @return transportPort
  **/
  @Schema(description = "The connection port number on the broker for SMF clients. The value \"-1\" means the port is specified in the JNDI Properties file. The default value is `-1`.")
  public Integer getTransportPort() {
    return transportPort;
  }

  public void setTransportPort(Integer transportPort) {
    this.transportPort = transportPort;
  }

  public MsgVpnJndiConnectionFactory transportReadTimeout(Integer transportReadTimeout) {
    this.transportReadTimeout = transportReadTimeout;
    return this;
  }

   /**
   * The timeout for reading a reply from the broker, in milliseconds. The default value is &#x60;10000&#x60;.
   * @return transportReadTimeout
  **/
  @Schema(description = "The timeout for reading a reply from the broker, in milliseconds. The default value is `10000`.")
  public Integer getTransportReadTimeout() {
    return transportReadTimeout;
  }

  public void setTransportReadTimeout(Integer transportReadTimeout) {
    this.transportReadTimeout = transportReadTimeout;
  }

  public MsgVpnJndiConnectionFactory transportReceiveBufferSize(Integer transportReceiveBufferSize) {
    this.transportReceiveBufferSize = transportReceiveBufferSize;
    return this;
  }

   /**
   * The size of the receive socket buffer, in bytes. It corresponds to the SO_RCVBUF socket option. The default value is &#x60;65536&#x60;.
   * @return transportReceiveBufferSize
  **/
  @Schema(description = "The size of the receive socket buffer, in bytes. It corresponds to the SO_RCVBUF socket option. The default value is `65536`.")
  public Integer getTransportReceiveBufferSize() {
    return transportReceiveBufferSize;
  }

  public void setTransportReceiveBufferSize(Integer transportReceiveBufferSize) {
    this.transportReceiveBufferSize = transportReceiveBufferSize;
  }

  public MsgVpnJndiConnectionFactory transportReconnectRetryCount(Integer transportReconnectRetryCount) {
    this.transportReconnectRetryCount = transportReconnectRetryCount;
    return this;
  }

   /**
   * The maximum number of attempts to reconnect to the host or list of hosts after the connection has been lost. The value \&quot;-1\&quot; means to retry forever. The default value is &#x60;3&#x60;.
   * @return transportReconnectRetryCount
  **/
  @Schema(description = "The maximum number of attempts to reconnect to the host or list of hosts after the connection has been lost. The value \"-1\" means to retry forever. The default value is `3`.")
  public Integer getTransportReconnectRetryCount() {
    return transportReconnectRetryCount;
  }

  public void setTransportReconnectRetryCount(Integer transportReconnectRetryCount) {
    this.transportReconnectRetryCount = transportReconnectRetryCount;
  }

  public MsgVpnJndiConnectionFactory transportReconnectRetryWait(Integer transportReconnectRetryWait) {
    this.transportReconnectRetryWait = transportReconnectRetryWait;
    return this;
  }

   /**
   * The amount of time before making another attempt to connect or reconnect to the host after the connection has been lost, in milliseconds. The default value is &#x60;3000&#x60;.
   * @return transportReconnectRetryWait
  **/
  @Schema(description = "The amount of time before making another attempt to connect or reconnect to the host after the connection has been lost, in milliseconds. The default value is `3000`.")
  public Integer getTransportReconnectRetryWait() {
    return transportReconnectRetryWait;
  }

  public void setTransportReconnectRetryWait(Integer transportReconnectRetryWait) {
    this.transportReconnectRetryWait = transportReconnectRetryWait;
  }

  public MsgVpnJndiConnectionFactory transportSendBufferSize(Integer transportSendBufferSize) {
    this.transportSendBufferSize = transportSendBufferSize;
    return this;
  }

   /**
   * The size of the send socket buffer, in bytes. It corresponds to the SO_SNDBUF socket option. The default value is &#x60;65536&#x60;.
   * @return transportSendBufferSize
  **/
  @Schema(description = "The size of the send socket buffer, in bytes. It corresponds to the SO_SNDBUF socket option. The default value is `65536`.")
  public Integer getTransportSendBufferSize() {
    return transportSendBufferSize;
  }

  public void setTransportSendBufferSize(Integer transportSendBufferSize) {
    this.transportSendBufferSize = transportSendBufferSize;
  }

  public MsgVpnJndiConnectionFactory transportTcpNoDelayEnabled(Boolean transportTcpNoDelayEnabled) {
    this.transportTcpNoDelayEnabled = transportTcpNoDelayEnabled;
    return this;
  }

   /**
   * Enable or disable the TCP_NODELAY option. When enabled, Nagle&#x27;s algorithm for TCP/IP congestion control (RFC 896) is disabled. The default value is &#x60;true&#x60;.
   * @return transportTcpNoDelayEnabled
  **/
  @Schema(description = "Enable or disable the TCP_NODELAY option. When enabled, Nagle's algorithm for TCP/IP congestion control (RFC 896) is disabled. The default value is `true`.")
  public Boolean isTransportTcpNoDelayEnabled() {
    return transportTcpNoDelayEnabled;
  }

  public void setTransportTcpNoDelayEnabled(Boolean transportTcpNoDelayEnabled) {
    this.transportTcpNoDelayEnabled = transportTcpNoDelayEnabled;
  }

  public MsgVpnJndiConnectionFactory xaEnabled(Boolean xaEnabled) {
    this.xaEnabled = xaEnabled;
    return this;
  }

   /**
   * Enable or disable this as an XA Connection Factory. When enabled, the Connection Factory can be cast to \&quot;XAConnectionFactory\&quot;, \&quot;XAQueueConnectionFactory\&quot; or \&quot;XATopicConnectionFactory\&quot;. The default value is &#x60;false&#x60;.
   * @return xaEnabled
  **/
  @Schema(description = "Enable or disable this as an XA Connection Factory. When enabled, the Connection Factory can be cast to \"XAConnectionFactory\", \"XAQueueConnectionFactory\" or \"XATopicConnectionFactory\". The default value is `false`.")
  public Boolean isXaEnabled() {
    return xaEnabled;
  }

  public void setXaEnabled(Boolean xaEnabled) {
    this.xaEnabled = xaEnabled;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MsgVpnJndiConnectionFactory msgVpnJndiConnectionFactory = (MsgVpnJndiConnectionFactory) o;
    return Objects.equals(this.allowDuplicateClientIdEnabled, msgVpnJndiConnectionFactory.allowDuplicateClientIdEnabled) &&
        Objects.equals(this.clientDescription, msgVpnJndiConnectionFactory.clientDescription) &&
        Objects.equals(this.clientId, msgVpnJndiConnectionFactory.clientId) &&
        Objects.equals(this.connectionFactoryName, msgVpnJndiConnectionFactory.connectionFactoryName) &&
        Objects.equals(this.dtoReceiveOverrideEnabled, msgVpnJndiConnectionFactory.dtoReceiveOverrideEnabled) &&
        Objects.equals(this.dtoReceiveSubscriberLocalPriority, msgVpnJndiConnectionFactory.dtoReceiveSubscriberLocalPriority) &&
        Objects.equals(this.dtoReceiveSubscriberNetworkPriority, msgVpnJndiConnectionFactory.dtoReceiveSubscriberNetworkPriority) &&
        Objects.equals(this.dtoSendEnabled, msgVpnJndiConnectionFactory.dtoSendEnabled) &&
        Objects.equals(this.dynamicEndpointCreateDurableEnabled, msgVpnJndiConnectionFactory.dynamicEndpointCreateDurableEnabled) &&
        Objects.equals(this.dynamicEndpointRespectTtlEnabled, msgVpnJndiConnectionFactory.dynamicEndpointRespectTtlEnabled) &&
        Objects.equals(this.guaranteedReceiveAckTimeout, msgVpnJndiConnectionFactory.guaranteedReceiveAckTimeout) &&
        Objects.equals(this.guaranteedReceiveReconnectRetryCount, msgVpnJndiConnectionFactory.guaranteedReceiveReconnectRetryCount) &&
        Objects.equals(this.guaranteedReceiveReconnectRetryWait, msgVpnJndiConnectionFactory.guaranteedReceiveReconnectRetryWait) &&
        Objects.equals(this.guaranteedReceiveWindowSize, msgVpnJndiConnectionFactory.guaranteedReceiveWindowSize) &&
        Objects.equals(this.guaranteedReceiveWindowSizeAckThreshold, msgVpnJndiConnectionFactory.guaranteedReceiveWindowSizeAckThreshold) &&
        Objects.equals(this.guaranteedSendAckTimeout, msgVpnJndiConnectionFactory.guaranteedSendAckTimeout) &&
        Objects.equals(this.guaranteedSendWindowSize, msgVpnJndiConnectionFactory.guaranteedSendWindowSize) &&
        Objects.equals(this.messagingDefaultDeliveryMode, msgVpnJndiConnectionFactory.messagingDefaultDeliveryMode) &&
        Objects.equals(this.messagingDefaultDmqEligibleEnabled, msgVpnJndiConnectionFactory.messagingDefaultDmqEligibleEnabled) &&
        Objects.equals(this.messagingDefaultElidingEligibleEnabled, msgVpnJndiConnectionFactory.messagingDefaultElidingEligibleEnabled) &&
        Objects.equals(this.messagingJmsxUserIdEnabled, msgVpnJndiConnectionFactory.messagingJmsxUserIdEnabled) &&
        Objects.equals(this.messagingTextInXmlPayloadEnabled, msgVpnJndiConnectionFactory.messagingTextInXmlPayloadEnabled) &&
        Objects.equals(this.msgVpnName, msgVpnJndiConnectionFactory.msgVpnName) &&
        Objects.equals(this.transportCompressionLevel, msgVpnJndiConnectionFactory.transportCompressionLevel) &&
        Objects.equals(this.transportConnectRetryCount, msgVpnJndiConnectionFactory.transportConnectRetryCount) &&
        Objects.equals(this.transportConnectRetryPerHostCount, msgVpnJndiConnectionFactory.transportConnectRetryPerHostCount) &&
        Objects.equals(this.transportConnectTimeout, msgVpnJndiConnectionFactory.transportConnectTimeout) &&
        Objects.equals(this.transportDirectTransportEnabled, msgVpnJndiConnectionFactory.transportDirectTransportEnabled) &&
        Objects.equals(this.transportKeepaliveCount, msgVpnJndiConnectionFactory.transportKeepaliveCount) &&
        Objects.equals(this.transportKeepaliveEnabled, msgVpnJndiConnectionFactory.transportKeepaliveEnabled) &&
        Objects.equals(this.transportKeepaliveInterval, msgVpnJndiConnectionFactory.transportKeepaliveInterval) &&
        Objects.equals(this.transportMsgCallbackOnIoThreadEnabled, msgVpnJndiConnectionFactory.transportMsgCallbackOnIoThreadEnabled) &&
        Objects.equals(this.transportOptimizeDirectEnabled, msgVpnJndiConnectionFactory.transportOptimizeDirectEnabled) &&
        Objects.equals(this.transportPort, msgVpnJndiConnectionFactory.transportPort) &&
        Objects.equals(this.transportReadTimeout, msgVpnJndiConnectionFactory.transportReadTimeout) &&
        Objects.equals(this.transportReceiveBufferSize, msgVpnJndiConnectionFactory.transportReceiveBufferSize) &&
        Objects.equals(this.transportReconnectRetryCount, msgVpnJndiConnectionFactory.transportReconnectRetryCount) &&
        Objects.equals(this.transportReconnectRetryWait, msgVpnJndiConnectionFactory.transportReconnectRetryWait) &&
        Objects.equals(this.transportSendBufferSize, msgVpnJndiConnectionFactory.transportSendBufferSize) &&
        Objects.equals(this.transportTcpNoDelayEnabled, msgVpnJndiConnectionFactory.transportTcpNoDelayEnabled) &&
        Objects.equals(this.xaEnabled, msgVpnJndiConnectionFactory.xaEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowDuplicateClientIdEnabled, clientDescription, clientId, connectionFactoryName, dtoReceiveOverrideEnabled, dtoReceiveSubscriberLocalPriority, dtoReceiveSubscriberNetworkPriority, dtoSendEnabled, dynamicEndpointCreateDurableEnabled, dynamicEndpointRespectTtlEnabled, guaranteedReceiveAckTimeout, guaranteedReceiveReconnectRetryCount, guaranteedReceiveReconnectRetryWait, guaranteedReceiveWindowSize, guaranteedReceiveWindowSizeAckThreshold, guaranteedSendAckTimeout, guaranteedSendWindowSize, messagingDefaultDeliveryMode, messagingDefaultDmqEligibleEnabled, messagingDefaultElidingEligibleEnabled, messagingJmsxUserIdEnabled, messagingTextInXmlPayloadEnabled, msgVpnName, transportCompressionLevel, transportConnectRetryCount, transportConnectRetryPerHostCount, transportConnectTimeout, transportDirectTransportEnabled, transportKeepaliveCount, transportKeepaliveEnabled, transportKeepaliveInterval, transportMsgCallbackOnIoThreadEnabled, transportOptimizeDirectEnabled, transportPort, transportReadTimeout, transportReceiveBufferSize, transportReconnectRetryCount, transportReconnectRetryWait, transportSendBufferSize, transportTcpNoDelayEnabled, xaEnabled);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MsgVpnJndiConnectionFactory {\n");
    
    sb.append("    allowDuplicateClientIdEnabled: ").append(toIndentedString(allowDuplicateClientIdEnabled)).append("\n");
    sb.append("    clientDescription: ").append(toIndentedString(clientDescription)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    connectionFactoryName: ").append(toIndentedString(connectionFactoryName)).append("\n");
    sb.append("    dtoReceiveOverrideEnabled: ").append(toIndentedString(dtoReceiveOverrideEnabled)).append("\n");
    sb.append("    dtoReceiveSubscriberLocalPriority: ").append(toIndentedString(dtoReceiveSubscriberLocalPriority)).append("\n");
    sb.append("    dtoReceiveSubscriberNetworkPriority: ").append(toIndentedString(dtoReceiveSubscriberNetworkPriority)).append("\n");
    sb.append("    dtoSendEnabled: ").append(toIndentedString(dtoSendEnabled)).append("\n");
    sb.append("    dynamicEndpointCreateDurableEnabled: ").append(toIndentedString(dynamicEndpointCreateDurableEnabled)).append("\n");
    sb.append("    dynamicEndpointRespectTtlEnabled: ").append(toIndentedString(dynamicEndpointRespectTtlEnabled)).append("\n");
    sb.append("    guaranteedReceiveAckTimeout: ").append(toIndentedString(guaranteedReceiveAckTimeout)).append("\n");
    sb.append("    guaranteedReceiveReconnectRetryCount: ").append(toIndentedString(guaranteedReceiveReconnectRetryCount)).append("\n");
    sb.append("    guaranteedReceiveReconnectRetryWait: ").append(toIndentedString(guaranteedReceiveReconnectRetryWait)).append("\n");
    sb.append("    guaranteedReceiveWindowSize: ").append(toIndentedString(guaranteedReceiveWindowSize)).append("\n");
    sb.append("    guaranteedReceiveWindowSizeAckThreshold: ").append(toIndentedString(guaranteedReceiveWindowSizeAckThreshold)).append("\n");
    sb.append("    guaranteedSendAckTimeout: ").append(toIndentedString(guaranteedSendAckTimeout)).append("\n");
    sb.append("    guaranteedSendWindowSize: ").append(toIndentedString(guaranteedSendWindowSize)).append("\n");
    sb.append("    messagingDefaultDeliveryMode: ").append(toIndentedString(messagingDefaultDeliveryMode)).append("\n");
    sb.append("    messagingDefaultDmqEligibleEnabled: ").append(toIndentedString(messagingDefaultDmqEligibleEnabled)).append("\n");
    sb.append("    messagingDefaultElidingEligibleEnabled: ").append(toIndentedString(messagingDefaultElidingEligibleEnabled)).append("\n");
    sb.append("    messagingJmsxUserIdEnabled: ").append(toIndentedString(messagingJmsxUserIdEnabled)).append("\n");
    sb.append("    messagingTextInXmlPayloadEnabled: ").append(toIndentedString(messagingTextInXmlPayloadEnabled)).append("\n");
    sb.append("    msgVpnName: ").append(toIndentedString(msgVpnName)).append("\n");
    sb.append("    transportCompressionLevel: ").append(toIndentedString(transportCompressionLevel)).append("\n");
    sb.append("    transportConnectRetryCount: ").append(toIndentedString(transportConnectRetryCount)).append("\n");
    sb.append("    transportConnectRetryPerHostCount: ").append(toIndentedString(transportConnectRetryPerHostCount)).append("\n");
    sb.append("    transportConnectTimeout: ").append(toIndentedString(transportConnectTimeout)).append("\n");
    sb.append("    transportDirectTransportEnabled: ").append(toIndentedString(transportDirectTransportEnabled)).append("\n");
    sb.append("    transportKeepaliveCount: ").append(toIndentedString(transportKeepaliveCount)).append("\n");
    sb.append("    transportKeepaliveEnabled: ").append(toIndentedString(transportKeepaliveEnabled)).append("\n");
    sb.append("    transportKeepaliveInterval: ").append(toIndentedString(transportKeepaliveInterval)).append("\n");
    sb.append("    transportMsgCallbackOnIoThreadEnabled: ").append(toIndentedString(transportMsgCallbackOnIoThreadEnabled)).append("\n");
    sb.append("    transportOptimizeDirectEnabled: ").append(toIndentedString(transportOptimizeDirectEnabled)).append("\n");
    sb.append("    transportPort: ").append(toIndentedString(transportPort)).append("\n");
    sb.append("    transportReadTimeout: ").append(toIndentedString(transportReadTimeout)).append("\n");
    sb.append("    transportReceiveBufferSize: ").append(toIndentedString(transportReceiveBufferSize)).append("\n");
    sb.append("    transportReconnectRetryCount: ").append(toIndentedString(transportReconnectRetryCount)).append("\n");
    sb.append("    transportReconnectRetryWait: ").append(toIndentedString(transportReconnectRetryWait)).append("\n");
    sb.append("    transportSendBufferSize: ").append(toIndentedString(transportSendBufferSize)).append("\n");
    sb.append("    transportTcpNoDelayEnabled: ").append(toIndentedString(transportTcpNoDelayEnabled)).append("\n");
    sb.append("    xaEnabled: ").append(toIndentedString(xaEnabled)).append("\n");
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
