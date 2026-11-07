package livecheck;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.PositionAPI;
import illustratedEntities.dialogue.panel.MultilineTextField;
import illustratedEntities.helper.TextHandler;
import illustratedEntities.memory.TextDataEntry;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.input.Keyboard;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// keys and mouse go straight to processInput, ClickChecks does real mouse and keyboard. Timers.FakeClipboard keeps
// copy and paste off the clipboard of whoever sits at the pc
public final class EditorChecks {
    private static final int FRAMES = 300;
    private static final int TYPED = 200;
    private static final int RANDOM_KEYS = 4000;
    private static final int[] RANDOM_POOL = {Keyboard.KEY_LEFT, Keyboard.KEY_RIGHT, Keyboard.KEY_UP, Keyboard.KEY_DOWN, Keyboard.KEY_HOME, Keyboard.KEY_END,
            Keyboard.KEY_PRIOR, Keyboard.KEY_NEXT, Keyboard.KEY_BACK, Keyboard.KEY_DELETE, Keyboard.KEY_RETURN, Keyboard.KEY_A, Keyboard.KEY_C,
            Keyboard.KEY_X, Keyboard.KEY_V, Keyboard.KEY_Z, Keyboard.KEY_Y, 0, 0, 0, 0, 0, 0};

    private EditorChecks() {
    }

    public static List<Step> editor() {
        List<Step> steps = new ArrayList<>();
        steps.add(EditorChecks::open);
        steps.add(c -> Ui.press(c, "desc"));
        steps.add(EditorChecks::keys);
        steps.add(EditorChecks::focus);
        steps.add(EditorChecks::cap);
        steps.add(EditorChecks::navigation);
        steps.add(EditorChecks::selection);
        steps.add(c -> {
            MultilineTextField f = field(0);
            focus(f);
            clear(f);
            type(f, "selected text shows a highlight");
            press(f, Keyboard.KEY_HOME, false, false);
            press(f, Keyboard.KEY_RIGHT, true, true);
            c.shot("editor-selection");
        });
        steps.add(EditorChecks::clipboard);
        steps.add(EditorChecks::undo);
        steps.add(EditorChecks::mouse);
        steps.add(EditorChecks::dragScroll);
        steps.add(EditorChecks::randomKeys);
        steps.add(EditorChecks::typingCost);
        steps.add(c -> Perf.sample(c, FRAMES));
        steps.add(c -> {
            Perf.report("editor 5000 chars");
            Perf.dump("editor 5000 chars");
            Perf.textPixels.clear();
            Perf.readText = true;
            c.frames(10);
        });
        steps.add(EditorChecks::textDrawn);
        steps.add(c -> c.shot("editor-long"));
        steps.add(EditorChecks::apply);
        steps.add(EditorChecks::applied);
        steps.add(EditorChecks::resetRestores);
        steps.add(c -> Ui.press(c, "clear"));
        steps.add(c -> {
            c.expect(field(0).getText().isEmpty(), "clear empties the box");
            Ui.press(c, "reset");
        });
        steps.add(c -> {
            c.expect("apply text".equals(field(0).getText()), "reset after clear brings the saved text back");
            Perf.popupCallback = null;
            Ui.press(c, "delete");
        });
        steps.add(c -> {
            c.expect(Perf.popupCallback != null, "delete opens the popup");
            c.expect(Ui.hasButton("confirm"), "editor stays open behind the popup");
            if (Perf.popupCallback != null) Perf.popupCallback.dismissCustomDialog(1);
            c.frames(30);
        });
        steps.add(c -> {
            c.expect("cancel".equals(Perf.popupResult), "popup cancels");
            c.expect(TextHandler.getDataForEntity(Ui.entity(c, "editorColony")) != null, "cancel keeps the description");
            c.expect("apply text".equals(field(0).getText()), "cancel keeps the text in the box");
            Perf.popupCallback = null;
            Ui.press(c, "delete");
        });
        steps.add(c -> {
            if (Perf.popupCallback != null) Perf.popupCallback.dismissCustomDialog(0);
            c.frames(30);
        });
        steps.add(c -> {
            c.expect("confirm".equals(Perf.popupResult), "popup confirms");
            c.expect(TextHandler.getDataForEntity(Ui.entity(c, "editorColony")) == null, "delete button removes the description");
            c.expect(field(0).getText().isEmpty(), "editor is empty after delete");
            Ui.press(c, "return");
        });
        steps.add(c -> {
            c.expect(Ui.hasButton("desc"), "return leaves the editor");
            Ui.closeDialog(c);
        });
        return steps;
    }

