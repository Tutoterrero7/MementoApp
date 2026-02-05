package com.arcides.mementoapp.presentation

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.arcides.mementoapp.R
import com.arcides.mementoapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        // Vinculamos la nueva NavigationBarView con el NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Gestión de visibilidad (Ocultar en pantallas de inicio/login)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.splashFragment, R.id.loginFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                    binding.fabAdd.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.fabAdd.visibility = View.VISIBLE
                }
            }
        }

        // Acción del botón central desacoplada mediante interfaz
        binding.fabAdd.setOnClickListener {
            val navHost = supportFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment
            val currentFragment = navHost?.childFragmentManager?.fragments?.firstOrNull()

            // Si el fragmento actual sabe manejar la acción global, se la delegamos
            (currentFragment as? GlobalActionProvider)?.onPrimaryActionClicked()
        }
    }
}
