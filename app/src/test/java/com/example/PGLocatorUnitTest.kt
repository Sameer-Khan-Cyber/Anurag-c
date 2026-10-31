package com.example

import com.example.pglocator.data.model.PGListing
import org.junit.Assert.*
import org.junit.Test

class PGLocatorUnitTest {

    @Test
    fun testPGListing_AmenitiesJsonParsing() {
        val listing = PGListing(
            id = 1,
            title = "Sample PG",
            amenities = "[\"WiFi\", \"AC\", \"Food\", \"Laundry\"]"
        )
        val parsed = listing.parsedAmenities()
        assertEquals(4, parsed.size)
        assertTrue(parsed.contains("WiFi"))
        assertTrue(parsed.contains("Food"))
    }

    @Test
    fun testPGListing_AmenitiesCsvParsing() {
        val listing = PGListing(
            id = 2,
            title = "Sample PG CSV",
            amenities = "WiFi, AC, Security, Gym"
        )
        val parsed = listing.parsedAmenities()
        assertEquals(4, parsed.size)
        assertTrue(parsed.contains("Security"))
        assertTrue(parsed.contains("Gym"))
    }

    @Test
    fun testPGListing_DefaultValues() {
        val listing = PGListing(
            id = 10,
            title = "Test PG",
            monthlyRent = 12000.0,
            status = "approved"
        )
        assertEquals("approved", listing.status)
        assertEquals(12000.0, listing.monthlyRent, 0.001)
    }
}
