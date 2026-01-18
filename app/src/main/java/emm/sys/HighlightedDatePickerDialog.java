package emm.sys;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class HighlightedDatePickerDialog extends DatePickerDialog {

    private Set<Integer> highlightedDays = new HashSet<>();
    private Drawable highlightDrawable;

    public HighlightedDatePickerDialog(Context context, OnDateSetListener listener,
                                       int year, int month, int day) {
        super(context, listener, year, month, day);
        init();
    }

    public HighlightedDatePickerDialog(Context context, int theme, OnDateSetListener listener,
                                       int year, int month, int day) {
        super(context, theme, listener, year, month, day);
        init();
    }

    private void init() {
        // Create a drawable for highlighted dates
        highlightDrawable = new ColorDrawable(Color.parseColor("#4CAF50")); // Green color
        ((ColorDrawable) highlightDrawable).setAlpha(100); // Semi-transparent

        // Try to highlight dates after dialog is shown
        getDatePicker().post(this::highlightDates);
    }

    public void setHighlightedDays(Set<Integer> days) {
        this.highlightedDays = days;
        highlightDates();
    }

    private void highlightDates() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                highlightDatesNewAPI();
            } else {
                highlightDatesOldAPI();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void highlightDatesNewAPI() {
        DatePicker datePicker = getDatePicker();

        // Get the day picker
        ViewGroup dayPicker = (ViewGroup) datePicker.getChildAt(0);

        if (dayPicker != null) {
            // Find day views
            for (int i = 0; i < dayPicker.getChildCount(); i++) {
                View child = dayPicker.getChildAt(i);

                if (child instanceof ViewGroup) {
                    ViewGroup weekView = (ViewGroup) child;

                    for (int j = 0; j < weekView.getChildCount(); j++) {
                        View dayView = weekView.getChildAt(j);

                        if (dayView != null) {
                            // Extract day number from view
                            String text = dayView.toString();

                            // Try to find day number in the text
                            for (int day : highlightedDays) {
                                if (text.contains(String.valueOf(day))) {
                                    dayView.setBackground(highlightDrawable);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void highlightDatesOldAPI() {
        try {
            // For older APIs, try to use reflection
            DatePicker datePicker = getDatePicker();

            // Get the private mDayPicker field
            Field dayPickerField = DatePicker.class.getDeclaredField("mDayPicker");
            dayPickerField.setAccessible(true);
            ViewGroup dayPicker = (ViewGroup) dayPickerField.get(datePicker);

            if (dayPicker != null) {
                for (int i = 0; i < dayPicker.getChildCount(); i++) {
                    View child = dayPicker.getChildAt(i);

                    if (child != null && child instanceof ViewGroup) {
                        ViewGroup weekView = (ViewGroup) child;

                        for (int j = 0; j < weekView.getChildCount(); j++) {
                            View dayView = weekView.getChildAt(j);

                            if (dayView != null) {
                                // Try to get the text view inside
                                if (dayView instanceof ViewGroup) {
                                    ViewGroup dayContainer = (ViewGroup) dayView;

                                    for (int k = 0; k < dayContainer.getChildCount(); k++) {
                                        View innerView = dayContainer.getChildAt(k);

                                        if (innerView != null) {
                                            String text = innerView.toString();

                                            for (int day : highlightedDays) {
                                                if (text.contains(String.valueOf(day))) {
                                                    innerView.setBackgroundDrawable(highlightDrawable);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}