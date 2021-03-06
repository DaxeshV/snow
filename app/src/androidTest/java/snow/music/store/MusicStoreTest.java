package snow.music.store;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.objectbox.BoxStore;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MusicStoreTest {
    private File test_directory;
    private BoxStore store;
    private MusicStore mMusicStore;

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Before
    public void setUp() {
        test_directory = new File(getContext().getCacheDir(), "objectbox-test");

        BoxStore.deleteAllFiles(test_directory);
        store = MyObjectBox.builder()
                .directory(test_directory)
                .build();
        MusicStore.init(store);
        mMusicStore = MusicStore.getInstance();
    }

    @After
    public void tearDown() {
        if (store != null) {
            store.close();
            store = null;
        }
        BoxStore.deleteAllFiles(test_directory);
    }

    @Test
    public void createMusicList() {
        final String name = "TestMusicList";
        final String description = "test description";

        MusicList musicList = mMusicStore.createMusicList(name, description);

        assertEquals(name, musicList.getName());
        assertEquals(description, musicList.getDescription());

        boolean exception = false;
        try {
            mMusicStore.createMusicList(MusicStore.MUSIC_LIST_FAVORITE);
        } catch (IllegalArgumentException e) {
            exception = true;
        }

        assertTrue(exception);
    }

    @Test
    public void getMusicList() {
        final String name = "TestMusicList";
        final String description = "test description";

        mMusicStore.createMusicList(name, description);
        MusicList musicList = mMusicStore.getMusicList(name);

        assertNotNull(musicList);
        assertEquals(name, musicList.getName());
        assertEquals(description, musicList.getDescription());
    }

    @Test
    public void updateMusicList() {
        final String name = "TestMusicList";
        final String description = "test description";

        final String newName = "NewTestMusicList";
        final String newDescription = "new test description";
        final Music music = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());
        mMusicStore.putMusic(music);

        MusicList musicList = mMusicStore.createMusicList(name, description);

        musicList.setName(newName);
        musicList.setDescription(newDescription);
        musicList.getMusicElements().add(music);

        mMusicStore.updateMusicList(musicList);

        musicList = mMusicStore.getMusicList(newName);

        assertNotNull(musicList);
        assertEquals(newName, musicList.getName());
        assertEquals(newDescription, musicList.getDescription());
        assertEquals(music, musicList.getMusicElements().get(0));
    }

    @Test
    public void deleteMusicList() {
        final String name = "TestMusicList";
        final String description = "test description";
        mMusicStore.createMusicList(name, description);

        MusicList musicList = mMusicStore.getMusicList(name);

        assertNotNull(musicList);

        mMusicStore.deleteMusicList(musicList);
        musicList = mMusicStore.getMusicList(name);

        assertNull(musicList);
    }

    @Test
    public void deleteMusicList_name() {
        final String name = "TestMusicList";
        final String description = "test description";
        mMusicStore.createMusicList(name, description);

        MusicList musicList = mMusicStore.getMusicList(name);

        assertNotNull(musicList);

        mMusicStore.deleteMusicList(name);
        musicList = mMusicStore.getMusicList(name);

        assertNull(musicList);
    }

    @Test
    public void isFavorite() {
        final Music music = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());
        mMusicStore.putMusic(music);

        MusicList favorite = mMusicStore.getFavoriteMusicList();
        favorite.getMusicElements().add(music);

        assertFalse(mMusicStore.isFavorite(music));

        mMusicStore.updateMusicList(favorite);

        assertTrue(mMusicStore.isFavorite(music));
    }

    @Test
    public void addToFavorite() {
        final Music music = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());
        mMusicStore.putMusic(music);

        mMusicStore.addToFavorite(music);
        assertTrue(mMusicStore.isFavorite(music));
    }

    @Test
    public void removeFromFavorite() {
        final Music music = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());
        mMusicStore.putMusic(music);

        mMusicStore.addToFavorite(music);
        assertTrue(mMusicStore.isFavorite(music));

        mMusicStore.removeFromFavorite(music);
        assertFalse(mMusicStore.isFavorite(music));
    }

    @Test
    public void addHistory() {
        final Music musicA = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());

        final Music musicB = new Music(
                0,
                "title2",
                "artist2",
                "album2",
                "https://www.test.com/test2.mp3",
                "https://www.test.com/test2.png",
                60_000,
                System.currentTimeMillis());
        mMusicStore.putMusic(musicA);
        mMusicStore.putMusic(musicB);

        mMusicStore.addHistory(musicA);
        mMusicStore.addHistory(musicB);

        assertEquals(musicA, mMusicStore.getAllHistory().get(0));
        assertEquals(musicB, mMusicStore.getAllHistory().get(1));

        mMusicStore.addHistory(musicA);

        assertEquals(musicB, mMusicStore.getAllHistory().get(0));
        assertEquals(musicA, mMusicStore.getAllHistory().get(1));
    }

    @Test
    public void removeHistory() {
        final Music music = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());
        mMusicStore.putMusic(music);

        mMusicStore.addHistory(music);

        assertEquals(music, mMusicStore.getAllHistory().get(0));

        mMusicStore.removeHistory(music);

        assertEquals(0, mMusicStore.getAllHistory().size());
    }

    @Test
    public void removeHistory_collection() {
        final Music musicA = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());

        final Music musicB = new Music(
                0,
                "title2",
                "artist2",
                "album2",
                "https://www.test.com/test2.mp3",
                "https://www.test.com/test2.png",
                60_000,
                System.currentTimeMillis());

        final Music musicC = new Music(
                0,
                "title3",
                "artist3",
                "album3",
                "https://www.test.com/test3.mp3",
                "https://www.test.com/test3.png",
                60_000,
                System.currentTimeMillis());

        mMusicStore.putMusic(musicA);
        mMusicStore.putMusic(musicB);
        mMusicStore.putMusic(musicC);

        mMusicStore.addHistory(musicA);
        mMusicStore.addHistory(musicB);
        mMusicStore.addHistory(musicC);

        List<Music> removeItems = new ArrayList<>();
        removeItems.add(musicA);
        removeItems.add(musicC);

        mMusicStore.removeHistory(removeItems);

        assertEquals(musicB, mMusicStore.getAllHistory().get(0));
    }

    @Test
    public void clearHistory() {
        final Music musicA = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());

        final Music musicB = new Music(
                0,
                "title2",
                "artist2",
                "album2",
                "https://www.test.com/test2.mp3",
                "https://www.test.com/test2.png",
                60_000,
                System.currentTimeMillis());

        final Music musicC = new Music(
                0,
                "title3",
                "artist3",
                "album3",
                "https://www.test.com/test3.mp3",
                "https://www.test.com/test3.png",
                60_000,
                System.currentTimeMillis());

        mMusicStore.putMusic(musicA);
        mMusicStore.putMusic(musicB);
        mMusicStore.putMusic(musicC);

        mMusicStore.addHistory(musicA);
        mMusicStore.addHistory(musicB);
        mMusicStore.addHistory(musicC);

        mMusicStore.clearHistory();

        assertEquals(0, mMusicStore.getAllHistory().size());
    }

    @Test
    public void getMusic() {
        final Music music1 = new Music(
                0,
                "title1",
                "artist1",
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());

        mMusicStore.putMusic(music1);

        Music music2 = mMusicStore.getMusic(music1.getId());

        assertEquals(music1, music2);
    }

    @Test
    public void getAllMusicList() {
        final String nameA = "Test Music List A";
        final String nameB = "Test Music List B";

        mMusicStore.createMusicList(nameA);
        mMusicStore.createMusicList(nameB);

        List<MusicList> allMusicList = mMusicStore.getAllMusicList();

        assertEquals(2, allMusicList.size());
        assertEquals(nameA, allMusicList.get(0).getName());
        assertEquals(nameB, allMusicList.get(1).getName());
    }

    @Test
    public void getAllArtist() {
        assertNotNull(mMusicStore.getAllArtist());

        final String artistA = "Artist A";
        final String artistB = "Artist B";

        final Music musicA = new Music(
                0,
                "title1",
                artistA,
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());

        final Music musicB = new Music(
                0,
                "title2",
                artistA,
                "album2",
                "https://www.test.com/test2.mp3",
                "https://www.test.com/test2.png",
                60_000,
                System.currentTimeMillis());

        final Music musicC = new Music(
                0,
                "title3",
                artistB,
                "album3",
                "https://www.test.com/test3.mp3",
                "https://www.test.com/test3.png",
                60_000,
                System.currentTimeMillis());

        mMusicStore.putMusic(musicA);
        mMusicStore.putMusic(musicB);
        mMusicStore.putMusic(musicC);

        List<String> allArtist = mMusicStore.getAllArtist();

        assertEquals(2, allArtist.size());
        assertTrue(allArtist.contains(artistA));
        assertTrue(allArtist.contains(artistB));
    }

    @Test
    public void getAllAlbum() {
        assertNotNull(mMusicStore.getAllAlbum());

        final String albumA = "Album A";
        final String albumB = "Album B";

        final Music musicA = new Music(
                0,
                "title1",
                "artist1",
                albumA,
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());

        final Music musicB = new Music(
                0,
                "title2",
                "artist2",
                albumA,
                "https://www.test.com/test2.mp3",
                "https://www.test.com/test2.png",
                60_000,
                System.currentTimeMillis());

        final Music musicC = new Music(
                0,
                "title3",
                "artist3",
                albumB,
                "https://www.test.com/test3.mp3",
                "https://www.test.com/test3.png",
                60_000,
                System.currentTimeMillis());

        mMusicStore.putMusic(musicA);
        mMusicStore.putMusic(musicB);
        mMusicStore.putMusic(musicC);

        List<String> allAlbum = mMusicStore.getAllAlbum();

        assertEquals(2, allAlbum.size());
        assertTrue(allAlbum.contains(albumA));
        assertTrue(allAlbum.contains(albumB));
    }

    @Test
    public void getArtistAllMusic() {
        assertNotNull(mMusicStore.getArtistAllMusic("none"));

        final String artistA = "Artist A";
        final String artistB = "Artist B";

        final Music musicA = new Music(
                0,
                "title1",
                artistA,
                "album1",
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());

        final Music musicB = new Music(
                0,
                "title2",
                artistA,
                "album2",
                "https://www.test.com/test2.mp3",
                "https://www.test.com/test2.png",
                60_000,
                System.currentTimeMillis());

        final Music musicC = new Music(
                0,
                "title3",
                artistB,
                "album3",
                "https://www.test.com/test3.mp3",
                "https://www.test.com/test3.png",
                60_000,
                System.currentTimeMillis());

        mMusicStore.putMusic(musicA);
        mMusicStore.putMusic(musicB);
        mMusicStore.putMusic(musicC);

        List<Music> musicsA = mMusicStore.getArtistAllMusic(artistA);
        List<Music> musicsB = mMusicStore.getArtistAllMusic(artistB);

        assertEquals(2, musicsA.size());
        assertEquals(1, musicsB.size());
    }

    @Test
    public void getAlbumAllMusic() {
        assertNotNull(mMusicStore.getAlbumAllMusic("none"));

        final String albumA = "Album A";
        final String albumB = "Album B";

        final Music musicA = new Music(
                0,
                "title1",
                "artist1",
                albumA,
                "https://www.test.com/test1.mp3",
                "https://www.test.com/test1.png",
                60_000,
                System.currentTimeMillis());

        final Music musicB = new Music(
                0,
                "title2",
                "artist2",
                albumA,
                "https://www.test.com/test2.mp3",
                "https://www.test.com/test2.png",
                60_000,
                System.currentTimeMillis());

        final Music musicC = new Music(
                0,
                "title3",
                "artist3",
                albumB,
                "https://www.test.com/test3.mp3",
                "https://www.test.com/test3.png",
                60_000,
                System.currentTimeMillis());

        mMusicStore.putMusic(musicA);
        mMusicStore.putMusic(musicB);
        mMusicStore.putMusic(musicC);

        List<Music> musicsA = mMusicStore.getAlbumAllMusic(albumA);
        List<Music> musicsB = mMusicStore.getAlbumAllMusic(albumB);

        assertEquals(2, musicsA.size());
        assertEquals(1, musicsB.size());
    }

    @Test(timeout = 3000)
    public void onFavoriteChangeListener() throws InterruptedException {
        final Music music = new Music(
                0,
                "title",
                "artist",
                "album",
                "https://www.test.com/test.mp3",
                "https://www.test.com/test.png",
                60_000,
                System.currentTimeMillis());

        mMusicStore.putMusic(music);

        CountDownLatch addLatch = new CountDownLatch(1);
        MusicStore.OnFavoriteChangeListener addListener = new MusicStore.OnFavoriteChangeListener() {
            @Override
            public void onFavoriteChanged() {
                addLatch.countDown();
            }
        };

        mMusicStore.addOnFavoriteChangeListener(addListener);

        mMusicStore.addToFavorite(music);
        addLatch.await();

        // assert
        assertTrue(mMusicStore.isFavorite(music));
        mMusicStore.removeOnFavoriteChangeListener(addListener);

        CountDownLatch removeLatch = new CountDownLatch(1);
        MusicStore.OnFavoriteChangeListener removeListener = new MusicStore.OnFavoriteChangeListener() {
            @Override
            public void onFavoriteChanged() {
                removeLatch.countDown();
            }
        };

        mMusicStore.addOnFavoriteChangeListener(removeListener);

        mMusicStore.removeFromFavorite(music);
        removeLatch.await();

        // assert
        assertFalse(mMusicStore.isFavorite(music));
        mMusicStore.removeOnFavoriteChangeListener(removeListener);
    }
}
