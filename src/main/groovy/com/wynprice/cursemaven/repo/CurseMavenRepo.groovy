package com.wynprice.cursemaven.repo


import com.wynprice.cursemaven.CurseMavenPlugin
import com.wynprice.cursemaven.CurseMavenResolver
import groovy.transform.TupleConstructor
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

import java.nio.file.Path
import java.nio.file.Paths

/**
 * The repo class used to control the caching and checking of dependencies
 * @author Wyn Price
 * @deprecated As of 1.3.0. The string based version is much better, and happenes at repository time.
 */
@Deprecated
@TupleConstructor class CurseMavenRepo  {

    /**
     * The group name that all of the artifacts have
     */
    static final String GROUP_NAME = "curse_gradle"

    private static CurseMavenRepo INSTANCE

    /**
     * The root path of the repo. Is the same as $GRADLE_USER_HOME/curse_gradle
     */
    Path baseDir

    /**
     * Initializes the repo, setting up the actual repository maven maven and setting the instance
     * @param project the project to initialize from
     */
    static void initialize(Project project) {
        def path = Paths.get(project.gradle.gradleUserHomeDir.path, "caches")

        //Setup the maven
        project.repositories.maven { MavenArtifactRepository repo ->
            repo.name = GROUP_NAME
            repo.url = path.toUri()
        }

        //Set the instance
        INSTANCE = new CurseMavenRepo(path.resolve(GROUP_NAME))
    }

    static CurseMavenRepo getInstance() {
        return INSTANCE
    }

    /**
     * Checks to see if the dependency is already cached. If so, then it won't re-download the dep
     * @param dep the dependency to check
     * @return whether the dependency already exists on the local cache repo
     */
    boolean isDepCached(CurseRepoDependency dep) {
        dep.getFile(this.baseDir).exists()
    }

    /**
     * Puts a dependency on this repo.
     * @param dep The depedency to download and place
     */
    void putDep(CurseRepoDependency dep) {
        //Make sure that the parent folders exist
        dep.getFolder(this.baseDir).mkdirs()

        //Download and place the main file
        dep.getFile(this.baseDir).withOutputStream {
            it.write getBytes("$CurseMavenPlugin.EXTENDED_CURSEFORGE_URL/${dep.slug}/download/${dep.fileID}/file")
        }

        //If there is sources, download and place those sources
        if(dep.sourcesFileID) {
            dep.getFile(this.baseDir, "sources").withOutputStream {
                it.write getBytes("$CurseMavenPlugin.EXTENDED_CURSEFORGE_URL/${dep.slug}/download/${dep.sourcesFileID}/file")
            }
        }
    }

    /**
     * Gets the bytes from a url
     * @param url the url to get the bytes from
     * @return the byte array of gotten from the url
     */
    static byte[] getBytes(String url) {
        println "[CurseMavenRepo] Getting binary at $url"
        CurseMavenPlugin.getPage(url).getWebResponse().getContentAsStream().bytes
    }
}
