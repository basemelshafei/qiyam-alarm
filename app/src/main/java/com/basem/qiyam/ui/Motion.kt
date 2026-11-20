package com.basem.qiyam.ui

/**
 * Live motion on screen: the ticking countdown and twinkling stars. Screen tests turn it off,
 * because a screen that never stops moving never "settles" for a test to check it.
 */
object Motion {
    @Volatile var live: Boolean = true
}
