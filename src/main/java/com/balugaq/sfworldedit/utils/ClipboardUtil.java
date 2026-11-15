package com.balugaq.sfworldedit.utils;

import com.balugaq.sfworldedit.implementation.SFWorldedit;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

@UtilityClass
public class ClipboardUtil {
    public static void send(@Nonnull Player player, String text) {
        send(player, SFWorldedit.getInstance().getString("messages.clipboard.title"), text);
    }

    public static void send(@Nonnull Player player, String display, String text) {
        send(player, display, SFWorldedit.getInstance().getString("messages.clipboard.hover"), text);
    }

    public static void send(@Nonnull Player player, String display, String hover, String text) {
        TextComponent msg = new TextComponent(display);
        msg.setUnderlined(true);
        msg.setItalic(true);
        msg.setColor(ChatColor.LIGHT_PURPLE);
        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text));
        player.spigot().sendMessage(msg);
    }
}
