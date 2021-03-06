package me.badstagram.vortex.commands.info;

import me.badstagram.vortex.commandhandler.Category;
import me.badstagram.vortex.commandhandler.Command;
import me.badstagram.vortex.commandhandler.context.impl.CommandContext;
import me.badstagram.vortex.exceptions.BadArgumentException;
import me.badstagram.vortex.exceptions.CommandExecutionException;
import me.badstagram.vortex.util.EmbedUtil;
import me.badstagram.vortex.util.FormatUtil;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class Server extends Command {
    public Server() {
        this.name = "server";
        this.help = "Get info about the server";
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.category = new Category("Info");


    }

    @Override
    public void execute(CommandContext ctx) throws CommandExecutionException, BadArgumentException {
        try {
            var guild = ctx.getGuild();
            var members = guild.getMembers();

            var onlineMembers = members.stream()
                    .filter(m -> m.getOnlineStatus() == OnlineStatus.ONLINE)
                    .count();

            var idleMembers = members.stream()
                    .filter(m -> m.getOnlineStatus() == OnlineStatus.IDLE)
                    .count();

            var dndMembers = members.stream()
                    .filter(m -> m.getOnlineStatus() == OnlineStatus.DO_NOT_DISTURB)
                    .count();

            var offlineMembers = members.stream()
                    .filter(m -> m.getOnlineStatus() == OnlineStatus.OFFLINE)
                    .count();

            var bots = members.stream()
                    .map(Member::getUser)
                    .filter(User::isBot)
                    .count();

            var humans = members.stream()
                    .map(Member::getUser)
                    .filter(u -> !u.isBot())
                    .count();

            var embed = EmbedUtil.createDefault()
                    .setTitle(guild.getName())
                    .addField("ID", guild.getId(), true)
                    .addField("Members", """
                            <:online:774743921560649810> %d
                            <:status_idle:774743953303535636> %d
                            <:do_not_disturb:774743995238318112> %d
                            <:status_offline:774744016699785247> %d
                                                        
                            <:bot:816279960750784552> %d
                            \uD83D\uDC71 %d
                                                        
                                                        
                                                        
                            """.formatted(onlineMembers, idleMembers, dndMembers, offlineMembers, bots, humans), true)
                    .addField("Features", FormatUtil.parseGuildFeatures(guild), false)
                    .addField("Assets", this.getAssets(guild), false)
                    .build();

            ctx.getChannel()
                    .sendMessage(embed)
                    .queue();

        } catch (Exception e) {
            throw new CommandExecutionException(e);
        }
    }

    protected String getAssets(Guild guild) {
        var icon = guild.getIconUrl();
        var banner = guild.getBannerUrl();
        var splash = guild.getSplashUrl();

        var assets = "";

        if (icon != null)
            assets += "[`Icon`](%s) ".formatted(icon);

        if (banner != null)
            assets += "[Banner](%s) ".formatted(banner);

        if (splash != null)
            assets += "[Splash](%s) ".formatted(splash);


        return assets;
    }

}

