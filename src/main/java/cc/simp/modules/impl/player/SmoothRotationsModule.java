package cc.simp.modules.impl.player;

import cc.simp.event.impl.player.MoveEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.Representation;
import cc.simp.utils.client.mc.MovementUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Smooth Rotations", category = ModuleCategory.PLAYER)
public final class SmoothRotationsModule extends Module {

    public static DoubleProperty rotSpeed = new DoubleProperty("Rotation Speed", 10, 1, 15, 1, Representation.INT);

}
