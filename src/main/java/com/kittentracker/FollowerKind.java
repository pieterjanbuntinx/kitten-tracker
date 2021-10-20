package com.kittentracker;

import net.runelite.api.NpcID;

public enum FollowerKind {
    NORMAL_CAT, LAZY_CAT, WILY_CAT, KITTEN, OVERGROWN_CAT, NON_FELINE;

    public static FollowerKind getFromFollowerId(int followerId) {
        if (followerId >= NpcID.CAT_1619 && followerId <= NpcID.HELLCAT) {
            return NORMAL_CAT;
        } else if (followerId >= NpcID.LAZY_CAT && followerId <= NpcID.LAZY_HELLCAT) {
            return LAZY_CAT;
        } else if (followerId >= NpcID.WILY_CAT && followerId <= NpcID.WILY_HELLCAT) {
            return WILY_CAT;
        } else if (followerId >= NpcID.KITTEN_5591 && followerId <= NpcID.HELLKITTEN) {
            return KITTEN;
        } else if (followerId >= NpcID.OVERGROWN_CAT && followerId <= NpcID.OVERGROWN_HELLCAT) {
            return OVERGROWN_CAT;
        } else {
            return NON_FELINE;
        }
    }
}
