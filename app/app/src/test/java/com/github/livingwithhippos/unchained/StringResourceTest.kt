package com.github.livingwithhippos.unchained

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class StringResourceTest {

    @Test
    @Config(qualifiers = "it")
    fun testPluralResourcesIt() {
        val context = RuntimeEnvironment.getApplication()

        val zeroServices =
            context.resources.getQuantityString(R.plurals.service_number_format, 0, 0)
        val singularServices =
            context.resources.getQuantityString(R.plurals.service_number_format, 1, 1)
        val pluralServices =
            context.resources.getQuantityString(R.plurals.service_number_format, 5, 5)
        assertEquals("0 servizi", zeroServices)
        assertEquals("1 servizio", singularServices)
        assertEquals("5 servizi", pluralServices)

        val zeroSeeders = context.resources.getQuantityString(R.plurals.seeders_format, 0, 0)
        val singularSeeders = context.resources.getQuantityString(R.plurals.seeders_format, 1, 1)
        val pluralSeeders = context.resources.getQuantityString(R.plurals.seeders_format, 5, 5)
        assertEquals(zeroSeeders, "0 seeders")
        assertEquals(singularSeeders, "1 seeder")
        assertEquals(pluralSeeders, "5 seeders")
    }

    @Test
    @Config(qualifiers = "en")
    fun testPluralResourcesEn() {
        val context = RuntimeEnvironment.getApplication()

        val zero = context.resources.getQuantityString(R.plurals.service_number_format, 0, 0)
        val singular = context.resources.getQuantityString(R.plurals.service_number_format, 1, 1)
        val plural = context.resources.getQuantityString(R.plurals.service_number_format, 5, 5)

        assertEquals("0 services", zero)
        assertEquals("1 service", singular)
        assertEquals("5 services", plural)
    }

    @Test
    @Config(qualifiers = "es")
    fun testPluralResourcesEs() {
        val context = RuntimeEnvironment.getApplication()

        val zero = context.resources.getQuantityString(R.plurals.service_number_format, 0, 0)
        val singular = context.resources.getQuantityString(R.plurals.service_number_format, 1, 1)
        val plural = context.resources.getQuantityString(R.plurals.service_number_format, 5, 5)

        assertEquals("0 servicios", zero)
        assertEquals("1 servicio", singular)
        assertEquals("5 servicios", plural)
    }

    @Test
    @Config(qualifiers = "fr")
    fun testPluralResourcesFr() {
        val context = RuntimeEnvironment.getApplication()

        val zero = context.resources.getQuantityString(R.plurals.service_number_format, 0, 0)
        val singular = context.resources.getQuantityString(R.plurals.service_number_format, 1, 1)
        val plural = context.resources.getQuantityString(R.plurals.service_number_format, 5, 5)

        // French 0 == 1 D;
        assertEquals("0 service", zero)
        assertEquals("1 service", singular)
        assertEquals("5 services", plural)
    }
}
