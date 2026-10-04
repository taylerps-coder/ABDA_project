package com.restaurant

import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success, Try}

// ============================================================================
// 14. OBJECT: MongoDB Connection Manager
// ============================================================================
object MongoDB {

  // 16. Encapsulation: private variable to read .env file
  private def loadEnvVariable(key: String): Option[String] = {
    // Check system environment variables first
    // 9. Example of Scala Option
    sys.env.get(key).orElse {
      val envFile = new File(".env")
      if (envFile.exists()) {
        // 11. Exception/error handling with Try
        Try {
          val source = Source.fromFile(envFile)
          val lines = source.getLines().toList
          source.close()
          lines
            .map(_.trim)
            // 7. Collection filter: ignore comments and empty lines
            .filterNot(line => line.isEmpty || line.startsWith("#"))
            // 6. Collection map: split by '='
            .flatMap { line =>
              val parts = line.split("=", 2)
              if (parts.length == 2) Some(parts(0).trim -> parts(1).trim)
              else None
            }
            .toMap
            .get(key)
        }.getOrElse(None)
      } else {
        None
      }
    }
  }

  // Load the connection string from .env or system environment
  val uriOption: Option[String] = loadEnvVariable("MONGODB_URI")

  // Database and collection names as per assignment requirement
  val databaseName: String = "sample_restaurants"
  val collectionName: String = "restaurants"

  // Lazy client initialization to avoid crashing at startup if URI is not yet configured
  private lazy val clientAndDb: Option[(MongoClient, MongoDatabase, MongoCollection[Document])] = {
    // 10. Example of pattern matching
    uriOption match {
      case Some(uri) if uri.nonEmpty && !uri.contains("YOUR_MONGODB_ATLAS_CONNECTION_STRING") =>
        // 11. Exception handling for connection parsing
        Try {
          val client = MongoClient(uri)
          val db = client.getDatabase(databaseName)
          val coll = db.getCollection(collectionName)
          (client, db, coll)
        } match {
          case Success(bundle) => Some(bundle)
          case Failure(ex) =>
            System.err.println(s"[MongoDB] Failed to initialize client: ${ex.getMessage}")
            None
        }
      case _ =>
        None
    }
  }

  def isConfigured: Boolean = clientAndDb.isDefined

  def getCollection: Option[MongoCollection[Document]] = clientAndDb.map(_._3)
  def getDatabase: Option[MongoDatabase] = clientAndDb.map(_._2)

  // ============================================================================
  // Helper to execute MongoDB Observables synchronously (beginner-friendly)
  // ============================================================================
  def sync[T](obs: Observable[T], timeout: Duration = 10.seconds): Seq[T] = {
    Try(Await.result(obs.toFuture(), timeout)) match {
      case Success(res) => res
      case Failure(ex) =>
        System.err.println(s"[MongoDB Error] Operation failed: ${ex.getMessage}")
        Seq.empty
    }
  }

  def syncOne[T](obs: Observable[T], timeout: Duration = 10.seconds): Option[T] = {
    Try(Await.result(obs.headOption(), timeout)) match {
      case Success(res) => res
      case Failure(ex) =>
        System.err.println(s"[MongoDB Error] Operation failed: ${ex.getMessage}")
        None
    }
  }

  // ============================================================================
  // Connection Test with graceful error message
  // ============================================================================
  def testConnection(): (Boolean, String) = {
    // 10. Pattern matching on clientAndDb
    clientAndDb match {
      case None =>
        (false, "MongoDB connection failed. Please check MONGODB_URI in .env.")
      case Some((_, db, _)) =>
        // 11. Try/Catch block for MongoDB ping
        Try {
          val pingDoc = Document("ping" -> 1)
          val result = Await.result(db.runCommand(pingDoc).toFuture(), 6.seconds)
          result
        } match {
          case Success(_) =>
            (true, "Connected to MongoDB Atlas successfully.")
          case Failure(ex) =>
            val msg = if (ex.getMessage != null && ex.getMessage.contains("authentication")) {
              "MongoDB authentication failed. Check your username/password in MONGODB_URI in .env."
            } else if (ex.getMessage != null && ex.getMessage.contains("timed out")) {
              "MongoDB connection timed out. Check Network Access (IP Whitelist) in Atlas."
            } else {
              "MongoDB connection failed. Please check MONGODB_URI in .env."
            }
            (false, msg)
        }
    }
  }
}
