#!/usr/bin/env kscript
//DEPS org.gitlab4j:gitlab4j-api:4.8.25
//DEPS org.eclipse.jgit:org.eclipse.jgit:5.0.1.201806211838-r

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.gitlab4j.api.*
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.Project
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.Response

val gitlabUrl = System.getenv("GITLAB_URL")
val gitlabPrivateToken = System.getenv("GITLAB_PRIVATE_TOKEN")
val dest = System.getenv("GITLAB_DEST")

val gitLabApi = GitLabApi(gitlabUrl, gitlabPrivateToken)
val customGroupApi = CustomGroupApi(gitLabApi)
val customTransportConfigCallBack = CustomTransportConfigCallBack()


val allGroups = customGroupApi.getGroupsIncludeAllAvailable(allAvailable = true)

for (group in allGroups) {
    checkout(group)
}

fun checkout(group: Group) {
    val projectPager = gitLabApi.groupApi.getProjects(group.id, 20)
    while (projectPager.hasNext()) {
        val projectsInPage = projectPager.next()
        checkout(group, projectsInPage)
    }
}

fun checkout(group: Group, projects: List<Project>) {
    for (project in projects) {
        checkout(group, project)
    }
}

fun checkout(group: Group, project: Project) {
    println("Checking out project: ${project.toShortString()}")
    val checkoutPath = Paths.get(dest, "${project.name}-${project.id}")
    val checkoutFile = checkoutPath.toFile()
    val mkdirs = checkoutFile.mkdirs()
    if (!mkdirs) {
        println("Try to resume checking out ${project.toShortString()} with group: ${group.name}")
        if (checkoutFile.list().isNotEmpty()) {
            println("Skipped ${project.toShortString()} because the directory is not empty")
            return;
        }
    }

    doCheckout(project, checkoutFile)
    println("Checked out project: ${project.toShortString()}")
}

fun doCheckout(project: Project, checkoutFile: File) {
    // todo handle checkout latest
    clone(project, checkoutFile)
}

fun clone(project: Project, checkoutFile: File) {
    Git.cloneRepository()
            .setURI(project.sshUrlToRepo)
            .setTransportConfigCallback(customTransportConfigCallBack)
            .setDirectory(checkoutFile)
            .call()
}

fun Project.toShortString(): String {
    return "id: $id and name: $name and path: $path"
}

class CustomGroupApi(private val gitLabApi: GitLabApi) : GroupApi(gitLabApi) {
    fun getGroupsIncludeAllAvailable(allAvailable: Boolean): List<Group> {
        val formData = GitLabApiForm().withParam("all_available", allAvailable).withParam("per_page", this.defaultPerPage)
        val response = get(Response.Status.OK, formData.asMap(), *arrayOf<Any>("groups"))
        return response.readEntity(object : GenericType<List<Group>>() {

        })
    }
}

class CustomTransportConfigCallBack : TransportConfigCallback {
    private val customSshSessionFactory = CustomSshSessionFactory()

    override fun configure(transport: Transport) {

        val sshTransport = transport as SshTransport
        sshTransport.sshSessionFactory = customSshSessionFactory
    }
}

class CustomSshSessionFactory : JschConfigSessionFactory() {
    private val customUserInfo = CustomUserInfo()

    override fun configure(host: OpenSshConfig.Host, session: Session) {
        session.setConfig("StrictHostKeyChecking", "no")
        session.userInfo = customUserInfo
    }

    override fun configureJSch(jsch: JSch) {
        jsch.addIdentity("~/.ssh/id_rsa")
    }
}

class CustomUserInfo : UserInfo {
    override fun promptPassphrase(message: String?): Boolean {
        return true
    }

    override fun getPassphrase(): String {
        return ""
    }

    override fun getPassword(): String? {
        return null
    }

    override fun promptYesNo(message: String): Boolean {
        return false
    }

    override fun showMessage(message: String?) {
    }

    override fun promptPassword(message: String?): Boolean {
        return false
    }
}






