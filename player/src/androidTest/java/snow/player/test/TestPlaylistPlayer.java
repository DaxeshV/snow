package snow.player.test;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import snow.player.PlayerConfig;
import snow.player.media.MusicItem;
import snow.player.media.MusicPlayer;
import snow.player.playlist.AbstractPlaylistPlayer;
import snow.player.playlist.Playlist;
import snow.player.playlist.PlaylistManager;
import snow.player.playlist.PlaylistState;

public class TestPlaylistPlayer extends AbstractPlaylistPlayer {
    private Tester mTester;

    public TestPlaylistPlayer(@NonNull Context context,
                              @NonNull PlayerConfig playerConfig,
                              @NonNull PlaylistState playlistState,
                              @NonNull PlaylistManager playlistManager,
                              @NonNull Playlist playlist) {
        super(context,playerConfig, playlistState, true, playlistManager, playlist);

        mTester = new Tester();
    }

    public Tester tester() {
        return mTester;
    }

    public static class Tester {
        private Runnable mPlayingAction;
        private Runnable mPlaylistAvailableAction;

        public void setPlayingAction(Runnable action) {
            mPlayingAction = action;
        }

        public void setOnPlaylistAvailableAction(Runnable action) {
            mPlaylistAvailableAction = action;
        }
    }

    @Override
    protected void onPlaying(int progress, long updateTime) {
        super.onPlaying(progress, updateTime);

        if (mTester.mPlayingAction != null) {
            mTester.mPlayingAction.run();
            mTester.mPlayingAction = null;
        }
    }

    @Override
    protected void onPlaylistAvailable(Playlist playlist) {
        super.onPlaylistAvailable(playlist);

        if (mTester.mPlaylistAvailableAction != null) {
            mTester.mPlaylistAvailableAction.run();
            mTester.mPlaylistAvailableAction = null;
        }
    }

    @Override
    protected boolean isCached(MusicItem musicItem, SoundQuality soundQuality) {
        return false;
    }

    @NonNull
    @Override
    protected MusicPlayer onCreateMusicPlayer(Context context) {
        return new TestMusicPlayer();
    }

    @Nullable
    @Override
    protected Uri retrieveMusicItemUri(@NonNull MusicItem musicItem, @NonNull SoundQuality soundQuality) throws Exception {
        return Uri.parse(musicItem.getUri());
    }
}
