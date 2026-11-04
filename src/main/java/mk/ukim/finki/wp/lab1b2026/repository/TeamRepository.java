package mk.ukim.finki.wp.lab1b2026.repository;

import mk.ukim.finki.wp.lab1b2026.model.Team;

import java.util.List;
import java.util.Optional;

public interface TeamRepository {
    List<Team> findAll();
    Optional<Team> findById(Long id);
    Team save(Team team);
}