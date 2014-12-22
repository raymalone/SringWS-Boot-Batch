package com.ouc.elster.mas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.ws.WebServiceException;

import mas.wsdl.ExecuteReportRequestType;
import mas.wsdl.ExecuteReportResponseType;
import mas.wsdl.ObjectFactory;
import mas.wsdl.ReportParameterType;
import mas.wsdl.ReportSignatureType;

import org.apache.http.conn.ConnectTimeoutException;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

import com.ouc.elster.mas.utils.ElsterExtractException;
import com.ouc.elster.mas.utils.FileUtils;

public class MasClient extends WebServiceGatewaySupport {

  Logger logger = LoggerFactory.getLogger("com.ouc.elster.mas.MASClient");

  @Autowired
  private ApplicationContext appContext;

  @Value("${USER}")
  private String USER;

  @Value("${DEFAULT_URI}")
  private String DEFAULT_URI;

  @Value("${REPORT_NAME}")
  private String REPORT_NAME;

  @Value("${TIMEZONE_ID}")
  private String TIMEZONE_ID;

  @Value("${OBSERVE_DST}")
  private String OBSERVE_DST;

  @Value("${START_TIME}")
  private String START_TIME;

  @Value("${END_TIME}")
  private String END_TIME;

  @Value("${START_INDEX}")
  private String START_INDEX;

  @Value("${TOTAL_MAX_RECORD_COUNT}")
  private String TOTAL_MAX_RECORD_COUNT;

  @Value("${MAX_RECORD_COUNT_PER_REQUEST}")
  private Integer MAX_RECORD_COUNT_PER_REQUEST;

  @Value("${EVENT_NAME}")
  private String EVENT_NAME;

  @Value("${REPORT_PATH}")
  private String REPORT_PATH;

  @Value("${REPORT_RESPONSE_FILENAME}")
  private String RESPONSE_FILENAME;

  @Value("${LOG.DIR}")
  private String LOG_DIR;

  @Value("${REPORT_REQUEST_FILENAME}")
  private String REPORT_REQUEST_FILENAME;

  @Value("${REPORT_RESPONSE_FILENAME}")
  private String REPORT_RESPONSE_FILENAME;

  @Value("${ARCHIVE_FILE_EXTENSION}")
  private String ARCHIVE_FILE_EXTENSION;

  @Value("${REPORT_FILE_EXTENSION}")
  private String REPORT_FILE_EXTENSION;

  @Value("${KEEP_RESPONSE_AFTER_ZIP}")
  private boolean KEEP_RESPONSE_AFTER_ZIP;

  @Value("${TIMEOUT_CONNECTION}")
  private int TIMEOUT_CONNECTION;

  @Value("${TIMEOUT_READ}")
  private int TIMEOUT_READ;

  private ExecuteReportRequestType requestType;

  private ReportParameterType timezoneParam;

  private ReportParameterType observeDstParam;

  private ObjectFactory objectFactory;

  private ReportParameterType maxResultsParam;

  private ReportParameterType startIndexParam;

  private ReportParameterType eventNameParam;

  private ReportSignatureType signature;

  private ArrayList<ReportParameterType> parameters = new ArrayList<ReportParameterType>();

  private DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss z");

  private DateTime now = new DateTime().withTimeAtStartOfDay();

  private ArrayList<Interval> dateTimeRangeList = new ArrayList<Interval>();

  private JAXBElement<ExecuteReportRequestType> reportRequestType;

  private MasClientInterceptor maci;

