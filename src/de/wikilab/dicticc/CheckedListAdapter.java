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

import java.util.List;

import de.wikilab.dicticc.DictionarySelectActivity.Dictfileitem;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;



public class CheckedListAdapter extends ArrayAdapter<Dictfileitem> {
	private int ViewResourceId, TextViewResourceId, CheckboxResourceId;
	private LayoutInflater Inflater;
	boolean AllowHtml;
	
	public CheckedListAdapter(Context context, int viewResourceId, int textViewResourceId, int checkboxResourceId, LayoutInflater inflater,
	List<Dictfileitem> objects, boolean allowHtml) {
		super(context, textViewResourceId, objects);
		TextViewResourceId = textViewResourceId;
		CheckboxResourceId = checkboxResourceId;
		ViewResourceId = viewResourceId;
		Inflater = inflater;
		AllowHtml = allowHtml;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row=Inflater.inflate(ViewResourceId, parent, false);
		TextView label=(TextView)row.findViewById(TextViewResourceId);
		ImageView checkbox=(ImageView)row.findViewById(CheckboxResourceId);
		
		Dictfileitem item = getItem(position);
		if (AllowHtml) label.setText(Html.fromHtml(item.title)); else label.setText(item.title);
		
		if (item.selected) checkbox.setImageResource(android.R.drawable.checkbox_on_background); else checkbox.setImageResource(android.R.drawable.checkbox_off_background);
		
		return row;
	}
}