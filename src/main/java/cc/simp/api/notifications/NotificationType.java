package cc.simp.api.notifications;


import cc.simp.processes.FontProcess;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.*;

@Getter
@AllArgsConstructor
public enum NotificationType {
    SUCCESS(new Color(20, 250, 90), FontProcess.CHECKMARK),
    DISABLE(new Color(255, 30, 30), FontProcess.XMARK),
    INFO(Color.WHITE, FontProcess.INFO),
    WARNING(Color.YELLOW, FontProcess.WARNING);
    private final Color color;
    private final String icon;
}
