package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.packet.PacketReceiveEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.network.play.server.S02PacketChat;
import org.apache.commons.lang3.RandomUtils;

import java.util.Arrays;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Insults", category = ModuleCategory.CLIENT)
public class InsultsModule extends Module {

    // These are so fucking cursed. :sob: -lumie

    private final String[] deathMessages = {"killed by", "void by", "slain by", "void while escaping", "was killed with magic while fighting",
            "couldn't fly while escaping", "fell to their death while escaping"};

    private final String[] insults = {"\"Cool man the sex man\" Awesome, so when did you lose your virginity? you do realize that's illegal because you aren't over the age of 18, right? Sorry, I'll correct myself. It is, by law, legal to have sexual intercourse once both consenting participants are above the legal age of consent, which in most cases is 16 to 17. Based on your immaturity regarding your name, and commend, and lack of grammar + punctuation, and lack of basic human respect towards others. I seriously doubt that you are 16 or 17, much less 18 or above. So, I would recommend that you do some investigating inside of yourself. Gosh. I can't think of the term... Oh! It's uhm... hm. how about you find God. Because clearly you fucking need it, you pathetic delinquent. I'd be surprised if you've even read this far honestly. What, gonna reply with, and i quote \"i ain't readin allat.\" Cool, go sky dive off of a building with a drop that is fatal. Don't bother bringing a parachute, this world would be a much better place if you dove without one. \"fake smile ahh art\" Shit, you make me sick.",
            "maybe i want u to put ur hands where u want to :3",
            "imagine getting killed by a client thats pasted from rise",
            "bald red man wants u to go get simp @github/x0lumie/Simp",
            "my dog itches so can u like itch it for me",
            "\"best aac bypasses!!1!1\" like bro stfu ur client is so donkey dooks",
            "how wood would alan wood suck if alan wood could suck wood?",
            "ouija board vs 25 woke students. im charlie kirk back from the dead mf",
            "i bet u love receiving backshots from KotlinProject",
            "x0lumie, is the best client dev of all time. get simp @github/x0lumie/Simp",
            "i paste them astolfo scripts like its ur moms first taste of my dih.",
            "rawr xD x3 nuzzles u UwU",
            "womp womp",
            "sniped by ducky $$ get my client @github/x0lumie/Simp",
            "polar pop bypass $$",
            "go back to 2022 skid #famous",
            "it's my b-day. b nice 2 me :<",
            "bombies is my little $1utt",
            "\"i'm not a furry but i do like to be called daddy uwu\"",
            "i knew some1 tht said he would let bombies stack donuts on it.. i agree."};

    @EventLink
    public final Listener<PacketReceiveEvent> PacketReceiveEvent = event -> {
        if (mc.thePlayer == null || !(event.getPacket() instanceof S02PacketChat))
            return;

        S02PacketChat packetChat = (S02PacketChat) event.getPacket();
        String chatComponent = packetChat.getChatComponent().getUnformattedText();

        Arrays.stream(deathMessages).filter(deathMessage -> chatComponent.contains(deathMessage + " " + mc.getSession().getUsername())).forEach(deathMessage -> mc.thePlayer.sendChatMessage(insults[RandomUtils.nextInt(0, insults.length)]));
        if (chatComponent.contains("KILL!"))
            mc.thePlayer.sendChatMessage(insults[RandomUtils.nextInt(0, insults.length)]);
    };
}
