package com.example.busmap.Page

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.busmap.model.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit = {}, onNavigateRegister: () -> Unit = {}) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var resetSent by remember { mutableStateOf(false) }
    val userRepo = remember { UserRepository() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = com.example.busmap.R.drawable.bus_logo),
                contentDescription = "BusMap Logo",
                modifier = Modifier
                    .size(96.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Đăng nhập", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        val result = userRepo.login(email, password)
                        loading = false
                        if (result.isSuccess) {
                            onLoginSuccess()
                        } else {
                            // Hiển thị thông báo tiếng Việt khi đăng nhập sai
                            error = "Email hoặc mật khẩu không đúng"
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Đang đăng nhập..." else "Đăng nhập")
            }
            TextButton(onClick = onNavigateRegister, modifier = Modifier.fillMaxWidth()) {
                Text("Chưa có tài khoản? Đăng ký")
            }
            TextButton(
                onClick = {
                    if (email.isNotBlank()) {
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                resetSent = task.isSuccessful
                                if (!task.isSuccessful) {
                                    error = task.exception?.message
                                }
                            }
                    } else {
                        error = "Vui lòng nhập email để đặt lại mật khẩu."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Quên mật khẩu?")
            }
            if (resetSent) {
                Text(
                    "Đã gửi email đặt lại mật khẩu, vui lòng kiểm tra hộp thư.",
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}