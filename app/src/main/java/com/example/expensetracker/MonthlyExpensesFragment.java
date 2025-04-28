package com.example.expensetracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.Adapter.ExpenseAdapter;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MonthlyExpensesFragment extends Fragment {

    private static final String ARG_MONTH = "month";
    private static final String ARG_YEAR = "year";

    private int month;
    private int year;
    private ExpenseDatabase expenseDb;
    private RecyclerView expenseRecyclerView;
    private ExpenseAdapter expenseAdapter;
    private TextView totalExpensesTextView;
    private Spinner filterCategorySpinner;
    private String filterCategory;
    private List<Expense> expenseList;

    public static MonthlyExpensesFragment newInstance(int month, int year) {
        MonthlyExpensesFragment fragment = new MonthlyExpensesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MONTH, month);
        args.putInt(ARG_YEAR, year);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            month = getArguments().getInt(ARG_MONTH);
            year = getArguments().getInt(ARG_YEAR);
        }
        expenseDb = new ExpenseDatabase(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_monthly_expenses, container, false);

        totalExpensesTextView = view.findViewById(R.id.month_total_expenses);
        expenseRecyclerView = view.findViewById(R.id.expense_recycler_view);
        filterCategorySpinner = view.findViewById(R.id.filter_category_spinner);

        expenseRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.expense_categories_with_all, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterCategorySpinner.setAdapter(adapter);
        filterCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterCategory = position == 0 ? null : parent.getItemAtPosition(position).toString();
                loadExpenses();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                filterCategory = null;
                loadExpenses();
            }
        });

        expenseRecyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    View child = rv.findChildViewUnder(e.getX(), e.getY());
                    if (child != null) {
                        showBalanceDialog();
                        return true;
                    }
                }
                return false;
            }
        });

        loadExpenses();
        return view;
    }

    private void loadExpenses() {
        Calendar startCal = Calendar.getInstance();
        startCal.set(year, month, 1, 0, 0, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.set(year, month, startCal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        endCal.set(Calendar.MILLISECOND, 999);

        expenseList = expenseDb.getExpensesForMonthCategory(startCal.getTime(), endCal.getTime(), filterCategory);
        expenseAdapter = new ExpenseAdapter(expenseList);
        expenseRecyclerView.setAdapter(expenseAdapter);
        updateSummary();
    }

    private void updateSummary() {
        float total = 0;
        for (Expense e : expenseList) total += e.getAmount();

        SharedPreferences prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        String currency = prefs.getString("currency", "$");
        totalExpensesTextView.setText("Total: " + currency + String.format("%.2f", total));
    }

    private void showBalanceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Update Balance");

        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                float newBalance = Float.parseFloat(input.getText().toString());
                requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .edit().putFloat("current_balance", newBalance).apply();
                Toast.makeText(requireContext(), "Balance updated!", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
