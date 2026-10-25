package com.virtualkeys.oneui.overlay;

import android.view.View;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.savedstate.SavedStateRegistryOwner;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;

/**
 * Attaches the lifecycle/saved-state/view-model owners Compose needs to a view
 * that is not hosted by an Activity. Kept in Java because the Kotlin declarations
 * are hidden from Kotlin callers in newer AndroidX versions.
 */
@SuppressWarnings("deprecation")
public final class ViewTreeOwners {

    private ViewTreeOwners() {
    }

    public static void attach(
        View view,
        LifecycleOwner lifecycleOwner,
        ViewModelStoreOwner viewModelStoreOwner,
        SavedStateRegistryOwner savedStateRegistryOwner
    ) {
        ViewTreeLifecycleOwner.set(view, lifecycleOwner);
        ViewTreeViewModelStoreOwner.set(view, viewModelStoreOwner);
        ViewTreeSavedStateRegistryOwner.set(view, savedStateRegistryOwner);
    }
}
