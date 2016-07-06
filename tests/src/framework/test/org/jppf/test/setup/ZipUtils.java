/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.org.jppf.test.setup;

import java.io.*;
import java.util.zip.*;

/**
 * Utility methods used in the geofencing demo.
 * @author Laurent Cohen
 */
public class ZipUtils {
  /**
   * Zip the specified file to a new file suffixed with '.zip'.
   * @param zipPath the path of the zip file to cretae.
   * @param paths the paths of the files to add to the zip.
   * @return the path to the zipped file, or {@code null} if an I/O error occurred.
   */
  static String zipFile(final String zipPath, final String...paths) {
    File zipFile = new File(zipPath);
    try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
      byte[] buffer = new byte[2048];
      for (String path: paths) {
        File inputFile = new File(path);
        try (InputStream is = new BufferedInputStream(new FileInputStream(inputFile))) {
          ZipEntry entry = new ZipEntry(inputFile.getName());
          zos.putNextEntry(entry);
          int n;
          while ((n = is.read(buffer)) > 0) {
            zos.write(buffer, 0, n);
          }
        }
      }
      zos.finish();
      return zipPath;
    } catch(Exception e) {
      //log.debug(String.format("to zip '%s' to '%s'", path, zipPath), e);
    }
    return null;
  }
}
