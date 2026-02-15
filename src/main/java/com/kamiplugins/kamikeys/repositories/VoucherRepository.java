package com.kamiplugins.kamikeys.repositories;

import com.kamiplugins.kamikeys.models.Voucher;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository {
    void save(Voucher voucher);
    Optional<Voucher> findById(String id);
    List<Voucher> findAll();
    List<Voucher> findByOwner(String ownerUuid);
    void update(Voucher voucher);
    void delete(String id);
    Optional<Voucher> findByLinkedKeyCode(String keyCode);

}