package rs.ftn.uns.novisad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rs.ftn.uns.novisad.model.AccountRequest;
import rs.ftn.uns.novisad.model.User;

/**
 * Slanje mejlova o vaznim dogadjajima: obrada zahteva za registraciju [A1]
 * i promena lozinke [K9].
 * <p>
 * Dok SMTP nije podesen (app.mail.enabled = false), poruka se samo belezi u log.
 * Time razvoj ne zavisi od mejl servera, a ukljucivanjem zastavice i unosom
 * spring.mail.* podataka slanje pocinje da radi bez izmena u kodu.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.mail.enabled}")
    private boolean enabled;

    @Value("${app.mail.from}")
    private String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    /** [A1] Zahtev za registraciju je prihvacen. */
    @Async
    public void sendRegistrationApproved(User user) {
        send(user.getEmail(),
                "Vas nalog je odobren",
                """
                Postovani/a %s %s,

                vas zahtev za registraciju na aplikaciji Novi Sad je prihvacen.
                Sada se mozete prijaviti svojom email adresom i lozinkom.

                Prijatno koriscenje!
                """.formatted(user.getFirstName(), user.getLastName()));
    }

    /** [A1] Zahtev za registraciju je odbijen. */
    @Async
    public void sendRegistrationRejected(AccountRequest request) {
        String reason = request.getRejectionReason() == null || request.getRejectionReason().isBlank()
                ? ""
                : "\nRazlog: " + request.getRejectionReason() + "\n";

        send(request.getEmail(),
                "Vas zahtev za registraciju je odbijen",
                """
                Postovani/a %s %s,

                nazalost, vas zahtev za registraciju na aplikaciji Novi Sad je odbijen.
                %s
                """.formatted(request.getFirstName(), request.getLastName(), reason));
    }

    /** [K9] Obavestenje da je lozinka promenjena. */
    @Async
    public void sendPasswordChanged(User user) {
        send(user.getEmail(),
                "Lozinka je promenjena",
                """
                Postovani/a %s %s,

                lozinka za vas nalog na aplikaciji Novi Sad je upravo promenjena.
                Ako ovu promenu niste izvrsili vi, odmah kontaktirajte administratora.
                """.formatted(user.getFirstName(), user.getLastName()));
    }

    private void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("Mejl nije poslat jer je slanje iskljuceno [primalac={}, naslov={}]", to, subject);
            return;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Slanje mejla je ukljuceno, ali JavaMailSender nije podesen [primalac={}]", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.info("Poslat mejl [primalac={}, naslov={}]", to, subject);
        } catch (Exception ex) {
            // Neuspeh slanja mejla ne sme da obori poslovnu operaciju.
            log.error("Neuspesno slanje mejla [primalac={}]: {}", to, ex.getMessage());
        }
    }
}
