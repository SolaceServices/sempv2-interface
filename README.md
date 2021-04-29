# sempv2-interface

## Description
Project sempv2-interface is Java library project for SEMP V2 management.

The project uses packages with generated code via https://editor.swagger.io/ 

The currently generated code is based on schema version 9.6 (2.16). The latest version is 9.8 (2.18). 

The code is grouped into four main packages:

- action - Mostly coming from generated OpenAPI schemas and handling SEMP v2 Actions operations.
- admin - Created manually to handle Solace Cloud administrative operations, which are specific for Solace PubSub+ Cloud. 
- config - Mostly coming from generated OpenAPI schemas and handling SEMP v2 Config operations.
- monitor - Mostly coming from generated OpenAPI schemas and handling SEMP v2 Monitor operations.

The packages are based on the structure exposed via the SEMP v2 API. More information is available on the SEMP web page: 
https://docs.solace.com/SEMP/Using-SEMP.htm

## Code generation

Go to https://editor.swagger.io/ and import the YAML files for Action, Config and Monitor one by one and generate Java client.

To build with a tool the code can be obtained from: https://github.com/swagger-api/swagger-codegen and then build the code:
`mvn clean package`

Create a config file named *config.json*:
	{
		"modelPackage" : "com.solace.psg.sempv2.action",
		"apiPackage" : "com.solace.psg.sempv2.action.api"
	}

To build with Java copy the Json schemas on the path *<swagger-codegentool-path>/modules/swagger-codegen-cli/target* and run: 
`java -jar swagger-codegen-cli.jar generate -i semp-v2-swagger-action.json -l java -o sempv2.action -c config.json`


  