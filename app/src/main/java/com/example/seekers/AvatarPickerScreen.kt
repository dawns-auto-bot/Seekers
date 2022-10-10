package com.example.seekers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.seekers.general.CustomButton
import com.example.seekers.ui.theme.avatarBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AvatarPickerScreen(
    vm: AvatarViewModel = viewModel(),
    navController: NavHostController,
    isCreator: Boolean,
) {

    val avatarId by vm.avatarId.observeAsState(R.drawable.avatar_empty)
    val nickname by vm.nickname.observeAsState("")
    val keyboardController = LocalSoftwareKeyboardController.current

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded }
    )
    val coroutineScope = rememberCoroutineScope()

    BackHandler(sheetState.isVisible) {
        coroutineScope.launch { sheetState.hide() }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            BottomSheet(onPick = {
                vm.avatarId.value = it
                coroutineScope.launch {
                    sheetState.hide()
                }
            })
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Choose your profile")
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.Black),
                backgroundColor = avatarBackground,
                modifier = Modifier
                    .clickable {
                        keyboardController?.hide()
                        coroutineScope.launch {
                            sheetState.show()
                        }
                    }
            ) {
                Image(
                    painter = painterResource(avatarId),
                    contentDescription = "avatar",
                    modifier = Modifier
                        .padding(30.dp)
                        .size(150.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Input(
                title = "Nickname",
                value = nickname,
                onChangeValue = {
                    vm.nickname.value = it
                })
            Spacer(modifier = Modifier.height(32.dp))
            CustomButton(text = if (isCreator) "Continue" else "Join lobby") {
                val avatarIndex = avatarList.indexOf(avatarId)
                if (!isCreator) {
                    navController.navigate(NavRoutes.Scanner.route + "/$nickname/$avatarIndex")
                } else {
                    navController.navigate(NavRoutes.LobbyCreation.route + "/$nickname/$avatarIndex")
                }
            }
        }
    }
}

var avatarList = listOf(
    R.drawable.bee,
    R.drawable.chameleon,
    R.drawable.chick,
    R.drawable.cow,
    R.drawable.crab,
    R.drawable.dog,
    R.drawable.elephant,
    R.drawable.fox,
    R.drawable.koala,
    R.drawable.lion,
    R.drawable.penguin,
    R.drawable.whale,
    R.drawable.avatar_empty
)

@Composable
fun BottomSheet(onPick: (Int) -> Unit) {
    Column(
        modifier = Modifier.padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Choose avatar", fontSize = 30.sp)
        Spacer(modifier = Modifier.height(32.dp))
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(avatarList) { avatar ->
                Card(
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Color.Black),
                    backgroundColor = avatarBackground,
                    modifier = Modifier
                        .padding(10.dp)
                        .clickable {
                            onPick(avatar)
                        }
                ) {
                    Image(
                        painter = painterResource(id = avatar),
                        contentDescription = "avatar",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(20.dp)
                    )
                }
            }
        }
    }

}

class AvatarViewModel() : ViewModel() {
    val avatarId = MutableLiveData<Int>(R.drawable.avatar_empty)
    val nickname = MutableLiveData("")
    val firestore = FirebaseHelper
}
