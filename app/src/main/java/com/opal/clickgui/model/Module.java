package com.opal.clickgui.model;

import java.util.List;

/**
 * 模块模型 — 对应 HTML 中 .mod 元素
 */
public class Module {
    public String name;
    public String categoryName;  // 所属分类名 (用于 ArrayList 颜色)
    public boolean on;          // 开启状态
    public List<Property> props; // 属性列表 (可为空)
    public boolean expanded;     // 属性面板展开状态
    public boolean visible = true; // 搜索过滤时使用

    // ---- 动画进度 ----
    public float expandAnim = 0f;    // 展开/收起动画 (0=收起, 1=展开)
    public float toggleAnim = 0f;    // 开关切换动画 (0=关, 1=开)

    // ---- 布局缓存 ----
    public float layoutY;       // 在分类面板内的 Y 偏移
    public float layoutHeight;  // 当前高度 (含展开的属性)

    // ---- 快捷键相关 ----
    public boolean keyEnabled = false;  // 快捷键是否启用
    public int keyCode = 0;             // 快捷键键码 (0=未设置)
    public float keyX = -1;             // 快捷键悬浮按钮 X 位置 (-1=未初始化)
    public float keyY = -1;             // 快捷键悬浮按钮 Y 位置
    public boolean keyDragging = false; // 是否正在拖动快捷键

    public Module(String name, boolean on, List<Property> props) {
        this.name = name;
        this.on = on;
        this.props = props;
        // 初始化动画目标值
        this.toggleAnim = on ? 1f : 0f;
        this.expandAnim = 0f;
    }

    public boolean hasProps() {
        return props != null && !props.isEmpty();
    }
}
