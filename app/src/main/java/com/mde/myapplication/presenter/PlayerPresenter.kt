package com.mde.myapplication.presenter

import android.content.Context
import com.spotify.sdk.android.authentication.AuthenticationResponse

/**
 * Manager class for Spotify client and Player
 */
interface PlayerPresenter {

    /**
     * Authenticate client
     */
    fun authenticateClient()

    /**
     * Configure player : must be call with an [AuthenticationResponse] after [onActivityResult]
     */
    fun configurePlayer(context: Context, response: AuthenticationResponse)

    /**
     * Play the first track, or play/pause the current track
     */
    fun playOrPause()

    /**
     * Play a specific track
     */
    fun playOrPause(uri: String)

    /**
     * Next track in the playlist order. Jump to first track at the end.
     */
    fun next()

    /**
     * Previous track in the playlist order. Jump to last track at the start.
     */
    fun previous()

    /**
     * IMPORTANT : call this when destroying your activity
     */
    fun onDestroy()
}