package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "expense_tracker.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_EXPENSES = "expenses";
    private static final String TABLE_BALANCE = "balance";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_NOTE = "note";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_BALANCE = "balance_amount";

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public ExpenseDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_EXPENSES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_AMOUNT + " REAL NOT NULL, " +
                COLUMN_CATEGORY + " TEXT NOT NULL, " +
                COLUMN_NOTE + " TEXT, " +
                COLUMN_DATE + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_BALANCE + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_BALANCE + " REAL NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE " + TABLE_BALANCE + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_BALANCE + " REAL NOT NULL)");
        }
    }

    // Original methods (keep unchanged)
    public long addExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_AMOUNT, expense.getAmount());
        values.put(COLUMN_CATEGORY, expense.getCategory());
        values.put(COLUMN_NOTE, expense.getNote());
        values.put(COLUMN_DATE, dateFormat.format(expense.getDate()));
        return db.insert(TABLE_EXPENSES, null, values);
    }

    public List<Expense> getAllExpenses() {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSES, null, null, null, null, null, COLUMN_DATE + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Expense expense = getExpenseFromCursor(cursor);
                expenses.add(expense);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return expenses;
    }

    public boolean updateExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_AMOUNT, expense.getAmount());
        values.put(COLUMN_CATEGORY, expense.getCategory());
        values.put(COLUMN_NOTE, expense.getNote());
        return db.update(TABLE_EXPENSES, values, COLUMN_ID + "=?",
                new String[]{String.valueOf(expense.getId())}) > 0;
    }

    public boolean deleteExpense(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_EXPENSES, COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}) > 0;
    }

    public float getTotalExpenses() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_EXPENSES, null);
        float total = cursor.moveToFirst() ? cursor.getFloat(0) : 0;
        cursor.close();
        return total;
    }

    @SuppressLint("Range")
    private Expense getExpenseFromCursor(Cursor cursor) {
        Expense expense = new Expense();
        expense.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
        expense.setAmount(cursor.getFloat(cursor.getColumnIndex(COLUMN_AMOUNT)));
        expense.setCategory(cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY)));
        expense.setNote(cursor.getString(cursor.getColumnIndex(COLUMN_NOTE)));
        try {
            expense.setDate(dateFormat.parse(cursor.getString(cursor.getColumnIndex(COLUMN_DATE))));
        } catch (ParseException e) {
            expense.setDate(new Date());
        }
        return expense;
    }
    // Add this method to ExpenseDatabase.java
    public List<Expense> getExpensesForMonthCategory(Date startDate, Date endDate, String category) {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Ensure startDate and endDate are not null
        if (startDate == null || endDate == null) {
            return expenses; // Return empty list if dates are invalid
        }

        String selection = COLUMN_DATE + " BETWEEN ? AND ?";
        ArrayList<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(dateFormat.format(startDate));
        selectionArgs.add(dateFormat.format(endDate));

        if (category != null && !category.isEmpty()) {
            selection += " AND " + COLUMN_CATEGORY + " = ?";
            selectionArgs.add(category);
        }

        Cursor cursor = db.query(
                TABLE_EXPENSES,
                null,
                selection,
                selectionArgs.toArray(new String[0]),
                null,
                null,
                COLUMN_DATE + " DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                expenses.add(getExpenseFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return expenses;
    }

    // New balance methods
    public void setInitialBalance(float balance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BALANCE, balance);
        if (getInitialBalance() == 0) {
            db.insert(TABLE_BALANCE, null, values);
        } else {
            db.update(TABLE_BALANCE, values, COLUMN_ID + "=1", null);
        }
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + COLUMN_CATEGORY + " FROM " + TABLE_EXPENSES, null);

        if (cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return categories;
    }

    public float getInitialBalance() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_BALANCE + " FROM " +
                TABLE_BALANCE + " LIMIT 1", null);
        float balance = cursor.moveToFirst() ? cursor.getFloat(0) : 0;
        cursor.close();
        return balance;
    }

    public float getCurrentBalance() {
        return getInitialBalance() - getTotalExpenses();
    }

    public float getMonthlyBalance(String monthYear) {
        return getInitialBalance() - getCumulativeExpensesUpTo(monthYear);
    }

    private float getCumulativeExpensesUpTo(String monthYear) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_AMOUNT + ") FROM " +
                        TABLE_EXPENSES + " WHERE strftime('%Y-%m', " + COLUMN_DATE + ") <= ?",
                new String[]{monthYear});
        float total = cursor.moveToFirst() ? cursor.getFloat(0) : 0;
        cursor.close();
        return total;
    }

    public List<MonthlySummary> getMonthlySummaries() {
        List<MonthlySummary> summaries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT strftime('%Y-%m', " + COLUMN_DATE +
                ") AS month, SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_EXPENSES +
                " GROUP BY month ORDER BY month DESC", null);

        while (cursor.moveToNext()) {
            summaries.add(new MonthlySummary(
                    cursor.getString(0),
                    cursor.getFloat(1),
                    getMonthlyBalance(cursor.getString(0))
            ));
        }
        cursor.close();
        return summaries;
    }

    // Add this method
    @SuppressLint("Range")
    public List<Expense> getExpensesByDate(Date date) {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_DATE + " = ?";
        String[] selectionArgs = new String[]{dateFormat.format(date)};

        Cursor cursor = db.query(
                TABLE_EXPENSES,
                null,
                selection,
                selectionArgs,
                null,
                null,
                COLUMN_DATE + " DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                Expense expense = getExpenseFromCursor(cursor);
                expenses.add(expense);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return expenses;
    }
}