    private static void open(Ctx c) {
        PlanetAPI planet = Ui.newColony(c, "editorColony");
        Colony.jump(planet.getContainingLocation(), planet.getLocation().x, planet.getLocation().y + 300f);
        Ui.openDialog(c, planet);
    }

    private static void keys(Ctx c) throws Throwable {
        MultilineTextField f = field(0);
        focus(f);
        clear(f);
        type(f, "one two three");
        c.expect("one two three".equals(f.getText()), "typing adds text");
        press(f, Keyboard.KEY_LEFT, true, false);
        c.expect(cursor(f) == 8, "ctrl left goes back a word");
        type(f, "x");
        c.expect("one two xthree".equals(f.getText()), "typing goes in at the cursor");
        press(f, Keyboard.KEY_HOME, false, false);
        c.expect(cursor(f) == 0, "home goes to the line start");
        press(f, Keyboard.KEY_DELETE, false, false);
        c.expect("ne two xthree".equals(f.getText()), "delete removes the next character");
        press(f, Keyboard.KEY_DELETE, true, false);
        c.expect("two xthree".equals(f.getText()), "ctrl delete removes the next word");
        press(f, Keyboard.KEY_END, false, false);
        c.expect(cursor(f) == f.getText().length(), "end goes to the line end");
        press(f, Keyboard.KEY_BACK, false, false);
        c.expect("two xthre".equals(f.getText()), "backspace removes the last character");
        press(f, Keyboard.KEY_BACK, true, false);
        c.expect("two ".equals(f.getText()), "ctrl backspace removes the last word");
        press(f, Keyboard.KEY_RETURN, false, false);
        type(f, "%$a’");
        press(f, Keyboard.KEY_TAB, false, false);
        c.expect("two \na'".equals(f.getText()), "enter adds a line, percent, dollar and tab are refused, curly quotes go straight");
        press(f, Keyboard.KEY_UP, false, false);
        c.expect(cursor(f) == 2, "up keeps the column (" + cursor(f) + ")");
        press(f, Keyboard.KEY_DOWN, false, false);
        c.expect(cursor(f) == f.getText().length(), "down keeps the column (" + cursor(f) + ")");
        press(f, Keyboard.KEY_BACK, false, true);
        c.expect("two \na".equals(f.getText()), "shift backspace works like backspace");
        clear(f);
    }

    private static void focus(Ctx c) throws Throwable {
        MultilineTextField first = field(0), second = field(1);
        focus(second);
        c.expect(!focused(first), "focus moves to the other box");
        type(second, "b");
        c.expect("b".equals(second.getText()) && first.getText().isEmpty(), "keys go to the focused box only");

        press(second, Keyboard.KEY_ESCAPE, false, false);
        c.expect(!focused(second), "escape leaves the box");
        KeyEvent event = new KeyEvent(0, 'x', false, false);
        second.processInput(single(event));
        c.expect("b".equals(second.getText()) && !event.isConsumed(), "keys pass through an unfocused box");

        focus(second);
        clear(second);
    }

