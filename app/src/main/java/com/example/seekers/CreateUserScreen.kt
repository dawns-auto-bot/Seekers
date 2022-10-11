package com.example.seekers

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.example.seekers.ui.theme.emailAvailable
import com.google.firebase.auth.FirebaseAuth


@Composable
fun CreateUserForm(
    model: AuthenticationViewModel = viewModel(),
    auth: FirebaseAuth,
    navController: NavController
) {
    // Email
    var email by remember { mutableStateOf(TextFieldValue("")) }
    val emailError by model.emailValidationError.observeAsState(null)
    val emailIsAvailable by model.emailIsAvailable.observeAsState(null)

    // Password
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var passwordVisible by remember { mutableStateOf(false) }
    val passwordError by model.passwordValidationError.observeAsState(null)

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val width = LocalConfiguration.current.screenWidthDp * 0.8


    Box(modifier = Modifier.fillMaxSize()) {
        val backBtn = Icons.Filled.ArrowBack
        val desc = "Back Button"
        IconButton(
            onClick = { navController.navigate(NavRoutes.MainScreen.route) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
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
            onValueChange = {
                model.emailIsAvailable.value = null
                email = it
                if (email.text.isNotBlank()) {
                    model.validateEmail(email.text)
                }
            },
            focusManager = focusManager,
            label = "Email",
            placeholder = "Email",
            trailingIcon = {
                if (emailIsAvailable == true && emailError == false) {
                    val image = Icons.Filled.TaskAlt
                    val description = "Email available"
                    Icon(image, description)
                } else {
                    null
                }
            },
            keyboardType = KeyboardType.Email,
            isError = emailError ?: false,
            emailIsAvailable = emailIsAvailable,
            modifier = Modifier
                .width(width.dp)
                .onFocusChanged {
                    if (!it.isFocused && emailError == false && email.text != "") {
                        model.checkEmailAvailability(email.text)
                    }
                }
        )
        if (emailError == true) {
            Card(
                modifier = Modifier
                    .width(width.dp)
                    .padding(5.dp),
                elevation = 0.dp,
                backgroundColor = Color.Transparent
            ) {
                ValidationErrorRow(
                    text = if (emailIsAvailable == false) "Email already in use"
                    else "Please provide a valid email address"
                )
            }
        }
        if (emailIsAvailable == true && emailError == false) {
            Card(
                modifier = Modifier
                    .width(width.dp)
                    .padding(5.dp),
                elevation = 0.dp,
                backgroundColor = Color.Transparent
            ) {
                Text(text = "Available", color = emailAvailable)
            }
        }
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
            isError = passwordError ?: false,
            modifier = Modifier
                .width(width.dp)
        )
        if (passwordError == true) {
            Card(
                shape = RoundedCornerShape(5.dp),
                border = BorderStroke(2.dp, Color.Red),
                modifier = Modifier
                    .padding(5.dp)
                    .width(width.dp),
                backgroundColor = Color.Transparent,
                elevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    ValidationErrorRow(
                        text = "Password requires at least:",
                        fontWeight = FontWeight.Bold
                    )
                    ValidationErrorRow(text = "\u2022 Minimum 8 characters long")
                    ValidationErrorRow(text = "\u2022 1 Capital letter")
                    ValidationErrorRow(text = "\u2022 1 Number")
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            CustomButton(
                onClick = {
                    if (password.text != "" && email.text != "" && passwordError != false) {
                        val validated = model.validatePassword(password.text)
                        if (validated) {
                            Toast.makeText(
                                context,
                                "Validation succeeded",
                                Toast.LENGTH_LONG
                            ).show()
                            auth.createUserWithEmailAndPassword(
                                email.text,
                                password.text
                            )
                                .addOnCompleteListener() {
                                    navController.navigate(NavRoutes.MainScreen.route)
                                }
                        }
                    }
                }, text = "Create account"
            )
        }
    }
}

@Composable
fun ValidationErrorRow(
    text: String,
    fontWeight: FontWeight = FontWeight.Normal,
    fontSize: TextUnit = 12.sp
) {
    Text(
        text = text,
        color = Color.Red,
        fontSize = fontSize,
        fontWeight = fontWeight
    )
}