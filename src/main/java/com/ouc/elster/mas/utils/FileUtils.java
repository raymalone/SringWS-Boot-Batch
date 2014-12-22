package com.ouc.elster.mas.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
  static Logger logger = LoggerFactory.getLogger("com.ouc.elster.mas.FileUtils");

  public static void writeResponseToFile(byte[] bytes, String reportFilename) {

    // Write xml response to a file
    try {
      Files.write(Paths.get(reportFilename), bytes);
    } catch (IOException e) {
      logger.error(e.getMessage());
    } catch (Exception e) {
      logger
          .error(
              "An error occurred trying to write the response to file. Either the response was null or malformed. Please try to run again and if problems persist, report this to OUC IT or Elster",
              e.getCause());
    }
    logger.info("Successfully wrote the response to " + reportFilename);
  }

  public static void createZipFileContainingResponses(String sourceDir, String outputFile,
      String reportFileExtention) throws IOException {

    ZipOutputStream zipFile = null;
    try {
      zipFile = new ZipOutputStream(new FileOutputStream(outputFile));

      compressDirectoryToZipfile(sourceDir, zipFile, reportFileExtention);
    } catch (IOException e) {
      logger.error("Unable to add report xml files to zip", e.getMessage());
      throw e;
    } finally {
      IOUtils.closeQuietly(zipFile);
    }

  }

  private static void compressDirectoryToZipfile(String sourceDir, ZipOutputStream out,
      String entryFileExtensionFilter) throws IOException {

    Iterator<File> reportFileIterator =
        org.apache.commons.io.FileUtils.iterateFiles(new File(sourceDir),
            new String[] {entryFileExtensionFilter}, false);

    while (reportFileIterator.hasNext()) {
      File reportFile = reportFileIterator.next();
      if (!reportFile.getName().contains("Request")) {
        ZipEntry entry = new ZipArchiveEntry(reportFile.getName());
        FileInputStream in = null;
        try {
          out.putNextEntry(entry);
          in = new FileInputStream(sourceDir + reportFile.getName());
          IOUtils.copy(in, out);
          logger.info(entry.getName() + " added to zip file");
        } catch (IOException e) {
          logger.error("Unable to locate report xml files to zip", e.getMessage());
          throw e;
        } finally {
          IOUtils.closeQuietly(in);
        }
        // Delete xml responses on successful zip
        org.apache.commons.io.FileUtils.deleteQuietly(reportFile);
      }
    }
  }
}
