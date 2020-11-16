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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.types.TypeManager;

public interface FileImporterAutomationService {

    // Only for FileImporter
    public static final String CALLBACK_FILEIMPORTER_CTX_VAR_NAME = "FileImporterAutomation_Result";

    public static final String CALLBACK_PARAM_PARENT_PATH = "parent_path";

    public static final String CALLBACK_PARAM_PARENT_TYPE = "parent_type";

    // Only for FolderImporter
    public static final String CALLBACK_PARAM_FOLDERISH_TITLE = "title";

    // Used only by the FileManager (not folderImporter). If null or "" => no creation, move to next importer plugin
    // (FileImporter)
    public static final String CALLBACK_RESULT_DOCTYPE = "docType";

    // Optional, and only for FileImporter (not FolderImporter)
    public static final String CALLBACK_RESULT_PROPERTIES = "properties";

    /**
     * Returns a created or updated document based on the given {@code context}. Will call the automation chain set in
     * the XML configuration to get the type of document to create.
     * The chain receives the blob as input and some parameters (parent path, parent document).
     * It returns values in the CALLBACK_FILEIMPORTER_CTX_VAR_NAME context variable, which is a JSON string with:
     * <code>
     *   {
     *     "docType": required, the type of document to create,
     *     "properties": optional. JSON object to setup the fields
     *   }
     * </code>
     * If the chain returns null or "" => the method returns null, so the FileManager can call the next file importer
     * plugin.
     * <br>
     * TODO: Make it simple => Change the logic and let the chain create the DocumentModel, do not ask it to return
     * values and properties
     * 
     * @param context
     * @return a document or null
     * @throws NuxeoException
     * @since 10.10
     */
    public DocumentModel createOrUpdate(FileImporterContext context) throws NuxeoException;

    /**
     * Returns a newly created Folderish document based on the given parameters. Will call the automation chain set in
     * the XML configuration, and this chain is in charge of creating the Folderish.
     * It receives the parent container as input and the title of the Folderish to create as parameter.
     * It must return the created Folderish, or null.
     * If there is no callback chain or the chain returns null => the method returns null, and the caller *MUST*
     * create a Folderish, there is no default value when you override the default folderImporter.
     * <br>
     * <b>Important</b>: The method assumes current user can access the parent container (path)
     * 
     * @param session
     * @param fullname
     * @param path
     * @param overwrite
     * @param typeManager
     * @return
     * @since 10.10
     */
    public DocumentModel createFolderish(CoreSession session, String fullname, String path, boolean overwrite,
            TypeManager typeManager);

}
