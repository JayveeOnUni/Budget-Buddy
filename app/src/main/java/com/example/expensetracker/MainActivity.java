package com.example.expensetracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private PieChart pieChart;
    private ExpenseDatabase expenseDb;
    private SharedPreferences prefs;
    private String currencySymbol;
    private TextView balanceTextView;

    private static final String PREF_BUDGET = "budget";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before anything else
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int theme = prefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(theme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        currencySymbol = prefs.getString("currency", "₹"); // Default to Rupee symbol

        expenseDb = new ExpenseDatabase(this);

        pieChart = findViewById(R.id.expense_chart);
        balanceTextView = findViewById(R.id.balance_text);

        // Set click listener on balance text to edit budget
        balanceTextView.setOnClickListener(v -> showEditBudgetDialog());

        FloatingActionButton addExpenseButton = findViewById(R.id.add_expense_btn);
        addExpenseButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        FloatingActionButton viewExpenseButton = findViewById(R.id.view_expense_btn);
        viewExpenseButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewExpensesActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateChartAndBalance();
    }

    private void updateChartAndBalance() {
        List<Expense> expenses = expenseDb.getAllExpenses();
        Map<String, Float> categoryTotals = new HashMap<>();

        float totalExpenses = 0f;

        for (Expense expense : expenses) {
            String category = expense.getCategory();
            float currentTotal = categoryTotals.getOrDefault(category, 0f);
            categoryTotals.put(category, currentTotal + expense.getAmount());
            totalExpenses += expense.getAmount();
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Expense Categories");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData data = new PieData(dataSet);

        // Increase pie slice value text size
        data.setValueTextSize(14f);

        pieChart.setData(data);

        pieChart.getDescription().setEnabled(false);

        // Set center text and increase its size
        pieChart.setCenterText("Expenses");
        pieChart.setCenterTextSize(24f);
        pieChart.setCenterTextColor(Color.BLACK);

        pieChart.animateY(1000);
        pieChart.invalidate();

        // Get budget from prefs, default 0
        float budget = prefs.getFloat(PREF_BUDGET, 0f);
        float availableBalance = budget - totalExpenses;

        // Format balance text
        String balanceText = String.format(Locale.getDefault(), "Balance: %s%.2f", currencySymbol, availableBalance);
        balanceTextView.setText(balanceText);

        // Change color based on positive or negative balance
        if (availableBalance >= 0) {
            balanceTextView.setTextColor(Color.parseColor("#388E3C")); // Green shade
        } else {
            balanceTextView.setTextColor(Color.parseColor("#D32F2F")); // Red shade
        }
    }

    private void showEditBudgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Budget");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // Pre-fill with current budget
        float currentBudget = prefs.getFloat(PREF_BUDGET, 0f);
        input.setText(String.format(Locale.getDefault(), "%.2f", currentBudget));
        input.setSelection(input.getText().length());

        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                float newBudget = Float.parseFloat(input.getText().toString());

                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat(PREF_BUDGET, newBudget);
                editor.apply();

                Toast.makeText(this, "Budget updated!", Toast.LENGTH_SHORT).show();

                updateChartAndBalance();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_help) {
            startActivity(new Intent(this, HelpActivity.class));
            return true;
        } else if (id == R.id.action_contact) {
            startActivity(new Intent(this, ContactActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
