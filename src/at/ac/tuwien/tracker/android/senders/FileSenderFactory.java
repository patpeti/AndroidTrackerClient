/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package at.ac.tuwien.tracker.android.senders;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.os.Environment;
import at.ac.tuwien.tracker.android.common.AppSettings;
import at.ac.tuwien.tracker.android.common.IActionListener;
import at.ac.tuwien.tracker.android.common.Session;
import at.ac.tuwien.tracker.android.common.Utilities;
import at.ac.tuwien.tracker.android.senders.dropbox.DropBoxHelper;
import at.ac.tuwien.tracker.android.senders.email.AutoEmailHelper;
import at.ac.tuwien.tracker.android.senders.gdocs.GDocsHelper;
import at.ac.tuwien.tracker.android.senders.opengts.OpenGTSHelper;
import at.ac.tuwien.tracker.android.senders.osm.OSMHelper;

public class FileSenderFactory
{

    public static IFileSender GetOsmSender(Context applicationContext, IActionListener callback)
    {
        return new OSMHelper(applicationContext, callback);
    }

    public static IFileSender GetDropBoxSender(Context applicationContext, IActionListener callback)
    {
        return new DropBoxHelper(applicationContext, callback);
    }

    public static IFileSender GetGDocsSender(Context applicationContext, IActionListener callback)
    {
        return new GDocsHelper(applicationContext, callback);
    }

    public static IFileSender GetEmailSender(IActionListener callback)
    {
        return new AutoEmailHelper(callback);
    }

    public static IFileSender GetOpenGTSSender(Context applicationContext, IActionListener callback)
    {
        return new OpenGTSHelper(applicationContext, callback);
    }

    public static void SendFiles(Context applicationContext, IActionListener callback)
    {


        final String currentFileName = Session.getCurrentFileName();

        File gpxFolder = new File(Environment.getExternalStorageDirectory(),
                "GPSLogger");

        if (!gpxFolder.exists())
        {
            callback.OnFailure();
            return;
        }

        List<File> files = new ArrayList<File>(Arrays.asList(gpxFolder.listFiles(new FilenameFilter()
        {
            public boolean accept(File file, String s)
            {
                return s.contains(currentFileName) && !s.contains("zip");
            }
        })));

        if (files.size() == 0)
        {
            callback.OnFailure();
            return;
        }

        if (AppSettings.shouldSendZipFile())
        {
            File zipFile = new File(gpxFolder.getPath(), currentFileName + ".zip");
            ArrayList<String> filePaths = new ArrayList<String>();

            for (File f : files)
            {
                filePaths.add(f.getAbsolutePath());
            }

            Utilities.LogInfo("Zipping file");
            ZipHelper zh = new ZipHelper(filePaths.toArray(new String[filePaths.size()]), zipFile.getAbsolutePath());
            zh.Zip();

            //files.clear();
            files.add(zipFile);
        }

        List<IFileSender> senders = GetFileSenders(applicationContext, callback);

        for (IFileSender sender : senders)
        {
            sender.UploadFile(files);
        }
    }


    public static List<IFileSender> GetFileSenders(Context applicationContext, IActionListener callback)
    {
        List<IFileSender> senders = new ArrayList<IFileSender>();

        if (GDocsHelper.IsLinked(applicationContext))
        {
            senders.add(new GDocsHelper(applicationContext, callback));
        }

        if (OSMHelper.IsOsmAuthorized(applicationContext))
        {
            senders.add(new OSMHelper(applicationContext, callback));
        }

        if (AppSettings.isAutoEmailEnabled())
        {
            senders.add(new AutoEmailHelper(callback));
        }

        DropBoxHelper dh = new DropBoxHelper(applicationContext, callback);

        if (dh.IsLinked())
        {
            senders.add(dh);
        }

        if (AppSettings.isAutoOpenGTSEnabled())
        {
            senders.add(new OpenGTSHelper(applicationContext, callback));
        }

        return senders;

    }
}
