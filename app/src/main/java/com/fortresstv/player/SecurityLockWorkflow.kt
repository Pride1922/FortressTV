package com.fortresstv.player

import com.fortresstv.player.profile.HouseholdProfile
import com.fortresstv.player.profile.ProfileRepository

/**
 * Workflow owner for adult content authorization, PIN verification,
 * and background inactivity lock session management.
 */
internal class SecurityLockWorkflow(
    private val profileRepository: ProfileRepository
) {
    /**
     * Executes the adult action if authorized, otherwise notifies that PIN entry is required.
     */
    fun requestAdultAction(
        activeHouseholdProfile: HouseholdProfile,
        action: (String?) -> Unit,
        onRequiresPin: () -> Unit
    ) {
        val status = profileRepository.getPinStatus()
        if (!activeHouseholdProfile.isChild && !status.hasPin) {
            action(null)
        } else if (profileRepository.isAdultUnlocked()) {
            action(profileRepository.currentUnlockToken())
        } else {
            onRequiresPin()
        }
    }

    /**
     * Responds to activity-level background inactivity timeouts by revoking the adult unlock session.
     */
    fun handleInactivityLock(
        inactivityTriggered: Boolean,
        onConsumed: () -> Unit
    ) {
        if (inactivityTriggered) {
            profileRepository.revokeAdultUnlock()
            onConsumed()
        }
    }
}
