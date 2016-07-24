/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland. 
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version. 
 * 
 * Funf is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.dartmouth.cs.funbewell;

import org.bewellapp.Ml_Toolkit_Application;

import android.content.SharedPreferences;
import edu.mit.media.funf.storage.RemoteArchive;
import edu.mit.media.funf.storage.UploadService;

public class HttpUploadService extends UploadService {

    public static final String UPLOAD_URL = REMOTE_ARCHIVE_ID;
    Ml_Toolkit_Application appState;
    @Override
    protected RemoteArchive getRemoteArchive(String name) {
	SharedPreferences sp = getSharedPreferences(
		Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE);
	appState = (Ml_Toolkit_Application) getApplicationContext();
	return new HttpArchive(sp,appState);
    }
}
