package com.arcides.mementoapp.presentation

import android.content.Intent
import android.os.Bundle
import android.view.View
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "mementoapp" && uri.host == "reset-password") {
                if (navController.currentDestination?.id != R.id.resetPasswordFragment) {
                    navController.navigate(R.id.resetPasswordFragment)
                }
            }
        }
    }
}
