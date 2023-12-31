package de.blau.android.measure;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import de.blau.android.R;
import de.blau.android.util.Util;

public final class Measure {

    /**
     * Private constructor
     */
    private Measure() {
        // nothing
    }

    /**
     * Check if the preferred measuring app is installed
     * 
     * @param context an Android Context
     * @return true if the package is available
     */
    public static boolean isAvailable(@NonNull Context context) {
        PackageManager pm = context.getPackageManager();
        return Util.isPackageInstalled(context.getString(R.string.streetmeasure_package), pm);
    }

    /**
     * Get an ActivityResultContract for the preferred external measuring app
     * 
     * @return an ActivityResultContract
     */
    public static ActivityResultContract<Params, Length> getContract() {
        return new de.blau.android.measure.streetmeasure.MeasureContract();
    }
}
