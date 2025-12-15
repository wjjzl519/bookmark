package com.dawang.bookmark.service;

import com.dawang.bookmark.model.BookmarkItem;
import com.dawang.bookmark.model.BookmarkState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 收藏状态持久化服务
 */
@State(name = "BookmarkPluginState", storages = @Storage("bookmark-plugin.xml"))
public class BookmarkStateService implements PersistentStateComponent<BookmarkState> {

    private BookmarkState state = new BookmarkState();

    @Override
    public @Nullable BookmarkState getState() {
        // 确保返回的状态对象不为 null
        if (state == null) {
            state = new BookmarkState();
        }
        return state;
    }

    @Override
    public void loadState(@NotNull BookmarkState state) {
        this.state = state;
        // 确保 bookmarks 列表不为 null
        if (this.state.getBookmarks() == null) {
            this.state.setBookmarks(new java.util.ArrayList<>());
        }
    }

    /**
     * 获取服务实例
     */
    public static BookmarkStateService getInstance() {
        // 尝试通过 ApplicationManager 获取服务
        BookmarkStateService service = ApplicationManager.getApplication().getService(BookmarkStateService.class);
        if (service == null) {
            // 如果获取失败，尝试通过 ServiceManager 获取（兼容旧版本）
            try {
                service = com.intellij.openapi.components.ServiceManager.getService(BookmarkStateService.class);
            } catch (Exception e) {
                // 如果还是失败，创建一个新实例（仅用于紧急情况）
                service = new BookmarkStateService();
            }
        }
        return service;
    }

    /**
     * 添加收藏项
     */
    public void addBookmark(BookmarkItem item) {
        if (item == null) {
            return;
        }
        // 确保 ID 不为空
        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId(UUID.randomUUID().toString());
        }
        // 确保 state 已初始化
        if (state == null) {
            state = new BookmarkState();
        }
        // 确保 bookmarks 列表已初始化
        if (state.getBookmarks() == null) {
            state.setBookmarks(new java.util.ArrayList<>());
        }
        // 确保 BookmarkState 的 bookmarks 列表不为 null
        if (state.bookmarks == null) {
            state.bookmarks = new java.util.ArrayList<>();
        }
        try {
            state.addBookmark(item);
        } catch (Exception e) {
            // 如果 addBookmark 失败，直接添加到列表
            if (!state.bookmarks.contains(item)) {
                state.bookmarks.add(item);
            }
        }
        // PersistentStateComponent 会在状态改变时自动保存
        // 确保 getState() 返回最新的状态对象
    }

    /**
     * 删除收藏项
     */
    public boolean removeBookmark(String id) {
        return state.removeBookmark(id);
    }

    /**
     * 获取所有收藏
     */
    public List<BookmarkItem> getAllBookmarks() {
        if (state == null) {
            state = new BookmarkState();
        }
        List<BookmarkItem> bookmarks = state.getBookmarks();
        if (bookmarks == null) {
            bookmarks = new java.util.ArrayList<>();
            state.setBookmarks(bookmarks);
        }
        return bookmarks;
    }

    /**
     * 根据ID查找收藏项
     */
    public BookmarkItem findBookmark(String id) {
        return state.findBookmark(id);
    }

    /**
     * 清空所有收藏
     */
    public void clearAll() {
        state.setBookmarks(new java.util.ArrayList<>());
    }

    /**
     * 获取数据存储文件路径（用于调试）
     * 数据实际存储在: {IDEA配置目录}/options/bookmark-plugin.xml
     */
    public String getStoragePath() {
        // 获取 IDEA 配置目录 - PathManager 是静态工具类
        String configPath = com.intellij.openapi.application.PathManager.getConfigPath();
        return configPath + "/options/bookmark-plugin.xml";
    }
}
