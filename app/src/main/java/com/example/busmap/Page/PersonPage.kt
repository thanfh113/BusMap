package com.example.busmap.Page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

// Define colors as constants for better consistency
private val primaryColor = Color(0xFF2E8B57)
private val textPrimaryColor = Color.Black
private val textSecondaryColor = Color(0xFF757575)
private val cardBackgroundColor = Color.White
private val iconBackgroundColor = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonPage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top app bar with gradient background
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Tài khoản",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (4).dp)  // Adjusted position to be closer to avatar
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)  // Reduced height to bring text closer to avatar
        )

        // Content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(70.dp))
            
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
                    painter = painterResource(id = R.drawable.thanhimg),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            // User information
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hà Tiến Thành",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "CT060138",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondaryColor
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Menu Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                ProfileMenuItem(
                    icon = R.drawable.user,
                    title = "Thông tin cá nhân",
                    onClick = { /* TODO: Handle click */ }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ProfileMenuItem(
                    icon = R.drawable.settings,
                    title = "Cài đặt",
                    onClick = { /* TODO: Handle click */ }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ProfileMenuItem(
                    icon = R.drawable.database,
                    title = "Cập nhật dữ liệu",
                    onClick = { /* TODO: Handle click */ }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ProfileMenuItem(
                    icon = R.drawable.star,
                    title = "Đánh giá ứng dụng",
                    onClick = { /* TODO: Handle click */ }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ProfileMenuItem(
                    icon = R.drawable.info,
                    title = "Thông tin công ty",
                    onClick = { /* TODO: Handle click */ }
                )
            }
        }
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

@Preview(showBackground = true)
@Composable
fun PersonPagePreview() {
    PersonPage()
}