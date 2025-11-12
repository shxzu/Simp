package cc.simp.api.commands.impl;

import cc.simp.Simp;
import cc.simp.api.commands.Command;
import cc.simp.api.config.Config;
import cc.simp.modules.impl.client.SpotifyIntegrationModule;
import cc.simp.utils.client.Logger;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class SpotifyCommand extends Command {

    public SpotifyCommand() {
        super("spotify", "Configures the Spotify Integration.", ".spotify auth [client-id] [client-secret] / reload", "s");
    }

    public String auth(String client_id_, String client_secret_) {
        try {
            URL url = new URL("https://accounts.spotify.com/api/token");
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");

            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
            writer.write("grant_type=client_credentials&client_id=" + client_id_ + "&client_secret=" + client_secret_);
            writer.flush();
            writer.close();
            httpConn.getOutputStream().close();

            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            String response = s.hasNext() ? s.next() : "";
            JSONObject obj = new JSONObject(response);
            return obj.getString("access_token");
        } catch (Exception e) {
            Logger.chatPrint("Spotify auth error");
        }
        return "error";
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            usage();
            return;
        }

        String command = Simp.requireNonNull(args[0]);

        if (command.equalsIgnoreCase("auth")) {
            if (args.length != 3) {
                usage();
                return;
            }
            String authtoken = auth(args[1], args[2]);
            if (authtoken != "error") {
                Logger.chatPrint("Logged in");
                Simp.INSTANCE.getModuleManager().getModule(SpotifyIntegrationModule.class).auth_token = authtoken;
                Simp.INSTANCE.getModuleManager().getModule(SpotifyIntegrationModule.class).client_id = args[1];
                Simp.INSTANCE.getModuleManager().getModule(SpotifyIntegrationModule.class).client_secret = args[2];
            }
        } else if (command.equalsIgnoreCase("reload")) {
            if (args.length != 1) {
                usage();
                return;
            }
            Simp.INSTANCE.getModuleManager().getModule(SpotifyIntegrationModule.class).getSpotifyInfo();
        } else {
            usage();
        }
    }
}