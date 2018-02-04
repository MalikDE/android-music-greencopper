package com.mde.myapplication.presenter

import com.mde.myapplication.view.TrackAdapter
import com.spotify.sdk.android.authentication.AuthenticationRequest

interface HomeView {

    /**
     * When this is call, you must open a Spotify Login activity to retrieve a Token
     */
    fun displayAuthentication(request: AuthenticationRequest)

    /**
     * Display home title
     */
    fun displayHomeTitle(userName: String)

    /**
     * Display a list of [TrackAdapter.TrackItem] that can be provided to an Adapter
     */
    fun displayPlaylist(tracks: List<TrackAdapter.TrackItem>, position: Int)

    /**
     * Update track progress
     * @param progress: current progress in ms
     * @param max: max progress in ms
     */
    fun updateProgress(progress: Int, max: Int)

    /**
     * Update play/pause state
     */
    fun updatePlayerState(isPlaying: Boolean)

    fun displayError(error: String?)
}