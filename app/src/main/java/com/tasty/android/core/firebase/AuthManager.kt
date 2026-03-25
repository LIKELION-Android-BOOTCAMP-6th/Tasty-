package com.tasty.android.core.firebase

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import kotlin.math.log

class AuthManager {
    // FirebaseAuth 인스턴스 생성
    private val auth = Firebase.auth


    // 현재 유저 아이디 (없을 경우 null 반환)
    val currentUserId : String? = auth.currentUser?.uid

    // 현재 로그인 상태 여부
    val isLoggedIn: Boolean = currentUserId != null

    // 회원가입
    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Log.d("jjam", result.user!!.uid)
            Result.success(result.user!!.uid)
        } catch (e: FirebaseException) {
            Log.d("jjam", e.toString())
            Result.failure(e)
        }
    }

    // 로그인
    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Log.d("jjam", result.user!!.uid)
            Result.success(result.user!!.uid)
        } catch (e: FirebaseAuthException) {
            Log.d("jjam", e.toString())
            Result.failure(e)
        }
    }

    // 로그아웃
    fun logOut() = auth.signOut()


}