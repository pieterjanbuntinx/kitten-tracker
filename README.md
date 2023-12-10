# Kitten Tracker RuneLite plugin

This plugin allows you to track the age of your pet kitten and shows how long before your kitten will leave you because of a lack of attention or when it is starving. It also shows notifications to remind you to feed your cat or give it attention. Clicking Guess Age updates the growth timer to the correct time if there is any offset.

The plugin abides by the following kitten mechanics:
- Kitten grows one growth tick every 90s.
    - Upon reaching 90s, this growth tick does not progress if the player is in an interface (dialog, bank menu, etc.).
        - Note: This does not include all interfaces.  If you can interact with the game while the interface is open (e.g. settings/world map is open, and you can still run around/skill/etc.), then that interface will not stall kitten growth.
        - It will progress immediately after exiting that interface, as long as the kitten has a chance to grow (ex: growth does not progress if player AFKs to logout in a bank interface)
        - If you are only in an interface during a different time of that tick progress (ex: banking during seconds 40-70 of the 90s tick), the kitten's growth will be unaffected by you being in that interface, as long as you're not in an interface at 90s.
    - The kitten's progress within that 90s tick gets reset each time it is picked up, or when the player logs out/hops worlds.
- Kitten has overhead text each and every time it grows a growth tick, so as our as the timer ticks down, it waits for that overhead text before continuing to the next growth tick.  This is a much easier "catch-all" than checking for every kind of interface and hoping we don't miss any.
  - If you are at a very crowded place with many players/pets/etc., sometimes your kitten will not render.  This prevents the plugin from seeing the overhead text.  In this case, we will instead check for some common interfaces that would prevent growth.  Similarly, you can't see your kitten's overhead text if you teleport into a new area on the same tick.  
    - This is not currently practical for the primary way to check growth, however, as there are likely hundreds of interfaces that pause growth (and hundreds more that do not pause growth) so it would be difficult to check for all of them.
- Kitten can only request hunger/attention at the time when a growth tick progresses.

Kitten's need for attention:
- Kitten's attention requests (and run away time) are *supposed* to be 4.5 mins apart, but these can be delayed by a hunger notification which seems to always take precedence.  (As hunger notifications always take precedence, hunger notifications are always consistent no matter what.)
  - Attention notifications can also be pushed back by upcoming hunger notifications, and not even ones that would coincide with the attention request time...  For example, see this chart of hunger and attention notifications:  https://i.imgur.com/6PFRJqB.png  You can easily replicate these results yourself as well.
    - So if you keep your cat well-fed and never get notified of hunger, these notifications are consistently 4.5 mins apart.
      - Your cat would also request attention every 22.5 minutes if stroked twice, or every 51 minutes if given a ball of wool.
- Attention given for 2 or more strokes is always consistent at 22.5 mins until requesting attention again (barring hunger notifications delaying it).
- Single-stroke mechanics add a variable time to the kitten requesting attention again.  It seems as if the more lonely your cat is (closer to running away), the less time a single stroke will add to its attention timer.  Single strokes can last as little at 7.5m before requesting attention again, but will last longer if you pet it before it's close to running away.
    - A few data points I've gathered (ignoring hunger mechanics, as the kitten was well-fed during these times):
        - Kitten is 1.5m from running away.
            - Single stroke now takes 7.5m to request attention again (16.5m from running away)
        - Kitten is 30m from running away (6m after double stroke).
            - Single stroke now takes 21m to request attention again (30m from running away)
        - Kitten is 31.5m from running away (4.5m after double stroke).
            - Single stroke gives full amount of 22.5m to request attention again.
        - I believe this can all be interpolated linearly, with it maxing out at 22.5 minutes until the next attention request.  Some testing (not very extensive) has shown this to be true so far.
            - Formula would be:  Time until run away after stroked = Time until run away just before stroked + 15 mins, maxing out at 22.5 mins until requesting attention again.
- IMPORTANT: many of these notes regarding the kitten's attention timer are not yet incorporated into the plugin, as it would require further testing.  I just wanted to include them here so that in case someone else wants to take my research/notes and go further with it.
  - The current way the plugin behaves as is follows:
    - Double stroke adds 36 minutes until the kitten runs away.  (Technically should be 31.5 mins, but more often than not hunger notifications will delay it to closer to 36m.)
    - Single stroke adds 24 minutes until the kitten runs away (as mentioned above, it's actually variable).
    - Ball of wool adds 60 minutes until the kitten runs away (accurate if hunger notifications do not interfere).
    - Kitten warnings for attention are 9 minutes (first warning) and 4.5 minutes (second warning) until the kitten runs away.