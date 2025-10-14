package cc.simp.processes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.*;
import java.util.List;

public class DraggingProcess {

    public static final Map<String, DraggableComponent> components = new HashMap<>();
    private static String draggingComponent = null;
    private static double dragStartX, dragStartY;
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static class DraggableComponent {
        private double x, y, width, height;

        public DraggableComponent(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        public double getWidth() { return width; }
        public void setWidth(double width) { this.width = width; }
        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }

    }

    public static void update() {
        if (mc.currentScreen instanceof GuiChat) {
            handleDragging();
        } else {
            draggingComponent = null;
        }
    }

    private static void handleDragging() {
        ScaledResolution sr = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;
        boolean isLeftMouseDown = Mouse.isButtonDown(0);

        for (Map.Entry<String, DraggableComponent> entry : components.entrySet()) {
            DraggableComponent component = entry.getValue();
            if (component.getWidth() <= 1 && component.getHeight() <= 1)
                continue;

            double x = component.getX();
            double y = component.getY();
            double width = component.getWidth();
            double height = component.getHeight();

            Gui.drawRect((int) x - 2, (int) y - 2, (int) (x + width) + 2, (int) (y + height) + 2,
                    new Color(120, 120, 120, 70).getRGB());
        }

        // Handle dragging logic
        if (draggingComponent != null) {
            if (isLeftMouseDown) {
                DraggableComponent component = components.get(draggingComponent);
                component.setX(mouseX - dragStartX);
                component.setY(mouseY - dragStartY);
            } else {
                draggingComponent = null;
            }
        } else if (isLeftMouseDown) {
            List<Map.Entry<String, DraggableComponent>> reversed = new ArrayList<>(components.entrySet());
            Collections.reverse(reversed);
            for (Map.Entry<String, DraggableComponent> entry : reversed) {
                DraggableComponent component = entry.getValue();
                if (component.getWidth() <= 1 && component.getHeight() <= 1)
                    continue;

                double x = component.getX();
                double y = component.getY();
                double width = component.getWidth();
                double height = component.getHeight();

                if (entry.getKey().equals("Arraylist")) {
                    x = component.getX() - component.getWidth();
                }

                if (mouseX >= x - 2 && mouseX <= x + width + 2 && mouseY >= y - 2 && mouseY <= y + height + 2) {
                    draggingComponent = entry.getKey();
                    dragStartX = mouseX - component.getX();
                    dragStartY = mouseY - component.getY();
                    break;
                }
            }
        }
    }
}
