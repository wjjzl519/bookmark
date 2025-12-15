package com.dawang.bookmark.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.jetbrains.annotations.Nullable;

import com.dawang.bookmark.model.BookmarkItem;
import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;

/**
 * 收藏内容查看对话框
 * 使用 JCEF 浏览器组件以 Markdown 格式显示收藏内容
 */
public class BookmarkViewDialog extends DialogWrapper {
    private final BookmarkItem item;
    private final Project project;
    private JBCefBrowser browser;
    private JPanel browserPanel; // 用于放置内容区域（JCEF 或降级方案）

    public BookmarkViewDialog(Project project, BookmarkItem item) {
        super(project);
        this.project = project;
        this.item = item;
        setTitle("收藏内容 - " + (item.getLabel() != null ? item.getLabel() : "未命名"));
        setResizable(true);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // 固定大小，缩小 10%
        panel.setPreferredSize(new Dimension(720, 540));

        // 创建信息面板
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 标签
        if (item.getLabel() != null && !item.getLabel().trim().isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 0;
            infoPanel.add(new JLabel("标签:"), gbc);
            gbc.gridx = 1;
            infoPanel.add(new JLabel(item.getLabel()), gbc);
        }

        // 备注
        if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 1;
            infoPanel.add(new JLabel("备注:"), gbc);
            gbc.gridx = 1;
            JTextArea noteArea = new JTextArea(item.getNote());
            noteArea.setEditable(false);
            noteArea.setOpaque(false);
            noteArea.setLineWrap(true);
            noteArea.setWrapStyleWord(true);
            infoPanel.add(noteArea, gbc);
        }

        // 文件路径
        if (item.getFilePath() != null && !item.getFilePath().trim().isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 2;
            infoPanel.add(new JLabel("文件:"), gbc);
            gbc.gridx = 1;
            JLabel fileLabel = new JLabel(item.getFilePath());
            fileLabel.setForeground(Color.GRAY);
            infoPanel.add(fileLabel, gbc);
        }

        // 时间
        gbc.gridx = 0;
        gbc.gridy = 3;
        infoPanel.add(new JLabel("时间:"), gbc);
        gbc.gridx = 1;
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        infoPanel.add(new JLabel(sdf.format(new java.util.Date(item.getTimestamp()))), gbc);

        // 创建内容显示区域 - 使用 JCEF 浏览器组件
        browserPanel = new JPanel(new BorderLayout());
        browserPanel.setBorder(BorderFactory.createTitledBorder("内容"));

        if (JBCefApp.isSupported()) {
            // 使用 JCEF 浏览器显示 Markdown
            browser = new JBCefBrowser();

            // 将 Markdown 转换为 HTML
            String htmlContent = convertMarkdownToHtml(item.getContent());

            // 加载 HTML 内容到浏览器
            browser.loadHTML(htmlContent);

            browserPanel.add(browser.getComponent(), BorderLayout.CENTER);
        } else {
            // JCEF 不支持时的降级方案
            JEditorPane contentPane = new JEditorPane();
            contentPane.setContentType("text/html");
            contentPane.setEditable(false);
            contentPane.setBackground(Color.WHITE);

            String htmlContent = convertMarkdownToHtml(item.getContent());
            contentPane.setText(htmlContent);
            contentPane.setCaretPosition(0);

            JScrollPane scrollPane = new JScrollPane(contentPane);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());

