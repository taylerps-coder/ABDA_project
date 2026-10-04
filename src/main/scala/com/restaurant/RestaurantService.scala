package com.restaurant

import org.bson.types.ObjectId
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Indexes._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model.{Accumulators, Aggregates, Sorts}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

// ============================================================================
// 12. CLASS & 16. ENCAPSULATION: Restaurant Service
// ============================================================================
class RestaurantService(val validator: DataValidator[Restaurant] = new RestaurantValidator()) {

  private def extractBsonString(key: String, doc: Document, default: String = ""): String = {
    doc.get(key) match {
      case Some(v) if v != null && v.isString => v.asString().getValue
      case _ => default
    }
  }

  // 16. Encapsulation: private helper to convert BSON Document to Restaurant case class
  private def docToRestaurant(doc: Document): Restaurant = {
    // 1. Variable extraction with safe defaults
    val idStr = doc.get("_id") match {
      case Some(oid: org.mongodb.scala.bson.BsonObjectId) => oid.getValue.toHexString
      case Some(str: org.mongodb.scala.bson.BsonString)   => str.getValue
      case Some(other) if other != null                   => other.toString
      case _                                              => ""
    }

    val name = extractBsonString("name", doc, "")
    val borough = extractBsonString("borough", doc, "Unknown")
    val cuisine = extractBsonString("cuisine", doc, "Other")

    // Extract address sub-document safely
    // 9. Example of Scala Option
    val address = doc.get("address").flatMap { bson =>
      if (bson != null && bson.isDocument) {
        val addrDoc = bson.asDocument()
        val bldg = if (addrDoc.containsKey("building") && addrDoc.get("building").isString) addrDoc.getString("building").getValue else ""
        val strt = if (addrDoc.containsKey("street") && addrDoc.get("street").isString) addrDoc.getString("street").getValue else ""
        val zip = if (addrDoc.containsKey("zipcode") && addrDoc.get("zipcode").isString) addrDoc.getString("zipcode").getValue else ""
        Some(Address(
          building = Option(bldg).getOrElse(""),
          street = Option(strt).getOrElse(""),
          zipcode = Option(zip).getOrElse("")
        ))
      } else None
    }.getOrElse(Address())

    // Extract grades array safely
    val gradesList: List[Grade] = doc.get[org.mongodb.scala.bson.BsonArray]("grades").map { arr =>
      // 6. Collection map: transform BSON array to List of Grade case classes
      arr.getValues.asScala.toList.flatMap { item =>
        if (item.isDocument) {
          val gDoc = item.asDocument()
          val gradeChar = if (gDoc.containsKey("grade") && gDoc.get("grade").isString) gDoc.getString("grade").getValue else "A"
          val scoreVal = if (gDoc.containsKey("score") && gDoc.get("score").isInt32) {
            gDoc.getInt32("score").getValue
          } else if (gDoc.containsKey("score") && gDoc.get("score").isInt64) {
            gDoc.getInt64("score").getValue.toInt
          } else if (gDoc.containsKey("score") && gDoc.get("score").isDouble) {
            gDoc.getDouble("score").getValue.toInt
          } else {
            0
          }
          Some(Grade(grade = gradeChar, score = scoreVal))
        } else None
      }
    }.getOrElse(List.empty)

    // Calculate effective score: average of grades, or direct score field if present
    val calculatedScore: Double = if (gradesList.nonEmpty) {
      val validScores = gradesList.map(_.score).filter(_ > 0)
      if (validScores.nonEmpty) validScores.sum.toDouble / validScores.length
      else 0.0
    } else if (doc.containsKey("score")) {
      // 10. Pattern matching on score type
      doc.get("score") match {
        case Some(d: org.mongodb.scala.bson.BsonDouble) => d.getValue
        case Some(i: org.mongodb.scala.bson.BsonInt32)  => i.getValue.toDouble
        case Some(l: org.mongodb.scala.bson.BsonInt64)  => l.getValue.toDouble
        case _                                          => 0.0
      }
    } else {
      0.0
    }

    Restaurant(
      id = idStr,
      name = name,
      borough = borough,
      cuisine = cuisine,
      address = address,
      grades = gradesList,
      score = math.round(calculatedScore * 10.0) / 10.0 // Round to 1 decimal place
    )
  }

  // 16. Encapsulation: helper to build ObjectId filter safely
  private def idFilter(id: String) = {
    if (ObjectId.isValid(id)) {
      equal("_id", new ObjectId(id))
    } else {
      equal("_id", id)
    }
  }

