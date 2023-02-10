package com.github.irtimir.intellijplatformpluginlinktoremote.actions


import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.logging.Logger


fun openRepository(dir: File): Repository {
    return FileRepositoryBuilder().readEnvironment().findGitDir(dir).build()
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

class LinkRemoteAction(
    private val remoteName: String,
    private val repo: Repository
) : AnAction(remoteName) {
    companion object {
        val LOG: Logger = Logger.getLogger(LinkRemoteAction::class.java.name)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val git = Git(repo)

        for (remote in git.remoteList().call()) {
            if (remote.name == remoteName) {
                val repoRoot = repo.directory.parent
                val editor = e.dataContext.getData(PlatformDataKeys.EDITOR)
                if (editor == null) {
                    LOG.warning("The context does not have an editor.")
                } else {
                    val virtualFile = e.dataContext.getData(PlatformDataKeys.VIRTUAL_FILE)
                    val filePath = virtualFile?.path
                    if (filePath == null || repoRoot !in filePath) {
                        LOG.warning("The file does not have a path, or the path to the file is not in the repository tree.")
                    } else {
                        var pathFromRepoRoot = ""
                        var currentFile = virtualFile
                        while (true) {
                            if (currentFile == null || currentFile.path == repoRoot) {
                                break
                            }
                            if (pathFromRepoRoot != "") {
                                pathFromRepoRoot = "/$pathFromRepoRoot"
                            }
                            pathFromRepoRoot = currentFile.name + pathFromRepoRoot
                            currentFile = currentFile.parent
                        }

                        val repoURL = normalizeRemoteUrl(
                            repo.config.getString(
                                "remote",
                                remoteName,
                                "url"
                            )
                        )
                        val currentSHA = getLastRev(git).name
                        val currentLineNo = editor.caretModel.primaryCaret.visualPosition.line + 1
                        val fullURL = "$repoURL/blob/$currentSHA/$pathFromRepoRoot#L${currentLineNo}"
                        val selection = StringSelection(fullURL)
                        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(selection, selection)
                    }
                }
            }
        }
    }
}

class LinkRemoteActionGroup : ActionGroup() {
    override fun update(e: AnActionEvent) {
        val editor = e.dataContext.getData(PlatformDataKeys.EDITOR)
        if (editor == null) {
            e.presentation.isEnabled = false
            return
        }

        val vf = e.dataContext.getData(PlatformDataKeys.VIRTUAL_FILE)
        val filePath = vf?.path
        if (filePath == null) {
            e.presentation.isEnabled = false
            return
        } else {
            val repo: Repository?
            try {
                repo = openRepository(File(filePath).parentFile)
            } catch (ex: IllegalArgumentException) {
                e.presentation.isEnabled = false
                return
            }
            try {
                getLastRev(Git(repo))
            } catch (ex: NoHeadException) {
                e.presentation.isEnabled = false
                return
            }
        }
    }

    override fun getChildren(e: AnActionEvent?): Array<out AnAction> {
        if (e?.presentation?.isEnabled == true) {
            val vf = e.dataContext.getData(PlatformDataKeys.VIRTUAL_FILE)
            val filePath = vf?.path
            if (filePath != null) {
                val repo: Repository = openRepository(File(filePath).parentFile)
                val actions: MutableList<LinkRemoteAction> = mutableListOf()
                for (n in repo.remoteNames) {
                    actions.add(LinkRemoteAction(n, repo))
                }
                return actions.toTypedArray()
            }
        }
        return arrayOf()
    }
}
