package cc.simp.ui.click.astolfo.buttons;

public abstract class AstolfoButton {
    public float x, y, width, height;

    public AstolfoButton(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void drawPanel(int mouseX, int mouseY);

    public abstract void mouseAction(int mouseX, int mouseY, boolean click, int button);

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY > y && mouseY < y + height;
    }
}
