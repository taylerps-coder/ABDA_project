package com.restaurant

class RestaurantTest extends munit.FunSuite {

  val validator = new RestaurantValidator()

  // Test 1: Model creation and default fields
  test("Restaurant model should hold correct fields") {
    val r = Restaurant(
      id = "12345",
      name = "Shake Shack",
      borough = "Manhattan",
      cuisine = "American",
      address = Address(building = "11", street = "Madison Ave", zipcode = "10010"),
      score = 14.5
    )

    assertEquals(r.name, "Shake Shack")
    assertEquals(r.cuisine, "American")
    assertEquals(r.borough, "Manhattan")
    assertEquals(r.address.zipcode, "10010")
    assertEquals(r.score, 14.5)
  }

  // Test 2: Validation passes for valid restaurant
  test("Validator should approve valid restaurant data") {
    val r = Restaurant(
      name = "Joe's Pizza",
      borough = "Manhattan",
      cuisine = "Pizza",
      address = Address(zipcode = "10014"),
      score = 10.0
    )

    val result = validator.validate(r)
    assert(result.isRight)
    assertEquals(result.toOption.get.name, "Joe's Pizza")
  }

  // Test 3: Validation fails for empty name
  test("Validator should reject empty restaurant name") {
    val r = Restaurant(
      name = "   ",
      borough = "Brooklyn",
      cuisine = "Bakery",
      score = 5.0
    )

    val result = validator.validate(r)
    assert(result.isLeft)
    assertEquals(result.left.toOption.get, "Restaurant name cannot be empty.")
  }

  // Test 4: Validation fails for empty cuisine
  test("Validator should reject empty cuisine") {
    val r = Restaurant(
      name = "Taco Bell",
      borough = "Queens",
      cuisine = "",
      score = 5.0
    )

    val result = validator.validate(r)
    assert(result.isLeft)
    assertEquals(result.left.toOption.get, "Cuisine cannot be empty.")
  }

  // Test 5: Validation fails for empty borough
  test("Validator should reject empty borough") {
    val r = Restaurant(
      name = "Chipotle",
      borough = "",
      cuisine = "Mexican",
      score = 8.0
    )

    val result = validator.validate(r)
    assert(result.isLeft)
    assertEquals(result.left.toOption.get, "Borough cannot be empty.")
  }

  // Test 6: Validation fails for negative score
  test("Validator should reject negative score") {
    val r = Restaurant(
      name = "Dunkin",
      borough = "Bronx",
      cuisine = "Donuts",
      score = -2.0
    )

    val result = validator.validate(r)
    assert(result.isLeft)
    assertEquals(result.left.toOption.get, "Score must be a positive number.")
  }

  // Test 7: Helper Option and pattern matching logic
  test("RestaurantHelper.formatBorough handles Option cases correctly") {
    assertEquals(RestaurantHelper.formatBorough(Some("brooklyn")), "Brooklyn")
    assertEquals(RestaurantHelper.formatBorough(Some("   ")), "Unknown")
    assertEquals(RestaurantHelper.formatBorough(None), "Not Specified")
  }

  // Test 8: Collection filter and map logic
  test("RestaurantHelper.filterHighScoreRestaurants filters and maps correctly") {
    val list = List(
      Restaurant(name = " Place A ", borough = "Manhattan", cuisine = "Cafe", score = 15.0),
      Restaurant(name = " Place B ", borough = "Queens", cuisine = "Bakery", score = 8.0),
      Restaurant(name = " Place C ", borough = "Brooklyn", cuisine = "Cafe", score = 20.0)
    )

    val filtered = RestaurantHelper.filterHighScoreRestaurants(list, 10.0)
    assertEquals(filtered.size, 2)
    assertEquals(filtered.head.name, "Place A") // trimmed
    assertEquals(filtered.last.name, "Place C") // trimmed
  }

  // Test 9: Collection groupBy logic
  test("RestaurantHelper.groupRestaurantsByCuisine groups items properly") {
    val list = List(
      Restaurant(name = "R1", borough = "Manhattan", cuisine = "Italian", score = 10.0),
      Restaurant(name = "R2", borough = "Queens", cuisine = "Italian", score = 12.0),
      Restaurant(name = "R3", borough = "Brooklyn", cuisine = "Chinese", score = 9.0)
    )

    val grouped = RestaurantHelper.groupRestaurantsByCuisine(list)
    assertEquals(grouped.getOrElse("Italian", 0), 2)
    assertEquals(grouped.getOrElse("Chinese", 0), 1)
  }
}
