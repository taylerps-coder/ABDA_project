# Restaurant Discovery & Analytics System

**Course:** Advanced Big Data Analytics (ABDA)  
**Assignment:** Final Coursework Project  
**Technology Stack:** Scala 3 &bull; MongoDB Atlas &bull; MongoDB Scala Driver &bull; Vanilla HTML/CSS/JavaScript  

---

## 1. Project Title
**Restaurant Discovery & Analytics System**  
A full-stack, distributed database analytics platform built from scratch to demonstrate real-time CRUD operations, multi-field filtering, compound indexing, and aggregation pipelines on MongoDB Atlas.

---

## 2. Aim
The primary aim of this project is to implement a robust, lightweight, and beginner-friendly Big Data application that connects a Scala 3 backend directly to a cloud-hosted MongoDB Atlas cluster without unnecessary abstractions or complex enterprise frameworks.

The system demonstrates:
- End-to-end integration between Scala and cloud NoSQL databases.
- Execution of distributed aggregation pipelines (`$group`, `$sort`, `$unwind`, `$avg`).
- Performance tuning using B-Tree single and compound indexing (`name_1`, `cuisine_1`, `borough_1_cuisine_1`).
- Clean functional and object-oriented programming concepts in Scala 3.

---

## 3. Scenario
City health inspectors and food enthusiasts in New York need a fast, centralized system to explore restaurants, query inspection safety scores, and analyze culinary patterns across boroughs. This system interfaces with the official MongoDB `sample_restaurants` dataset to deliver real-time discovery and analytical summaries.

---

## 4. Technologies
- **Programming Language:** Scala 3.3.3 (LTS)
- **Build Tool:** sbt 1.10.7
- **Database:** MongoDB Atlas (Cloud NoSQL DB)
- **Database Driver:** Official MongoDB Scala Driver (5.1.0)
- **HTTP Web Server:** Cask (Lightweight, beginner-friendly Scala HTTP server)
- **Serialization:** uPickle & uJson (Fast, zero-boilerplate JSON library)
- **Frontend:** Vanilla HTML5, CSS3, JavaScript (Fetch API) &mdash; *Zero Node.js, React, or Vite required*
- **Testing Framework:** MUnit 1.0.0
- **GUI Database Viewer:** MongoDB Compass

---

## 5. Architecture

```
+-----------------------------------------------------------+
|                   Browser Client                          |
|         (Single Page Application: HTML / CSS / JS)        |
+-----------------------------------------------------------+
                             |
                     HTTP / JSON REST API
                             v
+-----------------------------------------------------------+
|                   Scala 3 Backend                         |
|   +---------------------------------------------------+   |
|   |  Main.scala (Cask HTTP Server on port 8080)       |   |
|   +---------------------------------------------------+   |
|   |  RestaurantService.scala (CRUD, Filter, Analytics)|   |
|   +---------------------------------------------------+   |
|   |  MongoDB.scala (Atlas Connection & Observables)   |   |
|   +---------------------------------------------------+   |
|   |  Restaurant.scala (Case Classes & Validation)     |   |
|   +---------------------------------------------------+   |
+-----------------------------------------------------------+
                             |
                     TLS / MongoDB Protocol
                             v
+-----------------------------------------------------------+
|                   MongoDB Atlas (Cloud)                   |
|   Database:   sample_restaurants                          |
|   Collection: restaurants                                 |
+-----------------------------------------------------------+
                             ^
                             | (Optional GUI Inspection)
+-----------------------------------------------------------+
|                   MongoDB Compass                         |
+-----------------------------------------------------------+
```

---

## 6. Project Structure

```
Restaurant-Discovery/
│
├── build.sbt                            # sbt build definition & dependencies
├── README.md                            # Complete assignment documentation
├── .env                                 # Local MongoDB Atlas connection string (ignored by git)
├── .env.example                         # Template for environment configuration
├── .gitignore                           # Git ignore rules protecting credentials
│
├── project/
│   └── build.properties                 # sbt version (1.10.7)
│
├── src/
│   ├── main/
│   │   ├── scala/
│   │   │   └── com/
│   │   │       └── restaurant/
│   │   │           ├── Main.scala              # HTTP Server, REST API & file routing
│   │   │           ├── MongoDB.scala           # Atlas connection manager & error handling
│   │   │           ├── Restaurant.scala        # Data models, traits, case classes & validation
│   │   │           └── RestaurantService.scala # CRUD, Indexing & Aggregations
│   │   │
│   │   └── resources/
│   │       └── application.conf         # Application configuration settings
│   │
│   └── test/
│       └── scala/
│           └── com/
│               └── restaurant/
│                   └── RestaurantTest.scala    # Unit tests for models & validation
│
└── frontend/
    ├── index.html                       # Single-page user interface
    ├── style.css                        # Modern, responsive styling
    └── app.js                           # Client-side API integration using fetch()
```

