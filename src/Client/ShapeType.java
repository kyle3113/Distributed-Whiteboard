package Client;

public enum ShapeType {
    LINE, CIRCLE, RECT, OVAL, FREEHAND, ERASER, TEXT,
    FILL_CIRCLE, FILL_RECT, FILL_OVAL;

    public static ShapeType fromString(String string) {
        for (ShapeType shape : ShapeType.values()) {
            if (shape.toString().equals(string)) {
                return shape;
            }
        }
        return null;
    }

}
