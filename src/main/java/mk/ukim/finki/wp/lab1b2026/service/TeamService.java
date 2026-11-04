package mk.ukim.finki.wp.lab1b2026.service;

import mk.ukim.finki.wp.lab1b2026.model.Team;

import java.util.List;

public interface TeamService {
    List<Team> listTeams();
    Team findById(Long id);
    Team addPlayerToTeam(Long teamId, String playerId);
}
