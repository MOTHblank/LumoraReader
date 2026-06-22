package com.mothblank.lumorareader

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testZipSlipProtection_detectsTraversal() {
        // Create a mock temporary directory context
        val tempDir = java.io.File("C:\\Users\\Matheus\\AppData\\Local\\Temp\\lumora-test")
        
        // Malicious entry with path traversal attempt
        val maliciousEntryName = "OEBPS/../../../../etc/passwd"
        val file = java.io.File(tempDir, maliciousEntryName)
        
        val canonicalPath = file.canonicalPath
        val tempDirCanonicalPath = tempDir.canonicalPath
        
        val isTraversal = !canonicalPath.startsWith(tempDirCanonicalPath + java.io.File.separator) && 
                canonicalPath != tempDirCanonicalPath
                
        assertTrue("Malicious ZIP entry with path traversal should be flagged", isTraversal)
    }

    @Test
    fun testZipSlipProtection_allowsSafeEntry() {
        // Create a mock temporary directory context
        val tempDir = java.io.File("C:\\Users\\Matheus\\AppData\\Local\\Temp\\lumora-test")
        
        // Safe relative entry
        val safeEntryName = "OEBPS/content.opf"
        val file = java.io.File(tempDir, safeEntryName)
        
        val canonicalPath = file.canonicalPath
        val tempDirCanonicalPath = tempDir.canonicalPath
        
        val isTraversal = !canonicalPath.startsWith(tempDirCanonicalPath + java.io.File.separator) && 
                canonicalPath != tempDirCanonicalPath
                
        assertFalse("Safe relative entry should not be flagged as a traversal", isTraversal)
    }
}