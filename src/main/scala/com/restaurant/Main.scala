package com.restaurant

import java.nio.file.{Files, Paths}
import scala.util.Try

// ============================================================================
// 14. OBJECT: Main Entry Point & Cask HTTP Server
// ============================================================================
object Main extends cask.MainRoutes {

  override def port: Int = sys.env.getOrElse("PORT", "8080").toInt
  override def host: String = "0.0.0.0"

  // Instantiate the service layer
  val service = new RestaurantService()

  // Helper to read frontend files safely from disk
  private def readFrontendFile(fileName: String, contentType: String): cask.Response[String] = {
    // 11. Exception handling for reading files
    Try {
      val path = Paths.get("frontend", fileName)
      if (Files.exists(path)) {
        val content = new String(Files.readAllBytes(path), "UTF-8")
        cask.Response(
          content,
          headers = Seq(
            "Content-Type" -> contentType,
            "Cache-Control" -> "no-cache, no-store, must-revalidate, max-age=0",
            "Pragma" -> "no-cache",
            "Expires" -> "0"
          )
        )
      } else {
        cask.Response(s"File $fileName not found", statusCode = 404)
      }
    }.getOrElse(cask.Response("Error loading file", statusCode = 500))
  }

  // ==========================================================================
  // STATIC FRONTEND ROUTES (One command -> One application)
  // ==========================================================================
  @cask.get("/")
  def index(v: String = ""): cask.Response[String] = {
    readFrontendFile("index.html", "text/html; charset=utf-8")
  }

  @cask.get("/style.css")
  def style(v: String = ""): cask.Response[String] = {
    readFrontendFile("style.css", "text/css; charset=utf-8")
  }

  @cask.get("/theme.css")
  def themeCss(v: String = ""): cask.Response[String] = {
    readFrontendFile("style.css", "text/css; charset=utf-8")
  }

  @cask.get("/app.js")
  def appJs(v: String = ""): cask.Response[String] = {
    readFrontendFile("app.js", "application/javascript; charset=utf-8")
  }

  // ==========================================================================
  // 13. API ENDPOINTS: HEALTH
  // ==========================================================================
  @cask.get("/api/health")
  def health(): cask.Response[String] = {
    val (connected, message) = MongoDB.testConnection()
    val json = ujson.Obj(
      "status" -> (if (connected) "ok" else "error"),
      "mongoConnected" -> connected,
      "message" -> message
    )
    val statusCode = if (connected) 200 else 503
    cask.Response(json.render(), statusCode = statusCode, headers = Seq("Content-Type" -> "application/json"))
  }

  // ==========================================================================
  // 13. API ENDPOINTS: RESTAURANTS (CRUD)
  // ==========================================================================

  private def safeStr(s: String): String = if (s == null) "" else s

  // READ: List all restaurants
  @cask.get("/api/restaurants")
  def getRestaurants(limit: Int = 30): cask.Response[String] = {
    val list = service.getAll(limit)
    val jsonArray = ujson.Arr.from(list.map { r =>
      ujson.Obj(
        "id" -> safeStr(r.id),
        "name" -> safeStr(r.name),
        "cuisine" -> safeStr(r.cuisine),
        "borough" -> safeStr(r.borough),
        "zipcode" -> safeStr(r.address.zipcode),
        "building" -> safeStr(r.address.building),
        "street" -> safeStr(r.address.street),
        "score" -> r.score
      )
    })
    cask.Response(jsonArray.render(), headers = Seq("Content-Type" -> "application/json"))
  }

  // SEARCH AND FILTER: Restaurant Name, Cuisine, Borough, ZIP, Score
  @cask.get("/api/restaurants/search")
  def searchRestaurants(
    name: String = "",
    cuisine: String = "",
    borough: String = "",
    zipcode: String = "",
    minScore: Double = 0.0
  ): cask.Response[String] = {
    val list = service.search(
      name = if (name.trim.nonEmpty) Some(name.trim) else None,
      cuisine = if (cuisine.trim.nonEmpty) Some(cuisine.trim) else None,
      borough = if (borough.trim.nonEmpty) Some(borough.trim) else None,
      zipcode = if (zipcode.trim.nonEmpty) Some(zipcode.trim) else None,
      minScore = if (minScore > 0) Some(minScore) else None
    )

    val jsonArray = ujson.Arr.from(list.map { r =>
      ujson.Obj(
        "id" -> safeStr(r.id),
        "name" -> safeStr(r.name),
        "cuisine" -> safeStr(r.cuisine),
        "borough" -> safeStr(r.borough),
        "zipcode" -> safeStr(r.address.zipcode),
        "score" -> r.score
      )
    })
    cask.Response(jsonArray.render(), headers = Seq("Content-Type" -> "application/json"))
  }

