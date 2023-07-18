/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
package nuxeo.filemanager.automation;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FileImporterAutomationServiceImpl extends DefaultComponent implements FileImporterAutomationService {

    private static final Logger log = LogManager.getLogger(FileImporterAutomationServiceImpl.class);

    public static final String EXT_POINT = "configuration";

    protected FileImporterAutomationDescriptor descriptor;

    // Avoid flooding the log with WARNS if no callback chains are provided
    // Example: user provided a folderImporter callback, but no filemanager callback
    // => Warning will be displayed for all and every file drag and dropped (unless
    // the dev. chaged the priority and the pattern)
    boolean logNoConfigDone = false;

    boolean logNoFileManagerCBChainDone = false;

    boolean logNoFolderManagerCBChainDone = false;


    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (EXT_POINT.equals(extensionPoint)) {
            descriptor = (FileImporterAutomationDescriptor) contribution;
        }
    }

    protected boolean hasConfiguration() {

        if (descriptor == null) {
            if (!logNoConfigDone) {
                log.warn("No configuration contributed => not doing anything, letting Nuxeo decides");
                logNoConfigDone = true;
            }
            return false;
        }

        return true;
    }

    protected boolean hasAFileImporterChain() {

        if (!hasConfiguration()) {
            return false;
        }

        String chainId = descriptor.getChainId();
        if (StringUtils.isBlank(chainId)) {
            if (!logNoFileManagerCBChainDone) {
                log.warn(
                        "No chain ID provided for the File Importer => not doing anything when importing files (Nuxeo will call the next file importer plugin)");
                logNoFileManagerCBChainDone = true;
            }
            return false;
        }

        return true;
    }

    protected boolean hasAFolderImporterChain() {

        if (!hasConfiguration()) {
            return false;
        }

        String folderChainId = descriptor.getFolderImporterChain();
        if (StringUtils.isBlank(folderChainId)) {
            if (!logNoFolderManagerCBChainDone) {
                log.warn("No chain ID provided for Folder Importer => default behavior will apply.");
                logNoFolderManagerCBChainDone = true;
            }
            return false;
        }

        return true;
    }

    @Override
    public DocumentModel createOrUpdate(FileImporterContext context) throws NuxeoException {

        if (!hasAFileImporterChain()) {
            return null;
        }

        String chainId = descriptor.getChainId();
        DocumentModel doc = null;

        PathRef parentRef = new PathRef(context.getParentPath());
        DocumentModel parentDoc = context.getSession().getDocument(parentRef);
        Blob blob = context.getBlob();
        CoreSession session = context.getSession();

        AutomationService as = Framework.getService(AutomationService.class);
        OperationContext octx = new OperationContext(session);
        octx.setInput(blob);
        Map<String, Object> params = new HashMap<>();
        params.put(CALLBACK_PARAM_PARENT_PATH, parentDoc.getPathAsString());
        params.put(CALLBACK_PARAM_PARENT_TYPE, parentDoc.getType());

        try {
            blob = (Blob) as.run(octx, chainId, params);
            String resultStr = (String) octx.get(CALLBACK_FILEIMPORTER_CTX_VAR_NAME);
            if (StringUtils.isBlank(resultStr)) {
                return null;
            }

            // NOTICE: Using Jackson instead of default org.json.JSONObject because below,
            // we use a converter to Nuxeo Properties, that expect a Jackson JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode resultJson = objectMapper.readTree(resultStr);

            JsonNode docTypeJson = resultJson.get(CALLBACK_RESULT_DOCTYPE);
            if (docTypeJson == null) {
                return null;
            }
            String docType = docTypeJson.asText();
            if (StringUtils.isBlank(docType)) {
                return null;
            }

            String title = null;
            JsonNode propertiesJson = null;
            if (resultJson.hasNonNull(CALLBACK_RESULT_PROPERTIES)) {
                propertiesJson = resultJson.get(CALLBACK_RESULT_PROPERTIES);
                if (propertiesJson.hasNonNull("dc:title")) {
                    title = propertiesJson.get("dc:title").asText();
                }
            }
            if (StringUtils.isBlank(title)) {
                title = StringUtils.defaultIfBlank(context.getFileName(), blob.getFilename());
            }

            doc = session.createDocumentModel(parentDoc.getPathAsString(), title, docType);
            doc.setPropertyValue("dc:title", title);
            doc.setPropertyValue("file:content", (Serializable) blob);
            if (propertiesJson != null) {
                Properties props = new Properties(propertiesJson);
                DocumentHelper.setProperties(session, doc, props);
            }
            doc = session.createDocument(doc);

        } catch (OperationException | IOException e) {
            throw new NuxeoException("Failed to run the FileManager callback chain <" + chainId + ">", e);
        }

        return doc;
    }

    @Override
    public DocumentModel createFolderish(CoreSession session, String fullname, String path, boolean overwrite,
            TypeManager typeManager) {

        if (!hasAFolderImporterChain()) {
            return null;
        }

        String chainId = descriptor.getFolderImporterChain();
        DocumentModel folderish = null;

        // Doing as the default fileManagerService, cleaning up
        String title = FileManagerUtils.fetchFileName(fullname);

        // See interface => assumes current user has access to the parent
        PathRef parentRef = new PathRef(path);
        DocumentModel parentDoc = session.getDocument(parentRef);

        AutomationService as = Framework.getService(AutomationService.class);
        OperationContext octx = new OperationContext(session);
        octx.setInput(parentDoc);
        Map<String, Object> params = new HashMap<>();
        params.put(CALLBACK_PARAM_FOLDERISH_TITLE, title);
        try {
            folderish = (DocumentModel) as.run(octx, chainId, params);
        } catch (OperationException e) {
            throw new NuxeoException("Failed to run the FileManager callback chain <" + chainId + ">", e);
        }

        return folderish;
    }
}
