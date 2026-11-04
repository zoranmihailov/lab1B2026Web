package mk.ukim.finki.wp.lab1b2026.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Team {
    private Long id;
    private String name;
    private String city;
    private List<Player> players;
}
