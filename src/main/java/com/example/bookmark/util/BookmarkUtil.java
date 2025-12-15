package com.example.bookmark.util;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

/**
 * 收藏工具类
 */
public class BookmarkUtil {
    
    /**
     * 从事件中获取选中的文本
     * 优先从编辑器获取，如果编辑器没有选中，尝试从控制台获取
     */
    @Nullable
    public static String getSelectedText(AnActionEvent e) {
        // 方法1: 从编辑器获取
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            String text = selectionModel.getSelectedText();
            if (text != null && !text.trim().isEmpty()) {
                return text;
            }
        }
        
        // 方法2: 尝试从控制台获取
        // 控制台通常也使用 Editor，但可能需要特殊处理
        // 这里先返回 null，后续可以扩展
        
        return null;
    }
    
    /**
     * 获取当前文件路径
     */
    @Nullable
    public static String getCurrentFilePath(AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file != null) {
            return file.getPath();
        }
        return null;
    }
    
    /**
     * 获取选中文本的起始和结束位置
     */
    public static int[] getSelectionOffsets(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            return new int[]{
                selectionModel.getSelectionStart(),
                selectionModel.getSelectionEnd()
            };
        }
        return new int[]{-1, -1};
    }
    
    /**
     * 判断是否在编辑器中
     */
    public static boolean isInEditor(AnActionEvent e) {
        return e.getData(CommonDataKeys.EDITOR) != null;
    }
}
