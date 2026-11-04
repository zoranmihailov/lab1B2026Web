package mk.ukim.finki.wp.lab1b2026.service.impl;

import mk.ukim.finki.wp.lab1b2026.model.Player;
import mk.ukim.finki.wp.lab1b2026.repository.PlayerRepository;
import mk.ukim.finki.wp.lab1b2026.service.PlayerService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerServiceImpl implements PlayerService {
    private final PlayerRepository playerRepository;

    public PlayerServiceImpl(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public List<Player> listPlayers() {
        return playerRepository.findAll();
    }

    @Override
    public Player findByPlayerId(String playerId) {
        return playerRepository.findByPlayerId(playerId).orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }
}
