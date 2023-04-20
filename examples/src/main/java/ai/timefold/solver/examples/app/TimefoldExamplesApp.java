package ai.timefold.solver.examples.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import ai.timefold.solver.examples.cloudbalancing.app.CloudBalancingApp;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.swingui.OpenBrowserAction;
import ai.timefold.solver.examples.common.swingui.SolverAndPersistenceFrame;
import ai.timefold.solver.examples.conferencescheduling.app.ConferenceSchedulingApp;
import ai.timefold.solver.examples.curriculumcourse.app.CurriculumCourseApp;
import ai.timefold.solver.examples.examination.app.ExaminationApp;
import ai.timefold.solver.examples.flightcrewscheduling.app.FlightCrewSchedulingApp;
import ai.timefold.solver.examples.machinereassignment.app.MachineReassignmentApp;
import ai.timefold.solver.examples.meetingscheduling.app.MeetingSchedulingApp;
import ai.timefold.solver.examples.nqueens.app.NQueensApp;
import ai.timefold.solver.examples.nurserostering.app.NurseRosteringApp;
import ai.timefold.solver.examples.pas.app.PatientAdmissionScheduleApp;
import ai.timefold.solver.examples.projectjobscheduling.app.ProjectJobSchedulingApp;
import ai.timefold.solver.examples.taskassigning.app.TaskAssigningApp;
import ai.timefold.solver.examples.tennis.app.TennisApp;
import ai.timefold.solver.examples.travelingtournament.app.TravelingTournamentApp;
import ai.timefold.solver.examples.tsp.app.TspApp;
import ai.timefold.solver.examples.vehiclerouting.app.VehicleRoutingApp;

public class TimefoldExamplesApp extends JFrame {

    /**
     * Supported system properties: {@link CommonApp#DATA_DIR_SYSTEM_PROPERTY}.
     *
     * @param args never null
     */
    public static void main(String[] args) {
        CommonApp.prepareSwingEnvironment();
        TimefoldExamplesApp timefoldExamplesApp = new TimefoldExamplesApp();
        timefoldExamplesApp.pack();
        timefoldExamplesApp.setLocationRelativeTo(null);
        timefoldExamplesApp.setVisible(true);
    }

    private static String determineTimefoldExamplesVersion() {
        String timefoldExamplesVersion = TimefoldExamplesApp.class.getPackage().getImplementationVersion();
        if (timefoldExamplesVersion == null) {
            timefoldExamplesVersion = "";
        }
        return timefoldExamplesVersion;
    }

    private JTextArea descriptionTextArea;

    public TimefoldExamplesApp() {
        super("Timefold examples " + determineTimefoldExamplesVersion());
        setIconImage(SolverAndPersistenceFrame.TIMEFOLD_ICON.getImage());
        setContentPane(createContentPane());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private Container createContentPane() {
        JPanel contentPane = new JPanel(new BorderLayout(5, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel titleLabel = new JLabel("Which example do you want to see?", JLabel.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(20.0f));
        contentPane.add(titleLabel, BorderLayout.NORTH);
        JScrollPane examplesScrollPane = new JScrollPane(createExamplesPanel());
        examplesScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        examplesScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        examplesScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        contentPane.add(examplesScrollPane, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(createDescriptionPanel(), BorderLayout.CENTER);
        bottomPanel.add(createExtraPanel(), BorderLayout.EAST);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
        return contentPane;
    }

    private JPanel createExamplesPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 4, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Stream.of(new VehicleRoutingApp(),
                new NurseRosteringApp(),
                new TaskAssigningApp(),
                new CloudBalancingApp(),
                new ConferenceSchedulingApp(),
                new PatientAdmissionScheduleApp(),
                new MachineReassignmentApp(),
                new CurriculumCourseApp(),
                new ProjectJobSchedulingApp(),
                new ExaminationApp(),
                new MeetingSchedulingApp(),
                new TravelingTournamentApp(),
                new TennisApp(),
                new FlightCrewSchedulingApp(),
                new TspApp(), new NQueensApp())
                .map(this::createExampleButton)
                .forEach(panel::add);

        return panel;
    }

    private JButton createExampleButton(final CommonApp commonApp) {
        String iconResource = commonApp.getIconResource();
        Icon icon = iconResource == null ? new EmptyIcon() : new ImageIcon(getClass().getResource(iconResource));
        JButton button = new JButton(new AbstractAction(commonApp.getName(), icon) {
            @Override
            public void actionPerformed(ActionEvent e) {
                commonApp.init(TimefoldExamplesApp.this, false);
            }
        });
        button.setHorizontalAlignment(JButton.LEFT);
        button.setHorizontalTextPosition(JButton.RIGHT);
        button.setVerticalTextPosition(JButton.CENTER);
        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                descriptionTextArea.setText(commonApp.getDescription());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                descriptionTextArea.setText("");
            }

        });
        return button;
    }

    private JPanel createDescriptionPanel() {
        JPanel descriptionPanel = new JPanel(new BorderLayout(2, 2));
        descriptionPanel.add(new JLabel("Description"), BorderLayout.NORTH);
        descriptionTextArea = new JTextArea(8, 65);
        descriptionTextArea.setEditable(false);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionPanel.add(new JScrollPane(descriptionTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        return descriptionPanel;
    }

    private JPanel createExtraPanel() {
        JPanel extraPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        extraPanel.add(new JPanel());
        Action homepageAction = new OpenBrowserAction("timefold.ai", "https://timefold.ai");
        extraPanel.add(new JButton(homepageAction));
        Action documentationAction = new OpenBrowserAction("Documentation",
                "https://timefold.ai/docs/");
        extraPanel.add(new JButton(documentationAction));
        return extraPanel;
    }

    private static class EmptyIcon implements Icon {

        @Override
        public int getIconWidth() {
            return 64;
        }

        @Override
        public int getIconHeight() {
            return 64;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            // Do nothing
        }

    }

}
