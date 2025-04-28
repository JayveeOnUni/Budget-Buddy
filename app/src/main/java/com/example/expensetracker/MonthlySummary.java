package com.example.expensetracker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MonthlySummary {
    private final String monthYear;
    private final float totalExpenses;
    private final float remainingBalance;
    private static final SimpleDateFormat inputFormat =
            new SimpleDateFormat("yyyy-MM", Locale.getDefault());
    private static final SimpleDateFormat outputFormat =
            new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    public MonthlySummary(String monthYear, float totalExpenses, float remainingBalance) {
        this.monthYear = monthYear;
        this.totalExpenses = totalExpenses;
        this.remainingBalance = remainingBalance;
    }

    public String getFormattedMonth() {
        try {
            Date date = inputFormat.parse(monthYear);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return monthYear.replace("-", " ");
        }
    }

    public String getFormattedBalance() {
        return String.format(Locale.getDefault(), "₹%.2f", remainingBalance);
    }

    public float getTotalExpenses() {
        return totalExpenses;
    }

    public float getRemainingBalance() {
        return remainingBalance;
    }

    public String getMonthYear() {
        return monthYear;
    }
}
