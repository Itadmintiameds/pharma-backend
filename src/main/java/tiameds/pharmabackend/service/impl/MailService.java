package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendOtp(String to,String otp){

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Login OTP");
        message.setText(
                "Your OTP is : " + otp +
                        "\nValid for 5 minutes");

        mailSender.send(message);
    }

    public void sendVerificationOtp(String to,String otp){

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Email Verification OTP");
        message.setText(
                "Your email verification OTP is : " + otp +
                        "\nValid for 5 minutes");

        mailSender.send(message);
    }

    public void sendPasswordResetOtp(String to,String otp){

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Password Reset OTP");
        message.setText(
                "Your password reset OTP is : " + otp +
                        "\nValid for 5 minutes" +
                        "\n\nIf you did not request a password reset, ignore this email." +
                        " Your password will not change.");

        mailSender.send(message);
    }
}
