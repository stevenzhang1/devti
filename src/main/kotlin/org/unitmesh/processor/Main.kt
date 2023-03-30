package org.unitmesh.processor

import com.charleskorn.kaml.Yaml
import com.github.ajalt.clikt.core.CliktCommand
import org.slf4j.Logger
import org.unitmesh.processor.toolsets.GitCommandManager
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) = Runner().main(args)
class Runner : CliktCommand(help = "Action Runner") {
    override fun run() {
        logger.info("Runner started")
        //  load config `processor.yml` and start to scm
        val file = File("processor.yml").let {
            if(!it.exists()) {
                logger.error("Config file not found: ${it.absolutePath}")
                exitProcess(1)
            }

            it
        }

        val content = file.readText()
        val config = Yaml.default.decodeFromString(deserializer = PreProcessorConfig.serializer(), content)

        logger.info("Start to Clone code from GitHub")
        config.scm.forEach {
            // path from GitHub repository name
            val path = it.repository.split("/").last()
            val targetPath = clonedPath(path)
            // if directory exits and contains .git, then skip
            if (File(targetPath).exists() && File(targetPath + File.separator + ".git").exists()) {
                logger.info("Skip $targetPath")
                return@forEach
            }

            File(targetPath).mkdirs()

            val gitCommandManager = GitCommandManager(targetPath)
            gitCommandManager.shallowClone(it.repository, it.branch)
        }

        logger.info("Start to Filter Test Cases")
        // walkdir in `origindatasets` and filter test cases
        File("origindatasets").walkTopDown().forEach {
            // if a file ends with `Test.java` or `Tests.java`, then copy it to `datasets`
            if (it.isFile && (it.name.endsWith("Test.java") || it.name.endsWith("Tests.java"))) {
                val targetPath = "datasets" + File.separator + it.path.split(File.separator).last()
                it.copyTo(File(targetPath), true)
            }
        }


        logger.info("Runner finished")
    }

    private fun clonedPath(path: String) = "origindatasets" + File.separator + path

    companion object {
        val logger: Logger = org.slf4j.LoggerFactory.getLogger(Runner::class.java)
    }
}