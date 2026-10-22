package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.JarvisDatabase
import com.example.data.model.CommandHistoryEntity
import com.example.data.model.CommandType
import com.example.data.model.PcAction
import com.example.data.repository.GeminiIntentParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: JarvisDatabase
    private val intentParser = GeminiIntentParser()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, JarvisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppNameResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Jarvis Assistant", appName)
    }

    @Test
    fun testParseLaunchWhatsApp() = runBlocking {
        val result = intentParser.parseCommand("Open WhatsApp")
        assertEquals(CommandType.LAUNCH_APP, result.commandType)
        assertEquals("com.whatsapp", result.targetPackage)
    }

    @Test
    fun testParseCallPapa() = runBlocking {
        val result = intentParser.parseCommand("Call Papa")
        assertEquals(CommandType.CALL_PHONE, result.commandType)
        assertEquals("Papa", result.contactName)
        assertTrue(result.isConsequential)
    }

    @Test
    fun testWakeWordPrefixStripping() = runBlocking {
        val result = intentParser.parseCommand("Hi Jarvis, call Papa")
        assertEquals(CommandType.CALL_PHONE, result.commandType)
        assertEquals("Papa", result.contactName)
    }

    @Test
    fun testParseHinglishCall() = runBlocking {
        val result = intentParser.parseCommand("Papa ko call lagao")
        assertEquals(CommandType.CALL_PHONE, result.commandType)
        assertEquals("Papa", result.contactName)
    }

    @Test
    fun testParseLockPc() = runBlocking {
        val result = intentParser.parseCommand("Lock my PC")
        assertEquals(CommandType.PC_COMMAND, result.commandType)
        assertEquals(PcAction.LOCK_PC, result.pcAction)
    }

    @Test
    fun testParseFlashlightToggle() = runBlocking {
        val result = intentParser.parseCommand("Turn on flashlight")
        assertEquals(CommandType.TORCH_TOGGLE, result.commandType)
        assertEquals("on", result.settingValue)
    }

    @Test
    fun testParseVolumeUp() = runBlocking {
        val result = intentParser.parseCommand("Increase volume")
        assertEquals(CommandType.VOLUME_CONTROL, result.commandType)
        assertEquals("up", result.settingValue)
    }

    @Test
    fun testParseOpenFiles() = runBlocking {
        val result = intentParser.parseCommand("Open files")
        assertEquals(CommandType.OPEN_FILE, result.commandType)
    }

    @Test
    fun testParseWebSearch() = runBlocking {
        val result = intentParser.parseCommand("Search the web for Android development")
        assertEquals(CommandType.WEB_SEARCH, result.commandType)
        assertEquals("Android development", result.searchQuery)
    }

    @Test
    fun testParseYouTubeSearch() = runBlocking {
        val result = intentParser.parseCommand("Open YouTube and search for Python tutorials")
        assertEquals(CommandType.YOUTUBE_SEARCH, result.commandType)
        assertTrue(result.searchQuery?.contains("Python tutorials", ignoreCase = true) == true)
    }

    @Test
    fun testParseWhatsAppMessage() = runBlocking {
        val cmd = "Send a WhatsApp message to Rahul saying I will arrive in ten minutes"
        val result = intentParser.parseCommand(cmd)
        assertEquals(CommandType.WHATSAPP_MESSAGE, result.commandType)
        assertEquals("Rahul", result.contactName)
        assertEquals("I will arrive in ten minutes", result.messageBody)
        assertTrue(result.isConsequential)
    }

    @Test
    fun testParseEmergencyStop() = runBlocking {
        val result1 = intentParser.parseCommand("Emergency stop")
        assertEquals(CommandType.EMERGENCY_STOP, result1.commandType)

        val result2 = intentParser.parseCommand("ruk jao")
        assertEquals(CommandType.EMERGENCY_STOP, result2.commandType)
    }

    @Test
    fun testRoomDatabaseHistory() = runBlocking {
        val dao = database.commandDao()
        val entry = CommandHistoryEntity(
            rawCommand = "Call Papa",
            commandType = CommandType.CALL_PHONE.name,
            targetSummary = "Papa",
            executionStatus = "SUCCESS",
            statusMessage = "Dialer initiated."
        )
        val id = dao.insertCommand(entry)
        assertTrue(id > 0)

        val history = dao.getAllHistory().first()
        assertEquals(1, history.size)
        assertEquals("Call Papa", history[0].rawCommand)
        assertEquals("SUCCESS", history[0].executionStatus)
    }
}
