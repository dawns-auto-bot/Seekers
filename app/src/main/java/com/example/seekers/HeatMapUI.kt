package com.example.seekers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.seekers.general.AvatarIcon
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.SizzlingRed
import java.util.*

@Composable
fun PlayerListButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(shape = CircleShape, elevation = 4.dp, modifier = modifier.clickable { onClick() }) {
        Icon(
            imageVector = Icons.Filled.List,
            contentDescription = "playerList",
            modifier = Modifier.padding(8.dp)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayerListDialog(onDismiss: () -> Unit, players: List<Player>) {
    val height = LocalConfiguration.current.screenHeightDp * 0.7
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .padding(32.dp),
            backgroundColor = Powder,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 32.dp)) {
                Text(text = "PLAYERS", fontSize = 22.sp)
                Spacer(modifier = Modifier.height(32.dp))
                PlayerList(players = players)
            }
        }
    }
}

@Composable
fun PlayerList(players: List<Player>) {
    LazyColumn(Modifier.fillMaxWidth()) {
        items(players) {
            PlayerTile(player = it)
        }
    }
}

@Composable
fun PlayerTile(player: Player) {
    Column(Modifier.fillMaxWidth()) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarIcon(
                    resourceId = avatarList[player.avatarId], imgModifier = Modifier
                        .size(50.dp)
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = player.nickname)
                Spacer(modifier = Modifier.weight(1f))
                StatusPill(inGameStatus = player.inGameStatus)
            }
        }

    }
}

@Composable
fun StatusPill(inGameStatus: Int) {
    val status = when (inGameStatus) {
        InGameStatus.SEEKER.value -> "seeker"
        InGameStatus.PLAYER.value -> "hiding"
        InGameStatus.MOVING.value -> "moving"
        InGameStatus.ELIMINATED.value -> "eliminated"
        else -> "unknown"
    }
    val color = if (inGameStatus == InGameStatus.SEEKER.value) SizzlingRed else Color.LightGray

    Card(shape = RoundedCornerShape(16.dp), backgroundColor = color) {
        Text(
            text = status.uppercase(Locale.ROOT),
            fontSize = 16.sp,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(vertical = 4.dp),
            color = Color.White
        )
    }
}