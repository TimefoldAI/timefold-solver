package ai.timefold.solver.examples.travelingtournament.persistence;

import java.io.IOException;

import ai.timefold.solver.examples.common.persistence.AbstractTxtSolutionExporter;
import ai.timefold.solver.examples.common.persistence.SolutionConverter;
import ai.timefold.solver.examples.travelingtournament.app.TravelingTournamentApp;
import ai.timefold.solver.examples.travelingtournament.domain.Day;
import ai.timefold.solver.examples.travelingtournament.domain.Match;
import ai.timefold.solver.examples.travelingtournament.domain.Team;
import ai.timefold.solver.examples.travelingtournament.domain.TravelingTournament;

public class TravelingTournamentExporter extends AbstractTxtSolutionExporter<TravelingTournament> {

    private static final String OUTPUT_FILE_SUFFIX = "trick.txt";

    public static void main(String[] args) {
        SolutionConverter<TravelingTournament> converter =
                SolutionConverter.createExportConverter(TravelingTournamentApp.DATA_DIR_NAME,
                        new TravelingTournamentExporter(), new TravelingTournamentSolutionFileIO());
        converter.convertAll();
    }

    @Override
    public String getOutputFileSuffix() {
        return OUTPUT_FILE_SUFFIX;
    }

    @Override
    public TxtOutputBuilder<TravelingTournament> createTxtOutputBuilder() {
        return new TravelingTournamentOutputBuilder();
    }

    public static class TravelingTournamentOutputBuilder extends TxtOutputBuilder<TravelingTournament> {

        @Override
        public void writeSolution() throws IOException {
            int maximumTeamNameLength = 0;
            for (Team team : solution.getTeamList()) {
                if (team.getName().length() > maximumTeamNameLength) {
                    maximumTeamNameLength = team.getName().length();
                }
            }
            for (Team team : solution.getTeamList()) {
                bufferedWriter.write(String.format("%-" + (maximumTeamNameLength + 3) + "s", team.getName()));
            }
            bufferedWriter.write("\n");
            for (Team team : solution.getTeamList()) {
                bufferedWriter.write(
                        String.format("%-" + (maximumTeamNameLength + 3) + "s", team.getName().replaceAll("[\\w\\d]", "-")));
            }
            bufferedWriter.write("\n");
            for (Day day : solution.getDayList()) {
                for (Team team : solution.getTeamList()) {
                    // this could be put in a hashmap first for performance
                    boolean opponentIsHome = false;
                    Team opponentTeam = null;
                    for (Match match : solution.getMatchList()) {
                        if (match.getDay().equals(day)) {
                            if (match.getHomeTeam().equals(team)) {
                                opponentIsHome = false;
                                opponentTeam = match.getAwayTeam();
                            } else if (match.getAwayTeam().equals(team)) {
                                opponentIsHome = true;
                                opponentTeam = match.getHomeTeam();
                            }
                        }
                    }

                    if (opponentTeam != null) {
                        String opponentName = (opponentIsHome ? "@" : "") + opponentTeam.getName();
                        bufferedWriter.write(String.format("%-" + (maximumTeamNameLength + 3) + "s", opponentName));
                    }
                }
                bufferedWriter.write("\n");
            }
        }

    }

}
