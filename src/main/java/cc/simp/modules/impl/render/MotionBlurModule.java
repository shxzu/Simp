package cc.simp.modules.impl.render;

import cc.simp.event.impl.player.MotionEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.property.Property;
import cc.simp.property.impl.DoubleProperty;
import cc.simp.property.impl.EnumProperty;
import cc.simp.utils.client.Timer;
import cc.simp.utils.client.misc.MathUtils;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.*;

import java.util.ArrayList;
import java.util.List;

import static cc.simp.utils.client.Util.mc;

@ModuleInfo(label = "Motion Blur", category = ModuleCategory.RENDER)
public final class MotionBlurModule extends Module {
    public DoubleProperty blurAmountProperty = new DoubleProperty("Blur Amount", 7.0, 10.0, 0.0, 0.1);
}
