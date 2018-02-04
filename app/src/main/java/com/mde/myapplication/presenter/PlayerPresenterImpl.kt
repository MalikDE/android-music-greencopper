package com.mde.myapplication.presenter

import android.content.Context
import android.os.Handler
import android.util.Log
import com.mde.myapplication.*
import com.mde.myapplication.utils.Android
import com.mde.myapplication.utils.TAG
import com.mde.myapplication.view.TrackAdapter
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import com.spotify.sdk.android.player.*
import kaaes.spotify.webapi.android.SpotifyApi
import kaaes.spotify.webapi.android.SpotifyService
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

class PlayerPresenterImpl(private val view: HomeView) : PlayerPresenter, Player.NotificationCallback, ConnectionStateCallback {

    /**
     * This is a Spotify web service mapping for Android. This is required to request other info that
     * Spotify SDK does not provide (UserPrivate, Playlist tracks...)
     */
    private lateinit var service: SpotifyService

    //Spotify player
    private lateinit var player: Player

    //A list of tracks
    private val trackItems = mutableListOf<TrackAdapter.TrackItem>()

    /**
     * Polling is for retrieving track progress while playing
     */
    private val pollingTime = 500L
    private val handler = Handler()
    private val progressPolling = object : Runnable {
        override fun run() {
            val max = player.metadata?.currentTrack?.durationMs
            val current = player.playbackState.positionMs
            val isPlaying = player.playbackState?.isPlaying
            if (max != null && isPlaying != null && isPlaying) {
                view.updateProgress(current.toInt(), max.toInt())
            }
            //iteration
            handler.postDelayed(this, pollingTime)
        }
    }

    //not used but required
    private val operationCallback = object : Player.OperationCallback {
        override fun onSuccess() {}
        override fun onError(error: Error) = logError("ERROR:" + error)
    }

    override fun authenticateClient() {
        val request = AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(arrayOf("user-read-private", "playlist-read", "playlist-read-private", "streaming"))
                .build()
        view.displayAuthentication(request)
    }

    override fun configurePlayer(context: Context, response: AuthenticationResponse) {
        when (response.type) {
            AuthenticationResponse.Type.TOKEN -> {
                val playerConfig = Config(context, response.accessToken, CLIENT_ID)
                service = SpotifyApi().setAccessToken(response.accessToken).service
                player = Spotify.getPlayer(playerConfig, this, object : SpotifyPlayer.InitializationObserver {
                    override fun onInitialized(spotifyPlayer: SpotifyPlayer) {
                        player.apply {
                            addConnectionStateCallback(this@PlayerPresenterImpl)
                            addNotificationCallback(this@PlayerPresenterImpl)
                        }
                    }

                    override fun onError(throwable: Throwable) {
                        logError("Could not initialize player: ${throwable.message}")
                    }
                })

                launch(Android){
                    val user = async { service.me }.await()
                    view.displayHomeTitle(user.display_name)
                }

                //Fetch use tops
                fetchUserTops()
            }
        // Auth flow returned an logError
            AuthenticationResponse.Type.ERROR -> Log.e(TAG(), "Auth logError: ${response.error}")

        // Most likely auth flow was cancelled
            else -> logError("Auth result: ${response.type} ")
        }
    }

    private fun fetchUserTops() {
        //launch a new coroutine. Any callback will call on the main thread
        launch(Android) {
            //fetch top tracks synchronously
            val result = async { service.topTracks.items }.await()

            trackItems.clear()
            //map Track to custom TrackItem
            trackItems.addAll(result.map { TrackAdapter.TrackItem(TRACK_PREFIX + it.id, it.name, it.artists.joinToString { it.name }, it.album?.images?.first()?.url) })

            //Notify the view
            view.displayPlaylist(trackItems, 0)
        }
    }

    override fun playOrPause() {
        val isPlaying = player.playbackState.isPlaying
        if (isPlaying) {
            player.pause(operationCallback) //pause
        } else {
            val currentTrackId = player.metadata?.currentTrack?.uri
            if (currentTrackId == null || currentTrackId.isEmpty()) {
                playOrPause(trackItems.first().id) //start the first track of the playlist
            } else {
                playOrPause(currentTrackId) //start or pause the current track
            }
        }
    }

    override fun playOrPause(uri: String) {
        val spotifyUri = if (!uri.contains(TRACK_PREFIX)) TRACK_PREFIX + uri else uri
        val currentTrackId = player.metadata?.currentTrack?.uri
        if (player.playbackState.isPlaying && spotifyUri == currentTrackId) {
            player.pause(operationCallback) //pause
            handler.removeCallbacks(progressPolling)
        } else {
            if (uri != currentTrackId) {
                player.playUri(operationCallback, spotifyUri, 0, 0) //start new track
            } else {
                player.resume(operationCallback) //resume
            }
            handler.removeCallbacks(progressPolling)
            handler.postDelayed(progressPolling, pollingTime)
        }
    }

    private fun updateList(isPlaying: Boolean) {
        val currentTrackId = player.metadata?.currentTrack?.uri
        if (currentTrackId != null) {
            //update item playing state
            var index = 0
            for (i in 0 until trackItems.size) {
                val item = trackItems[i]
                item.isPlaying = isPlaying && item.id == currentTrackId
                if (item.id == currentTrackId) index = i //save index position
            }
            //notify view
            view.displayPlaylist(trackItems, index)
        }
    }

    override fun next() {
        val currentTrackId = player.metadata?.currentTrack?.uri
        val index = trackItems.indexOfFirst { it.id == currentTrackId }
        if (trackItems.size > index + 1) {
            playOrPause(trackItems[index + 1].id) //next track
        } else {
            playOrPause(trackItems[0].id) //go to start of playlist
        }
    }

    override fun previous() {
        val currentTrackId = player.metadata?.currentTrack?.uri
        val index = trackItems.indexOfFirst { it.id == currentTrackId }
        if (index - 1 >= 0) {
            playOrPause(trackItems[index - 1].id) //previous track
        } else {
            playOrPause(trackItems[trackItems.size - 1].id) //go to end of playlist
        }
    }


    override fun onDestroy() = Spotify.destroyPlayer(this)

    /**
     * Some callback that I don't use right now
     */
    override fun onLoggedOut() {}
    override fun onLoggedIn() {}
    override fun onConnectionMessage(error: String?) = logError(error)
    override fun onLoginFailed(error: Error?) = logError(error?.name)
    override fun onTemporaryError() = logError("onTemporaryError")
    override fun onPlaybackError(error: Error?) = logError(error?.name)


    /**
     * Callback fired every time player changes
     */
    override fun onPlaybackEvent(event: PlayerEvent?) {
        view.updatePlayerState(player.playbackState.isPlaying)

        when (event) {
            PlayerEvent.kSpPlaybackNotifyMetadataChanged -> Log.d(TAG(), player.metadata?.currentTrack?.uri)
            PlayerEvent.kSpPlaybackNotifyTrackChanged -> updateList(true)
            PlayerEvent.kSpPlaybackNotifyPlay -> updateList(true)
            PlayerEvent.kSpPlaybackNotifyPause -> updateList(false)
            PlayerEvent.kSpPlaybackNotifyTrackDelivered -> next()
        }
    }

    private fun logError(error: String?) {
        Log.e(TAG(), error)
        view.displayError(error)
    }
}