    private static void cap(Ctx c) throws Throwable {
        MultilineTextField f = field(0);
        focus(f);
        insert(f, "x".repeat(MultilineTextField.MAX_CHARS + 1000));
        c.expect(f.getText().length() == MultilineTextField.MAX_CHARS, "paste stops at 5000 characters (" + f.getText().length() + ")");
        type(f, "y");
        c.expect(f.getText().length() == MultilineTextField.MAX_CHARS, "typing stops at 5000 characters");
        press(f, Keyboard.KEY_BACK, false, false);
        type(f, "y");
        c.expect(f.getText().length() == MultilineTextField.MAX_CHARS && f.getText().endsWith("y"), "one character free takes one character");
        clear(f);
    }

    private static void navigation(Ctx c) throws Throwable {
        MultilineTextField f = field(0);
        focus(f);
        insert(f, longText());
        c.expect(scroll(f) > 0f, "box scrolls to the cursor after a long paste (" + scroll(f) + ")");
        press(f, Keyboard.KEY_HOME, true, false);
        c.expect(cursor(f) == 0 && scroll(f) == 0f, "ctrl home goes to the top");
        press(f, Keyboard.KEY_NEXT, false, false);
        c.expect(cursor(f) > 0 && scroll(f) > 0f, "page down moves and scrolls");
        press(f, Keyboard.KEY_PRIOR, false, false);
        c.expect(scroll(f) == 0f, "page up scrolls back");
        press(f, Keyboard.KEY_END, true, false);
        c.expect(cursor(f) == f.getText().length(), "ctrl end goes to the bottom");
    }

    private static void selection(Ctx c) throws Throwable {
        MultilineTextField f = field(0);
        focus(f);
        clear(f);
        type(f, "one two three");
        press(f, Keyboard.KEY_HOME, false, false);
        for (int i = 0; i < 3; i++) press(f, Keyboard.KEY_RIGHT, false, true);
        c.expect(selected(f).equals("one"), "shift right selects (" + selected(f) + ")");
        type(f, "1");
        c.expect("1 two three".equals(f.getText()), "typing replaces the selection");
        press(f, Keyboard.KEY_END, false, false);
        press(f, Keyboard.KEY_LEFT, true, true);
        c.expect(selected(f).equals("three"), "ctrl shift left selects a word (" + selected(f) + ")");
        press(f, Keyboard.KEY_BACK, false, false);
        c.expect("1 two ".equals(f.getText()), "backspace removes the selection");
        press(f, Keyboard.KEY_HOME, false, true);
        press(f, Keyboard.KEY_LEFT, false, false);
        c.expect(cursor(f) == 0 && selected(f).isEmpty(), "left drops the selection at its start");
        press(f, Keyboard.KEY_END, false, true);
        press(f, Keyboard.KEY_RIGHT, false, false);
        c.expect(cursor(f) == f.getText().length() && selected(f).isEmpty(), "right drops the selection at its end");
        press(f, Keyboard.KEY_RETURN, false, false);
        type(f, "next");
        press(f, Keyboard.KEY_UP, false, true);
        c.expect(selected(f).endsWith("\nnext") && selected(f).length() > 5, "shift up selects across lines (" + selected(f).replace("\n", "|") + ")");
        press(f, Keyboard.KEY_DELETE, false, false);
        c.expect(!f.getText().contains("next"), "delete removes a selection across lines");
        press(f, Keyboard.KEY_A, true, false);
        c.expect(selected(f).equals(f.getText()), "ctrl a selects all");
        press(f, Keyboard.KEY_ESCAPE, false, false);
        focus(f);
        c.expect(selected(f).isEmpty(), "escape drops the selection");
    }

