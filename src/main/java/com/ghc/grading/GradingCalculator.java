package com.ghc.grading;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Official GHC weighted grading rules. Scores and raw grades use a 0–100 scale. */
public final class GradingCalculator {
    private GradingCalculator() {}
    private static void validate(double grade) {
        if (!Double.isFinite(grade) || grade < 0 || grade > 100)
            throw new IllegalArgumentException("Each component score must be between 0 and 100.");
    }
    public static double calculateLecture(double writtenWorks,double unitTest,double termTest) {
        validate(writtenWorks); validate(unitTest); validate(termTest);
        return round(writtenWorks*.30+unitTest*.40+termTest*.30);
    }
    public static double calculateLaboratory(double writtenWorks,double unitTest,double termTest,double performanceTasks) {
        validate(writtenWorks); validate(unitTest); validate(termTest); validate(performanceTasks);
        return round(writtenWorks*.20+unitTest*.25+termTest*.20+performanceTasks*.35);
    }
    public static double finalRawGrade(double lectureGrade,double laboratoryGrade,boolean hasLaboratory) {
        validate(lectureGrade); validate(laboratoryGrade);
        return hasLaboratory ? round(lectureGrade*.60+laboratoryGrade*.40) : round(lectureGrade);
    }
    public static double round(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Grade must be finite.");
        return BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP).doubleValue();
    }
    public static String equivalentGrade(double rawGrade) {
        validate(rawGrade);
        if(rawGrade>=98)return "1.00";
        if(rawGrade>=91)return "1.25";
        if(rawGrade>=85)return "1.50";
        if(rawGrade>=79)return "1.75";
        if(rawGrade>=73)return "2.00";
        if(rawGrade>=67)return "2.25";
        if(rawGrade>=61)return "2.50";
        if(rawGrade>=55)return "2.75";
        if(rawGrade>=50)return "3.00";
        return "5.00";
    }
}
