package com.example.bismillahberdetak.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.example.bismillahberdetak.models.Reading;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CSVExporter {

    private static final String TAG = "CSVExporter";
    private static final String FILE_PREFIX = "BerdeTak_History_";
    private static final String FILE_EXTENSION = ".csv";

    public interface ExportCallback {
        void onSuccess(File file);
        void onFailure(String error);
    }

    public static void exportToCSV(Context context, List<Reading> readings, ExportCallback callback) {
        if (readings == null || readings.isEmpty()) {
            callback.onFailure("No data to export");
            return;
        }

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = FILE_PREFIX + timestamp + FILE_EXTENSION;

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            File csvFile = new File(downloadsDir, fileName);

            CSVWriter writer = new CSVWriter(new FileWriter(csvFile));

            String[] header = {
                    "Date",
                    "Time",
                    "Heart Rate (bpm)",
                    "SpO2 (%)"
            };
            writer.writeNext(header);

            // Write data rows
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

            for (Reading reading : readings) {
                Date date = new Date(reading.getTimestamp() * 1000);

                String[] row = {
                        dateFormat.format(date),
                        timeFormat.format(date),
                        String.valueOf(reading.getHeartRate()),
                        String.valueOf(reading.getSpo2())
                };

                writer.writeNext(row);
            }

            writer.close();

            Log.d(TAG, "CSV exported successfully: " + csvFile.getAbsolutePath());
            callback.onSuccess(csvFile);

        } catch (IOException e) {
            Log.e(TAG, "Failed to export CSV: " + e.getMessage());
            callback.onFailure("Failed to create CSV file: " + e.getMessage());
        }
    }

    public static void shareCSV(Context context, File csvFile) {
        try {
            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    csvFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BerdeTak Heart Rate & SpO2 History");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Heart Rate and SpO2 measurement history from BerdeTak Monitor.\n\n" +
                            "Data collected using MAX30102 sensor with Maxim Integrated algorithm.\n" +
                            "Based on peer-reviewed research: PMC6514840, PMC9041243, Europace 2023."
            );
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(shareIntent, "Share History via"));

            Log.d(TAG, "Share intent created for: " + csvFile.getName());

        } catch (Exception e) {
            Log.e(TAG, "Failed to share CSV: " + e.getMessage());
        }
    }

    public static void exportAndShare(Context context, List<Reading> readings, ExportCallback callback) {
        exportToCSV(context, readings, new ExportCallback() {
            @Override
            public void onSuccess(File file) {
                shareCSV(context, file);
                callback.onSuccess(file);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    public static List<File> getExportedCSVFiles() {
        List<File> csvFiles = new ArrayList<>();
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        if (downloadsDir.exists() && downloadsDir.isDirectory()) {
            File[] files = downloadsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith(FILE_PREFIX) && file.getName().endsWith(FILE_EXTENSION)) {
                        csvFiles.add(file);
                    }
                }
            }
        }

        return csvFiles;
    }
    public static boolean deleteCSVFile(File file) {
        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            Log.d(TAG, "CSV file deleted: " + deleted);
            return deleted;
        }
        return false;
    }
}