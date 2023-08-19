package pather.neofetchmc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeofetchMC implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("neofetch-mc");

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(NeofetchCommand::register);
    }
}