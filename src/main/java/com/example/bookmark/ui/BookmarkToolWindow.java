package com.example.bookmark.ui;

import com.example.bookmark.model.BookmarkItem;
import com.example.bookmark.service.BookmarkStateService;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏工具窗口
 */
public class BookmarkToolWindow {
    private JPanel contentPanel;
    private Tree bookmarkTree;
    private DefaultTreeModel treeModel;
    private Project project;

    public BookmarkToolWindow(Project project) {
        this.project = project;
        initUI();
        loadBookmarks();
    }

    private void initUI() {
        contentPanel = new JPanel(new BorderLayout());

        // 创建工具栏
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new RefreshAction());
        actionGroup.add(new ClearAllAction());

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                "BookmarkToolbar", actionGroup, true);
        toolbar.setTargetComponent(contentPanel);

        // 创建树形结构
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("收藏");
        treeModel = new DefaultTreeModel(root);
        bookmarkTree = new Tree(treeModel);
        bookmarkTree.setCellRenderer(new BookmarkTreeCellRenderer());
        bookmarkTree.setRootVisible(true);
        bookmarkTree.setShowsRootHandles(true);

        // 添加右键菜单
        addContextMenu();

        // 添加双击事件（跳转到源位置）
        bookmarkTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    navigateToBookmark();
                }
            }
        });

        // 布局
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toolbar.getComponent(), BorderLayout.WEST);
        contentPanel.add(topPanel, BorderLayout.NORTH);
        contentPanel.add(new JBScrollPane(bookmarkTree), BorderLayout.CENTER);
    }

    private void addContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem deleteItem = new JMenuItem("删除", AllIcons.General.Remove);
        deleteItem.addActionListener(e -> deleteSelectedBookmark());
        popupMenu.add(deleteItem);

        JMenuItem copyItem = new JMenuItem("复制内容", AllIcons.Actions.Copy);
        copyItem.addActionListener(e -> copyBookmarkContent());
        popupMenu.add(copyItem);

        bookmarkTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            private void showPopupMenu(MouseEvent e) {
                TreePath path = bookmarkTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    bookmarkTree.setSelectionPath(path);
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node.getUserObject() instanceof BookmarkItem) {
                        popupMenu.show(bookmarkTree, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    public void loadBookmarks() {
        // 从状态服务加载收藏
        BookmarkStateService service = BookmarkStateService.getInstance();
        if (service == null) {
            // 如果服务未初始化，清空显示
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            root.removeAllChildren();
            treeModel.reload();
            return;
        }

        List<BookmarkItem> bookmarks = service.getAllBookmarks();
        if (bookmarks == null || bookmarks.isEmpty()) {
            // 如果没有收藏，清空树
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            root.removeAllChildren();
            treeModel.reload();
            return;
        }

        // 按标签分组
        Map<String, List<BookmarkItem>> grouped = bookmarks.stream()
                .filter(item -> item.getLabel() != null) // 过滤掉标签为 null 的项
                .collect(Collectors.groupingBy(BookmarkItem::getLabel));

        // 构建树形结构
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();

        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // 按标签名排序
                .forEach(entry -> {
                    String label = entry.getKey();
                    List<BookmarkItem> items = entry.getValue();

                    // 按时间倒序排序（最新的在前）
                    items.sort(Comparator.comparingLong(BookmarkItem::getTimestamp).reversed());

                    LabelNode labelNode = new LabelNode(label, items.size());
                    DefaultMutableTreeNode labelTreeNode = new DefaultMutableTreeNode(labelNode);

                    items.forEach(item -> {
                        DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(item);
                        labelTreeNode.add(itemNode);
                    });

                    root.add(labelTreeNode);
                });

        treeModel.reload();
        expandAllNodes();
    }

    private void expandAllNodes() {
        for (int i = 0; i < bookmarkTree.getRowCount(); i++) {
            bookmarkTree.expandRow(i);
        }
    }

    private void navigateToBookmark() {
        TreePath selectionPath = bookmarkTree.getSelectionPath();
        if (selectionPath == null) {
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        Object userObject = node.getUserObject();

        if (userObject instanceof BookmarkItem) {
            BookmarkItem item = (BookmarkItem) userObject;
            // 显示 Markdown 查看对话框
            BookmarkViewDialog dialog = new BookmarkViewDialog(project, item);
            dialog.show();
        }
    }

    private void deleteSelectedBookmark() {
        TreePath selectionPath = bookmarkTree.getSelectionPath();
        if (selectionPath == null) {
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        Object userObject = node.getUserObject();

        if (userObject instanceof BookmarkItem) {
            BookmarkItem item = (BookmarkItem) userObject;
            int result = Messages.showYesNoDialog(
                    project,
                    "确定要删除这个收藏吗？",
                    "删除收藏",
                    Messages.getQuestionIcon());

            if (result == Messages.YES) {
                BookmarkStateService.getInstance().removeBookmark(item.getId());
                loadBookmarks(); // 重新加载
            }
        }
    }

    private void copyBookmarkContent() {
        TreePath selectionPath = bookmarkTree.getSelectionPath();
        if (selectionPath == null) {
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        Object userObject = node.getUserObject();

        if (userObject instanceof BookmarkItem) {
            BookmarkItem item = (BookmarkItem) userObject;
            String content = item.getContent();
            if (content != null) {
                java.awt.Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(content), null);
                // 显示自动关闭的通知
                showAutoCloseNotification("内容已复制到剪贴板");
            }
        }
    }

    /**
     * 显示自动关闭的通知
     */
    private void showAutoCloseNotification(String message) {
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
                            "提示",
                            message,
                            com.intellij.notification.NotificationType.INFORMATION).notify(project);
                }
            } catch (Exception e) {
                // 如果通知失败，静默处理
            }
        });
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    // 刷新操作
    private class RefreshAction extends com.intellij.openapi.actionSystem.AnAction {
        public RefreshAction() {
            super("刷新", "刷新收藏列表", AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(com.intellij.openapi.actionSystem.AnActionEvent e) {
            loadBookmarks();
        }
    }

    // 清空所有操作
    private class ClearAllAction extends com.intellij.openapi.actionSystem.AnAction {
        public ClearAllAction() {
            super("清空", "清空所有收藏", AllIcons.General.Remove);
        }

        @Override
        public void actionPerformed(com.intellij.openapi.actionSystem.AnActionEvent e) {
            int result = Messages.showYesNoDialog(
                    project,
                    "确定要清空所有收藏吗？此操作不可恢复！",
                    "清空收藏",
                    Messages.getWarningIcon());

            if (result == Messages.YES) {
                BookmarkStateService.getInstance().clearAll();
                loadBookmarks();
            }
        }
    }
}
