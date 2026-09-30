// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;
import com.google.gson.*;
import com.shouyun.jiahaomode.JiahaoMode;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
/** Client preferences only; gadget input and chart data are never persisted. */
public final class JiahaoClientConfig {
    public static boolean enableRandomJiahaoMoments=true;
    private static double musicVolume=1;
    public static double musicVolume() { return musicVolume; }
    public static void setMusicVolume(double value) {
        musicVolume=Double.isFinite(value)?Math.max(0,Math.min(1,value)):1;
        save();
    }
    public static void load() {
        var path=FabricLoader.getInstance().getConfigDir().resolve("jiahao-mode-client.json");
        try {
            if(Files.exists(path)) {
                var json=JsonParser.parseString(Files.readString(path,StandardCharsets.UTF_8)).getAsJsonObject();
                var value=json.get("enableRandomJiahaoMoments");
                if(value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isBoolean())enableRandomJiahaoMoments=value.getAsBoolean();
                var music=json.get("musicVolume");
                if(music!=null&&music.isJsonPrimitive()&&music.getAsJsonPrimitive().isNumber()) {
                    double volume=music.getAsDouble();
                    musicVolume=Double.isFinite(volume)?Math.max(0,Math.min(1,volume)):1;
                }
            } else {
                save();
            }
        } catch(Exception error){JiahaoMode.LOGGER.warn("Could not read Jiahao client preference; using default",error);}
    }
    private static void save() {
        var path=FabricLoader.getInstance().getConfigDir().resolve("jiahao-mode-client.json");
        try {
            JsonObject json=new JsonObject();
            if(Files.exists(path)) {
                try { json=JsonParser.parseString(Files.readString(path,StandardCharsets.UTF_8)).getAsJsonObject(); }
                catch(RuntimeException ignored) { /* Replace an unreadable preferences file. */ }
            }
            json.addProperty("enableRandomJiahaoMoments",enableRandomJiahaoMoments);
            json.addProperty("musicVolume",musicVolume);
            Files.createDirectories(path.getParent());
            Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(json)+"\n",StandardCharsets.UTF_8);
        } catch(Exception error) { JiahaoMode.LOGGER.warn("Could not save Jiahao client preferences",error); }
    }
    private JiahaoClientConfig() {}
}
