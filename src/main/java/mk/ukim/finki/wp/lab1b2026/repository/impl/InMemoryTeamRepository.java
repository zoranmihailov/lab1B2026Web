package mk.ukim.finki.wp.lab1b2026.repository.impl;


import mk.ukim.finki.wp.lab1b2026.bootstrap.DataHolder;
import mk.ukim.finki.wp.lab1b2026.model.Team;
import mk.ukim.finki.wp.lab1b2026.repository.TeamRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryTeamRepository implements TeamRepository {
    @Override
    public List<Team> findAll() {
        return DataHolder.teams;
    }

    @Override
    public Optional<Team> findById(Long id) {
        return DataHolder.teams.stream().filter(team -> team.getId().equals(id)).findFirst();
    }

    @Override
    public Team save(Team team) {
        DataHolder.teams.removeIf(c -> c.getId().equals(team.getId()));
        DataHolder.teams.add(team);
        return team;
    }
}
