package org.mskcc.cbio.maf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MAF Processor specific to the Annotator tool.
 *
 * @author Selcuk Onur Sumer
 */
public class AnnoMafProcessor extends MafProcessor
{
	protected List<String> annoHeaders;

	/**
	 * Constructor.
	 *
	 * @param headerLine    header line of the input MAF.
	 */
	public AnnoMafProcessor(String headerLine)
	{
		super(headerLine);
		this.annoHeaders = new ArrayList<String>(0);
	}

	/**
	 * Constructor.
	 *
	 * @param headerLine    header line of the input MAF.
	 * @param annoHeaders   headers specific to the annotator.
	 */
	public AnnoMafProcessor(String headerLine, List<String> annoHeaders)
	{
		super(headerLine);
		this.annoHeaders = annoHeaders;
	}

	/**
	 * Adds oncotator column names into the given header list.
	 *
	 * @param headerData    list of header names
	 * @param addMissing    indicates if the missing columns will also be added
	 */
	protected void addOncoColsToHeader(List<String> headerData,
			boolean addMissing)
	{
		for (String header : this.oncoHeaders)
		{
			// never add missing oncotator columns
			this.addColumnToHeader(headerData, header, false);
		}
	}

	/**
	 * Adds MA column names into the given header list.
	 *
	 * @param headerData    list of header names
	 * @param addMissing    indicates if the missing columns will also be added
	 */
	protected void addMaColsToHeader(List<String> headerData,
			boolean addMissing)
	{
		for (String header : this.maHeaders)
		{
			// never add missing MA columns
			this.addColumnToHeader(headerData, header, false);
		}
	}

	/**
	 * Adds other (custom) column names into the header list.
	 * @param headerData    list of header names
	 */
	protected void addOtherColsToHeader(List<String> headerData)
	{
		// add other custom columns
		super.addOtherColsToHeader(headerData);

		// always add missing annotator headers
		this.addNewColsToHeader(headerData);
	}

	/**
	 * Adds new annotator column names into the header list.
	 *
	 * @param headerData    list of header names
	 */
	protected void addNewColsToHeader(List<String> headerData)
	{
		for (String header : this.annoHeaders)
		{
			if (!headerData.contains(header))
			{
				// add missing annotator headers
				headerData.add(header);
			}
		}
	}

	/**
	 * Updates the data list by adding annotator data.
	 *
	 * @param data              data representing a single line in the input
	 */
	public void updateAnnoData(List<String> data, Map<String, String> annoData)
	{
		// create a new maf util for the new header line to get new indices
		String newHeaderLine = this.newHeaderLineAsString();
		MafUtil mafUtil = new MafUtil(newHeaderLine);

		// update the data using the annotator data
		for (String header: this.annoHeaders)
		{
			data.set(mafUtil.getColumnIndex(header), annoData.get(header));
		}
	}
}
