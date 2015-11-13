/** Copyright (c) 2013 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center 
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center 
 * has been advised of the possibility of such damage.
*/

// package
package org.mskcc.cbio.importer.util;

// imports
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.mskcc.cbio.annotator.AnnotateTool;
import org.mskcc.cbio.annotator.AnnotatorConfig;
import org.mskcc.cbio.importer.Admin;
import org.mskcc.cbio.importer.Config;
import org.mskcc.cbio.importer.FileUtils;

import org.apache.commons.io.LineIterator;

import org.mskcc.cbio.importer.model.DatatypeMetadata;
import org.mskcc.cbio.liftover.Hg18ToHg19;
import org.mskcc.cbio.maf.MafSanitizer;
import org.mskcc.cbio.mutassessor.MutationAssessorTool;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Arrays;

public class MutationFileUtil
{
	private static final String KNOWN_ONCOTATOR_HEADER = "ONCOTATOR_VARIANT_CLASSIFICATION";
    private static final String KNOWN_ANNOTATOR_HEADER = "HGVSp_Short";
	private static final ApplicationContext context = new ClassPathXmlApplicationContext(Admin.contextFile);
    private static final Logger logger = Logger.getLogger(MutationFileUtil.class);



    // determine if this staging file has already been annotated
    public static boolean isAnnotated(String fileName) throws Exception {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(fileName), "A staging file name is required");
        return Lists.newArrayList(getColumnHeaders(fileName)).contains(KNOWN_ANNOTATOR_HEADER);
    }

	public static boolean isOncotated(String fileName) throws Exception

	{
		return MutationFileUtil.isAnnotated(Arrays.asList(getColumnHeaders(fileName)));
	}

	public static boolean isAnnotated(List<String> columnHeaders)
	{
		return columnHeaders.contains(KNOWN_ANNOTATOR_HEADER);
	}

	public static boolean isEmpty(String filename) throws Exception
	{
		File file = new File(filename);
		FileUtils fileUtils = (FileUtils)MutationFileUtil.context.getBean("fileUtils");
		LineIterator it = fileUtils.getFileContents(FileUtils.FILE_URL_PREFIX + file.getCanonicalPath());
		String line = it.next();
		while (line.startsWith("#")) {
			line = it.next();
		}
		// we are pointing to file header now
		boolean toReturn = !it.hasNext();
		it.close();
		return toReturn;
	}
        
        public static String[] getColumnHeaders(String fileName) throws Exception {
            File file = new File(fileName);
            FileUtils fileUtils = (FileUtils)MutationFileUtil.context.getBean("fileUtils");
            LineIterator it = fileUtils.getFileContents(FileUtils.FILE_URL_PREFIX + file.getCanonicalPath());
            String line = it.next();
            while (line.startsWith("#")) {
                line = it.next();
            }
            String[] columnHeaders = line.split("\t");
            it.close();
            return columnHeaders;
        }

	public static File[] sanitizeMAF(String mafFilename) throws Exception
	{
        Preconditions.checkArgument(!Strings.isNullOrEmpty(mafFilename),"A MAF filename is required");
        if (isAnnotated(mafFilename) || isEmpty(mafFilename)) {
            return null;
        }
        
		FileUtils fileUtils = (FileUtils)MutationFileUtil.context.getBean("fileUtils");
        File mafFile = new File(mafFilename);
        File sanitizedFile = org.apache.commons.io.FileUtils.getFile(
                org.apache.commons.io.FileUtils.getTempDirectory(),
                "" + System.currentTimeMillis() + ".sanitizedMAF");

	    File miscMAF = org.apache.commons.io.FileUtils.getFile(
			    org.apache.commons.io.FileUtils.getTempDirectory(),
			    "" + System.currentTimeMillis() + ".miscMAF");

        MafSanitizer sanitizer = new MafSanitizer();
        sanitizer.sanitizeMaf(mafFile.getCanonicalPath(),
                sanitizedFile.getCanonicalPath(),
                miscMAF.getCanonicalPath());

		File[] toReturn = new File[] { sanitizedFile, miscMAF };
		return toReturn;
	}
	
    public static String getAnnotatedFile(String stagingFilename) throws Exception
    {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(stagingFilename),"A staging file name is required");

        if (isAnnotated(stagingFilename) || isEmpty(stagingFilename)) {
            return stagingFilename;
        }

        File stagingFile = new File(stagingFilename);
		FileUtils fileUtils = (FileUtils)MutationFileUtil.context.getBean("fileUtils");
        File tmpMAF = org.apache.commons.io.FileUtils.getFile(org.apache.commons.io.FileUtils.getTempDirectory(),
                "" + System.currentTimeMillis() + ".tmpMAF");

        String annotatedMAF = fileUtils.annotateMAF(FileUtils.FILE_URL_PREFIX + stagingFile.getCanonicalPath(),
					FileUtils.FILE_URL_PREFIX + tmpMAF.getCanonicalPath());

	    return annotatedMAF;
    }

    public static void main (String...args) {
        String mafFile = "/tmp/annotator_out.maf";
        try {
            System.out.println("Output = " +MutationFileUtil.getAnnotatedFile(mafFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
