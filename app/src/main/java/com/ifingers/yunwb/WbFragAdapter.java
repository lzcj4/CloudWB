package com.ifingers.yunwb;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ifingers.yunwb.dao.ImageListItem;

import java.net.URI;
import java.net.URL;
import java.util.LinkedList;

/**
 *
 * Created by Macoo on 1/25/2016.
 */
public class WbFragAdapter extends BaseAdapter {

    private LinkedList<ImageListItem> mData;
    private Context mContext;
    private FragmentManager mFragManager;

    public  WbFragAdapter(LinkedList<ImageListItem> data, Context context, FragmentManager manager){
        mData = data;
        mContext = context;
        mFragManager = manager;
    }

    public void updateImageLinks(LinkedList<ImageListItem> images){
        mData = images;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mData == null? 0 : mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.attach_list, parent, false);
            holder = new ViewHolder();
            holder.img_icon = (ImageView) convertView.findViewById(R.id.attach_icon);
            holder.txt_content = (TextView) convertView.findViewById(R.id.attach_content);
            convertView.setTag(holder);
        }
        else{
            holder = (ViewHolder) convertView.getTag();
        }

        if (!mData.get(position).getShowFromSDK()) {
            if ("".equals(mData.get(position).getmIconResPath())) {
                holder.img_icon.setBackgroundColor(Color.WHITE);
            } else {
                holder.img_icon.setImageBitmap(BitmapFactory.decodeFile(mData.get(position).getmIconResPath()));
            }
        } else {
            holder.img_icon.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources() ,R.mipmap.record));
        }

        holder.txt_content.setText(mData.get(position).getmTitle());

        return convertView;
    }

    static class ViewHolder{
        ImageView img_icon;
        TextView txt_content;
    }
}