---

## 7. MongoDB Atlas Setup (Step-by-Step)

Follow these simple steps to prepare your free MongoDB Atlas database:

1. **Log in to MongoDB Atlas:**
   - Go to [https://cloud.mongodb.com](https://cloud.mongodb.com) and log in or create a free account.
2. **Create a Free Cluster:**
   - Click **Create Deployment** &rarr; Select the **M0 Free** tier.
   - Choose any provider/region (e.g. AWS / Mumbai or N. Virginia).
3. **Load Sample Dataset (Important):**
   - In Atlas cluster dashboard, click the three dots (`...`) next to your cluster name.
   - Click **Load Sample Dataset**. This automatically imports `sample_restaurants`.
4. **Create Database User:**
   - Navigate to **Database Access** in the left menu.
   - Click **Add New Database User**.
   - Select **Password Authentication**. Set a username (e.g., `abda_user`) and a secure password.
   - Assign the role **Read and write to any database**.
5. **Configure Network Access (IP Whitelist):**
   - Navigate to **Network Access** in the left menu.
   - Click **Add IP Address**.
   - Click **Allow Access from Anywhere** (`0.0.0.0/0`) or add your current IP address. Click **Confirm**.
6. **Obtain Connection String:**
   - Click **Connect** on your Database deployment.
   - Select **Drivers** &rarr; Driver: **Scala** (or Java).
   - Copy the `mongodb+srv://...` connection string.

---

## 8. MongoDB Compass Connection

To inspect your Atlas cloud data using MongoDB Compass:

1. Open **MongoDB Compass** on your computer.
2. In the **New Connection** input box, paste your `mongodb+srv://...` connection string:
   ```
   mongodb+srv://<username>:<password>@cluster0.abcde.mongodb.net/
   ```
   *(Replace `<username>` and `<password>` with your actual credentials)*.
3. Click **Connect**.
4. In the left database list, open `sample_restaurants` &rarr; click `restaurants`.
5. You can now visually see every restaurant created, updated, or deleted through our Scala application!

---

## 9. Environment Variables

Create a file named `.env` in the root of the project:

```bash
# .env
MONGODB_URI=mongodb+srv://username:password@cluster0.abcde.mongodb.net/?retryWrites=true&w=majority
```

> **Security Note:** The `.env` file is listed in `.gitignore` so real credentials will never be committed to GitHub. An empty template is available in `.env.example`.

---

## 10. Running the Application

### Prerequisites
- Java 17+ installed (`java -version`)
- sbt installed (`sbt --version`)

### Run Tests
To run unit tests without requiring a MongoDB connection:
```bash
sbt test
```

### Start the Complete Application
Run a single command:
```bash
sbt run
```

When the application boots, you will see:
```text
==================================================
Restaurant Discovery & Analytics System
Course: Advanced Big Data Analytics (ABDA)
--------------------------------------------------
MongoDB Atlas: Connected
Server: http://localhost:8080
==================================================
```

Open your browser at:
**[http://localhost:8080](http://localhost:8080)**

---

## 11. Deploy to Render

Follow these steps to deploy this application to Render as a public web service:

1. **Push project to GitHub:**
   Commit all project files and push to your GitHub repository (verify `.env` is NOT committed; `.gitignore` and `.dockerignore` protect it).

2. **Create Render Web Service:**
   Log in to [Render](https://render.com) and click **New +** &rarr; **Web Service**.

3. **Select the repository:**
   Connect your GitHub account and choose the `Restaurant-Discovery` repository.

4. **Deploy using Docker:**
   Render will automatically detect the `Dockerfile` (or select **Docker** as the Runtime / Environment).

5. **Add MONGODB_URI as a Render environment variable:**
   Under **Environment Variables**, add:
   - **Key:** `MONGODB_URI`
   - **Value:** Your MongoDB Atlas connection string (e.g., `mongodb+srv://<username>:<password>@cluster0.abcde.mongodb.net/?retryWrites=true&w=majority`)

6. **Deploy:**
   Click **Deploy Web Service** (or **Create Web Service**). Render builds the Docker image using sbt and launches the lightweight container.

7. **Open the generated Render URL:**
   Once the deploy status turns green (**Live**), click your public `https://<service-name>.onrender.com` link to use the live system!

---

## 12. Features
- **Real-Time Atlas Connection Badge:** Indicates live cluster connection or helpful error guidance.
- **Interactive Dashboard:** Live summary cards for Total Restaurants, Cuisines, Boroughs, and Average Score.
- **Full CRUD Management:** Create, read, update, and delete documents with instant Atlas persistence.
- **Multi-Criteria Search & Filter:** Search by name, cuisine, borough, ZIP code, and minimum score simultaneously.
- **B-Tree Index Management:** Check and generate required single and compound indexes directly from the UI.
- **Aggregation Pipelines:** Groupings by cuisine, borough distributions, and average hygiene scores computed in Atlas.

---

## 13. CRUD Implementation Details

| Operation | HTTP Method | REST Endpoint | Description |
|-----------|-------------|---------------|-------------|
| **CREATE** | `POST` | `/api/restaurants` | Validates input and inserts a new restaurant document into Atlas. |
| **READ** | `GET` | `/api/restaurants` | Retrieves up to 30 recent restaurants. |
| **UPDATE** | `PUT` | `/api/restaurants/:id` | Modifies name, cuisine, borough, ZIP, and score of an existing document. |
| **DELETE** | `DELETE` | `/api/restaurants/:id` | Permanently deletes a restaurant document by `_id`. |

---

## 14. Search and Filters

The endpoint `GET /api/restaurants/search` accepts dynamic query parameters:
- `name` &mdash; Case-insensitive partial matching (e.g. `Shake`)
- `cuisine` &mdash; Case-insensitive cuisine filter (e.g. `American`)
- `borough` &mdash; Exact borough match (e.g. `Manhattan`)
- `zipcode` &mdash; Exact postal code match (e.g. `10010`)
- `minScore` &mdash; Filters restaurants with hygiene score &ge; threshold

Empty parameters are automatically omitted from the MongoDB query.

---

## 15. Indexing Strategy

Indexes are essential in Big Data systems to avoid expensive full-collection scans ($O(N)$) and achieve logarithmic ($O(\log N)$) B-Tree lookups:

1. **`name_1`** &mdash; Single field ascending index on `name`.
2. **`cuisine_1`** &mdash; Single field ascending index on `cuisine`.
3. **`borough_1_cuisine_1`** &mdash; Compound index covering queries that filter by borough and cuisine simultaneously.

Clicking **"⚡ Create Required Indexes"** in the web interface calls `POST /api/indexes`, executing `collection.createIndex()` in Atlas.

---

## 16. Aggregation Pipelines

Three MongoDB aggregation pipelines are executed inside the Atlas cluster:

### 1. Restaurants Grouped by Cuisine
Counts restaurants per cuisine and sorts by popularity:
```scala
Seq(
  Aggregates.filter(notEqual("cuisine", "")),
  Aggregates.group("$cuisine", Accumulators.sum("count", 1)),
  Aggregates.sort(Sorts.descending("count")),
  Aggregates.limit(10)
)
```

### 2. Restaurants Grouped by Borough
Aggregates geographical distribution across NYC:
```scala
Seq(
  Aggregates.filter(notEqual("borough", "Missing")),
  Aggregates.group("$borough", Accumulators.sum("count", 1)),
  Aggregates.sort(Sorts.descending("count"))
)
```

### 3. Average Score by Cuisine
Unwinds the inspection grades array and computes the mean score:
```scala
Seq(
  Aggregates.unwind("$grades"),
  Aggregates.filter(and(notEqual("cuisine", ""), gte("grades.score", 0))),
  Aggregates.group("$cuisine", Accumulators.avg("avgScore", "$grades.score"), Accumulators.sum("count", 1)),
  Aggregates.sort(Sorts.descending("avgScore")),
  Aggregates.limit(10)
)
```

---

## 17. Scala Concepts Used

The codebase naturally demonstrates key Scala programming paradigms with clear inline comments:

| Concept | Location in Code | Purpose |
|---------|------------------|---------|
| **1. Variables (`val` / `var`)** | `Restaurant.scala`, `RestaurantService.scala` | Immutable state definitions and mutable filter builders. |
| **2. Data Types** | `Restaurant.scala` | `String`, `Int`, `Double`, `Boolean`, `Long`. |
| **3. Conditions** | `RestaurantValidator.validate` | Guard clauses for data validation (`if / else`). |
| **4. Functions / Methods** | All files | Clean, modular functions. |
| **5. Collections** | `Restaurant.scala`, `RestaurantService.scala` | `List`, `Seq`, `Map`. |
| **6. `map`** | `RestaurantService.scala`, `Main.scala` | Transforming documents and collections. |
| **7. `filter`** | `RestaurantHelper.filterHighScoreRestaurants` | Filtering lists meeting score thresholds. |
| **8. `groupBy`** | `RestaurantHelper.groupRestaurantsByCuisine` | Grouping in-memory collections by key. |
| **9. `Option` (`Some` / `None`)** | `MongoDB.scala`, `RestaurantService.scala` | Safe handling of potentially absent values without `NullPointerException`. |
| **10. Pattern Matching** | `MongoDB.scala`, `Main.scala` | Type-safe dispatching on `Option`, `Either`, and JSON types. |
| **11. Error Handling** | `MongoDB.scala`, `RestaurantService.scala` | Graceful `Try / Success / Failure` wrapping without crashes. |
| **12. Classes** | `RestaurantValidator`, `RestaurantService` | Encapsulating service logic and validation rules. |
| **13. Case Classes** | `Restaurant`, `Address`, `Grade`, `CuisineStat` | Immutable domain data models with structural equality and serialization. |
| **14. Objects** | `MongoDB`, `RestaurantHelper`, `Main` | Singletons for connection management, utilities, and server startup. |
| **15. Methods** | All classes and objects | Public and private member functions. |
| **16. Encapsulation** | `MongoDB.loadEnvVariable`, `RestaurantService.docToRestaurant` | `private` helper methods hiding internal implementation details. |
| **17. Trait & Inheritance** | `trait DataValidator[T]`, `class RestaurantValidator extends DataValidator[Restaurant]` | Abstract interface definition and polymorphic implementation. |

---

## 18. Demonstration Steps (Professor Demo)

Follow this 11-step sequence to demonstrate the project:

1. **Step 1:** Open `http://localhost:8080` in your browser.
2. **Step 2:** Point out the green badge **"MongoDB Atlas: Connected"** and the **Dashboard Overview** cards.
3. **Step 3:** Show existing restaurant records in the main **Restaurants** table.
4. **Step 4:** In the **Search & Filter** form, type `Shake Shack` and click **Search**. Verify filtered results appear. Click **Clear**.
5. **Step 5:** Filter by `Cuisine = Italian` or `Borough = Manhattan`.
6. **Step 6:** Scroll to **Add Restaurant** and submit:
   - **Name:** `ABDA Demo Restaurant`
   - **Cuisine:** `Indian`
   - **Borough:** `Manhattan`
   - **ZIP:** `10001`
   - **Score:** `12`
7. **Step 7:** Open **MongoDB Compass**, refresh the `restaurants` collection, and show the professor the newly added record in the cloud!
8. **Step 8:** Return to the browser, click **✏️ Edit** on the demo restaurant, change score to `16`, and save.
9. **Step 9:** Click **🗑️ Delete** and confirm deletion.
10. **Step 10:** Scroll to **MongoDB Aggregation Analytics** to showcase:
    - Top Cuisines Table
    - Borough Distribution Table
    - Average Hygiene Score Table
11. **Step 11:** Scroll to **MongoDB Atlas Indexes**, click **"⚡ Create Required Indexes"**, and show `name_1`, `cuisine_1`, and `borough_1_cuisine_1` marked as active.

---

## 19. Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `"MongoDB connection failed. Please check MONGODB_URI in .env."` | Missing or invalid connection string in `.env` | Ensure `.env` exists with `MONGODB_URI=mongodb+srv://<user>:<pwd>@cluster...` |
| Connection timed out | IP address not whitelisted in Atlas | Go to Atlas &rarr; **Network Access** &rarr; **Add IP Address** &rarr; select **Allow Access from Anywhere** (`0.0.0.0/0`). |
| Authentication failed | Incorrect username or password in connection string | Verify database user password in Atlas &rarr; **Database Access**. |
| Port 8080 already in use | Another application is running on port 8080 | Close the existing process or change `port = 8080` in `Main.scala`. |

---

*Developed for the Advanced Big Data Analytics (ABDA) coursework.*
