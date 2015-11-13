/** Copyright (c) 2015 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cbio.maf;


import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Designed to merge two different MAF files into one.
 *
 * @author Selcuk Onur Sumer
 */
public class MafMerger
{
	/**
	 * Merges source MAF into target MAF by matching the source data to the
	 * correct target column. If no column exists in the target MAF for a
	 * certain data column in the source MAF, then that column will appear
	 * as an extra column at the end.
	 *
	 * @param targetMAF target MAF file
	 * @param sourceMAF source MAF file
	 * @return  merged MAF file
	 */
	public File merge(File targetMAF, File sourceMAF) throws IOException
	{
		File mergedMAF = new File(targetMAF.getCanonicalPath() + ".merged");

		BufferedReader targetReader = new BufferedReader(new FileReader(targetMAF));
		BufferedReader sourceReader = new BufferedReader(new FileReader(sourceMAF));
		BufferedWriter writer = new BufferedWriter(new FileWriter(mergedMAF));

		// get target header
		MafHeaderUtil targetHeaderUtil = new MafHeaderUtil();
		String targetHeader = targetHeaderUtil.extractHeader(targetReader);

		// get source header
		MafHeaderUtil sourceHeaderUtil = new MafHeaderUtil();
		String sourceHeader = sourceHeaderUtil.extractHeader(sourceReader);

		// find out additional columns
		List<String> additionalColumns = this.extractAdditionalCols(targetHeader, sourceHeader);
		AnnoMafProcessor processor = new AnnoMafProcessor(targetHeader, additionalColumns);

		// write the target file as is
		// (with additional empty columns included if necessary)

		// write comments/metadata to the output
		FileIOUtil.writeLines(writer, targetHeaderUtil.getComments());

		// write header to the outputs
		FileIOUtil.writeLine(writer, processor.newHeaderList());

		// write data lines from target MAF
		this.processFile(targetReader, writer, processor);


		// append the source MAF at the end of the target

		List<String> newHeaders = processor.getNewHeaders();
		processor = new AnnoMafProcessor(sourceHeader);
		// we need to manually set the new headers
		// to match the previous processor's headers,
		// instead of using processor.newHeaderList()
		processor.setNewHeaders(newHeaders);

		// write data lines from source MAF
		this.processFile(sourceReader, writer, processor);

		targetReader.close();
		sourceReader.close();
		writer.close();

		return mergedMAF;
	}

	protected void processFile(BufferedReader reader,
			BufferedWriter writer,
			AnnoMafProcessor processor) throws IOException
	{
		String line;

		while ((line = reader.readLine()) != null)
		{
			if (line.trim().length() == 0)
			{
				continue;
			}

			// process line
			List<String> data = processor.newDataList(line);
			FileIOUtil.writeLine(writer, data);
		}
	}

	protected List<String> extractAdditionalCols(String targetHeader, String sourceHeader)
	{
		List<String> targetColumns = Arrays.asList(targetHeader.split("\t"));
		List<String> sourceColumns = Arrays.asList(sourceHeader.split("\t"));
		List<String> difference = new LinkedList<String>();

		for (String sourceCol: sourceColumns)
		{
			if (!targetColumns.contains(sourceCol))
			{
				difference.add(sourceCol);
			}
		}

		return difference;
	}

	// main method for stand alone testing
	public static void main (String...args) {
		if (args.length < 2) {
			System.err.println("Usage java MafMerger <target file> <source file>");
			return;
		}
		File targetFile = new File(args[0]);
		File sourceFile = new File(args[1]);
		System.out.println("Merging " +sourceFile.getName() +" into " +targetFile.getName());
		try {
			File mergedFile = new MafMerger().merge(targetFile, sourceFile);
			System.out.println("Merged file: " +mergedFile.getName());
		} catch (IOException e) {;
			e.printStackTrace();
		}
	}
}
