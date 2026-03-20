package com.example.hospimanagmenetapp.feature.ehr.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

import com.example.hospimanagmenetapp.util.ValidationUtils;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

// Launches the device camera to scan a patient wristband barcode.
// Expected barcode content: a 10-digit NHS number encoded as CODE_128 or QR.
// SECURITY:
// - The scanned value is validated using NHS Mod 11 before any further action
// - If the barcode is invalid the scan is rejected and the raw value is never
//   logged or displayed
// ACCESSIBILITY:
// - DecoratedBarcodeView includes a built-in viewfinder overlay providing
//  visual guidance for camera positioning

public class BarcodeScannerActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private DecoratedBarcodeView barcodeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        barcodeView = new DecoratedBarcodeView(this);
        setContentView(barcodeView);

        // Request camera permission at runtime before starting scanner
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            startScanning();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            Toast.makeText(this,
                    "Camera permission is required for barcode scanning.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void startScanning() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                barcodeView.pause();

                String scanned = result.getText();

                if (scanned == null || !ValidationUtils.validateNhsNumber(scanned)) {
                    Toast.makeText(BarcodeScannerActivity.this,
                            "Invalid barcode — not a recognised NHS number.",
                            Toast.LENGTH_LONG).show();
                    barcodeView.resume();
                    return;
                }

                Intent i = new Intent(BarcodeScannerActivity.this,
                        PatientSummaryActivity.class);
                i.putExtra("nhsNumber", scanned);
                startActivity(i);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}