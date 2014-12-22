package com.ouc.elster.mas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import com.ouc.elster.mas.MasClient;
import com.ouc.elster.mas.MasClientInterceptor;
import com.ouc.elster.mas.utils.EmailUtil;

@Configuration
@ComponentScan(basePackages = "com.ouc.elster.mas")
@EnableAutoConfiguration
@PropertySources(value = {@PropertySource("classpath:application.properties"),
    @PropertySource(value = "file:application.properties", ignoreResourceNotFound = true)})
public class MasConfiguration {
  Logger logger = LoggerFactory.getLogger("com.ouc.elster.mas.MasConfiguration");

  @Value("${DEFAULT_URI}")
  private String defaultUri;

  @Value("${USER}")
  private String USER;

  @Value("${TIMEOUT_CONNECTION}")
  private int timeoutConnection;

  @Value("${TIMEOUT_READ}")
  private int timeoutRead;

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Bean
  public HttpComponentsMessageSender httpComponentsMessageSender() {
    HttpComponentsMessageSender httpComponentMessageSender = new HttpComponentsMessageSender();
    httpComponentMessageSender.setConnectionTimeout(timeoutConnection);
    httpComponentMessageSender.setReadTimeout(timeoutRead);

    return httpComponentMessageSender;
  }

  @Bean
  public EmailUtil emailUtil() {
    return new EmailUtil();
  }

  @Bean
  public Jaxb2Marshaller marshaller() {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setContextPath("mas.wsdl");
    return marshaller;
  }

  @Bean
  public MasClient masClient(Jaxb2Marshaller marshaller) {
    MasClient client = new MasClient();
    client.setMessageSender(httpComponentsMessageSender());
    client.setDefaultUri(defaultUri);
    client.setMarshaller(marshaller);
    client.setUnmarshaller(marshaller);
    return client;
  }

  @Bean
  public MasClientInterceptor masClientInterceptor() {
    return new MasClientInterceptor();
  }

  @Bean
  public EncryptDecrypt encryptDecrypt() {
    EncryptDecrypt ed = null;
    try {
      ed = new EncryptDecrypt("application.properties", "PASSWORD", "PASSWORD.is.encrypted");
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
    return ed;
  }

}
