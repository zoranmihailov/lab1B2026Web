package mk.ukim.finki.wp.lab1b2026.service;

import mk.ukim.finki.wp.lab1b2026.model.Player;

import java.util.List;

public interface PlayerService {
    List<Player> listPlayers();
    Player findByPlayerId(String playerId);
}
