package com.example.bismillahberdetak.models;

public class ProgressData {
    private int secondsPassed;
    private int totalSeconds;
    private int progress;
    private String status;

    // ✅ NEW: Add real-time readings support
    private int currentAvgSPO2;
    private int currentAvgHR;
    private int validReadings;

    // Empty constructor for Firebase
    public ProgressData() {
    }

    public ProgressData(int secondsPassed, int totalSeconds, int progress, String status) {
        this.secondsPassed = secondsPassed;
        this.totalSeconds = totalSeconds;
        this.progress = progress;
        this.status = status;
    }

    // Getters and Setters
    public int getSecondsPassed() {
        return secondsPassed;
    }

    public void setSecondsPassed(int secondsPassed) {
        this.secondsPassed = secondsPassed;
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    public void setTotalSeconds(int totalSeconds) {
        this.totalSeconds = totalSeconds;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ✅ NEW: Getters and setters for real-time readings
    public int getCurrentAvgSPO2() {
        return currentAvgSPO2;
    }

    public void setCurrentAvgSPO2(int currentAvgSPO2) {
        this.currentAvgSPO2 = currentAvgSPO2;
    }

    public int getCurrentAvgHR() {
        return currentAvgHR;
    }

    public void setCurrentAvgHR(int currentAvgHR) {
        this.currentAvgHR = currentAvgHR;
    }

    public int getValidReadings() {
        return validReadings;
    }

    public void setValidReadings(int validReadings) {
        this.validReadings = validReadings;
    }

    public int getSecondsRemaining() {
        return totalSeconds - secondsPassed;
    }

    // ✅ NEW: Check if real-time data is available
    public boolean hasRealtimeData() {
        return currentAvgHR > 0 && currentAvgSPO2 > 0;
    }

    @Override
    public String toString() {
        return "ProgressData{" +
                "secondsPassed=" + secondsPassed +
                ", totalSeconds=" + totalSeconds +
                ", progress=" + progress +
                ", status='" + status + '\'' +
                ", currentAvgSPO2=" + currentAvgSPO2 +
                ", currentAvgHR=" + currentAvgHR +
                ", validReadings=" + validReadings +
                '}';
    }
}