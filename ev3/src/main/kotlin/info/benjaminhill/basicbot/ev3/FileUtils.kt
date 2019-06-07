package info.benjaminhill.basicbot.ev3

import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.BiPredicate

/**
 * Like a recursive glob
 * Some file within a directory has some target string inside it.  We want that directory.
 */
fun findDirectory(
    mustContain: String,
    root: Path = Paths.get("/", "sys", "class"),
    globFilePattern: String = "glob:**",
    maxDepth: Int = 20
): Path {
    check(root.toFile().exists()) { "Can't find a child if root directory doesn't exist: $root" }
    val fileNameFilter = root.fileSystem.getPathMatcher(globFilePattern)!!
    return Files.find(
        root,
        maxDepth,
        BiPredicate { path, basicFileAttributes ->
            basicFileAttributes.isRegularFile && fileNameFilter.matches(path)
        },
        FileVisitOption.FOLLOW_LINKS
    ).filter {
        it.toFile()!!.let { file ->
            file.canRead() &&
                    file.length() in 0..5120 &&
                    file.readText().contains(mustContain)
        }
    }.findFirst().get().toAbsolutePath().parent
}

