package com.mindlol.probe.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivePackageResolverTest {

    private val selfPkg = "com.mindlol.probe.debug"
    private val instagramPkg = "com.instagram.android"
    private val youtubePkg = "com.google.android.youtube"
    private val chromePkg = "com.android.chrome"

    @Test
    fun `test 1 - Initial event from target app resolves target package`() {
        val resolved = ActivePackageResolver.resolveActivePackage(
            eventPackageName = instagramPkg,
            currentActivePackageName = null,
            selfPackageName = selfPkg
        )
        assertEquals(instagramPkg, resolved)
    }

    @Test
    fun `test 2 - SystemUI event while Instagram is active preserves Instagram`() {
        val resolved = ActivePackageResolver.resolveActivePackage(
            eventPackageName = "com.android.systemui",
            currentActivePackageName = instagramPkg,
            selfPackageName = selfPkg
        )
        assertEquals(instagramPkg, resolved)
    }

    @Test
    fun `test 3 - Android OS event while YouTube is active preserves YouTube`() {
        val resolved = ActivePackageResolver.resolveActivePackage(
            eventPackageName = "android",
            currentActivePackageName = youtubePkg,
            selfPackageName = selfPkg
        )
        assertEquals(youtubePkg, resolved)
    }

    @Test
    fun `test 4 - Self overlay event while Instagram is active preserves Instagram`() {
        val resolved = ActivePackageResolver.resolveActivePackage(
            eventPackageName = selfPkg,
            currentActivePackageName = instagramPkg,
            selfPackageName = selfPkg
        )
        assertEquals(instagramPkg, resolved)
    }

    @Test
    fun `test 5 - Real app switch to Chrome updates active package`() {
        val resolved = ActivePackageResolver.resolveActivePackage(
            eventPackageName = chromePkg,
            currentActivePackageName = instagramPkg,
            selfPackageName = selfPkg
        )
        assertEquals(chromePkg, resolved)
    }

    @Test
    fun `test 6 - Switching between Instagram and YouTube updates active package correctly`() {
        val step1 = ActivePackageResolver.resolveActivePackage(
            eventPackageName = instagramPkg,
            currentActivePackageName = null,
            selfPackageName = selfPkg
        )
        assertEquals(instagramPkg, step1)

        val step2 = ActivePackageResolver.resolveActivePackage(
            eventPackageName = youtubePkg,
            currentActivePackageName = step1,
            selfPackageName = selfPkg
        )
        assertEquals(youtubePkg, step2)

        val step3 = ActivePackageResolver.resolveActivePackage(
            eventPackageName = instagramPkg,
            currentActivePackageName = step2,
            selfPackageName = selfPkg
        )
        assertEquals(instagramPkg, step3)
    }

    @Test
    fun `test 7 - Null or blank event package preserves current active package`() {
        val resNull = ActivePackageResolver.resolveActivePackage(
            eventPackageName = null,
            currentActivePackageName = instagramPkg,
            selfPackageName = selfPkg
        )
        assertEquals(instagramPkg, resNull)

        val resBlank = ActivePackageResolver.resolveActivePackage(
            eventPackageName = "  ",
            currentActivePackageName = youtubePkg,
            selfPackageName = selfPkg
        )
        assertEquals(youtubePkg, resBlank)
    }
}
