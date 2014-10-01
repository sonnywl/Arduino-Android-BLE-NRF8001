package com.adapters;



import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.prac.R;
import com.sensors.Sensors;

public class ListAdapterCheckboxTitle extends BaseAdapter {

    public static final String TAG = ListAdapterCheckboxTitle.class.getSimpleName();
    private LayoutInflater mInflater;
    private String[] data;
    private boolean[] checks;

    public ListAdapterCheckboxTitle(Context context, String[] data) {
        mInflater = LayoutInflater.from(context);
        this.data = data;
        checks = new boolean[data.length];
        for (int i = 0; i < checks.length; i++) {
            checks[i] = false;
        }
    }

    public ListAdapterCheckboxTitle(Context context, String[] data, boolean[] checks) {
        this(context, data);
        if (checks != null) {
            this.checks = checks;
        }
    }

    @Override
    public int getCount() {
        return data.length;
    }

    @Override
    public Object getItem(int position) {
        return data[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.adapter_checkbox_title, parent, false);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.checkbox = (CheckBox) convertView.findViewById(R.id.list_checkbox);
        holder.textview = (TextView) convertView.findViewById(R.id.list_text_view);
        if(data[position].equals(Sensors.BLUETOOTH.getName())) {
            holder.checkbox.setVisibility(View.GONE);
        } else {
            holder.checkbox.setChecked(checks[position]);
        }
        holder.textview.setText(data[position]);
        return convertView;
    }

    public static class ViewHolder {
        CheckBox checkbox;
        TextView textview;
    }

    public void updateCheckBox(int position, boolean isChecked) {
        checks[position] = isChecked;
        notifyDataSetChanged();
    }

    public boolean[] getChecks() {
        return checks;
    }

}
