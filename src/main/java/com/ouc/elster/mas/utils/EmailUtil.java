package com.ouc.elster.mas.utils;

import java.io.File;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailUtil {

  Logger logger = LoggerFactory.getLogger("com.ouc.elster.mas.utils.EmailUtil");

  @Autowired
  private ApplicationContext appContext;

  @Value("${SMTP_HOST}")
  private String SMTP_HOST = "exhub.ouc.local";

  @Value("${ORIGINATING_HOST}")
  private String ORIGINATING_HOST = "vmhte07.ouc.local";

  @Value("${FROM}")
  private String FROM = "ElsterEventExtract@vmhte07.ouc.local";

  @Value("${REPLY_TO}")
  private String REPLY_TO = "rmalone@ouc.com";

  @Value("${SUBJECT}")
  private String SUBJECT = "Auto: Elster Event Extract ";

  @Value("${SUCCESS_BODY}")
  private String SUCCESS_BODY = "test success";

  @Value("${FAIL_BODY}")
  private String FAIL_BODY = "test fail";

  @Value("${SUCCESS_TO}")
  private String SUCCESS_TO = "rmalone@ouc.com";

  @Value("${FAIL_TO}")
  private String FAIL_TO = "rmalone@ouc.com";

  @Value("${SUCCESS_CC}")
  private String SUCCESS_CC = "rmalone@ouc.com";

  @Value("${FAIL_CC}")
  private String FAIL_CC = "rmalone@ouc.com";

  @Value("${SUCCESS_BCC}")
  private String SUCCESS_BCC = "rmalone@ouc.com";

  @Value("${FAIL_BCC}")
  private String FAIL_BCC = "rmalone@ouc.com";

  @Value("${AUTH_REQUIRED}")
  private boolean AUTH_REQUIRED = false;

  @Value("${EMAIL_USER_NAME}")
  private String USER_NAME = "malor686";

  @Value("${EMAIL_PASSWORD}")
  private String PASSWORD = "";

  @Value("${EMAIL_ADDRESS_PATTERN}")
  private String EMAIL_ADDRESS_REGEX = "\\b[A-Z0-9._%-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}\\b";

  // // Main method for testing
  // public static void main(String[] args) {
  // EmailUtil es = new EmailUtil();
  // es.emailStatusReport("error.log", new File("../log/logFile.log"),
  // Status.FAIL);
  // }

  public void emailStatusReport(String fileName, File file, Enum<?> status)
      throws MessagingException {
    logger.debug("Running email failure report");
    logger.debug("Email attachment is " + file.getAbsolutePath());

    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(SMTP_HOST);

    Properties props = new Properties();
    props.put("mail.smtp.localhost", ORIGINATING_HOST);

    sender.setJavaMailProperties(props);

    if (AUTH_REQUIRED) {
      sender.setUsername(USER_NAME);
      sender.setPassword(PASSWORD);
    }

    sender.setJavaMailProperties(props);

    MimeMessage message = sender.createMimeMessage();

    // use the true flag to indicate you need a multipart message
    MimeMessageHelper helper = new MimeMessageHelper(message, true);

    // invalid email addresses in these strings will throw
    // javax.mail.internet.AddressException so if we get bad emails,
    // remove them and continue, better some people get the mail than
    // none
    if (isValidEmailAddress(status.equals(Status.SUCCESS) ? SUCCESS_TO : FAIL_TO)) {
      helper.setTo((status.equals(Status.SUCCESS) ? SUCCESS_TO : FAIL_TO).split(";"));
    } else {
      logger
          .debug("To field for notification email is empty or contains invalid email address(es)!");
    }
    if (isValidEmailAddress(status.equals(Status.SUCCESS) ? SUCCESS_CC : FAIL_CC)) {
      helper.setCc((status.equals(Status.SUCCESS) ? SUCCESS_CC : FAIL_CC).split(";"));
    } else {
      logger
          .debug("Cc field for notification email is empty or contains invalid email address(es)!");
    }
    if (isValidEmailAddress(status.equals(Status.SUCCESS) ? SUCCESS_BCC : FAIL_BCC)) {
      helper.setBcc((status.equals(Status.SUCCESS) ? SUCCESS_BCC : FAIL_BCC).split(";"));
    } else {
      logger
          .debug("Bcc field for notification email is empty or contains invalid email address(es)!");
    }

    helper.setReplyTo(REPLY_TO);
    helper.setFrom(FROM);
    helper.setSubject(SUBJECT + " " + status);
    helper.setText(status.equals(Status.SUCCESS) ? SUCCESS_BODY : FAIL_BODY, false);
    helper.addAttachment(fileName, file);
    logger.debug("*************** EMAIL ******************");
    logger.debug("** Trying to send message with...");
    logger.debug("** host: " + sender.getHost());
    logger.debug("** to: " + (status.equals(Status.SUCCESS) ? SUCCESS_TO : FAIL_TO));
    logger.debug("** from: " + FROM);
    logger.debug("** subject: " + SUBJECT + status);
    logger.debug("** body: " + (status.equals(Status.SUCCESS) ? SUCCESS_BODY : FAIL_BODY));
    sender.send(message);
  }

  /**
   * @param emailInput
   * @return
   */
  private boolean isValidEmailAddress(String emailInput) {
    Pattern emailAddressPattern = Pattern.compile(EMAIL_ADDRESS_REGEX, Pattern.CASE_INSENSITIVE);

    // if it's blank it's not valid
    if (StringUtils.isBlank(emailInput))
      return false;

    // parse email list and validate each email
    String[] emails = emailInput.split(";");
    for (String email : emails) {
      Matcher m = emailAddressPattern.matcher(email);
      if (!m.matches())
        return false;
    }

    // all clear, valid email(s) exist
    return true;
  }

}
