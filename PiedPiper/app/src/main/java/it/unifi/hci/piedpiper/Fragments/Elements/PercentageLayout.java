package it.unifi.hci.piedpiper.Fragments.Elements;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import it.unifi.hci.piedpiper.R;

public class PercentageLayout extends LinearLayout {
    private final float defaultPercentage;

    public PercentageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PercentageLayout,
                0, 0);
        try {
            defaultPercentage = a.getFloat(R.styleable.PercentageLayout_default_percentage,0);
        } finally {
            a.recycle();
        }
    }

    public float getDefaultPercentage(){
        return defaultPercentage;
    }

    /*
    public PercentageLayout(Context context) {
        super(context);
    }


    public PercentageLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PercentageLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    */
}
