package pather.neofetchmc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class NeofetchCommand {
    public static final String ERROR_MISSING = "The Neofetch executable was not found on the system.";
    public static final String ERROR_INTERRUPTED = "The Neofetch process was interrupted. Please try again.";

    public static final String NEOFETCH_INFO_ONLY = "--off";
    public static final String NEOFETCH_LOGO_ONLY = "-L";

    public static final String NEOFETCH_ESCAPE_REGEX = "\u001B\\[[;?\\d]*[ -/]*[@-~]";
    public static final String WHITESPACE_LINE_REGEX = "(?m)^[ \t]*\r?\n";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("neofetch").executes(NeofetchCommand::run));
    }

    public static int run(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.of(getOutput()));
        return 1;
    }

    private static String getOutput() {
        return (getOutput(NEOFETCH_LOGO_ONLY).replaceAll(WHITESPACE_LINE_REGEX, "")
                + '\n'
                + getOutput(NEOFETCH_INFO_ONLY).replaceAll(WHITESPACE_LINE_REGEX, ""))
                .replaceAll(NEOFETCH_ESCAPE_REGEX, "");
        // Only temporary: remove colour escape sequences.
        // TODO: Add full colour support in future.
    }

    private static String getOutput(String neofetchArg) {
        ProcessBuilder pb = new ProcessBuilder("neofetch", "--block_width", "50", neofetchArg);
        Process neofetch = null;

        try {
            neofetch = pb.start();
        } catch (IOException e) {
            NeofetchMC.LOGGER.error(ERROR_MISSING, e);
            return ERROR_MISSING;
        }

        try {
            neofetch.waitFor();
        } catch (InterruptedException interruptedException) {
            NeofetchMC.LOGGER.error(ERROR_INTERRUPTED, interruptedException);
        }

        String output = new BufferedReader(
                new InputStreamReader(neofetch.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));

        return output;
    }
}