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
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;

public interface FileImporterAutomationService {

    public static final String CALLBACK_CTX_VAR_NAME = "FileImporterAutomation_Result";

    public static final String CALLBACK_PARAM_PARENT_PATH = "parent_path";

    public static final String CALLBACK_PARAM_PARENT_TYPE = "parent_type";

    // If null or "" => no creation, move to next importer plugin
    public static final String CALLBACK_RESULT_DOCTYPE = "docType";

    // Optional
    public static final String CALLBACK_RESULT_PROPERTIES = "properties";

    /**
     * Returns a created or updated document based on the given {@code context}. Will call the automation chain set in
     * the XL configuration to get the type of document to create.
     * If the chain returns null or "" => the method returns null, so the FileManager can call the next file importer
     * plugin.
     * 
     * @param context
     * @return a document or null
     * @throws NuxeoException
     * @since 10.10
     */
    public DocumentModel createOrUpdate(FileImporterContext context) throws NuxeoException;

}
