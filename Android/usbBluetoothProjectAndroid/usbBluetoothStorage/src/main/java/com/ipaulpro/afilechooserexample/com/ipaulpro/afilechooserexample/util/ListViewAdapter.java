package com.ipaulpro.afilechooserexample.com.ipaulpro.afilechooserexample.util;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ipaulpro.afilechooserexample.R;

/**
 * Created by admin on 9/8/2015.
 */
public class ListViewAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final String[] content;
    private final Integer[] imgid;

    public ListViewAdapter(Activity context, String[] content, Integer[] imgid) {
        super(context, R.layout.inflate_list_view, content);
        // TODO Auto-generated constructor stub

        this.context = context;
        this.content = content;
        this.imgid = imgid;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.inflate_list_view, null, true);

        TextView tvContent = (TextView) rowView.findViewById(R.id.tvContent);
        ImageView ivImage = (ImageView) rowView.findViewById(R.id.ivImage);

        tvContent.setText(content[position]);
        ivImage.setImageResource(imgid[position]);
        return rowView;

    };
}