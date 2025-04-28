package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
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
import java.io.OutputStream;
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
    private static final int REQUEST_CODE_EXPORT_CSV = 101;
    private static final int REQUEST_CODE_EXPORT_PDF = 102;
    private List<Expense> lastExportedExpenses;
    private Date lastExportedDate;

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
            Toast.makeText(this, "Theme set to Light!", Toast.LENGTH_SHORT).show();
            recreate();
        });

        btnDark.setOnClickListener(v -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            saveThemePreference("dark");
            Toast.makeText(this, "Theme set to Dark!", Toast.LENGTH_SHORT).show();
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

    // --- Export Feature Starts Here ---

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

        lastExportedExpenses = expenses;
        lastExportedDate = date;

        // Show format selection dialog
        showExportFormatDialog();
    }

    private void showExportFormatDialog() {
        String[] formats = {"CSV", "PDF"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Export Format")
                .setItems(formats, (dialog, which) -> {
                    if (which == 0) {
                        exportAsCSV();
                    } else {
                        exportAsPDF();
                    }
                })
                .show();
    }

    private void exportAsCSV() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "expenses.csv");
        startActivityForResult(intent, REQUEST_CODE_EXPORT_CSV);
    }

    private void exportAsPDF() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "expenses.pdf");
        startActivityForResult(intent, REQUEST_CODE_EXPORT_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (requestCode == REQUEST_CODE_EXPORT_CSV) {
                writeCSVToUri(uri, lastExportedExpenses);
            } else if (requestCode == REQUEST_CODE_EXPORT_PDF) {
                writePDFToUri(uri, lastExportedExpenses);
            }
        }
    }

    private void writeCSVToUri(Uri uri, List<Expense> expenses) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            StringBuilder sb = new StringBuilder();
            sb.append("ID,Amount,Category,Note,Date\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            for (Expense expense : expenses) {
                sb.append(expense.getId()).append(",");
                sb.append(String.format(Locale.getDefault(), "%.2f", expense.getAmount())).append(",");
                sb.append("\"").append(expense.getCategory()).append("\",");
                sb.append("\"").append(expense.getNote() != null ? expense.getNote().replace("\"", "\"\"") : "").append("\",");
                sb.append(sdf.format(expense.getDate())).append("\n");
            }
            outputStream.write(sb.toString().getBytes());
            outputStream.close();
            Toast.makeText(this, "Expenses exported as CSV!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void writePDFToUri(Uri uri, List<Expense> expenses) {
        try {
            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setTextSize(14);

            int y = 30;
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            canvas.drawText("Expenses for " + sdf.format(lastExportedDate), 30, y, paint);
            y += 30;
            paint.setTextSize(12);
            canvas.drawText("ID   Amount   Category   Note   Date", 30, y, paint);
            y += 20;

            for (Expense expense : expenses) {
                String line = expense.getId() + "   " +
                        String.format(Locale.getDefault(), "%.2f", expense.getAmount()) + "   " +
                        expense.getCategory() + "   " +
                        (expense.getNote() != null ? expense.getNote() : "") + "   " +
                        sdf.format(expense.getDate());
                canvas.drawText(line, 30, y, paint);
                y += 20;
                if (y > 800) { // New page if needed
                    pdfDocument.finishPage(page);
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 30;
                }
            }

            pdfDocument.finishPage(page);

            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            pdfDocument.writeTo(outputStream);
            pdfDocument.close();
            outputStream.close();
            Toast.makeText(this, "Expenses exported as PDF!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
