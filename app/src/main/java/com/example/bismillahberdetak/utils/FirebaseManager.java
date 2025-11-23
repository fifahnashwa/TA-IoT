package com.example.bismillahberdetak.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bismillahberdetak.models.Reading;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static final String USER_ID = "user001";

    private DatabaseReference databaseReference;
    private DatabaseReference userRef;

    // Listeners
    private ValueEventListener statusListener;
    private ValueEventListener instantReadingListener;
    private ValueEventListener latestListener;
    private ValueEventListener historyListener;

    public interface FirebaseCallback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    public FirebaseManager() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            databaseReference = database.getReference();
            userRef = databaseReference.child("users").child(USER_ID);
            Log.d(TAG, "FirebaseManager initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase: " + e.getMessage());
        }
    }

    // ==================== COMMAND METHODS ====================
    public void sendStartCommand(FirebaseCallback<Void> callback) {
        Log.d(TAG, "sendStartCommand() called");

        if (userRef == null) {
            Log.e(TAG, "❌ ERROR: userRef is NULL!");
            if (callback != null) callback.onFailure("Firebase not initialized");
            return;
        }

        userRef.child("command").setValue("start")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ START command sent successfully");
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to send START: " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void sendStopCommand(FirebaseCallback<Void> callback) {
        userRef.child("command").setValue("stop")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "STOP command sent");
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send STOP: " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    // ==================== STATUS LISTENER ====================
    public void listenToStatus(FirebaseCallback<String> callback) {
        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if (status != null) {
                    Log.d(TAG, "Status changed: " + status);
                    callback.onSuccess(status);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Status listener cancelled: " + error.getMessage());
                callback.onFailure(error.getMessage());
            }
        };

        userRef.child("status").addValueEventListener(statusListener);
    }

    // ✅ NEW: Listen to instant reading (replaces both progress and instant reading)
    public void listenToInstantReading(FirebaseCallback<Reading> callback) {
        instantReadingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "Instant reading doesn't exist yet");
                        return;
                    }

                    Reading reading = snapshot.getValue(Reading.class);

                    if (reading != null) {
                        Log.d(TAG, "Instant reading update: " + reading.toString());
                        callback.onSuccess(reading);
                    } else {
                        Log.d(TAG, "Reading data is null");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing instant reading: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Instant reading listener cancelled: " + error.getMessage());
                callback.onFailure(error.getMessage());
            }
        };

        userRef.child("instantReading").addValueEventListener(instantReadingListener);
        Log.d(TAG, "Started listening to instantReading");
    }

    // ==================== LATEST READING LISTENER ====================
    public void listenToLatestReading(FirebaseCallback<Reading> callback) {
        latestListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "Latest reading doesn't exist yet");
                        return;
                    }

                    Reading reading = snapshot.getValue(Reading.class);
                    if (reading != null && reading.getHeartRate() > 0) {
                        Log.d(TAG, "Latest reading: HR=" + reading.getHeartRate() + ", SpO2=" + reading.getSpo2());
                        callback.onSuccess(reading);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing latest reading: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Latest reading listener cancelled: " + error.getMessage());
                callback.onFailure(error.getMessage());
            }
        };

        userRef.child("latest").addValueEventListener(latestListener);
    }

    // ==================== HISTORY METHODS ====================
    public void fetchHistory(FirebaseCallback<List<Reading>> callback) {
        userRef.child("readings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Reading> readings = new ArrayList<>();
                try {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Reading reading = child.getValue(Reading.class);
                            if (reading != null && reading.getHeartRate() > 0 && reading.getSpo2() > 0) {
                                readings.add(reading);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Skipping invalid reading: " + e.getMessage());
                        }
                    }

                    // Sort by timestamp descending (newest first)
                    Collections.sort(readings, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

                    Log.d(TAG, "Fetched " + readings.size() + " readings");
                    callback.onSuccess(readings);
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching history: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch history: " + error.getMessage());
                callback.onFailure(error.getMessage());
            }
        });
    }

    // ✅ NEW: Fetch last N readings for chart
    public void fetchLastReadings(int limit, FirebaseCallback<List<Reading>> callback) {
        userRef.child("readings")
                .orderByChild("timestamp")
                .limitToLast(limit)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Reading> readings = new ArrayList<>();
                        try {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                Reading reading = child.getValue(Reading.class);
                                if (reading != null && reading.getHeartRate() > 0 && reading.getSpo2() > 0) {
                                    readings.add(reading);
                                }
                            }

                            // Sort ascending (oldest first) for chart display
                            Collections.sort(readings, (r1, r2) -> Long.compare(r1.getTimestamp(), r2.getTimestamp()));

                            Log.d(TAG, "Fetched last " + readings.size() + " readings for chart");
                            callback.onSuccess(readings);
                        } catch (Exception e) {
                            Log.e(TAG, "Error fetching last readings: " + e.getMessage());
                            callback.onFailure(e.getMessage());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to fetch last readings: " + error.getMessage());
                        callback.onFailure(error.getMessage());
                    }
                });
    }

    public void listenToHistory(FirebaseCallback<List<Reading>> callback) {
        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Reading> readings = new ArrayList<>();
                try {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Reading reading = child.getValue(Reading.class);
                            if (reading != null && reading.getHeartRate() > 0 && reading.getSpo2() > 0) {
                                readings.add(reading);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Skipping invalid reading: " + e.getMessage());
                        }
                    }

                    Collections.sort(readings, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

                    Log.d(TAG, "History updated: " + readings.size() + " readings");
                    callback.onSuccess(readings);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading history: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "History listener cancelled: " + error.getMessage());
                callback.onFailure(error.getMessage());
            }
        };

        userRef.child("readings").addValueEventListener(historyListener);
    }

    // ==================== CONNECTION CHECK ====================
    public void getLastSeen(FirebaseCallback<Long> callback) {
        userRef.child("lastSeen").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.exists()) {
                        Long lastSeen = snapshot.getValue(Long.class);
                        callback.onSuccess(lastSeen);
                    } else {
                        callback.onSuccess(null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting lastSeen: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    public void checkConnection(FirebaseCallback<Boolean> callback) {
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class) != null && snapshot.getValue(Boolean.class);
                Log.d(TAG, "Firebase connected: " + connected);
                callback.onSuccess(connected);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Connection check failed: " + error.getMessage());
                callback.onFailure(error.getMessage());
            }
        });
    }

    // ==================== CLEANUP ====================
    public void removeStatusListener() {
        if (statusListener != null) {
            userRef.child("status").removeEventListener(statusListener);
        }
    }

    public void removeInstantReadingListener() {
        if (instantReadingListener != null) {
            userRef.child("instantReading").removeEventListener(instantReadingListener);
            instantReadingListener = null;
            Log.d(TAG, "Removed instant reading listener");
        }
    }

    public void removeLatestReadingListener() {
        if (latestListener != null) {
            userRef.child("latest").removeEventListener(latestListener);
        }
    }

    public void removeHistoryListener() {
        if (historyListener != null) {
            userRef.child("readings").removeEventListener(historyListener);
        }
    }

    public void removeAllListeners() {
        removeStatusListener();
        removeInstantReadingListener();
        removeLatestReadingListener();
        removeHistoryListener();
        Log.d(TAG, "All listeners removed");
    }
}