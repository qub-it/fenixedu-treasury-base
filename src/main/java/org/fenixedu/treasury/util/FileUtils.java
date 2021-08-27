/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * 	(o) Redistributions of source code must retain the above
 * 	copyright notice, this list of conditions and the following
 * 	disclaimer.
 *
 * 	(o) Redistributions in binary form must reproduce the
 * 	above copyright notice, this list of conditions and the
 * 	following disclaimer in the documentation and/or other
 * 	materials provided with the distribution.
 *
 * 	(o) Neither the name of Quorum Born IT nor the names of
 * 	its contributors may be used to endorse or promote products
 * 	derived from this software without specific prior written
 * 	permission.
 *
 * 	(o) Universidade de Lisboa and its respective subsidiary
 * 	Serviços Centrais da Universidade de Lisboa (Departamento
 * 	de Informática), hereby referred to as the Beneficiary,
 * 	is the sole demonstrated end-user and ultimately the only
 * 	beneficiary of the redistributed binary form and/or source
 * 	code.
 *
 * 	(o) The Beneficiary is entrusted with either the binary form,
 * 	the source code, or both, and by accepting it, accepts the
 * 	terms of this License.
 *
 * 	(o) Redistribution of any binary form and/or source code is
 * 	only allowed in the scope of the Universidade de Lisboa
 * 	FenixEdu(™)’s implementation projects.
 *
 * 	(o) This license and conditions of redistribution of source
 * 	code/binary can oly be reviewed by the Steering Comittee of
 * 	FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT�? BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.treasury.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

public class FileUtils {

    // Cluster safe global unique temporary filename
    private static final String TEMPORARY_FILE_GLOBAL_UNIQUE_NAME_PREFIX = UUID.randomUUID().toString();

    private static final char[] SEPARATOR_CHARS = new char[] { '\\', '/' };

    public static String getTemporaryFileBaseName() {
        return TEMPORARY_FILE_GLOBAL_UNIQUE_NAME_PREFIX;
    }

    public static File createTemporaryFile() throws IOException {
        final File temporaryFile = File.createTempFile(TEMPORARY_FILE_GLOBAL_UNIQUE_NAME_PREFIX, "");
        // In case anything fails the file will be cleaned when jvm
        // shutsdown
        temporaryFile.deleteOnExit();
        return temporaryFile;
    }

    public static File copyToTemporaryFile(final InputStream inputStream) throws IOException {
        final File temporaryFile = createTemporaryFile();

        FileOutputStream targetFileOutputStream = null;
        try {
            targetFileOutputStream = new FileOutputStream(temporaryFile);
            ByteStreams.copy(inputStream, targetFileOutputStream);
        } finally {
            if (targetFileOutputStream != null) {
                targetFileOutputStream.close();
            }
            inputStream.close();
        }

        return temporaryFile;
    }

    public static String getFilenameOnly(final String filename) {
        for (final char separatorChar : SEPARATOR_CHARS) {
            if (filename.lastIndexOf(separatorChar) != -1) {
                return filename.substring(filename.lastIndexOf(separatorChar) + 1);
            }
        }

        return filename;
    }

    public static File unzipFile(File file) throws IOException {
        File tempDir = Files.createTempDir();
        FileInputStream fileInputStream = new FileInputStream(file);

        FileUtils.copyFileToAnotherDirWithRelativePaths(file.getParentFile(), tempDir, file);

        ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);

        ZipEntry zipEntry = zipInputStream.getNextEntry();
        File zipContentFile = null;
        File zipContentFileParentDir = null;
        while (zipEntry != null) {
            zipEntry.getName();
            zipContentFile = new File(tempDir, zipEntry.getName());
            zipContentFileParentDir = zipContentFile.getParentFile();
            zipContentFileParentDir.mkdirs();

            if (!zipEntry.isDirectory()) {
                zipContentFile.createNewFile();
            } else {
                zipContentFile.mkdirs();
            }

            zipContentFile.deleteOnExit();

            if (!zipEntry.isDirectory() && zipContentFile.exists() && zipContentFile.canWrite()) {
                OutputStream zipOs = new FileOutputStream(zipContentFile);
                ByteStreams.copy(zipInputStream, zipOs);
                zipOs.close();
            }

            zipInputStream.closeEntry();
            zipEntry = zipInputStream.getNextEntry();
        }

        return tempDir;
    }

    public static String makeRelativePath(String absoluteParentPath, String originalAbsoluteFilePath, String uniqueId) {
        if (originalAbsoluteFilePath != null && absoluteParentPath != null
                && originalAbsoluteFilePath.length() > absoluteParentPath.length()) {
            return originalAbsoluteFilePath.substring(absoluteParentPath.length() + 1);
        } else {
            return uniqueId;
        }
    }

    public static File copyFileToAnotherDirWithRelativePaths(File srcDir, File destDir, File originalFile)
            throws FileNotFoundException, IOException {
        String relativePath = makeRelativePath(srcDir.getAbsolutePath(), originalFile.getAbsolutePath(), "");
        File newFile = new File(destDir, relativePath);
        FileInputStream fis = new FileInputStream(originalFile);
        FileOutputStream fos = new FileOutputStream(newFile);
        ByteStreams.copy(fis, fos);
        fis.close();
        fos.close();
        return newFile;
    }

    public static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            }
            file.delete();
        }
        directory.delete();
    }

    public static byte[] readByteArray(File file) throws IOException {
        return Files.toByteArray(file);
    }
}
