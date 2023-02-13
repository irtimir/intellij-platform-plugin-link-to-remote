package com.github.irtimir.intellijplatformpluginlinktoremote.api

import com.github.irtimir.intellijplatformpluginlinktoremote.addRemote
import com.github.irtimir.intellijplatformpluginlinktoremote.commitRandomFile
import com.github.irtimir.intellijplatformpluginlinktoremote.createTmpRepository
import com.github.irtimir.intellijplatformpluginlinktoremote.helper.openRepository
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class ApiTest {
    private lateinit var localRepositoryPath: File
    private lateinit var git: Git
    private lateinit var lstSha: String

    @Before
    fun prepareTest() {
        localRepositoryPath = createTmpRepository()
        git = Git(openRepository(localRepositoryPath))
        lstSha = commitRandomFile(git)
        addRemote(git.repository)
    }

    @After
    fun cleanupTest() {
        FileUtils.deleteDirectory(localRepositoryPath)
    }

    @Test
    fun testLinkToRemote() {
        assertEquals(
            "http://github.com/user/repo/blob/$lstSha/testfile#L4",
            linkToRemote(git, "origin", File(localRepositoryPath, "testfile").path, 4),
        )
        assertEquals(
            "http://github.com/user/repo/blob/$lstSha/testdir/testfile#L10",
            linkToRemote(git, "origin", File(localRepositoryPath, "testdir/testfile").path, 10),
        )
    }
}