    private static void clipboard(Ctx c) throws Throwable {
        MultilineTextField f = field(0);
        focus(f);
        clear(f);
        // a paste only reads, so it proves the fake clipboard is in before anything writes to a clipboard
        Perf.clipboard = "fake clipboard";
        press(f, Keyboard.KEY_V, true, false);
        c.expect("fake clipboard".equals(f.getText()), "copy and paste use the fake clipboard in this run");
        if (!"fake clipboard".equals(f.getText())) return;
        clear(f);
        Perf.clipboard = "untouched";
        type(f, "copy me");
        press(f, Keyboard.KEY_C, true, false);
        c.expect("untouched".equals(Perf.clipboard), "ctrl c without a selection copies nothing");
        press(f, Keyboard.KEY_A, true, false);
        press(f, Keyboard.KEY_C, true, false);
        c.expect("copy me".equals(Perf.clipboard), "ctrl c copies the selection");
        press(f, Keyboard.KEY_X, true, false);
        c.expect(f.getText().isEmpty() && "copy me".equals(Perf.clipboard), "ctrl x cuts the selection");
        press(f, Keyboard.KEY_V, true, false);
        c.expect("copy me".equals(f.getText()), "ctrl v pastes");
        Perf.clipboard = "new";
        press(f, Keyboard.KEY_HOME, false, false);
        for (int i = 0; i < 4; i++) press(f, Keyboard.KEY_RIGHT, false, true);
        press(f, Keyboard.KEY_V, true, false);
        c.expect("new me".equals(f.getText()), "ctrl v replaces the selection");
        Perf.clipboard = "x".repeat(20);
        clear(f);
        insert(f, "y".repeat(MultilineTextField.MAX_CHARS - 1));
        for (int i = 0; i < 10; i++) press(f, Keyboard.KEY_LEFT, false, true);
        press(f, Keyboard.KEY_V, true, false);
        c.expect(f.getText().length() == MultilineTextField.MAX_CHARS, "paste over a selection still stops at 5000 characters (" + f.getText().length() + ")");
        clear(f);
    }

    private static void undo(Ctx c) throws Throwable {
        MultilineTextField f = field(0);
        focus(f);
        clear(f);
        type(f, "hello world");
        press(f, Keyboard.KEY_Z, true, false);
        c.expect("hello ".equals(f.getText()), "ctrl z undoes the last word (" + f.getText() + ")");
        press(f, Keyboard.KEY_Z, true, false);
        press(f, Keyboard.KEY_Z, true, false);
        c.expect(f.getText().isEmpty(), "ctrl z goes back to before typing (" + f.getText() + ")");
        press(f, Keyboard.KEY_Y, true, false);
        c.expect("hello".equals(f.getText()), "ctrl y redoes (" + f.getText() + ")");
        press(f, Keyboard.KEY_Z, true, true);
        c.expect("hello ".equals(f.getText()), "ctrl shift z redoes (" + f.getText() + ")");
        type(f, "x");
        press(f, Keyboard.KEY_Y, true, false);
        c.expect("hello x".equals(f.getText()), "typing drops the redo steps");
        press(f, Keyboard.KEY_A, true, false);
        press(f, Keyboard.KEY_BACK, false, false);
        press(f, Keyboard.KEY_Z, true, false);
        c.expect("hello x".equals(f.getText()), "ctrl z brings a deleted selection back");
        clear(f);
    }

    private static void mouse(Ctx c) throws Throwable {
        MultilineTextField f = field(0);
        focus(f);
        clear(f);
        type(f, "alpha beta gamma");
        PositionAPI p = position(f);
        float y = p.getY() + p.getHeight() - 12f;
        float left = p.getX() + 6f;
        float right = p.getX() + p.getWidth() - 20f;

        mouse(f, MouseEvent.down(left, y, false, false), MouseEvent.move(right, y), MouseEvent.up(right, y));
        c.expect("alpha beta gamma".equals(selected(f)), "drag selects (" + selected(f) + ")");
        mouse(f, MouseEvent.down(left, y, false, false), MouseEvent.up(left, y));
        c.expect(selected(f).isEmpty() && cursor(f) == 0, "click drops the selection and places the cursor");
        mouse(f, MouseEvent.down(right, y, true, false), MouseEvent.up(right, y));
        c.expect("alpha beta gamma".equals(selected(f)), "shift click extends the selection");
        float beta = left + textWidth("alpha be");
        mouse(f, MouseEvent.down(beta, y, false, true), MouseEvent.up(beta, y));
        c.expect("beta".equals(selected(f)), "double click selects a word (" + selected(f) + ")");

        MouseEvent outside = MouseEvent.down(p.getX() - 50f, y, false, false);
        mouse(f, outside, MouseEvent.up(p.getX() - 50f, y));
        c.expect(!focused(f) && !outside.isConsumed(), "click outside leaves the box and passes the click on");
        focus(f);
        clear(f);
    }

