
package edu.uci.fallsensor.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import edu.uci.arduinosensor.R;

public class SensorDialog extends DialogFragment
        implements OnClickListener {
    SensorDialogOnClickListener mListener;
    private final String msgString;
    private final int sensorStatus;
    public static SensorDialog newInstance(String msg, int status) {
        return new SensorDialog(msg, status);
    }

    private SensorDialog(String msg, int status) {
        msgString = msg;
        sensorStatus = status;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the SensorDialogOnClickListener so we can send events
            // to the host
            mListener = (SensorDialogOnClickListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement " + SensorDialogOnClickListener.class.getSimpleName());
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Sensor Detected Event Status " + sensorStatus)
                .setMessage(msgString)
                .setPositiveButton(getString(R.string.confirm), this)
                .setNegativeButton(getString(R.string.cancel), this);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE:
            case AlertDialog.BUTTON_NEGATIVE:
                mListener.notifySensorDialogListener(which);
                break;
        }
    }

    public interface SensorDialogOnClickListener {
        void notifySensorDialogListener(int choice);
    }
}
