package com.example.seekers

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.seekers.general.CustomButton
import com.example.seekers.general.LogOutButton
import com.example.seekers.ui.theme.Powder
import com.example.seekers.ui.theme.Raisin
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun StartGameScreen(navController: NavController) {
    val screenHeight = LocalConfiguration.current.screenHeightDp * 0.3

    Surface {
        Box(
            Modifier
                .fillMaxSize()
                .background(Powder)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight.dp)
                        .padding(horizontal = 15.dp, vertical = 5.dp)
                        .clickable { navController.navigate(NavRoutes.AvatarPicker.route + "/true") },
                    elevation = 10.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(R.drawable.illustration1),
                            contentDescription = "illustration",
                            modifier = Modifier
                                .fillMaxSize(),
                            alignment = Alignment.CenterStart
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(32.dp)
                        ) {
                            Column {
                                Text(text = "CREATE\nLOBBY", fontSize = 22.sp)
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(1.dp)
                                        .background(color = Raisin)
                                )
                            }
                        }
                    }

                    /*
                    CustomButton(text = "Create lobby") {
                        navController.navigate(NavRoutes.AvatarPicker.route + "/true")
                    } */
                }
                // Spacer(modifier = Modifier.height(50.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight.dp)
                        .padding(horizontal = 15.dp, vertical = 5.dp)

                        .clickable { navController.navigate(NavRoutes.AvatarPicker.route + "/false") },
                    elevation = 10.dp
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(R.drawable.illustration2),
                            contentDescription = "illustration",
                            modifier = Modifier
                                .fillMaxSize(),
                            alignment = Alignment.CenterEnd
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(32.dp)
                        ) {
                            Column {
                                Text(text = "JOIN\nLOBBY", fontSize = 22.sp)
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(1.dp)
                                        .background(color = Raisin)
                                )
                            }
                        }
                    }

                    /*
                    CustomButton(text = "Join lobby") {
                        navController.navigate(NavRoutes.AvatarPicker.route + "/false")
                    } */
                }

            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
            ) {
                LogOutButton(text = "Log out") {
                    Firebase.auth.signOut()
                    println("logged user: ${Firebase.auth.currentUser}")
                    navController.navigate(NavRoutes.MainScreen.route)
                }
            }
        }
    }
}

@Composable
private fun DividedImageCard() {
    val screenHeight = LocalConfiguration.current.screenHeightDp * 0.5

    Card() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight.dp)
                .padding(8.dp)
        ) {

            val shapeLeft = GenericShape { size: Size, layoutDirection: LayoutDirection ->
                val width = size.width
                val height = size.height
                moveTo(0f, height)
                lineTo(0f, 0f)
                lineTo(width, 0f)
                close()
            }

            val shapeRight = GenericShape { size: Size, layoutDirection: LayoutDirection ->
                val width = size.width
                val height = size.height
                moveTo(width, 0f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            val modifierLeft = Modifier
                .fillMaxWidth()

                .graphicsLayer {
                    clip = true
                    shape = shapeLeft
                }
                .clickable { }



            val modifierRight = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    clip = true
                    shape = shapeRight
                }
                .clickable { }


            Image(
                modifier = modifierLeft,
                painter = painterResource(id = R.drawable.illustration1),
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )

            Image(
                modifier = modifierRight,
                painter = painterResource(id = R.drawable.illustration2),
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )
        }
    }
}
