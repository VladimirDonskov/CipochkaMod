package com.example.cipochka;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CipochkaClient implements ClientModInitializer {
    public static final String MOD_ID = "cipochka";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Cipochka client mod loaded.");
    }
}
