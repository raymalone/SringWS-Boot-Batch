package com.ouc.elster.mas;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebServiceException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapEnvelopeException;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.w3c.dom.NodeList;

import com.ouc.elster.mas.config.EncryptDecrypt;

@Component
public class MasClientInterceptor implements ClientInterceptor {

  Logger logger = LoggerFactory.getLogger("com.ouc.elster.mas.MasClientInterceptor");

  @Autowired
  private ApplicationContext appContext;

  @Value("${USER}")
  private String USER;

  @Value("${XMLNS}")
  private String XMLNS;

  @Value("${PASSWORD}")
  private String PASSWORD;

  @Value("${REPORT_PATH}")
  private String REPORT_PATH;

  @Value("${REPORT_REQUEST_FILENAME}")
  private String REPORT_REQUEST_FILENAME;

  @Value("${REPORT_RESPONSE_FILENAME}")
  private String RESPONSE_FILENAME;

  @Value("${REPORT_FILE_EXTENSION}")
  private String REPORT_FILE_EXTENSION;

  @Value("${WRITE_REQUEST_TO_FILE}")
  private boolean WRITE_REQUEST_TO_FILE;

  private String startTimeParamValue;

  private String endTimeParamValue;

  private String headerNodeName = "MAS_WSI_Call_Header_Login";

  private boolean countOnlyRequest;

  @Override
  public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
    // logger.error("Soap fault received");
    return true;
  }

  @Override
  public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {

    EncryptDecrypt ed = appContext.getBean(EncryptDecrypt.class);

    WebServiceMessage wsm = messageContext.getRequest();

    QName headLoginQName = new QName(XMLNS, headerNodeName, "imp");
    QName headLoginUserQName = new QName(XMLNS, "user", "imp");
    QName headLoginPasswordQName = new QName(XMLNS, "password", "imp");

    if (wsm instanceof SoapMessage) {

      try {

        ((SoapMessage) wsm).getEnvelope().addNamespaceDeclaration("imp", XMLNS);
        SOAPMessage soapMessage = ((SaajSoapMessage) wsm).getSaajMessage();
        SOAPHeader header = soapMessage.getSOAPHeader();
        SOAPHeaderElement security = header.addHeaderElement(headLoginQName);
        security.addChildElement(headLoginUserQName).setTextContent(USER);
        security.addChildElement(headLoginPasswordQName).setTextContent(ed.decryptedUserPassword);

        // parse request to get some data we need (start time, end time,
        // max results count)
        NodeList nodes = soapMessage.getSOAPBody().getElementsByTagName("ns2:Parameter");
        for (int i = 0; i < nodes.getLength(); i++) {
          NodeList childNodes = nodes.item(i).getChildNodes();
          for (int y = 0; y < childNodes.getLength(); y++) {
            if ("Name".equals(childNodes.item(y).getLocalName())
                && "Start Time".equals(childNodes.item(y).getTextContent())) {
              startTimeParamValue =
                  childNodes.item(y).getNextSibling().getNextSibling().getTextContent();
            }
            if ("Name".equals(childNodes.item(y).getLocalName())
                && "End Time".equals(childNodes.item(y).getTextContent())) {
              endTimeParamValue =
                  childNodes.item(y).getNextSibling().getNextSibling().getTextContent();
            }
            // if max results requested was 1, this is a
            // count only request. logic to follow will prevent
            // writing the response to file. We will simply
            // return the response to the MasClient for additional
            // processing.
            if ("Name".equals(childNodes.item(y).getLocalName())
                && "Max Results".equals(childNodes.item(y).getTextContent())) {
              if ("1".equals(childNodes.item(y).getNextSibling().getNextSibling().getTextContent())) {
                countOnlyRequest = true;
              } else {
                countOnlyRequest = false;
              }
            }
          }
        }

      } catch (SoapEnvelopeException e) {
        logger.error("Failed to parse request, soap body inaccessable", e.getMessage());
        throw new WebServiceException("unrecoverable");
      } catch (SOAPException e) {
        logger
            .error(
                "Request Failed difficulty setting a header, not able to send a message, or not able to get a connection with the provider",
                e);
        throw new WebServiceException("unrecoverable");
      }
    }

    // backfill the startTimeParamValue and endTimeParamValue
    // with a format we can use for filename. have to do it here
    // so we account for those values being null and having to
    // be pulled out of the request.
    startTimeParamValue =
        StringUtils.replace(StringUtils.trimAllWhitespace(startTimeParamValue), ":", "");
    endTimeParamValue =
        StringUtils.replace(StringUtils.trimAllWhitespace(endTimeParamValue), ":", "");

    if (WRITE_REQUEST_TO_FILE) {
      OutputStream outputStream = null;
      try {
        StringBuilder filename =
            new StringBuilder(REPORT_REQUEST_FILENAME).append(startTimeParamValue).append("_thru_")
                .append(endTimeParamValue).append(".").append(REPORT_FILE_EXTENSION);
        outputStream = new FileOutputStream(REPORT_PATH + filename.toString());
        messageContext.getRequest().writeTo(outputStream);
        logger.info("Successfully wrote request file, " + filename + " to " + REPORT_PATH);
      } catch (Exception e) {
        logger.error("Unhandled exception writing request to file", e.getMessage());
        throw new WebServiceException("unrecoverable");
      } finally {
        IOUtils.closeQuietly(outputStream);
      }
    }
    return true;
  }

  @Override
  public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {

    // Only write the response to file if it's a
    // request for events, not when it's a request
    // to count available events
    if (!countOnlyRequest) {
      FileOutputStream out = null;

      try {

        StringBuilder filename =
            new StringBuilder(RESPONSE_FILENAME).append(startTimeParamValue).append("_thru_")
                .append(endTimeParamValue).append(".").append(REPORT_FILE_EXTENSION);
        out = new FileOutputStream(REPORT_PATH + filename.toString());
        messageContext.getResponse().writeTo(out);
        logger.info("Successfully wrote response file, " + filename + " to " + REPORT_PATH);

      } catch (Exception e) {
        logger.error("Unhandled exception writing response to file", e.getMessage());
        throw new WebServiceException("unrecoverable");
      } finally {
        IOUtils.closeQuietly(out);
      }
    }
    return true;
  }

  public ByteArrayOutputStream marshalResponse(JAXBElement<?> element) throws Exception {
    JAXBContext jc = JAXBContext.newInstance(element.getValue().getClass());
    Marshaller marshaller = jc.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
    marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    marshaller.marshal(element, baos);
    return baos;
  }

  @Override
  public void afterCompletion(MessageContext messageContext, Exception ex)
      throws WebServiceClientException {

  }

}
