package com.example.seekers

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.seekers.general.CustomButton
import com.example.seekers.general.CustomOutlinedTextField
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


@Composable
fun CreateUserForm(
    auth: FirebaseAuth,
    navController: NavController
) {
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        val backBtn = Icons.Filled.ArrowBack
        val desc = "Back Button"
        IconButton(
            onClick = { navController.navigate(NavRoutes.MainScreen.route) },
            modifier = Modifier.align(Alignment.TopStart).padding(20.dp)
        ) {
            Icon(imageVector = backBtn, desc, modifier = Modifier.size(32.dp))
        }
    }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(30.dp)
            .fillMaxSize(),
    ) {
        Text(text = "Create Account", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(text = "Create a new account", fontSize = 16.sp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(40.dp))
        CustomOutlinedTextField(
            value = email,
            onValueChange = { email = it },
            focusManager = focusManager,
            label = "Email",
            placeholder = "Email",
            keyboardType = KeyboardType.Email,
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
            passwordVisible = passwordVisible
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
                        auth.createUserWithEmailAndPassword(
                            email.text,
                            password.text
                        )
                            .addOnCompleteListener() {
                                navController.navigate(NavRoutes.MainScreen.route)
                            }
                    }
                }, text = "Create account"
            )
        }
    }
}