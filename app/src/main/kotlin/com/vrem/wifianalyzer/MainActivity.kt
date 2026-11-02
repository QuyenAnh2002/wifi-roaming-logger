/*
 * WiFiAnalyzer
 * Copyright (C) 2015 - 2026 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 * This program is free software:  you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.vrem.wifianalyzer

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.vrem.annotation.OpenClass
import com.vrem.wifianalyzer.export.CsvExport
import com.vrem.wifianalyzer.export.CsvShare
import com.vrem.wifianalyzer.navigation.NavigationMenu
import com.vrem.wifianalyzer.navigation.NavigationMenuControl
import com.vrem.wifianalyzer.navigation.NavigationMenuController
import com.vrem.wifianalyzer.navigation.availability.runTimerText
import com.vrem.wifianalyzer.navigation.options.OptionMenu
import com.vrem.wifianalyzer.navigation.options.collectRunTimer
import com.vrem.wifianalyzer.wifi.accesspoint.ConnectionView
import com.vrem.wifianalyzer.wifi.logger.ConnectionLogger
import com.vrem.wifianalyzer.wifi.scanner.collectWiFiData

@OpenClass
class MainActivity :
    AppCompatActivity(),
    NavigationMenuControl,
    OnSharedPreferenceChangeListener {
    internal lateinit var drawerNavigation: DrawerNavigation
    internal lateinit var mainReload: MainReload
    internal lateinit var navigationMenuController: NavigationMenuController
    internal lateinit var optionMenu: OptionMenu
    internal lateinit var connectionView: ConnectionView
    internal lateinit var connectionLogger: ConnectionLogger

    internal val permissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission(), ::onPermissionResult)

    // Wi-Fi RTT needs its own runtime permission (NEARBY_WIFI_DEVICES on Android 13+). A denial only disables RTT.
    internal val rttPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val csvExport = CsvExport()
    internal var csvShare = CsvShare(this)

    internal val exportCsvLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv"), ::onExportCsvResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        val mainContext = MainContext.INSTANCE
        mainContext.initialize(this, largeScreen)

        val settings = mainContext.settings
        settings.initializeDefaultValues()
        settings.themeStyle().setTheme(this)

        mainReload = MainReload(settings)

        super.onCreate(savedInstanceState)
        settings.migrateLanguage()
        installSplashScreen()
        setContentView(R.layout.main_activity)

        settings.registerOnSharedPreferenceChangeListener(this)
        optionMenu = OptionMenu()

        keepScreenOn(settings)

        val toolbar = setupToolbar()
        drawerNavigation = DrawerNavigation(this, toolbar)
        drawerNavigation.create()

        navigationMenuController = NavigationMenuController(this)
        navigationMenuController.currentNavigationMenu(settings.selectedMenu())
        onNavigationItemSelected(currentMenuItem())

        connectionView = ConnectionView(this)
        collectWiFiData { connectionView.update(it) }
        collectRunTimer(mainContext.runTimer) { runTimer -> optionMenu.menu?.let { runTimerText(it, runTimer) } }

        connectionLogger = sharedConnectionLogger
            ?: ConnectionLogger(applicationContext, mainContext.wiFiManagerWrapper).also { sharedConnectionLogger = it }

        onBackPressedDispatcher.addCallback(this, MainActivityBackPressed(this))

        if (savedInstanceState == null) {
            handleIntent(intent)
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    public override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerNavigation.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerNavigation.onConfigurationChanged(newConfig)
    }

    internal fun onPermissionResult(granted: Boolean) {
        if (!granted) {
            finish()
        }
    }

    /** Shares every scan observation recorded since the app started, not just the list on screen. */
    fun exportCsv() {
        val file = runCatching { csvShare.prepare(csvExport.fileName()) }.getOrNull()
        if (file == null) {
            Toast.makeText(this, R.string.export_not_available, Toast.LENGTH_LONG).show()
            return
        }
        MainContext.INSTANCE.scanRecorder.export(file) { success ->
            if (isDestroyed) {
                return@export
            }
            val shared = success && runCatching { startActivity(csvShare.chooser(file, saveExportIntent())) }.isSuccess
            if (!shared) {
                Toast.makeText(this, R.string.export_not_available, Toast.LENGTH_LONG).show()
            }
        }
    }

    internal fun saveExport() {
        val file = csvShare.latest()
        if (file == null) {
            Toast.makeText(this, R.string.export_not_available, Toast.LENGTH_LONG).show()
            return
        }
        exportCsvLauncher.launch(file.name)
    }

    internal fun onExportCsvResult(uri: Uri?) {
        if (uri == null) {
            return
        }
        runCatching {
            checkNotNull(csvShare.latest()).inputStream().use { input ->
                contentResolver.openOutputStream(uri)?.use { input.copyTo(it) } ?: error("Cannot open $uri")
            }
        }.onSuccess {
            Toast.makeText(this, R.string.export_saved, Toast.LENGTH_LONG).show()
        }.onFailure {
            runCatching { DocumentsContract.deleteDocument(contentResolver, uri) }
            Toast.makeText(this, R.string.export_not_available, Toast.LENGTH_LONG).show()
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SAVE_EXPORT) {
            saveExport()
        }
    }

    private fun saveExportIntent(): Intent =
        Intent(this, MainActivity::class.java)
            .setAction(ACTION_SAVE_EXPORT)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private val largeScreen: Boolean
        get() {
            val configuration = resources.configuration
            val screenLayoutSize = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
            return screenLayoutSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenLayoutSize == Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String?,
    ) {
        val mainContext = MainContext.INSTANCE
        if (mainReload.shouldReload(mainContext.settings)) {
            MainContext.INSTANCE.scannerService.stop()
            recreate()
        } else {
            keepScreenOn(mainContext.settings)
            update()
        }
    }

    fun update() {
        MainContext.INSTANCE.scannerService.update()
        updateActionBar()
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        closeDrawer()
        val currentNavigationMenu = NavigationMenu.find(menuItem.itemId)
        currentNavigationMenu.activateNavigationMenu(this)
        return true
    }

    fun closeDrawer(): Boolean {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
            return true
        }
        return false
    }

    public override fun onPause() {
        val scannerService = MainContext.INSTANCE.scannerService
        scannerService.pause()
        MainContext.INSTANCE.rttRanger.stop()
        updateActionBar()
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        val scannerService = MainContext.INSTANCE.scannerService
        if (MainContext.INSTANCE.permissionService.permissionGranted()) {
            if (!MainContext.INSTANCE.permissionService.locationEnabled()) {
                startLocationSettings()
            } else {
                MainContext.INSTANCE.locationTracker.start(mainLooper)
                if (MainContext.INSTANCE.runTimer.running) {
                    connectionLogger.start()
                }
            }
            scannerService.resume()
        } else {
            scannerService.pause()
        }
        updateActionBar()
    }

    fun toggleRun() {
        if (MainContext.INSTANCE.runTimer.running) {
            stopRun()
        } else {
            startRun()
        }
    }

    internal fun startRun() {
        val mainContext = MainContext.INSTANCE
        if (!mainContext.permissionService.permissionGranted()) {
            mainContext.permissionService.check()
            return
        }
        if (!mainContext.permissionService.locationEnabled()) {
            startLocationSettings()
            return
        }
        mainContext.locationTracker.start(mainLooper)
        mainContext.runTimer.start()
        connectionLogger.start()
        mainContext.rttRanger.missingPermission?.let { rttPermissionLauncher.launch(it) }
    }

    internal fun stopRun() {
        MainContext.INSTANCE.runTimer.stop()
        connectionLogger.close(::shareRun)
    }

    internal fun shareRun(files: List<Uri>) {
        if (files.isEmpty()) {
            Toast.makeText(this, R.string.no_data, Toast.LENGTH_LONG).show()
            return
        }
        runCatching { startActivity(csvShare.chooser(files)) }
            .onFailure { Toast.makeText(this, R.string.export_not_available, Toast.LENGTH_LONG).show() }
    }

    public override fun onStop() {
        connectionLogger.stop()
        MainContext.INSTANCE.locationTracker.stop()
        MainContext.INSTANCE.scannerService.stop()
        updateActionBar()
        super.onStop()
    }

    public override fun onDestroy() {
        if (!isChangingConfigurations) {
            connectionLogger.close()
            sharedConnectionLogger = null
            MainContext.INSTANCE.runTimer.reset()
        }
        super.onDestroy()
    }

    public override fun onStart() {
        super.onStart()
        if (MainContext.INSTANCE.permissionService.permissionGranted()) {
            if (!MainContext.INSTANCE.permissionService.locationEnabled()) {
                startLocationSettings()
            }
            MainContext.INSTANCE.scannerService.resume()
        } else {
            MainContext.INSTANCE.permissionService.check()
        }
        updateActionBar()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        optionMenu.create(this, menu)
        updateActionBar()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        optionMenu.select(item)
        updateActionBar()
        return true
    }

    fun updateActionBar() = currentNavigationMenu().activateOptions(this)

    override fun currentMenuItem(): MenuItem = navigationMenuController.currentMenuItem()

    override fun currentNavigationMenu(): NavigationMenu = navigationMenuController.currentNavigationMenu()

    override fun currentNavigationMenu(navigationMenu: NavigationMenu) {
        navigationMenuController.currentNavigationMenu(navigationMenu)
        MainContext.INSTANCE.settings.saveSelectedMenu(navigationMenu)
    }

    override fun navigationView(): NavigationView = navigationMenuController.drawerNavigationView

    fun wiFiStatusVisibility(visibility: Int) {
        findViewById<View>(R.id.main_connection).visibility = visibility
        findViewById<View>(R.id.main_warning_container).visibility = visibility
    }

    companion object {
        internal const val ACTION_SAVE_EXPORT = "com.vrem.wifianalyzer.action.SAVE_EXPORT"

        // Holds only the application context, so it survives activity recreation without leaking an activity.
        @SuppressLint("StaticFieldLeak")
        private var sharedConnectionLogger: ConnectionLogger? = null
    }
}
