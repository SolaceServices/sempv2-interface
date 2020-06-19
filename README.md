# SempFacade
Project sempv2-interface is library project for SEMP V2 service configuration.

The main  functionality is exposed via SEMP V2, which should be the primary API to be used to handle all the required tasks.  

### sempinterface 

Sempinterface has all the classes needed to excute SEMP commmands against the PubSub+ Broker.

### solacesemprequest 

Solacesemprequest has all request XML types of objects.

### solacesempreply 

Solacesempreply has all reply XML types of objects.

## SEMP V2 structure

The SEMP V2 library consist of a number of packages to handle all the operations available. The Packages are grouped into the following groups inside the org.solace.psg.sempv2:

- action
- admin 
- apiclient
- auth
- common
- config
- monitor

### action 

Action is used for API calls for service operations to execute specific service tasks like replay, etc. Subpackage 'api' contains all available operations and subpackage 'model' contains all related types and parameters.

### admin 

Action is used for API calls for administrative tasks for the Cloud Console such as handling Client Profiles, Users, Roles, Services, Token Management. Subpackage 'api' contains all available operations and subpackage 'model' contains all related types and parameters. 
Admin tasks are usually performed with bearer token authentication. Solace Cloud API has two types of token - API Tokens (created via Account settings -> token management in the Solace Cloud GUI) and User Tokens (create by calling the service Token API with account username and password).
Some operations (getting a token for Org and adding Ceritificate Authority) currently work only with User token (valid for 24 hours) but this should change. 

### apiclient 

Apiclient has classes to handle an implementation of OKHTTP Client with is used for all the RESTful calls to Solace SEMP V2. 

### auth 

Auth has classes to handle different authentication mechanisms used by Api Client and REST. 

### common 

Common has some common classes for all SEMP V2 packages. 

### config 

Config is used for API calls for service configuration tasks and subelements, like configuring VPNs, Queues, TopicEndpoints, MQTT, Acl Profile, etc. Subpackage 'api' contains all available operations and subpackage 'model' contains all related types and parameters.

### monitor 

Monitor is used for API calls for service monitoring tasks to retrieve runtime information for Queues, Stats, etc. Subpackage 'api' contains all available operations and subpackage 'model' contains all related types and parameters.


### SbbFacade

Sbb facade classes are localed in 'sbb' subpackage and should be used to handle tasks required by sbb configiration such as Service, Bridges, DMR and Cluster tasks which are required for environment management. The facade classes are ClusterFacade, ServiceFacade, DMRFacade and VPNFacade. There are several important classes used by the Facade classes. A lot of the functionality and the API is service-cetric. This means each API has an HTTP Client configured to call a specific service via its REST API.
Each VPN has a number of connection parameters and endpoints represented by the Service class. The latter doesn't hold additional details like passwords and detailed endpoints, which requires the use of a subclass named ServiceDetails. ServiceDetails has quite nested structure, so in order to simpify the usege of a service's important parameters, there is another classes which captures only those important properties called ServiceManagemtnContext. 
The Facade classes have a local Service (context) which configures is used to configure all elements associated with it in the SEMP V2 API. Some classes like DMRFacade have also a remote service (context), which represents a remote service required to configure its corresponding elements. Bridges are also bidirectional and require both local service and remote service objects in order to be configured correctly.   

#### ServiceFacade

The ServiceFacade class implements global access level operations. Those are tasks to manipulate Users, Roles, Token Management, Services Management, Certificate Management, Organbizations and Client Profiles.   

#### VPNFacade

The VpnFacade class implements Message VPN service objects associated with a Message VPN like ACLs, bridges, queues, etc. These data structures found in the model package usually have a prefix "MsgVpn" in their class name. 

#### ClusterFacade

The ClusterFacade class is used to bridge together a Cluster of services and add and delete bridges when a new service is added or deleted. It is using internally the ServiceFacade and the VpnFacade.

#### DMRFacade

The DMRFacade class is used to create DMR clusters, Add Cluster Links and Bridges in order to connect two external services together. Is it currently configured to use Basic authentication. 

## Building and Unit testing the library

Unit tests require a test serviceName, serviceId and accessToken (or user and pass) parameters to be configured in config.properties file. 
An account needs to be setup with at least one test service in order for the tests to be run. 
A parameter testClusterServiceIds=<serviceId>,<serviceId>,...<serviceId> needs to be added so bridge creation can be tested.
A sample list of parameters can be found in the config.properties file. 

To compile and build library with unit tests, without using an acceeeToken use the following command:

```bash
mvn clean compile package -Duser=<cloudUsername> -Dpass=<cloudPassword>
```

with accessToken set as a property in config.properties: accessToken=<token> :

```bash
mvn clean compile package 
```
