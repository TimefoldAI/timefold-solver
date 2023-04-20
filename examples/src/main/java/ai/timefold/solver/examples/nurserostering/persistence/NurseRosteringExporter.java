package ai.timefold.solver.examples.nurserostering.persistence;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import ai.timefold.solver.examples.common.persistence.AbstractXmlSolutionExporter;
import ai.timefold.solver.examples.common.persistence.SolutionConverter;
import ai.timefold.solver.examples.nurserostering.app.NurseRosteringApp;
import ai.timefold.solver.examples.nurserostering.domain.NurseRoster;
import ai.timefold.solver.examples.nurserostering.domain.Shift;
import ai.timefold.solver.examples.nurserostering.domain.ShiftAssignment;

import org.jdom2.Element;

public class NurseRosteringExporter extends AbstractXmlSolutionExporter<NurseRoster> {

    public static void main(String[] args) {
        SolutionConverter<NurseRoster> converter = SolutionConverter.createExportConverter(NurseRosteringApp.DATA_DIR_NAME,
                new NurseRosteringExporter(), new NurseRosterSolutionFileIO());
        converter.convertAll();
    }

    @Override
    public XmlOutputBuilder<NurseRoster> createXmlOutputBuilder() {
        return new NurseRosteringOutputBuilder();
    }

    public static class NurseRosteringOutputBuilder extends XmlOutputBuilder<NurseRoster> {

        private NurseRoster nurseRoster;

        @Override
        public void setSolution(NurseRoster solution) {
            nurseRoster = solution;
        }

        @Override
        public void writeSolution() throws IOException {
            Element solutionElement = new Element("Solution");
            document.setRootElement(solutionElement);

            Element schedulingPeriodIDElement = new Element("SchedulingPeriodID");
            schedulingPeriodIDElement.setText(nurseRoster.getCode());
            solutionElement.addContent(schedulingPeriodIDElement);

            Element competitorElement = new Element("Competitor");
            competitorElement.setText("Geoffrey De Smet with Timefold");
            solutionElement.addContent(competitorElement);

            Element softConstraintsPenaltyElement = new Element("SoftConstraintsPenalty");
            softConstraintsPenaltyElement.setText(Integer.toString(nurseRoster.getScore().softScore()));
            solutionElement.addContent(softConstraintsPenaltyElement);

            for (ShiftAssignment shiftAssignment : nurseRoster.getShiftAssignmentList()) {
                Shift shift = shiftAssignment.getShift();
                if (shift != null) {
                    Element assignmentElement = new Element("Assignment");
                    solutionElement.addContent(assignmentElement);

                    Element dateElement = new Element("Date");
                    dateElement.setText(shift.getShiftDate().getDate().format(DateTimeFormatter.ISO_DATE));
                    assignmentElement.addContent(dateElement);

                    Element employeeElement = new Element("Employee");
                    employeeElement.setText(shiftAssignment.getEmployee().getCode());
                    assignmentElement.addContent(employeeElement);

                    Element shiftTypeElement = new Element("ShiftType");
                    shiftTypeElement.setText(shift.getShiftType().getCode());
                    assignmentElement.addContent(shiftTypeElement);
                }
            }
        }
    }

}
