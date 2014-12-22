package com.ouc.elster.mas.utils;


public class ElsterExtractException extends RuntimeException {
  /**
   * 
   */
  private static final long serialVersionUID = 2426689299872537891L;

  /** The error message. */
  private String errorMessage;

  /**
   * This is the getter method for errorMessage.
   * 
   * @return java.lang.String
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * This method is the parametrized constructor of ElsterExtractException.
   *
   * @param errorMessage - String - error message
   */
  public ElsterExtractException(String errorMessage) {
    super(errorMessage);
    this.errorMessage = errorMessage;
  }

  public ElsterExtractException(Exception e) {
    super(e);
  }
}
