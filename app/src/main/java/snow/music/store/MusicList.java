package snow.music.store;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * 用于表示一个歌单。
 * <p>
 * 歌单中的歌曲是延迟加载的，会在第一次调用 {@link #getMusicElements()} 方法时进行加载并进行缓存。
 */
public class MusicList {
    private final MusicListEntity mMusicListEntity;
    private ElementList mElementList;

    MusicList(@NonNull MusicListEntity musicListEntity) {
        Preconditions.checkNotNull(musicListEntity);

        mMusicListEntity = musicListEntity;
    }

    synchronized void applyChanges() {
        if (mElementList == null) {
            return;
        }

        mElementList.applyChanges();
    }

    MusicListEntity getMusicListEntity() {
        return mMusicListEntity;
    }

    /**
     * 获取歌单的 ID.
     */
    public long getId() {
        return mMusicListEntity.id;
    }

    /**
     * 获取歌单名称。
     */
    @NonNull
    public String getName() {
        return mMusicListEntity.name;
    }

    /**
     * 设置歌单名。
     * <p>
     * 如果歌单名已存在，则忽略本次操作。可以调用 {@link MusicStore#isMusicListExists(String)}
     * 方法判断歌单名是否已存在。
     */
    public void setName(@NonNull String name) {
        Preconditions.checkNotNull(name);
        Preconditions.checkArgument(!name.isEmpty(), "name must not empty");

        if (isBuiltIn()) {
            return;
        }

        mMusicListEntity.name = name;
    }

    /**
     * 当前歌单是否是内置歌单。如果是，则不允许修改歌单的名称。
     */
    public boolean isBuiltIn() {
        return MusicStore.isBuiltInName(mMusicListEntity.name);
    }

    /**
     * 获取歌单的描述信息。
     */
    @NonNull
    public String getDescription() {
        return mMusicListEntity.description;
    }

    /**
     * 设置歌单的描述信息。
     */
    public void setDescription(@NonNull String description) {
        Preconditions.checkNotNull(description);

        mMusicListEntity.description = description;
    }

    /**
     * 获取歌单大小。
     */
    public long getSize() {
        return mMusicListEntity.size;
    }

    /**
     * 获取歌单中的所有歌曲。
     * <p>
     * 你可以对返回的 List 进行增删该操作，但不允许往列表中添加两首完全相同的音乐。
     */
    public synchronized List<Music> getMusicElements() {
        if (mElementList == null) {
            mElementList = new ElementList();
        }
        return mElementList;
    }

    private class ElementList implements List<Music> {
        private List<Music> mOrderedList;

        ElementList() {
            if (mMusicListEntity.orderBytes == null || mMusicListEntity.orderBytes.length <= 0) {
                mOrderedList = new ArrayList<>(mMusicListEntity.musicElements);
                return;
            }

            try {
                ByteArrayInputStream byteInput = new ByteArrayInputStream(mMusicListEntity.orderBytes);
                ObjectInputStream input = new ObjectInputStream(byteInput);

                mOrderedList = new ArrayList<>();
                while (input.available() > 0) {
                    long id = input.readLong();
                    if (id <= 0) {
                        mOrderedList = new ArrayList<>(mMusicListEntity.musicElements);
                        return;
                    }
                    mOrderedList.add(mMusicListEntity.musicElements.getById(id));
                }

                input.close();
            } catch (IOException e) {
                mOrderedList = new ArrayList<>(mMusicListEntity.musicElements);
                e.printStackTrace();
            }
        }

        void applyChanges() {
            mMusicListEntity.orderBytes = getOrderBytes();
            mMusicListEntity.size = mOrderedList.size();
        }

        @NonNull
        private byte[] getOrderBytes() {
            try {
                ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(mOrderedList.size() * 4);
                ObjectOutputStream output = new ObjectOutputStream(byteOutput);

                for (Music music : mOrderedList) {
                    output.writeLong(music.id);
                }

                output.flush();
                output.close();
                return byteOutput.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new byte[0];
        }

        @Override
        public int size() {
            return mOrderedList.size();
        }

        @Override
        public boolean isEmpty() {
            return mOrderedList.isEmpty();
        }

        @Override
        public boolean contains(@Nullable Object o) {
            return mOrderedList.contains(o);
        }

        @NonNull
        @Override
        public Iterator<Music> iterator() {
            return mOrderedList.iterator();
        }

        @NonNull
        @Override
        public Object[] toArray() {
            return mOrderedList.toArray();
        }

        @NonNull
        @Override
        public <T> T[] toArray(@NonNull T[] a) {
            return mOrderedList.toArray(a);
        }

        /**
         * 如果 element 已存在，则会忽略本次操作，并直接返回 false。
         */
        @Override
        public boolean add(Music t) {
            if (contains(t)) {
                return false;
            }

            boolean result = mOrderedList.add(t);

            if (result) {
                mMusicListEntity.musicElements.add(t);
                mMusicListEntity.size = mMusicListEntity.musicElements.size();
            }

            return result;
        }

        @Override
        public boolean remove(@Nullable Object o) {
            boolean result = mOrderedList.remove(o);

            if (result) {
                mMusicListEntity.musicElements.remove(o);
                mMusicListEntity.size = mMusicListEntity.musicElements.size();
            }

            return result;
        }

        @Override
        public boolean containsAll(@NonNull Collection<?> c) {
            Preconditions.checkNotNull(c);
            return mOrderedList.containsAll(c);
        }

        @Override
        public boolean addAll(@NonNull Collection<? extends Music> c) {
            Preconditions.checkNotNull(c);

            c = excludeDuplicates(c, true);

            boolean result = mOrderedList.addAll(c);

            if (result) {
                mMusicListEntity.musicElements.addAll(c);
                mMusicListEntity.size = mMusicListEntity.musicElements.size();
            }

            return result;
        }

        @Override
        public boolean addAll(int index, @NonNull Collection<? extends Music> c) {
            Preconditions.checkNotNull(c);

            c = excludeDuplicates(c, true);

            boolean result = mOrderedList.addAll(index, c);

            if (result) {
                mMusicListEntity.musicElements.addAll(c);
                mMusicListEntity.size = mMusicListEntity.musicElements.size();
            }

            return result;
        }

        @Override
        public boolean removeAll(@NonNull Collection<?> c) {
            Preconditions.checkNotNull(c);

            boolean result = mOrderedList.removeAll(c);

            if (result) {
                mMusicListEntity.musicElements.removeAll(c);
                mMusicListEntity.size = mMusicListEntity.musicElements.size();
            }

            return result;
        }

        @Override
        public boolean retainAll(@NonNull Collection<?> c) {
            Preconditions.checkNotNull(c);

            c = excludeDuplicates(c, false);

            boolean result = mOrderedList.retainAll(c);

            if (result) {
                mMusicListEntity.musicElements.retainAll(c);
                mMusicListEntity.size = mMusicListEntity.musicElements.size();
            }

            return result;
        }

        private Collection<Music> excludeDuplicates(Collection<?> c, boolean excludeExists) {
            List<Music> musicList = new ArrayList<>();

            for (Object music : c) {
                if (musicList.contains(music)) {
                    continue;
                }

                if (excludeExists && contains(music)) {
                    continue;
                }

                musicList.add((Music) music);
            }

            return musicList;
        }

        @Override
        public void clear() {
            mMusicListEntity.musicElements.clear();
            mMusicListEntity.size = mMusicListEntity.musicElements.size();
            mOrderedList.clear();
        }

        @Override
        public Music get(int index) {
            return mOrderedList.get(index);
        }

        /**
         * 如果 element 已存在，则会在设置完成后移除旧元素。
         */
        @Override
        public Music set(int index, Music element) {
            int elementIndex = indexOf(element);

            Music music = mOrderedList.set(index, element);
            mMusicListEntity.musicElements.set(mMusicListEntity.musicElements.indexOf(music), element);

            if (elementIndex > 0 && elementIndex != index) {
                remove(elementIndex);
            }

            return music;
        }

        /**
         * 如果 element 已存在，则会添加后移除旧元素。
         */
        @Override
        public void add(int index, Music element) {
            int elementIndex = indexOf(element);

            mMusicListEntity.musicElements.add(index, element);
            mMusicListEntity.size = mMusicListEntity.musicElements.size();

            if (elementIndex > -1) {
                remove(element);
            }

            mOrderedList.add(index, element);
        }

        @Override
        public Music remove(int index) {
            Music music = mOrderedList.remove(index);
            mMusicListEntity.musicElements.remove(music);
            mMusicListEntity.size = mMusicListEntity.musicElements.size();
            return music;
        }

        @Override
        public int indexOf(@Nullable Object o) {
            return mOrderedList.indexOf(o);
        }

        @Override
        public int lastIndexOf(@Nullable Object o) {
            return mOrderedList.lastIndexOf(o);
        }

        @NonNull
        @Override
        public ListIterator<Music> listIterator() {
            return mOrderedList.listIterator();
        }

        @NonNull
        @Override
        public ListIterator<Music> listIterator(int index) {
            return mOrderedList.listIterator(index);
        }

        @NonNull
        @Override
        public List<Music> subList(int fromIndex, int toIndex) {
            return mOrderedList.subList(fromIndex, toIndex);
        }
    }
}