  // ==========================================================================
  // 8. CRUD: READ (Get all / recent restaurants)
  // ==========================================================================
  def getAll(limit: Int = 30): Seq[Restaurant] = {
    MongoDB.getCollection match {
      case Some(coll) =>
        val docs = MongoDB.sync(coll.find().limit(limit))
        // 6. Collection map: transform documents into Restaurant instances
        docs.map(docToRestaurant)
      case None =>
        Seq.empty
    }
  }

  // Get single restaurant by ID
  def getById(id: String): Option[Restaurant] = {
    MongoDB.getCollection.flatMap { coll =>
      val docOpt = MongoDB.syncOne(coll.find(idFilter(id)).first())
      docOpt.map(docToRestaurant)
    }
  }

  // ==========================================================================
  // 9. SEARCH AND FILTER
  // ==========================================================================
  def search(
    name: Option[String],
    cuisine: Option[String],
    borough: Option[String],
    zipcode: Option[String],
    minScore: Option[Double],
    limit: Int = 40
  ): Seq[Restaurant] = {
    MongoDB.getCollection match {
      case Some(coll) =>
        // 5. Collections: Building filters list dynamically
        var filters = List.empty[org.bson.conversions.Bson]

        // 9. Option handling: Only add filter if provided and non-empty
        name.filter(_.trim.nonEmpty).foreach { n =>
          // Case-insensitive regex search
          filters = regex("name", s"(?i).*${java.util.regex.Pattern.quote(n.trim)}.*") :: filters
        }

        cuisine.filter(_.trim.nonEmpty).foreach { c =>
          filters = regex("cuisine", s"(?i).*${java.util.regex.Pattern.quote(c.trim)}.*") :: filters
        }

        borough.filter(_.trim.nonEmpty).foreach { b =>
          filters = regex("borough", s"(?i).*${java.util.regex.Pattern.quote(b.trim)}.*") :: filters
        }

        zipcode.filter(_.trim.nonEmpty).foreach { z =>
          filters = equal("address.zipcode", z.trim) :: filters
        }

        // 3. Condition check for score
        minScore.filter(_ > 0).foreach { s =>
          // Filter either by grades.score or top-level score
          filters = or(gte("grades.score", s.toInt), gte("score", s)) :: filters
        }

        val combinedFilter = if (filters.isEmpty) Document() else and(filters: _*)
        val docs = MongoDB.sync(coll.find(combinedFilter).limit(limit))
        val restaurants = docs.map(docToRestaurant)

        // 7. Filter: in-memory refinement for minimum score if specified
        minScore match {
          case Some(s) if s > 0 => restaurants.filter(_.score >= s)
          case _                => restaurants
        }

      case None =>
        Seq.empty
    }
  }

  // ==========================================================================
  // 8. CRUD: CREATE
  // ==========================================================================
  def create(r: Restaurant): Either[String, Restaurant] = {
    // Validate input first
    validator.validate(r) match {
      case Left(error) => Left(error)
      case Right(validR) =>
        MongoDB.getCollection match {
          case Some(coll) =>
            // 11. Exception handling around MongoDB insert
            Try {
              val newId = new ObjectId()
              val doc = Document(
                "_id" -> newId,
                "name" -> validR.name,
                "cuisine" -> validR.cuisine,
                "borough" -> validR.borough,
                "address" -> Document(
                  "building" -> validR.address.building,
                  "street" -> validR.address.street,
                  "zipcode" -> validR.address.zipcode
                ),
                "grades" -> List(
                  Document(
                    "grade" -> "A",
                    "score" -> validR.score.toInt
                  )
                ),
                "score" -> validR.score
              )

              MongoDB.syncOne(coll.insertOne(doc))
              validR.copy(id = newId.toHexString)
            } match {
              case Success(created) => Right(created)
              case Failure(ex)      => Left(s"Failed to create restaurant: ${ex.getMessage}")
            }
          case None =>
            Left("MongoDB connection failed. Please check MONGODB_URI in .env.")
        }
    }
  }

