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
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
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

@Slf4j
@PluginDescriptor(
        name = "Kitten Tracker"
)
public class KittenPlugin extends Plugin {
    private static final int VAR_PLAYER_FOLLOWER = 447;
    private static final int WIDGET_ID_DIALOG_NOTIFICATION_GROUP_ID = 229;
    private static final int WIDGET_ID_DIALOG_PLAYER_TEXT = 5;
    private static final int WIDGET_ID_DIALOG_NOTIFICATION_TEXT = 1;

    private static final String DIALOG_CAT_STROKE = "That cat sure loves to be stroked.";
    private static final String DIALOG_CAT_BALL_OF_WOOL = "That kitten loves to play with that ball of wool. I think itis its favourite.";
    private static final String DIALOG_CAT_GROWN = "Your kitten has grown into a healthy cat that can hunt for itself.";
    private static final String DIALOG_CAT_OVERGROWN = "Your cat has grown into a mighty feline, but it will no longer be able to chase vermin.";
    private static final String DIALOG_AFTER_TAKING_A_GOOD_LOOK = "After taking a good look at your kitten you guess that its age is: ";
    private static final String DIALOG_HAND_OVER_CAT_CIVILIAN = "You hand over the cat.You are given";

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
    private Instant kittenSpawned;
    private Instant catSpawned;
    private Instant kittenFed;
    private Instant kittenAttention;
    private int timeSpendGrowing = 0;
    private int timeNeglected = 0;
    private int timeHungry = 0;
    private int followerID = 0;
    private int previousFeline = 0;
    private KittenAttentionType lastAttentionType = KittenAttentionType.NEW_KITTEN;

    private Timer kittenAttentionTimer, growthTimer, kittenHungryTimer;

    private boolean cat = false; // npcId 1619-1625
    private boolean lazycat = false; // npcId 1626-1632
    private boolean wilycat = false; // npcId 5584-5590
    private boolean kitten = false; // npcId 5591-5597
    private boolean overgrown = false; // npcId 5598-5604
    private boolean nonFeline = false;

    private boolean timersPaused = false;
    private boolean attentionNotificationSend = false;
    private boolean hungryNotificationSend = false;
    private long attentionTimeLeft, growthTimeLeft, hungryTimeLeft;

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
        previousFeline = config.felineId();
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
        if (playerHasFollower()) {
            if (followerID == 0) { // player got a new follower
                getFollowerID();
            } else {
                /*
                 * Follower id has changed. Example: cat changed into hell cat
                 * Will not trigger with change kitten -> cat -> overgrown cat
                 * The notification dialog event (onGameTick) handles it in this case
                 */
                int followerVarPlayerValue = client.getVarpValue(VAR_PLAYER_FOLLOWER);

                int followerIdTemp = followerVarPlayerValue >> 16; // followerID is the first 2 bytes

                // if follower is a cat, change followerID
                if ((followerID >= NpcID.CAT_1619 && followerID <= NpcID.HELLCAT)
                        || (followerID >= NpcID.LAZY_CAT && followerID <= NpcID.LAZY_HELLCAT)
                        || (followerID >= NpcID.WILY_CAT && followerID <= NpcID.WILY_HELLCAT)
                        || (followerID >= NpcID.KITTEN_5591 && followerID <= NpcID.HELLKITTEN)
                        || (followerID >= NpcID.OVERGROWN_CAT && followerID <= NpcID.OVERGROWN_HELLCAT)) {
                    followerID = followerIdTemp;
                }
            }
        }

