// File: AddExpenseActivity.java
package com.example.expensetracker;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Date;

public class AddExpenseActivity extends AppCompatActivity {

    private EditText amountEditText;
    private Spinner categorySpinner;
    private EditText noteEditText;
    private ExpenseDatabase expenseDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Add Expense");

        // Initialize database
        expenseDb = new ExpenseDatabase(this);

        // Initialize views
        amountEditText = findViewById(R.id.expense_amount);
        categorySpinner = findViewById(R.id.expense_category);
        noteEditText = findViewById(R.id.expense_note);
        Button saveButton = findViewById(R.id.save_expense_btn);

        // Set up category spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.expense_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        // Set up save button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });
    }

    private void saveExpense() {
        try {
            String amountText = amountEditText.getText().toString();
            if (amountText.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }

            float amount = Float.parseFloat(amountText);
            String category = categorySpinner.getSelectedItem().toString();
            String note = noteEditText.getText().toString();

            Expense expense = new Expense();
            expense.setAmount(amount);
            expense.setCategory(category);
            expense.setNote(note);
            expense.setDate(new Date());

            long result = expenseDb.addExpense(expense);

            if (result > 0) {
                Toast.makeText(this, "Expense saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to save expense", Toast.LENGTH_SHORT).show();
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
        }
    }
}