package de.wikilab.dicticc;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by mw on 26.05.15.
 */
public class DictfileManager {
    public static class Dictfileitem implements Comparable<Dictfileitem> {
        public String title,file_prefix; boolean selected;
        @Override
        public String toString() {
            return title;
        }
        @Override
        public int compareTo(Dictfileitem arg0) {
            return title.compareTo(arg0.title);
        }
    }


    public static ArrayList<Dictfileitem> getList(Context ctx) {
        ArrayList<Dictfileitem> itemlist = new ArrayList<Dictfileitem>();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String currentDictFile = pref.getString("dictFile", null);
        boolean sortList = pref.getBoolean("behav_sort_dict_list", true);

        File[] subfiles = new File(Dict.getPath(ctx)).listFiles();
        for(int i = 0; i < subfiles.length; i++) {
            String name = subfiles[i].getName();
            if (name.endsWith("_info")) {
                Dictfileitem item = new Dictfileitem();
                item.file_prefix = name.substring(0, name.length()-5);

                if (item.file_prefix.equals(currentDictFile)) item.selected = true;

                try {
                    BufferedReader bfr = new BufferedReader(new InputStreamReader(Dict.openForRead(ctx, name)));
                    item.title = bfr.readLine();
                    bfr.close();
                } catch (Exception ex) {}
                if (item.title == null) item.title = name;
                itemlist.add(item);
            }
        }

        if (sortList) Collections.sort(itemlist);
        return itemlist;
    }

}
