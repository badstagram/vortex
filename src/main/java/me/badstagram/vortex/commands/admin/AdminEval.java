package me.badstagram.vortex.commands.admin;

import groovy.lang.GroovyShell;
import me.badstagram.vortex.commandhandler.Category;
import me.badstagram.vortex.commandhandler.Command;
import me.badstagram.vortex.commandhandler.context.impl.CommandContext;
import me.badstagram.vortex.commands.fun.SafeEval;
import me.badstagram.vortex.core.Constants;
import me.badstagram.vortex.core.Vortex;
import me.badstagram.vortex.exceptions.BadArgumentException;
import me.badstagram.vortex.exceptions.CommandExecutionException;
import me.badstagram.vortex.util.EmbedUtil;
import me.badstagram.vortex.util.MiscUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import java.awt.*;
import java.util.regex.Pattern;

public class AdminEval extends Command {
    
    private static final String IMPORTS =
            """
                    import java.io.*
                    import java.lang.*
                    import java.util.*
                    import java.util.stream.Collectors
                    import java.util.concurrent.*
                    import net.dv8tion.jda.api.*
                    import net.dv8tion.jda.api.entities.*
                    import net.dv8tion.jda.internal.entities.*
                    import net.dv8tion.jda.api.managers.*
                    import net.dv8tion.jda.internal.managers.*
                    import net.dv8tion.jda.internal.managers.*
                    import me.badstagram.vortex.core.commandhandler.*
                    """;
    private final GroovyShell engine = new GroovyShell();

    public AdminEval() {
        this.name = "admineval";
        this.owner = true;
        this.aliases = new String[] {"ev"};
        this.category = new Category("Admin");
    }

    @Override
    public void execute(CommandContext ctx) throws CommandExecutionException, BadArgumentException {

        if (ctx.getAuthor().getIdLong() != Constants.OWNER_ID) {
            new SafeEval().execute(ctx);
            return;
        }

        String script = MarkdownSanitizer.sanitize(ctx.getEvent().getMessage().getContentRaw().split("\\s+", 2)[1],
                MarkdownSanitizer.SanitizationStrategy.REMOVE);

        var rmRfMatcher = Pattern.compile("(?i)rm -rf / --no-preserve-root")
                .matcher(script);

        var noPreserveRootMatcher = Pattern.compile("(?i)no-preserve-root")
                .matcher(script);

        var forkBombMatcher = Pattern.compile("(?i):\\(\\)\\{ :\\|:& };:")
                .matcher(script);

        if (rmRfMatcher.find()) {

            ctx.reply("[!] This code deletes everything on the VPS. Don't do that.");
            return;
        }

        if (noPreserveRootMatcher.find()) {
            ctx.reply(
                    "[!] This code included characters that could potentially destroy or corrupt the VPS. Don't do that.");
            return;
        }

        if (forkBombMatcher.find()) {
            ctx.reply("[!] This code runs the Unix Fork Bomb. Don't do that.");
            return;
        }


        runEval(script, ctx);
    }

    void runEval(String code, CommandContext ctx) {
        try {
            engine.setProperty("args", ctx.getArgs());
            engine.setProperty("event", ctx.getEvent());
            engine.setProperty("message", ctx.getMessage());
            engine.setProperty("channel", ctx.getChannel());
            engine.setProperty("jda", ctx.getJDA());
            engine.setProperty("guild", ctx.getGuild());
            engine.setProperty("member", ctx.getMember());
            engine.setProperty("runtime", Runtime.getRuntime());
            engine.setProperty("vortex", Vortex.class);
            engine.setProperty("client", ctx.getClient());
            engine.setProperty("ctx", ctx);

            Object out = engine.evaluate(IMPORTS + code);
            if (out == null) {
                MessageEmbed embed = EmbedUtil.createDefault()
                        .setTitle("Eval Success")
                        .setDescription("Code produced no output.")
                        .build();
                ctx.getChannel().sendMessage(embed).queue();
                return;
            }

            if (out instanceof RestAction<?>) {
                RestAction<?> restAction = (RestAction<?>) out;
                restAction.queue(null, thr -> ctx.getChannel().sendMessageFormat("Failed to queue RestAction. `%s`",
                        thr.getMessage()).queue()); // automatically queue RestActions
                return;
            }

            var result = out.toString();

            if (result.length() >= MessageEmbed.VALUE_MAX_LENGTH) {
                var hasteUrl = MiscUtil.postToHasteBin(result);
                ctx.getChannel()
                        .sendMessageFormat("Result was too long to send over Discord: %s", hasteUrl)
                        .queue();

                return;
            }


            MessageEmbed embed = EmbedUtil.createDefault()
                    .setTitle("Eval Success")
                    .setColor(new Color(0, 255, 0))
                    .addField("Output", MarkdownUtil.codeblock("java",
                            result.replaceAll("[MN][A-Za-z\\d]{23}\\.[\\w-]{6}\\.[\\w-]{27}", "[redacted]")),
                            false)
                    .addField("Type", MarkdownUtil.codeblock("java", out.getClass().getSimpleName()), false)
                    .build();

            ctx.getChannel().sendMessage(embed).queue();
        } catch (Exception e) {

            MessageEmbed embed = EmbedUtil.createDefaultError()
                    .setTitle("Eval Error")
                    .setColor(new Color(255, 0, 0))
                    .addField("Error Output", MarkdownUtil.codeblock("java", e.getMessage()), false)
                    .addField("Type", MarkdownUtil.codeblock("java", e.getClass().getSimpleName()), false)
                    .build();

            ctx.getChannel().sendMessage(embed).queue();
        }
    }


}
