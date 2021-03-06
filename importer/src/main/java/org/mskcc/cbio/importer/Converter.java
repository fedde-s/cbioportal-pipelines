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
package org.mskcc.cbio.importer;

// imports
import java.util.Arrays;
import java.util.HashSet;
import org.mskcc.cbio.importer.model.DataMatrix;
import org.mskcc.cbio.importer.model.PortalMetadata;
import org.mskcc.cbio.importer.model.DatatypeMetadata;
import org.mskcc.cbio.importer.model.CancerStudyMetadata;

import java.util.Set;

/**
 * Interface used to convert portal data.
 */
public interface Converter {

	public static final String VALUE_DELIMITER = "\t";
	public static final String GENE_ID_COLUMN_HEADER_NAME = "Entrez_Gene_Id";
	public static final String GENE_SYMBOL_COLUMN_HEADER_NAME = "Hugo_Symbol";
	public static final String MUTATION_CASE_ID_COLUMN_HEADER = "Tumor_Sample_Barcode";
 	public static final String MUTATION_CASE_LIST_META_HEADER = "sequenced_samples";
        public static final Set<String> NON_CASE_IDS = new HashSet<String>(
        Arrays.asList("MIRNA", "LOCUS", "ID", "GENE SYMBOL", "ENTREZ_GENE_ID", "HUGO_SYMBOL", "LOCUS ID", "CYTOBAND", "COMPOSITE.ELEMENT.REF", "HYBRIDIZATION REF"));

	/**
	 * Converts data for the given portal.
	 *
     * @param portal String
	 * @param runDate String
	 * @param applyOverrides Boolean
	 * @throws Exception
	 */
	void convertData(String portal, String runDate, Boolean applyOverrides) throws Exception;

	/**
	 * Generates case lists for the given portal.
	 *
     * @param portal String
	 * @throws Exception
	 */
	void generateCaseLists(String portal) throws Exception;

    /**
	 * Applies overrides to the given portal using the given data source.
	 * Any datatypes within the excludes datatypes set will not have be overridden.
	 *
	 * @param portal String
	 * @param excludeDatatypes Set<String>
	 * @param applyCaseLists boolean
	 * @throws Exception
	 */
	void applyOverrides(String portal, Set<String> excludeDatatypes, boolean applyCaseLists) throws Exception;

	/**
	 * Creates a staging file from the given import data.
	 *
     * @param portalMetadata PortalMetadata
	 * @param cancerStudy CancerStudyMetadata
	 * @param datatypeMetadata DatatypeMetadata
	 * @param dataMatrices DataMatrix[]
	 * @throws Exception
	 */
	void createStagingFile(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata,
						   DatatypeMetadata datatypeMetadata, DataMatrix[] dataMatrices) throws Exception;
}
