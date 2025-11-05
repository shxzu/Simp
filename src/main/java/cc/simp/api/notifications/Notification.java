package cc.simp.api.notifications;

import cc.simp.Simp;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.modules.impl.visuals.NotificationsModule;
import cc.simp.modules.impl.visuals.PostProcessingModule;
import cc.simp.processes.FontProcess;
import cc.simp.utils.Util;
import cc.simp.utils.client.Timer;
import cc.simp.utils.render.RenderUtils;
import cc.simp.utils.render.animations.Animation;
import cc.simp.utils.render.animations.impl.DecelerateAnimation;
import lombok.Getter;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;

@Getter
public class Notification extends Util {

    private final NotificationType notificationType;
    private final String title, description;
    private final float time;
    private final Timer timerUtil;
    private final Animation animation;

    public Notification(NotificationType type, String title, String description) {
        this(type, title, description, NotificationManager.getToggleTime());
    }

    public Notification(NotificationType type, String title, String description, float time) {
        this.title = title;
        this.description = description;
        this.time = (long) (time * 1000);
        timerUtil = new Timer();
        this.notificationType = type;
        animation = new DecelerateAnimation(250, 1);
    }

    public void draw(float x, float y, float width, float height) {
        CustomFontRenderer fr = NotificationsModule.customFont.getValue() ? FontProcess.getCurrentFont() : FontProcess.getFont("mc");
        boolean lowerAlpha = Simp.INSTANCE.getModuleManager().getModule(PostProcessingModule.class).isEnabled();
        RenderUtils.drawRect(x, y, width, height, new Color(0.1F, 0.1F, 0.1F, lowerAlpha ? 0.4F : .75f));
        float percentage = Math.min((timerUtil.getTime() / getTime()), 1);
        RenderUtils.drawRect(x + (width * percentage), y + height - 1, width - (width * percentage), 1, getNotificationType().getColor());
        FontProcess.getFont("icon").drawString(getNotificationType().getIcon(), x + 3, (y + FontProcess.getFont("icon").getMiddleOfBox(height) + 1), getNotificationType().getColor().getRGB());

        fr.drawString(getTitle(), x + 7 + FontProcess.getFont("icon").getStringWidth(getNotificationType().getIcon()), y + 4, Color.WHITE.getRGB());
        fr.drawString(getDescription(), x + 7 + FontProcess.getFont("icon").getStringWidth(getNotificationType().getIcon()), y + 8.5f + FontProcess.getFont("mc").getHeight(), Color.WHITE.getRGB());
    }

}
