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

import org.nuxeo.ecm.core.api.DocumentModel;
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
    public DocumentModel createOrUpdate(FileImporterContext context) {

        FileImporterAutomationService service = Framework.getService(FileImporterAutomationService.class);

        return service.createOrUpdate(context);
    }

}
