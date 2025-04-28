package com.example.expensetracker.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.expensetracker.MonthlyExpensesFragment;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MonthlyPagerAdapter extends FragmentStateAdapter {
    private final List<MonthYear> monthYears = new ArrayList<>();

    public MonthlyPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 12; i++) {
            monthYears.add(new MonthYear(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)));
            calendar.add(Calendar.MONTH, -1);
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        MonthYear my = monthYears.get(position);
        return MonthlyExpensesFragment.newInstance(my.month, my.year);
    }

    @Override
    public int getItemCount() {
        return monthYears.size();
    }

    public String getTabTitle(int position) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, monthYears.get(position).month);
        cal.set(Calendar.YEAR, monthYears.get(position).year);
        return new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.getTime());
    }

    private static class MonthYear {
        final int month;
        final int year;
        MonthYear(int month, int year) {
            this.month = month;
            this.year = year;
        }
    }
}