  public void executeReport() throws Exception {
    try {
      // Initialize object factory
      objectFactory = new ObjectFactory();

      // Initialize types
      requestType = objectFactory.createExecuteReportRequestType();
      timezoneParam = objectFactory.createReportParameterType();
      observeDstParam = objectFactory.createReportParameterType();
      maxResultsParam = objectFactory.createReportParameterType();
      startIndexParam = objectFactory.createReportParameterType();
      eventNameParam = objectFactory.createReportParameterType();
      signature = objectFactory.createReportSignatureType();
      reportRequestType = objectFactory.createExecuteReportRequest(requestType);

      // Initialize client intercepter (this modified the request
      // and handles the response)
      maci = appContext.getBean(MasClientInterceptor.class);
      getWebServiceTemplate().setInterceptors(new ClientInterceptor[] {maci});

      // If startTime and endTime are not provided in the
      // app.properties , we will use the time range yesterday
      // until today
      if (StringUtils.isEmpty(START_TIME) || StringUtils.isEmpty(END_TIME)) {
        START_TIME = dateFormatter.print(now.minusDays(1));
        END_TIME = dateFormatter.print(now);
      }

      // Set type values, omit those that will change
      // when request is broken into multiple calls
      signature.setName(REPORT_NAME);

      timezoneParam.setName("Timezone");
      timezoneParam.setType("Timezone");
      timezoneParam.setValue(TIMEZONE_ID);

      observeDstParam.setName("ObservesDST");
      observeDstParam.setType("ElsterBoolean");
      observeDstParam.setValue(OBSERVE_DST);

      startIndexParam.setName("Start Index");
      startIndexParam.setType("xsd:string");
      startIndexParam.setValue(START_INDEX);

      eventNameParam.setName("Event Name");
      eventNameParam.setType("xsd:string");
      eventNameParam.setValue(EVENT_NAME);

      processRange(new Interval(dateFormatter.parseDateTime(START_TIME),
          dateFormatter.parseDateTime(END_TIME)));

      for (Interval interval : dateTimeRangeList) {
        getEventRecords(interval);
      }
      if (!CollectionUtils.isEmpty(dateTimeRangeList)) {
        String zipFileName =
            new StringBuilder(REPORT_PATH).append(REPORT_RESPONSE_FILENAME)
                .append(StringUtils.replace(StringUtils.trimAllWhitespace(START_TIME), ":", ""))
                .append("_thru_")
                .append(StringUtils.replace(StringUtils.trimAllWhitespace(END_TIME), ":", ""))
                .append(".").append(ARCHIVE_FILE_EXTENSION).toString();

        try {
          FileUtils.createZipFileContainingResponses(REPORT_PATH, zipFileName,
              REPORT_FILE_EXTENSION);
          logger.info("Successfully created zip file " + zipFileName);
        } catch (IOException e) {
          logger.error("Unable to zip response files", e.getMessage());
          throw new ElsterExtractException(e);
        }
      } else {
        logger.error("No extract reports generated.");
        throw new ElsterExtractException("No extract reports generated.");
      }
    } catch (Exception e) {
      throw e;
    }
  }

  private void processRange(List<Interval> dateRangeList) throws Exception {
    // for (Interval interval : dateRangeList) {
    for (int i = 0; i < dateRangeList.size(); i++) {
      logger.debug("processing range for " + dateRangeList.get(i).toString());
      processRange(dateRangeList.get(i));
    }
  }

  private void processRange(Interval dateRange) throws Exception {
    logger.debug("Max allowed records per request is " + MAX_RECORD_COUNT_PER_REQUEST.toString());
    logger.info("Processing interval " + dateRange);
    int recordCount = getRecordCount(dateRange);

    // if there are no records, do not add the range
    // to the range list, there are no events to pull
    if (recordCount > 0) {
      if (MAX_RECORD_COUNT_PER_REQUEST > recordCount) {
        dateTimeRangeList.add(dateRange);
        logger.info(dateRange + ", with a count of " + recordCount + ". Added Range to range list");

      } else {
        logger.info("Record count for interval " + dateRange
            + " is greater than the configured threshold");
        logger.info("count = " + recordCount + ". Max allowed = " + MAX_RECORD_COUNT_PER_REQUEST);
        processRange(splitDateRange(dateRange));
      }
    }
  }

  private List<Interval> splitDateRange(Interval dateRange) {
    List<Interval> splitRanges = new ArrayList<Interval>();
    Duration dur = new Duration(dateRange);

    int diffInMillis = (int) (dur.getMillis()) / 2;

    DateTime endDateTimeSubset = dateRange.getStart().plusMillis(diffInMillis);

    splitRanges.add(new Interval(dateRange.getStart(), endDateTimeSubset));
    splitRanges.add(new Interval(endDateTimeSubset, dateRange.getEnd()));

    for (Interval interval : splitRanges) {
      logger.debug("Range is " + dateFormatter.print(interval.getStart()) + " through "
          + dateFormatter.print(interval.getEnd()));
    }

    logger.info("Splitting interval to " + splitRanges.get(0) + " and " + splitRanges.get(1));

    return splitRanges;
  }

