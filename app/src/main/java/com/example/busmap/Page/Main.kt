package com.example.busmap.Page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.busmap.Page.FavoritePage
import com.example.busmap.Page.HomePage

// Define colors as constants for better consistency
private val activeColor = Color(0xFF4CAF50)
private val inactiveColor = Color(0xFF757575)
private val backgroundColor = Color.White

// Define tab data class for cleaner implementation
private data class TabItem(
    val title: String,
    val icon: ImageVector,
    val content: @Composable (NavController) -> Unit
)

@Composable
fun Main(navController: NavController) {
    // Define tab items
    val tabs = listOf(
        TabItem("Trang chủ", Icons.Default.Home) { HomePage(navController) },
        TabItem("Yêu thích", Icons.Default.Favorite) { FavoritePage(navController) },
        TabItem("Cá nhân", Icons.Default.Person) { PersonPage(navController) } // Sửa dòng này
    )

    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Content area
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            tabs[selectedTabIndex].content(navController)
        }

        // Bottom navigation bar
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = backgroundColor,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                        .height(0.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = { Spacer(modifier = Modifier.height(0.dp)) }
        ) {
            tabs.forEachIndexed { index, tabItem ->
                CustomTab(
                    title = tabItem.title,
                    icon = tabItem.icon,
                    selected = selectedTabIndex == index,
                    onSelected = { selectedTabIndex = index }
                )
            }
        }
    }
}

@Composable
private fun CustomTab(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Tab(
        selected = selected,
        onClick = onSelected,
        modifier = Modifier.padding(vertical = 8.dp),
        selectedContentColor = activeColor,
        unselectedContentColor = inactiveColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) activeColor else inactiveColor,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                color = if (selected) activeColor else inactiveColor
            )
        }
    }
}
