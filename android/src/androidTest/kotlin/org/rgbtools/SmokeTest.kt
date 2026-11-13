package org.rgbtools

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test for the rgb-lib Kotlin bindings.
 *
 * It only exercises operations that need no indexer, proxy or other service, so
 * it can run on a bare emulator. Its purpose is to catch an rgb-lib update
 * breaking the bindings: a native library that doesn't load, a changed or
 * removed API, a broken type or error conversion.
 *
 * Anything requiring a regtest environment is covered by the rgb-lib test
 * suite and is out of scope here.
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    private val bitcoinNetwork = BitcoinNetwork.REGTEST
    private val witnessVersion = WitnessVersion.TAPROOT
    private val backupPassword = "backup password"

    private lateinit var testDir: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDir = File(context.filesDir, "rgb-lib-smoke-test")
        testDir.deleteRecursively()
        check(testDir.mkdirs()) { "cannot create test directory $testDir" }
    }

    private fun dataDir(name: String = "data"): File =
        File(testDir, name).also { check(it.mkdirs()) { "cannot create data directory $it" } }

    private fun walletData(dataDir: File) =
        WalletData(
            dataDir = dataDir.absolutePath,
            bitcoinNetwork = bitcoinNetwork,
            databaseType = DatabaseType.SQLITE,
            maxAllocationsPerUtxo = 1u,
            supportedSchemas =
                listOf(
                    AssetSchema.NIA,
                    AssetSchema.UDA,
                    AssetSchema.CFA,
                    AssetSchema.IFA,
                ),
        )

    private fun singlesigKeys(keys: Keys) =
        SinglesigKeys(
            accountXpubVanilla = keys.accountXpubVanilla,
            accountXpubColored = keys.accountXpubColored,
            vanillaKeychain = null,
            masterFingerprint = keys.masterFingerprint,
            mnemonic = keys.mnemonic,
            witnessVersion = keys.witnessVersion,
        )

    private fun newWallet(dataDir: File, keys: Keys) = Wallet(walletData(dataDir), singlesigKeys(keys))

    /** The native library loads and keys can be generated. */
    @Test
    fun generateKeys() {
        val keys = generateKeys(bitcoinNetwork, witnessVersion)

        assertEquals(12, keys.mnemonic.split(" ").size)
        assertEquals(8, keys.masterFingerprint.length)
        assertTrue(keys.xpub.isNotEmpty())
        assertTrue(keys.accountXpubVanilla.isNotEmpty())
        assertTrue(keys.accountXpubColored.isNotEmpty())
        assertNotEquals(keys.accountXpubVanilla, keys.accountXpubColored)
        assertEquals(witnessVersion, keys.witnessVersion)

        // each call gives a brand new wallet
        assertNotEquals(keys.mnemonic, generateKeys(bitcoinNetwork, witnessVersion).mnemonic)
    }

    /** Keys can be restored from a mnemonic, deterministically. */
    @Test
    fun restoreKeys() {
        val keys = generateKeys(bitcoinNetwork, witnessVersion)

        val restored = restoreKeys(bitcoinNetwork, keys.mnemonic, witnessVersion)

        assertEquals(keys, restored)
    }

    /** Errors coming from the Rust side are converted to Kotlin exceptions. */
    @Test
    fun errorConversion() {
        // error without fields
        assertThrows(RgbLibException.InvalidMnemonic::class.java) {
            restoreKeys(bitcoinNetwork, "not a valid mnemonic", witnessVersion)
        }

        // error with fields
        val assetId = "rgb:2dkSTbr-jFhkeSZ8X-dwAiMSyLd-HhQ6gKCNM-dW9BjJVSc-5SG9dBr"
        newWallet(dataDir(), generateKeys(bitcoinNetwork, witnessVersion)).use { wallet ->
            val exception =
                assertThrows(RgbLibException.AssetNotFound::class.java) {
                    wallet.getAssetBalance(assetId)
                }
            assertEquals(assetId, exception.assetId)
        }
    }

    /** A wallet can be created and queried while offline. */
    @Test
    fun walletCreationAndOfflineQueries() {
        val dataDir = dataDir()
        val keys = generateKeys(bitcoinNetwork, witnessVersion)

        newWallet(dataDir, keys).use { wallet ->
            // wallet directories
            val walletDir = File(wallet.getWalletDir())
            assertTrue(walletDir.isDirectory)
            assertEquals(keys.masterFingerprint, walletDir.name)
            assertTrue(wallet.getMediaDir().startsWith(walletDir.absolutePath))

            // wallet data and keys round-trip
            assertEquals(bitcoinNetwork, wallet.getWalletData().bitcoinNetwork)
            assertEquals(1u, wallet.getWalletData().maxAllocationsPerUtxo)
            assertEquals(keys.masterFingerprint, wallet.getKeys().masterFingerprint)
            assertEquals(keys.accountXpubColored, wallet.getKeys().accountXpubColored)

            // descriptors
            val descriptors = wallet.getDescriptors()
            assertTrue(descriptors.colored.startsWith("tr("))
            assertTrue(descriptors.vanilla.startsWith("tr("))
            assertNotEquals(descriptors.colored, descriptors.vanilla)

            // addresses
            val address = wallet.getAddress()
            assertTrue("unexpected address: $address", address.startsWith("bcrt1"))
            assertNotEquals(address, wallet.getAddress())

            // an empty wallet has no balance, no assets and no transfers
            val btcBalance = wallet.getBtcBalance(null, true)
            assertEquals(Balance(0uL, 0uL, 0uL), btcBalance.vanilla)
            assertEquals(Balance(0uL, 0uL, 0uL), btcBalance.colored)
            val assets = wallet.listAssets(emptyList())
            assertEquals(emptyList<AssetNia>(), assets.nia)
            assertEquals(emptyList<AssetUda>(), assets.uda)
            assertEquals(emptyList<AssetCfa>(), assets.cfa)
            assertEquals(emptyList<AssetIfa>(), assets.ifa)
            assertTrue(wallet.listUnspents(null, false, true).isEmpty())
            assertTrue(wallet.listTransfers(null).isEmpty())
            assertTrue(wallet.listPendingVanillaTxs().isEmpty())
            assertTrue(wallet.listTransactions(null, true).isEmpty())
        }
    }

    /** An existing wallet can be loaded from its data directory. */
    @Test
    fun walletLoading() {
        val dataDir = dataDir()
        val keys = generateKeys(bitcoinNetwork, witnessVersion)
        val descriptors = newWallet(dataDir, keys).use { it.getDescriptors() }

        Wallet.load(dataDir.absolutePath, keys.masterFingerprint, keys.mnemonic).use { wallet ->
            assertEquals(descriptors, wallet.getDescriptors())
            assertEquals(keys.masterFingerprint, wallet.getKeys().masterFingerprint)
        }
    }

    /** A wallet can be backed up and the backup can be restored. */
    @Test
    fun walletBackupAndRestore() {
        val dataDir = dataDir()
        val keys = generateKeys(bitcoinNetwork, witnessVersion)
        val backupFile = File(testDir, "wallet.rgb-lib_backup")

        val descriptors =
            newWallet(dataDir, keys).use { wallet ->
                // a wallet that has seen no operation has nothing to back up
                assertFalse(wallet.backupInfo())
                val descriptors = wallet.getDescriptors()
                wallet.getAddress()
                assertTrue(wallet.backupInfo())

                wallet.backup(backupFile.absolutePath, backupPassword)
                assertFalse(wallet.backupInfo())

                descriptors
            }
        assertTrue(backupFile.length() > 0)

        val restoreDir = dataDir("restored")
        restoreBackup(backupFile.absolutePath, backupPassword, restoreDir.absolutePath)

        Wallet.load(restoreDir.absolutePath, keys.masterFingerprint, keys.mnemonic).use { wallet ->
            assertEquals(descriptors, wallet.getDescriptors())
        }
    }
}
