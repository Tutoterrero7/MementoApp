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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navegar al Login después de un breve delay
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded && findNavController().currentDestination?.id == R.id.splashFragment) {
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
            }
        }, 1500)
    }
}