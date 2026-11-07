package livecheck;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;

// a mouse event for MultilineTextField.processInput. posted window messages cannot drag or hold shift
public final class MouseEvent implements InputEventAPI {
    private final int x;
    private final int y;
    private final boolean down;
    private final boolean up;
    private final boolean shift;
    private final boolean doubleClick;
    private boolean consumed = false;

    private MouseEvent(float x, float y, boolean down, boolean up, boolean shift, boolean doubleClick) {
        this.x = Math.round(x);
        this.y = Math.round(y);
        this.down = down;
        this.up = up;
        this.shift = shift;
        this.doubleClick = doubleClick;
    }

    public static MouseEvent down(float x, float y, boolean shift, boolean doubleClick) {
        return new MouseEvent(x, y, true, false, shift, doubleClick);
    }

    public static MouseEvent up(float x, float y) {
        return new MouseEvent(x, y, false, true, false, false);
    }

    public static MouseEvent move(float x, float y) {
        return new MouseEvent(x, y, false, false, false, false);
    }

    @Override public int getX() { return x; }
    @Override public int getY() { return y; }
    @Override public boolean isLMBDownEvent() { return down; }
    @Override public boolean isLMBUpEvent() { return up; }
    @Override public boolean isMouseMoveEvent() { return !down && !up; }
    @Override public boolean isShiftDown() { return shift; }
    @Override public boolean isDoubleClick() { return doubleClick; }
    @Override public boolean isConsumed() { return consumed; }
    @Override public void consume() { consumed = true; }
    @Override public boolean isMouseEvent() { return true; }
    @Override public boolean isMouseDownEvent() { return down; }
    @Override public boolean isMouseUpEvent() { return up; }
    @Override public boolean isLMBEvent() { return down || up; }

    @Override public int getEventValue() { return 0; }
    @Override public int getDX() { return 0; }
    @Override public int getDY() { return 0; }
    @Override public InputEventClass getEventClass() { return null; }
    @Override public void logEvent() { }
    @Override public boolean isRepeat() { return false; }
    @Override public InputEventType getEventType() { return null; }
    @Override public boolean isKeyboardEvent() { return false; }
    @Override public boolean isKeyUpEvent() { return false; }
    @Override public boolean isKeyDownEvent() { return false; }
    @Override public boolean isRMBEvent() { return false; }
    @Override public boolean isRMBDownEvent() { return false; }
    @Override public boolean isRMBUpEvent() { return false; }
    @Override public boolean isMouseScrollEvent() { return false; }
    @Override public char getEventChar() { return 0; }
    @Override public boolean isAltDown() { return false; }
    @Override public boolean isCtrlDown() { return false; }
    @Override public boolean isUnmodified() { return !shift; }
    @Override public boolean isModifierKey() { return false; }
    @Override public boolean isControlDownEvent(String controlEnumName) { return false; }
    @Override public boolean isControlUpEvent(String controlEnumName) { return false; }
    @Override public boolean isControlActivated(String enumName) { return false; }
}
