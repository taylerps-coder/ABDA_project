package com.restaurant

import upickle.default.{ReadWriter => RW, macroRW}

// ============================================================================
// 17. TRAIT (Example of Scala Trait for simple inheritance/composition)
// ============================================================================
trait DataValidator[T] {
  def validate(item: T): Either[String, T]
}

// ============================================================================
// 13. CASE CLASSES & 2. DATA TYPES
// ============================================================================
// Case class representing the sub-document "address" in sample_restaurants
case class Address(
  building: String = "",
  street: String = "",
  zipcode: String = ""
)

object Address {
  implicit val rw: RW[Address] = macroRW
}

// Case class representing a grade inspection entry in sample_restaurants
case class Grade(
  date: String = "",
  grade: String = "A",
  score: Int = 0
)

object Grade {
  implicit val rw: RW[Grade] = macroRW
}

// Main Case Class representing a Restaurant in MongoDB Atlas
case class Restaurant(
  id: String = "",
  name: String,
  borough: String,
  cuisine: String,
  address: Address = Address(),
  grades: List[Grade] = List.empty,
  score: Double = 0.0
)

object Restaurant {
  implicit val rw: RW[Restaurant] = macroRW
}

// Case classes for Aggregations and Dashboard Analytics
case class CuisineStat(cuisine: String, count: Int)
object CuisineStat { implicit val rw: RW[CuisineStat] = macroRW }

case class BoroughStat(borough: String, count: Int)
object BoroughStat { implicit val rw: RW[BoroughStat] = macroRW }

case class ScoreStat(cuisine: String, avgScore: Double, count: Int)
object ScoreStat { implicit val rw: RW[ScoreStat] = macroRW }

case class DashboardStats(totalRestaurants: Long, totalCuisines: Int, totalBoroughs: Int, averageScore: Double)
object DashboardStats { implicit val rw: RW[DashboardStats] = macroRW }

// ============================================================================
// 12. CLASS & 16. ENCAPSULATION
// ============================================================================
// A validator class that encapsulates validation rules
class RestaurantValidator extends DataValidator[Restaurant] {

  // 16. Encapsulation: private helper method
  private def isValidZip(zip: String): Boolean = {
    // 3. Condition: zip is optional, but if given it should be alphanumeric/length <= 10
    if (zip.trim.isEmpty) true
    else zip.trim.length <= 10
  }

  // 4. Function/method implementing trait validation logic
  override def validate(r: Restaurant): Either[String, Restaurant] = {
    // 1. Variables: immutable value
    val trimmedName = r.name.trim
    val trimmedCuisine = r.cuisine.trim
    val trimmedBorough = r.borough.trim

    // 3. Conditions: Checking required fields
    if (trimmedName.isEmpty) {
      Left("Restaurant name cannot be empty.")
    } else if (trimmedCuisine.isEmpty) {
      Left("Cuisine cannot be empty.")
    } else if (trimmedBorough.isEmpty) {
      Left("Borough cannot be empty.")
    } else if (r.score < 0) {
      Left("Score must be a positive number.")
    } else if (!isValidZip(r.address.zipcode)) {
      Left("ZIP code is invalid.")
    } else {
      Right(r.copy(name = trimmedName, cuisine = trimmedCuisine, borough = trimmedBorough))
    }
  }
}

// ============================================================================
// 14. OBJECT (Companion object with helper utilities)
// ============================================================================
object RestaurantHelper {

  // 1. Variable (var example for demonstration)
  var totalProcessedRecords: Long = 0L

  // 9. Example of Scala Option and 10. Pattern matching
  def formatBorough(boroughOpt: Option[String]): String = {
    boroughOpt match {
      case Some(b) if b.trim.nonEmpty => b.trim.capitalize
      case Some(_)                    => "Unknown"
      case None                       => "Not Specified"
    }
  }

  // 6. Example of map & 7. Example of collection filter
  def filterHighScoreRestaurants(restaurants: List[Restaurant], minScore: Double): List[Restaurant] = {
    // 7. Collection filter: filters restaurants with score >= minScore
    val filtered = restaurants.filter(r => r.score >= minScore)

    // 6. Collection map: transform restaurants to ensure clean names
    filtered.map(r => r.copy(name = r.name.trim))
  }

  // 8. Example of collection groupBy
  def groupRestaurantsByCuisine(restaurants: List[Restaurant]): Map[String, Int] = {
    // 8. Collection groupBy: group by cuisine and count items
    restaurants
      .groupBy(_.cuisine)
      .map { case (cuisine, list) => (cuisine, list.size) }
  }
}
