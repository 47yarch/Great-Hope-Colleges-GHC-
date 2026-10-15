package com.ghc.dao;

import com.ghc.database.DatabaseConnection;
import com.ghc.grading.GradingCalculator;
import com.ghc.security.AccessControl;
import com.ghc.security.UserSession;
import java.sql.*;
import java.util.Map;

/** Service boundary for grade edits; verifies instructor-to-section assignment in the database. */
public final class GradeDAO {
    public void saveCalculatedGrade(int enrollmentId, Map<String, Double> components, boolean hasLaboratory) throws SQLException {
        UserSession user = AccessControl.requireSignedIn();
        if (enrollmentId <= 0 || components == null) throw new IllegalArgumentException("A valid enrollment and component scores are required.");
        double[] v = new double[7];
        String[] keys = {"LEC_WW","LEC_UNIT_TEST","LEC_TERM_TEST","LAB_WW","LAB_UNIT_TEST","LAB_TERM_TEST","LAB_PERFORMANCE"};
        for (int i=0;i<keys.length;i++) {
            Double score=components.get(keys[i]);
            if (score==null) {
                if (i<3 || hasLaboratory) throw new IllegalArgumentException("Missing required grade component: "+keys[i]);
                score=0.0;
            }
            if (!Double.isFinite(score) || score<0 || score>100) throw new IllegalArgumentException("Each component score must be between 0 and 100.");
            v[i]=score;
        }
        try(Connection c=DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int sectionId;
                try(PreparedStatement p=c.prepareStatement("SELECT SECTION_ID FROM ENROLLMENTS WHERE ID=? FOR UPDATE")) {
                    p.setInt(1,enrollmentId); try(ResultSet r=p.executeQuery()) {
                        if(!r.next()) throw new SQLException("Enrollment not found.");
                        if(r.getObject(1)==null) throw new IllegalArgumentException("Assign this enrollment to a class section before grading.");
                        sectionId=r.getInt(1);
                    }
                }
                if(user.isInstructor()) {
                    try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM CLASS_SECTIONS WHERE ID=? AND INSTRUCTOR_USER_ID=? AND ACTIVE=1")) {
                        p.setInt(1,sectionId);p.setInt(2,user.getUserId());try(ResultSet r=p.executeQuery()){r.next();if(r.getInt(1)==0)throw new SecurityException("You can only grade students in your active assigned classes.");}
                    }
                } else if(!user.isAdmin()) throw new SecurityException("Only an Administrator or assigned Instructor may edit grades.");
                for(int i=0;i<keys.length;i++) {
                    Double old=null;
                    try(PreparedStatement p=c.prepareStatement("SELECT SCORE FROM GRADE_COMPONENTS WHERE ENROLLMENT_ID=? AND COMPONENT_TYPE=?")){p.setInt(1,enrollmentId);p.setString(2,keys[i]);try(ResultSet r=p.executeQuery()){if(r.next())old=r.getDouble(1);}}
                    if(old==null){try(PreparedStatement p=c.prepareStatement("INSERT INTO GRADE_COMPONENTS (ENROLLMENT_ID,COMPONENT_TYPE,SCORE,UPDATED_BY) VALUES (?,?,?,?)")){p.setInt(1,enrollmentId);p.setString(2,keys[i]);p.setDouble(3,v[i]);p.setInt(4,user.getUserId());p.executeUpdate();}}
                    else{try(PreparedStatement p=c.prepareStatement("UPDATE GRADE_COMPONENTS SET SCORE=?,UPDATED_BY=?,UPDATED_AT=CURRENT_TIMESTAMP WHERE ENROLLMENT_ID=? AND COMPONENT_TYPE=?")){p.setDouble(1,v[i]);p.setInt(2,user.getUserId());p.setInt(3,enrollmentId);p.setString(4,keys[i]);p.executeUpdate();}}
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO GRADE_AUDIT (ENROLLMENT_ID,COMPONENT_TYPE,OLD_SCORE,NEW_SCORE,CHANGED_BY,NOTE) VALUES (?,?,?,?,?,?)")){p.setInt(1,enrollmentId);p.setString(2,keys[i]);if(old==null)p.setNull(3,Types.DOUBLE);else p.setDouble(3,old);p.setDouble(4,v[i]);p.setInt(5,user.getUserId());p.setString(6,"Saved through gradebook");p.executeUpdate();}
                }
                double lecture=GradingCalculator.calculateLecture(v[0],v[1],v[2]);
                double lab=GradingCalculator.calculateLaboratory(v[3],v[4],v[5],v[6]);
                double raw=GradingCalculator.finalRawGrade(lecture,lab,hasLaboratory);
                try(PreparedStatement p=c.prepareStatement("UPDATE ENROLLMENTS SET GRADE=?,GRADE_EQUIVALENT=?,REMARKS=? WHERE ID=?")){p.setDouble(1,raw);p.setString(2,GradingCalculator.equivalentGrade(raw));p.setString(3,raw>=50?"Passed":"Failed");p.setInt(4,enrollmentId);p.executeUpdate();}
                c.commit();
            } catch(Exception ex) { c.rollback(); if(ex instanceof SQLException se)throw se; if(ex instanceof RuntimeException re)throw re; throw new SQLException(ex); }
            finally { c.setAutoCommit(true); }
        }
    }
}
