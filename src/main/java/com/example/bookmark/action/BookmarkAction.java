package com.example.bookmark.action;

import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.example.bookmark.model.BookmarkItem;
import com.example.bookmark.service.BookmarkStateService;
import com.example.bookmark.ui.BookmarkInputDialog;
import com.example.bookmark.ui.BookmarkToolWindowFactory;
import com.example.bookmark.util.BookmarkUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

/**
 * 收藏操作 Action
 */
public class BookmarkAction extends AnAction {

    private static final javax.swing.Icon ICON = IconLoader.getIcon("/icons/bookmark.svg", BookmarkAction.class);

    public BookmarkAction() {
        super("收藏选中内容", "收藏当前选中的文本内容", ICON);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // 1. 获取选中文本
        String selectedText = BookmarkUtil.getSelectedText(e);
        if (selectedText == null || selectedText.trim().isEmpty()) {
            Messages.showWarningDialog(
                    project,
                    "请先选中要收藏的内容",
                    "收藏提示");
            return;
        }

        // 2. 弹出收藏输入对话框（支持标签选择和备注输入）
        BookmarkInputDialog dialog = new BookmarkInputDialog(project);
        if (!dialog.showAndGet()) {
            return; // 用户取消
        }

        String label = dialog.getLabel();
        String note = dialog.getNote();

        if (label == null || label.trim().isEmpty()) {
            return; // 标签为空
        }

        // 3. 创建收藏项
        BookmarkItem item = createBookmarkItem(selectedText, label.trim(), note, e);
        if (item == null) {
            Messages.showErrorDialog(
                    project,
                    "创建收藏项失败",
                    "错误");
            return;
        }

        // 4. 保存到状态
        BookmarkStateService service = BookmarkStateService.getInstance();
        if (service == null) {
            Messages.showErrorDialog(
                    project,
                    "无法访问收藏服务，请重启 IDE 后重试",
                    "错误");
            return;
        }

        try {
            service.addBookmark(item);
        } catch (Exception ex) {
            Messages.showErrorDialog(
                    project,
                    "保存收藏失败: " + ex.getMessage(),
                    "错误");
            return;
        }

        // 5. 刷新工具窗口（在 UI 线程中异步执行，确保数据已保存）
        com.intellij.openapi.application.ApplicationManager.getApplication()
                .invokeLater(() -> refreshToolWindow(project));

        // 6. 显示自动关闭的成功提示
        showAutoCloseNotification(project, "收藏成功！标签: " + label);
    }

    /**
     * 创建收藏项
     */
    private BookmarkItem createBookmarkItem(String content, String label, String note, AnActionEvent e) {
        BookmarkItem item = new BookmarkItem();
        item.setId(UUID.randomUUID().toString());
        item.setContent(content);
        item.setLabel(label);
        item.setNote(note);
        item.setTimestamp(System.currentTimeMillis());

        // 收集文件信息
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file != null) {
            item.setFilePath(file.getPath());
            item.setSourceType("EDITOR");

            // 收集位置信息
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            if (editor != null) {
                int[] offsets = BookmarkUtil.getSelectionOffsets(e);
                item.setStartOffset(offsets[0]);
                item.setEndOffset(offsets[1]);
            }
        } else {
            item.setSourceType("CONSOLE");
        }

        return item;
    }

    /**
     * 刷新工具窗口
     */
    private void refreshToolWindow(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(BookmarkToolWindowFactory.TOOL_WINDOW_ID);
        if (toolWindow != null) {
            // 通知工具窗口刷新
            BookmarkToolWindowFactory.refreshToolWindow(project);
        }
    }

    /**
     * 显示自动关闭的通知
     */
    private void showAutoCloseNotification(Project project, String message) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // 使用通知系统，会自动显示在右下角并自动消失
                // 通知组已在 plugin.xml 中注册
                com.intellij.notification.NotificationGroup notificationGroup = com.intellij.notification.NotificationGroupManager
                        .getInstance()
                        .getNotificationGroup("BookmarkPlugin");

                if (notificationGroup != null) {
                    notificationGroup.createNotification(
                            message,
                            com.intellij.notification.NotificationType.INFORMATION).notify(project);
                } else {
                    // 如果获取失败，直接创建通知（兼容旧版本）
                    new com.intellij.notification.Notification(
                            "BookmarkPlugin",
                            "收藏提示",
                            message,
                            com.intellij.notification.NotificationType.INFORMATION).notify(project);
                }
            } catch (Exception e) {
                // 如果通知失败，静默处理
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 可以根据是否有选中文本来启用/禁用 Action
        // 这里保持始终可用，在 actionPerformed 中检查
        e.getPresentation().setEnabled(true);
    }
}
