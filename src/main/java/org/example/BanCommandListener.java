package org.example;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.util.concurrent.TimeUnit;
import java.util.Comparator;

public class BanCommandListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (event.getAuthor().isBot() || !event.getMessage().getContentRaw().startsWith("!!ban")) return;

        Guild guild = event.getGuild();
        Member requester = event.getMember();
        String[] args = event.getMessage().getContentRaw().split("\\s+");

        if (args.length < 2) {
            event.getChannel().sendMessage("Usage: `!!ban <user_id>`").queue();
            return;
        }

        if (!hasPermission(requester, guild)) {
            event.getChannel().sendMessage("❌ You lack permission to use this command.").queue();
            return;
        }

        String userId = args[1];
        try {
            Long.parseLong(userId);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Invalid user ID.").queue();
            return;
        }

        int bannedCount = banUserAcrossGuilds(event.getJDA(), userId);
        event.getChannel().sendMessage(String.format("<@%s> banned from **%d** servers.", userId, bannedCount)).queue();
    }

    private boolean hasPermission(Member requester, Guild guild) {
        if (requester == null || guild == null) return false;

        if (requester.hasPermission(Permission.ADMINISTRATOR)) return true;

        Role botHighestRole = guild.getSelfMember().getRoles().stream()
                .max(Comparator.comparingInt(Role::getPosition))
                .orElse(null);
        Role userHighestRole = requester.getRoles().stream()
                .max(Comparator.comparingInt(Role::getPosition))
                .orElse(null);

        return (userHighestRole != null && botHighestRole != null &&
                userHighestRole.getPosition() > botHighestRole.getPosition());
    }

    private int banUserAcrossGuilds(JDA jda, String userId) {
        int bannedCount = 0;
        for (Guild guild : jda.getGuilds()) {
            if (guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                try {
                    guild.ban(UserSnowflake.fromId(userId), 0, TimeUnit.SECONDS)
                            .reason("Banned via bot command")
                            .complete();
                    bannedCount++;
                } catch (Exception e) {
                    // Log failure
                }
            }
        }
        return bannedCount;
    }
}