package com.github.irtimir.intellijplatformpluginlinktoremote.actions


import com.github.irtimir.intellijplatformpluginlinktoremote.api.FileNotInRepositoryTree
import com.github.irtimir.intellijplatformpluginlinktoremote.api.linkToRemote
import com.github.irtimir.intellijplatformpluginlinktoremote.helper.getLastRev
import com.github.irtimir.intellijplatformpluginlinktoremote.helper.openRepository
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.RemoteConfig
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.logging.Logger


class LinkRemoteAction(
    private val git: Git,
    private val remoteRepo: RemoteConfig,
) : AnAction(remoteRepo.name) {
    companion object {
        val LOG: Logger = Logger.getLogger(LinkRemoteAction::class.java.name)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.dataContext.getData(PlatformDataKeys.EDITOR)
        if (editor == null) {
            LOG.warning("The context does not have an editor.")
            return
        }
        val virtualFile = e.dataContext.getData(PlatformDataKeys.VIRTUAL_FILE)
        val filePath = virtualFile?.path
        if (filePath == null) {
            LOG.warning("The file does not have a path.")
            return
        }
        val fullURL: String
        try {
            fullURL = linkToRemote(
                git,
                remoteRepo.name,
                filePath,
                editor.caretModel.primaryCaret.logicalPosition.line + 1
            )
        } catch (exc: FileNotInRepositoryTree) {
            LOG.warning(exc.message)
            return
        }
        val selection = StringSelection(fullURL)
        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
        return
    }
}


class LinkRemoteActionGroup : ActionGroup() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val editor = e.dataContext.getData(PlatformDataKeys.EDITOR)
        if (editor == null) {
            e.presentation.isEnabled = false
            return
        }

        val filePath = e.dataContext.getData(PlatformDataKeys.VIRTUAL_FILE)?.path
        if (filePath == null) {
            e.presentation.isEnabled = false
            return
        }
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

    override fun getChildren(e: AnActionEvent?): Array<out AnAction> {
        if (e?.presentation?.isEnabled == true) {
            val vf = e.dataContext.getData(PlatformDataKeys.VIRTUAL_FILE)
            val filePath = vf?.path
            if (filePath != null) {
                val repo: Repository = openRepository(File(filePath).parentFile)
                val actions: MutableList<LinkRemoteAction> = mutableListOf()
                val git = Git(repo)
                for (remote in git.remoteList().call()) {
                    actions.add(LinkRemoteAction(git, remote))
                }
                return actions.toTypedArray()
            }
        }
        return arrayOf()
    }
}
