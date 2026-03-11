package com.arcides.mementoapp.data.repositories

import com.arcides.mementoapp.domain.models.User
import com.arcides.mementoapp.domain.repositories.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    override val currentUser: Flow<User?> = supabaseClient.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                val sbUser = status.session.user
                User(
                    id = sbUser?.id ?: "",
                    email = sbUser?.email ?: "",
                    name = sbUser?.userMetadata?.get("name")?.toString()?.replace("\"", "") ?: "",
                    profilePicture = sbUser?.userMetadata?.get("profilePicture")?.toString()?.replace("\"", "")
                )
            }
            else -> null
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val sbUser = supabaseClient.auth.currentUserOrNull()
            if (sbUser != null) {
                Result.success(User(
                    id = sbUser.id, 
                    email = sbUser.email ?: "", 
                    name = sbUser.userMetadata?.get("name")?.toString()?.replace("\"", "") ?: "",
                    profilePicture = sbUser.userMetadata?.get("profilePicture")?.toString()?.replace("\"", "")
                ))
            } else {
                Result.failure(Exception("Login failed: User is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String): Result<User> {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val sbUser = supabaseClient.auth.currentUserOrNull()
            if (sbUser != null) {
                Result.success(User(
                    id = sbUser.id, 
                    email = sbUser.email ?: "", 
                    name = sbUser.userMetadata?.get("name")?.toString()?.replace("\"", "") ?: ""
                ))
            } else {
                Result.failure(Exception("Registration successful, but user session not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            supabaseClient.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(name: String, profilePicture: String?): Result<User> {
        return try {
            supabaseClient.auth.updateUser {
                data = buildJsonObject {
                    put("name", name)
                    if (profilePicture != null) {
                        put("profilePicture", profilePicture)
                    }
                }
            }

            val sbUser = supabaseClient.auth.currentUserOrNull()
            Result.success(User(
                id = sbUser?.id ?: "",
                email = sbUser?.email ?: "",
                name = name,
                profilePicture = profilePicture
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            supabaseClient.auth.updateUser {
                password = newPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
