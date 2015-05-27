package de.wikilab.dicticc;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

/**
 * Created by mw on 26.05.15.
 */
public class DictionarySpinnerAdapter extends ArrayAdapter<DictfileManager.Dictfileitem>{
    public DictionarySpinnerAdapter(Context context, ArrayList<DictfileManager.Dictfileitem> items) {
        super(context, android.R.layout.simple_list_item_1, android.R.id.text1, items);
    }

    public int getPositionForTitle(String title) {
        for(int i=0; i<getCount(); i++) {
            if (getItem(i).toString().equals(title)) return i;
        }
        return -1;
    }
}
