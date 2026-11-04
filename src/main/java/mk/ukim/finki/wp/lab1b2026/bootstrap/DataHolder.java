package mk.ukim.finki.wp.lab1b2026.bootstrap;

import jakarta.annotation.PostConstruct;
import mk.ukim.finki.wp.lab1b2026.model.Player;
import mk.ukim.finki.wp.lab1b2026.model.Team;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataHolder {
    public static List<Team> teams = null;
    public static List<Player> players = null;

    @PostConstruct
    public void init(){
        teams = new ArrayList<>();
        players = new ArrayList<>();

        teams.add(new Team(1L, "Liverpool", "Liverpool",new ArrayList<>()));
        teams.add(new Team(2L, "Newcastle", "Newcastle",new ArrayList<>()));
        teams.add(new Team(3L, "Sevilla", "Sevilla",new ArrayList<>()));
        teams.add(new Team(4L, "Olympique Marseille", "Marseille",new ArrayList<>()));
        teams.add(new Team(5L, "Boca Juniors", "Buenos Aires",new ArrayList<>()));

        players.add(new Player("P1", "Marko", "Ilievski", "Midfielder", 10));
        players.add(new Player("P2", "Stefan", "Trajkov", "Goalkeeper", 1));
        players.add(new Player("P3", "Ana", "Petreska", "Forward", 9));
        players.add(new Player("P4", "Nikola", "Stojanov", "Defender", 5));
        players.add(new Player("P5", "Elena", "Dimitrova", "Midfielder", 7));
        players.add(new Player("P6", "Filip", "Angelov", "Forward", 10));
        players.add(new Player("P7", "Viktor", "Kostov", "Defender", 3));
        players.add(new Player("P8", "Sara", "Jovanova", "Forward", 11));
        players.add(new Player("P9", "David", "Naumov", "Defender", 4));
        players.add(new Player("P10", "Maja", "Ristova", "Midfielder", 8));
    }
}