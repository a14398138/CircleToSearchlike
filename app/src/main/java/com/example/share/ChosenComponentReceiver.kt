package com.example.share

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * BroadcastReceiver that intercepts the app selected by the user in Intent.createChooser.
 * Updates the last shared app target so its icon appears right next to the Share button.
 */
class ChosenComponentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val componentName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT) as? ComponentName
        }

        val isImage = intent.getBooleanExtra(EXTRA_IS_IMAGE, false)

        Log.d("ChosenComponentReceiver", "User selected share target: $componentName, isImage: $isImage")

        if (componentName != null) {
            ShareManager.getInstance(context).updateLastShareTargetFromComponent(componentName, isImage)
        }
    }

    companion object {
        const val EXTRA_IS_IMAGE = "extra_is_image"
    }
}
