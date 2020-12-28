package me.badstagram.vortex.util;

import io.sentry.Breadcrumb;
import io.sentry.Scope;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import me.badstagram.vortex.commandhandler.Command;
import me.badstagram.vortex.commandhandler.context.CommandContext;
import me.badstagram.vortex.commandhandler.context.SubCommandContext;
import me.badstagram.vortex.core.RunMode;
import me.badstagram.vortex.core.Vortex;

public class ErrorHandler {
    public static void handleCommandError(Throwable thr, Command cmd, CommandContext ctx) {
//        if (Vortex.getRunMode() == RunMode.DEVELOPMENT) return; // disable sentry logging on development

        Sentry.configureScope(scope -> {
            var user = new User();
            user.setUsername(ctx.getAuthor().getAsTag());
            user.setId(ctx.getAuthor().getId());

            var breadcrumb = new Breadcrumb();

            breadcrumb.setData("Command", cmd.getName());
            breadcrumb.setData("Command Args", String.join(" ", ctx.getArgs()));
            breadcrumb.setData("Message", String.join(" ", ctx.getEvent().getMessage().getContentRaw()));

            scope.setContexts("Guild Name", ctx.getGuild().getName());
            scope.setContexts("Guild ID", ctx.getGuild().getId());

            scope.setTag("Environment", Vortex.getRunMode().getName());

            scope.setUser(user);
            scope.addBreadcrumb(breadcrumb);

        });

        Sentry.captureException(thr);
        Sentry.configureScope(Scope::clear);
    }

    public static void handleCommandError(Throwable thr, Command cmd, SubCommandContext ctx) {
//        if (Vortex.getRunMode() == RunMode.DEVELOPMENT) return; // disable sentry logging on development

        Sentry.configureScope(scope -> {
            var user = new User();
            user.setUsername(ctx.getAuthor().getAsTag());
            user.setId(ctx.getAuthor().getId());

            var breadcrumb = new Breadcrumb();

            breadcrumb.setData("Command", cmd.getName());
            breadcrumb.setData("Command Args", String.join(" ", ctx.getArgs()));
            breadcrumb.setData("Message", String.join(" ", ctx.getEvent().getMessage().getContentRaw()));

            scope.setContexts("Guild Name", ctx.getGuild().getName());
            scope.setContexts("Guild ID", ctx.getGuild().getId());

            scope.setTag("Environment", Vortex.getRunMode().getName());

            scope.setUser(user);
            scope.addBreadcrumb(breadcrumb);

        });

        Sentry.captureException(thr);
        Sentry.configureScope(Scope::clear);
    }

    public static void handle(Throwable thr) {
        if (Vortex.getRunMode() == RunMode.DEVELOPMENT) {
            thr.printStackTrace();
            return;
        } // disable sentry logging on development

        Sentry.captureException(thr);
    }
}
