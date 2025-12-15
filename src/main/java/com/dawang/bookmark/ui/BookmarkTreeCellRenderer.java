package com.dawang.bookmark.ui;

import com.dawang.bookmark.model.BookmarkItem;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 收藏树形结构渲染器
 */
public class BookmarkTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();

        if (userObject instanceof LabelNode) {
            // 标签节点
            LabelNode labelNode = (LabelNode) userObject;
            setText(labelNode.toString());
            setIcon(UIManager.getIcon("Tree.closedIcon"));
        } else if (userObject instanceof BookmarkItem) {
            // 收藏项节点
            BookmarkItem item = (BookmarkItem) userObject;
            String preview = item.getContentPreview();
            String time = DATE_FORMAT.format(new Date(item.getTimestamp()));
            String displayText = preview + " - " + time;
            // 如果有备注，显示备注
            if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
                String note = item.getNote();
                if (note.length() > 30) {
                    note = note.substring(0, 30) + "...";
                }
                displayText += " [" + note + "]";
            }
            setText(displayText);
            setIcon(UIManager.getIcon("Tree.leafIcon"));
        } else if (userObject instanceof String && "收藏".equals(userObject)) {
            // 根节点
            setText("收藏");
            setIcon(UIManager.getIcon("Tree.openIcon"));
        }

        return this;
    }
}
