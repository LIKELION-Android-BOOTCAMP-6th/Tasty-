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

    // 현재 로그인 상태 여부
    val isLoggedIn: Boolean get() = auth.currentUser?.uid != null

    // 현재 로그인된 유저 객체 전체 가져오기
    fun getCurrentUser() = auth.currentUser

    // 회원가입 / 반환: uid
    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!.uid)
        } catch (e: FirebaseAuthException) {
            Result.failure(e)
        }
    }


    // 로그인 / 반환: uid
    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!.uid)
        } catch (e: FirebaseAuthException) {
            Result.failure(e)
        }
    }

    // 로그아웃
    fun logOut() = auth.signOut()


}