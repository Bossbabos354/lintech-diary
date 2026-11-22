package ru.school28.diary

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildInfoTest {

    @Test
    fun versionIsInjectedFromGradle() {
        val version = BuildInfo.VERSION_NAME
        assertTrue("версия должна быть вида X.Y.Z, а не '$version'", version.matches(Regex("""\d+\.\d+\.\d+""")))
        assertNotEquals("0.0.0", version)
    }
}
