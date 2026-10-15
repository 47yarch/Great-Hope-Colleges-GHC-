# Great Hope Colleges (GHC) — Student Enrollment, Grading and Records

Java Swing desktop application using JDBC and Apache Derby embedded database. The UI uses the GHC green, gold and beige theme.

## Requirements
- JDK 17 or newer
- Apache Maven 3.8+ for rebuilding from source

## Run
1. Open a terminal in the `Great_Hope_Colleges_GHC` project folder.
2. Rebuild from source: `mvn clean package`
3. Launch with `mvn exec:java` or `java -jar target/ghc-student-management-1.0.0.jar`.

The embedded database is stored in `ghc_database/GHCDB` relative to the working directory. Keep that folder to preserve your existing records. On first launch, the app runs an additive migration: it creates missing tables/columns but does not drop existing tables or delete student, course, enrollment, or account rows.

**Before updating an existing installation:** close the application and make a backup copy of the entire `ghc_database` folder. Do not copy a live Derby database while the application is open.

## First login
- Username: `admin`
- Password: `admin123`

The default credentials are for demonstration only. Create named accounts with appropriate roles and reset the demo password for any real deployment.

## Roles and modules
- **Administrator:** all modules, user account management, academic setup, courses, students, class sections, enrollment, gradebook, reports and school-wide settings.
- **Registrar:** student records, enrollment, section assignment, academic record search and printing. Registrar operations do not edit grade components.
- **Instructor:** assigned class sections, grade component entry for assigned sections, weighted grade calculation, grade reports and session appearance preferences.

Role restrictions are applied in the DAO/service layer as well as in navigation. The gradebook stores component scores and an audit record for each save.

## Grading rules
- Lecture: written works 30%, unit test 40%, term test 30%.
- Laboratory: written works 20%, unit test 25%, term test 20%, performance tasks 35%.
- Final raw grade: lecture 100% for courses without laboratory; otherwise lecture 60% and laboratory 40%.
- Raw grades are rounded to two decimal places. Equivalent thresholds: 98=1.00, 91=1.25, 85=1.50, 79=1.75, 73=2.00, 67=2.25, 61=2.50, 55=2.75, 50=3.00; below 50=5.00.

## Notes
- Create instructor accounts under **People → User Accounts** before assigning instructors to class sections.
- Create class sections and assign enrollments to a matching section before instructors can enter grades.
- Course and section codes are unique within their defined scope. Duplicate enrollments for the same student/course/school year/semester are prevented by the database.
- The embedded database is intended for a single desktop application instance, not concurrent multi-computer deployment.
