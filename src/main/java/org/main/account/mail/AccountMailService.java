package org.main.account.mail;

public interface AccountMailService {

    void sendVerification(String email, String rawToken);

    void sendPasswordReset(String email, String rawToken);
}
