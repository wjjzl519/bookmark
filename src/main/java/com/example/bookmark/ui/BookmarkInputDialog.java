package com.example.bookmark.ui;

import com.example.bookmark.service.BookmarkStateService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏输入对话框
 * 支持标签选择和备注输入
 */
public class BookmarkInputDialog extends DialogWrapper {
    private JTextField labelField;
    private JComboBox<String> labelComboBox;
    private JTextArea noteArea;
    private JCheckBox newLabelCheckBox;
    private final Project project;
    private final List<String> existingLabels;

    public BookmarkInputDialog(Project project) {
        super(project);
        this.project = project;
        this.existingLabels = getExistingLabels();
        setTitle("收藏内容");
        setResizable(false);
        init();
    }

    /**
     * 获取已存在的标签列表
     */
    private List<String> getExistingLabels() {
        BookmarkStateService service = BookmarkStateService.getInstance();
        if (service == null) {
            return java.util.Collections.emptyList();
        }
        return service.getAllBookmarks().stream()
                .map(item -> item.getLabel())
                .filter(label -> label != null && !label.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 标签选择区域
        JPanel labelPanel = new JPanel(new BorderLayout(5, 5));
        labelPanel.add(new JLabel("标签:"), BorderLayout.WEST);

        if (existingLabels.isEmpty()) {
            // 没有标签，直接输入
            labelField = new JTextField(20);
            labelPanel.add(labelField, BorderLayout.CENTER);
        } else {
            // 有标签，提供下拉选择
            labelComboBox = new JComboBox<>(existingLabels.toArray(new String[0]));
            labelComboBox.setEditable(true);
            labelComboBox.setSelectedItem("");
            labelPanel.add(labelComboBox, BorderLayout.CENTER);

            // 添加"新建标签"复选框
            newLabelCheckBox = new JCheckBox("新建标签");
            newLabelCheckBox.addActionListener(e -> {
                if (newLabelCheckBox.isSelected()) {
                    labelComboBox.setSelectedItem("");
                    labelComboBox.setEditable(true);
                } else {
                    labelComboBox.setSelectedItem(existingLabels.get(0));
                }
            });
            labelPanel.add(newLabelCheckBox, BorderLayout.EAST);
        }

        // 备注输入区域
        JPanel notePanel = new JPanel(new BorderLayout(5, 5));
        notePanel.add(new JLabel("备注（可选）:"), BorderLayout.NORTH);
        noteArea = new JTextArea(3, 20);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        JScrollPane noteScrollPane = new JScrollPane(noteArea);
        noteScrollPane.setPreferredSize(new Dimension(300, 60));
        notePanel.add(noteScrollPane, BorderLayout.CENTER);

        // 组合面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 10));
        mainPanel.add(labelPanel, BorderLayout.NORTH);
        mainPanel.add(notePanel, BorderLayout.CENTER);

        panel.add(mainPanel, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected void doOKAction() {
        String label = getLabel();
        if (label == null || label.trim().isEmpty()) {
            setErrorText("标签不能为空");
            return;
        }
        super.doOKAction();
    }

    /**
     * 获取标签
     */
    public String getLabel() {
        if (labelField != null) {
            return labelField.getText().trim();
        } else if (labelComboBox != null) {
            Object selected = labelComboBox.getSelectedItem();
            return selected != null ? selected.toString().trim() : "";
        }
        return "";
    }

    /**
     * 获取备注
     */
    public String getNote() {
        return noteArea != null ? noteArea.getText().trim() : "";
    }
}
