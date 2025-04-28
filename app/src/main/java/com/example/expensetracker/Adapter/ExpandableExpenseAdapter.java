package com.example.expensetracker.Adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensetracker.Expense;
import com.example.expensetracker.ExpenseDatabase;
import com.example.expensetracker.R;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ExpandableExpenseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_EXPENSE = 1;
    private List<ListItem> listItems;
    private ExpenseDatabase expenseDb;
    private final ExpenseListUpdateListener expenseListUpdateListener;

    public ExpandableExpenseAdapter(List<ListItem> listItems, ExpenseDatabase expenseDb, ExpenseListUpdateListener expenseListUpdateListener) {
        this.listItems = listItems;
        this.expenseDb = expenseDb;
        this.expenseListUpdateListener = expenseListUpdateListener;
    }

    public interface ExpenseListUpdateListener {
        void onExpenseListUpdated(List<Expense> expenses);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_expense, parent, false);
            return new ExpenseViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem listItem = listItems.get(position);
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.headerTitle.setText((String) listItem.getData());
        } else if (holder instanceof ExpenseViewHolder) {
            ExpenseViewHolder expenseHolder = (ExpenseViewHolder) holder;
            Expense expense = (Expense) listItem.getData();
            expenseHolder.amountTextView.setText(String.format(Locale.getDefault(), "%.2f", expense.getAmount()));
            expenseHolder.categoryTextView.setText(expense.getCategory());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            expenseHolder.dateTextView.setText(dateFormat.format(expense.getDate()));

            // Attach long click listener
            expenseHolder.itemView.setOnLongClickListener(v -> {
                showExpenseOptionsDialog(expense, position, expenseHolder.itemView.getContext());
                return true;
            });
        }
    }

    // showExpenseOptionsDialog Method
    private void showExpenseOptionsDialog(Expense expense, int position, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Options");
        String[] options = {"Edit", "Delete"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showEditExpenseDialog(expense, position, context);
                } else if (which == 1) {
                    deleteExpense(expense, position, context);
                }
            }
        });
        builder.show();
    }

    @Override
    public int getItemViewType(int position) {
        return listItems.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }

    // ViewHolders
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerTitle;
        HeaderViewHolder(View itemView) {
            super(itemView);
            headerTitle = itemView.findViewById(R.id.header_title);
        }
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView amountTextView, categoryTextView, dateTextView;
        View itemView;
        ExpenseViewHolder(View itemView) {
            super(itemView);
            amountTextView = itemView.findViewById(R.id.expense_amount);
            categoryTextView = itemView.findViewById(R.id.expense_category);
            dateTextView = itemView.findViewById(R.id.expense_date);
            this.itemView = itemView;
        }
    }

    // Data Structure
    public static class ListItem {
        private int type;
        private Object data;  // Could be header (String) or expense (Expense)

        public ListItem(int type, Object data) {
            this.type = type;
            this.data = data;
        }

        public int getType() {
            return type;
        }

        public Object getData() {
            return data;
        }
    }

    // showEditExpenseDialog Method
    @SuppressLint("NotifyDataSetChanged")
    private void showEditExpenseDialog(Expense expense, int position, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Expense");

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_expense, null);

        EditText amountEditText = view.findViewById(R.id.edit_expense_amount);  // Corrected ID
        Spinner categorySpinner = view.findViewById(R.id.edit_expense_category);  // Corrected ID
        EditText noteEditText = view.findViewById(R.id.edit_expense_note);  // Corrected ID

        amountEditText.setText(String.valueOf(expense.getAmount()));

        // Set up the spinner
        List<String> categories = expenseDb.getAllCategories();
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(categoryAdapter);

        // Set the spinner to the current category
        int categoryIndex = categories.indexOf(expense.getCategory());
        categorySpinner.setSelection(categoryIndex);

        noteEditText.setText(expense.getNote());

        builder.setView(view);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    float amount = Float.parseFloat(amountEditText.getText().toString());
                    String category = categorySpinner.getSelectedItem().toString();
                    String note = noteEditText.getText().toString();

                    // Update expense object
                    expense.setAmount(amount);
                    expense.setCategory(category);
                    expense.setNote(note);

                    // Update in db
                    expenseDb.updateExpense(expense);

                    // Update UI
                    listItems.set(position, new ListItem(TYPE_EXPENSE, expense));
                    notifyDataSetChanged();
                    expenseListUpdateListener.onExpenseListUpdated(expenseDb.getAllExpenses());

                    Toast.makeText(context, "Expense updated", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // deleteExpense Method
    @SuppressLint("NotifyDataSetChanged")
    private void deleteExpense(Expense expense, int position, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Expense");
        builder.setMessage("Are you sure you want to delete this expense?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Delete from db
                expenseDb.deleteExpense(expense.getId());

                // Update UI
                listItems.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, listItems.size());
                expenseListUpdateListener.onExpenseListUpdated(expenseDb.getAllExpenses());

                Toast.makeText(context, "Expense deleted", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }
}
