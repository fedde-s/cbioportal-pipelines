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
package org.mskcc.cbio.importer.fetcher.internal;

// imports
import org.mskcc.cbio.importer.*;
import org.mskcc.cbio.importer.model.*;
import org.mskcc.cbio.importer.dao.ImportDataRecordDAO;

import org.mskcc.cbio.portal.web_api.ConnectionManager;

import org.apache.commons.logging.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.methods.*;

import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class BiotabFetcherImpl extends FetcherBaseImpl implements Fetcher
{
    private static final int READ_TIMEOUT = 60000; // ms
    private static final int NO_REVISION_FOUND = -1;
    private static final String TUMOR_TYPE_REGEX = "<TUMOR_TYPE>";
    private static final String REVISION_REGEX = "<REVISION>";
    private static final String TCGA_PROPERTY_DELIMITER = "\\|";
	private static final Log LOG = LogFactory.getLog(BiotabFetcherImpl.class);

	private Config config;
	private FileUtils fileUtils;
	private ImportDataRecordDAO importDataRecordDAO;
	private DatabaseUtils databaseUtils;
	private DataSourcesMetadata dataSourceMetadata;

	private String tcgaClinicalURL;
	@Value("${tcga.clinical.url}")
	public void setTCGAClinicalURL(String tcgaClinicalURL) { this.tcgaClinicalURL = tcgaClinicalURL; }
	public String getTCGAClinicalURL() { return this.tcgaClinicalURL; }
	
	private String tcgaClinicalFilename;
	@Value("${tcga.clinical.filename}")
	public void setTCGAClinicalFilename(String tcgaClinicalFilename) { this.tcgaClinicalFilename = tcgaClinicalFilename; }
	public String getTCGAClinicalFilename() { return this.tcgaClinicalFilename; }

	public BiotabFetcherImpl(Config config, FileUtils fileUtils,
                             DatabaseUtils databaseUtils, ImportDataRecordDAO importDataRecordDAO)
    {

		this.config = config;
		this.fileUtils = fileUtils;
		this.databaseUtils = databaseUtils;
		this.importDataRecordDAO = importDataRecordDAO;
	}

	@Override
	public void fetchReferenceData(ReferenceMetadata referenceMetadata) throws Exception
    {
		throw new UnsupportedOperationException();
	}

    @Override
	public void fetch(String dataSource, String desiredRunDate, boolean updateStudiesWorksheet) throws Exception
    {

		logMessage(LOG, "fetch(), dateSource" + dataSource);
        initDataSourceMetadata(dataSource);
        fetchData();
        logMessage(LOG, "fetch(), complete.");
	}

    private void initDataSourceMetadata(String dataSource) throws Exception
    {
		Collection<DataSourcesMetadata> dataSourcesMetadata = config.getDataSourcesMetadata(dataSource);
		if (dataSourcesMetadata.isEmpty()) {
			throw new IllegalArgumentException("cannot instantiate a proper DataSourcesMetadata object.");			
		}
		this.dataSourceMetadata = dataSourcesMetadata.iterator().next();
    }

    private void fetchData() throws Exception
    {
		String[] urls = tcgaClinicalURL.split(TCGA_PROPERTY_DELIMITER);
		String[] filenames = tcgaClinicalFilename.split(TCGA_PROPERTY_DELIMITER);
		if (urls.length != filenames.length) {
			throw new IllegalArgumentException("Mismatched tcga.clinical.url and tcga.clinical.filename");
		}
        Pattern p = Pattern.compile("<a href=.+>(nationwidechildrens\\.org.*)</a>");
        for (String tumorType : config.getTumorTypesToDownload()) {
	for (int lc = 0; lc < urls.length; lc++) {
                            saveClinicalForTumorType(getURLToFileIndex(urls[lc],tumorType),tumorType, p);
                            break;			
	}
        }
    }      

    private String getURLToFileIndex(String url, String tumorType)
    {
        return (url.replace(TUMOR_TYPE_REGEX, tumorType.toLowerCase()));
    }

    private void saveClinicalForTumorType(String url, String tumorType, Pattern p)
    {
        HttpClient client = getHttpClient();
        HttpMethodParams params = client.getParams();
        params.setSoTimeout(READ_TIMEOUT);
        GetMethod method = new GetMethod(getURLToFileIndex(url, tumorType));

        try {
            if (client.executeMethod(method) == HttpStatus.SC_OK) {
                String line;
                BufferedReader bufReader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
                while((line = bufReader.readLine()) != null) {
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        String urlToDownload = url + m.group(1);
                        GetMethod fileDownloadMethod = new GetMethod(urlToDownload);
                        if(client.executeMethod(fileDownloadMethod) == HttpStatus.SC_OK) {
                            saveClinicalData(m.group(1), tumorType, fileDownloadMethod.getResponseBodyAsStream());
                        }
                    }
                }
            }
        }
        catch (Exception e) {}
        finally {
            method.releaseConnection();
        }
    }

    private HttpClient getHttpClient()
    {
        MultiThreadedHttpConnectionManager connectionManager =
            ConnectionManager.getConnectionManager();
        return new HttpClient(connectionManager);
    }

    private void saveClinicalData(String clinicalFilename, String tumorType, InputStream is) throws Exception
    {
        File clinicalDataFile =  fileUtils.createFileFromStream(getDestinationFilename(clinicalFilename,tumorType), is);
        createImportDataRecord(clinicalFilename, clinicalDataFile, tumorType);
    }

    private String getDestinationFilename(String clinicalFilename, String tumorType) throws Exception
    {
        return (getDownloadDirectory() +
                File.separator +
                tumorType +
                File.separator +
                clinicalFilename.replace(TUMOR_TYPE_REGEX, tumorType.toUpperCase()));
    }

    private String getDownloadDirectory() throws Exception
    {
        File downloadDirectory = new File(dataSourceMetadata.getDownloadDirectory());
        if (!downloadDirectory.exists()) {
            fileUtils.makeDirectory(downloadDirectory);
        }

        return downloadDirectory.getCanonicalPath();
    }


    
    private void createImportDataRecord(String clinicalFilename, File clinicalDataFile, String tumorType) throws Exception
    {
        String computedDigest = fileUtils.getMD5Digest(clinicalDataFile);
        for (DatatypeMetadata datatype : config.getFileDatatype(dataSourceMetadata, clinicalFilename)) {
            if (!datatype.isDownloaded()) continue;
            ImportDataRecord importDataRecord = new ImportDataRecord(dataSourceMetadata.getDataSource(),
                                                                         "tcga",
                                                                         tumorType, tumorType,
                                                                         datatype.getDatatype(),
                                                                         Fetcher.LATEST_RUN_INDICATOR,
                                                                         clinicalDataFile.getCanonicalPath(),
                                                                         computedDigest, clinicalFilename);
                importDataRecordDAO.importDataRecord(importDataRecord);
        }
    }
}
