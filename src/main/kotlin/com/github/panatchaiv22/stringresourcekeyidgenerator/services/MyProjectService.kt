package com.github.panatchaiv22.stringresourcekeyidgenerator.services

import com.intellij.openapi.project.Project
import com.github.panatchaiv22.stringresourcekeyidgenerator.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
