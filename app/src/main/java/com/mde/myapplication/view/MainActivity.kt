package com.mde.myapplication.view

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.mde.myapplication.R
import com.mde.myapplication.presenter.HomeView
import com.mde.myapplication.presenter.PlayerPresenter
import com.mde.myapplication.presenter.PlayerPresenterImpl
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), HomeView, TrackAdapter.OnPlayClickListener {

    //for activity result
    private val REQUEST_CODE = 1337

    private val presenter: PlayerPresenter = PlayerPresenterImpl(this)
    private val adapter = TrackAdapter(mutableListOf(), this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter.authenticateClient()

        //init views
        player_next.setOnClickListener { presenter.next() }
        player_previous.setOnClickListener { presenter.previous() }
        player_play_pause.setOnClickListener { presenter.playOrPause() }

        home_recycler.adapter = adapter
        home_recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        super.onActivityResult(requestCode, resultCode, intent)

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            presenter.configurePlayer(this, AuthenticationClient.getResponse(resultCode, intent))
        }
    }

    override fun displayHomeTitle(userName: String) {
        home_title.text = resources.getString(R.string.home_title, userName)
    }
    override fun displayAuthentication(request: AuthenticationRequest) {
        //open Spotify Login activity
        AuthenticationClient.openLoginActivity(this@MainActivity, REQUEST_CODE, request)
    }

    override fun displayPlaylist(tracks: List<TrackAdapter.TrackItem>, position: Int) {
        //update track adapter
        adapter.updateListItem(tracks)
        //scroll to the new position
        home_recycler.smoothScrollToPosition(position)
    }

    override fun displayError(error: String?) {
        player_error_tv.text = error
    }

    override fun updateProgress(progress: Int, max: Int) {
        player_progress.progress = progress
        player_progress.max = max
        player_maxMs.text = formatProgress(max.toLong())
        player_currentMs.text = formatProgress(progress.toLong())
    }

    private fun formatProgress(ms: Long): String {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(ms),
                TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms)))
    }

    override fun updatePlayerState(isPlaying: Boolean) {
        //playOrPause / pause icon
        player_play_pause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun onTrackClicked(trackId: String) {
        presenter.playOrPause(trackId)
    }
}

