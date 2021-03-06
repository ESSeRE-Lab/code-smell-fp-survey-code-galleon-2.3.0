package org.lnicholls.galleon.apps.iTunes;

/*
 * Copyright (C) 2005 Leon Nicholls
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * 
 * See the file "COPYING" for more details.
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.lnicholls.galleon.app.AppConfiguration;

public class iTunesConfiguration implements AppConfiguration {

    public String getName() {
        return mName;
    }

    public void setName(String value) {
        if (mName != null && !mName.equals(value))
            mModified = true;
        mName = value;
    }

    public String getPlaylistPath() {
        return mPlaylistPath;
    }

    public void setPlaylistPath(String value) {
        if (mPlaylistPath != null && !mPlaylistPath.equals(value))
            mModified = true;
        mPlaylistPath = value;
    }

    public void setModified(boolean value) {
        mModified = value;
    }

    public boolean isModified() {
        return mModified;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
    public boolean isShared()
    {
    	return mShared;
    }
    
    public void setShared(boolean value)
    {
    	mShared = value;
    }
    
    private boolean mShared;    

    private String mName;

    private String mPlaylistPath;

    private boolean mModified;

    private List mPaths = new ArrayList();
}