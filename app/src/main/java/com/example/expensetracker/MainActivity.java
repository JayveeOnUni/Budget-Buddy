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
        // Theme setup first
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String theme = prefs.getString("theme", "light");
        if ("dark".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Sets up toolbar.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Retrieves currency symbol from shared preferences (default: ₱).
        currencySymbol = prefs.getString("currency", "₱"); //

        // Initializes database and UI elements (PieChart, Balance TextView).
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
        List<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.MATERIAL_COLORS) colors.add(c);
        for (int c : ColorTemplate.PASTEL_COLORS) colors.add(c);

        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(14f);

        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Expenses");
        pieChart.setCenterTextSize(24f);
        pieChart.setCenterTextColor(Color.BLACK);
        pieChart.animateY(1000);
        pieChart.invalidate();

        // Budget calculations
        float budget = prefs.getFloat(PREF_BUDGET, 0f);
        float availableBalance = budget - totalExpenses;

        String balanceText = String.format(Locale.getDefault(), "Balance: %s%,.2f", currencySymbol, availableBalance);
        balanceTextView.setText(balanceText);

        if (availableBalance >= 0) {
            balanceTextView.setTextColor(Color.parseColor("#388E3C"));
        } else {
            balanceTextView.setTextColor(Color.parseColor("#D32F2F"));
        }
    }

    private void showEditBudgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Budget");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        float currentBudget = prefs.getFloat(PREF_BUDGET, 0f);
        input.setText(String.format(Locale.getDefault(), "%,.2f", currentBudget));
        input.setSelection(input.getText().length());

        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                float newBudget = Float.parseFloat(input.getText().toString());
                prefs.edit().putFloat(PREF_BUDGET, newBudget).apply();
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
