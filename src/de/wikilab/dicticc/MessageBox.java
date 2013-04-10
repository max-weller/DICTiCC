/**
 *  This file is part of DICTiCC.
 *  
 *  DICTiCC is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  DICTiCC is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with DICTiCC.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Copyright (c) 2011 by Max Weller <dicticc@max-weller.de>
 */

package de.wikilab.dicticc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.Html;

public class MessageBox {

	/**
	* Returns a Throwable as a String, if Throwable is null the String "null"
	* will be returned.
	*
	* @param aThrowable
	* @return
	*/
	public static String getStackTrace(final Throwable aThrowable) {
		if (aThrowable == null) {
			return "null";
		}
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}
	
	public static void alert(Context ctx, String text) {
		alert(ctx, text, null, ctx.getString(android.R.string.ok));
	}
	public static void alert(Context ctx, String text, String title) {
		alert(ctx, text, title, ctx.getString(android.R.string.ok));
	}
	public static void alert(Context ctx, String text, String title, String okButtonText) {
		AlertDialog ad = new AlertDialog.Builder(ctx).create();  
	    ad.setCancelable(false); // This blocks the 'BACK' button  
	    ad.setMessage(text);
	    ad.setTitle(title);
	    ad.setButton(okButtonText, new DialogInterface.OnClickListener() {  
	        @Override  
	        public void onClick(DialogInterface dialog, int which) {  
	            dialog.dismiss();                      
	        }  
	    });  
	    ad.show();
	}
	
	public static void showChangeLog(Context context) {
		final Context ctx = context;
		new AlertDialog.Builder(ctx)
    	.setTitle("Change log (PLEASE READ!)")
    	.setMessage(Html.fromHtml(
    			"<p>Version 2012-01-22\n</p>"+
    			"<p> Known bug: Import from ZIP archives crashes on some phones.\n</p>"+
    			"<p> Bugfix: Force close on search in non-existing chunks.\n</p>"+
    			"<p>Version 2012-01-08\n</p>"+
    			"<p> <b>YOU NEED TO RE-IMPORT ALL DICTIONARIES,</b> because of the new search technologie used. You can delete /sdcard/dictionaries after that.</p>\n"+
    			"<p> New Search Technologie: ignores accents and Umlauts because they are difficult to enter with soft keyboard</p>\n"+
    			"<p> Button to select dictionary has been moved to the title bar.</p>\n"+
    			"<p> Added a button to swap the translation direction</p>\n"+
    			"<p> Alternative theme with inverted colors available (Settings)</p>\n"+
    			"<p> Possibility to send bug report on import problems</p>\n"+
    			"</ul>\n\n<p><b>Thank you for using this app! I appreciate your feedback via E-Mail (android@wikilab.de) and Facebook (see Menu).</b></p>"))
    	.setPositiveButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Editor pedit = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
				pedit.putBoolean("show_changelog", true);
				pedit.commit();
				arg0.dismiss();
			}
		})
		.setNeutralButton("Don't show again", new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Editor pedit = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
				pedit.putBoolean("show_changelog", false);
				pedit.commit();
				arg0.dismiss();
			}
		})
		.show();
	}
	
}
