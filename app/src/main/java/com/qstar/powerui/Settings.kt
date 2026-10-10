package com.qstar.powerui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class Settings : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var tvCurrentTarget: TextView
    private lateinit var btnClear: Button
    private lateinit var btnLaunchTarget: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var rvApps: RecyclerView

    private lateinit var adapter: AppAdapter
    private val appList = mutableListOf<AppItem>()
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    companion object {
        const val PREFS_NAME = "powerui_prefs"
        const val KEY_TARGET_PACKAGE = "target_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        tvCurrentTarget = findViewById(R.id.tvCurrentTarget)
        btnClear = findViewById(R.id.btnClear)
        btnLaunchTarget = findViewById(R.id.btnLaunchTarget)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvApps = findViewById(R.id.rvApps)

        rvApps.layoutManager = LinearLayoutManager(this)
        val currentTarget = prefs.getString(KEY_TARGET_PACKAGE, null)

        adapter = AppAdapter(appList, currentTarget) { selectedApp ->
            onAppSelected(selectedApp)
        }
        rvApps.adapter = adapter

        updateCurrentTargetHeader(currentTarget)

        btnClear.setOnClickListener {
            clearTarget()
        }

        btnLaunchTarget.setOnClickListener {
            launchCurrentTarget()
        }

        loadInstalledApps()
    }

    private fun updateCurrentTargetHeader(targetPkg: String?) {
        if (!targetPkg.isNullOrBlank()) {
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(targetPkg, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                targetPkg
            }
            tvCurrentTarget.text = "$appName ($targetPkg)"
            btnLaunchTarget.isEnabled = true
            btnLaunchTarget.alpha = 1.0f
        } else {
            tvCurrentTarget.text = getString(R.string.target_none_selected)
            btnLaunchTarget.isEnabled = false
            btnLaunchTarget.alpha = 0.5f
        }
    }

    private fun onAppSelected(app: AppItem) {
        prefs.edit().putString(KEY_TARGET_PACKAGE, app.packageName).apply()
        adapter.setSelected(app.packageName)
        updateCurrentTargetHeader(app.packageName)

        val message = getString(R.string.toast_saved, app.name)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun clearTarget() {
        prefs.edit().remove(KEY_TARGET_PACKAGE).apply()
        adapter.setSelected(null)
        updateCurrentTargetHeader(null)

        Toast.makeText(this, getString(R.string.toast_cleared), Toast.LENGTH_SHORT).show()
    }

    private fun launchCurrentTarget() {
        val targetPkg = prefs.getString(KEY_TARGET_PACKAGE, null)
        if (targetPkg.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.toast_no_target), Toast.LENGTH_SHORT).show()
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPkg)
        if (launchIntent != null) {
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            val appName = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(targetPkg, 0)).toString()
            } catch (e: Exception) {
                targetPkg
            }
            Toast.makeText(this, getString(R.string.toast_launched, appName), Toast.LENGTH_SHORT).show()
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, getString(R.string.toast_no_target), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadInstalledApps() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        backgroundExecutor.execute {
            val pm = packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveList = pm.queryIntentActivities(mainIntent, 0)
            val loadedApps = mutableListOf<AppItem>()
            val currentSelfPkg = packageName

            for (ri in resolveList) {
                val pkg = ri.activityInfo.packageName
                if (pkg == currentSelfPkg) continue // Exclude self

                val label = try {
                    ri.loadLabel(pm).toString()
                } catch (e: Exception) {
                    pkg
                }

                val icon = try {
                    ri.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }

                loadedApps.add(AppItem(packageName = pkg, name = label, icon = icon))
            }

            // Remove duplicates by package name and sort alphabetically by name
            val distinctApps = loadedApps
                .distinctBy { it.packageName }
                .sortedBy { it.name.lowercase() }

            runOnUiThread {
                progressBar.visibility = View.GONE
                appList.clear()
                appList.addAll(distinctApps)
                val currentTarget = prefs.getString(KEY_TARGET_PACKAGE, null)
                adapter.updateData(appList, currentTarget)

                if (appList.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
    }
}