    // the mouse rests below the box, advance keeps scrolling and moving the selection end
    private static void dragScroll(Ctx c) throws Throwable {
        MultilineTextField f = field(0);
        focus(f);
        clear(f);
        insert(f, longText());
        press(f, Keyboard.KEY_HOME, true, false);
        PositionAPI p = position(f);
        float x = p.getX() + 20f;
        mouse(f, MouseEvent.down(x, p.getY() + p.getHeight() - 12f, false, false), MouseEvent.move(x, p.getY() - 20f));
        int before = cursor(f);
        for (int i = 0; i < 30; i++) f.advance(1f / 60f);
        c.expect(scroll(f) > 0f && cursor(f) > before, "dragging below the box scrolls and selects on (" + scroll(f) + ")");
        mouse(f, MouseEvent.up(x, p.getY() - 20f));
        float after = scroll(f);
        f.advance(0.5f);
        c.expect(scroll(f) == after, "releasing the mouse stops the scroll");
        clear(f);
    }

    // seeded, a failure repeats. each press has to leave cursor, selection and scroll in range
    private static void randomKeys(Ctx c) throws Throwable {
        MultilineTextField f = field(0);
        Random random = new Random(1);
        Perf.clipboard = "pasted ";
        int bad = 0;
        for (int i = 0; i < RANDOM_KEYS; i++) {
            int key = RANDOM_POOL[random.nextInt(RANDOM_POOL.length)];
            char character = key == 0 ? (char) ('a' + random.nextInt(26)) : 0;
            if (key == 0 && random.nextInt(5) == 0) character = ' ';
            press(f, key, character, random.nextInt(4) == 0, random.nextInt(4) == 0);
            int length = f.getText().length();
            if (cursor(f) < 0 || cursor(f) > length || anchor(f) < 0 || anchor(f) > length || length > MultilineTextField.MAX_CHARS || scroll(f) < 0f) bad++;
        }
        c.expect(bad == 0, RANDOM_KEYS + " random keys keep the cursor, selection and scroll in range (" + bad + " bad)");
    }

    private static void typingCost(Ctx c) throws Throwable {
        MultilineTextField f = field(0), other = field(1);
        focus(other);
        insert(other, longText());
        focus(f);
        clear(f);
        insert(f, longText());
        Perf.clear();
        for (int i = 0; i < TYPED; i++) {
            press(f, Keyboard.KEY_BACK, false, false);
            type(f, "y");
        }
        Perf.dump("typing at 5000");
        press(f, Keyboard.KEY_HOME, true, false);
        press(f, Keyboard.KEY_NEXT, false, false);
    }

    private static void textDrawn(Ctx c) throws Throwable {
        Perf.readText = false;
        for (int i = 0; i < 2; i++) {
            int[] pixels = Perf.textPixels.get(field(i));
            c.expect(pixels != null && pixels[0] > 1000, "long text draws in box " + (i + 1) + " (" + (pixels == null ? "none" : pixels[0]) + " bright pixels)");
            c.expect(pixels != null && pixels[1] < 5, "long text stays inside box " + (i + 1) + " (" + (pixels == null ? "none" : pixels[1]) + " bright pixels around it)");
        }
    }

    private static void apply(Ctx c) throws Throwable {
        MultilineTextField first = field(0), second = field(1);
        focus(first);
        clear(first);
        type(first, "apply text");
        focus(second);
        clear(second);
        type(second, "docked text");
        Ui.press(c, "confirm");
    }

