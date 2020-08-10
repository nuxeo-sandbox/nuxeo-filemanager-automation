# nuxeo-filemanager-automation

This is **W**ork **I**n **P**rogress. Mainly, this document has to be correctly written.


## About the Plugin
nuxeo-filemanager-automation is a plugin that allows to call an automation chain when a file is imported. The chain tells the system the type of document to create, and can also, optionally, set properties. This allows for tuning/configuring the system in Nuxeo Studio.

It contributes the [FileManager](https://doc.nuxeo.com/nxdoc/file-manager/) (see below)

1. Create an automation chain. We recommend JavaScript automation (more flexible with conditions)
  * The chain receives the `blob` as input, and must return it
  * Declare the parameters the chain receives (passed by the plugin)
    * `parentPath`: Full path of the parent where the current document is to be created
    * `parentType` is the document type of the parent where the current document is to be created
  * In the chain, check/test the context (`parentPath`, `parentType`, you can also load the parent if you need to test more properties, using `Repository.GetDocument`, typically), and return the values in a context variable
  * Set the `FileImporterAutomation_Result` context variable with the values
    * It is expected by the plugin as a JSON string with
      * `"docType"`: The type of document to create
      * `"properties"`: a JSON object to setup the fields
   * If you don't return the context variable or you set it to `null`, or if its `docTye` property is `null` or `""` => the plugin does nothing, meaning Nuxeo File Importer will pass the blob to the next plugin until a document is created

#### Examples
// This JS creates a `CustomDesign` if the parent is a `DeliverableFolder` and the blob is a JPEG. Else, it does nothing

```
function run(input, params) {
  
  ctx. FileImporterAutomation_Result = null;
  if(params.parentType === "DeliverableFolder") {
    if(input.getMimeType() === "image/jpg") {
      ctx. FileImporterAutomation_Result = "{\"docType\": \"CustomDesign\"}";
    }
  }
}
```

// This JS does the same but also sets up some properties:

```
function run(input, params) {
  
  ctx. FileImporterAutomation_Result = null;
  if(params.parentType === "DeliverableFolder") {
    if(input.getMimeType() === "image/jpg") {
      var result = {
        "docType": "CustomDesign",
        "properties": {
          "dc:title": "The title",
          "dc:description": "This image etc.";
          "customdesign:someField": 5
        }
      }
      ctx. FileImporterAutomation_Result = JSON.stringify(result);
    }
  }
}
```

## About the FileManager and this Plugin
The [FileManager](https://doc.nuxeo.com/nxdoc/file-manager/) is a Nuxeo service that creates a document for a blob. The logic for deciding which type of document to create (File, Picture, ...) is based on plugins which have an orders and filters.

Here, our plugin, by default:

* Has a high priority (order = 0)
* And will be called for every type of file. The contribution is:

```
<extension target="org.nuxeo.ecm.platform.filemanager.service.FileManagerService" point="plugins">
  
  <plugin name="ImporterWithAutomation" class="nuxeo.filemanager.automation.FileManagerAutomationPlugin" order="0">
    <filter>.*</filter>
  </plugin>
</extension>
```

To change this behavior, copy/paste this declaration in your Studio project and change the filter(s) and the order.

## About Performance
WARNING: As this plugin calls automation, using it at time of massive import will slow the system (compared to no nuxeo-filemanager-automation plugin installed), because of the overall cost of calling Automation for every file.
 

# Build-Installation

```
git clone https://github.com/nuxeo-filemanager-automation.git
cd nuxeo-filemanager-automation

mvn clean install
```


# Support

**These features are not part of the Nuxeo Production platform, they are not supported**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into the platform, not maintained here.

# Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content-oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).  
