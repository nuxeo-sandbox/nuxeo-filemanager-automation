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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("nuxeo.filemanager.automation.nuxeo-filemanager-automation-core")
public class TestFileManagerAutomation {

    public static final String TEST_FILE = "lorem-ipsum.pdf";

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected FileManager fileManager;

    @Inject
    protected FileImporterAutomationService fileManagerAutomation;

    protected DocumentModel folder;

    protected DocumentModel sectionRoot;

    @Before
    public void init() {

        folder = coreSession.createDocumentModel("/", "test", "Folder");
        folder = coreSession.createDocument(folder);

        sectionRoot = coreSession.createDocumentModel("/", "THE-SECTION-ROOT", "SectionRoot");
        sectionRoot = coreSession.createDocument(sectionRoot);
        
        coreSession.save();
    }
    
    protected FileImporterContext buildContextForTestFile(DocumentModel parent) throws Exception {
        
        File f = FileUtils.getResourceFileFromContext(TEST_FILE);

        Blob input = Blobs.createBlob(f, "application/pdf");

        FileImporterContext context = FileImporterContext.builder(coreSession, input, parent.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        
        return context;
    }

    @Test
    public void shouldReturnNulWhenNoChainContributed() throws Exception {

        FileImporterContext context = buildContextForTestFile(folder);

        // Our service should return null
        DocumentModel doc = fileManagerAutomation.createOrUpdate(context);
        assertNull(doc);
        
        // But the whole FileManager service should create a "File"
        doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        assertEquals("File", doc.getType());
        
    }
    
    @Test
    @Deploy("org.nuxeo.ecm.automation.scripting")
    @Deploy("nuxeo.filemanager.automation.nuxeo-filemanager-automation-core:test-return-a-section.xml")
    public void testCreateDependingOnParentType() throws Exception {//shouldReturnTheCorrectDocType

        FileImporterContext context = buildContextForTestFile(sectionRoot);
        
        // In a section root
        DocumentModel doc = fileManagerAutomation.createOrUpdate(context);
        assertNotNull(doc);
        assertEquals("Section", doc.getType());
        
        // Not in a section root
        context = buildContextForTestFile(folder);
        doc = fileManagerAutomation.createOrUpdate(context);
        assertNull(doc);
        
        
    }
    
    @Test
    @Deploy("org.nuxeo.ecm.automation.scripting")
    @Deploy("nuxeo.filemanager.automation.nuxeo-filemanager-automation-core:test-return-a-section-with-properties.xml")
    public void testCreateWithProperties() throws Exception {//shouldReturnTheCorrectDocType

        FileImporterContext context = buildContextForTestFile(sectionRoot);
        
        // In a section root
        DocumentModel doc = fileManagerAutomation.createOrUpdate(context);
        assertNotNull(doc);
        assertEquals("Section", doc.getType());

        // (see the script in test-return-a-section-with-properties.xml)
        assertEquals("THE TITLE", doc.getTitle());
        String desc = (String) doc.getPropertyValue("dc:description");
        assertEquals("THE DESC", desc);
        
    }

}
