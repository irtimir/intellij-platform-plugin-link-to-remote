package com.github.irtimir.intellijplatformpluginlinktoremote.helper

import com.github.irtimir.intellijplatformpluginlinktoremote.commitRandomFile
import com.github.irtimir.intellijplatformpluginlinktoremote.createTmpRepository
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.NoHeadException
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class HelperTest {
    private lateinit var localRepositoryPath: File

    @Before
    fun prepareTest() {
        localRepositoryPath = createTmpRepository()
    }

    @After
    fun cleanupTest() {
        FileUtils.deleteDirectory(localRepositoryPath)
    }

    @Test
    fun testOpenRepository() {
        openRepository(localRepositoryPath)
        openRepository(File(localRepositoryPath, "fstLevelFile"))
        openRepository(File(localRepositoryPath, "fstLevelFile/scdLevelFile"))

        assertFailsWith(
            exceptionClass = IllegalArgumentException::class,
            block = {
                openRepository(File("/localPath"))
            }
        )
    }

    @Test
    fun testGetLastRev() {
        val git = Git(openRepository(localRepositoryPath))
        commitRandomFile(git)
        getLastRev(git)
    }

    @Test
    fun testGetLastRevNoCommits() {
        assertFailsWith(
            exceptionClass = NoHeadException::class,
            block = {
                getLastRev(Git(openRepository(localRepositoryPath)))
            }
        )
    }

    @Test
    fun testOpenRepositoryNoRepository() {
        val tmpDir = Files.createTempDirectory("TestGitRepository").toFile()
        assertFailsWith(
            exceptionClass = IllegalArgumentException::class,
            block = {
                openRepository(tmpDir)
            }
        )
    }

    @Test
    fun testNormalizeRepositoryUrl() {
        assertEquals("http://github.com/user/repo", normalizeRemoteUrl("http://github.com/user/repo.git"))
        assertEquals("https://github.com/user/repo", normalizeRemoteUrl("https://github.com/user/repo.git"))
        assertEquals("https://github.com/user/repo", normalizeRemoteUrl("git@github.com:user/repo.git"))
    }
}
