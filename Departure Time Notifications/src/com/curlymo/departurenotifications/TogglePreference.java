package com.curlymo.departurenotifications;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

/**
 * A {@link Preference} that provides a two-state toggleable option.
 * <p>
 * This preference will store a boolean into the SharedPreferences.
 */
public class TogglePreference extends TwoStatePreference {
    private final Listener mListener = new Listener();
    private ExternalListener mExternalListener;

    /**
     * Construct a new TogglePreference with the given style options.
     *
     * @param context The Context that will style this preference
     * @param attrs Style attributes that differ from the default
     * @param defStyle Theme attribute defining the default style options
     */
    public TogglePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Construct a new TogglePreference with the given style options.
     *
     * @param context The Context that will style this preference
     * @param attrs Style attributes that differ from the default
     */
    public TogglePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Construct a new TogglePreference with default style options.
     *
     * @param context The Context that will style this preference
     */
    public TogglePreference(Context context) {
        this(context, null);
    }

    /** Inflates a custom layout for this preference, taking advantage of views with ids that are already
     * being used in the Preference base class.
     */
    @Override protected View onCreateView(ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
        return inflater.inflate(R.layout.toggle_preference_layout, parent, false);
    }

    /** Since the Preference base class handles the icon and summary (or summaryOn and summaryOff in TwoStatePreference)
     * we only need to handle the ToggleButton here. Simply get it from the previously created layout, set the data
     * against it and hook up a listener to handle user interaction with the button.
     */
    @Override protected void onBindView(View view) {
        super.onBindView(view);

        ToggleButton toggleButton = (ToggleButton) view.findViewById(R.id.toggle_togglebutton);
        toggleButton.setChecked(isChecked());
        toggleButton.setOnCheckedChangeListener(mListener);
    }

    /** This gets called when the preference (as a whole) is selected by the user. The TwoStatePreference 
     * implementation changes the actual state of this preference, which we don't want, since we're handling
     * preference clicks with our 'external' listener. Hence, don't call super.onClick(), but the onPreferenceClick
     * of our listener. */
    @Override protected void onClick() {
        if (mExternalListener != null) mExternalListener.onPreferenceClick();
    }

    /** Simple interface that defines an external listener that can be notified when the preference has been
     * been clicked. This may be useful e.g. to navigate to a new activity from your PreferenceActivity, or 
     * display a dialog. */
    public static interface ExternalListener {
        void onPreferenceClick();
    }

    /** Sets an external listener for this preference*/
    public void setExternalListener(ExternalListener listener) {
        mExternalListener = listener;
    }

    /** Listener to update the boolean flag that gets stored into the Shared Preferences */
    private class Listener implements CompoundButton.OnCheckedChangeListener {
    	
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!callChangeListener(isChecked)) {
                // Listener didn't like it, change it back.
                // CompoundButton will make sure we don't recurse.
                buttonView.setChecked(!isChecked);
                return;
            }

            TogglePreference.this.setChecked(isChecked);
        }
    }

}