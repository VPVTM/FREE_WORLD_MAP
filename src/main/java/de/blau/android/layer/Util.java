package de.blau.android.layer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.prefs.AdvancedPrefDatabase;

public final class Util {
    /**
     * Private constructor to avoid instantiation
     */
    private Util() {
        // private
    }

    /**
     * Add a layer on top
     * 
     * @param context an Android Context
     * @param type the LayerType
     */
    public static void addLayer(@NonNull final Context context, @NonNull final LayerType type) {
        addLayer(context, type, null);
    }

    /**
     * Add a layer on top
     * 
     * @param context an Android Context
     * @param type the LayerType
     * @param contentId an id identifying the contents or null
     */
    public static void addLayer(@NonNull final Context context, @NonNull final LayerType type, @Nullable final String contentId) {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(context)) {
            db.insertLayer(db.layerCount(), type, true, contentId);
        }
    }

    /**
     * Add an imagery layer on top of the latest layer of the same type
     * 
     * @param db the pref database
     * @param layerConfigs the current layer config
     * @param isOverlay if true this is an overlay
     * @param id the layer id
     */
    public static void addImageryLayer(@NonNull final AdvancedPrefDatabase db, @NonNull final LayerConfig[] layerConfigs, boolean isOverlay,
            @NonNull final String id) {
        final LayerType layerType = isOverlay ? LayerType.OVERLAYIMAGERY : LayerType.IMAGERY;
        int position = 0;
        for (LayerConfig config : layerConfigs) {
            if (layerType.equals(config.getType()) && config.getPosition() >= position) {
                position = config.getPosition() + 1;
            }
        }
        db.insertLayer(position, layerType, true, id);
    }
}
