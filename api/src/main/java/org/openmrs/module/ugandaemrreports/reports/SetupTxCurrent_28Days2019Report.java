package org.openmrs.module.ugandaemrreports.reports;

import org.openmrs.module.reporting.cohort.definition.BaseObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.common.RangeComparator;
import org.openmrs.module.reporting.dataset.definition.CohortIndicatorDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.indicator.dimension.CohortDefinitionDimension;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.ugandaemrreports.library.*;
import org.openmrs.module.ugandaemrreports.metadata.HIVMetadata;
import org.openmrs.module.ugandaemrreports.reporting.metadata.Dictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.openmrs.module.ugandaemrreports.library.CommonDatasetLibrary.settings;
import static org.openmrs.module.ugandaemrreports.library.CommonDatasetLibrary.getUgandaEMRVersion;

/**
 *  TX Current Report
 */
@Component
public class SetupTxCurrent_28Days2019Report extends UgandaEMRDataExportManager {

    @Autowired
    private DataFactory df;

    @Autowired
    ARTClinicCohortDefinitionLibrary hivCohorts;

    @Autowired
    private HIVCohortDefinitionLibrary hivCohortDefinitionLibrary;
    
    @Autowired
    private CommonDimensionLibrary commonDimensionLibrary;

    @Autowired
    private HIVMetadata hivMetadata;

    @Autowired
    private CommonCohortDefinitionLibrary cohortDefinitionLibrary;


    /**
     * @return the uuid for the report design for exporting to Excel
     */
    @Override
    public String getExcelDesignUuid() {
        return "f9610dc0-095e-47d9-be1c-06dc0e0818f7";
    }

    public String getJSONDesignUuid() {
        return "1fdf9fa1-d9b4-48a6-b235-3994f9df30cf";
    }

    @Override
    public String getUuid() {
        return "8dafdc32-97b7-4f49-828e-475cd4f09669";
    }

    @Override
    public String getName() {
        return "Tx_Current28Days 2019 Report";
    }

    @Override
    public String getDescription() {
        return "TX_CURR MER Indicator report for PEPFAR with Lost to Followup taken as 28 days from the last scheduled appointment";
    }

    @Override
    public List<Parameter> getParameters() {
        List<Parameter> l = new ArrayList<Parameter>();
        l.add(df.getStartDateParameter());
        l.add(df.getEndDateParameter());
        return l;
    }


    @Override
    public List<ReportDesign> constructReportDesigns(ReportDefinition reportDefinition) {
        return Arrays.asList(buildReportDesign(reportDefinition),buildJSONReportDesign(reportDefinition));
    }

    /**
     * Build the report design for the specified report, this allows a user to override the report design by adding
     * properties and other metadata to the report design
     *
     * @param reportDefinition
     * @return The report design
     */
    @Override
    public ReportDesign buildReportDesign(ReportDefinition reportDefinition) {
        return createExcelTemplateDesign(getExcelDesignUuid(), reportDefinition, "MER_TX_CURRENT_2019.xls");
    }

    public ReportDesign buildJSONReportDesign(ReportDefinition reportDefinition) {
        return createJSONTemplateDesign(getJSONDesignUuid(), reportDefinition, "MER_TX_CURRENT_2019.json");
    }