  // ==========================================================================
  // 8. CRUD: UPDATE
  // ==========================================================================
  def update(
    id: String,
    name: String,
    cuisine: String,
    borough: String,
    zipcode: String,
    score: Double
  ): Either[String, Restaurant] = {
    val dummy = Restaurant(
      id = id,
      name = name,
      cuisine = cuisine,
      borough = borough,
      address = Address(zipcode = zipcode),
      score = score
    )

    validator.validate(dummy) match {
      case Left(error) => Left(error)
      case Right(validR) =>
        MongoDB.getCollection match {
          case Some(coll) =>
            Try {
              val updateOps = combine(
                set("name", validR.name),
                set("cuisine", validR.cuisine),
                set("borough", validR.borough),
                set("address.zipcode", validR.address.zipcode),
                set("score", validR.score),
                // Also update the latest grade score
                set("grades.0.score", validR.score.toInt)
              )

              val res = MongoDB.syncOne(coll.updateOne(idFilter(id), updateOps))
              res match {
                case Some(updateResult) if updateResult.getMatchedCount > 0 =>
                  Right(validR)
                case _ =>
                  Left(s"Restaurant with ID $id not found.")
              }
            } match {
              case Success(res) => res
              case Failure(ex)  => Left(s"Failed to update restaurant: ${ex.getMessage}")
            }
          case None =>
            Left("MongoDB connection failed. Please check MONGODB_URI in .env.")
        }
    }
  }

  // ==========================================================================
  // 8. CRUD: DELETE
  // ==========================================================================
  def delete(id: String): Either[String, Boolean] = {
    MongoDB.getCollection match {
      case Some(coll) =>
        Try {
          val res = MongoDB.syncOne(coll.deleteOne(idFilter(id)))
          res match {
            case Some(deleteResult) if deleteResult.getDeletedCount > 0 =>
              Right(true)
            case _ =>
              Left(s"Restaurant with ID $id not found or already deleted.")
          }
        } match {
          case Success(res) => res
          case Failure(ex)  => Left(s"Failed to delete restaurant: ${ex.getMessage}")
        }
      case None =>
        Left("MongoDB connection failed. Please check MONGODB_URI in .env.")
    }
  }

  // ==========================================================================
  // 10. INDEXING
  // ==========================================================================
  // Required indexes:
  // 1. name_1 (ascending on name)
  // 2. cuisine_1 (ascending on cuisine)
  // 3. borough_1_cuisine_1 (compound index on borough + cuisine)
  def createRequiredIndexes(): Seq[String] = {
    MongoDB.getCollection match {
      case Some(coll) =>
        val index1 = MongoDB.syncOne(coll.createIndex(ascending("name")))
        val index2 = MongoDB.syncOne(coll.createIndex(ascending("cuisine")))
        val index3 = MongoDB.syncOne(coll.createIndex(compoundIndex(ascending("borough"), ascending("cuisine"))))
        Seq(
          index1.getOrElse("name_1"),
          index2.getOrElse("cuisine_1"),
          index3.getOrElse("borough_1_cuisine_1")
        )
      case None =>
        Seq.empty
    }
  }

  def listIndexes(): Seq[String] = {
    MongoDB.getCollection match {
      case Some(coll) =>
        val docs = MongoDB.sync(coll.listIndexes())
        // 6. Collection map: extract index names
        docs.flatMap { doc =>
          if (doc.containsKey("name")) Some(doc.getString("name"))
          else None
        }
      case None =>
        Seq.empty
    }
  }

  // ==========================================================================
  // 11. AGGREGATIONS
  // ==========================================================================
  // 1. Grouped by cuisine
  def aggregateByCuisine(limit: Int = 10): Seq[CuisineStat] = {
    MongoDB.getCollection match {
      case Some(coll) =>
        val pipeline = Seq(
          Aggregates.filter(notEqual("cuisine", "")),
          Aggregates.group("$cuisine", Accumulators.sum("count", 1)),
          Aggregates.sort(Sorts.descending("count")),
          Aggregates.limit(limit)
        )
        val docs = MongoDB.sync(coll.aggregate(pipeline))
        docs.map { d =>
          val rawCuisine = d.get("_id") match {
            case Some(s: org.mongodb.scala.bson.BsonString) => s.getValue
            case Some(other) if other != null               => other.toString
            case _                                          => "Unknown"
          }
          val countVal: Int = d.get("count") match {
            case Some(i: org.mongodb.scala.bson.BsonInt32)  => i.getValue
            case Some(l: org.mongodb.scala.bson.BsonInt64)  => l.getValue.toInt
            case Some(d: org.mongodb.scala.bson.BsonDouble) => d.getValue.toInt
            case _                                          => 0
          }
          CuisineStat(cuisine = if (rawCuisine == null || rawCuisine.trim.isEmpty) "Other" else rawCuisine.trim, count = countVal)
        }
      case None => Seq.empty
    }
  }

