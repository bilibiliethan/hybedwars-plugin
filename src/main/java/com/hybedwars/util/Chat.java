package com.hybedwars.util;

import com.hybedwars.config.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/** 文本工具：颜色代码转换、前缀消息、组件转换。 */
public final class Chat {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Chat() {
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String strip(String text) {
        return ChatColor.stripColor(color(text));
    }

    public static Component comp(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    /** 把 Component 转回旧版字符串（用于计分板等旧接口）。 */
    public static String legacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    /** 去掉文本中的颜色代码并生成唯一尾缀，用于计分板行。 */
    public static List<String> colorize(List<String> lines) {
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(color(line));
        }
        return out;
    }

    public static void raw(CommandSender to, String text) {
        if (text == null) {
            return;
        }
        for (String line : text.split("\n", -1)) {
            to.sendMessage(color(line));
        }
    }

    public static void raw(CommandSender to, List<String> lines) {
        for (String line : lines) {
            to.sendMessage(color(line));
        }
    }

    /** 发送带前缀的配置化消息。 */
    public static void send(CommandSender to, String key, Object... replacements) {
        String message = Messages.get(key, replacements);
        if (message == null || message.isEmpty()) {
            return;
        }
        to.sendMessage(color(Messages.prefix() + message));
    }

    /** 发送不带前缀的配置化消息。 */
    public static void sendRaw(CommandSender to, String key, Object... replacements) {
        String message = Messages.get(key, replacements);
        if (message == null || message.isEmpty()) {
            return;
        }
        to.sendMessage(color(message));
    }
}
