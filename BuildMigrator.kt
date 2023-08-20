package com.example.unittest

import java.io.File

class BuildMigrator {

    fun migrateDependencies(dependencies: String): MigrateDependenciesResult {
        val versions = ArrayList<String>()
        val libraries = ArrayList<String>()
        val newDependencies = ArrayList<String>()
        val dependenciesList = dependencies.split("\n".toRegex()).toTypedArray()

        for (line in dependenciesList) {
            if(line.isBlank()) continue
            if (line.contains("//") ) newDependencies.add(line.trim())
            else if (line.contains("project(path:"))
                newDependencies.add(line.trim())
            else if (thisLineIsLibraryVersion(line)) // like this def accompanist_version = '0.28.0'
                versions += getVersion(line) + "\n" // --> accompanist_version = '0.28.0'
            else {
                val dependencyConfig =
                    getDependencyConfig(line) // will return testImplementation ( or implementation ( ,etc
                val library =
                    getLibrary(line) // will return io.mockk:mockk:1.12.0 or androidx.compose.ui:ui ,etc

                if (thisLineContainLibraryVersion(library)) {
                    // if input io.mockk:mockk:1.12.0 then return pair("io.mockk:mockk","1.12.0")
                    val (libraryName, libraryVersion) = splitLibraryVersionFromLibraryName(library)

                   val  memberName =   getMemberName(libraryName)// if io.mockk:mockk then mockk-mockk

                    newDependencies += "$dependencyConfig libs." + memberName.replace(
                        "-",
                        "."
                    ) + " )\n"
                    if (libraries.find { it== "$memberName = { module = \"$libraryName\", version.ref = \"$memberName\" }\n" } == null)
                        libraries += "$memberName = { module = \"$libraryName\", version.ref = \"$memberName\" }\n"
                    if (versions.find { it.startsWith(memberName) } == null)
                        versions += "$memberName = \"$libraryVersion\"\n"
                }
                 else if(library.contains("$")){
                     val startIndex =
                         library.lastIndexOf("\\.").coerceAtLeast(library.lastIndexOf("-")) +1
                   val  memberName =  library.substring(startIndex = startIndex).replace(":$","-")
                       .replace(".","-").replace(":","-").replace("_","-")
                    val libraryName = library.substringBefore(":$")
                    newDependencies += "$dependencyConfig libs." + memberName.replace(
                        "-",
                        "."
                    ) + " )\n"
                    if (libraries.find { it == "$memberName = { module = \"$libraryName\"," +
                                " version.ref = \"${library.substringAfter("$")}\" }\n" } == null)
                    libraries += "$memberName = { module = \"$libraryName\"," +
                            " version.ref = \"${library.substringAfter("$")}\" }\n"
                 }

                 else {

                   val  memberName = library.substringAfter(":")
                    newDependencies += "$dependencyConfig libs." + memberName.replace(
                        "-",
                        "."
                    ) + " )\n"
                    if (libraries.find { it == "$memberName = { module = \"$library\"}\n" } == null)

                     libraries += "$memberName = { module = \"$library\"}\n"

                }

            }
        }

        return MigrateDependenciesResult(versions, libraries, newDependencies)
    }

    private fun thisLineIsLibraryVersion(line: String): Boolean = line.contains("=")
    private fun thisLineContainLibraryVersion(line: String): Boolean =
        line.substring(line.lastIndexOf(':') + 1).trim()[0].isDigit()

    private fun getVersion(line: String): String = line.substringAfter("def").trim()
    private fun getDependencyConfig(line: String): String =
        line.substringBefore("\"").substringBefore("\'").substringBefore('(').trim() + " ("

    private fun getLibrary(line: String): String =
        line.substringAfter("\'").substringAfter("\"").substringBefore("\"")
            .substringBefore("\'").trim()

    private fun splitLibraryVersionFromLibraryName(library: String): Pair<String, String> {
        val index = library.lastIndexOf(':')
        val libraryName = library.substring(0, index)
        val libraryVersion = library.substring(index + 1)
        return Pair(libraryName, libraryVersion)
    }
}

private fun getMemberName(libraryName: String): String =
    libraryName.substring(libraryName.lastIndexOf(".") + 1).replace(":", "-")


data class MigrateDependenciesResult(
    val versions: List<String>,
    val libraries: List<String>,
    val dependencies: List<String>
)
fun main() {

    val  result = BuildMigrator().migrateDependencies(dependencies)
    val fileName = "libs.txt"
    val file = File(fileName)

    try {
        file.createNewFile()
        file.writeText("[versions]\n")
        for(i in result.versions){
            file.appendText(i +"\n")

        }
        file.appendText("[libraries]\n")
        for(i in result.libraries)
            file.appendText(i +"\n")
        for (i in result.dependencies)
            file.appendText(i+"\n")
        println( file.absolutePath)
        println("File created and text added successfully.")
    } catch (e: Exception) {
        println("An error occurred: ${e.message}")
    }
}




val dependencies =""