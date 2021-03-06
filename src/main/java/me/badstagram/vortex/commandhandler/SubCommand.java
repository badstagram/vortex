package me.badstagram.vortex.commandhandler;

import me.badstagram.vortex.commandhandler.context.impl.CommandContext;
import me.badstagram.vortex.commandhandler.context.impl.SubCommandContext;
import me.badstagram.vortex.core.Constants;
import me.badstagram.vortex.exceptions.BadArgumentException;
import me.badstagram.vortex.exceptions.CommandExecutionException;
import me.badstagram.vortex.managers.CooldownManager;
import me.badstagram.vortex.util.EmbedUtil;
import me.badstagram.vortex.util.ErrorHandler;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class SubCommand extends Command {
    protected String name = "null";
    protected String help = "null";
    protected String usage = name;
    protected boolean owner = false;
    protected boolean supportServerOnly = false;
    protected boolean nsfw = false;
    protected long cooldown = 0;
    protected String[] aliases = new String[0];
    protected Permission[] botPermissions = Permission.EMPTY_PERMISSIONS;
    protected Permission[] userPermissions = Permission.EMPTY_PERMISSIONS;

    public abstract void execute(SubCommandContext ctx) throws BadArgumentException, CommandExecutionException;

    protected final void run(SubCommandContext ctx) {
        var msg = ctx.getMessage();
        var usr = msg.getAuthor();
        var member = msg.getMember();


        // child check

        String[] parts = String.join(" ", Arrays.copyOf(
                msg.getContentRaw().substring(ctx.getClient().getPrefix().length()).trim().split("\\s+", 2), 2))
                .split("\\s+");


        if (member == null || msg.isWebhookMessage()) {
            return;
        }

        if (!msg.isFromType(ChannelType.TEXT)) {
            return;
        }

        Member self = msg.getGuild().getSelfMember();

        if (!self.hasPermission(this.botPermissions)) {

            String perms = Arrays.stream(this.botPermissions)
                    .map(Permission::getName)
                    .collect(Collectors.joining());

            ctx.getChannel().sendMessageFormat(":x: I don't have the right permissions for this command. I need %s",
                    perms).queue();
            return;
        }

        if (!member.hasPermission(this.userPermissions)) {
            String perms = Arrays.stream(this.userPermissions)
                    .map(Permission::getName)
                    .collect(Collectors.joining());

            ctx.getChannel().sendMessageFormat(":x: You don't have the right permissions for this command. You need %s",
                    perms).queue();
            return;
        }

        if (this.supportServerOnly && ctx.getGuild().getIdLong() != 705938001519181877L) {
            ctx.getChannel()
                    .sendMessage(":x: This command can only be used in the support server.")
                    .queue();
            return;

        }

        if (!usr.getId().equals(ctx.getClient().getOwnerId()) && this.owner) {
            ctx.getChannel().sendMessage(
                    ":x: You don't have the right permissions for this command. You need Bot Owner").queue();
            return;
        }


        if (this.nsfw && !ctx.getChannel().isNSFW()) {
            ctx.getChannel().sendMessage("\uD83D\uDE0F This command can only be used in NSFW channels").queue(); // 😏
            return;

        }
        var cooldownMgr = new CooldownManager(ctx.getGuild().getId(), ctx.getAuthor().getId(), ctx.getEvent(), this);

        if (cooldownMgr.isOnCooldown()) {
            var embed = EmbedUtil.createDefaultError()
                    .setTitle("Woah there, slow it down a notch")
                    .setDescription(
                            "This command is on cooldown for another %s".formatted(cooldownMgr.getTimeRemaining()))
                    .build();

            ctx.getChannel()
                    .sendMessage(embed)
                    .queue();
            return;
        }

        try {
            this.execute(ctx);
            //            new CooldownManager(ctx.getGuild().getId(), ctx.getAuthor().getId(), ctx.getEvent(), this)
//                    .putCooldown();


        } catch (CommandExecutionException e) {

            if (e.getCause() instanceof BadArgumentException) {
                var bae = (BadArgumentException) e.getCause();
                var embed = EmbedUtil.createDefaultError()
                        .setTitle("One or more of the provided arguments is %s."
                                .formatted(bae.isMissing() ? "missing" : "invalid"))
                        .setDescription(
                                "Usage: `%s`. Refer to the [Docs](https://badstagram.gitbook.io/vortex) for more info."
                                        .formatted(
                                                this.usage))
                        .setFooter(
                                "%s argument: %s".formatted(bae.isMissing() ? "missing" : "invalid", bae.getArgument()))
                        .build();
                ctx.getChannel()
                        .sendMessage(embed)
                        .queue();

                return;
            }


            this.getLogger().error("There was an error while running a command", e);

            ErrorHandler.handleCommandError(e, this, ctx);

            var embed = EmbedUtil.createDefaultError()

                    .setTitle("There was an unexpected error while executing that command")
                    .setDescription("If this error persists, report it in the [Support Server](%s)".formatted(
                            Constants.SUPPORT_SERVER))
                    .setFooter(
                            "%s: %s".formatted(e.getCause().getClass().getCanonicalName(), e.getCause().getMessage()))
                    .build();

            ctx.getChannel()
                    .sendMessage(embed)
                    .queue();
        } catch (BadArgumentException e) {
            var embed = EmbedUtil.createDefaultError()
                    .setTitle("One or more of the provided arguments is %s."
                            .formatted(e.isMissing() ? "missing" : "invalid"))
                    .setDescription(
                            "Usage: `%s`. Refer to the [Docs](https://badstagram.gitbook.io/vortex) for more info."
                                    .formatted(
                                            this.usage))
                    .setFooter("%s argument: %s".formatted(e.isMissing() ? "missing" : "invalid", e.getArgument()))
                    .build();

            ctx.getChannel()
                    .sendMessage(embed)
                    .queue();
        }
    }

    @Override
    public void execute(CommandContext ctx) throws CommandExecutionException, BadArgumentException {
    }

}
