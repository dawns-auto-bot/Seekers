package com.example.seekers

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seekers.ui.theme.SeekersTheme
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.seekers.general.getLocationPermission
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.example.seekers.general.CustomButton

class MainActivity : ComponentActivity() {

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    private val REQ_ONE_TAP = 2  // Can be any integer unique to the Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Google
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(
                BeginSignInRequest.PasswordRequestOptions.builder()
                    .setSupported(true)
                    .build()
            )
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.your_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build()

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0, null
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                Log.d(TAG, e.localizedMessage)
            }


        FirebaseApp.initializeApp(this)
        getLocationPermission(this)

        setContent {
            SeekersTheme {
                MyAppNavHost()
            }
        }
    }
}

@Composable
fun MyAppNavHost() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.MainScreen.route
    ) {
        composable(NavRoutes.MainScreen.route) {
            MainScreen(navController)
        }
        composable(NavRoutes.StartGame.route) {
            StartGameScreen(navController)
        }
        // Avatar picker screen
        composable(
            NavRoutes.AvatarPicker.route + "/{isCreator}",
            arguments = listOf(
                navArgument("isCreator") { type = NavType.BoolType }
            )
        ) {
            val isCreator = it.arguments!!.getBoolean("isCreator")
            AvatarPickerScreen(navController = navController, isCreator = isCreator)
        }
        composable(NavRoutes.LobbyCreation.route + "/{nickname}/{avatarId}",
            arguments = listOf(
                navArgument("nickname") {
                    type = NavType.StringType
                },
                navArgument("avatarId") {
                    type = NavType.IntType
                }
            )) {
            val nickname = it.arguments!!.getString("nickname")!!
            val avatarId = it.arguments!!.getInt("avatarId")
            LobbyCreationScreen(
                navController = navController,
                nickname = nickname,
                avatarId = avatarId
            )
        }

        //Lobby screen with QR
        composable(
            NavRoutes.LobbyQR.route + "/{gameId}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
            )
        ) {
            val gameId = it.arguments!!.getString("gameId")!!
            LobbyQRScreen(navController = navController, gameId = gameId)
        }
        //QR Scanner
        composable(NavRoutes.Scanner.route + "/{nickname}/{avatarId}",
            arguments = listOf(
                navArgument("nickname") {
                    type = NavType.StringType
                },
                navArgument("avatarId") {
                    type = NavType.IntType
                }
            )
        ) {
            val nickname = it.arguments!!.getString("nickname")!!
            val avatarId = it.arguments!!.getInt("avatarId")
            QrScannerScreen(navController, nickname = nickname, avatarId = avatarId)
        }

        //Countdown
        composable(NavRoutes.Countdown.route + "/{seconds}",
            arguments = listOf(
                navArgument("seconds") { type = NavType.IntType }
            )) {
            val seconds = it.arguments!!.getInt("seconds")
            CountdownScreen(seconds = seconds, navController = navController)
        }
    }
}

@Composable
fun LoginBtn(navController: NavController) {
    Button(onClick = {
        navController.navigate(NavRoutes.StartGame.route)
    }) {
        Text(text = "fake login button")
    }
}

@Composable
fun MainScreen(navController: NavController) {
    val auth = Firebase.auth
    val authenticationViewModel = AuthenticationViewModel(auth)
    val token = stringResource(R.string.default_web_client_id)
    val context = LocalContext.current
    val loggedInUser: FirebaseUser? by authenticationViewModel.user.observeAsState(null)

    val launcher = googleRememberFirebaseAuthLauncher(
        onAuthComplete = {
            authenticationViewModel.setUser(it.user)
            navController.navigate(NavRoutes.StartGame.route)
        },
        onAuthError = {
            authenticationViewModel.setUser(null)
        }
    )

    Column (horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight()
    ) {
        if (loggedInUser == null) {
            CreateUserForm(model = authenticationViewModel, auth = auth, navController = navController)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
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
                    "Sign in with google", fontSize = 17.sp
                )
            }

        } else{
            Button(onClick = {
                authenticationViewModel.logOut()
            }) {
                Text("Sign out")
            }
            Button(onClick = {
                navController.navigate(NavRoutes.StartGame.route)
            }) {
                Text("Start game")
            }
        }
    }
}

@Composable
fun CreateUserForm(
    model: AuthenticationViewModel = viewModel(),
    auth: FirebaseAuth,
    navController: NavController
){

    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    Card(
        modifier = Modifier
            .padding(horizontal = 30.dp)
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(30.dp),
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }),
                label = { Text(text = "Email") },
                placeholder = { Text(text = "Email") },
                //modifier = Modifier.weight(0.5F)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }),
                label = { Text(text = "Password") },
                placeholder = { Text(text = "Password") },
                //modifier = Modifier.weight(0.5F)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier=Modifier.fillMaxWidth()){
                CustomButton(onClick = {
                if (email.text == "" || password.text == "") {
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            "Please give an email and a password",
                            "!",
                            SnackbarDuration.Short,
                        )
                    }
                } else {
                    auth.createUserWithEmailAndPassword(
                        email.text,
                        password.text
                    )
                        .addOnCompleteListener() {
                            model.setUser(auth.currentUser)
                            navController.navigate(NavRoutes.StartGame.route)
                        }
                }
            }
                    , text = "Create an account"
            )
        }}
    }
}

@Composable
fun GoogleButton(){

}

class AuthenticationViewModel(auth: FirebaseAuth) : ViewModel() {

    var fireBaseAuth = auth
    var user = MutableLiveData<FirebaseUser>(null)

    fun initializeUser() {
        user.value = fireBaseAuth.currentUser
    }

    fun setUser(firebaseUser: FirebaseUser?) {
        user.value = firebaseUser
    }

    fun logOut() {
        fireBaseAuth.signOut()
        user.value = null
    }

}

@Composable
fun googleRememberFirebaseAuthLauncher(
    onAuthComplete: (AuthResult) -> Unit,
    onAuthError: (ApiException) -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val scope = rememberCoroutineScope()
    return rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            scope.launch {
                val authResult = async { Firebase.auth.signInWithCredential(credential) }
                delay(2000)
                onAuthComplete(authResult.await().result)
            }
        } catch (e: ApiException) {
            onAuthError(e)
            Log.d("onAuthError", e.toString())
        }
    }
}