            browserPanel.add(scrollPane, BorderLayout.CENTER);
        }

        // 组合布局
        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(browserPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 将 Markdown 文本转换为 HTML
     * 支持基本的 Markdown 语法
     */
    private String convertMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "<p>（无内容）</p>";
        }

        // 按行处理
        String[] lines = markdown.split("\n");
        StringBuilder html = new StringBuilder();
        boolean inCodeBlock = false;
        boolean inList = false;

        for (String line : lines) {
            String trimmedLine = line.trim();

            // 代码块处理
            if (trimmedLine.startsWith("```")) {
                if (inCodeBlock) {
                    html.append("</code></pre>");
                    inCodeBlock = false;
                } else {
                    if (inList) {
                        html.append("</ul>");
                        inList = false;
                    }
                    html.append("<pre><code>");
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                // 在代码块中，直接转义并添加
                html.append(escapeHtml(line)).append("\n");
                continue;
            }

            // 标题处理
            if (trimmedLine.startsWith("### ")) {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                html.append("<h3>").append(escapeHtml(trimmedLine.substring(4))).append("</h3>");
                continue;
            } else if (trimmedLine.startsWith("## ")) {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                html.append("<h2>").append(escapeHtml(trimmedLine.substring(3))).append("</h2>");
                continue;
            } else if (trimmedLine.startsWith("# ")) {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                html.append("<h1>").append(escapeHtml(trimmedLine.substring(2))).append("</h1>");
                continue;
            }

            // 列表项处理
            if (trimmedLine.matches("^[-*] .+")) {
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                String listContent = processInlineMarkdown(trimmedLine.substring(2));
                html.append("<li>").append(listContent).append("</li>");
                continue;
            } else {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
            }

            // 空行处理
            if (trimmedLine.isEmpty()) {
                html.append("<br>");
                continue;
            }

            // 普通段落
            String processedLine = processInlineMarkdown(line);
            html.append("<p>").append(processedLine).append("</p>");
        }

        // 关闭未关闭的标签
        if (inCodeBlock) {
            html.append("</code></pre>");
        }
        if (inList) {
            html.append("</ul>");
        }

        // 添加现代化的 Markdown 样式（GitHub 风格）
        String styledHtml = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <style>\n" +
                "        * { box-sizing: border-box; }\n" +
                "        body {\n" +
                "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Helvetica Neue', Arial, 'Noto Sans', sans-serif;\n"
                +
                "            font-size: 16px;\n" +
                "            line-height: 1.6;\n" +
                "            padding: 20px;\n" +
                "            color: #24292e;\n" +
                "            background-color: #ffffff;\n" +
                "            max-width: 100%;\n" +
                "        }\n" +
                "        pre {\n" +
                "            background-color: #f6f8fa;\n" +
                "            border: 1px solid #e1e4e8;\n" +
                "            border-radius: 6px;\n" +
                "            padding: 16px;\n" +
                "            overflow-x: auto;\n" +
                "            margin: 16px 0;\n" +
                "            font-size: 85%;\n" +
                "            line-height: 1.45;\n" +
                "        }\n" +
                "        code {\n" +
                "            background-color: rgba(27, 31, 35, 0.05);\n" +
                "            padding: 0.2em 0.4em;\n" +
                "            border-radius: 3px;\n" +
                "            font-family: 'SFMono-Regular', 'Consolas', 'Liberation Mono', 'Menlo', monospace;\n" +
                "            font-size: 85%;\n" +
                "        }\n" +
                "        pre code {\n" +
                "            background-color: transparent;\n" +
                "            padding: 0;\n" +
                "            border-radius: 0;\n" +
                "            font-size: 100%;\n" +
                "        }\n" +
                "        h1 {\n" +
                "            font-size: 2em;\n" +
                "            border-bottom: 2px solid #eaecef;\n" +
                "            padding-bottom: 0.3em;\n" +
                "            margin-top: 24px;\n" +
                "            margin-bottom: 16px;\n" +
                "            font-weight: 600;\n" +
                "        }\n" +
                "        h2 {\n" +
                "            font-size: 1.5em;\n" +
                "            border-bottom: 1px solid #eaecef;\n" +
                "            padding-bottom: 0.3em;\n" +
                "            margin-top: 24px;\n" +
                "            margin-bottom: 16px;\n" +
                "            font-weight: 600;\n" +
                "        }\n" +
                "        h3 {\n" +
                "            font-size: 1.25em;\n" +
                "            margin-top: 24px;\n" +
                "            margin-bottom: 16px;\n" +
                "            font-weight: 600;\n" +
                "        }\n" +
                "        p {\n" +
                "            margin: 16px 0;\n" +
                "        }\n" +
                "        ul, ol {\n" +
                "            padding-left: 2em;\n" +
                "            margin: 16px 0;\n" +
                "        }\n" +
                "        li {\n" +
                "            margin: 4px 0;\n" +
                "        }\n" +
                "        a {\n" +
                "            color: #0366d6;\n" +
                "            text-decoration: none;\n" +
                "        }\n" +
                "        a:hover {\n" +
                "            text-decoration: underline;\n" +
                "        }\n" +
                "        strong {\n" +
                "            font-weight: 600;\n" +
                "        }\n" +
                "        em {\n" +
                "            font-style: italic;\n" +
                "        }\n" +
                "        blockquote {\n" +
                "            padding: 0 1em;\n" +
                "            color: #6a737d;\n" +
                "            border-left: 0.25em solid #dfe2e5;\n" +
                "            margin: 0;\n" +
                "        }\n" +
                "        table {\n" +
                "            border-collapse: collapse;\n" +
                "            margin: 16px 0;\n" +
                "        }\n" +
                "        table th,\n" +
                "        table td {\n" +
                "            border: 1px solid #dfe2e5;\n" +
                "            padding: 6px 13px;\n" +
                "        }\n" +
                "        table th {\n" +
                "            background-color: #f6f8fa;\n" +
                "            font-weight: 600;\n" +
                "        }\n" +
                "        hr {\n" +
                "            height: 0.25em;\n" +
                "            padding: 0;\n" +
                "            margin: 24px 0;\n" +
                "            background-color: #e1e4e8;\n" +
                "            border: 0;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                html.toString() +
                "\n</body>\n" +
                "</html>";

        return styledHtml;
    }

    /**
     * 处理行内 Markdown 语法（粗体、斜体、代码、链接）
     */
    private String processInlineMarkdown(String text) {
        // 先转义 HTML
        String result = escapeHtml(text);

        // 代码块中的内容不处理
        // 行内代码 (`...`)
        result = result.replaceAll("`([^`]+)`", "<code>$1</code>");

        // 粗体 (**...** 或 __...__)
        result = result.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
        result = result.replaceAll("__([^_]+)__", "<strong>$1</strong>");

        // 斜体 (*...* 或 _..._，但要避免与粗体冲突)
        result = result.replaceAll("(?<!\\*)\\*([^*]+)\\*(?!\\*)", "<em>$1</em>");
        result = result.replaceAll("(?<!_)_([^_]+)_(?!_)", "<em>$1</em>");

        // 链接 [text](url)
        result = result.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "<a href=\"$2\">$1</a>");

        return result;
    }

    /**
     * 转义 HTML 特殊字符
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 复制原始内容到剪贴板，并给出提示
     */
    private void copyContent() {
        String content = item.getContent() != null ? item.getContent() : "";
        Transferable transferable = new StringSelection(content);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
        notifyInfo("收藏内容已复制到剪贴板");
    }

    /**
     * 简单通知提示
     */
    private void notifyInfo(String message) {
        try {
            NotificationGroupManager.getInstance()
                    .getNotificationGroup("BookmarkPlugin")
                    .createNotification(message, NotificationType.INFORMATION)
                    .notify(project);
        } catch (Exception e) {
            // 忽略通知失败
        }
    }

    @Override
    protected Action[] createActions() {
        // 将复制按钮与 OK 按钮放在一起
        return new Action[] { new CopyAction(), getOKAction() };
    }

    @Override
    public void dispose() {
        // 清理浏览器资源
        if (browser != null) {
            browser.dispose();
        }
        super.dispose();
    }

    /**
     * 底部复制按钮，与 OK 按钮并列
     * 使用 DialogWrapperAction（protected 内部类，可以直接使用因为继承自 DialogWrapper）
     */
    private class CopyAction extends DialogWrapper.DialogWrapperAction {
        protected CopyAction() {
            super("复制内容");
            putValue(Action.SMALL_ICON, AllIcons.Actions.Copy);
        }

        @Override
        protected void doAction(ActionEvent e) {
            copyContent();
        }
    }
}
