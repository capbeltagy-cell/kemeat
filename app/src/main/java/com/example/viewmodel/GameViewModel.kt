package com.example.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.AdManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class GameViewModel(
    application: Application,
    private val repository: GameRepository
) : AndroidViewModel(application) {

    // Persistent States
    val userProgress = repository.userProgress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val leaderboard = repository.leaderboard.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val mIsMissionsLoading = MutableStateFlow(false)
    val missions = repository.missions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val achievements = repository.achievements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Game Session states
    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _gameTimeLeftLeft = MutableStateFlow(30)
    val gameTimeLeft: StateFlow<Int> = _gameTimeLeftLeft.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _scarabsTapped = MutableStateFlow(0)
    val scarabsTapped: StateFlow<Int> = _scarabsTapped.asStateFlow()

    private val _gemsTapped = MutableStateFlow(0)
    val gemsTapped: StateFlow<Int> = _gemsTapped.asStateFlow()

    private val _coinsCollectedInRound = MutableStateFlow(0)
    val coinsCollectedInRound: StateFlow<Int> = _coinsCollectedInRound.asStateFlow()

    private val _playerLives = MutableStateFlow(3)
    val playerLives: StateFlow<Int> = _playerLives.asStateFlow()

    private val _selectedTemple = MutableStateFlow(Temple.TEMPLE_OF_RA)
    val selectedTemple: StateFlow<Temple> = _selectedTemple.asStateFlow()

    // Spawn falling targets
    private val _fallingTargets = MutableStateFlow<List<FallingTarget>>(emptyList())
    val fallingTargets: StateFlow<List<FallingTarget>> = _fallingTargets.asStateFlow()

    // Interactive taps visuals
    private val _recentTapSparks = MutableStateFlow<List<TapSpark>>(emptyList())
    val recentTapSparks: StateFlow<List<TapSpark>> = _recentTapSparks.asStateFlow()

    // Lucky Wheel status
    private val _lastWheelReward = MutableStateFlow<String?>(null)
    val lastWheelReward: StateFlow<String?> = _lastWheelReward.asStateFlow()

    private val _isWheelSpinning = MutableStateFlow(false)
    val isWheelSpinning: StateFlow<Boolean> = _isWheelSpinning.asStateFlow()

    // UI Messages helper
    private val _showToastMessage = MutableStateFlow<String?>(null)
    val showToastMessage: StateFlow<String?> = _showToastMessage.asStateFlow()

    // High-fidelity Combo, Fever, Screen Shake & Boss properties
    private val _comboCount = MutableStateFlow(0)
    val comboCount: StateFlow<Int> = _comboCount.asStateFlow()

    private val _comboMultiplier = MutableStateFlow(1)
    val comboMultiplier: StateFlow<Int> = _comboMultiplier.asStateFlow()

    private val _isFeverMode = MutableStateFlow(false)
    val isFeverMode: StateFlow<Boolean> = _isFeverMode.asStateFlow()

    private val _screenShakeOffset = MutableStateFlow(0f)
    val screenShakeOffset: StateFlow<Float> = _screenShakeOffset.asStateFlow()

    private val _isBossChallenge = MutableStateFlow(false)
    val isBossChallenge: StateFlow<Boolean> = _isBossChallenge.asStateFlow()

    private val _bossHp = MutableStateFlow(0)
    val bossHp: StateFlow<Int> = _bossHp.asStateFlow()

    private val _bossMaxHp = MutableStateFlow(8)
    val bossMaxHp: StateFlow<Int> = _bossMaxHp.asStateFlow()

    private var gameLoopJob: Job? = null
    private var physicsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDefaultDataIfNeeded()
        }
        // Sync user sound preferences to EgyptianToneEngine in real-time
        viewModelScope.launch {
            userProgress.collect { prog ->
                prog?.let {
                    com.example.ui.EgyptianToneEngine.soundEnabledFlag = it.soundOn
                    com.example.ui.EgyptianToneEngine.musicEnabledFlag = it.musicOn
                    if (it.musicOn) {
                        com.example.ui.EgyptianToneEngine.startBgm()
                    } else {
                        com.example.ui.EgyptianToneEngine.stopBgm()
                    }
                }
            }
        }
    }

    fun selectTemple(temple: Temple) {
        _selectedTemple.value = temple
    }

    fun triggerToast(msg: String) {
        _showToastMessage.value = msg
        viewModelScope.launch {
            delay(1500)
            if (_showToastMessage.value == msg) {
                _showToastMessage.value = null
            }
        }
    }

    fun toggleSound() {
        val prog = userProgress.value ?: return
        viewModelScope.launch {
            repository.updateSoundSettings(!prog.soundOn, prog.musicOn)
        }
    }

    fun toggleMusic() {
        val prog = userProgress.value ?: return
        viewModelScope.launch {
            repository.updateSoundSettings(prog.soundOn, !prog.musicOn)
        }
    }

    fun toggleVibration() {
        val prog = userProgress.value ?: return
        viewModelScope.launch {
            repository.updateVibrationOn(!prog.vibrationOn)
        }
    }

    fun setLanguage(langCode: String) {
        viewModelScope.launch {
            repository.updateSelectedLanguage(langCode)
        }
    }

    fun setDifficulty(diff: String) {
        viewModelScope.launch {
            repository.updateDifficulty(diff)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.completeOnboarding()
            repository.addCoins(100)
            triggerToast("Onboarding Completed! Welcome gift of +100 Coins added!")
        }
    }

    fun removeAds() {
        viewModelScope.launch {
            repository.triggerAdsRemoved()
            triggerToast("Ads successfully disabled!")
        }
    }

    fun triggerVibration() {
        val prog = userProgress.value ?: return
        if (prog.vibrationOn) {
            try {
                val vibrator = getApplication<Application>().getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(80, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Daily Claim
    fun claimDailyReward() {
        viewModelScope.launch {
            val coinsEarned = repository.claimDailyReward()
            if (coinsEarned > 0) {
                com.example.ui.EgyptianToneEngine.playChestSound()
                triggerToast("Claimed Daily Reward! +$coinsEarned Golden Coins \uD83C\uDF1F")
            } else if (coinsEarned == -1) {
                triggerToast("Come back tomorrow to increase your streak reward!")
            } else {
                triggerToast("Claiming Daily Reward...")
            }
        }
    }

    // Wheel Spin
    fun spinLuckyWheel() {
        if (_isWheelSpinning.value) return
        val currentCoins = userProgress.value?.coins ?: 0
        if (currentCoins < 25) {
            triggerToast("Spinning costs 25 Coins!")
            return
        }

        viewModelScope.launch {
            repository.addCoins(-25)
            _isWheelSpinning.value = true
            _lastWheelReward.value = null
            
            // Simulating a long wheel spin
            delay(2200)

            val spinType = Random.nextInt(100)
            val (text, rewardCoins) = when {
                spinType < 40 -> Pair("50 Gold Coins!", 50)
                spinType < 70 -> Pair("100 Rich Coins \uD83C\uDFC6", 100)
                spinType < 85 -> Pair("Jackpot! 250 Coins \uD83D\uDC8E", 250)
                spinType < 95 -> Pair("Golden Gem! 150 Coins", 150)
                else -> Pair("Better luck next time! 10 Coins refund", 10)
            }

            repository.addCoins(rewardCoins)
            com.example.ui.EgyptianToneEngine.playChestSound()
            _lastWheelReward.value = text
            _isWheelSpinning.value = false
            triggerToast("Lucky Spin Won: $text")
        }
    }

    // Mystery chest
    fun claimMysteryChest() {
        viewModelScope.launch {
            val rCoins = Random.nextInt(200, 501)
            repository.addCoins(rCoins)
            com.example.ui.EgyptianToneEngine.playChestSound()
            triggerToast("Mystery Ancient Chest opened! Unlocked +$rCoins Golden Coins!")
        }
    }

    // Missions claim
    fun claimMission(id: String) {
        viewModelScope.launch {
            val coins = repository.claimMissionReward(id)
            if (coins > 0) {
                com.example.ui.EgyptianToneEngine.playChestSound()
                triggerToast("Claimed Mission! Received +$coins Coins")
            }
        }
    }

    // Unlock Skins & Temples
    fun shopBuySkin(skin: SkinItem) {
        viewModelScope.launch {
            val prog = userProgress.value ?: return@launch
            val list = prog.unlockedSkins.split(",").map { it.trim() }
            if (list.contains(skin.name)) {
                // equip code
                val ok = repository.equipSkin(skin.name)
                if (ok) triggerToast("Equipped: ${skin.name}")
            } else {
                // buy code
                if (prog.coins >= skin.costCoins) {
                    val ok = repository.unlockSkin(skin.name, skin.costCoins)
                    if (ok) {
                        repository.equipSkin(skin.name)
                        triggerToast("Unlocked & Equipped: ${skin.name}!")
                    }
                } else {
                    triggerToast("Not enough gold coins!")
                }
            }
        }
    }

    fun shopBuyTemple(temple: Temple) {
        if (temple == Temple.TEMPLE_OF_RA) return
        viewModelScope.launch {
            val prog = userProgress.value ?: return@launch
            val list = prog.unlockedTemples.split(",").map { it.trim() }
            if (list.contains(temple.displayName)) {
                _selectedTemple.value = temple
                triggerToast("Loaded Temple of ${temple.displayName}!")
            } else {
                if (prog.coins >= temple.unlockCost) {
                    val ok = repository.unlockTemple(temple.displayName, temple.unlockCost)
                    if (ok) {
                        _selectedTemple.value = temple
                        triggerToast("Temple ${temple.displayName} Unlocked \uD83C\uDFDB️")
                    }
                } else {
                    triggerToast("Need ${temple.unlockCost} Coins to unlock ${temple.displayName}!")
                }
            }
        }
    }


    // Screen shake trigger animation
    fun triggerScreenShake() {
        viewModelScope.launch {
            val offsets = listOf(-14f, 14f, -11f, 11f, -8f, 8f, -5f, 5f, -2f, 2f, 0f)
            for (offset in offsets) {
                _screenShakeOffset.value = offset
                delay(20)
            }
        }
    }

    // Start game
    fun startChallenge() {
        // Reset session state
        _score.value = 0
        _scarabsTapped.value = 0
        _gemsTapped.value = 0
        _coinsCollectedInRound.value = 0
        _playerLives.value = 3
        _gameTimeLeftLeft.value = 30
        _fallingTargets.value = emptyList()
        _recentTapSparks.value = emptyList()

        // Reset Combo & Fever Mode
        _comboCount.value = 0
        _comboMultiplier.value = 1
        _isFeverMode.value = false

        // Determine if it is a Boss Level (Every 5 levels is a Boss Challenge!)
        val level = userProgress.value?.currentLevel ?: 1
        val isBoss = level > 0 && level % 5 == 0
        _isBossChallenge.value = isBoss
        if (isBoss) {
            _bossHp.value = 8
            _bossMaxHp.value = 8
            com.example.ui.EgyptianToneEngine.playBossIntroSound()
            triggerToast("🔴 BOSS FIGHT: DEFEAT THE SACRED SPHINX GUARD!")
        }

        _gameState.value = GameState.Playing

        // Start gameplay loop
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (_gameTimeLeftLeft.value > 0 && _playerLives.value > 0) {
                delay(1000)
                _gameTimeLeftLeft.value -= 1
                
                // Active Spawner check
                spawnTimerCheck()
            }
            if (_playerLives.value <= 0) {
                // Trigger Revive popup once
                _gameState.value = GameState.ReviveOffer
            } else {
                commitChallengeComplete()
            }
        }

        physicsJob?.cancel()
        physicsJob = viewModelScope.launch {
            while (_gameState.value == GameState.Playing) {
                delay(40) // 25 FPS update
                updateFallingPhysics()
                decaySparks()
            }
        }
    }

    fun pauseChallenge() {
        if (_gameState.value == GameState.Playing) {
            _gameState.value = GameState.Paused
            physicsJob?.cancel()
        }
    }

    fun resumeChallenge() {
        if (_gameState.value == GameState.Paused) {
            _gameState.value = GameState.Playing
            physicsJob = viewModelScope.launch {
                while (_gameState.value == GameState.Playing) {
                    delay(40)
                    updateFallingPhysics()
                    decaySparks()
                }
            }
        }
    }

    // Interstitial tracking
    private var completedRoundsSinceLastAd = 0

    fun handleRoundFinishedInterstitial(activity: Activity, onFinished: () -> Unit) {
        val prog = userProgress.value
        if (prog?.adsRemoved == true) {
            onFinished()
            return
        }
        completedRoundsSinceLastAd++
        if (completedRoundsSinceLastAd >= 3) {
            completedRoundsSinceLastAd = 0
            AdManager.showInterstitial(activity) {
                onFinished()
            }
        } else {
            onFinished()
        }
    }

    val isAdLoading = MutableStateFlow(false)

    fun triggerReviveAdReward(activity: Activity) {
        val prog = userProgress.value
        if (prog?.adsRemoved == true) {
            grantRevive()
            return
        }
        isAdLoading.value = true
        AdManager.showRewarded(
            activity = activity,
            onRewardEarned = {
                grantRevive()
            },
            onAdClosed = {
                isAdLoading.value = false
            },
            onAdFailed = { error ->
                isAdLoading.value = false
                triggerToast("Sacred Ad failed: $error. Sandbox mercy granted!")
                grantRevive() // Never block gameplay if ad fails to load!
            }
        )
    }

    private fun grantRevive() {
        _playerLives.value = 2 // Grant 2 lives and resume
        _gameState.value = GameState.Playing
        triggerToast("Resurrected with 2 Hearts! ⚔️")
        
        // Re-trigger game loop
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (_gameTimeLeftLeft.value > 0 && _playerLives.value > 0) {
                delay(1000)
                _gameTimeLeftLeft.value -= 1
                spawnTimerCheck()
            }
            if (_playerLives.value <= 0) {
                commitChallengeGameOver()
            } else {
                commitChallengeComplete()
            }
        }
        // Re-trigger physics
        physicsJob?.cancel()
        physicsJob = viewModelScope.launch {
            while (_gameState.value == GameState.Playing) {
                delay(40)
                updateFallingPhysics()
                decaySparks()
            }
        }
    }

    fun doubleEndCoinsReward(activity: Activity) {
        val extra = _coinsCollectedInRound.value
        if (extra <= 0) return
        isAdLoading.value = true
        AdManager.showRewarded(
            activity = activity,
            onRewardEarned = {
                viewModelScope.launch {
                    repository.addCoins(extra)
                    _coinsCollectedInRound.value += extra
                    triggerToast("\uD83C\uDF1F Ad Watched! Coins doubled! Received extra +$extra coins!")
                }
            },
            onAdClosed = {
                isAdLoading.value = false
            },
            onAdFailed = { error ->
                isAdLoading.value = false
                triggerToast("Reward Ad failed: $error")
            }
        )
    }

    fun triggerExtraCoinsReward(activity: Activity) {
        isAdLoading.value = true
        AdManager.showRewarded(
            activity = activity,
            onRewardEarned = {
                viewModelScope.launch {
                    repository.addCoins(100)
                    triggerToast("\uD83D\uDCA3 Ad Watched! +100 Coins added to purse!")
                }
            },
            onAdClosed = {
                isAdLoading.value = false
            },
            onAdFailed = { error ->
                isAdLoading.value = false
                triggerToast("Reward Ad failed: $error")
            }
        )
    }

    fun spinLuckyWheelWithAd(activity: Activity) {
        if (_isWheelSpinning.value) return
        isAdLoading.value = true
        AdManager.showRewarded(
            activity = activity,
            onRewardEarned = {
                viewModelScope.launch {
                    _isWheelSpinning.value = true
                    _lastWheelReward.value = null
                    
                    delay(2200)

                    val spinType = Random.nextInt(100)
                    val (text, rewardCoins) = when {
                        spinType < 40 -> Pair("50 Gold Coins!", 50)
                        spinType < 70 -> Pair("100 Rich Coins \uD83C\uDFC6", 100)
                        spinType < 85 -> Pair("Grand Prize! 250 Coins \uD83D\uDC8E", 250)
                        spinType < 95 -> Pair("Golden Gem! 150 Coins", 150)
                        else -> Pair("Bonus Try! 10 Coins refund", 10)
                    }

                    repository.addCoins(rewardCoins)
                    _lastWheelReward.value = text
                    _isWheelSpinning.value = false
                    triggerToast("Lucky Spin Won: $text")
                }
            },
            onAdClosed = {
                isAdLoading.value = false
            },
            onAdFailed = { error ->
                isAdLoading.value = false
                triggerToast("Ad failed to load: $error")
            }
        )
    }

    fun skipReviveAndDie() {
        _gameState.value = GameState.GameOver(victory = false)
        commitChallengeGameOver()
    }

    // High-performance particle explosions
    fun spawnExplosion(x: Float, y: Float, count: Int, character: String, color: Long, size: Float = 26f) {
        val current = _recentTapSparks.value.toMutableList()
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2.0 * Math.PI
            val speed = Random.nextFloat() * 10f + 4f
            val dx = (Math.cos(angle) * speed).toFloat()
            val dy = (Math.sin(angle) * speed).toFloat() - 3f
            current.add(
                TapSpark(
                    id = UUID.randomUUID().toString(),
                    x = x,
                    y = y,
                    dx = dx,
                    dy = dy,
                    text = character,
                    color = color,
                    size = size,
                    lifespan = 1.0f
                )
            )
        }
        _recentTapSparks.value = current
    }

    private fun spawnTimerCheck() {
        if (_gameState.value != GameState.Playing) return
        val list = _fallingTargets.value.toMutableList()

        // Check if Boss level and boss target is not spawned yet and not active in list
        if (_isBossChallenge.value) {
            val bossPresent = list.any { it.type == TargetType.BOSS }
            // Spawn boss if it is not present and hp > 0
            if (!bossPresent && _bossHp.value > 0) {
                list.add(
                    FallingTarget(
                        id = "sphinx_boss_unit",
                        type = TargetType.BOSS,
                        xOffsetPerc = 0.38f,
                        yOffsetPerc = 0.05f,
                        speed = 0.003f, // floats down very slowly
                        pointsValue = 800,
                        coinsValue = 100
                    )
                )
                _fallingTargets.value = list
                return
            }
        }

        val count = Random.nextInt(1, 4)

        // Difficulty multipliers
        val speedMultiplier = when (userProgress.value?.difficulty) {
            "Hard" -> 1.30f
            "Expert" -> 1.65f
            else -> 1.0f // Normal
        }

        for (i in 0 until count) {
            val xPerc = Random.nextFloat() * 0.85f + 0.05f 
            val typeVal = Random.nextInt(100)
            
            // Under Fever Mode, only Gems and Scarabs spawn (no curse stones!)
            val type = if (_isFeverMode.value) {
                if (typeVal < 40) TargetType.GEM else TargetType.SCARAB
            } else {
                when {
                    typeVal < 15 -> TargetType.GEM
                    typeVal < 60 -> TargetType.SCARAB
                    else -> TargetType.BLACK_STONE
                }
            }

            val speed = when(type) {
                TargetType.GEM -> (Random.nextFloat() * 0.04f + 0.05f) * speedMultiplier
                TargetType.SCARAB -> (Random.nextFloat() * 0.02f + 0.03f) * speedMultiplier
                TargetType.BLACK_STONE -> (Random.nextFloat() * 0.03f + 0.04f) * speedMultiplier
                TargetType.BOSS -> 0.003f
            }
            list.add(
                FallingTarget(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    xOffsetPerc = xPerc,
                    yOffsetPerc = 0.0f,
                    speed = speed,
                    pointsValue = if (type == TargetType.GEM) 50 else if (type == TargetType.SCARAB) 10 else -15,
                    coinsValue = if (type == TargetType.GEM) 5 else if (type == TargetType.SCARAB) 1 else 0
                )
            )
        }
        _fallingTargets.value = list
    }

    private fun updateFallingPhysics() {
        if (_gameState.value != GameState.Playing) return
        val current = _fallingTargets.value
        val nextList = mutableListOf<FallingTarget>()

        var hitBottomPenalty = false

        for (target in current) {
            val nextY = target.yOffsetPerc + target.speed
            if (nextY >= 1.05f) {
                // target missed or fell off bottom
                if (target.type == TargetType.SCARAB || target.type == TargetType.GEM) {
                    _score.value = (_score.value - 2).coerceAtLeast(0)
                    hitBottomPenalty = true
                }
            } else {
                nextList.add(target.copy(yOffsetPerc = nextY))
            }
        }

        // If you let a golden scarab or sparkling gem fall off, combo is broken!
        if (hitBottomPenalty) {
            if (_comboCount.value > 0) {
                _comboCount.value = 0
                _comboMultiplier.value = 1
                _isFeverMode.value = false
                triggerToast("⚡ Missed Target! Combo Reset! ⚡")
            }
        }

        // Scarab physical trails - spawn subtle sparkles behind active scarabs / gems
        val sparks = _recentTapSparks.value.toMutableList()
        nextList.forEach { target ->
            if (Random.nextInt(6) == 0) { // 16% chance per tick to drop a trail element
                val itemX = target.xOffsetPerc * 350f // approximate virtual width
                val itemY = target.yOffsetPerc * 500f // approximate virtual height
                sparks.add(
                    TapSpark(
                        id = UUID.randomUUID().toString(),
                        x = itemX,
                        y = itemY,
                        dx = 0f,
                        dy = 1f, // drop slightly down
                        text = if (target.type == TargetType.GEM) "✨" else "🔶",
                        color = if (target.type == TargetType.GEM) 0xFF00FFFF else 0xFFFFA500,
                        size = 14f,
                        lifespan = 0.7f
                    )
                )
            }
        }
        _recentTapSparks.value = sparks
        _fallingTargets.value = nextList
    }

    private fun decaySparks() {
        val currentSparks = _recentTapSparks.value
        if (currentSparks.isEmpty()) return
        val nextSparks = currentSparks.map {
            it.copy(
                x = it.x + it.dx,
                y = it.y + it.dy,
                dy = it.dy + 0.6f, // gravity pull down!
                lifespan = it.lifespan - 0.05f
            )
        }.filter { it.lifespan > 0 }
        _recentTapSparks.value = nextSparks
    }

    // Tap Target
    fun tapTarget(id: String, tapX: Float, tapY: Float) {
        if (_gameState.value != GameState.Playing) return
        val targets = _fallingTargets.value
        val item = targets.find { it.id == id } ?: return

        // Trigger physical haptic response
        triggerVibration()

        val prog = userProgress.value
        val skinSparkColor = when(prog?.equippedSkin) {
            "Golden Scarab Gear" -> 0xFFFFD700
            "Anubis Priest Cloak" -> 0xFF9932CC
            "Horus Sun Armor" -> 0xFF87CEEB
            "Pharaoh Regal Gown" -> 0xFFFFA500
            else -> 0xFFFCECD0 // classic light sandstone
        }

        if (item.type == TargetType.BOSS) {
            // Hit boss target!
            _bossHp.value -= 1
            triggerScreenShake()
            com.example.ui.EgyptianToneEngine.playBossHitSound()

            // Flash elegant damage sparks
            spawnExplosion(tapX, tapY, 6, "💥", skinSparkColor, size = 28f)
            spawnExplosion(tapX, tapY, 4, "🪙", skinSparkColor, size = 20f)

            if (_bossHp.value <= 0) {
                // Boss slayed! Remove boss item
                _fallingTargets.value = targets.filter { it.id != id }
                
                val bountyPoints = item.pointsValue
                val bountyCoins = item.coinsValue
                _score.value += bountyPoints
                _coinsCollectedInRound.value += bountyCoins

                // Rare relic drops bonus: 30 gems gift!
                _gemsTapped.value += 15
                _coinsCollectedInRound.value += 50

                // Trigger huge victory explosion & sound
                triggerScreenShake()
                spawnExplosion(tapX, tapY, 15, "🪙", 0xFFFFD700, size = 24f)
                spawnExplosion(tapX, tapY, 10, "💎", 0xFF00FFFF, size = 24f)
                spawnExplosion(tapX, tapY, 12, "👑", 0xFFFFA500, size = 28f)

                com.example.ui.EgyptianToneEngine.playVictorySound()
                triggerToast("🏆 SPHINX GUARD SLAIN! Bounty: +$bountyPoints pts / +$bountyCoins Coins!")
                
                // End game immediately with victory!
                commitChallengeComplete()
            } else {
                // Bounce Boss back up in playfield
                _fallingTargets.value = targets.map {
                    if (it.id == id) it.copy(yOffsetPerc = (it.yOffsetPerc - 0.22f).coerceAtLeast(0.05f)) else it
                }
                triggerToast("BOSS HIT! HP: ${_bossHp.value}/${_bossMaxHp.value} ✴️")
            }
            return
        }

        // Remove item from falling state
        _fallingTargets.value = targets.filter { it.id != id }

        if (item.type == TargetType.BLACK_STONE) {
            // Hit Cursed stone! Deduct 1 heart, reset combo
            _playerLives.value -= 1
            _comboCount.value = 0
            _comboMultiplier.value = 1
            _isFeverMode.value = false

            triggerScreenShake()
            com.example.ui.EgyptianToneEngine.playCursedStoneSound()
            spawnExplosion(tapX, tapY, 10, "💀", 0xFF8B0000, size = 26f) // Dark skull explosion
            
            triggerToast("✴️ Hit Cursed Stone! Lost 1 Heart!")
            if (playerLives.value <= 0) {
                // Intercept game loop to offer revive
                _gameState.value = GameState.ReviveOffer
            }
        } else {
            // Hit Golden item! Increase combo, calculate points and coins
            _comboCount.value += 1

            // Fever Mode thresholds
            if (_comboCount.value >= 10 && !_isFeverMode.value) {
                _isFeverMode.value = true
                triggerToast("🔥 FEVER MODE ACTIVE! DOUBLE COINS! 🔥")
                triggerScreenShake()
                com.example.ui.EgyptianToneEngine.playVictorySound()
            }

            // Screen shake on streak threshold
            if (_comboCount.value >= 5) {
                triggerScreenShake()
            }

            // Calculate multiplier
            _comboMultiplier.value = (1 + _comboCount.value / 5).coerceAtMost(5)

            // Dynamic points calculation
            val bonusPoints = item.pointsValue * _comboMultiplier.value
            _score.value += bonusPoints

            // Double coins during Fever Mode!
            val rawCoins = item.coinsValue
            val coinReward = if (_isFeverMode.value) rawCoins * 2 else rawCoins
            _coinsCollectedInRound.value += coinReward

            // Sparkle Particle explosions based on target type!
            if (item.type == TargetType.GEM) {
                _gemsTapped.value += 1
                com.example.ui.EgyptianToneEngine.playGemSound()
                com.example.ui.EgyptianToneEngine.playComboSound(_comboCount.value)
                spawnExplosion(tapX, tapY, 8, "💎", 0xFF00FFFF, size = 24f) // Blue gem particles!
                spawnExplosion(tapX, tapY, 5, "✨", skinSparkColor, size = 20f)
                
                triggerToast("Gem Captured! +${coinReward} Coins 💎 (x${_comboMultiplier.value} Combo!)")
            } else {
                _scarabsTapped.value += 1
                com.example.ui.EgyptianToneEngine.playCoinSound()
                com.example.ui.EgyptianToneEngine.playComboSound(_comboCount.value)
                spawnExplosion(tapX, tapY, 8, "🪙", 0xFFFFD700, size = 24f) // Gold coin particles!
                spawnExplosion(tapX, tapY, 5, "🪲", skinSparkColor, size = 20f) // Scarab particles!

                // Rare Relic Drops: 5% chance to drop bonus rare scarabs/relics
                if (Random.nextInt(100) < 6) {
                    _coinsCollectedInRound.value += 15
                    spawnExplosion(tapX, tapY, 6, "⚜️", 0xFFFFD700, size = 26f) // Relic symbol
                    triggerToast("✨ Found Rare Relic! Bonus +15 Coins!")
                } else {
                    triggerToast("Scarab Saved! +${coinReward} Coin 🪲 (x${_comboMultiplier.value} Combo!)")
                }
            }
        }
    }

    private fun commitChallengeComplete() {
        _gameState.value = GameState.GameOver(victory = true)
        gameLoopJob?.cancel()
        physicsJob?.cancel()

        viewModelScope.launch {
            val rawCoins = _coinsCollectedInRound.value
            val levelResult = repository.finishTempleChallenge(
                scarabsTapped = _scarabsTapped.value,
                coinsEarned = rawCoins,
                levelCompleted = true,
                templeName = _selectedTemple.value.displayName
            )

            // Auto log highscore to leaderboard list
            val prog = userProgress.value
            val pName = prog?.equippedSkin ?: "Grand Archaeologist"
            repository.submitScore(pName, _score.value, _selectedTemple.value.displayName)

            if (levelResult.leveledUp) {
                triggerToast("\uD83C\uDF89 LEVELED UP to Level ${levelResult.newLevel}! \uD83C\uDFC5")
                if (levelResult.awardedMysteryChest) {
                    triggerToast("Mystery Chest Earned! Unlock it for massive riches!")
                }
            }
        }
    }

    private fun commitChallengeGameOver() {
        _gameState.value = GameState.GameOver(victory = false)
        gameLoopJob?.cancel()
        physicsJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        physicsJob?.cancel()
        com.example.ui.EgyptianToneEngine.stopBgm()
    }
}

