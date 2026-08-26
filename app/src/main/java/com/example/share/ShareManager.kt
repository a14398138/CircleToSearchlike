package com.example.share

import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.example.model.ShareTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ShareManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("circle_ocr_share_prefs", Context.MODE_PRIVATE)

    private val _lastTextShareTarget = MutableStateFlow<ShareTarget?>(null)
    val lastTextShareTarget: StateFlow<ShareTarget?> = _lastTextShareTarget.asStateFlow()

    private val _lastImageShareTarget = MutableStateFlow<ShareTarget?>(null)
    val lastImageShareTarget: StateFlow<ShareTarget?> = _lastImageShareTarget.asStateFlow()

    init {
        loadPersistedTargets()
    }

    companion object {
        @Volatile
        private var instance: ShareManager? = null

        fun getInstance(context: Context): ShareManager {
            return instance ?: synchronized(this) {
                instance ?: ShareManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun loadPersistedTargets() {
        val textPkg = prefs.getString("last_text_pkg", null)
        val textAct = prefs.getString("last_text_act", null)
        if (textPkg != null) {
            val target = resolveTarget(textPkg, textAct, "text/plain")
            if (target != null) {
                _lastTextShareTarget.value = target
            } else {
                _lastTextShareTarget.value = getAvailableShareTargets("text/plain").firstOrNull()
            }
        } else {
            val defaultTarget = getAvailableShareTargets("text/plain").firstOrNull()
            _lastTextShareTarget.value = defaultTarget
        }

        val imgPkg = prefs.getString("last_img_pkg", null)
        val imgAct = prefs.getString("last_img_act", null)
        if (imgPkg != null) {
            val target = resolveTarget(imgPkg, imgAct, "image/png")
            if (target != null) {
                _lastImageShareTarget.value = target
            } else {
                _lastImageShareTarget.value = getAvailableShareTargets("image/*").firstOrNull()
            }
        } else {
            val defaultTarget = getAvailableShareTargets("image/*").firstOrNull()
            _lastImageShareTarget.value = defaultTarget
        }
    }

    fun getAvailableShareTargets(mimeType: String): List<ShareTarget> {
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager.MATCH_ALL
        } else {
            PackageManager.MATCH_DEFAULT_ONLY
        }
        val resolveInfos: List<ResolveInfo> = try {
            pm.queryIntentActivities(intent, flags)
        } catch (e: Throwable) {
            emptyList()
        }

        return resolveInfos.mapNotNull { info ->
            try {
                val pkg = info.activityInfo.packageName
                if (pkg == appContext.packageName) return@mapNotNull null // Exclude self
                val act = info.activityInfo.name
                val label = info.loadLabel(pm).toString()
                val iconDrawable = info.loadIcon(pm)
                val iconBitmap = drawableToBitmap(iconDrawable)?.asImageBitmap()

                ShareTarget(
                    packageName = pkg,
                    activityName = act,
                    appName = label,
                    iconBitmap = iconBitmap
                )
            } catch (e: Throwable) {
                null
            }
        }.distinctBy { it.packageName }
    }

    fun updateLastShareTargetFromComponent(componentName: ComponentName, isImage: Boolean) {
        val mimeType = if (isImage) "image/png" else "text/plain"
        val target = resolveTarget(componentName.packageName, componentName.className, mimeType)
        if (target != null) {
            updateLastShareTarget(target, isImage)
        }
    }

    private fun resolveTarget(packageName: String, activityName: String?, mimeType: String): ShareTarget? {
        val pm = appContext.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            val iconDrawable = if (!activityName.isNullOrEmpty()) {
                try {
                    pm.getActivityIcon(ComponentName(packageName, activityName))
                } catch (e: Throwable) {
                    pm.getApplicationIcon(appInfo)
                }
            } else {
                pm.getApplicationIcon(appInfo)
            }
            val iconBitmap = drawableToBitmap(iconDrawable)?.asImageBitmap()
            ShareTarget(
                packageName = packageName,
                activityName = activityName,
                appName = label,
                iconBitmap = iconBitmap
            )
        } catch (e: Throwable) {
            null
        }
    }

    fun updateLastShareTarget(target: ShareTarget, isImage: Boolean) {
        if (isImage) {
            _lastImageShareTarget.value = target
            prefs.edit()
                .putString("last_img_pkg", target.packageName)
                .putString("last_img_act", target.activityName)
                .apply()
        } else {
            _lastTextShareTarget.value = target
            prefs.edit()
                .putString("last_text_pkg", target.packageName)
                .putString("last_text_act", target.activityName)
                .apply()
        }
    }

    fun copyTextToClipboard(text: String) {
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Circle OCR Selected Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(appContext, "クリップボードにコピーしました", Toast.LENGTH_SHORT).show()
    }

    fun shareTextGeneral(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

        val chooser = createTrackedChooser(sendIntent, "テキストを共有", isImage = false)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(chooser)
    }

    fun shareTextDirect(text: String, target: ShareTarget) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                if (target.activityName != null) {
                    component = ComponentName(target.packageName, target.activityName)
                } else {
                    setPackage(target.packageName)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            updateLastShareTarget(target, isImage = false)
            Toast.makeText(appContext, "${target.appName} に送信しました", Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            Log.e("ShareManager", "Direct text share failed, opening chooser", e)
            shareTextGeneral(text)
        }
    }

    suspend fun shareImageGeneral(bitmap: Bitmap, title: String = "画像を共有"): Boolean = withContext(Dispatchers.IO) {
        val uri = saveBitmapToCache(bitmap, "share_image_${System.currentTimeMillis()}.png", "images")
            ?: return@withContext false

        withContext(Dispatchers.Main) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = createTrackedChooser(sendIntent, title, isImage = true)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(chooser)
        }
        true
    }

    suspend fun shareImageDirect(bitmap: Bitmap, target: ShareTarget): Boolean = withContext(Dispatchers.IO) {
        val uri = saveBitmapToCache(bitmap, "direct_image_${System.currentTimeMillis()}.png", "crops")
            ?: return@withContext false

        withContext(Dispatchers.Main) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/png"
                    if (target.activityName != null) {
                        component = ComponentName(target.packageName, target.activityName)
                    } else {
                        setPackage(target.packageName)
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(intent)
                updateLastShareTarget(target, isImage = true)
                Toast.makeText(appContext, "${target.appName} に送信しました", Toast.LENGTH_SHORT).show()
            } catch (e: Throwable) {
                Log.e("ShareManager", "Direct image share failed", e)
                shareImageGeneral(bitmap, "画像を共有")
            }
        }
        true
    }

    private fun createTrackedChooser(targetIntent: Intent, title: String, isImage: Boolean): Intent {
        val receiverIntent = Intent(appContext, ChosenComponentReceiver::class.java).apply {
            putExtra(ChosenComponentReceiver.EXTRA_IS_IMAGE, isImage)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            if (isImage) 201 else 202,
            receiverIntent,
            flags
        )
        return Intent.createChooser(targetIntent, title, pendingIntent.intentSender)
    }

    suspend fun copyImageToClipboard(bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        val uri = saveBitmapToCache(bitmap, "clipboard_crop_${System.currentTimeMillis()}.png", "crops")
            ?: return@withContext false

        withContext(Dispatchers.Main) {
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newUri(appContext.contentResolver, "Cropped Image", uri)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(appContext, "画像をクリップボードにコピーしました", Toast.LENGTH_SHORT).show()
        }
        true
    }

    private fun saveBitmapToCache(bitmap: Bitmap, filename: String, subDir: String): Uri? {
        return try {
            val dir = File(appContext.cacheDir, subDir)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file
            )
        } catch (e: Throwable) {
            Log.e("ShareManager", "Failed to save bitmap to cache", e)
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return drawable.bitmap
            }
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Throwable) {
            null
        }
    }
}
