package com.example.bismillahberdetak.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bismillahberdetak.R;
import com.example.bismillahberdetak.models.Reading;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private Context context;
    private List<Reading> readings;

    public HistoryAdapter(Context context, List<Reading> readings) {
        this.context = context;
        this.readings = readings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reading reading = readings.get(position);

        // Date and time
        holder.textDate.setText(reading.getFormattedDateOnly());
        holder.textTime.setText(reading.getFormattedTime());

        // Readings (just the numbers, no health status)
        holder.textHeartRate.setText(String.valueOf(reading.getHeartRate()));
        holder.textSpo2.setText(String.valueOf(reading.getSpo2()));
    }

    @Override
    public int getItemCount() {
        return readings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView textDate, textTime;
        TextView textHeartRate, textSpo2;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_item);
            textDate = itemView.findViewById(R.id.text_date);
            textTime = itemView.findViewById(R.id.text_time);
            textHeartRate = itemView.findViewById(R.id.text_item_heart_rate);
            textSpo2 = itemView.findViewById(R.id.text_item_spo2);
        }
    }
}