        if (!playerHasFollower() && followerID != 0) // player lost it's follower
        {
            byeFollower();
        }

    }

    private void getFollowerID() {
        int followerVarPlayerValue = client.getVarpValue(VAR_PLAYER_FOLLOWER);

        // followerID is the first 2 bytes
        followerID = followerVarPlayerValue >> 16;
        if (followerID != 0) //Varbit needs to fill up first after logging in
        {
            previousFeline = config.felineId();
            newFollower();
        }

    }

    private void newFollower() {
        cat = false;
        lazycat = false;
        wilycat = false;
        kitten = false;
        overgrown = false;
        nonFeline = false;
        timersPaused = false;
        attentionNotificationSend = false;
        hungryNotificationSend = false;

        if (followerID >= NpcID.CAT_1619 && followerID <= NpcID.HELLCAT) {
            cat = true;
        } else if (followerID >= NpcID.LAZY_CAT && followerID <= NpcID.LAZY_HELLCAT) {
            lazycat = true;
        } else if (followerID >= NpcID.WILY_CAT && followerID <= NpcID.WILY_HELLCAT) {
            wilycat = true;
        } else if (followerID >= NpcID.KITTEN_5591 && followerID <= NpcID.HELLKITTEN) {
            kitten = true;
        } else if (followerID >= NpcID.OVERGROWN_CAT && followerID <= NpcID.OVERGROWN_HELLCAT) {
            overgrown = true;
        }

        if (!cat && !lazycat && !wilycat && !kitten && !overgrown) {
            nonFeline = true;
            return; // The NPC that spawned is not a feline.
        }
        if (kitten) {
            if (followerID == previousFeline) // The same kitten is back!
            {
                timeSpendGrowing = config.secondsAlive();
                timeNeglected = config.secondsNeglected();
                timeHungry = config.secondsHungry();
                kittenSpawned = Instant.now();
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

                kittenSpawned = Instant.now();
                kittenFed = Instant.now();
                kittenAttention = Instant.now();
                addKittenGrowthBox(TIME_TO_ADULTHOOD_IN_SECONDS);
                addHungryTimer(HUNGRY_TIME_BEFORE_KITTEN_RUNS_AWAY_IN_SECONDS);
                addAttentionTimer(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_MULTIPLE_STROKES_IN_SECONDS);
            }
        }
        if (cat) {
            if (followerID == previousFeline) // The same cat is back!
            {
                timeSpendGrowing = config.secondsAlive();
                addKittenGrowthBox((TIME_TO_ADULTHOOD_IN_SECONDS + TIME_TILL_OVERGROWN_IN_SECONDS - timeSpendGrowing));
            } else // new cat, new timer
            {
                catSpawned = Instant.now();
                addKittenGrowthBox(TIME_TILL_OVERGROWN_IN_SECONDS);
            }
        }
    }

    private void byeFollower() {
        if (kitten) {
            saveGrowthProgress();
            kittenSpawned = null;
            kittenFed = null;
            kittenAttention = null;
        }

        if (cat) {
            saveGrowthProgress();
            catSpawned = null;
        }

        infoBoxManager.removeIf(t -> t instanceof KittenGrowthTimer);
        infoBoxManager.removeIf(t -> t instanceof KittenHungryTimer);
        infoBoxManager.removeIf(t -> t instanceof KittenAttentionTimer);

        followerID = 0;
        cat = false;
        lazycat = false;
        wilycat = false;
        kitten = false;
        overgrown = false;
        nonFeline = false;
    }

    private void saveGrowthProgress() {
        if (kitten) {
            config.felineId(followerID);

            Duration timeAlive = Duration.between(kittenSpawned, Instant.now());
            int secondsAlive = Math.toIntExact(timeAlive.getSeconds());
            config.secondsAlive(timeSpendGrowing + secondsAlive);

            if (kittenFed != null) {
                Duration timeFed = Duration.between(kittenFed, Instant.now());
                int secondsFed = Math.toIntExact(timeFed.getSeconds());
                config.secondsHungry(secondsFed);
            } else {
                // kitten was not fed, so we add the time it has been out to the time it was already hungry for
                Duration timeSinceSpawn = Duration.between(kittenSpawned, Instant.now());
                int secondsSinceSpawn = Math.toIntExact(timeSinceSpawn.getSeconds());
                config.secondsHungry(timeHungry + secondsSinceSpawn);
            }

            if (kittenAttention != null) {
                Duration timeAttention = Duration.between(kittenAttention, Instant.now());
                int secondsAttention = Math.toIntExact(timeAttention.getSeconds());
                config.secondsNeglected(secondsAttention);
            } else {
                // kitten was not paid attention, so we add the time it has been out to the time it was already neglected for
                Duration timeSinceSpawn = Duration.between(kittenSpawned, Instant.now());
                int secondsSinceSpawn = Math.toIntExact(timeSinceSpawn.getSeconds());
                config.secondsNeglected(timeNeglected + secondsSinceSpawn);
            }
            config.lastAttentionType(lastAttentionType);
        }

        if (cat) {
            config.felineId(followerID);
            Duration timeAlive = Duration.between(catSpawned, Instant.now());
            int secondsAlive = Math.toIntExact(timeAlive.getSeconds());
            config.secondsAlive(timeSpendGrowing + secondsAlive);
            catSpawned = null;
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

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }
        String message = Text.removeTags(event.getMessage());
        switch (message) {
            case CHAT_STROKE_CAT: {
                if (kittenAttention != null) { // if kitten has had attention within the time since spawn
                    long timeSinceLastAttentionSeconds = Duration.between(kittenAttention, Instant.now()).toMillis() / 1000 + timeNeglected;

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
                kittenAttention = Instant.now();
                timeNeglected = 0;
                break;
            }
            case CHAT_THE_KITTEN_GOBBLES_UP_THE_FISH:
            case CHAT_THE_KITTEN_LAPS_UP_THE_MILK: {
                kittenFed = Instant.now();
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
                kittenFed = (Instant.now().minus(HUNGRY_TIME_BEFORE_FIRST_WARNING_IN_MINUTES, ChronoUnit.MINUTES));
                break;
            }
            case CHAT_YOUR_KITTEN_IS_VERY_HUNGRY: {
                if (config.kittenNotifications()) {
                    notifier.notify(message);
                }
                if (config.kittenHungryOverlay()) {
                    addHungryTimer(HUNGRY_FINAL_WARNING_TIME_LEFT_IN_SECONDS);
                }
                kittenFed = (Instant.now().minus(HUNGRY_TIME_BEFORE_FINAL_WARNING_IN_MINUTES, ChronoUnit.MINUTES));
                break;
            }
            case CHAT_YOUR_KITTEN_WANTS_ATTENTION: {
                if (config.kittenNotifications()) {
                    notifier.notify(message);
                }
                if (config.kittenAttentionOverlay()) {
                    addAttentionTimer(ATTENTION_FIRST_WARNING_TIME_LEFT_IN_SECONDS);
                }
                kittenAttention = (Instant.now().minus(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_SINGLE_STROKE_IN_SECONDS, ChronoUnit.MINUTES)); // used minimum time before kitten runs away, can be either 18, 25 or 51 minutes (+14 after warning)
                break;
            }
            case CHAT_YOUR_KITTEN_REALLY_WANTS_ATTENTION: {
                if (config.kittenNotifications()) {
                    notifier.notify(message);
                }
                if (config.kittenAttentionOverlay()) {
                    addAttentionTimer(ATTENTION_FINAL_WARNING_TIME_LEFT_IN_SECONDS);
                }
                kittenAttention = (Instant.now().minus(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_SINGLE_STROKE_IN_SECONDS, ChronoUnit.MINUTES)); // used minimum time before kitten runs away, can be either 18, 25 or 51 minutes (+14 after warning)
                break;
            }
            case CHAT_YOUR_KITTEN_GOT_LONELY_AND_RAN_OFF:
            case CHAT_THE_CAT_HAS_RUN_AWAY: //shoo away option
            {
                if (config.kittenNotifications()) {
                    notifier.notify(message);
                }
                kittenSpawned = null;
                kittenFed = null;
                kittenAttention = null;
                previousFeline = 0;
                config.felineId(0); // in case the new kitten has the same NpcID. We need to track growth progress from the beginning.

                infoBoxManager.removeIf(t -> t instanceof KittenGrowthTimer);
                infoBoxManager.removeIf(t -> t instanceof KittenHungryTimer);
                infoBoxManager.removeIf(t -> t instanceof KittenAttentionTimer);
                followerID = 0;
                cat = false;
                lazycat = false;
                wilycat = false;
                kitten = false;
                overgrown = false;
                nonFeline = false;
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

        boolean wake = true;
        if (playerDialog != null) {
            String playerText = Text.removeTags(playerDialog.getText()); // remove color and linebreaks
            if (playerText.equals(DIALOG_CAT_BALL_OF_WOOL)) {
                kittenAttention = Instant.now();
                if (config.kittenAttentionOverlay()) {
                    addAttentionTimer(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_BALL_OF_WOOL_IN_SECONDS);
                }
                timeNeglected = 0;
                lastAttentionType = KittenAttentionType.BALL_OF_WOOL;

                wake = false;
            }
        }

        Widget notificationDialog = client.getWidget(WIDGET_ID_DIALOG_NOTIFICATION_GROUP_ID, WIDGET_ID_DIALOG_NOTIFICATION_TEXT);
        if (notificationDialog != null) {
            String notificationText = Text.removeTags(notificationDialog.getText()); // remove color and linebreaks
            if (notificationText.equals(DIALOG_CAT_GROWN)) {
                kitten = false;
                getFollowerID();
                infoBoxManager.removeIf(t -> t instanceof KittenAttentionTimer);
                infoBoxManager.removeIf(t -> t instanceof KittenHungryTimer);
                wake = false;
            } else if (notificationText.equals(DIALOG_CAT_OVERGROWN)) {
                cat = false;
                getFollowerID();
                wake = false;
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

                Duration timeSinceSpawn = Duration.between(kittenSpawned, Instant.now());
                int secondsSinceSpawn = Math.toIntExact(timeSinceSpawn.getSeconds());
                int age = (hours * 3600) + (minutes * 60);
                timeSpendGrowing = age - secondsSinceSpawn; // substract this because it gets added to the total when calling saveGrowthProgress

                if (kitten) {
                    addKittenGrowthBox(TIME_TO_ADULTHOOD_IN_SECONDS - age);
                }
                if (cat) {
                    addKittenGrowthBox((TIME_TO_ADULTHOOD_IN_SECONDS + TIME_TILL_OVERGROWN_IN_SECONDS - age));
                }
                wake = false;
            }
        }

        Widget dialog = client.getWidget(WidgetID.DIALOG_SPRITE_GROUP_ID, 2);
        if (dialog != null) {
            String notificationText = Text.removeTags(dialog.getText());
            if (notificationText.startsWith(DIALOG_HAND_OVER_CAT_CIVILIAN)) {
                kittenSpawned = null;
                kittenFed = null;
                kittenAttention = null;
                previousFeline = 0;
                config.felineId(0); // in case the new kitten has the same NpcID. We need to track growth progress from the beginning.

                infoBoxManager.removeIf(t -> t instanceof KittenGrowthTimer);
                infoBoxManager.removeIf(t -> t instanceof KittenHungryTimer);
                infoBoxManager.removeIf(t -> t instanceof KittenAttentionTimer);
                followerID = 0;
                cat = false;
                lazycat = false;
                wilycat = false;
                kitten = false;
                overgrown = false;
                nonFeline = false;
                return;
            }
        }

        if (wake) {
            if (timersPaused) {
                timersPaused = false;
                addKittenGrowthBox((int) (growthTimeLeft / 1000));
                addHungryTimer((int) (hungryTimeLeft / 1000));
                addAttentionTimer((int) (attentionTimeLeft / 1000));
            }
        } else { // pause timers if a dialog window or notification window concerning your cat is open
            if (!timersPaused) {
                timersPaused = true;
                attentionTimeLeft = Math.abs(kittenAttentionTimer.getEndTime().until(Instant.now(), ChronoUnit.MILLIS));
                growthTimeLeft = Math.abs(growthTimer.getEndTime().until(Instant.now(), ChronoUnit.MILLIS));
                hungryTimeLeft = Math.abs(kittenHungryTimer.getEndTime().until(Instant.now(), ChronoUnit.MILLIS));
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
                if (kitten) {
                    timeSpendGrowing = config.secondsAlive();
                    Duration timeAlive = Duration.between(kittenSpawned, Instant.now());
                    int secondsAlive = Math.toIntExact(timeAlive.getSeconds());
                    addKittenGrowthBox(TIME_TO_ADULTHOOD_IN_SECONDS - secondsAlive - timeSpendGrowing);
                }
            }
            if (event.getNewValue().equals("false")) {
                if (kitten) {
                    infoBoxManager.removeIf(t -> t instanceof KittenGrowthTimer);
                }
            }
        }

        if (event.getKey().equals("catInfoBox")) {
            if (event.getNewValue().equals("true")) {
                if (cat) {
                    timeSpendGrowing = config.secondsAlive();
                    Duration timeAlive = Duration.between(catSpawned, Instant.now());
                    int secondsAlive = Math.toIntExact(timeAlive.getSeconds());
                    addKittenGrowthBox(TIME_TILL_OVERGROWN_IN_SECONDS + TIME_TO_ADULTHOOD_IN_SECONDS - secondsAlive - timeSpendGrowing);
                }
            }
            if (event.getNewValue().equals("false")) {
                if (cat) {
                    infoBoxManager.removeIf(t -> t instanceof KittenGrowthTimer);
                }
            }
        }

        if (event.getKey().equals("kittenAttentionBox")) {
            if (event.getNewValue().equals("true")) {
                if (kittenAttention != null) {
                    Duration timeAttention = Duration.between(kittenAttention, Instant.now());
                    int secondsAttention = Math.toIntExact(timeAttention.getSeconds());
                    addAttentionTimer(ATTENTION_TIME_BEFORE_KITTEN_RUNS_AWAY_SINGLE_STROKE_IN_SECONDS - secondsAttention);
                }
                if (kittenAttention == null) {
                    timeNeglected = config.secondsNeglected();
                    Duration timeSinceSpawn = Duration.between(kittenSpawned, Instant.now());
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
                if (kittenFed != null) {
                    Duration timeHungry = Duration.between(kittenFed, Instant.now());
                    int secondsHungry = Math.toIntExact(timeHungry.getSeconds());
                    addHungryTimer(HUNGRY_TIME_BEFORE_KITTEN_RUNS_AWAY_IN_SECONDS - secondsHungry);
                }
                if (kittenFed == null) {
                    timeHungry = config.secondsHungry();
                    Duration timeSinceSpawn = Duration.between(kittenSpawned, Instant.now());
                    int secondsSinceSpawn = Math.toIntExact(timeSinceSpawn.getSeconds());
                    addHungryTimer(HUNGRY_TIME_BEFORE_KITTEN_RUNS_AWAY_IN_SECONDS - timeHungry - secondsSinceSpawn);
                }
            }
            if (event.getNewValue().equals("false")) {
                infoBoxManager.removeIf(t -> t instanceof KittenHungryTimer);
            }
        }

    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState state = event.getGameState();
        switch (state) {
            case LOGGING_IN:
            case HOPPING:
            case CONNECTION_LOST:
                ready = true;
                break;
            case LOGGED_IN:
                if (ready) {
                    ready = false;
                }
                break;
            case LOGIN_SCREEN:
                byeFollower();
                break;
        }
    }

    @Inject
    public boolean playerHasFollower() {
        return (client.getVarpValue(VAR_PLAYER_FOLLOWER)) != -1;
    }

    public boolean isKitten() {
        return kitten;
    }

    public Long getTimeUntilFullyGrown() {
        if (growthTimer == null) {
            return 0L;
        }
        if (isKitten()) {
            return Math.abs(growthTimer.getEndTime().until(Instant.now(), ChronoUnit.MILLIS));
        }
        return 0L;
    }

    public boolean isCat() {
        return cat;
    }

    public boolean isOverGrown() {
        return overgrown;
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
            return Math.abs(kittenHungryTimer.getEndTime().until(Instant.now(), ChronoUnit.MILLIS));
        }
        return 0L;
    }

    public Long getTimeBeforeNeedingAttention() {
        if (kittenAttentionTimer == null) {
            return 0L;
        }
        if (isKitten()) {
            return Math.abs(kittenAttentionTimer.getEndTime().until(Instant.now(), ChronoUnit.MILLIS));
        } else {
            return 0L;
        }
    }
}
