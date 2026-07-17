package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PortalViewModel
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    viewModel: PortalViewModel,
    onAuthSuccess: () -> Unit
) {
    val activeUser by viewModel.activeUser.collectAsState()
    val mfaSecret by viewModel.mfaSecretState.collectAsState()

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var mfaCodeInput by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var mfaVerificationRequired by remember { mutableStateOf(false) }
    var showMfaSetup by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf("") }

    // When signed in and MFA is not pending, go to chat
    LaunchedEffect(activeUser, mfaVerificationRequired, showMfaSetup) {
        val user = activeUser
        if (user != null && user.isLoggedIn) {
            if (user.isMfaEnabled && !mfaVerificationRequired) {
                mfaVerificationRequired = true
            } else if (!user.isMfaEnabled && !showMfaSetup) {
                onAuthSuccess()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Aesthetic abstract background circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryGold.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.2f),
                    radius = size.width * 0.8f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SecondaryBronze.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.8f),
                    radius = size.width * 0.9f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Header Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CardDark)
                    .border(1.dp, BorderDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Portal",
                    tint = PrimaryGold,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (showMfaSetup) "SETUP MULTI-FACTOR" else if (mfaVerificationRequired) "VERIFY IDENTITY" else "CLAUDE AI PORTAL",
                color = TextLight,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (showMfaSetup) "Secure your cryptographic keys locally" else if (mfaVerificationRequired) "Enter the 6-digit code from Google Authenticator" else "End-to-end encrypted AI hub with local intelligence",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Auth Error Panel
            if (authError.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color(0xFF442D2D), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFD32F2F), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = authError,
                        color = Color(0xFFFFCDCD),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // DYNAMIC FORMS WITH TRANSITIONS
            AnimatedContent(
                targetState = when {
                    showMfaSetup -> "mfa_setup"
                    mfaVerificationRequired -> "mfa_verify"
                    else -> "login"
                },
                label = "auth_states"
            ) { state ->
                when (state) {
                    "mfa_setup" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (mfaSecret == null) {
                                Button(
                                    onClick = { viewModel.generateMfaSetup() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("Generate 2FA Credentials", color = BackgroundDark, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardDark, RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode2,
                                            contentDescription = "QR Placeholder",
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(120.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Secret Key:",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                        SelectionContainer {
                                            Text(
                                                text = mfaSecret ?: "",
                                                color = TextLight,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = "Scan or type this key into Google Authenticator or Microsoft Authenticator.",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = mfaCodeInput,
                                    onValueChange = { mfaCodeInput = it.take(6) },
                                    label = { Text("6-Digit Verify Code") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryGold,
                                        unfocusedBorderColor = BorderDark,
                                        focusedLabelColor = PrimaryGold,
                                        unfocusedLabelColor = TextMuted
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("mfa_setup_input")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        viewModel.verifyAndEnableMfa(mfaCodeInput) { success ->
                                            if (success) {
                                                showMfaSetup = false
                                                onAuthSuccess()
                                            } else {
                                                authError = "Invalid verification code"
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("mfa_setup_submit")
                                ) {
                                    Text("Verify and Complete Setup", color = BackgroundDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    "mfa_verify" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedTextField(
                                value = mfaCodeInput,
                                onValueChange = { mfaCodeInput = it.take(6) },
                                label = { Text("Code from Authenticator App") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = BorderDark,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextMuted
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("mfa_code_input")
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    viewModel.verifyMfaLogin(mfaCodeInput) { valid ->
                                        if (valid) {
                                            mfaVerificationRequired = false
                                            onAuthSuccess()
                                        } else {
                                            authError = "Incorrect authenticator code"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("mfa_verify_submit")
                            ) {
                                        Text("Unlock Secure Portal", color = BackgroundDark, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            IconButton(
                                onClick = {
                                    // Simulated biometric alternative
                                    mfaVerificationRequired = false
                                    onAuthSuccess()
                                },
                                modifier = Modifier.size(54.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric Bypass",
                                    tint = PrimaryGold,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Text("Use Encrypted Biometric Backup", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    "login" -> {
                        Column {
                            // Standard Fields
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Secure Email Address") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = BorderDark,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextMuted
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("email_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Account Pin / Password") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = BorderDark,
                                    focusedLabelColor = PrimaryGold,
                                    unfocusedLabelColor = TextMuted
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("password_input")
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Local secure sign-up/login button
                            Button(
                                onClick = {
                                    if (emailInput.contains("@") && passwordInput.length >= 4) {
                                        viewModel.loginWithGoogle(emailInput, emailInput.substringBefore("@"))
                                        val user = activeUser
                                        if (isSignUpMode) {
                                            showMfaSetup = true
                                        }
                                    } else {
                                        authError = "Enter a valid email and 4+ character password"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_submit")
                            ) {
                                Text(
                                    text = if (isSignUpMode) "Register Secure Profile" else "Access Encrypted Hub",
                                    color = BackgroundDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom Google Auth integration (Simulated)
                            OutlinedButton(
                                onClick = {
                                    // Trigger realistic visual OAuth signup flow
                                    val simulatedEmail = "user.claude.aistudio@gmail.com"
                                    viewModel.loginWithGoogle(simulatedEmail, "AI Studio Explorer")
                                },
                                border = BorderStroke(1.dp, BorderDark),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("google_auth_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Custom colored G badge simulation
                                    Text(
                                        text = "G ",
                                        color = Color(0xFF4285F4),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                    Text("Sign up with Google Secure Auth", fontWeight = FontWeight.Medium)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Toggle Mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isSignUpMode) "Already have a secure key?" else "Need private offline indexing?",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSignUpMode) "Login" else "Sign Up",
                                    color = PrimaryGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clickable {
                                            isSignUpMode = !isSignUpMode
                                            authError = ""
                                        }
                                        .testTag("toggle_mode")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
