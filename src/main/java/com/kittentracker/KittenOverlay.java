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
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

public class KittenOverlay extends OverlayPanel {
    private final Client client;
    private final KittenPlugin kittenPlugin;
    private final KittenConfig kittenConfig;
    private Instant blinkHunger = null;
    private Instant blinkAttention = null;

    private final static int blinkPeriod = 1000;

    @Inject
    private KittenOverlay(Client client, KittenPlugin kittenPlugin, KittenConfig kittenConfig) {
        super(kittenPlugin);
        setPosition(OverlayPosition.BOTTOM_LEFT);
        this.client = client;
        this.kittenPlugin = kittenPlugin;
        this.kittenConfig = kittenConfig;
        getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, OverlayManager.OPTION_CONFIGURE, "Kitten Tracker Overlay"));
        setPreferredSize(new Dimension(162, 88));
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
                    LineComponent lineComponent = LineComponent.builder()
                            .left("Grown up in: ")
                            .right(DurationFormatUtils.formatDuration(kittenPlugin.getTimeUntilFullyGrown(), getFormatForTime(), true))
                            .build();
                    panelComponent.getChildren().add(lineComponent);
                }

                if (kittenConfig.kittenHungryOverlay()) {
                    Color color = Color.WHITE;
                    Long timeUntilHungryMs = kittenPlugin.getTimeBeforeHungry();
                    if (timeUntilHungryMs < KittenPlugin.HUNGRY_TIME_ONE_MINUTE_WARNING_MS) { //
                        if (blinkHunger == null) {
                            blinkHunger = Instant.now();
                        } else {
                            Duration timeSinceLastBlink = Duration.between(blinkHunger, Instant.now());

                            if (timeSinceLastBlink.toMillis() > 2 * blinkPeriod) {
                                blinkHunger = Instant.now();
                                color = Color.ORANGE;
                            } else if (timeSinceLastBlink.toMillis() > blinkPeriod) {
                                color = Color.ORANGE;
                            } else {
                                color = Color.RED;
                            }
                        }
                    } else if (timeUntilHungryMs < KittenPlugin.HUNGRY_FINAL_WARNING_TIME_LEFT_IN_SECONDS * 1000) {
                        color = Color.RED;
                    } else if (timeUntilHungryMs < KittenPlugin.HUNGRY_FIRST_WARNING_TIME_LEFT_IN_SECONDS * 1000) {
                        color = Color.ORANGE;
                    }

                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Hungry in: ")
                            .rightColor(color)
                            .right(DurationFormatUtils.formatDuration(timeUntilHungryMs, getFormatForTime(), true))
                            .build()
                    );
                }

                if (kittenConfig.kittenAttentionOverlay()) {
                    Color color = Color.WHITE;
                    Long timeBeforeNeedingAttention = kittenPlugin.getTimeBeforeNeedingAttention();
                    if (timeBeforeNeedingAttention < KittenPlugin.ATTENTION_TIME_ONE_MINUTE_WARNING_MS) { //
                        if (blinkAttention == null) {
                            blinkAttention = Instant.now();
                        } else {
                            Duration timeSinceLastBlink = Duration.between(blinkAttention, Instant.now());

                            if (timeSinceLastBlink.toMillis() > 2 * blinkPeriod) {
                                blinkAttention = Instant.now();
                                color = Color.ORANGE;
                            } else if (timeSinceLastBlink.toMillis() > blinkPeriod) {
                                color = Color.ORANGE;
                            } else {
                                color = Color.RED;
                            }
                        }
                    } else if (timeBeforeNeedingAttention < KittenPlugin.ATTENTION_FINAL_WARNING_TIME_LEFT_IN_SECONDS * 1000) {
                        color = Color.RED;
                    } else if (timeBeforeNeedingAttention < KittenPlugin.ATTENTION_FIRST_WARNING_TIME_LEFT_IN_SECONDS * 1000) {
                        color = Color.ORANGE;
                    }

                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Needs attention in: ")
                            .rightColor(color)
                            .right(DurationFormatUtils.formatDuration(timeBeforeNeedingAttention, getFormatForTime(), true))
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
                                .left("Overgrown in: ")
                                .right(DurationFormatUtils.formatDuration(kittenPlugin.getTimeUntilOvergrown(), getFormatForTime(), true))
                                .build()
                        );
                    }
                }
            }
        }

        return super.render(graphics);
    }

    private String getFormatForTime() {
        if (kittenConfig.displaySeconds()) {
            return "H:mm:ss";
        } else {
            return "H:mm";
        }
    }

}
