package com.github.irtimir.intellijplatformpluginlinktoremote.helper

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

fun openRepository(dir: File): Repository {
    val builder = FileRepositoryBuilder()
    return builder.readEnvironment().findGitDir(dir).build()
}


fun getLastRev(git: Git): RevCommit {
    return git.log().setMaxCount(1).call().iterator().next()
}


fun normalizeRemoteUrl(remoteUrl: String): String {
    if (remoteUrl.startsWith("http")) {
        return remoteUrl.removeSuffix(".git")
    }

    val parts = remoteUrl
        .removePrefix("git@")
        .removeSuffix(".git")
        .split(":", limit = 2)

    return "https://" + parts[0] + '/' + parts[1]
}
