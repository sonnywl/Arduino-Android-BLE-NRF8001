
package edu.uci.fallsensor;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.adapters.ListAdapterTitleDescription;

import java.util.Iterator;

import edu.uci.arduinosensor.R;
import edu.uci.fallsensor.dialog.SensorDialog.SensorDialogOnClickListener;
import edu.uci.fallsensor.service.BLEController;
import edu.uci.fallsensor.service.BluetoothMetaData;
import edu.uci.fallsensor.service.BLEController.BLEServiceListener;
import edu.uci.fallsensor.service.BLEState;

public class MainActivity extends Activity implements
        OnClickListener, OnItemClickListener,
        BLEServiceListener, SensorDialogOnClickListener {
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY = "BDevices";
    private boolean scanning = false;
    private BLEController mController;
    private Button btnScan;
    private ListView lv;
    private TextView tx;
    private ListAdapterTitleDescription mAdapter;
    private BluetoothDevice device = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);
        lv = (ListView) findViewById(R.id.list_view_test);
        tx = (TextView) findViewById(R.id.text_view_test);
        btnScan = (Button) findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(this);
        lv.setOnItemClickListener(this);
        mAdapter = new ListAdapterTitleDescription(this);
        lv.setAdapter(mAdapter);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY)) {
                mAdapter.updateList(savedInstanceState.getParcelableArrayList(KEY).toArray(
                        new BluetoothDevice[] {}));
                checkTextViewDevices();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY, mAdapter.getDevices());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindController();
    }

    @Override
    protected void onPause() {
        unBindController();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mController != null) {
            unBindController();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (scanning) {
            menu.findItem(R.id.action_scanning).setVisible(true);
            menu.findItem(R.id.action_scanning).setActionView(R.layout.actionbar_progressbar);
        } else {
            menu.findItem(R.id.action_scanning).setVisible(false);
            menu.findItem(R.id.action_scanning).setActionView(null);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_msg:
                if (device != null) {
                    mController.sendDeviceMsg(device, "Hi");
                } else {
                    Toast.makeText(this, "Please Select a Device", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_disconnect:
                mController.disconnect(device);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        device = (BluetoothDevice) mAdapter.getItem(position);
        mController.connect(device);
        Toast.makeText(this, "Connected:" + device.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (mController != null) {
            mController.shutdown();
        }
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                mController.scanDevices();
                scanning = true;
                invalidateOptionsMenu();
                break;
        }
    }

    @Override
    public void notifyBLEListener(BluetoothGatt gatt, int rssi) {
        int i = 0;
        Iterator<BluetoothDevice> it = mAdapter.getDevices().iterator();
        BluetoothMetaData data = BluetoothMetaData.getInstance();
        BluetoothDevice bd = null;
        while (it.hasNext()) {
            bd = it.next();
            if (bd.getAddress().equals(device.getAddress())) {
                break;
            }
            i++;
        }
        View view = lv.getChildAt(i -
                lv.getFirstVisiblePosition());
        if (view == null || bd == null) {
            return;
        }
        TextView tx = (TextView) view.findViewById(R.id.adapter_text_right_info);
        tx.setText(data.getRssi(bd));

    }

    @Override
    public void notifyBLEListener(BLEState state, BluetoothDevice[] device) {
        switch (state) {
            default:
            case DEVICE_CONNECT:
                mAdapter.updateList(device);
                checkTextViewDevices();
                scanning = false;
                break;
        }
        invalidateOptionsMenu();
    }

    @Override
    public void notifyBLEListener(BLEState state, String sensorMsg, int status) {
        switch (state) {
            default:
            case CHARACTERISTIC_READ:
            case CHARACTERISTIC_CHANGED:
                /*
                 * SensorDialog dialog = SensorDialog.newInstance(sensorMsg, 0);
                 * dialog.show(getFragmentManager(), "dialog");
                 */
                break;
        }
    }

    private void bindController() {
        mController = BLEController.getInstance(this);
        mController.registerUiListener(this);
        mController.scanDevices();
        Log.i(TAG, "Service Connected " + mController.hashCode());
    }

    private void unBindController() {
        mController.removeUiListener(this);
    }

    private void checkTextViewDevices() {
        if (mAdapter.getCount() > 0) {
            tx.setVisibility(View.GONE);
        } else {
            tx.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void notifySensorDialogListener(int choice) {
        switch (choice) {
            case AlertDialog.BUTTON_NEGATIVE:
                mController.notifyUserDecision(device, 0);
                break;
            case AlertDialog.BUTTON_POSITIVE:
                mController.notifyUserDecision(device, 1);
                break;
        }
    }
}
