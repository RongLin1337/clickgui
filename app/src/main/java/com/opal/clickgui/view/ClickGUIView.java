package com.opal.clickgui.view;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.opal.clickgui.model.*;

import java.util.*;

/**
 * ─────────────────────────────────────────────────────────────
 *  Opal v2 ClickGUI — Android Canvas 1:1 还原
 * ─────────────────────────────────────────────────────────────
 *
 *  将 HTML/CSS 实现的 ClickGUI 使用 Canvas 绘制完整还原。
 *  所有视觉元素：动态岛、搜索栏、分类面板、模块行、属性面板
 *  (开关/滑块/模式选择/主题网格)、ArrayList、通知、HUD、
 *  Toggle Hint — 全部在此 View 中绘制和处理交互。
 *
 *  Scale(S): 原始 HTML 像素值 × S = Android 像素值
 */
public class ClickGUIView extends View {

    // ═══════════════════════════════════════════════════════
    //  常量
    // ═══════════════════════════════════════════════════════

    /** 全局缩放因子 — 将 HTML 中的 CSS px 值乘以此值得到 Android px
     *  动态计算，基于屏幕宽度自适应，基准为 360px 屏幕对应 S=3.0f */
    private float S = 3.0f;
    /** 基准设计宽度 (HTML 原始像素) */
    private static final float BASE_DESIGN_WIDTH = 360f;
    /** 最小/最大缩放限制 — 固定合理比例范围，防止 GUI 过大超出屏幕 */
    private static final float MIN_SCALE = 2.0f;
    private static final float MAX_SCALE = 3.2f;

    // 颜色常量 (来自 HTML :root 及各处 rgba)
    private static final int BG_DARK = 0xFF0A0A0A;
    private static final int ISLAND_BG = 0xC7090909;     // rgba(9,9,9,.78)
    private static final int CAT_BG = 0xD9FFFFFF;        // rgba(255,255,255,.85%) → 用深色
    private static final int CAT_BG_COLOR = 0xD90F0F0F;  // rgba(15,15,15,.85)
    private static final int MOD_BG = 0xB31E1E2D;        // rgba(30,30,45,.70)
    private static final int MOD_BG_HOVER = 0xC7262637;  // rgba(38,38,55,.78)
    private static final int PROPS_BG = 0x38000000;      // rgba(0,0,0,.22)
    private static final int AL_BG = 0x80090909;         // rgba(9,9,9,.5)
    private static final int NOTIF_BG = 0x80090909;      // rgba(9,9,9,.5)
    private static final int HINT_BG = 0xA6000000;       // rgba(0,0,0,.65)

    // 尺寸常量 (HTML px × S) — 动态计算，非静态
    private float CAT_W = 118 * 4.0f;
    private float CAT_GAP = 8 * 4.0f;
    private float CAT_RADIUS = 5 * 4.0f;
    private float CAT_HDR_H = 20 * 4.0f;
    private float MOD_H = 20 * 4.0f;
    private float PROP_BOOL_H = 17 * 4.0f;
    private float PROP_SLIDER_H = 15 * 4.0f;
    private float PROP_MODE_MIN_H = 32 * 4.0f;
    private float ISLAND_H = 28 * 4.0f;
    private float ISLAND_RADIUS = 14 * 4.0f;
    private float AL_ROW_H = 12 * 4.0f;
    private float NOTIF_H = 21 * 4.0f;
    private float NOTIF_RADIUS = 8 * 4.0f;

    // 通知类型
    private static final int N_INFO = 0, N_SUCCESS = 1, N_WARNING = 2, N_ERROR = 3;
    private static final int[] N_COLOR = {0xFF2DBFFE, 0xFF43E97B, 0xFFFFA94D, 0xFFD22730};
    private static final String[] N_ICON = {"ℹ", "✓", "⚠", "✕"};
    private static final int[] N_ICON_BG = {
            0x262DBFFE, 0x2643E97B, 0x26FFA94D, 0x26D22730
    };

    // Toggle Hint 动画时序 (ms)
    // HINT_DELAY     — 启动后延迟多久开始显示提示
    // HINT_FADE_IN   — 淡入时长
    // HINT_STAY      — 完全显示停留时长
    // HINT_FADE_OUT  — 淡出时长
    private static final long HINT_DELAY    = 1000L;
    private static final long HINT_FADE_IN  = 300L;
    private static final long HINT_STAY     = 2500L;
    private static final long HINT_FADE_OUT = 500L;

    // ═══════════════════════════════════════════════════════
    //  数据
    // ═══════════════════════════════════════════════════════

    private final Theme[] themes = {
            new Theme("Opal", "#2DBFFE", "#2499CB"),
            new Theme("Spearmint", "#61C2A2", "#41826C"),
            new Theme("Jade Green", "#00A86B", "#006942"),
            new Theme("Green Spirit", "#9FE2BF", "#00873E"),
            new Theme("Rosy Pink", "#FF66CC", "#BF4D99"),
            new Theme("Magenta", "#D53F77", "#9D446E"),
            new Theme("Hot Pink", "#E75480", "#AC4FC6"),
            new Theme("Lavender", "#DBA6F7", "#9873AC"),
            new Theme("Amethyst", "#9063CD", "#62438C"),
            new Theme("Purple Fire", "#B1A2CA", "#68478D"),
            new Theme("Sunset Pink", "#FF9114", "#F569E7"),
            new Theme("Blaze Orange", "#FFA94D", "#FF8200"),
            new Theme("Pink Blood", "#FFA6C9", "#E40046"),
            new Theme("Pastel", "#FF6D6A", "#BF5250"),
            new Theme("Neon Red", "#D22730", "#B8192A"),
            new Theme("Red Coffee", "#E1223B", "#4B1313"),
            new Theme("Deep Ocean", "#3C5291", "#001440"),
            new Theme("Chambray", "#3C5291", "#212EB6"),
            new Theme("Mint Blue", "#429E9D", "#285E5D"),
            new Theme("Pacific", "#05A9C7", "#047387"),
            new Theme("Tropical Ice", "#66FFD1", "#0695FF"),
            new Theme("Blue Purple", "#684DB2", "#043CAE"),
    };
    private int themeIndex = 0;
    private int c1, c2; // 当前主题色

    private final List<Category> categories = new ArrayList<>();

    // ═══════════════════════════════════════════════════════
    //  状态
    // ═══════════════════════════════════════════════════════

    private boolean guiOpen = true;
    private float guiScrollX = 0;
    private long guiOpenTime = 0; // GUI 打开时间戳
    private Module dynamicIslandModule;
    private Module arrayListModule;
    private float guiAnimProgress = 1f; // GUI 打开动画进度 0-1
    private float maxScrollX = 0;

    // 动画时间
    private long startTime;
    private long lastFrameTime;
    private int frameCount;
    private int fps = 0;
    private long fpsLastUpdate;

    // 通知
    private final List<Notif> notifications = new ArrayList<>();

    // 动态岛状态: 0=default 1=scaffold 2=target
    private int islandState = 0;
    private long islandStateTime;
    private int islandBlocks = 64;
    private float islandScaffoldPct = 0.8f;
    private String islandTargetName = "StellaPrincess";
    private float islandTargetHpPct = 0.6f;
    private String islandTargetHpTxt = "12/20";


    // Toggle hint
    private float hintAlpha = 0;
    private boolean hintShown = false;

    // 入场动画
    private float islandAnimProgress = 0;
    private final float[] catAnimProgress = new float[7];

    // 搜索栏

    // ═══════════════════════════════════════════════════════
    //  画笔
    // ═══════════════════════════════════════════════════════

    private final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();
    private final RectF rectF2 = new RectF();

    // ═══════════════════════════════════════════════════════
    //  触摸状态
    // ═══════════════════════════════════════════════════════

    private float lastTouchX, lastTouchY;
    private boolean isScrolling = false;
    private long touchDownTime;
    private float touchStartX, touchStartY;
    private float SCROLL_THRESHOLD = 10 * 4.0f;

    // 滑块拖拽状态
    private int dragSliderCatIdx = -1;
    private int dragSliderModIdx = -1;
    private int dragSliderPropIdx = -1;

    // 悬浮球 (FAB) — 替代三击顶部打开 GUI
    private static final float FAB_SIZE = 36f; // dp, 会乘以 S，调小尺寸
    private static final long FAB_LONG_PRESS_MS = 300L; // 长按触发拖动的时间阈值
    private float fabX = 0, fabY = 0;
    private boolean isDraggingFab = false;
    private float fabDownX, fabDownY;
    private long fabDownTime = 0;
    private boolean fabLongPressTriggered = false;
    private float fabAnimProgress = 1f; // 1=完全显示, 0=隐藏
    private boolean fabPressed = false;

    // 快捷键按钮
    private static final float KEY_BTN_W = 50f;  // 快捷键按钮宽度 dp
    private static final float KEY_BTN_H = 24f;  // 快捷键按钮高度 dp
    private static final float KEY_BTN_RADIUS = 12f; // 快捷键按钮圆角 dp
    private static final long MODULE_LONG_PRESS_MS = 400L; // 模块长按触发时间
    private static final long KEY_LONG_PRESS_MS = 300L; // 快捷键长按触发时间
    // 快捷键长按状态
    private Module longPressKeyModule = null;
    private float longPressKeyX = 0, longPressKeyY = 0;
    private boolean keyLongPressTriggered = false;
    private float keyScaleAnim = 1f; // 快捷键缩放动画
    // 模块长按检测状态
    private int longPressCatIdx = -1;
    private int longPressModIdx = -1;
    private float longPressX = 0, longPressY = 0;
    private boolean longPressTriggered = false;
    // 快捷键拖动状态
    private Module draggingKeyModule = null;
    private float dragKeyOffsetX = 0, dragKeyOffsetY = 0;

    // 控制台按钮

    // ArrayList 动画状态
    private final java.util.HashMap<String, Float> alEnterAnim = new java.util.HashMap<>();
    private final java.util.HashMap<String, Float> alCurrentY = new java.util.HashMap<>();
    // 背景图片
    private Bitmap backgroundBitmap = null;
    private boolean backgroundImageEnabled = false;
    // 垂直滚动
    private float guiScrollY = 0;
    private float maxScrollY = 0;

    // ═══════════════════════════════════════════════════════
    //  初始化
    // ═══════════════════════════════════════════════════════

    public ClickGUIView(Context context) {
        super(context);
        init();
    }

    public ClickGUIView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        startTime = System.currentTimeMillis();
        lastFrameTime = startTime;
        fpsLastUpdate = startTime;

        setTheme(0);
        initData();

