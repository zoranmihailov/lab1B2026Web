package mk.ukim.finki.wp.lab1b2026.service.impl;

import mk.ukim.finki.wp.lab1b2026.model.Player;
import mk.ukim.finki.wp.lab1b2026.model.Team;
import mk.ukim.finki.wp.lab1b2026.repository.PlayerRepository;
import mk.ukim.finki.wp.lab1b2026.repository.TeamRepository;
import mk.ukim.finki.wp.lab1b2026.service.TeamService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public TeamServiceImpl(TeamRepository teamRepository, PlayerRepository playerRepository) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }


    @Override
    public List<Team> listTeams() {
        return teamRepository.findAll();
    }

    @Override
    public Team findById(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Team not found"));
    }

    @Override
    public Team addPlayerToTeam(Long teamId, String playerId) {
        Team team = findById(teamId);
        Player player = playerRepository.findByPlayerId(playerId).orElseThrow(IllegalArgumentException::new);

        Optional<Team> currTeam = teamRepository.findAll().stream().filter(t -> t.getPlayers().stream()
                .anyMatch(p -> p.getPlayerId().equals(playerId))).findFirst();

        if(currTeam.isPresent()) {
            throw new IllegalArgumentException("Player already plays for "+currTeam.get().getName());
        }

        boolean jerseyTaken = team.getPlayers().stream()
                .anyMatch(p -> p.getJerseyNumber()==player.getJerseyNumber());

        if(jerseyTaken) {
            throw new IllegalArgumentException("Jersey number "+ player.getJerseyNumber() +" is already taken in this team");
        }

        team.getPlayers().add(player);
        return teamRepository.save(team);
    }
}
