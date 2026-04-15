package com.arcides.mementoapp.presentation

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.arcides.mementoapp.R
import com.arcides.mementoapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Vinculamos la BottomNavigationView con el NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Gestión de visibilidad de componentes globales
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.splashFragment, R.id.loginFragment, R.id.resetPasswordFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                    binding.fabAdd.visibility = View.GONE
                }
                else -> {
                    // Visibilidad instantánea para evitar animaciones de escala
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.fabAdd.visibility = View.VISIBLE
                }
            }
        }

        // Acción del botón central desacoplada mediante interfaz
        binding.fabAdd.setOnClickListener {
            val navHost = supportFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment
            val currentFragment = navHost?.childFragmentManager?.fragments?.firstOrNull()
            
            // Si el fragmento actual implementa GlobalActionProvider, delegamos la acción
            (currentFragment as? GlobalActionProvider)?.onPrimaryActionClicked()
        }

        // Manejar el Intent si la app se abre por un Deep Link
        handleIntent(intent)

        // Solicitar permisos necesarios
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        // 1. Permiso de Notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // 2. Permiso de Alarmas Exactas (Android 12+)
        checkExactAlarmPermission()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Las notificaciones están desactivadas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showExactAlarmDialog()
            }
        }
    }

    private fun showExactAlarmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de Alarmas Exactas")
            .setMessage("Para que los recordatorios de tus tareas suenen en el momento exacto, la aplicación necesita un permiso especial en los ajustes del sistema.")
            .setPositiveButton("Ir a Ajustes") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                }
            }
            .setNegativeButton("Más tarde", null)
            .show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Manejar Deep Links (como reset-password)
        intent?.data?.let { uri ->
            if (uri.scheme == "mementoapp" && uri.host == "reset-password") {
                if (navController.currentDestination?.id != R.id.resetPasswordFragment) {
                    navController.navigate(R.id.resetPasswordFragment)
                }
            }
        }
    }
}
