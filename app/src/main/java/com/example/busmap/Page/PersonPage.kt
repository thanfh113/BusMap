package com.example.busmap.Page

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.busmap.R
import com.example.busmap.model.User
import com.example.busmap.model.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.navigation.NavController

// Define colors as constants for better consistency
private val primaryColor = Color(0xFF2E8B57)
private val textPrimaryColor = Color.Black
private val textSecondaryColor = Color(0xFF757575)
private val cardBackgroundColor = Color.White
private val iconBackgroundColor = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonPage(navController: NavController) {
    val userRepo = remember { UserRepository() }
    val firebaseUser = userRepo.getCurrentUser()
    var displayName by remember { mutableStateOf(firebaseUser?.displayName ?: "") }
    var email by remember { mutableStateOf(firebaseUser?.email ?: "") }

    // Thêm state để lưu thông tin user từ database
    var userData by remember { mutableStateOf<User?>(null) }
    val scope = rememberCoroutineScope()

    // Lấy dữ liệu user từ database khi vào màn hình
    LaunchedEffect(firebaseUser?.uid) {
        firebaseUser?.uid?.let { uid ->
            val data = userRepo.getUserData(uid)
            userData = data
        }
    }

    // Dialog state
    var showInfoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Lấy NavController thực tế (bắt buộc phải truyền từ NavHost)
    val realNavController = navController

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Tài khoản",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (4).dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        )

        // Sử dụng LazyColumn để cuộn được khi nội dung dài
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Avatar with shadow
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bus_stop_icon),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = userData?.displayName?.ifBlank { null }
                        ?: displayName.ifBlank { "Chưa đặt tên" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = email.ifBlank { "Chưa có email" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondaryColor
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    ProfileMenuItem(
                        icon = R.drawable.user,
                        title = "Thông tin cá nhân",
                        onClick = { showInfoDialog = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileMenuItem(
                        icon = R.drawable.settings,
                        title = "Cài đặt",
                        onClick = { showSettingsDialog = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileMenuItem(
                        icon = R.drawable.database,
                        title = "Cập nhật dữ liệu",
                        onClick = { showUpdateDialog = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileMenuItem(
                        icon = R.drawable.star,
                        title = "Đánh giá ứng dụng",
                        onClick = { showRatingDialog = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Thay "Thông tin công ty" thành "Đăng xuất"
                    ProfileMenuItem(
                        icon = R.drawable.remove_32, // Bạn cần thêm icon logout vào drawable
                        title = "Đăng xuất",
                        onClick = { showLogoutDialog = true }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Thông tin cá nhân", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Họ tên: ${userData?.fullName.orEmpty().ifBlank { "Chưa cập nhật" }}")
                    Text("Ngày sinh: ${userData?.birthDate.orEmpty().ifBlank { "Chưa cập nhật" }}")
                    Text("Tên hiển thị: ${userData?.displayName.orEmpty().ifBlank { displayName.ifBlank { "Chưa đặt tên" } }}")
                    Text("Email: ${userData?.email.orEmpty().ifBlank { email.ifBlank { "Chưa có email" } }}")
                    Text("UID: ${firebaseUser?.uid ?: "Không có"}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Đóng") }
            }
        )
    }
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Cài đặt", fontWeight = FontWeight.Bold) },
            text = { Text("Chức năng này sẽ sớm được cập nhật.") },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Đóng") }
            }
        )
    }
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Cập nhật dữ liệu", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn muốn cập nhật dữ liệu mới nhất?") },
            confirmButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Đồng ý") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Hủy") }
            }
        )
    }
    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            title = { Text("Đánh giá ứng dụng", fontWeight = FontWeight.Bold) },
            text = { Text("Cảm ơn bạn đã sử dụng và đánh giá ứng dụng!") },
            confirmButton = {
                TextButton(onClick = { showRatingDialog = false }) { Text("Đóng") }
            }
        )
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Đăng xuất", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn đăng xuất?") },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    showLogoutDialog = false
                    // Điều hướng về màn hình đăng nhập, xóa sạch toàn bộ backstack
                    realNavController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }) { Text("Đăng xuất") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun ProfileMenuItem(icon: Int, title: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp)
        ) {
            // Icon with colored background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Menu item text
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimaryColor,
                modifier = Modifier.weight(1f)
            )

            // Navigation arrow
            Icon(
                painter = painterResource(id = R.drawable.next),
                contentDescription = null,
                tint = textSecondaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
