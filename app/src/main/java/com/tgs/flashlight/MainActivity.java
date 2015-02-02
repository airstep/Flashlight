package com.tgs.flashlight;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

import java.util.List;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        hideSystemTitleBar();
        hideNavigationBar();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new FlashlightFragment())
                    .commit();
        }
    }

    private void hideSystemTitleBar() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class FlashlightFragment extends Fragment implements SurfaceHolder.Callback {

        ImageButton btnSwitch;
        TextView tvBattery;
        private boolean isLightOn = false;

        private Camera camera;
        Camera.Parameters params;
        private boolean mIsFlashFound;

        @Override
        public void onStart() {
            super.onStart();
            mIsFlashFound = false;
            if (getView() != null) {
                SurfaceView preview = (SurfaceView) getView().findViewById(R.id.PREVIEW);
                SurfaceHolder mHolder = preview.getHolder();
                mHolder.addCallback(this);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_main, container, false);

            btnSwitch = (ImageButton) view.findViewById(R.id.flashlight_button);
            tvBattery = (TextView) view.findViewById(R.id.battery);

            toggleButtonImage();

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideNavigationBar();
                }
            });
            btnSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isLightOn) {
                        turnOffFlash();
                    } else {
                        turnOnFlash();
                    }
                }
            });
            return view;
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        private void hideNavigationBar() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                View decorView = getActivity().getWindow().getDecorView();
                int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN;
                decorView.setSystemUiVisibility(uiOptions);
            }
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            getActivity().registerReceiver(mBatInfoReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }

        private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                int level = intent.getIntExtra("level", 0);
                tvBattery.setText(String.valueOf(level) + "%");
            }
        };

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mBatInfoReceiver != null)
                getActivity().unregisterReceiver(mBatInfoReceiver);
        }

        private void turnOnFlash() {

            if (!isLightOn) {
                if (camera == null || params == null) {
                    return;
                }

                if (isHasFlash()) {
                    try {
                        mIsFlashFound = true;
                        params = camera.getParameters();
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(params);
                        camera.startPreview();
                        isLightOn = true;
                        hideNavigationBar();
                        toggleButtonImage();
                    } catch (RuntimeException e) {
                        Toast.makeText(getActivity(), getString(R.string.error) + ":"
                                + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (isAdded()) {
                        if (getActivity() != null) {
                            MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                                    .theme(Theme.DARK)
                                    .title(R.string.error)
                                    .content(R.string.no_flash)
                                    .positiveText(android.R.string.ok)
                                    .callback(new MaterialDialog.ButtonCallback() {
                                        @Override
                                        public void onPositive(MaterialDialog dialog) {
                                            super.onPositive(dialog);
                                            getActivity().finish();
                                            dialog.dismiss();
                                        }
                                    })
                                    .build();
                            dialog.show();
                        }
                    }
                }
            }
        }

        private boolean isHasFlash() {
            if (mIsFlashFound) return true;

            if (camera == null) {
                return false;
            }

            Camera.Parameters parameters = camera.getParameters();

            if (parameters.getFlashMode() == null) {
                return false;
            }

            List<String> supportedFlashModes = parameters.getSupportedFlashModes();

            if (supportedFlashModes == null ||
                    supportedFlashModes.isEmpty()|| supportedFlashModes.size() == 1 &&
                    supportedFlashModes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF)) {
                return false;
            }

            return true;
        }

        private void turnOffFlash() {
            if (isLightOn) {
                if (camera == null || params == null) {
                    return;
                }
                params = camera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(params);
                camera.stopPreview();
                isLightOn = false;
                hideNavigationBar();
                toggleButtonImage();
            }
        }

        private void toggleButtonImage() {
            if (isLightOn) {
                tvBattery.setVisibility(View.VISIBLE);
                btnSwitch.setImageResource(R.drawable.icon_bulb_on);
            } else {
                tvBattery.setVisibility(View.GONE);
                btnSwitch.setImageResource(R.drawable.icon_bulb_off);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (camera != null) {
                camera.stopPreview();
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (camera == null) {
                try {
                    camera = Camera.open();
                    params = camera.getParameters();
                    camera.setPreviewDisplay(holder);
                } catch (Exception e) {
                    if (camera != null)
                        camera.release();
                    camera = null;
                    Toast.makeText(getActivity(), getString(R.string.error) + ":"
                            + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
