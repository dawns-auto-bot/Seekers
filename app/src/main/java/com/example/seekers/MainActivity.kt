package com.example.seekers

import android.content.*
import android.content.ContentValues.TAG

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import com.example.seekers.ui.theme.SeekersTheme
import androidx.compose.runtime.livedata.observeAsState
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
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seekers.general.isEmailValid
import com.example.seekers.general.isPasswordValid
import kotlinx.coroutines.Dispatchers

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
                e.localizedMessage?.let { Log.d(TAG, it) }
            }

        FirebaseApp.initializeApp(this)

        setContent {
            SeekersTheme {
                MyAppNavHost()
            }
        }
    }
}

@Composable
fun MyAppNavHost(permissionVM: PermissionsViewModel = viewModel()) {

    val navController = rememberNavController()
    val auth = Firebase.auth
    val showPermissionDialog by permissionVM.showDialog.observeAsState(false)

    NavHost(
        navController = navController,
        startDestination = NavRoutes.MainScreen.route
    ) {
        // Login or Sign up
        composable(NavRoutes.MainScreen.route) {
            MainScreen(navController = navController)
        }

        // Create Account screen
        composable(NavRoutes.CreateAccount.route) {
            CreateUserForm(auth = auth, navController = navController)
        }

        // Create lobby or join game screen
        composable(NavRoutes.StartGame.route) {
            StartGameScreen(navController, permissionVM = permissionVM)
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

        //Lobby rules screen
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
                avatarId = avatarId,
                permissionVM = permissionVM
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
            LobbyQRScreen(
                navController = navController,
                gameId = gameId,
                permissionVM = permissionVM
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
            QrScannerScreen(
                navController,
                nickname = nickname,
                avatarId = avatarId,
                permissionVM = permissionVM
            )
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
            HeatMapScreen(
                mapControl = true,
                navController = navController,
                gameId = gameId,
                permissionVM = permissionVM
            )
        }
    }
    if (showPermissionDialog) {
        PermissionsDialog(onDismiss = { permissionVM.updateShowDialog(false) }, vm = permissionVM)
    }
}

@Composable
fun MainScreen(vm: AuthenticationViewModel = viewModel(), navController: NavController) {
    val token = stringResource(R.string.default_web_client_id)
    val loggedInUser: FirebaseUser? by vm.user.observeAsState(null)
    val gameStatus by vm.gameStatus.observeAsState()
    val gameId by vm.currentGameId.observeAsState()
    val userIsInUsers by vm.userIsInUsers.observeAsState()
    var loading by remember { mutableStateOf(true) }

    val launcher = googleRememberFirebaseAuthLauncher(
        onAuthComplete = {
            vm.setUser(it.user)
            Log.d("authenticated", "MainScreen: ${vm.fireBaseAuth.currentUser}")
        },
        onAuthError = {
            vm.setUser(null)
            Log.d("authenticated", "MainScreen: ${it.message}")
        }
    )

    LaunchedEffect(Unit) {
        vm.setUser(vm.fireBaseAuth.currentUser)
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
            println("inuser: $it")
            if (it) {
                vm.checkCurrentGame(loggedInUser!!.uid)
            } else {
                val changeMap = mapOf(
                    Pair("currentGameId", ""),
                    Pair("email", loggedInUser!!.email!!)
                )
                vm.addUserDoc(loggedInUser!!.uid, changeMap)
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
            gameId ?: return@LaunchedEffect
            when (it) {
                LobbyStatus.CREATED.ordinal -> {
                    navController.navigate(NavRoutes.LobbyQR.route + "/$gameId")
                }
                LobbyStatus.COUNTDOWN.ordinal -> {
                    navController.navigate(NavRoutes.Countdown.route + "/$gameId")
                }
                LobbyStatus.ACTIVE.ordinal -> {
                    navController.navigate(NavRoutes.Heatmap.route + "/$gameId")
                }
                //replace with endGameScreen
                else -> {
                    navController.navigate(NavRoutes.StartGame.route)
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        if (loggedInUser == null && !loading) {
            LoginForm(
                model = vm,
                navController = navController,
                token = token,
                launcher = launcher
            )
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

class AuthenticationViewModel() : ViewModel() {

    var fireBaseAuth = Firebase.auth
    var user = MutableLiveData<FirebaseUser>(null)
    var userIsInUsers = MutableLiveData<Boolean>()
    var emailValidationError = MutableLiveData<Boolean>()
    var emailIsAvailable = MutableLiveData<Boolean>()
    var passwordValidationError = MutableLiveData<Boolean>()
    var firestore = FirebaseHelper
    val currentGameId = MutableLiveData<String>()
    val gameStatus = MutableLiveData<Int>()

    fun validateEmail(email: String) {
        if (!isEmailValid(email)) {
            emailValidationError.postValue(true)
        } else
            emailValidationError.postValue(false)
    }

    fun validatePassword(password: String): Boolean {
        return if (!isPasswordValid(password)) {
            passwordValidationError.postValue(true)
            false
        } else {
            passwordValidationError.postValue(false)
            true
        }
    }

    fun updateUserDoc(userId: String, changeMap: Map<String, Any>) =
        firestore.updateUser(userId, changeMap)

    fun addUserDoc(userId: String, changeMap: Map<String, Any>) {
        firestore.addUser(changeMap, userId)
    }

    fun setUser(firebaseUser: FirebaseUser?) {
        user.value = firebaseUser
    }

    fun checkEmailAvailability(email: String) {
        firestore.getUsers().whereEqualTo("email", email).get().addOnSuccessListener { result ->
            if (result.documents.size == 0) {
                emailIsAvailable.value = true
            } else {
                emailIsAvailable.value = false
                emailValidationError.value = true
            }
        }
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
