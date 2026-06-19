package com.opal.clickgui.model;

/**
 * 模块属性模型 — 对应 HTML 中的 buildProp() 函数处理的四种类型
 *
 * t='bool'  → 开关切换
 * t='num'   → 数值滑块
 * t='mode'  → 模式选择下拉
 * t='theme' → 主题颜色网格
 */
public class Property {
    public static final int TYPE_BOOL = 0;
    public static final int TYPE_NUM = 1;
    public static final int TYPE_MODE = 2;
    public static final int TYPE_THEME = 3;

    public int type;
    public String label;

    // TYPE_BOOL
    public boolean boolVal;
    public float boolAnim = 0f;    // 开关动画进度 (0=关, 1=开)

    // TYPE_NUM
    public double min, max, val, step;
    public double displayVal;      // 显示用的平滑动画值

    // TYPE_MODE
    public String[] modes;
    public int modeIdx;
    public boolean modeOpen; // 下拉展开状态
    public float modeAnim = 0f;    // 下拉展开动画进度 (0=收起, 1=展开)

    // TYPE_THEME — 无额外字段，使用全局 themes 数组

    // ---- 布局缓存 (像素坐标, 由 ClickGUIView 填充) ----
    public float layoutY;
    public float layoutHeight;

    /** 创建 bool 属性 */
    public static Property bool(String label, boolean val) {
        Property p = new Property();
        p.type = TYPE_BOOL;
        p.label = label;
        p.boolVal = val;
        p.boolAnim = val ? 1f : 0f;
        return p;
    }

    /** 创建数值滑块属性 */
    public static Property num(String label, double min, double max, double val, double step) {
        Property p = new Property();
        p.type = TYPE_NUM;
        p.label = label;
        p.min = min;
        p.max = max;
        p.val = val;
        p.step = step;
        p.displayVal = val;
        return p;
    }

    /** 创建模式选择属性 */
    public static Property mode(String label, String[] modes, int curIdx) {
        Property p = new Property();
        p.type = TYPE_MODE;
        p.label = label;
        p.modes = modes;
        p.modeIdx = curIdx;
        p.modeAnim = 0f;
        return p;
    }

    /** 创建主题选择属性 */
    public static Property theme(String label) {
        Property p = new Property();
        p.type = TYPE_THEME;
        p.label = label;
        return p;
    }
}
