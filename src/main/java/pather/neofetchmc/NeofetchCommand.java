package pather.neofetchmc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
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

    public static final String REGEX_ANSI_COLOUR = "\\u001B\\[[0-9]{1,}m";
    public static final String REGEX_ANSI_ESCAPE = "\u001B\\[[;?\\d]*[ -/]*[@-~]";
    public static final String REGEX_WHITESPACE = "(?m)^[ \t]*\r?\n";

    public static final String ANSI_ESCAPE_RESET = "\033[0m";
    public static final String ANSI_ESCAPE_ITALIC = "\033[3m";
    public static final String ANSI_ESCAPE_UNDERLINE = "\033[4m";
    public static final String ANSI_ESCAPE_BLACK = "\033[30m";
    public static final String ANSI_ESCAPE_RED = "\033[31m";
    public static final String ANSI_ESCAPE_GREEN = "\033[32m";
    public static final String ANSI_ESCAPE_YELLOW = "\033[33m";
    public static final String ANSI_ESCAPE_BLUE = "\033[34m";
    public static final String ANSI_ESCAPE_MAGENTA = "\033[35m";
    public static final String ANSI_ESCAPE_CYAN = "\033[36m";
    public static final String ANSI_ESCAPE_WHITE = "\033[37m";

    // Some custom names here, for consistency.
    // https://minecraft.fandom.com/wiki/Formatting_codes
    public static final String MINECRAFT_ESCAPE_RESET = "\u00A7r";
    public static final String MINECRAFT_ESCAPE_ITALIC = "\u00A7o";
    public static final String MINECRAFT_ESCAPE_UNDERLINE = "\u00A7n";
    public static final String MINECRAFT_ESCAPE_BLACK = "\u00A70";
    public static final String MINECRAFT_ESCAPE_RED = "\u00A7c";
    public static final String MINECRAFT_ESCAPE_GREEN = "\u00A7a";
    public static final String MINECRAFT_ESCAPE_YELLOW = "\u00A7e";
    public static final String MINECRAFT_ESCAPE_BLUE = "\u00A79";
    public static final String MINECRAFT_ESCAPE_MAGENTA = "\u00A75";
    public static final String MINECRAFT_ESCAPE_CYAN = "\u00A7b";
    public static final String MINECRAFT_ESCAPE_WHITE = "\u00A7f";

    public static final HashMap<String, String> ANSI_TO_MINECRAFT = new HashMap<>();
    static {
        ANSI_TO_MINECRAFT.put(ANSI_ESCAPE_RESET, MINECRAFT_ESCAPE_RESET);
        ANSI_TO_MINECRAFT.put(ANSI_ESCAPE_ITALIC, MINECRAFT_ESCAPE_ITALIC);
        ANSI_TO_MINECRAFT.put(ANSI_ESCAPE_UNDERLINE, MINECRAFT_ESCAPE_UNDERLINE);
        ANSI_TO_MINECRAFT.put(ANSI_ESCAPE_BLACK, MINECRAFT_ESCAPE_BLACK);
        ANSI_TO_MINECRAFT.put(ANSI_ESCAPE_RED, MINECRAFT_ESCAPE_RED);
        ANSI_TO_MINECRAFT.put(ANSI_ESCAPE_GREEN, MINECRAFT_ESCAPE_GREEN);
        ANSI_TO_MINECRAFT.put(ANSI_ESCAPE_YELLOW, MINECRAFT_ESCAPE_YELLOW);
        ANSI_TO_MINECRAFT.put(ANSI_ESCAPE_BLUE, MINECRAFT_ESCAPE_BLUE);
        ANSI_TO_MINECRAFT.put(ANSI_ESCAPE_MAGENTA, MINECRAFT_ESCAPE_MAGENTA);
        ANSI_TO_MINECRAFT.put(ANSI_ESCAPE_CYAN, MINECRAFT_ESCAPE_CYAN);
        ANSI_TO_MINECRAFT.put(ANSI_ESCAPE_WHITE, MINECRAFT_ESCAPE_WHITE);
    }

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
        return getMinecraftFormattedOutput(NEOFETCH_LOGO_ONLY)
            .replaceAll(REGEX_ANSI_ESCAPE, "")
            .replaceAll(REGEX_WHITESPACE, "")
            + '\n'
            + getMinecraftFormattedOutput(NEOFETCH_INFO_ONLY)
            .replaceAll(REGEX_ANSI_ESCAPE, "")
            .replaceAll(REGEX_WHITESPACE, "");
    }

    private static String getMinecraftFormattedOutput(String neofetchArg) {
        String[] splitOutput = getSplitOutput(neofetchArg);

        // Perhaps subject to future performance optimisations.
        for (int i = 0; i < splitOutput.length; i++) {
            if (ANSI_TO_MINECRAFT.containsKey(splitOutput[i])) {
                splitOutput[i] = ANSI_TO_MINECRAFT.get(splitOutput[i]);
            }
        }

        return String.join("", splitOutput);
        /*
         * We're only converting the relevant formatting escapes to Minecraft
         * escapes here. The rest will be processed out later.
         */
    }

    private static String[] getSplitOutput(String neofetchArg) {
        String splitRegex = "(?<=" + REGEX_ANSI_COLOUR + ")|(?=" + REGEX_ANSI_COLOUR + ")";
        String raw = getRawOutput(neofetchArg);
        String[] rawSplit = raw.split(splitRegex);
        return rawSplit;
    }

    private static String getRawOutput(String neofetchArg) {
        ProcessBuilder pb = new ProcessBuilder("neofetch", neofetchArg);
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