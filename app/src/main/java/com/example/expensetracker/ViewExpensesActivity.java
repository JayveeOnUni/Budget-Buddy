package com.example.expensetracker;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensetracker.Adapter.ExpandableExpenseAdapter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;

public class ViewExpensesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ExpandableExpenseAdapter adapter;
    private ExpenseDatabase expenseDb;
    private TextView totalExpensesHeader;
    private List<Expense> allExpenses;
    private String currentFilterCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_expenses);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("View Expenses");
        }

        totalExpensesHeader = findViewById(R.id.total_expenses_header);
        recyclerView = findViewById(R.id.expenses_recycler_view);
        expenseDb = new ExpenseDatabase(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        allExpenses = expenseDb.getAllExpenses();
        loadExpensesByMonth(allExpenses);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_view_expenses, menu);  // Use the menu XML file
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_filter_category) {
            showCategoryFilterDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Show category filter dialog
    private void showCategoryFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter by Category");

        final List<String> categories = expenseDb.getAllCategories();
        categories.add(0, "All");

        CharSequence[] categoryArray = categories.toArray(new CharSequence[0]);
        int checkedItem = (currentFilterCategory == null) ? 0 : categories.indexOf(currentFilterCategory);

        builder.setSingleChoiceItems(categoryArray, checkedItem, (dialog, which) -> {
            if (which == 0) {
                currentFilterCategory = null;  // "All" selected
            } else {
                currentFilterCategory = categories.get(which);
            }
            loadExpensesByMonth(getFilteredExpenses());
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Get filtered expenses by category
    private List<Expense> getFilteredExpenses() {
        if (currentFilterCategory == null) {
            return allExpenses;
        }
        List<Expense> filteredExpenses = new ArrayList<>();
        for (Expense expense : allExpenses) {
            if (expense.getCategory().equals(currentFilterCategory)) {
                filteredExpenses.add(expense);
            }
        }
        return filteredExpenses;
    }

    // Load expenses grouped by month with totals and currency symbol
    private void loadExpensesByMonth(List<Expense> expenses) {
        Map<String, List<Expense>> groupedExpenses = new HashMap<>();
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        for (Expense expense : expenses) {
            String monthYear = monthYearFormat.format(expense.getDate());
            if (!groupedExpenses.containsKey(monthYear)) {
                groupedExpenses.put(monthYear, new ArrayList<>());
            }
            groupedExpenses.get(monthYear).add(expense);
        }

        List<ExpandableExpenseAdapter.ListItem> listItems = new ArrayList<>();
        float grandTotal = 0;

        List<String> months = new ArrayList<>(groupedExpenses.keySet());
        Collections.sort(months, new MonthComparator()); // Sort months descending

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String currency = prefs.getString("currency", "₱");

        for (String month : months) {
            List<Expense> specificMonthExpenses = groupedExpenses.get(month);
            float monthTotal = 0;
            for (Expense expense : specificMonthExpenses) {
                monthTotal += expense.getAmount();
            }

            String headerText = month + " (Total: " + currency + String.format(Locale.getDefault(), "%,.2f", monthTotal) + ")";
            listItems.add(new ExpandableExpenseAdapter.ListItem(ExpandableExpenseAdapter.TYPE_HEADER, headerText));

            grandTotal += monthTotal;

            // Sort expenses by date descending
            Collections.sort(specificMonthExpenses, (a, b) -> b.getDate().compareTo(a.getDate()));

            for (Expense expense : specificMonthExpenses) {
                listItems.add(new ExpandableExpenseAdapter.ListItem(ExpandableExpenseAdapter.TYPE_EXPENSE, expense));
            }
        }

        totalExpensesHeader.setText("Total Expenses: " + currency + String.format(Locale.getDefault(), "%,.2f", grandTotal));

        adapter = new ExpandableExpenseAdapter(listItems, expenseDb, this::loadExpensesByMonth);
        recyclerView.setAdapter(adapter);
    }

    // Comparator to sort months descending by date
    private static class MonthComparator implements Comparator<String> {
        SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        @Override
        public int compare(String month1, String month2) {
            try {
                Date date1 = format.parse(month1);
                Date date2 = format.parse(month2);
                return date2.compareTo(date1); // descending order
            } catch (Exception e) {
                return 0;
            }
        }
    }
}
