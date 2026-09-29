// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.client;
import com.google.gson.*;
import com.shouyun.jiahaomode.JiahaoMode;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
/** Only this preference is persisted. Gadget input and chart data never reach this class. */
public final class JiahaoClientConfig {
    public static boolean enableRandomJiahaoMoments=true;
    public static void load() {
        var path=FabricLoader.getInstance().getConfigDir().resolve("jiahao-mode-client.json");
        try {
            if(Files.exists(path)) {
                var json=JsonParser.parseString(Files.readString(path,StandardCharsets.UTF_8)).getAsJsonObject();
                var value=json.get("enableRandomJiahaoMoments");
                if(value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isBoolean())enableRandomJiahaoMoments=value.getAsBoolean();
            } else {
                Files.createDirectories(path.getParent());
                Files.writeString(path,"{\n  \"enableRandomJiahaoMoments\": true\n}\n",StandardCharsets.UTF_8);
            }
        } catch(Exception error){JiahaoMode.LOGGER.warn("Could not read Jiahao client preference; using default",error);}
    }
    private JiahaoClientConfig() {}
}
