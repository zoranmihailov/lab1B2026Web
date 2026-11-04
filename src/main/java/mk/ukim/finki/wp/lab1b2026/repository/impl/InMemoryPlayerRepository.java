package mk.ukim.finki.wp.lab1b2026.repository.impl;

import mk.ukim.finki.wp.lab1b2026.bootstrap.DataHolder;
import mk.ukim.finki.wp.lab1b2026.model.Player;
import mk.ukim.finki.wp.lab1b2026.repository.PlayerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryPlayerRepository implements PlayerRepository {
    @Override
    public List<Player> findAll() {
        return DataHolder.players;
    }

    @Override
    public Optional<Player> findByPlayerId(String playerId) {
        return DataHolder.players.stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst();
    }
}
