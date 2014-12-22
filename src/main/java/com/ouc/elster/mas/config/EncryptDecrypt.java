package com.ouc.elster.mas.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptDecrypt {
  Logger logger = LoggerFactory.getLogger("com.ouc.elster.mas.MASClient");

  private final String propertyFileName;
  private final String propertyKey;
  private final String isPropertyKeyEncrypted;

  public final String decryptedUserPassword;

  /**
   * The constructor does most of the work. It initializes all final variables and invoke two
   * methods for encryption and decryption job. After successful job the constructor puts the
   * decrypted PASSWORD in variable to be retrieved by calling class.
   * 
   * 
   * @param pPropertyFileName /Name of the properties file that contains the PASSWORD
   * @param pUserPasswordKey /Left hand side of the PASSWORD property as key.
   * @param pIsPasswordEncryptedKey /Key in the properties file that will tell us if the PASSWORD is
   *        already encrypted or not
   * 
   * @throws Exception
   */
  public EncryptDecrypt(String pPropertyFileName, String pUserPasswordKey,
      String pIsPasswordEncryptedKey) throws Exception {
    this.propertyFileName = pPropertyFileName;
    this.propertyKey = pUserPasswordKey;
    this.isPropertyKeyEncrypted = pIsPasswordEncryptedKey;
    try {
      encryptPropertyValue();
    } catch (ConfigurationException e) {
      throw new Exception("Problem encountered during encryption process", e);
    }
    decryptedUserPassword = decryptPropertyValue();

  }

  /**
   * The method that encrypt PASSWORD in the properties file. This method will first check if the
   * PASSWORD is already encrypted or not. If not then only it will encrypt the PASSWORD.
   * 
   * @throws ConfigurationException
   */
  private void encryptPropertyValue() throws ConfigurationException {
    logger.debug("Starting encryption operation");
    logger.debug("Start reading properties file");

    // Apache Commons Configuration
    PropertiesConfiguration config = new PropertiesConfiguration(propertyFileName);

    // Retrieve boolean properties value to see if PASSWORD is already encrypted or not
    boolean isEncrypted = Boolean.valueOf(config.getString(isPropertyKeyEncrypted));

    // Check if PASSWORD is encrypted?
    if (!isEncrypted) {
      String tmpPwd = config.getString(propertyKey);
      // System.out.println(tmpPwd);
      // Encrypt
      StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
      // This is a required PASSWORD for Jasypt. You will have to use the same PASSWORD to
      // retrieve decrypted PASSWORD later. T
      // This PASSWORD is not the PASSWORD we are trying to encrypt taken from properties file.
      encryptor.setPassword("Ek#Es93nW9Vbies");
      String encryptedPassword = encryptor.encrypt(tmpPwd);
      logger.debug("Encryption done and encrypted PASSWORD is : " + encryptedPassword);

      // Overwrite PASSWORD with encrypted PASSWORD in the properties file using Apache Commons
      // Configuration library
      config.setProperty(propertyKey, encryptedPassword);
      // Set the boolean flag to true to indicate future encryption operation that PASSWORD is
      // already encrypted
      config.setProperty(isPropertyKeyEncrypted, "true");
      // Save the properties file
      config.save();
    } else {
      logger.debug("User PASSWORD is already encrypted.\n ");
    }
  }

  private String decryptPropertyValue() throws ConfigurationException {
    logger.debug("Starting decryption");
    PropertiesConfiguration config = new PropertiesConfiguration(propertyFileName);
    String encryptedPropertyValue = config.getString(propertyKey);
    // System.out.println(encryptedPropertyValue);

    StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
    encryptor.setPassword("Ek#Es93nW9Vbies");
    String decryptedPropertyValue = encryptor.decrypt(encryptedPropertyValue);
    // System.out.println(decryptedPropertyValue);

    return decryptedPropertyValue;
  }
}
