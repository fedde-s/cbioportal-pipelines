/*
 *  Copyright (c) 2014 Memorial Sloan-Kettering Cancer Center.
 * 
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 *  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 *  documentation provided hereunder is on an "as is" basis, and
 *  Memorial Sloan-Kettering Cancer Center 
 *  has no obligations to provide maintenance, support,
 *  updates, enhancements or modifications.  In no event shall
 *  Memorial Sloan-Kettering Cancer Center
 *  be liable to any party for direct, indirect, special,
 *  incidental or consequential damages, including lost profits, arising
 *  out of the use of this software and its documentation, even if
 *  Memorial Sloan-Kettering Cancer Center 
 *  has been advised of the possibility of such damage.
 */
package org.mskcc.cbio.importer.persistence.staging.util;

import com.google.common.base.*;
import org.apache.commons.lang.StringUtils;
import org.mskcc.cbio.importer.persistence.staging.StagingCommonNames;

import java.io.File;

/*
 represents a collection of static utility methods used though out the application
 */
public class StagingUtils {
    /*
    Public method to determine the absolute file name for a file that
    starts with an environmental variable
    (e.g. $PORTAL_DATA_HOME/study/xyz.txt
    If the input parameter does not start with a $, the input is returned as is
     */
    public static String resolveFileFromEnvironmentVariable(String input){
        Preconditions.checkArgument(!Strings.isNullOrEmpty(input), "A file name is required");
        if(input.startsWith("$")) {
            String[] parts = StringUtils.split(input, File.separator);
            parts[0] = System.getenv(parts[0].replace("$", ""));
            return StagingCommonNames.pathJoiner.join(parts);
        } else {
            return input;
        }
    }
}