  // 2. Grouped by borough
  def aggregateByBorough(): Seq[BoroughStat] = {
    MongoDB.getCollection match {
      case Some(coll) =>
        val pipeline = Seq(
          Aggregates.filter(notEqual("borough", "Missing")),
          Aggregates.group("$borough", Accumulators.sum("count", 1)),
          Aggregates.sort(Sorts.descending("count"))
        )
        val docs = MongoDB.sync(coll.aggregate(pipeline))
        docs.map { d =>
          val rawBorough = d.get("_id") match {
            case Some(s: org.mongodb.scala.bson.BsonString) => s.getValue
            case Some(other) if other != null               => other.toString
            case _                                          => "Unknown"
          }
          val countVal: Int = d.get("count") match {
            case Some(i: org.mongodb.scala.bson.BsonInt32)  => i.getValue
            case Some(l: org.mongodb.scala.bson.BsonInt64)  => l.getValue.toInt
            case Some(d: org.mongodb.scala.bson.BsonDouble) => d.getValue.toInt
            case _                                          => 0
          }
          BoroughStat(borough = if (rawBorough == null || rawBorough.trim.isEmpty) "Unknown" else rawBorough.trim, count = countVal)
        }
      case None => Seq.empty
    }
  }

  // 3. Average score by cuisine
  def aggregateAverageScoreByCuisine(limit: Int = 10): Seq[ScoreStat] = {
    MongoDB.getCollection match {
      case Some(coll) =>
        val pipeline = Seq(
          Aggregates.unwind("$grades"),
          Aggregates.filter(and(notEqual("cuisine", ""), gte("grades.score", 0))),
          Aggregates.group("$cuisine", Accumulators.avg("avgScore", "$grades.score"), Accumulators.sum("count", 1)),
          Aggregates.sort(Sorts.descending("avgScore")),
          Aggregates.limit(limit)
        )
        val docs = MongoDB.sync(coll.aggregate(pipeline))
        docs.map { d =>
          val rawCuisine = d.get("_id") match {
            case Some(s: org.mongodb.scala.bson.BsonString) => s.getValue
            case Some(other) if other != null               => other.toString
            case _                                          => "Unknown"
          }
          val avgDouble: Double = d.get("avgScore") match {
            case Some(d: org.mongodb.scala.bson.BsonDouble) => d.getValue
            case Some(i: org.mongodb.scala.bson.BsonInt32)  => i.getValue.toDouble
            case Some(l: org.mongodb.scala.bson.BsonInt64)  => l.getValue.toDouble
            case _                                          => 0.0
          }
          val countVal: Int = d.get("count") match {
            case Some(i: org.mongodb.scala.bson.BsonInt32)  => i.getValue
            case Some(l: org.mongodb.scala.bson.BsonInt64)  => l.getValue.toInt
            case Some(d: org.mongodb.scala.bson.BsonDouble) => d.getValue.toInt
            case _                                          => 0
          }
          ScoreStat(
            cuisine = if (rawCuisine == null || rawCuisine.trim.isEmpty) "Other" else rawCuisine.trim,
            avgScore = math.round(avgDouble * 10.0) / 10.0,
            count = countVal
          )
        }
      case None => Seq.empty
    }
  }

  // Dashboard Overview Metrics
  def getDashboardStats(): DashboardStats = {
    MongoDB.getCollection match {
      case Some(coll) =>
        val totalCount = MongoDB.syncOne(coll.countDocuments()).getOrElse(0L)
        val cuisineCount = MongoDB.sync(coll.distinct[String]("cuisine")).size
        val boroughCount = MongoDB.sync(coll.distinct[String]("borough")).size

        // Calculate sample average score
        val avgPipeline = Seq(
          Aggregates.unwind("$grades"),
          Aggregates.group(null, Accumulators.avg("overallAvg", "$grades.score"))
        )
        val avgDoc = MongoDB.syncOne(coll.aggregate(avgPipeline))
        val overallAvg = avgDoc.flatMap { d =>
          d.get("overallAvg") match {
            case Some(d: org.mongodb.scala.bson.BsonDouble) => Some(math.round(d.getValue * 10.0) / 10.0)
            case Some(i: org.mongodb.scala.bson.BsonInt32)  => Some(i.getValue.toDouble)
            case _                                          => None
          }
        }.getOrElse(0.0)

        DashboardStats(
          totalRestaurants = totalCount,
          totalCuisines = cuisineCount,
          totalBoroughs = boroughCount,
          averageScore = overallAvg
        )
      case None =>
        DashboardStats(
          totalRestaurants = 0L,
          totalCuisines = 0,
          totalBoroughs = 0,
          averageScore = 0.0
        )
    }
  }
}
