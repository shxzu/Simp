package cc.simp.modules.impl.visuals;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.properties.Property;
import cc.simp.api.properties.impl.ModeProperty;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

@ModuleInfo(label = "Scoreboard", category = ModuleCategory.VISUALS)
public class ScoreboardModule extends Module {

    public static ModeProperty<Mode> scoreboardStyle = new ModeProperty<>("Scoreboard Style", Mode.Left);
    public static Property<Boolean> mcFont = new Property<>("Minecraft Font", false);

    public enum Mode {
        Vanilla("Vanilla"), VanillaOffset("Vanilla Offset"), Left("Left"), LeftOffset("Left Offset");

        public String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

    }

    private boolean renderedThisFrame = false;

    @EventLink
    public Listener<Render2DEvent> render2DEventListener = e -> renderedThisFrame = false;

    @EventLink
    public Listener<ShaderEvent> shaderEventListener = e -> {
        // Render the Simp scoreboard during shader passes so it gets included in postprocessing.
        if (!this.isEnabled()) return;
        if (renderedThisFrame) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective scoreobjective = null;
        net.minecraft.scoreboard.ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(mc.thePlayer.getName());

        if (scoreplayerteam != null) {
            int i1 = scoreplayerteam.getChatFormat().getColorIndex();

            if (i1 >= 0) {
                scoreobjective = scoreboard.getObjectiveInDisplaySlot(3 + i1);
            }
        }

        ScoreObjective scoreobjective1 = scoreobjective != null ? scoreobjective : scoreboard.getObjectiveInDisplaySlot(1);

        if (scoreobjective1 != null) {
            mc.ingameGUI.renderSimpScoreboard(scoreobjective1, new ScaledResolution(mc));
            renderedThisFrame = true;
        }
    };
}
