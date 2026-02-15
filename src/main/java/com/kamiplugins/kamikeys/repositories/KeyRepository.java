package com.kamiplugins.kamikeys.repositories;

import com.kamiplugins.kamikeys.models.Key;
import java.util.List;
import java.util.Optional;

public interface KeyRepository {
    void save(Key key);
    Optional<Key> findByCode(String code);
    List<Key> findAll();
    List<Key> findByOrigin(String origin);
    List<Key> findByType(String type);
    List<Key> findByExclusiveToName(String playerName);
    void update(Key key); // ← ADICIONADO: Método update
    void delete(String code);
}