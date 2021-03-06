package me.badstagram.vortex.commands.moderation;

import me.badstagram.vortex.commandhandler.Category;
import me.badstagram.vortex.commandhandler.Command;
import me.badstagram.vortex.commandhandler.context.impl.CommandContext;
import me.badstagram.vortex.core.Constants;
import me.badstagram.vortex.entities.enums.GuildPunishmentType;
import me.badstagram.vortex.exceptions.BadArgumentException;
import me.badstagram.vortex.exceptions.CommandExecutionException;
import me.badstagram.vortex.managers.GuildPunishmentManager;
import me.badstagram.vortex.util.EmbedUtil;
import me.badstagram.vortex.util.ArgumentParser;
import net.dv8tion.jda.api.Permission;

public class Ban extends Command {
    public Ban() {
        this.name = "ban";
        this.help = "Ban a member from the guild";
        this.botPermissions = new Permission[] {Permission.BAN_MEMBERS};
        this.userPermissions = new Permission[] {Permission.BAN_MEMBERS};
        this.category = new Category("Moderation");
    }

    @Override
    public void execute(CommandContext ctx) throws CommandExecutionException, BadArgumentException {


        var parser = new ArgumentParser(ctx.getArgs(), ctx.getMessage(), ctx.getJDA(), ctx.getGuild());

        var parsed = parser.parseUser();

        if (parsed.isEmpty()) throw new BadArgumentException("member", false);

        try {

            var target = parsed.get(0);
            var reason = String.join(" ", ctx.getArgs()
                    .subList(1, ctx.getArgs()
                            .size()));

            if (target.getIdLong() == ctx.getAuthor()
                    .getIdLong()) {
                ctx.getChannel()
                        .sendMessage(":x: You can't ban yourself.")
                        .queue();
                return;
            }


            var isMember = ctx.getGuild()
                    .isMember(target);
            var targetMember = ctx.getGuild()
                    .getMember(target);




            if (isMember) {
                if (!ctx.getSelfMember()
                        .canInteract(targetMember) || !ctx.getMember()
                        .canInteract(targetMember)) {
                    ctx.getChannel()
                            .sendMessageFormat(":x: %s could not be banned.", target.getAsTag())
                            .queue();
                    return;
                }

                target.openPrivateChannel()
                        .flatMap(ch -> ch.sendMessageFormat("You have been banned from %s by %s for %s", ctx.getGuild()
                                .getName(), ctx.getAuthor()
                                .getAsTag(), reason))
                        .queue(null, (err) -> {
                        });
            }

            ctx.getGuild()
                    .ban(target, 7)
                    .queue(v -> {
                        try {
                            var caseId = new GuildPunishmentManager(target.getId(),ctx.getGuild().getId())
                                    .createCase(reason, GuildPunishmentType.BAN, ctx.getMember().getId(), false, null, null);

                            ctx.getChannel()
                                    .sendMessageFormat(":white_check_mark: %s has been banned for `%s` | Case #%d",
                                            target.getAsTag(),
                                            reason, caseId)
                                    .queue();
                        } catch (Exception e) {
                            var embed = EmbedUtil.createDefaultError()

                                    .setTitle("There was an unexpected error while executing that command")
                                    .setDescription(
                                            "If this error persists, report it in the [Support Server](%s)".formatted(
                                                    Constants.SUPPORT_SERVER))
                                    .setFooter(
                                            "%s: %s".formatted(e.getCause()
                                                    .getClass()
                                                    .getCanonicalName(), e.getCause()
                                                    .getMessage()))
                                    .build();

                            ctx.getChannel()
                                    .sendMessage(embed)
                                    .queue();
                        }
                    }, ignored -> ctx.getChannel()
                            .sendMessageFormat(":x: %s could not be banned.", target.getAsTag())
                            .queue());
        } catch (Exception e) {
            throw new CommandExecutionException(e);
        }

    }
}
