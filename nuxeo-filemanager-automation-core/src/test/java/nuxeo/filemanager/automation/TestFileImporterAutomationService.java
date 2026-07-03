package nuxeo.filemanager.automation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.filemanager")
@Deploy("nuxeo.filemanager.automation.nuxeo-filemanager-automation-core")
public class TestFileImporterAutomationService {

    public static final String TEST_FILE = "lorem-ipsum.pdf";

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected FileManager fileManager;

    @Inject
    protected FileImporterAutomationService fileManagerAutomation;

    protected DocumentModel folder;

    @Before
    public void init() {

        folder = coreSession.createDocumentModel("/", "test", "Folder");
        folder = coreSession.createDocument(folder);

        coreSession.save();
    }

    @Test
    public void testService() {
        assertNotNull(fileManagerAutomation);
    }

    @Test
    public void shouldReturnNulWhenNoChainContributed() throws Exception {
        
        File f = FileUtils.getResourceFileFromContext(TEST_FILE);

        Blob input = Blobs.createBlob(f, "application/pdf");

        FileImporterContext context = FileImporterContext.builder(coreSession, input, "/")
                                                         .overwrite(true)
                                                         .build();

        // Our service should return null
        DocumentModel doc = fileManagerAutomation.createOrUpdate(context);
        assertNull(doc);

        // But the whole FileManager service should create a "File"
        doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        assertEquals("File", doc.getType());

    }

    @Test
    public void shouldReturnNullWhenNoChainContributedForFolderImporter() {

        // (See interface => return nulls if no chain or chain returns null)
        DocumentModel doc = fileManagerAutomation.createFolderish(coreSession, "The Folder", "/", false, null);
        assertNull(doc);

    }
}
