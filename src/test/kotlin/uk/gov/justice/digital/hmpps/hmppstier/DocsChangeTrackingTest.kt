package uk.gov.justice.digital.hmpps.hmppstier

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.security.MessageDigest

/**
 * Ensures that plain-language documentation stays in sync with the source code it describes.
 *
 * Each entry in `docs-change-tracking.yaml` links a markdown doc to one or more source files.
 * If any source file changes, this test fails until:
 *   1. The documentation is reviewed/updated to reflect the change
 *   2. The hash in the registry is updated to the new value
 *
 * This prevents documentation from silently drifting out of sync with the code.
 */
class DocsChangeTrackingTest {

    private val repoRoot = File(System.getProperty("user.dir"))
    private val registryFile = repoRoot.resolve("docs-change-tracking.yaml")

    @TestFactory
    fun `documentation stays in sync with source code`(): List<DynamicTest> {
        if (!registryFile.exists()) {
            return emptyList()
        }

        val yaml = Yaml()
        val entries: List<Map<String, Any>> = yaml.load(registryFile.readText())

        return entries.map { entry ->
            val id = entry["id"] as String
            val description = entry["description"] as String
            val docPath = entry["doc"] as String
            val sources = (entry["sources"] as List<*>).map { it as String }
            val expectedHash = entry["hash"] as String

            DynamicTest.dynamicTest("$id: $description") {
                val docFile = repoRoot.resolve(docPath)
                assertThat(docFile)
                    .describedAs("Documentation file '$docPath' must exist")
                    .exists()

                val sourceFiles = sources.flatMap { sourcePath ->
                    val file = repoRoot.resolve(sourcePath)
                    if (file.isDirectory) {
                        file.walkTopDown()
                            .filter { it.isFile && it.extension == "kt" }
                            .toList()
                    } else {
                        listOf(file)
                    }
                }.sortedBy { it.relativeTo(repoRoot).invariantSeparatorsPath }

                sourceFiles.forEach { file ->
                    assertThat(file)
                        .describedAs("Source file '${file.relativeTo(repoRoot)}' must exist")
                        .exists()
                }

                val actualHash = hashOf(sourceFiles)

                assertThat(actualHash)
                    .describedAs(
                        """
                        |Source files for '$id' have changed.
                        |
                        |Please:
                        |  1. Review and update '$docPath' if needed
                        |  2. Update the hash in docs-change-tracking.yaml to: $actualHash
                        |
                        |Source files tracked:
                        |${sourceFiles.joinToString("\n") { "  - ${it.relativeTo(repoRoot)}" }}
                        """.trimMargin()
                    )
                    .isEqualTo(expectedHash)
            }
        }
    }

    private fun hashOf(files: List<File>): String {
        val digest = MessageDigest.getInstance("MD5")
        files.forEach { file ->
            digest.update(file.relativeTo(repoRoot).invariantSeparatorsPath.toByteArray(Charsets.UTF_8))
            digest.update(0)
            digest.update(file.readBytes())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
