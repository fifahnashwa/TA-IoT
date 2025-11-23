package com.example.bismillahberdetak.models;

public class ConnectionStatus {

    public enum Status {
        CONNECTED,
        CONNECTING,
        DISCONNECTED
    }

    private Status firebaseStatus;
    private Status sensorStatus;
    private Status esp32Status;

    public ConnectionStatus() {
        this.firebaseStatus = Status.DISCONNECTED;
        this.sensorStatus = Status.DISCONNECTED;
        this.esp32Status = Status.DISCONNECTED;
    }

    public Status getFirebaseStatus() {
        return firebaseStatus;
    }

    public void setFirebaseStatus(Status firebaseStatus) {
        this.firebaseStatus = firebaseStatus;
    }

    public Status getSensorStatus() {
        return sensorStatus;
    }

    public void setSensorStatus(Status sensorStatus) {
        this.sensorStatus = sensorStatus;
    }

    public Status getEsp32Status() {
        return esp32Status;
    }

    public void setEsp32Status(Status esp32Status) {
        this.esp32Status = esp32Status;
    }

    public boolean isAllConnected() {
        return firebaseStatus == Status.CONNECTED &&
                sensorStatus == Status.CONNECTED &&
                esp32Status == Status.CONNECTED;
    }

    public boolean hasAnyDisconnected() {
        return firebaseStatus == Status.DISCONNECTED ||
                sensorStatus == Status.DISCONNECTED ||
                esp32Status == Status.DISCONNECTED;
    }

    @Override
    public String toString() {
        return "ConnectionStatus{" +
                "firebase=" + firebaseStatus +
                ", sensor=" + sensorStatus +
                ", esp32=" + esp32Status +
                '}';
    }
}