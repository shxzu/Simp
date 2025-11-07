package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.font.CustomFont;
import cc.simp.api.font.CustomFontRenderer;
import cc.simp.api.notifications.Notification;
import cc.simp.api.notifications.NotificationManager;
import cc.simp.api.properties.Property;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.DraggingProcess;
import cc.simp.processes.FontProcess;
import cc.simp.utils.render.animations.Animation;
import cc.simp.utils.render.animations.Direction;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Notifications", category = ModuleCategory.VISUALS)
public final class NotificationsModule extends Module {

    public static Property<Boolean> customFont  = new Property<>("Custom Font", false);

    private static boolean positionInitialized = false;
    private static final int NOTIFICATION_HEIGHT = 30;
    private static final int NOTIFICATION_SPACING = 5;
    private static final int MIN_NOTIFICATION_WIDTH = 150;

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> {
        renderNotifications();
    };

    @EventLink
    public Listener<ShaderEvent> shaderEventListener = e -> {
        renderNotifications();
    };

    private void initializePosition(ScaledResolution sr) {
        if (!positionInitialized && !DraggingProcess.components.containsKey("Notifications")) {
            DraggingProcess.components.put("Notifications",
                    new DraggingProcess.DraggableComponent(
                            sr.getScaledWidth() - MIN_NOTIFICATION_WIDTH - 10,
                            10
                    )
            );
            positionInitialized = true;
        }
    }

    private void renderNotifications() {
        ScaledResolution sr = new ScaledResolution(mc);
        CustomFontRenderer fr =  customFont.getValue() ? FontProcess.getCurrentFont() : FontProcess.getFont("mc");

        NotificationManager.setToggleTime(2f);

        float yOffset = 0;
        int maxWidth = MIN_NOTIFICATION_WIDTH;

        for (Notification notification : NotificationManager.getNotifications()) {
            int titleWidth = fr.getStringWidth(notification.getTitle());
            int descWidth = fr.getStringWidth(notification.getDescription());
            int iconWidth = FontProcess.getFont("icon").getStringWidth(notification.getNotificationType().getIcon());
            int notificationWidth = Math.max(titleWidth, descWidth) + iconWidth + 15;
            maxWidth = Math.max(maxWidth, notificationWidth);
        }

        for (Notification notification : NotificationManager.getNotifications()) {
            Animation animation = notification.getAnimation();
            animation.setDirection(notification.getTimerUtil().hasTimeElapsed((long) notification.getTime())
                    ? Direction.BACKWARDS
                    : Direction.FORWARDS);

            if (animation.finished(Direction.BACKWARDS)) {
                NotificationManager.getNotifications().remove(notification);
                continue;
            }

            animation.setDuration(250);

            int titleWidth = fr.getStringWidth(notification.getTitle());
            int descWidth = fr.getStringWidth(notification.getDescription());
            int iconWidth = FontProcess.getFont("icon").getStringWidth(notification.getNotificationType().getIcon());
            int notificationWidth = Math.max(titleWidth, descWidth) + iconWidth + 15;

            float slideOffset = (maxWidth - notificationWidth) * (1 - animation.getOutput().floatValue());
            float x = (float) (sr.getScaledWidth() - notificationWidth - 2 + slideOffset);
            float y = (float) (sr.getScaledHeight() - yOffset - NOTIFICATION_HEIGHT - 2);

            notification.draw(x, y, notificationWidth, NOTIFICATION_HEIGHT);
            yOffset += (NOTIFICATION_HEIGHT + NOTIFICATION_SPACING) * animation.getOutput().floatValue();
        }
    }
}
