package com.ouc.elster.mas;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.ouc.elster.mas.config.MasConfiguration;
import com.ouc.elster.mas.utils.ElsterExtractException;
import com.ouc.elster.mas.utils.EmailUtil;
import com.ouc.elster.mas.utils.Status;

@Component
public class Application {
  static Logger logger = LoggerFactory.getLogger("com.ouc.elster.mas.Application");

  public static void main(String[] args) {

    ApplicationContext ctx = SpringApplication.run(MasConfiguration.class, args);
    MasClient masClient = ctx.getBean(MasClient.class);
    EmailUtil email = ctx.getBean(com.ouc.elster.mas.utils.EmailUtil.class);

    Properties prop = new Properties();

    try {
      InputStream inputStream =
          Application.class.getClassLoader().getResourceAsStream("application.properties");
      prop.load(inputStream);

      masClient.executeReport();
      // send success email if report is generated
      email.emailStatusReport("elsterExtractLog.log",
          new File(prop.getProperty("LOG.DIR") + "/logFile.log"), Status.SUCCESS);
    } catch (IOException ioe) {
      logger.error("unable to find properties file to get logging directory");
    } catch (Exception e) {
      logger
          .error("Unrecoverable exception occurred, Elster Event Extract exiting", e.getMessage());
      try {
        email.emailStatusReport("error.log",
            new File(prop.getProperty("LOG.DIR") + "/logFile.log"), Status.FAIL);
      } catch (MessagingException e1) {
        logger.error("Unable to send status email", e1.getMessage());
        throw new ElsterExtractException(e1);
      }
      throw new ElsterExtractException(e);
    }
  }
}
