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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit = {}) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val userRepo = remember { UserRepository() }
    val scope = rememberCoroutineScope()

    fun isValidBirthDate(date: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }

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
            // Thêm logo ở đây
            Image(
                painter = painterResource(id = com.example.busmap.R.drawable.bus_logo),
                contentDescription = "BusMap Logo",
                modifier = Modifier
                    .size(96.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Đăng ký", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Họ tên") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                label = { Text("Ngày sinh (dd/MM/yyyy)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Tên hiển thị") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
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
                    if (fullName.isBlank()) {
                        error = "Họ tên không được để trống"
                        return@Button
                    }
                    if (email.isBlank()) {
                        error = "Email không được để trống"
                        return@Button
                    }
                    if (password.isBlank()) {
                        error = "Mật khẩu không được để trống"
                        return@Button
                    }
                    if (!isValidBirthDate(birthDate)) {
                        error = "Ngày sinh không hợp lệ. Định dạng đúng: dd/MM/yyyy"
                        return@Button
                    }
                    loading = true
                    error = null
                    scope.launch {
                        val result = userRepo.register(email, password, displayName, fullName, birthDate)
                        loading = false
                        if (result.isSuccess) {
                            onRegisterSuccess()
                        } else {
                            error = result.exceptionOrNull()?.message
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Đang đăng ký..." else "Đăng ký")
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}