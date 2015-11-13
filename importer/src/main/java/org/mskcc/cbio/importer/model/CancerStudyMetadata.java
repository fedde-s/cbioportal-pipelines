/** Copyright (c) 2012 Memorial Sloan-Kettering Cancer Center.
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
package org.mskcc.cbio.importer.model;

// imports

import org.mskcc.cbio.portal.model.CancerStudy;

import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.io.File;
import java.util.Properties;

/**
 * Class which contains cancer study metadata.
 */
public class CancerStudyMetadata {

    public static final String WORKSHEET_UPDATE_COLUMN_KEY = "CANCERSTUDIES";
    public static final String CANCER_STUDY_COLUMN_KEY = "CANCERSTUDIES";
    public static final String CANCER_TYPE_COLUMN_KEY = "CANCERTYPE";
    public static final String STABLE_ID_COLUMN_KEY = "STABLEID";
    public static final String NAME_COLUMN_KEY = "NAME";
    public static final String DESCRIPTION_COLUMN_KEY = "DESCRIPTION";
    public static final String CITATION_COLUMN_KEY = "CITATION";
    public static final String PMID_COLUMN_KEY = "PMID";
    public static final String GROUPS_COLUMN_KEY = "GROUPS";
    public static final String SHORT_NAME_COLUMN_KEY = "SHORTNAME";
    public static final String CONVERT_COLUMN_KEY = "CONVERT";
    public static final String TRIAGE_PORTAL_COLUMN_KEY = "triage-portal";
    public static final String MSK_PORTAL_COLUMN_KEY = "msk-automation-portal";
    public static final String MSK_IMPACT_PORTAL_COLUMN_KEY = "mskimpact-portal";
    public static final String SOURCE_COLUMN_KEY = "SOURCE";

    // delimiter between tumor type and center (used for find the path)

    public static final String CANCER_STUDY_DELIMITER = "/";

    // this is value in worsheet-matrix cell if cancer study is in a desired portal
    public static final String CANCER_STUDY_IN_PORTAL_INDICATOR = "x";

    // file/file extension of metadata file
    private static final String CANCER_STUDY_METADATA_FILE_EXT = ".txt";
    public static final String CANCER_STUDY_METADATA_FILE = "meta_study" + CANCER_STUDY_METADATA_FILE_EXT;

    // cancer study identifier delimiter (used in metadata files)
    private static final String CANCER_STUDY_IDENTIFIER_DELIMITER = "_";

    // these are the tags to replace in description
    public static final String NUM_CASES_TAG = "<NUM_CASES>";
    public static final String TUMOR_TYPE_TAG = "<TUMOR_TYPE>";
    public static final String TUMOR_TYPE_NAME_TAG = "<TUMOR_TYPE_NAME>";

    // bean properties
    private String name;
    private String tumorType;
    private TumorTypeMetadata tumorTypeMetadata;
    private String description;
    private String citation;
    private String pmid;
    private String studyPath;
    private String stableId;
    private String center;
    private String groups;
    private String shortName;
    private boolean convert;
    private boolean updateStudyDelete;

    /**
     * Create a CancerStudyMetadata instance with properties in given array.
     * ITs assumed order of properties is that from google worksheet.
     * cancerStudyPath is of the form brca/tcga/pub that you would find
     * on the google spreadsheet cancer_studies worksheet
     * All portal columns are ignored (anything > 1)
     *
     * @param properties String[]
     */
    public CancerStudyMetadata(String[] properties) {

        if (properties.length < 12) {
            throw new IllegalArgumentException("corrupt properties array passed to contructor");
		}
                
        this.studyPath = properties[0].trim();
        String[] parts = properties[0].trim().split(CANCER_STUDY_DELIMITER);
        this.center = (parts.length < 2) ? "No center defined" : parts[1];
        this.tumorType = properties[1].trim();
        this.stableId = properties[2].trim();
        if (this.stableId.isEmpty()) {
            this.stableId = this.studyPath.replaceAll("/", "_");
        }
        this.name = properties[3].trim();
        this.description = properties[4].trim();
        this.citation = properties[5].trim();
        this.pmid = properties[6].trim();
        this.groups = properties[7].trim();
        this.shortName = properties[8].trim();
        this.convert = Boolean.parseBoolean(properties[9].trim());
        this.updateStudyDelete = false;
	}

