package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private Spinner currencySpinner;
    private SharedPreferences preferences;
    private ExpenseDatabase expenseDb;
    private Button btnLight, btnDark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        preferences = getSharedPreferences("settings", MODE_PRIVATE);
        expenseDb = new ExpenseDatabase(this);

        // Initialize all UI components
        currencySpinner = findViewById(R.id.currency_spinner);
        btnLight = findViewById(R.id.btnLight);
        btnDark = findViewById(R.id.btnDark);

        // Load saved theme preference
        loadThemePreference();

        // Setup theme toggle buttons
        setupThemeButtons();

        // Currency spinner setup
        setupCurrencySpinner();

        // Export button
        Button exportButton = findViewById(R.id.export_expenses_btn);
        exportButton.setOnClickListener(v -> showDatePickerDialog());
    }

    private void loadThemePreference() {
        String theme = preferences.getString("theme", "light");
        if (theme.equals("dark")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupThemeButtons() {
        btnLight.setOnClickListener(v -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            saveThemePreference("light");
            Toast.makeText(this, "Theme set to Light !", Toast.LENGTH_SHORT).show();
            recreate();
        });

        btnDark.setOnClickListener(v -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            saveThemePreference("dark");
            Toast.makeText(this, "Theme set to Dark !", Toast.LENGTH_SHORT).show();
            recreate();
        });
    }

    private void saveThemePreference(String theme) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("theme", theme);
        editor.apply();
    }

    private void setupCurrencySpinner() {
        String[] currencies = {"₹", "$", "€", "£", "¥"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(adapter);

        String savedCurrency = preferences.getString("currency", "₹");
        int selectedIndex = 0;
        for (int i = 0; i < currencies.length; i++) {
            if (currencies[i].equals(savedCurrency)) {
                selectedIndex = i;
                break;
            }
        }
        currencySpinner.setSelection(selectedIndex);

        currencySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedCurrency = currencies[position];
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("currency", selectedCurrency);
                editor.apply();
                Toast.makeText(SettingsActivity.this, "Currency set to " + selectedCurrency, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    Date selectedDate = calendar.getTime();
                    exportExpensesByDate(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void exportExpensesByDate(Date date) {
        List<Expense> expenses = expenseDb.getExpensesByDate(date);

        if (expenses.isEmpty()) {
            Toast.makeText(this, "No expenses found for selected date", Toast.LENGTH_LONG).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        sb.append("Expenses for ").append(sdf.format(date)).append(":\n\n");

        float total = 0f;
        for (Expense expense : expenses) {
            sb.append(expense.getCategory())
                    .append(": ")
                    .append(preferences.getString("currency", "₹"))
                    .append(String.format(Locale.getDefault(), "%.2f", expense.getAmount()))
                    .append("\n");
            total += expense.getAmount();
        }
        sb.append("\nTotal: ").append(preferences.getString("currency", "₹")).append(String.format(Locale.getDefault(), "%.2f", total));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Exported Expenses")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
