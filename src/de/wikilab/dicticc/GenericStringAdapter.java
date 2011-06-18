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

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;



public class GenericStringAdapter extends ArrayAdapter<String> {
	private int ViewResourceId, TextViewResourceId;
	private LayoutInflater Inflater;
	boolean AllowHtml;
	
	public GenericStringAdapter(Context context, int viewResourceId, int textViewResourceId, LayoutInflater inflater,
	String[] objects, boolean allowHtml) {
		super(context, textViewResourceId, objects);
		TextViewResourceId = textViewResourceId;
		ViewResourceId = viewResourceId;
		Inflater = inflater;
		AllowHtml = allowHtml;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row=Inflater.inflate(ViewResourceId, parent, false);
		TextView label=(TextView)row.findViewById(TextViewResourceId);
		
		String txt = getItem(position);
		if (AllowHtml) label.setText(Html.fromHtml(txt)); else label.setText(txt);
		
		return row;
	}
}