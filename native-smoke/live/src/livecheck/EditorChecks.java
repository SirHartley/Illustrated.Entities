package livecheck;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import illustratedEntities.dialogue.panel.MultilineTextField;
import illustratedEntities.helper.TextHandler;
import illustratedEntities.memory.TextDataEntry;
import org.lwjgl.input.Keyboard;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// keys go straight to processInput, ClickChecks does real mouse and keyboard. the clipboard stays out, a run must not
// touch what the user copied, so paste is checked through insert and not through the Ctrl+V read
public final class EditorChecks {
    private static final int FRAMES = 300;
    private static final int TYPED = 200;
    private static final int RANDOM_KEYS = 4000;
    private static final int[] RANDOM_POOL = {Keyboard.KEY_LEFT, Keyboard.KEY_RIGHT, Keyboard.KEY_UP, Keyboard.KEY_DOWN, Keyboard.KEY_HOME, Keyboard.KEY_END,
            Keyboard.KEY_PRIOR, Keyboard.KEY_NEXT, Keyboard.KEY_BACK, Keyboard.KEY_DELETE, Keyboard.KEY_RETURN, 0, 0, 0, 0};

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
        steps.add(EditorChecks::randomKeys);
        steps.add(EditorChecks::typingCost);
        steps.add(c -> Perf.sample(c, FRAMES));
        steps.add(c -> {
            Perf.report("editor 5000 chars");
            Perf.dump("editor 5000 chars");
            c.shot("editor-long");
        });
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
            Ui.press(c, "delete");
        });
        steps.add(c -> Ui.press(c, "delete"));
        steps.add(c -> {
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
        press(f, Keyboard.KEY_BACK, false, true);
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
        c.expect(f.getText().isEmpty(), "shift backspace clears the box");
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
        press(second, Keyboard.KEY_BACK, false, true);
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
        press(f, Keyboard.KEY_BACK, false, true);
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

    // seeded, a failure repeats. each press has to leave cursor and scroll in range
    private static void randomKeys(Ctx c) throws Throwable {
        MultilineTextField f = field(0);
        Random random = new Random(1);
        int bad = 0;
        for (int i = 0; i < RANDOM_KEYS; i++) {
            int key = RANDOM_POOL[random.nextInt(RANDOM_POOL.length)];
            char character = key == 0 ? (char) ('a' + random.nextInt(26)) : 0;
            if (key == 0 && random.nextInt(5) == 0) character = ' ';
            press(f, key, character, random.nextInt(4) == 0, false);
            int length = f.getText().length();
            if (cursor(f) < 0 || cursor(f) > length || length > MultilineTextField.MAX_CHARS || scroll(f) < 0f) bad++;
        }
        c.expect(bad == 0, RANDOM_KEYS + " random keys keep the cursor and scroll in range (" + bad + " bad)");
    }

    private static void typingCost(Ctx c) throws Throwable {
        MultilineTextField f = field(0), other = field(1);
        focus(other);
        insert(other, longText());
        focus(f);
        press(f, Keyboard.KEY_BACK, false, true);
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

    private static void apply(Ctx c) throws Throwable {
        MultilineTextField first = field(0), second = field(1);
        focus(first);
        press(first, Keyboard.KEY_BACK, false, true);
        type(first, "apply text");
        focus(second);
        press(second, Keyboard.KEY_BACK, false, true);
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
