package com.lal.userservice.service;

import com.lal.userservice.model.CreditCard;
import com.lal.userservice.model.User;

import java.util.Optional;

public interface UserService {

    User save(User user);
    User confirm(String confirmationToken);
    User update(User user,String token);
    void addCreditCard(CreditCard creditCard, String token);
    void updateMilesAndRank(int miles, String token);
    Optional<User> findUserById(Long id);
}
