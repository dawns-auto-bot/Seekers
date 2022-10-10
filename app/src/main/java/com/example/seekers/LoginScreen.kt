package com.example.seekers

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.seekers.general.CustomButton
import com.example.seekers.general.CustomOutlinedTextField
import com.example.seekers.general.isEmailValid
import com.example.seekers.general.isPasswordValid
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@Composable
fun LoginForm(
    model: AuthenticationViewModel = viewModel(),
    navController: NavController,
    token: String,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    // Email
    var email by remember { mutableStateOf(TextFieldValue("")) }

    // Password
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var passwordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val width = LocalConfiguration.current.screenWidthDp * 0.8

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(30.dp),
    ) {
        Text(text = "Welcome Back", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(text = "Sign in to continue", fontSize = 16.sp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(40.dp))
        CustomOutlinedTextField(
            value = email,
            onValueChange = { email = it },
            focusManager = focusManager,
            label = "Email",
            placeholder = "Email",
            keyboardType = KeyboardType.Email,
            modifier = Modifier.width(width.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        CustomOutlinedTextField(
            value = password,
            onValueChange = { password = it },
            focusManager = focusManager,
            label = "Password",
            placeholder = "Password",
            trailingIcon = {
                val image = if (passwordVisible) {
                    Icons.Filled.Visibility
                } else {
                    Icons.Filled.VisibilityOff
                }
                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, description)
                }
            },
            keyboardType = KeyboardType.Password,
            passwordVisible = passwordVisible,
            modifier = Modifier.width(width.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            CustomButton(
                onClick = {
                    if (email.text == "" || password.text == "") {
                        Toast.makeText(
                            context,
                            "Please give an email and password!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        model.fireBaseAuth.signInWithEmailAndPassword(
                            email.text,
                            password.text
                        )
                            .addOnCompleteListener() {
                                println("logged in as: ${model.fireBaseAuth.currentUser}")
                                model.setUser(model.fireBaseAuth.currentUser)
                                navController.navigate(NavRoutes.StartGame.route)
                            }
                    }
                }, text = "Login"
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
        Text(text = "Or", fontSize = 16.sp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(40.dp))
        GoogleButton(token = token, context = context, launcher = launcher)
        Spacer(modifier = Modifier.height(30.dp))
        Row() {
            Text(text = "Don't have an account?", fontSize = 12.sp)
            Text(
                text = " Create one now",
                fontSize = 12.sp,
                color = Color.Blue,
                modifier = Modifier
                    .clickable {
                        navController.navigate(NavRoutes.CreateAccount.route)
                    }
            )
        }
    }
}

@Composable
fun GoogleButton(
    token: String,
    context: Context,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val width = LocalConfiguration.current.screenWidthDp * 0.8
    Button(
        modifier = Modifier.width(width.dp),
        onClick = {
            val gso =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(token)
                    .requestEmail()
                    .build()
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            launcher.launch(googleSignInClient.signInIntent)
        },
        colors = ButtonDefaults.buttonColors(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.google_logo),
            contentDescription = "Google logo"
        )
        Text(
            "Sign in with Google", fontSize = 17.sp
        )
    }
}