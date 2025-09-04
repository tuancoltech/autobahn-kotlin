package io.crossbar.autobahn.wamp.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Field

class IDGeneratorTest {
    private val MAX = 9007199254740992L // 2^53

    // Small helper to peek/set the private mNext via reflection
    private fun setNext(gen: IDGenerator, value: Long) {
        val f: Field = IDGenerator::class.java.getDeclaredField("mNext")
        f.isAccessible = true
        f.setLong(gen, value)
    }
    private fun getNext(gen: IDGenerator): Long {
        val f: Field = IDGenerator::class.java.getDeclaredField("mNext")
        f.isAccessible = true
        return f.getLong(gen)
    }

    @Test
    fun next_initial_returns1_andInternalStateIs1() {
        val gen = IDGenerator()
        val id = gen.next()
        assertEquals(1L, id)
        assertEquals(1L, getNext(gen))
    }

    @Test
    fun next_incrementsSequentially_forNormalRange() {
        val gen = IDGenerator()
        // first already tested; proceed with a couple more
        assertEquals(1L, gen.next())
        assertEquals(2L, gen.next())
        assertEquals(3L, gen.next())
        assertEquals(4L, gen.next())
        assertEquals(5L, gen.next())
    }

    @Test
    fun next_reachesMax_withoutWrapping_thenWrapsOnNextCall() {
        val gen = IDGenerator()
        // Set internal state to MAX - 1 so next() should yield MAX
        setNext(gen, MAX - 1)
        assertEquals(MAX, gen.next())         // hits the max exactly
        assertEquals(MAX, getNext(gen))       // stays at MAX internally

        // Next call should overflow the limit and wrap back to 1
        assertEquals(1L, gen.next())
        assertEquals(1L, getNext(gen))
    }

    @Test
    fun next_wrapsWhenExceedsMax_directlyFromMax() {
        val gen = IDGenerator()
        setNext(gen, MAX)                     // internal at max already
        val wrapped = gen.next()              // increments -> MAX+1 -> wrap to 1
        assertEquals(1L, wrapped)
        assertEquals(1L, getNext(gen))
    }

    @Test
    fun next_continuesAfterWrap() {
        val gen = IDGenerator()
        setNext(gen, MAX)                     // force wrap on next
        assertEquals(1L, gen.next())          // wrapped
        assertEquals(2L, gen.next())          // continues incrementing
        assertEquals(3L, gen.next())
    }

    @Test
    fun next_withNegativeInternalState_edgesToZero_thenIncrements() {
        // This exercises an artificial edge: if the field were ever negative.
        val gen = IDGenerator()
        setNext(gen, -1L)
        assertEquals(0L, gen.next())          // -1 + 1 = 0, still <= MAX, so allowed
        assertEquals(1L, gen.next())          // then increments normally
    }
}