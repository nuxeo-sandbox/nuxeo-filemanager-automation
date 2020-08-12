package nuxeo.filemanager.automation;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
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
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FileImporterAutomationServiceImpl extends DefaultComponent implements FileImporterAutomationService {

    private static final Log log = LogFactory.getLog(FileImporterAutomationServiceImpl.class);

    public static final String EXT_POINT = "configuration";

    protected FileImporterAutomationDescriptor descriptor;

    protected int noConfigCount = 0;

    /**
     * Component activated notification.
     * Called when the component is activated. All component dependencies are resolved at that moment.
     * Use this method to initialize the component.
     *
     * @param context the component context.
     */
    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
    }

    /**
     * Component deactivated notification.
     * Called before a component is unregistered.
     * Use this method to do cleanup if any and free any resources held by the component.
     *
     * @param context the component context.
     */
    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    /**
     * Application started notification.
     * Called after the application started.
     * You can do here any initialization that requires a working application
     * (all resolved bundles and components are active at that moment)
     *
     * @param context the component context. Use it to get the current bundle context
     * @throws Exception
     */
    @Override
    public void applicationStarted(ComponentContext context) {
        // do nothing by default. You can remove this method if not used.
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (EXT_POINT.equals(extensionPoint)) {
            descriptor = (FileImporterAutomationDescriptor) contribution;
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // Logic to do when unregistering any contribution
    }

    @Override
    public DocumentModel createOrUpdate(FileImporterContext context) throws NuxeoException {

        if (descriptor == null) {
            if ((noConfigCount % 50) == 0) {
                log.warn(
                        "No configuration contributed => not doing anything (Nuxeo will call the next file importer plugin)");
            }
            noConfigCount += 1;
            return null;
        }

        String chainId = descriptor.chainId;
        if (StringUtils.isBlank(chainId)) {
            if ((noConfigCount % 50) == 0) {
                log.warn("No chain ID provided => not doing anything (Nuxeo will call the next file importer plugin)");
            }
            noConfigCount += 1;
            return null;
        }

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
            String resultStr = (String) octx.get(CALLBACK_CTX_VAR_NAME);
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
            throw new NuxeoException("Failed to run the callback chain", e);
        }

        return doc;
    }
}
