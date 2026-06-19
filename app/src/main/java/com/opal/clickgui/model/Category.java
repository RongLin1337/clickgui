package com.opal.clickgui.model;

import java.util.List;

/**
 * 分类模型 — 对应 HTML 中 .cat 元素
 */
public class Category {
    public String name;
    public String icon; // Material Symbols 图标名 (用于映射)
    public List<Module> modules;

    // ---- 布局缓存 ----
    public float layoutX;
    public float layoutY;
    public float layoutWidth;
    public float layoutHeight;

    public Category(String name, String icon, List<Module> modules) {
        this.name = name;
        this.icon = icon;
        this.modules = modules;
    }
}
