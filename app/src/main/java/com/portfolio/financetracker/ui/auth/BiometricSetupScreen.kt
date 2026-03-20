package com.portfolio.financetracker.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BiometricSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: BiometricViewModel = hiltViewModel()
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(100.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Enable Biometric Security",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Secure your financial data with Fingerprint or Face Authentication. You can change this later in settings.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = { 
                    viewModel.setBiometricEnabled(true)
                    onSetupComplete()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Biometric Lock")
            }
            
            TextButton(
                onClick = { 
                    viewModel.skipFirstTimeSetup()
                    onSetupComplete()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for Now", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
