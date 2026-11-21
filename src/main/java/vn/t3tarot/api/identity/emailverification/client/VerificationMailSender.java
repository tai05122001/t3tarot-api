package vn.t3tarot.api.identity.emailverification.client;

public interface VerificationMailSender {

    void send(String email, String verificationToken);
}
