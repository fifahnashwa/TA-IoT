package com.example.bismillahberdetak.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.bismillahberdetak.R;
import com.example.bismillahberdetak.models.ConnectionStatus;
import com.example.bismillahberdetak.models.Reading;
import com.example.bismillahberdetak.utils.FirebaseManager;
import com.example.bismillahberdetak.utils.NotificationHelper;
import com.example.bismillahberdetak.views.HistoryLineChartView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final long CONNECTION_CHECK_INTERVAL = 10000; // 10 seconds
    private static final long ESP32_TIMEOUT = 6000; // 3x heartbeat (2s × 3)

    // Views
    private HistoryLineChartView chartView;
    private TextView textHeartRate, textSpo2;
    private TextView textFirebaseStatus, textSensorStatus, textEsp32Status;
    private View indicatorFirebase, indicatorSensor, indicatorEsp32;
    private TextView textStatusMessage, textProgressPercent, textSecondsRemaining;
    private LinearProgressIndicator progressBar;
    private CardView cardProgress;
    private MaterialButton btnStart;

    // Utilities
    private FirebaseManager firebaseManager;
    private NotificationHelper notificationHelper;
    private Handler connectionCheckHandler;
    private Runnable connectionCheckRunnable;

    // State
    private ConnectionStatus connectionStatus;
    private boolean isMeasuring = false;
    private int currentHeartRate = 0;
    private int currentSpo2 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initManagers();
        setupListeners();
        startPeriodicConnectionCheck();

        // Load chart data
        loadChartData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() - Checking connections...");
        checkConnections();
        loadChartData(); // Refresh chart on resume
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPeriodicConnectionCheck();
    }

    private void initViews() {
        // Chart
        chartView = findViewById(R.id.chart_view);

        // Readings
        textHeartRate = findViewById(R.id.text_heart_rate);
        textSpo2 = findViewById(R.id.text_spo2);

        // Connection indicators
        indicatorFirebase = findViewById(R.id.indicator_firebase);
        indicatorSensor = findViewById(R.id.indicator_sensor);
        indicatorEsp32 = findViewById(R.id.indicator_esp32);

        // Connection status texts
        textFirebaseStatus = findViewById(R.id.text_firebase_status);
        textSensorStatus = findViewById(R.id.text_sensor_status);
        textEsp32Status = findViewById(R.id.text_esp32_status);

        // Progress
        cardProgress = findViewById(R.id.card_progress);
        progressBar = findViewById(R.id.progress_bar);
        textProgressPercent = findViewById(R.id.text_progress_percent);
        textSecondsRemaining = findViewById(R.id.text_seconds_remaining);

        // Status message
        textStatusMessage = findViewById(R.id.text_status_message);

        // Button
        btnStart = findViewById(R.id.btn_toggle_measurement);
        btnStart.setEnabled(true);

        // Initial state
        connectionStatus = new ConnectionStatus();
        updateConnectionUI();
        resetProgressUI();
    }

    private void initManagers() {
        firebaseManager = new FirebaseManager();
        notificationHelper = new NotificationHelper(this);
    }

    private void setupListeners() {
        // History button
        findViewById(R.id.btn_history).setOnClickListener(v -> {
            notificationHelper.vibrateClick();
            startActivity(new Intent(MainActivity.this, HistoryActivity.class));
        });

        // Toggle button for Start/Stop
        btnStart.setOnClickListener(v -> {
            notificationHelper.vibrateClick();
            Log.d(TAG, "===== BUTTON CLICKED =====");
            Log.d(TAG, "isMeasuring: " + isMeasuring);
            Log.d(TAG, "isAllConnected: " + connectionStatus.isAllConnected());

            if (isMeasuring) {
                Log.d(TAG, "Showing stop confirmation dialog");
                showStopConfirmationDialog();
            } else {
                if (connectionStatus.isAllConnected()) {
                    Log.d(TAG, "All connected! Starting measurement...");
                    startMeasurement();
                } else {
                    Log.w(TAG, "Not all connected! Cannot start measurement");
                    Toast.makeText(MainActivity.this,
                            "Device not ready. Please wait for all connections.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        // Firebase listeners
        listenToFirebaseStatus();
        listenToInstantReading(); // ✅ SINGLE listener for everything
        listenToLatestReading();
    }

    private void startPeriodicConnectionCheck() {
        connectionCheckHandler = new Handler(Looper.getMainLooper());
        connectionCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isMeasuring) {
                    Log.d(TAG, "Periodic connection check...");
                    checkConnections();
                    checkESP32Alive(); // ✅ Check ESP32 heartbeat
                }
                connectionCheckHandler.postDelayed(this, CONNECTION_CHECK_INTERVAL);
            }
        };
        connectionCheckHandler.postDelayed(connectionCheckRunnable, CONNECTION_CHECK_INTERVAL);
    }

    private void stopPeriodicConnectionCheck() {
        if (connectionCheckHandler != null && connectionCheckRunnable != null) {
            connectionCheckHandler.removeCallbacks(connectionCheckRunnable);
        }
    }

    private void checkConnections() {
        Log.d(TAG, "checkConnections() called");

        // Check Firebase connection
        firebaseManager.checkConnection(new FirebaseManager.FirebaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean connected) {
                if (connected) {
                    connectionStatus.setFirebaseStatus(ConnectionStatus.Status.CONNECTED);
                    updateConnectionUI();
                    checkESP32Connection();
                } else {
                    connectionStatus.setFirebaseStatus(ConnectionStatus.Status.DISCONNECTED);
                    updateConnectionUI();
                    showConnectionError("Firebase disconnected. Please check your internet connection.");
                }
            }

            @Override
            public void onFailure(String error) {
                connectionStatus.setFirebaseStatus(ConnectionStatus.Status.DISCONNECTED);
                updateConnectionUI();
                Log.e(TAG, "Firebase connection check failed: " + error);
            }
        });
    }

    // ✅ NEW: Check ESP32 heartbeat
    private void checkESP32Alive() {
        firebaseManager.getLastSeen(new FirebaseManager.FirebaseCallback<Long>() {
            @Override
            public void onSuccess(Long lastSeen) {
                if (lastSeen != null) {
                    long now = System.currentTimeMillis() / 1000;
                    long timeSinceLastSeen = now - lastSeen;

                    Log.d(TAG, "ESP32 last seen: " + timeSinceLastSeen + "s ago");

                    if (timeSinceLastSeen > 6) { // 6 seconds timeout
                        connectionStatus.setEsp32Status(ConnectionStatus.Status.DISCONNECTED);
                        connectionStatus.setSensorStatus(ConnectionStatus.Status.DISCONNECTED);
                        updateConnectionUI();
                        Log.w(TAG, "ESP32 likely disconnected (last seen " + timeSinceLastSeen + "s ago)");
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to check ESP32 heartbeat: " + error);
            }
        });
    }

    private void checkESP32Connection() {
        firebaseManager.listenToStatus(new FirebaseManager.FirebaseCallback<String>() {
            @Override
            public void onSuccess(String status) {
                Log.d(TAG, "ESP32 Status: " + status);

                if (status == null || status.isEmpty()) {
                    connectionStatus.setEsp32Status(ConnectionStatus.Status.DISCONNECTED);
                    connectionStatus.setSensorStatus(ConnectionStatus.Status.DISCONNECTED);
                    updateConnectionUI();
                    return;
                }

                // ✅ Handle all status values
                switch (status) {
                    case "ready":
                        connectionStatus.setEsp32Status(ConnectionStatus.Status.CONNECTED);
                        connectionStatus.setSensorStatus(ConnectionStatus.Status.CONNECTED);
                        if (!isMeasuring) {
                            btnStart.setEnabled(true);
                            textStatusMessage.setText(R.string.ready);
                        }
                        break;

                    case "measuring":
                        connectionStatus.setEsp32Status(ConnectionStatus.Status.CONNECTED);
                        connectionStatus.setSensorStatus(ConnectionStatus.Status.CONNECTED);
                        if (!isMeasuring) {
                            // ESP32 says measuring but app doesn't know - sync state
                            isMeasuring = true;
                            updateMeasurementUI(true);
                        }
                        break;

                    case "completed":
                        onMeasurementCompleted();
                        break;

                    case "stopped":
                        onMeasurementStopped();
                        break;

                    default:
                        if (status.startsWith("error_")) {
                            handleError(status);
                        } else {
                            connectionStatus.setEsp32Status(ConnectionStatus.Status.CONNECTING);
                            connectionStatus.setSensorStatus(ConnectionStatus.Status.CONNECTING);
                        }
                        break;
                }

                updateConnectionUI();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Status listener error: " + error);
                connectionStatus.setEsp32Status(ConnectionStatus.Status.DISCONNECTED);
                connectionStatus.setSensorStatus(ConnectionStatus.Status.DISCONNECTED);
                updateConnectionUI();
                showConnectionError("ESP32 disconnected: " + error);
            }
        });
    }

    private void listenToFirebaseStatus() {
        // Already handled in checkESP32Connection
    }

    // ✅ SINGLE listener for ALL real-time data (replaces listenToProgress + old instantReading)
    private void listenToInstantReading() {
        firebaseManager.listenToInstantReading(new FirebaseManager.FirebaseCallback<Reading>() {
            @Override
            public void onSuccess(Reading reading) {
                if (reading != null) {
                    runOnUiThread(() -> {
                        if (isMeasuring) {
                            // ✅ Check hasValidReading flag
                            Boolean hasValid = reading.getHasValidReading();

                            if (hasValid != null && hasValid) {
                                // Valid reading - update display
                                Log.d(TAG, "Valid instant: HR=" + reading.getInstantHR() +
                                        ", SpO2=" + reading.getInstantSPO2());

                                // Update readings with instant values
                                animateValueChange(textHeartRate, reading.getInstantHR());
                                animateValueChange(textSpo2, reading.getInstantSPO2());
                            } else {
                                // Invalid reading - don't update, show measuring indicator
                                Log.d(TAG, "Invalid reading, waiting for valid data...");
                                // Optional: show loading indicator or keep previous values
                            }

                            // Always update progress bar (regardless of data validity)
                            updateProgressUI(reading);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Instant reading listener error: " + error);
                if (isMeasuring) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                "Failed to receive real-time data: " + error,
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void listenToLatestReading() {
        firebaseManager.listenToLatestReading(new FirebaseManager.FirebaseCallback<Reading>() {
            @Override
            public void onSuccess(Reading reading) {
                if (reading != null && reading.getHeartRate() > 0 && reading.getSpo2() > 0) {
                    runOnUiThread(() -> {
                        // Only update final readings when measurement completes
                        if (!isMeasuring) {
                            updateReadingUI(reading);
                            loadChartData(); // ✅ Refresh chart after new reading
                        }
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Latest reading listener error: " + error);
            }
        });
    }

    // ✅ Load chart data from history
    private void loadChartData() {
        firebaseManager.fetchLastReadings(10, new FirebaseManager.FirebaseCallback<List<Reading>>() {
            @Override
            public void onSuccess(List<Reading> readings) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Loaded " + readings.size() + " readings for chart");
                    chartView.setReadings(readings);
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to load chart data: " + error);
            }
        });
    }

    private void startMeasurement() {
        Log.d(TAG, "startMeasurement() called - sending START command to Firebase");

        // Immediate UI feedback
        isMeasuring = true;
        updateMeasurementUI(true);
        textStatusMessage.setText("Measuring,please wait...");
        resetProgressUI();

        firebaseManager.sendStartCommand(new FirebaseManager.FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Log.d(TAG, "START command sent successfully!");
                Toast.makeText(MainActivity.this, "Measurement started", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to send START command: " + error);
                // Revert UI on failure
                isMeasuring = false;
                updateMeasurementUI(false);
                textStatusMessage.setText(R.string.ready);
                resetProgressUI();
                Toast.makeText(MainActivity.this, "Failed to start: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void stopMeasurement() {
        Log.d(TAG, "stopMeasurement() called");

        firebaseManager.sendStopCommand(new FirebaseManager.FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                onMeasurementStopped();
                Toast.makeText(MainActivity.this, "Measurement stopped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(MainActivity.this, "Failed to stop: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateConnectionUI() {
        runOnUiThread(() -> {
            // Firebase
            updateStatusIndicator(indicatorFirebase, textFirebaseStatus, connectionStatus.getFirebaseStatus());

            // Sensor
            updateStatusIndicator(indicatorSensor, textSensorStatus, connectionStatus.getSensorStatus());

            // ESP32
            updateStatusIndicator(indicatorEsp32, textEsp32Status, connectionStatus.getEsp32Status());

            // Update button state
            if (!isMeasuring) {
                btnStart.setEnabled(connectionStatus.isAllConnected());
            }
        });
    }

    private void updateStatusIndicator(View indicator, TextView textView, ConnectionStatus.Status status) {
        int color;
        String text;

        switch (status) {
            case CONNECTED:
                color = ContextCompat.getColor(this, R.color.status_connected);
                text = getString(R.string.connected);
                break;
            case CONNECTING:
                color = ContextCompat.getColor(this, R.color.status_connecting);
                text = getString(R.string.connecting);
                break;
            case DISCONNECTED:
            default:
                color = ContextCompat.getColor(this, R.color.status_disconnected);
                text = getString(R.string.disconnected);
                break;
        }

        indicator.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.transparent));
        indicator.setBackgroundColor(color);
        textView.setText(text);
    }

    // ✅ Update progress from Reading object (unified data)
    private void updateProgressUI(Reading reading) {
        cardProgress.setVisibility(View.VISIBLE);
        progressBar.setProgress(reading.getProgress());
        textProgressPercent.setText(reading.getProgress() + "%");
        textSecondsRemaining.setText(getString(R.string.seconds_remaining, reading.getSecondsRemaining()));
    }

    private void updateReadingUI(Reading reading) {
        currentHeartRate = reading.getHeartRate();
        currentSpo2 = reading.getSpo2();

        // Animate value change
        animateValueChange(textHeartRate, currentHeartRate);
        animateValueChange(textSpo2, currentSpo2);
    }

    private void animateValueChange(TextView textView, int targetValue) {
        String currentText = textView.getText().toString();
        if (currentText.equals("--")) {
            textView.setText(String.valueOf(targetValue));
            return;
        }

        int currentValue;
        try {
            currentValue = Integer.parseInt(currentText);
        } catch (NumberFormatException e) {
            textView.setText(String.valueOf(targetValue));
            return;
        }

        if (Math.abs(targetValue - currentValue) < 5) {
            textView.setText(String.valueOf(targetValue));
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        int steps = 10;
        int increment = (targetValue - currentValue) / steps;

        for (int i = 0; i <= steps; i++) {
            final int value = currentValue + (increment * i);
            handler.postDelayed(() -> textView.setText(String.valueOf(value)), i * 30L);
        }
    }

    private void updateMeasurementUI(boolean measuring) {
        Log.d(TAG, "updateMeasurementUI called with measuring=" + measuring);

        if (measuring) {
            Log.d(TAG, "Changing button to STOP mode (red)");
            btnStart.setText(R.string.stop_measurement);
            btnStart.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_stop));
            btnStart.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.button_stop));
            cardProgress.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "Changing button to START mode (green)");
            btnStart.setText(R.string.start_measurement);
            btnStart.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_play));
            btnStart.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.button_start));
            btnStart.setEnabled(connectionStatus.isAllConnected());
        }
    }

    private void resetProgressUI() {
        progressBar.setProgress(0);
        textProgressPercent.setText("0%");
        textSecondsRemaining.setText(getString(R.string.seconds_remaining, 60));
    }

    private void onMeasurementCompleted() {
        Log.d(TAG, "onMeasurementCompleted() called");

        isMeasuring = false;
        updateMeasurementUI(false);
        resetProgressUI();

        // Hide progress card after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            cardProgress.setVisibility(View.GONE);
        }, 1000);

        textStatusMessage.setText(R.string.completed);

        // Show notification
        if (currentHeartRate > 0 && currentSpo2 > 0) {
            Reading reading = new Reading();
            reading.setHeartRate(currentHeartRate);
            reading.setSpo2(currentSpo2);
            notificationHelper.showMeasurementCompleteNotification(reading);
        }

        Toast.makeText(this, "Measurement completed successfully!", Toast.LENGTH_SHORT).show();

        // ✅ Reload chart with new data
        loadChartData();
    }

    private void onMeasurementStopped() {
        Log.d(TAG, "onMeasurementStopped() called");

        isMeasuring = false;
        updateMeasurementUI(false);
        resetProgressUI();
        cardProgress.setVisibility(View.GONE);
        textStatusMessage.setText(R.string.ready);
    }

    private void handleError(String errorStatus) {
        Log.e(TAG, "handleError() called with status: " + errorStatus);

        String errorMessage;
        switch (errorStatus) {
            case "error_finger_removed":
                errorMessage = getString(R.string.error_finger_removed);
                Toast.makeText(this, errorMessage + "\nSilakan ulangi pengukuran", Toast.LENGTH_LONG).show();
                break;
            case "error_invalid":
                errorMessage = getString(R.string.error_invalid_reading);
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                break;
            case "error_range":
                errorMessage = getString(R.string.error_out_of_range);
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                break;
            case "error_no_valid_readings":
                errorMessage = "Tidak ada pembacaan valid selama pengukuran. Pastikan jari Anda menempel dengan benar pada sensor.";
                Toast.makeText(this, errorMessage + "\nSilakan ulangi pengukuran", Toast.LENGTH_LONG).show();
                break;
            default:
                errorMessage = "Unknown error occurred: " + errorStatus;
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                break;
        }

        notificationHelper.showMeasurementErrorNotification(errorMessage);
        notificationHelper.vibrateError();

        // Reset UI properly after error
        isMeasuring = false;
        updateMeasurementUI(false);
        resetProgressUI();
        cardProgress.setVisibility(View.GONE);
        textStatusMessage.setText(R.string.ready);
    }

    private void showConnectionError(String message) {
        if (!isMeasuring) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showStopConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_stop_title)
                .setMessage(R.string.dialog_stop_message)
                .setPositiveButton(R.string.dialog_yes, (dialog, which) -> stopMeasurement())
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPeriodicConnectionCheck();
        firebaseManager.removeAllListeners();
    }
}