package mk.ukim.finki.wp.lab1b2026.repository;

import mk.ukim.finki.wp.lab1b2026.model.Player;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository {
    List<Player> findAll();
    Optional<Player> findByPlayerId(String playerId);
}
