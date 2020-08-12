# nuxeo-filemanager-automation
[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=Sandbox/sandbox_nuxeo-filemanager-automation-master)](https://qa.nuxeo.org/jenkins/job/Sandbox/job/sandbox_nuxeo-filemanager-automation-master/)

This is **W**ork **I**n **P**rogress. Mainly, this document has to be correctly written.


## About the Plugin
nuxeo-filemanager-automation is a plugin that contributes the [FileManager](https://doc.nuxeo.com/nxdoc/file-manager/) (see below), allowing for calling an automation chain when a file is imported. Typically, Nuxeo's FileManager is called to create a document of a certain type from a blob. This happens mainly when a user drag-n-drops file(s) in the UI (and does not choose "set properties"), when calling the `FileManager.Import` operation, etc.

The chain must then tell the plugin which type of document to create. It also can, optionally, set properties. This allows for tuning/configuring the system in Nuxeo Studio. Notice (see below) that the chain can ignore the call, which means the FileManager will call the next plugin until one returns a valid document. So, for example, if the user drops a .pdf, your chain may decide to create a `MyCustomDocType` document based on the context, or ignore the call and let Nuxeo creates a `File` document.

#### About Nuxeo's [FileManager](https://doc.nuxeo.com/nxdoc/file-manager/)
It is a Nuxeo service that creates a document for a blob. The logic for deciding which type of document to create (File, Picture, ...) is based on *plugins* which have an order property and filters:

* Order determines which plugin will be called first
* The filters allow for filtering based on the blob's mime-type. So, for example, if you have a FileManager plugin that should act only for JPEG images, you will add the image/jpeg filter
* When a blob is imported, the FileManager calls the plugins whose filter(s) match the mime-type, one after the other, until one creates a document.


## Using the Plugin

To use it you need to

1. Create an automation chain. Handle specific parameters received and set a specific context variable with the values you want to use for the creation
2. Contribute the XML extension that tells the plugin to use your automation chain

### 1. Create the Callback Automation Chain
Create an automation chain. We recommend JavaScript automation (more flexible with conditions)

* The chain receives the `blob` as input, and must return it.
* Declare the parameters the chain receives (passed by the plugin)
  * `parentPath`: Full path of the parent where the current document is to be created
  * `parentType` is the document type of the parent where the current document is to be created
* In the chain, check/test the context (`parentPath`, `parentType`, you can also load the parent if you need to test more properties, using `Repository.GetDocument`, typically), and return the values in the `FileImporterAutomation_Result` context variable
* About this `FileImporterAutomation_Result` context variable:
  * It is expected by the plugin as a **JSON object as string** with
    * `"docType"`: The type of document to create
    * `"properties"`: a JSON object to setup the fields
 * If you...
   * don't return the context variable,
   * or you set it to `null`,
   * or if its `docType` property is `null` or `""`
 * ...then, the plugin does nothing, meaning Nuxeo File Importer will pass the blob to the next plugin until a document is created

#### Examples
* This JS creates a `CustomDesign` if the parent is a `DeliverableFolder` and the blob is a JPEG. Else, it does nothing

```
function run(input, params) {
  // Initialize the result
  ctx. FileImporterAutomation_Result = null;
  if(params.parentType === "DeliverableFolder") {
    if(input.getMimeType() === "image/jpg") {
      ctx. FileImporterAutomation_Result = "{\"docType\": \"CustomDesign\"}";
    }
  }
}
```

* This JS does the same but also sets up some properties:

```
function run(input, params) {
  // Initialize the result
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

* Here, we make a decision based on the path of the parent

```
function run(input, params) {
  // Initialize the result
  ctx. FileImporterAutomation_Result = null;
  if(params.parentPath.indexOf("/SpecialFolder/") > -1) {
    if(input.getMimeType() === "image/jpg") {
      ctx. FileImporterAutomation_Result = "{\"docType\": \"CustomDesign\"}";
    }
  }
}
```

### 2. Contribute the Plugin's Extension point

In studio, add the following new [XML extension](https://doc.nuxeo.com/studio/advanced-settings/).

```
<extension target="nuxeo.filemanager.automation.FileImporterAutomationService"
           point="configuration">
  <configuration>
    <defaultChain>HERE-YOUR-CHAIN-ID</defaultChain>
  </configuration>
</extension>
```

Remember tyhe ID of a chain is prefixed with `javascript.` it is is a JS Automation chain. For example, if one of our examples above was named "CreateCustomDesignOnFileImport", you would write:

```
<extension target="nuxeo.filemanager.automation.FileImporterAutomationService"
           point="configuration">
  <configuration>
    <defaultChain>javascript.CreateCustomDesignOnFileImport</defaultChain>
  </configuration>
</extension>
```


## About the FileManager and this Plugin
The [FileManager](https://doc.nuxeo.com/nxdoc/file-manager/) is a Nuxeo service that creates a document for a blob. The logic for deciding which type of document to create (File, Picture, ...) is based on plugins which have a priority order and filter(s).

Here, our plugin, by default:

* Has a high priority (order = 0)
* And will be called for every type of file (filter: `.*`). The contribution is:

```
<extension target="org.nuxeo.ecm.platform.filemanager.service.FileManagerService"
           point="plugins">
  
  <plugin name="ImporterWithAutomation"
          class="nuxeo.filemanager.automation.FileManagerAutomationPlugin"
          order="0">
    <filter>.*</filter>
  </plugin>
</extension>
```

To change this behavior, copy/paste this declaration in your Studio project and change the filter(s) and the order. For example, to handle only JPEG, but still be called before the default plugins (so you have the opportunity to create your `CustomDesign` before Nuxeo creates a `Picture` document), you would add this XML to Studio:

```
<extension target="org.nuxeo.ecm.platform.filemanager.service.FileManagerService"
           point="plugins">
  
  <plugin name="ImporterWithAutomation"
          class="nuxeo.filemanager.automation.FileManagerAutomationPlugin"
          order="0">
    <filter>image/jpeg</filter>
  </plugin>
</extension>
```

## About Performance
**WARNING**: As this plugin calls automation, using it at time of massive import will slow the system (compared to no nuxeo-filemanager-automation plugin installed), because of the overall cost of calling Automation for every file.

Of course, an easy way to change that is to move the logic built in automation to a Java plugin (and remove the XML contribution)


# Build-Installation

```
git clone https://github.com/nuxeo-filemanager-automation.git
cd nuxeo-filemanager-automation

mvn clean install
```

The plugin is available via the public market place. If you have a valid subscription (even a trial one), you can just:

```
cd /path/to/nuxeo/bin/directory
./nuxeoctl mp-install -s nuxeo-filemanager-automation
```

Notice: The `-s` is required because we are installing a *snapshot* plugin on  *release* platform.


# Support

**These features are not part of the Nuxeo Production platform, they are not supported**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into the platform, not maintained here.

# Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content-oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).  
