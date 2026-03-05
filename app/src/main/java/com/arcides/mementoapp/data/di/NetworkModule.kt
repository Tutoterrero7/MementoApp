package com.arcides.mementoapp.data.di

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://willcdljjjhrzkforrdv.supabase.co",
            supabaseKey = "sb_publishable_r5wejJwwq0i8gjAg7qo7Kg_PaYuF8cT"
        ) {
            install(Postgrest)
            install(Auth)
            install(Realtime)
        }
    }
}
