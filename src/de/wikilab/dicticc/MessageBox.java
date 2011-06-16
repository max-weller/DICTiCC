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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class MessageBox {

	public static void alert(Context ctx, String text) {
		alert(ctx, text, null, "OK");
	}
	public static void alert(Context ctx, String text, String title) {
		alert(ctx, text, title, "OK");
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
	
}
