package net.kwatts.powtools.util.debugdrawer;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import io.palaima.debugdrawer.base.DebugModule;
import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.R;
import net.kwatts.powtools.util.BluetoothUtilImpl;
import net.kwatts.powtools.util.BluetoothUtilMockImpl;

public class DebugDrawerMockBle implements DebugModule {
    private MainActivity mainActivity;

    public DebugDrawerMockBle(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        View view = inflater.inflate(R.layout.debug_drawer_mock_ble, parent, false);

        Switch mockBle = view.findViewById(R.id.debug_drawer_ble_mock);
        mockBle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mainActivity.overrideBluetoothUtil(BluetoothUtilMockImpl::new);
            } else {
                mainActivity.overrideBluetoothUtil(owDevice ->
                        new BluetoothUtilImpl(buttonView.getContext(), owDevice)
                );
            }

        });

        return view;
    }

    @Override
    public void onOpened() {

    }

    @Override
    public void onClosed() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }
}
