package com.kittentracker;

import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.inject.Inject;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Collections;

public class KittenOverlay extends OverlayPanel {
    private final Client client;
    private final KittenPlugin kittenPlugin;
    private final KittenConfig kittenConfig;

    @Inject
    private KittenOverlay(Client client, KittenPlugin kittenPlugin, KittenConfig kittenConfig) {
        super(kittenPlugin);
        setPosition(OverlayPosition.BOTTOM_LEFT);
        this.client = client;
        this.kittenPlugin = kittenPlugin;
        this.kittenConfig = kittenConfig;
        getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, OverlayManager.OPTION_CONFIGURE, "Kitten Tracker Overlay"));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (kittenPlugin.playerHasFollower() && (kittenPlugin.isKitten() || kittenPlugin.isCat())) {
            if ((kittenPlugin.isKitten() && (kittenConfig.kittenOverlay() || kittenConfig.kittenHungryOverlay()
                    || kittenConfig.kittenAttentionOverlay())
                    || (kittenPlugin.isCat()) && kittenConfig.catOverlay())) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .leftFont(graphics.getFont().deriveFont(
                                Collections.singletonMap(
                                        TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD)))
                        .left(kittenPlugin.isKitten() ? "Kitten status" : "Cat status")
                        .build());
            }

            if (kittenPlugin.isKitten()) {
                if (kittenConfig.kittenOverlay()) {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Adult: ")
                            .right(DurationFormatUtils.formatDuration(kittenPlugin.getTimeUntilFullyGrown(), "H:mm:ss", true))
                            .build()
                    );
                }

                if (kittenConfig.kittenHungryOverlay()) {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Food: ")
                            .right(DurationFormatUtils.formatDuration(kittenPlugin.getTimeBeforeHungry(), "H:mm:ss", true))
                            .build()
                    );
                }

                if (kittenConfig.kittenAttentionOverlay()) {
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Play: ")
                            .right(DurationFormatUtils.formatDuration(kittenPlugin.getTimeBeforeNeedingAttention(), "H:mm:ss", true))
                            .build()
                    );
                }
            } else {
                if (kittenPlugin.isOverGrown()) {
                    if (kittenConfig.catOverlay()) {
                        panelComponent.getChildren().add(LineComponent.builder()
                                .left("You have an overgrown cat.")
                                .build()
                        );
                    }
                } else {
                    if (kittenConfig.catOverlay()) {
                        panelComponent.getChildren().add(LineComponent.builder()
                                .left("Overgrown: ")
                                .right(DurationFormatUtils.formatDuration(kittenPlugin.getTimeUntilOvergrown(), "H:mm:ss", true))
                                .build()
                        );
                    }
                }
            }
        }

        return super.render(graphics);
    }


}