    public CancerStudyMetadata(String studyPath, CancerStudy cancerStudy)
    {
        this.studyPath = studyPath;
        if (cancerStudy.getTypeOfCancerId() == null) {
            throw new IllegalArgumentException("cancerStudy-typeOfCancer cannot be null");
        }
        this.tumorType = cancerStudy.getTypeOfCancerId();
        if (cancerStudy.getCancerStudyStableId() == null) {
            throw new IllegalArgumentException("cancerStudy-stable id cannot be null");
        }
        this.stableId = cancerStudy.getCancerStudyStableId();
        this.name = (cancerStudy.getName() != null) ? cancerStudy.getName() : "";
        this.description = (cancerStudy.getDescription() != null) ? cancerStudy.getDescription() : "";
        this.citation = (cancerStudy.getCitation() != null) ? cancerStudy.getCitation() : "";
        this.pmid = (cancerStudy.getPmid() != null) ? cancerStudy.getPmid() : "";
        this.groups = (cancerStudy.getGroups() != null) ? StringUtils.join(cancerStudy.getGroups(), ";") : "";
        this.shortName = (cancerStudy.getShortName() != null) ? cancerStudy.getShortName() : "";
        this.convert = false;
        this.updateStudyDelete = false;
    }

	public CancerStudyMetadata(Properties props)
	{
		this.name = props.getProperty("name", "");
		this.tumorType = props.getProperty("type_of_cancer", "");
		this.stableId = props.getProperty("cancer_study_identifier", "");
		this.studyPath = props.getProperty("study_path", "");
		this.description = props.getProperty("description", "");
		this.citation = props.getProperty("citation", "");
		this.pmid = props.getProperty("pmid", "");
		this.groups = props.getProperty("groups", "");
		this.shortName = props.getProperty("short_name", "");
        this.updateStudyDelete = false;
	}

	public String getName() { return name; }
	public String getTumorType() { return tumorType; }
	public String getStableId() { return stableId; }
	public TumorTypeMetadata getTumorTypeMetadata() { return tumorTypeMetadata; }
	public void setTumorTypeMetadata(TumorTypeMetadata tumorTypeMetadata) { this.tumorTypeMetadata = tumorTypeMetadata; }
	public String getStudyPath() { return studyPath; }
    public String getCenter() { return center; }
	public String getDescription() { return description; }
	public String getCitation() { return citation; }
	public String getPMID() { return pmid; }
	public String getGroups() { return groups; }
    public String getShortName() { return shortName; }
	public Boolean isConverted() { return convert; }
    public Boolean isUpdateStudyDelete() { return updateStudyDelete; }
    public void setUpdateStudyDelete(boolean updateStudyDelete)
    {
        this.updateStudyDelete = updateStudyDelete;
    }
	public String getCancerStudyMetadataFilename() {
		//return getStudyPath() + File.separator + toString() + CANCER_STUDY_METADATA_FILE_EXT;
		return CANCER_STUDY_METADATA_FILE;
	}
	public String toString() { return stableId; }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof CancerStudyMetadata)) {
            return false;
        }
        CancerStudyMetadata other = (CancerStudyMetadata)obj;
        return (other.toString().equals(this.toString()));
    }

    public Map<String, String> getProperties() {
        Map<String, String> toReturn = new HashMap<String, String>();
        toReturn.put(CANCER_STUDY_COLUMN_KEY, studyPath);
        toReturn.put(CANCER_TYPE_COLUMN_KEY, tumorType);
        toReturn.put(STABLE_ID_COLUMN_KEY, stableId);
        toReturn.put(NAME_COLUMN_KEY, name);
        toReturn.put(DESCRIPTION_COLUMN_KEY, description);
        toReturn.put(CITATION_COLUMN_KEY, citation);
        toReturn.put(PMID_COLUMN_KEY, pmid);
        toReturn.put(GROUPS_COLUMN_KEY, groups);
        toReturn.put(SHORT_NAME_COLUMN_KEY, shortName);
        toReturn.put(CONVERT_COLUMN_KEY, Boolean.toString(convert));
        return toReturn;
    }
}
