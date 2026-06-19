package com.example.ui

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MyApplication
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.*
import kotlinx.coroutines.launch

// Internal state screen navigation
enum class GameScreenDestination {
    MainMenu, SelectionTemple, ExplorerSkins, AchievementsMissions, DailyRewardsWheel, ScoreLeaderboard, GameConfigSettings, PolicyTermsScreen
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainGameContainer(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(GameScreenDestination.MainMenu) }
    val gameState by viewModel.gameState.collectAsState()
    val progress by viewModel.userProgress.collectAsState()
    val toastMessage by viewModel.showToastMessage.collectAsState()
    val activity = LocalContext.current as? Activity

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ObsidianDark, SandDark)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Main Routing State Machine
        if (gameState == GameState.Playing || gameState == GameState.Paused || gameState == GameState.ReviveOffer || gameState is GameState.GameOver) {
            TempleChallengePlayfield(
                viewModel = viewModel,
                onBackToMenu = {
                    if (activity != null) {
                        viewModel.handleRoundFinishedInterstitial(activity) {
                            currentDestination = GameScreenDestination.MainMenu
                            viewModel.skipReviveAndDie() // resets state
                        }
                    } else {
                        currentDestination = GameScreenDestination.MainMenu
                        viewModel.skipReviveAndDie() // resets state
                    }
                }
            )
        } else {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() with
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    GameScreenDestination.MainMenu -> MainMenuScreen(
                        viewModel = viewModel,
                        onNavigate = { currentDestination = it }
                    )
                    GameScreenDestination.SelectionTemple -> TempleSelectionScreen(
                        viewModel = viewModel,
                        onBack = { currentDestination = GameScreenDestination.MainMenu }
                    )
                    GameScreenDestination.ExplorerSkins -> SkinsShopScreen(
                        viewModel = viewModel,
                        onBack = { currentDestination = GameScreenDestination.MainMenu }
                    )
                    GameScreenDestination.AchievementsMissions -> TrophiesAndMissionsScreen(
                        viewModel = viewModel,
                        onBack = { currentDestination = GameScreenDestination.MainMenu }
                    )
                    GameScreenDestination.DailyRewardsWheel -> DailyRewardWheelScreen(
                        viewModel = viewModel,
                        onBack = { currentDestination = GameScreenDestination.MainMenu }
                    )
                    GameScreenDestination.ScoreLeaderboard -> LeaderboardScreen(
                        viewModel = viewModel,
                        onBack = { currentDestination = GameScreenDestination.MainMenu }
                    )
                    GameScreenDestination.GameConfigSettings -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentDestination = GameScreenDestination.MainMenu },
                        onNavigateToPrivacy = { currentDestination = GameScreenDestination.PolicyTermsScreen }
                    )
                    GameScreenDestination.PolicyTermsScreen -> PrivacyTermsScreen(
                        onBack = { currentDestination = GameScreenDestination.GameConfigSettings }
                    )
                }
            }
        }

        // Beautiful Interactive Onboarding Tutorial Overlay
        if (progress != null && !progress!!.onboardingCompleted) {
            OnboardingTutorialScreen(
                viewModel = viewModel,
                selectedLang = progress!!.selectedLanguage
            )
        }

        // Beautiful floating Toast notification
        toastMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .background(Color(0xE61C120C), RoundedCornerShape(16.dp))
                    .border(1.dp, GoldLight, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = msg,
                    color = PearlWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 1. MAIN MENU SCREEN
 */
@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    onNavigate: (GameScreenDestination) -> Unit
) {
    val progress by viewModel.userProgress.collectAsState()
    val activeTemple by viewModel.selectedTemple.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Interactive Drawn Egypt Pyramid Landscape
            EgyptianStylizedCanvas(modifier = Modifier.fillMaxWidth().height(140.dp))
            
            Spacer(modifier = Modifier.height(8.dp))

            // Game Golden Logo Title
            Text(
                text = "KHEMET GOLD",
                style = MaterialTheme.typography.displaySmall.copy(
                    brush = Brush.verticalGradient(
                        colors = listOf(GoldLight, GoldMedium, GoldDark)
                    ),
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 4.sp
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Ancient Temple Arcade Adventure",
                color = SandLight.copy(alpha = 0.8f),
                fontSize = 12.sp,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Core Stats card (M3 Glassmorphism Style)
        item {
            progress?.let { prog ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, GoldMedium.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = SandDark.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Rank: Level ${prog.currentLevel}",
                                    color = PearlWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Custom level progress bar
                                val xpRatio = if (prog.highXpNeeded > 0) prog.xp.toFloat() / prog.highXpNeeded else 0f
                                LinearProgressIndicator(
                                    progress = { xpRatio },
                                    modifier = Modifier
                                        .width(160.dp)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = GoldLight,
                                    trackColor = ObsidianDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "XP: ${prog.xp} / ${prog.highXpNeeded}",
                                    color = SandLight.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }

                            // Purse
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(ObsidianDark, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Paid,
                                    contentDescription = "Gold Coin",
                                    tint = GoldLight,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${prog.coins}",
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.testTag("purse_coin_text")
                                )
                            }
                        }

                        Divider(color = SandMedium, modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // Active Outfit
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("OUTFIT", color = SandLight.copy(alpha = 0.6f), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "Outfit Skin",
                                        tint = LapisLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = prog.equippedSkin.substringBefore(" Skin"),
                                        color = PearlWhite,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Selected Temple
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TEMPLE LOCATION", color = SandLight.copy(alpha = 0.6f), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Fort,
                                        contentDescription = "Temple icon",
                                        tint = GoldMedium,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeTemple.displayName,
                                        color = PearlWhite,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Daily Streak
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("LOGIN STREAK", color = SandLight.copy(alpha = 0.6f), fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Streak Fire",
                                        tint = CurseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${prog.currentStreak} Days",
                                        color = PearlWhite,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: Box(modifier = Modifier.padding(16.dp)) { CircularProgressIndicator(color = GoldLight) }
        }

        // Play Challenge BIG CTA Button
        item {
            Button(
                onClick = { viewModel.startChallenge() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(60.dp)
                    .testTag("play_game_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldLight,
                    contentColor = ObsidianDark
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, PearlWhite)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Icon",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXPLORE TEMPLE CHALLENGE",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Grid Menu Options
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuGridItem(
                        title = "Select Temples",
                        subtitle = "Travel & unlock chambers",
                        icon = Icons.Default.Fort,
                        tint = GoldMedium,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(GameScreenDestination.SelectionTemple) }
                    )
                    MenuGridItem(
                        title = "Outfits & Skins",
                        subtitle = "Customize explorer avatar",
                        icon = Icons.Default.ShoppingBag,
                        tint = LapisLight,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(GameScreenDestination.ExplorerSkins) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuGridItem(
                        title = "Daily rewards",
                        subtitle = "Lucky wheel & login bonuses",
                        icon = Icons.Default.Casino,
                        tint = EmeraldGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(GameScreenDestination.DailyRewardsWheel) }
                    )
                    MenuGridItem(
                        title = "Quests & Medals",
                        subtitle = "Active tomb missions",
                        icon = Icons.Default.EmojiEvents,
                        tint = GoldLight,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(GameScreenDestination.AchievementsMissions) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuGridItem(
                        title = "Leaderboards",
                        subtitle = "Local records list",
                        icon = Icons.Default.Leaderboard,
                        tint = PearlWhite,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(GameScreenDestination.ScoreLeaderboard) }
                    )
                    MenuGridItem(
                        title = "Settings & Help",
                        subtitle = "Configure tools & compliance",
                        icon = Icons.Default.Settings,
                        tint = SandLight,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(GameScreenDestination.GameConfigSettings) }
                    )
                }
            }
        }

        // Bottom Brand Banner (Family friendliness compliance)
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianDark.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Safe Play badge",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Family Friendly Guaranteed",
                            color = PearlWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "No real money gambling or betting. Strictly virtual treasure coins for cosmetic fun.",
                            color = SandLight.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
        
        // Real Live AdMob Banner on Home / Main Menu screen
        item {
            AdMobBanner(
                adsRemoved = progress?.adsRemoved == true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun MenuGridItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag("menu_item_${title.replace(" ", "_").lowercase()}")
            .border(1.dp, SandMedium.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SandDark.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier
                    .size(28.dp)
                    .background(ObsidianDark.copy(alpha = 0.5f), CircleShape)
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = PearlWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = SandLight.copy(alpha = 0.6f),
                fontSize = 11.sp,
                lineHeight = 13.sp
            )
        }
    }
}

/**
 * 2. SELECT TEMPLE SCREEN
 */
@Composable
fun TempleSelectionScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val progress by viewModel.userProgress.collectAsState()
    val activeTemple by viewModel.selectedTemple.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
            }
            Text(
                text = "Map of Ancient Temples",
                color = GoldMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif
            )
        }

        Text(
            text = "Embark your journey through ancient architectural wonders of Kemet. Higher temples offer richer gems but faster falling hazards!",
            color = SandLight.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Divider(color = SandMedium, modifier = Modifier.padding(bottom = 12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            val templates = Temple.values()
            items(templates) { temple ->
                val isUnlocked = progress?.unlockedTemples?.split(",")?.map { it.trim() }?.contains(temple.displayName) == true
                val isSelected = activeTemple == temple

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) GoldLight else if (isUnlocked) SandMedium.copy(alpha = 0.5f) else Color.DarkGray,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) Color(temple.bgHex).copy(alpha = 0.6f) else Color(0x99111111)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = temple.displayName,
                                    color = if (isUnlocked) PearlWhite else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                if (!isUnlocked) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = temple.description,
                                color = if (isUnlocked) SandLight.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        if (isUnlocked) {
                            if (isSelected) {
                                Button(
                                    onClick = {},
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldMedium, contentColor = ObsidianDark),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = false
                                ) {
                                    Text("Active", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.selectTemple(temple) },
                                    colors = ButtonDefaults.buttonColors(containerColor = LapisLight, contentColor = PearlWhite),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Travel", fontSize = 12.sp)
                                }
                            }
                        } else {
                            // Buy Button
                            Button(
                                onClick = { viewModel.shopBuyTemple(temple) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldLight, contentColor = ObsidianDark),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Paid, contentDescription = "Coins", tint = ObsidianDark, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("${temple.unlockCost}", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. OUTFITS & SKINS STORE SCREEN
 */
@Composable
fun SkinsShopScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val progress by viewModel.userProgress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
            }
            Text(
                text = "Explorer Outfits Store",
                color = GoldMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Balance visual
        progress?.let { prog ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianDark, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your Available Coins Balance:", color = SandLight.copy(alpha = 0.8f), fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Paid, contentDescription = "Coins", tint = GoldLight)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${prog.coins}", color = GoldLight, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(SKINS_LIST) { skin ->
                val isUnlocked = progress?.unlockedSkins?.split(",")?.map { it.trim() }?.contains(skin.name) == true
                val isEquipped = progress?.equippedSkin == skin.name

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isEquipped) 2.dp else 1.dp,
                            color = if (isEquipped) GoldLight else SandMedium.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = SandDark.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Icon mapping
                            val iconVec = when (skin.displayIcon) {
                                "blur_on" -> Icons.Default.BlurOn
                                "wb_shadows" -> Icons.Default.Nightlight
                                "light_mode" -> Icons.Default.LightMode
                                "military_tech" -> Icons.Default.MilitaryTech
                                else -> Icons.Default.Person
                            }
                            Icon(
                                imageVector = iconVec,
                                contentDescription = skin.name,
                                tint = if (isUnlocked) GoldMedium else Color.Gray,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(ObsidianDark, CircleShape)
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = skin.name,
                                    color = if (isUnlocked) PearlWhite else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = skin.description,
                                    color = SandLight.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        if (isUnlocked) {
                            if (isEquipped) {
                                Button(
                                    onClick = {},
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldMedium, contentColor = ObsidianDark),
                                    enabled = false,
                                    modifier = Modifier.testTag("equipped_btn_${skin.name.replace(" ", "_")}")
                                ) {
                                    Text("Active", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.shopBuySkin(skin) },
                                    colors = ButtonDefaults.buttonColors(containerColor = LapisLight, contentColor = PearlWhite),
                                    modifier = Modifier.testTag("equip_btn_${skin.name.replace(" ", "_")}")
                                ) {
                                    Text("Equip", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.shopBuySkin(skin) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldLight, contentColor = ObsidianDark),
                                modifier = Modifier.testTag("buy_btn_${skin.name.replace(" ", "_")}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Paid, contentDescription = "Coin icon", tint = ObsidianDark, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("${skin.costCoins}", fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Real Live AdMob Banner on shop screen
            item {
                AdMobBanner(
                    adsRemoved = progress?.adsRemoved == true,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

/**
 * 4. TROPHIES AND MISSIONS SCREEN
 */
@Composable
fun TrophiesAndMissionsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    var tabIndex by remember { mutableStateOf(0) } // 0 = Missions, 1 = Achievements
    val activeMissions by viewModel.missions.collectAsState()
    val activeAchievements by viewModel.achievements.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
            }
            Text(
                text = "Tomb Quests & Trophies",
                color = GoldMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // M3 Tabs
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = ObsidianDark,
            contentColor = GoldLight
        ) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                text = { Text("Daily Missions", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                text = { Text("Historical Awards", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tabIndex == 0) {
            // Missions List
            if (activeMissions.isEmpty()) {
                Text("Loading local temple missions...", color = SandLight)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(activeMissions) { mission ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (mission.isCompleted && !mission.isClaimed) GoldMedium else SandMedium.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = SandDark.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mission.title,
                                            color = PearlWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = mission.description,
                                            color = SandLight.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            lineHeight = 13.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (mission.isClaimed) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Claimed",
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else if (mission.isCompleted) {
                                        Button(
                                            onClick = { viewModel.claimMission(mission.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldLight, contentColor = ObsidianDark),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("Claim +${mission.rewardCoins}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lock, contentDescription = "In progress", tint = Color.Gray, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${mission.currentCount}/${mission.targetCount}",
                                                color = GoldMedium,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val ratio = if (mission.targetCount > 0) mission.currentCount.toFloat() / mission.targetCount else 0f
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (mission.isCompleted) EmeraldGreen else GoldMedium,
                                    trackColor = ObsidianDark
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Achievements Trophies List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(activeAchievements) { ach ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (ach.isCompleted) GoldLight else Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ach.isCompleted) SandDark.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Badge Icon mapping
                            val iconVec = when (ach.iconName) {
                                "emoji_events" -> Icons.Default.EmojiEvents
                                "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
                                "fort" -> Icons.Default.Fort
                                "local_fire_department" -> Icons.Default.LocalFireDepartment
                                else -> Icons.Default.Stars
                            }

                            Icon(
                                imageVector = iconVec,
                                contentDescription = "Badge",
                                tint = if (ach.isCompleted) GoldLight else Color.DarkGray,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(ObsidianDark, CircleShape)
                                    .padding(8.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ach.title,
                                    color = if (ach.isCompleted) PearlWhite else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = ach.description,
                                    color = if (ach.isCompleted) SandLight.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Reward: ${ach.rewardText}",
                                    color = if (ach.isCompleted) EmeraldGreen else Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (ach.isCompleted) {
                                Icon(
                                    Icons.Default.Stars,
                                    contentDescription = "Completed",
                                    tint = GoldLight,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = "${ach.progress}/${ach.maxProgress}",
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. SPIN THE WHEEL AND MYSTERY CHEST SCREEN
 */
@Composable
fun DailyRewardWheelScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val progress by viewModel.userProgress.collectAsState()
    val isSpinning by viewModel.isWheelSpinning.collectAsState()
    val wheelReward by viewModel.lastWheelReward.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
            }
            Text(
                text = "Temple Fortune Chambers",
                color = GoldMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Check-in Card
        progress?.let { prog ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldMedium.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SandDark.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pharaoh's Daily Check-in",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Claim daily to power up your login streak! Maximum rewards grow with streak progress up to Day 7.",
                        color = SandLight.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (day in 1..7) {
                            val isClaimed = day < prog.currentStreak
                            val isToday = day == prog.currentStreak
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("D$day", color = if (isToday) GoldLight else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    imageVector = if (isClaimed) Icons.Default.CheckCircle else Icons.Default.LocalFireDepartment,
                                    contentDescription = "Day $day",
                                    tint = if (isClaimed) EmeraldGreen else if (isToday) CurseRed else Color.DarkGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.claimDailyReward() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldMedium, contentColor = ObsidianDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Claim Daily Gift", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Lucky Spin Wheel graphic (using Canvas dynamic rotatable circle)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, SandMedium.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Amulet Power Wheel",
                    color = PearlWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Cost to spin: 25 Gems. Win up to 250 coins!",
                    color = SandLight.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rotated spinning graphics
                val transition = rememberInfiniteTransition(label = "WheelRotation")
                val rotationAngle by if (isSpinning) {
                    transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "Spinning"
                    )
                } else {
                    remember { mutableStateOf(0f) }
                }

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawCircle(
                                    color = SandMedium,
                                    style = Stroke(width = 8.dp.toPx())
                                )
                            }
                    ) {
                        val strokePx = 6.dp.toPx()
                        // Draw sections
                        val colors = listOf(GoldLight, LapisBlue, EmeraldGreen, CurseRed, GoldDark, SandDark)
                        for (i in 0 until 6) {
                            drawArc(
                                brush = Brush.radialGradient(colors = listOf(colors[i], ObsidianDark)),
                                startAngle = i * 60f + rotationAngle,
                                sweepAngle = 55f,
                                useCenter = true
                            )
                        }
                    }

                    // Centered cursor wheel arrow
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Arrow",
                        tint = PearlWhite,
                        modifier = Modifier
                            .size(36.dp)
                            .background(SandDark, CircleShape)
                            .padding(6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.spinLuckyWheel() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldLight, contentColor = ObsidianDark),
                    enabled = !isSpinning,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isSpinning) "SPINNING..." else "SPIN FOR 25 COINS", fontWeight = FontWeight.Bold)
                }

                wheelReward?.let { reward ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Payout Received: $reward",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Mystery Chest every 5 levels notification
        progress?.let { prog ->
            val hasMysteryChest = prog.currentLevel >= 5
            if (hasMysteryChest) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.claimMysteryChest() }
                        .border(1.dp, GoldLight, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LapisBlue.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Chest",
                            tint = GoldLight,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Unopened Secret Pharaoh Chest Available!", color = PearlWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Claim massive random gold coin grants! TAPS TO OPEN.", color = SandLight, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        AdMobBanner(
            adsRemoved = progress?.adsRemoved == true,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}

/**
 * 6. LEADERBOARD SCREEN
 */
@Composable
fun LeaderboardScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val scores by viewModel.leaderboard.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
            }
            Text(
                text = "Khemet High Scores",
                color = GoldMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Track your historical run completions. Scores are calculated based on tap speeds and temple relics rescued.",
            color = SandLight.copy(alpha = 0.7f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (scores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "No Scores", tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No records found in this device yet.", color = Color.Gray, fontSize = 13.sp)
                    Text("Play challenge rounds to record your score!", color = Color.Gray.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(scores.take(20)) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SandDark.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Stars, contentDescription = "Score Rank", tint = GoldMedium, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = item.playerName, color = PearlWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "Chamber: ${item.temple}", color = SandLight.copy(alpha = 0.5f), fontSize = 11.sp)
                                }
                            }
                            Text(
                                text = "${item.score} pts",
                                color = GoldLight,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 7. SETTINGS AND HELP SCREEN
 */
@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val progress by viewModel.userProgress.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val currentLang = progress?.selectedLanguage ?: "en"
    val label = { key: String -> Translations.getString(key, currentLang) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
            }
            Text(
                text = label("settings"),
                color = GoldMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Audio Controls (Sound, Music, Vibration)
            item {
                Text(label("audio_settings"), color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                progress?.let { prog ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SandDark.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Sound FX Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (prog.soundOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                        contentDescription = "Sound",
                                        tint = SandLight
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(label("sound_fx"), color = PearlWhite, fontSize = 14.sp)
                                }
                                Switch(
                                    checked = prog.soundOn,
                                    onCheckedChange = { viewModel.toggleSound() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = GoldLight, checkedTrackColor = GoldDark)
                                )
                            }

                            Divider(color = SandMedium, modifier = Modifier.padding(vertical = 10.dp))

                            // Music Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (prog.musicOn) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                        contentDescription = "Music",
                                        tint = SandLight
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(label("ambient_music"), color = PearlWhite, fontSize = 14.sp)
                                }
                                Switch(
                                    checked = prog.musicOn,
                                    onCheckedChange = { viewModel.toggleMusic() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = GoldLight, checkedTrackColor = GoldDark)
                                )
                            }

                            Divider(color = SandMedium, modifier = Modifier.padding(vertical = 10.dp))

                            // Vibration Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (prog.vibrationOn) Icons.Default.Vibration else Icons.Default.SignalWifiStatusbarNull,
                                        contentDescription = "Vibration",
                                        tint = SandLight
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(label("vibration"), color = PearlWhite, fontSize = 14.sp)
                                }
                                Switch(
                                    checked = prog.vibrationOn,
                                    onCheckedChange = { viewModel.toggleVibration() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = GoldLight, checkedTrackColor = GoldDark)
                                )
                            }
                        }
                    }
                }
            }

            // Language Selection Grid Block
            item {
                Text(label("language"), color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                val languagesList = listOf(
                    "en" to "English 🇺🇸",
                    "ar" to "العربية 🇪🇬",
                    "es" to "Español 🇪🇸",
                    "fr" to "Français 🇫🇷",
                    "de" to "Deutsch 🇩🇪",
                    "pt" to "Português 🇵🇹",
                    "tr" to "Türkçe 🇹🇷",
                    "hi" to "हिन्दी 🇮🇳",
                    "ru" to "Русский 🇷🇺",
                    "it" to "Italiano 🇮🇹"
                )
                progress?.let { prog ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SandDark.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            languagesList.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { (code, displayName) ->
                                        val isSelected = prog.selectedLanguage == code
                                        OutlinedButton(
                                            onClick = { viewModel.setLanguage(code) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(vertical = 4.dp),
                                            border = BorderStroke(1.dp, if (isSelected) GoldLight else Color.Transparent),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (isSelected) GoldLight else PearlWhite
                                            )
                                        ) {
                                            Text(displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Difficulty Settings Block Selection
            item {
                Text(label("difficulty"), color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                progress?.let { prog ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SandDark.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Normal", "Hard", "Expert").forEach { diffOption ->
                                val isSelected = prog.difficulty == diffOption
                                Button(
                                    onClick = { viewModel.setDifficulty(diffOption) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) GoldLight else SandMedium.copy(alpha = 0.4f),
                                        contentColor = if (isSelected) ObsidianDark else PearlWhite
                                    ),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = when(diffOption) {
                                            "Normal" -> label("difficulty_normal")
                                            "Hard" -> label("difficulty_hard")
                                            else -> label("difficulty_expert")
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Remove Ads (FREE simulation block)
            item {
                progress?.let { prog ->
                    if (!prog.adsRemoved) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GoldLight, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = LapisBlue.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label("remove_ads"),
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label("remove_ads_text"),
                                    color = PearlWhite,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.removeAds() },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldLight, contentColor = ObsidianDark)
                                ) {
                                    Text(label("remove_ads_btn"), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Ads Removed", tint = EmeraldGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label("ads_removed_confirmed"), color = PearlWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Compliance notices
            item {
                Text(label("compliance_notices"), color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SandDark.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = "Notice", tint = CurseRed, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(label("parental_notice"), color = PearlWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = label("parental_notice_text"),
                                    color = SandLight.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                             }
                        }
                    }
                }
            }

            // Simulated Rewarded Ad trigger button
            item {
                Text(label("rewarded_test_title"), color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SandDark.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = label("rewarded_test_desc"),
                            color = SandLight.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { activity?.let { viewModel.triggerExtraCoinsReward(it) } },
                                colors = ButtonDefaults.buttonColors(containerColor = LapisLight, contentColor = PearlWhite),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.OndemandVideo, contentDescription = "Ad", tint = GoldLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(label("trigger_test_ad"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Privacy button policy screen
            item {
                Button(
                    onClick = { onNavigateToPrivacy() },
                    colors = ButtonDefaults.buttonColors(containerColor = SandMedium, contentColor = PearlWhite),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(label("privacy_policy_btn"), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Real Live AdMob Banner at the bottom of the Settings scroll list
            item {
                AdMobBanner(
                    adsRemoved = progress?.adsRemoved == true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 8. PRIVACY POLICY & TERMS SCREEN
 */
@Composable
fun PrivacyTermsScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
            }
            Text(
                text = "Privacy & User Terms",
                color = GoldMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Text(
                    text = "Privacy Policy Summary",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Effective Date: June 2026\n\n1. Information Collection: Khemet Gold is an offline-first game that processes all gameplay metrics locally using secure Android Room SQL architecture. We do NOT harvest or transmit device identifying cookies, location arrays, or private user credentials to remote clouds.\n\n2. Device Permissions: This application requests standard internet connectivity metrics strictly to support secure virtual ad networks and localized display rules. No hardware storage, tracking sensors, or camera logs are parsed.\n\n3. Child Protection: This module strictly complies with COPPA (Children's Online Privacy Protection) standards. We do not solicit personal details from children under 13.",
                    color = SandLight.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            item {
                Text(
                    text = "Terms of Service Highlights",
                    color = GoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "1. Game Use Agreement: By installing Khemet Gold, you agree to access content strictly in a personal leisure mode. Spoofing or reverse engineering local SQL databases to manipulate coins is prohibited.\n\n2. Virtual Wallet Valuation: Gold coins of Khemet are for vanity purposes only. Coins have zero cash equivalents and cannot be refunded, transferred, or exchanged for real-world legal tender.\n\n3. Disclaimer: The graphics and game design are hypothetical simulations of archaeological tombs and do not portray historical facts.",
                    color = SandLight.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 9. ACTIVE TEMPLE CHALLENGE PLAYFIELD (ARCADE INTERACTION SCREEN)
 */
@Composable
fun TempleChallengePlayfield(
    viewModel: GameViewModel,
    onBackToMenu: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val timeLeft by viewModel.gameTimeLeft.collectAsState()
    val score by viewModel.score.collectAsState()
    val coinsWon by viewModel.coinsCollectedInRound.collectAsState()
    val playerLives by viewModel.playerLives.collectAsState()
    val fallingTargets by viewModel.fallingTargets.collectAsState()
    val tapSparks by viewModel.recentTapSparks.collectAsState()
    val templeState by viewModel.selectedTemple.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val progress by viewModel.userProgress.collectAsState()

    // Screen Shake & Combo/Boss features
    val screenShakeOffset by viewModel.screenShakeOffset.collectAsState()
    val isFeverMode by viewModel.isFeverMode.collectAsState()
    val comboCount by viewModel.comboCount.collectAsState()
    val comboMultiplier by viewModel.comboMultiplier.collectAsState()
    val isBossChallenge by viewModel.isBossChallenge.collectAsState()
    val bossHp by viewModel.bossHp.collectAsState()
    val bossMaxHp by viewModel.bossMaxHp.collectAsState()

    var activePlayfieldHeight by remember { mutableStateOf(100f) }
    var activePlayfieldWidth by remember { mutableStateOf(100f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(templeState.bgHex))
            .padding(14.dp)
    ) {
        // Upper stats banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showExitConfirmDialog = true }) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Exit to Menu", tint = CurseRed)
            }

            // Score Tracker
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SCORE", color = SandLight.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("$score pts", color = GoldLight, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }

            // Coins Won Tracker
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("COINS WON", color = SandLight.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Paid, contentDescription = "Coins", tint = GoldLight, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("+$coinsWon", color = PearlWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Hearts Left
            Row {
                for (i in 1..3) {
                    val isFull = i <= playerLives
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Heart",
                        tint = if (isFull) CurseRed else Color.DarkGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Timer Linear Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianDark, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.Alarm, contentDescription = "Timer", tint = GoldLight, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            LinearProgressIndicator(
                progress = { timeLeft / 30f },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (timeLeft < 10) CurseRed else GoldLight,
                trackColor = Color.DarkGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("$timeLeft s", color = PearlWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Large Falling Game Field Canvas
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .offset(x = screenShakeOffset.dp, y = 0.dp) // Screen shake vibration offset
                .background(ObsidianDark.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .border(2.dp, GoldMedium, RoundedCornerShape(20.dp))
        ) {
            activePlayfieldWidth = constraints.maxWidth.toFloat()
            activePlayfieldHeight = constraints.maxHeight.toFloat()

            // Drawing custom ancient Egyptian background graphics on active container
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw a simple glowing background hieroglyphic disc
                drawCircle(
                    color = GoldDark.copy(alpha = 0.15f),
                    radius = activePlayfieldWidth * 0.35f,
                    center = Offset(activePlayfieldWidth / 2f, activePlayfieldHeight / 2f),
                    style = Stroke(width = 4.dp.toPx())
                )
                // Draw decorative pyramids lines
                val path = Path().apply {
                    moveTo(0f, activePlayfieldHeight)
                    lineTo(activePlayfieldWidth * 0.35f, activePlayfieldHeight * 0.7f)
                    lineTo(activePlayfieldWidth * 0.7f, activePlayfieldHeight)
                    
                    moveTo(activePlayfieldWidth * 0.5f, activePlayfieldHeight)
                    lineTo(activePlayfieldWidth * 0.8f, activePlayfieldHeight * 0.8f)
                    lineTo(activePlayfieldWidth, activePlayfieldHeight)
                }
                drawPath(path, color = SandMedium.copy(alpha = 0.2f), style = Stroke(width = 2.dp.toPx()))
            }

            // Map and Draw falling targets
            for (target in fallingTargets) {
                // Width calculation based on target type sizes (Boss targets are giant!)
                val boxSize = if (target.type == TargetType.BOSS) 80.dp else 48.dp
                val itemX = target.xOffsetPerc * (this.maxWidth - boxSize).value
                val itemY = target.yOffsetPerc * (this.maxHeight - boxSize).value

                Box(
                    modifier = Modifier
                        .offset(x = itemX.dp, y = itemY.dp)
                        .size(boxSize)
                        .testTag("falling_target_${target.id}")
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            viewModel.tapTarget(target.id, itemX, itemY)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Golden Scarab, Jewel, Cursed Stone, or Giant Boss Warlord design
                    when (target.type) {
                        TargetType.BOSS -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Mini health bar directly above boss unit
                                Row(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(6.dp)
                                        .background(Color.DarkGray, RoundedCornerShape(3.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = (bossHp.toFloat() / bossMaxHp.toFloat()).coerceIn(0f, 1f))
                                            .background(CurseRed, RoundedCornerShape(3.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Card(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .border(3.dp, GoldLight, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = ObsidianDark),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("🦁", fontSize = 34.sp) // Temple Sphynx Boss Guard Icon
                                    }
                                }
                            }
                        }
                        TargetType.SCARAB -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, GoldLight, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = GoldMedium),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("\uD83E\uDEB2", fontSize = 24.sp) // Beetles/Scarab
                                }
                            }
                        }
                        TargetType.GEM -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, Color.Cyan, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = LapisLight),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("\uD83D\uDC8E", fontSize = 22.sp) // Jewel gem
                                }
                            }
                        }
                        TargetType.BLACK_STONE -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.dp, CurseRed, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = ObsidianDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("\uD83E\uDEA8", fontSize = 20.sp) // Cursed Rock Stone
                                }
                            }
                        }
                    }
                }
            }

            // Draw Dynamic Tap Sparks and Falling Physics Particles (Coins, Gems, etc.)
            for (spark in tapSparks) {
                Box(
                    modifier = Modifier
                        .offset(x = spark.x.dp, y = spark.y.dp)
                        .size(spark.size.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = spark.text,
                        fontSize = (spark.size * 0.85f).sp,
                        color = Color(spark.color).copy(alpha = spark.lifespan)
                    )
                }
            }

            // Blazing Fever Mode and Combo Streaks overlay
            if (isFeverMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                        .background(Color(0xFFFF4500).copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                        .border(1.5.dp, GoldLight, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FEVER MODE ACTIVE! DOUBLE COINS!",
                            color = PearlWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            } else if (comboCount >= 3) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                        .background(GoldDark.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "COMBO: $comboCount (x$comboMultiplier!) ✨",
                        color = PearlWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Action Toolbar (Pause / Resume)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (gameState == GameState.Playing) viewModel.pauseChallenge() else viewModel.resumeChallenge()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SandDark, contentColor = PearlWhite)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (gameState == GameState.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Pause",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (gameState == GameState.Playing) "PAUSE" else "RESUME", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Text(
                text = "Tap Scarabs & Gems! Protect life!",
                color = SandLight.copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.End
            )
        }
    }

    // Interactive overlays: Pawns of state transitions
    if (gameState == GameState.Paused) {
        AlertDialog(
            onDismissRequest = { viewModel.resumeChallenge() },
            title = { Text("Game Paused", color = GoldLight, fontWeight = FontWeight.Bold) },
            text = { Text("The secrets of Khemet are waiting for your exploration. Keep tapping golden components, avoid black cursed rocks!") },
            confirmButton = {
                Button(onClick = { viewModel.resumeChallenge() }) { Text("RESUME CHALLENGE") }
            },
            dismissButton = {
                TextButton(onClick = onBackToMenu) { Text("BACK TO MENU") }
            },
            containerColor = SandDark,
            titleContentColor = GoldLight,
            textContentColor = SandLight
        )
    }

    if (gameState == GameState.ReviveOffer) {
        // Offer revive with rewarded ad
        AlertDialog(
            onDismissRequest = { viewModel.skipReviveAndDie() },
            title = { Text("\uD83D\uDC80 Temple Curse High!", color = CurseRed, fontWeight = FontWeight.Bold) },
            text = {
                Text("Your archaeology squad hit too many cursed stones and dropped to 0 hearts! Revive now to keep your score streak going?")
            },
            confirmButton = {
                Button(
                    onClick = { activity?.let { viewModel.triggerReviveAdReward(it) } },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldLight, contentColor = ObsidianDark)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.OndemandVideo, contentDescription = "Rewarded ad")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("REVIVE NOW (Watch Ad)", fontWeight = FontWeight.Black)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.skipReviveAndDie() }) {
                    Text("No Thanks, Finish Run", color = Color.Gray)
                }
            },
            containerColor = ObsidianDark,
            titleContentColor = CurseRed,
            textContentColor = SandLight
        )
    }

    if (gameState is GameState.GameOver) {
        val isVictory = (gameState as GameState.GameOver).victory
        AlertDialog(
            onDismissRequest = onBackToMenu,
            title = {
                Text(
                    text = if (isVictory) "\uD83C\uDF1F Challenge Completed!" else "\uD83D\uDC80 Tomb Cleared",
                    color = if (isVictory) GoldLight else CurseRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isVictory) "Amazing reflexes! You salvaged essential artifacts before the temple collapsed!"
                        else "Challenge ended. You received standard coins but can improve gears to explore further next time.",
                        color = SandLight,
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ObsidianDark.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Final Challenge Score:", color = SandLight, fontSize = 12.sp)
                                Text("$score pts", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gold Coins collected:", color = SandLight, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Paid, contentDescription = "Gold", tint = GoldLight, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("+$coinsWon Coins", color = PearlWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (isVictory && coinsWon > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Double reward Ad button!
                        Button(
                            onClick = { activity?.let { viewModel.doubleEndCoinsReward(it) } },
                            colors = ButtonDefaults.buttonColors(containerColor = LapisLight, contentColor = PearlWhite),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.OndemandVideo, contentDescription = "Ad Double", tint = GoldLight)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Double Level Coins (Watch Ad)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onBackToMenu,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldMedium, contentColor = ObsidianDark)
                ) {
                    Text("BACK TO TEMPLE MAP", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SandDark,
            titleContentColor = GoldLight,
            textContentColor = SandLight
        )
    }

    if (showExitConfirmDialog) {
        val currentLang = progress?.selectedLanguage ?: "en"
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text(Translations.getString("exit_confirm", currentLang), color = CurseRed, fontWeight = FontWeight.Bold) },
            text = { Text(Translations.getString("exit_desc", currentLang)) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        onBackToMenu()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CurseRed)
                ) {
                    Text(Translations.getString("yes", currentLang).uppercase(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text(Translations.getString("no", currentLang).uppercase(), color = Color.Gray)
                }
            },
            containerColor = ObsidianDark,
            titleContentColor = CurseRed,
            textContentColor = SandLight
        )
    }
}

/**
 * CUSTOM STYLIZED EGYPT CANVAS LOGO DESIGNS (PYRAMIDS drawing)
 */
@Composable
fun EgyptianStylizedCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background Golden glowing circle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GoldLight.copy(alpha = 0.4f), Color.Transparent),
                center = Offset(w / 2f, h * 0.4f)
            ),
            radius = h * 0.5f,
            center = Offset(w / 2f, h * 0.4f)
        )

        // Draw Pyramids on the sand dunes
        // Pyramid 1 (Left)
        val p1 = Path().apply {
            moveTo(w * 0.25f, h * 0.9f)
            lineTo(w * 0.42f, h * 0.35f)
            lineTo(w * 0.6f, h * 0.9f)
            close()
        }
        drawPath(p1, brush = Brush.linearGradient(colors = listOf(SandMedium, SandDark)))

        // Pyramid 1 shadow face
        val p1Shadow = Path().apply {
            moveTo(w * 0.42f, h * 0.35f)
            lineTo(w * 0.6f, h * 0.9f)
            lineTo(w * 0.5f, h * 0.9f)
            close()
        }
        drawPath(p1Shadow, color = Black50)

        // Pyramid 2 (Smaller on right)
        val p2 = Path().apply {
            moveTo(w * 0.5f, h * 0.9f)
            lineTo(w * 0.65f, h * 0.5f)
            lineTo(w * 0.82f, h * 0.9f)
            close()
        }
        drawPath(p2, brush = Brush.linearGradient(colors = listOf(GoldDark, SandMedium)))

        // Sand Line base
        drawLine(
            color = GoldLight,
            start = Offset(w * 0.1f, h * 0.9f),
            end = Offset(w * 0.9f, h * 0.9f),
            strokeWidth = 3.dp.toPx()
        )
    }
}

private val Black50 = Color(0x33000000)

@Composable
fun OnboardingTutorialScreen(
    viewModel: GameViewModel,
    selectedLang: String
) {
    var step by remember { mutableStateOf(1) }
    val label = { key: String -> Translations.getString(key, selectedLang) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0604)) // dim elegant theme overlay
            .clickable(enabled = false) {}
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, GoldLight, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = SandDark),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header progress dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    for (i in 1..3) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (i == step) GoldLight else Color.Gray)
                        )
                    }
                }

                // Dynamic icon relative to the step
                Icon(
                    imageVector = when(step) {
                        1 -> Icons.Default.Stars
                        2 -> Icons.Default.ReportProblem
                        else -> Icons.Default.Fort
                    },
                    contentDescription = null,
                    tint = if (step == 2) CurseRed else GoldLight,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Onboarding title Look-Up
                Text(
                    text = label(
                        when(step) {
                            1 -> "onboarding_title_1"
                            2 -> "onboarding_title_2"
                            else -> "onboarding_title_3"
                        }
                    ),
                    color = PearlWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Serif
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Onboarding description Look-Up
                Text(
                    text = label(
                        when(step) {
                            1 -> "onboarding_desc_1"
                            2 -> "onboarding_desc_2"
                            else -> "onboarding_desc_3"
                        }
                    ),
                    color = SandLight,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Next or Done button
                Button(
                    onClick = {
                        if (step < 3) {
                            step++
                        } else {
                            viewModel.completeOnboarding()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldLight, contentColor = ObsidianDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (step < 3) label("onboard_next").uppercase() else label("onboard_done").uppercase(),
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
