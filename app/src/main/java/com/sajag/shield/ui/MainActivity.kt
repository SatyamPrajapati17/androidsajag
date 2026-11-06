package com.sajag.shield.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.sajag.shield.ui.screens.MainShieldScreen
import com.sajag.shield.ui.theme.SajagMobileShieldTheme
import com.sajag.shield.ui.viewmodel.ShieldViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: ShieldViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
        val phoneStateGranted = permissions[android.Manifest.permission.READ_PHONE_STATE] ?: false
        val contactsGranted = permissions[android.Manifest.permission.READ_CONTACTS] ?: false

        Log.d("MainActivity", "Permissions: audio=$recordAudioGranted phone=$phoneStateGranted contacts=$contactsGranted")
        requestTelecomRoles()
    }

    private val callScreeningRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Sajag Active Call Screening Enabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            SajagMobileShieldTheme {
                MainShieldScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            requestTelecomRoles()
        }
    }

    private fun requestTelecomRoles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
            roleManager?.let { rm ->
                if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                    !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                ) {
                    val intent = rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    callScreeningRoleLauncher.launch(intent)
                }
            }
        }

        // Overlay permission for floating HUD
        if (!Settings.canDrawOverlays(this)) {
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try {
                startActivity(overlayIntent)
            } catch (e: Exception) {
                Log.w("MainActivity", "Unable to launch overlay settings: ${e.message}")
            }
        }
    }
}
