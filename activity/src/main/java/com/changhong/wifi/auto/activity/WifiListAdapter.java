package com.changhong.wifi.auto.activity;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class WifiListAdapter extends BaseAdapter {
    Context context;
    List<Map<String, Object>> mapListData;
    int layout;
    String[] from;
    int[] to;

    WifiListAdapter(Context context, List<Map<String, Object>> mapListData, @LayoutRes int layout, String[] from, @IdRes int[] to) {
        this.context = context;
        this.mapListData = mapListData;
        this.layout = layout;
        this.from = from;
        this.to = to;
    }

    @Override
    public int getCount() {
        return mapListData.size();
    }

    @Override
    public Object getItem(int position) {
        return (null != mapListData) ? mapListData.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return (null != mapListData) ? position : -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = LayoutInflater.from(this.context).inflate(this.layout, null);
        }

        TextView ssid = convertView.findViewById(R.id.SSID);
        TextView bssid = convertView.findViewById(R.id.BSSID);
        ImageView imageView = convertView.findViewById(R.id.img);

        ssid.setText((String) mapListData.get(position).get(from[0]));
        bssid.setText((String) mapListData.get(position).get(from[1]));
        imageView.setImageDrawable((Drawable) mapListData.get(position).get(from[2]));

        return convertView;
    }
}
