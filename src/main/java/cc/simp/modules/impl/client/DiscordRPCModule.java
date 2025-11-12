package cc.simp.modules.impl.client;

import cc.simp.Simp;
import cc.simp.api.events.impl.world.TickEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;

@ModuleInfo(label = "Discord RPC", category = ModuleCategory.CLIENT)
public class DiscordRPCModule extends Module {
    private static final String APPLICATION_ID = "1140815918478409770";

    public DiscordRPCModule() {
        super();
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
                .setReadyEventHandler(user -> System.out.println("[Discord RPC] Logged in as: " + user.username + " " + user.userId))
                .build();

        DiscordRPC.discordInitialize(APPLICATION_ID, handlers, true);

        Runtime.getRuntime().addShutdownHook(new Thread(DiscordRPC::discordShutdown));
    }

    @Override
    public void onDisable() {
        DiscordRPC.discordClearPresence();
    }

    @EventLink
    public final Listener<TickEvent> onTick = event -> {
        DiscordRichPresence presence = new DiscordRichPresence
                .Builder("making kids simp since 2025..")
                .setStartTimestamps(Simp.getStartTime())
                .setDetails("Simp [ " + Simp.VERSION + " ]")
                .setBigImage("icon", "https://github.com/shxzu/Simp")
                .build();

        DiscordRPC.discordUpdatePresence(presence);
    };

}
