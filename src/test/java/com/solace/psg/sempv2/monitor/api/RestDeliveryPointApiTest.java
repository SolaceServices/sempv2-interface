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


package com.solace.psg.sempv2.monitor.api;

import com.solace.psg.sempv2.apiclient.ApiException;
import com.solace.psg.sempv2.monitor.model.MsgVpnRestDeliveryPointQueueBindingResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnRestDeliveryPointQueueBindingsResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnRestDeliveryPointResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnRestDeliveryPointRestConsumerResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnRestDeliveryPointRestConsumersResponse;
import com.solace.psg.sempv2.monitor.model.MsgVpnRestDeliveryPointsResponse;
import com.solace.psg.sempv2.monitor.model.SempMetaOnlyResponse;
import org.junit.Test;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API tests for RestDeliveryPointApi
 */
@Ignore
public class RestDeliveryPointApiTest {

    private final RestDeliveryPointApi api = new RestDeliveryPointApi();

    
    /**
     * Get a REST Delivery Point object.
     *
     * Get a REST Delivery Point object.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| restDeliveryPointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointResponse response = api.getMsgVpnRestDeliveryPoint(msgVpnName, restDeliveryPointName, select);

        // TODO: test validations
    }
    
    /**
     * Get a Queue Binding object.
     *
     * Get a Queue Binding object.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| queueBindingName|x| restDeliveryPointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointQueueBindingTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String queueBindingName = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointQueueBindingResponse response = api.getMsgVpnRestDeliveryPointQueueBinding(msgVpnName, restDeliveryPointName, queueBindingName, select);

        // TODO: test validations
    }
    
    /**
     * Get a list of Queue Binding objects.
     *
     * Get a list of Queue Binding objects.  A Queue Binding for a REST Delivery Point attracts messages to be delivered to REST consumers. If the queue does not exist it can be created subsequently, and once the queue is operational the broker performs the queue binding. Removing the queue binding does not delete the queue itself. Similarly, removing the queue does not remove the queue binding, which fails until the queue is recreated or the queue binding is deleted.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| queueBindingName|x| restDeliveryPointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointQueueBindingsTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointQueueBindingsResponse response = api.getMsgVpnRestDeliveryPointQueueBindings(msgVpnName, restDeliveryPointName, count, cursor, where, select);

        // TODO: test validations
    }
    
    /**
     * Get a REST Consumer object.
     *
     * Get a REST Consumer object.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Deprecated :---|:---:|:---: counter.httpRequestConnectionCloseTxMsgCount||x counter.httpRequestOutstandingTxMsgCount||x counter.httpRequestTimedOutTxMsgCount||x counter.httpRequestTxByteCount||x counter.httpRequestTxMsgCount||x counter.httpResponseErrorRxMsgCount||x counter.httpResponseRxByteCount||x counter.httpResponseRxMsgCount||x counter.httpResponseSuccessRxMsgCount||x msgVpnName|x| restConsumerName|x| restDeliveryPointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointRestConsumerTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumerResponse response = api.getMsgVpnRestDeliveryPointRestConsumer(msgVpnName, restDeliveryPointName, restConsumerName, select);

        // TODO: test validations
    }
    
    /**
     * Get a Trusted Common Name object.
     *
     * Get a Trusted Common Name object.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| restConsumerName|x| restDeliveryPointName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        String tlsTrustedCommonName = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNameResponse response = api.getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonName(msgVpnName, restDeliveryPointName, restConsumerName, tlsTrustedCommonName, select);

        // TODO: test validations
    }
    
    /**
     * Get a list of Trusted Common Name objects.
     *
     * Get a list of Trusted Common Name objects.  The Trusted Common Names for the REST Consumer are used by encrypted transports to verify the name in the certificate presented by the remote REST consumer. They must include the common name of the remote REST consumer&#39;s server certificate.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| restConsumerName|x| restDeliveryPointName|x| tlsTrustedCommonName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        String restConsumerName = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNamesResponse response = api.getMsgVpnRestDeliveryPointRestConsumerTlsTrustedCommonNames(msgVpnName, restDeliveryPointName, restConsumerName, where, select);

        // TODO: test validations
    }
    
    /**
     * Get a list of REST Consumer objects.
     *
     * Get a list of REST Consumer objects.  REST Consumer objects establish HTTP connectivity to REST consumer applications who wish to receive messages from a broker.   Attribute|Identifying|Deprecated :---|:---:|:---: counter.httpRequestConnectionCloseTxMsgCount||x counter.httpRequestOutstandingTxMsgCount||x counter.httpRequestTimedOutTxMsgCount||x counter.httpRequestTxByteCount||x counter.httpRequestTxMsgCount||x counter.httpResponseErrorRxMsgCount||x counter.httpResponseRxByteCount||x counter.httpResponseRxMsgCount||x counter.httpResponseSuccessRxMsgCount||x msgVpnName|x| restConsumerName|x| restDeliveryPointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointRestConsumersTest() throws ApiException {
        String msgVpnName = null;
        String restDeliveryPointName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointRestConsumersResponse response = api.getMsgVpnRestDeliveryPointRestConsumers(msgVpnName, restDeliveryPointName, count, cursor, where, select);

        // TODO: test validations
    }
    
    /**
     * Get a list of REST Delivery Point objects.
     *
     * Get a list of REST Delivery Point objects.  A REST Delivery Point manages delivery of messages from queues to a named list of REST Consumers.   Attribute|Identifying|Deprecated :---|:---:|:---: msgVpnName|x| restDeliveryPointName|x|    A SEMP client authorized with a minimum access scope/level of \&quot;vpn/read-only\&quot; is required to perform this operation.  This has been available since 2.11.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getMsgVpnRestDeliveryPointsTest() throws ApiException {
        String msgVpnName = null;
        Integer count = null;
        String cursor = null;
        List<String> where = null;
        List<String> select = null;
        MsgVpnRestDeliveryPointsResponse response = api.getMsgVpnRestDeliveryPoints(msgVpnName, count, cursor, where, select);

        // TODO: test validations
    }
    
}
