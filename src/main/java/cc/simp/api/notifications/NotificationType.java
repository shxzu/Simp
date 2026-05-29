package cc.simp.api.notifications;


import cc.simp.utils.render.FontUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.*;

@Getter
@AllArgsConstructor
public enum NotificationType {
    SUCCESS(new Color(20, 250, 90), FontUtils.CHECKMARK),
    DISABLE(new Color(255, 30, 30), FontUtils.XMARK),
    INFO(Color.WHITE, FontUtils.INFO),
    WARNING(Color.YELLOW, FontUtils.WARNING);
    private final Color color;
    private final String icon;
}
