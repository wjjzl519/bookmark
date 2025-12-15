package com.dawang.bookmark.ui;

/**
 * 标签节点数据类
 */
public class LabelNode {
    private String label;
    private int count;
    
    public LabelNode(String label, int count) {
        this.label = label;
        this.count = count;
    }
    
    public String getLabel() {
        return label;
    }
    
    public int getCount() {
        return count;
    }
    
    @Override
    public String toString() {
        return label + " (" + count + ")";
    }
}
