package com.dawang.bookmark.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 收藏工具窗口工厂
 */
public class BookmarkToolWindowFactory implements ToolWindowFactory {

    public static final String TOOL_WINDOW_ID = "Bookmark";

    // 为每个项目维护独立的工具窗口实例
    private static final Map<Project, BookmarkToolWindow> projectToolWindows = new ConcurrentHashMap<>();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        BookmarkToolWindow toolWindowInstance = new BookmarkToolWindow(project);
        projectToolWindows.put(project, toolWindowInstance);

        Content content = ContentFactory.SERVICE.getInstance().createContent(
                toolWindowInstance.getContentPanel(),
                "",
                false);
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * 刷新工具窗口
     */
    public static void refreshToolWindow(Project project) {
        if (project == null || project.isDisposed()) {
            return;
        }

        // 确保在 UI 线程中执行
        com.intellij.openapi.application.ApplicationManager.getApplication()
                .invokeLater(() -> {
                    BookmarkToolWindow toolWindow = projectToolWindows.get(project);
                    if (toolWindow != null) {
                        toolWindow.loadBookmarks();
                    } else {
                        // 如果工具窗口还没有创建，尝试获取并刷新
                        com.intellij.openapi.wm.ToolWindowManager toolWindowManager = com.intellij.openapi.wm.ToolWindowManager
                                .getInstance(project);
                        com.intellij.openapi.wm.ToolWindow tw = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
                        if (tw != null && tw.isAvailable()) {
                            // 工具窗口存在但内容可能还没创建，再次延迟刷新
                            com.intellij.openapi.application.ApplicationManager.getApplication()
                                    .invokeLater(() -> {
                                        BookmarkToolWindow window = projectToolWindows.get(project);
                                        if (window != null) {
                                            window.loadBookmarks();
                                        }
                                    });
                        }
                    }
                });
    }

    /**
     * 清理项目实例（当项目关闭时）
     */
    public static void disposeProject(Project project) {
        projectToolWindows.remove(project);
    }
}
