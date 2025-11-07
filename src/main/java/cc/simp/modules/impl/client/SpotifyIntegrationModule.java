package cc.simp.modules.impl.client;

import cc.simp.api.events.impl.render.Render2DEvent;
import cc.simp.api.events.impl.render.ShaderEvent;
import cc.simp.api.events.impl.world.TickEvent;
import cc.simp.modules.Module;
import cc.simp.modules.ModuleCategory;
import cc.simp.modules.ModuleInfo;
import cc.simp.processes.ColorProcess;
import cc.simp.processes.DraggingProcess;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.util.Scanner;

import static cc.simp.utils.Util.mc;

@ModuleInfo(label = "Spotify", description = "Displays Spotify playback information", category = ModuleCategory.CLIENT)
public class SpotifyIntegrationModule extends Module {

    public String song = "Loading...";
    public String artist = "Loading...";
    public String auth_token = "";
    public String client_id = "";
    public String client_secret = "";
    public String progress = "1:43";
    public String length = "2:56";
    public int duration_ms = 176000;
    public int progress_ms = 103000;
    public int ticks = 0;

    public static boolean updatedInfo = false;
    private static boolean positionInitialized = false;
    private static final int DEFAULT_WIDTH = 180;
    private static final int DEFAULT_HEIGHT = 65;

    @EventLink
    public final Listener<Render2DEvent> onRender2D = event -> {
        renderSpotify();
    };

    @EventLink
    public final Listener<ShaderEvent> onShader = event -> {
        renderSpotify();
    };

    private void initializePosition(ScaledResolution sr) {
        if (!positionInitialized && !DraggingProcess.components.containsKey("Spotify")) {
            DraggingProcess.components.put("Spotify",
                    new DraggingProcess.DraggableComponent(
                            200,
                            200
                    )
            );
            positionInitialized = true;
        }
    }

    private void renderSpotify() {
        ScaledResolution sr = new ScaledResolution(mc);
        initializePosition(sr);

        DraggingProcess.DraggableComponent draggableComponent = DraggingProcess.components.get("Spotify");
        draggableComponent.setWidth(DEFAULT_WIDTH);
        draggableComponent.setHeight(DEFAULT_HEIGHT);

        float x = (float) draggableComponent.getX();
        float y = (float) draggableComponent.getY();

        // Format time strings
        progress = (progress_ms / 1000 / 60) + ":";
        if ((progress_ms / 1000 % 60) < 10) {
            progress += "0" + (int)(progress_ms / 1000 % 60);
        } else {
            progress += (int)(progress_ms / 1000 % 60);
        }

        length = (duration_ms / 1000 / 60) + ":";
        if ((duration_ms / 1000 % 60) < 10) {
            length += "0" + (duration_ms / 1000 % 60);
        } else {
            length += (int)(duration_ms / 1000 % 60);
        }

        Color lightest = new Color(44, 44, 44, 20);
        Color accentColor = ColorProcess.getColor();

        // Draw inner border
        Gui.drawRect((int) (x + 2.5), (int) (y + 2.5), (int) (x + DEFAULT_WIDTH - 2.5), (int) (y + DEFAULT_HEIGHT - 2.5), lightest.getRGB());

        // Draw background
        Gui.drawRect((int) (x + 3), (int) (y + 3.5), (int) (x + DEFAULT_WIDTH - 3), (int) (y + DEFAULT_HEIGHT - 3.5), new Color(15, 20, 35, 200).getRGB());

        // Draw top accent bar
        Gui.drawRect((int) (x + 3.5), (int) (y + 3.5), (int) (x + DEFAULT_WIDTH - 3.5), (int) (y + 4.5), accentColor.getRGB());
        Gui.drawRect((int) (x + 3.5), (int) (y + 4), (int) (x + DEFAULT_WIDTH - 3.5), (int) (y + 4.5), new Color(0, 0, 0, 110).getRGB());

        // Song and artist name
        mc.fontRendererObj.drawString(song, (int)(x + 10), (int)(y + 15), Color.WHITE.getRGB());
        mc.fontRendererObj.drawString(artist, (int)(x + 10), (int)(y + 28), new Color(255, 255, 255, 128).getRGB());

        // Progress background
        Gui.drawRect((int)(x + 10), (int)(y + 45), (int)(x + DEFAULT_WIDTH - 10), (int)(y + 51), new Color(50, 50, 50, 150).getRGB());

        // Progress bar
        int progressWidth = (int)((progress_ms * (DEFAULT_WIDTH - 20.0)) / duration_ms);
        Gui.drawRect((int)(x + 10), (int)(y + 45), (int)(x + 10 + progressWidth), (int)(y + 51), accentColor.getRGB());

        // Time and duration
        String timeText = progress + " / " + length;
        mc.fontRendererObj.drawString(timeText,
                (int)(x + DEFAULT_WIDTH - 10 - mc.fontRendererObj.getStringWidth(timeText)),
                (int)(y + 55), new Color(255, 255, 255, 128).getRGB());
    }

    public void auth() {
        new Thread(() -> {
            try {
                URL url = new URL("https://accounts.spotify.com/api/token");
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setRequestMethod("POST");

                httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpConn.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
                writer.write("grant_type=client_credentials&client_id=" + client_id + "&client_secret=" + client_secret);
                writer.flush();
                writer.close();
                httpConn.getOutputStream().close();

                InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                        ? httpConn.getInputStream()
                        : httpConn.getErrorStream();
                Scanner s = new Scanner(responseStream).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                JSONObject obj = new JSONObject(response);
                auth_token = obj.getString("access_token");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void getSpotifyInfo() {
        try {
            if (!updatedInfo) {
                updatedInfo = true;
                URL url = new URL("https://api.spotify.com/v1/me/player?market=US");
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setRequestMethod("GET");

                httpConn.setRequestProperty("authorization", "Bearer " + auth_token);

                InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                        ? httpConn.getInputStream()
                        : httpConn.getErrorStream();
                Scanner s = new Scanner(responseStream).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                JSONObject obj = new JSONObject(response);

                progress_ms = obj.getInt("progress_ms");
                JSONObject item = obj.getJSONObject("item");
                duration_ms = item.getInt("duration_ms");

                song = Normalizer.normalize(item.getString("name"), Normalizer.Form.NFKD).replaceAll("[^\\p{ASCII}]", "");
                if (song.length() > 20) {
                    song = song.substring(0, 20);
                }

                JSONArray artists = item.getJSONArray("artists");
                JSONObject author = new JSONObject(artists.get(0).toString());
                artist = Normalizer.normalize(author.getString("name"), Normalizer.Form.NFKD).replaceAll("[^\\p{ASCII}]", "");

                updatedInfo = false;
            }
        } catch (Exception e) {
            if (!client_id.isEmpty() && !client_secret.isEmpty()) {
                auth();
            }
            updatedInfo = false;
        }
    }

    @EventLink
    public final Listener<TickEvent> onTick = event -> {
        new Thread(() -> {
            if (ticks % (20 * 60) == 0 && this.isEnabled()) {
                updatedInfo = false;
                getSpotifyInfo();
            }

            if (ticks % 20 == 0 && this.isEnabled()) {
                if ((progress_ms + 1000) > duration_ms) {
                    getSpotifyInfo();
                } else {
                    progress_ms += 1000;
                }
            }
            ticks++;
        }).start();
    };
}
