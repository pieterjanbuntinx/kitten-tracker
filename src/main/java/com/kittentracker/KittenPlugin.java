/*
 * Copyright (c) 2018, Nachtmerrie <https://github.com/Nachtmerrie>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.kittentracker;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.Timer;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

@Slf4j
@PluginDescriptor(
        name = "Kitten Tracker"
)
public class KittenPlugin extends Plugin {
    private static final int VAR_PLAYER_FOLLOWER = 447;
    private static final int WIDGET_ID_DIALOG_NOTIFICATION_GROUP_ID = 229;
    private static final int WIDGET_ID_DIALOG_PLAYER_TEXT = 6;
    private static final int WIDGET_ID_DIALOG_NOTIFICATION_TEXT = 1;

    private static final String DIALOG_CAT_STROKE = "That cat sure loves to be stroked.";
    private static final String DIALOG_CAT_BALL_OF_WOOL = "That kitten loves to play with that ball of wool. I think itis its favourite."; // The typo is intentional and how the game reads it...
    private static final String DIALOG_CAT_GROWN = "Your kitten has grown into a healthy cat that can hunt for itself.";
    private static final String DIALOG_CAT_OVERGROWN = "Your cat has grown into a mighty feline, but it will no longer be able to chase vermin.";
    private static final String DIALOG_AFTER_TAKING_A_GOOD_LOOK = "After taking a good look at your kitten you guess that its age is: ";
    private static final String DIALOG_HAND_OVER_CAT_CIVILIAN = "You hand over the cat.You are given";
    private static final String DIALOG_GERTRUDE_GIVES_YOU_ANOTHER_KITTEN = "Gertrude gives you another kitten.";

    private static final String CHAT_STROKE_CAT = "You softly stroke your cat.";
    private static final String CHAT_THE_KITTEN_GOBBLES_UP_THE_FISH = "The kitten gobbles up the fish.";
    private static final String CHAT_THE_KITTEN_LAPS_UP_THE_MILK = "The kitten laps up the milk.";
    private static final String CHAT_YOUR_KITTEN_IS_HUNGRY = "Your kitten is hungry.";
    private static final String CHAT_YOUR_KITTEN_IS_VERY_HUNGRY = "Your kitten is very hungry.";
    private static final String CHAT_YOUR_KITTEN_WANTS_ATTENTION = "Your kitten wants attention.";
    private static final String CHAT_YOUR_KITTEN_REALLY_WANTS_ATTENTION = "Your kitten really wants attention.";
    private static final String CHAT_YOUR_KITTEN_GOT_LONELY_AND_RAN_OFF = "Your kitten got lonely and ran off.";
    private static final String CHAT_THE_CAT_HAS_RUN_AWAY = "The cat has run away.";

    private static final String NOTIFICATION_KITTEN_WILL_RUN_AWAY_IN_1_MINUTE = "Kitten will run away in 1 minute!";

    private static final String TOOLTIP_APPROXIMATE_TIME_LEFT_TO_GROW_INTO_A_CAT = "Approximate time left to grow into a cat";
    private static final String TOOLTIP_APPROXIMATE_TIME_LEFT_TO_TRANSFORM_INTO_AN_OVERGROWN_CAT = "Approximate time left to transform into an overgrown cat";
    private static final String TOOLTIP_TIME_UNTIL_YOUR_KITTEN_LEAVES_YOU_FOR_BEING_UNDERFED = "Time until your kitten leaves you for being underfed";
    private static final String TOOLTIP_APPROXIMATE_TIME_UNTIL_YOUR_KITTEN_LEAVES_YOU_FOR_BEING_NEGLECTFUL = "Approximate time until your kitten leaves you for being neglectful";

    public static final int HUNGRY_FIRST_WARNING_TIME_LEFT_IN_SECONDS = 6 * 60; // 6 MINUTES
    public static final int HUNGRY_FINAL_WARNING_TIME_LEFT_IN_SECONDS = 3 * 60; // 3 MINUTES
    public static final long HUNGRY_TIME_ONE_MINUTE_WARNING_MS = 1 * 60 * 1000; // 1 MINUTE
    private static final int HUNGRY_TIME_BEFORE_FIRST_WARNING_IN_MINUTES = 24; // 24 MINUTES
    private static final int HUNGRY_TIME_BEFORE_FINAL_WARNING_IN_MINUTES = 27; // 27 MINUTES
    private static final int HUNGRY_TIME_BEFORE_KITTEN_RUNS_AWAY_IN_SECONDS = 30 * 60; // 30 MINUTES

    public static final int ATTENTION_FIRST_WARNING_TIME_LEFT_IN_SECONDS = 14 * 60; // 14 MINUTES
    public static final int ATTENTION_FINAL_WARNING_TIME_LEFT_IN_SECONDS = 7 * 60; // 7 MINUTES
    public static final long ATTENTION_TIME_ONE_MINUTE_WARNING_MS = 1 * 60 * 1000; // 1 MINUTE
    public static final int ATTENTION_TIME_NEW_KITTEN_IN_SECONDS = 25 * 60; // 25 MINUTES
    public static final int ATTENTION_TIME_SINGLE_STROKE_IN_SECONDS = 18 * 60; // 18 MINUTES
    public static final int ATTENTION_TIME_MULTIPLE_STROKES_IN_SECONDS = 25 * 60; // 25 MINUTES
    public static final int ATTENTION_TIME_BALL_OF_WOOL_IN_SECONDS = 51 * 60; // 51 MINUTES
    public static final int ATTENTION_TIME_FROM_WARNING_TO_RUNNING_AWAY_IN_SECONDS = (7 + 7) * 60; // 14 MINUTES
    private static final int ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_NEW_KITTEN_IN_SECONDS = ATTENTION_TIME_NEW_KITTEN_IN_SECONDS + ATTENTION_TIME_FROM_WARNING_TO_RUNNING_AWAY_IN_SECONDS; // 32 MINUTES
    private static final int ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_SINGLE_STROKE_IN_SECONDS = ATTENTION_TIME_SINGLE_STROKE_IN_SECONDS + ATTENTION_TIME_FROM_WARNING_TO_RUNNING_AWAY_IN_SECONDS; // 32 MINUTES
    private static final int ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_MULTIPLE_STROKES_IN_SECONDS = ATTENTION_TIME_MULTIPLE_STROKES_IN_SECONDS + ATTENTION_TIME_FROM_WARNING_TO_RUNNING_AWAY_IN_SECONDS; // 39 MINUTES
    private static final int ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_BALL_OF_WOOL_IN_SECONDS = ATTENTION_TIME_BALL_OF_WOOL_IN_SECONDS + ATTENTION_TIME_FROM_WARNING_TO_RUNNING_AWAY_IN_SECONDS; // 65 MINUTES

    private static final int TIME_TO_ADULTHOOD_IN_SECONDS = 3 * 3600; // 3 HOURS
    private static final int TIME_TILL_OVERGROWN_IN_SECONDS = (int) (2.5 * 3600); // 2-3 HOURS -> 2.5 HOURS

    private boolean ready;
    private Instant kittenSpawnedTime;
    private Instant catSpawnedTime;
    private Instant kittenLastFedTime;
    private Instant kittenLastAttentionTime;
    private int timeSpendGrowing = 0;
    private int timeNeglected = 0;
    private int timeHungry = 0;
    private int followerID = 0;
    private int previousFollowerId = 0;
    private KittenAttentionType lastAttentionType = KittenAttentionType.NEW_KITTEN;

    private Timer kittenAttentionTimer, growthTimer, kittenHungryTimer;

    private FollowerKind followerKind = FollowerKind.NON_FELINE;

    private boolean attentionNotificationSend = false;
    private boolean hungryNotificationSend = false;

    private HashMap<Integer, Instant> openedWidgets = new HashMap<>();

    @Inject
    private Client client;
    @Inject
    private Notifier notifier;
    @Inject
    private KittenConfig config;
    @Inject
    private ClientThread clientThread;
    @Inject
    private ItemManager itemManager;
    @Inject
    private InfoBoxManager infoBoxManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private KittenOverlay overlay;

    @Provides
    KittenConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(KittenConfig.class);
    }

    @Override
    public void startUp() {
        clientThread.invokeLater(this::checkForFollower);
        previousFollowerId = config.felineId();
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        byeFollower();
    }

    private void checkForFollower() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        if (playerHasFollower()) {
            newFollower();
        }
    }


    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (playerHasFollower() && followerID != getCurrentFollowerId()) { // player has a new follower
            checkForNewFollower();
        }

        if (!playerHasFollower() && followerID != 0) // player lost it's follower
        {
            byeFollower();
        }
    }

    private void checkForNewFollower() {
        followerID = getCurrentFollowerId();
        if (followerID > 0) // Varbit needs to fill up first after logging in
        {
            previousFollowerId = followerID; // config.felineId();
            newFollower();
        }
    }

    private int getCurrentFollowerId() {
        int followerVarPlayerValue = client.getVarpValue(VAR_PLAYER_FOLLOWER);
        // followerID is the first 2 bytes
        return (followerVarPlayerValue >> 16);
    }

    private void newFollower() {
        attentionNotificationSend = false;
        hungryNotificationSend = false;
        followerKind = FollowerKind.getFromFollowerId(followerID);

        switch (followerKind) {
            case KITTEN:
                if (followerID == previousFollowerId) // The same kitten is back!
                {
                    timeSpendGrowing = config.secondsAlive();
                    timeNeglected = config.secondsNeglected();
                    timeHungry = config.secondsHungry();
                    kittenSpawnedTime = Instant.now();
                    lastAttentionType = config.lastAttentionType();
                    addKittenGrowthBox(TIME_TO_ADULTHOOD_IN_SECONDS - timeSpendGrowing);
                    addHungryTimer(HUNGRY_TIME_BEFORE_KITTEN_RUNS_AWAY_IN_SECONDS - timeHungry);
                    addAttentionTimer(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_SINGLE_STROKE_IN_SECONDS - timeNeglected);
                } else // new kitten, new timer
                {
                    config.secondsAlive(0);
                    config.secondsHungry(0);
                    config.secondsNeglected(0);
                    config.lastAttentionType(KittenAttentionType.NEW_KITTEN);

                    kittenSpawnedTime = Instant.now();
                    kittenLastFedTime = Instant.now();
                    kittenLastAttentionTime = Instant.now();
                    addKittenGrowthBox(TIME_TO_ADULTHOOD_IN_SECONDS);
                    addHungryTimer(HUNGRY_TIME_BEFORE_KITTEN_RUNS_AWAY_IN_SECONDS);
                    addAttentionTimer(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_MULTIPLE_STROKES_IN_SECONDS);
                }
                break;
            case NORMAL_CAT:
                if (followerID == previousFollowerId) { // The same cat is back!
                    timeSpendGrowing = config.secondsAlive();
                    addKittenGrowthBox((TIME_TO_ADULTHOOD_IN_SECONDS + TIME_TILL_OVERGROWN_IN_SECONDS - timeSpendGrowing));
                } else { // new cat, new timer
                    catSpawnedTime = Instant.now();
                    addKittenGrowthBox(TIME_TILL_OVERGROWN_IN_SECONDS);
                }
                break;
            case LAZY_CAT:
            case WILY_CAT:
            case OVERGROWN_CAT:
            case NON_FELINE:
                break;
        }
    }

    private void byeFollower() {
        switch (followerKind) {
            case KITTEN:
                saveGrowthProgress();
                kittenSpawnedTime = null;
                kittenLastFedTime = null;
                kittenLastAttentionTime = null;
                break;
            case NORMAL_CAT:
                saveGrowthProgress();
                catSpawnedTime = null;
                break;
            case LAZY_CAT:
            case WILY_CAT:
            case OVERGROWN_CAT:
            case NON_FELINE:
                kittenSpawnedTime = null;
                kittenLastFedTime = null;
                kittenLastAttentionTime = null;
                catSpawnedTime = null;
                break;
        }

        infoBoxManager.removeIf(t -> t instanceof KittenGrowthTimer);
        infoBoxManager.removeIf(t -> t instanceof KittenHungryTimer);
        infoBoxManager.removeIf(t -> t instanceof KittenAttentionTimer);

        followerID = 0;
    }

    private void saveGrowthProgress() {
        switch (followerKind) {
            case KITTEN: {
                config.felineId(followerID);
                if (kittenSpawnedTime != null) {
                    Duration timeAlive = Duration.between(kittenSpawnedTime, Instant.now());
                    int secondsAlive = Math.toIntExact(timeAlive.getSeconds());
                    config.secondsAlive(timeSpendGrowing + secondsAlive);
                } else {
                    log.debug("TimeAlive is null, no follower...");
                }

                if (kittenLastFedTime != null) {
                    Duration timeFed = Duration.between(kittenLastFedTime, Instant.now());
                    int secondsFed = Math.toIntExact(timeFed.getSeconds());
                    config.secondsHungry(secondsFed);
                } else if (kittenSpawnedTime != null) {
                    // kitten was not fed, so we add the time it has been out to the time it was already hungry for
                    Duration timeSinceSpawn = Duration.between(kittenSpawnedTime, Instant.now());
                    int secondsSinceSpawn = Math.toIntExact(timeSinceSpawn.getSeconds());
                    config.secondsHungry(timeHungry + secondsSinceSpawn);
                }

                if (kittenLastAttentionTime != null) {
                    Duration timeAttention = Duration.between(kittenLastAttentionTime, Instant.now());
                    int secondsAttention = Math.toIntExact(timeAttention.getSeconds());
                    config.secondsNeglected(secondsAttention);
                } else if (kittenSpawnedTime != null) {
                    // kitten was not paid attention, so we add the time it has been out to the time it was already neglected for
                    Duration timeSinceSpawn = Duration.between(kittenSpawnedTime, Instant.now());
                    int secondsSinceSpawn = Math.toIntExact(timeSinceSpawn.getSeconds());
                    config.secondsNeglected(timeNeglected + secondsSinceSpawn);
                }
                config.lastAttentionType(lastAttentionType);
                break;
            }
            case NORMAL_CAT: {
                config.felineId(followerID);
                if (kittenSpawnedTime != null) {
                    Duration timeAlive = Duration.between(catSpawnedTime, Instant.now());
                    int secondsAlive = Math.toIntExact(timeAlive.getSeconds());
                    config.secondsAlive(timeSpendGrowing + secondsAlive);
                } else {
                    log.debug("TimeAlive is null, no follower...");
                }
                break;
            }
            case LAZY_CAT:
            case WILY_CAT:
            case OVERGROWN_CAT:
            case NON_FELINE:
                break;
        }
    }

    private void addKittenGrowthBox(int seconds) {
        if (seconds <= 0) {
            return;
        }
        Felines feline = Felines.find(followerID);

        if (feline == null) {
            return;
        }

        growthTimer = new KittenGrowthTimer(feline, itemManager.getImage(feline.getItemSpriteId()), this, Duration.ofSeconds(seconds));
    }

    private void addHungryTimer(int seconds) {
        if (seconds <= 0) {
            return;
        }
        kittenHungryTimer = new KittenHungryTimer(itemManager.getImage(ItemID.SEASONED_SARDINE), this, Duration.ofSeconds(seconds));
    }

    private void addAttentionTimer(int seconds) {
        if (seconds <= 0) {
            return;
        }
        kittenAttentionTimer = new KittenAttentionTimer(itemManager.getImage(1759), this, Duration.ofSeconds(seconds));
    }

    // This is where the player interaction checks occur
    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }
        String message = Text.removeTags(event.getMessage());
        switch (message) {
            case CHAT_STROKE_CAT: {
                if (kittenLastAttentionTime != null) { // if kitten has had attention within the time since spawn
                    long timeSinceLastAttentionSeconds = Duration.between(kittenLastAttentionTime, Instant.now()).toMillis() / 1000 + timeNeglected;

                    // max time depending on whether previous stroke already was a single stroke or a multistroke
                    int maxTimePastForMultiStrokeSeconds = -1;
                    if (lastAttentionType != null) {
                        switch (lastAttentionType) {
                            case SINGLE_STROKE:
                            case MULTIPLE_STROKES:
                                maxTimePastForMultiStrokeSeconds = lastAttentionType.getAttentionTime();
                                break;
                        }
                    }

                    // check if is valid multistroke
                    if (timeSinceLastAttentionSeconds < maxTimePastForMultiStrokeSeconds) {
                        if (config.kittenAttentionOverlay()) {
                            addAttentionTimer(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_MULTIPLE_STROKES_IN_SECONDS);
                        }
                        lastAttentionType = KittenAttentionType.MULTIPLE_STROKES;
                    } else { // set timer to 18 mins
                        if (config.kittenAttentionOverlay()) {
                            addAttentionTimer(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_SINGLE_STROKE_IN_SECONDS);
                        }
                        lastAttentionType = KittenAttentionType.SINGLE_STROKE;
                    }
                } else { // set timer to 18 mins
                    if (config.kittenAttentionOverlay()) {
                        addAttentionTimer(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_SINGLE_STROKE_IN_SECONDS);
                    }
                    lastAttentionType = KittenAttentionType.SINGLE_STROKE;
                }
                kittenLastAttentionTime = Instant.now();
                timeNeglected = 0;
                break;
            }
            case CHAT_THE_KITTEN_GOBBLES_UP_THE_FISH:
            case CHAT_THE_KITTEN_LAPS_UP_THE_MILK: {
                kittenLastFedTime = Instant.now();
                if (config.kittenHungryOverlay()) {
                    addHungryTimer(HUNGRY_TIME_BEFORE_KITTEN_RUNS_AWAY_IN_SECONDS);
                }
                timeHungry = 0;
                break;
            }
            case CHAT_YOUR_KITTEN_IS_HUNGRY: // 6 minute warning
            {
                if (config.kittenNotifications()) {
                    notifier.notify(message);
                }
                if (config.kittenHungryOverlay()) {
                    addHungryTimer(HUNGRY_FIRST_WARNING_TIME_LEFT_IN_SECONDS);
                }
                kittenLastFedTime = (Instant.now().minus(HUNGRY_TIME_BEFORE_FIRST_WARNING_IN_MINUTES, ChronoUnit.MINUTES));
                break;
            }
            case CHAT_YOUR_KITTEN_IS_VERY_HUNGRY: { // 3 minute warning
                if (config.kittenNotifications()) {
                    notifier.notify(message);
                }
                if (config.kittenHungryOverlay()) {
                    addHungryTimer(HUNGRY_FINAL_WARNING_TIME_LEFT_IN_SECONDS);
                }
                kittenLastFedTime = (Instant.now().minus(HUNGRY_TIME_BEFORE_FINAL_WARNING_IN_MINUTES, ChronoUnit.MINUTES));
                break;
            }
            case CHAT_YOUR_KITTEN_WANTS_ATTENTION: { // 14 minute warning
                if (config.kittenNotifications()) {
                    notifier.notify(message);
                }
                if (config.kittenAttentionOverlay()) {
                    addAttentionTimer(ATTENTION_FIRST_WARNING_TIME_LEFT_IN_SECONDS);
                }
                kittenLastAttentionTime = (Instant.now().minus(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_SINGLE_STROKE_IN_SECONDS, ChronoUnit.MINUTES)); // used minimum time before kitten runs away, can be either 18, 25 or 51 minutes (+14 after warning)
                break;
            }
            case CHAT_YOUR_KITTEN_REALLY_WANTS_ATTENTION: { // 7 minute warning
                if (config.kittenNotifications()) {
                    notifier.notify(message);
                }
                if (config.kittenAttentionOverlay()) {
                    addAttentionTimer(ATTENTION_FINAL_WARNING_TIME_LEFT_IN_SECONDS);
                }
                kittenLastAttentionTime = (Instant.now().minus(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_SINGLE_STROKE_IN_SECONDS, ChronoUnit.MINUTES)); // used minimum time before kitten runs away, can be either 18, 25 or 51 minutes (+14 after warning)
                break;
            }
            case CHAT_YOUR_KITTEN_GOT_LONELY_AND_RAN_OFF:
            case CHAT_THE_CAT_HAS_RUN_AWAY: //shoo away option
            {
                if (config.kittenNotifications()) {
                    notifier.notify(message);
                }
                kittenSpawnedTime = null;
                kittenLastFedTime = null;
                kittenLastAttentionTime = null;
                previousFollowerId = 0;
                config.felineId(0); // in case the new kitten has the same NpcID. We need to track growth progress from the beginning.

                infoBoxManager.removeIf(t -> t instanceof KittenGrowthTimer);
                infoBoxManager.removeIf(t -> t instanceof KittenHungryTimer);
                infoBoxManager.removeIf(t -> t instanceof KittenAttentionTimer);
                followerID = 0;
                break;
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        // Send notification on 1 minute before kitten runs away (attention)
        long timeBeforeNeedingAttention = getTimeBeforeNeedingAttention();
        if (!attentionNotificationSend && timeBeforeNeedingAttention != 0 && timeBeforeNeedingAttention < ATTENTION_TIME_ONE_MINUTE_WARNING_MS) {
            notifier.notify(NOTIFICATION_KITTEN_WILL_RUN_AWAY_IN_1_MINUTE);
            attentionNotificationSend = true;
        }

        // Send notification on 1 minute before kitten runs away (hunger)
        long timeBeforeHungry = getTimeBeforeHungry();
        if (!hungryNotificationSend && timeBeforeHungry != 0 && timeBeforeHungry < HUNGRY_TIME_ONE_MINUTE_WARNING_MS) {
            notifier.notify(NOTIFICATION_KITTEN_WILL_RUN_AWAY_IN_1_MINUTE);
            hungryNotificationSend = true;
        }

        Widget playerDialog = client.getWidget(WidgetID.DIALOG_PLAYER_GROUP_ID, WIDGET_ID_DIALOG_PLAYER_TEXT);

        if (playerDialog != null) {
            String playerText = Text.removeTags(playerDialog.getText()); // remove color and linebreaks
            if (playerText.equals(DIALOG_CAT_BALL_OF_WOOL)) {
                kittenLastAttentionTime = Instant.now();
                if (config.kittenAttentionOverlay()) {
                    addAttentionTimer(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_BALL_OF_WOOL_IN_SECONDS);
                }
                timeNeglected = 0;
                lastAttentionType = KittenAttentionType.BALL_OF_WOOL;
            }
        }

        Widget notificationDialog = client.getWidget(WIDGET_ID_DIALOG_NOTIFICATION_GROUP_ID, WIDGET_ID_DIALOG_NOTIFICATION_TEXT);
        if (notificationDialog != null) {
            String notificationText = Text.removeTags(notificationDialog.getText()); // remove color and linebreaks
            if (notificationText.equals(DIALOG_GERTRUDE_GIVES_YOU_ANOTHER_KITTEN)) { // new kitten
                config.secondsAlive(0);
                config.secondsHungry(0);
                config.secondsNeglected(0);
                config.lastAttentionType(KittenAttentionType.NEW_KITTEN);

                kittenSpawnedTime = Instant.now();
                kittenLastFedTime = Instant.now();
                kittenLastAttentionTime = Instant.now();
                addKittenGrowthBox(TIME_TO_ADULTHOOD_IN_SECONDS);
                addHungryTimer(HUNGRY_TIME_BEFORE_KITTEN_RUNS_AWAY_IN_SECONDS);
                addAttentionTimer(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_MULTIPLE_STROKES_IN_SECONDS);
            } else if (notificationText.equals(DIALOG_CAT_GROWN)) {
                followerKind = FollowerKind.NORMAL_CAT;
                checkForNewFollower();
                infoBoxManager.removeIf(t -> t instanceof KittenAttentionTimer);
                infoBoxManager.removeIf(t -> t instanceof KittenHungryTimer);
            } else if (notificationText.equals(DIALOG_CAT_OVERGROWN)) {
                followerKind = FollowerKind.OVERGROWN_CAT;
                checkForNewFollower();
            } else if (notificationText.startsWith(DIALOG_AFTER_TAKING_A_GOOD_LOOK)) {
                String ageStr = notificationText.substring(DIALOG_AFTER_TAKING_A_GOOD_LOOK.length());
                int end = ageStr.indexOf("And approximate time until");
                ageStr = ageStr.substring(0, end);
                int hoursIndex = ageStr.indexOf("hours");
                if (hoursIndex < 0) {
                    hoursIndex = ageStr.indexOf("hour");
                }

                String hoursStr = "";
                if (hoursIndex > 0) {
                    hoursStr = ageStr.substring(0, hoursIndex);
                    hoursStr = hoursStr.trim();
                }
                int minutesIndex = ageStr.indexOf("minutes");
                if (minutesIndex < 0) {
                    minutesIndex = ageStr.indexOf("minute");
                }

                String minutesStr = "";
                if (minutesIndex > 0) {
                    if (hoursIndex > 0) {
                        minutesStr = ageStr.substring(hoursIndex + "hours".length(), minutesIndex);
                        minutesStr = minutesStr.trim();
                    } else {
                        minutesStr = ageStr.substring(0, minutesIndex);
                        minutesStr = minutesStr.trim();
                    }
                }

                int hours = 0;
                int minutes = 0;
                if (StringUtils.isNotEmpty(hoursStr)) {
                    try {
                        hours = Integer.parseInt(hoursStr);
                    } catch (NumberFormatException ex) {
                        log.debug(ex.getMessage());
                    }
                }

                if (StringUtils.isNotEmpty(minutesStr)) {
                    try {
                        minutes = Integer.parseInt(minutesStr);
                    } catch (NumberFormatException ex) {
                        log.debug(ex.getMessage());
                    }
                }

                Duration timeSinceSpawn = Duration.between(kittenSpawnedTime, Instant.now());
                int secondsSinceSpawn = Math.toIntExact(timeSinceSpawn.getSeconds());
                int age = (hours * 3600) + (minutes * 60);
                timeSpendGrowing = age - secondsSinceSpawn; // substract this because it gets added to the total when calling saveGrowthProgress
                addKittenGrowthBox(TIME_TO_ADULTHOOD_IN_SECONDS - age);
            }
        }

        Widget dialog = client.getWidget(WidgetID.DIALOG_SPRITE_GROUP_ID, 2);
        if (dialog != null) {
            String notificationText = Text.removeTags(dialog.getText());
            if (notificationText.startsWith(DIALOG_HAND_OVER_CAT_CIVILIAN)) {
                kittenSpawnedTime = null;
                kittenLastFedTime = null;
                kittenLastAttentionTime = null;
                previousFollowerId = 0;
                config.felineId(0); // in case the new kitten has the same NpcID. We need to track growth progress from the beginning.

                infoBoxManager.removeIf(t -> t instanceof KittenGrowthTimer);
                infoBoxManager.removeIf(t -> t instanceof KittenHungryTimer);
                infoBoxManager.removeIf(t -> t instanceof KittenAttentionTimer);
                followerID = 0;
            }
        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals("kittenConfig")) {
            return;
        }

        if (event.getKey().equals("kittenInfoBox")) {
            if (event.getNewValue().equals("true")) {
                if (isKitten()) {
                    timeSpendGrowing = config.secondsAlive();
                    Duration timeAlive = Duration.between(kittenSpawnedTime, Instant.now());
                    int secondsAlive = Math.toIntExact(timeAlive.getSeconds());
                    addKittenGrowthBox(TIME_TO_ADULTHOOD_IN_SECONDS - secondsAlive - timeSpendGrowing);
                }
            }
            if (event.getNewValue().equals("false")) {
                if (isKitten()) {
                    infoBoxManager.removeIf(t -> t instanceof KittenGrowthTimer);
                }
            }
        }

        if (event.getKey().equals("catInfoBox")) {
            if (event.getNewValue().equals("true")) {
                if (isCat()) {
                    timeSpendGrowing = config.secondsAlive();
                    Duration timeAlive = Duration.between(catSpawnedTime, Instant.now());
                    int secondsAlive = Math.toIntExact(timeAlive.getSeconds());
                    addKittenGrowthBox(TIME_TILL_OVERGROWN_IN_SECONDS + TIME_TO_ADULTHOOD_IN_SECONDS - secondsAlive - timeSpendGrowing);
                }
            }
            if (event.getNewValue().equals("false")) {
                if (isCat()) {
                    infoBoxManager.removeIf(t -> t instanceof KittenGrowthTimer);
                }
            }
        }

        if (event.getKey().equals("kittenAttentionBox")) {
            if (event.getNewValue().equals("true")) {
                if (kittenLastAttentionTime != null) {
                    Duration timeAttention = Duration.between(kittenLastAttentionTime, Instant.now());
                    int secondsAttention = Math.toIntExact(timeAttention.getSeconds());
                    addAttentionTimer(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_SINGLE_STROKE_IN_SECONDS - secondsAttention);
                }
                if (kittenLastAttentionTime == null) {
                    timeNeglected = config.secondsNeglected();
                    Duration timeSinceSpawn = Duration.between(kittenSpawnedTime, Instant.now());
                    int secondsSinceSpawn = Math.toIntExact(timeSinceSpawn.getSeconds());
                    addAttentionTimer(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_SINGLE_STROKE_IN_SECONDS - timeNeglected - secondsSinceSpawn);
                }
            }
            if (event.getNewValue().equals("false")) {
                infoBoxManager.removeIf(t -> t instanceof KittenAttentionTimer);
            }
        }

        if (event.getKey().equals("kittenHungryBox")) {
            if (event.getNewValue().equals("true")) {
                if (kittenLastFedTime != null) {
                    Duration timeHungry = Duration.between(kittenLastFedTime, Instant.now());
                    int secondsHungry = Math.toIntExact(timeHungry.getSeconds());
                    addHungryTimer(HUNGRY_TIME_BEFORE_KITTEN_RUNS_AWAY_IN_SECONDS - secondsHungry);
                }
                if (kittenLastFedTime == null) {
                    timeHungry = config.secondsHungry();
                    Duration timeSinceSpawn = Duration.between(kittenSpawnedTime, Instant.now());
                    int secondsSinceSpawn = Math.toIntExact(timeSinceSpawn.getSeconds());
                    addHungryTimer(HUNGRY_TIME_BEFORE_KITTEN_RUNS_AWAY_IN_SECONDS - timeHungry - secondsSinceSpawn);
                }
            }
            if (event.getNewValue().equals("false")) {
                infoBoxManager.removeIf(t -> t instanceof KittenHungryTimer);
            }
        }
    }

    /*
    // 2023-01-24
    // Commenting this out for now until a better assessment can be made if it is needed or not.
    // Personal testing has shown that time does not need to be added back after Widgets (main ones tested was
    //   dialog boxes and using a bank interface). Not sure if cutscenes count as a widget and if they affect it.
    @Subscribe
    private void onWidgetLoaded(WidgetLoaded ev) {
        openedWidgets.put(ev.getGroupId(), Instant.now());
    }

    @Subscribe
    private void onWidgetClosed(WidgetClosed ev) {
        Instant openedWidgetTime = openedWidgets.get(ev.getGroupId());
        if (openedWidgetTime != null) {
            if (ev.getModalMode() != WidgetModalMode.NON_MODAL) {
                addDurationToTimers(Duration.between(openedWidgetTime, Instant.now()));
            }
            openedWidgets.remove(ev.getGroupId());
        }
    }
    */

    private void addDurationToTimers(Duration duration) {
        if (duration == null) {
            return;
        }

        Duration timerDuration;
        if (growthTimer != null) {
            timerDuration = growthTimer.getDuration();
            if (growthTimer != null && timerDuration != null) {
                growthTimer.setDuration(timerDuration.plus(duration));
            }
        }

        if (kittenAttentionTimer != null) {
            timerDuration = kittenAttentionTimer.getDuration();
            if (kittenAttentionTimer != null && timerDuration != null) {
                kittenAttentionTimer.setDuration(timerDuration.plus(duration));
            }
        }

        if (kittenHungryTimer != null) {
            timerDuration = kittenHungryTimer.getDuration();
            if (kittenHungryTimer != null && timerDuration != null) {
                kittenHungryTimer.setDuration(timerDuration.plus(duration));
            }
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState state = event.getGameState();
        switch (state) {
            case LOGGING_IN:
            case HOPPING:
            case CONNECTION_LOST: // CHECK: this may be a condition causing the timer not to stop when the window is closed
                ready = true;
                break;
            case LOGGED_IN:
                if (ready) {
                    ready = false; // Currently not used for anything?
                }
                break;
            case LOGIN_SCREEN:
                byeFollower();
                break;
        }
    }

    @Inject
    public boolean playerHasFollower() {
        return (client.getVarpValue(VAR_PLAYER_FOLLOWER)) > 0;
    }

    public boolean isKitten() {
        return followerKind.equals(FollowerKind.KITTEN);
    }

    public Long getTimeUntilFullyGrown() {
        if (growthTimer == null) {
            return 0L;
        }
        if (isKitten()) {
            // `cull` returns true if timer is less than or equal to zero
            // This will keep the timer from going negative
            if (growthTimer.cull()) {
                return 0L;
            } else {
                return Math.abs(growthTimer.getEndTime().until(Instant.now(), ChronoUnit.MILLIS));
            }
        }
        return 0L;
    }

    public boolean isCat() {
        return followerKind.equals(FollowerKind.NORMAL_CAT);
    }

    public boolean isOverGrown() {
        return followerKind.equals(FollowerKind.OVERGROWN_CAT);
    }

    public Long getTimeUntilOvergrown() {
        if (growthTimer == null) {
            return 0L;
        }
        long ret = Math.abs(growthTimer.getEndTime().until(Instant.now(), ChronoUnit.MILLIS));
        if (isCat()) {
            return ret;
        }
        return ret + TIME_TILL_OVERGROWN_IN_SECONDS;
    }

    public Long getTimeBeforeHungry() {
        if (kittenHungryTimer == null) {
            return 0L;
        }
        if (isKitten()) {
            // `cull` returns true if timer is less than or equal to zero
            // This will keep the timer from going negative
            if (kittenHungryTimer.cull()) {
                return 0L;
            } else {
                return Math.abs(kittenHungryTimer.getEndTime().until(Instant.now(), ChronoUnit.MILLIS));
            }
        }
        return 0L;
    }

    public Long getTimeBeforeNeedingAttention() {
        if (kittenAttentionTimer == null) {
            return 0L;
        }
        if (isKitten()) {
            // `cull` returns true if timer is less than or equal to zero
            // This will keep the timer from going negative
            if (kittenAttentionTimer.cull()) {
                return 0L;
            } else {
                return Math.abs(kittenAttentionTimer.getEndTime().until(Instant.now(), ChronoUnit.MILLIS));
            }
        } else {
            return 0L;
        }
    }
}
