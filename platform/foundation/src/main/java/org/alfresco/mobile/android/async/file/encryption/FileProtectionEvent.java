/*******************************************************************************
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco Mobile for Android.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.alfresco.mobile.android.async.file.encryption;

import java.io.File;

import org.alfresco.mobile.android.async.LoaderResult;
import org.alfresco.mobile.android.async.OperationEvent;

public class FileProtectionEvent extends OperationEvent<Void>
{
    public final int intentAction;
    
    public final File protectedFile;

    public final boolean encryptionAction;

    public FileProtectionEvent(String requestId, LoaderResult<Void> result, File protectedFile, boolean encryptionAction, int intentAction)
    {
        super(requestId, result);
        this.intentAction = intentAction;
        this.protectedFile = protectedFile;
        this.encryptionAction = encryptionAction;
    }
}