        // 开启动画循环
        setWillNotDraw(false);
        postInvalidate();
    }

    private void setTheme(int idx) {
        themeIndex = idx;
        c1 = themes[idx].c1;
        c2 = themes[idx].c2;
    }

    /** 设置搜索 EditText (由 Activity 传入隐藏的 EditText) */
    //  数据初始化 — 完整复制 HTML 中的 CATS 数组
    // ═══════════════════════════════════════════════════════

    private void initData() {
        // ── Combat ──
        categories.add(new Category("Combat", "⚔", Arrays.asList(
                new Module("Anti Bot", false, Collections.emptyList()),
                new Module("Anti Fireball", false, Collections.emptyList()),
                new Module("Attack Delay", false, Collections.singletonList(
                        Property.num("Delay", 1, 10, 3, 0.5))),
                new Module("Auto Clicker", false, Arrays.asList(
                        Property.num("CPS Min", 5, 25, 12, 1),
                        Property.num("CPS Max", 5, 25, 16, 1))),
                new Module("Auto Head", false, Collections.emptyList()),
                new Module("Backtrack", false, Collections.singletonList(
                        Property.num("Ms", 50, 500, 100, 10))),
                new Module("Block", false, Collections.emptyList()),
                new Module("Criticals", false, Collections.singletonList(
                        Property.mode("Mode", new String[]{"Packet", "Jump"}, 0))),
                new Module("Kill Aura", false, Arrays.asList(
                        Property.num("Range", 2, 6, 3.5, 0.05),
                        Property.mode("Mode", new String[]{"Single", "Switch", "Multi"}, 0))),
                new Module("Piercing", false, Collections.emptyList()),
                new Module("Reach", true, Collections.singletonList(
                        Property.num("Range", 3, 6, 3.2, 0.05))),
                new Module("Velocity", false, Arrays.asList(
                        Property.mode("Mode", new String[]{"Normal", "Watchdog", "MushMC"}, 0),
                        Property.num("Horizontal", 0, 100, 0, 1),
                        Property.num("Vertical", 0, 100, 0, 1)))
        )));

        // ── Movement ──
        categories.add(new Category("Movement", "➤", Arrays.asList(
                new Module("Clipper", false, Collections.emptyList()),
                new Module("Fast Stop", false, Collections.emptyList()),
                new Module("Flight", false, Collections.singletonList(
                        Property.mode("Mode", new String[]{"Vanilla", "AirWalk", "Fireball"}, 0))),
                new Module("Inventory Move", true, Collections.emptyList()),
                new Module("Jump Cooldown", false, Collections.emptyList()),
                new Module("Long Jump", false, Collections.emptyList()),
                new Module("Movement Fix", false, Collections.emptyList()),
                new Module("No Slow", true, Collections.singletonList(
                        Property.mode("Mode", new String[]{"Watchdog", "Vanilla", "Universal"}, 0))),
                new Module("Phase", false, Collections.emptyList()),
                new Module("Physics", false, Collections.emptyList()),
                new Module("Safe Walk", false, Collections.emptyList()),
                new Module("Speed", false, Collections.singletonList(
                        Property.mode("Mode", new String[]{"Strafe", "Vanilla", "MushMC"}, 0))),
                new Module("Spider", false, Collections.emptyList()),
                new Module("Sprint", true, Collections.singletonList(
                        Property.bool("Omni", false))),
                new Module("Strafe", false, Collections.emptyList()),
                new Module("Target Strafe", false, Collections.emptyList())
        )));

        // ── Visual ──
        categories.add(new Category("Visual", "◉", Arrays.asList(
                new Module("Ambience", false, Collections.emptyList()),
                new Module("Animations", true, Collections.emptyList()),
                new Module("Dynamic Island", true, Collections.emptyList()),
                new Module("ArrayList", true, Collections.emptyList()),
                new Module("Attack Effects", false, Collections.emptyList()),
                new Module("Break Progress", false, Collections.emptyList()),
                new Module("Cape", false, Collections.emptyList()),
                new Module("Chams", false, Collections.emptyList()),
                new Module("Click GUI", true, Collections.emptyList()),
                new Module("Discord RPC", false, Collections.emptyList()),
                new Module("ESP", false, Arrays.asList(
                        Property.bool("Box", true),
                        Property.bool("Health Bar", true),
                        Property.bool("Name Tag", true))),
                new Module("Full Bright", true, Collections.emptyList()),
                new Module("No Hurt Camera", false, Collections.emptyList()),
                new Module("Overlay", true, Collections.emptyList()),
                new Module("Post Processing", true, Collections.emptyList()),
                new Module("Streamer Mode", false, Collections.emptyList()),
                new Module("Tab GUI", false, Collections.emptyList()),
                new Module("Theme", true, Collections.singletonList(
                        Property.theme("Color")))
        )));

        // ── World ──
        categories.add(new Category("World", "◐", Arrays.asList(
                new Module("Breaker", false, Collections.emptyList()),
                new Module("Fast Break", false, Collections.emptyList()),
                new Module("Scaffold", false, Collections.singletonList(
                        Property.mode("Mode", new String[]{"Watchdog", "AntiGC", "Vanilla", "Bloxd"}, 0))),
                new Module("Timer", false, Collections.singletonList(
                        Property.num("Speed", 0.5, 10, 1, 0.1)))
        )));

        // ── Player ──
        categories.add(new Category("Player", "♟", Arrays.asList(
                new Module("Anti Void", false, Collections.emptyList()),
                new Module("Auto Armor", false, Collections.emptyList()),
                new Module("Auto Chest", false, Collections.emptyList()),
                new Module("Auto Hypixel", false, Collections.emptyList()),
                new Module("Auto Tool", true, Collections.emptyList()),
                new Module("Blink", false, Collections.singletonList(
                        Property.num("Limit", 1, 300, 20, 1))),
                new Module("Chest Stealer", false, Collections.emptyList()),
                new Module("Disabler", true, Collections.singletonList(
                        Property.mode("Mode", new String[]{"Watchdog", "CubeCraft"}, 0))),
                new Module("Fast Use", false, Collections.emptyList()),
                new Module("Inventory Manager", false, Collections.emptyList()),
                new Module("IRC", false, Collections.emptyList()),
                new Module("No Fall", true, Collections.singletonList(
                        Property.mode("Mode", new String[]{"Spoof", "Watchdog"}, 0))),
                new Module("No Rotate", false, Collections.emptyList()),
                new Module("Party Spam", false, Collections.emptyList()),
                new Module("Spammer", false, Collections.emptyList())
        )));

        // ── Exploit ──
        categories.add(new Category("Exploit", "✦", Arrays.asList(
                new Module("Disabler", false, Collections.singletonList(
                        Property.mode("Mode", new String[]{"Watchdog", "Verus"}, 0))),
                new Module("Fake Hand", false, Collections.emptyList()),
                new Module("Fast Use", false, Collections.emptyList()),
                new Module("Party Spam", false, Collections.emptyList()),
                new Module("Velocity Stack", false, Collections.emptyList())
        )));

        // ── Other ──
        categories.add(new Category("Other", "⋯", Arrays.asList(
                new Module("Anti AFK", false, Collections.emptyList()),
                new Module("Auto Hypixel", false, Collections.emptyList()),
                new Module("Blink", false, Collections.emptyList()),
                new Module("Discord RPC", false, Collections.emptyList())
        )));

        // 模块按名称排序 (与 HTML 一致)
        for (Category cat : categories) {
            Collections.sort(cat.modules, (a, b) -> a.name.compareToIgnoreCase(b.name));
            // 设置每个模块的分类名 (用于 ArrayList 颜色)
            for (Module m : cat.modules) {
                m.categoryName = cat.name;

            }
        }

        // 缓存常用模块引用
        for (Category c : categories) {
            if ("Visual".equals(c.name)) {
                for (Module m : c.modules) {
                    if ("Dynamic Island".equals(m.name)) {
                        dynamicIslandModule = m;
                    } else if ("ArrayList".equals(m.name)) {
                        arrayListModule = m;
                    }
                }
            }
        }
    }
    //  布局计算
    // ═══════════════════════════════════════════════════════

    private float screenW, screenH;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenW = w;
        screenH = h;

        // 动态计算缩放因子 — 基于屏幕宽度自适应，限制在合理范围内
        // 基准: 360px 设计宽度对应 S=2.8f，最大不超过 3.2f，防止 GUI 过大超出屏幕
        float scaleFactor = Math.min(w, h) / BASE_DESIGN_WIDTH;
        S = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scaleFactor * 2.8f));

        // 更新所有依赖 S 的尺寸常量
        CAT_W = 118 * S;
        CAT_GAP = 8 * S;
        CAT_RADIUS = 5 * S;
        CAT_HDR_H = 20 * S;
        MOD_H = 20 * S;
        PROP_BOOL_H = 17 * S;
        PROP_SLIDER_H = 15 * S;
        PROP_MODE_MIN_H = 32 * S;
        ISLAND_H = 28 * S;
        ISLAND_RADIUS = 14 * S;
        AL_ROW_H = 12 * S;
        NOTIF_H = 21 * S;
        NOTIF_RADIUS = 8 * S; // 增大圆角，让通知看起来不那么方
        SCROLL_THRESHOLD = 10 * S;

        // 初始化悬浮球位置 (右侧中间)
        if (fabX == 0 && fabY == 0) {
            fabX = screenW - FAB_SIZE - 8 * S;
            fabY = screenH / 2f;
        }

        updateMaxScroll();
        updateMaxScrollY();
    }

    /** 更新分类面板的最大滚动范围 — 左对齐滚动，确保最后一个面板完全可见 */
    private void updateMaxScroll() {
        float totalW = categories.size() * CAT_W + (categories.size() - 1) * CAT_GAP;
        float sidePadding = 12 * S; // 左右边距
        maxScrollX = Math.max(0, totalW - screenW + sidePadding * 2);
        if (guiScrollX > maxScrollX) guiScrollX = maxScrollX;
        if (guiScrollX < 0) guiScrollX = 0;
    }

    /** 更新垂直滚动的最大范围 */
    private void updateMaxScrollY() {
        float maxHeight = 0;
        for (Category cat : categories) {
            calcCategoryLayout(cat);
            if (cat.layoutHeight > maxHeight) {
                maxHeight = cat.layoutHeight;
            }
        }
        float visibleH = screenH - 86 * S - 10 * S; // 减去顶部搜索栏和底部边距
        maxScrollY = Math.max(0, maxHeight - visibleH);
        if (guiScrollY > maxScrollY) guiScrollY = maxScrollY;
        if (guiScrollY < 0) guiScrollY = 0;
    }

    /** 计算分类面板内各模块和属性的 Y 偏移和高度 */
    private void calcCategoryLayout(Category cat) {
        float y = CAT_HDR_H;
        for (Module m : cat.modules) {
            if (!m.visible) {
                m.layoutY = y;
                m.layoutHeight = 0;
                continue;
            }
            m.layoutY = y;
            float h = MOD_H;
            // 使用动画进度计算实际显示高度
            if (m.hasProps() && m.expandAnim > 0.01f) {
                float propsHeight = 0;
                for (int i = 0; i < m.props.size(); i++) {
                    Property p = m.props.get(i);
                    p.layoutY = y + h + propsHeight;
                    float ph = getPropertyHeight(p);
                    p.layoutHeight = ph;
                    propsHeight += ph;
                }
                h += propsHeight * m.expandAnim;
            }
            m.layoutHeight = h;
            y += h;
        }
        cat.layoutHeight = y;
    }

    private float getPropertyHeight(Property p) {
        switch (p.type) {
            case Property.TYPE_BOOL: return PROP_BOOL_H;
            case Property.TYPE_NUM: return PROP_SLIDER_H;
            case Property.TYPE_MODE: {
                float h = PROP_MODE_MIN_H;
                if (p.modeOpen) {
                    h += p.modes.length * (9 * S); // 每个选项高度
                }
                return h;
            }
            case Property.TYPE_THEME: {
                int rows = (themes.length + 3) / 4; // 4列
                return 6 * S + rows * (CAT_W / 4 - 2 * S); // padding + grid
            }
            default: return PROP_BOOL_H;
        }
    }

    // ═══════════════════════════════════════════════════════
    //  绘制主循环
    // ═══════════════════════════════════════════════════════

    @Override
    protected void onDraw(Canvas canvas) {
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        // FPS 计算
        frameCount++;
        if (now - fpsLastUpdate >= 1000) {
            fps = frameCount;
            frameCount = 0;
            fpsLastUpdate = now;
        }

        // 更新入场动画进度
        if (elapsed > 200) islandAnimProgress = Math.min(1, (elapsed - 200) / 500f);
        // 更新 GUI 打开动画
        if (guiOpen && guiAnimProgress < 1f) {
            long guiElapsed = System.currentTimeMillis() - guiOpenTime;
            guiAnimProgress = Math.min(1f, guiElapsed / 300f); // 300ms 动画时长
        }

        if (guiOpen) {
            long guiElapsed = System.currentTimeMillis() - guiOpenTime;
            // 从左至右依次展开: 每个分类延迟 80ms, 动画时长 400ms
            for (int i = 0; i < categories.size() && i < 7; i++) {
                float delay = 50 + i * 80; // ms — 从左到右逐个出场
                float raw = Math.min(1, Math.max(0, (guiElapsed - delay) / 400f));
                catAnimProgress[i] = easeOutBack(raw);
            }
        }
        updateAnimations();

        // 更新动态岛状态 (5s/10s/15s 循环)
        updateIslandState(elapsed);

        // 更新通知
        updateNotifications(now);

        // 更新 Toggle hint — 平滑滑入/停留/滑出
        updateHint(elapsed);

        // 触发定时通知 (对应 HTML 中的 setTimeout)
        triggerScheduledEvents(elapsed);

        // ── 绘制各层 ──
        drawBackground(canvas);
        drawHUD(canvas);
        if (arrayListModule != null && arrayListModule.on) drawArrayList(canvas, now);
        if (guiOpen) {
            // 半透明黑色遮罩
            int maskAlpha = (int) (0x80 * guiAnimProgress); // 50% 透明度，带动画
            pFill.setColor(0x00000000 | (maskAlpha << 24));
            canvas.drawRect(0, 0, screenW, screenH, pFill);
            drawCategories(canvas, now);
        }
        drawNotifications(canvas);
        if (dynamicIslandModule != null && dynamicIslandModule.on) drawDynamicIsland(canvas, now);
        if (hintAlpha > 0.01f) {
            drawToggleHint(canvas);
        }

        // 快捷键按钮
        drawKeyBindings(canvas);

        // 悬浮球 (始终显示在最上层)
        drawFAB(canvas);


        // 持续重绘 (≈60fps)
        postInvalidateOnAnimation();
    }

    // ═══════════════════════════════════════════════════════
    //  绘制：背景
    // ═══════════════════════════════════════════════════════

    private void drawBackground(Canvas canvas) {
        // 背景图片
        if (backgroundImageEnabled && backgroundBitmap != null) {
            // 计算缩放比例，保持图片比例并填满屏幕
            float bitmapW = backgroundBitmap.getWidth();
            float bitmapH = backgroundBitmap.getHeight();
            float scale = Math.max(screenW / bitmapW, screenH / bitmapH);
            float drawW = bitmapW * scale;
            float drawH = bitmapH * scale;
            float drawX = (screenW - drawW) / 2;
            float drawY = (screenH - drawH) / 2;

            rectF.set(drawX, drawY, drawX + drawW, drawY + drawH);
            canvas.drawBitmap(backgroundBitmap, null, rectF, null);

            // 叠加一层淡淡的黑色，让文字更清晰
            pFill.setColor(0x40000000);
            canvas.drawRect(0, 0, screenW, screenH, pFill);
        } else {
            // 基础深色背景
            canvas.drawColor(BG_DARK);

            // 径向渐变 (绿色调氛围光)
            float w = screenW, h = screenH;

            // 底部主光
            pFill.reset();
            pFill.setAntiAlias(false);
            RadialGradient rg1 = new RadialGradient(
                    w / 2, h,
                    Math.max(w, h) * 0.7f,
                    new int[]{0x80193714, 0x00193714},
                    new float[]{0f, 0.7f},
                    Shader.TileMode.CLAMP
            );
            pFill.setShader(rg1);
            canvas.drawRect(0, h * 0.4f, w, h, pFill);

            // 左下角光
            pFill.reset();
            pFill.setAntiAlias(false);
            RadialGradient rg2 = new RadialGradient(
                    w * 0.15f, h * 0.85f,
                    Math.max(w, h) * 0.35f,
                    new int[]{0x59122D0F, 0x00122D0F},
                    new float[]{0f, 0.55f},
                    Shader.TileMode.CLAMP
            );
            pFill.setShader(rg2);
            canvas.drawRect(0, 0, w, h, pFill);

            // 右下角光
            pFill.reset();
            pFill.setAntiAlias(false);
            RadialGradient rg3 = new RadialGradient(
                    w * 0.85f, h * 0.9f,
                    Math.max(w, h) * 0.4f,
                    new int[]{0x4D14320C, 0x0014320C},
                    new float[]{0f, 0.6f},
                    Shader.TileMode.CLAMP
            );
            pFill.setShader(rg3);
            canvas.drawRect(0, 0, w, h, pFill);

            pFill.setShader(null);
        }
    }
    // ═══════════════════════════════════════════════════════

    private void drawHUD(Canvas canvas) {
        float x = 4 * S;
        float y = screenH - 4 * S;
        float lineH = 8 * S * 1.5f;

        String[] lines = {
                "FPS: " + fps,
                "BPS: 0.0",
                "XYZ: 5598 4 -9596"
        };

        pText.setTextAlign(Paint.Align.LEFT);
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];
            float lineY = y - (lines.length - 1 - i) * lineH;

            // 阴影
            pText.setColor(0xA6000000);
            pText.setTextSize(8 * S);
            pText.setTypeface(Typeface.DEFAULT);
            drawTextWithShadow(canvas, line, x, lineY, 0xA5FFFFFF, 8 * S);

            // 高亮值 (FPS数字等)
            int colonIdx = line.indexOf(':');
            if (colonIdx >= 0) {
                String val = line.substring(colonIdx + 1);
                float valX = x + pText.measureText(line.substring(0, colonIdx + 1));
                drawTextWithShadow(canvas, val, valX, lineY, c1, 8 * S);
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  绘制：ArrayList (右上角 — 已启用模块列表)
    // ═══════════════════════════════════════════════════════

    private void drawArrayList(Canvas canvas, long now) {
        // 收集已启用的模块, 按实际文字宽度降序排列（视觉上从长到短）
        List<Module> enabled = new ArrayList<>();
        for (Category cat : categories) {
            for (Module m : cat.modules) {
                if (m.on) enabled.add(m);
            }
        }

        if (enabled.isEmpty()) {
            // 清空动画状态
            alEnterAnim.clear();
            alCurrentY.clear();
            return;
        }

        // 按实际像素宽度排序，而不是字符数
        pText.setTextSize(10 * S);
        pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        final Paint measurePaint = pText;
        enabled.sort((a, b) -> Float.compare(measurePaint.measureText(b.name), measurePaint.measureText(a.name)));

        float rightX = screenW - 8 * S;
        float startY = 16 * S;
        float rowH = AL_ROW_H + 2 * S;
        float cornerRadius = 4 * S; // 优化圆角，稍微大一点

        int totalCount = enabled.size();

        // 第一步：计算目标位置，更新动画状态
        float[] targetY = new float[totalCount];
        for (int i = 0; i < totalCount; i++) {
            Module m = enabled.get(i);
            targetY[i] = startY + i * rowH;

            // 初始化新出现的模块
            if (!alEnterAnim.containsKey(m.name)) {
                alEnterAnim.put(m.name, 0f);
                alCurrentY.put(m.name, targetY[i]); // 初始位置就是目标位置，避免跳变
            }

            // 更新入场动画进度（非线性，easeOutCubic）
            float enterProgress = alEnterAnim.get(m.name);
            if (enterProgress < 1f) {
                enterProgress += 0.04f; // 动画速度
                if (enterProgress > 1f) enterProgress = 1f;
                alEnterAnim.put(m.name, enterProgress);
            }

            // 更新 Y 位置，平滑过渡到目标位置（非线性插值）
            float currentY = alCurrentY.get(m.name);
            float diff = targetY[i] - currentY;
            if (Math.abs(diff) > 0.1f) {
                currentY += diff * 0.15f; // 平滑系数，越大越快
                alCurrentY.put(m.name, currentY);
            }
        }

        // 清理不再显示的模块的动画状态
        java.util.HashSet<String> currentNames = new java.util.HashSet<>();
        for (Module m : enabled) currentNames.add(m.name);
        alEnterAnim.keySet().retainAll(currentNames);
        alCurrentY.keySet().retainAll(currentNames);

        // 第二步：绘制所有条目
        for (int i = 0; i < totalCount; i++) {
            Module m = enabled.get(i);
            String name = m.name;

            float enterProgress = alEnterAnim.get(name);
            float easedEnter = easeOutCubic(enterProgress); // 非线性缓动
            float rowY = alCurrentY.get(name);

            // 计算底部透明度渐变 — 越靠下越透明
            float alphaMultiplier = 1f;
            if (totalCount > 3) {
                float fadeStart = totalCount * 0.6f;
                if (i >= fadeStart) {
                    alphaMultiplier = 1f - (i - fadeStart) / (totalCount - fadeStart) * 0.6f;
                    alphaMultiplier = Math.max(0.2f, alphaMultiplier);
                }
            }
            // 乘入场动画的透明度
            alphaMultiplier *= easedEnter;

            // 计算颜色 (动画插值) — 主题色渐变
            int color = interpolateColorsBackAndForth(6, i * 20, now, c1, c2);

            // 测量文字宽度
            float textW = measurePaint.measureText(name);
            float paddingH = 8 * S;
            float rowW = textW + paddingH * 2;

            // 水平入场偏移 — 从右往左进入
            float offsetX = (1 - easedEnter) * (rowW + 20 * S);

            float leftX = rightX - rowW + offsetX;
            float drawRightX = rightX + offsetX;

            // 背景 — 半透明黑色（降低透明度）
            int bgAlpha = (int) (0x66 * alphaMultiplier); // 从 0xB2 降到 0x66，约 40% 透明度
            pFill.setColor(0x00000000 | (bgAlpha << 24));
            pFill.setShadowLayer(4 * S, 0, 2 * S, 0x80000000);

            // 根据位置决定圆角
            boolean isFirst = (i == 0);
            boolean isLast = (i == totalCount - 1);
            boolean isSingle = (totalCount == 1);

            rectF.set(leftX, rowY, drawRightX, rowY + rowH);

            if (isSingle) {
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, pFill);
            } else if (isFirst) {
                Path path = new Path();
                path.moveTo(leftX + cornerRadius, rowY);
                path.lineTo(drawRightX - cornerRadius, rowY);
                path.quadTo(drawRightX, rowY, drawRightX, rowY + cornerRadius);
                path.lineTo(drawRightX, rowY + rowH);
                path.lineTo(leftX, rowY + rowH);
                path.lineTo(leftX, rowY + cornerRadius);
                path.quadTo(leftX, rowY, leftX + cornerRadius, rowY);
                path.close();
                canvas.drawPath(path, pFill);
            } else if (isLast) {
                Path path = new Path();
                path.moveTo(leftX, rowY);
                path.lineTo(drawRightX, rowY);
                path.lineTo(drawRightX, rowY + rowH - cornerRadius);
                path.quadTo(drawRightX, rowY + rowH, drawRightX - cornerRadius, rowY + rowH);
                path.lineTo(leftX + cornerRadius, rowY + rowH);
                path.quadTo(leftX, rowY + rowH, leftX, rowY + rowH - cornerRadius);
                path.close();
                canvas.drawPath(path, pFill);
            } else {
                canvas.drawRect(rectF, pFill);
            }

            pFill.setShadowLayer(0, 0, 0, 0);

            // 文字 — 主题色渐变 + 右对齐
            int textAlpha = (int) (0xFF * alphaMultiplier);
            pText.setColor(color);
            pText.setAlpha(textAlpha);
            pText.setTextAlign(Paint.Align.RIGHT);
            float textX = drawRightX - paddingH;
            canvas.drawText(name, textX, rowY + rowH * 0.68f, pText);
        }
    }
    //  绘制：搜索栏
    // ═══════════════════════════════════════════════════════


    // ═══════════════════════════════════════════════════════
    //  绘制：分类面板 + 模块 + 属性
    // ═══════════════════════════════════════════════════════

    private void drawCategories(Canvas canvas, long now) {
        float totalW = categories.size() * CAT_W + (categories.size() - 1) * CAT_GAP;
        float sidePadding = 12 * S; // 左右边距，与 updateMaxScroll 保持一致
        float startX = sidePadding - guiScrollX; // 左对齐滚动
        float catY = 86 * S - guiScrollY;

        canvas.save();
        // 裁剪区域，防止面板超出屏幕
        rectF.set(0, catY - 5 * S, screenW, screenH);
        canvas.clipRect(rectF);

        for (int ci = 0; ci < categories.size(); ci++) {
            Category cat = categories.get(ci);
            cat.layoutX = startX + ci * (CAT_W + CAT_GAP);
            cat.layoutY = catY;

            // 计算可见模块
            long visibleCount = cat.modules.stream().filter(m -> m.visible).count();
            if (visibleCount == 0) continue;

            calcCategoryLayout(cat);

            float animP = ci < 7 ? catAnimProgress[ci] : 1;
            float catAlpha = Math.min(1, animP * 1.5f); // 透明度先到
            // 从左至右滑入: 初始偏移 -CAT_W*0.8, 稍微带回弹
            float catOffsetX = (1 - animP) * (-CAT_W * 0.8f);
            float catOffsetY = (1 - animP) * 8 * S;

            // 用 canvas.translate 实现整体偏移, 子元素坐标不用改
            canvas.save();
            canvas.translate(catOffsetX, catOffsetY);

            // 分类面板背景
            pFill.setColor(CAT_BG_COLOR);
            pFill.setAlpha((int)(0xD9 * catAlpha));
            rectF.set(cat.layoutX, cat.layoutY,
                      cat.layoutX + CAT_W, cat.layoutY + cat.layoutHeight);
            canvas.drawRoundRect(rectF, CAT_RADIUS, CAT_RADIUS, pFill);

            // 内部高光 (顶部)
            pFill.setColor(0x08FFFFFF);
            pFill.setAlpha((int)(0x08 * catAlpha));
            rectF2.set(cat.layoutX, cat.layoutY,
                       cat.layoutX + CAT_W, cat.layoutY + CAT_HDR_H);
            canvas.drawRoundRect(rectF2, CAT_RADIUS, CAT_RADIUS, pFill);

            // 边框
            pStroke.setColor(0x0DFFFFFF);
            pStroke.setAlpha((int)(0x0D * catAlpha));
            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setStrokeWidth(1 * S);
            rectF.set(cat.layoutX, cat.layoutY,
                      cat.layoutX + CAT_W, cat.layoutY + cat.layoutHeight);
            canvas.drawRoundRect(rectF, CAT_RADIUS, CAT_RADIUS, pStroke);
            pStroke.setStyle(Paint.Style.FILL);

            // 分类标题
            float hdrY = cat.layoutY;
            pText.setTextAlign(Paint.Align.LEFT);
            pText.setTextSize(9 * S);
            pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            pText.setColor(0xE6FFFFFF);
            pText.setAlpha((int)(0xE6 * catAlpha));
            canvas.drawText(cat.name, cat.layoutX + 7 * S, hdrY + CAT_HDR_H * 0.7f, pText);

            // 分类图标
            pText.setTextAlign(Paint.Align.RIGHT);
            pText.setTextSize(11 * S);
            pText.setTypeface(Typeface.DEFAULT);
            pText.setColor(0x73FFFFFF);
            pText.setAlpha((int)(0x73 * catAlpha));
            canvas.drawText(cat.icon, cat.layoutX + CAT_W - 6 * S, hdrY + CAT_HDR_H * 0.7f, pText);

            // 底部分隔线
            pStroke.setColor(0x08FFFFFF);
            pStroke.setAlpha((int)(0x08 * catAlpha));
            pStroke.setStrokeWidth(1 * S);
            canvas.drawLine(cat.layoutX, hdrY + CAT_HDR_H,
                            cat.layoutX + CAT_W, hdrY + CAT_HDR_H, pStroke);
            pStroke.setStyle(Paint.Style.FILL);

            // 模块列表
            for (int mi = 0; mi < cat.modules.size(); mi++) {
                Module m = cat.modules.get(mi);
                if (!m.visible) continue;
                drawModule(canvas, cat, m, ci, mi, catAlpha, 0); // offsetY=0, translate 已处理
            }

            canvas.restore();
        }

        canvas.restore();
    }

    private void drawModule(Canvas canvas, Category cat, Module m,
                            int catIdx, int modIdx, float catAlpha, float catOffsetY) {
        float modX = cat.layoutX;
        float modY = cat.layoutY + m.layoutY + catOffsetY;
        float modW = CAT_W;
        boolean isLast = (modIdx == cat.modules.size() - 1) ||
                cat.modules.subList(modIdx + 1, cat.modules.size())
                        .stream().noneMatch(mm -> mm.visible);

        // 模块背景
        pFill.setColor(MOD_BG);
        pFill.setAlpha((int)(0xB3 * catAlpha));
        if (isLast) {
            rectF.set(modX, modY, modX + modW, modY + MOD_H);
            canvas.drawRoundRect(rectF, 0, 0, pFill);
            // 底部圆角
            rectF.set(modX, modY, modX + modW, modY + MOD_H + CAT_RADIUS);
            canvas.drawRoundRect(rectF, 0, 0, pFill);
            // 只保留底部圆角 — 重新画一个方角覆盖上面
            rectF.set(modX, modY, modX + modW, modY + MOD_H);
            canvas.drawRect(rectF, pFill);
        } else {
            rectF.set(modX, modY, modX + modW, modY + MOD_H);
            canvas.drawRect(rectF, pFill);
        }

        // 开启时的渐变叠加 — 使用动画进度平滑过渡
        if (m.toggleAnim > 0.01f) {
            LinearGradient grad = new LinearGradient(
                    modX, modY, modX + modW, modY,
                    c1, c2, Shader.TileMode.CLAMP
            );
            pFill.setShader(grad);
            pFill.setAlpha((int)(0x66 * catAlpha * m.toggleAnim)); // 0.40 * 动画进度
            rectF.set(modX, modY, modX + modW, modY + MOD_H);
            canvas.drawRect(rectF, pFill);
            pFill.setShader(null);
        }

        // 底部分隔线
        if (!isLast) {
            pStroke.setColor(0x06FFFFFF);
            pStroke.setAlpha((int)(0x06 * catAlpha));
            pStroke.setStrokeWidth(1 * S);
            canvas.drawLine(modX, modY + MOD_H, modX + modW, modY + MOD_H, pStroke);
        }

        // 模块名称
        pText.setTextAlign(Paint.Align.LEFT);
        pText.setTextSize(8 * S);
        if (m.on) {
            pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            pText.setColor(0xFFFFFFFF);
        } else {
            pText.setTypeface(Typeface.DEFAULT);
            pText.setColor(0xFFCCCCCC);
        }
        pText.setAlpha((int)(0xFF * catAlpha));
        canvas.drawText(m.name, modX + 7 * S, modY + MOD_H * 0.68f, pText);

        // 展开箭头 (有属性时显示)
        if (m.hasProps()) {
            float arrowX = modX + modW - 17 * S / 2 - 8.5f * S;
            float arrowY = modY + MOD_H / 2;
            pText.setTextAlign(Paint.Align.CENTER);
            pText.setTextSize(12 * S);
            pText.setTypeface(Typeface.DEFAULT);
            pText.setColor(0xB3FFFFFF);
            pText.setAlpha((int)(0xB3 * catAlpha));

            // 旋转动画 (展开时旋转180°) — 使用动画进度平滑过渡
            canvas.save();
            canvas.rotate(m.expandAnim * 180, arrowX + 8.5f * S, arrowY);
            // 画 chevron (expand_more)
            pStroke.setColor(0xB3FFFFFF);
            pStroke.setAlpha((int)(0xB3 * catAlpha));
            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setStrokeWidth(1.5f * S);
            pStroke.setStrokeCap(Paint.Cap.ROUND);
            float cx = arrowX + 8.5f * S;
            float cy = arrowY;
            float sz = 4 * S;
            canvas.drawLine(cx - sz, cy - sz/2, cx, cy + sz/2, pStroke);
            canvas.drawLine(cx, cy + sz/2, cx + sz, cy - sz/2, pStroke);
            pStroke.setStyle(Paint.Style.FILL);
            pStroke.setStrokeCap(Paint.Cap.BUTT);
            canvas.restore();
        }

        // 绘制属性 (展开时) — 使用动画进度控制显示
        if (m.expandAnim > 0.01f && m.hasProps()) {
            for (int pi = 0; pi < m.props.size(); pi++) {
                Property p = m.props.get(pi);
                drawProperty(canvas, cat, m, p, catIdx, modIdx, pi, modX,
                        cat.layoutY + p.layoutY + catOffsetY, modW, catAlpha);
            }
        }
    }

    private void drawProperty(Canvas canvas, Category cat, Module m, Property p,
                              int catIdx, int modIdx, int propIdx,
                              float propX, float propY, float propW, float catAlpha) {
        // 属性区背景
        pFill.setColor(PROPS_BG);
        pFill.setAlpha((int)(0x38 * catAlpha));

        switch (p.type) {
            case Property.TYPE_BOOL: {
                rectF.set(propX, propY, propX + propW, propY + PROP_BOOL_H);
                canvas.drawRect(rectF, pFill);

                // 标签
                pText.setTextAlign(Paint.Align.LEFT);
                pText.setTextSize(7 * S);
                pText.setTypeface(Typeface.DEFAULT);
                pText.setColor(0x80FFFFFF);
                pText.setAlpha((int)(0x80 * catAlpha));
                Paint.FontMetrics fm = pText.getFontMetrics();
                float textY = propY + PROP_BOOL_H / 2 - (fm.top + fm.bottom) / 2;
                canvas.drawText(p.label, propX + 7 * S, textY, pText);

                // 开关 (Toggle)
                float togW = 17 * S;
                float togH = 8.5f * S;
                float togX = propX + propW - 7 * S - togW;
                float togY = propY + (PROP_BOOL_H - togH) / 2;
                float togRadius = togH / 2;

                // 开关背景 — 使用动画进度平滑过渡
                if (p.boolAnim > 0.01f) {
                    LinearGradient grad = new LinearGradient(
                            togX, togY, togX + togW, togY,
                            c2, c1, Shader.TileMode.CLAMP
                    );
                    pFill.setShader(grad);
                    pFill.setAlpha((int)(0xFF * catAlpha * p.boolAnim));
                } else {
                    pFill.setColor(0xFF818582);
                    pFill.setAlpha((int)(0xFF * catAlpha));
                }
                rectF.set(togX, togY, togX + togW, togY + togH);
                canvas.drawRoundRect(rectF, togRadius, togRadius, pFill);
                pFill.setShader(null);

                // 圆点 — 使用动画进度平滑移动
                float knobR = 3.25f * S;
                float knobX = togX + knobR + 1 * S + (togW - 2*knobR - 2*S) * p.boolAnim;
                float knobY = togY + togH / 2;
                pFill.setColor(0xFFFFFFFF);
                pFill.setAlpha((int)(0xFF * catAlpha));
                canvas.drawCircle(knobX, knobY, knobR, pFill);

                break;
            }

            case Property.TYPE_NUM: {
                rectF.set(propX, propY, propX + propW, propY + PROP_SLIDER_H);
                canvas.drawRect(rectF, pFill);

                // 顶部标签和值
                float topH = 9 * S;
                pText.setTextAlign(Paint.Align.LEFT);
                pText.setTextSize(7 * S);
                pText.setTypeface(Typeface.DEFAULT);
                pText.setColor(0x80FFFFFF);
                pText.setAlpha((int)(0x80 * catAlpha));
                Paint.FontMetrics fmNum = pText.getFontMetrics();
                float textYNum = propY + topH / 2 - (fmNum.top + fmNum.bottom) / 2;
                canvas.drawText(p.label, propX + 6 * S, textYNum, pText);

                // 数值 — 使用动画值平滑显示
                String valStr = formatNum(p.displayVal);
                pText.setTextAlign(Paint.Align.RIGHT);
                pText.setColor(0xA6FFFFFF);
                pText.setAlpha((int)(0xA6 * catAlpha));
                canvas.drawText(valStr, propX + propW - 6 * S, textYNum, pText);

                // 滑块轨道
                float trackX = propX + 5 * S;
                float trackW = propW - 10 * S;
                float trackY = propY + topH + 1 * S;
                float trackH = 5 * S;

                // 背景轨道
                float bgY = trackY + 1.75f * S;
                float bgH = 2.5f * S;
                pFill.setColor(0xFF373737);
                pFill.setAlpha((int)(0xFF * catAlpha));
                rectF.set(trackX, bgY, trackX + trackW, bgY + bgH);
                canvas.drawRoundRect(rectF, bgH / 2, bgH / 2, pFill);

                // 填充 — 使用动画值平滑过渡
                float pct = (float)((p.displayVal - p.min) / (p.max - p.min));
                float fillW = trackW * pct;
                LinearGradient grad = new LinearGradient(
                        trackX, 0, trackX + fillW, 0,
                        c1, c2, Shader.TileMode.CLAMP
                );
                pFill.setShader(grad);
                pFill.setAlpha((int)(0xFF * catAlpha));
                rectF.set(trackX, bgY, trackX + fillW, bgY + bgH);
                canvas.drawRoundRect(rectF, bgH / 2, bgH / 2, pFill);
                pFill.setShader(null);

                // 拖拽点 (Thumb)
                float thumbX = trackX + fillW;
                float thumbY = trackY;
                pFill.setColor(0xFFFFFFFF);
                pFill.setAlpha((int)(0xFF * catAlpha));
                rectF.set(thumbX - 1 * S, thumbY, thumbX + 1 * S, thumbY + trackH);
                canvas.drawRoundRect(rectF, 1 * S, 1 * S, pFill);
                break;
            }

            case Property.TYPE_MODE: {
                float ph = getPropertyHeight(p);
                rectF.set(propX, propY, propX + propW, propY + ph);
                canvas.drawRect(rectF, pFill);

                // 标签
                pText.setTextAlign(Paint.Align.LEFT);
                pText.setTextSize(7 * S);
                pText.setTypeface(Typeface.DEFAULT);
                pText.setColor(0x80FFFFFF);
                pText.setAlpha((int)(0x80 * catAlpha));
                canvas.drawText(p.label, propX + 5 * S, propY + 12 * S, pText);

                // 当前模式框
                float boxX = propX + 3 * S;
                float boxY = propY + 15 * S;
                float boxW = propW - 6 * S;
                float boxH = 14 * S;
                pFill.setColor(0x40000000);
                pFill.setAlpha((int)(0x40 * catAlpha));
                rectF.set(boxX, boxY, boxX + boxW, boxY + boxH);
                canvas.drawRoundRect(rectF, 4 * S, 4 * S, pFill);

                // 当前模式文字
                pText.setTextAlign(Paint.Align.LEFT);
                pText.setTextSize(7 * S);
                pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                pText.setColor(0xFFFFFFFF);
                pText.setAlpha((int)(0xFF * catAlpha));
                canvas.drawText(p.modes[p.modeIdx], boxX + 4 * S, boxY + boxH * 0.7f, pText);

                // 下拉箭头 — 使用动画进度平滑旋转
                pText.setTextAlign(Paint.Align.RIGHT);
                pText.setTypeface(Typeface.DEFAULT);
                pText.setColor(0x99FFFFFF);
                pText.setAlpha((int)(0x99 * catAlpha));
                canvas.save();
                canvas.rotate(p.modeAnim * 180, boxX + boxW - 6 * S, boxY + boxH / 2);
                pStroke.setColor(0x99FFFFFF);
                pStroke.setAlpha((int)(0x99 * catAlpha));
                pStroke.setStyle(Paint.Style.STROKE);
                pStroke.setStrokeWidth(1.5f * S);
                pStroke.setStrokeCap(Paint.Cap.ROUND);
                float cx = boxX + boxW - 6 * S;
                float cy = boxY + boxH / 2;
                float sz = 3.5f * S;
                canvas.drawLine(cx - sz, cy - sz/2, cx, cy + sz/2, pStroke);
                canvas.drawLine(cx, cy + sz/2, cx + sz, cy - sz/2, pStroke);
                pStroke.setStyle(Paint.Style.FILL);
                pStroke.setStrokeCap(Paint.Cap.BUTT);
                canvas.restore();

                // 下拉选项 (展开时) — 使用动画进度控制高度和透明度
                if (p.modeAnim > 0.01f) {
                    // 裁剪区域实现下拉展开效果
                    canvas.save();
                    rectF.set(propX, boxY + boxH, propX + propW, boxY + boxH + p.modes.length * 9 * S);
                    canvas.clipRect(rectF);

                    float optY = boxY + boxH + 1 * S;
                    for (int oi = 0; oi < p.modes.length; oi++) {
                        pText.setTextAlign(Paint.Align.LEFT);
                        pText.setTextSize(7 * S);
                        pText.setTypeface(Typeface.DEFAULT);
                        if (oi == p.modeIdx) {
                            pText.setColor(c1);
                            pText.setAlpha((int)(0xFF * catAlpha * p.modeAnim));
                        } else {
                            pText.setColor(0x8CFFFFFF);
                            pText.setAlpha((int)(0x8C * catAlpha * p.modeAnim));
                        }
                        canvas.drawText(p.modes[oi], boxX + 4 * S, optY + 8 * S, pText);
                        optY += 9 * S;
                    }
                    canvas.restore();
                }
                break;
            }

            case Property.TYPE_THEME: {
                float ph = getPropertyHeight(p);
                rectF.set(propX, propY, propX + propW, propY + ph);
                canvas.drawRect(rectF, pFill);

                // 4列网格
                float gridX = propX + 4 * S;
                float gridW = propW - 8 * S;
                float cellW = gridW / 4;
                float cellSize = cellW - 2 * S;
                float gap = 2 * S;

                for (int ti = 0; ti < themes.length; ti++) {
                    int col = ti % 4;
                    int row = ti / 4;
                    float dotX = gridX + col * cellW;
                    float dotY = propY + 3 * S + row * (cellSize + gap);
                    float dotR = 3 * S;

                    // 色块
                    LinearGradient grad = new LinearGradient(
                            dotX, dotY, dotX + cellSize, dotY + cellSize,
                            themes[ti].c1, themes[ti].c2, Shader.TileMode.CLAMP
                    );
                    pFill.setShader(grad);
                    pFill.setAlpha((int)(0xFF * catAlpha));
                    rectF.set(dotX, dotY, dotX + cellSize, dotY + cellSize);
                    canvas.drawRoundRect(rectF, 3 * S, 3 * S, pFill);
                    pFill.setShader(null);

                    // 选中边框
                    if (ti == themeIndex) {
                        pStroke.setColor(0x80FFFFFF);
                        pStroke.setAlpha((int)(0x80 * catAlpha));
                        pStroke.setStyle(Paint.Style.STROKE);
                        pStroke.setStrokeWidth(1.5f * S);
                        canvas.drawRoundRect(rectF, 3 * S, 3 * S, pStroke);
                        pStroke.setStyle(Paint.Style.FILL);
                    }
                }
                break;
            }
        }
    }

    // 临时变量用于属性位置缓存
    /** 获取当前布局缩放 */
    public float getScale() { return S; }

    // ═══════════════════════════════════════════════════════
    //  绘制：通知
    // ═══════════════════════════════════════════════════════

    private void drawNotifications(Canvas canvas) {
        float bottomMargin = 16 * S; // 底部边距
        float y = screenH - bottomMargin; // 最下面通知的底部 Y 坐标
        float rightX = screenW - 8 * S; // 右侧边距

        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notif n = notifications.get(i);
            long elapsed = System.currentTimeMillis() - n.startTime;
            float progress = Math.max(0, 1 - (float) elapsed / n.duration);

            // 位置
            float notifW = Math.max(100 * S, measureNotifWidth(n));
            float notifX = rightX - notifW;

            // 动画偏移 — 平滑从右侧滑入/滑出，带透明度渐变
            float offsetX = 0;
            float alphaMultiplier = 1f;
            if (!n.hiding && n.animProgress < 1f) {
                // 入场: 从右侧滑入 + 淡入, 使用 easeOutCubic 缓动
                float eased = easeOutCubic(n.animProgress);
                offsetX = (1 - eased) * (notifW + 20 * S);
                alphaMultiplier = eased;
            } else if (n.hiding) {
                // 出场: 向右滑出 + 淡出, 使用 easeInCubic 缓动
                float eased = easeInCubic(n.hideProgress);
                offsetX = eased * (notifW + 20 * S);
                alphaMultiplier = 1f - eased;
            }
            notifX += offsetX;
            // 计算通知顶部 Y：从底部往上排列，每个通知高度 + 间距
            float notifY = y - NOTIF_H - (notifications.size() - 1 - i) * (NOTIF_H + 4 * S);

            // 背景
            pFill.setColor(NOTIF_BG);
            pFill.setAlpha((int)(0x80 * alphaMultiplier));
            rectF.set(notifX, notifY, notifX + notifW, notifY + NOTIF_H);
            canvas.drawRoundRect(rectF, NOTIF_RADIUS, NOTIF_RADIUS, pFill);

            // 进度条
            pFill.setColor(N_COLOR[n.type]);
            pFill.setAlpha((int)(Color.alpha(N_COLOR[n.type]) * alphaMultiplier));
            float progW = (notifW - 0.5f * S) * progress;
            rectF2.set(notifX + 0.5f * S, notifY + NOTIF_H - 3 * S,
                       notifX + 0.5f * S + progW, notifY + NOTIF_H);
            canvas.drawRoundRect(rectF2, 2 * S, 2 * S, pFill);

            // 图标背景
            float iconBoxX = notifX + 3 * S;
            float iconBoxY = notifY + (NOTIF_H - 15 * S) / 2;
            float iconBoxS = 15 * S;
            pFill.setColor(N_ICON_BG[n.type]);
            pFill.setAlpha((int)(Color.alpha(N_ICON_BG[n.type]) * alphaMultiplier));
            rectF2.set(iconBoxX, iconBoxY, iconBoxX + iconBoxS, iconBoxY + iconBoxS);
            canvas.drawRoundRect(rectF2, 2.75f * S, 2.75f * S, pFill);

            // 图标
            pText.setTextAlign(Paint.Align.CENTER);
            pText.setTextSize(10 * S);
            pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            pText.setColor(N_COLOR[n.type]);
            pText.setAlpha((int)(Color.alpha(N_COLOR[n.type]) * alphaMultiplier));
            canvas.drawText(N_ICON[n.type], iconBoxX + iconBoxS / 2,
                            iconBoxY + iconBoxS * 0.75f, pText);

            // 标题
            float textX = iconBoxX + iconBoxS + 2.5f * S;
            pText.setTextAlign(Paint.Align.LEFT);
            pText.setTextSize(7 * S);
            pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            pText.setColor(0xFFFFFFFF);
            pText.setAlpha((int)(0xFF * alphaMultiplier));
            canvas.drawText(n.title, textX, notifY + 9 * S, pText);

            // 描述
            pText.setTextSize(6.5f * S);
            pText.setTypeface(Typeface.DEFAULT);
            pText.setColor(0xFFAAAAAA);
            pText.setAlpha((int)(0xAA * alphaMultiplier));
            canvas.drawText(n.desc, textX, notifY + 16 * S, pText);
        }
    }

    private float measureNotifWidth(Notif n) {
        pText.setTextSize(7 * S);
        pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        float titleW = pText.measureText(n.title);
        pText.setTextSize(6.5f * S);
        pText.setTypeface(Typeface.DEFAULT);
        float descW = pText.measureText(n.desc);
        float textW = Math.max(titleW, descW);
        return 3 * S + 15 * S + 2.5f * S + textW + 8 * S;
    }

    // ═══════════════════════════════════════════════════════
    //  绘制：动态岛
    // ═══════════════════════════════════════════════════════

    private void drawDynamicIsland(Canvas canvas, long now) {
        if (islandAnimProgress < 0.01f) return;

        float alpha = islandAnimProgress;

        // 计算宽度
        String leftText = "opal";
        String fpsText = "FPS: " + fps;
        String rightText = "hypixel.net";
        String separator = "|";

        pText.setTextSize(9 * S);
        pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        float leftW = pText.measureText(leftText);
        float fpsW = pText.measureText(fpsText);
        float rightW = pText.measureText(rightText);
        float separatorW = pText.measureText(separator);

        float paddingH = 16 * S; // 左右内边距
        float gap = 8 * S;
        float islandW = leftW + fpsW + rightW + separatorW * 2 + paddingH * 2 + gap * 4;
        float islandH = 28 * S;

        float islandX = (screenW - islandW) / 2;
        float islandY = 12 * S - (1 - islandAnimProgress) * 8 * S;

        // 半透明背景
        pFill.setColor(0x40000000);
        pFill.setAlpha((int)(0x40 * alpha));
        rectF.set(islandX, islandY, islandX + islandW, islandY + islandH);
        canvas.drawRoundRect(rectF, islandH / 2, islandH / 2, pFill);

        // 细边框
        pStroke.setColor(0x1AFFFFFF);
        pStroke.setAlpha((int)(0x1A * alpha));
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(0.5f * S);
        canvas.drawRoundRect(rectF, islandH / 2, islandH / 2, pStroke);
        pStroke.setStyle(Paint.Style.FILL);

        float textY = islandY + islandH * 0.68f;
        float cx = islandX + paddingH;

        // opal — 渐变文字
        pText.setTextAlign(Paint.Align.LEFT);
        LinearGradient textGrad = new LinearGradient(
                cx, 0, cx + leftW, 0,
                c2, c1, Shader.TileMode.CLAMP
        );
        pText.setShader(textGrad);
        pText.setAlpha((int)(0xFF * alpha));
        canvas.drawText(leftText, cx, textY, pText);
        pText.setShader(null);
        cx += leftW + gap;

        // 分隔符
        pText.setColor(0x33FFFFFF);
        pText.setAlpha((int)(0x33 * alpha));
        canvas.drawText(separator, cx, textY, pText);
        cx += separatorW + gap;

        // FPS — 主题色高亮
        pText.setColor(c1);
        pText.setAlpha((int)(0xFF * alpha));
        canvas.drawText(fpsText, cx, textY, pText);
        cx += fpsW + gap;

        // 分隔符
        pText.setColor(0x33FFFFFF);
        pText.setAlpha((int)(0x33 * alpha));
        canvas.drawText(separator, cx, textY, pText);
        cx += separatorW + gap;

        // hypixel.net — 白色
        pText.setColor(0xE6FFFFFF);
        pText.setAlpha((int)(0xE6 * alpha));
        canvas.drawText(rightText, cx, textY, pText);
    }
    private void drawToggleHint(Canvas canvas) {
        if (!hintShown && hintAlpha < 0.01f) return;

        String text1 = "点击悬浮球打开 ClickGUI";
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setTextSize(10 * S);
        pText.setTypeface(Typeface.DEFAULT);
        pText.setColor(0xB3FFFFFF);
        pText.setAlpha((int)(0xB3 * hintAlpha));

        float textW = pText.measureText(text1);
        float hintW = textW + 28 * S;
        float hintH = 32 * S;

        // 位置: 屏幕右下方
        float hintX = screenW - hintW - 16 * S;
        float hintY = screenH - hintH - 80 * S;

        // 滑入/滑出偏移: 淡入时从右下方滑入, 淡出时向上滑出
        // 非线性动画: 先快后慢 (easeOutCubic)
        long elapsed = System.currentTimeMillis() - startTime;
        long t = elapsed - HINT_DELAY;
        float slideX = 0;
        float slideY = 0;
        if (t < HINT_FADE_IN) {
            float raw = t / (float) HINT_FADE_IN;
            float eased = easeOutCubic(raw);
            slideX = (1 - eased) * 60 * S;  // 从右方 60*S 滑入
            slideY = (1 - eased) * 40 * S;  // 从下方 40*S 滑入
        } else if (t > HINT_FADE_IN + HINT_STAY) {
            float raw = Math.min(1, (t - HINT_FADE_IN - HINT_STAY) / (float) HINT_FADE_OUT);
            float eased = easeInCubic(raw);
            slideY = -eased * 60 * S;  // 向上 60*S 滑出
        }
        hintX += slideX;
        hintY += slideY;

        // 背景
        pFill.setColor(HINT_BG);
        pFill.setAlpha((int)(0xA6 * hintAlpha));
        rectF.set(hintX, hintY, hintX + hintW, hintY + hintH);
        canvas.drawRoundRect(rectF, 8 * S, 8 * S, pFill);

        // 边框
        pStroke.setColor(0x14FFFFFF);
        pStroke.setAlpha((int)(0x14 * hintAlpha));
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(1 * S);
        canvas.drawRoundRect(rectF, 8 * S, 8 * S, pStroke);
        pStroke.setStyle(Paint.Style.FILL);

        // 文字
        canvas.drawText(text1, hintX + hintW / 2, hintY + hintH * 0.65f, pText);
    }

    // ═══════════════════════════════════════════════════════
    //  绘制：快捷键按钮
    // ═══════════════════════════════════════════════════════

    private void drawKeyBindings(Canvas canvas) {
        float keyBtnW = KEY_BTN_W * S;
        float keyBtnH = KEY_BTN_H * S;
        float keyBtnRadius = KEY_BTN_RADIUS * S;

        for (Category cat : categories) {
            for (Module m : cat.modules) {
                if (!m.keyEnabled || m.keyX < 0) continue;

                float x = m.keyX;
                float y = m.keyY;

                // 长按缩放效果
                float scale = 1f;
                if (longPressKeyModule == m) {
                    scale = keyScaleAnim;
                }
                float scaledW = keyBtnW * scale;
                float scaledH = keyBtnH * scale;
                float drawX = x + (keyBtnW - scaledW) / 2;
                float drawY = y + (keyBtnH - scaledH) / 2;

                // 背景 — 开启时主题色渐变，关闭时半透明灰色
                if (m.on) {
                    LinearGradient grad = new LinearGradient(
                            drawX, drawY, drawX + scaledW, drawY + scaledH,
                            c1, c2, Shader.TileMode.CLAMP
                    );
                    pFill.setShader(grad);
                    pFill.setAlpha((int)(0xE6)); // ~90% 透明度
                } else {
                    pFill.setColor(0x802A2A2A); // 半透明深灰
                    pFill.setShader(null);
                }
                rectF.set(drawX, drawY, drawX + scaledW, drawY + scaledH);
                canvas.drawRoundRect(rectF, keyBtnRadius * scale, keyBtnRadius * scale, pFill);
                pFill.setShader(null);

                // 边框
                if (m.on) {
                    pStroke.setColor(0x33FFFFFF);
                } else {
                    pStroke.setColor(0x1AFFFFFF);
                }
                pStroke.setStyle(Paint.Style.STROKE);
                pStroke.setStrokeWidth(1 * S);
                canvas.drawRoundRect(rectF, keyBtnRadius * scale, keyBtnRadius * scale, pStroke);
                pStroke.setStyle(Paint.Style.FILL);

                // 文字 — 模块名称，截断以适应按钮宽度
                String text = m.name;
                float textSize = 8 * S * scale;
                pText.setTextSize(textSize);
                pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                float textMaxW = scaledW - 8 * S * scale;
                if (pText.measureText(text) > textMaxW) {
                    // 截断文字
                    for (int i = text.length(); i > 0; i--) {
                        String sub = text.substring(0, i) + "…";
                        if (pText.measureText(sub) <= textMaxW) {
                            text = sub;
                            break;
                        }
                    }
                }

                if (m.on) {
                    pText.setColor(0xFFFFFFFF);
                } else {
                    pText.setColor(0x99999999);
                }
                pText.setTextAlign(Paint.Align.CENTER);
                // 垂直居中对齐
                Paint.FontMetrics fontMetrics = pText.getFontMetrics();
                float textY = drawY + scaledH / 2 - (fontMetrics.top + fontMetrics.bottom) / 2;
                canvas.drawText(text, drawX + scaledW / 2, textY, pText);

                // 拖动时的按压效果
                if (m.keyDragging) {
                    pFill.setColor(0x33000000);
                    canvas.drawRoundRect(rectF, keyBtnRadius * scale, keyBtnRadius * scale, pFill);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  绘制：悬浮球 (FAB)
    // ═══════════════════════════════════════════════════════

    private void drawFAB(Canvas canvas) {
        if (fabAnimProgress < 0.01f) return;

        float size = FAB_SIZE * S;
        float x = fabX;
        float y = fabY;
        float alpha = fabAnimProgress;

        // 阴影
        pFill.setColor(0x40000000);
        pFill.setAlpha((int)(0x40 * alpha));
        canvas.drawCircle(x + size / 2 + 2 * S, y + size / 2 + 3 * S, size / 2, pFill);

        // 背景 — 渐变
        LinearGradient grad = new LinearGradient(
                x, y, x + size, y + size,
                c1, c2, Shader.TileMode.CLAMP
        );
        pFill.setShader(grad);
        pFill.setAlpha((int)(0xFF * alpha));
        canvas.drawCircle(x + size / 2, y + size / 2, size / 2, pFill);
        pFill.setShader(null);

        // 边框高光
        pStroke.setColor(0x33FFFFFF);
        pStroke.setAlpha((int)(0x33 * alpha));
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setStrokeWidth(1.5f * S);
        canvas.drawCircle(x + size / 2, y + size / 2, size / 2 - 0.75f * S, pStroke);
        pStroke.setStyle(Paint.Style.FILL);

        // 图标 — 菜单/齿轮图标
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setTextSize(20 * S);
        pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        pText.setColor(0xFFFFFFFF);
        pText.setAlpha((int)(0xFF * alpha));
        canvas.drawText("⚙", x + size / 2, y + size / 2 + 7 * S, pText);

        // 按下时的按压效果
        if (fabPressed) {
            pFill.setColor(0x33000000);
            pFill.setAlpha((int)(0x33 * alpha));
            canvas.drawCircle(x + size / 2, y + size / 2, size / 2, pFill);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  绘制：控制台按钮
    // ═══════════════════════════════════════════════════════


    // ═══════════════════════════════════════════════════════
    //  触摸处理
    // ═══════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        // ═══ 优先处理悬浮球触摸 ═══
        float fabSize = FAB_SIZE * S;
        boolean hitFab = x >= fabX && x <= fabX + fabSize &&
                         y >= fabY && y <= fabY + fabSize;

        // ═══ 检查是否点击了快捷键按钮 ═══
        Module hitKeyModule = null;
        float keyBtnW = KEY_BTN_W * S;
        float keyBtnH = KEY_BTN_H * S;
        for (Category cat : categories) {
            for (Module m : cat.modules) {
                if (!m.keyEnabled || m.keyX < 0) continue;
                if (x >= m.keyX && x <= m.keyX + keyBtnW &&
                    y >= m.keyY && y <= m.keyY + keyBtnH) {
                    hitKeyModule = m;
                    break;
                }
            }
            if (hitKeyModule != null) break;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 检查是否点中悬浮球
                if (hitFab) {
                    fabPressed = true;
                    fabDownX = x - fabX;
                    fabDownY = y - fabY;
                    fabDownTime = System.currentTimeMillis();
                    fabLongPressTriggered = false;
                    return true;
                }

                // 检查是否点中快捷键按钮
                if (hitKeyModule != null) {
                    longPressKeyModule = hitKeyModule;
                    longPressKeyX = x;
                    longPressKeyY = y;
                    keyLongPressTriggered = false;
                    keyScaleAnim = 1f;
                    draggingKeyModule = hitKeyModule;
                    dragKeyOffsetX = x - hitKeyModule.keyX;
                    dragKeyOffsetY = y - hitKeyModule.keyY;
                    hitKeyModule.keyDragging = false; // 先不标记为拖动，等长按触发后再拖动
                    return true;
                }


                touchStartX = x;
                touchStartY = y;
                lastTouchX = x;
                lastTouchY = y;
                touchDownTime = System.currentTimeMillis();
                isScrolling = false;

                // 检查滑块拖拽开始
                checkSliderDragStart(x, y);

                // 检查模块长按 — 找到点击的模块
                longPressCatIdx = -1;
                longPressModIdx = -1;
                longPressTriggered = false;
                if (guiOpen) {
                    float catY = 86 * S;
                    if (y >= catY) {
                        for (int ci = 0; ci < categories.size(); ci++) {
                            Category cat = categories.get(ci);
                            if (x < cat.layoutX || x > cat.layoutX + CAT_W) continue;
                            for (int mi = 0; mi < cat.modules.size(); mi++) {
                                Module m = cat.modules.get(mi);
                                if (!m.visible) continue;
                                float modAbsY = cat.layoutY + m.layoutY;
                                if (y >= modAbsY && y <= modAbsY + MOD_H) {
                                    // 排除箭头区域
                                    if (m.hasProps()) {
                                        float arrowX = cat.layoutX + CAT_W - 17 * S;
                                        if (x >= arrowX) break; // 箭头区域，不长按
                                    }
                                    longPressCatIdx = ci;
                                    longPressModIdx = mi;
                                    longPressX = x;
                                    longPressY = y;
                                    break;
                                }
                            }
                            if (longPressCatIdx >= 0) break;
                        }
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                // 悬浮球拖动 — 长按触发
                if (fabPressed && !isDraggingFab) {
                    long pressDuration = System.currentTimeMillis() - fabDownTime;
                    if (pressDuration >= FAB_LONG_PRESS_MS) {
                        isDraggingFab = true;
                        fabLongPressTriggered = true;
                    }
                }
                if (isDraggingFab) {
                    fabX = x - fabDownX;
                    fabY = y - fabDownY;
                    // 限制在屏幕内
                    fabX = Math.max(0, Math.min(screenW - fabSize, fabX));
                    fabY = Math.max(0, Math.min(screenH - fabSize, fabY));
                    return true;
                }

                // 快捷键按钮拖动
                if (longPressKeyModule != null) {
                    long pressDuration = System.currentTimeMillis() - touchDownTime;
                    float moveDist = (float) Math.sqrt(
                            Math.pow(x - longPressKeyX, 2) +
                            Math.pow(y - longPressKeyY, 2));

                    // 移动距离超过阈值则取消长按
                    if (moveDist > 8 * S && !keyLongPressTriggered) {
                        longPressKeyModule = null;
                        keyScaleAnim = 1f;
                    } else if (pressDuration >= KEY_LONG_PRESS_MS && !keyLongPressTriggered) {
                        // 触发长按
                        keyLongPressTriggered = true;
                        longPressKeyModule.keyDragging = true;
                        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    }

                    // 更新缩放动画
                    if (longPressKeyModule != null) {
                        if (keyLongPressTriggered) {
                            keyScaleAnim = 0.9f; // 长按时稍微缩小
                        } else {
                            // 按下时的轻微放大效果
                            keyScaleAnim = Math.min(1.05f, keyScaleAnim + 0.02f);
                        }
                    }

                    // 拖动
                    if (keyLongPressTriggered && draggingKeyModule != null) {
                        float newX = x - dragKeyOffsetX;
                        float newY = y - dragKeyOffsetY;
                        // 限制在屏幕内
                        newX = Math.max(0, Math.min(screenW - KEY_BTN_W * S, newX));
                        newY = Math.max(0, Math.min(screenH - KEY_BTN_H * S, newY));
                        draggingKeyModule.keyX = newX;
                        draggingKeyModule.keyY = newY;
                    }
                    return true;
                }

                // 模块长按检测
                if (longPressCatIdx >= 0 && !longPressTriggered) {
                    long pressDuration = System.currentTimeMillis() - touchDownTime;
                    float moveDist = (float) Math.sqrt(
                            Math.pow(x - longPressX, 2) +
                            Math.pow(y - longPressY, 2));
                    // 移动距离超过阈值则取消长按
                    if (moveDist > 8 * S) {
                        longPressCatIdx = -1;
                        longPressModIdx = -1;
                    } else if (pressDuration >= MODULE_LONG_PRESS_MS) {
                        // 触发长按
                        longPressTriggered = true;
                        Module m = categories.get(longPressCatIdx).modules.get(longPressModIdx);
                        m.keyEnabled = !m.keyEnabled;
                        if (m.keyEnabled) {
                            // 初始化快捷键位置 (左侧，按顺序排列)
                            int keyIndex = 0;
                            for (Category cat : categories) {
                                for (Module mm : cat.modules) {
                                    if (mm.keyEnabled && mm != m) keyIndex++;
                                }
                            }
                            m.keyX = 8 * S;
                            m.keyY = 100 * S + keyIndex * (KEY_BTN_H * S + 8 * S);
                        }
                        showNotif(m.name, m.keyEnabled ? "Keybind enabled" : "Keybind disabled",
                                m.keyEnabled ? N_SUCCESS : N_INFO, 1500);
                        // 触发震动反馈
                        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    }
                }

                if (dragSliderPropIdx >= 0) {
                    // 拖拽滑块
                    handleSliderDrag(x);
                    return true;
                }
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;
                if (!isScrolling && (Math.abs(x - touchStartX) > SCROLL_THRESHOLD ||
                                     Math.abs(y - touchStartY) > SCROLL_THRESHOLD)) {
                    isScrolling = true;
                    // 滚动时取消长按
                    if (longPressCatIdx >= 0) {
                        longPressCatIdx = -1;
                        longPressModIdx = -1;
                    }
                }
                if (isScrolling && guiOpen) {
                    guiScrollX -= dx;
                    guiScrollX = Math.max(0, Math.min(maxScrollX, guiScrollX));
                    // 垂直滚动
                    guiScrollY -= dy;
                    guiScrollY = Math.max(0, Math.min(maxScrollY, guiScrollY));
                }
                lastTouchX = x;
                lastTouchY = y;
                return true;

            case MotionEvent.ACTION_UP:
                // 悬浮球松手
                if (fabPressed) {
                    // 判断是否是点击（没有触发长按且移动距离很小）
                    float moveDist = (float) Math.sqrt(
                            Math.pow(x - (fabDownX + fabX), 2) +
                            Math.pow(y - (fabDownY + fabY), 2));
                    if (!fabLongPressTriggered && moveDist < 10 * S) {
                        toggleGUI();
                    }
                    isDraggingFab = false;
                    fabPressed = false;
                    fabLongPressTriggered = false;
                    return true;
                }

                // 快捷键按钮松手
                if (draggingKeyModule != null) {
                    // 只有没有触发长按且移动距离很小才视为点击
                    float moveDist = (float) Math.sqrt(
                            Math.pow(x - (dragKeyOffsetX + draggingKeyModule.keyX), 2) +
                            Math.pow(y - (dragKeyOffsetY + draggingKeyModule.keyY), 2));
                    if (!keyLongPressTriggered && moveDist < 5 * S && !draggingKeyModule.keyDragging) {
                        // 点击 — 切换模块开关
                        Module m = draggingKeyModule;
                        m.on = !m.on;
                        showNotif(m.name, m.on ? "Enabled" : "Disabled",
                                m.on ? N_SUCCESS : N_INFO, 1500);
                    }
                    draggingKeyModule.keyDragging = false;
                    draggingKeyModule = null;
                    longPressKeyModule = null;
                    keyLongPressTriggered = false;
                    keyScaleAnim = 1f;
                    return true;
                }

                // 如果触发了长按，不执行普通点击
                if (longPressTriggered) {
                    longPressCatIdx = -1;
                    longPressModIdx = -1;
                    longPressTriggered = false;
                    return true;
                }

                if (dragSliderPropIdx >= 0) {
                    dragSliderPropIdx = -1;
                    dragSliderModIdx = -1;
                    dragSliderCatIdx = -1;
                    return true;
                }
                if (!isScrolling) {
                    handleClick(x, y);
                }
                // 重置长按状态
                longPressCatIdx = -1;
                longPressModIdx = -1;
                longPressTriggered = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    private int tapCount = 0;
    private long lastTapTime = 0;

    private void checkTripleTap(float y) {
        long now = System.currentTimeMillis();
        if (y < 60 * S) {
            if (now - lastTapTime < 500) {
                tapCount++;
                if (tapCount >= 3) {
                    toggleGUI();
                    tapCount = 0;
                }
            } else {
                tapCount = 1;
            }
            lastTapTime = now;
        }
    }

    private void toggleGUI() {
        guiOpen = !guiOpen;
        if (guiOpen) {
            guiOpenTime = System.currentTimeMillis();
            guiAnimProgress = 0f;
        }
    }

    private void handleClick(float x, float y) {
        if (!guiOpen) return;

        // 2. 检查分类面板中的模块和属性
        float catY = 86 * S;
        if (y < catY) return;

        for (int ci = 0; ci < categories.size(); ci++) {
            Category cat = categories.get(ci);
            if (x < cat.layoutX || x > cat.layoutX + CAT_W) continue;

            // 点击在分类面板内
            for (int mi = 0; mi < cat.modules.size(); mi++) {
                Module m = cat.modules.get(mi);
                if (!m.visible) continue;

                float modAbsY = cat.layoutY + m.layoutY;
                float modAbsBottom = modAbsY + MOD_H;

                if (y >= modAbsY && y <= modAbsBottom) {
                    // 检查是否点击了箭头区域
                    if (m.hasProps()) {
                        float arrowX = cat.layoutX + CAT_W - 17 * S;
                        if (x >= arrowX) {
                            m.expanded = !m.expanded;
                            return;
                        }
                    }
                    // 切换模块开关
                    m.on = !m.on;
                    showNotif(m.name, m.on ? "Enabled" : "Disabled",
                            m.on ? N_SUCCESS : N_INFO, 1800);
                    return;
                }

                // 检查属性
                if (m.expanded && m.hasProps()) {
                    for (int pi = 0; pi < m.props.size(); pi++) {
                        Property p = m.props.get(pi);
                        float propAbsY = cat.layoutY + p.layoutY;
                        float propAbsBottom = propAbsY + p.layoutHeight;

                        if (y >= propAbsY && y <= propAbsBottom) {
                            handlePropertyClick(cat, m, p, ci, mi, pi,
                                    x - cat.layoutX, y - propAbsY);
                            return;
                        }
                    }
                }
            }
            return;
        }
    }


    private void handlePropertyClick(Category cat, Module m, Property p,
                                     int ci, int mi, int pi,
                                     float localX, float localY) {
        switch (p.type) {
            case Property.TYPE_BOOL:
                // 点击开关
                p.boolVal = !p.boolVal;
                break;

            case Property.TYPE_NUM:
                // 滑块 — 点击设置值或开始拖拽
                float trackX = 5 * S;
                float trackW = CAT_W - 10 * S;
                float pct = (localX - trackX) / trackW;
                pct = Math.max(0, Math.min(1, pct));
                p.val = p.min + (p.max - p.min) * pct;
                // 对齐到 step
                if (p.step > 0) {
                    p.val = Math.round(p.val / p.step) * p.step;
                }
                p.val = Math.max(p.min, Math.min(p.max, p.val));
                break;

            case Property.TYPE_MODE:
                // 点击模式框 — 切换下拉
                float boxY = 15 * S;
                float boxH = 14 * S;
                if (localY >= boxY && localY <= boxH + boxY + (p.modeOpen ? p.modes.length * 9 * S : 0)) {
                    if (localY <= boxY + boxH) {
                        p.modeOpen = !p.modeOpen;
                    } else {
                        // 选择选项
                        int optIdx = (int) ((localY - boxY - boxH - 1 * S) / (9 * S));
                        if (optIdx >= 0 && optIdx < p.modes.length) {
                            p.modeIdx = optIdx;
                            p.modeOpen = false;
                        }
                    }
                }
                break;

            case Property.TYPE_THEME:
                // 主题网格点击
                float gridX = 4 * S;
                float gridW = CAT_W - 8 * S;
                float cellW = gridW / 4;
                float cellSize = cellW - 2 * S;
                float gap = 2 * S;

                int col = (int) ((localX - gridX) / cellW);
                int row = (int) ((localY - 3 * S) / (cellSize + gap));
                int themeIdx = row * 4 + col;
                if (themeIdx >= 0 && themeIdx < themes.length) {
                    setTheme(themeIdx);
                }
                break;
        }
    }

    private void checkSliderDragStart(float x, float y) {
        if (!guiOpen) return;
        float catY = 86 * S;
        if (y < catY) return;

        for (int ci = 0; ci < categories.size(); ci++) {
            Category cat = categories.get(ci);
            if (x < cat.layoutX || x > cat.layoutX + CAT_W) continue;

            for (int mi = 0; mi < cat.modules.size(); mi++) {
                Module m = cat.modules.get(mi);
                if (!m.visible || !m.expanded || !m.hasProps()) continue;

                for (int pi = 0; pi < m.props.size(); pi++) {
                    Property p = m.props.get(pi);
                    if (p.type != Property.TYPE_NUM) continue;

                    float propAbsY = cat.layoutY + p.layoutY;
                    float trackY = propAbsY + 9 * S + 1 * S;
                    float trackH = 5 * S;

                    if (y >= trackY && y <= trackY + trackH + 4 * S) {
                        dragSliderCatIdx = ci;
                        dragSliderModIdx = mi;
                        dragSliderPropIdx = pi;
                        return;
                    }
                }
            }
        }
    }

    private void handleSliderDrag(float x) {
        if (dragSliderCatIdx < 0 || dragSliderPropIdx < 0) return;
        Category cat = categories.get(dragSliderCatIdx);
        Module m = cat.modules.get(dragSliderModIdx);
        Property p = m.props.get(dragSliderPropIdx);

        float trackX = cat.layoutX + 5 * S;
        float trackW = CAT_W - 10 * S;
        float pct = (x - trackX) / trackW;
        pct = Math.max(0, Math.min(1, pct));
        p.val = p.min + (p.max - p.min) * pct;
        if (p.step > 0) {
            p.val = Math.round(p.val / p.step) * p.step;
        }
        p.val = Math.max(p.min, Math.min(p.max, p.val));
    }

    // ═══════════════════════════════════════════════════════
    //  通知系统
    // ═══════════════════════════════════════════════════════

    private static class Notif {
        String title;
        String desc;
        int type;
        long startTime;
        int duration;
        boolean showing;
        boolean hiding;
        float animProgress;
        float hideProgress;
    }

    // ═══════════════════════════════════════════════════════
    //  控制台功能
    // ═══════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════
    //  背景图片设置
    // ═══════════════════════════════════════════════════════

    public void setBackgroundBitmap(Bitmap bitmap) {
        this.backgroundBitmap = bitmap;
        this.backgroundImageEnabled = (bitmap != null);
        invalidate();
    }

    public void setBackgroundImageEnabled(boolean enabled) {
        this.backgroundImageEnabled = enabled;
        invalidate();
    }

    public boolean isBackgroundImageEnabled() {
        return backgroundImageEnabled;
    }

    private void showNotif(String title, String desc, int type, int duration) {
        Notif n = new Notif();
        n.title = title;
        n.desc = desc;
        n.type = type;
        n.duration = duration;
        n.startTime = System.currentTimeMillis();
        n.showing = true;
        n.animProgress = 0;
        notifications.add(n);
    }

    private void updateNotifications(long now) {
        Iterator<Notif> it = notifications.iterator();
        while (it.hasNext()) {
            Notif n = it.next();
            long elapsed = now - n.startTime;

            if (!n.hiding) {
                // 入场动画
                n.animProgress = Math.min(1, (now - n.startTime) / 400f);
            }

            if (elapsed >= n.duration && !n.hiding) {
                n.hiding = true;
                n.hideProgress = 0;
            }

            if (n.hiding) {
                n.hideProgress += 1f / 20f; // ~400ms at 60fps
                if (n.hideProgress >= 1) {
                    it.remove();
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Toggle Hint 动画更新
    //  依据 elapsed 计算当前 hintAlpha, 与 drawToggleHint 中
    //  的滑入/停留/滑出三段时序保持一致:
    //    t < 0                              → 未显示 (alpha = 0)
    //    0 ≤ t < HINT_FADE_IN               → 淡入 (easeOutCubic)
    //    HINT_FADE_IN ≤ t < FADE_IN+STAY    → 完全显示 (alpha = 1)
    //    FADE_IN+STAY ≤ t < +FADE_OUT       → 淡出 (1 - easeInCubic)
    //    t ≥ FADE_IN+STAY+FADE_OUT          → 结束 (alpha = 0)
    // ═══════════════════════════════════════════════════════

    private void updateHint(long elapsed) {
        long t = elapsed - HINT_DELAY;
        if (t < 0) {
            hintAlpha = 0;
            hintShown = false;
            return;
        }
        hintShown = true;
        if (t < HINT_FADE_IN) {
            float raw = t / (float) HINT_FADE_IN;
            hintAlpha = easeOutCubic(raw);
        } else if (t < HINT_FADE_IN + HINT_STAY) {
            hintAlpha = 1f;
        } else if (t < HINT_FADE_IN + HINT_STAY + HINT_FADE_OUT) {
            float raw = (t - HINT_FADE_IN - HINT_STAY) / (float) HINT_FADE_OUT;
            hintAlpha = 1f - easeInCubic(raw);
        } else {
            hintAlpha = 0f;
        }
        // 钳制到 [0, 1] 防止浮点误差
        hintAlpha = Math.max(0f, Math.min(1f, hintAlpha));
    }

    // ═══════════════════════════════════════════════════════
    //  动态岛状态循环
    // ═══════════════════════════════════════════════════════

    private void updateIslandState(long elapsed) {
        // 周期 15s: 0-5s default, 5-10s scaffold, 10-15s target
        long cycle = elapsed % 15000;
        if (cycle < 5000) {
            islandState = 0;
        } else if (cycle < 10000) {
            if (islandState != 1) {
                islandState = 1;
                islandBlocks = 47;
                islandScaffoldPct = 0.65f;
            }
        } else {
            if (islandState != 2) {
                islandState = 2;
                islandTargetName = "StellaPrincess";
                islandTargetHpPct = 0.45f;
                islandTargetHpTxt = "9/20";
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  定时事件 (对应 HTML 中的 setTimeout)
    // ═══════════════════════════════════════════════════════

    private boolean notif1Sent = false;
    private boolean notif2Sent = false;

    private void triggerScheduledEvents(long elapsed) {
        if (!notif1Sent && elapsed > 800) {
            notif1Sent = true;
            showNotif("Opal", "Client loaded successfully", N_SUCCESS, 3500);
        }
        if (!notif2Sent && elapsed > 2200) {
            notif2Sent = true;
            showNotif("Disabler", "Watchdog bypass active", N_INFO, 3000);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  组件动画更新
    // ═══════════════════════════════════════════════════════

    /** 更新所有组件的动画进度 — 使用平滑插值实现流畅动画 */
    private void updateAnimations() {
        final float LERP_SPEED = 0.15f; // 每帧移动 15% 距离

        for (Category cat : categories) {
            for (Module m : cat.modules) {
                // 模块展开/收起动画
                float targetExpand = m.expanded ? 1f : 0f;
                m.expandAnim += (targetExpand - m.expandAnim) * LERP_SPEED;

                // 模块开关切换动画
                float targetToggle = m.on ? 1f : 0f;
                m.toggleAnim += (targetToggle - m.toggleAnim) * LERP_SPEED;

                // 属性动画
                if (m.hasProps()) {
                    for (Property p : m.props) {
                        if (p.type == Property.TYPE_BOOL) {
                            float targetBool = p.boolVal ? 1f : 0f;
                            p.boolAnim += (targetBool - p.boolAnim) * LERP_SPEED;
                        } else if (p.type == Property.TYPE_MODE) {
                            float targetMode = p.modeOpen ? 1f : 0f;
                            p.modeAnim += (targetMode - p.modeAnim) * LERP_SPEED;
                        } else if (p.type == Property.TYPE_NUM) {
                            // 数值滑块平滑动画
                            p.displayVal += (p.val - p.displayVal) * LERP_SPEED;
                        }
                    }
                }
            }
        }


        // 更新垂直滚动范围 (因为展开动画会改变高度)
        updateMaxScrollY();
    }

    // ═══════════════════════════════════════════════════════
    //  颜色工具
    // ═══════════════════════════════════════════════════════

    /** 对应 HTML 中的 interpolateColorsBackAndForth() */
    private int interpolateColorsBackAndForth(int speed, int offset, long now, int col1, int col2) {
        double a = ((now / (double) speed) - offset) % 360;
        a = (a + 360) % 360;
        if (a >= 180) a = 360 - a;
        return lerpColor(col1, col2, (float)(a * 2 / 360.0));
    }

    private int lerpColor(int c1, int c2, float t) {
        int r1 = Color.red(c1), g1 = Color.green(c1), b1 = Color.blue(c1);
        int r2 = Color.red(c2), g2 = Color.green(c2), b2 = Color.blue(c2);
        return Color.rgb(
                Math.round(r1 + (r2 - r1) * t),
                Math.round(g1 + (g2 - g1) * t),
                Math.round(b1 + (b2 - b1) * t)
        );
    }

    private int getShadowColor(int color) {
        return Color.rgb(
                Math.round(Color.red(color) * 0.25f),
                Math.round(Color.green(color) * 0.25f),
                Math.round(Color.blue(color) * 0.25f)
        );
    }

    /** 格式化数值显示 — 对应 HTML 中的 parseFloat(v.toFixed(3)).replace(/\.?0+$/,'') */
    private String formatNum(double v) {
        String s = String.format("%.3f", v);
        // 去除末尾零
        s = s.replaceAll("0+$", "");
        s = s.replaceAll("\\.$", "");
        return s;
    }

    /** 带阴影绘制文字 */
    private void drawTextWithShadow(Canvas canvas, String text, float x, float y,
                                     int color, float size) {
        pText.setColor(0x80000000);
        pText.setTextSize(size);
        pText.setTypeface(Typeface.DEFAULT);
        canvas.drawText(text, x + 1 * S, y + 1 * S, pText);
        pText.setColor(color);
        canvas.drawText(text, x, y, pText);
    }

    // ═══════════════════════════════════════════════════════
    //  缓动函数
    // ═══════════════════════════════════════════════════════

    /** easeOutBack — 带轻微回弹的减速, 用于分类面板入场 */
    private float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    /** easeOutCubic — 平滑减速, 用于提示淡入 */
    private float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    /** easeInCubic — 平滑加速, 用于提示淡出 */
    private float easeInCubic(float t) {
        return t * t * t;
    }
}
