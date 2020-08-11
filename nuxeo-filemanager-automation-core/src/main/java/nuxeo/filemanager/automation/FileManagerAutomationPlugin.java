package nuxeo.filemanager.automation;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.runtime.api.Framework;

/**
 * This class is called by the File Manager, and declared in the XML extension.
 * It follows the File Manager logic and returns either a document or null.
 * 
 * @since 10.10
 */
public class FileManagerAutomationPlugin extends AbstractFileImporter {

    private static final long serialVersionUID = 1L;

    @Override
    public DocumentModel createOrUpdate(FileImporterContext context) throws NuxeoException {

        FileImporterAutomationService service = Framework.getService(FileImporterAutomationService.class);

        return service.createOrUpdate(context);
    }

}
