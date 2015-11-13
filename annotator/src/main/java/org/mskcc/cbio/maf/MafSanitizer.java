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

import com.google.common.base.Joiner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs a sanity check for MAF files.
 *
 * @author Selcuk Onur Sumer
 */
public class MafSanitizer
{
	protected int invalidCount;
	private static final Log LOG = LogFactory.getLog(MafSanitizer.class);

	public MafSanitizer()
	{
		this.invalidCount = 0;
	}

	/**
	 * Checks the given input MAF file for errors.
	 *
	 * @param input     input MAF file
	 * @throws IOException
	 */
	public void sanitizeMaf(String input) throws IOException
	{
		sanitizeMaf(input, null);
	}

	/**
	 * Checks the given input MAF file for errors, and tries to fix the errors.
	 * If a specific line of the MAF file has critical errors that cannot be
	 * fixed, then outputs that line to a separate file than the given output.
	 *
	 * @param input     original input MAF filename
	 * @param output    sanitized output MAF filename
	 * @throws IOException
	 */
	public void sanitizeMaf(String input, String output) throws IOException
	{
		sanitizeMaf(input, output, null);
	}

	/**
	 * Checks the given input MAF file for errors, and tries to fix the errors.
	 * If a specific line of the MAF file has critical errors that cannot be
	 * fixed, then outputs that line to the misc file.
	 *
	 * @param input     original input MAF filename
	 * @param output    sanitized output MAF filename
	 * @param miscOut   misc output MAF filename
	 * @throws IOException
	 */
	public void sanitizeMaf(String input,
			String output,
			String miscOut) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(input));

		// the output MAF file that will contain sanitized entries
		BufferedWriter writer = (output == null) ?
				null : new BufferedWriter(new FileWriter(output));

		// the misc file that will contain mutations cannot be annotated due to a critical error
		BufferedWriter miscWriter = (output == null) ?
				null : new BufferedWriter(new FileWriter(generateMiscFilename(output, miscOut)));

		AnnoMafProcessor processor = null;
		MafHeaderUtil headerUtil = new MafHeaderUtil();
		String line = headerUtil.extractHeader(reader);

		MafUtil util = new MafUtil(line);
		MafRecord record;

		if (writer != null)
		{
			// TODO make sort & add missing optional!
			// process MAF file to sort & add missing standard columns
			processor = new AnnoMafProcessor(line);
			List<String> columnNames = processor.newHeaderList();

			// write comments/metadata to the output
			FileIOUtil.writeLines(writer, headerUtil.getComments());

			// write header to the outputs
			FileIOUtil.writeLine(writer, columnNames);
			FileIOUtil.writeLine(miscWriter, columnNames);

			//writer.write(line);
			//writer.newLine();
		}

		// including the header line
		int lineCount = headerUtil.getComments().size() + 1;

		while ((line = reader.readLine()) != null)
		{
			lineCount++;

			if (line.trim().length() == 0)
			{
				continue;
			}

			record = util.parseRecord(line);

			// try to sanitize line
			String sanitizedLine = sanitizeLine(util, record, line, lineCount);

			if (writer != null)
			{
				List<String> data;

				// if there are still critical errors,
				// then output the original line to a misc output file
				if (hasCriticalError(util.parseRecord(sanitizedLine)))
				{
					// process original line
					data = processor.newDataList(line);
					FileIOUtil.writeLine(miscWriter, data);
				}
				// write to the regular output if no critical error
				else
				{
					// process sanitized line
					data = processor.newDataList(sanitizedLine);
					FileIOUtil.writeLine(writer, data);
				}

				//writer.write(line);
				//writer.newLine();
			}
		}

		printSummary(invalidCount);

		reader.close();

		if (writer != null)
		{
			writer.close();
			miscWriter.close();
		}
	}

	private String generateMiscFilename(String output, String miscOut)
	{
		if (miscOut != null)
		{
			return miscOut;
		}

		String miscFile = "";
		int idx = output.lastIndexOf('.');

		if (idx != -1)
		{
			miscFile = output.substring(0, idx) + "_misc." + output.substring(idx + 1);
		}
		else
		{
			miscFile = output + "_misc";
		}

		return miscFile;
	}

	public boolean hasCriticalError(MafRecord record)
	{
		MafErrorReport report = this.identifyErrors(record);
		return report.hasCriticalError();
	}

	/**
	 * Prints a single line summary message.
	 *
	 * @param count number of lines with invalid values.
	 */
	public void printSummary(int count)
	{
		if (count > 0)
		{
			//System.out.println("Number of errors and/or warnings: " + count);
			LOG.info("[MafSanitizer] Number of errors and/or warnings: " + count);
		}
		else
		{
			//System.out.println("No error or warning");
			LOG.info("[MafSanitizer] Number of errors and/or warnings: " + count);
		}
	}

	public MafErrorReport identifyErrors(MafRecord record)
	{
		return this.identifyErrors(record, -1);
	}

	public MafErrorReport identifyErrors(MafRecord record, int lineNumber)
	{
		MafErrorReport errorReport = new MafErrorReport();
		errorReport.lineNumber = lineNumber;

		if (!isValidAllele(record.getReferenceAllele()))
		{
			errorReport.malformedAllele = true;
			errorReport.malformedRef = true;
		}

		if (!isValidAllele(record.getTumorSeqAllele1()))
		{
			errorReport.malformedAllele = true;
			errorReport.malformedTum1 = true;
		}

		if (!isValidAllele(record.getTumorSeqAllele2()))
		{
			errorReport.malformedAllele = true;
			errorReport.malformedTum2 = true;
		}

		// check for missing end position
		errorReport.missingEnd = (record.getEndPosition() == TabDelimitedFileUtil.NA_LONG);

		// check for missing start position
		errorReport.missingStart = (record.getStartPosition() == TabDelimitedFileUtil.NA_LONG);

		errorReport.invalidChromosome = !record.getChr().replace("chr", "").matches(
				"[1-9|X|Y|x|y]|[0-1][0-9]|[2][0-2]");

		// check if chromosome starts with "chr"
		errorReport.longChrName = record.getChr().startsWith("chr");

		// check if insertion type of mutations have adjacent coordinates
		errorReport.nonAdjacentIns = verifyInsAdjacency(record);

		// check the length of the mutation and the reference base (they should be equal)
		errorReport.invalidLength = false;

		if (!errorReport.missingStart && !errorReport.missingEnd)
		{
			errorReport.invalidLength = !isValidLength(record);
		}

		return errorReport;
	}

	// TODO log to a file instead of printing out
	public void printErrors(MafRecord record, MafErrorReport errorReport)
	{
		if (errorReport.containsError())
		{
			List<String> errorMessages = new ArrayList<String>();
			List<String> warningMessages = new ArrayList<String>();

			if (errorReport.invalidChromosome)
			{
				errorMessages.add("invalid chromosome");
			}

			if (errorReport.malformedRef)
			{
				errorMessages.add("invalid ref allele");
			}

			if (errorReport.malformedTum1 && errorReport.malformedTum2)
			{
				errorMessages.add("invalid tumor allele");
			}

			if (errorReport.longChrName)
			{
				warningMessages.add("redundant 'chr' in chromosome name");
			}

			if (errorReport.missingStart)
			{
				errorMessages.add("invalid start position");
			}

			if (errorReport.missingEnd)
			{
				warningMessages.add("invalid end position");
			}

			if (errorReport.nonAdjacentIns)
			{
				warningMessages.add("insertion type should have adjacent positions");
			}

			if (errorReport.invalidLength)
			{
				warningMessages.add("ref allele length do not match start-end positions");
			}

			String message = "";

			if (errorMessages.size() > 0 || warningMessages.size() > 0)
			{
				message = "[" + errorReport.lineNumber + "] ";
			}

			if (errorMessages.size() > 0)
			{
				message += "ERROR: " + Joiner.on(";").join(errorMessages) + ". ";
			}

			if (warningMessages.size() > 0)
			{
				message += "WARNING: " + Joiner.on(";").join(warningMessages) + ".";
			}

			if (message.length() > 0)
			{
				//System.out.println(message);
				LOG.warn("[MafSanitizer] " + message);
			}
		}
	}

	public String fixErrors(MafUtil util, MafRecord record, String line, MafErrorReport errorReport)
	{
		String parts[] = line.split("\t", -1);
		String newLine = "";

		String refAllele = record.getReferenceAllele();
		String tumAllele1 = record.getTumorSeqAllele1();
		String tumAllele2 = record.getTumorSeqAllele2();
		String chromosome = record.getChr();

		if (errorReport.malformedRef)
		{
			refAllele = correctRefAllele(refAllele, record);
		}

		if (errorReport.malformedTum1)
		{
			tumAllele1 = correctTumAllele(tumAllele1, record);
		}

		if (errorReport.malformedTum2)
		{
			tumAllele2 = correctTumAllele(tumAllele2, record);
	 		// To fix a bug in VEP annotation - if tumAllele2 is empty:
	 		// assign data under tumAllele1 to tumorAllel2 and let tumAllele1 be refAllele
			if (tumAllele2.isEmpty() && !tumAllele1.isEmpty() && !refAllele.isEmpty()) {
				tumAllele2 = tumAllele1;
				tumAllele1 = refAllele;
			}
		}

		if (errorReport.longChrName)
		{
			chromosome = chromosome.substring(3);
		}

		if (errorReport.invalidChromosome)
		{
			if (chromosome.equals("23"))
			{
				chromosome = "X";
			}
			else if (chromosome.equals("24"))
			{
				chromosome = "Y";
			}
		}

		for (int i=0; i < parts.length; i++)
		{
			if (errorReport.malformedAllele &&
			    util.getReferenceAlleleIndex() == i)
			{
				newLine += refAllele;
			}
			else if (errorReport.malformedAllele &&
			         util.getTumorSeqAllele1Index() == i)
			{
				newLine += tumAllele1;
			}
			else if (errorReport.malformedAllele &&
			         util.getTumorSeqAllele2Index() == i)
			{
				newLine += tumAllele2;
			}
			else if (errorReport.isInvalidPos() &&
			         util.getStartPositionIndex() == i)
			{
				newLine += extractPos(parts[i]);
			}
			else if (errorReport.isInvalidPos() &&
			         util.getEndPositionIndex() == i)
			{
				newLine += extractPos(parts[i]);
			}
			else if ((errorReport.missingEnd || errorReport.invalidLength) &&
			         util.getEndPositionIndex() == i)
			{
				newLine += calculateEndPos(record.getStartPosition(),
				                           refAllele);
			}
			else if ((errorReport.longChrName || errorReport.invalidChromosome) &&
			         util.getChrIndex() == i)
			{
				newLine += chromosome;
			}
			// TODO ignore nonAdjacentIns for now
//			else if (errorReport.nonAdjacentIns &&
//			         util.getEndPositionIndex() == i)
//			{
//				newLine += record.getEndPosition() + 1;
//			}
			else
			{
				newLine += parts[i];
			}

			if (i < parts.length - 1)
			{
				newLine += "\t";
			}
		}

		return newLine;
	}

	public String sanitizeLine(MafUtil util, MafRecord record, String line, int lineNumber)
	{
		String newLine = line;

		MafErrorReport errorReport = identifyErrors(record, lineNumber);
		printErrors(record, errorReport);

//		if (nonAdjacentIns)
//		{
//			System.out.println(record.getChr() + "_" + record.getStartPosition() + "_" + record.getEndPosition());
//		}

//		if (malformedAllele)
//		{
//			String[] parts = line.split("\t", -1);
//			String aaChange = "NA";
//			String maVariant = "NA";
//
//			if (util.getAaChangeIndex() != -1 &&
//				util.getAaChangeIndex() < parts.length)
//			{
//				aaChange = parts[util.getAaChangeIndex()];
//			}
//
//			if (util.getMaVariantIndex() != -1 &&
//				util.getMaVariantIndex() < parts.length)
//			{
//				maVariant = parts[util.getMaVariantIndex()];
//			}
//
//			System.out.println("[" + lineNumber + "] " +
//				record.getReferenceAllele() + " " +
//				record.getTumorSeqAllele1() + " " +
//				record.getTumorSeqAllele2() + " " +
//				"chr" + record.getChr() + ":" +
//				record.getStartPosition());
//		}

		if (errorReport.containsError())
		{
			invalidCount++;

			// fix errors (if possible)
			newLine = fixErrors(util, record, line, errorReport);
		}

		return newLine;
	}

	public boolean isValidAllele(String allele)
	{
		boolean valid = false;

		if (allele.matches("[TCGAtcga]+") ||
		    allele.equals("-"))
		{
			valid = true;
		}

		return valid;
	}

	public boolean isValidLength(MafRecord record)
	{
		boolean valid = false;

		if (record.getStartPosition() != TabDelimitedFileUtil.NA_INT &&
		    record.getEndPosition() != TabDelimitedFileUtil.NA_INT)
		{

			Long diff = record.getEndPosition() - record.getStartPosition();

			// assuming reference allele is valid (skipping check for "-")
			// TODO also check if ref allele is valid?
			if (record.getReferenceAllele().equals("-") ||
				record.getReferenceAllele().length() - 1 == diff)
			{
				valid = true;
			}
		}

		return valid;
	}

	public Long calculateEndPos(Long startPos,
			String refAllele)
	{
		Long endPos = startPos;

		if (refAllele.equals("-")) // insertion (should be adjacent)
		{
			endPos = startPos + 1;
		}
		else if (refAllele.matches("[0-9]+")) // length instead of the actual sequence
		{
			endPos = startPos + Integer.parseInt(refAllele) - 1;
		}
		else // other (depends on reference allele length)
		{
			endPos = startPos + refAllele.length() - 1;
		}

		return endPos;
	}

	private String correctRefAllele(String allele, MafRecord record)
	{
		String fineAllele = allele;

		if (allele.matches("[0-9]+"))
		{
			// TODO get the correct sequence from other sources...
		}
		else
		{
			fineAllele = correctTumAllele(allele, record);
		}

		return fineAllele;
	}

	private String correctTumAllele(String allele, MafRecord record)
	{
		String fineAllele = allele;
		StringBuffer buffer;

		if (allele.matches("[0-9]+"))
		{
			int length = Integer.parseInt(allele);
			buffer = new StringBuffer();

			// simply replace all with A
			for (int i = 0; i < length; i++)
			{
				buffer.append('A');
			}

			fineAllele = buffer.toString();
		}
		else if (allele.contains(";"))
		{
			// TODO find out which nucleotide corresponds to the reported amino_acid_change (by using record?)
		}
		else
		{
			// default replacement is "-"
			fineAllele = "";
		}

		return fineAllele;
	}

	private String extractPos(String position)
	{
		int index = 0;

		for (index = 0; index < position.length(); index++)
		{
			if (!Character.isDigit(position.charAt(index)))
			{
				break;
			}
		}

		return position.substring(0, index);
	}

	private boolean verifyInsAdjacency(MafRecord record)
	{
		return record.getReferenceAllele().equals("-") &&
		       record.getStartPosition() != TabDelimitedFileUtil.NA_LONG &&
		       record.getEndPosition() != TabDelimitedFileUtil.NA_LONG &&
		       record.getStartPosition() == record.getEndPosition();
	}
}