  // send a request to MAS only requesting one max result
  // so we can parse the record count for the interval
  private int getRecordCount(Interval dateRange) throws Exception {
    ReportParameterType startTimeParam = objectFactory.createReportParameterType();
    ReportParameterType endTimeParam = objectFactory.createReportParameterType();

    startTimeParam.setName("Start Time");
    startTimeParam.setType("StandardDateTimePlusTimeZoneType");
    startTimeParam.setValue(dateFormatter.print(dateRange.getStart()));

    endTimeParam.setName("End Time");
    endTimeParam.setType("StandardDateTimePlusTimeZoneType");
    endTimeParam.setValue(dateFormatter.print(dateRange.getEnd()));

    maxResultsParam.setName("Max Results");
    maxResultsParam.setType("xsd:string");
    maxResultsParam.setValue("1");

    parameters.clear();
    parameters.add(timezoneParam);
    parameters.add(observeDstParam);
    parameters.add(maxResultsParam);
    parameters.add(eventNameParam);
    parameters.add(startIndexParam);
    parameters.add(startTimeParam);
    parameters.add(endTimeParam);

    signature.getParameter().clear();
    signature.getParameter().addAll(parameters);

    requestType.setReportSignature(signature);

    int recordCount = 0;
    Object response = null;
    try {
      response = getWebServiceTemplate().marshalSendAndReceive(reportRequestType);
      logger.debug("Getting record count for interval " + dateRange);
    } catch (WebServiceException e) {
      if ("unrecoverable".equals(e.getMessage())) {
        throw new ElsterExtractException(e);
      }
    } catch (Exception e) {
      if (e instanceof ElsterExtractException) {
        throw e;
      } else if (e.getCause() instanceof java.net.SocketTimeoutException) {
        logger.error("Get record count response did not complete in the configured time ("
            + TIMEOUT_READ + " miliseconds)", e.getMessage());
      } else if (e.getCause() instanceof ConnectTimeoutException) {
        logger.error(
            "Get record count request did not successfully connect in the configured time ("
                + TIMEOUT_CONNECTION + " miliseconds)", e.getMessage());
      }
      logger.error("Unhandled Exception", e.getMessage());
      logger.info("Recovering from error, trying to get count again for " + dateRange.toString());
      getRecordCount(dateRange);
    }
    if (response instanceof JAXBElement<?>) {
      @SuppressWarnings("unchecked")
      JAXBElement<ExecuteReportResponseType> responseElement =
          (JAXBElement<ExecuteReportResponseType>) response;

      if (responseElement.getValue().getStatus().isIsError()) {
        logger.error("Error response returned");
        logger.error(responseElement.getValue().getStatus().getOverallMessage().getText());
        throw new ElsterExtractException("Error response returned");
      } else if (responseElement.getValue() != null
          && responseElement.getValue().getReport() != null
          && responseElement.getValue().getReport().getRecords() != null) {
        recordCount = responseElement.getValue().getReport().getHeader().getTotalRecordCount();
      }

    }
    logger.debug("Record count is " + recordCount);
    return recordCount;
  }

  private void getEventRecords(Interval interval) throws Exception {
    ReportParameterType startTimeParam = objectFactory.createReportParameterType();
    ReportParameterType endTimeParam = objectFactory.createReportParameterType();

    startTimeParam.setName("Start Time");
    startTimeParam.setType("StandardDateTimePlusTimeZoneType");
    startTimeParam.setValue(dateFormatter.print(interval.getStart()));

    endTimeParam.setName("End Time");
    endTimeParam.setType("StandardDateTimePlusTimeZoneType");
    endTimeParam.setValue(dateFormatter.print(interval.getEnd()));

    maxResultsParam.setName("Max Results");
    maxResultsParam.setType("xsd:string");
    maxResultsParam.setValue(TOTAL_MAX_RECORD_COUNT);

    parameters.clear();
    parameters.add(timezoneParam);
    parameters.add(observeDstParam);
    parameters.add(maxResultsParam);
    parameters.add(eventNameParam);
    parameters.add(startIndexParam);
    parameters.add(startTimeParam);
    parameters.add(endTimeParam);

    signature.getParameter().clear();
    signature.getParameter().addAll(parameters);

    requestType.setReportSignature(signature);

    try {
      logger.info("Getting events for interval " + interval);
      Object response = getWebServiceTemplate().marshalSendAndReceive(reportRequestType);

      if (response instanceof JAXBElement<?>) {
        @SuppressWarnings("unchecked")
        JAXBElement<ExecuteReportResponseType> responseElement =
            (JAXBElement<ExecuteReportResponseType>) response;

        if (responseElement.getValue().getStatus().isIsError()) {
          logger.error("Error response returned");
          logger.error(responseElement.getValue().getStatus().getOverallMessage().getText());

          throw new ElsterExtractException("Error response returned");
        }
      }

    } catch (Exception e) {
      if (e instanceof ElsterExtractException) {
        throw e;
      }

      if (e.getCause() instanceof java.net.SocketTimeoutException) {
        logger.error("Get event records response did not complete in the configured time ("
            + TIMEOUT_READ + " miliseconds)", e.getMessage());
      } else if (e.getCause() instanceof ConnectTimeoutException) {
        logger.error(
            "Get event records request did not successfully connect in the configured time ("
                + TIMEOUT_CONNECTION + " miliseconds)", e.getMessage());
      }
      logger.error("Unhandled Exception", e.getMessage());
      logger.info("Recovering from error, trying to get event records again for "
          + interval.toString());
      getEventRecords(interval);
    }
  }
}
