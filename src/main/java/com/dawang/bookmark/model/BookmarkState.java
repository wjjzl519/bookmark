package com.dawang.bookmark.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件状态数据模型
 */
public class BookmarkState implements Serializable {
    private static final long serialVersionUID = 1L;

    public List<BookmarkItem> bookmarks = new ArrayList<>();

    public List<BookmarkItem> getBookmarks() {
        return bookmarks;
    }

    public void setBookmarks(List<BookmarkItem> bookmarks) {
        this.bookmarks = bookmarks != null ? bookmarks : new ArrayList<>();
    }

    /**
     * 添加收藏项
     */
    public void addBookmark(BookmarkItem item) {
        if (item == null) {
            return;
        }
        // 确保 bookmarks 列表不为 null
        if (bookmarks == null) {
            bookmarks = new ArrayList<>();
        }
        // 检查是否已存在（通过 ID 判断）
        boolean exists = bookmarks.stream()
                .anyMatch(existing -> item.getId() != null && item.getId().equals(existing.getId()));
        if (!exists) {
            bookmarks.add(item);
        }
    }

    /**
     * 删除收藏项
     */
    public boolean removeBookmark(String id) {
        return bookmarks.removeIf(item -> id.equals(item.getId()));
    }

    /**
     * 根据ID查找收藏项
     */
    public BookmarkItem findBookmark(String id) {
        return bookmarks.stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElse(null);
    }
}
