package com.portfolio.financetracker.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.financetracker.domain.model.UserProfile

@Composable
fun AppDrawer(
    userProfile: UserProfile?,
    currentRoute: String?,
    onNavigateTo: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier) {

        // ── Profile header ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(24.dp)
        ) {
            Column {
                // Avatar circle with initials
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userProfile?.username
                            ?.firstOrNull()
                            ?.uppercaseChar()
                            ?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = userProfile?.username ?: "Guest",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = userProfile?.email ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Nav items ─────────────────────────────────────────────────────────
        DrawerItem(
            icon = Icons.Default.Dashboard,
            label = "Dashboard",
            selected = currentRoute == Screen.DashboardScreen.route,
            onClick = { onNavigateTo(Screen.DashboardScreen.route) }
        )
        DrawerItem(
            icon = Icons.Default.CalendarMonth,
            label = "Calendar & Reminders",
            selected = currentRoute == Screen.CalendarScreen.route,
            onClick = { onNavigateTo(Screen.CalendarScreen.route) }
        )
        DrawerItem(
            icon = Icons.Default.BarChart,
            label = "Charts",
            selected = currentRoute == Screen.ChartsScreen.route,
            onClick = { onNavigateTo(Screen.ChartsScreen.route) }
        )
        DrawerItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            selected = currentRoute == Screen.SettingsScreen.route,
            onClick = { onNavigateTo(Screen.SettingsScreen.route) }
        )
        DrawerItem(
            icon = Icons.Default.Info,
            label = "About Us",
            selected = currentRoute == Screen.AboutUsScreen.route,
            onClick = { onNavigateTo(Screen.AboutUsScreen.route) }
        )
        DrawerItem(
            icon = Icons.Default.Sms,
            label = "SMS Review",
            selected = currentRoute == Screen.PendingReviewScreen.route,
            onClick = { onNavigateTo(Screen.PendingReviewScreen.route) }
        )

        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider()

        // ── Sign out ──────────────────────────────────────────────────────────
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Logout, contentDescription = "Sign Out") },
            label = {
                Text(
                    "Sign Out",
                    color = MaterialTheme.colorScheme.error
                )
            },
            selected = false,
            onClick = onSignOut,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}
