package com.example.seekers

import android.content.*
import android.content.ContentValues.TAG
import android.os.Build

import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
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
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.seekers.general.CustomButton
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var sharedVM: SharedViewModel

    private val REQ_ONE_TAP = 2  // Can be any integer unique to the Activity

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedVM = ViewModelProvider(this)[SharedViewModel::class.java]


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
                e.localizedMessage?.let { Log.d(TAG, it) }
            }

        FirebaseApp.initializeApp(this)

        setContent {
            SeekersTheme {
                MyAppNavHost(startLocService = { }, sharedVM = sharedVM)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MyAppNavHost(startLocService: () -> Unit, sharedVM: SharedViewModel) {

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
//            startLocService()
            LobbyQRScreen(
                navController = navController,
                gameId = gameId,
                startLocService = startLocService,
                sharedVM = sharedVM
            )
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
        composable(
            NavRoutes.Countdown.route + "/{gameId}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
            )
        ) {
            val gameId = it.arguments!!.getString("gameId")!!
            CountdownScreen(gameId = gameId, navController = navController)
        }

        //Heatmap
        composable(
            NavRoutes.Heatmap.route + "/{gameId}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
            )
        ) {
            val gameId = it.arguments!!.getString("gameId")!!
            HeatMapScreen(mapControl = true, navController = navController, gameId = gameId)
        }
    }


}

@Composable
fun MainScreen(navController: NavController) {
    val auth = Firebase.auth
    val vm = AuthenticationViewModel(auth)
    vm.initializeUser()
    val token = stringResource(R.string.default_web_client_id)
    val context = LocalContext.current
    val loggedInUser: FirebaseUser? by vm.user.observeAsState(null)
    val gameStatus by vm.gameStatus.observeAsState()
    val gameId by vm.currentGameId.observeAsState()
    val userIsInUsers by vm.userIsInUsers.observeAsState()
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        launch(Dispatchers.Default) {
            delay(1000)
            loading = false
        }
    }

    LaunchedEffect(loggedInUser) {
        loggedInUser?.let {
            vm.checkUserInUsers(it.uid)
        }
    }

    LaunchedEffect(userIsInUsers) {
        userIsInUsers?.let {
            if (it) {
                vm.checkCurrentGame(loggedInUser!!.uid)
            } else {
                val changeMap = mapOf(
                    Pair("currentGameId", "")
                )
                vm.updateUserDoc(loggedInUser!!.uid, changeMap)
                navController.navigate(NavRoutes.StartGame.route)
            }
        }
    }

    LaunchedEffect(gameId) {
        gameId?.let {
            if (it.isBlank()) {
                navController.navigate(NavRoutes.StartGame.route)
            } else {
                vm.checkGameStatus(it)
            }
        }
    }

    LaunchedEffect(gameStatus) {
        gameStatus?.let {
            println("gameId $gameId")
            gameId ?: return@LaunchedEffect
            println("gameStatus $it")
            when (it) {
                LobbyStatus.CREATED.value -> {
                    navController.navigate(NavRoutes.LobbyQR.route + "/$gameId")
                }
                LobbyStatus.COUNTDOWN.value -> {
                    navController.navigate(NavRoutes.Countdown.route + "/$gameId")
                }
                LobbyStatus.ACTIVE.value -> {
                    navController.navigate(NavRoutes.Heatmap.route + "/$gameId")
                }
            }
        }
    }

    val launcher = googleRememberFirebaseAuthLauncher(
        onAuthComplete = {
            vm.setUser(it.user)
            Log.d("authenticated", "MainScreen: ${auth.currentUser}")
        },
        onAuthError = {
            vm.setUser(null)
            Log.d("authenticated", "MainScreen: ${it.message}")
        }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight()
    ) {

        if (loggedInUser == null && !loading) {
            CreateUserForm(
                model = vm,
                auth = auth,
                navController = navController
            )
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

        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    strokeWidth = 5.dp,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .size(100.dp)
                )
            }
        }
    }
}

@Composable
fun CreateUserForm(
    model: AuthenticationViewModel = viewModel(),
    auth: FirebaseAuth,
    navController: NavController
) {
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
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                CustomButton(
                    onClick = {
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
                                }
                        }
                    }, text = "Create an account"
                )
            }
        }
    }
}

class AuthenticationViewModel(auth: FirebaseAuth) : ViewModel() {

    var fireBaseAuth = auth
    var user = MutableLiveData<FirebaseUser>(null)
    var userIsInUsers = MutableLiveData<Boolean>()
    var firestore = FirestoreHelper
    val currentGameId = MutableLiveData<String>()
    val gameStatus = MutableLiveData<Int>()

    fun updateUserDoc(userId: String, changeMap: Map<String, Any>) =
        firestore.updateUser(userId, changeMap)

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

    fun checkUserInUsers(userId: String) {
        firestore.usersRef.get().addOnSuccessListener {
            val userList = it.documents.map { docs ->
                docs.id
            }
            userIsInUsers.postValue(userList.contains(userId))
        }
    }

    fun checkCurrentGame(playerId: String) {
        firestore.getUser(playerId).get()
            .addOnFailureListener {
                Log.e(TAG, "checkCurrentGame: ", it)
            }
            .addOnSuccessListener {
                val gameId = it.getString("currentGameId")
                gameId?.let { id ->
                    currentGameId.postValue(id)
                }
            }
    }

    fun checkGameStatus(gameId: String) {
        firestore.getLobby(gameId).get()
            .addOnSuccessListener {
                val lobby = it.toObject(Lobby::class.java)
                lobby?.let { lobby ->
                    println("checkGameStatus " + lobby.status.toString())
                    gameStatus.postValue(lobby.status)
                }
            }
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
