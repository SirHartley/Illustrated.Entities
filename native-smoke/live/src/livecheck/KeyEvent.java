package livecheck;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;

// a key press for MultilineTextField.processInput, posted window messages cannot hold ctrl or shift
public final class KeyEvent implements InputEventAPI {
    private final int key;
    private final char character;
    private final boolean ctrl;
    private final boolean shift;
    private boolean consumed = false;

    public KeyEvent(int key, char character, boolean ctrl, boolean shift) {
        this.key = key;
        this.character = character;
        this.ctrl = ctrl;
        this.shift = shift;
    }

    @Override public int getEventValue() { return key; }
    @Override public char getEventChar() { return character; }
    @Override public boolean isCtrlDown() { return ctrl; }
    @Override public boolean isShiftDown() { return shift; }
    @Override public boolean isKeyboardEvent() { return true; }
    @Override public boolean isKeyDownEvent() { return true; }
    @Override public boolean isConsumed() { return consumed; }
    @Override public void consume() { consumed = true; }

    @Override public int getX() { return 0; }
    @Override public int getY() { return 0; }
    @Override public int getDX() { return 0; }
    @Override public int getDY() { return 0; }
    @Override public InputEventClass getEventClass() { return null; }
    @Override public void logEvent() { }
    @Override public boolean isRepeat() { return false; }
    @Override public InputEventType getEventType() { return null; }
    @Override public boolean isMouseEvent() { return false; }
    @Override public boolean isKeyUpEvent() { return false; }
    @Override public boolean isMouseUpEvent() { return false; }
    @Override public boolean isMouseDownEvent() { return false; }
    @Override public boolean isLMBDownEvent() { return false; }
    @Override public boolean isLMBEvent() { return false; }
    @Override public boolean isRMBEvent() { return false; }
    @Override public boolean isLMBUpEvent() { return false; }
    @Override public boolean isRMBDownEvent() { return false; }
    @Override public boolean isRMBUpEvent() { return false; }
    @Override public boolean isMouseMoveEvent() { return false; }
    @Override public boolean isMouseScrollEvent() { return false; }
    @Override public boolean isAltDown() { return false; }
    @Override public boolean isUnmodified() { return !ctrl && !shift; }
    @Override public boolean isDoubleClick() { return false; }
    @Override public boolean isModifierKey() { return false; }
    @Override public boolean isControlDownEvent(String controlEnumName) { return false; }
    @Override public boolean isControlUpEvent(String controlEnumName) { return false; }
    @Override public boolean isControlActivated(String enumName) { return false; }
}
