package com.example.bismillahberdetak.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Reading {
    private int heartRate;
    private int spo2;
    private long timestamp;
    private int measurementTime;
    private int samples;
    private int duration;
    private String method;
    private String algorithm;
    private String reference;

    // ✅ NEW: For instant readings
    private Boolean hasValidReading;
    private int instantHR;
    private int instantSPO2;
    private int currentAvgHR;
    private int currentAvgSPO2;
    private int validReadings;
    private int secondsPassed;
    private int totalSeconds;
    private int progress;
    private String status;

    // Empty constructor for Firebase
    public Reading() {
    }

    public Reading(int heartRate, int spo2, long timestamp, int measurementTime,
                   int samples, int duration, String method, String algorithm, String reference) {
        this.heartRate = heartRate;
        this.spo2 = spo2;
        this.timestamp = timestamp;
        this.measurementTime = measurementTime;
        this.samples = samples;
        this.duration = duration;
        this.method = method;
        this.algorithm = algorithm;
        this.reference = reference;
    }

    // Getters and Setters
    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public int getSpo2() {
        return spo2;
    }

    public void setSpo2(int spo2) {
        this.spo2 = spo2;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getMeasurementTime() {
        return measurementTime;
    }

    public void setMeasurementTime(int measurementTime) {
        this.measurementTime = measurementTime;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    // ✅ NEW: Getters and setters for instant reading fields
    public Boolean getHasValidReading() {
        return hasValidReading;
    }

    public void setHasValidReading(Boolean hasValidReading) {
        this.hasValidReading = hasValidReading;
    }

    public int getInstantHR() {
        return instantHR;
    }

    public void setInstantHR(int instantHR) {
        this.instantHR = instantHR;
    }

    public int getInstantSPO2() {
        return instantSPO2;
    }

    public void setInstantSPO2(int instantSPO2) {
        this.instantSPO2 = instantSPO2;
    }

    public int getCurrentAvgHR() {
        return currentAvgHR;
    }

    public void setCurrentAvgHR(int currentAvgHR) {
        this.currentAvgHR = currentAvgHR;
    }

    public int getCurrentAvgSPO2() {
        return currentAvgSPO2;
    }

    public void setCurrentAvgSPO2(int currentAvgSPO2) {
        this.currentAvgSPO2 = currentAvgSPO2;
    }

    public int getValidReadings() {
        return validReadings;
    }

    public void setValidReadings(int validReadings) {
        this.validReadings = validReadings;
    }

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

    public int getSecondsRemaining() {
        return totalSeconds - secondsPassed;
    }

    // Helper methods
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp * 1000));
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp * 1000));
    }

    public String getFormattedDateOnly() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp * 1000));
    }

    public Date getDate() {
        return new Date(timestamp * 1000);
    }

    public HealthStatus getHealthStatus() {
        boolean hrNormal = heartRate >= 60 && heartRate <= 100;
        boolean hrWarning = (heartRate > 100 && heartRate <= 120) || (heartRate >= 40 && heartRate < 60);
        boolean hrCritical = heartRate < 40 || heartRate > 120;

        boolean spo2Normal = spo2 > 95;
        boolean spo2Warning = spo2 >= 90 && spo2 <= 95;
        boolean spo2Critical = spo2 < 90;

        if (hrCritical || spo2Critical) {
            return HealthStatus.CRITICAL;
        } else if (hrWarning || spo2Warning) {
            return HealthStatus.WARNING;
        } else {
            return HealthStatus.NORMAL;
        }
    }

    public enum HealthStatus {
        NORMAL,
        WARNING,
        CRITICAL
    }

    @Override
    public String toString() {
        return "Reading{" +
                "heartRate=" + heartRate +
                ", spo2=" + spo2 +
                ", timestamp=" + timestamp +
                ", hasValidReading=" + hasValidReading +
                ", progress=" + progress +
                '}';
    }
}