    @Override
    public ReportDefinition constructReportDefinition() {

        ReportDefinition rd = new ReportDefinition();

        rd.setUuid(getUuid());
        rd.setName(getName());
        rd.setDescription(getDescription());
        rd.setParameters(getParameters());

        CohortIndicatorDataSetDefinition dsd = new CohortIndicatorDataSetDefinition();

        dsd.setParameters(getParameters());
        rd.addDataSetDefinition("TX", Mapped.mapStraightThrough(dsd));
        rd.addDataSetDefinition("S",Mapped.mapStraightThrough(settings()));
        rd.addDataSetDefinition("aijar", Mapped.mapStraightThrough(getUgandaEMRVersion()));

        CohortDefinitionDimension ageDimension =commonDimensionLibrary.getFinerAgeWith55And65Ranges();
        dsd.addDimension("age", Mapped.mapStraightThrough(ageDimension));

        CohortDefinition PWIDS = df.getPatientsWithCodedObsDuringPeriod(Dictionary.getConcept("927563c5-cb91-4536-b23c-563a72d3f829"),hivMetadata.getARTSummaryPageEncounterType(),
                Arrays.asList(Dictionary.getConcept("160666AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")), BaseObsCohortDefinition.TimeModifier.LAST);

        CohortDefinition PIPS = df.getPatientsWithCodedObsDuringPeriod(Dictionary.getConcept("927563c5-cb91-4536-b23c-563a72d3f829"),hivMetadata.getARTSummaryPageEncounterType(),
                Arrays.asList(Dictionary.getConcept("162277AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")), BaseObsCohortDefinition.TimeModifier.LAST);

        CohortDefinition males = cohortDefinitionLibrary.males();
        CohortDefinition females = cohortDefinitionLibrary.females();

        CohortDefinition patientsWithLessThan3MonthsOfARVDrugsDispensed = hivCohortDefinitionLibrary.getPatientsThatReceivedDrugsForNoOfDaysByEndOfPeriod(90.0, RangeComparator.LESS_THAN);
        CohortDefinition patientsWithEqualOrGreaterThan6MonthsOfARVDrugsDispensed = hivCohortDefinitionLibrary.getPatientsThatReceivedDrugsForNoOfDaysByEndOfPeriod(180.0, RangeComparator.GREATER_EQUAL);
        CohortDefinition patientsWith3To5MonthsOfARVDrugsDispensed = hivCohortDefinitionLibrary.getPatientsThatReceivedDrugsForNoOfDaysByEndOfPeriod(90.0,RangeComparator.GREATER_EQUAL,150.0,RangeComparator.LESS_EQUAL);

        CohortDefinition above15Years = cohortDefinitionLibrary.above15Years();
        CohortDefinition below15Years = cohortDefinitionLibrary.MoHChildren();


        CohortDefinition beenOnArtDuringQuarter = hivCohortDefinitionLibrary.getActivePatientsWithLostToFollowUpAsByDays("28");

        CohortDefinition currentOnARTAndDrugsDispensedToPatientsBelow15Years = df.getPatientsInAll(beenOnArtDuringQuarter,below15Years);
        CohortDefinition currentOnARTAndDrugsDispensedToPatientsBelow15YearsFemales = df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsBelow15Years,females);
        CohortDefinition currentOnARTAndDrugsDispensedToPatientsBelow15YearsMales = df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsBelow15Years,males);
        CohortDefinition currentOnARTAndDrugsDispensedToPatientsAbove15Years = df.getPatientsInAll(beenOnArtDuringQuarter,above15Years);
        CohortDefinition currentOnARTAndDrugsDispensedToPatientsAbove15YearsFemales = df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsAbove15Years,females);
        CohortDefinition currentOnARTAndDrugsDispensedToPatientsAbove15YearsMales = df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsAbove15Years,males);

        Helper.addGender(dsd,"a","TX Curr on ART female",beenOnArtDuringQuarter);
        Helper.addGender(dsd,"b","TX Curr on ART male",beenOnArtDuringQuarter);

        Helper.addIndicator(dsd,"TOTAL","TX Curr TOTAL",beenOnArtDuringQuarter,"");

        Helper.addIndicator(dsd,"PWIDSf","PWIDs TX Curr on ART female",df.getPatientsInAll(females,PWIDS,beenOnArtDuringQuarter),"");
        Helper.addIndicator(dsd,"PWIDSm","PWIDs TX Curr on ART male",df.getPatientsInAll(males,PWIDS,beenOnArtDuringQuarter),"");

        Helper.addIndicator(dsd,"TOTAL_KP","PWIDs and PIP Tx Curr ",df.getPatientsInAll(df.getPatientsInAny(PWIDS,PIPS),beenOnArtDuringQuarter),"");
        Helper.addIndicator(dsd,"PWIDS","PWIDs TX Curr on ART ",df.getPatientsInAll(PWIDS,beenOnArtDuringQuarter),"");
        Helper.addIndicator(dsd,"PIPS","PWIDs TX Curr on ART ",df.getPatientsInAll(PIPS,beenOnArtDuringQuarter),"");

        Helper.addIndicator(dsd,"PIPf","PIPs TX Curr on ART female",df.getPatientsInAll(females,PIPS,beenOnArtDuringQuarter),"");
        Helper.addIndicator(dsd,"PIPm","PIPs TX Curr on ART male",df.getPatientsInAll(males,PIPS,beenOnArtDuringQuarter),"");

        Helper.addIndicator(dsd,"14e","<3 months of ARVs dispensed by <15yrs",df.getPatientsInAll(beenOnArtDuringQuarter,patientsWithLessThan3MonthsOfARVDrugsDispensed,below15Years),"");
        Helper.addIndicator(dsd,"15e","<3 months of ARVs dispensed by >15yrs",df.getPatientsInAll(beenOnArtDuringQuarter,patientsWithLessThan3MonthsOfARVDrugsDispensed,above15Years),"");
        Helper.addIndicator(dsd,"16e","3 to 5 months months of ARVs dispensed by <15yrs",df.getPatientsInAll(beenOnArtDuringQuarter,patientsWith3To5MonthsOfARVDrugsDispensed,below15Years),"");
        Helper.addIndicator(dsd,"17e","3 to 5 months months of ARVs dispensed by >15yrs",df.getPatientsInAll(beenOnArtDuringQuarter,patientsWith3To5MonthsOfARVDrugsDispensed,above15Years),"");
        Helper.addIndicator(dsd,"18e","6 or more months of ARVs dispensed to patient by <15yrs",df.getPatientsInAll(beenOnArtDuringQuarter,patientsWithEqualOrGreaterThan6MonthsOfARVDrugsDispensed,below15Years),"");
        Helper.addIndicator(dsd,"19e","6 or more months of ARVs dispensed to patient by >15yrs",df.getPatientsInAll(beenOnArtDuringQuarter,patientsWithEqualOrGreaterThan6MonthsOfARVDrugsDispensed,above15Years),"");
        Helper.addIndicator(dsd,"14c","<3 mnths ARV dispensing female less than 15",df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsBelow15YearsFemales,patientsWithLessThan3MonthsOfARVDrugsDispensed),"");
        Helper.addIndicator(dsd,"14d","<3 mnths ARV dispensing male less than 15",df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsBelow15YearsMales,patientsWithLessThan3MonthsOfARVDrugsDispensed),"");

        Helper.addIndicator(dsd,"15c","<3 mnths ARV dispensing female more than 15",df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsAbove15YearsFemales,patientsWithLessThan3MonthsOfARVDrugsDispensed),"");
        Helper.addIndicator(dsd,"15d","<3 mnths ARV dispensing male more than 15",df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsAbove15YearsMales,patientsWithLessThan3MonthsOfARVDrugsDispensed),"");

        Helper.addIndicator(dsd,"16c","3-5 mnths ARV dispensing female less than 15",df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsBelow15YearsFemales,patientsWith3To5MonthsOfARVDrugsDispensed),"");
        Helper.addIndicator(dsd,"16d","3-5 mnths ARV dispensing male less than 15",df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsBelow15YearsMales,patientsWith3To5MonthsOfARVDrugsDispensed),"");

        Helper.addIndicator(dsd,"17c","3-5 mnths ARV dispensing female more than 15",df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsAbove15YearsFemales,patientsWith3To5MonthsOfARVDrugsDispensed),"");
        Helper.addIndicator(dsd,"17d","3-5 mnths ARV dispensing male more than 15",df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsAbove15YearsMales,patientsWith3To5MonthsOfARVDrugsDispensed),"");

        Helper.addIndicator(dsd,"18c","6 mnths ARV dispensing female less than 15",df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsBelow15YearsFemales,patientsWithEqualOrGreaterThan6MonthsOfARVDrugsDispensed),"");
        Helper.addIndicator(dsd,"18d","6 mnths ARV dispensing male less than 15",df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsBelow15YearsMales,patientsWithEqualOrGreaterThan6MonthsOfARVDrugsDispensed),"");

        Helper.addIndicator(dsd,"19c","6 mnths ARV dispensing female more than 15",df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsAbove15YearsFemales,patientsWithEqualOrGreaterThan6MonthsOfARVDrugsDispensed),"");
        Helper.addIndicator(dsd,"19d","6 mnths ARV dispensing male more than 15",df.getPatientsInAll(currentOnARTAndDrugsDispensedToPatientsAbove15YearsMales,patientsWithEqualOrGreaterThan6MonthsOfARVDrugsDispensed),"");

        return rd;
    }






    @Override
    public String getVersion() {
        return "0.4.7";
    }
}
