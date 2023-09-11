package pather.neofetchmc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public static final char[] COLOUR_BAR_TOP = {'0', '4', '2', 'g', '1', '5', '3', '7'};
    public static final char[] COLOUR_BAR_BOTTOM = {'7', 'c', 'a', 'e', '9', 'd', 'b', 'f'};
    public static final String CHAR_BOX = "\u2588";
    public static final String STRING_BOX = CHAR_BOX + CHAR_BOX + CHAR_BOX;
    public static final int LENGTH_COLOUR_BAR = 42; //With extraneous whitespace

    public static final String REGEX_ANSI_COLOUR = "\\u001B\\[[0-9]{1,}m";
    public static final String REGEX_ANSI_ESCAPE = "\u001B\\[[;?\\d]*[ -/]*[@-~]";
    public static final String REGEX_ANSI_NO_CONSUME = "(?<=" + REGEX_ANSI_COLOUR + ")|(?=" + REGEX_ANSI_COLOUR + ")";
    public static final String REGEX_MINECRAFT_ESCAPE = "\\u00A7[a-z]";
    public static final String REGEX_WHITESPACE = "(?m)^[ \t]*\r?\n";

    public static final String ANSI_ESCAPE = "\033[";
    public static final String ANSI_ESCAPE_RESET = ANSI_ESCAPE + "0m";
    public static final String ANSI_ESCAPE_ITALIC = ANSI_ESCAPE + "3m";
    public static final String ANSI_ESCAPE_UNDERLINE = ANSI_ESCAPE + "4m";
    public static final String ANSI_ESCAPE_BLACK = ANSI_ESCAPE + "30m";
    public static final String ANSI_ESCAPE_RED = ANSI_ESCAPE + "31m";
    public static final String ANSI_ESCAPE_GREEN = ANSI_ESCAPE + "32m";
    public static final String ANSI_ESCAPE_YELLOW = ANSI_ESCAPE + "33m";
    public static final String ANSI_ESCAPE_BLUE = ANSI_ESCAPE + "34m";
    public static final String ANSI_ESCAPE_MAGENTA = ANSI_ESCAPE + "35m";
    public static final String ANSI_ESCAPE_CYAN = ANSI_ESCAPE + "36m";
    public static final String ANSI_ESCAPE_WHITE = ANSI_ESCAPE + "37m";

    // Some custom names here, for consistency.
    // https://minecraft.fandom.com/wiki/Formatting_codes
    public static final String MINECRAFT_ESCAPE = "\u00A7";
    public static final String MINECRAFT_ESCAPE_RESET = MINECRAFT_ESCAPE + 'r';
    public static final String MINECRAFT_ESCAPE_ITALIC = MINECRAFT_ESCAPE + 'o';
    public static final String MINECRAFT_ESCAPE_UNDERLINE = MINECRAFT_ESCAPE + 'n';
    public static final String MINECRAFT_ESCAPE_BLACK = MINECRAFT_ESCAPE + '0';
    public static final String MINECRAFT_ESCAPE_RED = MINECRAFT_ESCAPE + 'c';
    public static final String MINECRAFT_ESCAPE_GREEN = MINECRAFT_ESCAPE + 'a';
    public static final String MINECRAFT_ESCAPE_YELLOW = MINECRAFT_ESCAPE + 'e';
    public static final String MINECRAFT_ESCAPE_BLUE = MINECRAFT_ESCAPE + '9';
    public static final String MINECRAFT_ESCAPE_MAGENTA = MINECRAFT_ESCAPE + '5';
    public static final String MINECRAFT_ESCAPE_CYAN = MINECRAFT_ESCAPE + 'b';
    public static final String MINECRAFT_ESCAPE_WHITE = MINECRAFT_ESCAPE + 'f';

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
        return getLogoMinecraftFormatted()
                + "\n\n"
                + getInfoMinecraftFormatted()
                + "\n\n"
                + getColourBar();
    }

    private static String getLogoMinecraftFormatted() {
        String logo = getRawOutput(NEOFETCH_LOGO_ONLY);
        logo = convertAnsiToMinecraft(logo);
        logo = removeExtraneousCharacters(logo);
        logo = logo.replaceAll(MINECRAFT_ESCAPE_RESET, "");
        logo = addEnclosingEscapes(logo);
        return logo;
    }

    private static String getInfoMinecraftFormatted() {
        String info = getRawOutput(NEOFETCH_INFO_ONLY);
        info = convertAnsiToMinecraft(info);
        info = removeExtraneousCharacters(info);
        info = info.substring(0, info.length() - LENGTH_COLOUR_BAR);
        return info;
    }

    private static String getColourBar() {
        StringBuilder colourBar = new StringBuilder();

        for (char c: COLOUR_BAR_TOP) {
            colourBar.append(MINECRAFT_ESCAPE);
            colourBar.append(c);
            colourBar.append(STRING_BOX);
        }

        colourBar.append('\n');

        for (char c: COLOUR_BAR_BOTTOM) {
            colourBar.append(MINECRAFT_ESCAPE);
            colourBar.append(c);
            colourBar.append(STRING_BOX);
        }

        return colourBar.toString();
    }

    private static String removeExtraneousCharacters(String in) {
        in = in.replaceAll(REGEX_ANSI_ESCAPE, "")
                .replaceAll(REGEX_WHITESPACE, "");
        in = in.substring(0, in.length() - 1); // Extraneous newline
        return in;
    }

    private static String convertAnsiToMinecraft(String ansiFormatted) {
        String[] split = ansiFormatted.split(REGEX_ANSI_NO_CONSUME);

        for (int i = 0; i < split.length; i++) {
            if (ANSI_TO_MINECRAFT.containsKey(split[i])) {
                split[i] = ANSI_TO_MINECRAFT.get(split[i]);
            }
        }

        return String.join("", split);
    }

    private static String addEnclosingEscapes(String minecraftFormatted) {
        /* Add enclosing characters */
        Pattern p = Pattern.compile(REGEX_MINECRAFT_ESCAPE);
        String[] split = minecraftFormatted.split("\n");

        for (int i = 1; i < split.length; i++) {
            Matcher m = p.matcher(split[i - 1]);
            String lastMatch = null;

            while (m.find()) {
                lastMatch = m.group(m.groupCount());
            }

            split[i] = lastMatch + split[i];
        }

        for (int i = 0; i < split.length; i++) {
            split[i] += MINECRAFT_ESCAPE_RESET;
        }

        minecraftFormatted = String.join("\n", split);
        return minecraftFormatted;
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