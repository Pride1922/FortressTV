package com.fortresstv.player.profile

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ParentalPinStoreTest {

    private class MemoryPinPersistence(var payloadResult: PinPayloadResult? = null) : PinPersistence {
        private val namespaces = mutableMapOf<String, JSONObject>()
        private var currentNs = "local"

        override fun load(): PinPayloadResult {
            payloadResult?.let { return it }
            val data = namespaces[currentNs] ?: return if (currentNs == "local") PinPayloadResult.UnconfiguredLocal else PinPayloadResult.UnconfirmedAuthenticated
            return PinPayloadResult.Valid(JSONObject(data.toString()))
        }

        override fun save(json: JSONObject) {
            namespaces[currentNs] = JSONObject(json.toString())
            payloadResult = null
        }

        override fun switchNamespace(namespace: String) {
            currentNs = namespace
        }
    }

    @Test
    fun hashingPinIsConsistentAndRequiresDigits() {
        val salt = "test-salt-1234"
        val pin = "1234"
        val hash1 = ParentalPinStore.hashPin(salt, pin)
        val hash2 = ParentalPinStore.hashPin(salt, pin)
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // 256 bits = 64 hex characters

        // Different salt yields different hash
        val hash3 = ParentalPinStore.hashPin("other-salt", pin)
        assertNotEquals(hash1, hash3)

        // Invalid pins throw IllegalArgumentException
        try {
            ParentalPinStore.hashPin(salt, "12") // too short
            fail("Expected exception for short PIN")
        } catch (_: IllegalArgumentException) {}

        try {
            ParentalPinStore.hashPin(salt, "12ab") // non-digits
            fail("Expected exception for non-digit PIN")
        } catch (_: IllegalArgumentException) {}
    }

    @Test
    fun settingAndVerifyingPinLocally() {
        val persistence = MemoryPinPersistence()
        val store = ParentalPinStore(persistence)

        assertFalse(store.getStatus().hasPin)
        assertFalse(store.isAdultUnlocked())

        // Set PIN
        store.setPinLocally("1234")
        assertTrue(store.getStatus().hasPin)
        assertFalse(store.isAdultUnlocked())

        // Verify with wrong PIN
        val failedResult = store.verifyPinLocally("9999")
        assertTrue(failedResult is PinVerifyResult.Failed)
        assertEquals(4, (failedResult as PinVerifyResult.Failed).remainingAttempts)
        assertFalse(store.isAdultUnlocked())

        // Verify with correct PIN
        val successResult = store.verifyPinLocally("1234")
        assertTrue(successResult is PinVerifyResult.Success)
        assertTrue(store.isAdultUnlocked())
        assertEquals(5, store.getStatus().remainingAttempts)
    }

    @Test
    fun lockoutTriggeredAfterFiveFailedAttempts() {
        val persistence = MemoryPinPersistence()
        val store = ParentalPinStore(persistence)
        store.setPinLocally("4321")
        store.revokeAdultUnlock()

        for (i in 1..4) {
            val res = store.verifyPinLocally("0000")
            assertTrue(res is PinVerifyResult.Failed)
            assertEquals(5 - i, (res as PinVerifyResult.Failed).remainingAttempts)
        }

        // 5th attempt locks out
        val lockResult = store.verifyPinLocally("0000")
        assertTrue(lockResult is PinVerifyResult.Locked)
        assertTrue(store.getStatus().isLocked)

        // While locked, even correct PIN is rejected
        val lockedAttempt = store.verifyPinLocally("4321")
        assertTrue(lockedAttempt is PinVerifyResult.Locked)
        assertFalse(store.isAdultUnlocked())
    }

    @Test
    fun removingPinClearsState() {
        val persistence = MemoryPinPersistence()
        val store = ParentalPinStore(persistence)
        store.setPinLocally("1234")
        assertTrue(store.getStatus().hasPin)

        store.removePinLocally()
        assertFalse(store.getStatus().hasPin)
        assertFalse(store.isAdultUnlocked())
    }

    @Test
    fun corruptedStorageFailsClosed() {
        val persistence = MemoryPinPersistence(payloadResult = PinPayloadResult.Corrupted)
        val store = ParentalPinStore(persistence)

        // Corrupted store must report hasPin = true and isLocked = true, and isProtected = true
        val status = store.getStatus()
        assertEquals(PinConfigurationState.CORRUPTED, status.configurationState)
        assertTrue(status.hasPin)
        assertTrue(status.isLocked)
        assertTrue(status.isProtected)
        assertEquals(0, status.remainingAttempts)
        assertTrue(status.lockoutSeconds > 0)

        // Verification must be locked and fail closed
        val res = store.verifyPinLocally("1234")
        assertTrue(res is PinVerifyResult.Locked)
        assertFalse(store.isAdultUnlocked())
    }

    @Test
    fun unconfiguredLocalReportsGenuineNoPin() {
        val persistence = MemoryPinPersistence()
        val store = ParentalPinStore(persistence)
        val status = store.getStatus()
        assertEquals(PinConfigurationState.GENUINE_NO_PIN, status.configurationState)
        assertFalse(status.hasPin)
        assertFalse(status.isProtected)
    }

    @Test
    fun unconfirmedAuthenticatedReportsUnknownAndProtected() {
        val persistence = MemoryPinPersistence()
        val store = ParentalPinStore(persistence)
        store.switchNamespace("authenticated_account_uuid")
        val status = store.getStatus()
        assertEquals(PinConfigurationState.UNKNOWN, status.configurationState)
        assertFalse(status.hasPin)
        assertTrue(status.isProtected)
    }

    @Test
    fun confirmedNoPinReportsUnprotected() {
        val persistence = MemoryPinPersistence()
        val store = ParentalPinStore(persistence)
        store.switchNamespace("authenticated_account_uuid")
        store.syncServerStatus(hasPin = false, isLocked = false, remainingAttempts = 5, lockoutSeconds = 0)
        val status = store.getStatus()
        assertEquals(PinConfigurationState.CONFIRMED_NO_PIN, status.configurationState)
        assertFalse(status.hasPin)
        assertFalse(status.isProtected)
    }

    @Test
    fun serverManagedPinCannotBeBypassedLocally() {
        val persistence = MemoryPinPersistence()
        val store = ParentalPinStore(persistence)
        store.syncServerStatus(hasPin = true, isLocked = false, remainingAttempts = 5, lockoutSeconds = 0)

        assertTrue(store.getStatus().hasPin)
        // Attempting to verify locally when PIN is server_managed must fail
        val res = store.verifyPinLocally("1234")
        assertTrue(res is PinVerifyResult.Failed)
        assertFalse(store.isAdultUnlocked())
    }

    @Test
    fun switchingNamespaceRevokesAdultUnlockAndIsolatesState() {
        val persistence = MemoryPinPersistence()
        val store = ParentalPinStore(persistence)

        // Account A sets PIN and unlocks
        store.switchNamespace("account_A")
        store.setPinLocally("1111")
        assertTrue(store.getStatus().hasPin)
        store.grantAdultUnlock("token_A")
        assertTrue(store.isAdultUnlocked())

        // Switch to Account B
        store.switchNamespace("account_B")
        assertFalse(store.isAdultUnlocked())
        assertFalse(store.getStatus().hasPin) // Account B has no PIN set yet!

        // Switch back to Account A: unlock was revoked, but PIN exists
        store.switchNamespace("account_A")
        assertFalse(store.isAdultUnlocked())
        assertTrue(store.getStatus().hasPin)
    }
}
