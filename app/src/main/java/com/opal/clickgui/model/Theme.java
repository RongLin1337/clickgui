package com.opal.clickgui.model;

import android.graphics.Color;

/**
 * 主题模型 — 对应 HTML 中的 THEMES 数组
 */
public class Theme {
    public String name;
    public int c1; // 主色 (int color)
    public int c2; // 副色 (int color)

    public Theme(String name, String c1Hex, String c2Hex) {
        this.name = name;
        this.c1 = Color.parseColor(c1Hex);
        this.c2 = Color.parseColor(c2Hex);
    }

    /** 从 hex 字符串解析 RGB 分量 */
    public static int[] hexToRgb(int color) {
        return new int[]{
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        };
    }
}
