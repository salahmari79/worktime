package com.example.mywork.desktop

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class WorkTrackerApp : Application() {
    override fun start(primaryStage: Stage) {
        val loader = FXMLLoader(javaClass.getResource("/fxml/main.fxml"))
        val root: Parent = loader.load()
        
        primaryStage.title = "Work Tracker"
        primaryStage.scene = Scene(root)
        primaryStage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(WorkTrackerApp::class.java, *args)
        }
    }
} 