  // CREATE: Add new restaurant
  @cask.post("/api/restaurants")
  def createRestaurant(request: cask.Request): cask.Response[String] = {
    Try {
      val bodyStr = new String(request.readAllBytes(), "UTF-8")
      val parsed = ujson.read(bodyStr)
      val name = parsed.obj.get("name").map(_.str).getOrElse("")
      val cuisine = parsed.obj.get("cuisine").map(_.str).getOrElse("")
      val borough = parsed.obj.get("borough").map(_.str).getOrElse("")
      val zipcode = parsed.obj.get("zipcode").map(_.str).getOrElse("")
      val score: Double = parsed.obj.get("score") match {
        case Some(ujson.Num(num)) => num
        case Some(ujson.Str(str)) => Try(str.toDouble).getOrElse(0.0)
        case _                    => 0.0
      }

      val newRestaurant = Restaurant(
        name = name,
        cuisine = cuisine,
        borough = borough,
        address = Address(zipcode = zipcode),
        score = score
      )

      service.create(newRestaurant)
    } match {
      case scala.util.Success(Right(created)) =>
        val json = ujson.Obj(
          "success" -> true,
          "message" -> "Restaurant added successfully.",
          "restaurant" -> ujson.Obj(
            "id" -> created.id,
            "name" -> created.name,
            "cuisine" -> created.cuisine,
            "borough" -> created.borough,
            "zipcode" -> created.address.zipcode,
            "score" -> created.score
          )
        )
        cask.Response(json.render(), statusCode = 201, headers = Seq("Content-Type" -> "application/json"))
      case scala.util.Success(Left(errorMsg)) =>
        val json = ujson.Obj("success" -> false, "message" -> errorMsg)
        cask.Response(json.render(), statusCode = 400, headers = Seq("Content-Type" -> "application/json"))
      case scala.util.Failure(ex) =>
        val json = ujson.Obj("success" -> false, "message" -> s"Invalid request format: ${ex.getMessage}")
        cask.Response(json.render(), statusCode = 400, headers = Seq("Content-Type" -> "application/json"))
    }
  }

  // UPDATE: Edit existing restaurant
  @cask.put("/api/restaurants/:id")
  def updateRestaurant(id: String, request: cask.Request): cask.Response[String] = {
    Try {
      val bodyStr = new String(request.readAllBytes(), "UTF-8")
      val parsed = ujson.read(bodyStr)
      val name = parsed.obj.get("name").map(_.str).getOrElse("")
      val cuisine = parsed.obj.get("cuisine").map(_.str).getOrElse("")
      val borough = parsed.obj.get("borough").map(_.str).getOrElse("")
      val zipcode = parsed.obj.get("zipcode").map(_.str).getOrElse("")
      val score: Double = parsed.obj.get("score") match {
        case Some(ujson.Num(num)) => num
        case Some(ujson.Str(str)) => Try(str.toDouble).getOrElse(0.0)
        case _                    => 0.0
      }

      service.update(id, name, cuisine, borough, zipcode, score)
    } match {
      case scala.util.Success(Right(updated)) =>
        val json = ujson.Obj(
          "success" -> true,
          "message" -> "Restaurant updated successfully.",
          "restaurant" -> ujson.Obj(
            "id" -> id,
            "name" -> updated.name,
            "cuisine" -> updated.cuisine,
            "borough" -> updated.borough,
            "zipcode" -> updated.address.zipcode,
            "score" -> updated.score
          )
        )
        cask.Response(json.render(), statusCode = 200, headers = Seq("Content-Type" -> "application/json"))
      case scala.util.Success(Left(errorMsg)) =>
        val json = ujson.Obj("success" -> false, "message" -> errorMsg)
        cask.Response(json.render(), statusCode = 400, headers = Seq("Content-Type" -> "application/json"))
      case scala.util.Failure(ex) =>
        val json = ujson.Obj("success" -> false, "message" -> s"Invalid request: ${ex.getMessage}")
        cask.Response(json.render(), statusCode = 400, headers = Seq("Content-Type" -> "application/json"))
    }
  }

