
package com.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import edu.uci.arduinosensor.R;
import edu.uci.fallsensor.service.BluetoothMetaData;

import java.util.ArrayList;

public class ListAdapterTitleDescription extends BaseAdapter {
    private ArrayList<BluetoothDevice> devices;
    private LayoutInflater mInflater;
    private BluetoothMetaData metaData;
    public ListAdapterTitleDescription(Context context) {
        devices = new ArrayList<BluetoothDevice>();
        mInflater = LayoutInflater.from(context);
        metaData = BluetoothMetaData.getInstance();
    }

    public void updateList(BluetoothDevice[] devs) {
        devices.clear();
        for (BluetoothDevice device : devs) {
            if (!devices.contains(device)) {
                devices.add(device);
            }
        }
        notifyDataSetInvalidated();
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.adapter_title_description, parent, false);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.deviceName = (TextView) convertView.findViewById(R.id.adapter_text_attribute);
        holder.deviceAddress = (TextView) convertView.findViewById(R.id.adapter_text_info);
        holder.deviceSignal = (TextView) convertView.findViewById(R.id.adapter_text_right_info);
        holder.deviceName.setText(devices.get(position).getName());
        holder.deviceAddress.setText(devices.get(position).getAddress());
        holder.deviceSignal.setText(String.valueOf(metaData.getRssi(devices.get(position))));
        return convertView;
    }

    public ArrayList<BluetoothDevice> getDevices() {
        return devices;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceSignal;
    }

    public void updateDeviceInfo(BluetoothDevice device, int rssi) {

    }
}
