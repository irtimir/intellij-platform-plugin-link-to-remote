package com.github.irtimir.intellijplatformpluginlinktoremote.api

import com.github.irtimir.intellijplatformpluginlinktoremote.helper.normalizeRemoteUrl
import com.github.irtimir.intellijplatformpluginlinktoremote.helper.getLastRev
import org.eclipse.jgit.api.Git
import java.io.File


class FileNotInRepositoryTree(message: String) : Exception(message)

fun linkToRemote(git: Git, remoteName: String, fileAbsPath: String, lineNumber: Int): String {
    // FIXME:
    // the parent directory of .git is the root of the repository
    // I know it could be different, but right now I can't find a quick way to fix it
    val repositoryRoot = git.repository.directory.parent

    if (repositoryRoot !in fileAbsPath) {
        throw FileNotInRepositoryTree(
            "The file is not in the repository. Repository root: `$repositoryRoot`, File path: `$fileAbsPath`"
        )
    }

    var currentDir = File(fileAbsPath)
    val pathInRepositoryList = mutableListOf<String>()

    while (true) {
        if (currentDir.path == repositoryRoot) {
            break
        }
        pathInRepositoryList.add(currentDir.name)

        currentDir = currentDir.parentFile
    }
    pathInRepositoryList.reverse()
    val pathInRepository = pathInRepositoryList.joinToString(separator = "/")

    val repositoryURL = normalizeRemoteUrl(
        git.repository.config.getString(
            "remote",
            remoteName,
            "url"
        )
    )
    val currentSHA = getLastRev(git).name
    return "$repositoryURL/blob/$currentSHA/$pathInRepository#L${lineNumber}"
}
