package illustratedEntities.dialogue.panel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultilineTextField implements CustomUIPanelPlugin {

    public static final int MAX_CHARS = 5000;

    private static final float PAD = 5f;
    private static final float BAR_WIDTH = 4f;
    private static final float BAR_GAP = 4f;
    private static final float MIN_THUMB_HEIGHT = 16f;
    private static final float BLINK_TIME = 0.5f;
    private static final int WHEEL_LINES = 3;
    private static final float DRAG_SCROLL_LINES = 12f;
    private static final float LINE_BREAK_WIDTH = 4f;
    private static final int UNDO_LIMIT = 100;
    private static final IntBuffer SCISSOR_BOX = BufferUtils.createIntBuffer(16);

    private final LazyFont font;
    private final float fontSize;
    private final float lineHeight;
    private final List<MultilineTextField> focusGroup;

    private final StringBuilder text = new StringBuilder();
    private final List<int[]> lines = new ArrayList<>();
    private final Map<Integer, LazyFont.DrawableString> drawn = new HashMap<>();
    private float layoutWidth = -1f;

    private PositionAPI position;
    private int cursor;
    private int anchor;
    private float preferredX = -1f;
    private float scroll;
    private boolean focused;
    private float blink;

    private boolean draggingThumb;
    private float dragOffset;
    private boolean draggingText;
    private float mouseX;
    private float mouseY;

    private final Deque<Snapshot> undo = new ArrayDeque<>();
    private final Deque<Snapshot> redo = new ArrayDeque<>();
    private boolean wordOpen;

    private Color textColor;
    private float textAlpha = -1f;

    private record Snapshot(String text, int cursor, int anchor) {
    }

    public MultilineTextField(String text, List<MultilineTextField> focusGroup) {
        font = loadFont();
        fontSize = font.getBaseHeight();
        lineHeight = fontSize + 2f;
        this.focusGroup = focusGroup;
        focusGroup.add(this);
        if (text != null) this.text.append(normalize(text));
        cursor = this.text.length();
        anchor = cursor;
    }

    private static LazyFont loadFont() {
        try {
            return LazyFont.loadFont(Fonts.DEFAULT_SMALL);
        } catch (FontException ex) {
            throw new RuntimeException("Could not load font " + Fonts.DEFAULT_SMALL, ex);
        }
    }

    public String getText() {
        return text.toString();
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void advance(float amount) {
        blink = (blink + amount) % (2f * BLINK_TIME);
        if (draggingText && position != null) dragScroll(amount);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (position == null) return;
        ensureLayout();

        for (InputEventAPI event : events) {
            if (event.isConsumed()) continue;

            if (draggingThumb && event.isMouseMoveEvent()) {
                dragThumb(event.getY());
                event.consume();
            } else if (draggingText && event.isMouseMoveEvent()) {
                mouseX = event.getX();
                mouseY = event.getY();
                select(mouseX, mouseY);
                event.consume();
            } else if ((draggingThumb || draggingText) && event.isLMBUpEvent()) {
                draggingThumb = false;
                draggingText = false;
                event.consume();
            } else if (event.isLMBDownEvent()) {
                if (contains(event)) {
                    click(event);
                    event.consume();
                } else focused = false;
            } else if (event.isMouseScrollEvent() && contains(event)) {
                scroll -= Math.signum(event.getEventValue()) * WHEEL_LINES * lineHeight;
                clampScroll();
                event.consume();
            } else if (focused && event.isKeyboardEvent()) {
                // the dialog hands its children input last added first, so this runs before the option hotkeys
                if (event.isKeyDownEvent() || event.isRepeat()) handleKey(event);
                event.consume();
            }
        }
    }

    // own hit test, containsEvent casts the event to the engine's internal type
    private boolean contains(InputEventAPI event) {
        float x = event.getX(), y = event.getY();
        return x >= position.getX() && x <= position.getX() + position.getWidth()
                && y >= position.getY() && y <= position.getY() + position.getHeight();
    }

    private void click(InputEventAPI event) {
        focus();
        float x = event.getX(), y = event.getY();

        if (maxScroll() > 0f && x >= barX() - BAR_GAP / 2f) {
            float top = thumbTop();
            if (y <= top && y >= top - thumbHeight()) {
                draggingThumb = true;
                dragOffset = top - y;
            } else {
                scroll += y > top ? -viewHeight() : viewHeight();
                clampScroll();
            }
            return;
        }

        int index = indexAt(x, y);
        if (event.isShiftDown()) {
            moveTo(index, true);
        } else if (event.isDoubleClick()) {
            anchor = wordStart(index);
            cursor = wordEnd(index);
            preferredX = -1f;
        } else {
            moveTo(index, false);
            draggingText = true;
            mouseX = x;
            mouseY = y;
        }
    }

    private void focus() {
        blink = 0f;
        if (focused) return;
        for (MultilineTextField other : focusGroup) other.focused = false;
        focused = true;
        Global.getSoundPlayer().playUISound("ui_typer_buzz", 1f, 1f);
    }

    private void dragThumb(float y) {
        float travel = viewHeight() - thumbHeight();
        if (travel <= 0f) return;
        scroll = (textTop() - (y + dragOffset)) / travel * maxScroll();
        clampScroll();
    }

    // the mouse rests outside the box while the text scrolls, so this runs per frame and not per mouse event
    private void dragScroll(float amount) {
        float step = DRAG_SCROLL_LINES * lineHeight * amount;
        if (mouseY > textTop()) scroll -= step;
        else if (mouseY < textTop() - viewHeight()) scroll += step;
        else return;
        clampScroll();
        select(mouseX, mouseY);
    }

    private void select(float x, float y) {
        cursor = indexAt(x, y);
        preferredX = -1f;
        blink = 0f;
        wordOpen = false;
    }

    private void handleKey(InputEventAPI event) {
        int key = event.getEventValue();
        boolean shift = event.isShiftDown();
        // altgr reports ctrl and alt, so only plain ctrl counts as a shortcut
        boolean ctrl = event.isCtrlDown() && !event.isAltDown();

        switch (key) {
            case Keyboard.KEY_ESCAPE -> {
                focused = false;
                anchor = cursor;
            }
            case Keyboard.KEY_TAB -> {
            }
            case Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> insert("\n");
            case Keyboard.KEY_BACK -> {
                if (hasSelection()) replace(selectionStart(), selectionEnd(), "", false);
                else replace(ctrl ? wordLeft(cursor) : cursor - 1, cursor, "", false);
            }
            case Keyboard.KEY_DELETE -> {
                if (hasSelection()) replace(selectionStart(), selectionEnd(), "", false);
                else replace(cursor, ctrl ? wordRight(cursor) : cursor + 1, "", false);
            }
            case Keyboard.KEY_LEFT -> {
                if (hasSelection() && !shift && !ctrl) moveTo(selectionStart(), false);
                else moveTo(ctrl ? wordLeft(cursor) : Math.max(0, cursor - 1), shift);
            }
            case Keyboard.KEY_RIGHT -> {
                if (hasSelection() && !shift && !ctrl) moveTo(selectionEnd(), false);
                else moveTo(ctrl ? wordRight(cursor) : Math.min(text.length(), cursor + 1), shift);
            }
            case Keyboard.KEY_UP -> moveLines(-1, shift);
            case Keyboard.KEY_DOWN -> moveLines(1, shift);
            case Keyboard.KEY_PRIOR -> moveLines(-visibleLines(), shift);
            case Keyboard.KEY_NEXT -> moveLines(visibleLines(), shift);
            case Keyboard.KEY_HOME -> moveTo(ctrl ? 0 : lines.get(lineOf(cursor))[0], shift);
            case Keyboard.KEY_END -> moveTo(ctrl ? text.length() : lastPos(lineOf(cursor)), shift);
            default -> {
                if (ctrl) shortcut(key, shift);
                else if (event.getEventChar() != 0) {
                    String typed = clean(String.valueOf(event.getEventChar()));
                    if (!typed.isEmpty()) replace(selectionStart(), selectionEnd(), typed, !Character.isWhitespace(typed.charAt(0)));
                }
            }
        }
    }

    private void shortcut(int key, boolean shift) {
        switch (key) {
            case Keyboard.KEY_A -> {
                anchor = 0;
                cursor = text.length();
                preferredX = -1f;
            }
            case Keyboard.KEY_C -> copy();
            case Keyboard.KEY_X -> {
                if (copy()) replace(selectionStart(), selectionEnd(), "", false);
            }
            case Keyboard.KEY_V -> paste();
            case Keyboard.KEY_Z -> {
                if (shift) restore(redo, undo);
                else restore(undo, redo);
            }
            case Keyboard.KEY_Y -> restore(redo, undo);
            default -> {
            }
        }
    }

    private boolean copy() {
        if (!hasSelection()) return false;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text.substring(selectionStart(), selectionEnd())), null);
            return true;
        } catch (Exception ex) {
            buzz();
            return false;
        }
    }

    private void paste() {
        try {
            Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (data instanceof String pasted) insert(clean(pasted));
        } catch (Exception ex) {
            buzz();
        }
    }

    private void insert(String s) {
        replace(selectionStart(), selectionEnd(), s, false);
    }

    // every edit comes through here. typing a word only remembers its first letter for undo
    private void replace(int from, int to, String s, boolean wordChar) {
        from = Math.max(0, from);
        to = Math.min(text.length(), to);
        int room = MAX_CHARS - text.length() + Math.max(0, to - from);
        if (s.length() > room) s = s.substring(0, Math.max(0, room));
        if (from >= to && s.isEmpty()) {
            buzz();
            return;
        }

        if (!(wordChar && wordOpen && from == to && from == cursor)) remember();
        text.replace(from, to, s);
        cursor = from + s.length();
        anchor = cursor;
        wordOpen = wordChar;
        edited();
    }

    private void remember() {
        undo.push(snapshot());
        while (undo.size() > UNDO_LIMIT) undo.removeLast();
        redo.clear();
    }

    private Snapshot snapshot() {
        return new Snapshot(text.toString(), cursor, anchor);
    }

    private void restore(Deque<Snapshot> from, Deque<Snapshot> to) {
        if (from.isEmpty()) {
            buzz();
            return;
        }
        to.push(snapshot());
        Snapshot state = from.pop();
        text.setLength(0);
        text.append(state.text());
        cursor = state.cursor();
        anchor = state.anchor();
        wordOpen = false;
        edited();
    }

    private void edited() {
        relayout();
        preferredX = -1f;
        blink = 0f;
        showCursor();
        Global.getSoundPlayer().playUISound("ui_typer_type", 1f, 1f);
    }

    private void moveTo(int index, boolean extend) {
        cursor = index;
        if (!extend) anchor = index;
        preferredX = -1f;
        blink = 0f;
        wordOpen = false;
        showCursor();
    }

    private void moveLines(int delta, boolean extend) {
        int line = lineOf(cursor);
        if (preferredX < 0f) preferredX = width(lines.get(line)[0], cursor);
        int target = line + delta;
        if (target < 0) cursor = 0;
        else if (target >= lines.size()) cursor = text.length();
        else cursor = indexAt(target, preferredX);
        if (!extend) anchor = cursor;
        blink = 0f;
        wordOpen = false;
        showCursor();
    }

    private boolean hasSelection() {
        return anchor != cursor;
    }

    private int selectionStart() {
        return Math.min(anchor, cursor);
    }

    private int selectionEnd() {
        return Math.max(anchor, cursor);
    }

    private void showCursor() {
        float top = lineOf(cursor) * lineHeight;
        if (top < scroll) scroll = top;
        else if (top + lineHeight > scroll + viewHeight()) scroll = top + lineHeight - viewHeight();
        clampScroll();
    }

    private int wordLeft(int index) {
        while (index > 0 && Character.isWhitespace(text.charAt(index - 1))) index--;
        while (index > 0 && !Character.isWhitespace(text.charAt(index - 1))) index--;
        return index;
    }

    private int wordRight(int index) {
        int length = text.length();
        while (index < length && !Character.isWhitespace(text.charAt(index))) index++;
        while (index < length && Character.isWhitespace(text.charAt(index))) index++;
        return index;
    }

    private int wordStart(int index) {
        while (index > 0 && !Character.isWhitespace(text.charAt(index - 1))) index--;
        return index;
    }

    private int wordEnd(int index) {
        int length = text.length();
        while (index < length && !Character.isWhitespace(text.charAt(index))) index++;
        return index;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ')
                .replace('‘', '\'').replace('’', '\'').replace('“', '"').replace('”', '"')
                .replace('–', '-').replace('—', '-').replace("…", "...");
    }

    private String clean(String s) {
        StringBuilder out = new StringBuilder();
        for (char c : normalize(s).toCharArray()) {
            if (c == '\n' || isValidChar(c)) out.append(c);
        }
        return out.toString();
    }

    // same rules as vanilla TextFieldAPI.isValidChar, lazyfont hands back the '?' glyph for chars the font lacks
    private boolean isValidChar(char c) {
        if (c < 32 || c == 127 || c == '%' || c == '$') return false;
        return c == '?' || font.getChar(c) != font.getChar('?');
    }

    private void buzz() {
        Global.getSoundPlayer().playUISound("ui_typer_buzz", 1f, 1f);
    }

    private void ensureLayout() {
        float width = textWidth();
        if (width == layoutWidth) return;
        layoutWidth = width;
        relayout();
    }

    private void relayout() {
        lines.clear();
        disposeDrawn();

        int start = 0;
        while (true) {
            int newline = text.indexOf("\n", start);
            wrap(start, newline < 0 ? text.length() : newline);
            if (newline < 0) break;
            start = newline + 1;
        }

        clampScroll();
    }

    private void wrap(int start, int end) {
        if (start == end) {
            lines.add(new int[]{start, end});
            return;
        }

        while (start < end) {
            int cut = fit(start, end);
            if (cut < end) {
                int space = text.lastIndexOf(" ", cut);
                if (space >= start) cut = space + 1;
            }
            lines.add(new int[]{start, cut});
            start = cut;
        }
    }

    private int fit(int start, int end) {
        if (width(start, end) <= layoutWidth) return end;
        int low = start + 1;
        int high = end;
        if (width(start, low) > layoutWidth) return low;
        while (high - low > 1) {
            int mid = (low + high) >>> 1;
            if (width(start, mid) <= layoutWidth) low = mid;
            else high = mid;
        }
        return low;
    }

    private float width(int start, int end) {
        return font.calcWidth(text.substring(start, end), fontSize);
    }

    private int lineOf(int index) {
        for (int i = 0; i < lines.size(); i++) {
            if (index <= lastPos(i)) return i;
        }
        return lines.size() - 1;
    }

    // a wrapped line hands its end index to the next line, so the cursor never sits behind the wrap space
    private int lastPos(int line) {
        int end = lines.get(line)[1];
        boolean wrapped = line + 1 < lines.size() && lines.get(line + 1)[0] == end;
        return wrapped ? end - 1 : end;
    }

    private int indexAt(float x, float y) {
        int line = (int) Math.floor((textTop() - y + scroll) / lineHeight);
        return indexAt(Math.max(0, Math.min(lines.size() - 1, line)), x - textLeft());
    }

    private int indexAt(int line, float x) {
        int start = lines.get(line)[0];
        int last = lastPos(line);
        int best = start;
        float bestDistance = Float.MAX_VALUE;
        for (int i = start; i <= last; i++) {
            float distance = Math.abs(width(start, i) - x);
            if (distance < bestDistance) {
                best = i;
                bestDistance = distance;
            } else if (distance > bestDistance) break;
        }
        return best;
    }

    private int visibleLines() {
        return Math.max(1, (int) (viewHeight() / lineHeight));
    }

    private void clampScroll() {
        scroll = Math.max(0f, Math.min(maxScroll(), scroll));
    }

    private float maxScroll() {
        return Math.max(0f, lines.size() * lineHeight - viewHeight());
    }

    private float textLeft() {
        return position.getX() + PAD;
    }

    private float textTop() {
        return position.getY() + position.getHeight() - PAD;
    }

    private float textWidth() {
        return position.getWidth() - 2f * PAD - BAR_GAP - BAR_WIDTH;
    }

    private float viewHeight() {
        return position.getHeight() - 2f * PAD;
    }

    private float barX() {
        return position.getX() + position.getWidth() - PAD - BAR_WIDTH;
    }

    private float thumbHeight() {
        float content = lines.size() * lineHeight;
        return Math.min(viewHeight(), Math.max(MIN_THUMB_HEIGHT, viewHeight() * viewHeight() / content));
    }

    private float thumbTop() {
        float max = maxScroll();
        return textTop() - (max > 0f ? (viewHeight() - thumbHeight()) * scroll / max : 0f);
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (position == null) return;
        drawQuad(position.getX(), position.getY(), position.getWidth(), position.getHeight(),
                Misc.getDarkPlayerColor().darker(), alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (position == null) return;
        ensureLayout();

        float x = position.getX();
        float y = position.getY();
        float w = position.getWidth();
        float h = position.getHeight();
        drawBorder(x, y, w, h, focused ? Misc.getBasePlayerColor() : Misc.getDarkPlayerColor(), alphaMult);

        boolean parentClip = pushScissor(x, y + PAD, w, viewHeight());

        int first = Math.max(0, (int) (scroll / lineHeight));
        int last = Math.min(lines.size() - 1, (int) ((scroll + viewHeight()) / lineHeight));
        if (focused && hasSelection()) {
            Color highlight = withAlpha(Misc.getBasePlayerColor(), 0.35f);
            for (int i = first; i <= last; i++) drawSelection(i, highlight, alphaMult);
        }

        Color color = textColor(alphaMult);
        for (int i = first; i <= last; i++) {
            LazyFont.DrawableString line = drawn(i);
            line.setBaseColor(color);
            line.draw(textLeft(), textTop() + scroll - i * lineHeight - 1f);
        }

        if (focused && blink < BLINK_TIME) {
            int line = lineOf(cursor);
            float cursorX = textLeft() + width(lines.get(line)[0], cursor);
            float lineTop = textTop() + scroll - line * lineHeight;
            drawQuad(cursorX, lineTop - lineHeight, 1f, lineHeight, Misc.getBasePlayerColor(), alphaMult);
        }

        popScissor(parentClip);

        if (maxScroll() > 0f) {
            drawQuad(barX(), y + PAD, BAR_WIDTH, viewHeight(), Misc.getDarkPlayerColor(), alphaMult);
            drawQuad(barX(), thumbTop() - thumbHeight(), BAR_WIDTH, thumbHeight(), Misc.getBasePlayerColor(), alphaMult);
        }
    }

    // a selection that runs on past the line end gets a little extra width, so selected line breaks show
    private void drawSelection(int line, Color highlight, float alphaMult) {
        int start = lines.get(line)[0];
        int end = lines.get(line)[1];
        int from = Math.max(start, selectionStart());
        int to = Math.min(end, selectionEnd());
        boolean pastEnd = selectionEnd() > end;
        if (from > to || (from == to && !pastEnd)) return;

        float left = textLeft() + width(start, from);
        float right = textLeft() + width(start, to) + (pastEnd ? LINE_BREAK_WIDTH : 0f);
        float top = textTop() + scroll - line * lineHeight;
        drawQuad(left, top - lineHeight, right - left, lineHeight, highlight, alphaMult);
    }

    // vanilla scroll panels clip with their own scissor, so intersect with it and put it back after
    private static boolean pushScissor(float x, float y, float w, float h) {
        boolean parentClip = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, SCISSOR_BOX);

        // glScissor takes physical pixels, ui positions are virtual
        float scale = Global.getSettings().getScreenScaleMult();
        int left = (int) (x * scale);
        int bottom = (int) (y * scale);
        int right = left + (int) Math.ceil(w * scale);
        int top = bottom + (int) Math.ceil(h * scale);

        if (parentClip) {
            left = Math.max(left, SCISSOR_BOX.get(0));
            bottom = Math.max(bottom, SCISSOR_BOX.get(1));
            right = Math.min(right, SCISSOR_BOX.get(0) + SCISSOR_BOX.get(2));
            top = Math.min(top, SCISSOR_BOX.get(1) + SCISSOR_BOX.get(3));
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left, bottom, Math.max(0, right - left), Math.max(0, top - bottom));
        return parentClip;
    }

    private static void popScissor(boolean parentClip) {
        if (parentClip) GL11.glScissor(SCISSOR_BOX.get(0), SCISSOR_BOX.get(1), SCISSOR_BOX.get(2), SCISSOR_BOX.get(3));
        else GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private LazyFont.DrawableString drawn(int line) {
        LazyFont.DrawableString string = drawn.get(line);
        if (string == null) {
            int[] range = lines.get(line);
            string = font.createText(text.substring(range[0], range[1]), textColor, fontSize);
            string.setAnchor(LazyFont.TextAnchor.TOP_LEFT);
            drawn.put(line, string);
        }
        return string;
    }

    private void disposeDrawn() {
        for (LazyFont.DrawableString string : drawn.values()) string.dispose();
        drawn.clear();
    }

    private Color textColor(float alphaMult) {
        if (textColor == null || alphaMult != textAlpha) {
            textAlpha = alphaMult;
            textColor = withAlpha(Misc.getBasePlayerColor(), alphaMult);
        }
        return textColor;
    }

    private static Color withAlpha(Color color, float alphaMult) {
        int alpha = Math.max(0, Math.min(255, Math.round(color.getAlpha() * alphaMult)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static void drawQuad(float x, float y, float w, float h, Color color, float alphaMult) {
        setColor(color, alphaMult);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void drawBorder(float x, float y, float w, float h, Color color, float alphaMult) {
        setColor(color, alphaMult);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void setColor(Color color, float alphaMult) {
        Color c = withAlpha(color, alphaMult);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4ub((byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue(), (byte) c.getAlpha());
    }

    @Override
    public void buttonPressed(Object buttonId) {
    }
}