    private static void applied(Ctx c) throws Throwable {
        TextDataEntry data = TextHandler.getDataForEntity(Ui.entity(c, "editorColony"));
        c.expect(data != null && "apply text".equals(data.getText(1)) && "docked text".equals(data.getText(2)), "apply button saves both boxes");
        MultilineTextField f = field(0);
        focus(f);
        type(f, " changed");
        Ui.press(c, "reset");
    }

    private static void resetRestores(Ctx c) throws Throwable {
        c.expect("apply text".equals(field(0).getText()), "reset drops unsaved typing");
        SectorEntityToken t = Ui.entity(c, "editorColony");
        c.expect(t.getCustomDescriptionId() != null, "colony shows the applied description");
    }

    static MultilineTextField field(int index) throws Throwable {
        return fields().get(index);
    }

    @SuppressWarnings("unchecked")
    static List<MultilineTextField> fields() throws Throwable {
        return (List<MultilineTextField>) lookup().findVarHandle(MultilineTextField.class, "focusGroup", List.class).get(Perf.field);
    }

    static void focus(MultilineTextField f) throws Throwable {
        lookup().findVirtual(MultilineTextField.class, "focus", MethodType.methodType(void.class)).invoke(f);
    }

    static boolean focused(MultilineTextField f) throws Throwable {
        return (boolean) lookup().findVarHandle(MultilineTextField.class, "focused", boolean.class).get(f);
    }

    static float scroll(MultilineTextField f) throws Throwable {
        return (float) lookup().findVarHandle(MultilineTextField.class, "scroll", float.class).get(f);
    }

    static PositionAPI position(MultilineTextField f) throws Throwable {
        return (PositionAPI) lookup().findVarHandle(MultilineTextField.class, "position", PositionAPI.class).get(f);
    }

    static void insert(MultilineTextField f, String text) throws Throwable {
        lookup().findVirtual(MultilineTextField.class, "insert", MethodType.methodType(void.class, String.class)).invoke(f, text);
    }

    static String longText() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; out.length() < MultilineTextField.MAX_CHARS; i++) out.append(i % 40 == 39 ? "\n\n" : "word" + i + " ");
        return out.substring(0, MultilineTextField.MAX_CHARS - 1);
    }

    private static int cursor(MultilineTextField f) throws Throwable {
        return (int) lookup().findVarHandle(MultilineTextField.class, "cursor", int.class).get(f);
    }

    private static int anchor(MultilineTextField f) throws Throwable {
        return (int) lookup().findVarHandle(MultilineTextField.class, "anchor", int.class).get(f);
    }

    private static String selected(MultilineTextField f) throws Throwable {
        int a = anchor(f), b = cursor(f);
        return f.getText().substring(Math.min(a, b), Math.max(a, b));
    }

    // same font and size the field measures with
    private static float textWidth(String s) throws Exception {
        LazyFont font = LazyFont.loadFont(Fonts.DEFAULT_SMALL);
        return font.calcWidth(s, font.getBaseHeight());
    }

    private static void clear(MultilineTextField f) {
        press(f, Keyboard.KEY_A, true, false);
        press(f, Keyboard.KEY_BACK, false, false);
    }

    private static void mouse(MultilineTextField f, InputEventAPI... events) {
        f.processInput(new ArrayList<>(List.of(events)));
    }

    private static MethodHandles.Lookup lookup() throws IllegalAccessException {
        return MethodHandles.privateLookupIn(MultilineTextField.class, MethodHandles.lookup());
    }

    private static void press(MultilineTextField f, int key, boolean ctrl, boolean shift) {
        press(f, key, (char) 0, ctrl, shift);
    }

    private static void press(MultilineTextField f, int key, char character, boolean ctrl, boolean shift) {
        f.processInput(single(new KeyEvent(key, character, ctrl, shift)));
    }

    private static void type(MultilineTextField f, String text) {
        for (char character : text.toCharArray()) press(f, 0, character, false, false);
    }

    private static List<InputEventAPI> single(InputEventAPI event) {
        List<InputEventAPI> events = new ArrayList<>();
        events.add(event);
        return events;
    }
}
