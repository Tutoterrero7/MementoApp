package com.arcides.mementoapp.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arcides.mementoapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ¡NADA MÁS! El FragmentContainerView maneja la navegación automáticamente
    }
}