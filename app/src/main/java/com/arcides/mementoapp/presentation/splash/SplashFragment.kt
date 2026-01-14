package com.arcides.mementoapp.presentation.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.arcides.mementoapp.R

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Redirigir después de 2 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            // Aquí decidiremos: si usuario está logueado → Home, sino → Login
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }, 2000)
    }
}