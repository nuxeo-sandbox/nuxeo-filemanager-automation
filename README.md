# nuxeo-filemanager-automation
[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=Sandbox/sandbox_nuxeo-filemanager-automation-master)](https://qa.nuxeo.org/jenkins/job/Sandbox/job/sandbox_nuxeo-filemanager-automation-master/)


## About the Plugin
nuxeo-filemanager-automation is a plugin that contributes the [FileManager](https://doc.nuxeo.com/nxdoc/file-manager/) (see below), allowing for calling an automation chain when a file/folder is imported.

* When importing a *file*, typically, Nuxeo's FileManager is called to create a document of a certain type from a blob. This happens mainly when a user drag-n-drops file(s) in the UI (and does not choose "set properties"), when calling the `FileManager.Import` operation, etc.

The chain must then tell the plugin which type of document to create. It also can, optionally, set properties. This allows for tuning/configuring the system in Nuxeo Studio. Notice (see below) that the chain can ignore the call, which means the FileManager will call the next plugin until one returns a valid document. So, for example, if the user drops a .pdf, your chain may decide to create a `MyCustomDocType` document based on the context, or ignore the call and let Nuxeo creates a `File` document.

* When importing a Folderish (typically: via Nuxeo Drive or a connector), Nuxeo creates a `Folder` by default (there is no plugin chain mechanism as we have for a file). The callback chain must create the Folderish document (or return `null`, Nuxeo will then create a `Folder`)

#### About Nuxeo's [FileManager](https://doc.nuxeo.com/nxdoc/file-manager/)
It is a Nuxeo service that creates a document for a blob. The logic for deciding which type of document to create (File, Picture, ...) is based on *plugins* which have an order property and filters:

* Order determines which plugin will be called first
* The filters allow for filtering based on the blob's mime-type. So, for example, if you have a FileManager plugin that should act only for JPEG images, you will add the image/jpeg filter
* When a blob is imported, the FileManager calls the plugins whose filter(s) match the mime-type, one after the other, until one creates a document.

It can also be configured for creating a specific document type for a `Folderish`, with no cascade of plugins, only one "FolderImporter" can be set.


## Using the Plugin

1. For handling blob import => create an automation chain, handle the specific parameters received and set a specific context variable with the values you want to use for the creation
2. For handling creation of `Folderish` => create an automation chain that receives the parent container, create the `Folderish` of the type you wish and return it (or null and Nuxeo will create a `Folder`)
3. Contribute the XML extension that tells the plugin to use your automation chain(s)

Notice you can set one and/or the other (2 callback chains, or only one for files or only one for Folderish)

### 1. Create the Callback Automation Chain to Handle Files
(optional, you can have juste a FolderImporter callback)

We recommend JavaScript automation (more flexible with conditions)

* The chain receives the `blob` as input, and must return it.
* Declare the parameters the chain receives (passed by the plugin)
  * `parent_path`: Full path of the parent where the current document is to be created
  * `parent_type` is the document type of the parent where the current document is to be created
  * *WARNING* You must explicitly declare these parameters in the "Parameters" tab of your chain editor in Studio
* In the chain, check/test the parameters (`parent_path`, `parent_type`, you can also load the parent if you need to test more properties, using `Repository.GetDocument`, typically), and return the values in the `FileImporterAutomation_Result` context variable
* About this `FileImporterAutomation_Result` context variable:
  * It is expected by the plugin as a **JSON object as string** with
    * `"docType"`: The type of document to create.
    * `"properties"`: a JSON object to setup the fields. Optional.
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
  if(params.parent_type === "DeliverableFolder") {
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
  if(params.parent_type === "DeliverableFolder") {
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
  if(params.parent_path.indexOf("/SpecialFolder/") > -1) {
    if(input.getMimeType() === "image/jpg") {
      ctx. FileImporterAutomation_Result = "{\"docType\": \"CustomDesign\"}";
    }
  }
}
```

### 2. Create the Callback Automation Chain to Handle `Folderish`
(optional, you can have juste a FileImporter callback)

We recommend JavaScript automation (more flexible with conditions)

* The chain receives the parent `document` as input, returns `document`, the created Folderish.
* Declare the parameter the chain receives (passed by the plugin)
  * `title`: the title of the `Folderish` to create. This is the name of the folder the user uses on their Desktop.
  * *WARNING* You must explicitly declare this parameter in the "Parameters" tab of your chain editor in Studio
* In the chain, check/test the input, create the `Folderish` accordingly and return it.

#### Examples
* This JS creates a `DesignsContainer` if the parent's path contains "/Designs". Else, it creates a `Folder`:

```
function run(input, params) {

  var folderishDoc = null;
  
  if(input.path.indexOf("/Designs" > -1)) {
    folderishDoc = Document.Create(
	   input, {
	     'type': "DesignsContainer",
	     'name': params.title,
	     'properties': {
	       "dc:title": params.title,
	       "dc:description": "Created by the FolderImporterCallback chain"
	     }
	   }
	 );
  }
  
  return folderishDoc;
}
```

* Here we create different types of containers depending on the context:

```
function run(input, params) {

  var folderish, docType;
    
  folderish = null;
  // In this example we have 3 custom Folderish types
  // Depending on the parent type and path, we create one or the other
  // Also, we have special naming convention
  if(params.title === "SpecialDocs") {
    docType = "Folder";
  } else {
    switch (input.type) {
      case "Workspace":
        if(input.path.indexOf("/Assets") > -1) {
          docType = "AssetsContainer";
        } else {
          docType = "ClaimsContainer";
        }
        break;

      case "Folder":
        if(input.path.indexOf("/SpecialDocs") > -1) {
          docType = "CasesContainer";
        } else {
          docType = "ClaimsContainer";
        }
        break;

      default:
        docType = "Folder";
        break;
    }
  }
  
  folderish = Document.Create(
    input, {
      'type': docType,
      'name': params.title,
      'properties': {
        "dc:title": params.title,
        "dc:description": "Created by the FolderImporterCallback chain"
      }
    }
  );

  return folderish;
}
```

### 3. Contribute the Plugin's Extension point

In studio, add the following new [XML extension](https://doc.nuxeo.com/studio/advanced-settings/).

```
<extension target="nuxeo.filemanager.automation.FileImporterAutomationService"
           point="configuration">
  <configuration>
    <defaultChain>HERE-YOUR-CHAIN-ID</defaultChain>
    <folderImporterChain>HERE-YOUR-CHAIN-ID</folderImporterChain>
  </configuration>
</extension>
```

Remember the ID of a chain is prefixed with `javascript.` it is is a JS Automation chain. For example, if one of our examples above was named "CreateCustomDesignOnFileImport" and you also set a folder importer call back named "FolderImporterCallback", you would write:

```
<extension target="nuxeo.filemanager.automation.FileImporterAutomationService"
           point="configuration">
  <configuration>
    <defaultChain>javascript.CreateCustomDesignOnFileImport</defaultChain>
    <folderImporterChain>javascript.FolderImporterCallback</folderImporterChain>
  </configuration>
</extension>
```

If you do not set a `folderImporter`, as in...

```
<extension target="nuxeo.filemanager.automation.FileImporterAutomationService"
           point="configuration">
  <configuration>
    <defaultChain>javascript.CreateCustomDesignOnFileImport</defaultChain>
  </configuration>
</extension>
```

... then default behavior applies (Nuxeo always creates a `Folder`) 


## FileManager and Priorities
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
  
  <folderImporter name="FolderImporterWithAutomation"
      class="nuxeo.filemanager.automation.FolderImporter" />
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
  
  <folderImporter name="FolderImporterWithAutomation"
      class="nuxeo.filemanager.automation.FolderImporter" />
</extension>
```

**WARNING** Do not change the `folderImporter` declaration, unless you want to use another one or reset it to the default class.


## WARNINGS

#### Errors in the Automation Chain Callback
Make 100% sure the automation chain called by the plugin has no error. It is difficult to investigate automation errors in this context.


#### Performance
As this plugin calls automation, using it at time of massive import will slow the system (compared to no nuxeo-filemanager-automation plugin installed), because of the overall cost of calling Automation for every file.

Of course, an easy way to change that is to move the logic built in automation to a Java plugin (and remove the XML contribution)


## Build-Installation

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


## Support

**These features are not part of the Nuxeo Production platform, they are not supported**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into the platform, not maintained here.

## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content-oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).  
