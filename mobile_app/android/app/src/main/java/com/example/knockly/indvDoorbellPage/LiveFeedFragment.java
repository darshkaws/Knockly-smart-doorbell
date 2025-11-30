package com.example.knockly.indvDoorbellPage;

import static com.example.knockly.indvDoorbellPage.IndvDoorbellActivity.hostPiFeed;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.ui.PlayerView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.knockly.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LiveFeedFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
@UnstableApi
public class LiveFeedFragment extends Fragment {

    // the fragment initialization parameters for doorbellID
    private static final String ARG_DOORBELL_ID = "000000";

    private String mDoorbellID;
    private final long initialTime = System.currentTimeMillis() - 3000;
    private boolean liveFeedExpanded = false;
    private FragmentCallback callback;
    private final DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                    250,   // minBufferMs
                    500,   // maxBufferMs
                    50,    // bufferForPlaybackMs
                    50     // bufferForPlaybackAfterRebufferMs
            )
            .build();
    private RtspMediaSource mediaSourceFactory;
    private MediaItem mediaItem;
    private ExoPlayer exoPlayer;
    private ExoPlayer nextPlayer;
    private PlayerView playerView;
    private PlayerView playerView2;
    private boolean isPlayer1Active = true;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());

    private EditText editTextDate;
    private final Handler timestampHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateTimestampRunnable = new Runnable() {
        @Override
        public void run() {
            updateEditTextDate();
            timestampHandler.postDelayed(this, 1000); // update every 1 second
        }
    };

    // Interface to get functions from main activity
    public interface FragmentCallback{
        void expandLiveFeed();
        void shrinkLiveFeed();
    }

    public LiveFeedFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param doorbellID Parameter 1.
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static LiveFeedFragment newInstance(String doorbellID) {
        LiveFeedFragment fragment = new LiveFeedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOORBELL_ID, doorbellID);
        fragment.setArguments(args);
        return fragment;
    }

    // Send activity functions to interface
    @Override
    public void onAttach(@NonNull Context context){
        super.onAttach(context);

        try{
            callback = (FragmentCallback) context;
        }
        catch(ClassCastException e){
            // Error that will occur if activity does not implement the interface methods
            throw new ClassCastException(context
                    + " must implement FragmentCallback interface");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDoorbellID = getArguments().getString(ARG_DOORBELL_ID);
        }
    }

    private View view;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_live_feed, container, false);

        playerView = view.findViewById(R.id.playerView);
        playerView2 = view.findViewById(R.id.playerView2);
        editTextDate = view.findViewById(R.id.editTextDate);
        TextView noInputText = view.findViewById(R.id.noInputText);
        TextView liveText = view.findViewById(R.id.textView);

        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z", Locale.getDefault());
        editTextDate.setText(sdf.format(new Date(initialTime)));

        if (!hostPiFeed.isBlank()) {
            noInputText.setVisibility(View.GONE);  // Hide the message
            playerView.setVisibility(View.VISIBLE);
            playerView2.setVisibility(View.INVISIBLE);
            liveText.setVisibility(View.VISIBLE);
            String rtspUri = "rtsp://" + hostPiFeed + ":8554/doorbell";

            initializePlayer(rtspUri);
            timestampHandler.post(updateTimestampRunnable);
            scheduleReconnect(rtspUri);
        } else {
            noInputText.setVisibility(View.VISIBLE);  // Show the message
            playerView.setVisibility(View.GONE);
            playerView2.setVisibility(View.GONE);
            liveText.setVisibility(View.GONE);
        }

        ImageButton changeViewButton = view.findViewById(R.id.changeViewButton);

        changeViewButton.setOnClickListener(view -> {
            if (liveFeedExpanded){
                shrinkFeed(changeViewButton);
            }
            else{
                expandFeed(changeViewButton);
            }
            liveFeedExpanded = !liveFeedExpanded;
        });

        return view;
    }

    private void scheduleReconnect(String streamUrl) {
        long RECONNECT_INTERVAL_MS = 45000;

        reconnectHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isDetached() && !isRemoving()) {
                    reconnectStream(streamUrl);  // on main thread
                    reconnectHandler.postDelayed(this, RECONNECT_INTERVAL_MS);
                }
            }
        }, RECONNECT_INTERVAL_MS);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer(String streamUrl) {

        exoPlayer = new ExoPlayer.Builder(requireContext())
                .setLoadControl(loadControl)
                .setRenderersFactory(new DefaultRenderersFactory(requireContext())
                        .setEnableDecoderFallback(true) // safer fallback
                        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER))
                .build();

        playerView.setPlayer(exoPlayer);

        playerView.setUseController(false);

        mediaSourceFactory = new RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .createMediaSource(MediaItem.fromUri(streamUrl));
        exoPlayer.setMediaSource(mediaSourceFactory);

        mediaItem = new MediaItem.Builder()
                .setUri(streamUrl)
                .setLiveConfiguration(
                        new MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(40)
                                .build()
                )
                .build();
        exoPlayer.setMediaItem(mediaItem);

        exoPlayer.prepare();
        exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC);
        exoPlayer.setPlayWhenReady(true);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void reconnectStream(String streamUrl) {
        if (nextPlayer != null) {
            nextPlayer.release();
            nextPlayer = null;
        }

        nextPlayer = new ExoPlayer.Builder(requireContext())
                .setLoadControl(loadControl)
                .setRenderersFactory(new DefaultRenderersFactory(requireContext())
                        .setEnableDecoderFallback(true) // safer fallback
                        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER))
                .build();

        mediaSourceFactory = new RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .createMediaSource(MediaItem.fromUri(streamUrl));
        nextPlayer.setMediaSource(mediaSourceFactory);

        mediaItem = new MediaItem.Builder()
                .setUri(streamUrl)
                .setLiveConfiguration(
                        new MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(55)
                                .build()
                )
                .build();
        nextPlayer.setMediaItem(mediaItem);

        nextPlayer.prepare();
        nextPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC);

        nextPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    swapPlayers();
                }
            }
        });
    }

    private void swapPlayers() {
        PlayerView activeView = isPlayer1Active ? playerView : playerView2;
        PlayerView nextView = isPlayer1Active ? playerView2 : playerView;

        nextView.setAlpha(0f);
        nextView.setVisibility(View.VISIBLE);
        nextView.setPlayer(nextPlayer);
        nextPlayer.setPlayWhenReady(true);

        if (nextPlayer.isPlaying()) {
            nextView.animate().alpha(1f).setDuration(50).start();
            activeView.animate().alpha(0f).setDuration(50).withEndAction(() -> {
                activeView.setVisibility(View.GONE);
                if (exoPlayer != null) exoPlayer.release();
                exoPlayer = nextPlayer;
                nextPlayer = null;
                isPlayer1Active = !isPlayer1Active;
            }).start();
        }
    }

    private void updateEditTextDate() {
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            long offset = 3000;
            long currentSystemTime = System.currentTimeMillis() - offset;
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z", Locale.getDefault());
            String formattedTime = sdf.format(currentSystemTime);

            editTextDate.setText(formattedTime);
        }
    }

    private void shrinkFeed(ImageButton changeViewButton) {
        callback.shrinkLiveFeed();
        changeViewButton.setImageResource(R.drawable.fullscreen_arrows);
    }

    private void expandFeed(ImageButton changeViewButton) {
        callback.expandLiveFeed();
        changeViewButton.setImageResource(R.drawable.minimize_arrows);
    }

    public void setToggleButtonOn(){
        ToggleButton button = view.findViewById(R.id.toggleButton);
        button.setChecked(true);
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light));
    }

    public void setToggleButtonOff(){
        ToggleButton button = view.findViewById(R.id.toggleButton);
        button.setChecked(false);
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));
    }

    // Prevent memory leaks by removing callback when fragment is destroyed
    @Override
    public void onDetach(){
        super.onDetach();
        callback = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        reconnectHandler.removeCallbacksAndMessages(null);
        if (exoPlayer != null) exoPlayer.release();
        if (nextPlayer != null) nextPlayer.release();
        timestampHandler.removeCallbacks(updateTimestampRunnable);
    }
}