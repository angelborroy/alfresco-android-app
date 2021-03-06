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
package org.alfresco.mobile.android.sync.prepare;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.mobile.android.api.model.Document;
import org.alfresco.mobile.android.api.model.Node;
import org.alfresco.mobile.android.api.session.AlfrescoSession;
import org.alfresco.mobile.android.async.OperationSchema;
import org.alfresco.mobile.android.platform.accounts.AlfrescoAccount;
import org.alfresco.mobile.android.sync.FavoritesSyncManager;
import org.alfresco.mobile.android.sync.FavoritesSyncSchema;
import org.alfresco.mobile.android.sync.operations.FavoriteSync;

import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;

public class PrepareFavoriteHelper extends PrepareBaseHelper
{
    private static final String TAG = PrepareFavoriteHelper.class.getName();

    public PrepareFavoriteHelper(Context context, AlfrescoAccount acc, AlfrescoSession session, int mode,
            long syncScanningTimeStamp, SyncResult result, Node node)
    {
        super(context, acc, session, mode, syncScanningTimeStamp, result, node);
    }

    public PrepareFavoriteHelper(Context context, AlfrescoAccount acc, AlfrescoSession session, int mode,
            long syncScanningTimeStamp, SyncResult result, String nodeIdentifier)
    {
        super(context, acc, session, mode, syncScanningTimeStamp, result, nodeIdentifier);
    }

    @Override
    public List<FavoriteSync> prepare()
    {
        scan();
        return new ArrayList<FavoriteSync>(0);
    }

    protected void prepareCreation(Uri localUri, Document doc)
    {
        // If listing mode, update Metadata associated
        ContentValues cValues = new ContentValues();
        cValues.put(FavoritesSyncSchema.COLUMN_NODE_ID, doc.getIdentifier());
        cValues.put(FavoritesSyncSchema.COLUMN_SERVER_MODIFICATION_TIMESTAMP, doc.getModifiedAt().getTimeInMillis());
        context.getContentResolver().update(localUri, cValues, null, null);
    }

    protected void rename(Document doc, File localFile, Uri localUri)
    {
        // If Favorite listing simply rename the entry.
        ContentValues cValues = new ContentValues();
        cValues.put(OperationSchema.COLUMN_TITLE, doc.getName());
        context.getContentResolver().update(localUri, cValues, null, null);
    }

    protected void prepareUpdate(Document doc, Cursor cursorId, File localFile, Uri localUri)
    {
    }

    protected void prepareDelete(String id, Cursor cursorId)
    {
        // If Favorite listing simply delete the entry.
        context.getContentResolver().delete(
                FavoritesSyncManager.getUri(cursorId.getLong(FavoritesSyncSchema.COLUMN_ID_ID)), null, null);
    }
}
