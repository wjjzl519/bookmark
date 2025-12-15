package com.example.bookmark.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * 收藏项数据模型
 */
public class BookmarkItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id; // 唯一标识
    private String content; // 收藏的内容
    private String label; // 标签
    private String note; // 备注
    private long timestamp; // 收藏时间戳
    private String filePath; // 文件路径（如果来自文件）
    private int startOffset; // 起始位置（可选）
    private int endOffset; // 结束位置（可选）
    private String sourceType; // 来源类型：EDITOR, CONSOLE

    public BookmarkItem() {
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * 获取内容预览（前50个字符）
     */
    public String getContentPreview() {
        if (content == null) {
            return "";
        }
        if (content.length() <= 50) {
            return content;
        }
        return content.substring(0, 50) + "...";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BookmarkItem that = (BookmarkItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BookmarkItem{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", contentPreview='" + getContentPreview() + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