// Helpers
sealed interface GameState {
    object Idle : GameState
    object Playing : GameState
    object Paused : GameState
    object ReviveOffer : GameState
    data class GameOver(val victory: Boolean) : GameState
}

enum class TargetType {
    SCARAB, GEM, BLACK_STONE, BOSS
}

data class FallingTarget(
    val id: String,
    val type: TargetType,
    val xOffsetPerc: Float,
    val yOffsetPerc: Float,
    val speed: Float,
    val pointsValue: Int,
    val coinsValue: Int
)

data class TapSpark(
    val id: String = java.util.UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val text: String = "✨",
    val color: Long = 0xFFFFD700,
    val lifespan: Float = 1.0f,
    val size: Float = 24f
)

enum class Temple(val displayName: String, val unlockCost: Int, val description: String, val bgHex: Long) {
    TEMPLE_OF_RA("Temple of Ra", 0, "Seek the light of the Sun Lord in the golden dunes.", 0xFF1C1402),
    ANUBIS_GATE("Anubis Gate", 300, "Navigate the darkness where shadows rise and souls weight.", 0xFF0D0D14),
    HORUS_SKY("Horus Sky", 700, "Soar high in the heavenly canopy of Horus.", 0xFF00223E),
    NILE_TREASURE("Nile Treasure", 1200, "Float down the mystical river carrying endless pharaonic treasures.", 0xFF00382B),
    PHARAOH_VAULT("Pharaoh Vault", 2500, "The deepest sarcophagus filled with golden amulets.", 0xFF140F00)
}

data class SkinItem(
    val name: String,
    val description: String,
    val costCoins: Int,
    val displayIcon: String
)

val SKINS_LIST = listOf(
    SkinItem("Classic Explorer", "Default archaeologist leather jack.", 0, "person"),
    SkinItem("Golden Scarab Gear", "Shimmers like pure gold dust trail.", 300, "blur_on"),
    SkinItem("Anubis Priest Cloak", "Draws dark purple magical forcefields.", 500, "wb_shadows"),
    SkinItem("Horus Sun Armor", "Glows with the magnificent sky turquoise tail.", 1000, "light_mode"),
    SkinItem("Pharaoh Regal Gown", "The ultimate state-gown with crown of kings.", 2000, "military_tech")
)

/**
 * Custom Factory
 */
class GameViewModelFactory(
    private val application: Application,
    private val repository: GameRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
