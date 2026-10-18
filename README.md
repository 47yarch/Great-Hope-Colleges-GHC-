# Great Hope Colleges (GHC)

[![Language](https://img.shields.io/badge/Language-Java-orange?logo=openjdk&logoColor=white)](https://www.java.com/)
[![UI](https://img.shields.io/badge/UI-Java%20Swing%20%2F%20AWT-blue)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![Platform](https://img.shields.io/badge/Platform-Desktop-lightgrey)](https://www.java.com/)
[![Build Tool](https://img.shields.io/badge/Build-Apache%20Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Database](https://img.shields.io/badge/Database-Apache%20Derby-4479A1)](https://db.apache.org/derby/)
[![JDK](https://img.shields.io/badge/JDK-17%2B-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)

> A Java desktop application for student enrollment, grading, and academic record management. It uses Java Swing/AWT for the graphical interface, JDBC for database access, and an embedded Apache Derby database.

## Project at a Glance

| Item | Details |
|---|---|
| **Project** | Great Hope Colleges (GHC) — Student Enrollment, Grading and Record Management System |
| **Application Type** | Desktop Application |
| **Programming Language** | Java |
| **User Interface** | Java Swing / AWT |
| **Database Connectivity** | JDBC |
| **Database** | Apache Derby (Embedded) |
| **Build Tool** | Apache Maven |
| **Required JDK** | JDK 17 or newer |
| **Required Maven** | Apache Maven 3.8 or newer |

## 🖥️ Application Screenshots

### 🔐 Login Screen
![GHC Login Screen](docs/screenshots/login.png)

### 🏠 Dashboard
![GHC Dashboard](docs/screenshots/dashboard.png)

### 👨‍🎓 Student Directory
![Student Directory](docs/screenshots/student-directory.png)

### 📚 Courses and Subjects
![Courses and Subjects](docs/screenshots/course.png)

### 📝 Enrollment Management
![Enrollment Management](docs/screenshots/enrollment.png)

### 📊 Gradebook
![GHC Gradebook](docs/screenshots/gradebook.png)


---

## 📌 About the Project

Great Hope Colleges (GHC) is a desktop application designed to help manage student and academic records in one system. It uses Java for the application interface and Apache Derby for database storage.

The project is developed using Java Swing/AWT for the graphical user interface, JDBC for database connectivity, and Maven for project management.

## ✨ Features

- **User Login** — Provides a login interface for accessing the application.
- **Dashboard** — Main navigation interface for the system.
- **Student Management** — Organize and manage student records.
- **Course Management** — Manage available courses and subjects.
- **Enrollment Management** — Maintain student enrollment records.
- **Academic Grading** — Record and manage student grades.
- **Database Integration** — Store and retrieve application data using Apache Derby.
- **Role-Based Access** — Supports access based on configured user roles, where implemented.
- **Grade History/Audit** — Database structures support grade audit records, where implemented.

*The availability of each feature depends on the functionality implemented in the current version of the application.*

## 🏗️ System Architecture

The Great Hope Colleges (GHC) Student Enrollment and Grading Management System is a Java desktop application. Its logical architecture connects the user interface to application workflows and database operations through JDBC, using Apache Derby for data storage.

### Architecture Diagram

```mermaid
flowchart TD
    U["User"] --> UI["Presentation Layer<br/>Java Swing / AWT"]
    UI --> APP["Application Logic<br/>Validation and Workflows"]
    APP --> DATA["Database Access Layer<br/>Queries and Persistence"]
    DATA --> JDBC["JDBC Connection"]
    JDBC --> DB[("Apache Derby Database")]

    CFG["Database Configuration"] -.-> JDBC

    style U fill:#f7f3e8,stroke:#245b43,color:#24332b
    style UI fill:#245b43,stroke:#173e2d,color:#ffffff
    style APP fill:#36775a,stroke:#245b43,color:#ffffff
    style DATA fill:#f1d77f,stroke:#b99a37,color:#24332b
    style JDBC fill:#f7f3e8,stroke:#245b43,color:#24332b
    style DB fill:#245b43,stroke:#173e2d,color:#ffffff
```

### Architecture Components

| Component | Responsibility |
|---|---|
| Presentation Layer | Displays the desktop interface and collects user input through Java Swing/AWT. |
| Application Logic | Coordinates application workflows and validation. |
| Database Access Layer | Handles database initialization, queries, and persistence operations. |
| JDBC | Provides Java connectivity to the database. |
| Apache Derby | Stores application records in relational tables. |
| Database Configuration | Defines the connection details used by the application. |

*This diagram represents the logical architecture. The precise division of responsibilities should be checked against the current Java source code.*

### Data Flow

1. A user interacts with the GHC desktop interface.
2. The application processes the requested operation and validates input.
3. Database access code executes the necessary database operations.
4. JDBC communicates with Apache Derby.
5. The application receives the results and updates the interface.

## 🗄️ Database Design

GHC uses Apache Derby as its relational database. The database initializer defines or updates the tables required by the application.

### Database Table Overview

```mermaid
flowchart TB
    subgraph Accounts["Accounts and Configuration"]
        USERS["USERS"]
        SETTINGS["APP_SETTINGS"]
    end

    subgraph Academic["Academic Records"]
        STUDENTS["STUDENTS"]
        COURSES["COURSES"]
        DEPARTMENTS["DEPARTMENTS"]
        PROGRAMS["PROGRAMS"]
    end

    subgraph Enrollment["Class and Enrollment"]
        ENROLLMENTS["ENROLLMENTS"]
        SECTIONS["CLASS_SECTIONS"]
        SCHEDULES["SECTION_SCHEDULES"]
    end

    subgraph Grading["Grading and Audit"]
        COMPONENTS["GRADE_COMPONENTS"]
        AUDIT["GRADE_AUDIT"]
    end

    style Accounts fill:#f7f3e8,stroke:#245b43,color:#24332b
    style Academic fill:#e8f1e8,stroke:#36775a,color:#24332b
    style Enrollment fill:#fff8df,stroke:#b99a37,color:#24332b
    style Grading fill:#e8f1e8,stroke:#245b43,color:#24332b
```

The diagram groups tables by their apparent purpose. It intentionally does not draw foreign-key relationships because the exact constraints must be confirmed from the project's database schema.

### Table Descriptions

| Table | Purpose |
|---|---|
| `USERS` | Application user accounts. |
| `STUDENTS` | Student information and records. |
| `COURSES` | Course or subject catalog. |
| `ENROLLMENTS` | Student enrollment records. |
| `DEPARTMENTS` | Academic department records. |
| `PROGRAMS` | Academic program records. |
| `CLASS_SECTIONS` | Class section or offering records. |
| `SECTION_SCHEDULES` | Class schedule information. |
| `GRADE_COMPONENTS` | Grade component records used by the grading workflow. |
| `GRADE_AUDIT` | Grade audit or change-history records. |
| `APP_SETTINGS` | Application configuration or settings. |

*Descriptions are based on the table names and project context. Confirm exact columns, data types, primary keys, foreign keys, and constraints in `DatabaseInitializer.java` and the related database code.*

### Interactive Architecture and Database Explorer

Explore the clickable diagrams and table descriptions in the companion webpage:

**[Open the GHC Architecture & Database Explorer](docs/screenshots/ghc_architecture_explorer.html)**

The explorer provides a visual way to inspect application components and database tables. It is a static documentation page and does not connect to or modify the live database.

### Database Integrity and Security

- Define primary keys for records that need unique identifiers.
- Use foreign keys where appropriate to enforce valid relationships between records.
- Apply suitable constraints to prevent invalid or incomplete data.
- Keep database connection settings and credentials secure.
- Do not publish real student information, grades, or account credentials.
- Back up the database before performing schema migrations or transferring existing records.

These are recommended database practices; verify which constraints and protections are implemented in the current version.

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| Java 17 | Main programming language |
| Java Swing / AWT | Desktop graphical user interface |
| Apache Maven | Project build and dependency management |
| Apache Derby | Relational database |
| JDBC | Database connectivity |
| NetBeans IDE | Recommended development environment |

## 📋 Requirements

Before running the project, make sure you have:

- Java Development Kit (JDK) 17 or compatible configured version
- Apache Maven
- NetBeans IDE or another Java IDE
- The project source code and required dependencies

## 🚀 Getting Started

### 1. Clone the repository

Open a terminal and run:

```bash
git clone https://github.com/47yarch/Great-Hope-Colleges-GHC-.git
```

Navigate into the project directory:

```bash
cd Great-Hope-Colleges-GHC-
```

### 2. Open the project

**Using NetBeans:**

1. Open NetBeans.
2. Select **File → Open Project**.
3. Select the cloned `Great-Hope-Colleges-GHC-` folder.
4. Wait for Maven to resolve the project dependencies.

### 3. Build the project

From the project directory, run:

```bash
mvn clean compile
```

To package the project:

```bash
mvn clean package
```

### 4. Run the application

Run the project using the configured main class in your IDE. If your project is configured to use Maven's `exec` plugin, use its configured run command.

The application initializes its database through the project's database initialization code when configured to do so.

> **Important:** Use the correct database configuration for your environment. If the application uses an embedded Derby database, make sure the database path is accessible and that the required Derby dependencies are available.

## 🗄️ Database

The application uses **Apache Derby** as its relational database and JDBC to connect Java code to the database.

The database initialization code is responsible for creating or updating the tables required by the application and may insert initial records when configured.

Database tables in the project may include:

- `USERS`
- `STUDENTS`
- `COURSES`
- `ENROLLMENTS`
- `DEPARTMENTS`
- `PROGRAMS`
- `CLASS_SECTIONS`
- `SECTION_SCHEDULES`
- `GRADE_COMPONENTS`
- `GRADE_AUDIT`
- `APP_SETTINGS`

The exact schema depends on the version of the source code being used.

### Sharing the project with another developer

If another developer needs the existing database records:

1. Identify the Derby database location from the JDBC URL in the database connection class.
2. Determine whether the database is embedded and stored locally or configured elsewhere.
3. Back up the database before transferring it.
4. Share a consistent database backup or export through an appropriate private channel.
5. Ensure the other developer's database configuration points to the correct local database location.

**Do not commit database files containing real student information, grades, passwords, or other sensitive data to a public GitHub repository.** Use sample or test data for public demonstrations.

## 📁 Project Structure

The project follows a Maven-style Java directory structure. The exact folders may vary as the project develops.

```text
Great-Hope-Colleges-GHC-/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── ghc/
│       │           ├── database/
│       │           └── ui/
│       └── resources/
├── docs/
│   └── screenshots/
│       ├── login.png
│       ├── dashboard.png
│       ├── students.png
│       ├── courses.png
│       ├── enrollment.png
│       └── gradebook.png
├── pom.xml
└── README.md
```

This is a guide to the expected layout, not a guarantee that every listed file or directory already exists in the repository.

## 🔒 Security and Privacy

- Do not commit database credentials, passwords, or private configuration values.
- Avoid uploading screenshots containing real student details or grades.
- Use fictional or anonymized records for screenshots and demonstrations.
- Do not publish a database backup containing confidential information.
- Review the repository before making it public.

## 🧰 Troubleshooting

### Maven build fails

Verify that JDK 17 is installed and that Maven is using the correct Java version. Then run:

```bash
mvn clean compile
```

### Database connection fails

- Check the JDBC URL and database location.
- Verify that the Derby dependencies are available.
- Confirm that the application has permission to access the database directory.
- Check the console output for the specific database error.

### Screenshots do not appear on GitHub

- Verify that the images are committed to the repository.
- Check that the paths and capitalization match the README exactly.
- Use repository-relative paths, such as `docs/screenshots/login.png`.
- Confirm that the image files are valid PNG files.

## 🔮 Possible Future Improvements

- Improved validation and error messages
- More detailed reports and printable summaries
- Enhanced role and permission management
- Additional search and filtering options
- Database backup and restore tools
- Improved user interface and accessibility

These are potential improvements and are not necessarily implemented in the current version.

## 👥 Contributors

- **Agustin, Jeshaiah D.**
- **Sabulao, John Michael P.**

## 📄 License

No license has been specified yet. Unless a license is added to the repository, others should not assume they have permission to redistribute or reuse the project.

---

**Great Hope Colleges (GHC)**  
*Student Enrollment and Grading Management System*