  // DELETE: Remove restaurant
  @cask.delete("/api/restaurants/:id")
  def deleteRestaurant(id: String): cask.Response[String] = {
    service.delete(id) match {
      case Right(true) =>
        val json = ujson.Obj("success" -> true, "message" -> s"Restaurant $id deleted successfully.")
        cask.Response(json.render(), statusCode = 200, headers = Seq("Content-Type" -> "application/json"))
      case Right(false) =>
        val json = ujson.Obj("success" -> false, "message" -> s"Restaurant $id could not be deleted.")
        cask.Response(json.render(), statusCode = 404, headers = Seq("Content-Type" -> "application/json"))
      case Left(errorMsg) =>
        val json = ujson.Obj("success" -> false, "message" -> errorMsg)
        cask.Response(json.render(), statusCode = 400, headers = Seq("Content-Type" -> "application/json"))
    }
  }

  // ==========================================================================
  // 13. API ENDPOINTS: ANALYTICS
  // ==========================================================================

  // Aggregation 1: Grouped by cuisine
  @cask.get("/api/analytics/cuisine")
  def analyticsCuisine(limit: Int = 10): cask.Response[String] = {
    val results = service.aggregateByCuisine(limit)
    val arr = ujson.Arr.from(results.map { item =>
      ujson.Obj(
        "cuisine" -> safeStr(item.cuisine),
        "count" -> item.count
      )
    })
    cask.Response(arr.render(), headers = Seq("Content-Type" -> "application/json"))
  }

  // Aggregation 2: Grouped by borough
  @cask.get("/api/analytics/borough")
  def analyticsBorough(): cask.Response[String] = {
    val results = service.aggregateByBorough()
    val arr = ujson.Arr.from(results.map { item =>
      ujson.Obj(
        "borough" -> safeStr(item.borough),
        "count" -> item.count
      )
    })
    cask.Response(arr.render(), headers = Seq("Content-Type" -> "application/json"))
  }

  // Aggregation 3: Average score by cuisine
  @cask.get("/api/analytics/average-score")
  def analyticsAverageScore(limit: Int = 10): cask.Response[String] = {
    val results = service.aggregateAverageScoreByCuisine(limit)
    val arr = ujson.Arr.from(results.map { item =>
      ujson.Obj(
        "cuisine" -> safeStr(item.cuisine),
        "avgScore" -> item.avgScore,
        "count" -> item.count
      )
    })
    cask.Response(arr.render(), headers = Seq("Content-Type" -> "application/json"))
  }

  // ==========================================================================
  // 13. API ENDPOINTS: DASHBOARD
  // ==========================================================================
  @cask.get("/api/dashboard")
  def dashboard(): cask.Response[String] = {
    val stats = service.getDashboardStats()
    val json = ujson.Obj(
      "totalRestaurants" -> stats.totalRestaurants,
      "totalCuisines" -> stats.totalCuisines,
      "totalBoroughs" -> stats.totalBoroughs,
      "averageScore" -> stats.averageScore
    )
    cask.Response(json.render(), headers = Seq("Content-Type" -> "application/json"))
  }

  // ==========================================================================
  // 13. API ENDPOINTS: INDEXES
  // ==========================================================================
  @cask.get("/api/indexes")
  def getIndexes(): cask.Response[String] = {
    val indexes = service.listIndexes()
    val json = ujson.Obj(
      "indexes" -> ujson.Arr.from(indexes.map(ujson.Str(_)))
    )
    cask.Response(json.render(), headers = Seq("Content-Type" -> "application/json"))
  }

  @cask.post("/api/indexes")
  def createIndexes(): cask.Response[String] = {
    val created = service.createRequiredIndexes()
    val json = ujson.Obj(
      "success" -> true,
      "message" -> "Required indexes created successfully.",
      "created" -> ujson.Arr.from(created.map(ujson.Str(_)))
    )
    cask.Response(json.render(), headers = Seq("Content-Type" -> "application/json"))
  }

  // ==========================================================================
  // 21. STARTUP BANNER
  // ==========================================================================
  println("==================================================")
  println("Restaurant Discovery & Analytics System")
  println("Course: Advanced Big Data Analytics (ABDA)")
  println("--------------------------------------------------")

  val (isConnected, statusMsg) = MongoDB.testConnection()
  if (isConnected) {
    println("MongoDB Atlas: Connected")
    println(s"Server: http://localhost:$port")
  } else {
    println("MongoDB Atlas: Connection Failed")
    println(s"$statusMsg")
    println(s"Server running in offline/read-only mode: http://localhost:$port")
  }
  println("==================================================")

  initialize()
}
