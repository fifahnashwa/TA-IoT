package com.example.bismillahberdetak.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bismillahberdetak.R;
import com.example.bismillahberdetak.adapters.HistoryAdapter;
import com.example.bismillahberdetak.models.Reading;
import com.example.bismillahberdetak.utils.CSVExporter;
import com.example.bismillahberdetak.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private View textEmpty;  // Changed from TextView to View (it's a LinearLayout in XML)
    private ChipGroup chipGroupFilter;
    private Chip chipCustom;
    private MaterialButton btnExportCSV, btnShareCSV;

    private FirebaseManager firebaseManager;
    private List<Reading> allReadings = new ArrayList<>();
    private List<Reading> filteredReadings = new ArrayList<>();

    private FilterType currentFilter = FilterType.ALL;
    private Long customStartDate = null;
    private Long customEndDate = null;

    private enum FilterType {
        TODAY, YESTERDAY, LAST_7_DAYS, ALL, CUSTOM
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupFilterChips();
        loadHistory();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_history);
        textEmpty = findViewById(R.id.text_empty);
        chipGroupFilter = findViewById(R.id.chip_group_filter);
        chipCustom = findViewById(R.id.chip_custom);
        btnExportCSV = findViewById(R.id.btn_export_csv);
        btnShareCSV = findViewById(R.id.btn_share_csv);

        firebaseManager = new FirebaseManager();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.history_title);
        }
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(this, filteredReadings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterChips() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_today) {
                currentFilter = FilterType.TODAY;
                applyFilter();
            } else if (checkedId == R.id.chip_yesterday) {
                currentFilter = FilterType.YESTERDAY;
                applyFilter();
            } else if (checkedId == R.id.chip_last_7_days) {
                currentFilter = FilterType.LAST_7_DAYS;
                applyFilter();
            } else if (checkedId == R.id.chip_all) {
                currentFilter = FilterType.ALL;
                applyFilter();
            } else if (checkedId == R.id.chip_custom) {
                showCustomDatePicker();
            }
        });

        // Export CSV button
        btnExportCSV.setOnClickListener(v -> exportCSV());

        // Share CSV button
        btnShareCSV.setOnClickListener(v -> shareCSV());
    }

    private void showCustomDatePicker() {
        // Show dialog to choose: Single Day or Date Range
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_date_mode);

        String[] options = {
                getString(R.string.single_day),
                getString(R.string.date_range)
        };

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Single Day Picker
                showSingleDatePicker();
            } else {
                // Date Range Picker
                showDateRangePicker();
            }
        });

        builder.setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {
            // Uncheck custom chip, revert to previous filter
            uncheckCustomChip();
        });

        builder.setOnCancelListener(dialog -> {
            uncheckCustomChip();
        });

        builder.show();
    }

    private void showSingleDatePicker() {
        // Build constraints (only past dates)
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now());

        // Build single date picker
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_single_date)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Set both start and end to same date (single day)
            customStartDate = selection;
            customEndDate = selection + (24 * 60 * 60 * 1000) - 1; // End of day

            currentFilter = FilterType.CUSTOM;
            updateCustomChipText();
            applyFilter();
        });

        datePicker.addOnNegativeButtonClickListener(v -> {
            uncheckCustomChip();
        });

        datePicker.addOnCancelListener(dialog -> {
            uncheckCustomChip();
        });

        datePicker.show(getSupportFragmentManager(), "SINGLE_DATE_PICKER");
    }

    private void showDateRangePicker() {
        // Build constraints (only past dates)
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now());

        // Build date range picker
        MaterialDatePicker<Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(R.string.select_date_range)
                        .setCalendarConstraints(constraintsBuilder.build())
                        .build();

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            customStartDate = selection.first;
            customEndDate = selection.second + (24 * 60 * 60 * 1000) - 1; // End of day

            currentFilter = FilterType.CUSTOM;
            updateCustomChipText();
            applyFilter();
        });

        dateRangePicker.addOnNegativeButtonClickListener(v -> {
            uncheckCustomChip();
        });

        dateRangePicker.addOnCancelListener(dialog -> {
            uncheckCustomChip();
        });

        dateRangePicker.show(getSupportFragmentManager(), "DATE_RANGE_PICKER");
    }

    private void updateCustomChipText() {
        if (customStartDate == null) return;

        SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        if (customEndDate != null && !isSameDay(customStartDate, customEndDate)) {
            // Date range
            String startDateStr = format.format(new Date(customStartDate));
            String endDateStr = format.format(new Date(customEndDate));

            // Smart formatting: "Jan 15 - 20, 2024" if same month
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());

            String startMonth = monthFormat.format(new Date(customStartDate));
            String endMonth = monthFormat.format(new Date(customEndDate));
            String startDay = dayFormat.format(new Date(customStartDate));
            String endDay = dayFormat.format(new Date(customEndDate));
            String year = yearFormat.format(new Date(customEndDate));

            if (startMonth.equals(endMonth)) {
                chipCustom.setText(startMonth + " " + startDay + " - " + endDay + ", " + year);
            } else {
                chipCustom.setText(startDateStr + " - " + endDateStr);
            }
        } else {
            // Single day
            chipCustom.setText(format.format(new Date(customStartDate)));
        }
    }

    private boolean isSameDay(long millis1, long millis2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(millis1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(millis2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void uncheckCustomChip() {
        // Revert to previous filter chip
        switch (currentFilter) {
            case TODAY:
                chipGroupFilter.check(R.id.chip_today);
                break;
            case YESTERDAY:
                chipGroupFilter.check(R.id.chip_yesterday);
                break;
            case LAST_7_DAYS:
                chipGroupFilter.check(R.id.chip_last_7_days);
                break;
            case ALL:
            default:
                chipGroupFilter.check(R.id.chip_all);
                break;
        }
    }

    private void loadHistory() {
        firebaseManager.fetchHistory(new FirebaseManager.FirebaseCallback<List<Reading>>() {
            @Override
            public void onSuccess(List<Reading> readings) {
                runOnUiThread(() -> {
                    allReadings.clear();
                    allReadings.addAll(readings);
                    applyFilter();
                    Log.d(TAG, "Loaded " + readings.size() + " readings");
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(HistoryActivity.this, "Failed to load history: " + error, Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
            }
        });
    }

    private void applyFilter() {
        filteredReadings.clear();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long todayStart = calendar.getTimeInMillis() / 1000;

        calendar.add(Calendar.DAY_OF_YEAR, -1);
        long yesterdayStart = calendar.getTimeInMillis() / 1000;

        calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long sevenDaysAgo = calendar.getTimeInMillis() / 1000;

        for (Reading reading : allReadings) {
            long timestamp = reading.getTimestamp();

            switch (currentFilter) {
                case TODAY:
                    if (timestamp >= todayStart) {
                        filteredReadings.add(reading);
                    }
                    break;
                case YESTERDAY:
                    if (timestamp >= yesterdayStart && timestamp < todayStart) {
                        filteredReadings.add(reading);
                    }
                    break;
                case LAST_7_DAYS:
                    if (timestamp >= sevenDaysAgo) {
                        filteredReadings.add(reading);
                    }
                    break;
                case CUSTOM:
                    if (customStartDate != null && customEndDate != null) {
                        long readingMillis = timestamp * 1000;
                        if (readingMillis >= customStartDate && readingMillis <= customEndDate) {
                            filteredReadings.add(reading);
                        }
                    }
                    break;
                case ALL:
                default:
                    filteredReadings.add(reading);
                    break;
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();

        Log.d(TAG, "Filter applied: " + currentFilter + ", showing " + filteredReadings.size() + " readings");
    }

    private void updateEmptyState() {
        if (filteredReadings.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            textEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            textEmpty.setVisibility(View.GONE);
        }

        // Enable/disable export buttons
        btnExportCSV.setEnabled(!filteredReadings.isEmpty());
        btnShareCSV.setEnabled(!filteredReadings.isEmpty());
    }

    private void exportCSV() {
        if (filteredReadings.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        CSVExporter.exportToCSV(this, filteredReadings, new CSVExporter.ExportCallback() {
            @Override
            public void onSuccess(File file) {
                Toast.makeText(HistoryActivity.this,
                        getString(R.string.csv_exported) + "\n" + file.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(HistoryActivity.this,
                        getString(R.string.csv_export_failed) + ": " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void shareCSV() {
        if (filteredReadings.isEmpty()) {
            Toast.makeText(this, "No data to share", Toast.LENGTH_SHORT).show();
            return;
        }

        CSVExporter.exportAndShare(this, filteredReadings, new CSVExporter.ExportCallback() {
            @Override
            public void onSuccess(File file) {
                // Share intent will be shown automatically
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(HistoryActivity.this,
                        "Failed to share: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}