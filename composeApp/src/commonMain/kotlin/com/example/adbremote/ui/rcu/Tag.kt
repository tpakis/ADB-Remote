package com.example.adbremote.ui.rcu

/**
 * Enum representing all remote control button tags.
 * Each tag maps to an Android keycode for ADB input.
 */
enum class Tag(val keycode: Int) {
    // Number keys
    NUMBER_0(7),    // KEYCODE_0
    NUMBER_1(8),    // KEYCODE_1
    NUMBER_2(9),    // KEYCODE_2
    NUMBER_3(10),   // KEYCODE_3
    NUMBER_4(11),   // KEYCODE_4
    NUMBER_5(12),   // KEYCODE_5
    NUMBER_6(13),   // KEYCODE_6
    NUMBER_7(14),   // KEYCODE_7
    NUMBER_8(15),   // KEYCODE_8
    NUMBER_9(16),   // KEYCODE_9

    // D-pad navigation
    DPAD_UP(19),        // KEYCODE_DPAD_UP
    DPAD_DOWN(20),      // KEYCODE_DPAD_DOWN
    DPAD_LEFT(21),      // KEYCODE_DPAD_LEFT
    DPAD_RIGHT(22),     // KEYCODE_DPAD_RIGHT
    DPAD_CENTER(23),    // KEYCODE_DPAD_CENTER

    // Volume controls
    VOLUME_UP(24),      // KEYCODE_VOLUME_UP
    VOLUME_DOWN(25),    // KEYCODE_VOLUME_DOWN
    VOLUME_MUTE(164),   // KEYCODE_VOLUME_MUTE

    // Channel controls
    CHANNEL_UP(166),    // KEYCODE_CHANNEL_UP
    CHANNEL_DOWN(167),  // KEYCODE_CHANNEL_DOWN

    // Main controls
    POWER(26),          // KEYCODE_POWER
    HOME(3),            // KEYCODE_HOME
    BACK(4),            // KEYCODE_BACK
    MENU(82),           // KEYCODE_MENU
    SETTINGS(176),      // KEYCODE_SETTINGS

    // Media playback
    PLAY(126),          // KEYCODE_MEDIA_PLAY
    PAUSE(127),         // KEYCODE_MEDIA_PAUSE
    PLAY_PAUSE(85),     // KEYCODE_MEDIA_PLAY_PAUSE
    STOP(86),           // KEYCODE_MEDIA_STOP
    REWIND(89),         // KEYCODE_MEDIA_REWIND
    FAST_FORWARD(90),   // KEYCODE_MEDIA_FAST_FORWARD
    PREVIOUS(88),       // KEYCODE_MEDIA_PREVIOUS
    NEXT(87),           // KEYCODE_MEDIA_NEXT
    RECORD(130),        // KEYCODE_MEDIA_RECORD

    // TV/Android TV specific
    GUIDE(172),         // KEYCODE_GUIDE
    INFO(165),          // KEYCODE_INFO
    TV(170),            // KEYCODE_TV
    DVR(173),           // KEYCODE_DVR
    CAPTIONS(175),      // KEYCODE_CAPTIONS

    // Color keys (HbbTV)
    RED(183),           // KEYCODE_PROG_RED
    GREEN(184),         // KEYCODE_PROG_GREEN
    YELLOW(185),        // KEYCODE_PROG_YELLOW
    BLUE(186),          // KEYCODE_PROG_BLUE

    // Apps and search
    APPS(284),          // KEYCODE_ALL_APPS
    SEARCH(84),         // KEYCODE_SEARCH
    ASSISTANT(231),     // KEYCODE_VOICE_ASSIST

    // Additional
    TELETEXT(233),      // KEYCODE_TV_TELETEXT
    RECORDINGS(173),    // KEYCODE_DVR (same as DVR, for recordings)

    // External apps (custom intents)
    NETFLIX(0),         // Custom - launch Netflix
    YOUTUBE(0),         // Custom - launch YouTube
    PRIME_VIDEO(0),     // Custom - launch Prime Video
    DISNEY_PLUS(0);     // Custom - launch Disney+

    /**
     * Returns the ADB shell command to simulate this key press.
     */
    fun toAdbCommand(): String {
        return when (this) {
            NETFLIX -> "am start -n com.netflix.ninja/.MainActivity"
            YOUTUBE -> "am start -n com.google.android.youtube.tv/com.google.android.apps.youtube.tv.activity.ShellActivity"
            PRIME_VIDEO -> "am start -n com.amazon.amazonvideo.livingroom/com.amazon.ignition.IgnitionActivity"
            DISNEY_PLUS -> "am start -n com.disney.disneyplus/com.bamtechmedia.domern.main.MainActivity"
            else -> "input keyevent $keycode"
        }
    }
}
