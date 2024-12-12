package com.github.irtimir.intellijplatformpluginlinktoremote

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import java.io.File
import java.io.IOException

fun createTmpRepository(repositoryPath: File = File.createTempFile("TestGitRepository", "")): File {
    if (repositoryPath.isFile && !repositoryPath.delete()) {
        throw IOException("Could not delete temporary file $repositoryPath")
    }

    // create the directory
    Git.init().setDirectory(repositoryPath).call()
    return repositoryPath
}

fun commitRandomFile(git: Git): String {
    val fileName = "testfile"
    val file = File(git.repository.directory.parent, fileName)
    if (!file.createNewFile()) {
        throw IOException("Could not create file $file")
    }
    git.add().addFilepattern(fileName).call()
    git.commit().setMessage("Added $fileName").setSign(false).call()
    return git.log().setMaxCount(1).call().iterator().next().name
}

fun addRemote(
    repository: Repository,
    remoteName: String = "origin",
    remoteUrl: String = "http://github.com/user/repo"
): String {
    repository.config.setString("remote", remoteName, "url", remoteUrl)
    return remoteUrl
}
