package ai.timefold.solver.examples.conferencescheduling.swingui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.business.SolutionBusiness;
import ai.timefold.solver.examples.common.swingui.SolutionPanel;
import ai.timefold.solver.examples.conferencescheduling.domain.ConferenceSolution;
import ai.timefold.solver.examples.conferencescheduling.persistence.ConferenceSchedulingCfpDevoxxImporter;

public final class ConferenceCFPImportAction implements CommonApp.ExtraAction<ConferenceSolution> {

    @Override
    public String getName() {
        return "Import from CFP";
    }

    @Override
    public void accept(SolutionBusiness<ConferenceSolution, ?> solutionBusiness,
            SolutionPanel<ConferenceSolution> solutionPanel) {
        String[] cfpArray = { "cfp-devoxx" };
        JComboBox<String> cfpConferenceBox = new JComboBox<>(cfpArray);
        JTextField cfpRestUrlTextField = new JTextField("https://dvbe18.confinabox.com/api/conferences/DVBE18");
        Object[] dialogue = {
                "Choose conference:", cfpConferenceBox,
                "Enter CFP REST Url:", cfpRestUrlTextField,
        };

        int option = JOptionPane.showConfirmDialog(solutionPanel, dialogue, "Import", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String conferenceBaseUrl = cfpRestUrlTextField.getText();
            new ConferenceCFPImportWorker(solutionBusiness, solutionPanel, conferenceBaseUrl)
                    .executeAndShowDialog();
        }
    }

    private static final class ConferenceCFPImportWorker extends SwingWorker<ConferenceSolution, Void> {

        private final SolutionBusiness<ConferenceSolution, ?> solutionBusiness;
        private final SolutionPanel<ConferenceSolution> solutionPanel;
        private String conferenceBaseUrl;

        private final JDialog dialog;

        public ConferenceCFPImportWorker(SolutionBusiness<ConferenceSolution, ?> solutionBusiness,
                SolutionPanel<ConferenceSolution> solutionPanel,
                String conferenceBaseUrl) {
            this.solutionBusiness = solutionBusiness;
            this.solutionPanel = solutionPanel;
            this.conferenceBaseUrl = conferenceBaseUrl;
            dialog = new JDialog(solutionPanel.getSolverAndPersistenceFrame(), true);
            JPanel contentPane = new JPanel(new BorderLayout(10, 10));
            contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            contentPane.add(new JLabel("Importing CFP data in progress..."), BorderLayout.NORTH);
            JProgressBar progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
            progressBar.setIndeterminate(true);
            contentPane.add(progressBar, BorderLayout.CENTER);
            JButton button = new JButton("Cancel");
            button.addActionListener(e -> cancel(false));
            contentPane.add(button, BorderLayout.SOUTH);
            dialog.setContentPane(contentPane);
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    cancel(false);
                }
            });
            dialog.pack();
            dialog.setLocationRelativeTo(solutionPanel.getSolverAndPersistenceFrame());
        }

        public void executeAndShowDialog() {
            execute();
            dialog.setVisible(true);
        }

        @Override
        protected ConferenceSolution doInBackground() {
            return new ConferenceSchedulingCfpDevoxxImporter(conferenceBaseUrl).importSolution();
        }

        @Override
        protected void done() {
            dialog.dispose();
            if (isCancelled()) {
                return;
            }
            ConferenceSolution cfpProblem;
            try {
                cfpProblem = get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Importing was interrupted.", e);
            } catch (ExecutionException e) {
                JOptionPane.showMessageDialog(solutionPanel,
                        "CFP import failed.\nThe next dialog will explain the cause.\n\n"
                                + "Fix it in ConferenceSchedulingCfpDevoxxImporter.java in the timefold repository.");
                throw new IllegalStateException("Importing failed.", e.getCause());
            }
            solutionBusiness.setSolution(cfpProblem);
            solutionBusiness.setSolutionFileName(solutionBusiness.getSolution().getConferenceName());
            JOptionPane.showMessageDialog(solutionPanel, "CFP data imported successfully.");
            solutionPanel.getSolverAndPersistenceFrame().setSolutionLoaded(null);
        }
    }
}
