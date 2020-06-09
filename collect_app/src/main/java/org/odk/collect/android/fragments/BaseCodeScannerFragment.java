/* Copyright (C) 2017 Shobhit
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.fragments;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import timber.log.Timber;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import org.jetbrains.annotations.NotNull;
import org.odk.collect.android.R;
import org.odk.collect.android.utilities.CameraUtils;
import org.odk.collect.android.utilities.ToastUtils;

import org.odk.collect.android.utilities.WidgetAppearanceUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.zip.DataFormatException;

public abstract class BaseCodeScannerFragment extends Fragment implements DecoratedBarcodeView.TorchListener {
    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private Button switchFlashlightButton;
    private BeepManager beepManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        beepManager = new BeepManager(getActivity());

        View rootView = inflater.inflate(R.layout.fragment_scan, container, false);
        barcodeScannerView = rootView.findViewById(R.id.barcode_view);
        barcodeScannerView.setTorchListener(this);

        capture = new CaptureManager(getActivity(), barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        if (frontCameraUsed()) {
            switchToFrontCamera();
        }

        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                beepManager.playBeepSoundAndVibrate();
                try {
                    handleScanningResult(result);
                } catch (IOException | DataFormatException | IllegalArgumentException e) {
                    Timber.e(e);
                    ToastUtils.showShortToast(getString(R.string.invalid_qrcode));
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {

            }
        });

        switchFlashlightButton = rootView.findViewById(R.id.switch_flashlight);
        switchFlashlightButton.setOnClickListener(v -> switchFlashlight());
        // if the device does not have flashlight in its camera, then remove the switch flashlight button...
        if (!hasFlash() || frontCameraUsed()) {
            switchFlashlightButton.setVisibility(View.GONE);
        }

        return rootView;
    }

    private void switchToFrontCamera() {
        CameraSettings cameraSettings = new CameraSettings();
        cameraSettings.setRequestedCameraId(CameraUtils.getFrontCameraId());
        barcodeScannerView.getBarcodeView().setCameraSettings(cameraSettings);
    }

    private Intent getIntent() {
        Intent intent = new IntentIntegrator(getActivity())
                .setDesiredBarcodeFormats(getSupportedCodeFormats())
                .setPrompt(getContext().getString(R.string.barcode_scanner_prompt))
                .createScanIntent();
        intent.putExtra(Intents.Scan.SCAN_TYPE, Intents.Scan.MIXED_SCAN);
        return intent;
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        capture.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        barcodeScannerView.pauseAndWait();
        capture.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        barcodeScannerView.resume();
        capture.onResume();
    }

    @Override
    public void onDestroy() {
        capture.onDestroy();
        super.onDestroy();
    }

    private boolean hasFlash() {
        return getActivity().getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private boolean frontCameraUsed() {
        Bundle bundle = getActivity().getIntent().getExtras();
        return bundle != null && bundle.getBoolean(WidgetAppearanceUtils.FRONT);
    }

    private void switchFlashlight() {
        if (getString(R.string.turn_on_flashlight).equals(switchFlashlightButton.getText())) {
            barcodeScannerView.setTorchOn();
        } else {
            barcodeScannerView.setTorchOff();
        }
    }

    @Override
    public void onTorchOn() {
        switchFlashlightButton.setText(R.string.turn_off_flashlight);
    }

    @Override
    public void onTorchOff() {
        switchFlashlightButton.setText(R.string.turn_on_flashlight);
    }

    protected abstract Collection<String> getSupportedCodeFormats();

    protected abstract void handleScanningResult(BarcodeResult result) throws IOException, DataFormatException;
}
